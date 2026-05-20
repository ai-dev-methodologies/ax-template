package com.ax.template.authblueprint.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity — keyed by recipient user.
 * <p>
 * Trace:
 * <ul>
 *   <li>NOTIF-SEND-001 — persisted on POST with status=UNREAD</li>
 *   <li>NOTIF-LIST-001 / NOTIF-LIST-002 — indexed on (recipient_user_id, created_at, status)</li>
 *   <li>NOTIF-READ-001 — status transitions to READ</li>
 *   <li>NOTIF-DISMISS-001 — soft delete via {@code deleted=true}</li>
 *   <li>NOTIF-AUTHZ-002 — caller-side filter (recipient_user_id = caller) before any return</li>
 * </ul>
 * Manifest: {@code blueprints/notification-manifest.yaml}.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "ix_notifications_recipient_created",
               columnList = "recipient_user_id,created_at"),
        @Index(name = "ix_notifications_recipient_status",
               columnList = "recipient_user_id,status,deleted")
    }
)
public class Notification {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false, updatable = false, length = 255)
    private String recipientUserId;

    @Column(name = "type", nullable = false, updatable = false, length = 64)
    private String type;

    @Column(name = "title", nullable = false, updatable = false, length = 255)
    private String title;

    @Column(name = "body", updatable = false, length = 2000)
    private String body;

    @Column(name = "link", updatable = false, length = 1024)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected Notification() {}

    private Notification(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.recipientUserId = b.recipientUserId;
        this.type = b.type;
        this.title = b.title;
        this.body = b.body;
        this.link = b.link;
        this.status = (b.status != null) ? b.status : NotificationStatus.UNREAD;
        this.deleted = b.deleted;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.updatedAt = (b.updatedAt != null) ? b.updatedAt : this.createdAt;
    }

    public UUID getId() { return id; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public NotificationStatus getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** NOTIF-READ-001 — idempotent mark as read. */
    public void markRead(Instant now) {
        if (this.status != NotificationStatus.READ) {
            this.status = NotificationStatus.READ;
            this.updatedAt = now;
        }
    }

    /** NOTIF-DISMISS-001 — soft delete. */
    public void softDelete(Instant now) {
        this.deleted = true;
        this.updatedAt = now;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String recipientUserId;
        private String type;
        private String title;
        private String body;
        private String link;
        private NotificationStatus status;
        private boolean deleted = false;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder recipientUserId(String v) { this.recipientUserId = v; return this; }
        public Builder type(String v) { this.type = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder body(String v) { this.body = v; return this; }
        public Builder link(String v) { this.link = v; return this; }
        public Builder status(NotificationStatus v) { this.status = v; return this; }
        public Builder deleted(boolean v) { this.deleted = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        public Notification build() { return new Notification(this); }
    }
}
