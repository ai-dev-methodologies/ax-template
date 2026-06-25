package com.ax.template.authblueprint.commercepricing;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stateless pricing pipeline — pure computation, no @Transactional, no @Entity.
 *
 * <p>Implements two Broadleaf-origin pricing invariants:
 *
 * <h3>PRICING-ORDER-001 — phase order determines tax basis</h3>
 * The method-body order IS the determinism source (mirrors Broadleaf setOrder() constants):
 * <ol>
 *   <li>subTotal = Σ line.amount</li>
 *   <li>PRORATE orderDiscount conservingly (floor-remainder, Σ prorated == orderDiscount EXACTLY)</li>
 *   <li>For each line: taxableBase = amount − proratedDiscount (NET, post-discount)</li>
 *   <li>tax = Math.multiplyExact(taxableBase, taxBasisPoints) / 10000 (round-once, integer)</li>
 *   <li>total = subTotal − orderDiscount + shipping + Σ tax</li>
 * </ol>
 * Tax is on the NET (post-discount) base. Applying tax to gross is a phase-order violation.
 *
 * <h3>PRICING-TOTAL-001 — total closure</h3>
 * total == subTotal − orderDiscount + shipping + Σ tax EXACTLY (integer, no floating-point drift).
 * Σ(proratedDiscount) == orderDiscount EXACTLY (floor-remainder distribution).
 *
 * <p>All monetary arithmetic uses {@code long} minor units and {@link Math#multiplyExact}
 * to fail-closed on overflow. No {@code double} or {@code float} anywhere.
 */
@Service
public class PricingPipeline {

    /**
     * Price an order. All money values are integer minor units (e.g. cents).
     *
     * @param lines          line items (sku ref + amount); must be non-null and non-empty
     * @param orderDiscount  order-level discount in minor units; clamped to subTotal
     * @param shipping       shipping charge in minor units; non-negative
     * @param taxBasisPoints tax rate in basis points, e.g. 1000 = 10%; must be in [0, 10000]
     * @return fully-priced order with conservation guarantee
     * @throws PricingException if taxBasisPoints out of range or inputs invalid
     */
    public PricedOrder priceOrder(List<Line> lines, long orderDiscount, long shipping, int taxBasisPoints) {
        // ── Validate inputs ──────────────────────────────────────────────────────────
        if (lines == null || lines.isEmpty()) {
            throw PricingException.invalidInput("lines must be non-null and non-empty");
        }
        if (taxBasisPoints < 0 || taxBasisPoints > 10_000) {
            throw PricingException.invalidTaxRate(taxBasisPoints);
        }
        if (shipping < 0) {
            throw PricingException.invalidInput("shipping must be >= 0");
        }

        // ── Phase 1: subTotal = Σ line.amount ─────────────────────────────────────
        long subTotal = 0L;
        for (Line line : lines) {
            subTotal = Math.addExact(subTotal, Math.max(line.amount(), 0L));
        }

        // ── Phase 2: prorate orderDiscount conservingly ──────────────────────────
        // Clamp discount to subTotal (never negative taxable base at order level)
        long clampedDiscount = Math.min(orderDiscount, subTotal);
        clampedDiscount = Math.max(clampedDiscount, 0L);

        int n = lines.size();
        long[] proratedDiscounts = prorateConserving(clampedDiscount, lines, subTotal);

        // ── Phases 3 & 4: per-line taxableBase and tax ───────────────────────────
        long totalTax = 0L;
        PricedLine[] pricedLines = new PricedLine[n];
        for (int i = 0; i < n; i++) {
            long gross = Math.max(lines.get(i).amount(), 0L);
            long prorated = proratedDiscounts[i];
            // Clamp so taxable base is never negative
            long taxableBase = Math.max(gross - prorated, 0L);
            // Tax on NET (post-discount) base — PRICING-ORDER-001
            // Math.multiplyExact to fail-closed on overflow; integer division rounds toward zero
            long tax = Math.multiplyExact(taxableBase, taxBasisPoints) / 10_000;
            totalTax = Math.addExact(totalTax, tax);
            pricedLines[i] = new PricedLine(gross, prorated, taxableBase, tax);
        }

        // ── Phase 5: total closure ────────────────────────────────────────────────
        // total = subTotal − orderDiscount + shipping + Σ tax  (PRICING-TOTAL-001)
        long total = subTotal - clampedDiscount + shipping + totalTax;

        return new PricedOrder(subTotal, clampedDiscount, shipping, totalTax, total,
            List.of(pricedLines));
    }

    /**
     * Floor-remainder proration: distributes {@code discount} across lines proportional to amount.
     * Each share = floor(discount * amount_i / subTotal).
     * Leftover minor units (due to flooring) are distributed one-by-one to lines with the
     * largest fractional remainder, then by index as tie-break.
     * Invariant: Σ(shares) == discount EXACTLY.
     */
    private long[] prorateConserving(long discount, List<Line> lines, long subTotal) {
        int n = lines.size();
        if (subTotal == 0 || discount == 0) {
            return new long[n]; // all zeros
        }

        long[] shares = new long[n];
        long[] fractions = new long[n]; // scaled fractional remainder for tie-breaking

        for (int i = 0; i < n; i++) {
            long amount = Math.max(lines.get(i).amount(), 0L);
            long numerator = Math.multiplyExact(discount, amount);
            shares[i] = numerator / subTotal;
            fractions[i] = numerator % subTotal;
        }

        // Count distributed so far and compute leftover
        long distributed = 0L;
        for (long s : shares) distributed += s;
        long leftover = discount - distributed;

        // Sort indices by fractional remainder DESC, then index ASC (stable tie-break)
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> {
            int cmp = Long.compare(fractions[b], fractions[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });

        for (int k = 0; k < leftover; k++) {
            shares[indices[k]]++;
        }

        return shares;
    }

    // ── Value types ───────────────────────────────────────────────────────────────

    /**
     * Input line: a SKU reference and an amount in minor units.
     * Negative amounts are treated as zero (defensive clamp).
     */
    public record Line(String sku, long amount) {}

    /**
     * Priced line: computed values for one input line.
     *
     * @param gross            the original line amount (input, non-negative)
     * @param proratedDiscount the share of the order-level discount allocated to this line
     * @param taxableBase      gross − proratedDiscount (never negative, PRICING-ORDER-001)
     * @param tax              floor(taxableBase × taxBasisPoints / 10000)
     */
    public record PricedLine(long gross, long proratedDiscount, long taxableBase, long tax) {}

    /**
     * Priced order: the complete pricing result.
     *
     * <p>Closure invariant (PRICING-TOTAL-001):
     * {@code total == subTotal − orderDiscount + shipping + totalTax}
     *
     * @param subTotal       Σ line.amount
     * @param orderDiscount  the (clamped) order-level discount applied
     * @param shipping       shipping charge
     * @param totalTax       Σ(line.tax) across all priced lines
     * @param total          conserving total (closure formula above)
     * @param lines          per-line pricing detail
     */
    public record PricedOrder(
        long subTotal,
        long orderDiscount,
        long shipping,
        long totalTax,
        long total,
        List<PricedLine> lines) {}
}
