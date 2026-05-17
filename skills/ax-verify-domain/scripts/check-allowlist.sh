#!/usr/bin/env bash
# skills/ax-verify-domain/scripts/check-allowlist.sh
# Checks if <domain> is listed in practices/evals/trio_integrity_allowlist.yaml.
# Usage: bash check-allowlist.sh <domain>
# Exit 0 = domain is in allowlist; exit 1 = DOMAIN_NOT_IN_ALLOWLIST.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
    echo "USAGE: bash check-allowlist.sh <domain>" >&2
    exit 1
fi

ALLOWLIST="$REPO_ROOT/practices/evals/trio_integrity_allowlist.yaml"

if [ ! -f "$ALLOWLIST" ]; then
    echo "MISSING_ALLOWLIST: $ALLOWLIST" >&2
    exit 1
fi

if grep -q "domain: $DOMAIN" "$ALLOWLIST" || grep -q "  $DOMAIN:" "$ALLOWLIST" || grep -q "^$DOMAIN:" "$ALLOWLIST"; then
    exit 0
fi

echo "DOMAIN_NOT_IN_ALLOWLIST: $DOMAIN" >&2
echo "  Add '$DOMAIN' to $ALLOWLIST or run /ax-scaffold $DOMAIN" >&2
exit 1
