package com.ax.template.authblueprint.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EMIT family — WEBHOOK-EMIT-001, WEBHOOK-EMIT-002.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookEmitTest {

    @Mock WebhookEndpointRepository endpointRepository;
    @Mock WebhookDeliveryRepository deliveryRepository;
    @Mock WebhookHttpClient httpClient;
    @Mock RetryPolicy retryPolicy;
    @Mock CircuitBreakerPolicy circuitBreaker;

    HmacSigner hmacSigner;
    EventTypeMatcher matcher;
    WebhookEndpointService endpointService;
    WebhookSender sender;
    WebhookDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        hmacSigner = new HmacSigner();
        matcher = new EventTypeMatcher();
        endpointService = new WebhookEndpointService(endpointRepository, hmacSigner);
        sender = new WebhookSender(deliveryRepository, endpointRepository, httpClient,
            hmacSigner, retryPolicy, circuitBreaker);
        dispatcher = new WebhookDispatcher(endpointRepository, deliveryRepository, sender, matcher);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-EMIT-001")
    @DisplayName("WEBHOOK-EMIT-001 — register() persists endpoint with active=true and a server-generated secret")
    void emit001_register_persistsActiveEndpointWithSecret() {
        when(endpointRepository.findByUrl("https://hook.example.com/a")).thenReturn(Optional.empty());
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookEndpoint saved = endpointService.register("https://hook.example.com/a", "order.*");

        ArgumentCaptor<WebhookEndpoint> captor = ArgumentCaptor.forClass(WebhookEndpoint.class);
        verify(endpointRepository).save(captor.capture());
        WebhookEndpoint persisted = captor.getValue();

        assertThat(persisted.getId()).as("WEBHOOK-EMIT-001 — generated UUID").isNotNull();
        assertThat(persisted.getUrl()).isEqualTo("https://hook.example.com/a");
        assertThat(persisted.isActive()).as("WEBHOOK-EMIT-001 — active=true on register").isTrue();
        assertThat(persisted.getSigningSecret())
            .as("WEBHOOK-EMIT-001 — 256-bit secret = 64 hex chars")
            .hasSize(64);
        assertThat(persisted.getEventFilter()).isEqualTo("order.*");
        assertThat(saved).isSameAs(persisted);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-EMIT-001")
    @DisplayName("WEBHOOK-EMIT-001 — re-registering same URL rotates secret (idempotent upsert, no duplicate row)")
    void emit001_register_idempotentUpsertRotatesSecret() {
        WebhookEndpoint existing = WebhookEndpoint.create(
            "https://hook.example.com/a", "old-secret-hex", "order.*");
        UUID existingId = existing.getId();
        String oldSecret = existing.getSigningSecret();

        when(endpointRepository.findByUrl("https://hook.example.com/a"))
            .thenReturn(Optional.of(existing));
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookEndpoint result = endpointService.register("https://hook.example.com/a", "user.*");

        assertThat(result.getId())
            .as("WEBHOOK-EMIT-001 — same URL must reuse the SAME id (no duplicate row)")
            .isEqualTo(existingId);
        assertThat(result.getSigningSecret())
            .as("WEBHOOK-EMIT-001 — secret rotated on re-register")
            .isNotEqualTo(oldSecret);
        assertThat(result.getEventFilter())
            .as("WEBHOOK-EMIT-001 — event filter updated on re-register")
            .isEqualTo("user.*");
        verify(endpointRepository, never()).deleteById(any());
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-EMIT-002")
    @DisplayName("WEBHOOK-EMIT-002 — emit() POSTs JSON to every matching active endpoint, NOT to inactive or non-matching ones")
    void emit002_fanOutsToMatchingActiveEndpointsOnly() {
        WebhookEndpoint a = WebhookEndpoint.create(
            "https://a.example.com/hook", "secret-a", "order.*");
        WebhookEndpoint b = WebhookEndpoint.create(
            "https://b.example.com/hook", "secret-b", "order.*");
        WebhookEndpoint c = WebhookEndpoint.create(
            "https://c.example.com/hook", "secret-c", "user.*"); // does NOT match order.created

        when(endpointRepository.findByActiveTrue()).thenReturn(List.of(a, b, c));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.findById(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            // Build a stub PENDING delivery for the sender.attempt() call to load
            WebhookDelivery d = WebhookDelivery.enqueue(a.getId(), "order.created", "{\"x\":1}");
            // Reflect the requested id back so sender mutates the right one
            try {
                java.lang.reflect.Field f = WebhookDelivery.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(d, id);
            } catch (Exception ignore) {}
            return Optional.of(d);
        });
        when(endpointRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(endpointRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(httpClient.post(anyString(), anyString(), any())).thenReturn(
            new WebhookHttpClient.Response(200, null));

        List<WebhookDelivery> dispatched = dispatcher.emit("order.created", "{\"x\":1}");

        assertThat(dispatched)
            .as("WEBHOOK-EMIT-002 — exactly TWO matching endpoints received the event")
            .hasSize(2);

        // Verify HTTP client called for the 2 matching endpoints, content-type JSON,
        // and NOT called for endpoint c (filter mismatch).
        verify(httpClient, times(2)).post(anyString(), eq("{\"x\":1}"), any(Map.class));
        verify(httpClient, never()).post(eq("https://c.example.com/hook"), anyString(), any(Map.class));
    }
}
