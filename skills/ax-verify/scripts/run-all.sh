#!/usr/bin/env bash
# skills/ax-verify/scripts/run-all.sh — ax-verify Tier-1 orchestrator.
#
# Chains all verification steps in order:
#   1. 6 guards via practices/evals/run-all-guards.sh
#   2. Backend Gradle tests
#   3. Frontend Vitest unit tests
#   4. Playwright E2E
#
# Each step is binary: exit non-zero halts the chain and prints which
# Tier-2 skill to invoke for targeted fix-loop.
#
# Exit 0 iff all steps pass.
#
# Usage:
#   bash skills/ax-verify/scripts/run-all.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

PASS=0
FAIL=0

step_pass() {
    PASS=$((PASS + 1))
    echo "  PASS [$1]"
}

step_fail() {
    local label="$1"
    local tier2_hint="$2"
    FAIL=$((FAIL + 1))
    echo "  FAIL [$label]" >&2
    echo "  hint: invoke /$tier2_hint for targeted fix-loop" >&2
}

echo "=== ax-verify: run-all.sh ==="
echo ""

# ── Step 1: 6 guards ─────────────────────────────────────────────────────────
echo "[1] guards — bash practices/evals/run-all-guards.sh"
if bash "$REPO_ROOT/practices/evals/run-all-guards.sh"; then
    step_pass "guards"
else
    step_fail "guards" "ax-guard-evidence / ax-guard-spec-ref / ax-guard-substance / ax-guard-time-decay / ax-guard-trio-integrity / ax-guard-cross-trio"
    echo ""
    echo "run-all: FAIL at step 1 (guards)" >&2
    exit 1
fi

# ── Step 2: Backend Gradle tests ─────────────────────────────────────────────
echo ""
echo "[2] backend — ./gradlew test"
if bash "$SCRIPT_DIR/run-backend.sh"; then
    step_pass "backend"
else
    step_fail "backend" "ax-verify-java"
    echo ""
    echo "run-all: FAIL at step 2 (backend)" >&2
    exit 1
fi

# ── Step 3: Frontend unit tests ───────────────────────────────────────────────
echo ""
echo "[3] frontend-unit — npm run test -- --run"
if bash "$SCRIPT_DIR/run-frontend-unit.sh"; then
    step_pass "frontend-unit"
else
    step_fail "frontend-unit" "ax-verify-react"
    echo ""
    echo "run-all: FAIL at step 3 (frontend-unit)" >&2
    exit 1
fi

# ── Step 4: Playwright E2E ────────────────────────────────────────────────────
echo ""
echo "[4] e2e — npx playwright test"
if bash "$SCRIPT_DIR/run-e2e.sh"; then
    step_pass "e2e"
else
    step_fail "e2e" "ax-verify-react"
    echo ""
    echo "run-all: FAIL at step 4 (e2e)" >&2
    exit 1
fi

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
echo "run-all: all steps PASS"
exit 0
