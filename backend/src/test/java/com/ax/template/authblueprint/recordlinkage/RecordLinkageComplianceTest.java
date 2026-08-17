package com.ax.template.authblueprint.recordlinkage;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * record-linkage-l0 compliance — verified against the live recordlinkage reference workload.
 * The invariant: verdicts are banded with score/breakdown/thresholds RECORDED; the REVIEW band
 * decides only by an explicit human confirm/reject; a merge records per-field survivorship and
 * tombstones the loser with a forward pointer; resolution follows merge chains; concurrent
 * confirms decide once and capture a record once.
 * Spec: specs/record-linkage-l0.yaml (Fellegi-Sunter banding + PMC merge-audit anchor).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("RECORDLINKAGE")
class RecordLinkageComplianceTest {

    @LocalServerPort int port;
    @Autowired LinkageService service;
    String member;

    @BeforeEach
    void setup() {
        member = LinkageTestSupport.obtainToken(LinkageTestSupport.freshEmail("rl-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createRecord(String name, String birthDate, String identifier) {
        String body = "{\"fullName\":\"" + name + "\""
            + (birthDate == null ? "" : ",\"birthDate\":\"" + birthDate + "\"")
            + (identifier == null ? "" : ",\"identifier\":\"" + identifier + "\"") + "}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/linkage/records").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> propose(String aId, String bId) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"recordAId\":\"" + aId + "\",\"recordBId\":\"" + bId + "\"}")
        .when().post("/api/linkage/proposals").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> confirm(String proposalId) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/linkage/proposals/" + proposalId + "/confirm").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getRecord(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/linkage/records/" + id).then().statusCode(200).extract();
    }

    // ── LINK-BAND-001 — score + breakdown + thresholds recorded; verdict banded ──
    @Test @Tag("LINK-BAND-001")
    void propose_recordsScoreBreakdownThresholds_andBands() {
        // identical on all three fields → 1.0 → AUTO_MATCH
        String a = createRecord("Hong Gildong", "1990-01-01", "ID-1");
        String b = createRecord("hong  gildong", "1990-01-01", "id-1");        // normalization agrees
        ExtractableResponse<Response> auto = propose(a, b);
        assertThat(auto.statusCode()).isEqualTo(201);
        assertThat(auto.jsonPath().getString("band")).isEqualTo("AUTO_MATCH");
        assertThat(auto.jsonPath().getDouble("score")).isEqualTo(1.0);
        assertThat(auto.jsonPath().getString("breakdownJson"))
            .contains("\"fullName\":0.5").contains("\"birthDate\":0.3").contains("\"identifier\":0.2");
        assertThat(auto.jsonPath().getDouble("lowerThreshold")).isEqualTo(0.5);
        assertThat(auto.jsonPath().getDouble("upperThreshold")).isEqualTo(0.8);

        // name agreement only → 0.5 = lower → REVIEW; full disagreement → NO_MATCH
        String c = createRecord("Kim Chulsoo", "1985-05-05", "ID-C");
        String d = createRecord("Kim Chulsoo", null, "ID-D");
        assertThat(propose(c, d).jsonPath().getString("band")).isEqualTo("REVIEW");

        String e = createRecord("Lee Younghee", "1970-07-07", "ID-E");
        String f = createRecord("Park Minsoo", "1999-09-09", "ID-F");
        assertThat(propose(e, f).jsonPath().getString("band")).isEqualTo("NO_MATCH");

        // self-pair 422
        assertThat(propose(c, c).statusCode()).isEqualTo(422);
    }

    // ── LINK-BAND-001 — proposing a pair whose participant is already MERGED → 409 ──
    @Test @Tag("LINK-BAND-001")
    void propose_withMergedParticipant_is409() {
        String a = createRecord("Baek Hyunwoo", "1987-07-17", "MP-1");
        String b = createRecord("baek hyunwoo", "1987-07-17", "mp-1");
        ExtractableResponse<Response> merged = propose(a, b);
        assertThat(merged.jsonPath().getString("band")).isEqualTo("AUTO_MATCH");
        String loser = merged.jsonPath().getString("highRecordId");

        String fresh = createRecord("Baek Hyunwoo", "1987-07-17", "MP-2");
        ExtractableResponse<Response> bad = propose(loser, fresh);
        assertThat(bad.statusCode()).isEqualTo(409);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("LINKAGE_PARTICIPANT_MERGED");
    }

    // ── LINK-REVIEW-001 — reject closes the proposal; the records stay ACTIVE and untouched ──
    @Test @Tag("LINK-REVIEW-001")
    void reject_closesProposal_recordsUntouched() {
        String a = createRecord("Oh Sehun", "1994-04-12", "RJ-A");
        String b = createRecord("Oh Sehun", null, "RJ-B");
        String pid = propose(a, b).jsonPath().getString("id");        // REVIEW band

        ExtractableResponse<Response> rejected = given().header("Authorization", "Bearer " + member)
            .when().post("/api/linkage/proposals/" + pid + "/reject").then().statusCode(200).extract();
        assertThat(rejected.jsonPath().getString("status")).isEqualTo("REJECTED");
        assertThat(rejected.jsonPath().getString("decidedBy")).isNotBlank();
        assertThat(rejected.jsonPath().getString("decidedAt")).isNotBlank();

        for (String id : new String[]{a, b}) {                        // no merge happened
            ExtractableResponse<Response> r = getRecord(id);
            assertThat(r.jsonPath().getString("status")).isEqualTo("ACTIVE");
            assertThat(r.jsonPath().getString("mergedIntoId")).isNull();
        }
        assertThat(confirm(pid).statusCode()).isEqualTo(409);          // decided once — for good
    }

    // ── LINK-REVIEW-001 — REVIEW decides only by explicit confirm/reject; NO_MATCH unconfirmable ──
    @Test @Tag("LINK-REVIEW-001")
    void reviewBand_decidesOnlyByHuman_noMatchUnconfirmable_doubleDecide409() {
        String a = createRecord("Cho Insung", "1981-03-03", "ID-A1");
        String b = createRecord("Cho Insung", null, "ID-B1");
        ExtractableResponse<Response> review = propose(a, b);
        String pid = review.jsonPath().getString("id");
        assertThat(review.jsonPath().getString("band")).isEqualTo("REVIEW");
        assertThat(review.jsonPath().getString("status")).isEqualTo("PROPOSED");
        assertThat(getRecord(a).jsonPath().getString("status")).isEqualTo("ACTIVE"); // no auto-merge

        ExtractableResponse<Response> ok = confirm(pid);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getString("status")).isEqualTo("CONFIRMED");
        assertThat(ok.jsonPath().getString("decidedBy")).isNotBlank();
        assertThat(ok.jsonPath().getString("decidedAt")).isNotBlank();

        assertThat(confirm(pid).statusCode()).isEqualTo(409);                 // decides once

        // NO_MATCH cannot be confirmed
        String e = createRecord("Gil Sunja", "1960-06-06", "ID-E2");
        String f = createRecord("Na Mansoo", "1955-05-05", "ID-F2");
        String nm = propose(e, f).jsonPath().getString("id");
        ExtractableResponse<Response> bad = confirm(nm);
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("LINKAGE_NOT_CONFIRMABLE");
    }

    // ── LINK-SURVIVOR-001 — per-field survivorship recorded; loser tombstoned, values retained ──
    @Test @Tag("LINK-SURVIVOR-001")
    void merge_recordsPerFieldSurvivorship_andTombstonesLoserWithValues() {
        String a = createRecord("Seo Jiwoo", "1992-02-02", null);             // survivor (low id? either)
        String b = createRecord("seo jiwoo", "1992-02-02", "ID-FILL");        // loser supplies identifier
        ExtractableResponse<Response> p = propose(a, b);
        assertThat(p.jsonPath().getString("band")).isEqualTo("AUTO_MATCH");   // 0.5+0.3 = 0.8 ≥ upper

        String pid = p.jsonPath().getString("id");
        java.util.List<java.util.Map<String, Object>> decisions =
            given().header("Authorization", "Bearer " + member)
                .when().get("/api/linkage/proposals/" + pid + "/decisions")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(decisions).hasSize(3);                                     // one per identity field
        assertThat(decisions).allSatisfy(d ->
            assertThat(d.get("ruleApplied")).isEqualTo("PREFER_SURVIVOR_NON_BLANK"));

        String lowId = p.jsonPath().getString("lowRecordId");
        String highId = p.jsonPath().getString("highRecordId");
        ExtractableResponse<Response> survivor = getRecord(lowId);
        ExtractableResponse<Response> loser = getRecord(highId);
        assertThat(survivor.jsonPath().getString("status")).isEqualTo("ACTIVE");
        assertThat(survivor.jsonPath().getString("identifier")).isEqualTo("ID-FILL"); // gap filled
        assertThat(loser.jsonPath().getString("status")).isEqualTo("MERGED");
        assertThat(loser.jsonPath().getString("mergedIntoId")).isEqualTo(lowId);
        assertThat(loser.jsonPath().getString("fullName")).isNotBlank();      // values retained
    }

    // ── LINK-RESOLVE-001 — resolve follows chained merges to the living survivor ──
    @Test @Tag("LINK-RESOLVE-001")
    void resolve_followsChainedMerges() {
        String a = createRecord("Ha Eunseo", "1995-05-15", "CHAIN-1");
        String b = createRecord("ha eunseo", "1995-05-15", "chain-1");
        String firstLoser = propose(a, b).jsonPath().getString("highRecordId");
        String survivor1 = getRecord(firstLoser).jsonPath().getString("mergedIntoId");

        // now merge the surviving record itself into a third — building the chain A→B→C
        String c = createRecord("HA  EUNSEO", "1995-05-15", "CHAIN-1");
        ExtractableResponse<Response> p2 = propose(survivor1, c);
        assertThat(p2.jsonPath().getString("band")).isEqualTo("AUTO_MATCH");
        String finalSurvivor = p2.jsonPath().getString("lowRecordId");  // LOW record survives

        // resolving the FIRST loser walks the chain to THE final living survivor
        ExtractableResponse<Response> resolved = given().header("Authorization", "Bearer " + member)
            .when().get("/api/linkage/records/" + firstLoser + "/resolve").then().statusCode(200).extract();
        assertThat(resolved.jsonPath().getString("status")).isEqualTo("ACTIVE");
        assertThat(resolved.jsonPath().getString("id")).isEqualTo(finalSurvivor);
        // direct GET still shows the tombstone
        assertThat(getRecord(firstLoser).jsonPath().getString("status")).isEqualTo("MERGED");
    }

    // ── LINK-CONCURRENT-001 — keystone: N concurrent confirms → exactly one wins ──
    @Test @Tag("LINK-CONCURRENT-001")
    void concurrentConfirms_exactlyOneWins() throws Exception {
        String a = createRecord("Yoon Daeho", "1988-08-08", "RACE-A");
        String b = createRecord("Yoon Daeho", null, "RACE-B");                // REVIEW band
        UUID pid = UUID.fromString(propose(a, b).jsonPath().getString("id"));

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.confirm(pid, "racer");
                    codes.add(200);
                } catch (LinkageException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("LINK-CONCURRENT-001 — exactly one confirm wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // and the records merged exactly once
        long merged = java.util.stream.Stream.of(a, b)
            .filter(id -> "MERGED".equals(getRecord(id).jsonPath().getString("status"))).count();
        assertThat(merged).isEqualTo(1);
    }

    // ── LINK-CONCURRENT-001 — two proposals SHARING a participant: at most one captures it ──
    @Test @Tag("LINK-CONCURRENT-001")
    void concurrentConfirms_sharedParticipant_atMostOneCapture() throws Exception {
        // three same-name records (every pair is REVIEW); sort by UUID order so the SHARED
        // record is the HIGHEST id — i.e. it would be the LOSER of both merges
        java.util.List<UUID> ids = new java.util.ArrayList<>();
        for (String ident : new String[]{"SHARE-1", "SHARE-2", "SHARE-3"}) {
            ids.add(UUID.fromString(createRecord("Moon Jaein", null, ident)));
        }
        ids.sort(UUID::compareTo);
        UUID x = ids.get(0), y = ids.get(1), z = ids.get(2);          // z = shared loser-to-be

        UUID p1 = UUID.fromString(propose(x.toString(), z.toString()).jsonPath().getString("id"));
        UUID p2 = UUID.fromString(propose(y.toString(), z.toString()).jsonPath().getString("id"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (UUID pid : new UUID[]{p1, p2}) {
            pool.submit(() -> {
                start.await();
                try {
                    service.confirm(pid, "racer");
                    codes.add(200);
                } catch (LinkageException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // exactly one merge captured z; the other confirm found its participant MERGED → 409
        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("LINK-CONCURRENT-001 — at most one merge captures the shared record").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(1);

        ExtractableResponse<Response> shared = getRecord(z.toString());
        assertThat(shared.jsonPath().getString("status")).isEqualTo("MERGED");
        assertThat(shared.jsonPath().getString("mergedIntoId")).isIn(x.toString(), y.toString());
        long activeWinners = java.util.stream.Stream.of(x, y)
            .filter(id -> "ACTIVE".equals(getRecord(id.toString()).jsonPath().getString("status"))).count();
        assertThat(activeWinners).isEqualTo(2);                       // neither winner was tombstoned
    }
}
