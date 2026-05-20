package com.ax.template.authblueprint.search;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Search REST surface. Tenant scoping is derived from
 * {@link Authentication#getName()} — clients can NOT pass a tenantId in the URL
 * or body (SEARCH-AUTHZ-002).
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-AUTHZ-001 — SecurityConfig maps {@code /api/v1/search/**} to authenticated()</li>
 *   <li>SEARCH-AUTHZ-002 — caller's tenantId always overrides any client-supplied value</li>
 *   <li>SEARCH-QUERY-001 — POST {@code /api/v1/search} returns {@link SearchDto.SearchResultPage}</li>
 *   <li>SEARCH-QUERY-002 — Korean substrings pass through unchanged</li>
 *   <li>SEARCH-QUERY-003 — blank query → 400 RFC 7807 ProblemDetail</li>
 *   <li>SEARCH-INDEX-001 — POST {@code /api/v1/search/index} → 201</li>
 *   <li>SEARCH-INDEX-002 — DELETE {@code /api/v1/search/index/{id}} → 204</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    /** SEARCH-QUERY-003 — RFC 7807 problem type for validation errors. */
    public static final String VALIDATION_ERROR_TYPE = "https://ax-template.dev/problems/validation-error";

    private final SearchIndexService service;

    public SearchController(SearchIndexService service) {
        this.service = service;
    }

    @PostMapping
    public SearchDto.SearchResultPage search(
        @Valid @RequestBody SearchDto.SearchRequest request,
        Authentication auth
    ) {
        int page = (request.page() == null) ? 0 : request.page();
        int size = (request.size() == null) ? SearchIndexService.DEFAULT_PAGE_SIZE : request.size();
        return service.search(auth.getName(), request.query(), request.domain(), page, size);
    }

    @PostMapping("/index")
    public ResponseEntity<SearchDto.IndexResponse> index(
        @Valid @RequestBody SearchDto.IndexRequest request,
        Authentication auth
    ) {
        UUID id = service.index(
            auth.getName(),
            request.id(),
            request.domain(),
            request.content(),
            request.metadata()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new SearchDto.IndexResponse(id));
    }

    @DeleteMapping("/index/{id}")
    public ResponseEntity<Void> deleteIndex(@PathVariable UUID id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * SEARCH-QUERY-003 — RFC 7807 validation error response. Spring's default
     * {@link MethodArgumentNotValidException} mapping varies across versions;
     * we pin the response to {@link ProblemDetail} so the test contract is
     * deterministic.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("validation failed"));
        pd.setType(URI.create(VALIDATION_ERROR_TYPE));
        pd.setTitle("Validation Error");
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(VALIDATION_ERROR_TYPE));
        pd.setTitle("Validation Error");
        return ResponseEntity.badRequest().body(pd);
    }
}
