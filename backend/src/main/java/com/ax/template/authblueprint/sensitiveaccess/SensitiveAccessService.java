package com.ax.template.authblueprint.sensitiveaccess;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * sensitive-read-audit-l0 sole orchestrator. The primitive (SENSITIVE-READ-001): reading the raw
 * value of a {@link SensitiveField}-tagged datum is itself an audited event — {@link #reveal} writes
 * an immutable {@link SensitiveAccessLog} row (who/when/what/why per AU-3) in the SAME transaction
 * as, and BEFORE, it returns the raw value. The reveal and the record are one unit: if the log write
 * fails the transaction rolls back, so a reveal-without-record is unrepresentable. The default
 * projection ({@link #get}, masked) writes NO access-log row (SENSITIVE-MASK-001); only {@link #reveal}
 * does. A reveal MUST state a non-blank purpose (SENSITIVE-PURPOSE-001). The trail is admin-queryable
 * ({@link #accessLog}) and append-only — no delete path exists. Log rows are members:
 * {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class SensitiveAccessService {

    /** Reveal records a structured REVEAL event — the AU-3 "what type of event occurred". */
    static final int ACCESS_LOG_PAGE_CAP = 500;

    private final SensitiveRecordRepository records;
    private final MemberWriter members;
    private final SensitiveAccessMetrics metrics;
    private final Clock clock;

    public SensitiveAccessService(SensitiveRecordRepository records, MemberWriter members,
                                  SensitiveAccessMetrics metrics, Clock clock) {
        this.records = records;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public SensitiveRecord record(String recordRef, String fieldName, String rawValue, String owner) {
        SensitiveRecord r = new SensitiveRecord(UUID.randomUUID(), recordRef, fieldName, rawValue,
            owner, Instant.now(clock));
        SensitiveRecord saved = records.save(r);
        metrics.record("record", "ok");
        return saved;
    }

    /** SENSITIVE-MASK-001 — the non-privileged projection. NO access-log row is written: a masked
     *  read is not a sensitive read. */
    @Transactional(readOnly = true)
    public SensitiveRecord get(UUID recordId) {
        SensitiveRecord r = records.findById(recordId).orElseThrow(SensitiveAccessException::notFound);
        metrics.record("view", "ok");
        return r;
    }

    /**
     * SENSITIVE-READ/PURPOSE-001 — the audited reveal. Validates a non-blank purpose, appends an
     * immutable access-log row (accessor + occurredAt + recordRef + fieldName + purpose) via the
     * member writer, flushes it, and ONLY THEN returns the raw value. The write and the read share
     * this single transaction — a reveal that returns the value without recording the access cannot
     * happen (the record-before-return is the keystone the catalog claims).
     */
    @Transactional
    public String reveal(UUID recordId, String accessor, String purpose) {
        if (purpose == null || purpose.isBlank()) {
            metrics.record("reveal", "no_purpose");
            throw SensitiveAccessException.purposeRequired();         // 422 — no row, no value
        }
        SensitiveRecord r = records.findById(recordId).orElseThrow(SensitiveAccessException::notFound);
        Instant now = Instant.now(clock);
        // RECORD BEFORE RETURN — the access row is written (and flushed) before the raw value leaves.
        members.persistAndFlush(new SensitiveAccessLog(UUID.randomUUID(), r.getId(), r.getRecordRef(),
            r.getFieldName(), accessor, purpose.strip(), now));
        metrics.record("reveal", "recorded");
        return r.getRawValue();
    }

    /** SENSITIVE-QUERY-001 — the admin-only append-only access trail for one record. */
    @Transactional(readOnly = true)
    public List<SensitiveAccessLog> accessLog(UUID recordId) {
        get(recordId);                                                // 404 before an empty list
        metrics.record("query", "ok");
        return records.findAccessLog(recordId, PageRequest.of(0, ACCESS_LOG_PAGE_CAP));
    }
}
