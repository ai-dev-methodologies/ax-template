package com.ax.template.authblueprint.taxapplication;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

/**
 * The single combined tax record DERIVED for one order — the convergence target of
 * {@link TaxApplicationService#recompute}. Exactly ONE row may exist per order: the
 * {@code uq_tax_assessment_order} UNIQUE constraint on {@code order_id} makes a second tax row for
 * the same order UNREPRESENTABLE at the database layer, and the {@code @Check} makes a negative
 * tax amount unrepresentable.
 *
 * <p>It references its {@link TaxableOrder} by identity ({@code orderId}), never an object pointer
 * (cross-aggregate reference by id — HG-AGG-REF). Identity ({@code id}, {@code orderId}) is
 * immutable ({@code @Column(updatable=false)}); the recomputed columns ({@code taxAmountMinor},
 * {@code taxableBaseMinor}, {@code rateBasisPoints}, {@code computedAt}) are re-stamped IN PLACE by
 * the package-private {@link #recompute} — there is no public setter, so the amount can only move
 * through the sole-mutator service. Money is in integer minor units.
 */
@AggregateRoot
@Entity(name = "TaxAssessment")
@Table(name = "tax_assessments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tax_assessment_order", columnNames = {"order_id"})
})
@Check(constraints = "tax_amount_minor >= 0 AND taxable_base_minor >= 0 AND rate_basis_points >= 0")
public class TaxAssessment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** The order this is the combined tax for — referenced by identity, UNIQUE (one row per order). */
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    /** The current correct combined tax, integer minor units (never negative — see {@code @Check}). */
    @Column(name = "tax_amount_minor", nullable = false)
    private long taxAmountMinor;

    /** The non-exempt taxable base this amount was computed from (transparency). */
    @Column(name = "taxable_base_minor", nullable = false)
    private long taxableBaseMinor;

    /** The injected jurisdiction rate applied, in basis points (1 bp = 0.01%). */
    @Column(name = "rate_basis_points", nullable = false)
    private long rateBasisPoints;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected TaxAssessment() {}

    private TaxAssessment(UUID id, UUID orderId, long taxAmountMinor,
                          long taxableBaseMinor, long rateBasisPoints, Instant computedAt) {
        this.id = id;
        this.orderId = orderId;
        this.taxAmountMinor = taxAmountMinor;
        this.taxableBaseMinor = taxableBaseMinor;
        this.rateBasisPoints = rateBasisPoints;
        this.computedAt = computedAt;
    }

    /** Create the order's first combined tax record (the create branch of recompute). */
    static TaxAssessment create(UUID id, UUID orderId, long taxAmountMinor,
                                long taxableBaseMinor, long rateBasisPoints, Instant computedAt) {
        return new TaxAssessment(id, orderId, taxAmountMinor, taxableBaseMinor, rateBasisPoints, computedAt);
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public UUID getOrderId() { return orderId; }
    public long getTaxAmountMinor() { return taxAmountMinor; }
    public long getTaxableBaseMinor() { return taxableBaseMinor; }
    public long getRateBasisPoints() { return rateBasisPoints; }
    public Instant getComputedAt() { return computedAt; }

    /**
     * Sole-mutator recompute: re-stamp the combined tax IN PLACE (the update branch — the row's
     * identity {@code id}/{@code orderId} is preserved, so re-pricing converges to the same single
     * row). Package-private: only {@link TaxApplicationService} may call it.
     */
    void recompute(long taxAmountMinor, long taxableBaseMinor, long rateBasisPoints, Instant computedAt) {
        this.taxAmountMinor = taxAmountMinor;
        this.taxableBaseMinor = taxableBaseMinor;
        this.rateBasisPoints = rateBasisPoints;
        this.computedAt = computedAt;
    }
}
