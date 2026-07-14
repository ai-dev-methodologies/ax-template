package com.ax.template.authblueprint.netmetering;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * signed-dual-register-l0 compliance — verified against the live net-metering reference workload. The
 * invariant: a net meter is TWO independently value-monotone direction registers (IMPORT +, EXPORT −) and
 * a DERIVED signed net = cumulativeImport − cumulativeExport, computed under the meter row lock; a reading
 * below its direction cumulative is a 422; the net is cross-checked against an independent Σimport − Σexport
 * recompute; a billing-period close snapshots both cumulatives + the net delta and freezes the period (a
 * backdate is a 409). Spec: specs/signed-dual-register-l0.yaml (PURPA net metering / RFC 2578 Counter).
 *
 * Time determinism: all effectiveAt / boundary instants are EXPLICIT fixed ISO-8601 instants (never a
 * relative now()), so there is no system-TZ off-by-one at a midnight boundary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("NETMETERING")
class NetMeterComplianceTest {

    @LocalServerPort int port;
    String member;

    // fixed, strictly-ordered instants — t0 < t1 < t2 < t3 (no relative now(); TZ-independent)
    private static final String T0 = "2026-01-01T00:00:00Z";
    private static final String T1 = "2026-02-01T00:00:00Z";
    private static final String T2 = "2026-03-01T00:00:00Z";
    private static final String T3 = "2026-04-01T00:00:00Z";

    @BeforeEach
    void setup() {
        NetMeterTestSupport.useRandomPort(port);
        member = NetMeterTestSupport.obtainToken(NetMeterTestSupport.freshEmail("nm-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createMeter(String key, String imp, String exp) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"meterKey\":\"" + key + "\",\"initialImport\":" + imp + ",\"initialExport\":" + exp + "}")
        .when().post("/api/netmetering/meters").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> append(String key, String dir, String value, String effectiveAt) {
        String eff = effectiveAt == null ? "" : ",\"effectiveAt\":\"" + effectiveAt + "\"";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"direction\":\"" + dir + "\",\"readingValue\":" + value + eff + "}")
        .when().post("/api/netmetering/meters/" + key + "/readings").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> closePeriod(String key, String boundaryAt) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"boundaryAt\":\"" + boundaryAt + "\"}")
        .when().post("/api/netmetering/meters/" + key + "/periods").thenReturn().then().extract();
    }

    private String createMeterWithRates(String key, String imp, String exp, String rateImp, String rateExp) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"meterKey\":\"" + key + "\",\"initialImport\":" + imp + ",\"initialExport\":" + exp
                + ",\"rateImport\":" + rateImp + ",\"rateExport\":" + rateExp + "}")
        .when().post("/api/netmetering/meters").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> getMeter(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/netmetering/meters/" + key).then().statusCode(200).extract();
    }

    private void assertMeter(String key, String imp, String exp, String net) {
        ExtractableResponse<Response> r = getMeter(key);
        assertThat(new BigDecimal(r.jsonPath().getString("cumulativeImport"))).as("import").isEqualByComparingTo(imp);
        assertThat(new BigDecimal(r.jsonPath().getString("cumulativeExport"))).as("export").isEqualByComparingTo(exp);
        assertThat(new BigDecimal(r.jsonPath().getString("net"))).as("net").isEqualByComparingTo(net);
    }

    // ── NETM-DIRECTION-001 — each direction is value-monotone & independent; decrease 422 ──
    @Test @Tag("NETM-DIRECTION-001")
    void directionsAreIndependentlyMonotone_decreaseRejected() {
        String s = createMeter("nm-" + UUID.randomUUID(), "100", "0");

        // IMPORT read advances import only; export untouched
        ExtractableResponse<Response> imp = append(s, "IMPORT", "150", T0);
        assertThat(imp.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(imp.jsonPath().getString("delta"))).isEqualByComparingTo("50");
        assertMeter(s, "150", "0", "150");

        // EXPORT read advances export only; compared to the EXPORT cumulative (0), not import
        ExtractableResponse<Response> exp = append(s, "EXPORT", "40", T1);
        assertThat(exp.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(exp.jsonPath().getString("delta"))).isEqualByComparingTo("40");
        assertMeter(s, "150", "40", "110");                 // net = 150 − 40

        // an IMPORT read below the import cumulative (150) → 422, cumulatives unchanged
        ExtractableResponse<Response> down = append(s, "IMPORT", "120", T2);
        assertThat(down.statusCode()).isEqualTo(422);
        assertThat(down.path("code").toString()).isEqualTo("NETMETER_NOT_MONOTONE");
        assertMeter(s, "150", "40", "110");

        // an EXPORT read below the export cumulative (40) → 422 (independent of the larger import cumulative)
        ExtractableResponse<Response> exDown = append(s, "EXPORT", "30", T2);
        assertThat(exDown.statusCode()).isEqualTo(422);
        assertThat(exDown.path("code").toString()).isEqualTo("NETMETER_NOT_MONOTONE");
        assertMeter(s, "150", "40", "110");
    }

    // ── NETM-NET-001 — net is DERIVED = Σimport − Σexport, signed, cross-checked by independent recompute ──
    @Test @Tag("NETM-NET-001")
    void netIsDerivedSigned_andReconstructibleFromTheChain() {
        String s = createMeter("nm-" + UUID.randomUUID(), "0", "0");
        append(s, "IMPORT", "100", T0);     // +100
        append(s, "IMPORT", "130", T1);     // +30  → import 130
        append(s, "EXPORT", "200", T2);     // +200 → export 200, net now NEGATIVE (net feeder)
        assertMeter(s, "130", "200", "-70");

        // independent recompute from the immutable reading chain: Σ(import deltas) − Σ(export deltas)
        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/netmetering/meters/" + s + "/readings?size=50").then().statusCode(200).extract();
        List<String> dirs = hist.jsonPath().getList("data.direction").stream().map(String::valueOf).toList();
        List<String> deltas = hist.jsonPath().getList("data.delta").stream().map(String::valueOf).toList();
        BigDecimal recomputed = BigDecimal.ZERO;
        for (int i = 0; i < dirs.size(); i++) {
            BigDecimal d = new BigDecimal(deltas.get(i));
            recomputed = "IMPORT".equals(dirs.get(i)) ? recomputed.add(d) : recomputed.subtract(d);
        }
        assertThat(recomputed).as("Σ import deltas − Σ export deltas == recorded net").isEqualByComparingTo("-70");

        // the recorded BASIS net on the last reading equals the recompute (cross-check, not by-construction)
        BigDecimal lastNetAfter = new BigDecimal(hist.jsonPath().getList("data.netAfter")
            .get(dirs.size() - 1).toString());
        assertThat(lastNetAfter).isEqualByComparingTo("-70");
    }

    // ── NETM-PERIOD-001 — close snapshots both cumulatives + net delta; backdate / non-forward re-close 409 ──
    @Test @Tag("NETM-PERIOD-001")
    void periodCloseSnapshotsNetDelta_andClosedPeriodIsImmutable() {
        String s = createMeter("nm-" + UUID.randomUUID(), "0", "0");
        append(s, "IMPORT", "500", T0);      // net 500
        append(s, "EXPORT", "120", T1);      // net 380

        // close period at T2: net_start = 0, net_end = 380 → periodNetDelta = 380
        ExtractableResponse<Response> close = closePeriod(s, T2);
        assertThat(close.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(close.jsonPath().getString("importCumulative"))).isEqualByComparingTo("500");
        assertThat(new BigDecimal(close.jsonPath().getString("exportCumulative"))).isEqualByComparingTo("120");
        assertThat(new BigDecimal(close.jsonPath().getString("netStart"))).isEqualByComparingTo("0");
        assertThat(new BigDecimal(close.jsonPath().getString("netEnd"))).isEqualByComparingTo("380");
        assertThat(new BigDecimal(close.jsonPath().getString("periodNetDelta"))).isEqualByComparingTo("380");

        // a reading backdated AT/BEFORE the close boundary (T2) → 409, no retroactive shift
        ExtractableResponse<Response> back = append(s, "IMPORT", "600", T1);
        assertThat(back.statusCode()).isEqualTo(409);
        assertThat(back.path("code").toString()).isEqualTo("NETMETER_PERIOD_CLOSED");
        assertMeter(s, "500", "120", "380");

        // re-closing at/before the latest boundary (T2) → 409
        assertThat(closePeriod(s, T2).statusCode()).isEqualTo(409);

        // a forward reading (after T2) resumes; a second period delta is net_end − net_start (380)
        assertThat(append(s, "IMPORT", "700", T3).statusCode()).isEqualTo(201);   // net 700 − 120 = 580
        ExtractableResponse<Response> close2 = closePeriod(s, "2026-05-01T00:00:00Z");
        assertThat(close2.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(close2.jsonPath().getString("netStart"))).isEqualByComparingTo("380");
        assertThat(new BigDecimal(close2.jsonPath().getString("netEnd"))).isEqualByComparingTo("580");
        assertThat(new BigDecimal(close2.jsonPath().getString("periodNetDelta"))).isEqualByComparingTo("200");
    }

    // ── NETM-CONCURRENT-001 (keystone) — concurrent appends across both directions serialize on the meter row ──
    @Test @Tag("NETM-CONCURRENT-001")
    void concurrentAppends_serialize_netNeverFromAStaleCumulative() throws InterruptedException {
        String s = createMeter("nm-" + UUID.randomUUID(), "0", "0");
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 1; i <= threads; i++) {
                // alternate directions; distinct increasing values per direction (i*100)
                String dir = (i % 2 == 0) ? "EXPORT" : "IMPORT";
                String value = String.valueOf(i * 100);
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    // null effectiveAt → service uses the clock (all after Instant.MIN, never backdated)
                    codes.add(append(s, dir, value, null).statusCode());
                    return null;
                });
            }
            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(codes.stream().anyMatch(c -> c == 201)).as("at least one append commits").isTrue();

        // CONSERVATION: recorded net == Σ(committed import deltas) − Σ(committed export deltas).
        ExtractableResponse<Response> reg = getMeter(s);
        BigDecimal recordedNet = new BigDecimal(reg.jsonPath().getString("net"));
        BigDecimal cumImport = new BigDecimal(reg.jsonPath().getString("cumulativeImport"));
        BigDecimal cumExport = new BigDecimal(reg.jsonPath().getString("cumulativeExport"));
        assertThat(recordedNet).as("net == cumulativeImport − cumulativeExport (derived, no stale cumulative)")
            .isEqualByComparingTo(cumImport.subtract(cumExport));

        // independent recompute from the chain equals the recorded net
        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/netmetering/meters/" + s + "/readings?size=50").then().statusCode(200).extract();
        List<String> dirs = hist.jsonPath().getList("data.direction").stream().map(String::valueOf).toList();
        List<String> deltas = hist.jsonPath().getList("data.delta").stream().map(String::valueOf).toList();
        BigDecimal recomputed = BigDecimal.ZERO;
        for (int i = 0; i < dirs.size(); i++) {
            BigDecimal d = new BigDecimal(deltas.get(i));
            recomputed = "IMPORT".equals(dirs.get(i)) ? recomputed.add(d) : recomputed.subtract(d);
        }
        assertThat(recomputed).as("Σ committed import deltas − Σ committed export deltas == recorded net")
            .isEqualByComparingTo(recordedNet);

        // per-direction committed readings are strictly monotone by sequence
        for (String want : new String[]{"IMPORT", "EXPORT"}) {
            BigDecimal prev = BigDecimal.valueOf(-1);
            for (int i = 0; i < dirs.size(); i++) {
                if (!want.equals(dirs.get(i))) continue;
                BigDecimal cur = new BigDecimal(hist.jsonPath().getList("data.readingValue").get(i).toString());
                assertThat(cur).as(want + " committed reads strictly monotone by sequence").isGreaterThan(prev);
                prev = cur;
            }
        }
    }

    // ── NETM-RATE-001 — rate-asymmetric billed amount, derived from the SAME conserved cumulatives ──
    @Test @Tag("NETM-RATE-001")
    void closedPeriod_billedAmountIsRateAsymmetric_derivedFromConservedCumulatives() {
        String s = createMeterWithRates("nm-" + UUID.randomUUID(), "0", "0", "2.5", "1.2");
        append(s, "IMPORT", "100", T0);      // import delta 100
        append(s, "EXPORT", "40", T1);       // export delta 40

        ExtractableResponse<Response> close = closePeriod(s, T2);
        assertThat(close.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(close.jsonPath().getString("importDelta"))).isEqualByComparingTo("100");
        assertThat(new BigDecimal(close.jsonPath().getString("exportDelta"))).isEqualByComparingTo("40");
        // billed = 100*2.5 − 40*1.2 = 250 − 48 = 202
        assertThat(new BigDecimal(close.jsonPath().getString("billedAmount"))).isEqualByComparingTo("202");

        // a second period with more IMPORT only: billed keeps accruing from the NEW period-start baseline
        assertThat(append(s, "IMPORT", "150", T3).statusCode()).isEqualTo(201);   // period-2 import delta = 50
        ExtractableResponse<Response> close2 = closePeriod(s, "2026-05-01T00:00:00Z");
        assertThat(close2.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(close2.jsonPath().getString("importDelta"))).isEqualByComparingTo("50");
        assertThat(new BigDecimal(close2.jsonPath().getString("exportDelta"))).isEqualByComparingTo("0");
        assertThat(new BigDecimal(close2.jsonPath().getString("billedAmount"))).isEqualByComparingTo("125");  // 50*2.5
    }

    // ── NETM-RATE-001 — omitted rates default to 1: billed degenerates to the plain net delta ──
    @Test @Tag("NETM-RATE-001")
    void omittedRates_defaultToOne_billedEqualsNetDelta() {
        String s = createMeter("nm-" + UUID.randomUUID(), "0", "0");    // no rates supplied
        append(s, "IMPORT", "300", T0);
        append(s, "EXPORT", "80", T1);

        ExtractableResponse<Response> close = closePeriod(s, T2);
        assertThat(close.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(close.jsonPath().getString("rateImport"))).isEqualByComparingTo("1");
        assertThat(new BigDecimal(close.jsonPath().getString("rateExport"))).isEqualByComparingTo("1");
        assertThat(new BigDecimal(close.jsonPath().getString("billedAmount")))
            .isEqualByComparingTo(new BigDecimal(close.jsonPath().getString("periodNetDelta")));
    }

    // ── NETM-RATE-001 — a non-positive rate is rejected 400 at the bean-validation boundary. This
    //    catalog's convention (mirrored from thresholdterminal's identical @Positive-vs-invalidValue()
    //    shape): the DTO's @Positive is the FIRST gate and always wins over HTTP (400); the service's
    //    NetMeterException.invalidRate() (422) is defense-in-depth for a direct (non-HTTP) caller of
    //    NetMeterService — it is not exercised by this HTTP-level test, by design. ──
    @Test @Tag("NETM-RATE-001")
    void nonPositiveRate_isRejectedAtValidationBoundary_400() {
        ExtractableResponse<Response> bad = given()
            .header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"meterKey\":\"nm-" + UUID.randomUUID() + "\",\"initialImport\":0,\"initialExport\":0,"
                + "\"rateImport\":-1,\"rateExport\":1}")
        .when().post("/api/netmetering/meters").thenReturn().then().extract();
        assertThat(bad.statusCode()).isEqualTo(400);   // @Positive on the DTO catches it at the boundary
    }

    // ── RBAC — unauthenticated append is rejected; unknown meter is an IDOR-safe 404 ──
    @Test
    void rbac_unauthenticatedAppendIsRejected_unknownMeterIs404() {
        given().header("Content-Type", "application/json")
            .body("{\"direction\":\"IMPORT\",\"readingValue\":1}")
        .when().post("/api/netmetering/meters/x/readings").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/netmetering/meters/" + UUID.randomUUID())
            .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));
    }
}
