# Codex Critic Review — P1 Absorption PRD (Round 4, Iteration 1)

## Verdict: ITERATE

The PRD is directionally salvageable, but not approval-safe for autonomous execution. I ratify the Architect's four critical concerns: SP30/SP31 currently use a draft-spec loophole around billing, SP32 creates a regulated backend domain without a `backend_only` Spec Trio registration, the DELIBERATE verification matrix omits the required `observability_signal` column, and the parallel execution plan omits shared manifest/sentinel race thresholds. I also found one new blocker: SP32's business-registration TDD anchor uses a "known-valid mock" instead of the public/official fixture set required by its own pre-mortem.

## Architect findings disposition

1. **SP30+SP31 atomic collapse: agree.** PRD lines 153-154 and 301-323 split billing L1/L2/L3 + Spec Trio drafts from the billing L4/backend/allowlist close. The inherited rule at lines 23 and 164 says Spec Trio lands before or atomically with the backend/L4 domain it governs. Prior precedent is stricter: Catalog Extension rejected surface splitting because it violates atomic ordering (`2026-05-18-catalog-extension-prd.md:24-32`) and SP16/SP17/SP18/SP19 shipped domain trios atomically (`2026-05-18-catalog-extension-prd.md:286-379`); Functional Extension did the same for search and feature-flags (`2026-05-18-functional-extension-prd.md:168,597-633,715-750`). Collapse SP30+SP31, or move every billing-shaped artifact (`pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`, `pricing-page`, billing trio drafts) into the atomic billing SP while leaving only truly generic L1 primitives in a predecessor.
2. **`identity-verification: backend_only` allowlist: agree.** SP32 creates `templates/backend/identity-verification/` with controller/service/provider/adapters/DTO and a verified identity row (`2026-05-18-p1-absorption-prd.draft.md:155,333-338`) but §6.2 allows only `billing: full_trio` in `trio_integrity_allowlist.yaml` (`...draft.md:413-414`). The current allowlist explicitly supports `backend_only` and already uses it for `ratelimit`, `email-outbox`, and `scheduled-task` (`practices/evals/trio_integrity_allowlist.yaml:1-24`). A CI/DI callback backend handling regulated identity data is not less deserving of a backend Spec Trio than scheduled tasks.
3. **Verification Matrix `observability_signal`: agree.** §5.8 has columns only for SP, artifacts, verify skill, TDD anchor, first green command, and rollback (`...draft.md:385-394`). Prior approved PRDs used an `observability_signal` column (`2026-05-18-functional-extension-prd.md:803-808`; `2026-05-18-catalog-extension-prd.md:520`). DELIBERATE mode is not satisfied without signals for billing events, identity-verification audit hooks, rich-content mount errors, saved-view persistence violations, and impersonation/admin audit hooks.
4. **§6.4 sentinel race threshold: agree.** The ownership table lists snapshot writes (`...draft.md:417-418`) but does not list `practices/upstream/_MANIFEST.yaml`, `practices-react/upstream/_MANIFEST.yaml`, `practices/AGENTS.md`, or `practices-react/AGENTS.md`, all of which exist in the repo. The PRD then says `app-shell.tsx` is the only conflict surface (`...draft.md:421`), which is false for SP32/SP33/SP34 parallel rule/snapshot work. Add explicit ownership and halt/serialize thresholds.

Plus 7 dimensional verdicts:

| Dimension | Architect verdict | Codex disposition |
|---|---|---|
| (a) SP atomicity | WEAK | **Agree, blocking.** SP30/SP31 split and SP32 missing backend_only trio both break the atomic ordering pattern. |
| (b) Verification closure | PASS-WEAK | **Agree, but downgrade to WEAK until SP32 is classified.** Existing verify skills are present, but `identity-verification` cannot be verified as a domain until allowlist + backend Spec Trio exist. |
| (c) Evidence chain density | PASS | **Agree.** The draft names evidence anchors for templates/rules, but the SP32 TDD anchor must use official fixture data rather than a mock. |
| (d) Anti-pattern resistance | PASS-WEAK | **Agree.** No governance/release/CI creep and RRN-protective framing is right; impersonation rule bypass-via-renaming remains a should-fix. |
| (e) TDD anchor concreteness | PASS | **Disagree: PASS-WEAK.** Most rows are concrete, but SP30 has no RED for the Spec Trio drafts and SP32's "known-valid mock" conflicts with its own 100+ sample requirement. |
| (f) Korean enterprise | PASS-WEAK | **Agree.** 사업자등록번호 + CI/DI are defensible as opt-in primitives; NICE/SCI deferral and fixture provenance need sharper wording. |
| (g) Parallelizability | WEAK | **Agree, blocking.** `_MANIFEST.yaml` and AGENTS sentinel ownership are missing. |

## Criterion-by-criterion findings (A-L)

### A. Principle-Option consistency - WEAK

The principles are mostly coherent: composition-kit, evidence-anchoring, binary verification, Tier-1 cap, and no raw RRN all map to the chosen Option B+C hybrid (`...draft.md:17-24,63-81`). The weak point is the Atomic Spec-Trio principle. The recommendation says SP30 creates billing-facing artifacts and drafts, then SP31 closes the trio (`...draft.md:67-71,153-154`). That does not satisfy the stricter interpretation inherited from the prior PRDs, where domain-shaped UI/backend/spec artifacts land together.

### B. Fair alternatives - WEAK

Options A-E are not strawmen, but the most relevant alternative is missing: "strict atomic billing SP plus defer polish clusters." Architect correctly identified this gap (`2026-05-18-p1-absorption-architect-review.md:36-47`). Option A is rejected as an unthemed sweep (`...draft.md:38-42`), yet the recommendation absorbs billing, Korean specials, rich content, tables/filters, and admin polish in one cycle (`...draft.md:63-81`). Add Option F and reject it explicitly if Planner still chooses the hybrid.

### C. Risk mitigation clarity - FAIL

The §7 pre-mortem has thresholds, but SP-card risks are not consistently executable with owner + command + threshold. Examples:

- SP30 currency-input collision names a test but no command/threshold (`...draft.md:310`).
- SP31 provider/state-machine/atomic risks name tests but no owner and only partial thresholds (`...draft.md:324`).
- SP32 provider divergence says "adapter contract test feeds 3 provider sample payloads" while deliverables include only PASS + KCB adapters and Q1 defers SCI (`...draft.md:334,338,537`).
- SP33/SP34/SP35 risks mostly describe mitigation intent, not exact pass/fail thresholds (`...draft.md:352,366,380`).

Planner must normalize every SP-card risk to: owner SP/agent, command, threshold, and recovery action.

### D. Testable acceptance criteria - PASS-WEAK

Every SP has binary acceptance phrased around `/ax-verify-*` exits or failing fixtures (`...draft.md:307,321,335,349,363,377`). The weakness is SP30's "Spec Trio drafts pass YAML syntax check but trio_integrity_guard skips `.draft`" wording (`...draft.md:307`). The current guard validates domains from the allowlist and has no `.draft` suffix logic in `practices/evals/trio_integrity_guard.sh`; the draft files are skipped only because `billing` is not registered yet. The PRD should not claim unsupported suffix behavior as an acceptance mechanism.

### E. Concrete verification steps - PASS-WEAK

The named verify skills exist on disk: `skills/ax-verify-L1`, `ax-verify-L2`, `ax-verify-L3`, `ax-verify-L4`, `ax-verify-domain`, `ax-verify-java`, `ax-verify-react`, and Tier-1 `ax-verify` with `scripts/run-all.sh`. `run-all.sh` also executes the guard suite, backend tests, frontend unit tests, Playwright, and fork-receiver bundle. Remaining gap: if SP32 becomes `identity-verification: backend_only`, §5.8 must add `/ax-verify-domain identity-verification` or an equivalent backend-only domain command.

### F. TDD anchor concreteness - WEAK

Most SP anchors name test file, assertion, RED reason, and first green command (`...draft.md:309,323,337,351,365,379`). Two blockers remain:

- **SP30:** the TDD anchor tests `currency-input` only (`...draft.md:309`). It does not prove the Spec Trio drafts are written first, syntax-valid, or later atomically finalized. If SP30 remains separate, add a RED for draft trio syntax/shape; better, collapse SP30/SP31.
- **SP32:** the frontend anchor says `expect(validateBusinessRegistration('123-45-67890')).toBe(true)` for a "known-valid mock" (`...draft.md:337`), while Scenario 2 requires 100+ real valid/invalid samples from public/official sources (`...draft.md:463-471`). A mock cannot anchor a regulatory checksum rule. Use `templates/_tests/business-reg-fixtures.json` from Scenario 2 as the TDD fixture and cite the snapshot row.

### G. Pre-mortem adequacy (DELIBERATE) - PASS-WEAK

The PRD has five scenarios (`...draft.md:444-512`), and each has detection, mitigation, and threshold. This satisfies the shape, but the execution detail is uneven: Scenario 2 requires a 100+ sample dataset but SP32's TDD anchor does not consume it; Scenario 3 names a Playwright assertion but not the exact command; Scenario 5 covers `trio_integrity_allowlist` conflict but not `_MANIFEST.yaml` or AGENTS sentinel conflict. Add those missing commands/surfaces before approval.

### H. Architect findings disposition - PASS

I agree with all four critical findings and mostly agree with the seven dimensional verdicts. The only substantive disagreement is TDD anchor concreteness: I downgrade it to PASS-WEAK because SP30 and SP32 still have concrete anchor gaps.

### I. CLAUDE.md anti-pattern resistance - PASS-WEAK

No governance loop is introduced: §11 excludes public release/docs-site/CI/release creep (`...draft.md:572-583`), and CLAUDE.md forbids governance documents and fork-team policy enforcement (`CLAUDE.md:113-139`). No MockMvc-only path is planned: the PRD forbids MockMvc and requires RestAssured (`...draft.md:182,561-565`; `METHODOLOGY.md:191-230`). Korean enterprise specials are defensible because the draft blocks raw RRN collection instead of shipping an RRN input (`...draft.md:155,167,180,560`). Weakness: SP35's admin/impersonation rule needs an anti-bypass design so it does not become governance theater around a single helper name.

### J. Autonomous execution safety - FAIL

Rollback boundaries exist per SP (`...draft.md:400-402`) and each SP card names `git revert spXX-*` (`...draft.md:313,327,341,355,369,383`). Shared-artifact ownership is incomplete. The table omits:

- `practices/upstream/_MANIFEST.yaml`
- `practices-react/upstream/_MANIFEST.yaml`
- `practices/AGENTS.md`
- `practices-react/AGENTS.md`
- any backend_only trio files for `identity-verification`

The halt thresholds cover `trio_integrity_allowlist` only (`...draft.md:427-432`). Add thresholds for manifest/sentinel lost-write, stale sha256, and parallel rebase conflict. The ESCAPE valve exists (`...draft.md:434-436`) but is too permissive if it can bypass unresolved Critic blockers after only two iterations; require explicit blocker inventory and maintainer acknowledgement.

### K. Defer list discipline - PASS-WEAK

Most deferrals are defensible (`...draft.md:251-278`). I do not consider raw `phone-input-kr` blocking because SP32 owns the higher-level verification panel (`...draft.md:266`). I do not consider `rrn-masked-input` blocking because the PRD correctly ships a protective rule instead (`...draft.md:267`). Weak spots:

- `breadcrumb` deferral may conflict with SP35's new admin/audit L3 pages (`...draft.md:268,376`); explicitly state those pages use flat tab/header navigation or promote breadcrumb.
- `time-picker` deferral may conflict with subscription renewal/admin scheduling UX (`...draft.md:265,320`); explicitly state billing renewal dates are date-only or handled by existing date/date-range components.
- `subscription-management` appears inside the deferred table while the reason says it is absorbed into billing (`...draft.md:277,580`). Move it out of the deferred list or mark as "not deferred: absorbed by billing."

### L. My independent steelman - FAIL

SP32's regulatory fixture story is internally inconsistent. The pre-mortem correctly says business-registration validation must use 100+ real valid/invalid samples from public/official sources (`...draft.md:463-471`), but the SP card's TDD anchor uses a "known-valid mock" (`...draft.md:337`). For a Korean enterprise primitive, fixture provenance is the product: a mock checksum can make the test green while the catalog still rejects real businesses. This is not a wording nit; it undermines the evidence-anchored rule and the claimed regulatory safety.

## My independent steelman (criterion L)

The billing/domain boundary is still under-specified relative to existing `payment`. The PRD says billing does not depend on payment code beyond pattern reuse (`...draft.md:438-440`), while SP31 includes subscription, plan, invoice, billing event, Stripe Billing adapter, Toss recurring adapter, webhook receiver, and provider abstraction (`...draft.md:154,320`). That is orthogonal to one-shot payment only if the PRD declares the boundary: payment owns authorization/capture/refund; billing owns subscription lifecycle/invoice issuance/recurring event normalization; any money movement adapter reuse is pattern-only. Without that boundary, execution agents may either duplicate payment abstractions or cross-import payment code, both of which would violate composition-kit isolation and `no-l4-cross-import` style constraints.

Does this change the verdict? **Yes, reinforcing ITERATE.** It can be fixed with a boundary section in SP31; it is not fundamental enough for REJECT.

## Hard blockers (must fix before APPROVE)

1. Fix atomic Spec-Trio sequencing. Collapse SP30+SP31 into one atomic billing SP, or move all billing-shaped L2/L3/spec artifacts from SP30 into SP31 and leave only generic L1 primitives in SP30.
2. Add `identity-verification: backend_only` with backend Spec Trio files and `/ax-verify-domain identity-verification`, or explicitly remove the backend domain from SP32. Given current deliverables, backend_only is required.
3. Add `observability_signal` to §5.8 for every SP, with concrete metric/log/audit/event names and thresholds.
4. Add shared-artifact ownership and halt/serialize thresholds for `_MANIFEST.yaml` files and AGENTS.md sentinels across SP32/SP33/SP34/SP35.
5. Replace SP32's mock business-registration TDD fixture with official/public fixture data and align SP card acceptance with Scenario 2.
6. Convert every SP-card risk to owner + command + threshold + recovery.

## Soft suggestions

1. Add Option F: "strict atomic billing SP + defer polish clusters" to §1, then reject it explicitly if Planner keeps the 6-SP hybrid.
2. Strengthen SP35 impersonation rule against helper rename bypass; match canonical session/acting-as state, not only `assumeUserId()`.
3. Resolve breadcrumb/time-picker deferrals explicitly so SP35/SP31 do not reinvent them inline.
4. Clarify payment vs billing boundaries in SP31 to avoid cross-import or duplicate-abstraction drift.
5. Tighten ESCAPE valve wording so it cannot bypass unresolved critical blockers without an explicit residual-risk record.

## Re-review trigger (if ITERATE/REJECT)

Exact Planner revisions required for next iteration:

1. Revised sequencing/implementation plan where billing Spec Trio + billing L4 + billing backend + billing rules + allowlist entry land atomically, with SP numbering updated and §5.8/§6.1/§6.2 aligned.
2. Revised SP32 adding `identity-verification: backend_only` to `practices/evals/trio_integrity_allowlist.yaml`, plus `specs/identity-verification-l0.yaml`, `contracts/identity-verification-openapi.yaml`, `blueprints/identity-verification-manifest.yaml`, and a `/ax-verify-domain identity-verification` acceptance row.
3. Revised §5.8 Verification Matrix with `observability_signal`, and revised SP-card risks/§7 scenarios with owner, command, threshold, and recovery.
4. Revised §6.2/§6.4 ownership and halt thresholds for `practices/upstream/_MANIFEST.yaml`, `practices-react/upstream/_MANIFEST.yaml`, `practices/AGENTS.md`, and `practices-react/AGENTS.md`, including serialize-on-conflict rules for SP32/SP33/SP34.
5. Revised SP32 TDD fixture contract using official/public business-registration fixture data, not a mock "known-valid" number.
6. Revised defer list and boundary text: remove or reclassify `subscription-management` from the deferred table, decide breadcrumb/time-picker needs, and define payment-vs-billing ownership.

## ADR-ready content (for Step 6 if APPROVE)

Not ADR-ready yet. Provisional ADR content after blockers are fixed:

- **Decision:** Approve P1 absorption as an atomic billing domain plus Korean identity primitives and bounded forms/tables/admin polish.
- **Drivers:** Preserve atomic Spec Trio ordering, close high-frequency Korean SaaS billing/identity gaps, keep Tier-1 surface frozen, and maintain binary verification with observable signals.
- **Alternatives considered:** P1 sweep rejected for weak theme; strict billing-only cycle rejected only if each polish cluster remains evidence-backed with failing fixtures; defer-all rejected because billing and Korean specials are current fork-receiver blockers.
- **Why chosen:** Atomic billing plus backend_only identity-verification preserves the catalog's self-discoverability while allowing parallel non-domain polish only after shared manifest/sentinel ownership is explicit.
- **Consequences:** Billing becomes a full_trio domain; identity-verification becomes backend_only; SP32/SP33/SP34 parallelism is conditional on manifest/sentinel rebase safety; remaining P1 items defer only when no SP acceptance depends on them.
