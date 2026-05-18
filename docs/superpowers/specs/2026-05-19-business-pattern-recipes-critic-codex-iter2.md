# Codex Critic R5 iter 2

## Verdict

ITERATE.

Iter 2 closes the structural Synthesis-A blockers. One narrow blocker remains: the Korean evidence entries are structurally shaped, but the cited Korean quotations are not verified as verbatim on the referenced pages.

## Blocker closure (8)

1. `time-series-chart.tsx` not claimed as new: CLOSED. It is explicitly corrected as existing and excluded from the 3 shipped recipes.
2. 3 recipes only: CLOSED. Scope is `saas-subscription`, `e-commerce`, `crm`; seven patterns are deferred in the table.
3. `/ax-scaffold business --analyze` removed: CLOSED. Deterministic `/ax-scaffold business <pattern> <project-name>` only.
4. `/ax-verify-recipe` Tier-2 removed: CLOSED. Tier-1 remains 4; Tier-2 remains 8.
5. §5.5 TDD anchor table: CLOSED. 14 rows include `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`, and `owning_SP`.
6. `business_invariants` bind to existing `spec_ref` or `rule_ref`: CLOSED for resolvability. Local check found referenced spec/rule artifacts including `specs/billing-l0.yaml#BILLING-AUTHZ-002`, `specs/{feature-flags,audit-log,payment,crud}-l0.yaml`, and the named rule files.
7. `RECIPE_DEVIATION.md` removed: CLOSED. Override is inline `override_allowed:` frontmatter only.
8. Korean references structurally true: OPEN. Shape is present (`provenance_class: external`, URL, `citation`, `quoted_at`), but verbatim fidelity failed spot-check.

## Spot checks

- §1 RALPLAN-DR options preserved: PASS. Option A2/Synthesis-A is recommended; rejected options remain explicit.
- §10 honored constraints intact: PASS except the Korean citation fidelity claim.
- §7 pre-mortem has >=3 scenarios: PASS. Each has failure mode, detection, owner/command/threshold/recovery.
- §5.5 Verification Matrix has `observability_signal`: PASS, with JSON-line emission contract.

## Independent attack

BLOCKING: Korean evidence citation fidelity.

- Toss recurring billing URL in the PRD uses `https://docs.tosspayments.com/guides/v2/billing/overview`; fetched v2 billing content is available at `/guides/v2/billing`, but I did not find the PRD quote `"자동결제(빌링)는 고객의 카드 정보를 안전하게 저장하여 정기적으로 결제하는 기능입니다."`
- Toss payment-widget integration URL exists, but I did not find the PRD quote `"결제 승인은 결제 요청과 별도의 단계이며, 승인이 완료되어야 실제 결제가 처리됩니다."`
- Coupang developer portal URL exists and lists API categories such as 상품/배송/환불/정산, but I did not find the PRD quote `"쿠팡 셀러는 상품, 주문, 정산 API를 통해 시스템과 연동합니다."`
- Channel Talk URL exists, but I did not find the PRD quote `"고객 데이터, 영업 단계, 활동 이력을 한 곳에서 관리합니다."`

Fix: replace those citations with short exact snippets from the cited pages, update stale URLs where needed, or downgrade the claim to `internal_design` with rationale. Do not keep "verbatim" language for paraphrases.

INFORMATIONAL: Verdict harness precedent is sufficiently anchored. The PRD points to the Payment sealed sub-agent precedent, and the repo has `docs/blueprints/payment/acceptance/l4-sealed-rubric.md` with sealed-at commit, MUST/SHOULD criteria, anti-rigging rules, and result structure. SP38 should instantiate the same prompt/rubric discipline for each recipe.

INFORMATIONAL: T6 dry-run assertion should be tightened during SP36. The table says dry-run creates/outputs the correct file tree, but the first-green command only asserts exit 0. Prefer a golden stdout/tree snapshot or explicit `grep` checks for expected paths so dry-run validates the tree without writing files.

INFORMATIONAL: Inline `override_allowed:` is acceptable as anti-governance. Silent rationalization risk is acknowledged and intentionally left to fork-receiver policy; that is consistent with avoiding a catalog-owned deviation ceremony.

## Final verdict reasoning

The main iter 1 issues are fixed: scope is 3 recipes, all shipped recipes are verdict-covered, speculative inference is gone, no new Tier-2 skill is added, TDD anchors exist, invariants are resolvable, and the deviation ceremony is removed.

I cannot approve while §10/§11 claim "verified URL" and "verbatim citation" for Korean references that are not actually verbatim on the fetched sources. This is a narrow evidence-fidelity fix, not a design rejection.

## ADR (if APPROVE)

N/A.

## Re-review trigger (if ITERATE/REJECT)

Re-review only the Korean evidence entries in §4 and the corresponding §10/§11 claims. Approval is ready once every Korean reference either has an exact short citation from the cited URL with `quoted_at`, or is explicitly downgraded to `internal_design` with rationale.
