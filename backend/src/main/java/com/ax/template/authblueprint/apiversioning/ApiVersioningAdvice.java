package com.ax.template.authblueprint.apiversioning;

import java.net.URI;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link ApiVersioningException} to RFC 9457 problem+json with the status + stable {@code code}
 * each {@link ApiVersioningException.Kind} declares. {@code basePackages}-scoped + HIGHEST_PRECEDENCE
 * so it claims ONLY this package and maps ONLY {@link ApiVersioningException} — framework exceptions
 * still flow to {@code common.GlobalProblemDetailAdvice}. The {@code type} URI identifies the
 * version-negotiation failure (VERSION-NEGOTIATION-001); the detail is the exception's neutral
 * message, which never leaks build ids / hostnames / stack traces (VERSION-DISCOVERY-001).
 *
 * <p>Spec: specs/api-versioning-l0.yaml.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.apiversioning")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersioningAdvice {

    @ExceptionHandler(ApiVersioningException.class)
    public ResponseEntity<ProblemDetail> handle(ApiVersioningException ex) {
        ApiVersioningException.Kind kind = ex.kind();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(kind.status, ex.getMessage());
        pd.setType(URI.create("https://errors.example.com/api-version-" + kind.code.toLowerCase(Locale.ROOT).replace('_', '-')));
        pd.setTitle(kind.code);
        pd.setProperty("code", kind.code);
        return ResponseEntity.status(kind.status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
