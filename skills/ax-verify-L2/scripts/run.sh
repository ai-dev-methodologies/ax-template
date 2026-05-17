#!/usr/bin/env bash
# skills/ax-verify-L2/scripts/run.sh
# Tier-2 L2 layer verifier orchestrator.
# Steps 1-3 are binary; step 4 (cross_trio_guard) is conditional.
# Exit 0 iff steps 1-3 all pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-L2: run.sh ==="
echo ""

L2_DIR="$REPO_ROOT/templates/L2"

if [ ! -d "$L2_DIR" ]; then
    echo "  SKIP [all] templates/L2/ not yet present — pre-SP7 state"
    echo ""
    echo "=== ax-verify-L2: SKIP (no L2 content yet) ==="
    exit 0
fi

# Step 1: evidence_guard scoped to templates/L2/
echo "[1] evidence_guard (templates/L2/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$L2_DIR"; then
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 2: import-layer check (no L3/L4 imports in L2 files)
echo ""
echo "[2] import-layer check (L2 must not import L3/L4)"
if bash "$SCRIPT_DIR/check-imports.sh"; then
    echo "  PASS [import-layer]"
else
    echo "  FAIL [import-layer]" >&2
    echo "  hint: remove or reroute the ILLEGAL_IMPORT to an L1 abstraction" >&2
    exit 1
fi

# Step 3: Vitest component tests for L2 blocks
echo ""
echo "[3] Vitest L2 component tests"
L2_TEST_DIR="$REPO_ROOT/frontend/tests/L2"
if [ -d "$L2_TEST_DIR" ]; then
    cd "$REPO_ROOT/frontend" && npx vitest run tests/L2/
    echo "  PASS [Vitest-L2]"
else
    echo "  SKIP [Vitest-L2] frontend/tests/L2/ not yet present"
fi

# Step 4: cross_trio_guard (conditional — only if L4 has actual .tsx domain files)
echo ""
echo "[4] cross_trio_guard (conditional)"
L4_DIR="$REPO_ROOT/templates/L4"
if [ -d "$L4_DIR" ] && find "$L4_DIR" -name "*.tsx" -type f | grep -q .; then
    if bash "$REPO_ROOT/practices/evals/cross_trio_guard.sh"; then
        echo "  PASS [cross_trio_guard]"
    else
        echo "  FAIL [cross_trio_guard]" >&2
        echo "  hint: invoke /ax-guard-cross-trio; add evidence: to named L1/L2 file" >&2
        exit 1
    fi
else
    echo "  SKIP [cross_trio_guard] templates/L4/ has no domain content yet"
fi

echo ""
echo "=== ax-verify-L2: all required steps PASS ==="
exit 0
