package com.ax.template.authblueprint.quorumresolution;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

/**
 * quorum-resolution aggregate root. All policy columns are frozen at open time
 * (all updatable=false except status). Status mutated ONLY by {@link MotionStateMachine}
 * via package-private markTallying()/markResolved(). The @Check anchors
 * DB-level invariants that hold even under ddl-auto.
 */
@AggregateRoot
@Entity
@Table(name = "motions")
@Check(constraints = "status IN ('OPEN','TALLYING','RESOLVED')"
    + " AND total_eligible_weight >= 0"
    + " AND threshold_denominator > 0"
    + " AND quorum_denominator > 0"
    + " AND (tie_break_mode <> 'CHAIR_CASTING' OR tie_break_voter_id IS NOT NULL)")
public class Motion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "convener_id", nullable = false, updatable = false, length = 200)
    private String convenerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MotionStatus status;

    /** Total weight of ALL eligible voters, snapshotted at open (QR-FREEZE-001). */
    @Column(name = "total_eligible_weight", nullable = false, updatable = false)
    private long totalEligibleWeight;

    // ── Policy snapshot columns — all updatable=false ────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, updatable = false, length = 20)
    private RuleType ruleType;

    /** Numerator of the threshold fraction (M-of-N or percentage). */
    @Column(name = "threshold_numerator", nullable = false, updatable = false)
    private long thresholdNumerator;

    /** Denominator of the threshold fraction — must be > 0 (DB @Check). */
    @Column(name = "threshold_denominator", nullable = false, updatable = false)
    private long thresholdDenominator;

    /** Numerator of the quorum fraction. */
    @Column(name = "quorum_numerator", nullable = false, updatable = false)
    private long quorumNumerator;

    /** Denominator of the quorum fraction — must be > 0 (DB @Check). */
    @Column(name = "quorum_denominator", nullable = false, updatable = false)
    private long quorumDenominator;

    @Enumerated(EnumType.STRING)
    @Column(name = "abstention_mode", nullable = false, updatable = false, length = 30)
    private AbstentionMode abstentionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tie_break_mode", nullable = false, updatable = false, length = 20)
    private TieBreakMode tieBreakMode;

    /** Required iff tieBreakMode == CHAIR_CASTING (DB @Check). Frozen at open. */
    @Column(name = "tie_break_voter_id", updatable = false, length = 200)
    private String tieBreakVoterId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Motion() {}

    public Motion(UUID id, String convenerId, long totalEligibleWeight,
                  RuleType ruleType, long thresholdNumerator, long thresholdDenominator,
                  long quorumNumerator, long quorumDenominator,
                  AbstentionMode abstentionMode, TieBreakMode tieBreakMode,
                  String tieBreakVoterId, Instant createdAt) {
        this.id = id;
        this.convenerId = convenerId;
        this.status = MotionStatus.OPEN;
        this.totalEligibleWeight = totalEligibleWeight;
        this.ruleType = ruleType;
        this.thresholdNumerator = thresholdNumerator;
        this.thresholdDenominator = thresholdDenominator;
        this.quorumNumerator = quorumNumerator;
        this.quorumDenominator = quorumDenominator;
        this.abstentionMode = abstentionMode;
        this.tieBreakMode = tieBreakMode;
        this.tieBreakVoterId = tieBreakVoterId;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — called only by MotionStateMachine. */
    void markTallying() {
        MotionStateMachine.transition(this, MotionStatus.TALLYING);
    }

    /** Sole-mutator hook — called only by MotionStateMachine. */
    void markResolved() {
        MotionStateMachine.transition(this, MotionStatus.RESOLVED);
    }

    // ── package-private status write used by state machine only ──
    void setStatus(MotionStatus status) { this.status = status; }

    // ── public read-only accessors ──
    public UUID getId() { return id; }
    public String getConvenerId() { return convenerId; }
    public MotionStatus getStatus() { return status; }
    public long getTotalEligibleWeight() { return totalEligibleWeight; }
    public RuleType getRuleType() { return ruleType; }
    public long getThresholdNumerator() { return thresholdNumerator; }
    public long getThresholdDenominator() { return thresholdDenominator; }
    public long getQuorumNumerator() { return quorumNumerator; }
    public long getQuorumDenominator() { return quorumDenominator; }
    public AbstentionMode getAbstentionMode() { return abstentionMode; }
    public TieBreakMode getTieBreakMode() { return tieBreakMode; }
    public String getTieBreakVoterId() { return tieBreakVoterId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
