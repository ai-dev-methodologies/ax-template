#!/usr/bin/env bash
# skills/ax-fork-receiver/scripts/smoke.sh
# SP22: Run fork-receiver smoke tests at a target directory.
#
# Runs three checks at the target, in order:
#   1. verify/fork-receiver-smoke.sh         — L1 portability (path-leak + tsc)
#   2. verify/fork-receiver-full-tree-smoke.sh — L1+L2+L3+L4 tree check
#   3. practices/evals/run-all-guards.sh --include-fixtures — catalog guards
#
# Usage:
#   bash skills/ax-fork-receiver/scripts/smoke.sh <target-dir>
#
# Exit 0 iff all three checks pass.

set -uo pipefail

# ── Args ──────────────────────────────────────────────────────────────────────
if [ $# -lt 1 ]; then
    echo "Usage: smoke.sh <target-dir>" >&2
    exit 1
fi

TARGET_DIR="$1"

echo "[smoke] target: $TARGET_DIR"

# ── Validate target dir ───────────────────────────────────────────────────────
if [ ! -d "$TARGET_DIR" ]; then
    echo "[smoke] ERROR: target dir not found: $TARGET_DIR" >&2
    exit 1
fi

# Verify required scripts are present
for required in \
    "verify/fork-receiver-smoke.sh" \
    "verify/fork-receiver-full-tree-smoke.sh" \
    "practices/evals/run-all-guards.sh"
do
    if [ ! -f "$TARGET_DIR/$required" ]; then
        echo "[smoke] ERROR: required file missing: $TARGET_DIR/$required" >&2
        echo "[smoke] Is the tarball complete? Run bundle.sh first." >&2
        exit 1
    fi
done

PASS=0
FAIL=0

smoke_step() {
    local label="$1"
    shift
    echo ""
    echo "[smoke] --- $label ---"
    if "$@"; then
        PASS=$((PASS + 1))
        echo "[smoke] PASS: $label"
    else
        FAIL=$((FAIL + 1))
        echo "[smoke] FAIL: $label" >&2
    fi
}

# ── Step 1: L1 fork-receiver smoke ────────────────────────────────────────────
smoke_step "fork-receiver-smoke (L1)" \
    bash "$TARGET_DIR/verify/fork-receiver-smoke.sh"

# ── Step 2: Full-tree smoke (L1+L2+L3+L4) ────────────────────────────────────
smoke_step "fork-receiver-full-tree-smoke (L1+L2+L3+L4)" \
    bash "$TARGET_DIR/verify/fork-receiver-full-tree-smoke.sh"

# ── Step 3: Catalog guards (run from target as REPO_ROOT) ─────────────────────
# The guards resolve paths relative to the script's location.
# Running from $TARGET_DIR lets them find practices/rules/ etc.
smoke_step "practices/evals/run-all-guards.sh --include-fixtures" \
    bash -c "cd \"$TARGET_DIR\" && bash practices/evals/run-all-guards.sh --include-fixtures"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "=== Smoke summary: $PASS passed, $FAIL failed ==="

if [ "$FAIL" -gt 0 ]; then
    echo "[smoke] FAIL: $FAIL step(s) did not pass" >&2
    exit 1
fi

echo "[smoke] all smoke steps PASS — target is a valid fork receiver"
exit 0
