# Codex Critic Review — Component Catalog Completeness Audit (2026-05-18)

## Verdict: APPROVE WITH RESCOPE

The audit is directionally sound and well grounded in the repo, but it is not clean enough to drive SP13+ unchanged. The hard blockers are not path/count grounding; those mostly check out. The blockers are scope hygiene: the document claims 71 P0 items, later corrects itself to 75, lets P1/P2 totals drift, promotes several convenience or idiom rules to P0 without a broken L4 flow, and sequences backend domain implementation before the relevant Spec Trio artifacts exist. Rescope those before execution.

## Grounding verification
- Spot-checked paths: 10/10 exist
- Sampled existing paths: `templates/L1/components/accordion.tsx`, `templates/L1/components/tooltip.tsx`, `templates/L2/blocks/filter-bar.tsx`, `templates/L2/blocks/search-input.tsx`, `templates/L2/blocks/data-table.tsx`, `templates/L3/pages/auth-callback-page/page.tsx`, `templates/L3/pages/detail-page/[id]/page.tsx`, `practices/rules/web-rest-controller-annotation.md`, `practices-react/rules/l2-prefer-data-prop-over-direct-fetch.md`, `skills/ax-verify-L1/SKILL.md`
- Counts: verified vs claimed
- L1 components: 32 vs 32
- L2 blocks: 26 vs 26
- L3 page templates: 7 vs 7
- Java rules: 68 vs 68
- React rules: 70 vs 70
- Path citation volume: the audit has far more than 50 path-like citations; the grounding density is real.
- Count defect: the audit headline says 71 P0 and 74 P1, but the reconciliation section says 75 P0 and 78 P1. Treat the reconciled per-row count as authoritative, then update the executive summary and SP scopes.

## Weakest P0 items (5)
1. `kbd.tsx` — "absence is a design tell" is not a P0 defense. It should drop to P1 unless a P0 `search-palette` story fails binary verification because shortcut hints cannot render.
2. `search-results-page` — the backing `search-index` backend and `search` Spec Trio are P1 in the same audit. A P0 L3 page ahead of its contract is inconsistent. Drop to P1, or promote the full search contract/domain to P0 with an L4 flow that fails today.
3. `empty-data-page` — the repo already has `templates/L2/blocks/empty-state.tsx`; a full-page variant is useful but not catalog-completeness critical. Drop to P1 unless CRUD/dashboard first-run Playwright cannot be expressed with existing L2.
4. `/ax-add-rule` and `/ax-add-template` — these are authoring accelerators, not user-visible fork capability. Keep `/ax-fork-receiver` closer to P0 because it closes PRD SP5.5, but drop these two scaffolding skills to P1.
5. React idiom rules `prefer-react-19-use-over-useEffect-fetching` and `prefer-server-action-over-fetch-mutation` — the justifications read like framework preference, not P0 breakage. Keep as P1 unless the audit cites a current L4 violation and an official Next/React constraint that makes the existing pattern invalid.

## Anti-bloat verdict
- React rules +30: over-budget in priority, not in raw count. The audit actually recommends 26 React rules, not 30, so the numeric cap is respected. But the PRD SP7 rule was "implementation-proven needs only"; several P0 React rules are still idiom/speculation rules. Keep P0 for layer-boundary, serialization, traceId, and a11y rules tied to P0 blocks; demote the broad framework-preference and RRN-specific rules unless backed by failing fixtures.
- Java rules +30: justified numerically, over-ranked in places. The audit recommends 22 Java rules, under cap. Demote `webhook-signature-verify` because `integration-webhook` is P1, demote `auditing-jpa-listener` if `AuditingConfig` remains P1, and treat `idempotency-key-on-mutations` as an extension unless a non-payment mutation L4 flow fails today.

## Korean enterprise specificity verdict
- 도로명/지번: P0. This is common enough for Korean enterprise address capture and has a clear official address-service integration path.
- RRN masking: demote. RRN handling is legally sensitive and restricted; a universal composition kit should default to avoiding RRN collection, not shipping RRN input as P0. A P0 "no RRN logging / no RRN unless explicit legal basis" rule is defensible.
- 사업자등록번호: demote to P1. High-frequency for Korean B2B billing, vendor, and tax-invoice flows, but not universal across all SaaS screens. Promote only if company onboarding or tax invoice becomes a P0 reference workload.
- CI/DI 본인인증: demote. Important for regulated identity, age, finance, telecom, and fraud-sensitive flows; not universal B2B SaaS catalog completeness.
- 한글 IME composition: P0, but as behavior coverage, not a standalone component. Tie it to `combobox`, `search-palette`, `typeahead-search`, and filter inputs with tests that composition events do not prematurely debounce, submit, or corrupt text.

## SP plan integrity
- Each proposed SP has binary acceptance: mostly yes, but SP18 over-scopes by bundling P1 domain extractions into a "3 P0 remainder" SP, and SP21 must be recalculated after P0 demotions.
- TDD anchors specified: yes, at a usable level. Strengthen SP15/SP16 by naming failing fixtures for the new domain/template combinations, not only "block test per fixture."
- Spec Trio + Provenance + Skill pattern preserved: no, not as sequenced. SP15 ships `notification` backend templates and runs `/ax-verify-domain notification` before SP19 creates the notification Spec Trio. SP16 ships `audit-log` and `file-storage` backend skeletons while their Spec Trios are only P1. Fix by moving Spec Trios before their backend/L4 domains, or by splitting each domain so its Spec Trio, contract, manifest, template, and verification gate land in the same SP.

## New steelman

This is not automatically speculative just because it grows the catalog by roughly 194 P0/P1/P2 candidates. CLAUDE.md explicitly treats catalog expansion as normal value creation when each addition follows spec → rule → evidence → test and preserves React + Spring as equal partners. The audit passes that test for the core catalog gaps: backend baseline templates, L1 date/combobox/file/address primitives, runtime hardening, notifications, audit-log/file-storage, and accessibility. The growth becomes speculative only where it shifts from "a known template or L4 flow breaks today" to "a senior engineer might want this someday." The fix is rescope, not rejection.

## Recommended action
- Canonicalize counts: update every 71/74/49 reference to the reconciled per-row totals, or reduce the per-row P0 list back to 71.
- Demote weak P0s: `kbd`, `empty-data-page`, `search-results-page`, `/ax-add-rule`, `/ax-add-template`, broad React idiom rules, and RRN-specific P0 rules unless strengthened with failing L4 flows.
- Reorder SPs so Spec Trios precede or ship atomically with domain templates. Specifically, move notification/settings Spec Trios before SP15, and either promote audit-log/file-storage Spec Trios to P0 before SP16 or defer those backend skeletons.
- Keep Korean specificity, but split universal Korean UX (`도로명/지번`, Hangul IME behavior) from regulated/specialized identity handling (RRN, CI/DI).
- Keep the anti-bloat cap, but enforce it at P0 priority: each P0 rule must name the P0 template/L4 flow and the failing fixture it protects.
