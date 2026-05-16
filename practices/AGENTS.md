---
sentinel:
  source_concat_sha256: "4bdba738b5306139194e57e2dbada1892a859d5e67046ce612f76ad708bba5c0"
  rule_count: 64
  generated_by: "practices/generate_agents.sh"
---

# Practices — AGENTS.md (auto-generated)

This file is auto-generated from `practices/rules/*.md` in lexical order.
Do not edit by hand — re-run `practices/generate_agents.sh` after rule changes.

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
    private long amountCents;
    public void setAmountCents(long v) { this.amountCents = v; }   // mutable AFTER publish
    // ...
}
```

**Correct — record payload, every component final by construction:**

```java
public record OrderPlacedEvent(String orderId, long amountCents, Instant placedAt) {}

OrderPlacedEvent evt = new OrderPlacedEvent("ord-123", 4_999L, Instant.now());
publisher.publish(MessageTopics.ORDER_PLACED, evt);
// no setters, every component final, equals/hashCode/toString auto-generated
```

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
title: Redact PII before it enters a log statement
impact: HIGH
impactDescription: "Application logs are indexed and retained; raw PII is a compliance + breach-radius hazard"
tags:
  - observability
  - security
  - pii
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-OBS-003
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
evidence:
  - upstream_id: owasp-logging-cheatsheet
    section: "OWASP Logging Cheat Sheet — Data to exclude"
    quote: "exclude"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
---

## Redact PII before it enters a log statement

**Impact: HIGH — Application logs are indexed and retained; raw PII is a compliance + breach-radius hazard**

Logs flow through aggregators, SIEMs, retention buckets, backups, and developer terminals. Anything written to a log statement is — practically — broadcast to a wider audience than the original request handler ever was. Per the OWASP Logging Cheat Sheet, the safe default is to redact PII (email, phone, SSN, payment data, session tokens) *at the source*: before the string is handed to `log.info(...)`. Sanitising downstream (log scrubbers) is best-effort and routinely bypassed by new fields.

**Incorrect — raw user data in a log message:**

```java
String email = user.getEmail();
String phone = user.getPhone();
log.info("password reset for user " + email + " phone " + phone);
```

**Correct — redactor at the boundary:**

```java
log.info("password reset for user {}", PiiRedactor.redact(user.identifier()));
// or, prefer structured fields with a known-safe id:
log.atInfo().addKeyValue("user_id", user.id()).setMessage("password reset").log();
```

Verification: `./gradlew testPractices --tests "*NoPiiInLogs*"` exercises the `PiiRedactor` over emails / phones / SSNs and asserts the original strings are gone, the redaction markers are present, and clean strings pass through unchanged.

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)


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


