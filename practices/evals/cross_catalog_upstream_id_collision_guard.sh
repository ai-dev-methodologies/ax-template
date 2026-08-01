#!/usr/bin/env bash
# practices/evals/cross_catalog_upstream_id_collision_guard.sh — BACKLOG P3-137.
#
# WHAT IT FORBIDS
# ---------------
# ONE `upstream_id` registered in BOTH catalogs (`practices/upstream/_MANIFEST.yaml` and
# `practices-react/upstream/_MANIFEST.yaml`) that names TWO DIFFERENT SOURCES.
#
# WHY THAT IS A TRUTH DEFECT AND NOT A TIDINESS ONE
# -------------------------------------------------
# `evidence_quote_spotcheck_guard.resolve_snapshot_any_catalog` tries `practices/upstream/`
# FIRST and only then `practices-react/upstream/`. A `templates/**` citation therefore names
# an id, not a catalog — and which BODY that id resolves to is decided by that loop, not by
# the citing file. When the two catalogs hold different pages under one id, whether a quoted
# sentence is TRUE depends on a resolution order nobody reading the citation can see.
#
# This was measured, not theorised (Lane G, 2026-08-01): with `wcag-22-techniques-2026-05`
# holding the SC 4.1.3 *Understanding* page on the practices side and the *Techniques INDEX*
# on the react side, renaming the practices-side copy away in a throwaway clone flipped the
# protected template sweep from `exit 0` to `exit 1 with 4 findings` — the same two citations,
# same bytes, opposite verdict, decided entirely by which catalog answered.
#
# WHY "SAME SOURCE" AND NOT "FORBID SHARING OUTRIGHT"
# --------------------------------------------------
# MEASURED on the live tree at introduction: 61 practices ids × 42 practices-react ids share
# exactly 4 ids — `next-themes-2026-05`, `stripe-billing-2026-05`, `toss-billing-2026-05`
# (identical `source` on both sides: one upstream fact legitimately cited by both catalogs)
# and `wcag-22-techniques-2026-05` (two different pages). Forbidding shared ids outright would
# BLOCK the three honest ones to catch the one dishonest one. The defect is not that a name is
# reused; it is that a name MEANS TWO THINGS. So the invariant is agreement of `source`.
#
# THE GRANDFATHERED PAIR, AND WHY IT COULD NOT SIMPLY BE RENAMED
# -------------------------------------------------------------
# The one live collision is NOT dissolvable by an id rename, on either side, and that was
# measured rather than assumed (Lane H, first-hand, in throwaway clones of cf42258):
#   · react side  — `git mv` the snapshot + retitle the manifest id
#                   → manifest_snapshot_integrity_guard exit 2 `RECEIPT_ORPHANED`
#                     ("has an assembly receipt but no <id>.snapshot.md on disk").
#   · practices side — the same edit
#                   → manifest_snapshot_integrity_guard exit 2 `RECEIPT_ORPHANED`
#                   AND evidence_quote_spotcheck (protected) exit 1, 66 file(s) / 70 anchor(s)
#                     / 4 finding(s), because the two pinned template anchors stop resolving.
# `_FETCH-RECEIPTS.yaml` is append-only and binds an id to the body a fetch produced; an id is
# therefore part of a provenance record, not a label that can be re-spelled. Relabelling would
# be exactly the "manufacture a provenance claim" move the receipts chain exists to prevent.
# Deleting the react-side registration is refused for the same reason (its receipt orphans),
# and Lane G separately measured that a count-preserving RESPELL of a PINNED identity is
# refused by the protected-anchor ratchet with exit 5 `PROTECTED_IDENTITY_REMOVED` — the pin
# set may only GROW.
#
# So the pair is GRANDFATHERED, in a FROZEN set that cannot grow without editing this file:
# the value of this guard is that the NEXT collision is impossible to introduce silently, and
# grandfathering the one that provably cannot be dissolved is what lets the guard ship GREEN
# instead of shipping red and being ignored. Two properties keep the exception from becoming
# padding:
#   · it may only be a collision that ACTUALLY EXISTS — a grandfathered id that no longer
#     collides is `STALE_GRANDFATHER` (exit 1), so burn-down is visible and the set shrinks;
#   · it is a literal in this file, printed on every run, so growing it is a reviewed edit to
#     a guard rather than a line appended to a data file.
#
# Exit codes:
#   0  PASS — every shared id agrees on `source` (grandfathered collisions reported)
#   1  COLLISION (a shared id names two sources and is not grandfathered)
#      or STALE_GRANDFATHER (a grandfathered id no longer collides — remove it)
#      or SHARED_SNAPSHOT_UNREGISTERED (a body exists in both catalogs but one side has no
#         manifest entry, so which source it claims cannot be compared at all)
#   2  usage / setup / vacuity error (missing manifest, unparseable yaml, or — on a LIVE root
#      only — fewer shared ids than the pinned floor, i.e. the guard is checking nothing)
#
# Usage:
#   bash practices/evals/cross_catalog_upstream_id_collision_guard.sh
#   bash practices/evals/cross_catalog_upstream_id_collision_guard.sh --root DIR
#   bash practices/evals/cross_catalog_upstream_id_collision_guard.sh --show   # print census

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
SHOW=0
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="${2:-}"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --show) SHOW=1; shift ;;
        *) echo "cross_catalog_upstream_id_collision_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

IS_LIVE=1
if [ -n "$ROOT_OVERRIDE" ]; then
    REPO_ROOT="$(cd "$ROOT_OVERRIDE" 2>/dev/null && pwd)" || {
        echo "cross_catalog_upstream_id_collision_guard: cannot cd $ROOT_OVERRIDE" >&2; exit 2; }
    IS_LIVE=0
fi

PY_BIN="${AX_PY_BIN:-python3}"
command -v "$PY_BIN" >/dev/null 2>&1 || {
    echo "cross_catalog_upstream_id_collision_guard: python3 not found" >&2; exit 2; }

AX_ROOT="$REPO_ROOT" AX_IS_LIVE="$IS_LIVE" AX_SHOW="$SHOW" "$PY_BIN" - <<'PY'
import os, sys, pathlib, re

ROOT = pathlib.Path(os.environ["AX_ROOT"])
IS_LIVE = os.environ["AX_IS_LIVE"] == "1"
SHOW = os.environ["AX_SHOW"] == "1"

# ── the FROZEN grandfather set ────────────────────────────────────────────────
# Ids whose cross-catalog collision predates this guard AND is refused by the provenance
# ratchets (see the header for the measured exit codes). Growing this set is an edit to this
# file. Shrinking it is the intended direction and requires the collision to be really gone.
GRANDFATHERED_COLLISIONS = frozenset({
    "wcag-22-techniques-2026-05",
})

# Live-root non-vacuity floor: the number of ids that ACTUALLY appear in both catalogs. If the
# census collapses (a manifest emptied, a parser silently returning nothing) the guard would
# report a green "no collisions" about an empty set. Measured at introduction: 4.
LIVE_MIN_SHARED = 4

CATALOGS = ("practices", "practices-react")

def die(code, msg):
    print(f"cross_catalog_upstream_id_collision_guard: {msg}", file=sys.stderr)
    sys.exit(code)

# ── parse both manifests ──────────────────────────────────────────────────────
# PyYAML when available; otherwise a deliberately narrow line reader for the two fields this
# guard compares. The fallback is not a general yaml parser and says so — it reads `- id: "x"`
# and the `source:` that follows it inside the same list item, which is the shape
# practices/scripts/snapshot-extract.sh writes.
def load_manifest(path):
    text = path.read_text(encoding="utf-8", errors="replace")
    try:
        import yaml  # type: ignore
        doc = yaml.safe_load(text) or {}
        snaps = doc.get("snapshots")
        if not isinstance(snaps, list):
            die(2, f"MANIFEST_SHAPE — {path} has no `snapshots:` list")
        out = {}
        for s in snaps:
            if not isinstance(s, dict):
                die(2, f"MANIFEST_SHAPE — {path} contains a non-mapping snapshot entry")
            sid = s.get("id")
            if not isinstance(sid, str) or not sid.strip():
                die(2, f"MANIFEST_SHAPE — {path} contains an entry with no usable `id`")
            src = s.get("source")
            out[sid.strip()] = src.strip() if isinstance(src, str) else None
        return out
    except ImportError:
        pass
    out, cur = {}, None
    for raw in text.splitlines():
        m = re.match(r'^\s*-\s+id:\s*"?([^"\r\n]+?)"?\s*$', raw)
        if m:
            cur = m.group(1).strip()
            out.setdefault(cur, None)
            continue
        m = re.match(r'^\s+source:\s*"?([^"\r\n]+?)"?\s*$', raw)
        if m and cur is not None and out.get(cur) is None:
            out[cur] = m.group(1).strip()
    if not out:
        die(2, f"MANIFEST_SHAPE — {path} yielded no ids (PyYAML absent, fallback read nothing)")
    return out

manifests = {}
for cat in CATALOGS:
    p = ROOT / cat / "upstream" / "_MANIFEST.yaml"
    if not p.is_file():
        die(2, f"MANIFEST_MISSING — {p} does not exist")
    if p.is_symlink():
        die(2, f"MANIFEST_NOT_REGULAR — {p} is a symlink; the file this guard compares must "
               "be the file git records")
    manifests[cat] = load_manifest(p)

# ── snapshot bodies on disk, so an UNREGISTERED shared body cannot hide ───────
# Resolution is by FILE, so a body present in both catalogs matters even if a manifest forgot
# it — and in that case the sources cannot be compared at all, which is worse, not better.
bodies = {}
for cat in CATALOGS:
    d = ROOT / cat / "upstream"
    bodies[cat] = {f.name[: -len(".snapshot.md")]
                   for f in d.glob("*.snapshot.md")} if d.is_dir() else set()

shared_manifest = set(manifests[CATALOGS[0]]) & set(manifests[CATALOGS[1]])
shared_bodies = bodies[CATALOGS[0]] & bodies[CATALOGS[1]]
shared = sorted(shared_manifest | shared_bodies)

if IS_LIVE and len(shared) < LIVE_MIN_SHARED:
    die(2, f"CENSUS_COLLAPSED — {len(shared)} id(s) shared across the two catalogs, floor is "
           f"{LIVE_MIN_SHARED}. A green 'no collisions' over a collapsed census is a green "
           "nothing; re-check that both manifests parsed.")

collisions, grandfathered, unregistered, agreeing = [], [], [], []
for sid in shared:
    a = manifests[CATALOGS[0]].get(sid, "__ABSENT__")
    b = manifests[CATALOGS[1]].get(sid, "__ABSENT__")
    if a == "__ABSENT__" or b == "__ABSENT__" or a is None or b is None:
        unregistered.append((sid, a, b))
        continue
    # [87] fixture_kill_proof anchor. Neutering this comparison to a constant must GREEN
    # fixtures/cross-catalog-upstream-id/fail_same_id_two_sources — that is what proves the
    # fail fixture depends on THIS detector and is not passing for some incidental reason.
    differs = a != b
    if not differs:
        agreeing.append((sid, a))
    elif sid in GRANDFATHERED_COLLISIONS:
        grandfathered.append((sid, a, b))
    else:
        collisions.append((sid, a, b))

# NON-REDUNDANCY: a grandfathered id that is no longer a collision is padding.
# TWO branches, because "not colliding" has two shapes and only one of them is meaningful on an
# arbitrary root. A fixture root has no reason to contain this repo's grandfathered ids at all,
# so ABSENCE may not be a violation there — otherwise every fixture in the tree would have to
# carry them, and the exception's own test would be untestable.
#   · SHARED-BUT-AGREEING (any root): the id IS registered in both catalogs and the two sources
#     now match. The collision is demonstrably gone; the exception must go with it.
#   · ABSENT (live root only): the id is not shared at all any more. On the real tree that also
#     means the exception outlived its subject; on a fixture root it means nothing.
agreeing_ids = {s for s, _ in agreeing}
colliding_now = {s for s, _, _ in grandfathered} | {s for s, _, _ in collisions}
stale = sorted(GRANDFATHERED_COLLISIONS & agreeing_ids)
if IS_LIVE:
    stale = sorted(set(stale) | (GRANDFATHERED_COLLISIONS - colliding_now))

if SHOW:
    print(f"shared ids ({len(shared)}):")
    for sid, src in agreeing:
        print(f"  AGREE        {sid}\n                 {src}")
    for sid, a, b in grandfathered:
        print(f"  GRANDFATHER  {sid}\n                 {CATALOGS[0]}: {a}\n                 {CATALOGS[1]}: {b}")
    for sid, a, b in collisions:
        print(f"  COLLISION    {sid}\n                 {CATALOGS[0]}: {a}\n                 {CATALOGS[1]}: {b}")
    for sid, a, b in unregistered:
        print(f"  UNREGISTERED {sid}\n                 {CATALOGS[0]}: {a}\n                 {CATALOGS[1]}: {b}")

violations = 0
for sid, a, b in collisions:
    violations += 1
    print(f"VIOLATION: COLLISION — upstream_id `{sid}` is registered in BOTH catalogs against "
          f"DIFFERENT sources:\n    {CATALOGS[0]}/upstream: {a}\n    {CATALOGS[1]}/upstream: {b}\n"
          "  templates/** citations resolve practices-FIRST, so which body answers — and "
          "therefore whether a quoted sentence is true — depends on a resolution order the "
          "citing file cannot express. Give the two bodies two ids.", file=sys.stderr)

for sid, a, b in unregistered:
    violations += 1
    print(f"VIOLATION: SHARED_SNAPSHOT_UNREGISTERED — `{sid}.snapshot.md` exists in both "
          f"catalogs but the sources cannot be compared "
          f"({CATALOGS[0]}={a!r}, {CATALOGS[1]}={b!r}). Register both sides with a `source:`.",
          file=sys.stderr)

for sid in stale:
    violations += 1
    print(f"VIOLATION: STALE_GRANDFATHER — `{sid}` is in GRANDFATHERED_COLLISIONS but is not a "
          "live cross-catalog collision any more. Remove it from the frozen set; an exception "
          "kept past its subject is padding that hides the next one.", file=sys.stderr)

if violations:
    print(f"\ncross_catalog_upstream_id_collision_guard: {violations} violation(s) — BLOCKED",
          file=sys.stderr)
    sys.exit(1)

gf = ", ".join(sorted(s for s, _, _ in grandfathered)) or "none"
print(f"cross_catalog_upstream_id_collision_guard: PASS — {len(shared)} id(s) shared across "
      f"{len(manifests[CATALOGS[0]])} {CATALOGS[0]} + {len(manifests[CATALOGS[1]])} "
      f"{CATALOGS[1]} manifest ids; {len(agreeing)} agree on source, "
      f"{len(grandfathered)} grandfathered collision(s) [{gf}], 0 new")
sys.exit(0)
PY
