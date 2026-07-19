#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.e-commerce/scenario-guards/locale_format_guard.sh
#
# HAND-ROLLED — capability-gap signal (confirmed absent, not just "not found
# by us"). This scenario's brief adds a requirement: L4 pages doing
# locale-aware number/date formatting MUST use Intl.NumberFormat /
# Intl.DateTimeFormat, never raw toLocaleString()/string concatenation.
# practices/consumer-proof/engine/canary-gaps.yaml CANARY-001 already PLANTED
# this exact need and verified it absent at 2026-07-19:
#   grep -rlE "locale.{0,20}(number|date).{0,25}format|toLocaleString.{0,100}
#             (require|enforce|must|MUST)" practices-react/rules/*.md
#   → 0 matches. There is no @ax/eslint-plugin-ax rule for this shape either
#   (checked: react/eslint.config.mjs + practices-react/rules/*.md — no
#   locale-format rule id exists). So this is not reusable — it is hand-rolled
#   here, isolated to this scenario, as the intended-signature scanner Lane A
#   would otherwise provide via ESLint.
#
# WHAT IT ENFORCES (grep-based, deliberately simple — a real fix upstream
# would be a proper AST-shape ESLint rule, out of scope for a scenario probe):
#   FORBIDDEN in *.tsx under --root: `.toLocaleString(` with no arguments
#   (locale-blind) and manual date string concatenation of getMonth()/getDate()
#   /getFullYear() via `+`.
#   REQUIRED in the clean fixture: at least one `Intl.NumberFormat` or
#   `Intl.DateTimeFormat` call.
#
# Exit codes: 0 — no forbidden pattern AND Intl.* present · 1 — violation
# (signature: LOCALE_FORMAT_VIOLATION) · 2 — usage/env error.
#
# Usage: bash locale_format_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "locale_format_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "locale_format_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "locale_format_guard: no such dir $ROOT_OVERRIDE — SKIP"
    exit 0
fi

files="$(find "$ROOT_OVERRIDE" -name '*.tsx' 2>/dev/null || true)"
count=0
if [ -n "$files" ]; then
    count="$(printf '%s\n' "$files" | grep -c . || true)"
fi
if [ "$count" -eq 0 ]; then
    echo "locale_format_guard: 0 *.tsx files under $ROOT_OVERRIDE — nothing to check"
    exit 0
fi
echo "locale_format_guard: scanned $count *.tsx file(s)"

# Forbidden shape 1: bare toLocaleString() call with no argument (locale-blind).
FORBIDDEN_TOLOCALE='\.toLocaleString\(\)'
# Forbidden shape 2: manual date-part string concatenation (getMonth()/getDate()/getFullYear
# joined with `+`), the classic hand-rolled non-Intl date formatter.
FORBIDDEN_DATECONCAT='get(Month|Date|FullYear)\(\).*\+.*get(Month|Date|FullYear)\(\)'

hits="$(grep -rnE "$FORBIDDEN_TOLOCALE|$FORBIDDEN_DATECONCAT" "$ROOT_OVERRIDE" --include='*.tsx' 2>/dev/null || true)"

if [ -n "$hits" ]; then
    echo "VIOLATION: locale-blind formatting — raw toLocaleString()/manual date-string concat instead of Intl.NumberFormat/Intl.DateTimeFormat:" >&2
    echo "$hits" | sed 's/^/  /' >&2
    echo "locale_format_guard: LOCALE_FORMAT_VIOLATION — BLOCKED" >&2
    exit 1
fi

# Positive requirement: the clean rewrite must actually use Intl.
if ! grep -rlE 'Intl\.(NumberFormat|DateTimeFormat)' "$ROOT_OVERRIDE" --include='*.tsx' >/dev/null 2>&1; then
    echo "VIOLATION: no Intl.NumberFormat/Intl.DateTimeFormat usage found — locale-aware formatting requirement not met" >&2
    echo "locale_format_guard: LOCALE_FORMAT_VIOLATION — BLOCKED" >&2
    exit 1
fi

echo "locale_format_guard: no locale-blind formatting; Intl.NumberFormat/Intl.DateTimeFormat present"
exit 0
