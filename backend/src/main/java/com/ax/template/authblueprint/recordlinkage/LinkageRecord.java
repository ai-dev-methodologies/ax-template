package com.ax.template.authblueprint.recordlinkage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * record-linkage-l0 identity record. A MERGED record keeps its values verbatim and points
 * forward to its survivor — there is NO delete path anywhere in the domain
 * (LINK-SURVIVOR/RESOLVE-001). Lifecycle and survivorship application move only via the
 * package-private hooks, called by {@link LinkageService} under the record's row lock.
 */
@AggregateRoot
@Entity
@Table(name = "linkage_records")
@Check(constraints = "status <> 'MERGED' OR merged_into_id IS NOT NULL")
public class LinkageRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "identifier", length = 100)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status;

    /** Forward pointer — set exactly when this record loses a merge (LINK-RESOLVE-001). */
    @Column(name = "merged_into_id")
    private UUID mergedIntoId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LinkageRecord() {}

    public LinkageRecord(UUID id, String fullName, LocalDate birthDate, String identifier, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.identifier = identifier;
        this.status = RecordStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — tombstone as the merge's loser: values retained, pointer set. */
    void tombstone(UUID survivorId) {
        this.status = RecordStatus.MERGED;
        this.mergedIntoId = survivorId;
    }

    /** Sole-mutator hook — apply one survivorship-winning value onto the survivor. */
    void applySurvivorship(String field, String value) {
        switch (field) {
            case "fullName" -> this.fullName = value;
            case "birthDate" -> this.birthDate = value == null ? null : LocalDate.parse(value);
            case "identifier" -> this.identifier = value;
            default -> throw new IllegalArgumentException("unknown identity field: " + field);
        }
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getIdentifier() { return identifier; }
    public RecordStatus getStatus() { return status; }
    public UUID getMergedIntoId() { return mergedIntoId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
