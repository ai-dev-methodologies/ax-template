#!/usr/bin/env bash
# practices-react/evals/fixtures/impersonation-banner-required-when-acting-as-other-user/_run.sh
#
# Fixture runner for the impersonation-banner-required-when-acting-as-other-user rule.
#
# The rule detects any non-comment code line that sets session.actingAs to a
# non-null value (direct assignment OR {actingAs: <non-null>} object shape)
# without a co-located <ImpersonationBanner> tag in the scanned file(s).
#
# KEY PROPERTY: Rule matches the canonical actingAs STATE, not the function name.
# Renaming assumeUserId() → runAsUser() does NOT bypass the rule.
#
# Usage:
#   bash _run.sh pass                → exits 0 (pass fixture: banner present)
#   bash _run.sh fail                → exits 1 (fail fixture: banner absent)
#   bash _run.sh fail_helper_renamed → exits 1 (renamed helper still caught)
#   bash _run.sh both                → exits 0 (all cases correct)
#
# Exit 0 = expected behaviour confirmed
# Exit 1 = unexpected behaviour (gate fails)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODE="${1:-both}"

# ── scanner ───────────────────────────────────────────────────────────────────
# Detects actingAs mutations (to non-null values) without <ImpersonationBanner>.
# Skips comment lines. Skips `actingAs: null` / `actingAs = null` initializations.
# $1 = path to scan (file or directory)
scan_for_missing_banner() {
    local path="$1"
    python3 - "$path" <<'PY'
import sys, re, pathlib

# Match canonical actingAs mutations to non-null values (skip comment lines)
# Pattern 1: session.actingAs = <identifier>  (not null)
ACTING_AS_ASSIGN = re.compile(r'\.actingAs\s*=\s*(?!null\b)(\w+)')
# Pattern 2: { actingAs: <identifier> }  (not null)
ACTING_AS_OBJECT = re.compile(r'\bactingAs\s*:\s*(?!null\b)(\w+)')

# ImpersonationBanner usage patterns (JSX tag or data-component stub in pass fixture)
BANNER_PATTERNS = [
    re.compile(r'<ImpersonationBanner'),
    re.compile(r'data-component=["\']ImpersonationBanner["\']'),
]

target = pathlib.Path(sys.argv[1])
files = [target] if target.is_file() else (
    list(target.rglob("*.tsx")) + list(target.rglob("*.ts"))
)

violations = []
for f in sorted(files):
    text = f.read_text(encoding="utf-8", errors="ignore")
    lines = text.splitlines()

    # Check for actingAs mutation (skipping comment lines)
    has_acting_as = False
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('*'):
            continue
        if ACTING_AS_ASSIGN.search(line) or ACTING_AS_OBJECT.search(line):
            has_acting_as = True
            break

    # Check for banner in non-comment lines only
    has_banner = False
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('*'):
            continue
        if any(p.search(line) for p in BANNER_PATTERNS):
            has_banner = True
            break

    if has_acting_as and not has_banner:
        violations.append(f"  {f}: sets actingAs (non-null) without <ImpersonationBanner>")

if violations:
    print(f"IMPERSONATION_BANNER_MISSING: {len(violations)} violation(s)")
    for v in violations:
        print(v)
    sys.exit(1)
sys.exit(0)
PY
}

PASS_FIXTURE="$SCRIPT_DIR/pass_with_banner"
FAIL_DIRECT_FIXTURE="$SCRIPT_DIR/fail_direct_actingAs"
FAIL_RENAMED_FIXTURE="$SCRIPT_DIR/fail_helper_renamed_runAsUser"

overall=0

case "$MODE" in
    pass)
        echo "[impersonation-banner] mode=pass — expecting 0 violations (banner present)"
        if scan_for_missing_banner "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: banner present — correct]"
        else
            echo "  FAIL [pass fixture: false positive — banner present but rule fired]" >&2
            overall=1
        fi
        ;;
    fail)
        echo "[impersonation-banner] mode=fail — expecting violations (direct actingAs, no banner)"
        if ! scan_for_missing_banner "$FAIL_DIRECT_FIXTURE"; then
            echo "  PASS [fail_direct fixture: violation detected — correct]"
        else
            echo "  FAIL [fail_direct fixture: no violation — rule not firing on direct assignment]" >&2
            overall=1
        fi
        ;;
    fail_helper_renamed)
        echo "[impersonation-banner] mode=fail_helper_renamed — renamed helper must still be caught"
        echo "  Validates Critic Soft Suggestion 2: helper rename bypass is IMPOSSIBLE."
        if ! scan_for_missing_banner "$FAIL_RENAMED_FIXTURE"; then
            echo "  PASS [fail_helper_renamed: runAsUser() {actingAs: userId} caught — bypass impossible]"
        else
            echo "  FAIL [fail_helper_renamed: rule did NOT fire — bypass succeeded]" >&2
            echo "  REGRESSION: runAsUser() returning {actingAs: userId} was not detected!" >&2
            overall=1
        fi
        ;;
    both)
        echo "[impersonation-banner] mode=both"
        echo ""
        echo "  [pass fixture — banner present]"
        if scan_for_missing_banner "$PASS_FIXTURE"; then
            echo "  PASS [pass fixture: no false positives]"
        else
            echo "  FAIL [pass fixture: false positive — unexpected]" >&2
            overall=1
        fi

        echo ""
        echo "  [fail fixture — direct actingAs assignment]"
        if ! scan_for_missing_banner "$FAIL_DIRECT_FIXTURE"; then
            echo "  PASS [fail_direct: violation detected on direct assignment]"
        else
            echo "  FAIL [fail_direct: rule not firing on direct actingAs assignment]" >&2
            overall=1
        fi

        echo ""
        echo "  [fail fixture — renamed helper (Critic Soft Suggestion 2 bypass attempt)]"
        if ! scan_for_missing_banner "$FAIL_RENAMED_FIXTURE"; then
            echo "  PASS [fail_helper_renamed: runAsUser(){actingAs: id} caught — bypass impossible]"
        else
            echo "  FAIL [fail_helper_renamed: renamed helper NOT detected — bypass succeeded]" >&2
            overall=1
        fi
        ;;
    *)
        echo "Usage: $0 {pass|fail|fail_helper_renamed|both}" >&2
        exit 2
        ;;
esac

exit "$overall"
