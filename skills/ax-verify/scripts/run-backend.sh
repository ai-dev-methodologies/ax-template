#!/usr/bin/env bash
# skills/ax-verify/scripts/run-backend.sh — Gradle test wrapper.
#
# Runs all registered test{Domain} tasks via `./gradlew test`.
# Exit 0 = all domain tests pass.
# Exit non-zero = Gradle native exit (test failure or build error).
#
# Usage:
#   bash skills/ax-verify/scripts/run-backend.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"

if [ ! -d "$BACKEND_DIR" ]; then
    echo "run-backend: ERROR — backend/ directory not found at $BACKEND_DIR" >&2
    exit 1
fi

if [ ! -f "$BACKEND_DIR/gradlew" ]; then
    echo "run-backend: ERROR — gradlew not found in $BACKEND_DIR" >&2
    exit 1
fi

echo "run-backend: running ./gradlew test in $BACKEND_DIR"
cd "$BACKEND_DIR" && ./gradlew test
