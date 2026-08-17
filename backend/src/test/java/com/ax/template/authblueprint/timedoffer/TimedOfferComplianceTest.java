package com.ax.template.authblueprint.timedoffer;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * timed-offer-exclusive-assignment-l0 compliance — verified against the live timedoffer reference
 * workload. The invariant: an offer is OPEN until accept/decline/deadline; a sweep expires
 * past-deadline OPEN offers EXACTLY ONCE (recorded SYSTEM/when); at most ONE offer per subject is
 * ACCEPTED (the competing-accept loser gets 409); a declined/expired offer is re-offered as a NEW
 * append-only row; concurrent accepts for one subject converge so exactly one wins.
 * Spec: specs/timed-offer-exclusive-assignment-l0.yaml (CWE-362 + RFC 9110 §15.5.10 409 + FDCPA timed-notice).
 *
 * Time-determinism: deadlines are UTC Instants (TZ-agnostic) read against the injected Clock; past
 * deadlines use Instant.now().minusSeconds(...) with comfortable margin so no boundary flake.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TIMEDOFFER")
class TimedOfferComplianceTest {

    @LocalServerPort int port;
    @Autowired TimedOfferService service;
    @Autowired TimedOfferSweeper sweeper;
    String member;

    @BeforeEach
    void setup() {
        member = TimedOfferTestSupport.obtainToken(TimedOfferTestSupport.freshEmail("to-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String extend(String subjectId, String candidate, Instant deadline) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectId\":\"" + subjectId + "\",\"candidate\":\"" + candidate + "\","
                + "\"deadline\":\"" + deadline + "\"}")
        .when().post("/api/timed-offer/offers").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> accept(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/timed-offer/offers/" + id + "/accept").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> decline(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/timed-offer/offers/" + id + "/decline").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> reoffer(String id, String nextCandidate, Instant deadline) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"nextCandidate\":\"" + nextCandidate + "\",\"deadline\":\"" + deadline + "\"}")
        .when().post("/api/timed-offer/offers/" + id + "/reoffer").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getOffer(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/timed-offer/offers/" + id).then().statusCode(200).extract();
    }

    private String subject(String tag) {
        return "SUBJ-" + tag + "-" + UUID.randomUUID();
    }

    // ── TIMEDOFFER-LIFECYCLE-001 — OPEN until accept; acting on a terminal offer → 409 ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001")
    void offer_isOpenUntilAccepted_terminalRefused() {
        String id = extend(subject("LIFE"), "candidate-a", Instant.now().plusSeconds(3600));
        assertThat(getOffer(id).jsonPath().getString("status")).isEqualTo("OPEN");

        ExtractableResponse<Response> accepted = accept(id);
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(accepted.jsonPath().getString("status")).isEqualTo("ACCEPTED");
        assertThat(accepted.jsonPath().getString("decidedBy")).as("the deciding actor is recorded").isNotBlank();
        assertThat(accepted.jsonPath().getString("decidedAt")).isNotBlank();

        // accepting/declining a terminal offer → 409 TIMEDOFFER_NOT_OPEN
        ExtractableResponse<Response> again = accept(id);
        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.jsonPath().getString("code")).isEqualTo("TIMEDOFFER_NOT_OPEN");
        ExtractableResponse<Response> dec = decline(id);
        assertThat(dec.statusCode()).isEqualTo(409);
        assertThat(dec.jsonPath().getString("code")).isEqualTo("TIMEDOFFER_NOT_OPEN");
    }

    // ── TIMEDOFFER-LIFECYCLE-001 — the deadline sweep expires past-deadline OPEN offers, recorded SYSTEM ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001")
    void sweep_expiresPastDeadlineOpenOffers_exactlyOnce_recordedSystem() {
        String id = extend(subject("SWEEP"), "candidate-late", Instant.now().minusSeconds(60)); // already past
        assertThat(getOffer(id).jsonPath().getString("status")).isEqualTo("OPEN");

        int swept = sweeper.sweepOnce();
        assertThat(swept).as("at least the due offer was visited").isGreaterThanOrEqualTo(1);

        ExtractableResponse<Response> expired = getOffer(id);
        assertThat(expired.jsonPath().getString("status")).isEqualTo("EXPIRED");
        assertThat(expired.jsonPath().getString("decidedBy")).as("SYSTEM expired it").isEqualTo("SYSTEM");
        assertThat(expired.jsonPath().getString("decidedAt")).as("when is recorded").isNotBlank();

        // a second sweep pass is an idempotent no-op (still EXPIRED, no double-record / exception)
        sweeper.sweepOnce();
        assertThat(getOffer(id).jsonPath().getString("status")).isEqualTo("EXPIRED");
    }

    // ── TIMEDOFFER-LIFECYCLE-001 — a past-deadline offer cannot be accepted (409) ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001")
    void accept_pastDeadline_is409() {
        String id = extend(subject("PASTDDL"), "candidate-x", Instant.now().minusSeconds(30));
        ExtractableResponse<Response> bad = accept(id);
        assertThat(bad.statusCode()).isEqualTo(409);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("TIMEDOFFER_EXPIRED");
    }

    // ── TIMEDOFFER-EXCLUSIVE-001 — at most one accepted offer per subject; the loser gets 409 ──
    @Test @Tag("TIMEDOFFER-EXCLUSIVE-001")
    void exclusive_competingAccept_loserGets409() {
        String subj = subject("EXCL");
        String a = extend(subj, "candidate-a", Instant.now().plusSeconds(3600));
        String b = extend(subj, "candidate-b", Instant.now().plusSeconds(3600));   // competing offer, same subject

        assertThat(accept(a).statusCode()).isEqualTo(200);

        ExtractableResponse<Response> loser = accept(b);     // different offer, same subject → loser
        assertThat(loser.statusCode()).isEqualTo(409);
        assertThat(loser.jsonPath().getString("code")).isEqualTo("TIMEDOFFER_SUBJECT_ALREADY_ASSIGNED");

        // the subject's ladder shows exactly one ACCEPTED offer
        var statuses = given().header("Authorization", "Bearer " + member)
            .when().get("/api/timed-offer/subjects/" + subj + "/ladder")
            .then().statusCode(200).extract().jsonPath().getList("status");
        assertThat(statuses.stream().filter("ACCEPTED"::equals).count())
            .as("exactly one accepted offer per subject").isEqualTo(1L);
    }

    // ── TIMEDOFFER-LADDER-001 — decline → re-offer to next candidate as a NEW append-only row ──
    @Test @Tag("TIMEDOFFER-LADDER-001")
    void ladder_declineThenReoffer_appendsNewRow_monotonicAttempt() {
        String subj = subject("LADDER");
        String first = extend(subj, "candidate-1", Instant.now().plusSeconds(3600));
        assertThat(getOffer(first).jsonPath().getInt("attemptSeq")).isEqualTo(1);

        assertThat(decline(first).statusCode()).isEqualTo(200);

        ExtractableResponse<Response> second = reoffer(first, "candidate-2", Instant.now().plusSeconds(3600));
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(second.jsonPath().getInt("attemptSeq")).as("monotonic attempt").isEqualTo(2);
        assertThat(second.jsonPath().getString("priorOfferId")).as("references the prior offer").isEqualTo(first);
        assertThat(second.jsonPath().getString("candidate")).isEqualTo("candidate-2");

        // the ladder is append-only: the first row is untouched (still DECLINED, candidate-1)
        assertThat(getOffer(first).jsonPath().getString("status")).isEqualTo("DECLINED");
        assertThat(getOffer(first).jsonPath().getString("candidate")).isEqualTo("candidate-1");

        var ladder = given().header("Authorization", "Bearer " + member)
            .when().get("/api/timed-offer/subjects/" + subj + "/ladder")
            .then().statusCode(200).extract().jsonPath().getList("attemptSeq");
        assertThat(ladder).containsExactly(1, 2);
    }

    // ── TIMEDOFFER-LADDER-001 — re-offering an OPEN offer is refused (422) ──
    @Test @Tag("TIMEDOFFER-LADDER-001")
    void reoffer_openOffer_is422() {
        String id = extend(subject("REOFOPEN"), "candidate-1", Instant.now().plusSeconds(3600));
        ExtractableResponse<Response> bad = reoffer(id, "candidate-2", Instant.now().plusSeconds(3600));
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("TIMEDOFFER_NOT_REOFFERABLE");
    }

    // ── TIMEDOFFER-CONCURRENT-001 — keystone: N concurrent accepts for one subject → exactly one wins ──
    @Test @Tag("TIMEDOFFER-CONCURRENT-001")
    void concurrentAccepts_acrossCompetingOffers_exactlyOneWins() throws Exception {
        String subj = subject("RACE");
        Instant deadline = Instant.now().plusSeconds(3600);
        int n = 8;
        UUID[] offerIds = new UUID[n];
        for (int i = 0; i < n; i++) {
            offerIds[i] = UUID.fromString(extend(subj, "candidate-" + i, deadline)); // n competing offers, one subject
        }

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            final UUID offerId = offerIds[i];
            pool.submit(() -> {
                start.await();
                try {
                    service.accept(offerId, "racer");
                    codes.add(200);
                } catch (TimedOfferException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("TIMEDOFFER-CONCURRENT-001 — exactly one accept wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the subject has exactly one ACCEPTED offer (one Assignment row)
        var statuses = given().header("Authorization", "Bearer " + member)
            .when().get("/api/timed-offer/subjects/" + subj + "/ladder")
            .then().statusCode(200).extract().jsonPath().getList("status");
        assertThat(statuses.stream().filter("ACCEPTED"::equals).count())
            .as("exactly one assignment for the subject").isEqualTo(1L);
    }
}
