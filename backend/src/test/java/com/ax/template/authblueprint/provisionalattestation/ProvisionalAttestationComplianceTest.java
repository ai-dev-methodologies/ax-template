package com.ax.template.authblueprint.provisionalattestation;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * provisional-attestation-l0 compliance — verified against the live provisionalattestation
 * reference workload. The invariant: a 2-state PROVISIONAL -> ATTESTED lifecycle; the attestor
 * must differ from the author; PROVISIONAL content is author-editable but frozen once ATTESTED,
 * with a content-hash binding that makes an out-of-band tamper detectable; downstream queries
 * can filter attested-only vs include-provisional.
 * Spec: specs/provisional-attestation-l0.yaml (Joint Commission co-signature + CWE-841).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("PROVISIONALATTESTATION")
class ProvisionalAttestationComplianceTest {

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;
    String author;
    String attestor;

    @BeforeEach
    void setup() {
        ProvisionalAttestationTestSupport.useRandomPort(port);
        author = ProvisionalAttestationTestSupport.obtainToken(
            ProvisionalAttestationTestSupport.freshEmail("patt-author"), "MEMBER");
        attestor = ProvisionalAttestationTestSupport.obtainToken(
            ProvisionalAttestationTestSupport.freshEmail("patt-attestor"), "MEMBER");
    }

    private ExtractableResponse<Response> author(String token, String content) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"content\":\"" + content + "\"}")
        .when().post("/api/provisional-attestation/records").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> edit(String token, String id, String content) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"content\":\"" + content + "\"}")
        .when().put("/api/provisional-attestation/records/" + id).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> attest(String token, String id) {
        return given().header("Authorization", "Bearer " + token)
        .when().post("/api/provisional-attestation/records/" + id + "/attest").thenReturn().then().extract();
    }

    // ── PATT-LIFECYCLE-001 — 2-state lifecycle; re-attest is terminal-rejected ──
    @Test @Tag("PATT-LIFECYCLE-001")
    void created_provisional_thenAttested_thenTerminal() {
        String id = author(author, "draft note").jsonPath().getString("id");
        assertThat(given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records/" + id)
            .then().statusCode(200).extract().jsonPath().getString("status")).isEqualTo("PROVISIONAL");

        ExtractableResponse<Response> attested = attest(attestor, id);
        assertThat(attested.statusCode()).isEqualTo(200);
        assertThat(attested.jsonPath().getString("status")).isEqualTo("ATTESTED");
        assertThat(attested.jsonPath().getString("attestedBy")).isNotBlank();

        // ATTESTED is terminal — re-attesting is rejected 409
        ExtractableResponse<Response> reAttest = attest(attestor, id);
        assertThat(reAttest.statusCode()).isEqualTo(409);
        assertThat(reAttest.jsonPath().getString("code")).isEqualTo("PATT_ILLEGAL_TRANSITION");
    }

    // ── PATT-DISTINCT-002 — self-attestation is rejected fail-closed ──
    @Test @Tag("PATT-DISTINCT-002")
    void selfAttestation_isRejected_distinctAttestorSucceeds() {
        String id = author(author, "draft note").jsonPath().getString("id");

        ExtractableResponse<Response> selfAttest = attest(author, id);
        assertThat(selfAttest.statusCode()).isEqualTo(422);
        assertThat(selfAttest.jsonPath().getString("code")).isEqualTo("PATT_SELF_ATTESTATION");
        // status remains PROVISIONAL after the rejected self-attestation
        assertThat(given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records/" + id)
            .then().statusCode(200).extract().jsonPath().getString("status")).isEqualTo("PROVISIONAL");

        ExtractableResponse<Response> distinctAttest = attest(attestor, id);
        assertThat(distinctAttest.statusCode()).isEqualTo(200);
        assertThat(distinctAttest.jsonPath().getString("status")).isEqualTo("ATTESTED");
    }

    // ── PATT-FREEZE-003 — author edits PROVISIONAL; edit after ATTESTED is 409; tamper is detectable ──
    @Test @Tag("PATT-FREEZE-003")
    void authorEditsWhileProvisional_frozenAfterAttested_tamperDetectable() {
        String id = author(author, "v1 content").jsonPath().getString("id");

        // author may edit while PROVISIONAL
        ExtractableResponse<Response> edited = edit(author, id, "v2 content");
        assertThat(edited.statusCode()).isEqualTo(200);
        assertThat(edited.jsonPath().getString("content")).isEqualTo("v2 content");

        attest(attestor, id);

        // edit after ATTESTED -> 409, content unchanged
        ExtractableResponse<Response> editAfterAttest = edit(author, id, "v3 content");
        assertThat(editAfterAttest.statusCode()).isEqualTo(409);
        assertThat(given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records/" + id)
            .then().statusCode(200).extract().jsonPath().getString("content")).isEqualTo("v2 content");

        // verify on an untouched ATTESTED record -> no tamper
        assertThat(given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records/" + id + "/verify")
            .then().statusCode(200).extract().jsonPath().getBoolean("tamperDetected")).isFalse();

        // simulate an out-of-band write bypassing the frozen-content guard (raw SQL, not via the API)
        jdbcTemplate.update("UPDATE provisional_records SET content = ? WHERE id = ?", "TAMPERED", id);

        // verify now detects the drift
        assertThat(given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records/" + id + "/verify")
            .then().statusCode(200).extract().jsonPath().getBoolean("tamperDetected")).isTrue();
    }

    // ── PATT-DOWNSTREAM-004 — attested-only vs include-provisional filter; status always visible ──
    @Test @Tag("PATT-DOWNSTREAM-004")
    void listFiltersAttestedOnlyVsIncludeProvisional() {
        String provisionalId = author(author, "still provisional").jsonPath().getString("id");
        String attestedId = author(author, "will be attested").jsonPath().getString("id");
        attest(attestor, attestedId);

        // default (attested-only) never returns the provisional record
        var attestedOnly = given().header("Authorization", "Bearer " + author)
            .when().get("/api/provisional-attestation/records")
            .then().statusCode(200).extract().jsonPath();
        java.util.List<String> attestedOnlyIds = attestedOnly.getList("content.id", String.class);
        assertThat(attestedOnlyIds).contains(attestedId).doesNotContain(provisionalId);

        // include-provisional=true returns both, each carrying its own status
        var both = given().header("Authorization", "Bearer " + author)
            .queryParam("includeProvisional", "true")
            .when().get("/api/provisional-attestation/records")
            .then().statusCode(200).extract().jsonPath();
        java.util.List<String> bothIds = both.getList("content.id", String.class);
        assertThat(bothIds).contains(attestedId, provisionalId);
    }
}
