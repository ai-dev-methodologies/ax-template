#!/usr/bin/env bash
# practices/evals/cross_recipe_inv_uniqueness_guard.sh — R12 SP49 (TD-2026-05-24-030).
#
# Hard guard. Block any two active recipes from declaring identical
# (L4_domain_prefix, business_invariants[].id) pairs.
#
#   L4_domain_prefix is derived from each INV's spec_ref:
#       specs/audit-log-l0.yaml#AUDIT-RECORD-001  →  audit-log
#       specs/crud-security.yaml                  →  crud-security
#   INVs anchored only via rule_ref or co-shipped-rule (no spec_ref) are skipped
#   for indexing — no L4 domain can be derived.
#
# Recipe-prefixed IDs (e.g. CRM-INV-001, API-GATEWAY-RELAY-INV-001) are exempt
# by construction: no two recipes share a recipe-name prefix, so their (prefix,
# id) keys cannot collide. This guard is PROTECTIVE not corrective — the disk
# census at R12 PRD signature confirms zero current collisions across the 11
# active recipes. The guard's value materializes when a future cycle adds a
# recipe that would otherwise collide (TD-2026-05-24-030).
#
# Usage:
#   bash practices/evals/cross_recipe_inv_uniqueness_guard.sh              # live repo (specs/recipes/)
#   bash practices/evals/cross_recipe_inv_uniqueness_guard.sh --fixtures   # run pass + fail fixtures
#   bash practices/evals/cross_recipe_inv_uniqueness_guard.sh --root DIR   # scan DIR/*-recipe-l0.yaml
#
# Exit codes: 0 PASS · 1 collision found · 2 usage error.

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
        *) echo "cross_recipe_inv_uniqueness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/cross_recipe_inv_uniqueness"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "cross_recipe_inv_uniqueness_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [cross_recipe_inv_uniqueness/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [cross_recipe_inv_uniqueness/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [cross_recipe_inv_uniqueness/$(basename "$sub")] — expected exit 1 on FAIL fixture"
            fail=$((fail + 1))
        else
            echo "PASS [cross_recipe_inv_uniqueness/$(basename "$sub")]"
            pass=$((pass + 1))
        fi
    done

    echo ""
    echo "cross_recipe_inv_uniqueness_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live mode (or --root override) ────────────────────────────────────────────
SCAN_DIR="${ROOT_OVERRIDE:-$REPO_ROOT/specs/recipes}"
if [ ! -d "$SCAN_DIR" ]; then
    # No recipes directory yet — pass vacuously (matches recipe_governance_guard policy).
    echo "cross_recipe_inv_uniqueness_guard: no recipes directory at $SCAN_DIR — nothing to check"
    exit 0
fi

python3 - "$SCAN_DIR" <<'PYEOF'
import sys
import pathlib
import re
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

scan_dir = pathlib.Path(sys.argv[1])

# Match any *.yaml in the directory so fixture-mode files (with arbitrary
# names) are picked up alongside live specs/recipes/*-recipe-l0.yaml.
spec_files = sorted(scan_dir.glob("*.yaml"))
if not spec_files:
    print(f"cross_recipe_inv_uniqueness_guard: no recipe YAMLs found in {scan_dir} — nothing to check")
    sys.exit(0)

# index: (L4_domain_prefix, inv_id) -> list of (recipe_spec_path, spec_ref_value)
from collections import defaultdict
index = defaultdict(list)

inv_count = 0
skipped_no_spec_ref = 0

for spec_path in spec_files:
    text = spec_path.read_text()

    # Slice the business_invariants block (everything between the key and the next top-level key).
    m = re.search(r'^business_invariants:\s*\n(.*?)(?=^\w|\Z)', text, re.MULTILINE | re.DOTALL)
    if not m:
        continue
    inv_text = m.group(1)

    # Split by "- id:" entry headers. The split prefix is skipped (everything before
    # the first entry — leading whitespace or commentary).
    parts = re.split(r'(?m)^\s{2}-\s+id:', inv_text)
    for block in parts[1:]:
        inv_count += 1
        id_m = re.match(r'\s*([A-Za-z0-9_-]+)', block)
        if not id_m:
            continue
        inv_id = id_m.group(1)

        spec_ref_m = re.search(r'spec_ref:\s*"?([^"\n]+)"?', block)
        if not spec_ref_m:
            skipped_no_spec_ref += 1
            continue

        spec_ref_val = spec_ref_m.group(1).strip()

        # Derive L4_domain_prefix from the spec_ref filename:
        #   specs/audit-log-l0.yaml#AUDIT-RECORD-001  →  audit-log
        #   specs/crud-security.yaml                  →  crud-security
        file_part = spec_ref_val.split('#', 1)[0].strip()
        base = pathlib.PurePath(file_part).stem  # drops .yaml extension
        # Trim "-l0" / "-l1" suffix for L4-domain specs (audit-log-l0 → audit-log).
        prefix = re.sub(r'-l\d+$', '', base)

        index[(prefix, inv_id)].append((spec_path.name, spec_ref_val))

violations = 0
for (prefix, inv_id), occurrences in sorted(index.items()):
    distinct_recipes = sorted({path for (path, _ref) in occurrences})
    if len(distinct_recipes) > 1:
        print(f"VIOLATION [cross_recipe_inv_uniqueness]: ({prefix}, {inv_id}) declared in ≥2 recipes:")
        for (path, ref) in occurrences:
            print(f"  - {path}  spec_ref={ref}")
        violations += 1

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
if violations == 0:
    print(f'{{"signal":"recipe.inv.cross_recipe_uniqueness_pass","value":{inv_count},"skipped_no_spec_ref":{skipped_no_spec_ref},"ts":"{ts}"}}')
    sys.exit(0)
else:
    print(f'{{"signal":"recipe.inv.cross_recipe_collision_count","value":{violations},"ts":"{ts}"}}')
    sys.exit(1)
PYEOF
