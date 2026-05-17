#!/usr/bin/env bash
# skills/ax-verify-L3/scripts/check-imports.sh
# Static import parser for templates/L3/.
# Enforces: L3 files must NOT import from templates/L4/.
# Exit 0 = no illegal imports; exit 1 = first ILLEGAL_IMPORT on stderr.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

L3_DIR="$REPO_ROOT/templates/L3"

if [ ! -d "$L3_DIR" ]; then
    echo "  SKIP templates/L3/ not present"
    exit 0
fi

FAIL=0

while IFS= read -r file; do
    while IFS= read -r import_line; do
        if [[ "$import_line" =~ templates/L4/ ]]; then
            echo "ILLEGAL_IMPORT: $file imports ${import_line}" >&2
            FAIL=1
            break
        fi
    done < <(grep -E "^import .* from ['\"]" "$file" 2>/dev/null || true)
done < <(find "$L3_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \))

if [ "$FAIL" -ne 0 ]; then
    exit 1
fi

exit 0
