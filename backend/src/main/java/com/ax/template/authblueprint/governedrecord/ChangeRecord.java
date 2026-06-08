package com.ax.template.authblueprint.governedrecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * attested-change-record-l0 append-only change record (ACR-APPEND-ONLY-001 / 21 CFR 11.10(e):
 * "record changes shall not obscure previously recorded information"). EVERY column is
 * {@code @Column(updatable=false)}, there is no setter, and a row is never UPDATEd or deleted — a
 * correction is a NEW appended record. {@code sequence} is strictly monotonic per (datumId, fieldName),
 * allocated under the datum's row lock so causal order is unambiguous under concurrent edits.
 */
@AggregateMember(root = GovernedDatum.class)
@Entity
@Table(name = "governed_change_records",
    uniqueConstraints = @UniqueConstraint(name = "uq_governed_change_seq",
        columnNames = {"datum_id", "field_name", "sequence_no"}))
// ACR-ENVELOPE-001 / ACR-APPEND-ONLY-001 — DB backstops LIVE under ddl-auto: a non-blank reason and a
// positive monotonic sequence (the unique constraint above makes a duplicate per-field sequence
// unrepresentable, so a concurrent-edit sequence collision is caught at the DB even if app logic slips).
@Check(constraints = "sequence_no >= 1 AND LENGTH(TRIM(reason)) > 0")
public class ChangeRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "datum_id", nullable = false, updatable = false)
    private UUID datumId;

    @Column(name = "field_name", nullable = false, updatable = false, length = 120)
    private String fieldName;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    /** Pre-image — the field's value immediately before this edit (ACR-PREIMAGE-001). */
    @Column(name = "old_value", updatable = false, length = 2000)
    private String oldValue;

    @Column(name = "new_value", nullable = false, updatable = false, length = 2000)
    private String newValue;

    @Column(name = "reason", nullable = false, updatable = false, length = 1000)
    private String reason;

    /** Optional pinned controlled-vocabulary version (ACR-VOCAB-001); null for free-text reason. */
    @Column(name = "reason_vocab_version", updatable = false, length = 40)
    private String reasonVocabVersion;

    @Column(name = "actor", nullable = false, updatable = false, length = 255)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ChangeRecord() {}

    public ChangeRecord(UUID id, UUID datumId, String fieldName, long sequenceNo, String oldValue,
                        String newValue, String reason, String reasonVocabVersion, String actor, Instant occurredAt) {
        this.id = id;
        this.datumId = datumId;
        this.fieldName = fieldName;
        this.sequenceNo = sequenceNo;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.reasonVocabVersion = reasonVocabVersion;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getDatumId() { return datumId; }
    public String getFieldName() { return fieldName; }
    public long getSequenceNo() { return sequenceNo; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public String getReasonVocabVersion() { return reasonVocabVersion; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
