package com.ax.template.authblueprint.realtime;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Package-scoped RFC 9457 {@code application/problem+json} mapping for the realtime-policy
 * domain. {@code basePackages}-scoped to {@code realtime} so it NEVER claims another
 * domain's exceptions — additive-only (mirrors {@code i18n.I18nProblemAdvice}).
 *
 * <p>RT-CHANNEL-AUTH-002: a cross-scope subscribe → {@code 403 Forbidden}.
 * <p>RT-OBSERVABILITY-001: a subscribe/publish to an unknown topic → {@code 404 Not Found}.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.realtime")
public class RealtimeProblemAdvice {

    private static final URI CROSS_TENANT_TYPE =
            URI.create("https://errors.example.com/realtime-cross-tenant-subscription");
    private static final URI UNKNOWN_TOPIC_TYPE =
            URI.create("https://errors.example.com/realtime-unknown-topic");

    @ExceptionHandler(CrossTenantSubscriptionException.class)
    public ResponseEntity<ProblemDetail> handleCrossTenant(CrossTenantSubscriptionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(CROSS_TENANT_TYPE);
        pd.setTitle("Cross-Tenant Subscription Rejected");
        pd.setProperty("code", CrossTenantSubscriptionException.CODE);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(UnknownTopicException.class)
    public ResponseEntity<ProblemDetail> handleUnknownTopic(UnknownTopicException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(UNKNOWN_TOPIC_TYPE);
        pd.setTitle("Unknown Realtime Topic");
        pd.setProperty("code", UnknownTopicException.CODE);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
