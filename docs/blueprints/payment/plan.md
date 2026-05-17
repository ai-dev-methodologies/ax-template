# Payment Blueprint — Master Plan (Consensus-Approved)

**Status**: Plan approved by ralplan consensus (Planner + Architect + Codex Critic), 2026-05-17.
**Approval verdict**: Codex Critic Iteration 2 = **APPROVE** (7/7 PASS criteria A–G).
**Execution status**: NOT STARTED. P0 is the next phase.
**Estimated effort**: 7.5–10 days.

---

## Architecture Decision Record (ADR)

### Decision

Add Payment as the **4th domain blueprint** in ax-template, using **Option A2**:
Mock provider + Order + Payment + **Full/Partial Refund** + **provider failure matrix** + **reconciliation-lite** + **AUTHZ** + **concurrency tests**.

### Drivers

1. **Vision alignment** — composition kit + 선 순환 시스템 (catalog growth + virtuous cycle). Payment is the 4th domain following the same 5-step METHODOLOGY playbook (auth / CRUD / rate-limit / payment).
2. **Domain real-failure-mode pressure** — payment's actual failure surface is concurrency (double-charge / refund race), float precision, provider timeout limbo, IDOR, reconciliation drift — NOT just "Auth with $$$" superficial security. Plan addresses each with a binding spec item + test + impl + verification command.
3. **Empirical validation strength** — L1 (mechanical gates) + L2 (5-step compliance) + L3 (fork simulation) + L4 (sealed sub-agent acceptance test) + cold-start test. Each layer is binary verifiable by script or sealed rubric.

### Alternatives considered

| Option | Verdict | Invalidation rationale |
|--------|---------|------------------------|
| ~~Original A~~ (Mock + Full Refund only, no concurrency/recon/AUTHZ/provider matrix) | **REJECTED** | Codex Critic Iter 1: "polished demo, not a hard test of the methodology". Doesn't exercise payment's actual failure modes. |
| **A2** (current — Mock + Full/Partial + provider matrix + recon-lite + AUTHZ + concurrency) | **SELECTED** | Real payment failure modes exercised under tight scope. Provider integration deferred. |
| B (Mock + Stripe adapter) | **DEFERRED to next iteration** | Adapter integration adds credentials/compliance/flake risk without strengthening methodology validation — that's already done by A2. AI2-3 second-provider paper exercise covers the abstraction-survival concern. |
| C (Mock + Stripe + Toss + React UI + webhook + reconciliation) | **DEFERRED to subsequent iterations** | Webhook and reconciliation are distinct domains; bundling violates "one domain per blueprint" composition principle. React UI iteration also deferred. |
| Notification-first instead of Payment (Architect steelman Iter 1) | **ASSESSED, NOT BLOCKING** | Notification's weak external standards would stress the evidence system harder, but Payment's selection is defensible for vision priority (Korean enterprise standard stack). Notification scheduled as the next blueprint after Payment ships. |

### Why chosen

A2 is the right balance:
- exercises payment's actual failure surface (concurrency / float / IDOR / recon / provider timeout)
- preserves "tight scope" virtue (no provider integration, no UI iteration, no webhook)
- produces 14 concrete artifacts verifiable by script
- demonstrates methodology generalization from auth/CRUD/rate-limit to a more semantically complex domain
- the sealed L4 prompt + rubric (committed before P6 catalog growth) provides falsifiable acceptance for the vision claim "AI agent works inside the catalog"

### Consequences

**Positive**:
- 4th domain validates methodology survives semantic pressure (state machine, concurrency, money semantics, reconciliation)
- Catalog grows by 3–5 generalized rules (exact count TBD after P1.5 generalization audit — `payment-*` namespace only if non-general)
- `verify/blueprint-completeness.sh` script becomes a reusable standard for "blueprint complete" across all future domains
- Cold-start test (`verify/cold-start-test.sh`) empirically validates session-loss-survivable principle
- METHODOLOGY.md Appendix C formalizes the standard procedure with Gradle + npm/vitest verification primitives — frees future React-side blueprints from Spring-Boot-shaped assumptions

**Negative**:
- 7.5–10 days investment
- Provider integration deferred (PaymentProvider abstraction stress-tested only via AI2-3 paper exercise — real Stripe/Toss adapter risk surfaces only in iteration B)
- React payment UI not in this blueprint (frontend/ remains auth-only on the React reference workload)

### Follow-ups

- **Next blueprint iteration B**: Stripe adapter — implements PaymentProvider against real provider; AI2-3 paper exercise becomes binary test.
- **Subsequent iteration C**: Toss adapter + React payment UI + webhook handling + scheduled reconciliation job.
- **METHODOLOGY.md generalization**: Appendix C is the standard procedure for all subsequent domains (Notification, File upload, Audit log, etc.).
- **Notification as 5th domain**: Architect Iter 1 steelman — Notification stresses the evidence system harder due to weak external standards. Scheduled after Payment ships.

---

## Principles (5, all binary-checkable)

1. **Spec-first (contract-first)** — Spec Trio + 4 hard gates PASS before any Java code. **Check**: `bash practices/evals/spec_ref_guard.sh && substance_guard.sh && time_decay_guard.sh && evidence_guard.sh` exits 0.
2. **TDD with checkpoint commits** — RED → GREEN → REFACTOR with git checkpoint per stage. **Check**: git log shows the three checkpoint commits per behavior on the active branch.
3. **Mechanical enforcement at every layer** — `.githooks/pre-commit` runs hard gates + testPayment + testPractices regression on every commit touching payment. **Check**: hook configured + commits proven via pre-commit transcript.
4. **Composition kit growth — binary** — `bash verify/blueprint-completeness.sh payment` exits 0 (script written in P0.7, lists 14 artifacts + verifies each).
5. **Session-loss survivable — binary** — `bash verify/cold-start-test.sh payment` exits 0 (cold-start agent with only `docs/blueprints/payment/`, `specs/payment-l0.yaml`, `blueprints/payment-manifest.yaml`, and `practices/AGENTS.md` reads the plan and runs `./gradlew testPayment` correctly + identifies the next failing item).

---

## Spec Trio Outline

### `specs/payment-l0.yaml` — 7 families, ~24 items

| Family | Items | Description |
|--------|-------|-------------|
| AUTHZ | 4 | auth required, refund authority, IDOR cross-user denial, admin override audited |
| IDEMP | 3 | Idempotency-Key required, 24h replay returns original, atomic dedupe under concurrency |
| MONEY | 3 | BigDecimal in Java, minor-units-or-decimal-string in JSON (reject float), ISO 4217 + per-currency scale |
| STATE | 3 | state machine CREATED→AUTHORIZED→CAPTURED\|VOIDED→REFUNDED\|PARTIAL_REFUNDED\|UNKNOWN, transitions atomic with @Version, illegal transition rejected |
| REFUND | 3 | 30-day window (manifest configurable), partial refund supported with sum(refunds) ≤ payment.amount invariant, refund-of-refund denied |
| PROVIDER | 6 | matrix: timeout→UNKNOWN / 5xx / 4xx-decline / malformed-response / network-reset / idempotency-replay |
| SEC | 3 | no PAN in logs/db/error-responses (@JsonIgnore + toString override + LogCaptor test), hardcoded secrets scan, TLS-only |
| OBS | 2 | metrics counters (attempted/succeeded/failed/refunded/recon_drift_detected), MDC propagation (payment_id + idempotency_key + correlation_id) |
| RECON | 2 | immutable event ledger with hash chain, invariant test |

### `contracts/payment-openapi.yaml` — 5 endpoints

- `POST /api/payments` (create, requires Idempotency-Key header)
- `GET /api/payments/{id}` (status, owned-by-caller only)
- `POST /api/payments/{id}/refund` (full or partial)
- `GET /api/payments` (list, paginated, owned-by-caller)
- `POST /api/payments/{id}/void` (void unauthorized payment)

Schema requirements (Codex C9):
- `amount`: integer minor units (KRW=원, USD=cents) OR explicit decimal string — float JSON numbers REJECTED with 400 + RFC 7807 detail
- `currency`: ISO 4217 enum
- `Idempotency-Key` header: required for POST mutations, format constrained
- Error responses: RFC 7807 ProblemDetail with stable problem-type URIs

### `blueprints/payment-manifest.yaml`

```yaml
pci_dss:
  saq_tier: SAQ-A          # tokenization out-of-scope; no PAN storage
  pan_storage: NONE
  trace:
    PAYMENT-SEC-001: PCI-DSS-3.4   # PAN protection
    PAYMENT-SEC-002: PCI-DSS-6.5   # secure coding
    PAYMENT-SEC-003: PCI-DSS-4.1   # transmission encryption

provider:
  type: mock
  failure_modes:                   # AI2-1 enumerated
    - timeout
    - http_5xx
    - http_4xx_decline
    - malformed_response
    - network_reset
    - idempotency_replay
  timeout_ms: 5000
  retry: { max: 3, backoff: exponential }

idempotency:
  window_hours: 24
  store: caffeine                  # iteration B: postgres

currency:
  default: KRW
  allowed: [KRW, USD]
  scales: { KRW: 0, USD: 2 }

amounts:
  type: BigDecimal
  json_format: minor_units_or_decimal_string  # reject float JSON
  max: 100000000

refund:
  window_hours: 720                # 30 days
  partial_supported: true
  refund_of_refund: forbidden

state_machine:
  states: [CREATED, AUTHORIZED, CAPTURED, VOIDED, REFUNDED, PARTIAL_REFUNDED, UNKNOWN]
  legal_transitions: [...]         # explicit table

observability:
  pan_redaction: required
  metrics: [payment_attempted_total, payment_succeeded_total, payment_failed_total, refund_processed_total, recon_drift_detected_total]
  mdc: [payment_id, idempotency_key, correlation_id]

ledger:
  table: payment_events
  schema: "(event_id PK, payment_id FK, type ENUM, occurred_at, payload_hash CHAR(64), prev_hash CHAR(64))"
  append_only: true
  hash_chain: true

reconciliation:
  invariant: "sum(captured.amount from ledger) - sum(refunded.amount from ledger) = Payment.balance (stored)"
  derivation: "ledger is source of truth; stored Payment.balance MUST match ledger-derived value; ReconciliationInvariantTest detects divergence"
```

---

## Pre-mortem — 7 scenarios with risk rows

| # | Failure mode | Spec | Test class / method | Impl path | Expected RED | Verification command |
|---|--------------|------|---------------------|-----------|-------------|---------------------|
| 1 | Double-charge under retry storm | PAYMENT-IDEMP-001 | `IdempotencyConcurrencyTest#sameKeyConcurrent5x_onlyOneCharge` | `IdempotencyKeyStore` (Caffeine TTL=24h, atomic `putIfAbsent`) | 5 charges created, test asserts 1 | `./gradlew testPayment --tests "*IdempotencyConcurrency*"` |
| 2 | Refund concurrency wrong amount | PAYMENT-REFUND-002 | `RefundConcurrencyTest#twoPartialRefundsSimultaneous` | `RefundService` with `@Version` optimistic lock on Payment | Race allows over-refund | `./gradlew testPayment --tests "*RefundConcurrency*"` |
| 3 | Float/rounding via JSON | PAYMENT-MONEY-002 | `MoneyDeserializationTest#floatJsonNumberRejected` | Custom Jackson deserializer rejects `JsonToken.VALUE_NUMBER_FLOAT` | Float JSON accepted, precision loss | `./gradlew testPayment --tests "*MoneyDeserialization*"` |
| 4 | Unauthorized refund (IDOR) | PAYMENT-AUTHZ-002 | `AuthzTest#refundByDifferentUser403` | `@PreAuthorize("@paymentSecurity.canRefund(#paymentId, authentication)")` | Any authenticated user can refund | `./gradlew testPayment --tests "*Authz*"` |
| 5 | Provider timeout limbo state | PAYMENT-PROVIDER-001 | `ProviderTimeoutTest#timeoutResultsInUnknownState` + `ReconciliationJobTest#resolvesUnknownAfterRetry` | Payment state machine has `UNKNOWN`; `ReconciliationJob` periodic | Timeout marks payment FAILED prematurely | `./gradlew testPayment --tests "*ProviderTimeout*" "*Reconciliation*"` |
| 6 | PAN leakage in exception path | PAYMENT-SEC-001 | `PanRedactionTest#exceptionPathDoesNotLeakPan` | `@JsonIgnore` + `toString()` override + `PaymentExceptionHandler` redaction | Stack trace contains PAN | `./gradlew testPayment --tests "*PanRedaction*"` |
| 7 | Reconciliation drift | PAYMENT-RECON-001 | `ReconciliationInvariantTest#ledgerSumEqualsBalance` | `PaymentEventLedger` (append-only, hash-chained), invariant query | State machine changes without ledger entry | `./gradlew testPayment --tests "*ReconciliationInvariant*"` |

---

## Test plan (per layer)

| Layer | Coverage | Frameworks |
|-------|----------|-----------|
| **Unit** | `IdempotencyKeyStore`, `MoneyDeserializer`, `StateMachine.transition()` legal/illegal, `RefundCalculator` (partial sums), `PaymentEventLedger` (append + hash-chain) | JUnit 5 + Mockito |
| **Integration** | `@SpringBootTest` + H2 + mock provider; full HTTP flow via RestAssured; AUTHZ negative tests; provider failure matrix (6 modes) | @SpringBootTest + RestAssured @Tag("PAYMENT") |
| **Concurrency** | `@RepeatedTest(20)` + `CountDownLatch` for idempotency race + refund race | JUnit 5 |
| **Contract** | OpenAPI schema validation; request validator rejects float JSON | `npx @apidevtools/swagger-cli validate` |
| **Observability** | assertion-level: `assertThat(logCaptor.getInfoLogs()).noneMatch(panFixture)`, `meterRegistry.counter("payment_attempted_total").count() == N` | LogCaptor + Micrometer SimpleMeterRegistry |
| **Security** | `everything-claude-code:security-reviewer` Skill output (primary authority); supplemental grep automation as cheap check | Skill output + grep |
| **Reconciliation** | Invariant query: ledger-derived `sum(captured)-sum(refunded)` MUST equal stored `Payment.balance` | JdbcTemplate query in test |
| **Regression** | testAsvs + testCrud + testRateLimit + testPractices ALL GREEN | existing Gradle tasks |

---

## Phase plan (23 phases)

```
P0     docs/blueprints/payment/ + plan.md + memory status entry + progress.md (this file already exists from P0)
P0.5   ★ L4 prompt + rubric SEALED — git commit before P6 catalog growth
       Rubric items include AI2-3: "PaymentProvider interface survives Stripe PaymentIntent + Toss V2 mapping without breaking changes"
P0.7   ★ verify/blueprint-completeness.sh script — argument: blueprint name; checks 14 artifacts present + commands exit 0
P0.9   ★ verify/cold-start-test.sh script — spawns context-0 agent with only docs/specs/manifest/AGENTS.md, validates it can run testPayment + identify next failing item

P1     Spec Trio — 7 families / ~24 items
       specs/payment-l0.yaml, contracts/payment-openapi.yaml, blueprints/payment-manifest.yaml
P1.25  ★ PCI-DSS SAQ scope declaration in manifest + PAYMENT-SEC-* → PCI ID mapping
P1.3   ★ L4 pre-registration commit ("sealed: L4 prompt + rubric for Payment blueprint")
P1.5   ★ Generalization Audit per proposed rule
       Each candidate Payment rule classified: new_generic / extend_existing / payment_specific / reject_duplicate
       Compare against existing observability-no-pii-in-logs, transaction rules, validation rules, RestAssured rule

P2     /tdd-workflow RED — 30-40 @Tag("PAYMENT") tests across all 24 spec items
       testPayment Gradle task registered in backend/build.gradle.kts
       ALL RED confirmed
       Checkpoint commit: "test: payment reproducer (RED)"

P3.0   /tdd-workflow GREEN baseline — Spring Boot minimal impl
       Entity (Payment, Refund, OrderRef, PaymentEvent), Repositories (JPA), Service layer, Controller (5 endpoints), MockProvider
       Iterate: code → testPayment → fix
       Checkpoint commit: "feat(payment): baseline impl (GREEN)"

P3.3   Provider failure matrix tests
       6 dedicated tests against MockProvider failure modes (timeout / 5xx / 4xx / malformed / network-reset / idempotency-replay)
       Checkpoint commit: "feat(payment): provider failure matrix"

P3.6   Concurrency tests
       IdempotencyConcurrencyTest + RefundConcurrencyTest with @RepeatedTest + CountDownLatch
       Impl: @Version optimistic lock on Payment; atomic putIfAbsent in IdempotencyKeyStore
       Checkpoint commit: "feat(payment): concurrency safety"

P3.9   Reconciliation invariant
       payment_events table (append-only, hash-chained)
       ReconciliationInvariantTest queries ledger, asserts ledger-derived balance == stored Payment.balance
       Checkpoint commit: "feat(payment): reconciliation ledger + invariant"

P4     /tdd-workflow REFACTOR + everything-claude-code:java-reviewer Skill
       64 Java practices rules applied (testPractices regression GREEN)
       Coverage check (target 80%+)
       Checkpoint commit: "refactor(payment): clean up after GREEN"
       java-reviewer output → docs/blueprints/payment/decisions.md

P5     everything-claude-code:security-reviewer Skill call (primary security authority)
       PCI checklist trace: PAYMENT-SEC-* mapped to PCI-DSS requirements
       Supplemental: `grep -rn "creditCard\|panNumber\|cardNumber" backend/src/main/java/` returns 0 hits
       Findings fix + commit: "fix(payment): security-review findings"
       security-reviewer output → docs/blueprints/payment/security-review.md

P6     Catalog growth — Payment-specific Java rules (count determined by P1.5 audit, likely 3-5)
       practices/rules/payment-*.md OR generic rules (e.g., observability-no-pii-in-logs extension)
       practices/upstream/ snapshot additions (Stripe API / PCI-DSS / ISO 4217 / RFC 7807)
       practices/generate_agents.sh → AGENTS.md auto-regen
       Checkpoint commit: "feat(practices): payment-derived rules + upstream snapshots"

P7     /verification-loop full sweep (6 phases)
       Build / Types / Lint / Tests / Security / Diff — see Command Matrix below
P7.5   ★ verify/blueprint-completeness.sh payment → exit 0 (P4 binary check)
P7.7   ★ verify/cold-start-test.sh payment → exit 0 (P5 binary check)
       verification-log.md append all results

P8     L3 fork simulation
       git clone repo → /tmp/ax-payment-test → ./gradlew test + practices/evals/run.sh + practices-react/evals/run.sh ALL PASS
       Result → docs/blueprints/payment/acceptance/l3-fork-simulation.md

P9     L4 sealed sub-agent acceptance test
       Agent({subagent_type: "general-purpose", prompt: <sealed prompt from P0.5>})
       Sub-agent task: add partial-refund-of-partial-refund feature using only catalog + AGENTS.md
       Evaluate against sealed rubric (committed at P1.3)
       Includes AI2-3 paper exercise: "PaymentProvider interface survives Stripe PaymentIntent + Toss V2 mapping"
       Result → docs/blueprints/payment/acceptance/l4-subagent-test.md

P10    METHODOLOGY.md Appendix C — "Adding a New Domain (standard procedure)"
       Verification primitive: `<framework-native test runner with domain filter>` — Gradle (Spring) AND npm/vitest (React) examples
       12-step standardized procedure derived from Payment work
       Checkpoint commit: "docs(methodology): Appendix C — standard procedure"

P11    Push + memory finalize + /save-session checkpoint
       memory payment_blueprint_status.md → "COMPLETE — <date>"
       MEMORY.md index updated
```

---

## Command matrix

| Phase / Gate | Command | Expected exit |
|--------------|---------|---------------|
| RED gate | `cd backend && ./gradlew testPayment` | non-zero (tests fail) |
| GREEN gate | `cd backend && ./gradlew testPayment` | 0 |
| Concurrency gate | `cd backend && ./gradlew testPayment --tests "*Concurrency*"` | 0 |
| Reconciliation gate | `cd backend && ./gradlew testPayment --tests "*ReconciliationInvariant*"` | 0 |
| Regression gate | `cd backend && ./gradlew testAsvs testCrud testRateLimit testPractices` | 0 |
| Contract gate | `npx @apidevtools/swagger-cli validate contracts/payment-openapi.yaml` | 0 |
| Java hard gates | `bash practices/evals/spec_ref_guard.sh && substance_guard.sh && time_decay_guard.sh && evidence_guard.sh` | 0 |
| React hard gates | `bash practices-react/evals/run.sh` | 0 |
| Blueprint complete | `bash verify/blueprint-completeness.sh payment` | 0 |
| Cold-start | `bash verify/cold-start-test.sh payment` | 0 |
| Security automation (supplemental — NOT authoritative) | `grep -rn "creditCard\|panNumber\|cardNumber" backend/src/main/java/ \| grep -v test \| wc -l` | 0 hits |
| Security authority | `everything-claude-code:security-reviewer` Skill — PCI checklist | rubric PASS |
| L4 sealed test | manual review of sub-agent output vs P0.5 sealed rubric | rubric PASS |

---

## 14-artifact completion checklist

| # | Artifact | Location | Verification |
|---|----------|----------|-------------|
| 1 | Compliance spec | `specs/payment-l0.yaml` | spec_ref guard PASS |
| 2 | API contract | `contracts/payment-openapi.yaml` | swagger-cli validate exit 0 |
| 3 | Policy manifest | `blueprints/payment-manifest.yaml` | YAML parse + required keys (pci_dss, provider, idempotency, currency, amounts, refund, state_machine, observability, ledger, reconciliation) |
| 4 | @Tag tests | `backend/src/test/.../payment/*.java` | `./gradlew testPayment` PASS + 80%+ coverage |
| 5 | Spring Boot impl | `backend/src/main/.../payment/` | testPayment + java-reviewer + security-reviewer PASS |
| 6 | Gradle task | `backend/build.gradle.kts` — `testPayment` task registered | `./gradlew tasks` lists it |
| 7 | Payment-derived rules | `practices/rules/payment-*.md` OR generic rule extensions (3–5 after P1.5 audit) | 4 hard gates PASS |
| 8 | Upstream snapshots | `practices/upstream/{pci-dss,stripe-api,iso-4217,rfc-7807}.snapshot.md` | time_decay PASS |
| 9 | AGENTS.md regen | auto-regen via `practices/generate_agents.sh` | sha256 sentinel match |
| 10 | Blueprint docs | `docs/blueprints/payment/{plan,progress,decisions,security-review,verification-log}.md` + `acceptance/` | files exist |
| 11 | Methodology appendix | `METHODOLOGY.md` Appendix C | section exists |
| 12 | `verify/blueprint-completeness.sh` script | `verify/blueprint-completeness.sh` | `bash verify/blueprint-completeness.sh payment` exit 0 |
| 13 | Sealed L4 prompt + rubric | `docs/blueprints/payment/acceptance/l4-sealed-prompt.md` + `l4-sealed-rubric.md` | committed at P1.3 (before P6) |
| 14 | `verify/cold-start-test.sh` script | `verify/cold-start-test.sh` | `bash verify/cold-start-test.sh payment` exit 0 |

---

## Estimated timeline

| Phase group | Days |
|-------------|------|
| P0–P0.9 (docs dir + verification scripts + L4 sealing) | 1 |
| P1–P1.5 (Spec Trio + PCI scope + L4 commit + generalization audit) | 1 |
| P2 (RED 30–40 tests) | 1 |
| P3.0–P3.9 (GREEN baseline + provider matrix + concurrency + recon) | 2–3 |
| P4–P5 (REFACTOR + java-reviewer + security-reviewer + fixes) | 1 |
| P6 (Payment-derived rules + snapshots, post-audit) | 0.5–1 |
| P7–P7.7 (verification-loop + blueprint-completeness + cold-start) | 0.5 |
| P8 (L3 fork sim) | 0.25 |
| P9 (L4 sealed test + analysis) | 0.5 |
| P10 (METHODOLOGY Appendix C) | 0.25 |
| P11 (push + memory finalize) | 0.1 |
| **Total** | **7.5–10 days** |

---

## Iteration trail

| Iteration | Planner output | Architect verdict | Critic verdict |
|-----------|----------------|-------------------|----------------|
| 1 | Option A: Mock + Full Refund + Spring only / 14 items / 11 phases | ITERATE (5 amendments) | ITERATE (10-item change set, "polished demo not hard test") |
| 2 | Option A2: + partial refund + provider matrix + concurrency + recon + AUTHZ / 24 items / 23 phases (including AI2-1..4 polish) | ITERATE (4 narrow — provider matrix dimensions, ledger schema, second-provider rubric item, phase numbering) | **APPROVE** ✅ |

Codex Iter 2 final nits (non-blocking, applied to this plan):
1. Phase count normalized: 23 phases (P0..P11 with decimals)
2. Security grep marked **supplemental** — primary authority = PAYMENT-SEC tests + security-reviewer Skill
3. Ledger `net_balance` clarified — **derived from ledger** (source of truth); stored `Payment.balance` MUST match ledger-derived value; `ReconciliationInvariantTest` detects divergence

---

## Resumability — if session is lost

Read in order:
1. This file (`docs/blueprints/payment/plan.md`) — full plan
2. `docs/blueprints/payment/progress.md` — append-only log of completed phases (created in P0)
3. `~/.claude/projects/<slug>/memory/payment_blueprint_status.md` — current status
4. `git log --oneline -20` — checkpoint commits indicate exact phase
5. `./gradlew tasks --all | grep -i payment` — confirms whether testPayment is registered
6. Resume from the next un-checkpointed phase
