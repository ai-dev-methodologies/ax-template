/**
 * @ax-template-meta
 * template_id: backend/integration/WebhookOutboxRepository
 * layer: backend-infrastructure
 * domain: integration
 * anchors_rule: transactional-outbox-no-dual-write.md
 * provenance_class: internal_design
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Implement with Spring Data JPA or JdbcTemplate against a webhook_outbox table.
 */
package com.example.app.integration;

import java.util.List;
import java.util.UUID;

/**
 * Repository port for the webhook outbox.
 */
public interface WebhookOutboxRepository {

    WebhookOutbox save(WebhookOutbox entry);

    /** Returns PENDING entries whose retry count is below maxRetries and next_retry_at <= now. */
    List<WebhookOutbox> findPendingWithRetryBudget(int maxRetries);

    void markDone(UUID id);

    void markFailed(UUID id);

    void scheduleRetry(UUID id, int newRetryCount, long delayMs);
}
