package com.ax.template.authblueprint.tieredauthority;

import io.restassured.http.ContentType;
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
 * amount-tiered-authority-l0 (전결 규정) compliance — RestAssured black-box against the live
 * tieredauthority reference workload. Spec: specs/amount-tiered-authority-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TIEREDAUTHORITY")
class TieredAuthorityComplianceTest {

    @LocalServerPort int port;
    String admin;
    String member;

    @BeforeEach
    void setup() {
        admin = TieredAuthorityTestSupport.obtainToken(TieredAuthorityTestSupport.freshEmail("ata-admin"), "ADMIN");
        member = TieredAuthorityTestSupport.obtainToken(TieredAuthorityTestSupport.freshEmail("ata-member"), "MEMBER");
    }

    private ExtractableResponse<Response> configure(String bandsJson) {
        return given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("{\"bands\":" + bandsJson + "}")
        .when().post("/api/tiered-authority/config").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> decide(BigDecimalLike amount, int level, String outcome) {
        return given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"amount\":" + amount.raw + ",\"deciderLevel\":" + level + ",\"outcome\":\"" + outcome + "\"}")
        .when().post("/api/tiered-authority/decisions").thenReturn().then().extract();
    }

    // tiny helper so call sites read as decide(amt(500000), 2, "x") instead of a raw string literal
    private record BigDecimalLike(String raw) {}
    private static BigDecimalLike amt(long v) { return new BigDecimalLike(String.valueOf(v)); }

    // ── ATA-TIER-001 — insufficient authority is 403, fail-closed (never auto-escalated) ──
    @Test
    @Tag("ATA-TIER-001")
    void decide_rejectsInsufficientAuthority_neverAutoEscalates() {
        assertThat(configure("[{\"minAmount\":0,\"maxAmount\":1000000,\"minDeciderLevel\":2},"
            + "{\"minAmount\":1000000,\"minDeciderLevel\":3}]").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> denied = decide(amt(500000), 1, "approve");
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.jsonPath().getString("code")).isEqualTo("INSUFFICIENT_AUTHORITY");

        ExtractableResponse<Response> ok = decide(amt(500000), 2, "approve");
        assertThat(ok.statusCode()).isEqualTo(201);
        assertThat(ok.jsonPath().getInt("bandMinDeciderLevel")).isEqualTo(2);
        assertThat(ok.jsonPath().getInt("deciderLevel")).isEqualTo(2);

        // the higher band still gates level 2 out
        ExtractableResponse<Response> deniedHigh = decide(amt(2000000), 2, "approve");
        assertThat(deniedHigh.statusCode()).isEqualTo(403);
    }

    // ── ATA-BOUNDARY-001 — tiling required at config time; half-open [lo,hi) at decide time ──
    @Test
    @Tag("ATA-BOUNDARY-001")
    void config_rejectsGapAndOverlap_boundaryBelongsToExactlyOneBand() {
        ExtractableResponse<Response> overlap = configure(
            "[{\"minAmount\":0,\"maxAmount\":500,\"minDeciderLevel\":1},"
            + "{\"minAmount\":400,\"maxAmount\":1000,\"minDeciderLevel\":2}]");
        assertThat(overlap.statusCode()).isEqualTo(422);
        assertThat(overlap.jsonPath().getString("code")).isEqualTo("TIER_BOUNDARY_INVALID");

        ExtractableResponse<Response> gap = configure(
            "[{\"minAmount\":0,\"maxAmount\":400,\"minDeciderLevel\":1},"
            + "{\"minAmount\":600,\"maxAmount\":1000,\"minDeciderLevel\":2}]");
        assertThat(gap.statusCode()).isEqualTo(422);

        // contiguous tiling, second band open-ended → 201
        assertThat(configure("[{\"minAmount\":0,\"maxAmount\":500,\"minDeciderLevel\":1},"
            + "{\"minAmount\":500,\"minDeciderLevel\":2}]").statusCode()).isEqualTo(201);

        // amount == boundary (500) resolves to the SECOND band (half-open: lo IN, hi NOT) —
        // proven by requiring level 2 (band2's minimum), not level 1 (band1's minimum).
        ExtractableResponse<Response> atBoundary = decide(amt(500), 1, "x");
        assertThat(atBoundary.statusCode()).isEqualTo(403); // band2 requires level 2, decider is level 1

        ExtractableResponse<Response> atBoundaryOk = decide(amt(500), 2, "x");
        assertThat(atBoundaryOk.statusCode()).isEqualTo(201);
        assertThat(new java.math.BigDecimal(atBoundaryOk.jsonPath().getString("bandMinAmount")))
            .isEqualByComparingTo("500");
    }

    // ── ATA-SNAPSHOT-001 — a later reconfiguration never rewrites a past decision's record ──
    @Test
    @Tag("ATA-SNAPSHOT-001")
    void decisionRecord_isImmutableSnapshot_survivesLaterReconfiguration() {
        assertThat(configure("[{\"minAmount\":0,\"minDeciderLevel\":2}]").statusCode()).isEqualTo(201);
        int versionAtDecision = given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-authority/config").then().statusCode(200).extract().path("tableVersion");

        ExtractableResponse<Response> decision = decide(amt(777), 2, "approved-v1");
        assertThat(decision.statusCode()).isEqualTo(201);
        String decisionId = decision.jsonPath().getString("id");
        assertThat(decision.jsonPath().getInt("tableVersion")).isEqualTo(versionAtDecision);
        assertThat(decision.jsonPath().getInt("bandMinDeciderLevel")).isEqualTo(2);

        // reconfigure: a NEW table version that would now reject the same decider (level 2 < 5)
        assertThat(configure("[{\"minAmount\":0,\"minDeciderLevel\":5}]").statusCode()).isEqualTo(201);

        // the ORIGINAL decision record is untouched — still reports the version it was decided against
        ExtractableResponse<Response> replay = given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-authority/decisions/" + decisionId).then().statusCode(200).extract();
        assertThat(replay.jsonPath().getInt("tableVersion")).isEqualTo(versionAtDecision);
        assertThat(replay.jsonPath().getInt("bandMinDeciderLevel")).isEqualTo(2);
        assertThat(replay.jsonPath().getInt("deciderLevel")).isEqualTo(2);
        assertThat(replay.jsonPath().getString("outcome")).isEqualTo("approved-v1");

        // deciding fresh against the NEW table with the same level now fails
        ExtractableResponse<Response> nowDenied = decide(amt(777), 2, "approve-v2");
        assertThat(nowDenied.statusCode()).isEqualTo(403);
    }

    @Test
    @Tag("ATA-SNAPSHOT-001")
    void decision_unknownId_is404() {
        ExtractableResponse<Response> notFound = given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-authority/decisions/" + java.util.UUID.randomUUID())
            .thenReturn().then().extract();
        assertThat(notFound.statusCode()).isEqualTo(404);
        assertThat(notFound.jsonPath().getString("code")).isEqualTo("DECISION_NOT_FOUND");
    }
}
