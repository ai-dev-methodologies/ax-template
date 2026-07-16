package com.ax.template.authblueprint.idempotency;

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
 * Maps the idempotency reference surface's 400 key signals to RFC 9457 problem+json.
 * {@code basePackages}-scoped + HIGHEST_PRECEDENCE so it claims ONLY this package; it maps the two
 * specific key exceptions plus the body-constraint rejection and NOTHING broader (no
 * {@code Exception.class} catch-all) so framework exceptions still flow to
 * {@code common.GlobalProblemDetailAdvice}.
 *
 * <p>Spec: specs/idempotency-l0.yaml#IDEMPOTENCY-KEY-001 / -SCOPE-001 / -PAYLOAD-001.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.idempotency")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdempotencyAdvice {

    private static final URI KEY_INVALID_TYPE = URI.create("https://errors.example.com/idempotency-key-invalid");
    private static final URI KEY_NOT_ALLOWED_TYPE = URI.create("https://errors.example.com/idempotency-key-not-allowed");
    private static final URI BODY_TOO_LARGE_TYPE = URI.create("https://errors.example.com/request-body-too-large");

    @ExceptionHandler(IdempotencyKeyInvalidException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(IdempotencyKeyInvalidException ex) {
        return badRequest(KEY_INVALID_TYPE, "Invalid Idempotency-Key", IdempotencyKeyInvalidException.CODE, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleNotAllowed(IdempotencyKeyNotAllowedException ex) {
        return badRequest(KEY_NOT_ALLOWED_TYPE, "Idempotency-Key Not Allowed", IdempotencyKeyNotAllowedException.CODE, ex.getMessage());
    }

    /**
     * A body that trips the streaming constraints is rejected with 413. The detail is generic —
     * the (abusive) body is NEVER echoed back (response-amplification defense).
     */
    @ExceptionHandler(RequestBodyConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleBodyConstraint(RequestBodyConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "The request body exceeds the maximum allowed size.");
        pd.setType(BODY_TOO_LARGE_TYPE);
        pd.setTitle("Request body too large");
        pd.setProperty("code", RequestBodyConstraintViolationException.CODE);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private static ResponseEntity<ProblemDetail> badRequest(URI type, String title, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setProperty("code", code);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
