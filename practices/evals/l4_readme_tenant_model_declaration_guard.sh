#!/usr/bin/env bash
# practices/evals/l4_readme_tenant_model_declaration_guard.sh — iter-8 hard gate.
#
# Mechanical enforcement of specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001
# clause (b) — "every templates/L4/<domain>/README.md MUST declare its tenant
# model via a `**Tenant model**:` line that cites this spec anchor".
#
# Closes the half-closure that iter-7 left in place: the spec MUST now covers
# both fork-receiver entry surfaces (RECIPE.md frontmatter + L4 README), and
# iter-7 added the declaration line to all 12 L4 READMEs, but only the
# RECIPE.md side had a mechanical guard (recipe_tenant_model_declaration_
# guard.sh). A future edit that drops the **Tenant model** line from a
# templates/L4/<domain>/README.md would silently re-introduce the
# half-closure pattern. This guard locks the L4 surface symmetrically.
#
# Each templates/L4/<domain>/README.md MUST satisfy:
#   #1 Contain a line starting with `**Tenant model**:` (literal Markdown
#      bold form, matching the spec requirement text).
#   #2 That line MUST cite anchor `MULTI-TENANT-ISOLATION-DEFAULT-001`
#      (with or without the `specs/multi-tenant-l0.yaml#` prefix).
#   #3 The declared value MUST be `single` or `multi`.
#   #4 If the declared value is `multi`, the README MUST also cite at
#      least one of `MULTI-TENANT-ISOLATION-00{1,2,3}` or
#      `MULTI-TENANT-PROPAGATION-00{1,2}` (mirroring MUST #2 of the
#      recipe-side guard — adoption claim without an anchor is a
#      Spec Trio self-violation).
#
# Usage:
#   bash practices/evals/l4_readme_tenant_model_declaration_guard.sh
#   bash practices/evals/l4_readme_tenant_model_declaration_guard.sh <L4_dir>
#
# Exit codes:
#   0 — every L4 README declares tenant_model with spec anchor + valid value
#   1 — at least one L4 README violates one of the four MUSTs

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
L4_DIR="${1:-$REPO_ROOT/templates/L4}"

violations=0
pass=0

if [ ! -d "$L4_DIR" ]; then
    echo "l4_readme_tenant_model_declaration_guard: templates/L4 not found at $L4_DIR — nothing to check"
    exit 0
fi

for readme in "$L4_DIR"/*/README.md; do
    [ -f "$readme" ] || continue
    name="$(basename "$(dirname "$readme")")"

    # MUST #1 — declaration line present
    line="$(grep -m1 -E '^\*\*Tenant model\*\*:' "$readme" || true)"
    if [ -z "$line" ]; then
        echo "VIOLATION [$name]: templates/L4/$name/README.md missing '**Tenant model**:' declaration line (specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001 clause (b))" >&2
        violations=$((violations + 1))
        continue
    fi

    # MUST #2 — line cites canonical anchor
    if ! echo "$line" | grep -qE 'MULTI-TENANT-ISOLATION-DEFAULT-001'; then
        echo "VIOLATION [$name]: '**Tenant model**:' line does not cite anchor 'MULTI-TENANT-ISOLATION-DEFAULT-001' — got: $line" >&2
        violations=$((violations + 1))
        continue
    fi

    # MUST #3 — value is single|multi
    value="$(echo "$line" | grep -oE '\*\*Tenant model\*\*:[[:space:]]*`?(single|multi)`?' | grep -oE '(single|multi)' | head -1)"
    if [ -z "$value" ]; then
        echo "VIOLATION [$name]: '**Tenant model**:' line value not 'single' or 'multi' — got: $line" >&2
        violations=$((violations + 1))
        continue
    fi

    # MUST #4 — multi requires isolation/propagation cite
    if [ "$value" = "multi" ]; then
        if ! grep -qE 'MULTI-TENANT-(ISOLATION-00[123]|PROPAGATION-00[12])' "$readme"; then
            echo "VIOLATION [$name]: '**Tenant model**: multi' declared but README does not cite any MULTI-TENANT-ISOLATION-00{1,2,3} or MULTI-TENANT-PROPAGATION-00{1,2} anchor (multi declaration without adoption anchor is a Spec Trio self-violation)" >&2
            violations=$((violations + 1))
            continue
        fi
    fi

    pass=$((pass + 1))
done

if [ "$violations" -eq 0 ]; then
    echo "l4_readme_tenant_model_declaration_guard: PASS — $pass L4 README(s) declare tenant_model with spec anchor"
    exit 0
fi

echo "l4_readme_tenant_model_declaration_guard: FAIL — $violations violation(s) ($pass PASS)" >&2
exit 1
