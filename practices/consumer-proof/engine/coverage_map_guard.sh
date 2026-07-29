#!/usr/bin/env bash
# practices/consumer-proof/engine/coverage_map_guard.sh
#
# Part 1.5 of the gap-convergence engine design — MECE/schema/disk-truth guard
# for coverage-map.yaml. See lib/coverage_map_guard.py for the eight checks.
#
# REGISTERED (BACKLOG P2-44, 2026-07-29): the wave-1 "engine stays isolated per
# the design's Part 3.7" posture was LIFTED. While isolated, every check here —
# including the S3 composition check (P2-29) and the disk-truth/nonvacuity checks
# (P3-58/P3-60) — was enforced ONLY when a human ran this file by hand, so an R25
# completion could be declared while coverage-map.yaml lied about its own coverage
# (reproduction from the row: point a covered cell's nonvacuity at a nonexistent
# path → verify-completion.sh still PASSED). It is now registered live + fixtures
# as [99] in practices/evals/run-all-guards.sh, which the R25 `hard-guards` step
# runs. Still runnable standalone, and --fixtures still proves it genuinely blocks
# (each fail_* fixture must exit 1; pass_clean must exit 0).
#
# TOOLCHAIN POSTURE: FAIL-CLOSED on a missing parser (exit 2 = "cannot verify",
# never a silent exit 0) — the convention the 16 PyYAML-dependent guards adopted in
# P2-46. practices/evals/pyyaml_preflight_coverage_guard.sh [95] re-derives the
# dependent set from disk, so this file and lib/coverage_map_guard.py are both
# picked up automatically and are probed under a simulated PyYAML absence.
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

# PyYAML preflight — FAIL-CLOSED (exit 2 = "cannot verify"), never a silent exit 0.
# The probe is a heredoc so `import yaml` stands on its own line: [95] enumerates the
# PyYAML-dependent set by PARSING import statements, and an inline `python3 -c "import
# yaml"` hides the dependency from that census. Each fallback interpreter is now probed
# BEFORE being selected (the pyshim branch used to be taken on mere existence, so a shim
# without PyYAML was chosen and then failed the check it was selected to satisfy).
_has_yaml() {
    "$1" - >/dev/null 2>&1 <<'PYPROBE'
import yaml
PYPROBE
}

PY="python3"
if ! _has_yaml "$PY"; then
    if [ -x "$HOME/.pyshim/python3" ] && _has_yaml "$HOME/.pyshim/python3"; then
        PY="$HOME/.pyshim/python3"
    elif [ -x "/usr/bin/python3" ] && _has_yaml "/usr/bin/python3"; then
        PY="/usr/bin/python3"
    fi
fi
if ! _has_yaml "$PY"; then
    echo "coverage_map_guard: BLOCK — cannot verify: PyYAML is required and no python3 on" >&2
    echo "  PATH provides it. Exiting 2 (cannot verify) rather than reporting a pass this" >&2
    echo "  run did not earn." >&2
    echo "  Install: python3 -m pip install pyyaml   (or: export PATH=\"\$HOME/.pyshim:\$PATH\")" >&2
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
