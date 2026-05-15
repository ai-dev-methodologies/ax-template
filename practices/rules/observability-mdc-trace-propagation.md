---
title: Populate MDC trace_id for every request, clear on exit
impact: MEDIUM
impactDescription: "Without per-request trace id, logs from concurrent requests interleave irrecoverably"
tags:
  - observability
  - mdc
  - tracing
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-002
upstream:
  - "https://www.slf4j.org/manual.html"
evidence:
  - upstream_id: slf4j-mdc
    section: "SLF4J Mapped Diagnostic Context (MDC)"
    quote: "Mapped Diagnostic Context"
  - source_type: external
    citation: "SLF4J Manual §Mapped Diagnostic Context"
    url: "https://www.slf4j.org/manual.html#mdc"
---

## Populate MDC trace_id for every request, clear on exit

**Impact: MEDIUM — Without per-request trace id, logs from concurrent requests interleave irrecoverably**

Servlet containers reuse threads across requests. Without a Mapped Diagnostic Context (MDC) entry pinned to the current request, log lines from request A and request B end up interleaved on the same thread with no way to reconstruct one request's trail. The standard remedy is a filter at the edge that reads `X-Request-Id` from the inbound headers (or mints a UUID when absent), sets `MDC.trace_id`, and *crucially* clears it on exit so the next request on the same thread does not inherit the previous id.

**Incorrect — filter forgets to clear MDC:**

```java
MDC.put("trace_id", id);
chain.doFilter(req, res);   // exception path leaves MDC dirty; next request on this thread keeps the wrong id
```

**Correct — MDC cleared in `finally`:**

```java
String id = Optional.ofNullable(req.getHeader("X-Request-Id"))
        .filter(s -> !s.isBlank())
        .orElse(UUID.randomUUID().toString());
MDC.put("trace_id", id);
try {
    chain.doFilter(req, res);
} finally {
    MDC.remove("trace_id");
}
```

Verification: `./gradlew testPractices --tests "*MdcPropagation*"` invokes the filter with a mock chain and asserts (a) MDC contains the inbound header value during the chain and is null afterward, (b) a UUID is minted when the header is absent.

Reference: [SLF4J Manual — MDC](https://www.slf4j.org/manual.html#mdc)
