package com.ax.template.authblueprint.taxapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One order line presented for tax application: its taxable base in integer minor units and a
 * DECLARED per-line exemption flag. An exempt line contributes ZERO to the order's taxable base
 * (EXEMPT-SKIP) — exemption is declared, never inferred.
 *
 * <p>An {@code @Embeddable} (not an {@code @Entity}): lines are owned by the {@link TaxableOrder}
 * aggregate root via an {@code @ElementCollection}, so they carry no identity or repository of
 * their own and need no aggregate tag.
 */
@Embeddable
public class TaxLine {

    /** This line's taxable base, integer minor units (no fractional currency). */
    @Column(name = "taxable_base_minor", nullable = false)
    private long taxableBaseMinor;

    /** DECLARED per-line exemption — an exempt line contributes 0 to the taxable base. */
    @Column(name = "exempt", nullable = false)
    private boolean exempt;

    protected TaxLine() {}

    public TaxLine(long taxableBaseMinor, boolean exempt) {
        this.taxableBaseMinor = taxableBaseMinor;
        this.exempt = exempt;
    }

    public long getTaxableBaseMinor() { return taxableBaseMinor; }

    public boolean isExempt() { return exempt; }
}
