/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationDto
 * layer: backend-domain
 * domain: notification
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (Final, Java 16)"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "OWASP Mass Assignment Cheat Sheet — only expose fields the client is allowed to set"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All inner records are Java 16 records — immutable value types with auto equals/hashCode/toString.
 *   Map at service layer: Response.from(Notification entity).
 */
package com.example.app.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO container for the notification domain.
 *
 * <p>Contains three inner records:
 * <ul>
 *   <li>{@link SendRequest}     — body for POST /api/notifications (admin only)
 *   <li>{@link Response}        — response body for single notification or list item
 *   <li>{@link PreferencesResponse} — response for GET/PATCH /api/notifications/preferences
 *   <li>{@link UpdatePreferencesRequest} — body for PATCH /api/notifications/preferences
 * </ul>
 *
 * <p>Rule reference: PRACTICES-LANG-001 (records for DTOs).
 */
public final class NotificationDto {

    private NotificationDto() {}

    // ─── request DTOs ────────────────────────────────────────────────────────

    /**
     * Request body for POST /api/notifications.
     *
     * <p>Admin-only endpoint. Caller supplies the target recipient and message content.
     */
    public record SendRequest(
        @NotNull
        UUID recipientUserId,

        @NotNull
        Notification.NotificationType type,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 2000)
        String body,

        @Size(max = 2048)
        String actionUrl
    ) {}

    /**
     * Request body for PATCH /api/notifications/preferences.
     *
     * <p>Partial update: null fields are ignored; only non-null fields are applied.
     */
    public record UpdatePreferencesRequest(
        Boolean inAppEnabled,
        Boolean emailEnabled
    ) {}

    // ─── response DTOs ───────────────────────────────────────────────────────

    /**
     * Response DTO for a single notification or list item.
     */
    public record Response(
        UUID id,
        UUID recipientUserId,
        Notification.NotificationType type,
        String title,
        String body,
        String actionUrl,
        Notification.NotificationStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        /**
         * Maps a Notification entity to its response DTO.
         */
        public static Response from(Notification n) {
            return new Response(
                n.getId(),
                n.getRecipientUserId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getActionUrl(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getUpdatedAt()
            );
        }
    }

    /**
     * Response DTO for notification channel preferences.
     */
    public record PreferencesResponse(
        UUID userId,
        boolean inAppEnabled,
        boolean emailEnabled,
        Instant updatedAt
    ) {
        /**
         * Default preferences (all channels enabled).
         *
         * <p>Returned when no preferences row exists yet (lazy-init policy).
         */
        public static PreferencesResponse defaults(UUID userId) {
            return new PreferencesResponse(userId, true, true, null);
        }

        /**
         * Maps a NotificationPreferences entity to its response DTO.
         */
        public static PreferencesResponse from(NotificationPreferences prefs) {
            return new PreferencesResponse(
                prefs.getUserId(),
                prefs.isInAppEnabled(),
                prefs.isEmailEnabled(),
                prefs.getUpdatedAt()
            );
        }
    }
}
