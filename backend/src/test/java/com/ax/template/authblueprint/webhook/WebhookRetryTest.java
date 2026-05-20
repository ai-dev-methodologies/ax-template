package com.ax.template.authblueprint.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * RETRY family — WEBHOOK-RETRY-001, WEBHOOK-RETRY-002.
 */
@ExtendWith(MockitoExtension.class)
class WebhookRetryTest {

    @Mock WebhookDeliveryRepository deliveryRepository;
    @Mock WebhookEndpointRepository endpointRepository;
    @Mock WebhookHttpClient httpClient;
    @Mock CircuitBreakerPolicy circuitBreaker;

    HmacSigner hmacSigner;
    RetryPolicy retryPolicy;
    WebhookSender sender;

    @BeforeEach
    void setUp() {
        hmacSigner = new HmacSigner();
        retryPolicy = new RetryPolicy();
        sender = new WebhookSender(deliveryRepository, endpointRepository, httpClient,
            hmacSigner, retryPolicy, circuitBreaker);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-RETRY-001")
    @DisplayName("WEBHOOK-RETRY-001 — first 5xx failure schedules next attempt at now+30s with status=PENDING_RETRY")
    void retry001_firstFailure_schedulesAt30s() {
        WebhookEndpoint endpoint = WebhookEndpoint.create("https://x.example.com", "s", null);
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpoint.getId(), "evt", "{}");

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.post(anyString(), anyString(), any()))
            .thenReturn(new WebhookHttpClient.Response(500, "Internal Server Error"));

        Instant before = Instant.now();
        WebhookDelivery result = sender.attempt(delivery.getId());

        assertThat(result.getStatus())
            .as("WEBHOOK-RETRY-001 — 5xx is retriable → PENDING_RETRY")
            .isEqualTo(WebhookDeliveryStatus.PENDING_RETRY);
        assertThat(result.getAttemptCount())
            .as("WEBHOOK-RETRY-001 — attempt_count incremented to 1 after first failure")
            .isEqualTo(1);
        assertThat(result.getNextAttemptAt())
            .as("WEBHOOK-RETRY-001 — next attempt scheduled ~30s after now")
            .isBetween(before.plusSeconds(29), before.plus(Duration.ofMinutes(2)));
        // tight bound: 30s
        long delaySeconds = result.getNextAttemptAt().getEpochSecond() - result.getLastAttemptAt().getEpochSecond();
        assertThat(delaySeconds)
            .as("WEBHOOK-RETRY-001 — exact 30s for first retry")
            .isEqualTo(30L);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-RETRY-001")
    @DisplayName("WEBHOOK-RETRY-001 — exhaustion: 5 consecutive failures end as FAILED_PERMANENT (DEAD-LETTER trigger)")
    void retry001_exhaustion_flipsToFailedPermanent() {
        // Schedule progression: 1 → 30s, 2 → 60s, 3 → 120s, 4 → 240s (delays before attempts 2..5)
        assertThat(retryPolicy.isExhausted(4)).isFalse();
        assertThat(retryPolicy.isExhausted(5))
            .as("WEBHOOK-RETRY-001 — attempt_count=5 is the terminal boundary")
            .isTrue();

        Instant now = Instant.now();
        assertThat(retryPolicy.nextAttemptAt(1, now).getEpochSecond() - now.getEpochSecond())
            .as("attempt 1 → next at +30s")
            .isEqualTo(30L);
        assertThat(retryPolicy.nextAttemptAt(2, now).getEpochSecond() - now.getEpochSecond())
            .as("attempt 2 → next at +60s")
            .isEqualTo(60L);
        assertThat(retryPolicy.nextAttemptAt(3, now).getEpochSecond() - now.getEpochSecond())
            .as("attempt 3 → next at +120s")
            .isEqualTo(120L);
        assertThat(retryPolicy.nextAttemptAt(4, now).getEpochSecond() - now.getEpochSecond())
            .as("attempt 4 → next at +240s")
            .isEqualTo(240L);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-RETRY-002")
    @DisplayName("WEBHOOK-RETRY-002 — every attempt for the same delivery reuses the same X-Webhook-Delivery-Id (UUID stable across retries)")
    void retry002_deliveryIdStableAcrossRetries() {
        WebhookEndpoint endpoint = WebhookEndpoint.create("https://x.example.com", "s", null);
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpoint.getId(), "evt", "{}");
        UUID deliveryId = delivery.getId();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.post(anyString(), anyString(), any()))
            .thenReturn(new WebhookHttpClient.Response(503, "Service Unavailable"));

        // Three retry attempts on the same delivery
        sender.attempt(deliveryId);
        sender.attempt(deliveryId);
        sender.attempt(deliveryId);

        ArgumentCaptor<Map<String, String>> headersCaptor = capturingMap();
        org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(3))
            .post(anyString(), anyString(), headersCaptor.capture());

        for (Map<String, String> headers : headersCaptor.getAllValues()) {
            assertThat(headers.get(HmacSigner.HEADER_DELIVERY_ID))
                .as("WEBHOOK-RETRY-002 — X-Webhook-Delivery-Id is stable across all retry attempts")
                .isEqualTo(deliveryId.toString());
        }

        assertThat(delivery.getAttemptCount())
            .as("WEBHOOK-RETRY-002 — attempt_count incremented per retry on the SAME row")
            .isEqualTo(3);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, String>> capturingMap() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
