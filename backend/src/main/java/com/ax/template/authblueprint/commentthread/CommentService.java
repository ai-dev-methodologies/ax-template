package com.ax.template.authblueprint.commentthread;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.commentthread.CommentDtos.CommentEditResponse;
import com.ax.template.authblueprint.commentthread.CommentDtos.CommentHistoryResponse;
import com.ax.template.authblueprint.commentthread.CommentDtos.CommentListResponse;
import com.ax.template.authblueprint.commentthread.CommentDtos.CommentResponse;
import com.ax.template.authblueprint.commentthread.CommentDtos.CreateCommentRequest;
import com.ax.template.authblueprint.commentthread.CommentDtos.UpdateCommentRequest;
import com.ax.template.authblueprint.commentthread.CommentExceptions.CommentNotFoundException;
import com.ax.template.authblueprint.commentthread.CommentExceptions.CrossEntityReplyException;
import com.ax.template.authblueprint.commentthread.CommentExceptions.DeleteForbiddenException;
import com.ax.template.authblueprint.commentthread.CommentExceptions.EditForbiddenException;
import com.ax.template.authblueprint.commentthread.CommentExceptions.ParentCommentNotFoundException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentEditRepository editRepository;
    private final Clock clock;

    public CommentService(CommentRepository commentRepository,
                          CommentEditRepository editRepository,
                          Clock clock) {
        this.commentRepository = commentRepository;
        this.editRepository = editRepository;
        this.clock = clock;
    }

    @Transactional
    public CommentResponse create(String authorUserId, CreateCommentRequest request) {
        UUID parentId = request.parentCommentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ParentCommentNotFoundException(parentId));
            if (!parent.getEntityType().equals(request.entityType())
                || !parent.getEntityId().equals(request.entityId())) {
                throw new CrossEntityReplyException(
                    "parent " + parentId + " is on (" + parent.getEntityType() + ", " + parent.getEntityId()
                    + ") but reply targets (" + request.entityType() + ", " + request.entityId() + ")");
            }
        }
        Comment comment = Comment.builder()
            .authorUserId(authorUserId)
            .entityType(request.entityType())
            .entityId(request.entityId())
            .parentCommentId(parentId)
            .body(request.body())
            .status(CommentStatus.ACTIVE)
            .createdAt(Instant.now(clock))
            .build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse edit(Authentication auth, UUID id, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new CommentNotFoundException(id));
        if (!comment.getAuthorUserId().equals(auth.getName())) {
            throw new EditForbiddenException("only the author may edit comment " + id);
        }
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new EditForbiddenException("comment " + id + " is deleted");
        }
        // Capture pre-image BEFORE mutating the parent (COMMENT-HISTORY-001).
        editRepository.save(new CommentEdit(
            comment.getId(),
            Instant.now(clock),
            auth.getName(),
            comment.getBody()
        ));
        comment.editBody(request.body(), Instant.now(clock));
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Authentication auth, UUID id) {
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new CommentNotFoundException(id));
        boolean isAuthor = comment.getAuthorUserId().equals(auth.getName());
        boolean isAdmin = isAdmin(auth);
        if (!isAuthor && !isAdmin) {
            throw new DeleteForbiddenException("only the author or ROLE_ADMIN may delete comment " + id);
        }
        comment.softDelete(auth.getName(), Instant.now(clock));
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public CommentResponse get(UUID id) {
        return CommentResponse.from(commentRepository.findById(id)
            .orElseThrow(() -> new CommentNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public CommentListResponse byEntity(String entityType, String entityId) {
        List<Comment> rows = commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        List<CommentResponse> items = rows.stream().map(CommentResponse::from).toList();
        return new CommentListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public CommentHistoryResponse history(Authentication auth, UUID id) {
        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            throw new CommentNotFoundException(id);
        }
        boolean isAuthor = comment.getAuthorUserId().equals(auth.getName());
        boolean isAdmin = isAdmin(auth);
        if (!isAuthor && !isAdmin) {
            // COMMENT-HISTORY-002: 404 (not 403) for IDOR safety
            throw new CommentNotFoundException(id);
        }
        List<CommentEditResponse> edits = editRepository.findByCommentIdOrderByEditedAtAsc(id).stream()
            .map(CommentEditResponse::from)
            .toList();
        return new CommentHistoryResponse(id, edits);
    }

    private static boolean isAdmin(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority())) return true;
        }
        return false;
    }
}
