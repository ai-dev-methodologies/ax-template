#!/usr/bin/env bash
# practices/consumer-proof/engine/coverage_map_guard.sh
#
# Part 1.5 of the gap-convergence engine design — MECE/schema/disk-truth guard
# for coverage-map.yaml. See lib/coverage_map_guard.py for the five checks.
#
# STANDALONE in wave 1: this guard is intentionally NOT registered into
# practices/evals/run-all-guards.sh (the engine stays isolated per the
# design's Part 3.7). Run it directly, or via --fixtures to prove it
# genuinely blocks (each fail_* fixture must exit 1; pass_clean must exit 0).
#
# Usage:
#   bash practices/consumer-proof/engine/coverage_map_guard.sh
#       # runs against the real coverage-map.yaml — exit 0 expected
#   bash practices/consumer-proof/engine/coverage_map_guard.sh --map PATH
#   bash practices/consumer-proof/engine/coverage_map_guard.sh --fixtures
#       # additionally runs all fixtures/coverage_map_guard/{fail_*,pass_clean}.yaml
#       # and asserts fail_*.yaml -> exit 1, pass_clean.yaml -> exit 0
#
# Exit codes: 0 = PASS · 1 = FAIL (findings printed) · 2 = usage/toolchain error.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
MAP_PATH="$SCRIPT_DIR/coverage-map.yaml"
RUN_FIXTURES=0

while [ $# -gt 0 ]; do
    case "$1" in
        --map) MAP_PATH="$2"; shift 2 ;;
        --map=*) MAP_PATH="${1#--map=}"; shift ;;
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        --fixtures) RUN_FIXTURES=1; shift ;;
        *) echo "coverage_map_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

PY="python3"
if ! "$PY" -c "import yaml" >/dev/null 2>&1; then
    if [ -x "$HOME/.pyshim/python3" ]; then
        PY="$HOME/.pyshim/python3"
    elif [ -x "/usr/bin/python3" ] && /usr/bin/python3 -c "import yaml" >/dev/null 2>&1; then
        PY="/usr/bin/python3"
    fi
fi
if ! "$PY" -c "import yaml" >/dev/null 2>&1; then
    echo "coverage_map_guard: no python3 with PyYAML found on PATH." >&2
    echo "  Try: export PATH=\"\$HOME/.pyshim:\$PATH\"" >&2
    exit 2
fi

overall=0

echo "=== coverage_map_guard: real map ==="
"$PY" "$SCRIPT_DIR/lib/coverage_map_guard.py" --map "$MAP_PATH" --repo-root "$REPO_ROOT"
real_exit=$?
if [ $real_exit -ne 0 ]; then
    overall=1
fi

if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/coverage_map_guard"
    echo ""
    echo "=== coverage_map_guard: fixture proof (fail_* -> 1, pass_clean -> 0) ==="
    fixture_fail=0
    for f in "$FIXTURES_DIR"/fail_*.yaml; do
        [ -f "$f" ] || continue
        name="$(basename "$f")"
        "$PY" "$SCRIPT_DIR/lib/coverage_map_guard.py" --map "$f" --repo-root "$REPO_ROOT" >/tmp/cmg_fixture_out.$$ 2>&1
        code=$?
        if [ $code -eq 1 ]; then
            echo "  PASS (correctly blocked): $name -> exit 1"
        else
            echo "  FAIL (fixture did not block as expected): $name -> exit $code"
            cat /tmp/cmg_fixture_out.$$
            fixture_fail=1
        fi
        rm -f /tmp/cmg_fixture_out.$$
    done
    for f in "$FIXTURES_DIR"/pass_clean.yaml; do
        [ -f "$f" ] || continue
        name="$(basename "$f")"
        "$PY" "$SCRIPT_DIR/lib/coverage_map_guard.py" --map "$f" --repo-root "$REPO_ROOT" >/tmp/cmg_fixture_out.$$ 2>&1
        code=$?
        if [ $code -eq 0 ]; then
            echo "  PASS (correctly clean): $name -> exit 0"
        else
            echo "  FAIL (pass_clean fixture did not pass): $name -> exit $code"
            cat /tmp/cmg_fixture_out.$$
            fixture_fail=1
        fi
        rm -f /tmp/cmg_fixture_out.$$
    done
    if [ $fixture_fail -ne 0 ]; then
        overall=1
    fi
fi

exit $overall
