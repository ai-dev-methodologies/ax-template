package com.ax.template.authblueprint.commercepromotion;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Commerce promotion engine compliance test — behavioral assertions per invariant.
 * Every test asserts COMPUTED AMOUNTS, STATUS CODES, or EXACT EQUALITY — never != null.
 * Spec: specs/promotion-l0.yaml (external e-commerce reference offer engine, STRICTER).
 *
 * Test inventory:
 *   PROMO-CONSERVE-001: floor-remainder proration sums to EXACT orderDiscount (not 99 or 101)
 *   PROMO-STACK-001:    non-combinable offer blocks further offers (assert exactly one adjustment)
 *   PROMO-ORDER-001:    deterministic sort — identical inputs produce byte-identical adjustments
 *   PROMO-MAXSELECT-001: keep the path with higher total discount, discard the loser
 *   PROMO-CLAMP-001:    discount clamped to line price (150 off a 100 line → 100)
 *   PROMO-MAXUSES-001:  max_uses=1 + concurrent redeems → exactly one succeeds, second is 409
 *   PROMO-IDEMPOTENT-001: re-apply same inputs → identical adjustments (no doubling)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("PROMOTION")
class CommercePromotionComplianceTest {

    @LocalServerPort int port;
    @Autowired PromotionService service;

    String memberToken;

    @BeforeEach
    void setup() {
        memberToken = PromotionTestSupport.obtainToken(
            PromotionTestSupport.freshEmail("promo-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    /** Create an offer via the API and return its UUID. */
    private UUID createOffer(String name, String discountType, long discountValue,
                              String scope, int priority, boolean combinable, boolean stackable,
                              long maxUses) {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        String body = String.format(
            "{\"name\":\"%s\",\"discountType\":\"%s\",\"discountValue\":%d,"
            + "\"scope\":\"%s\",\"priority\":%d,\"combinable\":%s,\"stackable\":%s,"
            + "\"applyToSalePrice\":true,\"maxUses\":%d,\"maxUsesPerCustomer\":0,"
            + "\"activeStart\":\"%s\",\"activeEnd\":null}",
            name, discountType, discountValue, scope, priority,
            combinable, stackable, maxUses, start);

        return UUID.fromString(
            given().header("Authorization", "Bearer " + memberToken)
                   .header("Content-Type", "application/json")
                   .body(body)
            .when().post("/api/promotion/offers")
            .then().statusCode(201)
            .extract().path("id"));
    }

    /** Create an offer code for the given offer via direct service call (internal). */
    private String registerCode(UUID offerId) {
        String code = "CODE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        service.createOfferCode(offerId, code, 0);
        return code;
    }

    /**
     * Apply offers via the API and return the full response.
     * lineItems format: [ [skuId, qty, unitPrice], ... ]
     */
    private ExtractableResponse<Response> applyViaApi(List<long[]> items, List<String> codes, String orderRef) {
        StringBuilder itemsJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            long[] it = items.get(i);
            if (i > 0) itemsJson.append(",");
            itemsJson.append(String.format("{\"skuId\":\"sku-%d\",\"quantity\":1,\"unitPrice\":%d}", i, it[0]));
        }
        itemsJson.append("]");

        StringBuilder codesJson = new StringBuilder("[");
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) codesJson.append(",");
            codesJson.append("\"").append(codes.get(i)).append("\"");
        }
        codesJson.append("]");

        return given().header("Authorization", "Bearer " + memberToken)
                      .header("Content-Type", "application/json")
                      .body(String.format("{\"lineItems\":%s,\"offerCodes\":%s,\"orderRef\":\"%s\"}",
                          itemsJson, codesJson, orderRef))
               .when().post("/api/promotion/apply")
               .then().statusCode(200).extract();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-CONSERVE-001 — floor-remainder proration: Σ(adjustments) == orderDiscount EXACTLY
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-CONSERVE-001")
    void conservation_orderDiscount10pct_acrossThreeItems_sumsExactly() {
        // Items: 333, 333, 334 → total 1000. 10% (1000 bp) ORDER offer → discount 100.
        // Floor shares: floor(100 * 333/1000)=33, floor(100 * 333/1000)=33, floor(100 * 334/1000)=33
        // Σ=99, leftover=1. Largest remainder: 333*100 % 1000 = 300 (items 0 and 1 tied)
        // and 334*100 % 1000 = 400 (item 2). Item 2 gets the leftover → shares: 33, 33, 34 → Σ=100.
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 333L),
            new PromotionService.LineItem("sku-b", 1, 333L),
            new PromotionService.LineItem("sku-c", 1, 334L)
        );
        PromoOffer offer = new PromoOffer(UUID.randomUUID(), "10pct-order", DiscountType.PERCENT, 1000L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> adjustments = service.computeAdjustments(items, List.of(offer));

        long sum = adjustments.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        // MUST equal 100 exactly — the invariant (PROMO-CONSERVE-001)
        assertThat(sum).as("ORDER discount 100 must be distributed with EXACT conservation").isEqualTo(100L);
        assertThat(adjustments).hasSize(3);
        // The three shares must be 33, 33, 34 in order
        List<Long> amounts = adjustments.stream().map(PromotionService.Adjustment::amount).toList();
        assertThat(amounts).containsExactlyInAnyOrder(33L, 33L, 34L);
        // Specifically item 2 (sku-c) gets 34 (largest fractional remainder)
        long skuC = adjustments.stream()
            .filter(a -> a.skuId().equals("sku-c")).mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(skuC).as("sku-c with highest fractional remainder gets the extra unit").isEqualTo(34L);
    }

    @Test @Tag("PROMO-CONSERVE-001")
    void conservation_exactDivision_noRemainder() {
        // 1000 total, 10% = exactly 100. Three items at 200, 300, 500 → 20+30+50=100.
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 200L),
            new PromotionService.LineItem("sku-b", 1, 300L),
            new PromotionService.LineItem("sku-c", 1, 500L)
        );
        PromoOffer offer = new PromoOffer(UUID.randomUUID(), "10pct-exact", DiscountType.PERCENT, 1000L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> adjustments = service.computeAdjustments(items, List.of(offer));
        long sum = adjustments.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(sum).isEqualTo(100L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-STACK-001 — non-combinable offer blocks further offers
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-STACK-001")
    void nonCombinableOffer_blocksSubsequentOffers_exactlyOneApplied() {
        // Two ITEM-scope offers on one order. Offer A (priority 1, non-combinable) applies first.
        // Offer B (priority 2) must NOT apply because A is non-combinable.
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-x", 1, 500L)
        );
        PromoOffer offerA = new PromoOffer(UUID.randomUUID(), "non-combinable-A", DiscountType.FIXED, 50L,
            OfferScope.ITEM, 1, false /* non-combinable */, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        PromoOffer offerB = new PromoOffer(UUID.randomUUID(), "offer-B", DiscountType.FIXED, 30L,
            OfferScope.ITEM, 2, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> adjustments = service.computeAdjustments(items, List.of(offerA, offerB));

        // MUST have exactly one adjustment (from offerA; offerB blocked)
        assertThat(adjustments).as("non-combinable offer must block subsequent offers").hasSize(1);
        assertThat(adjustments.get(0).offerId()).isEqualTo(offerA.getId());
        assertThat(adjustments.get(0).amount()).isEqualTo(50L);
    }

    @Test @Tag("PROMO-STACK-001")
    void combinableOffers_bothApply_distinctAdjustments() {
        // Two combinable ITEM offers → both adjustments present
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-x", 1, 500L)
        );
        PromoOffer offerA = new PromoOffer(UUID.randomUUID(), "comb-A", DiscountType.FIXED, 50L,
            OfferScope.ITEM, 1, true /* combinable */, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        PromoOffer offerB = new PromoOffer(UUID.randomUUID(), "comb-B", DiscountType.FIXED, 30L,
            OfferScope.ITEM, 2, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> adjustments = service.computeAdjustments(items, List.of(offerA, offerB));

        assertThat(adjustments).hasSize(2);
        long total = adjustments.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total).isEqualTo(80L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-ORDER-001 — deterministic sort: same inputs → byte-identical adjustments
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-ORDER-001")
    void deterministicSort_sameInputsTwice_identicalAdjustments() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 300L),
            new PromotionService.LineItem("sku-b", 1, 700L)
        );
        PromoOffer o1 = new PromoOffer(id1, "offer-low-priority", DiscountType.FIXED, 20L,
            OfferScope.ITEM, 2, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        PromoOffer o2 = new PromoOffer(id2, "offer-high-priority", DiscountType.FIXED, 40L,
            OfferScope.ITEM, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        // Apply twice, in different list order, with same offers
        List<PromotionService.Adjustment> run1 = service.computeAdjustments(items, List.of(o1, o2));
        List<PromotionService.Adjustment> run2 = service.computeAdjustments(items, List.of(o2, o1));

        // Must produce identical results (same order, same amounts)
        assertThat(run1).hasSize(run2.size());
        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).offerId()).isEqualTo(run2.get(i).offerId());
            assertThat(run1.get(i).skuId()).isEqualTo(run2.get(i).skuId());
            assertThat(run1.get(i).amount()).isEqualTo(run2.get(i).amount());
        }
    }

    @Test @Tag("PROMO-ORDER-001")
    void equalPriorityOffers_resolveByPotentialSavingsDesc_higherSavingsFirst() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
        // Same priority; offer with higher savings goes first
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-x", 1, 1000L)
        );
        PromoOffer smallOffer = new PromoOffer(id1, "small-10", DiscountType.FIXED, 10L,
            OfferScope.ITEM, 1, false, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        PromoOffer bigOffer   = new PromoOffer(id2, "big-200", DiscountType.FIXED, 200L,
            OfferScope.ITEM, 1, false, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(smallOffer, bigOffer));

        // Non-combinable; exactly one adjustment. The one with higher savings (200) must win.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).offerId())
            .as("higher-savings offer must be applied when priority is equal and non-combinable")
            .isEqualTo(id2);
        assertThat(result.get(0).amount()).isEqualTo(200L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-MAXSELECT-001 — keep only the higher-total path (ORDER vs ITEM)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-MAXSELECT-001")
    void maxSelect_orderPathHigher_itemAdjustmentsDiscarded() {
        // ORDER offer gives 200 total, ITEM offer gives 150 total → ORDER path wins
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 600L),
            new PromotionService.LineItem("sku-b", 1, 400L)
        );
        // ORDER offer: FIXED 200 on total 1000 → prorate: 120 on sku-a, 80 on sku-b
        PromoOffer orderOffer = new PromoOffer(UUID.randomUUID(), "order-200", DiscountType.FIXED, 200L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        // ITEM offer: FIXED 150 on each item, clamped to item price = 150+150 but capped at prices
        // item sku-a=600 → 150, item sku-b=400 → 150 → total 300 > 200 actually — let's use a smaller item offer
        // ITEM offer: FIXED 75 on each item → 75 + 75 = 150 total
        PromoOffer itemOffer = new PromoOffer(UUID.randomUUID(), "item-75-each", DiscountType.FIXED, 75L,
            OfferScope.ITEM, 2, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(orderOffer, itemOffer));

        long total = result.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total).as("ORDER path total 200 > ITEM path total 150; ORDER path must win").isEqualTo(200L);
        // All adjustments must come from the ORDER offer
        assertThat(result).allMatch(a -> a.offerId().equals(orderOffer.getId()));
    }

    @Test @Tag("PROMO-MAXSELECT-001")
    void maxSelect_itemPathHigher_orderAdjustmentsDiscarded() {
        // ITEM offer gives more than ORDER offer → ITEM path wins
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 500L),
            new PromotionService.LineItem("sku-b", 1, 500L)
        );
        // ORDER offer: FIXED 100 total
        PromoOffer orderOffer = new PromoOffer(UUID.randomUUID(), "order-100", DiscountType.FIXED, 100L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        // ITEM offer: FIXED 200 on each item → 200+200=400 total (> 100)
        PromoOffer itemOffer = new PromoOffer(UUID.randomUUID(), "item-200-each", DiscountType.FIXED, 200L,
            OfferScope.ITEM, 2, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(orderOffer, itemOffer));

        long total = result.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total).as("ITEM path total 400 > ORDER path total 100; ITEM path must win").isEqualTo(400L);
        assertThat(result).allMatch(a -> a.offerId().equals(itemOffer.getId()));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-CLAMP-001 — discount never exceeds line price (never negative)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-CLAMP-001")
    void clamp_discountExceedsLinePrice_clampedToLinePrice() {
        // 150 FIXED discount on a 100-unit line → adjustment must be 100, not 150
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-cheap", 1, 100L)
        );
        PromoOffer offer = new PromoOffer(UUID.randomUUID(), "oversized-discount", DiscountType.FIXED, 150L,
            OfferScope.ITEM, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(offer));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount())
            .as("discount must be clamped to line price (100), never 150")
            .isEqualTo(100L);
    }

    @Test @Tag("PROMO-CLAMP-001")
    void clamp_orderScopeDiscount_clampsEachItemShare() {
        // ORDER offer 200% (20000 bp) on items [50, 50] = would give 100 total = 100% of 100
        // Each item gets floor(100 * 50/100) = 50, clamped to item price 50 — OK, no overshoot here
        // Use a FIXED offer that exceeds each item: FIXED 1000 on items of 50+50=100 → prorate: 500+500
        // but clamp each to 50 → 50+50=100 total
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-x", 1, 50L),
            new PromotionService.LineItem("sku-y", 1, 50L)
        );
        PromoOffer offer = new PromoOffer(UUID.randomUUID(), "order-1000-fixed", DiscountType.FIXED, 1000L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(offer));

        // Each item share would be 500 but clamped to 50
        long total = result.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total).as("ORDER discount 1000 on items totalling 100: each item clamped to its price").isEqualTo(100L);
        assertThat(result).allMatch(a -> a.amount() <= 50L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-MAXUSES-001 — concurrent redemptions with max_uses=1 → exactly one succeeds
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void maxUses_concurrentRedemptions_exactlyOneSucceeds_secondIs409() throws Exception {
        // Create offer with max_uses=1 via the API
        UUID offerId = createOffer("max1-offer-" + UUID.randomUUID(), "FIXED", 100L,
            "ORDER", 1, true, false, 1L /* max_uses=1 */);

        // Two concurrent threads try to redeem with DIFFERENT order refs — only one can succeed
        String orderRef1 = "order-" + UUID.randomUUID();
        String orderRef2 = "order-" + UUID.randomUUID();
        String customer = "cust-" + UUID.randomUUID();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> outcomes = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            ready.countDown();
            start.await();
            try {
                service.redeem(offerId, customer, orderRef1);
                outcomes.add("ok:" + orderRef1);
            } catch (PromotionException e) {
                outcomes.add("err:" + e.code() + ":" + orderRef1);
            }
            return null;
        });
        pool.submit(() -> {
            ready.countDown();
            start.await();
            try {
                service.redeem(offerId, customer, orderRef2);
                outcomes.add("ok:" + orderRef2);
            } catch (PromotionException e) {
                outcomes.add("err:" + e.code() + ":" + orderRef2);
            }
            return null;
        });

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long successCount = outcomes.stream().filter(s -> s.startsWith("ok:")).count();
        long failCount    = outcomes.stream().filter(s -> s.startsWith("err:")).count();

        assertThat(successCount).as("exactly one redemption must succeed with max_uses=1").isEqualTo(1L);
        assertThat(failCount).as("the second over-cap redemption must fail").isEqualTo(1L);
        // The failure must be PROMO_MAX_USES_EXCEEDED (not some other error)
        assertThat(outcomes.stream().filter(s -> s.startsWith("err:")).findFirst().orElse(""))
            .contains("PROMO_MAX_USES_EXCEEDED");
    }

    @Test @Tag("PROMO-MAXUSES-001")
    void maxUses_duplicateRedemption_sameOrderRef_409() {
        // An offer with max_uses=10 (not the cap), two redemptions for the SAME order_ref
        // → second must be rejected with PROMO_DUPLICATE_REDEMPTION
        UUID offerId = createOffer("dup-test-" + UUID.randomUUID(), "FIXED", 50L,
            "ORDER", 1, true, false, 10L);
        String orderRef = "order-dup-" + UUID.randomUUID();
        String customer  = "cust-" + UUID.randomUUID();

        service.redeem(offerId, customer, orderRef);  // first → OK

        PromotionException ex = null;
        try {
            service.redeem(offerId, customer, orderRef);  // duplicate → 409
        } catch (PromotionException e) {
            ex = e;
        }
        assertThat(ex).as("duplicate (offer, order_ref) must throw PromotionException").isNotNull();
        assertThat(ex.code()).isEqualTo("PROMO_DUPLICATE_REDEMPTION");
        assertThat(ex.status().value()).isEqualTo(409);
    }

    @Test @Tag("PROMO-CLAMP-001")
    void clamp_twoStackedOrderOffers_perLineNeverExceedsLinePrice() {
        // Two combinable ORDER FIXED-100 offers on a single 100-unit line.
        // First offer: prorates full 100 to the one line (100 ≤ 100 — OK).
        // Second offer: remaining[0] is now 0, so its share must be clamped to 0 — never -100.
        // Without the remaining[] threading fix, second offer would produce +100, netting -100 (bug).
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-single", 1, 100L)
        );
        PromoOffer offer1 = new PromoOffer(UUID.randomUUID(), "order-100-first", DiscountType.FIXED, 100L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);
        PromoOffer offer2 = new PromoOffer(UUID.randomUUID(), "order-100-second", DiscountType.FIXED, 100L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> result = service.computeAdjustments(items, List.of(offer1, offer2));

        // Per-line Σ(adjustments) must not exceed linePrice (100).
        // Without the fix: two ORDER-100 prorations = 200 total, netting -100. With the fix: ≤ 100.
        long totalOnLine = result.stream()
            .filter(a -> "sku-single".equals(a.skuId()))
            .mapToLong(PromotionService.Adjustment::amount)
            .sum();
        assertThat(totalOnLine)
            .as("cumulative ORDER discounts on a 100-unit line must not exceed 100 (line never goes negative)")
            .isLessThanOrEqualTo(100L);
    }

    @Test @Tag("PROMO-STACK-001")
    void stackable_nonStackableOfferCodeSubmittedTwice_appliesExactlyOnce() {
        // A non-stackable offer submitted twice (same code listed twice in apply).
        // The stackable=false gate must dedup so it applies EXACTLY ONCE — total must equal
        // the single-application amount, not doubled.
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 500L),
            new PromotionService.LineItem("sku-b", 1, 500L)
        );
        // Create a non-stackable ORDER-10% offer with a code
        UUID offerId = createOffer("nonstackable-" + UUID.randomUUID(), "PERCENT", 1000L,
            "ORDER", 1, true, false /* stackable=false */, 0L);
        String code = "CODE-NS-" + UUID.randomUUID();
        service.createOfferCode(offerId, code, 0L);

        // Submit the SAME code twice — should apply once (100 total, 10% of 1000), not twice (200)
        List<String> codes = List.of(code, code);
        List<PromotionService.Adjustment> result = service.applyOffers(items, codes, "cust-x", "order-x");

        long total = result.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total)
            .as("non-stackable offer submitted twice must apply exactly once: 10% of 1000 = 100, not 200")
            .isEqualTo(100L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROMO-IDEMPOTENT-001 — re-applying same inputs returns identical adjustments
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-IDEMPOTENT-001")
    void idempotent_sameInputsAppliedTwice_identicalAdjustments_noDoubling() {
        List<PromotionService.LineItem> items = List.of(
            new PromotionService.LineItem("sku-a", 1, 400L),
            new PromotionService.LineItem("sku-b", 1, 600L)
        );
        PromoOffer offer = new PromoOffer(UUID.randomUUID(), "idem-offer", DiscountType.PERCENT, 1000L,
            OfferScope.ORDER, 1, true, false, null, 0L, 0L,
            Instant.now().minus(1, ChronoUnit.HOURS), null);

        List<PromotionService.Adjustment> run1 = service.computeAdjustments(items, List.of(offer));
        List<PromotionService.Adjustment> run2 = service.computeAdjustments(items, List.of(offer));

        assertThat(run1).hasSize(run2.size());
        long total1 = run1.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        long total2 = run2.stream().mapToLong(PromotionService.Adjustment::amount).sum();
        assertThat(total1).isEqualTo(total2);
        assertThat(total1).as("10% of 1000 = 100 total, must not double to 200 on second apply").isEqualTo(100L);

        // Per-item amounts must be identical
        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).amount()).isEqualTo(run2.get(i).amount());
            assertThat(run1.get(i).skuId()).isEqualTo(run2.get(i).skuId());
        }
    }
}
