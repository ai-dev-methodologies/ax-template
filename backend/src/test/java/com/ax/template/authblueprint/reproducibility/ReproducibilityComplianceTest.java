package com.ax.template.authblueprint.reproducibility;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * reproducible-procedure-l0 compliance — verified against the live reproducibility reference
 * workload. The invariant: a DRAW records a server-generated seed + algorithm + canonical input
 * hash + selection and is REPLAYABLE byte-identically; a CLASSIFICATION pins its classifier version
 * (same input + version idempotent, a newer version a separate result); a sensitive subject is
 * role-blinded (MEMBER sees the mask, only ADMIN unmasks).
 * Spec: specs/reproducible-procedure-l0.yaml (NIST SP 800-90A DRBG + NIST SP 800-53 least-privilege + CWE-330).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("REPRODUCIBILITY")
class ReproducibilityComplianceTest {

    @LocalServerPort int port;
    @Autowired ReproducibilityService service;
    String member;
    String admin;

    @BeforeEach
    void setup() {
        member = ReproducibilityTestSupport.obtainToken(ReproducibilityTestSupport.freshEmail("repro-member"), "MEMBER");
        admin = ReproducibilityTestSupport.obtainToken(ReproducibilityTestSupport.freshEmail("repro-admin"), "ADMIN");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> draw(String token, String body) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/reproducibility/draws").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> classify(String token, String body) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/reproducibility/classifications").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getProc(String token, String id) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/reproducibility/procedures/" + id).then().extract();
    }

    private ExtractableResponse<Response> replay(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/reproducibility/procedures/" + id + "/replay").thenReturn().then().extract();
    }

    private static String drawBody(String ref, int k) {
        return "{\"inputSetRef\":\"" + ref + "\",\"k\":" + k + ",\"subject\":\"subject-9911\","
            + "\"candidates\":[\"c1\",\"c2\",\"c3\",\"c4\",\"c5\",\"c6\",\"c7\",\"c8\",\"c9\",\"c10\"]}";
    }

    // ── PROC-DRAW-001 — a draw records its seed + algorithm + canonical input hash + selection ──
    @Test @Tag("PROC-DRAW-001")
    void draw_recordsSeed_algorithm_inputHash_selection_serverSide() {
        ExtractableResponse<Response> r = draw(member, drawBody("DRAW-1", 3));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getString("kind")).isEqualTo("DRAW");
        assertThat(r.jsonPath().getString("seed")).as("the seed is recorded (server-generated)").isNotBlank();
        assertThat(r.jsonPath().getString("algorithm")).isEqualTo("FISHER_YATES_JAVA_RANDOM_V1");
        assertThat(r.jsonPath().getString("inputHash")).as("a 64-hex SHA-256 input hash").hasSize(64);
        assertThat(r.jsonPath().getList("selectedIds")).as("k=3 selected").hasSize(3);
        // the selection is a subset of the candidate set
        assertThat(List.of("c1","c2","c3","c4","c5","c6","c7","c8","c9","c10"))
            .containsAll(r.jsonPath().getList("selectedIds"));
    }

    // ── PROC-DRAW-001 — the seed is server-side even though the caller cannot supply one ──
    @Test @Tag("PROC-DRAW-001")
    void draw_seedIsServerGenerated_evenWhenCallerOmitsIt() {
        // the request body has NO seed field at all — the server still records one
        ExtractableResponse<Response> r = draw(member, drawBody("DRAW-NOSEED", 2));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getLong("seed")).as("a server-generated seed is recorded").isNotNull();
    }

    // ── PROC-REPLAY-001 — keystone: replay re-derives the byte-identical selection, mutates nothing ──
    @Test @Tag("PROC-REPLAY-001")
    void replay_reproducesByteIdenticalSelection_andMutatesNothing() {
        ExtractableResponse<Response> drawn = draw(member, drawBody("REPLAY-1", 4));
        assertThat(drawn.statusCode()).isEqualTo(201);
        String id = drawn.jsonPath().getString("id");
        List<String> recorded = drawn.jsonPath().getList("selectedIds");
        String recordedSeed = getProc(member, id).jsonPath().getString("seed");

        // replay N times → every replay reproduces the byte-identical selection
        for (int i = 0; i < 5; i++) {
            ExtractableResponse<Response> rep = replay(id);
            assertThat(rep.statusCode()).isEqualTo(200);
            assertThat(rep.jsonPath().getList("selectedIds"))
                .as("replay #" + i + " reproduces the recorded selection").isEqualTo(recorded);
            assertThat(rep.jsonPath().getBoolean("reproduced")).isTrue();
        }

        // the recorded procedure is unchanged by replay (same seed, same selection)
        ExtractableResponse<Response> after = getProc(member, id);
        assertThat(after.jsonPath().getString("seed")).isEqualTo(recordedSeed);
        assertThat(after.jsonPath().getList("selectedIds")).isEqualTo(recorded);
    }

    // ── PROC-REPLAY-001 — only a draw is replayable; a classification is 422 ──
    @Test @Tag("PROC-REPLAY-001")
    void replay_ofAClassification_is422() {
        ExtractableResponse<Response> cls = classify(member,
            "{\"inputSetRef\":\"C\",\"input\":\"payload-x\",\"classifierVersion\":\"v1\",\"resolvedClass\":\"A\"}");
        assertThat(cls.statusCode()).isEqualTo(201);
        ExtractableResponse<Response> bad = replay(cls.jsonPath().getString("id"));
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("PROC_NOT_REPLAYABLE");
    }

    // ── PROC-CLASS-001 — same input + same version is byte-identical; a new version is separate ──
    @Test @Tag("PROC-CLASS-001")
    void classify_sameInputSameVersion_isIdempotent_newVersionIsSeparate() {
        String body1 = "{\"inputSetRef\":\"CASE-7\",\"input\":\"the-canonical-input\",\"classifierVersion\":\"v1\",\"resolvedClass\":\"CLASS_A\"}";
        ExtractableResponse<Response> first = classify(member, body1);
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");
        assertThat(first.jsonPath().getString("resolvedClass")).isEqualTo("CLASS_A");
        assertThat(first.jsonPath().getString("classifierVersion")).isEqualTo("v1");

        // re-classify the SAME input under the SAME version → byte-identical (same id/class/version)
        ExtractableResponse<Response> again = classify(member,
            "{\"inputSetRef\":\"CASE-7\",\"input\":\"the-canonical-input\",\"classifierVersion\":\"v1\",\"resolvedClass\":\"CLASS_B\"}");
        assertThat(again.statusCode()).isEqualTo(201);
        assertThat(again.jsonPath().getString("id")).as("idempotent — same row").isEqualTo(firstId);
        assertThat(again.jsonPath().getString("resolvedClass")).as("history NOT re-labeled").isEqualTo("CLASS_A");
        assertThat(again.jsonPath().getString("classifierVersion")).isEqualTo("v1");

        // classify the same input under a NEWER version → a SEPARATE result; v1 row unchanged
        ExtractableResponse<Response> v2 = classify(member,
            "{\"inputSetRef\":\"CASE-7\",\"input\":\"the-canonical-input\",\"classifierVersion\":\"v2\",\"resolvedClass\":\"CLASS_C\"}");
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getString("id")).as("a newer version is a separate row").isNotEqualTo(firstId);
        assertThat(v2.jsonPath().getString("resolvedClass")).isEqualTo("CLASS_C");
        assertThat(getProc(member, firstId).jsonPath().getString("resolvedClass"))
            .as("the v1 result is unchanged").isEqualTo("CLASS_A");
    }

    // ── PROC-CLASS-001 — the same input hash is recorded; equal inputs collapse to one v1 row ──
    @Test @Tag("PROC-CLASS-001")
    void classify_recordsInputHash() {
        ExtractableResponse<Response> r = classify(member,
            "{\"inputSetRef\":\"H\",\"input\":\"hash-me\",\"classifierVersion\":\"v9\",\"resolvedClass\":\"Z\"}");
        assertThat(r.jsonPath().getString("inputHash")).as("a 64-hex SHA-256 input hash").hasSize(64);
    }

    // ── PROC-BLIND-001 — a MEMBER sees only the masked subject; only an ADMIN unmasks ──
    @Test @Tag("PROC-BLIND-001")
    void blinding_memberSeesMask_adminUnmasks_memberUnmaskForbidden() {
        ExtractableResponse<Response> drawn = draw(member, drawBody("BLIND-1", 2));
        String id = drawn.jsonPath().getString("id");

        // the MEMBER projection masks the subject and NEVER contains the raw value
        ExtractableResponse<Response> seen = getProc(member, id);
        assertThat(seen.statusCode()).isEqualTo(200);
        assertThat(seen.jsonPath().getString("maskedSubject")).as("masked, deterministic").isEqualTo("s***1");
        assertThat(seen.asString()).as("the raw subject NEVER appears in the MEMBER body")
            .doesNotContain("subject-9911");

        // a MEMBER cannot reach the unmask endpoint (least privilege) → 403
        int memberUnmask = given().header("Authorization", "Bearer " + member)
            .when().get("/api/reproducibility/procedures/" + id + "/unmask").thenReturn().statusCode();
        assertThat(memberUnmask).isEqualTo(403);

        // an ADMIN unmasks the raw subject
        ExtractableResponse<Response> unmasked = given().header("Authorization", "Bearer " + admin)
            .when().get("/api/reproducibility/procedures/" + id + "/unmask").then().statusCode(200).extract();
        assertThat(unmasked.jsonPath().getString("subject")).isEqualTo("subject-9911");
    }

    // ── error shape — a missing procedure is an RFC 9457 404 ──
    @Test @Tag("PROC-DRAW-001")
    void get_missingProcedure_is404_problemJson() {
        ExtractableResponse<Response> r = getProc(member, "00000000-0000-0000-0000-000000000000");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
