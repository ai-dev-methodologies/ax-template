package com.ax.template.authblueprint.settlement;

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
 * settlement-finality-l0 compliance — verified against the live settlement reference workload.
 * The invariant: the two legs of a DvP settlement commit atomically (partial unrepresentable);
 * SETTLED is the irrevocable final state after which novation/cancel/amend are all 409; a
 * pre-finality novation conserves the net obligation and is recorded append-only; a failed
 * settlement walks the exactly-once fail ladder; and concurrent settles finalize exactly once.
 * Spec: specs/settlement-finality-l0.yaml (BIS CPMI DvP/finality/novation + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("SETTLEMENT")
class SettlementComplianceTest {

    @LocalServerPort int port;
    @Autowired SettlementService service;
    String member;

    @BeforeEach
    void setup() {
        SettlementTestSupport.useRandomPort(port);
        member = SettlementTestSupport.obtainToken(SettlementTestSupport.freshEmail("settle-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> create(String tradeRef, String delivery, String payment, String obligation) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"tradeRef\":\"" + tradeRef + "\",\"deliveryParty\":\"" + delivery + "\",\"paymentParty\":\""
                + payment + "\",\"netObligation\":" + obligation + "}")
        .when().post("/api/settlement/instructions").then().statusCode(201).extract();
    }

    private String createId(String tradeRef) {
        return create(tradeRef, "BrokerA", "BrokerB", "1000000.00").jsonPath().getString("id");
    }

    private ExtractableResponse<Response> verb(String id, String verb) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/settlement/instructions/" + id + "/" + verb).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> novate(String id, String leg, String assuming) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"leg\":\"" + leg + "\",\"assumingParty\":\"" + assuming + "\"}")
        .when().post("/api/settlement/instructions/" + id + "/novate").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/settlement/instructions/" + id).then().statusCode(200).extract();
    }

    // ── SETTLE-DVP-001 — both legs commit atomically; partial unrepresentable ──
    @Test @Tag("SETTLE-DVP-001")
    void settle_commitsBothLegsAtomically_andRecordsFinality() {
        ExtractableResponse<Response> created = create("TRD-DVP-1", "DeliveryCo", "PaymentCo", "500000.00");
        assertThat(created.jsonPath().getString("status")).isEqualTo("PENDING");
        assertThat(created.jsonPath().getBoolean("deliverySettled")).isFalse();
        assertThat(created.jsonPath().getBoolean("paymentSettled")).isFalse();
        assertThat(created.jsonPath().getString("finalAt")).isNull();

        String id = created.jsonPath().getString("id");
        ExtractableResponse<Response> settled = verb(id, "settle");
        assertThat(settled.statusCode()).isEqualTo(200);
        assertThat(settled.jsonPath().getString("status")).isEqualTo("SETTLED");
        // DvP — both legs true together (never one alone)
        assertThat(settled.jsonPath().getBoolean("deliverySettled")).isTrue();
        assertThat(settled.jsonPath().getBoolean("paymentSettled")).isTrue();
        assertThat(settled.jsonPath().getString("finalAt")).isNotBlank();
    }

    // ── SETTLE-DVP-001 — settle from a terminal-failed (BUYIN) instruction is rejected ──
    @Test @Tag("SETTLE-DVP-001")
    void settle_fromBuyin_is409() {
        String id = createId("TRD-DVP-2");
        assertThat(verb(id, "fail").statusCode()).isEqualTo(200);
        assertThat(verb(id, "retry").statusCode()).isEqualTo(200);
        assertThat(verb(id, "buyin").statusCode()).isEqualTo(200);
        ExtractableResponse<Response> bad = verb(id, "settle");
        assertThat(bad.statusCode()).isEqualTo(409);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("SETTLEMENT_NOT_SETTLEABLE");
    }

    // ── SETTLE-FINAL-001 — finality is irrevocable: novate/re-settle/fail all 409 ──
    @Test @Tag("SETTLE-FINAL-001")
    void finality_isIrrevocable_everyMutatingVerbBlocked() {
        String id = createId("TRD-FINAL-1");
        assertThat(verb(id, "settle").statusCode()).isEqualTo(200);

        ExtractableResponse<Response> reSettle = verb(id, "settle");
        assertThat(reSettle.statusCode()).isEqualTo(409);
        assertThat(reSettle.jsonPath().getString("code")).isEqualTo("SETTLEMENT_ALREADY_FINAL");

        ExtractableResponse<Response> lateNovate = novate(id, "DELIVERY", "NewBroker");
        assertThat(lateNovate.statusCode()).isEqualTo(409);
        assertThat(lateNovate.jsonPath().getString("code")).isEqualTo("SETTLEMENT_ALREADY_FINAL");

        ExtractableResponse<Response> lateFail = verb(id, "fail");
        assertThat(lateFail.statusCode()).isEqualTo(409);
        assertThat(lateFail.jsonPath().getString("code")).isEqualTo("SETTLEMENT_ALREADY_FINAL");

        assertThat(get(id).jsonPath().getString("finalAt")).isNotBlank();   // finality instant recorded
    }

    // ── SETTLE-NOVATE-001 — pre-finality counterparty replacement conserves the obligation ──
    @Test @Tag("SETTLE-NOVATE-001")
    void novate_replacesCounterparty_conservesObligation_recordedAppendOnly() {
        ExtractableResponse<Response> created = create("TRD-NOV-1", "OldDelivery", "PayCo", "750000.00");
        String id = created.jsonPath().getString("id");

        ExtractableResponse<Response> novated = novate(id, "DELIVERY", "NewDelivery");
        assertThat(novated.statusCode()).isEqualTo(200);
        assertThat(novated.jsonPath().getString("deliveryParty")).isEqualTo("NewDelivery");
        assertThat(novated.jsonPath().getString("paymentParty")).isEqualTo("PayCo");        // untouched
        assertThat(novated.jsonPath().getFloat("netObligation")).isEqualTo(750000.0f);      // CONSERVED

        java.util.List<java.util.Map<String, Object>> novations =
            given().header("Authorization", "Bearer " + member)
                .when().get("/api/settlement/instructions/" + id + "/novations")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(novations).hasSize(1);
        assertThat(novations.get(0).get("leg")).isEqualTo("DELIVERY");
        assertThat(novations.get(0).get("releasedParty")).isEqualTo("OldDelivery");
        assertThat(novations.get(0).get("assumingParty")).isEqualTo("NewDelivery");
        assertThat(((Number) novations.get(0).get("assumedObligation")).doubleValue()).isEqualTo(750000.0);
        assertThat(novations.get(0).get("novatedBy")).asString().isNotBlank();

        // a novated instruction can still reach finality (DvP commit unaffected by the swap)
        assertThat(verb(id, "settle").statusCode()).isEqualTo(200);
    }

    // ── SETTLE-NOVATE-001 — novating to the party already owing the leg is rejected (422) ──
    @Test @Tag("SETTLE-NOVATE-001")
    void novate_toSameParty_is422() {
        ExtractableResponse<Response> created = create("TRD-NOV-2", "DeliveryCo", "SamePay", "100.00");
        String id = created.jsonPath().getString("id");
        ExtractableResponse<Response> bad = novate(id, "PAYMENT", "SamePay");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("SETTLEMENT_NOVATION_NO_CHANGE");
    }

    // ── SETTLE-LADDER-001 — fail ladder walked once each; off-graph edges 409; recovery to finality ──
    @Test @Tag("SETTLE-LADDER-001")
    void failLadder_exactlyOnceEdges_offGraph409_recoverToFinality() {
        // illegal edges from PENDING: retry / buyin (only fail is legal)
        String p = createId("TRD-LAD-1");
        ExtractableResponse<Response> earlyRetry = verb(p, "retry");
        assertThat(earlyRetry.statusCode()).isEqualTo(409);
        assertThat(earlyRetry.jsonPath().getString("code")).isEqualTo("SETTLEMENT_ILLEGAL_LADDER_EDGE");
        assertThat(verb(p, "buyin").statusCode()).isEqualTo(409);

        // the full ladder, exactly once each
        assertThat(verb(p, "fail").jsonPath().getString("status")).isEqualTo("FAILED");
        assertThat(verb(p, "fail").statusCode()).isEqualTo(409);            // repeat edge rejected
        assertThat(verb(p, "retry").jsonPath().getString("status")).isEqualTo("RETRY");
        assertThat(verb(p, "fail").statusCode()).isEqualTo(409);            // reverse edge rejected
        assertThat(verb(p, "buyin").jsonPath().getString("status")).isEqualTo("BUYIN");
        assertThat(verb(p, "retry").statusCode()).isEqualTo(409);           // BUYIN is terminal

        // a FAILED instruction can still recover to finality via settle
        String q = createId("TRD-LAD-2");
        assertThat(verb(q, "fail").jsonPath().getString("status")).isEqualTo("FAILED");
        ExtractableResponse<Response> recovered = verb(q, "settle");
        assertThat(recovered.statusCode()).isEqualTo(200);
        assertThat(recovered.jsonPath().getString("status")).isEqualTo("SETTLED");
        assertThat(recovered.jsonPath().getBoolean("deliverySettled")).isTrue();
        assertThat(recovered.jsonPath().getBoolean("paymentSettled")).isTrue();
    }

    // ── SETTLE-CONCURRENT-001 — keystone: N concurrent settles → exactly one finalizes ──
    @Test @Tag("SETTLE-CONCURRENT-001")
    void concurrentSettles_exactlyOneFinalizes() throws Exception {
        UUID id = UUID.fromString(createId("TRD-RACE-1"));

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.settle(id);
                    codes.add(200);
                } catch (SettlementException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("SETTLE-CONCURRENT-001 — exactly one settle finalizes").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the instruction is SETTLED exactly once with both legs true and a single finality instant
        ExtractableResponse<Response> finalState = get(id.toString());
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("SETTLED");
        assertThat(finalState.jsonPath().getBoolean("deliverySettled")).isTrue();
        assertThat(finalState.jsonPath().getBoolean("paymentSettled")).isTrue();
        assertThat(finalState.jsonPath().getString("finalAt")).isNotBlank();
    }
}
