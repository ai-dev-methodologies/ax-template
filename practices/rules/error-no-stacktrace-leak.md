---
title: Error responses must not leak stack-trace or exception class names
impact: HIGH
impactDescription: "Stack traces in client responses are a recurring source of OWASP-API #8 Security Misconfiguration findings"
tags:
  - error
  - security
  - information-disclosure
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ERR-003
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/"
evidence:
  - upstream_id: owasp-api-error-handling
    section: "OWASP API Top 10 (2023) #8 — Security Misconfiguration"
    quote: "Security Misconfiguration"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API8 Security Misconfiguration"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/"
---

## Error responses must not leak stack-trace or exception class names

**Impact: HIGH — Stack traces in client responses are a recurring source of OWASP-API #8 Security Misconfiguration findings**

When `server.error.include-stacktrace=always` is left on (or a controller catches an exception and returns `ex.toString()` / `ex.getStackTrace()`), the response body exposes class FQNs, frame paths, library versions, and sometimes filesystem paths. Attackers use this to fingerprint the stack, look up known CVEs for the listed library versions, and locate handlers worth probing. The error envelope must contain a human-readable `detail` and nothing else internal.

**Incorrect — full stack trace returned to the client:**

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<String> any(Exception ex) {
    var sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    return ResponseEntity.status(500).body(sw.toString()); // leaks frames + class names
}
```

**Correct — log internally, return a sanitised body:**

```java
private static final Logger log = LoggerFactory.getLogger(Advice.class);

@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> any(Exception ex) {
    log.error("unhandled error", ex);   // stack stays in server logs
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    pd.setTitle("Internal Error");
    return ResponseEntity.status(500)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
}
```

Verification: `./gradlew testPractices --tests "*NoStacktraceLeak*"` asserts the response bodies for `/practices/demo/bad` and `/practices/demo/missing` do not contain the markers `java.lang.`, `Exception`, tab-at, `Caused by:`, or `StackTrace`.

Reference: [OWASP API Security Top 10 (2023) — API8 Security Misconfiguration](https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/)
