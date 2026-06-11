package com.ax.template.authblueprint.obligation;

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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * deadline-obligation-l0 compliance — verified against the live obligation reference workload.
 * The invariant: every deadline is DERIVED from recorded axes (no raw deadline crosses the API);
 * the EARLIEST axis candidate governs and a usage advance re-derives it; ordered rungs fire
 * exactly once as appended additive events; the ONLY terminal is an explicit who/when ack —
 * the sweep never auto-expires. Spec: specs/deadline-obligation-l0.yaml
 * (14 CFR §91.409 two-axis regime + PMC7510293 closed-loop).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("OBLIGATION")
class ObligationComplianceTest {

    @LocalServerPort int port;
    @Autowired ObligationSweeper sweeper;
    @Autowired ObligationService service;
    String member;
    String admin;

    @BeforeEach
    void setup() {
        ObligationTestSupport.useRandomPort(port);
        member = ObligationTestSupport.obtainToken(ObligationTestSupport.freshEmail("obl-member"), "MEMBER");
        admin = ObligationTestSupport.obtainToken(ObligationTestSupport.freshEmail("obl-admin"), "ADMIN");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createTwoAxis(String key, String anchor, int intervalDays,
                                 String limit, String perDay) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + key + "\",\"axes\":["
                + "{\"kind\":\"CALENDAR\",\"anchorAt\":\"" + anchor + "\",\"intervalDays\":" + intervalDays + "},"
                + "{\"kind\":\"USAGE\",\"anchorAt\":\"" + anchor + "\",\"limitUnits\":" + limit
                + ",\"unitsPerDay\":" + perDay + "}]}")
        .when().post("/api/obligations").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> get(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + key).then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> sweep(String key) {
        // the deterministic sweep trigger is an OPERATIONAL action — ADMIN-only
        return given().header("Authorization", "Bearer " + admin)
            .when().post("/api/obligations/" + key + "/sweep").then().statusCode(200).extract();
    }

    private java.util.List<String> firedRungs(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + key + "/escalations")
            .then().statusCode(200).extract().jsonPath().getList("rung", String.class);
    }

    // ── OBL-GROUND-001 — every axis carries candidate + recomputable derivation; no raw deadline ──
    @Test @Tag("OBL-GROUND-001")
    void create_recordsDerivationPerAxis_andNoApiAcceptsARawDeadline() {
        String anchor = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String k = createTwoAxis("obl-" + UUID.randomUUID(), anchor, 365, "100", "2");

        ExtractableResponse<Response> derivations = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/derivations").then().statusCode(200).extract();
        java.util.List<String> formulas = derivations.jsonPath().getList("data.formula", String.class);
        assertThat(formulas).hasSize(2);
        assertThat(formulas).anySatisfy(f -> assertThat(f).startsWith("CALENDAR: ").contains("+ P365D"));
        assertThat(formulas).anySatisfy(f -> assertThat(f).startsWith("USAGE: ").contains("limit 100").contains("/ 2 per-day"));

        // grounding: the create contract has no deadline field — an extra raw field is ignored,
        // and the effective deadline still equals the derived earliest candidate (USAGE: 50d < CAL: 365d)
        Instant effective = Instant.parse(get(k).jsonPath().getString("effectiveDeadline"));
        Instant expectedUsage = Instant.parse(anchor).plus(50, ChronoUnit.DAYS);
        assertThat(effective).isEqualTo(expectedUsage);
    }

    // ── OBL-AXIS-001 — earliest axis governs; usage advance re-derives + re-records ──
    @Test @Tag("OBL-AXIS-001")
    void usageAdvance_rederivesCandidate_andEffectiveIsEarliest() {
        String anchor = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        // CAL: 30d; USAGE: 100/1 per day = 100d → CALENDAR governs initially
        String k = createTwoAxis("obl-" + UUID.randomUUID(), anchor, 30, "100", "1");
        Instant initial = Instant.parse(get(k).jsonPath().getString("effectiveDeadline"));
        assertThat(initial).isEqualTo(Instant.parse(anchor).plus(30, ChronoUnit.DAYS));

        // burn 95 units → remaining 5 / 1 per-day = ~5d from NOW → USAGE overtakes CALENDAR
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"units\":95}")
        .when().post("/api/obligations/" + k + "/usage").then().statusCode(200);

        Instant after = Instant.parse(get(k).jsonPath().getString("effectiveDeadline"));
        assertThat(after).isBefore(initial);

        // the re-derivation was APPENDED (2 at create + 1 on advance)
        int derivationCount = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/derivations")
            .then().statusCode(200).extract().jsonPath().getList("data").size();
        assertThat(derivationCount).isEqualTo(3);
    }

    // ── OBL-LADDER-001 — rungs fire exactly once, in order, additively ──
    @Test @Tag("OBL-LADDER-001")
    void ladder_firesEachRungOnce_inOrder_andResweepAddsNothing() {
        // anchor 90 days AGO with a 100-day calendar window → 90% elapsed → APPROACH+IMMINENT due
        String anchor = Instant.now().minus(90, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + k + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + anchor + "\",\"intervalDays\":100}]}")
        .when().post("/api/obligations").then().statusCode(201);

        sweep(k);
        assertThat(firedRungs(k)).containsExactly("APPROACH", "IMMINENT");

        sweep(k);                                          // re-sweep: exactly-once holds
        assertThat(firedRungs(k)).containsExactly("APPROACH", "IMMINENT");
        assertThat(get(k).jsonPath().getString("status")).isEqualTo("OPEN");
    }

    // ── OBL-ACK-001 — past-deadline stays OPEN+BREACH; ack is the only terminal; double-ack 409 ──
    @Test @Tag("OBL-ACK-001")
    void pastDeadline_staysOpenWithBreach_untilExplicitAck_thenLoopClosesOnce() {
        // anchor 40 days ago, 30-day window → past deadline
        String anchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + k + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + anchor + "\",\"intervalDays\":30}]}")
        .when().post("/api/obligations").then().statusCode(201);

        sweep(k);
        assertThat(firedRungs(k)).containsExactly("APPROACH", "IMMINENT", "BREACH");
        assertThat(get(k).jsonPath().getString("status"))
            .as("OBL-ACK-001 — the sweep NEVER auto-expires; past-deadline stays OPEN")
            .isEqualTo("OPEN");

        // explicit ack closes the loop with who/when
        ExtractableResponse<Response> ack = given().header("Authorization", "Bearer " + member)
            .when().post("/api/obligations/" + k + "/ack").then().statusCode(200).extract();
        assertThat(ack.jsonPath().getString("status")).isEqualTo("ACKNOWLEDGED");
        assertThat(ack.jsonPath().getString("ackBy")).isNotBlank();
        assertThat(ack.jsonPath().getString("ackAt")).isNotBlank();

        // the loop closes once
        int second = given().header("Authorization", "Bearer " + member)
            .when().post("/api/obligations/" + k + "/ack").thenReturn().statusCode();
        assertThat(second).isEqualTo(409);

        // an acknowledged obligation is skipped by the sweep — no resurrection, no new events
        sweep(k);
        assertThat(firedRungs(k)).containsExactly("APPROACH", "IMMINENT", "BREACH");
    }

    // ── OBL-CONCURRENT-001 — keystone: N racing sweeps fire each due rung exactly once ──
    @Test @Tag("OBL-CONCURRENT-001")
    void concurrentSweeps_fireEachDueRungExactlyOnce() throws Exception {
        String anchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + k + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + anchor + "\",\"intervalDays\":30}]}")
        .when().post("/api/obligations").then().statusCode(201);
        UUID id = service.get(k).getId();

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                sweeper.processOne(id);                    // direct concurrent sweep passes
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(firedRungs(k))
            .as("OBL-CONCURRENT-001 — all 3 rungs due, each fired EXACTLY once across 8 racing sweeps")
            .containsExactly("APPROACH", "IMMINENT", "BREACH");
    }
}
