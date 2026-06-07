package com.ax.template.authblueprint.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * banded-pricing-l0 — the REAL reusable primitive for marginal / tiered / time-of-use pricing
 * (specs/banded-pricing-l0.yaml). Segments ONE quantity across an ordered set of half-open bands,
 * charging each portion at its own band's rate: {@code charge = Σ over bands of qtyInBand × rate}.
 *
 * <p>The bands TILE the axis {@code [0, ∞)} with no gap and no overlap — band i is the half-open
 * interval {@code [prev-threshold, this-threshold)}, thresholds are strictly increasing and positive,
 * adjacent bands abut (Dijkstra EWD831: the upper bound of one equals the lower bound of the next), and
 * EXACTLY the last band is unbounded ({@code upperExclusive == null}) so the whole axis is covered.
 * Every unit of the quantity is therefore charged exactly once at exactly its band's rate
 * ({@code Σ qtyInBand == quantity}). The monetary total is rounded ONCE (the exact Σ is rounded at the
 * end), never a sum of independently-rounded per-band charges (the Money-pattern penny hazard).
 *
 * <p>This is a stateless pure function (no Spring) — the conserving, half-open-tiling counterpart to
 * {@link WaterfallAllocator} (which partitions one <em>amount</em> across capped tiers, no per-unit
 * rate) and to a rounded-split (which splits a <em>known total</em>). DISTINCT from both.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * var bands = List.of(
 *     new BandedPricer.Band(new BigDecimal("10"), new BigDecimal("1.00")),   // [0,10)  @ 1.00
 *     new BandedPricer.Band(new BigDecimal("20"), new BigDecimal("0.50")),   // [10,20) @ 0.50
 *     new BandedPricer.Band(null,                 new BigDecimal("0.10")));  // [20,∞)  @ 0.10
 * BandedPricer.price(new BigDecimal("25"), bands, 2).total();   // 15.50  (10×1 + 10×0.5 + 5×0.1)
 * }</pre>
 */
public final class BandedPricer {

    private BandedPricer() {}

    /**
     * One band of the rate schedule.
     *
     * @param upperExclusive the band's exclusive upper bound; {@code null} ONLY for the last
     *                       (unbounded) band. Bands are interpreted in list order as the half-open
     *                       intervals {@code [previous upper, this upper)}.
     * @param rate           the per-unit price applied to the quantity that falls in this band ({@code >= 0}).
     */
    public record Band(BigDecimal upperExclusive, BigDecimal rate) {}

    /** The charge attributed to one band (full precision — the authoritative rounding is on the total). */
    public record BandCharge(BigDecimal lowerInclusive, BigDecimal upperExclusive,
                             BigDecimal qtyInBand, BigDecimal rate, BigDecimal charge) {}

    /**
     * @param total            the round-ONCE monetary total (Σ exact per-band charges, rounded at the end)
     * @param quantityCharged  Σ of qtyInBand across bands — equals the input quantity (conservation)
     * @param breakdown        per-band detail at full precision
     */
    public record Result(BigDecimal total, BigDecimal quantityCharged, List<BandCharge> breakdown) {}

    /**
     * Price {@code quantity} against the {@code bands} (BAND-SEGMENT/TILING/CONSERVE-001).
     *
     * @param quantity the non-negative quantity to price
     * @param bands    the rate schedule — tiling, strictly-increasing, last-unbounded (validated)
     * @param scale    the currency scale the TOTAL is rounded to (HALF_UP), once, at the end
     * @return the conserving {@link Result}
     * @throws IllegalArgumentException if the quantity is negative or the bands do not tile [0,∞)
     */
    public static Result price(BigDecimal quantity, List<Band> bands, int scale) {
        if (quantity == null || quantity.signum() < 0) {
            throw new IllegalArgumentException("quantity must be non-negative: " + quantity);
        }
        validateTiling(bands);
        BigDecimal lo = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal charged = BigDecimal.ZERO;
        List<BandCharge> breakdown = new ArrayList<>(bands.size());
        for (Band b : bands) {
            BigDecimal hi = b.upperExclusive() == null ? quantity.max(lo) : b.upperExclusive();
            BigDecimal qtyInBand = quantity.min(hi).subtract(lo).max(BigDecimal.ZERO);   // clamp to [lo,hi)
            BigDecimal charge = qtyInBand.multiply(b.rate());                            // exact, no per-band rounding
            breakdown.add(new BandCharge(lo, b.upperExclusive(), qtyInBand, b.rate(), charge));
            total = total.add(charge);
            charged = charged.add(qtyInBand);
            lo = hi;
        }
        if (charged.compareTo(quantity) != 0) {                 // tiling guarantees this; backstop only
            throw new IllegalStateException("quantity not fully charged (bands do not tile): "
                + charged + " != " + quantity);
        }
        return new Result(total.setScale(scale, RoundingMode.HALF_UP), charged, List.copyOf(breakdown));
    }

    /** Strictly-increasing positive thresholds; exactly the last band unbounded; non-negative rates. */
    private static void validateTiling(List<Band> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new IllegalArgumentException("at least one band is required");
        }
        BigDecimal prev = BigDecimal.ZERO;       // the first band's lower bound is 0
        for (int i = 0; i < bands.size(); i++) {
            Band b = bands.get(i);
            if (b == null || b.rate() == null || b.rate().signum() < 0) {
                throw new IllegalArgumentException("each band rate must be non-negative");
            }
            boolean last = i == bands.size() - 1;
            if (last) {
                if (b.upperExclusive() != null) {
                    throw new IllegalArgumentException("the last band must be unbounded (null upperExclusive)");
                }
            } else {
                if (b.upperExclusive() == null) {
                    throw new IllegalArgumentException("only the last band may be unbounded");
                }
                if (b.upperExclusive().compareTo(prev) <= 0) {
                    throw new IllegalArgumentException("band thresholds must be strictly increasing: "
                        + b.upperExclusive() + " after " + prev);
                }
                prev = b.upperExclusive();
            }
        }
    }
}
