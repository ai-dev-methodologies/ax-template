---
title: "Every ProblemDetail error response must include a traceId property"
rule_id: traceid-in-error-response
impact: HIGH
impactDescription: "Without traceId in the error body, callers cannot correlate a 4xx/5xx response with the server's structured log entry"
tags:
  - observability
  - error
  - tracing
  - rfc-7807
provenance_class: internal_design
protects_template_id: templates/backend/global-exception-handler/GlobalExceptionHandler.java
failing_fixture_path: practices/evals/fixtures/traceid-in-error-response/fail_no_traceid/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-001"
verification:
  gradle_task: testPractices
  notes: "Assert ProblemDetail response body for every 4xx/5xx handler contains a non-null 'traceId' property."
evidence:
  - upstream_id: rfc-7807
    section: "Problem Details for HTTP APIs — extension members"
    quote: "Problem Details"
  - upstream_id: slf4j-mdc
    section: "SLF4J Mapped Diagnostic Context (MDC)"
    quote: "Mapped Diagnostic Context"
  - source_type: external
    citation: "RFC 7807 §3.2 — Extension Members: problem detail objects may extend the base format with additional properties to aid debugging"
    url: "https://www.rfc-editor.org/rfc/rfc7807#section-3.2"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OpenTelemetry Trace Context W3C Specification — trace-id propagation for cross-service correlation"
    url: "https://www.w3.org/TR/trace-context/#trace-id"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Every ProblemDetail error response must include a `traceId` property

**Impact: HIGH — An error body without `traceId` is a dead-end for the caller: they receive a 4xx/5xx but have no handle to find the correlated server log entry.**

RFC 7807 defines a standard error envelope (`application/problem+json`) and explicitly permits extension members. The `traceId` extension member closes the loop between client error UI and server structured logs: when a user reports an error, support can use the displayed `traceId` to pull the exact log line from the SIEM without asking for reproduction steps.

The `traceId` value is sourced from the SLF4J MDC key `trace_id` (populated by the `TraceIdFilter` per `observability-mdc-trace-propagation.md`). If the MDC key is absent (e.g., unit tests), fall back to a synthetic `no-trace` sentinel.

**Incorrect — ProblemDetail returned without `traceId`:**

```java
@ExceptionHandler(IllegalArgumentException.class)
public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setTitle("Validation Error");
    // VIOLATION: no traceId — caller cannot correlate this error with server logs
    return pd;
}
```

**Correct — `traceId` from MDC attached to every error response:**

```java
@ExceptionHandler(IllegalArgumentException.class)
public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setTitle("Validation Error");
    pd.setProperty("traceId", traceId());   // ← required
    return pd;
}

@ExceptionHandler(RuntimeException.class)
public ProblemDetail handleRuntime(RuntimeException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    pd.setProperty("traceId", traceId());   // ← required on 5xx especially
    return pd;
}

private static String traceId() {
    String id = MDC.get("trace_id");
    return id != null ? id : "no-trace";
}
```

## Why this matters

- Every `@ExceptionHandler` method in `GlobalExceptionHandler` is a potential terminal point for a user-visible error. Without `traceId`, the client-side error boundary has no correlation data — the support team must rely on approximate timestamps, which is unreliable when multiple users hit the same endpoint.
- The `traceId` from MDC is set by the `TraceIdFilter` on every inbound request (see `observability-mdc-trace-propagation.md`). Forwarding it in the error response is a zero-overhead operation.
- Pairs with `traceid-propagated-client.md` in `practices-react/` which requires Server Actions to propagate `traceId` on their error path.

## Failing fixture

See: `practices/evals/fixtures/traceid-in-error-response/fail_no_traceid/GlobalExceptionHandler.java` — both `@ExceptionHandler` methods return `ProblemDetail` without calling `pd.setProperty("traceId", ...)`. Guard catches: response body missing `traceId` key.

Reference: [RFC 7807 §3.2 — Problem Details for HTTP APIs: Extension Members](https://www.rfc-editor.org/rfc/rfc7807#section-3.2)

Reference: [W3C Trace Context — trace-id propagation](https://www.w3.org/TR/trace-context/#trace-id)
