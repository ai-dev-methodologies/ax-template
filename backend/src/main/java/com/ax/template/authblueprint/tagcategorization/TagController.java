package com.ax.template.authblueprint.tagcategorization;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import com.ax.template.authblueprint.tagcategorization.TagDtos.AttachTagRequest;
import com.ax.template.authblueprint.tagcategorization.TagDtos.CreateTagRequest;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagAttachmentResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagListResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.UpdateTagRequest;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TagResponse> create(Authentication auth,
                                              @Valid @RequestBody CreateTagRequest body) {
        TagResponse response = service.create(auth.getName(), body);
        return ResponseEntity.created(URI.create("/api/tags/" + response.id())).body(response);
    }

    @GetMapping
    public TagListResponse list(@RequestParam(name = "parent", required = false) UUID parent) {
        return service.list(parent);
    }

    @GetMapping("/{id}")
    public TagResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TagResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTagRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "false") boolean cascade) {
        service.delete(id, cascade);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attach")
    public ResponseEntity<TagAttachmentResponse> attach(Authentication auth,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody AttachTagRequest body) {
        TagService.AttachResult result = service.attach(auth.getName(), id, body);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @DeleteMapping("/{id}/attach/{entityType}/{entityId}")
    public ResponseEntity<Void> detach(@PathVariable UUID id,
                                       @PathVariable String entityType,
                                       @PathVariable String entityId) {
        service.detach(id, entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    public TagListResponse byEntity(@PathVariable String entityType,
                                    @PathVariable String entityId) {
        return service.byEntity(entityType, entityId);
    }

    // ── Exception → HTTP mapping ─────────────────────────────────────────────

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(TagNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateSlugException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateSlug(DuplicateSlugException ex) {
        return problem(HttpStatus.BAD_REQUEST, "DUPLICATE_SLUG", ex.getMessage());
    }

    @ExceptionHandler(ParentTagNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleParentNotFound(ParentTagNotFoundException ex) {
        return problem(HttpStatus.BAD_REQUEST, "PARENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(TagHasChildrenException.class)
    public ResponseEntity<ProblemDetail> handleHasChildren(TagHasChildrenException ex) {
        return problem(HttpStatus.CONFLICT, "TAG_HAS_CHILDREN", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
