package com.ax.template.authblueprint.valuationrun;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * valuation-run-projection-l0 compliance — verified against the live valuationrun reference
 * workload. The invariant: an immutable versioned run pinned to an as-of instant + recorded
 * basis (as-of read returns the greatest as-of ≤ T); a conserving fan-out to N per-position
 * outputs whose Σ equals the run total (checked independently); a rebase that appends a new
 * baseline while retaining prior runs verbatim; concurrent recompute/rebase serialize so exactly
 * one new version is created.
 * Spec: specs/valuation-run-projection-l0.yaml (Rule 2a-4 as-of NAV + Reg S-X 210.3-04 + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("VALUATIONRUN")
class ValuationRunComplianceTest {

    @LocalServerPort int port;
    @Autowired ValuationRunService service;
    String member;

    @BeforeEach
    void setup() {
        ValuationRunTestSupport.useRandomPort(port);
        member = ValuationRunTestSupport.obtainToken(ValuationRunTestSupport.freshEmail("val-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createSubject(String ref) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + ref + "\"}")
        .when().post("/api/valuation-run/subjects").then().statusCode(201).extract().path("id");
    }

    private static String positionsJson(Map<String, String> positions) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : positions.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        return sb.append('}').toString();
    }

    private ExtractableResponse<Response> recompute(String id, int expectedHead, String declaredTotal,
                                                    String basis, Map<String, String> positions) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"expectedHeadVersion\":" + expectedHead + ",\"declaredTotal\":" + declaredTotal + ","
                + "\"basis\":\"" + basis + "\",\"positions\":" + positionsJson(positions) + "}")
        .when().post("/api/valuation-run/subjects/" + id + "/runs").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> rebase(String id, int fromVersion, String declaredTotal,
                                                 String basis, Map<String, String> positions) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"fromRunVersion\":" + fromVersion + ",\"declaredTotal\":" + declaredTotal + ","
                + "\"basis\":\"" + basis + "\",\"positions\":" + positionsJson(positions) + "}")
        .when().post("/api/valuation-run/subjects/" + id + "/rebase").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> asOf(String id, String at) {
        return given().header("Authorization", "Bearer " + member)
            .queryParam("at", at)
        .when().get("/api/valuation-run/subjects/" + id + "/as-of").thenReturn().then().extract();
    }

    private static Map<String, String> pos(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    // ── VALRUN-ASOF-001 — point-in-time read returns the greatest as-of ≤ T ──
    @Test @Tag("VALRUN-ASOF-001")
    void asOf_returnsGreatestAsOfAtOrBeforeT_never404IfPresent() {
        String id = createSubject("PORT-ASOF");

        // three runs at strictly increasing as-of instants
        String t1 = recompute(id, 0, "100.00", "basis-v1", pos("a", "100.00")).jsonPath().getString("asOf");
        String t2 = recompute(id, 1, "200.00", "basis-v2", pos("a", "200.00")).jsonPath().getString("asOf");
        String t3 = recompute(id, 2, "300.00", "basis-v3", pos("a", "300.00")).jsonPath().getString("asOf");
        assertThat(Instant.parse(t1)).isBefore(Instant.parse(t2));
        assertThat(Instant.parse(t2)).isBefore(Instant.parse(t3));

        // as-of exactly t2 → the v2 run (200), never the later v3
        ExtractableResponse<Response> atT2 = asOf(id, t2);
        assertThat(atT2.statusCode()).isEqualTo(200);
        assertThat(atT2.jsonPath().getInt("runVersion")).isEqualTo(2);
        assertThat(atT2.jsonPath().getDouble("totalValue")).isEqualTo(200.0);

        // as-of strictly between t2 and t3 → still the v2 run (no later run existed yet)
        Instant between = Instant.parse(t2).plusMillis(1);
        assertThat(between).isBefore(Instant.parse(t3));
        ExtractableResponse<Response> mid = asOf(id, between.toString());
        assertThat(mid.statusCode()).isEqualTo(200);
        assertThat(mid.jsonPath().getInt("runVersion")).isEqualTo(2);

        // as-of strictly before the first run → 404 VALRUN_NO_RUN_AS_OF
        ExtractableResponse<Response> tooEarly = asOf(id, Instant.parse(t1).minusSeconds(60).toString());
        assertThat(tooEarly.statusCode()).isEqualTo(404);
        assertThat(tooEarly.jsonPath().getString("code")).isEqualTo("VALRUN_NO_RUN_AS_OF");

        // the run response carries as-of + basis + total (reconstructible basis)
        assertThat(atT2.jsonPath().getString("basis")).isEqualTo("basis-v2");
        assertThat(atT2.jsonPath().getString("asOf")).isNotBlank();
    }

    // ── VALRUN-FANOUT-001 — Σ outputs equals the run total; partial fan-out unrepresentable ──
    @Test @Tag("VALRUN-FANOUT-001")
    void fanOut_conserves_andPartialFanOutIs422() {
        String id = createSubject("PORT-FANOUT");

        // a conserving fan-out [a:30, b:70] total 100 → 2 output rows summing to 100
        ExtractableResponse<Response> ok = recompute(id, 0, "100.00", "basis", pos("a", "30.00", "b", "70.00"));
        assertThat(ok.statusCode()).isEqualTo(201);
        int v = ok.jsonPath().getInt("runVersion");
        Map<String, Object> outputs = given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/runs/" + v + "/outputs")
            .then().statusCode(200).extract().jsonPath().getMap("$");
        assertThat(outputs).hasSize(2);
        assertThat(new java.math.BigDecimal(outputs.get("a").toString())).isEqualByComparingTo("30");
        assertThat(new java.math.BigDecimal(outputs.get("b").toString())).isEqualByComparingTo("70");

        // a NON-conserving fan-out: declaredTotal 100 but positions sum to 90 → 422, nothing persisted
        ExtractableResponse<Response> bad = recompute(id, 1, "100.00", "basis", pos("a", "30.00", "b", "60.00"));
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("VALRUN_FANOUT_NOT_CONSERVED");

        // the rejected version did NOT land — the head is still the conserving v1
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id)
            .then().statusCode(200).extract().jsonPath().getInt("headRunVersion")).isEqualTo(1);
    }

    // ── VALRUN-REBASE-001 — rebase appends a baseline retaining prior runs; stale head → 409 ──
    @Test @Tag("VALRUN-REBASE-001")
    void rebase_appendsBaseline_retainsPriorRuns_staleHead409() {
        String id = createSubject("PORT-REBASE");
        recompute(id, 0, "100.00", "basis-v1", pos("a", "100.00"));   // v1

        // rebase from the current head (v1) → v2 baseline with a forward pointer at v1
        ExtractableResponse<Response> v2 = rebase(id, 1, "200.00", "split-2for1", pos("a", "200.00"));
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getInt("runVersion")).isEqualTo(2);
        assertThat(v2.jsonPath().getInt("rebasedFromRunVersion")).isEqualTo(1);

        // v1 is retained VERBATIM
        ExtractableResponse<Response> v1 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/runs/1").then().statusCode(200).extract();
        assertThat(v1.jsonPath().getString("basis")).isEqualTo("basis-v1");
        assertThat(v1.jsonPath().getDouble("totalValue")).isEqualTo(100.0);

        // current resolves to the latest baseline (v2)
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/current")
            .then().statusCode(200).extract().jsonPath().getInt("runVersion")).isEqualTo(2);

        // rebasing again from the STALE head (v1) → 409 VALRUN_NOT_CURRENT (chain stays linear)
        ExtractableResponse<Response> stale = rebase(id, 1, "400.00", "split-again", pos("a", "400.00"));
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(stale.jsonPath().getString("code")).isEqualTo("VALRUN_NOT_CURRENT");

        // the run trail lists v1 then v2 in order
        var versions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/runs")
            .then().statusCode(200).extract().jsonPath().getList("runVersion");
        assertThat(versions).containsExactly(1, 2);
    }

    // ── VALRUN-IMMUTABLE-001 — a recompute appends a new version; the prior is unchanged ──
    @Test @Tag("VALRUN-IMMUTABLE-001")
    void recompute_appendsNewVersion_priorUnchanged() {
        String id = createSubject("PORT-IMMUT");
        recompute(id, 0, "100.00", "basis-v1", pos("a", "100.00"));
        recompute(id, 1, "150.00", "basis-v2", pos("a", "150.00"));

        // v1 is unchanged after v2 lands
        ExtractableResponse<Response> v1 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/runs/1").then().statusCode(200).extract();
        assertThat(v1.jsonPath().getDouble("totalValue")).isEqualTo(100.0);
        assertThat(v1.jsonPath().getString("basis")).isEqualTo("basis-v1");
        assertThat(v1.jsonPath().getInt("runVersion")).isEqualTo(1);
    }

    // ── VALRUN-CONCURRENT-001 — keystone: N concurrent recomputes → exactly one new version ──
    @Test @Tag("VALRUN-CONCURRENT-001")
    void concurrentRecomputes_exactlyOneNewVersion() throws Exception {
        String id = createSubject("PORT-RACE");
        UUID subjectId = UUID.fromString(id);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    Map<String, BigDecimal> positions = new LinkedHashMap<>();
                    positions.put("a", new BigDecimal("100.00"));
                    service.recompute(subjectId, 0, new BigDecimal("100.00"), "race-basis", positions);
                    codes.add(201);
                } catch (ValuationRunException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 201).count())
            .as("VALRUN-CONCURRENT-001 — exactly one recompute wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the head advanced by exactly one version, with exactly one run row at v1
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id)
            .then().statusCode(200).extract().jsonPath().getInt("headRunVersion")).isEqualTo(1);
        var versions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/valuation-run/subjects/" + id + "/runs")
            .then().statusCode(200).extract().jsonPath().getList("runVersion");
        assertThat(versions).as("exactly one run row at the next version").containsExactly(1);
    }

    // ── VALRUN-FALLBACK-001 — priority-order fallback across sources; provenance recorded; fail-closed ──
    @Test @Tag("VALRUN-FALLBACK-001")
    void asOfFallback_triesSourcesInPriorityOrder_recordsProvenance_failsClosedWhenNoneQualify() {
        String id = createSubject("PORT-FALLBACK");

        // only a SECONDARY-source run exists — PRIMARY has no run at all yet
        String tSecondary = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"expectedHeadVersion\":0,\"declaredTotal\":\"50.00\",\"basis\":\"b\","
                + "\"positions\":{\"a\":50.00}}")
        .when().post("/api/valuation-run/subjects/" + id + "/runs/sources/SECONDARY")
            .then().statusCode(201).extract().jsonPath().getString("asOf");

        // a fallback naming [PRIMARY, SECONDARY] at a time only SECONDARY qualifies -> served by SECONDARY
        Instant afterSecondary = Instant.parse(tSecondary).plusMillis(1);
        var fallbackToSecondary = given().header("Authorization", "Bearer " + member)
            .queryParam("at", afterSecondary.toString())
            .queryParam("sources", "PRIMARY").queryParam("sources", "SECONDARY")
        .when().get("/api/valuation-run/subjects/" + id + "/as-of-fallback").thenReturn().then().extract();
        assertThat(fallbackToSecondary.statusCode()).isEqualTo(200);
        assertThat(fallbackToSecondary.jsonPath().getString("sourceRef")).isEqualTo("SECONDARY");

        // now PRIMARY also gets a run — priority order means PRIMARY wins even though it's newer,
        // not "most recent across sources" (SECONDARY's run also still qualifies at this instant)
        String tPrimary = recompute(id, 1, "100.00", "basis", pos("a", "100.00")).jsonPath().getString("asOf");
        Instant afterPrimary = Instant.parse(tPrimary).plusMillis(1);
        var fallbackToPrimary = given().header("Authorization", "Bearer " + member)
            .queryParam("at", afterPrimary.toString())
            .queryParam("sources", "PRIMARY").queryParam("sources", "SECONDARY")
        .when().get("/api/valuation-run/subjects/" + id + "/as-of-fallback").thenReturn().then().extract();
        assertThat(fallbackToPrimary.statusCode()).isEqualTo(200);
        assertThat(fallbackToPrimary.jsonPath().getString("sourceRef")).isEqualTo("PRIMARY");

        // DISCRIMINATING case — give the LOWER-priority source (SECONDARY) a STRICTLY MORE
        // RECENT qualifying run than PRIMARY's. If the implementation picked "most recent across
        // sources" instead of the caller's priority order, this would now return SECONDARY; the
        // spec (VALRUN-FALLBACK-001) requires priority order to still win, so PRIMARY must be
        // returned even though its own run is the OLDER of the two qualifying runs.
        String tSecondaryNewer = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"expectedHeadVersion\":2,\"declaredTotal\":\"75.00\",\"basis\":\"b2\","
                + "\"positions\":{\"a\":75.00}}")
        .when().post("/api/valuation-run/subjects/" + id + "/runs/sources/SECONDARY")
            .then().statusCode(201).extract().jsonPath().getString("asOf");
        Instant afterSecondaryNewer = Instant.parse(tSecondaryNewer).plusMillis(1);
        assertThat(afterSecondaryNewer).isAfter(afterPrimary);   // SECONDARY's run is now the newer one

        var fallbackStillPrimary = given().header("Authorization", "Bearer " + member)
            .queryParam("at", afterSecondaryNewer.toString())
            .queryParam("sources", "PRIMARY").queryParam("sources", "SECONDARY")
        .when().get("/api/valuation-run/subjects/" + id + "/as-of-fallback").thenReturn().then().extract();
        assertThat(fallbackStillPrimary.statusCode()).isEqualTo(200);
        assertThat(fallbackStillPrimary.jsonPath().getString("sourceRef")).as(
            "priority order must win over recency — PRIMARY still qualifies (older run) and is "
                + "higher-priority than SECONDARY, even though SECONDARY now holds the newer run")
            .isEqualTo("PRIMARY");

        // a fallback naming sources with NO qualifying run anywhere -> 404, fail-closed
        var noneQualify = given().header("Authorization", "Bearer " + member)
            .queryParam("at", afterPrimary.toString())
            .queryParam("sources", "TERTIARY").queryParam("sources", "QUATERNARY")
        .when().get("/api/valuation-run/subjects/" + id + "/as-of-fallback").thenReturn().then().extract();
        assertThat(noneQualify.statusCode()).isEqualTo(404);
        assertThat(noneQualify.jsonPath().getString("code")).isEqualTo("VALRUN_NO_QUALIFYING_SOURCE");
    }

    // ── VALRUN-ASOF-001 — empty fan-out is rejected ──
    @Test @Tag("VALRUN-FANOUT-001")
    void emptyFanOut_is422() {
        String id = createSubject("PORT-EMPTY");
        ExtractableResponse<Response> bad = given().header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body("{\"declaredTotal\":0,\"basis\":\"b\",\"positions\":{}}")
        .when().post("/api/valuation-run/subjects/" + id + "/runs").thenReturn().then().extract();
        // an empty positions map fails @NotEmpty bean validation → 400 problem+json
        assertThat(bad.statusCode()).isEqualTo(400);
    }
}
