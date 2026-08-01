#!/usr/bin/env bash
# practices/evals/derived_block_license_guard.sh — derived-block provenance ledger guard (D-4).
#
# THE INVARIANT: every templates/**/*.tsx file that carries an `@ax-codified-from` header
# (a block CODIFIED FROM a community 21st.dev component — see each file's own frontmatter
# `evidence:` block) must be registered in templates/DERIVED-SOURCES.yaml, and that
# registration must be TRUE, not just PRESENT. A prior draft of this guard checked only
# that fields were non-empty, which lets an arbitrary fake `upstream`, a duplicated path,
# or `included_in_package: false` pass silently — this guard checks the ledger's CONTENT
# against the disk, not merely its existence. Six invariants, each independently BLOCKING:
#   (1) COMPLETENESS   — every header-bearing file on disk is registered (no silent gap).
#   (2) REVERSE EXISTS — every registered `path` still exists on disk (no orphaned entry).
#   (3) ONE-TO-ONE     — no `path` is registered twice.
#   (4) PROVENANCE     — the registered `upstream` matches the file's OWN header verbatim
#                        (whitespace-trim only) — the only check that catches a FORGED
#                        upstream citation; without it the ledger's truth claim is unverified.
#   (5) INCLUSION      — `included_in_package == true` for every entry (the maintainer
#                        decision recorded in the ledger's `policy:` block, made real).
#   (6) FIELD BLANKS   — path/upstream/upstream_license/included_in_package/codified_at
#                        are all non-blank.
#
# Non-vacuity: zero header-bearing files found is NOT a green "nothing to check" — it is
# NO_DERIVED_BLOCKS_FOUND (exit 2), because a broken glob or a relocated templates/ tree
# would otherwise report PASS about an empty set. On a LIVE root (no --root override) the
# count is additionally floored at LIVE_MIN_DERIVED below — set to the disk-measured count
# at introduction, so a mass de-registration cannot silently shrink the census.
#
# SELF-MATCH HAZARD: templates/DERIVED-SOURCES.yaml itself lives under templates/, the
# tree this guard scans. The census/extraction commands below MUST be quoted in full here
# (a file outside templates/) rather than in the ledger's own comments, or the ledger would
# register itself as a block needing a ledger entry:
#   grep -rln "@ax-codified-from" templates/ | sort         # census
#   grep -oE "@ax-codified-from\s+\S+" <file>                # per-file extraction
# The scan below additionally excludes the ledger's own path as defense in depth.
#
# Usage:
#   bash practices/evals/derived_block_license_guard.sh                 # live repo root
#   bash practices/evals/derived_block_license_guard.sh --root DIR      # fixture mode
#   bash practices/evals/derived_block_license_guard.sh --root=DIR      # fixture mode (= form)
#
# Exit: 0 PASS · 1 VIOLATION (one or more invariants broken) ·
#       2 NO_DERIVED_BLOCKS_FOUND / LIVE_FLOOR_BREACH / usage-setup error.

set -uo pipefail

# ── Fail closed: this guard verifies through PyYAML (same policy as evidence_guard.sh /
# spec_ref_guard.sh — pinned mechanically by pyyaml_preflight_coverage_guard.sh [95]).
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "derived_block_license_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "derived_block_license_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

IS_LIVE=1
if [ -n "$ROOT_OVERRIDE" ]; then
    [ -d "$ROOT_OVERRIDE" ] || {
        echo "derived_block_license_guard: root not found: $ROOT_OVERRIDE" >&2; exit 2; }
    SCAN_ROOT="$(cd "$ROOT_OVERRIDE" && pwd)"
    IS_LIVE=0
else
    SCAN_ROOT="$REPO_ROOT"
fi

# Live-mode floor — the disk-measured count of header-bearing files at introduction
# (2026-08-02: 13, all under templates/L2/blocks/). MAY NOT be silently lowered; a
# legitimate reduction is a reviewed edit to this constant, not a data-file change.
LIVE_MIN_DERIVED=13

python3 - "$SCAN_ROOT" "$IS_LIVE" "$LIVE_MIN_DERIVED" <<'PY'
import re
import sys
import yaml
from pathlib import Path

scan_root = Path(sys.argv[1])
is_live = sys.argv[2] == "1"
live_min = int(sys.argv[3])

templates_dir = scan_root / "templates"
LEDGER_REL = "templates/DERIVED-SOURCES.yaml"
HEADER_RE = re.compile(r'@ax-codified-from\s+(.+)')

def find_header_files():
    found = {}
    if not templates_dir.is_dir():
        return found
    for path in sorted(templates_dir.rglob("*")):
        if not path.is_file():
            continue
        # Defense in depth: the ledger itself lives under templates/ and its own
        # documentation must describe this tag — excluded so a prose mention there can
        # never register the ledger as a block needing a ledger entry.
        if path.relative_to(scan_root).as_posix() == LEDGER_REL:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        if "@ax-codified-from" not in text:
            continue
        m = HEADER_RE.search(text)
        if not m:
            continue
        rel = path.relative_to(scan_root).as_posix()
        found[rel] = m.group(1).strip()
    return found

disk = find_header_files()

# ── non-vacuity (any root) ────────────────────────────────────────────────────
if not disk:
    print("derived_block_license_guard: BLOCK — NO_DERIVED_BLOCKS_FOUND: zero "
          "@ax-codified-from files under templates/ — a collapsed glob or relocated "
          "tree would otherwise report PASS about an empty set.", file=sys.stderr)
    sys.exit(2)

# ── live-mode floor ────────────────────────────────────────────────────────────
if is_live and len(disk) < live_min:
    print(f"derived_block_license_guard: BLOCK — LIVE_FLOOR_BREACH: {len(disk)} "
          f"header-bearing file(s) found on the live root, below the pinned floor "
          f"LIVE_MIN_DERIVED={live_min}.", file=sys.stderr)
    sys.exit(2)

# ── load ledger ────────────────────────────────────────────────────────────────
ledger_path = templates_dir / "DERIVED-SOURCES.yaml"
if not ledger_path.is_file():
    print(f"derived_block_license_guard: BLOCK — ledger not found: {ledger_path}", file=sys.stderr)
    sys.exit(2)
try:
    doc = yaml.safe_load(ledger_path.read_text(encoding="utf-8")) or {}
except yaml.YAMLError as e:
    print(f"derived_block_license_guard: BLOCK — cannot parse ledger: {e}", file=sys.stderr)
    sys.exit(2)

entries = doc.get("derived") or []

violations = []
seen_paths = {}
REQUIRED_FIELDS = ("path", "upstream", "upstream_license", "included_in_package", "codified_at")

for idx, entry in enumerate(entries):
    if not isinstance(entry, dict):
        violations.append(f"MALFORMED_ENTRY — derived[{idx}] is not a mapping: {entry!r}")
        continue
    path = entry.get("path")
    path_key = str(path) if path is not None else f"<missing path at derived[{idx}]>"

    # (6) required fields non-blank
    for field in REQUIRED_FIELDS:
        val = entry.get(field)
        if val is None or (isinstance(val, str) and val.strip() == ""):
            violations.append(f"MISSING_FIELD — {path_key}: '{field}' is blank or absent")

    # (3) one-to-one — duplicate path registration
    if path is not None:
        if path in seen_paths:
            violations.append(
                f"DUPLICATE_PATH — '{path}' is registered more than once "
                f"(derived[{seen_paths[path]}] and derived[{idx}])")
        else:
            seen_paths[path] = idx

    # (5) inclusion must be boolean true (not the string "true", not falsy)
    if entry.get("included_in_package") is not True:
        violations.append(
            f"FALSE_INCLUSION — {path_key}: included_in_package is "
            f"{entry.get('included_in_package')!r}, expected true")

    # (2) reverse existence — registered path must exist on disk
    if path is not None and path not in disk:
        violations.append(f"STALE_ENTRY — '{path}' is registered but not found on disk")

    # (4) provenance — registered upstream must match the file's own header, verbatim
    # modulo whitespace-trim, for every entry whose path DOES exist on disk.
    if path is not None and path in disk:
        disk_upstream = disk[path]
        ledger_upstream = entry.get("upstream")
        ledger_upstream_norm = ledger_upstream.strip() if isinstance(ledger_upstream, str) else ledger_upstream
        if ledger_upstream_norm != disk_upstream:
            violations.append(
                f"WRONG_UPSTREAM — '{path}': ledger records {ledger_upstream!r} but the "
                f"file's own @ax-codified-from header says {disk_upstream!r}")

# (1) completeness — every header-bearing file on disk must be registered
registered_paths = set(seen_paths.keys())
for path in sorted(disk.keys()):
    if path not in registered_paths:
        violations.append(f"UNREGISTERED — '{path}' carries @ax-codified-from but is not in the ledger")

if violations:
    print("derived_block_license_guard: FAIL — provenance ledger invariant(s) violated:", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    sys.exit(1)

print(f"derived_block_license_guard: PASS — {len(disk)} derived block(s), ledger agrees "
      f"exactly with disk (registration, existence, uniqueness, provenance, inclusion, "
      f"fields all verified).")
sys.exit(0)
PY
