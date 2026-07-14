package com.ax.template.authblueprint.appealindependence;

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
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * AppealDecision — one entry in a self-referencing appeal chain (APPEAL-DISTINCT-001 /
 * APPEAL-CHAIN-001 / APPEAL-OUTCOME-001). Fully append-only: every column
 * {@code updatable=false}, no public setter — an appeal outcome is a NEW row, never a mutation
 * of the row it appeals. Each row is its own {@link AggregateRoot} (a flat, self-referencing
 * chain by id, not an object graph) — {@code parentDecisionId} references a PRIOR row's id,
 * never a JPA object pointer, so there is no cross-aggregate reference violation.
 *
 * <p>{@code appealedDeciderBy} denormalizes the decider of the row THIS row appeals (null for
 * {@code kind=ORIGINAL}) purely so the DB @Check can enforce the immediate-parent case of
 * APPEAL-DISTINCT-001 without a cross-row subquery (CHECK constraints are row-local) — mirroring
 * {@code decisiongov.DecisionVersion}'s four-eyes @Check precedent. The FULL chain-wide
 * distinctness (APPEAL-CHAIN-001 — differ from ALL prior deciders, not just the parent) is an
 * app-level check in {@link AppealService}; only the pairwise case is DB-expressible.
 */
@AggregateRoot
@Entity
@Table(name = "appeal_decisions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_appeal_parent_decision", columnNames = {"parent_decision_id"})
})
@Check(constraints = "(kind = 'ORIGINAL') OR (appealed_decider_by IS NOT NULL AND decided_by <> appealed_decider_by)")
public class AppealDecision {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** {@code null} for the ORIGINAL (level 0) row — the head of the chain. */
    @Column(name = "parent_decision_id", updatable = false)
    private UUID parentDecisionId;

    /** The ORIGINAL row's own id — equals {@code id} for the ORIGINAL itself; convenience for chain queries. */
    @Column(name = "chain_root_id", nullable = false, updatable = false)
    private UUID chainRootId;

    @Column(name = "level", nullable = false, updatable = false)
    private int level;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 16)
    private AppealDecisionKind kind;

    @Column(name = "decided_by", nullable = false, updatable = false, length = 200)
    private String decidedBy;

    /** Denormalized decider of the row this one appeals — see class javadoc. Null for ORIGINAL. */
    @Column(name = "appealed_decider_by", updatable = false, length = 200)
    private String appealedDeciderBy;

    @Column(name = "outcome", nullable = false, updatable = false, length = 32)
    private String outcome;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected AppealDecision() {}

    public AppealDecision(UUID id, UUID parentDecisionId, UUID chainRootId, int level,
                          AppealDecisionKind kind, String decidedBy, String appealedDeciderBy,
                          String outcome, Instant decidedAt) {
        this.id = id;
        this.parentDecisionId = parentDecisionId;
        this.chainRootId = chainRootId;
        this.level = level;
        this.kind = kind;
        this.decidedBy = decidedBy;
        this.appealedDeciderBy = appealedDeciderBy;
        this.outcome = outcome;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getParentDecisionId() { return parentDecisionId; }
    public UUID getChainRootId() { return chainRootId; }
    public int getLevel() { return level; }
    public AppealDecisionKind getKind() { return kind; }
    public String getDecidedBy() { return decidedBy; }
    public String getAppealedDeciderBy() { return appealedDeciderBy; }
    public String getOutcome() { return outcome; }
    public Instant getDecidedAt() { return decidedAt; }
}
