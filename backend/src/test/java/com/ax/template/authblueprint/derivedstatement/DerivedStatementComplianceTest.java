package com.ax.template.authblueprint.derivedstatement;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * derived-statement-l0 compliance — verified against the live derivedstatement reference
 * workload. The invariant: a statement's identity is a content hash of (subject, period,
 * basis), never a client idempotency header; retry is safe by construction; content is
 * immutable once generated. Spec: specs/derived-statement-l0.yaml (RFC 9110 §9.2.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DERIVEDSTATEMENT")
class DerivedStatementComplianceTest {

    @LocalServerPort int port;
    String token;

    @BeforeEach
    void setup() {
        DerivedStatementTestSupport.useRandomPort(port);
        token = DerivedStatementTestSupport.obtainToken(
            DerivedStatementTestSupport.freshEmail("stmt"), "MEMBER");
    }

    private ExtractableResponse<Response> generate(String period, String basisJson) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"period\":\"" + period + "\",\"basis\":" + basisJson + "}")
        .when().post("/api/statements").thenReturn().then().extract();
    }

    // ── STMT-DERIVE-001 — same (subject, period, basis) resolves to the SAME statement; changed basis appends v2 ──
    @Test @Tag("STMT-DERIVE-001")
    void generate_sameBasis_returnsSameStatement_changedBasis_appendsVersion() {
        String period = "2026-07";
        String basisAB = "[{\"label\":\"A\",\"amount\":10.00},{\"label\":\"B\",\"amount\":20.00}]";

        ExtractableResponse<Response> first = generate(period, basisAB);
        assertThat(first.statusCode()).isEqualTo(201);
        String idV1 = first.jsonPath().getString("id");
        assertThat(first.jsonPath().getInt("versionNo")).isEqualTo(1);

        // IDENTICAL basis (order-shuffled) → the SAME statement, no new row
        String basisBAShuffled = "[{\"label\":\"B\",\"amount\":20.00},{\"label\":\"A\",\"amount\":10.00}]";
        ExtractableResponse<Response> replay = generate(period, basisBAShuffled);
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.jsonPath().getString("id")).isEqualTo(idV1);
        assertThat(replay.jsonPath().getInt("versionNo")).isEqualTo(1);

        // CHANGED basis → a NEW version; v1 remains fetchable, unchanged
        String basisABC = "[{\"label\":\"A\",\"amount\":10.00},{\"label\":\"B\",\"amount\":20.00},"
            + "{\"label\":\"C\",\"amount\":30.00}]";
        ExtractableResponse<Response> v2 = generate(period, basisABC);
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getInt("versionNo")).isEqualTo(2);
        assertThat(v2.jsonPath().getString("id")).isNotEqualTo(idV1);

        ExtractableResponse<Response> fetchedV1 = given().header("Authorization", "Bearer " + token)
            .when().get("/api/statements/" + idV1).thenReturn().then().extract();
        assertThat(fetchedV1.statusCode()).isEqualTo(200);
        assertThat(fetchedV1.jsonPath().getInt("versionNo")).isEqualTo(1);
        assertThat(fetchedV1.jsonPath().getFloat("totalAmount")).isEqualTo(30.0f);
    }

    // ── STMT-RETRY-002 — a double-submit with NO idempotency header is safe by construction ──
    @Test @Tag("STMT-RETRY-002")
    void generate_doubleSubmitWithoutIdempotencyHeader_isSafe() {
        String period = "2026-08";
        String basis = "[{\"label\":\"X\",\"amount\":5.00}]";

        ExtractableResponse<Response> call1 = generate(period, basis);
        ExtractableResponse<Response> call2 = generate(period, basis);
        assertThat(call1.statusCode()).isEqualTo(201);
        assertThat(call2.statusCode()).isEqualTo(201);
        assertThat(call1.jsonPath().getString("id")).isEqualTo(call2.jsonPath().getString("id"));

        ExtractableResponse<Response> list = given().header("Authorization", "Bearer " + token)
            .when().get("/api/statements?period=" + period).thenReturn().then().extract();
        assertThat(list.jsonPath().getList("id")).hasSize(1);
    }

    // ── STMT-IMMUTABLE-003 — a fetched v1's content is byte-identical after a v2 was generated ──
    @Test @Tag("STMT-IMMUTABLE-003")
    void statement_contentImmutable_afterRegeneration() {
        String period = "2026-09";
        ExtractableResponse<Response> v1 = generate(period, "[{\"label\":\"A\",\"amount\":1.00}]");
        String v1Hash = v1.jsonPath().getString("basisHash");
        generate(period, "[{\"label\":\"A\",\"amount\":1.00},{\"label\":\"B\",\"amount\":2.00}]");

        ExtractableResponse<Response> refetched = given().header("Authorization", "Bearer " + token)
            .when().get("/api/statements/" + v1.jsonPath().getString("id")).thenReturn().then().extract();
        assertThat(refetched.jsonPath().getString("basisHash")).isEqualTo(v1Hash);
        assertThat(refetched.jsonPath().getInt("versionNo")).isEqualTo(1);
    }

    // ── AuthZ — every endpoint requires a JWT ──
    @Test @Tag("STMT-DERIVE-001")
    void generate_withoutToken_is401() {
        assertThat(given().header("Content-Type", "application/json")
            .body("{\"period\":\"2026-01\",\"basis\":[{\"label\":\"A\",\"amount\":1.00}]}")
            .when().post("/api/statements").thenReturn().statusCode()).isEqualTo(401);
    }
}
