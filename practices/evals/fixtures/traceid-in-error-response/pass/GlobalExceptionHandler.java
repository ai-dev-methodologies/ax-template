/**
 * FIXTURE: traceid-in-error-response/pass
 * Demonstrates CORRECT pattern: every ProblemDetail includes a traceId property
 * sourced from SLF4J MDC so callers can correlate client errors with server logs.
 */
package com.example.fixture.traceid_in_error_response;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Validation Error");
        // CORRECT: attach traceId from MDC so the client can quote it in support tickets
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        String id = MDC.get("trace_id");
        return id != null ? id : "no-trace";
    }
}
