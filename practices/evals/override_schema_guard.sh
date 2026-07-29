#!/usr/bin/env bash
# practices/evals/override_schema_guard.sh — dogfood-8 (gap 6 closure, 33rd hard guard candidate).
#
# Sentinel guard. Validates the `override_allowed:` block of every
# specs/recipes/<pattern>-recipe-l0.yaml against the published schema
# specs/recipes/_override-schema.yaml.
#
# As of dogfood-8 (2026-05-20) zero recipes carry an active override —
# every override_allowed block is empty or contains only commented-out
# examples. The guard's job today is to *lock the shape* so that the
# first uncommented override in any future fork is structurally
# validated against the convention the catalog has been emitting
# commented-out examples of.
#
# Without this guard, the first divergent shape (e.g. enabled_l4_domains
# without rationale, or a misspelled l4-domain name) gets copied across
# downstream forks. Once enough forks carry it the catalog loses the
# ability to reason about overrides uniformly.
#
# Validation policy (matches the schema's MUST rules):
#   1. override_allowed is REQUIRED at the top level of every
#      specs/recipes/*-recipe-l0.yaml.
#   2. override_allowed must be a YAML mapping (may be empty).
#   3. The ONLY currently-defined field-path is `enabled_l4_domains`.
#      Any other key under override_allowed is REJECTED.
#   4. If enabled_l4_domains is present:
#        - at least one of `add` / `skip` MUST be present
#        - `rationale` MUST be present, length ≥ 16
#        - `citation`  MUST be present, length ≥ 1
#        - any l4-domain in add/skip MUST exist in
#          templates/L4/<domain>/ on disk (re-derived at guard run-time)
#        - duplicate entries inside add or skip are REJECTED
#
# Implementation: pure python3 + PyYAML. No external jsonschema
# dependency so the catalog stays bootstrap-clean on minimal envs.
#
# Exit codes:
#   0 — every recipe override_allowed block validates
#   1 — at least one violation
#   2 — bad args / missing python3 / missing PyYAML in --strict mode

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

STRICT_MODE=0
VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --strict) STRICT_MODE=1; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "override_schema_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "override_schema_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

RECIPES_DIR="$REPO_ROOT/specs/recipes"
L4_DIR="$REPO_ROOT/templates/L4"
SCHEMA_FILE="$REPO_ROOT/specs/recipes/_override-schema.yaml"

if [ ! -d "$RECIPES_DIR" ]; then
    echo "override_schema_guard: missing specs/recipes/ dir" >&2
    exit 2
fi
if [ ! -d "$L4_DIR" ]; then
    echo "override_schema_guard: missing templates/L4/ dir (cannot derive enum)" >&2
    exit 2
fi
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "override_schema_guard: missing schema specs/recipes/_override-schema.yaml" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    if [ "$STRICT_MODE" -eq 1 ]; then
        echo "override_schema_guard: FAIL — python3 not on PATH (--strict)" >&2
        exit 2
    fi
    echo "override_schema_guard: SKIP — python3 not on PATH"
    exit 0
fi

if ! python3 -c "import yaml" >/dev/null 2>&1; then
    # No parser ⇒ nothing was verified. Exit 2 regardless of --strict: the old
    # non-strict path exited 0, which any caller reads as PASS.
    echo "override_schema_guard: BLOCK — cannot verify: PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

OUT=$(RECIPES_DIR="$RECIPES_DIR" L4_DIR="$L4_DIR" VERBOSE="$VERBOSE" python3 - <<'PYEOF'
import os, sys, glob
import yaml

recipes_dir = os.environ["RECIPES_DIR"]
l4_dir      = os.environ["L4_DIR"]
verbose     = os.environ.get("VERBOSE", "0") == "1"

# Re-derive the l4 enum from disk.
# Sources, in priority order:
#   1) templates/L4/<domain>/ subdirectories
#   2) blueprints/<domain>-manifest.yaml stems
# Cross-cutting infra (practices, ui-tokens) is excluded — these have
# manifests but are not selectable L4 domains.
INFRA_EXCLUDE = {"practices", "ui-tokens"}
l4_enum = set()

if os.path.isdir(l4_dir):
    for name in os.listdir(l4_dir):
        full = os.path.join(l4_dir, name)
        if os.path.isdir(full) and not name.startswith("_") and not name.startswith("."):
            l4_enum.add(name)

# Merge blueprints manifest stems (the canonical L4 catalog index)
bp_dir = os.path.join(os.path.dirname(l4_dir.rstrip("/")), "..", "blueprints")
bp_dir = os.path.normpath(bp_dir)
if os.path.isdir(bp_dir):
    for name in os.listdir(bp_dir):
        if name.endswith("-manifest.yaml"):
            stem = name[:-len("-manifest.yaml")]
            if stem.endswith("-ui"):
                stem = stem[:-len("-ui")]
            if stem and stem not in INFRA_EXCLUDE:
                l4_enum.add(stem)

if not l4_enum:
    # fallback to the static enum from the schema; conservative behaviour
    # so we never block on a misconfigured templates/L4/ + blueprints/ pair
    l4_enum = {
        "audit-log","auth","billing","crud","email-outbox","feature-flags",
        "file-storage","i18n-policy","identity-verification","multi-tenant",
        "notification","payment","ratelimit","realtime-policy",
        "scheduled-task","search","webhook",
    }

ALLOWED_OVERRIDE_FIELDS = {"enabled_l4_domains"}
ALLOWED_OPS            = {"add", "skip"}
RATIONALE_MIN          = 16

violations = []
recipes_checked = 0
overrides_active = 0  # overrides with at least one uncommented op

def fail(file, msg):
    violations.append(f"FAIL {os.path.relpath(file)} :: {msg}")

for path in sorted(glob.glob(os.path.join(recipes_dir, "*-recipe-l0.yaml"))):
    recipes_checked += 1
    try:
        with open(path) as fh:
            data = yaml.safe_load(fh)
    except Exception as e:
        fail(path, f"yaml parse error: {e}")
        continue

    if not isinstance(data, dict):
        fail(path, "top-level not a mapping")
        continue

    if "override_allowed" not in data:
        fail(path, "missing required key: override_allowed")
        continue

    ov = data["override_allowed"]
    if ov is None:
        # explicit `override_allowed:` with no value parses to None — treat as empty
        ov = {}

    if not isinstance(ov, dict):
        fail(path, f"override_allowed must be a mapping, got {type(ov).__name__}")
        continue

    # additionalProperties:false
    unknown = set(ov.keys()) - ALLOWED_OVERRIDE_FIELDS
    if unknown:
        fail(path, f"override_allowed has unknown field(s): {sorted(unknown)}")
        continue

    if "enabled_l4_domains" in ov:
        overrides_active += 1
        block = ov["enabled_l4_domains"]
        if not isinstance(block, dict):
            fail(path, f"enabled_l4_domains must be a mapping, got {type(block).__name__}")
            continue

        unknown_inner = set(block.keys()) - (ALLOWED_OPS | {"rationale", "citation"})
        if unknown_inner:
            fail(path, f"enabled_l4_domains has unknown key(s): {sorted(unknown_inner)}")
            continue

        ops_present = ALLOWED_OPS & set(block.keys())
        if not ops_present:
            fail(path, "enabled_l4_domains needs at least one of `add` or `skip`")
            continue

        for op in ops_present:
            value = block[op]
            if not isinstance(value, list) or not value:
                fail(path, f"enabled_l4_domains.{op} must be a non-empty list")
                continue
            seen = set()
            for item in value:
                if not isinstance(item, str):
                    fail(path, f"enabled_l4_domains.{op} item must be a string, got {type(item).__name__}")
                    continue
                if item in seen:
                    fail(path, f"enabled_l4_domains.{op} has duplicate {item!r}")
                    continue
                seen.add(item)
                if item not in l4_enum:
                    fail(path, f"enabled_l4_domains.{op} contains unknown l4-domain {item!r} (not in templates/L4/)")

        rationale = block.get("rationale")
        if not isinstance(rationale, str) or len(rationale) < RATIONALE_MIN:
            fail(path, f"rationale must be a string of length ≥ {RATIONALE_MIN}")

        citation = block.get("citation")
        if not isinstance(citation, str) or len(citation) < 1:
            fail(path, "citation must be a non-empty string")

if verbose or violations:
    for v in violations:
        print(v)

print(f"SUMMARY recipes_checked={recipes_checked} active_overrides={overrides_active} violations={len(violations)} l4_enum_size={len(l4_enum)}")
sys.exit(1 if violations else 0)
PYEOF
)
PY_EXIT=$?

if [ "$VERBOSE" -eq 1 ] || [ "$PY_EXIT" -ne 0 ]; then
    echo "$OUT"
fi

if [ "$PY_EXIT" -ne 0 ]; then
    echo "" >&2
    echo "override_schema_guard: FAIL — see violations above." >&2
    echo "" >&2
    echo "Fix policy: align the recipe to specs/recipes/_override-schema.yaml." >&2
    echo "  - override_allowed: {} is allowed (sentinel / no active deviation)" >&2
    echo "  - enabled_l4_domains is the only field-path supported in R8" >&2
    echo "  - rationale (≥16 chars) AND citation (non-empty) are required" >&2
    echo "  - l4-domain names must match a directory under templates/L4/" >&2
    exit 1
fi

SUMMARY_LINE=$(echo "$OUT" | grep "^SUMMARY" || echo "SUMMARY ?")
echo "override_schema_guard: PASS — $SUMMARY_LINE"
exit 0
