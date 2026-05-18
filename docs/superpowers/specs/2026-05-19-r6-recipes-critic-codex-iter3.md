# Codex Critic R6 iter 3 FINAL

## Verdict

APPROVE.

Q1 is closed. R6 SP39+SP40 can start.

## Q1 closure (1)

CLOSED. Iter 3 removes the false iter-2 "zero code change" / substring-containment claim and replaces it with an explicit SP39 guard delta:

- `recipe_governance_guard.sh` changes from singular-only `grep -q "applied_recipe:"` to dual-form extended regex acceptance equivalent to `grep -qE "^applied_recipe:|^applied_recipes:"`.
- R5 singular `applied_recipe:` remains valid for backward compatibility.
- R6+ plural `applied_recipes:` is canonical.
- SP39 atomically adds `pass_applied_recipes_plural/RECIPE.md` and `fail_applied_recipes_empty_list/RECIPE.md`.
- `business-domain-must-declare-applied-recipe.md` gets the dual-form rule clarification.

The required critic evidence is now in the SP39 scope: plural list passes; empty plural list fails.

## Independent attack (one)

Non-blocking implementation risk: the regex alone can detect the `applied_recipes:` header but cannot prove the list has at least one item.

Iter 3 handles this by making empty-list rejection an explicit guard requirement and by requiring the fail fixture to exit non-zero. That is sufficient for planning approval; SP39 must implement actual list-body validation, not only the header regex.

## Final reasoning

The only iter-2 blocker was the broken Q1 guard contract. Iter 3 fixes it in every required location: principle 7, §5 Q1 resolution, §7 pre-mortem, §8 TD-019, §9 honored constraints, and SP39 atomic scope.

No need to reopen the 3-recipe scope, Korean ledger, cluster framing, TDD anchors, SP40 verdict/tag policy, or deferred recipe decisions.

## ADR (if APPROVE)

ADR-ready.

TD-2026-05-18-019 should record: `applied_recipes:` plural list is canonical for R6+; R5 singular forms remain legacy-valid; SP39 updates the guard to dual-form acceptance and adds fixtures proving plural-list pass plus empty-list fail. The iter-2 no-code-change framing is rejected because disk evidence falsified it.
