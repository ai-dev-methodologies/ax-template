#!/usr/bin/env bash
# skills/ax-verify-L3/scripts/run.sh
# Tier-2 L3 layer verifier orchestrator.
# Steps 1-3 are binary; step 4 (Vitest) is conditional.
# Exit 0 iff steps 1-3 all pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-L3: run.sh ==="
echo ""

L3_DIR="$REPO_ROOT/templates/L3"

if [ ! -d "$L3_DIR" ]; then
    echo "  SKIP [all] templates/L3/ not yet present — pre-SP6 state"
    echo ""
    echo "=== ax-verify-L3: SKIP (no L3 content yet) ==="
    exit 0
fi

# Step 1: evidence_guard scoped to templates/L3/
echo "[1] evidence_guard (templates/L3/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$L3_DIR"; then
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 2: import-layer check (no L4 imports in L3 files)
echo ""
echo "[2] import-layer check (L3 must not import L4)"
if bash "$SCRIPT_DIR/check-imports.sh"; then
    echo "  PASS [import-layer]"
else
    echo "  FAIL [import-layer]" >&2
    echo "  hint: remove the ILLEGAL_IMPORT to templates/L4/ from the named L3 file" >&2
    exit 1
fi

# Step 3: slot-contract README presence
echo ""
echo "[3] slot-contract validation"
if bash "$SCRIPT_DIR/check-slot-contracts.sh"; then
    echo "  PASS [slot-contracts]"
else
    echo "  FAIL [slot-contracts]" >&2
    echo "  hint: add README.md with '## Slot contract' section to the named L3 family" >&2
    exit 1
fi

# Step 4: Vitest L3 tests (conditional)
echo ""
echo "[4] Vitest L3 tests (conditional)"
L3_TEST_DIR="$REPO_ROOT/frontend/tests/L3"
if [ -d "$L3_TEST_DIR" ]; then
    cd "$REPO_ROOT/frontend" && npx vitest run tests/L3/
    echo "  PASS [Vitest-L3]"
else
    echo "  SKIP [Vitest-L3] frontend/tests/L3/ not yet present"
fi

echo ""
echo "=== ax-verify-L3: all required steps PASS ==="
exit 0
