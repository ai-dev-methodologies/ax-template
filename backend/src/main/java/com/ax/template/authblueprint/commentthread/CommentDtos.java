package com.ax.template.authblueprint.commentthread;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommentDtos {

    private CommentDtos() {}

    public static final String DELETED_BODY_MASK = "[deleted]";

    public record CreateCommentRequest(
        @NotBlank @Size(max = 64) String entityType,
        @NotBlank @Size(max = 255) String entityId,
        @NotBlank @Size(max = 4000) String body,
        UUID parentCommentId
    ) {}

    public record UpdateCommentRequest(@NotBlank @Size(max = 4000) String body) {}

    public record CommentResponse(
        UUID id,
        String authorUserId,
        String entityType,
        String entityId,
        UUID parentCommentId,
        String body,
        CommentStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String deletedByUserId
    ) {

        public static CommentResponse from(Comment c) {
            String visibleBody = (c.getStatus() == CommentStatus.DELETED || c.getBody() == null)
                ? DELETED_BODY_MASK
                : c.getBody();
            return new CommentResponse(
                c.getId(),
                c.getAuthorUserId(),
                c.getEntityType(),
                c.getEntityId(),
                c.getParentCommentId(),
                visibleBody,
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getDeletedAt(),
                c.getDeletedByUserId()
            );
        }
    }

    public record CommentListResponse(List<CommentResponse> items, long totalElements) {}

    public record CommentEditResponse(
        UUID id,
        Instant editedAt,
        String editedByUserId,
        String previousBody
    ) {
        public static CommentEditResponse from(CommentEdit e) {
            return new CommentEditResponse(e.getId(), e.getEditedAt(), e.getEditedByUserId(), e.getPreviousBody());
        }
    }

    public record CommentHistoryResponse(UUID commentId, List<CommentEditResponse> edits) {}
}
