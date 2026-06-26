#!/usr/bin/env bash
# practices/evals/fail_fast_blocking_audit_guard.sh — pins the load-bearing fail-fast invariant.
#
# THE INVARIANT (binary, non-vacuous): verify-completion.sh's step-level fail-fast (a HARD_FAIL in a
# `fail_fast: true` step short-circuits the remaining steps) MUST NOT weaken the no-bypass contract.
# Concretely, when a fail_fast step FAILS the run MUST:
#   (a) short-circuit — print "⛔ fail-fast" and SKIP the heavy downstream steps; AND
#   (b) STILL write a BLOCKING audit line (exit=1, hard_fail>0, pass=0) to .ax-verify/runs.jsonl
#       — so the pre-push recency guard REJECTS the push. A fail-fast break must never yield a
#       passing/satisfying audit line (which would silently break "full R25 is the sole gate").
# And on a CLEAN run (the fail_fast step passes) fail-fast MUST NOT fire: the downstream step runs
# and the audit line is clean (exit=0, hard_fail=0, pass=2).
#
# WHY a committed guard: the fail-fast logic was proven correct at introduction (adversarial review +
# manual forced FAIL), but nothing pinned it — a future refactor of the Summary/audit block could
# silently regress the no-bypass guarantee with no test to catch it. This guard runs the REAL
# verify-completion.sh in an isolated harness (REPO_ROOT = a throwaway dir, so the real runs.jsonl is
# never touched) against two committed fixture checklists and asserts opposite, discriminating outcomes.
#
# Usage:
#   bash practices/evals/fail_fast_blocking_audit_guard.sh --checklist <yaml> --expect breaks|clean
#   bash practices/evals/fail_fast_blocking_audit_guard.sh            # runs both committed fixtures
# Exit 0 = invariant holds for the requested expectation. Exit 1 = invariant violated (BLOCK).
# Exit 2 = harness/setup error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURE_DIR="$SCRIPT_DIR/fixtures/fail-fast-blocking-audit"

CHECKLIST=""
EXPECT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --checklist) CHECKLIST="$2"; shift 2 ;;
        --expect) EXPECT="$2"; shift 2 ;;
        *) echo "fail_fast_blocking_audit_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

VC="$REPO_ROOT/practices/scripts/verify-completion.sh"
COLLAPSE="$REPO_ROOT/practices/scripts/_collapse_plan.py"
[ -f "$VC" ] || { echo "fail_fast_blocking_audit_guard: FAIL — verify-completion.sh not found at $VC" >&2; exit 2; }

# Run the REAL verify-completion.sh against one fixture checklist in an isolated harness and
# assert the requested expectation. Echoes nothing on success; prints the violation on failure.
run_scenario() {
    local checklist="$1" expect="$2"
    [ -f "$checklist" ] || { echo "  missing fixture checklist: $checklist" >&2; return 2; }

    local harness; harness="$(mktemp -d)"
    # shellcheck disable=SC2064
    trap "rm -rf '$harness'" RETURN
    mkdir -p "$harness/practices/scripts"
    cp "$VC" "$harness/practices/scripts/verify-completion.sh"
    [ -f "$COLLAPSE" ] && cp "$COLLAPSE" "$harness/practices/scripts/_collapse_plan.py"
    cp "$checklist" "$harness/practices/verification-checklist.yaml"

    # Capture via a FILE redirect, NOT command substitution: verify-completion spawns a per-step
    # timeout watchdog (a backgrounded sleep). Under `$(...)` capture, an orphaned watchdog can hold
    # the stdout pipe open and make the capture block until it dies (a race). A `>file` redirect lets
    # the parent return on the main script's waitpid, independent of any detached watchdog fd.
    local outfile="$harness/run.out" out exit_code
    bash "$harness/practices/scripts/verify-completion.sh" >"$outfile" 2>&1 && exit_code=0 || exit_code=$?
    out="$(cat "$outfile" 2>/dev/null)"

    local audit="$harness/.ax-verify/runs.jsonl"
    [ -f "$audit" ] || { echo "  [$expect] no audit line written (runs.jsonl absent) — break may have skipped the audit write" >&2; return 1; }
    local line; line="$(tail -1 "$audit")"
    local a_exit a_pass a_hf
    a_exit="$(printf '%s' "$line" | grep -oE '"exit":[0-9]+'      | grep -oE '[0-9]+')"
    a_pass="$(printf '%s' "$line" | grep -oE '"pass":[0-9]+'      | grep -oE '[0-9]+')"
    a_hf="$(  printf '%s' "$line" | grep -oE '"hard_fail":[0-9]+' | grep -oE '[0-9]+')"

    local has_ff=0
    printf '%s' "$out" | grep -qF "⛔ fail-fast" && has_ff=1

    local bad=0
    if [ "$expect" = "breaks" ]; then
        # fail_fast step FAILED → must short-circuit AND write a blocking audit line.
        [ "$exit_code" -ne 0 ]   || { echo "  [breaks] run exited 0 — a failing fail_fast step must exit non-zero" >&2; bad=1; }
        [ "$has_ff" -eq 1 ]      || { echo "  [breaks] '⛔ fail-fast' NOT printed — short-circuit did not fire" >&2; bad=1; }
        [ "$a_exit" = "1" ]      || { echo "  [breaks] audit exit=$a_exit (want 1) — recency guard would NOT block the push" >&2; bad=1; }
        [ "${a_hf:-0}" -ge 1 ]   || { echo "  [breaks] audit hard_fail=$a_hf (want >=1)" >&2; bad=1; }
        [ "$a_pass" = "0" ]      || { echo "  [breaks] audit pass=$a_pass (want 0) — downstream step was NOT skipped (fail-fast vacuous)" >&2; bad=1; }
    elif [ "$expect" = "clean" ]; then
        # fail_fast step PASSED → fail-fast must NOT fire; downstream runs; audit clean.
        [ "$exit_code" -eq 0 ]   || { echo "  [clean] run exited $exit_code (want 0) on an all-pass checklist" >&2; bad=1; }
        [ "$has_ff" -eq 0 ]      || { echo "  [clean] '⛔ fail-fast' printed on a clean run — fail-fast fired spuriously" >&2; bad=1; }
        [ "$a_exit" = "0" ]      || { echo "  [clean] audit exit=$a_exit (want 0)" >&2; bad=1; }
        [ "${a_hf:-1}" -eq 0 ]   || { echo "  [clean] audit hard_fail=$a_hf (want 0)" >&2; bad=1; }
        [ "$a_pass" = "2" ]      || { echo "  [clean] audit pass=$a_pass (want 2) — downstream step did NOT run" >&2; bad=1; }
    else
        echo "  unknown --expect '$expect' (want breaks|clean)" >&2; return 2
    fi
    return "$bad"
}

if [ -n "$CHECKLIST" ] && [ -n "$EXPECT" ]; then
    if run_scenario "$CHECKLIST" "$EXPECT"; then
        echo "fail_fast_blocking_audit_guard: PASS — [$EXPECT] invariant holds"
        exit 0
    fi
    echo "fail_fast_blocking_audit_guard: FAIL — [$EXPECT] invariant VIOLATED (the fail-fast no-bypass guarantee regressed)" >&2
    exit 1
fi

# Default: run both committed fixtures; pass only if both discriminating outcomes hold.
RC=0
run_scenario "$FIXTURE_DIR/failfast_breaks.yaml" breaks || RC=1
run_scenario "$FIXTURE_DIR/clean_runs_all.yaml"  clean  || RC=1
if [ "$RC" -eq 0 ]; then
    echo "fail_fast_blocking_audit_guard: PASS — fail-fast short-circuits with a blocking audit, and stays inert on clean runs"
    exit 0
fi
echo "fail_fast_blocking_audit_guard: FAIL — fail-fast no-bypass invariant violated" >&2
exit 1
