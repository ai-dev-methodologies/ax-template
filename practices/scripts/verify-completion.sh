#!/usr/bin/env bash
# practices/scripts/verify-completion.sh — R25/R27/R28 mechanical completion contract.
#
# Reads practices/verification-checklist.yaml and runs every step sequentially.
# Exit 0  → ALL steps PASS (task may declare done).
# Exit 1  → at least one non-advisory step FAILED; fix_playbook printed.
# Exit 2  → setup error (yaml missing, python3 missing, toolchain absent, etc.).
#
# Toolchain preflight (P2-16 — BLOCK, not silent SKIP): before executing the plan
# the script fail-closes with exit 2 + a one-line reason when a REQUIRED toolchain
# for the RESOLVED step set is missing:
#   (i)   no yaml parser  — neither PyYAML (python3 -c 'import yaml') nor yq. Always
#         required (the checklist is yaml). Blocks unconditionally. EITHER parser
#         is genuinely sufficient HERE: the checklist parser below falls back to
#         `yq -o=json` when PyYAML is absent.
#   (ii)  no JDK 21       — only when a backend/gradle step is scheduled. Resolves
#         java via JAVA_HOME then PATH; requires major == 21 (build.gradle.kts
#         toolchain = JavaLanguageVersion.of(21)). The macOS /usr/bin/java stub
#         (no runtime) fails `-version` → unresolved → BLOCK.
#   (iii) no node/npm     — only when the frontend-lint step is scheduled. A
#         fork-receiver running backend-only steps (e.g. --step backend-build) is
#         NOT blocked by missing node — the preflight respects --step filtering by
#         inspecting the already-filtered command plan.
#   (iv)  no PyYAML       — only when a catalog-guard step is scheduled (any command
#         invoking a script under an `evals/` directory). Unlike (i), yq is NOT a
#         substitute here: the guards themselves embed `python3 ... import yaml`
#         with no yq path, and several of them SILENTLY SKIP (exit 0) when PyYAML
#         is absent — i.e. without this check a yq-only machine would report a
#         PASSING R25 while whole guards never ran. Step-gated exactly like (iii):
#         a backend-only run (--step backend-build) is NOT blocked.
# Full prerequisite list: JDK 21, PyYAML or yq (checklist parse) + PyYAML
# specifically (catalog-guard steps), node+npm (frontend steps only), bash, git.
# See CLAUDE.md "R25 toolchain prerequisites".
#
# Test seam (honest, documented): AX_PREFLIGHT_FAKE_MISSING is a pipe/comma list of
# yaml|pyyaml|jdk|node that forces a tool to appear missing, so the block matrix is
# testable without uninstalling toolchains. It ONLY forces-missing; it never
# forces-present. Unset in normal operation.
#
# R28 surface upgrades (additive, no schema change):
#   • per-domain-tests step COLLAPSES its 15 mandatory ./gradlew testXxx tasks
#     into a SINGLE warm-daemon invocation (`--continue` so one fail does not
#     short-circuit). 2 advisory tasks (testIntegration, testPortability) run
#     afterwards individually so their RED is isolated to themselves.
#   • Real-time streaming: gradle `--console=plain` injected automatically so
#     `> Task :testCrud` lines surface immediately (R27 fix, retained).
#   • Per-step timeout (yaml `timeout_seconds`) implemented via background
#     watchdog with SIGTERM → 30s grace → SIGKILL. Race-free: watchdog killed
#     atomically once the command exits.
#   • `--resume` reads .ax-verify/last_run.jsonl and skips steps whose
#     {step_id, head_sha, tree_fingerprint} matches and were PASS. HEAD drift wipes
#     resume, and so does WORKING-TREE drift at the same HEAD: a record produced by a
#     tree with an uncommitted change is not consumable once that change is reverted
#     (the reviewer's "lint passes only while the edit is present" path). A refused
#     record is announced and RE-VERIFIED, never silently skipped.
#     PASS is provenance-bearing: a step that produced no observed command
#     outcome is recorded UNRUN and never skipped, and a run that ends in a
#     ledger BLOCK discards its resume record entirely rather than publishing
#     one a later --resume could consume. Pinned by resume_provenance_guard [96].
#
# Provenance rules (what a PASS is allowed to mean) — [96] layers A–D:
#   • Every result-ledger row carries an ORIGIN column: `executed` (a command
#     actually ran and its exit code was observed), `resumed` (inherited from a
#     PASS record for the SAME head_sha), or `unrun` (a row that exists although
#     nothing ran — an absent working directory). Only `executed`/`resumed` rows
#     are evidence; `unrun` rows are accounting placeholders and can never carry
#     a step to PASS.
#   • An ABSENT working directory is a BLOCK for a non-advisory command (layer C),
#     not a skip. A step whose directory does not exist was not verified, and
#     "silence is not success". Advisory commands stay advisory (WARN).
#   • A step may be recorded PASS only when EVERY planned non-advisory command of
#     that step actually executed with an acceptable outcome (layer D). Otherwise
#     it is UNRUN, which `--resume` never skips.
#   • A MALFORMED CHECKLIST STEP IS A CONTRACT ERROR, not a pass. Two families, both of
#     which certify that nothing happened: a step that cannot be RUN (`commands: []`, or
#     command text that is blank/whitespace/comment-only — `bash -c '# off'` exits 0 and
#     is recorded as an EXECUTED PASS — or a canonical no-op placeholder such as `true`,
#     `:` or `exit 0` whose fixed status is the expected one; finite denylist, see
#     NOOP_PLACEHOLDER_EXIT for what that check does NOT claim), and one that cannot be IDENTIFIED (blank id,
#     non-slug id, duplicate id — STEP_ORDER is iterated unquoted, so a blank id produces
#     ZERO iterations and nothing of that step runs). Selected steps must be a unique slug
#     id + at least one command that actually runs something, enforced at parse time
#     (exit 2), and backstopped structurally after execution: every DECLARED step must
#     have emitted at least one plan row (the plan cannot be its own witness).
#   • RESUME-SKIP is `resumed`, not `executed`: it is publishable only because the
#     record it consumed was itself published by a run at the SAME head_sha that
#     executed the step. By induction every PASS at head H roots in an execution
#     at head H. The resume record carries a "provenance" field so that chain is
#     auditable; it is deliberately NOT a gate (see the preloader comment).
#
# Side effect: writes an audit log line to .ax-verify/runs.jsonl with
#   {ts, head_sha, exit, pass, warn_advisory, hard_fail, skip, full_run,
#    tree_fingerprint, tree_clean, head_sha_end, tree_fingerprint_end, tree_clean_end,
#    tree_stable, tree_samples, anchor_sha, anchor_kind, anchor_sha_end, anchor_stable}
# That key set IS the schema: completion_checklist_recency_guard pins it and cross-checks the pin
# against the printf below, so a hand-authored line with a different field set is refused instead
# of leniently parsed (P1-4, ROUND 4). Changing the fields here requires changing the pin there.
#
# anchor_sha_end/anchor_stable are the anchor's CLOSING endpoint (P1-2, ROUND 4): the ref the
# ratchets measure against is an ordinary local ref, so it can be moved for the minutes the
# guards run and restored before this line is written. It is now read at both ends, exactly like
# the tree, and a drift makes the run a HARD FAIL that is still written to the log.
#
# anchor_sha/anchor_kind record WHICH RELEASE the ratcheting guards measured against. They are
# resolved by practices/scripts/lib/release_anchor.sh — the same helper the guards source — and
# the pre-push hook requires anchor_sha to equal the sha the REMOTE advertises for the ref being
# pushed. Without that binding, `git update-ref refs/remotes/origin/main <synthetic>` (an
# ordinary local ref write, invisible to the tree fingerprint) makes every ratchet bootstrap-skip
# and R25 pass. See TD-2026-07-30-P1-anchor-authenticity.
# so that completion_checklist_recency_guard.sh can audit recency AND provenance.
# tree_fingerprint/tree_clean exist because head_sha alone does not identify the code that
# was verified: R25 runs on dirty trees, so one head covers arbitrarily many trees. PUSH
# eligibility therefore requires evidence produced from the CLEAN tree of the pushed commit
# (tree_clean=true) — an uncommitted fix that makes the run pass, then stashed, no longer
# certifies the commit that ships. LOCAL iteration is untouched: dirty-tree runs stay
# perfectly usable, including for --resume.
# The *_end / tree_stable / tree_samples fields exist because a RUN IS NOT AN INSTANT: the
# start values are measured before the first step and the line is written after the last one,
# tens of minutes later. They report the same three facts at the closing endpoint plus whether
# every sample taken at the step boundaries in between agreed, so push eligibility can require
# start == end == clean instead of trusting one boolean captured before any step ran.
#
# Iron Law: this script is the SOLE source of truth for "is the task done".
# Do NOT bypass with --skip flags. There is no opt-out.
#
# Usage:
#   bash practices/scripts/verify-completion.sh
#   bash practices/scripts/verify-completion.sh --step <id>   # run one step only (partial:
#                                                # audit line gets full_run=false and does
#                                                # NOT satisfy the R25 recency guard)
#   bash practices/scripts/verify-completion.sh --dry-run     # parse + plan, no exec
#   bash practices/scripts/verify-completion.sh --json        # emit machine-readable summary
#   bash practices/scripts/verify-completion.sh --resume      # skip prior PASS steps (same HEAD)
#   bash practices/scripts/verify-completion.sh --no-collapse # disable per-domain collapse
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
_AX_PF_LABEL="verify-completion"
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
_AX_HRM_LABEL="verify-completion"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=1
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

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd)"
# ── HERMETIC RUNTIME BOOTSTRAP (B): bind the git identity to the trusted root ────────
# Part A removed the inherited git context; this derives the real one and REQUIRES it to be the
# root this entry resolved for itself. `git -C <root>` WALKS UP when <root> is not itself a work
# tree, so without this a gate can authenticate a repository that merely CONTAINS the directory it
# is scanning. The derived gitdir/worktree are then passed EXPLICITLY on every call for this root
# (see ax_git), so nothing downstream depends on discovery at all.
# NOT exported, and unset first: a binding that could arrive from the environment would be a
# NEW redirection channel of exactly the kind part A just closed. Every entry derives its own.
unset AX_GIT_BOUND_ROOT AX_GIT_BOUND_DIR
if "$AX_GIT_BIN" -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_hcan="$(builtin cd "$REPO_ROOT" 2>/dev/null && builtin pwd -P)"
    _ax_htop="$("$AX_GIT_BIN" -C "$REPO_ROOT" rev-parse --show-toplevel 2>/dev/null)"
    _ax_htop="$(builtin cd "${_ax_htop:-/nonexistent}" 2>/dev/null && builtin pwd -P)"
    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then
        { echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the git work tree answering this gate's"
          echo "  reads is '${_ax_htop:-<unresolvable>}', not the root it was invoked for"
          echo "  ('${_ax_hcan:-$REPO_ROOT}'). Every head / status / fingerprint / anchor answer would"
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

CHECKLIST="$REPO_ROOT/practices/verification-checklist.yaml"
AUDIT_DIR="$REPO_ROOT/.ax-verify"
AUDIT_LOG="$AUDIT_DIR/runs.jsonl"
RESUME_LOG="$AUDIT_DIR/last_run.jsonl"
COLLAPSE_HELPER="$SCRIPT_DIR/_collapse_plan.py"

STEP_FILTER=""
DRY_RUN=0
JSON_OUTPUT=0
RESUME=0
COLLAPSE=1

while [ $# -gt 0 ]; do
    case "$1" in
        --step) STEP_FILTER="$2"; shift 2 ;;
        --step=*) STEP_FILTER="${1#--step=}"; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        --json) JSON_OUTPUT=1; shift ;;
        --resume) RESUME=1; shift ;;
        --no-collapse) COLLAPSE=0; shift ;;
        --help|-h)
            sed -n '2,30p' "$0"; exit 0 ;;
        *)
            echo "verify-completion: unknown arg: $1" >&2
            echo "use --help for usage" >&2
            exit 2
            ;;
    esac
done

if [ ! -f "$CHECKLIST" ]; then
    echo "verify-completion: checklist yaml missing: $CHECKLIST" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "verify-completion: python3 not in PATH (required for yaml parsing)" >&2
    exit 2
fi

# ── Toolchain preflight seam ─────────────────────────────────────────────────
# AX_PREFLIGHT_FAKE_MISSING forces a tool (yaml|pyyaml|jdk|node) to appear missing so
# the BLOCK matrix is testable without uninstalling toolchains (see header).
AX_PREFLIGHT_FAKE_MISSING="${AX_PREFLIGHT_FAKE_MISSING:-}"
preflight_faked() {
    case "|${AX_PREFLIGHT_FAKE_MISSING//,/|}|" in
        *"|$1|"*) return 0 ;;
        *) return 1 ;;
    esac
}

# ── Preflight (i): yaml parser — required unconditionally (checklist is yaml) ──
# ROUND 6 / P1-2(c): the probe now runs the AUTHENTICATED interpreter with the SAME flags the
# consuming call site uses (-E), because a capability established under one interpreter/flag set
# says nothing about another. It BLOCKS; it never degrades to a skip — 'the parser was missing so
# we did not check' is the fail-open this whole round is about.
if preflight_faked yaml || { ! "$AX_PY_BIN" -E -c 'import yaml' >/dev/null 2>&1 && ! command -v yq >/dev/null 2>&1; }; then
    echo "verify-completion: R25 BLOCK (HERMETIC_PY_YAML_UNAVAILABLE): the authenticated" >&2
    echo "  interpreter $AX_PY_BIN cannot 'import yaml' under -E and yq is not on PATH, so the" >&2
    echo "  checklist cannot be parsed. Install PyYAML for THIS interpreter" >&2
    echo "  ($AX_PY_BIN -m pip install pyyaml) or install yq. Blocking, not skipping." >&2
    exit 2
fi

mkdir -p "$AUDIT_DIR"

# ── P1-1 (ROUND 4, TD-2026-07-30-P1-anchor-runtime): git must interpret objects as they ARE ──
# `git replace <real> <fabricated>` keeps every sha identical and swaps the OBJECT every ordinary
# git command reads. Every head/status/diff below — and the tree fingerprint's python
# subprocesses, and every guard this runner starts — would then be describing a history the
# attacker wrote while this file records the authentic shas. Exported HERE, before the FIRST git
# call in this script, because an export placed after one is a check that arrives too late.
# (The shared helper exports it too; that is for guards started without this runner.)
export GIT_NO_REPLACE_OBJECTS=1

CURRENT_HEAD="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo "unknown")"

# ── Which RELEASE did the ratcheting guards measure against? (P1-X layer 3) ──
# (cross-family reviewer ROUND 3, 2026-07-30; TD-2026-07-30-P1-anchor-authenticity.)
# Two guards in this run ratchet against "the previous release", which they resolve from
# refs/remotes/origin/main. THAT IS AN ORDINARY LOCAL REF — `git update-ref` can aim it at a
# synthetic commit whose tree merely DROPS the ratcheting files, at which point every ratchet
# takes its first-release bootstrap skip and R25 passes. The tree fingerprint above cannot see
# it: a ref is not part of the working tree.
# So the audit line RECORDS the anchor the guards used, resolved by the SAME helper they source
# (practices/scripts/lib/release_anchor.sh) so runner and guards cannot disagree, and the
# pre-push hook — which git hands the AUTHORITATIVE remote sha, taken from the remote's own
# advertisement rather than from any local ref — requires the two to be EQUAL. A forged ref
# therefore produces an audit line bound to a commit the remote does not have, and the push is
# refused. Layers (1) ancestry and (2) bootstrap-plausibility live in the guards themselves;
# this is the layer that holds even if both were bypassed.
#
# ── P1-3(b) INLINE HELPER PREFLIGHT — DELIBERATELY DUPLICATED (ROUND 4) ──────────────
# `[ -f ]`/`.` FOLLOW SYMLINKS, so the helper path could be a link to anything and the loader
# would source it. The check has to run BEFORE the source, which means it cannot live inside the
# helper — that would be the object under test certifying itself. Duplicated verbatim in both
# ratcheting guards and in .githooks/pre-push; the duplication IS the bootstrap.
_ax_pf_rel="practices/scripts/lib/release_anchor.sh"
_ax_pf_cur="$REPO_ROOT"
for _ax_pf_part in practices scripts lib release_anchor.sh; do
    _ax_pf_cur="$_ax_pf_cur/$_ax_pf_part"
    if [ -L "$_ax_pf_cur" ]; then
        echo "verify-completion: R25 BLOCK: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} resolves through" >&2
        echo "  a SYMLINK at ${_ax_pf_cur}. The release-anchor helper decides which commit this run" >&2
        echo "  records as the release it ratcheted against; a link makes the bytes that are SOURCED" >&2
        echo "  differ from the bytes git records for that path." >&2
        exit 2
    fi
done
if "$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_pf_mode="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" ls-tree HEAD -- "$_ax_pf_rel" 2>/dev/null | head -1)"
    _ax_pf_mode="${_ax_pf_mode%% *}"
    case "${_ax_pf_mode:-}" in
        100644|100755|"") ;;
        *) echo "verify-completion: R25 BLOCK: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} has git mode" >&2
           echo "  ${_ax_pf_mode} at HEAD, which is not a regular file blob (100644/100755)." >&2
           exit 2 ;;
    esac
fi
if [ ! -f "$SCRIPT_DIR/lib/release_anchor.sh" ]; then
    echo "verify-completion: R25 BLOCK: RELEASE_ANCHOR_LIB_MISSING — practices/scripts/lib/release_anchor.sh" >&2
    echo "  is absent. It resolves the release anchor this run records; without it the audit line" >&2
    echo "  cannot say which release was ratcheted against, so the run BLOCKS rather than writing" >&2
    echo "  an unbound record. (No environment override is consulted here: the runner always uses" >&2
    echo "  the committed helper.)" >&2
    exit 2
fi
# shellcheck source=practices/scripts/lib/release_anchor.sh
. "$SCRIPT_DIR/lib/release_anchor.sh"
# P1-1: a tree carrying git REPLACEMENT REFS can answer every ancestry/history question out of a
# fabricated object graph while every sha stays authentic. The guards refuse to ratchet on such a
# tree; the runner refuses to certify one, for the same reason and before spending the run.
if ! ax_anchor_check_replace_refs "$REPO_ROOT" "verify-completion"; then
    echo "verify-completion: R25 BLOCK: ANCHOR_REPLACE_REFS_PRESENT (see above)" >&2
    exit 2
fi
ax_anchor_resolve "$REPO_ROOT"
# P1-2 SINGLE-RESOLUTION BINDING: resolve ONCE, then publish that resolution to every child
# process. Each guard re-reads the ref at its own moment of use and must get the same commit
# (ANCHOR_REF_MOVED_MIDRUN), and this runner re-verifies it once more after the last step. The
# audit line's anchor_sha is therefore the value the guards were held to, not a fourth
# independent read of a ref that anything can move in between.
ax_anchor_export_pin "$REPO_ROOT"
ANCHOR_SHA="${AX_ANCHOR_SHA:-unavailable}"
ANCHOR_KIND="${AX_ANCHOR_KIND:-unavailable}"
ANCHOR_REF_AT_START="${AX_ANCHOR_REF:-}"

# ── ROUND 5 / P1-3: THE RATCHET'S OWN IMPLEMENTATION MUST BE THE COMMITTED ONE ───────
# Every check this run makes about these six files asks whether they are REGULAR FILES. None
# asked what they SAY — and the measured attack is a regular-file tree_fingerprint.py rewritten
# to print a constant, after which the runner that WRITES the evidence and the verifier that
# RECOMPUTES it are the same compromised implementation. Comparing the working-tree bytes against
# what git records at HEAD closes the uncommitted half here (the push gate closes the committed
# half by recomputing with the PREVIOUS RELEASE's copy).
# Deliberate cost: editing the ratchet's own code now blocks R25 until the edit is committed.
if ! ax_ratchet_toolchain_authentic "$REPO_ROOT" "verify-completion" "HEAD" \
        $(ax_ratchet_toolchain_paths); then
    echo "verify-completion: R25 BLOCK: RATCHET_TOOLCHAIN_MODIFIED (see above)" >&2
    exit 2
fi

# ── Working-tree fingerprint — what a resume record is actually bound to ─────
# head_sha alone does NOT identify the code that ran. R25 is routinely invoked on a DIRTY
# tree, so at one head H the tree can differ arbitrarily between two invocations:
#   1. at H, an UNCOMMITTED edit makes `--step frontend-lint` pass → PASS record published
#   2. the edit is reverted/stashed — head is still H, the lint failure is back
#   3. `--resume` skips frontend-lint on that record and publishes full_run=true for H,
#      which completion_checklist_recency_guard accepts. The failing tree was never linted.
# So each record additionally carries a fingerprint of the tree that produced it, and the
# preloader refuses any record whose fingerprint differs from the tree in front of it.
# Refusal is loud and means RE-VERIFY (the step simply runs again) — never a silent skip.
#
# The fingerprint hashes, deterministically: `git status --porcelain -z -uall` (the set and
# state of dirty/untracked paths), `git diff HEAD --binary` (the CONTENT of every tracked
# modification, staged or not), and the bytes of every untracked non-ignored file (invisible
# to `git diff HEAD`). Ignored paths are excluded by git itself, so build/, node_modules/
# and .ax-verify/ churn does not invalidate a record — measured cost on this repo: ~60ms.
#
# HONEST LIMIT: in a NON-git working tree there is no cheap tree identity, and the value
# degrades to the constant "nogit" — head_sha is "unknown" there too, so such a run can
# never serve as push evidence anyway (the recency guard is a git hook matching real SHAs).
# Chosen over "require a clean worktree to publish/consume": that would be sound but would
# delete resume for its actual use case (a long full run on a work-in-progress tree), and
# a fingerprint gives the same guarantee — a record is consumable only by the tree that
# produced it — without forbidding the workflow.
# P1-4 (ROUND 4): the algorithm moved to practices/scripts/lib/tree_fingerprint.py so that the
# recency guard can RECOMPUTE this value instead of trusting the number this script wrote. Two
# copies of a hash would be two chances to drift, and a drifted recompute fails honest runs.
# The behaviour is unchanged: prints the digest, or the constant "nogit" for a non-git tree.
# ROUND 5 / P1-3: FAIL CLOSED, and say WHY out loud. The helper distinguishes four states by exit
# code — 0 a digest (or the honest "nogit" of a non-git tree), 3 GIT_CONTEXT_REDIRECTED, 4 a git
# tree it could not read, and (ROUND 6 / P1-4) 5 GIT_FILTERS_PRESENT: a repository that declares a
# `filter.<n>.clean` cannot be fingerprinted honestly, because every content answer git gives is
# then the FILTER'S OUTPUT — and on a repository that IS git anything but a 64-hex digest is a
# BLOCK. The old shape degraded silently to a per-run "unverifiable-" placeholder, which merely
# moved the failure to the push gate and, in the meantime, let a run report a tree it never
# identified.
compute_tree_fingerprint() {
    "$AX_PY_BIN" -I -S "$SCRIPT_DIR/lib/tree_fingerprint.py" "$REPO_ROOT"
}
# ax_fp_is_digest <value> — the SHAPE is checked, not merely non-emptiness: "x" and "nogit" both
# satisfied "the tree was identified" before this round.
ax_fp_is_digest() {
    case "${1:-}" in
        *[!0-9a-f]*|"") return 1 ;;
        *) [ "${#1}" -eq 64 ] ;;
    esac
}
IS_GIT_TREE=0
[ -n "${AX_GIT_BOUND_ROOT:-}" ] && IS_GIT_TREE=1
# Prints the fingerprint; returns 1 when it is unusable ON A GIT TREE. The BLOCK itself lives in
# ax_fp_block, called by the caller — an `exit` inside `$( … )` would kill only the subshell and
# leave the run going with an empty value, which is the fail-open shape in miniature.
compute_tree_fingerprint_checked() {
    local fp rc
    fp="$(compute_tree_fingerprint 2>/dev/null)"; rc=$?
    if [ "$IS_GIT_TREE" -eq 1 ] && { [ "$rc" -ne 0 ] || ! ax_fp_is_digest "$fp"; }; then
        printf '%s' "${fp:-<nothing>} (exit ${rc})"
        return 1
    fi
    [ -n "$fp" ] || fp="unverifiable-$$-$(date -u +%s)"
    printf '%s' "$fp"
}
ax_fp_block() {
    {
        echo "verify-completion: R25 BLOCK: FINGERPRINT_UNVERIFIABLE — the working-tree fingerprint"
        echo "  helper returned '${1:-<nothing>}' on a git work tree (observed ${2:-at start})."
        echo "  That value is the only thing binding this run's evidence to the code it ran on, and"
        echo "  an unusable one used to degrade into a placeholder instead of stopping. A redirected"
        echo "  git context (helper exit 3) is the shape that matters: it makes a DIRTY tree report"
        echo "  the clean-tree constant, i.e. a certificate for code nobody has."
    } >&2
    exit 2
}
CURRENT_TREE_FP="$(compute_tree_fingerprint_checked)" || ax_fp_block "$CURRENT_TREE_FP"

# ── Was this run performed on the COMMITTED tree of head_sha? ────────────────
# The fingerprint above says WHICH tree ran; this says whether that tree is the one a
# reader of head_sha would get. They answer different questions, and the second is what
# PUSH evidence needs: a push ships a COMMIT, so evidence gathered from a working tree
# that differs from that commit is evidence about code nobody will receive. The reviewer's
# path needs no --resume at all — R25 passes because of an uncommitted fix, the fix is
# stashed, and the untouched audit line still certifies the commit.
# Recorded (never enforced here): a dirty-tree run remains fully usable for local
# iteration and for --resume; only the push gate reads this field.
# HONEST LIMIT: git-ignored paths are excluded, because git cannot pin them and the run
# genuinely depends on some of them (node_modules/, build/). "Clean" therefore means
# "identical to the commit in every path git tracks or would track".
#
# SINGLE DECIDER: every cleanliness answer in this script comes from here, at every sample
# point. Two independent copies of the test would let a mutation neuter one and leave the
# other honest, which would make the push-evidence mutation matrix [97] report a layer as
# load-bearing when it is not.
observe_tree_clean() {
    local clean=false
    if "$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse --verify HEAD >/dev/null 2>&1 \
        && [ -z "$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" status --porcelain -uall 2>/dev/null)" ]; then
        clean=true
    fi
    printf '%s' "$clean"
}
CURRENT_TREE_CLEAN="$(observe_tree_clean)"

# ── The run is not an instant: sample the tree ACROSS it ─────────────────────
# The three facts above (head, fingerprint, cleanliness) were captured BEFORE the first step
# and the audit line is written AFTER the last one. A full run takes tens of minutes, and
# every one of those minutes sits inside that window:
#   1. start clean at a HEAD whose committed state FAILS a later step
#   2. while an early long step runs, make the uncommitted fix that step needs
#   3. the later step passes — because of an edit no commit contains
#   4. revert it; the run ends, the audit line still says tree_clean=true
#   5. push. The consumer trusted one boolean captured before any of this happened.
# So the endpoints are re-measured after execution AND the tree is re-sampled at every step
# boundary, and the audit records both endpoints plus whether every sample agreed. The
# consumer then VERIFIES a relation (start == end, and stable across the run) instead of
# trusting a single value.
#
# HONEST LIMIT — sampling granularity: observations happen at step boundaries and at the end
# of the run. A change made AND undone entirely inside one step's execution is not observed;
# this narrows the window from "the whole run" to "one step", it does not eliminate it.
# Closing it completely would need the tree to be immutable for the duration (a read-only
# snapshot / container), which is a fork-receiver infrastructure decision, not a shell check.
#
# ROUND 5 / P1-3(c) — A DIRTY SAMPLE IS PERMANENT. The closing endpoint used to be simply the LAST
# sample, so an intermediate observation of a dirty tree (or of a moved anchor) could be erased by
# a clean ending. Worse, the only thing that made an intermediate dirt visible was the FINGERPRINT
# differing — i.e. it depended on the very helper P1-3 shows can be replaced. The two accumulators
# below are therefore independent of the fingerprint: ANY sample that observes `git status`
# non-empty makes tree_clean_end false FOR THE WHOLE RUN, and any sample that observes the anchor
# ref elsewhere makes anchor_stable false for the whole run. They only ever move one way.
START_TREE_FP="$CURRENT_TREE_FP"
START_TREE_CLEAN="$CURRENT_TREE_CLEAN"
TREE_SAMPLES=1
TREE_STABLE=true
TREE_EVER_DIRTY=false
[ "$CURRENT_TREE_CLEAN" = true ] || TREE_EVER_DIRTY=true
ANCHOR_STABLE=true
ANCHOR_MOVED_SEEN=""

# observe_tree <where> — take a sample, bind subsequent resume records to it, and record any
# drift from the start sample. Costs one `git status` + one `git diff HEAD` (~60ms here).
observe_tree() {
    local where="$1" fp clean head anchor_now
    fp="$(compute_tree_fingerprint_checked)" || ax_fp_block "$fp" "$where"
    clean="$(observe_tree_clean)"
    head="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo "unknown")"
    TREE_SAMPLES=$((TREE_SAMPLES + 1))
    if [ "$clean" != true ] && [ "$TREE_EVER_DIRTY" = false ]; then
        TREE_EVER_DIRTY=true
        echo "  ⚠ the working tree was DIRTY when sampled $where — this run can no longer" >&2
        echo "    certify a push, and no later clean sample will erase that." >&2
    fi
    # The anchor gets the same treatment as the tree, and for the same reason: the ref is an
    # ordinary local one, so between two endpoint reads it can be aimed at a commit that lacks the
    # ratcheting files (every ratchet bootstrap-skips) and put back. Sampling at every step
    # boundary narrows that window to one step, exactly like the tree.
    if [ -n "$ANCHOR_REF_AT_START" ] && [ "$ANCHOR_SHA" != "unavailable" ]; then
        anchor_now="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse --verify --quiet \
            "${ANCHOR_REF_AT_START}^{commit}" 2>/dev/null)"
        if [ "$anchor_now" != "$ANCHOR_SHA" ]; then
            if [ "$ANCHOR_STABLE" = true ]; then
                echo "  ⚠ the release anchor ${ANCHOR_REF_AT_START} MOVED during this run (observed" >&2
                echo "    $where): ${ANCHOR_SHA} → ${anchor_now:-<unresolvable>}. Recorded permanently." >&2
            fi
            ANCHOR_STABLE=false
            ANCHOR_MOVED_SEEN="$where"
        fi
    fi
    if [ "$fp" != "$START_TREE_FP" ] || [ "$head" != "$CURRENT_HEAD" ]; then
        if [ "$TREE_STABLE" = true ]; then
            echo "  ⚠ the working tree CHANGED during this run (observed $where)." >&2
            echo "    start: head=${CURRENT_HEAD:0:12} tree=${START_TREE_FP:0:12}" >&2
            echo "    now  : head=${head:0:12} tree=${fp:0:12}" >&2
            echo "    The run stays usable locally, but it can no longer certify a push: the steps" >&2
            echo "    before and after this point verified DIFFERENT code. Re-run on a settled tree." >&2
        fi
        TREE_STABLE=false
    fi
    # Resume records published from here on are bound to the tree that actually produced
    # them, not to the one this run started with.
    CURRENT_TREE_FP="$fp"
    CURRENT_TREE_CLEAN="$clean"
}

# ── 1. Parse the checklist into a flat command plan via python3 ───────────────
# Output schema (one line per command, tab-separated):
#   <step_id>\t<step_title>\t<command>\t<working_directory>\t<expected_exit>\t<advisory>\t<timeout_seconds>
PLAN_FILE=$(mktemp)
RESULTS_FILE=$(mktemp)
PLAYBOOK_DIR=$(mktemp -d)
RESUME_TMP=$(mktemp)
# The logical steps the checklist DEMANDED for this run (<index>\t<id>), written by the
# emitter beside the plan. Independent of the plan so "a step vanished" is detectable.
DECLARED_STEPS="$PLAN_FILE.steps"
cleanup() {
    # RESUME_NEW is defined later (line ~226); guard with :- so an early-exit
    # trap before it is set does not trip `set -u`. The .failfast/.steps sidecars are
    # written next to PLAN_FILE by the python emitter — remove them here too.
    rm -f "$PLAN_FILE" "$PLAN_FILE.failfast" "$DECLARED_STEPS" "$RESULTS_FILE" \
        "$RESUME_TMP" "${RESUME_NEW:-}"
    rm -rf "$PLAYBOOK_DIR"
}
trap cleanup EXIT

# ROUND 6 / P1-2, THE ONE DELIBERATE EXCEPTION TO -I -S: this block needs PyYAML, which lives in
# site-packages (and, on the maintainer machine, in the USER site directory that -I removes —
# measured: `python3 -I -c "import yaml"` → ModuleNotFoundError). So it runs with -E, which still
# refuses PYTHONPATH / PYTHONHOME / PYTHONSTARTUP — the reviewer's injection vector — while
# leaving the interpreter's own installed packages reachable. The residual (a sitecustomize.py
# already inside site-packages) is the same trust domain as PyYAML itself, and is stated rather
# than papered over. The capability probe above BLOCKS when yaml is unavailable; it never skips.
"$AX_PY_BIN" -E - "$CHECKLIST" "$STEP_FILTER" "$PLAN_FILE" "$PLAYBOOK_DIR" <<'PYEOF'
import sys, pathlib, re

try:
    import yaml
except ImportError:
    yaml = None

checklist_path, step_filter, plan_path, playbook_dir = sys.argv[1:5]
text = pathlib.Path(checklist_path).read_text()

if yaml is None:
    import subprocess, json
    try:
        out = subprocess.check_output(["yq", "-o=json", ".", checklist_path])
        doc = json.loads(out)
    except Exception as e:
        print(f"verify-completion: cannot parse yaml without PyYAML or yq ({e})", file=sys.stderr)
        sys.exit(2)
else:
    doc = yaml.safe_load(text)

defaults = doc.get("defaults") or {}
default_wd = defaults.get("working_directory", ".")
default_timeout = int(defaults.get("timeout_seconds", 900))

# ── The malformed-step contract (parse-time BLOCK, exit 3) ───────────────────
# A step that cannot be RUN and a step that cannot be IDENTIFIED are both invisible to
# the accounting below, and each has its own way of certifying that nothing happened:
#
#   commands: []          — emits no plan row, so the step vanishes from STEP_ORDER, and
#                           every downstream accounting check iterates STEP_ORDER. A full
#                           run with one other (green) step publishes full_run=true while
#                           a required step of the contract was never verified.
#   command: "# disabled" — `bash -c '# disabled'` exits 0. The runner observes an exit
#   command: "   "          code, records origin=executed, and the step is a genuine PASS
#                           by every provenance rule we have. Nothing ran.
#   command: "true" / ":"  — identical outcome via the canonical "switch this step off"
#   command: "exit 0"       idioms: a fixed exit status matching expected_exit, no program of
#                           the contract run. Rejected by a FINITE denylist only — see the
#                           NOOP_PLACEHOLDER_EXIT comment for the boundary this does NOT claim.
#   id: "" / id: absent   — STEP_ORDER is iterated with `for sid in $STEP_ORDER`, which
#                           word-splits: a blank id yields ZERO shell iterations, so none
#                           of the step's commands execute, and the counters stay green.
#   id: "a b" / "a*b"     — same iteration, but worse: word-splitting and globbing make
#                           the id match other steps' rows or none at all.
#   duplicate id          — STEP_ORDER dedupes by id, so two logical steps merge into one
#                           verdict and one accounting row.
#   commands: ["x"]       — a non-mapping entry has no `command:` key at all.
#
# All of them are contract errors, not results, so they BLOCK at parse time (exit 3) with
# the offending step named. Only SELECTED steps are checked: --step filtering `continue`s
# first, so a backend-only partial run is not blocked by a malformed step elsewhere.
# Structurally backstopped after execution by the declared-step check in the runner (a
# declared step that emitted no plan row, or that has no usable id, blocks there too).
SLUG_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\Z")

# ── The no-op PLACEHOLDER denylist — a finite list, and deliberately nothing more ──
# A step disabled by hand is normally disabled with one of a handful of canonical idioms.
# Each has a FIXED exit status and runs no program of the contract, so when that status is
# the step's expected_exit the runner observes an exit code, records origin=executed, and
# certifies the step — with nothing verified. That is the same certification the blank /
# comment-only shapes produce.
#
# BOUNDARY, stated once here and repeated in every message this produces: deciding whether
# an ARBITRARY shell command performs meaningful work is undecidable, and this check does not
# attempt it. It recognises the placeholder idioms below and nothing else. `curl -s x >
# /dev/null`, a script whose body is empty, a gradle task with no tests — all stay admitted.
# What this buys is narrow and real: a required step cannot be switched off by writing the
# canonical "do nothing, exit success" token into it.
#
# The map is TOKEN → the exit status it always produces. A placeholder is a contract error
# only when that status MATCHES the step's expected_exit, i.e. only when it would CERTIFY the
# step. `command: false` under the default `expected_exit: 0` is not a placeholder pass — it
# is an honest FAIL, and several catalog harnesses legitimately use exactly that to force a
# RED. Blocking it would be over-correction: it certifies nothing.
NOOP_PLACEHOLDER_EXIT = {
    ":": 0, "true": 0, "/bin/true": 0, "/usr/bin/true": 0, "exit": 0, "exit 0": 0,
    "false": 1, "/bin/false": 1, "/usr/bin/false": 1, "exit 1": 1,
}
# `;`, `&&`, `||` and newlines separate the fragments a chain of placeholders is written as
# (`true; true`, `: && true`). Splitting is only ever used to REQUIRE that EVERY fragment is
# a placeholder — a chain containing one real command has a non-placeholder fragment and is
# admitted, so the split can never widen the denylist.
#
# P3-88: once every fragment is known to be a placeholder, the chain's exit status is not
# "the last fragment's" — it is whatever SHORT-CIRCUIT evaluation produces. The old model
# read the last fragment and therefore admitted two shapes that do certify a step with
# nothing run: `true || false` (real status 0; the model said 1) and `false && true` (real
# status 1; the model said 0). Every token here has a FIXED status, so the chain is fully
# decidable — no shell semantics are being guessed. `exit N` additionally TERMINATES the
# shell, so nothing after an executed `exit` runs (`exit 1 || true` is status 1, not 0).
#
# The boundary is unchanged and still finite: this models `;`, `&&`, `||` and newlines over
# chains whose every fragment is one of the tokens above. Wrappers (`bash -c …`), grouping
# (`{ …; }`, `( … )`), pipelines (`|`), negation (`!`), backgrounding (`&`), redirections and
# any variable expansion put the command outside the denylist entirely — it is ADMITTED,
# because deciding whether an arbitrary shell command does work is undecidable.
NOOP_SEPARATOR_RE = re.compile(r"\s*(;|&&|\|\|)\s*")
NOOP_TRAILING_OP_RE = re.compile(r"(?:;|&&|\|\|)\s*\Z")
NOOP_TRAILING_COMMENT_RE = re.compile(r"\s+#.*\Z")


def noop_chain_status(lines):
    """Exit status of an ALL-PLACEHOLDER chain, or None when the shape is not one.

    Returns None (→ the command is ADMITTED) whenever any fragment is not a known
    placeholder token, which is what keeps this a denylist rather than an analysis.
    """
    joined = ""
    for line in lines:
        if not joined:
            joined = line
        elif NOOP_TRAILING_OP_RE.search(joined):
            joined += " " + line          # bash continues the chain after a dangling ;/&&/||
        else:
            joined += " ; " + line        # otherwise a newline is a command separator

    parts = NOOP_SEPARATOR_RE.split(joined)
    frags = [NOOP_TRAILING_COMMENT_RE.sub("", f).strip() for f in parts[0::2]]
    ops = parts[1::2]
    if frags and frags[-1] == "" and ops:     # a trailing `;` leaves one empty fragment
        frags, ops = frags[:-1], ops[:-1]
    if not frags or any(f not in NOOP_PLACEHOLDER_EXIT for f in frags):
        return None

    status = NOOP_PLACEHOLDER_EXIT[frags[0]]
    if frags[0].startswith("exit"):
        return status                          # the shell is gone; the rest is unreachable
    for i, frag in enumerate(frags[1:], start=1):
        op = ops[i - 1]
        if op == "&&" and status != 0:
            continue                           # short-circuited: not executed
        if op == "||" and status == 0:
            continue                           # short-circuited: not executed
        status = NOOP_PLACEHOLDER_EXIT[frag]
        if frag.startswith("exit"):
            return status
    return status


def command_runs_nothing(text, expected_exit=0):
    """Reason label when the shell would run NOTHING for this step, else None.

    Two families, both of which certify that nothing happened:
      • blank / whitespace / comment-only text — `bash -c '# off'` exits 0 with no program run
      • a canonical no-op PLACEHOLDER (see NOOP_PLACEHOLDER_EXIT) whose fixed exit status is
        the one the step expects, so it is recorded as an EXECUTED PASS
    """
    effective = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        effective.append(stripped)
    if not effective:
        return "blank" if not text.strip() else "comment-only"

    # A trailing comment is stripped for THIS test only (`true  # disabled for now` is the
    # same placeholder). Stripping is safe even where it is wrong — e.g. `grep '#' f` becomes
    # `grep '` — because the result is then matched against the denylist, and anything that is
    # not literally one of those tokens is admitted.
    #
    # P3-88: the chain's status comes from SHORT-CIRCUIT evaluation, not from the last
    # fragment — see noop_chain_status. `true || false` and `false && true` are placeholders
    # that certify a step; the previous last-fragment model admitted both.
    status = noop_chain_status(effective)
    if status is None:
        return None
    # expected_exit is compared as an int because the runner compares it with `[ -eq ]`,
    # which treats `expected_exit: "0"` and `expected_exit: 0` identically; an unparsable
    # value is left to the runner to reject rather than silently treated as 0.
    try:
        expected = int(expected_exit)
    except (TypeError, ValueError):
        return None
    if status != expected:
        return None
    return "a no-op placeholder"


def malformed_reason(step, sid, seen_ids):
    """None when the step is a well-formed contract; else a one-line human reason."""
    if not isinstance(sid, str) or not sid.strip():
        return ("declares no usable id (`id:` is empty, absent, or not a string). A step "
                "without an id cannot be iterated, so none of its commands would run")
    if not SLUG_RE.match(sid):
        return (f"id {sid!r} is not a slug — ids are iterated unquoted, so whitespace and "
                f"glob characters silently corrupt step accounting. Use [A-Za-z0-9._-]")
    if sid in seen_ids:
        return (f"id {sid!r} is declared more than once — duplicate ids merge into a single "
                f"verdict, so one of the two logical steps is never accounted for")
    commands = step.get("commands") or []
    if not commands:
        return ("declares no commands (`commands:` is empty or absent). A step with nothing "
                "to run cannot be verified, and it would disappear from the plan silently")
    for pos, entry in enumerate(commands, 1):
        if not isinstance(entry, dict):
            return (f"command #{pos} is not a mapping (got {type(entry).__name__}) — expected "
                    f"`- command: <shell>`")
        text = entry.get("command")
        if not isinstance(text, str):
            return (f"command #{pos} has no `command:` string (got {type(text).__name__})")
        expected_exit = entry.get("expected_exit", 0)
        shape = command_runs_nothing(text, expected_exit)
        if shape:
            idioms = ", ".join(repr(k) for k in sorted(NOOP_PLACEHOLDER_EXIT))
            return (f"command #{pos} is {shape} ({text.strip()[:48]!r}) — it exits "
                    f"{expected_exit} without running anything, so it would be recorded as an "
                    f"EXECUTED PASS. BOUNDARY: this rejects blank/comment-only text plus a "
                    f"FINITE denylist of placeholder idioms ({idioms}), and only when the "
                    f"idiom's fixed exit status matches this command's expected_exit — i.e. "
                    f"only when it would CERTIFY the step. It does NOT, and cannot, prove that "
                    f"any other command does useful work")
    return None


lines = []
fail_fast_sids = []
selected_steps = []
seen_ids = set()
playbook_out = pathlib.Path(playbook_dir)
for step_index, step in enumerate(doc.get("checklist") or [], 1):
    if not isinstance(step, dict):
        print(f"verify-completion: R25 BLOCK: checklist entry #{step_index} is not a mapping "
              f"(got {type(step).__name__}); expected a step with `id:` and `commands:`.",
              file=sys.stderr)
        sys.exit(3)
    sid = step.get("id", "")
    if step_filter and sid != step_filter:
        continue
    problem = malformed_reason(step, sid, seen_ids)
    if problem:
        named = sid if (isinstance(sid, str) and sid.strip()) else "<no id>"
        print(f"verify-completion: R25 BLOCK: checklist step #{step_index} '{named}' {problem}.",
              file=sys.stderr)
        sys.exit(3)
    seen_ids.add(sid)
    selected_steps.append((step_index, sid))
    if step.get("fail_fast", False):
        fail_fast_sids.append(sid)
    title = step.get("title", sid)
    step_timeout = int(step.get("timeout_seconds", default_timeout))
    (playbook_out / f"{sid}.txt").write_text(step.get("fix_playbook", ""))
    step_commands = step.get("commands") or []
    for cmd_entry in step_commands:
        cmd = cmd_entry.get("command", "")
        wd = cmd_entry.get("working_directory", default_wd)
        expected_exit = cmd_entry.get("expected_exit", 0)
        advisory = str(bool(cmd_entry.get("advisory", False))).lower()
        cmd_timeout = int(cmd_entry.get("timeout_seconds", step_timeout))
        if "\t" in cmd:
            print(f"verify-completion: tab character in command: {cmd}", file=sys.stderr)
            sys.exit(2)
        lines.append(f"{sid}\t{title}\t{cmd}\t{wd}\t{expected_exit}\t{advisory}\t{cmd_timeout}")

pathlib.Path(plan_path).write_text("\n".join(lines) + ("\n" if lines else ""))
# Sidecar: step ids marked `fail_fast: true` — a HARD_FAIL in one of these short-circuits
# the remaining steps (so a structural pre-gate FAIL does not still pay the ~18-min per-domain suite).
pathlib.Path(plan_path + ".failfast").write_text("\n".join(fail_fast_sids) + ("\n" if fail_fast_sids else ""))
# Sidecar: the LOGICAL steps this run selected, as <declaration_index>\t<id>. The plan is a
# flat list of COMMANDS, so a step that emitted no row is simply absent from it — and every
# accounting check derives its step set FROM the plan, which is what let a vanished step ride
# through unverified. This manifest is the independent record of what the contract demanded,
# so the runner can assert plan ⊇ declared instead of taking the plan's word for it. The
# index is carried because a blank id is exactly one of the shapes being detected.
pathlib.Path(plan_path + ".steps").write_text(
    "".join(f"{idx}\t{sid}\n" for idx, sid in selected_steps))
PYEOF

PLAN_EXIT=$?
if [ "$PLAN_EXIT" -eq 3 ]; then
    echo "verify-completion: R25 BLOCK: the checklist is MALFORMED — the offending step is named" \
         "above. A step selected for this run must be identifiable (a unique slug id) and must" \
         "actually run something. A step that emits no plan row disappears from the executed plan" \
         "AND from every accounting check; a step whose command text is blank, comment-only, or a" \
         "canonical no-op placeholder (':', 'true', '/bin/true', 'exit 0' …) exits with the" \
         "expected status without running anything and is recorded as an EXECUTED PASS. Either way" \
         "the run would report green with that step never verified. Fix the step, or delete it." \
         "BOUNDARY: the placeholder check is a finite denylist of those idioms — it cannot prove" \
         "that an arbitrary command does useful work, and does not claim to." >&2
    exit 2
fi
if [ "$PLAN_EXIT" -ne 0 ]; then
    echo "verify-completion: failed to parse checklist" >&2
    exit 2
fi

if [ ! -s "$PLAN_FILE" ]; then
    if [ -n "$STEP_FILTER" ]; then
        echo "verify-completion: no commands matched --step '$STEP_FILTER'" >&2
        exit 2
    fi
    echo "verify-completion: checklist contained no commands" >&2
    exit 2
fi

# ── Preflight (ii)/(iii): heavy toolchains, gated on the RESOLVED step set ────
# PLAN_FILE is already filtered by --step, so a backend-only run never trips the
# node check and vice-versa. Columns: 1=step_id 3=command 4=working_directory.
NEEDS_JDK=0
NEEDS_NODE=0
NEEDS_PYYAML=0
if awk -F'\t' '$3 ~ /gradlew/ || $4 == "backend" { found=1 } END { exit !found }' "$PLAN_FILE"; then
    NEEDS_JDK=1
fi
if awk -F'\t' '$1 == "frontend-lint" || $4 == "frontend" || $3 ~ /npm[ ]/ { found=1 } END { exit !found }' "$PLAN_FILE"; then
    NEEDS_NODE=1
fi
# Catalog guards live under practices/evals/ + practices-react/evals/ and are composed
# freely (run-all-guards.sh alone fans out to ~97 of them). Rather than enumerate which
# individual guard needs PyYAML — a list that rots the moment a guard is added — any
# command invoking a script under an `evals/` directory requires the parser. Deliberately
# conservative in that direction; the step-gating that matters (a backend/gradle-only or
# frontend-only run needs no PyYAML) is preserved. pyyaml_preflight_coverage_guard.sh [95]
# mechanically re-derives the PyYAML-dependent guard set and FAILS if this heuristic
# stops covering it.
if awk -F'\t' '$3 ~ /evals\// { found=1 } END { exit !found }' "$PLAN_FILE"; then
    NEEDS_PYYAML=1
fi

if [ "$NEEDS_JDK" -eq 1 ]; then
    JAVA_BIN=""
    if preflight_faked jdk; then
        JAVA_BIN=""
    elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_BIN="$JAVA_HOME/bin/java"
    elif command -v java >/dev/null 2>&1; then
        JAVA_BIN="java"
    fi
    JMAJOR=""
    if [ -n "$JAVA_BIN" ]; then
        # `java -version` → stderr line like: openjdk version "21.0.11" 2026-04-21
        # Legacy "1.8.0" form → major 8; modern "21.0.x" → major 21.
        JMAJOR="$("$JAVA_BIN" -version 2>&1 | awk -F'"' '/version/ { print $2; exit }' \
            | awk -F. '{ print ($1 == "1") ? $2 : $1 }')"
    fi
    if [ "$JMAJOR" != "21" ]; then
        echo "verify-completion: R25 BLOCK: JDK 21 required for backend/gradle steps" \
             "(build.gradle.kts toolchain = JavaLanguageVersion.of(21)); resolved java=${JAVA_BIN:-none}" \
             "major=${JMAJOR:-unresolved}. Set JAVA_HOME to a JDK 21 install." >&2
        exit 2
    fi
fi

if [ "$NEEDS_NODE" -eq 1 ]; then
    if preflight_faked node || ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
        echo "verify-completion: R25 BLOCK: node + npm required for the frontend-lint step" \
             "(resolved step set includes it). Install Node.js, or run backend-only steps with --step." >&2
        exit 2
    fi
fi

if [ "$NEEDS_PYYAML" -eq 1 ]; then
    if preflight_faked pyyaml || ! "$AX_PY_BIN" -E -c 'import yaml' >/dev/null 2>&1; then
        echo "verify-completion: R25 BLOCK: PyYAML (python3 -c 'import yaml') required for the catalog guard steps" \
             "(resolved step set runs scripts under evals/). yq is NOT a substitute here — the guards embed" \
             "'import yaml' with no yq path and several SKIP silently without it, which would report a PASS" \
             "for guards that never ran. Install with: python3 -m pip install pyyaml" >&2
        exit 2
    fi
fi

# Steps marked `fail_fast: true` short-circuit the run on a HARD_FAIL (e.g. backend-build,
# structural-pregate) — so a compile/structural break does not still pay the ~18-min per-domain suite.
FAIL_FAST_SIDS=$(cat "$PLAN_FILE.failfast" 2>/dev/null || true)

# ── 1b. Resume preload (atomic): map step_id → PASS|FAIL only when HEAD matches.
declare_resume_pass() {
    grep -F "$1" "$RESUME_TMP" >/dev/null 2>&1
}
if [ "$RESUME" -eq 1 ] && [ -f "$RESUME_LOG" ]; then
    "$AX_PY_BIN" -I -S - "$RESUME_LOG" "$CURRENT_HEAD" "$RESUME_TMP" "$CURRENT_TREE_FP" <<'PYEOF'
import sys, pathlib, json
resume_path, head, out_path, tree_fp = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
keep = []
stale = []
for line in pathlib.Path(resume_path).read_text().splitlines():
    line = line.strip()
    if not line:
        continue
    try:
        rec = json.loads(line)
    except Exception:
        continue
    if rec.get("head_sha") != head:
        continue
    if rec.get("status") != "PASS":
        continue
    # NOTE: the "provenance" field is audit metadata, NOT an admission gate. Making the
    # preloader reject non-executed provenance would add a third blocking layer that
    # masks the loss of layer A, hollowing out resume_provenance_guard's non-vacuity
    # matrix ("A+B neutered must still REPRODUCE the defect"). Provenance is enforced
    # where it is produced (emit_step_verdict), not where it is consumed.
    #
    # The TREE FINGERPRINT is different in kind, and IS an admission gate: it is the only
    # thing that ties the record to the code that ran. head_sha is satisfied by any tree at
    # that head, including one whose uncommitted change has since been reverted. A record
    # without the field predates this binding and is likewise refused — fail closed.
    rec_fp = rec.get("tree_fingerprint")
    if rec_fp != tree_fp:
        stale.append((rec.get("step_id") or "?", rec_fp or "(absent)"))
        continue
    sid = rec.get("step_id")
    if sid:
        keep.append(sid)
if stale:
    steps = ", ".join(sorted({s for s, _ in stale}))
    seen = sorted({f for _, f in stale})
    sys.stderr.write(
        "verify-completion: resume record was produced against a DIFFERENT tree — re-verifying.\n"
        f"  head_sha matches ({head}) but the working tree does not:\n"
        f"    record tree : {', '.join(seen)}\n"
        f"    current tree: {tree_fp}\n"
        f"  affected step(s): {steps}\n"
        "  A step verified against other file contents is not evidence for these ones, so the\n"
        "  record is NOT consumed. Those steps run again (this is a re-verify, not a failure).\n")
pathlib.Path(out_path).write_text("\n".join(sorted(set(keep))) + ("\n" if keep else ""))
PYEOF
fi

# ── 2. Print the plan ─────────────────────────────────────────────────────────
echo "=== verify-completion.sh — R25/R28 mechanical completion contract ==="
echo "checklist: practices/verification-checklist.yaml"
echo "head_sha : $CURRENT_HEAD"
[ "$RESUME" -eq 1 ] && echo "resume   : enabled (skip PASS steps with matching head_sha)"
[ "$COLLAPSE" -eq 0 ] && echo "collapse : disabled (per-domain tasks will run separately)"
echo ""
CURRENT_STEP=""
while IFS=$'\t' read -r sid title cmd wd expected advisory timeout_s; do
    if [ "$sid" != "$CURRENT_STEP" ]; then
        CURRENT_STEP="$sid"
        echo "▸ step: $sid — $title  (timeout ${timeout_s}s)"
    fi
    adv_label=""
    [ "$advisory" = "true" ] && adv_label=" [advisory]"
    echo "    \$ ( cd $wd && $cmd )${adv_label} (expect exit $expected)"
done < "$PLAN_FILE"
echo ""

if [ "$DRY_RUN" -eq 1 ]; then
    echo "verify-completion: --dry-run set, exiting without execution"
    exit 0
fi

# ── 3. Atomic resume-log writer ──────────────────────────────────────────────
# .ax-verify/last_run.jsonl is rewritten atomically every step so a Ctrl-C or
# SIGKILL never leaves a half-line. Pattern: write tmp → mv -f.
RESUME_NEW=$(mktemp)
# Seed RESUME_NEW with any PASS lines for OTHER head_shas we'd want to overwrite.
# Simpler: start fresh each run; resume only consults prior file. So no seed.
: > "$RESUME_NEW"

emit_resume() {
    local sid="$1" status="$2" provenance="${3:-none}"
    local ts
    ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    # tree_fingerprint pins the record to the working tree that produced it — head_sha alone
    # is satisfied by any tree at that head (see compute_tree_fingerprint).
    printf '{"step_id":"%s","status":"%s","ts":"%s","head_sha":"%s","provenance":"%s","tree_fingerprint":"%s"}\n' \
        "$sid" "$status" "$ts" "$CURRENT_HEAD" "$provenance" "$CURRENT_TREE_FP" >> "$RESUME_NEW"
    # Atomic publish: copy to a sibling tmp then rename. mv on same filesystem
    # is atomic per POSIX. Crash mid-rename ⇒ caller sees either old or new.
    cp "$RESUME_NEW" "$RESUME_LOG.tmp.$$"
    mv -f "$RESUME_LOG.tmp.$$" "$RESUME_LOG"
}

# ── 3b. Step verdict — PASS requires PROVENANCE, not merely absent failure ───
# A step may be recorded PASS only if at least one of its commands actually
# produced an OBSERVED outcome in RESULTS_FILE. "HARD_FAIL did not increase" is
# not evidence of success: if the plan or the ledger vanishes mid-run (the P0-30
# TMPDIR-wipe shape) the command loop reads nothing, no failure can be counted,
# and the step would be certified PASS — a certification `--resume` then honours,
# skipping the command that never ran. That is resume laundering: a run this
# script correctly BLOCKS can seed a green resume on the next invocation.
# UNRUN is deliberately a distinct, non-PASS status: the resume preloader keeps
# only status == "PASS", so an UNRUN step is always re-executed.
#
# A ROW IS NOT AN EXECUTION (layer D). "the step has a row" was the second half of
# the same unsoundness: a row can exist because the command was skipped (absent
# working directory) rather than run, and such a row used to satisfy both this
# verdict and the accounting check below. So PASS additionally requires that EVERY
# planned non-advisory command of the step actually executed:
#   req_planned  — non-advisory commands the plan scheduled for this step
#   req_executed — of those, how many reached the runner and produced an exit code
# A step with no required commands at all (every command advisory) still needs one
# executed/resumed row; otherwise nothing was verified and PASS would be a fiction.
# Echoes nothing; returns 1 = FAIL, 2 = UNRUN, 0 = PASS.
emit_step_verdict() {
    local sid="$1" hard_before="$2" req_planned="${3:-0}" req_executed="${4:-0}"
    local recorded verified reason=""
    if [ "$HARD_FAIL" -gt "$hard_before" ]; then
        emit_resume "$sid" "FAIL" "executed"
        return 1
    fi
    recorded=$(awk -F'\t' -v s="$sid" '$1==s { c++ } END { print c+0 }' "$RESULTS_FILE" 2>/dev/null || echo 0)
    verified=$(awk -F'\t' -v s="$sid" '$1==s && ($6=="executed" || $6=="resumed") { c++ } END { print c+0 }' \
        "$RESULTS_FILE" 2>/dev/null || echo 0)
    if [ "${recorded:-0}" -eq 0 ]; then
        reason="recorded NO command outcome (plan or ledger lost mid-run)"
    elif [ "$req_planned" -gt 0 ] && [ "$req_executed" -lt "$req_planned" ]; then
        reason="only $req_executed of $req_planned required command(s) actually executed"
    elif [ "${verified:-0}" -eq 0 ]; then
        reason="produced no executed outcome at all (every command was skipped)"
    fi
    if [ -n "$reason" ]; then
        echo "  UNRUN — step [$sid] $reason." >&2
        echo "          Recording UNRUN, not PASS: an unobserved step must never be resume-skipped." >&2
        emit_resume "$sid" "UNRUN"
        return 2
    fi
    emit_resume "$sid" "PASS" "executed"
    return 0
}

# ── 4. Watchdog wrapper: run command with timeout, line-buffered streaming ──
# We background the command (so its stdout/stderr stay attached to OUR fds —
# stream live), then background a watchdog that SIGTERMs on timeout and SIGKILL
# after a 30s grace period. Watchdog is killed atomically once the command
# exits. No `timeout` binary dependency.
run_with_timeout() {
    local exec_wd="$1" exec_cmd="$2" timeout_s="$3"

    # Background the user command. Critical: redirect stdin from /dev/null so
    # it does NOT inherit the parent's stdin (which, when the caller is e.g.
    # `while ... done < PLAN_FILE`, would be a file descriptor that keeps the
    # subshell's read-end open and breaks bash 3.2's wait/SIGCHLD coupling —
    # observed as "gradle exits, BUILD SUCCESSFUL printed, but parent never
    # advances past `wait $cmd_pid`"). We deliberately do NOT use `set -m`:
    # job control under non-interactive shells corrupts wait() semantics here.
    ( cd "$exec_wd" && bash -c "$exec_cmd" ) </dev/null &
    local cmd_pid=$!

    # Watchdog: a background sleeper that signals the child if it overruns,
    # and exits silently if killed by the parent on natural completion.
    (
        trap 'exit 0' TERM
        sleep "$timeout_s"
        if kill -0 "$cmd_pid" 2>/dev/null; then
            echo "" >&2
            echo "  [watchdog] step exceeded ${timeout_s}s — sending SIGTERM to pid $cmd_pid" >&2
            kill -TERM "$cmd_pid" 2>/dev/null
            # 30s graceful shutdown window.
            local i=0
            while [ "$i" -lt 30 ] && kill -0 "$cmd_pid" 2>/dev/null; do
                sleep 1
                i=$((i + 1))
            done
            if kill -0 "$cmd_pid" 2>/dev/null; then
                echo "  [watchdog] grace expired — sending SIGKILL to pid $cmd_pid" >&2
                kill -KILL "$cmd_pid" 2>/dev/null
            fi
        fi
    ) &
    local wd_pid=$!
    # Disown the watchdog from the job table so bash never prints
    # "Killed: 9" on its termination. `disown` is bash-3.2 safe.
    disown "$wd_pid" 2>/dev/null || true

    # Wait for the user command. `wait <pid>` propagates the child's exit code.
    wait "$cmd_pid" 2>/dev/null
    local actual_exit=$?

    # Tear down the watchdog. We DELIBERATELY do not `wait $wd_pid` afterwards:
    # bash 3.2 (macOS default) can hang on `wait` for a backgrounded subshell
    # that we just signalled. Instead, brief polling reap + SIGKILL fallback.
    kill -TERM "$wd_pid" 2>/dev/null
    local i=0
    while [ "$i" -lt 5 ]; do
        kill -0 "$wd_pid" 2>/dev/null || break
        sleep 0.1 2>/dev/null || sleep 1
        i=$((i + 1))
    done
    kill -KILL "$wd_pid" 2>/dev/null

    return "$actual_exit"
}

# ── 5. Inject `--console=plain` into gradle invocations (R27 streaming fix) ──
inject_gradle_plain_console() {
    local in="$1"
    if [[ "$in" == *"./gradlew"* ]] && [[ "$in" != *"--console="* ]]; then
        echo "${in/.\/gradlew/.\/gradlew --console=plain}"
    else
        echo "$in"
    fi
}

# ── 6. Execute the plan ──────────────────────────────────────────────────────
HARD_FAIL=0
ADVISORY_FAIL=0
SKIP_RESUME_COUNT=0
# Initialised HERE, never inherited. `SHORT_CIRCUITED="${SHORT_CIRCUITED:-0}"` at the
# accounting check would have honoured an EXPORTED SHORT_CIRCUITED=1 from the caller's
# environment, which suppresses the "every planned step has an outcome" check — an
# env-var bypass of a fail-closed gate. The value is set only by the fail-fast break below.
SHORT_CIRCUITED=0

# Iterate by step. For each step:
#   1. If --resume and step is in resume cache, SKIP and emit PASS.
#   2. If COLLAPSE && step has multiple gradle ./gradlew commands in the same wd,
#      collapse non-advisory ones into a single `./gradlew taskA taskB --continue`
#      and run advisory commands separately after.
#   3. Else run each command sequentially with watchdog.

# Group plan by step_id while preserving order.
STEP_ORDER=$(awk -F'\t' '!seen[$1]++ { print $1 }' "$PLAN_FILE")

for sid in $STEP_ORDER; do
    # Step header.
    title=$(awk -F'\t' -v s="$sid" '$1==s { print $2; exit }' "$PLAN_FILE")
    echo "── [$sid] $title ──────────────────────────────────────────────"

    # Sample the tree BEFORE the step: this is the code the step is about to verify, and the
    # tree any resume record it publishes will be bound to.
    observe_tree "before step [$sid]"

    # Resume short-circuit.
    if [ "$RESUME" -eq 1 ] && [ -s "$RESUME_TMP" ] && grep -Fxq "$sid" "$RESUME_TMP"; then
        echo "  SKIP (resume): step PASS for head $CURRENT_HEAD already recorded"
        # origin=resumed, NOT executed: this row is evidence only because the record it
        # consumed was published by a run at the SAME head_sha that executed the step.
        echo -e "$sid\tRESUME-SKIP\tPASS\t0\tfalse\tresumed" >> "$RESULTS_FILE"
        SKIP_RESUME_COUNT=$((SKIP_RESUME_COUNT + 1))
        emit_resume "$sid" "PASS" "resumed"
        continue
    fi

    # Try collapse for this step.
    COLLAPSED_PLAN=$(mktemp)
    if [ "$COLLAPSE" -eq 1 ]; then
        "$AX_PY_BIN" -I -S "$COLLAPSE_HELPER" "$PLAN_FILE" "$sid" > "$COLLAPSED_PLAN" 2>/dev/null || true
    fi

    STEP_HARD_FAIL_BEFORE=$HARD_FAIL
    # Required-command coverage for this step (layer D). PLAN_FILE col 6 = advisory.
    STEP_REQ_PLANNED=$(awk -F'\t' -v s="$sid" '$1==s && $6!="true" { c++ } END { print c+0 }' "$PLAN_FILE")
    STEP_REQ_EXECUTED=0

    if [ -s "$COLLAPSED_PLAN" ]; then
        # ── Collapsed path: one warm gradle daemon for all non-advisory tasks ─
        # First line: <wd>\t<collapsed_cmd>\t<advisory_count>
        # Subsequent lines: <wd>\t<advisory_cmd>\tadvisory
        first_line=$(head -n 1 "$COLLAPSED_PLAN")
        coll_wd=$(echo "$first_line" | cut -f1)
        coll_cmd=$(echo "$first_line" | cut -f2)

        if [ "$coll_wd" = "." ] || [ -z "$coll_wd" ]; then
            exec_wd="$REPO_ROOT"
        else
            exec_wd="$REPO_ROOT/$coll_wd"
        fi

        # Use the timeout of the step (max across commands). Re-read first cmd row.
        step_timeout=$(awk -F'\t' -v s="$sid" '$1==s { if($7+0 > max) max=$7+0 } END { print (max?max:900) }' "$PLAN_FILE")

        if [ -n "$coll_cmd" ]; then
            # Append --continue if not already there.
            exec_cmd="$coll_cmd"
            [[ "$exec_cmd" != *"--continue"* ]] && exec_cmd="$exec_cmd --continue"
            exec_cmd=$(inject_gradle_plain_console "$exec_cmd")

            echo "  RUN (collapsed)  \$ ( cd $coll_wd && $exec_cmd )"
            run_with_timeout "$exec_wd" "$exec_cmd" "$step_timeout"
            actual_exit=$?

            if [ "$actual_exit" -eq 0 ]; then
                echo "  PASS (collapsed) — exit 0"
                echo -e "$sid\t$exec_cmd\tPASS\t0\tfalse\texecuted" >> "$RESULTS_FILE"
                # The collapsed invocation IS every non-advisory command of this step
                # (_collapse_plan.py collapses all of them or emits nothing) and runs with
                # --continue, so a clean exit means each one ran. On a non-zero exit the
                # step is a HARD_FAIL anyway and coverage is deliberately left at 0.
                STEP_REQ_EXECUTED=$STEP_REQ_PLANNED
            else
                HARD_FAIL=$((HARD_FAIL + 1))
                echo "  FAIL (collapsed) — exit $actual_exit"
                echo -e "$sid\t$exec_cmd\tFAIL\t$actual_exit\tfalse\texecuted" >> "$RESULTS_FILE"
            fi
        fi

        # Advisory commands run individually so each RED stays scoped.
        tail -n +2 "$COLLAPSED_PLAN" | while IFS=$'\t' read -r adv_wd adv_cmd _adv_tag; do
            [ -z "$adv_cmd" ] && continue
            if [ "$adv_wd" = "." ] || [ -z "$adv_wd" ]; then
                exec_wd_adv="$REPO_ROOT"
            else
                exec_wd_adv="$REPO_ROOT/$adv_wd"
            fi
            exec_cmd_adv=$(inject_gradle_plain_console "$adv_cmd")
            echo "  RUN (advisory)   \$ ( cd $adv_wd && $exec_cmd_adv )"
            run_with_timeout "$exec_wd_adv" "$exec_cmd_adv" "$step_timeout"
            adv_exit=$?
            if [ "$adv_exit" -eq 0 ]; then
                echo "  PASS (advisory)  — exit 0"
                echo -e "$sid\t$exec_cmd_adv\tPASS\t0\ttrue\texecuted" >> "$RESULTS_FILE"
            else
                # advisory FAIL is a WARN; do NOT bump HARD_FAIL. We can't
                # mutate ADVISORY_FAIL from subshell, so log a sentinel line.
                echo "  WARN (advisory)  — exit $adv_exit (advisory, continuing)"
                echo -e "$sid\t$exec_cmd_adv\tWARN\t$adv_exit\ttrue\texecuted" >> "$RESULTS_FILE"
            fi
        done

        # Recompute ADVISORY_FAIL from the results file for THIS step.
        step_adv_fails=$(awk -F'\t' -v s="$sid" '$1==s && $3=="WARN" { c++ } END { print c+0 }' "$RESULTS_FILE")
        # Subtract any previously-counted advisory fails for prior steps to avoid double-count.
        ADVISORY_FAIL=$(awk -F'\t' '$3=="WARN" { c++ } END { print c+0 }' "$RESULTS_FILE")

        # Determine PASS/FAIL/UNRUN of step (PASS requires an observed outcome).
        emit_step_verdict "$sid" "$STEP_HARD_FAIL_BEFORE" "$STEP_REQ_PLANNED" "$STEP_REQ_EXECUTED"
        if [ $? -eq 1 ] && [ -s "$PLAYBOOK_DIR/$sid.txt" ]; then
            echo ""
            echo "  ▼ fix_playbook for step [$sid]:"
            sed 's/^/      /' "$PLAYBOOK_DIR/$sid.txt"
            echo ""
        fi
    else
        # ── Sequential path: run each command with watchdog ──────────────────
        STEP_FAILED_ALREADY=""
        while IFS=$'\t' read -r p_sid _p_title cmd wd expected advisory timeout_s; do
            [ "$p_sid" = "$sid" ] || continue
            if [ "$wd" = "." ] || [ -z "$wd" ]; then
                exec_wd="$REPO_ROOT"
            else
                exec_wd="$REPO_ROOT/$wd"
            fi

            # ── Layer C: an ABSENT working directory is a BLOCK, never a silent skip ──
            # A command whose directory does not exist did not run, so nothing about it was
            # verified. Recording SKIP and carrying on used to leave a row that satisfied
            # both the step verdict and the accounting check — so `mv frontend frontend.off;
            # verify-completion.sh --step frontend-lint` exited 0 with `npm run lint` never
            # invoked, and a later --resume skipped the step on that record. Advisory
            # commands stay advisory (WARN): they are non-blocking by construction.
            if [ ! -d "$exec_wd" ]; then
                dir_missing_blocks=1
                [ "$advisory" = "true" ] && dir_missing_blocks=0
                if [ "$dir_missing_blocks" -eq 1 ]; then
                    HARD_FAIL=$((HARD_FAIL + 1))
                    echo "  FAIL \$ ( cd $wd && $cmd ) — working dir does not exist ($exec_wd);" \
                         "the command never ran, so this step cannot be certified"
                    echo -e "$sid\t$cmd\tFAIL\tdir-missing\t$advisory\tunrun" >> "$RESULTS_FILE"
                    if [ -z "$STEP_FAILED_ALREADY" ]; then
                        STEP_FAILED_ALREADY="1"
                        if [ -s "$PLAYBOOK_DIR/$sid.txt" ]; then
                            echo ""
                            echo "  ▼ fix_playbook for step [$sid]:"
                            sed 's/^/      /' "$PLAYBOOK_DIR/$sid.txt"
                            echo ""
                        fi
                    fi
                else
                    ADVISORY_FAIL=$((ADVISORY_FAIL + 1))
                    echo "  WARN \$ ( cd $wd && $cmd ) — working dir does not exist (ADVISORY, not run)"
                    echo -e "$sid\t$cmd\tWARN\tdir-missing\t$advisory\tunrun" >> "$RESULTS_FILE"
                fi
                continue
            fi

            exec_cmd=$(inject_gradle_plain_console "$cmd")
            echo "  RUN  \$ ( cd $wd && $cmd )  (timeout ${timeout_s}s)"
            run_with_timeout "$exec_wd" "$exec_cmd" "$timeout_s"
            actual_exit=$?
            # The command reached the runner and produced an exit code — that, and only
            # that, is what "executed" means for the layer-D coverage count.
            [ "$advisory" = "true" ] || STEP_REQ_EXECUTED=$((STEP_REQ_EXECUTED + 1))

            if [ "$actual_exit" -eq "$expected" ]; then
                echo "  PASS \$ ( cd $wd && $cmd ) — exit $actual_exit"
                echo -e "$sid\t$cmd\tPASS\t$actual_exit\t$advisory\texecuted" >> "$RESULTS_FILE"
            else
                if [ "$advisory" = "true" ]; then
                    ADVISORY_FAIL=$((ADVISORY_FAIL + 1))
                    echo "  WARN \$ ( cd $wd && $cmd ) — exit $actual_exit (expected $expected, ADVISORY)"
                    echo -e "$sid\t$cmd\tWARN\t$actual_exit\t$advisory\texecuted" >> "$RESULTS_FILE"
                else
                    HARD_FAIL=$((HARD_FAIL + 1))
                    echo "  FAIL \$ ( cd $wd && $cmd ) — exit $actual_exit (expected $expected)"
                    echo -e "$sid\t$cmd\tFAIL\t$actual_exit\t$advisory\texecuted" >> "$RESULTS_FILE"
                    if [ -z "$STEP_FAILED_ALREADY" ]; then
                        STEP_FAILED_ALREADY="1"
                        if [ -s "$PLAYBOOK_DIR/$sid.txt" ]; then
                            echo ""
                            echo "  ▼ fix_playbook for step [$sid]:"
                            sed 's/^/      /' "$PLAYBOOK_DIR/$sid.txt"
                            echo ""
                        fi
                    fi
                fi
            fi
        done < "$PLAN_FILE"

        emit_step_verdict "$sid" "$STEP_HARD_FAIL_BEFORE" "$STEP_REQ_PLANNED" "$STEP_REQ_EXECUTED"
    fi

    rm -f "$COLLAPSED_PLAN"

    # fail-fast: a HARD_FAIL in a `fail_fast: true` step short-circuits the remaining steps.
    if [ "$HARD_FAIL" -gt "$STEP_HARD_FAIL_BEFORE" ] && printf '%s\n' "$FAIL_FAST_SIDS" | grep -qxF "$sid"; then
        echo ""
        echo "  ⛔ fail-fast: step [$sid] FAILED — short-circuiting the remaining steps"
        echo "     (a fail_fast pre-gate failed; the heavy downstream steps are skipped — fix + re-run)."
        SHORT_CIRCUITED=1
        break
    fi
done

# ── Closing endpoint: what was the tree when the last step finished? ─────────
# Paired with the start snapshot this is what the push gate compares. Both endpoints go into
# the audit line so the consumer can check start == end itself rather than trust one flag.
observe_tree "end of run"
END_HEAD="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo "unknown")"
END_TREE_FP="$CURRENT_TREE_FP"
# ROUND 5 / P1-3(c): the closing value is the ACCUMULATED one, not the last reading. A run that
# was ever observed dirty ends dirty, whatever the final sample says — otherwise "make the fix,
# let the step pass, revert it" survives as long as the endpoints happen to look clean, and the
# only thing that noticed the middle was a fingerprint difference produced by the very helper this
# round shows can be replaced.
END_TREE_CLEAN="$CURRENT_TREE_CLEAN"
[ "$TREE_EVER_DIRTY" = true ] && END_TREE_CLEAN=false

# ── Closing endpoint for the ANCHOR (P1-2, ROUND 4, TD-2026-07-30-P1-anchor-runtime) ──
# The tree was sampled across the run because A RUN IS NOT AN INSTANT. The anchor REF has
# exactly the same property and none of the protection: refs/remotes/origin/main is an ordinary
# local ref, so it can be aimed at an ancient root commit for the minutes the ratcheting guards
# are running (they then find their own files absent and bootstrap-skip) and restored before the
# audit line is written. The recorded anchor_sha would still be the honest one and the pre-push
# binding against the remote's advertisement would still match perfectly.
# So the ref is re-read HERE, at the closing endpoint, exactly as the tree is, and both readings
# go into the audit line: anchor_sha (start) / anchor_sha_end (end) / anchor_stable. A drift is
# a HARD FAIL — the run is written to the audit log AS A FAILURE rather than suppressed, because
# fail_fast_blocking_audit_guard requires every blocking path to leave a trail, and because a
# silent re-resolution would be precisely the laundering this check exists to stop.
# HONEST LIMIT, inherited from the same shape as tree sampling: this is two observations, not
# continuous custody. A ref moved and restored entirely between the two reads is unobserved —
# though the guards' own pin checks add further observation points inside the window.
# ROUND 5: ANCHOR_STABLE is NOT re-initialised here — it is the accumulator observe_tree has been
# maintaining at every step boundary, so an anchor that moved in the middle and was put back
# before the end can no longer be erased by this closing read agreeing with the start.
ANCHOR_SHA_END="$ANCHOR_SHA"
if [ -n "$ANCHOR_REF_AT_START" ] && [ "$ANCHOR_SHA" != "unavailable" ]; then
    ANCHOR_SHA_END="$("$AX_GIT_BIN" --no-replace-objects -C "$REPO_ROOT" rev-parse --verify --quiet "${ANCHOR_REF_AT_START}^{commit}" 2>/dev/null)"
    [ -n "$ANCHOR_SHA_END" ] || ANCHOR_SHA_END="unresolvable"
    if [ "$ANCHOR_SHA_END" != "$ANCHOR_SHA" ] || [ "$ANCHOR_STABLE" != true ]; then
        ANCHOR_STABLE=false
        HARD_FAIL=$((HARD_FAIL + 1))
        {
            echo ""
            echo "  ⛔ ANCHOR_REF_MOVED_MIDRUN — ${ANCHOR_REF_AT_START} changed during this run."
            echo "     at start: ${ANCHOR_SHA}"
            echo "     at end  : ${ANCHOR_SHA_END}"
            [ -n "$ANCHOR_MOVED_SEEN" ] && \
            echo "     first seen elsewhere at: ${ANCHOR_MOVED_SEEN} (an intermediate sample; a"
            [ -n "$ANCHOR_MOVED_SEEN" ] && \
            echo "     matching pair of endpoints does not undo it)"
            echo "     The ratcheting guards measure 'the previous release' through that ref. A"
            echo "     reference point that moves while the run is in progress was not a reference"
            echo "     point: aiming it at a commit that merely LACKS the ratcheting files makes"
            echo "     every ratchet take its first-release bootstrap skip, and restoring it before"
            echo "     the run ends leaves the audit line looking honest. This run certifies"
            echo "     nothing. Re-run R25 on a settled repository."
        } >&2
    fi
fi

# Fault injection for the check below (same spirit as AX_PREFLIGHT_FAKE_MISSING):
#   AX_FAKE_LEDGER_LOSS=missing  — the ledger disappears entirely mid-run
#   AX_FAKE_LEDGER_LOSS=partial  — the ledger survives but an earlier step's records are gone
# Both reproduce the 2026-07-29 TMPDIR-wipe shape without needing a destructive guard.
case "${AX_FAKE_LEDGER_LOSS:-}" in
    missing) rm -f "$RESULTS_FILE" ;;
    partial) _ax_first="$(awk -F'\t' 'NR==1 { print $1 }' "$RESULTS_FILE" 2>/dev/null)"
             [ -n "$_ax_first" ] && awk -F'\t' -v s="$_ax_first" '$1!=s' "$RESULTS_FILE" > "$RESULTS_FILE.tmp" \
                 && mv "$RESULTS_FILE.tmp" "$RESULTS_FILE" ;;
esac

LEDGER_BROKEN=0
# ── 6b. Accounting integrity — FAIL CLOSED ──────────────────────────────────
# A run may claim green ONLY if its own result ledger is intact. On 2026-07-29 a
# guard recursively deleted TMPDIR mid-run (it rmtree'd the PARENT of its shim dir),
# destroying RESULTS_FILE. Every step after that point recorded nothing, so the
# summary counted 0 failures and printed "PASS — all steps green" while three whole
# steps went unaccounted. A failure in those steps would have been invisible.
# Silence is not success: an unreadable or incomplete ledger BLOCKS (exit 2), it does
# not degrade into a PASS.
if [ ! -r "$RESULTS_FILE" ]; then
    echo "verify-completion: R25 BLOCK: result ledger is missing or unreadable ($RESULTS_FILE)." \
         "Step outcomes were not recorded, so a PASS cannot be claimed. This usually means" \
         "something deleted the temp file mid-run." >&2
    LEDGER_BROKEN=1
fi
# A row is not an execution: only `executed` (a command ran) and `resumed` (inherited from
# a PASS at this same head_sha) rows count as an outcome here. An `unrun` row — written when
# a command's working directory was absent — is an accounting placeholder, and letting it
# satisfy "every planned step has an outcome" is what allowed a step that never executed to
# ride through as a synthetic PASS. Steps whose commands are ALL advisory are exempt: they
# cannot block by construction, so demanding an execution from them would over-block.
_ax_unrecorded=""
while IFS= read -r _ax_sid; do
    [ -n "$_ax_sid" ] || continue
    # `$3=="FAIL"` also counts as an outcome: the run is already exiting 1 with an honest
    # per-command message, and re-reporting it here as a corrupt ledger (exit 2) would
    # misdiagnose it. Every FAIL row bumps HARD_FAIL, so this can never mask a green claim.
    awk -F'\t' -v s="$_ax_sid" \
        '$1==s && ($6=="executed" || $6=="resumed" || $3=="FAIL") { found=1 } END { exit !found }' \
        "$RESULTS_FILE" && continue
    # All-advisory step with nothing executed → WARN, not BLOCK (already counted advisory).
    # FAIL CLOSED: the exemption is claimable only from a READABLE plan. If the plan itself
    # vanished mid-run (the P0-30 TMPDIR-wipe shape) we cannot tell whether the step had
    # required commands, and "cannot tell" must never buy an exemption from the gate.
    _ax_req=1
    if [ -r "$PLAN_FILE" ]; then
        _ax_req=$(awk -F'\t' -v s="$_ax_sid" '$1==s && $6!="true" { c++ } END { print c+0 }' "$PLAN_FILE")
    fi
    if [ "${_ax_req:-1}" -eq 0 ]; then
        echo "verify-completion: WARN: step [$_ax_sid] is all-advisory and none of its commands ran." >&2
        continue
    fi
    _ax_unrecorded="$_ax_unrecorded $_ax_sid"
done <<EOF_STEPS
$STEP_ORDER
EOF_STEPS
# A fail_fast short-circuit legitimately leaves the remaining steps unrun and therefore
# unrecorded — that is the designed behaviour, not a corrupted ledger. Only a run that
# went the distance may be held to "every planned step has an outcome".
if [ -n "$_ax_unrecorded" ] && [ "$SHORT_CIRCUITED" -eq 0 ]; then
    echo "verify-completion: R25 BLOCK: no EXECUTED outcome recorded for step(s):$_ax_unrecorded" \
         "— either the result ledger is incomplete, or the step's commands never ran (a row" \
         "that exists because a command was skipped is not evidence). A PASS cannot be claimed." >&2
    LEDGER_BROKEN=1
fi

# ── 6c. Structural step accounting — DECLARED ⊆ EMITTED ─────────────────────
# The check above answers "did every step IN THE PLAN produce an outcome" — it derives its
# step set from the plan, so a step that never reached the plan is not merely unchecked, it
# is unseen. Two shapes exploit exactly that:
#   • `commands: []` — the emitter produces no row, the step is absent from STEP_ORDER, and
#     the run publishes green with a required step never verified.
#   • a blank/whitespace id — rows exist, but `for sid in $STEP_ORDER` word-splits them away,
#     so the step gets ZERO shell iterations and no command of it ever runs.
# Both are BLOCKed at parse time; this is the structural backstop that does not depend on the
# parser's own opinion: the declared-step manifest is compared against the plan the runner
# actually executed. An unreadable manifest is itself fail-closed — we cannot tell what the
# contract demanded, and "cannot tell" never buys an exemption.
_ax_declared_broken=""
if [ ! -r "$DECLARED_STEPS" ]; then
    _ax_declared_broken=" (the declared-step manifest was lost mid-run)"
else
    while IFS=$'\t' read -r _ax_idx _ax_did; do
        [ -n "$_ax_idx" ] || continue
        if [ -z "$_ax_did" ]; then
            _ax_declared_broken="$_ax_declared_broken step#$_ax_idx(no-usable-id)"
            continue
        fi
        awk -F'\t' -v s="$_ax_did" '$1==s { found=1 } END { exit !found }' "$PLAN_FILE" 2>/dev/null \
            && continue
        _ax_declared_broken="$_ax_declared_broken $_ax_did(emitted-no-command)"
    done < "$DECLARED_STEPS"
fi
if [ -n "$_ax_declared_broken" ] && [ "$SHORT_CIRCUITED" -eq 0 ]; then
    echo "verify-completion: R25 BLOCK: checklist step(s) selected for this run never entered the" \
         "executed plan:$_ax_declared_broken — a declared step that emits no command, or that has" \
         "no usable id, is invisible to every accounting check, so a green verdict would certify" \
         "work that was never even scheduled." >&2
    LEDGER_BROKEN=1
fi

# ── 7. Summary ──────────────────────────────────────────────────────────────
echo ""
echo "=== verify-completion.sh — Summary ==="
PASS_COUNT=$(grep -c $'\tPASS\t' "$RESULTS_FILE" 2>/dev/null || true)
SKIP_COUNT=$(grep -c $'\tSKIP\t' "$RESULTS_FILE" 2>/dev/null || true)
PASS_COUNT=${PASS_COUNT:-0}
SKIP_COUNT=${SKIP_COUNT:-0}
echo "  PASS         : $PASS_COUNT"
echo "  WARN(advisory): $ADVISORY_FAIL"
echo "  FAIL         : $HARD_FAIL"
echo "  SKIP         : $SKIP_COUNT"
[ "$SKIP_RESUME_COUNT" -gt 0 ] && echo "  RESUME-SKIP  : $SKIP_RESUME_COUNT"

# ── 8. Audit log line (consumed by 49th hard guard) ─────────────────────────
TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
EXIT_CODE=0
[ "$HARD_FAIL" -gt 0 ] && EXIT_CODE=1
# A broken result ledger outranks the counters: they were derived from the very file
# that is missing/incomplete, so they cannot be trusted. Exit 2 (BLOCK) — but only
# AFTER the audit line is written, because fail_fast_blocking_audit_guard enforces
# that no blocking path may skip the audit trail.
[ "${LEDGER_BROKEN:-0}" -ne 0 ] && EXIT_CODE=2

# full_run distinguishes a whole-checklist run from a --step partial run. The
# recency guard accepts ONLY full_run=true — otherwise a single trivial step
# (e.g. --step backend-build) would write an exit=0/hard_fail=0 line that is
# byte-indistinguishable from a full PASS (confirmed dogfood P2, 2026-07-10).
# --resume stays full_run=true: it re-verifies the remaining steps of the SAME
# HEAD whose earlier steps already passed, so the full contract holds at HEAD.
FULL_RUN=true
[ -n "$STEP_FILTER" ] && FULL_RUN=false

# Atomic append: write to tmp + sed concat is overkill — line-buffered >> is
# atomic for writes under PIPE_BUF (4096 on linux/macOS) and our line is < 200
# bytes. Append is safe.
# tree_fingerprint / tree_clean make the line say WHICH TREE was verified, not just at which
# head. Without them the audit line is satisfied by any tree at that head — including one whose
# uncommitted fix has since been stashed — and the push gate cannot tell the difference.
# The *_end fields and tree_stable/tree_samples say WHEN it was that tree: the head/fingerprint/
# cleanliness measured after the last step, plus whether every sample taken across the run
# agreed with the start. A consumer that only reads the start values is trusting a measurement
# taken before any step ran (a real full run here is ~37 minutes wide).
# anchor_sha / anchor_kind say WHICH RELEASE the ratcheting guards in this run measured against
# (P1-X layer 3, ROUND 3). The tree fields above pin the CODE; these pin the REFERENCE POINT,
# which lives in a mutable local ref and is otherwise invisible to every consumer. The pre-push
# hook compares anchor_sha against the sha the REMOTE advertises for the ref being pushed, so a
# forged refs/remotes/origin/main is unpushable regardless of what the guards concluded.
# anchor_sha_end / anchor_stable are the anchor's CLOSING endpoint (P1-2, ROUND 4). The tree got
# two endpoints because a run is not an instant; the ref needs them for the same reason and had
# none. A consumer can now verify the relation (start == end) instead of trusting a single read
# taken before the guards ran.
# ── THIS printf IS THE AUDIT SCHEMA (P1-4, ROUND 4) ──────────────────────────────────
# completion_checklist_recency_guard pins the exact key set a genuine line carries and re-derives
# it FROM THIS FORMAT STRING (AUDIT_WRITER_SCHEMA_DRIFT if the two disagree), so a hand-authored
# line whose field set differs — one key too many, one too few, a duplicated key — is refused
# rather than parsed leniently. Adding or removing a field here therefore REQUIRES updating the
# guard's pinned key list in the same commit; that coupling is the point.
printf '{"ts":"%s","head_sha":"%s","exit":%d,"pass":%d,"warn_advisory":%d,"hard_fail":%d,"skip":%d,"full_run":%s,"tree_fingerprint":"%s","tree_clean":%s,"head_sha_end":"%s","tree_fingerprint_end":"%s","tree_clean_end":%s,"tree_stable":%s,"tree_samples":%d,"anchor_sha":"%s","anchor_kind":"%s","anchor_sha_end":"%s","anchor_stable":%s}\n' \
    "$TS" "$CURRENT_HEAD" "$EXIT_CODE" "$PASS_COUNT" "$ADVISORY_FAIL" "$HARD_FAIL" "$SKIP_COUNT" "$FULL_RUN" \
    "$START_TREE_FP" "$START_TREE_CLEAN" \
    "$END_HEAD" "$END_TREE_FP" "$END_TREE_CLEAN" "$TREE_STABLE" "$TREE_SAMPLES" \
    "$ANCHOR_SHA" "$ANCHOR_KIND" "$ANCHOR_SHA_END" "$ANCHOR_STABLE" \
    >> "$AUDIT_LOG"

# ── ax-ledger capture — every verify run leaves a per-project usage trace (progress / violation),
# so a fork-receiver's gate history is reviewable (복기) and improvable. Never fails the gate. ──
_AX_LEDGER="$(dirname "${BASH_SOURCE[0]:-$0}")/ax-ledger-log.sh"
if [ -f "$_AX_LEDGER" ]; then
    if [ "$HARD_FAIL" -gt 0 ]; then
        bash "$_AX_LEDGER" violation gate=verify-completion outcome=fail "pass=$PASS_COUNT" "fail=$HARD_FAIL" \
            severity=block detail="R25 gate FAILED at HEAD" >/dev/null 2>&1 || true
    else
        bash "$_AX_LEDGER" gate_run gate=verify-completion outcome=pass "pass=$PASS_COUNT" "fail=0" \
            severity=info detail="R25 gate PASSED" >/dev/null 2>&1 || true
    fi
fi

# Finalize resume log atomically — but ONLY for a run whose accounting is intact.
# A run that ends in a ledger BLOCK must leave NO usable resume record. Its step
# outcomes are UNKNOWN by definition (the summary counters were derived from the very
# file that is missing/incomplete), so certifying ANY of its steps would let the next
# `--resume` skip work this run never observed — laundering a blocked run green.
# emit_resume publishes incrementally, so suppressing this final mv is NOT enough:
# the partial record is already on disk and must be actively discarded.
# Discarding (rather than restoring a pre-run record) is the conservative choice: the
# incremental publishes already overwrote it, and a re-run from scratch is always sound.
if [ "${LEDGER_BROKEN:-0}" -ne 0 ]; then
    rm -f "$RESUME_LOG" "$RESUME_LOG.tmp.$$"
    echo "  resume record DISCARDED — this run's outcomes are unknown, so no step may be" >&2
    echo "  resume-skipped later. The next run re-verifies the full checklist." >&2
else
    mv -f "$RESUME_NEW" "$RESUME_LOG"
fi

if [ "$JSON_OUTPUT" -eq 1 ]; then
    tail -1 "$AUDIT_LOG"
fi

# Ledger integrity outranks the counters below: HARD_FAIL/PASS_COUNT are derived by
# grepping the very file that is missing or incomplete, so a 0 there means "unknown",
# not "clean". Placed AFTER the audit write above so the blocking path still leaves a
# trail (fail_fast_blocking_audit_guard enforces that no blocking path skips the audit).
if [ "${LEDGER_BROKEN:-0}" -ne 0 ]; then
    echo ""
    echo "verify-completion: BLOCKED — the result ledger was missing or incomplete;"
    echo "step outcomes are UNKNOWN, so this run cannot claim green."
    echo "Iron Law: task is NOT done. Re-run R25. If it recurs, something is deleting"
    echo "the temp ledger mid-run (see P0-30)."
    exit 2
fi

if [ "$HARD_FAIL" -gt 0 ]; then
    echo ""
    echo "verify-completion: FAIL — $HARD_FAIL non-advisory step(s) failed"
    echo "Iron Law: task is NOT done. Apply the fix_playbook above and re-run."
    exit 1
fi

if [ "$ADVISORY_FAIL" -gt 0 ]; then
    echo ""
    echo "verify-completion: PASS with $ADVISORY_FAIL advisory warning(s)"
    echo "Advisory items are knowingly advisory in the catalog (see fix_playbook)."
    exit 0
fi

echo ""
echo "verify-completion: PASS — all steps green. Task may declare done."
exit 0
