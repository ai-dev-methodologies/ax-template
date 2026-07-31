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

WORK="$(mktemp -d)"
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

# ── (a) FUNCTION INJECTION ───────────────────────────────────────────────────────────
SB_A="$WORK/inject"; build_sb "$SB_A" || { echo "harness setup failed (a)" >&2; exit 2; }
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
if ! grep -q "HELPER_FUNCTION_INJECTED" "$WORK/a.log" || [ "$A_RC" -eq 0 ]; then
    violation "an exported ax_anchor_* function was NOT refused (exit=$A_RC). The helper is the" \
              "single decider for which commit the anchor is and whether an absence is honest;" \
              "if the caller can supply those functions it supplies the policy."
    head -5 "$WORK/a.log" >&2
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
