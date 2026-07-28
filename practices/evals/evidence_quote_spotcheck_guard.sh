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
#                no `# min_entries:` directive · fewer entries than that directive · a
#                declared min_entries below the guard-pinned live floor
#                (LIVE_MIN_PROTECTED_ENTRIES, applies when scanning the real repo root,
#                i.e. no --root) · an entry whose file does not exist · an entry whose file
#                carries zero upstream_id evidence · an entry whose declared upstream_id is
#                not actually cited by that file · zero anchors scanned overall.
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
REPO_ROOT="${ROOT_OVERRIDE:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
# LIVE_ROOT=1 ⇔ scanning the real repo tree (no --root fixture override). The guard-pinned
# protected-ledger floor applies only there; fixture roots declare their own min_entries.
LIVE_ROOT=0; [ -z "$ROOT_OVERRIDE" ] && LIVE_ROOT=1

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
            uid = str(entry["upstream_id"])
            quote = str(entry.get("quote", ""))
            rel = os.path.relpath(rule_path, root)
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

def check_template_anchor(rel, uid, quote):
    """Records a finding for one (file, upstream_id) citation. Returns nothing."""
    found_catalog, snap_text = resolve_snapshot_any_catalog(uid)
    if found_catalog is None:
        template_misses.append((rel, uid, "TEMPLATE_SNAPSHOT_FILE_MISSING", ""))
    elif normalize(quote) not in snap_text:
        template_misses.append((rel, uid, "TEMPLATE_QUOTE_NOT_IN_SNAPSHOT", quote[:90]))

def load_protected_ledger():
    """Parses the committed protected-anchor ledger. EVERY degenerate shape exits 2 rather
    than yielding an empty (vacuously passing) protected set — see the --templates-only-protected
    header block for the enumeration."""
    path = os.path.join(root, PROTECTED_LEDGER_REL)
    if not os.path.isfile(path):
        die_structural(f"PROTECTED_LEDGER_MISSING — {PROTECTED_LEDGER_REL} not found under {root}")
    declared_min = None
    entries = []
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
        if "::" not in line:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        rel, uid = (part.strip() for part in line.split("::", 1))
        if not rel or not uid:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        entries.append((rel, uid))
    if declared_min is None:
        die_structural(f"PROTECTED_LEDGER_NO_MIN_ENTRIES — {PROTECTED_LEDGER_REL} must declare "
                       "`# min_entries: N` (the count that may not be reduced)")
    if not entries:
        die_structural(f"PROTECTED_LEDGER_EMPTY — {PROTECTED_LEDGER_REL} declares no anchors; "
                       "an emptied ledger is a silenced gate, not a clean tree")
    if len(entries) < declared_min:
        die_structural(f"PROTECTED_LEDGER_SHRUNK — {len(entries)} anchor(s) < declared "
                       f"min_entries={declared_min}")
    if live_root and declared_min < LIVE_MIN_PROTECTED_ENTRIES:
        die_structural(f"PROTECTED_LEDGER_FLOOR — declared min_entries={declared_min} is below the "
                       f"guard-pinned live floor {LIVE_MIN_PROTECTED_ENTRIES} "
                       "(LIVE_MIN_PROTECTED_ENTRIES may not be reduced)")
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
        matching = [e for e in cited if str(e["upstream_id"]) == uid]
        if not matching:
            die_structural(f"PROTECTED_LEDGER_ANCHOR_ABSENT — {rel} does not cite "
                           f"upstream_id={uid} (ledger is stale or the anchor was renamed)")
        template_scanned += 1
        for entry in matching:
            template_quotes += 1
            check_template_anchor(rel, uid, str(entry.get("quote", "")))
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
            check_template_anchor(os.path.relpath(path, root),
                                  str(entry["upstream_id"]), str(entry.get("quote", "")))

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

quote_misses = [m for m in misses if m[2] == "QUOTE_NOT_IN_SNAPSHOT"]
missing_misses = [m for m in misses if m[2] == "SNAPSHOT_FILE_MISSING"]
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
