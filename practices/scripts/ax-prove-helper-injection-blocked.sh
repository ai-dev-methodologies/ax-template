#!/usr/bin/env bash
# practices/scripts/ax-prove-helper-injection-blocked.sh
#   FALSIFICATION PROOF for P1-3 (cross-family reviewer ROUND 4, 2026-07-30;
#   TD-2026-07-30-P1-anchor-runtime): THE SHARED HELPER IS AN INJECTABLE POLICY SURFACE.
#
# WHY A PROOF SCRIPT AND NOT A FIXTURE
#   The ratcheting guards' fixture mode takes a `--root` DIRECTORY. Two of the three sub-attacks
#   here are not properties of a directory at all:
#     (a) FUNCTION INJECTION lives in the ENVIRONMENT (bash imports exported functions across
#         `bash script.sh`), and
#     (c) HELPER DELETION is about which file the loader falls back to.
#   Only (b), the symlinked helper, is directory-shaped, and even that one has to be a symlink to
#   a REAL helper for the attack to be interesting. So the shell-testable proof is a scripted
#   scenario, in the same family as ax-prove-gate-blocks-agent.sh /
#   ax-prove-evidence-gate-blocks-agent.sh, and it is registered in run-all-guards.sh so it runs
#   inside R25's hard-guards step like everything else.
#
# WHAT IT PROVES (each attack is run against the LIVE guard, in a throwaway sandbox):
#   (a) an exported ax_anchor_* function (with a forged `_AX_RELEASE_ANCHOR_LIB=1` marker, which
#       is what made the old early-return hand the attacker the policy) → HELPER_FUNCTION_INJECTED
#   (b) practices/scripts/lib/release_anchor.sh replaced by a SYMLINK      → HELPER_PATH_NOT_REGULAR
#   (c) the helper DELETED while AX_RELEASE_ANCHOR_LIB points elsewhere    → RELEASE_ANCHOR_LIB_MISSING
#   (d) NEGATIVE CONTROL: the same sandbox with no attack reaches NONE of those three codes, so
#       the three results above are attributable to the attacks and not to the sandbox.
#   (e) PRE-FIX REPRODUCTION for (a): the round-3 helper (read from git, never written) accepts
#       the injected functions and reports the attacker's anchor — the attack was real.
#
# The sandbox is minimal on purpose: every check above fires BEFORE the guard reads any catalog
# content, so it needs only the guard, the helper and a git work tree. Nothing outside the
# throwaway directory is touched, and the live tree is only ever READ.
#
# Exit: 0 all attacks blocked · 1 at least one attack not blocked · 2 harness error.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
GUARD_REL="practices/evals/evidence_quote_spotcheck_guard.sh"
HELPER_REL="practices/scripts/lib/release_anchor.sh"

[ -f "$REPO_ROOT/$GUARD_REL" ]  || { echo "ax-prove-helper-injection: missing $GUARD_REL" >&2; exit 2; }
[ -f "$REPO_ROOT/$HELPER_REL" ] || { echo "ax-prove-helper-injection: missing $HELPER_REL" >&2; exit 2; }
command -v git >/dev/null 2>&1  || { echo "ax-prove-helper-injection: git required" >&2; exit 2; }

# BACKLOG P2-67: mktemp is resolved to an ABSOLUTE path (a PATH-earlier shim can return a
# directory the attacker owns — this catalog ships exactly such a shim in
# resume_provenance_guard.sh) and the returned directory is verified: a real directory, owned by
# this euid, with no group/other write. This harness builds SANDBOXES in it and runs gates there.
_AX_MK="$(PATH=/usr/bin:/bin:/usr/local/bin command -v mktemp 2>/dev/null || true)"
case "$_AX_MK" in /*) ;; *) echo "$(basename "$0"): mktemp did not resolve to an absolute path" >&2; exit 2 ;; esac
WORK="$("$_AX_MK" -d "${TMPDIR:-/tmp}/ax-prove.XXXXXXXX")"
_AX_ST="$(stat -f '%u %Lp' "$WORK" 2>/dev/null)" || _AX_ST=""
[ -n "$_AX_ST" ] || _AX_ST="$(stat -c '%u %a' "$WORK" 2>/dev/null)" || _AX_ST=""
case "$_AX_ST" in
    [0-9]*" "[0-7][0-7][0-7]|[0-9]*" "[0-7][0-7][0-7][0-7]) ;;
    *) echo "$(basename "$0"): the owner/mode of $WORK could not be read (stat said '${_AX_ST:-<nothing>}')" >&2; exit 2 ;;
esac
if [ ! -d "$WORK" ] || [ -L "$WORK" ] || [ "${_AX_ST%% *}" != "${EUID:-$(id -u)}" ] \
   || [ $(( 8#${_AX_ST##* } & 8#22 )) -ne 0 ]; then
    echo "$(basename "$0"): refusing a temp dir that is not a private directory owned by this uid ($WORK, stat '$_AX_ST')" >&2
    exit 2
fi
trap 'rm -rf "$WORK"' EXIT

FAIL=0
note() { echo "  $*"; }
violation() { echo "  VIOLATION: $*" >&2; FAIL=1; }

# build_sb <dir> — a minimal git work tree carrying the LIVE guard + the LIVE helper.
build_sb() {
    local sb="$1"
    mkdir -p "$sb/practices/evals" "$sb/practices/scripts/lib" || return 2
    cp "$REPO_ROOT/$GUARD_REL"  "$sb/practices/evals/"   || return 2
    cp "$REPO_ROOT/$HELPER_REL" "$sb/practices/scripts/lib/" || return 2
    ( cd "$sb" && git init -q . && git add -A \
      && git -c user.email=p@x -c user.name=p commit -q -m "sandbox" ) >/dev/null 2>&1 || return 2
    return 0
}

run_guard() {  # run_guard <sb> <logfile> [env assignments...] — returns the guard's exit code
    local sb="$1" log="$2"; shift 2
    ( cd "$sb" && env "$@" bash "$sb/$GUARD_REL" ) > "$log" 2>&1
}

echo "=== ax-prove-helper-injection-blocked — P1-3, ROUND 4 ==="

# ── OPENING THE CHANNEL THAT BACKLOG P2-70 CLOSED ────────────────────────────────────
# The subject of (a)/(a2) is an IN-SCRIPT refusal of an exported shell function. P2-70 gave this
# entry an `env -i` privileged re-exec with a measured allowlist, and `env -i` DELETES the
# BASH_FUNC_* entries from the environ — so the injected functions never reach the process and
# there is nothing in-script left to refuse. MEASURED when P2-70 landed: (a) went from a loud
# HERMETIC_PREFLIGHT_HOSTILE / non-zero exit to a clean exit 0 with the honest verdict.
# That is the invocation-boundary control WORKING (it is strictly stronger than refusing: the
# hostile definition is never in the process at all), but it would make the in-script checks
# unobservable — and an unobservable check rots, which is the exact failure this prover exists to
# prevent. So the two layers are proven SEPARATELY:
#   · (a) / (a2) keep proving the IN-SCRIPT refusals non-vacuous, on a sandbox copy whose
#     privileged block is reverted to the INHERITING form — i.e. the channel the three
#     inheriting entries (.githooks/pre-push, run-all-guards.sh, verify-completion.sh) still have,
#     and the channel every entry had before P2-70;
#   · (a3) proves the NEW control on the SHIPPED form: the same attack must be INERT.
# The revert is anchored and goes stale LOUDLY, exactly like the (a2) preflight strip.
open_env_channel() {   # <sandbox> — revert the guard copy's env -i re-exec to the inheriting form
    python3 - "$1/$GUARD_REL" <<'PYOPEN'
import sys, pathlib
p = pathlib.Path(sys.argv[1]); t = p.read_text(encoding="utf-8")
old = 'exec /usr/bin/env -i "${_AX_PV_ENV[@]}" "$BASH" -p "$0" "$@"'
new = 'exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@"'
n = t.count(old)
if n != 1:
    print("env -i re-exec anchor occurs %dx (expected 1) in %s" % (n, sys.argv[1]), file=sys.stderr)
    sys.exit(3)
p.write_text(t.replace(old, new), encoding="utf-8")
PYOPEN
}

# ── (a) FUNCTION INJECTION ───────────────────────────────────────────────────────────
SB_A="$WORK/inject"; build_sb "$SB_A" || { echo "harness setup failed (a)" >&2; exit 2; }
open_env_channel "$SB_A" || { echo "harness setup failed (a — env -i anchor stale)" >&2; exit 2; }
( cd "$SB_A" && git add -A && git -c user.email=p@x -c user.name=p commit -q -m "inheriting env" ) >/dev/null 2>&1
cat > "$WORK/attack_a.sh" <<'ATTACK'
set -uo pipefail
# The attacker's policy: no anchor at all, everything permitted. Under the round-3 helper this
# survived the source, because the idempotence guard returned BEFORE the definitions.
ax_anchor_resolve()               { AX_ANCHOR_REF=""; AX_ANCHOR_KIND="unavailable"; AX_ANCHOR_SHA="unavailable"; }
ax_anchor_check_ancestry()        { return 0; }
ax_anchor_release_paths_regular() { return 0; }
ax_anchor_worktree_paths_regular(){ return 0; }
export -f ax_anchor_resolve ax_anchor_check_ancestry \
          ax_anchor_release_paths_regular ax_anchor_worktree_paths_regular
export _AX_RELEASE_ANCHOR_LIB=1
cd "$1" && bash "$2"
ATTACK
bash "$WORK/attack_a.sh" "$SB_A" "$SB_A/$GUARD_REL" > "$WORK/a.log" 2>&1; A_RC=$?
note "(a) exported ax_anchor_* + forged marker : exit=$A_RC"
# ROUND 6: the pure-keyword preflight refuses ANY exported function before the round-4 namespace
# check can name it, so this case now reports HERMETIC_PREFLIGHT_HOSTILE. Either code is a
# refusal — and (a2) below keeps the round-4 check itself non-vacuous by removing the preflight.
if ! grep -qE "HELPER_FUNCTION_INJECTED|HERMETIC_PREFLIGHT_HOSTILE" "$WORK/a.log" || [ "$A_RC" -eq 0 ]; then
    violation "an exported ax_anchor_* function was NOT refused (exit=$A_RC). The helper is the" \
              "single decider for which commit the anchor is and whether an absence is honest;" \
              "if the caller can supply those functions it supplies the policy."
    head -5 "$WORK/a.log" >&2
fi

# ── (a2) THE ROUND-4 NAMESPACE CHECK, WITH THE ROUND-6 PREFLIGHT REMOVED ─────────────
# (a) is now answered by the round-6 preflight, which would make the round-4 check invisible —
# and an invisible check rots. Here the preflight is stripped from the sandbox's copies (the
# anchor is asserted to occur exactly once in each, so this goes stale LOUDLY), leaving the
# round-4 helper-namespace refusal as the only thing that can block the same attack.
SB_A2="$WORK/inject-nopreflight"; build_sb "$SB_A2" || { echo "harness setup failed (a2)" >&2; exit 2; }
open_env_channel "$SB_A2" || { echo "harness setup failed (a2 — env -i anchor stale)" >&2; exit 2; }
python3 - "$SB_A2/$GUARD_REL" "$SB_A2/$HELPER_REL" <<'PYSTRIP'
import sys, pathlib
anchor = '_AX_PF_ENV="$(/usr/bin/env)"'
for path in sys.argv[1:]:
    p = pathlib.Path(path); t = p.read_text(encoding="utf-8")
    n = t.count(anchor)
    if n != 1:
        print(f"preflight anchor occurs {n}x (expected 1) in {path}", file=sys.stderr)
        sys.exit(3)
    p.write_text(t.replace(anchor, '_AX_PF_ENV="AX_NEUTERED"'), encoding="utf-8")
PYSTRIP
if [ $? -ne 0 ]; then echo "harness setup failed (a2 — preflight anchor stale)" >&2; exit 2; fi
( cd "$SB_A2" && git add -A && git -c user.email=p@x -c user.name=p commit -q -m "no preflight" ) >/dev/null 2>&1
bash "$WORK/attack_a.sh" "$SB_A2" "$SB_A2/$GUARD_REL" > "$WORK/a2.log" 2>&1; A2_RC=$?
note "(a2) same attack, ROUND-6 preflight removed: exit=$A2_RC"
if ! grep -q "HELPER_FUNCTION_INJECTED" "$WORK/a2.log" || [ "$A2_RC" -eq 0 ]; then
    violation "with the round-6 preflight removed, the round-4 helper-NAMESPACE refusal did not" \
              "fire (exit=$A2_RC). (a) would then be proving only that the preflight works, and" \
              "the namespace check would be dead code nobody notices."
    head -5 "$WORK/a2.log" >&2
fi

# ── (a3) THE SHIPPED FORM: BACKLOG P2-70's env -i MUST MAKE THE SAME ATTACK INERT ────
# (a)/(a2) run against a sandbox whose privileged block was reverted to the inheriting form, so
# they prove the in-script refusals. THIS case runs the guard AS SHIPPED. The exported functions
# must never reach the process — so the expected outcome is not a refusal but a NORMAL, HONEST
# verdict: exit 0, no HELPER_FUNCTION_INJECTED/HERMETIC_PREFLIGHT_HOSTILE (there is nothing left
# to refuse), and — the load-bearing half — NO EVIDENCE THAT THE ATTACKER'S POLICY TOOK EFFECT.
# The attacker's ax_anchor_resolve sets AX_ANCHOR_KIND="unavailable"; a guard that had adopted it
# would report the anchor as unavailable and skip its ratchet. A guard that never saw it reports
# the real anchor. That distinction is what is asserted.
SB_A3="$WORK/inject-shipped"; build_sb "$SB_A3" || { echo "harness setup failed (a3)" >&2; exit 2; }
bash "$WORK/attack_a.sh" "$SB_A3" "$SB_A3/$GUARD_REL" > "$WORK/a3.log" 2>&1; A3_RC=$?
note "(a3) same attack vs the SHIPPED env -i form: exit=$A3_RC (want 0 = inert)"
if [ "$A3_RC" -ne 0 ]; then
    violation "the P2-70 env -i re-exec did not make the exported-function attack inert" \
              "(exit=$A3_RC). \`env -i\` deletes the BASH_FUNC_* entries, so this entry should" \
              "reach its honest verdict with nothing to refuse; a non-zero exit means either the" \
              "allowlist is missing something the guard needs, or the channel is still open."
    head -5 "$WORK/a3.log" >&2
elif grep -qE "HELPER_FUNCTION_INJECTED|HERMETIC_PREFLIGHT_HOSTILE" "$WORK/a3.log"; then
    violation "the exported functions REACHED the shipped form of this entry (its log carries an" \
              "injection/hostile-environment refusal). P2-70's allowlist re-exec is supposed to" \
              "delete BASH_FUNC_* before the process starts; if a refusal fired, it did not."
    head -5 "$WORK/a3.log" >&2
elif grep -q "ANCHOR_FALLBACK\|anchor ratchet OK" "$WORK/a3.log"; then
    note "     the guard resolved its own anchor — the attacker's ax_anchor_resolve never ran"
else
    violation "the shipped form exited 0 but its log shows no sign that the guard resolved its" \
              "OWN anchor, so this case cannot distinguish 'the attack was inert' from 'the guard" \
              "adopted the attacker's ax_anchor_resolve and skipped its ratchet'."
    head -5 "$WORK/a3.log" >&2
fi

# ── (e) PRE-FIX REPRODUCTION of (a) ──────────────────────────────────────────────────
# The round-3 helper, read out of git (never written to the tree). If the attack was not real
# there is nothing to have fixed, and this proof would be vacuous.
if git -C "$REPO_ROOT" cat-file -e "HEAD:$HELPER_REL" 2>/dev/null; then
    git -C "$REPO_ROOT" show "HEAD:$HELPER_REL" > "$WORK/helper_head.sh" 2>/dev/null
    cat > "$WORK/attack_e.sh" <<'ATTACK'
set -uo pipefail
ax_anchor_resolve() { AX_ANCHOR_KIND="ATTACKER"; AX_ANCHOR_SHA="ATTACKER"; }
export -f ax_anchor_resolve
export _AX_RELEASE_ANCHOR_LIB=1
bash -c 'set -uo pipefail; . "$1"; ax_anchor_resolve "$PWD"; echo "anchor=${AX_ANCHOR_KIND:-?}"' _ "$1"
ATTACK
    E_OUT="$(bash "$WORK/attack_e.sh" "$WORK/helper_head.sh" 2>&1)"; E_RC=$?
    if [ "$E_RC" -eq 0 ] && printf '%s' "$E_OUT" | grep -q "anchor=ATTACKER"; then
        note "(e) pre-fix (round-3 helper) reproduces  : exit=0, anchor=ATTACKER — attack was real"
    else
        note "(e) pre-fix reproduction inconclusive    : exit=$E_RC out=${E_OUT//$'\n'/ }"
        note "    (the committed helper at HEAD already blocks it — proof (a) still stands)"
    fi
else
    note "(e) pre-fix reproduction skipped          : no HEAD copy of the helper to read"
fi

# ── (b) SYMLINKED HELPER ─────────────────────────────────────────────────────────────
SB_B="$WORK/symlink"; build_sb "$SB_B" || { echo "harness setup failed (b)" >&2; exit 2; }
cp "$REPO_ROOT/$HELPER_REL" "$SB_B/practices/scripts/lib/real_helper.sh"
rm -f "$SB_B/$HELPER_REL"
ln -s "real_helper.sh" "$SB_B/$HELPER_REL"
run_guard "$SB_B" "$WORK/b.log" _AX_PROVE=1; B_RC=$?
note "(b) helper replaced by a symlink         : exit=$B_RC"
if ! grep -q "HELPER_PATH_NOT_REGULAR" "$WORK/b.log" || [ "$B_RC" -eq 0 ]; then
    violation "a SYMLINKED release_anchor.sh was NOT refused (exit=$B_RC). \`source\` follows the" \
              "link, so the bytes executed are whatever the target holds while git records a link" \
              "blob for that path — the same anchor-side/self-side asymmetry P1-Y closes."
    head -5 "$WORK/b.log" >&2
fi

# ── (c) DELETED HELPER + env override on a GIT root ──────────────────────────────────
SB_C="$WORK/deleted"; build_sb "$SB_C" || { echo "harness setup failed (c)" >&2; exit 2; }
cp "$REPO_ROOT/$HELPER_REL" "$WORK/elsewhere_helper.sh"
rm -f "$SB_C/$HELPER_REL"
run_guard "$SB_C" "$WORK/c.log" AX_RELEASE_ANCHOR_LIB="$WORK/elsewhere_helper.sh"; C_RC=$?
note "(c) helper deleted, AX_RELEASE_ANCHOR_LIB set : exit=$C_RC"
if ! grep -q "RELEASE_ANCHOR_LIB_MISSING" "$WORK/c.log" || [ "$C_RC" -eq 0 ]; then
    violation "on a GIT work tree a missing helper was satisfied from AX_RELEASE_ANCHOR_LIB" \
              "(exit=$C_RC). The override exists for the relocated-copy sandbox [87] only; a live" \
              "root that has lost its committed helper is a tampered tree, not a sandbox."
    head -5 "$WORK/c.log" >&2
fi

# ── (d) NEGATIVE CONTROL ─────────────────────────────────────────────────────────────
# The same sandbox, unattacked. It does NOT exit 0 — it has no catalog to scan — but it must not
# reach any of the three codes above, which is what makes (a)/(b)/(c) attributable.
SB_D="$WORK/control"; build_sb "$SB_D" || { echo "harness setup failed (d)" >&2; exit 2; }
run_guard "$SB_D" "$WORK/d.log" _AX_PROVE=1; D_RC=$?
note "(d) negative control (no attack)         : exit=$D_RC"
if grep -qE "HELPER_FUNCTION_INJECTED|HELPER_PATH_NOT_REGULAR|RELEASE_ANCHOR_LIB_MISSING" "$WORK/d.log"; then
    violation "the UNATTACKED sandbox already reports one of the three codes, so the results" \
              "above prove nothing about the attacks. Fix the harness."
    head -5 "$WORK/d.log" >&2
fi

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "ax-prove-helper-injection-blocked: FAIL — an injection path is open" >&2
    exit 1
fi
echo "ax-prove-helper-injection-blocked: PASS — function injection, helper symlink and helper"
echo "  deletion are each refused by the live guard; the unattacked control reaches none of them."
exit 0
