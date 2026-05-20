package com.ax.template.authblueprint.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO returned to API clients. Excludes {@code deleted} (soft-delete flag is
 * internal) and omits cross-user fields.
 */
public record NotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    String link,
    NotificationStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getType(),
            n.getTitle(),
            n.getBody(),
            n.getLink(),
            n.getStatus(),
            n.getCreatedAt(),
            n.getUpdatedAt()
        );
    }
}
