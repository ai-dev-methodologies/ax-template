/**
 * @ax-template-meta
 * template_id: backend/notification/Notification
 * layer: backend-domain
 * domain: notification
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "OWASP ASVS V4.2.1 — Access control verifies user owns the resource"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Notification entity covers in-app delivery, read lifecycle, and soft-delete.
 *   recipientUserId must always be compared against JWT principal to prevent IDOR.
 */
package com.example.app.notification;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity — in-app notification message for a specific recipient.
 *
 * <p>Lifecycle:
 * <pre>
 *   UNREAD (created) → READ (markRead) → [soft-deleted] (dismiss)
 * </pre>
 *
 * <p>All mutations (markRead, dismiss) must verify that the caller's userId
 * matches {@code recipientUserId} before applying the change.
 * Cross-user access must return 404 (not 403) per NOTIF-AUTHZ-002.
 *
 * <p>Extends {@code BaseEntity} (from SP13) for: id (UUID), createdAt, updatedAt, deleted.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_status", columnList = "recipient_user_id, status"),
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
    }
)
public class Notification extends BaseEntity {

    // ─── recipient ─────────────────────────────────────────────────────────

    /**
     * Target user of this notification. Never exposes this via URL path;
     * always resolved from the JWT principal on service calls.
     */
    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private UUID recipientUserId;

    // ─── content ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Optional deep-link URL for the notification action. */
    @Column(name = "action_url", length = 2048)
    private String actionUrl;

    // ─── lifecycle ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.UNREAD;

    /** Timestamp when the notification was marked READ. Null if still UNREAD. */
    @Column(name = "read_at")
    private Instant readAt;

    // ─── constructors ──────────────────────────────────────────────────────

    protected Notification() {
        // JPA
    }

    /**
     * Factory constructor — creates a new UNREAD notification.
     *
     * @param recipientUserId target user's UUID
     * @param type            notification category
     * @param title           short title (max 255 chars)
     * @param body            full body text (max 2000 chars)
     * @param actionUrl       optional deep-link URL (null if none)
     */
    public static Notification create(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String body,
            String actionUrl) {
        var n = new Notification();
        n.recipientUserId = recipientUserId;
        n.type = type;
        n.title = title;
        n.body = body;
        n.actionUrl = actionUrl;
        n.status = NotificationStatus.UNREAD;
        return n;
    }

    // ─── domain transitions ────────────────────────────────────────────────

    /**
     * Marks this notification as READ (idempotent).
     *
     * <p>Calling on an already-READ notification is a no-op.
     */
    public void markRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = Instant.now();
        }
    }

    // ─── getters ───────────────────────────────────────────────────────────

    public UUID getRecipientUserId() { return recipientUserId; }
    public NotificationType getType()      { return type; }
    public String getTitle()               { return title; }
    public String getBody()                { return body; }
    public String getActionUrl()           { return actionUrl; }
    public NotificationStatus getStatus()  { return status; }
    public Instant getReadAt()             { return readAt; }

    // ─── enums ─────────────────────────────────────────────────────────────

    public enum NotificationType {
        SYSTEM, ALERT, REMINDER, PROMOTION, ACCOUNT
    }

    public enum NotificationStatus {
        UNREAD, READ
    }
}
