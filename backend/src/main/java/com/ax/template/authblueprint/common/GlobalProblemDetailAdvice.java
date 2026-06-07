package com.ax.template.authblueprint.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared cross-cutting RFC 9457 {@code application/problem+json} fallback for the
 * COMMON framework exceptions every HTTP domain would otherwise have to re-handle.
 *
 * <p>IMW1-B (IDW1 dogfood 2026-05-29): the only previously-global advice
 * ({@code practices.PracticesProblemDetailAdvice}) is {@code basePackages}-scoped to
 * {@code practices}, so every other domain either hand-rolled its own
 * {@link MethodArgumentNotValidException} → 400 handler (drift + one real bug across
 * 3/3 personas) or let {@code @Valid} failures fall through to a misleading Spring
 * Security {@code 403} at {@code /error}. This advice closes that hole once, in
 * {@code common}, as a REUSABLE fallback so a fork-receiver inherits a uniform
 * problem+json error contract for the framework exceptions below.
 *
 * <h2>Precedence — fallback only, never an override</h2>
 * This advice is annotated {@link Order @Order(Ordered.LOWEST_PRECEDENCE)} so it loses
 * to:
 * <ul>
 *   <li>any controller-local {@code @ExceptionHandler} (absolute precedence for its
 *       own controller — e.g. {@code ApprovalController}, {@code ReportExportController});</li>
 *   <li>any {@code basePackages}-scoped {@code @ControllerAdvice} for its own package
 *       (e.g. {@code PracticesProblemDetailAdvice} keeps mapping
 *       {@code MethodArgumentNotValidException} inside {@code practices};
 *       {@code PaymentExceptionHandler} keeps mapping
 *       {@code HttpMessageNotReadableException} inside {@code payment}).</li>
 * </ul>
 * Because Spring picks the highest-priority applicable advice, a more-specific
 * domain advice always wins; this fallback handles only requests no one else claims.
 *
 * <h2>Scope — framework exceptions only</h2>
 * It deliberately handles ONLY the framework exceptions domains should not have to
 * re-handle PLUS the two COMMON cross-cutting signals shipped in this package
 * ({@link ResourceNotFoundException}, {@link InvalidPageRequestException}); it does NOT
 * register a catch-all {@code Exception} handler, nor a broad
 * {@code IllegalArgumentException} handler (which would mask genuine programming bugs),
 * so it never masks a domain's own business exceptions.
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} + {@link ConstraintViolationException}
 *       → 400 with the shared {@code errors[]} extension array
 *       (aligns with {@code problem-details-l0} PROBLEM-VALIDATION-001 +
 *       {@code request-validation-l0} VALIDATION-ERROR-001);</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (unreadable / malformed body);</li>
 *   <li>{@link HttpMediaTypeNotSupportedException} → 415;</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} → 405;</li>
 *   <li>{@link ResourceNotFoundException} → 404 (code {@code NOT_FOUND}) — the
 *       IDOR-safe-404 primitive. {@code @ResponseStatus(404)} alone is a TRAP under the
 *       reference {@code SecurityConfig}: {@code sendError(404)} re-dispatches to
 *       {@code /error}, which re-enters the filter chain and is caught by
 *       {@code anyRequest().denyAll()} → a misleading 403. This handler returns the
 *       404 DIRECTLY (no re-dispatch), closing that trap;</li>
 *   <li>{@link InvalidPageRequestException} → 400 (code {@code PAGE_SIZE_INVALID}) —
 *       the typed out-of-range page-request signal from
 *       {@link OffsetPageSupport#clamp(int, int, int)} (same 403 trap if left
 *       unmapped).</li>
 *   <li>{@link OptimisticLockingSupport.PreconditionRequiredException} → 428
 *       (code {@code PRECONDITION_REQUIRED}) — conditional write arrived without an
 *       {@code If-Match} header (RFC 6585 §3);</li>
 *   <li>{@link OptimisticLockingSupport.PreconditionFailedException} → 412
 *       (code {@code PRECONDITION_FAILED}) — supplied {@code If-Match} validator is stale
 *       (RFC 9110 §15.5.13); carries the authoritative {@code current_etag} member;</li>
 *   <li>{@link ObjectOptimisticLockingFailureException} → 409
 *       (code {@code OPTIMISTIC_LOCK_CONFLICT}) — a {@code @Version} bump lost the race at
 *       flush time (RFC 9110 §15.5.10). These three close the IDW4 hole where every
 *       {@link OptimisticLockingSupport} adopter hand-rolled the same mappings.</li>
 * </ul>
 * Each body carries a stable {@code code} extension member so clients branch on
 * {@code code} (not free-text {@code detail}).
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalProblemDetailAdvice {

    private static final URI VALIDATION_TYPE = URI.create("https://errors.example.com/validation");
    private static final URI MALFORMED_BODY_TYPE = URI.create("https://errors.example.com/malformed-request-body");
    private static final URI UNSUPPORTED_MEDIA_TYPE = URI.create("https://errors.example.com/unsupported-media-type");
    private static final URI METHOD_NOT_ALLOWED_TYPE = URI.create("https://errors.example.com/method-not-allowed");
    private static final URI NOT_FOUND_TYPE = URI.create("https://errors.example.com/not-found");
    private static final URI PAGE_SIZE_INVALID_TYPE = URI.create("https://errors.example.com/page-size-invalid");
    private static final URI PRECONDITION_REQUIRED_TYPE =
            URI.create(OptimisticLockingSupport.TYPE_PRECONDITION_REQUIRED);
    private static final URI PRECONDITION_FAILED_TYPE =
            URI.create(OptimisticLockingSupport.TYPE_PRECONDITION_FAILED);
    private static final URI OPTIMISTIC_LOCK_CONFLICT_TYPE =
            URI.create("urn:problem:optimistic-lock-conflict");
    private static final URI LOCK_CONFLICT_TYPE =
            URI.create("urn:problem:lock-conflict");
    private static final URI CONSENT_REQUIRED_TYPE =
            URI.create("https://errors.example.com/consent-required");

    /**
     * {@code @Valid}/{@code @Validated} body-binding failures. Reports EVERY field +
     * object error in a single {@code errors[]} array (no fail-fast). Each entry
     * carries {@code field} (task contract) plus the spec-canonical {@code pointer}
     * (RFC 6901 JSON Pointer) / {@code name}, a stable {@code code} (the failed
     * constraint, e.g. {@code NotBlank}), and {@code message} / {@code detail}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(errorEntry(fe.getField(), constraintCode(fe.getCode()), defaultMessage(fe.getDefaultMessage())));
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                errors.add(errorEntry(ge.getObjectName(), constraintCode(ge.getCode()), defaultMessage(ge.getDefaultMessage()))));
        return validationProblem(errors);
    }

    /**
     * {@code @Validated} method-parameter / path / query constraint failures
     * (Bean Validation raised outside body binding).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        if (ex.getConstraintViolations() != null) {
            for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
                errors.add(errorEntry(leafField(cv.getPropertyPath()), constraintCode(annotationCode(cv)), defaultMessage(cv.getMessage())));
            }
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, MALFORMED_BODY_TYPE, "Malformed Request Body",
                "MALFORMED_REQUEST_BODY", "Request body is missing or could not be parsed.");
        return entity(HttpStatus.BAD_REQUEST, pd);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        ProblemDetail pd = problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "UNSUPPORTED_MEDIA_TYPE", "The request Content-Type is not supported by this endpoint.");
        return entity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail pd = problem(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_TYPE, "Method Not Allowed",
                "METHOD_NOT_ALLOWED", "The HTTP method is not supported by this endpoint.");
        return entity(HttpStatus.METHOD_NOT_ALLOWED, pd);
    }

    /**
     * COMMON IDOR-safe 404. Returns the {@code 404} DIRECTLY (no {@code sendError}
     * re-dispatch), so {@link ResourceNotFoundException} surfaces as
     * {@code 404 application/problem+json} instead of the misleading {@code 403} the
     * {@code @ResponseStatus} + {@code /error} re-entry produces under the reference
     * {@code SecurityConfig}. Carries the exception's neutral not-found message so the
     * existence of another caller's resource is never leaked (see
     * {@link ResourceNotFoundException}).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = problem(HttpStatus.NOT_FOUND, NOT_FOUND_TYPE, "Not Found",
                "NOT_FOUND", defaultMessage(ex.getMessage()));
        return entity(HttpStatus.NOT_FOUND, pd);
    }

    /**
     * COMMON out-of-range page request → {@code 400} (code {@code PAGE_SIZE_INVALID}).
     * Mirrors the {@link ResourceNotFoundException} handler: returning the response
     * directly avoids the {@code /error} re-dispatch that would otherwise turn the
     * unmapped {@link InvalidPageRequestException} into a {@code 403}. Deliberately
     * narrow — only the typed {@link InvalidPageRequestException} is mapped, NOT a broad
     * {@link IllegalArgumentException}, so genuine programming bugs are not masked.
     */
    @ExceptionHandler(InvalidPageRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPageRequest(InvalidPageRequestException ex) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, PAGE_SIZE_INVALID_TYPE, "Invalid Page Request",
                "PAGE_SIZE_INVALID", defaultMessage(ex.getMessage()));
        return entity(HttpStatus.BAD_REQUEST, pd);
    }

    /**
     * COMMON conditional-write {@code If-Match} absent → {@code 428 Precondition Required}
     * (RFC 6585 §3). Raised by {@link OptimisticLockingSupport#requireMatch(String, String, long)}
     * when a mutation arrives without an {@code If-Match} validator. Mirrors the
     * {@link ResourceNotFoundException} handler (returns the response directly, no
     * {@code /error} re-dispatch). Closes the hole the first regulated consumer hit:
     * {@code OptimisticLockingSupport} raises this signal but no global advice mapped it,
     * so every adopter hand-rolled the same 3 ProblemDetail mappings.
     */
    @ExceptionHandler(OptimisticLockingSupport.PreconditionRequiredException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionRequired(
            OptimisticLockingSupport.PreconditionRequiredException ex) {
        ProblemDetail pd = problem(HttpStatus.PRECONDITION_REQUIRED, PRECONDITION_REQUIRED_TYPE,
                "Precondition Required", "PRECONDITION_REQUIRED", defaultMessage(ex.getMessage()));
        return entity(HttpStatus.PRECONDITION_REQUIRED, pd);
    }

    /**
     * COMMON stale conditional-write validator → {@code 412 Precondition Failed}
     * (RFC 9110 §15.5.13). Raised by {@link OptimisticLockingSupport#requireMatch(String, String, long)}
     * when the supplied {@code If-Match} validator no longer matches the current version.
     * Carries the authoritative {@code current_etag} extension member so the client can
     * re-GET, merge, and retry (spec OPTLOCK-RETRY-001).
     */
    @ExceptionHandler(OptimisticLockingSupport.PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailed(
            OptimisticLockingSupport.PreconditionFailedException ex) {
        ProblemDetail pd = problem(HttpStatus.PRECONDITION_FAILED, PRECONDITION_FAILED_TYPE,
                "Precondition Failed", "PRECONDITION_FAILED", defaultMessage(ex.getMessage()));
        if (ex.currentEtag() != null) {
            pd.setProperty("current_etag", ex.currentEtag());
        }
        return entity(HttpStatus.PRECONDITION_FAILED, pd);
    }

    /**
     * COMMON concurrent-write conflict → {@code 409 Conflict} (RFC 9110 §15.5.10).
     * The persistence provider raises {@link ObjectOptimisticLockingFailureException} when
     * a {@code @Version} bump loses the race at flush time (two writers passed the
     * {@code If-Match} check, then both flushed). Returns the response directly to avoid the
     * {@code /error} re-dispatch trap; carries no row-existence detail so a neutral
     * "concurrent write" message is surfaced rather than persistence internals.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockConflict(
            ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, OPTIMISTIC_LOCK_CONFLICT_TYPE,
                "Conflict", "OPTIMISTIC_LOCK_CONFLICT",
                "The resource was modified concurrently; re-read and retry.");
        return entity(HttpStatus.CONFLICT, pd);
    }

    /**
     * A PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) waiter that could not acquire the row lock within the
     * configured timeout raises {@link PessimisticLockingFailureException} (e.g. CannotAcquireLockException).
     * Map it to a deterministic, retryable 409 — every {@code @Lock(PESSIMISTIC_WRITE)} adopter (dispatch,
     * costshare, reservation, register, netting, governedrecord) gets a clean conflict instead of a raw 500.
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handlePessimisticLockConflict(
            PessimisticLockingFailureException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, LOCK_CONFLICT_TYPE,
                "Conflict", "LOCK_CONFLICT",
                "Could not acquire the row lock under contention; retry the request.");
        return entity(HttpStatus.CONFLICT, pd);
    }

    /**
     * COMMON purpose-gated consent failure → {@code 403 Forbidden}
     * (code {@code CONSENT_REQUIRED}). Raised by
     * {@link ConsentGate#requireConsent(String, String, java.util.List)} when a
     * purpose-gated operation is attempted without an active consent grant
     * (spec {@code consent-management-l0#CONSENT-PURPOSE-001}). Returns the response
     * directly (no {@code /error} re-dispatch); carries the absent {@code purpose}
     * as an extension member so a client knows which grant to capture, without
     * leaking any subject identity.
     */
    @ExceptionHandler(ConsentGate.ConsentRequiredException.class)
    public ResponseEntity<ProblemDetail> handleConsentRequired(ConsentGate.ConsentRequiredException ex) {
        ProblemDetail pd = problem(HttpStatus.FORBIDDEN, CONSENT_REQUIRED_TYPE, "Consent Required",
                ConsentGate.ConsentRequiredException.CODE, defaultMessage(ex.getMessage()));
        if (ex.purpose() != null) {
            pd.setProperty("purpose", ex.purpose());
        }
        return entity(HttpStatus.FORBIDDEN, pd);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ResponseEntity<ProblemDetail> validationProblem(List<Map<String, String>> errors) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE, "Validation Failed",
                "VALIDATION_FAILED", "One or more fields failed validation.");
        pd.setProperty("errors", errors);
        return entity(HttpStatus.BAD_REQUEST, pd);
    }

    private static ProblemDetail problem(HttpStatusCode status, URI type, String title, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setProperty("code", code);
        // problem-details-l0 PROBLEM-FORMAT-001: the fifth RFC 9457 member, identifying THIS
        // occurrence. Sourced from the current request URI so every framework-exception body
        // routed through this fallback carries instance (not only the demo reference advice).
        URI instance = currentRequestInstance();
        if (instance != null) {
            pd.setInstance(instance);
        }
        return pd;
    }

    /** Current request URI as the problem {@code instance}, or null outside a servlet request. */
    private static URI currentRequestInstance() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            try {
                return URI.create(sra.getRequest().getRequestURI());
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static ResponseEntity<ProblemDetail> entity(HttpStatusCode status, ProblemDetail pd) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * One {@code errors[]} entry. Carries {@code field} (task contract), {@code code}
     * (stable machine constraint id), {@code message} / {@code detail} (human-readable,
     * localizable), and the spec-canonical {@code pointer} (RFC 6901 JSON Pointer) +
     * {@code name} locators from problem-details-l0 / request-validation-l0.
     */
    private static Map<String, String> errorEntry(String field, String code, String message) {
        String safe = field == null ? "" : field;
        return Map.of(
                "field", safe,
                "name", safe,
                "pointer", "/" + safe.replace('.', '/'),
                "code", code,
                "message", message,
                "detail", message);
    }

    private static String constraintCode(String code) {
        return (code == null || code.isBlank()) ? "Invalid" : code;
    }

    private static String defaultMessage(String message) {
        return (message == null || message.isBlank()) ? "invalid" : message;
    }

    private static String annotationCode(ConstraintViolation<?> cv) {
        if (cv.getConstraintDescriptor() == null
                || cv.getConstraintDescriptor().getAnnotation() == null) {
            return "Invalid";
        }
        return cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }

    /** Last path segment of a Bean Validation property path (e.g. {@code create.arg0.title} → {@code title}). */
    private static String leafField(Path path) {
        String leaf = "";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                leaf = node.getName();
            }
        }
        return leaf;
    }
}
