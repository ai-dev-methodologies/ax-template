package com.ax.template.authblueprint.inventoryreservation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * two-axis-inventory-reservation-l0 compliance — verified against the live inventory-reservation
 * reference workload. The invariant: AVAILABLE = onHand − reserved is a DERIVED quantity (never
 * stored); a reserve requires available ≥ q (422 else) and only increments reserved (onHand
 * untouched); a commit decrements BOTH axes (goods leave); a release decrements reserved alone
 * (hold frees); a reservation moves HELD → (COMMITTED|RELEASED) exactly once (409 otherwise);
 * reserved == Σ(HELD quantities) and 0 ≤ reserved ≤ onHand at every step; concurrent reserves
 * serialize so exactly available/q win.
 * Spec: specs/two-axis-inventory-reservation-l0.yaml (ATP available/reserved + Saga two-phase + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("INVENTORYRESERVATION")
class InventoryReservationComplianceTest {

    @LocalServerPort int port;
    @Autowired InventoryReservationService service;
    String member;

    @BeforeEach
    void setup() {
        InventoryReservationTestSupport.useRandomPort(port);
        member = InventoryReservationTestSupport.obtainToken(
            InventoryReservationTestSupport.freshEmail("invres-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createItem(String sku, long onHand) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"sku\":\"" + sku + "\",\"onHand\":" + onHand + "}")
        .when().post("/api/inventory-reservation/items").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> reserve(String itemId, long q) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"quantity\":" + q + "}")
        .when().post("/api/inventory-reservation/items/" + itemId + "/reservations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> commit(String reservationId) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/inventory-reservation/reservations/" + reservationId + "/commit").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> release(String reservationId) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/inventory-reservation/reservations/" + reservationId + "/release").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getItem(String itemId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/inventory-reservation/items/" + itemId).then().statusCode(200).extract();
    }

    // ── INVRES-RESERVE-001 — reserve requires derived available ≥ q; onHand untouched; 422 else ──
    @Test @Tag("INVRES-RESERVE-001")
    void reserve_withinAvailable_holdsWithoutTouchingOnHand_overReserveIs422() {
        String item = createItem("SKU-RESERVE", 100);

        // a new item has available == onHand
        ExtractableResponse<Response> fresh = getItem(item);
        assertThat(fresh.jsonPath().getLong("onHand")).isEqualTo(100L);
        assertThat(fresh.jsonPath().getLong("reserved")).isEqualTo(0L);
        assertThat(fresh.jsonPath().getLong("available")).isEqualTo(100L);

        // reserve 30 → HELD; reserved 30, onHand unchanged, available 70
        ExtractableResponse<Response> r = reserve(item, 30);
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getString("status")).isEqualTo("HELD");
        assertThat(r.jsonPath().getLong("quantity")).isEqualTo(30L);

        ExtractableResponse<Response> afterHold = getItem(item);
        assertThat(afterHold.jsonPath().getLong("onHand")).as("onHand untouched by a hold").isEqualTo(100L);
        assertThat(afterHold.jsonPath().getLong("reserved")).isEqualTo(30L);
        assertThat(afterHold.jsonPath().getLong("available")).as("available = onHand − reserved").isEqualTo(70L);

        // reserve 80 > available(70) → 422, nothing mutated
        ExtractableResponse<Response> over = reserve(item, 80);
        assertThat(over.statusCode()).isEqualTo(422);
        assertThat(over.jsonPath().getString("code")).isEqualTo("INVENTORY_INSUFFICIENT_AVAILABLE");
        ExtractableResponse<Response> unchanged = getItem(item);
        assertThat(unchanged.jsonPath().getLong("reserved")).as("a refused reserve mutates nothing").isEqualTo(30L);
        assertThat(unchanged.jsonPath().getLong("available")).isEqualTo(70L);

        // reserving exactly the remaining available is allowed (boundary)
        assertThat(reserve(item, 70).statusCode()).isEqualTo(201);
        assertThat(getItem(item).jsonPath().getLong("available")).isEqualTo(0L);
        // and now even reserving 1 is refused
        assertThat(reserve(item, 1).statusCode()).isEqualTo(422);
    }

    // ── INVRES-COMMIT-001 — commit decrements BOTH onHand and reserved; exactly-once ──
    @Test @Tag("INVRES-COMMIT-001")
    void commit_decrementsBothAxes_exactlyOnce() {
        String item = createItem("SKU-COMMIT", 50);
        String resId = reserve(item, 20).jsonPath().getString("id");

        // before commit: onHand 50, reserved 20, available 30
        assertThat(getItem(item).jsonPath().getLong("available")).isEqualTo(30L);

        ExtractableResponse<Response> committed = commit(resId);
        assertThat(committed.statusCode()).isEqualTo(200);
        assertThat(committed.jsonPath().getString("status")).isEqualTo("COMMITTED");

        // after commit: BOTH axes fell by 20 → onHand 30, reserved 0, available 30 (unchanged by the pivot)
        ExtractableResponse<Response> afterCommit = getItem(item);
        assertThat(afterCommit.jsonPath().getLong("onHand")).as("onHand fell — goods left").isEqualTo(30L);
        assertThat(afterCommit.jsonPath().getLong("reserved")).as("reserved fell — hold consumed").isEqualTo(0L);
        assertThat(afterCommit.jsonPath().getLong("available")).isEqualTo(30L);

        // exactly-once: a second commit → 409
        ExtractableResponse<Response> again = commit(resId);
        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.jsonPath().getString("code")).isEqualTo("INVENTORY_RESERVATION_NOT_HELD");

        // a committed reservation can never be released
        ExtractableResponse<Response> rel = release(resId);
        assertThat(rel.statusCode()).isEqualTo(409);
        assertThat(rel.jsonPath().getString("code")).isEqualTo("INVENTORY_RESERVATION_NOT_HELD");
    }

    // ── INVRES-RELEASE-001 — release decrements reserved alone (onHand untouched); exactly-once ──
    @Test @Tag("INVRES-RELEASE-001")
    void release_freesHold_onHandUntouched_exactlyOnce() {
        String item = createItem("SKU-RELEASE", 40);
        String resId = reserve(item, 25).jsonPath().getString("id");
        assertThat(getItem(item).jsonPath().getLong("available")).isEqualTo(15L);

        ExtractableResponse<Response> released = release(resId);
        assertThat(released.statusCode()).isEqualTo(200);
        assertThat(released.jsonPath().getString("status")).isEqualTo("RELEASED");

        // after release: reserved back to 0, onHand untouched, available grows back to 40
        ExtractableResponse<Response> afterRelease = getItem(item);
        assertThat(afterRelease.jsonPath().getLong("onHand")).as("onHand untouched by a release").isEqualTo(40L);
        assertThat(afterRelease.jsonPath().getLong("reserved")).isEqualTo(0L);
        assertThat(afterRelease.jsonPath().getLong("available")).as("available grew back").isEqualTo(40L);

        // exactly-once: a second release → 409; a released reservation can never be committed
        assertThat(release(resId).statusCode()).isEqualTo(409);
        ExtractableResponse<Response> com = commit(resId);
        assertThat(com.statusCode()).isEqualTo(409);
        assertThat(com.jsonPath().getString("code")).isEqualTo("INVENTORY_RESERVATION_NOT_HELD");
    }

    // ── INVRES-CONSERVE-001 — reserved == Σ(HELD quantities) and 0 ≤ reserved ≤ onHand at every step ──
    @Test @Tag("INVRES-CONSERVE-001")
    void conservation_reservedEqualsSumOfHeldQuantities_andBounded() {
        String item = createItem("SKU-CONSERVE", 100);
        String a = reserve(item, 20).jsonPath().getString("id");
        String b = reserve(item, 30).jsonPath().getString("id");
        String c = reserve(item, 10).jsonPath().getString("id");

        // three HELD holds → reserved == 60 == Σ(HELD)
        assertThat(getItem(item).jsonPath().getLong("reserved")).isEqualTo(60L);
        assertThat(heldSum(item)).as("reserved == Σ(HELD quantities)").isEqualTo(60L);

        // commit one (consumes its hold AND on-hand), release another (frees its hold)
        assertThat(commit(a).statusCode()).isEqualTo(200);   // a no longer HELD; onHand 80, reserved 40
        assertThat(release(b).statusCode()).isEqualTo(200);  // b no longer HELD; reserved 10

        ExtractableResponse<Response> item2 = getItem(item);
        long onHand = item2.jsonPath().getLong("onHand");
        long reserved = item2.jsonPath().getLong("reserved");
        assertThat(onHand).isEqualTo(80L);
        assertThat(reserved).as("only c (10) is still HELD").isEqualTo(10L);
        assertThat(heldSum(item)).as("Σ(HELD) tracks reserved after commit+release").isEqualTo(10L);
        assertThat(reserved).isGreaterThanOrEqualTo(0L);
        assertThat(reserved).as("0 ≤ reserved ≤ onHand").isLessThanOrEqualTo(onHand);
        assertThat(item2.jsonPath().getLong("available")).isEqualTo(70L);

        // c still HELD and committable
        assertThat(commit(c).statusCode()).isEqualTo(200);
        assertThat(heldSum(item)).isEqualTo(0L);
        assertThat(getItem(item).jsonPath().getLong("reserved")).isEqualTo(0L);
    }

    private long heldSum(String itemId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/inventory-reservation/items/" + itemId + "/held-sum")
            .then().statusCode(200).extract().as(Long.class);
    }

    // ── INVRES-CONCURRENT-001 — keystone: N concurrent reserves against available = k*q → exactly k win ──
    @Test @Tag("INVRES-CONCURRENT-001")
    void concurrentReserves_exactlyAvailableOverQ_win() throws Exception {
        // available = 100, q = 25 → exactly 4 reserves can win; 8 threads race
        String item = createItem("SKU-RACE", 100);
        UUID itemId = UUID.fromString(item);
        long q = 25L;
        int n = 8;
        int expectedWinners = 4;   // floor(100 / 25)

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.reserve(itemId, q, "racer");
                    codes.add(201);
                } catch (InventoryReservationException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 201).count())
            .as("INVRES-CONCURRENT-001 — exactly floor(available/q) reserves win").isEqualTo((long) expectedWinners);
        assertThat(codes.stream().filter(c -> c == 422).count()).isEqualTo((long) (n - expectedWinners));

        // the item ended with reserved == 100 (== onHand), never over-reserved, exactly 4 HELD rows
        ExtractableResponse<Response> item2 = getItem(item);
        assertThat(item2.jsonPath().getLong("reserved")).isEqualTo(100L);
        assertThat(item2.jsonPath().getLong("reserved")).isLessThanOrEqualTo(item2.jsonPath().getLong("onHand"));
        assertThat(item2.jsonPath().getLong("available")).isEqualTo(0L);
        assertThat(heldSum(item)).as("exactly the winners' holds are recorded").isEqualTo(100L);
    }
}
