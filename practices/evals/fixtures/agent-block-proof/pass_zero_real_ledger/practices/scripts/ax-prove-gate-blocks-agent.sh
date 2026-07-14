#!/usr/bin/env bash
# FIXTURE proof script — a faithful, self-contained miniature of the real
# practices/scripts/ax-prove-gate-blocks-agent.sh. It TOGGLES correctly:
# block (Map) -> correct (ProblemDetail) -> pass, with the same
# 'blocked_rc -ne 1' fail-guard and a reference to controller_problemdetail_guard.sh.
# agent_block_proof_guard's pass fixture asserts this proof toggles and is non-vacuous.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/controller_problemdetail_guard.sh"

LEDGER="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}/events.jsonl"
mkdir -p "$(dirname "$LEDGER")"
log_agent() { printf '{"kind": "%s", "actor": "agent"}\n' "$1" >> "$LEDGER"; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
PKG="$TMP/backend/src/main/java/com/ax/template/authblueprint/demo"
mkdir -p "$PKG"
CTRL="$PKG/DemoController.java"

# 1. agent writes a VIOLATING handler (returns Map)
printf 'public Map<String, String> handle() { return null; }\n' > "$CTRL"

# 2. real-shaped guard must BLOCK (exit 1)
set +e
bash "$GUARD" --root "$TMP" >/dev/null 2>&1
blocked_rc=$?
set -e
if [ "$blocked_rc" -ne 1 ]; then
    echo "[FAIL] guard did NOT block (exit $blocked_rc, expected 1)"
    exit 1
fi
log_agent violation

# 3. agent corrects to ProblemDetail
printf 'public ProblemDetail handle() { return null; }\n' > "$CTRL"

# 4. same guard must now PASS (exit 0)
set +e
bash "$GUARD" --root "$TMP" >/dev/null 2>&1
fixed_rc=$?
set -e
if [ "$fixed_rc" -ne 0 ]; then
    echo "[FAIL] guard did NOT pass the fix (exit $fixed_rc, expected 0)"
    exit 1
fi
log_agent progress

echo "PROVEN (fixture): block -> pass held"
exit 0
