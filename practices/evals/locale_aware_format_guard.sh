#!/usr/bin/env bash
# practices/evals/locale_aware_format_guard.sh
#
# Closes the confirmed catalog gap surfaced by consumer-proof cell
# S3.e-commerce (practices/consumer-proof/engine/canary-gaps.yaml CANARY-001):
# there was no practices-react rule, no shipped ax/* ESLint rule, and no
# standalone shell guard enforcing locale-aware number/date formatting on the
# frontend. See practices-react/rules/locale-aware-number-date-format.md for
# the rule this guard mechanically enforces (spec_ref:
# specs/i18n-policy-l0.yaml#I18N-FORMATTING-001 — the backend item already
# requires NumberFormat/DateTimeFormatter over hard-coded format strings; this
# guard is the FE-side symmetric enforcement via Intl.NumberFormat /
# Intl.DateTimeFormat).
#
# Forbidden shapes in *.tsx:
#   1. bare `.toLocaleString()` with no locale/options argument (locale-blind
#      — silently follows the server/runtime default locale, not the caller's).
#   2. manual date-part string concatenation via getMonth()/getDate()/
#      getFullYear() joined with `+` — the classic hand-rolled non-Intl date
#      formatter that also hard-codes a display order (Korean uses
#      yyyy.MM.dd, US uses MM/dd/yyyy — a `+`-concatenated string can't
#      switch order per locale).
# This guard only forbids the anti-pattern above (mirrors the shape of
# money_boundary_seam_guard.sh — a repo-wide scan cannot require every *.tsx
# file to contain Intl.* formatting, since most components format neither
# a number nor a date). A directory with zero *.tsx files is a SKIP, not a
# silent pass.
#
# Exit codes: 0 — no forbidden pattern AND Intl.* present (or nothing to scan)
#             1 — violation (signature: LOCALE_FORMAT_VIOLATION)
#             2 — usage/env error.
#
# Usage:
#   bash practices/evals/locale_aware_format_guard.sh                # default root=frontend/src
#   bash practices/evals/locale_aware_format_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "locale_aware_format_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SRC="${ROOT_OVERRIDE:-$REPO_ROOT/frontend/src}"

if [ ! -d "$SRC" ]; then
    echo "locale_aware_format_guard: no such dir $SRC — SKIP"
    exit 0
fi

files="$(find "$SRC" -name '*.tsx' 2>/dev/null || true)"
count=0
if [ -n "$files" ]; then
    count="$(printf '%s\n' "$files" | grep -c . || true)"
fi
if [ "$count" -eq 0 ]; then
    echo "locale_aware_format_guard: 0 *.tsx files under $SRC — nothing to check"
    exit 0
fi
echo "locale_aware_format_guard: scanned $count *.tsx file(s) under $SRC"

# Forbidden shape 1: bare toLocaleString() call with no argument (locale-blind).
FORBIDDEN_TOLOCALE='\.toLocaleString\(\)'
# Forbidden shape 2: manual date-part string concatenation (getMonth()/getDate()/
# getFullYear() joined with `+`), the classic hand-rolled non-Intl date formatter.
FORBIDDEN_DATECONCAT='get(Month|Date|FullYear)\(\).*\+.*get(Month|Date|FullYear)\(\)'

hits="$(grep -rnE "$FORBIDDEN_TOLOCALE|$FORBIDDEN_DATECONCAT" "$SRC" --include='*.tsx' 2>/dev/null || true)"

if [ -n "$hits" ]; then
    echo "VIOLATION: locale-blind formatting — raw toLocaleString()/manual date-string concat instead of Intl.NumberFormat/Intl.DateTimeFormat:" >&2
    echo "$hits" | sed 's/^/  /' >&2
    echo "locale_aware_format_guard: LOCALE_FORMAT_VIOLATION — BLOCKED" >&2
    exit 1
fi

echo "locale_aware_format_guard: no locale-blind formatting found"
exit 0
