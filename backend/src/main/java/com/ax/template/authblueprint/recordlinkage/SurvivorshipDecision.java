package com.ax.template.authblueprint.recordlinkage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable per-field survivorship decision (LINK-SURVIVOR-001): which value won, which
 * record supplied it, and the rule that chose it. Appended at merge time — fully append-only;
 * one decision per (proposal, field).
 */
@AggregateMember(root = MatchProposal.class)
@Entity
@Table(name = "survivorship_decisions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_survivorship_field", columnNames = {"proposal_id", "field_name"})
})
public class SurvivorshipDecision {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false, updatable = false)
    private UUID proposalId;

    @Column(name = "field_name", nullable = false, updatable = false, length = 50)
    private String fieldName;

    @Column(name = "winning_value", updatable = false, length = 500)
    private String winningValue;

    @Column(name = "source_record_id", updatable = false)
    private UUID sourceRecordId;

    @Column(name = "rule_applied", nullable = false, updatable = false, length = 100)
    private String ruleApplied;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected SurvivorshipDecision() {}

    public SurvivorshipDecision(UUID id, UUID proposalId, String fieldName, String winningValue,
                                UUID sourceRecordId, String ruleApplied, Instant decidedAt) {
        this.id = id;
        this.proposalId = proposalId;
        this.fieldName = fieldName;
        this.winningValue = winningValue;
        this.sourceRecordId = sourceRecordId;
        this.ruleApplied = ruleApplied;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public String getFieldName() { return fieldName; }
    public String getWinningValue() { return winningValue; }
    public UUID getSourceRecordId() { return sourceRecordId; }
    public String getRuleApplied() { return ruleApplied; }
    public Instant getDecidedAt() { return decidedAt; }
}
