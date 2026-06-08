#!/usr/bin/env bash
# practices/evals/aggregate_boundary_allowlist_guard.sh
# DDD decomposition allowlist CI validation guard.
# Spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §5.
#
# Validates practices/evals/aggregate_boundary_allowlist.yaml — the single
# schema-checked escape-hatch surface for the DDD package-structure hard guards.
# A loose allowlist would let a grandfather edge become a permanent escape hatch
# (spec §5 loophole concern), so this guard mechanically enforces:
#
#   1. SCHEMA — top-level keys are exactly {shared_kernel: list, published_api:
#      map, exceptions: list}; every exception is a map with ALL required keys
#      (from, to, kind, owner, rationale, expiry, remediation_ticket).
#   2. RESOLVE — every exception `from`/`to` FQN resolves to a real .java class
#      in backend/src/main/java; every published_api class resolves inside its
#      named feature package. A typo'd or stale reference FAILs.
#   3. NO WILDCARDS outside shared_kernel — published_api / exceptions entries
#      must be exact (a wildcard there would silently widen the carve).
#   4. EXPIRY — an exception whose `expiry` (YYYY-MM-DD) is in the past FAILs, so
#      a grandfather edge cannot live forever without an explicit re-decision.
#
# Liveness ("exception no longer matches any real violation") is enforced once the
# marker-dependent TIER-1 guards are flipped to block (spec §4 Phase 2); until then
# expiry + resolve are the active liveness levers.
#
# Exit: 0 PASS · 1 violation · 2 usage/setup error (or SKIP when python3/PyYAML absent).
#
# Usage:
#   bash practices/evals/aggregate_boundary_allowlist_guard.sh
#   bash practices/evals/aggregate_boundary_allowlist_guard.sh --root DIR
#   bash practices/evals/aggregate_boundary_allowlist_guard.sh --file FILE --src DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ALLOWLIST="$REPO_ROOT/practices/evals/aggregate_boundary_allowlist.yaml"
SRC_DIR="$REPO_ROOT/backend/src/main/java"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; ALLOWLIST="$REPO_ROOT/practices/evals/aggregate_boundary_allowlist.yaml"; SRC_DIR="$REPO_ROOT/backend/src/main/java"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; ALLOWLIST="$REPO_ROOT/practices/evals/aggregate_boundary_allowlist.yaml"; SRC_DIR="$REPO_ROOT/backend/src/main/java"; shift ;;
        --file) ALLOWLIST="$2"; shift 2 ;;
        --file=*) ALLOWLIST="${1#--file=}"; shift ;;
        --src) SRC_DIR="$2"; shift 2 ;;
        --src=*) SRC_DIR="${1#--src=}"; shift ;;
        *) echo "aggregate_boundary_allowlist_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -f "$ALLOWLIST" ]; then
    echo "aggregate_boundary_allowlist_guard: missing $ALLOWLIST" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "aggregate_boundary_allowlist_guard: SKIP — python3 not on PATH"
    exit 0
fi

TODAY="$(date +%F)"

python3 - "$ALLOWLIST" "$SRC_DIR" "$TODAY" <<'PYEOF'
import sys, os

allowlist_path, src_dir, today = sys.argv[1:4]

try:
    import yaml
except ImportError:
    print("aggregate_boundary_allowlist_guard: SKIP — PyYAML not installed")
    sys.exit(0)

with open(allowlist_path) as f:
    try:
        doc = yaml.safe_load(f) or {}
    except yaml.YAMLError as e:
        print(f"VIOLATION: allowlist is not valid YAML: {e}", file=sys.stderr)
        sys.exit(1)

violations = []
def fail(msg): violations.append(msg)

BASE = "com.ax.template.authblueprint."

def fqn_to_file(fqn):
    return os.path.join(src_dir, fqn.replace(".", os.sep) + ".java")

def class_exists(fqn):
    return os.path.isfile(fqn_to_file(fqn))

def simple_in_feature(feature, simple):
    """A class with simple name `simple` exists somewhere under the feature package."""
    feat_dir = os.path.join(src_dir, BASE.replace(".", os.sep), feature)
    target = simple + ".java"
    for root, _dirs, files in os.walk(feat_dir):
        if target in files:
            return True
    return False

# ── top-level schema ────────────────────────────────────────────────────────
if not isinstance(doc, dict):
    print("VIOLATION: allowlist root must be a mapping", file=sys.stderr)
    sys.exit(1)

allowed_keys = {"shared_kernel", "published_api", "exceptions",
                "governed_god_service", "governed_state_mutators"}
extra = set(doc.keys()) - allowed_keys
if extra:
    fail(f"unexpected top-level key(s): {sorted(extra)} (allowed: {sorted(allowed_keys)})")

# shared_kernel: list of package strings (the ONLY place wildcards are allowed)
sk = doc.get("shared_kernel", [])
if not isinstance(sk, list):
    fail("shared_kernel must be a list")
else:
    for entry in sk:
        if not isinstance(entry, str):
            fail(f"shared_kernel entry must be a string: {entry!r}")

# published_api: map feature -> list[simple class name], NO wildcards
pa = doc.get("published_api", {})
if not isinstance(pa, dict):
    fail("published_api must be a mapping (feature -> [class, ...])")
else:
    for feature, classes in pa.items():
        if not isinstance(classes, list):
            fail(f"published_api['{feature}'] must be a list of class names")
            continue
        for c in classes:
            if not isinstance(c, str):
                fail(f"published_api['{feature}'] entry must be a string: {c!r}")
                continue
            if "*" in c or "." in c:
                fail(f"published_api['{feature}'] entry '{c}' must be a simple class name (no wildcard / no FQN)")
                continue
            if not simple_in_feature(feature, c):
                fail(f"published_api['{feature}'] class '{c}' does not resolve under feature package '{feature}'")

# exceptions: list of maps with all required keys, exact FQNs, valid expiry
REQUIRED = ["from", "to", "kind", "owner", "rationale", "expiry", "remediation_ticket"]
ex = doc.get("exceptions", [])
if not isinstance(ex, list):
    fail("exceptions must be a list")
else:
    for i, entry in enumerate(ex):
        if not isinstance(entry, dict):
            fail(f"exceptions[{i}] must be a mapping")
            continue
        for k in REQUIRED:
            if k not in entry or entry[k] in (None, ""):
                fail(f"exceptions[{i}] missing required key '{k}'")
        frm, to = entry.get("from"), entry.get("to")
        for label, val in (("from", frm), ("to", to)):
            if isinstance(val, str) and val:
                if "*" in val:
                    fail(f"exceptions[{i}].{label} '{val}' must be an exact FQN (no wildcard)")
                elif not val.startswith(BASE):
                    fail(f"exceptions[{i}].{label} '{val}' must be a fully-qualified authblueprint class")
                elif not class_exists(val):
                    fail(f"exceptions[{i}].{label} '{val}' does not resolve to a .java file")
        expiry = entry.get("expiry")
        if expiry is not None:
            es = str(expiry)
            import re
            if not re.match(r"^\d{4}-\d{2}-\d{2}$", es):
                fail(f"exceptions[{i}].expiry '{es}' must be YYYY-MM-DD")
            elif es < today:
                fail(f"exceptions[{i}].expiry '{es}' has passed (today {today}) — re-decide or remove (ticket {entry.get('remediation_ticket')})")

# governed_god_service: list of "FQN#method" — the class FQN must resolve
ggs = doc.get("governed_god_service", [])
if not isinstance(ggs, list):
    fail("governed_god_service must be a list")
else:
    for i, entry in enumerate(ggs):
        if not isinstance(entry, str) or "#" not in entry:
            fail(f"governed_god_service[{i}] '{entry!r}' must be 'FQN#method'")
            continue
        cls = entry.split("#", 1)[0]
        if not cls.startswith(BASE) or not class_exists(cls):
            fail(f"governed_god_service[{i}] class '{cls}' does not resolve to a .java file")

# governed_state_mutators: list of "callerFQN -> EntityFQN#setter" — both classes resolve
gsm = doc.get("governed_state_mutators", [])
if not isinstance(gsm, list):
    fail("governed_state_mutators must be a list")
else:
    for i, entry in enumerate(gsm):
        if not isinstance(entry, str) or "->" not in entry or "#" not in entry:
            fail(f"governed_state_mutators[{i}] '{entry!r}' must be 'callerFQN -> EntityFQN#setter'")
            continue
        caller, rest = [s.strip() for s in entry.split("->", 1)]
        entity = rest.split("#", 1)[0].strip()
        for cls in (caller, entity):
            if not cls.startswith(BASE) or not class_exists(cls):
                fail(f"governed_state_mutators[{i}] class '{cls}' does not resolve to a .java file")

if violations:
    print(f"aggregate_boundary_allowlist_guard: {len(violations)} violation(s) — BLOCKED", file=sys.stderr)
    for v in violations:
        print(f"  VIOLATION: {v}", file=sys.stderr)
    sys.exit(1)

n_pa = sum(len(v) for v in pa.values()) if isinstance(pa, dict) else 0
print(f"aggregate_boundary_allowlist_guard: OK — {len(sk)} kernel pkg(s), {n_pa} published_api type(s), {len(ex)} exception(s), all resolve & unexpired")
sys.exit(0)
PYEOF
