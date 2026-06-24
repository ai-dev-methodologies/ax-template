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
 * A fully append-only, immutable ballot cast by an eligible voter. All columns updatable=false.
 * UNIQUE(motion_id, voter_id) makes double-vote unrepresentable in the database.
 * weight_at_cast is copied from the voter's frozen weight at cast time (QR-FREEZE-001).
 */
@AggregateMember(root = Motion.class)
@Entity
@Table(name = "quorum_ballots", uniqueConstraints = {
    @UniqueConstraint(name = "uq_quorum_ballot_voter", columnNames = {"motion_id", "voter_id"})
})
@Check(constraints = "choice IN ('YES','NO','ABSTAIN') AND weight_at_cast > 0")
public class Ballot {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "motion_id", nullable = false, updatable = false)
    private UUID motionId;

    @Column(name = "voter_id", nullable = false, updatable = false, length = 200)
    private String voterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, updatable = false, length = 10)
    private Choice choice;

    /** Copied from EligibleVoter.weight at cast time — immutable thereafter. */
    @Column(name = "weight_at_cast", nullable = false, updatable = false)
    private long weightAtCast;

    @Column(name = "cast_at", nullable = false, updatable = false)
    private Instant castAt;

    protected Ballot() {}

    public Ballot(UUID id, UUID motionId, String voterId, Choice choice, long weightAtCast, Instant castAt) {
        this.id = id;
        this.motionId = motionId;
        this.voterId = voterId;
        this.choice = choice;
        this.weightAtCast = weightAtCast;
        this.castAt = castAt;
    }

    public UUID getId() { return id; }
    public UUID getMotionId() { return motionId; }
    public String getVoterId() { return voterId; }
    public Choice getChoice() { return choice; }
    public long getWeightAtCast() { return weightAtCast; }
    public Instant getCastAt() { return castAt; }
}
