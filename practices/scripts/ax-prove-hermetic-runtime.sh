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

echo "=== ax-prove-hermetic-runtime — ROUND 5 (P1-1 / P1-2 / P1-3) ==="

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
    if ! grep -q "HELPER_FUNCTION_INJECTED" "$WORK/$tag.log" || [ "$rc" -eq 0 ]; then
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

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "ax-prove-hermetic-runtime: FAIL — an inherited-runtime path is open" >&2
    exit 1
fi
echo "ax-prove-hermetic-runtime: PASS — a redirected git context, an exported git/cd/pwd/python3,"
echo "  and a tampered fingerprint helper (committed or not) are each refused by the live push"
echo "  gate; the unattacked control passes, and with the hermetic scrub neutered the redirection"
echo "  attack lands again — so the refusals are attributable to the fix."
exit 0
