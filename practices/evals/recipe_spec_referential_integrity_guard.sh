#!/usr/bin/env bash
# practices/evals/recipe_spec_referential_integrity_guard.sh — SP35 hard gate.
#
# Validates that every recipe-level spec in specs/recipes/*.yaml is internally
# consistent with the existing catalog on disk.
#
# Checks per spec file:
#   1. enabled_l4_domains: every entry resolves to templates/L4/<domain>/
#   2. l2_blocks_used:     every entry resolves to templates/L2/blocks/<name>.tsx
#   3. l3_pages_used:      every entry resolves to templates/L3/pages/<name>/
#   4. business_invariants[*]: every row has spec_ref OR rule_ref (not both required)
#      - spec_ref: "specs/<file>#<ID>" → specs/<file> must exist AND (if #ID given) ID must appear in file
#      - rule_ref: "practices/rules/<file>.md" → file must exist
#
# Emits one JSON observability line to stdout after all checks.
#
# Exit 0  — all references valid
# Exit 1  — one or more violations (details printed to stdout)
# Exit 2  — bad arguments
#
# Usage:
#   bash practices/evals/recipe_spec_referential_integrity_guard.sh
#   bash practices/evals/recipe_spec_referential_integrity_guard.sh --verbose
#   bash practices/evals/recipe_spec_referential_integrity_guard.sh --spec specs/recipes/crm-recipe-l0.yaml

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

VERBOSE=0
SINGLE_SPEC=""

while [ $# -gt 0 ]; do
    case "$1" in
        --verbose|-v) VERBOSE=1; shift ;;
        --spec) SINGLE_SPEC="$2"; shift 2 ;;
        --spec=*) SINGLE_SPEC="${1#--spec=}"; shift ;;
        *) echo "recipe_spec_referential_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Delegate all validation logic to Python for portability (bash 3/4 compat, no yq dependency)
python3 - "$REPO_ROOT" "$VERBOSE" "$SINGLE_SPEC" <<'PYEOF'
import sys
import os
import pathlib
import re

repo_root = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"
single_spec = sys.argv[3]

violations = 0
pass_count = 0

def vprint(*args):
    if verbose:
        print(*args)

def check_spec(spec_path: pathlib.Path) -> int:
    """Returns number of violations found in this spec."""
    local_violations = 0
    text = spec_path.read_text()

    # --- Parse YAML manually (avoid yq/PyYAML dependency; extract fields via regex) ---

    # Extract pattern
    m = re.search(r'^pattern:\s*(.+)$', text, re.MULTILINE)
    pattern_name = m.group(1).strip().strip('"') if m else spec_path.stem

    # --- Check enabled_l4_domains ---
    l4_block = re.search(r'enabled_l4_domains:\s*\n((?:  -[^\n]+\n)+)', text)
    if not l4_block:
        print(f"VIOLATION [{spec_path.name}]: missing enabled_l4_domains field")
        return 1

    l4_domains = re.findall(r'  -\s*(.+)', l4_block.group(1))
    for domain in l4_domains:
        domain = domain.strip().strip('"')
        domain_path = repo_root / "templates" / "L4" / domain
        if not domain_path.is_dir():
            print(f"VIOLATION [{spec_path.name}]: enabled_l4_domains '{domain}' → templates/L4/{domain}/ NOT FOUND")
            local_violations += 1
        else:
            vprint(f"  OK  enabled_l4_domains '{domain}' → templates/L4/{domain}/ ✓")

    # --- Check l2_blocks_used ---
    l2_block = re.search(r'l2_blocks_used:\s*\n((?:  -[^\n]+\n)+)', text)
    if l2_block:
        l2_blocks = re.findall(r'  -\s*(.+)', l2_block.group(1))
        for block in l2_blocks:
            block = block.strip().strip('"')
            block_path = repo_root / "templates" / "L2" / "blocks" / f"{block}.tsx"
            if not block_path.is_file():
                print(f"VIOLATION [{spec_path.name}]: l2_blocks_used '{block}' → templates/L2/blocks/{block}.tsx NOT FOUND")
                local_violations += 1
            else:
                vprint(f"  OK  l2_blocks_used '{block}' → templates/L2/blocks/{block}.tsx ✓")

    # --- Check l3_pages_used ---
    l3_block = re.search(r'l3_pages_used:\s*\n((?:  -[^\n]+\n)+)', text)
    if l3_block:
        l3_pages = re.findall(r'  -\s*(.+)', l3_block.group(1))
        for page in l3_pages:
            page = page.strip().strip('"')
            page_path = repo_root / "templates" / "L3" / "pages" / page
            if not page_path.is_dir():
                print(f"VIOLATION [{spec_path.name}]: l3_pages_used '{page}' → templates/L3/pages/{page}/ NOT FOUND")
                local_violations += 1
            else:
                vprint(f"  OK  l3_pages_used '{page}' → templates/L3/pages/{page}/ ✓")

    # --- Check business_invariants ---
    # Parse invariant blocks: find each "- id:" under business_invariants
    # We use a simple state machine: collect everything between business_invariants: and the next top-level key
    inv_section = re.search(r'^business_invariants:\s*\n(.*?)(?=^\w|\Z)', text, re.MULTILINE | re.DOTALL)
    if not inv_section:
        print(f"VIOLATION [{spec_path.name}]: missing business_invariants field")
        return local_violations + 1

    inv_text = inv_section.group(1)
    inv_ids = re.findall(r'\bid:\s*(\S+)', inv_text)

    if not inv_ids:
        print(f"VIOLATION [{spec_path.name}]: business_invariants has no entries")
        local_violations += 1
    else:
        # For each invariant, check that spec_ref or rule_ref is present and resolves
        # Split invariant blocks by "- id:"
        inv_blocks = re.split(r'(?m)^\s{2}-\s+id:', inv_text)
        for i, block in enumerate(inv_blocks[1:], 1):  # skip empty first
            inv_id = re.match(r'\s*(\S+)', block)
            inv_id_str = inv_id.group(1) if inv_id else f"invariant#{i}"

            has_spec_ref = bool(re.search(r'spec_ref:', block))
            has_rule_ref = bool(re.search(r'rule_ref:', block))

            if not has_spec_ref and not has_rule_ref:
                print(f"VIOLATION [{spec_path.name}]: invariant {inv_id_str} has neither spec_ref nor rule_ref")
                local_violations += 1
                continue

            # Validate spec_ref resolution
            if has_spec_ref:
                spec_ref_m = re.search(r'spec_ref:\s*"?([^"\n]+)"?', block)
                if spec_ref_m:
                    spec_ref_val = spec_ref_m.group(1).strip()
                    # Format: specs/<file>.yaml or specs/<file>.yaml#<ID>
                    parts = spec_ref_val.split('#', 1)
                    spec_file_rel = parts[0].strip()
                    spec_id = parts[1].strip() if len(parts) > 1 else None
                    spec_file_abs = repo_root / spec_file_rel
                    if not spec_file_abs.is_file():
                        print(f"VIOLATION [{spec_path.name}]: invariant {inv_id_str} spec_ref '{spec_file_rel}' NOT FOUND on disk")
                        local_violations += 1
                    else:
                        if spec_id:
                            # Check that the ID appears in the spec file
                            spec_content = spec_file_abs.read_text()
                            if spec_id not in spec_content:
                                print(f"VIOLATION [{spec_path.name}]: invariant {inv_id_str} spec_ref ID '{spec_id}' not found in {spec_file_rel}")
                                local_violations += 1
                            else:
                                vprint(f"  OK  invariant {inv_id_str} spec_ref '{spec_ref_val}' ✓")
                        else:
                            vprint(f"  OK  invariant {inv_id_str} spec_ref '{spec_ref_val}' (no ID fragment) ✓")

            # Validate rule_ref resolution
            if has_rule_ref:
                rule_ref_m = re.search(r'rule_ref:\s*"?([^"\n]+)"?', block)
                if rule_ref_m:
                    rule_ref_val = rule_ref_m.group(1).strip()
                    rule_file_abs = repo_root / rule_ref_val
                    if not rule_file_abs.is_file():
                        print(f"VIOLATION [{spec_path.name}]: invariant {inv_id_str} rule_ref '{rule_ref_val}' NOT FOUND on disk")
                        local_violations += 1
                    else:
                        vprint(f"  OK  invariant {inv_id_str} rule_ref '{rule_ref_val}' ✓")

    # --- Check evidence: at least 1 external entry ---
    external_evidences = re.findall(r'provenance_class:\s*external', text)
    if not external_evidences:
        print(f"VIOLATION [{spec_path.name}]: no external evidence entry found (requires ≥1 provenance_class: external)")
        local_violations += 1
    else:
        vprint(f"  OK  evidence: {len(external_evidences)} external citation(s) ✓")

    # --- Check internal_design entries have rationale ---
    internal_blocks = re.findall(r'provenance_class:\s*internal_design(.*?)(?=provenance_class:|\Z)', text, re.DOTALL)
    for ib in internal_blocks:
        if not re.search(r'rationale:\s*\S+', ib):
            print(f"VIOLATION [{spec_path.name}]: an internal_design evidence block is missing 'rationale:' field")
            local_violations += 1

    return local_violations


# Collect spec files to check
if single_spec:
    spec_path = pathlib.Path(single_spec)
    if not spec_path.is_absolute():
        spec_path = repo_root / spec_path
    spec_files = [spec_path] if spec_path.is_file() else []
    if not spec_files:
        print(f"recipe_spec_referential_integrity_guard: spec file not found: {single_spec}")
        sys.exit(2)
else:
    recipes_spec_dir = repo_root / "specs" / "recipes"
    spec_files = sorted(recipes_spec_dir.glob("*-recipe-l0.yaml")) if recipes_spec_dir.is_dir() else []

if not spec_files:
    print("recipe_spec_referential_integrity_guard: no recipe specs found in specs/recipes/ — nothing to check")
    sys.exit(0)

import datetime
for spec_path in spec_files:
    vprint(f"\nChecking {spec_path.name} ...")
    v = check_spec(spec_path)
    violations += v
    if v == 0:
        pass_count += 1
        if not verbose:
            print(f"PASS [{spec_path.name}]")

# Emit observability signal
ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
if violations == 0:
    print(f'{{"signal":"recipe.spec.referential_integrity_pass_count","value":{pass_count},"ts":"{ts}"}}')
else:
    print(f'{{"signal":"recipe.spec.referential_integrity_violation_count","value":{violations},"ts":"{ts}"}}')

sys.exit(0 if violations == 0 else 1)
PYEOF
