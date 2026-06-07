package com.ax.template.authblueprint.dispatch;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Time-dependent dispatch items (OFFER-TOCTOU-003 / AVAIL-SWEEP-001 / AVAIL-FRESH-002). Runs with a
 * 1-second offer TTL and 1-second staleness window so a short, deterministic sleep crosses the
 * deadline — no flaky timing race, no mutable-clock context surgery. Separate context from the
 * default-TTL {@link DispatchComplianceTest}. Spec: specs/timed-offer-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
    "dispatch.offer-ttl-seconds=1",
    "dispatch.staleness-window-seconds=1",
    "dispatch.max-cascade-depth=5"
})
@Tag("DISPATCH")
class DispatchExpiryComplianceTest {

    private static final long PAST_DEADLINE_MS = 1200;   // > 1s TTL / staleness

    @LocalServerPort int port;
    @Autowired DispatchSweeper sweeper;
    String admin;
    String member;

    @BeforeEach
    void setup() {
        DispatchTestSupport.useRandomPort(port);
        admin = DispatchTestSupport.obtainToken(DispatchTestSupport.freshEmail("dispx-admin"), "ADMIN");
        member = DispatchTestSupport.obtainToken(DispatchTestSupport.freshEmail("dispx-member"), "MEMBER");
    }

    private String registerProvider(String handle) {
        return given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"handle\":\"" + handle + "\"}")
        .when().post("/api/admin/dispatch/providers").then().statusCode(201).extract().path("id");
    }

    private String createRequest(String desc) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"description\":\"" + desc + "\"}")
        .when().post("/api/dispatch/requests").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> offer(String requestId, String providerId) {
        return given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"requestId\":\"" + requestId + "\",\"providerId\":\"" + providerId + "\"}")
        .when().post("/api/admin/dispatch/offers").thenReturn().then().extract();
    }

    private String offerStatus(String offerId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/dispatch/offers/" + offerId).then().statusCode(200).extract().path("status");
    }

    private String requestStatus(String requestId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/dispatch/requests/" + requestId).then().statusCode(200).extract().path("status");
    }

    // ── OFFER-TOCTOU-003 — accept after the deadline is rejected (re-check at use) ──
    @Test @Tag("OFFER-TOCTOU-003")
    void acceptAfterDeadline_isRejected() throws InterruptedException {
        String p = registerProvider("toctou");
        String r = createRequest("toctou");
        String offerId = offer(r, p).path("id");

        Thread.sleep(PAST_DEADLINE_MS);   // cross the 1s TTL — offer still nominally PENDING (sweep not run)

        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + offerId + "/accept")
        .then().statusCode(409).body("code", equalTo("OFFER_EXPIRED"));
        assertThat(requestStatus(r)).isEqualTo("OFFERED");   // not assigned
    }

    // ── AVAIL-SWEEP-001 (a) — the sweep does NOT clobber an already-accepted offer ──
    @Test @Tag("AVAIL-SWEEP-001")
    void sweepDoesNotClobberAcceptedOffer() throws InterruptedException {
        String p = registerProvider("sweep-a");
        String r = createRequest("sweep-a");
        String offerId = offer(r, p).path("id");
        // accept BEFORE the deadline
        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + offerId + "/accept").then().statusCode(200);

        Thread.sleep(PAST_DEADLINE_MS);   // deadline now passed
        sweeper.sweepOnce();              // the accepted (non-PENDING) offer is NOT in the due set

        // deterministic invariant (independent of any other test's leftover offers in the shared DB):
        // an ACCEPTED offer is never force-expired by the sweep, and its request stays ASSIGNED.
        assertThat(offerStatus(offerId)).as("sweep must not expire an accepted offer").isEqualTo("ACCEPTED");
        assertThat(requestStatus(r)).isEqualTo("ASSIGNED");
    }

    // ── AVAIL-SWEEP-001 (b) — the sweep DOES expire a due PENDING offer ──────────
    @Test @Tag("AVAIL-SWEEP-001")
    void sweepExpiresDuePendingOffer() throws InterruptedException {
        String p = registerProvider("sweep-b");
        String r = createRequest("sweep-b");
        String offerId = offer(r, p).path("id");

        Thread.sleep(PAST_DEADLINE_MS);   // offer past deadline AND provider now stale (no re-offer target)
        int swept = sweeper.sweepOnce();

        assertThat(swept).as("at least this due offer was swept (count is shared-DB-dependent)")
            .isGreaterThanOrEqualTo(1);
        assertThat(offerStatus(offerId)).isEqualTo("EXPIRED");
        assertThat(requestStatus(r)).isEqualTo("UNFULFILLED");   // no fresh candidate left -> exhausted
    }

    // ── AVAIL-FRESH-002 — a stale-heartbeat provider is not offerable ────────────
    @Test @Tag("AVAIL-FRESH-002")
    void staleProviderIsNotOfferable() throws InterruptedException {
        String p = registerProvider("stale");
        String r = createRequest("stale");

        Thread.sleep(PAST_DEADLINE_MS);   // heartbeat now older than the 1s staleness window

        ExtractableResponse<Response> resp = offer(r, p);
        assertThat(resp.statusCode()).isEqualTo(422);
        assertThat(resp.path("code").toString()).isEqualTo("PROVIDER_NOT_ELIGIBLE");
    }
}
