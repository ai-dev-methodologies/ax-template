#!/usr/bin/env bash
# skills/ax-verify-domain/scripts/run-playwright.sh
# Scoped Playwright runner for a single domain.
# Usage: bash run-playwright.sh <domain>
# Exit 0 = tests pass or no tests yet; exit 1 = test failure.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
    echo "USAGE: bash run-playwright.sh <domain>" >&2
    exit 1
fi

TEST_DIR="$REPO_ROOT/frontend/tests/L4/$DOMAIN"

if [ ! -d "$TEST_DIR" ]; then
    echo "  SKIP tests/L4/$DOMAIN/ not yet present (pre-SP8)"
    exit 0
fi

cd "$REPO_ROOT/frontend"
echo "  Running: npx playwright test tests/L4/$DOMAIN/"
npx playwright test "tests/L4/$DOMAIN/"
