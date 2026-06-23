package com.ax.template.authblueprint.sensitiveaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * sensitive-read-audit-l0 root: one record carrying a governed sensitive datum (SENSITIVE-READ-001).
 * The {@code rawValue} column is the @SensitiveField — reading it through {@link SensitiveAccessService}
 * is an audited event that appends an immutable {@link SensitiveAccessLog} row in the same transaction
 * before the value is returned. The default (non-privileged) projection masks the value; the raw value
 * is reached ONLY via the audited reveal path (SENSITIVE-MASK-001). The owner records who custodies the
 * datum. The record is created once; the sensitive value is immutable (updatable=false) — there is NO
 * delete path anywhere in this domain (a sensitive record is closed, never silently removed).
 */
@AggregateRoot
@Entity
@Table(name = "sensitive_records")
public class SensitiveRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Opaque external reference for the record (account/subject id) — recorded verbatim, immutable. */
    @Column(name = "record_ref", nullable = false, updatable = false, length = 200)
    private String recordRef;

    /** The audit field name recorded on each access-log row (what is being read). */
    @Column(name = "field_name", nullable = false, updatable = false, length = 100)
    private String fieldName;

    /**
     * The governed sensitive datum. {@link SensitiveField}-tagged: reading it through the service
     * is an audited event. Immutable once recorded. The default projection NEVER returns this raw
     * value — only the masked form (SENSITIVE-MASK-001).
     */
    @SensitiveField("rawValue")
    @Column(name = "raw_value", nullable = false, updatable = false, length = 500)
    private String rawValue;

    /** The custodian (the Authentication principal that recorded the datum). */
    @Column(name = "owner", nullable = false, updatable = false, length = 200)
    private String owner;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SensitiveRecord() {}

    public SensitiveRecord(UUID id, String recordRef, String fieldName, String rawValue,
                           String owner, Instant createdAt) {
        this.id = id;
        this.recordRef = recordRef;
        this.fieldName = fieldName;
        this.rawValue = rawValue;
        this.owner = owner;
        this.createdAt = createdAt;
    }

    /**
     * The masked, non-privileged projection of the sensitive value (SENSITIVE-MASK-001): the last
     * 4 characters preserved, everything before fixed-asterisked. A bare GET returns ONLY this — the
     * raw value is reached only through the audited reveal path. Pure / no side effect.
     */
    public String maskedValue() {
        String v = this.rawValue;
        if (v == null || v.length() <= 4) {
            return "****";
        }
        return "****" + v.substring(v.length() - 4);
    }

    public UUID getId() { return id; }
    public String getRecordRef() { return recordRef; }
    public String getFieldName() { return fieldName; }

    /** The RAW sensitive value (SENSITIVE-READ-001). Reached ONLY via the audited service reveal
     *  path — never serialized into the default projection. */
    @SensitiveField("rawValue")
    public String getRawValue() { return rawValue; }

    public String getOwner() { return owner; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
