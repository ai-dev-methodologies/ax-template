package com.ax.template.authblueprint.webhooksigning;

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
 * Maps {@link WebhookSigningException} to RFC 9457 problem+json with the status + stable {@code code}
 * each {@link WebhookSigningException.Kind} declares. {@code basePackages}-scoped + HIGHEST_PRECEDENCE
 * so it claims ONLY this package and maps ONLY {@link WebhookSigningException} — framework exceptions
 * still flow to {@code common.GlobalProblemDetailAdvice}. The detail is the exception's neutral
 * message, which by construction never carries the secret, the raw signature, or the body
 * (WHSIGN-SECRET-001).
 *
 * <p>Spec: specs/webhook-signing-l0.yaml.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.webhooksigning")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebhookSigningAdvice {

    @ExceptionHandler(WebhookSigningException.class)
    public ResponseEntity<ProblemDetail> handle(WebhookSigningException ex) {
        WebhookSigningException.Kind kind = ex.kind();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(kind.status, ex.getMessage());
        pd.setType(URI.create("https://errors.example.com/webhook-signing-" + kind.code.toLowerCase(Locale.ROOT).replace('_', '-')));
        pd.setTitle(kind.code);
        pd.setProperty("code", kind.code);
        return ResponseEntity.status(kind.status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
