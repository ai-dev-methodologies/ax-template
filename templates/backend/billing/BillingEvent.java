/**
 * @ax-template-meta
 * template_id: backend/billing/BillingEvent
 * layer: backend-domain
 * domain: billing
 * anchors_rule: billing-event-idempotent.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Idempotency"
 *     quote: "To perform an idempotent request, provide an additional Idempotency-Key: <key> header to the request."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "멱등성"
 *     quote: "Idempotency-Key 헤더를 사용하면 네트워크 오류로 인한 재시도 시 중복 결제를 방지할 수 있습니다."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   idempotencyKey is UNIQUE — duplicate provider events (same key) are rejected by DB constraint.
 *   @SQLDelete required on this @Entity subclass.
 *   BillingEvent is append-only; never update an existing event row.
 */
package com.example.app.billing;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;

/**
 * BillingEvent entity — immutable audit record of a billing lifecycle event.
 *
 * <p>Every state transition in the billing domain records a BillingEvent.
 * BillingEvents are append-only: once created, they are never mutated.
 *
 * <p>Idempotency: {@code idempotencyKey} is a UNIQUE constraint.
 * Duplicate webhook delivery (same provider event) is detected by this key
 * and rejected with HTTP 409 Conflict before a second row is inserted.
 *
 * <p>Rule enforced: {@code billing-event-idempotent} (PRACTICES-BILLING-001).
 *
 * <p>Boundary: BillingEvent belongs to billing domain. No import from payment domain.
 */
@Entity
@SQLDelete(sql = "UPDATE billing_events SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "billing_events",
    indexes = {
        @Index(name = "idx_billing_events_sub", columnList = "subscription_id"),
        @Index(name = "idx_billing_events_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_billing_events_occurred", columnList = "occurred_at"),
    }
)
public class BillingEvent extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private BillingEventType eventType;

    /**
     * Idempotency key — UNIQUE per event. Prevents duplicate event processing.
     * For provider webhooks: use providerEventId. For internal events: use UUID.
     *
     * <p>Rule: billing-event-idempotent — all BillingEvent writes MUST supply this key.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    /** Provider's event ID (e.g., Stripe evt_xxx, Toss payment_xxx). Null for internal events. */
    @Column(name = "provider_event_id", length = 255)
    private String providerEventId;

    /**
     * JSON metadata for the event (provider-specific fields, normalized).
     * Never store raw PAN or sensitive payment credentials here.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;

    /** When the event occurred (provider timestamp or local Instant.now()). */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected BillingEvent() {}

    /**
     * Factory constructor for internal events (no provider event ID).
     */
    public static BillingEvent createInternal(
            UUID subscriptionId,
            BillingEventType eventType,
            String metadataJson) {
        var e = new BillingEvent();
        e.subscriptionId = subscriptionId;
        e.eventType = eventType;
        e.idempotencyKey = UUID.randomUUID().toString();
        e.occurredAt = Instant.now();
        e.metadataJson = metadataJson;
        return e;
    }

    /**
     * Factory constructor for webhook-sourced events.
     *
     * @param subscriptionId     billing subscription UUID
     * @param eventType          normalized event type
     * @param idempotencyKey     caller-supplied idempotency key (or providerEventId)
     * @param providerEventId    provider's event identifier (e.g., Stripe evt_xxx)
     * @param occurredAt         provider's event timestamp
     * @param metadataJson       normalized metadata JSON string
     */
    public static BillingEvent fromWebhook(
            UUID subscriptionId,
            BillingEventType eventType,
            String idempotencyKey,
            String providerEventId,
            Instant occurredAt,
            String metadataJson) {
        var e = new BillingEvent();
        e.subscriptionId = subscriptionId;
        e.eventType = eventType;
        e.idempotencyKey = idempotencyKey;
        e.providerEventId = providerEventId;
        e.occurredAt = occurredAt;
        e.metadataJson = metadataJson;
        return e;
    }

    public UUID getSubscriptionId()          { return subscriptionId; }
    public BillingEventType getEventType()   { return eventType; }
    public String getIdempotencyKey()        { return idempotencyKey; }
    public String getProviderEventId()       { return providerEventId; }
    public String getMetadataJson()          { return metadataJson; }
    public Instant getOccurredAt()           { return occurredAt; }

    public enum BillingEventType {
        SUBSCRIPTION_CREATED,
        TRIAL_END,
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        SUBSCRIPTION_RENEWED,
        SUBSCRIPTION_CANCELLED,
        PLAN_CHANGED,
        WEBHOOK_RECEIVED,
        ADMIN_OVERRIDE
    }
}
