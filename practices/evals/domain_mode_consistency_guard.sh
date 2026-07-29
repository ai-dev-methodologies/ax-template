#!/usr/bin/env bash
# practices/evals/domain_mode_consistency_guard.sh — 3-way domain_mode agreement [100]
#
# BACKLOG P3-83. A domain's trio mode is declared in up to THREE places and nothing
# compared them:
#   (A) specs/<domain>-*.yaml            → `domain_mode:`
#   (B) practices/evals/trio_integrity_allowlist.yaml → `domains.<domain>`
#   (C) templates/L4/<domain>/README.md  → the Status line
#
# THE INSTANCE THAT MOTIVATED THIS: specs/ratelimit-l0.yaml declared `full_trio` while
# the allowlist, the L4 README ("Frontend trio deliberately skipped") and CLAUDE.md all
# said backend-only. The spec line was simply false, and it survived because
# full_trio_artifact_completeness_guard's old `os.path.isdir()` check was satisfied by a
# lone README. When that check was hardened (2026-07-28) the lie surfaced immediately and
# the spec was corrected — but what was fixed was the INSTANCE, not the CLASS. Nothing
# still asserts the three declarations agree, so the same edit reintroduces the same lie.
#
# WHY IT MATTERS BEYOND TIDINESS: (B) is the ENFORCEMENT input. trio_integrity_guard reads
# the allowlist, and `backend_only` there SKIPS the frontend Spec Trio check entirely. So a
# domain whose spec says full_trio and whose UI ships, but whose allowlist entry still says
# backend_only, has its frontend trio silently unenforced. A disagreement here is not a
# documentation nit; it is a gate that stopped running.
#
# WHAT IS CHECKED (per domain, fail-closed):
#   1. Wherever TWO OR MORE of (A)(B)(C) declare a mode, they must be EQUAL. Domains with a
#      single declaration are out of scope by construction — there is nothing to compare —
#      and that is stated in the output rather than hidden.
#   2. A README Status segment containing TWO DIFFERENT mode tokens is AMBIGUOUS and FAILS.
#      The guard refuses to pick one; guessing is how a contradiction becomes invisible.
#   3. Two backend specs for the same domain that declare DIFFERENT modes FAIL (a domain
#      cannot be two things at once, whichever file the reader happens to open).
#   4. Every known divergence must be in DIVERGENCE_ALLOWLIST below with an exact observed
#      triple, a class, a reason and a backlog_ref. The allowance is NON-REDUNDANCY-CHECKED
#      in both directions: an allowance whose domain is no longer divergent FAILS (stale
#      allowances cannot rot into lies), and an allowance whose observed triple no longer
#      matches FAILS (the divergence changed shape and must be re-reviewed).
#   4b. Every backlog_ref must NAME A ROW THAT EXISTS in docs/BACKLOG.md, and for a
#      known_gap that row must still be OPEN (`- [ ]`). A non-empty string was previously
#      enough, so the ref could name a row that had been closed — or one that never existed
#      — and the guard would keep printing the gap as "tracked" while nothing tracked it.
#      The class split is deliberate: by_design refs a DECISION, whose row may legitimately
#      be closed as a decision record, so only existence is required there. known_gap refs a
#      DEFECT; a closed row means either the defect was fixed (then the allowance is stale
#      and this guard should be re-blocking, which is exactly what happens) or the row was
#      closed while the divergence remained. Exit semantics, stated because they are the
#      point: a known_gap whose row is OPEN keeps exiting 0 — visibility, not blocking — and
#      turns into exit 1 the moment that row closes or vanishes.
#   5. Census floors. The number of L4 READMEs declaring a mode may not fall below
#      MIN_README_DECLARATIONS, and the number of cross-checked domains may not fall below
#      MIN_CROSSCHECKED. Deleting the Status line from every README would otherwise empty
#      the (C) axis and leave the guard reporting a green nothing.
#
# The allowlist lives IN THIS GUARD, not in a data file, deliberately — the [92] precedent:
# widening it is then a reviewed edit to the gate rather than one more line a stray
# copy-paste carries along.
#
# HONEST SCOPE (residuals, registered rather than papered over):
#   - A domain declaring its mode in exactly ONE place is unconstrained. This guard proves
#     AGREEMENT, not COMPLETENESS; requiring all three everywhere would demand a Status line
#     in the 6 L4 READMEs that have none and a frontend spec for domains that have no UI.
#     The floors keep that surface from shrinking, which is the enforceable part.
#   - The backlog_ref check (4b) validates THIS GUARD'S OWN allowance table, which is the
#     same on every tree, against whatever docs/BACKLOG.md the analysed root ships. The
#     catalog root ships the canonical one and MUST have it (a missing BACKLOG.md there is an
#     error, not a skip); an analysed root without one is reported as SKIPPED on stdout.
#     Because the table is currently EMPTY (P2-47), the check would otherwise have no subject
#     on any tree — its own fail fixtures included, which would pass while proving nothing. So
#     a NON-CATALOG tree may declare probe subjects in practices/evals/
#     domain_mode_probe_allowances.yaml: the domain_mode_floors.yaml pattern, for the same
#     reason ([87] can only pass `--root DIR`, never a value). It is REFUSED on the catalog
#     tree and can only ADD subjects — there is no field in it that suppresses or relaxes a
#     check, so it cannot be used to buy a pass.
#   - (C) is prose. Extraction is anchored to the VALUE of the first Status line — the text
#     after its colon, first sentence only (plus one continuation line when the value wraps).
#     Narrative after that sentence legitimately names the other mode ("R39 shipped this
#     domain as a backend-only stub; R44 added the Next.js surface"), so a wider window
#     manufactures contradictions in nine READMEs that do not have one. A second mode token
#     INSIDE the value ERRORS instead of being ranked.
#
# Usage:
#   bash practices/evals/domain_mode_consistency_guard.sh
#   bash practices/evals/domain_mode_consistency_guard.sh --root DIR
#   bash practices/evals/domain_mode_consistency_guard.sh --root DIR --min-readme-declarations N --min-crosschecked N
#   bash practices/evals/domain_mode_consistency_guard.sh --fixtures
#
# Exit: 0 = every multi-declared domain agrees · 1 = contradiction / ambiguous README /
#       stale or mismatched allowance / census floor breached · 2 = cannot verify
#       (missing python3 or PyYAML — never a silent pass).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURE_DIR="$SCRIPT_DIR/fixtures/domain-mode-consistency"

ROOT_OVERRIDE=""
RUN_FIXTURES=0
MIN_README=""
MIN_CROSS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --min-readme-declarations) MIN_README="$2"; shift 2 ;;
        --min-readme-declarations=*) MIN_README="${1#--min-readme-declarations=}"; shift ;;
        --min-crosschecked) MIN_CROSS="$2"; shift 2 ;;
        --min-crosschecked=*) MIN_CROSS="${1#--min-crosschecked=}"; shift ;;
        --fixtures) RUN_FIXTURES=1; shift ;;
        *) echo "domain_mode_consistency_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v python3 >/dev/null 2>&1; then
    echo "domain_mode_consistency_guard: BLOCK — cannot verify: python3 not on PATH" >&2
    exit 2
fi
# Fail-closed parser preflight (P2-46 convention). The probe is a heredoc so the import
# statement stands on its own line and pyyaml_preflight_coverage_guard [95], which derives
# the PyYAML-dependent set by PARSING imports, picks this guard up from disk automatically.
if ! python3 - >/dev/null 2>&1 <<'PYPROBE'
import yaml
PYPROBE
then
    echo "domain_mode_consistency_guard: BLOCK — cannot verify: PyYAML is required and this" >&2
    echo "  python3 does not have it. Exiting 2 (cannot verify) rather than reporting a pass" >&2
    echo "  this run did not earn. Install: python3 -m pip install pyyaml" >&2
    exit 2
fi

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

run_root() {
    AX_ROOT="$1" AX_MIN_README="${2:-}" AX_MIN_CROSS="${3:-}" python3 - <<'PYEOF'
import os
import re
import sys

import yaml

root = os.environ["AX_ROOT"]

MODES = ("full_trio", "backend_only", "frontend_only")

# ── which tree is this? ──────────────────────────────────────────────────────
# The catalog is the tree that ships run-all-guards.sh. This is a DISK FACT, not a
# caller-supplied flag — a flag would let a live run opt out of its own floors and its own
# divergence allowances, which is the thing being guarded against.
is_catalog_tree = os.path.isfile(os.path.join(root, "practices", "evals", "run-all-guards.sh"))

# ── census floors (see header §5) ────────────────────────────────────────────
# Catalog values as of 2026-07-29, MEASURED not guessed: 14 of the 25 L4 READMEs declare a
# mode, and 52 of the 53 domains in the universe carry two or more declarations. They are
# floors, not targets — lowering either is a reviewed edit to this gate, which is why on the
# catalog tree they are CONSTANTS: neither the environment nor an on-disk file can lower
# them, so no invocation can quietly buy itself a smaller census.
#
# A fixture tree is one domain, so it cannot meet a catalog floor. There the default is 0
# and two overrides are honoured: the caller's --min-* flags, and a floors file the tree
# carries itself. The file matters because fixture_kill_proof_guard [87] can only invoke a
# guard as `<guard> --root <fixture>` — it cannot pass a number — so a fixture that must
# breach a floor has to be able to SAY so on disk. fail_floor_breach does exactly that.
if is_catalog_tree:
    MIN_README_DECLARATIONS, MIN_CROSSCHECKED = 14, 52
else:
    MIN_README_DECLARATIONS, MIN_CROSSCHECKED = 0, 0
    floors_file = os.path.join(root, "practices", "evals", "domain_mode_floors.yaml")
    if os.path.isfile(floors_file):
        declared = yaml.safe_load(open(floors_file, encoding="utf-8")) or {}
        MIN_README_DECLARATIONS = int(declared.get("min_readme_declarations", 0))
        MIN_CROSSCHECKED = int(declared.get("min_crosschecked", 0))
    if os.environ.get("AX_MIN_README"):
        MIN_README_DECLARATIONS = int(os.environ["AX_MIN_README"])
    if os.environ.get("AX_MIN_CROSS"):
        MIN_CROSSCHECKED = int(os.environ["AX_MIN_CROSS"])

# ── divergence allowlist ─────────────────────────────────────────────────────
# key    : domain
# observed: the EXACT triple that is allowed to diverge (source → mode, absent sources
#           omitted). A different triple is a different divergence and re-BLOCKS.
# klass  : by_design  — the divergence is correct and permanent
#          known_gap  — the divergence is a REAL defect that is tracked, not accepted;
#                       it is printed on every run so it cannot fade into the background
# reason / backlog_ref: mandatory, both non-empty.
DIVERGENCE_ALLOWLIST = {
    # EMPTY as of 2026-07-30 (P2-47 closure), and an empty table is the HEALTHY state: every
    # domain declaring its mode in two or more places now agrees, so there is nothing to
    # excuse. This is a result, not an unfinished edit.
    #
    # HISTORY — the three allowances this table shipped with, and why they are gone:
    # the 2026-07-29 census (P3-83) that produced this guard opened with webhook,
    # scheduled-task and email-outbox as known_gap. All three had been promoted to a
    # shipping Next.js surface under templates/L4/<domain>/app/ (R48 / R49 / R51) with their
    # backend spec + L4 README moved to full_trio, while the trio allowlist entry still read
    # backend_only ("no frontend UI in scope"). The consequence was the one in the header:
    # trio_integrity_guard SKIPPED the frontend Spec Trio for exactly those three domains.
    # They were not closable at census time — flipping the allowlist entry alone makes
    # trio_integrity_guard fail MISSING_FRONTEND_SPEC, because none of the three owned
    # specs/<d>-frontend-l0.yaml, contracts/<d>-ui.yaml or blueprints/<d>-ui-manifest.yaml
    # (the three artifacts its full_trio check demands). BACKLOG P2-47 (residual-18 wave,
    # 2026-07-30) authored those nine artifacts and flipped the three allowlist entries to
    # full_trio, so the divergences no longer exist and the frontend trios are now actually
    # validated on every run instead of skipped.
    #
    # The non-redundancy check further down is what FORCED the removal rather than leaving
    # three stale allowances behind: an allowance whose domain is no longer divergent fails
    # the run, because a stale allowance silently pre-approves the next real contradiction.
    #
    # Adding an entry back is a reviewed edit to the gate (the [92] precedent). klass
    # by_design = the divergence is correct and permanent; known_gap = a real defect being
    # tracked, whose backlog_ref must name a row that exists and is still open.
    #
    # WORDING RULE for any future `reason` (P3-99): state the SHAPE of the divergence — a
    # shipping frontend under app/ against an allowlist that says backend_only — and never
    # a count. The original three reasons said "6 .tsx"; two of the domains grew a seventh
    # file and the prose went stale while still reading as a measurement. A reason that
    # restates a number the tree owns is a second, unenforced copy of that number.
}

errors = []
notes = []        # allowed divergences, printed on every run
tree_notes = []   # facts about the tree being analysed


def norm(text):
    """full-trio / full_trio / Full Trio → full_trio."""
    return text.strip().lower().replace("-", "_")


# ── (A) spec declarations ────────────────────────────────────────────────────
specs_dir = os.path.join(root, "specs")


def spec_modes(domain):
    """Every backend spec for this domain and the mode it declares.

    Resolution starts from trio_integrity_guard's rule — specs/<domain>-*.yaml minus the
    frontend spec — but that prefix glob over-matches: `webhook-signing-l0.yaml` is a
    SEPARATE concern spec, not the webhook domain spec, and reading it as webhook's makes
    the two "disagree" about a domain only one of them describes. So when the glob returns
    more than one candidate, the CANONICAL name `<domain>-l0.yaml` wins outright and the
    siblings are dropped; if there is no canonical name and still more than one candidate,
    every candidate is read and a disagreement among them is reported (check 3) — taking
    [0], as the trio guard does, would hide it.
    """
    out = {}
    if not os.path.isdir(specs_dir):
        return out
    candidates = [
        n for n in sorted(os.listdir(specs_dir))
        if n.endswith(".yaml") and "frontend" not in n
        and (n[: -len(".yaml")] == domain or n[: -len(".yaml")].startswith(domain + "-"))
    ]
    canonical = f"{domain}-l0.yaml"
    if len(candidates) > 1 and canonical in candidates:
        candidates = [canonical]
    for name in candidates:
        path = os.path.join(specs_dir, name)
        try:
            doc = yaml.safe_load(open(path, encoding="utf-8")) or {}
        except yaml.YAMLError as exc:
            errors.append(f"{domain}: spec {name} is not parseable YAML ({exc.__class__.__name__})")
            continue
        if not isinstance(doc, dict):
            continue
        mode = doc.get("domain_mode")
        if mode is None:
            continue
        out[name] = norm(str(mode))
    return out


# ── (B) allowlist declarations ───────────────────────────────────────────────
allowlist_path = os.path.join(root, "practices", "evals", "trio_integrity_allowlist.yaml")
if not os.path.isfile(allowlist_path):
    print(f"domain_mode_consistency_guard: BLOCK — allowlist not found: {allowlist_path}",
          file=sys.stderr)
    sys.exit(1)
try:
    allowlist_doc = yaml.safe_load(open(allowlist_path, encoding="utf-8")) or {}
except yaml.YAMLError as exc:
    print(f"domain_mode_consistency_guard: BLOCK — trio_integrity_allowlist.yaml is not "
          f"parseable YAML ({exc.__class__.__name__})", file=sys.stderr)
    sys.exit(1)
allowlist_modes = {k: norm(str(v)) for k, v in (allowlist_doc.get("domains") or {}).items()}

# ── (C) README declarations ──────────────────────────────────────────────────
L4_DIR = os.path.join(root, "templates", "L4")
STATUS_RE = re.compile(r"^\s*\**\s*status\b", re.I)
TOKEN_RE = re.compile(r"\b(full[-_ ]trio|backend[-_ ]only|frontend[-_ ]only)\b", re.I)


def readme_mode(domain):
    """The mode declared by the L4 README's Status block, or None.

    Anchored TIGHTLY, and the tightness is load-bearing. The declaration is the VALUE of
    the first Status line — the text after its colon, up to the end of that first sentence.
    Everything after it is narrative that routinely names the OTHER mode perfectly
    legitimately ("R39 shipped this domain as a backend-only stub; R44 added the Next.js
    surface"), so a wider window reports a contradiction in nine READMEs that do not have
    one. A README whose Status line ends at the colon has its value on the next non-blank
    line (the `**Status (per IMPLEMENTATION-STATUS.md)**:` / `**impl**` shape), so exactly
    that one continuation is read.

    Two DIFFERENT tokens inside the value is genuinely ambiguous: the caller reports it as
    an error rather than resolving it by precedence.
    """
    path = os.path.join(L4_DIR, domain, "README.md")
    if not os.path.isfile(path):
        return None, False
    lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
    start = next((i for i, ln in enumerate(lines) if STATUS_RE.match(ln)), None)
    if start is None:
        return None, False
    head = lines[start]
    value = head.split(":", 1)[1] if ":" in head else ""
    if not value.strip():
        value = next((ln for ln in lines[start + 1:] if ln.strip()), "")
    # First sentence only. A trailing '.' with no following space (a filename, a version)
    # is not a sentence end, so split on '. ' and also honour a value ending in '.'.
    value = re.split(r"\.\s", value, maxsplit=1)[0]
    found = {norm(m.group(1).replace(" ", "_")) for m in TOKEN_RE.finditer(value)}
    if len(found) > 1:
        return sorted(found), True          # ambiguous
    if len(found) == 1:
        return found.pop(), False
    return None, False


# ── universe ─────────────────────────────────────────────────────────────────
# Every domain that CAN carry a second declaration: an L4 directory or an allowlist key.
# A spec-only domain has exactly one declaration and nothing to disagree with.
l4_domains = set()
if os.path.isdir(L4_DIR):
    l4_domains = {d for d in os.listdir(L4_DIR) if os.path.isdir(os.path.join(L4_DIR, d))}
universe = sorted(l4_domains | set(allowlist_modes))

readme_declarations = 0
crosschecked = 0
divergent = {}

for domain in universe:
    declared = {}

    smodes = spec_modes(domain)
    distinct_spec = set(smodes.values())
    if len(distinct_spec) > 1:
        detail = ", ".join(f"{n}={m}" for n, m in sorted(smodes.items()))
        errors.append(
            f"{domain}: two backend specs declare DIFFERENT domain_mode values ({detail}) — "
            f"the mode a reader gets depends on which file they open")
    elif distinct_spec:
        declared["spec"] = distinct_spec.pop()

    if domain in allowlist_modes:
        declared["allowlist"] = allowlist_modes[domain]

    rmode, ambiguous = readme_mode(domain)
    if ambiguous:
        errors.append(
            f"{domain}: templates/L4/{domain}/README.md Status block declares TWO different "
            f"modes ({', '.join(rmode)}) — ambiguous, and this guard will not rank them")
    elif rmode is not None:
        declared["readme"] = rmode
        readme_declarations += 1

    for src, mode in declared.items():
        if mode not in MODES:
            errors.append(
                f"{domain}: {src} declares '{mode}', which is not one of {', '.join(MODES)}")

    if len(declared) < 2:
        continue
    crosschecked += 1
    if len(set(declared.values())) > 1:
        divergent[domain] = declared

# ── divergence disposal ──────────────────────────────────────────────────────
# DIVERGENCE_ALLOWLIST names domains of THIS catalog, so it applies only when the tree
# being analysed IS this catalog (is_catalog_tree, established above from disk). On a
# fixture tree the allowances are inapplicable and every divergence must therefore BLOCK on
# its own — which is what the fail fixture asserts. The skip is printed, never silent.
if not is_catalog_tree:
    tree_notes.append(
        f"non-catalog tree (no practices/evals/run-all-guards.sh) — "
        f"{len(DIVERGENCE_ALLOWLIST)} catalog divergence allowance(s) NOT applied to this "
        f"tree's divergences (their backlog_ref liveness IS still checked — header §4b)")

for domain, declared in sorted(divergent.items()):
    allowance = DIVERGENCE_ALLOWLIST.get(domain) if is_catalog_tree else None
    shape = ", ".join(f"{k}={v}" for k, v in sorted(declared.items()))
    if allowance is None:
        errors.append(
            f"{domain}: domain_mode CONTRADICTION — {shape}. These declarations must agree; "
            f"the allowlist entry is what trio_integrity_guard actually enforces, so a "
            f"disagreement means a Spec Trio check is not running where a spec says it should")
        continue
    if allowance.get("observed") != declared:
        want = ", ".join(f"{k}={v}" for k, v in sorted((allowance.get("observed") or {}).items()))
        errors.append(
            f"{domain}: divergence allowance is STALE — allowed [{want}] but observed [{shape}]. "
            f"The divergence changed shape and must be re-reviewed, not inherited")
        continue
    if allowance.get("klass") not in ("by_design", "known_gap"):
        errors.append(f"{domain}: divergence allowance has no valid class (by_design|known_gap)")
    if not (allowance.get("reason") or "").strip():
        errors.append(f"{domain}: divergence allowance carries no reason")
    if not (allowance.get("backlog_ref") or "").strip():
        errors.append(f"{domain}: divergence allowance carries no backlog_ref")
    notes.append(f"{allowance.get('klass')}: {domain} [{shape}] — {allowance.get('backlog_ref')}")

# Non-redundancy: an allowance for a domain that is NOT divergent is stale and must go.
for domain in sorted(DIVERGENCE_ALLOWLIST if is_catalog_tree else {}):
    if domain not in divergent:
        errors.append(
            f"{domain}: divergence allowance is UNUSED — the domain's declarations now agree "
            f"(or it disappeared). Remove the allowance; a stale allowance silently pre-approves "
            f"the next real contradiction")

# ── backlog_ref liveness (header §4b) ────────────────────────────────────────
# A non-empty backlog_ref only proved someone typed something. The ref is the ONLY thing
# that makes a known_gap "tracked rather than accepted", so it must name a row that exists,
# and for a known_gap that row must still be open. Otherwise the allowlist rots exactly the
# way the divergences it describes did: the gap keeps being printed as tracked while its row
# has been closed or renamed away underneath it.
#
# The table is guard-internal and identical on every tree, so it is validated against
# whatever docs/BACKLOG.md the analysed root ships — required on the catalog tree, reported
# as skipped elsewhere (see HONEST SCOPE).
BACKLOG_REL = os.path.join("docs", "BACKLOG.md")
backlog_path = os.path.join(root, BACKLOG_REL)
# Anchored to line start: a row is `- [ ] P2-47 …`. In-body sibling references are written
# parenthesised — `(P3-82)` — precisely so a scanner cannot mistake a mention for a row, and
# this regex inherits that convention rather than inventing a second one.
BACKLOG_ROW_RE = re.compile(r"^\s*-\s+\[([ xX])\]\s+(P\d+-\d+)\b")
REF_ID_RE = re.compile(r"P\d+-\d+")

backlog_rows = None
if os.path.isfile(backlog_path):
    backlog_rows = {}
    for line in open(backlog_path, encoding="utf-8", errors="replace").read().splitlines():
        m = BACKLOG_ROW_RE.match(line)
        if m:
            backlog_rows.setdefault(m.group(2), []).append(m.group(1).strip().lower() == "x")
elif is_catalog_tree:
    errors.append(
        f"{BACKLOG_REL} not found on the catalog tree — every divergence allowance names a "
        f"backlog row, and with no backlog to check against those refs cannot be verified; "
        f"skipping would report a pass this run did not earn")

# ── ref-liveness subjects ────────────────────────────────────────────────────
# Normally the subjects ARE the allowance table. The table is legitimately EMPTY today
# (P2-47 closed the last three), and an empty subject list would leave this check with
# nothing to run on any tree — including its own fail fixtures, which would then pass and
# quietly stop proving anything. So a NON-CATALOG tree may declare probe subjects on disk.
#
# This is the domain_mode_floors.yaml pattern already used above, for the same reason: [87]
# can only invoke `<guard> --root <fixture>` and cannot pass a value, so a fixture that needs
# to state something has to state it ON DISK. The safety properties that make it not a
# loophole: the file is REFUSED on the catalog tree, and its entries can only ADD subjects to
# check — there is no field that suppresses or relaxes anything.
PROBE_REL = os.path.join("practices", "evals", "domain_mode_probe_allowances.yaml")
probe_path = os.path.join(root, PROBE_REL)
ref_subjects = [(d, a.get("klass"), (a.get("backlog_ref") or "").strip())
                for d, a in sorted(DIVERGENCE_ALLOWLIST.items())]

if os.path.isfile(probe_path):
    if is_catalog_tree:
        errors.append(
            f"{PROBE_REL} exists on the CATALOG tree — probe subjects are a fixture-only "
            f"device for keeping the backlog_ref check provable while the real allowance "
            f"table is empty. On the catalog tree the subjects must be the table itself, so "
            f"this file is refused rather than honoured")
    else:
        try:
            probe_doc = yaml.safe_load(open(probe_path, encoding="utf-8")) or {}
        except yaml.YAMLError as exc:
            probe_doc = {}
            errors.append(f"{PROBE_REL} is not parseable YAML ({exc.__class__.__name__})")
        for entry in (probe_doc.get("probe_allowances") or []):
            ref_subjects.append((f"probe:{entry.get('name', '?')}",
                                 entry.get("klass"),
                                 str(entry.get("backlog_ref") or "").strip()))
        tree_notes.append(
            f"{len(ref_subjects) - len(DIVERGENCE_ALLOWLIST)} probe ref-subject(s) from "
            f"{PROBE_REL} (non-catalog tree only; adds subjects, relaxes nothing)")

if backlog_rows is None:
    tree_notes.append(f"no {BACKLOG_REL} under the analysed root — backlog_ref liveness SKIPPED")
else:
    tree_notes.append(f"backlog_ref liveness: {len(ref_subjects)} subject(s) checked against "
                      f"{len(backlog_rows)} backlog row(s)")
    for domain, klass, ref in ref_subjects:
        if not ref:
            continue                      # already reported where the allowance is applied
        ref_ids = REF_ID_RE.findall(ref)
        if not ref_ids:
            errors.append(
                f"{domain}: backlog_ref '{ref}' names no backlog row id (expected a P<n>-<n> "
                f"identifier) — an unresolvable ref tracks nothing")
            continue
        for ref_id in ref_ids:
            states = backlog_rows.get(ref_id)
            if states is None:
                errors.append(
                    f"{domain}: backlog_ref {ref_id} has NO ROW in {BACKLOG_REL} — the "
                    f"divergence is described as tracked by a row that does not exist")
                continue
            # `states or []` rather than `states`: the None case is already handled and
            # `continue`d above, so this only matters when the branch above is DISABLED —
            # which is exactly what fixture_kill_proof_guard [87] does to prove the
            # missing-row fixture depends on the missing-row check. Without it the neutered
            # run dies on `any(None)` and the mutation reads as "guard broken" instead of
            # "fixture flipped", and the branch becomes unprovable in isolation.
            if klass == "known_gap" and any(states or []):
                errors.append(
                    f"{domain}: backlog_ref {ref_id} is CLOSED in {BACKLOG_REL} but the "
                    f"known_gap divergence is still present. Either the divergence was "
                    f"resolved — then remove this allowance — or the row was closed while the "
                    f"gap remained; a known_gap pointing at a closed row tracks nothing")

# ── census floors ────────────────────────────────────────────────────────────
if readme_declarations < MIN_README_DECLARATIONS:
    errors.append(
        f"README census FLOOR breached: {readme_declarations} L4 README(s) declare a mode, "
        f"floor is {MIN_README_DECLARATIONS} — the (C) axis shrank, so agreement is being "
        f"proven against fewer declarations than before")
if crosschecked < MIN_CROSSCHECKED:
    errors.append(
        f"cross-check FLOOR breached: {crosschecked} domain(s) carry two or more declarations, "
        f"floor is {MIN_CROSSCHECKED} — a guard cross-checking nothing reports green for free")

# ── report ───────────────────────────────────────────────────────────────────
print(f"  domains in universe        : {len(universe)} (L4 dirs ∪ allowlist keys)")
print(f"  cross-checked (>=2 sources): {crosschecked}  [floor {MIN_CROSSCHECKED}]")
print(f"  L4 READMEs declaring a mode: {readme_declarations}  [floor {MIN_README_DECLARATIONS}]")
print(f"  single-declaration domains : {len(universe) - crosschecked} (nothing to compare — out of scope by construction)")
for n in tree_notes:
    print(f"  NOTE: {n}")
for n in notes:
    print(f"  ALLOWED DIVERGENCE — {n}")

if errors:
    print("", file=sys.stderr)
    for e in errors:
        print(f"  VIOLATION: {e}", file=sys.stderr)
    sys.exit(1)
sys.exit(0)
PYEOF
}

if [ "$RUN_FIXTURES" -eq 1 ]; then
    RC=0
    echo "── [fixture pass_clean]"
    run_root "$FIXTURE_DIR/pass_clean" 1 1
    [ $? -eq 0 ] || { echo "domain_mode_consistency_guard: FAIL — pass_clean did not exit 0" >&2; RC=1; }
    echo "── [fixture fail_spec_contradicts_allowlist]"
    run_root "$FIXTURE_DIR/fail_spec_contradicts_allowlist" 1 1
    [ $? -eq 1 ] || { echo "domain_mode_consistency_guard: FAIL — fail_spec_contradicts_allowlist did not exit 1" >&2; RC=1; }
    # floor 0: an ambiguous Status value contributes NO declaration, so a floor of 1 would
    # fire too and the fixture would stop being specific to the ambiguity check.
    echo "── [fixture fail_readme_ambiguous]"
    run_root "$FIXTURE_DIR/fail_readme_ambiguous" 0 1
    [ $? -eq 1 ] || { echo "domain_mode_consistency_guard: FAIL — fail_readme_ambiguous did not exit 1" >&2; RC=1; }
    echo "── [fixture fail_floor_breach]"
    run_root "$FIXTURE_DIR/fail_floor_breach" 1 1
    [ $? -eq 1 ] || { echo "domain_mode_consistency_guard: FAIL — fail_floor_breach did not exit 1" >&2; RC=1; }
    # 4b: the two rotted-backlog_ref branches. Both trees are otherwise clean copies of
    # pass_clean — one domain, agreeing triple, no divergence, no floor to breach — so their
    # exit 1 can only come from the ref check itself. Each carries one probe subject on disk
    # (domain_mode_probe_allowances.yaml), because the real allowance table is empty and a
    # check with no subject would pass vacuously.
    echo "── [fixture fail_backlog_ref_closed]"
    run_root "$FIXTURE_DIR/fail_backlog_ref_closed" 1 1
    [ $? -eq 1 ] || { echo "domain_mode_consistency_guard: FAIL — fail_backlog_ref_closed did not exit 1" >&2; RC=1; }
    echo "── [fixture fail_backlog_ref_missing]"
    run_root "$FIXTURE_DIR/fail_backlog_ref_missing" 1 1
    [ $? -eq 1 ] || { echo "domain_mode_consistency_guard: FAIL — fail_backlog_ref_missing did not exit 1" >&2; RC=1; }
    if [ "$RC" -eq 0 ]; then
        echo "domain_mode_consistency_guard: PASS — fixture set discriminates"
        exit 0
    fi
    exit 1
fi

echo "── [$( [ -n "$ROOT_OVERRIDE" ] && echo "root $REPO_ROOT" || echo "live repo" )]"
run_root "$REPO_ROOT" "$MIN_README" "$MIN_CROSS"
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "domain_mode_consistency_guard: PASS — every multi-declared domain agrees (allowed divergences listed above)"
elif [ "$RC" -eq 1 ]; then
    echo "domain_mode_consistency_guard: FAIL — see VIOLATION lines above" >&2
fi
exit "$RC"
