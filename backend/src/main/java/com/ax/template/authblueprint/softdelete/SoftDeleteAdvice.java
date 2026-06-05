package com.ax.template.authblueprint.softdelete;

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
 * Maps {@link SoftDeleteConflictException} (unique / restore-window / not-deleted) to RFC 9457
 * problem+json with HTTP 409. {@code basePackages}-scoped + HIGHEST_PRECEDENCE; maps ONLY this one
 * exception (no {@code Exception.class} catch-all), so 404 (ResourceNotFoundException) and framework
 * exceptions still flow to {@code common.GlobalProblemDetailAdvice}.
 *
 * <p>Spec: specs/soft-delete-l0.yaml#SOFTDELETE-UNIQUE-001 / -RESTORE-001.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.softdelete")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SoftDeleteAdvice {

    private static final URI CONFLICT_TYPE = URI.create("https://errors.example.com/soft-delete-conflict");

    @ExceptionHandler(SoftDeleteConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(SoftDeleteConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(CONFLICT_TYPE);
        pd.setTitle("Soft-Delete Conflict");
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
