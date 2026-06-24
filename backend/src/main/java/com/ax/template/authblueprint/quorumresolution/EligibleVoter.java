package com.ax.template.authblueprint.quorumresolution;

import com.ax.template.authblueprint.common.AggregateMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.util.UUID;

/**
 * An eligible voter registered at motion-open. Fully append-only — all columns updatable=false.
 * weight > 0 is DB-backstopped by @Check.
 */
@AggregateMember(root = Motion.class)
@Entity
@Table(name = "quorum_eligible_voters", uniqueConstraints = {
    @UniqueConstraint(name = "uq_quorum_eligible_voter", columnNames = {"motion_id", "voter_id"})
})
@Check(constraints = "weight > 0")
public class EligibleVoter {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "motion_id", nullable = false, updatable = false)
    private UUID motionId;

    @Column(name = "voter_id", nullable = false, updatable = false, length = 200)
    private String voterId;

    @Column(name = "weight", nullable = false, updatable = false)
    private long weight;

    protected EligibleVoter() {}

    public EligibleVoter(UUID id, UUID motionId, String voterId, long weight) {
        this.id = id;
        this.motionId = motionId;
        this.voterId = voterId;
        this.weight = weight;
    }

    public UUID getId() { return id; }
    public UUID getMotionId() { return motionId; }
    public String getVoterId() { return voterId; }
    public long getWeight() { return weight; }
}
