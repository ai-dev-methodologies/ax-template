---
title: Use RestClient for outbound HTTP, not RestTemplate
impact: MEDIUM
impactDescription: "RestTemplate is maintenance-mode since Spring 6.1; RestClient is the actively-developed sync client"
tags:
  - http
  - rest-client
  - spring
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-HTTP-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-HTTP-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
evidence:
  - upstream_id: spring-rest-clients
    section: "Spring Framework — RestClient"
    quote: "RestClient"
  - source_type: external
    citation: "Spring Framework Reference — REST Clients (RestClient)"
    url: "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient"
---

## Use RestClient for outbound HTTP, not RestTemplate

**Impact: MEDIUM — RestTemplate is maintenance-mode since Spring 6.1; RestClient is the actively-developed sync client**

`RestTemplate` is officially in maintenance mode as of Spring 6.1 — it still works, but no new features are added and its API is awkward (overloaded methods, no fluent builder). `RestClient` is the modern fluent sync HTTP client (introduced in Spring Framework 6.1 / Spring Boot 3.2) — it shares the underlying `ClientHttpRequestFactory` infrastructure but exposes a builder API similar to `WebClient`. New outbound HTTP code should use RestClient; existing RestTemplate code should be migrated when touched.

**Incorrect — RestTemplate for new HTTP code:**

```kotlin
@Bean
public RestTemplate http() {
    return new RestTemplate();        // maintenance-mode API
}
```

**Correct — RestClient with explicit factory:**

```java
@Bean
public RestClient practicesHttpClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2_000);
    factory.setReadTimeout(5_000);
    return RestClient.builder()
            .requestFactory(factory)
            .baseUrl("https://api.example.com")
            .build();
}
```

Verification: `./gradlew testPractices --tests "*RestClientOverRestTemplate*"` walks `HttpClientConfig.@Bean` methods and asserts at least one returns `RestClient` and zero return `RestTemplate`.

Reference: [Spring Framework — RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
