# PRD — ax-template remaining-registered-work closure wave ("ledger-zero") — rev 4 (post-Critic re-pass)

- Author: RALPLAN Planner (Fable 5), 2026-07-28. **rev 4** fixes exactly the three items the Critic re-pass held PARTIAL (#2, #6, #10) plus their consequential edits; items marked APPLIED (1,3,4,5,7,8,9,11,12) are unchanged. Disposition tables (with the rev-3 over-claim corrected) at end of §8.
- Worktree: `/Users/plletdata/Documents/AI/kyjin/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template-cproof` (branch `feat/consumer-proof-poc`, HEAD 50fa467, clean)
- Env: `export PATH="$HOME/.pyshim:$PATH"`; `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` for any gradle step. **All build/test commands are cd-qualified** — `(cd backend && ./gradlew …)`, `(cd frontend && npm …)` / `(cd frontend && npx vitest run …)`.
- Ground truth (Critic-confirmed + rev-4 disk re-derivation): 22 open rows; 107 scored cells / weight 131 / earned 102.0 / C_total 0.7786 / 0 downgrades; open cells S1 = 20 XB (10 gap / 10 partial, loss 15.0) + S1.multi-tenant.BE (0.5), S2 = 5 XB partials (3.5), S3 = 10 partials (10.0). 57 `enum:` blocks / 20 contract files; React markers 16/15/25 of 102; 79 L4 `page.tsx` / 34 useQuery / 1 `*-view.tsx`; `.ax-verify/` root-ignored (`.gitignore:26`); all 12 W10 gradle tasks exist; 21 contracts carry page-envelope fields; `currency-formatter.tsx`'s Stripe quote exists verbatim (snapshot :27) — **`currency-input.tsx:7-9` is the only fabricated anchor**. **rev-4 re-derived**: `ApprovalRequestRepository` (`ApprovalRequestRepository.java`) has exactly three query methods — `findByRequesterUserIdOrderByCreatedAtDesc` (:15, requester-only list, used at `ApprovalService:162` behind `GET /approvals`), `findInboxFor` (:29, @Query — PENDING steps assigned to caller on SUBMITTED requests, used at `ApprovalService:177-182` behind `GET /approvals/inbox`), and `findVisibleTo` (:35-45, **ID-scoped `Optional`, used ONLY by single-GET at `ApprovalService:171`** — there is no full-visibility LIST query); the sessions list is **`GET /api/sessions`** (`SessionController.java:44`, operationId `listSessions`), `AdminSessionController.java:33` exposes DELETE only; inbound webhook receivers are explicitly OUT of `blueprints/webhook-manifest.yaml` (its :19 scopes them to `practices/rules/webhook-hmac-required.md` + `specs/spring-practices-l0.yaml#PRACTICES-INTEG-001`); the favorites contract file is `contracts/favorites-bookmarks-openapi.yaml`.
- Note: `scratchpad/wave3-plan.md` no longer exists (rotated); its two claims (S1-XB dominant tail; S3 = 10.0 weighted) were re-derived from `coverage-map.yaml` and hold.

---

## 1. RALPLAN-DR summary

### Principles (grounded in the repo's north star)

1. **Mechanical enforcement over review-only claims.** Every closure ends in a binary check (guard fixture pair, gradle test, vitest) that goes RED when the fix is reverted. A doc edit with no mutation lock is what created P2-33 — do not reproduce that class.
2. **Honest under-claiming.** Coverage cells flip only by post-hoc adjudication against the finished artifact, never by plan promise (precedent: the S2.MONEY-QUANTITY.XB covered→partial retraction). Retraction/no-flip is the cheap default. Mid-wave findings are registered (denominator grows), not suppressed.
3. **Evidence-anchored, surgical changes.** Each item touches only the files its done-when names plus their test/guard surface. R26 stays clean. Corollary: a **fabricated evidence anchor currently passes every gate** (P2-40, `currency-input.tsx:7-9`) — in a catalog whose product claim is evidence-anchoring, that is the highest-integrity-value fix in the ledger and is ranked accordingly.
4. **Convergence is the metric, discovery is frozen.** This wave spends effort exclusively inside the ledger; opening new signature space is out of scope. Right-sizing an item and registering the remainder (P2-28) is convergence-honest. Corollary (rev-4, from Critic #2): **no test may protect an unused code path — tests bind to the selectors production actually calls.**
5. **One gradle owner; single-writer bookkeeping; committed-SHA discipline (R25 runs at a committed, clean-worktree SHA that is the push SHA); codex is last and its approval is persisted as a SHA-bound receipt.**

### Decision Drivers (top 3)

1. **Convergence math**: 22 open rows stand between 186/208 (89%) and full closure of the registered set. Every row has a concrete done-when; none requires new design discovery.
2. **Defect risk concentration**: 10 of the 22 are P2 verification-escapes on authz/contract/enforcement seams — a fabricated evidence citation passing all gates (P2-40), contracts that 400/blank when followed (P2-35), an empty-403 masquerading as authz failure (P2-41), an authz selector whose parity test protects zero shipped lines (P2-39). Exactly the "green-but-hollow" class this repo's methodology exists to kill.
3. **Cost asymmetry of the coverage tails**: closing the 22 (right-sized) is one wave. The S1-XB tail (20 cells × four-leg harness) + S3 capstones (10 adversarial-review-bound cross-domain E2E tests) is multiple waves of serial BE authoring (re-derived weights: S1-XB 15.0 + S3 10.0 of the 29.0 loss; remainder S2-XB 3.5 + S1.multi-tenant.BE 0.5).

### Viable Options

**Option A — "ledger-zero": close all 22 registered items (two right-sized with registered remainders), stop; coverage flips only by post-hoc adjudication.**
- Pros: completes the registered set; every item has a written done-when; heterogeneous small items parallelize; C_total can move honestly without unearned claims.
- Cons: C_total moves at most ~+0.8pt; the 25.0 weighted S1-XB/S3 mass stays open; a critic can say "you polished the ledger and ignored the map".

**Option B — close the 22 AND drive coverage convergence (S3 capstones + S1-XB harness).**
- Pros: attacks the dominant weighted mass (S3 10.0, S1-XB 15.0); the S3 bar is now defined.
- Cons: each capstone is an EcommerceE2ETest-class artifact subject to the adversarial review that REJECTED S3.saas-subscription's probe; 10 of them + 20×≤4 XB legs is 2–4× this wave, all BE-heavy → serializes on the single gradle owner and produces an unreviewable diff. Session history: wave-3's STOP analysis cut exactly this tail.

**Option C — defect-risk-first: close the 10 P2s (+P3-58/60/61), defer the P3 tail.**
- Pros: fastest risk burn-down; smallest diff.
- Cons / rejection: **rejected solely on the chosen driver — ledger convergence is the north-star metric, and any deferral leaves the registered set open regardless of per-item cost.** (rev-3 correction retained: no cost estimate is offered — the rejection is driver-based, not arithmetic.)

**DECISION: Option A**, with S1-XB/S3 convergence deferred to a dedicated follow-up ralplan. Invalidation for B: structurally incompatible with a 22-item parallel wave (serial gradle + per-recipe adversarial review). Invalidation for C: driver-based as stated above.

---

## 2. Scope

### IN — all 22 open rows, grouped by DEFECT CLASS

| WS | Defect class | Items | Parallel? |
|---|---|---|---|
| **W** | **Wire-contract binding seam** — "the wire contract is not mechanically bound to the code that serves it." One shared harness idiom (committed BE golden → read-only assertion legs on both sides → mutation lock) instantiated 5×: (i) contract-enum + L4-vocabulary parity guard, (ii) approval action-set golden, (iii) validation `errors[]` golden, (iv) scoped page-envelope sweep, (v) ProblemDetail emitted-body rebind. | P2-34 → P2-33, P2-35, P2-41, P3-63 → P2-38a → P2-38b → P2-39, P2-36, P3-54, P3-61 | **Serial gradle-owner lane**, internal order as listed. |
| **G** | Guard coverage holes (enforcement seams) | **P2-40 (first — fabricated evidence)**, P2-37 (Branch A, frozen dialect), P3-58, P3-59, P3-60 | Independent of each other and of gradle. |
| **T** | L4 template fidelity + FE precision | P2-28 (right-sized), P3-55, P3-62, P3-64, P3-65, P3-66 | FE/vitest/guard-only. Highly parallel. |
| **E** | Residual policy closures | P3-56 (a/b doc; c rides the gradle lane) | (a)/(b) parallel. |

### OUT (with reason)

- **S1-XB four-leg harness (20 cells)** and **S3 capstone stamping (10 recipes)** — deferred to the follow-up ralplan. No S1/S3 flips claimed.
- **S1.multi-tenant.BE** — no governing backlog row.
- **New dogfoods / new cells / proposed-cells.yaml** — discovery frozen.
- **P2-28 full conversion of all 79 pages** — right-sized to convention + guard + 5 verticals; remainder REGISTERED as a new row (denominator +1).
- **`fractionDigitsFor` → `@ax/core` promotion** — P3-65 closes via source-parity test; promotion is a registered follow-up.
- **`frontend/tsconfig.json` lib bump** — P3-64 uses `formatToParts` instead.
- **`templates/L1/components/currency-formatter.tsx`** — explicitly NOT edited (correctly anchored; G1's positive scan subject).

### Coverage-cell flips: NOT promised

Default outcome is **NO flip** (C_total stays 0.7786). Exactly ONE cell is a flip candidate, adjudicated in Phase 3 against the finished artifact:

- **S2.AUTHZ.XB** — candidate iff W6a landed (shared predicates — **no 38a ⇒ no flip, categorically**) AND a written honesty judgment in the cell `notes` answers its three enumerated reasons ONE BY ONE.
- **S2.INPUT-VALIDATION.XB is NOT a candidate.** Its gap is request-schema→runtime-payload parity; W8 binds only the validation-error OUTPUT shape. A future request-schema/runtime harness may reopen it.

Permitted C_total outcomes: **{0.7786 (default), 0.7863 (AUTHZ.XB adjudicated)}**. Any `coverage-map.yaml` edit is Phase-4 single-writer and must survive `coverage_map_guard.sh` + `coverage-report.sh` with 0 downgrades.

---

## 3. Per-item execution spec

Format: **Done-when** / **Files** / **Acceptance (testable)** / **RED-on-revert**. All gradle/npm commands cd-qualified.

### WS-W — Wire-contract binding seam (serial gradle lane)

**W1 = P2-34 — L4 webhook fork-copy: state-model reconciliation + written mapping decision.**
- NOT a rename: the L4 copy's 5-value vocabulary (`page.tsx:33` `PENDING|IN_FLIGHT|SUCCEEDED|FAILED|DEAD_LETTER`) is **non-isomorphic** with the canonical 4 (`PENDING|PENDING_RETRY|SUCCEEDED|FAILED_PERMANENT`); `page.tsx:87` (replay gated on FAILED|DEAD_LETTER) is REPLAY SEMANTICS, not vocabulary.
- Done-when: an explicit canonical←L4 mapping table in the commit body, applied: `PENDING→PENDING`; `IN_FLIGHT→dropped` (reference model has no in-flight state); `FAILED→FAILED_PERMANENT`; `DEAD_LETTER→FAILED_PERMANENT`; replay gates on `FAILED_PERMANENT` only (PENDING_RETRY is auto-retried — manual replay would double-send; recorded in the table). All drift sites updated: `WebhookDelivery.java.skeleton:38` (**its stale token is `SUCCESS`**, not IN_FLIGHT), `page.tsx:33,70,74,76,87,130-131,176,203-206,320`, `providers.tsx:35`, `templates/L4/webhook/README.md:205,212`.
- Files: the four L4 surfaces; W2 manifest `vocab_scan` entries.
- Acceptance: `grep -rnw 'IN_FLIGHT\|DEAD_LETTER\|SUCCESS' templates/L4/webhook/` → 0 stray-vocab hits (word-boundary; `SUCCESS` is not a substring of `SUCCEEDED`, so this is exact); mapping table present in commit body.
- RED-on-revert (per-surface, FOUR dedicated demonstrations after W2 lands — rev-4 makes the skeleton one real): (1) restore `IN_FLIGHT` in `page.tsx:33` ⇒ guard exit 1; (2) **restore `SUCCESS` in `WebhookDelivery.java.skeleton:38` ⇒ guard exit 1** (locked by the skeleton entry's `require_all` + forbidden `SUCCESS`, below); (3) restore the `providers.tsx:35` variant ⇒ exit 1; (4) restore the `README.md:205` table row ⇒ exit 1. All four recorded in the closure note.
- MUST land before W2 (else guard born RED).

**W2 = P2-33 — contract↔code enum parity guard, exhaustive-by-construction, with L4 vocabulary scan-kinds (skeleton lock fixed per Critic re-pass #6).**
- Done-when: new guard `practices/evals/contract_enum_parity_guard.sh` + python lib that (i) **enumerates EVERY `enum:` block in `contracts/*.yaml` (57 today across 20 files, incl. the 3 inline-map enums) and FAILS if any is unclassified**; (ii) compares each classified block's YAML constant set against the mapped Java enum's constants.
- Manifest `practices/evals/contract-enum-map.yaml` — **two entry kinds, schema-explicit**:
  - `kind: contract_enum` (contract-path + JSON-pointer): exactly one of `java_enum: <FQCN>` (optional `wire_extra: [ALL]` + mandatory `reason:`) | `wire_only: <reason>`.
  - `kind: vocab_scan` (L4/doc surfaces the contract_enum schema cannot express): `file:` + `canonical: [PENDING, PENDING_RETRY, SUCCEEDED, FAILED_PERMANENT]` + `forbidden: […]` + optional `require_all: true`. Matching is **word-boundary token grep** (`SUCCESS` will not false-match `SUCCEEDED`). **rev-4 entries (all four surfaces locked):**
    - `page.tsx` (TS union): `require_all: true`, `forbidden: [IN_FLIGHT, DEAD_LETTER, FAILED]`-minus-legals per the union literal (exact list finalized against the post-W1 file; FAILED alone is forbidden only as the bare union member, expressed as the union-literal `require_all` check).
    - **`WebhookDelivery.java.skeleton`: `require_all: true` AND `forbidden: [SUCCESS, IN_FLIGHT, DEAD_LETTER]`** — restoring the skeleton's real drift token `SUCCESS` now fails on BOTH axes (missing `SUCCEEDED` from the canonical set + forbidden token present). This closes the rev-3 gap where only 3 of 4 surfaces were actually locked.
    - `providers.tsx`: `forbidden: [IN_FLIGHT, DEAD_LETTER, SUCCESS]`.
    - `templates/L4/webhook/README.md`: `forbidden: [IN_FLIGHT, DEAD_LETTER, SUCCESS]`.
- Constant extraction (java side) is grep/parse — no JVM — fixture-tested against the **three real shapes**: plain (`WebhookDeliveryStatus.java`), constructor-arg (`ExportFormat.java:12-13`; same shape Unit/RetentionTier/DsrRequestType/EscalationRung), nested-in-entity (`WebhookDelivery.java.skeleton:38`).
- Heuristic-risk statement (PM-2): *classification* is killed by the exhaustive manifest (unclassified = FAIL; no name inference); *extraction/vocab_scan* are deterministic parsing/word-boundary grep — fragile only without fixtures, hence the fixture sets. Materially different from the 13-round lint fossil.
- Files: guard + lib + `fixtures/contract_enum_parity_guard/{pass_clean, fail_drift, fail_unmapped_enum, fail_vocab_scan}` (the vocab fixture includes a **skeleton-SUCCESS case**); `run-all-guards.sh` registration; manifest.
- Acceptance: guard PASS on HEAD (57/57 classified + all vocab_scan green); `fail_drift` exits 1; `fail_unmapped_enum` exits 1; `fail_vocab_scan` (incl. skeleton-SUCCESS) exits 1; extractor fixtures pass for all three shapes.
- RED-on-revert: `contracts/webhook-openapi.yaml:148` → `SUCCESS` ⇒ exit 1; delete a manifest entry ⇒ exit 1; W1's four per-surface restores ⇒ exit 1 each (skeleton restore is a dedicated, individually-run demonstration).

**W3 = P2-35 — sibling contracts' `default: ALL` / undeclared-param family (3 cases).**
- Done-when: (a) identity-verification: `IdentityVerificationService.listAdmin` (≈:168-170) treats `ALL` (case-insensitive, notification precedent) as no-filter. (b) email-outbox: ALL→null in `EmailOutboxAdminController` (:47) via the `parseStatusFilter` pattern. (c) scheduled-task: declare + implement `status` in `ScheduledTaskController` (≈:85-86) per contract (:20-25). Each case: black-box RestAssured regression — `?provider=ALL`/`?status=ALL` returns the unfiltered page (not empty/400); a real value filters.
- Files: 3 controllers/services + 3 test classes; contracts only for response examples. W2 manifest gains `wire_extra: [ALL]` entries for these three.
- Acceptance: `(cd backend && ./gradlew testIdentityVerification testEmailOutbox testScheduledTask)` GREEN with new @Tag items.
- RED-on-revert: revert each branch → its test fails. Interpretation: implement ALL in code (notification made ALL the family convention; shrinking contracts breaks fork copies).

**W4 = P2-41 — unmapped binding exceptions surface as empty 403.**
- Done-when: (1) census — `grep -rn "MissingServletRequestParameter" backend/src/test`; map `MissingServletRequestParameterException` in `GlobalProblemDetailAdvice` → 400 ProblemDetail, code `MISSING_PARAMETER`, parameter name + expected type only (mirror of `handleTypeMismatch`, no input echo); (2) `contracts/webhook-openapi.yaml` GET /webhook-deliveries gains the 400 via the family BadRequest component.
- Files: `GlobalProblemDetailAdvice.java`, `contracts/webhook-openapi.yaml`, one new test (required no-default `@RequestParam` controller — geoquery or sessionmanagement — hit without the param, assert 400 + ProblemDetail body).
- Acceptance: new test GREEN under its owning task; `(cd backend && ./gradlew testWebhook)` GREEN.
- RED-on-revert: delete the handler → test observes empty 403 → RED.

**W5 = P3-63 — approver read access after terminal state (narrow, per Critic #1).**
- Done-when: DECISION: YES, narrowly — **SUBMITTED-visibility preserved unchanged; terminal visibility added ONLY for an approver who ACTED** (their step's status ∈ {APPROVED, REJECTED} with `approverUserId = caller`). Implemented first in `findVisibleTo` (:35-45 — still live at this point in the lane; W6a then absorbs the rule into `canView` and deletes `findVisibleTo`, see W6a); spec WF-AUTHZ-002 notes + `blueprints/approval-workflow-manifest.yaml` updated.
- Tests: acted approver GETs APPROVED/REJECTED request → 200; **DRAFT request, assigned approver → 404; unacted later approver after rejection/cancellation → 404; unrelated user → 404**.
- Golden: `authorized-actions.golden.json` gains the distinguishing PAIR — `acted approver, terminal → [view]` / `unacted approver, terminal → []`.
- Acceptance: `(cd backend && ./gradlew testApprovalWorkflow)` GREEN incl. the negatives.
- RED-on-revert: restore the SUBMITTED-only arm → acted-approver test RED; re-widen (drop the acted-only qualifier) → DRAFT/unacted negatives RED.
- Fallback (row permits NO + record): DECISION record + `[x]`, zero code.
- Sequencing honesty: W5→W6→W7 is rework avoidance via the shared golden, not code-forced.

**W6a = P2-38a — extract the approval authz guards into named, shared predicates (rev-4: real read-path topology, per Critic re-pass #2).**
- Disk truth (rev-4 re-derived): inline throws in `actOnStep` (:195-202 terminal/not-SUBMITTED; :209-217 approver mismatch, R83 PII-safe message; :219-227 strict ordering) and `loadOwn` (:245-251 requester-only 404). Read topology: **`findVisibleTo` (:35-45) is ID-scoped and serves ONLY single-GET (`ApprovalService:171`)**; the only real LIST selectors are **`findByRequesterUserIdOrderByCreatedAtDesc` (:15 — requester list, `ApprovalService:162`, `GET /approvals`)** and **`findInboxFor` (:29 — PENDING steps assigned to caller on SUBMITTED requests, `ApprovalService:177-182`, `GET /approvals/inbox`)**. There is NO full-visibility list query — rev-3's "JPQL↔canView parity matrix for LIST" targeted a path that does not exist and would have protected an unused policy.
- **Step 0 (pre-refactor characterization, unchanged from rev 3):** exact-message assertions for the two 409s, the 409 STEP_OUT_OF_ORDER, the exact 403 NotApprover string (locks R83), and the 404 ownership behavior — committed green against the UNMODIFIED service before any extraction.
- Done-when:
  1. `ApprovalActionGuards` (service package) exposes `isActionable(request)`, `isAssignedApprover(step, actorUserId)`, `isNextActionableStep(request, step)`, `isRequester(request, callerUserId)`, `canView(request, callerUserId)` (the full visibility rule incl. W5's acted-approver arm).
  2. Consumers: `actOnStep` → isActionable/isAssignedApprover/isNextActionableStep; `loadOwn` → `isRequester`; **single-GET (`ApprovalService:171`) → `findById` + `canView` (404 on false)**.
  3. **`findVisibleTo` is DELETED in the same change that switches single-GET to `canView`** — an explicit step, not an option. Grep-check: `grep -rn findVisibleTo backend/src` → 0. **Invariant (stated for the executor and Phase-3 reviewer): no test may protect an unused repository policy** — any test that would only go RED by editing dead code is itself a defect.
  4. **Real-selector contract tests replace the fictitious LIST parity**: (a) *requester list* — seeded matrix: every row returned by `GET /approvals` satisfies `isRequester(row, caller)`, and a request of ANOTHER user (visible to caller only as approver) does NOT appear; (b) *inbox* — every entry returned by `GET /approvals/inbox` is a PENDING step assigned to the caller on a SUBMITTED request (i.e. consistent with `isAssignedApprover ∧ isActionable`), and an acted or terminal-request step does NOT appear. Both are black-box RestAssured against the live endpoints, checked against the predicates' semantics.
- Behavior change: zero (Step-0 characterizations prove it; the deleted query's semantics live on in `canView`, which W5's positive/negative tests pin).
- Files: Step-0 tests; `ApprovalActionGuards`; `ApprovalService` (single-GET rewire + repository method deletion); the two real-selector contract tests.
- Acceptance: `(cd backend && ./gradlew testApprovalWorkflow)` GREEN with zero edits to Step-0 assertions; `grep -rn findVisibleTo backend/src` → 0.
- **Mutation proofs (THREE, re-targeted to live paths):** (1) *ordering*: neuter `isNextActionableStep` → the 409 STEP_OUT_OF_ORDER action-path test AND the W6b parity test both RED from one edit; (2) *ownership*: neuter `isRequester` → loadOwn ownership test AND the requester-list contract test both RED; (3) *visibility*: neuter `canView` → single-GET visibility tests (W5 positive 200→404 and/or negatives) AND the W6b parity `view` action both RED. All three demonstrated and recorded.
- **GATE: no W6a ⇒ no S2.AUTHZ.XB flip, categorically.**

**W6b = P2-38b — `allowedActions` computed via W6a's predicates, emitted on the wire.**
- Done-when: `ApprovalRequestResponse` (+`ApprovalStepResponse` where step-scoped) gains `allowedActions: string[]` ⊆ `{submit,cancel,approve,reject,view}`, computed by `ApprovalActionEvaluator` delegating ONLY to `ApprovalActionGuards` + the state machines' legal-transition tables — zero local transition/authz logic; contract + blueprint document the field; the BE parity test asserts the **live HTTP response's** `allowedActions` equals the golden row (RestAssured).
- Files: 2 DTOs, evaluator, controller wiring, contract, manifest, BE parity rebind, golden.
- Acceptance: `(cd backend && ./gradlew testApprovalWorkflow)` GREEN; evaluator unit matrix (role × status × step-position) GREEN.
- RED-on-revert: the three W6a mutation proofs; plus flip one evaluator delegation edge → parity vs golden RED.

**W7 = P2-39 — promote the action-set selector to L0; test AND page import it.**
- Done-when: selector → `templates/L0/fork-receiver-kit/authorized-actions.ts` (prefers `allowedActions` when present; derivation only as documented fallback for older-BE forks); `frontend/tests/authz-action-parity.vitest.ts` (:63-89) imports it; `templates/L4/approval-workflow/app/(approvals)/[id]/page.tsx` `describeChain` (:123-149) replaced by the same import.
- Precedent stated accurately: `parse-page-envelope.ts` has ONE consumer (the parity vitest); no L4 page imports it — the both-import pattern is NEW here, mechanically established via page.tsx:30-31's existing L0 imports.
- Acceptance: `(cd frontend && npx vitest run tests/authz-action-parity.vitest.ts)` GREEN; `grep -rn "describeChain" templates/L4/approval-workflow` → import-based usage only; own-blocks lint GREEN.
- RED-on-revert: remove ordering handling in the shared module → parity vitest RED, now protecting the shipped page too.

**W8 = P2-36 — validation `errors[]` bound to the FE parser (read-only golden idiom).**
- Done-when: **committed** golden `frontend/tests/_fixtures/validation-error.golden.json` (real bean-validation 400 incl. `errors[]` from `GlobalProblemDetailAdvice` :361-370, :412-421). **Assertion path READ-ONLY on both legs**: BE test serializes the production error in memory and compares against the committed golden (never writes it); FE leg `frontend/tests/field-errors-parity.vitest.ts` reads the same file into `parse-field-errors.ts`. **Regeneration is a separate explicit manual command** (`-Dgolden.regenerate=true`, documented) — never in the assertion path.
- The hand-built `{field,defaultMessage}` case in `fmw2-primitives.vitest.ts:63-80` retained only as documented legacy-shape tolerance.
- Acceptance: both legs GREEN. Mutation locks: rename `pointer`→`ptr` in the advice → BE leg RED; change the parser's field key → FE leg RED.
- No coverage-flip candidacy (§2).

**W9 = P3-61 — ProblemDetail six-members test asserts the emitted body.** Subject → `MAPPER.readTree(MAPPER.writeValueAsString(buildRepresentativeError()))` in `ProblemDetailContractParityTest` (:150-151). Acceptance: owning task GREEN via `(cd backend && ./gradlew <owning task>)`. RED-on-revert: with the new subject, deleting a member from the emitted shape flips THIS test independently of test 1 (observe once; record).

**W10 = P3-54 — page-envelope parity beyond scenario-local (candidates BOUND to contract operations, per Critic re-pass #10a/b).**
- Disk truth: 21 of 36 contracts declare page-envelope members; a full live sweep needs per-domain auth+seed — the artifact class this PRD defers.
- **Candidate binding table (endpoint → operationId → response schema → owning gradle task; disk-verified 2026-07-28 — the executor implements against these bindings and does not re-derive them):**

| # | Endpoint | Contract file | operationId | Envelope schema (declares the page members) | Owning task |
|---|---|---|---|---|---|
| 1 | `GET /audit-logs` | `audit-log-openapi.yaml` | `listAuditLogs` | `AuditLogPage` (required content, totalElements, totalPages, page, size) | `testAuditLog` |
| 2 | `GET /email-outbox` | `email-outbox-openapi.yaml` | `listEmailOutbox` | `EmailOutboxPage` | `testEmailOutbox` |
| 3 | `GET /webhook-deliveries` | `webhook-openapi.yaml` | `listWebhookDeliveries` | `WebhookDeliveryPage` | `testWebhook` |
| 4 | `GET /notifications` | `notification-openapi.yaml` | `listNotifications` | `NotificationPage` | `testNotification` |
| 5 | `GET /scheduled-tasks` | `scheduled-task-openapi.yaml` | `listScheduledTasks` | `ScheduledTaskPage` | `testScheduledTask` |
| 6 | `GET /admin/identity-verification` | `identity-verification-openapi.yaml` | `listVerifiedIdentities` | `PagedVerifiedIdentityResponse` (required content, totalElements, totalPages, page, size) | `testIdentityVerification` |
| 7 | **`GET /sessions`** (`SessionController.java:44` — rev-4 correction: the "session-management admin list" named in rev 3 does not exist; `AdminSessionController.java:33` is DELETE-only) | `session-management-openapi.yaml` | `listSessions` | `SessionListResponse` (required items, totalElements) | `testSessionManagement` |
| 8 | `GET /favorites` | **`favorites-bookmarks-openapi.yaml`** (rev-4: exact filename) | `listFavorites` | envelope schema (required items, totalElements) | `testFavorites` |
| 9 | `GET /activities` | `activity-feed-openapi.yaml` | `listActivities` | envelope (required items, page, size, totalElements) | `testActivityFeed` |
| 10 | `GET /api-keys` | `api-key-openapi.yaml` | `listApiKeys` | envelope (required items, totalElements) | `testApiKey` |
| 11 | `GET /approvals` + `GET /approvals/inbox` | `approval-workflow-openapi.yaml` | `listMyApprovalRequests` / `listApprovalInbox` | list/inbox response schemas (`ApprovalInboxResponse` items, totalElements) | `testApprovalWorkflow` |
| 12 | `GET /admin/feature-flags` | `feature-flags-openapi.yaml` | `listFeatureFlags` | `FeatureFlagPage` | `testFeatureFlags` |

- Done-when: `PageEnvelopeCatalogSweepTest` (tagged, wired under `testCommonPrimitives` or its own task) drives each reachable bound endpoint as an authorized principal and asserts the response's member set equals **that endpoint's contract-declared required member set** (the binding column above — this is the parity, per-contract, not one fixed shape); every non-covered declaring endpoint sits in an explicit allowlist **with a pinned count assertion**; closure line states the ratio (swept/21).
- Escape hatch (decided in Phase 3): reachable set < 8 domains ⇒ decision-record fallback (precedent P3-41/42).
- Acceptance: `(cd backend && ./gradlew testCommonPrimitives)` (or owning task) GREEN; allowlist count pinned.
- RED-on-revert: rename one envelope member in one swept controller's DTO → sweep RED.

**W11 = P3-56(c) — dead `assert` → runtime check.** Replace `GovernedFormStateMachine.java:80` `assert` with an explicit `IllegalStateException` + 1 unit test proving a widening FORWARD edge trips it, run under default `-da`. Acceptance: owning task GREEN via `(cd backend && ./gradlew <owning task>)`. RED-on-revert: revert to `assert` → test RED under `-da`.

### WS-G — Guard coverage holes (no gradle; P2-40 FIRST)

**G1 = P2-40 — evidence_quote_spotcheck skips templates/** frontmatter (highest integrity value).**
- Fabricated anchor: **`currency-input.tsx:7-9` ONLY**. **`currency-formatter.tsx` is correctly anchored (exact section+quote at snapshot :27) and is NOT edited — it is the guard's positive real-repository scan subject.**
- Done-when: guard sweeps `templates/**` files carrying an `evidence:` frontmatter block; `currency-input.tsx`'s citation corrected to a real snapshot string (candidate: the Zero-decimal currencies section); fail fixture with a nonexistent quote exits 1.
- Non-vacuity ritual: run the extended guard ONCE before the fix — must FAIL on `currency-input.tsx` AND PASS on `currency-formatter.tsx` (both recorded); then fix; then full PASS.
- Acceptance: guard PASS post-fix; `grep -F "<corrected quote>" practices/upstream/stripe-billing-2026-05.snapshot.md` ≥ 1; before-fix FAIL/PASS pair recorded.
- RED-on-revert: restore the fabricated quote → exit 1.

**G2 = P2-37 — substance gate for practices-react: Branch A with a FROZEN dialect.**
- Corrected premise (closure note): React rules are NOT ungated — `practices-react/evals/run.sh:36-38` runs spec_ref/time_decay/evidence with `--catalog practices-react`; substance is the only missing gate (Java markers don't port: 16/15/25 of 102).
- **Dialect FROZEN — four clauses, exact parse semantics, no post-census substitution:** (1) frontmatter `impactDescription` non-empty scalar (quote-strip, ≥1 non-whitespace char); (2) frontmatter `verification:` block with non-empty `notes:` scalar, OR body heading `^##.*Verification` followed by ≥1 non-blank prose line before the next heading; (3) ≥1 fenced code block ≥3 lines not matching `(TODO|FIXME|\.\.\.|placeholder|<your)` (case-insensitive); (4) ≥1 `https?://` URL in frontmatter or body.
- Census-first over the 102 rules; every failing rule is a REAL finding — remediate the body, never weaken a clause; N recorded.
- **Fixtures: FOUR negatives, one per clause** (`fail_react_no_impact`, `fail_react_no_verification`, `fail_react_placeholder_fence`, `fail_react_no_url`) + one pass fixture. FORBIDDEN: any silently-laxer React mode — dialect name printed in guard output.
- Branch B (rejected, recorded): dialect is definable today; deferral leaves the only ungated axis open another wave.
- Acceptance: `run-all-guards.sh` PASS with the react-substance step present; all four negatives exit 1; census note records N.
- RED-on-revert: remove the registration → acceptance re-run asserts the step's presence; each clause-neuter flips its fixture 1→0.

**G3 = P3-58 — check 7's compose-spec exclusion is content-blind.** Content heuristic for S3 nonvacuity entries under `frontend/tests/**`: if every assertion derives from `fs.existsSync`/`readFileSync`, reject regardless of filename; blocking fixture reproduces the rename-bypass (`booking-flow.spec.ts`). Necessary-not-sufficient floor; no semantic classification (PM-2). Acceptance: guard PASS on real map; fixture exits 1. RED-on-revert: revert the content check → fixture flips 1→0.

**G4 = P3-59 — stale header comment (exact assertion).** After G3 (extends check 7) and G5 (adds check 8), `lib/coverage_map_guard.py` has **eight** checks. Done-when: `coverage_map_guard.sh:5` reads exactly `# See lib/coverage_map_guard.py for the eight checks`; closure commit records `grep -c '^def check' lib/coverage_map_guard.py` == 8 (number copied from disk at edit time). Sequenced AFTER G3+G5. Acceptance: `grep -F 'for the eight checks' practices/consumer-proof/engine/coverage_map_guard.sh` = 1; recorded count = 8. RED-on-revert: n/a (doc line; triviality exemption stated).

**G5 = P3-60 — S1/S2 `.md`-only nonvacuity floor (prophylactic, stated honestly).** 0 of 70 covered S1/S2 cells are `.md`-only — zero live subjects; gates nothing today; README note says exactly that. Done-when: check 8 in `coverage_map_guard.py` (S1/S2: `covered` ⇒ ≥1 non-`.md` nonvacuity entry), fixture pair, README note. Acceptance: guard PASS on real map; `fail_md_only_s1` exits 1. RED-on-revert: revert check 8 → fixture flips.

### WS-T — L4 template fidelity + FE precision

**T1 = P2-28 — L4 pages untestable-as-shipped (right-sized).**
- Disk truth: 79 `page.tsx` / 24 verticals (34 useQuery); 1 exemplar. Full conversion is larger than the other 21 items combined for zero coverage gain; the row demands a CONVENTION.
- Done-when: (i) convention DOCUMENTED (templates/L4 README + `docs/NEW-DOMAIN-CHECKLIST.md` FE section): data-rendering `page.tsx` extracts its render layer into a co-located pure `<domain>-<surface>-view.tsx` (props-only, no `@ax/blocks` bare specifiers, no `useQuery`); routing/redirect shells exempt with inline note. (ii) `l4_presentational_view_guard.sh` enforces over a **conversion ledger** (born green; ledger-shrink RED). (iii) **5 verticals converted** + audit-log: webhook (synergy W1), approval-workflow (synergy W7), payment, email-outbox, crud. (iv) ≥1 vitest per converted vertical rendering the view with fixture props. (v) remainder (~18 verticals / ~55 pages) REGISTERED as a new BACKLOG row (denominator +1, Phase 4).
- Acceptance: `(cd frontend && npm run test)` GREEN incl. ≥5 view vitests; guard PASS; `(cd frontend && npm run lint)` GREEN; new row present.
- RED-on-revert: re-inline one converted view → guard exits 1; delete a view's null-safety branch → its vitest RED.
- Scope guard: extraction only; behavioral defects found → register, don't fix inline.

**T2 = P3-62 — rate-limit-banner untested branches + misleading title.** Add `parseRetryAfter('Wed, 21 Oct 2026 07:28:00 GMT', fixedNow)` (:50-55) and `extractRetryAfterFrom429(new Response(null,{status:200})) === null` (:64); fix the :75 title. Acceptance: `(cd frontend && npx vitest run tests/rate-limit-banner.vitest.tsx)` GREEN. RED-on-revert: flip HTTP-date parse to null → RED.

**T3 = P3-64 — `apps/pay` formatMinor loses 1 minor unit at MAX_SAFE_INTEGER.** Rewrite `format(Number(major))` (:137-145) via `Intl.NumberFormat.formatToParts` over string-decomposed parts (no tsconfig bump). Convert `pay-money-fraction-digits.vitest.ts:93-105` characterization → correct-value assertion (`…409.91`). Acceptance: `(cd frontend && npx vitest run tests/pay-money-fraction-digits.vitest.ts)` GREEN. RED-on-revert: restore `format(Number(major))` → RED.

**T4 = P3-65 — `fractionDigitsFor` duplicate copies.** `frontend/tests/money-source-parity.vitest.ts` asserts the two copies agree over the full ISO-4217 special-cases table ("최소한" branch; `@ax/core` promotion = registered follow-up). Acceptance: `(cd frontend && npx vitest run tests/money-source-parity.vitest.ts)` GREEN. RED-on-revert: remove BHD from one copy's 3-digit set → RED.

**T5 = P3-66 — L2 frontmatter `dependencies` drift.** Fix `invoice-list.tsx:14` + `pricing-table.tsx:18` → `dependencies: [currency-input]`; add `l2_frontmatter_deps_guard.sh` (declared dependency must appear in the file's import specifiers); fixture pair. Acceptance: guard PASS. RED-on-revert: restore `currency-formatter` → exit 1.

**T6 = P3-55 — identity-name lint heuristic + own-blocks rule-list drift.** Option (b): (i) keep the two justified inline disables (page.tsx:184,186) + rule-doc note; (ii) sync `frontend/eslint.own-blocks.config.mjs` to the full ax/* set (add `no-caller-identity-from-props`; `no-app-local-ui-primitives` excluded with inline reason); (iii) mechanize: extend `lint_own_blocks_guard.sh` to diff the config's rule list against `practices-react/eslint-plugin-ax/rules/` minus an explicit exclusion allowlist. Acceptance: L4 sweep lint GREEN; guard PASS. RED-on-revert: drop the newly-wired rule → guard exits 1.

### WS-E — Residual policy closures

**E1 = P3-56(a)(b) — doc closures (exact destinations + verbatim blocks).**
- (a) `blueprints/audit-log-manifest.yaml` — extend the existing RBAC section:
  ```yaml
  read_policy:
    default: any-authenticated
    rationale: "Reference posture: audit rows store masked actorIp and never expose metadataJson; broad read supports internal transparency. Fork-receivers with stricter threat models MUST pick an option below. (OWASP ASVS V7 — access to audit trails is itself access-controlled.)"
    options:
      - id: self-only
        description: "MEMBER/MANAGER may list/get only rows where actorUserId == caller; ADMIN/AUDITOR unrestricted."
      - id: auditor-role-only
        description: "LIST/GET restricted to ROLE_ADMIN / ROLE_AUDITOR (mirrors the EXPORT surface, AuditLogExportController:35-36)."
  ```
- (b) **Destination decided (rev-4, per Critic #10c): `practices/rules/webhook-hmac-required.md`** — the inbound-receiver documentation surface that `blueprints/webhook-manifest.yaml:19` itself names when excluding inbound from the (sender-side) manifest. The rule's body gains a fenced YAML pattern block, verbatim (frontmatter untouched — evidence/spec_ref gates unaffected; note: this edit is under `practices/`, so the pre-commit 4-gate hook fires — expected, and it passes because frontmatter is unchanged):
  ```yaml
  replay_dedup_marking:
    current_behavior: "InboundSignatureVerifier.verify() marks firstSeen at signature-verification time (ReplayDedupStore:34); no rollback/unmark path exists."
    trap: "A fork-receiver that adds downstream processing after verify() will permanently 409 a sender's legitimate retry of the same event_id within the 300s tolerance window whenever that downstream processing fails."
    required_pattern: "Fork-receivers adding downstream processing MUST either (a) move firstSeen marking to after downstream success, or (b) add an unmark-on-failure compensation. The reference repo keeps mark-at-verify because it has no downstream processing (trap latent by design)."
  ```
- Acceptance: `grep -n 'read_policy:' blueprints/audit-log-manifest.yaml` = 1 match with the two option ids; `grep -n 'replay_dedup_marking:' practices/rules/webhook-hmac-required.md` = 1 match containing the MUST sentence.

---

## 4. Expanded test plan (deliberate mode)

**Unit**
- WS-G/T guards: pass/fail fixture pairs (G2 carries FOUR clause-negatives; W2's vocab fixture includes the skeleton-SUCCESS case); kill-proofs in [87] where the neuter vocabulary fits, else the observed 1→0 flip recorded.
- W2: extractor fixtures for the three enum shapes + `fail_vocab_scan`.
- T2/T3/T4: boundary-value vitests (HTTP-date fixed clock; MAX_SAFE_INTEGER with KRW/BHD; full special-cases table).
- W6b: evaluator unit matrix (role × status × step-position).

**Integration**
- WS-W: RestAssured black-box per W3 case and W4 (400-with-body, never empty 403), gradle-owner only, aggregated into final R25.
- W5/W6: `testApprovalWorkflow` — acted-approver terminal 200; DRAFT-assignee / unacted-approver / unrelated 404 negatives; Step-0 exact-message characterizations green before AND after extraction; **the two real-selector contract tests (requester list ↔ `isRequester`; inbox ↔ `isAssignedApprover ∧ isActionable`) — replacing rev-3's fictitious full-visibility LIST parity**; `grep findVisibleTo backend/src` → 0; live-HTTP `allowedActions` == golden.
- W8/W9: golden dual-leg with read-only assertion paths (regeneration only via the separate explicit command).

**E2E**
- No new browser E2E. `EcommerceE2ETest` + FlowITs remain the net inside R25. `(cd frontend && npm run build)` stays green.

**Observability**
- `ax-ledger-log.sh progress gate=<item> outcome=pass` per closure.
- `coverage-report.sh --write` exactly once in Phase 4 (0 downgrades).
- R25 audit entry bound to the committed final SHA; per-ref `--expect-sha` at push; codex approval persisted as SHA-bound `.ax-verify/codex-<sha>.json` (root-ignored, `.gitignore:26`).

**Non-vacuity discipline**
- WS-W: the exact backlog reproductions (webhook enum revert; W1's FOUR per-surface restores incl. the dedicated skeleton-SUCCESS demo; ALL-param pre-fix behaviors); W6a's THREE mutation proofs on live paths only (**invariant: no test may protect an unused repository policy** — `findVisibleTo` is deleted, not test-wrapped); goldens compared read-only against production bytes.
- WS-G: G1's before-fix run FAILs on `currency-input.tsx` and PASSes on `currency-formatter.tsx` (positive control), both recorded.
- WS-T: one re-inline flip (guard) + one branch-deletion flip (vitest) per lane batch.
- W11 runs under default `-da`.

---

## 5. Pre-mortem (3 scenarios, grounded in this session's history)

**PM-1 — Parallel lanes ship BE tests that first break inside the final R25.**
- History: 2026-07-14 gradle-lock spinlock; wave-3 lanes without gradle shipped Java that first compiled in R25.
- Leading indicator: any non-gradle-owner lane writes a `.java` file and claims done-when without a recorded `(cd backend && ./gradlew test<Domain>)` output in its lane note.
- Mitigation: ALL java items live in the single serial WS-W lane (plus W11); other lanes never claim java done-whens; the owner runs the owning per-domain task per batch; R25 FULL once at the end. WS-T proves with `(cd frontend && npx vitest run <file>)`.

**PM-2 — A heuristic guard (enum-parity, vocab_scan, check-7-content, react dialect) triggers a codex ping-pong.**
- History: 13 codex rounds on a heuristic FE lint (P3-55 is the fossil); rule: same-theme distinct ×3 → immediate batch-audit.
- Leading indicator: first codex round returns ≥2 distinct findings against one guard's classification behavior.
- Mitigation: (i) design de-risks — W2 separates classification (exhaustive manifest; unclassified=FAIL) from extraction/vocab_scan (deterministic word-boundary grep, fixture-locked incl. skeleton-SUCCESS); G2's dialect is FROZEN with per-clause fixtures; G3 is a documented necessary-not-sufficient floor. (ii) If the indicator fires: `/batch-audit` on that guard's decision surface (3–4 lenses), never a third 1-by-1 round.

**PM-3 — Bookkeeping/commit-ordering breaks: BACKLOG integrity guard, coverage-map guard, doc-count drift, or an uncommitted/post-R25 change invalidates the push.**
- History: integrity guard blocked a push over a table-aggregation miss (2026-07-10); sibling-ids must be parenthesized; doc counts drift with every guard; pre-push `--expect-sha` makes ANY post-R25 commit an AUDIT_STALE_HEAD.
- Leading indicator: two lanes stage edits to `docs/BACKLOG.md`/`coverage-map.yaml`/count lines; a finding "fixed inline" without a row; a dirty worktree at R25 start; any commit after R25.
- Mitigation: Phase 4 single-writer covers ALL FOUR `doc_headline_count_guard.sh` sites, recounted from disk; **Phase 4 ends with a COMMIT of the fully integrated tree + `git status --porcelain` empty + `SHA_final` recorded; R25 runs at exactly that SHA**; post-R25 fixes are committed first, then a full R25 re-run at the new head.

---

## 6. Execution sequencing for /ralph

Hard constraints: ONE gradle owner; BE validation centralized in final R25; bookkeeping single-writer; **R25 FULL PASS at the committed SHA that is pushed**; codex xhigh LAST with a persisted receipt.

**Commit/R25/push invariant:**
1. Phase 4 finishes by **committing the fully integrated tree** — `git status --porcelain` MUST be empty; record `SHA_final = git rev-parse HEAD`.
2. Phase 5 runs R25 FULL at `SHA_final` (clean worktree verified). Audit entry binds to `SHA_final`, `full_run=true`.
3. ZERO commits after R25. Any forced change: **commit fix → new `SHA_final'` → full R25 re-run at `SHA_final'` → codex re-trigger at `SHA_final'`**. Push only when push-head == last-R25 SHA == codex-approved SHA. Pre-push per-ref `--expect-sha` (AUDIT_STALE_HEAD / AUDIT_PARTIAL_RUN / hard_fail) enforces mechanically; /ralph treats "commit after R25 without re-run" as a hard error.

**Phase 0 — setup (main loop).** Re-read `gotchas.md`; confirm worktree clean at 50fa467; `export PATH="$HOME/.pyshim:$PATH"`; announce lane map + file ownership.

**Phase 1 — parallel non-gradle lanes.**
- Lane G (sonnet): **G1 first** (before-fix FAIL-on-`currency-input` + PASS-on-`currency-formatter`) → G2 (census → remediations → fixtures) → G3 → G5 → **G4 last** (header count from disk). Proof: `run-all-guards.sh` + fixtures.
- Lane T1 (sonnet): T2, T3, T4, T5, T6. Proof: targeted `(cd frontend && npx vitest run …)` + lint.
- Lanes T2–T3 (sonnet ×2): T1's 5 verticals (webhook+approval-workflow one shard, file-interlocked with W1/W7; payment+email-outbox+crud the other) + guard + ledger + docs. Proof per shard: vitests + `(cd frontend && npm run build)` on merge.
- Lane E (haiku/sonnet): E1 (exact blocks per §3 — note the (b) edit is under `practices/`, pre-commit gates fire).
- Gate 1: main loop reviews lane notes; each lane shows its demonstrated RED flip; G1's dual before-fix evidence present.

**Phase 2 — gradle-owner serial track (opus for W5/W6a/W6b, sonnet otherwise).**
Order: W1 → W2 (born green; 57/57 + all four vocab_scan surfaces incl. skeleton) → W3 → W4 → W5 (narrow widening in `findVisibleTo`, still live + negatives + golden pair) → **W6a Step-0 characterization commit** → W6a extraction (single-GET → `canView`; **delete `findVisibleTo`**; real-selector contract tests; three mutation proofs) → W6b → W7 → W8 → W9 → W10 (bound-candidate sweep or Phase-3 fallback) → W11. Run each owning per-domain task as it lands; NO aggregate until Phase 5.
- Gate 2: all touched per-domain tasks GREEN; `(cd frontend && npm run test)` + `(cd frontend && npm run lint)` green; W6a's three mutation proofs + `grep findVisibleTo backend/src` → 0 recorded; W1's four per-surface guard REDs demonstrated.

**Phase 3 — adversarial review (opus, read-only) + adjudication.** Lenses: (i) authz semantics W5–W7 (mandatory — verify the narrow-widening negatives, the real-selector contract tests, and that no test binds to deleted/unused paths); (ii) guard non-vacuity (WS-G + W2 incl. skeleton lock); (iii) contract/wire compatibility (W3/W4); (iv) L4 extraction fidelity (2 of 5 verticals). **Adjudication**: S2.AUTHZ.XB only — three reasons one-by-one; default no-flip; W6a gate checked. W10's <8-domain fallback decided here. Findings: fix if in-surface, register if new.

**Phase 4 — bookkeeping (single writer; ends in the invariant's COMMIT).** BACKLOG: 22 rows → `[x]` + T1 remainder row (+ Phase-3 registrations; sibling-ids parenthesized); tier table re-aggregated. Coverage-map: only the adjudicated AUTHZ.XB flip (if granted); `coverage_map_guard.sh` + `coverage-report.sh --write` (0 downgrades). Doc counts recounted from disk at ALL FOUR sites (expected 94→97; disk recount normative). Ledger events. **Commit → clean-worktree check → record `SHA_final`.**

**Phase 5 — R25 FULL (gradle owner).** `JAVA_HOME=… bash practices/scripts/verify-completion.sh` at `SHA_final` — full run, no `--step`. Known flake (R22 ContextCache / testRateLimit dead-port): isolate-confirm then re-run; never paper over unknown failures.

**Phase 6 — codex xhigh (LAST gate, receipted).** Single-pass `codex exec` (`< /dev/null`, `-c model="gpt-5.6-sol"`, effort xhigh) over the `SHA_final` diff. **Persist `.ax-verify/codex-<SHA_final>.json`: model, effort, verdict, timestamp, reviewed sha.** P2+ findings: commit fix → new SHA → affected task → Phase-4 delta if needed → R25 FULL at new SHA → codex re-trigger + new receipt. Same-theme distinct ×3 → batch-audit (PM-2). APPROVE receipt required at the exact push head.

**Phase 7 — push + wrap.** Push to main (pre-push per-ref `--expect-sha` enforces); `ax-ledger-review.sh`; session memory update; worktree cleanup.

---

## 7. Acceptance criteria (binary)

1. All 22 target rows `[x]` with closure refs. Denominator = 208 + 1 (T1 remainder row) + N (Phase-3/codex registrations) → tier table shows **208/(209+N)**; summary line states both. Expected **208/209 ≈ 99.5%** at N=0.
2. `coverage-report.sh` prints **0 honesty downgrades** and **C_total ∈ {0.7786, 0.7863}** — default 0.7786; 0.7863 only with the S2.AUTHZ.XB adjudication (gated on W6a) written into the cell `notes`. No other value permitted. No-flip is a VALID PASS. S1/S3 unchanged.
3. `coverage_map_guard.sh --fixtures` PASS incl. the G3 content fixture and G5 check-8 fixture.
4. `run-all-guards.sh --include-fixtures` PASS with new steps present: react-dialect substance (four clause-negatives exit 1), contract_enum_parity (fail_drift + fail_unmapped_enum + fail_vocab_scan incl. skeleton-SUCCESS exit 1), l4_presentational_view, l2_frontmatter_deps.
5. Every row reproduction fails RED when re-applied — Phase-3 spot-checks at minimum: webhook enum revert (W2); **each of W1's FOUR surface-restores individually, incl. the dedicated skeleton-`SUCCESS` restore** ⇒ guard exit 1; `?status=ALL` pre-fix behavior (W3); W6a's three mutation proofs on live paths (ordering / ownership incl. requester-list contract test / visibility incl. single-GET canView), with `grep findVisibleTo backend/src` → 0; fabricated `currency-input` quote incl. the before-fix FAIL and formatter positive-control PASS (G1); `booking-flow.spec.ts` rename bypass (G3); BHD removal (T4).
6. **Commit-ordering invariant held**: push head SHA == R25 FULL audit SHA (per-ref `--expect-sha` green) == the SHA in `.ax-verify/codex-<sha>.json` APPROVE receipt; zero commits after the last R25; `git status --porcelain` empty at R25 start.
7. `doc_headline_count_guard.sh` PASS with all four sites updated; counts match disk recount.
8. R26 clean (`private_boundary_guard` PASS inside R25).

## 8. ADR

- **Decision**: One "ledger-zero" wave closing all 22 registered rows — two right-sized with registered remainders (P2-28; P3-54 with bound candidates + fallback) — executed as four defect-class workstreams; the wire-contract-binding seam is one serial gradle lane sharing a single read-only-golden parity idiom. Flip candidacy limited to S2.AUTHZ.XB, adjudicated post-hoc (default none). S1-XB/S3 deferred.
- **Drivers**: ledger convergence is the north-star metric; P2 rows concentrate authz/contract enforcement escapes incl. a live fabricated evidence anchor; the S1-XB/S3 mass (25.0 weighted) is structurally serial and incompatible with a 22-item parallel wave.
- **Alternatives**: (B) bundle coverage convergence — rejected: 2–4× size, gradle-serial, unreviewable in one codex pass. (C) P2-only — rejected on the driver alone.
- **Why chosen**: highest certainty-per-token; written done-whens throughout; the three known failure modes have tested mitigations; the seam grouping gives the reviewer one idiom.
- **Consequences**: C_total ∈ {0.7786, 0.7863} honestly; remaining map loss cleanly two-shaped; +3 run-all guards + 1 extended engine check recounted at four doc sites; denominator grows ≥1; W5 widens read visibility narrowly (acted approvers only, negatives enforced); W6a leaves approval authz as named shared predicates AND **removes `findVisibleTo` (dead-policy elimination — the read paths now share one visibility source of truth)**; goldens gain a read-only assertion discipline.
- **Follow-ups (registered, not in-wave)**: (i) wave-5 ralplan: S3 capstones + S1-XB legs (W10's bound sweep pre-pays the pagination leg; W2 pre-pays contract-parity legs); (ii) T1 remainder (~18 verticals); (iii) `fractionDigitsFor` → `@ax/core`; (iv) request-schema→runtime-payload harness (may reopen S2.INPUT-VALIDATION.XB); (v) `proposed-cells.yaml` + orchestrator stay out.

### Amendment disposition — rev 2 (Architect)

A1/A2/A3/A4/A7/A9/A10 applied in full; A5/A6/A8 applied-adapted; P2-40 re-rank, WS-C rationale, P2-39 precedent, P3-60 honesty applied. Rejected: none.

### Iteration disposition — rev 3 → rev 4 (codex Critic)

**Amendment-integrity correction (acknowledged):** rev 3's table over-claimed #2, #6, and #10 as "Applied in full"; the Critic re-pass held all three PARTIAL. The rows below state the true prior status and the rev-4 remedy. All other items (plan-error/currency-formatter, 1, 3, 4, 5, 7, 8, 9, 11, 12) were confirmed APPLIED by the Critic re-pass and are unchanged in rev 4.

| # | rev-3 true status | rev-4 remedy |
|---|---|---|
| 2 (W6a extraction) | **PARTIAL** — the "JPQL↔canView parity matrix for LIST" targeted `findVisibleTo`, which is ID-scoped single-GET only; after canView adoption it is dead code, so the test would have protected an unused repository policy (the very defect class W6a closes). | `findVisibleTo` **deleted** as an explicit step in the same change that switches single-GET to `canView` (grep-checked 0); real-selector contract tests added against `findByRequesterUserIdOrderByCreatedAtDesc` (requester list ↔ `isRequester`) and `findInboxFor` (inbox ↔ `isAssignedApprover ∧ isActionable`); mutation proofs re-targeted to live paths; invariant stated: **no test may protect an unused repository policy** (also lifted into Principles §1.4). |
| 6 (skeleton lock) | **PARTIAL** — forbidden set was only `IN_FLIGHT`/`DEAD_LETTER` and `require_all` covered the TS union only; restoring the skeleton's real drift token `SUCCESS` left the guard GREEN (3 of 4 surfaces locked). | Skeleton entry now `require_all: true` **AND** `forbidden: [SUCCESS, IN_FLIGHT, DEAD_LETTER]` (word-boundary matching; `SUCCESS` ⊄ `SUCCEEDED`); a **dedicated skeleton-SUCCESS restore RED demonstration** joins the other three per-surface proofs (W1, acceptance 5); `fail_vocab_scan` fixture includes the skeleton case. |
| 10 (verification concreteness) | **PARTIAL** — one candidate endpoint was fictional ("session-management admin list"; `AdminSessionController.java:33` is DELETE-only); candidates were not bound to contract operations; E1(b)'s destination was undecided ("webhook rule/manifest") while the manifest excludes inbound receivers. | W10 candidate #7 corrected to **`GET /sessions`** (`SessionController.java:44`, `listSessions`, `SessionListResponse`); **all 12 candidates bound in a table**: endpoint → contract file → operationId → envelope schema → owning gradle task (favorites file corrected to `favorites-bookmarks-openapi.yaml`); E1(b) destination decided: **`practices/rules/webhook-hmac-required.md`** (the file `blueprints/webhook-manifest.yaml:19` itself scopes inbound to), with the full `replay_dedup_marking:` YAML block inlined verbatim. |

**Rejected items: none.** One standing adaptation from rev 3 (#6's in-guard `vocab_scan` kind instead of a separate guard) is retained — now with the skeleton surface genuinely locked.
