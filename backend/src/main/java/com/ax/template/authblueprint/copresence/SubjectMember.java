package com.ax.template.authblueprint.copresence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * negative-copresence-gate-l0 member of a subject's set. {@code concept} is the NORMALIZED concept key
 * the gate intersects on (a drug/allergen/role class, not the literal {@code label}). A member is
 * activated ONLY through {@link CopresenceService} (the sole gate); there is no public setter, so a
 * write cannot bypass the contraindication check. {@code overrideReason}/{@code overriddenFindings}
 * record a GATE-OVERRIDE-001 proceed-past-a-RELATIVE-finding, by reference, bound to this row.
 */
@Entity
@Table(name = "copresence_members")
public class SubjectMember {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "concept", nullable = false, updatable = false, length = 200)
    private String concept;

    @Column(name = "label", nullable = false, updatable = false, length = 400)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    /** Non-null iff this member was admitted by overriding RELATIVE finding(s) (GATE-OVERRIDE-001). */
    @Column(name = "override_reason", updatable = false, length = 1000)
    private String overrideReason;

    /** The overridden findings recorded BY REFERENCE (e.g. "RELATIVE:concept-x"); null when none. */
    @Column(name = "overridden_findings", updatable = false, length = 2000)
    private String overriddenFindings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SubjectMember() {}

    public SubjectMember(UUID id, UUID subjectId, String concept, String label, MemberStatus status,
                         String overrideReason, String overriddenFindings, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.concept = concept;
        this.label = label;
        this.status = status;
        this.overrideReason = overrideReason;
        this.overriddenFindings = overriddenFindings;
        this.createdAt = createdAt;
    }

    /** Leave the active set (so it no longer participates in future gate evaluations). Sole-mutator. */
    void markRemoved() {
        this.status = MemberStatus.REMOVED;
    }

    public UUID getId() { return id; }
    public UUID getSubjectId() { return subjectId; }
    public String getConcept() { return concept; }
    public String getLabel() { return label; }
    public MemberStatus getStatus() { return status; }
    public String getOverrideReason() { return overrideReason; }
    public String getOverriddenFindings() { return overriddenFindings; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
