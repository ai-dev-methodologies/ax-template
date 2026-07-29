#!/usr/bin/env bash
# practices/evals/midrun_tree_mutation_guard.sh — push evidence must describe a SETTLED tree,
# not two endpoints of a long window [98]
#
# THE INVARIANT (binary, behavioral): a push may proceed only when the R25 audit line it rides
# on was produced by a run whose working tree did not change WHILE it ran. Evidence assembled
# from a moving tree describes no single body of code.
#
# WHY (cross-family review P1, 2026-07-29 — the TOCTOU half of the tree-binding defect):
#   [97] bound push evidence to the CLEAN tree of the pushed commit, but it read one snapshot:
#   head, fingerprint and cleanliness were captured BEFORE the first step and the audit line
#   was appended AFTER the last — a window measured at 2,225 seconds on a real full run. So:
#     1. start clean at commit H, whose committed state FAILS a later step
#     2. while an early long step runs, make the uncommitted fix that step needs
#     3. the later step passes — on an edit no commit contains
#     4. revert it before the run ends; both endpoints are pristine and identical
#     5. `git push H` — every recorded value says "clean tree at H", and the push lands
#   Endpoint evidence cannot see this by construction: the opening and closing snapshots are
#   byte-identical. Only samples taken ACROSS the run witness it.
#
# THE FIX, IN TWO SERIAL LINKS (each load-bearing alone — the matrix asserts exactly that):
#   (S) PRODUCER — verify-completion.sh re-measures the tree at every step boundary and at
#       the end, and records {head_sha_end, tree_fingerprint_end, tree_clean_end, tree_stable,
#       tree_samples}. A producer that always claims "stable" defeats any consumer.
#   (T) CONSUMER — completion_checklist_recency_guard.sh refuses a line that does not show a
#       settled tree: tree_stable true, both endpoints equal to the audited sha/fingerprint,
#       at least two samples. A consumer that ignores the fields defeats any producer.
#
# HONEST LIMIT — sampling granularity: observations happen at step boundaries and at the end
# of the run, so a change made AND undone entirely inside ONE step's execution is not
# observed. This narrows the exposure from "the whole run" to "one step"; it does not
# eliminate it. Closing it completely requires the tree to be immutable while the contract
# runs (read-only snapshot / container), which is fork-receiver infrastructure, not a shell
# check — recorded here rather than papered over.
#
# DECISION (BACKLOG P3-89, recorded 2026-07-29): KEEP step-boundary sampling. The reviewer
# who raised this confirmed the intra-step window is REACHABLE, and confirmed the paragraph
# above describes it accurately — so this is a scope decision, not a correction.
#   BENEFIT of intra-step periodic sampling: shrinks the window from "one step" to "one
#     sampling interval". It does NOT close the class — any interval leaves a smaller window,
#     and only an immutable tree removes it. So the ceiling on the benefit is a constant
#     factor, never a category change.
#   COST: a background sampler running for the length of an R25 run (~2,200s observed) has to
#     be spawned, reaped on every exit path including SIGTERM/timeout, and kept from racing
#     the steps it samples — a watcher whose own failure modes (orphaned process, false
#     "mutated" from reading a file mid-write) would land on the PUSH path, the most
#     load-bearing gate in the catalog. Buying a constant-factor reduction with new
#     concurrency on that path is a bad trade.
#   The exposure is also already bounded by the OTHER links: the pushed commit's tree must be
#   clean at both endpoints and equal to the audited fingerprint, so exploiting the intra-step
#   window requires an edit that is applied and reverted inside one step — the deliberate,
#   reviewer-constructed shape, not an accident.
#   REVISIT TRIGGER (explicit, so this is falsifiable rather than permanent): a DEMONSTRATED
#   real evasion — an intra-step mutation that laundered a push and was not constructed for
#   the purpose of demonstrating it. On that evidence, prefer an immutable-tree run
#   (read-only snapshot / container) over a sampler; the sampler is the option this decision
#   rejects, not the goal.
#
# SCOPE (anti-over-correction, asserted): only PUSH eligibility tightens. A run whose steps
# write only GIT-IGNORED artifacts (every real R25 run does: build/, .ax-verify/) is still
# settled, and a legitimate clean run still ships. Assertions 2 and 3 pin both.
#
# HOW IT IS MEASURED (end to end, never by inspection):
#   A throwaway git repo with a bare "remote", the REAL .githooks/pre-push + pre-push-lib.sh
#   wired via core.hooksPath, the subject verify-completion.sh and the subject recency guard.
#   The checklist itself performs the mutation: step 1 writes the "fix", step 2 passes only
#   because of it, step 3 reverts it — so the tree is clean at both endpoints and the whole
#   scenario is deterministic without a second shell. Ground truth is the REMOTE: laundering
#   means the bare repo's branch actually advanced to the commit that fails the contract.
#
# Usage:
#   bash practices/evals/midrun_tree_mutation_guard.sh
#       LIVE: assert the real pair, then the mutation matrix. Exit 0.
#   bash practices/evals/midrun_tree_mutation_guard.sh --fixture-root DIR
#       Run the SAME assertions against copies neutered per DIR's `neuter-mode` file
#       (none|s|t|st), skipping the matrix. The committed fail_* fixtures must exit 1 — that
#       is what proves these assertions detect the regression rather than passing vacuously.
#
# Exit: 0 = push evidence describes a settled tree · 1 = a tree mutated mid-run can still
#       certify a push (BLOCK) · 2 = setup/tooling error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Find the repo even when this script runs from a copy outside the tree — which is exactly
# what fixture_kill_proof_guard.sh [87] does when it runs a neutered mutant from tmp.
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
        *) echo "midrun_tree_mutation_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SUBJECT_MODE="none"
if [ -n "$FIXTURE_ROOT" ]; then
    [ -d "$FIXTURE_ROOT" ] || {
        echo "midrun_tree_mutation_guard: fixture root not found: $FIXTURE_ROOT" >&2; exit 2; }
    [ -f "$FIXTURE_ROOT/neuter-mode" ] || {
        echo "midrun_tree_mutation_guard: fixture is missing its neuter-mode file" >&2; exit 2; }
    SUBJECT_MODE="$(tr -d ' \t\n' < "$FIXTURE_ROOT/neuter-mode")"
    case "$SUBJECT_MODE" in
        none|s|t|st) ;;
        *) echo "midrun_tree_mutation_guard: unknown neuter-mode '$SUBJECT_MODE'" >&2; exit 2 ;;
    esac
fi

for f in "$REAL_VC" "$REAL_RECENCY" "$REAL_HOOK" "$REAL_HOOK_LIB"; do
    [ -f "$f" ] || { echo "midrun_tree_mutation_guard: missing $f" >&2; exit 2; }
done
command -v python3 >/dev/null 2>&1 || { echo "midrun_tree_mutation_guard: python3 required" >&2; exit 2; }
command -v git >/dev/null 2>&1 || { echo "midrun_tree_mutation_guard: git required" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

VIOLATIONS=0
violation() { echo "  VIOLATION: $*" >&2; VIOLATIONS=$((VIOLATIONS + 1)); }

GIT_ID=(-c user.email=ax@example.invalid -c user.name=ax)

# ── Neuter helpers ───────────────────────────────────────────────────────────
# (S) the producer stops noticing that a sample disagreed with the start, so every line
#     claims a settled tree. (T) the consumer stops requiring one.
NEUTER_S_ANCHOR='TREE_STABLE=false'
NEUTER_S_VALUE='TREE_STABLE=true'
NEUTER_T_ANCHOR='if not tree_settled:'
NEUTER_T_VALUE='if False:'

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
    local want_s=0 want_t=0
    case "$mode" in
        s) want_s=1 ;;
        t) want_t=1 ;;
        st) want_s=1; want_t=1 ;;
    esac
    neuter_copy "$REAL_VC" "$dest/verify-completion.sh" \
        "$NEUTER_S_ANCHOR" "$NEUTER_S_VALUE" "$want_s" || return 3
    neuter_copy "$REAL_RECENCY" "$dest/completion_checklist_recency_guard.sh" \
        "$NEUTER_T_ANCHOR" "$NEUTER_T_VALUE" "$want_t" || return 3
    return 0
}

# The mid-run mutation, expressed as the checklist itself so the scenario is deterministic
# and needs no second shell: the tree is CLEAN when the run starts, dirty while the gated
# step is verified, and CLEAN again before the run ends. `gate.txt` is committed holding
# "fail", so step `gated` passes only on the uncommitted edit step `mutate` just made.
MUTATION_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 60
checklist:
  - id: mutate
    title: "an uncommitted fix appears while the run is in progress"
    commands:
      - command: '"'"'printf pass\\n > gate.txt'"'"'
        expected_exit: 0
  - id: gated
    title: "passes only because of the edit no commit contains"
    commands:
      - command: '"'"'grep -q pass gate.txt'"'"'
        expected_exit: 0
  - id: restore
    title: "the edit is undone before the run ends — both endpoints look pristine"
    commands:
      - command: '"'"'git checkout -- gate.txt'"'"'
        expected_exit: 0'

# The same three-step shape with NO mutation: gate.txt is committed holding "pass". Used for
# the over-correction assertions, so a failure there cannot be blamed on step count.
SETTLED_CHECKLIST='version: 1
defaults:
  working_directory: "."
  timeout_seconds: 60
checklist:
  - id: alpha
    title: "reads the committed tree"
    commands:
      - command: '"'"'grep -q pass gate.txt'"'"'
        expected_exit: 0
  - id: beta
    title: "writes only GIT-IGNORED artifacts, exactly as a real R25 step does"
    commands:
      - command: '"'"'mkdir -p build && date > build/artifact.txt'"'"'
        expected_exit: 0
  - id: gamma
    title: "reads the committed tree again"
    commands:
      - command: '"'"'grep -q pass gate.txt'"'"'
        expected_exit: 0'

# ── Sandbox builder ──────────────────────────────────────────────────────────
# $1 = sandbox dir, $2 = subject dir, $3 = checklist, $4 = committed gate.txt content.
# Leaves: $sb/repo at a commit already pushed to $sb/remote.git, hooks installed via
# core.hooksPath but not yet consulted (the scaffold commit was pushed before that).
build_repo() {
    local sb="$1" subject="$2" checklist="$3" gate="$4"
    mkdir -p "$sb/repo/practices/scripts" "$sb/repo/practices/evals" "$sb/repo/.githooks"
    cp "$subject/verify-completion.sh" "$sb/repo/practices/scripts/"
    cp "$subject/completion_checklist_recency_guard.sh" "$sb/repo/practices/evals/"
    [ -f "$REPO_ROOT/practices/scripts/_collapse_plan.py" ] && \
        cp "$REPO_ROOT/practices/scripts/_collapse_plan.py" "$sb/repo/practices/scripts/"
    cp "$REAL_HOOK" "$REAL_HOOK_LIB" "$sb/repo/.githooks/"
    chmod +x "$sb/repo/.githooks/pre-push"
    printf '%s\n' "$checklist" > "$sb/repo/practices/verification-checklist.yaml"
    printf '%s\n' "$gate" > "$sb/repo/gate.txt"
    # Same ignore set as the real repo: the run's own audit writes and build artifacts must
    # not, by themselves, make the tree look mutated.
    printf '.ax-verify/\nbuild/\n' > "$sb/repo/.gitignore"
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

vc_run() {
    local sb="$1" log="$2"; shift 2
    ( cd "$sb/repo" && bash practices/scripts/verify-completion.sh "$@" ) > "$log" 2>&1
    return $?
}

# ── The mid-run-mutation push harness ────────────────────────────────────────
# Returns 0 = LAUNDERED (the remote actually advanced to a commit whose committed state fails
# the checklist), 1 = held, 2 = harness setup failure. Sets MW_DESC to a one-line witness.
MW_DESC=""
midrun_launders() {
    local subject="$1" tag="$2"
    local sb="$WORK/midrun-$tag"
    build_repo "$sb" "$subject" "$MUTATION_CHECKLIST" "fail" || { MW_DESC="harness setup failed"; return 2; }

    # commit B — its COMMITTED state fails `gated`. The tree is CLEAN here and stays clean at
    # both endpoints of the run; only the middle of the run is dirty.
    printf 'fail\n' > "$sb/repo/gate.txt"
    ( cd "$sb/repo" && git add -A && git "${GIT_ID[@]}" commit -q --allow-empty -m "breaks the gate" ) >/dev/null 2>&1
    local b_sha
    b_sha="$(git -C "$sb/repo" rev-parse HEAD)"

    vc_run "$sb" "$sb/r25.log"; local rc=$?
    # The scenario is only meaningful if the tree really did end clean — otherwise this would
    # be [97]'s dirty-tree case wearing a different hat, and the guard would prove nothing.
    local end_dirty
    end_dirty="$(git -C "$sb/repo" status --porcelain -uall | wc -l | tr -d ' ')"

    ( cd "$sb/repo" && git push origin main ) > "$sb/push.log" 2>&1
    local push_rc=$?
    local remote_tip
    remote_tip="$(git -C "$sb/remote.git" rev-parse refs/heads/main 2>/dev/null || echo none)"
    local landed=0
    [ "$remote_tip" = "$b_sha" ] && landed=1
    local audit
    audit="$(tail -1 "$sb/repo/.ax-verify/runs.jsonl" 2>/dev/null || echo '(absent)')"
    MW_DESC="r25=$rc dirty_paths_at_end=$end_dirty push=$push_rc unverified_commit_landed=$landed audit=$audit"
    if [ "$end_dirty" -ne 0 ]; then
        MW_DESC="$MW_DESC [HARNESS BROKEN: the tree did not end clean]"
        return 2
    fi
    [ "$landed" -eq 1 ]
}

echo "── [mid-run mutation] harness: clean at both endpoints, dirty exactly while the gated step ran"

SUBJECT="$WORK/subject"
if ! make_subject "$SUBJECT" "$SUBJECT_MODE"; then
    echo "midrun_tree_mutation_guard: could not build the subject (neuter-mode=$SUBJECT_MODE)" >&2
    echo "  an anchor no longer appears exactly once — this guard's mutation proof is stale." >&2
    exit 2
fi
[ "$SUBJECT_MODE" = "none" ] || \
    echo "  MODE: fixture neuter-mode=$SUBJECT_MODE (layers neutered in copies of the live scripts)"

midrun_launders "$SUBJECT" subject; LAUNDERED=$?
if [ "$LAUNDERED" -eq 2 ]; then
    echo "  subject          : $MW_DESC"
    echo "midrun_tree_mutation_guard: harness setup failed" >&2; exit 2
fi
echo "  subject          : $MW_DESC"
if [ "$LAUNDERED" -eq 0 ]; then
    violation "a commit whose committed state FAILS the contract was pushed on an audit line" \
              "whose ENDPOINTS were both clean. The tree was mutated after the run started and" \
              "restored before it finished, so the steps in between verified code the commit" \
              "does not contain — and a single snapshot cannot tell the difference."
fi

# ── Assertion 2: a legitimate settled run must still ship ────────────────────
# Over-correction check, with the SAME step count as the harness above so a failure cannot be
# attributed to the shape of the checklist.
SB_OK="$WORK/settled"
if ! build_repo "$SB_OK" "$SUBJECT" "$SETTLED_CHECKLIST" "pass"; then
    echo "midrun_tree_mutation_guard: harness setup failed (settled case)" >&2; exit 2
fi
( cd "$SB_OK/repo" && git "${GIT_ID[@]}" commit -q --allow-empty -m "real work, committed" ) >/dev/null 2>&1
OK_SHA="$(git -C "$SB_OK/repo" rev-parse HEAD)"
vc_run "$SB_OK" "$SB_OK/r25.log"; OK_R25=$?
( cd "$SB_OK/repo" && git push origin main ) > "$SB_OK/push.log" 2>&1
OK_PUSH=$?
OK_TIP="$(git -C "$SB_OK/remote.git" rev-parse refs/heads/main 2>/dev/null || echo none)"
OK_LANDED=0; [ "$OK_TIP" = "$OK_SHA" ] && OK_LANDED=1
echo "  settled push     : r25=$OK_R25 push=$OK_PUSH landed=$OK_LANDED (want 0/0/1)"
if [ "$OK_R25" -ne 0 ] || [ "$OK_PUSH" -ne 0 ] || [ "$OK_LANDED" -ne 1 ]; then
    violation "a settled, committed change could not be pushed (r25=$OK_R25 push=$OK_PUSH" \
              "landed=$OK_LANDED). Steps that write only GIT-IGNORED artifacts — which every" \
              "real R25 step does (build/, .ax-verify/) — must not count as a mutated tree." \
              "Hook output:" "$(tail -5 "$SB_OK/push.log" 2>/dev/null | tr '\n' ' ')"
fi

# ── Assertion 3: the audit line must actually carry the sampling evidence ────
# Structural, and deliberately about the PRODUCER's output rather than the consumer's verdict:
# assertion 2 would also pass if the fields were absent and the consumer had stopped looking.
OK_AUDIT="$(tail -1 "$SB_OK/repo/.ax-verify/runs.jsonl" 2>/dev/null || echo '')"
echo "  settled audit    : $OK_AUDIT"
MISSING=""
for field in head_sha_end tree_fingerprint_end tree_clean_end tree_stable tree_samples; do
    printf '%s' "$OK_AUDIT" | grep -q "\"$field\"" || MISSING="$MISSING $field"
done
if [ -n "$MISSING" ]; then
    violation "the audit line of a good run does not record the across-the-run measurement" \
              "(missing:$MISSING). Without it the consumer has nothing to verify and the" \
              "window between the first step and the audit write is unobserved again."
fi
# More than one sample must have been taken — a "sampling" producer that samples once is the
# unfixed producer with extra fields.
SAMPLES="$(printf '%s' "$OK_AUDIT" | sed -n 's/.*"tree_samples":\([0-9]*\).*/\1/p')"
if [ -z "$SAMPLES" ] || [ "$SAMPLES" -lt 2 ]; then
    violation "the run recorded tree_samples=${SAMPLES:-none}: the tree was not re-measured" \
              "across the run, so the endpoints are the only evidence there is."
fi

# ── Assertion 4 (non-vacuity matrix) — LIVE mode only ────────────────────────
# (S) and (T) are SERIAL links in one chain, not redundant layers: a producer that always
# claims stability defeats an attentive consumer, and an indifferent consumer defeats an
# honest producer. The honest matrix is therefore "each neuter alone must REPRODUCE".
if [ -z "$FIXTURE_ROOT" ]; then
    echo "── [mutation matrix] producer sampling (S) and consumer enforcement (T) — serial links"
    for mode in st s t; do
        MUT="$WORK/mut-$mode"
        if ! make_subject "$MUT" "$mode"; then
            violation "could not neuter '$mode' — an anchor no longer appears exactly once in" \
                      "verify-completion.sh / completion_checklist_recency_guard.sh, so this" \
                      "guard's mutation proof is stale."
            continue
        fi
        midrun_launders "$MUT" "$mode"; MUT_LAUNDERED=$?
        case "$mode" in
            st) label="S+T neutered (must REPRODUCE)" ;;
            s)  label="S neutered   (must REPRODUCE — a producer that never sees drift defeats the consumer)" ;;
            t)  label="T neutered   (must REPRODUCE — an indifferent consumer defeats the producer)" ;;
        esac
        echo "  $label: $MW_DESC"
        if [ "$MUT_LAUNDERED" -eq 2 ]; then
            violation "the '$mode' harness failed to set up, so it proves nothing."
        elif [ "$MUT_LAUNDERED" -ne 0 ]; then
            violation "with '$mode' neutered the harness did NOT reproduce the mid-run-mutation" \
                      "push. The assertions above therefore prove nothing — they would pass on" \
                      "the unfixed scripts too. Fix the harness, not the guard."
        fi
    done
fi

echo ""
if [ "$VIOLATIONS" -gt 0 ]; then
    echo "midrun_tree_mutation_guard: FAIL — $VIOLATIONS violation(s): a tree that changed while R25 ran can still certify a push" >&2
    exit 1
fi
echo "midrun_tree_mutation_guard: PASS — push evidence must come from a tree that did not change while the contract ran (producer samples at every step boundary and records both endpoints + stability, the recency guard verifies the relation), while steps that write only git-ignored artifacts still ship. LIMIT: sampling is at step boundaries, so a change made and undone inside one step is unobserved."
exit 0
