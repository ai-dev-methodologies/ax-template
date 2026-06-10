package com.ax.template.authblueprint.decisiongov;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable determination in a scope's chain (DG-BASIS/RECOMPUTE/OVERRIDE-001):
 * fully append-only — every column {@code updatable=false}, no public setter. The
 * four-eyes inequality and the reason requirement are DB-backstopped via @Check so
 * they hold even under ddl-auto. {@code basisJson} is the appraisal-sufficient
 * snapshot (ASOP 41 §3.2); for an OVERRIDE it records the basis deviated FROM.
 */
@AggregateMember(root = DecisionScope.class)
@Entity
@Table(name = "decision_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_decision_scope_version", columnNames = {"scope_id", "version_no"})
})
// DG-OVERRIDE-001 four-eyes + DG-RECOMPUTE-001 reason — LIVE under ddl-auto.
@Check(constraints = "version_no >= 1"
    + " AND (kind = 'COMPUTED' OR LENGTH(TRIM(reason)) > 0)"
    + " AND (kind <> 'OVERRIDE' OR (approved_by IS NOT NULL AND approved_by <> decided_by))")
public class DecisionVersion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    private UUID scopeId;

    @Column(name = "version_no", nullable = false, updatable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private DecisionKind kind;

    /** Appraisal-sufficient basis snapshot — inputs/assumptions/method version (DG-BASIS-001). */
    @Column(name = "basis_json", nullable = false, updatable = false, length = 4000)
    private String basisJson;

    @Column(name = "outcome", nullable = false, updatable = false, length = 500)
    private String outcome;

    @Column(name = "reason", updatable = false, length = 1000)
    private String reason;

    @Column(name = "decided_by", nullable = false, updatable = false, length = 200)
    private String decidedBy;

    /** Four-eyes approver — REQUIRED and ≠ decidedBy for OVERRIDE (DG-OVERRIDE-001). */
    @Column(name = "approved_by", updatable = false, length = 200)
    private String approvedBy;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected DecisionVersion() {}

    public DecisionVersion(UUID id, UUID scopeId, int versionNo, DecisionKind kind, String basisJson,
                           String outcome, String reason, String decidedBy, String approvedBy,
                           Instant decidedAt) {
        this.id = id;
        this.scopeId = scopeId;
        this.versionNo = versionNo;
        this.kind = kind;
        this.basisJson = basisJson;
        this.outcome = outcome;
        this.reason = reason;
        this.decidedBy = decidedBy;
        this.approvedBy = approvedBy;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getScopeId() { return scopeId; }
    public int getVersionNo() { return versionNo; }
    public DecisionKind getKind() { return kind; }
    public String getBasisJson() { return basisJson; }
    public String getOutcome() { return outcome; }
    public String getReason() { return reason; }
    public String getDecidedBy() { return decidedBy; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
