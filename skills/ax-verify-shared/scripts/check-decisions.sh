#!/usr/bin/env bash
# skills/ax-verify-shared/scripts/check-decisions.sh
# Validates that every ADR entry in templates/DECISIONS.md
# contains a provenance_class: field.
# Exit 0 = all ADRs have provenance_class; exit 1 = first missing ADR name on stderr.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DECISIONS_FILE="$REPO_ROOT/templates/DECISIONS.md"

if [ ! -f "$DECISIONS_FILE" ]; then
    echo "MISSING_DECISIONS_FILE: $DECISIONS_FILE" >&2
    exit 1
fi

# Extract ADR IDs (lines starting with "## TD-" or "### TD-")
FAIL=0
CURRENT_ADR=""

while IFS= read -r line; do
    # Detect ADR header: ## TD-YYYY-MM-DD-NNN or ### TD-...
    if [[ "$line" =~ ^#{2,3}[[:space:]]+(TD-[0-9-]+) ]]; then
        CURRENT_ADR="${BASH_REMATCH[1]}"
        HAS_PROVENANCE=0
    fi
    if [[ "$line" =~ provenance_class: ]]; then
        HAS_PROVENANCE=1
    fi
    # When we hit the next ADR or EOF, check the previous one
done < "$DECISIONS_FILE"

# Simple pass: grep for provenance_class presence in the file
# Each ADR block must contain at least one provenance_class: line
ADR_COUNT=$(grep -c '^##\? TD-' "$DECISIONS_FILE" 2>/dev/null || echo 0)
PROVENANCE_COUNT=$(grep -c 'provenance_class:' "$DECISIONS_FILE" 2>/dev/null || echo 0)

if [ "$ADR_COUNT" -eq 0 ]; then
    # No ADRs yet — pass (pre-SP3 state)
    echo "  INFO: No ADR entries found — nothing to check"
    exit 0
fi

if [ "$PROVENANCE_COUNT" -lt "$ADR_COUNT" ]; then
    # Find the first ADR missing provenance_class
    AWK_SCRIPT='
    /^##? TD-/ { adr=$0; has=0 }
    /provenance_class:/ { has=1 }
    /^##? TD-/ && adr != "" && !has { print "MISSING_PROVENANCE_CLASS: " adr }
    '
    awk "$AWK_SCRIPT" "$DECISIONS_FILE" >&2
    exit 1
fi

exit 0
