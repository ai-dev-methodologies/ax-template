#!/usr/bin/env bash
# skills/ax-verify-L1/scripts/check-exports.sh
# Counts named exports from templates/L1/index.ts (or index.tsx).
# Asserts >= 39 component exports are present (32 baseline + 7 SP14 P0 primitives).
# Exit 0 = count >= 39; exit 1 = insufficient exports.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

EXPECTED=39

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
