#!/usr/bin/env bash
# practices/evals/resume_provenance_guard.sh — a BLOCKED run may not launder itself green [96]
#
# THE INVARIANT (binary, behavioral): verify-completion.sh must never certify a step whose
# outcome it did not observe, and a run that ends in a ledger BLOCK must leave behind NO
# resume record a later `--resume` can consume.
#
# WHY (the defect this pins, found by cross-family review of P0-30):
#   P0-30 made an incomplete result ledger a hard BLOCK (exit 2) — correct, but only for
#   the run in front of it. The step verdict was still "HARD_FAIL did not increase ⇒ PASS",
#   and PASS verdicts are published to .ax-verify/last_run.jsonl incrementally. So when a
#   step's commands never executed at all (the plan/ledger vanished mid-run — the 2026-07-29
#   TMPDIR-wipe shape), that step was written down as PASS. The run itself blocked, but the
#   NEXT invocation with --resume read that record, skipped the step, never ran the command,
#   and exited 0 with full_run=true. completion_checklist_recency_guard.sh validates
#   exit/hard_fail/full_run — not provenance — so the push would have been allowed.
#   Measured on the pre-fix script with the two-step harness below:
#     run 1 → exit 2 (BLOCK)  but  last_run.jsonl: {"step_id":"must-fail","status":"PASS"}
#     run 2 → exit 0, "PASS — all steps green", RESUME-SKIP 2, `false` never executed.
#
# THE SECOND DEFECT — same unsoundness, different entry point (cross-family review, 2026-07-29):
#   Layer (A) asks "did the step produce a ROW?", and a SKIPPED command produces one too.
#   verify-completion.sh recorded SKIP (not a failure) when a command's working directory was
#   absent, so the reviewer's harness laundered a step that never executed:
#     mv frontend frontend.off
#     verify-completion.sh --step frontend-lint   → exit 0, `npm run lint` NEVER RAN, PASS recorded
#     mv frontend.off frontend
#     verify-completion.sh --resume               → frontend-lint RESUME-SKIPped, exit 0, full_run=true
#   Measured on the pre-fix script: run1 exit 0 / {"step_id":"frontend-lint","status":"PASS"} /
#   marker file absent; run2 exit 0 "PASS — all steps green" with the command still never run.
#
# THE FOUR LAYERS THIS GUARD PINS (each asserted to block its harness on its own):
#   (A) provenance — a step may be recorded PASS only if at least one of its commands
#       produced an observed row in the result ledger; otherwise it is recorded UNRUN,
#       which the resume preloader (status == "PASS" only) never skips.
#   (B) publication — a run ending in LEDGER_BROKEN discards the resume record entirely
#       instead of publishing it. Suppressing the final mv is not sufficient: verdicts are
#       published incrementally, so the partial record must be actively removed.
#   (C) absent working directory BLOCKS a non-advisory command instead of skipping it.
#       Advisory commands stay advisory (WARN) — the fix must not turn a knowingly-advisory
#       item into a blocker.
#   (D) required-command coverage — a step is PASS only when EVERY planned non-advisory
#       command actually executed. This is what makes "the step has a row" insufficient on
#       its own, and it holds even where (C) does not apply.
#   (E) tree binding — a resume record is consumable only by the WORKING TREE that produced
#       it, not merely by the same head_sha. See THE THIRD DEFECT below.
#   (F) well-formed step contract — a SELECTED checklist step that cannot be RUN or cannot be
#       IDENTIFIED is a parse-time BLOCK. See THE FOURTH DEFECT below.
#   (G) declared ⊆ emitted — after execution, every step the checklist DECLARED for this run
#       must have emitted at least one plan row and have a usable id. This is the structural
#       backstop for (F): every other accounting check derives its step set FROM the plan, so
#       a step that never reached the plan is not merely unchecked, it is unseen.
#
# THE THIRD DEFECT — resume evidence was bound to HEAD, not to the tree that ran (2026-07-29):
#   R25 is routinely invoked on a dirty tree, so one head_sha covers arbitrarily many trees.
#   The preloader checked only {head_sha, status}, so the reviewer's path laundered a failing
#   tree green with no ledger damage and no missing directory at all:
#     1. at head H, an UNCOMMITTED edit makes `frontend-lint` pass → PASS record published
#     2. the edit is reverted — head is still H and the lint failure is back
#     3. `--resume` skips frontend-lint on that record: `npm run lint` never runs
#     4. the run publishes exit=0 / full_run=true for H, which the recency guard accepts
#   Measured on the pre-fix script in a throwaway git copy of this repo whose COMMITTED state
#   fails lint: run1 exit 0 (dirty tree) → revert → run2 --resume exit 0, "SKIP (resume)",
#   `npm run lint` never invoked, {"exit":0,...,"full_run":true} published at that head.
#   Post-fix the same path exits 1 with the record refused out loud and the lint re-run.
#
# THE FOURTH DEFECT — a MALFORMED step certifies that nothing happened (2026-07-29, extended):
#   The plan is a flat list of COMMANDS, so a step declaring `commands: []` emitted no row,
#   never entered STEP_ORDER, and was therefore invisible to every downstream accounting
#   check (all of which iterate STEP_ORDER). The non-empty-plan check was satisfied by any
#   other step. Measured on the pre-fix script with a two-step checklist (one empty, one
#   green): step_order=['green'], exit 0, "PASS — all steps green" — a required step of the
#   contract was silently dropped.
#   Cross-family review then proved the same certification through three more shapes, and
#   only the first was closed by the non-empty-commands check:
#     commands: [{command: "# temporarily disabled"}]  → `bash -c` exits 0, the runner
#       observes an exit code, and the row is recorded origin=executed: a genuine PASS by
#       every provenance rule in this file, with nothing run. (probe: shell exit 0)
#     commands: [{command: "   "}]                     → identical shape. (probe: shell exit 0)
#     id: "" (or absent/whitespace)                    → STEP_ORDER is iterated with
#       `for sid in $STEP_ORDER`, which word-splits: the step gets ZERO shell iterations, so
#       none of its commands run at all and the counters stay green. (probe: 0 iterations)
#   Post-fix each shape BLOCKs: (F) rejects all of them at parse time by name, and (G) is the
#   structural backstop for the two that make the step VANISH (empty commands ⇒ no plan row,
#   blank id ⇒ nothing iterable). The inert-command shapes are (F)-only by construction: they
#   DO emit a plan row and DO execute, so no structural check can see them — that asymmetry is
#   asserted, not glossed over, in the mutation matrix below.
#
# ALSO PINNED: SHORT_CIRCUITED must be initialised internally, never inherited. It gates
# the "every planned step has an outcome" accounting check, so an exported SHORT_CIRCUITED=1
# from the caller's environment would have suppressed a real accounting gap.
#
# NON-VACUITY (this is the part that makes the assertions worth anything):
#   The guard does not merely assert the fixed script behaves. It also runs the SAME
#   assertions against copies of verify-completion.sh with each layer and each pair
#   neutered — (A)/(B)/(A+B) against the ledger-loss harness, (C)/(D)/(C+D) against the
#   absent-working-directory harness — and requires that:
#     • both-neutered REPRODUCES the laundering (a second --resume run exits 0). If it does
#       not, the harness no longer exercises the defect and the whole guard is hollow ⇒ BLOCK.
#     • either layer alone still blocks it. Measured, not assumed — with (A) neutered the
#       resume run exits 2 (no record to consume); with (B) neutered the record survives but
#       carries {"step_id":"must-fail","status":"UNRUN"}, so the resume run re-executes the
#       command and exits 1. Both layers are kept: (B) refuses to certify anything from a
#       run whose accounting is unknown, (A) fixes the verdict rule itself and so does not
#       depend on the accounting check's fail_fast exemption staying correct.
#   (E) carries its own single-layer matrix: with the layer neutered in a copy of the live
#   script its harness must REPRODUCE the defect, and the live script must hold. (F)/(G) carry
#   a per-shape matrix instead, because their coverage genuinely differs by shape: F+G
#   neutered must reproduce ALL four shapes; F alone must reproduce exactly the two inert-
#   command shapes and still BLOCK the two vanishing ones (that is what proves G load-bearing);
#   G alone must block all four (F catches them earlier).
#   The (E) harness needs a real git working tree (that is what a tree fingerprint is
#   computed from), so its sandbox is `git init`ed and committed; the other harnesses stay
#   non-git, where the fingerprint degrades to a constant and resume behaves exactly as
#   before — which is also what keeps assertion 2 (legitimate resume) meaningful there.
#   The (C)/(D) matrix runs against a step with TWO non-advisory commands — one in an absent
#   directory, one that really runs — because that is the shape that separates them: with (C)
#   neutered the skipped command coexists with a genuine row, so only (D) can catch it.
#   Measured: C+D neutered → run2 exits 0 with the command never executed (defect reproduced);
#   (C) alone → run1 exits 1; (D) alone → the step is recorded UNRUN and re-executed.
#
# HARNESS ISOLATION (why a mktemp shim exists):
#   The reproduction needs a step that wipes the RUN'S OWN temp dir. macOS BSD `mktemp`
#   with no template ignores TMPDIR (it uses _CS_DARWIN_USER_TEMP_DIR), so pointing TMPDIR
#   at a private directory is not enough — without the shim the harness would either be a
#   no-op or, worse, wipe the machine's shared temp dir out from under concurrent work.
#   A PATH-earlier mktemp that honours TMPDIR makes the reproduction faithful AND contained.
#   Nothing outside the throwaway sandbox is touched; the live .ax-verify is never read.
#
# Usage:
#   bash practices/evals/resume_provenance_guard.sh
#       LIVE: assert the real verify-completion.sh, then the mutation matrix. Exit 0.
#   bash practices/evals/resume_provenance_guard.sh --fixture-root DIR
#       Run the SAME assertions against a copy of the real script neutered per DIR's
#       `neuter-mode` file (none|a|b|ab), skipping the matrix. The committed
#       fixtures/resume-provenance/fail_unfixed (mode ab) must exit 1 — that is what
#       proves these assertions detect the regression rather than passing vacuously.
#       The fixture carries a MODE, not a frozen copy of verify-completion.sh: the
#       mutation is always derived from the live script, so it cannot rot into testing
#       a stale artifact.
#
# Exit: 0 = provenance holds · 1 = a blocked run can launder a later resume green (BLOCK)
#       2 = setup/tooling error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# This guard needs the REAL verify-completion.sh, so it must find the repo even when the
# script itself is executed from a copy outside the tree — which is exactly what
# fixture_kill_proof_guard.sh [87] does when it runs the neutered mutant from a tmp path.
# Fall back to walking up from the working directory.
if [ ! -f "$REPO_ROOT/practices/scripts/verify-completion.sh" ]; then
    _d="$(pwd -P)"
    while [ "$_d" != "/" ]; do
        if [ -f "$_d/practices/scripts/verify-completion.sh" ]; then REPO_ROOT="$_d"; break; fi
        _d="$(dirname "$_d")"
    done
fi
REAL_VC="$REPO_ROOT/practices/scripts/verify-completion.sh"

FIXTURE_ROOT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --fixture-root)   FIXTURE_ROOT="$2"; shift 2 ;;
        --fixture-root=*) FIXTURE_ROOT="${1#--fixture-root=}"; shift ;;
        *) echo "resume_provenance_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SUBJECT_MODE="none"
if [ -n "$FIXTURE_ROOT" ]; then
    [ -d "$FIXTURE_ROOT" ] || {
        echo "resume_provenance_guard: fixture root not found: $FIXTURE_ROOT" >&2; exit 2; }
    [ -f "$FIXTURE_ROOT/neuter-mode" ] || {
        echo "resume_provenance_guard: fixture is missing its neuter-mode file: $FIXTURE_ROOT/neuter-mode" >&2; exit 2; }
    SUBJECT_MODE="$(tr -d ' \t\n' < "$FIXTURE_ROOT/neuter-mode")"
    case "$SUBJECT_MODE" in
        none|a|b|ab|c|d|cd|e|f|g|fg) ;;
        *) echo "resume_provenance_guard: unknown neuter-mode '$SUBJECT_MODE' in $FIXTURE_ROOT" >&2; exit 2 ;;
    esac
fi

[ -f "$REAL_VC" ] || { echo "resume_provenance_guard: verify-completion.sh not found at $REAL_VC" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "resume_provenance_guard: python3 required" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

VIOLATIONS=0
violation() { echo "  VIOLATION: $*" >&2; VIOLATIONS=$((VIOLATIONS + 1)); }

# ── Neuter helpers ───────────────────────────────────────────────────────────
# Layer (A): the UNRUN verdict is downgraded back to the pre-fix "absence of failure
# means PASS". Layer (B): a blocked run publishes its partial resume record again.
# Layer (C): an absent working directory stops blocking and degrades to the pre-fix
# silent skip. Layer (D): the required-command coverage test is short-circuited off.
NEUTER_A_ANCHOR='emit_resume "$sid" "UNRUN"'
NEUTER_A_VALUE='emit_resume "$sid" "PASS"'
NEUTER_B_ANCHOR='rm -f "$RESUME_LOG" "$RESUME_LOG.tmp.$$"'
NEUTER_B_VALUE='mv -f "$RESUME_NEW" "$RESUME_LOG"'
NEUTER_C_ANCHOR='dir_missing_blocks=1'
NEUTER_C_VALUE='dir_missing_blocks=0'
NEUTER_D_ANCHOR='elif [ "$req_planned" -gt 0 ] && [ "$req_executed" -lt "$req_planned" ]; then'
NEUTER_D_VALUE='elif false; then'
# Layer (E): the resume preloader stops comparing the record's tree fingerprint with the
# tree in front of it — i.e. back to "same head_sha is good enough". Layer (F): the parser
# stops rejecting malformed steps, so a step that cannot run (or cannot be identified) is
# admitted. Layer (G): the runner stops asserting that every DECLARED step emitted a plan
# row with a usable id, so a vanished step is unseen by all accounting.
NEUTER_E_ANCHOR='if rec_fp != tree_fp:'
NEUTER_E_VALUE='if False:'
NEUTER_F_ANCHOR='if problem:'
NEUTER_F_VALUE='if False:'
NEUTER_G_ANCHOR='if [ -n "$_ax_declared_broken" ] && [ "$SHORT_CIRCUITED" -eq 0 ]; then'
NEUTER_G_VALUE='if false; then'

# make_vc <dest> <subset of abcdefg | none> — write a (possibly neutered) verify-completion.sh.
make_vc() {
    local dest="$1" mode="$2"
    python3 - "$REAL_VC" "$dest" "$mode" \
        "$NEUTER_A_ANCHOR" "$NEUTER_A_VALUE" "$NEUTER_B_ANCHOR" "$NEUTER_B_VALUE" \
        "$NEUTER_C_ANCHOR" "$NEUTER_C_VALUE" "$NEUTER_D_ANCHOR" "$NEUTER_D_VALUE" \
        "$NEUTER_E_ANCHOR" "$NEUTER_E_VALUE" "$NEUTER_F_ANCHOR" "$NEUTER_F_VALUE" \
        "$NEUTER_G_ANCHOR" "$NEUTER_G_VALUE" <<'PY'
import sys
src, dest, mode = sys.argv[1:4]
pairs = sys.argv[4:18]
text = open(src, encoding="utf-8").read()
# The mode is a SET of layer letters, not a substring to search. "none" must select
# nothing — and it literally contains 'e'/'n'/'o', so `letter in mode` would silently
# neuter layer (e) on the unmutated subject and make the live assertion self-defeating.
selected = set() if mode in ("", "none") else set(mode)
layers = zip("abcdefg", pairs[0::2], pairs[1::2])
for want, anchor, value in layers:
    if want not in selected:
        continue
    n = text.count(anchor)
    if n != 1:
        print(f"anchor for layer {want} occurs {n}x (expected exactly 1): {anchor!r}", file=sys.stderr)
        sys.exit(3)
    text = text.replace(anchor, value)
open(dest, "w", encoding="utf-8").write(text)
PY
}

# ── Sandbox builder ──────────────────────────────────────────────────────────
# $1 = sandbox dir, $2 = verify-completion.sh to install, $3 = checklist body
build_sandbox() {
    local sb="$1" vc="$2" checklist="$3"
    mkdir -p "$sb/repo/practices/scripts" "$sb/tmp" "$sb/bin"
    cp "$vc" "$sb/repo/practices/scripts/verify-completion.sh"
    [ -f "$REPO_ROOT/practices/scripts/_collapse_plan.py" ] && \
        cp "$REPO_ROOT/practices/scripts/_collapse_plan.py" "$sb/repo/practices/scripts/"
    cat > "$sb/bin/mktemp" <<'SHIM'
#!/bin/sh
# TMPDIR-honouring mktemp (BSD mktemp ignores TMPDIR without a template).
case "${1:-}" in
  -d) exec /usr/bin/mktemp -d "${TMPDIR%/}/ax-shim.XXXXXXXX" ;;
  "")  exec /usr/bin/mktemp    "${TMPDIR%/}/ax-shim.XXXXXXXX" ;;
  *)  exec /usr/bin/mktemp "$@" ;;
esac
SHIM
    chmod +x "$sb/bin/mktemp"
    printf '%s\n' "$checklist" > "$sb/repo/practices/verification-checklist.yaml"
}

# vc_run <sandbox> <logfile> [env assignments via VC_ENV] -- <args...>
VC_ENV=""
vc_run() {
    local sb="$1" log="$2"; shift 2
    ( cd "$sb/repo" && env $VC_ENV TMPDIR="$sb/tmp" PATH="$sb/bin:$PATH" \
        bash practices/scripts/verify-completion.sh "$@" ) > "$log" 2>&1
    return $?
}

LAUNDER_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 3
checklist:
  - id: wipe-tmp
    title: "wipe the run temp dir (the P0-30 destructive-guard shape)"
    commands:
      - command: '"'"'find "$TMPDIR" -mindepth 1 -delete || true'"'"'
        expected_exit: 0
  - id: must-fail
    title: "a command that MUST be observed failing"
    commands:
      - command: '"'"'false'"'"'
        expected_exit: 0'

CLEAN_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 3
checklist:
  - id: alpha
    title: "trivially green step"
    commands:
      - command: '"'"'true'"'"'
        expected_exit: 0
  - id: beta
    title: "second trivially green step"
    commands:
      - command: '"'"'true'"'"'
        expected_exit: 0'

# The absent-working-directory harness. Step `mixed` deliberately pairs a command whose
# working_directory is moved away with one that really runs, so a skipped command coexists
# with a genuine ledger row — the shape that separates layer (C) from layer (D). The marker
# file is the ground truth: it exists if and only if the command actually executed.
WORKDIR_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: mixed
    title: "one command in a directory that is absent + one that really runs"
    commands:
      - command: '"'"'touch ../workdir-cmd-ran'"'"'
        working_directory: "subdir"
        expected_exit: 0
      - command: '"'"'true'"'"'
        expected_exit: 0
  - id: green
    title: "trivially green second step"
    commands:
      - command: '"'"'true'"'"'
        expected_exit: 0'

# The tree-binding harness (layer E). `gate.txt` is COMMITTED holding "fail", so the step
# passes only while an uncommitted edit is present — the reviewer'"'"'s frontend-lint shape
# reduced to one grep. .ax-verify is gitignored exactly as in the real repo, so the run'"'"'s
# own audit writes do not perturb the fingerprint.
TREE_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: tree-gated
    title: "passes only while the uncommitted edit is present"
    commands:
      - command: '"'"'grep -q pass gate.txt'"'"'
        expected_exit: 0'

# The malformed-step harness (layers F + G). Each shape is a REQUIRED step that certifies
# nothing happened, beside a genuinely green step so the run has a real executed row.
# `malformed-cmd-ran` is the ground truth: it exists if and only if the malformed step's
# command actually ran. Two shapes (empty commands / non-mapping entry) have no command that
# could create it, which is precisely their defect.
malformed_checklist() {
    case "$1" in
      emptycommands) cat <<'YAML'
version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: required-but-empty
    title: "a required step that declares NO commands"
    commands: []
  - id: green
    title: "trivially green step"
    commands:
      - command: 'true'
        expected_exit: 0
YAML
      ;;
      commentonly) cat <<'YAML'
version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: required-but-inert
    title: "a required step whose command is a comment — bash -c exits 0"
    commands:
      - command: '# temporarily disabled'
        expected_exit: 0
  - id: green
    title: "trivially green step"
    commands:
      - command: 'true'
        expected_exit: 0
YAML
      ;;
      whitespaceonly) cat <<'YAML'
version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: required-but-blank
    title: "a required step whose command is whitespace — bash -c exits 0"
    commands:
      - command: '   '
        expected_exit: 0
  - id: green
    title: "trivially green step"
    commands:
      - command: 'true'
        expected_exit: 0
YAML
      ;;
      blankid) cat <<'YAML'
version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: ""
    title: "a required step with no id — STEP_ORDER word-splits it into zero iterations"
    commands:
      - command: 'touch malformed-cmd-ran'
        expected_exit: 0
  - id: green
    title: "trivially green step"
    commands:
      - command: 'true'
        expected_exit: 0
YAML
      ;;
      notmapping) cat <<'YAML'
version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: required-but-scalar
    title: "a required step whose command entry is a bare string, not a mapping"
    commands:
      - 'touch malformed-cmd-ran'
  - id: green
    title: "trivially green step"
    commands:
      - command: 'true'
        expected_exit: 0
YAML
      ;;
      *) echo "malformed_checklist: unknown shape: $1" >&2; return 2 ;;
    esac
}

WD_ADVISORY_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: advisory-absent
    title: "an ADVISORY command whose directory is absent must stay advisory"
    commands:
      - command: '"'"'touch ../workdir-cmd-ran'"'"'
        working_directory: "subdir"
        expected_exit: 0
        advisory: true
      - command: '"'"'true'"'"'
        expected_exit: 0'

WD_ALL_ADVISORY_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 20
checklist:
  - id: all-advisory
    title: "every command advisory, directory absent — must WARN, not BLOCK"
    commands:
      - command: '"'"'touch ../workdir-cmd-ran'"'"'
        working_directory: "subdir"
        expected_exit: 0
        advisory: true
  - id: green
    title: "trivially green second step"
    commands:
      - command: '"'"'true'"'"'
        expected_exit: 0'

# ── Assertion 1: the laundering harness ──────────────────────────────────────
# Runs the two-step harness twice (plain, then --resume) and decides ONE question: did the
# blocked run launder itself green? Two facts witness the same defect and are perfectly
# correlated by construction, so they form a single detection axis rather than two:
#   • the --resume run claims green (exit 0) — the consequence, and
#   • the blocked run left a record certifying the unobserved step as PASS — the mechanism.
# Returns 0 = LAUNDERED, 1 = held. Sets LW_DESC to a one-line witness.
LW_DESC=""
launders() {
    local vc="$1" tag="$2"
    local sb="$WORK/launder-$tag"
    build_sandbox "$sb" "$vc" "$LAUNDER_CHECKLIST"
    VC_ENV=""
    vc_run "$sb" "$sb/run1.log"; local rc1=$?
    local record="(absent)"
    [ -f "$sb/repo/.ax-verify/last_run.jsonl" ] && \
        record="$(tr -d '\n' < "$sb/repo/.ax-verify/last_run.jsonl")"
    vc_run "$sb" "$sb/run2.log" --resume; local rc2=$?
    local skipped=0
    grep -q 'SKIP (resume)' "$sb/run2.log" 2>/dev/null && skipped=1
    # certified = the BLOCKED run wrote down the step whose command never ran as PASS.
    local certified=0
    printf '%s' "$record" | grep -q '"step_id":"must-fail","status":"PASS"' && certified=1
    LW_DESC="run1=$rc1 run2=$rc2 resume_skip_in_run2=$skipped certified_unobserved_step=$certified record=$record"
    [ "$rc2" -eq 0 ] || [ "$certified" -eq 1 ]
}

# ── The absent-working-directory harness ─────────────────────────────────────
# Mirrors the reviewer's `mv frontend frontend.off` reproduction: run once with the directory
# gone, restore it, then --resume. Returns 0 = LAUNDERED (the resume run claimed green while
# the command never executed), 1 = held. Sets WW_DESC to a one-line witness.
WW_DESC=""
workdir_launders() {
    local vc="$1" tag="$2"
    local sb="$WORK/workdir-$tag"
    build_sandbox "$sb" "$vc" "$WORKDIR_CHECKLIST"
    mkdir -p "$sb/repo/subdir"
    VC_ENV=""
    mv "$sb/repo/subdir" "$sb/repo/subdir.off"
    vc_run "$sb" "$sb/run1.log"; local rc1=$?
    local record="(absent)"
    [ -f "$sb/repo/.ax-verify/last_run.jsonl" ] && \
        record="$(grep -o '"step_id":"mixed","status":"[A-Z]*"' "$sb/repo/.ax-verify/last_run.jsonl" | tail -1)"
    mv "$sb/repo/subdir.off" "$sb/repo/subdir"
    vc_run "$sb" "$sb/run2.log" --resume; local rc2=$?
    local ran=0
    [ -f "$sb/repo/workdir-cmd-ran" ] && ran=1
    WW_DESC="run1=$rc1 run2=$rc2 command_ever_executed=$ran record=${record:-(none)}"
    # Laundered ⟺ a green run was published although the command never executed.
    [ "$rc2" -eq 0 ] && [ "$ran" -eq 0 ]
}

# ── The tree-binding harness (layer E) ───────────────────────────────────────
# Reproduces the reviewer's path exactly, in a throwaway git repo whose COMMITTED state
# FAILS the step: (1) an uncommitted edit makes it pass and a full run publishes the record,
# (2) the edit is reverted — head unchanged, failure back, (3) --resume. Returns 0 =
# LAUNDERED (a green run was published at that head although the step, against THIS tree,
# fails and never ran), 1 = held. Sets TW_DESC to a one-line witness.
# git is a baseline R25 prerequisite; if it is unavailable the harness cannot run at all.
TW_DESC=""
tree_launders() {
    local vc="$1" tag="$2"
    local sb="$WORK/tree-$tag"
    build_sandbox "$sb" "$vc" "$TREE_CHECKLIST"
    printf 'fail\n' > "$sb/repo/gate.txt"
    printf '.ax-verify/\n' > "$sb/repo/.gitignore"
    ( cd "$sb/repo" && git init -q . && git add -A \
        && git -c user.email=ax@example.invalid -c user.name=ax commit -q -m harness ) >/dev/null 2>&1 || {
        echo "resume_provenance_guard: could not create the git harness (git required)" >&2; return 2; }
    VC_ENV=""
    # (1) uncommitted edit ⇒ the step passes; the record is published for this dirty tree.
    printf 'pass\n' > "$sb/repo/gate.txt"
    vc_run "$sb" "$sb/run1.log"; local trc1=$?
    # (2) revert it — head_sha is unchanged, the failure is back.
    ( cd "$sb/repo" && git checkout -q -- gate.txt )
    # (3) --resume: consuming the record here skips a step that this tree fails.
    vc_run "$sb" "$sb/run2.log" --resume; local trc2=$?
    local tskip=0
    grep -q 'SKIP (resume)' "$sb/run2.log" 2>/dev/null && tskip=1
    TW_DESC="run1=$trc1 run2=$trc2 resume_skip_in_run2=$tskip gate=$(tr -d '\n' < "$sb/repo/gate.txt")"
    # Laundered ⟺ the resume run claimed green AND it did so by skipping, i.e. without ever
    # re-running the step against the tree actually in front of it.
    [ "$trc2" -eq 0 ] && [ "$tskip" -eq 1 ]
}

# ── The malformed-step harness (layers F + G) ────────────────────────────────
# Runs ONE malformed shape beside a green step. Returns 0 = LAUNDERED (the run published a
# green verdict although that step ran nothing), 1 = held. Sets EW_DESC to a one-line witness.
# The verdict is a single expression on purpose: one detection, one axis (see the note under
# assertion 5) so fixture_kill_proof [87] can prove each fixture non-vacuous by neutering it.
EW_DESC=""
malformed_step_launders() {
    local vc="$1" tag="$2" shape="$3"
    local sb="$WORK/malformed-$shape-$tag"
    build_sandbox "$sb" "$vc" "$(malformed_checklist "$shape")"
    VC_ENV=""
    vc_run "$sb" "$sb/run.log"; local erc=$?
    local mran=0
    [ -f "$sb/repo/malformed-cmd-ran" ] && mran=1
    EW_DESC="shape=$shape exit=$erc malformed_step_command_ever_ran=$mran"
    [ "$erc" -eq 0 ] && [ "$mran" -eq 0 ]
}

echo "── [resume provenance] laundering harness: step 1 wipes the run's temp dir, step 2 is \`false\`"

VC_SUBJECT="$WORK/vc-subject.sh"
make_vc "$VC_SUBJECT" "$SUBJECT_MODE" || {
    echo "resume_provenance_guard: could not build the subject (neuter-mode=$SUBJECT_MODE)" >&2; exit 2; }
[ "$SUBJECT_MODE" = "none" ] || \
    echo "  MODE: fixture neuter-mode=$SUBJECT_MODE (layers neutered in a copy of the live script)"

launders "$VC_SUBJECT" subject; LAUNDERED=$?
echo "  subject          : $LW_DESC"
if [ "$LAUNDERED" -eq 0 ]; then
    violation "a run that BLOCKED on an incomplete ledger laundered itself green on the next" \
              "--resume: the step whose command never executed was certified PASS and skipped."
fi

# ── Assertion 2: legitimate resume must still work (no over-correction) ──────
SB_CLEAN="$WORK/clean"
build_sandbox "$SB_CLEAN" "$VC_SUBJECT" "$CLEAN_CHECKLIST"
VC_ENV=""
vc_run "$SB_CLEAN" "$SB_CLEAN/run1.log"; CLEAN_RC1=$?
vc_run "$SB_CLEAN" "$SB_CLEAN/run2.log" --resume; CLEAN_RC2=$?
CLEAN_SKIPS=$(grep -c 'SKIP (resume)' "$SB_CLEAN/run2.log" 2>/dev/null || echo 0)
echo "  clean resume     : run1=$CLEAN_RC1 run2=$CLEAN_RC2 resume_skips=$CLEAN_SKIPS"
if [ "$CLEAN_RC1" -ne 0 ] || [ "$CLEAN_RC2" -ne 0 ] || [ "${CLEAN_SKIPS:-0}" -lt 2 ]; then
    violation "a CLEAN run no longer supports --resume (run1=$CLEAN_RC1 run2=$CLEAN_RC2" \
              "skips=$CLEAN_SKIPS, expected 0/0/2). The fix must not disable legitimate resume."
fi

# ── Assertion 3: SHORT_CIRCUITED must not be inheritable ─────────────────────
# AX_FAKE_LEDGER_LOSS=partial drops an earlier step's rows. The accounting check catches
# that ONLY when SHORT_CIRCUITED is 0. If the variable is honoured from the environment,
# exporting SHORT_CIRCUITED=1 suppresses the check and the run degrades to a PASS.
SB_ENV="$WORK/envbypass"
build_sandbox "$SB_ENV" "$VC_SUBJECT" "$CLEAN_CHECKLIST"
VC_ENV="AX_FAKE_LEDGER_LOSS=partial SHORT_CIRCUITED=1"
vc_run "$SB_ENV" "$SB_ENV/run.log"; ENV_RC=$?
VC_ENV=""
echo "  env bypass       : SHORT_CIRCUITED=1 + AX_FAKE_LEDGER_LOSS=partial → exit $ENV_RC (want 2)"
if [ "$ENV_RC" -ne 2 ]; then
    violation "an exported SHORT_CIRCUITED=1 suppressed the accounting check (exit $ENV_RC," \
              "expected 2). The flag gates a fail-closed gate and must be initialised internally."
fi

# ── Assertion 4: an absent working directory may not become resumable PASS evidence ──
echo "── [workdir provenance] harness: a non-advisory command whose directory was moved away"
workdir_launders "$VC_SUBJECT" subject; WD_LAUNDERED=$?
echo "  subject          : $WW_DESC"
if [ "$WD_LAUNDERED" -eq 0 ]; then
    violation "a command whose working directory was ABSENT was recorded as passing evidence:" \
              "the run claimed green and a later --resume skipped the step, so the command never" \
              "executed at all. A missing directory means unverified, not verified-OK."
fi

# ── Assertion 5: advisory commands must STAY advisory ────────────────────────
# The fix must block on absent directories without promoting a knowingly-advisory item into
# a blocker. Two shapes: an advisory command beside a required one (run must stay green), and
# a step whose commands are ALL advisory (must WARN, never BLOCK the whole checklist).
SB_ADV="$WORK/workdir-advisory"
build_sandbox "$SB_ADV" "$VC_SUBJECT" "$WD_ADVISORY_CHECKLIST"
VC_ENV=""
vc_run "$SB_ADV" "$SB_ADV/run.log"; ADV_RC=$?
ADV_WARN=$(grep -c 'WARN' "$SB_ADV/run.log" 2>/dev/null || echo 0)
echo "  advisory absent  : exit $ADV_RC (want 0) warn_lines=$ADV_WARN (want >0)"
if [ "$ADV_RC" -ne 0 ] || [ "${ADV_WARN:-0}" -lt 1 ]; then
    violation "an ADVISORY command with an absent working directory changed the run's verdict" \
              "(exit $ADV_RC, warn lines $ADV_WARN). Advisory items must warn, never block."
fi

SB_ALLADV="$WORK/workdir-all-advisory"
build_sandbox "$SB_ALLADV" "$VC_SUBJECT" "$WD_ALL_ADVISORY_CHECKLIST"
VC_ENV=""
vc_run "$SB_ALLADV" "$SB_ALLADV/run.log"; ALLADV_RC=$?
vc_run "$SB_ALLADV" "$SB_ALLADV/run2.log" --resume; ALLADV_RC2=$?
ALLADV_SKIPPED=0
grep -q 'all-advisory' "$SB_ALLADV/run2.log" && \
    grep -A1 'all-advisory' "$SB_ALLADV/run2.log" | grep -q 'SKIP (resume)' && ALLADV_SKIPPED=1
echo "  all-advisory step: exit $ALLADV_RC (want 0, not a BLOCK) resume_skipped=$ALLADV_SKIPPED (want 0)"
if [ "$ALLADV_RC" -ne 0 ]; then
    violation "a step whose commands are ALL advisory BLOCKED the run (exit $ALLADV_RC) because" \
              "its directory was absent. That is over-blocking: advisory items cannot fail a run."
fi
# resume_skipped is REPORTED, not asserted — deliberately. An all-advisory step that never
# executed becomes publishable only if the UNRUN verdict itself is broken, i.e. exactly the
# layer (A) defect assertion 1 already detects through its own harness. Asserting it here too
# would give the `ab` fixture a SECOND detection axis, and fixture_kill_proof_guard [87] would
# then correctly report that fixture as vacuous w.r.t. its registered anchor — the fixture
# would still go red with assertion 1's logic neutered. One detection, one axis.

# ── Assertion 6: a resume record may not outlive the tree that produced it ───
echo "── [tree binding] harness: same head_sha, but the uncommitted change that made the step pass is reverted"
tree_launders "$VC_SUBJECT" subject; TREE_LAUNDERED=$?
if [ "$TREE_LAUNDERED" -eq 2 ]; then
    echo "resume_provenance_guard: git harness unavailable" >&2; exit 2
fi
echo "  subject          : $TW_DESC"
if [ "$TREE_LAUNDERED" -eq 0 ]; then
    violation "a resume record produced by a DIFFERENT working tree at the same head_sha was" \
              "consumed: the step was skipped although the tree in front of the run fails it." \
              "head_sha does not identify the code that ran — a dirty tree makes one head cover many."
fi

# ── Assertion 7: tree binding must not break resume on an UNCHANGED tree ─────
# Over-correction check. Same git sandbox shape, but nothing is touched between the runs:
# the second run must still resume-skip. (The dirty-but-identical case is the common one —
# R25 is normally run on a work-in-progress tree — so it is what is asserted here.)
SB_TREE_STABLE="$WORK/tree-stable"
build_sandbox "$SB_TREE_STABLE" "$VC_SUBJECT" "$TREE_CHECKLIST"
printf 'pass\n' > "$SB_TREE_STABLE/repo/gate.txt"
printf '.ax-verify/\n' > "$SB_TREE_STABLE/repo/.gitignore"
( cd "$SB_TREE_STABLE/repo" && git init -q . && git add -A \
    && git -c user.email=ax@example.invalid -c user.name=ax commit -q -m harness ) >/dev/null 2>&1
printf 'pass\n# work in progress\n' > "$SB_TREE_STABLE/repo/gate.txt"   # dirty, and left dirty
VC_ENV=""
vc_run "$SB_TREE_STABLE" "$SB_TREE_STABLE/run1.log"; TS_RC1=$?
vc_run "$SB_TREE_STABLE" "$SB_TREE_STABLE/run2.log" --resume; TS_RC2=$?
TS_SKIPS=$(grep -c 'SKIP (resume)' "$SB_TREE_STABLE/run2.log" 2>/dev/null || echo 0)
echo "  unchanged tree   : run1=$TS_RC1 run2=$TS_RC2 resume_skips=$TS_SKIPS (want 0/0/1)"
if [ "$TS_RC1" -ne 0 ] || [ "$TS_RC2" -ne 0 ] || [ "${TS_SKIPS:-0}" -lt 1 ]; then
    violation "resume stopped working on an UNCHANGED (dirty) working tree (run1=$TS_RC1" \
              "run2=$TS_RC2 skips=$TS_SKIPS, expected 0/0/1). The tree binding must refuse a" \
              "record from a DIFFERENT tree, not refuse every record."
fi

# ── Assertion 8: a malformed step is a BLOCK, never a certification ──────────
# Four shapes, each of which used to publish green with the step having run nothing. The
# fifth (a non-mapping command entry) is asserted too: it has no `command:` key at all.
echo "── [malformed step] harness: a required step that cannot be run, or cannot be identified"
MALFORMED_SHAPES="emptycommands commentonly whitespaceonly blankid notmapping"
for shape in $MALFORMED_SHAPES; do
    malformed_step_launders "$VC_SUBJECT" subject "$shape"; SHAPE_LAUNDERED=$?
    echo "  subject          : $EW_DESC"
    if [ "$SHAPE_LAUNDERED" -eq 0 ]; then
        violation "malformed step shape '$shape' published a GREEN verdict although the step" \
                  "ran nothing. A step that emits no command row is absent from STEP_ORDER and" \
                  "therefore from every accounting check; a step whose command text is inert" \
                  "records an EXECUTED PASS for a shell that did nothing; a step with no usable" \
                  "id gets zero iterations. None of them verified the contract."
    fi
done

# ── Assertion 9: the malformed-step BLOCK must respect --step selection ──────
# Only steps SELECTED for the run are held to the contract; a partial run that never selected
# the malformed step must still work, or the fix would break every backend-only invocation.
SB_EMPTY_SEL="$WORK/malformed-selection"
build_sandbox "$SB_EMPTY_SEL" "$VC_SUBJECT" "$(malformed_checklist emptycommands)"
VC_ENV=""
vc_run "$SB_EMPTY_SEL" "$SB_EMPTY_SEL/run.log" --step green; SEL_RC=$?
echo "  --step green     : exit $SEL_RC (want 0 — the empty step was not selected)"
if [ "$SEL_RC" -ne 0 ]; then
    violation "--step selection of a non-empty step was BLOCKED by an empty step elsewhere in" \
              "the checklist (exit $SEL_RC). The non-empty requirement applies to the RESOLVED" \
              "step set only; over-blocking here would break every partial run."
fi

# ── Assertion 10 (non-vacuity + layer matrix) — LIVE mode only ───────────────
# A fixture subject IS already neutered, so re-neutering it would prove nothing.
if [ -z "$FIXTURE_ROOT" ]; then
    echo "── [mutation matrix] each layer neutered in a copy of the real script"
    for mode in ab a b; do
        VC_MUT="$WORK/vc-$mode.sh"
        if ! make_vc "$VC_MUT" "$mode"; then
            violation "could not neuter layer(s) '$mode' — the anchor no longer appears exactly" \
                      "once in verify-completion.sh, so this guard's mutation proof is stale."
            continue
        fi
        launders "$VC_MUT" "$mode"; MUT_LAUNDERED=$?; MW="$LW_DESC"
        case "$mode" in
            ab) want_launder=0; label="A+B neutered (must REPRODUCE the defect)" ;;
            a)  want_launder=1; label="A neutered   (B alone must still block)" ;;
            b)  want_launder=1; label="B neutered   (A alone must still block)" ;;
        esac
        echo "  $label: $MW"
        if [ "$want_launder" -eq 0 ] && [ "$MUT_LAUNDERED" -ne 0 ]; then
            violation "with BOTH layers neutered the harness did NOT reproduce the laundering." \
                      "The assertions above therefore prove nothing — they would pass on the" \
                      "unfixed script too. Fix the harness, not the guard."
        fi
        if [ "$want_launder" -eq 1 ] && [ "$MUT_LAUNDERED" -eq 0 ]; then
            violation "with layer '$mode' neutered the blocked run laundered itself green —" \
                      "the remaining layer does not hold the invariant on its own."
        fi
    done

    echo "── [mutation matrix] absent-working-directory layers (C = block, D = required coverage)"
    for mode in cd c d; do
        VC_MUT="$WORK/vc-$mode.sh"
        if ! make_vc "$VC_MUT" "$mode"; then
            violation "could not neuter layer(s) '$mode' — the anchor no longer appears exactly" \
                      "once in verify-completion.sh, so this guard's mutation proof is stale."
            continue
        fi
        workdir_launders "$VC_MUT" "$mode"; MUT_LAUNDERED=$?; MW="$WW_DESC"
        case "$mode" in
            cd) want_launder=0; label="C+D neutered (must REPRODUCE the defect)" ;;
            c)  want_launder=1; label="C neutered   (D alone must still block)" ;;
            d)  want_launder=1; label="D neutered   (C alone must still block)" ;;
        esac
        echo "  $label: $MW"
        if [ "$want_launder" -eq 0 ] && [ "$MUT_LAUNDERED" -ne 0 ]; then
            violation "with BOTH absent-directory layers neutered the harness did NOT reproduce" \
                      "the laundering. The assertions above therefore prove nothing — they would" \
                      "pass on the unfixed script too. Fix the harness, not the guard."
        fi
        if [ "$want_launder" -eq 1 ] && [ "$MUT_LAUNDERED" -eq 0 ]; then
            violation "with layer '$mode' neutered a command that never ran was still certified —" \
                      "the remaining layer does not hold the invariant on its own."
        fi
    done

    # Layer (E) is a single load-bearing check, so its matrix is one row: neutered ⇒ the
    # harness must REPRODUCE the defect, otherwise assertion 6 would pass on the unfixed
    # script and prove nothing.
    echo "── [mutation matrix] tree binding (E)"
    VC_MUT_E="$WORK/vc-e.sh"
    if make_vc "$VC_MUT_E" "e"; then
        tree_launders "$VC_MUT_E" e; E_LAUNDERED=$?
        echo "  E neutered   (must REPRODUCE the defect): $TW_DESC"
        if [ "$E_LAUNDERED" -ne 0 ]; then
            violation "with the tree-fingerprint admission check neutered the harness did NOT" \
                      "reproduce the stale-tree resume. Assertion 6 therefore proves nothing —" \
                      "it would pass on the unfixed script too. Fix the harness, not the guard."
        fi
    else
        violation "could not neuter layer 'e' — the anchor no longer appears exactly once in" \
                  "verify-completion.sh, so this guard's mutation proof is stale."
    fi

    # (F) parse-time contract vs (G) structural declared ⊆ emitted. Their coverage differs by
    # shape, and the matrix asserts exactly that rather than a flat "both are needed":
    #   fg  — every shape must REPRODUCE (else assertion 8 proves nothing)
    #   f   — only the INERT-COMMAND shapes may reproduce; the VANISHING shapes must still be
    #         blocked, which is what makes (G) load-bearing rather than decorative
    #   g   — nothing may reproduce: (F) rejects all four earlier, at parse time
    # `notmapping` is deliberately absent from the matrix: with (F) neutered the emitter hits
    # the missing `command:` key and the run dies as a parse error, so that shape blocks either
    # way and cannot witness a layer. It is asserted (assertion 8), never used as proof.
    echo "── [mutation matrix] malformed-step contract: F = parse-time, G = declared ⊆ emitted"
    for mode in fg f g; do
        VC_MUT="$WORK/vc-$mode.sh"
        if ! make_vc "$VC_MUT" "$mode"; then
            violation "could not neuter layer(s) '$mode' — the anchor no longer appears exactly" \
                      "once in verify-completion.sh, so this guard's mutation proof is stale."
            continue
        fi
        for shape in emptycommands commentonly whitespaceonly blankid; do
            case "$mode:$shape" in
                fg:*)                          want_launder=0 ;;
                f:commentonly|f:whitespaceonly) want_launder=0 ;;
                *)                             want_launder=1 ;;
            esac
            malformed_step_launders "$VC_MUT" "$mode" "$shape"; MUT_LAUNDERED=$?
            if [ "$want_launder" -eq 0 ]; then
                echo "  $mode neutered (must REPRODUCE): $EW_DESC"
            else
                echo "  $mode neutered (must still BLOCK): $EW_DESC"
            fi
            if [ "$want_launder" -eq 0 ] && [ "$MUT_LAUNDERED" -ne 0 ]; then
                violation "with layer(s) '$mode' neutered the '$shape' shape did NOT reproduce" \
                          "the defect. Assertion 8 therefore proves nothing for it — it would" \
                          "pass on the unfixed script too. Fix the harness, not the guard."
            fi
            if [ "$want_launder" -eq 1 ] && [ "$MUT_LAUNDERED" -eq 0 ]; then
                violation "with layer(s) '$mode' neutered the '$shape' shape laundered a green" \
                          "verdict — the remaining layer does not hold the contract on its own."
            fi
        done
    done
fi

echo ""
if [ "$VIOLATIONS" -gt 0 ]; then
    echo "resume_provenance_guard: FAIL — $VIOLATIONS violation(s): a blocked R25 run can seed a false-green resume" >&2
    exit 1
fi
echo "resume_provenance_guard: PASS — an unobserved step is never certified, a command whose working directory is absent blocks instead of skipping, every required command must actually execute before a step is publishable, a resume record is consumable only by the tree that produced it, a malformed step (no commands / inert command text / no usable id / duplicate id / non-mapping entry) BLOCKS the parse and every declared step must have emitted a plan row, a blocked run publishes no resume record, SHORT_CIRCUITED is not inheritable, advisory stays advisory, and legitimate resume still works"
exit 0
