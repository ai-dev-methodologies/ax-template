#!/usr/bin/env bash
# skills/ax-verify-L2/scripts/check-imports.sh
# Static import parser for templates/L2/.
# Enforces: L2 files must NOT import from templates/L3/ or templates/L4/.
# Exit 0 = no illegal imports; exit 1 = first ILLEGAL_IMPORT on stderr.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

L2_DIR="$REPO_ROOT/templates/L2"

if [ ! -d "$L2_DIR" ]; then
    echo "  SKIP templates/L2/ not present"
    exit 0
fi

FAIL=0

# Find all .tsx and .ts files under L2
while IFS= read -r file; do
    # grep for import statements that reference L3 or L4
    while IFS= read -r import_line; do
        if [[ "$import_line" =~ templates/L[34]/ ]]; then
            echo "ILLEGAL_IMPORT: $file imports ${import_line}" >&2
            FAIL=1
            break
        fi
    done < <(grep -E "^import .* from ['\"]" "$file" 2>/dev/null || true)
done < <(find "$L2_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \))

if [ "$FAIL" -ne 0 ]; then
    exit 1
fi

exit 0
