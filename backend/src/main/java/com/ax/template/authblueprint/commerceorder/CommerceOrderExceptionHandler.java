package com.ax.template.authblueprint.commerceorder;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 9457 problem+json handler for {@link CommerceOrderException}.
 * Scoped to the commerceorder package so it doesn't intercept other domains.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.commerceorder")
public class CommerceOrderExceptionHandler {

    @ExceptionHandler(CommerceOrderException.class)
    public ResponseEntity<ProblemDetail> handleCommerceOrderException(CommerceOrderException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        pd.setType(ex.type());
        pd.setTitle(ex.getCode().replace('_', ' '));
        pd.setProperty("code", ex.getCode());
        return ResponseEntity.status(ex.getHttpStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
    }
}
