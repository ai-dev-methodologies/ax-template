#!/usr/bin/env bash
# skills/ax-guard-time-decay/scripts/run.sh
# Tier-3 time-decay guard — thin wrapper around practices/evals/time_decay_guard.sh.
# Usage: bash run.sh [<scope-path>]
# Exits with the same code as time_decay_guard.sh.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

SCOPE="${1:-}"

if [ -n "$SCOPE" ]; then
    exec bash "$REPO_ROOT/practices/evals/time_decay_guard.sh" "$SCOPE"
else
    exec bash "$REPO_ROOT/practices/evals/time_decay_guard.sh"
fi
