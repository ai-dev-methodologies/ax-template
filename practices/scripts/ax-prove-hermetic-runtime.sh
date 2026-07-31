#!/usr/bin/env bash
# practices/scripts/ax-prove-hermetic-runtime.sh
#   FALSIFICATION PROOF for ROUND 5 (cross-family reviewer, 2026-07-30;
#   TD-2026-07-30-P1-hermetic-runtime): THE RATCHET MUST NOT INHERIT ITS OWN RUNTIME.
#
# Round 4 proved that the helper's own NAMES cannot be injected. Round 5 attacks one level down —
# the RUNTIME the gate is made of:
#     · the git REPOSITORY is chosen by the environment  (GIT_DIR / GIT_WORK_TREE …)
#     · the COMMANDS are chosen by the environment       (exported `git` / `cd` / `pwd` / `python3`)
#     · the gate's own IMPLEMENTATION is a mutable file  (tree_fingerprint.py rewritten to lie)
# Each is an INPUT, and an input a gate trusts must be authenticated at the moment of use.
#
# WHAT IT PROVES (every attack is run against the LIVE push gate, in a throwaway sandbox):
#   (A) GIT_DIR/GIT_WORK_TREE aimed at a CLEAN shadow checkout of the same commit, while the tree
#       being pushed is DIRTY and the record claims it was clean       → the push gate REFUSES.
#       Attributable: the same sandbox with the hermetic scrub NEUTERED accepts it, so the scrub
#       is what refuses — not some incidental property of the sandbox.
#   (B) an exported `git` function                                     → HELPER_FUNCTION_INJECTED
#   (C) an exported `cd` + `pwd` pair                                  → HELPER_FUNCTION_INJECTED
#   (D) an exported `python3` that returns 0 — measured pre-fix to turn a FAILING recency guard
#       into exit 0, i.e. an entire python gate skipped              → HELPER_FUNCTION_INJECTED
#   (E1) tree_fingerprint.py rewritten to print a constant, UNCOMMITTED → RATCHET_TOOLCHAIN_MODIFIED
#   (E2) the same tamper COMMITTED, so the toolchain matches HEAD: the recompute is performed with
#        the PREVIOUS RELEASE's copy of the helper, so the lie still does not survive
#                                                                      → AUDIT_FINGERPRINT_*
#   (F) NEGATIVE CONTROL: the same sandbox with no attack at all PASSES (exit 0) and reaches NONE
#       of those codes — which is what makes (A)-(E) attributable to the attacks.
#
# ROUND 6 (TD-2026-07-30-P1-preflight-and-raw-bytes) adds the four classes the reviewer opened
# next. Two invariants name them: (alpha) NOTHING OVERRIDABLE MAY EXECUTE BEFORE THE SCRUB THAT
# DETECTS OVERRIDES; (beta) ANY CLAIM ABOUT BYTES MUST BE MADE ON RAW BYTES.
#   (G) BASH_FUNC_set / BASH_FUNC_[ injected through the ENVIRONMENT — the two commands round 5
#       ran BEFORE its own scrub                                    → HERMETIC_PREFLIGHT_HOSTILE
#       (G') the same attack with the round-6 preflight removed lands again (exit 0).
#   (H) a symlink named python3 pointing at /usr/bin/true, which satisfied every -f/-x/absolute
#       test and skipped the whole python body                      → HERMETIC_TOOL_UNAUTHENTIC
#       (H') the same attack with the identity self-report removed lands again.
#   (I) PYTHONPATH → sitecustomize.py calling os._exit(0)           → the gate reaches its honest
#       non-zero verdict anyway (scrubbed env + `-I -S`).
#   (J) a `filter.<n>.clean` that echoes the committed copy, so a TAMPERED toolchain file reads
#       byte-identical through git                                  → GIT_FILTERS_PRESENT /
#       RATCHET_TOOLCHAIN_MODIFIED, and (J') with the raw-byte comparison reverted to
#       `git diff --quiet` the tamper is accepted again (rc 0).
#   (K) a COMMITTED push-only bypass inside the recency guard (`[ -n "$EXPECT_SHA" ] && exit 0`),
#       which every HEAD-anchored self-check certifies because a committed backdoor IS HEAD
#                                                                   → RATCHET_PRIOR_RELEASE_GUARD_FAILED
#       (K') with the prior-release re-run removed the bypass passes the hook again (exit 0).
#
# Nothing outside the throwaway directory is touched; the live tree is only ever READ.
# Exit: 0 all attacks blocked · 1 at least one attack open · 2 harness error.
set -uo pipefail

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd -P)"

RECENCY_REL="practices/evals/completion_checklist_recency_guard.sh"
FP_REL="practices/scripts/lib/tree_fingerprint.py"
COPY_RELS=(
    "$RECENCY_REL"
    "$FP_REL"
    "practices/scripts/lib/release_anchor.sh"
    "practices/scripts/verify-completion.sh"
    "practices/scripts/ax-ledger-log.sh"
    "practices/evals/evidence_quote_spotcheck_guard.sh"
    "practices/evals/manifest_snapshot_integrity_guard.sh"
    "practices/evals/run-all-guards.sh"
    ".githooks/pre-push"
    ".githooks/pre-push-lib.sh"
)
for rel in "${COPY_RELS[@]}"; do
    [ -f "$REPO_ROOT/$rel" ] || { echo "ax-prove-hermetic-runtime: missing $rel" >&2; exit 2; }
done
command -v git >/dev/null 2>&1 || { echo "ax-prove-hermetic-runtime: git required" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAIL=0
note() { echo "  $*"; }
violation() { echo "  VIOLATION: $*" >&2; FAIL=1; }

GIT_ID=(-c user.email=ax@example.invalid -c user.name=ax)

# prefix_neuter <sb> — rebuild the sandbox's copies as the PRE-ROUND-5 shape: the hermetic
# scrubs, the git-context binding and the independent status read are removed. Every anchor is
# asserted to occur exactly once, so this goes STALE LOUDLY rather than silently proving nothing.
prefix_neuter() {
    python3 - "$1/repo/practices/evals/completion_checklist_recency_guard.sh" \
              "$1/repo/practices/scripts/lib/tree_fingerprint.py" <<'PY'
import sys
guard, fp = sys.argv[1], sys.argv[2]
edits = {
    guard: [
        ('for _ax_hn in ${!GIT_@}; do unset "$_ax_hn" 2>/dev/null || true; done',
         ': # PRE-ROUND-5: the GIT_* family was inherited'),
        ('unset BASH_ENV ENV GIT_DIR GIT_WORK_TREE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY GIT_INDEX_FILE \\',
         'unset BASH_ENV ENV \\'),
        ('GIT_ENV = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}',
         'GIT_ENV = dict(os.environ)'),
        ('    live_git_root = os.path.realpath(top_out) == os.path.realpath(str(root))',
         '    live_git_root = True'),
        ('    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then',
         '    if false; then'),
        ('    if st_out:', '    if False:'),
    ],
    fp: [
        ('    env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}',
         '    env = dict(os.environ)'),
        ('    if not top or os.path.realpath(top) != os.path.realpath(repo):', '    if False:'),
    ],
}
for path, pairs in edits.items():
    text = open(path, encoding="utf-8").read()
    for anchor, value in pairs:
        n = text.count(anchor)
        if n != 1:
            print(f"anchor occurs {n}x (expected 1) in {path}: {anchor[:60]!r}", file=sys.stderr)
            sys.exit(3)
        text = text.replace(anchor, value)
    open(path, "w", encoding="utf-8").write(text)
PY
}

# build_sb <dir> [prefix] — a git work tree carrying the LIVE gate, with an honest origin/main, plus the
# audit artifacts a genuine R25 run would have left. The audit line is written by this harness
# (that is the point: it is an ordinary text file) but every value in it is HONEST, so the
# unattacked control PASSES and each attack is the only difference.
build_sb() {
    local sb="$1" prefix="${2:-}" fp head
    mkdir -p "$sb/repo" || return 2
    ( builtin cd "$sb/repo" && git init -q . ) >/dev/null 2>&1 || return 2
    local rel
    for rel in "${COPY_RELS[@]}"; do
        mkdir -p "$sb/repo/$(dirname "$rel")" || return 2
        cp "$REPO_ROOT/$rel" "$sb/repo/$rel" || return 2
    done
    # A no-op gradle wrapper so the push hook's regression STAGE can complete in the sandbox: the
    # thing under test is the R25 stage above it, and a missing backend/ would make every hook run
    # exit 1 for a reason that has nothing to do with the attack (ROUND 6).
    mkdir -p "$sb/repo/backend" || return 2
    printf '#!/bin/sh\nexit 0\n' > "$sb/repo/backend/gradlew" || return 2
    chmod +x "$sb/repo/backend/gradlew" || return 2
    # The PRE-fix shape is committed as the sandbox's whole history, so the "must reproduce" probe
    # is a genuine pre-round-5 world rather than a live tree with a patch on top.
    if [ -n "$prefix" ]; then prefix_neuter "$sb" || return 3; fi
    ( builtin cd "$sb/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "gate" ) >/dev/null 2>&1 || return 2
    # an origin/main that HAS this state — the anchor the recompute reads its implementation from
    git init -q --bare "$sb/remote.git" >/dev/null 2>&1 || return 2
    ( builtin cd "$sb/repo" && git remote add origin "$sb/remote.git" \
      && git push -q origin HEAD:refs/heads/main && git fetch -q origin ) >/dev/null 2>&1 || return 2
    ( builtin cd "$sb/repo" && git update-ref refs/remotes/origin/main HEAD ) >/dev/null 2>&1 || return 2
    return 0
}

# write_audit <sb> [fingerprint-override]
write_audit() {
    local sb="$1" fp_override="${2:-}" head anchor fp
    head="$(git -C "$sb/repo" rev-parse HEAD)"
    anchor="$(git -C "$sb/repo" rev-parse refs/remotes/origin/main)"
    fp="${fp_override:-$(python3 "$sb/repo/$FP_REL" "$sb/repo" 2>/dev/null)}"
    mkdir -p "$sb/repo/.ax-verify"
    printf '{"ts":"2026-07-30T00:00:00Z","head_sha":"%s","exit":0,"pass":1,"warn_advisory":0,"hard_fail":0,"skip":0,"full_run":true,"tree_fingerprint":"%s","tree_clean":true,"head_sha_end":"%s","tree_fingerprint_end":"%s","tree_clean_end":true,"tree_stable":true,"tree_samples":3,"anchor_sha":"%s","anchor_kind":"origin/main","anchor_sha_end":"%s","anchor_stable":true}\n' \
        "$head" "$fp" "$head" "$fp" "$anchor" "$anchor" > "$sb/repo/.ax-verify/runs.jsonl"
    printf '{"step_id":"gate","status":"PASS","head_sha":"%s","tree_fingerprint":"%s"}\n' \
        "$head" "$fp" > "$sb/repo/.ax-verify/last_run.jsonl"
    # .ax-verify must not itself make the tree dirty — the gate now reads `git status` directly.
    printf '.ax-verify/\n' > "$sb/repo/.gitignore"
    ( builtin cd "$sb/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "ignore audit dir" ) >/dev/null 2>&1
    # rewrite with the post-commit head/fingerprint
    head="$(git -C "$sb/repo" rev-parse HEAD)"
    fp="${fp_override:-$(python3 "$sb/repo/$FP_REL" "$sb/repo" 2>/dev/null)}"
    printf '{"ts":"2026-07-30T00:00:00Z","head_sha":"%s","exit":0,"pass":1,"warn_advisory":0,"hard_fail":0,"skip":0,"full_run":true,"tree_fingerprint":"%s","tree_clean":true,"head_sha_end":"%s","tree_fingerprint_end":"%s","tree_clean_end":true,"tree_stable":true,"tree_samples":3,"anchor_sha":"%s","anchor_kind":"origin/main","anchor_sha_end":"%s","anchor_stable":true}\n' \
        "$head" "$fp" "$head" "$fp" "$anchor" "$anchor" > "$sb/repo/.ax-verify/runs.jsonl"
    printf '{"step_id":"gate","status":"PASS","head_sha":"%s","tree_fingerprint":"%s"}\n' \
        "$head" "$fp" > "$sb/repo/.ax-verify/last_run.jsonl"
}

run_gate() {   # run_gate <sb> <log> [env assignments...] — the LIVE recency guard, as the hook calls it
    local sb="$1" log="$2"; shift 2
    ( builtin cd "$sb/repo" && env "$@" bash "$sb/repo/$RECENCY_REL" ) > "$log" 2>&1
}

# ── ROUND 6 neuters. Same contract as prefix_neuter: every anchor must occur EXACTLY ONCE, so a
# refactor makes this harness fail loudly instead of silently proving nothing.
round6_neuter() {   # round6_neuter <sb> <what>   what ∈ preflight|identity|rawbytes|priorrelease
    "${AX_PY_BIN:-python3}" - "$1/repo" "$2" <<'PY'
import sys, pathlib
repo, what = pathlib.Path(sys.argv[1]), sys.argv[2]
GUARD = repo / "practices/evals/completion_checklist_recency_guard.sh"
ANCHOR = repo / "practices/scripts/lib/release_anchor.sh"
HOOK = repo / ".githooks/pre-push"
edits = {
    # (G') remove the pure-keyword preflight: the round-5 world, where `set -uo pipefail` and the
    #      bootstrap's own `[ … ]` executed before anything looked at the runtime.
    "preflight": [(GUARD, [('_AX_PF_ENV="$(/usr/bin/env)"', '_AX_PF_ENV="AX_NEUTERED"')])],
    # (H') remove the interpreter self-report: back to "absolute + -x is good enough".
    "identity": [(GUARD, [('            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;',
                           '            *) AX_PY_BIN="$_ax_hb" ;;\n'
                           '            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;')])],
    # (J') revert the toolchain byte comparison to the filter-honouring `git diff --quiet`.
    # A MINIMAL anchor on purpose: the surrounding lines legitimately move (the absent-on-both-
    # sides exemption landed between rounds and broke a multi-line anchor). This one line IS the
    # comparison — reverting it to the filter-honouring `git diff --quiet` is the pre-round-6 world.
    "rawbytes": [(ANCHOR, [
        ('    ax_ratchet_filters_absent "$repo" "$label" "$@" || bad=1', '    :'),
        ('        [ -n "$want" ] && [ "$want" = "$have" ] && continue',
         "\n".join([
            '        ax_git "$repo" diff --quiet "$rev" -- "$rel" >/dev/null 2>&1',
            '        rc=$?',
            '        [ "$rc" -eq 0 ] && continue']))])],
    # (K') remove the prior-release re-run from the hook.
    "priorrelease": [(HOOK, [('    pp_anchor_recency_gate "$ANCHOR_REV" "$local_sha" || exit 1',
                              '    : # PRE-ROUND-6: the hook trusted the tree copy of the guard')])],
}
for path, pairs in edits[what]:
    text = path.read_text(encoding="utf-8")
    for a, b in pairs:
        n = text.count(a)
        if n != 1:
            print(f"neuter anchor occurs {n}x (expected 1) in {path}: {a[:60]!r}", file=sys.stderr)
            sys.exit(3)
        text = text.replace(a, b)
    path.write_text(text, encoding="utf-8")
PY
    local rc=$?
    [ "$rc" -ne 0 ] && return "$rc"
    # The pre-round-6 shape is COMMITTED and published as the sandbox's origin/main, so the
    # sandbox is a self-consistent OLD WORLD rather than a new world with an uncommitted edit —
    # otherwise the toolchain-authenticity check fires on the neuter itself and the reproduction
    # would be measuring the wrong thing.
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "pre-round-6" \
      && git update-ref refs/remotes/origin/main HEAD \
      && git push -q -f origin HEAD:refs/heads/main ) >/dev/null 2>&1
    return 0
}

# A sandbox whose HONEST verdict is NON-ZERO: the audit log is removed, so the gate must say
# AUDIT_LOG_MISSING. That is the only way to measure an attack whose payload is "make the gate
# exit 0" — against a passing control, success and subversion are the same number.
build_sb_red() {
    local sb="$1" neuter="${2:-}"
    build_sb "$sb" || return 2
    write_audit "$sb"
    [ -n "$neuter" ] && { round6_neuter "$sb" "$neuter" || return 3; }
    rm -f "$sb/repo/.ax-verify/runs.jsonl" "$sb/repo/.ax-verify/last_run.jsonl"
    return 0
}

echo "=== ax-prove-hermetic-runtime — ROUND 5 (P1-1 / P1-2 / P1-3) + ROUND 6 (preflight / raw bytes) ==="

# ── (F) NEGATIVE CONTROL, first: an unattacked sandbox must actually PASS ─────────────
SB_OK="$WORK/control"; build_sb "$SB_OK" || { echo "harness setup failed (control)" >&2; exit 2; }
write_audit "$SB_OK"
run_gate "$SB_OK" "$WORK/f.log"; F_RC=$?
note "(F) negative control (no attack)          : exit=$F_RC (want 0)"
if [ "$F_RC" -ne 0 ]; then
    violation "the UNATTACKED sandbox does not pass, so every 'blocked' below could be an artefact" \
              "of the harness rather than of the attack. Fix the harness, not the gate."
    head -3 "$WORK/f.log" >&2
    echo "ax-prove-hermetic-runtime: FAIL — control broken" >&2
    exit 1
fi
if grep -qE "GIT_CONTEXT_REDIRECTED|HELPER_FUNCTION_INJECTED|RATCHET_TOOLCHAIN_MODIFIED|AUDIT_TREE_DIRTY_NOW|AUDIT_FINGERPRINT" "$WORK/f.log"; then
    violation "the unattacked control already reports one of the attack codes."
fi

# ── (A) GIT CONTEXT REDIRECTION ──────────────────────────────────────────────────────
# The tree being audited is DIRTY; a CLEAN shadow checkout of the same commit is handed to git
# through the environment. Pre-fix, every read (status, fingerprint, toplevel) described the
# shadow, so the dirty tree certified itself clean.
SB_A="$WORK/redirect"; build_sb "$SB_A" || { echo "harness setup failed (A)" >&2; exit 2; }
write_audit "$SB_A"
git clone -q "$SB_A/repo" "$SB_A/shadow" >/dev/null 2>&1 || { echo "harness setup failed (A/clone)" >&2; exit 2; }
printf 'uncommitted\n' > "$SB_A/repo/DIRTY.txt"          # the tree that will be pushed is dirty
run_gate "$SB_A" "$WORK/a.log" GIT_DIR="$SB_A/shadow/.git" GIT_WORK_TREE="$SB_A/shadow"; A_RC=$?
note "(A) GIT_DIR/GIT_WORK_TREE → clean shadow  : exit=$A_RC (want non-zero)"
if [ "$A_RC" -eq 0 ]; then
    violation "a DIRTY tree certified itself through a redirected git context (exit 0). Every" \
              "head/status/fingerprint answer then describes a repository nobody is pushing."
    head -3 "$WORK/a.log" >&2
fi
# Attributability: neuter the two scrubs (shell + python) in a COPY and the same attack lands.
SB_A2="$WORK/redirect-prefix"
build_sb "$SB_A2" prefix || { echo "harness setup failed (A2 — neuter anchors stale?)" >&2; exit 2; }
write_audit "$SB_A2"
git clone -q "$SB_A2/repo" "$SB_A2/shadow" >/dev/null 2>&1
printf 'uncommitted\n' > "$SB_A2/repo/DIRTY.txt"
run_gate "$SB_A2" "$WORK/a2.log" GIT_DIR="$SB_A2/shadow/.git" GIT_WORK_TREE="$SB_A2/shadow"; A2_RC=$?
note "(A') same attack, ROUND-5 additions removed: exit=$A2_RC (want 0 = reproduces)"
if [ "$A2_RC" -ne 0 ]; then
    violation "in a pre-round-5 sandbox the attack did NOT reproduce (exit=$A2_RC), so (A) proves" \
              "nothing about the round-5 additions — something else is doing the blocking, and" \
              "this harness has gone stale. Fix the harness, not the gate."
    head -3 "$WORK/a2.log" >&2
fi

# ── (B)/(C)/(D) EXPORTED FUNCTION INJECTION THROUGH COMMAND DEPENDENCIES ─────────────
inject_case() {   # inject_case <tag> <label> <attack-body>
    local tag="$1" label="$2" body="$3" rc
    local sb="$WORK/inject-$tag"
    build_sb "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    write_audit "$sb"
    { printf 'set -uo pipefail\n'; printf '%s\n' "$body"; printf 'builtin cd "$1" && bash "%s"\n' "$RECENCY_REL"; } \
        > "$WORK/attack-$tag.sh"
    bash "$WORK/attack-$tag.sh" "$sb/repo" > "$WORK/$tag.log" 2>&1; rc=$?
    note "($tag) $label: exit=$rc"
    # ROUND 6: the pure-keyword preflight refuses ANY exported function BEFORE the round-5
    # dependency-list scrub can name it, so these cases now report HERMETIC_PREFLIGHT_HOSTILE.
    # Either code is a refusal; what must never happen is exit 0.
    if ! grep -qE "HELPER_FUNCTION_INJECTED|HERMETIC_PREFLIGHT_HOSTILE" "$WORK/$tag.log" || [ "$rc" -eq 0 ]; then
        violation "an exported function named after a command the gate INVOKES was not refused" \
                  "(exit=$rc). bash imports exported functions across \`bash script.sh\`, so the" \
                  "caller supplies the program every check below runs."
        head -3 "$WORK/$tag.log" >&2
    fi
}
inject_case B "exported \`git\`                      " \
    'git() { case " $* " in *"rev-parse"*) echo 0000000000000000000000000000000000000000; return 0 ;; esac; command git "$@"; }
export -f git'
inject_case C "exported \`cd\` + \`pwd\`               " \
    'cd() { builtin cd "$@" 2>/dev/null || return 0; }
pwd() { echo /foreign/repo; }
export -f cd pwd'
inject_case D "exported \`python3\` returning 0       " \
    'python3() { return 0; }
export -f python3'

# ── (E1) THE FINGERPRINT HELPER, TAMPERED AND UNCOMMITTED ────────────────────────────
SB_E1="$WORK/tamper-uncommitted"; build_sb "$SB_E1" || { echo "harness setup failed (E1)" >&2; exit 2; }
write_audit "$SB_E1"
printf '#!/usr/bin/env python3\nprint("nogit")\n' > "$SB_E1/repo/$FP_REL"
run_gate "$SB_E1" "$WORK/e1.log"; E1_RC=$?
note "(E1) helper rewritten to lie, uncommitted : exit=$E1_RC (want non-zero)"
if [ "$E1_RC" -eq 0 ] || ! grep -q "RATCHET_TOOLCHAIN_MODIFIED" "$WORK/e1.log"; then
    violation "an uncommitted rewrite of tree_fingerprint.py was accepted (exit=$E1_RC). Its MODE" \
              "is unchanged — it is still a regular file — which is all the pre-round-5 checks" \
              "asked. \`print(\"nogit\")\` then disabled the recompute entirely."
    head -3 "$WORK/e1.log" >&2
fi

# ── (E2) THE SAME TAMPER, COMMITTED ─────────────────────────────────────────────────
# Now the working tree matches HEAD, so (E1)'s check is satisfied. The recompute is performed with
# the copy of the helper carried by the PREVIOUS RELEASE (origin/main), which is not the tampered
# one — the verifier no longer shares the runner's implementation.
SB_E2="$WORK/tamper-committed"; build_sb "$SB_E2" || { echo "harness setup failed (E2)" >&2; exit 2; }
write_audit "$SB_E2"
printf '#!/usr/bin/env python3\nimport sys\nprint("%s")\n' \
    "$(python3 -c 'print("d"*64)')" > "$SB_E2/repo/$FP_REL"
( builtin cd "$SB_E2/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "tamper" ) >/dev/null 2>&1
# re-point the record at the new head, still claiming the constant the tampered helper prints
write_audit "$SB_E2" "$(python3 -c 'print("d"*64)')"
run_gate "$SB_E2" "$WORK/e2.log"; E2_RC=$?
note "(E2) same tamper, COMMITTED               : exit=$E2_RC (want non-zero)"
if [ "$E2_RC" -eq 0 ] || ! grep -qE "AUDIT_FINGERPRINT_(MISMATCH|UNVERIFIABLE)" "$WORK/e2.log"; then
    violation "a COMMITTED rewrite of tree_fingerprint.py made the gate recompute the very" \
              "constant the record claims (exit=$E2_RC): the writer and the verifier were the" \
              "same compromised implementation. The recompute must use the previous release's copy."
    head -3 "$WORK/e2.log" >&2
fi

# ══ ROUND 6 ═══════════════════════════════════════════════════════════════════════════
# ── (G) EXPORTED COMMANDS THAT RAN BEFORE THE SCRUB (invariant alpha) ────────────────
preflight_case() {   # preflight_case <tag> <envvar> <label>
    local tag="$1" var="$2" label="$3" rc
    local sb="$WORK/pf-$tag"
    build_sb_red "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    ( builtin cd "$sb/repo" && env "$var" bash "$sb/repo/$RECENCY_REL" ) > "$WORK/$tag.log" 2>&1; rc=$?
    note "(G/$tag) $label: exit=$rc (want non-zero)"
    if [ "$rc" -eq 0 ] || ! grep -q "HERMETIC_PREFLIGHT_HOSTILE" "$WORK/$tag.log"; then
        violation "an exported \`${tag}\` survived into the gate (exit=$rc). It is invoked BEFORE the" \
                  "round-5 scrub could look at it, so the scrub's dependency list is irrelevant."
        head -3 "$WORK/$tag.log" >&2
    fi
}
preflight_case set 'BASH_FUNC_set%%=() { exit 0; }' "exported \`set\`  (round-5: pre-push exit 0)  "
preflight_case bracket 'BASH_FUNC_[%%=() { exit 0; }' "exported \`[\`    (round-5: guard exit 0)    "
SB_G2="$WORK/pf-neutered"; build_sb_red "$SB_G2" preflight || { echo "harness setup failed (G')" >&2; exit 2; }
( builtin cd "$SB_G2/repo" && env 'BASH_FUNC_set%%=() { exit 0; }' bash "$SB_G2/repo/$RECENCY_REL" ) \
    > "$WORK/g2.log" 2>&1; G2_RC=$?
note "(G') same attack, ROUND-6 preflight removed: exit=$G2_RC (want 0 = reproduces)"
if [ "$G2_RC" -ne 0 ]; then
    violation "in a pre-round-6 sandbox the exported-\`set\` attack did NOT reproduce (exit=$G2_RC)," \
              "so (G) proves nothing about the preflight. The harness has gone stale."
fi

# ── (H) A PATH IS NOT AN IDENTITY ───────────────────────────────────────────────────
mkdir -p "$WORK/fakebin"
ln -sf /usr/bin/true "$WORK/fakebin/python3" 2>/dev/null || cp /usr/bin/true "$WORK/fakebin/python3"
SB_H="$WORK/py-true"; build_sb_red "$SB_H" || { echo "harness setup failed (H)" >&2; exit 2; }
run_gate "$SB_H" "$WORK/h.log" PATH="$WORK/fakebin:$PATH"; H_RC=$?
note "(H) python3 → /usr/bin/true symlink       : exit=$H_RC (want non-zero)"
if [ "$H_RC" -eq 0 ] || ! grep -q "HERMETIC_TOOL_UNAUTHENTIC" "$WORK/h.log"; then
    violation "a symlink named python3 pointing at /usr/bin/true was accepted as the interpreter" \
              "(exit=$H_RC). -f/-x FOLLOW SYMLINKS; only running the program identifies it."
    head -3 "$WORK/h.log" >&2
fi
SB_H2="$WORK/py-true-neutered"; build_sb_red "$SB_H2" identity || { echo "harness setup failed (H')" >&2; exit 2; }
run_gate "$SB_H2" "$WORK/h2.log" PATH="$WORK/fakebin:$PATH"; H2_RC=$?
note "(H') same attack, self-report removed      : exit=$H2_RC (want 0 = reproduces)"
if [ "$H2_RC" -ne 0 ]; then
    violation "the /usr/bin/true interpreter did NOT reproduce with the identity check removed" \
              "(exit=$H2_RC); (H) is therefore not attributable to it. Harness stale."
fi

# ── (I) PYTHON STARTUP HOOK ─────────────────────────────────────────────────────────
mkdir -p "$WORK/pyhook"; printf 'import os\nos._exit(0)\n' > "$WORK/pyhook/sitecustomize.py"
SB_I="$WORK/pyhook-sb"; build_sb_red "$SB_I" || { echo "harness setup failed (I)" >&2; exit 2; }
run_gate "$SB_I" "$WORK/i.log" PYTHONPATH="$WORK/pyhook"; I_RC=$?
note "(I) PYTHONPATH sitecustomize os._exit(0)  : exit=$I_RC (want non-zero)"
if [ "$I_RC" -eq 0 ] || ! grep -q "AUDIT_LOG_MISSING" "$WORK/i.log"; then
    violation "a sitecustomize.py reached via PYTHONPATH silenced the gate (exit=$I_RC): the whole" \
              "python body is skipped before it runs a line of ours."
    head -3 "$WORK/i.log" >&2
fi

# ── (J) BYTE CLAIMS THROUGH A CLEAN FILTER (invariant beta) ─────────────────────────
SB_J="$WORK/filter"; build_sb "$SB_J" || { echo "harness setup failed (J)" >&2; exit 2; }
git -C "$SB_J/repo" show "HEAD:$FP_REL" > "$SB_J/committed_fp.py" 2>/dev/null
git -C "$SB_J/repo" config filter.axmask.clean "cat $SB_J/committed_fp.py"
mkdir -p "$SB_J/repo/.git/info"; printf '%s filter=axmask\n' "$FP_REL" > "$SB_J/repo/.git/info/attributes"
printf '\n# TAMPERED — attacker payload\n' >> "$SB_J/repo/$FP_REL"
( builtin cd "$SB_J/repo" && bash -c 'source practices/scripts/lib/release_anchor.sh
ax_ratchet_toolchain_authentic "$PWD" probe HEAD $(ax_ratchet_toolchain_paths)' ) > "$WORK/j.log" 2>&1; J_RC=$?
note "(J) clean filter masks a tampered file    : exit=$J_RC (want non-zero)"
if [ "$J_RC" -eq 0 ] || ! grep -qE "GIT_FILTERS_PRESENT|RATCHET_TOOLCHAIN_MODIFIED" "$WORK/j.log"; then
    violation "a \`filter.<n>.clean\` that echoes the committed copy made a TAMPERED toolchain file" \
              "compare byte-identical to its blob (exit=$J_RC). Byte claims must be raw."
    head -3 "$WORK/j.log" >&2
fi
SB_J2="$WORK/filter-neutered"; build_sb "$SB_J2" || { echo "harness setup failed (J')" >&2; exit 2; }
round6_neuter "$SB_J2" rawbytes || { echo "harness setup failed (J'/neuter stale)" >&2; exit 2; }
git -C "$SB_J2/repo" show "HEAD:$FP_REL" > "$SB_J2/committed_fp.py" 2>/dev/null
git -C "$SB_J2/repo" config filter.axmask.clean "cat $SB_J2/committed_fp.py"
mkdir -p "$SB_J2/repo/.git/info"; printf '%s filter=axmask\n' "$FP_REL" > "$SB_J2/repo/.git/info/attributes"
printf '\n# TAMPERED — attacker payload\n' >> "$SB_J2/repo/$FP_REL"
( builtin cd "$SB_J2/repo" && bash -c 'source practices/scripts/lib/release_anchor.sh
ax_ratchet_toolchain_authentic "$PWD" probe HEAD $(ax_ratchet_toolchain_paths)' ) > "$WORK/j2.log" 2>&1; J2_RC=$?
note "(J') same tamper, raw comparison reverted  : exit=$J2_RC (want 0 = reproduces)"
if [ "$J2_RC" -ne 0 ]; then
    violation "with the byte comparison reverted to \`git diff --quiet\` the filter mask did NOT" \
              "reproduce (exit=$J2_RC); (J) is not attributable to the raw-byte fix. Harness stale."
    head -3 "$WORK/j2.log" >&2
fi

# ── (K) A COMMITTED PUSH-ONLY BYPASS (the self-authentication circle) ────────────────
backdoor() {   # backdoor <sb> — commit the reviewer's push-only bypass into the tree's guard
    "${AX_PY_BIN:-python3}" - "$1/repo/$RECENCY_REL" <<'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]); s = p.read_text(encoding="utf-8")
needle = "\n".join([
    '        *) echo "completion_checklist_recency_guard: unknown arg: $1" >&2; exit 2 ;;',
    '    esac',
    'done',
    ''])
assert s.count(needle) == 1, "backdoor anchor stale"
patch = needle + "\n".join(['', 'if [ -n "$EXPECT_SHA" ]; then exit 0; fi', ''])
p.write_text(s.replace(needle, patch), encoding="utf-8")
PY
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "backdoor" ) >/dev/null 2>&1
}
push_spec() {   # push_spec <sb> — the stdin git feeds the hook for a push that advances main
    printf 'refs/heads/main %s refs/heads/main %s
' \
        "$(git -C "$1/repo" rev-parse HEAD)" "$(git -C "$1/repo" rev-parse refs/remotes/origin/main)"
}
SB_K="$WORK/backdoor"; build_sb "$SB_K" || { echo "harness setup failed (K)" >&2; exit 2; }
write_audit "$SB_K"
backdoor "$SB_K" || { echo "harness setup failed (K/anchor stale)" >&2; exit 2; }
push_spec "$SB_K" | ( builtin cd "$SB_K/repo" && bash "$SB_K/repo/.githooks/pre-push" origin "$SB_K/repo" ) \
    > "$WORK/k.log" 2>&1; K_RC=$?
note "(K) COMMITTED push-only bypass in the guard: exit=$K_RC (want non-zero)"
if [ "$K_RC" -eq 0 ] || ! grep -q "RATCHET_PRIOR_RELEASE_GUARD_FAILED" "$WORK/k.log"; then
    violation "a COMMITTED \`[ -n \"\$EXPECT_SHA\" ] && exit 0\` inside the recency guard passed the" \
              "push gate (exit=$K_RC). Every self-check anchored on HEAD certifies it, because a" \
              "committed backdoor IS HEAD; the prior release's copy must be the one that runs."
    head -5 "$WORK/k.log" >&2
fi
SB_K2="$WORK/backdoor-neutered"; build_sb "$SB_K2" || { echo "harness setup failed (K')" >&2; exit 2; }
write_audit "$SB_K2"
round6_neuter "$SB_K2" priorrelease || { echo "harness setup failed (K'/neuter stale)" >&2; exit 2; }
backdoor "$SB_K2" || { echo "harness setup failed (K'/anchor stale)" >&2; exit 2; }
push_spec "$SB_K2" | ( builtin cd "$SB_K2/repo" && bash "$SB_K2/repo/.githooks/pre-push" origin "$SB_K2/repo" ) \
    > "$WORK/k2.log" 2>&1; K2_RC=$?
note "(K') same bypass, prior-release re-run gone: exit=$K2_RC (want 0 = reproduces)"
if [ "$K2_RC" -ne 0 ]; then
    violation "with the prior-release re-run removed the committed bypass did NOT reproduce" \
              "(exit=$K2_RC); (K) is not attributable to it. Harness stale."
    head -5 "$WORK/k2.log" >&2
fi

# ── (L) POSITIVE CONTROL FOR THE NEW PUSH PATH ──────────────────────────────────────
# (K) shows the prior-release re-run REFUSING. This shows it AGREEING on an honest tree — without
# which "it blocks" could just mean "it always blocks", and the gate would be unshippable.
SB_L="$WORK/push-ok"; build_sb "$SB_L" || { echo "harness setup failed (L)" >&2; exit 2; }
write_audit "$SB_L"
push_spec "$SB_L" | ( builtin cd "$SB_L/repo" && bash "$SB_L/repo/.githooks/pre-push" origin "$SB_L/repo" ) \
    > "$WORK/l.log" 2>&1; L_RC=$?
note "(L) honest push through the FULL hook     : exit=$L_RC (want 0)"
if [ "$L_RC" -ne 0 ] || ! grep -q "previous release's recency guard also PASSES" "$WORK/l.log"; then
    violation "the round-6 prior-release re-run does not agree on an HONEST tree (exit=$L_RC), so" \
              "(K)'s refusal is not evidence of anything: a gate that always blocks blocks nothing."
    head -8 "$WORK/l.log" >&2
fi

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "ax-prove-hermetic-runtime: FAIL — an inherited-runtime path is open" >&2
    exit 1
fi
echo "ax-prove-hermetic-runtime: PASS — a redirected git context, an exported git/cd/pwd/python3,"
echo "  a tampered fingerprint helper (committed or not), an exported set/[ arriving before the"
echo "  scrub, a /usr/bin/true interpreter, a PYTHONPATH sitecustomize, a clean-filter byte mask"
echo "  and a COMMITTED push-only bypass are each refused by the live gates; the unattacked control"
echo "  passes, and every round-5/6 addition has a neutered twin in which its attack lands again —"
echo "  so the refusals are attributable to the fixes, not to the sandbox."
exit 0
