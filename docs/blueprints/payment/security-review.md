# Payment Blueprint — P5 Security Review

**Story:** US-014 (P5 — security-reviewer primary security authority)
**Reviewer Skill:** `everything-claude-code:security-review`
**Reviewed package:** `backend/src/main/java/com/ax/template/authblueprint/payment/`
**Spec context:** `specs/payment-l0.yaml` (PAYMENT-SEC-001..003 + AUTHZ-001..004)
**Policy context:** `blueprints/payment-manifest.yaml#pci_dss` (SAQ-A scope declaration)
**Reviewed against base commit:** `336ead0` (US-009 P3.0 GREEN baseline)
**Review date:** 2026-05-17

---

## 1. Executive Summary

Walked the security-review Skill's 10-section checklist (Secrets / Input
Validation / SQL Injection / AuthN+AuthZ / XSS / CSRF / Rate Limiting /
Sensitive Data Exposure / Blockchain / Dependencies) against the Payment
baseline impl. The implementation is sound — PCI-DSS SAQ-A scope is honored
(no PAN field anywhere in the package; opaque tokenized references only).
Found one HIGH finding (admin reconciliation endpoint left permit-all for
test convenience), and three MEDIUM findings on defense-in-depth measures.

**Totals:** 1 CRITICAL · 1 HIGH · 4 MEDIUM · 3 LOW
- **CRITICAL:** 0 found
- **HIGH:** 1 FIXED in this commit
- **MEDIUM:** 4 — 2 FIXED in this commit; 2 DEFERRED with rationale
- **LOW:** 3 — DOCUMENTED for future tightening, not blocking

**Net result:** PCI-DSS SAQ-A scope verified PASS on all 3 PAYMENT-SEC items.
Supplemental hardcoded-secrets grep returns 0 hits.

---

## 2. Findings (security-review Skill output, triaged)

### CRITICAL — 0 findings

None.

### HIGH-1 — Admin reconciliation endpoint was permit-all (PAYMENT-AUTHZ-004 violation)

**Severity:** HIGH (PCI-DSS audit-log integrity)
**Skill section:** §4 Authentication & Authorization
**Location:** `backend/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java:36` (before fix)

#### Vulnerability

```java
// BEFORE:
.requestMatchers(HttpMethod.POST, "/api/admin/reconciliation/run").permitAll()
.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
```

The `/api/admin/reconciliation/run` endpoint was explicitly carved out as
`permitAll()` to make the PaymentObservabilityTest probe simpler. The endpoint:

1. Appends `RECONCILIATION_DRIFT` events to the append-only `payment_events`
   ledger on every invocation.
2. Increments the `recon_drift_detected_total` Micrometer counter.

Allowing unauthenticated access lets an adversary:
- Pollute the audit ledger with arbitrary RECONCILIATION_DRIFT events (no
  attribution, no actor ID — there is no authenticated principal).
- Poison the observability metric stream (DDoS on the alerting pipeline).

Violates PAYMENT-AUTHZ-004 (admin override actions are audited with actor ID).
The recon endpoint emits audit events but has no authenticated actor, so
audit trail attribution is broken.

#### Fix (applied)

Removed the carve-out. All `/api/admin/**` paths — including the recon
endpoint — now require `ROLE_ADMIN`. PaymentObservabilityTest's
`metricsCounters_reconDriftDetectedTotalIncremented` was updated to obtain
an admin token (`obtainToken("recon-admin@obs001.test", "ADMIN")`) before
posting to the endpoint, consistent with the existing
`PaymentAuthzTest#adminOverrideAudited` pattern.

#### Verification

- `./gradlew testPayment` — 47/47 GREEN after fix
- `./gradlew testAsvs testCrud testRateLimit testPractices` — ALL GREEN
- File `SecurityConfig.java` line range that changed: 30-39

#### Status

**FIXED** in this commit.

---

### MEDIUM-1 — `Payment.paymentMethodToken` lacked `@JsonIgnore` (defense-in-depth)

**Severity:** MEDIUM (defense-in-depth — no exploitable leak today)
**Skill section:** §8 Sensitive Data Exposure
**Location:** `backend/src/main/java/com/ax/template/authblueprint/payment/Payment.java:78`

#### Observation

The `paymentMethodToken` field on the Payment entity stores the opaque,
tokenized payment method reference (per SAQ-A — no raw PAN). The current
`PaymentController#paymentBody()` carefully excludes this field from API
responses (verified — `paymentBody` only puts `id`, `orderId`, `amount`,
`capturedAmount`, `balance`, `currency`, `status`, `state`, `declineReason`,
`createdAt`, `updatedAt`).

However, if a future controller or admin endpoint ever returns the Payment
entity directly (e.g., `return paymentRepository.findById(id)`), Jackson
would serialize the token by default. This is a defense-in-depth gap.

#### Fix (applied)

Added `@JsonIgnore` to the field. Even if a future regression returns the
Payment entity directly, Jackson will not serialize the token. The DB
storage is unaffected (JPA `@Column` is separate from Jackson `@JsonIgnore`).

#### Verification

- Field `Payment.paymentMethodToken` annotated `@JsonIgnore` (line ~80)
- Import added: `com.fasterxml.jackson.annotation.JsonIgnore`
- All payment tests continue to GREEN (no test asserted the field was
  serialized — confirmed by reading `paymentBody()` callers)

#### Status

**FIXED** in this commit.

---

### MEDIUM-2 — `HttpMessageNotReadableException` detail leaked Jackson internals

**Severity:** MEDIUM (information disclosure — internal package names + field paths)
**Skill section:** §8 Sensitive Data Exposure (error message hygiene)
**Location:** `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentExceptionHandler.java:69-80` (before fix)

#### Observation

Jackson exception messages include reference-chain suffixes like:

```
... (through reference chain: com.ax.template.authblueprint.payment.CreatePaymentRequest["amount"])
```

These suffixes leak:
- Internal package structure (`com.ax.template.authblueprint.payment`)
- Internal DTO class names (`CreatePaymentRequest`)
- Internal field names (`amount`)

While not directly exploitable, this gives an attacker recon information
about server-side type structure and increases reconnaissance signal.

The existing implementation also passed `at [Source: ...]` location markers
which include byte offsets from the request body.

#### Fix (applied)

Added `sanitizeJacksonMessage()` helper that strips `(through reference
chain ...)` and `at [Source: ...]` suffixes while preserving the leading
human-readable message. MoneyDeserializer's intentional float-rejection
message is preserved verbatim (verified — PaymentMoneyTest's
`jsonFormat_floatJson_rejectedWith400` still asserts and passes against
the text containing `"float"`, `"decimal string"`, `"minor unit"`, or
`"integer"`).

#### Verification

- File `PaymentExceptionHandler.java` — added `sanitizeJacksonMessage`
  private static method (~17 LoC)
- `./gradlew testPayment` GREEN (47/47) — float-rejection assertion still passes

#### Status

**FIXED** in this commit.

---

### MEDIUM-3 — Rate limiting not bound to /api/payments/**

**Severity:** MEDIUM
**Skill section:** §7 Rate Limiting
**Location:** `application.yml#ratelimit` (defined for /api/ratelimit/** only)

#### Observation

The auth blueprint includes a rate-limiter (`ratelimit:` config in
`application.yml`), but it is only bound to `/api/ratelimit/**` for the
RateLimit blueprint demo. Payment creation endpoints `/api/payments` and
mutation endpoints `/api/payments/{id}/refund|void|capture|authorize` are
NOT subject to rate limiting.

Payment-specific rate limiting matters for:
- Brute-force attack on Idempotency-Key collision (low practical risk —
  store is keyed by `(userId, key)`).
- Resource exhaustion via repeated POST `/api/payments` (database growth).
- Refund-flood attacks if an attacker compromises a user account.

#### Decision

**DEFERRED.** Rate limiting on payment endpoints is a cross-blueprint
concern that belongs to the existing rate-limiter system. Adding it would
require either:
1. Extending the RateLimit blueprint to declare a new policy for
   `/api/payments/**` (requires Spec Trio update — out of P5 scope).
2. Inlining a payment-specific rate limiter (creates a single-use abstraction
   — violates Karpathy Simplicity-First guideline).

**Follow-up task:** Create a US-XXX in a future wave to bind the existing
rate-limiter to `/api/payments/**` via a new `policy` entry in
`blueprints/ratelimit-manifest.yaml`. This is a configuration-only change
once the manifest supports per-path policies.

#### Status

**DEFERRED** with rationale + follow-up task documented.

---

### MEDIUM-4 — `PaymentAdminController` lacks `@PreAuthorize` defense-in-depth

**Severity:** MEDIUM (low — URL pattern already enforces ROLE_ADMIN after HIGH-1 fix)
**Skill section:** §4 Authentication & Authorization
**Location:** `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentAdminController.java` (all methods)

#### Observation

After fixing HIGH-1, all `/api/admin/**` paths require `ROLE_ADMIN` via
SecurityConfig's URL-pattern matcher. However, method-level
`@PreAuthorize("hasAuthority('ROLE_ADMIN')")` annotations are absent on
`forceVoid`, `events`, and `runReconciliation`.

Defense-in-depth rationale: if a future SecurityConfig refactor accidentally
removes or weakens the `/api/admin/**` matcher, method-level guards would
still hold. `@EnableMethodSecurity` is already declared on SecurityConfig,
so `@PreAuthorize` is enforceable.

#### Decision

**DEFERRED — DOCUMENTED.** The URL-pattern guard is sufficient today
(HIGH-1 is FIXED). Adding `@PreAuthorize` is genuine defense-in-depth but
adds annotation noise that does not change behavior. Prefer to add
`@PreAuthorize` as part of US-013 (java-reviewer / refactor) if Agent A
deems it consistent with the project's style.

#### Status

**DEFERRED** — documented as low-priority hardening for future iteration.

---

### LOW-1 — `PaymentMdcFilter` injects placeholder UUIDs into MDC

**Severity:** LOW (operational noise, not security)
**Skill section:** §8 Sensitive Data Exposure
**Location:** `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentMdcFilter.java:62-65`

#### Observation

```java
MDC.put(MDC_IDEMPOTENCY_KEY,
    (idemKey == null || idemKey.isBlank()) ? ("no-key-" + UUID.randomUUID()) : idemKey);
// ...
String paymentId = m.find() ? m.group(1) : ("pending-" + UUID.randomUUID());
MDC.put(MDC_PAYMENT_ID, paymentId);
```

Placeholder UUID format like `no-key-<uuid>` and `pending-<uuid>` is
operationally noisy but does not leak sensitive data. Log analysis systems
may treat these as unique keys (cardinality explosion in some indexes).

#### Decision

**DOCUMENTED.** Not a security issue. Operability concern only. A future
refactor could populate MDC keys to empty string instead of placeholder
UUIDs; this is a tradeoff between "always-present MDC keys" (current
choice, required by some PaymentObservabilityTest assertions) and "absent
MDC keys" (operationally cleaner).

#### Status

**DOCUMENTED.**

---

### LOW-2 — OAuth dummy secrets in application.yml

**Severity:** LOW (out of payment package scope)
**Skill section:** §1 Secrets Management
**Location:** `backend/src/main/resources/application.yml:20-45`

#### Observation

```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID:dummy-google-id}
  client-secret: ${GOOGLE_CLIENT_SECRET:dummy-google-secret}
```

Default-fallback `dummy-google-secret` etc. used to satisfy Spring's
mandatory OAuth client property validation in test environments. Not in
payment package scope and explicitly documented in the auth blueprint.

#### Decision

**DOCUMENTED.** Out of payment package scope. Auth blueprint handles this.

#### Status

**DOCUMENTED.**

---

### LOW-3 — `PaymentJsonOperatorRewriter` is test-only DataSource proxy

**Severity:** LOW (test infrastructure only — no production execution path)
**Skill section:** §3 SQL Injection Prevention
**Location:** `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentJsonOperatorRewriter.java`

#### Observation

The rewriter rewrites SQL strings via regex (`payload->>'key'` →
`REGEXP_SUBSTR(...)` for H2 compatibility). This pattern would be a SQL
injection concern if user-controllable strings were ever concatenated into
the rewritten SQL. Reviewed all callers — the rewriter only matches keys
from native JPA queries whose SQL is fixed at compile time, never derived
from user input.

#### Decision

**DOCUMENTED.** Safe by construction. Production deployments use Postgres
directly and do not exercise this rewriter (only matches H2 connections).

The class header comment could be strengthened to declare TEST-ONLY use,
but the comment already says "production deployments use PostgreSQL which
parses natively; this rewriter exists only so the reference workload can
run the reconciliation tests against H2 in CI."

#### Status

**DOCUMENTED.**

---

## 3. Skill Section Pass/Fail Summary

| § | Section | Verdict | Notes |
|---|---------|---------|-------|
| 1 | Secrets Management | PASS | No hardcoded secrets in payment package; supplemental grep = 0 hits |
| 2 | Input Validation | PASS | `@NotNull`/`@NotBlank` + MoneyDeserializer + currency whitelist + scale check |
| 3 | SQL Injection | PASS | JPA parameterized queries; rewriter test-only and safe |
| 4 | AuthN + AuthZ | PASS (after HIGH-1 fix) | JWT via Spring resource server; IDOR-safe ownership filter; admin endpoints behind ROLE_ADMIN |
| 5 | XSS | PASS | JSON-only API; no HTML rendering; ProblemDetail Jackson-escaped; MEDIUM-2 fixed |
| 6 | CSRF | PASS | Stateless JWT + CSRF disabled for /api/** is industry-standard |
| 7 | Rate Limiting | DEFERRED | MEDIUM-3 — bind existing rate limiter to /api/payments/** in follow-up |
| 8 | Sensitive Data Exposure | PASS | No PAN in logs/DB/responses; tokens hidden; MEDIUM-1 + MEDIUM-2 fixed |
| 9 | Blockchain | N/A | Not applicable to payment blueprint |
| 10 | Dependencies | OUT-OF-SCOPE | Dependency audit is a separate workflow |

---

## 4. PCI-DSS Trace (manual verification by maintainer)

| Spec ID | PCI-DSS Requirement | Verification Evidence | Status |
|---------|---------------------|----------------------|--------|
| PAYMENT-SEC-001 | 3.4 — Render PAN unreadable | (a) `PaymentSecurityTest#panRedaction_*` 3 methods GREEN (testPayment passes 47/47). (b) DB schema audit — no `pan` / `card_number` / `cvv` column in `payments` or `payment_events` tables (Payment.java + PaymentEvent.java verified). (c) `paymentMethodToken` is the only token-bearing field and is annotated `@JsonIgnore` (MEDIUM-1 fix). (d) `LogCaptor`-equivalent TestLogAppender confirms no PAN fixture (`4111111111111111`) in any captured log line. (e) `PaymentNotFoundException` handler returns generic "payment not found" (no ID echo, no PAN). | **PASS** |
| PAYMENT-SEC-002 | 6.5 — Secure coding | (a) Supplemental grep returns 0 hits: `grep -rn "creditCard\|panNumber\|cardNumber" backend/src/main/java/ \| grep -v test \| wc -l == 0`. (b) Repository layer audit — no SQL string concatenation; all queries are JPA-derived (`findByIdAndUserId`, `sumByPaymentId`, etc.) using parameterized binding. (c) Constructor injection used throughout payment package (PaymentService, RefundService, PaymentEventLedger, MockProvider, IdempotencyKeyStore, PaymentMdcFilter — no field-injection `@Autowired`). (d) No misuse of `Optional` as field/parameter type. (e) No use of `Math.random` for security-sensitive values (UUID.randomUUID + sha256 used). | **PASS** |
| PAYMENT-SEC-003 | 4.1 — Encrypt transmission | (a) SecurityConfig HSTS header configured: `Strict-Transport-Security` with `max-age=31536000` + `includeSubDomains(true)` + `requestMatcher(req -> true)` so the header is set on every response (verified by `PaymentSecurityTest#tlsOnly_httpsHeaderPresentOnResponse` GREEN). (b) Production TLS termination occurs at the load balancer / ingress (per the inline SecurityConfig comment); the application-layer HSTS header documents the requires-secure policy. (c) The reference workload binds to HTTP for testing; the documented production posture is that any infrastructure-layer HTTP listener is either absent or redirects to HTTPS via the load balancer. | **PASS** |

**Trace overall:** 3 / 3 PASS

---

## 5. Supplemental Hardcoded-Secrets Grep

Continuous security check (also enforced by
`PaymentSecurityTest#hardcodedSecretsScan_zeroHits`):

```
grep -rn "creditCard\|panNumber\|cardNumber" backend/src/main/java/ \
  | grep -v test | wc -l
# Output: 0
```

**Result:** 0 hits — PASS.

---

## 6. Acceptance Criteria Status (US-014)

| Criterion | Status |
|-----------|--------|
| `everything-claude-code:security-reviewer` Skill invoked with payment package + manifest + spec as input | DONE |
| Skill output saved to `docs/blueprints/payment/security-review.md` | DONE (this file) |
| PCI-DSS checklist trace verified — PAYMENT-SEC-001 → 3.4, PAYMENT-SEC-002 → 6.5, PAYMENT-SEC-003 → 4.1 | DONE (§4) |
| Supplemental grep: `grep -rn 'creditCard\|panNumber\|cardNumber' backend/src/main/java/ \| grep -v test \| wc -l` returns 0 | DONE (§5) |
| All security-reviewer findings classified critical/high addressed; medium/low documented with rationale | DONE — 0 CRITICAL, 1 HIGH FIXED, 2/4 MEDIUM FIXED + 2 DEFERRED with rationale, 3 LOW DOCUMENTED |
| `git commit 'fix(payment): security-review findings + PCI trace verified'` | (this commit) |

---

## 7. Open Follow-ups (NOT P5 scope)

1. **Rate-limit `/api/payments/**`** (MEDIUM-3). Extend `blueprints/ratelimit-manifest.yaml` with a payment-specific policy + bind via the existing rate-limiter filter. Requires Spec Trio update.
2. **`@PreAuthorize` on `PaymentAdminController` methods** (MEDIUM-4). Belongs to US-013 java-reviewer's defense-in-depth pass.
3. **Production HTTPS enforcement at the application layer** — currently relies on infrastructure TLS termination. A future iteration could add `requiresChannel(...).requiresSecure()` to SecurityConfig and gate that on a profile flag.
4. **Bind rate-limiting to admin endpoints** — `/api/admin/reconciliation/run` now requires ROLE_ADMIN but should also be rate-limited to prevent ledger-append flood by a compromised admin token.
5. **Idempotency-Key length validation** — currently accepts any non-blank string. Consider enforcing UUID/v4 format or a max-length of 255 chars to prevent DB-column overrun attacks.

---

## 8. Sign-off

**Reviewer:** Wave 5 Agent B (Opus) — US-014 P5 security-reviewer Skill pass
**Date:** 2026-05-17
**Reference commit (base):** 336ead0 (US-009 P3.0 GREEN baseline)
**Result:** ACCEPTED — PCI-DSS SAQ-A scope verified PASS; 1 HIGH + 2 MEDIUM fixed; 2 MEDIUM + 3 LOW documented for future iteration; supplemental grep PASS.
