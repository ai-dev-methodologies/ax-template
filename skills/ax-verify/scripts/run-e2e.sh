#!/usr/bin/env bash
# skills/ax-verify/scripts/run-e2e.sh — Playwright E2E runner.
#
# Runs `npx playwright test` in the frontend directory.
# Exit 0 = all E2E specs pass.
#
# Usage:
#   bash skills/ax-verify/scripts/run-e2e.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"

if [ ! -d "$FRONTEND_DIR" ]; then
    echo "run-e2e: ERROR — frontend/ directory not found at $FRONTEND_DIR" >&2
    exit 1
fi

if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    echo "run-e2e: ERROR — package.json not found in $FRONTEND_DIR" >&2
    exit 1
fi

echo "run-e2e: running npx playwright test in $FRONTEND_DIR"
cd "$FRONTEND_DIR" && npx playwright test
