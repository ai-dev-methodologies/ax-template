package com.ax.template.authblueprint.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only ledger row for every webhook + state-machine transition.
 * <p>Trace:
 * <ul>
 *   <li>BILLING-IDEMP-001 — DB unique constraint on {@code providerEventId}.</li>
 *   <li>BILLING-STATE-002 — every TRIAL→ACTIVE writes TRIAL_END; ACTIVE→PAST_DUE writes PAYMENT_FAILED.</li>
 * </ul>
 */
@Entity
@Table(name = "billing_events")
public class BillingEvent {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "provider_event_id", nullable = false, length = 120, unique = true)
    private String providerEventId;

    @Column(name = "subscription_id", length = 36)
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private BillingEventType eventType;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "payload", length = 4000)
    private String payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected BillingEvent() {}

    private BillingEvent(String id, String providerEventId, String subscriptionId,
                         BillingEventType eventType, String provider, String payload,
                         Instant receivedAt) {
        this.id = id;
        this.providerEventId = Objects.requireNonNull(providerEventId);
        this.subscriptionId = subscriptionId;
        this.eventType = Objects.requireNonNull(eventType);
        this.provider = Objects.requireNonNull(provider);
        this.payload = payload;
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    public static BillingEvent of(String providerEventId, String subscriptionId,
                                  BillingEventType type, String provider, String payload) {
        return new BillingEvent(UUID.randomUUID().toString(), providerEventId,
            subscriptionId, type, provider, payload, Instant.now());
    }

    public String getId() { return id; }
    public String getProviderEventId() { return providerEventId; }
    public String getSubscriptionId() { return subscriptionId; }
    public BillingEventType getEventType() { return eventType; }
    public String getProvider() { return provider; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
}
