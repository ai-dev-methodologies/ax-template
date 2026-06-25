package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Commerce promotion engine — sole orchestrator. Implements all PROMO-* invariants:
 *
 * PROMO-CONSERVE-001: ORDER-scope discount prorated with floor-remainder distribution so
 *   Σ(adjustments) == orderDiscount EXACTLY (no floating-point, integer arithmetic only).
 *   Math.multiplyExact used for basis-points multiplication to fail-closed on overflow.
 *
 * PROMO-STACK-001: combinable (other-offers gate) and stackable (same-offer gate) are
 *   independent gates. stackable=false means the offer applies AT MOST ONCE per call —
 *   duplicate codes for the same non-stackable offer are deduped in resolveOffers().
 *   A non-combinable offer, once applied, blocks all further offers in its path.
 *
 * PROMO-ORDER-001: deterministic sort by (priority ASC, potential_savings DESC, id ASC) —
 *   stable, byte-identical results for identical inputs.
 *
 * PROMO-MAXSELECT-001: if both ORDER-path and ITEM-path are present in the offer set,
 *   keep only the path with the larger total discount, discard the loser's adjustments.
 *
 * PROMO-CLAMP-001: each item adjustment is clamped to [0, linePrice] — line never goes negative.
 *
 * PROMO-MAXUSES-001: redemption acquires the offer row under PESSIMISTIC_WRITE then checks
 *   counts; the UNIQUE(offer_id, order_ref) constraint is the atomic backstop.
 *
 * PROMO-IDEMPOTENT-001: re-applying the same inputs returns identical adjustments (pure function).
 */
@Service
public class PromotionService {

    /** Basis-points denominator: 10 000 bp = 100%. PERCENT discount = value * bp / 10000. */
    static final long BASIS_POINTS = 10_000L;

    private final PromoOfferRepository offers;
    private final MemberWriter members;
    private final PromotionMetrics metrics;
    private final Clock clock;

    public PromotionService(PromoOfferRepository offers, MemberWriter members,
                            PromotionMetrics metrics, Clock clock) {
        this.offers = offers;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    // ─── Offer lifecycle ──────────────────────────────────────────────────────────

    @Transactional
    public PromoOffer createOffer(String name, DiscountType discountType, long discountValue,
                             OfferScope scope, int priority, boolean combinable, boolean stackable,
                             Boolean applyToSalePrice, long maxUses, long maxUsesPerCustomer,
                             Instant activeStart, Instant activeEnd) {
        if (discountValue < 0) throw PromotionException.invalidOffer("discount_value must be >= 0");
        if (discountType == DiscountType.PERCENT && discountValue > BASIS_POINTS) {
            throw PromotionException.invalidOffer("PERCENT discount_value must be <= 10000 basis-points (100%)");
        }
        PromoOffer o = new PromoOffer(UUID.randomUUID(), name, discountType, discountValue,
            scope, priority, combinable, stackable, applyToSalePrice, maxUses, maxUsesPerCustomer,
            activeStart != null ? activeStart : Instant.now(clock), activeEnd);
        metrics.recordApply("create_ok");
        return offers.saveAndFlush(o);
    }

    @Transactional(readOnly = true)
    public PromoOffer getOffer(UUID id) {
        return offers.findById(id).orElseThrow(() -> PromotionException.offerNotFound(id.toString()));
    }

    // ─── Core: apply offers to a line-item set ────────────────────────────────────

    /**
     * PROMO-IDEMPOTENT-001: pure function — same inputs always yield same outputs.
     * offerCodes may be empty (directly applied offers are out of scope for this surface;
     * callers pass codes to resolve which offers to apply).
     * Returns the adjustment set — one Adjustment per (offerId, skuId or "ORDER") pair.
     */
    @Transactional(readOnly = true)
    public List<Adjustment> applyOffers(List<LineItem> lineItems, List<String> offerCodes,
                                        String customerId, String orderRef) {
        if (lineItems == null || lineItems.isEmpty()) {
            metrics.recordApply("no_items");
            return List.of();
        }

        // Resolve codes → offers
        List<PromoOffer> resolved = resolveOffers(offerCodes);
        if (resolved.isEmpty()) {
            metrics.recordApply("no_offers");
            return List.of();
        }

        List<Adjustment> result = computeAdjustments(lineItems, resolved);
        metrics.recordApply("ok");
        return result;
    }

    /**
     * Compute adjustments without any side effects. This is the pure engine
     * invoked by applyOffers (read path) and also callable from tests directly.
     *
     * PROMO-ORDER-001: sort offers by (priority ASC, potential_savings DESC, id ASC).
     * PROMO-STACK-001: non-combinable offer, once applied, blocks further offers.
     * PROMO-MAXSELECT-001: keep the higher-total path between ORDER and ITEM offers.
     * PROMO-CLAMP-001: adjustment per item clamped to [0, linePrice].
     * PROMO-CONSERVE-001: ORDER-scope discount distributed via floor-remainder.
     */
    List<Adjustment> computeAdjustments(List<LineItem> lineItems, List<PromoOffer> offerList) {
        long orderTotal = lineItems.stream().mapToLong(LineItem::unitPrice).map(p -> p > 0 ? p : 0).sum();

        // Sort: (priority ASC, potential_savings DESC, id ASC) — PROMO-ORDER-001
        // potential_savings covers both ORDER-scope (vs orderTotal) and ITEM-scope (sum across items).
        List<PromoOffer> sorted = new ArrayList<>(offerList);
        sorted.sort(Comparator
            .comparingInt(PromoOffer::getPriority)
            .thenComparing(Comparator.comparingLong((PromoOffer o) -> computePotentialSavings(o, lineItems, orderTotal)).reversed())
            .thenComparing(o -> o.getId().toString()));

        // Separate into ORDER-scope and ITEM-scope candidates
        List<PromoOffer> orderOffers = sorted.stream().filter(o -> o.getScope() == OfferScope.ORDER).toList();
        List<PromoOffer> itemOffers  = sorted.stream().filter(o -> o.getScope() == OfferScope.ITEM).toList();

        List<Adjustment> orderPath = applyOrderScopeOffers(orderOffers, lineItems, orderTotal);
        List<Adjustment> itemPath  = applyItemScopeOffers(itemOffers, lineItems);

        long orderPathTotal = orderPath.stream().mapToLong(Adjustment::amount).sum();
        long itemPathTotal  = itemPath.stream().mapToLong(Adjustment::amount).sum();

        // PROMO-MAXSELECT-001: if BOTH paths produced adjustments keep the larger
        if (!orderPath.isEmpty() && !itemPath.isEmpty()) {
            return orderPathTotal >= itemPathTotal ? orderPath : itemPath;
        }
        if (!orderPath.isEmpty()) return orderPath;
        return itemPath;
    }

    /**
     * Apply ORDER-scope offers respecting combinable gate.
     * PROMO-CONSERVE-001: the total ORDER discount is prorated across items by floor-remainder.
     * PROMO-CLAMP-001: a shared remaining[] per line is threaded across all stacked ORDER offers
     * so that the cumulative ORDER discount on any single line never exceeds its price.
     */
    private List<Adjustment> applyOrderScopeOffers(List<PromoOffer> orderOffers, List<LineItem> lineItems, long orderTotal) {
        List<Adjustment> result = new ArrayList<>();
        boolean combinabilityBroken = false;
        // Track remaining price per item across all stacked ORDER offers (PROMO-CLAMP-001).
        long[] remaining = lineItems.stream().mapToLong(li -> Math.max(li.unitPrice(), 0)).toArray();

        for (PromoOffer offer : orderOffers) {
            if (combinabilityBroken) break;

            long orderDiscount = computeOrderLevelSavings(offer, orderTotal);
            if (orderDiscount <= 0) continue;

            // Prorate across items — PROMO-CONSERVE-001 — clamped to remaining per-item balance
            List<Adjustment> prorated = prorateOrderDiscount(offer, orderDiscount, lineItems, remaining);
            result.addAll(prorated);

            if (!offer.isCombinable()) {
                combinabilityBroken = true;  // combinable gate
            }
        }
        return result;
    }

    /**
     * Apply ITEM-scope offers respecting combinable/stackable gates.
     * PROMO-CLAMP-001: each item adjustment clamped to [0, linePrice].
     */
    private List<Adjustment> applyItemScopeOffers(List<PromoOffer> itemOffers, List<LineItem> lineItems) {
        List<Adjustment> result = new ArrayList<>();
        boolean combinabilityBroken = false;
        // Track remaining price per item after previous adjustments (for clamping)
        long[] remaining = lineItems.stream().mapToLong(li -> Math.max(li.unitPrice(), 0)).toArray();

        for (PromoOffer offer : itemOffers) {
            if (combinabilityBroken) break;

            boolean anyApplied = false;
            for (int i = 0; i < lineItems.size(); i++) {
                LineItem item = lineItems.get(i);
                long linePrice = Math.max(item.unitPrice(), 0);
                if (linePrice == 0) continue;

                long rawDiscount = computeItemLevelSavings(offer, linePrice);
                // PROMO-CLAMP-001: clamp to remaining price for this item
                long clamped = Math.min(rawDiscount, remaining[i]);
                if (clamped <= 0) continue;

                result.add(new Adjustment(offer.getId(), offer.getName(), item.skuId(), clamped));
                remaining[i] -= clamped;
                anyApplied = true;
            }

            if (anyApplied && !offer.isCombinable()) {
                combinabilityBroken = true;  // PROMO-STACK-001
            }
        }
        return result;
    }

    /**
     * PROMO-CONSERVE-001 — floor-remainder distribution.
     * Each item share = floor(orderDiscount * itemAmount / totalAmount).
     * Leftover minor units distributed one-by-one to items with the largest fractional part,
     * then by item index as stable tie-break. Σ(shares) == orderDiscount EXACTLY.
     *
     * PROMO-CLAMP-001: each share is further clamped to remaining[i] (the caller-owned
     * per-item running balance) so cumulative ORDER discounts on one line never exceed its price.
     * remaining[i] is decremented in-place for each adjustment produced.
     */
    private List<Adjustment> prorateOrderDiscount(PromoOffer offer, long orderDiscount,
                                                   List<LineItem> lineItems, long[] remaining) {
        long orderTotal = lineItems.stream().mapToLong(li -> Math.max(li.unitPrice(), 0)).sum();
        if (orderTotal == 0) return List.of();

        int n = lineItems.size();
        long[] shares = new long[n];
        long[] fractions = new long[n];  // scaled remainder for tie-breaking

        for (int i = 0; i < n; i++) {
            long price = Math.max(lineItems.get(i).unitPrice(), 0);
            // Use Math.multiplyExact to fail-closed on overflow
            long numerator = Math.multiplyExact(orderDiscount, price);
            shares[i] = numerator / orderTotal;
            fractions[i] = numerator % orderTotal;
        }

        long distributed = 0;
        for (long s : shares) distributed += s;
        long leftover = orderDiscount - distributed;

        // Distribute leftover: give one extra to items with largest fractional remainder
        // Tie-break by index (stable). Build an index array sorted by remainder DESC, then index ASC.
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> {
            int cmp = Long.compare(fractions[b], fractions[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        for (int k = 0; k < leftover; k++) {
            shares[indices[k]]++;
        }

        // Clamp each share to remaining[i] (PROMO-CLAMP-001) and build adjustments.
        // remaining[i] is updated so subsequent ORDER offers cannot over-discount the same line.
        List<Adjustment> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long clamped = Math.min(shares[i], remaining[i]);
            if (clamped > 0) {
                result.add(new Adjustment(offer.getId(), offer.getName(), lineItems.get(i).skuId(), clamped));
                remaining[i] -= clamped;
            }
        }
        return result;
    }

    /** Compute the order-level discount this offer would produce for the given order total. */
    private long computeOrderLevelSavings(PromoOffer offer, long orderTotal) {
        if (offer.getScope() != OfferScope.ORDER) return 0L;
        return applyDiscount(offer, orderTotal);
    }

    /** Compute the item-level discount this offer would produce for the given line price. */
    private long computeItemLevelSavings(PromoOffer offer, long linePrice) {
        if (offer.getScope() != OfferScope.ITEM) return 0L;
        return applyDiscount(offer, linePrice);
    }

    /**
     * Total potential savings for sort purposes — PROMO-ORDER-001.
     * For ORDER-scope: savings on orderTotal.
     * For ITEM-scope: sum of savings across all line items (unclamped, for ranking only).
     */
    private long computePotentialSavings(PromoOffer offer, List<LineItem> lineItems, long orderTotal) {
        if (offer.getScope() == OfferScope.ORDER) {
            return computeOrderLevelSavings(offer, orderTotal);
        }
        // ITEM scope: sum unclamped discount across items
        return lineItems.stream()
            .mapToLong(li -> computeItemLevelSavings(offer, Math.max(li.unitPrice(), 0)))
            .sum();
    }

    /**
     * Core discount arithmetic — integer only, no double/float.
     * PERCENT: Math.multiplyExact(value * basisPoints, amount) / BASIS_POINTS — fail-closed on overflow.
     * FIXED: discount_value directly.
     */
    private long applyDiscount(PromoOffer offer, long amount) {
        return switch (offer.getDiscountType()) {
            case PERCENT -> {
                // discountValue is basis-points (e.g. 1000 = 10%)
                // result = floor(amount * discountValue / 10000)
                long numerator = Math.multiplyExact(amount, offer.getDiscountValue());
                yield numerator / BASIS_POINTS;
            }
            case FIXED -> Math.min(offer.getDiscountValue(), amount);
        };
    }

    // ─── Redemption ───────────────────────────────────────────────────────────────

    /**
     * PROMO-MAXUSES-001: acquires offer under PESSIMISTIC_WRITE, checks counts,
     * then inserts OfferRedemption. UNIQUE(offer_id, order_ref) is the atomic backstop:
     * a concurrent insert for the same (offer, orderRef) will throw DataIntegrityViolationException → 409.
     */
    @Transactional
    public PromoOfferRedemption redeem(UUID offerId, String customerId, String orderRef) {
        PromoOffer offer = offers.findByIdForUpdate(offerId)
            .orElseThrow(() -> PromotionException.offerNotFound(offerId.toString()));

        // PROMO-MAXUSES-001 global cap check (after acquiring the lock)
        if (offer.getMaxUses() > 0) {
            long used = offers.countRedemptionsByOfferId(offerId);
            if (used >= offer.getMaxUses()) {
                metrics.recordRedeem("max_uses_exceeded");
                throw PromotionException.maxUsesExceeded();
            }
        }

        // per-customer cap check
        if (offer.getMaxUsesPerCustomer() > 0) {
            long customerUsed = offers.countRedemptionsByOfferIdAndCustomerId(offerId, customerId);
            if (customerUsed >= offer.getMaxUsesPerCustomer()) {
                metrics.recordRedeem("max_uses_per_customer_exceeded");
                throw PromotionException.maxUsesPerCustomerExceeded();
            }
        }

        try {
            PromoOfferRedemption r = members.persist(
                new PromoOfferRedemption(UUID.randomUUID(), offerId, customerId, orderRef, Instant.now(clock)));
            // Force the INSERT SQL now so a UNIQUE constraint violation surfaces within this try/catch.
            // Without flush(), H2/Hibernate defers the SQL to commit time — outside our catch scope.
            offers.flush();
            metrics.recordRedeem("ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            // UNIQUE(offer_id, order_ref) backstop — duplicate redemption for same order
            metrics.recordRedeem("duplicate_redemption");
            throw PromotionException.duplicateRedemption();
        }
    }

    // ─── OfferCode management ─────────────────────────────────────────────────────

    @Transactional
    public PromoOfferCode createOfferCode(UUID offerId, String code, long maxUses) {
        offers.findById(offerId).orElseThrow(() -> PromotionException.offerNotFound(offerId.toString()));
        return members.persist(new PromoOfferCode(UUID.randomUUID(), offerId, code, maxUses));
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────────

    /**
     * Resolve offer codes to PromoOffer instances.
     * PROMO-STACK-001 (stackable gate): a non-stackable offer (stackable=false) appears AT MOST ONCE
     * per apply-call regardless of how many codes resolve to it. Duplicate codes for the same
     * non-stackable offer are silently deduplicated. A stackable=true offer may appear multiple
     * times (e.g. two distinct codes both backed by the same stackable offer).
     */
    private List<PromoOffer> resolveOffers(List<String> offerCodes) {
        if (offerCodes == null || offerCodes.isEmpty()) return List.of();
        List<PromoOffer> result = new ArrayList<>();
        // Track offer IDs that have already been added for non-stackable offers
        Set<UUID> seenNonStackable = new LinkedHashSet<>();
        for (String code : offerCodes) {
            PromoOfferCode oc = offers.findCodeByCode(code)
                .orElseThrow(() -> PromotionException.codeNotFound(code));
            PromoOffer o = offers.findById(oc.getOfferId())
                .orElseThrow(() -> PromotionException.offerNotFound(oc.getOfferId().toString()));
            if (!o.isStackable()) {
                // Non-stackable: include only the first occurrence
                if (seenNonStackable.add(o.getId())) {
                    result.add(o);
                }
            } else {
                result.add(o);
            }
        }
        return result;
    }

    // ─── Value types ──────────────────────────────────────────────────────────────

    /**
     * A line item in an order: sku, quantity, and unit price in minor units (BIGINT).
     * Money is always integer minor units — never double.
     */
    public record LineItem(String skuId, int quantity, long unitPrice) {}

    /**
     * One discount adjustment applied to a specific item (or the order, skuId="ORDER").
     * {@code amount} is always >= 0, clamped to the line price (PROMO-CLAMP-001).
     */
    public record Adjustment(UUID offerId, String offerName, String skuId, long amount) {}
}
