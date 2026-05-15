---
title: Every HTTP client must declare finite connect + read timeouts
impact: HIGH
impactDescription: "Default null = infinite. One slow upstream silently exhausts the connection pool."
tags:
  - http
  - timeout
  - reliability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-HTTP-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-HTTP-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
evidence:
  - upstream_id: spring-rest-clients
    section: "Spring Framework — ClientHttpRequestFactory configuration"
    quote: "ClientHttpRequestFactory"
  - source_type: external
    citation: "Spring Framework Reference — Configuring the underlying ClientHttpRequestFactory"
    url: "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
---

## Every HTTP client must declare finite connect + read timeouts

**Impact: HIGH — Default null = infinite. One slow upstream silently exhausts the connection pool.**

`SimpleClientHttpRequestFactory` (and most underlying HTTP clients) treat unset / null timeouts as "wait forever". A slow or stalled upstream causes every in-flight call to hang on its socket; under traffic the connection pool fills, then the executor queue fills, then the JVM exhausts worker threads — all without an exception that points at the upstream. Every HTTP client declaration must pin a finite connect timeout AND a finite read timeout. Reasonable starting points: connect 2s, read 5s; tune per upstream SLA.

**Incorrect — no timeout configuration:**

```java
@Bean
public RestClient http() {
    return RestClient.builder()
            .baseUrl("https://api.example.com")
            .build();                  // default factory — infinite timeouts
}
```

**Correct — finite connect + read timeouts:**

```java
@Bean
public RestClient http() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(2_000);        // 2s connect
    f.setReadTimeout(5_000);           // 5s read
    return RestClient.builder()
            .requestFactory(f)
            .baseUrl("https://api.example.com")
            .build();
}
```

Verification: `./gradlew testPractices --tests "*ExplicitTimeouts*"` asserts `HttpClientConfig.CONNECT_TIMEOUT` and `READ_TIMEOUT` are between `Duration.ZERO` and `Duration.ofMinutes(1)`, and that the `buildClient(...)` helper accepts custom timeouts and produces a usable client.

Reference: [Spring Framework — REST Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
