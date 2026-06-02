---
title: Include a /v{N}/ segment in every public API URI
impact: MEDIUM
impactDescription: "URI versioning is cache-friendly, tool-friendly, and the most-deployed evolution strategy"
tags:
  - api
  - versioning
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-API-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-API-003
upstream:
  - "https://google.aip.dev/180"
evidence:
  - upstream_id: google-aip-versioning
    section: "Google AIP-180 — Versioning"
    quote: "version"
  - source_type: external
    citation: "Google AIP-180 — API Versioning"
    url: "https://google.aip.dev/180"
---

## Include a /v{N}/ segment in every public API URI

**Impact: MEDIUM — URI versioning is cache-friendly, tool-friendly, and the most-deployed evolution strategy**

Once a public API has external consumers, breaking changes need a path that lets clients migrate at their own pace. The three viable strategies are URI versioning (`/v1/...`), header versioning (`Accept: application/vnd.example.v1+json`), and query parameter (`?version=1`). URI versioning is the only one that survives every CDN, every proxy, every command-line `curl`, and every Swagger / OpenAPI tool unchanged. AIP-180 documents this as the default for new APIs and recommends staying on `v1` until breaking changes force `v2`.

**Incorrect — un-versioned public endpoint:**

```java
@GetMapping("/parents")
public Page<ParentResponse> list(Pageable p) { ... }
```

**Correct — `/v1/` segment in the path:**

```java
@GetMapping("/v1/parents")
public Page<ParentResponse> list(Pageable p) { ... }
```

Verification: `./gradlew testPractices --tests "*VersioningUriPrefix*"` asserts the `/v1/` path returns 200 and the un-versioned path does NOT return 200 (its exact status — 401/403/404 — varies by SecurityFilterChain), then asserts via reflection that the handler's `@GetMapping` value contains `/v1/`.

Reference: [Google AIP-180 — API Versioning](https://google.aip.dev/180)
