#!/usr/bin/env bash
# practices/evals/saved_view_url_state_guard.sh — FMW4b binary guard for the
# rule saved-view-must-be-url-state-or-server-persisted. Replaces that rule's
# prior verification.type:review with two MECHANICAL, scope-limited checks so
# the most-emphasized URL-as-state rule is enforced, not just reviewed.
#
# CHECK 1 — forbidden persistence (repo-wide, scoped to saved-view files):
#   A "saved-view file" is a .ts/.tsx under templates/ that references the
#   SavedView identifier OR imports templates/L2/blocks/saved-view. Such a file
#   MUST NOT make a localStorage METHOD CALL (getItem/setItem/removeItem/clear
#   or window.localStorage). localStorage view state is non-shareable,
#   non-bookmarkable, and lost in incognito (web.dev URL-as-state; MDN).
#
#   The predicate matches a localStorage CALL, never the bare word — so the
#   saved-view.tsx block (which only NAMES localStorage in its "FORBIDDEN"
#   JSDoc + the `'url' | 'server'` type that excludes it) does NOT false-
#   positive. This is the FMW4a lesson applied: a name-based check needs a
#   shape guard. Test files (*.spec.* / *.test.*) and _fixtures are excluded.
#
# CHECK 2 — catalog dogfoods URL-state (one pinned reference path):
#   templates/L4/crud/app/(crud)/items/page.tsx MUST drive page/sort/filter
#   through useUrlListState (the L0 URL-state primitive). A positive assertion
#   on a single pinned path → zero false positives; if the reference regresses
#   to React.useState for view config, the useUrlListState call disappears and
#   this fails.
#
# Modes:
#   (default)            scan the live repo; exit 0 clean, 1 on any violation.
#   --check-fixture DIR  CHECK 1 only, over DIR; exit 1 if a saved-view file in
#                        DIR makes a localStorage call (the fail fixture), else 0.
#
# Exit: 0 PASS · 1 violation · 2 usage error.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# A localStorage *method call* (not the bare identifier / a string literal).
LS_CALL='localStorage[[:space:]]*\.[[:space:]]*(get|set|remove)Item|localStorage[[:space:]]*\.[[:space:]]*clear|window[[:space:]]*\.[[:space:]]*localStorage'
# A saved-view file marker.
SV_MARKER='SavedView|blocks/saved-view'

PINNED_REF="templates/L4/crud/app/(crud)/items/page.tsx"

# Collect saved-view .ts/.tsx files under a root (excluding tests + fixtures).
collect_saved_view_files() {
    local root="$1" include_fixtures="${2:-0}"
    local find_args=(-type f \( -name '*.ts' -o -name '*.tsx' \)
        -not -name '*.spec.*' -not -name '*.test.*')
    [ "$include_fixtures" -eq 0 ] && find_args+=(-not -path '*/_fixtures/*' -not -path '*/fixtures/*')
    while IFS= read -r f; do
        [ -n "$f" ] || continue
        if grep -qE "$SV_MARKER" "$f" 2>/dev/null; then
            printf '%s\n' "$f"
        fi
    done < <(find "$root" "${find_args[@]}" 2>/dev/null | sort)
}

# ── --check-fixture DIR mode (CHECK 1 only) ─────────────────────────────────
if [ "${1:-}" = "--check-fixture" ]; then
    DIR="${2:-}"
    [ -n "$DIR" ] && [ -d "$DIR" ] || { echo "saved_view_url_state_guard: --check-fixture needs an existing dir" >&2; exit 2; }
    hit=0
    while IFS= read -r f; do
        [ -n "$f" ] || continue
        if grep -qE "$LS_CALL" "$f" 2>/dev/null; then
            echo "  VIOLATION: $f makes a localStorage call for saved-view state"
            hit=1
        fi
    done < <(collect_saved_view_files "$DIR" 1)
    [ "$hit" -eq 0 ] && echo "saved_view_url_state_guard: PASS (fixture clean) — $DIR" || echo "saved_view_url_state_guard: localStorage detected (expected for a fail fixture) — $DIR"
    exit "$hit"
fi

if [ $# -gt 0 ]; then
    echo "saved_view_url_state_guard: unknown arg: $1" >&2
    exit 2
fi

# ── default: live repo scan ─────────────────────────────────────────────────
cd "$REPO_ROOT" || { echo "saved_view_url_state_guard: cannot cd $REPO_ROOT" >&2; exit 2; }
VIOLATIONS=0

# CHECK 1
while IFS= read -r f; do
    [ -n "$f" ] || continue
    if grep -qE "$LS_CALL" "$f" 2>/dev/null; then
        echo "CHECK 1 VIOLATION: $f manages saved-view state via localStorage (use URL or server)"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done < <(collect_saved_view_files "$REPO_ROOT/templates" 0)

# CHECK 2
if [ ! -f "$PINNED_REF" ]; then
    echo "CHECK 2 VIOLATION: pinned reference missing: $PINNED_REF"
    VIOLATIONS=$((VIOLATIONS + 1))
elif ! grep -qE 'useUrlListState[[:space:]]*\(' "$PINNED_REF" 2>/dev/null; then
    echo "CHECK 2 VIOLATION: $PINNED_REF does not drive view state through useUrlListState()"
    echo "  (the L4 crud list reference must dogfood the URL-as-state rule — FMW4b)"
    VIOLATIONS=$((VIOLATIONS + 1))
fi

if [ "$VIOLATIONS" -ne 0 ]; then
    echo "saved_view_url_state_guard: FAIL — $VIOLATIONS violation(s)"
    exit 1
fi

echo "saved_view_url_state_guard: PASS — saved-view files are URL/server-persisted; L4 crud ref uses useUrlListState"
exit 0
