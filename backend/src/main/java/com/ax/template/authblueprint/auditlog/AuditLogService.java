package com.ax.template.authblueprint.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.action;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.actorId;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.from;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.outcome;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.resourceId;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.resourceType;
import static com.ax.template.authblueprint.auditlog.AuditLogSpecifications.to;

/**
 * Application service for audit logs.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RECORD-002 — only {@code record()} and read methods are exposed; no update/delete API</li>
 *   <li>AUDIT-RECORD-003 — {@code record()} runs in a {@code REQUIRES_NEW} transaction so a write
 *       failure does not roll back the caller</li>
 *   <li>AUDIT-LIST-001 / AUDIT-LIST-002 — pagination + multi-filter combination</li>
 * </ul>
 */
@Service
public class AuditLogService {

    /** Manifest default page size (list_policy.default_page_size). */
    public static final int DEFAULT_PAGE_SIZE = 50;
    /** Manifest cap (list_policy.max_page_size). */
    public static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository repository;
    private final AuditLogPiiRedactor piiRedactor;

    public AuditLogService(AuditLogRepository repository, AuditLogPiiRedactor piiRedactor) {
        this.repository = repository;
        this.piiRedactor = piiRedactor;
    }

    /**
     * Record a new audit log entry in an isolated transaction.
     * <p>
     * Trace: AUDIT-RECORD-001 / AUDIT-RECORD-003.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(AuditLog entry) {
        return repository.save(entry);
    }

    /**
     * Record an audit entry from the published {@link AuditLogDto} port — the cross-feature
     * write surface (other features build a DTO instead of constructing the {@link AuditLog}
     * aggregate root directly; see DDD decomposition spec §6 / {@code HG-FEAT-ISOLATION}).
     * <p>
     * Runs in its own {@code REQUIRES_NEW} transaction and calls {@code repository.save}
     * directly — it must NOT delegate to {@link #record(AuditLog)}, because a self-invocation
     * bypasses the Spring proxy and would lose the isolated-transaction semantics.
     * <p>
     * Trace: AUDIT-RECORD-001 / AUDIT-RECORD-003 / AUDIT-PII-001 — {@code actorIp} is
     * redacted through the SAME {@link AuditLogPiiRedactor} the {@code @Audited}
     * aspect uses, so cross-feature callers using this published port get the
     * identical PII posture as the aspect-driven write path (S2.AUDIT-PII.XB closure).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(AuditLogDto dto) {
        AuditLog.Builder b = AuditLog.builder()
            .actorUserId(dto.actorUserId())
            .actorIp(piiRedactor.redactIp(dto.actorIp()))
            .action(dto.action())
            .resourceType(dto.resourceType())
            .resourceId(dto.resourceId())
            .correlationId(dto.correlationId())
            .userAgent(dto.userAgent())
            .metadataJson(dto.metadataJson());
        // Unset id/outcome/timestamp fall through to the entity builder's defaults
        // (random id / SUCCESS / now), preserving the prior caller behaviour exactly.
        if (dto.id() != null) {
            b.id(dto.id());
        }
        if (dto.outcome() != null) {
            b.outcome(dto.outcome());
        }
        if (dto.timestamp() != null) {
            b.timestamp(dto.timestamp());
        }
        return repository.save(b.build());
    }

    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public AuditLogPage list(AuditLogFilter filter, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize,
            Sort.by(Sort.Direction.DESC, "timestamp"));

        Specification<AuditLog> spec = Specification
            .where(actorId(filter.actorId()))
            .and(resourceType(filter.resourceType()))
            .and(resourceId(filter.resourceId()))
            .and(action(filter.action()))
            .and(outcome(filter.outcome()))
            .and(from(filter.from()))
            .and(to(filter.to()));

        Page<AuditLog> result = repository.findAll(spec, pageable);
        return new AuditLogPage(
            result.map(AuditLogResponse::from).getContent(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize()
        );
    }

    /**
     * Delete entries older than the cutoff.
     * <p>
     * Trace: AUDIT-RETENTION-001 — the single delete path in the catalog.
     * Only invoked from {@link AuditLogRetentionJob}.
     */
    @Transactional
    public int purgeOlderThan(Instant cutoff) {
        return repository.deleteByTimestampBefore(cutoff);
    }

    /** Filter criteria for the list / export query. */
    public record AuditLogFilter(
        String actorId,
        String resourceType,
        String resourceId,
        String action,
        AuditOutcome outcome,
        Instant from,
        Instant to
    ) {
        public static AuditLogFilter empty() {
            return new AuditLogFilter(null, null, null, null, null, null, null);
        }
    }
}
