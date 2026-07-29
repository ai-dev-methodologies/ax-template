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
    # 2026-07-29 census (P3-83). Three domains were promoted to a shipping Next.js surface
    # (webhook R48, scheduled-task R49, email-outbox R51) and their backend spec + L4 README
    # were updated to full_trio — but the trio allowlist entry was never flipped, and its
    # "no frontend UI in scope" comment is now false in all three. The consequence is the
    # one described in the header: trio_integrity_guard skips the frontend Spec Trio for
    # exactly these three. They are NOT closable here — flipping the allowlist makes
    # trio_integrity_guard fail MISSING_FRONTEND_SPEC, because specs/<d>-frontend-l0.yaml
    # does not exist for any of them (11 domains have one; these three do not). Closing it
    # means authoring three frontend Spec Trios, which is a domain-lane task, not a guard
    # wiring task. Registered as known_gap so the contradiction is enumerated on every run
    # instead of being invisible, which is the whole point of P3-83.
    #
    # backlog_ref: P2-47, registered 2026-07-29 as the row for THESE THREE DIVERGENCES — a
    # verification escape, since the allowlist lying means those frontend Spec Trios are
    # silently unenforced by trio_integrity_guard. P3-83 is the separate row that produced
    # this GUARD; it stays only as the chain's origin and is deliberately NOT the ref here,
    # because closing P3-83 (the guard exists) does not close P2-47 (the divergences remain).
    "webhook": {
        "observed": {"spec": "full_trio", "allowlist": "backend_only", "readme": "full_trio"},
        "klass": "known_gap",
        "reason": "R48 shipped the admin Next.js surface (6 .tsx under templates/L4/webhook/app/) "
                  "and spec+README moved to full_trio; the allowlist entry still reads backend_only, "
                  "so trio_integrity_guard skips this domain's frontend trio. Cannot flip here: "
                  "specs/webhook-frontend-l0.yaml does not exist.",
        "backlog_ref": "P2-47",
    },
    "scheduled-task": {
        "observed": {"spec": "full_trio", "allowlist": "backend_only", "readme": "full_trio"},
        "klass": "known_gap",
        "reason": "R49 shipped the admin Next.js surface (6 .tsx under templates/L4/scheduled-task/app/) "
                  "and spec+README moved to full_trio; the allowlist entry still reads backend_only, "
                  "so trio_integrity_guard skips this domain's frontend trio. Cannot flip here: "
                  "specs/scheduled-task-frontend-l0.yaml does not exist.",
        "backlog_ref": "P2-47",
    },
    "email-outbox": {
        "observed": {"spec": "full_trio", "allowlist": "backend_only", "readme": "full_trio"},
        "klass": "known_gap",
        "reason": "R51 shipped the admin Next.js surface (6 .tsx under templates/L4/email-outbox/app/) "
                  "and spec+README moved to full_trio; the allowlist entry still reads backend_only, "
                  "so trio_integrity_guard skips this domain's frontend trio. Cannot flip here: "
                  "specs/email-outbox-frontend-l0.yaml does not exist.",
        "backlog_ref": "P2-47",
    },
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
        f"{len(DIVERGENCE_ALLOWLIST)} catalog divergence allowance(s) NOT applied here")

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
