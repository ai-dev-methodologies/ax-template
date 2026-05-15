#!/usr/bin/env bash
# practices/evals/portability/run.sh — portability axis runner.
# Iterates every registered fixture under practices/evals/fixtures/ and reports per-fixture
# readiness (smoke) and optional build status (--full). Build red ≠ exit 1 (advisory only).
set -uo pipefail

cd "$(dirname "$0")/../../.."

FIXTURES_DIR="practices/evals/fixtures"
FULL=false
[[ "${1:-}" == "--full" ]] && FULL=true

echo "## Portability"
echo ""
echo "| Fixture | Build tool | Mode | Result |"
echo "|---------|------------|------|--------|"

found_any=false
for fx in "$FIXTURES_DIR"/*; do
    [[ -d "$fx" ]] || continue
    [[ "$(basename "$fx")" == ".gitkeep" ]] && continue
    found_any=true
    name="$(basename "$fx")"

    # Detect build tool
    if [[ -x "$fx/mvnw" ]]; then
        tool="maven (./mvnw)"
        build_cmd="./mvnw -q -DskipTests package"
    elif [[ -x "$fx/gradlew" ]]; then
        tool="gradle (./gradlew)"
        build_cmd="./gradlew -q -x test assemble"
    else
        echo "| $name | N/A | — | SKIP (no mvnw/gradlew) |"
        continue
    fi

    if ! $FULL; then
        echo "| $name | $tool | smoke | PASS-prep (use --full to actually build) |"
        continue
    fi

    if (cd "$fx" && eval "$build_cmd"); then
        echo "| $name | $tool | --full | GREEN |"
    else
        echo "| $name | $tool | --full | RED (advisory only) |"
    fi
done

if ! $found_any; then
    echo "| (none) | — | — | N/A — no fixtures registered |"
fi

exit 0
