package com.ax.template.integration;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: WebhookReceiver HMAC-SHA256 verification.
 *
 * <p>RED phase: these tests FAIL until WebhookReceiver is wired into the
 * application context and a webhook endpoint is registered.
 *
 * <p>GREEN phase: passes after:
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
@SpringBootTest(classes = AuthBlueprintBackendApplication.class,
        properties = "ax.webhook.secret=test-secret-key-for-it")
@AutoConfigureMockMvc
class WebhookReceiverIT {

    private static final String TEST_SECRET = "test-secret-key-for-it";
    private static final String WEBHOOK_ENDPOINT = "/api/test/webhooks";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("POST without X-Hub-Signature-256 header returns 401")
    void webhook_missingSignatureHeader_returns401() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"push\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST with invalid HMAC signature returns 401")
    void webhook_invalidSignature_returns401() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=deadbeefdeadbeef")
                        .header("X-GitHub-Delivery", "test-delivery-001")
                        .content("{\"event\":\"push\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST with valid HMAC signature returns 200")
    void webhook_validSignature_returns200() throws Exception {
        byte[] body = "{\"event\":\"push\"}".getBytes();
        String signature = "sha256=" + computeHmac(body, TEST_SECRET);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Delivery", "test-delivery-002")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST with valid signature but duplicate delivery id returns 200 (idempotent)")
    void webhook_duplicateDeliveryId_returns200Idempotently() throws Exception {
        byte[] body = "{\"event\":\"push\"}".getBytes();
        String signature = "sha256=" + computeHmac(body, TEST_SECRET);
        String deliveryId = "test-delivery-003";

        // First delivery
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Delivery", deliveryId)
                        .content(body))
                .andExpect(status().isOk());

        // Second delivery — same id, should return 200 without reprocessing
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Delivery", deliveryId)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String computeHmac(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data));
    }
}
