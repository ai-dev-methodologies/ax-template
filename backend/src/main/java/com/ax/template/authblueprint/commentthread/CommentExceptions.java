package com.ax.template.authblueprint.commentthread;

import java.util.UUID;

public final class CommentExceptions {

    private CommentExceptions() {}

    public static class CommentNotFoundException extends RuntimeException {
        public CommentNotFoundException(UUID id) { super("comment not found: " + id); }
    }

    /** COMMENT-THREAD-001 — mapped to HTTP 400 PARENT_COMMENT_NOT_FOUND. */
    public static class ParentCommentNotFoundException extends RuntimeException {
        public ParentCommentNotFoundException(UUID id) { super("parent comment not found: " + id); }
    }

    /** COMMENT-THREAD-003 — mapped to HTTP 400 CROSS_ENTITY_REPLY. */
    public static class CrossEntityReplyException extends RuntimeException {
        public CrossEntityReplyException(String detail) { super(detail); }
    }

    /** COMMENT-AUTHZ-002 — mapped to HTTP 403 EDIT_FORBIDDEN. */
    public static class EditForbiddenException extends RuntimeException {
        public EditForbiddenException(String detail) { super(detail); }
    }

    /** COMMENT-AUTHZ-003 — mapped to HTTP 403 DELETE_FORBIDDEN. */
    public static class DeleteForbiddenException extends RuntimeException {
        public DeleteForbiddenException(String detail) { super(detail); }
    }
}
