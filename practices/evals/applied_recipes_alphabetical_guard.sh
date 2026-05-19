#!/usr/bin/env bash
# practices/evals/applied_recipes_alphabetical_guard.sh — R12 SP49 (TD-2026-05-24-031).
#
# Hard guard. For each templates/L4/<domain>/README.md containing an
# `applied_recipes:` plural-list block, assert the entries are alphabetically
# sorted (ASCII case-insensitive lexical). Single-entry lists pass vacuously.
#
# Honored skips:
#   * R5 legacy singular form `applied_recipe: <name>` (no plural block) — SKIP.
#   * L4 READMEs without any `applied_recipes:` key (e.g. file-storage, practices) — SKIP.
#
# Inline trailing comments on list entries are stripped before comparison:
#   "- api-gateway-relay  # R10 SP47 alphabetical insert" → "api-gateway-relay".
# Prose mentions of `applied_recipes:` inside backticks are ignored — only lines
# that BEGIN with `applied_recipes:` at column 0 open a list block.
#
# Usage:
#   bash practices/evals/applied_recipes_alphabetical_guard.sh             # live repo
#   bash practices/evals/applied_recipes_alphabetical_guard.sh --fixtures  # pass + fail fixtures
#   bash practices/evals/applied_recipes_alphabetical_guard.sh --root DIR  # scan DIR/templates/L4/*/README.md
#                                                                          # or, when no templates/L4/ subtree,
#                                                                          # scan DIR/*/README.md as L4 roots.
#
# Exit codes: 0 PASS · 1 unsorted (or empty list) · 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "applied_recipes_alphabetical_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/applied_recipes_alphabetical"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "applied_recipes_alphabetical_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [applied_recipes_alphabetical/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [applied_recipes_alphabetical/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [applied_recipes_alphabetical/$(basename "$sub")] — expected exit 1 on FAIL fixture"
            fail=$((fail + 1))
        else
            echo "PASS [applied_recipes_alphabetical/$(basename "$sub")]"
            pass=$((pass + 1))
        fi
    done

    echo ""
    echo "applied_recipes_alphabetical_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Resolve scan roots ──────────────────────────────────────────────────────
if [ -n "$ROOT_OVERRIDE" ]; then
    SCAN_BASE="$ROOT_OVERRIDE"
else
    SCAN_BASE="$REPO_ROOT"
fi

python3 - "$SCAN_BASE" <<'PYEOF'
import sys
import pathlib
import re
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

scan_base = pathlib.Path(sys.argv[1])

# Collect README.md candidates.
#   1. Preferred: <scan_base>/templates/L4/*/README.md (live repo + fixtures mirroring tree)
#   2. Fallback: <scan_base>/*/README.md (compact fixture layout — one subdir per L4)
candidates = sorted((scan_base / "templates" / "L4").glob("*/README.md"))
if not candidates:
    candidates = sorted(scan_base.glob("*/README.md"))

if not candidates:
    print(f"applied_recipes_alphabetical_guard: no L4 README candidates under {scan_base} — nothing to check")
    sys.exit(0)

def extract_applied_recipes_block(text):
    """Return list of entries or None if no plural block present.

    A plural block begins at a line that BEGINS with `applied_recipes:` (column 0),
    not at a backticked prose mention. The block runs until the next non-empty line
    that doesn't start with two spaces + "- " (allowing trailing comments).

    A README may contain multiple such blocks in narrative + canonical positions;
    we take the FIRST occurrence as the canonical Recipe Composition block.
    """
    entries = None
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]
        if re.match(r'^applied_recipes:\s*$', line):
            entries = []
            i += 1
            while i < len(lines):
                ln = lines[i]
                item_m = re.match(r'^  -\s+(\S[^\s#]*)', ln)
                if item_m:
                    entries.append(item_m.group(1).strip())
                    i += 1
                    continue
                # Blank line inside the block — terminates block.
                if ln.strip() == "":
                    break
                # Any other shape — terminates block.
                break
            return entries
        i += 1
    return None

violations = 0
checked = 0
skipped = 0

for readme in candidates:
    text = readme.read_text()
    entries = extract_applied_recipes_block(text)
    rel = readme.relative_to(scan_base) if readme.is_relative_to(scan_base) else readme
    if entries is None:
        # No plural applied_recipes: block — SKIP per guard contract (R5 singular or keyless L4).
        skipped += 1
        continue
    if len(entries) == 0:
        # Empty list — recipe_governance_guard handles the "empty list" violation;
        # alphabetical guard cannot verify ordering against zero entries → treat
        # as violation here too so the discipline is enforced symmetrically.
        print(f"VIOLATION [applied_recipes_alphabetical]: {rel} — applied_recipes: block has zero entries")
        violations += 1
        continue
    checked += 1
    # Case-insensitive ASCII lexical sort.
    sorted_entries = sorted(entries, key=lambda s: s.lower())
    if entries != sorted_entries:
        print(f"VIOLATION [applied_recipes_alphabetical]: {rel} — entries not alphabetical")
        print(f"  observed: {entries}")
        print(f"  expected: {sorted_entries}")
        violations += 1

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
if violations == 0:
    print(f'{{"signal":"l4.applied_recipes.alphabetical_pass_count","value":{checked},"skipped":{skipped},"ts":"{ts}"}}')
    sys.exit(0)
else:
    print(f'{{"signal":"l4.applied_recipes.alphabetical_violation_count","value":{violations},"ts":"{ts}"}}')
    sys.exit(1)
PYEOF
