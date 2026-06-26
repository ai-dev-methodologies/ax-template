#!/usr/bin/env bash
# verify/quick-verify.sh — ITERATION-ONLY fast feedback during development.
#
#   ⚠️  THIS IS NOT THE R25 COMPLETION GATE.
#   It DOES NOT write the .ax-verify/runs.jsonl audit log and DOES NOT satisfy the pre-push
#   recency guard. You MUST run `bash practices/scripts/verify-completion.sh` (R25) and see it
#   PASS at HEAD before declaring a task done / pushing. quick-verify is a strict SUBSET of R25's
#   steps (build + structural-pregate testPractices + your domain test(s) + run-all-guards) that
#   skips the full ~84-task per-domain suite and the aggregate test — for a ~1-2 min dev loop on a
#   single domain instead of the ~18 min full run.
#
# Usage:  bash verify/quick-verify.sh <testDomainTask> [more testTasks...]
#   e.g.  bash verify/quick-verify.sh testRatingSummary
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || { echo "quick-verify: cannot cd repo root" >&2; exit 2; }

[ "$#" -ge 1 ] || { echo "usage: bash verify/quick-verify.sh <testDomainTask> [more...]" >&2; exit 2; }

echo "════════════════════════════════════════════════════════════════════════════════"
echo "  quick-verify — ITERATION-ONLY fast feedback. NOT the R25 completion gate."
echo "  It does NOT write the audit log; run  bash practices/scripts/verify-completion.sh"
echo "  (R25) and see it PASS at HEAD before declaring done / pushing."
echo "════════════════════════════════════════════════════════════════════════════════"

FAIL=0
GRADLE_ARGS="--no-daemon -Dorg.gradle.jvmargs=-Xmx2g"

echo ""
echo "▸ backend compile"
( cd backend && ./gradlew build -x test $GRADLE_ARGS ) || { echo "  ✗ compile FAILED"; FAIL=1; }

if [ "$FAIL" -eq 0 ]; then
    echo ""
    echo "▸ structural-pregate (testPractices) + domain test(s): $*"
    ( cd backend && ./gradlew testPractices "$@" $GRADLE_ARGS ) || { echo "  ✗ structural/domain test FAILED"; FAIL=1; }
fi

if [ "$FAIL" -eq 0 ]; then
    echo ""
    echo "▸ run-all-guards"
    bash practices/evals/run-all-guards.sh || { echo "  ✗ a guard FAILED"; FAIL=1; }
fi

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "quick-verify: FAIL — fix, then re-run.  (Reminder: the full R25 is the completion gate.)"
    exit 1
fi
echo "quick-verify: PASS (ITERATION-ONLY — run verify-completion.sh and see R25 PASS before declaring done)."
exit 0
