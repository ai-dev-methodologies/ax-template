---
name: spring-practices
description: Spring Boot / Java best-practices reviewer for the ax-template practices maintainer. Triggers when editing practices/rules/*.md or specs/spring-practices-l0.yaml. Provides the 22-category advisory catalog and when-to-apply guidance.
metadata:
  priority: 4
  docs:
    - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
    - "https://docs.spring.io/spring-security/reference/"
    - "https://owasp.org/www-project-application-security-verification-standard/"
  pathPatterns:
    - 'practices/rules/**/*.md'
    - 'specs/spring-practices-*.yaml'
    - 'practices/_template.md'
    - 'practices/rubric.yaml'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - spring practices
    - java best practices
    - rule catalog
    - practices maintainer
    - spring rule
  intents:
    - add a new practice rule
    - review spring boot code quality
    - check rule frontmatter
    - evaluate practices coverage
  entities:
    - Spring Boot
    - JPA
    - Spring Security
    - practices
    - ASVS
    - Spec Trio

---

# ax-template Spring Boot Practices

> **STATUS: FROZEN v1.0 (2026-05-17).** This Java/Spring catalog is a **frozen
> reference snapshot**, not an actively curated catalog. Empirical validation
> (Round 3, 2026-05-17) confirmed that the active growth lane is
> `practices-react/` + `eslint-plugin-ax`. Java catalog remains as the
> methodology's worked example — first proof that Spec Trio + binary
> verification + evidence-anchored rules carry. New Spring rules are NOT
> added here; new domains are NOT added here. See `practices/STATUS.md` for
> details.

Advisory catalog for maintainers adding rules to `practices/rules/`. This directory is the **view layer of Spec Trio, NOT a 4th tier** — every rule `.md` must anchor to a `spec_ref` in `specs/`. Hard merge gate is the binary outcome of `./gradlew test{Domain}`.

## Position in the Spec Trio

```
specs/         ← Compliance Spec (source of truth, binary gate)
contracts/     ← API Contract
blueprints/    ← Policy Manifest
practices/     ← VIEW LAYER (human-readable catalog, advisory metrics only)
```

`practices/` is a projection over `specs/`. It cannot gate merges by itself. A rule without `spec_ref` is rejected by `evals/spec_ref_guard.sh` before it reaches review.

## When to Apply

Reference these guidelines when:
- Authoring a new rule in `practices/rules/`
- Selecting which `spec_ref` category a rule belongs to
- Running `evals/run.sh` to produce an advisory report
- Promoting a sub-category to its own spec domain (advisory: ≥ 3 items in one sub-category)
- Reviewing whether the 22-category distribution is balanced

## Rule Authoring Flow

1. **Spec item first** — add or reuse an item in `specs/spring-practices-l0.yaml` (or a promoted domain spec)
2. **Test** — add `@Tag("PRACTICES")` + `@Tag("PRACTICES-<ID>")` test; run `./gradlew testPractices` → PASS
3. **Rule doc** — create `practices/rules/<category>-<slug>.md` with required frontmatter (`spec_ref`, `verification.gradle_task`, `verification.tag`)
4. **Guard** — `evals/spec_ref_guard.sh` blocks merge if `spec_ref` is missing
5. **Advisory** — `evals/run.sh` updates report; scores are advisory, never gate merges

## Category Catalog (22 categories — ADVISORY)

| Priority | Category | Prefix | Impact |
|----------|----------|--------|--------|
| 1 | Language idioms | `lang-` | CRITICAL |
| 2 | Core framework | `core-` | CRITICAL |
| 3 | Configuration | `config-` | HIGH |
| 4 | Web / MVC | `web-` | HIGH |
| 5 | HTTP client | `http-` | HIGH |
| 6 | Persistence / JPA | `persistence-` | HIGH |
| 7 | Transaction | `transaction-` | HIGH |
| 8 | Migration | `migration-` | MEDIUM-HIGH |
| 9 | Security | `security-` | CRITICAL |
| 10 | Validation | `validation-` | HIGH |
| 11 | Error handling | `error-` | HIGH |
| 12 | API design | `api-` | MEDIUM-HIGH |
| 13 | Async / reactive | `async-` | MEDIUM |
| 14 | Messaging | `messaging-` | MEDIUM |
| 15 | Cache | `cache-` | MEDIUM |
| 16 | Observability | `observability-` | MEDIUM |
| 17 | Actuator | `actuator-` | MEDIUM |
| 18 | Testing | `testing-` | HIGH |
| 19 | Build | `build-` | MEDIUM |
| 20 | Native image | `native-` | LOW |
| 21 | Architecture | `arch-` | MEDIUM |
| 22 | Quality / code style | `quality-` | LOW-MEDIUM |

### 1. Language Idioms (`lang-`) — CRITICAL

- `lang-prefer-records` — Use Java records for immutable DTOs
- `lang-sealed-types` — Use sealed interfaces for closed hierarchies
- `lang-text-blocks` — Use text blocks for multi-line SQL / JSON strings
- `lang-var-inference` — Use `var` for local type inference where clarity holds
- `lang-optional-no-field` — Never store `Optional` as a field; use for return types only

### 2. Core Framework (`core-`) — CRITICAL

- `core-constructor-injection` — Prefer constructor injection over field injection
- `core-avoid-applicationcontext` — Do not call `ApplicationContext.getBean()` in production code
- `core-bean-scope-prototype` — Inject prototype-scoped beans via `ObjectProvider` or `@Lookup`
- `core-event-decoupling` — Use `ApplicationEvent` / `@EventListener` for cross-service decoupling

### 3. Configuration (`config-`) — HIGH

- `config-typed-properties` — Bind with `@ConfigurationProperties` record; never raw `@Value` for complex config
- `config-no-hardcoded-secrets` — No credentials in `application.yml`; use env-variable placeholders
- `config-profile-separation` — Separate `application-{profile}.yml`; never override prod values in test profile
- `config-actuator-exposure` — Restrict actuator endpoints via `management.endpoints.web.exposure.include`

### 4. Web / MVC (`web-`) — HIGH

- `web-responseentity-return` — Controllers return `ResponseEntity<T>` with explicit HTTP status
- `web-validated-on-controller` — Place `@Validated` on controller class, `@Valid` on parameter
- `web-no-service-in-controller` — Controllers call service layer only; no repository access
- `web-request-scope-bean` — Never inject `HttpServletRequest` directly; use method param

### 5. HTTP Client (`http-`) — HIGH

- `http-webclient-over-resttemplate` — Use `WebClient` (or `RestClient` 3.2+) instead of deprecated `RestTemplate`
- `http-timeout-mandatory` — Always configure connect-timeout and read-timeout on HTTP client beans
- `http-circuit-breaker` — Wrap external HTTP calls with Resilience4j circuit breaker

### 6. Persistence / JPA (`persistence-`) — HIGH

- `persistence-no-n-plus-1` — Use `JOIN FETCH` or `@EntityGraph` to avoid N+1 queries (`#PERS-001`)
- `persistence-projection-interfaces` — Use Spring Data projections for read-only queries
- `persistence-auditing` — Enable `@EnableJpaAuditing`; use `@CreatedDate` / `@LastModifiedDate`
- `persistence-batch-insert` — Configure `hibernate.jdbc.batch_size` for bulk inserts

### 7. Transaction (`transaction-`) — HIGH

- `transaction-no-self-invocation` — Do not call `@Transactional` methods from within the same bean (`#TX-001`)
- `transaction-read-only` — Mark read-only service methods with `@Transactional(readOnly=true)`
- `transaction-propagation-explicit` — Declare `propagation` explicitly when nesting transactions

### 8. Migration (`migration-`) — MEDIUM-HIGH

- `migration-flyway-versioned` — Use versioned Flyway scripts (`V{n}__{desc}.sql`)
- `migration-no-data-in-schema` — Separate schema DDL from seed data
- `migration-backward-compatible` — Multi-step migration for column renames: add → backfill → drop

### 9. Security (`security-`) — CRITICAL

- `security-method-level` — Enable `@EnableMethodSecurity`; use `@PreAuthorize`
- `security-csrf-spa` — Configure CSRF correctly for SPA
- `security-password-encoder` — Use `BCryptPasswordEncoder` (strength ≥ 12) or Argon2
- `security-sensitive-log` — Never log passwords, tokens, or PII

### 10. Validation (`validation-`) — HIGH

- `validation-mass-assignment-guard` — Use separate request DTOs; never bind directly to `@Entity` (`#VAL-001`)
- `validation-constraint-annotation` — Create custom `@Constraint` annotations for reusable domain rules
- `validation-service-layer` — Re-validate in service layer; controller validation is defense-in-depth only

### 11. Error Handling (`error-`) — HIGH

- `error-problem-detail` — Return RFC 7807 `ProblemDetail` from `@ControllerAdvice`
- `error-no-swallow` — Never catch and ignore exceptions
- `error-typed-exceptions` — Define domain exception hierarchy

### 12. API Design (`api-`) — MEDIUM-HIGH

- `api-versioned-path` — Version APIs via URL path prefix (`/api/v1/`)
- `api-pagination-slice` — Return paginated results as `Slice<T>` or `Page<T>`
- `api-idempotency-key` — Mutating endpoints support `Idempotency-Key` header

### 13. Async / Reactive (`async-`) — MEDIUM

- `async-thread-pool` — Configure a named `ThreadPoolTaskExecutor`
- `async-exception-propagation` — Return `CompletableFuture`; set uncaught exception handler
- `async-virtual-threads` — Prefer virtual threads (`spring.threads.virtual.enabled=true`) in Spring Boot 3.2+

### 14. Messaging (`messaging-`) — MEDIUM

- `messaging-outbox-pattern` — Use transactional outbox for reliable message publishing
- `messaging-idempotent-consumer` — Make consumers idempotent; store processed message IDs
- `messaging-dead-letter` — Configure DLQ/DLT for all consumer bindings

### 15. Cache (`cache-`) — MEDIUM

- `cache-key-strategy` — Explicitly define `@CacheEvict` + `@CachePut` strategy
- `cache-caffeine-over-simple` — Use Caffeine cache manager for local in-process caches
- `cache-null-values` — Configure `cache-null-values: false` unless intentional

### 16. Observability (`observability-`) — MEDIUM

- `observability-micrometer-tracing` — Instrument spans with Micrometer Tracing; propagate `traceId` in MDC
- `observability-structured-logging` — Use Logback JSON encoder + `%mdc{traceId}`
- `observability-metrics-naming` — Follow Micrometer naming convention (`snake_case`, unit suffix)

### 17. Actuator (`actuator-`) — MEDIUM

- `actuator-health-groups` — Define `management.endpoint.health.group.*` for liveness/readiness probes
- `actuator-info-endpoint` — Populate `/actuator/info` with `build-info` and `git-info` plugins
- `actuator-security` — Protect actuator via separate management port or security filter chain

### 18. Testing (`testing-`) — HIGH

- `testing-restassured-blackbox` — Use RestAssured (black-box HTTP) for ASVS / PRACTICES tests, not MockMvc (`#TEST-001`)
- `testing-slices` — Use `@DataJpaTest`, `@WebMvcTest`, `@JsonTest` slices over full `@SpringBootTest`
- `testing-testcontainers` — Use Testcontainers for database integration tests; no H2 in JPA tests
- `testing-builder-pattern` — Use test builder / fixture factories; no repetitive `new Entity(...)` in tests

### 19. Build (`build-`) — MEDIUM

- `build-dependency-management` — Pin versions via Spring Boot BOM
- `build-lint-checkstyle` — Run Checkstyle (Google style) in `check` lifecycle
- `build-reproducible` — Enable reproducible builds in Gradle

### 20. Native Image (`native-`) — LOW

- `native-reflection-hints` — Register reflection hints via `@RegisterReflectionForBinding`
- `native-testcontainers-native` — Run native integration tests with native compilation profile

### 21. Architecture (`arch-`) — MEDIUM

- `arch-package-by-feature` — Organize by feature package, not layer
- `arch-modulith-boundaries` — Use Spring Modulith `@ApplicationModule` to enforce module boundaries
- `arch-hexagonal-ports` — Define explicit port interfaces between domain and infrastructure

### 22. Quality / Code Style (`quality-`) — LOW-MEDIUM

- `quality-no-lombok-data` — Avoid `@Data` on JPA entities; prefer records
- `quality-method-length` — Keep methods ≤ 30 lines
- `quality-cyclomatic-complexity` — Keep cyclomatic complexity ≤ 10 per method

## Evaluation Axes (Advisory — NEVER gate merges)

| Axis | Sub-script | Description |
|------|-----------|-------------|
| Detection | `evals/cases/run.sh` | Test case hit-rate across rules |
| Outcome | `evals/outcome/run.sh` | Semgrep + SpotBugs new-issue delta |
| Reference | (inline in run.sh) | spec_ref completeness across all rules |
| Portability | `evals/portability/run.sh` | Fixture build green with rules applied |
| Drift | `evals/drift/run.sh` | upstream snapshot age (fetched_at ≤ 30 days) |

All axis scores are advisory. The **only** hard gates are:
1. `spec_ref` present in rule frontmatter (`evals/spec_ref_guard.sh` → exit ≠ 0 if missing)
2. `./gradlew test{Domain}` PASS for the referenced spec item

## How to Use

Read individual rule files for detailed explanations and code examples:

```
practices/rules/persistence-no-n-plus-1.md
practices/rules/transaction-no-self-invocation.md
```

Each rule file contains (per `practices/_template.md`):
- Frontmatter with `spec_ref`, `verification.gradle_task`, `verification.tag`
- Brief explanation of why it matters
- Non-compliant code example
- Compliant code example
- References to upstream snapshot

## Full Advisory Report

Run `./practices/evals/run.sh` to generate a dated report:

```
practices/evals/reports/YYYY-MM-DD.md
```

Report includes per-axis advisory scores and TOTAL line. Scores are metrics, not gates.
