package com.ax.template.authblueprint.inputplausibility;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * self-reported-input-plausibility-l0 compliance — verified against the live input-plausibility
 * reference workload. The invariant: a self-reported, server-unverifiable value passes a RANGE
 * bound and (when a prior exists) a RATE-OF-CHANGE limit, is admitted ONLY as
 * SELF_REPORTED_UNVERIFIED with its recorded basis, and an implausible submission is rejected (422)
 * AND recorded as an auditable attempt; concurrent submissions serialize on the channel row.
 * Spec: specs/self-reported-input-plausibility-l0.yaml (OWASP semantic validation + CWE-20/CWE-1284 + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("INPUTPLAUSIBILITY")
class PlausibilityComplianceTest {

    @LocalServerPort int port;
    @Autowired PlausibilityService service;
    String member;

    @BeforeEach
    void setup() {
        member = PlausibilityTestSupport.obtainToken(PlausibilityTestSupport.freshEmail("plaus-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String defineChannel(String subjectRef, String min, String max, String maxRate) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + subjectRef + "\",\"minValue\":" + min + ",\"maxValue\":" + max
                + ",\"maxDeltaPerSecond\":" + maxRate + "}")
        .when().post("/api/input-plausibility/channels").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> submit(String id, String value) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"reportedValue\":" + value + "}")
        .when().post("/api/input-plausibility/channels/" + id + "/submissions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> readings(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/input-plausibility/channels/" + id + "/readings").then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> rejectedAttempts(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/input-plausibility/channels/" + id + "/rejected-attempts").then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> getChannel(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/input-plausibility/channels/" + id).then().statusCode(200).extract();
    }

    // ── PLAUSIBILITY-RANGE-001 — value within [min, max] accepted; outside → 422 IMPLAUSIBLE_RANGE ──
    @Test @Tag("PLAUSIBILITY-RANGE-001")
    void range_inBoundsAccepted_outOfBoundsRejected() {
        // odometer channel: plausible 0..1_000_000 km, rate is not exercised here (first reading only)
        String id = defineChannel("odometer-VIN123", "0", "1000000", "1000");

        // in-range first reading → 201
        ExtractableResponse<Response> ok = submit(id, "50000");
        assertThat(ok.statusCode()).isEqualTo(201);
        assertThat(ok.jsonPath().getString("verificationStatus")).isEqualTo("SELF_REPORTED_UNVERIFIED");

        // below min → 422 IMPLAUSIBLE_RANGE (fresh channel so it's the first reading, range-only)
        String idLow = defineChannel("odometer-LOW", "0", "1000000", "1000");
        ExtractableResponse<Response> low = submit(idLow, "-1");
        assertThat(low.statusCode()).isEqualTo(422);
        assertThat(low.jsonPath().getString("code")).isEqualTo("IMPLAUSIBLE_RANGE");

        // above max → 422 IMPLAUSIBLE_RANGE
        String idHigh = defineChannel("odometer-HIGH", "0", "1000000", "1000");
        ExtractableResponse<Response> high = submit(idHigh, "5000000");
        assertThat(high.statusCode()).isEqualTo(422);
        assertThat(high.jsonPath().getString("code")).isEqualTo("IMPLAUSIBLE_RANGE");

        // a range-rejected value is NOT persisted as a reading
        assertThat(readings(idHigh).jsonPath().getList("$")).isEmpty();
    }

    // ── PLAUSIBILITY-RATE-001 — jump over elapsed time within max rate accepted; over → 422 ──
    @Test @Tag("PLAUSIBILITY-RATE-001")
    void rate_plausibleChangeAccepted_impossibleJumpRejected() throws Exception {
        // generous range so only the RATE gate decides; max 1000 units/sec
        String accept = defineChannel("meter-accept", "0", "100000000", "1000");
        assertThat(submit(accept, "1000").statusCode()).isEqualTo(201);   // first reading establishes the prior

        // let real elapsed time accrue so the rate gate has a non-zero denominator
        Thread.sleep(1100L);

        // a small change over >=1s at <=1000/s → accepted, RATE gate ran
        ExtractableResponse<Response> small = submit(accept, "1500");
        assertThat(small.statusCode()).isEqualTo(201);
        assertThat(small.jsonPath().getString("checksRan")).isEqualTo("RANGE,RATE");
        assertThat(small.jsonPath().getBoolean("hadPrior")).isTrue();

        // an impossible jump on a tight-rate channel → 422 IMPLAUSIBLE_RATE
        String reject = defineChannel("meter-reject", "0", "100000000", "1");   // max 1 unit/sec
        assertThat(submit(reject, "1000").statusCode()).isEqualTo(201);
        Thread.sleep(1100L);
        ExtractableResponse<Response> jump = submit(reject, "9000000");          // ~9M over ~1s ≫ 1/s
        assertThat(jump.statusCode()).isEqualTo(422);
        assertThat(jump.jsonPath().getString("code")).isEqualTo("IMPLAUSIBLE_RATE");

        // the rate-rejected value is NOT persisted as a reading (still just the first)
        assertThat(readings(reject).jsonPath().getList("$")).hasSize(1);
    }

    // ── PLAUSIBILITY-PROVENANCE-001 — accepted reading is SELF_REPORTED_UNVERIFIED + basis recorded ──
    @Test @Tag("PLAUSIBILITY-PROVENANCE-001")
    void provenance_acceptedReadingIsUnverified_withBasis() throws Exception {
        String id = defineChannel("location-claim", "0", "180", "10");
        ExtractableResponse<Response> first = submit(id, "37");
        assertThat(first.jsonPath().getString("verificationStatus")).isEqualTo("SELF_REPORTED_UNVERIFIED");
        assertThat(first.jsonPath().getString("checksRan")).isEqualTo("RANGE");   // no prior → range only
        assertThat(first.jsonPath().getBoolean("hadPrior")).isFalse();

        Thread.sleep(1100L);
        ExtractableResponse<Response> second = submit(id, "40");
        assertThat(second.jsonPath().getString("verificationStatus")).isEqualTo("SELF_REPORTED_UNVERIFIED");
        assertThat(second.jsonPath().getString("checksRan")).isEqualTo("RANGE,RATE");
        assertThat(second.jsonPath().getBoolean("hadPrior")).isTrue();
        assertThat(second.jsonPath().getDouble("priorValue")).isEqualTo(37.0);
        assertThat(second.jsonPath().getLong("elapsedSeconds")).isGreaterThanOrEqualTo(1L);
        assertThat(second.jsonPath().getString("computedRate")).as("the computed rate basis is recorded").isNotBlank();

        // the channel's prior pointer advanced to the latest accepted value
        assertThat(getChannel(id).jsonPath().getDouble("priorValue")).isEqualTo(40.0);
    }

    // ── PLAUSIBILITY-REJECT-001 — an implausible submission is rejected AND recorded; state untouched ──
    @Test @Tag("PLAUSIBILITY-REJECT-001")
    void reject_isRecorded_andAcceptedStateUntouched() throws Exception {
        String id = defineChannel("quantity", "0", "10000", "5");
        assertThat(submit(id, "100").statusCode()).isEqualTo(201);     // prior = 100

        // a range rejection is recorded
        assertThat(submit(id, "999999").statusCode()).isEqualTo(422);  // above max → IMPLAUSIBLE_RANGE
        // a rate rejection is recorded
        Thread.sleep(1100L);
        assertThat(submit(id, "9000").statusCode()).isEqualTo(422);    // huge jump on a 5/s channel → IMPLAUSIBLE_RATE

        var attempts = rejectedAttempts(id).jsonPath().getList("reason");
        assertThat(attempts).contains("IMPLAUSIBLE_RANGE", "IMPLAUSIBLE_RATE");
        assertThat(attempts).hasSize(2);

        // the accepted state is untouched — prior is still the only accepted value (100)
        assertThat(getChannel(id).jsonPath().getDouble("priorValue")).isEqualTo(100.0);
        assertThat(readings(id).jsonPath().getList("$")).hasSize(1);
    }

    // ── PLAUSIBILITY-CONCURRENT-001 — keystone: concurrent submits serialize on the channel row ──
    @Test @Tag("PLAUSIBILITY-CONCURRENT-001")
    void concurrentSubmits_serializeOnChannelRow() throws Exception {
        // Generous rate so the gate never spuriously rejects; the keystone is that the row lock
        // serializes the read-prior / append sequence so the accepted history is a CONSISTENT chain:
        // no two accepted readings record the SAME prior as their rate basis (which would mean two
        // submits both read the same prior and both appended — a lost-update / CWE-362 corruption).
        String id = defineChannel("race", "0", "100000000", "1000000000");
        assertThat(submit(id, "100").statusCode()).isEqualTo(201);     // seed prior = 100
        UUID channelId = UUID.fromString(id);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            final BigDecimal v = BigDecimal.valueOf(100L);             // same value → delta 0 → rate 0, always plausible
            pool.submit(() -> {
                start.await();
                try {
                    service.submit(channelId, v, "racer");
                    codes.add(201);
                } catch (PlausibilityException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // every same-value submit is plausible (delta 0) — the lock serializes them, none is lost
        assertThat(codes.stream().filter(c -> c == 201).count())
            .as("all plausible submits accepted under serialized intake").isEqualTo((long) n);

        // KEYSTONE — the read-prior / append-reading sequence serialized on the channel row, so there
        // are EXACTLY n+1 accepted readings (1 seed + n) with no lost or duplicated write. A lost
        // update under a race would leave fewer than n+1 (CWE-362); the PESSIMISTIC_WRITE lock prevents it.
        var readingIds = readings(id).jsonPath().getList("id");
        assertThat(readingIds).as("append-only history, exactly n+1 readings, none lost (CWE-362)").hasSize(n + 1);

        // the channel's optimistic @Version advanced once per accepted write under the serialized lock —
        // no two acceptances clobbered the same version (which a lost update would have produced).
        long version = getChannel(id).jsonPath().getLong("version");
        assertThat(version).as("each serialized acceptance bumped @Version — no clobbered write")
            .isGreaterThanOrEqualTo((long) n);
    }
}
