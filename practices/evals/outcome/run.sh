#!/usr/bin/env bash
# practices/evals/outcome/run.sh — advisory outcome metrics (SAST + build time).
# NEVER exits non-zero. All results are advisory; missing tools → N/A.
set -uo pipefail

cd "$(dirname "$0")/../../.."

echo "## Outcome (advisory)"
echo ""
echo "| Metric | Result |"
echo "|--------|--------|"

# Semgrep
if command -v semgrep >/dev/null 2>&1; then
    count="$(semgrep --quiet --config p/owasp-top-ten --json practices/ 2>/dev/null \
        | python3 -c 'import sys,json; print(len(json.load(sys.stdin).get("results",[])))' 2>/dev/null || echo "N/A")"
    echo "| Semgrep findings | ${count} |"
else
    echo "| Semgrep findings | N/A (not installed) |"
fi

# SpotBugs via gradle (only if plugin present)
if grep -q "spotbugs" backend/build.gradle.kts 2>/dev/null; then
    echo "| SpotBugs | (plugin present — run \`./gradlew spotbugsMain\`) |"
else
    echo "| SpotBugs | N/A (no plugin) |"
fi

# testPractices wall-clock
if [[ -x backend/gradlew ]]; then
    start="$(python3 -c 'import time; print(time.time())')"
    (cd backend && ./gradlew testPractices --quiet >/dev/null 2>&1) || true
    end="$(python3 -c 'import time; print(time.time())')"
    elapsed="$(python3 -c "print(f'{${end}-${start}:.2f}')")"
    echo "| testPractices wall-clock (s) | ${elapsed} |"
else
    echo "| testPractices wall-clock | N/A (no gradlew) |"
fi

echo ""
echo "_Advisory only — these metrics never block merges._"
exit 0
