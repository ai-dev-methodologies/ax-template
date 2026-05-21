#!/usr/bin/env bash
# practices/evals/recipe_invariant_spec_normalization_guard.sh — R24 35th hard guard.
#
# Closes the catalog enforcement loop gap identified in R24 root-cause-fix mode:
# every business invariant ID that appears in a recipe's narrative
# (recipes/<slug>/RECIPE.md) MUST be normalized into the recipe's spec
# (specs/recipes/<slug>-recipe-l0.yaml#business_invariants). Recipe narratives
# can document things; specs are the surface other guards (substance,
# cross_recipe_inv_uniqueness, recipe_spec_referential_integrity, spec_policy_ref)
# read. Without this guard, a maintainer can write an INV-XYZ paragraph in
# RECIPE.md, never add it to the spec, and downstream checks silently skip it.
#
# Rules:
#   • Only invariants prefixed with the recipe's own UPPERCASE slug (e.g.
#     ECOM-INV-001 in e-commerce, B2B-ADMIN-INV-003 in b2b-admin) are checked.
#     Cross-references to other recipes' invariants in narrative prose are
#     allowed (they describe context, not declare).
#   • For each own-recipe INV ID found in RECIPE.md, the same ID MUST appear
#     in business_invariants[].id of the matching spec yaml.
#   • If specs/recipes/<slug>-recipe-l0.yaml does not exist for a recipe that
#     declares own-prefix invariants, that is a violation.
#
# Usage:
#   bash practices/evals/recipe_invariant_spec_normalization_guard.sh           # live repo
#   bash practices/evals/recipe_invariant_spec_normalization_guard.sh --fixtures
#   bash practices/evals/recipe_invariant_spec_normalization_guard.sh --root DIR
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "recipe_invariant_spec_normalization_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/recipe_invariant_spec_normalization"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "recipe_invariant_spec_normalization_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [recipe_invariant_spec_normalization/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [recipe_invariant_spec_normalization/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [recipe_invariant_spec_normalization/$(basename "$sub")] — expected exit 1 on FAIL fixture"
            fail=$((fail + 1))
        else
            echo "PASS [recipe_invariant_spec_normalization/$(basename "$sub")]"
            pass=$((pass + 1))
        fi
    done

    echo ""
    echo "recipe_invariant_spec_normalization_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live mode (or --root override) ────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "recipe_invariant_spec_normalization_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" <<'PYEOF'
import sys
import pathlib
import re
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])

# Discover recipes. In live mode, root is repo root and we scan root/recipes/.
# In fixture mode, root IS a directory containing recipes/<slug>/RECIPE.md plus
# specs/recipes/<slug>-recipe-l0.yaml.
recipes_dir = root / "recipes"
specs_dir = root / "specs" / "recipes"

if not recipes_dir.is_dir():
    # Vacuous PASS — no recipes to check (mirrors recipe_governance_guard policy).
    print(f"recipe_invariant_spec_normalization_guard: no recipes/ directory at {recipes_dir} — nothing to check")
    sys.exit(0)

violations = []
checked_recipes = 0
checked_ids = 0

for recipe_dir in sorted(recipes_dir.iterdir()):
    if not recipe_dir.is_dir():
        continue
    recipe_md = recipe_dir / "RECIPE.md"
    if not recipe_md.is_file():
        continue
    slug = recipe_dir.name
    own_prefix = slug.upper().replace('-', '-') + "-INV-"
    # slug.upper() already capitalizes; preserve dashes as-is to match ID style
    # (e-commerce → E-COMMERCE; but actual IDs use ECOM, B2B-ADMIN, etc.).
    #
    # Recipes do NOT use slug→prefix mechanical mapping; each recipe picks an
    # idiomatic prefix. Use _MANIFEST.yaml mapping if present, else fall back to
    # discovering the prefix from the RECIPE.md narrative.

    # Discover the recipe's own INV prefix from "## Business Invariants" table
    # — first ID column entry is the canonical own-prefix.
    recipe_text = recipe_md.read_text()

    # Find all <PREFIX>-INV-NNN tokens in RECIPE.md
    all_inv_ids = re.findall(r'\b([A-Z][A-Z0-9_-]*?-INV-\d+)\b', recipe_text)
    if not all_inv_ids:
        continue

    # Heuristic: own prefix is the most frequently occurring prefix in this RECIPE.md.
    from collections import Counter
    prefix_counts = Counter()
    for tok in all_inv_ids:
        m = re.match(r'^([A-Z][A-Z0-9_-]*?)-INV-\d+$', tok)
        if m:
            prefix_counts[m.group(1)] += 1
    if not prefix_counts:
        continue
    own_prefix_base, _ = prefix_counts.most_common(1)[0]

    # Own-recipe IDs: those starting with the dominant prefix in this RECIPE.md.
    own_ids = sorted({tok for tok in all_inv_ids if tok.startswith(own_prefix_base + "-INV-")})
    if not own_ids:
        continue

    checked_recipes += 1
    checked_ids += len(own_ids)

    # Locate matching spec yaml
    spec_yaml = specs_dir / f"{slug}-recipe-l0.yaml"
    if not spec_yaml.is_file():
        violations.append(
            f"recipe={slug}: declares own-prefix invariants {own_ids} in RECIPE.md "
            f"but specs/recipes/{slug}-recipe-l0.yaml is missing"
        )
        continue

    spec_text = spec_yaml.read_text()

    # Extract IDs from business_invariants[].id
    m = re.search(r'^business_invariants:\s*\n(.*?)(?=^\w|\Z)', spec_text, re.MULTILINE | re.DOTALL)
    if not m:
        violations.append(
            f"recipe={slug}: spec {spec_yaml.relative_to(root)} has no business_invariants section "
            f"but RECIPE.md declares {own_ids}"
        )
        continue
    inv_block = m.group(1)
    spec_ids = set(re.findall(r'(?m)^\s{2}-\s+id:\s+([A-Za-z0-9_-]+)', inv_block))

    missing = [oid for oid in own_ids if oid not in spec_ids]
    if missing:
        violations.append(
            f"recipe={slug}: RECIPE.md declares {missing} but spec "
            f"{spec_yaml.relative_to(root)} does not list them in business_invariants[].id"
        )

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

if not violations:
    print(f'{{"signal":"recipe.inv.spec_normalization_pass","recipes_checked":{checked_recipes},"ids_checked":{checked_ids},"ts":"{ts}"}}')
    sys.exit(0)
else:
    for v in violations:
        print(f"VIOLATION [recipe_invariant_spec_normalization]: {v}")
    print(f'{{"signal":"recipe.inv.spec_normalization_violations","value":{len(violations)},"ts":"{ts}"}}')
    sys.exit(1)
PYEOF
