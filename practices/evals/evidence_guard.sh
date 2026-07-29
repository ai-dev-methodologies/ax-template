#!/usr/bin/env bash
# practices/evals/evidence_guard.sh — fourth binary hard gate.
#
# Forbids rules whose claims are not anchored to a recorded external source. The rule body
# itself is allowed to be Claude-authored; the *justification* must trace back to either
# (a) a snapshot in {catalog}/upstream/_MANIFEST.yaml, or (b) an explicit external citation
# (RFC, JEP, vendor docs, peer-reviewed paper).
#
# This gate is the answer to "why was this rule made?" — every rule must be auditable.
#
# SCOPE — STRUCTURE, not TRUTH (BACKLOG P2-1): this gate verifies the evidence SHAPE
# (upstream_id resolves in _MANIFEST.yaml, section/quote/citation/url non-empty). It does
# NOT verify the quote text actually appears in the snapshot or on the live page — a
# fabricated quote with valid structure passes. The offline half of that escape is covered
# by evidence_quote_spotcheck_guard.sh (deterministic quote-vs-snapshot sweep, advisory);
# the online half (quote-vs-live-page) only a live-fetch audit can verify.
#
# SCOPE — TWO SURFACES: {catalog}/rules/*.md (the loop below) AND templates/** (the §4.10
# walk at the bottom, promoted from a file COUNT to real structural verification by BACKLOG
# P2-43). The templates walk resolves against the repo root, so it runs once per invocation
# regardless of --catalog; --templates-root DIR runs it alone against a fixture tree.
#
# Usage:
#   bash practices/evals/evidence_guard.sh                       # default catalog=practices
#   bash practices/evals/evidence_guard.sh --catalog practices-react
#   bash practices/evals/evidence_guard.sh --templates-root DIR  # fixtures (see §4.10 below)
set -uo pipefail

CATALOG="${CATALOG:-practices}"
CATALOG_DIR_OVERRIDE=""
ROOT_OVERRIDE=""
TEMPLATES_ONLY=0
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        # --templates-root DIR: run ONLY the §4.10 templates walk, rooted at DIR. One flag
        # rather than a --root/--templates-only pair so the [87] kill-proof harness (which
        # invokes `bash <guard> <fixture_arg> <fixture_path>`) can drive these fixtures.
        --templates-root) ROOT_OVERRIDE="$2"; TEMPLATES_ONLY=1; shift 2 ;;
        --templates-root=*) ROOT_OVERRIDE="${1#--templates-root=}"; TEMPLATES_ONLY=1; shift ;;
        /*) CATALOG_DIR_OVERRIDE="$1"; shift ;;
        *) echo "evidence_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "evidence_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CATALOG_DIR="${CATALOG_DIR_OVERRIDE:-$REPO_ROOT/$CATALOG}"

# The templates/ walk (§4.10, below) resolves against the repo root, which fixtures
# relocate with --templates-root. The rules loop keeps using CATALOG_DIR exactly as before.
TEMPLATES_ROOT="$REPO_ROOT"
if [ -n "$ROOT_OVERRIDE" ]; then
    if [ ! -d "$ROOT_OVERRIDE" ]; then
        echo "evidence_guard: --templates-root '$ROOT_OVERRIDE' is not a directory" >&2
        exit 2
    fi
    TEMPLATES_ROOT="$(cd "$ROOT_OVERRIDE" && pwd)"
fi

if [ "$TEMPLATES_ONLY" -eq 0 ]; then

if [ ! -d "$CATALOG_DIR" ]; then
    echo "evidence_guard: catalog '$CATALOG' not found at $CATALOG_DIR — nothing to check"
    exit 0
fi

cd "$CATALOG_DIR"

violations=0
shopt -s nullglob

# Build the set of registered snapshot ids for upstream_id validation.
MANIFEST_IDS=""
if [[ -f upstream/_MANIFEST.yaml ]]; then
    MANIFEST_IDS=$(python3 - <<'PY'
import yaml, pathlib
d = yaml.safe_load(pathlib.Path("upstream/_MANIFEST.yaml").read_text()) or {}
print("\n".join(s.get("id","") for s in d.get("snapshots", [])))
PY
    )
    # Same distinction as the per-rule loop below: an unparseable manifest yields an
    # EMPTY id set, under which every upstream_id looks unregistered — a parse failure
    # masquerading as a catalog-wide evidence violation. Cannot parse ⇒ BLOCK.
    if [[ $? -ne 0 ]]; then
        echo "evidence_guard: BLOCK — cannot parse upstream/_MANIFEST.yaml (tooling/parse failure, NOT an evidence violation)" >&2
        exit 2
    fi
fi

for rule in rules/*.md; do
    [[ "$(basename "$rule")" == "_template.md" ]] && continue
    [[ "$(basename "$rule")" == ".gitkeep" ]] && continue

    python3 - "$rule" "$MANIFEST_IDS" <<'PY'
import pathlib, sys, yaml

path = pathlib.Path(sys.argv[1])
manifest_ids = set(filter(None, sys.argv[2].splitlines()))
text = path.read_text()

# Extract frontmatter (between leading --- fences).
if not text.startswith("---"):
    print(f"VIOLATION [{path}]: no YAML frontmatter")
    sys.exit(1)
parts = text.split("---", 2)
if len(parts) < 3:
    print(f"VIOLATION [{path}]: malformed frontmatter")
    sys.exit(1)

try:
    fm = yaml.safe_load(parts[1]) or {}
except yaml.YAMLError as e:
    print(f"VIOLATION [{path}]: frontmatter YAML parse error: {e}")
    sys.exit(1)

ev = fm.get("evidence")
if not isinstance(ev, list) or len(ev) == 0:
    print(f"VIOLATION [{path}]: `evidence` field missing or empty (need ≥1 entry)")
    sys.exit(1)

prov = fm.get("provenance_class")
errors = []
for i, item in enumerate(ev):
    if not isinstance(item, dict):
        errors.append(f"entry {i}: not a mapping")
        continue

    if "upstream_id" in item:
        uid = item["upstream_id"]
        if uid not in manifest_ids:
            errors.append(f"entry {i}: upstream_id={uid!r} not found in _MANIFEST.yaml (known: {sorted(manifest_ids)})")
        if not str(item.get("section", "")).strip():
            errors.append(f"entry {i}: missing `section`")
        if not str(item.get("quote", "")).strip():
            errors.append(f"entry {i}: missing `quote`")
    elif item.get("source_type") == "external":
        if not str(item.get("citation", "")).strip():
            errors.append(f"entry {i}: missing `citation`")
        if not str(item.get("url", "")).strip():
            errors.append(f"entry {i}: missing `url`")
        # P2-20: an internal_design rule's design decision is author-made, so any
        # external citation it carries can only anchor a GENERIC principle, never
        # mandate the specific rule. Force that honesty mechanically — the entry
        # must declare `anchors: generic_principle_only`. If a citation truly
        # mandated the rule, the rule would not be internal_design.
        if prov == "internal_design" and str(item.get("anchors", "")).strip() != "generic_principle_only":
            errors.append(f"entry {i}: provenance_class=internal_design + source_type=external requires `anchors: generic_principle_only`")
    else:
        errors.append(f"entry {i}: must have either `upstream_id` or `source_type: external`")

# Template placeholder rejection: any url containing the exact placeholder from _template.md
# must not survive into a real rule.
placeholder_marker = "(replace with the standard / docs you actually consulted)"
for i, item in enumerate(ev):
    if isinstance(item, dict) and placeholder_marker in str(item.get("citation", "")):
        errors.append(f"entry {i}: citation still contains the _template.md placeholder")

if errors:
    print(f"VIOLATION [{path}]:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
sys.exit(0)
PY
    # Distinguish "this rule violates the contract" from "the checker could not run".
    # exit 1 is the checker's own violation signal; ANY other non-zero (ImportError,
    # traceback, killed interpreter) means we did not actually verify this rule, and
    # counting it as a violation produces a false diagnosis — historically 233 phantom
    # "lack auditable evidence" findings when PyYAML was merely absent, sending a
    # contributor after a problem that did not exist. Cannot-verify ⇒ BLOCK (exit 2).
    rule_rc=$?
    if [[ $rule_rc -eq 1 ]]; then
        violations=$((violations + 1))
    elif [[ $rule_rc -ne 0 ]]; then
        echo "evidence_guard: BLOCK — cannot verify $rule: checker exited $rule_rc (tooling/parse failure, NOT an evidence violation)" >&2
        exit 2
    fi
done

if [[ $violations -gt 0 ]]; then
    echo "evidence_guard: $violations rule(s) lack auditable evidence — merge BLOCKED" >&2
    exit 1
fi

fi  # end: TEMPLATES_ONLY == 0

# ── templates/ walk extension (§4.10) ────────────────────────────────────────
# Walk templates/L{0,1,2,3,4}/**, templates/backend/**, and templates/DECISIONS.md and
# STRUCTURALLY VERIFY the evidence they carry — the same contract the rules loop above
# applies to {catalog}/rules/*.md, in the three shapes this tree actually uses.
#
# BACKLOG P2-43 — WHY THIS IS NO LONGER A COUNT. Until 2026-07-29 this block globbed the
# tree, counted the files, and printed "evidence check passed (catalog rules already
# verified above)". Nothing under templates/** was ever read: an `upstream_id` naming a
# snapshot no manifest registers, an evidence entry with an empty `citation`, or a
# frontmatter block that is not valid YAML at all all passed — while the gate reported a
# scan. templates/DECISIONS.md still opens with "All entries are machine-validated by
# evidence_guard.sh (via §4.10 templates/ walk extension)"; PRD §4.10 promised a
# BaseController.java template "is verified by reading its evidence: block". This block is
# that promise, kept.
#
# THE THREE SHAPES (census-derived, not invented — every evidence-bearing file in the tree
# is one of them):
#   A. leading YAML frontmatter, optionally wrapped in a /* */ comment — .tsx/.ts/.yaml/.md
#   B. an @ax-template-meta Javadoc block — .java
#   C. fenced ```yaml ADR blocks inside templates/DECISIONS.md (one per ADR, so the file
#      has no leading frontmatter; `evidence` there is a single mapping, not a list)
#
# PER-ENTRY CONTRACT (mirrors the rules loop; entry kinds are the ones on disk):
#   `upstream_id`            → must resolve in practices/upstream/_MANIFEST.yaml OR
#                              practices-react/upstream/_MANIFEST.yaml (a template may cite
#                              either catalog), plus non-empty `section` and non-empty
#                              `quote` (ADR blocks carry the quoted text in `citation`, so
#                              either field satisfies the quote clause)
#   `source_type: external`  → non-empty `citation` + `url`, or a non-empty `source_refs:`
#                              list whose every member has non-empty `url` + `quote`
#   `source_type: internal*` → non-empty `rationale`
#   anything else            → VIOLATION (no silent pass for an unrecognised shape)
#   plus the _template.md placeholder-citation rejection the rules loop applies.
#
# PRESENCE, so the check cannot be dodged by deleting the block it verifies: a shape-A file
# declaring `template_id`, every shape-B @ax-template-meta block, and every shape-C ADR must
# carry ≥1 evidence entry. Three shape-B files predate the contract and carry none; they are
# listed explicitly in JAVA_NO_EVIDENCE_EXEMPT below and the list is NON-REDUNDANCY-CHECKED
# (an exemption for a file that does carry evidence, or that has no meta block at all, is
# itself a VIOLATION) so it cannot rot into a hiding place.
#
# HONEST SCOPE — what this block does NOT verify, stated rather than implied:
#   • quote TRUTH (does the quote appear in the snapshot body / on the live page). That is
#     evidence_quote_spotcheck_guard.sh [74]; for templates/** it is ADVISORY except the
#     explicitly protected anchors — a documented decision (~105 pre-existing quote↔snapshot
#     misalignments), not an oversight of this gate.
#   • snapshot BODY presence on disk. `upstream_id` resolution here is manifest membership,
#     exactly as for rules — 14 registered ids have no committed body, which is the same
#     spotcheck backlog. Requiring bodies here would silently import that backlog.
#   • the rules-side `provenance_class: internal_design` + external-citation ⇒
#     `anchors: generic_principle_only` clause. That is a rule-document contract; templates
#     are source files and the census shows they never adopted it. Not imported.
#   • `templates/_tests/**` and `templates/AGENTS.md` — outside the §4.10 walk before and
#     after this change (AGENTS.md is a sentinel doc with no evidence contract).
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN;
# and if a shape has files but yielded ZERO verified entries the parser has silently stopped
# reading, which is the very failure this block was promoted out of → BLOCK.

TEMPLATES_DIR="$TEMPLATES_ROOT/templates"
if [[ -d "$TEMPLATES_DIR" ]]; then
    templates_files=()
    shopt -s nullglob
    for f in \
        "$TEMPLATES_DIR"/DECISIONS.md \
        "$TEMPLATES_DIR"/L0/*.md "$TEMPLATES_DIR"/L0/*.tsx "$TEMPLATES_DIR"/L0/*.ts "$TEMPLATES_DIR"/L0/*.yaml \
        "$TEMPLATES_DIR"/L1/*.md "$TEMPLATES_DIR"/L1/*.tsx "$TEMPLATES_DIR"/L1/*.ts "$TEMPLATES_DIR"/L1/*.yaml \
        "$TEMPLATES_DIR"/L2/*.md "$TEMPLATES_DIR"/L2/*.tsx "$TEMPLATES_DIR"/L2/*.ts "$TEMPLATES_DIR"/L2/*.yaml \
        "$TEMPLATES_DIR"/L3/*.md "$TEMPLATES_DIR"/L3/*.tsx "$TEMPLATES_DIR"/L3/*.ts "$TEMPLATES_DIR"/L3/*.yaml \
        "$TEMPLATES_DIR"/L4/*.md "$TEMPLATES_DIR"/L4/*.tsx "$TEMPLATES_DIR"/L4/*.ts "$TEMPLATES_DIR"/L4/*.yaml \
        "$TEMPLATES_DIR"/backend/*.java "$TEMPLATES_DIR"/backend/*.yaml "$TEMPLATES_DIR"/backend/*.md; do
        [[ -f "$f" ]] && templates_files+=("$f")
    done
    # Also recurse into subdirs
    while IFS= read -r f; do
        [[ -f "$f" ]] && templates_files+=("$f")
    done < <(find "$TEMPLATES_DIR/L0" "$TEMPLATES_DIR/L1" "$TEMPLATES_DIR/L2" "$TEMPLATES_DIR/L3" "$TEMPLATES_DIR/L4" "$TEMPLATES_DIR/backend" \
        -name "*.md" -o -name "*.tsx" -o -name "*.ts" -o -name "*.yaml" -o -name "*.java" 2>/dev/null | sort)
    shopt -u nullglob

    # Deduplicate
    IFS=$'\n' templates_files=($(printf '%s\n' "${templates_files[@]}" | sort -u)); unset IFS

    if [[ ${#templates_files[@]} -eq 0 ]]; then
        echo "evidence_guard: ZERO_SCAN — templates/ exists but no scannable files found — merge BLOCKED" >&2
        exit 1
    fi

    TEMPLATES_ROOT="$TEMPLATES_ROOT" TEMPLATES_WALKED="${#templates_files[@]}" python3 - <<'PY'
import os, pathlib, re, sys

try:
    import yaml
except ImportError:  # unreachable: the preflight above already blocked on this
    print("evidence_guard: BLOCK — cannot verify templates/: PyYAML missing", file=sys.stderr)
    sys.exit(2)

ROOT = pathlib.Path(os.environ["TEMPLATES_ROOT"]).resolve()
WALKED = int(os.environ["TEMPLATES_WALKED"])
TDIR = ROOT / "templates"

FM_RE = re.compile(r'\A(?:/\*\s*\n)?---\n(.*?)\n---', re.S)
JAVA_META_RE = re.compile(r'/\*\*.*?@ax-template-meta(.*?)\*/', re.S)
ADR_RE = re.compile(r'```yaml\n(.*?)```', re.S)
PLACEHOLDER = "(replace with the standard / docs you actually consulted)"

FM_DIRS = ("L0", "L1", "L2", "L3", "L4", "backend")
FM_EXTS = {".md", ".tsx", ".ts", ".yaml"}

# Shape-B files that predate the evidence contract and carry no `evidence:` at all.
# Non-redundancy-checked below: an entry that is wrong in EITHER direction fails.
JAVA_NO_EVIDENCE_EXEMPT = (
    "templates/backend/import-export/ImportException.java",
    "templates/backend/integration/WebhookOutbox.java",
    "templates/backend/integration/WebhookOutboxRepository.java",
)

# ── anchors_rule axis (BACKLOG P3-90, folded in 2026-07-29) ──────────────────
# templates/backend/_check-anchors.sh reported 57 violations while being wired to NO gate,
# so the count could grow unnoticed. 42 of those were the anchors axis: a shape-B block whose
# `anchors_rule:` named a rule file that does not exist (19), or that declared no
# `anchors_rule` at all (23). That axis lives here now — this walk already parses every
# @ax-template-meta block, so it is a new CHECK on an existing walk, not a new guard file, and
# it needs no run-all-guards registration or headline-count change.
#
# SCOPE: the axis is active only when the scanned tree carries the real catalog
# (ROOT/practices/rules). Fixture trees under practices/evals/fixtures/template-evidence/
# deliberately ship a minimal tree with no rules/ dir, and making a missing catalog fatal
# would convert every one of those fixtures — including pass_clean — into a failure while
# proving nothing about anchors. Same precedent as the spotcheck guard's live-root pinning.
# Non-vacuity is enforced below: if the axis IS active and meta blocks were walked, at least
# one anchor must actually have been resolved, so the axis cannot silently check nothing.
#
# Files legitimately carrying NO anchor. Each is a port/abstraction or a config whose
# invariant the catalog does not (yet) state, so anchoring it to the nearest rule would be an
# over-claim — the failure mode this axis exists to prevent. They declare the reason in an
# `anchors_rule_absent:` field (prose; deliberately NOT `anchors_rule:`, so the machine-
# readable fact stays "absent"). Checked in BOTH directions, like JAVA_NO_EVIDENCE_EXEMPT:
# an exempt file that regains an anchors_rule, or that stops being a meta-block file, fails.
JAVA_NO_ANCHOR_EXEMPT = (
    "templates/backend/email-outbox/EmailSenderService.java",
    "templates/backend/file-storage/FileValidationService.java",
    "templates/backend/file-storage/MultipartConfig.java",
    "templates/backend/import-export/ImportException.java",
    "templates/backend/realtime/SseSubscription.java",
    "templates/backend/scheduled-task/LockingPolicy.java",
    "templates/backend/search/SearchBackend.java",
    "templates/backend/search/SearchQueryParser.java",
)

RULES_DIR = ROOT / "practices" / "rules"
ANCHORS_AXIS_ACTIVE = RULES_DIR.is_dir()
# `anchors_rule_absent:` must not satisfy this — the key is anchored to a literal colon.
ANCHOR_RE = re.compile(r'^anchors_rule:[ \t]*(.*)$', re.M)
RULE_TOKEN_RE = re.compile(r'[a-z][a-z0-9-]*\.md')
# Second live anchor FORM, found by this axis on its first run: 11 templates anchor to a spec
# or contract ITEM (`specs/audit-log-l0.yaml#AUDIT-RECORD-001`,
# `contracts/audit-log-openapi.yaml#listAuditLogs`, `auth-asvs-l1.yaml#ASVS-2.7.1`) rather
# than to a rules/*.md file. _check-anchors.py scanned only for `*.md` tokens, so for these it
# found nothing to check and passed silently — a whole anchor form outside every gate. Both
# forms resolve here: the document must exist AND the fragment must occur in it, so a renamed
# spec item is caught the same way a deleted rule is.
REF_TOKEN_RE = re.compile(r'([A-Za-z0-9_./-]+\.(?:yaml|yml|json|md))#(\S+)')
REF_SEARCH_DIRS = ("", "specs", "contracts", "blueprints")
anchors_checked = 0
anchor_exempt_seen = set()


def resolve_ref_document(ref_path):
    """A ref may be written repo-relative or bare; try the repo root then the canonical
    artifact dirs. Returns the resolved Path or None."""
    for d in REF_SEARCH_DIRS:
        cand = (ROOT / d / ref_path) if d else (ROOT / ref_path)
        if cand.is_file():
            return cand
        # bare filename written without its directory (e.g. `auth-asvs-l1.yaml#ASVS-2.7.1`)
        cand = (ROOT / d / pathlib.Path(ref_path).name) if d else None
        if cand is not None and cand.is_file():
            return cand
    return None


def check_anchors_rule(rel, block_text):
    """Resolve the meta block's anchors_rule. Returns the number of anchors actually resolved.
    Every failure path appends to `errors` (fail-closed)."""
    global anchors_checked
    m = ANCHOR_RE.search(block_text)
    exempt = rel in JAVA_NO_ANCHOR_EXEMPT
    if exempt:
        anchor_exempt_seen.add(rel)
    if m is None:
        if not exempt:
            errors.append(f"{rel}: @ax-template-meta block declares no `anchors_rule` and is not "
                          f"listed in JAVA_NO_ANCHOR_EXEMPT")
        return 0
    if exempt:
        errors.append(f"{rel}: listed in JAVA_NO_ANCHOR_EXEMPT but DOES declare `anchors_rule` — "
                      f"remove the stale exemption")
    value = m.group(1).strip()
    refs = REF_TOKEN_RE.findall(value)
    # A rule token inside a ref (`…/foo.md#bar`) must not be counted twice.
    consumed = " ".join(f"{p}#{frag}" for p, frag in refs)
    rules = [r for r in RULE_TOKEN_RE.findall(value) if r not in consumed]
    if not rules and not refs:
        # A non-empty value naming nothing resolvable is how a dead anchor hides: the field
        # looks populated to a human and resolves to nothing for the gate.
        errors.append(f"{rel}: `anchors_rule: {value!r}` resolves to nothing (expected a "
                      f"<name>.md rule or a <document>#<item> spec/contract reference)")
        return 0
    resolved = 0
    for ref in rules:
        if (RULES_DIR / ref).is_file():
            resolved += 1
        else:
            errors.append(f"{rel}: anchors_rule references a rule that does not exist: {ref}")
    for ref_path, fragment in refs:
        doc = resolve_ref_document(ref_path)
        if doc is None:
            errors.append(f"{rel}: anchors_rule references a document that does not exist: "
                          f"{ref_path}")
            continue
        if fragment not in doc.read_text(errors="replace"):
            errors.append(f"{rel}: anchors_rule references {ref_path}#{fragment} but "
                          f"{doc.relative_to(ROOT)} does not contain {fragment!r}")
            continue
        resolved += 1
    anchors_checked += resolved
    return resolved

errors = []
entries = {"frontmatter": 0, "java_meta": 0, "adr": 0}
files = {"frontmatter": 0, "java_meta": 0, "adr": 0}


def load_manifest_ids():
    ids, seen = set(), 0
    for cat in ("practices", "practices-react"):
        p = ROOT / cat / "upstream" / "_MANIFEST.yaml"
        if not p.exists():
            continue
        seen += 1
        try:
            d = yaml.safe_load(p.read_text()) or {}
        except yaml.YAMLError as exc:
            print(f"evidence_guard: BLOCK — cannot parse {p} ({exc}) — tooling failure, NOT an "
                  f"evidence violation", file=sys.stderr)
            sys.exit(2)
        ids |= {s.get("id") for s in (d.get("snapshots") or [])
                if isinstance(s, dict) and s.get("id")}
    return ids, seen


def evidence_subblock(lines):
    """Isolate the `evidence:` key from a block whose OTHER keys may not be valid YAML.

    Shape B embeds `usage: |` prose that can dedent to column 0 and break a whole-block
    parse; that is the usage field's problem, not the evidence field's, so the evidence key
    plus its indented continuation lines are extracted deterministically and parsed alone.
    """
    out, started = [], False
    for line in lines:
        if re.match(r'^evidence:', line):
            started = True
            out.append(line)
            continue
        if started:
            if line.strip() == "" or line.startswith((" ", "\t", "-")):
                out.append(line)
            else:
                break
    return "\n".join(out) if out else None


def entries_of(fm):
    """`evidence:` is a list in shapes A/B and a single mapping in shape C."""
    ev = fm.get("evidence")
    if ev is None:
        return None
    return ev if isinstance(ev, list) else [ev]


def entry_kind(ent):
    """Which of the three recognised evidence shapes this entry is — or None."""
    if "upstream_id" in ent:
        return "upstream_id"
    if ent.get("source_type") == "external":
        return "external"
    if str(ent.get("source_type", "")).startswith("internal"):
        return "internal"
    return None


def check_entry(src, idx, ent, ids):
    """Returns 1 if a recognised entry shape was verified, 0 otherwise."""
    if not isinstance(ent, dict):
        errors.append(f"{src}: evidence[{idx}] is not a mapping")
        return 0
    verified = 0
    kind = entry_kind(ent)
    if kind is None:
        errors.append(f"{src}: evidence[{idx}] unrecognised shape (keys={sorted(ent.keys())}) "
                      f"— need `upstream_id`, `source_type: external`, or `source_type: internal*`")
    if kind == "upstream_id":
        uid = ent["upstream_id"]
        if uid not in ids:
            errors.append(f"{src}: evidence[{idx}] upstream_id={uid!r} is not registered in "
                          f"any upstream/_MANIFEST.yaml")
        if not str(ent.get("section", "")).strip():
            errors.append(f"{src}: evidence[{idx}] upstream_id={uid!r} missing `section`")
        if not (str(ent.get("quote", "")).strip() or str(ent.get("citation", "")).strip()):
            errors.append(f"{src}: evidence[{idx}] upstream_id={uid!r} missing `quote`")
        verified = 1
    elif kind == "external":
        refs = ent.get("source_refs")
        if isinstance(refs, list) and refs:
            for j, ref in enumerate(refs):
                if not isinstance(ref, dict):
                    errors.append(f"{src}: evidence[{idx}].source_refs[{j}] is not a mapping")
                    continue
                if not str(ref.get("url", "")).strip():
                    errors.append(f"{src}: evidence[{idx}].source_refs[{j}] missing `url`")
                if not str(ref.get("quote", "")).strip():
                    errors.append(f"{src}: evidence[{idx}].source_refs[{j}] missing `quote`")
        else:
            if not str(ent.get("citation", "")).strip():
                errors.append(f"{src}: evidence[{idx}] source_type=external missing `citation`")
            if not str(ent.get("url", "")).strip():
                errors.append(f"{src}: evidence[{idx}] source_type=external missing `url`")
        verified = 1
    elif kind == "internal":
        if not str(ent.get("rationale", "")).strip():
            errors.append(f"{src}: evidence[{idx}] source_type={ent.get('source_type')!r} "
                          f"missing `rationale`")
        verified = 1
    if PLACEHOLDER in str(ent.get("citation", "")):
        errors.append(f"{src}: evidence[{idx}] citation still carries the _template.md placeholder")
    return verified


manifest_ids, manifests_seen = load_manifest_ids()
if manifests_seen == 0:
    print("evidence_guard: BLOCK — cannot verify templates/: no upstream/_MANIFEST.yaml under "
          f"{ROOT} (cannot resolve any upstream_id)", file=sys.stderr)
    sys.exit(2)

# ── shape A: leading frontmatter ────────────────────────────────────────────
for d in FM_DIRS:
    base = TDIR / d
    if not base.is_dir():
        continue
    for f in sorted(base.rglob("*")):
        if not f.is_file() or f.suffix not in FM_EXTS:
            continue
        rel = str(f.relative_to(ROOT))
        m = FM_RE.search(f.read_text(errors="replace"))
        if not m:
            continue
        files["frontmatter"] += 1
        parse_error = ""
        try:
            fm = yaml.safe_load(m.group(1)) or {}
        except yaml.YAMLError as exc:
            fm, parse_error = {}, str(exc).splitlines()[0].strip()
        # Not a tooling failure: the FILE's frontmatter is not YAML. Every consumer (this
        # gate, the deps guard, the quote spotcheck — whose parser does `except: continue`)
        # silently skips it, so an unparseable header is a way to become invisible to the
        # whole gate stack. Three live files were in exactly that state (P2-43).
        if parse_error != "":
            errors.append(f"{rel}: frontmatter is not parseable YAML ({parse_error})")
            continue
        if not isinstance(fm, dict):
            errors.append(f"{rel}: frontmatter is not a mapping")
            continue
        ents = entries_of(fm)
        if ents is None:
            if "template_id" in fm:
                errors.append(f"{rel}: declares template_id but carries no `evidence:` block")
            continue
        if not ents:
            errors.append(f"{rel}: `evidence:` is present but empty")
            continue
        for i, ent in enumerate(ents):
            entries["frontmatter"] += check_entry(rel, i, ent, manifest_ids)

# ── shape B: @ax-template-meta Javadoc ──────────────────────────────────────
exempt_seen = set()
for f in sorted(TDIR.rglob("*.java")):
    rel = str(f.relative_to(ROOT))
    m = JAVA_META_RE.search(f.read_text(errors="replace"))
    if not m:
        continue
    files["java_meta"] += 1
    block = [re.sub(r'^\s*\*\s?', '', line) for line in m.group(1).splitlines()]
    # P3-90 anchors axis — runs BEFORE any `continue` below, so a block that is exempt from
    # (or fails) the evidence check is still held to its anchor.
    if ANCHORS_AXIS_ACTIVE:
        check_anchors_rule(rel, "\n".join(block))
    sub = evidence_subblock(block)
    # Record the sighting BEFORE either branch: `exempt_seen` answers "does this exempted
    # path still name a real meta-block file", which is true either way. Deciding it inside
    # a branch would make the two exemption axes (stale-because-it-has-evidence below,
    # stale-because-the-file-is-gone at the end) depend on each other.
    if rel in JAVA_NO_EVIDENCE_EXEMPT:
        exempt_seen.add(rel)
    if sub is None:
        if rel not in JAVA_NO_EVIDENCE_EXEMPT:
            errors.append(f"{rel}: @ax-template-meta block carries no `evidence:`")
        continue
    # `sub is not None` is implied by the `continue` above; it is spelled out so this
    # condition is a distinct, self-describing anchor for the [87] kill-proof manifest.
    if sub is not None and rel in JAVA_NO_EVIDENCE_EXEMPT:
        errors.append(f"{rel}: listed in JAVA_NO_EVIDENCE_EXEMPT but DOES carry evidence — "
                      f"remove the stale exemption")
    try:
        parsed = yaml.safe_load(sub) or {}
    except yaml.YAMLError as exc:
        errors.append(f"{rel}: @ax-template-meta `evidence:` is not parseable YAML "
                      f"({str(exc).splitlines()[0].strip()})")
        continue
    ents = entries_of(parsed) if isinstance(parsed, dict) else None
    if not ents:
        errors.append(f"{rel}: @ax-template-meta `evidence:` is present but empty")
        continue
    for i, ent in enumerate(ents):
        entries["java_meta"] += check_entry(rel, i, ent, manifest_ids)

for p in JAVA_NO_EVIDENCE_EXEMPT:
    if (ROOT / p).exists() and p not in exempt_seen:
        errors.append(f"{p}: stale JAVA_NO_EVIDENCE_EXEMPT entry — the file carries no "
                      f"@ax-template-meta block at all")

# P3-90: the anchor exemption's other direction. An entry naming a file that is gone, or that
# stopped carrying a meta block, is a licence to skip a check that nothing is using — the same
# staleness the evidence exemption above closes.
if ANCHORS_AXIS_ACTIVE:
    for p in JAVA_NO_ANCHOR_EXEMPT:
        if (ROOT / p).exists() and p not in anchor_exempt_seen:
            errors.append(f"{p}: stale JAVA_NO_ANCHOR_EXEMPT entry — the file carries no "
                          f"@ax-template-meta block at all")
    # Non-vacuity: meta blocks were walked with the catalog present, so at least one anchor
    # must have resolved. Zero means the block/field parse silently stopped matching — the
    # failure this axis was folded in to end.
    if files["java_meta"] > 0 and anchors_checked == 0:
        errors.append(f"ZERO_ANCHORS: {files['java_meta']} @ax-template-meta block(s) were "
                      f"walked against a present practices/rules/ but NOT ONE anchors_rule "
                      f"resolved — the anchors axis is vacuous")

# ── shape C: DECISIONS.md ADR blocks ────────────────────────────────────────
dec = TDIR / "DECISIONS.md"
if dec.is_file():
    for block in ADR_RE.findall(dec.read_text(errors="replace")):
        body = block.strip()
        if body.startswith("---"):
            body = body.strip("-\n")
        files["adr"] += 1
        try:
            fm = yaml.safe_load(body) or {}
        except yaml.YAMLError as exc:
            errors.append(f"templates/DECISIONS.md: ADR yaml block is not parseable YAML "
                          f"({str(exc).splitlines()[0].strip()})")
            continue
        if not isinstance(fm, dict):
            errors.append("templates/DECISIONS.md: ADR yaml block is not a mapping")
            continue
        src = f"templates/DECISIONS.md[{fm.get('adr_id', '<no adr_id>')}]"
        ents = entries_of(fm)
        if not ents:
            errors.append(f"{src}: no `evidence:` block")
            continue
        for i, ent in enumerate(ents):
            entries["adr"] += check_entry(src, i, ent, manifest_ids)

# ── non-vacuity: a shape with files but zero verified entries means the parser
#    stopped reading — the exact failure this promotion exists to end.
for shape in ("frontmatter", "java_meta", "adr"):
    if files[shape] > 0 and entries[shape] == 0:
        errors.append(f"ZERO_VERIFIED[{shape}]: {files[shape]} file(s) carry this shape but "
                      f"NOT ONE evidence entry was verified — the scan is vacuous")

total = sum(entries.values())
if errors:
    print(f"evidence_guard: templates/ walk — {len(errors)} evidence violation(s):", file=sys.stderr)
    for e in errors:
        print(f"  VIOLATION {e}", file=sys.stderr)
    print("evidence_guard: templates/ evidence check FAILED — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

print(f"evidence_guard: templates/ walk found {WALKED} file(s) — VERIFIED {total} evidence "
      f"entr{'y' if total == 1 else 'ies'} across "
      f"{files['frontmatter']} frontmatter + {files['java_meta']} @ax-template-meta + "
      f"{files['adr']} ADR block(s): upstream_id resolution, required fields, entry shape")
sys.exit(0)
PY
    templates_rc=$?
    if [[ $templates_rc -ne 0 ]]; then
        exit $templates_rc
    fi
elif [ "$TEMPLATES_ONLY" -eq 1 ]; then
    # --templates-root exists so a fixture can exercise THIS block. A run of it that finds
    # no templates/ dir verified nothing, so it must not share an exit code with a pass.
    echo "evidence_guard: BLOCK — --templates-root given but no templates/ dir under $TEMPLATES_ROOT" >&2
    exit 2
fi

if [ "$TEMPLATES_ONLY" -eq 1 ]; then
    echo "evidence_guard: templates-root run complete (catalog rules NOT checked in this mode)"
    exit 0
fi

echo "evidence_guard: all rules have auditable evidence"
exit 0
