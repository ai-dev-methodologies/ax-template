package com.ax.template.authblueprint.webhook;

import com.ax.template.authblueprint.auditlog.AuditLog;
import com.ax.template.authblueprint.auditlog.AuditLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CIRCUIT-BREAKER family — WEBHOOK-CIRCUIT-001.
 */
@ExtendWith(MockitoExtension.class)
class WebhookCircuitBreakerTest {

    @Mock WebhookDeliveryRepository deliveryRepository;
    @Mock WebhookEndpointRepository endpointRepository;
    @Mock AuditLogService auditLogService;

    CircuitBreakerPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CircuitBreakerPolicy(deliveryRepository, endpointRepository, auditLogService);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-CIRCUIT-001")
    @DisplayName("WEBHOOK-CIRCUIT-001 — 90% failure rate over 50 attempts opens circuit + emits audit log")
    void circuit001_ninetyPercentFailureOpensCircuit() {
        UUID endpointId = UUID.randomUUID();
        WebhookEndpoint endpoint = WebhookEndpoint.create("https://broken.example.com", "s", null);
        // Reflect the desired id into the endpoint so equality lines up
        setId(endpoint, endpointId);

        // 50 terminal deliveries: 45 FAILED_PERMANENT + 5 SUCCEEDED = 90% failure rate
        List<WebhookDelivery> window = synthesizeTerminalWindow(endpointId, 45, 5);
        when(deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(eq(endpointId), any(Pageable.class)))
            .thenReturn(window);
        when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        policy.evaluate(endpointId);

        assertThat(endpoint.isActive())
            .as("WEBHOOK-CIRCUIT-001 — endpoint MUST be deactivated when failure rate ≥ 90%")
            .isFalse();
        verify(endpointRepository).save(endpoint);

        // Audit-log row emitted with action=webhook.endpoint.circuit_opened
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).record(auditCaptor.capture());
        AuditLog audited = auditCaptor.getValue();
        assertThat(audited.getAction()).isEqualTo(CircuitBreakerPolicy.AUDIT_ACTION_CIRCUIT_OPENED);
        assertThat(audited.getResourceType()).isEqualTo("webhook_endpoint");
        assertThat(audited.getResourceId()).isEqualTo(endpointId.toString());
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-CIRCUIT-001")
    @DisplayName("WEBHOOK-CIRCUIT-001 — 88% failure rate (44/50) does NOT open the circuit")
    void circuit001_belowThreshold_stayClosed() {
        UUID endpointId = UUID.randomUUID();
        WebhookEndpoint endpoint = WebhookEndpoint.create("https://ok.example.com", "s", null);
        setId(endpoint, endpointId);

        // 44 FAILED + 6 SUCCEEDED = 88% — below 90% threshold
        List<WebhookDelivery> window = synthesizeTerminalWindow(endpointId, 44, 6);
        when(deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(eq(endpointId), any(Pageable.class)))
            .thenReturn(window);

        policy.evaluate(endpointId);

        assertThat(endpoint.isActive())
            .as("WEBHOOK-CIRCUIT-001 — 88% failure rate must NOT open the circuit")
            .isTrue();
        verify(endpointRepository, never()).save(any(WebhookEndpoint.class));
        verify(auditLogService, never()).record(any());
    }

    private List<WebhookDelivery> synthesizeTerminalWindow(UUID endpointId, int failed, int succeeded) {
        List<WebhookDelivery> rows = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < failed; i++) {
            WebhookDelivery d = WebhookDelivery.enqueue(endpointId, "evt", "{}");
            d.markFailedPermanent(500, "boom", now);
            rows.add(d);
        }
        for (int i = 0; i < succeeded; i++) {
            WebhookDelivery d = WebhookDelivery.enqueue(endpointId, "evt", "{}");
            d.markSucceeded(200, now);
            rows.add(d);
        }
        return rows;
    }

    private static void setId(WebhookEndpoint endpoint, UUID id) {
        try {
            java.lang.reflect.Field f = WebhookEndpoint.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(endpoint, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
