#!/usr/bin/env bash
# practices/scripts/pre-commit-fast-guards.sh — R99 pre-wave fast guard subset.
#
# Mechanical closure of the R98 P12 F10 NEW finding (pre-commit guard timing
# gap). Per docs/dogfood-ledger/r98-iter2.yaml + persona-registry.yaml
# review_summary.R98.cross_wave_observations.r98_p12_new_gap_signal:
#
#   "spec_policy_ref_guard caught chat-messaging-l0 7 policy_ref orphans at
#    verify-completion stage POST-commit. 3-gate model (pre-commit /
#    pre-push / verify-completion) — pre-commit hook doesn't currently run
#    full catalog-meta guard set (speed budget). R99 candidate: define
#    pre-commit guard subset (speed-budgeted)."
#
# This script is the catalog-side helper for fork-receivers to opt into.
# Fork-receivers wiring up .git/hooks/pre-commit invoke this script.
# Catalog provides the contract; fork-receiver chooses adoption per the
# CLAUDE.md fork-receiver-decides-git-policy principle.
#
# Fast subset (target < 10s on warm filesystem cache):
#   1. spec_ref_guard                         — spec referential integrity
#   2. spec_policy_ref_guard                  — policy_ref ↔ blueprints/*.yaml resolution
#   3. manifest_yaml_strict_parse_guard       — YAML parse correctness
#   4. dogfood_ledger_guard                   — ledger schema
#   5. dogfood_finding_expiry_trigger_guard   — scope_deferral expiry markers
#   6. wave_kickoff_ledger_guard              — 48th (phase α atomic)
#   7. registry_backfill_completeness_guard   — 49th (commit-5 backfill)
#
# Excluded from fast subset (slow / require deep file scans):
#   • evidence_guard          (505 files scanned, ~3-5s)
#   • substance_guard         (cross-rule consistency)
#   • time_decay_guard        (date arithmetic across snapshots)
#   • trio_integrity_guard    (cross-file Spec Trio consistency)
#   • ledger_audit_*          (large ledger walks)
#   • All tenant-scope guards (multi-file kafka/realtime/broker walks)
#   • recipe_governance_guard (recipe-tree walk)
#   • test_tag_naming_convention_guard (Java AST parse)
#
# Effect: any fork-receiver invoking this in pre-commit catches the
# R98-class issues (policy_ref orphans, missing expiry markers, ledger
# schema breaks, wave_kickoff/registry_backfill omissions) BEFORE the
# commit lands. Verify-completion + pre-push still cover the remaining
# 42 guards (deeper catalog-meta + cross-cutting). 3-gate model:
#   pre-commit       → fast-7 subset (this script)
#   pre-push         → completion_checklist_recency_guard (49th hard guard)
#   verify-completion → all 49 guards (Iron Law R25)
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.
#
# Usage (fork-receiver opt-in):
#   # Direct invocation
#   bash practices/scripts/pre-commit-fast-guards.sh
#
#   # As pre-commit hook (.git/hooks/pre-commit)
#   #!/usr/bin/env bash
#   bash practices/scripts/pre-commit-fast-guards.sh || exit 1
#
#   # Or via Husky / lefthook / pre-commit framework
#   # (depends on fork-receiver's tooling — catalog does not prescribe)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
EVALS_DIR="$REPO_ROOT/practices/evals"

VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --verbose|-v) VERBOSE=1; shift ;;
        --help|-h)
            sed -n '2,60p' "$0"
            exit 0
            ;;
        *) echo "pre-commit-fast-guards: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$EVALS_DIR" ]; then
    echo "pre-commit-fast-guards: practices/evals/ not found at $EVALS_DIR" >&2
    exit 2
fi

START_TS=$(date +%s)
PASS=0
FAIL=0
RESULTS=()

run_fast_guard() {
    local label="$1"
    local script="$2"
    local before
    local after

    before=$(date +%s)
    if bash "$EVALS_DIR/$script" >/dev/null 2>&1; then
        after=$(date +%s)
        PASS=$((PASS + 1))
        RESULTS+=("PASS [$label] ($((after - before))s)")
        [ "$VERBOSE" -eq 1 ] && echo "  ✓ $label"
    else
        after=$(date +%s)
        FAIL=$((FAIL + 1))
        RESULTS+=("FAIL [$label] ($((after - before))s)")
        echo "  ✗ $label — re-run standalone for details: bash $EVALS_DIR/$script" >&2
    fi
}

echo "=== pre-commit-fast-guards (R99 fast-7 subset) ==="

run_fast_guard "spec_ref"                       "spec_ref_guard.sh"
run_fast_guard "spec_policy_ref"                "spec_policy_ref_guard.sh"
run_fast_guard "manifest_yaml_strict_parse"     "manifest_yaml_strict_parse_guard.sh"
run_fast_guard "dogfood_ledger"                 "dogfood_ledger_guard.sh"
run_fast_guard "dogfood_finding_expiry_trigger" "dogfood_finding_expiry_trigger_guard.sh"
run_fast_guard "wave_kickoff_ledger"            "wave_kickoff_ledger_guard.sh"
run_fast_guard "registry_backfill_completeness" "registry_backfill_completeness_guard.sh"

END_TS=$(date +%s)
ELAPSED=$((END_TS - START_TS))

if [ "$VERBOSE" -eq 1 ]; then
    echo ""
    echo "=== Results ==="
    for r in "${RESULTS[@]}"; do
        echo "  $r"
    done
fi

echo ""
echo "pre-commit-fast-guards: $PASS PASS · $FAIL FAIL · ${ELAPSED}s elapsed"

if [ "$FAIL" -gt 0 ]; then
    echo "pre-commit-fast-guards: FAIL — commit blocked. Fix the violation(s) above or run with --verbose for timing details." >&2
    echo "pre-commit-fast-guards: To bypass for emergency (NOT recommended): \`git commit --no-verify\`. Iron Law R25 still applies — verify-completion must PASS before declaring task done." >&2
    exit 1
fi

# Time budget warning (fast subset should stay under 10s on warm cache).
if [ "$ELAPSED" -gt 15 ]; then
    echo "pre-commit-fast-guards: WARNING — elapsed ${ELAPSED}s exceeds 15s soft budget. Consider trimming subset or investigating slow guard via --verbose." >&2
fi

exit 0
