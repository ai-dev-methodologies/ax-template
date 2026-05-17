#!/usr/bin/env bash
# skills/ax-verify-java/scripts/run-gradle.sh
# Wraps ./gradlew with a task argument.
# Usage: bash run-gradle.sh <task>
# Example: bash run-gradle.sh testPractices
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

TASK="${1:-test}"

echo "  Running: cd backend && ./gradlew $TASK"
cd "$REPO_ROOT/backend" && ./gradlew "$TASK"
