#!/usr/bin/env bash
# skills/ax-verify-shared/scripts/regen-agents.sh
# Thin wrapper around templates/generate_agents.sh.
# Regenerates AGENTS.md and checks if the sha256 changed.
# Exit 0 = sha unchanged (catalog in sync with AGENTS.md).
# Exit 1 = sha changed (AGENTS.md is stale; recommit the regenerated version).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

GENERATE_SCRIPT="$REPO_ROOT/templates/generate_agents.sh"
AGENTS_FILE="$REPO_ROOT/practices/AGENTS.md"

if [ ! -f "$GENERATE_SCRIPT" ]; then
    echo "  SKIP [regen-agents] templates/generate_agents.sh not found — pre-SP generation"
    exit 0
fi

if [ ! -f "$AGENTS_FILE" ]; then
    echo "  SKIP [regen-agents] practices/AGENTS.md not found — pre-SP sentinel"
    exit 0
fi

# Capture sha before
SHA_BEFORE=$(sha256sum "$AGENTS_FILE" 2>/dev/null | awk '{print $1}' || shasum -a 256 "$AGENTS_FILE" | awk '{print $1}')

# Regenerate
bash "$GENERATE_SCRIPT" >/dev/null 2>&1

# Capture sha after
SHA_AFTER=$(sha256sum "$AGENTS_FILE" 2>/dev/null | awk '{print $1}' || shasum -a 256 "$AGENTS_FILE" | awk '{print $1}')

if [ "$SHA_BEFORE" != "$SHA_AFTER" ]; then
    echo "AGENTS_MD_STALE: sha changed after regeneration" >&2
    echo "  Before: $SHA_BEFORE" >&2
    echo "  After:  $SHA_AFTER" >&2
    echo "  Commit the regenerated practices/AGENTS.md" >&2
    exit 1
fi

exit 0
