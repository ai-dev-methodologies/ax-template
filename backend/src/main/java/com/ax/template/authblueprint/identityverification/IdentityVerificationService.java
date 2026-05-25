package com.ax.template.authblueprint.identityverification;

import com.ax.template.authblueprint.auditlog.AuditLog;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import com.ax.template.authblueprint.auditlog.AuditOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R54 — sole mutator for the identity-verification domain.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>IDV-PROVIDER-002 — resolve adapter by provider name; unknown → throw.</li>
 *   <li>IDV-CALLBACK-002 — extract via adapter, persist {@link VerifiedIdentity}.</li>
 *   <li>IDV-AUDIT-001 — every callback attempt (SUCCESS / HMAC_FAIL /
 *       EXTRACTION_FAIL / UNKNOWN_PROVIDER) publishes an {@link AuditLog} entry
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
            VerifiedIdentity saved = repository.save(VerifiedIdentity.create(data));
            publishAudit(providerName, AuditOutcome.SUCCESS, "SUCCESS", data.ci());
            return saved;
        } catch (IdentityVerificationException ex) {
            publishAudit(providerName, AuditOutcome.FAILURE, ex.reason().name(), null);
            throw ex;
        }
    }

    public void recordHmacFailure(String providerName) {
        publishAudit(providerName, AuditOutcome.FAILURE, "HMAC_FAIL", null);
    }

    public void recordUnknownProvider(String providerName) {
        publishAudit(providerName, AuditOutcome.FAILURE, "UNKNOWN_PROVIDER", null);
    }

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
            auditLogService.record(AuditLog.builder()
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
