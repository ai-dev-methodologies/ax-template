package com.ax.template.authblueprint.exceptiongate;

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
 * orthogonal-exception-gate-l0 compliance — verified against the live exceptiongate reference
 * workload. The invariant: the exception dimension and the subject's primary lifecycle are
 * genuinely independent axes; gated operations fail closed while raised; lifting restores
 * everything and the audit trail is append-only.
 * Spec: specs/orthogonal-exception-gate-l0.yaml (generalized from dsr.DsrRestrictionGate).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("EXCEPTIONGATE")
class ExceptionGateComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = ExceptionGateTestSupport.obtainToken(ExceptionGateTestSupport.freshEmail("eg-member"), "MEMBER");
    }

    private ExtractableResponse<Response> post(String path, String body) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body == null ? "{}" : body)
        .when().post("/api/exception-gate/" + path).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String path) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/exception-gate/" + path).then().statusCode(200).extract();
    }

    // ── EXC-DIM-INDEPENDENT-001 — neither axis mutates the other, either direction ──
    @Test @Tag("EXC-DIM-INDEPENDENT-001")
    void raiseAndPrimaryAdvance_areGenuinelyIndependentAxes() {
        String subjectId = "acct-" + UUID.randomUUID();
        String path = "litigation-hold/" + subjectId;

        post(path + "/primary-state", "{\"newState\":\"UNDER_REVIEW\"}");
        post(path + "/raise", "{\"reason\":\"discovery hold\"}");
        // raising must NOT touch the primary lifecycle
        assertThat(get(path).jsonPath().getString("primaryState")).isEqualTo("UNDER_REVIEW");

        post(path + "/primary-state", "{\"newState\":\"CLOSED\"}");
        // advancing the primary lifecycle must NOT touch the exception dimension
        ExtractableResponse<Response> after = get(path);
        assertThat(after.jsonPath().getBoolean("raised")).isTrue();
        assertThat(after.jsonPath().getString("reason")).isEqualTo("discovery hold");
        assertThat(after.jsonPath().getString("primaryState")).isEqualTo("CLOSED");
    }

    // ── EXC-DIM-BLOCK-001 — gated ops fail closed while raised; non-gated ops unaffected ──
    @Test @Tag("EXC-DIM-BLOCK-001")
    void gatedOperations_failClosed_nonGatedUnaffected() {
        String subjectId = "acct-" + UUID.randomUUID();
        String path = "kyc-freeze/" + subjectId;

        // before raising, a gated op is allowed
        assertThat(post(path + "/probe", "{\"operation\":\"write\"}").statusCode()).isEqualTo(200);

        post(path + "/raise", "{\"reason\":\"kyc review\"}");

        ExtractableResponse<Response> gated = post(path + "/probe", "{\"operation\":\"write\"}");
        assertThat(gated.statusCode()).isEqualTo(423);
        assertThat(gated.jsonPath().getString("code")).isEqualTo("EXCEPTION_GATE_BLOCKED");

        ExtractableResponse<Response> exportProbe = post(path + "/probe", "{\"operation\":\"export\"}");
        assertThat(exportProbe.statusCode()).isEqualTo(423);

        // a NON-gated operation is completely unaffected
        ExtractableResponse<Response> nonGated = post(path + "/probe", "{\"operation\":\"read\"}");
        assertThat(nonGated.statusCode()).isEqualTo(200);
        assertThat(nonGated.jsonPath().getBoolean("allowed")).isTrue();
    }

    // ── EXC-DIM-LIFT-001 — lift restores everything; audited append-only; both idempotent ──
    @Test @Tag("EXC-DIM-LIFT-001")
    void lift_restoresFullOperationSet_auditedAppendOnly_bothIdempotent() {
        String subjectId = "acct-" + UUID.randomUUID();
        String path = "kyc-freeze/" + subjectId;

        post(path + "/raise", "{\"reason\":\"first\"}");
        post(path + "/raise", "{\"reason\":\"re-raise\"}");                 // idempotent — no error
        assertThat(get(path).jsonPath().getBoolean("raised")).isTrue();

        assertThat(get(path + "/audit").jsonPath().getList("$")).hasSize(2);
        assertThat(get(path + "/audit").jsonPath().getList("action")).containsExactly("RAISE", "RAISE");

        ExtractableResponse<Response> liftResp = post(path + "/lift", "{\"reason\":\"cleared\"}");
        assertThat(liftResp.statusCode()).isEqualTo(200);
        assertThat(liftResp.jsonPath().getBoolean("raised")).isFalse();
        assertThat(post(path + "/probe", "{\"operation\":\"write\"}").statusCode()).isEqualTo(200);

        // lifting again is idempotent — no error
        assertThat(post(path + "/lift", "{\"reason\":\"cleared again\"}").statusCode()).isEqualTo(200);

        assertThat(get(path + "/audit").jsonPath().getList("action"))
            .containsExactly("RAISE", "RAISE", "LIFT", "LIFT");
    }
}
