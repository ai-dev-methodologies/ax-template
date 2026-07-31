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

# ── P1-1 (ROUND 4, TD-2026-07-30-P1-anchor-runtime) ──────────────────────────────────
# Every git read in this file must see the real object graph. `git replace` keeps shas identical
# while swapping the objects rev-parse/merge-base/diff return, and the decisions below (which sha
# is the push base, does this diff touch the regression scope) are exactly those reads.
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
_AX_PF_LABEL="pre-push-lib.sh"
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
_AX_HRM_LABEL="pre-push-lib.sh"; _AX_HRM_EXIT=1; _AX_HRM_NEED_PY=0
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
# sys.path, -S skips site entirely, which is what kills a sitecustomize.py payload. NOT usable for
# the three call sites that need PyYAML out of site-packages (the checklist parser and the two
# ratcheting guards' bodies) — those use -E, which still refuses PYTHONPATH/PYTHONHOME/
# PYTHONSTARTUP, and are preceded by an `import yaml` capability probe that BLOCKS (never skips).
# See DECISIONS.md TD-2026-07-30-P1-preflight-and-raw-bytes for the honest limit that leaves.
# PUBLISHED, NOT CONSUMED IN-TREE: every in-tree call site spells the flags LITERALLY so that
# `grep -n ' -I -S '` finds all of them; this export exists for fork-receiver call sites.
export AX_PY_ISO="-I -S"
unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH

# ── P1-3(a) LOAD-TIME INJECTION CHECK (ROUND 4) ──────────────────────────────────────
# THE EARLY RETURN IS GONE. It used to sit here, BEFORE any definition, so
# `export -f pp_anchor_expect_sha` + `_AX_PRE_PUSH_LIB=1` left an attacker's version of the
# ANCHOR BINDING authoritative inside the push gate — the one function that decides whether the
# hook demands the remote's advertised sha at all. Definitions are now re-established on every
# source; the marker guards only the injection REPORT, never the definitions.
# Same two signatures as practices/scripts/lib/release_anchor.sh, deliberately duplicated rather
# than shared: a hook must not depend on a library to decide whether its library was replaced.
_AX_PP_FN_NAMES="pp_anchor_expect_sha pp_effective_push_specs pp_ref_is_deletion \
pp_resolve_ref_base pp_diff_touches_scope pp_git"

_ax_pp_injection_report() {
    {
        echo ".githooks/pre-push-lib.sh: HELPER_FUNCTION_INJECTED — $1"
        echo "  These functions decide the push gate's branches: which refs ship work, which sha"
        echo "  the regression diffs against, and whether the R25 recency guard is handed the"
        echo "  REMOTE's advertised anchor sha. A definition arriving from outside (bash imports"
        echo "  exported functions across \`bash hook\`) replaces the gate's policy with the"
        echo "  caller's. Names reserved by this file: ${_AX_PP_FN_NAMES}"
    } >&2
    exit 1
}

if [[ -n "${_AX_PRE_PUSH_LIB:-}" ]]; then
    if declare -p _AX_PRE_PUSH_LIB 2>/dev/null | grep -q '^declare -x'; then
        _ax_pp_injection_report \
            "_AX_PRE_PUSH_LIB arrived from the ENVIRONMENT (exported); this file sets it as a
  plain shell variable, so an exported one is a forged 'already loaded' marker."
    fi
else
    for _ax_pp_fn in $_AX_PP_FN_NAMES; do
        if declare -F "$_ax_pp_fn" >/dev/null 2>&1; then
            _ax_pp_injection_report \
                "the function ${_ax_pp_fn} was ALREADY DEFINED before this file was sourced, and
  no prior source in this shell set the load marker."
        fi
    done
    unset _ax_pp_fn
fi
for _ax_pp_bf in ${!BASH_FUNC_@}; do
    case "$_ax_pp_bf" in
        BASH_FUNC_pp_*) _ax_pp_injection_report \
            "the environment carries ${_ax_pp_bf} — an exported shell function aimed at this
  file's namespace." ;;
    esac
done
unset _ax_pp_bf
for _ax_pp_fn in $_AX_PP_FN_NAMES; do unset -f "$_ax_pp_fn" 2>/dev/null || true; done
unset _ax_pp_fn
# Plain (NOT exported) marker: idempotent for the REPORT only, never for the definitions.
_AX_PRE_PUSH_LIB=1

# pp_git — every git read in this file goes through the ABSOLUTE, validated binary and, when the
# hook has bound a root, through that root's EXPLICIT git context (ROUND 5 / P1-1+P1-2: the bare
# word `git` resolves through PATH and through an inherited exported FUNCTION, and discovery is
# what GIT_DIR/GIT_WORK_TREE hijack).
pp_git() {
    if [[ -n "${AX_GIT_BOUND_ROOT:-}" && -n "${AX_GIT_BOUND_DIR:-}" ]]; then
        "${AX_GIT_BIN:-git}" -C "$AX_GIT_BOUND_ROOT" --git-dir="$AX_GIT_BOUND_DIR" \
            --work-tree="$AX_GIT_BOUND_ROOT" --no-replace-objects "$@"
    else
        "${AX_GIT_BIN:-git}" --no-replace-objects "$@"
    fi
}

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
    head_sha="$(pp_git rev-parse HEAD)"
    if up_sha="$(pp_git rev-parse '@{u}' 2>/dev/null)"; then
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
        base="$(pp_git merge-base "$local_sha" origin/HEAD 2>/dev/null \
             || pp_git merge-base "$local_sha" origin/main 2>/dev/null || true)"
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
    if ! diff_files="$(pp_git diff --name-only "$base" "$local_sha" 2>&1)"; then
        printf '%s' "$diff_files"
        return 2
    fi
    grep -qE "$PP_REGRESSION_SCOPE" <<< "$diff_files"
}
