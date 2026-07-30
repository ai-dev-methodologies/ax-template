#!/usr/bin/env bash
# practices/evals/manifest_snapshot_integrity_guard.sh — BACKLOG P2-57 / PRD-final-4 W1b.
#
# WHY THIS GUARD EXISTS
# ---------------------
# The protected-anchor ratchet in evidence_quote_spotcheck_guard.sh pins 64 template
# citations to the TEXT OF SNAPSHOT BODIES. That pin is only as strong as the immutability of
# those bodies — and until this guard landed, NOTHING checksummed them. The wave-start census
# measured the consequence: of the 91 manifest ids that have a committed
# `<id>.snapshot.md` on disk, **71 had a `sha`/`bytes` pair that did not describe the file it
# claims to describe** — and no gate looked. Three of them (recharts-2026-05,
# next-intl-2026-05, kakao-postcode-2026-05) shared ONE sha across three different byte
# counts, which is not drift: a sha256 cannot be the digest of three different byte strings,
# so at least two of those three records were never computed from anything. stripe-billing
# recorded 1657 bytes for a 2089-byte file. Pinning 46 further identities to bodies in that
# state would have been a PAPER ratchet: the quote lock would hold while the thing quoted
# stayed freely editable.
#
# WHAT IT CHECKS — THREE DOMAINS, NEVER A CROSS-DOMAIN EQUALITY
# ------------------------------------------------------------
# The refresh transaction (curl → committed deterministic extractor → snapshot + manifest +
# receipts) is verified as a CHAIN of three checks in three DISTINCT digest domains. They are
# deliberately not collapsed into one comparison: a single "does everything agree" digest
# would be satisfiable by recomputing it, which is what a doctored refresh does.
#
#   (a) FILE domain — for EVERY manifest id in EITHER catalog whose `<id>.snapshot.md` exists,
#       the manifest's `sha` and `bytes` must equal `shasum -a 256` / `wc -c` of the WHOLE FILE
#       on disk. Divergence is MANIFEST_FILE_DIVERGED (exit 1) unless the (catalog, id) is
#       carried by the shrink-only allowlist described below.
#
#   (b) BODY domain — for every W1-TOUCHED id, the sha256 the snapshot's own header RECORDS for
#       its body must equal the sha256 recomputed from the file with the header stripped. The
#       header/body boundary is the FIRST literal `---\n\n` in the file (Lane A's authoring
#       convention, documented in docs/final4-laneA-handoff.md §9). Note the header records the
#       BODY's digest, never the file's: a self-referential file-sha inside the file it hashes
#       is unverifiable by construction.
#
#   (c) RECEIPT domain — that same body-sha must equal the id's `kind: assembly` row in
#       practices/upstream/_FETCH-RECEIPTS.yaml (or, for a single-URL id with no assembly row,
#       that URL's `kind: fetch` row), AND every per-URL receipt id the assembly references must
#       exist in the ledger. Anything else is RECEIPT_MISSING (exit 2). This is the check that
#       makes a fetch the ONLY legal way to refresh a snapshot: editing a body and its manifest
#       entry together — the natural way to launder a doctored snapshot past check (a) — leaves
#       no receipt describing the new bytes, and there is no way to write one without recording
#       a URL, an HTTP status and a fetched_at.
#
# "W1-TOUCHED" IS DERIVED, NOT LISTED
# ----------------------------------
# The touched set is exactly the set of (catalog, snapshot_id) pairs carrying a `kind: assembly`
# receipt row. A hardcoded list would rot the moment a later wave refreshes another id, and
# worse, it would be the guard's author asserting which ids are covered rather than the
# committed receipts ledger showing it. Consequence, stated: refreshing an id WITHOUT appending
# an assembly row leaves it in the (a)-only population, which is why (a) applies to everything
# and the allowlist is shrink-only — a silently-refreshed id lands in (a) as a divergence that
# is not in the frozen baseline, and additions to the allowlist are rejected.
#
# ALLOWLIST — SHRINK-ONLY, REASON-BEARING, NON-REDUNDANT
# -----------------------------------------------------
# practices/evals/manifest_snapshot_integrity_allowlist.yaml. The honest position: for 63
# pre-existing ids the ORIGINALLY-FETCHED body is lost, so the recorded sha/bytes are
# unverifiable-or-fabricated HISTORY that cannot be corrected without a re-fetch. Suppressing
# them is what lets this guard block on everything else today instead of being registered as
# advisory and never promoted. Four mechanics keep that suppression from becoming a hiding
# place, and all four are enforced by this guard rather than by convention:
#   1. `baseline_universe` — the wave-start divergent census (71 entries) is FROZEN in the
#      allowlist. Every entry must be in it; ADDITIONS ARE REJECTED (exit 2). New divergence can
#      therefore never be allowlisted, only fixed.
#   2. unique (catalog, id) keys — a duplicate is exit 2. A count is not an identity.
#   3. NON-REDUNDANCY — an entry whose manifest NOW matches disk is STALE and FAILS (exit 1).
#      Without this, the list could be padded with already-clean ids so that its length stopped
#      describing the residual, and burn-down progress would be invisible.
#   4. per-entry `reason:` — a non-empty string. An unexplained suppression is a suppression
#      nobody will ever revisit.
# Net effect: the list can only shrink, and its length IS the residual (63 at introduction).
#
# NON-VACUITY
# -----------
# Zero ids checked is exit 2, never a green pass. On the live tree two census floors additionally
# apply (LIVE_MIN_IDS / LIVE_MIN_TOUCHED, measured at introduction), so deleting snapshots or
# emptying the receipts ledger cannot turn this gate into a green nothing.
#
# EXIT: 0 PASS · 1 divergence / stale allowlist / body-sha mismatch · 2 structural
#       (RECEIPT_MISSING, allowlist shape or subset violation, missing inputs, zero scan).
#
# Usage:
#   bash practices/evals/manifest_snapshot_integrity_guard.sh
#   bash practices/evals/manifest_snapshot_integrity_guard.sh --root DIR
#   bash practices/evals/manifest_snapshot_integrity_guard.sh --allowlist PATH
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SELF_REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"

ROOT_OVERRIDE=""
ALLOWLIST_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --allowlist) ALLOWLIST_OVERRIDE="$2"; shift 2 ;;
        --allowlist=*) ALLOWLIST_OVERRIDE="${1#--allowlist=}"; shift ;;
        *) echo "manifest_snapshot_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

REPO_ROOT="${ROOT_OVERRIDE:-$SELF_REPO_ROOT}"
# LIVE_ROOT keyed on the CANONICALIZED physical path, not on "was --root passed?" — the
# evidence_quote_spotcheck_guard round-4 finding: keying on arg presence lets an explicit
# `--root <the actual repo>` resolve to the identical tree while silently dropping every
# live-only floor.
RESOLVED_ROOT="$(cd "$REPO_ROOT" 2>/dev/null && pwd -P)" || {
    echo "manifest_snapshot_integrity_guard: cannot resolve root: $REPO_ROOT" >&2; exit 2; }
LIVE_ROOT=0
[ "$RESOLVED_ROOT" = "$SELF_REPO_ROOT" ] && LIVE_ROOT=1

command -v python3 >/dev/null 2>&1 || {
    echo "manifest_snapshot_integrity_guard: python3 required" >&2; exit 2; }

LIVE_ROOT="$LIVE_ROOT" ALLOWLIST_OVERRIDE="$ALLOWLIST_OVERRIDE" \
python3 - "$RESOLVED_ROOT" << 'PY'
import hashlib, os, sys

root = sys.argv[1]
live_root = os.environ.get("LIVE_ROOT") == "1"
allowlist_override = os.environ.get("ALLOWLIST_OVERRIDE") or ""

try:
    import yaml
except ImportError:
    print("manifest_snapshot_integrity_guard: PyYAML unavailable — cannot run", file=sys.stderr)
    sys.exit(2)

CATALOGS = ("practices", "practices-react")
ALLOWLIST_REL = os.path.join("practices", "evals",
                             "manifest_snapshot_integrity_allowlist.yaml")

# Census floors, measured on the live tree at introduction (2026-07-30): 91 manifest ids have a
# committed snapshot body; 8 of them are W1-touched (7 distinct snapshot files — stripe-billing
# is byte-identical in both catalogs and is registered once per catalog). MAY NOT BE REDUCED:
# they exist so that deleting snapshots, or emptying the receipts ledger, fails loudly instead
# of shrinking the checked population toward a green nothing.
LIVE_MIN_IDS = 91
LIVE_MIN_TOUCHED = 8

HEADER_DIVIDER = "---\n\n"
BODY_SHA_LABEL = "Body SHA-256"

structural = []   # exit 2
findings = []     # exit 1


def die_structural(msg):
    print(f"manifest_snapshot_integrity_guard: {msg}", file=sys.stderr)
    sys.exit(2)


def load_yaml(path, what):
    try:
        return yaml.safe_load(open(path, encoding="utf-8"))
    except Exception as exc:
        die_structural(f"PARSE_ERROR — {what} ({path}): {exc}")


# ── (a) FILE domain census: manifest sha/bytes vs the whole file on disk ──────────────
disk = {}          # (catalog, id) -> (file_sha, file_bytes)
recorded = {}      # (catalog, id) -> (manifest_sha, manifest_bytes)
manifest_seen = set()
for catalog in CATALOGS:
    mpath = os.path.join(root, catalog, "upstream", "_MANIFEST.yaml")
    if not os.path.isfile(mpath):
        continue
    doc = load_yaml(mpath, f"{catalog} manifest") or {}
    entries = doc.get("snapshots") or []
    if not isinstance(entries, list):
        die_structural(f"MANIFEST_SHAPE — {catalog}/upstream/_MANIFEST.yaml `snapshots` is "
                       f"{type(entries).__name__}, expected a list")
    for entry in entries:
        if not isinstance(entry, dict) or "id" not in entry:
            continue
        sid = entry["id"]
        if not isinstance(sid, str):
            die_structural(f"MANIFEST_NON_STRING_ID — {catalog}/upstream/_MANIFEST.yaml has an "
                           f"id of type {type(sid).__name__} ({sid!r}); an id is an identity "
                           "component and is never coerced")
        key = (catalog, sid)
        if key in manifest_seen:
            die_structural(f"MANIFEST_DUPLICATE_ID — {catalog}::{sid} is declared twice; two "
                           "records for one id mean the answer depends on which one a reader "
                           "stops at")
        manifest_seen.add(key)
        spath = os.path.join(root, catalog, "upstream", sid + ".snapshot.md")
        if not os.path.isfile(spath):
            # No committed body — out of scope by construction (there is nothing to checksum).
            # Reported in the summary so the population is visible, never silently dropped.
            continue
        raw = open(spath, "rb").read()
        disk[key] = (hashlib.sha256(raw).hexdigest(), len(raw))
        recorded[key] = (entry.get("sha"), entry.get("bytes"))

divergent_keys = set()
for key, (file_sha, file_bytes) in sorted(disk.items()):
    e_sha, e_bytes = recorded[key]
    if e_sha != file_sha or e_bytes != file_bytes:
        divergent_keys.add(key)

# ── allowlist: shape, subset-only, uniqueness, non-redundancy, reasons ────────────────
allow_path = allowlist_override or os.path.join(root, ALLOWLIST_REL)
allowed = {}       # (catalog, id) -> reason
baseline = set()
if not os.path.isfile(allow_path):
    if live_root:
        die_structural(f"ALLOWLIST_MISSING — {ALLOWLIST_REL} not found under {root}; the live "
                       "tree must carry the reason-bearing residual list (an absent allowlist "
                       "is an unreviewed suppression of every divergence, or a gate that "
                       "cannot pass — neither is a legible state)")
else:
    adoc = load_yaml(allow_path, "allowlist") or {}
    if not isinstance(adoc, dict):
        die_structural(f"ALLOWLIST_SHAPE — {allow_path} is not a mapping")
    raw_baseline = adoc.get("baseline_universe")
    if not isinstance(raw_baseline, list) or not raw_baseline:
        die_structural("ALLOWLIST_NO_BASELINE — the allowlist must declare a non-empty "
                       "`baseline_universe:` list (the FROZEN wave-start divergent census). "
                       "Without it, subset-only cannot be enforced and any future divergence "
                       "could be allowlisted away")
    for item in raw_baseline:
        if not isinstance(item, str) or "::" not in item:
            die_structural(f"ALLOWLIST_BASELINE_MALFORMED — baseline entry {item!r} "
                           "(expected '<catalog>::<id>')")
        cat, sid = item.split("::", 1)
        bkey = (cat.strip(), sid.strip())
        if bkey in baseline:
            die_structural(f"ALLOWLIST_BASELINE_DUPLICATE — {item!r} appears twice; the frozen "
                           "universe is a SET and a repeat inflates its declared size")
        baseline.add(bkey)
    declared_count = adoc.get("baseline_count")
    if declared_count is not None and declared_count != len(baseline):
        die_structural(f"ALLOWLIST_BASELINE_COUNT — declared baseline_count={declared_count} "
                       f"but the list holds {len(baseline)} unique entries")

    for entry in (adoc.get("entries") or []):
        if not isinstance(entry, dict):
            die_structural(f"ALLOWLIST_ENTRY_SHAPE — entry {entry!r} is not a mapping")
        cat, sid, reason = entry.get("catalog"), entry.get("id"), entry.get("reason")
        if not isinstance(cat, str) or cat not in CATALOGS:
            die_structural(f"ALLOWLIST_ENTRY_CATALOG — entry {entry!r} `catalog` must be one of "
                           f"{CATALOGS}")
        if not isinstance(sid, str) or not sid.strip():
            die_structural(f"ALLOWLIST_ENTRY_ID — entry {entry!r} `id` must be a non-empty string")
        if not isinstance(reason, str) or not reason.strip():
            die_structural(f"ALLOWLIST_ENTRY_NO_REASON — {cat}::{sid} carries no non-empty "
                           "`reason:`. An unexplained suppression is one nobody revisits")
        key = (cat, sid)
        if key in allowed:
            die_structural(f"ALLOWLIST_DUPLICATE_KEY — {cat}::{sid} is listed more than once; "
                           "entries are keyed by unique (catalog, id)")
        if key not in baseline:
            die_structural(f"ALLOWLIST_NOT_IN_BASELINE — {cat}::{sid} is not in the frozen "
                           f"baseline_universe ({len(baseline)} entries). The allowlist is "
                           "SHRINK-ONLY: removal is the only legal edit, so divergence "
                           "introduced after the freeze can be fixed but never suppressed")
        allowed[key] = reason

    # NON-REDUNDANCY. Checked in the allowlist's own domain (does this entry still describe a
    # real divergence?) rather than folded into the divergence loop, so a padded list fails even
    # when nothing else is wrong.
    for key in sorted(allowed):
        if key not in divergent_keys:
            cat, sid = key
            if key not in disk:
                findings.append(f"ALLOWLIST_STALE — {cat}::{sid} is allowlisted but has no "
                                "committed snapshot body to diverge from; remove the entry")
            else:
                findings.append(f"ALLOWLIST_STALE — {cat}::{sid} is allowlisted but its manifest "
                                "sha/bytes NOW MATCH disk. A suppression that suppresses "
                                "nothing makes the list stop describing the residual and hides "
                                "burn-down progress; remove the entry")

for key in sorted(divergent_keys):
    if key in allowed:
        continue
    cat, sid = key
    file_sha, file_bytes = disk[key]
    e_sha, e_bytes = recorded[key]
    findings.append(
        f"MANIFEST_FILE_DIVERGED — {cat}::{sid} manifest records sha={e_sha} bytes={e_bytes}, "
        f"disk file is sha={file_sha} bytes={file_bytes}. The manifest does not describe the "
        "file it claims to describe, so nothing pinned to that body is actually pinned")

# ── (b)+(c) BODY and RECEIPT domains for every W1-touched id ──────────────────────────
receipts_path = os.path.join(root, "practices", "upstream", "_FETCH-RECEIPTS.yaml")
fetch_rows = {}     # receipt id -> row
assembly = {}       # (catalog, snapshot_id) -> row
if os.path.isfile(receipts_path):
    rdoc = load_yaml(receipts_path, "receipts ledger") or {}
    rows = rdoc.get("receipts") or []
    if not isinstance(rows, list):
        die_structural("RECEIPTS_SHAPE — practices/upstream/_FETCH-RECEIPTS.yaml `receipts` is "
                       f"{type(rows).__name__}, expected a list")
    for row in rows:
        if not isinstance(row, dict):
            continue
        kind = row.get("kind")
        rid = row.get("id")
        if kind == "fetch":
            if rid in fetch_rows:
                die_structural(f"RECEIPTS_DUPLICATE_ID — fetch receipt id {rid!r} appears twice; "
                               "a receipt id is referenced by assembly rows and must resolve to "
                               "exactly one record")
            fetch_rows[rid] = row
        elif kind == "assembly":
            akey = (row.get("catalog"), row.get("snapshot_id"))
            if akey in assembly:
                die_structural(f"RECEIPTS_DUPLICATE_ASSEMBLY — {akey[0]}::{akey[1]} carries two "
                               "assembly rows; the body digest of one identity is one value")
            assembly[akey] = row

touched = sorted(assembly)
for key in touched:
    cat, sid = key
    row = assembly[key]
    spath = os.path.join(root, cat, "upstream", str(sid) + ".snapshot.md")
    if not os.path.isfile(spath):
        die_structural(f"RECEIPT_ORPHANED — {cat}::{sid} has an assembly receipt but no "
                       f"{sid}.snapshot.md on disk; a receipt for a body that does not exist "
                       "describes nothing")
    if key in allowed:
        die_structural(f"TOUCHED_ID_ALLOWLISTED — {cat}::{sid} was refreshed in this wave (it "
                       "carries an assembly receipt) AND is allowlisted. A freshly-fetched id "
                       "has a verifiable digest by construction; suppressing it would be "
                       "laundering the one population the chain can prove")

    raw_text = open(spath, encoding="utf-8", errors="replace").read()
    idx = raw_text.find(HEADER_DIVIDER)
    if idx == -1:
        die_structural(f"NO_HEADER_DIVIDER — {cat}::{sid} has no literal '---' + blank line "
                       "separating its provenance header from its body; the body cannot be "
                       "isolated, so its digest cannot be recomputed")
    body = raw_text[idx + len(HEADER_DIVIDER):]
    header = raw_text[:idx]

    # (b) BODY domain — the header's own recorded digest vs a recompute over the stripped body.
    header_sha = None
    for line in header.splitlines():
        if BODY_SHA_LABEL in line and ":" in line:
            candidate = line.rsplit(":", 1)[1].strip().strip("*` ")
            if len(candidate) == 64 and all(c in "0123456789abcdef" for c in candidate.lower()):
                header_sha = candidate.lower()
                break
    if header_sha is None:
        die_structural(f"HEADER_BODY_SHA_MISSING — {cat}::{sid} header declares no "
                       f"'{BODY_SHA_LABEL}' line carrying a 64-hex digest. A refreshed snapshot "
                       "records the digest of its own body; without it there is nothing for the "
                       "receipt chain to attach to")
    body_sha = hashlib.sha256(body.encode("utf-8")).hexdigest()
    if header_sha != body_sha:
        findings.append(
            f"HEADER_BODY_SHA_MISMATCH — {cat}::{sid} header records body sha256={header_sha} "
            f"but the body below the divider hashes to {body_sha}. The body was edited after "
            "the header was written (or the header was written about different bytes)")

    # (c) RECEIPT domain — the same body digest must appear in the committed fetch ledger.
    receipt_sha = row.get("body_sha256")
    if not isinstance(receipt_sha, str) or len(receipt_sha) != 64:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} carries no "
                       "64-hex `body_sha256`; a receipt without a digest records that a fetch "
                       "happened but not what it produced")
    if receipt_sha.lower() != body_sha:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} body hashes to {body_sha} but no receipt "
                       f"describes those bytes (assembly row {row.get('id')!r} records "
                       f"{receipt_sha}). A snapshot and its manifest entry edited together "
                       "WITHOUT a new fetch is exactly this state: refresh is only legal through "
                       "a fetch that appends receipts")
    declared_body_bytes = row.get("body_bytes")
    if declared_body_bytes is not None and declared_body_bytes != len(body.encode("utf-8")):
        findings.append(
            f"RECEIPT_BODY_BYTES — {cat}::{sid} assembly row records body_bytes="
            f"{declared_body_bytes} but the body is {len(body.encode('utf-8'))} bytes")
    refs = row.get("from_receipts") or []
    if not isinstance(refs, list) or not refs:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} references "
                       "no per-URL fetch receipts; an assembly with no inputs is a digest with "
                       "no provenance")
    absent = [r for r in refs if r not in fetch_rows]
    if absent:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} references "
                       f"fetch receipt(s) absent from the ledger: {', '.join(map(str, absent))}. "
                       "Every URL in the chain must be recorded, or the chain has a link nobody "
                       "can inspect")

# ── non-vacuity ──────────────────────────────────────────────────────────────────────
if not disk:
    die_structural("ZERO_SCAN — no manifest id with a committed snapshot body was found under "
                   f"{root}; a run that checksums nothing is not a clean tree")
if live_root:
    if len(disk) < LIVE_MIN_IDS:
        die_structural(f"CENSUS_FLOOR — {len(disk)} manifest id(s) with a committed body < the "
                       f"guard-pinned live floor {LIVE_MIN_IDS} (LIVE_MIN_IDS may not be "
                       "reduced). Snapshot bodies were deleted, which shrinks what this gate "
                       "checks rather than making it pass")
    if len(touched) < LIVE_MIN_TOUCHED:
        die_structural(f"CENSUS_FLOOR — {len(touched)} assembly receipt(s) < the guard-pinned "
                       f"live floor {LIVE_MIN_TOUCHED} (LIVE_MIN_TOUCHED may not be reduced). "
                       "The three-domain chain only runs on ids that have one, so emptying the "
                       "ledger would silently retire it")

if structural:
    for msg in structural:
        print(f"manifest_snapshot_integrity_guard: {msg}", file=sys.stderr)
    sys.exit(2)

if findings:
    for msg in findings:
        print(f"VIOLATION: {msg}", file=sys.stderr)
    print("", file=sys.stderr)
    print(f"manifest_snapshot_integrity_guard: {len(findings)} integrity violation(s) — BLOCKED",
          file=sys.stderr)
    sys.exit(1)

print(f"manifest_snapshot_integrity_guard: PASS — {len(disk)} manifest id(s) with a committed "
      f"body checksummed against disk ({len(allowed)} carried by the shrink-only residual "
      f"allowlist, baseline universe {len(baseline)}); {len(touched)} W1-touched id(s) verified "
      "through the full file←body←receipt chain")
sys.exit(0)
PY
