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
# ── BACKLOG P3-97 — the aria-busy leg is (page, view)-pair aware and JSX-anchored ──
#
# Two defects closed here, both of the "guard fooled by a comment" class that
# P3-81 closed elsewhere:
#
#   1. The probe was a bare `grep -q 'aria-busy'`, so a page carrying only a
#      TRUTHFUL comment ("aria-busy lives in the co-located view") satisfied
#      it. The probe now requires a JSX ATTRIBUTE — `aria-busy=` followed by
#      `{`, `"` or `'` — on a line that is not itself a comment. A comment
#      mentioning the attribute name no longer counts.
#   2. Under the P2-42 presentational split the mutation's button markup —
#      and therefore its aria-busy — legitimately lives in the co-located
#      `*-view.tsx`, not in page.tsx. Requirement (1) alone would have
#      FAILED those pages. So the guard resolves each page's converted view
#      through practices/evals/l4_presentational_view_ledger.yaml (the
#      enforced record [94] already checks against disk) and accepts the
#      attribute in EITHER file of the pair.
#
# A page with no ledger entry keeps single-file semantics: unconverted pages
# must carry the attribute themselves. The dataUpdatedAt leg stays page-scoped
# by construction — it is destructured from the useQuery call, which the
# convention keeps on the page.
#
# Exit codes:
#   0 — every refetchInterval-using L4 surface has dataUpdatedAt (and
#       aria-busy when useMutation is present).
#   1 — at least one violation.
#   2 — usage / environment error (incl. missing python3 / PyYAML / ledger —
#       fail-closed: an unreadable ledger would silently degrade every
#       converted pair back to single-file mode).
#
# Usage:
#   bash practices/evals/background_poll_refresh_state_guard.sh
#   bash practices/evals/background_poll_refresh_state_guard.sh --root DIR
#   bash practices/evals/background_poll_refresh_state_guard.sh --root DIR --ledger FILE
#
# Bash 3.2 compatible (no associative arrays, no ${var,,}).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
LEDGER_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --ledger) LEDGER_OVERRIDE="$2"; shift 2 ;;
        --ledger=*) LEDGER_OVERRIDE="${1#--ledger=}"; shift ;;
        *) echo "background_poll_refresh_state_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

LEDGER_FILE="$SCRIPT_DIR/l4_presentational_view_ledger.yaml"
if [ -n "$LEDGER_OVERRIDE" ]; then
    # Absolutise against the INVOCATION cwd — the guard cd's to REPO_ROOT below,
    # so a relative --ledger would otherwise resolve against the wrong directory.
    case "$LEDGER_OVERRIDE" in
        /*) LEDGER_FILE="$LEDGER_OVERRIDE" ;;
        *)  LEDGER_FILE="$(pwd)/$LEDGER_OVERRIDE" ;;
    esac
fi

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

# ── P3-97: (page, view) pair resolution ──────────────────────────────────────
# Fail closed on a missing parser or ledger: degrading to single-file mode
# would make every converted pair fail spuriously, and degrading to
# "accept anything" would reopen the comment hole this closes.
if ! command -v python3 >/dev/null 2>&1; then
    echo "background_poll_refresh_state_guard: FAIL — python3 not on PATH (needed to read $LEDGER_FILE)" >&2
    exit 2
fi
if ! python3 -c "import yaml" >/dev/null 2>&1; then
    echo "background_poll_refresh_state_guard: FAIL — PyYAML not installed (needed to read $LEDGER_FILE)" >&2
    exit 2
fi
if [ ! -f "$LEDGER_FILE" ]; then
    echo "background_poll_refresh_state_guard: FAIL — missing presentational-view ledger $LEDGER_FILE" >&2
    exit 2
fi

# One `page<TAB>view` line per ledger entry, paths repo-root-relative.
PAIRS=$(LEDGER_FILE="$LEDGER_FILE" python3 - <<'PYEOF'
import os, sys, yaml
with open(os.environ["LEDGER_FILE"], encoding="utf-8") as fh:
    ledger = yaml.safe_load(fh) or {}
for entry in (ledger.get("entries") or []):
    page, view = entry.get("page"), entry.get("view")
    if page and view:
        print(f"{page}\t{view}")
PYEOF
) || { echo "background_poll_refresh_state_guard: FAIL — could not parse $LEDGER_FILE" >&2; exit 2; }

# Echo the ledgered view for $1, or nothing when the page is unconverted.
view_for_page() {
    printf '%s\n' "$PAIRS" | awk -F'\t' -v p="$1" '$1 == p { print $2; exit }'
}

# A REAL aria-busy JSX attribute (`aria-busy={...}` / `aria-busy="true"`).
# Excludes prose mentioning the attribute name, and the tailwind `aria-busy:`
# variant in a className (no `=` follows).
ARIA_BUSY_ATTR_RE='aria-busy[[:space:]]*=[[:space:]]*[{"'"'"']'

# Does $1 contain ERE $2 in CODE — i.e. on a line that is not itself a comment?
#
# The `refetchInterval:` trigger has been line-anchored since R82b precisely so a
# comment could not fire it; both compliance legs were plain whole-file greps, so
# a comment could SATISFY them. P3-97 names that for aria-busy; the identical hole
# in the dataUpdatedAt leg surfaced while writing this guard's own fixtures (the
# fixture's explanatory comment mentioned the token and the fixture passed), so
# both legs go through this one probe.
#
# Boundary (declared, not hidden): a `//`-, `*`- or `/*`-prefixed line counts as
# comment, so a token inside the CONTINUATION line of a block comment that starts
# with neither marker would still count. Full comment tokenization is out of scope
# for a bash guard; no such occurrence exists in the scanned trees, and the fail
# fixtures pin the realistic forms (`//` line comments and JSDoc `*` bodies).
has_code_token() {
    [ -f "$1" ] || return 1
    awk -v re="$2" '
        {
            stripped = $0
            sub(/^[[:space:]]+/, "", stripped)
            if (stripped ~ /^\/\// || stripped ~ /^\*/ || stripped ~ /^\/\*/) next
            if ($0 ~ re) { found = 1; exit }
        }
        END { exit(found ? 0 : 1) }
    ' "$1"
}

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

    if ! has_code_token "$f" 'dataUpdatedAt'; then
        violations=$((violations + 1))
        violation_lines="${violation_lines}
$f:$locator — refetchInterval${note:+ ($note)} present without dataUpdatedAt in code (a comment naming it does NOT satisfy this)"
    fi

    if grep -qE 'useMutation\b' "$f" 2>/dev/null; then
        # P3-97 — accept the JSX attribute in this file OR in its ledgered
        # co-located presentational view (P2-42 split moved the button markup
        # there). A COMMENT naming the attribute satisfies neither.
        local paired_view
        paired_view="$(view_for_page "$f")"
        if ! has_code_token "$f" "$ARIA_BUSY_ATTR_RE" \
            && { [ -z "$paired_view" ] || ! has_code_token "$paired_view" "$ARIA_BUSY_ATTR_RE"; }; then
            local where
            if [ -n "$paired_view" ]; then
                where="neither this file nor its ledgered view ($paired_view) carries an aria-busy JSX attribute"
            else
                where="no aria-busy JSX attribute (and this page has no ledgered presentational view to hold one)"
            fi
            violations=$((violations + 1))
            violation_lines="${violation_lines}
$f:$locator — refetchInterval + useMutation${note:+ ($note)} present but $where on the mutation trigger (R82 WCAG SC 4.1.3). A comment mentioning aria-busy does NOT satisfy this."
        fi
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
    echo "" >&2
    echo "For the aria-busy leg, put the real attribute on the mutation trigger — in this" >&2
    echo "page or in its ledgered co-located view (P2-42 split):" >&2
    echo "  <button aria-busy={mutation.isPending || undefined} ...>" >&2
    echo "background_poll_refresh_state_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "background_poll_refresh_state_guard: every L4 refetchInterval has a sibling dataUpdatedAt reference" \
     "(P3-97: both legs matched in CODE, not comments; the aria-busy JSX attribute accepted across each (page, view) pair)"
exit 0
