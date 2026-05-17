#!/usr/bin/env bash
# skills/ax-guard-cross-trio/scripts/run.sh
# Tier-3 cross_trio guard — thin wrapper around practices/evals/cross_trio_guard.sh.
# Usage: bash run.sh [--domain <domain>]
# Exits with the same code as cross_trio_guard.sh.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

exec bash "$REPO_ROOT/practices/evals/cross_trio_guard.sh" "$@"
