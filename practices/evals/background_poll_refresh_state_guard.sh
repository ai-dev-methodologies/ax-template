#!/usr/bin/env bash
# practices/evals/background_poll_refresh_state_guard.sh
# R82b (44th hard guard) — mechanises practices/rules/background-poll-must-show-refresh-state.md
# (R82). Scans every L4 frontend (.tsx) file for code-level
# `refetchInterval:` declarations inside useQuery option blocks; for each
# matching file, requires a sibling reference to `dataUpdatedAt` so the
# polling cadence is visible to the operator. If the same file ALSO uses
# `useMutation`, requires an `aria-busy` reference so screen-reader users
# track the in-flight mutation state alongside the polled freshness
# (WCAG SC 4.1.3 Status Messages).
#
# Detection is line-anchored (`^[[:space:]]*refetchInterval[[:space:]]*:`)
# so it skips:
#   - the same string inside a `//` or `/* ... */` comment
#   - the same string inside a JSDoc / TSDoc frontmatter block
#   - mentions inside template strings or markdown headers
#
# Two trigger patterns:
#   - Direct per-useQuery refetchInterval at the call site (any L4 .tsx).
#   - QueryClient-default refetchInterval inside an L4 providers.tsx,
#     which implicitly polls every query in the same L4. In that case the
#     guard walks every sibling .tsx under templates/L4/<domain>/ and
#     enforces the same compliance per page.
#
# Exit codes:
#   0 — every refetchInterval-using L4 surface has dataUpdatedAt (and
#       aria-busy when useMutation is present).
#   1 — at least one violation.
#   2 — usage / environment error.
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
L2_DIR="templates/L2"
[ ! -d "$L4_DIR" ] && exit 0

# Pattern: start-of-line (after whitespace) `refetchInterval:`. The
# leading-anchor excludes comment occurrences such as
# `// refetchInterval: ...` and `* refetchInterval: ...` because the
# closest non-space prefix of those lines is `//` or `*`, not the bare
# `refetchInterval` identifier.
CODE_PATTERN='^[[:space:]]*refetchInterval[[:space:]]*:'

violations=0
violation_lines=""

# Shared R82-compliance probe. Args:
#   $1 — source file (the page being audited)
#   $2 — locator string for the violation message ("<lineno>" for direct,
#        "*" + provider-citation for provider-default branch)
#   $3 — optional context note appended to the violation message
check_page_r82() {
    local f="$1"
    local locator="$2"
    local note="$3"

    if ! grep -qE 'useQuery\b' "$f" 2>/dev/null; then
        return
    fi

    if ! grep -qE 'dataUpdatedAt' "$f" 2>/dev/null; then
        violations=$((violations + 1))
        violation_lines="${violation_lines}
$f:$locator — refetchInterval${note:+ ($note)} present without dataUpdatedAt"
    fi

    if grep -qE 'useMutation\b' "$f" 2>/dev/null \
        && ! grep -qE 'aria-busy' "$f" 2>/dev/null; then
        violations=$((violations + 1))
        violation_lines="${violation_lines}
$f:$locator — refetchInterval + useMutation${note:+ ($note)} present without aria-busy on the mutation trigger (R82 WCAG SC 4.1.3)"
    fi
}

while IFS= read -r -d '' file; do
    # Skip *.md and other non-source files; we scan .tsx and .ts only.
    case "$file" in
        *.tsx|*.ts) ;;
        *) continue ;;
    esac

    base=$(basename "$file")

    # Branch A: providers.tsx with a QueryClient-default refetchInterval.
    # The default polls every query inside the L4, so every sibling
    # page under templates/L4/<domain>/ must satisfy R82. Scope the
    # walk to the L4 domain root (dirname twice up from providers.tsx).
    if [ "$base" = "providers.tsx" ] || [ "$base" = "providers.ts" ]; then
        if ! grep -qE "$CODE_PATTERN" "$file" 2>/dev/null; then
            continue
        fi
        l4_domain_dir="$(dirname "$(dirname "$file")")"
        while IFS= read -r -d '' page_file; do
            pb=$(basename "$page_file")
            case "$pb" in providers.tsx|providers.ts|layout.tsx|layout.ts) continue ;; esac
            check_page_r82 "$page_file" "*" "QueryClient-default in $file"
        done < <(find "$l4_domain_dir" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null)
        continue
    fi

    # Branch B: layout.tsx is infrastructure; skip.
    if [ "$base" = "layout.tsx" ] || [ "$base" = "layout.ts" ]; then
        continue
    fi

    # Branch C: direct per-useQuery refetchInterval at this call site.
    matches=$(grep -nE "$CODE_PATTERN" "$file" 2>/dev/null || true)
    [ -z "$matches" ] && continue

    # Emit one violation per matching line so the operator sees the
    # exact useQuery declaration to fix; reuse the same compliance probe.
    while IFS= read -r match_line; do
        line_no="${match_line%%:*}"
        check_page_r82 "$file" "$line_no" ""
    done <<EOF
$matches
EOF
# R82-iter4 extension — combine L4 + L2 scan targets. notification-bell /
# notification-list / future L2 polling blocks compose into L4 pages
# and must satisfy the same R82 contract or the catalog's L4 audit
# misses them (codex iter3 outside-scope finding). Build the combined
# path list outside the process substitution so the heredoc above
# parses cleanly.
done < <(
    { find "$L4_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null;
      [ -d "$L2_DIR" ] && find "$L2_DIR" -type f \( -name "*.tsx" -o -name "*.ts" \) -print0 2>/dev/null;
      true; }
)

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
