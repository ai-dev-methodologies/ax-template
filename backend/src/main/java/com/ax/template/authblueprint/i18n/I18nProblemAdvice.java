package com.ax.template.authblueprint.i18n;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Package-scoped RFC 9457 {@code application/problem+json} mapping for the i18n-policy
 * domain. {@code basePackages}-scoped to {@code i18n} so it NEVER claims another domain's
 * exceptions — additive-only (mirrors {@code payment.PaymentExceptionHandler} /
 * {@code practices.PracticesProblemDetailAdvice}).
 *
 * <p>I18N-TIMEZONE-001: a naive (offset-less) inbound date-time → {@code 400
 * INVALID_DATETIME}, not silent coercion to UTC.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.i18n")
public class I18nProblemAdvice {

    private static final URI INVALID_DATETIME_TYPE =
            URI.create("https://errors.example.com/invalid-datetime");

    @ExceptionHandler(I18nTimePolicy.InvalidDateTimeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDateTime(
            I18nTimePolicy.InvalidDateTimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(INVALID_DATETIME_TYPE);
        pd.setTitle("Invalid Date-Time");
        pd.setProperty("code", I18nTimePolicy.INVALID_DATETIME_CODE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
