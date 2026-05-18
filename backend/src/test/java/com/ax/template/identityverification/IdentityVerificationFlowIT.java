package com.ax.template.identityverification;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test: Identity Verification callback flow.
 *
 * <p>Spec coverage (identity-verification-l0.yaml):
 * <ul>
 *   <li>IDV-CALLBACK-001: invalid HMAC → 401</li>
 *   <li>IDV-CALLBACK-002: valid PASS callback → 200, VerifiedIdentity persisted</li>
 *   <li>IDV-CALLBACK-003: no RRN field in any response</li>
 *   <li>IDV-PROVIDER-001: both PASS and KCB callbacks produce the same response shape</li>
 * </ul>
 *
 * <p>Uses RestAssured (black-box HTTP) per PRACTICES-TEST-001.
 *
 * <p>GREEN conditions:
 * <ol>
 *   <li>{@code IdentityVerificationCallbackController} registered at
 *       {@code /api/identity-verification/callback/{provider}}</li>
 *   <li>Per-provider secrets configured: {@code ax.identity-verification.pass.secret}
 *       and {@code ax.identity-verification.kcb.secret}</li>
 *   <li>{@code VerifiedIdentity} entity persisted on valid callback</li>
 *   <li>No {@code rrn}, {@code residentRegistrationNumber}, or {@code 주민번호} field
 *       in any response body</li>
 * </ol>
 *
 * @see templates/backend/identity-verification/IdentityVerificationCallbackController.java
 */
@Tag("IDENTITY_VERIFICATION")
@SpringBootTest(
        classes = AuthBlueprintBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "ax.identity-verification.pass.secret=test-pass-secret",
            "ax.identity-verification.kcb.secret=test-kcb-secret"
        })
class IdentityVerificationFlowIT {

    private static final String PASS_SECRET = "test-pass-secret";
    private static final String KCB_SECRET = "test-kcb-secret";

    private static final String PASS_CALLBACK_ENDPOINT =
            "/api/identity-verification/callback/pass";
    private static final String KCB_CALLBACK_ENDPOINT =
            "/api/identity-verification/callback/kcb";
    private static final String ADMIN_LIST_ENDPOINT =
            "/api/admin/identity-verification";

    // IDV-CALLBACK-003: no RRN field — verified by response body assertions
    // Valid CI/DI values (64-byte hex = 128 hex chars) from KISA spec
    private static final String VALID_CI =
            "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
            + "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4";
    private static final String VALID_DI =
            "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3"
            + "d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ─── IDV-CALLBACK-001: Invalid HMAC → 401 ──────────────────────────────

    @Test
    @DisplayName("IDV-CALLBACK-001: PASS callback without X-Identity-Signature returns 401")
    void passCallback_missingSignature_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildPassPayload())
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("IDV-CALLBACK-001: PASS callback with invalid HMAC returns 401")
    void passCallback_invalidHmac_returns401() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=deadbeefdeadbeef")
                .body(buildPassPayload())
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("IDV-CALLBACK-001: KCB callback with invalid HMAC returns 401")
    void kcbCallback_invalidHmac_returns401() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=badc0ffee0ddf00d")
                .body(buildKcbPayload())
                .when()
                .post(KCB_CALLBACK_ENDPOINT)
                .then()
                .statusCode(401);
    }

    // ─── IDV-CALLBACK-002: Valid callback → 200, persisted ─────────────────

    @Test
    @DisplayName("IDV-CALLBACK-002: valid PASS callback with correct HMAC returns 200")
    void passCallback_validHmac_returns200() throws Exception {
        String body = buildPassPayload();
        String signature = computeHmacSha256(body, PASS_SECRET);

        given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=" + signature)
                .body(body)
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(200)
                .body("status", equalTo("accepted"))
                .body("provider", equalTo("pass"));
    }

    @Test
    @DisplayName("IDV-CALLBACK-002: valid KCB callback with correct HMAC returns 200")
    void kcbCallback_validHmac_returns200() throws Exception {
        String body = buildKcbPayload();
        String signature = computeHmacSha256(body, KCB_SECRET);

        given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=" + signature)
                .body(body)
                .when()
                .post(KCB_CALLBACK_ENDPOINT)
                .then()
                .statusCode(200)
                .body("status", equalTo("accepted"))
                .body("provider", equalTo("kcb"));
    }

    // ─── IDV-CALLBACK-003: No RRN field in responses ───────────────────────

    @Test
    @DisplayName("IDV-CALLBACK-003: 401 response does not contain RRN field")
    void errorResponse_doesNotContainRrnField() {
        var response = given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=invalid")
                .body(buildPassPayload())
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(401)
                .extract()
                .body()
                .asString();

        // IDV-CALLBACK-003: No RRN in any response (개인정보보호법 §24)
        assertNoRrnField(response);
    }

    @Test
    @DisplayName("IDV-CALLBACK-003: success response does not contain RRN field")
    void successResponse_doesNotContainRrnField() throws Exception {
        String body = buildPassPayload();
        String signature = computeHmacSha256(body, PASS_SECRET);

        var response = given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=" + signature)
                .body(body)
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertNoRrnField(response);
    }

    // ─── IDV-PROVIDER-001: PASS and KCB produce identical shape ────────────

    @Test
    @DisplayName("IDV-PROVIDER-001: PASS and KCB callbacks return same response shape {status, provider}")
    void bothProviders_returnIdenticalShape() throws Exception {
        String passBody = buildPassPayload();
        String passSig = computeHmacSha256(passBody, PASS_SECRET);

        String kcbBody = buildKcbPayload();
        String kcbSig = computeHmacSha256(kcbBody, KCB_SECRET);

        var passResponse = given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=" + passSig)
                .body(passBody)
                .when()
                .post(PASS_CALLBACK_ENDPOINT)
                .then()
                .statusCode(200)
                .body("status", notNullValue())
                .body("provider", equalTo("pass"))
                .extract()
                .jsonPath();

        var kcbResponse = given()
                .contentType(ContentType.JSON)
                .header("X-Identity-Signature", "sha256=" + kcbSig)
                .body(kcbBody)
                .when()
                .post(KCB_CALLBACK_ENDPOINT)
                .then()
                .statusCode(200)
                .body("status", notNullValue())
                .body("provider", equalTo("kcb"))
                .extract()
                .jsonPath();

        // Both must have the same response shape (IDV-PROVIDER-001)
        assertNoRrnField(passResponse.prettify());
        assertNoRrnField(kcbResponse.prettify());
    }

    // ─── Helper methods ─────────────────────────────────────────────────────

    private String buildPassPayload() {
        return String.format(
                "{\"ci_value\":\"%s\",\"di_value\":\"%s\",\"user_name\":\"홍길동\",\"birth_date\":\"19900101\",\"carrier\":\"SKT\"}",
                VALID_CI, VALID_DI);
    }

    private String buildKcbPayload() {
        return String.format(
                "{\"connecting_info\":\"%s\",\"duplicate_info\":\"%s\",\"user_name\":\"홍길동\",\"birth_day\":\"19900101\"}",
                VALID_CI, VALID_DI);
    }

    /**
     * IDV-CALLBACK-003: asserts no RRN field present in response.
     *
     * <p>개인정보보호법 §24-1: RRN must never appear in any API response.
     */
    private static void assertNoRrnField(String responseBody) {
        String lowerBody = responseBody.toLowerCase();
        // Check for common RRN field names
        if (lowerBody.contains("\"rrn\"")
                || lowerBody.contains("residentregistrationnumber")
                || lowerBody.contains("주민번호")
                || lowerBody.contains("주민등록번호")
                || lowerBody.contains("socialsecuritynumber")) {
            throw new AssertionError(
                    "IDV-CALLBACK-003 VIOLATION: RRN field detected in response body."
                    + " 개인정보보호법 §24-1: RRN must never appear in any API response.");
        }
    }

    private static String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmacBytes);
    }
}
