#!/usr/bin/env bash
# practices-react/evals/fixtures/feature_gate/_run.sh
#
# Fixture runner for the prefer-feature-gate-over-env-check rule.
# Implements the rule as a Python regex scan over .tsx/.jsx files.
#
# RULE: process.env.NEXT_PUBLIC_FEATURE_* or process.env.NEXT_PUBLIC_FF_*
#       in JSX files → VIOLATION (must use FeatureGate or feature-flags API)
#
# Usage:
#   bash _run.sh pass     → exits 0 (pass fixture has no violations)
#   bash _run.sh fail     → exits 1 (fail fixture has violations)
#   bash _run.sh both     → exits 0 (pass OK) + exits 1 (fail OK); overall 0
#
# Exit 0 = expected behaviour confirmed
# Exit 1 = unexpected behaviour (gate fails)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODE="${1:-both}"

# ── scanner function ──────────────────────────────────────────────────────────
# Returns exit 0 if no violations; exit 1 if violations found.
# $1 = path to scan (file or directory)
scan_for_env_feature_flag() {
    local path="$1"
    python3 - "$path" <<'PY'
import sys, re, pathlib

# Matches: process.env.NEXT_PUBLIC_FEATURE_ or process.env.NEXT_PUBLIC_FF_
# Also matches: process.env.FEATURE_ (without NEXT_PUBLIC_ prefix)
ENV_FLAG = re.compile(
    r'process\.env\.(NEXT_PUBLIC_)?(FEATURE_|FF_)'
)

target = pathlib.Path(sys.argv[1])
files = [target] if target.is_file() else list(target.rglob("*.tsx")) + list(target.rglob("*.jsx"))

violations = []
for f in sorted(files):
    text = f.read_text(encoding="utf-8", errors="ignore")
    for i, line in enumerate(text.splitlines(), 1):
        if ENV_FLAG.search(line):
            violations.append(f"  {f}:{i}: {line.strip()[:100]}")

if violations:
    print(f"PREFER_FEATURE_GATE: {len(violations)} violation(s) — use FeatureGate instead of process.env")
    for v in violations:
        print(v)
    sys.exit(1)
sys.exit(0)
PY
}

PASS_FIXTURE="$SCRIPT_DIR/pass/Component.tsx"
FAIL_FIXTURE="$SCRIPT_DIR/fail_process_env_check/Component.tsx"

overall=0

case "$MODE" in
    pass)
        echo "[feature_gate] mode=pass — expecting 0 violations"
        if scan_for_env_feature_flag "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: no violations — correct]"
        else
            echo "  FAIL [pass fixture: unexpected violations found]" >&2
            overall=1
        fi
        ;;
    fail)
        echo "[feature_gate] mode=fail — expecting violations"
        if ! scan_for_env_feature_flag "$FAIL_FIXTURE"; then
            echo "  PASS [fail fixture: violations detected — correct]"
        else
            echo "  FAIL [fail fixture: no violations detected — rule not working]" >&2
            overall=1
        fi
        ;;
    both)
        echo "[feature_gate] mode=both"
        echo ""
        echo "  [pass fixture]"
        if scan_for_env_feature_flag "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: no violations]"
        else
            echo "  FAIL [pass fixture: violations found — unexpected]" >&2
            overall=1
        fi
        echo ""
        echo "  [fail fixture]"
        if ! scan_for_env_feature_flag "$FAIL_FIXTURE"; then
            echo "  PASS [fail fixture: violations detected — correct]"
        else
            echo "  FAIL [fail fixture: no violations found — rule not firing]" >&2
            overall=1
        fi
        ;;
    *)
        echo "Usage: $0 {pass|fail|both}" >&2
        exit 2
        ;;
esac

exit "$overall"
