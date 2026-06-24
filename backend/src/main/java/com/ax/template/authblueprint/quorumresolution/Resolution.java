package com.ax.template.authblueprint.quorumresolution;

import com.ax.template.authblueprint.common.AggregateMember;

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

/**
 * The immutable resolution of a motion — one per motion (UNIQUE motion_id).
 * All columns updatable=false. The @Check anchors the arithmetic invariants
 * so they hold even under ddl-auto.
 *
 * <p>Re-resolve is idempotent: UNIQUE(motion_id) prevents a second insert;
 * the service returns the existing row byte-identical (QR-IDEMPOTENT-001).
 */
@AggregateMember(root = Motion.class)
@Entity
@Table(name = "quorum_resolutions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_quorum_resolution_motion", columnNames = {"motion_id"})
})
@Check(constraints = "outcome IN ('PASSED','REJECTED','NO_DECISION')"
    + " AND yes_weight + no_weight + abstain_weight <= total_eligible_weight"
    + " AND cast_eligible_weight <= total_eligible_weight")
public class Resolution {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "motion_id", nullable = false, updatable = false)
    private UUID motionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private Outcome outcome;

    @Column(name = "yes_weight", nullable = false, updatable = false)
    private long yesWeight;

    @Column(name = "no_weight", nullable = false, updatable = false)
    private long noWeight;

    @Column(name = "abstain_weight", nullable = false, updatable = false)
    private long abstainWeight;

    /** Sum of weights of all cast ballots (≤ totalEligibleWeight). Quorum is checked against totalEligibleWeight. */
    @Column(name = "cast_eligible_weight", nullable = false, updatable = false)
    private long castEligibleWeight;

    /** Frozen totalEligibleWeight from the motion snapshot — used for quorum. */
    @Column(name = "total_eligible_weight", nullable = false, updatable = false)
    private long totalEligibleWeight;

    @Column(name = "resolved_at", nullable = false, updatable = false)
    private Instant resolvedAt;

    protected Resolution() {}

    public Resolution(UUID id, UUID motionId, Outcome outcome,
                      long yesWeight, long noWeight, long abstainWeight,
                      long castEligibleWeight, long totalEligibleWeight, Instant resolvedAt) {
        this.id = id;
        this.motionId = motionId;
        this.outcome = outcome;
        this.yesWeight = yesWeight;
        this.noWeight = noWeight;
        this.abstainWeight = abstainWeight;
        this.castEligibleWeight = castEligibleWeight;
        this.totalEligibleWeight = totalEligibleWeight;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() { return id; }
    public UUID getMotionId() { return motionId; }
    public Outcome getOutcome() { return outcome; }
    public long getYesWeight() { return yesWeight; }
    public long getNoWeight() { return noWeight; }
    public long getAbstainWeight() { return abstainWeight; }
    public long getCastEligibleWeight() { return castEligibleWeight; }
    public long getTotalEligibleWeight() { return totalEligibleWeight; }
    public Instant getResolvedAt() { return resolvedAt; }
}
