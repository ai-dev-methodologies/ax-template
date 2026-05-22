package com.ax.template.authblueprint.commentthread;

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
 * Comment — entity-agnostic threaded comment with soft-delete.
 *
 * <p>Trace:
 * <ul>
 *   <li>COMMENT-CRUD-001 — authorUserId stamped server-side</li>
 *   <li>COMMENT-CRUD-003 — soft delete (status flip + body→NULL)</li>
 *   <li>COMMENT-THREAD-002 — parentCommentId nullable; flat list ordered by createdAt</li>
 *   <li>COMMENT-AUTHZ-002/003 — author + admin scoping</li>
 * </ul>
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "ix_comments_entity_created", columnList = "entity_type,entity_id,created_at"),
        @Index(name = "ix_comments_parent", columnList = "parent_comment_id"),
        @Index(name = "ix_comments_author_status", columnList = "author_user_id,status")
    }
)
public class Comment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "author_user_id", nullable = false, updatable = false, length = 255)
    private String authorUserId;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 255)
    private String entityId;

    @Column(name = "parent_comment_id", updatable = false)
    private UUID parentCommentId;

    /** Body is mutable on edit; NULL when soft-deleted. */
    @Column(name = "body", length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by_user_id", length = 255)
    private String deletedByUserId;

    protected Comment() {}

    private Comment(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.authorUserId = b.authorUserId;
        this.entityType = b.entityType;
        this.entityId = b.entityId;
        this.parentCommentId = b.parentCommentId;
        this.body = b.body;
        this.status = (b.status != null) ? b.status : CommentStatus.ACTIVE;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public String getAuthorUserId() { return authorUserId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public UUID getParentCommentId() { return parentCommentId; }
    public String getBody() { return body; }
    public CommentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public String getDeletedByUserId() { return deletedByUserId; }

    // Package-private — service is the only mutator.
    void editBody(String newBody, Instant now) {
        this.body = newBody;
        this.updatedAt = now;
    }

    void softDelete(String actorUserId, Instant now) {
        if (this.status == CommentStatus.DELETED) return;
        this.status = CommentStatus.DELETED;
        this.body = null;
        this.deletedAt = now;
        this.deletedByUserId = actorUserId;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String authorUserId;
        private String entityType;
        private String entityId;
        private UUID parentCommentId;
        private String body;
        private CommentStatus status;
        private Instant createdAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder authorUserId(String v) { this.authorUserId = v; return this; }
        public Builder entityType(String v) { this.entityType = v; return this; }
        public Builder entityId(String v) { this.entityId = v; return this; }
        public Builder parentCommentId(UUID v) { this.parentCommentId = v; return this; }
        public Builder body(String v) { this.body = v; return this; }
        public Builder status(CommentStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public Comment build() { return new Comment(this); }
    }
}
