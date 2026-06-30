# STO Generic Phase-1 Seams — Design

**Date:** 2026-06-28
**Topic:** sto-generic-seams
**Status:** Design (approved) — drives ultragoal plan + implementation
**Worktree:** `ax-template-sto-seams` (branch `feat/sto-generic-seams`, off `feat/sto-transfer-pilot` = TRANSFER+REGISTER)
**Predecessor:** `docs/superpowers/specs/2026-06-27-tokenized-securities-strategy-design.md` (§3 named 3 invariants; this realizes the 2 deferred ones + 1 issuance lifecycle)

---

## 0. Why these exist — "generic now" vs "fork-only"

ax-template's pattern is **absorb the chain-agnostic invariant + leave a typed SPI seam; the fork
plugs the concrete chain/vendor into the seam** (exactly how `InvestorEligibility` works — the fork
swaps in ONCHAINID/KYC). So most "Phase 1" capabilities split into a generic half (invariant + SPI +
composition, buildable NOW and testable with a test-double) and a concrete half (the actual chain
contract / vendor SDK / license, fork-only). This design realizes the three generic halves that have
**zero company dependency**. Honest boundary: invariants are proven now with test-doubles; "equivalence
with a real chain" is a fork concern, but "invariant holds + the seam is in place" is binary now.

---

## 1. Seam A — holder-authorization (`HOLDER-AUTHZ`)

**Problem closed:** the pilot lets any authenticated MEMBER transfer from any `fromHolderId` (the one
MEDIUM from the pilot's code review). A transfer initiator must be authorized for the debited holder.

**SPI:** `HolderAuthorization { boolean controls(String callerPrincipal, String holderId); }`
- Default impl `OwnershipHolderAuthorization` ← `HolderOwnership` aggregate (callerPrincipal, holderId;
  unique on (holder_id) — a holder is controlled by exactly one principal). **fail-closed**: no row ⇒ not controlled.
- A fork swaps this for on-chain identity (ONCHAINID) — the seam is the swap point.
- Supports **custodial / omnibus** holders (one principal controls many holders) — real in securities.

**Ownership establishment:** `POST /api/security-tokens/holders/{holderId}/ownership` — self-claim by the
caller (first-claim-wins; a second principal claiming an owned holder → 409). ADMIN may assign on behalf
(audit). Claiming is idempotent for the same principal.

**Invariant + spec items:**
- **HOLDER-AUTHZ-001** — a transfer whose `fromHolderId` is NOT controlled by the caller is rejected
  **403**, register unchanged. (closes the pilot MEDIUM)
- **HOLDER-AUTHZ-002** — deny-by-default: with no ownership claim a holder is uncontrolled; a claim
  enables the controlling principal; a different principal cannot transfer from it. First-claim-wins (409 on conflict).

**Composition:** `common/CallerScope` (caller principal = `Authentication.getName()`); pattern mirrors
`EligibleInvestor`/grant.
**Transfer-gate change:** add, as the FIRST gate in `transfer(...)` (before lock-up etc.), a
`holderAuthorization.controls(caller, fromHolderId)` check → 403 if false (still fail-closed, before any mutation).
**Pilot impact (intentional):** transfer tests claim ownership of each sender holder before transferring
(e.g. the `member` caller claims `ISSUER`/`ALICE`), exactly like granting eligibility.

---

## 2. Seam B — on-chain/off-chain consistency (`ANCHOR`)

**Problem:** the distributed ledger is the legal book; the off-chain register must converge with it. In
Phase 0 (no chain) we model the off-chain register as truth + an anchor seam, and prove convergence with
a test-double anchor that can be made to diverge.

**SPI:** `OnChainAnchor { String anchor(AnchorIntent intent); }` returning a tx-ref.
- Default impl `InMemoryAnchor` — records each anchored transfer keyed by `transferId`, returns a
  deterministic synthetic ref; exposes its recorded view for reconciliation. A fork swaps in the real chain client.
- `TransferEntry` gains an immutable `anchorRef` (the returned ref), set when the transfer is applied.

**⚑ Decision (approved):** Phase 0 = **off-chain register is the source of truth; the anchor is a
write-through mirror.** Every applied transfer is anchored (anchor() called inside the same transaction,
ref stored on the entry). The fork later promotes the chain to authoritative; the invariant (register and
anchor must agree, divergence is a *detected break*, never silent) is identical either way.

**Invariant + spec items:**
- **ANCHOR-001** — every applied transfer carries a non-null `anchorRef` from `OnChainAnchor.anchor(...)`;
  an entry without an anchor ref is unrepresentable (no transfer commits without anchoring).
- **ANCHOR-002** — reconciliation detects register↔anchor divergence: a `reconcile(tokenCode)` operation
  compares the register's applied transfers against the anchor's recorded view and reports any divergence
  as a **break** (composing the `reconciliation` domain), **never silently accepted** (fail-closed). Tested
  by a test-double anchor that drops/mutates a record → reconcile flags the break.

**Composition:** the existing `reconciliation` domain (break classify/dispose). `OnChainAnchor` is the new seam.
**Pilot impact:** `transfer(...)` calls `anchor()` and stores `anchorRef`; `TransferEntry` + migration gain
the column. Moderate, additive (does not change the gate ordering).

---

## 3. Seam C — issuance lifecycle (`ISSUE-LIFECYCLE`)

**Problem:** the pilot creates ready-to-transfer tokens; a real security is ISSUED through a lifecycle
before it trades.

**Model:** add `IssuanceStatus { DRAFT, ISSUED }` to `SecurityTokenRegister` + a
`SecurityTokenIssuanceStateMachine` (one-way `DRAFT → ISSUED`, sole mutator of status).
- `createToken(...)` creates **DRAFT** (no issuer holding seeded yet).
- `issue(tokenCode)` (ADMIN) transitions DRAFT→ISSUED and seeds the issuer holding = `totalUnits`
  (conservation established at issuance). One-way (no un-issue).
- `transfer(...)` requires status ISSUED → a transfer against a DRAFT token is rejected.

**⚑ Decision (approved):** lifecycle only (DRAFT→ISSUED, ADMIN-gated). Composing `approval-workflow`
4-eyes is a further refinement, deferred (YAGNI for the seam).

**Invariant + spec items:**
- **ISSUE-001** — a DRAFT token cannot be transferred (transfer against DRAFT → **409**); only after
  `issue()` (ISSUED) does transfer proceed to the gates.
- **ISSUE-002** — `issue()` is ADMIN-only, one-way (re-issue / un-issue refused 409), and seeds the
  issuer holding = totalUnits exactly once (Σ holdings == totalUnits established at issuance).

**Composition:** approval-workflow (deferred). State machine mirrors `ThresholdRegisterStateMachine` (one-way).
**Pilot impact (intentional):** `createToken` now yields DRAFT → transfer tests call `issue()` (as ADMIN)
before transferring. Seeding moves from create-time to issue-time.

---

## 4. Interaction order (the composed transfer gate, after all 3 seams)

`transfer(...)`: load+PESSIMISTIC_WRITE → **status==ISSUED?** (ISSUE-001) → idempotency replay →
**caller controls fromHolderId?** (HOLDER-AUTHZ-001) → units>0 → lock-up → balance → eligibility →
holding-limit → applyTransfer → **anchor()** (ANCHOR-001) → save. Every gate fail-closed, before mutation.
(`reconcile()` is a separate read path for ANCHOR-002.)

---

## 5. Enforcement + verification plan (REQUIRED — built into every story)

Each seam is one story; every story runs this loop (no exceptions):
1. **TDD-first** (`/tdd-workflow`): write the failing ComplianceTest + ViolationProof assertions FIRST
   (RED), then implement to GREEN. The `@Tag("<ITEM-ID>")` binds each spec item.
2. **verification-loop** (`/verification-loop`): structured verification of the change (behavior runs,
   not just compile).
3. **ax-template feedback loop / checklist enforcement** — the catalog's mechanical gates ARE the
   enforcement: `./gradlew testTokenizedSecurities --no-daemon` GREEN + `run-all-guards.sh` GREEN +
   **R25 `verify-completion.sh` PASS** (the Iron Law). New domain-registration surfaces (spec items bound,
   doc counts, AGENTS regen if a rule is added, verification-checklist coverage) updated per story.
4. **Adversarial review** (separate pass — verifier/code-reviewer subagent, refute-by-default) before
   checkpoint — catches green-but-hollow (non-vacuity: would the test fail if the gate were removed?).
5. **ultragoal checkpoint** with evidence; the durable ledger records start/checkpoint per story.

## 6. Dogfooding (after all 3 implemented + verified — REQUIRED)

Compose a **realistic end-to-end STO flow** as a dogfood exercise (not just unit tests):
`createToken (DRAFT) → issue (ADMIN, ISSUED) → claim holder ownership → grant eligibility →
anchored compliant transfer → reconcile (clean) → inject a divergence → reconcile (break flagged)`.
Run it as a persona would; classify every friction/gap into `docs/dogfood-ledger/sto-generic-seams-iter1.*`
(the catalog's dogfood-ledger pattern). Close real bugs found (iterate until the iteration is dry). This is
the "real implementation verification" beyond unit coverage — it has repeatedly surfaced bugs unit tests miss.

## 7. Final quality gate

ai-slop-cleaner (reviewer-only) + verification + `/code-review` (cloud or local) — all clean — then the
ultragoal final checkpoint records the quality-gate evidence. Not done until clean + R25 PASS at HEAD.

---

## 8. Completion criteria (per story + overall)

- Each seam: spec items bound + ComplianceTest + ViolationProof GREEN + `./gradlew testTokenizedSecurities`
  GREEN + run-all-guards GREEN + R25 PASS + adversarial review APPROVED + ultragoal checkpoint.
- Overall: 3 seams merged on `feat/sto-generic-seams`, dogfood-ledger iteration dry, final gate clean,
  ultragoal final checkpoint with quality-gate-json.
- Scope boundary preserved: every SPI default is a chain-agnostic test-double; NO concrete chain/vendor/
  license code (that stays fork-only). Honest Phase-1 disclosures kept in spec + READMEs.
