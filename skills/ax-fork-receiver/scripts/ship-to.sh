#!/usr/bin/env bash
# skills/ax-fork-receiver/scripts/ship-to.sh
# SP22: Extract ax-template catalog tarball to a target directory.
#
# Usage:
#   bash skills/ax-fork-receiver/scripts/ship-to.sh <tarball> <target-dir> [--force]
#
# --force: overwrite target-dir if non-empty (default: refuse if non-empty)
#
# Exit 0 on success, 1 on failure.

set -uo pipefail

# ── Args ──────────────────────────────────────────────────────────────────────
if [ $# -lt 2 ]; then
    echo "Usage: ship-to.sh <tarball> <target-dir> [--force]" >&2
    exit 1
fi

TARBALL="$1"
TARGET_DIR="$2"
FORCE=0

shift 2
while [ $# -gt 0 ]; do
    case "$1" in
        --force) FORCE=1; shift ;;
        *) echo "ship-to: unknown arg: $1" >&2; exit 1 ;;
    esac
done

echo "[ship-to] tarball: $TARBALL"
echo "[ship-to] target:  $TARGET_DIR"

# ── Validate tarball ──────────────────────────────────────────────────────────
if [ ! -f "$TARBALL" ]; then
    echo "[ship-to] ERROR: tarball not found: $TARBALL" >&2
    exit 1
fi

# ── Validate / create target dir ─────────────────────────────────────────────
if [ -d "$TARGET_DIR" ]; then
    # Check if non-empty
    if [ "$(ls -A "$TARGET_DIR" 2>/dev/null | wc -l | tr -d ' ')" -gt 0 ]; then
        if [ "$FORCE" -eq 0 ]; then
            echo "[ship-to] ERROR: target dir '$TARGET_DIR' is non-empty." >&2
            echo "[ship-to] Use --force to overwrite." >&2
            exit 1
        else
            echo "[ship-to] --force: overwriting non-empty target dir"
        fi
    fi
else
    mkdir -p "$TARGET_DIR"
fi

# ── Extract tarball ───────────────────────────────────────────────────────────
echo "[ship-to] extracting..."
if ! tar -xzf "$TARBALL" -C "$TARGET_DIR"; then
    echo "[ship-to] ERROR: extraction failed" >&2
    exit 1
fi

# ── Count extracted files ─────────────────────────────────────────────────────
FILE_COUNT=$(find "$TARGET_DIR" -type f | wc -l | tr -d ' ')
echo "[ship-to] extracted $FILE_COUNT files → $TARGET_DIR"

# ── Print receiver setup instructions ────────────────────────────────────────
echo ""
echo "============================================================"
echo " Fork receiver setup"
echo "============================================================"
echo ""
echo " 1. Enter the target directory:"
echo "    cd $TARGET_DIR"
echo ""
echo " 2. Install pnpm (if not already installed):"
echo "    npm install -g pnpm"
echo ""
echo " 3. Install frontend peer deps (see templates/L1/PEER_DEPS.md):"
echo "    cd <your-next-app>"
echo "    pnpm add \$(cat $TARGET_DIR/templates/L1/PEER_DEPS.md | grep '^  \"' | grep -v '^  \"dev' | awk -F'\"' '{print \$2}' | paste -sd' ')"
echo ""
echo " 4. Verify L1 portability:"
echo "    bash $TARGET_DIR/verify/fork-receiver-smoke.sh"
echo ""
echo " 5. Run catalog guards at target:"
echo "    cd $TARGET_DIR && bash practices/evals/run-all-guards.sh --include-fixtures"
echo ""
echo "============================================================"
echo "[ship-to] DONE"
exit 0
