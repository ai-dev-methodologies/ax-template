---
title: Disable CSRF only for bearer-token paths, never globally
impact: HIGH
impactDescription: "Global csrf().disable() weakens every browser-driven endpoint, present and future"
tags:
  - security
  - csrf
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-SECURITY-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-SECURITY-002
upstream:
  - "https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html"
evidence:
  - upstream_id: spring-security-csrf
    section: "Spring Security — Configuring CSRF Protection (ignoringRequestMatchers)"
    quote: 'ignoringRequestMatchers("/api/*")'
  - source_type: external
    citation: "OWASP Cross-Site Request Forgery Prevention Cheat Sheet"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html"
---

## Disable CSRF only for bearer-token paths, never globally

**Impact: HIGH — Global csrf().disable() weakens every browser-driven endpoint, present and future**

Spring Security's CSRF protection defends cookie-borne sessions from cross-origin POSTs. A JWT-style API authenticates from the `Authorization` header — no cookie, no CSRF surface — so it is correct to disable CSRF for those paths. It is NOT correct to disable CSRF globally. The same `SecurityFilterChain` typically serves h2-console, /actuator endpoints, future server-rendered pages, future form submissions; `csrf().disable()` strips the protection from every one of them. The mechanical remedy is `csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", ...))` — bearer paths bypass, everything else stays defended.

**Incorrect — global CSRF disable:**

```java
http
    .csrf(csrf -> csrf.disable())   // weakens h2-console, /actuator, every future browser endpoint
    .authorizeHttpRequests(...)
    ...;
```

**Correct — scoped CSRF ignore for bearer-token paths only:**

```java
http
    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/practices/demo/**"))
    .authorizeHttpRequests(...)
    ...;
```

Verification: `./gradlew testPractices --tests "*CsrfScopedDisable*"` reads `SecurityConfig.java` and asserts it contains `ignoringRequestMatchers` and does NOT contain the global `csrf().disable()` pattern.

Reference: [Spring Security — CSRF Protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) · [OWASP CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
