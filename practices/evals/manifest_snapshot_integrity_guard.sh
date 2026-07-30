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
# CHAIN SCOPE IS DERIVED — FROM THE LEDGER **AND** FROM THE RELEASE ANCHOR
# -----------------------------------------------------------------------
# The chain runs on the union of two derived sets, never a hardcoded list (which would rot the
# moment a later wave refreshes another id, and would be the guard's author asserting coverage
# rather than committed artifacts showing it):
#   · every (catalog, snapshot_id) carrying a `kind: assembly` receipt row; PLUS
#   · every id that DIFFERS FROM THE RELEASE ANCHOR — its `<id>.snapshot.md` differs from
#     `git show <ANCHOR>:<file>`, or its manifest `sha`/`bytes` entry differs from the ANCHOR's
#     manifest (P1-2a). A changed id with no assembly row dies RECEIPT_MISSING.
# Why the second half exists: deriving scope from the assembly rows alone let the LEDGER decide
# what gets verified, and the ledger is editable. The reviewer refreshed an allowlisted,
# non-assembly id (practices-react::cwv-2026) — edit body, sync manifest to the new file, delete
# the allowlist row — and (a) passed, the shrink was legal, and the chain never looked. Ids
# byte-identical to the released tree stay exempt, which is what keeps the ~20 legacy-clean,
# pre-receipt ids passing without a re-fetch.
#
# ANCHOR RATCHET — FLOORS AND BASELINES LIVE IN THE PREVIOUS RELEASE
# -----------------------------------------------------------------
# (P1-2 / P1-3, cross-family reviewer 2026-07-30; TD-2026-07-30-P1-anchor-ratchet.) Every
# "frozen" surface here used to be re-read from the same mutable tree that the commit under
# review is editing, so a coherent edit of all of them at once tripped nothing. Three checks now
# compare against `git show <ANCHOR>:<path>` (exit 4 for all three):
#   · RECEIPT_LEDGER_MUTATED — every row present in the ANCHOR's _FETCH-RECEIPTS.yaml must still
#     be present and unchanged; only APPENDS are legal. This kills rewriting an existing assembly
#     row's `body_sha256` to match a doctored body.
#   · BASELINE_MUTATED — `baseline_universe` must EQUAL the anchor's. Enforcing it against itself
#     was self-certifying: adding a newly-diverged id to the baseline satisfied subset-only.
#   · ALLOWLIST_GREW — residual `entries` must be a SUBSET of the anchor's; shrink-only ACROSS
#     RELEASES, so a new divergence can never be suppressed and must pass the full chain.
# Multiple assembly rows per identity are consequently LEGAL (a repeat refresh appends rather
# than rewrites): the chain binds to the LATEST assembly row per id, and append-only keeps every
# earlier row byte-intact, so supersession is auditable instead of a silent overwrite.
#
# WHY THIS IS SOUND (gate ordering): R25 runs with the tree at HEAD, which is AHEAD of
# origin/main — it is the commit being released — so origin/main is genuinely the prior state and
# is not editable by the commit under verification; the pre-push recency guard then binds every
# push to an R25 run at that HEAD. Anchor resolution: origin/main → HEAD (WEAKER: a
# detached/fork-fresh clone; a change already committed locally is present in the anchor itself)
# → unavailable (no git/no commits: the anchor checks WARN and are SKIPPED — inert in a tarball
# export, which is also unpushable, so the released path keeps the gate). An anchor predating a
# surface prints an advisory and skips that half (first-release bootstrap); git history is
# immutable to the working tree, so that state cannot be manufactured by the edit under review.
# FIXTURE ROOTS NEVER ANCHOR — every anchor check is gated on LIVE_ROOT, like the LIVE_MIN_*
# floors, because a fixture isolates one failure mode and has no release history of its own.
# Consequence, disclosed: the anchor checks are therefore NOT fixture-coverable (a fixture has no
# anchor to ratchet against), so their non-vacuity evidence is the live reproductions recorded in
# practices/DECISIONS.md TD-2026-07-30-P1-anchor-ratchet — each reproduced against the released
# copy of this guard (exit 0) and against this one (exit 4 / 2), then restored byte-identically.
#
# RESIDUAL, STATED RATHER THAN PAPERED OVER: a receipt is a SELF-REPORTED record, and no offline
# gate can prove that a curl actually happened. Append-only closes REWRITING a released receipt;
# it does not close APPENDING a fabricated one (doctor the body, sync manifest + header, then
# append a new fetch row and a new assembly row whose digest matches the doctored bytes). What the
# chain therefore guarantees is not "a doctored refresh is impossible" but "a doctored refresh
# leaves a permanent, immutable, reviewable claim in the ledger naming a URL, an HTTP status and a
# fetched_at" — undeniable in the record rather than prevented. Closing the remainder needs
# evidence the tree cannot author (an independent fetch at review time, e.g. the periodic network
# external_url_spot_audit.sh, or a signed transparency log). Two candidate tightenings were
# considered and deliberately NOT added here, because both carry false-positive risk that would
# have to be paid by future legitimate waves: requiring a newly-appended assembly row to cite at
# least one newly-appended fetch row (breaks registering an existing body under a second catalog),
# and freezing `notes:` (breaks documenting a later refresh).
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
#       (RECEIPT_MISSING, allowlist shape or in-tree subset violation, missing inputs, zero scan)
#       · 4 cross-release ratchet violation (RECEIPT_LEDGER_MUTATED / BASELINE_MUTATED /
#       ALLOWLIST_GREW) — deliberately distinct from 1 and 2 so "the ratchet was rolled back
#       across releases" is never readable as "a body diverged" or "a shape is wrong".
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

# ── RELEASE ANCHOR (P1 anchor-ratchet, TD-2026-07-30-P1-anchor-ratchet) ───────────────
# See the ANCHOR RATCHET header block. Resolution order origin/main (strong: R25 runs at a tree
# AHEAD of it) → HEAD (weaker; detached/fork-fresh only) → unavailable (WARN + skip). Gated on
# LIVE_ROOT exactly like the LIVE_MIN_* floors: FIXTURE ROOTS NEVER ANCHOR, since a fixture
# isolates one failure mode and has no release history of its own.
GIT_ANCHOR=""
GIT_ANCHOR_KIND="unavailable"
if [ "$LIVE_ROOT" = "1" ] && command -v git >/dev/null 2>&1 \
   && git -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    if git -C "$SELF_REPO_ROOT" rev-parse --verify --quiet origin/main >/dev/null 2>&1; then
        GIT_ANCHOR="origin/main"; GIT_ANCHOR_KIND="origin/main"
    elif git -C "$SELF_REPO_ROOT" rev-parse --verify --quiet HEAD >/dev/null 2>&1; then
        GIT_ANCHOR="HEAD"; GIT_ANCHOR_KIND="HEAD"
    fi
fi

LIVE_ROOT="$LIVE_ROOT" ALLOWLIST_OVERRIDE="$ALLOWLIST_OVERRIDE" \
GIT_ANCHOR="$GIT_ANCHOR" GIT_ANCHOR_KIND="$GIT_ANCHOR_KIND" GIT_REPO_ROOT="$SELF_REPO_ROOT" \
python3 - "$RESOLVED_ROOT" << 'PY'
import hashlib, os, re, subprocess, sys

root = sys.argv[1]
live_root = os.environ.get("LIVE_ROOT") == "1"
allowlist_override = os.environ.get("ALLOWLIST_OVERRIDE") or ""
anchor = os.environ.get("GIT_ANCHOR") or ""
anchor_kind = os.environ.get("GIT_ANCHOR_KIND") or "unavailable"
git_root = os.environ.get("GIT_REPO_ROOT") or root
anchored = bool(live_root and anchor)

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


# ── ANCHOR PLUMBING (P1-2 / P1-3) ─────────────────────────────────────────────────────
ANCHOR_EXIT = 4    # cross-release ratchet violation: distinct from 1 (findings) / 2 (structural)


def die_anchor(code, msg):
    print(f"manifest_snapshot_integrity_guard: {code} — {msg}", file=sys.stderr)
    sys.exit(ANCHOR_EXIT)


def anchor_warn(msg):
    print(f"manifest_snapshot_integrity_guard: WARN {msg}", file=sys.stderr)


def anchor_blob(rel):
    """Bytes of `rel` as of the release anchor, or None when that path did not exist there."""
    proc = subprocess.run(["git", "-C", git_root, "show", f"{anchor}:{rel}"],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return proc.stdout if proc.returncode == 0 else None


def anchor_yaml(rel, what):
    """Parsed YAML of `rel` at the anchor, or None when absent/unparseable (bootstrap)."""
    raw = anchor_blob(rel)
    if raw is None:
        return None
    try:
        return yaml.safe_load(raw.decode("utf-8", errors="replace"))
    except Exception as exc:
        anchor_warn(f"ANCHOR_UNPARSEABLE — {what} at {anchor_kind} does not parse ({exc}); the "
                    "ratchet that depends on it is SKIPPED. Git history is immutable to the "
                    "working tree, so this state cannot be manufactured by the edit under review")
        return None


def anchor_changed_paths():
    """Repo-relative paths under either catalog's upstream/ that DIFFER from the anchor.

    Two sources, and both are needed: `git diff --name-only <anchor>` covers tracked
    modifications/additions/deletions of the working tree against the released commit, and
    `git ls-files --others` covers UNTRACKED files — a brand-new snapshot body has no anchor
    blob and no diff entry, and leaving it invisible would re-open exactly the hole this
    check closes. Returns None when git cannot answer (caller degrades to a WARN)."""
    specs = [f"{c}/upstream" for c in CATALOGS]
    out = set()
    diff = subprocess.run(["git", "-C", git_root, "diff", "--name-only", anchor, "--", *specs],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if diff.returncode != 0:
        return None
    out.update(p for p in diff.stdout.decode("utf-8", "replace").splitlines() if p)
    others = subprocess.run(["git", "-C", git_root, "ls-files", "--others",
                             "--exclude-standard", "--", *specs],
                            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if others.returncode == 0:
        out.update(p for p in others.stdout.decode("utf-8", "replace").splitlines() if p)
    return out


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

# ── CHANGE-DRIVEN CHAIN SCOPE (P1-2a) ─────────────────────────────────────────────────
# The chain used to run on exactly the ids carrying a `kind: assembly` receipt — i.e. the guard
# asked the *receipts ledger* which ids to verify, and the ledger is one of the things an editor
# controls. So the reviewer refreshed an ALLOWLISTED, NON-ASSEMBLY id (practices-react::cwv-2026):
# edit the snapshot body, sync the manifest sha/bytes to the new file, delete its allowlist row.
# (a) passed (manifest now matches disk), the allowlist was smaller (legal, shrink-only), and the
# chain never looked because there was no assembly row — a refresh with no receipt at all.
#
# The scope is now derived from the RELEASE ANCHOR instead: an id whose snapshot FILE differs
# from `git show ANCHOR:<file>`, or whose manifest sha/bytes entry differs from the ANCHOR's
# manifest, is a CHANGED id and must pass the full 3-domain chain (header body-sha recompute +
# receipt). Ids that are byte-identical to the released tree stay exactly as exempt as they were
# — which is what keeps the ~20 legacy-clean, pre-receipt ids passing without a re-fetch.
#
# Scope is limited to ids that have a body on disk NOW: with no body there is nothing to
# checksum, which is the same boundary domain (a) already draws. A manifest edit for a
# body-less id therefore remains out of scope, stated rather than silently relied upon.
changed_keys = set()
if anchored:
    changed_paths = anchor_changed_paths()
    if changed_paths is None:
        anchor_warn("ANCHOR_DIFF_UNAVAILABLE — `git diff` against "
                    f"{anchor_kind} failed; change-driven chain scope SKIPPED (the assembly-row "
                    "population is still verified)")
    else:
        manifest_changed = set()
        for pth in changed_paths:
            for catalog in CATALOGS:
                prefix = f"{catalog}/upstream/"
                if not pth.startswith(prefix):
                    continue
                leaf = pth[len(prefix):]
                if leaf == "_MANIFEST.yaml":
                    manifest_changed.add(catalog)
                elif leaf.endswith(".snapshot.md"):
                    key = (catalog, leaf[: -len(".snapshot.md")])
                    if key in disk:
                        changed_keys.add(key)
        for catalog in sorted(manifest_changed):
            adoc_m = anchor_yaml(f"{catalog}/upstream/_MANIFEST.yaml", f"{catalog} manifest")
            prior_records = {}
            if isinstance(adoc_m, dict):
                for entry in (adoc_m.get("snapshots") or []):
                    if isinstance(entry, dict) and isinstance(entry.get("id"), str):
                        prior_records[entry["id"]] = (entry.get("sha"), entry.get("bytes"))
            for key in disk:
                if key[0] != catalog:
                    continue
                if prior_records.get(key[1]) != recorded[key]:
                    # Absent at the anchor (new id) or a rewritten sha/bytes pair — either way
                    # the manifest now makes a claim the released tree did not make.
                    changed_keys.add(key)
elif live_root:
    anchor_warn("ANCHOR_UNAVAILABLE — no git anchor (origin/main or HEAD) could be resolved, so "
                "change-driven chain scope is SKIPPED and only assembly-row ids run the full "
                "chain. A tree with no git history cannot be pushed either (the pre-push recency "
                "guard is a git hook), so the released path keeps the ratchet")
if anchored and anchor_kind != "origin/main":
    anchor_warn(f"ANCHOR_FALLBACK — ratcheting against {anchor_kind}, not origin/main. Weaker by "
                "construction: an edit that is already committed locally is present in the "
                "anchor itself")

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

    # ── P1-3: THE "FROZEN" BASELINE IS NOW FROZEN IN THE PREVIOUS RELEASE ─────────────
    # `baseline_universe` was described as FROZEN and enforced only against ITSELF: every entry
    # had to be a member of the very list the same editor was writing. So the reviewer took a
    # clean id (practices-react::mdn-promise-all), diverged its manifest record, and then added
    # it to baseline_universe + entries + bumped baseline_count — subset-only was satisfied
    # because the baseline had grown to contain it. "Frozen" that is re-read from the mutable
    # tree is not frozen; it is self-certifying.
    #
    # Both halves are therefore compared to the ANCHOR's copy of this same allowlist:
    #   · baseline_universe must be EQUAL to the anchor's (BASELINE_MUTATED) — frozen forever
    #     after the first release, in BOTH directions: an addition re-opens suppression, and a
    #     removal would silently shrink the universe that subset-only is measured against.
    #   · residual `entries` must be a SUBSET of the anchor's entries (ALLOWLIST_GREW) — the
    #     list may only shrink ACROSS RELEASES, so a new divergence can never enter it and must
    #     pass the full chain instead.
    # Deliberate consequence, disclosed: on the live tree the comparison is always against the
    # COMMITTED allowlist path at the anchor, even under --allowlist. Pointing the live run at
    # some other file therefore trips BASELINE_MUTATED rather than silently adopting that file's
    # baseline — fail-closed, since an override that redefines the frozen universe is the same
    # bypass by another route.
    if anchored:
        prior_allow = anchor_yaml(ALLOWLIST_REL.replace(os.sep, "/"), "allowlist")
        if prior_allow is None:
            anchor_warn(f"ANCHOR_ALLOWLIST_ABSENT — {ALLOWLIST_REL} does not exist at "
                        f"{anchor_kind}; nothing to ratchet against (first-release bootstrap). "
                        "Baseline-freeze and shrink-only checks SKIPPED for this run")
        elif not isinstance(prior_allow, dict):
            anchor_warn(f"ANCHOR_ALLOWLIST_SHAPE — {ALLOWLIST_REL} at {anchor_kind} is not a "
                        "mapping; baseline-freeze and shrink-only checks SKIPPED")
        else:
            prior_baseline = set()
            prior_baseline_ok = isinstance(prior_allow.get("baseline_universe"), list)
            for item in (prior_allow.get("baseline_universe") or []):
                if isinstance(item, str) and "::" in item:
                    pcat, psid = item.split("::", 1)
                    prior_baseline.add((pcat.strip(), psid.strip()))
                else:
                    prior_baseline_ok = False
            if not prior_baseline_ok or not prior_baseline:
                anchor_warn(f"ANCHOR_BASELINE_MALFORMED — the {anchor_kind} allowlist declares no "
                            "well-formed baseline_universe; baseline-freeze check SKIPPED")
            elif prior_baseline != baseline:
                added = sorted(baseline - prior_baseline)
                dropped = sorted(prior_baseline - baseline)
                detail = []
                if added:
                    detail.append("ADDED " + ", ".join(f"{c}::{i}" for c, i in added))
                if dropped:
                    detail.append("REMOVED " + ", ".join(f"{c}::{i}" for c, i in dropped))
                die_anchor("BASELINE_MUTATED",
                           f"baseline_universe differs from {anchor_kind}:{ALLOWLIST_REL} "
                           f"({len(prior_baseline)} → {len(baseline)} entries; "
                           + "; ".join(detail) + "). The wave-start census is FROZEN: an "
                           "ADDITION allowlists divergence introduced after the freeze (the "
                           "in-tree subset check cannot see it, because the list it checks "
                           "membership against is the list being edited), and a REMOVAL shrinks "
                           "the universe that shrink-only is measured against. Fix the "
                           "divergence, or re-fetch the id and record receipts")
            prior_entries = set()
            for entry in (prior_allow.get("entries") or []):
                if isinstance(entry, dict) and isinstance(entry.get("catalog"), str) \
                        and isinstance(entry.get("id"), str):
                    prior_entries.add((entry["catalog"], entry["id"]))
            grew = sorted(set(allowed) - prior_entries)
            if grew:
                die_anchor("ALLOWLIST_GREW",
                           f"{len(grew)} residual entry(ies) are not in the {anchor_kind} "
                           "allowlist: " + ", ".join(f"{c}::{i}" for c, i in grew)
                           + f" ({len(prior_entries)} → {len(allowed)} entries). The residual is "
                           "SHRINK-ONLY ACROSS RELEASES: removal is the only legal edit, so a "
                           "divergence that was not already suppressed by the previous release "
                           "must pass the full file←body←receipt chain, never be suppressed")

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
RECEIPTS_REL = "practices/upstream/_FETCH-RECEIPTS.yaml"
receipts_path = os.path.join(root, "practices", "upstream", "_FETCH-RECEIPTS.yaml")
receipts_raw = ""
fetch_rows = {}     # receipt id -> row
assembly = {}       # (catalog, snapshot_id) -> LATEST row for that identity
assembly_rows = 0
cur_rows = []
row_ids = set()
if os.path.isfile(receipts_path):
    receipts_raw = open(receipts_path, encoding="utf-8", errors="replace").read()
    rdoc = load_yaml(receipts_path, "receipts ledger") or {}
    cur_rows = rdoc.get("receipts") or []
    if not isinstance(cur_rows, list):
        die_structural("RECEIPTS_SHAPE — practices/upstream/_FETCH-RECEIPTS.yaml `receipts` is "
                       f"{type(cur_rows).__name__}, expected a list")
    for row in cur_rows:
        if not isinstance(row, dict):
            continue
        kind = row.get("kind")
        rid = row.get("id")
        # Row ids are unique across EVERY kind, not just within `fetch`: an assembly row is
        # addressed by id in the append-only ratchet below, so two rows sharing one id would make
        # "is the released row still intact?" depend on which one a reader stops at.
        if rid is not None:
            if rid in row_ids:
                die_structural(f"RECEIPTS_DUPLICATE_ID — receipt id {rid!r} appears twice; a "
                               "receipt id is referenced by assembly rows and by the append-only "
                               "ratchet, and must resolve to exactly one record")
            row_ids.add(rid)
        if kind == "fetch":
            fetch_rows[rid] = row
        elif kind == "assembly":
            # MULTIPLE ASSEMBLY ROWS PER IDENTITY ARE LEGAL (P1-2c). The ledger is append-only,
            # so a SECOND refresh of the same id appends a second assembly row rather than
            # rewriting the first — forbidding that (the previous RECEIPTS_DUPLICATE_ASSEMBLY)
            # would have forced every repeat refresh to MUTATE history, which is precisely what
            # the append-only ratchet must forbid. The chain therefore binds to the LATEST
            # assembly row per identity (last in file order = last appended), and the
            # append-only check below guarantees the older rows stay byte-intact, so the
            # supersession is auditable rather than a silent overwrite.
            akey = (row.get("catalog"), row.get("snapshot_id"))
            assembly[akey] = row
            assembly_rows += 1

# ── P1-2b: RECEIPTS LEDGER IS APPEND-ONLY vs THE ANCHOR ───────────────────────────────
# Domain (c) asks "does a receipt describe these bytes?" and the receipts file is in the tree,
# so the answer was writable: REWRITE an existing assembly row's `body_sha256` to the doctored
# body's digest and the chain closed on a receipt that had been edited to fit. (The old
# referenced-row-existence check only asked whether the `from_receipts` ids RESOLVE, never
# whether the row they resolve to still says what it said when it was written.)
#
# So every row present in the ANCHOR's ledger must still be present and unchanged here; only
# APPENDS are legal. Two layers, both reported as RECEIPT_LEDGER_MUTATED (exit 4):
#   1. PARSED-ROW identity — the anchor row's mapping must equal a current row of the same id.
#      Robust to reflowing/re-quoting, which is the layer that always runs.
#   2. BYTE-CHUNK identity — when both files chunk cleanly into column-0 `- id: <id>` list
#      items, the anchor's exact chunk text must appear verbatim (trailing blank lines
#      normalized, since those belong to the separator between rows and not to the row). This
#      catches an edit that parses identically. If either side does not chunk cleanly the layer
#      prints an advisory and only layer 1 applies — stated rather than assumed.
# Top-level keys other than `receipts` (e.g. `notes:`) are NOT frozen: prose about the ledger is
# not a provenance record, and freezing it would make documenting a later refresh impossible.


def receipt_chunks(text):
    """`id` -> exact chunk text, for a ledger whose list items start at column 0. None when the
    file does not have that shape (then the byte layer is skipped, not silently passed)."""
    chunks, cur_id, buf = {}, None, []
    for line in text.splitlines(keepends=True):
        if line.startswith("- "):
            if cur_id is not None:
                chunks[cur_id] = "".join(buf)
            m = re.match(r"- id:\s*(\S+)\s*$", line.rstrip("\n"))
            if not m:
                return None
            cur_id, buf = m.group(1).strip("'\""), [line]
        elif cur_id is not None:
            if line.startswith((" ", "\t")) or not line.strip():
                buf.append(line)
            else:
                chunks[cur_id] = "".join(buf)
                cur_id, buf = None, []
    if cur_id is not None:
        chunks[cur_id] = "".join(buf)
    return chunks or None


if anchored:
    prior_raw = anchor_blob(RECEIPTS_REL)
    if prior_raw is None:
        anchor_warn(f"ANCHOR_RECEIPTS_ABSENT — {RECEIPTS_REL} does not exist at {anchor_kind}; "
                    "nothing to ratchet against (first-release bootstrap). Append-only check "
                    "SKIPPED")
    elif not receipts_raw:
        die_anchor("RECEIPT_LEDGER_MUTATED",
                   f"{RECEIPTS_REL} exists at {anchor_kind} but is absent from the working tree. "
                   "Deleting the ledger deletes every provenance record the chain attaches to; "
                   "the ledger is append-only")
    else:
        prior_text = prior_raw.decode("utf-8", errors="replace")
        try:
            prior_doc = yaml.safe_load(prior_text) or {}
        except Exception as exc:
            prior_doc = None
            anchor_warn(f"ANCHOR_UNPARSEABLE — {RECEIPTS_REL} at {anchor_kind} does not parse "
                        f"({exc}); append-only check SKIPPED")
        if isinstance(prior_doc, dict):
            prior_rows = prior_doc.get("receipts") or []
            cur_by_id = {}
            for row in cur_rows:
                if isinstance(row, dict) and row.get("id") is not None:
                    cur_by_id[row["id"]] = row
            mutated = []
            for prow in prior_rows:
                if not isinstance(prow, dict):
                    continue
                prid = prow.get("id")
                if prid not in cur_by_id:
                    mutated.append(f"{prid!r}: released row DELETED")
                elif cur_by_id[prid] != prow:
                    changed = sorted(
                        k for k in set(prow) | set(cur_by_id[prid])
                        if prow.get(k) != cur_by_id[prid].get(k))
                    mutated.append(f"{prid!r}: field(s) {', '.join(changed)} REWRITTEN")
            if mutated:
                die_anchor("RECEIPT_LEDGER_MUTATED",
                           f"{len(mutated)} row(s) of {RECEIPTS_REL} differ from {anchor_kind}: "
                           + "; ".join(mutated[:8])
                           + (" …" if len(mutated) > 8 else "")
                           + ". A receipt records what a fetch produced; rewriting one is how a "
                             "doctored body gets a matching `body_sha256` and closes domain (c) "
                             "on manufactured provenance. The ledger is APPEND-ONLY: a repeat "
                             "refresh appends a new assembly row (the chain binds to the latest), "
                             "it never edits the old one")
            prior_chunks = receipt_chunks(prior_text)
            cur_chunks = receipt_chunks(receipts_raw)
            if prior_chunks is None or cur_chunks is None:
                anchor_warn("ANCHOR_RECEIPTS_UNCHUNKABLE — the ledger's list items are not "
                            "column-0 `- id: <id>` entries on one or both sides, so the "
                            "byte-identity layer is inapplicable; parsed-row identity still "
                            "applies (an edit that changes no parsed value is not detected)")
            else:
                def trim(chunk):
                    return re.sub(r"\s+$", "", chunk)
                byte_mutated = [rid for rid, txt in prior_chunks.items()
                                if trim(cur_chunks.get(rid, "")) != trim(txt)]
                if byte_mutated:
                    die_anchor("RECEIPT_LEDGER_MUTATED",
                               f"{len(byte_mutated)} released row(s) of {RECEIPTS_REL} are not "
                               "byte-identical to " + anchor_kind + ": "
                               + ", ".join(map(repr, sorted(byte_mutated)[:8]))
                               + (" …" if len(byte_mutated) > 8 else "")
                               + ". Only appends are legal")

# ── (b)+(c) BODY and RECEIPT domains: assembly-row ids UNION anchor-changed ids ────────
# `touched` (the assembly-row population) keeps its meaning for the LIVE_MIN_TOUCHED floor;
# `chain_keys` is what actually runs the chain and additionally carries every id that DIFFERS
# from the release anchor (P1-2a). A changed id with no assembly row is the reviewer's
# refresh-without-receipt and dies RECEIPT_MISSING before anything else is inspected — that is
# the whole point: a refresh is legal only through a fetch that appends receipts.
touched = sorted(assembly)
chain_keys = sorted(set(assembly) | changed_keys)
for key in chain_keys:
    cat, sid = key
    row = assembly.get(key)
    if row is None:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} differs from the release anchor "
                       f"({anchor_kind}) — its snapshot body and/or its manifest sha/bytes entry "
                       "changed — but the ledger carries NO `kind: assembly` receipt for it. A "
                       "refreshed snapshot with no receipt records that bytes changed and not "
                       "what produced them; syncing the manifest to the new file and dropping an "
                       "allowlist row does not substitute for a fetch. Re-fetch with "
                       "practices/scripts/snapshot-extract.sh and append its receipts")
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
      f"allowlist, baseline universe {len(baseline)}); {len(chain_keys)} id(s) verified through "
      f"the full file←body←receipt chain ({len(touched)} identity(ies) with an assembly receipt "
      f"over {assembly_rows} assembly row(s), {len(changed_keys)} changed vs "
      f"{anchor_kind if anchored else 'no anchor'})")
sys.exit(0)
PY
