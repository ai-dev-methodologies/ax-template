/**
 * @ax-template-meta
 * template_id: backend/error/GlobalExceptionHandler
 * layer: backend-cross-cutting
 * anchors_rule: error-controller-advice.md (PRACTICES-ERR-001)
 *               error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "RFC 7807 Problem Details for HTTP APIs"
 *     url: "https://datatracker.ietf.org/doc/html/rfc7807"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @ControllerAdvice / @RestControllerAdvice"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @ExceptionHandler"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Extend or adapt the @ExceptionHandler methods for domain-specific exceptions.
 *   Use ProblemDetailFactory for all error response construction.
 */
package com.example.app.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised RFC 7807 exception handler.
 *
 * <p>All application exceptions are translated here to {@link ProblemDetail} bodies via
 * {@link ProblemDetailFactory}. Handlers are ordered from most-specific to least-specific.
 *
 * <p>Pattern: extends {@link ResponseEntityExceptionHandler} so Spring MVC's built-in
 * exception mappings (MethodArgumentNotValidException, TypeMismatchException, etc.) are
 * also translated to ProblemDetail automatically.
 *
 * <p>Rule reference: PRACTICES-ERR-001 (centralised advice), PRACTICES-ERR-002 (RFC 7807).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // Spring MVC validation (MethodArgumentNotValidException)
    // -------------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        String detail = "Validation failed: " + String.join(", ", fieldErrors);
        ProblemDetail pd = ProblemDetailFactory.of(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "validation-error",
                "Validation Error",
                detail,
                servletRequest);
        pd.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    // -------------------------------------------------------------------------
    // Bean validation (ConstraintViolationException from @Validated)
    // -------------------------------------------------------------------------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<String> violations = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());

        String detail = "Constraint violation: " + String.join(", ", violations);
        ProblemDetail pd = ProblemDetailFactory.of(
                HttpStatus.BAD_REQUEST, "constraint-violation",
                "Constraint Violation", detail, request);
        pd.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(pd);
    }

    // -------------------------------------------------------------------------
    // Security exceptions
    // -------------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetailFactory.unauthorized(ex.getMessage(), request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ProblemDetailFactory.forbidden("You do not have permission for this operation.", request));
    }

    // -------------------------------------------------------------------------
    // Domain exceptions — add @ExceptionHandler methods per domain exception
    // -------------------------------------------------------------------------
    // Example:
    //
    // @ExceptionHandler(ItemNotFoundException.class)
    // public ResponseEntity<ProblemDetail> handleItemNotFound(
    //         ItemNotFoundException ex, HttpServletRequest request) {
    //     return ResponseEntity.status(HttpStatus.NOT_FOUND)
    //             .body(ProblemDetailFactory.notFound(ex.getMessage(), request));
    // }

    // -------------------------------------------------------------------------
    // Fallback — catch-all
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(ProblemDetailFactory.internalError(request));
    }
}
