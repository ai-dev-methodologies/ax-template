#!/usr/bin/env bash
# skills/ax-verify-L3/scripts/check-slot-contracts.sh
# For each of 7 L3 family directories, asserts a README.md exists
# and contains a "## Slots" or "## Slot contract" section.
# Exit 0 = all present; exit 1 = first missing family on stderr.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

L3_DIR="$REPO_ROOT/templates/L3"

if [ ! -d "$L3_DIR" ]; then
    echo "  SKIP templates/L3/ not present"
    exit 0
fi

FAIL=0

# Each direct subdirectory of L3 is a "family"
while IFS= read -r family_dir; do
    family_name="$(basename "$family_dir")"
    readme="$family_dir/README.md"

    if [ ! -f "$readme" ]; then
        echo "MISSING_SLOT_CONTRACT: $family_name (no README.md)" >&2
        FAIL=1
        continue
    fi

    # Check for slot section header
    if ! grep -qiE '^#{1,3}\s+(Slots|Slot contract)' "$readme"; then
        echo "MISSING_SLOT_CONTRACT: $family_name (README.md has no '## Slots' or '## Slot contract' section)" >&2
        FAIL=1
    fi
done < <(find "$L3_DIR" -maxdepth 1 -mindepth 1 -type d)

if [ "$FAIL" -ne 0 ]; then
    exit 1
fi

exit 0
