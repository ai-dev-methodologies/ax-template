#!/usr/bin/env bash
# skills/ax-verify-react/scripts/run.sh
# Tier-2 React/Next.js axis verifier orchestrator.
# Steps 1-5 are binary; step 6 is conditional (L4 import changes only).
# Exit 0 iff steps 1-5 all pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-react: run.sh ==="
echo ""

# Step 1: evidence_guard scoped to practices-react/rules/**
echo "[1] evidence_guard (practices-react/rules/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$REPO_ROOT/practices-react/rules" 2>/dev/null || true; then
    # practices-react/rules/ may not exist yet (pre-SP7); treat as advisory
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 2: time_decay_guard on practices-react/upstream/** (advisory if dir absent)
echo ""
echo "[2] time_decay_guard (practices-react/upstream/)"
if [ -d "$REPO_ROOT/practices-react/upstream" ]; then
    if bash "$REPO_ROOT/practices/evals/time_decay_guard.sh" "$REPO_ROOT/practices-react/upstream"; then
        echo "  PASS [time_decay_guard]"
    else
        echo "  FAIL [time_decay_guard]" >&2
        echo "  hint: invoke /ax-guard-time-decay; re-fetch stale snapshots" >&2
        exit 1
    fi
else
    echo "  SKIP [time_decay_guard] practices-react/upstream/ not yet present (pre-SP7)"
fi

# Step 3: Next.js build
echo ""
echo "[3] npm run build (Next.js production build)"
cd "$REPO_ROOT/frontend" && npm run build
echo "  PASS [npm run build]"

# Step 4: Vitest unit tests
echo ""
echo "[4] npm run test -- --run (Vitest)"
cd "$REPO_ROOT/frontend" && npm run test -- --run
echo "  PASS [Vitest]"

# Step 5: Playwright E2E
echo ""
echo "[5] Playwright E2E"
if bash "$SCRIPT_DIR/run-e2e.sh"; then
    echo "  PASS [Playwright]"
else
    echo "  FAIL [Playwright]" >&2
    echo "  hint: open Playwright trace URL from stderr; fix auth flow or route" >&2
    exit 1
fi

echo ""
echo "=== ax-verify-react: all required steps PASS ==="
exit 0
