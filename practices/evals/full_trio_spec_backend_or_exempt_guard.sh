#!/usr/bin/env bash
# practices/evals/full_trio_spec_backend_or_exempt_guard.sh
# ax G003 enforcement-coverage hardening — closes the ONE escape class the
# existing guard suite did not mechanically catch.
#
# THE INVARIANT (the REVERSE of domain_spec_trio_guard): every spec that DECLARES
# `domain_mode: full_trio` must EITHER be actually backend-enforced OR be honestly
# listed as a rule-tier exemption. domain_spec_trio_guard only checks the forward
# direction — "every EXISTING domain (L4 dir ∪ backend test task) carries the Trio
# its mode requires". It never checks that a full_trio SPEC has a backing backend
# domain. So a spec can claim `full_trio` while its invariant is verified at
# RULE/REVIEW tier only (bound via rule_verification_binding, no runtime backend
# gate) — weaker than the binary-test domains, and the full_trio claim is dishonest.
#
# A full_trio spec is BACKEND-ENFORCED iff ANY of:
#   - templates/L4/<base>/ dir exists, OR
#   - its items are covered by a per-domain `./gradlew test<X>` task — resolved
#     ACCURATELY (NOT by naive base-name): grep the spec's item IDs in the backend
#     test tree, take the @Tag(s) of the test class(es) that reference them, and
#     check that tag is in a per-domain task's includeTags(...). Many task names
#     are abbreviated (decision-governance→testDecisionGov, order→testCommerceOrder,
#     deadline-obligation→testObligation), so the item-tag link is authoritative.
#
# A full_trio spec that is NEITHER backend-enforced NOR listed in
# practices/evals/ruletier_full_trio_allowlist.yaml (with a non-empty rationale)
# ⇒ this guard BLOCKS. An allowlist entry that names a spec which IS actually
# backend-enforced (or does not exist / is not full_trio) is a STALE/dead entry ⇒
# this guard BLOCKS too — exemption is for genuine rule-tier escapes only.
#
# Exit: 0 PASS · 1 a full_trio spec is unenforced+unexempted, or a stale exemption · 2 usage.
#
# Usage:
#   bash practices/evals/full_trio_spec_backend_or_exempt_guard.sh
#   bash practices/evals/full_trio_spec_backend_or_exempt_guard.sh --repo-root DIR
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ALLOWLIST=""
while [ $# -gt 0 ]; do
    case "$1" in
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        --allowlist) ALLOWLIST="$2"; shift 2 ;;
        --allowlist=*) ALLOWLIST="${1#--allowlist=}"; shift ;;
        *) echo "full_trio_spec_backend_or_exempt_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -z "$ALLOWLIST" ] && ALLOWLIST="$REPO_ROOT/practices/evals/ruletier_full_trio_allowlist.yaml"

python3 - "$REPO_ROOT" "$ALLOWLIST" <<'PY'
import sys, os, re, glob
repo, allowlist_path = sys.argv[1], sys.argv[2]

# ── 1. per-domain task tag union (restricted to verification-checklist per-domain-tests when present) ──
gradle = ''
gp = os.path.join(repo, 'backend/build.gradle.kts')
if os.path.exists(gp):
    gradle = open(gp, encoding='utf-8', errors='ignore').read()

task_tags = {}  # testX -> set(tags)
for m in re.finditer(r'tasks\.register<Test>\("(test[A-Za-z0-9]+)"\)\s*\{(.*?)\n\}', gradle, re.S):
    name, body = m.group(1), m.group(2)
    tagset = set()
    for blk in re.findall(r'includeTags\(\s*((?:"[^"]+"\s*,?\s*)+)\)', body, re.S):
        tagset |= set(re.findall(r'"([^"]+)"', blk))
    task_tags[name] = tagset

checklist_path = os.path.join(repo, 'practices/verification-checklist.yaml')
per_domain = None
if os.path.exists(checklist_path):
    cl = open(checklist_path, encoding='utf-8', errors='ignore').read()
    per_domain = set('test' + t for t in re.findall(r'gradlew test([A-Za-z0-9]+)', cl))

per_domain_tag_union = set()
task_by_tag = {}
for tname, tagset in task_tags.items():
    if per_domain is not None and tname not in per_domain:
        continue
    for tg in tagset:
        per_domain_tag_union.add(tg)
        task_by_tag.setdefault(tg, set()).add(tname)

# ── 2. L4 dirs ──
l4 = {os.path.basename(p.rstrip('/')) for p in glob.glob(os.path.join(repo, 'templates/L4/*/'))}
ALIAS = {'crud-security': 'crud', 'auth-asvs': 'auth'}

# ── 3. @Tag map over backend test sources ──
TAG_RE = re.compile(r'@(?:[A-Za-z_][\w.]*\.)?Tag\("([^"]+)"\)')
file_tags = {}
file_text = {}
for f in glob.glob(os.path.join(repo, 'backend/src/test/**/*.java'), recursive=True):
    try:
        txt = open(f, encoding='utf-8', errors='ignore').read()
    except OSError:
        continue
    file_text[f] = txt
    file_tags[f] = set(TAG_RE.findall(txt))

# ── 4. allowlist (filename -> rationale) ──
allow = {}
allow_dups = []
if os.path.exists(allowlist_path):
    atxt = open(allowlist_path, encoding='utf-8', errors='ignore').read()
    # entries: `- spec: <file>` followed (within the block) by `rationale: <text>`
    for m in re.finditer(r'^\s*-\s*spec:\s*"?([^"\n]+?)"?\s*$(.*?)(?=^\s*-\s*spec:|\Z)',
                         atxt, re.S | re.M):
        spec = m.group(1).strip()
        body = m.group(2)
        rm = re.search(r'rationale:\s*"?(.+?)"?\s*$', body, re.M)
        rationale = (rm.group(1).strip() if rm else '')
        if spec in allow:
            allow_dups.append(spec)
        allow[spec] = rationale

def spec_base(fn):
    b = re.sub(r'-(l0|l1)\.yaml$', '', fn)
    b = re.sub(r'\.yaml$', '', b)
    b = b.replace('-frontend', '')
    return b

def item_ids(txt):
    mi = re.search(r'^items:', txt, re.M)
    body = txt[mi.start():] if mi else txt
    return re.findall(r'^\s*-\s*id:\s*"?([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)"?', body, re.M)

def enforced(fn, txt):
    """Return (True, reason) if backend-enforced, else (False, '')."""
    base = spec_base(fn)
    if base in l4:
        return True, f"L4 dir templates/L4/{base}/"
    alias = ALIAS.get(base, base)
    if alias in l4:
        return True, f"L4 dir templates/L4/{alias}/"
    # per-domain task coverage via item-id -> test class -> @Tag
    ids = item_ids(txt)
    covering_tags = set()
    if ids:
        # whole-token match (NOT substring): an item id must appear delimited by non-id chars,
        # so a short id can't be falsely "enforced" by being a substring of a longer id-like token
        # in an unrelated backend test (false-negative that would let a real escape skip the allowlist).
        id_res = [re.compile(r'(?<![A-Za-z0-9_-])' + re.escape(iid) + r'(?![A-Za-z0-9_-])') for iid in ids]
        for f, ftxt in file_text.items():
            if any(r.search(ftxt) for r in id_res):
                covering_tags |= file_tags[f]
    hit = covering_tags & per_domain_tag_union
    if hit:
        tasks = set()
        for tg in hit:
            tasks |= task_by_tag.get(tg, set())
        return True, f"per-domain task tag {sorted(hit)} via {sorted(tasks)}"
    return False, ''

# ── classify every full_trio spec ──
full_trio = []
for s in sorted(glob.glob(os.path.join(repo, 'specs/*.yaml'))):
    txt = open(s, encoding='utf-8', errors='ignore').read()
    if re.search(r'^\s*domain_mode:\s*"?full_trio"?\s*(#.*)?$', txt, re.M):
        full_trio.append((os.path.basename(s), txt))

violations = []
enforced_count = 0
exempt_count = 0
exempt_specs = set()
for fn, txt in full_trio:
    is_enf, why = enforced(fn, txt)
    in_allow = fn in allow
    if is_enf:
        enforced_count += 1
        if in_allow:
            violations.append(
                f"{fn}: STALE exemption — spec IS backend-enforced ({why}); "
                f"remove it from ruletier_full_trio_allowlist.yaml (exempt rule-tier escapes only)")
        continue
    # not backend-enforced
    if in_allow:
        if not allow[fn]:
            violations.append(f"{fn}: rule-tier exemption present but rationale is EMPTY (one-line rationale required)")
        else:
            exempt_count += 1
            exempt_specs.add(fn)
        continue
    violations.append(
        f"{fn}: full_trio but NEITHER backend-enforced (no templates/L4/<base>/ dir, "
        f"no per-domain test task covering its item @Tags) NOR exempted — add a backend "
        f"domain (test task / L4 dir) OR list it in ruletier_full_trio_allowlist.yaml with a rationale")

# dead allowlist entries: name a spec that is not a full_trio spec (missing / wrong mode)
full_trio_names = {fn for fn, _ in full_trio}
for spec in sorted(allow):
    if spec not in full_trio_names:
        violations.append(f"{spec}: dead exemption — not a full_trio spec under specs/ (remove from allowlist)")
for spec in allow_dups:
    violations.append(f"{spec}: duplicate allowlist entry")

print(f"[full_trio_spec_backend_or_exempt] {len(full_trio)} full_trio spec(s): "
      f"{enforced_count} backend-enforced, {exempt_count} rule-tier-exempt")
if violations:
    print(f"FAIL: {len(violations)} violation(s):")
    for v in violations:
        print(f"  {v}")
    sys.exit(1)
print("PASS — every full_trio spec is backend-enforced OR honestly exempted as rule-tier")
PY
rc=$?
exit $rc
