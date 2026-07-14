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

import java.math.BigDecimal;
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

    /** A past-due CALENDAR obligation + a far-future (but < 100y horizon) USAGE axis (so
     *  advanceUsage is callable to drive the cycle axis) + a declared breach basis amount
     *  (OBL-CONSEQUENCE-001). 5000 units at 1/day ≈ 13.7 years — later than the past CALENDAR
     *  deadline, comfortably inside ObligationAxis's ~100-year projection horizon. */
    private String createPastDueWithBasis(String key, String anchor, int intervalDays, String basis) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + key + "\",\"axes\":["
                + "{\"kind\":\"CALENDAR\",\"anchorAt\":\"" + anchor + "\",\"intervalDays\":" + intervalDays + "},"
                + "{\"kind\":\"USAGE\",\"anchorAt\":\"" + anchor + "\",\"limitUnits\":5000,\"unitsPerDay\":1}],"
                + "\"breachBasisAmount\":" + basis + "}")
        .when().post("/api/obligations").then().statusCode(201);
        return key;
    }

    private void advanceUsage(String key, String units) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"units\":" + units + "}")
        .when().post("/api/obligations/" + key + "/usage").then().statusCode(200);
    }

    /** The auth principal (JWT subject) is the userId, NOT the email (JwtTokenService.subject) —
     *  ack's response echoes {@code Authentication.getName()} as ackBy, so a throwaway ack is the
     *  simplest way to learn EXACTLY what a self-grant would need to declare as owner. */
    private String resolveMemberPrincipal() {
        String probe = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + probe + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + Instant.now().truncatedTo(ChronoUnit.SECONDS) + "\",\"intervalDays\":1}]}")
        .when().post("/api/obligations").then().statusCode(201);
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/obligations/" + probe + "/ack")
            .then().statusCode(200).extract().jsonPath().getString("ackBy");
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

    // ── OBL-CONSEQUENCE-001 — exactly-once consequence, ONLY when a basis was declared ──
    @Test @Tag("OBL-CONSEQUENCE-001")
    void breachCrossing_bindsConsequenceExactlyOnce_onlyWhenBasisDeclared() {
        String anchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();

        String withBasis = createPastDueWithBasis("obl-" + UUID.randomUUID(), anchor, 30, "1000");
        sweep(withBasis);
        ExtractableResponse<Response> first = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + withBasis + "/consequence").then().statusCode(200).extract();
        assertThat(new BigDecimal(first.jsonPath().getString("basisAmount"))).isEqualByComparingTo("1000");
        String recordedAt = first.jsonPath().getString("recordedAt");

        // re-sweep: exactly-once holds — the SAME record, not a second one
        sweep(withBasis);
        ExtractableResponse<Response> again = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + withBasis + "/consequence").then().statusCode(200).extract();
        assertThat(again.jsonPath().getString("recordedAt")).isEqualTo(recordedAt);

        // an obligation with NO declared basis: BREACH still fires (visibility unconditional),
        // but no consequence ever attaches
        String anchor2 = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String noBasis = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + noBasis + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + anchor2 + "\",\"intervalDays\":30}]}")
        .when().post("/api/obligations").then().statusCode(201);
        sweep(noBasis);
        assertThat(firedRungs(noBasis)).contains("BREACH");
        int noConsequence = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + noBasis + "/consequence").thenReturn().statusCode();
        assertThat(noConsequence)
            .as("OBL-CONSEQUENCE-001 — no declared basis amount means no consequence, ever")
            .isEqualTo(404);
    }

    // ── OBL-INTEREST-ACCRUE-001 — derive-on-read, not a stored running total ──
    @Test @Tag("OBL-INTEREST-ACCRUE-001")
    void accruedInterest_isDerivedFresh_fromDeadlineToNow() {
        // 40 days ago anchor, 30-day window → ~10 real days overdue right now
        String anchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = createPastDueWithBasis("obl-" + UUID.randomUUID(), anchor, 30, "10000");
        sweep(k);

        ExtractableResponse<Response> firstRead = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/consequence").then().statusCode(200).extract();
        BigDecimal firstAccrued = new BigDecimal(firstRead.jsonPath().getString("accruedInterest"));
        assertThat(firstAccrued)
            .as("OBL-INTEREST-ACCRUE-001 — ~10 real days overdue on a 10000 basis at 8%/yr must be > 0")
            .isGreaterThan(BigDecimal.ZERO);

        // a second read moments later recomputes fresh from the SAME three inputs — it must not
        // free-run like a stored counter would; the difference is bounded by the elapsed wall time
        ExtractableResponse<Response> secondRead = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/consequence").then().statusCode(200).extract();
        BigDecimal secondAccrued = new BigDecimal(secondRead.jsonPath().getString("accruedInterest"));
        assertThat(secondAccrued.subtract(firstAccrued).abs())
            .as("recompute is idempotent given (near-)identical inputs — not a mutated-in-place total")
            .isLessThan(new BigDecimal("0.01"));
        assertThat(secondRead.jsonPath().getString("basisAmount"))
            .as("the basis and deadline snapshot are immutable across reads")
            .isEqualTo(firstRead.jsonPath().getString("basisAmount"));
    }

    // ── OBL-WAIVER-001 — dual-axis: an active waiver suppresses the consequence, never the ladder;
    //    EITHER axis lapsing (here: the usage-cycle axis) reactivates enforcement on the next sweep ──
    @Test @Tag("OBL-WAIVER-001")
    void activeWaiver_suppressesConsequenceOnly_reactivatesWhenCycleAxisLapses() {
        String anchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = createPastDueWithBasis("obl-" + UUID.randomUUID(), anchor, 30, "500");

        // grant a waiver: TIME axis far in the future, USAGE-CYCLE axis bound at 2
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationOwner\":\"third-party-owner\",\"reason\":\"pending dispute\","
                + "\"expiresAt\":\"" + Instant.now().plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
                + "\",\"expiresAfterCycles\":2}")
        .when().post("/api/obligations/" + k + "/waivers").then().statusCode(201);

        sweep(k);
        assertThat(firedRungs(k))
            .as("OBL-WAIVER-001 — the ladder is UNAFFECTED by a waiver; it still fires on schedule")
            .contains("BREACH");
        int suppressed = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/consequence").thenReturn().statusCode();
        assertThat(suppressed)
            .as("OBL-WAIVER-001 — a valid waiver suppresses ONLY the consequence")
            .isEqualTo(404);

        // exhaust the usage-cycle axis (2 advances) — the waiver is now cycle-invalid even though
        // its TIME axis is still a year away: EITHER axis lapsing ends validity
        advanceUsage(k, "1");
        advanceUsage(k, "1");

        sweep(k);
        ExtractableResponse<Response> reactivated = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/consequence").then().statusCode(200).extract();
        assertThat(new BigDecimal(reactivated.jsonPath().getString("basisAmount")))
            .as("OBL-WAIVER-001 — the cycle axis lapsing alone reactivates enforcement on the next sweep")
            .isEqualByComparingTo("500");
    }

    // ── OBL-WAIVER-002 — 4-eyes grant, immutability, revoke appends a new record ──
    @Test @Tag("OBL-WAIVER-002")
    void waiverGrant_isFourEyesAndImmutable_revokeAppendsARecord_neverMutates() {
        String anchor = Instant.now().plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = "obl-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + k + "\",\"axes\":[{\"kind\":\"CALENDAR\",\"anchorAt\":\""
                + anchor + "\",\"intervalDays\":30}]}")
        .when().post("/api/obligations").then().statusCode(201);

        // self-grant rejected — the grantor (the authenticated caller) cannot equal the declared owner
        String memberPrincipal = resolveMemberPrincipal();
        ExtractableResponse<Response> selfGrant = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"obligationOwner\":\"" + memberPrincipal + "\",\"reason\":\"self\","
                + "\"expiresAt\":\"" + Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
                + "\",\"expiresAfterCycles\":5}")
        .when().post("/api/obligations/" + k + "/waivers").thenReturn().then().extract();
        assertThat(selfGrant.statusCode()).isEqualTo(422);
        assertThat(selfGrant.jsonPath().getString("code")).isEqualTo("OBLIGATION_WAIVER_SELF_GRANT");

        // an expiresAt already in the past is rejected — a waiver cannot be born invalid
        ExtractableResponse<Response> bornExpired = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"obligationOwner\":\"third-party\",\"reason\":\"test\","
                + "\"expiresAt\":\"" + Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
                + "\",\"expiresAfterCycles\":5}")
        .when().post("/api/obligations/" + k + "/waivers").thenReturn().then().extract();
        assertThat(bornExpired.statusCode()).isEqualTo(422);
        assertThat(bornExpired.jsonPath().getString("code")).isEqualTo("OBLIGATION_INVALID_WAIVER");

        ExtractableResponse<Response> granted = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"obligationOwner\":\"third-party\",\"reason\":\"pending dispute\","
                + "\"expiresAt\":\"" + Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
                + "\",\"expiresAfterCycles\":5}")
        .when().post("/api/obligations/" + k + "/waivers").then().statusCode(201).extract();
        String waiverId = granted.jsonPath().getString("id");
        assertThat(granted.jsonPath().getBoolean("active")).isTrue();

        given().header("Authorization", "Bearer " + member)
            .when().post("/api/obligations/" + k + "/waivers/" + waiverId + "/revoke")
            .then().statusCode(204);

        // the grant row is unchanged; a revocation makes it no-longer-active — never a mutation
        ExtractableResponse<Response> list = given().header("Authorization", "Bearer " + member)
            .when().get("/api/obligations/" + k + "/waivers").then().statusCode(200).extract();
        assertThat(list.jsonPath().getString("[0].reason")).isEqualTo("pending dispute");
        assertThat(list.jsonPath().getBoolean("[0].active"))
            .as("OBL-WAIVER-002 — revoked, so no longer active")
            .isFalse();

        // revoking twice is a deterministic 409 — the loop of "revoke" closes exactly once
        int secondRevoke = given().header("Authorization", "Bearer " + member)
            .when().post("/api/obligations/" + k + "/waivers/" + waiverId + "/revoke").thenReturn().statusCode();
        assertThat(secondRevoke).isEqualTo(409);
    }
}
