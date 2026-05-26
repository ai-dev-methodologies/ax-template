package com.ax.template.authblueprint.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One row per (endpoint, event) delivery. Retries reuse the same row so that
 * {@code X-Webhook-Delivery-Id} stays stable across the retry chain
 * (WEBHOOK-RETRY-002).
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-EMIT-002 — one row created per matching active endpoint before the HTTP call.</li>
 *   <li>WEBHOOK-RETRY-001 — {@code attemptCount} + {@code nextAttemptAt} drive the backoff schedule.</li>
 *   <li>WEBHOOK-RETRY-002 — primary-key {@link #id} is the stable delivery_id header value.</li>
 *   <li>WEBHOOK-DEAD-LETTER-001 — terminal transition to {@link WebhookDeliveryStatus#FAILED_PERMANENT}.</li>
 *   <li>WEBHOOK-DEAD-LETTER-002 — admin replay creates a NEW row with a fresh id, leaving this one intact.</li>
 * </ul>
 */
@Entity
@Table(
    name = "webhook_deliveries",
    indexes = {
        @Index(name = "ix_webhook_deliveries_endpoint", columnList = "endpoint_id"),
        @Index(name = "ix_webhook_deliveries_status", columnList = "status"),
        @Index(name = "ix_webhook_deliveries_next_attempt_at", columnList = "next_attempt_at")
    }
)
public class WebhookDelivery {

    /** Maximum attempts including the first — WEBHOOK-RETRY-001 manifest contract. */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. */
    protected WebhookDelivery() {}

    private WebhookDelivery(UUID id, UUID endpointId, String eventType, String body,
                            WebhookDeliveryStatus status, int attemptCount,
                            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.endpointId = Objects.requireNonNull(endpointId, "endpointId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.body = Objects.requireNonNull(body, "body");
        this.status = Objects.requireNonNull(status, "status");
        this.attemptCount = attemptCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** WEBHOOK-EMIT-002 — enqueue a fresh delivery row before the first HTTP attempt. */
    public static WebhookDelivery enqueue(UUID endpointId, String eventType, String body) {
        return new WebhookDelivery(UUID.randomUUID(), endpointId, eventType, body,
            WebhookDeliveryStatus.PENDING, 0, Instant.now());
    }

    /**
     * Record a successful attempt — terminal transition to {@link WebhookDeliveryStatus#SUCCEEDED}.
     */
    public void markSucceeded(int responseCode, Instant when) {
        this.status = WebhookDeliveryStatus.SUCCEEDED;
        this.lastResponseCode = responseCode;
        this.lastAttemptAt = when;
        this.nextAttemptAt = null;
        this.lastError = null;
        this.attemptCount += 1;
    }

    /**
     * Record a retriable failure and schedule the next attempt — WEBHOOK-RETRY-001.
     */
    public void markRetry(Integer responseCode, String errorMessage, Instant when, Instant nextAttemptAt) {
        this.status = WebhookDeliveryStatus.PENDING_RETRY;
        this.lastResponseCode = responseCode;
        this.lastError = truncate(errorMessage);
        this.lastAttemptAt = when;
        this.nextAttemptAt = nextAttemptAt;
        this.attemptCount += 1;
    }

    /**
     * Record a terminal failure — WEBHOOK-DEAD-LETTER-001 / permanent 4xx classification.
     */
    public void markFailedPermanent(Integer responseCode, String errorMessage, Instant when) {
        this.status = WebhookDeliveryStatus.FAILED_PERMANENT;
        this.lastResponseCode = responseCode;
        this.lastError = truncate(errorMessage);
        this.lastAttemptAt = when;
        this.nextAttemptAt = null;
        this.attemptCount += 1;
    }

    /**
     * R63 — anchors R61 server-side-stored-error-sanitize. Webhook
     * provider exceptions routinely carry the destination URL host,
     * partner tenant ids, signed-headers excerpts, and JWT/Bearer
     * fragments. Scrub PII at the entity boundary so the column
     * itself never holds plain values, regardless of which caller
     * invoked markRetry / markFailedPermanent.
     */
    private static String truncate(String s) {
        if (s == null) return null;
        String scrubbed =
            com.ax.template.authblueprint.emailoutbox.EmailPiiHelper.sanitizeReason(s);
        return scrubbed.length() > 1024 ? scrubbed.substring(0, 1024) : scrubbed;
    }

    public UUID getId() { return id; }
    public UUID getEndpointId() { return endpointId; }
    public String getEventType() { return eventType; }
    public String getBody() { return body; }
    public WebhookDeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Integer getLastResponseCode() { return lastResponseCode; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
