package com.ax.template.authblueprint.bundlepricing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A bundle-level fee — an immutable value object added to the ITEM_SUM roll-up
 * (absorbed from the external reference {@code BundleOrderItemFeePrice}).
 *
 * <p>An {@code @Embeddable} value (NOT an aggregate-tagged {@code @Entity}): a fee has no
 * identity of its own and is owned wholly by its {@link CompositeItem} via an
 * {@code @ElementCollection}. {@code amount} is integer minor units (long). {@code taxable}
 * decides whether the fee contributes to the bundle's taxable price (BUNDLE-DERIVED-001).
 *
 * <p>All columns are {@code updatable=false}: a fee, once attached at composite creation,
 * is immutable — there is no public mutator, so a fee cannot be re-priced under a composite
 * whose total is derived from it.
 */
@Embeddable
public class BundleFee {

    @Column(name = "label", length = 200, updatable = false)
    private String label;

    /** Fee amount in minor currency units (e.g. cents). */
    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "taxable", nullable = false, updatable = false)
    private boolean taxable;

    protected BundleFee() {}

    public BundleFee(String label, long amount, boolean taxable) {
        this.label = label;
        this.amount = amount;
        this.taxable = taxable;
    }

    public String getLabel() { return label; }
    public long getAmount() { return amount; }
    public boolean isTaxable() { return taxable; }
}
