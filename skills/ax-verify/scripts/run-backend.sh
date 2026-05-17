#!/usr/bin/env bash
# skills/ax-verify/scripts/run-backend.sh — Gradle test wrapper.
#
# Runs registered non-advisory test{Domain} tasks:
#   testAsvs + testCrud + testPractices + testRateLimit + testPayment
#
# testPortability is EXCLUDED — it is advisory per CLAUDE.md (requires external
# fixtures built via practices/evals/portability/run.sh --full; skip it in CI).
#
# Exit 0 = all core domain tests pass.
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

# Run core domain tasks. testPortability is advisory and excluded.
TASKS=()
for task in testAsvs testCrud testPractices testRateLimit testPayment; do
    # Only include tasks that exist in the build file
    if grep -q "\"$task\"" "$BACKEND_DIR/build.gradle.kts" 2>/dev/null; then
        TASKS+=("$task")
    fi
done

if [ "${#TASKS[@]}" -eq 0 ]; then
    echo "run-backend: WARNING — no known test tasks found; falling back to ./gradlew test" >&2
    cd "$BACKEND_DIR" && ./gradlew test
    exit $?
fi

echo "run-backend: running ./gradlew ${TASKS[*]} in $BACKEND_DIR"
cd "$BACKEND_DIR" && ./gradlew "${TASKS[@]}"
