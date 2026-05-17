#!/usr/bin/env bash
# skills/ax-guard-substance/scripts/run.sh
# Tier-3 substance guard — thin wrapper around practices/evals/substance_guard.sh.
# Usage: bash run.sh [<scope-path>]
# Exits with the same code as substance_guard.sh.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

SCOPE="${1:-}"

if [ -n "$SCOPE" ]; then
    exec bash "$REPO_ROOT/practices/evals/substance_guard.sh" "$SCOPE"
else
    exec bash "$REPO_ROOT/practices/evals/substance_guard.sh"
fi
