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
# ── ROUND 5 / P1-1: THE GIT REPOSITORY IS AN INHERITED IDENTITY ─────────────────────
# `git -C <path>` says WHERE to start looking. It does NOT say WHICH REPOSITORY to use: GIT_DIR,
# GIT_WORK_TREE, GIT_COMMON_DIR, GIT_OBJECT_DIRECTORY, GIT_ALTERNATE_OBJECT_DIRECTORIES,
# GIT_INDEX_FILE, GIT_NAMESPACE, GIT_CEILING_DIRECTORIES and the GIT_CONFIG* family all override
# discovery and configuration from the ENVIRONMENT, and every one of them is inherited.
# MEASURED, in a throwaway clone: with GIT_DIR/GIT_WORK_TREE aimed at a CLEAN shadow checkout of
# the same commit, the fingerprint of a DIRTY tree came back as `0a815065ebf5…` — the clean-tree
# constant — while the honest value was `c764531c8f18…`; `rev-parse --show-toplevel` reported the
# shadow, and the cleanliness probe reported true. R25 would then run its tests against the dirty
# tree while every head/status/fingerprint/anchor/cleanliness read describes the clean one; revert
# the dirty fix afterwards and the pushed commit keeps a perfect-looking audit line.
# AX_RELEASE_ANCHOR_ROOT does not help: it scopes the PATHNAME, never the git context.
# ANSWER, applied at the earliest bootstrap point of every entry (see the block below, which is
# duplicated verbatim into every consumer because it must precede the first git call and any
# `source`): the entire GIT_* discovery + config-injection family is UNSET, GIT_NO_REPLACE_OBJECTS
# is re-established by us, the real toplevel is derived from the trusted REPO_ROOT and REQUIRED to
# equal it (GIT_CONTEXT_REDIRECTED, blocking), and the derived gitdir/worktree are passed
# EXPLICITLY on every invocation for the bound root — no implicit discovery anywhere, including
# inside tree_fingerprint.py.
#
# ── ROUND 5 / P1-2: THE COMMANDS THEMSELVES ARE INHERITED ───────────────────────────
# Round 4 reserved this file's own names (ax_anchor_*/ax_git). It did not reserve the COMMANDS
# those functions call. bash imports exported functions across `bash script.sh`, so `export -f
# git` puts the caller's code inside every anchor read. MEASURED: an exported `git` function
# rewrote the runner-owned pin (PIN_BEFORE 5e1f5a9 → PIN_AFTER 00c9e2c, every rc 0); an exported
# `pwd` moved the pin's canonical root to a foreign repo (PIN_ROOT=/foreign/repo, and the
# root-scoped pin comparison then fires against the wrong repo); an exported `python3` turned the
# recency guard from exit 1 (AUDIT_LOG_MISSING) into exit 0, i.e. an entire python guard skipped.
# ANSWER: every command name the ratchet actually invokes is (a) refused when it arrives already
# defined — HELPER_FUNCTION_INJECTED — and (b) `unset -f`'d anyway; BASH_ENV/ENV/BASH_FUNC_*
# remnants are refused outright (HERMETIC_ENV_HOSTILE); and `git`/`python3` are resolved ONCE to
# ABSOLUTE paths from a sanitized PATH and validated to be executable regular files outside the
# repository (HERMETIC_TOOL_UNUSABLE). Callers use "$AX_GIT_BIN"/"$AX_PY_BIN" and `builtin cd` /
# `builtin pwd`, so neither the PATH nor a function can re-point them afterwards.
#
# ── ROUND 5 / P1-3: THE RATCHET AUTHENTICATED ITS OWN MUTABLE IMPLEMENTATION ────────
# The toolchain lists checked filesystem/git MODE, never BYTES, so a REGULAR-FILE
# tree_fingerprint.py edited to print a constant passed every path check and was then used by BOTH
# the runner (writing the record) and the verifier (recomputing it) — one compromised
# implementation authenticating itself. MEASURED: with a hand-authored audit line carrying a bogus
# fingerprint the recency guard exits 1 (AUDIT_FINGERPRINT_MISMATCH); replace the helper with
# `print("nogit")` and it exits 0, because the recompute only objected when the output was
# non-empty AND not `nogit` — unknown fell OPEN.
# ANSWER, two halves: `ax_ratchet_toolchain_authentic` below requires each toolchain file's
# WORKING-TREE BYTES to equal what git records for it at the sha under audit
# (RATCHET_TOOLCHAIN_MODIFIED, blocking — an uncommitted edit to the ratchet's own code can never
# certify anything), and the recency guard recomputes the fingerprint with the PREVIOUS RELEASE'S
# copy of the helper, extracted from git outside the tree, so the verifier no longer shares the
# runner's implementation. HONEST CONSEQUENCE, stated because it is a real cost: this binds the
# toolchain to the previous release, so a change to these files must ship in a release whose own
# gate ran with the PRIOR implementation. A refactor is free (the prior implementation computes
# the same digest); a change to what the algorithm OUTPUTS cannot pass this gate at all and is a
# deliberate, documented, human decision — see practices/DECISIONS.md
# (TD-2026-07-30-P1-hermetic-runtime).
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
#   ax_ratchet_toolchain_authentic "$REPO_ROOT" "<label>" <rev> <rel>...   # → 1 on tampering
#   ax_anchor_bind_repo "$REPO_ROOT" "<label>"        # → 1 on GIT_CONTEXT_REDIRECTED
#   ax_ratchet_filters_absent "$REPO_ROOT" "<label>" <rel>...              # → 1 on GIT_FILTERS_PRESENT

# ── P1-1: git must interpret objects as they are, not as refs/replace/* says ──────────
# Exported (not merely set) so that it reaches the python subprocesses in the guards, which is
# where most of the anchor reads actually happen. ROUND 5: the whole GIT_* family is scrubbed by
# the hermetic bootstrap in every entry FIRST — including this variable, because
# `GIT_NO_REPLACE_OBJECTS=0` inherited from the environment RE-ENABLES replacement refs — and this
# line re-establishes the value we actually want.
export GIT_NO_REPLACE_OBJECTS=1

# ── ROUND 6 / P1-1: PURE-KEYWORD PREFLIGHT — THESE ARE THE FIRST EXECUTABLE LINES ────
# (TD-2026-07-30-P1-preflight-and-raw-bytes.) INVARIANT (α): NOTHING OVERRIDABLE MAY EXECUTE
# BEFORE THE SCRUB THAT DETECTS OVERRIDES. Round 5 put `set -uo pipefail` and `[ -n … ]` ahead of
# its own hermetic bootstrap, and both are ordinary command lookups: MEASURED —
#   BASH_FUNC_set%%='() { exit 0; }'  → .githooks/pre-push exit 0   (honest baseline 1)
#   BASH_FUNC_[%%='() { exit 0; }'    → this guard      exit 0      (honest baseline 2)
# A dependency LIST cannot fix that, because the list is consulted by code that has already run.
# So the first thing every entry does is expressed ONLY in constructs an exported function cannot
# reach:
#   · shell KEYWORDS (`if`, `case`, `for`, `[[ ]]`) — never resolved through the function table;
#   · variable ASSIGNMENT — not a command at all;
#   · ONE command invoked by ABSOLUTE PATH (`/usr/bin/env`): bash never looks a word containing a
#     slash up as a function, so this call cannot be hijacked;
#   · abort via `${x:?msg}` — a PARAMETER EXPANSION, not a command. A non-interactive shell writes
#     msg to stderr and exits non-zero. `exit`, `echo`, `printf`, `[`, `test` and `set` are all
#     shadowable and are therefore unusable at this point.
# WHY /usr/bin/env AND NOT `${!BASH_FUNC_@}`: bash imports BASH_FUNC_* out of the environment and
# then DELETES the shell variable, so the prefix expansion is EMPTY in the child while the environ
# still carries the entry (measured, bash 3.2.57 / Apple). The environ is the only place the
# channel is visible, and /usr/bin/env is the only unhijackable way to read it.
_AX_PF_LABEL="release_anchor.sh"
_AX_PF_ENV="$(/usr/bin/env)"
case "$_AX_PF_ENV" in
    "") _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_UNVERIFIABLE — /usr/bin/env produced no output, so this gate cannot tell whether the environment carries exported shell functions. It is the one read that cannot be hijacked; without it, unknown never passes."} ;;
esac
case "$_AX_PF_ENV" in
    *BASH_FUNC_*) _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_HOSTILE — the environment carries an exported shell function (BASH_FUNC_*). bash imports it BEFORE this script's first line, so it replaces a command this gate runs: measured, an exported set/[ turned this gate's honest exit into exit 0. Unset it (unset -f <name>) and re-run."} ;;
esac
case "${BASH_ENV:-}${ENV:-}" in
    ?*) _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_HOSTILE — BASH_ENV/ENV is set, so every non-interactive bash this gate starts would source that file before running the gate's own code. Unset it and re-run."} ;;
esac
unset _AX_PF_ENV _AX_PF_NULL _AX_PF_DIE _AX_PF_LABEL
# ── ROUND 5 / P1-1+P1-2: HERMETIC RUNTIME BOOTSTRAP (A) — DELIBERATELY DUPLICATED ────
# (TD-2026-07-30-P1-hermetic-runtime; the full argument lives in the header of
#  practices/scripts/lib/release_anchor.sh.) THE RATCHET MAY NOT INHERIT ITS OWN RUNTIME.
# Measured: an exported `git` function rewrote the anchor pin; an exported `pwd` moved the pin's
# root to a foreign repo; an exported `python3` turned a FAILING guard into exit 0; GIT_DIR/
# GIT_WORK_TREE made every read describe a CLEAN shadow checkout of a DIRTY tree. All of it
# arrives through the environment, so it must die BEFORE the first git call, BEFORE any `source`,
# and BEFORE this script computes its own directory with `cd`/`pwd` — which is exactly why these
# lines cannot live in a sourced file. The duplication IS the bootstrap.
_AX_HRM_LABEL="release_anchor.sh"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=0
# ROUND 6 / P1-1(b): the list is now EVERY name any entry invokes anywhere, enumerated by
# grepping this catalog's own gate files — including the shell keyword-lookalikes that round 5
# omitted and that the reviewer weaponised (`set`, `[`, `test`, `printf`, `exit`, `exec`, `read`,
# `trap`, `shift`, `local`, `eval`). The pure-keyword preflight above already refuses ANY exported
# function, so this list is belt-and-braces — but a shortfall here used to BE the hole, and a list
# that is merely "what we remembered" is what round 5 shipped.
_AX_HRM_DEPS="git bash sh python python3 env cd pwd command builtin printf echo eval exec read \
test declare unset export local source grep egrep fgrep sed awk cut tr sort uniq head tail wc \
find ls cat cp mv rm mkdir rmdir mktemp dirname basename date shasum sha256sum md5sum xargs tee \
true false set exit return trap shift getopts times umask ulimit wait kill jobs let shopt \
enable alias unalias type hash caller readonly typeset mapfile readarray realpath readlink stat \
chmod touch diff cmp od base64 seq expr sleep tar gzip curl jq yq printenv id whoami uname \
install"
# Glob-metacharacter names (`[`, `[[`) cannot ride the unquoted split above — they would be
# read as bracket expressions by pathname expansion — so they are appended QUOTED at each use.
_AX_HRM_DEPS_Q="[ [["
_AX_HRM_BAD=""
for _ax_hn in $_AX_HRM_DEPS "[" "[["; do
    declare -F "$_ax_hn" >/dev/null 2>&1 && _AX_HRM_BAD="$_ax_hn"
done
if [ -n "$_AX_HRM_BAD" ]; then
    { echo "$_AX_HRM_LABEL: HELPER_FUNCTION_INJECTED — this shell already defines a function named"
      echo "  '$_AX_HRM_BAD', and that is a COMMAND THIS GATE INVOKES. bash imports exported"
      echo "  functions across \`bash script.sh\`, so the definition silently replaces the program"
      echo "  every check below runs — measured: an exported \`git\` rewrote the release-anchor pin,"
      echo "  an exported \`python3\` turned a failing guard into exit 0, an exported \`pwd\` moved the"
      echo "  pin's root to a foreign repository. Unset it (\`unset -f $_AX_HRM_BAD\`) and re-run."; } >&2
    exit $_AX_HRM_EXIT
fi
for _ax_hn in $_AX_HRM_DEPS "[" "[["; do unset -f "$_ax_hn" 2>/dev/null || true; done
# Any exported shell function that SURVIVED the scrub above is refused too — the enumeration is a
# list of what we know we call, and an unknown runtime is not a safe one. Names in this catalog's
# own namespaces are reported as HELPER_FUNCTION_INJECTED instead, because that is what they are
# and because the round-4 policy checks own that vocabulary.
_AX_HRM_FUNCS="$(/usr/bin/env | sed -n 's/^BASH_FUNC_\([A-Za-z_][A-Za-z0-9_]*\).*/\1/p' | tr '\n' ' ')"
if [ -n "${BASH_ENV:-}${ENV:-}" ]; then
    { echo "$_AX_HRM_LABEL: HERMETIC_ENV_HOSTILE — the environment sets BASH_ENV/ENV, which EVERY"
      echo "  non-interactive bash this gate starts would source before running the gate's own"
      echo "  code. That is code the gate never read, executing inside the gate. Unset it and"
      echo "  re-run — fail-closed on purpose: an unknown runtime is not a safe one."; } >&2
    exit $_AX_HRM_EXIT
elif [ -n "$_AX_HRM_FUNCS" ]; then
    case " $_AX_HRM_FUNCS " in
        *" ax_"*|*" _ax_"*|*" pp_"*)
            { echo "$_AX_HRM_LABEL: HELPER_FUNCTION_INJECTED — the environment carries an exported"
              echo "  shell function in this catalog's own namespace: ${_AX_HRM_FUNCS}"
              echo "  Those names ARE the ratchet policy — which commit the anchor is, whether an"
              echo "  absence is an honest bootstrap, which refs ship work. A definition arriving"
              echo "  from outside replaces the policy with the caller's."; } >&2 ;;
        *)
            { echo "$_AX_HRM_LABEL: HERMETIC_ENV_HOSTILE — the environment carries exported shell"
              echo "  function(s): ${_AX_HRM_FUNCS}"
              echo "  An exported function replaces a command this gate calls, after every check it"
              echo "  performs. The enumerated dependency list above is what we know we invoke; a"
              echo "  runtime carrying definitions we did not enumerate is refused rather than"
              echo "  assumed harmless. Unset them and re-run."; } >&2 ;;
    esac
    exit $_AX_HRM_EXIT
fi
# The WHOLE GIT_* family, not a denylist of the ones we thought of: every one of GIT_DIR,
# GIT_WORK_TREE, GIT_COMMON_DIR, GIT_OBJECT_DIRECTORY, GIT_ALTERNATE_OBJECT_DIRECTORIES,
# GIT_INDEX_FILE, GIT_NAMESPACE, GIT_CEILING_DIRECTORIES, GIT_CONFIG*, GIT_EXEC_PATH redirects
# what `git -C <path>` actually reads. GIT_NO_REPLACE_OBJECTS is scrubbed too and re-set below:
# inherited as 0 it RE-ENABLES replacement refs.
for _ax_hn in ${!GIT_@}; do unset "$_ax_hn" 2>/dev/null || true; done
unset BASH_ENV ENV GIT_DIR GIT_WORK_TREE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY GIT_INDEX_FILE \
    GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_NAMESPACE GIT_CEILING_DIRECTORIES GIT_CONFIG \
    GIT_CONFIG_GLOBAL GIT_CONFIG_SYSTEM GIT_CONFIG_NOSYSTEM GIT_CONFIG_COUNT GIT_EXEC_PATH \
    GIT_DISCOVERY_ACROSS_FILESYSTEM 2>/dev/null || true
export GIT_NO_REPLACE_OBJECTS=1
# git and python3 are resolved ONCE, to absolute paths, from a PATH stripped of relative entries
# (a `.` on PATH is a shim in whatever directory the gate happens to run from). The inherited
# order is otherwise preserved, because forcing system directories here would silently swap the
# interpreter that carries PyYAML and make whole guards skip — a fail-open dressed as hardening.
unset AX_GIT_BIN AX_PY_BIN
_AX_HRM_PATH=""; _ax_hifs="$IFS"; IFS=:
for _ax_hd in $PATH; do
    case "$_ax_hd" in /*) ;; *) continue ;; esac
    [ -d "$_ax_hd" ] || continue
    _AX_HRM_PATH="${_AX_HRM_PATH:+$_AX_HRM_PATH:}$_ax_hd"
done
IFS="$_ax_hifs"; unset _ax_hifs _ax_hd
AX_GIT_BIN="$(PATH="$_AX_HRM_PATH" command -v git 2>/dev/null || true)"
AX_PY_BIN="$(PATH="$_AX_HRM_PATH" command -v python3 2>/dev/null || true)"
# ── ROUND 6 / P1-2: A PATH IS NOT AN IDENTITY ───────────────────────────────────────
# INVARIANT: a tool this gate runs must SAY WHAT IT IS. `-f`/`-x` FOLLOW SYMLINKS and assert
# nothing about the program: MEASURED — a symlink named python3 pointing at /usr/bin/true passed
# every lexical/`-x` test and turned the recency guard's entire python body into exit 0 (honest
# baseline 1). So each tool is (a) canonicalised through its real directory, (b) refused if it
# lives inside the tree under audit, and (c) made to IDENTIFY ITSELF by being RUN — `git --version`
# must produce a git version banner, python3 must print a python self-report under `-I -S`. A
# /usr/bin/true symlink prints nothing and is refused.
# The PYTHON* family is scrubbed for the same reason the GIT_* family is: PYTHONPATH /
# PYTHONHOME / PYTHONEXECUTABLE / PYTHONSTARTUP redirect what the interpreter IS before a single
# line of ours runs — measured, a PYTHONPATH sitecustomize.py doing `os._exit(0)` skipped the whole
# python guard (honest baseline 1). Scrubbing is belt; `-I -S` at every ratchet call site is braces.
for _ax_hn in ${!PYTHON@}; do unset "$_ax_hn" 2>/dev/null || true; done
unset PYTHONPATH PYTHONHOME PYTHONSTARTUP PYTHONEXECUTABLE PYTHONUSERBASE PYTHONWARNINGS \
    PYTHONIOENCODING PYTHONMALLOC PYTHONBREAKPOINT PYTHONDEVMODE PYTHONPYCACHEPREFIX \
    PYTHONOPTIMIZE PYTHONVERBOSE PYTHONINSPECT PYTHONCASEOK PYTHONNOUSERSITE 2>/dev/null || true
export PYTHONDONTWRITEBYTECODE=1
for _ax_hn in "git=$AX_GIT_BIN" "python3=$AX_PY_BIN"; do
    if [ "${_ax_hn%%=*}" = "python3" ] && [ "$_AX_HRM_NEED_PY" != "1" ]; then continue; fi
    _ax_hb="${_ax_hn#*=}"
    if [ -z "$_ax_hb" ] || [ "${_ax_hb#/}" = "$_ax_hb" ] || [ ! -f "$_ax_hb" ] || [ ! -x "$_ax_hb" ]; then
        { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — ${_ax_hn%%=*} did not resolve to an"
          echo "  executable regular file on an absolute path (got '${_ax_hb:-<nothing>}')."
          echo "  This gate runs that program to decide whether a release may ship; it will not"
          echo "  guess, and it will not fall back to whatever the inherited PATH offers."; } >&2
        exit $_AX_HRM_EXIT
    fi
    # (a) canonicalise: the DIRECTORY is resolved with `builtin pwd -P`, so a symlinked directory
    #     on the way to the tool cannot disguise where it lives.
    _ax_hdir="$(builtin cd "$(dirname "$_ax_hb")" 2>/dev/null && builtin pwd -P)" || _ax_hdir=""
    if [ -z "$_ax_hdir" ]; then
        { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — the directory of ${_ax_hn%%=*} ('$_ax_hb')"
          echo "  could not be canonicalised, so this gate cannot say where the program it is about"
          echo "  to run actually lives."; } >&2
        exit $_AX_HRM_EXIT
    fi
    _ax_hb="$_ax_hdir/$(basename "$_ax_hb")"
    # (c) identity by SELF-REPORT — the only statement about a program that a path cannot forge.
    if [ "${_ax_hn%%=*}" = "git" ]; then
        _ax_hver="$("$_ax_hb" --version 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "git version "[0-9]*) AX_GIT_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as git (\`git --version\` said '${_ax_hver:-<nothing>}')."
                  echo "  Lexical absoluteness and -f/-x FOLLOW SYMLINKS and say nothing about the"
                  echo "  program: a symlink named python3 → /usr/bin/true satisfied all of them and"
                  echo "  turned an entire guard into exit 0. A tool this gate runs must say what it is."; } >&2
                exit $_AX_HRM_EXIT ;;
        esac
    else
        _ax_hver="$("$_ax_hb" -I -S -c 'import sys;sys.stdout.write("AXPY %d %d %s %s" % (sys.version_info[0], sys.version_info[1], sys.implementation.name, sys.executable or "-"))' 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as a python3 interpreter under \`-I -S\` (it said"
                  echo "  '${_ax_hver:-<nothing>}'). MEASURED: a symlink named python3 → /usr/bin/true"
                  echo "  passed every path test and silently skipped this gate's whole python body"
                  echo "  (exit 0 where the honest answer was 1). Identity is what the program says."; } >&2
                exit $_AX_HRM_EXIT ;;
        esac
    fi
done
export AX_GIT_BIN AX_PY_BIN
# Ratchet-internal python ALWAYS runs isolated: -I ignores PYTHON* env + user site + cwd on
# sys.path, -S skips site entirely, which is what kills a sitecustomize.py payload. Exported so
# every call site in this catalog spells it the same way. NOT usable for the checklist parser,
# which needs PyYAML out of site-packages — that one call site uses -E and is preceded by an
# explicit `import yaml` capability probe that BLOCKS (never skips). See DECISIONS.md
# TD-2026-07-30-P1-preflight-and-raw-bytes.
export AX_PY_ISO="-I -S"
unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH

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
ax_anchor_worktree_paths_regular ax_anchor_verify_unmoved _ax_anchor_bootstrap_implausible \
ax_anchor_bind_repo ax_ratchet_toolchain_authentic ax_ratchet_toolchain_paths \
ax_ratchet_filters_absent"

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
#   ROUND 5 (P1-1/P1-2): the BINARY is "$AX_GIT_BIN" — an absolute path validated by the hermetic
#   bootstrap — because the bare word `git` resolves through PATH and, worse, through an inherited
#   exported FUNCTION (measured: one rewrote the anchor pin). And when the repo is the root this
#   entry BOUND, the git context is passed EXPLICITLY (--git-dir/--work-tree derived from that
#   root and validated against it) instead of being discovered: `-C` alone would still walk up,
#   and discovery is exactly what GIT_DIR/GIT_WORK_TREE hijack. `-C` is kept FIRST so relative
#   pathspecs keep resolving inside the work tree.
ax_git() {
    local repo="$1"; shift
    if [ -n "${AX_GIT_BOUND_ROOT:-}" ] && [ "$repo" = "$AX_GIT_BOUND_ROOT" ] \
       && [ -n "${AX_GIT_BOUND_DIR:-}" ]; then
        "${AX_GIT_BIN:-git}" -C "$repo" --git-dir="$AX_GIT_BOUND_DIR" \
            --work-tree="$AX_GIT_BOUND_ROOT" --no-replace-objects "$@"
    else
        "${AX_GIT_BIN:-git}" --no-replace-objects -C "$repo" "$@"
    fi
}

# ax_anchor_bind_repo <repo_root> <label>
#   ROUND 5 / P1-1, the function form of hermetic-bootstrap part B, for callers that discover a
#   root LATER than their own bootstrap (a sandbox, a --root override that turns out to be a real
#   work tree). 0 = bound (or not a git tree at all, which is the relocated-copy sandbox);
#   1 = GIT_CONTEXT_REDIRECTED, blocking for the caller.
#   The bound pair is a PLAIN shell variable on purpose — exporting it would recreate the
#   environment-supplied git context this whole round removes.
ax_anchor_bind_repo() {
    local repo="$1" label="$2" can top
    AX_GIT_BOUND_ROOT=""
    AX_GIT_BOUND_DIR=""
    "${AX_GIT_BIN:-git}" --no-replace-objects -C "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    can="$(builtin cd "$repo" 2>/dev/null && builtin pwd -P)" || can=""
    top="$("${AX_GIT_BIN:-git}" --no-replace-objects -C "$repo" rev-parse --show-toplevel 2>/dev/null)"
    top="$(builtin cd "${top:-/nonexistent}" 2>/dev/null && builtin pwd -P)" || top=""
    if [ -z "$can" ] || [ "$can" != "$top" ]; then
        {
            echo "${label}: GIT_CONTEXT_REDIRECTED — the git work tree answering reads for"
            echo "    ${repo}"
            echo "  is '${top:-<unresolvable>}'. A gate that reads one repository while verifying"
            echo "  another has verified nothing: with the context aimed at a clean shadow checkout"
            echo "  of the same commit, a DIRTY tree reports the clean-tree fingerprint constant."
        } >&2
        return 1
    fi
    AX_GIT_BOUND_ROOT="$can"
    AX_GIT_BOUND_DIR="$("${AX_GIT_BIN:-git}" --no-replace-objects -C "$can" rev-parse --absolute-git-dir 2>/dev/null)"
    [ -n "$AX_GIT_BOUND_DIR" ] || { AX_GIT_BOUND_ROOT=""; return 1; }
    return 0
}

# ax_ratchet_toolchain_paths — the ratchet's OWN implementation files, in one place so the runner,
# both ratcheting guards and the push gate cannot disagree about what "the toolchain" is.
#   ROUND 6 / P1-3(b): the list was SIX files and EXCLUDED the two ratcheting guards and the guard
#   runner — i.e. three of the programs that actually decide whether a release may ship were not on
#   the list of programs whose bytes are checked. It is nine now, and this function is the single
#   definition so the runner, both guards and the push gate cannot disagree about what "the
#   toolchain" is.
ax_ratchet_toolchain_paths() {
    printf '%s\n' \
        "practices/scripts/lib/release_anchor.sh" \
        "practices/scripts/lib/tree_fingerprint.py" \
        "practices/scripts/verify-completion.sh" \
        "practices/evals/completion_checklist_recency_guard.sh" \
        "practices/evals/evidence_quote_spotcheck_guard.sh" \
        "practices/evals/manifest_snapshot_integrity_guard.sh" \
        "practices/evals/run-all-guards.sh" \
        ".githooks/pre-push" \
        ".githooks/pre-push-lib.sh"
}

# ax_ratchet_filters_absent <repo_root> <label> <rel>...
#   ROUND 6 / P1-4. INVARIANT (β): ANY CLAIM ABOUT BYTES MUST BE MADE ON RAW BYTES, NEVER THROUGH A
#   FILTERED VIEW. git's clean/smudge machinery sits between the working file and every content
#   answer git gives: `git diff`, `git status`, `git hash-object` (without --no-filters) and
#   `git diff HEAD --binary` all report the CLEAN-FILTER OUTPUT, not what is on disk.
#   MEASURED (the reviewer's axmask reproduction, re-run here): with
#       git config filter.axmask.clean 'cat <the committed copy>'
#       $GIT_DIR/info/attributes:  practices/scripts/lib/tree_fingerprint.py filter=axmask
#   a working tree_fingerprint.py carrying appended attacker bytes produced
#       git diff --quiet HEAD -- <path>                       → rc 0   ("identical")
#       git hash-object      -- <path>                        → 79e18672…  (= the HEAD blob)
#       git hash-object --no-filters -- <path>                → 3627bc8c…  (the truth)
#   so ax_ratchet_toolchain_authentic (which used `git diff --quiet`) returned 0 on a tampered
#   toolchain. The byte comparison is fixed below; THIS function is the second answer, because
#   "we passed --no-filters at every site" is a claim about every present and future call site:
#   a live root that DECLARES a clean/smudge filter, carries a non-empty $GIT_DIR/info/attributes,
#   or attaches a filter attribute to any ratchet-critical path is refused outright. A legitimate
#   release of this catalog has none of the three.
#   0 = no filtering machinery in sight; 1 = GIT_FILTERS_PRESENT (blocking).
#   No-op on a non-git root (the relocated-copy sandbox), which has no config to declare.
ax_ratchet_filters_absent() {
    local repo="$1" label="$2"; shift 2
    local decl attrs gitdir rel out bad=0
    "${AX_GIT_BIN:-git}" --no-replace-objects -C "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    decl="$(ax_git "$repo" config --get-regexp '^filter\..*\.(clean|smudge)$' 2>/dev/null)"
    if [ -n "$decl" ]; then
        {
            echo "${label}: GIT_FILTERS_PRESENT — this repository DECLARES git content filters:"
            echo "$decl" | sed 's/^/    /'
            echo "  A clean filter sits between the file on disk and every content answer git gives."
            echo "  Measured: with one installed, a tampered ratchet file read as byte-identical to"
            echo "  its committed blob (\`git diff --quiet\` rc 0) while its raw bytes differed."
            echo "  A released catalog has no reason to carry one. Remove it (git config --unset"
            echo "  filter.<name>.clean/.smudge) and re-run."
        } >&2
        bad=1
    fi
    gitdir="$(ax_git "$repo" rev-parse --absolute-git-dir 2>/dev/null)"
    if [ -n "$gitdir" ] && [ -s "$gitdir/info/attributes" ]; then
        {
            echo "${label}: GIT_FILTERS_PRESENT — \$GIT_DIR/info/attributes is non-empty."
            echo "    ${gitdir}/info/attributes"
            echo "  That file is NOT tracked and NOT reviewed, and it is where a filter is attached"
            echo "  to a path without touching a single committed byte. Empty it and re-run."
        } >&2
        bad=1
    fi
    for rel in "$@"; do
        out="$(ax_git "$repo" check-attr filter -- "$rel" 2>/dev/null)"
        case "$out" in
            *": filter: unspecified"|"") ;;
            *)  {
                    echo "${label}: GIT_FILTERS_PRESENT — a filter attribute is attached to a"
                    echo "  ratchet-critical path:"
                    echo "    ${out}"
                    echo "  Every byte claim this gate makes about that path would be a claim about the"
                    echo "  filter's OUTPUT, not about the file."
                } >&2
                bad=1 ;;
        esac
    done
    return "$bad"
}

# ax_ratchet_toolchain_authentic <repo_root> <label> <rev> <rel>...
#   ROUND 5 / P1-3. THE MODE OF A FILE IS NOT ITS CONTENTS. Every existing check on these paths
#   asks whether they are regular files (lstat + git mode); none asked what they SAY. A
#   regular-file tree_fingerprint.py edited to `print("nogit")` satisfied all of them, and the
#   runner and the verifier then shared that one compromised implementation — measured: the push
#   gate went from exit 1 (AUDIT_FINGERPRINT_MISMATCH on a hand-authored line) to exit 0.
#   So the working-tree BYTES of each toolchain file must equal what git records for that path at
#   <rev> (the sha under audit). `git diff --quiet <rev> -- <path>` is used rather than a hash
#   comparison so that git's own filters/eol handling are applied on both sides.
#   0 = every path authentic; 1 = RATCHET_TOOLCHAIN_MODIFIED / _UNVERIFIABLE (blocking).
#   No-op when <repo_root> is not a git work tree (the relocated-copy sandbox [87] runs there and
#   has no history to compare against; such a tree is also unpushable).
#   COST, disclosed: an UNCOMMITTED edit to the ratchet's own code now blocks the gate that would
#   certify it. That is the intended shape — the ratchet may not authenticate itself from an
#   implementation nobody has committed — and it means toolchain work is "commit, then verify".
ax_ratchet_toolchain_authentic() {
    local repo="$1" label="$2" rev="$3"; shift 3
    local rel bad=0 rc want have
    "${AX_GIT_BIN:-git}" --no-replace-objects -C "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    # ROUND 6 / P1-4: the comparison is on RAW BYTES. It used to be `git diff --quiet <rev> -- <rel>`
    # "so that git's own filters/eol handling are applied on both sides" — which is precisely the
    # defect: a `filter.<n>.clean` that echoes the committed copy makes ANY working file compare
    # equal to its blob (measured, rc 0 on a tampered tree_fingerprint.py). `hash-object
    # --no-filters` hashes the file as it is on disk, with no clean filter and no eol conversion,
    # and is compared to the blob id git records at <rev>. Two ids, both raw, no filtered view.
    ax_ratchet_filters_absent "$repo" "$label" "$@" || bad=1
    for rel in "$@"; do
        want="$(ax_git "$repo" rev-parse --verify --quiet "${rev}:${rel}" 2>/dev/null)"
        have="$(ax_git "$repo" hash-object --no-filters -t blob -- "$rel" 2>/dev/null)"
        rc=1
        if [ -z "$want" ] || [ -z "$have" ]; then rc=2; fi
        [ -n "$want" ] && [ "$want" = "$have" ] && continue
        {
            if [ "$rc" -eq 1 ]; then
                echo "${label}: RATCHET_TOOLCHAIN_MODIFIED — ${rel} in the working tree is NOT the"
                echo "  ${rel} that git records at ${rev} (raw ${have:-<unreadable>} vs ${want:-<absent>})."
            else
                echo "${label}: RATCHET_TOOLCHAIN_UNVERIFIABLE — ${rel} could not be compared against"
                echo "  ${rev} on RAW BYTES (blob at rev: ${want:-<absent>}; on disk: ${have:-<unreadable>}),"
                echo "  so the gate cannot tell which implementation it is about to run. Unknown never"
                echo "  passes."
            fi
            echo "  THESE ARE THE FILES THAT DECIDE WHETHER A RELEASE MAY SHIP: the anchor policy,"
            echo "  the tree fingerprint, the runner, the push gate and the hook. Every other check"
            echo "  on them asks whether they are REGULAR FILES; this one asks what they SAY,"
            echo "  because a regular-file tree_fingerprint.py rewritten to print a constant passed"
            echo "  all of the former and was then used by BOTH the runner that writes the evidence"
            echo "  and the verifier that recomputes it — one implementation authenticating itself."
            echo "  Commit the change (or restore the file) and re-run: the ratchet does not"
            echo "  certify anything from an implementation that exists only in someone's tree."
        } >&2
        bad=1
    done
    return "$bad"
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
    [ -n "${AX_GIT_BIN:-}" ] || command -v git >/dev/null 2>&1 || return 0
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
    # `builtin cd`/`builtin pwd` (ROUND 5 / P1-2): an exported `pwd` function decided this value
    # in the reviewer's measurement (PIN_ROOT=/foreign/repo), which is what the pin is keyed on.
    canon="$(builtin cd "$repo" 2>/dev/null && builtin pwd -P)" || canon=""
    if [ -n "${AX_RELEASE_ANCHOR_SHA:-}" ] && [ -n "$canon" ] \
       && [ "${AX_RELEASE_ANCHOR_ROOT:-}" = "$canon" ]; then
        AX_ANCHOR_PIN_APPLIED=1
        # AX_RELEASE_ANCHOR_REF is part of the pin and was previously exported but never checked
        # (registered P2, closed here because it is one clause of the same comparison): the pin
        # names WHICH REF was resolved, and a pin that agrees on the sha while disagreeing on the
        # ref is not the resolution this run was held to.
        if [ "${AX_RELEASE_ANCHOR_SHA}" != "$AX_ANCHOR_SHA" ] \
           || [ "${AX_RELEASE_ANCHOR_KIND:-}" != "$AX_ANCHOR_KIND" ] \
           || [ "${AX_RELEASE_ANCHOR_REF:-}" != "$AX_ANCHOR_REF" ]; then
            AX_ANCHOR_PIN_MISMATCH="pinned ${AX_RELEASE_ANCHOR_KIND:-?}[${AX_RELEASE_ANCHOR_REF:-?}]=${AX_RELEASE_ANCHOR_SHA} but the ref now resolves to ${AX_ANCHOR_KIND}[${AX_ANCHOR_REF}]=${AX_ANCHOR_SHA}"
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
    canon="$(builtin cd "$repo" 2>/dev/null && builtin pwd -P)" || canon=""
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
    local repo="$1" label="$2" refs rc
    [ -n "${AX_GIT_BIN:-}" ] || command -v git >/dev/null 2>&1 || return 0
    ax_git "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    # ROUND 5 fail-closed sweep: `refs=$(… 2>/dev/null)` made an ENUMERATION FAILURE
    # indistinguishable from "there are none" — i.e. the one state an attacker can manufacture
    # (break the ref store, or make the command fail) read as clean. An unanswerable question is
    # not an answer.
    refs="$(ax_git "$repo" for-each-ref --format='%(refname)' refs/replace/ 2>/dev/null)"
    rc=$?
    if [ "$rc" -ne 0 ]; then
        {
            echo "${label}: ANCHOR_REPLACE_REFS_PRESENT — the replacement-ref enumeration itself"
            echo "  FAILED (git exited ${rc}), so this run cannot tell whether refs/replace/* is"
            echo "  empty. That question decides whether every ancestry / bootstrap / sha comparison"
            echo "  below is being answered out of a fabricated object graph, and an unanswerable"
            echo "  question fails closed."
        } >&2
        return 1
    fi
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
    # ROUND 5 fail-closed sweep: an unreadable HEAD used to mean "nothing to compare, carry on".
    # On a repository that HAS an anchor, a HEAD that cannot be read is not an empty repo — it is
    # a repository whose state we could not establish, and the ancestry layer is the one that
    # refuses a forged anchor. Unknown blocks.
    if [ -z "$head" ]; then
        {
            echo "${label}: ANCHOR_NOT_ANCESTOR — an anchor resolved (${AX_ANCHOR_KIND} ="
            echo "  ${AX_ANCHOR_SHA}) but HEAD could not be read, so 'does this release descend from"
            echo "  the release it claims to ratchet against' has no answer. The ancestry layer is"
            echo "  what refuses an anchor aimed at a synthetic commit, so it fails closed."
        } >&2
        return 1
    fi
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
    local repo="$1" label="$2" rel="$3" hist last_touch reason="" rc
    hist="$(ax_git "$repo" rev-list --max-count=1 "$AX_ANCHOR_SHA" -- "$rel" 2>/dev/null)"
    rc=$?
    # ROUND 5 fail-closed sweep: a FAILED rev-list produced an empty `hist`, which this function
    # reads as "the anchor's history never contained the path" — the bootstrap SKIP. The one
    # branch that lets a ratchet stand down was reachable by making a git command fail.
    if [ "$rc" -ne 0 ]; then
        {
            echo "${label}: ANCHOR_BOOTSTRAP_IMPLAUSIBLE — ${rel} is absent at the release anchor"
            echo "  (${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}) and the history walk that decides whether"
            echo "  that absence is honest FAILED (git exited ${rc}). The honest-absence branch is"
            echo "  the one that switches a ratchet off, so it is never taken on a failed probe."
        } >&2
        return 1
    fi
    if [ -n "$hist" ]; then
        reason="the anchor's OWN history contains ${rel} (last touched by ${hist}), so its"$'\n'"    absence at ${AX_ANCHOR_SHA} is a DELETION, not a never-existed"
    else
        last_touch="$(ax_git "$repo" rev-list --max-count=1 HEAD -- "$rel" 2>/dev/null)"
        # Same fail-closed rule for the redundant probe: a failed walk is not a clean one.
        if [ $? -ne 0 ]; then
            {
                echo "${label}: ANCHOR_BOOTSTRAP_IMPLAUSIBLE — ${rel} is absent at the anchor and the"
                echo "  second, redundant history probe (from HEAD's walk) FAILED. Two probes exist so"
                echo "  a trick that fools one still has to fool the other; a probe that cannot run"
                echo "  has not been fooled, it has been silenced, and silence is not absence."
            } >&2
            return 1
        fi
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
