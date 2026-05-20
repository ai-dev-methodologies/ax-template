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
    `.../multitenancy/` subpackage and asserts the 16 canonical files exist:
      (1) TenantContext.java
      (2) TenantOwned.java
      (3) TenantBoundaryViolationException.java
      (4) TenantContextMissingException.java
      (5) MultiTenantProblemDetailAdvice.java
      (6) TenantAwareAsyncConfig.java
      (7) TenantContextAwareTaskDecorator.java
      (8) TenantFilterActivationFilter.java
      (9) AuthorizedTenant.java                  ← added dogfood-5
      (10) TenantId.java                         ← added dogfood-5
      (11) AuthorizedTenantInterceptor.java      ← added dogfood-5
      (12) AuditEvent.java                       ← added R4 (GAP-R3-3)
      (13) TenantIterationScheduler.java         ← added R6 (GAP-R3-5)
      (14) TenantAwareSseEmitterRegistry.java    ← added R7 (GAP-NEW-1)
      (15) TenantAwareRedisPubSubBridge.java     ← added R8 (GAP-NEW-2)
      (16) TenantAwareKafkaConsumer.java         ← added R9 (kafka-consumer)
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
└── TenantAwareKafkaConsumer.java               # tenant-scoped Kafka business-event consumer (R9, opt-in)
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
