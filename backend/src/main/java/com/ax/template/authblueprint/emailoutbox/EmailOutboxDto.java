package com.ax.template.authblueprint.emailoutbox;

import java.time.Instant;
import java.util.UUID;

public final class EmailOutboxDto {

    private EmailOutboxDto() {}

    /**
     * R50 stored-server-error-sanitize-at-render-layer: the catalog client
     * MUST pass {@code lastError} through a render-layer sanitize helper
     * before display (the value is server-derived from the sender adapter
     * and can carry stack-trace fragments or internal hostnames).
     */
    public record OutboxResponse(
        UUID id,
        String recipient,
        String templateCode,
        String subject,
        EmailOutboxStatus status,
        int retryCount,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant sentAt
    ) {
        public static OutboxResponse from(EmailOutbox e) {
            return new OutboxResponse(
                e.getId(), e.getRecipient(), e.getTemplateCode(), e.getSubject(),
                e.getStatus(), e.getRetryCount(), e.getNextAttemptAt(),
                e.getLastError(), e.getCreatedAt(), e.getSentAt());
        }
    }
}
