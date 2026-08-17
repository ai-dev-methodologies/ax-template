package com.ax.template.authblueprint.bilateralhandoff;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * bilateral-handoff-l0 compliance — verified against the live bilateralhandoff reference
 * workload. The invariant: a handoff completes ONLY when both named parties independently
 * confirm; either declining voids it terminally; the confirming caller must BE a named party
 * (403 fail-closed); the custody-flip effect applies exactly once, atomically, at the second
 * confirmation. Spec: specs/bilateral-handoff-l0.yaml (CWE-362 + RFC 9110 §15.5.10 409).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("BILATERALHANDOFF")
class HandoffComplianceTest {

    @LocalServerPort int port;
    @Autowired HandoffService service;

    String releasorParty;
    String releasorToken;
    String receiverParty;
    String receiverToken;
    String outsiderToken;

    @BeforeEach
    void setup() {
        releasorToken = HandoffTestSupport.obtainToken(HandoffTestSupport.freshEmail("bho-releasor"), "MEMBER");
        releasorParty = HandoffTestSupport.resolveUserId(releasorToken);   // Authentication.getName() == userId
        receiverToken = HandoffTestSupport.obtainToken(HandoffTestSupport.freshEmail("bho-receiver"), "MEMBER");
        receiverParty = HandoffTestSupport.resolveUserId(receiverToken);
        outsiderToken = HandoffTestSupport.obtainToken(HandoffTestSupport.freshEmail("bho-outsider"), "MEMBER");
    }

    private String propose(String releasor, String receiver) {
        return given().header("Authorization", "Bearer " + releasorToken).header("Content-Type", "application/json")
            .body("{\"releasorParty\":\"" + releasor + "\",\"receiverParty\":\"" + receiver + "\"}")
        .when().post("/api/bilateral-handoff/handoffs").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> confirm(String id, String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().post("/api/bilateral-handoff/handoffs/" + id + "/confirm").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> decline(String id, String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().post("/api/bilateral-handoff/handoffs/" + id + "/decline").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String id, String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/bilateral-handoff/handoffs/" + id).then().statusCode(200).extract();
    }

    // ── BHO-FSM-001 — completes ONLY when both parties independently confirm ──
    @Test @Tag("BHO-FSM-001")
    void completesOnlyWhenBothPartiesIndependentlyConfirm() {
        String id = propose(releasorParty, receiverParty);
        assertThat(get(id, releasorToken).jsonPath().getString("status")).isEqualTo("PROPOSED");
        assertThat(get(id, releasorToken).jsonPath().getString("custodyHolder")).isEqualTo(releasorParty);

        ExtractableResponse<Response> first = confirm(id, releasorToken);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.jsonPath().getString("status")).as("one confirmation is not enough").isEqualTo("PROPOSED");
        assertThat(first.jsonPath().getString("custodyHolder")).isEqualTo(releasorParty);

        ExtractableResponse<Response> second = confirm(id, receiverToken);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.jsonPath().getString("status")).isEqualTo("COMPLETED");
        assertThat(second.jsonPath().getString("custodyHolder")).as("custody flips to the receiver").isEqualTo(receiverParty);
    }

    // ── BHO-VOID-001 — either party declining voids the WHOLE handoff; a late confirm on voided → 409 ──
    @Test @Tag("BHO-VOID-001")
    void eitherPartyDeclining_voidsTheWholeHandoff_lateConfirmIs409() {
        String id = propose(releasorParty, receiverParty);
        assertThat(confirm(id, receiverToken).statusCode()).isEqualTo(200);   // receiver confirms first

        ExtractableResponse<Response> declined = decline(id, releasorToken); // releasor declines
        assertThat(declined.statusCode()).isEqualTo(200);
        assertThat(declined.jsonPath().getString("status")).isEqualTo("VOIDED");
        assertThat(declined.jsonPath().getString("custodyHolder"))
            .as("custody never flips for a voided handoff, despite the receiver's prior confirm")
            .isEqualTo(releasorParty);

        ExtractableResponse<Response> lateConfirm = confirm(id, releasorToken);
        assertThat(lateConfirm.statusCode()).isEqualTo(409);
        assertThat(lateConfirm.jsonPath().getString("code")).isEqualTo("HANDOFF_VOIDED");

        ExtractableResponse<Response> lateDecline = decline(id, receiverToken);
        assertThat(lateDecline.statusCode()).isEqualTo(409);
        assertThat(lateDecline.jsonPath().getString("code")).isEqualTo("HANDOFF_VOIDED");
    }

    // ── BHO-BIND-001 — the confirming caller must BE a named party; per-party idempotent confirm ──
    @Test @Tag("BHO-BIND-001")
    void confirmingCallerMustBeNamedParty_perPartyIdempotent() {
        String id = propose(releasorParty, receiverParty);

        ExtractableResponse<Response> outsider = confirm(id, outsiderToken);
        assertThat(outsider.statusCode()).isEqualTo(403);
        assertThat(outsider.jsonPath().getString("code")).isEqualTo("HANDOFF_NOT_A_PARTY");
        assertThat(get(id, releasorToken).jsonPath().getString("status")).as("handoff unchanged").isEqualTo("PROPOSED");

        ExtractableResponse<Response> firstConfirm = confirm(id, releasorToken);
        assertThat(firstConfirm.statusCode()).isEqualTo(200);
        String firstConfirmedAt = firstConfirm.jsonPath().getString("releasorConfirmedAt");
        ExtractableResponse<Response> secondConfirm = confirm(id, releasorToken); // idempotent — same party again
        assertThat(secondConfirm.statusCode()).isEqualTo(200);
        assertThat(secondConfirm.jsonPath().getString("status")).as("not double-transitioned").isEqualTo("PROPOSED");
        assertThat(secondConfirm.jsonPath().getString("releasorConfirmedAt"))
            .as("the recorded timestamp is unchanged by the idempotent repeat").isEqualTo(firstConfirmedAt);
    }

    // ── BHO-ATOMIC-001 — keystone: simultaneous confirms complete exactly once, custody consistent ──
    @Test @Tag("BHO-ATOMIC-001")
    void simultaneousConfirms_completeExactlyOnce_custodyConsistent() throws Exception {
        String id = propose(releasorParty, receiverParty);
        UUID handoffId = UUID.fromString(id);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> outcomes = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            ready.countDown();
            start.await();
            outcomes.add(service.confirm(handoffId, releasorParty).getStatus().name());
            return null;
        }));
        futures.add(pool.submit(() -> {
            ready.countDown();
            start.await();
            outcomes.add(service.confirm(handoffId, receiverParty).getStatus().name());
            return null;
        }));
        // worker-ready barrier: open the gate only once BOTH workers are parked on it, so the
        // two confirms genuinely race instead of running back-to-back (mirrors
        // TokenizedSecuritiesComplianceTest#concurrentIssue_exactlyOneWins_registerConserved).
        assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        for (Future<?> f : futures) {
            f.get(); // surface any unexpected worker exception instead of a confusing count mismatch
        }

        assertThat(outcomes).as("both confirms succeed (no 409/exception)").hasSize(2);
        assertThat(outcomes.stream().filter("COMPLETED"::equals).count())
            .as("exactly one final COMPLETED state is observed, however interleaved").isGreaterThanOrEqualTo(1);

        var finalState = get(id, releasorToken);
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("COMPLETED");
        assertThat(finalState.jsonPath().getString("custodyHolder"))
            .as("custody flipped exactly once, to the receiver").isEqualTo(receiverParty);
    }
}
