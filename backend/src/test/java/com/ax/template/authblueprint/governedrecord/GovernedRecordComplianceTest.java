package com.ax.template.authblueprint.governedrecord;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * attested-change-record-l0 compliance — verified against the live governedrecord reference workload.
 * The invariant: a governed edit atomically records who/when/old/new/reason, rejects a blank reason
 * (422 before the change), and appends an immutable per-field history that never obscures a prior
 * value. Spec: specs/attested-change-record-l0.yaml (FDA 21 CFR 11.10(e) / ALCOA).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("GOVERNEDRECORD")
class GovernedRecordComplianceTest {

    @LocalServerPort int port;
    @org.springframework.beans.factory.annotation.Autowired
    com.ax.template.authblueprint.common.MemberWriter memberWriter;
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.transaction.support.TransactionTemplate txTemplate;
    String member;

    @BeforeEach
    void setup() {
        GovernedRecordTestSupport.useRandomPort(port);
        member = GovernedRecordTestSupport.obtainToken(GovernedRecordTestSupport.freshEmail("gr-member"), "MEMBER");
    }

    private String createDatum(String name, String value) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"name\":\"" + name + "\",\"value\":\"" + value + "\"}")
        .when().post("/api/governed-data").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> change(String id, String newValue, String reasonJson) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"newValue\":\"" + newValue + "\"" + reasonJson + "}")
        .when().put("/api/governed-data/" + id).thenReturn().then().extract();
    }

    private String valueOf(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id).then().statusCode(200).extract().path("value");
    }

    // ── ACR-ENVELOPE-001 / ACR-PREIMAGE-001 — a reasoned edit records the full envelope ──
    @Test @Tag("ACR-ENVELOPE-001") @Tag("ACR-PREIMAGE-001")
    void edit_withReason_recordsWhoWhenOldNewReason() {
        String id = createDatum("lab-glucose-" + UUID.randomUUID(), "11.8");
        change(id, "12.4", ",\"reason\":\"transcription-error\"").statusCode();
        assertThat(valueOf(id)).isEqualTo("12.4");

        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id + "/history").then().statusCode(200).extract();
        assertThat(hist.jsonPath().getInt("data.size()")).isEqualTo(1);
        assertThat(hist.path("data[0].oldValue").toString()).isEqualTo("11.8");
        assertThat(hist.path("data[0].newValue").toString()).isEqualTo("12.4");
        assertThat(hist.path("data[0].reason").toString()).isEqualTo("transcription-error");
        assertThat(hist.jsonPath().getInt("data[0].sequenceNo")).isEqualTo(1);
        assertThat(hist.path("data[0].actor").toString()).isNotBlank();   // who (the authenticated principal)
        assertThat((Object) hist.path("data[0].occurredAt")).isNotNull();          // when
        assertThat(hist.jsonPath().getLong("pagination.totalElements")).isEqualTo(1L);
    }

    // ── ACR-ENVELOPE-001 — a blank/missing reason is rejected (422) before the value changes ──
    @Test @Tag("ACR-ENVELOPE-001")
    void edit_blankOrMissingReason_422_valueUnchanged() {
        String id = createDatum("lab-blank-" + UUID.randomUUID(), "5.0");
        // blank reason -> 422
        ExtractableResponse<Response> blank = change(id, "9.0", ",\"reason\":\"  \"");
        assertThat(blank.statusCode()).isEqualTo(422);
        assertThat(blank.path("code").toString()).isEqualTo("ATTESTED_REASON_REQUIRED");
        // missing reason field -> 422 (service contract, not a 400)
        assertThat(change(id, "9.0", "").statusCode()).isEqualTo(422);
        assertThat(valueOf(id)).as("value unchanged after rejected edits").isEqualTo("5.0");
    }

    // ── ACR-PREIMAGE-001 / ACR-APPEND-ONLY-001 — N edits form an unbroken old->new chain ──
    @Test @Tag("ACR-PREIMAGE-001") @Tag("ACR-APPEND-ONLY-001")
    void successiveEdits_formGapFreeImmutableChain() {
        String id = createDatum("lab-chain-" + UUID.randomUUID(), "A");
        change(id, "B", ",\"reason\":\"source-correction\"");
        change(id, "C", ",\"reason\":\"query-resolution\"");
        change(id, "D", ",\"reason\":\"unit-conversion\"");

        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id + "/history").then().statusCode(200).extract();
        assertThat(hist.jsonPath().getInt("data.size()")).isEqualTo(3);
        // sequence is monotonic and the old->new links chain from the original value to current
        assertThat(hist.path("data[0].oldValue").toString()).isEqualTo("A");
        assertThat(hist.path("data[0].newValue").toString()).isEqualTo("B");
        assertThat(hist.path("data[1].oldValue").toString()).isEqualTo("B");   // == prev newValue
        assertThat(hist.path("data[1].newValue").toString()).isEqualTo("C");
        assertThat(hist.path("data[2].oldValue").toString()).isEqualTo("C");
        assertThat(hist.path("data[2].newValue").toString()).isEqualTo("D");
        assertThat(hist.jsonPath().getList("data.sequenceNo")).containsExactly(1, 2, 3);
        assertThat(valueOf(id)).isEqualTo("D");
    }

    // ── ACR-APPEND-ONLY-001 — history is paginated; the full trail is retrievable and completeness
    //    is signalled, so a long-lived record is NEVER silently truncated (the inverse of the spec) ──
    @Test @Tag("ACR-APPEND-ONLY-001")
    void history_isPaginated_fullTrailRetrievable_neverSilentlyTruncated() {
        String id = createDatum("lab-page-" + UUID.randomUUID(), "0");
        for (int i = 1; i <= 5; i++) {
            change(id, String.valueOf(i), ",\"reason\":\"source-correction\"").statusCode();
        }
        // page 0, size 2 — partial slice MUST signal there is more (no silent cap)
        ExtractableResponse<Response> p0 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id + "/history?page=0&size=2").then().statusCode(200).extract();
        assertThat(p0.jsonPath().getInt("data.size()")).isEqualTo(2);
        assertThat(p0.jsonPath().getLong("pagination.totalElements")).isEqualTo(5L);
        assertThat(p0.jsonPath().getInt("pagination.totalPages")).isEqualTo(3);
        assertThat(p0.jsonPath().getBoolean("pagination.hasMore")).isTrue();

        // last page — completeness reachable, hasMore false, and the union covers EVERY sequence
        ExtractableResponse<Response> p2 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id + "/history?page=2&size=2").then().statusCode(200).extract();
        assertThat(p2.jsonPath().getInt("data.size()")).isEqualTo(1);
        assertThat(p2.jsonPath().getBoolean("pagination.hasMore")).isFalse();
        assertThat(p2.jsonPath().getList("data.sequenceNo")).containsExactly(5);
    }

    // ── ACR-APPEND-ONLY-001 — two concurrent edits to one datum never collide on a sequence:
    //    both are recorded, sequences are exactly {1,2}, and the old->new chain stays gap-free ──
    @Test @Tag("ACR-APPEND-ONLY-001")
    void concurrentEdits_neverCollideOnSequence_chainStaysGapFree() throws Exception {
        String id = createDatum("lab-race-" + UUID.randomUUID(), "start");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var go = new java.util.concurrent.CountDownLatch(1);
        try {
            for (String v : new String[]{"P", "Q"}) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    change(id, v, ",\"reason\":\"source-correction\"");
                    return null;
                });
            }
            ready.await();
            go.countDown();         // fire both simultaneously
            pool.shutdown();
            assertThat(pool.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/governed-data/" + id + "/history?size=10").then().statusCode(200).extract();
        // both edits recorded, no lost write, no duplicate sequence (the "never collide" property)
        assertThat(hist.jsonPath().getInt("data.size()")).isEqualTo(2);
        assertThat(hist.jsonPath().getList("data.sequenceNo")).containsExactly(1, 2);
        // gap-free even under the race: seq1.oldValue is the original, seq2.oldValue == seq1.newValue
        assertThat(hist.path("data[0].oldValue").toString()).isEqualTo("start");
        assertThat(hist.path("data[1].oldValue").toString()).isEqualTo(hist.path("data[0].newValue").toString());
    }

    // ── RBAC + IDOR + dup-name ──
    @Test
    void rbac_unauth401_unknown404_dupName409() {
        given().header("Content-Type", "application/json").body("{\"newValue\":\"x\",\"reason\":\"r\"}")
        .when().put("/api/governed-data/" + UUID.randomUUID()).then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
        .when().get("/api/governed-data/" + UUID.randomUUID())
        .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));

        String name = "dup-" + UUID.randomUUID();
        createDatum(name, "1");
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"name\":\"" + name + "\",\"value\":\"2\"}")
        .when().post("/api/governed-data").then().statusCode(409).body("code", equalTo("ATTESTED_DUPLICATE_NAME"));
    }


    // ── wave-4 regression pin — MemberWriter MUST be @Repository so a flush-time constraint
    //    violation translates to Spring's DataIntegrityViolationException (the deleted
    //    SimpleJpaRepository proxies provided this; GovernedRecordService's sequence-conflict
    //    catch -> 409 depends on it). A @Component MemberWriter throws the raw JPA exception
    //    and this test FAILS. ──
    @Test @Tag("ATTESTED-SEQ-001")
    void memberWriter_translatesConstraintViolation_toSpringDataIntegrityViolation() {
        String id = createDatum("seq-pin-" + UUID.randomUUID(), "v1");
        UUID datumId = UUID.fromString(id);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                txTemplate.executeWithoutResult(tx -> {
                    // duplicate (datum, field, sequence) — violates uq_governed_change_seq at flush
                    memberWriter.persistAndFlush(new ChangeRecord(UUID.randomUUID(), datumId, "value",
                        99L, "a", "b", "pin", null, "tester", java.time.Instant.now()));
                    memberWriter.persistAndFlush(new ChangeRecord(UUID.randomUUID(), datumId, "value",
                        99L, "b", "c", "pin", null, "tester", java.time.Instant.now()));
                }))
            .as("flush-time unique violation must surface as Spring's translated exception")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
