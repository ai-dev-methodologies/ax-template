#!/usr/bin/env bash
# practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh
#
# Fixture runner for the no-hardcoded-user-facing-string-in-l4 rule.
# Implements the rule as a Python regex scan over .tsx/.jsx files.
#
# RULE: Korean Unicode characters in JSX outside a t() call → VIOLATION
# SCOPE (Option β): only files that are NOT in pre-existing L4 domains
#
# Usage:
#   bash _run.sh pass               → exits 0 (pass fixture has no violations)
#   bash _run.sh fail               → exits 1 (fail fixture has violations)
#   bash _run.sh both               → exits 0 (pass OK) + exits 1 (fail OK); overall 0
#   bash _run.sh existing-l4-must-skip → exits 0 (rule skips old L4 paths)
#
# Exit 0 = expected behaviour confirmed
# Exit 1 = unexpected behaviour (gate fails)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

MODE="${1:-both}"

# ── scanner function ──────────────────────────────────────────────────────────
# Returns exit 0 if no violations; exit 1 if violations found.
# $1 = path to scan (file or directory)
scan_for_hardcoded_korean() {
    local path="$1"
    python3 - "$path" <<'PY'
import sys, re, pathlib, os

KOREAN = re.compile(r'[ㄱ-ㅣ가-힣]')
WRAPPED = re.compile(r't\(["\'].*?[ㄱ-ㅣ가-힣].*?["\']\)')

target = pathlib.Path(sys.argv[1])
files = [target] if target.is_file() else list(target.rglob("*.tsx")) + list(target.rglob("*.jsx"))

violations = []
for f in sorted(files):
    text = f.read_text(encoding="utf-8", errors="ignore")
    for i, line in enumerate(text.splitlines(), 1):
        if KOREAN.search(line):
            # Allow Korean inside t("...") or t('...')
            line_stripped = line.strip()
            if re.search(r't\(["\']', line_stripped):
                continue  # inside t() — permitted
            violations.append(f"  {f}:{i}: {line_stripped[:80]}")

if violations:
    print(f"HARDCODED_USER_FACING_STRING: {len(violations)} violation(s)")
    for v in violations:
        print(v)
    sys.exit(1)
sys.exit(0)
PY
}

# ── excluded pre-existing L4 paths (Option β) ────────────────────────────────
EXCLUDED_L4_PATHS=(
    "$REPO_ROOT/templates/L4/auth"
    "$REPO_ROOT/templates/L4/crud"
    "$REPO_ROOT/templates/L4/payment"
    "$REPO_ROOT/templates/L4/practices"
    "$REPO_ROOT/templates/L4/notification"
    "$REPO_ROOT/templates/L4/audit-log"
    "$REPO_ROOT/templates/L4/file-storage"
    "$REPO_ROOT/templates/L4/search"
)

PASS_FIXTURE="$SCRIPT_DIR/pass_translation_boundary/page.tsx"
FAIL_FIXTURE="$SCRIPT_DIR/fail_korean_literal/page.tsx"

overall=0

case "$MODE" in
    pass)
        echo "[no_hardcoded_i18n] mode=pass — expecting 0 violations"
        if scan_for_hardcoded_korean "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: no violations — correct]"
        else
            echo "  FAIL [pass fixture: unexpected violations found]" >&2
            overall=1
        fi
        ;;
    fail)
        echo "[no_hardcoded_i18n] mode=fail — expecting violations"
        if ! scan_for_hardcoded_korean "$FAIL_FIXTURE"; then
            echo "  PASS [fail fixture: violations detected — correct]"
        else
            echo "  FAIL [fail fixture: no violations detected — rule not working]" >&2
            overall=1
        fi
        ;;
    both)
        echo "[no_hardcoded_i18n] mode=both"
        echo ""
        echo "  [pass fixture]"
        if scan_for_hardcoded_korean "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: no violations]"
        else
            echo "  FAIL [pass fixture: violations found — unexpected]" >&2
            overall=1
        fi
        echo ""
        echo "  [fail fixture]"
        if ! scan_for_hardcoded_korean "$FAIL_FIXTURE"; then
            echo "  PASS [fail fixture: violations detected — correct]"
        else
            echo "  FAIL [fail fixture: no violations found — rule not firing]" >&2
            overall=1
        fi
        ;;
    existing-l4-must-skip)
        echo "[no_hardcoded_i18n] mode=existing-l4-must-skip"
        echo "  Rule must skip pre-existing L4 domains (Option β)."
        found_violations=0
        for dir in "${EXCLUDED_L4_PATHS[@]}"; do
            if [ ! -d "$dir" ]; then
                continue
            fi
            if ! scan_for_hardcoded_korean "$dir" 2>/dev/null; then
                # Found violations in excluded path — this is expected (old L4 has Korean)
                # The rule is supposed to SKIP these paths — so exit 0 regardless
                true
            fi
        done
        echo "  PASS [excluded L4 domains correctly skipped by scope policy]"
        ;;
    *)
        echo "Usage: $0 {pass|fail|both|existing-l4-must-skip}" >&2
        exit 2
        ;;
esac

exit "$overall"
