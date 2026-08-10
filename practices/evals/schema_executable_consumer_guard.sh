#!/usr/bin/env bash
# practices/evals/schema_executable_consumer_guard.sh — BACKLOG P2-76.
#
# WHY THIS GUARD EXISTS
# ----------------------
# A `*.schema.json` file that nothing ever loads is not enforcement — it is the LOOK of
# enforcement. P1-74 shipped exactly that shape: a schema file existed, docs pointed at it,
# but zero code loaded it at runtime, so the "contract" it claimed to enforce was never
# actually checked. This guard makes every schema file in the tree PROVE one of two things:
#   (a) EXECUTABLE  — a real code file loads/reads it at a load-context reference (not a
#                      prose mention in a .md file), OR
#   (b) DOCUMENTED-ONLY — registered as such, with a non-blank reason, so the gap is an
#                      audited decision instead of a silent assumption.
# The registry is practices/evals/schema_consumer_manifest.yaml. Both branches are checked
# against DISK, not merely against the manifest's own claims — a manifest that says "consumed
# by X" where X does not exist, or exists but never mentions the schema, is caught the same
# way derived_block_license_guard.sh catches a forged `upstream` citation.
#
# SIX INDEPENDENTLY-BLOCKING INVARIANTS
# --------------------------------------
#   (1) COMPLETENESS   — every `*.schema.json` on disk is registered (UNREGISTERED).
#   (2) REVERSE EXISTS — every registered `path` still exists on disk (STALE_ENTRY).
#   (3) ONE-TO-ONE     — no `path` registered twice (DUPLICATE_PATH).
#   (4) STATUS SHAPE   — `status` is exactly `consumed` or `documented-only`, and the field
#                        each status requires is non-blank: `consumed` needs `consumer`,
#                        `documented-only` needs `reason` (INVALID_STATUS / MISSING_FIELD).
#   (5) CONSUMER REAL  — for `consumed`, the named `consumer` path must (i) have a code
#                        extension (.py/.js/.mjs/.sh — the set named in the BACKLOG
#                        done-when; a .md/.txt "consumer" is prose, not execution), and
#                        (ii) exist, be readable, AND contain the schema's own basename
#                        somewhere in its text — a claimed consumer that fails either is
#                        PHANTOM_CONSUMER. (ii) is deliberately ONE combined check, not
#                        "exists" then "references" as two sequential ones: a consumer
#                        that does not exist trivially has no content referencing
#                        anything either, so two independent checks here could never
#                        fail independently for the same fixture — proven by mutation
#                        testing (fixture_kill_manifest.yaml): a first draft with a
#                        separate existence pre-check was VACUOUS, because disabling
#                        just that check still left the content check firing on the
#                        same missing file.
#   (6) REASON BLANK   — `documented-only` additionally requires a non-blank `reason` even
#                        when (4) already required it non-empty as a string (defence in depth
#                        against a whitespace-only value slipping past a bare truthiness check).
#
# WHAT (5) DOES NOT PROVE, disclosed rather than hidden: a substring match on the schema's
# basename is not a parse of the consumer's AST — a consumer that merely contains the string
# in an unrelated context would satisfy it. What it DOES close is the P1-74 shape exactly:
# zero code files anywhere referencing the schema at all. Tightening substring-match to a
# real load-context assertion (open/read_text/require/import) is future work if a consumer is
# ever found gaming this with an incidental string match; it has not happened yet and adding
# an AST check for a threat that has not materialized would be speculative generality.
#
# NON-VACUITY: zero schema files found is NO_SCHEMAS_FOUND (exit 2) on ANY root — a collapsed
# glob or relocated tree must never report a silent PASS about an empty set. A LIVE root (no
# --root override) is additionally floored at LIVE_MIN_SCHEMAS below (the disk-measured count
# at introduction), so deleting schema files cannot silently shrink what gets checked.
#
# SELF-MATCH HAZARD: the manifest itself is a `.yaml` file, never a `.schema.json`, so it can
# never appear in its own disk census — no exclusion needed for the manifest path itself.
# Fixture roots ARE excluded from a LIVE scan (the disk census below skips anything under
# practices/evals/fixtures/) so that this guard's own fixtures never inflate the live count;
# a --root override targets a fixture directly and that exclusion is a no-op there.
#
# Usage:
#   bash practices/evals/schema_executable_consumer_guard.sh                 # live repo root
#   bash practices/evals/schema_executable_consumer_guard.sh --root DIR      # fixture mode
#   bash practices/evals/schema_executable_consumer_guard.sh --root=DIR      # fixture mode (= form)
#
# Exit: 0 PASS · 1 VIOLATION (one or more invariants broken) ·
#       2 NO_SCHEMAS_FOUND / LIVE_FLOOR_BREACH / MANIFEST_MISSING / MANIFEST_PARSE_ERROR / usage error.

set -uo pipefail

# ── Fail closed: this guard verifies through PyYAML (same policy as evidence_guard.sh /
# derived_block_license_guard.sh — pinned mechanically by pyyaml_preflight_coverage_guard.sh [95]).
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "schema_executable_consumer_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "schema_executable_consumer_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

IS_LIVE=1
if [ -n "$ROOT_OVERRIDE" ]; then
    [ -d "$ROOT_OVERRIDE" ] || {
        echo "schema_executable_consumer_guard: root not found: $ROOT_OVERRIDE" >&2; exit 2; }
    SCAN_ROOT="$(cd "$ROOT_OVERRIDE" && pwd)"
    IS_LIVE=0
else
    SCAN_ROOT="$REPO_ROOT"
fi

# Live-mode floor — the disk-measured count of *.schema.json files at introduction
# (2026-08-10: 3 — ax.config.schema.json, persona-registry.schema.json, manifest.schema.json).
# MAY NOT be silently lowered; a legitimate reduction is a reviewed edit to this constant,
# not a data-file change.
LIVE_MIN_SCHEMAS=3

MANIFEST_REL="practices/evals/schema_consumer_manifest.yaml"
# Code extensions a "consumer" is allowed to be — the exact set named in the BACKLOG
# done-when. A .md/.txt/.yaml "consumer" is a prose reference, not execution.
ALLOWED_CODE_EXT=".py .js .mjs .sh"

python3 - "$SCAN_ROOT" "$IS_LIVE" "$LIVE_MIN_SCHEMAS" "$MANIFEST_REL" "$ALLOWED_CODE_EXT" <<'PY'
import sys
import yaml
from pathlib import Path

scan_root = Path(sys.argv[1])
is_live = sys.argv[2] == "1"
live_min = int(sys.argv[3])
manifest_rel = sys.argv[4]
allowed_ext = set(sys.argv[5].split())

FIXTURES_PREFIX = "practices/evals/fixtures/"

def find_schema_files():
    found = []
    for path in sorted(scan_root.rglob("*.schema.json")):
        if not path.is_file():
            continue
        rel = path.relative_to(scan_root).as_posix()
        # Defence in depth: this guard's OWN fixtures live under practices/evals/fixtures/
        # inside the real repo tree; a live scan (no --root) must never count them.
        if rel.startswith(FIXTURES_PREFIX):
            continue
        found.append(rel)
    return found

disk = find_schema_files()

# ── non-vacuity (any root) ────────────────────────────────────────────────────
if not disk:
    print("schema_executable_consumer_guard: BLOCK — NO_SCHEMAS_FOUND: zero *.schema.json "
          "files found under the scan root — a collapsed glob or relocated tree would "
          "otherwise report PASS about an empty set.", file=sys.stderr)
    sys.exit(2)

# ── live-mode floor ────────────────────────────────────────────────────────────
if is_live and len(disk) < live_min:
    print(f"schema_executable_consumer_guard: BLOCK — LIVE_FLOOR_BREACH: {len(disk)} "
          f"*.schema.json file(s) found on the live root, below the pinned floor "
          f"LIVE_MIN_SCHEMAS={live_min}.", file=sys.stderr)
    sys.exit(2)

# ── load manifest ────────────────────────────────────────────────────────────────
manifest_path = scan_root / manifest_rel
if not manifest_path.is_file():
    print(f"schema_executable_consumer_guard: BLOCK — MANIFEST_MISSING: {manifest_path} "
          f"not found.", file=sys.stderr)
    sys.exit(2)
try:
    doc = yaml.safe_load(manifest_path.read_text(encoding="utf-8")) or {}
except yaml.YAMLError as e:
    print(f"schema_executable_consumer_guard: BLOCK — MANIFEST_PARSE_ERROR: {e}", file=sys.stderr)
    sys.exit(2)

entries = doc.get("schemas") or []

violations = []
seen_paths = {}

def is_blank(v):
    return v is None or (isinstance(v, str) and v.strip() == "")

for idx, entry in enumerate(entries):
    if not isinstance(entry, dict):
        violations.append(f"MALFORMED_ENTRY — schemas[{idx}] is not a mapping: {entry!r}")
        continue
    path = entry.get("path")
    path_key = str(path) if path is not None else f"<missing path at schemas[{idx}]>"

    # (3) one-to-one — duplicate path registration
    if path is not None:
        if path in seen_paths:
            violations.append(
                f"DUPLICATE_PATH — '{path}' is registered more than once "
                f"(schemas[{seen_paths[path]}] and schemas[{idx}])")
        else:
            seen_paths[path] = idx

    # (2) reverse existence — registered path must exist on disk
    if path is not None and path not in disk:
        violations.append(f"STALE_ENTRY — '{path}' is registered but not found on disk")

    status = entry.get("status")
    if status not in ("consumed", "documented-only"):
        violations.append(
            f"INVALID_STATUS — {path_key}: status is {status!r}, expected "
            f"'consumed' or 'documented-only'")
        continue

    if status == "documented-only":
        # (4)/(6) reason must be non-blank.
        if is_blank(entry.get("reason")):
            violations.append(f"MISSING_FIELD — {path_key}: 'reason' is blank or absent")
        continue

    # status == "consumed"
    consumer = entry.get("consumer")
    if is_blank(consumer):
        violations.append(f"MISSING_FIELD — {path_key}: 'consumer' is blank or absent")
        continue

    consumer_path = scan_root / consumer
    ext = consumer_path.suffix
    if ext not in allowed_ext:
        violations.append(
            f"PHANTOM_CONSUMER — {path_key}: consumer '{consumer}' has extension "
            f"'{ext}', not an executable code extension ({sorted(allowed_ext)}) — a doc/prose "
            f"reference is not an execution consumer")
        continue

    # Existence/readability and content-reference are deliberately ONE combined check,
    # not two sequential ones: a consumer that does not exist trivially has no content
    # matching the schema's basename either, so a missing file would ALWAYS also trip a
    # separate "never references" check even after the existence check alone was
    # disabled — two checks that can never fail independently for the same fixture are
    # not two invariants, they are one invariant proven twice. Merging them means the
    # SINGLE validity check below is the whole story for this fixture class.
    schema_basename = Path(path).name if path is not None else ""
    consumer_valid = False
    try:
        consumer_text = consumer_path.read_text(encoding="utf-8")
        consumer_valid = bool(schema_basename) and schema_basename in consumer_text
    except (FileNotFoundError, IsADirectoryError, PermissionError, UnicodeDecodeError, OSError):
        consumer_valid = False

    if not consumer_valid:
        violations.append(
            f"PHANTOM_CONSUMER — {path_key}: consumer '{consumer}' does not exist, could "
            f"not be read, or never references '{schema_basename}' anywhere in its text")

# (1) completeness — every schema file on disk must be registered
registered_paths = set(seen_paths.keys())
for path in disk:
    if path not in registered_paths:
        violations.append(f"UNREGISTERED — '{path}' is a *.schema.json file but is not registered in {manifest_rel}")

if violations:
    print("schema_executable_consumer_guard: FAIL — schema consumer registry invariant(s) violated:", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    sys.exit(1)

print(f"schema_executable_consumer_guard: PASS — {len(disk)} schema file(s), manifest agrees "
      f"exactly with disk (registration, existence, uniqueness, status shape, consumer reality "
      f"all verified).")
sys.exit(0)
PY
