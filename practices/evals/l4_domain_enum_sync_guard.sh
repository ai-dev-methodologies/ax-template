#!/usr/bin/env bash
# practices/evals/l4_domain_enum_sync_guard.sh
# dogfood-12 (R12 closure, 35th hard guard).
#
# Enforces the 3-source coherence of L4 domain enumeration across:
#
#   1. DISK    — templates/L4/<domain>/ directories
#   2. SCHEMA  — specs/recipes/_override-schema.yaml#$defs/l4_domain enum
#   3. RECIPES — specs/recipes/*-recipe-l0.yaml#enabled_l4_domains lists
#
# against the canonical classification at specs/l4-domain-classification.yaml.
#
# Six invariants validated (matching the spec's I1..I6):
#
#   I1  Every disk dir under templates/L4/ (excluding `.gitkeep`) appears in
#       exactly one tier of the classification file.
#   I2  Every entry in the schema enum appears in exactly one of SELECTABLE
#       or FUTURE_ADD (never INFRA).
#   I3  Every name listed in any tier of the classification file is unique
#       across all tiers (no double-listed name).
#   I4  INFRA entries: on disk AND NOT in schema enum AND NOT referenced by
#       any recipe's enabled_l4_domains.
#   I5  SELECTABLE entries: on disk AND in schema enum.
#   I6  FUTURE_ADD entries: NOT on disk AND in schema enum.
#
# Exit codes:
#   0 — all six invariants hold
#   1 — at least one invariant violated
#   2 — bad args / missing python3 / missing PyYAML in --strict mode /
#       missing required source files
#
# Implementation: pure python3 + PyYAML (matches the override_schema_guard.sh
# pattern; no jsonschema dependency).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

STRICT_MODE=0
VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --strict) STRICT_MODE=1; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "l4_domain_enum_sync_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "l4_domain_enum_sync_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

CLASSIFICATION_FILE="$REPO_ROOT/specs/l4-domain-classification.yaml"
SCHEMA_FILE="$REPO_ROOT/specs/recipes/_override-schema.yaml"
RECIPES_DIR="$REPO_ROOT/specs/recipes"
L4_DIR="$REPO_ROOT/templates/L4"

if [ ! -f "$CLASSIFICATION_FILE" ]; then
    echo "l4_domain_enum_sync_guard: missing $CLASSIFICATION_FILE" >&2
    exit 2
fi
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "l4_domain_enum_sync_guard: missing $SCHEMA_FILE" >&2
    exit 2
fi
if [ ! -d "$RECIPES_DIR" ]; then
    echo "l4_domain_enum_sync_guard: missing $RECIPES_DIR" >&2
    exit 2
fi
if [ ! -d "$L4_DIR" ]; then
    echo "l4_domain_enum_sync_guard: missing $L4_DIR" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    if [ "$STRICT_MODE" -eq 1 ]; then
        echo "l4_domain_enum_sync_guard: FAIL — python3 not on PATH (--strict)" >&2
        exit 2
    fi
    echo "l4_domain_enum_sync_guard: SKIP — python3 not on PATH"
    exit 0
fi

if ! python3 -c "import yaml" >/dev/null 2>&1; then
    # No parser ⇒ nothing was verified. Exit 2 regardless of --strict: the old
    # non-strict path exited 0, which any caller reads as PASS.
    echo "l4_domain_enum_sync_guard: BLOCK — cannot verify: PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

VERBOSE_FLAG="0"
if [ "$VERBOSE" -eq 1 ]; then VERBOSE_FLAG="1"; fi

CLASSIFICATION_FILE="$CLASSIFICATION_FILE" \
SCHEMA_FILE="$SCHEMA_FILE" \
RECIPES_DIR="$RECIPES_DIR" \
L4_DIR="$L4_DIR" \
VERBOSE_FLAG="$VERBOSE_FLAG" \
python3 - <<'PYEOF'
import os
import sys
import glob
import yaml

CF      = os.environ["CLASSIFICATION_FILE"]
SF      = os.environ["SCHEMA_FILE"]
RDIR    = os.environ["RECIPES_DIR"]
L4DIR   = os.environ["L4_DIR"]
VERBOSE = os.environ["VERBOSE_FLAG"] == "1"

errors = []

def log(msg):
    if VERBOSE:
        print(f"l4_domain_enum_sync_guard: {msg}")

# ── Load classification ──────────────────────────────────────────────────────
with open(CF) as fh:
    classification = yaml.safe_load(fh)

tiers = classification.get("tiers") or {}
infra_items      = [it["name"] for it in (tiers.get("infra")      or {}).get("items", [])]
selectable_items = [it["name"] for it in (tiers.get("selectable") or {}).get("items", [])]
future_items     = [it["name"] for it in (tiers.get("future_add") or {}).get("items", [])]

infra_set      = set(infra_items)
selectable_set = set(selectable_items)
future_set     = set(future_items)

log(f"infra={sorted(infra_set)}")
log(f"selectable={sorted(selectable_set)}")
log(f"future_add={sorted(future_set)}")

# ── Load schema enum ─────────────────────────────────────────────────────────
with open(SF) as fh:
    schema = yaml.safe_load(fh)
try:
    schema_enum = list(schema["$defs"]["l4_domain"]["enum"])
except (KeyError, TypeError):
    errors.append("schema: cannot locate $defs.l4_domain.enum in override-schema.yaml")
    schema_enum = []
schema_set = set(schema_enum)
log(f"schema_enum={sorted(schema_set)}")

# ── Load disk dirs ───────────────────────────────────────────────────────────
disk_set = set()
for entry in sorted(os.listdir(L4DIR)):
    path = os.path.join(L4DIR, entry)
    if entry.startswith("."):       # .gitkeep etc
        continue
    if not os.path.isdir(path):
        continue
    disk_set.add(entry)
log(f"disk={sorted(disk_set)}")

# ── Load recipes union ───────────────────────────────────────────────────────
recipes_union = set()
recipes_per_domain = {}  # domain -> [recipe_slug]
for f in sorted(glob.glob(os.path.join(RDIR, "*-recipe-l0.yaml"))):
    base = os.path.basename(f)
    if base.startswith("_"):
        continue
    slug = base.replace("-recipe-l0.yaml", "")
    try:
        with open(f) as fh:
            rec = yaml.safe_load(fh)
    except yaml.YAMLError as e:
        errors.append(f"recipe parse: {base}: {e}")
        continue
    enabled = (rec or {}).get("enabled_l4_domains") or []
    if not isinstance(enabled, list):
        errors.append(f"recipe {base}: enabled_l4_domains is not a list")
        continue
    for d in enabled:
        recipes_union.add(d)
        recipes_per_domain.setdefault(d, []).append(slug)
log(f"recipes_union={sorted(recipes_union)}")

# ── I3: tier-uniqueness ──────────────────────────────────────────────────────
# Run first — downstream invariants assume tiers are disjoint and unique.
all_tier_names = infra_items + selectable_items + future_items
dup = set()
seen = set()
for n in all_tier_names:
    if n in seen:
        dup.add(n)
    seen.add(n)
if dup:
    errors.append(
        f"I3: name(s) appear in >1 tier of classification: {sorted(dup)}"
    )

# Per-tier duplicate detection (within the SAME tier list)
for tier_name, items in (("infra", infra_items),
                        ("selectable", selectable_items),
                        ("future_add", future_items)):
    s = set()
    for n in items:
        if n in s:
            errors.append(f"I3: duplicate name '{n}' within tier '{tier_name}'")
        s.add(n)

# ── I1: every disk dir is classified ─────────────────────────────────────────
classified = infra_set | selectable_set | future_set
unclassified_disk = disk_set - classified
if unclassified_disk:
    errors.append(
        f"I1: disk dir(s) under templates/L4/ not classified: "
        f"{sorted(unclassified_disk)}"
    )

# ── I2: every schema enum entry is in selectable OR future_add ───────────────
unclassified_schema = schema_set - (selectable_set | future_set)
if unclassified_schema:
    errors.append(
        f"I2: schema enum entry(ies) not in SELECTABLE or FUTURE_ADD: "
        f"{sorted(unclassified_schema)}"
    )
# Conversely: an INFRA name in the schema enum is a hard violation.
infra_in_schema = infra_set & schema_set
if infra_in_schema:
    errors.append(
        f"I2: INFRA entry(ies) present in schema enum (must be excluded): "
        f"{sorted(infra_in_schema)}"
    )

# ── I4: INFRA — on disk, NOT in schema, NOT in recipes ───────────────────────
for n in sorted(infra_set):
    if n not in disk_set:
        errors.append(f"I4: INFRA '{n}' missing from disk (templates/L4/{n}/)")
    if n in schema_set:
        errors.append(f"I4: INFRA '{n}' must NOT appear in schema enum")
    if n in recipes_union:
        errors.append(
            f"I4: INFRA '{n}' must NOT be referenced by any recipe "
            f"enabled_l4_domains (currently in: "
            f"{sorted(recipes_per_domain.get(n, []))})"
        )

# ── I5: SELECTABLE — on disk AND in schema ───────────────────────────────────
for n in sorted(selectable_set):
    if n not in disk_set:
        errors.append(
            f"I5: SELECTABLE '{n}' missing from disk (templates/L4/{n}/)"
        )
    if n not in schema_set:
        errors.append(f"I5: SELECTABLE '{n}' missing from schema enum")

# ── I6: FUTURE_ADD — NOT on disk AND in schema ───────────────────────────────
for n in sorted(future_set):
    if n in disk_set:
        errors.append(
            f"I6: FUTURE_ADD '{n}' present on disk — promote to SELECTABLE"
        )
    if n not in schema_set:
        errors.append(f"I6: FUTURE_ADD '{n}' missing from schema enum")

# ── Report ───────────────────────────────────────────────────────────────────
if errors:
    print("l4_domain_enum_sync_guard: FAIL", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)

print("l4_domain_enum_sync_guard: PASS — 6 invariants (I1..I6) hold")
print(f"  disk        = {len(disk_set)} dirs")
print(f"  schema enum = {len(schema_set)} entries")
print(f"  recipes union = {len(recipes_union)} domains")
print(f"  classified  = INFRA {len(infra_set)} + SELECTABLE "
      f"{len(selectable_set)} + FUTURE_ADD {len(future_set)} "
      f"= {len(classified)} total")
sys.exit(0)
PYEOF
EXIT=$?
exit $EXIT
