package com.ax.template.authblueprint.correctionrefire;

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

/**
 * correction-refire-l0 compliance — verified against the live correctionrefire reference
 * workload. The invariant: a correction is an append-only new version (the prior never mutated);
 * a correction over a version whose ack was CLOSED re-opens the loop with a brand-new pending
 * ack; an identical re-publish is a no-op (no ack spam); a multi-correction chain keeps each
 * version's ack state independent, and "current" is always derived (never a stored pointer).
 * Spec: specs/correction-refire-l0.yaml (Joint Commission closed-loop comms + CWE-372).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("CORRECTIONREFIRE")
class CorrectionRefireComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        CorrectionRefireTestSupport.useRandomPort(port);
        member = CorrectionRefireTestSupport.obtainToken(
            CorrectionRefireTestSupport.freshEmail("crf-member"), "MEMBER");
    }

    private ExtractableResponse<Response> publish(String subjectRef, String content) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"content\":\"" + content + "\"}")
        .when().post("/api/correction-refire/subjects/" + subjectRef + "/publish").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> ack(String subjectRef, int version) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/correction-refire/subjects/" + subjectRef + "/versions/" + version + "/ack")
            .thenReturn().then().extract();
    }

    private String ackStatus(String subjectRef, int version) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/correction-refire/subjects/" + subjectRef + "/versions/" + version + "/ack")
            .then().statusCode(200).extract().jsonPath().getString("status");
    }

    // ── CRF-SUPERSEDE-001 — a correction is append-only; the prior version is unchanged ──
    @Test @Tag("CRF-SUPERSEDE-001")
    void correction_isAppendOnly_priorVersionUnchanged() {
        String subject = "SUB-" + UUID.randomUUID();
        ExtractableResponse<Response> v1 = publish(subject, "original content");
        assertThat(v1.statusCode()).isEqualTo(201);
        assertThat(v1.jsonPath().getInt("version")).isEqualTo(1);
        assertThat((Object) v1.jsonPath().get("correctsVersion")).isNull();

        ExtractableResponse<Response> v2 = publish(subject, "corrected content");
        assertThat(v2.jsonPath().getInt("version")).isEqualTo(2);
        assertThat(v2.jsonPath().getInt("correctsVersion")).isEqualTo(1);

        // v1 is retained VERBATIM
        ExtractableResponse<Response> v1Reread = given().header("Authorization", "Bearer " + member)
            .when().get("/api/correction-refire/subjects/" + subject + "/versions/1")
            .then().statusCode(200).extract();
        assertThat(v1Reread.jsonPath().getString("content")).isEqualTo("original content");
    }

    // ── CRF-REFIRE-002 — a correction over a CLOSED ack re-opens the loop with a NEW pending ack ──
    @Test @Tag("CRF-REFIRE-002")
    void correctionOverClosedAck_reopensLoop_priorAckUntouched() {
        String subject = "SUB-" + UUID.randomUUID();
        publish(subject, "v1 content");
        ExtractableResponse<Response> ackV1 = ack(subject, 1);
        assertThat(ackV1.statusCode()).isEqualTo(200);
        assertThat(ackV1.jsonPath().getString("status")).isEqualTo("CLOSED");

        publish(subject, "v2 content");   // a correction over a version whose ack is CLOSED

        // v2 has its OWN brand-new PENDING ack
        assertThat(ackStatus(subject, 2)).isEqualTo("PENDING");
        // v1's CLOSED ack is untouched
        assertThat(ackStatus(subject, 1)).isEqualTo("CLOSED");
    }

    // ── CRF-IDEMPOTENT-003 — re-publishing IDENTICAL content is a no-op; no ack spam ──
    @Test @Tag("CRF-IDEMPOTENT-003")
    void identicalRepublish_isNoOp_noAckSpam() {
        String subject = "SUB-" + UUID.randomUUID();
        publish(subject, "same content");
        ack(subject, 1);

        ExtractableResponse<Response> repeat = publish(subject, "same content");
        // no new version was created — still version 1
        assertThat(repeat.jsonPath().getInt("version")).isEqualTo(1);

        // v1's ack remains CLOSED, unchanged — no new pending ack was spawned
        assertThat(ackStatus(subject, 1)).isEqualTo("CLOSED");
        // there is still no version 2
        ExtractableResponse<Response> noV2 = given().header("Authorization", "Bearer " + member)
            .when().get("/api/correction-refire/subjects/" + subject + "/versions/2").thenReturn().then().extract();
        assertThat(noV2.statusCode()).isEqualTo(404);
    }

    // ── CRF-CHAIN-004 — multi-correction chain keeps each version's ack independent; current is derived ──
    @Test @Tag("CRF-CHAIN-004")
    void multiCorrectionChain_independentAckState_currentIsDerived() {
        String subject = "SUB-" + UUID.randomUUID();
        publish(subject, "v1");
        ack(subject, 1);
        publish(subject, "v2");
        ack(subject, 2);
        publish(subject, "v3");   // v3 re-opens; v1 and v2 stay CLOSED and untouched

        assertThat(ackStatus(subject, 3)).isEqualTo("PENDING");
        assertThat(ackStatus(subject, 2)).isEqualTo("CLOSED");
        assertThat(ackStatus(subject, 1)).isEqualTo("CLOSED");

        // current resolves to v3 — derived from the chain (MAX version), not a stored field
        ExtractableResponse<Response> current = given().header("Authorization", "Bearer " + member)
            .when().get("/api/correction-refire/subjects/" + subject + "/current")
            .then().statusCode(200).extract();
        assertThat(current.jsonPath().getInt("version")).isEqualTo(3);

        var versions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/correction-refire/subjects/" + subject + "/versions")
            .then().statusCode(200).extract().jsonPath().getList("version");
        assertThat(versions).containsExactly(1, 2, 3);
    }
}
