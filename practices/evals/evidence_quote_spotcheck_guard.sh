#!/usr/bin/env bash
# practices/evals/evidence_quote_spotcheck_guard.sh — BACKLOG P2-1 (74th hard guard, ADVISORY live).
#
# evidence_guard.sh verifies evidence STRUCTURE only (upstream_id resolves, section/quote
# non-empty) — it never checks that the quote TEXT actually appears in the snapshot, so a
# fabricated or mis-attributed quote passes every blocking gate (proven by the 2026-06-01
# live-fetch audit, which found 56 such defects in a one-off pass). This guard closes the
# offline half of that escape DETERMINISTICALLY (full sweep, not random sampling — R25
# demands same-input/same-output): for every rule evidence entry carrying an `upstream_id`,
# the quote must appear as a substring of the referenced snapshot body
# ({catalog}/upstream/{upstream_id}.snapshot.md) after HTML-stripping + whitespace/typography
# normalization. `source_type: external` entries (URL-only, no snapshot) are out of scope —
# only a live fetch can verify those (see the live-fetch audit methodology).
#
# Mode:
#   default      ADVISORY — prints every mismatch as WARN + a summary, always exits 0.
#                Rationale: the first full sweep surfaced 95 pre-existing quote↔snapshot
#                misalignments (mostly quotes verified against the LIVE page while the disk
#                snapshot is a partial digest of it) — a blocking gate would freeze the
#                catalog on day one. Burn the backlog down, then promote via --strict.
#   --strict     exit 1 on any {catalog}/rules/*.md mismatch (the promotion path; also how
#                fixtures prove non-vacuity in run-all-guards).
#   --allow-missing-snapshot
#                In --strict mode only: QUOTE_NOT_IN_SNAPSHOT stays fatal (exit 1), but
#                SNAPSHOT_FILE_MISSING findings are DOWNGRADED to an advisory WARN list
#                (printed, non-fatal). This flag exists for an honest reason: the practices/
#                (Java-side) upstream/ manifest records sha/bytes only — the {id}.snapshot.md
#                BODIES were never committed, so those quotes cannot be verified offline and
#                restoring the bodies requires a network fetch (registered as a backlog
#                residual). The flag lets the QUOTE half of the sweep promote to strict while
#                the network-bound MISSING half is tracked but not blocking. Without --strict
#                the flag is inert (everything is advisory anyway).
#   --root DIR   scan DIR instead of the repo root (fixtures).
#
#   --include-templates
#                BACKLOG P2-40. Additionally sweeps templates/**/*.{tsx,ts} for the same
#                comment-wrapped frontmatter shape used across the L1-L4 template tree
#                (`/*\n---\n<yaml>\n---\n*/` at the top of the file) and quote-checks any
#                `evidence[].upstream_id` entry against EITHER catalog's upstream/ (a
#                template can cite a Java-side or a React-side snapshot). Findings are
#                tagged TEMPLATE_QUOTE_NOT_IN_SNAPSHOT / TEMPLATE_SNAPSHOT_FILE_MISSING and
#                are ALWAYS printed, but only participate in the --strict fatal exit when
#                --strict-templates is ALSO passed (see below) — this is the same
#                advisory-first / promote-later posture already established for the rules/
#                sweep, applied to a brand-new scan surface. `templates/**/*.md` (the
#                DECISIONS.md ADR log) is explicitly OUT OF SCOPE: it embeds MULTIPLE
#                YAML-fenced blocks per file (one per ADR) rather than one leading
#                frontmatter block, and a small number of its `evidence` entries are a
#                single-mapping `source_type: internal` shape, not the upstream_id+quote
#                list shape this guard verifies — a structurally different doc type,
#                disclosed here rather than silently mis-scanned.
#   --strict-templates
#                Promotes templates/** findings (only) to fatal under --strict. Kept
#                SEPARATE from --strict on purpose: the first live --include-templates sweep
#                (2026-07-28) found the one PRD-named fabricated anchor
#                (templates/L1/components/currency-input.tsx, fixed) PLUS ~105 pre-existing
#                quote↔snapshot misalignments across templates/L1/components/**
#                (mostly the same "paraphrased against the live page, not the digest
#                snapshot" class already known from the rules/ sweep — e.g. every
#                shadcn-ui-2026-05 citation quotes a per-component description that
#                the committed snapshot, an overview-only digest, never actually contains).
#                Flipping the templates scan straight to --strict would freeze the live
#                gate on a ~105-item backlog that is out of scope for the guard-coverage
#                closure that added this scan surface — tracked as a new backlog candidate
#                instead of silently fixed or silently dropped. Without --strict-templates
#                the flag is inert (findings are advisory regardless of --strict).
#   --templates-only-protected
#                P2-40 follow-up (2026-07-28 reviewer finding). --include-templates alone,
#                as registered live, is ADVISORY — so restoring the fabricated
#                templates/L1/components/currency-input.tsx anchor only WARNed and the
#                registered live invocation still exited 0: the required RED-on-revert did
#                NOT hold and the "fabricated anchor is blocked" claim was unearned. This
#                flag restricts the templates/** sweep to exactly the anchors listed in the
#                committed ledger
#                    practices/evals/evidence_protected_template_anchors.txt
#                (implies --include-templates), so those anchors CAN be fatal under
#                --strict --strict-templates while the ~105 pre-existing misalignments in
#                the unlisted remainder stay out of scope. The ledger is anchor-scoped
#                (`<path>::<upstream_id>` per line), not file-scoped, because
#                currency-formatter.tsx carries TWO upstream anchors of which only the
#                stripe-billing one is disk-clean — protecting the file wholesale would
#                have meant either an unearned pass or rewriting an unrelated quote to
#                make the gate green. Honest under-claiming: the protected set is exactly
#                what is verified, and what is excluded is named in the ledger.
#
#                Fail-closed non-vacuity (each ⇒ exit 2, so the gate cannot be emptied
#                into a silent pass): ledger absent · ledger with no entries · ledger with
#                no `# min_entries:` directive · fewer UNIQUE entries than that directive · a
#                declared min_entries below the guard-pinned live floor
#                (LIVE_MIN_PROTECTED_ENTRIES, applies when scanning the real repo root,
#                i.e. no --root) · an entry whose file does not exist · an entry whose file
#                carries zero upstream_id evidence · an entry whose declared upstream_id is
#                not actually cited by that file · a matching entry whose `quote` is missing
#                or normalized-empty (codex round-2: "" is a substring of every snapshot, so
#                blanking/deleting the quote used to pass vacuously instead of failing) ·
#                a matching entry whose `section` is missing or blank (same-shape bypass) ·
#                zero anchors scanned overall.
#
#                IDENTITY PINNING + TYPE STRICTNESS (codex round-3 closure, 2026-07-28).
#                Rounds 1-3 each bypassed this gate with a different trick because every
#                previous fix validated the SHAPE of a ledger entry while leaving the
#                IDENTITY of what must be protected — and the TYPE of the scalars compared —
#                unpinned. Both holes are closed by construction here:
#
#                (a) TYPE-STRICT SCALARS. `quote`, `section` and `upstream_id` of a protected
#                    anchor must be genuine YAML strings. Nothing is `str()`-coerced into the
#                    comparison any more: an unquoted `quote: 0` used to become the string
#                    "0", which occurs verbatim in the Stripe snapshot body ⇒ substring match
#                    ⇒ exit 0; `section: null` used to become the string "None", which is not
#                    blank ⇒ slipped past the blank check. int / float / bool / null / list /
#                    dict / missing each now exit 2 with their own reason code
#                    (PROTECTED_LEDGER_{MISSING,NON_STRING}_{QUOTE,SECTION} /
#                    PROTECTED_LEDGER_NON_STRING_UPSTREAM_ID). A protected quote must also
#                    clear MIN_PROTECTED_QUOTE_CHARS after normalization — a legally-typed
#                    one-character quote ("a") is the same vacuous-substring attack wearing a
#                    string's clothes.
#
#                (b) IDENTITY PINNING. `min_entries` preserved a ROW COUNT, not identities:
#                    delete the currency-input row, duplicate the clean currency-formatter
#                    row, keep min_entries: 2 ⇒ the guard checked the formatter twice, exited
#                    0, and currency-input became free to fabricate or blank. The ledger is
#                    now consumed as a SET of (path, upstream_id) identities: duplicate
#                    tuples are rejected outright (PROTECTED_LEDGER_DUPLICATE_IDENTITY), the
#                    min_entries comparison counts UNIQUE tuples, and a required-identity set
#                    must be fully present — sourced from BOTH the guard-pinned
#                    LIVE_REQUIRED_PROTECTED_IDENTITIES (real repo tree only; may not be
#                    reduced) and any `# require: <path>::<upstream_id>` directives declared
#                    in the ledger itself (all roots, incl. fixtures). A missing required
#                    identity is PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING (exit 2)
#                    REGARDLESS of row count. Ledger paths are additionally constrained to
#                    safe repo-relative form (PROTECTED_LEDGER_UNSAFE_PATH) so an identity
#                    can neither escape the scanned root nor be spelled two ways.
#
#                (c) Same-pass closures found while re-reading the whole protected path: a
#                    protected anchor whose snapshot body does not exist on disk is now a
#                    structural failure (PROTECTED_LEDGER_SNAPSHOT_MISSING, exit 2) instead
#                    of a finding that only bit under --strict-templates — deleting the
#                    snapshot must not be cheaper than falsifying the quote; and the declared
#                    `section` must itself occur in the snapshot body
#                    (TEMPLATE_SECTION_NOT_IN_SNAPSHOT finding ⇒ exit 1 under
#                    --strict --strict-templates), so a fabricated section is no longer
#                    unverified free text.
#
# Usage:
#   bash practices/evals/evidence_quote_spotcheck_guard.sh
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --allow-missing-snapshot
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --root evals/fixtures/...
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates --strict --strict-templates --root evals/fixtures/...
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --strict-templates --templates-only-protected
set -uo pipefail

STRICT=0
ALLOW_MISSING=0
ROOT_OVERRIDE=""
INCLUDE_TEMPLATES=0
STRICT_TEMPLATES=0
TEMPLATES_ONLY_PROTECTED=0
while [ $# -gt 0 ]; do
    case "$1" in
        --strict) STRICT=1; shift ;;
        --allow-missing-snapshot) ALLOW_MISSING=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --include-templates) INCLUDE_TEMPLATES=1; shift ;;
        --strict-templates) STRICT_TEMPLATES=1; shift ;;
        # implies --include-templates so the flag can never be a silent no-op
        --templates-only-protected) TEMPLATES_ONLY_PROTECTED=1; INCLUDE_TEMPLATES=1; shift ;;
        *) echo "evidence_quote_spotcheck_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SELF_REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
REPO_ROOT="${ROOT_OVERRIDE:-$SELF_REPO_ROOT}"
# LIVE_ROOT=1 ⇔ the RESOLVED scan root IS this repository, however it was supplied. The
# guard-pinned protected-identity floor applies only there; fixture roots declare their own
# min_entries. Reviewer finding (round 4): keying this on "was --root passed?" let an explicit
# `--root <the actual repo>` resolve to the identical physical tree while silently DROPPING the
# pinned identities — a protected anchor could then be substituted away and its fabricated quote
# would land only in the advisory pool. Compare canonicalized physical paths (pwd -P, so symlink
# and `.`/`..` spellings cannot alias past the check) instead of arg presence.
RESOLVED_ROOT="$(cd "$REPO_ROOT" 2>/dev/null && pwd -P)" || RESOLVED_ROOT=""
LIVE_ROOT=0; [ -n "$RESOLVED_ROOT" ] && [ "$RESOLVED_ROOT" = "$SELF_REPO_ROOT" ] && LIVE_ROOT=1

STRICT="$STRICT" ALLOW_MISSING="$ALLOW_MISSING" INCLUDE_TEMPLATES="$INCLUDE_TEMPLATES" \
STRICT_TEMPLATES="$STRICT_TEMPLATES" TEMPLATES_ONLY_PROTECTED="$TEMPLATES_ONLY_PROTECTED" \
LIVE_ROOT="$LIVE_ROOT" python3 - "$REPO_ROOT" << 'PY'
import glob, html, os, re, sys

root = sys.argv[1]
strict = os.environ.get("STRICT") == "1"
allow_missing = os.environ.get("ALLOW_MISSING") == "1"
include_templates = os.environ.get("INCLUDE_TEMPLATES") == "1"
strict_templates = os.environ.get("STRICT_TEMPLATES") == "1"
protected_only = os.environ.get("TEMPLATES_ONLY_PROTECTED") == "1"
live_root = os.environ.get("LIVE_ROOT") == "1"

# Committed protected-anchor ledger + the floor its declared min_entries may never drop
# below on the real repo tree. BOTH numbers are deliberately duplicated (ledger directive +
# guard constant) so emptying the gate requires two coordinated edits instead of one silent
# deletion. LIVE_MIN_PROTECTED_ENTRIES MAY NOT BE REDUCED.
PROTECTED_LEDGER_REL = os.path.join("practices", "evals",
                                    "evidence_protected_template_anchors.txt")
LIVE_MIN_PROTECTED_ENTRIES = 2

# Identity pinning (codex round-3). A COUNT is not an identity: min_entries=2 was satisfied
# by duplicating one clean row after deleting the row that actually matters. These exact
# (path, upstream_id) tuples MUST appear in the ledger whenever the real repo tree is
# scanned. Mirrored by `# require:` directives in the ledger itself, so dropping a protected
# identity takes two coordinated edits — the same posture as min_entries.
# LIVE_REQUIRED_PROTECTED_IDENTITIES MAY NOT BE REDUCED.
LIVE_REQUIRED_PROTECTED_IDENTITIES = frozenset({
    # The anchor P2-40 found FABRICATED — the RED-on-revert subject of this whole gate.
    ("templates/L1/components/currency-input.tsx", "stripe-billing-2026-05"),
    # Positive control — an already-correct anchor, so the gate is proven to pass for an
    # honest citation and not merely to fail for a dishonest one.
    ("templates/L1/components/currency-formatter.tsx", "stripe-billing-2026-05"),
})

# A protected quote must carry substance. `quote: 0` is closed by the isinstance check, but
# `quote: "a"` is a genuine non-empty string that is still a substring of essentially every
# snapshot body — the same vacuous-substring attack with a valid type. Normalized length
# floor; the two live protected quotes are ~110 chars.
MIN_PROTECTED_QUOTE_CHARS = 24

def die_structural(msg):
    print(f"evidence_quote_spotcheck_guard: {msg}", file=sys.stderr)
    sys.exit(2)

def normalize(s):
    s = html.unescape(s)
    s = (s.replace('‘', "'").replace('’', "'")
          .replace('“', '"').replace('”', '"')
          .replace('—', '-').replace('–', '-')
          .replace('…', '...').replace(' ', ' '))
    return re.sub(r'\s+', ' ', s).strip()

def strip_html(s):
    s = re.sub(r'<script\b.*?</script>', ' ', s, flags=re.S | re.I)
    s = re.sub(r'<style\b.*?</style>', ' ', s, flags=re.S | re.I)
    return re.sub(r'<[^>]*>', ' ', s)

try:
    import yaml
except ImportError:
    print("evidence_quote_spotcheck_guard: PyYAML unavailable — cannot run", file=sys.stderr)
    sys.exit(2)

snap_cache = {}

def load_snapshot(catalog, uid):
    """Returns (snap_path or None, normalized_text or None)."""
    snap = os.path.join(root, catalog, "upstream", uid + ".snapshot.md")
    if not os.path.isfile(snap):
        return None, None
    if snap not in snap_cache:
        snap_cache[snap] = normalize(strip_html(open(snap, errors="replace").read()))
    return snap, snap_cache[snap]

def resolve_snapshot_any_catalog(uid):
    """A templates/** citation isn't scoped to one catalog — try practices, then
    practices-react. Returns (catalog_or_None, snap_text_or_None)."""
    for catalog in ("practices", "practices-react"):
        snap, text = load_snapshot(catalog, uid)
        if snap is not None:
            return catalog, text
    return None, None

quotes = 0
scanned_rules = 0
misses = []
for catalog in ("practices", "practices-react"):
    rules_glob = os.path.join(root, catalog, "rules", "*.md")
    for rule_path in sorted(glob.glob(rules_glob)):
        if os.path.basename(rule_path) == "_template.md":
            continue
        text = open(rule_path, errors="replace").read()
        m = re.match(r'^---\n(.*?)\n---\n', text, re.S)
        if not m:
            continue
        try:
            fm = yaml.safe_load(m.group(1))
        except Exception:
            continue
        if not isinstance(fm, dict):
            continue
        scanned_rules += 1
        for entry in (fm.get("evidence") or []):
            if not isinstance(entry, dict) or "upstream_id" not in entry:
                continue
            quotes += 1
            rel = os.path.relpath(rule_path, root)
            # Same coercion class the protected sweep closes structurally (codex round-3),
            # applied to the fatal live rules sweep as FINDINGS: `str()` used to manufacture
            # a passing value out of a non-string (`0` → "0", a real substring) or out of an
            # absent key ("" → a substring of every snapshot). Neither is coerced now. The
            # protected-set length floor is deliberately NOT applied here — 48 legitimate
            # short single-token rule quotes exist and re-anchoring them is a separate,
            # disclosed backlog item, not something to smuggle in under this gate.
            uid_raw = entry["upstream_id"]
            quote_raw = entry.get("quote", None)
            if not isinstance(uid_raw, str):
                misses.append((rel, repr(uid_raw), "UPSTREAM_ID_NOT_A_STRING", ""))
                continue
            uid = uid_raw
            if not isinstance(quote_raw, str):
                misses.append((rel, uid, "QUOTE_MISSING_OR_NOT_A_STRING",
                               type(quote_raw).__name__))
                continue
            quote = quote_raw
            if not normalize(quote):
                misses.append((rel, uid, "QUOTE_BLANK", ""))
                continue
            snap, snap_text = load_snapshot(catalog, uid)
            if snap is None:
                misses.append((rel, uid, "SNAPSHOT_FILE_MISSING", ""))
                continue
            if normalize(quote) not in snap_text:
                misses.append((rel, uid, "QUOTE_NOT_IN_SNAPSHOT", quote[:90]))

# ── templates/**/*.{tsx,ts} sweep (BACKLOG P2-40) ────────────────────────────
# Separate bucket — see --strict-templates header comment for why these do NOT
# join `misses` (the rules-sweep fatal-exit pool) unconditionally.
template_quotes = 0
template_scanned = 0
template_misses = []

def parse_template_frontmatter(path):
    """Leading `/*\\n---\\n<yaml>\\n---\\n*/` block as a dict, or None. Shared by the full
    sweep and the protected-ledger sweep so the two can never drift apart."""
    text = open(path, errors="replace").read()
    m = re.match(r'/\*\n---\n(.*?)\n---\n\*/', text, re.S)
    if not m:
        return None
    try:
        fm = yaml.safe_load(m.group(1))
    except Exception:
        return None
    return fm if isinstance(fm, dict) else None

def upstream_evidence_entries(fm):
    return [e for e in (fm.get("evidence") or [])
            if isinstance(e, dict) and "upstream_id" in e]

def check_template_anchor(rel, uid, quote, section=None, protected=False):
    """Records a finding for one (file, upstream_id) citation. Returns nothing.

    protected=True (ledger sweep) tightens two things over the advisory full sweep:
      · a snapshot body that does not exist is a STRUCTURAL failure (exit 2), not a finding
        that only bites under --strict-templates — otherwise deleting the snapshot is a
        cheaper bypass than falsifying the quote;
      · the declared `section` must itself occur in the snapshot body, so a fabricated
        section is verified text rather than unchecked prose."""
    found_catalog, snap_text = resolve_snapshot_any_catalog(uid)
    if found_catalog is None:
        if protected:
            die_structural(f"PROTECTED_LEDGER_SNAPSHOT_MISSING — {rel}::{uid} resolves to no "
                           "snapshot body under practices/upstream or practices-react/upstream; "
                           "a protected anchor with no snapshot verifies nothing")
        template_misses.append((rel, uid, "TEMPLATE_SNAPSHOT_FILE_MISSING", ""))
        return
    if normalize(quote) not in snap_text:
        template_misses.append((rel, uid, "TEMPLATE_QUOTE_NOT_IN_SNAPSHOT", quote[:90]))
    if section is not None and normalize(section) not in snap_text:
        template_misses.append((rel, uid, "TEMPLATE_SECTION_NOT_IN_SNAPSHOT", section[:90]))

def require_protected_str(rel, uid, entry, field):
    """Type-strict scalar extraction for a PROTECTED anchor. Returns the value only when it
    is a genuine YAML string; every other shape (missing / null / int / float / bool / list /
    dict) is a structural ledger defect with its own reason code.

    This exists because `str(entry.get(field, ""))` silently manufactured a passing value out
    of a non-string: `quote: 0` → "0" (a literal substring of the Stripe snapshot body) and
    `section: null` → "None" (non-blank, so the blank check never fired). Coercion is the
    bypass; refusing to coerce is the fix. (YAML `true`/`0`/`1.5` parse to bool/int/float, and
    none of those is a `str`, so every unquoted scalar lands in the non-string branch.)"""
    upper = field.upper()
    if field not in entry:
        die_structural(f"PROTECTED_LEDGER_MISSING_{upper} — {rel}::{uid} declares no `{field}` "
                       f"key; a protected anchor without a {field} verifies nothing")
    value = entry[field]
    if not isinstance(value, str):
        die_structural(f"PROTECTED_LEDGER_NON_STRING_{upper} — {rel}::{uid} `{field}` is "
                       f"{type(value).__name__} ({value!r}), not a string. Protected scalars "
                       "are never str()-coerced: `0` would become \"0\" (a real substring of a "
                       "snapshot body) and `null` would become \"None\" (non-blank), both of "
                       "which pass a check they should fail.")
    return value

def safe_ledger_path(rel, line):
    """A ledger path is an IDENTITY component, so it must have exactly one spelling and it
    must stay inside the scanned root. Absolute paths, `..` traversal, backslashes and `.`
    segments are rejected rather than normalized — normalizing would let one identity be
    written two ways and defeat the required-identity set comparison."""
    if os.path.isabs(rel) or rel.startswith("/") or "\\" in rel:
        die_structural(f"PROTECTED_LEDGER_UNSAFE_PATH — {line!r} (path must be repo-relative)")
    parts = rel.split("/")
    if any(p in ("", ".", "..") for p in parts):
        die_structural(f"PROTECTED_LEDGER_UNSAFE_PATH — {line!r} "
                       "(no empty, '.' or '..' path segments; an identity has one spelling)")
    return rel

def load_protected_ledger():
    """Parses the committed protected-anchor ledger into an ORDERED SET of required
    identities. EVERY degenerate shape exits 2 rather than yielding an empty (vacuously
    passing) protected set — see the --templates-only-protected header block for the
    enumeration. Identity semantics (round-3): duplicates rejected, min_entries compared
    against the UNIQUE count, and the required-identity set (guard-pinned on the live tree +
    `# require:` directives anywhere) must be fully present."""
    path = os.path.join(root, PROTECTED_LEDGER_REL)
    if not os.path.isfile(path):
        die_structural(f"PROTECTED_LEDGER_MISSING — {PROTECTED_LEDGER_REL} not found under {root}")
    declared_min = None
    entries = []          # unique, in file order
    seen = set()
    declared_required = set()
    for raw in open(path, errors="replace").read().splitlines():
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#"):
            m = re.match(r'#\s*min_entries:\s*(\d+)\s*$', line)
            if m:
                if declared_min is not None:
                    die_structural("PROTECTED_LEDGER_DUPLICATE_MIN_ENTRIES — exactly one "
                                   "`# min_entries: N` directive is allowed")
                declared_min = int(m.group(1))
                continue
            m = re.match(r'#\s*require:\s*(\S.*?)\s*$', line)
            if m:
                spec = m.group(1)
                if "::" not in spec:
                    die_structural(f"PROTECTED_LEDGER_MALFORMED_REQUIRE — {line!r} "
                                   "(expected `# require: <path>::<upstream_id>`)")
                rrel, ruid = (part.strip() for part in spec.split("::", 1))
                if not rrel or not ruid:
                    die_structural(f"PROTECTED_LEDGER_MALFORMED_REQUIRE — {line!r} "
                                   "(expected `# require: <path>::<upstream_id>`)")
                declared_required.add((safe_ledger_path(rrel, line), ruid))
            continue
        if "::" not in line:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        rel, uid = (part.strip() for part in line.split("::", 1))
        if not rel or not uid:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        identity = (safe_ledger_path(rel, line), uid)
        if identity in seen:
            # Round-3 bypass: delete the row that matters, duplicate a clean row, keep the
            # count. A repeated identity inflates min_entries while shrinking the protected
            # SET — it is a ledger defect, never a legitimate shape.
            die_structural(f"PROTECTED_LEDGER_DUPLICATE_IDENTITY — {rel}::{uid} is listed more "
                           "than once; min_entries counts unique identities, and duplicating a "
                           "clean anchor is how a protected anchor gets silently dropped")
        seen.add(identity)
        entries.append(identity)
    if declared_min is None:
        die_structural(f"PROTECTED_LEDGER_NO_MIN_ENTRIES — {PROTECTED_LEDGER_REL} must declare "
                       "`# min_entries: N` (the count that may not be reduced)")
    if not entries:
        die_structural(f"PROTECTED_LEDGER_EMPTY — {PROTECTED_LEDGER_REL} declares no anchors; "
                       "an emptied ledger is a silenced gate, not a clean tree")
    if len(entries) < declared_min:
        die_structural(f"PROTECTED_LEDGER_SHRUNK — {len(entries)} unique anchor(s) < declared "
                       f"min_entries={declared_min}")
    if live_root and declared_min < LIVE_MIN_PROTECTED_ENTRIES:
        die_structural(f"PROTECTED_LEDGER_FLOOR — declared min_entries={declared_min} is below the "
                       f"guard-pinned live floor {LIVE_MIN_PROTECTED_ENTRIES} "
                       "(LIVE_MIN_PROTECTED_ENTRIES may not be reduced)")

    required = set(declared_required)
    if live_root:
        required |= set(LIVE_REQUIRED_PROTECTED_IDENTITIES)
    absent = sorted(required - seen)
    if absent:
        die_structural("PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING — "
                       + "; ".join(f"{r}::{u}" for r, u in absent)
                       + f" absent from {PROTECTED_LEDGER_REL} (row count is not identity: the "
                         "required anchors must each be present, however many rows the ledger has)")
    return entries

if protected_only:
    # Restricted sweep: exactly the ledger's anchors, and they ARE fatal under
    # --strict --strict-templates (unlike the advisory full sweep below).
    for rel, uid in load_protected_ledger():
        path = os.path.join(root, rel)
        if not os.path.isfile(path):
            die_structural(f"PROTECTED_LEDGER_FILE_MISSING — {rel} is listed in "
                           f"{PROTECTED_LEDGER_REL} but does not exist")
        fm = parse_template_frontmatter(path)
        if fm is None:
            die_structural(f"PROTECTED_LEDGER_NO_FRONTMATTER — {rel} has no parsable leading "
                           "evidence frontmatter block")
        cited = upstream_evidence_entries(fm)
        if not cited:
            die_structural(f"PROTECTED_LEDGER_NO_EVIDENCE — {rel} carries zero upstream_id "
                           "evidence entries; protecting it would verify nothing")
        # Round-3 (a): `str(e["upstream_id"])` coerced non-string ids into the identity
        # comparison. A protected file's upstream anchors must be genuine strings, so an
        # `upstream_id: 0` can never be matched-by-coercion against a ledger id.
        for e in cited:
            if not isinstance(e["upstream_id"], str):
                die_structural(f"PROTECTED_LEDGER_NON_STRING_UPSTREAM_ID — {rel} cites "
                               f"upstream_id of type {type(e['upstream_id']).__name__} "
                               f"({e['upstream_id']!r}); protected anchors are never coerced")
        matching = [e for e in cited if e["upstream_id"] == uid]
        if not matching:
            die_structural(f"PROTECTED_LEDGER_ANCHOR_ABSENT — {rel} does not cite "
                           f"upstream_id={uid} (ledger is stale or the anchor was renamed)")
        template_scanned += 1
        for entry in matching:
            template_quotes += 1
            # Codex round-2: `quote` defaulted to "" when absent, and "" is a substring of
            # EVERY snapshot body, so blanking or deleting the protected quote made
            # check_template_anchor() vacuously pass (0 findings, exit 0) — the fabricated-
            # anchor defense was bypassed by REMOVING the quote instead of falsifying it.
            # Codex round-3: the round-2 fix still ran `str(...)` FIRST, so `quote: 0` became
            # "0" (a real substring of the Stripe snapshot ⇒ pass) and `section: null` became
            # "None" (not blank ⇒ pass). Type comes before value now: a protected scalar must
            # BE a string; nothing is coerced into the comparison. Every rejection is a
            # structural ledger defect (exit 2, same family as the PROTECTED_LEDGER_* checks
            # above) raised BEFORE check_template_anchor can see the value.
            quote = require_protected_str(rel, uid, entry, "quote")
            section = require_protected_str(rel, uid, entry, "section")
            if not normalize(quote):
                die_structural(f"PROTECTED_LEDGER_EMPTY_QUOTE — {rel}::{uid} carries a "
                               "missing or blank `quote` — an empty quote is vacuously a "
                               "substring of every snapshot body and would silently pass")
            if len(normalize(quote)) < MIN_PROTECTED_QUOTE_CHARS:
                die_structural(f"PROTECTED_LEDGER_QUOTE_TOO_SHORT — {rel}::{uid} quote "
                               f"normalizes to {len(normalize(quote))} char(s) "
                               f"(< {MIN_PROTECTED_QUOTE_CHARS}); a quote too short to be "
                               "distinctive is a substring of nearly every snapshot body — "
                               "the empty-quote bypass wearing a valid type")
            if not section.strip():
                die_structural(f"PROTECTED_LEDGER_EMPTY_SECTION — {rel}::{uid} carries a "
                               "missing or blank `section` — a protected anchor with no "
                               "declared section verifies nothing about which part of the "
                               "snapshot backs the quote")
            check_template_anchor(rel, uid, quote, section=section, protected=True)
    if template_quotes == 0:
        die_structural("ZERO_SCAN — protected ledger resolved but no upstream_id anchor was "
                       "actually checked")
elif include_templates:
    templates_dir = os.path.join(root, "templates")
    tmpl_paths = []
    if os.path.isdir(templates_dir):
        for ext in ("*.tsx", "*.ts"):
            tmpl_paths += glob.glob(os.path.join(templates_dir, "**", ext), recursive=True)
    for path in sorted(set(tmpl_paths)):
        fm = parse_template_frontmatter(path)
        if fm is None:
            continue
        template_scanned += 1
        for entry in upstream_evidence_entries(fm):
            template_quotes += 1
            rel = os.path.relpath(path, root)
            # No str() coercion here either (round-3): a non-string upstream_id/quote, or an
            # absent quote, is reported as its own finding instead of being turned into a
            # value that happens to match.
            uid_raw = entry["upstream_id"]
            quote_raw = entry.get("quote", None)
            if not isinstance(uid_raw, str):
                template_misses.append((rel, repr(uid_raw), "TEMPLATE_UPSTREAM_ID_NOT_A_STRING", ""))
                continue
            if not isinstance(quote_raw, str):
                template_misses.append((rel, uid_raw, "TEMPLATE_QUOTE_MISSING_OR_NOT_A_STRING",
                                        type(quote_raw).__name__))
                continue
            if not normalize(quote_raw):
                template_misses.append((rel, uid_raw, "TEMPLATE_QUOTE_BLANK", ""))
                continue
            check_template_anchor(rel, uid_raw, quote_raw)

    # Zero-scan guard: template files with a frontmatter block were found but none carried
    # an upstream_id evidence entry — for the real repo that is a broken invocation; for a
    # deliberately evidence-free fixture this branch is simply never reached (template_scanned
    # stays 0 too, see check below), so it only fires on the "found frontmatter, found no
    # evidence at all" degenerate case.
    if template_scanned > 0 and template_quotes == 0:
        print("evidence_quote_spotcheck_guard: ZERO_SCAN — templates/**/*.{tsx,ts} frontmatter "
              "found but no upstream_id evidence scanned", file=sys.stderr)
        sys.exit(2)

# Zero-scan guard: a root with rule files but zero upstream quotes is a broken invocation,
# not a clean pass.
if scanned_rules > 0 and quotes == 0:
    print("evidence_quote_spotcheck_guard: ZERO_SCAN — rules found but no upstream_id evidence scanned", file=sys.stderr)
    sys.exit(2)

for rel, uid, kind, detail in misses:
    print(f"WARN [{rel}] upstream_id={uid}: {kind}" + (f" — quote starts: {detail!r}" if detail else ""))
for rel, uid, kind, detail in template_misses:
    print(f"WARN [{rel}] upstream_id={uid}: {kind}" + (f" — quote starts: {detail!r}" if detail else ""))

# Everything that is NOT the network-bound SNAPSHOT_FILE_MISSING class stays fatal under
# --allow-missing-snapshot. Defined as the complement on purpose: an allowlist of kinds
# would silently drop any kind added later (e.g. the round-3 type-strict findings) out of
# the fatal pool — exactly the "new shape slips through an old filter" bug this guard keeps
# being bypassed by.
missing_misses = [m for m in misses if m[2] == "SNAPSHOT_FILE_MISSING"]
quote_misses = [m for m in misses if m[2] != "SNAPSHOT_FILE_MISSING"]
verdict = f"{len(misses)} of {quotes} upstream quote(s) do not match their snapshot body"

if protected_only:
    print(f"evidence_quote_spotcheck_guard: PROTECTED templates anchors ({PROTECTED_LEDGER_REL}) — "
          f"{template_scanned} file(s), {template_quotes} anchor(s), {len(template_misses)} finding(s)"
          + ("" if strict_templates else " (advisory — pass --strict --strict-templates to promote)"))
elif include_templates:
    print(f"evidence_quote_spotcheck_guard: templates/**/*.{{tsx,ts}} — {template_scanned} file(s) with "
          f"evidence frontmatter, {template_quotes} upstream_id quote(s), {len(template_misses)} "
          f"finding(s)" + ("" if strict_templates else " (advisory — pass --strict-templates to promote)"))

def fatal_template_findings():
    return strict and strict_templates and template_misses

# --allow-missing-snapshot (strict only): QUOTE mismatches stay fatal; SNAPSHOT_FILE_MISSING
# is downgraded to a non-fatal advisory list. See the header for why (Java-side snapshot
# bodies are uncommitted / network-bound — a tracked backlog residual). Applies to the
# rules/ sweep only — templates/** findings are gated by --strict-templates, not this flag.
if strict and allow_missing:
    if missing_misses:
        print(f"evidence_quote_spotcheck_guard: ADVISORY — {len(missing_misses)} SNAPSHOT_FILE_MISSING "
              f"finding(s) downgraded (--allow-missing-snapshot; network-bound backlog residual)")
    if quote_misses or fatal_template_findings():
        print(f"evidence_quote_spotcheck_guard: {len(quote_misses)} of {quotes} upstream quote(s) do not "
              f"match their snapshot body — BLOCKED (--strict)", file=sys.stderr)
        sys.exit(1)
    print(f"evidence_quote_spotcheck_guard: all resolvable upstream quote(s) verified "
          f"({len(missing_misses)} missing-snapshot finding(s) advisory)")
    sys.exit(0)

if (misses and strict) or fatal_template_findings():
    if misses:
        print(f"evidence_quote_spotcheck_guard: {verdict} — BLOCKED (--strict)", file=sys.stderr)
    if fatal_template_findings():
        print(f"evidence_quote_spotcheck_guard: {len(template_misses)} of {template_quotes} "
              f"templates/**/*.{{tsx,ts}} upstream quote(s) do not match their snapshot body — "
              f"BLOCKED (--strict --strict-templates)", file=sys.stderr)
    sys.exit(1)
if misses:
    print(f"evidence_quote_spotcheck_guard: ADVISORY — {verdict} (exit 0; promote with --strict once burned down)")
else:
    print(f"evidence_quote_spotcheck_guard: all {quotes} upstream quote(s) verified against snapshot bodies")
sys.exit(0)
PY
