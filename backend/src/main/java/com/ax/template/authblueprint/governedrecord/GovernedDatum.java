package com.ax.template.authblueprint.governedrecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * attested-change-record-l0 governed datapoint. {@code value} is mutated ONLY via the package-private
 * {@link #setValueInternal} — there is NO public setter — so {@link GovernedRecordService} is the sole
 * mutator and every change flows through an appended {@link ChangeRecord} (ACR-ENVELOPE-001).
 * {@code name}/{@code createdBy}/{@code createdAt} immutable; {@code @Version} for atomic edits.
 */
@Entity
@Table(name = "governed_data")
public class GovernedDatum {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, updatable = false, unique = true, length = 200)
    private String name;

    // NB: column is "datum_value" — "value" is a reserved word in H2/SQL and breaks DDL.
    @Column(name = "datum_value", nullable = false, length = 2000)
    private String value;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GovernedDatum() {}

    public GovernedDatum(UUID id, String name, String value, String createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — package-private so only GovernedRecordService (same package) changes value,
     *  always after appending the attesting ChangeRecord. No public setter exists. */
    void setValueInternal(String next) {
        this.value = next;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public Long getVersion() { return version; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
