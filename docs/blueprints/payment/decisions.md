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
