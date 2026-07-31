#!/usr/bin/env bash
# practices/scripts/lib/release_anchor.sh — THE single release-anchor resolver + authenticator.
#
# (P1-X / P1-Y, cross-family reviewer ROUND 3, 2026-07-30; TD-2026-07-30-P1-anchor-authenticity.)
# (P1-1 … P1-4, cross-family reviewer ROUND 4, 2026-07-30; TD-2026-07-30-P1-anchor-runtime.)
#
# WHY THIS FILE EXISTS
# --------------------
# Two ratcheting guards (evidence_quote_spotcheck_guard, manifest_snapshot_integrity_guard) and
# the R25 runner (practices/scripts/verify-completion.sh) all need to answer the SAME question:
# "which commit is the previous release?". Round 2 left three independent copies of that logic,
# and the reviewer's round-3 attacks turn on the answer being (a) attacker-mutable and (b) not
# recorded anywhere the push gate can check. A single helper makes the runner's recorded
# anchor_sha and the guards' resolved anchor the same object by construction, which is what
# layer (3) below authenticates.
#
# ROUND 4, THE UNIFYING PRINCIPLE (state it once, apply it everywhere):
#   EVERY INPUT THE RATCHET TRUSTS MUST BE AUTHENTICATED AT THE MOMENT OF USE, NOT ASSUMED.
#   Rounds 1-3 authenticated the anchor's CONTENT (what it says), its IDENTITY (which commit)
#   and its REPRESENTATION (regular file vs symlink). Round 4 found that the ratchet's own
#   RUNTIME was still unauthenticated in four places, each of which is an input:
#     · how git INTERPRETS objects        → refs/replace/* rewrite history under a stable sha
#     · the anchor ref AT EACH READ       → the ref can move between the runner's read and the
#                                           guard's read, and again before the run ends
#     · the helper CODE ITSELF            → an inherited exported function, a symlinked helper,
#                                           or a deleted helper decides the policy
#     · the audit RECORD                  → a hand-authored line is a claim, not evidence
#
# ── P1-X: THE LOCAL ANCHOR REF IS ATTACKER-MUTABLE ───────────────────────────────────
# `refs/remotes/origin/main` is an ORDINARY LOCAL REF. `git update-ref` writes it. So:
#     M     = the malicious downgrade commit (this is what will be pushed)
#     FAKE  = a synthetic commit whose TREE is M's tree MINUS the two anchor paths,
#             parented on the REAL origin/main
#     git update-ref refs/remotes/origin/main FAKE
#     run R25   → both ratchets resolve their anchor to FAKE, find their own file ABSENT
#                 there, take the "first-release bootstrap" skip, and pass
#     git update-ref refs/remotes/origin/main <REAL>   → push M
# Nothing in round 2 looked at WHICH commit the ref pointed to; the audit line fingerprinted
# the working tree, and a ref is not part of the working tree.
#
# THREE INDEPENDENT LAYERS, all of which must hold:
#
#   (1) ANCESTRY — ANCHOR_NOT_ANCESTOR (blocking).
#       The resolved anchor commit MUST be an ancestor of HEAD
#       (`git merge-base --is-ancestor <anchor> HEAD`). On a normal release HEAD descends from
#       origin/main, so this is free. The reviewer's FAKE is parented on the REAL origin/main
#       and is therefore NOT an ancestor of M — the forgery is refused before any comparison.
#       LEGITIMATE EXCEPTION SHAPE, and it BLOCKS rather than skips: a local branch that has
#       diverged from origin/main (origin/main moved on, this branch has not been rebased).
#       The remedy is stated in the failure text — `git fetch origin && git rebase origin/main`
#       (or merge) — because a release that does not descend from the release it claims to
#       ratchet against has not actually been ratcheted: the two trees' histories are disjoint
#       past the merge base, so "the floor may only rise" is a comparison between two branches,
#       not between two releases. Skipping here would be the bypass; blocking is the point.
#
#   (2) BOOTSTRAP IS NOT FILE-ABSENCE ALONE — ANCHOR_BOOTSTRAP_IMPLAUSIBLE (blocking).
#       PLAINLY: a file that HAS HISTORY IN THIS REPO CAN NEVER LEGITIMATELY BE "ABSENT IN THE
#       PREVIOUS RELEASE". Absence at the anchor is honored as a first-release bootstrap ONLY
#       when the anchor's own history never contained the path:
#         (2a) `git rev-list --max-count=1 <anchor> -- <path>` must be EMPTY. The reviewer's
#              FAKE is parented on the REAL origin/main, which carries both guards, so FAKE
#              itself is a commit that TOUCHED the path (it deleted it) and rev-list returns it.
#              This layer therefore kills the reproduction on its own, independently of (1).
#         (2b) redundant probe from the OTHER end, so that a git-level trick which fools one
#              walk still has to fool a second one starting elsewhere: let C be the last commit
#              reachable from HEAD that touched the path; if C exists and C is an ancestor of
#              the anchor, then the anchor's tree cannot honestly lack the path.
#       DELIBERATELY NOT IMPLEMENTED AS WRITTEN IN THE BRIEF: "`rev-list -1 HEAD~1 -- <path>`
#       is non-empty ⇒ manufactured" would BLOCK A LEGITIMATE CASE — a ratcheting file added
#       one or two commits ago, while origin/main still predates it, has real HEAD~1 history and
#       a real, honest absence at the anchor. The predicate above is the false-positive-free form
#       of the same idea: it asks whether the ANCHOR's history had the file, not whether ANY
#       ancestor of HEAD did.
#
#   (3) AUDIT-BOUND ANCHOR + PRE-PUSH AUTHENTICATION (implemented by the callers, see
#       verify-completion.sh's audit writer and completion_checklist_recency_guard.sh).
#       verify-completion.sh records `anchor_sha`/`anchor_kind` — resolved HERE, so the runner
#       and the guards cannot disagree — in the audit line. The pre-push hook is handed the
#       AUTHORITATIVE remote sha by git itself (it comes from the remote's advertisement, not
#       from any local ref) and passes it to the recency guard, which requires the recorded
#       anchor_sha to EQUAL it. A forged local ref therefore produces an audit line bound to a
#       commit the remote does not have, and the push is refused. This is the layer that makes
#       ref forgery UNPUSHABLE even if (1) and (2) were both bypassed.
#       Legitimate exception, disclosed: the remote moved between the R25 run and the push
#       (someone else pushed). The honest resolution is `git fetch` + re-run R25 — the ratchet
#       was measured against a release that is no longer the release being extended.
#
# ── P1-Y: SYMLINK REPRESENTATION LAUNDERING ──────────────────────────────────────────
# THE ASYMMETRY, stated once because it is the whole class: THE ANCHOR SIDE READS GIT OBJECTS
# (`git show`, `git ls-tree`) AND THE SELF SIDE READS THE FILESYSTEM (`open()` follows symlinks).
# Any place those two disagree is a laundering channel. A path replaced by a SYMLINK is read by
# the self side as the TARGET's content and by the anchor side as the LINK BLOB (which is just
# the target pathname) — so a guard can verify itself green while its successor's anchor parse
# sees whatever the attacker chose to spell in a pathname.
#   ax_anchor_worktree_paths_regular — lstat (`[ -L ]`), NOT stat, on the leaf AND on every path
#     component below the base, so a symlinked DIRECTORY cannot launder a regular leaf either.
#     Runs on EVERY root (live and fixture) because it inspects the tree in front of it and
#     needs no release history — which is what makes it the half a FIXTURE can prove.
#   ax_anchor_release_paths_regular — `git ls-tree <anchor> -- <path>` mode must be 100644 or
#     100755. Mode 120000 (symlink) is ANCHOR_PATH_NOT_REGULAR, blocking. Anchor-bound, so
#     live-root only and not fixture-coverable; its evidence is the RED reproductions recorded
#     in practices/DECISIONS.md.
#
# ── ROUND 4 / P1-1: GIT REPLACEMENT REFS — THE SHA IS STABLE, THE OBJECT IS NOT ──────
# `git replace <real> <fabricated>` installs refs/replace/<real-sha> → <fabricated-object>.
# Every ordinary git command (rev-list, show, ls-tree, merge-base, log, diff) then reads the
# FABRICATED object while every sha printed anywhere stays the REAL one. So layers (1) and (2)
# above — which are exactly rev-list/ls-tree/merge-base reads — can be answered out of a
# fabricated history while the audit line records the authentic sha and the pre-push binding in
# layer (3) matches the remote perfectly. The tree fingerprint cannot see it either: a
# replacement ref is not part of the working tree.
# TWO answers, both applied, because either alone is thin:
#   · EVERY git invocation in this file (and in every consumer) runs with
#     GIT_NO_REPLACE_OBJECTS=1 — exported HERE so it is inherited by shell git calls AND by the
#     python subprocesses inside the guards, which is the class of call site a wrapper function
#     cannot reach. `ax_git` additionally passes `--no-replace-objects` explicitly, so a call
#     that somehow loses the environment still reads real objects. Belt and braces on purpose:
#     the env var is the one that no call site can forget, the flag is the one that survives an
#     env-scrubbing wrapper.
#   · ax_anchor_check_replace_refs BLOCKS when refs/replace/* is non-empty on a live root
#     (ANCHOR_REPLACE_REFS_PRESENT). A legitimate release has no reason to carry replacement
#     refs; carrying them while a ratchet runs is indistinguishable from staging the attack, and
#     "we read past them" is a claim about every call site forever, which is not a thing this
#     catalog can keep true by inspection.
#
# ── ROUND 4 / P1-2: TOCTOU ON THE ANCHOR REF ────────────────────────────────────────
# The runner resolved the anchor for the AUDIT LINE, and each guard re-resolved origin/main
# INDEPENDENTLY, later. Between those reads the ref is writable: aim it at an ancient root
# commit while the guards run (they bootstrap-skip, since the ratcheting files are absent from
# a root commit and — before this round — the runner's recorded sha was the honest one).
# SINGLE-RESOLUTION BINDING closes it:
#   · the runner resolves ONCE and exports AX_RELEASE_ANCHOR_SHA/_KIND/_REF/_ROOT (the root is
#     part of the pin so a nested sandbox run cannot inherit an unrelated repo's pin);
#   · every guard, at ITS OWN read, re-resolves and requires the fresh value to EQUAL the pin
#     (ANCHOR_REF_MOVED_MIDRUN, blocking) — the pin is used, and it is used only after being
#     re-authenticated against the ref as it stands at that instant;
#   · a guard run STANDALONE (no pin in the environment) resolves for itself and calls
#     ax_anchor_verify_unmoved before it exits, so it too observes the ref twice;
#   · the runner re-verifies at the END of the run and writes the audit line as a FAILURE if the
#     ref moved (anchor_stable=false), mirroring the existing tree_stable/tree_samples pattern.
# HONEST LIMIT, same shape as tree sampling: observation is at read boundaries. A ref moved and
# restored entirely between two reads is unobserved. The window is narrowed, not closed.
#
# ── ROUND 4 / P1-3: THIS FILE IS AN INJECTABLE POLICY SURFACE ───────────────────────
# Round 3 made this file the single decider — which also made it the single thing worth
# attacking. Three sub-attacks, three answers:
#   (a) INHERITED FUNCTIONS. The old idempotence guard returned early on `_AX_RELEASE_ANCHOR_LIB`
#       BEFORE defining anything, so `export -f ax_anchor_resolve` + `_AX_RELEASE_ANCHOR_LIB=1`
#       left the ATTACKER's functions authoritative in every consumer. FIX: the early return is
#       GONE — this file ALWAYS (re)defines its functions on source; the marker now guards only
#       the injection REPORT, never the definitions. Before defining, every ax_anchor_*/_ax_*
#       name is `unset -f`, and a name that ARRIVES already defined (with the marker unset, i.e.
#       nobody in this shell legitimately sourced us yet) is HELPER_FUNCTION_INJECTED, exit 2.
#       An EXPORTED marker is itself the forgery signature and is refused the same way — a
#       legitimate marker is a plain shell variable set by this file.
#   (b) SYMLINKED HELPER. Both loaders used `[ -f ]`, which FOLLOWS symlinks, and did so before
#       any mode check existed. FIX: each consumer runs a minimal lstat + `git ls-tree` mode
#       preflight INLINE, immediately before sourcing. Bootstrap problem, stated because it
#       dictates the shape: the check cannot live in the file it is checking, so those few lines
#       are DELIBERATELY DUPLICATED in every consumer rather than factored out.
#   (c) DELETED HELPER. The helper was on nobody's anchor-critical path list, so deleting it
#       activated the AX_RELEASE_ANCHOR_LIB env fallback. FIX: this file, both hook files, the
#       runner and the recency guard are now ON both guards' anchor-critical lists (deletion is
#       ANCHOR_BOOTSTRAP_IMPLAUSIBLE, symlinking is ANCHOR_PATH_NOT_REGULAR / SELF_PATH_NOT_
#       REGULAR), and the env fallback is honored ONLY on a NON-git root — i.e. exactly the
#       relocated-copy sandbox [87] needs, never on a live tree.
#
# CONTRACT
#   Every function reads git + its arguments, prints diagnostics to STDERR, and returns a status.
#   NONE calls `exit` — each caller maps a violation onto ITS OWN exit code, because the two
#   guards have different, already-published code spaces.
#   ONE DELIBERATE EXCEPTION: the LOAD-TIME injection check below exits 2 directly. There is no
#   caller state to map onto yet (the functions do not exist), and an injected policy surface
#   must not be allowed to return into a caller that would then call the injected functions.
#
# Usage:
#   source practices/scripts/lib/release_anchor.sh
#   ax_anchor_resolve "$REPO_ROOT"                    # → AX_ANCHOR_REF/_KIND/_SHA
#   ax_anchor_check_pin "<label>"                     # → 1 on ANCHOR_REF_MOVED_MIDRUN (vs pin)
#   ax_anchor_check_replace_refs "$REPO_ROOT" "<label>"  # → 1 on ANCHOR_REPLACE_REFS_PRESENT
#   ax_anchor_check_ancestry "$REPO_ROOT" "<label>"   # → 1 on ANCHOR_NOT_ANCESTOR
#   ax_anchor_release_paths_regular "$REPO_ROOT" "<label>" <rel>...
#   ax_anchor_worktree_paths_regular "$BASE" "<label>" <rel>...
#   ax_anchor_verify_unmoved "$REPO_ROOT" "<label>"   # → 1 on ANCHOR_REF_MOVED_MIDRUN (vs own read)
#   ax_anchor_export_pin "$REPO_ROOT"                 # runner only: publish the single resolution

# ── P1-1: git must interpret objects as they are, not as refs/replace/* says ──────────
# Exported (not merely set) so that it reaches the python subprocesses in the guards, which is
# where most of the anchor reads actually happen.
export GIT_NO_REPLACE_OBJECTS=1

# ── P1-3(a): LOAD-TIME INJECTION CHECK ───────────────────────────────────────────────
# Runs BEFORE any definition, and the definitions below run UNCONDITIONALLY afterwards. Two
# distinct signatures, both blocking:
#   · the marker arrived EXPORTED — this file only ever sets it as a plain shell variable, so an
#     exported one was manufactured by a parent trying to suppress the check;
#   · a helper function name is already defined while the marker is UNSET — nobody in this shell
#     legitimately sourced this file yet, so the definition came from outside (an exported
#     function survives `bash script.sh`; that is the delivery channel).
# A second legitimate source in the SAME shell (two guards sourced by one harness) sets the
# marker first, so it is not mistaken for injection — and it still re-runs every definition.
_AX_ANCHOR_FN_NAMES="ax_git ax_anchor_resolve ax_anchor_export_pin ax_anchor_check_pin \
ax_anchor_check_replace_refs ax_anchor_check_ancestry ax_anchor_release_paths_regular \
ax_anchor_worktree_paths_regular ax_anchor_verify_unmoved _ax_anchor_bootstrap_implausible"

_ax_anchor_injection_report() {
    {
        echo "release_anchor.sh: HELPER_FUNCTION_INJECTED — $1"
        echo "  This file is THE policy surface for the release ratchet: it decides which commit"
        echo "  the anchor is, whether that commit is an ancestor, whether an absence is an honest"
        echo "  first-release bootstrap, and whether the anchored paths are regular files. A"
        echo "  definition of any of those that arrives from OUTSIDE (bash imports exported"
        echo "  functions across \`bash script.sh\`) replaces the policy with the caller's."
        echo "  The definitions below are re-established unconditionally, so the injection is"
        echo "  already dead by the time you read this — this BLOCK exists because a process that"
        echo "  tried it must not be allowed to continue into the gate it was aiming at."
        echo "  Names reserved by this file: ${_AX_ANCHOR_FN_NAMES}"
    } >&2
    exit 2
}

if [ -n "${_AX_RELEASE_ANCHOR_LIB:-}" ]; then
    if declare -p _AX_RELEASE_ANCHOR_LIB 2>/dev/null | grep -q '^declare -x'; then
        _ax_anchor_injection_report \
            "_AX_RELEASE_ANCHOR_LIB arrived from the ENVIRONMENT (exported). This file sets it as
  a plain shell variable; an exported one is a forged 'already loaded' marker."
    fi
else
    for _ax_fn in $_AX_ANCHOR_FN_NAMES; do
        if declare -F "$_ax_fn" >/dev/null 2>&1; then
            _ax_anchor_injection_report \
                "the function ${_ax_fn} was ALREADY DEFINED before this file was sourced, and no
  prior source in this shell set the load marker."
        fi
    done
    unset _ax_fn
fi
# Belt and braces for shells that expose imported functions as BASH_FUNC_* variables (bash 4+):
# catch a name that bash REFUSED to import as a function but still left in the environment.
for _ax_bf in ${!BASH_FUNC_@}; do
    case "$_ax_bf" in
        BASH_FUNC_ax_anchor_*|BASH_FUNC__ax_anchor_*|BASH_FUNC_ax_git*|BASH_FUNC_pp_*)
            _ax_anchor_injection_report \
                "the environment carries ${_ax_bf} — an exported shell function aimed at this
  file's namespace." ;;
    esac
done
unset _ax_bf

# Definitions are re-established on EVERY source. `unset -f` first so a name that survived the
# checks above (or was defined by a legitimate earlier source) cannot linger in any form.
for _ax_fn in $_AX_ANCHOR_FN_NAMES; do unset -f "$_ax_fn" 2>/dev/null || true; done
unset _ax_fn
# Plain (NOT exported) marker: a second source in this shell is idempotent for the REPORT only.
_AX_RELEASE_ANCHOR_LIB=1

# The remote-tracking ref the ratchet anchors to, and the REMOTE-SIDE ref name whose sha the
# pre-push hook receives from git. They are two spellings of the same branch and are kept
# together here so layer (3) cannot drift apart from layers (1)/(2).
AX_ANCHOR_TRACKING_REF="origin/main"
AX_ANCHOR_REMOTE_REF="refs/heads/main"
AX_ANCHOR_ZERO_SHA="0000000000000000000000000000000000000000"

# ax_git <repo> <git-args>...
#   THE single git call site of this file (P1-1). `--no-replace-objects` is passed explicitly in
#   addition to the exported GIT_NO_REPLACE_OBJECTS above: the environment is what python
#   subprocesses and forgetful call sites inherit, the flag is what survives a wrapper that
#   scrubs the environment. Neither alone covers both.
ax_git() {
    local repo="$1"; shift
    git --no-replace-objects -C "$repo" "$@"
}

# ax_anchor_resolve <repo_root>
#   Sets AX_ANCHOR_REF (the rev to pass to git), AX_ANCHOR_KIND (origin/main | HEAD |
#   unavailable) and AX_ANCHOR_SHA (the resolved commit sha, or "unavailable").
#   Resolution order, unchanged from round 1-2: origin/main (strong — R25 runs at a tree AHEAD
#   of it) → HEAD (weaker: a detached/fork-fresh clone; a downgrade already committed locally
#   is present in the anchor itself) → unavailable. Always returns 0; the caller decides what an
#   unavailable anchor means for it.
#   P1-2: when the RUNNER has published a pin for THIS repo root, the fresh resolution must equal
#   it. The pin is not trusted blindly and the ref is not trusted blindly — they are required to
#   agree, which is what makes "the runner recorded X" and "the guard measured against X" the
#   same statement. Disagreement is recorded in AX_ANCHOR_PIN_MISMATCH for ax_anchor_check_pin;
#   this function still returns 0 so callers keep their existing control flow.
ax_anchor_resolve() {
    local repo="$1" canon
    AX_ANCHOR_REF=""
    AX_ANCHOR_KIND="unavailable"
    AX_ANCHOR_SHA="unavailable"
    AX_ANCHOR_PIN_MISMATCH=""
    AX_ANCHOR_PIN_APPLIED=0
    command -v git >/dev/null 2>&1 || return 0
    ax_git "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    if ax_git "$repo" rev-parse --verify --quiet "$AX_ANCHOR_TRACKING_REF" >/dev/null 2>&1; then
        AX_ANCHOR_REF="$AX_ANCHOR_TRACKING_REF"
        AX_ANCHOR_KIND="origin/main"
    elif ax_git "$repo" rev-parse --verify --quiet HEAD >/dev/null 2>&1; then
        AX_ANCHOR_REF="HEAD"
        AX_ANCHOR_KIND="HEAD"
    else
        return 0
    fi
    AX_ANCHOR_SHA="$(ax_git "$repo" rev-parse --verify --quiet "${AX_ANCHOR_REF}^{commit}" 2>/dev/null)"
    if [ -z "$AX_ANCHOR_SHA" ]; then
        AX_ANCHOR_REF=""
        AX_ANCHOR_KIND="unavailable"
        AX_ANCHOR_SHA="unavailable"
        return 0
    fi
    # Pin comparison — only for the repo the pin was taken in. A nested sandbox run (guards [87]
    # /[97]/[98] build throwaway repos) inherits the outer environment; keying on the root keeps
    # an unrelated repo's pin from firing there, and the sandbox's own runner re-publishes.
    canon="$(cd "$repo" 2>/dev/null && pwd -P)" || canon=""
    if [ -n "${AX_RELEASE_ANCHOR_SHA:-}" ] && [ -n "$canon" ] \
       && [ "${AX_RELEASE_ANCHOR_ROOT:-}" = "$canon" ]; then
        AX_ANCHOR_PIN_APPLIED=1
        if [ "${AX_RELEASE_ANCHOR_SHA}" != "$AX_ANCHOR_SHA" ] \
           || [ "${AX_RELEASE_ANCHOR_KIND:-}" != "$AX_ANCHOR_KIND" ]; then
            AX_ANCHOR_PIN_MISMATCH="pinned ${AX_RELEASE_ANCHOR_KIND:-?}=${AX_RELEASE_ANCHOR_SHA} but the ref now resolves to ${AX_ANCHOR_KIND}=${AX_ANCHOR_SHA}"
        else
            # Equal by construction now — use the PINNED value, so the sha this guard ratchets
            # against is literally the object the runner recorded.
            AX_ANCHOR_SHA="$AX_RELEASE_ANCHOR_SHA"
        fi
    fi
    return 0
}

# ax_anchor_export_pin <repo_root>
#   RUNNER ONLY. Publishes the single resolution to every child process (guards, hooks). Must be
#   called immediately after ax_anchor_resolve so no window exists between the value that goes
#   into the audit line and the value the children are held to.
ax_anchor_export_pin() {
    local repo="$1" canon
    canon="$(cd "$repo" 2>/dev/null && pwd -P)" || canon=""
    [ -n "$canon" ] || return 0
    [ -n "${AX_ANCHOR_SHA:-}" ] && [ "$AX_ANCHOR_SHA" != "unavailable" ] || return 0
    export AX_RELEASE_ANCHOR_SHA="$AX_ANCHOR_SHA"
    export AX_RELEASE_ANCHOR_KIND="$AX_ANCHOR_KIND"
    export AX_RELEASE_ANCHOR_REF="$AX_ANCHOR_REF"
    export AX_RELEASE_ANCHOR_ROOT="$canon"
    return 0
}

# ax_anchor_check_pin <label>
#   P1-2. 0 = no pin, or the pin and the ref agree; 1 = ANCHOR_REF_MOVED_MIDRUN (blocking).
ax_anchor_check_pin() {
    local label="$1"
    [ -n "${AX_ANCHOR_PIN_MISMATCH:-}" ] || return 0
    {
        echo "${label}: ANCHOR_REF_MOVED_MIDRUN — the release anchor moved DURING this run."
        echo "    ${AX_ANCHOR_PIN_MISMATCH}"
        echo "  The R25 runner resolved the anchor ONCE, recorded that sha in the audit line, and"
        echo "  published it to every guard it starts. This guard re-read the ref at its own"
        echo "  moment of use and got something else. Both readings cannot be the release being"
        echo "  extended, so neither is trusted."
        echo "  WHY THIS IS BLOCKING: refs/remotes/origin/main is an ORDINARY LOCAL REF. Aiming it"
        echo "  at an ancient root commit only while the guards run makes every ratchet find its"
        echo "  own file absent and take the first-release bootstrap skip, while the audit line"
        echo "  keeps the honest sha and the pre-push binding still matches the remote perfectly."
        echo "  A run whose reference point changed underneath it has measured nothing."
        echo "  If this was innocent (a \`git fetch\` landed mid-run), re-run R25 on a settled repo."
    } >&2
    return 1
}

# ax_anchor_check_replace_refs <repo_root> <label>
#   P1-1. 0 = no replacement refs; 1 = ANCHOR_REPLACE_REFS_PRESENT (blocking).
#   Uses for-each-ref rather than `git replace -l` so the listing itself cannot be affected by
#   the replacement machinery it is inspecting.
ax_anchor_check_replace_refs() {
    local repo="$1" label="$2" refs
    command -v git >/dev/null 2>&1 || return 0
    ax_git "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    refs="$(ax_git "$repo" for-each-ref --format='%(refname)' refs/replace/ 2>/dev/null)"
    [ -n "$refs" ] || return 0
    {
        echo "${label}: ANCHOR_REPLACE_REFS_PRESENT — this repository carries git REPLACEMENT REFS:"
        echo "$refs" | sed 's/^/    /'
        echo "  \`git replace <real> <fabricated>\` keeps every SHA identical while every ordinary"
        echo "  git command (rev-list, show, ls-tree, merge-base) reads the FABRICATED object. The"
        echo "  whole anchor authentication — ancestry, bootstrap-plausibility, blob modes — is"
        echo "  made of exactly those commands, so a replacement ref answers them out of a history"
        echo "  the attacker wrote, while the audit line records the authentic sha and the pre-push"
        echo "  binding against the remote's advertisement still matches. Nothing downstream can"
        echo "  tell the difference: a replacement ref is not part of the working tree either."
        echo "  Every git call in this ratchet already runs with GIT_NO_REPLACE_OBJECTS=1 and"
        echo "  --no-replace-objects; this BLOCK exists because 'we read past them everywhere' is a"
        echo "  claim about every present and future call site, and a released tree has no reason"
        echo "  to carry them at all. Remove them and re-run:  git replace -d <ref>"
    } >&2
    return 1
}

# ax_anchor_check_ancestry <repo_root> <label>
#   Layer (1). 0 = anchor is an ancestor of HEAD (or there is no anchor / no HEAD to compare);
#   1 = ANCHOR_NOT_ANCESTOR, blocking for the caller.
ax_anchor_check_ancestry() {
    local repo="$1" label="$2" head
    [ -n "${AX_ANCHOR_REF:-}" ] || return 0
    head="$(ax_git "$repo" rev-parse --verify --quiet HEAD 2>/dev/null)" || head=""
    [ -n "$head" ] || return 0
    if ax_git "$repo" merge-base --is-ancestor "$AX_ANCHOR_SHA" "$head" 2>/dev/null; then
        return 0
    fi
    {
        echo "${label}: ANCHOR_NOT_ANCESTOR — the resolved release anchor"
        echo "    ${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}"
        echo "  is NOT an ancestor of HEAD (${head}). A release ratchets against the release it"
        echo "  EXTENDS, so on any honest release HEAD descends from the anchor and this check is"
        echo "  free. It fails in exactly two situations, and both must block:"
        echo "    · refs/remotes/origin/main is an ORDINARY LOCAL REF that \`git update-ref\` can"
        echo "      point anywhere. Aiming it at a synthetic commit that merely LACKS the"
        echo "      ratcheting files makes every ratchet take its first-release bootstrap skip."
        echo "      That commit is parented on the real release, so it is not an ancestor of the"
        echo "      commit being shipped — which is what this check sees."
        echo "    · your branch has genuinely diverged: origin/main moved on and this branch was"
        echo "      never rebased onto it. Then the comparison is between two BRANCHES, not two"
        echo "      releases, and 'the floor may only rise' does not mean what it says. Fix it:"
        echo "        git fetch origin && git rebase origin/main   (or merge origin/main)"
        echo "      then re-run. This is a BLOCK, never a skip: an unratchetable tree must not ship."
    } >&2
    return 1
}

# _ax_anchor_bootstrap_implausible <repo_root> <label> <rel>
#   Layer (2). Called ONLY when <rel> is absent at the anchor. 0 = the absence is a plausible
#   first-release bootstrap; 1 = ANCHOR_BOOTSTRAP_IMPLAUSIBLE (blocking).
_ax_anchor_bootstrap_implausible() {
    local repo="$1" label="$2" rel="$3" hist last_touch reason=""
    hist="$(ax_git "$repo" rev-list --max-count=1 "$AX_ANCHOR_SHA" -- "$rel" 2>/dev/null)"
    if [ -n "$hist" ]; then
        reason="the anchor's OWN history contains ${rel} (last touched by ${hist}), so its"$'\n'"    absence at ${AX_ANCHOR_SHA} is a DELETION, not a never-existed"
    else
        last_touch="$(ax_git "$repo" rev-list --max-count=1 HEAD -- "$rel" 2>/dev/null)"
        if [ -n "$last_touch" ] \
           && ax_git "$repo" merge-base --is-ancestor "$last_touch" "$AX_ANCHOR_SHA" 2>/dev/null; then
            reason="the commit that last touched ${rel} on HEAD's history (${last_touch}) is an"$'\n'"    ANCESTOR of the anchor, so the anchor's tree cannot honestly lack it"
        fi
    fi
    [ -n "$reason" ] || return 0
    {
        echo "${label}: ANCHOR_BOOTSTRAP_IMPLAUSIBLE — ${rel} is absent at the release anchor"
        echo "  (${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}), but that absence is MANUFACTURED:"
        echo "    ${reason}."
        echo "  THE RULE, plainly: A FILE THAT HAS HISTORY IN THIS REPO CAN NEVER LEGITIMATELY BE"
        echo "  'ABSENT IN THE PREVIOUS RELEASE'. Absence is honored as a first-release bootstrap"
        echo "  skip ONLY for a path the anchor's history never contained. Otherwise the skip is"
        echo "  the payload: point refs/remotes/origin/main at a commit whose tree simply DROPS the"
        echo "  ratcheting files, and every ratchet politely bootstraps itself out of existence."
        echo "  If you are deliberately deleting this path, the ratchet it carries is being"
        echo "  retired — do that in a commit that says so, not through an anchor that pretends"
        echo "  the file was never here."
    } >&2
    return 1
}

# ax_anchor_release_paths_regular <repo_root> <label> <rel>...
#   Layer P1-Y (anchor side) + layer (2). For each path, as of the resolved anchor:
#     · present with mode 100644/100755 → OK
#     · present with any other mode (120000 symlink, 160000 gitlink, 040000 tree) →
#       ANCHOR_PATH_NOT_REGULAR, blocking
#     · absent → the bootstrap-plausibility test above
#   No-op when no anchor resolved (the caller has already decided what that means).
#   0 = all clean, 1 = at least one violation (every violation is printed).
ax_anchor_release_paths_regular() {
    local repo="$1" label="$2"; shift 2
    local rel entry mode bad=0
    [ -n "${AX_ANCHOR_REF:-}" ] || return 0
    for rel in "$@"; do
        entry="$(ax_git "$repo" ls-tree "$AX_ANCHOR_SHA" -- "$rel" 2>/dev/null | head -1)"
        if [ -z "$entry" ]; then
            _ax_anchor_bootstrap_implausible "$repo" "$label" "$rel" || bad=1
            continue
        fi
        mode="${entry%% *}"
        case "$mode" in
            100644|100755) ;;
            *)
                {
                    echo "${label}: ANCHOR_PATH_NOT_REGULAR — ${rel} exists at the release anchor"
                    echo "  (${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}) with git mode ${mode}, which is not a"
                    echo "  regular file blob (100644/100755). Mode 120000 is a SYMLINK, and a symlink is"
                    echo "  the representation-laundering channel this check exists to close: the anchor"
                    echo "  side reads GIT OBJECTS, so \`git show\` hands back the LINK BLOB — the target"
                    echo "  PATHNAME — while the self side reads the FILESYSTEM and follows the link to"
                    echo "  the real content. Choose a pathname that is also parseable-but-weakened"
                    echo "  source and the two sides can be made to disagree by construction. An"
                    echo "  anchor-critical path must be a regular file on BOTH sides."
                } >&2
                bad=1
                ;;
        esac
    done
    return "$bad"
}

# ax_anchor_worktree_paths_regular <base_dir> <label> <rel>...
#   Layer P1-Y (self side). lstat-based, so it sees the LINK rather than its target, and it
#   walks EVERY path component below <base_dir> — a symlinked directory launders a regular leaf
#   just as well as a symlinked leaf does. A path that does not exist is not a violation here
#   (absence is the callers' own business); a path that exists but is not a regular file is.
#   Runs on every root, live or fixture — this is the fixture-provable half of P1-Y.
#   0 = all clean, 1 = at least one violation.
ax_anchor_worktree_paths_regular() {
    local base="$1" label="$2"; shift 2
    local rel rest part cur bad=0 hit
    for rel in "$@"; do
        cur="$base"
        rest="$rel"
        hit=0
        while [ -n "$rest" ]; do
            part="${rest%%/*}"
            if [ "$part" = "$rest" ]; then rest=""; else rest="${rest#*/}"; fi
            [ -n "$part" ] || continue
            cur="$cur/$part"
            if [ -L "$cur" ]; then
                {
                    echo "${label}: SELF_PATH_NOT_REGULAR — ${rel} resolves through a SYMLINK at"
                    echo "  ${cur#"$base"/} (under ${base})."
                    echo "  Every check in this guard that reads the filesystem follows that link, while"
                    echo "  every check that reads the release anchor gets the LINK BLOB — the target"
                    echo "  PATHNAME — from git. The two sides then disagree by construction, which is"
                    echo "  precisely the laundering channel: make the TARGET PATHNAME itself parse as"
                    echo "  weakened source, and this generation verifies the real file green while the"
                    echo "  next generation's anchor parse reads the pathname instead."
                    echo "  An anchor-critical path (and every directory on the way to it) must be a"
                    echo "  regular file / real directory. Replace the symlink with the file itself."
                } >&2
                bad=1
                hit=1
                break
            fi
        done
        if [ "$hit" -eq 0 ] && [ -e "$cur" ] && [ ! -f "$cur" ]; then
            {
                echo "${label}: SELF_PATH_NOT_REGULAR — ${rel} exists under ${base} but is not a"
                echo "  regular file. Anchor-critical paths are compared against git blobs; anything"
                echo "  that is not a regular file cannot be compared and must not be trusted."
            } >&2
            bad=1
        fi
    done
    return "$bad"
}

# ax_anchor_verify_unmoved <repo_root> <label>
#   P1-2, the standalone half: re-read the anchor ref NOW and require it to still hold the value
#   this process resolved. Callers invoke it after their anchor-dependent work, so that a run
#   with no runner pin in the environment still observes the ref at two separate instants.
#   0 = unchanged (or nothing to compare); 1 = ANCHOR_REF_MOVED_MIDRUN, blocking.
ax_anchor_verify_unmoved() {
    local repo="$1" label="$2" now
    [ -n "${AX_ANCHOR_REF:-}" ] || return 0
    [ -n "${AX_ANCHOR_SHA:-}" ] && [ "$AX_ANCHOR_SHA" != "unavailable" ] || return 0
    now="$(ax_git "$repo" rev-parse --verify --quiet "${AX_ANCHOR_REF}^{commit}" 2>/dev/null)" || now=""
    [ "$now" = "$AX_ANCHOR_SHA" ] && return 0
    {
        echo "${label}: ANCHOR_REF_MOVED_MIDRUN — ${AX_ANCHOR_REF} changed while this check ran."
        echo "    at resolution: ${AX_ANCHOR_SHA}"
        echo "    now         : ${now:-<unresolvable>}"
        echo "  The whole ratchet is a comparison against ONE release. A reference point that moves"
        echo "  between the read that selected it and the read that confirms it was not a reference"
        echo "  point — every conclusion drawn in between is about an object that is no longer the"
        echo "  thing being extended. refs/remotes/origin/main is an ordinary local ref, so this is"
        echo "  writable by anything running concurrently, deliberately or not."
        echo "  Re-run on a settled repository (finish any \`git fetch\` first)."
    } >&2
    return 1
}
