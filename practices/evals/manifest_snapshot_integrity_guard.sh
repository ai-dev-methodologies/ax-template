#!/usr/bin/env bash
# practices/evals/manifest_snapshot_integrity_guard.sh — BACKLOG P2-57 / PRD-final-4 W1b.
#
# WHY THIS GUARD EXISTS
# ---------------------
# The protected-anchor ratchet in evidence_quote_spotcheck_guard.sh pins 64 template
# citations to the TEXT OF SNAPSHOT BODIES. That pin is only as strong as the immutability of
# those bodies — and until this guard landed, NOTHING checksummed them. The wave-start census
# measured the consequence: of the 91 manifest ids that have a committed
# `<id>.snapshot.md` on disk, **71 had a `sha`/`bytes` pair that did not describe the file it
# claims to describe** — and no gate looked. Three of them (recharts-2026-05,
# next-intl-2026-05, kakao-postcode-2026-05) shared ONE sha across three different byte
# counts, which is not drift: a sha256 cannot be the digest of three different byte strings,
# so at least two of those three records were never computed from anything. stripe-billing
# recorded 1657 bytes for a 2089-byte file. Pinning 46 further identities to bodies in that
# state would have been a PAPER ratchet: the quote lock would hold while the thing quoted
# stayed freely editable.
#
# WHAT IT CHECKS — THREE DOMAINS, NEVER A CROSS-DOMAIN EQUALITY
# ------------------------------------------------------------
# The refresh transaction (curl → committed deterministic extractor → snapshot + manifest +
# receipts) is verified as a CHAIN of three checks in three DISTINCT digest domains. They are
# deliberately not collapsed into one comparison: a single "does everything agree" digest
# would be satisfiable by recomputing it, which is what a doctored refresh does.
#
#   (a) FILE domain — for EVERY manifest id in EITHER catalog whose `<id>.snapshot.md` exists,
#       the manifest's `sha` and `bytes` must equal `shasum -a 256` / `wc -c` of the WHOLE FILE
#       on disk. Divergence is MANIFEST_FILE_DIVERGED (exit 1) unless the (catalog, id) is
#       carried by the shrink-only allowlist described below.
#
#   (b) BODY domain — for every W1-TOUCHED id, the sha256 the snapshot's own header RECORDS for
#       its body must equal the sha256 recomputed from the file with the header stripped. The
#       header/body boundary is the FIRST literal `---\n\n` in the file (Lane A's authoring
#       convention, documented in docs/final4-laneA-handoff.md §9). Note the header records the
#       BODY's digest, never the file's: a self-referential file-sha inside the file it hashes
#       is unverifiable by construction.
#
#   (c) RECEIPT domain — that same body-sha must equal the id's `kind: assembly` row in
#       practices/upstream/_FETCH-RECEIPTS.yaml (or, for a single-URL id with no assembly row,
#       that URL's `kind: fetch` row), AND every per-URL receipt id the assembly references must
#       exist in the ledger. Anything else is RECEIPT_MISSING (exit 2). This is the check that
#       makes a fetch the ONLY legal way to refresh a snapshot: editing a body and its manifest
#       entry together — the natural way to launder a doctored snapshot past check (a) — leaves
#       no receipt describing the new bytes, and there is no way to write one without recording
#       a URL, an HTTP status and a fetched_at.
#
# CHAIN SCOPE IS DERIVED — FROM THE LEDGER **AND** FROM THE RELEASE ANCHOR
# -----------------------------------------------------------------------
# The chain runs on the union of two derived sets, never a hardcoded list (which would rot the
# moment a later wave refreshes another id, and would be the guard's author asserting coverage
# rather than committed artifacts showing it):
#   · every (catalog, snapshot_id) carrying a `kind: assembly` receipt row; PLUS
#   · every id that DIFFERS FROM THE RELEASE ANCHOR — its `<id>.snapshot.md` differs from
#     `git show <ANCHOR>:<file>`, or its manifest `sha`/`bytes` entry differs from the ANCHOR's
#     manifest (P1-2a). A changed id with no assembly row dies RECEIPT_MISSING.
# Why the second half exists: deriving scope from the assembly rows alone let the LEDGER decide
# what gets verified, and the ledger is editable. The reviewer refreshed an allowlisted,
# non-assembly id (practices-react::cwv-2026) — edit body, sync manifest to the new file, delete
# the allowlist row — and (a) passed, the shrink was legal, and the chain never looked. Ids
# byte-identical to the released tree stay exempt, which is what keeps the ~20 legacy-clean,
# pre-receipt ids passing without a re-fetch.
#
# ANCHOR RATCHET — FLOORS AND BASELINES LIVE IN THE PREVIOUS RELEASE
# -----------------------------------------------------------------
# (P1-2 / P1-3, cross-family reviewer 2026-07-30; TD-2026-07-30-P1-anchor-ratchet.) Every
# "frozen" surface here used to be re-read from the same mutable tree that the commit under
# review is editing, so a coherent edit of all of them at once tripped nothing. Three checks now
# compare against `git show <ANCHOR>:<path>` (exit 4 for all three):
#   · RECEIPT_LEDGER_MUTATED — every row present in the ANCHOR's _FETCH-RECEIPTS.yaml must still
#     be present and unchanged; only APPENDS are legal. This kills rewriting an existing assembly
#     row's `body_sha256` to match a doctored body.
#   · BASELINE_MUTATED — `baseline_universe` must EQUAL the anchor's. Enforcing it against itself
#     was self-certifying: adding a newly-diverged id to the baseline satisfied subset-only.
#   · ALLOWLIST_GREW — residual `entries` must be a SUBSET of the anchor's; shrink-only ACROSS
#     RELEASES, so a new divergence can never be suppressed and must pass the full chain.
# Multiple assembly rows per identity are consequently LEGAL (a repeat refresh appends rather
# than rewrites): the chain binds to the LATEST assembly row per id, and append-only keeps every
# earlier row byte-intact, so supersession is auditable instead of a silent overwrite.
#
# WHY THIS IS SOUND (gate ordering): R25 runs with the tree at HEAD, which is AHEAD of
# origin/main — it is the commit being released — so origin/main is genuinely the prior state and
# is not editable by the commit under verification; the pre-push recency guard then binds every
# push to an R25 run at that HEAD. Anchor resolution: origin/main → HEAD (WEAKER: a
# detached/fork-fresh clone; a change already committed locally is present in the anchor itself)
# → unavailable (no git/no commits: the anchor checks WARN and are SKIPPED — inert in a tarball
# export, which is also unpushable, so the released path keeps the gate). An anchor predating a
# surface prints an advisory and skips that half (first-release bootstrap); git history is
# immutable to the working tree, so that state cannot be manufactured by the edit under review.
# FIXTURE ROOTS NEVER ANCHOR — every anchor check is gated on LIVE_ROOT, like the LIVE_MIN_*
# floors, because a fixture isolates one failure mode and has no release history of its own.
# Consequence, disclosed: the anchor checks are therefore NOT fixture-coverable (a fixture has no
# anchor to ratchet against), so their non-vacuity evidence is the live reproductions recorded in
# practices/DECISIONS.md TD-2026-07-30-P1-anchor-ratchet — each reproduced against the released
# copy of this guard (exit 0) and against this one (exit 4 / 2), then restored byte-identically.
#
# RESIDUAL, STATED RATHER THAN PAPERED OVER: a receipt is a SELF-REPORTED record, and no offline
# gate can prove that a curl actually happened. Append-only closes REWRITING a released receipt;
# it does not close APPENDING a fabricated one (doctor the body, sync manifest + header, then
# append a new fetch row and a new assembly row whose digest matches the doctored bytes). What the
# chain therefore guarantees is not "a doctored refresh is impossible" but "a doctored refresh
# leaves a permanent, immutable, reviewable claim in the ledger naming a URL, an HTTP status and a
# fetched_at" — undeniable in the record rather than prevented. Closing the remainder needs
# evidence the tree cannot author (an independent fetch at review time, e.g. the periodic network
# external_url_spot_audit.sh, or a signed transparency log). Two candidate tightenings were
# considered and deliberately NOT added here, because both carry false-positive risk that would
# have to be paid by future legitimate waves: requiring a newly-appended assembly row to cite at
# least one newly-appended fetch row (breaks registering an existing body under a second catalog),
# and freezing `notes:` (breaks documenting a later refresh).
#
# ALLOWLIST — SHRINK-ONLY, REASON-BEARING, NON-REDUNDANT
# -----------------------------------------------------
# practices/evals/manifest_snapshot_integrity_allowlist.yaml. The honest position: for 63
# pre-existing ids the ORIGINALLY-FETCHED body is lost, so the recorded sha/bytes are
# unverifiable-or-fabricated HISTORY that cannot be corrected without a re-fetch. Suppressing
# them is what lets this guard block on everything else today instead of being registered as
# advisory and never promoted. Four mechanics keep that suppression from becoming a hiding
# place, and all four are enforced by this guard rather than by convention:
#   1. `baseline_universe` — the wave-start divergent census (71 entries) is FROZEN in the
#      allowlist. Every entry must be in it; ADDITIONS ARE REJECTED (exit 2). New divergence can
#      therefore never be allowlisted, only fixed.
#   2. unique (catalog, id) keys — a duplicate is exit 2. A count is not an identity.
#   3. NON-REDUNDANCY — an entry whose manifest NOW matches disk is STALE and FAILS (exit 1).
#      Without this, the list could be padded with already-clean ids so that its length stopped
#      describing the residual, and burn-down progress would be invisible.
#   4. per-entry `reason:` — a non-empty string. An unexplained suppression is a suppression
#      nobody will ever revisit.
# Net effect: the list can only shrink, and its length IS the residual (63 at introduction).
#
# NON-VACUITY
# -----------
# Zero ids checked is exit 2, never a green pass. On the live tree two census floors additionally
# apply (LIVE_MIN_IDS / LIVE_MIN_TOUCHED, measured at introduction), so deleting snapshots or
# emptying the receipts ledger cannot turn this gate into a green nothing.
#
# EXIT: 0 PASS · 1 divergence / stale allowlist / body-sha mismatch · 2 structural
#       (RECEIPT_MISSING, allowlist shape or in-tree subset violation, missing inputs, zero scan,
#       RECEIPTS_SELF_UNCHUNKABLE) · 4 cross-release ratchet violation (RECEIPT_LEDGER_MUTATED —
#       rewritten row, deleted row, NON-PREFIX row/chunk order, or an unchunkable ledger on a live
#       root / BASELINE_MUTATED / ALLOWLIST_GREW) — deliberately distinct from 1 and 2 so "the
#       ratchet was rolled back across releases" is never readable as "a body diverged" or "a shape
#       is wrong".
#       · 5 ANCHOR AUTHENTICITY (round 3, TD-2026-07-30-P1-anchor-authenticity) — the anchor itself
#         could not be trusted, as opposed to a ratchet having been rolled back:
#           ANCHOR_NOT_ANCESTOR          the resolved anchor is not an ancestor of HEAD
#           ANCHOR_BOOTSTRAP_IMPLAUSIBLE a path "absent at the anchor" that HAS history there
#           ANCHOR_PATH_NOT_REGULAR      an anchor-critical path is a symlink (mode 120000) at the
#                                        anchor — the anchor side reads GIT OBJECTS while the self
#                                        side reads the FILESYSTEM, and a link makes them disagree
#           ANCHOR_MALFORMED             an anchor payload of the wrong TYPE or unparseable; never
#                                        a silent skip, never conflated with "absent"
#           ANCHOR_DIFF_UNAVAILABLE      `git diff` against the anchor failed on a live root
#         (SELF_PATH_NOT_REGULAR — the working-tree half of the symlink check — is exit 2, because
#         it is an ordinary structural defect of the tree being scanned and runs on EVERY root.)
#
# Usage:
#   bash practices/evals/manifest_snapshot_integrity_guard.sh
#   bash practices/evals/manifest_snapshot_integrity_guard.sh --root DIR
#   bash practices/evals/manifest_snapshot_integrity_guard.sh --allowlist PATH
# ── ROUND 7 / P1-1: PRIVILEGED-MODE RE-EXEC — THIS IS THE FIRST EXECUTABLE TEXT ─────
# (TD-2026-07-30-P1-privileged-startup.) The round-6 preflight below is IN-SCRIPT, and bash
# sources $BASH_ENV BEFORE a script's first line — so ANY in-script sensor is structurally too
# late. MEASURED (cross-family reviewer, round 7): a SELF-ERASING payload
#     builtin unset BASH_ENV ENV; function set(){ builtin exit 0; }
# leaves NOTHING for the preflight to find — BASH_ENV is gone from the environ it reads, and the
# function is NOT exported so no BASH_FUNC_* entry exists — and the first `set -uo pipefail` this
# file runs exits 0. `shopt -s expand_aliases` + an alias is the same class.
# THE FIX IS AT THE INVOCATION BOUNDARY, not in the script: bash PRIVILEGED MODE (-p) does not
# process $BASH_ENV/$ENV and does not import functions from the environment. This entry therefore
# re-executes ITSELF under -p before doing anything else. Measured, bash 3.2.57(1) / Apple:
#     BASH_ENV=<payload> bash  <entry>   → exit 0          (pre-fix; the gate never ran)
#     BASH_ENV=<payload> bash -p <entry> → honest verdict   (payload inert)
# CONSTRUCTS USED, and why each is unhijackable: `case` is a shell KEYWORD (never resolved through
# the function table); $-, ${x-} and ${x:?} are PARAMETER EXPANSIONS, not commands; /usr/bin/env is
# invoked by ABSOLUTE PATH (bash never looks a word containing a slash up as a function).
# `exec` IS shadowable, so the SECOND case re-asserts privileged mode AFTER it: a neutered exec
# falls through to a non-zero abort instead of quietly continuing unprivileged (measured: a
# BASH_ENV defining exec(){ :; } exits 1 here, it does not proceed).
# THE LOOP MARKER IS NOT TRUSTED. AX_PRIV_REEXEC means only "a re-exec was already attempted".
# An attacker who PRESETS it does not skip the re-exec — that branch ABORTS (measured, exit 1).
# It is unset the moment privileged mode holds, so it never reaches a child entry and cannot turn
# into a one-shot disable for the guards this entry launches.
# RESIDUAL, stated rather than hidden: a $BASH_ENV whose first line is `exit 0` ends the shell
# before this line exists (measured, exit 0, nothing printed). No in-script construct can survive
# that. It is the environment-control boundary declared in practices/DECISIONS.md,
# TD-2026-07-30-(ratchet-threat-model) — the same adversary can simply not install the hooks.
case $- in
    *p*) ;;
    *) case "${AX_PRIV_REEXEC-}" in
           1) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"manifest_snapshot_integrity_guard: HERMETIC_PRIVILEGED_UNREACHABLE — a re-exec into bash privileged mode was already attempted and this shell is STILL unprivileged. Either exec is shadowed by a function, or AX_PRIV_REEXEC was preset in the environment to skip the re-exec. Both are refused; nothing in this gate runs unprivileged. Start it from a clean shell."} ;;
           *) case "${BASH:-}" in
                  /*) exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@" ;;
                  *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"manifest_snapshot_integrity_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the running interpreter (BASH) is not an absolute path, so the SAME interpreter cannot be named unambiguously for the privileged re-exec."} ;;
              esac ;;
       esac ;;
esac
case $- in
    *p*) ;;
    *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"manifest_snapshot_integrity_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the re-exec returned instead of replacing this process, which means exec itself is shadowed. Unprivileged execution is refused."} ;;
esac
unset AX_PRIV_REEXEC

# ── ROUND 6 / P1-1: PURE-KEYWORD PREFLIGHT — SECOND, AND BEFORE ANY OVERRIDABLE COMMAND ──
# CORRECTION (ROUND 7): round 6 claimed this block was "the first executable text of all 8
# entries". It was NOT. The two SOURCED libs (practices/scripts/lib/release_anchor.sh:251 and
# .githooks/pre-push-lib.sh:27) ran `export GIT_NO_REPLACE_OBJECTS=1` FIRST, and `export` is an
# ordinary command lookup: MEASURED, an exported export() plus `alias exit=:` produced
# SOURCE_RC=0 for both. Those two exports now sit BELOW this preflight, and the round-7
# privileged re-exec above is what actually executes first.
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
_AX_PF_LABEL="manifest_snapshot_integrity_guard"
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
set -uo pipefail

# ── ROUND 5 / P1-1+P1-2: HERMETIC RUNTIME BOOTSTRAP (A) — DELIBERATELY DUPLICATED ────
# (TD-2026-07-30-P1-hermetic-runtime; the full argument lives in the header of
#  practices/scripts/lib/release_anchor.sh.) THE RATCHET MAY NOT INHERIT ITS OWN RUNTIME.
# Measured: an exported `git` function rewrote the anchor pin; an exported `pwd` moved the pin's
# root to a foreign repo; an exported `python3` turned a FAILING guard into exit 0; GIT_DIR/
# GIT_WORK_TREE made every read describe a CLEAN shadow checkout of a DIRTY tree. All of it
# arrives through the environment, so it must die BEFORE the first git call, BEFORE any `source`,
# and BEFORE this script computes its own directory with `cd`/`pwd` — which is exactly why these
# lines cannot live in a sourced file. The duplication IS the bootstrap.
_AX_HRM_LABEL="manifest_snapshot_integrity_guard"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=1
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
# ── ROUND 6 / P1-2: A PATH IS NOT EVEN A SMOKE TEST ─────────────────────────────────
# INVARIANT: a tool this gate runs must at least ANSWER LIKE THE PROGRAM IT IS SUPPOSED TO BE.
# `-f`/`-x` FOLLOW SYMLINKS and assert nothing at all: MEASURED — a symlink named python3
# pointing at /usr/bin/true passed every lexical/`-x` test and turned the recency guard's entire
# python body into exit 0 (honest baseline 1). So each tool is (a) canonicalised through its real
# directory, (b) refused if it lives inside the tree under audit, and (c) RUN once against a
# fixed challenge — `git --version` must produce a git version banner, python3 must print a
# self-report under `-I -S`. A /usr/bin/true symlink prints nothing and is refused.
# WHAT THAT IS AND IS NOT (corrected, reviewer ROUND 8 / P1-B — the earlier prose called this
# 'identity' and the tool 'authenticated', and it is NEITHER). The challenge is PUBLIC and FIXED,
# so a hostile wrapper forwards it to the real binary, answers correctly, and does what it likes
# with everything else — measured by the reviewer in round 7, which flipped fail_audit_log_missing
# from exit 1 to exit 0. This is therefore a SMOKE TEST for a MIS-RESOLVED tool (a stub, a
# /usr/bin/true symlink, a shim on an inherited PATH), not authentication and not identity. PATH
# executables are DECLARED TRUSTED — see practices/DECISIONS.md TD-2026-07-30-(ratchet-threat-
# model) and docs/BACKLOG.md P2-68 for the external-trust-root work that would change that. The
# code name HERMETIC_TOOL_UNAUTHENTIC is kept because guards, fixtures and DECISIONS entries
# reference it; it means 'did not answer the smoke test', never 'failed authentication'.
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
    # (c) the SMOKE TEST: run it once and read what it says. A path cannot make this statement,
    #     and a hostile wrapper can (it forwards the fixed public challenge) — so this catches a
    #     mis-resolved tool, which is the in-scope class, and nothing beyond it.
    if [ "${_ax_hn%%=*}" = "git" ]; then
        _ax_hver="$("$_ax_hb" --version 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "git version "[0-9]*) AX_GIT_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as git (\`git --version\` said '${_ax_hver:-<nothing>}')."
                  echo "  Lexical absoluteness and -f/-x FOLLOW SYMLINKS and say nothing about the"
                  echo "  program: a symlink named python3 → /usr/bin/true satisfied all of them and"
                  echo "  turned an entire guard into exit 0. A mis-resolved tool is refused here; a"
                  echo "  hostile PATH wrapper is NOT caught by this and is declared out of scope."; } >&2
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
                  echo "  (exit 0 where the honest answer was 1). This is a smoke test for a"
                  echo "  mis-resolved interpreter, not authentication — see DECISIONS."; } >&2
                exit $_AX_HRM_EXIT ;;
        esac
    fi
done
export AX_GIT_BIN AX_PY_BIN
# Ratchet-internal python ALWAYS runs isolated: -I ignores PYTHON* env + user site + cwd on
# sys.path, -S skips site entirely, which is what kills a sitecustomize.py payload. NOT usable for
# the three call sites that need PyYAML out of site-packages (the checklist parser and the two
# ratcheting guards' bodies) — those use -E, which still refuses PYTHONPATH/PYTHONHOME/
# PYTHONSTARTUP, and are preceded by an `import yaml` capability probe that BLOCKS (never skips).
# See DECISIONS.md TD-2026-07-30-P1-preflight-and-raw-bytes for the honest limit that leaves.
# PUBLISHED, NOT CONSUMED IN-TREE: every in-tree call site spells the flags LITERALLY so that
# `grep -n ' -I -S '` finds all of them; this export exists for fork-receiver call sites.
export AX_PY_ISO="-I -S"
unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
SELF_REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd -P)"
# ── HERMETIC RUNTIME BOOTSTRAP (B): bind the git identity to the trusted root ────────
# Part A removed the inherited git context; this derives the real one and REQUIRES it to be the
# root this entry resolved for itself. `git -C <root>` WALKS UP when <root> is not itself a work
# tree, so without this a gate can authenticate a repository that merely CONTAINS the directory it
# is scanning. The derived gitdir/worktree are then passed EXPLICITLY on every call for this root
# (see ax_git), so nothing downstream depends on discovery at all.
# NOT exported, and unset first: a binding that could arrive from the environment would be a
# NEW redirection channel of exactly the kind part A just closed. Every entry derives its own.
unset AX_GIT_BOUND_ROOT AX_GIT_BOUND_DIR
if "$AX_GIT_BIN" -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_hcan="$(builtin cd "$SELF_REPO_ROOT" 2>/dev/null && builtin pwd -P)"
    _ax_htop="$("$AX_GIT_BIN" -C "$SELF_REPO_ROOT" rev-parse --show-toplevel 2>/dev/null)"
    _ax_htop="$(builtin cd "${_ax_htop:-/nonexistent}" 2>/dev/null && builtin pwd -P)"
    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then
        { echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the git work tree answering this gate's"
          echo "  reads is '${_ax_htop:-<unresolvable>}', not the root it was invoked for"
          echo "  ('${_ax_hcan:-$SELF_REPO_ROOT}'). Every head / status / fingerprint / anchor answer would"
          echo "  then describe a different tree than the one being verified — measured: with the"
          echo "  context aimed at a clean shadow checkout, a DIRTY tree reported the clean-tree"
          echo "  fingerprint constant and a clean status."; } >&2
        exit $_AX_HRM_EXIT
    fi
    AX_GIT_BOUND_ROOT="$_ax_hcan"
    AX_GIT_BOUND_DIR="$("$AX_GIT_BIN" -C "$AX_GIT_BOUND_ROOT" rev-parse --absolute-git-dir 2>/dev/null)"
    if [ -z "$AX_GIT_BOUND_DIR" ]; then
        echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the gitdir of '$AX_GIT_BOUND_ROOT' could not" >&2
        echo "  be derived, so no explicit git context can be pinned. Blocking rather than falling" >&2
        echo "  back to discovery, which is the thing being pinned." >&2
        exit $_AX_HRM_EXIT
    fi
    case "$AX_GIT_BIN" in "$AX_GIT_BOUND_ROOT"/*) _ax_htop=repo ;; *) _ax_htop="" ;; esac
    case "$AX_PY_BIN" in "$AX_GIT_BOUND_ROOT"/*) _ax_htop=repo ;; esac
    if [ -n "$_ax_htop" ]; then
        echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — git/python3 resolved to a binary INSIDE the" >&2
        echo "  repository being verified ($AX_GIT_BIN / $AX_PY_BIN). The tree under audit does not" >&2
        echo "  get to supply the programs that audit it." >&2
        exit $_AX_HRM_EXIT
    fi
    unset _ax_hcan _ax_htop
fi


ROOT_OVERRIDE=""
ALLOWLIST_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --allowlist) ALLOWLIST_OVERRIDE="$2"; shift 2 ;;
        --allowlist=*) ALLOWLIST_OVERRIDE="${1#--allowlist=}"; shift ;;
        *) echo "manifest_snapshot_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

REPO_ROOT="${ROOT_OVERRIDE:-$SELF_REPO_ROOT}"
# LIVE_ROOT keyed on the CANONICALIZED physical path, not on "was --root passed?" — the
# evidence_quote_spotcheck_guard round-4 finding: keying on arg presence lets an explicit
# `--root <the actual repo>` resolve to the identical tree while silently dropping every
# live-only floor.
RESOLVED_ROOT="$(cd "$REPO_ROOT" 2>/dev/null && pwd -P)" || {
    echo "manifest_snapshot_integrity_guard: cannot resolve root: $REPO_ROOT" >&2; exit 2; }
LIVE_ROOT=0
[ "$RESOLVED_ROOT" = "$SELF_REPO_ROOT" ] && LIVE_ROOT=1

# ROUND 6 / P2: the interpreter that ANSWERED THE SMOKE TEST (not "authenticated" — the challenge
# is public and fixed, so a wrapper forwards it; see the bootstrap header), probed with the flags
# the call site below uses
# (-E, because the body imports PyYAML). `command -v python3` said only that some file existed.
[ -n "${AX_PY_BIN:-}" ] && "$AX_PY_BIN" -E -c 'import sys' >/dev/null 2>&1 || {
    echo "manifest_snapshot_integrity_guard: HERMETIC_TOOL_UNUSABLE — no python3 that answered the smoke test" >&2
    exit 2; }

# ── RELEASE ANCHOR (P1 anchor-ratchet, TD-2026-07-30-P1-anchor-ratchet) ───────────────
# See the ANCHOR RATCHET header block. Resolution order origin/main (strong: R25 runs at a tree
# AHEAD of it) → HEAD (weaker; detached/fork-fresh only) → unavailable (WARN + skip). Gated on
# LIVE_ROOT exactly like the LIVE_MIN_* floors: FIXTURE ROOTS NEVER ANCHOR, since a fixture
# isolates one failure mode and has no release history of its own.
#
# ROUND 3 (P1-X/P1-Y, TD-2026-07-30-P1-anchor-authenticity): resolution moved into the SINGLE
# helper practices/scripts/lib/release_anchor.sh, shared with evidence_quote_spotcheck_guard and
# with verify-completion.sh's audit writer, so the sha the runner RECORDS is the sha this guard
# RATCHETS AGAINST. Three new blocking codes, fully documented in the helper:
#   · ANCHOR_NOT_ANCESTOR          — refs/remotes/origin/main is an ORDINARY LOCAL REF; aiming it
#                                    at a synthetic commit whose tree merely DROPS the anchored
#                                    paths turns every "absent at the anchor ⇒ bootstrap skip"
#                                    below into a bypass. The anchor must be an ancestor of HEAD.
#   · ANCHOR_BOOTSTRAP_IMPLAUSIBLE — a file that HAS HISTORY IN THIS REPO can never legitimately
#                                    be "absent in the previous release".
#   · ANCHOR_PATH_NOT_REGULAR / SELF_PATH_NOT_REGULAR — the anchor side reads GIT OBJECTS and the
#                                    self side reads the FILESYSTEM. A symlinked ledger makes
#                                    `git show` return a scalar STRING (the target pathname) while
#                                    this process parses the real YAML — and every prefix/chunk
#                                    layer below used to be nested under `isinstance(prior_doc,
#                                    dict)` with no blocking else, so the whole append-only ratchet
#                                    retired itself in silence. Both sides must be regular files.
# SELF_PATH_NOT_REGULAR runs on EVERY root (live and fixture): it inspects the tree in front of it
# rather than a release history, which makes it the fixture-provable half of P1-Y.
ANCHOR_AUTH_EXIT=5     # distinct from 1 (findings) / 2 (structural) / 4 (ratchet violation)
#
# ROUND 4 (P1-1/P1-2/P1-3, TD-2026-07-30-P1-anchor-runtime): rounds 1-3 authenticated WHAT the
# anchor says, WHICH commit it is and HOW its bytes are represented; round 4 found the ratchet's
# own RUNTIME unauthenticated. Two more codes at ANCHOR_AUTH_EXIT, one structural:
#   · ANCHOR_REPLACE_REFS_PRESENT — `git replace` keeps every sha and swaps the OBJECT, so the
#     ancestry / bootstrap / ls-tree reads above can all be answered out of a fabricated history.
#     Every git call now runs --no-replace-objects (+ GIT_NO_REPLACE_OBJECTS=1 exported by the
#     helper, which is what reaches the python subprocesses below), and a tree that carries
#     replacement refs at all is refused.
#   · ANCHOR_REF_MOVED_MIDRUN — single-resolution binding: the R25 runner resolves the anchor
#     ONCE and exports it; this guard re-reads the ref at its own moment of use and must agree,
#     then re-reads it again after its work (so a standalone run observes it twice too).
#   · HELPER_PATH_NOT_REGULAR (EXIT 2, structural) — the inline preflight below.
#
# ── P1-3(b): INLINE HELPER PREFLIGHT — DELIBERATELY DUPLICATED IN EVERY CONSUMER ──────
# `[ -f ]` FOLLOWS SYMLINKS: the old loader would source a link to anything, and did so before
# any mode check existed. The check must run BEFORE the source, so it cannot live in the helper —
# that would be the object under test certifying itself. These ~15 lines are duplicated in
# evidence_quote_spotcheck_guard, manifest_snapshot_integrity_guard, verify-completion.sh and
# .githooks/pre-push ON PURPOSE. The duplication IS the bootstrap.
_ax_pf_rel="practices/scripts/lib/release_anchor.sh"
_ax_pf_cur="$SELF_REPO_ROOT"
for _ax_pf_part in practices scripts lib release_anchor.sh; do
    _ax_pf_cur="$_ax_pf_cur/$_ax_pf_part"
    if [ -L "$_ax_pf_cur" ]; then
        echo "manifest_snapshot_integrity_guard: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} resolves" >&2
        echo "  through a SYMLINK at ${_ax_pf_cur}. The release-anchor helper decides the entire" >&2
        echo "  ratchet policy; a link makes the bytes that are SOURCED differ from the bytes git" >&2
        echo "  records for that path — the same asymmetry the P1-Y checks close for the ledger." >&2
        exit 2
    fi
done
# ROUND 6 / P2: the validated ABSOLUTE binary, not the bare word (which resolves through PATH).
if [ -n "${AX_GIT_BIN:-}" ] \
   && "$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_pf_mode="$("$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" ls-tree HEAD -- "$_ax_pf_rel" 2>/dev/null | head -1)"
    _ax_pf_mode="${_ax_pf_mode%% *}"
    case "${_ax_pf_mode:-}" in
        100644|100755|"") ;;   # "" = untracked: no committed mode to check, lstat walk above stands
        *)
            echo "manifest_snapshot_integrity_guard: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} has git" >&2
            echo "  mode ${_ax_pf_mode} at HEAD, which is not a regular file blob (100644/100755)." >&2
            exit 2 ;;
    esac
fi
# shellcheck source=practices/scripts/lib/release_anchor.sh
# RELOCATED-COPY AFFORDANCE, now gated (P1-3(v)): fixture_kill_proof_guard [87] proves fixture
# non-vacuity by running a MUTATED COPY of this file from a bare temp path, where the
# repo-relative helper does not exist. AX_RELEASE_ANCHOR_LIB names it for THAT case only, and the
# gate is explicit: consulted ONLY when the committed path is absent AND this root is not a git
# work tree. On a live tree a missing helper is a BLOCK, not an invitation to load the policy
# from wherever the environment points; deletion is separately blocking because the helper is now
# on the anchor-critical path list below.
AX_ANCHOR_LIB="$SELF_REPO_ROOT/practices/scripts/lib/release_anchor.sh"
if [ ! -f "$AX_ANCHOR_LIB" ] \
   && ! "$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    AX_ANCHOR_LIB="${AX_RELEASE_ANCHOR_LIB:-$AX_ANCHOR_LIB}"
fi
if [ ! -f "$AX_ANCHOR_LIB" ]; then
    echo "manifest_snapshot_integrity_guard: RELEASE_ANCHOR_LIB_MISSING — practices/scripts/lib/release_anchor.sh not" >&2
    echo "  found under $SELF_REPO_ROOT. It carries the anchor resolution, the symlink (P1-Y)" >&2
    echo "  checks and the ancestry/bootstrap authentication (P1-X); without it this guard cannot" >&2
    echo "  run its ratchet at all, so it BLOCKS rather than degrading silently." >&2
    echo "  On a git work tree the AX_RELEASE_ANCHOR_LIB override is NOT consulted: a live root" >&2
    echo "  that has lost its committed helper is a tampered tree, not a sandbox." >&2
    exit 2
fi
. "$AX_ANCHOR_LIB"

# Anchor-critical paths. The guard file itself lives in the REAL repo; the ledger, allowlist and
# manifests are read from the SCANNED root (a fixture ships its own copies).
ANCHOR_SELF_REL="practices/evals/manifest_snapshot_integrity_guard.sh"
ANCHOR_ROOT_RELS=(
    "practices/upstream/_FETCH-RECEIPTS.yaml"
    "practices/evals/manifest_snapshot_integrity_allowlist.yaml"
    "practices/upstream/_MANIFEST.yaml"
    "practices-react/upstream/_MANIFEST.yaml"
)
# P1-3(iv): the ratchet's own TOOLCHAIN is anchor-critical too. Until round 4 none of these was
# on any list, so deleting the helper activated the env fallback above and symlinking the hook or
# the runner was invisible to every gate. Always the REAL repo (a fixture root has no toolchain).
ANCHOR_TOOLCHAIN_RELS=(
    "practices/scripts/lib/release_anchor.sh"
    "practices/scripts/lib/tree_fingerprint.py"
    "practices/scripts/verify-completion.sh"
    "practices/evals/completion_checklist_recency_guard.sh"
    ".githooks/pre-push"
    ".githooks/pre-push-lib.sh"
)

ax_anchor_worktree_paths_regular "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" \
    "$ANCHOR_SELF_REL" "${ANCHOR_TOOLCHAIN_RELS[@]}" || exit 2
ax_anchor_worktree_paths_regular "$RESOLVED_ROOT" "manifest_snapshot_integrity_guard" \
    "${ANCHOR_ROOT_RELS[@]}" || exit 2

GIT_ANCHOR=""
GIT_ANCHOR_KIND="unavailable"
if [ "$LIVE_ROOT" = "1" ]; then
    ax_anchor_check_replace_refs "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" \
        || exit "$ANCHOR_AUTH_EXIT"
    ax_anchor_resolve "$SELF_REPO_ROOT"
    ax_anchor_check_pin "manifest_snapshot_integrity_guard" || exit "$ANCHOR_AUTH_EXIT"
    GIT_ANCHOR="$AX_ANCHOR_REF"
    GIT_ANCHOR_KIND="$AX_ANCHOR_KIND"
    if [ -n "$GIT_ANCHOR" ]; then
        ax_anchor_check_ancestry "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" \
            || exit "$ANCHOR_AUTH_EXIT"
        ax_anchor_release_paths_regular "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" \
            "$ANCHOR_SELF_REL" "${ANCHOR_ROOT_RELS[@]}" "${ANCHOR_TOOLCHAIN_RELS[@]}" \
            || exit "$ANCHOR_AUTH_EXIT"
    # ROUND 5 / P1-3 (TD-2026-07-30-P1-hermetic-runtime): the list above checks that these paths
    # are REGULAR FILES, on both sides. It says nothing about what they CONTAIN — and the measured
    # attack is a regular-file tree_fingerprint.py rewritten to print a constant, after which the
    # runner writing the evidence and the gate recomputing it are the same compromised
    # implementation. So the toolchain's working-tree bytes must equal what git records at HEAD.
    ax_ratchet_toolchain_authentic "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" "HEAD" \
        $(ax_ratchet_toolchain_paths) || exit "$ANCHOR_AUTH_EXIT"
    fi
fi

LIVE_ROOT="$LIVE_ROOT" ALLOWLIST_OVERRIDE="$ALLOWLIST_OVERRIDE" \
GIT_ANCHOR="$GIT_ANCHOR" GIT_ANCHOR_KIND="$GIT_ANCHOR_KIND" GIT_REPO_ROOT="$SELF_REPO_ROOT" \
"$AX_PY_BIN" -E - "$RESOLVED_ROOT" << 'PY'
import hashlib, os, re, subprocess, sys

root = sys.argv[1]
live_root = os.environ.get("LIVE_ROOT") == "1"
allowlist_override = os.environ.get("ALLOWLIST_OVERRIDE") or ""
anchor = os.environ.get("GIT_ANCHOR") or ""
anchor_kind = os.environ.get("GIT_ANCHOR_KIND") or "unavailable"
git_root = os.environ.get("GIT_REPO_ROOT") or root
anchored = bool(live_root and anchor)

try:
    import yaml
except ImportError:
    print("manifest_snapshot_integrity_guard: PyYAML unavailable — cannot run", file=sys.stderr)
    sys.exit(2)

CATALOGS = ("practices", "practices-react")
ALLOWLIST_REL = os.path.join("practices", "evals",
                             "manifest_snapshot_integrity_allowlist.yaml")

# Census floors, measured on the live tree at introduction (2026-07-30): 91 manifest ids have a
# committed snapshot body; 8 of them are W1-touched (7 distinct snapshot files — stripe-billing
# is byte-identical in both catalogs and is registered once per catalog). MAY NOT BE REDUCED:
# they exist so that deleting snapshots, or emptying the receipts ledger, fails loudly instead
# of shrinking the checked population toward a green nothing.
LIVE_MIN_IDS = 91
LIVE_MIN_TOUCHED = 8

HEADER_DIVIDER = "---\n\n"
BODY_SHA_LABEL = "Body SHA-256"

structural = []   # exit 2
findings = []     # exit 1


def die_structural(msg):
    print(f"manifest_snapshot_integrity_guard: {msg}", file=sys.stderr)
    sys.exit(2)


def load_yaml(path, what):
    try:
        return yaml.safe_load(open(path, encoding="utf-8"))
    except Exception as exc:
        die_structural(f"PARSE_ERROR — {what} ({path}): {exc}")


# ── ANCHOR PLUMBING (P1-2 / P1-3) ─────────────────────────────────────────────────────
ANCHOR_EXIT = 4    # cross-release ratchet violation: distinct from 1 (findings) / 2 (structural)


def die_anchor(code, msg):
    print(f"manifest_snapshot_integrity_guard: {code} — {msg}", file=sys.stderr)
    sys.exit(ANCHOR_EXIT)


def anchor_warn(msg):
    print(f"manifest_snapshot_integrity_guard: WARN {msg}", file=sys.stderr)


# ROUND 3 (P1-X/P1-Y): authenticity of the ANCHOR ITSELF, as opposed to a ratchet VIOLATION.
# Shares the shell preamble's exit 5 so "the anchor could not be trusted" is never readable as
# "a ratchet was rolled back" (4), "a body diverged" (1) or "a shape is wrong" (2).
ANCHOR_AUTH_EXIT = 5


def die_anchor_auth(code, msg):
    print(f"manifest_snapshot_integrity_guard: {code} — {msg}", file=sys.stderr)
    sys.exit(ANCHOR_AUTH_EXIT)


def anchor_blob(rel):
    """Bytes of `rel` as of the release anchor, or None when that path did not exist there."""
    proc = subprocess.run(["git", "-C", git_root, "show", f"{anchor}:{rel}"],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return proc.stdout if proc.returncode == 0 else None


ANCHOR_ABSENT = object()   # sentinel: the path did not exist at the anchor (≠ "unparseable")


def anchor_yaml(rel, what, strict=False):
    """Parsed YAML of `rel` at the anchor.

    Returns ANCHOR_ABSENT when the path did not exist there, and the parsed document otherwise.
    ROUND 3 (P1-Y layer 2): absence and unparseability used to COLLAPSE into the same `None`,
    so an anchor blob that does not parse degraded into "first-release bootstrap → skip". A
    symlinked ledger reaches exactly that state — `git show` returns the target PATHNAME, which
    is a YAML scalar, not a mapping — so the collapse was a silent retirement of the ratchet.
    With strict=True an unparseable anchor blob is ANCHOR_MALFORMED and BLOCKS."""
    raw = anchor_blob(rel)
    if raw is None:
        return ANCHOR_ABSENT
    try:
        return yaml.safe_load(raw.decode("utf-8", errors="replace"))
    except Exception as exc:
        if strict:
            die_anchor_auth("ANCHOR_MALFORMED",
                            f"{what} at {anchor_kind} ({rel}) does not parse ({exc}). An anchor "
                            "payload that is not readable is NOT a bootstrap: 'absent' and "
                            "'unreadable' are different states, and collapsing them let an "
                            "unreadable anchor silently retire the ratchet that depends on it. "
                            "Re-anchor onto a release whose copy parses, or restore it")
        anchor_warn(f"ANCHOR_UNPARSEABLE — {what} at {anchor_kind} does not parse ({exc}); the "
                    "ratchet that depends on it is SKIPPED. Git history is immutable to the "
                    "working tree, so this state cannot be manufactured by the edit under review")
        return None


# ── DYNAMIC SNAPSHOT PATHS GET THE RATCHET-CRITICAL TREATMENT (BACKLOG P3-111) ────────
# The ratchet-critical paths are a STATIC list and get three checks (release_anchor.sh:
# ax_anchor_worktree_paths_regular walks EVERY path component with lstat;
# ax_anchor_release_paths_regular requires git mode 100644/100755 AT THE ANCHOR). The snapshot
# bodies are the opposite kind of path — the id comes out of `_MANIFEST.yaml`, which is an
# ORDINARY EDITABLE FILE in the tree — and they had a single lstat on the LEAF. Three gaps
# followed, and each is a way to make the filesystem side and the git-object side read different
# things, which is the entire laundering channel this guard exists to close:
#   (1) A SYMLINKED INTERMEDIATE. `practices/upstream` (or a directory below it, once an id
#       carries a separator) being a link launders a "regular" leaf exactly as well as a
#       symlinked leaf does — os.path.islink(<leaf>) is False the whole time.
#   (2) NO ANCHOR-SIDE MODE CHECK. The self side lstats; the anchor side only ever called
#       `git show ANCHOR:<rel>`, which for a mode-120000 entry hands back THE TARGET PATHNAME as
#       if it were the body. The bytes then differ from the file for a reason no check names.
#   (3) THE ID IS NOT A NAME, IT IS A PATH FRAGMENT. `sid` was concatenated straight into a
#       path, so an id containing `/` or `..` addresses a file outside <catalog>/upstream/
#       entirely — the manifest would then be checksumming, and ratcheting, something else.
# Structural (exit 2) rather than a finding, matching the leaf check it replaces.
def snapshot_path_regular(catalog, sid):
    rel = f"{catalog}/upstream/{sid}.snapshot.md"
    prefix = f"{catalog}/upstream/"
    # (3) containment — normalise and require the result to stay under <catalog>/upstream/.
    norm = os.path.normpath(rel).replace(os.sep, "/")
    if not norm.startswith(prefix) or "/../" in f"/{norm}/" or norm != rel:
        die_structural(f"SNAPSHOT_PATH_ESCAPES — the manifest id {catalog}::{sid!r} builds the "
                       f"path {rel!r}, which does not name a plain file directly under "
                       f"{prefix}. An id is an identity component, not a path fragment: "
                       f"accepting a separator or a `..` lets an editable manifest point the "
                       f"census at a file outside the snapshot tree, which is then the thing "
                       f"checksummed and ratcheted")
    # (1) every path component, lstat, from the scanned root down to the leaf.
    cur = root
    for part in norm.split("/"):
        cur = os.path.join(cur, part)
        if os.path.islink(cur):
            die_structural(f"SELF_PATH_NOT_REGULAR — {rel} resolves through a SYMLINK at "
                           f"{os.path.relpath(cur, root)}. Snapshot bodies are compared against "
                           "git blobs across releases; a link anywhere on the way makes the "
                           "filesystem side and the git-object side read different bytes by "
                           "construction. Replace it with the real directory/file")
    if os.path.exists(cur) and not os.path.isfile(cur):
        die_structural(f"SELF_PATH_NOT_REGULAR — {rel} exists but is not a regular file. A "
                       "snapshot body is compared against a git blob; anything that is not a "
                       "regular file cannot be compared and must not be trusted")
    # (2) anchor-side git mode. Only on an anchored live root — a fixture root has no anchor,
    # exactly as everywhere else in this guard. A FAILED ls-tree is not "absent": the mode
    # question would then be unanswered, and this guard's fail-closed sweep does not accept
    # unanswered as clean.
    if not anchored:
        return
    proc = subprocess.run(["git", "-C", git_root, "ls-tree", anchor, "--", rel],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if proc.returncode != 0:
        die_anchor_auth("ANCHOR_PATH_UNVERIFIABLE",
                        f"`git ls-tree {anchor_kind} -- {rel}` failed (rc={proc.returncode}), so "
                        "this run cannot tell what the release anchor records that path AS. The "
                        "mode is what decides whether `git show ANCHOR:<rel>` hands back the "
                        "body or a link target, so an unanswerable mode is refused")
    out = proc.stdout.decode("utf-8", "replace").splitlines()
    if out:
        mode = out[0].split()[0]
        if mode not in ("100644", "100755"):
            die_anchor_auth("ANCHOR_PATH_NOT_REGULAR",
                            f"{rel} exists at the release anchor ({anchor_kind} = {anchor[:12]}) "
                            f"with git mode {mode}, which is not a regular file blob "
                            "(100644/100755). Mode 120000 is a SYMLINK, and the anchor side of "
                            "this guard reads GIT OBJECTS — `git show` would hand back the LINK "
                            "BLOB, i.e. the target PATHNAME, while this process checksums the "
                            "file the link resolves to. The two sides then disagree by "
                            "construction, which is the laundering channel")


def anchor_changed_paths():
    """Repo-relative paths under either catalog's upstream/ that DIFFER from the anchor.

    Two sources, and both are needed: `git diff --name-only <anchor>` covers tracked
    modifications/additions/deletions of the working tree against the released commit, and
    `git ls-files --others` covers UNTRACKED files — a brand-new snapshot body has no anchor
    blob and no diff entry, and leaving it invisible would re-open exactly the hole this
    check closes. Returns None when git cannot answer (caller degrades to a WARN)."""
    specs = [f"{c}/upstream" for c in CATALOGS]
    out = set()
    diff = subprocess.run(["git", "-C", git_root, "diff", "--name-only", anchor, "--", *specs],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if diff.returncode != 0:
        return None
    out.update(p for p in diff.stdout.decode("utf-8", "replace").splitlines() if p)
    others = subprocess.run(["git", "-C", git_root, "ls-files", "--others",
                             "--exclude-standard", "--", *specs],
                            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if others.returncode == 0:
        out.update(p for p in others.stdout.decode("utf-8", "replace").splitlines() if p)
    return out


# ── (a) FILE domain census: manifest sha/bytes vs the whole file on disk ──────────────
disk = {}          # (catalog, id) -> (file_sha, file_bytes)
recorded = {}      # (catalog, id) -> (manifest_sha, manifest_bytes)
manifest_seen = set()
for catalog in CATALOGS:
    mpath = os.path.join(root, catalog, "upstream", "_MANIFEST.yaml")
    if not os.path.isfile(mpath):
        continue
    doc = load_yaml(mpath, f"{catalog} manifest") or {}
    entries = doc.get("snapshots") or []
    if not isinstance(entries, list):
        die_structural(f"MANIFEST_SHAPE — {catalog}/upstream/_MANIFEST.yaml `snapshots` is "
                       f"{type(entries).__name__}, expected a list")
    for entry in entries:
        if not isinstance(entry, dict) or "id" not in entry:
            continue
        sid = entry["id"]
        if not isinstance(sid, str):
            die_structural(f"MANIFEST_NON_STRING_ID — {catalog}/upstream/_MANIFEST.yaml has an "
                           f"id of type {type(sid).__name__} ({sid!r}); an id is an identity "
                           "component and is never coerced")
        key = (catalog, sid)
        if key in manifest_seen:
            die_structural(f"MANIFEST_DUPLICATE_ID — {catalog}::{sid} is declared twice; two "
                           "records for one id mean the answer depends on which one a reader "
                           "stops at")
        manifest_seen.add(key)
        spath = os.path.join(root, catalog, "upstream", sid + ".snapshot.md")
        # ROUND 3 (P1-Y layer 1, extended to the bodies themselves): every read below follows
        # symlinks (open/isfile use stat), while the anchor side reads git blobs. A symlinked
        # BODY is therefore the same laundering channel as a symlinked ledger — this process
        # checksums the target, the next release's `git show` sees a pathname. lstat, not stat.
        # BACKLOG P3-111: that was a LEAF-ONLY lstat, and these paths are DYNAMIC — the id comes
        # out of an editable manifest. The ratchet-critical (static) paths get three things and
        # these got one; snapshot_path_regular() now applies all three here as well.
        snapshot_path_regular(catalog, sid)
        if not os.path.isfile(spath):
            # No committed body — out of scope by construction (there is nothing to checksum).
            # Reported in the summary so the population is visible, never silently dropped.
            continue
        raw = open(spath, "rb").read()
        disk[key] = (hashlib.sha256(raw).hexdigest(), len(raw))
        recorded[key] = (entry.get("sha"), entry.get("bytes"))

divergent_keys = set()
for key, (file_sha, file_bytes) in sorted(disk.items()):
    e_sha, e_bytes = recorded[key]
    if e_sha != file_sha or e_bytes != file_bytes:
        divergent_keys.add(key)

# ── CHANGE-DRIVEN CHAIN SCOPE (P1-2a) ─────────────────────────────────────────────────
# The chain used to run on exactly the ids carrying a `kind: assembly` receipt — i.e. the guard
# asked the *receipts ledger* which ids to verify, and the ledger is one of the things an editor
# controls. So the reviewer refreshed an ALLOWLISTED, NON-ASSEMBLY id (practices-react::cwv-2026):
# edit the snapshot body, sync the manifest sha/bytes to the new file, delete its allowlist row.
# (a) passed (manifest now matches disk), the allowlist was smaller (legal, shrink-only), and the
# chain never looked because there was no assembly row — a refresh with no receipt at all.
#
# The scope is now derived from the RELEASE ANCHOR instead: an id whose snapshot FILE differs
# from `git show ANCHOR:<file>`, or whose manifest sha/bytes entry differs from the ANCHOR's
# manifest, is a CHANGED id and must pass the full 3-domain chain (header body-sha recompute +
# receipt). Ids that are byte-identical to the released tree stay exactly as exempt as they were
# — which is what keeps the ~20 legacy-clean, pre-receipt ids passing without a re-fetch.
#
# Scope is limited to ids that have a body on disk NOW: with no body there is nothing to
# checksum, which is the same boundary domain (a) already draws. A manifest edit for a
# body-less id therefore remains out of scope, stated rather than silently relied upon.
changed_keys = set()
if anchored:
    changed_paths = anchor_changed_paths()
    if changed_paths is None:
        # ROUND 3 (P1-Y layer 2 sweep): was WARN+SKIP on every root. Change-driven scope IS the
        # layer that makes an edited-but-unreceipted body fatal, so on a LIVE root a failure to
        # compute it is a silent retirement, not a degradation — and a `git diff` against a
        # resolved anchor only fails when the anchor is not what it claims to be. Fixture roots
        # never anchor at all, so the advisory there is unreachable rather than lenient.
        if live_root:
            die_anchor_auth("ANCHOR_DIFF_UNAVAILABLE",
                            f"`git diff --name-only {anchor_kind}` failed on the live tree, so "
                            "the change-driven chain scope cannot be computed. That scope is what "
                            "forces an id whose snapshot body differs from the released one "
                            "through the full file←body←receipt chain; skipping it would leave a "
                            "refresh-without-receipt unlooked-at. Verify the anchor resolves "
                            "(`git rev-parse origin/main`) and the objects are present "
                            "(`git fetch origin`), then re-run")
        anchor_warn("ANCHOR_DIFF_UNAVAILABLE — `git diff` against "
                    f"{anchor_kind} failed; change-driven chain scope SKIPPED (the assembly-row "
                    "population is still verified)")
    else:
        manifest_changed = set()
        for pth in changed_paths:
            for catalog in CATALOGS:
                prefix = f"{catalog}/upstream/"
                if not pth.startswith(prefix):
                    continue
                leaf = pth[len(prefix):]
                if leaf == "_MANIFEST.yaml":
                    manifest_changed.add(catalog)
                elif leaf.endswith(".snapshot.md"):
                    key = (catalog, leaf[: -len(".snapshot.md")])
                    if key in disk:
                        changed_keys.add(key)
        for catalog in sorted(manifest_changed):
            adoc_m = anchor_yaml(f"{catalog}/upstream/_MANIFEST.yaml", f"{catalog} manifest")
            prior_records = {}
            if isinstance(adoc_m, dict):
                for entry in (adoc_m.get("snapshots") or []):
                    if isinstance(entry, dict) and isinstance(entry.get("id"), str):
                        prior_records[entry["id"]] = (entry.get("sha"), entry.get("bytes"))
            for key in disk:
                if key[0] != catalog:
                    continue
                if prior_records.get(key[1]) != recorded[key]:
                    # Absent at the anchor (new id) or a rewritten sha/bytes pair — either way
                    # the manifest now makes a claim the released tree did not make.
                    changed_keys.add(key)
elif live_root:
    anchor_warn("ANCHOR_UNAVAILABLE — no git anchor (origin/main or HEAD) could be resolved, so "
                "change-driven chain scope is SKIPPED and only assembly-row ids run the full "
                "chain. A tree with no git history cannot be pushed either (the pre-push recency "
                "guard is a git hook), so the released path keeps the ratchet")
if anchored and anchor_kind != "origin/main":
    anchor_warn(f"ANCHOR_FALLBACK — ratcheting against {anchor_kind}, not origin/main. Weaker by "
                "construction: an edit that is already committed locally is present in the "
                "anchor itself")

# ── allowlist: shape, subset-only, uniqueness, non-redundancy, reasons ────────────────
allow_path = allowlist_override or os.path.join(root, ALLOWLIST_REL)
allowed = {}       # (catalog, id) -> reason
baseline = set()
if not os.path.isfile(allow_path):
    if live_root:
        die_structural(f"ALLOWLIST_MISSING — {ALLOWLIST_REL} not found under {root}; the live "
                       "tree must carry the reason-bearing residual list (an absent allowlist "
                       "is an unreviewed suppression of every divergence, or a gate that "
                       "cannot pass — neither is a legible state)")
else:
    adoc = load_yaml(allow_path, "allowlist") or {}
    if not isinstance(adoc, dict):
        die_structural(f"ALLOWLIST_SHAPE — {allow_path} is not a mapping")
    raw_baseline = adoc.get("baseline_universe")
    if not isinstance(raw_baseline, list) or not raw_baseline:
        die_structural("ALLOWLIST_NO_BASELINE — the allowlist must declare a non-empty "
                       "`baseline_universe:` list (the FROZEN wave-start divergent census). "
                       "Without it, subset-only cannot be enforced and any future divergence "
                       "could be allowlisted away")
    for item in raw_baseline:
        if not isinstance(item, str) or "::" not in item:
            die_structural(f"ALLOWLIST_BASELINE_MALFORMED — baseline entry {item!r} "
                           "(expected '<catalog>::<id>')")
        cat, sid = item.split("::", 1)
        bkey = (cat.strip(), sid.strip())
        if bkey in baseline:
            die_structural(f"ALLOWLIST_BASELINE_DUPLICATE — {item!r} appears twice; the frozen "
                           "universe is a SET and a repeat inflates its declared size")
        baseline.add(bkey)
    declared_count = adoc.get("baseline_count")
    if declared_count is not None and declared_count != len(baseline):
        die_structural(f"ALLOWLIST_BASELINE_COUNT — declared baseline_count={declared_count} "
                       f"but the list holds {len(baseline)} unique entries")

    for entry in (adoc.get("entries") or []):
        if not isinstance(entry, dict):
            die_structural(f"ALLOWLIST_ENTRY_SHAPE — entry {entry!r} is not a mapping")
        cat, sid, reason = entry.get("catalog"), entry.get("id"), entry.get("reason")
        if not isinstance(cat, str) or cat not in CATALOGS:
            die_structural(f"ALLOWLIST_ENTRY_CATALOG — entry {entry!r} `catalog` must be one of "
                           f"{CATALOGS}")
        if not isinstance(sid, str) or not sid.strip():
            die_structural(f"ALLOWLIST_ENTRY_ID — entry {entry!r} `id` must be a non-empty string")
        if not isinstance(reason, str) or not reason.strip():
            die_structural(f"ALLOWLIST_ENTRY_NO_REASON — {cat}::{sid} carries no non-empty "
                           "`reason:`. An unexplained suppression is one nobody revisits")
        key = (cat, sid)
        if key in allowed:
            die_structural(f"ALLOWLIST_DUPLICATE_KEY — {cat}::{sid} is listed more than once; "
                           "entries are keyed by unique (catalog, id)")
        if key not in baseline:
            die_structural(f"ALLOWLIST_NOT_IN_BASELINE — {cat}::{sid} is not in the frozen "
                           f"baseline_universe ({len(baseline)} entries). The allowlist is "
                           "SHRINK-ONLY: removal is the only legal edit, so divergence "
                           "introduced after the freeze can be fixed but never suppressed")
        allowed[key] = reason

    # ── P1-3: THE "FROZEN" BASELINE IS NOW FROZEN IN THE PREVIOUS RELEASE ─────────────
    # `baseline_universe` was described as FROZEN and enforced only against ITSELF: every entry
    # had to be a member of the very list the same editor was writing. So the reviewer took a
    # clean id (practices-react::mdn-promise-all), diverged its manifest record, and then added
    # it to baseline_universe + entries + bumped baseline_count — subset-only was satisfied
    # because the baseline had grown to contain it. "Frozen" that is re-read from the mutable
    # tree is not frozen; it is self-certifying.
    #
    # Both halves are therefore compared to the ANCHOR's copy of this same allowlist:
    #   · baseline_universe must be EQUAL to the anchor's (BASELINE_MUTATED) — frozen forever
    #     after the first release, in BOTH directions: an addition re-opens suppression, and a
    #     removal would silently shrink the universe that subset-only is measured against.
    #   · residual `entries` must be a SUBSET of the anchor's entries (ALLOWLIST_GREW) — the
    #     list may only shrink ACROSS RELEASES, so a new divergence can never enter it and must
    #     pass the full chain instead.
    # Deliberate consequence, disclosed: on the live tree the comparison is always against the
    # COMMITTED allowlist path at the anchor, even under --allowlist. Pointing the live run at
    # some other file therefore trips BASELINE_MUTATED rather than silently adopting that file's
    # baseline — fail-closed, since an override that redefines the frozen universe is the same
    # bypass by another route.
    if anchored:
        prior_allow = anchor_yaml(ALLOWLIST_REL.replace(os.sep, "/"), "allowlist", strict=True)
        if prior_allow is ANCHOR_ABSENT:
            # Reachable only for a genuinely new path: the shell preamble has already required
            # (ANCHOR_BOOTSTRAP_IMPLAUSIBLE, exit 5) that the anchor's own history never carried
            # this file, and (ANCHOR_NOT_ANCESTOR) that the anchor descends into HEAD.
            anchor_warn(f"ANCHOR_ALLOWLIST_ABSENT — {ALLOWLIST_REL} does not exist at "
                        f"{anchor_kind}; nothing to ratchet against (first-release bootstrap). "
                        "Baseline-freeze and shrink-only checks SKIPPED for this run")
        elif not isinstance(prior_allow, dict):
            # ROUND 3 (P1-Y layer 2): was WARN+SKIP. `git show <anchor>:<allowlist>` on a
            # SYMLINKED allowlist returns the target PATHNAME — a YAML scalar string, not a
            # mapping — and this branch then retired the baseline-freeze and shrink-only ratchets
            # without failing anything. An anchor payload of the wrong TYPE is malformed, never a
            # bootstrap.
            die_anchor_auth("ANCHOR_MALFORMED",
                            f"{ALLOWLIST_REL} at {anchor_kind} parses to "
                            f"{type(prior_allow).__name__}, not a mapping. The baseline-freeze and "
                            "shrink-only ratchets are defined over that mapping, so a wrong-type "
                            "anchor payload silently retires them — which is why this is fatal "
                            "rather than advisory. A scalar here is the signature of a SYMLINKED "
                            "allowlist: git hands the anchor side the link's target pathname while "
                            "this process reads the real file through the link")
        else:
            prior_baseline = set()
            prior_baseline_ok = isinstance(prior_allow.get("baseline_universe"), list)
            for item in (prior_allow.get("baseline_universe") or []):
                if isinstance(item, str) and "::" in item:
                    pcat, psid = item.split("::", 1)
                    prior_baseline.add((pcat.strip(), psid.strip()))
                else:
                    prior_baseline_ok = False
            if not prior_baseline_ok or not prior_baseline:
                # ROUND 3 (P1-Y layer 2): was WARN+SKIP. The anchor's allowlist is a mapping we
                # got here, so a missing/ill-formed baseline_universe is not a bootstrap — it is
                # a released allowlist that cannot serve as the frozen reference, and skipping
                # leaves the freeze unenforced for exactly the release that needs it.
                die_anchor_auth("ANCHOR_BASELINE_MALFORMED",
                                f"the {anchor_kind} copy of {ALLOWLIST_REL} declares no "
                                "well-formed baseline_universe (expected a list of "
                                "'catalog::id' strings). The wave-start census is the FROZEN "
                                "reference the in-tree baseline is compared against; without it "
                                "the freeze check has nothing to compare and would silently pass. "
                                "Re-anchor onto a release that carries the census, or restore it")
            elif prior_baseline != baseline:
                added = sorted(baseline - prior_baseline)
                dropped = sorted(prior_baseline - baseline)
                detail = []
                if added:
                    detail.append("ADDED " + ", ".join(f"{c}::{i}" for c, i in added))
                if dropped:
                    detail.append("REMOVED " + ", ".join(f"{c}::{i}" for c, i in dropped))
                die_anchor("BASELINE_MUTATED",
                           f"baseline_universe differs from {anchor_kind}:{ALLOWLIST_REL} "
                           f"({len(prior_baseline)} → {len(baseline)} entries; "
                           + "; ".join(detail) + "). The wave-start census is FROZEN: an "
                           "ADDITION allowlists divergence introduced after the freeze (the "
                           "in-tree subset check cannot see it, because the list it checks "
                           "membership against is the list being edited), and a REMOVAL shrinks "
                           "the universe that shrink-only is measured against. Fix the "
                           "divergence, or re-fetch the id and record receipts")
            prior_entries = set()
            for entry in (prior_allow.get("entries") or []):
                if isinstance(entry, dict) and isinstance(entry.get("catalog"), str) \
                        and isinstance(entry.get("id"), str):
                    prior_entries.add((entry["catalog"], entry["id"]))
            grew = sorted(set(allowed) - prior_entries)
            if grew:
                die_anchor("ALLOWLIST_GREW",
                           f"{len(grew)} residual entry(ies) are not in the {anchor_kind} "
                           "allowlist: " + ", ".join(f"{c}::{i}" for c, i in grew)
                           + f" ({len(prior_entries)} → {len(allowed)} entries). The residual is "
                           "SHRINK-ONLY ACROSS RELEASES: removal is the only legal edit, so a "
                           "divergence that was not already suppressed by the previous release "
                           "must pass the full file←body←receipt chain, never be suppressed")

    # NON-REDUNDANCY. Checked in the allowlist's own domain (does this entry still describe a
    # real divergence?) rather than folded into the divergence loop, so a padded list fails even
    # when nothing else is wrong.
    for key in sorted(allowed):
        if key not in divergent_keys:
            cat, sid = key
            if key not in disk:
                findings.append(f"ALLOWLIST_STALE — {cat}::{sid} is allowlisted but has no "
                                "committed snapshot body to diverge from; remove the entry")
            else:
                findings.append(f"ALLOWLIST_STALE — {cat}::{sid} is allowlisted but its manifest "
                                "sha/bytes NOW MATCH disk. A suppression that suppresses "
                                "nothing makes the list stop describing the residual and hides "
                                "burn-down progress; remove the entry")

for key in sorted(divergent_keys):
    if key in allowed:
        continue
    cat, sid = key
    file_sha, file_bytes = disk[key]
    e_sha, e_bytes = recorded[key]
    findings.append(
        f"MANIFEST_FILE_DIVERGED — {cat}::{sid} manifest records sha={e_sha} bytes={e_bytes}, "
        f"disk file is sha={file_sha} bytes={file_bytes}. The manifest does not describe the "
        "file it claims to describe, so nothing pinned to that body is actually pinned")

# ── (b)+(c) BODY and RECEIPT domains for every W1-touched id ──────────────────────────
RECEIPTS_REL = "practices/upstream/_FETCH-RECEIPTS.yaml"
receipts_path = os.path.join(root, "practices", "upstream", "_FETCH-RECEIPTS.yaml")
receipts_raw = ""
fetch_rows = {}     # receipt id -> row
assembly = {}       # (catalog, snapshot_id) -> LATEST row for that identity
assembly_rows = 0
cur_rows = []
row_ids = set()
if os.path.isfile(receipts_path):
    receipts_raw = open(receipts_path, encoding="utf-8", errors="replace").read()
    rdoc = load_yaml(receipts_path, "receipts ledger") or {}
    cur_rows = rdoc.get("receipts") or []
    if not isinstance(cur_rows, list):
        die_structural("RECEIPTS_SHAPE — practices/upstream/_FETCH-RECEIPTS.yaml `receipts` is "
                       f"{type(cur_rows).__name__}, expected a list")
    for row in cur_rows:
        if not isinstance(row, dict):
            continue
        kind = row.get("kind")
        rid = row.get("id")
        # Row ids are unique across EVERY kind, not just within `fetch`: an assembly row is
        # addressed by id in the append-only ratchet below, so two rows sharing one id would make
        # "is the released row still intact?" depend on which one a reader stops at.
        if rid is not None:
            if rid in row_ids:
                die_structural(f"RECEIPTS_DUPLICATE_ID — receipt id {rid!r} appears twice; a "
                               "receipt id is referenced by assembly rows and by the append-only "
                               "ratchet, and must resolve to exactly one record")
            row_ids.add(rid)
        if kind == "fetch":
            fetch_rows[rid] = row
        elif kind == "assembly":
            # MULTIPLE ASSEMBLY ROWS PER IDENTITY ARE LEGAL (P1-2c). The ledger is append-only,
            # so a SECOND refresh of the same id appends a second assembly row rather than
            # rewriting the first — forbidding that (the previous RECEIPTS_DUPLICATE_ASSEMBLY)
            # would have forced every repeat refresh to MUTATE history, which is precisely what
            # the append-only ratchet must forbid. The chain therefore binds to the LATEST
            # assembly row per identity (last in file order = last appended), and the
            # append-only check below guarantees the older rows stay byte-intact, so the
            # supersession is auditable rather than a silent overwrite.
            akey = (row.get("catalog"), row.get("snapshot_id"))
            assembly[akey] = row
            assembly_rows += 1

# ── P1-2b: RECEIPTS LEDGER IS APPEND-ONLY vs THE ANCHOR ───────────────────────────────
# Domain (c) asks "does a receipt describe these bytes?" and the receipts file is in the tree,
# so the answer was writable: REWRITE an existing assembly row's `body_sha256` to the doctored
# body's digest and the chain closed on a receipt that had been edited to fit. (The old
# referenced-row-existence check only asked whether the `from_receipts` ids RESOLVE, never
# whether the row they resolve to still says what it said when it was written.)
#
# So every row present in the ANCHOR's ledger must still be present and unchanged here; only
# APPENDS are legal. THREE layers, all reported as RECEIPT_LEDGER_MUTATED (exit 4):
#   1. PARSED-ROW identity — the anchor row's mapping must equal a current row of the same id.
#      Robust to reflowing/re-quoting, which is the layer that always runs.
#   2. BYTE-CHUNK identity — the anchor's exact chunk text must appear verbatim (trailing blank
#      lines normalized, since those belong to the separator between rows and not to the row).
#      This catches an edit that parses identically.
#   3. PREFIX (ORDER) identity — see below. Layers 1-2 were ORDER-BLIND (both keyed by id), and
#      order is load-bearing here.
# Top-level keys other than `receipts` (e.g. `notes:`) are NOT frozen: prose about the ledger is
# not a provenance record, and freezing it would make documenting a later refresh impossible.
#
# ORDER IS LOAD-BEARING — THE APPEND-ONLY CHECK IS NOW A PREFIX CHECK
# ------------------------------------------------------------------
# (P1-B, cross-family reviewer ROUND 2, 2026-07-30; TD-2026-07-30-P1-anchor-ratchet-r2.)
#
# Allowing multiple assembly rows per identity (P1-2c) made "which row wins" a real question, and
# the answer is FILE ORDER: the chain binds to the LAST assembly row for an id. But the
# append-only check compared rows and chunks BY ID, discarding order — so file order was
# simultaneously authoritative for the verdict and unprotected by the ratchet. The reviewer walked
# straight through the gap:
#   anchor (release N): rows [… , asm-v1(body1), asm-v2(body2)], snapshot = body2 (latest wins)
#   attack (release N+1): SWAP the two intact chunks to [… , asm-v2, asm-v1] and revert the
#                         snapshot + manifest + header back to body1.
#   result: every parsed row is present and equal by id; every byte chunk is present and equal by
#           id; nothing "changed" — but asm-v1 is now LATEST, so the chain verifies body1 and a
#           released refresh has been silently ROLLED BACK.
# An append-only ledger is a SEQUENCE, not a set. So the anchor's row-ID sequence must be an
# EXACT PREFIX of the current sequence, and (when both sides chunk) so must the byte-chunk
# sequence. Only suffix appends are legal; reordering, insertion anywhere but the end, and removal
# are all RECEIPT_LEDGER_MUTATED (exit 4). Note the prefix rule SUBSUMES deletion detection and
# makes the by-id layers a redundancy rather than the only defense — deliberately kept, because
# they produce the precise "field X of row Y was rewritten" diagnostic that a prefix mismatch
# cannot.
#
# CHUNKABILITY IS NOW MANDATORY ON A LIVE ROOT (not an advisory downgrade)
# -----------------------------------------------------------------------
# ANCHOR_RECEIPTS_UNCHUNKABLE used to print an advisory and drop the byte layer. That is the same
# two-release laundering shape closed in evidence_quote_spotcheck_guard (P1-A): the reviewer
# INDENTED the whole `receipts:` list — semantically identical YAML, so every value-level check
# still passed — and the byte layer silently retired itself. Two changes:
#   · SELF-CHUNK (generation-N kill, exit 2 RECEIPTS_SELF_UNCHUNKABLE): the CURRENT ledger must
#     chunk — on EVERY root that has one, live or fixture. The ledger is MACHINE-OWNED (written by
#     practices/scripts/snapshot-extract.sh and appended to by refresh waves), so its shape is ours
#     to keep verifiable; an indent-the-list reformat can therefore never ship and can never become
#     an anchor. Deliberately NOT live-root-gated, unlike every anchor check: it inspects the tree
#     in front of it rather than a release history, so it is the one half of this P1 that a FIXTURE
#     can prove (fail_ledger_unchunkable is a byte-copy of pass_clean whose ledger is indented and
#     nothing else — exit 0 → exit 2 is attributable to the indent alone). Every pre-existing
#     fixture ledger already chunks, so universalizing it costs nothing and buys the proof.
#   · ANCHOR side (exit 4 RECEIPT_LEDGER_MUTATED): an anchor that does not chunk is BLOCKING on a
#     live root — belt-and-braces for any anchor committed before the self-chunk check existed.
#     Fixture roots keep the advisory here, because they never anchor at all.


def receipt_chunks(text):
    """`id` -> exact chunk text, for a ledger whose list items start at column 0. None when the
    file does not have that shape (then the byte layer is skipped, not silently passed)."""
    chunks, cur_id, buf = {}, None, []
    for line in text.splitlines(keepends=True):
        if line.startswith("- "):
            if cur_id is not None:
                chunks[cur_id] = "".join(buf)
            m = re.match(r"- id:\s*(\S+)\s*$", line.rstrip("\n"))
            if not m:
                return None
            cur_id, buf = m.group(1).strip("'\""), [line]
        elif cur_id is not None:
            if line.startswith((" ", "\t")) or not line.strip():
                buf.append(line)
            else:
                chunks[cur_id] = "".join(buf)
                cur_id, buf = None, []
    if cur_id is not None:
        chunks[cur_id] = "".join(buf)
    return chunks or None


# ── P1-B: SELF-CHUNK CHECK (generation-N kill for the indent-the-list laundering) ──────
# Runs before anything consults the anchor, independently of whether an anchor resolved, and on
# EVERY root (see the header): a shape that a future release could not verify must not ship, and
# unlike the anchor checks this one inspects only the tree in front of it, so a fixture can prove
# it. Every pre-existing fixture ledger already chunks.
cur_chunks = receipt_chunks(receipts_raw) if receipts_raw else None
if receipts_raw and cur_chunks is None:
    die_structural(f"RECEIPTS_SELF_UNCHUNKABLE — {RECEIPTS_REL} does not chunk into column-0 "
                   "`- id: <id>` list items. The byte-identity and prefix layers of the "
                   "append-only ratchet are defined over those chunks, so a ledger in this shape "
                   "retires them — and since this file becomes the NEXT release's anchor, shipping "
                   "it is the first half of a two-release bypass (indent the list: semantically "
                   "identical YAML, every value-level check still green, byte layer silently "
                   "gone). The ledger is machine-owned (practices/scripts/snapshot-extract.sh "
                   "writes it and refresh waves append to it), so keeping it chunkable is a "
                   "formatting rule we own, not a constraint imposed from outside")

if anchored:
    prior_raw = anchor_blob(RECEIPTS_REL)
    if prior_raw is None:
        # Reachable only for a genuinely new path (ROUND 3): the shell preamble has already
        # required ANCHOR_BOOTSTRAP_IMPLAUSIBLE-freedom (the anchor's own history never carried
        # this file) and ANCHOR_NOT_ANCESTOR-freedom (the anchor descends into HEAD), so a forged
        # refs/remotes/origin/main aimed at a tree that merely DROPS the ledger cannot land here.
        anchor_warn(f"ANCHOR_RECEIPTS_ABSENT — {RECEIPTS_REL} does not exist at {anchor_kind}; "
                    "nothing to ratchet against (first-release bootstrap). Append-only check "
                    "SKIPPED")
    elif not receipts_raw:
        die_anchor("RECEIPT_LEDGER_MUTATED",
                   f"{RECEIPTS_REL} exists at {anchor_kind} but is absent from the working tree. "
                   "Deleting the ledger deletes every provenance record the chain attaches to; "
                   "the ledger is append-only")
    else:
        prior_text = prior_raw.decode("utf-8", errors="replace")
        # ── ROUND 3 (P1-Y layer 2): FAIL-CLOSED ANCHOR PARSE ──────────────────────────
        # Both of the following used to be silent skips, and TOGETHER they were the whole
        # append-only ratchet's off switch:
        #   · an unparseable anchor blob WARNed and set prior_doc = None;
        #   · everything below was nested under `if isinstance(prior_doc, dict)` WITH NO ELSE,
        #     so a non-mapping payload skipped the by-id, prefix and byte-chunk layers in silence.
        # `git show <anchor>:_FETCH-RECEIPTS.yaml` on a SYMLINKED ledger returns the target
        # PATHNAME — which YAML parses cleanly as a STRING — so the reviewer reached the second
        # branch with an intact-looking anchor and no error anywhere. An anchor payload that is
        # not the expected TYPE is ANCHOR_MALFORMED and blocks.
        try:
            prior_doc = yaml.safe_load(prior_text) or {}
        except Exception as exc:
            die_anchor_auth("ANCHOR_MALFORMED",
                            f"{RECEIPTS_REL} at {anchor_kind} does not parse ({exc}). The "
                            "append-only ratchet (by-id identity, row-order prefix, byte-chunk "
                            "prefix) is defined over that document; an unreadable anchor retires "
                            "all three at once, so this is fatal rather than advisory. Re-anchor "
                            "onto a release whose ledger parses, or restore it")
        if not isinstance(prior_doc, dict):
            die_anchor_auth("ANCHOR_MALFORMED",
                            f"{RECEIPTS_REL} at {anchor_kind} parses to "
                            f"{type(prior_doc).__name__}, not a mapping. A scalar here is the "
                            "signature of a SYMLINKED ledger: git hands the anchor side the "
                            "link's target PATHNAME while this process reads the real YAML "
                            "through the link — the anchor side reads GIT OBJECTS, the self side "
                            "reads the FILESYSTEM, and every place those disagree is a laundering "
                            "channel. The append-only ratchet is defined over the mapping, so a "
                            "wrong-type payload silently retires it")
        prior_rows = prior_doc.get("receipts") or []
        cur_by_id = {}
        for row in cur_rows:
            if isinstance(row, dict) and row.get("id") is not None:
                cur_by_id[row["id"]] = row
        mutated = []
        for prow in prior_rows:
            if not isinstance(prow, dict):
                continue
            prid = prow.get("id")
            if prid not in cur_by_id:
                mutated.append(f"{prid!r}: released row DELETED")
            elif cur_by_id[prid] != prow:
                changed = sorted(
                    k for k in set(prow) | set(cur_by_id[prid])
                    if prow.get(k) != cur_by_id[prid].get(k))
                mutated.append(f"{prid!r}: field(s) {', '.join(changed)} REWRITTEN")
        if mutated:
            die_anchor("RECEIPT_LEDGER_MUTATED",
                       f"{len(mutated)} row(s) of {RECEIPTS_REL} differ from {anchor_kind}: "
                       + "; ".join(mutated[:8])
                       + (" …" if len(mutated) > 8 else "")
                       + ". A receipt records what a fetch produced; rewriting one is how a "
                         "doctored body gets a matching `body_sha256` and closes domain (c) "
                         "on manufactured provenance. The ledger is APPEND-ONLY: a repeat "
                         "refresh appends a new assembly row (the chain binds to the latest), "
                         "it never edits the old one")
        # ── P1-B layer 3: PREFIX (ORDER) identity over the PARSED row-id sequence ──
        # Layers 1-2 are keyed by id and therefore order-blind, while the chain's verdict
        # binds to the LAST assembly row for an identity. Swapping two intact rows changes
        # the verdict without changing any row, so the sequence itself is ratcheted: the
        # anchor's row-id sequence must be an EXACT PREFIX of the current one.
        prior_id_seq = [r.get("id") for r in prior_rows
                        if isinstance(r, dict) and r.get("id") is not None]
        cur_id_seq = [r.get("id") for r in cur_rows
                      if isinstance(r, dict) and r.get("id") is not None]
        head = cur_id_seq[:len(prior_id_seq)]
        if head != prior_id_seq:
            first = next((i for i in range(max(len(head), len(prior_id_seq)))
                          if head[i:i + 1] != prior_id_seq[i:i + 1]), 0)
            die_anchor("RECEIPT_LEDGER_MUTATED",
                       f"the {anchor_kind} row order is not a PREFIX of the current "
                       f"{RECEIPTS_REL}: at position {first} the released ledger has "
                       f"{prior_id_seq[first:first + 1] or ['<end>']} and this tree has "
                       f"{head[first:first + 1] or ['<end>']} "
                       f"({len(prior_id_seq)} released row(s), {len(cur_id_seq)} now). An "
                       "append-only ledger is a SEQUENCE, not a set: the chain binds to the "
                       "LAST assembly row for an identity, so REORDERING two intact rows "
                       "silently rolls a released refresh back to an earlier body while every "
                       "by-id comparison still reports 'unchanged'. Only SUFFIX APPENDS are "
                       "legal — insertion before the end, reordering and removal are not")
        prior_chunks = receipt_chunks(prior_text)
        if prior_chunks is None or cur_chunks is None:
            if live_root:
                # P1-B: on a live root an unchunkable ledger is BLOCKING, not an advisory
                # downgrade. The current-side case is already dead (RECEIPTS_SELF_UNCHUNKABLE
                # above); this is the belt-and-braces for an ANCHOR committed before that
                # check existed, i.e. exactly the state the reviewer's indent-the-list
                # laundering would have created one release earlier.
                die_anchor("RECEIPT_LEDGER_MUTATED",
                           f"ANCHOR_RECEIPTS_UNCHUNKABLE — {RECEIPTS_REL} does not chunk into "
                           "column-0 `- id: <id>` list items at "
                           f"{anchor_kind if prior_chunks is None else 'the working tree'}, so "
                           "the byte-identity and prefix-by-bytes layers cannot run. That is a "
                           "SILENT RETIREMENT of the ratchet, which is why it is fatal here "
                           "rather than advisory: an indent-the-list reformat is semantically "
                           "identical YAML and passes every value-level check. Re-anchor onto "
                           "a release whose ledger chunks, or restore the shape")
            anchor_warn("ANCHOR_RECEIPTS_UNCHUNKABLE — the ledger's list items are not "
                        "column-0 `- id: <id>` entries on one or both sides, so the "
                        "byte-identity layer is inapplicable; parsed-row identity still "
                        "applies (an edit that changes no parsed value is not detected). "
                        "Advisory on FIXTURE roots only — blocking on the live tree")
        else:
            def trim(chunk):
                return re.sub(r"\s+$", "", chunk)
            byte_mutated = [rid for rid, txt in prior_chunks.items()
                            if trim(cur_chunks.get(rid, "")) != trim(txt)]
            if byte_mutated:
                die_anchor("RECEIPT_LEDGER_MUTATED",
                           f"{len(byte_mutated)} released row(s) of {RECEIPTS_REL} are not "
                           "byte-identical to " + anchor_kind + ": "
                           + ", ".join(map(repr, sorted(byte_mutated)[:8]))
                           + (" …" if len(byte_mutated) > 8 else "")
                           + ". Only appends are legal")
            # PREFIX identity over the BYTE-CHUNK sequence — the same order ratchet applied
            # in the byte domain, so a reorder that somehow parsed into an identical row-id
            # sequence still cannot pass.
            prior_chunk_seq = list(prior_chunks.keys())
            cur_chunk_seq = list(cur_chunks.keys())
            if cur_chunk_seq[:len(prior_chunk_seq)] != prior_chunk_seq:
                die_anchor("RECEIPT_LEDGER_MUTATED",
                           f"the {anchor_kind} BYTE-CHUNK order is not a PREFIX of the current "
                           f"{RECEIPTS_REL} ({len(prior_chunk_seq)} released chunk(s), "
                           f"{len(cur_chunk_seq)} now). Only suffix appends are legal")

# ── (b)+(c) BODY and RECEIPT domains: assembly-row ids UNION anchor-changed ids ────────
# `touched` (the assembly-row population) keeps its meaning for the LIVE_MIN_TOUCHED floor;
# `chain_keys` is what actually runs the chain and additionally carries every id that DIFFERS
# from the release anchor (P1-2a). A changed id with no assembly row is the reviewer's
# refresh-without-receipt and dies RECEIPT_MISSING before anything else is inspected — that is
# the whole point: a refresh is legal only through a fetch that appends receipts.
touched = sorted(assembly)
chain_keys = sorted(set(assembly) | changed_keys)
for key in chain_keys:
    cat, sid = key
    row = assembly.get(key)
    if row is None:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} differs from the release anchor "
                       f"({anchor_kind}) — its snapshot body and/or its manifest sha/bytes entry "
                       "changed — but the ledger carries NO `kind: assembly` receipt for it. A "
                       "refreshed snapshot with no receipt records that bytes changed and not "
                       "what produced them; syncing the manifest to the new file and dropping an "
                       "allowlist row does not substitute for a fetch. Re-fetch with "
                       "practices/scripts/snapshot-extract.sh and append its receipts")
    spath = os.path.join(root, cat, "upstream", str(sid) + ".snapshot.md")
    if not os.path.isfile(spath):
        die_structural(f"RECEIPT_ORPHANED — {cat}::{sid} has an assembly receipt but no "
                       f"{sid}.snapshot.md on disk; a receipt for a body that does not exist "
                       "describes nothing")
    if key in allowed:
        die_structural(f"TOUCHED_ID_ALLOWLISTED — {cat}::{sid} was refreshed in this wave (it "
                       "carries an assembly receipt) AND is allowlisted. A freshly-fetched id "
                       "has a verifiable digest by construction; suppressing it would be "
                       "laundering the one population the chain can prove")

    raw_text = open(spath, encoding="utf-8", errors="replace").read()
    idx = raw_text.find(HEADER_DIVIDER)
    if idx == -1:
        die_structural(f"NO_HEADER_DIVIDER — {cat}::{sid} has no literal '---' + blank line "
                       "separating its provenance header from its body; the body cannot be "
                       "isolated, so its digest cannot be recomputed")
    body = raw_text[idx + len(HEADER_DIVIDER):]
    header = raw_text[:idx]

    # (b) BODY domain — the header's own recorded digest vs a recompute over the stripped body.
    header_sha = None
    for line in header.splitlines():
        if BODY_SHA_LABEL in line and ":" in line:
            candidate = line.rsplit(":", 1)[1].strip().strip("*` ")
            if len(candidate) == 64 and all(c in "0123456789abcdef" for c in candidate.lower()):
                header_sha = candidate.lower()
                break
    if header_sha is None:
        die_structural(f"HEADER_BODY_SHA_MISSING — {cat}::{sid} header declares no "
                       f"'{BODY_SHA_LABEL}' line carrying a 64-hex digest. A refreshed snapshot "
                       "records the digest of its own body; without it there is nothing for the "
                       "receipt chain to attach to")
    body_sha = hashlib.sha256(body.encode("utf-8")).hexdigest()
    if header_sha != body_sha:
        findings.append(
            f"HEADER_BODY_SHA_MISMATCH — {cat}::{sid} header records body sha256={header_sha} "
            f"but the body below the divider hashes to {body_sha}. The body was edited after "
            "the header was written (or the header was written about different bytes)")

    # (c) RECEIPT domain — the same body digest must appear in the committed fetch ledger.
    receipt_sha = row.get("body_sha256")
    if not isinstance(receipt_sha, str) or len(receipt_sha) != 64:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} carries no "
                       "64-hex `body_sha256`; a receipt without a digest records that a fetch "
                       "happened but not what it produced")
    if receipt_sha.lower() != body_sha:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} body hashes to {body_sha} but no receipt "
                       f"describes those bytes (assembly row {row.get('id')!r} records "
                       f"{receipt_sha}). A snapshot and its manifest entry edited together "
                       "WITHOUT a new fetch is exactly this state: refresh is only legal through "
                       "a fetch that appends receipts")
    declared_body_bytes = row.get("body_bytes")
    if declared_body_bytes is not None and declared_body_bytes != len(body.encode("utf-8")):
        findings.append(
            f"RECEIPT_BODY_BYTES — {cat}::{sid} assembly row records body_bytes="
            f"{declared_body_bytes} but the body is {len(body.encode('utf-8'))} bytes")
    refs = row.get("from_receipts") or []
    if not isinstance(refs, list) or not refs:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} references "
                       "no per-URL fetch receipts; an assembly with no inputs is a digest with "
                       "no provenance")
    absent = [r for r in refs if r not in fetch_rows]
    if absent:
        die_structural(f"RECEIPT_MISSING — {cat}::{sid} assembly row {row.get('id')!r} references "
                       f"fetch receipt(s) absent from the ledger: {', '.join(map(str, absent))}. "
                       "Every URL in the chain must be recorded, or the chain has a link nobody "
                       "can inspect")

# ── non-vacuity ──────────────────────────────────────────────────────────────────────
if not disk:
    die_structural("ZERO_SCAN — no manifest id with a committed snapshot body was found under "
                   f"{root}; a run that checksums nothing is not a clean tree")
if live_root:
    if len(disk) < LIVE_MIN_IDS:
        die_structural(f"CENSUS_FLOOR — {len(disk)} manifest id(s) with a committed body < the "
                       f"guard-pinned live floor {LIVE_MIN_IDS} (LIVE_MIN_IDS may not be "
                       "reduced). Snapshot bodies were deleted, which shrinks what this gate "
                       "checks rather than making it pass")
    if len(touched) < LIVE_MIN_TOUCHED:
        die_structural(f"CENSUS_FLOOR — {len(touched)} assembly receipt(s) < the guard-pinned "
                       f"live floor {LIVE_MIN_TOUCHED} (LIVE_MIN_TOUCHED may not be reduced). "
                       "The three-domain chain only runs on ids that have one, so emptying the "
                       "ledger would silently retire it")

if structural:
    for msg in structural:
        print(f"manifest_snapshot_integrity_guard: {msg}", file=sys.stderr)
    sys.exit(2)

if findings:
    for msg in findings:
        print(f"VIOLATION: {msg}", file=sys.stderr)
    print("", file=sys.stderr)
    print(f"manifest_snapshot_integrity_guard: {len(findings)} integrity violation(s) — BLOCKED",
          file=sys.stderr)
    sys.exit(1)

print(f"manifest_snapshot_integrity_guard: PASS — {len(disk)} manifest id(s) with a committed "
      f"body checksummed against disk ({len(allowed)} carried by the shrink-only residual "
      f"allowlist, baseline universe {len(baseline)}); {len(chain_keys)} id(s) verified through "
      f"the full file←body←receipt chain ({len(touched)} identity(ies) with an assembly receipt "
      f"over {assembly_rows} assembly row(s), {len(changed_keys)} changed vs "
      f"{anchor_kind if anchored else 'no anchor'})")
sys.exit(0)
PY
GUARD_RC=$?

# ── P1-2, standalone half: observe the anchor ref a SECOND time ───────────────────────
# (ROUND 4, TD-2026-07-30-P1-anchor-runtime.) The pin check above compares this guard's read
# against the RUNNER's, which only exists when a runner started us. A guard invoked directly has
# no pin, so it authenticates the ref against ITSELF by re-reading it now that the ratchet work
# is done. A ref that moved in between means every conclusion above concerns a commit that is no
# longer the release being extended — and refs/remotes/origin/main is an ordinary local ref.
# HONEST LIMIT: two observations bound a window, they do not eliminate it. A ref moved and
# restored entirely between these two reads is unobserved.
if [ "$LIVE_ROOT" = "1" ] && [ -n "$GIT_ANCHOR" ]; then
    ax_anchor_verify_unmoved "$SELF_REPO_ROOT" "manifest_snapshot_integrity_guard" \
        || exit "$ANCHOR_AUTH_EXIT"
fi
exit "$GUARD_RC"
