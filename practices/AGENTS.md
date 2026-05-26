---
sentinel:
  source_concat_sha256: "34b2f4164f39a5d663de5b2a32ac338cc1f05aeeda54f9ed51a1f40c26a62913"
  rule_count: 111
  generated_by: "practices/generate_agents.sh"
---

# Practices — AGENTS.md (auto-generated)

This file is auto-generated from `practices/rules/*.md` in lexical order.
Do not edit by hand — re-run `practices/generate_agents.sh` after rule changes.

Sentinel sha covers rule concat ONLY (TD-024 sha-input clause).
TOC section below is observability outside the fingerprint (TD-033 R13).

## MANDATORY (R25) before declaring any task done

AI agents MUST run `bash practices/scripts/verify-completion.sh` and confirm
exit 0 before stating the task is complete. The 49th hard guard
(`completion_checklist_recency_guard.sh`) audits the resulting log and
BLOCKS push when no entry matches HEAD. There is no opt-out flag.

<!-- @source rules/actuator-build-info.md -->

---
title: Enable Spring Boot buildInfo() and surface it via /actuator/info
impact: MEDIUM
impactDescription: "Operators need a machine-readable answer to 'what version is running'"
tags:
  - actuator
  - build
  - observability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — /info endpoint and build properties"
    quote: "info"
  - source_type: external
    citation: "Spring Boot Reference — Info Endpoint (build properties)"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.info"
---

## Enable Spring Boot buildInfo() and surface it via /actuator/info

**Impact: MEDIUM — Operators need a machine-readable answer to "what version is running"**

When the on-call asks "what's running in prod right now", "git tag" is not enough — multiple commits can ship the same version, and the pod may be on an out-of-date image. The Spring Boot Gradle plugin's `buildInfo()` task generates `META-INF/build-info.properties` at build time (version + groupId + artifactId + name + build time + Git SHA if the Git plugin is also present). The actuator `/info` endpoint then surfaces these fields. The combination becomes a curl-able answer that an alert runbook can hyperlink to.

**Incorrect — no buildInfo, `/actuator/info` returns `{}`:**

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}
// no springBoot { buildInfo() } — /actuator/info is empty
```

**Correct — buildInfo + actuator info enabled:**

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}

springBoot {
    buildInfo()                      // generates META-INF/build-info.properties at build
}
```

```yaml
management:
  info:
    build:
      enabled: true                  # surface build-info.properties on /actuator/info
```

Verification: `./gradlew testPractices --tests "*BuildInfo*"` reads `build.gradle.kts` to assert `springBoot { buildInfo() }` is present and reads `application.yml` to assert `management.info.build.enabled: true`.

Reference: [Spring Boot — Info Endpoint](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.info)


<!-- @source rules/actuator-kubernetes-probes.md -->

---
title: Expose /actuator/health/liveness + /actuator/health/readiness
impact: HIGH
impactDescription: "Without separate probes K8s can't tell a wedged pod from one still booting"
tags:
  - actuator
  - kubernetes
  - probes
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — Kubernetes Probes (liveness / readiness)"
    quote: "liveness"
  - source_type: external
    citation: "Spring Boot Reference — Kubernetes Probes"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes"
---

## Expose /actuator/health/liveness + /actuator/health/readiness

**Impact: HIGH — Without separate probes K8s can't tell a wedged pod from one still booting**

A single `/actuator/health` endpoint conflates two questions: "is the process alive" and "is the process ready to serve traffic". Kubernetes (and most orchestrators) needs them separately. A liveness probe failing → kill + restart the pod. A readiness probe failing → keep the pod running but stop routing traffic to it. Without dedicated endpoints the orchestrator picks one default behavior for both signals and gets at least one of them wrong (either restart-loops a slow-starting pod, or routes traffic at a pod that hasn't finished initializing).

**Incorrect — only the aggregated /actuator/health (default):**

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health,info
```

**Correct — probes enabled + states explicitly tracked:**

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,mappings
```

Verification: `./gradlew testPractices --tests "*KubernetesProbes*"` starts the app on a random port and asserts `/actuator/health/liveness` and `/actuator/health/readiness` both return 200 with UP/DOWN status.

Reference: [Spring Boot — Kubernetes Probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)


<!-- @source rules/actuator-restrict-exposure.md -->

---
title: management.endpoints.web.exposure.include must be an explicit allow-list
impact: HIGH
impactDescription: "First wildcard added for debugging ships env / beans / heapdump to production"
tags:
  - actuator
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ACTUATOR-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ACTUATOR-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
evidence:
  - upstream_id: spring-boot-actuator-endpoints
    section: "Spring Boot Actuator — Endpoint exposure"
    quote: "exposure"
  - source_type: external
    citation: "Spring Boot Reference — Exposing Endpoints"
    url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.exposing"
---

## management.endpoints.web.exposure.include must be an explicit allow-list

**Impact: HIGH — First wildcard added for debugging ships env / beans / heapdump to production**

Spring Boot's default web exposure is conservative — `health` and `info` only. Many teams override that default with `management.endpoints.web.exposure.include: '*'` for a debugging session and forget to revert. `*` exposes `env` (full configuration including secret values once `show-values: always` is set), `beans` (the entire DI graph), `heapdump` (memory image), `threaddump` (call stacks), `loggers` (runtime level changes), `metrics`, and — if `spring.application.admin.enabled` — `shutdown`. The mechanical remedy is to require an explicit allow-list and reject the wildcard.

**Incorrect — wildcard exposure:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"                  # exposes env, beans, heapdump, threaddump, loggers...
```

**Correct — explicit allow-list:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,mappings
```

Verification: `./gradlew testPractices --tests "*RestrictExposure*"` reads `application.yml`, finds the `include:` line, rejects `*` wildcards, and rejects any of `env`, `beans`, `heapdump`, `threaddump`, `loggers`, `configprops`, `metrics`, `shutdown`.

Reference: [Spring Boot — Exposing Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.exposing)


<!-- @source rules/admin-cannot-rewrite-user-content.md -->

---
title: ROLE_ADMIN may MODERATE (delete) but MUST NOT rewrite user-authored content
impact: HIGH
impactDescription: "Admin-edit-of-user-content destroys audit trail trust — the strongest counter-evidence against the platform's good faith"
tags:
  - authz
  - audit
  - moderation
  - trust
spec_ref: "specs/comment-thread-l0.yaml#COMMENT-AUTHZ-002"
verification:
  gradle_task: testCommentThread
  tag: COMMENT-AUTHZ-002
upstream:
  - "https://gdpr-info.eu/art-5-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 5(1)(a) — Lawfulness, fairness and transparency"
    url: "https://gdpr-info.eu/art-5-gdpr/"
    quote: "Personal data shall be processed lawfully, fairly and in a transparent manner in relation to the data subject."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "OWASP ASVS V8.3.4 — Verify that sensitive personal information is subject to data retention classification"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that sensitive personal information is subject to data retention classification, such that old or out of date data is deleted automatically, on a schedule, or as the situation requires."
    quoted_at: "2026-05-22"
---

## ROLE_ADMIN may MODERATE (delete) but MUST NOT rewrite user-authored content

**Impact: HIGH — Admin-edit-of-user-content destroys audit trust**

The split between moderation (delete) and rewriting (edit) is the hinge of any audit-grade content system. A platform whose admins can silently rewrite user posts cannot credibly claim to preserve the user's voice. The user has no way to prove the published text was their own. This is also the precise failure mode that destroys long-lived comment systems: a single staff "fix" of someone's typo erodes the contract that the published text is the user's own words.

The catalog rule: `ROLE_ADMIN` is permitted on DELETE (moderation outcome) but rejected on PUT/edit. The author is the only principal allowed to mutate text. The author can edit their own text; the audit trail (CommentEdit row) captures the pre-image. If the platform needs the offending text removed for legal reasons, the path is delete (status flip to DELETED, body masked) — not rewrite.

Catalog evidence (R36 comment-thread, COMMENT-AUTHZ-002): `CommentService.edit()` checks `comment.getAuthorUserId().equals(auth.getName())` regardless of authority. Admin attempts return 403 EDIT_FORBIDDEN. The dedicated admin endpoint (under `/api/admin/comments`) accepts only DELETE, not PUT.

**Incorrect — admin can edit any user's content:**

```java
@PutMapping("/api/comments/{id}")
public CommentResponse edit(Authentication auth, @PathVariable UUID id, @RequestBody UpdateRequest body) {
    Comment c = repo.findById(id).orElseThrow();
    // Anti-pattern: admin override allowed
    if (!c.getAuthorUserId().equals(auth.getName()) && !isAdmin(auth)) {
        throw new AccessDeniedException();
    }
    c.editBody(body.body(), Instant.now());
    return CommentResponse.from(c);
}
```

A leaked admin token now silently rewrites any comment. Even with no leak: this code asks the platform's staff to be trusted with rewriting any user's words. The audit invariant is broken by design.

**Correct — author-only edit, admin can only moderate (delete):**

```java
@PutMapping("/api/comments/{id}")
public CommentResponse edit(Authentication auth, @PathVariable UUID id, @RequestBody UpdateRequest body) {
    Comment c = repo.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
    if (!c.getAuthorUserId().equals(auth.getName())) {
        // No `|| isAdmin(auth)` here — even admin cannot rewrite
        throw new EditForbiddenException("only the author may edit comment " + id);
    }
    // captures pre-image to CommentEdit BEFORE mutating
    editHistory.save(new CommentEdit(c.getId(), Instant.now(), auth.getName(), c.getBody()));
    c.editBody(body.body(), Instant.now());
    return CommentResponse.from(c);
}

@DeleteMapping("/api/admin/comments/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<Void> moderate(Authentication auth, @PathVariable UUID id) {
    service.softDelete(id, auth.getName());           // status flip + body→NULL + audit row
    return ResponseEntity.noContent().build();
}
```

The admin path is delete-only. The author path is the only edit path. The audit trail (`actedByUserId` + `CommentEdit`) is honest because the operation it records is the only operation that ever happened.

**This rule applies anywhere user-authored text is subject to revision history**: comments, reviews, posts, change logs, journal entries, ticket descriptions. It does NOT apply to admin-curated metadata (tag names, category labels, feature-flag descriptions) where the admin IS the authoring party.

Reference: [GDPR Article 5 — Lawfulness, fairness and transparency](https://gdpr-info.eu/art-5-gdpr/)

Reference: [OWASP ASVS V8 — Data Protection](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/api-idempotency-key-required.md -->

---
title: POST endpoints with non-idempotent side effects must require an Idempotency-Key header
impact: HIGH
impactDescription: "Network retries on POST without dedupe cause duplicate side effects (double charge, duplicate send)"
tags:
  - api
  - http
  - idempotency
  - retry-safety
spec_ref: "specs/payment-l0.yaml#PAYMENT-IDEMP-001"
verification:
  gradle_task: testPayment
  tag: PAYMENT-IDEMP-001
upstream:
  - "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
  - "https://docs.stripe.com/api/idempotent_requests"
evidence:
  - upstream_id: rfc-7807
    section: "Problem Details for HTTP APIs — error envelope for the missing-key 400"
    quote: "Problem Details"
  - source_type: external
    citation: "IETF draft — The Idempotency-Key HTTP Header Field (draft-ietf-httpapi-idempotency-key-header)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
  - source_type: external
    citation: "Stripe API Reference — Idempotent requests"
    url: "https://docs.stripe.com/api/idempotent_requests"
---

## POST endpoints with non-idempotent side effects must require an Idempotency-Key header

**Impact: HIGH — Network retries on POST without dedupe cause duplicate side effects (double charge, duplicate send)**

`POST` is the HTTP verb defined as non-idempotent: the IETF semantics allow each call to create a fresh resource or trigger a fresh side effect. Any production network — mobile, browser fetch with auto-retry, load balancer retries, service-mesh retries — will retry a request that timed out, returned 502, or lost its socket. Without a dedupe protocol the second attempt double-charges a card, double-sends an email, or double-creates a job. The de-facto fix, standardised by an IETF draft (`draft-ietf-httpapi-idempotency-key-header`) and implemented by Stripe, Adyen, Plaid, GitHub, and Square, is the `Idempotency-Key` request header: the client supplies a unique key per logical operation; the server caches the response keyed by `(principal, key)` for a TTL window (commonly 24 hours) and on a duplicate-key arrival returns the *cached* original response without re-executing the side effect.

This rule applies to any POST endpoint whose execution has a non-idempotent side effect: payment authorize/capture/refund/void, notification dispatch (email / SMS / push), order placement, file upload finalize, webhook delivery. The rule does **not** apply to read-only POST endpoints (rare but valid) or to `PUT`/`DELETE` endpoints whose semantics are already idempotent by HTTP definition.

**Incorrect — POST without idempotency key, retry replays the side effect:**

```java
@PostMapping("/api/payments")
public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest req) {
    // network retry → second invocation → second charge
    return paymentService.charge(req);
}
```

**Correct — required header + dedupe store:**

```java
@PostMapping("/api/payments")
public PaymentResponse create(
        @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey,
        @Valid @RequestBody CreatePaymentRequest req,
        Authentication auth) {
    String principal = auth.getName();
    return idempotencyStore.computeIfAbsent(principal, idempotencyKey,
            () -> paymentService.charge(req));   // executes once per (principal, key)
}
// Missing header → @RequestHeader's required=true returns 400 with an RFC 7807
// ProblemDetail describing the missing-header constraint.
```

The store layer (Caffeine, Redis, or a database table) MUST be atomic — `putIfAbsent` semantics or `SELECT ... FOR UPDATE` — so that concurrent duplicate requests with the same key collapse to one execution and the losers either wait for the result or receive the same cached payload. A non-atomic implementation re-creates the double-charge bug under racing retries.

Verification: `./gradlew testPayment --tests "*Idempotency*"` exercises (a) missing-header → 400 RFC 7807, (b) duplicate-key within TTL → cached response, no second charge, (c) 5-thread concurrent same-key race → exactly one charge created. `Idempotency-Key` is the canonical header name; alternative names (`X-Idempotency-Key`, `Request-Id`) should be avoided for interop with PSP and platform tooling that assume the IETF draft name.

Reference: [IETF draft — The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)

Reference: [Stripe API — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)


<!-- @source rules/api-no-entity-leak.md -->

---
title: Return DTO records from controllers, never JPA entities
impact: HIGH
impactDescription: "Returning entities leaks association graphs, lazy fields, and breaks the API contract on every entity edit"
tags:
  - api
  - dto
  - persistence
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-API-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-API-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
evidence:
  - upstream_id: spring-mvc-modelattribute
    section: "Spring MVC — controller method arguments and return values"
    quote: "@ModelAttribute"
  - source_type: external
    citation: "Spring Framework Reference — Controllers and DTOs"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
---

## Return DTO records from controllers, never JPA entities

**Impact: HIGH — Returning entities leaks association graphs, lazy fields, and breaks the API contract on every entity edit**

Returning a JPA entity from a controller writes the full entity surface into the response body. Lazy associations get triggered, internal-only fields appear, and every entity refactor silently rewrites the public API contract — the day someone adds an `@OneToMany` for an internal cache, the API breaks. The remedy is a record DTO that explicitly lists which fields cross the boundary. The mapping function (`from(entity)`) is the single place the contract is defined.

**Incorrect — return the JPA entity directly:**

```java
@GetMapping("/parents")
public List<Parent> list() {
    return parents.findAll();             // children collection leaks, lazy fields trigger
}
```

**Correct — DTO record collapses the entity into a contract surface:**

```java
public record ParentResponse(Long id, String name, int childCount) {
    public static ParentResponse from(Parent p) {
        return new ParentResponse(p.getId(), p.getName(), p.getChildren().size());
    }
}

@GetMapping("/v1/parents")
public Page<ParentResponse> list(Pageable pageable) {
    return parents.findAll(pageable).map(ParentResponse::from);
}
```

Verification: `./gradlew testPractices --tests "*NoEntityLeak*"` asserts the JSON body contains the DTO field (`childCount`) and does NOT contain the entity field (`children`).

Reference: [Spring MVC — Controller arguments and returns](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)


<!-- @source rules/api-pagination-pageable.md -->

---
title: List endpoints must use Pageable and clamp size
impact: HIGH
impactDescription: "Unbounded list endpoints are a recurring latency + memory hazard"
tags:
  - api
  - pagination
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-API-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-API-001
upstream:
  - "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
evidence:
  - upstream_id: spring-data-paging
    section: "Spring Data — Pageable / Page<T>"
    quote: "Pageable"
  - source_type: external
    citation: "Spring Data Commons Reference — Core Concepts (Pageable)"
    url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
---

## List endpoints must use Pageable and clamp size

**Impact: HIGH — Unbounded list endpoints are a recurring latency + memory hazard**

`@GetMapping("/parents") List<Parent> all()` works fine on a developer laptop with 12 rows. The same endpoint against a production table with 4 million rows times out the connection pool, exhausts heap, or returns megabytes of JSON to a mobile client that wanted the first ten. The contract every list endpoint must enforce: accept `page` + `size` (or a `Pageable`), clamp `size` to a documented maximum, and return `Page<DTO>` so the client gets total counts and navigation links alongside the slice.

**Incorrect — unbounded findAll() exposed as a list:**

```java
@GetMapping("/parents")
public List<Parent> all() {
    return parentRepo.findAll();             // 4M rows? sure, here you go
}
```

**Correct — Pageable parameter, clamped size, Page<DTO> response:**

```java
private static final int MAX_PAGE_SIZE = 100;

@GetMapping("/v1/parents")
public Page<ParentResponse> listParents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
) {
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
    return parentRepo.findAll(pageable).map(ParentResponse::from);
}
```

Verification: `./gradlew testPractices --tests "*PaginationPageable*"` asserts the endpoint honors `?page=0&size=5`, defaults to a reasonable size, and clamps an oversize `?size=10000` to the documented maximum.

Reference: [Spring Data — Core Concepts (Pageable)](https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html)


<!-- @source rules/api-versioning-uri-prefix.md -->

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

Verification: `./gradlew testPractices --tests "*VersioningUriPrefix*"` asserts the `/v1/` path returns 200 and the un-versioned path returns 404, then asserts via reflection that the handler's `@GetMapping` value contains `/v1/`.

Reference: [Google AIP-180 — API Versioning](https://google.aip.dev/180)


<!-- @source rules/async-completablefuture-return-type.md -->

---
title: "@Async methods must return CompletableFuture, never void"
impact: HIGH
impactDescription: "void @Async swallows exceptions inside the executor and breaks await/compose at the caller"
tags:
  - async
  - spring
  - exception-handling
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
  - "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
evidence:
  - upstream_id: spring-task-execution
    section: "Spring — @Async return types and exception propagation"
    quote: "Async"
  - upstream_id: spring-scheduling
    section: "Spring — @Async / CompletableFuture"
    quote: "CompletableFuture"
  - source_type: external
    citation: "Spring Framework Reference — @Async"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async"
---

## @Async methods must return CompletableFuture, never void

**Impact: HIGH — void @Async swallows exceptions inside the executor and breaks await/compose at the caller**

The `@Async` proxy returns immediately and dispatches the body to the task executor. A `void` return type means the caller cannot await the result and — more dangerous — cannot observe an exception thrown inside the executor; the exception is logged (sometimes) and silently lost. Returning `CompletableFuture<T>` lets the caller `.thenApply(...)` / `.get()` / `.exceptionally(...)`, lets composition work, and surfaces exceptions through the future's completion state.

**Incorrect — void return swallows exceptions:**

```java
@Service
public class ReportService {
    @Async
    public void generateReport(ReportRequest req) {   // exceptions disappear into the executor
        if (!req.valid()) throw new IllegalStateException("bad input");
        sink.write(buildReport(req));
    }
}
```

**Correct — CompletableFuture lets the caller await + observe failure:**

```java
@Service
public class ReportService {
    @Async
    public CompletableFuture<Path> generateReport(ReportRequest req) {
        if (!req.valid()) {
            return CompletableFuture.failedFuture(new IllegalStateException("bad input"));
        }
        return CompletableFuture.completedFuture(sink.write(buildReport(req)));
    }
}
```

Verification: `./gradlew testPractices --tests "*SpringAsyncReturnsCompletableFuture*"` asserts via reflection that the @Async method's return type is `CompletableFuture` and awaits a result through the proxy.

Reference: [Spring Framework — @Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)


<!-- @source rules/async-scheduled-fixed-delay-vs-fixed-rate.md -->

---
title: Use fixedDelay for variable-duration tasks; reserve fixedRate for instant heartbeats
impact: MEDIUM
impactDescription: "fixedRate on slow tasks piles invocations and exhausts the scheduler pool"
tags:
  - async
  - scheduled
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
evidence:
  - upstream_id: spring-scheduling
    section: "Spring — @Scheduled (fixedDelay vs fixedRate)"
    quote: "fixedDelay"
  - source_type: external
    citation: "Spring Framework Reference — @Scheduled attributes"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled"
---

## Use fixedDelay for variable-duration tasks; reserve fixedRate for instant heartbeats

**Impact: MEDIUM — fixedRate on slow tasks piles invocations and exhausts the scheduler pool**

`@Scheduled(fixedDelay = N)` waits N ms AFTER the previous run finishes. A cleanup that occasionally takes 5 minutes simply waits another N ms before the next run — it cannot stack. `@Scheduled(fixedRate = N)` invokes every N ms from the previous invocation's START. If a run takes longer than N ms, the next one starts immediately and invocations queue up; under load the scheduler pool exhausts and other scheduled work starves. Use `fixedRate` only for tasks that finish in noticeably less than N ms (cheap heartbeats, cadence-sensitive metrics).

**Incorrect — fixedRate on a long-running cleanup:**

```java
@Scheduled(fixedRate = 60_000L)        // 60s cadence — if cleanup takes 90s, runs stack
public void cleanup() {
    purgeOldRecords();                 // sometimes 5 minutes
}
```

**Correct — fixedDelay on a long-running cleanup:**

```java
@Scheduled(fixedDelay = 60_000L)       // 60s gap AFTER the previous run finishes
public void cleanup() {
    purgeOldRecords();
}
```

Verification: `./gradlew testPractices --tests "*ScheduledFixedDelay*"` asserts via reflection that the cleanup fixture has `fixedDelay > 0` and `fixedRate ≤ 0`, while the heartbeat fixture has the inverse.

Reference: [Spring Framework — @Scheduled attributes](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled)


<!-- @source rules/async-virtual-thread-executor.md -->

---
title: Use JDK 21 virtual threads for blocking-IO workloads
impact: HIGH
impactDescription: "Cheap threads remove the headcount-limited platform-thread bottleneck for blocking I/O"
tags:
  - async
  - concurrency
  - jdk21
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ASYNC-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ASYNC-001
upstream:
  - "https://openjdk.org/jeps/444"
  - "https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
evidence:
  - upstream_id: jep-444-virtual-threads
    section: "JEP 444 — Virtual Threads (Final)"
    quote: "Thread.ofVirtual"
  - upstream_id: spring-task-execution
    section: "Spring Boot — Task Execution with virtual threads"
    quote: "virtual"
  - source_type: external
    citation: "JEP 444 — Virtual Threads (Final), Java 21"
    url: "https://openjdk.org/jeps/444"
---

## Use JDK 21 virtual threads for blocking-IO workloads

**Impact: HIGH — Cheap threads remove the headcount-limited platform-thread bottleneck for blocking I/O**

Platform threads (the default since Java 1.0) are 1:1 with OS threads — expensive to allocate, scarce on a process. A connection-pool-style design caps concurrency at a few hundred. Virtual threads (JEP 444, Java 21) are M:N over a small carrier pool: an idle virtual thread parked on a `socket.read()` consumes ~kilobytes, not megabytes. Workloads that spend most of their wall-clock blocked on I/O (HTTP outbound, JDBC, message-queue consumers) should switch to `Executors.newVirtualThreadPerTaskExecutor()`. CPU-bound work stays on platform threads — virtual threads do not make the CPU faster.

**Incorrect — capped platform-thread pool for an outbound-call workload:**

```java
ExecutorService pool = Executors.newFixedThreadPool(50);   // 50 concurrent ceiling
for (var batch : batches) {
    pool.submit(() -> http.fetch(batch.url()));            // 99% wall-clock waiting on socket
}
```

**Correct — virtual threads scale to the workload, not the pool size:**

```java
try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
    for (var batch : batches) {
        pool.submit(() -> http.fetch(batch.url()));
    }
}   // close blocks until all virtual threads finish
```

Verification: `./gradlew testPractices --tests "*VirtualThreadExecutor*"` submits a task to the executor and asserts `Thread.currentThread().isVirtual()` is `true` inside the body.

Reference: [JEP 444 — Virtual Threads](https://openjdk.org/jeps/444) · [Spring Boot — Task Execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)


<!-- @source rules/audit-log-pii-hash-required.md -->

---
title: AUDIT log lines MUST hash PII identifiers — never write raw email / phone / RRN to log aggregators
impact: HIGH
impactDescription: "Operator log aggregators (ELK, Splunk, CloudWatch, Datadog) typically have looser access controls than the primary DB and longer retention. Writing raw PII to audit log lines silently expands the PII surface area to every engineer who can query the log."
tags:
  - pii
  - audit
  - logging
  - korean-enterprise
  - 개인정보보호법
spec_ref: "specs/email-outbox-l0.yaml#EMAIL-ADMIN-001"
verification:
  source: "backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailOutboxService.java"
  pattern: "AUDIT.info(\"verb=ADMIN_RETRY id={} recipientHash={}\", id, EmailPiiHelper.recipientHash(row.getRecipient()))"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://www.rfc-editor.org/rfc/rfc6234"
evidence:
  - source_type: external
    citation: "OWASP ASVS V7 — Error Handling and Logging"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the application does not log credentials, payment details, or other sensitive data."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "RFC 6234 — US Secure Hash Algorithms (SHA-256 deterministic hash for correlation tokens)"
    url: "https://www.rfc-editor.org/rfc/rfc6234"
    quote: "SHA-256 is a secure hash algorithm. The use of this algorithm enables determination of a message's integrity: any change to the message will, with a very high probability, result in a different message digest."
    quoted_at: "2026-05-26"
---

## AUDIT log lines MUST hash PII identifiers — never write raw email / phone / RRN to log aggregators

**Impact: HIGH — log aggregators silently expand the PII surface area**

When a service writes an AUDIT log line carrying a user identifier — email,
phone number, 주민등록번호, or any value the privacy regime classifies as PII —
that identifier flows to every downstream log aggregator: ELK, Splunk,
CloudWatch, Datadog, Loki. The aggregator's access control is typically
WIDER than the primary database (every on-call engineer needs to query
logs), the retention is LONGER (logs are kept for 30/90/180 days for
incident forensics), and the geographic scope is BROADER (logs replicate
across regions while the DB may not). Storing the raw value once at the
service is multiplied N-fold by the log fan-out.

The catalog rule is binary: AUDIT log lines MUST hash any PII identifier
before emitting the log statement. The hash is a stable correlation token
— same input deterministically produces the same hash, so an SRE
investigating "why did this row keep failing" can trace the row across
log lines without ever seeing the raw email. Use a truncated SHA-256
(16 hex chars is sufficient for ops correlation; the collision risk for
typical org sizes is on the order of 10^-19).

The full identifier remains in the primary DB column where the access
control is tighter — admins who legitimately need it can query the row
by id. Operators reading the log get the hash; they never see the raw.

**Incorrect — writes raw recipient email to the operator log aggregator:**

```java
@Transactional
public EmailOutbox adminRetry(UUID id) {
    EmailOutbox row = outboxRepository.findById(id).orElseThrow(...);
    row.resetForRetry();
    AUDIT.info("verb=ADMIN_RETRY id={} recipient={}", id, row.getRecipient());
    return outboxRepository.save(row);
}
```

**Correct — hash the identifier before logging:**

```java
static String recipientHash(String email) {
    if (email == null || email.isBlank()) return "(none)";
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(email.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest).substring(0, 16);
}

AUDIT.info("verb=ADMIN_RETRY id={} recipientHash={}", id, recipientHash(row.getRecipient()));
// → "verb=ADMIN_RETRY id=abc... recipientHash=4f3a9b7e8c1d2f0a"  ✅ stable correlation token, no PII
```

Reference: [OWASP ASVS V7 — Error Handling and Logging](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [RFC 6234 — US Secure Hash Algorithms (SHA-256)](https://www.rfc-editor.org/rfc/rfc6234)

## How to apply

Before adding `log.info(...)` / `AUDIT.info(...)` / metric labels, check every
positional argument:

```text
for each interpolated value in the log statement:
  if value is a recipient email / phone / RRN / 주민등록번호 / national ID:
    REWRITE: pass through recipientHash() / phoneHash() / similar
  if value is an entity id (UUID, integer PK):
    OK — entity ids are not PII
  if value is a request id / correlation id:
    OK
```

The catalog ships `EmailPiiHelper.recipientHash()` in the email-outbox L4
as the canonical example. Other L4s that touch PII in their audit lines
should follow the same pattern; the helper is small (one method, no
dependencies) so duplicating it per L4 is fine until enough L4s converge
to justify lifting to L0/fork-receiver-kit (the R53 pattern).

## Anti-patterns

- "I'll just log `userId` — that's not PII" — userId is fine IF the rest of
  the table doesn't carry the email next to it. If the audit table joins
  to users by id, the operator can `JOIN users` and recover the email,
  defeating the purpose. Hashing keeps log queries opaque to mass PII
  recovery.
- "We have log access controls — only on-call sees it" — log access
  controls drift; team membership changes; SOC-2 audits routinely find
  ex-employees retaining log access for weeks. Hashing is structural,
  not policy.
- "Hashing makes debugging harder" — same input → same hash, so an SRE
  filters logs by `recipientHash=<value>` after retrieving the value
  from the DB once. The investigation pattern is one extra SQL query.


<!-- @source rules/background-poll-must-show-refresh-state.md -->

---
title: Background-polled pages MUST expose dataUpdatedAt + aria-busy on mutations
impact: HIGH
impactDescription: "Any TanStack Query useQuery with refetchInterval that hides the dataUpdatedAt timestamp gives operators a confidently-stale view; mutations on the same page without aria-busy break WCAG SC 4.1.3 status-message expectations for screen readers tracking the operation outcome."
tags:
  - accessibility
  - wcag
  - tanstack-query
  - background-poll
  - data-freshness
  - aria-busy
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  source: "practices/evals/background_poll_refresh_state_guard.sh (R82b — 44th hard guard)"
  pattern: "useQuery({ refetchInterval, ... }) — every match MUST be paired with a dataUpdatedAt reference in the same React function body; every mutation button on the same page MUST set aria-busy until settled"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — useQuery options (refetchInterval / dataUpdatedAt)"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
    quote: "refetchInterval: number | false | ((query: Query) => number | false | undefined) — If set to a number, all queries will continuously refetch at this frequency in milliseconds"
    quoted_at: "2026-05-26"
  - upstream_id: wcag-22-techniques-2026-05
    section: "SC 4.1.3 Status Messages (Level AA)"
    quote: "status messages can be programmatically"
---

## Background-polled pages MUST expose dataUpdatedAt + aria-busy on mutations

**Impact: HIGH — operators trust a stale dashboard; screen reader users miss mutation-result status.**

R50 (`incident-dashboard-background-poll-plus-refresh`) introduced the "poll-in-background + visible last-updated timestamp + Refresh button" trio for incident dashboards. R82 generalises the requirement to **every** background-polled page, not only incident dashboards, and adds the partner requirement for any mutation triggered from that page:

1. **`dataUpdatedAt` MUST be visible.** Any `useQuery({...refetchInterval: <ms>...})` MUST destructure `dataUpdatedAt` and render it in the page header (or an equivalent always-visible region). A timestamp the operator can read silently confirms the polling cadence; without it, a network blip or query-error retry can leave the data older than the polling interval, and the operator has no way to notice.

2. **Mutations triggered from the page MUST set `aria-busy="true"` on the trigger button until the mutation settles.** Screen-reader users who initiate a mutation (Retry, Approve, Revoke, Force-Cancel) on a background-polled page would otherwise hear only the polled-query updates and the mutation status would be silent. WCAG SC 4.1.3 Status Messages requires programmatic status conveyance; `aria-busy` is the canonical mechanism for "operation in flight" on the triggering control.

This is the bridge between TanStack Query's freshness model and WCAG 2.2's status-messages requirement. R50 covers the freshness side for the SRE persona; R82 covers the screen-reader / mutation-status side for the accessibility persona.

**Incorrect — refetchInterval without visible dataUpdatedAt, mutation button without aria-busy:**

```tsx
const { data } = useQuery({
  queryKey: ['retry-queue'],
  queryFn: fetchQueue,
  refetchInterval: 5_000,
})

const retryMutation = useMutation({ mutationFn: retryRow })

<button onClick={() => retryMutation.mutate(row.id)}>
  Retry
</button>
```

The operator cannot tell whether the visible PENDING count is 5 s old or 5 min old. A screen-reader user clicks Retry and hears nothing for the duration of the request — the next thing they hear is the polled refetch result, which may or may not reflect their click.

**Correct — dataUpdatedAt rendered, aria-busy reflects the mutation lifecycle:**

```tsx
const { data, dataUpdatedAt, refetch } = useQuery({
  queryKey: ['retry-queue'],
  queryFn: fetchQueue,
  refetchInterval: 5_000,
  refetchIntervalInBackground: true,
})

const retryMutation = useMutation({ mutationFn: retryRow })

<header className="flex items-center gap-2">
  <span aria-live="polite" className="text-xs">
    {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
  </span>
  <button onClick={() => refetch()} className="text-xs">Refresh</button>
</header>

<button
  type="button"
  aria-busy={retryMutation.isPending}
  disabled={retryMutation.isPending}
  onClick={() => retryMutation.mutate(row.id)}
>
  {retryMutation.isPending ? 'Retrying…' : 'Retry'}
</button>
```

**Apply this rule to**: any page that uses `useQuery` with a numeric `refetchInterval` AND issues at least one `useMutation` triggered by a user-facing control on the same page. The R51 email-outbox admin page already satisfies the pattern; R55 favorites (no refetchInterval) is out of scope.

**When NOT to apply**: pages with `useQuery` but no `refetchInterval` (e.g. one-shot loads, manual-refetch surfaces). The freshness signal is the operator's own re-fetch, so a visible timestamp adds noise rather than safety.

A pair-with rule: when the polled data renders server-supplied error strings, apply `server-side-stored-error-sanitize` (R61) at the storage boundary so a screen-shared incident bridge cannot leak PII through the same surface this rule keeps fresh.

Reference: [TanStack Query v5 — useQuery API](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery)

Reference: [WCAG 2.2 — Understanding Success Criterion 4.1.3: Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)


<!-- @source rules/billing-event-idempotent.md -->

---
title: "All BillingEvent writes must carry a unique idempotencyKey; duplicate provider events must be rejected without creating a second row"
rule_id: billing-event-idempotent
impact: CRITICAL
impactDescription: "Duplicate webhook delivery (common from Stripe/Toss under network instability) creates duplicate BillingEvents, double-transitions subscription state, and emits double counter increments"
tags:
  - billing
  - idempotency
  - webhook
  - event-sourcing
provenance_class: internal_design
protects_template_id: templates/backend/billing/BillingEvent.java
failing_fixture_path: practices/evals/fixtures/billing-event-idempotent/fail_no_idempotency_key/
spec_ref: "specs/billing-l0.yaml#BILLING-IDEMP-001"
verification:
  type: review
  notes: |
    Check: every BillingEvent.createInternal() or BillingEvent.fromWebhook() call
    supplies a non-null, non-empty idempotencyKey.
    DB: billing_events.idempotency_key has UNIQUE constraint.
    WebhookBillingReceiver catches duplicate-key exceptions and returns 200 (not 5xx).
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Idempotency"
    quote: "Stripe stores results for at least 24 hours. Retrying the same key within the window returns the original response without creating a duplicate resource."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "멱등성"
    quote: "Idempotency-Key 헤더를 사용하면 네트워크 오류로 인한 재시도 시 중복 결제를 방지할 수 있습니다."
  - source_type: external
    citation: "IETF draft — The Idempotency-Key HTTP Header Field (exactly-once semantics)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## BillingEvent writes must carry a unique idempotencyKey

**Impact: CRITICAL — Duplicate webhook delivery (normal under provider SLA) creates duplicate BillingEvents that cause double subscription state transitions, double counter increments, and incorrect invoice generation.**

Both Stripe and Toss Payments guarantee **at-least-once** delivery of webhook events. Under network instability, the same event may arrive 2–3 times within a few seconds. Without idempotency protection, each delivery creates a new BillingEvent row, triggering a second state transition (e.g., ACTIVE → PAST_DUE twice) and emitting billing observability counters twice.

### Enforcement

1. **DB UNIQUE constraint** on `billing_events.idempotency_key` (see `BillingEvent.java`).
2. **Factory constructors** `BillingEvent.createInternal()` and `BillingEvent.fromWebhook()` require non-null `idempotencyKey`.
3. **WebhookBillingReceiver** catches `DataIntegrityViolationException` from duplicate-key inserts and returns HTTP 200 without re-processing.
4. **Observability**: `billing.event.idempotency_hit_count` counter increments on every detected duplicate.

**Incorrect — BillingEvent without idempotencyKey:**

```java
// VIOLATION: no idempotencyKey → duplicate webhook creates second row → double state transition
BillingEvent event = new BillingEvent();
event.setSubscriptionId(sub.getId());
event.setEventType(PAYMENT_SUCCEEDED);
// idempotencyKey not set → null constraint violation or silent duplicate
billingEventRepository.save(event);
```

**Correct — BillingEvent with idempotencyKey from provider event ID:**

```java
// CORRECT: fromWebhook() sets idempotencyKey from provider event ID
BillingEvent event = BillingEvent.fromWebhook(
    sub.getId(),
    BillingEventType.PAYMENT_SUCCEEDED,
    providerWebhookEvent.getId(),   // idempotencyKey = stripe evt_xxx or toss payment_xxx
    providerWebhookEvent.getId(),
    providerWebhookEvent.getTimestamp(),
    metadataJson
);
billingEventRepository.save(event);
// Duplicate webhook with same providerEventId → DataIntegrityViolationException → return 200
```

Reference: https://stripe.com/docs/api/idempotent_requests

## Failing fixture

See: `practices/evals/fixtures/billing-event-idempotent/fail_no_idempotency_key/` — BillingEvent created via a constructor that leaves `idempotencyKey` null. ArchUnit or static analysis flags the missing field.

See: `practices/evals/fixtures/billing-event-idempotent/pass_idempotency_key_set/` — correct usage.


<!-- @source rules/build-java-toolchain-explicit.md -->

---
title: Declare an explicit Java toolchain in build.gradle.kts
impact: MEDIUM
impactDescription: "Without an explicit toolchain the build silently follows the developer's $PATH"
tags:
  - build
  - gradle
  - toolchain
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-003
upstream:
  - "https://docs.gradle.org/current/userguide/toolchains.html"
evidence:
  - upstream_id: gradle-toolchains
    section: "Gradle Toolchains for JVM projects"
    quote: "JavaLanguageVersion"
  - source_type: external
    citation: "Gradle User Guide — Toolchains for JVM projects"
    url: "https://docs.gradle.org/current/userguide/toolchains.html"
---

## Declare an explicit Java toolchain in build.gradle.kts

**Impact: MEDIUM — Without an explicit toolchain the build silently follows the developer's $PATH**

Gradle's default behavior is to compile + run tests against whichever JDK is on the user's `JAVA_HOME` / `$PATH`. A developer with JDK 17 produces a working jar; a CI agent with JDK 21 produces a different jar; a teammate with JDK 11 produces a build error. The toolchain block makes the JDK an explicit input to the build — Gradle downloads the correct JDK if necessary and refuses to silently use a different one.

**Incorrect — no toolchain block, $PATH decides:**

```kotlin
plugins { java }
group = "com.example"
// no java { toolchain { ... } } — every developer's local JDK becomes the build's JDK
```

**Correct — explicit toolchain pins the JDK:**

```kotlin
plugins { java }
group = "com.example"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Verification: `./gradlew testPractices --tests "*JavaToolchainExplicit*"` reads `backend/build.gradle.kts` and asserts both the `toolchain` keyword and a `JavaLanguageVersion.of(...)` call are present.

Reference: [Gradle User Guide — Toolchains for JVM projects](https://docs.gradle.org/current/userguide/toolchains.html)


<!-- @source rules/build-no-snapshot-dependencies.md -->

---
title: Production builds must not depend on -SNAPSHOT artifacts
impact: HIGH
impactDescription: "SNAPSHOT versions mutate underfoot — reproducibility and bisectability lost"
tags:
  - build
  - dependency-management
  - reproducibility
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-002
upstream:
  - "https://docs.gradle.org/current/userguide/dynamic_versions.html"
evidence:
  - source_type: external
    citation: "Gradle User Guide — Dynamic and changing versions"
    url: "https://docs.gradle.org/current/userguide/dynamic_versions.html"
  - source_type: external
    citation: "Maven — SNAPSHOT versioning semantics"
    url: "https://maven.apache.org/repository/internal-and-snapshot-repositories.html"
---

## Production builds must not depend on -SNAPSHOT artifacts

**Impact: HIGH — SNAPSHOT versions mutate underfoot — reproducibility and bisectability lost**

A `-SNAPSHOT` coordinate is, by Maven / Gradle contract, a "the latest build of that version line, fetched fresh". Two consecutive `./gradlew build` runs against a SNAPSHOT can resolve to different artifacts. CI green at 09:00 becomes CI red at 10:00 with no commit in between. `git bisect` cannot identify the change because the change is *not in the repo*. A bug fixed against one SNAPSHOT resurfaces against another. Production builds must depend on released, immutable versions only — SNAPSHOTs belong in local experiments, not committed `build.gradle.kts`.

**Incorrect — SNAPSHOT in a production dependency declaration:**

```kotlin
dependencies {
    implementation("com.example:my-internal-lib:2.4.0-SNAPSHOT")     // mutates underfoot
}
```

**Correct — released version:**

```kotlin
dependencies {
    implementation("com.example:my-internal-lib:2.4.0")              // immutable, reproducible
}
```

Verification: `./gradlew testPractices --tests "*NoSnapshotDependencies*"` scans `backend/build.gradle.kts` line-by-line (skipping comments) and asserts no line contains `-SNAPSHOT`.

Reference: [Gradle — Dynamic and changing versions](https://docs.gradle.org/current/userguide/dynamic_versions.html) · [Maven — SNAPSHOT semantics](https://maven.apache.org/repository/internal-and-snapshot-repositories.html)


<!-- @source rules/build-spring-boot-bom.md -->

---
title: Apply the Spring Boot dependency-management plugin (BOM pinning)
impact: HIGH
impactDescription: "Hand-picked starter versions drift and produce class-version mismatches"
tags:
  - build
  - gradle
  - dependency-management
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-BUILD-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-BUILD-001
upstream:
  - "https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
evidence:
  - upstream_id: spring-dependency-management
    section: "Spring dependency-management plugin — importing BOMs"
    quote: "dependency management"
  - source_type: external
    citation: "Spring dependency-management Plugin Reference"
    url: "https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
---

## Apply the Spring Boot dependency-management plugin (BOM pinning)

**Impact: HIGH — Hand-picked starter versions drift and produce class-version mismatches**

`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa` are not independent libraries — they each pull in dozens of transitive Spring + Jackson + Hibernate + Tomcat versions that must be aligned. The Spring Boot BOM pins those versions for one tested combination. Without the dependency-management plugin (or the Spring Boot Gradle plugin's equivalent), Gradle picks "newest version wins" per transitive, and most teams discover the resulting drift via a `NoSuchMethodError` in production months later.

**Incorrect — hand-pinned starter versions, no BOM:**

```kotlin
plugins {
    java
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-security:3.1.5")
    // transitive Jackson, Tomcat, Hibernate versions resolved by Gradle's mediation — drift inevitable
}
```

**Correct — Spring Boot plugin + dependency-management BOM:**

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")     // version from BOM
    implementation("org.springframework.boot:spring-boot-starter-security")
}
```

Verification: `./gradlew testPractices --tests "*SpringBootBom*"` reads `backend/build.gradle.kts` and asserts both plugin ids are applied.

Reference: [Spring dependency-management Plugin](https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/)


<!-- @source rules/business-domain-must-declare-applied-recipe.md -->

---
title: "Every L4 domain README that participates in a Business Pattern Recipe composition must declare applied_recipe: <pattern-name> in its frontmatter metadata block"
rule_id: business-domain-must-declare-applied-recipe
impact: HIGH
impactDescription: "Missing applied_recipe: declaration makes the recipe composition invisible to recipe_governance_guard.sh, breaks the audit trail linking business domains to their governing recipe, and allows ad-hoc composition to drift undetected from the recipe contract"
tags:
  - architecture
  - recipe-composition
  - metadata
  - audit-trail
  - l4-layer
provenance_class: internal_design
protects_template_id: templates/L4/<domain>/README.md
failing_fixture_path: practices/evals/fixtures/business-domain-must-declare-applied-recipe/fail_no_applied_recipe/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-002"
verification:
  type: review
  notes: |
    recipe_governance_guard.sh scans every recipes/*/RECIPE.md enabled_l4_domains list.
    For each domain listed, it reads templates/L4/<domain>/README.md and asserts
    the applied_recipe: field is present and matches the recipe pattern name.
    Missing field or wrong value → VIOLATION.
evidence:
  - source_type: external
    citation: "arc42 — Architecture Decision Records: every architectural decision must be traceable; undeclared composition cannot be verified or evolved without breaking hidden assumptions"
    url: "https://arc42.org/overview/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Spring Modulith reference — @ApplicationModule annotation makes module membership explicit and machine-verifiable; undeclared module boundaries are enforced to fail loudly"
    url: "https://docs.spring.io/spring-modulith/reference/fundamentals.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "카카오페이 기술 블로그 — 도메인 레이어 설계: 도메인 간 의존 관계를 명시적으로 선언하고 리뷰 시 추적 가능하게 유지합니다"
    url: "https://tech.kakaopay.com/post/kakaopay-msa-platform/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## business-domain-must-declare-applied-recipe

**Impact: HIGH — When an L4 domain participates in a Business Pattern Recipe, its README must carry `applied_recipe: <pattern-name>`. Without this field, `recipe_governance_guard.sh` cannot confirm the domain's wiring matches the recipe contract, and the composition drifts silently.**

The ax-template composition kit tracks which recipe governs each L4 domain via the `applied_recipe:` metadata field. This field is the single source of truth linking:
- the domain's README (human-readable entry point)
- the recipe's `enabled_l4_domains:` list (machine-readable contract)
- the guard script's validation loop

When the field is absent, the guard treats the domain as ungoverned — any composition drift goes undetected until a manual audit.

**Incorrect — L4 billing README without applied_recipe: in a saas-subscription context:**

```markdown
# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

<!-- VIOLATION: no applied_recipe: field -->
<!-- recipe_governance_guard.sh: FAIL — billing is listed in saas-subscription RECIPE.md
     enabled_l4_domains but README declares no applied_recipe -->
```

**Correct — L4 billing README with applied_recipe: declared:**

```markdown
# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

## Recipe Composition

applied_recipe: saas-subscription

<!-- recipe_governance_guard.sh: PASS — billing declares applied_recipe matching
     the recipe that lists it in enabled_l4_domains -->
```

### Where to declare

The `applied_recipe:` field belongs in the L4 domain README under a `## Recipe Composition` section. Format:

```
applied_recipe: <pattern-name>
```

Where `<pattern-name>` is the directory name under `recipes/` (e.g., `saas-subscription`, `e-commerce`, `crm`).

If a domain participates in multiple recipes, use the R6+ canonical plural form (with ≥1 list entry required — an empty `applied_recipes:` block is a violation) OR the R5 legacy multi-line form; both satisfy this rule:

```
# R6+ canonical (preferred for ≥2 recipes, alphabetically sorted):
applied_recipes:
  - e-commerce
  - saas-subscription

# R5 legacy (still valid, preserved for backward-compat):
applied_recipe: saas-subscription
applied_recipe_secondary: e-commerce
```

**Note (TD-2026-05-18-019):** Both `applied_recipe:` (R5 singular legacy) and `applied_recipes:` (R6+ plural canonical) satisfy this rule. `recipe_governance_guard.sh` accepts both forms via dual-form regex alternation. `applied_recipes:` MUST have ≥1 list item; an empty list is an explicit violation.

## Failing fixture

See: `practices/evals/fixtures/business-domain-must-declare-applied-recipe/fail_no_applied_recipe/README.md` — billing domain README without `applied_recipe:` field.

See: `practices/evals/fixtures/business-domain-must-declare-applied-recipe/pass/README.md` — billing domain README with `applied_recipe: saas-subscription`.

Reference: https://docs.spring.io/spring-modulith/reference/fundamentals.html


<!-- @source rules/cache-caffeine-expiration.md -->

---
title: Caffeine cache must declare explicit expireAfterWrite and maximumSize
impact: HIGH
impactDescription: "Without expireAfterWrite, sparse caches keep entries forever — stale data leaks across deploys and secret rotations"
tags:
  - cache
  - caffeine
  - ttl
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/io/caching.html"
evidence:
  - upstream_id: spring-boot-cache
    section: "Spring Boot — Caffeine Cache configuration"
    quote: "expireAfter"
  - source_type: external
    citation: "Spring Boot Reference — Caching with Caffeine"
    url: "https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.caffeine"
---

## Caffeine cache must declare explicit expireAfterWrite and maximumSize

**Impact: HIGH — Without expireAfterWrite, sparse caches keep entries forever — stale data leaks across deploys and secret rotations**

Caffeine has no implicit TTL. A `Caffeine.newBuilder().maximumSize(1_000).build()` keeps every entry until size pressure evicts it; for a cache that mostly holds 100 entries against a 1k cap, the *effective* TTL is "until the process restarts." That means a secret rotation doesn't take effect (the old secret is cached), a feature-flag flip doesn't take effect (the old flag is cached), and any cache poisoning incident becomes permanent until restart. Declaring `expireAfterWrite` makes time-based eviction part of the cache contract, and `maximumSize` bounds the heap footprint.

**Incorrect — no expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));   // no TTL — entries kept until size pressure
    return mgr;
}
```

**Correct — explicit expireAfterWrite + maximumSize:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))             // time-based eviction part of contract
            .maximumSize(1_000));                                // heap footprint bounded
    return mgr;
}
```

Verification: `./gradlew testPractices --tests "*CaffeineExpiration*"` asserts `CacheConfig.LOOKUP_TTL > Duration.ZERO` and `LOOKUP_MAX_SIZE > 0`.

Reference: [Spring Boot — Caching with Caffeine](https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.caffeine)


<!-- @source rules/cache-explicit-name-key-sync.md -->

---
title: "@Cacheable must declare value, key, and sync=true explicitly"
impact: HIGH
impactDescription: "Defaulted key on multi-arg methods is unstable; without sync=true a cold key suffers N-way stampede on bursts"
tags:
  - cache
  - caffeine
  - spring-cache
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: spring-cache-abstraction
    section: "Spring Framework — Cache Abstraction (@Cacheable sync)"
    quote: "sync"
  - source_type: external
    citation: "Spring Framework Reference — Cache Abstraction"
    url: "https://docs.spring.io/spring-framework/reference/integration/cache.html#cache-annotations-cacheable-synchronized"
---

## @Cacheable must declare value, key, and sync=true explicitly

**Impact: HIGH — Defaulted key on multi-arg methods is unstable; without sync=true a cold key suffers N-way stampede on bursts**

Bare `@Cacheable("cacheName")` derives the key from the method's full parameter list — that's fine for a single `Long id` argument and lethal for multi-argument methods where the key becomes unstable across parameter reordering, mutable types, or boxed/primitive mismatches. And without `sync = true`, a burst of N parallel requests for the same cold key all stampede past the cache and into the backing store; each one stores its own result, and only one ends up in the cache. Declaring `value`, `key` (SpEL), and `sync = true` makes the contract explicit and the stampede impossible.

**Incorrect — defaulted key, no sync:**

```java
@Service
public class LookupService {
    @Cacheable("practices.lookup")                   // key = all args; no sync
    public String lookup(Long id, String tenantId) {
        return loadFromDb(id, tenantId);             // stampede on cold key
    }
}
```

**Correct — explicit name + SpEL key + sync:**

```java
@Service
public class LookupService {
    @Cacheable(value = "practices.lookup", key = "#tenantId + ':' + #id", sync = true)
    public String lookup(Long id, String tenantId) {
        return loadFromDb(id, tenantId);             // sync=true serializes cold-key loads
    }
}
```

Verification: `./gradlew testPractices --tests "*ExplicitNameKey*"` reflects on `CachedLookupService.lookup` and asserts `@Cacheable.value()` is non-empty, `key()` is non-blank, `sync()` is true.

Reference: [Spring Cache Abstraction — Synchronized Caching](https://docs.spring.io/spring-framework/reference/integration/cache.html#cache-annotations-cacheable-synchronized)


<!-- @source rules/cache-not-on-controllers.md -->

---
title: "@Cacheable / @CachePut / @CacheEvict are forbidden on @RestController classes"
impact: HIGH
impactDescription: "Controller-layer caching captures request-derived state (principal, locale, headers) — cross-user response leakage"
tags:
  - cache
  - controller
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: spring-cache-abstraction
    section: "Spring Framework — Cache Abstraction usage guidance"
    quote: "Cacheable"
  - source_type: external
    citation: "Spring Framework Reference — Cache Abstraction"
    url: "https://docs.spring.io/spring-framework/reference/integration/cache.html"
---

## @Cacheable / @CachePut / @CacheEvict are forbidden on @RestController classes

**Impact: HIGH — Controller-layer caching captures request-derived state (principal, locale, headers) — cross-user response leakage**

Caching at the controller layer caches the *entire HTTP response* — but the response was built from implicit request context: the authenticated principal, the locale, headers like `Accept-Language`, and any cookies Spring forwards into the model. Two different users hitting the same path with the same path-variables produce two different responses; caching one means the *other* user can be served a response they were never authorized to see. The mechanical remedy is to forbid cache annotations at the controller layer entirely and push caching down to the service layer, where the inputs are explicit method arguments under your control.

**Incorrect — controller-layer caching:**

```java
@RestController
public class UserController {
    @Cacheable("user.profile")                       // caches response — principal-derived state leaks
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return service.profileFor(auth.getName());
    }
}
```

**Correct — caching at the service layer, controller stays uncached:**

```java
@RestController
public class UserController {
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return service.profileFor(auth.getName());   // service.profileFor() may cache safely
    }
}

@Service
public class UserService {
    @Cacheable(value = "user.profile", key = "#username", sync = true)
    public UserResponse profileFor(String username) { ... }
}
```

Verification: `./gradlew testPractices --tests "*NotOnControllers*"` runs an ArchUnit rule that asserts no `@RestController` class is annotated with `@Cacheable`, `@CachePut`, or `@CacheEvict`.

Reference: [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)


<!-- @source rules/cacheable-requires-explicit-ttl.md -->

---
title: "@Cacheable caches must have explicit TTL configured on the CacheManager"
impact: HIGH
impactDescription: "Without explicit TTL, cache entries persist until process restart — secret rotations and feature flag changes take effect only after the process is killed"
tags:
  - cache
  - ttl
  - caffeine
  - redis
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-003
failing_fixture_path: "practices/evals/fixtures/cacheable_ttl/fail_no_ttl"
passing_fixture_path: "practices/evals/fixtures/cacheable_ttl/pass"
protects_template_ids:
  - "templates/backend/cache/CaffeineConfig.java"
  - "templates/backend/cache/RedisCacheConfig.java"
upstream:
  - "https://github.com/ben-manes/caffeine/wiki/Eviction"
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: caffeine-2026-05
    section: "No Implicit TTL"
    quote: "Unlike some cache providers, Caffeine has no global default TTL. If neither expireAfterWrite nor expireAfterAccess is configured: entries are only evicted when maximumSize is exceeded"
  - upstream_id: spring-cache-2026-05
    section: "TTL / Eviction Policy"
    quote: "How can I Set the TTL/TTI/Eviction policy/XXX feature? The Spring Cache abstraction deliberately does not enforce TTL at the abstraction layer. TTL and eviction are provider-specific"
  - source_type: external
    citation: "Caffeine Wiki/Eviction — expireAfterWrite: Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value."
    url: "https://github.com/ben-manes/caffeine/wiki/Eviction"
---

## @Cacheable caches must have explicit TTL configured on the CacheManager

**Impact: HIGH — Without explicit TTL, cache entries persist until process restart — secret rotations and feature flag changes take effect only after the process is killed**

Spring's `@Cacheable` abstraction deliberately delegates TTL enforcement to the underlying provider. Neither Caffeine nor Redis applies any implicit TTL when one is not configured. A `CaffeineCacheManager` built without `expireAfterWrite` and a `RedisCacheManager` built without `entryTtl()` will both keep entries indefinitely — or until size-based eviction pressure removes them.

The practical consequences:
1. **Security:** An API key or secret cached at startup remains cached after rotation. The service keeps using the old credential until restarted.
2. **Feature flags:** A cached `false` flag value stays `false` even after the flag is flipped.
3. **Configuration:** Application configuration cached at startup becomes stale after a live config update.

**Incorrect — Caffeine without expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));
    // No expireAfterWrite — entries kept until size pressure evicts them
    return mgr;
}
```

**Incorrect — Redis without entryTtl:**

```java
@Bean
public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            // No entryTtl — entries stored with no Redis TTL, persist forever
            .build();
}
```

**Correct — Caffeine with explicit expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))  // time-based eviction is part of the contract
            .maximumSize(1_000));
    return mgr;
}
```

**Correct — Redis with explicit entryTtl:**

```java
@Bean
public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))     // explicit TTL — REQUIRED
            .disableCachingNullValues();
    return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
}
```

Use named constants for TTL values so they are visible at code-review time:

```java
public static final Duration LOOKUP_TTL = Duration.ofMinutes(5);
public static final Duration CONFIG_TTL  = Duration.ofHours(1);
```

See reference templates:
- `templates/backend/cache/CaffeineConfig.java` — process-local cache with per-cache TTL map
- `templates/backend/cache/RedisCacheConfig.java` — distributed cache with per-cache TTL map

Verification: `./gradlew testPractices --tests "*CacheableTtl*"` asserts that every `@Cacheable`-enabled `CacheManager` bean declares a non-zero TTL.

Reference: [Caffeine Wiki — Eviction](https://github.com/ben-manes/caffeine/wiki/Eviction) | [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)


<!-- @source rules/caller-authentication-only-no-userid-param.md -->

---
title: Caller identity derives from Authentication only — never accept userId via path or query
impact: HIGH
impactDescription: "Accepting userId via path/query opens structural IDOR — a bug-free check is harder than removing the parameter"
tags:
  - api
  - authz
  - idor
  - owner-scoped
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-AUTHZ-002"
verification:
  gradle_task: testFavorites
  tag: FAV-AUTHZ-002
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization (BOLA)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
    quote: "Object level authorization is an access control mechanism that is usually implemented at the code level to validate that one user can only access objects that they should have access to."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "OWASP ASVS V4.2.1 — Verify that the application uses a single vetted access control mechanism for accessing protected data and resources"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the application uses a single vetted access control mechanism for accessing protected data and resources."
    quoted_at: "2026-05-22"
---

## Caller identity derives from Authentication only — never accept userId via path or query

**Impact: HIGH — Accepting userId via path/query opens structural IDOR**

The canonical Broken Object Level Authorization (BOLA / IDOR) pattern — OWASP API Top 10's #1 risk — is endpoints that take a userId-shaped parameter and check it against the caller's authority. The check works when it works. The check fails open the moment a developer forgets to add it, mis-orders the filter chain, or accepts the parameter as a hint and trusts it elsewhere in the request flow.

The structural defense is simpler: **do not accept a userId parameter at all**. Derive the caller from `Authentication.getName()` server-side. There is no parameter for an attacker to enumerate. There is no "did we remember to check it" question because there is no input to check. This pattern is uniform across favorites (R34), activity-feed (R35), comment-thread (R36), session-management (R33), api-key management (R30), file-storage, and approval-workflow — every owner-scoped surface in the catalog.

**Incorrect — accepts userId in path, then "checks" it:**

```java
@GetMapping("/api/favorites/by-user/{userId}")
public List<Favorite> myFavorites(Authentication auth, @PathVariable String userId) {
    if (!auth.getName().equals(userId)) {
        throw new AccessDeniedException("not your favorites");
    }
    return service.list(userId);
}
```

The check is correct, but the *structure* invites failure. A second endpoint forgets the check; a code reviewer misses it; a refactor moves the path variable into the service layer where the check no longer applies.

**Correct — derive caller from Authentication, no userId parameter:**

```java
@GetMapping("/api/favorites")
public List<Favorite> myFavorites(Authentication auth) {
    return service.list(auth.getName());
}
```

There is nothing for an attacker to flip. The userId is server-side, end-to-end. Cross-user enumeration is *structurally impossible*, not just *currently checked*.

This rule applies to read endpoints, mutation endpoints, and aggregation endpoints alike. For admin endpoints that legitimately need to act on arbitrary users, use a dedicated `/api/admin/...` path gated by `hasAuthority("ROLE_ADMIN")` AND record the actor in the resource's `actedByUserId` column for audit — the admin path is the only place where another user's identifier appears.

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [OWASP ASVS V4 — Access Control](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/chunked-import-required-when-rowcount-gt-1000.md -->

---
title: CSV and Excel imports with potentially >1000 rows must use chunked streaming with per-chunk transactions
impact: HIGH
impactDescription: "Importing large files with readAll() loads the entire dataset into heap and wraps it in a single transaction, causing OOM errors and blocking rollback of earlier valid rows on late failures"
tags:
  - integration
  - performance
  - import
  - chunking
  - transaction
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-INTEG-002"
verification:
  gradle_task: testIntegration
  tag: INTEGRATION
failing_fixture_path: "practices/evals/fixtures/chunked_import/fail_no_chunk"
passing_fixture_path: "practices/evals/fixtures/chunked_import/pass"
evidence:
  - source_type: external
    citation: "OpenCSV — CSVReader.readNext() streams one row at a time from the underlying reader; CSVReader.readAll() materialises the entire file into a List<String[]> in heap memory"
    url: "https://opencsv.sourceforge.net/#reading_into_beans_by_name"
  - source_type: external
    citation: "Apache POI SXSSF API — for large Excel files, use SXSSFWorkbook (streaming read) or XSSFWorkbook with row-by-row iteration; loading all rows at once causes heap pressure above ~50k rows"
    url: "https://poi.apache.org/components/spreadsheet/how-to.html#sxssf"
  - source_type: external
    citation: "Spring Batch Reference — chunk-oriented processing: read N items, process, write, then commit; bounds memory usage to chunk size regardless of total input size"
    url: "https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing.html"
---

## CSV and Excel imports with potentially >1000 rows must use chunked streaming with per-chunk transactions

**Impact: HIGH — Importing large files with `readAll()` loads the entire dataset into heap and wraps it in a single transaction, causing OOM errors and blocking rollback of earlier valid rows on late failures**

Production CSV/Excel imports frequently exceed 10,000–100,000 rows. Two anti-patterns cause catastrophic failures at scale:

1. **`readAll()` / full-load** — `CSVReader.readAll()` and `XSSFWorkbook` sheet iteration into a `List` load all rows into heap simultaneously. A 100,000-row × 5-column file at ~200 bytes/row = 20 MB minimum; object overhead easily doubles this. Concurrent imports OOM the JVM.

2. **Single outer `@Transactional`** — wrapping the entire import in one transaction holds a DB connection open for its entire duration, blocks rollback at the row that fails (rolling back 50,000 already-saved rows), and degrades write performance due to lock accumulation.

**Required pattern:**
- Use `CSVReader.readNext()` (streaming, one row at a time) or Apache POI row-by-row iteration
- Accumulate rows into a `List<String[]>` chunk of `CHUNK_SIZE` (100–1000)
- Call a `@Transactional` method that persists the chunk and returns — this commits only those rows
- Collect row-level errors into an accumulator without aborting the batch

**Incorrect — `readAll()` + single outer `@Transactional`:**

```java
@Transactional          // VIOLATION: wraps entire import in one transaction
public ImportResult importFile(MultipartFile file) {
    List<String[]> allRows = new CSVReader(reader).readAll();  // VIOLATION: loads all rows into heap
    repository.saveAll(allRows.stream().map(this::toEntity).toList());
    return new ImportResult(allRows.size(), 0, List.of());
}
```

**Correct — streaming `readNext()` with per-chunk `@Transactional`:**

```java
public static final int CHUNK_SIZE = 500;

public ImportResult importFile(MultipartFile file) {          // no @Transactional here
    List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
    String[] row;
    while ((row = csvReader.readNext()) != null) {
        chunk.add(row);
        if (chunk.size() >= CHUNK_SIZE) {
            persistChunk(chunk, ...);    // each chunk is its own transaction
            chunk.clear();
        }
    }
    if (!chunk.isEmpty()) persistChunk(chunk, ...);
}

@Transactional                            // CORRECT: scoped to chunk only
public int persistChunk(List<String[]> rows, ...) {
    // validate + save rows; collect errors without throwing
}
```

See `templates/backend/import-export/CsvImportService.java` for the reference implementation.

Reference: [OpenCSV — Reading large CSV files with readNext()](https://opencsv.sourceforge.net/#reading_into_beans_by_name)

Reference: [Apache POI SXSSF — Streaming API for large Excel files](https://poi.apache.org/components/spreadsheet/how-to.html#sxssf)

Reference: [Spring Batch — Chunk-Oriented Processing](https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing.html)


<!-- @source rules/client-must-not-fabricate-audit-timestamps.md -->

---
title: Client must NOT fabricate audit timestamps — server is the source of truth
impact: HIGH
impactDescription: "A UI that shows a wall-clock time the server did not record creates an audit-truth mismatch that erodes incident-timeline reconstruction"
tags:
  - audit
  - forensic
  - timestamp
  - optimistic-update
spec_ref: "specs/activity-feed-l0.yaml#ACT-READ-001"
verification:
  source: "templates/L4/activity-feed/app/(activities)/page.tsx"
  pattern: "pendingReadIds Set<string> in component state — cache only ever carries backend's readAt or null; no `new Date().toISOString()` written into cache"
upstream:
  - "https://gdpr-info.eu/art-5-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 5(1)(d) — Personal data shall be accurate"
    url: "https://gdpr-info.eu/art-5-gdpr/"
    quote: "Personal data shall be: accurate and, where necessary, kept up to date; every reasonable step must be taken to ensure that personal data that are inaccurate, having regard to the purposes for which they are processed, are erased or rectified without delay (accuracy)."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V8 — Data Protection (logging accuracy + integrity)"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that authentication and session events are logged including admin login, user login, password change, and other security-relevant events."
    quoted_at: "2026-05-25"
---

## Client must NOT fabricate audit timestamps — server is the source of truth

**Impact: HIGH — a UI that lies about *when* eats into incident timeline integrity**

When the client optimistically updates state for a mutation that has an audit timestamp (`readAt`, `actedAt`, `deletedAt`, `revokedAt`, `acknowledgedAt`), it is tempting to immediately set `field = new Date().toISOString()` so the row reflects the action without waiting for the server response. **Do not do this.** The server's actual timestamp will differ — by the network round-trip, by clock skew if the client's wall clock is off, by intentional client tampering. The audit log holds the server's timestamp; the screenshot the user takes shows the client's. When those diverge in an incident-response review, the system's trust posture collapses: *the UI showed me one time, the log shows another, which is the real evidence?*

Two narrow classes of timestamp can be client-rendered safely:
1. **"as-of" relative labels with no absolute claim** — e.g. `"just now"`, `"a moment ago"` rendered from a `pending: boolean` flag. These do not claim to be the audit time.
2. **Display-only formatting of a server-returned timestamp** — once the server has responded with the canonical value, the client may format it (`new Date(serverIso).toLocaleString(...)`).

**The forbidden pattern: write `new Date()` into the cache as if it were the server's authoritative timestamp.**

**Incorrect — fabricated optimistic timestamp:**

```ts
// ❌ Fabricated readAt — client clock, not server truth.
const read = useMutation({
  mutationFn: markRead,
  onSuccess: (_void, id) => {
    qc.setQueryData(['activity-feed'], (old) => ({
      ...old,
      items: old.items.map((e) =>
        e.id === id ? { ...e, readAt: new Date().toISOString() } : e,
      ),
    }))
  },
})

// Renderer displays a time the server may never have stored:
<span>read {timeAgo(event.readAt, now)}</span>
```

**Correct — typed pending set in component state, cache only ever holds backend truth:**

```ts
const [pendingReadIds, setPendingReadIds] = React.useState<Set<string>>(() => new Set())

const read = useMutation({
  mutationFn: markRead,             // returns the backend's authoritative readAt
  onMutate: (id) => {
    setPendingReadIds((prev) => new Set(prev).add(id))
  },
  onSettled: (_data, _err, id) => {
    setPendingReadIds((prev) => {
      const next = new Set(prev)
      next.delete(id)
      return next
    })
  },
  onSuccess: () => {
    // Family-key invalidate so the next refetch carries the server's readAt.
    qc.invalidateQueries({ queryKey: ['activity-feed'] })
  },
})

// Renderer: "marking read…" while pending, then the server's real timestamp:
const isPendingRead = isUnread && pendingReadIds.has(event.id)
{isPendingRead
  ? '· marking read…'
  : event.readAt && `· read ${timeAgo(event.readAt, now)}`}
```

Apply this rule to any timestamp field that ends up in an audit query or a compliance export: `readAt`, `actedAt`, `submittedAt`, `cancelledAt`, `revokedAt`, `approvedAt`, `rejectedAt`, `acknowledgedAt`, `verifiedAt`. The bias toward "show something instantly" is real and important — solve it with a pending sentinel and a `"…ing"` label, not with `new Date()`.

Reference: [GDPR Article 5 — Lawfulness, fairness, accuracy](https://gdpr-info.eu/art-5-gdpr/)

Reference: [OWASP ASVS V8 — Data Protection & Logging](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/config-no-secret-in-yaml.md -->

---
title: Never hardcode secrets in application.yml; use ${ENV[:default]}
impact: HIGH
impactDescription: "A hardcoded secret in yaml is one git-log search away from credential leak"
tags:
  - config
  - security
  - secrets
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/external-config.html"
evidence:
  - upstream_id: spring-boot-external-config
    section: "Spring Boot — Externalized Configuration (env-var placeholders)"
    quote: "@Value"
  - source_type: external
    citation: "OWASP Cheat Sheet — Secrets Management"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html"
---

## Never hardcode secrets in application.yml; use ${ENV[:default]}

**Impact: HIGH — A hardcoded secret in yaml is one git-log search away from credential leak**

`client-secret: hunter2` in `application.yml` is a credential leak the moment the file lands in git. Public mirrors get scraped within minutes, internal forks land in archived repos forever, and the credential has to be rotated everywhere it was used — across all environments, all consumers — before the leak is contained. The mechanical remedy is universal: route every sensitive key through an environment variable placeholder. Default-only values (`dummy-foo` / `changeme`) are acceptable in committed config so a clone runs out of the box; the real secret is supplied at runtime through the env var.

**Incorrect — literal secret in committed yaml:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: real-client-id-1234
            client-secret: real-secret-abcdef          # leaked the moment it lands in git
```

**Correct — env-var placeholder with safe default:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:dummy-google-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:dummy-google-secret}
```

Verification: `./gradlew testPractices --tests "*NoSecretInYaml*"` scans `application.yml` line by line, applies a sensitive-key regex (`client-secret`, `api-key`, `access-token`, `jwt-secret`, `encryption-key`, `webhook-secret`), and rejects any line whose value is a non-empty literal that does not start with `${`.

Reference: [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)


<!-- @source rules/config-profile-isolation.md -->

---
title: Move profile-specific config out of application.yml into application-{profile}.yml
impact: MEDIUM
impactDescription: "Profile-gated blocks in one big yaml file are unauditable per environment"
tags:
  - config
  - profiles
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/profiles.html"
evidence:
  - upstream_id: spring-boot-profiles
    section: "Spring Boot — Profile-specific Files"
    quote: "spring.profiles"
  - source_type: external
    citation: "Spring Boot Reference — Profile-specific Configuration Files"
    url: "https://docs.spring.io/spring-boot/reference/features/profiles.html"
---

## Move profile-specific config out of application.yml into application-{profile}.yml

**Impact: MEDIUM — Profile-gated blocks in one big yaml file are unauditable per environment**

Spring Boot supports two profile-config strategies: separate `application-{profile}.yml` files (one per environment), or inline gated documents inside `application.yml` using `spring.config.activate.on-profile`. The inline form encourages cramming every environment's keys into the base file behind conditionals — and after a few rotations no human can tell which keys actually apply where. Worse, a stale or mis-typed condition silently leaks dev / staging behaviour into prod. The mechanical remedy is to put each environment's keys in its own file (`application-prod.yml`, `application-dev.yml`, `application-test.yml`) and keep the base `application.yml` profile-agnostic.

**Incorrect — environment-specific blocks gated inline:**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:test

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://prod-db:5432/app
```

**Correct — separate file per profile:**

```yaml
# application.yml — profile-agnostic
spring:
  application:
    name: my-app

# application-prod.yml — only loaded when prod profile is active
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/app
```

Verification: `./gradlew testPractices --tests "*ProfileIsolation*"` reads `application.yml` and asserts the body does not contain `spring.config.activate.on-profile` or the legacy `on-profile:` key.

Reference: [Spring Boot — Profile-specific Configuration Files](https://docs.spring.io/spring-boot/reference/features/profiles.html#features.profiles.specific)


<!-- @source rules/config-typed-properties.md -->

---
title: Bind config through @ConfigurationProperties records, not @Value
impact: HIGH
impactDescription: "One typed contract beats scattered untyped string injections"
tags:
  - config
  - configuration-properties
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CONFIG-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CONFIG-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/features/external-config.html"
evidence:
  - upstream_id: spring-boot-external-config
    section: "Spring Boot — Type-safe Configuration Properties"
    quote: "@ConfigurationProperties"
  - source_type: external
    citation: "Spring Boot Reference — Type-safe Configuration Properties"
    url: "https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties"
---

## Bind config through @ConfigurationProperties records, not @Value

**Impact: HIGH — One typed contract beats scattered untyped string injections**

`@Value("${smtp.host}")` injects one config key at one field. Multiply that by twenty fields and the contract for what `smtp.*` looks like lives nowhere — each field is its own undocumented binding. `@ConfigurationProperties` records collect the whole namespace into one immutable type. Spring Boot validates the binding at startup, the IDE refactors every callsite atomically when a field is renamed, and tests instantiate the record with plain `new`.

**Incorrect — scattered @Value injection:**

```java
@Service
public class SmtpSender {
    @Value("${smtp.host}") private String host;
    @Value("${smtp.port:587}") private int port;
    @Value("${smtp.username}") private String username;
    // ... no single source of truth, mutable fields, untyped binding
}
```

**Correct — @ConfigurationProperties record:**

```java
@ConfigurationProperties("smtp")
public record SmtpProperties(String host, int port, String username) {
    public SmtpProperties {
        if (port <= 0) port = 587;
    }
}

@Service
public class SmtpSender {
    private final SmtpProperties props;
    public SmtpSender(SmtpProperties props) { this.props = props; }
}
```

Verification: `./gradlew testPractices --tests "*TypedProperties*"` runs an ArchUnit rule that rejects any `@Value`-annotated field in the practices/ subtree, plus a reflective check that the `PracticesAppProperties` fixture is a record with an explicit `@ConfigurationProperties` namespace.

Reference: [Spring Boot — Type-safe Configuration Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties)


<!-- @source rules/core-aop-proxy-no-final.md -->

---
title: Do not mark proxied beans (or their public methods) as final
impact: HIGH
impactDescription: "CGLIB cannot subclass a final type — @Transactional / @Async advice is silently dropped"
tags:
  - core
  - aop
  - proxy
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/aop/proxying.html"
evidence:
  - upstream_id: spring-aop-proxying
    section: CGLIB subclass-based proxying restrictions
    quote: dynamic proxies or CGLIB to create the proxy for a given target object. JDK dynamic proxies are built into the JDK, whereas CGLIB is a common open-source class definition library (repackaged into spring-core ). If the target object to be proxied implements at
  - source_type: external
    citation: 'Spring Framework Reference — §Proxying mechanisms (CGLIB final-class restriction)'
    url: 'https://docs.spring.io/spring-framework/reference/core/aop/proxying.html'
  - source_type: external
    citation: "Baeldung — Spring's CGLIB Proxy Limitations"
    url: 'https://www.baeldung.com/spring-aop-vs-aspectj#proxy-types'
---

## Do not mark proxied beans (or their public methods) as final

**Impact: HIGH — CGLIB cannot subclass a final type — @Transactional / @Async advice is silently dropped**

Spring applies AOP advice (`@Transactional`, `@Async`, `@Cacheable`, custom aspects) by wrapping the bean in a CGLIB-generated subclass that overrides the public methods. When the bean's class is `final` — or a public method is `final` — CGLIB cannot subclass / override, the proxy is not produced, and the annotation is effectively a no-op. The bug is silent: no exception at startup, no log line at the call site, just missing advice.

**Incorrect — final on the bean class:**

```java
@Service
public final class ReportService {       // CGLIB cannot subclass — @Transactional dropped
    @Transactional
    public void persist() { ... }
}
```

**Correct — non-final class and non-final public methods:**

```java
@Service
public class ReportService {              // subclass-able by CGLIB; @Transactional honored
    @Transactional
    public void persist() { ... }
}
```

Verification: `./gradlew testPractices --tests "*AopFinalClass*"` asserts `AopUtils.isAopProxy(bean)` is true and that neither the class nor its public methods are final.

Reference: [Spring Framework — Proxying mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)


<!-- @source rules/core-constructor-injection.md -->

---
title: Use constructor injection with final fields
impact: HIGH
impactDescription: "Surfaces missing/circular dependencies at construction time; enables immutability + plain-JUnit tests"
tags:
  - core
  - di
  - immutability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection"
evidence:
  - upstream_id: spring-beans-constructor-injection
    section: Constructor-based Dependency Injection
    quote: 'dency-injected with constructor injection: Java Kotlin public class SimpleMovieLister { // the SimpleMovieLister has a dependency on a MovieFinder private final MovieFinder movieFinder; // a constructor so that the Spring container can inject a MovieFinder pub'
  - source_type: external
    citation: 'Spring Framework Reference — §Constructor-based Dependency Injection'
    url: 'https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection'
  - source_type: external
    citation: 'Spring Team Blog — Why we changed @Autowired on constructors recommendation (Sep 2016)'
    url: 'https://spring.io/blog/2016/03/04/core-container-refinements-in-spring-framework-4-3'
---

## Use constructor injection with final fields

**Impact: HIGH — Surfaces missing/circular dependencies at construction time; enables immutability + plain-JUnit tests**

Field injection puts an `@Autowired` annotation on a non-final field, leaving the dependency mutable, mockable only via reflection, and silent about circular dependencies until the application starts. Constructor injection declares the same dependency as a `final` field initialized in the constructor — the class cannot be instantiated without it, the dependency cannot be reassigned, and unit tests instantiate the bean with plain `new`.

**Incorrect — field injection:**

```java
@Service
public class FieldInjectedService {
    @Autowired                       // requires reflection in tests; field is non-final
    private ParentRepository parents;

    public long countParents() {
        return parents.count();
    }
}
```

**Correct — constructor injection with `final`:**

```java
@Service
public class ConstructorInjectedService {
    private final ParentRepository parents;

    public ConstructorInjectedService(ParentRepository parents) {
        this.parents = parents;
    }

    public long countParents() {
        return parents.count();
    }
}
```

Verification: `./gradlew testPractices --tests "*ConstructorInjection*"` asserts the correct fixture has a `final` field and a matching single-arg constructor; the anti-pattern fixture has a non-final field.

Reference: [Spring Framework — Constructor-based DI](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection)


<!-- @source rules/core-singleton-no-mutable-state.md -->

---
title: Singleton beans must not carry unsynchronized mutable state
impact: HIGH
impactDescription: "Default singleton scope + plain int/HashMap = silent lost updates under concurrency"
tags:
  - core
  - concurrency
  - thread-safety
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html"
evidence:
  - upstream_id: spring-beans-scopes
    section: Singleton scope semantics in Spring beans
    quote: s Scope Description singleton (Default) Scopes a single bean definition to a single object instance for each Spring IoC container. prototype Scopes a single bean definition to any number of object instances. request Scopes a single bean definition to the lifec
  - source_type: external
    citation: 'Spring Framework Reference — §Bean scopes (singleton default, thread-safety implications)'
    url: 'https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html'
  - source_type: external
    citation: 'Java Concurrency in Practice (Goetz et al., 2006) — §Chapter 3: Sharing Objects'
    url: 'https://jcip.net/'
---

## Singleton beans must not carry unsynchronized mutable state

**Impact: HIGH — Default singleton scope + plain int/HashMap = silent lost updates under concurrency**

Spring's default scope is singleton. A `@Component` with a plain mutable field is one shared instance across all requests; `int count; count++;` is a read-modify-write that races. The result is silent: counters drift, caches miss, audit logs lose entries, with no exception to point at. Either make the state thread-safe (`AtomicLong`, `ConcurrentHashMap`, immutable copies), guard it explicitly, or change the scope (`@Scope("prototype")` per request).

**Incorrect — singleton with unsynchronized mutation:**

```java
@Component
public class MutableSingletonCounter {
    private int count;                    // shared mutable state, races on increment
    public void increment() { count++; }
    public int get() { return count; }
}
```

**Correct — atomic primitive guards the read-modify-write:**

```java
@Component
public class AtomicSingletonCounter {
    private final AtomicLong count = new AtomicLong();
    public void increment() { count.incrementAndGet(); }
    public long get() { return count.get(); }
}
```

Verification: `./gradlew testPractices --tests "*SingletonState*"` runs 32 × 1000 concurrent increments and asserts the atomic counter is exactly equal to the expected total; the unsynchronized counterpart is bounded above by the same total and typically loses updates on real hardware.

Reference: [Spring Framework — Bean scopes](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html)


<!-- @source rules/currency-amount-precision-explicit.md -->

---
title: "All monetary amounts in billing domain must be stored as long integer minor units; float, double, and BigDecimal representations are prohibited"
rule_id: currency-amount-precision-explicit
impact: CRITICAL
impactDescription: "float/double representation of monetary amounts causes silent rounding errors (e.g., 10.1 KRW stored as 10.099999...). BigDecimal is verbose and mutation-prone. Stripe and Toss both use integer minor units as their canonical wire format."
tags:
  - billing
  - currency
  - precision
  - integer-minor-units
provenance_class: internal_design
protects_template_id: templates/backend/billing/Plan.java
failing_fixture_path: practices/evals/fixtures/currency-amount-precision/fail_float_amount/
spec_ref: "specs/billing-l0.yaml#BILLING-CUR-001"
verification:
  type: archunit
  notes: |
    ArchUnit rule:
    fields().that().areDeclaredInClassesThat().resideInAPackage("..billing..")
    .and().haveNameMatching(".*[Aa]mount.*|.*[Pp]rice.*|.*[Ff]ee.*|.*[Cc]ost.*")
    .should().haveRawType(long.class)
    Controller validation:
    POST endpoints that accept amount fields use @RequestBody with a record type;
    if the field is typed as double/float in the JSON, Jackson rejects it with 400.
    Failing fixture: any billing entity field named *amount*/*price*/*fee*/*cost* typed as double/float/BigDecimal.
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Amounts and currencies"
    quote: "All amounts are stored in the smallest currency unit (e.g., 100 cents to charge $1.00). For zero-decimal currencies such as JPY or KRW, use the amount directly (e.g., 150 to charge ¥150)."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "금액 단위"
    quote: "amount 필드는 항상 정수(원 단위)로 전달합니다. 소수점 금액은 허용하지 않습니다."
  - source_type: external
    citation: "Martin Fowler — Money pattern: store amounts as integer minor units to avoid floating-point rounding; pair with a Currency object for formatting."
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## All monetary amounts must be long integer minor units

**Impact: CRITICAL — float/double amounts silently accumulate rounding errors. A 0.1 KRW float error compounded over 1,000 invoices is 100 KRW gone. Stripe and Toss both define integer minor units as canonical. This template enforces the same.**

Both Stripe and Toss Payments use integer minor-unit amounts as their canonical wire format:
- KRW (South Korean Won): no subdivisions — 1,000 KRW = `1000` (long)
- USD (US Dollar): cents — $10.00 = `1000` (long, cents)
- JPY: no subdivisions — ¥150 = `150` (long)

### What "minor units" means

| Currency | Decimal | Minor units (long) |
|---|---|---|
| KRW ₩10,000 | 10000.00 | `10000L` |
| USD $9.99 | 9.99 | `999L` |
| EUR €4.50 | 4.50 | `450L` |

**Incorrect — float or BigDecimal storage for monetary amounts:**

```java
// VIOLATION: float causes rounding loss on any non-exact binary fraction
private float amount;  // 10.1 stored as 10.09999942779541 (IEE 754)
// VIOLATION: BigDecimal is verbose and mutation-prone
private BigDecimal amountDue;
// VIOLATION: double — same rounding problem as float
private double price;
```

**Correct — long integer minor units for all monetary amounts:**

```java
// CORRECT: long, minor units — KRW 10,000원 stored as 10000L
private long amount;
// CORRECT: Invoice.java — both fields as long
private long amountDue;
private long amountPaid;
```

Reference: https://martinfowler.com/eaaCatalog/money.html

### Correct — rejecting float inputs at the HTTP boundary

```java
// BillingController.java — CreateSubscriptionRequest record
// amount is declared as long; if client sends 9.99, Jackson throws 400
record CreateSubscriptionRequest(
    @NotNull UUID planId,
    @NotBlank String provider
) {}

// PlanController.java — CreatePlanRequest record
record CreatePlanRequest(
    @NotBlank String name,
    @Positive long amount,  // rejects float JSON with 400 ProblemDetail
    @NotBlank String currency,
    @NotBlank String billingCycle
) {}
```

### Display formatting

For display, convert minor units to decimal in the frontend using `CurrencyFormatter` (L1):

```typescript
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

// KRW: no decimal places
formatCurrencyAmount(10000, 'KRW', 'ko-KR') // → "₩10,000"

// USD: two decimal places
formatCurrencyAmount(999, 'USD', 'en-US') // → "$9.99"
```

**Never convert back to float/double for storage or computation.** All arithmetic (discounts, proration) stays in long arithmetic.

## ArchUnit enforcement

```java
// CurrencyAmountPrecisionArchTest.java
@ArchTest
static final ArchRule billingAmountFieldsMustBeLong = fields()
    .that().areDeclaredInClassesThat().resideInAPackage("..billing..")
    .and().haveNameMatching(".*[Aa]mount.*|.*[Pp]rice.*|.*[Ff]ee.*|.*[Cc]ost.*")
    .should().haveRawType(long.class)
    .because("All monetary amounts in billing domain must be long integer minor units");
```

## Failing fixture

See: `practices/evals/fixtures/currency-amount-precision/fail_float_amount/BillingPlanFloatAmount.java` — a Plan entity with `private double amount`.

See: `practices/evals/fixtures/currency-amount-precision/pass_integer_amount/BillingPlanLongAmount.java` — correct `private long amount`.


<!-- @source rules/destructive-action-confirm-with-side-effects.md -->

---
title: Destructive admin actions MUST confirm with explicit side-effect enumeration
impact: HIGH
impactDescription: "A bare-onClick destructive action under pager-driven triage produces single-misclick incidents (duplicate deliveries, voided approvals, lost notes) — the confirm copy must spell out which side effects will happen"
tags:
  - admin
  - destructive-action
  - confirm-dialog
  - incident-prevention
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L4/scheduled-task/app/(admin)/scheduled-tasks/page.tsx, templates/L4/favorites-bookmarks/app/(favorites)/page.tsx"
  pattern: "window.confirm with verbatim enumeration of downstream side effects (HTTP POST to partner / db writes / notifications / audit-trail invalidation / quota voided) BEFORE the mutation fires"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 3.3.4 Error Prevention (Legal, Financial, Data) (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html"
    quote: "For Web pages that cause legal commitments or financial transactions for the user to occur, that modify or delete user-controllable data in data storage systems, or that submit user test responses, at least one of the following is true: submissions are reversible, data is checked for input errors, or a mechanism is available for reviewing, confirming, and correcting information before finalizing the submission."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V14.3 — Unintended Security Disclosure / Error Prevention"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify the application has defenses against destructive operations being performed without intent, including but not limited to confirmation prompts for irreversible changes."
    quoted_at: "2026-05-25"
---

## Destructive admin actions MUST confirm with explicit side-effect enumeration

**Impact: HIGH — a bare onClick on a destructive admin action turns one misclick into one incident**

Webhook delete / replay, scheduled-task trigger, mark-all-read, favorite-remove-with-note, approval-cancel-with-priors — these all have downstream side effects that are not trivially reversible:

- Webhook **replay** sends another POST to a partner. The partner may not implement idempotency. Duplicate side effects (double charge, double notification, double inventory move) cascade.
- Scheduled-task **trigger** runs the cron job out of cycle. The job fires its own POST/email/db writes as if scheduled.
- **Mark-all-read** on a notification surface clears server-side audit fact "did the operator actually read this?" — even when the inbox was wrong.
- **Delete favorite** with a note destroys the note (often a Korean enterprise 결재 / follow-up context).
- **Cancel approval** with one or more upstream approvals already granted voids those decisions in the audit trail.

The catalog convention since R43 / R46 / R48 / R49 is: confirm with **verbatim enumeration of the consequences**, not a generic "Are you sure?". The operator needs to read the side effects in the dialog so a 3am-pager-fatigue mind can stop before the click.

**Incorrect — bare onClick with no consequence enumeration:**

```tsx
<button
  type="button"
  aria-busy={replay.isPending || undefined}
  aria-disabled={replay.isPending || undefined}
  onClick={() => {
    if (replay.isPending) return
    replay.mutate(delivery.id)
  }}
>
  Replay
</button>
```

A pager-driven SRE during incident response can misclick this. The aria-busy + click guard prevents double-fire mid-flight (R47 rule), but the operator's intent is not verified before the first click commits. Replay fires immediately. The partner endpoint receives a duplicate delivery. There is no recovery from the partner side.

**Correct — confirm with side-effect enumeration:**

```tsx
<button
  onClick={() => {
    const ok = window.confirm(
      `Re-enqueue this delivery?\n\n${delivery.eventType} (attempt ${delivery.attemptCount})\nendpoint ${delivery.endpointId}\n\nThis sends another HTTP POST to the partner endpoint. If the original eventually succeeded server-side, the partner receives duplicate side effects.`,
    )
    if (!ok) return
    replay.mutate(delivery.id)
  }}
>
  Replay
</button>
```

The dialog text MUST satisfy three properties:
1. **Name the action** in past tense framing of consequence ("Re-enqueue this delivery?" not "Are you sure?")
2. **Show identifying context** for the specific object (event type, request id, approver chain, etc.) — so an operator with multiple windows knows which row this refers to
3. **List the side effects** as plain sentences. Korean enterprise partners frequently lack idempotent receivers; financial side effects (PG, inventory, billing) cascade

`window.confirm` is the catalog baseline — a fork-receiver may replace it with a styled Dialog primitive, but the three properties survive the swap. Native `confirm` is a11y-degraded vs a styled modal (separate rule `mutation-in-flight-uses-aria-busy` covers the in-flight state), but for the one-shot destructive-confirm path it is the lowest-common-denominator that catches misclick.

**When to apply this rule**: any mutation where (a) the server commits an irreversible side effect, (b) the side effect cascades to a third party (partner endpoint, downstream system, audit log), OR (c) reversibility requires multi-party coordination (re-issue a webhook secret + notify all downstream verifiers, re-file a cancelled approval, restore a deleted comment with note). For (a)+(b)+(c) any single condition triggers the rule.

**When NOT to apply**: trivially-reversible actions (toggle favorite, mark single notification read, edit a draft) — confirm there adds friction without preventing meaningful loss.

Reference: [WCAG 2.2 SC 3.3.4 — Error Prevention (Legal, Financial, Data)](https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html)

Reference: [OWASP ASVS V14.3 — Error Prevention](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/dogfood-finding-must-have-expiry-trigger.md -->

---
title: Dogfood-ledger scope_deferral findings MUST include an explicit expiry trigger
impact: MEDIUM
impactDescription: "A scope_deferral entry without a concrete re-open condition risks becoming permanent, unreviewable technical debt: the catalog cannot mechanically detect when the underlying constraint changes (cap-bump, new audit emission, new entity domain) and the deferral silently outlives its rationale. Catalog quality regresses one ledger entry at a time."
tags:
  - dogfood
  - ledger
  - catalog-quality
  - scope-deferral
  - expiry-trigger
  - technical-debt
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-DOGFOOD-LEDGER-001"
verification:
  source: "practices/evals/dogfood_finding_expiry_trigger_guard.sh (R85b — 45th hard guard)"
  pattern: "Every docs/dogfood-ledger/*.yaml entry where classification=scope_deferral MUST contain at least one of the anchored expiry-trigger marker phrases in its finding text: 'expiry trigger:', 're-opens when', 're-opens before', 'reopens when', 'reopens before', 'defer until', 'deferred until', 'expires on', 'sunsets on', 'before the fork-receiver', 'before the first', 'before the cap'. Bare 'before a' / 'before any' are intentionally excluded as too lenient (accidental prose can match). Markers are case-insensitive substring matches."
upstream:
  - "https://martinfowler.com/bliki/TechnicalDebtQuadrant.html"
  - "https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final"
evidence:
  - source_type: external
    citation: "Martin Fowler — TechnicalDebtQuadrant: 'The prudent debt example is deliberate because the team knows they are taking on a debt, and thus puts some thought as to whether the payoff for an earlier release is greater than the costs of paying it off.' Applied to dogfood scope_deferrals: a deferral is the explicit catalog act of taking on prudent-deliberate debt, so Fowler's 'thought about the payoff' obligation translates directly to a recorded re-open condition. Without it, the entry slides into the inadvertent quadrant — debt the team no longer remembers it is carrying."
    url: "https://martinfowler.com/bliki/TechnicalDebtQuadrant.html"
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "NIST SP 800-53 Rev. 5 — Control RA-7 Risk Response, control statement (verbatim): 'Respond to findings from security and privacy assessments, monitoring, and audits in accordance with organizational risk tolerance.' RA-7 distinguishes two response shapes the catalog should not conflate — (i) mitigation that is deferred generates a Plan of Action and Milestones tracking the future close, and (ii) acceptance requires recorded justification anchored to organizational risk tolerance. Applied to a dogfood scope_deferral, the catalog is choosing the acceptance shape (no mitigation planned at this layer), and the expiry trigger documents the future condition under which the acceptance should be re-assessed against that tolerance posture."
    url: "https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final"
    quoted_at: "2026-05-26"
---

## Dogfood-ledger scope_deferral findings MUST include an explicit expiry trigger

**Impact: MEDIUM — defer-without-trigger is invisible technical debt that the catalog cannot self-audit.**

R71 `dogfood_ledger_guard.sh` mechanically enforces ledger structure (iteration, persona, finding, classification, references_artifact_path). It does NOT enforce content quality — specifically, a `scope_deferral` entry can ship with reasoning like "fork-receiver decides" and pass the guard, even though that reasoning gives the catalog no way to mechanically detect when the deferral should re-open.

This rule closes that gap: every `scope_deferral` entry MUST include an explicit re-open condition. A reader (or a future maintainer scanning the ledger) MUST be able to answer "what would make this no longer deferred?" by reading the finding text alone.

**Two acceptable trigger shapes:**

1. **Explicit phrase**: `"expiry trigger: <condition>"` or `"re-opens when <condition>"` or `"re-opens before <condition>"`.
2. **Inline "before X" pattern**: a sentence containing `"before <fork-receiver action>"` where the action is concrete (cap bump, first audit-log emission, first PII-linked entity wired, etc.).

The guard is intentionally lenient on phrasing — any one of the above patterns satisfies it. The strictness is on **presence**, not form.

**Incorrect — defer with no re-open condition:**

```yaml
- persona: P2
  finding: "F7: body column is TEXT plain; fork-receiver-owned decision (catalog refuses to choose Hibernate @ColumnTransformer / pgcrypto / RDS at-rest)"
  classification: scope_deferral
```

The reader knows the catalog deferred, but cannot tell when the deferral should re-open. Is it when fork-receiver enables column-at-rest encryption? When the body content gets a new field that's clearly sensitive? When a compliance audit flags it? The text is silent.

**Correct — explicit "before" trigger:**

```yaml
- persona: P2
  finding: "F7: body column is TEXT plain; fork-receiver-owned decision (catalog refuses to choose Hibernate @ColumnTransformer / pgcrypto / RDS at-rest). Expiry trigger: re-opens before the fork-receiver renders ANY user-typed prose into the body template (template engine variable substitution from a user-controlled field), because at that point a verbatim-stored body crosses into the PII surface that demands at-rest encryption."
  classification: scope_deferral
```

Reader now knows: the deferral persists while body content is system-generated transactional text; it re-opens the moment user-typed content enters the template.

**Apply this rule to**: every entry in `docs/dogfood-ledger/*.yaml` whose `classification` is `scope_deferral`.

**When NOT to apply**: entries classified as `real_bug` (those are closed in the same wave per the dogfood protocol) or `methodology_gap` (those should be addressed by changing the methodology, not deferred indefinitely). Only `scope_deferral` carries the trigger requirement.

A pair-with rule: R71 `dogfood_ledger_guard` already enforces the classification schema. R85 is the content-quality layer on top — same ledger, finer-grained discipline.

Reference: [Martin Fowler — Technical Debt Quadrant](https://martinfowler.com/bliki/TechnicalDebtQuadrant.html)

Reference: [NIST SP 800-53 Rev. 5 — RA-7 Risk Response](https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final)


<!-- @source rules/dogfood-finding-real-bug-must-reference-closure-commit.md -->

---
title: Dogfood-ledger real_bug findings MUST reference closure_commit_sha
impact: MEDIUM
impactDescription: "A real_bug entry without a recorded closure_commit_sha leaves the ledger ↔ git boundary one-way: the prose says 'Closure: X' but a future maintainer cannot mechanically verify which git revision actually landed the fix. The R71 ledger-guard enforces the classification field; R85 enforces re-open conditions on scope_deferral entries; R86 closes the symmetric gap on real_bug entries."
tags:
  - dogfood
  - ledger
  - catalog-quality
  - real-bug
  - closure-traceability
  - git
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-DOGFOOD-LEDGER-002"
verification:
  source: "practices/evals/dogfood_finding_real_bug_closure_commit_guard.sh (R86b — 46th hard guard)"
  pattern: "Every docs/dogfood-ledger/*.yaml entry where classification=real_bug MUST carry a closure_commit_sha field whose value (a) is non-empty, (b) matches ^[0-9a-f]{7,40}$, AND (c) resolves to an existing commit in the local repository (git cat-file -e <sha>^{commit})."
upstream:
  - "https://www.kernel.org/doc/html/latest/process/submitting-patches.html"
  - "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue"
evidence:
  - source_type: external
    citation: "Linux Kernel — Submitting Patches, Fixes: trailer convention (verbatim): 'If your patch fixes a bug in a specific commit, e.g. you found an issue using git bisect, please use the Fixes: tag with at least the first 12 characters of the SHA-1 ID, and the one line summary.' The kernel community has used this convention for over a decade to make bug-fix commits programmatically linkable to the commits they fix. R86 applies the same intent to the catalog's dogfood ledger: a real_bug finding's closure_commit_sha is the catalog's equivalent of a Fixes: trailer, anchoring the ledger entry to a verifiable git revision."
    url: "https://www.kernel.org/doc/html/latest/process/submitting-patches.html"
    quoted_at: "2026-05-27"
  - source_type: external
    citation: "GitHub Docs — Linking a pull request to an issue (verbatim): 'You can also use closing keywords in a commit message. The issue will be closed when you merge the commit into the default branch, but the pull request that contains the commit will not be listed as a linked pull request.' The supported keywords are 'close, closes, closed, fix, fixes, fixed, resolve, resolves, resolved'. The traceability shape is the same one the catalog wants: a fix lives in a commit, and the issue / finding records the commit that closed it."
    url: "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue"
    quoted_at: "2026-05-27"
---

## Dogfood-ledger real_bug findings MUST reference closure_commit_sha

**Impact: MEDIUM — without a recorded closure SHA, the ledger and git history drift apart and the catalog cannot self-verify its own closures.**

R71 `dogfood_ledger_guard.sh` enforces the classification field on every finding (real_bug / scope_deferral / methodology_gap). R85 enforces an explicit re-open condition on scope_deferral entries. R86 closes the symmetric gap on real_bug entries: every closure MUST carry a `closure_commit_sha` field whose value is a real, resolvable git commit hash.

The prose-only "Closure: …" pattern that the catalog has used so far (e.g., "Closure: caught DataIntegrityViolationException in FavoriteController.handleConcurrentDuplicate") tells the reader WHAT changed but does not tell them WHICH commit landed the change. A future maintainer reading the ledger six months later cannot mechanically confirm the fix shipped — they must search git history by hand, infer from commit messages, and hope no later commit undid the work.

The fix shape mirrors the Linux Kernel's `Fixes:` trailer + GitHub's closing-keywords convention: pin every closure to a specific git revision so the ledger ↔ git boundary is bidirectional.

**Incorrect — closure described in prose, no SHA:**

```yaml
- persona: P1
  finding: "F12: processQueue summary AUDIT.info only fires when processed > 0... Closure: AUDIT.debug 'verb=PROCESS_QUEUE_EMPTY total=0' on empty branch."
  classification: real_bug
  references_artifact_path: backend/src/main/java/.../EmailOutboxService.java
```

The closure prose is informative but the ledger cannot answer "did this actually land?" without a git search.

**Correct — closure_commit_sha pins the closure:**

```yaml
- persona: P1
  finding: "F12: processQueue summary AUDIT.info only fires when processed > 0... Closure: AUDIT.debug 'verb=PROCESS_QUEUE_EMPTY total=0' on empty branch."
  classification: real_bug
  closure_commit_sha: b475685
  references_artifact_path: backend/src/main/java/.../EmailOutboxService.java
```

Reader and guard can now both confirm the closure: `git show b475685` shows the actual diff.

**Apply this rule to**: every `real_bug` entry in `docs/dogfood-ledger/*.yaml`. The SHA may be short (≥ 7 hex chars) or full (40 hex chars).

**When NOT to apply**: entries classified as `scope_deferral` (those carry expiry triggers per R85) or `methodology_gap` (those drive methodology change, not a single closure commit). Only `real_bug` carries the SHA requirement.

A pair-with rule: R85 enforces re-open conditions on scope_deferral entries; R86 enforces closure SHAs on real_bug entries. Together they make every ledger row mechanically auditable in both directions — has-this-been-closed and when-will-this-re-open.

Reference: [Linux Kernel — Submitting Patches](https://www.kernel.org/doc/html/latest/process/submitting-patches.html)

Reference: [GitHub Docs — Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue)


<!-- @source rules/error-controller-advice.md -->

---
title: Translate exceptions through a centralised @RestControllerAdvice
impact: HIGH
impactDescription: "One audited exception → HTTP mapping; per-controller try/catch sprawl is anti-pattern"
tags:
  - error
  - advice
  - spring-mvc
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ERR-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "@ControllerAdvice / @RestControllerAdvice"
    quote: "@ControllerAdvice"
  - source_type: external
    citation: "Spring Framework Reference §Controller Advice"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
---

## Translate exceptions through a centralised @RestControllerAdvice

**Impact: HIGH — One audited exception → HTTP mapping; per-controller try/catch sprawl is anti-pattern**

When each controller carries its own `try { ... } catch (DomainException e) { return ResponseEntity.status(...).body(...); }`, the same exception ends up mapped differently in different endpoints — sometimes 400, sometimes 422, sometimes 500. The auditable mapping lives in a single `@RestControllerAdvice` class scoped (by `basePackages` or `assignableTypes`) to the relevant slice of the application. Adding a new exception means one new `@ExceptionHandler` method, not a sweep across every controller.

**Incorrect — controller swallows the exception and shapes its own response:**

```java
@GetMapping("/users/{id}")
public ResponseEntity<?> get(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(service.findById(id));
    } catch (NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
```

**Correct — controller is thin; advice owns the mapping:**

```java
@RestControllerAdvice(basePackages = "com.example.users")
public class UsersExceptionAdvice {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
```

Verification: `./gradlew testPractices --tests "*ErrorControllerAdvice*"` hits two demo endpoints and asserts the advice maps `IllegalArgumentException → 400` and `NoSuchElementException → 404`.

Reference: [Spring Framework Reference — Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)


<!-- @source rules/error-message-not-in-native-title-attribute.md -->

---
title: Mutation error messages MUST NOT render in the native `title` tooltip
impact: MEDIUM
impactDescription: "Native title tooltips appear in screenshots, screencasts, and over-the-shoulder views — server prose surfaced there can leak incidental PII or internal product names"
tags:
  - error-handling
  - a11y
  - pii-side-channel
  - aria-live
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx"
  pattern: "title={ariaLabel} only; error.message rendered in a separate role='alert' aria-live span next to the button"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V8.3 — Sensitive Private Data"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that sensitive information is not transmitted via URL parameters or hidden form fields and is sanitized or removed when no longer required."
    quoted_at: "2026-05-25"
---

## Mutation error messages MUST NOT render in the native `title` tooltip

**Impact: MEDIUM — native tooltips are a quiet side-channel for incidental information leaks**

The native `title` attribute on a button or link renders on hover, persists in the DOM, appears in screenshots and screencasts, and shows over-the-shoulder during screenshare. It is also non-dismissable — once it appears the user has no way to clear it short of moving the mouse. When the value is a server-emitted error message, that message reaches every surface that re-captures the page.

Server error messages often carry information the catalog's PII deny-list cannot fully scrub: internal product names (`Subscription tier "enterprise"`), billing URLs, role / tenant / subscription identifiers, partial stack-trace excerpts, queue identifiers, vendor product codes. The catalog's `parse-error.ts` deny-list catches the most-dangerous PII shapes (email, IP, JWT, PEM headers, internal hostnames, Korean RRN + mobile) but cannot enumerate every operator's product vocabulary. The right answer is to keep error prose out of the `title` slot entirely.

The replacement surface is an inline `role="alert"` span (an ARIA live region). Sighted users see the error next to the action that produced it; screen-reader users hear it announced via aria-live without taking focus; the native tooltip retains a stable, public-safe value (the button's `aria-label`).

**Incorrect — mutation error falls back into `title`, leaks via every screenshot and screenshare:**

```tsx
<button
  type="button"
  aria-label={ariaLabel}
  title={
    toggle.error
      ? toggle.error.message              // ❌ server prose lands in the native tooltip
      : ariaLabel
  }
  onClick={() => toggle.mutate()}
>
  ★
</button>
```

**Correct — title carries the aria-label only; errors render in a separate role='alert' span:**

```tsx
<>
  <button
    type="button"
    aria-label={ariaLabel}
    title={ariaLabel}                     // ✅ Public-safe stable value
    aria-busy={toggle.isPending || undefined}
    aria-disabled={busy || undefined}
    onClick={() => {
      if (busy) return
      toggle.mutate(...)
    }}
  >
    ★
  </button>
  {(toggle.error || error) && (
    <span role="alert" className="ml-1 text-xs text-red-700">
      {(toggle.error ?? (error as Error)).message}
    </span>
  )}
</>
```

Two follow-on patterns travel with this rule:
- Use `aria-busy` + `aria-disabled` instead of native `disabled` while a mutation is in flight (separate rule). Native `disabled` removes the element from the tab order mid-flight; `aria-busy` preserves focus and lets the screen reader announce the busy state.
- Allow the user to dismiss a sticky error banner. TanStack Query mutation errors do NOT auto-clear when the next `mutate()` succeeds — they require an explicit `mutation.reset()`. Pair the alert with a Dismiss button that calls `.reset()`.

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)

Reference: [OWASP ASVS V8 — Sensitive Private Data](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/error-no-stacktrace-leak.md -->

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


<!-- @source rules/error-rfc7807-problem-detail.md -->

---
title: Error bodies must follow RFC 7807 application/problem+json
impact: HIGH
impactDescription: "IETF-standardised error envelope — clients can parse problem.type / title / status / detail uniformly"
tags:
  - error
  - rfc-7807
  - api-contract
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ERR-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-ERR-002
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc7807"
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
evidence:
  - upstream_id: rfc-7807
    section: "RFC 7807 §3.1 — Members of a Problem Details Object"
    quote: "application/problem+json"
  - upstream_id: spring-mvc-exception-handler
    section: "Spring's ProblemDetail support in @ExceptionHandler"
    quote: "ProblemDetail"
  - source_type: external
    citation: "RFC 7807 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc7807"
---

## Error bodies must follow RFC 7807 application/problem+json

**Impact: HIGH — IETF-standardised error envelope — clients can parse problem.type / title / status / detail uniformly**

Every team invents a different shape for error JSON until someone codifies one. RFC 7807 (Problem Details for HTTP APIs) is the existing standard: a media type `application/problem+json` and a base schema with `type` (URI identifying the error class), `title` (short human label), `status` (HTTP status, matches header), `detail` (human-readable description), and optional `instance`. Spring's `ProblemDetail` returns this shape out of the box. Adopting it means clients have one parser for all HTTP errors, not one per service.

**Incorrect — ad-hoc error envelope:**

```java
return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "bad input", "ts", Instant.now().toString()));
```

**Correct — RFC 7807 ProblemDetail:**

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> badArg(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("https://errors.example.com/bad-argument"));
    pd.setTitle("Bad Argument");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
}
```

Verification: `./gradlew testPractices --tests "*Rfc7807ProblemDetail*"` asserts the response carries `Content-Type: application/problem+json` and a body with `type / title / status / detail`.

Reference: [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) · [Spring `@ExceptionHandler` + `ProblemDetail`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)


<!-- @source rules/hooks-before-conditional-return.md -->

---
title: React hooks MUST be called before any conditional early return — Rules of Hooks
impact: HIGH
impactDescription: "Hooks placed after early returns mount into different slots between renders and crash the component with 'Rendered fewer hooks than during the previous render'"
tags:
  - react
  - hooks
  - rules-of-hooks
  - render-correctness
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RERENDER-001"
verification:
  source: "templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx"
  pattern: "all useQuery / useMutation / useState / useMemo calls above the `if (isLoading) return …` / `if (error) return …` / `if (!data) return …` block"
upstream:
  - "https://react.dev/reference/rules/rules-of-hooks"
  - "https://react.dev/learn/state-as-a-snapshot"
evidence:
  - source_type: external
    citation: "React — Rules of Hooks (Only call Hooks at the top level)"
    url: "https://react.dev/reference/rules/rules-of-hooks"
    quote: "Don't call Hooks inside loops, conditions, or nested functions. Instead, always use Hooks at the top level of your React function, before any early returns."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "React — State as a Snapshot"
    url: "https://react.dev/learn/state-as-a-snapshot"
    quote: "A state variable's value never changes within a render, even if its event handler's code is asynchronous."
    quoted_at: "2026-05-25"
---

## React hooks MUST be called before any conditional early return — Rules of Hooks

**Impact: HIGH — every render must call hooks in the same order**

React tracks hook state by call order. A hook placed after a conditional early return is sometimes called and sometimes not, depending on the early-return condition. On the first render where the early-return fires, the hook is skipped; on the next render where data arrives and execution continues past the early returns, the hook is called for the first time. React's internal slot counter sees a different shape than the prior render and throws `'Rendered fewer hooks than during the previous render'`. In production builds the failure mode is silent state corruption between slots (the "second hook" gets state belonging to the "first hook").

This is the most common AI-generated React bug. The pattern looks correct — guard against null data, then use it. But the guard must come AFTER all hooks, not before.

Apply this rule to: `useState`, `useEffect`, `useMemo`, `useCallback`, `useRef`, `useQuery`, `useMutation`, custom hooks (`useFoo`). Anything starting with `use*` follows the same rule.

**Incorrect — hook placed after the data guard:**

```tsx
export default function DetailPage() {
  const { data, isLoading, error } = useQuery(...)
  const [comment, setComment] = useState('')

  if (isLoading) return <Spinner />
  if (error) return <ErrorPanel />
  if (!data) return <NotFound />

  // ❌ This memo is sometimes called, sometimes not.
  // First render: data === undefined → early return at line above → memo NEVER runs.
  // Second render after data arrives → memo runs for the first time.
  // React: 'Rendered more hooks than during the previous render'.
  const summary = React.useMemo(() => buildSummary(data), [data])

  return <Page summary={summary} />
}
```

**Correct — every hook lives above the early returns; guard nullable inputs INSIDE the memo body:**

```tsx
export default function DetailPage() {
  const { data, isLoading, error } = useQuery(...)
  const [comment, setComment] = useState('')

  // ✅ Hook called unconditionally on every render.
  // The nullable input is handled inside the memo, not by a structural guard around it.
  const summary = React.useMemo(
    () => (data ? buildSummary(data) : null),
    [data],
  )

  if (isLoading) return <Spinner />
  if (error) return <ErrorPanel />
  if (!data) return <NotFound />

  return <Page summary={summary!} />
}
```

The `data!` non-null assertion at the use-site is safe because the `!data` early return already established `data` is non-null at that point. TypeScript's narrowing tracks that.

**A note on `chainPreview` / `chain` patterns**: when a derived value depends on the not-yet-loaded data, do **not** double-derive (`chainPreview = data ? compute() : null; … chain = chainPreview ?? compute()`) — the second derivation is provably unreachable after the `!data` guard, and the duplication invites drift. Compute once inside a memo whose deps include the data, then assert non-null at the use-site.

Reference: [React — Rules of Hooks](https://react.dev/reference/rules/rules-of-hooks)

Reference: [React — State as a Snapshot](https://react.dev/learn/state-as-a-snapshot)


<!-- @source rules/http-delete-idempotency-rfc9110.md -->

---
title: DELETE endpoints MUST be idempotent — second call on absent target returns 204, not 404
impact: MEDIUM
impactDescription: "Non-idempotent DELETE causes client retry loops on network failures + breaks RFC 9110 contract"
tags:
  - api
  - http
  - idempotency
  - retry-safety
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-002"
verification:
  gradle_task: testFavorites
  tag: FAV-CRUD-002
upstream:
  - "https://www.rfc-editor.org/rfc/rfc9110.html#name-delete"
evidence:
  - source_type: external
    citation: "RFC 9110 §9.3.5 — HTTP DELETE method idempotency"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-delete"
    quote: "The DELETE method requests that the origin server remove the association between the target resource and its current functionality. ... The methods defined as idempotent are PUT, DELETE, and the safe request methods."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "RFC 9110 §9.2.2 — Idempotent Methods"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods"
    quote: "A request method is considered idempotent if the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request."
    quoted_at: "2026-05-22"
---

## DELETE endpoints MUST be idempotent — second call on absent target returns 204, not 404

**Impact: MEDIUM — Non-idempotent DELETE causes client retry loops and breaks the HTTP contract**

RFC 9110 §9.2.2 specifies DELETE as one of three idempotent methods. The contract: "the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request." A DELETE that returns 404 on a second call has *observably different* server effects between calls — the client sees success then failure — which is the literal definition of non-idempotency.

Practically: every production network retries on connection reset, 502, gateway timeout. If the first DELETE succeeded but the response was lost, the client retries. If the server returns 404 on the retry, the client thinks the operation failed and either errors loudly or surfaces a confusing "already gone" state. The catalog pattern (tag-categorization R32, favorites R34, session-management R33 revoke, comment-thread R36 soft-delete) returns 204 unconditionally — the resource is gone, whether the first call did the work or not.

**Incorrect — DELETE returns 404 on second call:**

```java
@DeleteMapping("/api/favorites/{id}")
public ResponseEntity<Void> remove(@PathVariable UUID id) {
    Favorite f = repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));   // ← 404 on retry
    repo.delete(f);
    return ResponseEntity.noContent().build();
}
```

A client whose first response was lost will retry, get 404, and think the operation failed.

**Correct — DELETE returns 204 whether the row existed or not:**

```java
@DeleteMapping("/api/favorites/{entityType}/{entityId}")
public ResponseEntity<Void> remove(Authentication auth,
                                    @PathVariable String entityType,
                                    @PathVariable String entityId) {
    service.remove(auth.getName(), entityType, entityId);  // matches 0 or 1 rows; both → 204
    return ResponseEntity.noContent().build();
}
```

The service issues a `DELETE WHERE …` and discards the row-count. RFC 9110's idempotency contract is satisfied: client retries do not change the observable result.

**Exception — when 404 is semantically required**: hard-delete on a resource the caller is expected to own (e.g. revoke API key — the caller is acting on a specific id they presumably know exists). In these cases the second call STILL returns 204 if you treat the deletion as idempotent. If the resource was never the caller's, return 404 once (IDOR-safe). The principle: *idempotency is about the server effect*, not about whether the caller is allowed to know the row's history.

**Soft-delete corollary**: when DELETE is implemented as status-flip (e.g. comment-thread soft-delete), the second call observes status already DELETED, leaves `deletedAt` unchanged, and returns 204. The state is identical to the post-first-call state — the definition of idempotent.

Reference: [RFC 9110 §9.3.5 — HTTP DELETE](https://www.rfc-editor.org/rfc/rfc9110.html#name-delete)

Reference: [RFC 9110 §9.2.2 — Idempotent Methods](https://www.rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods)


<!-- @source rules/http-explicit-timeouts.md -->

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


<!-- @source rules/http-restclient-over-resttemplate.md -->

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


<!-- @source rules/http-shared-client-singleton.md -->

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


<!-- @source rules/idempotency-key-on-mutations.md -->

---
title: "Payment, notification, and email-outbox POST mutations must enforce Idempotency-Key via @RequireIdempotencyKey"
rule_id: idempotency-key-on-mutations
impact: CRITICAL
impactDescription: "A network retry on a POST mutation without idempotency protection creates duplicate charges, duplicate notifications, or duplicate emails"
tags:
  - idempotency
  - payment
  - notification
  - email-outbox
  - retry-safety
provenance_class: internal_design
protects_template_id: templates/backend/payment/PaymentController.java
failing_fixture_path: practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  type: review
  notes: "All @PostMapping handlers in payment, notification, and email-outbox controllers must carry @RequireIdempotencyKey. The annotation triggers the IdempotencyFilter which caches responses by key."
evidence:
  - source_type: external
    citation: "IETF draft-ietf-httpapi-idempotency-key-header — The Idempotency-Key HTTP Header Field (deduplicated retry semantics)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Stripe API Reference — Idempotent requests: all POST requests accept an Idempotency-Key header to guarantee exactly-once delivery"
    url: "https://docs.stripe.com/api/idempotent_requests"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "AWS API Gateway — Idempotency tokens for preventing duplicate requests in stateful operations"
    url: "https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-routes.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Payment, notification, and email-outbox POST mutations must enforce Idempotency-Key via `@RequireIdempotencyKey`

**Impact: CRITICAL — Network retries without idempotency guards cause duplicate charges, duplicate SMS/push notifications, and duplicate email sends that are indistinguishable from the first request.**

The `api-idempotency-key-required.md` rule defines the general pattern (Idempotency-Key header + 400 on missing). This rule strengthens the enforcement for the three **high-risk mutation domains** — payment, notification, and email-outbox — by requiring the `@RequireIdempotencyKey` annotation, which wires the handler to the `IdempotencyFilter` cache at the framework level rather than relying on ad-hoc header checks in each handler.

The annotation semantics:
1. Filter reads `Idempotency-Key` header from the request.
2. On first call: processes normally, stores `(key → serialised ResponseEntity)` in the idempotency store (Redis/DB).
3. On retry with the same key: returns the cached response immediately, skipping handler execution.
4. Missing key: 400 `application/problem+json` with `type=urn:ax:idempotency:key-missing`.

**Incorrect — POST mutation handlers without @RequireIdempotencyKey:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // VIOLATION: a retried POST will create a second charge
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.charge(request));
    }

    // VIOLATION: notification send also missing
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        notificationService.send(req);
        return ResponseEntity.accepted().build();
    }
}
```

**Correct — @RequireIdempotencyKey on all side-effecting POST handlers:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // CORRECT: IdempotencyFilter intercepts and deduplicates retries
    @RequireIdempotencyKey
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.charge(request));
    }

    @RequireIdempotencyKey
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        notificationService.send(req);
        return ResponseEntity.accepted().build();
    }
}
```

## Why this matters

The payment, notification, and email-outbox templates are the three domains where retried mutations have user-visible, financially and operationally significant consequences:
- **Payment**: double-charge is a regulatory incident and a customer refund obligation.
- **Notification**: duplicate push/SMS sends degrade user trust.
- **Email-outbox**: duplicate transactional emails (password reset, OTP) may violate ESP rate limits and confuse users.

Unlike read endpoints or idempotent writes (PUT/PATCH), POST mutations in these domains have no natural key to deduplicate on — the `Idempotency-Key` header is the caller-supplied deduplication token.

The `@RequireIdempotencyKey` annotation is defined in `templates/backend/idempotency/` and is wired to `IdempotencyFilter` via AOP. Its use is checked at code-review time for all three domains.

## Failing fixture

See: `practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/PaymentController.java` — two `@PostMapping` handlers in the payment controller without `@RequireIdempotencyKey`. A network retry creates a second charge and a second notification.

Reference: [IETF draft — The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)

Reference: [Stripe API Reference — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)


<!-- @source rules/incident-dashboard-background-poll-plus-refresh.md -->

---
title: Incident dashboards MUST poll in background AND expose a manual Refresh control with "last updated" timestamp
impact: MEDIUM
impactDescription: "TanStack Query default pauses polling when a tab is backgrounded — SRE second-monitor incident views silently stale, causing mis-assessed urgency during pager response"
tags:
  - incident-response
  - sre
  - tanstack-query
  - background-poll
  - data-freshness
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/page.tsx"
  pattern: "useQuery with refetchInterval + refetchIntervalInBackground:true + visible dataUpdatedAt timestamp + manual Refresh button"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - "https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — useQuery options (refetchIntervalInBackground)"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
    quote: "refetchIntervalInBackground: boolean — If set to true, queries that are set to continuously refetch with a refetchInterval will continue to refetch while their tab is in the background."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "MDN Web Docs — Page Visibility API"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API"
    quote: "When the user navigates to a different tab or minimizes the browser containing the tab with the page, the API sends a visibilitychange event to listeners. ... browsers tend to throttle setTimeout and setInterval calls when the page is hidden."
    quoted_at: "2026-05-25"
---

## Incident dashboards MUST poll in background AND expose a manual Refresh control with "last updated" timestamp

**Impact: MEDIUM — a stale dashboard during pager response leads to wrong urgency assessment**

Webhook deliveries, scheduled-task history, activity-feed inbox, approval-workflow inbox, file-storage virus-scan queue, billing-event ledger — all incident-bearing surfaces share the same usage pattern: an SRE / on-call leaves the dashboard open on a secondary monitor during pager rotation. When the tab goes to background (browser switches to another window, screen locks, OS suspends inactive tabs), TanStack Query's default behavior pauses `refetchInterval` polling. When the SRE switches back, the data shows the state from when the tab was last focused — not the current state.

Mis-assessed urgency follows. An SRE sees DEAD_LETTER count at 3 (stale), responds at low urgency, while the live count is at 47. The SRE files a low-priority ticket; the actual incident is severe.

The fix has two parts:
1. **Poll continues in background** — `refetchIntervalInBackground: true` overrides the default-pause behavior. SRE on second monitor or pager-rotating across multiple incident dashboards sees fresh data without needing to refocus each tab.
2. **Visible "last updated" timestamp + manual Refresh button** — even with (1), network blips, server-side rate-limiting, or query-error retries can leave the data older than the polling cadence. A "Updated 14:32:18" indicator next to a Refresh button lets the SRE confirm staleness explicitly and force a fresh fetch when the auto-poll lags.

**Incorrect — default polling pauses in background; no staleness indicator:**

```tsx
const { data, error, isLoading } = useQuery({
  queryKey: ['webhook-deliveries'],
  queryFn: fetchDeliveries,
  refetchInterval: 10_000,
})
```

The SRE puts this on a second monitor at 14:00. At 14:15 they switch back. The data they see is from 14:01 (the moment of last focus before the browser backgrounded the tab). The DEAD_LETTER count looks normal — but the live count is much worse.

**Correct — background polling continues + Refresh + staleness timestamp:**

```tsx
const { data, error, isLoading, dataUpdatedAt, refetch } = useQuery({
  queryKey: ['webhook-deliveries'],
  queryFn: fetchDeliveries,
  refetchInterval: 10_000,
  refetchIntervalInBackground: true,
})

// In the header, alongside filters:
<span className="text-xs text-muted-foreground" aria-live="polite">
  {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
</span>
<button
  type="button"
  className="rounded border px-2 py-1 text-xs hover:bg-muted"
  onClick={() => refetch()}
>
  Refresh
</button>
```

**Apply this rule to**: any frontend surface that satisfies all three:
- Status data transitions during expected lifecycle (PENDING → IN_FLIGHT → SUCCEEDED / FAILED, ENABLED → DISABLED, queued → dispatched → ack'd)
- Used during incident response (failure triage, manual intervention, postmortem)
- Likely viewed on a second monitor or in a browser tab the operator does not actively focus on every minute

**When NOT to apply**: user-driven CRUD surfaces (a comment thread, a tag library, a favorite list) where the operator's own action is what triggers the next render and staleness does not change incident outcome.

A pair-with rule: when the dashboard surfaces server-supplied error strings (`lastError`, `errorMessage`), apply `stored-server-error-sanitize-at-render-layer` so a screen-shared incident bridge does not leak PII / internal hostnames via the same surface this rule keeps fresh.

Reference: [TanStack Query v5 — useQuery API](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery)

Reference: [MDN — Page Visibility API](https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API)


<!-- @source rules/korean-brn-format.md -->

---
title: "Backend endpoints accepting a Korean Business Registration Number (사업자등록번호) must validate the input against the 10-digit NNN-NN-NNNNN format before persistence or logging"
rule_id: korean-brn-format
impact: HIGH
impactDescription: "Korean B2B integration endpoints (tax invoices, e-Tax, supplier onboarding, payment ledger) silently accept malformed BRN strings (truncated, free-form, including 주민등록번호 by mistake) when no format check runs at the controller boundary. The downstream effects — failed NTS reconciliation, mis-routed VAT, RRN leakage through a field reused as a BRN slot — surface only at audit time. A 10-digit NNN-NN-NNNNN regex enforced at the DTO layer rejects all four classes at the boundary."
tags:
  - validation
  - identity
  - brn
  - korean-compliance
  - locked_constraint
provenance_class: locked_constraint
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  type: review
  status: manual
  notes: "Static analysis: every backend DTO field semantically representing a 사업자등록번호 (commonly named brn, businessRegistrationNumber, businessNumber, 사업자등록번호, businessRegNo) must be wired to a Jakarta `ConstraintValidator` that applies the regex ^[0-9]{3}-[0-9]{2}-[0-9]{5}$ before any service-layer call. Inputs failing the regex must be rejected with HTTP 400 + RFC 7807 problem detail; never persisted in unvalidated form; never logged in raw form. The checksum algorithm (mod-10 weighted-sum) is intentionally OUT-OF-SCOPE for this rule (deferred R13+ as a separate rule contingent on an authoritative source landing) — see practices/DECISIONS.md TD-2026-05-24-030/031 cycle scope."
evidence:
  - source_type: external
    citation: "한국은행 — 통화정책의 효율적 수행을 통해 물가 안정과 금융안정을 도모 (Korean enterprise financial authority surface; adjacent-Korean anchor per the R8/R9/R10 adjacent-fallback precedent — direct BRN-format Korean docs at 위키백과 사업자등록번호, namu.wiki, hometax.go.kr, law.go.kr were unreachable on 2026-05-24 per practices/upstream/r12-sp49-evidence-snapshot.md downgrade cluster)"
    url: "https://www.bok.or.kr/portal/main/main.do"
    quoted_at: "2026-05-24"
decided_at: "2026-05-24"
---

## Backend endpoints accepting a 사업자등록번호 must validate the input against the 10-digit NNN-NN-NNNNN format

**Impact: HIGH — Korean B2B endpoints (세금계산서, e-Tax, supplier onboarding, payment ledger) silently accept malformed 사업자등록번호 input when no controller-boundary check runs. Downstream effects (failed NTS reconciliation, mis-routed VAT, accidental RRN leakage through a reused field) surface only at audit time.**

The 사업자등록번호 (Business Registration Number, BRN) is a 10-digit identifier issued by the 국세청 (National Tax Service) to every business entity registered in Korea. Its canonical display form is `NNN-NN-NNNNN` — 3-digit 세무서 code + 2-digit individual/corporate code + 5-digit sequence — and the same 10-digit shape is what `세금계산서 작성요령` requires on every issued tax invoice. The rule constrained here is **format-only**: any backend endpoint accepting a BRN field must run the regex `^[0-9]{3}-[0-9]{2}-[0-9]{5}$` (or the equivalent compact `[0-9]{10}` form normalised before validation) at the DTO layer before the service tier runs.

The **mod-10 weighted-sum checksum** that NTS publishes alongside the format is intentionally **out of scope** for this rule. R12 evidence collection on 2026-05-24 could not surface a verbatim Korean authoritative source for the checksum algorithm (위키백과 사업자등록번호 alt URL is 200 OK but its content does not cover the 10-digit format or the checksum; namu.wiki is bot-blocked; en.wikipedia "Business_registration_number" returns 404; law.go.kr / hometax.go.kr / NTS-7660 host-wide downgraded — see practices/upstream/r12-sp49-evidence-snapshot.md). A separate `korean-brn-checksum` rule is queued in `practices/DECISIONS.md#deferred-rules-r13` and will ship once an authoritative source lands.

**Incorrect — DTO accepts arbitrary string in a BRN slot; service layer assumes well-formed input:**

```java
public record CreateSupplierRequest(
        @NotBlank String name,
        @NotBlank String brn,            // accepts "1234567890", "abc", a raw 13-digit RRN, "123-45-6789012", anything
        @NotBlank String contactEmail
) {}

@PostMapping("/api/suppliers")
public ResponseEntity<Void> create(@RequestBody @Valid CreateSupplierRequest req) {
    supplierService.register(req.name(), req.brn(), req.contactEmail());  // persisted unvalidated
    return ResponseEntity.created(URI.create("/api/suppliers/" + req.brn())).build();
}
```

**Correct — Jakarta ConstraintValidator runs at the DTO boundary; only the 10-digit NNN-NN-NNNNN shape proceeds to the service tier:**

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BusinessRegistrationNumberValidator.class)
public @interface BusinessRegistrationNumber {
    String message() default "BRN must match NNN-NN-NNNNN (3-2-5 digits)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public final class BusinessRegistrationNumberValidator
        implements ConstraintValidator<BusinessRegistrationNumber, String> {

    // Canonical Korean format: 3-digit 세무서 code · 2-digit individual/corporate code · 5-digit sequence.
    private static final java.util.regex.Pattern BRN =
            java.util.regex.Pattern.compile("^[0-9]{3}-[0-9]{2}-[0-9]{5}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) {
            return false;   // @NotNull is enforced separately; here, null = invalid BRN shape.
        }
        return BRN.matcher(value).matches();
    }
}

public record CreateSupplierRequest(
        @NotBlank String name,
        @NotBlank @BusinessRegistrationNumber String brn,
        @NotBlank @Email String contactEmail
) {}
```

The matching 400 response is shaped by the project's existing `GlobalExceptionHandler` (RFC 7807 ProblemDetail). No raw BRN appears in the error message — only a stable problem `type` URI (`urn:ax:supplier:invalid-brn`) and a sanitized property pointer, so the application logs do not leak the rejected value.

### Why "format-only" and not checksum

The mod-10 weighted-sum checksum NTS publishes is a stronger check (it rejects typos that pass the format gate), but R12 evidence collection on 2026-05-24 found no Korean authoritative source verbatim-reachable to anchor the algorithm. Shipping a checksum-coupled rule against vendor-blog reconstructions of the algorithm would fail the catalog's `evidence:` discipline (every normative claim must be sourced from a verbatim upstream — see `practices/AGENTS.md` evidence-anchored rule provenance contract).

R12 PRD §4.3 + practices/DECISIONS.md TD-2026-05-24-030/031 cycle explicitly defers `korean-brn-checksum` to a later cycle. The format-only rule still closes the four most common failure modes — truncated input, free-form text, an RRN pasted into a BRN slot, the wrong separator pattern.

### What this rule does NOT do

- It does not validate that the 3-digit prefix is a real 세무서 code (NTS publishes the list; the list rotates as offices reorganise — too volatile for a static rule).
- It does not validate the checksum (see above).
- It does not cover RRN (주민등록번호); that lives in `no-rrn-collection-without-legal-basis.md` and is a stricter legal-basis rule, not a format rule.
- It does not impose a storage encoding; teams choose between persisting the dashed form (`123-45-67890`) or the bare-digit form (`1234567890`) per their schema convention. Both shapes are valid as long as the controller-boundary regex accepts only the dashed canonical form on the wire.

Reference: https://www.bok.or.kr/portal/main/main.do


<!-- @source rules/korean-vat-10-percent-calculation.md -->

---
title: "Backend services computing Korean VAT must use BigDecimal with rate 0.10 and HALF_UP rounding; float, double, and inline rate literals (0.10d / 0.10f) are prohibited"
rule_id: korean-vat-10-percent-calculation
impact: HIGH
impactDescription: "Korean VAT (부가가치세) is fixed at 10% by statute. Computing VAT with float / double silently introduces sub-부 rounding errors that compound across invoices; declaring the rate as 0.10d (double literal) inside a BigDecimal constructor (`new BigDecimal(0.10d)`) materializes the float-noise value 0.1000000000000000055511151231257827021181583404541015625 into the audit trail. The HALF_UP requirement matches the Korean invoice rounding convention — different rounding modes systematically over- or under-collect across invoice volume."
tags:
  - billing
  - tax
  - vat
  - korean-compliance
  - currency
provenance_class: external
spec_ref: "specs/billing-l0.yaml#BILLING-CUR-001"
verification:
  type: review
  status: manual
  notes: "Static analysis (1): grep -rE '\\b0\\.10[dfDF]?\\b' against any billing / payment / invoice / tax module must return zero matches OUTSIDE a BigDecimal(\"0.10\") string-constructor call. Static analysis (2): every method computing a VAT amount must use BigDecimal.setScale(0, RoundingMode.HALF_UP) (or the equivalent 2-arg .multiply + .setScale chain) — never .doubleValue() / .floatValue() intermediates. The 3 representative fixtures asserted by review: (i) supply 1000 → vat 100; (ii) supply 1001 → vat 100; (iii) supply 1005 → vat 101 (HALF_UP on the .5 boundary). Cross-links: lang-bigdecimal-for-money.md + currency-amount-precision-explicit.md (long minor-units transport) — the VAT rate is the lone decimal-rate exception that BigDecimal exists to handle."
evidence:
  - source_type: external
    citation: "Wikipedia (Korean) 부가가치세 — verbatim '대한민국 10% VAT = 부가세(附加稅) 또는 부가가치세(附加價値稅)'"
    url: "https://ko.wikipedia.org/wiki/부가가치세"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "Wikipedia (Korean) 부가가치세 — verbatim '대한민국에서는 1977년 7월 1일부터 시행하였다.'"
    url: "https://ko.wikipedia.org/wiki/부가가치세"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "국세청 (NTS) 부가가치세 기장의무 — verbatim '직전연도(2024년) 업종별 수입금액 기준으로 판단'"
    url: "https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669"
    quoted_at: "2026-05-24"
  - source_type: external
    citation: "PwC Tax Summaries (Korea) — verbatim 'VAT is generally levied at a rate of 10% on the supply of goods and services in Korea.'"
    url: "https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes"
    quoted_at: "2026-05-24"
decided_at: "2026-05-24"
---

## Korean VAT must be computed with BigDecimal("0.10") and HALF_UP rounding

**Impact: HIGH — Korean 부가가치세 is fixed at 10% by statute. Float / double arithmetic silently accumulates rounding noise; inline `0.10d` literals materialize float-noise values into the audit trail; non-HALF_UP rounding systematically biases collection across invoice volume.**

The Korean 부가가치세 (Value-Added Tax) rate is 10% on the supply of goods and services and has been in force since 1977-07-01. The PwC Tax Summaries restate the rule in English: *"VAT is generally levied at a rate of 10% on the supply of goods and services in Korea."* The 국세청 (NTS) `부가가치세 기장의무` page restates the bookkeeping threshold tied to the prior-year revenue ('직전연도(2024년) 업종별 수입금액 기준으로 판단'). The rate itself is single-valued and statutory — there is no business case for computing it with floating-point arithmetic, and every Korean-domain backend that bills, invoices, or settles must use the BigDecimal-with-HALF_UP path.

This rule sits beside `currency-amount-precision-explicit.md` (which mandates long integer minor units for transport storage) and `lang-bigdecimal-for-money.md` (which forbids float / double for money). The VAT rate is the **only** decimal-rate exception in the Korean billing pipeline — long arithmetic still applies to the resulting amounts.

**Incorrect — double rate; float intermediate; banker's-rounding default; inline 0.10d:**

```java
public long computeVatAmount(long supplyAmount) {
    // VIOLATION (1): double accumulates IEEE 754 noise.
    double rate = 0.10d;
    double vat = supplyAmount * rate;          // 1005 * 0.10 = 100.50000000000001
    return Math.round(vat);                    // banker's-rounding NOT HALF_UP — drifts on .5
}

// VIOLATION (2): inline double literal inside BigDecimal materializes float noise.
public BigDecimal vat(BigDecimal supply) {
    return supply.multiply(new BigDecimal(0.10d));   // rate becomes 0.1000000000000000055511...
}
```

**Correct — BigDecimal("0.10") string constructor; HALF_UP rounding to scale 0; cross-linked to long minor-units transport:**

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class KoreanVat {

    // Statutory rate per 위키백과 부가가치세 + PwC Tax Summaries (Korea).
    // String constructor is mandatory: new BigDecimal(0.10d) materializes IEEE 754 noise.
    private static final BigDecimal RATE = new BigDecimal("0.10");

    private KoreanVat() {}

    /**
     * Compute the VAT amount (in 원, integer scale 0) for a supply amount.
     * HALF_UP matches the Korean tax-invoice rounding convention.
     */
    public static long computeVatAmount(long supplyAmount) {
        // supply * 0.10, rounded HALF_UP to the nearest 원.
        return BigDecimal.valueOf(supplyAmount)
                .multiply(RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
```

The three boundary cases anchored by review:

| `supplyAmount` (원) | `RATE` | unrounded product | `HALF_UP` (scale 0) | result |
|---|---|---|---|---|
| `1000`  | `BigDecimal("0.10")` | `100.00`     | `100`           | `100`  |
| `1001`  | `BigDecimal("0.10")` | `100.10`     | `100`           | `100`  |
| `1005`  | `BigDecimal("0.10")` | `100.50`     | `101` (HALF_UP) | `101`  |

These three cases pin the rule unambiguously: the exact-zero-cents case (`1000 → 100`), the round-down case (`1001 → 100`), and the half-boundary case (`1005 → 101`) where HALF_UP differs from HALF_EVEN (banker's rounding) — `HALF_EVEN` would round `1005 → 100`, and the silent drift across invoice volume is what the rule prevents.

### Cross-links

- Transport storage: `currency-amount-precision-explicit.md` — supply / VAT amounts are stored and wired as long integer minor units (원). The BigDecimal path exists only inside the VAT computation method.
- Money type rule: `lang-bigdecimal-for-money.md` — float / double prohibited for all monetary fields. The VAT rate constant is the only decimal value in the chain.
- Statutory rate: 위키백과 부가가치세 establishes the 10% rate and the 1977-07-01 enactment; PwC Tax Summaries cross-anchors in English; NTS 부가가치세 기장의무 provides the surrounding bookkeeping context.

### Why HALF_UP and not HALF_EVEN

Korean tax-invoice convention rounds the 0.5 boundary **up**, not to the nearest even integer (banker's rounding). `setScale(0, RoundingMode.HALF_UP)` matches `세금계산서` rounding; `RoundingMode.HALF_EVEN` (Java's default for `BigDecimal.divide` without a mode argument) does not. The rule is enforced by explicit `HALF_UP` at every VAT site — never by reliance on the BigDecimal default.

Reference: https://ko.wikipedia.org/wiki/부가가치세

Reference: https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669

Reference: https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes


<!-- @source rules/lang-bigdecimal-for-money.md -->

---
title: Monetary amounts must use BigDecimal — never float or double
impact: HIGH
impactDescription: "IEEE-754 binary floating point cannot represent most decimal fractions exactly; arithmetic on monetary doubles silently drifts"
tags:
  - lang
  - money
  - precision
  - bigdecimal
spec_ref: "specs/payment-l0.yaml#PAYMENT-MONEY-001"
verification:
  gradle_task: testPayment
  tag: PAYMENT-MONEY-001
upstream:
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
  - "https://ieeexplore.ieee.org/document/8766229"
evidence:
  - upstream_id: iso-4217
    section: "Amount representation rules — decimal-string vs minor-units"
    quote: "JSON number with a decimal point"
  - source_type: external
    citation: "Effective Java (3rd ed., Joshua Bloch) — Item 60: Avoid float and double if exact answers are required"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html"
  - source_type: external
    citation: "IEEE 754-2019 — Standard for Floating-Point Arithmetic"
    url: "https://ieeexplore.ieee.org/document/8766229"
---

## Monetary amounts must use BigDecimal — never float or double

**Impact: HIGH — IEEE-754 binary floating point cannot represent most decimal fractions exactly; arithmetic on monetary doubles silently drifts**

`double` and `float` are binary floating point — they can represent `0.5`, `0.25`, `0.75` exactly but cannot represent `0.1`, `0.2`, `0.3`, or any tenth that is not a sum of negative powers of two. The classic demonstration `0.1 + 0.2 == 0.30000000000000004` is harmless in a graph but catastrophic in a ledger: a refund computed as `paid - capturedAmount` over a few thousand line items will accumulate sub-cent rounding error that breaks reconciliation, fails audit invariants, and shows up months later as a `recon_drift_detected_total` counter incrementing in production. The Java standard library answer, codified in `java.math.BigDecimal` and recommended verbatim by *Effective Java* Item 60, is unconditional: monetary amounts use `BigDecimal`; never `double`, `float`, or `Number`. The compiler will not catch this — only a rule + a static-analysis scan will.

**Tradeoff — long minor-units integer:** A legitimate alternative is to store an integer in the smallest subdivision of the currency (KRW 1000원 → `1000`, USD $10.99 → `1099`). This is what Stripe, Adyen, and most PSP REST APIs do because integers are exact end-to-end. The tradeoff is binding: **if** the codebase chooses `long` minor-units, **every** monetary field and every arithmetic step must commit to that representation. A mix of `BigDecimal` in some places and `long amountCents` in others reintroduces conversion bugs at every boundary. This rule mandates `BigDecimal` by default; a codebase-wide migration to `long` minor-units is permitted only if (a) documented in `DECISIONS.md`, (b) enforced by a separate ArchUnit rule asserting no `BigDecimal` appears in monetary positions, and (c) the per-currency scale check from `payment-iso-4217-currency.md` is rewritten to assert the integer's implicit scale matches the currency. The mixed form is what this rule rejects.

**Incorrect — monetary fields typed as double, silent precision drift:**

```java
public class Payment {
    private double amount;            // 0.1 + 0.2 → 0.30000000000000004
    private double capturedAmount;
    // partial-refund check uses subtraction → accumulates rounding error
}
```

**Correct — BigDecimal with explicit scale at construction:**

```java
public class Payment {
    private BigDecimal amount;
    private BigDecimal capturedAmount;

    public static Payment of(BigDecimal raw, String currency) {
        int scale = Currency.getInstance(currency).getDefaultFractionDigits();
        BigDecimal scaled = raw.setScale(scale, RoundingMode.UNNECESSARY);
        return new Payment(scaled, scaled, currency);
    }
}
// raw.setScale(scale, UNNECESSARY) throws ArithmeticException if the input
// already has more decimals than the currency allows — surfaces scale
// violations as 400 RFC 7807 rather than silent truncation.
```

A grep / ArchUnit rule completes the loop: scan the monetary package and assert `float` and `double` do not appear on any monetary-named field. Pair this rule with `payment-iso-4217-currency.md` (per-currency scale validation) and with a Jackson deserializer that rejects JSON `number` tokens with a decimal point (only integer minor units and explicit decimal strings are accepted on the wire).

Verification: `./gradlew testPayment --tests "*Money*"` exercises the deserializer (float-token rejection), the scale validator (KRW with 2 decimals → 400, BHD with 2 decimals → 400 because BHD scale is 3), and a partial-refund-sum invariant test that subtracts repeated partial refunds from `capturedAmount` and asserts exact zero (no sub-cent drift). Static scan: `grep -rn 'float\|double' backend/src/main/java/.../payment/` returns 0 hits on monetary fields.

Reference: [java.math.BigDecimal — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

Reference: [IEEE 754-2019 — Standard for Floating-Point Arithmetic](https://ieeexplore.ieee.org/document/8766229)


<!-- @source rules/lang-no-public-mutable-fields.md -->

---
title: No public, non-static, non-final instance fields outside records
impact: MEDIUM
impactDescription: "Public mutable fields bypass encapsulation and break every dependent on next refactor"
tags:
  - lang
  - encapsulation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-003
upstream:
  - "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html"
evidence:
  - source_type: external
    citation: "Java Language Specification §8.3.1 — Field Modifiers"
    url: "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1"
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 16: In public classes, use accessor methods, not public fields"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
---

## No public, non-static, non-final instance fields outside records

**Impact: MEDIUM — Public mutable fields bypass encapsulation and break every dependent on next refactor**

`public String name;` looks convenient. It also means the field cannot be renamed, retyped, validated on set, or replaced with a derived accessor without breaking every caller — and there is no observable point where invariants can be enforced. Effective Java Item 16 codifies the rule: "in public classes, use accessor methods, not public fields". Constants (`public static final`) are unaffected. Record components project public accessor *methods*, not fields, so records are exempt by construction.

**Incorrect — public mutable instance field:**

```java
public class Counter {
    public int value;                      // anything can set; no validation; no future-proofing
}
```

**Correct — record or accessor method on the class:**

```java
public record CounterSnapshot(int value) {}

// or, if you need behavior on a class:
public class Counter {
    private int value;                     // encapsulated
    public int value() { return value; }
    public void increment() { value++; }
}
```

Verification: `./gradlew testPractices --tests "*NoPublicMutableFields*"` runs an ArchUnit field rule that picks every public, non-static, non-final field on a non-record class and fails if any are found.

Reference: [JLS §8.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1) · Effective Java Item 16


<!-- @source rules/lang-records-for-dtos.md -->

---
title: Transport DTOs (*Request / *Response) must be Java records
impact: MEDIUM
impactDescription: "Immutability + equals/hashCode/toString + one-line field contract, zero boilerplate"
tags:
  - lang
  - records
  - dto
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-001
upstream:
  - "https://openjdk.org/jeps/395"
evidence:
  - upstream_id: jep-395-records
    section: "JEP 395 — Records (Final)"
    quote: "record"
  - source_type: external
    citation: "JEP 395 — Records (Final, Java 16)"
    url: "https://openjdk.org/jeps/395"
---

## Transport DTOs (*Request / *Response) must be Java records

**Impact: MEDIUM — Immutability + equals/hashCode/toString + one-line field contract, zero boilerplate**

A transport DTO has one job: carry a fixed set of fields across the wire. A `record` declares that in a single line and produces immutability, `equals` / `hashCode` / `toString`, and clean serialization for free. The classic 80-line `class` with private fields + 8 getters + setters + manual `equals` is pure boilerplate that survives only because IDE templates exist. Java has had records since 16; there is no longer a good reason for a hand-rolled transport class. (Domain entities with behavior or with mutable invariants are NOT the target of this rule — they may legitimately be classes.)

**Incorrect — hand-rolled DTO with mutable fields and boilerplate accessors:**

```java
public class UserCreateRequest {
    private String name;
    private String email;
    public UserCreateRequest() {}
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    // equals, hashCode, toString — usually wrong or missing
}
```

**Correct — record:**

```java
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @NotBlank @Email String email
) {}
```

Verification: `./gradlew testPractices --tests "*RecordsForDtos*"` runs an ArchUnit rule that picks every `*Request` and `*Response` class under `practices/` and asserts each is a record.

Reference: [JEP 395 — Records (Final)](https://openjdk.org/jeps/395)


<!-- @source rules/lang-sealed-result-hierarchies.md -->

---
title: Model closed result hierarchies with sealed interface + record permits
impact: MEDIUM
impactDescription: "Compiler-enforced exhaustive handling of every terminal outcome"
tags:
  - lang
  - sealed
  - pattern-matching
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-002
upstream:
  - "https://openjdk.org/jeps/409"
evidence:
  - upstream_id: jep-409-sealed-classes
    section: "JEP 409 — Sealed Classes (Final)"
    quote: "sealed"
  - source_type: external
    citation: "JEP 409 — Sealed Classes (Final, Java 17)"
    url: "https://openjdk.org/jeps/409"
---

## Model closed result hierarchies with sealed interface + record permits

**Impact: MEDIUM — Compiler-enforced exhaustive handling of every terminal outcome**

Before sealed types, a "result" type was an `enum` (no carried data), a class hierarchy with `instanceof` chains (forgettable), or a single `Result` class with optional fields (silent wrong-state bugs). `sealed interface` + permitted records gives a closed hierarchy: each outcome carries its own typed data, and a pattern-matching `switch` over the sealed type is exhaustive — the compiler refuses to forget a branch. Adding a third outcome is a single compile-time signal: every `switch` on the type becomes a compile error until the new case is handled.

**Incorrect — boolean + nullable error field:**

```java
public class PaymentResult {
    private final boolean success;
    private final String errorCode;          // null when success — silent footgun
    private final String transactionId;      // null when failure — same
    // ...
}
```

**Correct — sealed interface + record subtypes:**

```java
public sealed interface PaymentResult permits PaymentSuccess, PaymentFailure {
    record PaymentSuccess(String txId, long amount) implements PaymentResult {}
    record PaymentFailure(String errorCode, String message) implements PaymentResult {}
}

// Exhaustive switch — compiler errors if a branch is added but not handled.
String describe(PaymentResult r) {
    return switch (r) {
        case PaymentSuccess s -> "ok:" + s.txId();
        case PaymentFailure f -> "fail:" + f.errorCode();
    };
}
```

Verification: `./gradlew testPractices --tests "*SealedResultHierarchy*"` asserts `PaymentResult.class.isSealed()`, that all `getPermittedSubclasses()` are records, and that an exhaustive `switch` over the type compiles.

Reference: [JEP 409 — Sealed Classes (Final)](https://openjdk.org/jeps/409)


<!-- @source rules/messaging-payload-record.md -->

---
title: Message and event payloads must be Java records (immutable by construction)
impact: MEDIUM
impactDescription: "Mutable payloads can be modified after publish — the in-flight copy and the delivered copy disagree"
tags:
  - messaging
  - immutability
  - records
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — event payload semantics"
    quote: "ApplicationEvent"
  - source_type: external
    citation: "JEP 395 — Records (final)"
    url: "https://openjdk.org/jeps/395"
---

## Message and event payloads must be Java records (immutable by construction)

**Impact: MEDIUM — Mutable payloads can be modified after publish — the in-flight copy and the delivered copy disagree**

A POJO with setters that the publisher pushes onto a queue can be mutated by the caller *after* `publish()` returns. Depending on the serializer's timing — and on whether the in-process bus passes by reference versus by copy — the consumer may observe the mutated state, the original state, or worse, a half-mutated state. Records make the question moot: every component is final, there are no setters, and any "change" produces a new record instance. JEP 395 (Java 16) finalized records exactly for these transport-layer value carriers.

**Incorrect — mutable POJO as payload:**

```java
public class OrderPlacedEvent {
    private String orderId;
    private String customerId;
    public void setCustomerId(String v) { this.customerId = v; }   // mutable AFTER publish
    // ...
}
```

**Correct — record payload, every component final by construction:**

```java
public record OrderPlacedEvent(String orderId, String customerId, Instant placedAt) {}

OrderPlacedEvent evt = new OrderPlacedEvent("ord-123", "cust-42", Instant.now());
publisher.publish(MessageTopics.ORDER_PLACED, evt);
// no setters, every component final, equals/hashCode/toString auto-generated
```

(Earlier iterations of this rule used `long amountCents` to illustrate a payload
field. That example is intentionally avoided here because monetary fields are
governed by `lang-bigdecimal-for-money.md`, which mandates `BigDecimal` unless
the codebase commits whole-system to integer minor-units. Using a non-monetary
field (`customerId`) keeps the immutability lesson clear without colliding with
the monetary-precision rule.)

Verification: `./gradlew testPractices --tests "*PayloadRecord*"` asserts `OrderPlacedEvent.class.isRecord()` and that every declared field is final.

Reference: [JEP 395 — Records](https://openjdk.org/jeps/395)


<!-- @source rules/messaging-publisher-interface.md -->

---
title: Service-layer publishers must depend on an abstract MessagePublisher interface
impact: HIGH
impactDescription: "A concrete-typed publisher field couples the domain to one broker SDK — broker swap becomes a refactor"
tags:
  - messaging
  - abstraction
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — ApplicationEventPublisher abstraction"
    quote: "ApplicationEventPublisher"
  - source_type: external
    citation: "Spring Framework Reference — Standard and Custom Events"
    url: "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events"
---

**Current ax-template adapter (2026-05-16):** `SpringEventMessagePublisher` (@Primary) — uses Spring's built-in `ApplicationEventPublisher` for in-process publish-subscribe, zero broker dependency. `InMemoryMessagePublisher` is the test-only impl (not @Component). Kafka/RabbitMQ adapters plug in behind the same interface when broker infrastructure is decided.

## Service-layer publishers must depend on an abstract MessagePublisher interface

**Impact: HIGH — A concrete-typed publisher field couples the domain to one broker SDK — broker swap becomes a refactor**

A service that holds a `KafkaTemplate<String, OrderPlacedEvent>` field has *imported the broker* into the domain layer — the broker's serialization model, retry semantics, and partitioning concept are now domain concepts. Swapping Kafka for RabbitMQ or going broker-less for tests means rewriting every service that publishes. The remedy is the standard hexagonal pattern: the domain owns an abstract `MessagePublisher` interface; concrete impls (`KafkaMessagePublisher`, `RabbitMessagePublisher`, `InMemoryMessagePublisher` for tests) live in an adapter package and are wired via Spring. The current template ships only `InMemoryMessagePublisher` — production-broker impls plug in later behind the same interface.

**Incorrect — service couples to the broker SDK:**

```java
@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderPlacedEvent> kafka;   // broker SDK leaks into domain
    public OrderEventPublisher(KafkaTemplate<String, OrderPlacedEvent> kafka) { this.kafka = kafka; }
    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafka.send("order.placed", event.orderId(), event);
    }
}
```

**Correct — service depends on the domain-owned interface:**

```java
public interface MessagePublisher {
    void publish(String topic, Object payload);
}

@Service
public class OrderEventPublisher {
    private final MessagePublisher publisher;                       // interface, not implementation
    public OrderEventPublisher(MessagePublisher publisher) { this.publisher = publisher; }
    public void publishOrderPlaced(OrderPlacedEvent event) {
        publisher.publish(MessageTopics.ORDER_PLACED, event);
    }
}
```

Verification: `./gradlew testPractices --tests "*PublisherInterface*"` reflects on `OrderEventPublisher.publisher` and asserts the declared type equals `MessagePublisher.class` and `isInterface()`.

Reference: [Spring Framework — Standard and Custom Events (ApplicationEventPublisher abstraction)](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)


<!-- @source rules/messaging-topic-name-constant.md -->

---
title: Topic / routing-key names must be public-static-final constants, not inline string literals
impact: MEDIUM
impactDescription: "Inline topic literals diverge silently between publisher and consumer; the queue goes empty in prod"
tags:
  - messaging
  - constants
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MESSAGING-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MESSAGING-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
evidence:
  - upstream_id: spring-application-events
    section: "Spring Framework — event identity / topic identity"
    quote: "event"
  - source_type: external
    citation: "Effective Java (3rd ed.) — Item 22: Use interfaces only to define types (constants-class pattern)"
    url: "https://www.oreilly.com/library/view/effective-java-third/9780134686097/"
---

## Topic / routing-key names must be public-static-final constants, not inline string literals

**Impact: MEDIUM — Inline topic literals diverge silently between publisher and consumer; the queue goes empty in prod**

When a topic name is an inline `"order.placed"` in the publisher and an inline `"orders.placed"` in the consumer (one extra `s`), the build is green, the tests pass, and the queue silently goes empty in production. The compiler doesn't know two string literals were meant to refer to the same topic. A single `MessageTopics` final class with `public static final String ORDER_PLACED = "practices.order.placed"` forces both ends through the same symbol — a rename is one edit, not a code-search-replace across the codebase. The constants holder is a `final` class with a private constructor so it cannot be subclassed or instantiated (Effective Java Item 22 — interfaces define types, not constants holders).

**Incorrect — inline literals at every call site:**

```java
publisher.publish("order.placed", event);                          // publisher
// ... elsewhere ...
@KafkaListener(topics = "orders.placed")                           // consumer — one extra 's', silent break
public void onOrderPlaced(OrderPlacedEvent event) { ... }
```

**Correct — single constants class, both sides reference it:**

```java
public final class MessageTopics {
    public static final String ORDER_PLACED = "practices.order.placed";
    private MessageTopics() {}                                     // not instantiable
}

publisher.publish(MessageTopics.ORDER_PLACED, event);              // publisher
@KafkaListener(topics = MessageTopics.ORDER_PLACED)                // consumer — same symbol, rename-safe
public void onOrderPlaced(OrderPlacedEvent event) { ... }
```

Verification: `./gradlew testPractices --tests "*TopicNameConstant*"` reflects on `MessageTopics.ORDER_PLACED` and asserts public + static + final + String, plus the holder class itself is final.

Reference: [Spring Framework — Standard and Custom Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events) · Effective Java (3rd ed.) — Item 22: Use interfaces only to define types.


<!-- @source rules/migration-forward-only.md -->

---
title: Migration versions are unique and monotonic — never renumber an applied migration
impact: HIGH
impactDescription: "Renaming an applied migration breaks Flyway's checksum check on every downstream environment"
tags:
  - migration
  - flyway
  - immutability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Flyway checksum / immutability contract"
    quote: "Flyway"
  - source_type: external
    citation: "Flyway documentation — How Flyway works (immutable history + checksums)"
    url: "https://documentation.red-gate.com/fd/how-flyway-works-271583665.html"
---

## Migration versions are unique and monotonic — never renumber an applied migration

**Impact: HIGH — Renaming an applied migration breaks Flyway's checksum check on every downstream environment**

Once `V003__add_orders_user_id.sql` has been applied in any environment, the migration is recorded in `flyway_schema_history` with a checksum of its file contents. Renaming it to `V002__add_orders_user_id.sql` (because someone "forgot" a step in between) or editing its body to fix a typo changes that checksum. The next `flyway migrate` in that environment detects the checksum mismatch and refuses to start the application. The new edit / renumber lands in a fresh environment fine, breaks every existing one.

The mechanical contract: migration files are immutable after they ship. New schema changes get a new `V{N+1}__...` file. Versions must be unique (no two files with the same `V{N}` prefix) and strictly monotonic.

**Incorrect — renumbering an existing migration:**

```
# Before (shipped):
V001__create_users.sql
V002__create_orders.sql

# After (broken — V002 renamed to V003, new file inserted as V002):
V001__create_users.sql
V002__create_orders_priority_column.sql    ← was a new file
V003__create_orders.sql                    ← was V002 — every existing env's checksum breaks
```

**Correct — forward-only, monotonic:**

```
V001__create_users.sql
V002__create_orders.sql
V003__add_orders_priority_column.sql       ← new file at next version
```

Verification: `./gradlew testPractices --tests "*ForwardOnly*"` parses the leading `V{N}` prefix off each filename and asserts no duplicates + sorted list increases strictly.

Reference: [Flyway — How Flyway works](https://documentation.red-gate.com/fd/how-flyway-works-271583665.html)


<!-- @source rules/migration-no-baseline-on-migrate.md -->

---
title: spring.flyway.baseline-on-migrate must not be enabled in base config
impact: HIGH
impactDescription: "Silent baseline on a missing history table makes every prior migration unaudited"
tags:
  - migration
  - flyway
  - production-safety
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-003
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Flyway baseline / repair"
    quote: "Flyway"
  - source_type: external
    citation: "Flyway documentation — Baseline / baseline-on-migrate"
    url: "https://documentation.red-gate.com/fd/baseline-184549760.html"
---

## spring.flyway.baseline-on-migrate must not be enabled in base config

**Impact: HIGH — Silent baseline on a missing history table makes every prior migration unaudited**

`baseline-on-migrate: true` tells Flyway: "if the `flyway_schema_history` table is missing, silently mark the current schema as the baseline (version = `baseline-version`) and proceed". That is exactly what you want exactly once — initial adoption of Flyway on a database that already has tables. It is exactly what you do NOT want in steady-state: a dropped / mis-restored history table re-baselines the environment, every prior migration is "considered applied" without verification, and the next migration runs against undocumented state.

The contract: leave `baseline-on-migrate` unset (Flyway default = false) in the base `application.yml`. If a one-off baseline is needed, do it on a profile-scoped config (`application-baseline.yml`) that is only activated for the one-time operation.

**Incorrect — baseline-on-migrate true by default:**

```yaml
spring:
  flyway:
    baseline-on-migrate: true          # silent footgun: any missing history table re-baselines
```

**Correct — unset / false; profile-scoped if needed at all:**

```yaml
# application.yml (steady-state)
spring:
  flyway:
    enabled: true                      # baseline-on-migrate unset (= false default)

# application-baseline.yml (activated ONLY for the one-time baseline run)
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

Verification: `./gradlew testPractices --tests "*NoBaselineOnMigrate*"` scans the base `application.yml` and asserts the literals `baseline-on-migrate: true` and `baselineOnMigrate: true` do not appear.

Reference: [Flyway — Baseline](https://documentation.red-gate.com/fd/baseline-184549760.html)


<!-- @source rules/migration-versioned-naming.md -->

---
title: SQL migrations must follow Flyway V{version}__{description}.sql naming
impact: HIGH
impactDescription: "Misnamed migrations are silently skipped by Flyway — schema drift across environments"
tags:
  - migration
  - flyway
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-MIGRATION-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-MIGRATION-001
upstream:
  - "https://docs.spring.io/spring-boot/reference/data/sql.html"
evidence:
  - upstream_id: spring-boot-sql-migration
    section: "Spring Boot — Migration Tool (Flyway)"
    quote: "Flyway"
  - source_type: external
    citation: "Flyway documentation — Migration naming convention"
    url: "https://documentation.red-gate.com/fd/migrations-271583622.html"
---

## SQL migrations must follow Flyway V{version}__{description}.sql naming

**Impact: HIGH — Misnamed migrations are silently skipped by Flyway — schema drift across environments**

Flyway picks up migrations by filename. `V001__create_orders.sql` runs at version 1. `001_create_orders.sql` (no `V`, single underscore) does not match the pattern — Flyway silently ignores it, the schema change never happens in any environment that runs `flyway migrate`, and the bug surfaces only when a developer notices the table does not exist. The naming convention is mechanical and enforceable: `V` prefix, version digits (optionally dotted), DOUBLE underscore, description, `.sql`.

**Incorrect — wrong naming, silently skipped:**

```
src/main/resources/db/migration/
├── 001_create_orders.sql            ← no V prefix, single underscore — SKIPPED
├── V1_create_users.sql              ← single underscore between version and description — SKIPPED
└── v002__add_index.sql              ← lowercase v — SKIPPED
```

**Correct — every file matches V{version}__{description}.sql:**

```
src/main/resources/db/migration/
├── V001__create_users.sql
├── V002__create_orders.sql
└── V003__add_orders_user_id_index.sql
```

Verification: `./gradlew testPractices --tests "*VersionedNaming*"` lists `db/migration/*.sql` and asserts each filename matches `^V[0-9]+(?:[._][0-9]+)*__[A-Za-z0-9_]+\.sql$`.

Reference: [Flyway — Migration naming](https://documentation.red-gate.com/fd/migrations-271583622.html) · [Spring Boot — Flyway integration](https://docs.spring.io/spring-boot/reference/data/sql.html)


<!-- @source rules/multi-tenant-aop-guard-skeleton.md -->

---
title: "Recipes declaring tenant_model: multi must adopt the canonical multi-tenant skeleton — cross-cutting <root>.multitenancy package, TenantOwned marker on every tenant-scoped @Entity, globally-ordered MultiTenantProblemDetailAdvice, and explicit ThreadPoolTaskExecutor with TenantContextAwareTaskDecorator"
rule_id: multi-tenant-aop-guard-skeleton
impact: HIGH
impactDescription: "Without the canonical skeleton, fork-receivers reinvent six load-bearing decisions (package layout, marker interface, advice scope/order, executor bean, tenant_id type, filter activation point) independently — each wrong decision creates a silent cross-tenant leak vector that no current guard detects"
tags:
  - multi-tenant
  - aop
  - cross-cutting
  - skeleton
  - leak-prevention
  - l4-layer
provenance_class: internal_design
protects_template_id: blueprints/multi-tenant-manifest.yaml
failing_fixture_path: practices/evals/fixtures/multi-tenant-aop-guard-skeleton/
spec_ref: "specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-003"
verification:
  type: fixture
  guard_script: practices/evals/multi_tenant_aop_guard_skeleton_guard.sh
  passing_fixture: practices/evals/fixtures/multi-tenant-aop-guard-skeleton/passing/
  failing_fixture: practices/evals/fixtures/multi-tenant-aop-guard-skeleton/failing/
  notes: |
    Mechanical guard (dogfood-5 — promoted from review). Walks every
    `.../multitenancy/` subpackage and asserts the 20 canonical files exist:
      (1) TenantContext.java
      (2) TenantOwned.java
      (3) TenantBoundaryViolationException.java
      (4) TenantContextMissingException.java
      (5) MultiTenantProblemDetailAdvice.java
      (6) TenantAwareAsyncConfig.java
      (7) TenantContextAwareTaskDecorator.java
      (8) TenantFilterActivationFilter.java
      (9) AuthorizedTenant.java                       ← added dogfood-5
      (10) TenantId.java                              ← added dogfood-5
      (11) AuthorizedTenantInterceptor.java           ← added dogfood-5
      (12) AuditEvent.java                            ← added R4 (GAP-R3-3)
      (13) TenantIterationScheduler.java              ← added R6 (GAP-R3-5)
      (14) TenantAwareSseEmitterRegistry.java         ← added R7 (GAP-NEW-1)
      (15) TenantAwareRedisPubSubBridge.java          ← added R8 (GAP-NEW-2)
      (16) TenantAwareKafkaConsumer.java              ← added R9 (kafka-consumer)
      (17) TenantAwareKafkaStreamsTopology.java       ← added R10 (kafka-streams)
      (18) TenantAwareInteractiveQueryService.java    ← added R11 (kafka-streams-iq)
      (19) TenantAwareStandbyForwardingService.java   ← added R12 (kafka-streams-standby-rpc)
      (20) TenantAwareWebClientFilter.java            ← added R13 (webclient-async-tenant-scope)
    Failing-fixture sibling omits (11) — guard MUST trip with --fixtures.
    Body verification (@Around pointcut wiring, generic detail message,
    fail-fast on @TenantId misuse) anchored in manifest interceptor_skeleton.
evidence:
  - source_type: external
    citation: "Hibernate User Guide — Multi-tenancy: @FilterDef('tenantFilter') with row-level discriminator is the documented row-level isolation pattern; the guide is explicit that the filter must be enabled per Session and that 'forgetting to enable the filter is silent — queries simply return rows from all tenants'"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#multitenacy"
    quoted_at: "2026-05-20"
  - source_type: external
    citation: "Spring Framework reference — TaskDecorator: 'A callback interface for a decorator to be applied to any Runnable about to be executed. Note that such a decorator is not necessarily being applied to the user-supplied Runnable/Callable but rather to the actual execution callback (which may be a wrapper around the user-supplied task).' Establishes that TaskDecorator binds at executor level, not at @Async level — confirming the prerequisite_executor_bean requirement"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-task-namespace"
    quoted_at: "2026-05-20"
  - source_type: external
    citation: "RFC 7807 — Problem Details for HTTP APIs §3.1: each problem type has a stable URI; mixing 404 (existence-leakage prevention for cross-tenant access) and 500 (server-side context-missing bug) under one Problem Type URI conflates client and server faults and breaks the contract that 'a problem type SHOULD describe a single class of problem'"
    url: "https://datatracker.ietf.org/doc/html/rfc7807#section-3.1"
    quoted_at: "2026-05-20"
decided_at: "2026-05-20"
---

## multi-tenant-aop-guard-skeleton

**Impact: HIGH — Recipes declaring `tenant_model: multi` MUST adopt the canonical skeleton anchored in `blueprints/multi-tenant-manifest.yaml`. Six load-bearing decisions are pre-resolved there; deviating on any one creates a silent leak vector.**

### Why a skeleton (not free composition)

The P2 dogfooding session on 2026-05-20 attempted to write three concrete files from the multi-tenant blueprint alone:

1. `TenantBoundaryViolationException.java`
2. `TenantContextAwareTaskDecorator.java`
3. `TenantFilterConfig.java`

All three stalled at decisions the blueprint did not pin down:

| Decision | Risk if wrong |
|---|---|
| Which package owns cross-cutting multi-tenant types | placing under a domain package (e.g. `authblueprint.payment.multitenancy`) hides infra inside business code |
| Which `@RestControllerAdvice` scope catches the exception | per-domain `basePackages` advices skip cross-cutting exceptions — exception bubbles out as 500 |
| Whether `@EnableAsync` alone is enough for TaskDecorator | default `SimpleAsyncTaskExecutor` is not decoratable — TenantContext silently lost on every `@Async` call |
| `tenant_id` Java type (`Long` / `UUID` / `String`) | `Long` allows cross-tenant id collision in dev/staging; `String` skips format validation |
| Where Hibernate `@Filter` is activated and disabled | activation in `@Transactional` boundary is too late; missing disable lets pooled sessions leak filter state |
| Whether every `@Entity` carries a marker | without `TenantOwned`, static analysis cannot enumerate tenant-scoped entities — leak vectors are invisible |

Each of these has exactly one correct answer in this codebase. The skeleton encodes those answers.

### Canonical adoption — what fork-receivers copy

When a recipe declares `tenant_model: multi`, the fork-receiver MUST materialise the following at `com.<root>.multitenancy/`:

```
com/<root>/multitenancy/
├── TenantContext.java                          # request-scoped ThreadLocal
├── TenantOwned.java                            # marker interface: UUID getTenantId()
├── TenantBoundaryViolationException.java       # AOP guard violation → 404
├── TenantContextMissingException.java          # async boundary lost context → 500
├── MultiTenantProblemDetailAdvice.java         # global @Order(HIGHEST_PRECEDENCE + 100)
├── TenantAwareAsyncConfig.java                 # explicit ThreadPoolTaskExecutor
├── TenantContextAwareTaskDecorator.java        # captures + restores context across @Async
├── TenantFilterActivationFilter.java           # enables Hibernate @Filter per request
├── AuthorizedTenantInterceptor.java            # service-boundary AOP guard
├── AuditEvent.java                             # @TenantId-annotated audit row (R4)
├── TenantIterationScheduler.java               # per-tenant @Scheduled iteration (R6)
├── TenantAwareSseEmitterRegistry.java          # long-lived push connection registry (R7)
├── TenantAwareRedisPubSubBridge.java           # cross-node broker fan-out bridge (R8, opt-in)
├── TenantAwareKafkaConsumer.java               # tenant-scoped Kafka business-event consumer (R9, opt-in)
├── TenantAwareKafkaStreamsTopology.java        # tenant-scoped Kafka Streams (KStream/KTable) topology (R10, opt-in)
├── TenantAwareInteractiveQueryService.java     # tenant-scoped state-store reads via store.range (R11, opt-in)
├── TenantAwareStandbyForwardingService.java    # tenant-scoped cross-node IQ fan-out (R12, opt-in)
└── TenantAwareWebClientFilter.java             # tenant-scoped outbound WebFlux WebClient filter (R13, opt-in)
```

Each file's body is shipped as `java_skeleton:` block in `blueprints/multi-tenant-manifest.yaml` — adoption is mechanical substitution of `<root>` and integration into existing config.

**Incorrect example: placing multi-tenant infra under a business domain package**

```
com/example/payment/
└── multitenancy/
    └── TenantContext.java                      # VIOLATION
```

Why this fails: every tenant-scoped business domain (`payment`, `notification`, `audit-log`, etc.) ends up importing `payment.multitenancy` — creating a phantom dependency on `payment` from unrelated packages. When `payment` is later forked into its own module, every other domain breaks.

**Correct example: cross-cutting at top level**

```
com/example/
├── multitenancy/
│   └── TenantContext.java                      # OK
├── payment/
├── notification/
└── audit-log/
```

### Incorrect — per-domain @RestControllerAdvice catches TenantBoundaryViolation

```java
@RestControllerAdvice(basePackages = "com.example.payment")
public class PaymentProblemDetailAdvice {
    @ExceptionHandler(TenantBoundaryViolationException.class)  // VIOLATION
    public ResponseEntity<ProblemDetail> handle(...) { ... }
}
```

Why this fails: every domain that throws `TenantBoundaryViolationException` needs its own copy — except `basePackages` is per-advice, so the exception thrown from `notification` is NOT caught by the payment-scoped advice and bubbles as 500. Two outcomes: (a) duplicated advice code, (b) inconsistent HTTP mapping across domains.

### Correct — global advice with explicit @Order

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MultiTenantProblemDetailAdvice {
    @ExceptionHandler(TenantBoundaryViolationException.class)
    public ResponseEntity<ProblemDetail> handle(...) { ... }
}
```

### Incorrect — @EnableAsync alone, no executor bean

```java
@SpringBootApplication
@EnableAsync
public class App {}
// VIOLATION: default SimpleAsyncTaskExecutor — TaskDecorator hook ignored
```

Why this fails: Spring's default `SimpleAsyncTaskExecutor` spawns a new thread per task and exposes no `setTaskDecorator` hook. `TenantContext` is never copied to the worker thread. Every `@Async` call sees an empty ThreadLocal — either NPE at `TenantContext.current().orElseThrow()` OR (worse) silent default-tenant fallback if defensive code masks the missing context.

### Correct — explicit ThreadPoolTaskExecutor with decorator

```java
@Configuration
@EnableAsync
public class TenantAwareAsyncConfig implements AsyncConfigurer {
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new TenantContextAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

### Tenant id type — UUID (canonical)

| Choice | Verdict | Reason |
|---|---|---|
| `java.util.UUID` | CANONICAL | opaque, no cross-tenant collision risk in dev/staging, cross-tenant references detectable in logs |
| `Long` | FORBIDDEN | tenant id `1` in dev and `1` in staging can map to different orgs; not opaque; sequential id leaks tenant order |
| `String` | FORBIDDEN | no format validation at boundary; allows `null`, empty, `"undefined"`, `"NaN"` |

The JWT custom claim name (wire format) is snake_case `tenant_id`; the Java getter is camelCase `getTenantId()` per JavaBeans. Both follow their own ecosystem convention — the skeleton aligns with both.

### Failing fixture

See `practices/evals/fixtures/multi-tenant-aop-guard-skeleton/` (deferred to next round — mechanical guard scaffold). For now this rule operates in `verification.type: review` mode: human review of fork-receiver multi-tenant adoption against the six-decision checklist.

Reference: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#multitenacy


<!-- @source rules/mutation-in-flight-uses-aria-busy.md -->

---
title: In-flight mutations MUST use aria-busy + aria-disabled, not native `disabled`
impact: MEDIUM
impactDescription: "Native `disabled` removes the element from the tab order mid-flight — keyboard users lose focus context and screen readers miss the busy announcement"
tags:
  - a11y
  - aria
  - keyboard-nav
  - mutation
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx"
  pattern: "aria-busy + aria-disabled set during isPending; onClick guards with `if (busy) return`; native `disabled` attribute NOT used for in-flight state"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
  - "https://www.w3.org/TR/wai-aria-1.2/#aria-busy"
evidence:
  - source_type: external
    citation: "WAI-ARIA 1.2 — aria-busy property"
    url: "https://www.w3.org/TR/wai-aria-1.2/#aria-busy"
    quote: "Indicates an element is being modified and that assistive technologies MAY want to wait until the modifications are complete before exposing them to the user."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "Status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
---

## In-flight mutations MUST use aria-busy + aria-disabled, not native `disabled`

**Impact: MEDIUM — native `disabled` is the wrong tool for transient busy state**

The HTML `disabled` attribute is for elements that are *currently not interactive*. A button that is in the middle of dispatching a mutation is conceptually busy, not disabled — the user wants it back as soon as the network round-trip completes, focus should stay on it, and assistive tech should announce "busy, please wait" rather than silently removing the element from interaction.

Native `disabled` does three problematic things during the in-flight window:
1. Removes the element from the tab order, so a keyboard user pressing Tab after the click finds focus suddenly elsewhere when the page re-renders with `disabled=true`.
2. Suppresses click + focus events entirely, so a screen reader has no way to announce status.
3. Gets re-enabled on the next render with no signal about why, so a sighted user who clicked once and saw nothing happen has no model for "should I click again or wait?"

The ARIA replacement is `aria-busy` + `aria-disabled`. Both are properties, not interactivity blockers — the element stays in the tab order, focus is preserved, and the screen reader announces the busy state via the page's aria-live mechanism. To prevent double-fire on rapid clicks, guard inside the click handler: `if (busy) return`.

**Incorrect — native `disabled` mid-mutation:**

```tsx
<button
  type="button"
  disabled={toggle.isPending}         // ❌ removed from tab order, no busy announcement
  onClick={() => toggle.mutate(...)}
>
  Save
</button>
```

**Correct — aria-busy + aria-disabled + click guard:**

```tsx
const busy = isLoading || toggle.isPending

<button
  type="button"
  aria-busy={toggle.isPending || undefined}
  aria-disabled={busy || undefined}
  className="… aria-busy:opacity-60 aria-disabled:opacity-50"
  onClick={() => {
    if (busy) return                 // ✅ double-click guard, focus preserved
    toggle.mutate(...)
  }}
>
  Save
</button>
```

Use `undefined` (not `false`) for the aria props when the state is not active — `aria-busy="false"` is technically valid but tooling-noisy. The `aria-busy:` and `aria-disabled:` Tailwind variants pair cleanly for the visual cue without depending on the native `disabled` style.

This rule pairs with **error-message-not-in-native-title-attribute** — together they keep the button's a11y surface clean during failure modes too.

Reference: [WAI-ARIA 1.2 — aria-busy](https://www.w3.org/TR/wai-aria-1.2/#aria-busy)

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)


<!-- @source rules/mutation-skipped-outcome-surfaces-reason.md -->

---
title: Mutations that may NO-OP (skipped by server invariant) MUST surface the skipped outcome with the server's reason
impact: MEDIUM
impactDescription: "A green-toast 'Success' after a server-skipped mutation tells the operator the work happened when it did not — operator moves on assuming side effects landed"
tags:
  - mutation
  - server-skip
  - outcome-surfacing
  - distributed-lock
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-LOCK-001"
verification:
  source: "templates/L4/scheduled-task/app/(admin)/scheduled-tasks/page.tsx"
  pattern: "trigger.onSuccess sets triggerOutcome state; render differentiates executed=true (green) vs executed=false (amber + reason string from server) instead of collapsing both into one success banner"
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc9457"
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
evidence:
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
    quote: "The 'detail' member is a JSON string containing a human-readable explanation specific to this occurrence of the problem."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
---

## Mutations that may NO-OP (skipped by server invariant) MUST surface the skipped outcome with the server's reason

**Impact: MEDIUM — silent server-skipped mutations train operators to trust outcomes that did not happen**

Some mutations have HTTP 200 success responses that mean "I received your request" — not "I executed the work". The catalog has several:

- **Scheduled-task trigger** — the backend acquires a `DatabaseAdvisoryLock` per task before running. If another instance already holds the lock, the trigger response is `{ executed: false, reason: 'another instance is running this task' }` with HTTP 200. The mutation did not run.
- **Activity-feed mark-read** on an event the caller cannot see — server returns 204 (RFC 9110 idempotent shape) but the read state did not change.
- **Webhook replay** when the partner circuit breaker is open — server queues the request but immediately moves it to DEAD_LETTER without sending. HTTP 200 with no actual delivery.
- **Approval-workflow self-approve attempt** — server-enforced invariant rejects with 409 + reason "requester cannot approve own request" (this one is rejected, not skipped, but the operator-side outcome is the same: "I clicked Approve and nothing happened").

The naive client pattern collapses all `onSuccess` into a single green banner — "Triggered", "Marked read", "Replayed", "Approved". The operator reads the banner and moves on. The mutation did not actually do the work.

The correct pattern requires three properties:
1. **Read the executed/skipped signal** from the response body (`executed: boolean`, or absence of the side effect's confirmation field)
2. **Render differentiated outcome** — green for executed, amber/yellow for skipped, with the server's `reason` quoted verbatim
3. **Don't collapse skipped into error** — a skipped mutation is not a failure (the server enforced an invariant correctly), so it does not belong in the error-alert banner. It belongs in a `role='status'` aria-live region with distinct styling.

**Incorrect — collapse executed/skipped into one success:**

```tsx
const trigger = useMutation({
  mutationFn: triggerTask,
  onSuccess: () => {
    // ❌ Single banner regardless of executed=true/false
    toast.success('Triggered')
  },
})
```

The SRE sees "Triggered" green. The work did not happen. They go back to triaging the next item.

**Correct — surface skipped with reason:**

```tsx
const [triggerOutcome, setTriggerOutcome] = React.useState<{
  taskId: string
  executed: boolean
  reason: string | null
} | null>(null)

const trigger = useMutation({
  mutationFn: triggerTask,
  onMutate: () => setTriggerOutcome(null),
  onSuccess: (resp, id) => {
    // Server may return executed: false when DatabaseAdvisoryLock blocks
    setTriggerOutcome({ taskId: id, executed: resp.executed, reason: resp.reason })
  },
})

// In JSX:
{triggerOutcome && (
  <div
    role="status"
    aria-live="polite"
    className={`rounded border px-3 py-1.5 text-sm ${
      triggerOutcome.executed
        ? 'border-green-300 bg-green-50 text-green-900'
        : 'border-amber-300 bg-amber-50 text-amber-900'
    }`}
  >
    {triggerOutcome.executed
      ? 'Trigger accepted — job queued for execution.'
      : `Trigger skipped — ${triggerOutcome.reason ?? 'another instance is running this task'}`}
  </div>
)}
```

Three properties confirmed:
- (1) reads `resp.executed`
- (2) green-vs-amber differentiation with the server's `resp.reason`
- (3) `role='status'` (not `role='alert'`) because skipped-by-invariant is not an error

**When to apply**: any mutation whose backend documents a "no-op success" path — distributed-lock skip, circuit-breaker skip, idempotent-already-applied skip, invariant-enforced skip. The catalog convention is to give those endpoints a discriminated response (`executed: boolean` plus `reason: string | null`) so the client can render unambiguously.

**When NOT to apply**: mutations where the server's contract guarantees side effects landed on every HTTP 200 (most CRUD). Single green toast / inline confirmation is fine there.

Pairs with `destructive-action-confirm-with-side-effects` — the confirm dialog tells the operator what *will* happen; this rule's outcome banner tells them what *did* happen.

Reference: [RFC 9457 — Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc9457)

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)


<!-- @source rules/no-billing-cross-import-from-payment.md -->

---
title: "billing and payment packages must not import each other; the boundary defined in §5.2.6 is enforced by ArchUnit and ESLint"
rule_id: no-billing-cross-import-from-payment
impact: CRITICAL
impactDescription: "Cross-importing between billing and payment creates a circular bounded-context dependency. Any change to payment internals (e.g., PaymentMethod, PaymentStatus) leaks into billing and forces cascading changes. Subscription lifecycle (billing domain) must never depend on one-shot charge logic (payment domain)."
tags:
  - billing
  - payment
  - boundary
  - ddd
  - domain-separation
provenance_class: internal_design
protects_template_id: templates/backend/billing/BillingService.java
failing_fixture_path: practices/evals/fixtures/no-billing-cross-import-from-payment/fail_billing_imports_payment/
spec_ref: "specs/billing-l0.yaml#BILLING-BOUNDARY-001"
verification:
  type: archunit
  notes: |
    ArchUnit rules (two directional):
    noClasses().that().resideInAPackage("..billing..")
        .should().dependOnClassesThat().resideInAPackage("..payment..")
    noClasses().that().resideInAPackage("..payment..")
        .should().dependOnClassesThat().resideInAPackage("..billing..")
    Failing fixture: any billing class with import ax.template.payment.* or vice versa.
evidence:
  - source_type: external
    citation: "Domain-Driven Design (Evans): Each bounded context has an explicit contract at its boundary. Cross-importing internals couples contexts at the class level, violating autonomy and enabling cascading changes."
    url: "https://martinfowler.com/bliki/BoundedContext.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Stripe API Reference 2026-05 — Charges vs. Subscriptions are separate API resources with no direct dependency between them. A subscription's lifecycle uses invoice and billing objects, not charge objects."
    url: "https://stripe.com/docs/api/subscriptions"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Sam Newman — Building Microservices (2nd ed.): Services in separate bounded contexts must communicate via published events or APIs, never via direct class-level imports."
    url: "https://samnewman.io/books/building_microservices_2nd_edition/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## billing ↔ payment cross-import is prohibited

**Impact: CRITICAL — billing domain (subscription lifecycle, invoices, recurring events) and payment domain (one-shot authorize/capture/refund) are separate bounded contexts per §5.2.6. Cross-imports couple contexts at the Java class level, breaking independent deployability and forcing cascading changes.**

### §5.2.6 Payment vs Billing Boundary

| Concern | Owner |
|---|---|
| One-shot authorize/capture/refund | `payment` domain |
| Subscription lifecycle | `billing` domain |
| Invoice issuance | `billing` domain |
| Plan management | `billing` domain |
| Recurring billing event normalization | `billing` domain |

Communication between the domains, if needed, must go through:
1. **Application events** (Spring `ApplicationEvent` or Kafka topic)
2. **Shared kernel** types only (primitives, common value objects in a `shared` package)

Direct Java `import ax.template.payment.*` or `import ax.template.billing.*` from the opposing context is **prohibited**.

**Incorrect — billing imports payment internals (cross-context dependency):**

```java
// VIOLATION: billing service directly importing payment domain class
package ax.template.billing;

import ax.template.payment.PaymentMethod;      // ← VIOLATION
import ax.template.payment.PaymentService;     // ← VIOLATION

@Service
public class BillingService {
    private final PaymentService paymentService;
    public void renewSubscription(UUID subId) {
        paymentService.charge(...); // cross-context direct call — forbidden
    }
}
```

**Correct — billing domain coordinates via ApplicationEvent, no payment imports:**

```java
// CORRECT: billing emits an event; payment coordinator listens (no payment.* import)
package ax.template.billing;

@Service
public class BillingService {
    private final ApplicationEventPublisher events;
    @Transactional
    public void handleRenewal(UUID subscriptionId) {
        events.publishEvent(new SubscriptionRenewalDueEvent(subscriptionId, amountDue));
        // No payment import needed — payment domain handles via its own listener
    }
}
```

Reference: https://martinfowler.com/bliki/BoundedContext.html

### Incorrect — payment imports billing internals

```java
// VIOLATION: payment domain importing billing domain class
package ax.template.payment;

import ax.template.billing.Subscription;       // ← VIOLATION
import ax.template.billing.BillingEvent;       // ← VIOLATION

@Service
public class PaymentService {
    public void processRefund(UUID subId) {
        // Should not know about Subscription entity
        Subscription sub = subscriptionRepository.findById(subId);
    }
}
```

### Correct — event-driven coordination

```java
// CORRECT: billing emits an event; payment (or a coordinator) listens
// billing domain:
@Service
public class BillingService {
    private final ApplicationEventPublisher events;

    @Transactional
    public void handleSubscriptionRenewal(UUID subscriptionId) {
        // ... state machine transition ...
        events.publishEvent(new SubscriptionRenewalDueEvent(subscriptionId, amountDue));
        // No payment import needed
    }
}

// Coordinator (shared layer or separate service) — NOT in billing or payment:
@Component
public class RenewalCoordinator {
    @EventListener
    public void onRenewalDue(SubscriptionRenewalDueEvent event) {
        // Calls payment domain via its API, not its internals
        paymentGateway.charge(event.subscriptionId(), event.amountDue());
    }
}
```

### Correct — shared kernel for common types only

```java
// shared package (not billing, not payment) — OK to import from either context:
package ax.template.shared;

public record MoneyAmount(long amount, String currency) {}
public record UserId(UUID value) {}
```

## ArchUnit enforcement

```java
// BillingPaymentBoundaryArchTest.java
@ArchTest
static final ArchRule billingMustNotImportPayment = noClasses()
    .that().resideInAPackage("..billing..")
    .should().dependOnClassesThat().resideInAPackage("..payment..")
    .because("billing and payment are separate bounded contexts (§5.2.6)");

@ArchTest
static final ArchRule paymentMustNotImportBilling = noClasses()
    .that().resideInAPackage("..payment..")
    .should().dependOnClassesThat().resideInAPackage("..billing..")
    .because("billing and payment are separate bounded contexts (§5.2.6)");
```

## Failing fixture

See: `practices/evals/fixtures/no-billing-cross-import-from-payment/fail_billing_imports_payment/BillingServiceCrossImport.java` — a billing service that imports `ax.template.payment.PaymentService`.

See: `practices/evals/fixtures/no-billing-cross-import-from-payment/pass_idempotency_pattern_no_import/BillingServiceNoPaymentImport.java` — correct billing service with no payment imports.


<!-- @source rules/no-rrn-collection-without-legal-basis.md -->

---
title: "Backend services must not accept, store, or process raw RRN (주민등록번호) without an explicit @LegalBasis annotation"
rule_id: no-rrn-collection-without-legal-basis
impact: CRITICAL
impactDescription: "RRN is a Sensitive Personal Information (고유식별정보) under 개인정보보호법 §24; processing it without explicit statutory legal basis triggers mandatory breach notification and fines up to ₩30M per violation"
tags:
  - privacy
  - pii
  - rrn
  - identity
  - locked_constraint
  - korean-compliance
provenance_class: locked_constraint
protects_template_id: templates/backend/identity-verification/
failing_fixture_path: practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/
spec_ref: "specs/identity-verification-l0.yaml#IDV-CALLBACK-003"
verification:
  type: review
  status: manual
  notes: "Static analysis: grep -rn '@RequestParam\\|@RequestBody\\|String.*rrn\\|String.*주민' --include='*.java' | grep -v '@LegalBasis\\|//.*CORRECT\\|test/\\|fixture/' must return zero matches in production code. Structural check: VerifiedIdentity entity must have no field named rrn/residentRegistrationNumber/socialSecurityNumber."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 제1항 — 고유식별정보의 처리 제한: 사업자는 법령에 특별한 규정이 있는 경우 외에는 주민등록번호 등 고유식별정보를 처리할 수 없음"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 본인인증 가이드라인 — CI(연결정보)/DI(중복확인정보)를 이용하여 주민등록번호를 수집하지 않고 본인인증을 수행하는 방법"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP ASVS V6.2.1 — Verify that regulated private data is stored encrypted at rest and that this data cannot be easily decrypted"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Backend services must not accept, store, or process raw RRN (주민등록번호) without an explicit `@LegalBasis` annotation

**Impact: CRITICAL — 개인정보보호법 §24-1 classifies the Resident Registration Number (주민등록번호) as 고유식별정보 (Unique Identification Information). Processing it without a specific statutory basis is prohibited and carries:**
- **Administrative fines up to ₩30M per violation**
- **Mandatory breach notification obligations**
- **Criminal liability for responsible officers (up to 5 years imprisonment, ₩50M fine)**

This rule is a **locked constraint**: it derives from statute and cannot be relaxed by project-level override.

**Incorrect — DTO accepts raw RRN without @LegalBasis annotation:**

```java
// VIOLATION: RRN in DTO without @LegalBasis — 개인정보보호법 §24 violation
@PostMapping("/api/users/register")
public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
    userService.register(request.getName(), request.getRrn());
    return ResponseEntity.ok().build();
}
public record RegistrationRequest(String name, String email, String rrn) {}
```

**Correct — use CI/DI from KISA 본인인증 instead of RRN:**

```java
// CORRECT — identity verified via CI/DI; no RRN field in any DTO
@PostMapping("/api/users/register")
public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
    userService.registerWithVerifiedIdentity(request.getName(), request.ci());
    return ResponseEntity.ok().build();
}
public record RegistrationRequest(String name, String email, String ci) {}
```

Reference: https://www.law.go.kr/법령/개인정보보호법

### If RRN processing is legally required (rare statutory case)

```java
// ✅ CORRECT (statutory exception only) — @LegalBasis annotation is mandatory
@PostMapping("/api/kyc/verify")
public ResponseEntity<Void> kycVerify(@RequestBody KycRequest request) {
    // CORRECT: @LegalBasis documents the specific statute
    kycService.verifyWithRrn(request.getRrn());
    return ResponseEntity.ok().build();
}

public record KycRequest(
    @LegalBasis(law = "금융실명거래 및 비밀보장에 관한 법률 §3",
                purpose = "금융거래 실명확인 — 법령상 수집 의무",
                retentionYears = 5)
    String rrn   // STATUTORY EXCEPTION: documented legal basis required
) {}
```

### Why this matters

개인정보보호법 §24 and related statutes impose:
1. **Collection prohibition** — Unless a specific law (금융실명법, 주민등록법 §7의5 등) authorizes it.
2. **Separate consent requirement** — A specific, separate consent gate (§18).
3. **Encryption requirement** — If collected, must be stored encrypted (§29).
4. **Minimum necessary principle** — Collect only the minimum required for the stated purpose.

For identity verification (본인인증), KISA provides a lawful alternative:
- **PASS / KCB 본인인증** produces CI (Connecting Information) and DI (Duplicate Information)
- CI is a 64-byte hex token that uniquely identifies a person across services — **without the RRN**
- Use `templates/backend/identity-verification/` for the vendor-agnostic adapter pattern

### RRN field name patterns this rule targets

```
rrn, residentRegistrationNumber, socialSecurityNumber, idNumber (context: RRN),
주민등록번호, 주민번호, juminNumber, rrNum
```

Exclusions (false-positive guard per Risk 4 in PRD):
```
ci, di, verifiedIdentityNumber, externalId, connectingInfo, duplicateInfo
```

## Failing fixture

See: `practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/`
— A DTO with a field named `rrn` and no `@LegalBasis` annotation. Static analysis catches field name pattern.

React companion rule: `practices-react/rules/no-rrn-collection-without-legal-basis.md`

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한](https://www.law.go.kr/법령/개인정보보호법)

Reference: [KISA 본인인증 가이드라인 — CI/DI 대체 방법](https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO)


<!-- @source rules/no-rrn-logging.md -->

---
title: "RRN (주민등록번호) must never appear in any log statement at any level"
rule_id: no-rrn-logging
impact: CRITICAL
impactDescription: "RRN is Sensitive Personal Information under 개인정보보호법 §24; its appearance in application logs constitutes an unauthorized disclosure breach"
tags:
  - privacy
  - pii
  - rrn
  - observability
  - locked_constraint
provenance_class: locked_constraint
protects_template_id: templates/backend/global-exception-handler/GlobalExceptionHandler.java
failing_fixture_path: practices/evals/fixtures/no-rrn-logging/fail_rrn_in_log/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  type: review
  notes: "Static analysis: grep -rn 'log\\.' --include='*.java' | grep -i 'rrn\\|주민' must return zero matches in production code."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 — 고유식별정보의 처리 제한 (Korean Personal Information Protection Act §24 — Restrictions on Processing Unique Identification Information)"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 개인정보 기술적·관리적 보호조치 기준 — 접속기록의 위변조방지 및 RRN 처리"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude: sensitive personal identifiers must never be written to log files"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## RRN (주민등록번호) must never appear in any log statement at any level

**Impact: CRITICAL — 개인정보보호법 §24 classifies the Resident Registration Number as a unique identification information (고유식별정보); its unauthorized disclosure triggers mandatory breach notification and administrative penalties.**

Application logs are retained by aggregators, SIEMs, object-storage buckets, and developer workstations. Any `log.info(...)`, `log.debug(...)`, `log.warn(...)`, or `log.error(...)` statement that includes an RRN constitutes an unauthorized disclosure if any of those sinks are accessed by personnel without proper clearance.

This rule is a **locked constraint**: it derives from statute (개인정보보호법 §24) rather than engineering preference. It cannot be relaxed by project-level override.

**Incorrect — RRN written to INFO and DEBUG log levels:**

```java
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // VIOLATION: RRN in log — 개인정보보호법 §24 breach
        log.info("registering user {} with RRN: {}", name, rrn);
        log.debug("verifying identity for rrn={}", rrn);
    }
}
```

**Correct — log a non-sensitive identifier only:**

```java
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // CORRECT: only the name (non-sensitive) is logged
        log.info("registering user name={}", name);
        // RRN processed in-memory and never emitted to any log sink
    }

    public void verifyIdentity(String rrn) {
        // CORRECT: log the outcome, not the RRN
        log.debug("identity verification attempted");
        boolean result = doVerify(rrn);
        log.info("identity verification result={}", result);
    }
}
```

## Why this matters

개인정보보호법 §24 imposes:
- Mandatory consent before collecting unique identification information
- Processing restrictions: only the minimum necessary for the stated purpose
- **Disclosure prohibition**: unauthorized disclosure (including to a log aggregator) triggers notification duties and fines up to ₩30M per violation

Application logs flow to: log aggregators (ELK/OpenSearch), S3 retention, developer terminals, CI artifact stores. None of these are controlled personal-information processing systems under §24.

The safe default is to **never log the RRN**, not to try to redact it downstream. Log scrubbers are best-effort and routinely bypass new fields.

## Failing fixture

See: `practices/evals/fixtures/no-rrn-logging/fail_rrn_in_log/UserService.java` — `log.info` and `log.debug` statements containing the `rrn` variable. A static analysis guard scanning for `log\.\(info\|debug\|warn\|error\).*rrn` catches both.

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한 (Korean Personal Information Protection Act §24)](https://www.law.go.kr/법령/개인정보보호법)

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)


<!-- @source rules/observability-mdc-trace-propagation.md -->

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


<!-- @source rules/observability-no-pii-in-logs.md -->

---
title: Redact PII (including PAN) before it enters a log statement
impact: HIGH
impactDescription: "Application logs are indexed and retained; raw PII or PAN is a compliance + breach-radius hazard"
tags:
  - observability
  - security
  - pii
  - pci-dss
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-003
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
  - "https://www.pcisecuritystandards.org/document_library/?category=saqs"
evidence:
  - upstream_id: owasp-logging-cheatsheet
    section: "OWASP Logging Cheat Sheet — Data to exclude"
    quote: "exclude"
  - upstream_id: pci-dss-saq-a
    section: "Requirement 3.4 — PAN rendered unreadable"
    quote: "PAN is rendered unreadable anywhere it is stored"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
  - source_type: external
    citation: "PCI-DSS v4.0 Requirement 3.4 — PAN rendered unreadable"
    url: "https://www.pcisecuritystandards.org/document_library/"
---

## Redact PII (including PAN) before it enters a log statement

**Impact: HIGH — Application logs are indexed and retained; raw PII or PAN is a compliance + breach-radius hazard**

Logs flow through aggregators, SIEMs, retention buckets, backups, and developer terminals. Anything written to a log statement is — practically — broadcast to a wider audience than the original request handler ever was. Per the OWASP Logging Cheat Sheet, the safe default is to redact PII (email, phone, SSN, payment data, session tokens) *at the source*: before the string is handed to `log.info(...)`. Sanitising downstream (log scrubbers) is best-effort and routinely bypassed by new fields.

For payment-handling code the bar is stricter: PCI-DSS Requirement 3.4 mandates that the Primary Account Number (PAN — the 13-19 digit card number) be rendered unreadable wherever it is stored, **including in application logs**. The token-vs-PAN distinction matters: an opaque provider-issued token is safe to log, but the raw PAN, CVV, expiration date, and any combination of those is Sensitive Authentication Data (SAD) that must never appear in plaintext. Use `@JsonIgnore` on PAN-bearing fields plus an explicit `toString()` override that returns `[REDACTED]`.

**Incorrect — raw user data in a log message:**

```java
String email = user.getEmail();
String phone = user.getPhone();
String pan = paymentMethod.getPan();   // 16-digit card number
log.info("password reset for user " + email + " phone " + phone + " card " + pan);
```

**Correct — redactor at the boundary:**

```java
log.info("password reset for user {}", PiiRedactor.redact(user.identifier()));
// or, prefer structured fields with a known-safe id:
log.atInfo().addKeyValue("user_id", user.id()).setMessage("password reset").log();
```

**Correct — PAN-bearing field with @JsonIgnore + toString override:**

```java
public final class PaymentMethodToken {
    @JsonIgnore
    private final String rawPan;   // never serialized to JSON, never logged

    public PaymentMethodToken(String rawPan) { this.rawPan = rawPan; }

    @Override
    public String toString() {
        return "[REDACTED]";   // log.info("token={}", token) → "token=[REDACTED]"
    }
}
```

Verification: `./gradlew testPractices --tests "*NoPiiInLogs*"` exercises the `PiiRedactor` over emails / phones / SSNs / 16-digit card numbers and asserts the original strings are gone, the redaction markers are present, and clean strings pass through unchanged. PAN coverage is additionally asserted by the Payment blueprint's `PanRedactionTest` (`./gradlew testPayment --tests "*PanRedaction*"`).

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)

Reference: [PCI Security Standards Council — Document Library (PCI-DSS v4.0 Requirement 3.4)](https://www.pcisecuritystandards.org/document_library/)


<!-- @source rules/observability-structured-logging.md -->

---
title: Emit structured key-value pairs, not concatenated log strings
impact: MEDIUM
impactDescription: "JSON appenders can index typed fields; concatenated strings can only be grep-searched"
tags:
  - observability
  - logging
  - structured-logs
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-001
upstream:
  - "https://logback.qos.ch/manual/layouts.html"
evidence:
  - upstream_id: logback-layouts
    section: "Logback PatternLayout and structured encoders"
    quote: "PatternLayout"
  - source_type: external
    citation: "SLF4J 2.x — Fluent logging API (addKeyValue)"
    url: "https://www.slf4j.org/manual.html#fluent"
---

## Emit structured key-value pairs, not concatenated log strings

**Impact: MEDIUM — JSON appenders can index typed fields; concatenated strings can only be grep-searched**

Once logs land in a search system (Elasticsearch, Loki, CloudWatch Insights), the difference between `"order processed order_id=ord-123 amount=42"` and a structured event `{message: "order processed", order_id: "ord-123", amount: 42}` is the difference between regex-hunting and `order_id:"ord-123"` queries. SLF4J 2.x's fluent API and Logback's structured encoders preserve typed key-value pairs end-to-end; concatenation discards them at the call site.

**Incorrect — string-concatenated log:**

```java
String message = "order processed order_id=" + order.id() + " amount=" + order.amount();
log.info(message);
```

**Correct — structured key-value pairs:**

```java
log.atInfo()
   .addKeyValue("order_id", order.id())
   .addKeyValue("amount", order.amount())
   .setMessage("order processed")
   .log();
```

Verification: `./gradlew testPractices --tests "*StructuredLogging*"` attaches a Logback ListAppender, exercises both code paths, and asserts the structured path emits `KeyValuePair`s while the concatenated path emits none.

Reference: [SLF4J Fluent API](https://www.slf4j.org/manual.html#fluent) · [Logback Layouts](https://logback.qos.ch/manual/layouts.html)


<!-- @source rules/optimistic-update-snapshot-rollback.md -->

---
title: Optimistic update MUST snapshot-and-rollback — never invalidate-only
impact: MEDIUM
impactDescription: "Invalidate-only patterns leave UI lagging one network round-trip behind every action; without rollback, transient failures leave the cache stuck in the wrong state"
tags:
  - tanstack-query
  - optimistic-update
  - mutation
  - cache-coherence
spec_ref: "specs/activity-feed-l0.yaml#ACT-MARK-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx, templates/L4/favorites-bookmarks/app/(favorites)/page.tsx"
  pattern: "onMutate snapshot + setQueryData optimistic write + onError ctx.previous rollback + onSettled invalidate"
upstream:
  - "https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates"
  - "https://tanstack.com/query/latest/docs/framework/react/guides/mutations"
evidence:
  - source_type: external
    citation: "TanStack Query v5 — Optimistic Updates via the Cache"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates"
    quote: "When we want to optimistically update some state before the mutation is completed, we can use the onMutate option. ... The data returned from onMutate is passed to the onError handler so it can be used to undo the optimistic update."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "TanStack Query v5 — useMutation API"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/mutations"
    quote: "onError, retry, retryDelay, scope: { id }, onMutate(variables): ... — A function that fires before the mutation function is fired. Useful to perform optimistic updates to a resource in hopes that the mutation succeeds."
    quoted_at: "2026-05-25"
---

## Optimistic update MUST snapshot-and-rollback — never invalidate-only

**Impact: MEDIUM — invalidate-only is a lie about latency; un-rolled-back failure is a worse lie about state**

The simplest pattern for a mutation in TanStack Query is `onSuccess: () => qc.invalidateQueries(queryKey)`. It is correct but slow: the UI does not change until the invalidated query refetches over the network. For mutations whose user-perceived correctness depends on immediate visual feedback (toggles, removes, marks-as-read), this latency is unacceptable — and the in-flight window introduces a fresh race:

> Click 1 fires. Cache still says `favorited: false`. UI shows ☆. Mutation in flight.
> Click 2 within the RTT reads cache `{ favorited: false }` and fires *another* add. Duplicate-key on add, or harmless redundant DELETE on remove.

The correct pattern has four parts, all required, in this order:
1. **onMutate** — cancel any in-flight refetch of the affected key; snapshot the current cache value as a return context; write the optimistic new value into the cache.
2. **The mutation reads its decision from variables, not the cache.** Either the caller passes the direction explicitly (`mutate({ direction: 'add' })`) or onMutate captures it into the returned context before flipping. Do *not* re-read `data?.favorited` inside mutationFn — that read is what the snapshot was meant to replace.
3. **onError** — restore the snapshot from the context. Cache returns to backend truth.
4. **onSettled** — invalidate the affected key (and any cross-query family that mirrors the same state) so the next refetch reconciles against the backend.

This pattern combines latency reduction (cache flips at onMutate) with truth preservation (snapshot restoration on failure) and cross-query coherence (family-key invalidation at onSettled).

**Incorrect — invalidate-only with cache read inside mutationFn:**

```tsx
const toggle = useMutation({
  mutationFn: async () => {
    // ❌ reads cache mid-mutation — second rapid click re-reads stale value
    if (data?.favorited) await removeFavorite(...)
    else await addFavorite(...)
  },
  onSuccess: () => qc.invalidateQueries({ queryKey }),
})
```

**Correct — onMutate snapshot + direction variables + onError rollback + onSettled invalidate:**

```tsx
type Direction = 'add' | 'remove'

const toggle = useMutation({
  mutationFn: async (direction: Direction) => {
    if (direction === 'remove') await removeFavorite(...)
    else await addFavorite(...)
  },
  onMutate: async (direction) => {
    await qc.cancelQueries({ queryKey })
    const previous = qc.getQueryData<CheckResponse>(queryKey)
    qc.setQueryData<CheckResponse>(queryKey, { favorited: direction === 'add' })
    return { previous }
  },
  onError: (_err, _direction, ctx) => {
    if (ctx?.previous) qc.setQueryData(queryKey, ctx.previous)
    qc.invalidateQueries({ queryKey: ['related-list'] })  // cross-query coherence on error
  },
  onSettled: () => {
    qc.invalidateQueries({ queryKey })
    qc.invalidateQueries({ queryKey: ['related-list'] })  // family-key invalidate
  },
})

// At the click site: snapshot direction from current cache, pass into mutate:
<button onClick={() => {
  if (busy) return
  toggle.mutate(favorited ? 'remove' : 'add')
}} />
```

For a list-removal pattern (remove a row from a paginated list), the snapshot+rollback target is the list query data, and the optimistic write is a `filter()` over `items`:

```tsx
onMutate: async ({ entityType, entityId }) => {
  await qc.cancelQueries({ queryKey: ['list'] })
  const previous = qc.getQueryData<ListResponse>(['list'])
  qc.setQueryData<ListResponse>(['list'], (old) =>
    old ? { ...old, items: old.items.filter((it) => !(it.entityType === entityType && it.entityId === entityId)) } : old
  )
  return { previous }
},
onError: (_err, _vars, ctx) => {
  if (ctx?.previous) qc.setQueryData(['list'], ctx.previous)
},
onSettled: () => qc.invalidateQueries({ queryKey: ['list'] }),
```

The "no fabricated timestamps" rule (`client-must-not-fabricate-audit-timestamps`) pairs with this one: when the optimistic state includes an audit timestamp, hold the pending state in a component-local typed Set rather than writing a synthetic timestamp into the cache. The cache should only ever carry backend truth or null.

Reference: [TanStack Query v5 — Optimistic Updates](https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates)

Reference: [TanStack Query v5 — Mutations](https://tanstack.com/query/latest/docs/framework/react/guides/mutations)


<!-- @source rules/payment-iso-4217-currency.md -->

---
title: Currency codes must be ISO 4217 alpha-3 and the amount scale must match the currency's minor-unit count
impact: HIGH
impactDescription: "A KRW amount with two decimal places, or a BHD amount with two decimals, silently misrepresents value by orders of magnitude"
tags:
  - payment
  - validation
  - iso-4217
  - currency
spec_ref: "specs/payment-l0.yaml#PAYMENT-MONEY-003"
verification:
  gradle_task: testPayment
  tag: PAYMENT-MONEY-003
upstream:
  - "https://www.iso.org/iso-4217-currency-codes.html"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html"
evidence:
  - upstream_id: iso-4217
    section: "Minor unit (scale) per currency"
    quote: "minor unit"
  - source_type: external
    citation: "ISO 4217 — Codes for the representation of currencies (ISO)"
    url: "https://www.iso.org/iso-4217-currency-codes.html"
  - source_type: external
    citation: "java.util.Currency.getDefaultFractionDigits() — Java SE 21 API"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html#getDefaultFractionDigits()"
---

## Currency codes must be ISO 4217 alpha-3 and the amount scale must match the currency's minor-unit count

**Impact: HIGH — A KRW amount with two decimal places, or a BHD amount with two decimals, silently misrepresents value by orders of magnitude**

ISO 4217 fixes a three-letter alpha-3 code per currency (KRW, USD, JPY, EUR, BHD, ...) and the **minor-unit count** — the number of digits after the decimal separator that the currency uses canonically. KRW and JPY are 0-decimal currencies (한국 원 and 円 do not subdivide); USD, EUR, GBP and most others are 2-decimal; BHD, KWD, OMR are 3-decimal; UYW and CLF are 4-decimal. Treating one as another silently scales the value: `100` interpreted as KRW means 100원, but interpreted as USD-with-implicit-cents means $1.00 — a factor-of-100 discrepancy. The bug is hard to detect from the inside because the integer arithmetic is exact; only a per-currency validator that consults `Currency.getInstance(code).getDefaultFractionDigits()` catches it. Mandating both the code lookup (well-formed alpha-3) **and** the scale check (`BigDecimal.scale() == Currency.getDefaultFractionDigits()`) closes the gap.

This rule sits in the `payment-*` namespace because — at the time of writing — Payment is the only multi-currency domain in the catalog. A future Invoice / FX / Billing blueprint with a second multi-currency surface would justify promoting this rule to `validation-currency-code.md` under the generic `validation-*` namespace; the promotion trigger is documented in `practices/DECISIONS.md`.

**Incorrect — currency is an arbitrary string; amount scale is whatever the deserializer happened to produce:**

```java
public record CreatePaymentRequest(
        BigDecimal amount,
        String currency,           // accepts "krw", "krwon", "XYZ", anything
        String orderId
) {
    // no validation: amount=10.99, currency="KRW" → stored as 10.99원
    // (KRW has no sub-units; the .99 is silently meaningless)
}
```

**Correct — Currency.getInstance + per-currency scale assertion:**

```java
public record CreatePaymentRequest(
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String orderId
) {}

@Service
public class CurrencyValidator {

    public void validate(BigDecimal amount, String currency) {
        Currency iso;
        try {
            iso = Currency.getInstance(currency);          // throws IllegalArgumentException if not ISO 4217
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currency);   // → 400 RFC 7807, type=urn:ax:payment:invalid-currency
        }
        int allowedScale = iso.getDefaultFractionDigits(); // KRW=0, USD=2, BHD=3
        if (amount.scale() > allowedScale) {
            throw new ScaleMismatchException(currency, allowedScale, amount.scale());
        }
    }
}
```

Pair this rule with `lang-bigdecimal-for-money.md` (which forbids `double`/`float` for monetary fields) and with a Jackson deserializer that rejects JSON number tokens with a decimal point. The wire-side accepted shapes are **integer minor units** (KRW `1000`, USD `1099`, BHD `10250`) or **explicit decimal strings** with exactly `getDefaultFractionDigits()` digits after the point (KRW `"1000"`, USD `"10.99"`, BHD `"10.250"`). JSON floats are never accepted.

Verification: `./gradlew testPayment --tests "*Currency*"` exercises: (a) `{"currency": "XYZ"}` → 400; (b) `{"currency": "KRW", "amount": "10.99"}` → 400 (scale violation); (c) `{"currency": "USD", "amount": "10.999"}` → 400 (3 digits > USD's 2); (d) `{"currency": "KRW", "amount": 1000}` → 201; (e) `{"currency": "BHD", "amount": "10.250"}` → 201 (3 digits matches BHD scale).

Reference: [ISO 4217 — Codes for the representation of currencies](https://www.iso.org/iso-4217-currency-codes.html)

Reference: [java.util.Currency — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Currency.html)


<!-- @source rules/persistence-batch-inserts.md -->

---
title: Configure hibernate.jdbc.batch_size + order_inserts for bulk persists
impact: HIGH
impactDescription: "Without batch_size, every persist is a round-trip — 10× or worse latency on bulk paths"
tags:
  - persistence
  - jpa
  - performance
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-003
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: "Spring Data JPA — query methods & JPA properties"
    quote: "hibernate"
  - source_type: external
    citation: "Hibernate User Guide — Batching"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch"
---

## Configure hibernate.jdbc.batch_size + order_inserts for bulk persists

**Impact: HIGH — Without batch_size, every persist is a round-trip — 10× or worse latency on bulk paths**

The defaults persist one entity per JDBC round-trip. For 200 inserts in one transaction that's 200 round-trips. `hibernate.jdbc.batch_size = N` instructs Hibernate to pack up to N inserts into a single JDBC batch; `hibernate.order_inserts = true` reorders them so same-table inserts cluster (the batch can only span identical statements). Without `order_inserts` the batch is fragmented and most rounds-trips remain. Both flags belong on the EntityManagerFactory; they are not per-method choices.

**Incorrect — defaults: one round-trip per insert:**

```yaml
spring:
  jpa:
    properties:
      # nothing here — silent N round-trips on bulk paths
```

**Correct — batch_size and order_inserts together:**

```yaml
spring:
  jpa:
    properties:
      hibernate.jdbc.batch_size: 20
      hibernate.order_inserts: true
      hibernate.order_updates: true
```

Verification: `./gradlew testPractices --tests "*BatchInsert*"` sets the properties via @TestPropertySource and asserts `EntityManagerFactory.getProperties()` carries them.

Reference: [Hibernate User Guide — Batching](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch)


<!-- @source rules/persistence-entity-graph.md -->

---
title: Prefer @EntityGraph for annotation-driven fetch shape
impact: MEDIUM
impactDescription: "Same N+1 remedy as JOIN FETCH but expressed declaratively at the repository surface"
tags:
  - persistence
  - jpa
  - entity-graph
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-002
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: "Spring Data JPA — EntityGraph attribute paths"
    quote: "EntityGraph"
  - source_type: external
    citation: "Spring Data JPA Reference — Configuring Fetch- and LoadGraphs"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph"
---

## Prefer @EntityGraph for annotation-driven fetch shape

**Impact: MEDIUM — Same N+1 remedy as JOIN FETCH but expressed declaratively at the repository surface**

`JOIN FETCH` works, but it tangles JPQL projection with fetch shape and forces every variant query to repeat the join. `@EntityGraph(attributePaths = {"children"})` keeps the JPQL focused on filtering and declares the fetch contract at the method signature. Spring Data merges the graph hint into the query when it's executed — same single round-trip, cleaner separation.

**Incorrect — fetch shape woven into every JPQL string:**

```java
@Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children WHERE p.tenantId = :tenantId")
List<Parent> findByTenantWithChildren(Long tenantId);

@Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children WHERE p.archived = false")
List<Parent> findActiveWithChildren();
```

**Correct — single fetch contract at the annotation:**

```java
@EntityGraph(attributePaths = {"children"})
@Query("SELECT p FROM Parent p WHERE p.tenantId = :tenantId")
List<Parent> findByTenant(Long tenantId);

@EntityGraph(attributePaths = {"children"})
@Query("SELECT p FROM Parent p WHERE p.archived = false")
List<Parent> findActive();
```

Verification: `./gradlew testPractices --tests "*EntityGraph*"` asserts the annotation-driven method produces exactly one prepared statement on the seeded 3×2 fixture.

Reference: [Spring Data JPA — EntityGraph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)


<!-- @source rules/persistence-no-n-plus-1.md -->

---
title: Prevent N+1 queries with explicit fetch shape
impact: HIGH
impactDescription: "Reduces parent-collection iteration from N+1 SELECTs to 1"
tags:
  - persistence
  - jpa
  - n-plus-one
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-001
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: EntityGraph definition + fetch plan
    quote: o support with the @EntityGraph annotation, which lets you reference a @NamedEntityGraph definition. You can use that annotation on an entity to configure the fetch plan of the resulting query. The type ( Fetch or Load ) of the fetching can be configured by us
  - source_type: external
    citation: 'Spring Data JPA Reference — Fetching strategies (JOIN FETCH and @EntityGraph)'
    url: 'https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html'
  - source_type: external
    citation: 'Hibernate User Guide — Performance §Fetching'
    url: 'https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching'
---

## Prevent N+1 queries with explicit fetch shape

**Impact: HIGH — Reduces parent-collection iteration from N+1 SELECTs to 1**

Lazy-loaded associations issue a SELECT each time they are touched. Iterating a parent collection and reading its children naively causes 1 + N queries — invisible in development with a few rows, catastrophic in production. The remedy is to declare the fetch shape at the query layer using `JOIN FETCH` or `@EntityGraph`, so the database returns the full graph in a single round trip.

**Incorrect — implicit lazy iteration triggers N+1:**

```java
var parents = parentRepo.findAll();
parents.forEach(p -> p.getChildren().size()); // each access fires a SELECT
```

**Correct — fetch shape declared at the query:**

```java
public interface ParentRepository extends JpaRepository<Parent, Long> {
    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children")
    List<Parent> findAllWithChildren();
}
```

Verification: `./gradlew testPractices --tests "*NPlusOne*"` exercises both paths and asserts `Statistics.getPrepareStatementCount()` equals 1 for the fetched path.

Reference: [Spring Data JPA — Fetching strategies](https://docs.spring.io/spring-data/jpa/reference/jpa/fetching.html)


<!-- @source rules/persistence-optimistic-locking.md -->

---
title: Add @Version to entities updated under concurrent traffic
impact: HIGH
impactDescription: "Without @Version, concurrent updates silently lose one of them (last-writer-wins)"
tags:
  - persistence
  - jpa
  - concurrency
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-004
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
evidence:
  - upstream_id: spring-jpa-fetching
    section: "Spring Data JPA — locking"
    quote: "lock"
  - source_type: external
    citation: "Hibernate User Guide — Optimistic Locking"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
---

## Add @Version to entities updated under concurrent traffic

**Impact: HIGH — Without @Version, concurrent updates silently lose one of them (last-writer-wins)**

Two transactions that both `findById(...)`, modify the same row, and persist will both succeed under the default last-writer-wins policy. One of the two updates is gone with no exception, no log line, no record. JPA's `@Version` column closes the gap: every persist increments it, and a commit that carries a stale version throws `OptimisticLockException` (Spring Data wraps it as `ObjectOptimisticLockingFailureException`). The caller can retry the operation or surface the conflict.

**Incorrect — no version column, silent lost update:**

```java
@Entity
public class Account {
    @Id @GeneratedValue Long id;
    long balance;
    // no @Version — concurrent updates race
}
```

**Correct — @Version on the entity:**

```java
@Entity
public class Account {
    @Id @GeneratedValue Long id;
    long balance;

    @Version
    long version;          // bumped by JPA on each persist
}
```

Verification: `./gradlew testPractices --tests "*OptimisticLocking*"` persists an entity, races two stale references, and asserts the loser throws `ObjectOptimisticLockingFailureException` / `OptimisticLockException`.

Reference: [Hibernate User Guide — Optimistic Locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)


<!-- @source rules/persistence-state-machine-atomic.md -->

---
title: State machine transitions must be atomic — @Version + transactional boundary + explicit transition method
impact: HIGH
impactDescription: "Concurrent transitions on the same workflow entity must produce exactly one winner; the loser gets a 409, not a corrupted state"
tags:
  - persistence
  - jpa
  - state-machine
  - concurrency
spec_ref: "specs/payment-l0.yaml#PAYMENT-STATE-002"
verification:
  gradle_task: testPayment
  tag: PAYMENT-STATE-002
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
  - "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring Framework — declarative transaction management"
    quote: "transaction"
  - source_type: external
    citation: "Hibernate User Guide — Optimistic Locking (@Version)"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
  - source_type: external
    citation: "Spring Data JPA — Locking"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
---

## State machine transitions must be atomic — @Version + transactional boundary + explicit transition method

**Impact: HIGH — Concurrent transitions on the same workflow entity must produce exactly one winner; the loser gets a 409, not a corrupted state**

Any entity with a lifecycle — `WorkItem` (QUEUED → RUNNING → DONE → FAILED), `Order` (PENDING → CONFIRMED → SHIPPED → DELIVERED), `Subscription` (TRIAL → ACTIVE → PAUSED → CANCELLED), `Payment` (CREATED → AUTHORIZED → CAPTURED → REFUNDED) — encodes a transition graph. Three things must hold simultaneously, and the bug surface for missing any of them is identical: silent corruption of the entity's state under concurrency. (1) The legal transitions must live in a single dedicated method (or a `StateMachine` companion type) that throws on illegal events — no direct field mutation of the state column anywhere else in the codebase. (2) Each transition must execute inside a transactional boundary, so the state column write and any dependent writes (audit ledger, denormalized counters, outgoing event publish) commit atomically or roll back together. (3) The entity must carry `@Version` so that two concurrent transactions racing the same entity collide on optimistic-lock check — one wins, the other surfaces as `ObjectOptimisticLockingFailureException` which the exception handler translates to HTTP 409. `persistence-optimistic-locking.md` covers the @Version primitive in isolation; this rule combines it with the dedicated transition method and the transactional boundary, which is the shape required for any workflow state machine.

**Incorrect — direct field mutation, no @Version, no transition method:**

```java
@Entity
public class WorkItem {
    @Id @GeneratedValue Long id;
    @Enumerated(EnumType.STRING) WorkState state;
    // no @Version — two concurrent transitions both succeed, last writer wins
}

@Service
public class WorkService {
    @Transactional
    public void markRunning(long id) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.setState(WorkState.RUNNING);          // direct mutation, no transition check
        repo.save(item);
    }

    @Transactional
    public void markDone(long id) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.setState(WorkState.DONE);             // can be called from QUEUED — skips RUNNING
        repo.save(item);
    }
}
```

**Correct — dedicated transition method on the entity + @Version + transactional caller:**

```java
@Entity
public class WorkItem {
    @Id @GeneratedValue Long id;

    @Enumerated(EnumType.STRING)
    private WorkState state;

    @Version
    private long version;             // optimistic lock — bumped on every persist

    public void transition(WorkEvent event) {
        WorkState next = WorkStateMachine.next(this.state, event);
        if (next == null) {
            throw new IllegalStateTransitionException(this.state, event);
        }
        this.state = next;            // single mutation site, gated by the state machine
    }

    public WorkState state() { return state; }
}

@Service
public class WorkService {
    @Transactional
    public void apply(long id, WorkEvent event) {
        WorkItem item = repo.findById(id).orElseThrow();
        item.transition(event);       // throws on illegal event
        repo.save(item);              // @Version mismatch → ObjectOptimisticLockingFailureException → 409
    }
}
```

The `WorkStateMachine.next(state, event)` pure function returns the next state or `null` for an illegal transition. The exception handler maps `IllegalStateTransitionException` to HTTP 409 with an RFC 7807 `application/problem+json` body that includes `currentState` and `attemptedEvent` extensions, so clients can react programmatically.

Verification: `./gradlew testPayment --tests "*StateMachine*"` exercises the legal-transition matrix (all defined transitions succeed; all undefined transitions throw `IllegalStateTransitionException`) and a concurrent-transition race test — two threads call `transition(CAPTURE)` on the same `AUTHORIZED` entity simultaneously; one succeeds, the other receives 409 via the optimistic-lock collision.

Reference: [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

Reference: [Hibernate User Guide — Optimistic Locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)


<!-- @source rules/pii-masked-at-dto-boundary.md -->

---
title: Raw PII (IP, User-Agent, credentials) stored on entity for forensics but masked at DTO boundary
impact: HIGH
impactDescription: "Excessive Data Exposure (OWASP API3) is leaking what was collected for forensics directly into client responses"
tags:
  - privacy
  - dto
  - data-exposure
  - audit
spec_ref: "specs/session-management-l0.yaml#SESS-INTROSPECT-002"
verification:
  gradle_task: testSessionManagement
  tag: SESS-INTROSPECT-002
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - "https://gdpr-info.eu/art-25-gdpr/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization (replaces 2019's Excessive Data Exposure)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
    quote: "Lack of or improper authorization validation at the object property level. This leads to information exposure or manipulation by unauthorized parties."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "GDPR Article 25 — Data protection by design and by default"
    url: "https://gdpr-info.eu/art-25-gdpr/"
    quote: "The controller shall, both at the time of the determination of the means for processing and at the time of the processing itself, implement appropriate technical and organisational measures, such as pseudonymisation."
    quoted_at: "2026-05-22"
---

## Raw PII stored on entity for forensics but masked at DTO boundary

**Impact: HIGH — OWASP API3 Excessive Data Exposure is leaking what forensics needed**

The honest forensic posture often requires storing more than the API should ever return: the full IP, the full User-Agent string, the storage key for a file blob, the hashed API key digest. These belong on the entity row for after-the-fact investigation. They MUST NOT belong on the DTO that a client sees. The catalog convention: the raw column carries `@JsonIgnore` so accidental entity serialization cannot leak it; the DTO carries only a masked or summarized form (`ipAddressMasked`, `userAgentSummary`, `prefix`, never the storage key).

Examples in the catalog:
- **session-management (R33)**: `SessionRecord.ipAddress` and `SessionRecord.userAgent` are `@JsonIgnore`; `SessionResponse` carries `ipAddressMasked` (last octet → "xxx") and `userAgentSummary` ("Chrome on Windows"). The full UA never leaves the server.
- **api-key (R30)**: `ApiKey.hashedValue` is `@JsonIgnore`; `ApiKeyResponse` carries `prefix` (first 8 chars of the plaintext) for display, never the hash. Plaintext is returned exactly once at creation.
- **file-storage**: `StoredFile.storageKey` is `@JsonIgnore` (the opaque internal UUID); the DTO uses only `id` for client references.

**Incorrect — raw fields reach DTO directly:**

```java
@Entity
public class SessionRecord {
    private String ipAddress;       // 203.0.113.42 — full IP visible in any response
    private String userAgent;       // full UA string — fingerprinting vector

    // Jackson serializes both fields verbatim
}
```

A single endpoint returning the entity, or a list endpoint forgetting to map to a DTO, leaks the full PII. The 2019 OWASP "Excessive Data Exposure" entry was renamed in 2023 to "Broken Object Property Level Authorization" — the root cause is the same: per-property authorization was skipped because the developer trusted that *some* entity-level check would catch it.

**Correct — entity carries raw + @JsonIgnore, DTO carries masked:**

```java
@Entity
public class SessionRecord {
    @JsonIgnore                                       // never serialized verbatim
    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @JsonIgnore
    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    @JsonIgnore public String getIpAddress() { return ipAddress; }
    @JsonIgnore public String getUserAgent() { return userAgent; }
}

public record SessionResponse(
    UUID id,
    String ipAddressMasked,       // "203.0.113.xxx" — last octet redacted
    String userAgentSummary,      // "Chrome on Windows"
    // …
) {
    public static SessionResponse from(SessionRecord s, Clock clock) {
        return new SessionResponse(
            s.getId(),
            IpAddressMasker.mask(s.getIpAddress()),
            UserAgentSummarizer.summarize(s.getUserAgent()),
            // …
        );
    }
}
```

The structural defense — `@JsonIgnore` plus a separate DTO — means a developer cannot accidentally serialize the entity directly without first being forced to acknowledge the missing fields. The masker / summarizer encapsulates the redaction policy, so consistency across endpoints is mechanical rather than reviewer-dependent.

**Apply this pattern when**: storing IP / User-Agent / device fingerprint / credential digest / opaque internal key on an entity that may also produce read-side DTOs. The forensic value justifies keeping the raw column; the privacy posture forbids returning it.

Reference: [OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)

Reference: [GDPR Article 25 — Data protection by design and by default](https://gdpr-info.eu/art-25-gdpr/)


<!-- @source rules/polymorphic-entity-ref-path-segment-guard.md -->

---
title: Polymorphic (entityType, entityId) refs MUST be path-segment guarded client-side
impact: MEDIUM
impactDescription: "encodeURIComponent masks injection on the wire but Spring re-decodes before PathVariable matching — a client guard refuses the request before it leaves the browser"
tags:
  - bola
  - defense-in-depth
  - path-injection
  - polymorphic-entity
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-VALID-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/entity-key.ts"
  pattern: "assertSafeEntityRef(entityType, entityId) rejects values containing '/', '?', '#', '\\0', '\\', or a leading '.' — called by every fetch that emits the pair as a path segment"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - "https://cwe.mitre.org/data/definitions/22.html"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
    quote: "Object level authorization is an access control mechanism that is usually implemented at the code level to validate that one user can only access objects that they should have access to."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "CWE-22 — Improper Limitation of a Pathname to a Restricted Directory ('Path Traversal')"
    url: "https://cwe.mitre.org/data/definitions/22.html"
    quote: "The product uses external input to construct a pathname that is intended to identify a file or directory that is located underneath a restricted parent directory, but the product does not properly neutralize special elements within the pathname that can cause the pathname to resolve to a location that is outside of the restricted directory."
    quoted_at: "2026-05-25"
---

## Polymorphic (entityType, entityId) refs MUST be path-segment guarded client-side

**Impact: MEDIUM — encodeURIComponent is not enough on its own**

When a catalog domain models a polymorphic relationship via an `(entityType, entityId)` pair (favorites-bookmarks, tag-categorization attachments, comment-thread, activity-feed objects), those two strings often end up encoded as path segments in REST URLs:

```
DELETE /api/favorites/{entityType}/{entityId}
GET    /api/comments/by-entity/{entityType}/{entityId}
GET    /api/tags/by-entity/{entityType}/{entityId}
```

`encodeURIComponent(entityType)` correctly percent-encodes `/`, `?`, `#`, and other path-separators on the wire. But Spring MVC (and most web frameworks) decode the URL-encoded path **before** `PathVariable` matching — that's the whole point of percent-encoding. A `entityType` of `'../admin/users'` arrives at the controller as the raw string `../admin/users`. If the controller's spec yaml only enforces `@Size(max = 64)` without a charset constraint (which is the default in the catalog as shipped), the backend has no first-line defense.

The cleanest defense-in-depth pattern is to validate the pair client-side **before** the fetch leaves the browser. The validator is a one-shot helper that throws on any character likely to confuse path resolution:

- `/` (forward slash) — direct path-segment break
- `?` (query) — bumps the value into the query string
- `#` (fragment) — strips the value at the URL parser
- `\0` (NUL) — historic terminator-truncation hazard
- `\` (backslash) — Windows-style separator some frameworks treat as `/`
- leading `.` — combined with `.` makes `..`, the traversal prefix

This is *purely defense-in-depth*. The backend SHOULD constrain charset on the spec yaml field via a regex pattern (`@Pattern(regexp = "[a-zA-Z0-9_-]+")`), and the catalog tracks that as a deferred backend-contract item. But the client guard is free to ship today and closes the attack surface from the only side of the contract a fork-receiver controls.

**Incorrect — only encodeURIComponent, no client-side charset guard:**

```ts
async function removeFavorite(entityType: string, entityId: string) {
  // ❌ encodeURIComponent encodes the path-injection characters on the wire,
  // but Spring decodes them before @PathVariable matching.
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  if (!res.ok) throw new Error('Failed to remove favorite')
}
```

**Correct — client-side assertSafeEntityRef gates the fetch:**

```ts
// app/entity-key.ts
export function assertSafeEntityRef(entityType: string, entityId: string): void {
  for (const [name, value, max] of [
    ['entityType', entityType, 64],
    ['entityId', entityId, 255],
  ] as const) {
    if (!value || value.length === 0) throw new Error(`Invalid ${name}: empty`)
    if (value.length > max) throw new Error(`Invalid ${name}: longer than ${max} characters`)
    if (/[\\/?#\0]/.test(value)) throw new Error(`Invalid ${name}: contains forbidden characters`)
    if (value.startsWith('.')) throw new Error(`Invalid ${name}: cannot start with '.'`)
  }
}

// fetch site:
async function removeFavorite(entityType: string, entityId: string) {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  if (!res.ok) throw await parseError(res, 'Failed to remove favorite')
}
```

The guard belongs in a shared module (one per catalog domain or one shared across the polymorphic-entity-using L4 set) so a fork-receiver replacing a single fetch helper inherits the validation by import, not by copy-paste.

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [CWE-22 — Path Traversal](https://cwe.mitre.org/data/definitions/22.html)


<!-- @source rules/prefer-recipe-composition-over-l4-cross-import.md -->

---
title: "When a business domain matches a Business Pattern Recipe, cross-L4 wiring must follow the Recipe composition contract; ad-hoc multi-L4 cross-imports without applied_recipe declaration are prohibited"
rule_id: prefer-recipe-composition-over-l4-cross-import
impact: HIGH
impactDescription: "Ad-hoc cross-L4 imports that duplicate a Recipe's composition contract create undeclared coupling between domains, make the recipe audit trail invisible to tooling, and produce two incompatible wiring paths for the same business pattern"
tags:
  - architecture
  - recipe-composition
  - l4-layer
  - domain-isolation
  - composition-kit
provenance_class: internal_design
protects_template_id: recipes/*/RECIPE.md
failing_fixture_path: practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-001"
verification:
  type: review
  notes: |
    ArchUnit: detect Spring services that import from 2+ L4 domain packages
    (ax.template.billing + ax.template.notification + ax.template.featureflags, etc.)
    when the owning L4 domain README lacks applied_recipe: field.
    Acceptable: single-hop cross-L4 for shared utilities.
    Violation: multi-domain composition without recipe declaration.
evidence:
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture: composition patterns prevent ad-hoc coupling by making dependencies explicit through a shared composition contract"
    url: "https://martinfowler.com/eaaCatalog/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Spring Modulith reference — modules communicate via published events or explicit API types; direct package imports between modules create structural coupling that Spring Modulith enforces at test time"
    url: "https://docs.spring.io/spring-modulith/reference/fundamentals.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "토스 기술 블로그 — 도메인 모듈 설계: 도메인 간 직접 의존 대신 이벤트 또는 명시적 조합 계약을 통해 결합도를 낮춥니다"
    url: "https://toss.tech/article/slash21-backend"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## prefer-recipe-composition-over-l4-cross-import (Java)

**Impact: HIGH — When a business domain matches a shipped Recipe Pattern (saas-subscription, e-commerce, crm), the cross-L4 wiring must declare its recipe via `applied_recipe:`. Ad-hoc multi-L4 imports without this declaration create invisible coupling and duplicate the recipe composition out-of-band.**

The ax-template composition kit ships Business Pattern Recipes that define *how* multiple L4 domains compose into a coherent business feature. When a Spring service ad-hoc imports from billing, notification, and feature-flags all at once — without `applied_recipe:` in the domain README — it implements a recipe-equivalent pattern off-catalog. This defeats the guard chain and breaks the composition audit trail.

**Incorrect — multi-L4 composition without recipe declaration:**

```java
// VIOLATION: SaasSubscriptionOrchestrator wires billing + feature-flags + notification
// without applied_recipe: in the L4 domain README → ad-hoc recipe duplicate
package ax.template.saas;

import ax.template.billing.SubscriptionService;        // ← L4/billing cross-import
import ax.template.featureflags.FeatureFlagEvaluator;  // ← L4/feature-flags cross-import
import ax.template.notification.NotificationService;   // ← L4/notification cross-import

import org.springframework.stereotype.Service;

/**
 * WRONG: Manually implements saas-subscription composition without
 * declaring applied_recipe: saas-subscription in the domain README.
 * ArchUnit flags: 3 L4-package cross-imports without recipe metadata.
 */
@Service
class SaasSubscriptionOrchestrator {

    private final SubscriptionService subscriptions;
    private final FeatureFlagEvaluator flags;
    private final NotificationService notifications;

    SaasSubscriptionOrchestrator(
            SubscriptionService subscriptions,
            FeatureFlagEvaluator flags,
            NotificationService notifications) {
        this.subscriptions = subscriptions;
        this.flags = flags;
        this.notifications = notifications;
    }

    void onPlanUpgrade(String tenantId, String newPlan) {
        // ad-hoc wiring of billing → feature-flags → notification
        // duplicates saas-subscription RECIPE.md composition contract
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}
```

**Correct — domain README declares applied_recipe; composition follows the contract:**

```java
// CORRECT: The L4 domain README declares:
//   applied_recipe: saas-subscription
// The orchestrator still wires billing + feature-flags + notification but
// the recipe metadata makes the composition explicit and guard-visible.

package ax.template.saas;

import ax.template.billing.SubscriptionService;
import ax.template.featureflags.FeatureFlagEvaluator;
import ax.template.notification.NotificationService;

import org.springframework.stereotype.Service;

/**
 * CORRECT: domain README carries applied_recipe: saas-subscription.
 * recipe_governance_guard.sh validates this wiring matches RECIPE.md.
 */
@Service
class SaasSubscriptionOrchestrator {
    // same wiring — the declaration makes it compliant
    void onPlanUpgrade(String tenantId, String newPlan) {
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}
```

### Detection

ArchUnit: `noClasses().that().resideInAPackage("..saas..")` imports 2+ distinct L4 packages AND corresponding README lacks `applied_recipe:` field.

## Failing fixture

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/SaasOrchestrator.java` — three L4 cross-imports without recipe declaration. Guard must flag.

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/pass/SaasOrchestrator.java` — same imports with `applied_recipe: saas-subscription` in companion README.md.

Reference: https://martinfowler.com/eaaCatalog/


<!-- @source rules/presigned-url-signature-required.md -->

---
title: "File-storage presigned URLs must include an HMAC server signature before returning to callers"
rule_id: presigned-url-signature-required
impact: HIGH
impactDescription: "An unsigned presigned URL can be constructed by anyone who knows the storage key, bypassing all authorization checks in the application layer"
tags:
  - file-storage
  - security
  - presigned-url
  - hmac
  - authorization
provenance_class: internal_design
protects_template_id: templates/backend/file-storage/PresignedUrlService.java
failing_fixture_path: practices/evals/fixtures/presigned-url-signature-required/fail_no_signature/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-001"
verification:
  type: review
  notes: "Every PresignedUrlService.generateDownloadUrl / generateUploadUrl must compute HMAC over (objectKey + expiry) and append sig + exp query parameters."
evidence:
  - source_type: external
    citation: "AWS S3 Developer Guide — Presigned URLs: if a request is made by using the temporary security credentials of an IAM role, the presigned URL expires when the credentials used to sign the URL expire"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP Cheat Sheet — Insecure Direct Object References (IDOR): all resource access must verify authorization at the application layer, not just at the storage layer"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "NIST SP 800-186 §3.1 — HMAC-based URL authentication as integrity and authenticity check for temporary access tokens"
    url: "https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-186.pdf"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## File-storage presigned URLs must include an HMAC server signature before returning to callers

**Impact: HIGH — A presigned URL without an HMAC envelope can be constructed by any caller who observes or guesses the storage key. The S3 presigned URL alone only proves the caller knew the AWS credentials at signing time — it does not prove the application authorised the specific user.**

S3 presigned URLs embed AWS credentials and expire after a configured duration. However, they bypass the application's own authorization layer: a caller who obtains the `objectKey` can construct a functionally equivalent presigned URL themselves by re-signing with the same AWS credentials (if they are leaked) or by extending the expiry. Adding an HMAC signature over `(objectKey + expiry)` with an application-controlled secret provides a server-side authenticity check that the download endpoint can verify before proxying or redirecting.

**Incorrect — presigned URL returned directly without HMAC:**

```java
@Service
public class PresignedUrlService {

    public String generateDownloadUrl(String objectKey) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(BUCKET).key(objectKey).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(get).build();
        // VIOLATION: raw S3 URL returned — no HMAC envelope
        return presigner.presignGetObject(req).url().toString();
    }
}
```

**Correct — HMAC signature appended as query parameters:**

```java
@Service
public class PresignedUrlService {

    private final byte[] hmacSecret;

    public String generateDownloadUrl(String objectKey) throws Exception {
        long expires = Instant.now().plusSeconds(900).getEpochSecond();
        String payload = objectKey + ":" + expires;
        String sig = hmacSign(payload);                    // HmacSHA256(key, payload)

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(BUCKET).key(objectKey).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(900))
                .getObjectRequest(get).build();
        String s3Url = presigner.presignGetObject(req).url().toString();

        // CORRECT: ?sig=<hmac>&exp=<epoch> allows server-side verification
        return s3Url + "&sig=" + sig + "&exp=" + expires;
    }

    private String hmacSign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
        return Base64.getUrlEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
```

## Why this matters

Without the HMAC envelope, the download flow has no application-layer authorization gate: the S3 presigned URL is sufficient to retrieve the object. If the `objectKey` is guessable (sequential IDs, predictable patterns), an attacker can download arbitrary files. With HMAC:

1. The download endpoint verifies `sig = HMAC(objectKey + exp)` before proxying.
2. If the signature is invalid or expired, the request is rejected with 403 before touching S3.
3. The HMAC secret is application-controlled — rotating it invalidates all outstanding URLs.

This pattern applies to both download (GET presigned) and upload (PUT presigned) URLs.

## Failing fixture

See: `practices/evals/fixtures/presigned-url-signature-required/fail_no_signature/PresignedUrlService.java` — `generateDownloadUrl` returns the raw S3 presigned URL without HMAC. No `sig` or `exp` parameters in the returned URL.

Reference: [AWS S3 Developer Guide — Using presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)

Reference: [OWASP — Insecure Direct Object Reference Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html)


<!-- @source rules/promote-on-third-use.md -->

---
title: Catalog utilities MUST be promoted to a shared package on the third adoption — or carry an explicit deferral with expiry
impact: MEDIUM
impactDescription: "Inline-duplicated utilities drift across adopters. Each fix to the canonical version must be hand-mirrored N times, and divergence is a constant audit liability. The third adoption is the cheapest moment to lift; later lifts touch more call sites and accumulate more drift."
tags:
  - catalog-meta
  - shared-utility
  - rule-of-three
  - dry
  - refactor-discipline
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-001"
verification:
  source: "backend/src/main/java/com/ax/template/authblueprint/common/AuditPiiHelper.java"
  pattern: "AuditPiiHelper was inline EmailPiiHelper until 7 modules adopted it; R67 lifted it to common/ once the rule-of-three threshold was satisfied. Inline-duplicate copies are gone; one canonical version, every adopter imports it."
upstream:
  - "https://www.oreilly.com/library/view/the-pragmatic-programmer/020161622X/"
  - "https://abseil.io/resources/swe-book"
evidence:
  - source_type: external
    citation: "Hunt & Thomas — The Pragmatic Programmer (2nd ed) §7 The Evils of Duplication: 'Every piece of knowledge must have a single, unambiguous, authoritative representation within a system.' The rule of three (extract on the third copy) is the practical operationalization."
    url: "https://www.oreilly.com/library/view/the-pragmatic-programmer/020161622X/"
    quote: "Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "Software Engineering at Google — Chapter 9 Code Review: review velocity drops when duplicate logic needs synchronized changes across multiple modules; consolidate when the parallel-edit cost crosses the maintenance threshold."
    url: "https://abseil.io/resources/swe-book"
    quote: "Code that is duplicated tends to drift; the cost of keeping copies in sync compounds with each call site that needs the same fix applied."
    quoted_at: "2026-05-26"
---

## Catalog utilities MUST be promoted to a shared package on the third adoption — or carry an explicit deferral with expiry

**Impact: MEDIUM — DRY drift is a slow leak that compounds across the catalog**

The catalog is composed of many domain modules that occasionally need the
same utility helper — a PII hasher, a JSON ProblemDetail parser, a string
sanitizer, a cron expression validator. The first two modules to need a
given helper typically duplicate it inline because the cost of lifting
(new package, new test home, import surface) is higher than the cost of
the second copy. **By the third adoption that calculus inverts**: the
helper has proven its general-purpose shape (three independent modules
converged on the same interface), and each further inline copy starts
accumulating its own divergence (the email-outbox copy gets a PII regex
update; the activity-feed copy stays on the old version; ops can no
longer reason about which audit log has the latest scrubber).

R67 in the ax-template session lifted `EmailPiiHelper` →
`com.ax.template.authblueprint.common.AuditPiiHelper` after seven backend
modules had adopted it (the threshold was crossed at module 3 — R63's
multi-module sweep — but the lift itself was deferred two commits). The
threshold-then-defer pattern is fine **only when the deferral is
explicit**, with a recorded expiry. Silent deferral is just permanent
duplication.

**Incorrect — third adopter copies the helper inline without lifting:**

```java
// Module C — third adopter
// Copies the same recipientHash() function inline because "we already
// have two copies, what's one more?" — the helper is now in three
// packages with no canonical source of truth. The next regex fix
// applies to one copy; the others drift silently.
public final class ModuleCPiiHelper {
    public static String recipientHash(String email) {
        if (email == null || email.isBlank()) return "(none)";
        // ... duplicated SHA-256 truncate logic ...
    }
}
```

**Correct — third adopter triggers the lift to a shared package:**

```java
// Step 1: create the shared package (or use an existing one).
// New file: backend/src/main/java/com/ax/.../common/AuditPiiHelper.java
package com.ax.template.authblueprint.common;
public final class AuditPiiHelper {
    public static String piiHash(String value) {
        // canonical implementation lives here
    }
}

// Step 2: every existing caller (including modules A and B that had
// inline copies) imports from the new location. The inline copies
// are deleted in the SAME commit so there is no transition window
// where divergence is possible.

// Module A:
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(email) ...

// Module B:
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(userId) ...

// Module C (the third adopter — drove the lift):
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(phone) ...
```

Reference: [Hunt & Thomas — The Pragmatic Programmer (2nd ed) §7 The Evils of Duplication](https://www.oreilly.com/library/view/the-pragmatic-programmer/020161622X/)
Reference: [Software Engineering at Google — Code Review chapter](https://abseil.io/resources/swe-book)

## When to defer the lift (explicit deferral discipline)

Sometimes the third adoption arrives before the canonical shared package
exists. Lifting at that moment means choosing the package location, the
class name, the public method set — decisions worth two adopters of
context but maybe not enough at three. **Deferral is allowed**, with
three required disciplines:

1. **Record the deferral in the commit message of the third adoption.**
   "Helper X is now in three modules; deferring the lift to package
   common/Y because <reason>. Lift trigger: <fourth adoption | 2026-Q3
   refactor sprint | <named owner>>."
2. **Set a concrete expiry.** Either a date or a triggering event. "Lift
   on the fourth adoption" is the default trigger. "Lift in Q3 2026" is
   acceptable if the upstream package design is contested.
3. **Don't defer twice.** If the helper reaches module 4 without lifting,
   the lift is now overdue. The next adoption MUST do the lift; further
   deferral makes the rule meaningless.

## How to apply

When opening a PR that adds a third call site for an inline-duplicated
helper:

```text
adopter_count = git grep -l "<helperFunctionName>" -- '**/*.java' '**/*.ts' | wc -l

if adopter_count >= 3:
  if shared package exists:
    REQUIRE: same commit moves all inline copies to imports;
             delete the inline duplicates
  else:
    REQUIRE: same commit creates the shared package, lifts all copies
    OR: explicit deferral in commit message with expiry trigger
```

## Anti-patterns

- "We'll lift it later when we have time" — there is no future time when
  this is cheaper; defer with concrete expiry or do it now.
- "Copy is fine; the inline version is small" — small inline copies are
  the worst because they look harmless until a security fix needs to be
  applied to all of them. The PII regex deny-list in
  `templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError` was
  exactly this case until R63 lifted.
- "Different packages, different concerns, the duplication is intentional"
  — sometimes true (e.g. a logger named for the domain), but the helper
  under review has zero domain coupling. Domain-coupled helpers stay in
  their domain package; domain-neutral helpers lift to common.
- "Lift means a breaking change for fork-receivers" — ax-template is a
  source-of-truth catalog, not a published library. Fork-receivers
  receive the post-lift state; no semver to honor.


<!-- @source rules/quality-no-system-streams.md -->

---
title: Production code must not write to System.out / System.err
impact: MEDIUM
impactDescription: "Standard streams bypass the logger — no MDC, no structured fields, no appender routing"
tags:
  - quality
  - logging
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-003
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit — GeneralCodingRules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit pre-canned rule — NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS"
    url: "https://www.archunit.org/userguide/html/000_Index.html#_some_general_coding_rules"
---

## Production code must not write to System.out / System.err

**Impact: MEDIUM — Standard streams bypass the logger — no MDC, no structured fields, no appender routing**

`System.out.println(...)` writes to stdout directly. That bypass means:
- the line carries no `MDC.trace_id` (PRACTICES-OBS-002),
- the line is not a structured event (PRACTICES-OBS-001 — no key-value pairs),
- the line is not routed through the configured appender (no JSON shape, no log-server fan-out),
- and worse, secret values that *would* have been redacted by PRACTICES-OBS-003's `PiiRedactor` are emitted raw.

`System.out` is a debugging crutch that survives into production. The mechanical remedy is an ArchUnit rule that flags every access. New code uses an SLF4J `Logger`; old code gets migrated.

**Incorrect — debug print via standard streams:**

```java
@Service
public class TokenIssuer {
    public String issue(String email) {
        String t = mintToken();
        System.out.println("[AUTH-TOKEN] type=ISSUE email=" + email + " token=" + t);
        return t;
    }
}
```

**Correct — structured logger:**

```java
@Service
public class TokenIssuer {
    private static final Logger log = LoggerFactory.getLogger(TokenIssuer.class);

    public String issue(String email) {
        String t = mintToken();
        log.atInfo()
           .addKeyValue("event", "auth-token-issued")
           .addKeyValue("email", PiiRedactor.redact(email))
           .log();
        return t;
    }
}
```

Verification: `./gradlew testPractices --tests "*NoSystemStreams*"` runs ArchUnit's pre-canned `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS` rule over the practices/ subtree.

Reference: [ArchUnit — GeneralCodingRules](https://www.archunit.org/userguide/html/000_Index.html#_some_general_coding_rules)


<!-- @source rules/quality-optional-only-as-return.md -->

---
title: Optional is a return type — never a field, never a parameter
impact: MEDIUM
impactDescription: "Optional as a field adds allocation, defeats serialization, and is rarely meaningful"
tags:
  - quality
  - optional
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-001
upstream:
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html"
evidence:
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 55: Return optionals judiciously"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
  - source_type: external
    citation: "Stuart Marks (Oracle) — Optional class Javadoc API note"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html"
---

## Optional is a return type — never a field, never a parameter

**Impact: MEDIUM — Optional as a field adds allocation, defeats serialization, and is rarely meaningful**

`Optional<T>` was designed to communicate "may be absent" at the API boundary — the return type of a finder, an aggregate, a parser. As a field it produces an extra wrapper allocation per instance, defeats serialization (it is not `Serializable`), and is rarely more expressive than a nullable field. As a parameter it forces the caller to wrap a value it already has — the caller cannot pass `null`, but it cannot pass the value either. Effective Java Item 55 codifies the restriction: return type only.

**Incorrect — Optional as a field:**

```java
public class Order {
    private Optional<Discount> discount;          // extra allocation per Order; serialization broken
    public Optional<Discount> getDiscount() { return discount; }
}
```

**Correct — nullable field, Optional only at the boundary:**

```java
public class Order {
    private Discount discount;                    // may be null
    public Optional<Discount> getDiscount() {
        return Optional.ofNullable(discount);     // Optional appears at the return boundary only
    }
}
```

Verification: `./gradlew testPractices --tests "*OptionalNotAsField*"` runs an ArchUnit rule that rejects any field with raw type `Optional` in the practices/ subtree.

Reference: Effective Java Item 55 · [Optional Javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)


<!-- @source rules/quality-utility-class-shape.md -->

---
title: Utility classes must be final + private no-arg constructor
impact: LOW
impactDescription: "Without it the class is subclassable (instance state creeps in) and instantiable (silent no-op)"
tags:
  - quality
  - utility-class
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-QUALITY-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-QUALITY-002
upstream:
  - "https://www.oreilly.com/library/view/effective-java/9780134686097/"
evidence:
  - source_type: external
    citation: "Effective Java (Bloch, 3rd ed.) — Item 4: Enforce noninstantiability with a private constructor"
    url: "https://www.oreilly.com/library/view/effective-java/9780134686097/"
  - source_type: external
    citation: "JLS §8.8.10 — Preventing Instantiation of a Class"
    url: "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html"
---

## Utility classes must be final + private no-arg constructor

**Impact: LOW — Without it the class is subclassable (instance state creeps in) and instantiable (silent no-op)**

A "utility class" — one whose every member is `static` — exists only as a namespace for related functions. Without explicit constraints the JVM gives it a default public constructor and lets anyone subclass it. Both are silent: a `new PiiRedactor()` is a no-op that suggests the class has state, and `class HardenedRedactor extends PiiRedactor { String key = ...; }` adds the state the original class deliberately omitted. Effective Java Item 4: declare the class `final` and give it a single private no-arg constructor.

**Incorrect — implicit public constructor + subclassable:**

```java
public class PiiRedactor {                       // not final, no constructor declared
    public static String redact(String s) { ... }
    // implicit `public PiiRedactor() {}` — anyone can instantiate or subclass
}
```

**Correct — final class with private constructor:**

```java
public final class PiiRedactor {
    private PiiRedactor() { /* utility */ }      // explicitly uninstantiable
    public static String redact(String s) { ... }
}
```

Verification: `./gradlew testPractices --tests "*UtilityClassShape*"` reflects on `PiiRedactor` and asserts the class is final, has exactly one declared constructor, that constructor takes no arguments, and is private.

Reference: Effective Java Item 4 · [JLS §8.8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html)


<!-- @source rules/rbac-stub-default-fail-closed.md -->

---
title: RBAC role stub MUST default to least-privilege role — never 'admin' in dev
impact: HIGH
impactDescription: "A stub returning 'admin' by default exposes admin UI on every staging/preview deployment where the fork-receiver forgot to wire the real role source"
tags:
  - rbac
  - authz
  - bfla
  - least-privilege
  - dev-stub
spec_ref: "specs/tag-categorization-l0.yaml#TAG-AUTHZ-001"
verification:
  source: "templates/L4/tag-categorization/app/use-caller-id.ts"
  pattern: "useCallerRole() returns 'user' in dev by default; admin path requires explicit `NEXT_PUBLIC_DEV_AS_ADMIN=1` env opt-in"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
    quote: "Authorization checks for a function or resource are usually managed via configuration, and sometimes at the code level. Implementing proper checks can be a confusing task, since modern applications can contain many types of roles or groups and complex user hierarchy (e.g., sub-users, users with more than one role)."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V4 — Access Control (least privilege principle)"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the principle of least privilege exists — users should only be able to access functions, data files, URLs, controllers, services, and other resources, for which they possess specific authorization."
    quoted_at: "2026-05-25"
---

## RBAC role stub MUST default to least-privilege role — never 'admin' in dev

**Impact: HIGH — fail-OPEN defaults travel further than fork-receivers realize**

When a catalog template ships a stub for the calling user's role (or any other authorization claim), the dev default decides what every fork-receiver sees on day one. If that default is `'admin'`, every staging deployment, every Vercel preview, every QA environment, every demo to a stakeholder presents admin UI to whoever is viewing — including users who *should* be locked out. The server's `@PreAuthorize` / RBAC gate eventually rejects the request, but the UI has already lied about availability.

The principle of least privilege says: when in doubt, default to the most restricted role and require explicit opt-in to widen. The dev stub follows the same rule. Default to `'user'` (or whatever your least-privileged role is). Provide an explicit env-var opt-in (e.g. `NEXT_PUBLIC_DEV_AS_ADMIN=1`) that catalog devs flip when they need to exercise the admin path. Emit a one-shot `console.warn` on first call so the stub is visible to a fork-receiver inspecting their devtools.

The production hard-stop is a separate rule (a stub that ships to production should `throw new Error('Identity provider not configured')` so a missed integration cannot ship silently). The least-privilege default protects everything *between* dev and production — staging, preview, QA — where the stub is still active and a wrong default exposes admin UI to non-admin viewers.

**Incorrect — admin by default; staging deploys with stub still wired silently expose admin UI:**

```ts
export function useCallerRole(): 'admin' | 'user' {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('Identity provider not configured')
  }
  return 'admin'                       // ❌ Every preview / staging / QA env shows admin UI to everyone
}
```

**Correct — user by default; admin requires explicit env opt-in; one-shot dev warning:**

```ts
let warnedCallerRole = false

export function useCallerRole(): 'admin' | 'user' {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('useCallerRole: Identity provider not configured')
  }
  if (!warnedCallerRole) {
    warnedCallerRole = true
    console.warn(
      '[ax-template] useCallerRole stub active. Wire your real RBAC source. ' +
        'Set NEXT_PUBLIC_DEV_AS_ADMIN=1 to exercise the admin path locally.',
    )
  }
  const devAsAdmin = process.env.NEXT_PUBLIC_DEV_AS_ADMIN === '1'
  return devAsAdmin ? 'admin' : 'user'      // ✅ Least privilege by default
}
```

This applies symmetrically to **any** authorization-related stub a catalog template ships: role, permissions array, feature-flag boolean, tenant id, team membership. A stub that returns "yes" by default is the wrong default. Return "no" by default; require explicit dev opt-in.

Reference: [OWASP API Security Top 10 (2023) — API5:2023 BFLA](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)

Reference: [OWASP ASVS V4 — Access Control Design](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/recipe-invariants-must-resolve.md -->

---
title: "Every business_invariants entry in a recipe spec YAML must carry spec_ref: or rule_ref: pointing to an existing artifact; unresolvable references are prohibited"
rule_id: recipe-invariants-must-resolve
impact: CRITICAL
impactDescription: "A business invariant with an unresolvable spec_ref or rule_ref cannot be enforced — it is a claim with no evidence chain. Unresolvable references silently degrade the recipe from an enforceable contract to advisory prose, defeating the composition kit's binary-verification guarantee"
tags:
  - recipe-composition
  - invariants
  - referential-integrity
  - evidence-chain
  - spec-trio
provenance_class: internal_design
protects_template_id: specs/recipes/*.yaml
failing_fixture_path: practices/evals/fixtures/recipe-invariants-must-resolve/fail_unresolvable_spec_ref/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-003"
verification:
  type: script
  notes: |
    recipe_governance_guard.sh (SP37) and recipe_spec_referential_integrity_guard.sh (SP35)
    both walk specs/recipes/*.yaml business_invariants list.
    For each entry:
      - spec_ref: → resolve specs/<file>.yaml existence + optional #anchor check
      - rule_ref: → resolve practices/rules/<file>.md existence
    Missing field OR non-existent artifact → VIOLATION, exit 1.
    Zero-invariants is a WARN not a FAIL (recipe may be L2-only).
evidence:
  - source_type: external
    citation: "OWASP ASVS — every security requirement must reference a testable control; untestable requirements provide false assurance and cannot be verified in a security audit"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "arc42 — Architecture Decisions: requirements must be traceable to their sources; orphaned requirements cannot be prioritized, evolved, or removed safely"
    url: "https://arc42.org/overview/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "토스 기술 블로그 — 요구사항 추적성: 비즈니스 불변식은 반드시 검증 가능한 스펙이나 룰에 연결되어야 합니다. 연결되지 않은 불변식은 사문화됩니다"
    url: "https://toss.tech/article/requirements-traceability"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## recipe-invariants-must-resolve

**Impact: CRITICAL — Every `business_invariants` entry in a recipe spec YAML must resolve to a real artifact. An invariant with a dangling `spec_ref:` or `rule_ref:` cannot be enforced by any guard, making the recipe's contract unverifiable.**

The composition kit's binary-verification guarantee requires that every business invariant in a recipe traces to either:
- **`spec_ref:`** — an item in an existing `specs/*.yaml` file (e.g., `specs/billing-l0.yaml#BILLING-IDEMP-001`)
- **`rule_ref:`** — an existing rule file in `practices/rules/*.md` (e.g., `practices/rules/billing-event-idempotent.md`)

If the referenced file does not exist, `recipe_governance_guard.sh` exits 1 and blocks merge.

**Incorrect — recipe YAML with unresolvable spec_ref:**

```yaml
# VIOLATION: specs/recipes/saas-subscription-recipe-l0.yaml
business_invariants:
  - id: SAAS-INV-001
    description: "subscription must have ≥1 active plan"
    # VIOLATION: specs/nonexistent-l0.yaml does not exist
    spec_ref: "specs/nonexistent-l0.yaml#NONEXISTENT-001"

  - id: SAAS-INV-002
    description: "usage metering resets on billing cycle boundary"
    # VIOLATION: rule_ref points to non-existent rule file
    rule_ref: "practices/rules/billing-cycle-reset-nonexistent.md"
```

### Failing — business_invariant with neither spec_ref nor rule_ref

```yaml
business_invariants:
  - id: SAAS-INV-003
    description: "feature-gate enforcement matches plan tier"
    # VIOLATION: no spec_ref and no rule_ref — unenforceable invariant
    rationale: "Manually verified during code review"
```

**Correct — all business_invariants resolve to existing artifacts:**

```yaml
# CORRECT: specs/recipes/saas-subscription-recipe-l0.yaml
business_invariants:
  - id: SAAS-INV-001
    description: "subscription must have ≥1 active plan"
    # EXISTS: specs/billing-l0.yaml is a real file on disk
    spec_ref: "specs/billing-l0.yaml#BILLING-AUTHZ-002"

  - id: SAAS-INV-002
    description: "usage metering resets on billing cycle boundary"
    # EXISTS: practices/rules/billing-event-idempotent.md is a real file on disk
    rule_ref: "practices/rules/billing-event-idempotent.md"

  - id: SAAS-INV-003
    description: "feature-gate enforcement matches plan tier"
    # EXISTS: specs/feature-flags-l0.yaml is a real file on disk
    spec_ref: "specs/feature-flags-l0.yaml"
```

### Resolution rules

| Field | Required format | Guard check |
|---|---|---|
| `spec_ref:` | `specs/<file>.yaml` or `specs/<file>.yaml#ANCHOR` | File must exist; anchor is informational |
| `rule_ref:` | `practices/rules/<file>.md` | File must exist |
| Neither | — | VIOLATION — at least one must be present |

## Failing fixture

See: `practices/evals/fixtures/recipe-invariants-must-resolve/fail_unresolvable_spec_ref/recipe.yaml` — `business_invariants` entries reference `specs/nonexistent-l0.yaml` which does not exist.

See: `practices/evals/fixtures/recipe-invariants-must-resolve/pass/recipe.yaml` — all `business_invariants` reference `specs/billing-l0.yaml` which exists.

Reference: https://owasp.org/www-project-application-security-verification-standard/


<!-- @source rules/secret-shown-once-uses-beforeunload-guard.md -->

---
title: One-time-revealed plaintext secrets MUST wire beforeunload guard for the duration of the reveal panel
impact: HIGH
impactDescription: "Plaintext secrets shown once (api-key, webhook signing secret) live only in component state — a stray reload / tab close / route navigation destroys them with no server-side recovery path"
tags:
  - secret
  - one-time-reveal
  - beforeunload
  - credential-lifecycle
  - api-key
  - webhook
spec_ref: "specs/api-key-l0.yaml#KEY-STORAGE-001"
verification:
  source: "templates/L4/webhook/app/(admin)/webhooks/page.tsx (SecretRevealPanel), templates/L4/api-key/app/(api-key)/page.tsx (catalog plaintext-shown-once flow)"
  pattern: "useEffect(() => { window.addEventListener('beforeunload', handler) ... }, []) inside the panel component that holds the secret in React state, with returnValue assignment to trigger the native prompt"
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "MDN Web Docs — Window: beforeunload event"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event"
    quote: "The beforeunload event is fired when the current window, contained document, and associated resources are about to be unloaded. ... To trigger the dialog, an event handler in the page should call the preventDefault() method on the event."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V2.10 — Service Authentication Requirements"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that passwords are stored in a form that is resistant to offline attacks. ... Passwords SHALL be salted and hashed using an approved one-way key derivation."
    quoted_at: "2026-05-25"
---

## One-time-revealed plaintext secrets MUST wire beforeunload guard for the duration of the reveal panel

**Impact: HIGH — irrecoverable secret loss is a high-friction operational hazard**

The catalog has at least two surfaces where a server-irrecoverable plaintext secret is revealed exactly once to the operator:

- **api-key** — when the admin issues a new API key, the response carries the plaintext key value. The server stores only `SHA-256(key)`. Future GETs never return the plaintext.
- **webhook** — when the admin registers a new webhook endpoint, the response carries the `signingSecret` used for HMAC-SHA256 over `<timestamp>.<body>`. The server stores only the hash. Future GETs never return it.

Similar patterns will appear in any catalog L4 that follows the "secret stored as hash" pattern — OAuth client secrets, magic-link tokens, recovery codes, signing keys.

In all of these, the plaintext lives in React component state ONLY for the duration of the reveal panel. The moment the panel unmounts (via Acknowledge click, route navigation, tab close, browser crash, or an accidental reload), the plaintext is gone with no server-side recovery. The operator must delete the endpoint and register a new one — which forces every downstream verifier to be reconfigured (multi-party coordination cost, sometimes across organization boundaries).

The catalog convention since R48 is: **wire `beforeunload` for the duration of the reveal panel**. The native browser prompt is the last line of defense against accidental reload/close. Modern browsers ignore the custom message and show a generic "Leave site? Changes you made may not be saved" — but the `returnValue` assignment is what triggers it.

**Incorrect — bare panel; reload destroys secret silently:**

```tsx
function SecretRevealPanel({ endpoint, onAcknowledge }) {
  return (
    <section>
      <input readOnly value={endpoint.signingSecret} />
      <button onClick={onAcknowledge}>I have saved the secret</button>
    </section>
  )
}
```

A pager-driven SRE hits Cmd-R out of muscle memory. The secret is gone forever. The endpoint must be deleted and recreated. Downstream Stripe/PayPal/partner verifier needs reconfiguration.

**Correct — beforeunload guard for the panel's lifetime:**

```tsx
function SecretRevealPanel({ endpoint, onAcknowledge }) {
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      // Modern browsers ignore custom messages but the returnValue assignment
      // is what triggers the native prompt.
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [])

  return (
    <section role="alert">
      <h2>Save this signing secret now — shown ONCE.</h2>
      <input readOnly value={endpoint.signingSecret} />
      <button onClick={onAcknowledge}>I have saved the secret</button>
    </section>
  )
}
```

**Pairs with three companion patterns**:
1. **Acknowledge gated on Copy** — the acknowledge button is `aria-disabled` until the operator clicks Copy at least once (defends against misclick on the acknowledge button itself, which is often visually close to Copy)
2. **Clipboard failure surfaced** — `navigator.clipboard.writeText` can fail silently in locked-down environments; the operator must see "Copy failed — select manually" rather than assume the copy succeeded
3. **Sibling create form disabled while reveal pending** — a second registration submitted while the panel is up would overwrite the revealed state with the new response, losing the first secret

`sessionStorage` / `localStorage` persistence is the WRONG fix — it creates a second leak surface (DevTools inspection, browser extension scraping, multi-user shared workstation). The `beforeunload` prompt is the right tradeoff: prevent accidental loss without creating a persistent attack surface.

**When to apply**: any frontend surface that displays a server-irrecoverable plaintext credential. The catalog's R48 webhook SecretRevealPanel is the reference implementation; the api-key L4 plaintext-shown-once flow follows the same pattern.

**When NOT to apply**: re-displayable credentials (OAuth tokens with `/refresh` endpoint, JWTs the server can re-issue, session cookies). The recovery cost is low; the beforeunload prompt becomes friction without benefit.

Reference: [MDN — Window: beforeunload event](https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event)

Reference: [OWASP ASVS V2.10 — Service Authentication Requirements](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/security-csrf-scoped-disable.md -->

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
    quote: "ignoringRequestMatchers"
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


<!-- @source rules/security-default-headers.md -->

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


<!-- @source rules/security-stateless-session-policy.md -->

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


<!-- @source rules/server-side-stored-error-sanitize.md -->

---
title: Stored error columns MUST be PII-sanitized at storage time — render-layer scrub alone is insufficient
impact: HIGH
impactDescription: "When a service persists a sender / adapter / job error string verbatim and relies only on a render-layer scrubber to redact PII, every operator with raw DB query access reads PII the catalog promised would be redacted. The render layer is a defense, not the only defense — defense-in-depth requires server-side scrub on write."
tags:
  - pii
  - storage
  - defense-in-depth
  - error-handling
  - korean-enterprise
spec_ref: "specs/email-outbox-l0.yaml#EMAIL-SEND-002"
verification:
  source: "backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailOutboxService.java"
  pattern: "row.markFailure(EmailPiiHelper.sanitizeReason(trimmed), now, ...) — sender exception scrubbed BEFORE persist, not only at render"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://cwe.mitre.org/data/definitions/532.html"
evidence:
  - source_type: external
    citation: "OWASP ASVS V8 — Data Protection"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify the application minimizes the number of parameters in a request, such as hidden fields, AJAX variables, cookies and header values."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "CWE-532 — Insertion of Sensitive Information into Log File"
    url: "https://cwe.mitre.org/data/definitions/532.html"
    quote: "Information written to log files can be of a sensitive nature and give valuable guidance to an attacker or expose sensitive user information."
    quoted_at: "2026-05-26"
---

## Stored error columns MUST be PII-sanitized at storage time — render-layer scrub alone is insufficient

**Impact: HIGH — render-layer scrub fails for direct DB query paths**

When a service persists a thrown exception message (sender adapter failure,
job runner stack trace, webhook delivery error) verbatim into a database
column intended for operator display — `email_outbox.last_error`,
`webhook_delivery.error`, `scheduled_task_history.failure_reason`, similar —
the column itself is now a PII reservoir. Provider exceptions routinely
embed:

- the recipient email ("SMTP rejected target@example.com")
- the user's 주민등록번호 ("ID 901231-1234567 not found")
- a JWT or Bearer token ("auth header Bearer eyJ...")
- an internal hostname ("connection refused from mailer.internal")
- an IPv4 ("could not reach 10.0.5.12")

The catalog already mandates a RENDER-layer scrub via R50
`stored-server-error-sanitize-at-render-layer` so the admin UI shows
`[REDACTED]` instead of the raw fragment. But the render layer is only
one of many readers. Direct DB access — an SRE running `SELECT
last_error FROM email_outbox WHERE status='DLQ'`, a backup restoring to
a forensic lab, a fork-receiver's BI pipeline reading the table — all
bypass the UI scrub. The column is the persistence boundary; defense-in-
depth requires the scrub to apply at the WRITE path, not just at the
READ path.

Mechanically: every code path that calls `setLastError(reason)` /
`markFailure(reason, ...)` / similar MUST first pass `reason` through a
PII deny-list scrubber identical to the render-layer rule's pattern set.
The catalog ships `EmailPiiHelper.sanitizeReason()` (JVM) and
`templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError`
(TypeScript) as the canonical pair. Apply both — the render layer keeps
its scrub as a second line of defense for the unlikely case that a
fork-receiver introduces a new write site that forgets the storage-time
scrub.

**Incorrect — stores sender exception verbatim; relies on UI scrubber alone:**

```java
catch (EmailSendException ex) {
    String reason = ex.getMessage();   // ❌ stored verbatim — may embed PII
    if (reason != null && reason.length() > 1000) {
        reason = reason.substring(0, 1000);  // length cap but no scrub
    }
    row.markFailure(reason, now, ...);
    // → DB column email_outbox.last_error = "SMTP rejected alice@example.com: 550 ..."
    // → SRE running `SELECT last_error FROM email_outbox` reads PII directly
}
```

**Correct — scrub at storage time, render-layer keeps its scrub as second line:**

```java
catch (EmailSendException ex) {
    String raw = ex.getMessage() == null ? "unknown error" : ex.getMessage();
    String trimmed = raw.length() > 1000 ? raw.substring(0, 1000) : raw;
    String reason = EmailPiiHelper.sanitizeReason(trimmed);  // ✅ scrub BEFORE persist
    row.markFailure(reason, now, delay -> now.plusSeconds(delay));
    // → DB column email_outbox.last_error = "SMTP rejected [REDACTED]: 550 ..."
    // → operator SELECT or admin UI both see redacted form
}
```

Reference: [OWASP ASVS V8 — Data Protection](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [CWE-532 — Insertion of Sensitive Information into Log File](https://cwe.mitre.org/data/definitions/532.html)

## How to apply

Audit every persisted error-string column with grep — `last_error`,
`error_message`, `failure_reason`, `stderr`, `stack_trace`. For each
write site, confirm the input flows through a sanitize helper that
redacts:

- KR RRN — `\d{6}-\d{7}`
- KR mobile — `01[016789]-?\d{3,4}-?\d{4}`
- JWT shape — `eyJ[A-Za-z0-9._-]{20,}`
- Bearer header value
- `sk-...` / `ghp_...` secret prefixes
- email address
- IPv4
- `*.internal`, `*.local` hostnames

The canonical scrubber lives in `EmailPiiHelper.sanitizeReason` (JVM) /
`templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError`
(TypeScript). Duplicate the deny-list per-language helper until enough
modules converge to justify a shared library.

## Anti-patterns

- "The render layer already redacts — why scrub at storage too?" — render
  layer protects UI consumers. DB queries, backups, BI exports, and
  forensic restores all bypass it.
- "Length cap is enough" — substring(0, 1000) is a size limit, not a
  content filter. A 200-char message can still embed an email.
- "We trust the sender adapter to throw clean errors" — sender adapters
  bubble up upstream library exceptions (JavaMail, AWS SES SDK, SendGrid
  client) whose error strings the catalog cannot control.


<!-- @source rules/soft-delete-audit-trail.md -->

---
title: Soft-delete via status flip preserves audit trail — hard-delete forbidden when audit matters
impact: HIGH
impactDescription: "Hard-delete on audit-grade entities loses the why/when/who; the right-to-erasure mandate is satisfied by body redaction, not row removal"
tags:
  - audit
  - soft-delete
  - gdpr
  - data-retention
spec_ref: "specs/comment-thread-l0.yaml#COMMENT-CRUD-003"
verification:
  gradle_task: testCommentThread
  tag: COMMENT-CRUD-003
upstream:
  - "https://gdpr-info.eu/art-17-gdpr/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "GDPR Article 17 — Right to erasure ('right to be forgotten')"
    url: "https://gdpr-info.eu/art-17-gdpr/"
    quote: "The data subject shall have the right to obtain from the controller the erasure of personal data concerning him or her without undue delay. ... [The controller shall] take account of available technology and the cost of implementation."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "OWASP ASVS V8.3.5 — Verify that sensitive information is sanitized or removed when no longer required"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that sensitive information is sanitized or removed when no longer required (e.g., data retention)."
    quoted_at: "2026-05-22"
---

## Soft-delete via status flip preserves audit trail — hard-delete forbidden when audit matters

**Impact: HIGH — Hard-delete on audit-grade entities loses the who/when/why**

The right-to-erasure mandate (GDPR Article 17) does NOT require row removal. It requires that personal data "concerning the data subject" be erased. The catalog soft-delete pattern satisfies this by clearing the data (body → NULL, DTO mask `[deleted]`) while preserving the audit metadata (`deletedAt`, `deletedByUserId`, the original `createdAt`, and any edit history). The personal data is gone; the act of deletion is recorded.

Hard-delete loses what compliance needs (who deleted what when) and what threading needs (a reply's parent still must resolve). Audit-grade entities — comments, sessions, file uploads, approval requests, payment events, audit logs themselves — should never hard-delete in their domain code. The catalog pattern (R33 session-management, R36 comment-thread) uses status flip + content nulling.

Catalog evidence:
- **R33 session-management (SESS-LIFECYCLE-003)**: logout flips `status` ACTIVE → REVOKED, sets `revokedAt`, stamps `revokedByUserId`. The row stays for historical session audit.
- **R36 comment-thread (COMMENT-CRUD-003)**: delete flips `status` ACTIVE → DELETED, clears `body` to NULL, stamps `deletedAt` + `deletedByUserId`. The DTO masks the missing body as `'[deleted]'`. Replies remain readable; thread structure is preserved.

**Incorrect — hard-delete loses audit metadata:**

```java
@DeleteMapping("/api/comments/{id}")
public ResponseEntity<Void> delete(@PathVariable UUID id) {
    repo.deleteById(id);                              // row gone forever
    return ResponseEntity.noContent().build();
}
```

Replies to this comment now reference a missing parent. The audit log cannot answer "who deleted comment X". GDPR erasure is over-satisfied — the metadata about *the act of erasure* is lost too.

**Correct — soft-delete with status flip + content clearing:**

```java
@DeleteMapping("/api/comments/{id}")
public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
    Comment c = repo.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
    // Authorization check elided — author OR admin per admin-cannot-rewrite-user-content rule
    c.softDelete(auth.getName(), Instant.now(clock));  // status flip + body→NULL + deletedAt + deletedByUserId
    repo.save(c);
    return ResponseEntity.noContent().build();
}

// Entity:
void softDelete(String actorUserId, Instant now) {
    if (this.status == CommentStatus.DELETED) return;  // idempotent
    this.status = CommentStatus.DELETED;
    this.body = null;                                  // personal data cleared
    this.deletedAt = now;
    this.deletedByUserId = actorUserId;
}

// DTO masks for read side:
public static CommentResponse from(Comment c) {
    String visibleBody = (c.getStatus() == CommentStatus.DELETED || c.getBody() == null)
        ? "[deleted]"
        : c.getBody();
    // … rest of mapping
}
```

The body is gone (GDPR erasure satisfied); the audit (deletedAt + deletedByUserId) survives; replies still resolve their parent. Edit history rows (`CommentEdit`) are preserved across the delete so a moderator can still reconstruct what the comment said and when it was edited — without the body itself.

**Apply this pattern when**: the entity participates in an audit trail or a thread / chain / graph where its absence would break adjacent rows. Apply hard-delete only for entities with no audit value (transient session caches, ephemeral computation outputs).

**Anti-cascade**: soft-delete should NOT cascade to dependent rows. Comment's replies remain ACTIVE even when the parent comment is DELETED. Session's `ActivityRead` rows remain. The user can still see *that* something happened; the body is what's gone.

Reference: [GDPR Article 17 — Right to erasure](https://gdpr-info.eu/art-17-gdpr/)

Reference: [OWASP ASVS V8.3.5 — Data retention sanitization](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/soft-delete-only-on-base-entity.md -->

---
title: "Soft-delete must be implemented via @SQLDelete on BaseEntity subclasses, never via application-level flag fields"
impact: HIGH
impactDescription: "Boolean deleted=true flags are invisible to @Where filters, produce schema drift, and allow JPA hard-deletes to silently bypass the soft-delete contract. Timestamp-based @SQLDelete + @Where guarantees every ORM DELETE becomes an UPDATE with no application code changes."
tags:
  - persistence
  - soft-delete
  - hibernate
  - base-entity
  - data-integrity
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-005"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-005
failing_fixture_path: "practices/evals/fixtures/soft_delete/fail_boolean_flag"
passing_fixture_path: "practices/evals/fixtures/soft_delete/pass"
protects_template_ids:
  - "templates/backend/BaseEntity.java"
  - "templates/backend/notification/Notification.java"
  - "templates/backend/notification/NotificationPreferences.java"
  - "templates/backend/audit-log/AuditLog.java"
  - "templates/backend/file-storage/StoredFile.java"
  - "templates/backend/email-outbox/EmailOutbox.java"
  - "templates/backend/email-outbox/EmailTemplate.java"
  - "templates/backend/scheduled-task/ScheduledTask.java"
  - "templates/backend/scheduled-task/JobHistory.java"
upstream:
  - "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
  - "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where"
evidence:
  - source_type: external
    citation: "Hibernate ORM 6.4 User Guide — @SQLDelete: Customizes the SQL DELETE statement; when set to an UPDATE, every call to EntityManager.remove() or repository deleteById() runs the UPDATE instead, enabling transparent soft-delete without application-layer interception."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
  - source_type: external
    citation: "Hibernate ORM 6.4 User Guide — @Where(clause): Adds a predicate appended to every JPQL/Criteria query for the annotated entity or collection. Use @Where(clause = 'deleted_at IS NULL') on the @MappedSuperclass so all standard queries automatically exclude soft-deleted rows."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where"
  - source_type: external
    citation: "Hibernate ORM 6.4 Release Notes — @SoftDelete introduced in 6.4 but is boolean-only (BIT/BOOLEAN column). For TIMESTAMP-based soft-delete columns (deleted_at TIMESTAMP NULL), use @SQLDelete + @Where(clause = 'deleted_at IS NULL') instead."
    url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
---

## Soft-delete must be implemented via @SQLDelete on BaseEntity subclasses

**Impact: HIGH — Boolean deleted=true flags are invisible to @Where filters, produce schema drift, and allow JPA hard-deletes to silently bypass the soft-delete contract.**

Soft-delete is a data-retention pattern: rows are never physically removed; instead, a marker signals "this row is logically gone." There are two implementation paths:

1. **Boolean flag** (`deleted BOOLEAN DEFAULT FALSE`) — application code sets `entity.setDeleted(true)` and every query must manually add `WHERE deleted = false`.
2. **Timestamp column** (`deleted_at TIMESTAMP NULL`) — Hibernate's `@SQLDelete` converts every ORM-triggered `DELETE` into `UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`, and `@Where(clause = "deleted_at IS NULL")` on the `@MappedSuperclass` excludes soft-deleted rows from all JPQL/Criteria queries automatically.

The boolean flag approach has three critical failure modes:
- **Inconsistent enforcement:** a single repository query that forgets `AND deleted = false` leaks deleted data.
- **No audit timestamp:** you cannot determine *when* a record was deleted without a separate audit column.
- **Hard-delete bypass:** `entityManager.remove()` and `repository.deleteById()` physically delete the row unless every code path is reviewed and individually guarded.

With `@SQLDelete` + `@Where`, hard-delete bypass is structurally impossible: Hibernate rewrites the DELETE at the JDBC level before it reaches the database. No application code can accidentally bypass this.

**Incorrect — boolean flag soft-delete:**

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // BAD: application must remember to filter WHERE deleted = false in every query
    @Column(nullable = false)
    private boolean deleted = false;

    // No audit trail of when deletion happened
}
```

**Correct — timestamp-based @SQLDelete + BaseEntity:**

```java
// BaseEntity (shared superclass — @MappedSuperclass):
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")      // applied to ALL subclass queries automatically
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "deleted_at")            // NULL = active, non-null = deleted
    private Instant deletedAt;
}

// Concrete entity — MUST carry @SQLDelete:
@Entity
@Table(name = "notifications")
@SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Notification extends BaseEntity {
    // domain fields only — audit + soft-delete inherited from BaseEntity
    @Column(name = "title", nullable = false, length = 255)
    private String title;
}
```

**What @SQLDelete does:**
When Spring Data calls `notificationRepository.deleteById(id)` or JPA calls `entityManager.remove(entity)`, Hibernate intercepts the DELETE and executes:
```sql
-- Intercepted by @SQLDelete — never reaches the database as DELETE:
UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?
```

**What @Where does:**
Every standard JPQL or Criteria query on `Notification` automatically has `AND deleted_at IS NULL` appended:
```sql
-- Standard findById — @Where appended automatically:
SELECT n.* FROM notifications n WHERE n.id = ? AND n.deleted_at IS NULL

-- findAll — @Where appended automatically:
SELECT n.* FROM notifications n WHERE n.deleted_at IS NULL
```

**Database migration requirement:**
All 8 entities that extend `BaseEntity` require a `deleted_at TIMESTAMP NULL` column and a partial index for query performance:
```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_not_deleted
    ON notifications (id) WHERE deleted_at IS NULL;
```

See `templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql` for the full migration covering all 8 entities.

Verification: `./gradlew testPractices --tests "*BaseEntitySoftDelete*"` asserts that every `@Entity` in the base template package that extends `BaseEntity` also carries `@SQLDelete`.

Reference: [Hibernate ORM 6.4 — @SQLDelete](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete) | [Hibernate ORM 6.4 — @Where](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#mapping-where)


<!-- @source rules/spec-domain-mode-gates-frontend-trio.md -->

---
title: Frontend full-trio MUST be gated by the spec's `domain_mode` declaration
impact: HIGH
impactDescription: "Generating a frontend full-trio for a `domain_mode: backend_only` L4 silently overrides the spec's deliberate design (e.g. server-to-server callback domains where no user-facing surface exists) and reopens fork-receiver autonomy decisions that the spec deliberately closed"
tags:
  - catalog-meta
  - spec-discipline
  - scope-discipline
  - frontend-trio
  - domain-mode
spec_ref: "specs/identity-verification-l0.yaml#L5"
verification:
  source: "specs/identity-verification-l0.yaml"
  pattern: "domain_mode: backend_only declared at the top of the spec; templates/L4/identity-verification/ intentionally does NOT exist on disk; the catalog refuses to create a frontend trio for this domain even when an AI agent or master plan asks for one"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://www.rfc-editor.org/rfc/rfc2119"
evidence:
  - source_type: external
    citation: "OWASP ASVS V4 §1.2 — Authentication Architecture: applicable to all components, modules, frameworks, platforms, and operating systems; the architecture must be documented and approved before development."
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the authentication architecture is documented and approved before development."
    quoted_at: "2026-05-26"
  - source_type: external
    citation: "RFC 2119 — Key words for use in RFCs to Indicate Requirement Levels"
    url: "https://www.rfc-editor.org/rfc/rfc2119"
    quote: "MUST. This word, or the terms REQUIRED or SHALL, mean that the definition is an absolute requirement of the specification."
    quoted_at: "2026-05-26"
---

## Frontend full-trio MUST be gated by the spec's `domain_mode` declaration

**Impact: HIGH — silent spec override exposes surfaces a spec deliberately closed**

The catalog's L4 spec files declare a `domain_mode` field at the top
(`backend_only` / `full_trio` / `frontend_only`). This field is a binding
RFC 2119 MUST: it states whether the domain has any user-facing surface at
all. Server-to-server domains (KYC callback, payment provider webhook,
OAuth token-exchange, server-driven scheduler) declare `backend_only`
because they have NO end-user UI by construction — the provider's own
SDK / web flow handles user identity collection, and the consuming app
only receives the result via signed callback.

Generating `templates/L4/<domain>/app/...` for a `backend_only` domain
silently overrides this design. The PII consequences can be severe: the
identity-verification spec stores CI / DI correlation tokens that are
intentionally NEVER user-visible (개인정보보호법 §24-1, 개인정보 보호위원회
가이드라인). An "admin verified-identities list" page would expose them in
the UI — a surface the spec closed deliberately, and that fork-receivers
who need a different exposure pattern (e.g. logging only via audit) cannot
quietly opt out of once the page exists on disk.

When an AI agent / master plan / persona asks for "L4 \<domain\> frontend
full-trio", the catalog MUST check the spec's `domain_mode` before
creating any file under `templates/L4/<domain>/app/`. If the field is
absent or set to `backend_only`, the work is re-scoped to backend
residual closure (entities, services, audits, admin endpoints). The
absence of `templates/L4/<domain>/` on disk is the spec speaking.

**Incorrect — generating a frontend trio for a `backend_only` domain:**

```text
# specs/identity-verification-l0.yaml — line 5
domain_mode: backend_only   # no frontend UI in scope; CI/DI callback is server-to-server

# AI agent or master plan asks: "create identity-verification frontend full-trio"
$ mkdir -p templates/L4/identity-verification/app/(admin)/verified-identities
$ # ❌ Creates /api/admin/identity-verification UI exposing CI/DI in an admin table
$ # ❌ Overrides the spec's explicit `backend_only` declaration
$ # ❌ Reopens the R2 closure (fork-receiver-owned admin) without spec amendment
```

**Correct — read `domain_mode` first, re-scope to backend residual closure:**

```text
$ grep '^domain_mode' specs/identity-verification-l0.yaml
domain_mode: backend_only

# domain_mode == backend_only → refuse frontend trio.
# Re-scope to backend residual closure:
$ # ✅ Add VerifiedIdentity entity + repository (IDV-CALLBACK-002 persistence)
$ # ✅ Add IdentityVerificationService with audit publish (IDV-AUDIT-001)
$ # ✅ Add IdentityVerificationAdminController @PreAuthorize ROLE_ADMIN
$ # ✅ Do NOT create templates/L4/identity-verification/
```

Reference: [OWASP ASVS V4 §1.2 — Authentication Architecture](https://owasp.org/www-project-application-security-verification-standard/)
Reference: [RFC 2119 — Key Words for Use in RFCs to Indicate Requirement Levels](https://www.rfc-editor.org/rfc/rfc2119)

## How to apply

```text
mode = read(specs/<domain>-l0.yaml#domain_mode)
if mode is null or mode == "backend_only":
  REFUSE.
  Re-scope to: <domain> backend residual closure.
elif mode == "full_trio" or mode == "frontend_only":
  Proceed.
else:
  STOP. Unknown domain_mode value — surface to user.
```

## Verification surface

Enforced mechanically by the 41st hard guard, R59 — see
[`practices/evals/l4_frontend_domain_mode_guard.sh`](../evals/l4_frontend_domain_mode_guard.sh).
The guard refuses to merge any commit where a `templates/L4/<domain>/app/`
tree exists but the matching `specs/<domain>-l0.yaml#domain_mode` is
`backend_only`, absent, or unknown. Fallback spec path
`specs/<domain>-frontend-l0.yaml` is also accepted (auth / crud).

## Anti-patterns

- "The spec is silent on `domain_mode`, so I assume `full_trio`" — NO. Absent
  is a design signal; treat as `backend_only` until the user opts in.
- "The master plan said to do it" — master plans can be wrong (R54 in
  ax-template was). The spec is the source of truth.
- "I'll just add the frontend; fork-receivers can delete it if they don't
  want it" — adding overrides the spec's design decision; deletion is a
  burden on every fork-receiver instead of zero burden if absent.


<!-- @source rules/stored-server-error-sanitize-at-render-layer.md -->

---
title: Server-supplied stored error strings MUST pass a PII / secret deny-list at the render layer
impact: HIGH
impactDescription: "errorMessage / lastError fields persisted on entities and rendered to admin views leak PII / internal hostnames / credentials via screen-share + screenshot; render-layer deny-list is defense-in-depth even when backend sanitization is the canonical fix"
tags:
  - pii
  - error-handling
  - defense-in-depth
  - admin-surface
  - screen-share-leak
spec_ref: "specs/webhook-l0.yaml#WEBHOOK-DEAD-LETTER-002"
verification:
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L4/scheduled-task/app/parse-error.ts (sanitizeStoredError helper)"
  pattern: "sanitize helper applied to any server-stored error field (lastError on Delivery, errorMessage on JobHistory) before inline render; regex deny-list includes email / Bearer / JWT / IPv4 / .internal/.local / Korean RRN / Korean mobile / PEM headers / GitHub PAT"
upstream:
  - "https://cwe.mitre.org/data/definitions/209.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "CWE-209 — Generation of Error Message Containing Sensitive Information"
    url: "https://cwe.mitre.org/data/definitions/209.html"
    quote: "The product generates an error message that includes sensitive information about its environment, users, or associated data. ... An attacker can use the additional information provided in error messages to mount attacks targeted on the specific environment or configuration."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V14.3 — Unintended Security Disclosure"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that the application does not output debug or error messages to console, logs, or HTTP responses that contain sensitive information such as session identifiers, credentials, or PII."
    quoted_at: "2026-05-25"
---

## Server-supplied stored error strings MUST pass a PII / secret deny-list at the render layer

**Impact: HIGH — incident-bridge screen-share is a regular leak vector for raw server errors**

There are two classes of server-supplied error string a frontend renders:

1. **Transient (fetch-time)** — the `error.message` returned by a failed mutation or query. Rule `error-message-not-in-native-title-attribute` (R47) covers this: errors render in `role='alert'` aria-live spans, not in native `title` tooltips, and `parse-error.ts` already has a PII deny-list at the fetch boundary.

2. **Stored (persisted on an entity)** — `lastError` on a webhook delivery row, `errorMessage` on a scheduled-task `JobHistory` row, `lastFailureReason` on a billing event, `verificationError` on a KYC attempt. These are server-side strings written into the DB at the moment a job / delivery / verification failed, then surfaced as part of the entity's DTO on every GET.

The second class is the dangerous one. Because the error is *stored*, the same bytes are read back every time an admin page renders. An SRE screen-sharing the deliveries page during an incident bridge replays the leak every time the page repaints. Slack screenshots, recorded incident calls, post-incident video reviews — all replay.

`parseError`'s deny-list covers transient errors. Stored errors need the same deny-list applied at the render boundary, as defense-in-depth even when the backend should be sanitizing on write (the catalog tracks "backend DTO sanitization" as the canonical fix per domain; this rule is the layer the frontend owns regardless).

**Incorrect — stored errorMessage rendered raw:**

```tsx
{d.lastError && (
  <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs">
    last error: <code>{d.lastError}</code>
  </div>
)}
```

A stack-trace excerpt, a backend Bearer token leaked into the message, an internal hostname (`db-prod.internal`), a Korean RRN that crept into a logging line — all appear inline. JSX escapes HTML but does NOT sanitize content patterns.

**Correct — sanitize helper applied at render:**

```ts
// app/parse-error.ts (per-domain or in fork-receiver-kit)
const STORED_ERROR_MAX = 200
export function sanitizeStoredError(raw: string | null): string {
  if (!raw) return ''
  const looksSensitive =
    /@[\w.-]+\.[A-Za-z]{2,}/.test(raw) ||
    /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN |ghp_|ghs_)/i.test(raw) ||
    /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(raw) ||
    /\.internal\b|\.local\b/.test(raw) ||
    /\d{6}-\d{7}/.test(raw) ||                    // Korean RRN
    /01[016789]-?\d{3,4}-?\d{4}/.test(raw) ||     // Korean mobile
    /eyJ[A-Za-z0-9._-]{20,}/.test(raw)             // JWT
  if (looksSensitive) return '[redacted — see server logs]'
  return raw.length <= STORED_ERROR_MAX ? raw : `${raw.slice(0, STORED_ERROR_MAX)}… [truncated]`
}
```

```tsx
{d.lastError && (
  <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs">
    last error: <code>{sanitizeStoredError(d.lastError)}</code>
  </div>
)}
```

**Deny-list locale**: the catalog ships Korean enterprise patterns (RRN `XXXXXX-XXXXXXX`, mobile `010-XXXX-XXXX` and other carrier prefixes) by default. Fork-receivers operating in other locales extend the deny-list with locale-specific PII shapes (US SSN, EU national IDs, JP MyNumber) — this is a domain-level extension point, not a one-size-fits-all global rule.

**When to apply**: any entity DTO field that carries server-side error text accessible to admin / SRE views — `lastError`, `errorMessage`, `failureReason`, `verificationError`, `auditNote`, `lastFailureDetail`. Apply at every render site of the field, not just the most-trafficked one (different pages render the same field).

**When NOT to apply**: short structured error codes (`ERR_TIMEOUT`, `RATE_LIMITED`) without free-form server prose. The deny-list's job is to catch free-form text; a structured enum is already safe.

Reference: [CWE-209 — Information Exposure Through Error Messages](https://cwe.mitre.org/data/definitions/209.html)

Reference: [OWASP ASVS V14.3 — Unintended Security Disclosure](https://owasp.org/www-project-application-security-verification-standard/)


<!-- @source rules/subscription-state-machine-explicit.md -->

---
title: "Subscription.status must only be mutated through SubscriptionStateMachine; direct setStatus() calls outside the state machine are prohibited"
rule_id: subscription-state-machine-explicit
impact: CRITICAL
impactDescription: "Direct setStatus() calls bypass the state machine's transition validation and BillingEvent recording, creating silent state corruption and missing audit trail entries"
tags:
  - billing
  - state-machine
  - subscription
  - audit
provenance_class: internal_design
protects_template_id: templates/backend/billing/Subscription.java
failing_fixture_path: practices/evals/fixtures/subscription-state-machine/fail_direct_setstatus/
spec_ref: "specs/billing-l0.yaml#BILLING-STATE-001"
verification:
  type: archunit
  notes: |
    ArchUnit rule:
    noClasses().that().areNotAssignableTo(SubscriptionStateMachine.class)
    .should().callMethodWhere(
      target().hasName("applyStatusTransition")
      .and(owner().isAssignableTo(Subscription.class))
    )
    Failing fixture: any class besides SubscriptionStateMachine calling applyStatusTransition().
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Subscription lifecycle"
    quote: "trialing — trial period active; active — subscription is current; past_due — latest invoice payment attempt failed; canceled — subscription ended"
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "정기결제 구독 상태 매핑"
    quote: "ACTIVE: 정상 사용 가능, INACTIVE: 카드 만료/분실 등으로 비활성화"
  - source_type: external
    citation: "Domain-Driven Design — Aggregates encapsulate invariants; state transitions are explicit methods on the aggregate, not raw field mutations"
    url: "https://martinfowler.com/bliki/DDD_Aggregate.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Subscription.status must only be mutated through SubscriptionStateMachine

**Impact: CRITICAL — Calling `subscription.applyStatusTransition()` directly from service code bypasses transition validation, skips BillingEvent recording, and leaves the audit trail incomplete. Subscription state becomes inconsistent with billing events.**

The `SubscriptionStateMachine` is the sole class responsible for:
1. Validating whether a transition is allowed (TRIAL→PAST_DUE is invalid; PAST_DUE→ACTIVE is valid).
2. Calling `Subscription.applyStatusTransition()` (package-private method).
3. Recording a `BillingEvent` for the transition (append-only audit trail).
4. Emitting `billing.subscription.lifecycle_transition` counter.

Any code that mutates `Subscription.status` outside this machine will:
- Skip transition validation (allowing impossible states like CANCELLED→ACTIVE without payment).
- Leave no BillingEvent audit record (compliance and debugging impact).
- Cause observability counters to miss transitions.

**Incorrect — direct applyStatusTransition() outside SubscriptionStateMachine:**

```java
// VIOLATION: direct mutation bypasses validation and BillingEvent recording
subscription.applyStatusTransition(SubscriptionStatus.ACTIVE);
subscriptionRepository.save(subscription);
// No BillingEvent recorded. Transition validation skipped. Counter not incremented.
```

**Correct — all state transitions through SubscriptionStateMachine.transition():**

```java
// CORRECT: all state transitions through the state machine
BillingEvent event = stateMachine.transition(
    subscription,
    SubscriptionStateMachine.Trigger.PAYMENT_SUCCEEDED_WEBHOOK,
    webhookMetadataJson
);
// Validates PAST_DUE→ACTIVE transition.
// Saves BillingEvent(PAYMENT_SUCCEEDED, idempotencyKey=...).
// Increments billing.subscription.lifecycle_transition counter.
```

Reference: https://martinfowler.com/bliki/DDD_Aggregate.html

## ArchUnit enforcement

```java
// OnlyStateMachineMutatesSubscriptionStatusArchTest.java
@ArchTest
static final ArchRule onlyStateMachineMutatesStatus = noClasses()
    .that().areNotAssignableTo(SubscriptionStateMachine.class)
    .should().callMethodWhere(
        target().hasName("applyStatusTransition")
            .and(owner().isAssignableTo(Subscription.class))
    )
    .because("Subscription status may only be changed via SubscriptionStateMachine");
```

## Failing fixture

See: `practices/evals/fixtures/subscription-state-machine/fail_direct_setstatus/BillingServiceDirectStatus.java` — a service method that calls `subscription.applyStatusTransition()` directly.


<!-- @source rules/testing-archunit-layer-boundary.md -->

---
title: Enforce controller → service → repository layering with ArchUnit
impact: MEDIUM
impactDescription: "Mechanical check prevents upward layer references that compile-time can't catch"
tags:
  - testing
  - archunit
  - architecture
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-002
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — Rules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit User Guide — Rules and Slicing"
    url: "https://www.archunit.org/userguide/html/000_Index.html"
---

## Enforce controller → service → repository layering with ArchUnit

**Impact: MEDIUM — Mechanical check prevents upward layer references that compile-time can't catch**

Java's compiler is happy to let a `*Service` import a `*Controller` — the bytecode is perfectly valid. The architecture-level rule "services don't depend on controllers" lives only in human review and slowly erodes. ArchUnit moves that rule into the test suite: a single `noClasses()...should().dependOnClassesThat()` rule scans the bytecode and fails the build when a *Service or *Repository reaches upward into the layer above it.

**Incorrect — a service that imports a controller (no compile error, slow architectural rot):**

```java
@Service
public class ReportService {
    private final ReportController controller;        // upward dependency
    public Result generate() {
        return controller.handle(...);                 // service calling controller is upside-down
    }
}
```

**Correct — ArchUnit rule fails the build on the violation:**

```java
@Test
void servicesDoNotDependOnControllers() {
    JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example.app");
    noClasses().that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
            .check(classes);
}
```

Verification: `./gradlew testPractices --tests "*ArchitectureLayerBoundary*"` runs the layer rule for the practices/ subtree and asserts no *Service → *Controller and no *Repository → *(Controller|Service) edges exist.

Reference: [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)


<!-- @source rules/testing-archunit-no-cyclic-packages.md -->

---
title: Forbid cyclic package dependencies with ArchUnit slicing
impact: MEDIUM
impactDescription: "Cycles block isolated testing of any module in the cycle"
tags:
  - testing
  - archunit
  - architecture
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-003
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — slicing rules"
    quote: "ArchRule"
  - source_type: external
    citation: "ArchUnit User Guide — Slices"
    url: "https://www.archunit.org/userguide/html/000_Index.html#_slices"
---

## Forbid cyclic package dependencies with ArchUnit slicing

**Impact: MEDIUM — Cycles block isolated testing of any module in the cycle**

If package `a` imports something from `b` and `b` imports something from `a`, neither package can be loaded, compiled (in isolation), or tested without the other — they have collapsed into one module wearing two names. Cycles also block module extraction (Spring Modulith, separate Maven module) and obscure the dependency graph for readers. ArchUnit's slicing detector partitions the namespace and reports any cycle.

**Incorrect — two packages with cross-imports:**

```java
// package com.example.users
package com.example.users;
import com.example.billing.InvoiceFormatter;       // depends on billing
...

// package com.example.billing
package com.example.billing;
import com.example.users.UserPreferences;           // depends on users — CYCLE
```

**Correct — ArchUnit rule catches the cycle:**

```java
@Test
void noCyclicPackageDependencies() {
    JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example..");
    SlicesRuleDefinition.slices()
            .matching("com.example.(*)..")
            .should().beFreeOfCycles()
            .check(classes);
}
```

Verification: `./gradlew testPractices --tests "*NoCyclicPackage*"` partitions the practices/ subtree by direct child package and asserts the slice graph is acyclic.

Reference: [ArchUnit User Guide — Slices](https://www.archunit.org/userguide/html/000_Index.html#_slices)


<!-- @source rules/testing-archunit-repository-shape.md -->

---
title: Classes named *Repository must extend Spring Data's JpaRepository
impact: MEDIUM
impactDescription: "Stops hand-rolled \"repository\" services that bypass the data-access layer's guarantees"
tags:
  - testing
  - archunit
  - persistence
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-004
upstream:
  - "https://www.archunit.org/userguide/html/000_Index.html"
evidence:
  - upstream_id: archunit-userguide
    section: "ArchUnit User Guide — class predicates"
    quote: "JavaClasses"
  - source_type: external
    citation: "Spring Data JPA — Defining repository interfaces"
    url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
---

## Classes named *Repository must extend Spring Data's JpaRepository

**Impact: MEDIUM — Stops hand-rolled "repository" services that bypass the data-access layer's guarantees**

`@Service public class OrderRepository { ... }` is a common drift pattern: a service class that calls `entityManager.createQuery(...)` directly, named "Repository" because that is where the data-access code lives in the developer's head. The drift defeats Spring Data's query derivation, ignores its method-level transaction defaults, sidesteps the `@Repository` exception translation, and confuses every reader who expects the name `*Repository` to mean a Spring Data interface. The ArchUnit rule enforces the shape: classes named *Repository must be interfaces that extend `JpaRepository`.

**Incorrect — hand-rolled "repository" as a class:**

```java
@Service                                          // not @Repository, not Spring Data
public class OrderRepository {                    // class, not interface
    private final EntityManager em;
    public Order findById(Long id) {
        return em.createQuery(...).getSingleResult();
    }
}
```

**Correct — Spring Data interface:**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByCustomerEmail(String email);
}
```

Verification: `./gradlew testPractices --tests "*RepositoriesExtendJpa*"` runs an ArchUnit rule that picks up every `*Repository` class and asserts it is an interface assignable to `JpaRepository`.

Reference: [Spring Data JPA — Defining Repository Interfaces](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)


<!-- @source rules/testing-restassured-blackbox.md -->

---
title: Use RestAssured + @LocalServerPort for practice tests, not MockMvc
impact: MEDIUM
impactDescription: "Black-box HTTP keeps tests portable across implementations"
tags:
  - testing
  - rest-assured
  - portability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-001
upstream:
  - "https://rest-assured.io/"
  - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.spring-boot-applications"
evidence:
  - upstream_id: rest-assured-usage
    section: REST-assured given/when/then DSL
    quote: 'bers as BigDecimal: given (). config ( RestAssured . config (). jsonConfig ( jsonConfig (). numberReturnType ( BIG_DECIMAL ))). when (). get ( "/price" ). then (). body ( "price" , is ( new BigDecimal ( 12.12 )); JSON Schema validation From version 2.1.0 REST'
  - upstream_id: spring-boot-testing
    section: WebEnvironment.RANDOM_PORT and @LocalServerPort
    quote: our test runs. The @LocalServerPort annotation can be used to inject the actual port used into your test. Tests that need to make REST calls to the started server can autowire a RestTestClient by annotating the test class with @AutoConfigureRestTestClient . Th
  - source_type: external
    citation: 'REST-assured — Usage Guide'
    url: 'https://github.com/rest-assured/rest-assured/wiki/Usage'
  - source_type: external
    citation: 'Spring Boot Reference — §Testing: WebEnvironment.RANDOM_PORT + @LocalServerPort'
    url: 'https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.with-running-server'
---

## Use RestAssured + @LocalServerPort for practice tests, not MockMvc

**Impact: MEDIUM — Black-box HTTP keeps tests portable across implementations**

MockMvc couples a test to Spring's internal dispatcher and its bean configuration. The same test cannot run against a different implementation of the same contract — it ties verification to the framework's internals. RestAssured against `@LocalServerPort` exercises the real HTTP stack: filter chain, serialization, headers, status codes. The same test JAR is portable to any conforming implementation.

**Incorrect — MockMvc binds the test to the dispatcher servlet:**

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mvc;

    @Test void getUser() throws Exception {
        mvc.perform(get("/users/1")).andExpect(status().isOk());
    }
}
```

**Correct — RestAssured against the real HTTP server:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTest {
    @LocalServerPort int port;

    @BeforeEach void setup() { RestAssured.port = port; }

    @Test void getUser() {
        given().when().get("/users/1").then().statusCode(200);
    }
}
```

Verification: `./gradlew testPractices --tests "*RestAssured*"` hits `/actuator/health` over a real port and asserts the test class itself contains no MockMvc references.

Reference: [RestAssured](https://rest-assured.io/) · [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.spring-boot-applications)


<!-- @source rules/traceid-in-error-response.md -->

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


<!-- @source rules/transaction-no-self-invocation.md -->

---
title: Do not self-invoke @Transactional methods
impact: HIGH
impactDescription: "Self-invocation silently bypasses transaction advice, breaking atomicity"
tags:
  - transaction
  - aop
  - spring-proxy
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: Self-invocation and the proxy
    quote: ed. This means that self-invocation (in effect, a method within the target object calling another method of the target object) does not lead to an actual transaction at runtime even if the invoked method is marked with @Transactional . Also, the proxy must be
  - upstream_id: spring-aop-proxying
    section: Understanding AOP proxies
    quote: ng do not have this self-invocation issue because they apply advice within the bytecode instead of via a proxy. Mixing Aspect Types Programmatic Creation of @AspectJ Proxies Spring Framework Stable 7.0.7 6.2.18 Snapshot 7.1.0-SNAPSHOT 7.0.8-SNAPSHOT 6.2.19-SNA
  - source_type: external
    citation: 'Spring Framework Reference — §Declarative transaction management: Method visibility (proxy mechanism)'
    url: 'https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html'
  - source_type: external
    citation: 'Spring Framework Reference — §Understanding AOP proxies (self-invocation)'
    url: 'https://docs.spring.io/spring-framework/reference/core/aop/proxying.html'
---

## Do not self-invoke @Transactional methods

**Impact: HIGH — Self-invocation silently bypasses transaction advice, breaking atomicity**

Spring `@Transactional` is implemented by an AOP proxy. When a public method on a service calls another method on the same instance via `this.method()`, the call goes directly to the underlying class — the proxy never sees it, and the `@Transactional` advice is skipped. The bug is silent: no exception, no log, just no transaction. Failures partway through never roll back, dirty reads slip through, and audit logs lose causality.

**Incorrect — self-invocation skips the proxy:**

```java
@Service
public class ReportService {
    public void generate() {
        this.persistReport();   // direct call, proxy bypassed → no transaction
    }

    @Transactional
    public void persistReport() {
        repo.saveAll(...);
    }
}
```

**Correct — invoke through a separate bean (proxy is honored):**

```java
@Service
public class ReportService {
    private final ReportPersistence persistence;
    public ReportService(ReportPersistence persistence) {
        this.persistence = persistence;
    }
    public void generate() {
        persistence.persistReport();   // through proxy → @Transactional honored
    }
}

@Service
public class ReportPersistence {
    @Transactional
    public void persistReport() {
        repo.saveAll(...);
    }
}
```

Verification: `./gradlew testPractices --tests "*SelfInvocation*"` asserts `TransactionSynchronizationManager.isActualTransactionActive()` is `false` after self-invocation and `true` after proxy invocation.

Reference: [Spring Framework — Declarative transaction management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)


<!-- @source rules/transaction-propagation-requires-new.md -->

---
title: Use Propagation.REQUIRES_NEW for writes that must persist independently
impact: MEDIUM
impactDescription: "Audit logs / side-effect writes lost when the caller's outer transaction rolls back"
tags:
  - transaction
  - propagation
  - audit
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-004
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional propagation — REQUIRED vs REQUIRES_NEW"
    quote: "propagation"
  - source_type: external
    citation: "Spring Framework Reference — Transaction Propagation"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
---

## Use Propagation.REQUIRES_NEW for writes that must persist independently

**Impact: MEDIUM — Audit logs / side-effect writes lost when the caller's outer transaction rolls back**

The default propagation is `REQUIRED`: the method joins the caller's transaction (or starts one if none exists). That is correct for almost every business operation. But audit logs, billing side-effects, outbox writes, and other "this must commit regardless of the caller's success/failure" writes must run in `Propagation.REQUIRES_NEW` — the framework suspends the outer transaction, opens a new one for the inner method, commits it, and resumes the outer. Otherwise an outer rollback silently swallows the audit record that was supposed to survive.

**Incorrect — default REQUIRED for an audit write:**

```java
@Service
public class TransferService {
    @Transactional
    public void transfer(...) {
        accounts.debit(...);
        accounts.credit(...);
        auditWriter.record(...);   // joins this tx — outer rollback loses the audit row
        throw new BusinessRuleViolation();
    }
}
```

**Correct — REQUIRES_NEW for the audit write:**

```java
@Service
public class AuditWriter {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent e) {
        auditRepo.save(e);
    }
}
```

Verification: `./gradlew testPractices --tests "*PropagationRequiresNew*"` opens an outer `@Transactional` test method, calls a `REQUIRES_NEW` bean, and asserts the inner transaction name differs from the outer (proving the suspend / new-tx semantics).

Reference: [Spring Framework — Transaction Propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)


<!-- @source rules/transaction-readonly-queries.md -->

---
title: Mark read-only queries with @Transactional(readOnly = true)
impact: MEDIUM
impactDescription: "Skips dirty-checking + enables replica routing — silent perf win when set, silent overhead when forgotten"
tags:
  - transaction
  - performance
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional attributes — readOnly"
    quote: "@Transactional"
  - source_type: external
    citation: "Spring Framework Reference — Declarative transaction management"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
---

## Mark read-only queries with @Transactional(readOnly = true)

**Impact: MEDIUM — Skips dirty-checking + enables replica routing — silent perf win when set, silent overhead when forgotten**

`@Transactional(readOnly = true)` is more than documentation. JPA / Hibernate uses the flag to skip the dirty-checking pass at flush time; a `ReplicaAwareDataSource` / `LazyConnectionDataSourceProxy` reads the flag to route the connection to a read replica. Forgetting it on a query-only path is silent — the data still returns, but with full read-write overhead and (when configured) on the primary.

**Incorrect — default @Transactional on a read-only method:**

```java
@Service
public class OrderQueryService {
    @Transactional                                // read-write semantics on a query
    public List<OrderSummary> recentOrders(...) { ... }
}
```

**Correct — explicit readOnly flag:**

```java
@Service
public class OrderQueryService {
    @Transactional(readOnly = true)               // dirty-check skipped, replica-routable
    public List<OrderSummary> recentOrders(...) { ... }
}
```

Verification: `./gradlew testPractices --tests "*TransactionReadOnly*"` asserts `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` is `true` inside the read-only method and `false` inside the default method.

Reference: [Spring Framework — Declarative transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)


<!-- @source rules/transaction-rollback-on-checked.md -->

---
title: Declare rollbackFor when the method throws a checked exception
impact: HIGH
impactDescription: "Default rollback policy ignores checked exceptions — half-done writes commit"
tags:
  - transaction
  - rollback
  - exception
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional — rollbackFor attribute"
    quote: "rollback"
  - source_type: external
    citation: "Spring Framework Reference — Rolling back a declarative transaction"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html"
---

## Declare rollbackFor when the method throws a checked exception

**Impact: HIGH — Default rollback policy ignores checked exceptions — half-done writes commit**

`@Transactional` rolls back the transaction only when the method throws an *unchecked* exception (`RuntimeException` or `Error`). A checked exception that escapes the method — `IOException`, `JsonProcessingException`, the project's own domain checked exceptions — is treated as a successful return: the transaction commits, half-done writes persist, and the caller sees an exception thrown over fully-committed state. The remedy is to declare `rollbackFor = Exception.class` (or the narrower checked types) on the annotation.

**Incorrect — default rollback policy hides checked-exception failures:**

```java
@Transactional
public void persistReport(Report r) throws IOException {
    repo.save(r);
    fileSink.write(r);          // throws IOException — repo.save already committed
}
```

**Correct — rollbackFor declares the contract:**

```java
@Transactional(rollbackFor = Exception.class)
public void persistReport(Report r) throws IOException {
    repo.save(r);
    fileSink.write(r);          // IOException now rolls back the save
}
```

Verification: `./gradlew testPractices --tests "*RollbackForChecked*"` asserts via reflection that the correct fixture declares `rollbackFor = Exception.class` and the anti-pattern fixture leaves it empty.

Reference: [Spring Framework — Rolling back declarative transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)


<!-- @source rules/validation-custom-constraint.md -->

---
title: Encode domain-specific shape in @Constraint + ConstraintValidator
impact: MEDIUM
impactDescription: "Keeps the rule on the field, not in service code; composes with built-ins"
tags:
  - validation
  - custom-constraint
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-003
upstream:
  - "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/"
evidence:
  - upstream_id: hibernate-validator
    section: "Hibernate Validator — defining a custom ConstraintValidator"
    quote: "ConstraintValidator"
  - source_type: external
    citation: "Hibernate Validator Reference — Custom constraints"
    url: "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#section-creating-custom-constraint"
---

## Encode domain-specific shape in @Constraint + ConstraintValidator

**Impact: MEDIUM — Keeps the rule on the field, not in service code; composes with built-ins**

Built-in constraints cover the common 80%, but every project has shapes the spec does not — `@ValidUsername`, `@ValidIsbn`, `@ValidUkPostcode`. Re-implementing them inside services (`if (!username.matches(pattern)) throw ...`) duplicates the rule at every call site. Defining a `@Constraint` annotation with a `ConstraintValidator` makes the rule field-local, composable with built-ins, and consumable everywhere a validation annotation is — DTOs, method parameters, record components.

**Incorrect — imperative regex check inside the service:**

```java
public User register(String username, ...) {
    if (!username.matches("^[a-z0-9_]{3,20}$")) {
        throw new IllegalArgumentException("bad username");
    }
    ...
}
```

**Correct — custom @ValidUsername annotation + ConstraintValidator:**

```java
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {
    String message() default "must be 3-20 lowercase letters, digits, or underscore";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {
    private static final Pattern P = Pattern.compile("^[a-z0-9_]{3,20}$");
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value == null || P.matcher(value).matches();
    }
}
```

Verification: `./gradlew testPractices --tests "*CustomConstraint*"` asserts invalid usernames (`BAD-USERNAME`, `ab`) are rejected with `errors.field` containing `username`, and that a valid username (`bob_1`) is accepted.

Reference: [Hibernate Validator — Creating custom constraints](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#section-creating-custom-constraint)


<!-- @source rules/validation-error-envelope.md -->

---
title: Return validation failures as RFC 7807 with a structured errors[] array
impact: HIGH
impactDescription: "Clients can render per-field messages without parsing free-form strings"
tags:
  - validation
  - error
  - rfc-7807
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-004
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc7807"
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
evidence:
  - upstream_id: rfc-7807
    section: "RFC 7807 — Problem Details + extension members"
    quote: "extension"
  - upstream_id: spring-mvc-validation
    section: "Spring MVC — MethodArgumentNotValidException carries BindingResult"
    quote: "MethodArgumentNotValidException"
  - source_type: external
    citation: "RFC 7807 §3.2 — Extension Members"
    url: "https://datatracker.ietf.org/doc/html/rfc7807#section-3.2"
---

## Return validation failures as RFC 7807 with a structured errors[] array

**Impact: HIGH — Clients can render per-field messages without parsing free-form strings**

A `MethodArgumentNotValidException` carries a `BindingResult` with one entry per violating field. The default Spring response is a generic 400, which forces clients to parse the message string. The contract-friendly response is an RFC 7807 ProblemDetail with the standard `type/title/status/detail` keys *plus* an `errors` extension array of `{field, message}` objects. Clients render per-field error labels next to inputs, dashboards aggregate by field, and the response shape stays uniform across all validation paths.

**Incorrect — generic 400 body forces clients to scrape strings:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<String> bad(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest()
            .body("validation failed: " + ex.getMessage());
}
```

**Correct — ProblemDetail with an errors[] extension:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setType(URI.create("https://errors.example.com/validation"));
    pd.setTitle("Validation Error");
    pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of(
                    "field",   e.getField(),
                    "message", e.getDefaultMessage()
            ))
            .toList());
    return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
}
```

Verification: `./gradlew testPractices --tests "*ErrorEnvelope*"` asserts the response carries `application/problem+json`, `type` / `title` / `status` / `detail`, AND an `errors[]` array containing entries for every failing field (`name`, `email`, `username`).

Reference: [RFC 7807 §3.2 Extension Members](https://datatracker.ietf.org/doc/html/rfc7807#section-3.2) · [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)


<!-- @source rules/validation-jakarta-bean-constraints.md -->

---
title: Annotate DTOs with Jakarta Bean Validation + @Valid on the handler
impact: HIGH
impactDescription: "Standard constraint vocabulary; one mechanism handles every endpoint uniformly"
tags:
  - validation
  - jakarta
  - dto
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
  - "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/"
evidence:
  - upstream_id: spring-mvc-validation
    section: "Spring MVC @Valid + MethodArgumentNotValidException"
    quote: "@Valid"
  - upstream_id: hibernate-validator
    section: "Hibernate Validator — built-in constraints (@NotBlank, @Email, @Size)"
    quote: "@NotBlank"
  - source_type: external
    citation: "Spring Framework Reference — Validation in Spring MVC"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
---

## Annotate DTOs with Jakarta Bean Validation + @Valid on the handler

**Impact: HIGH — Standard constraint vocabulary; one mechanism handles every endpoint uniformly**

Hand-rolled `if (req.name == null || req.name.isBlank()) throw new IllegalArgumentException(...)` scatters validation across every controller method. Jakarta Bean Validation moves the rules onto the DTO, lets Spring run them automatically when `@Valid` is on the handler parameter, and turns the failure into a single typed exception (`MethodArgumentNotValidException`) that one `@ExceptionHandler` can map to a uniform response shape.

**Incorrect — imperative checks scattered across handlers:**

```java
@PostMapping("/users")
public User create(@RequestBody UserCreateRequest req) {
    if (req.name() == null || req.name().isBlank()) {
        throw new IllegalArgumentException("name is required");
    }
    if (req.email() == null || !req.email().contains("@")) {
        throw new IllegalArgumentException("email is invalid");
    }
    return service.create(req);
}
```

**Correct — constraints on the DTO + @Valid on the handler:**

```java
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @NotBlank @Email String email
) {}

@PostMapping("/users")
public User create(@Valid @RequestBody UserCreateRequest req) {
    return service.create(req);
}
```

Verification: `./gradlew testPractices --tests "*JakartaBeanConstraints*"` exercises blank, oversized, and invalid-email payloads, asserts each returns 400, and asserts a valid payload returns 200.

Reference: [Spring MVC — Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html) · [Hibernate Validator Reference](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/)


<!-- @source rules/validation-mass-assignment-guard.md -->

---
title: Bind HTTP payloads to a whitelist DTO, never directly to an entity
impact: HIGH
impactDescription: "Prevents privilege escalation via mass-assignment of protected entity fields"
tags:
  - validation
  - dto
  - mass-assignment
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
evidence:
  - upstream_id: owasp-mass-assignment
    section: Mass Assignment definition and remediation
    quote: Mass Assignment - OWASP Cheat Sheet Series Skip to content OWASP Cheat Sheet Series Mass Assignment Initializing search OWASP/CheatSheetSeries OWASP Cheat Sheet Series OWASP/CheatSheetSeries Introduction Index Alphabetical Index ASVS Index
  - upstream_id: cwe-915
    section: CWE-915 — Improperly Controlled Modification of Dynamically-Determined Object Attributes
    quote: 'CWE - CWE-915: Improperly Controlled Modification of Dynamically-Determined Object Attributes (4.20) Common Weakness Enumeration A community-developed list of SW &amp; HW weaknesses that can become vulnerabilities Home &gt; CWE List &gt; CWE-915: Improper'
  - upstream_id: spring-mvc-modelattribute
    section: '@ModelAttribute / data binding'
    quote: sources Validation, Data Binding, and Type Conversion Validation Using Spring&#8217;s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Va
  - source_type: external
    citation: 'OWASP Mass Assignment Cheat Sheet'
    url: 'https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html'
  - source_type: external
    citation: 'CWE-915: Improperly Controlled Modification of Dynamically-Determined Object Attributes'
    url: 'https://cwe.mitre.org/data/definitions/915.html'
---

## Bind HTTP payloads to a whitelist DTO, never directly to an entity

**Impact: HIGH — Prevents privilege escalation via mass-assignment of protected entity fields**

If a controller accepts `@RequestBody Entity entity`, every JSON field maps onto the entity by name. An attacker who adds `"role":"ADMIN"` to an otherwise-legitimate update body silently escalates privilege. The remedy: bind to a DTO whose fields enumerate only the user-controllable inputs. Protected fields (role, enabled, owner, internal IDs) are absent from the DTO and therefore cannot be set, regardless of what the attacker sends.

**Incorrect — direct entity binding lets the client set anything:**

```java
@PutMapping("/users/{id}")
public User update(@PathVariable Long id, @RequestBody User user) {
    user.setId(id);
    return userRepo.save(user); // attacker controls role, enabled, etc.
}
```

**Correct — bind to a whitelist DTO, then copy only safe fields:**

```java
public class UserUpdateDto {
    @NotBlank public String name;
    @Email public String email;
    // no role, no enabled — by design
}

@PutMapping("/users/{id}")
public User update(@PathVariable Long id, @RequestBody @Valid UserUpdateDto dto) {
    User u = userRepo.findById(id).orElseThrow();
    u.setName(dto.name);
    u.setEmail(dto.email);
    return userRepo.save(u);   // role / enabled keep server-controlled values
}
```

Verification: `./gradlew testPractices --tests "*MassAssignment*"` asserts that direct entity binding propagates the attacker's `role` field, while DTO-mediated binding preserves the server default `USER`.

Reference: [Spring MVC — @ModelAttribute method arguments](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)


<!-- @source rules/web-explicit-produces.md -->

---
title: Controllers must declare produces = application/json explicitly
impact: MEDIUM
impactDescription: "Without explicit produces, content negotiation can serve XML or text depending on Accept header"
tags:
  - web
  - content-negotiation
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "Spring MVC — @RequestMapping consumes / produces"
    quote: "produces"
  - source_type: external
    citation: "Spring Framework Reference — Producible Media Types"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-produces"
---

## Controllers must declare produces = application/json explicitly

**Impact: MEDIUM — Without explicit produces, content negotiation can serve XML or text depending on Accept header**

Spring's default `ContentNegotiationManager` derives the response content type from the request's `Accept` header. If a client sends `Accept: application/xml` and an XML message converter is on the classpath (e.g. via `spring-boot-starter-data-rest`), the same handler suddenly returns XML — a contract shift the API never agreed to. The mechanical remedy is to declare `produces = MediaType.APPLICATION_JSON_VALUE` on the controller's class-level `@RequestMapping`. Spring honors it as a hard requirement: any incompatible Accept header returns 406 instead of silently re-serializing.

**Incorrect — implicit produces, controlled by Accept header:**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { ... }
}
// Accept: application/xml + Jackson-XML on classpath → XML response (silent contract drift)
```

**Correct — explicit produces declared on the class:**

```java
@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { ... }
}
// Accept: application/xml → 406 Not Acceptable. JSON contract preserved.
```

Verification: `./gradlew testPractices --tests "*ProducesContract*"` uses reflection on the class-level `@RequestMapping` and asserts `produces()` is non-empty and contains `application/json`.

Reference: [Spring MVC — Producible Media Types](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-produces)


<!-- @source rules/web-rest-controller-annotation.md -->

---
title: JSON-API controllers must carry @RestController, never bare @Controller
impact: MEDIUM
impactDescription: "Bare @Controller resolves return values as view names — silent 404 for DTO returns"
tags:
  - web
  - controller
  - spring-mvc
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "Spring MVC — @RestController shortcut for @Controller + @ResponseBody"
    quote: "@RestController"
  - source_type: external
    citation: "Spring Framework Reference — Annotated Controllers"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html"
---

## JSON-API controllers must carry @RestController, never bare @Controller

**Impact: MEDIUM — Bare @Controller resolves return values as view names — silent 404 for DTO returns**

`@Controller` is the original Spring MVC annotation; its return values are interpreted as *view names* (the controller is half of a server-rendered MVC pair). A JSON API returning a DTO from a `@Controller` method does NOT serialize the DTO — Spring looks up a view named after the DTO's `toString()` and 404s when none exists. `@RestController` is the meta-annotation `@Controller` + `@ResponseBody`; every method's return value is serialized through the configured converter chain (Jackson → JSON by default). The mechanical remedy is to enforce `@RestController` on every class whose name ends with `Controller`.

**Incorrect — bare @Controller for a JSON endpoint:**

```java
@Controller                                  // resolves return values as view names
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.findById(id);         // Spring tries to render a view named after the DTO
    }
}
```

**Correct — @RestController:**

```java
@RestController                              // == @Controller + @ResponseBody
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.findById(id);         // serialized as JSON via Jackson
    }
}
```

Verification: `./gradlew testPractices --tests "*RestControllerAnnotation*"` runs an ArchUnit rule that asserts every `*Controller` class under `practices/` is annotated with `@RestController`.

Reference: [Spring Framework — Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)


<!-- @source rules/web-specific-mapping-methods.md -->

---
title: Use @GetMapping / @PostMapping shortcuts, never bare @RequestMapping
impact: MEDIUM
impactDescription: "@RequestMapping(method=...) is verbose AND footgun — forgetting method= exposes every verb"
tags:
  - web
  - mapping
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-WEB-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-WEB-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
evidence:
  - upstream_id: spring-mvc-controlleradvice
    section: "Spring MVC — HTTP method-specific shortcuts"
    quote: "@GetMapping"
  - source_type: external
    citation: "Spring Framework Reference — HTTP method-specific shortcuts"
    url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
---

## Use @GetMapping / @PostMapping shortcuts, never bare @RequestMapping

**Impact: MEDIUM — @RequestMapping(method=...) is verbose AND footgun — forgetting method= exposes every verb**

`@RequestMapping("/users/{id}")` without an explicit `method = RequestMethod.GET` matches every HTTP verb — GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD. A handler that reads a user by id becomes silently reachable by POST, by DELETE, by every verb a curious client tries. The method-specific shortcuts (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`) make the verb mandatory by construction — the annotation name *is* the verb. Class-level `@RequestMapping` for the path prefix is fine; method-level `@RequestMapping` is the anti-pattern.

**Incorrect — bare @RequestMapping on a method:**

```java
@RestController
public class UserController {
    @RequestMapping("/users/{id}")             // matches GET, POST, PUT, DELETE, PATCH, ...
    public UserResponse get(@PathVariable Long id) { ... }
}
```

**Correct — method-specific shortcut:**

```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")                 // GET only
    public UserResponse get(@PathVariable Long id) { ... }
}
```

Verification: `./gradlew testPractices --tests "*SpecificMappingMethods*"` walks every declared method on `PracticesDemoController`, flags any that carry `@RequestMapping` without one of the method-specific shortcuts.

Reference: [Spring MVC — HTTP method-specific shortcuts](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)


<!-- @source rules/webhook-hmac-required.md -->

---
title: Inbound webhook endpoints must verify HMAC-SHA256 signatures before processing
impact: HIGH
impactDescription: "Webhook endpoints without signature verification accept forged payloads from any attacker who knows the endpoint URL"
tags:
  - integration
  - security
  - hmac
  - webhook
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-INTEG-001"
verification:
  gradle_task: testIntegration
  tag: INTEGRATION
failing_fixture_path: "practices/evals/fixtures/webhook_hmac/fail_no_hmac"
passing_fixture_path: "practices/evals/fixtures/webhook_hmac/pass"
evidence:
  - source_type: external
    citation: "GitHub Docs — Validating webhook deliveries: use MessageDigest.isEqual() for constant-time comparison to prevent timing attacks; compare sha256= prefix and hex digest"
    url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
  - source_type: external
    citation: "OWASP ASVS V13.2.6 — Verify that webhook payloads are verified with an HMAC signature or equivalent mechanism before processing to ensure authenticity and integrity"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
  - source_type: external
    citation: "RFC 2104 — HMAC: Keyed-Hashing for Message Authentication. Section 2: HMAC-SHA256 requires constant-time comparison to prevent timing side channels"
    url: "https://www.rfc-editor.org/rfc/rfc2104"
---

## Inbound webhook endpoints must verify HMAC-SHA256 signatures before processing

**Impact: HIGH — Webhook endpoints without signature verification accept forged payloads from any attacker who knows the endpoint URL**

External webhook providers (GitHub, Stripe, Twilio, etc.) sign each outbound event with an HMAC-SHA256 digest of the raw request body using a shared secret. The receiver must verify this signature **before** deserialising or acting on the payload. Skipping verification allows an attacker to POST any payload — triggering deployments, marking orders as paid, or injecting arbitrary events — without possessing the shared secret.

Critical implementation details:
1. **Raw bytes, not parsed JSON** — use `@RequestBody byte[]`, never `@RequestBody String` or a DTO, because JSON parsers normalise whitespace and key ordering, which alters the byte representation and breaks HMAC verification.
2. **Constant-time comparison** — use `MessageDigest.isEqual(expected, received)`, never `Arrays.equals` or `String.equals`. The latter short-circuit on the first mismatch and leak the valid prefix length to a timing attacker.
3. **`sha256=` prefix** — the industry convention (GitHub, Stripe) is `sha256=<hexdigest>`; verify the prefix before hex-decoding.
4. **Store the secret in Vault / Secrets Manager** — never hardcode in source.

**Incorrect — processes payload without any signature check:**

```java
@PostMapping("/api/webhooks/github")
public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
    // VIOLATION: no HMAC verification — any request is accepted
    processEvent(payload);
    return ResponseEntity.ok().build();
}
```

**Correct — constant-time HMAC verification before processing:**

```java
@PostMapping("/api/webhooks/github")
public ResponseEntity<Void> receiveWebhook(
        @RequestHeader("X-Hub-Signature-256") String signatureHeader,
        @RequestBody byte[] rawBody) {

    // Step 1: verify HMAC (throws 401 on failure)
    webhookReceiver.verify(signatureHeader, rawBody);

    // Step 2: idempotency check
    webhookReceiver.markProcessed(deliveryId);

    // Step 3: process
    processEvent(rawBody);
    return ResponseEntity.ok().build();
}
```

See `templates/backend/integration/WebhookReceiver.java` for the reference implementation.

Reference: [GitHub Docs — Validating webhook deliveries](https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries)

Reference: [OWASP ASVS V13.2.6 — Webhook payload verification](https://owasp.org/www-project-application-security-verification-standard/)

Reference: [RFC 2104 — HMAC: Keyed-Hashing for Message Authentication](https://www.rfc-editor.org/rfc/rfc2104)


---

# Catalog TOC (observability — not part of sentinel sha)

## L4 domains

- **activity-feed** — applied by: (none)
- **api-key** — applied by: (none)
- **approval-workflow** — applied by: (none)
- **audit-log** — applied by: api-gateway-relay, b2b-admin, booking, cms, community, crm, e-commerce, internal-it, lms, marketplace, saas-subscription
- **auth** — applied by: api-gateway-relay, b2b-admin, cms, community, internal-it, lms, saas-subscription
- **billing** — applied by: (none)
- **comment-thread** — applied by: (none)
- **crud** — applied by: api-gateway-relay, b2b-admin, booking, cms, community, crm, e-commerce, internal-it, lms, marketplace
- **email-outbox** — applied by: (none)
- **favorites-bookmarks** — applied by: (none)
- **feature-flags** — applied by: api-gateway-relay, b2b-admin, booking, lms, marketplace, saas-subscription
- **file-storage** — applied by: (none)
- **notification** — applied by: api-gateway-relay, booking, cms, community, crm, e-commerce, internal-it, lms, marketplace, saas-subscription
- **payment** — applied by: booking, e-commerce, marketplace
- **practices** — applied by: (none)
- **scheduled-task** — applied by: api-gateway-relay, cms, internal-it, lms
- **search** — applied by: b2b-admin, cms, community, crm, e-commerce, marketplace
- **session-management** — applied by: (none)
- **tag-categorization** — applied by: (none)
- **webhook** — applied by: api-gateway-relay, internal-it

## Active recipes

- **api-gateway-relay** — enabled L4: audit-log, auth, crud, scheduled-task, webhook
- **b2b-admin** — enabled L4: audit-log, auth, crud, feature-flags, search
- **booking** — enabled L4: audit-log, crud, feature-flags, notification, payment
- **cms** — enabled L4: audit-log, crud, notification, scheduled-task
- **community** — enabled L4: audit-log, auth, crud, notification, search
- **crm** — enabled L4: crud, audit-log, notification, search
- **e-commerce** — enabled L4: crud, payment, notification, audit-log, search
- **internal-it** — enabled L4: audit-log, auth, crud, notification, scheduled-task, webhook
- **lms** — enabled L4: audit-log, auth, crud, notification, scheduled-task
- **marketplace** — enabled L4: audit-log, crud, notification, payment, search
- **saas-subscription** — enabled L4: billing, auth, feature-flags, notification, audit-log

## Sealed verdicts

- api-gateway-relay-verdict
- b2b-admin-verdict
- booking-verdict
- cms-verdict
- community-verdict
- crm-verdict
- e-commerce-verdict
- internal-it-verdict
- lms-verdict
- marketplace-verdict
- saas-subscription-verdict
- scheduler-l4-verdict
- webhook-l4-verdict

