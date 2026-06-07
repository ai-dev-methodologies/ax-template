#!/usr/bin/env bash
# skills/ax-plan/scripts/emit-red-stubs.sh — ax-plan (ultragoal G005) Step 8.
#
# Emits 1:1 RED @Tag test stubs for a domain's compliance spec: one FAILING
# @Test per `applicable: true` spec item, tagged with BOTH the UPPERCASE domain
# tag (so the method lands in `test<Domain>` via includeTags) AND the exact item
# id (so spec_item_verification_binding_guard.sh resolves the item via its
# implicit `iid ∈ tags` path). A RED stub is still a BOUND item — RED is the TDD
# start state, not a failure of the plan.
#
# Also emits the MANDATORY <Domain>ViolationProofTest.java stub (plain reflection,
# no Spring context) because l4_domain_reachability_guard fails the build if an
# entity-bearing domain ships without one.
#
# Usage:   bash skills/ax-plan/scripts/emit-red-stubs.sh <domain-kebab>
# Example: bash skills/ax-plan/scripts/emit-red-stubs.sh api-key
#
# Safe by default: refuses to overwrite an existing ComplianceTest (a dev may
# have turned stubs green). Remove the file to regenerate.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
    echo "emit-red-stubs: usage: emit-red-stubs.sh <domain-kebab>" >&2
    exit 2
fi

SPEC="$REPO_ROOT/specs/${DOMAIN}-l0.yaml"
SPEC_FE="$REPO_ROOT/specs/${DOMAIN}-frontend-l0.yaml"
if [ -f "$SPEC_FE" ] && [ ! -f "$SPEC" ]; then
    echo "emit-red-stubs: '${DOMAIN}' has only a *-frontend-l0.yaml (no backend spec) — bind its UI"
    echo "  items with verification: { mechanism: rule, ref: <practices-react-rule> }, not a Java @Tag"
    echo "  stub. (mechanism:rule FE binding is used across specs/*-frontend-l0.yaml, e.g."
    echo "  audit-log-frontend; practices-frontend-l0.yaml is the canonical domain_mode: frontend_only.)"
    echo "  Nothing for this script to emit."
    exit 0
fi
if [ ! -f "$SPEC" ]; then
    echo "emit-red-stubs: SKELETON_MISSING — $SPEC not found. Run /ax-scaffold ${DOMAIN} first." >&2
    exit 3
fi

python3 - "$REPO_ROOT" "$DOMAIN" "$SPEC" <<'PY'
import sys, os, re

repo_root, domain, spec_path = sys.argv[1], sys.argv[2], sys.argv[3]

# Derive naming conventions from the kebab domain name.
#   api-key -> DOMAIN_TAG=API_KEY, pkg=apikey, Class=ApiKey
parts = re.split(r'[-_]', domain)
domain_tag = "_".join(p.upper() for p in parts)          # API_KEY
pkg = "".join(p.lower() for p in parts)                   # apikey
pascal = "".join(p[:1].upper() + p[1:] for p in parts)   # ApiKey

txt = open(spec_path, encoding='utf-8').read()

# Parse items: id + first verification-mechanism (if any) + first notes line.
# Item block = '  - id: X' .. next sibling '  - id:' (same as the binding guard).
ITEM = re.compile(r'^(\s*)-\s*id:\s*(?:"([^"]+)"|([A-Za-z0-9_-]+))(.*?)(?=^\1-\s*id:|\Z)', re.S | re.M)
ITEM_ID_RE = re.compile(r'^[A-Z0-9]+-(?:[A-Z0-9]+-)?[0-9]')  # binding guard's id shape

emitted = []
skipped_bound = []
skipped_nonapplicable = []
skipped_badid = []
for m in ITEM.finditer(txt):
    iid = m.group(2) or m.group(3)
    blk = m.group(4)
    if not iid:
        continue
    if re.search(r'applicable:\s*false', blk):
        skipped_nonapplicable.append(iid); continue
    if not ITEM_ID_RE.match(iid):
        skipped_badid.append(iid); continue
    # Already bound by an explicit verification block (guard/rule/deferred/tag)?
    if re.search(r'^\s+verification:\s*$', blk, re.M):
        skipped_bound.append(iid); continue
    note = ""
    nm = re.search(r'notes:\s*[>|]?\s*"?(.+)', blk)
    if nm:
        note = nm.group(1).strip().strip('"').split('\n')[0][:90]
    if not note:
        rm = re.search(r'requirement:\s*"?(.+)', blk)
        note = (rm.group(1).strip().strip('"')[:90]) if rm else "implement this item"
    emitted.append((iid, note))

if not emitted and not skipped_bound:
    print("emit-red-stubs: no applicable, unbound items found in", os.path.relpath(spec_path, repo_root))
    print("  (skipped non-applicable:", len(skipped_nonapplicable), "| bad-id:", skipped_badid, ")")
    sys.exit(0 if not skipped_badid else 1)

test_dir = os.path.join(repo_root, "backend", "src", "test", "java", "com", "ax", "template", "authblueprint", pkg)
os.makedirs(test_dir, exist_ok=True)
comp_path = os.path.join(test_dir, f"{pascal}ComplianceTest.java")
vpt_path = os.path.join(test_dir, f"{pascal}ViolationProofTest.java")

if os.path.exists(comp_path):
    print(f"emit-red-stubs: {os.path.relpath(comp_path, repo_root)} already exists — refusing to overwrite.")
    print("  Remove it to regenerate, or add new item methods by hand. (dev may have turned stubs green.)")
    sys.exit(0)

def method_name(iid, idx):
    return "plan_" + re.sub(r'[^A-Za-z0-9]', '_', iid) + f"_item{idx}"

methods = []
for i, (iid, note) in enumerate(emitted, 1):
    safe_note = note.replace('"', '\\"').replace('\n', ' ')
    methods.append(f'''    @Test
    @Tag("{domain_tag}")
    @Tag("{iid}")
    void {method_name(iid, i)}() {{
        // RED stub (ax-plan): item {iid} is BOUND (guard counts @Tag) but unimplemented.
        org.junit.jupiter.api.Assertions.fail("RED: implement {iid} — {safe_note}");
    }}''')

comp = f'''package com.ax.template.authblueprint.{pkg};

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {pascal}ComplianceTest — ax-plan RED stubs ({len(emitted)} items) for specs/{domain}-l0.yaml.
 *
 * Generated by skills/ax-plan/scripts/emit-red-stubs.sh. Every method is a FAILING
 * (RED) stub carrying @Tag("{domain_tag}") (so it runs under ./gradlew test{pascal})
 * and @Tag("ITEM-ID") (so spec_item_verification_binding_guard.sh counts the item as
 * BOUND). RED is the TDD start state — dev implements until each turns GREEN.
 * Replace the fail(...) bodies with real RestAssured black-box assertions; do NOT
 * delete the @Tag("ITEM-ID") line (it is the binding anchor).
 */
@Tag("{domain_tag}")
class {pascal}ComplianceTest {{

{chr(10).join(methods)}
}}
'''
open(comp_path, 'w', encoding='utf-8').write(comp)

# Mandatory ViolationProofTest stub (plain reflection, no Spring context).
if not os.path.exists(vpt_path):
    vpt = f'''package com.ax.template.authblueprint.{pkg};

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {pascal}ViolationProofTest — ax-plan RED stub. l4_domain_reachability_guard requires
 * an entity-bearing domain to ship a *ViolationProofTest. Replace this stub with real
 * structural assertions (reflection over the entity: immutable columns / no public
 * setter / id updatable=false) that would FAIL if a future refactor relaxed the
 * domain's invariant. Plain reflection — NO @SpringBootTest.
 */
@Tag("{domain_tag}")
class {pascal}ViolationProofTest {{

    @Test
    @Tag("{domain_tag}")
    void violation_proofStub() {{
        // RED stub: write at least one structural negative once the entity exists.
        org.junit.jupiter.api.Assertions.fail(
            "RED: add a {pascal} structural ViolationProof (immutable column / no public setter).");
    }}
}}
'''
    open(vpt_path, 'w', encoding='utf-8').write(vpt)

print(f"emit-red-stubs: wrote {os.path.relpath(comp_path, repo_root)} ({len(emitted)} RED @Tag stubs)")
print(f"emit-red-stubs: wrote {os.path.relpath(vpt_path, repo_root)} (ViolationProof RED stub)")
if skipped_bound:
    print(f"  skipped {len(skipped_bound)} already-bound item(s): {', '.join(skipped_bound[:6])}{'...' if len(skipped_bound)>6 else ''}")
if skipped_badid:
    print(f"  WARNING {len(skipped_badid)} item id(s) do NOT match the binding-guard regex (will be silently skipped by the guard): {', '.join(skipped_badid)}")
print()
print(f"  Next: register the gradle task, then run ./gradlew test{pascal} (expect RED).")
print(f"    tasks.register<Test>(\"test{pascal}\") {{ useJUnitPlatform {{ includeTags(\"{domain_tag}\") }} ; group = \"verification\" }}")
PY
