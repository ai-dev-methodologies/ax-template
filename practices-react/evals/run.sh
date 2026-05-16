#!/usr/bin/env bash
# practices-react/evals/run.sh — binary gate runner for the React catalog.
#
# Invokes the three hard gates (spec_ref / time_decay / evidence) with
# --catalog=practices-react. Each gate already lives in practices/evals/ — we
# do not duplicate them, we just delegate.
#
# Exit codes:
#   0 — all three gates pass
#   1 — at least one gate failed
#
# Usage:
#   bash practices-react/evals/run.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
GUARDS_DIR="${REPO_ROOT}/practices/evals"

failures=0

run_gate() {
    local name="$1"; shift
    echo "── $name ──"
    if bash "$@"; then
        echo "  PASS"
    else
        echo "  FAIL"
        failures=$((failures + 1))
    fi
}

run_gate "spec_ref_guard"   "${GUARDS_DIR}/spec_ref_guard.sh"   --catalog practices-react
run_gate "time_decay_guard" "${GUARDS_DIR}/time_decay_guard.sh" --catalog practices-react
run_gate "evidence_guard"   "${GUARDS_DIR}/evidence_guard.sh"   --catalog practices-react

if [ "$failures" -gt 0 ]; then
    echo
    echo "practices-react/evals/run.sh: $failures gate(s) failed — merge BLOCKED" >&2
    exit 1
fi

echo
echo "practices-react/evals/run.sh: all 3 gates passed"
exit 0
