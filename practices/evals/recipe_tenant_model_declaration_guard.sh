#!/usr/bin/env bash
# practices/evals/recipe_tenant_model_declaration_guard.sh — iter-3/4 hard gate.
#
# Two-MUST enforcement of specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001
# and the related "Multi-tenant recipes MUST adopt one of the 3 isolation
# strategies" clause:
#
#   MUST #1 (iter-3, NC6) — every recipes/*/RECIPE.md declares a
#   `tenant_model:` frontmatter key with value `single` OR `multi`.
#
#   MUST #2 (iter-4, NC7) — every recipe that declares `tenant_model: multi`
#   ALSO cites at least one of `MULTI-TENANT-ISOLATION-00{1,2,3}` or
#   `MULTI-TENANT-PROPAGATION-00{1,2}` somewhere in the RECIPE.md body
#   (typically the business_invariants table). The MULTI-TENANT-ISOLATION-001
#   /-002/-003 items in specs/multi-tenant-l0.yaml require multi-tenant
#   recipes to adopt one isolation strategy + propagation. A `multi`
#   declaration without any anchor cite is a documented adoption claim
#   with zero anchored basis — Spec Trio self-violation.
#
# Usage:
#   bash practices/evals/recipe_tenant_model_declaration_guard.sh
#
# Exit codes:
#   0 — every RECIPE.md declares tenant_model: single|multi
#   1 — at least one recipe is missing the declaration or has an invalid value

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RECIPES_DIR="${1:-$REPO_ROOT/recipes}"

violations=0
pass=0

if [ ! -d "$RECIPES_DIR" ]; then
    echo "recipe_tenant_model_declaration_guard: recipes/ not found at $RECIPES_DIR — nothing to check"
    exit 0
fi

for recipe_md in "$RECIPES_DIR"/*/RECIPE.md; do
    [ -f "$recipe_md" ] || continue
    name="$(basename "$(dirname "$recipe_md")")"
    line="$(grep -E '^tenant_model:[[:space:]]*(single|multi)([[:space:]#].*)?$' "$recipe_md" | head -1)"
    if [ -z "$line" ]; then
        # Check whether tenant_model exists at all (to give a clearer message)
        any="$(grep -E '^tenant_model:' "$recipe_md" | head -1)"
        if [ -z "$any" ]; then
            echo "VIOLATION [$name]: RECIPE.md missing 'tenant_model:' declaration (must be 'single' or 'multi')" >&2
        else
            echo "VIOLATION [$name]: tenant_model value invalid — '$any' (must start with 'single' or 'multi')" >&2
        fi
        violations=$((violations + 1))
        continue
    fi

    # MUST #2 (iter-4, NC7) — multi declarations must cite at least one
    # ISOLATION-001/002/003 or PROPAGATION-001/002 anchor.
    if echo "$line" | grep -qE '^tenant_model:[[:space:]]*multi'; then
        if ! grep -qE 'MULTI-TENANT-(ISOLATION-00[123]|PROPAGATION-00[12])' "$recipe_md"; then
            echo "VIOLATION [$name]: tenant_model: multi declared but RECIPE.md does not cite any MULTI-TENANT-ISOLATION-00{1,2,3} or MULTI-TENANT-PROPAGATION-00{1,2} anchor (specs/multi-tenant-l0.yaml MULTI-TENANT-ISOLATION-001/002/003 + MULTI-TENANT-PROPAGATION-001/002 require multi-tenant recipes to adopt one isolation strategy + propagation)" >&2
            violations=$((violations + 1))
            continue
        fi
    fi

    pass=$((pass + 1))
done

if [ "$violations" -eq 0 ]; then
    echo "recipe_tenant_model_declaration_guard: PASS — $pass recipe(s) declare tenant_model"
    exit 0
fi

echo "recipe_tenant_model_declaration_guard: FAIL — $violations violation(s) ($pass PASS)" >&2
exit 1
