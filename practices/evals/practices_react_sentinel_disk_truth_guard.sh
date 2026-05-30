#!/usr/bin/env bash
# practices/evals/practices_react_sentinel_disk_truth_guard.sh
# 2026-05-30 consistency audit (Phase 2 candidate C5) — the React mirror of
# agents_md_toc_disk_truth_guard.sh.
#
# The Java catalog has agents_md_toc_disk_truth_guard.sh (run-all-guards step
# [11]) which re-runs practices/generate_agents.sh and diffs against the
# committed practices/AGENTS.md, so a rule add that forgets the regen is BLOCKED.
# The React catalog (practices-react/) had NO equivalent — its generate_agents.sh
# also writes SKILL.md (rule_count + family table), but nothing re-ran it. Result
# (caught by the audit): the catalog grew 68→86 rules; AGENTS.md sentinel updated
# to 86 but SKILL.md's intro count + family table silently stayed at 68 — a
# generated sentinel that LIED about its own coverage, with no mechanical guard.
#
# This guard binary-verifies the React sentinel matches disk truth:
#   1. snapshot committed AGENTS.md + SKILL.md
#   2. re-run practices-react/generate_agents.sh
#   3. diff the regenerated files against the snapshots (any diff = drift)
#   4. restore the snapshots (the guard must not leave a mutated tree)
#   5. assert the SKILL.md family-table Rules column sums to the AGENTS.md
#      rule_count (the reconciliation invariant — the table must never
#      under-report total coverage)
#
# Exit: 0 PASS · 1 drift/non-idempotent · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/practices_react_sentinel_disk_truth_guard.sh
#   bash practices/evals/practices_react_sentinel_disk_truth_guard.sh --root DIR
#     (fixture mode: DIR must contain committed AGENTS.md + SKILL.md +
#      regenerated AGENTS.md + SKILL.md named *.committed / *.regenerated)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PR_DIR="$REPO_ROOT/practices-react"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "practices_react_sentinel_disk_truth_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Sum the "| Family | N | ... |" Rules column inside the auto-family-table block.
family_table_sum() {
    awk '/BEGIN:auto-family-table/,/END:auto-family-table/' "$1" \
        | grep -oE '\|[[:space:]]*[0-9]+[[:space:]]*\|' \
        | grep -oE '[0-9]+' \
        | awk '{s += $1} END {print s + 0}'
}

# rule_count from the AGENTS.md frontmatter sentinel.
agents_rule_count() {
    grep -oE 'rule_count:[[:space:]]*[0-9]+' "$1" | grep -oE '[0-9]+' | head -1
}

if [ -n "$ROOT_OVERRIDE" ]; then
    A_C="$ROOT_OVERRIDE/AGENTS.committed"; A_R="$ROOT_OVERRIDE/AGENTS.regenerated"
    S_C="$ROOT_OVERRIDE/SKILL.committed";  S_R="$ROOT_OVERRIDE/SKILL.regenerated"
    for f in "$A_C" "$A_R" "$S_C" "$S_R"; do
        [ -f "$f" ] || { echo "FAIL: fixture missing $f" >&2; exit 2; }
    done
    diff -q "$A_C" "$A_R" >/dev/null || { echo "FAIL: AGENTS.md drift (fixture)" >&2; exit 1; }
    diff -q "$S_C" "$S_R" >/dev/null || { echo "FAIL: SKILL.md drift (fixture)" >&2; exit 1; }
    exit 0
fi

[ -d "$PR_DIR" ]                       || { echo "practices_react_sentinel_disk_truth_guard: $PR_DIR not found" >&2; exit 2; }
[ -f "$PR_DIR/generate_agents.sh" ]    || { echo "practices_react_sentinel_disk_truth_guard: generate_agents.sh missing" >&2; exit 2; }
[ -f "$PR_DIR/AGENTS.md" ]             || { echo "practices_react_sentinel_disk_truth_guard: AGENTS.md missing" >&2; exit 2; }
[ -f "$PR_DIR/SKILL.md" ]              || { echo "practices_react_sentinel_disk_truth_guard: SKILL.md missing" >&2; exit 2; }

SNAP_A="$(mktemp -t pr_agents.XXXXXX)"
SNAP_S="$(mktemp -t pr_skill.XXXXXX)"
GEN_LOG="$(mktemp -t pr_gen.XXXXXX)"
trap 'rm -f "$SNAP_A" "$SNAP_S" "$GEN_LOG"' EXIT

cp "$PR_DIR/AGENTS.md" "$SNAP_A"
cp "$PR_DIR/SKILL.md"  "$SNAP_S"

if ! ( cd "$PR_DIR" && bash generate_agents.sh ) > "$GEN_LOG" 2>&1; then
    echo "FAIL: practices-react/generate_agents.sh exited non-zero" >&2
    cat "$GEN_LOG" >&2
    cp "$SNAP_A" "$PR_DIR/AGENTS.md"; cp "$SNAP_S" "$PR_DIR/SKILL.md"
    exit 1
fi

rc=0
if ! diff -q "$SNAP_A" "$PR_DIR/AGENTS.md" >/dev/null; then
    echo "FAIL: practices-react/AGENTS.md drifted from disk truth (rule add without regen, or non-idempotent generator)" >&2
    rc=1
fi
if ! diff -q "$SNAP_S" "$PR_DIR/SKILL.md" >/dev/null; then
    echo "FAIL: practices-react/SKILL.md (rule_count / family table) drifted from disk truth — re-run practices-react/generate_agents.sh and commit" >&2
    rc=1
fi

# Restore the committed files regardless of diff outcome.
cp "$SNAP_A" "$PR_DIR/AGENTS.md"; cp "$SNAP_S" "$PR_DIR/SKILL.md"
[ "$rc" -eq 0 ] || exit 1

# Reconciliation invariant: the family-table Rules column MUST sum to rule_count
# so the table never under-reports coverage (the exact bug this guard closes).
SUM="$(family_table_sum "$PR_DIR/SKILL.md")"
COUNT="$(agents_rule_count "$PR_DIR/AGENTS.md")"
if [ -z "$COUNT" ]; then
    echo "FAIL: could not read rule_count from practices-react/AGENTS.md sentinel" >&2
    exit 1
fi
if [ "$SUM" != "$COUNT" ]; then
    echo "FAIL: SKILL.md family-table sums to $SUM but AGENTS.md rule_count is $COUNT — the table under-reports coverage" >&2
    exit 1
fi

echo "practices_react_sentinel_disk_truth_guard: AGENTS.md + SKILL.md match disk truth (rule_count=$COUNT, family-table sum=$SUM)"
exit 0
