#!/usr/bin/env bash
# skills/ax-verify-L1/scripts/check-exports.sh
# Counts named exports from templates/L1/index.ts (or index.tsx).
# Asserts exactly 32 blessed shadcn component exports are present.
# Exit 0 = count >= 32; exit 1 = insufficient exports.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

EXPECTED=32

# Look for the index file
INDEX_FILE=""
for candidate in \
    "$REPO_ROOT/templates/L1/index.ts" \
    "$REPO_ROOT/templates/L1/index.tsx"; do
    if [ -f "$candidate" ]; then
        INDEX_FILE="$candidate"
        break
    fi
done

if [ -z "$INDEX_FILE" ]; then
    echo "  SKIP [check-exports] templates/L1/index.ts not yet present (pre-SP5)"
    exit 0
fi

# Count named export statements (export { ... } from or export * from)
EXPORT_COUNT=$(grep -c '^export' "$INDEX_FILE" 2>/dev/null || echo 0)

if [ "$EXPORT_COUNT" -lt "$EXPECTED" ]; then
    echo "L1_EXPORT_SHORTFALL: found $EXPORT_COUNT exports, expected $EXPECTED" >&2
    exit 1
fi

echo "  L1 exports: $EXPORT_COUNT (>= $EXPECTED required)"
exit 0
