#!/usr/bin/env bash
# FIXTURE proof script — DELIBERATELY VACUOUS. It exits 0 unconditionally and
# never actually runs any gate, so the block-then-pass thesis is NOT proven:
# it carries no return-code fail-guard and references no real enforcement guard.
# agent_block_proof_guard's fail fixture asserts this is caught (exit 1):
# without the non-vacuity checks, a proof like this would still report success.
set -euo pipefail
echo "[1] (vacuous) pretending an agent wrote a violating handler"
echo "[2] (vacuous) pretending a gate blocked it"
echo "[4] (vacuous) pretending a gate passed the fix"
echo "PROVEN (falsely): this exits 0 without ever exercising a real gate"
exit 0
