package com.ax.template.authblueprint.secretsmanagement;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link SecretException} to RFC 9457 problem+json with the status + stable {@code code} each
 * {@link SecretException.Kind} declares. {@code basePackages}-scoped + HIGHEST_PRECEDENCE so it
 * claims ONLY this package and maps ONLY {@link SecretException} — framework exceptions still flow to
 * {@code common.GlobalProblemDetailAdvice}. The detail is the exception's neutral message, which by
 * construction never carries the secret value (SECRET-NO-LOG-001).
 *
 * <p>Spec: specs/secrets-management-l0.yaml.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.secretsmanagement")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretAdvice {

    @ExceptionHandler(SecretException.class)
    public ResponseEntity<ProblemDetail> handle(SecretException ex) {
        SecretException.Kind kind = ex.kind();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(kind.status, ex.getMessage());
        pd.setType(URI.create("https://errors.example.com/secret-" + kind.code.toLowerCase().replace('_', '-')));
        pd.setTitle(kind.code);
        pd.setProperty("code", kind.code);
        return ResponseEntity.status(kind.status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
