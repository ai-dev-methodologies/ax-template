#!/usr/bin/env bash
# practices/evals/recipe_sibling_sync_guard.sh — dogfood-7 (gap 5 closure, 31st hard guard).
#
# Validates that every recipes/<pattern>/RECIPE.md frontmatter matches its
# sibling specs/recipes/<pattern>-recipe-l0.yaml across three list keys:
#   - enabled_l4_domains
#   - l2_blocks_used
#   - l3_pages_used
#
# Both artifacts on disk MUST agree because both are read by downstream
# skills (/ax-scaffold, /ax-verify-recipe). Drift between them lets the
# human-readable RECIPE.md disagree with the machine-readable spec —
# fork-receivers and AI agents see different recipes.
#
# Drift policy:
#   - spec yaml is the machine-readable ground truth
#   - RECIPE.md is its human-facing mirror
#   - L1 primitives (templates/L1/components/) MUST NOT appear in
#     l2_blocks_used on either side
#
# Discovered by dogfood-7 (P2 R7): booking RECIPE.md had three L1 primitives
# (calendar / date-range-picker / relative-time) in l2_blocks_used that the
# spec yaml deliberately excluded. cross_trio_guard catches templates/
# imports but never compared the two recipe sibling files to each other.
#
# Exit codes:
#   0 — all recipe pairs in sync
#   1 — at least one drift row
#   2 — bad arguments / missing inputs

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "recipe_sibling_sync_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

RECIPES_DIR="$REPO_ROOT/recipes"
SPECS_DIR="$REPO_ROOT/specs/recipes"

if [ ! -d "$RECIPES_DIR" ] || [ ! -d "$SPECS_DIR" ]; then
    echo "recipe_sibling_sync_guard: missing recipes/ or specs/recipes/ dir" >&2
    exit 2
fi

# Extract a YAML list under a top-level key (between "key:" and next top-level key).
# Args: $1 = file, $2 = key name. Prints sorted unique entries (one per line).
extract_list() {
    local file="$1"
    local key="$2"
    awk -v k="^${key}:" '
        $0 ~ k                 { capture=1; next }
        capture && /^[a-zA-Z_]/{ capture=0 }
        capture && /^[[:space:]]*-[[:space:]]+/ {
            sub(/^[[:space:]]*-[[:space:]]+/, "")
            sub(/[[:space:]]*#.*/, "")
            sub(/[[:space:]]+$/, "")
            if (length($0) > 0) print
        }
    ' "$file" | sort -u
}

DRIFT=0
DRIFT_ROWS=()
PAIRS=0

for recipe_md in "$RECIPES_DIR"/*/RECIPE.md; do
    [ -f "$recipe_md" ] || continue
    pattern=$(basename "$(dirname "$recipe_md")")
    spec_yaml="$SPECS_DIR/${pattern}-recipe-l0.yaml"

    if [ ! -f "$spec_yaml" ]; then
        DRIFT_ROWS+=("MISSING_SPEC pattern=$pattern expected=$spec_yaml")
        DRIFT=$((DRIFT + 1))
        continue
    fi
    PAIRS=$((PAIRS + 1))

    for key in enabled_l4_domains l2_blocks_used l3_pages_used; do
        recipe_list=$(extract_list "$recipe_md" "$key")
        spec_list=$(extract_list "$spec_yaml" "$key")

        only_recipe=$(comm -23 <(echo "$recipe_list") <(echo "$spec_list") | grep -v '^$' || true)
        only_spec=$(comm -13 <(echo "$recipe_list") <(echo "$spec_list") | grep -v '^$' || true)

        if [ -n "$only_recipe" ] || [ -n "$only_spec" ]; then
            DRIFT=$((DRIFT + 1))
            DRIFT_ROWS+=("DRIFT pattern=$pattern key=$key")
            if [ -n "$only_recipe" ]; then
                while IFS= read -r item; do
                    DRIFT_ROWS+=("  +recipe_md_only: $item")
                done <<< "$only_recipe"
            fi
            if [ -n "$only_spec" ]; then
                while IFS= read -r item; do
                    DRIFT_ROWS+=("  +spec_yaml_only: $item")
                done <<< "$only_spec"
            fi
        fi
    done
done

if [ "$DRIFT" -gt 0 ]; then
    echo "recipe_sibling_sync_guard: FAIL — ${DRIFT} drift finding(s) across ${PAIRS} pair(s)" >&2
    for row in "${DRIFT_ROWS[@]}"; do
        echo "  $row" >&2
    done
    echo "" >&2
    echo "Fix policy: spec yaml is ground truth. Reconcile RECIPE.md to match" >&2
    echo "the sibling spec, OR add the missing entry to the spec yaml if it is" >&2
    echo "a legitimate new dependency. L1 primitives (calendar / date-range-picker /" >&2
    echo "relative-time / etc.) MUST NOT appear in l2_blocks_used." >&2
    exit 1
fi

echo "recipe_sibling_sync_guard: PASS — ${PAIRS} recipe pair(s) in sync across 3 keys"
exit 0
