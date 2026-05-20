package com.ax.template.authblueprint.webhook;

import java.time.Instant;
import java.util.UUID;

/**
 * REST DTO container for the webhook admin surface.
 * <p>
 * Records mirror exactly what blueprints/webhook-manifest.yaml#emit.stored_fields
 * exposes — note that {@code signing_secret} is returned ONCE on the create
 * response (so the admin can store it) and NEVER on subsequent GETs.
 */
public final class WebhookDto {

    private WebhookDto() {}

    public record RegisterRequest(String url, String eventFilter) {}

    /** Returned by POST — includes the secret so the caller can persist it. */
    public record EndpointWithSecret(
        UUID id,
        String url,
        boolean active,
        String signingSecret,
        String eventFilter,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static EndpointWithSecret from(WebhookEndpoint e) {
            return new EndpointWithSecret(
                e.getId(), e.getUrl(), e.isActive(), e.getSigningSecret(),
                e.getEventFilter(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    /** Returned by GET — secret is omitted. */
    public record EndpointResponse(
        UUID id,
        String url,
        boolean active,
        String eventFilter,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static EndpointResponse from(WebhookEndpoint e) {
            return new EndpointResponse(
                e.getId(), e.getUrl(), e.isActive(),
                e.getEventFilter(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public record DeliveryResponse(
        UUID id,
        UUID endpointId,
        String eventType,
        WebhookDeliveryStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Integer lastResponseCode,
        Instant lastAttemptAt,
        String lastError,
        Instant createdAt
    ) {
        public static DeliveryResponse from(WebhookDelivery d) {
            return new DeliveryResponse(
                d.getId(), d.getEndpointId(), d.getEventType(), d.getStatus(),
                d.getAttemptCount(), d.getNextAttemptAt(), d.getLastResponseCode(),
                d.getLastAttemptAt(), d.getLastError(), d.getCreatedAt());
        }
    }
}
