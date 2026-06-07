package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * banded-pricing-l0 compliance — the pure BandedPricer primitive (no Spring). Marginal/tiered pricing:
 * a quantity is segmented across half-open bands, each charged at its own rate; the bands tile [0,∞);
 * every unit is charged once; the total is rounded once. Spec: specs/banded-pricing-l0.yaml.
 */
@Tag("BANDEDPRICE")
class BandedPricerTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static List<BandedPricer.Band> tiered() {
        return List.of(
            new BandedPricer.Band(bd("10"), bd("1.00")),    // [0,10)  @ 1.00
            new BandedPricer.Band(bd("20"), bd("0.50")),    // [10,20) @ 0.50
            new BandedPricer.Band(null,     bd("0.10")));   // [20,∞)  @ 0.10
    }

    // ── BAND-SEGMENT-001 — qty segmented across bands, charge = Σ qty-in-band × rate ──
    @Test @Tag("BAND-SEGMENT-001")
    void marginalBanding_chargesEachPortionAtItsBandRate() {
        BandedPricer.Result r = BandedPricer.price(bd("25"), tiered(), 2);
        // 10×1.00 + 10×0.50 + 5×0.10 = 10 + 5 + 0.5 = 15.50
        assertThat(r.total()).isEqualByComparingTo("15.50");
        assertThat(r.breakdown()).hasSize(3);
        assertThat(r.breakdown().get(0).qtyInBand()).isEqualByComparingTo("10");
        assertThat(r.breakdown().get(1).qtyInBand()).isEqualByComparingTo("10");
        assertThat(r.breakdown().get(2).qtyInBand()).isEqualByComparingTo("5");
        assertThat(r.breakdown().get(2).charge()).isEqualByComparingTo("0.5");

        // a quantity exactly on a threshold falls in the LOWER band (half-open [lo,hi)): Q=10 → only band 0
        BandedPricer.Result edge = BandedPricer.price(bd("10"), tiered(), 2);
        assertThat(edge.total()).isEqualByComparingTo("10.00");
        assertThat(edge.breakdown().get(1).qtyInBand()).isEqualByComparingTo("0");

        // a single unbounded band is a flat rate over [0,∞)
        BandedPricer.Result flat = BandedPricer.price(bd("7"), List.of(new BandedPricer.Band(null, bd("2"))), 2);
        assertThat(flat.total()).isEqualByComparingTo("14.00");
        assertThat(BandedPricer.price(BigDecimal.ZERO, tiered(), 2).total()).isEqualByComparingTo("0.00");
    }

    // ── BAND-TILING-001 — bands must tile [0,∞): strictly increasing, last unbounded, non-negative ──
    @Test @Tag("BAND-TILING-001")
    void bandsMustTileTheAxis() {
        // non-increasing thresholds
        assertThatThrownBy(() -> BandedPricer.price(bd("5"), List.of(
                new BandedPricer.Band(bd("10"), bd("1")),
                new BandedPricer.Band(bd("10"), bd("1")),   // not strictly increasing
                new BandedPricer.Band(null, bd("1"))), 2))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("strictly increasing");
        // bounded last band (axis not fully covered → a quantity beyond it would be un-priced)
        assertThatThrownBy(() -> BandedPricer.price(bd("5"), List.of(
                new BandedPricer.Band(bd("10"), bd("1"))), 2))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("last band must be unbounded");
        // an interior unbounded band
        assertThatThrownBy(() -> BandedPricer.price(bd("5"), List.of(
                new BandedPricer.Band(null, bd("1")),
                new BandedPricer.Band(null, bd("1"))), 2))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only the last band");
        // negative rate
        assertThatThrownBy(() -> BandedPricer.price(bd("5"), List.of(
                new BandedPricer.Band(null, bd("-1"))), 2))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
        // negative quantity
        assertThatThrownBy(() -> BandedPricer.price(bd("-1"), tiered(), 2))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
    }

    // ── BAND-CONSERVE-001 — Σ qty-in-band == Q; total rounded ONCE (≠ sum-of-rounded) ──
    @Test @Tag("BAND-CONSERVE-001")
    void conservesQuantity_andRoundsOnce() {
        // conservation across a fractional quantity
        BandedPricer.Result r = BandedPricer.price(bd("12.5"), List.of(
            new BandedPricer.Band(bd("10"), bd("1")),
            new BandedPricer.Band(null, bd("0.5"))), 2);
        BigDecimal sumQty = r.breakdown().stream().map(BandedPricer.BandCharge::qtyInBand)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumQty).as("Σ qty-in-band == Q").isEqualByComparingTo("12.5");
        assertThat(r.total()).isEqualByComparingTo("11.25");   // 10×1 + 2.5×0.5

        // round-ONCE: two bands each charging 0.005 → exact total 0.010 rounds to 0.01,
        // NOT sum-of-rounded (round(0.005)=0.01 twice = 0.02). The primitive must produce 0.01.
        BandedPricer.Result penny = BandedPricer.price(bd("2"), List.of(
            new BandedPricer.Band(bd("1"), bd("0.005")),
            new BandedPricer.Band(null, bd("0.005"))), 2);
        assertThat(penny.total()).as("round-once total, not sum-of-rounded").isEqualByComparingTo("0.01");
    }
}
