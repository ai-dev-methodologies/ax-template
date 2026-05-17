#!/usr/bin/env bash
# skills/ax-verify-L4/scripts/run-playwright-L4.sh
# Scoped Playwright runner for tests/L4/.
# Usage: bash run-playwright-L4.sh [<domain>]
# Without arg: runs all tests under frontend/tests/L4/
# With domain arg: runs only frontend/tests/L4/<domain>/
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN="${1:-}"

cd "$REPO_ROOT/frontend"

L4_TEST_DIR="tests/L4"
if [ ! -d "$L4_TEST_DIR" ]; then
    echo "  SKIP tests/L4/ not yet present (pre-SP8)"
    exit 0
fi

if [ -n "$DOMAIN" ]; then
    echo "  Running: npx playwright test $L4_TEST_DIR/$DOMAIN/"
    npx playwright test "$L4_TEST_DIR/$DOMAIN/"
else
    echo "  Running: npx playwright test $L4_TEST_DIR/"
    npx playwright test "$L4_TEST_DIR/"
fi
