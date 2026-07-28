#!/usr/bin/env bash
# practices/evals/l2_frontmatter_deps_guard.sh
# BACKLOG P3-66 — L2 block frontmatter `dependencies:` drift.
#
# templates/L2/blocks/invoice-list.tsx and pricing-table.tsx both declared
# `dependencies: [currency-formatter, ...]` while their actual import is
# `formatCurrencyAmount` from templates/L1/components/currency-input.tsx — a
# real sibling file, but NOT the one named. currency-formatter.tsx is a
# different, correctly-anchored L1 component (see G1/P2-40); the drift is a
# stale/renamed reference, not a fabrication.
#
# THE INVARIANT (necessary-not-sufficient floor, deliberately narrow — see
# NOTE below): if an L2 block declares a dependency slug D that resolves to a
# REAL sibling template file (templates/L1/components/D.tsx or
# templates/L2/blocks/D.tsx), and the file does NOT actually import D, BUT it
# DOES import some OTHER real sibling S from the very same directory (S != D)
# — that is strong, mechanical evidence D is stale: the file swapped which
# sibling it uses and nobody updated the frontmatter to match. Flag it.
#
# NOTE — why this does NOT check "every declared dependency must be
# imported": the vast majority of `dependencies:` entries across this catalog
# (button, badge, input, label, select, checkbox, textarea, form, card,
# separator, tooltip, alert-dialog, table, popover, date-range-picker, …) name
# REAL templates/L1/components/*.tsx files that are intentionally NEVER
# imported by module specifier — L2 blocks render them as plain HTML/Tailwind
# (shadcn-primitive vocabulary, documentation-only; templates/ is copy-target
# source, never bundled by any app in this repo). A blanket "must appear in
# imports" rule would false-positive on dozens of pre-existing, correct blocks.
# This guard only fires on the DEMONSTRABLE mismatch case (declared-but-unused
# sibling co-existing with an actually-imported different sibling) — it will
# not (and is not meant to) catch a merely-omitted or doc-only dependency.
#
# Exit codes:
#   0 — no stale-sibling-dependency mismatch found
#   1 — at least one violation
#   2 — usage error / missing required source dirs / missing python3
#
# Usage:
#   bash practices/evals/l2_frontmatter_deps_guard.sh
#   bash practices/evals/l2_frontmatter_deps_guard.sh --root DIR   # e.g. a fixture root

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "l2_frontmatter_deps_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

L1_DIR="$REPO_ROOT/templates/L1/components"
L2_DIR="$REPO_ROOT/templates/L2/blocks"

if [ ! -d "$L1_DIR" ]; then
    echo "l2_frontmatter_deps_guard: missing $L1_DIR" >&2
    exit 2
fi
if [ ! -d "$L2_DIR" ]; then
    echo "l2_frontmatter_deps_guard: missing $L2_DIR" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "l2_frontmatter_deps_guard: FAIL — python3 not on PATH" >&2
    exit 2
fi

L1_DIR="$L1_DIR" L2_DIR="$L2_DIR" python3 - <<'PYEOF'
import os
import re
import sys

l1_dir = os.environ["L1_DIR"]
l2_dir = os.environ["L2_DIR"]

def slugs_of(dirpath):
    out = set()
    for name in os.listdir(dirpath):
        if name.endswith((".ts", ".tsx")) and os.path.isfile(os.path.join(dirpath, name)):
            out.add(re.sub(r"\.tsx?$", "", name))
    return out

l1_slugs = slugs_of(l1_dir)
l2_slugs = slugs_of(l2_dir)

DEPS_RE = re.compile(r"^\s*dependencies\s*:\s*\[(.*?)\]\s*$", re.MULTILINE)
IMPORT_FROM_RE = re.compile(r"\bimport\b[\s\S]*?\bfrom\s*['\"]([^'\"]+)['\"]")

def parse_deps(text):
    m = DEPS_RE.search(text)
    if not m:
        return []
    raw = m.group(1).strip()
    if raw == "":
        return []
    out = []
    for tok in raw.split(","):
        tok = tok.strip().strip('"').strip("'")
        if tok:
            out.append(tok)
    return out

def import_slug(path, own_dir_slugs):
    """Resolve an import specifier to a bare slug IF it points at
    templates/L1/components/<slug> or templates/L2/blocks/<slug> (any prefix:
    '@/', relative, or bare), or at a same-directory relative sibling
    ('./<slug>', '../blocks/<slug>')."""
    m = re.search(r"templates/L1/components/([^'\"/]+)", path)
    if m:
        return "L1", re.sub(r"\.tsx?$", "", m.group(1))
    m = re.search(r"templates/L2/blocks/([^'\"/]+)", path)
    if m:
        return "L2", re.sub(r"\.tsx?$", "", m.group(1))
    if path.startswith("./") or path.startswith("../"):
        base = re.sub(r"\.tsx?$", "", path.rsplit("/", 1)[-1])
        if base in own_dir_slugs:
            return "L2", base
    return None, None

violations = []

for name in sorted(os.listdir(l2_dir)):
    if not name.endswith((".ts", ".tsx")):
        continue
    fpath = os.path.join(l2_dir, name)
    if not os.path.isfile(fpath):
        continue
    with open(fpath, "r", encoding="utf-8") as fh:
        text = fh.read()

    declared = parse_deps(text)
    if not declared:
        continue

    imported_l1 = set()
    imported_l2 = set()
    for m in IMPORT_FROM_RE.finditer(text):
        layer, slug = import_slug(m.group(1), l2_slugs)
        if layer == "L1":
            imported_l1.add(slug)
        elif layer == "L2":
            imported_l2.add(slug)

    for dep in declared:
        if dep in l1_slugs:
            if dep in imported_l1:
                continue
            if imported_l1:  # some OTHER L1 sibling IS imported instead
                violations.append(
                    f"{name}: declares dependency '{dep}' (templates/L1/components/{dep}.tsx exists) "
                    f"but is not imported; the file actually imports L1 sibling(s) {sorted(imported_l1)} instead "
                    f"— stale/renamed dependency reference"
                )
        elif dep in l2_slugs:
            if dep in imported_l2:
                continue
            if imported_l2:  # some OTHER L2 sibling IS imported instead
                violations.append(
                    f"{name}: declares dependency '{dep}' (templates/L2/blocks/{dep}.tsx exists) "
                    f"but is not imported; the file actually imports L2 sibling(s) {sorted(imported_l2)} instead "
                    f"— stale/renamed dependency reference"
                )
        # else: dep does not resolve to a real sibling template file (generic
        # UI-primitive vocabulary or an npm package name) — not checkable here,
        # intentionally out of scope (see NOTE at top of this script).

if violations:
    print("l2_frontmatter_deps_guard: FAIL", file=sys.stderr)
    for v in violations:
        print(f"  - {v}", file=sys.stderr)
    sys.exit(1)

print(f"l2_frontmatter_deps_guard: PASS — {len(os.listdir(l2_dir))} L2 block file(s) scanned, "
      f"no stale-sibling-dependency mismatch")
sys.exit(0)
PYEOF
exit $?
