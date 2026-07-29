/**
 * @ax-template-meta
 * template_id: backend/integration/WebhookOutbox
 * layer: backend-domain
 * domain: integration
 * anchors_rule: transactional-outbox-no-dual-write.md
 * provenance_class: internal_design
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Create a webhook_outbox table with columns: id, target_url, event_type,
 *   payload, status (PENDING/DONE/FAILED), retry_count, next_retry_at.
 */
package com.example.app.integration;

import java.util.UUID;

/**
 * Outbox entry for outbound webhook delivery.
 * Adapt to your JPA entity or JDBC record as needed.
 */
public record WebhookOutbox(
        UUID   id,
        String targetUrl,
        String eventType,
        String payload,
        String status,
        int    retryCount
) {
    /** Construct a new PENDING entry (no ID assigned until persist). */
    public WebhookOutbox(String targetUrl, String eventType, String payload) {
        this(UUID.randomUUID(), targetUrl, eventType, payload, "PENDING", 0);
    }
}
