/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogService
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-RECORD-001
 *               specs/audit-log-l0.yaml#AUDIT-RECORD-003
 *               specs/audit-log-l0.yaml#AUDIT-LIST-001
 *               specs/audit-log-l0.yaml#AUDIT-LIST-002
 *               specs/audit-log-l0.yaml#AUDIT-RETENTION-001
 *               specs/audit-log-l0.yaml#AUDIT-RETENTION-003
 *               specs/audit-log-l0.yaml#AUDIT-PII-001
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — Transaction propagation REQUIRES_NEW for independent transactions"
 *     url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
 *   - source_type: external
 *     citation: "Spring Reference — @Scheduled for cron-based retention jobs"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   record() must be called from AuditLoggingAspect — never directly from controllers.
 *   purgeExpired() is driven by the @Scheduled cron and should not be called from API code.
 */
package com.example.app.auditlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * AuditLogService — application service for the audit-log domain.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Record new entries (AUDIT-RECORD-001, AUDIT-RECORD-003).
 *   <li>List/filter entries with pagination (AUDIT-LIST-001, AUDIT-LIST-002).
 *   <li>Retrieve a single entry by ID (AUDIT-LIST-001).
 *   <li>Scheduled retention purge (AUDIT-RETENTION-001, AUDIT-RETENTION-003).
 *   <li>PII redaction before persistence (AUDIT-PII-001).
 * </ul>
 *
 * <p>Non-blocking guarantee (AUDIT-RECORD-003):
 * {@code record()} uses {@code Propagation.REQUIRES_NEW} so that an audit persistence
 * failure is isolated from the caller's transaction. The aspect catches exceptions
 * from this service and logs them at ERROR level — the originating operation proceeds.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogPiiRedactor piiRedactor;

    @Value("${audit.retention.default-days:90}")
    private int defaultRetentionDays;

    @Value("${audit.retention.warning-threshold:10000}")
    private int retentionWarningThreshold;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           AuditLogPiiRedactor piiRedactor) {
        this.auditLogRepository = auditLogRepository;
        this.piiRedactor = piiRedactor;
    }

    // ─── Record ──────────────────────────────────────────────────────────────

    /**
     * Persists a new audit log entry in an independent transaction.
     *
     * <p>REQUIRES_NEW ensures that a persistence failure does not roll back the
     * caller's business transaction (AUDIT-RECORD-003).
     *
     * <p>PII fields are redacted before persisting (AUDIT-PII-001).
     *
     * @param entry a fully-populated {@link AuditLog} built by the aspect
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog entry) {
        AuditLog redacted = piiRedactor.redact(entry);
        auditLogRepository.save(redacted);
        log.debug("Audit recorded: actor={} action={} resource={}/{} outcome={}",
            redacted.getActorId(), redacted.getAction(),
            redacted.getResourceType(), redacted.getResourceId(),
            redacted.getOutcome());
    }

    // ─── List / Get ──────────────────────────────────────────────────────────

    /**
     * Returns a paginated, filtered list of audit log entries (AUDIT-LIST-001, AUDIT-LIST-002).
     *
     * @param query  filter + pagination parameters
     * @return page of {@link AuditLogDto.Summary}
     */
    @Transactional(readOnly = true)
    public AuditLogDto.Page<AuditLogDto.Summary> listAuditLogs(AuditLogQueryDto query) {
        Specification<AuditLog> spec = AuditLogSpecifications.fromQuery(query);
        PageRequest pageable = PageRequest.of(
            query.effectivePage(),
            query.effectiveSize(),
            Sort.by(Sort.Direction.DESC, "timestamp")
        );
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        Page<AuditLogDto.Summary> summaries = page.map(AuditLogDto.Summary::from);
        return AuditLogDto.Page.from(summaries);
    }

    /**
     * Returns the full detail of a single audit log entry by ID (AUDIT-LIST-001).
     *
     * @param id the audit log entry UUID
     * @return detail DTO, or empty if not found
     */
    @Transactional(readOnly = true)
    public java.util.Optional<AuditLogDto.Detail> getAuditLog(UUID id) {
        return auditLogRepository.findById(id).map(AuditLogDto.Detail::from);
    }

    // ─── Retention ───────────────────────────────────────────────────────────

    /**
     * Scheduled retention purge job (AUDIT-RETENTION-001, AUDIT-RETENTION-003).
     *
     * <p>Deletes entries older than {@code audit.retention.default-days} (default: 90 days).
     * Runs on a configurable cron schedule (default: daily at 02:00 UTC).
     *
     * <p>Emits WARN log if the deleted count exceeds {@code audit.retention.warning-threshold}.
     */
    @Scheduled(cron = "${audit.retention.cron:0 0 2 * * *}")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(defaultRetentionDays, ChronoUnit.DAYS);
        int deleted = auditLogRepository.deleteByTimestampBefore(cutoff);
        if (deleted >= retentionWarningThreshold) {
            log.warn("Audit retention purge deleted {} records (threshold={}). "
                + "Consider reducing retention period or increasing log volume capacity.",
                deleted, retentionWarningThreshold);
        } else {
            log.info("Audit retention purge complete: deleted={} cutoff={}", deleted, cutoff);
        }
    }
}
