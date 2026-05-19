# Codex Critic R9 iter 1

## Verdict

ITERATE.

The R9 draft is close and the 3-SP Synthesis-B shape is execution-sensible: SP45 webhook L4, SP45b internal-it, SP46 final/partial-tag policy. I do not require Synthesis-A. However, the Architect's 2 HIGH + 4 MEDIUM findings all stand and should be applied verbatim before approval.

I found one new issue Architect did not call out. I grade it INFORMATIONAL, not blocking: the webhook HMAC primitive is directionally covered by existing repo rules and RFC 2104, but R9 should explicitly bind or explain that relationship so TD-025 is not weaker than the existing inbound-webhook security rule.

## Architect findings disposition

1. **H1 — TD-027 condition (c) self-fulfilling: STANDS.** PRD `:404-405` allows a candidate L4 to qualify because one in-scope recipe is written beside it. Tighten to Architect's two-signal test: shipped active consumer plus plausible future/deferred candidate.

2. **H2 — DECISIONS.md format drift: STANDS.** `practices/DECISIONS.md` uses `## <RULE_ID>` structured entries and has no `TD-2026-*` entries. R9 proposes appending TD-025/026/027 without specifying the format retrofit.

3. **M3 — §4.4 redundant evidence summary: STANDS.** The full ledger plus density floor is enough; the compact summary repeats the same facts and has confusing downgrade arithmetic.

4. **M4 — webhook-domain assertion-count semantics: STANDS.** `>=8/15 assertions PASS` is new wording; cite or mirror `skills/_tests/L4/scheduler-domain.test.sh` semantics directly.

5. **M5 — INV-005 promotion criterion ambiguous: STANDS.** `payment-callback-secret` is hypothetical. Mark promotion deferred until cross-domain demand exists, or name a real candidate.

6. **M6 — Partial-tag catalog desync window: STANDS.** Webhook README keeps `applied_recipes: [internal-it]` while `_MANIFEST.yaml` may mark internal-it `active-verdict-pending`. Add the inline annotation Architect requested.

## Criterion findings (A-L)

**A. Principle-Option consistency — PARTIAL.** Composition-kit, recipe-no-code, and Tier caps are consistent. The weak TD-027 gate conflicts with the catalog-discipline principle until H1 is fixed.

**B. Fair alternatives — PASS.** Three options are described with bounded pros/cons, and Option 3 has explicit Synthesis-B rationale tied to R7.

**C. Risk mitigation clarity — PASS-PARTIAL.** Pre-flight, rollback, partial-tag, and stop conditions are concrete. M6 still needs the catalog-desync annotation.

**D. Testable acceptance binary — PASS.** `/ax-verify`, recipe guards, webhook-domain test, sealed verdict thresholds, and tag policy are binary.

**E. Concrete verification — PASS with notes.** Existing verification surfaces resolve: `skills/ax-verify/SKILL.md`, `skills/ax-verify-domain/SKILL.md`, `recipe_governance_guard.sh`, `recipe_spec_referential_integrity_guard.sh`, and `skills/_tests/L4/scheduler-domain.test.sh` exist. Webhook/internal-it sealed verdict files are planned deliverables, not expected on disk yet.

Specific disk checks: `ls templates/L4 | wc -l` returns 11; `templates/L4/webhook` and `specs/webhook*` are absent; sealed verdict count is 10; audit-log and notification anchors resolve at the cited lines. `specs/crud-l0.yaml` is absent, but R9 does not cite it; current CRUD anchors live in `specs/crud-security.yaml`.

**F. TDD anchor concreteness — PASS.** Webhook and internal-it each name a test file, RED reason, first GREEN command, and owning SP. INV-005 has a co-shipped invariant test path.

**G. Pre-mortem adequacy — PASS.** DELIBERATE mode minimum is 3; R9 has 4 real scenarios with likelihood/impact/mitigation.

**H. Expanded test plan — PASS-PARTIAL.** The plan covers Spec Trio structure, recipe guards, compose tests, webhook-domain test, sealed verdicts, and `/ax-verify`. Apply M4/M6 before final.

**I. Architect findings disposition — ITERATE.** All 6 findings stand.

**J. CLAUDE.md anti-patterns — PASS.** No governance process, no MockMvc-only mandate, no fork-team git/CI policy enforcement, no `RECIPE_DEVIATION.md`, and recipe-no-code scope is preserved.

**K. Autonomous safety — PASS-PARTIAL.** No destructive ops; rollback is per-SP; SP45b is gated on SP45. The partial-tag desync annotation is still required.

**L. Independent steelman — INFORMATIONAL.** See next section.

Additional requested checks:

- Korean ledger has **2 verbatim PASS** rows (Toss + Naver Works), matching R8's 2-PASS level.
- §4.4 has well over the minimum logical attempts: 5 English source families plus 3 Korean source families, with alternate/redirect rows.
- `l2_blocks_used` for internal-it all resolve to files under `templates/L2/blocks/*.tsx`: `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `notification-bell`, `notification-list`, `confirm-dialog`.

## My steelman attack (one new)

**INFORMATIONAL — webhook HMAC signing should explicitly bind to existing HMAC/RFC anchors or state why outbound signing is separate.**

R9's webhook spec plans `WEBHOOK-SIGN-001` and `WEBHOOK-SIGN-002` around `HMAC-SHA256(secret, body)` and timestamp-covered signatures (`PRD :177-179`). The evidence quotes from GitHub/Stripe establish webhook delivery and signing/retry semantics, but the PRD does not explicitly cite the repo's existing security anchor for HMAC.

Disk evidence: `practices/rules/webhook-hmac-required.md` already cites GitHub validation guidance, OWASP ASVS V13.2.6, and **RFC 2104**; `specs/spring-practices-l0.yaml:103-109` already has `PRACTICES-INTEG-001` for HMAC-SHA256 webhook verification. Those are inbound receiver rules, while R9 is defining outbound sender signing. That distinction is enough to avoid a blocker, but TD-025 should say either:

- outbound webhook signing deliberately reuses the same HMAC/RFC 2104 cryptographic anchor, while receiver verification remains covered by `PRACTICES-INTEG-001`, or
- outbound sender signing is a distinct catalog axis and will be anchored by the new `specs/webhook-l0.yaml` plus RFC 2104 in the SP45 evidence snapshot.

Without that sentence, the new L4's core security mechanism is less explicitly standards-anchored than the repo's existing inbound webhook rule.

## Hard blockers

- Apply Architect H1: tighten TD-027 condition (c) to require two consumer signals.
- Apply Architect H2: define how TD-025/026/027 fit `practices/DECISIONS.md` format before SP45/SP45b.
- Apply Architect M3-M6 verbatim before approval; they are small but part of the iter-1 acceptance gate.

## Soft suggestions

- Fix the compact evidence-summary downgrade count; the underlying ledger is strong, but the row arithmetic is hard to audit.
- Add one sentence near the `specs/crud-l0.yaml` disk check or internal-it invariants explaining that CRUD anchors use `specs/crud-security.yaml`, not a `crud-l0` file.
- In TD-025, mention the existing `webhook-hmac-required` rule/RFC 2104 relationship so future reviewers do not re-open the HMAC anchoring question.

## Re-review trigger

Re-review after iter 2:

- closes all 2 HIGH + 4 MEDIUM architect findings,
- preserves the 3-SP Synthesis-B plan,
- keeps internal-it L2 inventory disk-resolvable,
- clarifies CRUD spec path naming,
- and adds the outbound webhook HMAC/RFC anchoring sentence or a deliberate separation rationale.

## ADR-ready (if APPROVE)

Not ADR-ready yet. TD-025/026/027 are directionally useful, but TD-027 needs the two-signal condition and DECISIONS.md format must be resolved before approval.
