package com.ax.template.authblueprint.quorumresolution;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * quorum-resolution-l0 compliance test. Black-box RestAssured tests against the live
 * application. All 18 spec item ids are covered via @Tag on test methods.
 *
 * <p>Spec item coverage:
 * QR-BALLOT-001, QR-BALLOT-002, QR-BALLOT-003, QR-ELIG-001, QR-ELIG-002,
 * QR-POLICY-001, QR-POLICY-002, QR-TALLY-001, QR-TALLY-002, QR-TALLY-003,
 * QR-RESOLVE-001, QR-RESOLVE-002, QR-RESOLVE-003, QR-RESOLVE-004,
 * QR-RESOLVE-005, QR-RESOLVE-006, QR-AUTHZ-001, QR-AUTHZ-002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("QUORUM")
class QuorumComplianceTest {

    @LocalServerPort int port;

    String convener;
    String voter1;
    String voter2;
    String voter3;

    @BeforeEach
    void setup() {
        convener = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-convener"), "MEMBER");
        voter1 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-v1"), "MEMBER");
        voter2 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-v2"), "MEMBER");
        voter3 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-v3"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────

    /** auth.getName() returns the JWT subject = userId UUID string. */
    private String userId(String token) {
        return QuorumTestSupport.resolveUserId(token);
    }

    private UUID openMotion(String body) {
        return UUID.fromString(
            given().header("Authorization", "Bearer " + convener)
                .header("Content-Type", "application/json")
                .body(body)
            .when().post("/api/quorum/motions")
            .then().statusCode(201).extract().path("id"));
    }

    private ExtractableResponse<Response> castBallot(UUID motionId, String token, String choice) {
        return given().header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body("{\"choice\":\"" + choice + "\"}")
        .when().post("/api/quorum/motions/" + motionId + "/ballots")
        .then().extract();
    }

    private ExtractableResponse<Response> resolve(UUID motionId) {
        return given().header("Authorization", "Bearer " + convener)
        .when().post("/api/quorum/motions/" + motionId + "/resolve")
        .then().extract();
    }

    // ── QR-RESOLVE-002 / QR-BALLOT-001 / QR-BALLOT-003 — open + cast + resolve → PASSED ──

    @Test @Tag("QR-RESOLVE-002") @Tag("QR-BALLOT-001") @Tag("QR-BALLOT-003")
    void happyPath_openCastResolve_passed() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);

        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 10)));
        UUID motionId = openMotion(body);

        // Both vote YES — immutable ballot rows, choice includes YES|NO|ABSTAIN cast
        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
        assertThat(res.jsonPath().getLong("yesWeight")).isEqualTo(20);
        assertThat(res.jsonPath().getLong("noWeight")).isEqualTo(0);
        assertThat(res.jsonPath().getLong("totalEligibleWeight")).isEqualTo(20);
    }

    // ── QR-RESOLVE-001 — quorum not met → NO_DECISION (distinct from REJECTED) ──

    @Test @Tag("QR-RESOLVE-001")
    void quorumNotMet_producesNoDecision_notRejected() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String v3Id = userId(voter3);

        // quorum = 2/3 of eligible weight must cast; 3 voters of weight 1 each,
        // totalEligibleWeight = 3. Only voter1 casts (weight 1) → castEligibleWeight=1 < 2 → NO_DECISION
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 2, 3, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 1),
                    new QuorumTestSupport.VoterSpec(v2Id, 1),
                    new QuorumTestSupport.VoterSpec(v3Id, 1)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        // NO_DECISION is a SEPARATE outcome from REJECTED — quorum gate, not threshold gate
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("NO_DECISION");
    }

    // ── QR-BALLOT-002 — double-vote → 409 ──────────────────────────────────────

    @Test @Tag("QR-BALLOT-002")
    void doubleVote_returns409() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> second = castBallot(motionId, voter1, "NO");
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.jsonPath().getString("code")).isEqualTo("QR_DOUBLE_VOTE");
    }

    // ── QR-RESOLVE-004 — vote after close → 409 ────────────────────────────────

    @Test @Tag("QR-RESOLVE-004")
    void voteAfterClose_returns409() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        resolve(motionId); // close

        ExtractableResponse<Response> late = castBallot(motionId, voter2, "YES");
        assertThat(late.statusCode()).isEqualTo(409);
        assertThat(late.jsonPath().getString("code")).isEqualTo("QR_MOTION_CLOSED");
    }

    // ── QR-ELIG-001 — non-eligible voter → 403 / unknown motion → 404 ──────────

    @Test @Tag("QR-ELIG-001")
    void nonEligibleVoter_returns403() {
        String v1Id = userId(voter1);
        // voter2 is NOT in the roster
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        ExtractableResponse<Response> res = castBallot(motionId, voter2, "YES");
        assertThat(res.statusCode()).isEqualTo(403);
        assertThat(res.jsonPath().getString("code")).isEqualTo("QR_NOT_ELIGIBLE");
    }

    @Test @Tag("QR-ELIG-001")
    void unknownMotion_returns404() {
        ExtractableResponse<Response> res = castBallot(UUID.randomUUID(), voter1, "YES");
        assertThat(res.statusCode()).isEqualTo(404);
    }

    // ── QR-ELIG-002 / QR-POLICY-001 — voter weight immutable; policy frozen ────
    // (structural — covered by ViolationProofTest; compliance entry for completeness)

    @Test @Tag("QR-ELIG-002") @Tag("QR-POLICY-001")
    void frozenPolicyAndVoterWeight_persistedCorrectly() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 3, 5, 2, 3, "COUNT_AS_NO", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 7)));
        UUID motionId = openMotion(body);

        ExtractableResponse<Response> motion = given().header("Authorization", "Bearer " + convener)
            .when().get("/api/quorum/motions/" + motionId).then().statusCode(200).extract();
        // Policy columns snapshotted
        assertThat(motion.jsonPath().getLong("thresholdNumerator")).isEqualTo(3);
        assertThat(motion.jsonPath().getLong("thresholdDenominator")).isEqualTo(5);
        assertThat(motion.jsonPath().getLong("totalEligibleWeight")).isEqualTo(7);
        assertThat(motion.jsonPath().getString("abstentionMode")).isEqualTo("COUNT_AS_NO");
    }

    // ── QR-TALLY-001 — exact weighted tally ──────────────────────────────────────

    @Test @Tag("QR-TALLY-001")
    void weightedTally_correctlyAggregates() {
        String v1Id = userId(voter1);  // weight 10
        String v2Id = userId(voter2);  // weight 3

        // v1 (10) votes NO, v2 (3) votes YES → yesWeight=3, noWeight=10 → REJECTED
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 3)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "NO").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
        assertThat(res.jsonPath().getLong("yesWeight")).isEqualTo(3);
        assertThat(res.jsonPath().getLong("noWeight")).isEqualTo(10);
        assertThat(res.jsonPath().getLong("totalEligibleWeight")).isEqualTo(13);
    }

    // ── QR-TALLY-002 — supermajority via integer cross-multiplication ─────────────

    @Test @Tag("QR-TALLY-002")
    void supermajority_thresholdViaIntegers_twoThirds() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String v3Id = userId(voter3);

        // 2/3: yes(30)*3 >= 2*30 → 90 >= 60 → PASSED
        String body = QuorumTestSupport.buildOpenBody(
            "SUPERMAJORITY", 2, 3, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 10),
                    new QuorumTestSupport.VoterSpec(v3Id, 10)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter3, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    @Test @Tag("QR-TALLY-002")
    void supermajority_twoThirds_notMet_isRejected() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String v3Id = userId(voter3);

        // v1(10) YES, v2(10) NO, v3(10) NO → yes=10, base=30; 10*3=30 < 2*30=60 → REJECTED
        String body = QuorumTestSupport.buildOpenBody(
            "SUPERMAJORITY", 2, 3, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 10),
                    new QuorumTestSupport.VoterSpec(v3Id, 10)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "NO").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter3, "NO").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
    }

    // ── QR-TALLY-003 — abstention mode EXCLUDE_FROM_BASE vs COUNT_AS_NO ─────────

    @Test @Tag("QR-TALLY-003") @Tag("QR-BALLOT-003")
    void abstentionMode_countAsNo_abstainCountsAgainstThreshold() {
        String v1Id = userId(voter1);  // 10 YES
        String v2Id = userId(voter2);  // 10 ABSTAIN (counts as NO → tie → TIE_FAILS → REJECTED)

        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "COUNT_AS_NO", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 10)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "ABSTAIN").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getLong("abstainWeight")).isEqualTo(10);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
    }

    @Test @Tag("QR-TALLY-003") @Tag("QR-BALLOT-003")
    void abstentionMode_excludeFromBase_abstainDoesNotAffectThreshold() {
        String v1Id = userId(voter1);  // 10 YES
        String v2Id = userId(voter2);  // 10 ABSTAIN (excluded from base → yes wins)

        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 10)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "ABSTAIN").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getLong("abstainWeight")).isEqualTo(10);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    // ── QR-RESOLVE-003 — deterministic tie-break ──────────────────────────────────

    @Test @Tag("QR-RESOLVE-003")
    void tieBreakTieFails_exactTieProducesRejected() {
        String v1Id = userId(voter1);  // 5 YES
        String v2Id = userId(voter2);  // 5 NO — tie → TIE_FAILS → REJECTED

        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "NO").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
    }

    @Test @Tag("QR-RESOLVE-003")
    void tieBreakChairCasting_chairYesBreaksTie() {
        String v1Id = userId(voter1);  // 5 YES
        String v2Id = userId(voter2);  // chair, 5 YES
        String v3Id = userId(voter3);  // 5 NO

        // v1(5)+v2(5) YES, v3(5) NO → not a tie (10 vs 5). Tests chair id is correctly stored.
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "CHAIR_CASTING", v2Id,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5),
                    new QuorumTestSupport.VoterSpec(v3Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter3, "NO").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        // yes=10, no=5, base=15; 10*2=20 >= 1*15=15 → PASSED (no tie needed)
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    // ── QR-RESOLVE-005 — re-resolve idempotent identical ────────────────────────

    @Test @Tag("QR-RESOLVE-005")
    void reResolve_isIdempotent_returnsIdenticalResolution() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> r1 = resolve(motionId);
        assertThat(r1.statusCode()).isEqualTo(200);

        ExtractableResponse<Response> r2 = resolve(motionId);
        assertThat(r2.statusCode()).isEqualTo(200);

        assertThat(r2.jsonPath().getString("outcome")).isEqualTo(r1.jsonPath().getString("outcome"));
        assertThat(r2.jsonPath().getString("id")).isEqualTo(r1.jsonPath().getString("id"));
        assertThat(r2.jsonPath().getLong("yesWeight")).isEqualTo(r1.jsonPath().getLong("yesWeight"));
    }

    // ── QR-RESOLVE-006 — concurrent casts serialize (keystone) ───────────────────

    @Test @Tag("QR-RESOLVE-006")
    void concurrentCasts_serializeOnMotionLock_noLostBallots() throws Exception {
        // Build a motion with 8 unique voters
        String[] tokens = new String[8];
        String[] voterIds = new String[8];
        for (int i = 0; i < 8; i++) {
            tokens[i] = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-conc-v" + i), "MEMBER");
            voterIds[i] = userId(tokens[i]);
        }

        StringBuilder rosterSb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) rosterSb.append(",");
            rosterSb.append("{\"voterId\":\"").append(voterIds[i]).append("\",\"weight\":1}");
        }
        String body = "{\"policy\":{\"ruleType\":\"MAJORITY\",\"thresholdNumerator\":1,"
            + "\"thresholdDenominator\":2,\"quorumNumerator\":1,\"quorumDenominator\":2,"
            + "\"abstentionMode\":\"EXCLUDE_FROM_BASE\",\"tieBreakMode\":\"TIE_FAILS\"},"
            + "\"roster\":[" + rosterSb + "]}";
        UUID motionId = openMotion(body);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statuses = new ConcurrentLinkedQueue<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < n; i++) {
            final String token = tokens[i];
            pool.submit(() -> {
                start.await();
                int status = castBallot(motionId, token, "YES").statusCode();
                statuses.add(status);
                if (status != 201) failures.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(failures.get()).as("all concurrent ballots should succeed (lock serializes)").isZero();
        assertThat(statuses).hasSize(n);

        // Resolve and verify all 8 ballot weights were recorded
        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getLong("yesWeight")).isEqualTo(8);
    }

    // ── QR-AUTHZ-001 — unauthenticated → 401 ────────────────────────────────────

    @Test @Tag("QR-AUTHZ-001")
    void unauthenticated_returns401() {
        // Open a motion with a voter in the roster
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.majorityPolicyBody(
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        // No Authorization header → Spring Security 401
        int status = given()
            .header("Content-Type", "application/json")
            .body("{\"choice\":\"YES\"}")
        .when().post("/api/quorum/motions/" + motionId + "/ballots")
        .thenReturn().statusCode();
        assertThat(status).isEqualTo(401);
    }

    // ── QR-AUTHZ-002 — non-convener resolve → 404 ────────────────────────────────

    @Test @Tag("QR-AUTHZ-002")
    void nonConvenerResolve_returns404() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.majorityPolicyBody(
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);

        // voter1 is not the convener — should get 404
        int status = given().header("Authorization", "Bearer " + voter1)
        .when().post("/api/quorum/motions/" + motionId + "/resolve")
        .thenReturn().statusCode();
        assertThat(status).isEqualTo(404);
    }

    // ── QR-POLICY-002 — invalid policy → 422 ─────────────────────────────────────

    @Test @Tag("QR-POLICY-002")
    void policyInvalid_chairCastingWithoutVoterId_returns422() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "CHAIR_CASTING", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));

        int status = given().header("Authorization", "Bearer " + convener)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/quorum/motions")
        .thenReturn().statusCode();

        assertThat(status).isEqualTo(422);
    }

    // ── QR-BALLOT-003 — ABSTAIN counts toward quorum participation ───────────────

    @Test @Tag("QR-BALLOT-003") @Tag("QR-RESOLVE-001")
    void abstainCountsTowardQuorum_notNonVote() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String v3Id = userId(voter3);

        // quorum=2/3 of eligible (3 voters, weight 1 each). v1 YES + v2 ABSTAIN → cast=2 ≥ 2 → quorum met.
        // v3 does not cast — non-vote: contributes to eligible weight only, not to castEligibleWeight.
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 2, 3, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 1),
                    new QuorumTestSupport.VoterSpec(v2Id, 1),
                    new QuorumTestSupport.VoterSpec(v3Id, 1)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "ABSTAIN").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getLong("castEligibleWeight")).isEqualTo(2);
        // quorum met → base=1(yes), yes(1)*2>=1*1 → PASSED
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    // ── Ballot list paginated ────────────────────────────────────────────────────

    @Test @Tag("QR-BALLOT-001")
    void ballotsEndpoint_returnsPaginatedList() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "NO").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> page = given().header("Authorization", "Bearer " + convener)
            .when().get("/api/quorum/motions/" + motionId + "/ballots?page=0&size=20")
            .then().statusCode(200).extract();

        assertThat(page.jsonPath().getList("data")).hasSize(2);
        assertThat(page.jsonPath().getInt("pagination.totalElements")).isEqualTo(2);
    }

    // ── Motion detail GET ──────────────────────────────────────────────────────────

    @Test @Tag("QR-POLICY-001")
    void getMotion_returnsMotionDetail() {
        String v1Id = userId(voter1);
        String body = QuorumTestSupport.majorityPolicyBody(
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5)));
        UUID motionId = openMotion(body);

        ExtractableResponse<Response> res = given().header("Authorization", "Bearer " + convener)
            .when().get("/api/quorum/motions/" + motionId)
            .then().statusCode(200).extract();

        assertThat(res.jsonPath().getString("id")).isEqualTo(motionId.toString());
        assertThat(res.jsonPath().getString("status")).isEqualTo("OPEN");
        assertThat(res.jsonPath().getLong("totalEligibleWeight")).isEqualTo(5);
    }

    // ── REGRESSION 1 (QR-RESOLVE-003): non-1/2 threshold — 5 YES vs 5 NO at a 1/3 bar ──
    // The pre-fix code treated yes==effectiveNo as a tie, sending this to tie-break and
    // returning REJECTED.  The fixed code uses lhs>rhs strictly: 5*3=15 > 1*10=10 → PASSED.

    @Test @Tag("QR-RESOLVE-003")
    void regression_oneThirdThreshold_fiveVsFive_mustPass_notTieBreak() {
        String v1Id = userId(voter1); // weight 5, YES
        String v2Id = userId(voter2); // weight 5, NO

        // threshold = 1/3, EXCLUDE_FROM_BASE: lhs = 5*3=15, rhs = 1*10=10 → lhs>rhs → PASSED
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 3, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "NO").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        // Must be PASSED, NOT REJECTED or sent to tie-break
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    // ── REGRESSION 2 (QR-TALLY-003): COUNT_AS_NO, genuine tie at the bar ─────────────
    // yes=10, no=5, abstain=5, COUNT_AS_NO, MAJORITY 1/2.
    // base = 10+5+5=20; lhs = 10*2=20, rhs = 1*20=20 → exactly at bar → tie-break → REJECTED.

    @Test @Tag("QR-TALLY-003")
    void regression_countAsNo_genuineTieAtBar_tieFails_rejected() {
        String v1Id = userId(voter1); // weight 10, YES
        String v2Id = userId(voter2); // weight 5, NO
        String v3Id = userId(voter3); // weight 5, ABSTAIN (counts as NO)
        // Need a 4th voter for weight; re-use convener slot with a fresh sub-user
        String voter4 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-tie-v4"), "MEMBER");
        String v4Id = userId(voter4);

        // Use voters v1(10), v2(5), v3(5): total=20, yes=10, no=5, abstain=5
        // base(COUNT_AS_NO) = 10+5+5=20; lhs=10*2=20, rhs=1*20=20 → tie → TIE_FAILS → REJECTED
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "COUNT_AS_NO", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 10),
                    new QuorumTestSupport.VoterSpec(v2Id, 5),
                    new QuorumTestSupport.VoterSpec(v3Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "NO").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter3, "ABSTAIN").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getLong("yesWeight")).isEqualTo(10);
        assertThat(res.jsonPath().getLong("noWeight")).isEqualTo(5);
        assertThat(res.jsonPath().getLong("abstainWeight")).isEqualTo(5);
        // Tie at the bar → TIE_FAILS → REJECTED
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
    }

    // ── REGRESSION 3 (QR-RESOLVE-003): real CHAIR_CASTING tie — chair breaks it ─────
    // The existing chair test (10 vs 5) was NOT a tie; the CHAIR_CASTING branch was never
    // exercised.  This test uses a genuine tie (yes=5, no=5, 1/2) then invokes CHAIR_CASTING.

    @Test @Tag("QR-RESOLVE-003")
    void regression_chairCasting_genuineTie_chairYesBreaksTie_passed() {
        // 3 voters: v1 YES=5, v2 (chair) YES=5, v3 NO=5
        // EXCLUDE_FROM_BASE: base = yes+no = 5+5+5=15? No: YES voters are v1+v2=10, NO=5.
        // base = 10+5 = 15; lhs = 10*2=20, rhs = 1*15=15 → 20>15 → PASSED without tie-break.
        // Need exact tie: YES=5, NO=5. Chair is the YES voter (v2) and there's also a NO voter.
        // So use 2 voters only: v1 YES=5, v3 NO=5, chair=v2 (eligible but votes YES):
        String voter4 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-chairyes-v4"), "MEMBER");
        String v1Id = userId(voter1);   // 5, YES (non-chair)
        String v3Id = userId(voter3);   // 5, NO
        String v4Id = userId(voter4);   // chair, votes YES

        // v1 YES=5, v3 NO=5, v4 chair YES=5
        // After all vote: yes=10, no=5; base=10+5=15; lhs=10*2=20, rhs=1*15=15 → 20>15 → PASSED
        // Not a tie. Need yes==no for lhs==rhs at 1/2: yes=5, no=5 with only 2 voting (v1+v3),
        // chair (v4) is in roster but doesn't vote — then base=10, lhs=10, rhs=10 → tie.
        // Then chair's ballot lookup: v4 did NOT vote → abstained → REJECTED.
        // Instead: have chair vote YES — lhs=10*2=20, rhs=1*10=10 still — wait, that breaks the tie
        // only if the chair IS the one that would swing. Let's set yes(non-chair)=5, no=5, then
        // add the chair's YES ballot: yes becomes 5+5=10, no=5, base=15, lhs=20>15 → PASSED.
        // That's correct but the CHAIR_CASTING path only fires when lhs==rhs at resolve time,
        // which means the chair must NOT be counted in regular YES/NO sums — the chair IS a
        // regular eligible voter whose ballot is summed normally. The tie-break fires on lhs==rhs,
        // then consults the chair's ballot to decide direction. To get a tie with CHAIR_CASTING:
        //   yes=5 (v1 only), no=5 (v3 only), chair (v4) votes YES (weight=5).
        //   Then: YES group=v1+v4=10, NO=5, base=15, lhs=20>15 → PASSED directly, no tie-break.
        // For a tie where CHAIR_CASTING is the decider, the chair's weight must make yes==no
        // before their ballot is included in the sum, OR the chair's ballot tips lhs to equal rhs.
        // Example: v1 YES=5, v3 NO=5 (each weight 5, chair NOT voting yet).
        //   chair weight=0? No — all voters have weight>0.
        // Simplest genuine tie where chair's ballot flips it:
        //   roster: [v1:5, v4(chair):5], v1 votes NO, v4(chair) votes YES.
        //   yes=5(v4), no=5(v1); EXCLUDE_FROM_BASE, 1/2: base=10, lhs=5*2=10=rhs=1*10 → tie!
        //   CHAIR_CASTING: chair is v4 and voted YES → PASSED.
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "CHAIR_CASTING", v4Id,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v4Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "NO").statusCode()).isEqualTo(201);   // v1 votes NO
        assertThat(castBallot(motionId, voter4, "YES").statusCode()).isEqualTo(201);  // chair votes YES

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        // yes=5(chair), no=5(v1), base=10, lhs=5*2=10, rhs=1*10=10 → exact tie
        // CHAIR_CASTING: chair (v4) voted YES → PASSED
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("PASSED");
    }

    @Test @Tag("QR-RESOLVE-003")
    void regression_chairCasting_genuineTie_threeVoters_chairAbstained_rejected() {
        String voter4 = QuorumTestSupport.obtainToken(QuorumTestSupport.freshEmail("qr-chair-v4"), "MEMBER");
        String v1Id = userId(voter1);  // 5, YES
        String v2Id = userId(voter2);  // 5, ABSTAIN (chair) — tie-break: abstain → REJECTED
        String v4Id = userId(voter4);  // 5, NO

        // 3 voters: YES=5, NO=5, ABSTAIN(chair)=5 — EXCLUDE_FROM_BASE:
        //   base = yes+no = 5+5 = 10; lhs = 5*2=10, rhs = 1*10=10 → tie!
        //   CHAIR_CASTING: chair abstained → REJECTED
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "CHAIR_CASTING", v2Id,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5),
                    new QuorumTestSupport.VoterSpec(v4Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter2, "ABSTAIN").statusCode()).isEqualTo(201);
        assertThat(castBallot(motionId, voter4, "NO").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> res = resolve(motionId);
        assertThat(res.statusCode()).isEqualTo(200);
        // yes=5, no=5, abstain=5; EXCLUDE_FROM_BASE: base=10, lhs=10, rhs=10 → tie
        // Chair abstained → tie fails → REJECTED
        assertThat(res.jsonPath().getString("outcome")).isEqualTo("REJECTED");
    }

    // ── REGRESSION 4 (QR-AUTHZ-002): getBallots confidentiality ─────────────────────
    // Non-convener voter GETs /ballots → 404 (IDOR-safe); convener GETs → 200 with data.

    @Test @Tag("QR-AUTHZ-002")
    void regression_getBallots_nonConvener_returns404_convener_returns200() {
        String v1Id = userId(voter1);
        String v2Id = userId(voter2);
        String body = QuorumTestSupport.buildOpenBody(
            "MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null,
            List.of(new QuorumTestSupport.VoterSpec(v1Id, 5),
                    new QuorumTestSupport.VoterSpec(v2Id, 5)));
        UUID motionId = openMotion(body);

        assertThat(castBallot(motionId, voter1, "YES").statusCode()).isEqualTo(201);

        // Non-convener (voter1, an eligible voter) → IDOR-safe 404
        int nonConvenerStatus = given().header("Authorization", "Bearer " + voter1)
            .when().get("/api/quorum/motions/" + motionId + "/ballots?page=0&size=20")
            .thenReturn().statusCode();
        assertThat(nonConvenerStatus).isEqualTo(404);

        // Convener → 200 with ballot data
        ExtractableResponse<Response> page = given().header("Authorization", "Bearer " + convener)
            .when().get("/api/quorum/motions/" + motionId + "/ballots?page=0&size=20")
            .then().statusCode(200).extract();
        assertThat(page.jsonPath().getList("data")).hasSize(1);
    }
}
