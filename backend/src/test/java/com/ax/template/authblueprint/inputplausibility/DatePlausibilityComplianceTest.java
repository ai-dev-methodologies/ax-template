package com.ax.template.authblueprint.inputplausibility;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAUSIBILITY-DATE-RANGE/FUTURE-001 compliance (BACKLOG P3-16) — verified against the live
 * date-plausibility extension of the input-plausibility reference workload. The invariant: a
 * DATE-typed self-reported asserted fact passes a window bound relative to the injected Clock's
 * reference instant — never wall-clock — is admitted ONLY as SELF_REPORTED_UNVERIFIED, and an
 * implausible date is rejected (422) AND recorded as an auditable attempt.
 *
 * <p>A FIXED clock is pinned via {@link FixedClockConfig} (mirrors EmailOutboxComplianceTest's
 * {@code Clock.fixed} convention): without it, the test would capture wall-clock "now" and the
 * SERVICE would independently re-read {@code Instant.now(clock)} milliseconds later, silently
 * shrinking the "exactly-at-tolerance" boundary case to a value strictly inside the window (an
 * inclusive→exclusive mutation would still pass) and making the "tolerance+1s rejected" case
 * sub-second wall-clock-flaky. With the clock fixed, the test's {@code FIXED} constant IS the
 * server's {@code referenceAt} for every call, so both boundary assertions are deterministic.
 * Spec: specs/self-reported-input-plausibility-l0.yaml (OWASP semantic validation + CWE-1284).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("INPUTPLAUSIBILITY")
class DatePlausibilityComplianceTest {

    static final Instant FIXED = Instant.parse("2026-05-25T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED, ZoneOffset.UTC);
        }
    }

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = PlausibilityTestSupport.obtainToken(PlausibilityTestSupport.freshEmail("date-plaus-member"), "MEMBER");
    }

    private String defineChannel(String subjectRef, long lookbackSeconds, long lookaheadSeconds) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + subjectRef + "\",\"maxLookbackSeconds\":" + lookbackSeconds
                + ",\"maxLookaheadSeconds\":" + lookaheadSeconds + "}")
        .when().post("/api/input-plausibility/date-channels").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> submit(String id, Instant assertedAt) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"assertedAt\":\"" + assertedAt + "\"}")
        .when().post("/api/input-plausibility/date-channels/" + id + "/submissions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> readings(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/input-plausibility/date-channels/" + id + "/readings").then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> rejectedAttempts(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/input-plausibility/date-channels/" + id + "/rejected-attempts").then().statusCode(200).extract();
    }

    // ── PLAUSIBILITY-DATE-RANGE-001 — inside window accepted; outside → 422 IMPLAUSIBLE_DATE_RANGE ──
    @Test @Tag("PLAUSIBILITY-DATE-RANGE-001")
    void dateRange_insideWindowAccepted_outsideWindowRejectedAndRecorded() {
        String id = defineChannel("loss-date-claim", 30L * 24 * 3600, 3L * 24 * 3600);   // 30d back, 3d ahead

        // inside the window — accepted, unverified provenance
        ExtractableResponse<Response> ok = submit(id, FIXED.minusSeconds(3600));
        assertThat(ok.statusCode()).isEqualTo(201);
        assertThat(ok.jsonPath().getString("verificationStatus")).isEqualTo("SELF_REPORTED_UNVERIFIED");

        // too far in the past — 422, not persisted as a reading, but IS recorded as a rejected attempt
        ExtractableResponse<Response> tooOld = submit(id, FIXED.minusSeconds(60L * 24 * 3600));
        assertThat(tooOld.statusCode()).isEqualTo(422);
        assertThat(tooOld.jsonPath().getString("code")).isEqualTo("IMPLAUSIBLE_DATE_RANGE");
        assertThat(readings(id).jsonPath().getList("$")).hasSize(1);   // only the accepted one
        assertThat(rejectedAttempts(id).jsonPath().getList("reason")).contains("IMPLAUSIBLE_DATE_RANGE");
    }

    // ── PLAUSIBILITY-DATE-FUTURE-001 — exact-tolerance boundary passes, tolerance+1s fails ──
    @Test @Tag("PLAUSIBILITY-DATE-FUTURE-001")
    void dateFuture_exactToleranceAccepted_oneSecondBeyondRejected() {
        long lookaheadSeconds = 7L * 24 * 3600;   // 7 days
        String id = defineChannel("event-date-claim", 0L, lookaheadSeconds);

        // exactly at the tolerance boundary (reference + maxLookahead) → accepted (inclusive upper bound).
        // FIXED is both the test's assertion basis AND the server's referenceAt (Clock.fixed never
        // advances), so this is deterministic — not a race against wall-clock drift.
        ExtractableResponse<Response> boundary = submit(id, FIXED.plusSeconds(lookaheadSeconds));
        assertThat(boundary.statusCode()).as("exactly-at-tolerance must be accepted (inclusive)").isEqualTo(201);

        // one second beyond the tolerance → fail-closed rejection
        ExtractableResponse<Response> beyond = submit(id, FIXED.plusSeconds(lookaheadSeconds + 1));
        assertThat(beyond.statusCode()).as("tolerance+1s must be rejected").isEqualTo(422);
        assertThat(beyond.jsonPath().getString("code")).isEqualTo("IMPLAUSIBLE_DATE_RANGE");
    }
}
