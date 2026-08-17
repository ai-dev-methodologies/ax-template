#!/usr/bin/env bash
# practices/evals/downstream_release_recency_guard.sh — guard [114], release gate.
#
# Closes GH #92 step 5: a push that bumps .claude-plugin/plugin.json's `version` must not ship
# unless the downstream fixture harness (practices/scripts/verify-downstream.sh, Layer 1) actually
# ran GREEN against the exact commit being pushed. This guard does not run the harness itself
# (that would make pre-push slow and network-dependent, and duplicate Layer 1's job) — it only
# AUDITS the log Layer 1 writes to `.ax-downstream/runs.jsonl`, offline, reading only what is
# already on disk. Same division of labor as the 49th guard
# (completion_checklist_recency_guard.sh) audits `.ax-verify/runs.jsonl` written by
# verify-completion.sh, without re-running R25.
#
# PLACEMENT: this guard runs from .githooks/pre-push (wired by the orchestrator), NOT from
# run-all-guards.sh / R25. Placing a version-bump gate inside R25 is a bootstrap deadlock: the
# very commit that bumps the version could never carry a post-bump audit log entry, because R25
# runs BEFORE that commit exists. Precedent: completion_checklist_recency_guard.sh is the 49th
# guard because of exactly this reasoning, and it too lives at pre-push, not inside R25. Order:
# commit -> verify-downstream.sh (writes .ax-downstream/runs.jsonl) -> R25 -> push (THIS guard).
#
# TRIGGER CONDITION: fires only when `.claude-plugin/plugin.json`'s `version` VALUE differs
# between the push range's base and the sha being pushed (a value comparison, not a file-touched
# comparison — the file can change without the version changing, e.g. a schema-shape edit, and
# that must NOT fire this gate).
#
# WHAT "PASS" REQUIRES — the LATEST line of `.ax-downstream/runs.jsonl` must satisfy ALL of:
#   (i)   head_sha            == the sha being pushed
#   (ii)  tree_clean           is boolean true
#   (iii) assertions           is a non-empty object whose values are ALL boolean true AND whose
#         KEY SET IS EXACTLY THE HARNESS'S DECLARED ASSERTION MANIFEST — missing key and extra key
#         both BLOCK. "every value that happens to be present is true" is NOT enough and was a real
#         hole: `{"forged-single": true}` satisfied it, so a single hand-typed line passed the
#         release gate while claiming nothing. The manifest is NOT re-listed here (a second source
#         of truth would drift); it is PARSED from the harness itself — the one
#         `# ax:assertions <id> <id> …` line in practices/scripts/verify-downstream.sh, read AT THE
#         PUSHED SHA. Add or remove a note() call there and this gate follows automatically, and
#         the harness cross-checks that line against the ids it actually records at run time
#         (exit 8 on drift), so the declaration cannot rot into a lie either.
#   (iiib) verdict             == "pass", and override == [] (an empty list, and the key must be
#         present). An --artifact-override run installs a body that is NOT what the SKILL.md
#         carries — it is a regression differential by construction and can never be release
#         evidence, no matter how green its assertions look.
#   (iv)  artifact_digests      is an object mapping each ax:artifact id (see
#         practices/scripts/lib/ax_markers.py) to the sha256 of that artifact's exact body text,
#         and this guard RECOMPUTES that same map from the SKILL.md files as they exist AT THE
#         PUSHED SHA and requires an EXACT key-for-key match (missing key, extra key, or any value
#         mismatch all BLOCK). This is the one check that is genuinely hard to forge here: a green
#         audit line can be hand-typed, but the digest comparison is against the marker bodies the
#         push actually carries, recomputed fresh every time — editing a SKILL.md after
#         verify-downstream.sh ran (even if the JSONL line is left untouched, or hand-edited to
#         match the OLD content) is caught because the recompute reads the NEW content.
#   (v)   the line's FIELD SET is EXACTLY what the harness's audit writer emits — no missing key,
#         no extra key — with that pin CROSS-CHECKED (live mode) against the single `entry = {…}`
#         literal in the harness at the pushed sha, so the pin cannot rot into a lie. P2-106: every
#         check (i)-(iv) is a check on a field the line's own author chose to supply; none of them
#         asked whether the record had the SHAPE a genuine run produces. Measured pre-fix, a line
#         with only the six audited fields (no `timestamp`, no `harness`) plus one invented key
#         passed with exit 0.
#   (vi)  the digest recompute in (iv) actually MEASURED SOMETHING — a recompute that discovers
#         zero ax:artifact markers BLOCKS instead of comparing {} to {} and reporting a match. The
#         gate's strongest check must not be silently convertible into a tautology. Correspondingly
#         a SKILL.md that ls-tree lists but show cannot read BLOCKS rather than being skipped: a
#         silently narrower recompute is a smaller universe, not a smaller job.
#   (vii) the repository carries NO git REPLACEMENT REFS (live mode), and every git call runs with
#         GIT_NO_REPLACE_OBJECTS=1. `git replace` keeps shas identical and swaps the objects, and
#         in live mode EVERY input here is a git read — both plugin.json bodies, the harness that
#         declares the manifest, and the SKILL.md bodies behind (iv). Measured pre-fix: one
#         `git replace` aimed the pushed sha at a version-unchanged commit and the gate reported
#         "version unchanged — gate does not fire", exit 0, with no log consulted at all.
#   (viii) the GIT_*/PYTHON* environment families are scrubbed (and the scrub VERIFIED) before any
#         of the above runs. Measured pre-fix: `GIT_DIR=<empty decoy>/.git` made the gate announce
#         "not a plugin tree, gate does not apply" and exit 0 — a one-variable bypass of the whole
#         release gate, cheaper than the hand-authored log line this header calls the cheap forgery.
#
# HONEST STRENGTH DECLARATION (do not overclaim — this was a Critic-flagged risk in the design
# discussion for this guard, referenced as "D13"):
#   completion_checklist_recency_guard.sh is ~2,500 lines and ~14 forgery-resistance checks: a
#   privileged-mode re-exec that neutralizes BASH_ENV/exported-function hijacking, a full
#   GIT_*/PYTHON* environment scrub, tool identity smoke-tests, mktemp trust verification, git
#   `--no-replace-objects`, byte-for-byte tree fingerprinting sampled at every step boundary, a
#   remote-ref anchor binding, and a duplicate-key-rejecting JSON parser — and its own header
#   states plainly that even all of that does not make forgery IMPOSSIBLE, only that it closes the
#   cheap, observed shape (a hand-appended JSONL line).
#   THIS GUARD STILL DOES NOT REIMPLEMENT THAT MACHINERY, and it is a SMALLER, WEAKER gate. What
#   P2-106 changed (2026-08-18) is the subset of it that this gate's own inputs actually needed:
#   the schema pin + writer cross-check (v), the non-vacuity of the recompute (vi), the
#   replacement-ref refusal (vii), and the environment scrub (viii). Each was added because a
#   MEASURED forgery walked through the pre-fix gate, not because the precedent has one.
#   WHAT IS STILL ABSENT, named so nobody has to rediscover it:
#     · NO PRIVILEGED RE-EXEC. The precedent re-execs itself under `bash -p` with an `env -i`
#       allowlist before its first ordinary command, because $BASH_ENV is sourced before a
#       script's first line and an exported function can shadow `set`/`[`/`unset`. This gate's
#       scrub (viii) runs as the first executable text and VERIFIES itself with keyword-only
#       constructs, so a neutered `unset` BLOCKS — but a $BASH_ENV payload whose first line is
#       `exit 0` still ends the shell before any of it exists.
#     · NO TOOL-IDENTITY CHECK. The `git` and `python3` this gate runs are the ambient ones; the
#       hook's hermetic PATH bootstrap covers its own git, not this subprocess's interpreter.
#     · NO SECOND ARTIFACT TO CORROBORATE. The precedent's check 14 requires the run's per-step
#       ledger to agree with its summary line. verify-downstream.sh publishes no surviving second
#       artifact (its per-run scratch dir is torn down), so there is nothing here to demand.
#       Making it publish one is a change to the harness, which this gate does not own.
#     · NO KEY. An HMAC would make forgery infeasible instead of merely inconvenient, and it is
#       deliberately not shipped for the same reason the precedent gives: a PUBLIC fork-base
#       catalog has nowhere to keep a key, and a key committed beside the data authenticates
#       nothing.
#   So the residual is REAL and unchanged in kind: a party with write access to the pushing
#   machine who is willing to run the same commands the harness runs can still fabricate a passing
#   record. Everything recomputable is also reproducible. What this gate closes is the CHEAP,
#   OBSERVED shapes — the hand-appended line, the shape-wrong line, the vacuous recompute, the
#   swapped object graph, the redirected environment.
#
# NOT IMPLEMENTED, DELIBERATELY (P2-106 — over-implementation is a defect too; each of these is a
# check the precedent has, or an obvious-looking one, that does NOTHING for THIS gate's subject):
#   · VALUE-SHAPE PINS on head_sha / the digest values (40-hex, 64-hex). The precedent needs them
#     because its tree_fingerprint was compared to nothing (`"x"` passed). Here every one of those
#     values is already compared for EQUALITY against something this gate derives itself — the sha
#     git resolves, the digest map recomputed from the tree — so a value of the wrong shape cannot
#     be equal to a value of the right one. A regex would add a second, weaker statement of a check
#     that already holds, and would grow a second place to update when the shapes change.
#   · A `harness` VALUE check ("must say practices/scripts/verify-downstream.sh"). Self-asserting:
#     a forger who types the field types its value. Its PRESENCE is load-bearing (v); its content
#     authenticates nothing and pretending otherwise is exactly the theater this family refuses.
#   · TIMESTAMP PLAUSIBILITY (parses as a date / is recent). The gate's freshness question is
#     answered by head_sha + tree_clean + the digest recompute, all of which bind to the pushed
#     TREE. A clock reading binds to nothing and is trivially typed; the precedent does not check
#     its `ts` either.
#   · SYMLINK / PERMISSION CHECKS on .ax-downstream/runs.jsonl. Whoever can point that path at
#     another file can equally well append to the real one — the check would cost a code path and
#     close nothing.
#   · GIT CONTENT FILTERS (the precedent's 12b0). That refusal exists because the precedent
#     compares WORKING-TREE BYTES to blobs, and a clean/process filter sits between them. This
#     gate never reads a working file in live mode: plugin.json, the harness and every SKILL.md
#     come from `git show <sha>:<path>`, which serves blob content. Nothing here is filterable.
#   · TREE SAMPLING ACROSS THE RUN (the precedent's 7/8). That closes a measured 2,225-second
#     window in which R25 runs its steps. This gate performs one offline audit lasting under a
#     second and computes no verdict from anything it could re-read; there is no window to sample.
#
# FIXTURE / --root DESIGN: fixture trees under
# practices/evals/fixtures/downstream_release_recency/ are NOT git repositories (no .git/) so that
# they can be committed as plain tracked files, mirroring completion_checklist_recency's
# `.ax-verify/`-based fixtures. This guard detects git-rootedness itself:
#   - LIVE mode (SCAN_ROOT/.git resolves): version-before/after and the SKILL.md content used for
#     digest recompute are all read via `git show <sha>:<path>` — never the working tree — so a
#     dirty local edit after committing cannot influence the verdict.
#   - FIXTURE-SHAPED mode (no .git): version-after comes from <root>/.claude-plugin/plugin.json,
#     version-before from <root>/.ax-downstream/prev_version.txt, the sha to match against
#     runs.jsonl's head_sha from <root>/.ax-downstream/expected_head.txt, the declared assertion
#     manifest from <root>/.ax-downstream/expected_assertions.txt (whitespace-separated ids,
#     #-comments ignored — the stand-in for parsing the harness, which a fixture tree does not
#     carry), and the SKILL.md content
#     for digest recompute is read directly from <root>/skills/*/SKILL.md on disk. This mode is
#     used both by `--fixtures` (which drives it once per pass_*/fail_* subdirectory) and by any
#     manual `--root DIR` invocation against a non-git directory.
#
# OPT-OUT: AX_SKIP_DOWNSTREAM_RELEASE_GATE=1 is honored ONLY INSIDE THE ONE DIAGNOSED CASE it was
#   written for — a base commit that could not be resolved (root commit / shallow clone / a
#   fork-receiver's first push to a new remote branch). BACKLOG P2-111(a), an external adversarial
#   critic's measurement: this variable used to be consulted BEFORE applicability and BEFORE base
#   resolution were even attempted, so exporting it turned the release gate off unconditionally —
#   a one-line bypass of the entire gate, reachable by anyone who read the header.
#   The rule now is:
#     · base resolution FAILED + opt-out set  → skip, loudly, exit 0 (the diagnosed case).
#     · base resolution FAILED + no opt-out   → exit 3, loudly, with the opt-out spelled inline.
#     · anything else + opt-out set           → the variable is IGNORED and the gate runs in full.
#       The fact that it was ignored is printed LOUDLY on stderr: a silently-ignored kill switch
#       leaves an operator staring at a gate that "should have been off" with no explanation.
#   Still honored ONLY for a live invocation (no `--root`; `--live-root` counts as live — see
#   below). A `--root` call is by construction a controlled/test call and must not be defeatable
#   by whatever the ambient shell exports, or `--fixtures` would spuriously pass in such an
#   environment. In FIXTURE-SHAPED mode the diagnosed case cannot arise at all: there is no base
#   COMMIT to resolve, only a prev_version.txt that is either there or not (and "not there" is a
#   legitimate "no previous version", not a resolution failure). Fixture
#   fail_optout_outside_diagnosed_case asserts exactly that — with the variable exported, a
#   failing tree still exits 1.
#
# DECOMMISSION vs "not a plugin tree" (BACKLOG P2-111(c)): a push whose head carries NO
#   .claude-plugin/plugin.json is not automatically out of scope. Two different shapes hide there
#   and they must not be collapsed:
#     · the manifest exists on the BASE side and is GONE at head → the push UNPUBLISHES the
#       plugin. That is a legitimate operation and it passes — but it is announced LOUDLY on
#       stderr, because "delete the manifest, push, restore it later" is otherwise a quiet way to
#       move a tree past this gate. (Restoring it later fires the gate anyway: that range's base
#       has no version and its head does, which reads as a changed value.)
#     · the manifest is absent on BOTH sides → this is simply not a plugin tree. Nothing to
#       check, silent pass.
#   The gate FIRES normally whenever the manifest is present at head, whatever the base side says.
#
# Exit codes: 0 pass (gate satisfied OR did not fire OR explicitly skipped) · 1 violation (used
# by every required fail_* fixture — practices/evals/fixture_kill_proof_guard.sh [87] only
# registers exit-1 fail fixtures) · 2 usage error · 3 base commit unresolved (live only).
# The environment refusal AX_DOWNSTREAM_ENV_UNSCRUBBABLE (viii) also exits 1: it is a BLOCK, and a
# new exit code would only give the pre-push hook a fourth branch to explain the same "stop".
#
# Usage:
#   bash practices/evals/downstream_release_recency_guard.sh
#       live repo, resolves head=HEAD and base=merge-base(HEAD, origin/HEAD or origin/main)
#   bash practices/evals/downstream_release_recency_guard.sh --head SHA --base SHA
#       live repo, caller-supplied range. .githooks/pre-push supplies the local_sha/base it already
#       resolved via pp_resolve_ref_base (see that function's header), and passes NO --base at all
#       when that resolution failed — P2-111(b): the gate is asked anyway and diagnoses the
#       unresolvable base itself instead of being skipped.
#   bash practices/evals/downstream_release_recency_guard.sh --root DIR
#       audit DIR instead of this repo; DIR may or may not be a git repository (see above).
#       A CONTROLLED/TEST call: the opt-out is never honored here.
#   bash practices/evals/downstream_release_recency_guard.sh --live-root DIR --head SHA --base SHA
#       audit git repository DIR as a GENUINE LIVE invocation. Exists for BACKLOG P2-111(d): the
#       pre-push hook extracts THIS FILE (and practices/scripts/lib/ax_markers.py) out of git into
#       a private /tmp directory and runs that copy, so the program doing the auditing is not
#       whatever the working checkout happens to hold. It prefers the PREVIOUS RELEASE's copy —
#       which the commit being pushed cannot have edited — and falls back to the pushed sha's own
#       copy when the previous release does not carry both files; pp_downstream_release_gate's
#       header states the full three-rung ladder and what each rung does and does not close. The
#       extracted copy is not inside the repository, so it must be told which tree to audit — and
#       it is still a live push gate, so the diagnosed-case opt-out must remain reachable, which
#       `--root` deliberately forbids.
#   bash practices/evals/downstream_release_recency_guard.sh --fixtures
#       run every pass_*/fail_* fixture under fixtures/downstream_release_recency/
#
# HERMETICITY, HONESTLY BOUNDED (P2-111(d), partial by construction): with the pre-push wiring
# above, the two files that decide this verdict — the guard and the marker parser — come out of
# git and are blob-hash-verified after extraction. On the STRONG rung they come from the previous
# release, so a commit that neuters the guard does not get to have the neutered guard clear it;
# on the WEAK rung they come from the pushed sha, which closes only the working-checkout gap; the
# LEGACY rung is the pre-P2-111(d) behaviour and closes neither. Every run prints which rung it
# took, and the ladder ratchets upward on its own once a `--live-root`-capable guard reaches the
# anchor branch. What is NOT closed by any rung: `.githooks/pre-push` and
# `.githooks/pre-push-lib.sh` are executed by git from the working checkout and cannot
# authenticate themselves (that residual is already stated in the hook's own header), and the
# `git`/`python3` binaries this guard invokes are the ambient ones — the hook's hermetic PATH
# bootstrap covers its own git, not this subprocess's python3. Both are backlog, not silently
# claimed here.

# ── (viii) RUNTIME-CONTEXT SCRUB — THE FIRST EXECUTABLE TEXT (P2-106) ────────────────
# MEASURED, pre-fix, against the guard as committed at bd19e251 and the real clone of this
# repository (isolated copy, so a concurrent lane could not move the log under the measurement):
#     GIT_DIR=/tmp/decoy/.git bash <guard> --live-root <clone> --head bd19e251… --base f4457530
#       → "no plugin manifest on either side of this range — not a plugin tree, gate does not
#          apply."  exit 0
# One exported variable, no repository access, and the RELEASE GATE DECLARES ITSELF INAPPLICABLE.
# That is cheaper than the hand-authored log line this guard's header calls "the cheap forgery",
# and it does not even need a log line: every `git -C <root>` call below — the version-before/after
# comparison, the assertion manifest, and the digest recompute that is this gate's one check with
# real teeth — is answered out of whatever object store the environment names.
# GIT_DIR / GIT_WORK_TREE / GIT_OBJECT_DIRECTORY / GIT_ALTERNATE_OBJECT_DIRECTORIES /
# GIT_CEILING_DIRECTORIES / GIT_CONFIG* are all that class, and PYTHON* is the same hole for the
# interpreter (PYTHONPATH precedes the stdlib for a `python3 -` script, so `hashlib` itself is
# shadowable). A denylist can only remove what somebody thought of, so this is a FAMILY sweep —
# the same reasoning that made the precedent's GIT_* scrub a family sweep rather than a name list.
# GIT_NO_REPLACE_OBJECTS is scrubbed with the family and RE-SET below, because it is the one
# member of it this gate wants (see (vii)).
# THE SCRUB IS VERIFIED, NOT ASSUMED: `unset` is an ordinary command lookup and a shell function
# can shadow it, so after the sweep the surviving names are re-read with `${!GIT_@}`/`${!PYTHON@}`
# — parameter expansions, not command lookups — and a non-empty remainder BLOCKS. A scrub that
# quietly did nothing would be worse than no scrub, because the header would then claim it.
# HONEST BOUND, stated rather than implied: this is NOT the precedent's privileged re-exec
# (`bash -p` + `env -i` allowlist). A $BASH_ENV payload still runs before this line, and `set`/`[`
# remain shadowable. That residual is the same one .githooks/pre-push already declares for itself
# — an adversary with that much control over the pushing shell can simply not install the hooks —
# and closing it here is registered as backlog, not silently claimed. See "NOT IMPLEMENTED" below.
for _ax_v in ${!GIT_@} ${!PYTHON@}; do
    unset "$_ax_v" 2>/dev/null
done
_ax_left=
for _ax_v in ${!GIT_@} ${!PYTHON@}; do _ax_left="$_ax_left $_ax_v"; done
case "$_ax_left" in
    "") ;;
    *)  _ax_null=
        _ax_die=${_ax_null:?"downstream_release_recency_guard: AX_DOWNSTREAM_ENV_UNSCRUBBABLE — the GIT_*/PYTHON* environment families were unset and are STILL present ($_ax_left). \`unset\` is an ordinary command lookup, so a shell function shadowing it defeats the scrub silently; this gate refuses to run in a context where it cannot establish which object store and which interpreter path its own subprocesses will use. Start it from a clean shell."} ;;
esac
unset _ax_v _ax_left
export GIT_NO_REPLACE_OBJECTS=1

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""
LIVE_ROOT_OVERRIDE=""
HEAD_ARG=""
BASE_ARG=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --live-root) LIVE_ROOT_OVERRIDE="$2"; shift 2 ;;
        --live-root=*) LIVE_ROOT_OVERRIDE="${1#--live-root=}"; shift ;;
        --head) HEAD_ARG="$2"; shift 2 ;;
        --head=*) HEAD_ARG="${1#--head=}"; shift ;;
        --base) BASE_ARG="$2"; shift 2 ;;
        --base=*) BASE_ARG="${1#--base=}"; shift ;;
        *) echo "downstream_release_recency_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/downstream_release_recency"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "downstream_release_recency_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    # A fixture whose basename mentions `optout` is run WITH AX_SKIP_DOWNSTREAM_RELEASE_GATE=1
    # exported, because that variable is the thing under test for it (P2-111(a)); every other
    # fixture is run with the variable explicitly UNSET, so an ambient export in the operator's
    # shell can neither weaken nor strengthen any fixture's verdict.
    fx_env() {
        case "$(basename "$1")" in
            *optout*) printf '%s' "1" ;;
            *)        printf '%s' "" ;;
        esac
    }

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if AX_SKIP_DOWNSTREAM_RELEASE_GATE="$(fx_env "$sub")" bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [downstream_release_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [downstream_release_recency/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        rc=0
        AX_SKIP_DOWNSTREAM_RELEASE_GATE="$(fx_env "$sub")" bash "$0" --root "$sub" >/dev/null 2>&1 || rc=$?
        if [ "$rc" -eq 1 ]; then
            echo "PASS [downstream_release_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [downstream_release_recency/$(basename "$sub")] — expected exit 1 on FAIL fixture, got $rc"
            fail=$((fail + 1))
        fi
    done

    echo ""
    echo "downstream_release_recency_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live / --root / --live-root mode ─────────────────────────────────────────
if [ -n "$ROOT_OVERRIDE" ] && [ -n "$LIVE_ROOT_OVERRIDE" ]; then
    echo "downstream_release_recency_guard: --root and --live-root are mutually exclusive (they " \
         "make opposite statements about whether this is a controlled call)." >&2
    exit 2
fi
SCAN_ROOT="${ROOT_OVERRIDE:-${LIVE_ROOT_OVERRIDE:-$REPO_ROOT}}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "downstream_release_recency_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi
SCAN_ROOT="$(cd "$SCAN_ROOT" && pwd)"

IS_LIVE=1
[ -n "$ROOT_OVERRIDE" ] && IS_LIVE=0

# Opt-out: RECORDED here, ACTED ON only inside the diagnosed case (base commit unresolved), deep
# in the python block below. P2-111(a): this used to `exit 0` right here, ahead of applicability
# and ahead of base resolution, which made the variable an unconditional off switch for the whole
# release gate. Live invocations only (`--root` is a controlled call — see header).
OPTOUT=0
if [ "$IS_LIVE" -eq 1 ] && [ "${AX_SKIP_DOWNSTREAM_RELEASE_GATE:-}" = "1" ]; then
    OPTOUT=1
    echo "downstream_release_recency_guard: AX_SKIP_DOWNSTREAM_RELEASE_GATE=1 is set. It is honored" >&2
    echo "  ONLY if this run cannot resolve a base commit — the one case this gate refuses to guess" >&2
    echo "  through. In every other situation it is IGNORED and the gate runs in full; if that is" >&2
    echo "  what happens, the next line will say so rather than leave you wondering." >&2
fi

# RELOCATED-COPY AFFORDANCE (mirrors AX_RELEASE_ANCHOR_LIB — see
# evidence_quote_spotcheck_guard.sh / manifest_snapshot_integrity_guard.sh, and
# install_artifact_extractability_guard.sh's identical AX_MARKERS_LIB_DIR): fixture_kill_proof_
# guard.sh [87] proves fixture non-vacuity by running a MUTATED COPY of this file from a bare temp
# path, where the repo-relative ax_markers.py does not exist. AX_MARKERS_LIB_DIR names it for
# THAT case only, and the gate is explicit: the override is consulted ONLY when the committed path
# is absent AND this root is not a git work tree — i.e. exactly the relocated sandbox. On any live
# tree a missing ax_markers.py is a BLOCK, never an invitation to load the module from elsewhere.
AX_MARKERS_DIR="$REPO_ROOT/practices/scripts/lib"
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ] \
   && ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    AX_MARKERS_DIR="${AX_MARKERS_LIB_DIR:-$AX_MARKERS_DIR}"
fi
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ]; then
    echo "downstream_release_recency_guard: cannot find practices/scripts/lib/ax_markers.py under $REPO_ROOT" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" "$HEAD_ARG" "$BASE_ARG" "$AX_MARKERS_DIR" "$OPTOUT" <<'PYEOF'
import sys
import os
import json
import hashlib
import re
import subprocess
import tempfile
import shutil

root, head_arg, base_arg, ax_markers_dir, optout_arg = sys.argv[1:6]
optout = optout_arg == "1"
sys.path.insert(0, ax_markers_dir)
import ax_markers  # noqa: E402


def fail(code, *lines):
    print(f"downstream_release_recency_guard: {code}", file=sys.stderr)
    for ln in lines:
        print(f"  {ln}", file=sys.stderr)
    sys.exit(1)


def git(*args, cwd):
    p = subprocess.run(["git"] + list(args), cwd=cwd, stdout=subprocess.PIPE,
                        stderr=subprocess.DEVNULL)
    return p.returncode, p.stdout.decode(errors="replace").strip()


def digest_map_from_paths(paths):
    """id -> sha256(body) for every ax:artifact marker discovered across `paths`."""
    artifacts = ax_markers.discover(paths)
    out = {}
    for a in artifacts:
        if not a.id:
            continue
        out[a.id] = hashlib.sha256(a.body.encode("utf-8")).hexdigest()
    return out


def read_json_file(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def applicability_or_exit(version_before, version_after, before_present, after_present):
    """Decide whether the release gate FIRES, or exit 0 on one of the three non-firing shapes.

    P2-111(c): "head carries no manifest" was previously read as one thing ("not applicable") when
    it is two. Removing a manifest that the base side HAD is a decommission — a real operation
    with a real release surface behind it, so it is announced rather than absorbed into silence;
    otherwise "delete it, push, restore it" is a quiet way past this gate. A manifest absent on
    BOTH sides is the genuinely inapplicable shape and stays silent.
    """
    if not after_present:
        if before_present:
            print("downstream_release_recency_guard: DECOMMISSION — this push REMOVES the plugin "
                  "manifest that the base side carried.", file=sys.stderr)
            print("  This is not a release, so the release gate has nothing to audit and does not "
                  "block it. It is printed LOUDLY on purpose: deleting the manifest is the one "
                  "shape that looks like 'gate does not apply' while actually changing what the "
                  "tree publishes. Restoring it in a later push fires the gate normally — that "
                  "range's base has no version and its head does.", file=sys.stderr)
            sys.exit(0)
        print("downstream_release_recency_guard: no plugin manifest on either side of this range "
              "— not a plugin tree, gate does not apply.")
        sys.exit(0)
    if version_before == version_after:
        print("downstream_release_recency_guard: plugin.json version unchanged in this "
              "push range — gate does not fire.")
        sys.exit(0)


HARNESS_REL = "practices/scripts/verify-downstream.sh"
MANIFEST_RE = re.compile(r"^#\s*ax:assertions\s+(\S.*)$")
MANIFEST_SOURCE = "(unresolved)"

# ── (v) THE AUDIT LINE'S FIELD SET, PINNED (P2-106) ──────────────────────────────────
# MEASURED, pre-fix: a line carrying ONLY the six fields this gate reads —
#     {"head_sha":…, "tree_clean":true, "assertions":{…}, "artifact_digests":{…},
#      "override":[], "verdict":"pass", "I_TYPED_THIS_BY_HAND":"yes"}
# — passed with exit 0. No `timestamp`, no `harness`, plus a field no writer has ever emitted.
# Every check this gate had was a check on a field the FORGER CHOSE TO SUPPLY; nothing asked
# whether the record had the shape a genuine run produces. The committed fixtures proved the same
# point from the other side: they carried a `"ts"` key the writer does not emit at all, and passed.
# A hand-authored record is written by hand — it carries the fields its author knew about — so
# pinning the exact set turns "I copied an old line and edited the shas" into a failure and turns
# every field the harness gains later into a new detector.
AUDIT_SCHEMA_KEYS = (
    "timestamp", "head_sha", "tree_clean", "assertions", "artifact_digests",
    "override", "verdict", "harness",
)


def harness_text_at_head():
    """practices/scripts/verify-downstream.sh AS COMMITTED AT THE PUSHED SHA. Live mode only.

    Read from git, never from the working tree, for the same reason the assertion manifest is:
    the program whose output this gate audits must be the one the push actually carries.
    """
    rc, content = git("show", f"{head_sha}:{HARNESS_REL}", cwd=root)
    return rc, content


def writer_emitted_schema():
    """The field set the COMMITTED harness's audit writer actually emits.

    The pin above is only worth having if it cannot rot: if the harness gains a field and this
    tuple does not, the pin would start rejecting every honest run (or, worse, a future editor
    would "fix" it by deleting the pin). So the pin is corroborated against the code that writes
    the file this gate reads. The extraction is SINGLE-AUTHORITATIVE — exactly one line-initial
    `entry = {` literal, and the file must actually serialize `entry` — because "a dict that looks
    like the schema" is not the schema; the schema is the shape of the write this gate reads back.
    Zero means the harness no longer emits the record; two or more means this gate cannot say
    which one is authoritative, and unknown never passes.
    """
    rc, content = harness_text_at_head()
    if rc != 0 or not content:
        fail("AX_DOWNSTREAM_WRITER_SCHEMA_UNRESOLVED",
             f"could not read {HARNESS_REL} at {head_sha!r}, so the pinned audit-line shape "
             "stands uncorroborated. A pin nobody checked against the writer is a comment.")
    lines = content.split("\n")
    starts = [i for i, ln in enumerate(lines) if re.match(r"^entry = \{\s*$", ln)]
    if len(starts) != 1:
        fail("AX_DOWNSTREAM_WRITER_SCHEMA_UNRESOLVED",
             f"{HARNESS_REL} at {head_sha!r} contains {len(starts)} line-initial `entry = {{` "
             "literals and this gate requires EXACTLY ONE. Zero means the harness no longer "
             "builds the record this gate verifies; two or more means 'the first regex-shaped "
             "hit' would silently become the authority.")
    i = starts[0] + 1
    body = []
    while i < len(lines) and not re.match(r"^\}\s*$", lines[i]):
        body.append(lines[i])
        i += 1
    if i >= len(lines):
        fail("AX_DOWNSTREAM_WRITER_SCHEMA_UNRESOLVED",
             f"the `entry = {{` literal in {HARNESS_REL} at {head_sha!r} is never closed by a "
             "line-initial `}}`, so its field set could not be read.")
    keys = tuple(m.group(1) for m in
                 (re.match(r'\s*"([a-z_]+)":', ln) for ln in body) if m)
    if not keys:
        fail("AX_DOWNSTREAM_WRITER_SCHEMA_UNRESOLVED",
             f"the `entry = {{` literal in {HARNESS_REL} at {head_sha!r} declares no string keys.")
    if "json.dumps(entry" not in content:
        fail("AX_DOWNSTREAM_WRITER_SCHEMA_UNRESOLVED",
             f"{HARNESS_REL} at {head_sha!r} builds an `entry` dict but never serializes it. The "
             "pin must be taken from the statement that WRITES the log this gate reads, not from "
             "a dict that resembles it.")
    return keys


def declared_assertion_ids():
    """The COMPLETE set of assertion ids a full harness run must record.

    Derived, never duplicated: in live mode from the harness's single `# ax:assertions` line AS IT
    EXISTS AT THE PUSHED SHA (so the gate follows the harness automatically, and a hand-edited
    working copy cannot widen or narrow it); in fixture-shaped mode from the fixture's own
    .ax-downstream/expected_assertions.txt, because a fixture tree carries no harness. An
    unresolvable manifest is a BLOCK, never a skipped check.
    """
    global MANIFEST_SOURCE
    if is_git:
        MANIFEST_SOURCE = f"{HARNESS_REL} @ {expected_head}"
        rc, content = git("show", f"{head_sha}:{HARNESS_REL}", cwd=root)
        if rc != 0 or not content:
            fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
                 f"could not read {HARNESS_REL} at {head_sha!r}. The gate cannot know which "
                 "assertions a complete run must record, so it refuses to accept the log.")
        decls = [m.group(1) for m in
                 (MANIFEST_RE.match(ln) for ln in content.splitlines()) if m]
    else:
        MANIFEST_SOURCE = ".ax-downstream/expected_assertions.txt"
        path = os.path.join(root, ".ax-downstream", "expected_assertions.txt")
        if not os.path.isfile(path):
            fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
                 f"{path} does not exist. In fixture-shaped mode this file declares the complete "
                 "assertion set; without it the completeness check cannot run.")
        with open(path, encoding="utf-8") as f:
            body = "\n".join(ln for ln in f.read().splitlines()
                             if ln.strip() and not ln.strip().startswith("#"))
        decls = [body] if body.strip() else []

    if len(decls) != 1:
        fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
             f"expected exactly ONE assertion-manifest declaration in {MANIFEST_SOURCE}, found "
             f"{len(decls)}.")
    ids = set(decls[0].split())
    if not ids:
        fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
             f"the assertion manifest in {MANIFEST_SOURCE} declares zero ids — an empty manifest "
             "would make the completeness check vacuous.")
    return ids


# ── Detect git-rootedness ────────────────────────────────────────────────────
# `git -C <root> rev-parse --git-dir` WALKS UP when <root> is not itself a work tree — every
# fixture under practices/evals/fixtures/ lives INSIDE this repository, so that check alone would
# always succeed and treat every fixture as "live git mode" against ax-template's own history.
# The real question is whether ROOT ITSELF is the toplevel of a git work tree.
rc, toplevel = git("rev-parse", "--show-toplevel", cwd=root)
is_git = False
if rc == 0 and toplevel:
    try:
        is_git = os.path.samefile(toplevel, root)
    except OSError:
        is_git = False

tmpdir = None
try:
    if is_git:
        # ── (vii) REPLACEMENT REFS — checked BEFORE any git answer is believed ────────────
        # MEASURED, pre-fix, in an isolated clone at bd19e251 (the real tree, the real log):
        #     git replace -f bd19e251… <a commit whose plugin.json still says the BASE version>
        #     bash <guard> --live-root <clone> --head bd19e251… --base f4457530
        #       → "plugin.json version unchanged in this push range — gate does not fire."  exit 0
        # `git replace` keeps every sha IDENTICAL and swaps the object that show/ls-tree/merge-base
        # read. Every input this gate has in live mode is a git read: both plugin.json bodies, the
        # harness that declares the assertion manifest, and the SKILL.md bodies whose digests are
        # recomputed. All of them can be answered out of a fabricated graph while the audit line
        # records authentic shas — so the ONE check with teeth can be made to agree with a stale
        # log instead of the pushed tree. GIT_NO_REPLACE_OBJECTS=1 is exported by the shell half,
        # and a tree carrying such refs AT ALL is refused: "we read past them" is a claim about
        # every call site forever, and a released catalog has no reason to carry them.
        # FAIL-CLOSED on enumeration failure — an enumeration that did not run cannot report
        # emptiness, and "unanswerable" is the one state an attacker can manufacture.
        rc_rep, replace_refs = git("for-each-ref", "--format=%(refname)", "refs/replace/",
                                   cwd=root)
        if rc_rep != 0:
            fail("AX_DOWNSTREAM_REPLACE_REFS_PRESENT",
                 f"the replacement-ref enumeration itself failed (git exited {rc_rep}) in {root}, "
                 "so this gate cannot tell whether refs/replace/* is empty. That question decides "
                 "whether every git answer below came out of a fabricated object graph, and an "
                 "unanswerable question fails closed.")
        if replace_refs:
            fail("AX_DOWNSTREAM_REPLACE_REFS_PRESENT",
                 f"this repository carries git replacement refs "
                 f"({replace_refs.splitlines()}). They keep every sha identical while swapping the "
                 "OBJECT that show/ls-tree/merge-base read, so plugin.json's version, the "
                 "assertion manifest and the recomputed artifact digests can all be answered out "
                 "of a fabricated graph. Remove them (`git replace -d <ref>`) and re-run.")

        head_sha = head_arg
        if not head_sha:
            rc, out = git("rev-parse", "HEAD", cwd=root)
            head_sha = out if rc == 0 else ""
        if not head_sha:
            print("downstream_release_recency_guard: AX_DOWNSTREAM_HEAD_UNRESOLVED — could not "
                  "resolve a head sha for this repository.", file=sys.stderr)
            sys.exit(2)

        base_sha = base_arg
        if not base_sha:
            rc, out = git("merge-base", head_sha, "origin/HEAD", cwd=root)
            base_sha = out if rc == 0 else ""
        if not base_sha:
            rc, out = git("merge-base", head_sha, "origin/main", cwd=root)
            base_sha = out if rc == 0 else ""
        if not base_sha:
            # THE DIAGNOSED CASE — the only place the opt-out means anything (P2-111(a)).
            if optout:
                print("downstream_release_recency_guard: base commit UNRESOLVED *and* "
                      "AX_SKIP_DOWNSTREAM_RELEASE_GATE=1 — gate skipped.", file=sys.stderr)
                print("  This is the single situation the opt-out exists for: with no base commit "
                      "there is no range in which to compare plugin.json's version, so the gate "
                      "cannot decide anything either way. The skip is recorded here, at the point "
                      "of diagnosis, rather than asserted before any of it was checked.",
                      file=sys.stderr)
                sys.exit(0)
            print("downstream_release_recency_guard: AX_DOWNSTREAM_BASE_UNRESOLVED — could not "
                  "resolve a base commit for this push range (root commit / shallow clone / "
                  "first push to a new remote branch are the known causes). This gate cannot "
                  "tell whether .claude-plugin/plugin.json's version changed in the range, so "
                  "it refuses to guess.", file=sys.stderr)
            print("  Opt out explicitly if this is a legitimate case: "
                  "AX_SKIP_DOWNSTREAM_RELEASE_GATE=1", file=sys.stderr)
            sys.exit(3)

        if optout:
            print("downstream_release_recency_guard: AX_SKIP_DOWNSTREAM_RELEASE_GATE=1 IGNORED — "
                  f"the base commit resolved fine ({base_sha[:12]}), so the diagnosed case this "
                  "opt-out exists for does not apply. The gate runs in full.", file=sys.stderr)

        rc, before_raw = git("show", f"{base_sha}:.claude-plugin/plugin.json", cwd=root)
        before_present = rc == 0
        version_before = None
        if before_present:
            try:
                version_before = json.loads(before_raw).get("version")
            except ValueError:
                version_before = None
        rc, after_raw = git("show", f"{head_sha}:.claude-plugin/plugin.json", cwd=root)
        after_present = rc == 0
        version_after = None
        if after_present:
            try:
                version_after = json.loads(after_raw).get("version")
            except ValueError:
                version_after = None

        applicability_or_exit(version_before, version_after, before_present, after_present)

        # Gate fires. Extract every skills/*/SKILL.md AT head_sha (never the working tree).
        rc, listing = git("ls-tree", "-r", "--name-only", head_sha, "--", "skills", cwd=root)
        skill_paths_in_tree = [ln for ln in listing.splitlines() if ln.endswith("/SKILL.md")]
        tmpdir = tempfile.mkdtemp(prefix="ax-downstream-recency-")
        extracted = []
        for idx, rel in enumerate(skill_paths_in_tree):
            rc, content = git("show", f"{head_sha}:{rel}", cwd=root)
            if rc != 0:
                # P2-106: this used to `continue`. A SKILL.md that ls-tree listed and show cannot
                # read is not "one fewer file to digest" — it SILENTLY NARROWS the recompute, and
                # a log line that omits the same ids then matches a smaller universe than the push
                # actually ships. The set comparison below cannot see the difference, because both
                # sides shrank. Unknown never passes.
                fail("AX_DOWNSTREAM_SKILL_UNREADABLE",
                     f"{rel} is listed in the tree of {head_sha!r} but its blob could not be read, "
                     "so the artifact digests recomputed below would silently cover fewer "
                     "artifacts than the push ships.")
            dest = os.path.join(tmpdir, f"skill_{idx}.md")
            with open(dest, "w", encoding="utf-8") as f:
                f.write(content)
            extracted.append(dest)
        recomputed = digest_map_from_paths(extracted)

        log_path = os.path.join(root, ".ax-downstream", "runs.jsonl")
        expected_head = head_sha
    else:
        # FIXTURE-SHAPED mode — no git dependency at all. There is no base COMMIT here, so the
        # diagnosed case the opt-out serves cannot arise; `optout` is therefore never consulted on
        # this path, which is what fail_optout_outside_diagnosed_case pins.
        plugin_path = os.path.join(root, ".claude-plugin", "plugin.json")
        after_present = os.path.isfile(plugin_path)
        plugin_json = read_json_file(plugin_path)
        version_after = plugin_json.get("version") if isinstance(plugin_json, dict) else None

        prev_version_file = os.path.join(root, ".ax-downstream", "prev_version.txt")
        before_present = os.path.isfile(prev_version_file)
        version_before = None
        if before_present:
            with open(prev_version_file, encoding="utf-8") as f:
                version_before = f.read().strip()

        applicability_or_exit(version_before, version_after, before_present, after_present)

        # ── (i-bis) THE SHA TO MATCH AGAINST MUST BE KNOWN — fail-open closed (P2-106) ────
        # MEASURED, pre-fix: delete expected_head.txt from a passing fixture tree and set the log
        # line's "head_sha" to "" → exit 0 ("PASS — audit log matches the pushed sha"). The
        # staleness check is an EQUALITY, so when this file is absent it compares "" to "" and
        # certifies a run about no commit at all. That is precisely the shape the precedent's
        # fail_git_context_redirected fixture pins for the 49th guard: when the gate cannot
        # establish WHICH head the record is supposed to be about, it must refuse, not default.
        expected_head_file = os.path.join(root, ".ax-downstream", "expected_head.txt")
        expected_head = ""
        if os.path.isfile(expected_head_file):
            with open(expected_head_file, encoding="utf-8") as f:
                expected_head = f.read().strip()
        if not expected_head:
            fail("AX_DOWNSTREAM_EXPECTED_HEAD_UNRESOLVED",
                 f"{expected_head_file} is absent or empty, so in fixture-shaped mode this gate "
                 "has no sha to hold the audit log against. An empty expectation would compare "
                 "equal to an empty recorded head_sha and certify a run about nothing.")

        import glob
        skill_paths = sorted(glob.glob(os.path.join(root, "skills", "*", "SKILL.md")))
        recomputed = digest_map_from_paths(skill_paths)

        log_path = os.path.join(root, ".ax-downstream", "runs.jsonl")

    # ── Common validation once the gate has fired ────────────────────────────
    if not os.path.isfile(log_path):
        fail("AX_DOWNSTREAM_LOG_MISSING",
             f"{log_path} does not exist. A version bump requires a fresh "
             "practices/scripts/verify-downstream.sh run's audit log for the pushed sha.")

    with open(log_path, encoding="utf-8") as f:
        lines = [ln for ln in f.read().splitlines() if ln.strip()]
    if not lines:
        fail("AX_DOWNSTREAM_LOG_MISSING", f"{log_path} exists but is empty.")

    def _reject_dup_keys(pairs):
        seen = set()
        out = {}
        for k, v in pairs:
            if k in seen:
                raise ValueError(f"duplicate key {k!r} in audit log line")
            seen.add(k)
            out[k] = v
        return out

    try:
        latest = json.loads(lines[-1], object_pairs_hook=_reject_dup_keys)
    except ValueError as exc:
        fail("AX_DOWNSTREAM_LOG_UNPARSEABLE", f"{log_path}'s latest line is not valid JSON: {exc}")

    if not isinstance(latest, dict):
        fail("AX_DOWNSTREAM_LOG_UNPARSEABLE", f"{log_path}'s latest line is not a JSON object.")

    # ── (v) SCHEMA PIN — the shape first, the values afterwards ──────────────────────
    # Placed here, ahead of every field-level check, because a record of the wrong shape does not
    # deserve field-level diagnosis: it was not written by the harness. (The duplicate-key refusal
    # above is the other half of the same idea — `object_pairs_hook` is applied to EVERY object in
    # the line, nested ones included, so a lenient parser cannot resolve a repeated key in the
    # writer's favour anywhere in the record.)
    line_keys = set(latest)
    pinned = set(AUDIT_SCHEMA_KEYS)
    if line_keys != pinned:
        fail("AX_DOWNSTREAM_LOG_SCHEMA_MISMATCH",
             f"the latest audit line's field set is not the one "
             f"{HARNESS_REL} emits (undeclared_extra={sorted(line_keys - pinned)}, "
             f"missing={sorted(pinned - line_keys)}).",
             ".ax-downstream/runs.jsonl is an ordinary text file, and every other check in this "
             "gate compares values the line's own author supplied. Requiring the exact shape "
             "means a hand-authored line has to reproduce the writer, not merely satisfy the "
             "reader. Re-run practices/scripts/verify-downstream.sh to get a genuine line.")

    # ── (v-bis) …AND THE PIN IS CORROBORATED AGAINST THE COMMITTED WRITER ────────────
    # Live mode only: a fixture tree carries no harness, exactly as the manifest declaration is
    # read from expected_assertions.txt there. The pin still applies to the LINE in both modes;
    # only this cross-check stands down, and only where there is nothing to cross-check against.
    if is_git:
        emitted = writer_emitted_schema()
        if tuple(AUDIT_SCHEMA_KEYS) != emitted:
            fail("AX_DOWNSTREAM_WRITER_SCHEMA_DRIFT",
                 f"this gate pins the audit-line shape as {list(AUDIT_SCHEMA_KEYS)} but the "
                 f"harness at {head_sha!r} emits {list(emitted)}.",
                 "The pin exists so a hand-authored line cannot pass as genuine; a pin that no "
                 "longer matches the writer either rejects every honest run or accepts a shape "
                 "nobody reviewed. Update BOTH in the same commit.")

    if latest.get("head_sha") != expected_head:
        fail("AX_DOWNSTREAM_LOG_STALE_HEAD",
             f"latest audit line's head_sha={latest.get('head_sha')!r} does not match the sha "
             f"being pushed ({expected_head!r}). Re-run verify-downstream.sh against the "
             "committed tree of the sha you are pushing.")

    if latest.get("tree_clean") is not True:
        fail("AX_DOWNSTREAM_LOG_DIRTY_TREE",
             f"latest audit line's tree_clean={latest.get('tree_clean')!r}, not boolean true. "
             "verify-downstream.sh must have run against a clean, fully-committed tree.")

    assertions = latest.get("assertions")
    if not isinstance(assertions, dict) or not assertions:
        fail("AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS",
             "latest audit line's 'assertions' is missing, not an object, or empty. A single "
             "summary flag is not accepted — every behavioral assertion must be individually "
             "recorded as boolean true.")
    failing = sorted(k for k, v in assertions.items() if v is not True)
    if failing:
        fail("AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS",
             f"the following assertion(s) are not boolean true: {failing}")

    # ── COMPLETENESS: the recorded key set must be EXACTLY the harness's declared manifest ──
    # Derived, never re-listed here (see header (iii)): parsed from the harness at the pushed sha
    # in live mode, from the fixture's expected_assertions.txt in fixture-shaped mode. Without
    # this, `{"forged-single": true}` passes every check above.
    declared = declared_assertion_ids()
    recorded_keys = set(assertions)
    missing_assertions = sorted(declared - recorded_keys)
    extra_assertions = sorted(recorded_keys - declared)
    if missing_assertions or extra_assertions:
        fail("AX_DOWNSTREAM_ASSERTION_SET_MISMATCH",
             f"the audit log's assertion key set is not the harness's declared set "
             f"({MANIFEST_SOURCE}). not_recorded={missing_assertions} "
             f"undeclared_extra={extra_assertions}",
             "A green line that records only some (or none) of the harness's assertions is not "
             "evidence that the harness ran — it is evidence that SOMETHING wrote a line.")

    if latest.get("verdict") != "pass":
        fail("AX_DOWNSTREAM_LOG_NOT_PASS",
             f"latest audit line's verdict={latest.get('verdict')!r}, not 'pass'. Only a run the "
             "harness itself declared passing can back a release.")

    override = latest.get("override")
    if override != []:
        fail("AX_DOWNSTREAM_LOG_OVERRIDE_PRESENT",
             f"latest audit line's override={override!r}; a release requires an empty list. An "
             "--artifact-override run installs a body the SKILL.md does not carry, so it is a "
             "regression differential by construction, never release evidence. A MISSING "
             "'override' key fails here too: it means the line was not written by the current "
             "harness schema.")

    # ── (vi) THE RECOMPUTE MUST HAVE MEASURED SOMETHING (P2-106) ─────────────────────
    # MEASURED, pre-fix: remove skills/ from a passing tree and set the line's
    # "artifact_digests" to {} → exit 0, "artifact digests match". They did: the empty map equals
    # the empty map. This gate's own header calls the digest recompute "the one check with
    # genuinely hard-to-forge teeth" — and its teeth were CONDITIONAL on there being artifacts to
    # find. Anything that empties the recompute (no skills/ at head, an ls-tree that returned
    # nothing, a marker syntax the parser stopped recognising) silently converts the strongest
    # check in the gate into a tautology, and the caller cannot tell from the PASS line.
    # Same rule the assertion manifest already applies to itself ("an empty manifest would make
    # the completeness check vacuous"), applied to the other completeness claim.
    if not recomputed:
        fail("AX_DOWNSTREAM_DIGEST_RECOMPUTE_VACUOUS",
             f"recomputing artifact digests from the SKILL.md files at {expected_head!r} found "
             "ZERO ax:artifact markers, so the digest comparison below would compare an empty map "
             "to an empty map and report a match.",
             "That check is the only one here that measures the PUSHED TREE rather than the log's "
             "own claims; with nothing to measure it certifies nothing. Either this tree ships no "
             "install artifacts (in which case it has no release surface for this gate to "
             "protect) or the marker discovery broke — both block.")

    logged_digests = latest.get("artifact_digests")
    if not isinstance(logged_digests, dict):
        fail("AX_DOWNSTREAM_DIGEST_MISMATCH",
             "latest audit line's 'artifact_digests' is missing or not an object.")

    recomputed_keys = set(recomputed)
    logged_keys = set(logged_digests)
    missing = sorted(recomputed_keys - logged_keys)
    extra = sorted(logged_keys - recomputed_keys)
    mismatched = sorted(
        k for k in (recomputed_keys & logged_keys) if recomputed[k] != logged_digests[k]
    )
    if missing or extra or mismatched:
        fail("AX_DOWNSTREAM_DIGEST_MISMATCH",
             f"recomputed artifact digests (from the SKILL.md files at {expected_head!r}) do not "
             f"match the audit log's artifact_digests. missing_in_log={missing} "
             f"extra_in_log={extra} value_mismatch={mismatched}")

    print("downstream_release_recency_guard: PASS — audit log matches the pushed sha, tree was "
          "clean, all assertions true, artifact digests match.")
    sys.exit(0)
finally:
    if tmpdir is not None:
        shutil.rmtree(tmpdir, ignore_errors=True)
PYEOF
exit $?
