package com.ax.template.authblueprint.offereligibility;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Offer-eligibility — the SOLE evaluator of the applicability predicate. This is the WHO/WHICH-ITEMS
 * gate, distinct from discount MATH (promotion-l0 owns the math; this service owns applicability only).
 *
 * <p>The correctness property: eligibility is evaluated DETERMINISTICALLY from the offer's declared
 * criteria, and an ineligible offer can never reach the discount-application path. Evaluation is
 * <b>fail-closed</b> (deny by default): unknown or missing criteria ⇒ NOT applied. {@link #decide}
 * is a pure function — no I/O, no wall-clock read, no mutation — so the same offer + context always
 * yields the same recorded decision + reason.
 *
 * <p>Two independent gates (evaluated in a fixed order; the first failing gate's reason is recorded):
 * <ol>
 *   <li><b>FAIL-CLOSED criteria completeness</b> — qualifier, target, and eligibility criteria must
 *       all be declared, and the context must carry a resolvable customer; else NOT applied.</li>
 *   <li><b>SEGMENT-ELIGIBILITY</b> — the customer must be on the allow-list OR in the matched segment.</li>
 *   <li><b>QUALIFIER-MINQTY</b> — the qualifying lines must total at least {@code minQualifierQty},
 *       and a target line must be present; else NOT applied (BOGO-style).</li>
 * </ol>
 */
@Service
public class OfferEligibilityService {

    private final EligibilityOfferRepository offers;
    private final OfferEligibilityMetrics metrics;
    private final Clock clock;

    public OfferEligibilityService(EligibilityOfferRepository offers,
                                   OfferEligibilityMetrics metrics, Clock clock) {
        this.offers = offers;
        this.metrics = metrics;
        this.clock = clock;
    }

    // ─── Offer declaration lifecycle ────────────────────────────────────────────────

    @Transactional
    public EligibilityOffer createOffer(String name, String qualifierSku, String qualifierTag,
                                        int minQualifierQty, String targetSku, String targetTag,
                                        long discountBasisPoints, String eligibleSegment,
                                        Set<UUID> eligibleCustomerIds) {
        if (minQualifierQty < 1) {
            throw OfferEligibilityException.invalidOffer("min_qualifier_qty must be >= 1");
        }
        if (discountBasisPoints < 0) {
            throw OfferEligibilityException.invalidOffer("discount_basis_points must be >= 0");
        }
        EligibilityOffer offer = new EligibilityOffer(UUID.randomUUID(), name,
            blankToNull(qualifierSku), blankToNull(qualifierTag), minQualifierQty,
            blankToNull(targetSku), blankToNull(targetTag), discountBasisPoints,
            blankToNull(eligibleSegment),
            eligibleCustomerIds == null ? Set.of() : eligibleCustomerIds,
            Instant.now(clock));
        EligibilityOffer saved = offers.saveAndFlush(offer);
        metrics.recordOfferCreated();
        return saved;
    }

    @Transactional(readOnly = true)
    public EligibilityOffer getOffer(UUID id) {
        return offers.findById(id)
            .orElseThrow(() -> OfferEligibilityException.offerNotFound(id.toString()));
    }

    // ─── Core: evaluate eligibility (the sole evaluator) ────────────────────────────

    /**
     * Evaluate {@code offerId} against the SUPPLIED {@code ctx}. Trust boundary: the customer
     * identity and segments in {@code ctx} are taken as given — this evaluator does NOT bind them to
     * an authenticated principal and assumes a trusted upstream resolved/authorized the context. It
     * decides applicability only; it is not the authorization seam (see the controller Javadoc).
     */
    @Transactional(readOnly = true)
    public EligibilityDecision evaluate(UUID offerId, EvaluationContext ctx) {
        EligibilityOffer offer = offers.findById(offerId)
            .orElseThrow(() -> OfferEligibilityException.offerNotFound(offerId.toString()));
        EligibilityDecision decision = decide(offer, ctx);
        metrics.recordEvaluation(decision.applied() ? "applied" : "not_applied", decision.reason().name());
        return decision;
    }

    /**
     * The pure decision function — deterministic, fail-closed, side-effect-free. Package-private so it
     * can be exercised directly. Returns exactly one {@link EligibilityDecision}; the FIRST failing
     * gate's reason is the recorded reason, and only the all-pass path returns an applied decision.
     */
    EligibilityDecision decide(EligibilityOffer offer, EvaluationContext ctx) {
        // 1. FAIL-CLOSED: every criterion must be DECLARED (deny by default on missing/unknown).
        boolean hasQualifierCriteria = isPresent(offer.getQualifierSku()) || isPresent(offer.getQualifierTag());
        if (!hasQualifierCriteria) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.MISSING_QUALIFIER_CRITERIA);
        }
        boolean hasTargetCriteria = isPresent(offer.getTargetSku()) || isPresent(offer.getTargetTag());
        if (!hasTargetCriteria) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.MISSING_TARGET_CRITERIA);
        }
        boolean hasEligibilityCriteria = !offer.getEligibleCustomerIds().isEmpty() || isPresent(offer.getEligibleSegment());
        if (!hasEligibilityCriteria) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.MISSING_ELIGIBILITY_CRITERIA);
        }
        if (ctx == null || ctx.customerId() == null) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.UNKNOWN_CUSTOMER);
        }

        // 2. SEGMENT-ELIGIBILITY: customer-xref allow-list OR matched segment.
        boolean inAllowList = offer.getEligibleCustomerIds().contains(ctx.customerId());
        boolean inSegment = isPresent(offer.getEligibleSegment())
            && ctx.customerSegments() != null
            && ctx.customerSegments().contains(offer.getEligibleSegment());
        if (!inAllowList && !inSegment) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.CUSTOMER_NOT_ELIGIBLE);
        }

        // 3. QUALIFIER-MINQTY (BOGO-style): the qualifying lines must total >= minQualifierQty.
        List<Line> lines = ctx.lines() == null ? List.of() : ctx.lines();
        long qualifyingQty = lines.stream()
            .filter(l -> matches(l, offer.getQualifierSku(), offer.getQualifierTag()))
            .mapToInt(Line::quantity)
            .filter(q -> q > 0)
            .sum();
        if (qualifyingQty < offer.getMinQualifierQty()) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.QUALIFIER_MIN_QTY_NOT_MET);
        }

        // 4. A target line must exist for the discount to land on.
        boolean hasTargetLine = lines.stream()
            .anyMatch(l -> matches(l, offer.getTargetSku(), offer.getTargetTag()));
        if (!hasTargetLine) {
            return EligibilityDecision.notApplied(offer.getId(), EligibilityReason.NO_TARGET_LINE);
        }

        // All gates passed — applicable.
        return EligibilityDecision.applied(offer.getId());
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return isPresent(s) ? s : null;
    }

    /** A line matches when it carries the declared SKU OR the declared tag (whichever is present). */
    private static boolean matches(Line line, String sku, String tag) {
        return (isPresent(sku) && sku.equals(line.sku()))
            || (isPresent(tag) && tag.equals(line.tag()));
    }

    // ─── Value types ────────────────────────────────────────────────────────────────

    /** An order line: an optional SKU, an optional tag, and a quantity (minor-unit money is not relevant here). */
    public record Line(String sku, String tag, int quantity) {}

    /** The order/customer context an offer is evaluated against. */
    public record EvaluationContext(UUID customerId, Set<String> customerSegments, List<Line> lines) {}

    /** The recorded decision: applied iff {@code reason == ELIGIBLE}; otherwise a fail-closed not-applied reason. */
    public record EligibilityDecision(UUID offerId, boolean applied, EligibilityReason reason) {
        static EligibilityDecision applied(UUID offerId) {
            return new EligibilityDecision(offerId, true, EligibilityReason.ELIGIBLE);
        }
        static EligibilityDecision notApplied(UUID offerId, EligibilityReason reason) {
            return new EligibilityDecision(offerId, false, reason);
        }
    }
}
