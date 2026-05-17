#!/usr/bin/env bash
# skills/ax-guard-trio-integrity/scripts/run.sh
# Tier-3 trio_integrity guard — thin wrapper around practices/evals/trio_integrity_guard.sh.
# Usage: bash run.sh [--domain <domain>]
# Exits with the same code as trio_integrity_guard.sh.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

exec bash "$REPO_ROOT/practices/evals/trio_integrity_guard.sh" "$@"
