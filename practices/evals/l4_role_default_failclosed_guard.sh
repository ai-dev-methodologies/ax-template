#!/usr/bin/env bash
# practices/evals/l4_role_default_failclosed_guard.sh
# R83 (42nd hard guard) — mechanises practices/rules/rbac-stub-default-fail-closed.md
# (R47), found via R75 audit when audit-log/export/page.tsx had `const hasExportRole = true`
# as a fail-OPEN default.
#
# Greps templates/L4/**/*.tsx for inline role/auth flags assigned to true at declaration:
#
#   const has<X>Role        = true   # canonical fail-OPEN pattern (R75 case)
#   const isAdmin           = true
#   const isAuditor         = true
#   const isEditor          = true
#   const isOwner           = true
#   const isManager         = true
#   const can(Edit|Delete|Create|Update|Approve|Manage)... = true
#
# Comments and string literals are excluded (the R75 rule body has these
# patterns inside <pre> blocks; those are legitimate references, not code).
#
# Exit codes:
#   0 — no fail-OPEN role default in any L4 page
#   1 — at least one violation
#   2 — usage error / missing L4 dir
#
# Usage:
#   bash practices/evals/l4_role_default_failclosed_guard.sh
#   bash practices/evals/l4_role_default_failclosed_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "l4_role_default_failclosed_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

L4_DIR="templates/L4"
[ ! -d "$L4_DIR" ] && exit 0

# Pattern: declaration of a role/permission flag assigned literal `true`.
# Anchored to the start-of-statement (let|const|var) to avoid matching
# inside strings, comments, or object property values.
PATTERN='^[[:space:]]*(const|let|var)[[:space:]]+(has[A-Z][a-zA-Z]*Role|is(Admin|Auditor|Editor|Owner|Manager)|can(Edit|Delete|Create|Update|Approve|Manage)[a-zA-Z]*)[[:space:]]*=[[:space:]]*true\b'

violations=0
matches=""

# Iterate only .tsx and .ts files under templates/L4/**/app/
while IFS= read -r -d '' file; do
    while IFS= read -r line; do
        violations=$((violations + 1))
        if [ -z "$matches" ]; then
            matches="$file: $line"
        else
            matches="$matches
$file: $line"
        fi
    done < <(grep -nE "$PATTERN" "$file" 2>/dev/null || true)
done < <(find "$L4_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null)

if [ "$violations" -gt 0 ]; then
    echo "VIOLATION: L4 frontend page declares fail-OPEN role default (R47 rbac-stub-default-fail-closed):" >&2
    echo "$matches" >&2
    echo "" >&2
    echo "Apply useCallerRole() from templates/L0/fork-receiver-kit/use-caller-id:" >&2
    echo "  const callerRole = useCallerRole()" >&2
    echo "  const hasXRole = callerRole === 'admin'   // fail-CLOSED" >&2
    echo "l4_role_default_failclosed_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "l4_role_default_failclosed_guard: every L4 role/permission flag uses fail-closed default"
exit 0
