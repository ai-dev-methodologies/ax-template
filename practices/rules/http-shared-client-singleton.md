---
title: Declare HTTP clients as @Bean singletons, never per-call
impact: HIGH
impactDescription: "Per-call new RestClient() discards the connection pool, ignores timeouts, and adds steady GC pressure"
tags:
  - http
  - lifecycle
  - performance
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-HTTP-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-HTTP-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
evidence:
  - upstream_id: spring-rest-clients
    section: "Spring Framework — RestClient lifecycle"
    quote: "RestClient"
  - source_type: external
    citation: "Spring Framework Reference — REST Clients (RestClient.Builder)"
    url: "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient"
---

## Declare HTTP clients as @Bean singletons, never per-call

**Impact: HIGH — Per-call new RestClient() discards the connection pool, ignores timeouts, and adds steady GC pressure**

A `RestClient` is not a request — it is an HTTP client *handle* with a configured `ClientHttpRequestFactory`, a connection pool, timeout policy, and serializer chain. Constructing one in every controller method or service call discards the pool on each request, ignores all the careful timeout / interceptor configuration, and produces a steady allocation rate that the GC has to clean up. The right shape: a single `@Bean` injected wherever it is used. Spring's default singleton scope guarantees one instance per `ApplicationContext`.

**Incorrect — per-call construction:**

```java
public Response fetch(String id) {
    return new RestClient.Builder()           // new client on every call
            .baseUrl("https://api.example.com")
            .build()
            .get().uri("/items/{id}", id)
            .retrieve()
            .body(Response.class);
}
```

**Correct — injected singleton @Bean:**

```java
@Service
public class ItemService {
    private final RestClient http;
    public ItemService(RestClient practicesHttpClient) {
        this.http = practicesHttpClient;       // singleton injected once
    }
    public Response fetch(String id) {
        return http.get().uri("/items/{id}", id).retrieve().body(Response.class);
    }
}
```

Verification: `./gradlew testPractices --tests "*SharedClientSingleton*"` is a `@SpringBootTest` that injects the bean twice and asserts both references are the *same instance* (assertJ `isSameAs`).

Reference: [Spring Framework — RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
