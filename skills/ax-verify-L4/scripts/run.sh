#!/usr/bin/env bash
# skills/ax-verify-L4/scripts/run.sh
# Tier-2 L4 layer verifier orchestrator.
# Steps 1-4 are binary.
# Exit 0 iff all steps pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-L4: run.sh ==="
echo ""

L4_DIR="$REPO_ROOT/templates/L4"

if [ ! -d "$L4_DIR" ] || [ -z "$(ls -A "$L4_DIR" 2>/dev/null)" ]; then
    echo "  SKIP [all] templates/L4/ has no domain content — pre-SP8 state"
    echo ""
    echo "=== ax-verify-L4: SKIP (no L4 content yet) ==="
    exit 0
fi

# Step 1: evidence_guard scoped to templates/L4/
echo "[1] evidence_guard (templates/L4/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$L4_DIR"; then
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 2: cross_trio_guard (full L4 scope)
echo ""
echo "[2] cross_trio_guard (all L4 domains)"
if bash "$REPO_ROOT/practices/evals/cross_trio_guard.sh"; then
    echo "  PASS [cross_trio_guard]"
else
    echo "  FAIL [cross_trio_guard]" >&2
    echo "  hint: add evidence: block to the named L1/L2/L3 file (not the L4 file)" >&2
    exit 1
fi

# Step 3: trio_integrity_guard
echo ""
echo "[3] trio_integrity_guard"
if bash "$REPO_ROOT/practices/evals/trio_integrity_guard.sh"; then
    echo "  PASS [trio_integrity_guard]"
else
    echo "  FAIL [trio_integrity_guard]" >&2
    echo "  hint: invoke /ax-guard-trio-integrity; see error code table for fix" >&2
    exit 1
fi

# Step 4: Playwright E2E for all L4 domains
echo ""
echo "[4] Playwright E2E (tests/L4/)"
if bash "$SCRIPT_DIR/run-playwright-L4.sh"; then
    echo "  PASS [Playwright-L4]"
else
    echo "  FAIL [Playwright-L4]" >&2
    echo "  hint: open Playwright trace; fix the named domain flow" >&2
    exit 1
fi

echo ""
echo "=== ax-verify-L4: all required steps PASS ==="
exit 0
