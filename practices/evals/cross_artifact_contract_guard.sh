#!/usr/bin/env bash
# practices/evals/cross_artifact_contract_guard.sh — cross-artifact drift guard [113].
#
# THE INVARIANT: two artifacts that are SUPPOSED to agree on a value or a set MUST actually
# agree, measured by deriving the value/set from EACH artifact independently and diffing
# them — never by grepping one artifact for a string the other artifact's fix happened to
# introduce (that would be a tautology: the check would just re-assert what the fix already
# says, and could never fail on a future drift that used a different literal). Two prior
# incidents motivate this, both GH #92 root-cause Class C ("cross-artifact drift" — two
# artifacts that must agree independently evolve):
#   F-024/#86 — skills/ax-install-hooks/SKILL.md's hook body called `-PaxRootPackage=...`
#               while skills/ax-install-java-enforcement/SKILL.md's build.gradle.kts snippet
#               read `providers.gradleProperty("...")` under a DIFFERENT property name; the
#               ArchUnit gate silently fell back to a generic default package and PASSed on
#               real violations. Fixed once by hand; nothing stopped it drifting again.
#   F-019/#80 — a shipped practices-react/eslint-plugin-ax/rules/*.js id was absent from the
#               catalog's INDEX.md, making it invisible to ax-practices' INDEX-only routing.
#
# CORRECTION (P2-110, 2026-08-15) — the first shipped version of check (a) was NOT actually
# independent, and this repo does not hide corrections. It `grep`'d each SKILL.md's ENTIRE
# file text for `-P<name>=` / `findProperty("...")`/`gradleProperty("...")`, not the text of
# the specific fenced artifact the hook/build actually installs. An external adversarial
# critic reproduced the hole: delete the real `-PaxRootPackage=` invocation from the
# `hook-body` marker's fence body (skills/ax-install-hooks/SKILL.md's installed hook, then at
# line 173) while leaving the self-check CHECKLIST PROSE elsewhere in the same file that
# merely *describes* the requirement (then at line ~340, "The java block's ... call passes
# `-PaxRootPackage=...`") — the guard still PASSed, because whole-file grep cannot tell an
# artifact that enforces a contract from prose that only narrates it. That is exactly the
# "grep one artifact for a string that happens to be present" tautology this guard's own
# header warned against, just one level removed (the string it fell back to matching was
# documentation of the fix, not the fix's own literal). Fixed by scoping BOTH sides' extraction
# to the marker's FENCE BODY ONLY (practices/scripts/lib/ax_markers.py's discover(), the same
# structural parser guard [112]/verify-downstream.sh already use), never the surrounding
# Markdown prose. See CHECK (a) below for the corrected shape, and the
# fail_prose_only_p_contract fixture for the regression test that reproduces the critic's exact
# scenario (prior guard: PASS; corrected guard: BLOCK).
#
# CHECK (a) — hook <-> java-skill `-P` GRADLE PROPERTY NAME:
#   Derives the property name from EACH side independently, reading ONLY the body of one
#   specific `<!-- ax:artifact ... -->`-marked fenced block per side (never any other text in
#   the file, including prose that merely describes the contract — see the P2-110 correction
#   above):
#     side 1 (java skill):  every `project.findProperty("X")` / `providers.gradleProperty("X")`
#                            call inside the `java-gradle-testpractices` marker's fence body in
#                            skills/ax-install-java-enforcement/SKILL.md.
#     side 2 (hooks skill):  every `-PX=` flag on a line that also contains `gradlew`, inside
#                            the `hook-body` marker's fence body in
#                            skills/ax-install-hooks/SKILL.md.
#   0 distinct names on EITHER side, or >1 distinct name on EITHER side, is itself a BLOCK —
#   an ambiguous contract is exactly the shape that let F-024 hide (the java side declared
#   one name, the hook side quietly used another, and nothing forced them into the same
#   variable). Only when both sides resolve to exactly one name are they compared for
#   equality. This is non-tautological: change ONLY one side's literal INSIDE ITS MARKER FENCE
#   and the derived sets diverge — nothing in this guard's own source has to change to catch
#   it. Changing text OUTSIDE either marker's fence (prose, checklists, comments) has NO effect
#   on either derived set, which is the specific property the P2-110 fix restores.
#
#   Marker-id resolution: the two marker ids ("hook-body" and "java-gradle-testpractices") ARE
#   hardcoded literals in this guard, not discovered from `kind=`/`base=` attributes alone —
#   both SKILL.md files carry more than one `kind=file-fragment`/`kind=file` marker (the java
#   skill alone has two separate `kind=file-fragment base=java.root path=build.gradle.kts`
#   markers, `java-archunit-dep` and `java-gradle-testpractices`; those attributes cannot tell
#   them apart), so id is the only unambiguous handle. The BLOCK-not-silent-skip guarantee
#   required for a hardcoded id lives in the extraction step itself: if
#   `ax_markers.discover()` does not yield an artifact with the expected id in the expected
#   source file, this guard treats that exactly like "0 distinct property names found" on that
#   side (see the VIOLATION(a) messages below) — a renamed/removed marker cannot silently make
#   the check pass, it degrades straight into the existing 0-count BLOCK path, with its own
#   explicit "marker not found" message so the failure is diagnosable rather than confused with
#   "marker present but empty".
#
#   Token-substitution note: skills/ax-install-hooks/SKILL.md's rendered fenced blocks may
#   carry `ax:subst`/`@@ns.path@@` placeholder tokens (see the marker grammar this arc
#   introduced). `ax_markers.discover()` — the extraction this guard now uses — returns the
#   fence body EXACTLY as authored, un-rendered (rendering, via `ax_markers.render()`, is a
#   separate, later step this guard never calls). The property-NAME position (`-P<name>=`) is
#   captured by `[A-Za-z0-9_]+`, which cannot match the literal characters `@`/`.` that
#   `@@ns.path@@` tokens are built from. A token sitting in that position would therefore
#   simply fail to produce a match on that line (not silently match as a bogus name) and fall
#   through to the "0 distinct names" BLOCK above — fail closed, not fail silent. Only the
#   property VALUE position (`-P...="$VAR"` / `@@config.java.testTask@@` assigned to
#   JAVA_TEST_TASK) uses subst tokens on disk today; this guard never reads that position.
#
# CHECK (b) — shipped ESLint rule id <-> practices-react/INDEX.md coverage:
#   Derives rule ids from disk (practices-react/eslint-plugin-ax/rules/*.js filenames — zero
#   hardcoded count, zero hardcoded id) and checks each is actually FINDABLE in the
#   generated practices-react/INDEX.md, which is the ONLY thing ax-practices' routing reads
#   (per react_eslint_rule_doc_coverage_guard.sh's own header). This is a DIFFERENT
#   invariant than react_eslint_rule_doc_coverage_guard.sh [110]:
#     [110]  rule .js  <->  a catalog DOC FILE existing under practices-react/rules/
#     [113]  rule id   <->  that id being VISIBLE IN INDEX.md, the artifact routing reads
#   A rule can pass [110] (a doc file exists) and still fail [113] if generate_index.sh was
#   never re-run after the doc landed — INDEX.md is a generated snapshot, not a live query,
#   so the two CAN legitimately diverge (exactly the F-019/#80 shape: doc/rule shipped,
#   INDEX.md stale).
#
#   Live measurement taken before writing this guard (2026-08-14): of the 15 rules under
#   practices-react/eslint-plugin-ax/rules/*.js, 7 do NOT appear as their own literal rule id
#   anywhere in INDEX.md (no-array-includes-in-loop, no-array-mutate-on-state,
#   no-broad-barrel-imports, no-falsy-numeric-render, no-inline-component-definition,
#   prefer-functional-setstate, react-async-parallel) — because INDEX.md is generated from
#   catalog DOC SLUGS (e.g. `js-set-map-lookups`), and these 7 rules are covered only via
#   [110]'s method-2 alias (a doc's frontmatter `verification.rule_id: "ax/<id>"`, filename
#   != rule id — the same bundle-barrel-imports.md / rerender-no-inline-components.md
#   pattern [110] documents). A naive raw-id-in-INDEX check would therefore report these 7
#   as live RED on a perfectly healthy catalog — the exact false-positive [110]'s own header
#   warns a filename-only check would produce, just moved one hop further downstream. This
#   guard resolves aliases THE SAME WAY [110] does before checking INDEX.md:
#     method 1 — the rule id itself appears in INDEX.md (word-bounded, not raw substring).
#     method 2 — some practices-react/rules/*.md declares
#                `verification.rule_id: "ax/<id>"` (identical extraction to [110]), and THAT
#                doc's filename slug appears in INDEX.md.
#   With both methods, the current tree is 15/15 covered (verified below) — i.e. [113] is
#   live GREEN today, not because the invariant is trivial, but because [110]'s alias
#   coverage already keeps INDEX.md's generator honest; [113] exists to catch the moment
#   that stops being true (INDEX.md regenerated stale, or a new rule shipped without ever
#   running generate_index.sh).
#
# Check (b) is deliberately grep/sed-based, NO `import yaml` / PyYAML dependency — see
# pyyaml_preflight_coverage_guard.sh [95]: a PyYAML-dependent guard acquires an extra
# reachability obligation this guard has no need to take on. Check (a) DOES now shell out to
# python3 to reuse practices/scripts/lib/ax_markers.py (stdlib-only, no PyYAML — see that
# file's own header), the same module guard [112] and verify-downstream.sh already depend on;
# python3 itself is an unconditional R25 toolchain prerequisite (see CLAUDE.md), so this adds
# no new reachability obligation to the R25 pipeline.
#
# Exit: 0 PASS (both checks agree) · 1 a cross-artifact drift found (check a or b) · 2
# usage/setup error (a required source file/tool is missing).
#
# Usage:
#   bash practices/evals/cross_artifact_contract_guard.sh
#   bash practices/evals/cross_artifact_contract_guard.sh --root DIR   # fixture tree; DIR must
#     contain skills/ax-install-{java-enforcement,hooks}/SKILL.md (each carrying its expected
#     ax:artifact marker) and practices-react/{eslint-plugin-ax/rules,rules,INDEX.md}
#
# Registration: this guard IS registered as [113] in practices/evals/run-all-guards.sh (the
# prior header line claiming otherwise was stale — corrected here, same P2-110 pass, since
# this repo does not let a known-false statement sit uncorrected in a file it is already
# touching).
#
# What this deliberately does NOT do:
#   - Does not re-implement or duplicate [110]'s rule<->doc-FILE check; [113] assumes the doc
#     file resolution [110] proves and asks a strictly different question (doc<->INDEX.md).
#   - Does not validate every `-P` flag anywhere in the hooks skill, only the one(s) on a
#     `gradlew`-invoking line inside the `hook-body` marker's fence body, so unrelated `-P`
#     mentions in prose (including the self-check checklist) do not pollute derivation.
#   - Does not render ax:subst tokens (see the token-substitution note in CHECK (a) above for
#     why that is safe for the specific positions this guard reads).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT="$REPO_ROOT"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "cross_artifact_contract_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$ROOT" ]; then
    echo "cross_artifact_contract_guard: root not found: $ROOT" >&2
    exit 2
fi
ROOT="$(cd "$ROOT" && pwd)"

JAVA_SKILL="$ROOT/skills/ax-install-java-enforcement/SKILL.md"
HOOKS_SKILL="$ROOT/skills/ax-install-hooks/SKILL.md"
ESLINT_RULES_DIR="$ROOT/practices-react/eslint-plugin-ax/rules"
INDEX_FILE="$ROOT/practices-react/INDEX.md"
DOCS_DIR="$ROOT/practices-react/rules"

for f in "$JAVA_SKILL" "$HOOKS_SKILL" "$INDEX_FILE"; do
    if [ ! -f "$f" ]; then
        echo "cross_artifact_contract_guard: required file not found: $f" >&2
        exit 2
    fi
done
if [ ! -d "$ESLINT_RULES_DIR" ]; then
    echo "cross_artifact_contract_guard: $ESLINT_RULES_DIR not found" >&2
    exit 2
fi
if [ ! -d "$DOCS_DIR" ]; then
    echo "cross_artifact_contract_guard: $DOCS_DIR not found" >&2
    exit 2
fi

command -v python3 >/dev/null 2>&1 || {
    echo "cross_artifact_contract_guard: python3 required (to parse ax:artifact markers via ax_markers.py)" >&2
    exit 2
}

# RELOCATED-COPY AFFORDANCE (mirrors AX_MARKERS_LIB_DIR in install_artifact_extractability_
# guard.sh — see its own header for the fuller rationale): fixture_kill_proof_guard.sh [87]
# proves fixture non-vacuity by running a MUTATED COPY of this file from a bare temp path, where
# the repo-relative ax_markers.py does not exist. AX_MARKERS_LIB_DIR names it for THAT case
# only, and the gate is explicit: the override is consulted ONLY when the committed path is
# absent AND this root is not a git work tree — i.e. exactly the relocated sandbox. On any live
# tree a missing ax_markers.py is a BLOCK, never an invitation to load the module from elsewhere.
AX_MARKERS_DIR="$REPO_ROOT/practices/scripts/lib"
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ] \
   && ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    AX_MARKERS_DIR="${AX_MARKERS_LIB_DIR:-$AX_MARKERS_DIR}"
fi
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ]; then
    echo "cross_artifact_contract_guard: cannot find practices/scripts/lib/ax_markers.py under $REPO_ROOT" >&2
    exit 2
fi

violations=0

# ---------------------------------------------------------------------------
# CHECK (a) — hook <-> java-skill -P property-name contract, derived from each side's
# ax:artifact marker FENCE BODY ONLY (P2-110 fix — see header CORRECTION note).
# ---------------------------------------------------------------------------

HOOK_MARKER_ID="hook-body"
JAVA_MARKER_ID="java-gradle-testpractices"
NOTFOUND_SENTINEL="__AX_MARKER_NOT_FOUND__"

HOOK_BODY_OUT="$(mktemp)"
JAVA_BODY_OUT="$(mktemp)"
cleanup() { rm -f "$HOOK_BODY_OUT" "$JAVA_BODY_OUT"; }
trap cleanup EXIT

python3 - "$AX_MARKERS_DIR" "$HOOKS_SKILL" "$JAVA_SKILL" "$HOOK_BODY_OUT" "$JAVA_BODY_OUT" \
    "$HOOK_MARKER_ID" "$JAVA_MARKER_ID" "$NOTFOUND_SENTINEL" <<'PYEOF'
import sys
import os
import re

(ax_markers_dir, hooks_skill, java_skill, hook_out, java_out,
 hook_marker_id, java_marker_id, sentinel) = sys.argv[1:9]
sys.path.insert(0, ax_markers_dir)
import ax_markers  # noqa: E402

artifacts = ax_markers.discover([hooks_skill, java_skill])


def find(artifact_id, source_file):
    target = os.path.abspath(source_file)
    for a in artifacts:
        if a.id == artifact_id and os.path.abspath(a.source_file) == target:
            return a
    return None


JAVA_PROP_RE = re.compile(r'(?:findProperty|gradleProperty)\("([A-Za-z0-9_]+)"\)')
HOOK_PROP_RE = re.compile(r'-P([A-Za-z0-9_]+)=')

hook_artifact = find(hook_marker_id, hooks_skill)
with open(hook_out, "w", encoding="utf-8") as f:
    if hook_artifact is None:
        f.write(sentinel + "\n")
    else:
        names = set()
        for line in hook_artifact.body.split("\n"):
            if "gradlew" not in line:
                continue
            names.update(HOOK_PROP_RE.findall(line))
        for n in sorted(names):
            f.write(n + "\n")

java_artifact = find(java_marker_id, java_skill)
with open(java_out, "w", encoding="utf-8") as f:
    if java_artifact is None:
        f.write(sentinel + "\n")
    else:
        for n in sorted(set(JAVA_PROP_RE.findall(java_artifact.body))):
            f.write(n + "\n")
PYEOF
py_rc=$?
if [ "$py_rc" -ne 0 ]; then
    echo "cross_artifact_contract_guard: ax_markers.py marker discovery failed (exit $py_rc) — cannot verify check (a)" >&2
    exit 2
fi

java_body_raw="$(cat "$JAVA_BODY_OUT")"
hook_body_raw="$(cat "$HOOK_BODY_OUT")"

java_props=""
java_prop_count=0
hook_props=""
hook_prop_count=0

if [ "$java_body_raw" = "$NOTFOUND_SENTINEL" ]; then
    echo "VIOLATION(a): ax:artifact marker id=\"$JAVA_MARKER_ID\" not found in $JAVA_SKILL — cannot independently derive the java-side gradle property name (fail-closed BLOCK, not a silent skip)" >&2
    violations=$((violations + 1))
else
    java_props="$java_body_raw"
    java_prop_count="$(printf '%s\n' "$java_props" | grep -c . || true)"
    if [ "$java_prop_count" -eq 0 ]; then
        echo "VIOLATION(a): no findProperty(\"...\")/gradleProperty(\"...\") call found in the \"$JAVA_MARKER_ID\" marker body of $JAVA_SKILL — contract undeclared" >&2
        violations=$((violations + 1))
    elif [ "$java_prop_count" -gt 1 ]; then
        echo "VIOLATION(a): the \"$JAVA_MARKER_ID\" marker body of $JAVA_SKILL declares $java_prop_count distinct gradle property names ($(printf '%s' "$java_props" | tr '\n' ' ')) — ambiguous contract" >&2
        violations=$((violations + 1))
    fi
fi

if [ "$hook_body_raw" = "$NOTFOUND_SENTINEL" ]; then
    echo "VIOLATION(a): ax:artifact marker id=\"$HOOK_MARKER_ID\" not found in $HOOKS_SKILL — cannot independently derive the hook-side -P property name (fail-closed BLOCK, not a silent skip)" >&2
    violations=$((violations + 1))
else
    hook_props="$hook_body_raw"
    hook_prop_count="$(printf '%s\n' "$hook_props" | grep -c . || true)"
    if [ "$hook_prop_count" -eq 0 ]; then
        echo "VIOLATION(a): no -P<name>= flag found on any gradlew-invoking line inside the \"$HOOK_MARKER_ID\" marker body of $HOOKS_SKILL — this is the F-024/#86/P2-110 shape (the actual installed artifact carries no -P; prose elsewhere merely describing one does not count)" >&2
        violations=$((violations + 1))
    elif [ "$hook_prop_count" -gt 1 ]; then
        echo "VIOLATION(a): the \"$HOOK_MARKER_ID\" marker body of $HOOKS_SKILL's gradlew invocation(s) pass $hook_prop_count distinct -P property names ($(printf '%s' "$hook_props" | tr '\n' ' ')) — ambiguous contract" >&2
        violations=$((violations + 1))
    fi
fi

if [ "$java_prop_count" -eq 1 ] && [ "$hook_prop_count" -eq 1 ] && [ "$java_props" != "$hook_props" ]; then
    echo "VIOLATION(a): -P property drift — the \"$JAVA_MARKER_ID\" marker body of $JAVA_SKILL declares \"$java_props\" but the \"$HOOK_MARKER_ID\" marker body of $HOOKS_SKILL's gradlew call passes \"$hook_props\" (F-024/#86 shape)" >&2
    violations=$((violations + 1))
fi

# ---------------------------------------------------------------------------
# CHECK (b) — shipped ESLint rule id <-> INDEX.md visibility (word-bounded).
# ---------------------------------------------------------------------------

# All verification.rule_id values of shape `"ax/<id>"` declared anywhere under DOCS_DIR/*.md
# (identical extraction to [110]'s alias resolution — the quoted "ax/" prefix disambiguates
# from the unrelated top-level `rule_id:` field some docs carry for their own naming).
declared_rule_ids="$(grep -rhoE 'rule_id:[[:space:]]*"ax/[a-zA-Z0-9_-]+"' "$DOCS_DIR" 2>/dev/null \
    | sed -E 's/^rule_id:[[:space:]]*"ax\/([a-zA-Z0-9_-]+)"$/\1/' | sort -u)"

index_has_token() {
    # word-bounded literal search: token must not be glued to another id/word-char on
    # either side, so e.g. a hypothetical "no-god-route-2" entry cannot false-positive
    # coverage for "no-god-route".
    local token="$1"
    grep -qE "(^|[^A-Za-z0-9_-])${token}([^A-Za-z0-9_-]|$)" "$INDEX_FILE"
}

checked=0
shopt -s nullglob
for f in "$ESLINT_RULES_DIR"/*.js; do
    id="$(basename "$f" .js)"
    checked=$((checked + 1))

    # Method (1): the rule id itself is visible in INDEX.md.
    if index_has_token "$id"; then
        continue
    fi

    # Method (2): some doc's verification.rule_id == "ax/<id>" AND that doc's filename
    # slug is visible in INDEX.md.
    covered_via_alias=0
    if printf '%s\n' "$declared_rule_ids" | grep -qxF "$id"; then
        for docfile in "$DOCS_DIR"/*.md; do
            [ -f "$docfile" ] || continue
            slug="$(basename "$docfile" .md)"
            if grep -qE "rule_id:[[:space:]]*\"ax/${id}\"" "$docfile" && index_has_token "$slug"; then
                covered_via_alias=1
                break
            fi
        done
    fi
    if [ "$covered_via_alias" -eq 1 ]; then
        continue
    fi

    echo "VIOLATION(b): practices-react/eslint-plugin-ax/rules/$id.js has no visible entry in $INDEX_FILE — neither its own id nor an aliased doc slug (verification.rule_id: \"ax/$id\") appears there" >&2
    violations=$((violations + 1))
done
shopt -u nullglob

if [ "$checked" -eq 0 ]; then
    echo "cross_artifact_contract_guard: no *.js rules found under $ESLINT_RULES_DIR — check (b) had nothing to check"
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "cross_artifact_contract_guard: $violations violation(s) — BLOCKED" >&2
    exit 1
fi

echo "cross_artifact_contract_guard: PASS — check (a) -P property name (\"$java_props\") agrees between the \"$HOOK_MARKER_ID\" marker body of $HOOKS_SKILL and the \"$JAVA_MARKER_ID\" marker body of $JAVA_SKILL; check (b) all $checked eslint-plugin-ax rule id(s) visible in INDEX.md (direct or aliased)"
exit 0
