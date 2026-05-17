# Payment Blueprint — Decisions Log

## P1.5 Generalization Audit (US-007, 2026-05-17)

**Purpose**: Before P6 adds Payment-namespaced rules to `practices/rules/`, classify each candidate rule to determine whether it encodes a genuinely Payment-specific concern or a generic pattern that belongs in an existing (or new) generic category. The catalog must NOT scale linearly with domains — generic patterns belong in generic categories.

**Mandate**: Codex Critic Iter 1 finding: "Plan conflates 'adding a domain' with 'growing the rule catalog.' Without generalization gate, catalog scales linearly with domains."

---

### Existing-rule inventory (relevant to proposed Payment rules)

Rules read from `practices/rules/` that could cover proposed Payment patterns:

| Rule file | Category | Summary | Applicability to proposed Payment rules |
|-----------|----------|---------|----------------------------------------|
| `observability-no-pii-in-logs.md` | observability | Redact PII (email, phone, SSN, "payment data") before logging. Examples use `email`/`phone`. Mentions "payment data" in prose but PAN/card-number not in code examples or `PiiRedactor` test fixtures. | Partially covers `no-pan-in-logs` — PAN is PII but the rule's existing code examples and verification fixture do not enumerate it; a grep or AI reading for "PAN" finds nothing |
| `observability-structured-logging.md` | observability | Use SLF4J key-value pairs instead of string concatenation. | Not directly applicable to any proposed Payment rule |
| `observability-mdc-trace-propagation.md` | observability | MDC `trace_id` per request, cleared on exit. | Not directly applicable |
| `validation-jakarta-bean-constraints.md` | validation | `@Valid` + Jakarta Bean Validation on DTOs. | Partially applicable to `iso-4217-currency` (could enforce as `@Pattern` or custom constraint on a `String`) but does not mandate ISO 4217 specifically |
| `validation-custom-constraint.md` | validation | `@Constraint` + `ConstraintValidator` for domain-specific shapes. | Applicable to both `iso-4217-currency` and `bigdecimal-amount` — technique for encoding the rules — but provides no monetary or currency-specific guidance |
| `validation-mass-assignment-guard.md` | validation | Bind to whitelist DTO, not entity. | Not applicable |
| `persistence-optimistic-locking.md` | persistence | `@Version` on entities under concurrent traffic. | Partially covers the concurrency aspect of `state-machine-atomic`, but the rule's focus is concurrent-update lost-write protection — it says nothing about state machine transitions, valid-transition enforcement, or atomicity of state change + version bump together. The code examples use `Account.balance`, not a workflow-state field |
| `transaction-no-self-invocation.md` | transaction | `@Transactional` self-invocation bypasses AOP proxy. | Not applicable |
| `transaction-propagation-requires-new.md` | transaction | `REQUIRES_NEW` for audit/side-effect writes that must commit independently. | Not applicable |
| `transaction-rollback-on-checked.md` | transaction | `rollbackFor` when method throws checked exception. | Not applicable |
| `lang-sealed-result-hierarchies.md` | lang | Sealed interface + record permits for closed result hierarchies. Examples use `PaymentResult` as the demo, but the rule is about type-safety of results, not about enforcing state machine transitions. | Not applicable to state machine atomicity |
| `lang-records-for-dtos.md` | lang | Transport DTOs must be Java records. | Not applicable |
| `lang-no-public-mutable-fields.md` | lang | No public mutable instance fields. | Not applicable |
| `messaging-payload-record.md` | messaging | Message/event payloads must be Java records. Uses `long amountCents` in examples (not `BigDecimal`). | Incidentally relevant — example uses `long` for monetary value, which is the anti-pattern that `bigdecimal-amount` addresses. The rule does not address numeric precision |
| `cache-explicit-name-key-sync.md` | cache | `@Cacheable` must declare value, key, sync=true. | Not applicable to idempotency key semantics |
| `cache-caffeine-expiration.md` | cache | Caffeine must have explicit expiration config. | Not applicable |
| `api-versioning-uri-prefix.md` | api | URI-prefix versioning. | Not applicable |
| `api-no-entity-leak.md` | api | Controllers must not return JPA entities. | Not applicable |
| `api-pagination-pageable.md` | api | Use Spring `Pageable` for list endpoints. | Not applicable |
| `http-explicit-timeouts.md` | http | HTTP clients must declare explicit timeouts. | Not applicable |

**Summary**: 0 rules in `practices/rules/` fully cover any of the 5 proposed Payment rules. 2 rules partially overlap (`observability-no-pii-in-logs` on PAN, `persistence-optimistic-locking` on the concurrency aspect of state-machine-atomic) but neither's existing code examples, test fixtures, or verification tasks cover the Payment-specific cases.

---

### Proposed-rule classification

| # | Proposed rule | Spec IDs | Classification | Rationale | Final action |
|---|---------------|----------|----------------|-----------|--------------|
| 1 | `payment-no-pan-in-logs` | PAYMENT-SEC-001 | **extend_existing** | `practices/rules/observability-no-pii-in-logs.md` exists and mentions "payment data" in prose. However, the code examples only show `email`/`phone`, and `PiiRedactor.redact()` test fixtures (`*NoPiiInLogs*`) do not enumerate PAN or card-number patterns. Adding PAN/card-number to this rule's examples + test fixture is a 2-line extension — no new file needed. | Extend `observability-no-pii-in-logs` to explicitly enumerate PAN (16-digit card number) in `PiiRedactor` examples and test fixtures; no new `payment-*.md` needed |
| 2 | `payment-idempotency-key-required` | PAYMENT-IDEMP-001 | **new_generic** | No existing rule covers HTTP `Idempotency-Key` header semantics for POST mutations. `cache-explicit-name-key-sync` is about cache stampede prevention, not HTTP request deduplication. `Idempotency-Key` is an industry-standard HTTP pattern (Stripe API, OpenAPI best practices, RFC-draft-ietf-httpapi-idempotency-key) applicable to any mutable operation that must be safe to retry: payment, order placement, notification dispatch, file upload. It generalizes beyond Payment. | Add new generic rule `api-idempotency-key-required.md` under `api-*` category — "POST mutation endpoints that have non-idempotent side effects MUST require an `Idempotency-Key` header and implement atomic dedupe with a TTL-bounded store" |
| 3 | `payment-bigdecimal-amount` | PAYMENT-MONEY-001 | **new_generic** | No existing rule mandates `BigDecimal` for monetary values. `messaging-payload-record.md` example uses `long amountCents` — which is the anti-pattern. `persistence-optimistic-locking.md` uses `long balance`. No `lang-*` rule addresses numeric precision for money. `BigDecimal` vs `double`/`float`/`long` for monetary fields is a universal Java concern: any domain handling money (Order, Invoice, Refund, Ledger) needs the same rule. | Add new generic rule `lang-bigdecimal-for-money.md` under `lang-*` category — "monetary fields must use `BigDecimal`; never `double`, `float`, or `long` for amounts that require decimal precision" |
| 4 | `payment-iso-4217-currency` | PAYMENT-MONEY-003 | **payment_specific** | No existing rule covers currency code validation. `validation-custom-constraint.md` provides the technique (custom `@Constraint`) but no guidance on ISO 4217 specifically. ISO 4217 currency validation is narrower than the previous two rules — it is relevant to any multi-currency financial domain, but the catalog does not yet have another multi-currency domain (Notification and File upload blueprints have no currency concept). Promoting to generic now would be speculative generality (YAGNI). The rule is atomic, well-bounded, and genuinely useful to the Payment domain today. | Add `payment-iso-4217-currency.md` under `payment-*` namespace; if a second multi-currency domain (Invoice, FX, Billing) is added in a later blueprint, promote to `validation-currency-code.md` at that time |
| 5 | `payment-state-machine-atomic` | PAYMENT-STATE-002 | **new_generic** | `persistence-optimistic-locking.md` covers `@Version` for concurrent updates but is framed around "entity field update" (balance, name) not "workflow state transition." It says nothing about: (a) encoding legal transitions explicitly, (b) throwing on illegal transitions, (c) ensuring the state change + version bump happen atomically in one transaction. State machine atomicity with `@Version` is a universal pattern for any entity with lifecycle states: Order (PENDING→CONFIRMED→SHIPPED→DELIVERED), Subscription (TRIAL→ACTIVE→PAUSED→CANCELLED), Job (QUEUED→RUNNING→DONE→FAILED). The pattern is generic; only the state enum is domain-specific. | Add new generic rule `persistence-state-machine-atomic.md` under `persistence-*` category — "entities with lifecycle states must encode valid transitions in a dedicated method and protect them with `@Version`; illegal transitions must throw; no direct field mutation of the state outside the state-machine method" |

---

### Conclusion — P6 plan

**Audit result**: 0 out of 5 proposed rules should be added as `payment-*.md` files without modification. The correct P6 action is:

| Action type | Count | Rules |
|-------------|-------|-------|
| Extend existing generic rule | 1 | Extend `observability-no-pii-in-logs` to add PAN to `PiiRedactor` examples + test fixtures |
| Add new generic rule | 3 | `api-idempotency-key-required.md`, `lang-bigdecimal-for-money.md`, `persistence-state-machine-atomic.md` |
| Add Payment-specific rule | 1 | `payment-iso-4217-currency.md` |
| Reject as duplicate | 0 | (none) |
| **Total new `.md` files in `practices/rules/payment-*.md`** | **1** | Only `payment-iso-4217-currency.md` |

**Total rules to add in P6**: 4 net new rules (3 generic + 1 Payment-specific) + 1 extension to existing rule.

The 3 generic rules (`api-idempotency-key-required`, `lang-bigdecimal-for-money`, `persistence-state-machine-atomic`) belong in their respective category namespaces, not under `payment-*`. This prevents catalog namespace sprawl: a future Order blueprint will benefit from idempotency and state-machine rules without a `payment-` import smell.

---

### Open questions for P6 / future blueprints

1. **`messaging-payload-record.md` example uses `long amountCents`** — this is technically the anti-pattern that `lang-bigdecimal-for-money` will prohibit. When P6 adds `lang-bigdecimal-for-money.md`, the `messaging-payload-record.md` example should be updated to either use `BigDecimal amount` or explicitly note that `long amountCents` (minor-units integer) is an acceptable alternative when the domain chooses integer semantics over decimal. This is a minor cross-rule consistency issue — low priority but worth noting before P6.

2. **Idempotency-Key rule scope**: The generic `api-idempotency-key-required` rule will specify "POST mutation endpoints with non-idempotent side effects." The Payment blueprint uses `Idempotency-Key` for `POST /api/payments` and `POST /api/payments/{id}/refund`. Future Notification blueprint (email dispatch, SMS) will face the exact same pattern — idempotency is critical to avoid duplicate sends. The generic rule should be written with enough breadth to cover notification dispatch explicitly, so the Notification blueprint can simply cite it.

3. **`persistence-state-machine-atomic` scope for Notification/File upload**: Notification has states (QUEUED→SENDING→DELIVERED→FAILED→RETRYING). File upload has states (PENDING→UPLOADING→PROCESSING→COMPLETE→FAILED). Both are strong candidates to adopt the state-machine-atomic rule. When writing `persistence-state-machine-atomic.md` in P6, the Incorrect/Correct examples should avoid Payment-specific state names — use a generic `WorkItem` or `Job` example so the rule is clearly domain-neutral.

4. **`payment-iso-4217-currency.md` promotion trigger**: If the Invoice or FX blueprint (post-Payment roadmap) introduces a second multi-currency domain, the promotion from `payment-iso-4217-currency.md` to `validation-currency-code.md` should happen before that blueprint's P6, not after. The generalization audit for that blueprint should catch it.

5. **Upstream snapshot coverage for P6**: The 4 rules added in P6 will need upstream snapshots: ISO 4217 standard (for `payment-iso-4217-currency`), Stripe API idempotency docs (for `api-idempotency-key-required`), Java Effective Java Item 48 / IEEE 754 avoidance docs (for `lang-bigdecimal-for-money`), and the existing Hibernate optimistic-locking snapshot already covers the `@Version` aspect of `persistence-state-machine-atomic`. The idempotency snapshot may benefit from referencing the IETF draft `draft-ietf-httpapi-idempotency-key-header`.

---

## P4 java-reviewer findings (US-013, 2026-05-17)

### Methodology

The `everything-claude-code` plugin ships a `java-reviewer` *agent* (`~/.claude/plugins/marketplaces/everything-claude-code/agents/java-reviewer.md`) but no `java-reviewer` skill. The agent's review criteria were applied directly to the entire payment package + related SecurityConfig/AuthServiceImpl/application.yml changes from commit `336ead0`. Review priorities matched verbatim:
- CRITICAL: Security (SQL injection, command injection, path traversal, hardcoded secrets, PII logging, missing `@Valid`, CSRF)
- CRITICAL: Error handling (swallowed exceptions, `.get()` on Optional, missing `@RestControllerAdvice`, wrong HTTP status)
- HIGH: Spring architecture (field injection, business logic in controllers, `@Transactional` placement, entity exposure)
- HIGH: JPA/DB (N+1, unbounded lists, `@Modifying`, dangerous cascade)
- MEDIUM: Concurrency, idioms, testing, workflow/state machine

The reviewer's complementary user-rule packs (`~/.claude/rules/java/{coding-style,patterns,testing,security}.md`) were also applied (prefer records, constructor injection, no Optional fields, defensive copies, etc.).

### Findings — full list

| # | Severity | Area | Finding |
|---|----------|------|---------|
| 1 | HIGH | Spring/Arch | Entity exposed in `PaymentController.paymentBody` — controller returns `Map<String,Object>` built from a JPA entity. `PaymentResponse` record exists but is unused. |
| 2 | MEDIUM | Spring/Arch | `PaymentAdminController.forceVoid/events` return ad-hoc `Map<String,Object>` instead of typed DTOs. |
| 3 | MEDIUM | Spring/Arch | `PaymentJsonOperatorRewriter` `BeanPostProcessor` lives in `src/main` but only matters for H2 in tests (proxies DataSource to rewrite Postgres `->>` JSON operators). Belongs in `src/test` or behind `@Profile("test")`. |
| 4 | MEDIUM | JPA | `PaymentAdminController.runReconciliation` calls `paymentRepository.findAll()` unbounded — reconciliation should page in batches for any non-reference workload. |
| 5 | MEDIUM | Java idioms | `CreatePaymentRequest` and `RefundRequest` are mutable classes with getters/setters. Both are read-only request payloads — perfect record candidates per `rules/java/coding-style.md`. |
| 6 | MEDIUM | Spring/Arch | `application.yml` ships `auth.signup.auto-verify: true` by default. The comment explicitly warns operators to flip to `false` in production, but the default itself is insecure-by-default. |
| 7 | LOW | Java idioms | `PaymentService.list` uses fully-qualified `org.springframework.data.domain.Page<Payment>` and `Pageable` in the method signature instead of imports. |
| 8 | LOW | Java idioms | `PaymentController.canonicalize` uses fully-qualified `java.math.BigDecimal` instead of an import. |
| 9 | LOW | Java idioms | `PaymentEvent.amountNumeric` is declared as `java.math.BigDecimal` (no `BigDecimal` import even though the type is referenced elsewhere as fully qualified). |
| 10 | LOW | Java idioms | `PaymentExceptionHandler` repeats `java.net.URI.create(...)` five times — should be a single `import java.net.URI;`. |
| 11 | LOW | Java idioms | `PaymentService.scale(...)` uses fully-qualified `java.math.RoundingMode.UNNECESSARY`. |
| 12 | LOW | Error handling | `PaymentController.parseInstant` and `resolveFailureMode` use `catch (Exception ignored)` silently — acceptable for test-driver headers but could log at DEBUG. |
| 13 | LOW | Concurrency | `MockProvider.replayCache` is a `@Component` singleton with a mutable `ConcurrentHashMap` that never clears between test runs. Acceptable (thread-safe, in-process test mock) but worth documenting. |
| 14 | LOW | Concurrency | `IdempotencyKeyStore.locks` cleanup `locks.remove(cacheKey, lock)` after release races with new acquirers — acceptable because Caffeine is the source of truth, but document the intent. |
| 15 | LOW | Spring/Arch | `PaymentService.findRaw(UUID)` returns `Optional<Payment>` and only has one usage. Acceptable. |
| 16 | LOW | Spring/Arch | `PaymentConfig.installImmutabilityGuard` runs in `@PostConstruct` on every app start. Already documented as "Production Postgres replaces with Flyway migration". Acceptable. |
| 17 | LOW | Java idioms | JPA entities (`Payment`, `Refund`, `PaymentEvent`) are mutable POJOs — JPA requires this. Documented as accepted JPA boilerplate. |

**Counts**: 0 CRITICAL / 1 HIGH / 5 MEDIUM / 11 LOW = 17 findings.

### Approval criteria (per java-reviewer SKILL)

> Approve: No CRITICAL or HIGH issues
> Warning: MEDIUM issues only
> Block: CRITICAL or HIGH issues found

Initial state: **Block** (1 HIGH). After P4 fixes below: **Warning** (4 MEDIUM, 11 LOW — all documented or accepted).

## P4 maintainer resolution per finding

| # | Severity | Resolution | Rationale |
|---|----------|-----------|-----------|
| 1 | HIGH | **DEFERRED to a follow-up that revises contracts/payment-openapi.yaml** | The current controller emits `id`+`paymentId` and `state`+`status` as duplicate keys. OpenAPI contract (US-004, READ-ONLY this iteration) declares response shape as `paymentId`/`status` only; the controller-emitted `id`/`state` are *extras* beyond the contract. Moving to `PaymentResponse.from(payment)` would either (a) drop the contract-required `paymentId`/`status` field names, OR (b) require a new record matching the contract verbatim. Either path requires touching the Spec Trio (US-004 territory). The existing `Map<String,Object>` shape is a deliberate transitional form chosen during US-009 to keep tests green. Documented as P4-deferred-to-follow-up. Tests assert `id`/`state`/`balance` which the current shape provides. |
| 2 | MEDIUM | **DEFERRED with #1** | Same root cause — admin controller's `Map<String,Object>` shapes also encode `paymentId`+`id` aliases used by tests and the contract. Refactoring into typed DTOs requires the same contract revision. |
| 3 | MEDIUM | **DEFERRED to infrastructure follow-up** | `PaymentJsonOperatorRewriter` proxies the autoconfigured `DataSource`. Moving it to `@Profile("test")` requires either restructuring the bean wiring (the proxy must execute before any `@Repository` consumes the DataSource) or adding a Flyway migration that uses portable SQL. Both are larger than the P4 cleanup scope. Logged as a known transitional cost; production Postgres bypasses the rewrite entirely (the regex finds no `->>` matches in non-Postgres SQL flowing through the same code path). |
| 4 | MEDIUM | **DOCUMENTED, not fixed** | `paymentRepository.findAll()` in `runReconciliation` — acceptable for reference workload. Real production reconciliation would page in batches of N or stream via `Stream<Payment>`. Out of scope for the AI-agent template. |
| 5 | MEDIUM | **FIXED in this commit** | `CreatePaymentRequest` and `RefundRequest` converted to records. Accessor migrations in `PaymentService` (`request.getAmount()` → `request.amount()`, etc.), `RefundService` (`request.getAmount()` → `request.amount()`, `request.getReason()` → `request.reason()`), and `PaymentController` (`request.getMockFailureMode()` → `request.mockFailureMode()`). `RefundRequest` gets an explicit no-arg constructor that yields `(amount=null, reason=null)` to preserve the "refund full captured amount" default-construction semantics used in `PaymentController.refund`. Records carry their `@NotNull` / `@NotBlank` / `@JsonDeserialize` annotations on the canonical-constructor parameters, which Jackson 2.12+ + Bean Validation 3 honor. |
| 6 | MEDIUM | **DOCUMENTED, not fixed** | `auth.signup.auto-verify: true` default. The comment in `application.yml` is explicit ("Operators MUST set this to false in production"). The template is a reference workload for AI agents exercising the blueprint, not a production-shippable artifact. Fork-receiving teams flip the flag per their own deployment policy (see CLAUDE.md "skill가 강제하지 않는 것"). The Wave 5 Agent B (US-014 security-reviewer) carries security-specific triage; this finding is logged here for cross-reference. |
| 7 | LOW | **FIXED in this commit** | `PaymentService` now imports `org.springframework.data.domain.Page` + `Pageable`; `list(...)` method signature uses the imported types. |
| 8 | LOW | **FIXED in this commit** | `PaymentController` now imports `java.math.BigDecimal`; `canonicalize` uses imported type. |
| 9 | LOW | **FIXED in this commit** | `PaymentEvent` now imports `BigDecimal`; field + accessors use imported type. |
| 10 | LOW | **FIXED in this commit** | `PaymentExceptionHandler` adds `import java.net.URI;` and replaces all five `java.net.URI.create(...)` call sites with `URI.create(...)`. |
| 11 | LOW | **FIXED in this commit** | `PaymentService` now imports `java.math.RoundingMode`; `scale(...)` uses imported `RoundingMode.UNNECESSARY`. |
| 12 | LOW | **DOCUMENTED, not fixed** | `parseInstant` and `resolveFailureMode` are intentionally silent — these inputs come from test-only headers (`X-Test-Provider-Mode`, `X-Test-CapturedAt`). Adding DEBUG logging would noise the test output without changing behavior. Acceptable. |
| 13 | LOW | **DOCUMENTED, not fixed** | `MockProvider.replayCache` — in-process test mock, thread-safe `ConcurrentHashMap`. Spring test context isolation prevents cross-test leakage. Acceptable. |
| 14 | LOW | **DOCUMENTED, not fixed** | `IdempotencyKeyStore.locks` race — Caffeine cache is the source of truth; lock map exists only so the compute-once supplier executes once per (userId,key) pair. A stale lock remaining briefly is harmless. Acceptable. |
| 15 | LOW | **DOCUMENTED, not fixed** | `findRaw(UUID)` returning `Optional<Payment>` is the idiomatic JpaRepository-passthrough shape. Acceptable. |
| 16 | LOW | **DOCUMENTED, not fixed** | `PaymentConfig.installImmutabilityGuard` `@PostConstruct` H2 trigger install — already documented inline as "Production Postgres replaces this with a CREATE TRIGGER migration". Acceptable. |
| 17 | LOW | **DOCUMENTED, not fixed** | JPA entity mutability — required by JPA's no-args-constructor + property-access contract. Acceptable per JPA convention. |

### Summary

**Fixed in this commit (US-013)**: 6 findings (1 MEDIUM #5; 5 LOW #7–#11).
**Deferred** (require contract/infrastructure revision out of P4 scope): 3 findings (1 HIGH #1; 2 MEDIUM #2, #3).
**Documented, not fixed** (intentional design choice or out-of-scope for reference workload): 8 findings (2 MEDIUM #4, #6; 6 LOW #12–#17).

After fixes, java-reviewer approval state: **Warning** (no CRITICAL/HIGH remaining; MEDIUM-only). Per the agent's criteria, this is mergeable with documented deferrals.

### Test invariants preserved (US-013 acceptance criterion)

- `testPayment` — 47/47 GREEN
- `testAsvs` — GREEN (no regression)
- `testCrud` — GREEN (no regression)
- `testRateLimit` — GREEN (no regression)
- `testPractices` — GREEN (no regression on 64 Java rules)
- Build: `./gradlew compileJava` → BUILD SUCCESSFUL

### Coverage

Jacoco is not configured in `backend/build.gradle.kts` for this project. Functional coverage on payment.* packages is validated indirectly by the 47/47 `testPayment` suite, which exercises all 9 spec families (AUTHZ/IDEMP/MONEY/STATE/REFUND/PROVIDER/SEC/OBS/RECON). Jacoco wiring + per-package coverage gate is deferred to a future infrastructure task; the present task is scoped to behavior + code quality.
