/**
 * @ax-template-meta
 * template_id: backend/error/ProblemDetailFactory
 * layer: backend-cross-cutting
 * anchors_rule: error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "RFC 7807 Problem Details for HTTP APIs §3"
 *     url: "https://datatracker.ietf.org/doc/html/rfc7807"
 *   - source_type: external
 *     citation: "Spring Framework 6 ProblemDetail support"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
 * usage: |
 *   Replace 'com.example.app' with your actual base package.
 *   Use ProblemDetailFactory.of(status, type, title, detail, request) in your
 *   GlobalExceptionHandler to produce RFC 7807-compliant error responses.
 */
package com.example.app.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

/**
 * Factory for RFC 7807 {@link ProblemDetail} instances.
 *
 * <p>Every error response from this application must carry:
 * <ul>
 *   <li>{@code type} — a URI identifying the problem class (type URI)
 *   <li>{@code title} — short, human-readable summary
 *   <li>{@code status} — HTTP status code
 *   <li>{@code detail} — human-readable explanation for this occurrence
 *   <li>{@code instance} — URI of the specific request (request path + query)
 *   <li>{@code properties.traceId} — correlation / trace ID for log cross-referencing
 * </ul>
 *
 * <p>Reference: RFC 7807 §3 "Members of a Problem Details Object".
 */
public final class ProblemDetailFactory {

    /** Base URI for problem type identifiers. Override in fork receiver via properties. */
    private static final String TYPE_BASE = "https://api.example.com/problems/";

    private ProblemDetailFactory() {}

    /**
     * Creates a {@link ProblemDetail} with all required RFC 7807 fields populated.
     *
     * @param status  HTTP status for this problem
     * @param type    short type slug, e.g. {@code "validation-error"} — appended to {@link #TYPE_BASE}
     * @param title   short invariant summary, e.g. {@code "Validation Error"}
     * @param detail  human-readable explanation for this specific occurrence
     * @param request current HTTP request (used to populate {@code instance} and {@code traceId})
     * @return fully populated {@link ProblemDetail}
     */
    public static ProblemDetail of(HttpStatus status,
                                   String type,
                                   String title,
                                   String detail,
                                   HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + type));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("traceId", resolveTraceId(request));
        return pd;
    }

    /**
     * Convenience overload without an {@link HttpServletRequest} — for use in tests
     * or contexts where the servlet request is unavailable.
     */
    public static ProblemDetail of(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + type));
        pd.setTitle(title);
        pd.setProperty("traceId", UUID.randomUUID().toString());
        return pd;
    }

    // ---------------------------------------------------------------------------
    // Pre-built factory methods for the most common problem types
    // ---------------------------------------------------------------------------

    public static ProblemDetail notFound(String detail, HttpServletRequest request) {
        return of(HttpStatus.NOT_FOUND, "not-found", "Resource Not Found", detail, request);
    }

    public static ProblemDetail badRequest(String detail, HttpServletRequest request) {
        return of(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request", detail, request);
    }

    public static ProblemDetail unauthorized(String detail, HttpServletRequest request) {
        return of(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", detail, request);
    }

    public static ProblemDetail forbidden(String detail, HttpServletRequest request) {
        return of(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", detail, request);
    }

    public static ProblemDetail conflict(String detail, HttpServletRequest request) {
        return of(HttpStatus.CONFLICT, "conflict", "Conflict", detail, request);
    }

    public static ProblemDetail unprocessable(String detail, HttpServletRequest request) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable-entity",
                "Unprocessable Entity", detail, request);
    }

    public static ProblemDetail tooManyRequests(String detail, HttpServletRequest request) {
        return of(HttpStatus.TOO_MANY_REQUESTS, "too-many-requests",
                "Too Many Requests", detail, request);
    }

    public static ProblemDetail internalError(HttpServletRequest request) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error",
                "An unexpected error occurred. See traceId for details.", request);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static String resolveTraceId(HttpServletRequest request) {
        // Prefer X-Correlation-Id set by CorrelationIdFilter (upstream MDC value).
        // Fall back to a new UUID if absent.
        String correlationId = (String) request.getAttribute("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
