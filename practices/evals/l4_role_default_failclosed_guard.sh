#!/usr/bin/env bash
# practices/evals/l4_role_default_failclosed_guard.sh
# R83 (42nd hard guard) — mechanises practices/rules/rbac-stub-default-fail-closed.md
# (R47), found via R75 audit when audit-log/export/page.tsx had `const hasExportRole = true`
# as a fail-OPEN default.
#
# R83b (Wave-A US-004 extension, same file) widens the check to ALSO
# catch dynamic role assignment where the LHS of === is NOT the canonical
# useCallerRole hook return:
#
#   const has<X>Role  = userType === 'admin'        # VIOLATION — userType ≠ callerRole
#   const isAdmin     = currentUser.role === 'ADMIN' # VIOLATION — member access ≠ callerRole
#   const hasAuditor  = callerRole === 'auditor'    # OK — callerRole is the canonical hook return
#   const isOwner     = useCallerRole() === 'owner' # OK — direct hook call
#
# Pass 1 (static-true) — Greps templates/L4/**/*.tsx for inline role/auth
# flags assigned to true at declaration:
#
#   const has<X>Role        = true   # canonical fail-OPEN pattern (R75 case)
#   const isAdmin           = true
#   const isAuditor         = true
#   const isEditor          = true
#   const isOwner           = true
#   const isManager         = true
#   const can(Edit|Delete|Create|Update|Approve|Manage)... = true
#
# Pass 2 (dynamic-from-non-callerRole) — Same role-flag identifier set,
# but the RHS is a `=== <something>` comparison whose LHS is NOT
# `callerRole` (the variable returned by useCallerRole) and NOT a direct
# `useCallerRole()` invocation. Catches the failure mode where a developer
# derives a role flag from a non-canonical source (a server-supplied user
# payload, a query-param, a feature-flag value) and silently bypasses
# fork-receiver RBAC discipline.
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

# Role-flag identifier alternation, shared by Pass 1 and Pass 2.
ROLE_IDENT='(has[A-Z][a-zA-Z]*Role|is(Admin|Auditor|Editor|Owner|Manager)|can(Edit|Delete|Create|Update|Approve|Manage)[a-zA-Z]*)'

# Pass 1 — declaration of a role/permission flag assigned literal `true`.
# Anchored to the start-of-statement (let|const|var) to avoid matching
# inside strings, comments, or object property values.
STATIC_TRUE_PATTERN="^[[:space:]]*(const|let|var)[[:space:]]+${ROLE_IDENT}[[:space:]]*=[[:space:]]*true\\b"

# Pass 2 — role-flag assigned to a `=== <something>` expression.
DYNAMIC_PATTERN="^[[:space:]]*(const|let|var)[[:space:]]+${ROLE_IDENT}[[:space:]]*=.*==="

# Whitelist regex for "the LHS of === is the canonical callerRole source"
# — applied to each Pass-2 match. Tolerates surrounding whitespace.
ALLOWED_LHS='(callerRole|useCallerRole\([[:space:]]*\))[[:space:]]*===|===[[:space:]]*(callerRole|useCallerRole\([[:space:]]*\))'

violations=0
matches=""

append_violation() {
    if [ -z "$matches" ]; then
        matches="$1"
    else
        matches="$matches
$1"
    fi
    violations=$((violations + 1))
}

# Iterate only .tsx and .ts files under templates/L4/.
while IFS= read -r -d '' file; do
    # Pass 1 — static true.
    while IFS= read -r line; do
        append_violation "$file: $line"
    done < <(grep -nE "$STATIC_TRUE_PATTERN" "$file" 2>/dev/null || true)

    # Pass 2 — dynamic === with non-callerRole LHS. Extract the exact LHS
    # of `===` (text between the assignment `=` and `===`), trim whitespace
    # and compare against the canonical normalized forms `callerRole` and
    # `useCallerRole()`. Substring match is intentionally rejected — a line
    # like `const isAdmin = !callerRole === 'admin'` must NOT pass.
    while IFS= read -r line; do
        # `line` is "<lineno>:<source>"; strip the line-number prefix.
        source_part="${line#*:}"

        # Capture everything between the assignment `=` and the `===`.
        # The leading `[^=]` excludes the assignment from belonging to a
        # `==` or `===` operator, but the file pattern guarantees the
        # `===` appears later on the line.
        if [[ "$source_part" =~ [^=!]=[[:space:]]*([^=]*)=== ]]; then
            lhs="${BASH_REMATCH[1]}"
            # Trim leading + trailing whitespace.
            lhs="${lhs#"${lhs%%[![:space:]]*}"}"
            lhs="${lhs%"${lhs##*[![:space:]]}"}"
            if [ "$lhs" = "callerRole" ] || [ "$lhs" = "useCallerRole()" ]; then
                continue
            fi
        fi
        append_violation "$file: $line"
    done < <(grep -nE "$DYNAMIC_PATTERN" "$file" 2>/dev/null || true)
done < <(find "$L4_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null)

if [ "$violations" -gt 0 ]; then
    echo "VIOLATION: L4 frontend page declares a fail-OPEN role default OR derives a role flag from a non-canonical source (R47 rbac-stub-default-fail-closed):" >&2
    echo "$matches" >&2
    echo "" >&2
    echo "Apply useCallerRole() from templates/L0/fork-receiver-kit/use-caller-id:" >&2
    echo "  const callerRole = useCallerRole()" >&2
    echo "  const hasXRole = callerRole === 'admin'   // fail-CLOSED" >&2
    echo "l4_role_default_failclosed_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "l4_role_default_failclosed_guard: every L4 role/permission flag uses callerRole as fail-closed source"
exit 0
