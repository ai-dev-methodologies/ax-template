package com.ax.template.authblueprint.copresence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * negative-copresence-gate-l0 subject — the entity whose member SET the gate evaluates against (e.g. a
 * patient, an actor with roles, a resource with reservations). The subject row is the LOCK ANCHOR:
 * activating a member takes this row under PESSIMISTIC_WRITE so the active-set read is in-transaction
 * and concurrent adds serialize (GATE-CONCURRENT-001). {@code subjectKey}/{@code createdAt} immutable;
 * no public setter. {@code @Version} backstops.
 */
@Entity
@Table(name = "copresence_subjects")
public class Subject {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_key", nullable = false, updatable = false, length = 200, unique = true)
    private String subjectKey;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Subject() {}

    public Subject(UUID id, String subjectKey, Instant createdAt) {
        this.id = id;
        this.subjectKey = subjectKey;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getSubjectKey() { return subjectKey; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
