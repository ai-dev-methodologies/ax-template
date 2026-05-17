#!/usr/bin/env bash
# skills/ax-guard-spec-ref/scripts/run.sh
# Tier-3 spec_ref guard — thin wrapper around practices/evals/spec_ref_guard.sh.
# Usage: bash run.sh [<scope-path>]
# Exits with the same code as spec_ref_guard.sh.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

SCOPE="${1:-}"

if [ -n "$SCOPE" ]; then
    exec bash "$REPO_ROOT/practices/evals/spec_ref_guard.sh" "$SCOPE"
else
    exec bash "$REPO_ROOT/practices/evals/spec_ref_guard.sh"
fi
