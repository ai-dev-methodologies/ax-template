package com.ax.template.authblueprint.taxapplication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order-level tax application — the SOLE mutator of the {@link TaxAssessment} (the single combined
 * tax record). It realizes two portable, framework-agnostic correctness invariants. The jurisdiction
 * rate is INJECTED by the caller (no rate table, no external tax provider lives here).
 *
 * <ol>
 *   <li><b>EXEMPT-SKIP</b> ({@link #taxableBase}): a tax-exempt customer OR a tax-exempt line ⇒
 *       ZERO taxable base for that scope — exempt lines contribute 0, and a fully-exempt order has
 *       a taxable base of 0, hence ZERO tax. Exemption is DECLARED, never inferred.</li>
 *   <li><b>IDEMPOTENT-RECOMPUTE</b> ({@link #recompute}): re-pricing an order converges to exactly
 *       ONE combined tax record (find-existing → update-or-create-or-remove). Re-running the
 *       computation any number of times leaves exactly one tax row whose amount == the current
 *       correct tax — never duplicated, never stranded; a now-exempt order's prior tax row is
 *       removed, not left behind.</li>
 * </ol>
 *
 * <p>{@link #computeTax} and {@link #taxableBase} are pure, deterministic, side-effect-free
 * functions of their inputs — the same order + rate always yields the same tax.
 */
@Service
public class TaxApplicationService {

    /** Basis-point denominator: 10000 bp = 100% (1 bp = 0.01%). */
    private static final long BASIS_POINT_DENOMINATOR = 10_000L;

    private final TaxableOrderRepository orders;
    private final TaxAssessmentRepository assessments;
    private final TaxApplicationMetrics metrics;
    private final Clock clock;

    public TaxApplicationService(TaxableOrderRepository orders, TaxAssessmentRepository assessments,
                                 TaxApplicationMetrics metrics, Clock clock) {
        this.orders = orders;
        this.assessments = assessments;
        this.metrics = metrics;
        this.clock = clock;
    }

    // ─── Taxable-order lifecycle ────────────────────────────────────────────────────

    @Transactional
    public TaxableOrder createOrder(boolean customerExempt, List<TaxLine> lines) {
        List<TaxLine> safe = lines == null ? List.of() : lines;
        for (TaxLine l : safe) {
            if (l.getTaxableBaseMinor() < 0) {
                throw TaxApplicationException.invalidLine("line taxable_base_minor must be >= 0");
            }
        }
        TaxableOrder order = new TaxableOrder(UUID.randomUUID(), customerExempt, safe, Instant.now(clock));
        TaxableOrder saved = orders.saveAndFlush(order);
        metrics.recordOrderCreated();
        return saved;
    }

    @Transactional(readOnly = true)
    public TaxableOrder getOrder(UUID id) {
        return orders.findById(id)
            .orElseThrow(() -> TaxApplicationException.orderNotFound(id.toString()));
    }

    /**
     * Declare the order's customer tax-exempt (the re-declaration that precedes a now-exempt
     * re-price). Writes ONLY the {@link TaxableOrder} aggregate; the prior tax row is removed by the
     * following {@link #recompute}, not here.
     */
    @Transactional
    public TaxableOrder declareCustomerExempt(UUID orderId, boolean exempt) {
        TaxableOrder order = orders.findById(orderId)
            .orElseThrow(() -> TaxApplicationException.orderNotFound(orderId.toString()));
        order.declareCustomerExempt(exempt);
        return orders.saveAndFlush(order);
    }

    // ─── Core: idempotent recompute (the sole mutator of TaxAssessment) ─────────────

    /**
     * Re-price the order: compute the correct combined tax from the declared input and the injected
     * rate, then converge the order's tax to exactly ONE {@link TaxAssessment} row
     * (find-existing → update-or-create-or-remove). Idempotent: repeated application has the same
     * effect as one (RFC 9110 §9.2.2). Writes ONLY the {@link TaxAssessment} aggregate.
     */
    @Transactional
    public TaxResult recompute(UUID orderId, long rateBasisPoints) {
        if (rateBasisPoints < 0) {
            throw TaxApplicationException.invalidRate(rateBasisPoints);
        }
        TaxableOrder order = orders.findById(orderId)
            .orElseThrow(() -> TaxApplicationException.orderNotFound(orderId.toString()));

        long base = taxableBase(order);                 // EXEMPT-SKIP folded in here
        long tax = computeTax(base, rateBasisPoints);
        Optional<TaxAssessment> existing = assessments.findByOrderIdForUpdate(orderId);

        if (base == 0) {
            // Fully exempt / nothing taxable → converge to ZERO rows: remove the prior row, never
            // strand it (a now-exempt order leaves no tax behind).
            existing.ifPresent(assessments::delete);
            metrics.recordRecompute(existing.isPresent() ? "removed" : "exempt_zero");
            return TaxResult.none(orderId);
        }

        // Non-exempt → exactly one row carrying the current correct tax (update in place or create).
        TaxAssessment row = existing
            .map(a -> { a.recompute(tax, base, rateBasisPoints, Instant.now(clock)); return a; })
            .orElseGet(() -> TaxAssessment.create(
                UUID.randomUUID(), orderId, tax, base, rateBasisPoints, Instant.now(clock)));
        TaxAssessment saved = assessments.save(row);
        metrics.recordRecompute(existing.isPresent() ? "updated" : "created");
        return TaxResult.of(saved);
    }

    /** The order's current combined tax — the single row if present, else a zero (no-row) result. */
    @Transactional(readOnly = true)
    public TaxResult currentTax(UUID orderId) {
        orders.findById(orderId)
            .orElseThrow(() -> TaxApplicationException.orderNotFound(orderId.toString()));
        return assessments.findByOrderId(orderId).map(TaxResult::of).orElseGet(() -> TaxResult.none(orderId));
    }

    // ─── Pure computation (deterministic, side-effect-free; package-private for direct test) ────

    /**
     * EXEMPT-SKIP: the order's non-exempt taxable base. A tax-exempt customer yields 0; an exempt
     * line contributes 0; only non-exempt, positive line bases are summed.
     */
    static long taxableBase(TaxableOrder order) {
        if (order.isCustomerExempt()) {
            return 0L;
        }
        return order.getLines().stream()
            .filter(l -> !l.isExempt())
            .mapToLong(TaxLine::getTaxableBaseMinor)
            .filter(b -> b > 0)
            .sum();
    }

    /**
     * Tax = round(taxableBase × rate), rate in basis points, integer minor units, half-up rounding.
     * Pure: depends only on its arguments. A zero base (the exempt path) yields exactly 0.
     */
    static long computeTax(long taxableBaseMinor, long rateBasisPoints) {
        if (taxableBaseMinor <= 0 || rateBasisPoints <= 0) {
            return 0L;
        }
        long product = Math.multiplyExact(taxableBaseMinor, rateBasisPoints);
        // round half-up over the basis-point denominator (both operands non-negative here)
        return Math.floorDiv(product + (BASIS_POINT_DENOMINATOR / 2), BASIS_POINT_DENOMINATOR);
    }

    // ─── Value type ─────────────────────────────────────────────────────────────────

    /**
     * The recompute / read result: {@code present} is true iff a combined tax row exists for the
     * order. When absent (fully exempt / nothing taxable), {@code taxAmountMinor} is 0 and
     * {@code assessmentId} is null.
     */
    public record TaxResult(UUID orderId, boolean present, long taxAmountMinor, UUID assessmentId) {
        static TaxResult of(TaxAssessment a) {
            return new TaxResult(a.getOrderId(), true, a.getTaxAmountMinor(), a.getId());
        }
        static TaxResult none(UUID orderId) {
            return new TaxResult(orderId, false, 0L, null);
        }
    }
}
