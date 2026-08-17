package com.ax.template.authblueprint.dispatch;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * exclusive-assignment-l0 + timed-offer-l0 compliance — every item verified against the live
 * dispatch reference workload. Default TTL/staleness (no expiry needed here); cascade depth pinned
 * to 2. The keystone is the concurrency proof (EXCL-CLAIM-001): M concurrent accepts on ONE
 * provider resolve to EXACTLY ONE winner via the atomic conditional UPDATE.
 * Expiry/freshness items live in {@link DispatchExpiryComplianceTest}. Spec: specs/exclusive-assignment-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"dispatch.max-cascade-depth=2"})
@Tag("DISPATCH")
class DispatchComplianceTest {

    @LocalServerPort int port;
    String admin;
    String member;

    @BeforeEach
    void setup() {
        admin = DispatchTestSupport.obtainToken(DispatchTestSupport.freshEmail("disp-admin"), "ADMIN");
        member = DispatchTestSupport.obtainToken(DispatchTestSupport.freshEmail("disp-member"), "MEMBER");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────
    private String registerProvider(String handle) {
        return given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"handle\":\"" + handle + "\"}")
        .when().post("/api/admin/dispatch/providers")
        .then().statusCode(201).body("status", equalTo("AVAILABLE")).extract().path("id");
    }

    private String createRequest(String desc) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"description\":\"" + desc + "\"}")
        .when().post("/api/dispatch/requests")
        .then().statusCode(201).body("status", equalTo("PENDING")).extract().path("id");
    }

    private ExtractableResponse<Response> offer(String requestId, String providerId) {
        return given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"requestId\":\"" + requestId + "\",\"providerId\":\"" + providerId + "\"}")
        .when().post("/api/admin/dispatch/offers").thenReturn().then().extract();
    }

    private String requestStatus(String requestId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/dispatch/requests/" + requestId).then().statusCode(200).extract().path("status");
    }

    private String providerStatus(String providerId) {
        return given().header("Authorization", "Bearer " + admin)
            .when().get("/api/dispatch/providers/" + providerId).then().statusCode(200).extract().path("status");
    }

    // ── OFFER-FSM-001 ───────────────────────────────────────────────────────────
    @Test @Tag("OFFER-FSM-001")
    void offerLifecycle_atMostOnePendingPerRequest() {
        String p1 = registerProvider("fsm-1");
        String p2 = registerProvider("fsm-2");
        String r = createRequest("fsm");
        ExtractableResponse<Response> offerResp = offer(r, p1);
        assertThat(offerResp.statusCode()).isEqualTo(201);
        String offerId = offerResp.path("id");
        assertThat(offerId).isNotNull();

        // a second PENDING offer for the same request is rejected (DB partial-unique backstops it)
        ExtractableResponse<Response> dup = offer(r, p2);
        assertThat(dup.statusCode()).isEqualTo(409);
        assertThat(dup.path("code").toString()).isEqualTo("DUPLICATE_PENDING_OFFER");

        // accept resolves the offer + the request
        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + offerId + "/accept")
        .then().statusCode(200).body("status", equalTo("ACCEPTED"));
        assertThat(requestStatus(r)).isEqualTo("ASSIGNED");
    }

    // ── OFFER-FSM-001 — concurrent offers for one request resolve to a single PENDING ──
    @Test @Tag("OFFER-FSM-001")
    void concurrentOffer_sameRequest_exactlyOneSucceeds() throws InterruptedException {
        String p1 = registerProvider("co-1");
        String p2 = registerProvider("co-2");
        String r = createRequest("concurrent-offer");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicReference<String> rejectCode = new AtomicReference<>();
        for (String pid : new String[]{p1, p2}) {
            pool.submit(() -> {
                try {
                    go.await();
                    ExtractableResponse<Response> resp = given().header("Authorization", "Bearer " + admin)
                        .header("Content-Type", "application/json")
                        .body("{\"requestId\":\"" + r + "\",\"providerId\":\"" + pid + "\"}")
                        .when().post("/api/admin/dispatch/offers").thenReturn().then().extract();
                    if (resp.statusCode() == 201) {
                        created.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                        rejectCode.set(resp.path("code"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // the per-request pessimistic lock serializes the two offers — exactly one PENDING is created
        assertThat(created.get()).as("exactly one concurrent offer creates a PENDING").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(rejectCode.get()).isEqualTo("DUPLICATE_PENDING_OFFER");
    }

    // ── EXCL-CLAIM-001 / EXCL-PAIR-002 / EXCL-409-004 (the keystone concurrency proof) ──
    @Test @Tag("EXCL-CLAIM-001") @Tag("EXCL-PAIR-002") @Tag("EXCL-409-004")
    void concurrentAccept_oneProviderTwoRequests_exactlyOneWins() throws InterruptedException {
        String provider = registerProvider("solo");
        String ra = createRequest("race-a");
        String rb = createRequest("race-b");
        ExtractableResponse<Response> offerRespA = offer(ra, provider);
        assertThat(offerRespA.statusCode()).isEqualTo(201);
        String offerA = offerRespA.path("id");   // provider may hold a PENDING offer per request
        ExtractableResponse<Response> offerRespB = offer(rb, provider);
        assertThat(offerRespB.statusCode()).isEqualTo(201);
        String offerB = offerRespB.path("id");   // (at-most-one-PENDING is per REQUEST, not per provider)

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicReference<String> loserCode = new AtomicReference<>();
        for (String offerId : new String[]{offerA, offerB}) {
            pool.submit(() -> {
                try {
                    go.await();
                    ExtractableResponse<Response> resp = given().header("Authorization", "Bearer " + member)
                        .when().post("/api/dispatch/offers/" + offerId + "/accept").thenReturn().then().extract();
                    if (resp.statusCode() == 200) {
                        success.incrementAndGet();
                    } else {
                        conflict.incrementAndGet();
                        loserCode.set(resp.path("code"));
                        assertThat(resp.statusCode()).isEqualTo(409);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // EXACTLY ONE winner; the loser is a 409 with the side-specific DRIVER_ALREADY_BUSY code
        assertThat(success.get()).as("exactly one accept wins the provider").isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(loserCode.get()).isEqualTo("DRIVER_ALREADY_BUSY");

        // provider claimed once; exactly one request ASSIGNED, the other rolled back to OFFERED (no partial)
        assertThat(providerStatus(provider)).isEqualTo("ASSIGNED");
        long assigned = java.util.stream.Stream.of(requestStatus(ra), requestStatus(rb))
            .filter("ASSIGNED"::equals).count();
        long offered = java.util.stream.Stream.of(requestStatus(ra), requestStatus(rb))
            .filter("OFFERED"::equals).count();
        assertThat(assigned).as("exactly one request assigned").isEqualTo(1);
        assertThat(offered).as("loser's request rolled back to OFFERED (no partial assignment)").isEqualTo(1);
    }

    // ── EXCL-409-004 — the OTHER distinct code (request side) ────────────────────
    @Test @Tag("EXCL-409-004")
    void jobAlreadyTaken_whenRequestNoLongerOfferable() {
        String provider = registerProvider("p-job");
        String r = createRequest("job");
        ExtractableResponse<Response> offerResp = offer(r, provider);
        assertThat(offerResp.statusCode()).isEqualTo(201);
        String offerId = offerResp.path("id");

        // request leaves OFFERED (cancelled) between offer and accept
        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/requests/" + r + "/cancel")
        .then().statusCode(200).body("status", equalTo("CANCELLED"));

        // accept finds the offer still PENDING (not expired) but the request no longer OFFERED -> JOB_ALREADY_TAKEN
        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + offerId + "/accept")
        .then().statusCode(409).body("code", equalTo("JOB_ALREADY_TAKEN"));
    }

    // ── OFFER-ATOMIC-002 — decline re-offers the next candidate atomically ───────
    @Test @Tag("OFFER-ATOMIC-002")
    void declineReOffersNextCandidate_inOneTransaction() {
        String p1 = registerProvider("atomic-1");
        String p2 = registerProvider("atomic-2");
        String r = createRequest("atomic");
        ExtractableResponse<Response> offerResp = offer(r, p1);
        assertThat(offerResp.statusCode()).isEqualTo(201);
        String o1 = offerResp.path("id");

        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + o1 + "/decline")
        .then().statusCode(200).body("status", equalTo("DECLINED"));

        // request stays OFFERED (re-offered, not unfulfilled); exactly one PENDING offer, now to p2 at ordinal 2
        assertThat(requestStatus(r)).isEqualTo("OFFERED");
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/dispatch/requests/" + r + "/current-offer")
        .then().statusCode(200)
            .body("status", equalTo("PENDING"))
            .body("providerId", equalTo(p2))
            .body("ordinal", equalTo(2));
        // a manual re-offer is rejected — proves there is exactly one PENDING offer
        assertThat(offer(r, p1).statusCode()).isEqualTo(409);
    }

    // ── OFFER-CASCADE-004 — bounded cascade exhausts to UNFULFILLED ──────────────
    @Test @Tag("OFFER-CASCADE-004")
    void cascadeExhaustsToUnfulfilled_atMaxDepth() {
        String p1 = registerProvider("casc-1");
        String p2 = registerProvider("casc-2");
        registerProvider("casc-3");   // a further candidate exists, but maxDepth=2 stops the cascade
        String r = createRequest("cascade");
        ExtractableResponse<Response> offerResp = offer(r, p1);
        assertThat(offerResp.statusCode()).isEqualTo(201);
        String o1 = offerResp.path("id");
        assertThat(offer(r, p1).statusCode()).isEqualTo(409);   // single pending invariant

        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + o1 + "/decline").then().statusCode(200);
        // re-offered at ordinal 2
        String o2 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/dispatch/requests/" + r + "/current-offer")
            .then().statusCode(200).body("ordinal", equalTo(2)).extract().path("id");

        given().header("Authorization", "Bearer " + member)
        .when().post("/api/dispatch/offers/" + o2 + "/decline").then().statusCode(200);
        // ordinal 2 == maxDepth -> request exhausts to UNFULFILLED (not an infinite re-offer loop)
        assertThat(requestStatus(r)).isEqualTo("UNFULFILLED");
    }

    // ── RBAC + IDOR posture (checklist) ─────────────────────────────────────────
    @Test
    void rbac_memberCannotRegisterProvider_unknownIs404_unauthIs401() {
        // dispatcher surface is ROLE_ADMIN (gated by /api/admin/**)
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"handle\":\"sneaky\"}")
        .when().post("/api/admin/dispatch/providers").then().statusCode(403);

        // unknown id -> 404 problem+json (IDOR-safe)
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/dispatch/requests/" + UUID.randomUUID())
        .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));

        // unauthenticated -> 401
        given().when().get("/api/dispatch/requests/" + UUID.randomUUID()).then().statusCode(401);
    }
}
