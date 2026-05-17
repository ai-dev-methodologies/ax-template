#!/usr/bin/env bash
# skills/ax-verify-react/scripts/run-e2e.sh
# Playwright E2E runner.
# Usage: bash run-e2e.sh [<test-path>]
# Default: runs full test suite under frontend/tests/
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

TEST_PATH="${1:-}"

cd "$REPO_ROOT/frontend"

if [ -n "$TEST_PATH" ]; then
    echo "  Running: npx playwright test $TEST_PATH"
    npx playwright test "$TEST_PATH"
else
    echo "  Running: npx playwright test"
    npx playwright test
fi
