#!/usr/bin/env bash
# skills/ax-verify-L1/scripts/run.sh
# Tier-2 L1 layer verifier orchestrator.
# Steps 1-5 are binary.
# Exit 0 iff all steps pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-L1: run.sh ==="
echo ""

L1_DIR="$REPO_ROOT/templates/L1"

if [ ! -d "$L1_DIR" ]; then
    echo "  SKIP [all] templates/L1/ not yet present — pre-SP5 state"
    echo ""
    echo "=== ax-verify-L1: SKIP (no L1 content yet) ==="
    exit 0
fi

# Step 1: evidence_guard scoped to templates/L1/
echo "[1] evidence_guard (templates/L1/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$L1_DIR"; then
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 2: shadcn drift check
echo ""
echo "[2] shadcn drift check (templates/L1/_check-shadcn-drift.sh)"
DRIFT_SCRIPT="$L1_DIR/_check-shadcn-drift.sh"
if [ -f "$DRIFT_SCRIPT" ]; then
    if bash "$DRIFT_SCRIPT"; then
        echo "  PASS [shadcn-drift]"
    else
        echo "  FAIL [shadcn-drift]" >&2
        echo "  hint: update snapshot or revert component change" >&2
        exit 1
    fi
else
    echo "  SKIP [shadcn-drift] _check-shadcn-drift.sh not yet present"
fi

# Step 3: token-contract Vitest spec
echo ""
echo "[3] Vitest token-contract spec"
TOKEN_SPEC="$REPO_ROOT/frontend/tests/L1/token-contract.spec.ts"
if [ -f "$TOKEN_SPEC" ]; then
    cd "$REPO_ROOT/frontend" && npx vitest run tests/L1/token-contract.spec.ts
    echo "  PASS [token-contract]"
else
    echo "  SKIP [token-contract] tests/L1/token-contract.spec.ts not yet present"
fi

# Step 4: fork-receiver smoke
echo ""
echo "[4] fork-receiver smoke (verify/fork-receiver-smoke.sh)"
SMOKE_SCRIPT="$REPO_ROOT/verify/fork-receiver-smoke.sh"
if [ -f "$SMOKE_SCRIPT" ]; then
    if bash "$SMOKE_SCRIPT"; then
        echo "  PASS [fork-receiver-smoke]"
    else
        echo "  FAIL [fork-receiver-smoke]" >&2
        echo "  hint: remove cross-layer import from the named L1 file" >&2
        exit 1
    fi
else
    echo "  SKIP [fork-receiver-smoke] verify/fork-receiver-smoke.sh not yet present (pre-SP5.5)"
fi

# Step 5: export completeness (32 blessed components)
echo ""
echo "[5] L1 export completeness check"
if bash "$SCRIPT_DIR/check-exports.sh"; then
    echo "  PASS [check-exports]"
else
    echo "  FAIL [check-exports]" >&2
    echo "  hint: ensure all 32 blessed shadcn components are exported from templates/L1/index.ts" >&2
    exit 1
fi

echo ""
echo "=== ax-verify-L1: all required steps PASS ==="
exit 0
