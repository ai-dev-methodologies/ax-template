package com.ax.template.authblueprint.duplicatesubmission;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * duplicate-submission-key-l0 compliance — verified against the live duplicatesubmission
 * reference workload. The invariant: an exact natural same-loss key match against an ACTIVE
 * submission is a deterministic 409 referencing the existing one; a near (fuzzy-window) match is
 * accepted but flagged for review; withdrawing a submission releases its key for legitimate
 * resubmission.
 * Spec: specs/duplicate-submission-key-l0.yaml (insurance duplicate-claim detection practice + CWE-694).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DUPLICATESUBMISSION")
class DuplicateSubmissionComplianceTest {

    @LocalServerPort int port;
    @Autowired DuplicateSubmissionService service;
    String member;

    @BeforeEach
    void setup() {
        member = DuplicateSubmissionTestSupport.obtainToken(
            DuplicateSubmissionTestSupport.freshEmail("dupkey-member"), "MEMBER");
    }

    private String defineChannel(String scopeLabel, int fuzzyWindowDays) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeLabel\":\"" + scopeLabel + "\",\"fuzzyWindowDays\":" + fuzzyWindowDays + "}")
        .when().post("/api/duplicate-submissions/channels").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> submit(String channelId, String subjectRef, LocalDate lossDate, String lossType) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + subjectRef + "\",\"lossDate\":\"" + lossDate + "\",\"lossType\":\"" + lossType + "\"}")
        .when().post("/api/duplicate-submissions/channels/" + channelId + "/submissions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> withdraw(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/duplicate-submissions/submissions/" + id + "/withdraw").thenReturn().then().extract();
    }

    // ── DUPKEY-NATURAL-001 — exact key match against an ACTIVE submission → 409, references existing ──
    @Test @Tag("DUPKEY-NATURAL-001")
    void naturalKey_exactMatchAgainstActive_isRejectedReferencingExisting() {
        String channelId = defineChannel("auto-claims", 0);
        LocalDate lossDate = LocalDate.of(2026, 3, 1);

        ExtractableResponse<Response> first = submit(channelId, "subject-A", lossDate, "COLLISION");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");

        ExtractableResponse<Response> dup = submit(channelId, "subject-A", lossDate, "COLLISION");
        assertThat(dup.statusCode()).isEqualTo(409);
        assertThat(dup.jsonPath().getString("code")).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(dup.jsonPath().getString("conflictingSubmissionId")).isEqualTo(firstId);
    }

    // ── DUPKEY-FUZZY-002 — a near-match in the fuzzy window is accepted but flagged, linked to the suspect ──
    @Test @Tag("DUPKEY-FUZZY-002")
    void fuzzy_nearMatchWithinWindow_acceptedButFlagged_outsideWindowNotFlagged() {
        String channelId = defineChannel("home-claims", 3);   // 3-day fuzzy window
        LocalDate lossDate = LocalDate.of(2026, 5, 10);

        ExtractableResponse<Response> first = submit(channelId, "subject-B", lossDate, "FIRE");
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.jsonPath().getBoolean("flaggedForReview")).isFalse();
        String firstId = first.jsonPath().getString("id");

        // 2 days later, same subject+type, DIFFERENT loss date → distinct natural key, near-match band
        ExtractableResponse<Response> near = submit(channelId, "subject-B", lossDate.plusDays(2), "FIRE");
        assertThat(near.statusCode()).as("a near-match is accepted, never hard-rejected").isEqualTo(201);
        assertThat(near.jsonPath().getBoolean("flaggedForReview")).isTrue();
        assertThat(near.jsonPath().getString("suspectSubmissionId")).isEqualTo(firstId);

        // 10 days later — outside the 3-day fuzzy window → not flagged
        ExtractableResponse<Response> far = submit(channelId, "subject-B", lossDate.plusDays(10), "FIRE");
        assertThat(far.statusCode()).isEqualTo(201);
        assertThat(far.jsonPath().getBoolean("flaggedForReview")).isFalse();
    }

    // ── DUPKEY-WITHDRAWN-003 — withdrawal releases the key; resubmission of the same key succeeds ──
    @Test @Tag("DUPKEY-WITHDRAWN-003")
    void withdrawn_releasesKey_resubmissionSucceeds_doubleWithdrawConflicts() {
        String channelId = defineChannel("ticket-intake", 0);
        LocalDate lossDate = LocalDate.of(2026, 6, 1);

        ExtractableResponse<Response> first = submit(channelId, "subject-C", lossDate, "OUTAGE");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");

        ExtractableResponse<Response> withdrawResp = withdraw(firstId);
        assertThat(withdrawResp.statusCode()).isEqualTo(200);
        assertThat(withdrawResp.jsonPath().getString("status")).isEqualTo("WITHDRAWN");

        // the SAME natural key resubmits cleanly — a new submission, not a 409
        ExtractableResponse<Response> resubmit = submit(channelId, "subject-C", lossDate, "OUTAGE");
        assertThat(resubmit.statusCode()).as("resubmission after withdrawal must succeed").isEqualTo(201);
        assertThat(resubmit.jsonPath().getString("id")).isNotEqualTo(firstId);

        // double-withdraw is a 409 — the transition is terminal
        ExtractableResponse<Response> doubleWithdraw = withdraw(firstId);
        assertThat(doubleWithdraw.statusCode()).isEqualTo(409);
        assertThat(doubleWithdraw.jsonPath().getString("code")).isEqualTo("SUBMISSION_ILLEGAL_TRANSITION");
    }

    // ── DUPKEY-NATURAL-001 keystone — the UNIQUE(channel_id, active_key) constraint, not just the
    // pre-check, resolves a genuine concurrent race: two threads can both pass findActiveByNaturalKey
    // (neither has committed yet) and both attempt to insert — only the DataIntegrityViolationException
    // → 409 translation on the LOSER proves the DB backstop is load-bearing, not vacuous.
    @Test @Tag("DUPKEY-NATURAL-001")
    void concurrentSameKeySubmits_exactlyOneWins_uniqueConstraintBackstopsThePreCheck() throws Exception {
        String channelId = defineChannel("race-claims", 0);
        UUID channelUuid = UUID.fromString(channelId);
        LocalDate lossDate = LocalDate.of(2026, 7, 1);

        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.submit(channelUuid, "subject-race", lossDate, "RACE", "racer");
                    codes.add(201);
                } catch (DuplicateSubmissionException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 201).count())
            .as("exactly one concurrent submission of the SAME natural key must win").isEqualTo(1L);
        assertThat(codes.stream().filter(c -> c == 409).count())
            .as("the loser must see a deterministic 409 — the UNIQUE constraint backstops the pre-check race")
            .isEqualTo(1L);
    }
}
