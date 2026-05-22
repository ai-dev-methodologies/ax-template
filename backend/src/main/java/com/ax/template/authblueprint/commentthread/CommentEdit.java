package com.ax.template.authblueprint.commentthread;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * CommentEdit — immutable edit-history row. Captured BEFORE each PUT mutates the
 * parent Comment so the original text is preserved even across multiple edits.
 *
 * <p>Trace: COMMENT-HISTORY-001 — every field is @Column(updatable=false).
 */
@Entity
@Table(
    name = "comment_edits",
    indexes = {
        @Index(name = "ix_comment_edits_comment_edited", columnList = "comment_id,edited_at")
    }
)
public class CommentEdit {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "comment_id", nullable = false, updatable = false)
    private UUID commentId;

    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt;

    @Column(name = "edited_by_user_id", nullable = false, updatable = false, length = 255)
    private String editedByUserId;

    @Column(name = "previous_body", updatable = false, length = 4000)
    private String previousBody;

    protected CommentEdit() {}

    public CommentEdit(UUID commentId, Instant editedAt, String editedByUserId, String previousBody) {
        this.id = UUID.randomUUID();
        this.commentId = commentId;
        this.editedAt = editedAt;
        this.editedByUserId = editedByUserId;
        this.previousBody = previousBody;
    }

    public UUID getId() { return id; }
    public UUID getCommentId() { return commentId; }
    public Instant getEditedAt() { return editedAt; }
    public String getEditedByUserId() { return editedByUserId; }
    public String getPreviousBody() { return previousBody; }
}
