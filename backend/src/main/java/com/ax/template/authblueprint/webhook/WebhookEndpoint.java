package com.ax.template.authblueprint.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Registered outbound webhook endpoint.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-EMIT-001 — persisted by {@link WebhookEndpointService#register} with
 *       {@code active=true} and a server-generated 256-bit signing secret.</li>
 *   <li>blueprints/webhook-manifest.yaml#emit — stored_fields contract</li>
 *   <li>WEBHOOK-CIRCUIT-001 — {@link #deactivate} flips {@code active=false} when the
 *       rolling failure-rate threshold is crossed.</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "webhook_endpoints",
    indexes = {
        @Index(name = "ix_webhook_endpoints_url", columnList = "url", unique = true),
        @Index(name = "ix_webhook_endpoints_active", columnList = "active")
    }
)
public class WebhookEndpoint {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "signing_secret", nullable = false, length = 128)
    private String signingSecret;

    /** Event filter pattern, e.g. "order.*" or "order.created"; null = match all. */
    @Column(name = "event_filter", length = 255)
    private String eventFilter;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Required by JPA. */
    protected WebhookEndpoint() {}

    private WebhookEndpoint(UUID id, String url, boolean active, String signingSecret,
                            String eventFilter, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.url = Objects.requireNonNull(url, "url");
        this.active = active;
        this.signingSecret = Objects.requireNonNull(signingSecret, "signingSecret");
        this.eventFilter = eventFilter;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Factory: WEBHOOK-EMIT-001 — creates an active endpoint with a freshly generated secret.
     */
    public static WebhookEndpoint create(String url, String signingSecret, String eventFilter) {
        Instant now = Instant.now();
        return new WebhookEndpoint(UUID.randomUUID(), url, true, signingSecret, eventFilter, now, now);
    }

    /** WEBHOOK-EMIT-001 — rotate secret on re-registration (idempotent upsert by URL). */
    public void rotateSecret(String newSecret) {
        this.signingSecret = Objects.requireNonNull(newSecret, "newSecret");
        this.updatedAt = Instant.now();
    }

    /** Update the event filter without touching the secret. */
    public void updateEventFilter(String eventFilter) {
        this.eventFilter = eventFilter;
        this.updatedAt = Instant.now();
    }

    /** WEBHOOK-CIRCUIT-001 — circuit-open transition. */
    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    /** Manual admin re-activation after circuit open. */
    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUrl() { return url; }
    public boolean isActive() { return active; }
    public String getSigningSecret() { return signingSecret; }
    public String getEventFilter() { return eventFilter; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
