package com.ax.template.integration;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static io.restassured.RestAssured.given;

/**
 * Integration test: WebhookReceiver HMAC-SHA256 verification.
 *
 * <p>Uses RestAssured (black-box HTTP) per PRACTICES-TEST-001.
 *
 * <p>GREEN: passes after:
 * <ol>
 *   <li>{@code WebhookReceiver} bean is registered in the application context.
 *   <li>A {@code POST /api/test/webhooks} endpoint verifies via {@code WebhookReceiver.verify()}.
 *   <li>{@code ax.webhook.secret} is configured in test application properties.
 * </ol>
 *
 * <p>Rule protected: {@code webhook-hmac-required} (PRACTICES-INTEG-001).
 *
 * @see com.ax.template.authblueprint.integration.WebhookReceiver
 */
@Tag("INTEGRATION")
@SpringBootTest(
        classes = AuthBlueprintBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ax.webhook.secret=test-secret-key-for-it")
class WebhookReceiverIT {

    private static final String TEST_SECRET = "test-secret-key-for-it";
    private static final String WEBHOOK_ENDPOINT = "/api/test/webhooks";

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("POST without X-Hub-Signature-256 header returns 401")
    void webhook_missingSignatureHeader_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"event\":\"push\"}")
                .when()
                .post(WEBHOOK_ENDPOINT)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST with invalid HMAC signature returns 401")
    void webhook_invalidSignature_returns401() {
        given()
                .contentType(ContentType.JSON)
                .header("X-Hub-Signature-256", "sha256=deadbeefdeadbeef")
                .header("X-GitHub-Delivery", "test-delivery-001")
                .body("{\"event\":\"push\"}")
                .when()
                .post(WEBHOOK_ENDPOINT)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST with valid HMAC signature returns 200")
    void webhook_validSignature_returns200() throws Exception {
        byte[] body = "{\"event\":\"push\"}".getBytes();
        String signature = "sha256=" + computeHmac(body, TEST_SECRET);

        given()
                .contentType(ContentType.JSON)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Delivery", "test-delivery-002")
                .body(body)
                .when()
                .post(WEBHOOK_ENDPOINT)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("POST with valid signature but duplicate delivery id returns 200 (idempotent)")
    void webhook_duplicateDeliveryId_returns200Idempotently() throws Exception {
        byte[] body = "{\"event\":\"push\"}".getBytes();
        String signature = "sha256=" + computeHmac(body, TEST_SECRET);
        String deliveryId = "test-delivery-003";

        // First delivery
        given()
                .contentType(ContentType.JSON)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Delivery", deliveryId)
                .body(body)
                .when()
                .post(WEBHOOK_ENDPOINT)
                .then()
                .statusCode(200);

        // Second delivery — same id, should return 200 without reprocessing
        given()
                .contentType(ContentType.JSON)
                .header("X-Hub-Signature-256", signature)
                .header("X-GitHub-Delivery", deliveryId)
                .body(body)
                .when()
                .post(WEBHOOK_ENDPOINT)
                .then()
                .statusCode(200);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String computeHmac(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data));
    }
}
