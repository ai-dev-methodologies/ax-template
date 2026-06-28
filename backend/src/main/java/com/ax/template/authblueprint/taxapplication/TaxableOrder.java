package com.ax.template.authblueprint.taxapplication;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An order presented for order-level tax application — the DECLARED taxable input the tax
 * computation reads. It carries an order-level customer exemption flag and a list of
 * {@link TaxLine} lines (each with its own taxable base + per-line exemption). Money is in
 * integer minor units throughout.
 *
 * <p>This aggregate is the INPUT half of two portable correctness invariants realized by
 * {@link TaxApplicationService} (the sole mutator), distinct from the derived
 * {@link TaxAssessment} (the single combined tax record):
 * <ol>
 *   <li><b>EXEMPT-SKIP</b>: a tax-exempt customer OR a tax-exempt line ⇒ ZERO tax for that
 *       scope — exempt lines contribute 0 and a fully-exempt order has total taxable base 0.</li>
 *   <li><b>IDEMPOTENT-RECOMPUTE</b>: re-pricing reads this declaration and converges the order's
 *       tax to exactly one {@link TaxAssessment} row.</li>
 * </ol>
 *
 * <p>Identity ({@code id}, {@code createdAt}) is immutable ({@code @Column(updatable=false)}, no
 * public setter). The customer-exemption declaration is re-declarable input — mutated ONLY through
 * the package-private {@link #declareCustomerExempt(boolean)} called by the sole-mutator service.
 */
@AggregateRoot
@Entity(name = "TaxableOrder")
@Table(name = "taxable_orders")
public class TaxableOrder {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Order-level customer exemption — re-declarable input; an exempt customer pays ZERO tax. */
    @Column(name = "customer_exempt", nullable = false)
    private boolean customerExempt;

    /** The taxable lines (taxable base + per-line exemption), owned through this aggregate root. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "taxable_order_lines",
        joinColumns = @JoinColumn(name = "order_id"))
    private List<TaxLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaxableOrder() {}

    public TaxableOrder(UUID id, boolean customerExempt, List<TaxLine> lines, Instant createdAt) {
        this.id = id;
        this.customerExempt = customerExempt;
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public boolean isCustomerExempt() { return customerExempt; }
    public Instant getCreatedAt() { return createdAt; }

    /** Defensive copy — the line set is read-only to callers. */
    public List<TaxLine> getLines() { return List.copyOf(lines); }

    /**
     * Sole-mutator entry point for the order-level exemption declaration. Package-private: only
     * {@link TaxApplicationService} may flip it, so the re-price that follows removes the prior tax
     * row rather than leaving it stranded.
     */
    void declareCustomerExempt(boolean exempt) {
        this.customerExempt = exempt;
    }
}
