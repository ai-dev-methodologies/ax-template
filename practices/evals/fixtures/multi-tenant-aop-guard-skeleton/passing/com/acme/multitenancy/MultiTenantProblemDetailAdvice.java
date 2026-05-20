package com.acme.multitenancy;

import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#aop-guard.advice_scope
 * with <root> = acme.
 *
 * Global scope (no basePackages) + HIGHEST_PRECEDENCE + 100 — required
 * to catch TenantBoundaryViolationException thrown from any business
 * domain and to win over per-domain advices that may handle the same
 * exception type differently in their fallback handlers.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MultiTenantProblemDetailAdvice {

    private static final URI TENANT_BOUNDARY_TYPE =
        URI.create("https://errors.example.com/tenant-boundary");
    private static final URI TENANT_CONTEXT_MISSING_TYPE =
        URI.create("https://errors.example.com/tenant-context-missing");

    @ExceptionHandler(TenantBoundaryViolationException.class)
    public ResponseEntity<ProblemDetail> handleBoundary(TenantBoundaryViolationException ex) {
        // 404 — never 403. Existence-leakage prevention is the whole point.
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
        pd.setType(TENANT_BOUNDARY_TYPE);
        pd.setTitle("Resource Not Found");
        // Detail message stays generic — never include tenant_id or resource id.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ProblemDetail> handleMissingContext(TenantContextMissingException ex) {
        // 500 not 404 — this is a server bug (async boundary lost context),
        // not a client authz failure. Distinct ProblemDetail type so ops can
        // alert on it separately.
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        pd.setType(TENANT_CONTEXT_MISSING_TYPE);
        pd.setTitle("Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
