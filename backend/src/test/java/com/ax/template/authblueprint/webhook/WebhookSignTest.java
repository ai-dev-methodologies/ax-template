package com.ax.template.authblueprint.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * SIGN family — WEBHOOK-SIGN-001, WEBHOOK-SIGN-002.
 */
@ExtendWith(MockitoExtension.class)
class WebhookSignTest {

    @Mock WebhookDeliveryRepository deliveryRepository;
    @Mock WebhookEndpointRepository endpointRepository;
    @Mock WebhookHttpClient httpClient;
    @Mock RetryPolicy retryPolicy;
    @Mock CircuitBreakerPolicy circuitBreaker;

    HmacSigner hmacSigner;
    WebhookSender sender;

    @BeforeEach
    void setUp() {
        hmacSigner = new HmacSigner();
        sender = new WebhookSender(deliveryRepository, endpointRepository, httpClient,
            hmacSigner, retryPolicy, circuitBreaker);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-SIGN-001")
    @DisplayName("WEBHOOK-SIGN-001 — outbound request carries X-Webhook-Signature: sha256=hex(HMAC-SHA256(secret, timestamp.body))")
    void sign001_outboundCarriesHmacSha256Signature() {
        String secret = "test-secret-32-bytes-hex-encoded-here";
        String body = "{\"event\":\"order.created\",\"orderId\":42}";

        WebhookEndpoint endpoint = WebhookEndpoint.create(
            "https://hook.example.com/sign", secret, null);
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpoint.getId(), "order.created", body);

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.post(anyString(), anyString(), any()))
            .thenReturn(new WebhookHttpClient.Response(200, null));

        sender.attempt(delivery.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(httpClient).post(
            org.mockito.ArgumentMatchers.eq("https://hook.example.com/sign"),
            org.mockito.ArgumentMatchers.eq(body),
            headersCaptor.capture()
        );
        Map<String, String> headers = headersCaptor.getValue();

        assertThat(headers).containsKey(HmacSigner.HEADER_SIGNATURE);
        String sig = headers.get(HmacSigner.HEADER_SIGNATURE);
        assertThat(sig)
            .as("WEBHOOK-SIGN-001 — header format sha256=<lowercase-hex>")
            .startsWith("sha256=");

        long ts = Long.parseLong(headers.get(HmacSigner.HEADER_TIMESTAMP));
        String expected = "sha256=" + HexFormat.of().formatHex(
            hmacSha256(secret, ts + "." + body));
        assertThat(sig)
            .as("WEBHOOK-SIGN-001 — signature MUST equal HMAC-SHA256(secret, timestamp + '.' + body)")
            .isEqualTo(expected);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-SIGN-002")
    @DisplayName("WEBHOOK-SIGN-002 — both X-Webhook-Timestamp and X-Webhook-Signature present; signed input is timestamp + '.' + body")
    void sign002_timestampHeaderAndSignedInputFormat() {
        String secret = "abc123";
        String body = "{\"a\":1}";

        WebhookEndpoint endpoint = WebhookEndpoint.create(
            "https://hook.example.com/sign2", secret, null);
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpoint.getId(), "evt", body);

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.post(anyString(), anyString(), any()))
            .thenReturn(new WebhookHttpClient.Response(200, null));

        long beforeMs = System.currentTimeMillis() / 1000;
        sender.attempt(delivery.getId());
        long afterMs = System.currentTimeMillis() / 1000;

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(httpClient).post(
            org.mockito.ArgumentMatchers.eq("https://hook.example.com/sign2"),
            org.mockito.ArgumentMatchers.eq(body),
            headersCaptor.capture());

        Map<String, String> headers = headersCaptor.getValue();
        assertThat(headers).containsKey(HmacSigner.HEADER_TIMESTAMP);
        long ts = Long.parseLong(headers.get(HmacSigner.HEADER_TIMESTAMP));
        assertThat(ts)
            .as("WEBHOOK-SIGN-002 — timestamp captured at send time (unix seconds)")
            .isBetween(beforeMs, afterMs);

        // Verify helper inverse: hmacSigner.verify accepts the same construction
        boolean verified = hmacSigner.verify(secret, ts, body, headers.get(HmacSigner.HEADER_SIGNATURE));
        assertThat(verified)
            .as("WEBHOOK-SIGN-002 — receiver verify helper accepts sender output")
            .isTrue();

        // And rejects a tampered body
        boolean tamperedVerified = hmacSigner.verify(secret, ts, "{\"a\":2}",
            headers.get(HmacSigner.HEADER_SIGNATURE));
        assertThat(tamperedVerified)
            .as("WEBHOOK-SIGN-002 — receiver verify helper rejects tampered body")
            .isFalse();
    }

    private static byte[] hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
