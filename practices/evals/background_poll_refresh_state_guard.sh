#!/usr/bin/env bash
# practices/evals/background_poll_refresh_state_guard.sh
# R82b (44th hard guard) — mechanises practices/rules/background-poll-must-show-refresh-state.md
# (R82). Scans every L4 frontend (.tsx) file for code-level
# `refetchInterval:` declarations inside useQuery option blocks; for each
# matching file, requires a sibling reference to `dataUpdatedAt` so the
# polling cadence is visible to the operator.
#
# Detection is line-anchored (`^[[:space:]]*refetchInterval[[:space:]]*:`)
# so it skips:
#   - the same string inside a `//` or `/* ... */` comment
#   - the same string inside a JSDoc / TSDoc frontmatter block
#   - mentions inside template strings or markdown headers
#
# A file that uses refetchInterval but does NOT reference dataUpdatedAt is
# in violation of R82 — operators get a stale view with no freshness
# signal.
#
# Exit codes:
#   0 — every refetchInterval-using L4 file also references dataUpdatedAt
#   1 — at least one file uses refetchInterval without dataUpdatedAt
#   2 — usage / environment error
#
# Usage:
#   bash practices/evals/background_poll_refresh_state_guard.sh
#   bash practices/evals/background_poll_refresh_state_guard.sh --root DIR
#
# Bash 3.2 compatible (no associative arrays, no ${var,,}).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "background_poll_refresh_state_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

L4_DIR="templates/L4"
[ ! -d "$L4_DIR" ] && exit 0

# Pattern: start-of-line (after whitespace) `refetchInterval:`. The
# leading-anchor excludes comment occurrences such as
# `// refetchInterval: ...` and `* refetchInterval: ...` because the
# closest non-space prefix of those lines is `//` or `*`, not the bare
# `refetchInterval` identifier.
CODE_PATTERN='^[[:space:]]*refetchInterval[[:space:]]*:'

violations=0
violation_lines=""

while IFS= read -r -d '' file; do
    # Skip *.md and other non-source files; we scan .tsx and .ts only.
    case "$file" in
        *.tsx|*.ts) ;;
        *) continue ;;
    esac

    # Exclude infrastructure files. R82 governs per-useQuery refetchInterval
    # at the *call site*. A QueryClient-default refetchInterval inside
    # providers.tsx (or a top-level layout) is a config pattern: it applies
    # to every query in the app and there is no single render surface to
    # attach a dataUpdatedAt timestamp to. Adoption of R82 still requires
    # the page-level renders to expose dataUpdatedAt, which this guard
    # checks at the page-level useQuery call site.
    base=$(basename "$file")
    case "$base" in
        providers.tsx|providers.ts) continue ;;
        layout.tsx|layout.ts) continue ;;
    esac

    # Does this file actually use refetchInterval in code?
    matches=$(grep -nE "$CODE_PATTERN" "$file" 2>/dev/null || true)
    [ -z "$matches" ] && continue

    # Check 1: file references dataUpdatedAt somewhere (destructure, JSX
    # usage, helper variable). Match the identifier as a word so a
    # partial substring inside a comment about `lastDataUpdatedAtSeen`
    # would still count — that level of strictness is delegated to the
    # R82 rule's reviewer guidance, not to this mechanical first pass.
    has_data_updated_at=0
    if grep -qE 'dataUpdatedAt' "$file" 2>/dev/null; then
        has_data_updated_at=1
    fi

    # Check 2: if the file ALSO uses useMutation, R82 requires the
    # mutation trigger button to expose aria-busy so screen-reader
    # users see the in-flight signal alongside the polled freshness.
    # If no useMutation is present, the freshness signal alone
    # satisfies R82.
    needs_aria_busy=0
    if grep -qE 'useMutation\b' "$file" 2>/dev/null; then
        needs_aria_busy=1
    fi

    has_aria_busy=0
    if grep -qE 'aria-busy' "$file" 2>/dev/null; then
        has_aria_busy=1
    fi

    while IFS= read -r match_line; do
        line_no="${match_line%%:*}"
        if [ "$has_data_updated_at" -eq 0 ]; then
            violations=$((violations + 1))
            violation_lines="${violation_lines}
$file:$line_no — refetchInterval used without sibling dataUpdatedAt reference"
        fi
        if [ "$needs_aria_busy" -eq 1 ] && [ "$has_aria_busy" -eq 0 ]; then
            violations=$((violations + 1))
            violation_lines="${violation_lines}
$file:$line_no — refetchInterval + useMutation present without aria-busy on the mutation trigger (R82 WCAG SC 4.1.3)"
        fi
    done <<EOF
$matches
EOF
done < <(find "$L4_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null)

if [ "$violations" -gt 0 ]; then
    echo "VIOLATION: L4 frontend uses refetchInterval without visible dataUpdatedAt (R82 background-poll-must-show-refresh-state):" >&2
    echo "$violation_lines" >&2
    echo "" >&2
    echo "Destructure dataUpdatedAt from the same useQuery and render it as a freshness timestamp:" >&2
    echo "  const { data, dataUpdatedAt } = useQuery({ refetchInterval: ... })" >&2
    echo "  <span aria-live=\"polite\">{dataUpdatedAt && \`Updated \${new Date(dataUpdatedAt).toLocaleTimeString()}\`}</span>" >&2
    echo "background_poll_refresh_state_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "background_poll_refresh_state_guard: every L4 refetchInterval has a sibling dataUpdatedAt reference"
exit 0
