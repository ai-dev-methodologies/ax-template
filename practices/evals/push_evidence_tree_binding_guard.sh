#!/usr/bin/env bash
# practices/evals/push_evidence_tree_binding_guard.sh — push evidence must come from the
# CLEAN tree of the pushed commit [97]
#
# THE INVARIANT (binary, behavioral): a push may proceed only when the R25 audit line it
# rides on was produced by a run over the COMMITTED tree of the sha being pushed. Evidence
# gathered from a working tree that differs from that commit certifies code the receiver
# will never get.
#
# WHY (cross-family review P1, 2026-07-29 — the consequential half of the tree-binding defect):
#   `--resume` was bound to the tree that produced its record (resume_provenance_guard layer
#   E), but the PUSH gate was still bound to head_sha alone — and head_sha does not identify
#   code. R25 is routinely invoked on a dirty tree, so one head covers arbitrarily many trees.
#   The reviewer's reproduction needs NO --resume at all:
#     1. committed HEAD H fails a step (frontend lint, in the real repo)
#     2. an UNCOMMITTED fix makes a full R25 run pass → {head_sha:H, exit:0, full_run:true}
#     3. the fix is reverted/stashed — nothing re-runs, the audit line is untouched
#     4. `git push` H
#     5. .githooks/pre-push consults completion_checklist_recency_guard.sh, which checked only
#        {head_sha, exit, full_run} — all three still green — and the push landed.
#   The pushed tree was never verified; the verified tree was never pushed.
#
# THE FIX, IN TWO SERIAL LINKS (this is not defense in depth — each link is load-bearing on
# its own, and the matrix below asserts exactly that by requiring EACH neuter to reproduce):
#   (P) PRODUCER — verify-completion.sh records `tree_clean` (and `tree_fingerprint`) in the
#       audit line, honestly: true only when `git status --porcelain -uall` is empty at a
#       resolvable HEAD. A producer that always claims "clean" defeats any consumer.
#   (C) CONSUMER — completion_checklist_recency_guard.sh refuses a line whose tree_clean is
#       not exactly True. A consumer that ignores the field defeats any producer.
#
# SCOPE (deliberate, and asserted as an anti-over-correction axis): only PUSH eligibility
# tightens. A dirty-tree run remains a full-value local run and remains usable for `--resume`
# (verify-completion binds those by fingerprint, not by cleanliness), so the iteration loop is
# unchanged. What changes: shipping requires re-running the contract once the work is
# committed. Assertion 3 pins that the local loop was not broken.
#
# HONEST LIMIT: git-ignored paths (node_modules/, build/) are outside git's model and cannot
# be pinned by any of this. "Clean" means identical to the commit in every path git tracks or
# would track — the residual is that an R25 run depends on ignored artifacts nobody ships.
#
# HOW IT IS MEASURED (end to end, never by inspection):
#   A throwaway git repo with a bare "remote", the REAL .githooks/pre-push + pre-push-lib.sh
#   wired via core.hooksPath, the subject verify-completion.sh, and the subject recency guard.
#   The single checklist step passes only while `gate.txt` says "pass" — the reviewer's
#   frontend-lint shape reduced to one grep. Ground truth is the REMOTE: laundering means the
#   bare repo's branch actually advanced to the unverified commit.
#   Measured on the pre-fix pair: R25 exit 0 on the dirty tree, `git push` exit 0, remote tip
#   == the commit whose committed state fails the step. Post-fix: push exit 1,
#   AUDIT_DIRTY_TREE_EVIDENCE, remote tip unchanged.
#
# Usage:
#   bash practices/evals/push_evidence_tree_binding_guard.sh
#       LIVE: assert the real pair, then the mutation matrix. Exit 0.
#   bash practices/evals/push_evidence_tree_binding_guard.sh --fixture-root DIR
#       Run the SAME assertions against copies neutered per DIR's `neuter-mode` file
#       (none|p|c|pc), skipping the matrix. The committed fail_* fixtures must exit 1 —
#       that is what proves these assertions detect the regression rather than passing
#       vacuously. A fixture carries a MODE, not a frozen copy of the scripts, so it cannot
#       rot into testing a stale artifact.
#
# Exit: 0 = push evidence is tree-bound · 1 = a stale/dirty-tree audit can ship a commit that
#       was never verified (BLOCK) · 2 = setup/tooling error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$REPO_ROOT/.." && pwd)"

# Find the repo even when this script is executed from a copy outside the tree — which is
# exactly what fixture_kill_proof_guard.sh [87] does when it runs a neutered mutant from tmp.
if [ ! -f "$REPO_ROOT/practices/scripts/verify-completion.sh" ]; then
    _d="$(pwd -P)"
    while [ "$_d" != "/" ]; do
        if [ -f "$_d/practices/scripts/verify-completion.sh" ]; then REPO_ROOT="$_d"; break; fi
        _d="$(dirname "$_d")"
    done
fi

REAL_VC="$REPO_ROOT/practices/scripts/verify-completion.sh"
REAL_RECENCY="$REPO_ROOT/practices/evals/completion_checklist_recency_guard.sh"
REAL_HOOK="$REPO_ROOT/.githooks/pre-push"
REAL_HOOK_LIB="$REPO_ROOT/.githooks/pre-push-lib.sh"

FIXTURE_ROOT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --fixture-root)   FIXTURE_ROOT="$2"; shift 2 ;;
        --fixture-root=*) FIXTURE_ROOT="${1#--fixture-root=}"; shift ;;
        *) echo "push_evidence_tree_binding_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SUBJECT_MODE="none"
if [ -n "$FIXTURE_ROOT" ]; then
    [ -d "$FIXTURE_ROOT" ] || {
        echo "push_evidence_tree_binding_guard: fixture root not found: $FIXTURE_ROOT" >&2; exit 2; }
    [ -f "$FIXTURE_ROOT/neuter-mode" ] || {
        echo "push_evidence_tree_binding_guard: fixture is missing its neuter-mode file" >&2; exit 2; }
    SUBJECT_MODE="$(tr -d ' \t\n' < "$FIXTURE_ROOT/neuter-mode")"
    case "$SUBJECT_MODE" in
        none|p|c|pc) ;;
        *) echo "push_evidence_tree_binding_guard: unknown neuter-mode '$SUBJECT_MODE'" >&2; exit 2 ;;
    esac
fi

for f in "$REAL_VC" "$REAL_RECENCY" "$REAL_HOOK" "$REAL_HOOK_LIB"; do
    [ -f "$f" ] || { echo "push_evidence_tree_binding_guard: missing $f" >&2; exit 2; }
done
command -v python3 >/dev/null 2>&1 || { echo "push_evidence_tree_binding_guard: python3 required" >&2; exit 2; }
command -v git >/dev/null 2>&1 || { echo "push_evidence_tree_binding_guard: git required" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

VIOLATIONS=0
violation() { echo "  VIOLATION: $*" >&2; VIOLATIONS=$((VIOLATIONS + 1)); }

GIT_ID=(-c user.email=ax@example.invalid -c user.name=ax)

# ── Neuter helpers ───────────────────────────────────────────────────────────
# (P) the producer stops telling the truth about the tree it ran on: every run claims the
#     committed tree. (C) the consumer stops requiring it.
NEUTER_P_ANCHOR='CURRENT_TREE_CLEAN=false'
NEUTER_P_VALUE='CURRENT_TREE_CLEAN=true'
NEUTER_C_ANCHOR='latest.get("tree_clean") is not True'
NEUTER_C_VALUE='False'

# neuter_copy <src> <dest> <anchor> <value> <apply:0|1>
neuter_copy() {
    python3 - "$1" "$2" "$3" "$4" "$5" <<'PY'
import sys
src, dest, anchor, value, apply_it = sys.argv[1:6]
text = open(src, encoding="utf-8").read()
if apply_it == "1":
    n = text.count(anchor)
    if n != 1:
        print(f"anchor occurs {n}x (expected exactly 1) in {src}: {anchor!r}", file=sys.stderr)
        sys.exit(3)
    text = text.replace(anchor, value)
open(dest, "w", encoding="utf-8").write(text)
PY
}

# make_subject <destdir> <mode> — materialise the (possibly neutered) producer + consumer.
make_subject() {
    local dest="$1" mode="$2"
    mkdir -p "$dest"
    local want_p=0 want_c=0
    case "$mode" in
        p) want_p=1 ;;
        c) want_c=1 ;;
        pc) want_p=1; want_c=1 ;;
    esac
    neuter_copy "$REAL_VC" "$dest/verify-completion.sh" \
        "$NEUTER_P_ANCHOR" "$NEUTER_P_VALUE" "$want_p" || return 3
    neuter_copy "$REAL_RECENCY" "$dest/completion_checklist_recency_guard.sh" \
        "$NEUTER_C_ANCHOR" "$NEUTER_C_VALUE" "$want_c" || return 3
    return 0
}

# The checklist: one step that passes only while the working copy of gate.txt says "pass".
CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 60
checklist:
  - id: tree-gated
    title: "passes only while the working tree says pass"
    commands:
      - command: '"'"'grep -q pass gate.txt'"'"'
        expected_exit: 0'

# ── Sandbox builder ──────────────────────────────────────────────────────────
# $1 = sandbox dir, $2 = subject dir (verify-completion.sh + recency guard).
# Leaves: $sb/repo at commit A (gate.txt=pass) already pushed to $sb/remote.git, hooks
# installed via core.hooksPath but NOT yet consulted (commit A was pushed before that).
build_repo() {
    local sb="$1" subject="$2"
    mkdir -p "$sb/repo/practices/scripts" "$sb/repo/practices/evals" "$sb/repo/.githooks"
    cp "$subject/verify-completion.sh" "$sb/repo/practices/scripts/"
    cp "$subject/completion_checklist_recency_guard.sh" "$sb/repo/practices/evals/"
    [ -f "$REPO_ROOT/practices/scripts/_collapse_plan.py" ] && \
        cp "$REPO_ROOT/practices/scripts/_collapse_plan.py" "$sb/repo/practices/scripts/"
    cp "$REAL_HOOK" "$REAL_HOOK_LIB" "$sb/repo/.githooks/"
    chmod +x "$sb/repo/.githooks/pre-push"
    printf '%s\n' "$CHECKLIST" > "$sb/repo/practices/verification-checklist.yaml"
    printf 'pass\n' > "$sb/repo/gate.txt"
    printf '.ax-verify/\n' > "$sb/repo/.gitignore"
    git init -q --bare "$sb/remote.git" >/dev/null 2>&1 || return 2
    (
        cd "$sb/repo" || exit 2
        git init -q . >/dev/null 2>&1 || exit 2
        git symbolic-ref HEAD refs/heads/main
        git add -A
        git "${GIT_ID[@]}" commit -q -m "scaffold"
        git remote add origin "$sb/remote.git"
        git push -q origin main            # before hooks are consulted: no audit exists yet
        git config core.hooksPath .githooks
    ) >/dev/null 2>&1 || return 2
    return 0
}

# vc_run <sandbox> <log> [args...] — run the sandbox's own verify-completion.sh.
vc_run() {
    local sb="$1" log="$2"; shift 2
    ( cd "$sb/repo" && bash practices/scripts/verify-completion.sh "$@" ) > "$log" 2>&1
    return $?
}

# ── The stale-evidence push harness ──────────────────────────────────────────
# The reviewer's five steps, end to end, with the real hook wired. Returns 0 = LAUNDERED
# (the remote actually advanced to a commit whose committed state fails the checklist),
# 1 = held, 2 = harness setup failure. Sets PW_DESC to a one-line witness.
PW_DESC=""
push_launders() {
    local subject="$1" tag="$2"
    local sb="$WORK/push-$tag"
    build_repo "$sb" "$subject" || { PW_DESC="harness setup failed"; return 2; }

    # (1) commit B — its COMMITTED state fails the step.
    printf 'fail\n' > "$sb/repo/gate.txt"
    ( cd "$sb/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "breaks the gate" ) >/dev/null 2>&1
    local b_sha
    b_sha="$(git -C "$sb/repo" rev-parse HEAD)"

    # (2) an UNCOMMITTED fix makes the full run pass — the audit line is written for B.
    printf 'pass\n' > "$sb/repo/gate.txt"
    vc_run "$sb" "$sb/r25-dirty.log"; local rc_dirty=$?

    # (3) the fix is reverted. Head is still B, the failure is back, nothing re-runs.
    ( cd "$sb/repo" && git checkout -q -- gate.txt )

    # (4) push B.
    ( cd "$sb/repo" && git push origin main ) > "$sb/push.log" 2>&1
    local push_rc=$?

    # (5) ground truth: did the REMOTE advance to the unverified commit?
    local remote_tip
    remote_tip="$(git -C "$sb/remote.git" rev-parse refs/heads/main 2>/dev/null || echo none)"
    local landed=0
    [ "$remote_tip" = "$b_sha" ] && landed=1
    local audit
    audit="$(tail -1 "$sb/repo/.ax-verify/runs.jsonl" 2>/dev/null || echo '(absent)')"
    PW_DESC="r25_on_dirty_tree=$rc_dirty push=$push_rc unverified_commit_landed=$landed audit=$audit"
    [ "$landed" -eq 1 ]
}

echo "── [push evidence] harness: R25 passes only because of an uncommitted fix, which is then reverted"

SUBJECT="$WORK/subject"
if ! make_subject "$SUBJECT" "$SUBJECT_MODE"; then
    echo "push_evidence_tree_binding_guard: could not build the subject (neuter-mode=$SUBJECT_MODE)" >&2
    echo "  an anchor no longer appears exactly once — this guard's mutation proof is stale." >&2
    exit 2
fi
[ "$SUBJECT_MODE" = "none" ] || \
    echo "  MODE: fixture neuter-mode=$SUBJECT_MODE (layers neutered in copies of the live scripts)"

push_launders "$SUBJECT" subject; LAUNDERED=$?
if [ "$LAUNDERED" -eq 2 ]; then
    echo "push_evidence_tree_binding_guard: harness setup failed" >&2; exit 2
fi
echo "  subject          : $PW_DESC"
if [ "$LAUNDERED" -eq 0 ]; then
    violation "a commit whose committed state FAILS the contract was pushed on an audit line" \
              "produced by a dirty tree. head_sha does not identify code: the tree that passed" \
              "was never pushed, and the tree that was pushed was never verified."
fi

# ── Assertion 2: a legitimate clean-tree run must still ship ─────────────────
# Over-correction check. Same shape, but the fix is COMMITTED before R25 runs, which is the
# whole workflow this gate asks for. The push must succeed.
SB_OK="$WORK/push-legit"
if ! build_repo "$SB_OK" "$SUBJECT"; then
    echo "push_evidence_tree_binding_guard: harness setup failed (legit case)" >&2; exit 2
fi
printf 'pass\n# committed work in progress\n' > "$SB_OK/repo/gate.txt"
( cd "$SB_OK/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "real work, committed" ) >/dev/null 2>&1
OK_SHA="$(git -C "$SB_OK/repo" rev-parse HEAD)"
vc_run "$SB_OK" "$SB_OK/r25-clean.log"; OK_R25=$?
( cd "$SB_OK/repo" && git push origin main ) > "$SB_OK/push.log" 2>&1
OK_PUSH=$?
OK_TIP="$(git -C "$SB_OK/remote.git" rev-parse refs/heads/main 2>/dev/null || echo none)"
OK_LANDED=0; [ "$OK_TIP" = "$OK_SHA" ] && OK_LANDED=1
echo "  clean-tree push  : r25=$OK_R25 push=$OK_PUSH landed=$OK_LANDED (want 0/0/1)"
if [ "$OK_R25" -ne 0 ] || [ "$OK_PUSH" -ne 0 ] || [ "$OK_LANDED" -ne 1 ]; then
    violation "a COMMITTED, verified change could not be pushed (r25=$OK_R25 push=$OK_PUSH" \
              "landed=$OK_LANDED). The gate must refuse evidence from a DIFFERENT tree, not" \
              "refuse every push. Hook output:" "$(tail -5 "$SB_OK/push.log" 2>/dev/null | tr '\n' ' ')"
fi

# ── Assertion 3: the LOCAL loop must be untouched ────────────────────────────
# A dirty-tree run stays a full-value run and stays resumable — the tightening applies to
# push eligibility only. If this breaks, the fix has cost the workflow R25 exists to serve.
SB_LOCAL="$WORK/local-loop"
if ! build_repo "$SB_LOCAL" "$SUBJECT"; then
    echo "push_evidence_tree_binding_guard: harness setup failed (local case)" >&2; exit 2
fi
printf 'pass\n# uncommitted work in progress\n' > "$SB_LOCAL/repo/gate.txt"   # dirty, left dirty
vc_run "$SB_LOCAL" "$SB_LOCAL/run1.log"; LOC_RC1=$?
vc_run "$SB_LOCAL" "$SB_LOCAL/run2.log" --resume; LOC_RC2=$?
LOC_SKIPS=$(grep -c 'SKIP (resume)' "$SB_LOCAL/run2.log" 2>/dev/null || echo 0)
echo "  dirty local loop : run1=$LOC_RC1 run2(--resume)=$LOC_RC2 resume_skips=$LOC_SKIPS (want 0/0/1)"
if [ "$LOC_RC1" -ne 0 ] || [ "$LOC_RC2" -ne 0 ] || [ "${LOC_SKIPS:-0}" -lt 1 ]; then
    violation "a dirty-tree run stopped being a usable local run (run1=$LOC_RC1 run2=$LOC_RC2" \
              "skips=$LOC_SKIPS, expected 0/0/1). Only PUSH eligibility may require a clean tree;" \
              "R25 is designed to be run on work in progress."
fi

# ── Assertion 4: the hook must verify the PUSHED sha, not the checkout's HEAD ──
# The per-ref binding is what makes the audit line refer to the commit being shipped; without
# it the harness above would be testing the wrong sha. Structural, because a behavioral probe
# for it already exists (pre_push_decision_guard).
if ! grep -q -- '--expect-sha "\$local_sha"' "$REAL_HOOK"; then
    violation ".githooks/pre-push no longer passes --expect-sha \"\$local_sha\" to the recency" \
              "guard, so a push of a non-checked-out branch would be audited against the" \
              "checkout's HEAD — the tree binding would be attached to the wrong commit."
fi

# ── Assertion 5 (non-vacuity matrix) — LIVE mode only ────────────────────────
# (P) and (C) are SERIAL links in one chain, not redundant layers: a dishonest producer
# defeats an honest consumer and vice-versa. So the honest matrix is "each neuter alone must
# REPRODUCE" — asserting that either one alone still blocks would be false, and claiming it
# would be the exact "broad false guarantee" this catalog exists to prevent.
if [ -z "$FIXTURE_ROOT" ]; then
    echo "── [mutation matrix] producer honesty (P) and consumer enforcement (C) — serial links"
    for mode in pc p c; do
        MUT="$WORK/mut-$mode"
        if ! make_subject "$MUT" "$mode"; then
            violation "could not neuter '$mode' — an anchor no longer appears exactly once in" \
                      "verify-completion.sh / completion_checklist_recency_guard.sh, so this" \
                      "guard's mutation proof is stale."
            continue
        fi
        push_launders "$MUT" "$mode"; MUT_LAUNDERED=$?
        case "$mode" in
            pc) label="P+C neutered (must REPRODUCE)" ;;
            p)  label="P neutered   (must REPRODUCE — a lying producer defeats the consumer)" ;;
            c)  label="C neutered   (must REPRODUCE — an indifferent consumer defeats the producer)" ;;
        esac
        echo "  $label: $PW_DESC"
        if [ "$MUT_LAUNDERED" -eq 2 ]; then
            violation "the '$mode' harness failed to set up, so it proves nothing."
        elif [ "$MUT_LAUNDERED" -ne 0 ]; then
            violation "with '$mode' neutered the harness did NOT reproduce the stale-evidence" \
                      "push. The assertions above therefore prove nothing — they would pass on" \
                      "the unfixed scripts too. Fix the harness, not the guard."
        fi
    done
fi

echo ""
if [ "$VIOLATIONS" -gt 0 ]; then
    echo "push_evidence_tree_binding_guard: FAIL — $VIOLATIONS violation(s): a commit can be pushed on evidence from a tree it does not contain" >&2
    exit 1
fi
echo "push_evidence_tree_binding_guard: PASS — push evidence must come from the CLEAN tree of the pushed commit (producer records tree_clean honestly, the recency guard requires it, the hook audits the pushed sha), while dirty-tree runs remain fully usable locally and for --resume"
exit 0
