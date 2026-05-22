package com.ax.template.authblueprint.commentthread;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

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

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(Authentication auth,
                                                  @Valid @RequestBody CreateCommentRequest body) {
        CommentResponse resp = service.create(auth.getName(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/comments/" + resp.id()))
            .body(resp);
    }

    @GetMapping("/{id}")
    public CommentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CommentResponse edit(Authentication auth, @PathVariable UUID id,
                                @Valid @RequestBody UpdateCommentRequest body) {
        return service.edit(auth, id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        service.delete(auth, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    public CommentListResponse byEntity(@PathVariable String entityType,
                                        @PathVariable String entityId) {
        return service.byEntity(entityType, entityId);
    }

    @GetMapping("/{id}/history")
    public CommentHistoryResponse history(Authentication auth, @PathVariable UUID id) {
        return service.history(auth, id);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(CommentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ParentCommentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleParentNotFound(ParentCommentNotFoundException ex) {
        return problem(HttpStatus.BAD_REQUEST, "PARENT_COMMENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(CrossEntityReplyException.class)
    public ResponseEntity<ProblemDetail> handleCrossEntity(CrossEntityReplyException ex) {
        return problem(HttpStatus.BAD_REQUEST, "CROSS_ENTITY_REPLY", ex.getMessage());
    }

    @ExceptionHandler(EditForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleEditForbidden(EditForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "EDIT_FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(DeleteForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleDeleteForbidden(DeleteForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "DELETE_FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation failed");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
