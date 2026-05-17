#!/usr/bin/env bash
# skills/ax-verify/scripts/run-frontend-unit.sh — Vitest unit test runner (non-watch).
#
# Runs `npm run test -- --run` so Vitest exits after a single pass instead of
# watching for file changes. Exit 0 = all unit tests pass.
#
# Usage:
#   bash skills/ax-verify/scripts/run-frontend-unit.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/frontend"

if [ ! -d "$FRONTEND_DIR" ]; then
    echo "run-frontend-unit: ERROR — frontend/ directory not found at $FRONTEND_DIR" >&2
    exit 1
fi

if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    echo "run-frontend-unit: ERROR — package.json not found in $FRONTEND_DIR" >&2
    exit 1
fi

echo "run-frontend-unit: running npm run test -- --run in $FRONTEND_DIR"
cd "$FRONTEND_DIR" && npm run test -- --run
