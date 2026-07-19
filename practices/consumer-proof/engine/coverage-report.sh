#!/usr/bin/env bash
# practices/consumer-proof/engine/coverage-report.sh
#
# Part 2.2 of the gap-convergence engine design. Thin wrapper around
# lib/coverage_report.py: parses coverage-map.yaml, applies honesty
# downgrades against disk truth (yaml self-report never outranks disk),
# prints per-tier + total weighted coverage and the top-N uncovered cells
# ranked by w·(1−score).
#
# Usage:
#   bash practices/consumer-proof/engine/coverage-report.sh
#   bash practices/consumer-proof/engine/coverage-report.sh --write   # also
#       regenerates docs/coverage-map/COVERAGE.md + appends to
#       docs/coverage-map/coverage-history.jsonl
#   bash practices/consumer-proof/engine/coverage-report.sh --top-n 10
#   bash practices/consumer-proof/engine/coverage-report.sh --repo-root DIR
#
# Requires python3 + PyYAML on PATH (see CLAUDE.md R25 toolchain
# prerequisites — this machine needs the pyshim:
#   export PATH="$HOME/.pyshim:$PATH"
# or call /usr/bin/python3 directly).
#
# Exit codes: 0 = report printed (this script does not gate pass/fail —
# that is coverage_map_guard.sh's job). 2 = usage / toolchain error.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

PY="python3"
if ! "$PY" -c "import yaml" >/dev/null 2>&1; then
    if [ -x "$HOME/.pyshim/python3" ]; then
        PY="$HOME/.pyshim/python3"
    elif [ -x "/usr/bin/python3" ] && /usr/bin/python3 -c "import yaml" >/dev/null 2>&1; then
        PY="/usr/bin/python3"
    fi
fi

if ! "$PY" -c "import yaml" >/dev/null 2>&1; then
    echo "coverage-report.sh: no python3 with PyYAML found on PATH." >&2
    echo "  Try: export PATH=\"\$HOME/.pyshim:\$PATH\"" >&2
    exit 2
fi

exec "$PY" "$SCRIPT_DIR/lib/coverage_report.py" --repo-root "$REPO_ROOT" "$@"
