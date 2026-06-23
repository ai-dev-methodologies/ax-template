package com.ax.template.authblueprint.sensitiveaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One IMMUTABLE sensitive-read audit record (SENSITIVE-READ-001): the WHO/WHEN/WHAT/WHY that
 * NIST SP 800-53 AU-3 demands of an audit record — the accessor (the Authentication principal),
 * the occurredAt instant (from the injected Clock), the recordRef + fieldName read, and the stated
 * purpose (AU-2 rationale). Appended by {@link SensitiveAccessService} in the SAME transaction as,
 * and BEFORE, the raw value is returned — a reveal without this row is unrepresentable. Every
 * column is {@code @Column(updatable=false)}; the log is append-only with NO delete path.
 */
@AggregateMember(root = SensitiveRecord.class)
@Entity
@Table(name = "sensitive_access_logs")
public class SensitiveAccessLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    /** WHAT — the external record reference whose value was read. */
    @Column(name = "record_ref", nullable = false, updatable = false, length = 200)
    private String recordRef;

    /** WHAT — the sensitive field that was read. */
    @Column(name = "field_name", nullable = false, updatable = false, length = 100)
    private String fieldName;

    /** WHO — the Authentication principal that accessed the raw value (never a body/path param). */
    @Column(name = "accessor", nullable = false, updatable = false, length = 200)
    private String accessor;

    /** WHY — the stated, non-blank purpose for the access (AU-2 rationale; SENSITIVE-PURPOSE-001). */
    @Column(name = "purpose", nullable = false, updatable = false, length = 500)
    private String purpose;

    /** WHEN — the recorded access instant from the injected Clock. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SensitiveAccessLog() {}

    public SensitiveAccessLog(UUID id, UUID recordId, String recordRef, String fieldName,
                              String accessor, String purpose, Instant occurredAt) {
        this.id = id;
        this.recordId = recordId;
        this.recordRef = recordRef;
        this.fieldName = fieldName;
        this.accessor = accessor;
        this.purpose = purpose;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getRecordId() { return recordId; }
    public String getRecordRef() { return recordRef; }
    public String getFieldName() { return fieldName; }
    public String getAccessor() { return accessor; }
    public String getPurpose() { return purpose; }
    public Instant getOccurredAt() { return occurredAt; }
}
