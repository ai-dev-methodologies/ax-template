package com.ax.template.authblueprint.emailoutbox;

import java.time.Instant;
import java.util.UUID;

import java.util.List;

public final class EmailOutboxDto {

    private EmailOutboxDto() {}

    /**
     * R50 stored-server-error-sanitize-at-render-layer: the catalog client
     * MUST pass {@code lastError} through a render-layer sanitize helper
     * before display (the value is server-derived from the sender adapter
     * and can carry stack-trace fragments or internal hostnames).
     *
     * <p>R60 dogfood F6 closure (server-side companion): {@code lastError}
     * is now also scrubbed at storage time via
     * {@link AuditPiiHelper#sanitizeReason}. The render-layer scrub is
     * defense-in-depth, not the sole filter.
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
        Instant sentAt,
        Instant lastFailureAt
    ) {
        public static OutboxResponse from(EmailOutbox e) {
            return new OutboxResponse(
                e.getId(), e.getRecipient(), e.getTemplateCode(), e.getSubject(),
                e.getStatus(), e.getRetryCount(), e.getNextAttemptAt(),
                e.getLastError(), e.getCreatedAt(), e.getSentAt(),
                e.getLastFailureAt());
        }
    }

    /**
     * R60 dogfood F3 closure — admin list now returns Page metadata so
     * the operator UI can paginate accurately. The previous List-only
     * shape dropped Page.totalElements at the controller boundary; a
     * 5000-row outbox displayed as "50 rows shown" with no clue how
     * many more existed.
     */
    public record OutboxPage(
        List<OutboxResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
}
