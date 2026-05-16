#!/usr/bin/env bash
# practices/evals/portability/run.sh — portability axis runner.
# Iterates every registered fixture under practices/evals/fixtures/ and reports per-fixture
# readiness (smoke) and optional build status (--full). Build red ≠ exit 1 (advisory only).
set -uo pipefail

cd "$(dirname "$0")/../../.."

FIXTURES_DIR="practices/evals/fixtures"
FULL=false
[[ "${1:-}" == "--full" ]] && FULL=true

# JDK 21 auto-detect for fixtures that require it (e.g. spring-modulith-example).
# macOS: /usr/libexec/java_home -v 21. Linux/CI: rely on JAVA_HOME already pointing at 21.
if [[ -z "${JAVA_HOME:-}" ]] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q '"21'; then
    if command -v /usr/libexec/java_home >/dev/null; then
        if detected="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
            export JAVA_HOME="$detected"
            echo "[portability] JAVA_HOME → $JAVA_HOME (jdk 21 auto-detected)"
        fi
    fi
fi

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
