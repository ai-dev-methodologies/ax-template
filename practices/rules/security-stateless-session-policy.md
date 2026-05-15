---
title: SessionCreationPolicy.STATELESS for JWT / bearer-token APIs
impact: HIGH
impactDescription: "Without STATELESS, successful auth issues a JSESSIONID cookie the API never agreed to manage"
tags:
  - security
  - session
  - jwt
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-SECURITY-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-SECURITY-001
upstream:
  - "https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html"
evidence:
  - upstream_id: spring-security-stateless
    section: "Spring Security — SessionCreationPolicy.STATELESS"
    quote: "STATELESS"
  - source_type: external
    citation: "Spring Security Reference — Session Management"
    url: "https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html"
---

## SessionCreationPolicy.STATELESS for JWT / bearer-token APIs

**Impact: HIGH — Without STATELESS, successful auth issues a JSESSIONID cookie the API never agreed to manage**

The Spring Security default is `IF_REQUIRED` — create an `HttpSession` whenever a filter needs one. For a JWT / bearer-token API this is wrong: the API authenticates from the Authorization header, has no use for a session, but Spring still creates one on the first successful authentication. From that point on every request carries a `JSESSIONID` cookie back, the browser begins associating session state with the API, and CSRF semantics shift from "we ignore CSRF on /api/** because there is no cookie" to "we now have a cookie to defend". The remedy is one line: `.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`.

**Incorrect — implicit IF_REQUIRED session policy:**

```java
@Bean
SecurityFilterChain filter(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            // no sessionManagement — Spring defaults to IF_REQUIRED
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))
            .build();
}
```

**Correct — explicit STATELESS:**

```java
@Bean
SecurityFilterChain filter(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))
            .build();
}
```

Verification: `./gradlew testPractices --tests "*StatelessSession*"` reads `SecurityConfig.java` and asserts it contains the literal `SessionCreationPolicy.STATELESS`.

Reference: [Spring Security — Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
