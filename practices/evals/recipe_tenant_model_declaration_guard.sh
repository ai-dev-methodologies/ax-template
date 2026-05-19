#!/usr/bin/env bash
# practices/evals/recipe_tenant_model_declaration_guard.sh — iter-3 hard gate.
#
# Mechanical regression prevention: every recipes/*/RECIPE.md MUST declare
# a `tenant_model:` frontmatter key with value `single` OR `multi`. Closes
# P2 Round 3 NC6 — the iter-2 fix of 10 missing declarations needs a
# regression lock so the next recipe author cannot silently re-introduce
# the violation. Spec anchor:
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001.
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
    pass=$((pass + 1))
done

if [ "$violations" -eq 0 ]; then
    echo "recipe_tenant_model_declaration_guard: PASS — $pass recipe(s) declare tenant_model"
    exit 0
fi

echo "recipe_tenant_model_declaration_guard: FAIL — $violations violation(s) ($pass PASS)" >&2
exit 1
