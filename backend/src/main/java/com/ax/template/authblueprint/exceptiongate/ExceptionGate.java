package com.ax.template.authblueprint.exceptiongate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * orthogonal-exception-gate-l0 root — an exception dimension (a raised/lifted flag) that is
 * ORTHOGONAL to whatever primary lifecycle the subject also has. Generalized from the
 * reference workload's {@code DsrRestrictionGate} (GDPR Art 18 processing restriction), which
 * was the first proven instance of this exact shape: a fail-closed flag that blocks a
 * configurable operation set while raised, independent of and never mutating the subject's
 * own state machine (EXC-DIM-INDEPENDENT-001). {@code primaryState} here stands in for
 * "whatever the subject's own lifecycle field is" — a real adopter's subject already has one;
 * this reference workload carries its own toy copy purely so the independence invariant is
 * directly testable without wiring a second domain.
 */
@AggregateRoot
@Entity
@Table(name = "exception_gates", uniqueConstraints = {
    @UniqueConstraint(name = "uq_exception_gate_subject", columnNames = {"subject_type", "subject_id"})
})
public class ExceptionGate {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_type", nullable = false, updatable = false, length = 100)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    @Column(name = "raised", nullable = false)
    private boolean raised;

    @Column(name = "reason", length = 500)
    private String reason;

    /** Stand-in for the subject's OWN primary lifecycle — orthogonal to {@link #raised}. */
    @Column(name = "primary_state", nullable = false, length = 100)
    private String primaryState;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExceptionGate() {}

    public ExceptionGate(UUID id, String subjectType, String subjectId, Instant createdAt) {
        this.id = id;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.raised = false;
        this.primaryState = "ACTIVE";
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — EXC-DIM-INDEPENDENT-001: touches ONLY the exception dimension. */
    void raise(String reason) {
        this.raised = true;
        this.reason = reason;
    }

    /** Sole-mutator hook — EXC-DIM-INDEPENDENT-001: touches ONLY the exception dimension. */
    void lift() {
        this.raised = false;
        this.reason = null;
    }

    /** Sole-mutator hook — EXC-DIM-INDEPENDENT-001: touches ONLY the primary lifecycle. */
    void advancePrimary(String newState) {
        this.primaryState = newState;
    }

    public UUID getId() { return id; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }
    public boolean isRaised() { return raised; }
    public String getReason() { return reason; }
    public String getPrimaryState() { return primaryState; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
