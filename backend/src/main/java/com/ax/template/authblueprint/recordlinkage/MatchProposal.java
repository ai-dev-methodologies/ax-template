package com.ax.template.authblueprint.recordlinkage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * record-linkage-l0 match proposal: the APPRAISABLE verdict row (LINK-BAND-001) — score,
 * per-field feature breakdown, and the thresholds in force are all persisted, so a bare
 * unexplained verdict is unrepresentable. Records are referenced BY ID in ascending order
 * (low/high — the lock-order discipline). A proposal decides exactly once, recording
 * who/when (@Check backstop; LINK-REVIEW-001), via the package-private {@link #decide}.
 */
@AggregateRoot
@Entity
@Table(name = "match_proposals")
@Check(constraints = "score >= 0 AND score <= 1 AND lower_threshold < upper_threshold"
    + " AND (status = 'PROPOSED' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL))")
public class MatchProposal {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The smaller record id — ascending order is the deadlock guard (LINK-CONCURRENT-001). */
    @Column(name = "low_record_id", nullable = false, updatable = false)
    private UUID lowRecordId;

    @Column(name = "high_record_id", nullable = false, updatable = false)
    private UUID highRecordId;

    @Column(name = "score", nullable = false, updatable = false, precision = 5, scale = 4)
    private BigDecimal score;

    /** Per-field contribution trail — which fields agreed and what each added (LINK-BAND-001). */
    @Column(name = "breakdown_json", nullable = false, updatable = false, length = 1000)
    private String breakdownJson;

    @Column(name = "lower_threshold", nullable = false, updatable = false, precision = 5, scale = 4)
    private BigDecimal lowerThreshold;

    @Column(name = "upper_threshold", nullable = false, updatable = false, precision = 5, scale = 4)
    private BigDecimal upperThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = false, updatable = false, length = 20)
    private MatchBand band;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status;

    @Column(name = "decided_by", length = 200)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MatchProposal() {}

    public MatchProposal(UUID id, UUID lowRecordId, UUID highRecordId, BigDecimal score,
                         String breakdownJson, BigDecimal lowerThreshold, BigDecimal upperThreshold,
                         MatchBand band, Instant createdAt) {
        this.id = id;
        this.lowRecordId = lowRecordId;
        this.highRecordId = highRecordId;
        this.score = score;
        this.breakdownJson = breakdownJson;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.band = band;
        this.status = ProposalStatus.PROPOSED;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — a proposal decides exactly once, with who/when (LINK-REVIEW-001). */
    void decide(ProposalStatus terminal, String by, Instant at) {
        this.status = terminal;
        this.decidedBy = by;
        this.decidedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getLowRecordId() { return lowRecordId; }
    public UUID getHighRecordId() { return highRecordId; }
    public BigDecimal getScore() { return score; }
    public String getBreakdownJson() { return breakdownJson; }
    public BigDecimal getLowerThreshold() { return lowerThreshold; }
    public BigDecimal getUpperThreshold() { return upperThreshold; }
    public MatchBand getBand() { return band; }
    public ProposalStatus getStatus() { return status; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
