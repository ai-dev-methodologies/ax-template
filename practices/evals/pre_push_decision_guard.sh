#!/usr/bin/env bash
# practices/evals/pre_push_decision_guard.sh — behavior-test the pre-push hook's
# decision surface (P2-17).
#
# WHY: .githooks/pre-push encodes 10 distinct branch decisions (per-ref R25 sha
# selection, delete-only skip, multi-ref union, out-of-scope skip, in-scope run,
# new-branch merge-base, unresolvable base fail-closed, git-diff-failure BLOCK,
# empty-stdin fallback) plus the primary block path (stale/missing R25 audit).
# These were proven correct at introduction and by cross-review, but nothing
# PINNED them — a future refactor of the hook could silently regress a
# fail-closed branch with no test to catch it. This guard exercises every branch
# and asserts BOTH the exit code AND which stages fired.
#
# HOW (never touches real gradle or the real R25 audit):
#   - Pure-decision scenarios call the extracted lib functions
#     (.githooks/pre-push-lib.sh) directly inside a throwaway `git init` repo.
#   - Integration scenarios invoke the REAL .githooks/pre-push with crafted
#     stdin inside a throwaway repo whose practices/evals/
#     completion_checklist_recency_guard.sh is a STUB (records the --expect-sha,
#     honors AX_STUB_RECENCY_EXIT) and whose backend/gradlew is a STUB (echoes a
#     marker, exits 0). The hook cd's to the throwaway root, so both stubs win —
#     the sweep NEVER invokes real gradle or the real recency guard.
#
# The committed expectation table lives at
# practices/evals/fixtures/pre-push-decision/scenarios.yaml; this guard asserts
# 1:1 parity with the scenarios it executes (the table cannot silently drift from
# the code) before running them.
#
# Exit codes: 0 all scenarios pass · 1 a scenario mismatched (BLOCK) · 2 harness error.
#
# LIMITS: this proves the hook's DECISION wiring — it does not run the real R25
# guard or the real gradle suite (those are stubbed by construction, so the
# sweep is hermetic and offline). The real R25 guard's own non-vacuity is pinned
# by completion_checklist_recency_guard.sh --fixtures + fixture_kill_proof [87];
# the real regression suite by the per-domain gradle tasks. A green sweep here
# means "given a passing R25 and a passing suite, the hook routes every ref to
# the correct stage"; it is silent about whether the suite itself is correct.

# ── ROUND 7 / P1-1: PRIVILEGED-MODE RE-EXEC (TD-2026-07-30-P1-privileged-startup) ───
# This guard SOURCES .githooks/pre-push-lib.sh to exercise its pure decision functions, and that
# lib now ASSERTS bash privileged mode as its first executable text — a sourced file cannot
# re-exec without replacing its caller, so it requires the caller to already be privileged. This
# guard is therefore a first-class CALLER of the lib and must meet the same contract; without
# this block every `source` below aborts with HERMETIC_PRIVILEGED_UNREACHABLE (measured in the
# round-7 sweep). Re-execing here also covers the subshells, since privileged mode is a property
# of the PROCESS. Same construction as the eight entries: `case` is a keyword, ${x:?} is a
# parameter expansion, /usr/bin/env is absolute, and the second case catches a shadowed `exec`.
case $- in
    *p*) ;;
    *) case "${AX_PRIV_REEXEC-}" in
           1) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"pre_push_decision_guard: HERMETIC_PRIVILEGED_UNREACHABLE — a re-exec into bash privileged mode was already attempted and this shell is STILL unprivileged."} ;;
           *) case "${BASH:-}" in
                  /*) exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@" ;;
                  *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"pre_push_decision_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the running interpreter (BASH) is not an absolute path."} ;;
              esac ;;
       esac ;;
esac
case $- in
    *p*) ;;
    *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"pre_push_decision_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the re-exec returned instead of replacing this process, which means exec itself is shadowed."} ;;
esac
unset AX_PRIV_REEXEC
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
HOOK_SRC="$REPO_ROOT/.githooks/pre-push"
LIB_SRC="$REPO_ROOT/.githooks/pre-push-lib.sh"
SCENARIOS_YAML="$SCRIPT_DIR/fixtures/pre-push-decision/scenarios.yaml"
ZERO="0000000000000000000000000000000000000000"

PASS=0
FAIL=0
RESULTS=()
EXECUTED_IDS=()

pass_case() { PASS=$((PASS + 1)); RESULTS+=("PASS [$1]"); EXECUTED_IDS+=("$1"); }
fail_case() {
    FAIL=$((FAIL + 1)); RESULTS+=("FAIL [$1] — $2"); EXECUTED_IDS+=("$1")
}

[[ -f "$HOOK_SRC" ]] || { echo "pre_push_decision_guard: FAIL — hook not found at $HOOK_SRC" >&2; exit 2; }
[[ -f "$LIB_SRC"  ]] || { echo "pre_push_decision_guard: FAIL — lib not found at $LIB_SRC" >&2; exit 2; }

# ── scratch repo scaffold ─────────────────────────────────────────────────────
# Build a throwaway git repo that mirrors the real repo's hook-relevant layout:
# a base commit on `main`, the real hook + lib copied into .githooks/, a STUB
# recency guard, and a STUB gradlew. Echoes the repo dir on stdout.
# Optional arg1 = a lib file to install INSTEAD of the real one (used by the
# self-proof to install a mutated lib and confirm the sweep is non-vacuous).
make_scratch() {
    local lib_override="${1:-$LIB_SRC}" work
    work="$(mktemp -d "${TMPDIR:-/tmp}/pp-decision.XXXXXX")" || return 2
    (
        cd "$work" || exit 2
        git init -q -b main .
        git config user.email t@ax.test
        git config user.name  ax-test
        git config commit.gpgsign false
        mkdir -p docs backend practices/evals practices/scripts .githooks
        cp "$HOOK_SRC" .githooks/pre-push
        cp "$lib_override" .githooks/pre-push-lib.sh
        chmod +x .githooks/pre-push

        # STUB recency guard: parse --expect-sha, append it to AX_STUB_RECENCY_LOG,
        # exit AX_STUB_RECENCY_EXIT (default 0). Never touches the real audit log.
        # BACKLOG P3-112: the stub records --expect-anchor-sha TOO, and tags WHICH copy of the
        # guard it is. The hook runs the recency guard twice per shipping ref — the PREVIOUS
        # RELEASE'S copy, extracted out of git into /tmp/ax-ratchet.*, and then the tree's own —
        # and only the second one carries the anchor binding conditionally (ANCHOR_ARGS is empty
        # for a non-anchor branch). Without the tag the two calls are indistinguishable in one
        # log and an assertion about "the anchor arg" could be satisfied by the wrong call.
        cat > practices/evals/completion_checklist_recency_guard.sh <<'STUB'
#!/usr/bin/env bash
sha=""; anchor=""
while [ $# -gt 0 ]; do
    case "$1" in
        --expect-sha) sha="$2"; shift 2 ;;
        --expect-anchor-sha) anchor="$2"; shift 2 ;;
        *) shift ;;
    esac
done
[ -n "${AX_STUB_RECENCY_LOG:-}" ] && printf '%s\n' "$sha" >> "$AX_STUB_RECENCY_LOG"
if [ -n "${AX_STUB_ANCHOR_LOG:-}" ]; then
    case "$0" in
        */ax-ratchet.*/*) _tag=anchor-copy ;;
        *) _tag=tree-copy ;;
    esac
    printf '%s %s %s\n' "$_tag" "$sha" "${anchor:-<none>}" >> "$AX_STUB_ANCHOR_LOG"
fi
exit "${AX_STUB_RECENCY_EXIT:-0}"
STUB
        chmod +x practices/evals/completion_checklist_recency_guard.sh

        # STUB gradlew: echo a marker so the integration scenarios can assert the
        # regression suite fired (or did not), then exit 0. Never runs real gradle.
        cat > backend/gradlew <<'STUB'
#!/usr/bin/env bash
echo "__GRADLEW_FIRED__ $*"
exit 0
STUB
        chmod +x backend/gradlew

        echo base > docs/readme.md
        git add -A
        git commit -qm "base"
    ) || return 2
    echo "$work"
}

# Run the scratch hook with crafted stdin. Args:
#   <repo> <stdin-string> [recency_exit=0] [recency_log=]
# The recency_exit/recency_log are exported onto the `bash` child so the STUB
# recency guard honors them. Sets globals OUT (combined stdout+stderr) and RC
# (hook exit code). MUST be called directly, NOT inside $(...) — a
# command-substitution subshell would discard the RC/OUT assignments.
OUT=""
RC=0
run_hook() {
    local repo="$1" stdin_str="$2" rexit="${3:-0}" rlog="${4:-}" alog="${5:-}"
    OUT="$(cd "$repo" && printf '%s' "$stdin_str" | \
        AX_STUB_RECENCY_EXIT="$rexit" AX_STUB_RECENCY_LOG="$rlog" AX_STUB_ANCHOR_LOG="$alog" \
        bash .githooks/pre-push origin file://"$repo" 2>&1)"
    RC=$?
}

# ── 1. parity: every scenario id in scenarios.yaml is executed here ───────────
check_parity() {
    [[ -f "$SCENARIOS_YAML" ]] || { echo "pre_push_decision_guard: FAIL — scenarios.yaml missing at $SCENARIOS_YAML" >&2; exit 2; }
    local declared
    declared="$(python3 - "$SCENARIOS_YAML" <<'PY'
import sys, re
ids = []
for ln in open(sys.argv[1]):
    m = re.match(r'\s*-\s*id:\s*(\S+)', ln)
    if m:
        ids.append(m.group(1))
print("\n".join(ids))
PY
)"
    # store for post-run comparison
    DECLARED_IDS="$declared"
}

# ══ SCENARIOS ═════════════════════════════════════════════════════════════════

# S1 — per-ref R25 uses the PUSHED local_sha, not the checkout HEAD.
scenario_s1_r25_uses_pushed_sha() {
    local id="s1-r25-uses-pushed-sha" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"
      git checkout -q -b feature
      echo more >> docs/readme.md && git add -A && git commit -qm c1
      git checkout -q main )                       # HEAD = base (C0), feature tip = C1
    local head_sha feat_sha log
    head_sha="$(cd "$repo" && git rev-parse main)"
    feat_sha="$(cd "$repo" && git rev-parse feature)"
    log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/feature $feat_sha refs/heads/feature $ZERO" 0 "$log"; out="$OUT"
    local recorded; recorded="$(head -1 "$log" 2>/dev/null)"
    if [[ "$recorded" == "$feat_sha" && "$recorded" != "$head_sha" ]]; then
        pass_case "$id"
    else
        fail_case "$id" "recency got '$recorded'; expected pushed $feat_sha (not HEAD $head_sha)"
    fi
    rm -rf "$repo"
}

# S2 — delete-only push skips both stages, exit 0.
scenario_s2_delete_only_skips() {
    local id="s2-delete-only-skips" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/gone $ZERO refs/heads/gone $(cd "$repo" && git rev-parse main)" 0 "$log"; out="$OUT"
    if [[ "$RC" -eq 0 && ! -s "$log" && "$out" == *"delete-only push"* && "$out" != *"__GRADLEW_FIRED__"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC recency-log-nonempty=$([[ -s "$log" ]] && echo y || echo n) out=<<$out>>"
    fi
    # pure-function cross-check
    if ( cd "$repo" && source .githooks/pre-push-lib.sh && pp_ref_is_deletion "$ZERO" && ! pp_ref_is_deletion "abc123" ); then
        pass_case "s2b-is-deletion-predicate"
    else
        fail_case "s2b-is-deletion-predicate" "pp_ref_is_deletion mis-classified"
    fi
    rm -rf "$repo"
}

# S3 — multi-ref one-ship-one-delete: deletion skipped, ship gets R25.
scenario_s3_multiref_ship_and_delete() {
    local id="s3-multiref-ship-and-delete" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"; echo x >> docs/readme.md && git add -A && git commit -qm c1 )
    local ship_sha; ship_sha="$(cd "$repo" && git rev-parse main)"
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/main $ship_sha refs/heads/main $ZERO
refs/heads/old $ZERO refs/heads/old $ship_sha" 0 "$log"; out="$OUT"
    # TWO recency calls for the ONE shipping ref, none for the deletion (ROUND 6): the hook now
    # runs the PREVIOUS RELEASE'S copy of the guard before the tree's own copy, so a committed
    # backdoor in the tree copy cannot certify itself (TD-2026-07-30-P1-preflight-and-raw-bytes).
    # Both calls carry the same --expect-sha; what this scenario is about is that the DELETION ref
    # contributes none and the ship sha — not HEAD — is the one passed.
    local n; n="$(wc -l < "$log" 2>/dev/null | tr -d ' ')"
    local distinct; distinct="$(sort -u "$log" 2>/dev/null | wc -l | tr -d ' ')"
    if [[ "$RC" -eq 0 && "$n" == "2" && "$distinct" == "1" && "$(head -1 "$log")" == "$ship_sha" ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC recency-calls=$n distinct=$distinct first=$(head -1 "$log" 2>/dev/null)"
    fi
    rm -rf "$repo"
}

# S4 — multi-ref regression union: two in-scope refs → suite runs exactly ONCE.
scenario_s4_regression_union_once() {
    local id="s4-regression-union-once" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    # two commits each touching backend/ (in scope), pushed as two refs
    ( cd "$repo"
      mkdir -p backend/src
      echo a > backend/src/A.java && git add -A && git commit -qm ca
      git branch b1
      echo b > backend/src/B.java && git add -A && git commit -qm cb
      git branch b2 )
    local b1 b2 remote_base
    remote_base="$(cd "$repo" && git rev-parse HEAD~2 2>/dev/null || git rev-parse HEAD)"
    b1="$(cd "$repo" && git rev-parse b1)"
    b2="$(cd "$repo" && git rev-parse b2)"
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/b1 $b1 refs/heads/b1 $remote_base
refs/heads/b2 $b2 refs/heads/b2 $remote_base" 0 "$log"; out="$OUT"
    local fired; fired="$(grep -c "__GRADLEW_FIRED__" <<< "$out")"
    if [[ "$RC" -eq 0 && "$fired" == "1" && "$out" == *"running full regression"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC gradlew-fired=$fired (expected exactly 1)"
    fi
    rm -rf "$repo"
}

# S5 — docs-only out-of-scope: R25 runs, regression SKIPPED, exit 0.
scenario_s5_out_of_scope_skips_regression() {
    local id="s5-out-of-scope-skips-regression" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    local base; base="$(cd "$repo" && git rev-parse main)"
    ( cd "$repo"; echo docschange >> docs/readme.md && git add -A && git commit -qm docs )
    local tip; tip="$(cd "$repo" && git rev-parse main)"
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/main $tip refs/heads/main $base" 0 "$log"; out="$OUT"
    if [[ "$RC" -eq 0 && -s "$log" && "$out" != *"__GRADLEW_FIRED__"* && "$out" != *"running full regression"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC regression-fired=$([[ "$out" == *__GRADLEW_FIRED__* ]] && echo y || echo n)"
    fi
    rm -rf "$repo"
}

# S6 — in-scope path (backend/): regression RUNS, exit 0.
scenario_s6_in_scope_runs_regression() {
    local id="s6-in-scope-runs-regression" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    local base; base="$(cd "$repo" && git rev-parse main)"
    ( cd "$repo"; mkdir -p backend/src && echo c > backend/src/C.java && git add -A && git commit -qm backend )
    local tip; tip="$(cd "$repo" && git rev-parse main)"
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/main $tip refs/heads/main $base" 0 "$log"; out="$OUT"
    if [[ "$RC" -eq 0 && "$out" == *"__GRADLEW_FIRED__ -q testPractices testAsvs testCrud"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC out=<<$out>>"
    fi
    rm -rf "$repo"
}

# S7 — new remote branch (remote_sha=ZERO): base = merge-base with origin/main.
scenario_s7_new_branch_mergebase() {
    local id="s7-new-branch-mergebase" repo
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"
      git update-ref refs/remotes/origin/main "$(git rev-parse main)"   # origin tip = C0
      mkdir -p backend/src && echo d > backend/src/D.java && git add -A && git commit -qm d )
    local tip base_expected
    tip="$(cd "$repo" && git rev-parse main)"
    base_expected="$(cd "$repo" && git rev-parse refs/remotes/origin/main)"
    # pure-function assertion: pp_resolve_ref_base with ZERO remote_sha resolves to the merge-base
    local base_got rc
    base_got="$(cd "$repo" && source .githooks/pre-push-lib.sh && pp_resolve_ref_base "$tip" "$ZERO")"; rc=$?
    if [[ "$rc" -eq 0 && "$base_got" == "$base_expected" ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$rc base='$base_got' expected merge-base '$base_expected'"
    fi
    rm -rf "$repo"
}

# S8 — unresolvable base (ZERO remote, no origin refs): fail-closed, suite RUNS.
scenario_s8_unresolvable_base_failclosed() {
    local id="s8-unresolvable-base-failclosed" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    # no origin/main, no origin/HEAD → merge-base fails → base empty
    local tip; tip="$(cd "$repo" && git rev-parse main)"
    # pure-function: resolve returns non-zero (unresolvable)
    if ( cd "$repo" && source .githooks/pre-push-lib.sh && ! pp_resolve_ref_base "$tip" "$ZERO" >/dev/null ); then
        pass_case "s8b-resolve-unresolvable-returns-nonzero"
    else
        fail_case "s8b-resolve-unresolvable-returns-nonzero" "pp_resolve_ref_base did not fail on missing origin"
    fi
    # integration: hook fail-closes → regression suite fires
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/wip $tip refs/heads/wip $ZERO" 0 "$log"; out="$OUT"
    if [[ "$RC" -eq 0 && "$out" == *"__GRADLEW_FIRED__"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC regression-fired=$([[ "$out" == *__GRADLEW_FIRED__* ]] && echo y || echo n) (expected fail-closed run)"
    fi
    rm -rf "$repo"
}

# S9 — git diff failure (bogus base object): fail-closed BLOCK, exit 1.
scenario_s9_diff_failure_blocks() {
    local id="s9-diff-failure-blocks" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"; echo e >> docs/readme.md && git add -A && git commit -qm e )
    local tip bogus="deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
    tip="$(cd "$repo" && git rev-parse main)"
    local log="$repo/.recency.log"
    # remote_sha = bogus non-existent object (not ZERO) → git diff bogus..tip fails
    run_hook "$repo" "refs/heads/main $tip refs/heads/main $bogus" 0 "$log"; out="$OUT"
    if [[ "$RC" -eq 1 && "$out" == *"git diff failed"* && "$out" == *"push BLOCKED (fail-closed)"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC (expected 1 + 'git diff failed') out=<<$out>>"
    fi
    rm -rf "$repo"
}

# S10 — empty-stdin manual fallback: single-ref HEAD vs @{u} (or ZERO base).
scenario_s10_empty_stdin_fallback() {
    local id="s10-empty-stdin-fallback" repo
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    local head_sha specs_noup
    head_sha="$(cd "$repo" && git rev-parse HEAD)"
    # no upstream configured → fallback uses ZERO base
    specs_noup="$(cd "$repo" && source .githooks/pre-push-lib.sh && pp_effective_push_specs "")"
    if [[ "$specs_noup" == "HEAD $head_sha HEAD $ZERO" ]]; then
        pass_case "${id}-no-upstream"
    else
        fail_case "${id}-no-upstream" "got '$specs_noup' expected 'HEAD $head_sha HEAD $ZERO'"
    fi
    # with an upstream → fallback uses the upstream sha
    ( cd "$repo"
      git remote add origin file://"$repo" 2>/dev/null || true
      git update-ref refs/remotes/origin/main "$(git rev-parse HEAD)"
      git config branch.main.remote origin
      git config branch.main.merge refs/heads/main )
    local up_sha specs_up
    up_sha="$(cd "$repo" && git rev-parse '@{u}')"
    specs_up="$(cd "$repo" && source .githooks/pre-push-lib.sh && pp_effective_push_specs "")"
    if [[ "$specs_up" == "HEAD $head_sha HEAD $up_sha" ]]; then
        pass_case "${id}-with-upstream"
    else
        fail_case "${id}-with-upstream" "got '$specs_up' expected 'HEAD $head_sha HEAD $up_sha'"
    fi
    # non-empty stdin is passed through verbatim
    local passthru; passthru="$(cd "$repo" && source .githooks/pre-push-lib.sh && pp_effective_push_specs "a b c d")"
    if [[ "$passthru" == "a b c d" ]]; then
        pass_case "${id}-passthrough"
    else
        fail_case "${id}-passthrough" "got '$passthru'"
    fi
    rm -rf "$repo"
}

# PRIMARY BLOCK — stale/missing R25 audit: recency guard fails → push BLOCKED.
scenario_primary_r25_block() {
    local id="primary-r25-block" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"; echo f >> docs/readme.md && git add -A && git commit -qm f )
    local tip; tip="$(cd "$repo" && git rev-parse main)"
    local base; base="$(cd "$repo" && git rev-parse HEAD~1)"
    run_hook "$repo" "refs/heads/main $tip refs/heads/main $base" 1; out="$OUT"
    # ROUND 6: a failing recency verdict may now surface from EITHER copy — the previous
    # release's (run first, reported as RATCHET_PRIOR_RELEASE_GUARD_FAILED) or the tree's. Both
    # are the same R25 block; what must hold is that the regression never runs.
    if [[ "$RC" -eq 1 && ( "$out" == *"completion_checklist_recency_guard FAILED"* \
                        || "$out" == *"RATCHET_PRIOR_RELEASE_GUARD_FAILED"* ) \
          && "$out" != *"__GRADLEW_FIRED__"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "rc=$RC (expected 1 + recency-FAILED, no regression) out=<<$out>>"
    fi
    rm -rf "$repo"
}

# S11 — THE ANCHOR BINDING IS ACTUALLY WIRED (BACKLOG P3-112).
# The hook's whole layer-3 authentication is one argument: --expect-anchor-sha <the sha GIT took
# from the remote's own advertisement>. Every scenario above asserted --expect-sha and the stage
# that fired; none asserted that this argument was passed at all, so deleting `ANCHOR_ARGS` from
# the hook — which removes the binding entirely and lets a forged refs/remotes/origin/main stand
# — left the whole table GREEN. Asserted here in BOTH directions, because the binding is
# deliberately conditional:
#   · anchor branch (remote ref refs/heads/main), remote already has it → the TREE copy must
#     receive exactly the advertised remote sha;
#   · any other ref → the tree copy must receive NOTHING (documented no-op: that push does not
#     advance origin/main, so there is no remote advertisement to bind to).
# The anchor-copy call is logged too and deliberately NOT constrained here: it always receives an
# extraction rev, which may legitimately be the local fallback.
scenario_s11_anchor_sha_wired() {
    local id="s11-anchor-sha-wired" repo out
    repo="$(make_scratch)" || { fail_case "$id" "scratch setup failed"; return; }
    ( cd "$repo"; echo a >> docs/readme.md && git add -A && git commit -qm c1 ) >/dev/null 2>&1
    local tip base alog tree_line
    tip="$(cd "$repo" && git rev-parse main)"
    base="$(cd "$repo" && git rev-parse HEAD~1)"
    alog="$repo/.anchor.log"

    # (a) anchor branch: the remote advertises $base for refs/heads/main.
    run_hook "$repo" "refs/heads/main $tip refs/heads/main $base" 0 "" "$alog"
    tree_line="$(grep '^tree-copy ' "$alog" 2>/dev/null | head -1)"
    if [[ "$tree_line" == "tree-copy $tip $base" ]]; then
        pass_case "$id"
    else
        fail_case "$id" "tree-copy call got '<<${tree_line:-<no tree-copy call>}>>'; expected 'tree-copy $tip $base' (rc=$RC)"
    fi

    # (b) NON-anchor branch: no remote advertisement to bind to → no anchor arg on the tree copy.
    ( cd "$repo" && git checkout -q -b feature && echo b >> docs/readme.md \
      && git add -A && git commit -qm c2 ) >/dev/null 2>&1
    local feat; feat="$(cd "$repo" && git rev-parse feature)"
    : > "$alog"
    run_hook "$repo" "refs/heads/feature $feat refs/heads/feature $tip" 0 "" "$alog"
    tree_line="$(grep '^tree-copy ' "$alog" 2>/dev/null | head -1)"
    if [[ "$tree_line" == "tree-copy $feat <none>" ]]; then
        pass_case "${id}-nonanchor-unbound"
    else
        fail_case "${id}-nonanchor-unbound" "tree-copy call got '<<${tree_line:-<no tree-copy call>}>>'; expected 'tree-copy $feat <none>'"
    fi
    rm -rf "$repo"
}

# SELF-PROOF — the sweep is NON-VACUOUS: with a MUTATED lib whose deletion
# predicate never matches, the delete-only decision must REGRESS (R25 gets called
# with the ZERO sha and SHIPPING becomes 1). If the mutated lib still produced the
# correct delete-only skip, the s2 scenario would be vacuous. This is the
# committed analogue of fixture_kill_proof [87]: it proves s2's assertion depends
# on the real branch logic, not on an always-true accident.
scenario_selfproof_nonvacuous() {
    local id="selfproof-nonvacuous" repo out mutlib
    mutlib="$(mktemp)"
    # neuter pp_ref_is_deletion so a ZERO sha is NOT recognized as a deletion
    sed -E 's/\[\[ "\$1" == "\$PP_ZERO_SHA" \]\]/[[ "$1" == "__NEVER_A_DELETION__" ]]/' "$LIB_SRC" > "$mutlib"
    if ! grep -q '__NEVER_A_DELETION__' "$mutlib"; then
        fail_case "$id" "mutation sed did not apply — anchor drifted in pre-push-lib.sh"
        rm -f "$mutlib"; return
    fi
    repo="$(make_scratch "$mutlib")" || { fail_case "$id" "scratch setup failed"; rm -f "$mutlib"; return; }
    local log="$repo/.recency.log"
    run_hook "$repo" "refs/heads/gone $ZERO refs/heads/gone $(cd "$repo" && git rev-parse main)" 0 "$log"; out="$OUT"
    # With the mutation, the delete-only skip is gone: R25 fires for the ZERO sha.
    if [[ -s "$log" && "$out" == *"recency check for 000000000000"* && "$out" != *"delete-only push"* ]]; then
        pass_case "$id"
    else
        fail_case "$id" "mutated lib did NOT regress delete-only skip → s2 is VACUOUS (log-empty=$([[ -s "$log" ]] && echo n || echo y))"
    fi
    rm -rf "$repo"; rm -f "$mutlib"
}

# ── run ───────────────────────────────────────────────────────────────────────
check_parity

scenario_s1_r25_uses_pushed_sha
scenario_s2_delete_only_skips
scenario_s3_multiref_ship_and_delete
scenario_s4_regression_union_once
scenario_s5_out_of_scope_skips_regression
scenario_s6_in_scope_runs_regression
scenario_s7_new_branch_mergebase
scenario_s8_unresolvable_base_failclosed
scenario_s9_diff_failure_blocks
scenario_s10_empty_stdin_fallback
scenario_s11_anchor_sha_wired
scenario_primary_r25_block
scenario_selfproof_nonvacuous

# parity check: every declared id must have been executed as a top-level case.
# (top-level scenario ids match the yaml; the *b / -no-upstream sub-assertions
# are extra falsification checks not required to appear in the table.)
missing_ids=""
while IFS= read -r did; do
    [[ -z "$did" ]] && continue
    # a declared id is satisfied by an exact match OR by any executed sub-id that
    # extends it (s10-empty-stdin-fallback ⇒ s10-...-with-upstream et al.).
    hit=0
    for eid in "${EXECUTED_IDS[@]}"; do [[ "$eid" == "$did" || "$eid" == "$did"* ]] && hit=1 && break; done
    [[ "$hit" -eq 0 ]] && missing_ids+="$did "
done <<< "$DECLARED_IDS"

echo "=== pre_push_decision_guard — hook decision-surface sweep ==="
for r in "${RESULTS[@]}"; do echo "  $r"; done
echo ""
if [[ -n "$missing_ids" ]]; then
    echo "pre_push_decision_guard: FAIL — scenarios.yaml declares ids not executed: $missing_ids" >&2
    exit 1
fi
echo "Total: $PASS passed, $FAIL failed"
if [[ "$FAIL" -gt 0 ]]; then
    echo "pre_push_decision_guard: FAIL — $FAIL scenario(s) mismatched expected decision" >&2
    exit 1
fi
echo "pre_push_decision_guard: all scenarios PASS"
exit 0
