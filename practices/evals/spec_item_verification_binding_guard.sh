#!/usr/bin/env bash
# practices/evals/spec_item_verification_binding_guard.sh
# ax-plan (R1+R2) hard gate — the pre-code planning teeth.
#
# THE INVARIANT (binary, bidirectional): every APPLICABLE item in EVERY specs/*-l0.yaml
# must declare a verification binding that MECHANICALLY RESOLVES. A plan item that no test /
# guard / rule verifies = a gap the catalog refuses to ship.
#
# A spec item resolves iff ONE of:
#   - explicit `verification: { mechanism: tag,   ref }`   -> ref is a real @Tag("<ref>") in test sources
#   - explicit `verification: { mechanism: guard, ref }`   -> ref is a real practices/evals/<ref>(.sh)
#   - explicit `verification: { mechanism: rule,  ref }`   -> ref is a real {practices,practices-react}/rules/<ref>.md
#   - explicit `verification: { mechanism: deferred, citation }` -> citation is non-empty (illustrative/backlog, owned)
#   - NO explicit `verification:` block  ->  IMPLICIT tag binding: @Tag("<item.id>") must exist
#
# This generalizes rule_tag_binding_guard (which binds RULE.verification.tag) to spec ITEMS,
# and accepts the three real mechanisms the 2026-06-05 audit found in use (tag / AOP-guard /
# practices-rule) so it never forces @Tag onto a correctly guard-verified concern (e.g. multi-tenant).
#
# Exit 0 = every applicable item resolves. Exit 1 = at least one item is unbound (prints worklist).
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SPECS_DIR="$REPO_ROOT/specs"
while [ $# -gt 0 ]; do
    case "$1" in
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        --specs-dir) SPECS_DIR="$2"; shift 2 ;;
        --specs-dir=*) SPECS_DIR="${1#--specs-dir=}"; shift ;;
        *) echo "spec_item_verification_binding_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ ! -d "$SPECS_DIR" ]; then
    echo "spec_item_verification_binding_guard: specs dir '$SPECS_DIR' not found — nothing to check"
    exit 0
fi

python3 - "$REPO_ROOT" "$SPECS_DIR" <<'PY'
import sys, os, re, glob
from collections import defaultdict
repo, specs_dir = sys.argv[1], sys.argv[2]

# --- resolvable references ------------------------------------------------------------------
tags = set()
TAG_RE = re.compile(r'@(?:[A-Za-z_][\w.]*\.)?Tag\("([^"]+)"\)')  # matches @Tag(..) AND @org.junit.jupiter.api.Tag(..)
for f in glob.glob(os.path.join(repo, 'backend/src/test/**/*.java'), recursive=True):
    try:
        tags |= set(TAG_RE.findall(open(f, encoding='utf-8', errors='ignore').read()))
    except OSError:
        pass
guards = {os.path.basename(p) for p in glob.glob(os.path.join(repo, 'practices/evals/*.sh'))}
guards_noext = {g[:-3] for g in guards}
rules = set()
rule_anchors = {}   # spec-item-id -> rule stem that governs it via `spec_ref: specs/<f>#<ID>`
for d in ('practices/rules', 'practices-react/rules'):
    for p in glob.glob(os.path.join(repo, d, '*.md')):
        stem = os.path.basename(p)[:-3]
        rules.add(stem)
        body = open(p, encoding='utf-8', errors='ignore').read()
        # an evidence-anchored catalog rule that spec_refs an item IS that item's verification mechanism
        for anchor in re.findall(r'spec_ref:\s*"?[^"\n#]+#([A-Za-z0-9-]+)"?', body):
            rule_anchors.setdefault(anchor, stem)

ITEM_TAG_RE = re.compile(r'^[A-Z0-9]+-(?:[A-Z0-9]+-)?[0-9]')   # DOMAIN-FAMILY-NNN or DOMAIN-N (RATELIMIT-1)

def items(txt):
    # each item is `- id: X` (quoted OR unquoted — frontend specs use unquoted IDs) and the lines
    # until the next sibling `- id:` at the same indent
    out = []
    for m in re.finditer(r'^(\s*)-\s*id:\s*(?:"([^"]+)"|([A-Za-z0-9_-]+))(.*?)(?=^\1-\s*id:|\Z)', txt, re.S | re.M):
        out.append((m.group(2) or m.group(3), m.group(4)))
    return out

def _verification_body(blk):
    # Linear, indent-bounded extraction of the `verification:` child block.
    # Replaces a catastrophic-backtracking regex: capture only the lines that
    # are MORE indented than the `verification:` key, stopping at the first
    # line whose indent is <= the key's (the next sibling YAML key) or EOF.
    lines = blk.split('\n')
    for i, ln in enumerate(lines):
        m = re.match(r'^(\s*)verification:\s*$', ln)
        if not m:
            continue
        base = len(m.group(1))
        child = []
        for nxt in lines[i + 1:]:
            if nxt.strip() == '':
                continue
            if (len(nxt) - len(nxt.lstrip())) <= base:
                break
            child.append(nxt)
        return '\n'.join(child)
    return None

def resolve(iid, blk):
    body = _verification_body(blk)
    if body is not None:
        mech = (re.search(r'mechanism:\s*"?(\w+)"?', body) or [None, ''])[1]
        ref = (re.search(r'ref:\s*"?([^"\n]+)"?', body) or [None, ''])[1].strip()
        if mech == 'tag':
            return (ref in tags), f"tag '{ref}' has no @Tag in tests"
        if mech == 'guard':
            hit = ref in guards or ref in guards_noext  # exact filename match (no substring false-pass)
            return hit, f"guard '{ref}' has no practices/evals match"
        if mech == 'rule':
            return (ref in rules), f"rule '{ref}' has no rules/*.md match"
        if mech == 'deferred':
            has_cite = bool(re.search(r'citation:\s*\S', body))
            return has_cite, "deferred binding without a citation"
        return False, f"unknown verification mechanism '{mech}'"
    # implicit binding: a real @Tag(id) test, OR an evidence-anchored rule that spec_refs this item
    if iid in tags:
        return True, ""
    if iid in rule_anchors:
        return True, ""
    return False, f"no @Tag(\"{iid}\"), no rule spec_ref, no explicit verification"

specs = sorted(glob.glob(os.path.join(specs_dir, '*-l0.yaml')))
unresolved = []
checked = 0
spec_tags = set()
for s in specs:
    base = os.path.basename(s)[:-len('-l0.yaml')]
    txt = open(s, encoding='utf-8').read()
    for iid, blk in items(txt):
        if iid and ITEM_TAG_RE.match(iid):
            spec_tags.add(iid)
        if re.search(r'applicable:\s*false', blk):
            continue
        checked += 1
        ok, why = resolve(iid, blk)
        if not ok:
            unresolved.append((base, iid, why))

# bidirectional advisory: item-style @Tags with NO owning spec item (orphan verification)
orphan = sorted(t for t in tags if ITEM_TAG_RE.match(t) and t not in spec_tags)

print(f"[spec_item_verification_binding] checked {checked} applicable items across {len(specs)} specs")
if orphan:
    print(f"[advisory] {len(orphan)} item-style @Tag(s) with no owning spec item: {', '.join(orphan[:12])}{'…' if len(orphan) > 12 else ''}")

if unresolved:
    by = defaultdict(list)
    for b, i, _ in unresolved:
        by[b].append(i)
    print(f"FAIL: {len(unresolved)} unbound item(s) across {len(by)} spec(s):")
    for b in sorted(by):
        print(f"  {b} ({len(by[b])}): {', '.join(by[b])}")
    sys.exit(1)

print("PASS — every applicable spec item resolves a verification binding (tag|guard|rule|deferred)")
PY
rc=$?
exit $rc
