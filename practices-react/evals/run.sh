#!/usr/bin/env bash
# practices-react/evals/run.sh — binary gate runner for the React catalog.
#
# Invokes the four hard gates (spec_ref / time_decay / evidence / substance) with
# --catalog=practices-react. Each gate already lives in practices/evals/ — we
# do not duplicate them, we just delegate.
#
# BACKLOG P2-37 (closed): substance_guard was the one gate NOT wired here — not a
# design decision to leave React ungated, just the one missing invocation. It runs
# its own React-specific dialect (dialect=react-frozen-v1 — see substance_guard.sh
# header) because the Java dialect's body markers (`**Incorrect`/`**Correct`/
# `Reference:`) do not port (only 16/15/25 of the 102 React rules carry them).
#
# Exit codes:
#   0 — all four gates pass
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
run_gate "substance_guard"  "${GUARDS_DIR}/substance_guard.sh"  --catalog practices-react

if [ "$failures" -gt 0 ]; then
    echo
    echo "practices-react/evals/run.sh: $failures gate(s) failed — merge BLOCKED" >&2
    exit 1
fi

echo
echo "practices-react/evals/run.sh: all 4 gates passed"
exit 0
