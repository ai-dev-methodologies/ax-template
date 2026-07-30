#!/usr/bin/env bash
# .githooks/pre-push-lib.sh — pure decision logic for the pre-push hook.
#
# Extracted from .githooks/pre-push so the hook's branch decisions can be
# exercised by practices/evals/pre_push_decision_guard.sh WITHOUT shipping a
# real push or invoking real gradle. The hook SOURCES this file; its OBSERVABLE
# behavior (stdout markers, exit codes, env honored) is byte-for-byte unchanged —
# this file only factors out the per-ref decisions the hook already made inline.
#
# Purity contract: every function here reads git (rev-parse / merge-base / diff)
# and its arguments only. NONE writes files, hits the network, or calls exit —
# each returns via stdout + exit code so the hook keeps orchestrating the echo
# markers and `exit` statements exactly as before. This keeps the load-bearing
# I/O (the R25 recency guard call, the gradle regression run) in the hook and
# the branch logic here, testable in isolation.
#
# LIMITS: this file is the DECISION surface only. It does NOT run R25 recency,
# does NOT run the gradle regression suite, and does NOT decide whether to run
# them — the hook does, using these predicates. A test of this file proves the
# branch selection is correct; proving the hook wires the branches to the right
# side effects requires the integration scenarios in pre_push_decision_guard.sh.

# Idempotent source guard.
[[ -n "${_AX_PRE_PUSH_LIB:-}" ]] && return 0
_AX_PRE_PUSH_LIB=1

PP_ZERO_SHA="0000000000000000000000000000000000000000"
PP_REGRESSION_SCOPE='^(backend/|practices/|specs/spring-practices-l0\.yaml)'
# The REMOTE-side ref whose advertised sha is the release anchor the ratcheting guards measure
# against. Kept in lockstep with AX_ANCHOR_REMOTE_REF in practices/scripts/lib/release_anchor.sh.
PP_ANCHOR_REMOTE_REF="refs/heads/main"

# pp_anchor_expect_sha <remote_ref> <remote_sha>
#   P1-X layer 3 (2026-07-30) — the ref is not the tree. The ratcheting guards resolve "the
#   previous release" from refs/remotes/origin/main, which is an ORDINARY LOCAL REF that
#   `git update-ref` can aim at a synthetic commit lacking the ratcheting files (every ratchet
#   then bootstrap-skips and R25 passes on a downgrade). Git hands THIS hook the remote sha from
#   the remote's own advertisement, so it is the one authoritative copy in the whole pipeline.
#   Prints that sha when this ref IS the anchor branch and the remote already has it; prints
#   NOTHING (and returns 1) otherwise:
#     · a different ref (feature branch) does not advance origin/main, so there is nothing to
#       authenticate — the check fires on the push that actually publishes the release;
#     · a ZERO remote sha means the remote does not have this branch yet, so it advertises no
#       release to bind to.
#   Pure: reads its arguments only.
pp_anchor_expect_sha() {
    local remote_ref="$1" remote_sha="$2"
    [[ "$remote_ref" == "$PP_ANCHOR_REMOTE_REF" ]] || return 1
    [[ -n "$remote_sha" && "$remote_sha" != "$PP_ZERO_SHA" ]] || return 1
    printf '%s' "$remote_sha"
    return 0
}

# pp_effective_push_specs <stdin-content>
#   Scenario 10 — empty-stdin manual fallback. Git feeds the hook
#   `local_ref local_sha remote_ref remote_sha` lines on stdin per `man
#   githooks`; a manual invocation with no stdin behaves as a single-ref push of
#   HEAD against the upstream tracking branch (or ZERO base when there is none).
#   Prints the effective PUSH_SPECS string (no trailing newline, matching the
#   original `PUSH_SPECS="$(cat)"` capture). Never fails.
pp_effective_push_specs() {
    local raw="$1"
    if [[ -n "$raw" ]]; then
        printf '%s' "$raw"
        return 0
    fi
    local head_sha up_sha
    head_sha="$(git rev-parse HEAD)"
    if up_sha="$(git rev-parse '@{u}' 2>/dev/null)"; then
        printf 'HEAD %s HEAD %s' "$head_sha" "$up_sha"
    else
        printf 'HEAD %s HEAD %s' "$head_sha" "$PP_ZERO_SHA"
    fi
}

# pp_ref_is_deletion <local_sha>
#   Scenario 2/3 — a deletion (local_sha == ZERO) ships no work, so the hook
#   skips both stages for that ref. Exit 0 = deletion, exit 1 = ships commits.
pp_ref_is_deletion() {
    [[ "$1" == "$PP_ZERO_SHA" ]]
}

# pp_resolve_ref_base <local_sha> <remote_sha>
#   Scenario 7/8 — the base for THIS ref's regression diff. Remote tip when it
#   exists; else the merge-base with the default branch (a new remote branch:
#   remote_sha is ZERO). Prints the base sha on stdout and returns 0 when
#   resolved; returns 1 (no stdout) when unresolvable so the caller can
#   fail-closed (run the suite) rather than assume out-of-scope.
pp_resolve_ref_base() {
    local local_sha="$1" remote_sha="$2" base
    base="$remote_sha"
    if [[ "$base" == "$PP_ZERO_SHA" ]]; then
        base="$(git merge-base "$local_sha" origin/HEAD 2>/dev/null \
             || git merge-base "$local_sha" origin/main 2>/dev/null || true)"
    fi
    if [[ -z "$base" ]]; then
        return 1
    fi
    printf '%s' "$base"
    return 0
}

# pp_diff_touches_scope <base> <local_sha>
#   Scenario 5/6/9 — does the ref's diff touch the regression scope (backend/,
#   practices/, or the seed spec)?
#     exit 0 — touches scope (regression needed)
#     exit 1 — out of scope
#     exit 2 — `git diff` itself FAILED (bad range / missing objects); the diff
#              error text is printed on stdout so the caller can surface it. This
#              is fail-closed: the hook BLOCKS rather than silently skipping.
pp_diff_touches_scope() {
    local base="$1" local_sha="$2" diff_files
    if ! diff_files="$(git diff --name-only "$base" "$local_sha" 2>&1)"; then
        printf '%s' "$diff_files"
        return 2
    fi
    grep -qE "$PP_REGRESSION_SCOPE" <<< "$diff_files"
}
