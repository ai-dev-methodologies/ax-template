package com.ax.template.authblueprint.identityverification;

import com.ax.template.authblueprint.auditlog.AuditLogDto;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import com.ax.template.authblueprint.auditlog.AuditOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * R54 — sole mutator for the identity-verification domain.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>IDV-PROVIDER-002 — resolve adapter by provider name; unknown → throw.</li>
 *   <li>IDV-CALLBACK-002 — extract via adapter, persist {@link VerifiedIdentity}.</li>
 *   <li>IDV-AUDIT-001 — every callback attempt (SUCCESS / HMAC_FAIL /
 *       EXTRACTION_FAIL / UNKNOWN_PROVIDER) publishes an {@link AuditLogDto} entry
 *       via {@link AuditLogService}; CI is recorded, RRN never is.</li>
 * </ul>
 *
 * <p>HMAC verification stays at the controller layer because it needs the raw
 * body byte-for-byte before any JSON parsing. The controller invokes
 * {@link #recordHmacFailure(String)} on a verification miss so all four outcome
 * paths funnel through this service for audit consistency.
 */
@Service
public class IdentityVerificationService {

    public static final String AUDIT_ACTION = "IDENTITY_VERIFICATION_CALLBACK";
    public static final String RESOURCE_TYPE = "verified_identity";

    /** IDV-CONCORDANCE-001 — distinct action constant; never folded into {@link #AUDIT_ACTION}. */
    public static final String CONCORDANCE_AUDIT_ACTION = "IDENTITY_VERIFICATION_CONCORDANCE_MISMATCH";

    /** IDV-ADMIN-001 — pagination bounds for the admin browse surface. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    private static final Logger log =
        LoggerFactory.getLogger(IdentityVerificationService.class);

    private final Map<String, IdentityVerificationProvider> providersByName;
    private final VerifiedIdentityRepository repository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public IdentityVerificationService(List<IdentityVerificationProvider> providers,
                                        VerifiedIdentityRepository repository,
                                        AuditLogService auditLogService,
                                        ObjectMapper objectMapper) {
        Map<String, IdentityVerificationProvider> map = new java.util.LinkedHashMap<>();
        for (IdentityVerificationProvider p : providers) {
            map.put(p.providerName(), p);
        }
        this.providersByName = Map.copyOf(map);
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VerifiedIdentity processCallback(String providerName, byte[] rawBody) {
        IdentityVerificationProvider adapter = providersByName.get(providerName);
        if (adapter == null) {
            publishAudit(providerName, AuditOutcome.FAILURE, "UNKNOWN_PROVIDER", null);
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.UNKNOWN_PROVIDER,
                "Unknown identity verification provider: " + providerName);
        }
        try {
            VerifiedIdentityData data = adapter.extract(rawBody);
            detectConcordanceMismatch(data);
            VerifiedIdentity saved = repository.save(VerifiedIdentity.create(data));
            publishAudit(providerName, AuditOutcome.SUCCESS, "SUCCESS", data.ci());
            return saved;
        } catch (IdentityVerificationException ex) {
            publishAudit(providerName, AuditOutcome.FAILURE, ex.reason().name(), null);
            throw ex;
        }
    }

    /**
     * IDV-CONCORDANCE-001 — a re-verification presenting the SAME ci with a DIFFERENT di (or
     * vice versa) is a concordance mismatch: rejected fail-closed BEFORE any persist, with its
     * own distinct-action audit event (in addition to the generic per-callback audit the outer
     * catch always fires). A full match (both tokens equal) or no prior match (neither token
     * seen before) is not a mismatch and proceeds normally.
     */
    private void detectConcordanceMismatch(VerifiedIdentityData data) {
        boolean mismatch = repository.findByCi(data.ci()).stream().anyMatch(v -> !v.getDi().equals(data.di()))
            || repository.findByDi(data.di()).stream().anyMatch(v -> !v.getCi().equals(data.ci()));
        if (mismatch) {
            publishConcordanceMismatchAudit(data.providerName(), data.ci());
            throw new IdentityVerificationException(IdentityVerificationException.Reason.CONCORDANCE_MISMATCH,
                "Re-verification presented a ci/di pair inconsistent with a previously verified identity");
        }
    }

    private void publishConcordanceMismatchAudit(String providerName, String ci) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("providerName", providerName == null ? "" : providerName);
        meta.put("ci", ci);
        // IDV-CALLBACK-003 posture preserved: no rrn / residentRegistrationNumber /
        // 주민등록번호 key ever enters this audit metadata.
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise concordance-mismatch audit metadata (provider={})", providerName, ex);
            metadataJson = null;
        }
        try {
            auditLogService.record(AuditLogDto.builder()
                .action(CONCORDANCE_AUDIT_ACTION)
                .resourceType(RESOURCE_TYPE)
                .resourceId(ci)
                .outcome(AuditOutcome.FAILURE)
                .timestamp(Instant.now())
                .metadataJson(metadataJson)
                .build());
        } catch (RuntimeException ex) {
            log.error("AuditLog publish failed for IDV concordance mismatch (provider={})", providerName, ex);
        }
    }

    public void recordHmacFailure(String providerName) {
        publishAudit(providerName, AuditOutcome.FAILURE, "HMAC_FAIL", null);
    }

    public void recordUnknownProvider(String providerName) {
        publishAudit(providerName, AuditOutcome.FAILURE, "UNKNOWN_PROVIDER", null);
    }

    /**
     * IDV-ADMIN-001 — paginated VerifiedIdentity browse for ROLE_ADMIN.
     *
     * <p>Page/size are clamped to the catalog bounds, results are ordered by
     * {@code verifiedAt DESC}, and an optional {@code provider} filter narrows
     * by provider name. The response is mapped to the tight {@link Row} shape
     * (id / providerName / verifiedAt / name / dob) — CI and DI are correlation
     * tokens kept internal, never surfaced in the list view.
     *
     * <p>This read lives in the service so the admin controller never touches
     * the repository directly (layer-boundary invariant; PRACTICES-TEST-002).
     */
    @Transactional(readOnly = true)
    public PageResponse listAdmin(int page, int size, String provider) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "verifiedAt"));

        Page<VerifiedIdentity> result = (provider == null || provider.isBlank())
            ? repository.findAll(pageable)
            : repository.findAllByProviderName(provider, pageable);

        List<Row> rows = result.getContent().stream().map(Row::from).toList();
        return new PageResponse(
                rows,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    /** IDV-ADMIN-001 list row — tight projection, no CI / DI / RRN. */
    public record Row(UUID id, String providerName, Instant verifiedAt,
                      String name, String dob) {
        static Row from(VerifiedIdentity v) {
            return new Row(v.getId(), v.getProviderName(), v.getVerifiedAt(),
                           v.getName(), v.getDob());
        }
    }

    /** IDV-ADMIN-001 paginated response envelope. */
    public record PageResponse(List<Row> content, int page, int size,
                                long totalElements, int totalPages) {}

    private void publishAudit(String providerName, AuditOutcome outcome,
                               String outcomeDetail, String ci) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("providerName", providerName == null ? "" : providerName);
        meta.put("outcome", outcomeDetail);
        if (ci != null) {
            meta.put("ci", ci);
        }
        // IDV-AUDIT-001: structural invariant — no rrn / residentRegistrationNumber
        // / 주민등록번호 keys ever enter the audit metadata. The map literal above
        // is exhaustive; the violation-proof test guards this contract.

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise audit metadata for IDV callback "
                  + "(provider={}, outcome={})", providerName, outcomeDetail, ex);
            metadataJson = null;
        }

        // resource_id is non-nullable in the audit table — when no CI exists
        // (HMAC_FAIL / UNKNOWN_PROVIDER / EXTRACTION_FAIL) we use the literal
        // "(no-ci)" as a stable placeholder. The actual CI (or absence of it)
        // is in metadata.ci so dashboards can filter accurately.
        String resourceId = (ci != null && !ci.isBlank()) ? ci : "(no-ci)";

        try {
            auditLogService.record(AuditLogDto.builder()
                .action(AUDIT_ACTION)
                .resourceType(RESOURCE_TYPE)
                .resourceId(resourceId)
                .outcome(outcome)
                .timestamp(Instant.now())
                .metadataJson(metadataJson)
                .build());
        } catch (RuntimeException ex) {
            // Audit is best-effort — never break the callback flow because the
            // audit table is unavailable. Log the failure server-side so an
            // ops review can reconstruct.
            log.error("AuditLog publish failed for IDV callback "
                  + "(provider={}, outcome={})", providerName, outcomeDetail, ex);
        }
    }
}
