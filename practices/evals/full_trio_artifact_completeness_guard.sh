#!/usr/bin/env bash
# practices/evals/full_trio_artifact_completeness_guard.sh
# P2-22 (audit-seal) — closes the domain_mode-vs-artifact drift axis.
#
# THE INVARIANT: METHODOLOGY.md defines `domain_mode: full_trio` = Backend Trio AND
# Frontend Trio BOTH REQUIRED. So every spec that DECLARES full_trio MUST own the
# frontend-side artifacts that make the claim true:
#   - contracts/<stem>-*.yaml        (a UI/API contract for the domain)
#   - blueprints/<stem>-*.yaml       (a policy manifest for the domain)
#   - templates/L4/<stem>/           (the L4 vertical fork-receivers copy)
#
# Before this guard NO surface checked the axis: a spec could sit on
# `domain_mode: full_trio` while owning zero contract, zero blueprint, and no L4
# dir (a lie on the frontend axis). The pre-existing guards cover adjacent axes:
#   - domain_spec_trio_guard.sh          — forward: EXISTING L4/task domains carry
#                                          the Trio their mode requires (scoped to
#                                          the L4 domain SET, skips non-L4 specs).
#   - full_trio_spec_backend_or_exempt   — the BACKEND axis (full_trio spec must be
#                                          backend-enforced or rule-tier-exempt).
# This guard is the FRONTEND-axis obverse: every full_trio spec must actually have
# its frontend Trio artifacts on disk.
#
# Zero-scan guard: if NO spec declares full_trio, FAIL (non-vacuity — prevents a
# silent rename of the field from making this gate vacuously pass).
#
# Exit: 0 PASS · 1 a full_trio spec is missing an artifact / zero-scan · 2 usage.
#
# Usage:
#   bash practices/evals/full_trio_artifact_completeness_guard.sh
#   bash practices/evals/full_trio_artifact_completeness_guard.sh --root DIR
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        *) echo "full_trio_artifact_completeness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

python3 - "$REPO_ROOT" <<'PY'
import sys, os, re, glob
repo = sys.argv[1]

# stem derivation mirrors full_trio_spec_backend_or_exempt_guard.spec_base + ALIAS
ALIAS = {'crud-security': 'crud', 'auth-asvs': 'auth'}
def stem(fn):
    b = re.sub(r'-(l0|l1)\.yaml$', '', fn)
    b = re.sub(r'\.yaml$', '', b)
    b = b.replace('-frontend', '')
    return ALIAS.get(b, b)

def has(glb):
    return bool(glob.glob(os.path.join(repo, glb)))

# collect full_trio specs, grouped by stem (frontend + backend spec share a stem)
stems = {}  # stem -> [spec basenames]
for s in sorted(glob.glob(os.path.join(repo, 'specs/*.yaml'))):
    txt = open(s, encoding='utf-8', errors='ignore').read()
    if re.search(r'^\s*domain_mode:\s*"?full_trio"?\s*(#.*)?$', txt, re.M):
        st = stem(os.path.basename(s))
        stems.setdefault(st, []).append(os.path.basename(s))

scanned = len(stems)
violations = []
for st in sorted(stems):
    missing = []
    if not has(f'contracts/{st}-*.yaml') and not has(f'contracts/{st}.yaml'):
        missing.append(f'contracts/{st}-*.yaml')
    if not has(f'blueprints/{st}-*.yaml') and not has(f'blueprints/{st}.yaml'):
        missing.append(f'blueprints/{st}-*.yaml')
    if not os.path.isdir(os.path.join(repo, f'templates/L4/{st}')):
        missing.append(f'templates/L4/{st}/')
    if missing:
        specs = ', '.join(stems[st])
        violations.append(
            f"{st} (spec: {specs}): domain_mode: full_trio but missing "
            f"{', '.join(missing)} — full_trio REQUIRES a frontend Trio; "
            f"either scaffold the missing artifact(s) OR reclassify the spec to "
            f"domain_mode: backend_only")

print(f"[full_trio_artifact_completeness] scanned {scanned} full_trio stem(s)")

if scanned == 0:
    print("FAIL: ZERO_SCAN — no spec declares domain_mode: full_trio; the field may "
          "have been renamed/removed, making this gate vacuous — merge BLOCKED")
    sys.exit(1)

if violations:
    print(f"FAIL: {len(violations)} full_trio artifact gap(s):")
    for v in violations:
        print(f"  {v}")
    sys.exit(1)

print("PASS — every full_trio spec owns its frontend Trio (contract + blueprint + L4 dir)")
PY
rc=$?
exit $rc
