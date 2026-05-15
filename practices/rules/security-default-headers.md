---
title: Keep Spring Security's default response headers enabled
impact: HIGH
impactDescription: "Disabling the header chain opens MIME sniffing + clickjacking + cleartext fallback"
tags:
  - security
  - headers
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-SECURITY-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-SECURITY-003
upstream:
  - "https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html"
evidence:
  - upstream_id: spring-security-headers
    section: "Spring Security — Default Security Headers"
    quote: "X-Frame-Options"
  - source_type: external
    citation: "OWASP Secure Headers Project"
    url: "https://owasp.org/www-project-secure-headers/"
---

## Keep Spring Security's default response headers enabled

**Impact: HIGH — Disabling the header chain opens MIME sniffing + clickjacking + cleartext fallback**

Spring Security wires a conservative default set of response headers on every response: `X-Content-Type-Options: nosniff` (stops MIME sniffing), `X-Frame-Options: DENY` / `SAMEORIGIN` (stops clickjacking), `Strict-Transport-Security` (HTTPS enforcement under HTTPS), `Cache-Control` / `Pragma` (prevents caching of authenticated responses), and `X-XSS-Protection`. Each header closes a specific attack class. `.headers(headers -> headers.disable())` turns them all off in one line — typically added during a debugging session and forgotten. The mechanical remedy is to assert two of the cheapest-to-verify headers (`X-Content-Type-Options` and `X-Frame-Options`) on a real HTTP response.

**Incorrect — disabling the entire header chain:**

```java
http
    .headers(headers -> headers.disable())    // drops nosniff, frame-options, HSTS — every default
    ...;
```

**Correct — keep the chain on, customise individual headers only:**

```java
http
    .headers(headers -> headers
        .frameOptions(frame -> frame.sameOrigin())  // override one header explicitly
        // every other default stays applied
    )
    ...;
```

Verification: `./gradlew testPractices --tests "*DefaultHeaders*"` is a `@SpringBootTest(RANDOM_PORT)` that GETs `/actuator/health` and asserts `X-Content-Type-Options: nosniff` and `X-Frame-Options: (SAMEORIGIN|DENY)` are present on the response.

Reference: [Spring Security — Default Security Headers](https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html) · [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
