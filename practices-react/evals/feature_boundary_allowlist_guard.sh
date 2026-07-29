#!/usr/bin/env bash
# practices-react/evals/feature_boundary_allowlist_guard.sh
# Frontend decomposition allowlist CI validation guard.
# Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md §5.
# Backend analog: practices/evals/aggregate_boundary_allowlist_guard.sh.
#
# Validates practices-react/feature_boundary_allowlist.yaml — the schema-checked
# escape-hatch surface for the frontend decomposition ESLint rules. A loose
# allowlist would let a grandfather deep-import become a permanent escape hatch, so
# this guard mechanically enforces:
#
#   1. SCHEMA — top-level keys are exactly {shared_layers: list, published_api: map,
#      exceptions: list}; every exception is a map with ALL required keys
#      (from, to, kind, owner, rationale, expiry, remediation_ticket).
#   2. RESOLVE — every exception from/to and every published_api entry resolves to a
#      real path under frontend/src (a file, a dir, or a dir with an index.*).
#   3. NO WILDCARDS outside shared_layers.
#   4. EXPIRY — an exception whose `expiry` (YYYY-MM-DD) is in the past FAILs.
#
# Exit: 0 PASS · 1 violation · 2 usage/setup error (or SKIP when python3/PyYAML absent).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ALLOWLIST="$REPO_ROOT/practices-react/feature_boundary_allowlist.yaml"
SRC_DIR="$REPO_ROOT/frontend/src"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; ALLOWLIST="$REPO_ROOT/practices-react/feature_boundary_allowlist.yaml"; SRC_DIR="$REPO_ROOT/frontend/src"; shift 2 ;;
        --file) ALLOWLIST="$2"; shift 2 ;;
        --file=*) ALLOWLIST="${1#--file=}"; shift ;;
        --src) SRC_DIR="$2"; shift 2 ;;
        --src=*) SRC_DIR="${1#--src=}"; shift ;;
        *) echo "feature_boundary_allowlist_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "feature_boundary_allowlist_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

if [ ! -f "$ALLOWLIST" ]; then
    echo "feature_boundary_allowlist_guard: missing $ALLOWLIST" >&2
    exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo "feature_boundary_allowlist_guard: SKIP — python3 not on PATH"
    exit 0
fi

TODAY="$(date +%F)"

python3 - "$ALLOWLIST" "$SRC_DIR" "$TODAY" <<'PYEOF'
import sys, os, re

allowlist_path, src_dir, today = sys.argv[1:4]
try:
    import yaml
except ImportError:
    # Cannot verify ⇒ exit 2. Never 0: an unverified pass is the defect, not a courtesy.
    print("feature_boundary_allowlist_guard: BLOCK — cannot verify: PyYAML required", file=sys.stderr)
    sys.exit(2)

with open(allowlist_path) as f:
    try:
        doc = yaml.safe_load(f) or {}
    except yaml.YAMLError as e:
        print(f"VIOLATION: allowlist is not valid YAML: {e}", file=sys.stderr); sys.exit(1)

violations = []
def fail(m): violations.append(m)

def resolves(spec):
    """A `@/x` (or src-relative) spec resolves to a real file/dir/index under frontend/src."""
    if not isinstance(spec, str) or not spec:
        return False
    rel = spec[2:] if spec.startswith("@/") else spec
    base = os.path.join(src_dir, rel)
    if os.path.exists(base):
        return True
    for ext in (".ts", ".tsx", ".js", ".jsx"):
        if os.path.isfile(base + ext):
            return True
        if os.path.isfile(os.path.join(base, "index" + ext)):
            return True
    return False

if not isinstance(doc, dict):
    print("VIOLATION: allowlist root must be a mapping", file=sys.stderr); sys.exit(1)

allowed_keys = {"shared_layers", "published_api", "exceptions"}
extra = set(doc.keys()) - allowed_keys
if extra:
    fail(f"unexpected top-level key(s): {sorted(extra)} (allowed: {sorted(allowed_keys)})")

sl = doc.get("shared_layers", [])
if not isinstance(sl, list):
    fail("shared_layers must be a list")
else:
    for e in sl:
        if not isinstance(e, str):
            fail(f"shared_layers entry must be a string: {e!r}")

pa = doc.get("published_api", {})
if not isinstance(pa, dict):
    fail("published_api must be a mapping (feature -> [barrel, ...])")
else:
    for feature, entries in pa.items():
        if not isinstance(entries, list):
            fail(f"published_api['{feature}'] must be a list"); continue
        for e in entries:
            if not isinstance(e, str) or "*" in e:
                fail(f"published_api['{feature}'] entry '{e}' must be an exact path (no wildcard)"); continue
            if not resolves(e):
                fail(f"published_api['{feature}'] '{e}' does not resolve under frontend/src")

REQUIRED = ["from", "to", "kind", "owner", "rationale", "expiry", "remediation_ticket"]
ex = doc.get("exceptions", [])
if not isinstance(ex, list):
    fail("exceptions must be a list")
else:
    for i, entry in enumerate(ex):
        if not isinstance(entry, dict):
            fail(f"exceptions[{i}] must be a mapping"); continue
        for k in REQUIRED:
            if k not in entry or entry[k] in (None, ""):
                fail(f"exceptions[{i}] missing required key '{k}'")
        for label in ("from", "to"):
            v = entry.get(label)
            if isinstance(v, str) and v:
                if "*" in v:
                    fail(f"exceptions[{i}].{label} '{v}' must be an exact path (no wildcard)")
                elif not resolves(v):
                    fail(f"exceptions[{i}].{label} '{v}' does not resolve under frontend/src")
        expiry = entry.get("expiry")
        if expiry is not None:
            es = str(expiry)
            if not re.match(r"^\d{4}-\d{2}-\d{2}$", es):
                fail(f"exceptions[{i}].expiry '{es}' must be YYYY-MM-DD")
            elif es < today:
                fail(f"exceptions[{i}].expiry '{es}' has passed (today {today}) — re-decide or remove (ticket {entry.get('remediation_ticket')})")

if violations:
    print(f"feature_boundary_allowlist_guard: {len(violations)} violation(s) — BLOCKED", file=sys.stderr)
    for v in violations:
        print(f"  VIOLATION: {v}", file=sys.stderr)
    sys.exit(1)

n_pa = sum(len(v) for v in pa.values()) if isinstance(pa, dict) else 0
print(f"feature_boundary_allowlist_guard: OK — {len(sl)} shared layer(s), {n_pa} published_api barrel(s), {len(ex)} exception(s), all resolve & unexpired")
sys.exit(0)
PYEOF
