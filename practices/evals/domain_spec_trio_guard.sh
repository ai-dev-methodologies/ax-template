#!/usr/bin/env bash
# practices/evals/domain_spec_trio_guard.sh
# ax-plan (D1) hard gate — a domain may not exist without a complete Spec Trio.
#
# THE INVARIANT: every REAL L4 domain (a templates/L4/<domain>/ dir OR a backend test<Domain>
# task) MUST carry the Spec Trio its domain_mode requires — so code can never outrun the plan:
#   full_trio     -> specs/<d>-l0.yaml + contracts/<d>-*.yaml + blueprints/<d>-*.yaml
#   backend_only  -> specs/<d>-l0.yaml + blueprints/<d>-*.yaml            (policy required; API contract optional)
#   frontend_only -> specs/<d>-frontend-l0.yaml + contracts/<d>-ui*.yaml + blueprints/<d>-ui*.yaml
#   (missing/undeclared domain_mode is itself a gap -> FAIL)
#
# Scope is the L4 domain set ONLY — cross-cutting practice/recipe specs (caching, cors, …) have
# no endpoints and are governed by spec_item_verification_binding_guard, not by a Trio.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
while [ $# -gt 0 ]; do
    case "$1" in
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        *) echo "domain_spec_trio_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

python3 - "$REPO_ROOT" <<'PY'
import sys, os, re, glob
repo = sys.argv[1]

# domain set: templates/L4/<domain>/ dirs  ∪  backend test<Domain> gradle tasks
l4 = {os.path.basename(p.rstrip('/')) for p in glob.glob(os.path.join(repo, 'templates/L4/*/'))}
gradle = ''
gp = os.path.join(repo, 'backend/build.gradle.kts')
if os.path.exists(gp):
    gradle = open(gp, encoding='utf-8').read()
tasknorm = {t.lower() for t in re.findall(r'(?:register|create)\w*\("test([A-Za-z0-9]+)"', gradle)}

def has_task(dom):
    return dom.replace('-', '').lower() in tasknorm

domains = sorted(l4 | {d for d in l4} )
# add task-only domains (e.g. report-export, identity-verification) discovered from specs that have a task
for s in glob.glob(os.path.join(repo, 'specs/*-l0.yaml')):
    base = os.path.basename(s)[:-len('-l0.yaml')]
    if base.endswith('-frontend'):
        continue
    if has_task(base):
        domains = sorted(set(domains) | {base})

def first(glb):
    return bool(glob.glob(os.path.join(repo, glb)))

def domain_spec(d):
    # canonical specs/<d>-l0.yaml, else the domain's bespoke primary spec (auth-asvs-l1, crud-security…)
    p = os.path.join(repo, f'specs/{d}-l0.yaml')
    if os.path.exists(p):
        return p
    cands = [c for c in sorted(glob.glob(os.path.join(repo, f'specs/{d}-*.yaml')))
             if '-frontend' not in c and not c.endswith('-report.md')]
    return cands[0] if cands else None

violations = []
checked = 0
for d in sorted(set(domains)):
    spec = domain_spec(d)
    fspec = os.path.join(repo, f'specs/{d}-frontend-l0.yaml')
    if spec is None and not os.path.exists(fspec):
        violations.append((d, "no specs/<d>-*.yaml (domain has no compliance spec)"))
        continue
    src = spec if spec else fspec
    txt = open(src, encoding='utf-8').read()
    m = re.search(r'^\s*domain_mode:\s*"?([\w_]+)"?', txt, re.M)
    mode = m.group(1) if m else None
    checked += 1
    if mode is None:
        violations.append((d, "spec does not declare domain_mode (full_trio|backend_only|frontend_only)"))
        continue
    need = []
    if mode == 'full_trio':
        if not os.path.exists(spec): need.append(f'specs/{d}-l0.yaml')
        if not first(f'contracts/{d}-*.yaml'): need.append(f'contracts/{d}-*.yaml')
        if not first(f'blueprints/{d}-*.yaml'): need.append(f'blueprints/{d}-*.yaml')
    elif mode == 'backend_only':
        if not os.path.exists(spec): need.append(f'specs/{d}-l0.yaml')
        if not first(f'blueprints/{d}-*.yaml'): need.append(f'blueprints/{d}-*.yaml')
    elif mode == 'frontend_only':
        if not first(f'specs/{d}-frontend-l0.yaml'): need.append(f'specs/{d}-frontend-l0.yaml')
        if not first(f'contracts/{d}-ui*.yaml') and not first(f'contracts/{d}-*ui*.yaml'): need.append(f'contracts/{d}-ui*.yaml')
        if not first(f'blueprints/{d}-*ui*.yaml') and not first(f'blueprints/{d}-ui*.yaml'): need.append(f'blueprints/{d}-ui*.yaml')
    else:
        violations.append((d, f"unknown domain_mode '{mode}'"))
        continue
    for n in need:
        violations.append((d, f"[{mode}] missing {n}"))

print(f"[domain_spec_trio] checked {len(set(domains))} L4 domains")
if violations:
    print(f"FAIL: {len(violations)} Trio gap(s):")
    for d, why in violations:
        print(f"  {d}: {why}")
    sys.exit(1)
print("PASS — every L4 domain carries the Spec Trio its domain_mode requires")
PY
rc=$?
exit $rc
