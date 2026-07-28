#!/usr/bin/env bash
# practices/evals/l4_presentational_view_guard.sh
# BACKLOG P2-28 — L4 pages untestable-as-shipped, right-sized.
#
# Enforces the convention documented in templates/L4/README.md and
# docs/NEW-DOMAIN-CHECKLIST.md §7: a data-rendering L4 page.tsx extracts its
# render layer into a co-located pure <domain>-<surface>-view.tsx (props-only,
# no data-fetching hooks) so the render path is unit-testable from a vitest
# outside frontend/ without a QueryClientProvider or shared vitest-config
# aliasing for a bare npm specifier (@tanstack/react-query, cmdk, ...).
#
# Checked against practices/evals/l4_presentational_view_ledger.yaml, which
# is the enforced RECORD of which (page, view) pairs have actually been
# converted — NOT a claim about every L4 page (full conversion of the ~79
# page.tsx files is out of scope; see BACKLOG "L4 render-testability —
# remainder"). Per entry:
#   1. the view file must exist on disk;
#   2. the view file must NOT import a data-fetching hook (useQuery/useSWR/
#      useMutation bare specifiers @tanstack/react-query or swr) — "no
#      useQuery" per the convention;
#   3. the page file must exist AND import the view (a relative specifier
#      resolving to the view's own basename) — catches "re-inlined the JSX
#      and let the view file rot unimported".
#
# Ledger-shrink protection: entries may be ADDED over time; the ledger may
# never claim fewer than `min_entries` (declared inside the ledger file
# itself) — silently deleting an entry to hide a regression trips this floor.
#
# Exit codes:
#   0 — every ledger entry's page/view pair holds
#   1 — at least one violation (missing file / view has a data-fetching hook /
#       page does not import its view / ledger shrunk below min_entries)
#   2 — usage error / missing required source files / missing python3 / PyYAML
#
# Usage:
#   bash practices/evals/l4_presentational_view_guard.sh
#   bash practices/evals/l4_presentational_view_guard.sh --root DIR   # e.g. a fixture root

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
        *) echo "l4_presentational_view_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

LEDGER_FILE="$SCRIPT_DIR/l4_presentational_view_ledger.yaml"
[ -n "$LEDGER_OVERRIDE" ] && LEDGER_FILE="$LEDGER_OVERRIDE"

if [ ! -f "$LEDGER_FILE" ]; then
    echo "l4_presentational_view_guard: missing ledger $LEDGER_FILE" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "l4_presentational_view_guard: FAIL — python3 not on PATH" >&2
    exit 2
fi

if ! python3 -c "import yaml" >/dev/null 2>&1; then
    echo "l4_presentational_view_guard: FAIL — PyYAML not installed" >&2
    exit 2
fi

REPO_ROOT="$REPO_ROOT" LEDGER_FILE="$LEDGER_FILE" python3 - <<'PYEOF'
import os
import re
import sys
import yaml

repo_root = os.environ["REPO_ROOT"]
ledger_file = os.environ["LEDGER_FILE"]

with open(ledger_file, encoding="utf-8") as fh:
    ledger = yaml.safe_load(fh) or {}

min_entries = ledger.get("min_entries")
entries = ledger.get("entries") or []

errors = []

if not isinstance(min_entries, int):
    errors.append("ledger: min_entries missing or not an integer")
    min_entries = 0

if len(entries) < min_entries:
    errors.append(
        f"LEDGER SHRUNK: {len(entries)} entr{'y' if len(entries)==1 else 'ies'} present, "
        f"but min_entries declares {min_entries} — an entry was removed without lowering "
        f"min_entries (or a real regression occurred)"
    )

DATA_FETCH_HOOK_RE = re.compile(
    r"from\s*['\"](@tanstack/react-query|swr)['\"]"
)

for entry in entries:
    domain = entry.get("domain", "?")
    surface = entry.get("surface", "?")
    label = f"{domain}/{surface}"

    page_rel = entry.get("page")
    view_rel = entry.get("view")
    if not page_rel or not view_rel:
        errors.append(f"[{label}] ledger entry missing 'page' or 'view' key")
        continue

    page_path = os.path.join(repo_root, page_rel)
    view_path = os.path.join(repo_root, view_rel)

    if not os.path.isfile(view_path):
        errors.append(f"[{label}] view file missing: {view_rel}")
        continue
    if not os.path.isfile(page_path):
        errors.append(f"[{label}] page file missing: {page_rel}")
        continue

    with open(view_path, encoding="utf-8") as fh:
        view_text = fh.read()
    with open(page_path, encoding="utf-8") as fh:
        page_text = fh.read()

    m = DATA_FETCH_HOOK_RE.search(view_text)
    if m:
        errors.append(
            f"[{label}] view file imports a data-fetching hook module ('{m.group(1)}') — "
            f"the convention requires the view to be props-only, no useQuery/useSWR: {view_rel}"
        )

    view_basename = re.sub(r"\.tsx?$", "", os.path.basename(view_rel))
    import_re = re.compile(
        r"from\s*['\"]\.[^'\"]*" + re.escape(view_basename) + r"['\"]"
    )
    if not import_re.search(page_text):
        errors.append(
            f"[{label}] page file does not import its ledgered view "
            f"(expected a relative import resolving to '{view_basename}'): {page_rel} -> {view_rel}"
        )

if errors:
    print("l4_presentational_view_guard: FAIL", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)

print(f"l4_presentational_view_guard: PASS — {len(entries)} ledgered (page, view) pair(s) hold "
      f"(min_entries={min_entries})")
sys.exit(0)
PYEOF
exit $?
