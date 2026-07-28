#!/usr/bin/env bash
# practices/evals/full_trio_artifact_completeness_guard.sh
# P2-22 (audit-seal) — closes the domain_mode-vs-artifact drift axis.
#
# THE INVARIANT: METHODOLOGY.md defines `domain_mode: full_trio` = Backend Trio AND
# Frontend Trio BOTH REQUIRED. So every spec that DECLARES full_trio MUST own the
# frontend-side artifacts that make the claim true:
#   - contracts/<stem>-*.yaml        (a UI/API contract for the domain)
#   - blueprints/<stem>-*.yaml       (a policy manifest for the domain)
#   - templates/L4/<stem>/           (the L4 vertical fork-receivers copy, MUST
#                                     contain a real, non-empty .tsx route/page —
#                                     P1-2 fix: os.path.isdir() alone is vacuous,
#                                     an empty or .gitkeep-only directory is not a
#                                     frontend Trio and must not satisfy this axis)
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

# P1-2 (cross-family review, xhigh) — os.path.isdir() alone is vacuous: a
# fork-copy vertical reduced to an empty directory (or one holding only a
# .gitkeep) still satisfies isdir() and the domain_mode: full_trio claim would
# pass while shipping ZERO frontend artifact. A full_trio spec promises a real
# Frontend Trio (a route/page a fork-receiver can actually copy), so the L4 leg
# must contain at least one non-empty .tsx file — matching what every healthy
# full_trio vertical on disk actually ships (min observed: 2 .tsx files, e.g.
# search/, feature-flags/; see docs/BROADLEAF-ABSORPTION.md sibling verticals).
def l4_frontend_gap(st):
    base = os.path.join(repo, f'templates/L4/{st}')
    if not os.path.isdir(base):
        return f'templates/L4/{st}/ (directory missing)'
    tsx_files = []
    for root, _dirs, files in os.walk(base):
        for fn in files:
            if fn.endswith('.tsx'):
                tsx_files.append(os.path.join(root, fn))
    has_nonempty_tsx = False
    for fp in tsx_files:
        try:
            if open(fp, encoding='utf-8', errors='ignore').read().strip():
                has_nonempty_tsx = True
                break
        except OSError:
            continue
    if not has_nonempty_tsx:
        return (f'templates/L4/{st}/**/*.tsx (directory exists but has no '
                f'non-empty .tsx — an empty/placeholder dir is not a frontend Trio)')
    return None

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
    l4_gap = l4_frontend_gap(st)
    if l4_gap:
        missing.append(l4_gap)
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
