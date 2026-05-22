#!/usr/bin/env bash
# practices/evals/composition_completeness_guard.sh — R37 42nd hard guard.
# Architect provocative recommendation: every L4 domain must be in 1+
# recipes/*/RECIPE.md applied_block_inv within N commits of introduction, OR
# manifest declares experimental: true with comparator_domain.
#
# Gradient enforcement (architect synthesis vs blanket freeze):
#   - Domain N+1 (next domain added) must be in 1+ recipe within 5 commits
#   - Domain N+2 within 3 commits
#   - Domain N+3+ within 1 commit
#
# Older domains exempted via practices/legacy_backfill_ledger.md.
#
# v1 (R37) policy: WARN-level for now (informational; not yet HARD FAIL).
# Promote to HARD FAIL when next domain is added (R38+) to enforce gradient
# from R38 onward without retroactively breaking the existing tree.
#
# Usage:
#   bash practices/evals/composition_completeness_guard.sh
#   bash practices/evals/composition_completeness_guard.sh --strict   # HARD FAIL mode
#   bash practices/evals/composition_completeness_guard.sh --fixtures
#   bash practices/evals/composition_completeness_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
STRICT=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --strict) STRICT=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "composition_completeness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/composition_completeness"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "composition_completeness_guard: fixtures dir missing" >&2
        exit 2
    fi
    pass=0; fail=0
    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --strict --root "$sub" >/dev/null 2>&1; then
            echo "PASS [composition_completeness/$(basename "$sub")]"; pass=$((pass+1))
        else
            echo "FAIL [composition_completeness/$(basename "$sub")] (expected pass)"; fail=$((fail+1))
        fi
    done
    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --strict --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [composition_completeness/$(basename "$sub")] (expected fail)"; fail=$((fail+1))
        else
            echo "PASS [composition_completeness/$(basename "$sub")]"; pass=$((pass+1))
        fi
    done
    if [ "$fail" -ne 0 ]; then exit 1; fi
    echo "composition_completeness_guard: fixtures PASS ($pass)"
    exit 0
fi

ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
LEDGER="$ROOT/practices/legacy_backfill_ledger.md"

EXEMPT=""
if [ -f "$LEDGER" ]; then
    EXEMPT=$(grep -E '^\| [a-z][a-z0-9-]+ \|' "$LEDGER" | awk -F'|' '{gsub(/ /, "", $2); print $2}')
fi
is_exempt() {
    local d="$1"
    for e in $EXEMPT; do [ "$e" = "$d" ] && return 0; done
    return 1
}

# All known L4 kebab names from manifest files.
warnings=0; violations=0
for mf in "$ROOT"/blueprints/*-manifest.yaml; do
    [ -f "$mf" ] || continue
    base="$(basename "$mf" -manifest.yaml)"
    # Skip non-L4 manifests (auth, ratelimit etc are L4 but multi-tenant + ui not).
    case "$base" in
        *-ui|multi-tenant|i18n-policy|email-outbox) continue ;;
    esac
    is_exempt "$base" && continue

    # Check recipe membership.
    has_recipe=0
    if grep -r --include=RECIPE.md -l "$base" "$ROOT/recipes" 2>/dev/null | grep -q .; then
        has_recipe=1
    fi

    # Check experimental opt-out.
    is_experimental=0
    if grep -q '^experimental: true' "$mf" 2>/dev/null && grep -q '^comparator_domain:' "$mf" 2>/dev/null; then
        is_experimental=1
    fi

    if [ "$has_recipe" -eq 0 ] && [ "$is_experimental" -eq 0 ]; then
        if [ "$STRICT" -eq 1 ]; then
            echo "VIOLATION: domain $base not in any recipe and not marked experimental" >&2
            violations=$((violations+1))
        else
            echo "WARN: domain $base not in any recipe (gradient enforcement; promote to --strict when adding next domain)" >&2
            warnings=$((warnings+1))
        fi
    fi
done

if [ "$violations" -ne 0 ]; then
    echo "composition_completeness_guard: FAIL with $violations violations" >&2
    exit 1
fi

if [ "$warnings" -ne 0 ]; then
    echo "composition_completeness_guard: PASS with $warnings warnings (informational; not blocking)"
else
    echo "composition_completeness_guard: PASS"
fi
exit 0
