package com.ax.template.authblueprint.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * DEAD-LETTER family — WEBHOOK-DEAD-LETTER-001, WEBHOOK-DEAD-LETTER-002.
 */
@ExtendWith(MockitoExtension.class)
class WebhookDeadLetterTest {

    @Mock WebhookDeliveryRepository deliveryRepository;
    @Mock WebhookEndpointRepository endpointRepository;
    @Mock WebhookHttpClient httpClient;
    @Mock CircuitBreakerPolicy circuitBreaker;

    HmacSigner hmacSigner;
    RetryPolicy retryPolicy;
    WebhookSender sender;
    WebhookAdminService adminService;

    @BeforeEach
    void setUp() {
        hmacSigner = new HmacSigner();
        retryPolicy = new RetryPolicy();
        sender = new WebhookSender(deliveryRepository, endpointRepository, httpClient,
            hmacSigner, retryPolicy, circuitBreaker);
        adminService = new WebhookAdminService(deliveryRepository);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-DEAD-LETTER-001")
    @DisplayName("WEBHOOK-DEAD-LETTER-001 — 5 consecutive failures end as FAILED_PERMANENT; row NOT deleted")
    void deadLetter001_exhaustedRetries_endAsFailedPermanent() {
        WebhookEndpoint endpoint = WebhookEndpoint.create("https://dl.example.com", "s", null);
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpoint.getId(), "evt", "{}");

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.post(anyString(), anyString(), any()))
            .thenReturn(new WebhookHttpClient.Response(500, "boom"));

        // Five attempts → terminal on the 5th
        sender.attempt(delivery.getId()); // 1 → PENDING_RETRY
        sender.attempt(delivery.getId()); // 2 → PENDING_RETRY
        sender.attempt(delivery.getId()); // 3 → PENDING_RETRY
        sender.attempt(delivery.getId()); // 4 → PENDING_RETRY
        sender.attempt(delivery.getId()); // 5 → FAILED_PERMANENT

        assertThat(delivery.getStatus())
            .as("WEBHOOK-DEAD-LETTER-001 — after 5 failures status MUST be FAILED_PERMANENT")
            .isEqualTo(WebhookDeliveryStatus.FAILED_PERMANENT);
        assertThat(delivery.getAttemptCount())
            .as("WEBHOOK-DEAD-LETTER-001 — attempt_count=5 reflects all attempts")
            .isEqualTo(5);
        assertThat(delivery.getLastResponseCode())
            .as("WEBHOOK-DEAD-LETTER-001 — last_response_code preserved for inspection")
            .isEqualTo(500);
        assertThat(delivery.getLastAttemptAt())
            .as("WEBHOOK-DEAD-LETTER-001 — last_attempt_at recorded")
            .isNotNull();
        // Row preserved — never deleted
        org.mockito.Mockito.verify(deliveryRepository, org.mockito.Mockito.never())
            .deleteById(any(UUID.class));
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-DEAD-LETTER-002")
    @DisplayName("WEBHOOK-DEAD-LETTER-002 — admin replay creates NEW delivery row with fresh delivery_id; original preserved")
    void deadLetter002_adminReplay_createsFreshDeliveryIdChain() {
        UUID endpointId = UUID.randomUUID();
        WebhookDelivery original = WebhookDelivery.enqueue(endpointId, "order.created", "{\"id\":99}");
        // Simulate the original being in FAILED_PERMANENT
        original.markFailedPermanent(500, "exhausted", java.time.Instant.now());
        // Attempts are bumped 5x already
        for (int i = 1; i < 5; i++) original.markFailedPermanent(500, "exhausted", java.time.Instant.now());
        UUID originalId = original.getId();

        when(deliveryRepository.findById(originalId)).thenReturn(Optional.of(original));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookDelivery replay = adminService.replay(originalId);

        assertThat(replay.getId())
            .as("WEBHOOK-DEAD-LETTER-002 — fresh delivery_id, distinct from the original")
            .isNotEqualTo(originalId);
        assertThat(replay.getEndpointId())
            .as("WEBHOOK-DEAD-LETTER-002 — replay targets the SAME endpoint")
            .isEqualTo(endpointId);
        assertThat(replay.getEventType()).isEqualTo("order.created");
        assertThat(replay.getBody()).isEqualTo("{\"id\":99}");
        assertThat(replay.getStatus())
            .as("WEBHOOK-DEAD-LETTER-002 — fresh row starts as PENDING (attempt_count=0)")
            .isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(replay.getAttemptCount())
            .as("WEBHOOK-DEAD-LETTER-002 — attempt_count reset to 0 (1 after first attempt)")
            .isEqualTo(0);

        // Original row preserved
        assertThat(original.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED_PERMANENT);
        org.mockito.Mockito.verify(deliveryRepository, org.mockito.Mockito.never())
            .deleteById(any(UUID.class));

        // Save was called for the replay (fresh row)
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        org.mockito.Mockito.verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(replay.getId());
    }
}
