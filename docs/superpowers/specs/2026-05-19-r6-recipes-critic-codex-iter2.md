# Codex Critic R6 iter 2

## Verdict

ITERATE.

R6 iter 2 closes Synthesis-A and most of the iter 1 blockers. The remaining blocker is narrow: the PRD's Q1 closure says the existing R5 guard already accepts `applied_recipes:` plural form because it contains the literal substring `applied_recipe:`. It does not. The current guard requires `grep -q "applied_recipe:"`, and a plural-only README fails that check.

This is a concrete execution blocker because §5 adopts `applied_recipes:` as the forward-canonical multi-recipe representation and says no guard code change is required.

## Blocker closure (6+Synthesis-A)

1. Synthesis-A trim to 3 recipes: CLOSED. Scope is now `booking + marketplace + b2b-admin`; `community + lms + cms + internal-it` are deferred with refreshed triggers in §10.

2. Broken `spec_ref:` IDs: CLOSED for existing disk anchors. The prior broken CRUD/AUDIT/PAYMENT refs were replaced with disk-real anchors or same-SP recipe-level invariants. Existing cited anchors checked below resolve.

3. Korean WebFetch ledger: CLOSED. §4.4 has 2 external verbatim PASS rows for Etsy and Stripe Connect, 1 Korean verbatim PASS for channel.io, and 3 documented downgrades for Naver, Booking.com, and Jira.

4. Cluster claim honesty: CLOSED. §4.5 now says logical theme clustering with shared L4 mutations and an append-only sorted protocol, not composition disjointness.

5. Q1 multi-recipe membership: OPEN / BLOCKING. Option (b) is chosen, but the compatibility proof is false. `applied_recipe:` is not a substring of `applied_recipes:` because the plural has `s` before the colon. Evidence:

```text
practices/evals/recipe_governance_guard.sh:44 -> grep -q "applied_recipe:"
printf 'applied_recipes:\n  - booking\n' | grep -q 'applied_recipe:' -> exit 1
printf 'applied_recipe: booking\n' | grep -q 'applied_recipe:' -> exit 0
```

6. SP42 -> SP40 FINAL and tag policy: CLOSED. §4.5/§6 define a 3-verdict harness matching the R5 SP38 scale, and `v1.4.0-recipes-complete` is held unless all 3 sealed verdicts pass.

7. Per-recipe TDD anchors: CLOSED. §4.1, §4.2, and §4.3 each include `test_file`, `assertion`, `expected_RED_reason`, `first_GREEN_command`, and `owning_SP`.

## Disk validation

Spot-check anchors requested:

```text
specs/payment-l0.yaml#PAYMENT-STATE-002: PASS (line 159)
specs/audit-log-l0.yaml#AUDIT-RECORD-001: PASS (line 7)
specs/search-l0.yaml#SEARCH-AUTHZ-001: PASS (line 7)
specs/feature-flags-l0.yaml#FF-CRUD-003: PASS (line 91)
```

Additional cited anchors checked:

```text
specs/payment-l0.yaml#PAYMENT-REFUND-001: PASS (line 189)
specs/audit-log-l0.yaml#AUDIT-RECORD-002: PASS (line 23)
specs/audit-log-l0.yaml#AUDIT-RETENTION-001: PASS (line 88)
specs/feature-flags-l0.yaml#FF-AUTHZ-001: PASS (line 7)
specs/auth-asvs-l1.yaml#ASVS-V4.2.1: PASS (line 153)
specs/auth-asvs-l1.yaml#ASVS-V4.2.2: PASS (line 160)
practices/rules/idempotency-key-on-mutations.md: PASS
practices/rules/no-rrn-collection-without-legal-basis.md: PASS
practices/evals/recipe_spec_referential_integrity_guard.sh: PASS
```

Recipe-level invariant anchors are planned same-SP artifacts, not current disk artifacts. That is acceptable if SP39 creates the recipe specs/templates atomically before running the referential-integrity guard.

## Independent attack

BLOCKING: existing R5 guard does not actually accept `applied_recipes:` plural-only form.

The PRD repeats this false assumption in §1 principle 7, §5 disk evidence, §7 pre-mortem, §8 ADR TD-2026-05-18-019, §9 honored constraints, and §12 verdict line. It also says no guard code change is required.

The narrow fix is one of:

- Keep R6 on existing R5-compatible syntax only: `applied_recipe:`, `applied_recipe_secondary:`, `applied_recipe_tertiary:`. Remove the claim that `applied_recipes:` is canonical in R6.
- Or keep Option (b), but make SP39 explicitly include guard + fixture updates so `applied_recipes:` list form is accepted and empty/malformed lists fail.

I do not see a separate blocker in the 2-SP structure after the trim to 3 recipes. Booking evidence remains the weakest row, but §7 and §6 now make the mitigation concrete enough: tag is held on verdict failure, and SP41 either adds stronger evidence or returns booking to deferred.

## Final reasoning

This is close to approval, but the Q1 proof is exactly the kind of guard-contract mistake iter 1 asked to resolve before execution. If SP39 proceeds as written, plural-only fixtures will fail, or worse, existing singular lines will mask that the new canonical list is not enforced.

Iterate only the Q1 membership section and related claims. No need to reopen the 3-recipe scope, Korean ledger, cluster framing, TDD anchors, or SP40 tag policy.

## ADR (if APPROVE)

Not ADR-ready while TD-2026-05-18-019 rests on the false no-code-change premise.

## Re-review trigger (if ITERATE/REJECT)

Re-review after the PRD either:

1. removes `applied_recipes:` as R6 canonical syntax and stays with R5 singular/secondary fields, or
2. explicitly includes SP39 guard + fixture changes proving plural list acceptance and malformed-list rejection.

Required evidence for approval: `printf 'applied_recipes:\n  - booking\n'` or an equivalent fixture passes the updated guard, while an empty list fixture fails.
