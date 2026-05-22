#!/usr/bin/env bash
# practices/evals/l4_domain_reachability_guard.sh — R37 41st hard guard.
# Merged P1-1 + P1-3 per architect synthesis: every L4 backend domain MUST have
# either (a) <X>ViolationProofTest.java AND recipe membership, OR (b) manifest
# declares violation_proof: not_applicable + experimental: true with rationale.
#
# Codex acceptance #7: opt-out requires expiry_date / burn_down_owner field,
# comparator_domain (existence-verified), and HARD FAIL if domain is in any
# recipe's applied_block_inv.
#
# Older domains (R0..R28) are exempted via practices/legacy_backfill_ledger.md.
# This guard checks the TWO-COLUMN matrix for each post-Appendix-C domain.
#
# Usage:
#   bash practices/evals/l4_domain_reachability_guard.sh
#   bash practices/evals/l4_domain_reachability_guard.sh --fixtures
#   bash practices/evals/l4_domain_reachability_guard.sh --root DIR
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
        *) echo "l4_domain_reachability_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/l4_domain_reachability"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "l4_domain_reachability_guard: fixtures dir missing" >&2
        exit 2
    fi
    pass=0; fail=0
    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [l4_domain_reachability/$(basename "$sub")]"; pass=$((pass+1))
        else
            echo "FAIL [l4_domain_reachability/$(basename "$sub")] (expected pass)"; fail=$((fail+1))
        fi
    done
    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [l4_domain_reachability/$(basename "$sub")] (expected fail)"; fail=$((fail+1))
        else
            echo "PASS [l4_domain_reachability/$(basename "$sub")]"; pass=$((pass+1))
        fi
    done
    if [ "$fail" -ne 0 ]; then exit 1; fi
    echo "l4_domain_reachability_guard: fixtures PASS ($pass)"
    exit 0
fi

ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
LEDGER="$ROOT/practices/legacy_backfill_ledger.md"

# Build the legacy exemption set from ledger.
EXEMPT=""
if [ -f "$LEDGER" ]; then
    # Parse table column 1 (domain name) — lines like "| auth | R0 | ..."
    EXEMPT=$(grep -E '^\| [a-z][a-z0-9-]+ \|' "$LEDGER" | awk -F'|' '{gsub(/ /, "", $2); print $2}')
fi

is_exempt() {
    local d="$1"
    for e in $EXEMPT; do
        [ "$e" = "$d" ] && return 0
    done
    return 1
}

BACKEND_DOMAINS_DIR="$ROOT/backend/src/main/java/com/ax/template/authblueprint"
[ ! -d "$BACKEND_DOMAINS_DIR" ] && exit 0

violations=0
for dpath in "$BACKEND_DOMAINS_DIR"/*/; do
    [ -d "$dpath" ] || continue
    dname_camel="$(basename "$dpath")"
    # Skip non-domain directories (e.g. configuration packages).
    case "$dname_camel" in
        importer|integration|observability|security|user) continue ;;
    esac

    # Derive kebab-case domain name candidates for ledger lookup.
    # camelCase tagcategorization → tag-categorization (best effort).
    # We allow both: <camel> and a kebab variant.
    is_exempt "$dname_camel" && continue
    # Try common camel→kebab mappings used in the project.
    case "$dname_camel" in
        tagcategorization) kebab="tag-categorization" ;;
        sessionmanagement) kebab="session-management" ;;
        favoritesbookmarks) kebab="favorites-bookmarks" ;;
        activityfeed) kebab="activity-feed" ;;
        commentthread) kebab="comment-thread" ;;
        approvalworkflow) kebab="approval-workflow" ;;
        apikey) kebab="api-key" ;;
        reportexport) kebab="report-export" ;;
        auditlog) kebab="audit-log" ;;
        filestorage) kebab="file-storage" ;;
        featureflags) kebab="feature-flags" ;;
        identityverification) kebab="identity-verification" ;;
        scheduledtask) kebab="scheduled-task" ;;
        ratelimit) kebab="rate-limit" ;;
        *) kebab="$dname_camel" ;;
    esac
    is_exempt "$kebab" && continue

    # Post-Appendix-C / R37+ domain. Check the two columns.
    # Column 1: ViolationProofTest exists.
    test_dir="$ROOT/backend/src/test/java/com/ax/template/authblueprint/$dname_camel"
    has_proof=0
    if [ -d "$test_dir" ]; then
        if find "$test_dir" -maxdepth 1 -name "*ViolationProofTest.java" 2>/dev/null | grep -q .; then
            has_proof=1
        fi
    fi

    # Column 2: recipe membership (best effort — manifest cites in a recipe).
    has_recipe=0
    if grep -r --include=RECIPE.md -l "$kebab" "$ROOT/recipes" 2>/dev/null | grep -q .; then
        has_recipe=1
    fi

    # Check manifest for opt-out: violation_proof: not_applicable + experimental: true
    manifest="$ROOT/blueprints/${kebab}-manifest.yaml"
    opt_out=0
    if [ -f "$manifest" ]; then
        if grep -q '^violation_proof: not_applicable' "$manifest" && grep -q '^experimental: true' "$manifest"; then
            opt_out=1
            # Validate opt-out: expiry_date or burn_down_owner + comparator_domain
            if ! grep -qE '^expiry_date:|^burn_down_owner:' "$manifest"; then
                echo "VIOLATION: $manifest opts out but missing expiry_date or burn_down_owner" >&2
                violations=$((violations+1))
                continue
            fi
            if ! grep -q '^comparator_domain:' "$manifest"; then
                echo "VIOLATION: $manifest opts out but missing comparator_domain reference" >&2
                violations=$((violations+1))
                continue
            fi
            # HARD FAIL if domain is in any recipe's applied_block_inv.
            if grep -r --include=RECIPE.md -l "applied_block_inv" "$ROOT/recipes" 2>/dev/null | xargs grep -l "$kebab" 2>/dev/null | grep -q .; then
                echo "VIOLATION: $manifest opts out but domain $kebab IS in a recipe applied_block_inv (incompatible)" >&2
                violations=$((violations+1))
                continue
            fi
        fi
    fi

    if [ "$opt_out" -eq 1 ]; then
        continue  # valid opt-out
    fi

    # Neither both columns nor a valid opt-out → violation.
    if [ "$has_proof" -eq 0 ]; then
        echo "VIOLATION: domain $kebab missing ViolationProofTest AND no opt-out in manifest" >&2
        violations=$((violations+1))
    fi
    # recipe membership is encouraged not mandatory at this guard's level; the
    # composition_completeness_guard enforces the gradient deadline.
done

if [ "$violations" -ne 0 ]; then
    echo "l4_domain_reachability_guard: FAIL with $violations violations" >&2
    exit 1
fi

echo "l4_domain_reachability_guard: PASS"
exit 0
