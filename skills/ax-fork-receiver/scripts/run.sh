#!/usr/bin/env bash
# skills/ax-fork-receiver/scripts/run.sh
# SP22: Master orchestrator for /ax-fork-receiver workflow.
#
# Usage:
#   bash skills/ax-fork-receiver/scripts/run.sh --bundle-only
#   bash skills/ax-fork-receiver/scripts/run.sh --target=<path>
#   bash skills/ax-fork-receiver/scripts/run.sh --target=<path> --force
#
# Modes:
#   --bundle-only : Skip source GREEN check. Bundle only, print tarball path.
#   --target=<p>  : Bundle + ship to <p> + run smoke at <p>.
#   --force       : Pass --force to ship-to.sh (overwrite non-empty target).
#
# Default (no args): same as --bundle-only.
#
# Exit 0 iff all active steps pass.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# ── Parse args ────────────────────────────────────────────────────────────────
BUNDLE_ONLY=0
TARGET_DIR=""
FORCE=0

for arg in "$@"; do
    case "$arg" in
        --bundle-only) BUNDLE_ONLY=1 ;;
        --target=*)    TARGET_DIR="${arg#--target=}" ;;
        --force)       FORCE=1 ;;
        *)
            echo "run.sh: unknown arg: $arg" >&2
            echo "Usage: run.sh [--bundle-only | --target=<path>] [--force]" >&2
            exit 1
        ;;
    esac
done

# If no args, default to bundle-only
if [ "$BUNDLE_ONLY" -eq 0 ] && [ -z "$TARGET_DIR" ]; then
    BUNDLE_ONLY=1
fi

echo "=== /ax-fork-receiver ==="
echo ""

PASS=0
FAIL=0

step_pass() {
    PASS=$((PASS + 1))
    echo "  PASS [$1]"
}

step_fail() {
    FAIL=$((FAIL + 1))
    echo "  FAIL [$1]: $2" >&2
}

# ── Step 1: Source GREEN check (skipped in --bundle-only mode) ────────────────
if [ "$BUNDLE_ONLY" -eq 0 ]; then
    echo "[1] source GREEN — bash skills/ax-verify/scripts/run-all.sh"
    if bash "$REPO_ROOT/skills/ax-verify/scripts/run-all.sh"; then
        step_pass "source-green"
    else
        step_fail "source-green" "ax-verify run-all.sh returned non-zero"
        echo ""
        echo "run.sh: FAIL at step 1 (source GREEN)" >&2
        exit 1
    fi
else
    echo "[1] source GREEN — SKIPPED (--bundle-only)"
fi

# ── Step 2: Bundle ────────────────────────────────────────────────────────────
echo ""
SHA=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown")
mkdir -p "$REPO_ROOT/dist"
TARBALL="$REPO_ROOT/dist/ax-template-catalog-${SHA}.tar.gz"

echo "[2] bundle — bash skills/ax-fork-receiver/scripts/bundle.sh"
if bash "$SCRIPT_DIR/bundle.sh" "$TARBALL"; then
    step_pass "bundle"
else
    step_fail "bundle" "bundle.sh returned non-zero"
    echo ""
    echo "run.sh: FAIL at step 2 (bundle)" >&2
    exit 1
fi

# Print tarball info
TARBALL_BYTES=$(wc -c < "$TARBALL" | tr -d ' ')
TARBALL_MB=$((TARBALL_BYTES / 1024 / 1024))
if command -v sha256sum >/dev/null 2>&1; then
    TARBALL_SHA=$(sha256sum "$TARBALL" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
    TARBALL_SHA=$(shasum -a 256 "$TARBALL" | awk '{print $1}')
else
    TARBALL_SHA="(unavailable)"
fi
echo "  tarball: $TARBALL (${TARBALL_MB}MB)"
echo "  sha256:  $TARBALL_SHA"

# ── Step 3: Ship to target (if --target given) ────────────────────────────────
if [ -n "$TARGET_DIR" ]; then
    echo ""
    echo "[3] ship — bash skills/ax-fork-receiver/scripts/ship-to.sh"
    SHIP_ARGS=("$TARBALL" "$TARGET_DIR")
    if [ "$FORCE" -eq 1 ]; then
        SHIP_ARGS+=("--force")
    fi
    if bash "$SCRIPT_DIR/ship-to.sh" "${SHIP_ARGS[@]}"; then
        step_pass "ship-to"
    else
        step_fail "ship-to" "ship-to.sh returned non-zero"
        echo ""
        echo "run.sh: FAIL at step 3 (ship-to)" >&2
        exit 1
    fi
else
    echo ""
    echo "[3] ship — SKIPPED (no --target given)"
fi

# ── Step 4: Smoke at target (if --target given) ───────────────────────────────
if [ -n "$TARGET_DIR" ]; then
    echo ""
    echo "[4] smoke — bash skills/ax-fork-receiver/scripts/smoke.sh"
    if bash "$SCRIPT_DIR/smoke.sh" "$TARGET_DIR"; then
        step_pass "smoke"
    else
        step_fail "smoke" "smoke.sh returned non-zero"
        echo ""
        echo "run.sh: FAIL at step 4 (smoke)" >&2
        exit 1
    fi
else
    echo "[4] smoke — SKIPPED (no --target given)"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ "$FAIL" -gt 0 ]; then
    echo "run.sh: FAIL" >&2
    exit 1
fi

echo ""
echo "Fork receiver install one-liner:"
echo "  bash $REPO_ROOT/skills/ax-fork-receiver/scripts/run.sh --target=<your-dir>"
echo ""
echo "run.sh: all steps PASS"
exit 0
