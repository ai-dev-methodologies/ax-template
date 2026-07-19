package com.ax.template.authblueprint.featureflags;

import com.ax.template.authblueprint.auditlog.Audited;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin CRUD surface for feature flags.
 * <p>
 * SecurityConfig pins {@code /api/v1/admin/**} to {@code ROLE_ADMIN}
 * (FF-AUTHZ-001/002); this controller ALSO declares a class-level
 * {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} as defense-in-depth (method
 * security is the primary, locally-verifiable gate; the path matcher stays as a
 * complementary layer).
 * <p>
 * Trace:
 * <ul>
 *   <li>FF-CRUD-001..004 — create / list / patch / delete endpoints.</li>
 *   <li>FF-VALID-001/002 — Bean Validation on {@link FeatureFlagDto.CreateRequest}
 *       and {@link FeatureFlagDto.UpdateRequest}; field errors are surfaced as
 *       RFC 7807 {@link ProblemDetail}.</li>
 *   <li>blueprints/feature-flags-manifest.yaml#crud</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class FeatureFlagAdminController {

    public static final String NOT_FOUND_TYPE     = "https://ax-template.dev/problems/feature-flag-not-found";
    public static final String DUPLICATE_TYPE     = "https://ax-template.dev/problems/feature-flag-duplicate";
    public static final String VALIDATION_TYPE    = "https://ax-template.dev/problems/feature-flag-validation";

    private final FeatureFlagService service;

    public FeatureFlagAdminController(FeatureFlagService service) {
        this.service = service;
    }

    @PostMapping
    @Audited(action = "CREATE", resourceType = "feature_flag")
    public ResponseEntity<FeatureFlagDto.FlagResponse> create(
        @RequestBody @Valid FeatureFlagDto.CreateRequest req) {

        FeatureFlag flag = service.create(req.name(), req.enabled(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(FeatureFlagDto.FlagResponse.from(flag));
    }

    @GetMapping
    public FeatureFlagDto.FlagPage list(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size) {

        Page<FeatureFlag> p = service.list(PageRequest.of(page, size));
        List<FeatureFlagDto.FlagResponse> items = p.getContent().stream()
            .map(FeatureFlagDto.FlagResponse::from)
            .toList();
        return new FeatureFlagDto.FlagPage(items, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    @PatchMapping("/{name}")
    @Audited(action = "UPDATE", resourceType = "feature_flag")
    public FeatureFlagDto.FlagResponse update(
        @PathVariable String name,
        @RequestBody @Valid FeatureFlagDto.UpdateRequest req) {

        FeatureFlag flag = service.update(name, req.enabled(), req.description());
        return FeatureFlagDto.FlagResponse.from(flag);
    }

    @DeleteMapping("/{name}")
    @Audited(action = "DELETE", resourceType = "feature_flag")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    // ─── error mapping ───────────────────────────────────────────────────────

    @ExceptionHandler(FeatureFlagNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(FeatureFlagNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(NOT_FOUND_TYPE));
        pd.setTitle("Feature flag not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(DuplicateFeatureFlagException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateFeatureFlagException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(DUPLICATE_TYPE));
        pd.setTitle("Duplicate feature flag");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create(VALIDATION_TYPE));
        pd.setTitle("Validation failed");
        return ResponseEntity.badRequest().body(pd);
    }
}
