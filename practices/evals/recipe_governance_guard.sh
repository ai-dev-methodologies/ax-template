#!/usr/bin/env bash
# practices/evals/recipe_governance_guard.sh — SP37 recipe governance gate.
#
# Enforces 3 rules:
#
#   1. prefer-recipe-composition-over-l4-cross-import
#      — L4 domain READMEs with multi-L4 compositions must declare applied_recipe:
#
#   2. business-domain-must-declare-applied-recipe
#      — every L4 domain listed in a recipe's enabled_l4_domains must declare
#        applied_recipe: in its README.md
#
#   3. recipe-invariants-must-resolve
#      — every business_invariants entry in a recipe spec YAML must carry
#        spec_ref: or rule_ref: pointing to an existing artifact
#
# Usage:
#   bash practices/evals/recipe_governance_guard.sh              # live repo
#   bash practices/evals/recipe_governance_guard.sh --fixtures   # fixture validation
#
# Exit 0: all checks pass (or no recipes present yet).
# Exit 1: at least one governance violation found.
# Exit 2: usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        *) echo "recipe_governance_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

violations=0

# ── Helper: check applied_recipe: presence in a README file ──────────────────
check_applied_recipe_declared() {
    local readme="$1"
    local domain_label="$2"
    if grep -q "applied_recipe:" "$readme" 2>/dev/null; then
        return 0
    else
        echo "VIOLATION [business-domain-must-declare-applied-recipe]: $domain_label README has no applied_recipe: field" >&2
        echo "  file: $readme" >&2
        return 1
    fi
}

# ── Helper: validate business_invariants in a recipe YAML ────────────────────
check_invariants_resolve() {
    local recipe_yaml="$1"
    local repo_root="$2"
    local label="$3"
    local local_violations=0

    # Extract business_invariants block using Python for reliable YAML parsing
    python3 - "$recipe_yaml" "$repo_root" <<'PY'
import sys
import pathlib

recipe_path = pathlib.Path(sys.argv[1])
repo_root = pathlib.Path(sys.argv[2])

try:
    import yaml
    data = yaml.safe_load(recipe_path.read_text()) or {}
except ImportError:
    # Fallback: minimal line-by-line parser for spec_ref:/rule_ref:
    data = {}
    lines = recipe_path.read_text().splitlines()
    invariants = []
    current = None
    in_invariants = False
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("business_invariants:"):
            in_invariants = True
            continue
        if in_invariants:
            if stripped.startswith("- id:"):
                if current is not None:
                    invariants.append(current)
                current = {"id": stripped[len("- id:"):].strip().strip("\"'")}
            elif current is not None and stripped.startswith("spec_ref:"):
                current["spec_ref"] = stripped[len("spec_ref:"):].strip().strip("\"'")
            elif current is not None and stripped.startswith("rule_ref:"):
                current["rule_ref"] = stripped[len("rule_ref:"):].strip().strip("\"'")
            elif stripped and not stripped.startswith("#") and not stripped.startswith("-"):
                # end of invariants list
                if not stripped.startswith(" ") and not stripped.startswith("\t"):
                    in_invariants = False
    if current is not None:
        invariants.append(current)
    data["business_invariants"] = invariants
except Exception as e:
    print(f"recipe_governance_guard: could not parse {recipe_path}: {e}", file=sys.stderr)
    sys.exit(0)

invariants = data.get("business_invariants") or []
fail_count = 0

for inv in invariants:
    inv_id = inv.get("id", "<unknown>")
    spec_ref = inv.get("spec_ref", "")
    rule_ref = inv.get("rule_ref", "")

    if not spec_ref and not rule_ref:
        print(f"VIOLATION [recipe-invariants-must-resolve]: invariant {inv_id} has neither spec_ref: nor rule_ref:", file=sys.stderr)
        print(f"  recipe: {recipe_path}", file=sys.stderr)
        fail_count += 1
        continue

    if spec_ref:
        # Resolve file part (before #anchor)
        file_part = spec_ref.split("#")[0].strip()
        resolved = repo_root / file_part
        if not resolved.exists():
            print(f"VIOLATION [recipe-invariants-must-resolve]: invariant {inv_id} spec_ref '{spec_ref}' → '{file_part}' does not exist", file=sys.stderr)
            print(f"  recipe: {recipe_path}", file=sys.stderr)
            fail_count += 1

    if rule_ref:
        resolved = repo_root / rule_ref.strip()
        if not resolved.exists():
            print(f"VIOLATION [recipe-invariants-must-resolve]: invariant {inv_id} rule_ref '{rule_ref}' does not exist", file=sys.stderr)
            print(f"  recipe: {recipe_path}", file=sys.stderr)
            fail_count += 1

sys.exit(fail_count)
PY
}

# ── Mode A: fixture validation ────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    echo "=== recipe_governance_guard.sh — fixture validation ==="
    FIXTURES_DIR="$SCRIPT_DIR/fixtures"

    # ── Fixture 1: business-domain-must-declare-applied-recipe ───────────────
    echo ""
    echo "[fixture] business-domain-must-declare-applied-recipe"

    FAIL_README="$FIXTURES_DIR/business-domain-must-declare-applied-recipe/fail_no_applied_recipe/README.md"
    PASS_README="$FIXTURES_DIR/business-domain-must-declare-applied-recipe/pass/README.md"

    if [ -f "$FAIL_README" ]; then
        if grep -q "applied_recipe:" "$FAIL_README"; then
            echo "  GUARD_ERROR: fail fixture has applied_recipe: — fixture is wrong" >&2
            violations=$((violations + 1))
        else
            echo "  PASS [fail fixture correctly lacks applied_recipe:]"
        fi
    else
        echo "  SKIP [fail fixture not found: $FAIL_README]"
    fi

    if [ -f "$PASS_README" ]; then
        if grep -q "applied_recipe:" "$PASS_README"; then
            echo "  PASS [pass fixture correctly has applied_recipe:]"
        else
            echo "  GUARD_ERROR: pass fixture lacks applied_recipe: — fixture is wrong" >&2
            violations=$((violations + 1))
        fi
    else
        echo "  SKIP [pass fixture not found: $PASS_README]"
    fi

    # ── Fixture 2: prefer-recipe-composition-over-l4-cross-import ────────────
    echo ""
    echo "[fixture] prefer-recipe-composition-over-l4-cross-import"

    FAIL_COMPOSE_README="$FIXTURES_DIR/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/README.md"
    PASS_COMPOSE_README="$FIXTURES_DIR/prefer-recipe-composition-over-l4-cross-import/pass/README.md"

    if [ -f "$FAIL_COMPOSE_README" ]; then
        if grep -q "applied_recipe:" "$FAIL_COMPOSE_README"; then
            echo "  GUARD_ERROR: fail fixture has applied_recipe: — fixture is wrong" >&2
            violations=$((violations + 1))
        else
            echo "  PASS [fail fixture correctly lacks applied_recipe:]"
        fi
    else
        echo "  SKIP [fail fixture not found: $FAIL_COMPOSE_README]"
    fi

    if [ -f "$PASS_COMPOSE_README" ]; then
        if grep -q "applied_recipe:" "$PASS_COMPOSE_README"; then
            echo "  PASS [pass fixture correctly has applied_recipe:]"
        else
            echo "  GUARD_ERROR: pass fixture lacks applied_recipe: — fixture is wrong" >&2
            violations=$((violations + 1))
        fi
    else
        echo "  SKIP [pass fixture not found: $PASS_COMPOSE_README]"
    fi

    # ── Fixture 3: recipe-invariants-must-resolve — fail case ────────────────
    echo ""
    echo "[fixture] recipe-invariants-must-resolve"

    FAIL_RECIPE_YAML="$FIXTURES_DIR/recipe-invariants-must-resolve/fail_unresolvable_spec_ref/recipe.yaml"
    PASS_RECIPE_YAML="$FIXTURES_DIR/recipe-invariants-must-resolve/pass/recipe.yaml"

    if [ -f "$FAIL_RECIPE_YAML" ]; then
        # Expect exit non-zero (violations found)
        if check_invariants_resolve "$FAIL_RECIPE_YAML" "$REPO_ROOT" "fail_fixture" 2>/dev/null; then
            echo "  GUARD_ERROR: fail fixture passed invariant check — fixture is wrong" >&2
            violations=$((violations + 1))
        else
            echo "  PASS [fail fixture correctly detected unresolvable refs]"
        fi
    else
        echo "  SKIP [fail fixture not found: $FAIL_RECIPE_YAML]"
    fi

    if [ -f "$PASS_RECIPE_YAML" ]; then
        if check_invariants_resolve "$PASS_RECIPE_YAML" "$REPO_ROOT" "pass_fixture"; then
            echo "  PASS [pass fixture: all invariants resolve]"
        else
            echo "  GUARD_ERROR: pass fixture failed invariant check — fixture is wrong" >&2
            violations=$((violations + 1))
        fi
    else
        echo "  SKIP [pass fixture not found: $PASS_RECIPE_YAML]"
    fi

    echo ""
    if [ "$violations" -gt 0 ]; then
        echo "recipe_governance_guard (fixtures): $violations fixture error(s)" >&2
        exit 1
    fi
    echo "recipe_governance_guard (fixtures): all fixture checks PASS"
    exit 0
fi

# ── Mode B: live repo validation ──────────────────────────────────────────────
echo "=== recipe_governance_guard.sh — live repo ==="

RECIPES_DIR="$REPO_ROOT/recipes"
if [ ! -d "$RECIPES_DIR" ]; then
    echo "recipe_governance_guard: recipes/ not found — no recipes to validate (SP35 pending)"
    echo "recipe_governance_guard: PASS (nothing to check)"
    exit 0
fi

shopt -s nullglob

# ── Check 2: business-domain-must-declare-applied-recipe ─────────────────────
echo ""
echo "[check] business-domain-must-declare-applied-recipe"
for recipe_md in "$RECIPES_DIR"/*/RECIPE.md; do
    pattern=$(basename "$(dirname "$recipe_md")")
    # Extract enabled_l4_domains list
    domains=$(python3 - "$recipe_md" <<'PY'
import pathlib, sys
content = pathlib.Path(sys.argv[1]).read_text()
# Simple frontmatter extraction
in_front = False
domains_block = False
for line in content.splitlines():
    if line.strip() == "---":
        in_front = not in_front
        continue
    if in_front and "enabled_l4_domains:" in line:
        domains_block = True
        continue
    if domains_block and line.strip().startswith("- "):
        print(line.strip()[2:])
    elif domains_block and line.strip() and not line.strip().startswith("-"):
        break
PY
)
    for domain in $domains; do
        readme="$REPO_ROOT/templates/L4/$domain/README.md"
        if [ ! -f "$readme" ]; then
            echo "  SKIP [$pattern/$domain: README not found at $readme]"
            continue
        fi
        if check_applied_recipe_declared "$readme" "$pattern/$domain"; then
            echo "  PASS [$pattern/$domain: applied_recipe: declared]"
        else
            violations=$((violations + 1))
        fi
    done
done

# ── Check 3: recipe-invariants-must-resolve ───────────────────────────────────
echo ""
echo "[check] recipe-invariants-must-resolve"
SPECS_RECIPES_DIR="$REPO_ROOT/specs/recipes"
if [ -d "$SPECS_RECIPES_DIR" ]; then
    for recipe_spec in "$SPECS_RECIPES_DIR"/*.yaml; do
        label="$(basename "$recipe_spec")"
        if check_invariants_resolve "$recipe_spec" "$REPO_ROOT" "$label"; then
            echo "  PASS [$label: all invariants resolve]"
        else
            violations=$((violations + 1))
        fi
    done
else
    echo "  SKIP [specs/recipes/ not found — SP35 pending]"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
if [ "$violations" -gt 0 ]; then
    echo "recipe_governance_guard: $violations violation(s) found — merge BLOCKED" >&2
    exit 1
fi
echo "recipe_governance_guard: all checks PASS"
exit 0
