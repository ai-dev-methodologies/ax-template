#!/usr/bin/env bash
# practices/evals/manifest_yaml_strict_parse_guard.sh — dogfood-8 (NEW-3 closure, 32nd hard guard candidate).
#
# Validates that every blueprints/*.yaml passes PyYAML strict parse with
# duplicate-key detection. This catches three silent-failure classes that
# downstream skills (/ax-scaffold, /ax-verify-recipe, codegen consumers)
# would otherwise hit in production:
#
#   1. Indentation typos in block scalars — e.g. dogfood-6 multi-tenant
#      line 450 mid-comment `*` jumped to column 4. PyYAML's lenient
#      parser swallowed it; strict consumers (round-trip mechanical
#      substitution) crashed.
#   2. Inline mapping flow without space after key colon — e.g.
#      `placeholder:{ token: ... }` parses as a scalar key in some
#      tooling and an inline mapping in others. The discrepancy hides
#      a real schema break.
#   3. Duplicate keys at the same mapping level — first wins in default
#      PyYAML, last wins in some other engines. Catalog consumers that
#      compare key sets across artifacts (cross_trio_guard, recipe_sibling)
#      see different shapes depending on which library reads the file.
#
# All three are silent in the lenient parser. They surface as either an
# accidental PASS (false-confidence) or a runtime crash in a skill that
# runs months after the file was written. The 32nd guard makes them a
# blocking failure at catalog-quality check time.
#
# Scope: blueprints/*.yaml only.
#   - specs/recipes/, specs/, contracts/ already round-trip clean
#     (verified dogfood-8 dry-run, 2026-05-20).
#   - blueprints is the canonical home for policy manifests that other
#     skills mechanically substitute into. Strict parse is required here.
#
# Verification policy:
#   - require python3 with PyYAML 5+ available; otherwise SKIP (non-blocking)
#     so the catalog runs on minimal envs without breaking.
#   - in strict mode (CI / run-all-guards.sh), missing PyYAML is exit 2.
#
# Discovered by dogfood-8 (P2 R8 NEW-3): dry-run found
# `blueprints/ui-tokens-manifest.yaml` line 27 — `placeholder:{` without
# the conventional space before `{`, which the YAML 1.1 strict parser
# rejects with "expected <block end>, but found ','". The other 28
# blueprints pass; the issue is hidden because UI-token consumers all
# go through a JSON pre-process step that masks the parse error.
#
# Exit codes:
#   0 — all blueprints parse strict + no duplicate keys
#   1 — at least one strict-parse failure or duplicate key
#   2 — bad args / PyYAML missing in --strict mode

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

STRICT_MODE=0
VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --strict)  STRICT_MODE=1; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "manifest_yaml_strict_parse_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "manifest_yaml_strict_parse_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

BLUEPRINTS_DIR="$REPO_ROOT/blueprints"
if [ ! -d "$BLUEPRINTS_DIR" ]; then
    echo "manifest_yaml_strict_parse_guard: missing blueprints/ dir" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    if [ "$STRICT_MODE" -eq 1 ]; then
        echo "manifest_yaml_strict_parse_guard: FAIL — python3 not on PATH (--strict)" >&2
        exit 2
    fi
    echo "manifest_yaml_strict_parse_guard: SKIP — python3 not on PATH"
    exit 0
fi

if ! python3 -c "import yaml" >/dev/null 2>&1; then
    # No parser ⇒ nothing was verified. Exit 2 regardless of --strict: the old
    # non-strict path exited 0, which any caller reads as PASS.
    echo "manifest_yaml_strict_parse_guard: BLOCK — cannot verify: PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

# Run strict parse via inline python3. Output:
#   PASS <file>
#   FAIL <file> :: <one-line message>
RESULT=$(BLUEPRINTS_DIR="$BLUEPRINTS_DIR" VERBOSE="$VERBOSE" python3 - <<'PYEOF'
import os, sys, glob
import yaml

blueprints_dir = os.environ["BLUEPRINTS_DIR"]
verbose = os.environ.get("VERBOSE", "0") == "1"

class DuplicateKeyError(Exception):
    pass

class StrictLoader(yaml.SafeLoader):
    pass

def construct_mapping_strict(loader, node, deep=False):
    mapping = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in mapping:
            line = key_node.start_mark.line + 1
            raise DuplicateKeyError(f"duplicate key {key!r} at line {line}")
        value = loader.construct_object(value_node, deep=deep)
        mapping[key] = value
    return mapping

StrictLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
    construct_mapping_strict,
)

failures = 0
total = 0
for path in sorted(glob.glob(os.path.join(blueprints_dir, "*.yaml"))):
    total += 1
    rel = os.path.relpath(path)
    try:
        with open(path) as fh:
            yaml.load(fh, Loader=StrictLoader)
    except (yaml.YAMLError, DuplicateKeyError) as e:
        failures += 1
        msg = str(e).replace("\n", " | ")
        print(f"FAIL {rel} :: {msg}")
        continue
    except Exception as e:
        failures += 1
        print(f"FAIL {rel} :: unexpected {type(e).__name__}: {e}")
        continue
    if verbose:
        print(f"PASS {rel}")

print(f"SUMMARY total={total} failures={failures}")
sys.exit(1 if failures > 0 else 0)
PYEOF
)
PY_EXIT=$?

# echo result transcript
if [ "$VERBOSE" -eq 1 ] || [ "$PY_EXIT" -ne 0 ]; then
    echo "$RESULT"
fi

if [ "$PY_EXIT" -ne 0 ]; then
    echo "" >&2
    echo "manifest_yaml_strict_parse_guard: FAIL — at least one blueprint fails strict parse." >&2
    echo "" >&2
    echo "Fix policy:" >&2
    echo "  - block scalar indentation typos must align to the surrounding block" >&2
    echo "  - inline mappings must have one space after the parent key colon:" >&2
    echo "      WRONG: placeholder:{ token: ... }" >&2
    echo "      RIGHT: placeholder: { token: ... }" >&2
    echo "  - no duplicate keys at the same mapping level" >&2
    echo "" >&2
    echo "Discovered files appear in the FAIL rows above. Reconcile and re-run." >&2
    exit 1
fi

SUMMARY_LINE=$(echo "$RESULT" | grep "^SUMMARY" || echo "SUMMARY total=? failures=?")
echo "manifest_yaml_strict_parse_guard: PASS — $SUMMARY_LINE"
exit 0
