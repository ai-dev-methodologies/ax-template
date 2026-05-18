/**
 * FIXTURE: traceid-in-error-response/fail_no_traceid
 * Demonstrates WRONG pattern: ProblemDetail returned without a traceId field.
 * Guard must catch: ProblemDetail body does not include traceId property.
 * Violates traceid-in-error-response rule.
 */
package com.example.fixture.traceid_in_error_response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        // VIOLATION: ProblemDetail has no traceId — caller cannot correlate this
        // error with server logs.
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Validation Error");
        // missing: pd.setProperty("traceId", MDC.get("trace_id"));
        return pd;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        // VIOLATION: same — no traceId on 500 errors is even more critical
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
