package com.ax.template.authblueprint.dsr;

import com.ax.template.authblueprint.auditlog.AuditLogDto;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import com.ax.template.authblueprint.auditlog.AuditOutcome;
import com.ax.template.authblueprint.common.AuditPiiHelper;
import com.ax.template.authblueprint.common.CallerScope;
import com.ax.template.authblueprint.common.ResourceNotFoundException;
import com.ax.template.authblueprint.dsr.DsrDtos.AccessBundle;
import com.ax.template.authblueprint.dsr.DsrDtos.DsrRequestResponse;
import com.ax.template.authblueprint.dsr.DsrDtos.ErasureManifest;
import com.ax.template.authblueprint.dsr.DsrDtos.RectifyRequest;
import com.ax.template.authblueprint.dsr.PersonalDataProvider.RetainedCategory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sole orchestrator for the data-subject-rights domain. ALL repository access,
 * RBAC (caller = {@link Authentication#getName()}, never a path/body param), and
 * IDOR-safe 404s live here. The controller is thin and never touches the repository.
 *
 * <p>Trace: DSR-ACCESS-001 / RECTIFY-001 / ERASURE-001 / PORTABILITY-001 /
 * RESTRICT-001 / SLA-001 / OBSERVABILITY-001 (specs/data-subject-rights-l0.yaml).
 */
@Service
public class DsrService {

    /** DSR-SLA-001 — GDPR Art 12(3) one-month baseline. */
    static final int SLA_BASELINE_DAYS = 30;
    /** DSR-SLA-001 — extendable by up to two further months. */
    static final int SLA_MAX_EXTENSION_DAYS = 60;

    private static final String AUDIT_RESOURCE_TYPE = "DSR_REQUEST";

    private final DsrRequestRepository repository;
    private final DsrRequestStateMachine stateMachine;
    private final DsrRestrictionGate restrictionGate;
    private final DsrMetrics metrics;
    private final AuditLogService auditLogService;
    private final List<PersonalDataProvider> providers;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public DsrService(DsrRequestRepository repository,
                      DsrRequestStateMachine stateMachine,
                      DsrRestrictionGate restrictionGate,
                      DsrMetrics metrics,
                      AuditLogService auditLogService,
                      List<PersonalDataProvider> providers,
                      Clock clock,
                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.restrictionGate = restrictionGate;
        this.metrics = metrics;
        this.auditLogService = auditLogService;
        this.providers = providers;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    // ── DSR-ACCESS-001 ────────────────────────────────────────────────────────

    /**
     * Open a subject-access request and aggregate personal data across every
     * registered {@link PersonalDataProvider}. Re-requesting while one is in flight
     * → 409 DSR_ACCESS_IN_FLIGHT.
     */
    @Transactional
    public AccessBundle openAccess(Authentication auth) {
        String subject = auth.getName();
        // DSR-RESTRICT-001 fail-closed: a subject-access request is non-storage
        // processing — block it (423) while the subject is restricted, consistent
        // with the rectify/portability gates. Subject can self-lift then re-access.
        restrictionGate.checkProcessingAllowed(subject);
        requireNoInFlight(subject, DsrRequestType.ACCESS);

        DsrRequest req = open(subject, DsrRequestType.ACCESS);

        Map<String, Map<String, Object>> modules = new LinkedHashMap<>();
        for (PersonalDataProvider p : providers) {
            modules.put(p.moduleName(), new LinkedHashMap<>(p.collect(subject)));
        }
        audit(subject, "DSR_ACCESS_OPEN", req.getId(), AuditOutcome.SUCCESS);
        return new AccessBundle(DsrRequestResponse.from(req), modules);
    }

    // ── DSR-RECTIFY-001 ───────────────────────────────────────────────────────

    /**
     * Field-level rectification. Field outside the recipe-declared allowlist → 422
     * DSR_FIELD_NOT_RECTIFIABLE; current_value mismatch → 409 DSR_RECTIFY_STALE.
     * An accepted rectification writes a before/after audit record.
     */
    @Transactional
    public DsrRequestResponse rectify(Authentication auth, RectifyRequest body) {
        String subject = auth.getName();
        restrictionGate.checkProcessingAllowed(subject);

        if (!rectifiableFields().contains(body.fieldPath())) {
            throw DsrException.fieldNotRectifiable(body.fieldPath());
        }
        String stored = currentStoredValue(subject, body.fieldPath());
        if (stored == null || !stored.equals(body.currentValue())) {
            throw DsrException.rectifyStale();
        }

        DsrRequest req = open(subject, DsrRequestType.RECTIFY);
        // before/after audit (values themselves are personal data → store a
        // non-recoverable correlation hash rather than raw values).
        String metadata = "{\"field\":\"" + body.fieldPath()
            + "\",\"before_hash\":\"" + AuditPiiHelper.piiHash(body.currentValue())
            + "\",\"after_hash\":\"" + AuditPiiHelper.piiHash(body.correctedValue()) + "\"}";
        auditWithMetadata(subject, "DSR_RECTIFY", req.getId(), AuditOutcome.SUCCESS, metadata);
        close(req, DsrRequestType.RECTIFY);
        return DsrRequestResponse.from(req);
    }

    // ── DSR-ERASURE-001 ───────────────────────────────────────────────────────

    /**
     * Erasure (soft-delete → purge) across modules. Idempotent: a re-request on an
     * already-erased subject returns the prior manifest, never 500. A module under
     * legal-hold yields a partial-erasure manifest (retained categories + basis).
     */
    @Transactional
    public ErasureManifest erase(Authentication auth) {
        String subject = auth.getName();
        // DSR-RESTRICT-001 fail-closed: erasure is a write/processing operation —
        // a restricted subject MUST lift first (423), otherwise restriction (retain)
        // and erasure (delete) silently conflict.
        restrictionGate.checkProcessingAllowed(subject);

        // Idempotency (DSR-ERASURE-001): a prior terminal erasure returns its STORED
        // manifest verbatim — we never re-run provider erase() on a re-request (no
        // re-mutation / concurrency churn, and the manifest is identical, never 500).
        Optional<DsrRequest> prior = repository
            .findBySubjectIdOrderByReceivedAtDesc(subject, PageRequest.of(0, 50))
            .stream()
            .filter(r -> r.getType() == DsrRequestType.ERASURE && r.getErasureManifestJson() != null)
            .findFirst();
        if (prior.isPresent()) {
            return readManifest(prior.get().getErasureManifestJson());
        }

        // New erasure: soft-delete → purge across modules, collecting any legal-hold
        // retained categories (GDPR Art 17(3) partial-erasure manifest).
        DsrRequest req = open(subject, DsrRequestType.ERASURE);
        List<ErasureManifest.RetainedCategory> retained = new ArrayList<>();
        for (PersonalDataProvider p : providers) {
            for (RetainedCategory rc : p.erase(subject)) {
                retained.add(new ErasureManifest.RetainedCategory(rc.category(), rc.legalBasis()));
            }
        }
        boolean fullyErased = retained.isEmpty();
        String legalBasis = fullyErased ? "data_subject_request" : "partial_legal_hold";
        close(req, DsrRequestType.ERASURE);

        ErasureManifest manifest = new ErasureManifest(
            req.getId(), req.getClosedAt(), legalBasis, fullyErased, retained);
        // Persist the manifest so a future re-request returns it verbatim (idempotency).
        req.setErasureManifestJson(writeManifest(manifest));
        repository.save(req);
        audit(subject, "DSR_ERASURE", req.getId(), AuditOutcome.SUCCESS);
        return manifest;
    }

    private String writeManifest(ErasureManifest manifest) {
        try {
            return objectMapper.writeValueAsString(manifest);
        } catch (JacksonException e) {
            throw new IllegalStateException("DSR erasure manifest serialization failed", e);
        }
    }

    private ErasureManifest readManifest(String json) {
        try {
            return objectMapper.readValue(json, ErasureManifest.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("DSR erasure manifest deserialization failed", e);
        }
    }

    // ── DSR-PORTABILITY-001 ───────────────────────────────────────────────────

    /**
     * Portability export. Unsupported format → 400 DSR_PORTABILITY_FORMAT_INVALID.
     * Returns the subject-provided data only (derived {@code riskScore}-style fields
     * are excluded by the caller's column projection in a fork-receiver; here the
     * demo provider's map is returned and the controller pins a schema_version).
     */
    @Transactional
    public AccessBundle producePortableCopy(Authentication auth, String format) {
        String subject = auth.getName();
        // DSR-RESTRICT-001 fail-closed: the restriction gate MUST precede format
        // validation — a restricted subject sending a bad format must see 423
        // (security-enforced), not a 400 that leaks the check order.
        restrictionGate.checkProcessingAllowed(subject);
        // Locale.ROOT: locale-independent fold for the format lookup (a default-locale toLowerCase()
        // mis-maps ASCII under some locales, e.g. Turkish 'I'→'ı').
        String fmt = (format == null || format.isBlank()) ? "json" : format.toLowerCase(Locale.ROOT);
        if (!fmt.equals("json") && !fmt.equals("csv")) {
            throw DsrException.portabilityFormatInvalid(format);
        }

        DsrRequest req = open(subject, DsrRequestType.PORTABILITY);
        Map<String, Map<String, Object>> modules = new LinkedHashMap<>();
        for (PersonalDataProvider p : providers) {
            modules.put(p.moduleName(), new LinkedHashMap<>(p.collect(subject)));
        }
        audit(subject, "DSR_PORTABILITY", req.getId(), AuditOutcome.SUCCESS);
        close(req, DsrRequestType.PORTABILITY);
        return new AccessBundle(DsrRequestResponse.from(req), modules);
    }

    // ── DSR-RESTRICT-001 ──────────────────────────────────────────────────────

    /** Freeze processing without deletion. */
    @Transactional
    public DsrRequestResponse restrict(Authentication auth) {
        String subject = auth.getName();
        restrictionGate.restrict(subject);
        DsrRequest req = open(subject, DsrRequestType.RESTRICT);
        audit(subject, "DSR_RESTRICT", req.getId(), AuditOutcome.SUCCESS);
        return DsrRequestResponse.from(req);
    }

    /** Lift a restriction; writes an audit record. */
    @Transactional
    public DsrRequestResponse lift(Authentication auth, String justification) {
        String subject = auth.getName();
        restrictionGate.lift(subject);
        DsrRequest req = open(subject, DsrRequestType.RESTRICT);
        String metadata = "{\"action\":\"lift\",\"justification_hash\":\""
            + AuditPiiHelper.piiHash(justification) + "\"}";
        auditWithMetadata(subject, "DSR_RESTRICT_LIFT", req.getId(), AuditOutcome.SUCCESS, metadata);
        close(req, DsrRequestType.RESTRICT);
        return DsrRequestResponse.from(req);
    }

    // ── DSR-SLA-001 ───────────────────────────────────────────────────────────

    /** Owner-scoped (IDOR-safe 404) request lookup. Admin sees any subject's request. */
    @Transactional(readOnly = true)
    public DsrRequestResponse get(Authentication auth, UUID requestId) {
        CallerScope caller = CallerScope.of(auth);
        DsrRequest req = caller.isAdmin()
            ? repository.findById(requestId).orElseThrow(ResourceNotFoundException::new)
            : repository.findByIdAndSubjectId(requestId, caller.userId())
                .orElseThrow(ResourceNotFoundException::new);
        return DsrRequestResponse.from(req);
    }

    /** Extend a single request's due date by ≤ 60 further days with a reason. */
    @Transactional
    public DsrRequestResponse extend(Authentication auth, UUID requestId, int days, String reason) {
        String subject = auth.getName();
        DsrRequest req = repository.findByIdAndSubjectId(requestId, subject)
            .orElseThrow(ResourceNotFoundException::new);
        int requested = req.getExtensionDays() + days;
        int capped = Math.min(Math.max(requested, 0), SLA_MAX_EXTENSION_DAYS);
        req.setExtensionDays(capped);
        req.setExtensionReason(reason);
        req.setDueAt(req.getReceivedAt().plus(SLA_BASELINE_DAYS + capped, ChronoUnit.DAYS));
        return DsrRequestResponse.from(repository.save(req));
    }

    /**
     * Flag every still-open request at/over its due date as SLA-breaching. Invoked
     * by {@link DsrSlaSweeper} (@Scheduled) and directly by tests. Returns the count
     * newly flagged.
     */
    @Transactional
    public int sweepSlaBreaches() {
        Instant now = Instant.now(clock);
        Pageable page = PageRequest.of(0, 500);
        int flagged = 0;
        for (DsrRequest req : repository
                .findByStatusNotAndDueAtLessThanEqual(DsrRequestStatus.CLOSED, now, page)) {
            if (!req.isSlaBreached()) {
                req.setSlaBreached(true);
                repository.save(req);
                metrics.recordSlaBreach(req.getType());
                flagged++;
            }
        }
        return flagged;
    }

    // ── shared internals ──────────────────────────────────────────────────────

    private void requireNoInFlight(String subject, DsrRequestType type) {
        repository.findFirstBySubjectIdAndTypeAndStatusNot(subject, type, DsrRequestStatus.CLOSED)
            .ifPresent(r -> { throw DsrException.accessInFlight(); });
    }

    private DsrRequest open(String subject, DsrRequestType type) {
        Instant now = Instant.now(clock);
        DsrRequest req = DsrRequest.builder()
            .subjectId(subject)
            .type(type)
            .status(DsrRequestStatus.RECEIVED)
            .receivedAt(now)
            .dueAt(now.plus(SLA_BASELINE_DAYS, ChronoUnit.DAYS))
            .build();
        DsrRequest saved = repository.save(req);
        metrics.recordRequest(type);
        return saved;
    }

    private void close(DsrRequest req, DsrRequestType type) {
        stateMachine.markClosed(req);
        repository.save(req);
        metrics.recordProcessingTime(type, Duration.between(req.getReceivedAt(), req.getClosedAt()));
    }

    private List<String> rectifiableFields() {
        List<String> all = new ArrayList<>();
        for (PersonalDataProvider p : providers) {
            all.addAll(p.rectifiableFields());
        }
        return all;
    }

    /**
     * Resolve the subject's current value for a rectifiable field path
     * ({@code module.field}). Reads from the owning provider's export map.
     */
    private String currentStoredValue(String subject, String fieldPath) {
        int dot = fieldPath.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String module = fieldPath.substring(0, dot);
        String field = fieldPath.substring(dot + 1);
        for (PersonalDataProvider p : providers) {
            if (p.moduleName().equals(module)) {
                Object v = p.collect(subject).get(field);
                return v == null ? null : String.valueOf(v);
            }
        }
        return null;
    }

    private void audit(String subject, String action, UUID requestId, AuditOutcome outcome) {
        auditWithMetadata(subject, action, requestId, outcome, null);
    }

    private void auditWithMetadata(String subject, String action, UUID requestId,
                                   AuditOutcome outcome, String metadataJson) {
        try {
            auditLogService.record(AuditLogDto.builder()
                .actorUserId(AuditPiiHelper.piiHash(subject))
                .action(action)
                .resourceType(AUDIT_RESOURCE_TYPE)
                .resourceId(requestId.toString())
                .outcome(outcome)
                .timestamp(Instant.now(clock))
                .metadataJson(metadataJson)
                .build());
        } catch (RuntimeException ex) {
            // Audit is best-effort; never break the DSR flow because the audit
            // table is unavailable.
        }
    }
}
