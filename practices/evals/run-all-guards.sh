#!/usr/bin/env bash
# practices/evals/run-all-guards.sh — SP37 acceptance gate.
#
# Runs all guards (6 core + recipe_governance) against both the live repo
# and, when --include-fixtures is passed, against every fixture directory.
#
# Exit 0 if all expected exits match.
# Exit 1 with summary if any mismatch.
#
# NOTE (R25): completion_checklist_recency_guard.sh (49th hard guard) is
# intentionally NOT invoked from this script. It audits the audit log that
# verify-completion.sh writes — including it here would create a self-
# referential cycle (verify-completion → this script → 49th guard → log
# that does not yet exist for the current run). The 49th guard runs from
# `.githooks/pre-push` and can be invoked standalone. See HOOKS-GUIDE.md.
#
# Usage:
#   bash practices/evals/run-all-guards.sh
#   bash practices/evals/run-all-guards.sh --include-fixtures
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
           1) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"run-all-guards: HERMETIC_PRIVILEGED_UNREACHABLE — a re-exec into bash privileged mode was already attempted and this shell is STILL unprivileged. Either exec is shadowed by a function, or AX_PRIV_REEXEC was preset in the environment to skip the re-exec. Both are refused; nothing in this gate runs unprivileged. Start it from a clean shell."} ;;
           *) case "${BASH:-}" in
                  /*) exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@" ;;
                  *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"run-all-guards: HERMETIC_PRIVILEGED_UNREACHABLE — the running interpreter (BASH) is not an absolute path, so the SAME interpreter cannot be named unambiguously for the privileged re-exec."} ;;
              esac ;;
       esac ;;
esac
case $- in
    *p*) ;;
    *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"run-all-guards: HERMETIC_PRIVILEGED_UNREACHABLE — the re-exec returned instead of replacing this process, which means exec itself is shadowed. Unprivileged execution is refused."} ;;
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
_AX_PF_LABEL="run-all-guards"
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
_AX_HRM_LABEL="run-all-guards"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=1
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
REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd)"

INCLUDE_FIXTURES=0
while [ $# -gt 0 ]; do
    case "$1" in
        --include-fixtures) INCLUDE_FIXTURES=1; shift ;;
        *) echo "run-all-guards: unknown arg: $1" >&2; exit 2 ;;
    esac
done

PASS=0
FAIL=0
RESULTS=()

run_guard() {
    local label="$1"
    local expected_exit="$2"
    shift 2
    local cmd=("$@")

    local output
    local actual_exit
    output=$("${cmd[@]}" 2>&1) && actual_exit=0 || actual_exit=$?

    if [ "$actual_exit" -eq "$expected_exit" ]; then
        PASS=$((PASS + 1))
        RESULTS+=("PASS [$label]")
    else
        FAIL=$((FAIL + 1))
        RESULTS+=("FAIL [$label] expected exit $expected_exit, got $actual_exit")
        RESULTS+=("     output: $(echo "$output" | head -3)")
    fi
}

echo "=== run-all-guards.sh — SP3 acceptance gate ==="
echo ""

# ── 1. evidence_guard (practices + practices-react) ─────────────────────────
echo "[1] evidence_guard.sh"
run_guard "evidence_guard/practices" 0 \
    bash "$SCRIPT_DIR/evidence_guard.sh" --catalog practices
run_guard "evidence_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/evidence_guard.sh" --catalog practices-react
# P2-20: internal_design + source_type:external evidence must declare
# `anchors: generic_principle_only`. Fixture pair proves the new branch
# non-vacuously (missing → BLOCK, present → PASS).
run_guard "evidence_guard/fixture_missing_caveat" 1 \
    bash "$SCRIPT_DIR/evidence_guard.sh" "$SCRIPT_DIR/fixtures/evidence-caveat/fail_missing_caveat"
run_guard "evidence_guard/fixture_caveat_present" 0 \
    bash "$SCRIPT_DIR/evidence_guard.sh" "$SCRIPT_DIR/fixtures/evidence-caveat/pass_caveat_present"
# BACKLOG P2-43 — the §4.10 templates/ walk announced a scan it never performed: it globbed
# templates/**, COUNTED the files and printed "evidence check passed", reading none of them.
# An upstream_id no manifest registers, an external entry with no url, a template_id file
# with the evidence block deleted, and a frontmatter that is not YAML at all (which ALSO
# makes the file invisible to evidence_quote_spotcheck, whose parser skips unparseable
# frontmatter) all passed. The walk now verifies evidence STRUCTURE across the three shapes
# in this tree — leading frontmatter, @ax-template-meta javadoc, DECISIONS.md ADR blocks —
# and the live runs above cover it (558 files walked, 998 entries verified). Promoting the
# walk found 3 live defects: notification-bell / notification-list / virtualized-table each
# carried an unquoted @scoped dependency, so their whole frontmatter — 6 evidence entries —
# was silently unparseable and skipped by every consumer. Fixtures pin one defect class each
# (--templates-root isolates the walk so a fixture root does not have to carry a rules
# catalog); pass_clean is the positive control that all three shapes still PASS honestly.
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "evidence_guard/fixture_templates_pass_clean" 0 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/pass_clean"
    run_guard "evidence_guard/fixture_templates_unresolved_upstream_id" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_unresolved_upstream_id"
    run_guard "evidence_guard/fixture_templates_frontmatter_unparseable" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_frontmatter_unparseable"
    run_guard "evidence_guard/fixture_templates_missing_evidence" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_missing_evidence"
    run_guard "evidence_guard/fixture_templates_java_external_no_url" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_java_external_no_url"
    run_guard "evidence_guard/fixture_templates_adr_unknown_shape" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_adr_unknown_shape"
    # Non-redundancy of JAVA_NO_EVIDENCE_EXEMPT: an exemption for a file that DOES carry
    # evidence must FAIL, so the list cannot rot into a place to hide a file.
    run_guard "evidence_guard/fixture_templates_java_missing_evidence" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_java_missing_evidence"
    run_guard "evidence_guard/fixture_templates_stale_exemption" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_stale_exemption"
    # P3-90 (Lane E) — the anchors_rule axis: a template whose `anchors_rule:` names a rule
    # that is not on disk. templates/backend/_check-anchors.sh reported 42 such stale
    # pointers while being wired into no gate at all; this pair makes the axis blocking.
    # The two trees differ by exactly one line (the rule the pointer names), so the fail
    # fixture cannot pass for an unrelated reason.
    run_guard "evidence_guard/fixture_templates_anchors_pass" 0 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/pass_anchors_clean"
    run_guard "evidence_guard/fixture_templates_stale_anchors_rule" 1 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/template-evidence/fail_stale_anchors_rule"
    # Fail-closed: --templates-only against a root with no templates/ verified nothing and
    # must not share an exit code with a pass.
    run_guard "evidence_guard/fixture_templates_absent_tree" 2 \
        bash "$SCRIPT_DIR/evidence_guard.sh" --templates-root "$SCRIPT_DIR/fixtures/evidence-caveat"
fi

# ── 2. spec_ref_guard (practices + practices-react) ──────────────────────────
echo "[2] spec_ref_guard.sh"
run_guard "spec_ref_guard/practices" 0 \
    bash "$SCRIPT_DIR/spec_ref_guard.sh" --catalog practices
run_guard "spec_ref_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/spec_ref_guard.sh" --catalog practices-react

# ── 3. substance_guard (practices + practices-react) ─────────────────────────
echo "[3] substance_guard.sh"
run_guard "substance_guard/practices" 0 \
    bash "$SCRIPT_DIR/substance_guard.sh"
# BACKLOG P2-37 — dialect=react-frozen-v1 (see substance_guard.sh header). Live +
# one negative fixture per FROZEN clause + one pass fixture (non-vacuity: each
# fixture isolates exactly one clause so neutering that clause's check flips its
# own fixture 1->0 without touching the other three).
run_guard "substance_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "substance_guard/fixture_fail_react_no_impact" 1 \
        bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react "$SCRIPT_DIR/fixtures/react-substance/fail_react_no_impact"
    run_guard "substance_guard/fixture_fail_react_no_verification" 1 \
        bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react "$SCRIPT_DIR/fixtures/react-substance/fail_react_no_verification"
    run_guard "substance_guard/fixture_fail_react_placeholder_fence" 1 \
        bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react "$SCRIPT_DIR/fixtures/react-substance/fail_react_placeholder_fence"
    run_guard "substance_guard/fixture_fail_react_no_url" 1 \
        bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react "$SCRIPT_DIR/fixtures/react-substance/fail_react_no_url"
    run_guard "substance_guard/fixture_pass_react_clean" 0 \
        bash "$SCRIPT_DIR/substance_guard.sh" --catalog practices-react "$SCRIPT_DIR/fixtures/react-substance/pass_react_clean"
    # BACKLOG P3-73 — the templates/ walk (ZERO_SCAN subgate) had NO fixture, and it was
    # silently no-op under a RELATIVE invocation: the templates root was re-derived from
    # ${BASH_SOURCE[0]} after the Java dialect's `cd`, so `bash practices/evals/substance_guard.sh`
    # (how .githooks/pre-commit calls it) resolved it to "/templates", skipped the walk and
    # still exited 0. These three scenarios pin it. The templates root is intentionally NOT
    # CLI-parameterizable (it must track the guard's own location so the live catalog cannot
    # be swapped from the command line), so the scenarios build a throwaway repo skeleton and
    # copy the LIVE guard into it — see the harness header.
    run_guard "substance_guard/fixture_fail_templates_zero_scan" 1 \
        bash "$SCRIPT_DIR/fixtures/substance-templates-walk/substance_templates_walk_run.sh" fail_zero_scan
    run_guard "substance_guard/fixture_pass_templates_present" 0 \
        bash "$SCRIPT_DIR/fixtures/substance-templates-walk/substance_templates_walk_run.sh" pass_templates_present
    run_guard "substance_guard/fixture_pass_relative_invocation" 0 \
        bash "$SCRIPT_DIR/fixtures/substance-templates-walk/substance_templates_walk_run.sh" pass_relative_invocation
fi

# ── 4. time_decay_guard (practices + practices-react) ────────────────────────
echo "[4] time_decay_guard.sh"
run_guard "time_decay_guard/practices" 0 \
    bash "$SCRIPT_DIR/time_decay_guard.sh" --catalog practices
run_guard "time_decay_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/time_decay_guard.sh" --catalog practices-react

# ── 5. trio_integrity_guard (live repo) ──────────────────────────────────────
echo "[5] trio_integrity_guard.sh (live repo)"
# P2-21: the Spec Trio is ax-template's self-declared primary defense against AI
# hallucination — it MUST be gated on the LIVE repo, not fixtures only. The
# SP3-era stub that accepted "any exit code / fixtures only" left the live specs+
# templates un-gated for the entire maturity window (the guard now passes live:
# 88 files scanned across the allowlist domains). Run it live in the ALWAYS-ON
# section; the fixture calls below retain falsification coverage.
run_guard "trio_integrity/live" 0 \
    bash "$SCRIPT_DIR/trio_integrity_guard.sh"

# ── 6. cross_trio_guard (live repo) ──────────────────────────────────────────
echo "[6] cross_trio_guard.sh (live repo)"
# P2-21: templates/L4/ is no longer empty (137 files across 21 domains); the
# SP3-era ZERO_SCAN stub is stale. Gate live imports on the LIVE repo so an
# orphaned L2 import in a real template can never slip past R25. Fixture calls
# below retain falsification coverage.
run_guard "cross_trio/live" 0 \
    bash "$SCRIPT_DIR/cross_trio_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "=== Fixture verification ==="
    FIXTURES_TRIO="$SCRIPT_DIR/fixtures/trio_integrity"
    FIXTURES_CROSS="$SCRIPT_DIR/fixtures/cross_trio"

    # trio_integrity fixtures
    echo "[trio_integrity] pass/"
    run_guard "trio_integrity/pass" 0 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/pass"

    echo "[trio_integrity] fail_missing_frontend_yaml/"
    run_guard "trio_integrity/fail_missing_frontend_yaml" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_missing_frontend_yaml"

    echo "[trio_integrity] fail_unresolved_operation_id/"
    run_guard "trio_integrity/fail_unresolved_operation_id" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_unresolved_operation_id"

    echo "[trio_integrity] fail_coverage_shortfall/"
    run_guard "trio_integrity/fail_coverage_shortfall" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_coverage_shortfall"

    echo "[trio_integrity] fail_zero_scan/"
    run_guard "trio_integrity/fail_zero_scan" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_zero_scan"

    echo "[trio_integrity] pass_frontend_only_practices/"
    run_guard "trio_integrity/pass_frontend_only_practices" 0 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/pass_frontend_only_practices"

    echo "[trio_integrity] fail_frontend_only_missing_source_ref/"
    run_guard "trio_integrity/fail_frontend_only_missing_source_ref" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_missing_source_ref"

    echo "[trio_integrity] fail_frontend_only_unreachable_route/"
    run_guard "trio_integrity/fail_frontend_only_unreachable_route" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_unreachable_route"

    echo "[trio_integrity] fail_frontend_only_item_non_null_operation/"
    run_guard "trio_integrity/fail_frontend_only_item_non_null_operation" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_item_non_null_operation"

    # cross_trio fixtures
    echo "[cross_trio] pass/"
    run_guard "cross_trio/pass" 0 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/pass"

    echo "[cross_trio] fail_orphan_l2_import/"
    run_guard "cross_trio/fail_orphan_l2_import" 1 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/fail_orphan_l2_import"

    echo "[cross_trio] fail_zero_scan/"
    run_guard "cross_trio/fail_zero_scan" 1 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/fail_zero_scan"

    # BACKLOG P2-45 — a tsx-less templates/L4/<dir>/ is skipped here (correctly: it has no
    # imports to evidence-anchor, and the "declared frontend vertical must ship a real .tsx"
    # axis is owned by full_trio_artifact_completeness_guard — failing on it here would
    # duplicate that guard AND false-positive on the 4 live backend_only verticals). What was
    # wrong is that the skip was SILENT. Since the deliverable is output, not an exit code,
    # an exit-code fixture cannot prove it — this harness asserts the SKIP line + count, so
    # deleting the reporting flips it 0 -> 1.
    echo "[cross_trio] pass_skip_reported/ (P2-45 skip visibility)"
    run_guard "cross_trio/pass_skip_reported" 0 \
        bash "$FIXTURES_CROSS/cross_trio_skip_report_run.sh"

    # ── 7. recipe_governance_guard (SP37) ────────────────────────────────────
    echo ""
    echo "[7] recipe_governance_guard.sh (fixtures)"
    run_guard "recipe_governance/fixtures" 0 \
        bash "$SCRIPT_DIR/recipe_governance_guard.sh" --fixtures

    # ── 9f. cross_recipe_inv_uniqueness_guard fixtures (R12 SP49) ────────────
    echo ""
    echo "[9f] cross_recipe_inv_uniqueness_guard.sh (fixtures)"
    run_guard "cross_recipe_inv_uniqueness/fixtures" 0 \
        bash "$SCRIPT_DIR/cross_recipe_inv_uniqueness_guard.sh" --fixtures

    # ── 10f. applied_recipes_alphabetical_guard fixtures (R12 SP49) ──────────
    echo ""
    echo "[10f] applied_recipes_alphabetical_guard.sh (fixtures)"
    run_guard "applied_recipes_alphabetical/fixtures" 0 \
        bash "$SCRIPT_DIR/applied_recipes_alphabetical_guard.sh" --fixtures

    # ── 11f. agents_md_toc_disk_truth_guard fixtures (R13 SP51 — TD-2026-05-25-033)
    echo ""
    echo "[11f] agents_md_toc_disk_truth_guard.sh (fixtures)"
    run_guard "agents_md_toc_disk_truth/pass_unmodified_toc" 0 \
        bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/agents_md_toc_disk_truth/pass_unmodified_toc"
    run_guard "agents_md_toc_disk_truth/fail_manual_toc_edit" 1 \
        bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/agents_md_toc_disk_truth/fail_manual_toc_edit"
fi

# ── 7. recipe_governance_guard (live repo) ───────────────────────────────────
echo "[7] recipe_governance_guard.sh (live repo)"
# Exits 0 when recipes/ does not exist. Validates applied_recipe: annotations
# and recipe invariant resolution once recipes/ lands (SP35+).
run_guard "recipe_governance/live" 0 \
    bash "$SCRIPT_DIR/recipe_governance_guard.sh"

# ── 8. recipe_spec_referential_integrity_guard (SP35) ────────────────────────
echo "[8] recipe_spec_referential_integrity_guard.sh (live repo)"
# Validates enabled_l4_domains, l2_blocks_used, l3_pages_used, and
# business_invariants referential integrity across all specs/recipes/*.yaml.
# Enforces the recipe-invariants-must-resolve rule (SP37).
run_guard "recipe_spec_referential_integrity/live" 0 \
    bash "$SCRIPT_DIR/recipe_spec_referential_integrity_guard.sh"

# ── 9. cross_recipe_inv_uniqueness_guard (R12 SP49 — TD-2026-05-24-030) ──────
echo "[9] cross_recipe_inv_uniqueness_guard.sh (live repo)"
# Protective guard: blocks future cycles from declaring the same
# (L4_domain_prefix, business_invariants[].id) pair across ≥2 recipes.
# Disk census at R12 PRD signature: zero current collisions.
run_guard "cross_recipe_inv_uniqueness/live" 0 \
    bash "$SCRIPT_DIR/cross_recipe_inv_uniqueness_guard.sh"

# ── 10. applied_recipes_alphabetical_guard (R12 SP49 — TD-2026-05-24-031) ────
echo "[10] applied_recipes_alphabetical_guard.sh (live repo)"
# Mechanizes R6-R10 manual alphabetical-insert discipline for the
# applied_recipes: plural list in templates/L4/*/README.md. Skips R5 legacy
# singular form and keyless L4 READMEs.
run_guard "applied_recipes_alphabetical/live" 0 \
    bash "$SCRIPT_DIR/applied_recipes_alphabetical_guard.sh"

# ── 11. agents_md_toc_disk_truth_guard (R13 SP51 — TD-2026-05-25-033) ────────
echo "[11] agents_md_toc_disk_truth_guard.sh (live repo)"
# 25th hard guard. Re-runs practices/generate_agents.sh and diffs against
# the committed practices/AGENTS.md (whole file + defensive TOC slice).
# Surfaces sha-asymmetry: rule edits trigger sentinel sha refresh, but
# L4/recipe/verdict adds or hand-edited TOC bodies leave the sentinel intact
# while drifting the TOC — caught here.
run_guard "agents_md_toc_disk_truth/live" 0 \
    bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh"

# ── 11.5. recipe_tenant_model_declaration_guard (iter-3 — NC6 closure) ──────
echo "[11.5] recipe_tenant_model_declaration_guard.sh (live repo)"
# Mechanical regression prevention for spec MUST
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001 clause (a)
# ("RECIPE.md frontmatter MUST declare tenant_model: single | multi").
# iter-2 closed the 10-recipe coverage gap; this guard locks the
# regression so the next recipe author cannot silently omit declaration.
run_guard "recipe_tenant_model_declaration/live" 0 \
    bash "$SCRIPT_DIR/recipe_tenant_model_declaration_guard.sh"

# ── 11.6. l4_readme_tenant_model_declaration_guard (iter-8 — NC11 symmetry) ─
echo "[11.6] l4_readme_tenant_model_declaration_guard.sh (live repo)"
# Mechanical regression prevention for spec MUST
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001 clause (b)
# ("every templates/L4/<domain>/README.md MUST declare its tenant model
# via a `**Tenant model**:` line that cites this spec anchor"). iter-7
# extended the spec MUST to cover both entry surfaces and added the
# declaration line to all 12 L4 READMEs; this guard locks the L4 side
# symmetrically (the RECIPE.md side is already locked above).
run_guard "l4_readme_tenant_model_declaration/live" 0 \
    bash "$SCRIPT_DIR/l4_readme_tenant_model_declaration_guard.sh"

# ── 12. spec_policy_ref_guard (R17 — TD-2026-05-20-035) ──────────────────────
echo "[12] spec_policy_ref_guard.sh (live repo)"
# 26th hard guard. Validates every `policy_ref: blueprints/<file>.yaml#<anchor>`
# in specs/*.yaml resolves to (1) an existing blueprint file and (2) an
# existing top-level or nested YAML anchor inside it. Closes the P2 R16
# critique: cross_trio_guard caught templates/ imports but missed
# spec-internal policy_ref dangling references — Spec Trio self-violation
# slipped through. R17 also fixed 5 pre-existing dangling refs discovered
# on first guard run (audit-log#immutability, auth#login.rate_limit,
# email-outbox#admin, file-storage#security × 2).
run_guard "spec_policy_ref/live" 0 \
    bash "$SCRIPT_DIR/spec_policy_ref_guard.sh"

# ── 13. payment_provider_type_enum_guard (iter-10 — NEW-NC6 closure) ─────────
echo "[13] payment_provider_type_enum_guard.sh (live repo)"
# 29th hard guard. Enforces spec MUST
# specs/payment-l0.yaml#PAYMENT-PROVIDER-ENUM-001:
# blueprints/payment-manifest.yaml#provider.type MUST be a member of
# #provider.type_allowed (strict string equality, snake_case ASCII).
# Closes P2 Round 10 NEW-NC6 — silent-acceptance of free-string typos
# in the PaymentProvider SPI selection key.
run_guard "payment_provider_type_enum/live" 0 \
    bash "$SCRIPT_DIR/payment_provider_type_enum_guard.sh"

# ── 14. multi_tenant_aop_guard_skeleton (dogfood-5 — P2 R3 GAP-NEW-2 closure) ─
echo "[14] multi_tenant_aop_guard_skeleton_guard.sh (live repo + passing fixture)"
# 30th hard guard. Enforces practices/rules/multi-tenant-aop-guard-skeleton.md
# 11-file canonical adoption at every .../multitenancy/ package. Closes
# P2 Round 3 GAP-NEW-2: manifest aop-guard named AuthorizedTenantInterceptor
# + @AuthorizedTenant + @TenantId but shipped no body, leaving the most
# security-critical 60 lines of multi-tenant adoption to fork-receiver
# invention (risking 403-vs-404 existence leakage and tenant_id detail
# leakage). dogfood-5 ships the bodies + this guard.
run_guard "multi_tenant_aop_guard_skeleton/live" 0 \
    bash "$SCRIPT_DIR/multi_tenant_aop_guard_skeleton_guard.sh"

# ── 15. recipe_sibling_sync_guard (dogfood-7 — gap 5 closure) ────────────────
echo "[15] recipe_sibling_sync_guard.sh (live repo)"
# 31st hard guard. Compares recipes/<pattern>/RECIPE.md frontmatter against
# specs/recipes/<pattern>-recipe-l0.yaml across enabled_l4_domains,
# l2_blocks_used, l3_pages_used. Discovered by P2 R7 dry-run: booking
# RECIPE.md had three L1 primitives (calendar / date-range-picker /
# relative-time) in l2_blocks_used that the spec yaml deliberately
# excluded — cross_trio_guard never compared the two recipe siblings.
run_guard "recipe_sibling_sync/live" 0 \
    bash "$SCRIPT_DIR/recipe_sibling_sync_guard.sh"

# ── 16. manifest_yaml_strict_parse_guard (dogfood-9 — NEW-3 closure, 32nd guard) ─
echo "[16] manifest_yaml_strict_parse_guard.sh (live repo)"
# Strict YAML parse + duplicate-key detection for every blueprints/*-manifest.yaml.
# Discovered by P2 R8 dry-run: ui-tokens-manifest.yaml line 27 had
# `placeholder:{ ... }` (no space between key colon and inline flow mapping)
# — lenient parsers accept, strict reject. Same pattern as the dogfood-6
# manifest typo fix; this guard locks in the property across all 29 manifests.
run_guard "manifest_yaml_strict_parse/live" 0 \
    bash "$SCRIPT_DIR/manifest_yaml_strict_parse_guard.sh"

# ── 17. override_schema_guard (dogfood-9 — gap 6 sentinel, 33rd guard) ───────
echo "[17] override_schema_guard.sh (live repo)"
# Sentinel: validates `override_allowed:` blocks in recipes/<slug>/RECIPE.md
# frontmatter + specs/recipes/<slug>-recipe-l0.yaml against the contract at
# specs/recipes/_override-schema.yaml. Today (dogfood-9 land) every override
# is commented-out — guard fires only when fork-receivers activate one and
# violate the schema (missing rationale, unknown L4 domain, placeholder
# citation, etc.). Closes P2 R3 gap 6.
run_guard "override_schema/live" 0 \
    bash "$SCRIPT_DIR/override_schema_guard.sh"

# ── 18. ledger_audit_nullability_guard (dogfood-11 — R11 GAP-B closure, 34th guard) ─
echo "[18] ledger_audit_nullability_guard.sh (live repo)"
# Locks the JPA entity column nullability (PaymentEvent.paymentId @Column
# nullable=...) and the Flyway migration SQL nullability (CREATE TABLE
# payment_events.payment_id + any ALTER COLUMN DROP/SET NOT NULL statements
# across V*.sql) in lockstep. Closes the dogfood-10 stopgap that routed
# redirect-style PG callback signature_fail audit rows with unresolved
# inboundOrderId to a sentinel UUID(0,0) inside payment_events — which
# polluted the PAYMENT-RECON-001 hash chain. dogfood-11 relaxed the NOT NULL
# (V006 + entity update) so orphan audit rows persist with paymentId=null;
# this guard makes future desync between the two sides a hard fail. Spec
# anchor: specs/payment-l0.yaml#PAYMENT-CALLBACK-001.
run_guard "ledger_audit_nullability/live" 0 \
    bash "$SCRIPT_DIR/ledger_audit_nullability_guard.sh"

# ── 19. l4_domain_enum_sync_guard (dogfood-12 — R12 closure, 35th guard) ─────
echo "[19] l4_domain_enum_sync_guard.sh (live repo)"
# Enforces 3-source coherence of L4 domain enumeration across
# (1) templates/L4/<domain>/ disk dirs,
# (2) specs/recipes/_override-schema.yaml $defs.l4_domain enum,
# (3) specs/recipes/*-recipe-l0.yaml enabled_l4_domains lists,
# against the canonical classification specs/l4-domain-classification.yaml.
# Validates 6 invariants (I1..I6): tier uniqueness + classified-disk-coverage
# + classified-schema-coverage + per-tier disk/schema/recipe presence rules.
# Closes the R12 framing gap (3-source disagreement was undocumented +
# unverified before dogfood-12).
run_guard "l4_domain_enum_sync/live" 0 \
    bash "$SCRIPT_DIR/l4_domain_enum_sync_guard.sh"

echo ""
echo "[20] payment_callback_restassured_compliance_guard.sh (live repo)"
# 35th hard guard — dogfood-13 R13 GAP-C closure.
# Makes specs/payment-l0.yaml#PAYMENT-CALLBACK-001's test_method scalar
# ("RestAssured integration test") mechanically binding: at least one
# integration test in backend/src/test/.../payment/ MUST hit all three
# markers (RestAssured import + @SpringBootTest RANDOM_PORT + literal
# "/api/payments/callback/" POST). MockMvc usage in any *Callback* file
# is rejected as CLAUDE.md anti-pattern.
run_guard "payment_callback_restassured_compliance/live" 0 \
    bash "$SCRIPT_DIR/payment_callback_restassured_compliance_guard.sh"

# ── 21. payment_provider_qualifier_consistency_guard (dogfood-14 — R14 GAP-A closure, 36th guard) ─
echo ""
echo "[21] payment_provider_qualifier_consistency_guard.sh (live repo)"
# 36th hard guard. Locks the SlowProviderLatencyDecorator ↔ MockProvider bean
# resolution contract: decorator constructor MUST take interface PaymentProvider
# + @Qualifier("rawPaymentProvider"), MockProvider MUST register under bean name
# "rawPaymentProvider". Without this guard, a fork-receiver adding a real PG
# adapter (Stripe / Toss / KG Inicis / NICE / KCP) would hit a silent bean
# resolution conflict the moment they tried to keep both beans scoped by
# @Profile — discovered only at runtime when @Primary fell back to the mock.
run_guard "payment_provider_qualifier_consistency/live" 0 \
    bash "$SCRIPT_DIR/payment_provider_qualifier_consistency_guard.sh"

# ── 22. ledger_audit_tenant_nullable_guard (R4 — P2 GAP-R3-3 closure, 37th guard) ─
echo ""
echo "[22] ledger_audit_tenant_nullable_guard.sh (live repo + passing fixture)"
# 37th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #ledger-audit-tenant-scope: audit / ledger / append-only event entities
# that may be appended OUTSIDE a tenant-scoped request boundary
# (e.g. PG callback signature_fail at permitAll endpoint) MUST NOT
# implement TenantOwned, MUST declare tenant_id @Column nullable=true,
# and MUST expose getTenantId() returning Optional<UUID>. Closes
# P2 dogfood R3 GAP-R3-3: PaymentEventLedger.append() invoked from
# PaymentCallbackController signature_fail path threw
# TenantContextMissingException → 500 → external PG retried indefinitely
# (NICE / Toss V1 retry up to 24h). The catalog previously had no
# policy for the asymmetry between request-scoped resources and audit
# entities; this guard locks the policy mechanically.
run_guard "ledger_audit_tenant_nullable/live" 0 \
    bash "$SCRIPT_DIR/ledger_audit_tenant_nullable_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[22f] ledger_audit_tenant_nullable_guard.sh --fixtures"
    run_guard "ledger_audit_tenant_nullable/fixtures" 0 \
        bash "$SCRIPT_DIR/ledger_audit_tenant_nullable_guard.sh" --fixtures
fi

# ── 23. callback_tenant_resolution_guard (R5 — P2 GAP-R3-4 closure, 38th guard) ─
echo ""
echo "[23] callback_tenant_resolution_guard.sh (live repo + passing fixture)"
# 38th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #callback-tenant-resolution.verifier_contract: external PG callback
# verifiers (NICE / Toss / KakaoPay) in multi-tenant fork-receivers MUST
# atomically pair signature verification with tenant resolution in one
# call returning UUID, MUST consume raw bytes (not String) for the
# request body, and MUST distinguish no-match from multiple-match via
# two distinct exception types (CallbackSignatureMismatchException vs
# AmbiguousTenantResolutionException). Closes P2 dogfood R3 GAP-R3-4:
# the manifest's #context-resolution forbade orderId / path / query as
# tenant signals but never said what IS allowed for permitAll callback
# endpoints, leaving the most security-critical resolution to invented
# (and forgeable) heuristics. R5 ships the canonical per-tenant secret
# policy + this guard.
run_guard "callback_tenant_resolution/live" 0 \
    bash "$SCRIPT_DIR/callback_tenant_resolution_guard.sh"

# ── 24. scheduled_task_tenant_scope_guard (R6 — P2 GAP-R3-5 closure, 39th guard) ─
echo ""
echo "[24] scheduled_task_tenant_scope_guard.sh (live repo + passing fixture)"
# 39th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #scheduled-task-tenant-scope: @Scheduled / Quartz / Shedlock-style
# periodic jobs (reconciliation sweeps, retention purges, daily summaries)
# in multi-tenant fork-receivers MUST adopt the per-tenant iteration
# pattern with three load-bearing properties — balanced TenantContext
# set/clear via try/finally (count equality), tenantCatalog.listActive()
# as the SINGLE source of tenant enumeration (no hardcoded UUID lists),
# and a @SchedulerLock name template containing the #tenantId substring
# (Shedlock SpEL) so two cluster nodes can process two tenants in
# parallel instead of serializing the whole fleet behind one global
# lock. Closes P2 dogfood R3 GAP-R3-5: the manifest covered
# request-scoped (#context-resolution), AOP-scoped (#aop-guard),
# async-submission-scoped (#async-propagation), and callback-scoped
# (#callback-tenant-resolution) — but the scheduler/cron path was
# undefined, leaving fork-receivers writing a nightly
# PaymentReconciliationJob to reach for either bare repository.findAll()
# (silent cross-tenant read) or TenantContext.set(SYSTEM_TENANT_UUID)
# (the GAP-R3-3 sentinel anti-pattern in a new disguise).
run_guard "scheduled_task_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/scheduled_task_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[24f] scheduled_task_tenant_scope_guard.sh --fixtures"
    run_guard "scheduled_task_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/scheduled_task_tenant_scope_guard.sh" --fixtures
fi

# ── 25. realtime_connection_tenant_scope_guard (R7 — P2 GAP-NEW-1 closure, 40th guard) ─
echo ""
echo "[25] realtime_connection_tenant_scope_guard.sh (live repo + passing fixture)"
# 40th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #realtime-connection-tenant-scope: SseEmitter / WebSocketSession-based
# long-lived push connections in multi-tenant fork-receivers MUST adopt
# the registry + per-message set/clear pattern with three load-bearing
# clauses — connection-registration reads tenantId from TenantContext
# .current() (not from @RequestParam / @RequestHeader), broadcast
# iterates with a tenantId equality filter (not bare emitters.forEach
# (send)), and per-message TenantContext.set / TenantContext.clear
# wraps each .send() call with count equality (mirrors the R6 39th
# clause-1 algorithm). Closes P2 dogfood R7 GAP-NEW-1: the manifest
# covered request-scoped (#context-resolution), AOP-scoped
# (#aop-guard), async-submission-scoped (#async-propagation),
# callback-scoped (#callback-tenant-resolution), and scheduler-scoped
# (#scheduled-task-tenant-scope) — but the long-lived push connection
# regime (SSE / STOMP @MessageMapping / raw WebSocketHandler) was
# undefined, leaving fork-receivers writing a tenant admin dashboard
# SseEmitter to reach for either bare emitters.forEach(e -> e.send(...))
# inside an @EventListener (silent cross-tenant push leak) or
# @RequestParam("tenant_id") tenantId at register time (attacker
# subscribes to any tenant's stream by passing the URL).
run_guard "realtime_connection_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/realtime_connection_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[25f] realtime_connection_tenant_scope_guard.sh --fixtures"
    run_guard "realtime_connection_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/realtime_connection_tenant_scope_guard.sh" --fixtures
fi

# ── 26. broker_fanout_tenant_scope_guard (R8 — P2 GAP-NEW-2 closure, 41st guard) ─
echo ""
echo "[26] broker_fanout_tenant_scope_guard.sh (live repo + passing fixture)"
# 41st hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #broker-fanout-tenant-scope: cross-node broker fan-out bridges
# (Redis Pub/Sub, Kafka) used to scale SSE / WebSocket realtime push
# horizontally beyond a single node MUST adopt the envelope-header +
# per-message set/clear pattern with four load-bearing clauses —
# publish-side wraps payload in TenantBrokerEnvelope (or Kafka
# X-Tenant-Id header) before convertAndSend (not bare
# convertAndSend(channel, payload)), subscribe-side listener body
# reads tenantId from the envelope BEFORE TenantContext.current()
# (broker thread has empty TenantContext by construction),
# per-message TenantContext.set / TenantContext.clear wraps each
# dispatch with count equality (mirrors the R7 40th clause-3
# algorithm), and local sendToTenant dispatch passes
# envelope.tenantId() EXPLICITLY (not TenantContext.current()).
# Closes P2 dogfood R8 GAP-NEW-2: R7 closed single-node long-lived
# connection scope but explicitly named broker fan-out as the
# unresolved >1-node SSE deployment case. fork-receivers writing
# a 2-node SSE deployment behind an LB would reach for either bare
# redisTemplate.convertAndSend(channel, payload) (consumer-side
# listener has no tenant signal — NPE / silent default-tenant
# fallback / stale-tenantId-from-previous-message leak) or
# consumer-side TenantContext.current() (returns Optional.empty on
# broker thread). 41st guard mechanically blocks all four
# anti-patterns. Single-node deployments live-repo SKIP — no
# .../multitenancy/TenantAware*Bridge.java present.
run_guard "broker_fanout_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/broker_fanout_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[26f] broker_fanout_tenant_scope_guard.sh --fixtures"
    run_guard "broker_fanout_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/broker_fanout_tenant_scope_guard.sh" --fixtures
fi

# ── 27. kafka_consumer_tenant_scope_guard (R9 — kafka-consumer closure, 42nd guard) ─
echo ""
echo "[27] kafka_consumer_tenant_scope_guard.sh (live repo + passing fixture)"
# 42nd hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-consumer-tenant-scope: long-running Kafka business-event
# consumers (distinct surface from #broker-fanout-tenant-scope which
# covers fan-out-INTO-SSE bridges) MUST adopt the shared-topic +
# X-Tenant-Id header + per-record set/clear pattern with five
# load-bearing clauses — listener body reads the per-record
# X-Tenant-Id header BEFORE any TenantContext.current() call
# (consumer poll thread has empty TenantContext by construction);
# batch listeners (List<ConsumerRecord> / ConsumerRecords
# signature) MUST set TenantContext INSIDE the for-loop, not
# once at method entry (batch_set_once is the most subtle
# failure mode — passes single-tenant tests, leaks in
# interleaved production batches); count(set) == count(clear)
# in the listener body; ConsumerRebalanceListener callbacks
# (onPartitionsAssigned / onPartitionsRevoked) MUST be tenant-
# free (the poll thread between batches has no tenant signal);
# manual Acknowledgment MUST NOT run inside the per-record
# TenantContext span (canonical: batch ack after the for-loop).
# Closes the kafka-consumer open question carried in R7
# (#realtime-connection-tenant-scope.open_questions_remaining[2])
# and re-affirmed in R8 (#broker-fanout-tenant-scope.open_questions_remaining[1]).
# Kafka-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareKafkaConsumer.java present.
run_guard "kafka_consumer_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_consumer_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[27f] kafka_consumer_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_consumer_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_consumer_tenant_scope_guard.sh" --fixtures
fi

# ── 28. kafka_streams_tenant_scope_guard (R10 — kafka-streams closure, 43rd guard) ─
echo ""
echo "[28] kafka_streams_tenant_scope_guard.sh (live repo + passing fixture)"
# 43rd hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-tenant-scope: Kafka Streams (KStream / KTable)
# topologies (distinct surface from #kafka-consumer-tenant-scope:
# R9 covers stateless @KafkaListener consumers; this anchor covers
# stateful aggregation pipelines with RocksDB-backed state stores,
# wall-clock punctuators, and tenant-namespaced joins) MUST adopt
# the tenant-prefixed-key + punctuator key-decode + tenant-namespaced
# join pattern with five load-bearing clauses — topologies with
# groupBy/groupByKey/aggregate MUST have an upstream selectKey
# lambda that reads the X-Tenant-Id header and constructs a
# composite key with KEY_SEPARATOR (single RocksDB store partitions
# per-tenant by key prefix); punctuator bodies that call forward(...)
# MUST wrap each per-key forward in TenantContext.set/clear
# (StreamThread has empty TenantContext by construction);
# count(set) == count(clear) inside the punctuator lambda body;
# topologies with .join/.leftJoin/.outerJoin MUST also have a
# tenant-prefix selectKey on the upstream stream; Materialized.as
# state store names MUST NOT interpolate tenantId (static topology
# build cannot declare dynamic stores — rebalance/standby breaks).
# Closes the kafka-streams open question that was the first
# entry (R9-era index [0]) of #kafka-consumer-tenant-scope.open_questions_remaining
# in the R9-committed manifest — entry removed on R10 closure.
# Stream-processing-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareKafkaStreamsTopology.java present.
run_guard "kafka_streams_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[28f] kafka_streams_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_tenant_scope_guard.sh" --fixtures
fi

# ── 29. kafka_streams_interactive_queries_tenant_scope_guard
#      (R11 — IQ read-side OBVERSE of R10 closure, 44th guard) ────────────────
echo ""
echo "[29] kafka_streams_interactive_queries_tenant_scope_guard.sh (live repo + passing fixture)"
# 44th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-interactive-queries-tenant-scope: Kafka Streams
# Interactive Queries (HTTP-exposed state-store reads — distinct
# surface from #kafka-streams-tenant-scope: R10 is the WRITE side
# (selectKey tenant-prefix + punctuator set/clear); this anchor
# covers the READ side that mirrors the write-side prefix at
# request time) MUST adopt the (TenantContext.current() prefix +
# store.range scoped scan + path mismatch → 404 + fresh store
# reference per query) pattern with four load-bearing clauses —
# IQ files MUST call TenantContext.current() (no path/query/body
# tenantId as prefix); MUST NOT call store.all() (unscoped scan
# fragile under refactor); MUST NOT throw AccessDeniedException
# or map to HTTP 403 (existence leak — canonical is 404 via
# TenantBoundaryViolationException + MultiTenantProblemDetailAdvice);
# MUST NOT declare a ReadOnly*Store field (caching across requests
# breaks under Streams rebalance — partition reassignment makes
# the cached reference read from the OLD assignment).
# Closes the IQ open question that was the first entry (R10-era
# index [0]) of #kafka-streams-tenant-scope.open_questions_remaining
# in the R10-committed manifest — entry removed on R11 closure.
# IQ-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareInteractiveQueryService.java present.
run_guard "kafka_streams_interactive_queries_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_interactive_queries_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[29f] kafka_streams_interactive_queries_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_interactive_queries_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_interactive_queries_tenant_scope_guard.sh" --fixtures
fi

# ── 30. kafka_streams_standby_rpc_tenant_scope_guard
#      (R12 — cluster fan-out OBVERSE of R11 closure, 45th guard) ──────────────
echo ""
echo "[30] kafka_streams_standby_rpc_tenant_scope_guard.sh (live repo + passing fixture)"
# 45th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-standby-rpc-tenant-scope: Kafka Streams Interactive
# Queries cluster fan-out (the cross-node RPC layer that activates
# when the local node does NOT host the partition for a tenant's
# prefix; distinct surface from
# #kafka-streams-interactive-queries-tenant-scope: R11 is the
# SINGLE-NODE store-range read, this anchor covers the MULTI-NODE
# router that decides local-vs-remote and HTTP-forwards remote
# calls) MUST adopt the (TenantContext.current() at the router +
# X-Tenant-Id header on every forward + no tenantId in forward URL
# + fresh metadata lookup per query) pattern with four load-bearing
# clauses — standby forwarder files MUST set X-Tenant-Id on every
# forward (without it the receiving node's TenantContext is empty
# and R11 clause(1) trips on the remote IQ service); MUST call
# TenantContext.current() (no path/query/body tenantId as the
# metadata-lookup key); MUST NOT embed tenantId as a URL path
# segment (header is the sole tenant carrier across the wire);
# MUST NOT cache KeyQueryMetadata / Collection<StreamsMetadata>
# fields or HostInfo fields with initialisers (Streams rebalance
# invalidates prior metadata — constructor-injected self-host
# HostInfo identity fields without `=` initialisers are permitted).
# Closes the standby replica RPC open question that was the first
# entry (R11-era index [0]) of
# #kafka-streams-interactive-queries-tenant-scope.open_questions_remaining
# in the R11-committed manifest — entry removed on R12 closure.
# Single-node clusters and IQ-free deployments live-repo SKIP —
# no .../multitenancy/TenantAwareStandbyForwardingService.java present.
run_guard "kafka_streams_standby_rpc_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_standby_rpc_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[30f] kafka_streams_standby_rpc_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_standby_rpc_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_standby_rpc_tenant_scope_guard.sh" --fixtures
fi

# ── 31. webclient_async_tenant_scope_guard
#      (R13 — reactive async-router OBVERSE of R12 closure, 46th guard) ─────────
echo ""
echo "[31] webclient_async_tenant_scope_guard.sh (live repo + passing fixture)"
# 46th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #webclient-async-tenant-scope: outbound Spring WebFlux WebClient
# adoption (the reactive HTTP client surface that fork-receivers
# reach for when calling third-party SaaS APIs — Slack, Stripe,
# SendGrid, NICE페이먼츠 채널 조회, etc.) MUST adopt the
# (X-Tenant-Id header set in the filter + Mono.deferContextual as
# the SOLE tenant extraction point + Reactor Context written at the
# controller from TenantContext.current() + zero ThreadLocal access
# inside the reactive chain) pattern with four load-bearing
# clauses — every ExchangeFilterFunction file MUST stamp the
# X-Tenant-Id header (without it the receiving service sees no
# tenant signal); MUST reference Mono.deferContextual / ContextView
# (Reactor Context is the only chain-safe tenant carrier across
# scheduler hops); MUST NOT call TenantContext.current() outside a
# `public static` servlet-thread helper (the filter body runs on a
# Reactor scheduler thread where the ThreadLocal is empty or holds
# a stale prior subscription's tenant); MUST NOT call
# TenantContext.set / TenantContext.clear at all (these have zero
# legitimate use inside a WebClient filter and are the canonical
# scheduler-worker reuse leak vector). Closes the reactive /
# WebClient async fan-out open question that was the third entry
# (R12-era index [2]) of
# #kafka-streams-standby-rpc-tenant-scope.open_questions_remaining
# in the R12-committed manifest — entry removed on R13 closure.
# WebClient-free deployments live-repo SKIP —
# no .../multitenancy/TenantAwareWebClientFilter.java present.
run_guard "webclient_async_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/webclient_async_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[31f] webclient_async_tenant_scope_guard.sh --fixtures"
    run_guard "webclient_async_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/webclient_async_tenant_scope_guard.sh" --fixtures
fi

# ── 32. recipe_invariant_spec_normalization_guard
#       (R24 — enforcement-loop-closure, 47th guard) ─────────────────────────
echo ""
echo "[32] recipe_invariant_spec_normalization_guard.sh (live repo + passing fixture)"
# R24 35th-new hard guard (catalog total: 47).
# Closes the catalog enforcement-loop gap surfaced in R24 root-cause-fix mode:
# every business invariant ID prefixed with a recipe's own UPPERCASE prefix
# (e.g. ECOM-INV-001 in recipes/e-commerce/RECIPE.md, B2B-ADMIN-INV-003 in
# recipes/b2b-admin/RECIPE.md) MUST appear in the matching recipe spec
# (specs/recipes/<slug>-recipe-l0.yaml#business_invariants[].id). RECIPE.md is
# narrative; the spec yaml is the surface that downstream guards (substance,
# cross_recipe_inv_uniqueness, recipe_spec_referential_integrity,
# spec_policy_ref) actually read. Before this guard a maintainer could add an
# INV-XYZ paragraph to RECIPE.md without normalizing it to the spec and every
# mechanical check would silently skip it — catalog vision "규칙 밖 AI output
# BLOCKED" leaked at the recipe-narrative-to-spec boundary.
run_guard "recipe_invariant_spec_normalization/live" 0 \
    bash "$SCRIPT_DIR/recipe_invariant_spec_normalization_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[32f] recipe_invariant_spec_normalization_guard.sh --fixtures"
    run_guard "recipe_invariant_spec_normalization/fixtures" 0 \
        bash "$SCRIPT_DIR/recipe_invariant_spec_normalization_guard.sh" --fixtures
fi

# ── 33. test_tag_naming_convention_guard
#       (R24 — enforcement-loop-closure, 48th guard) ─────────────────────────
echo ""
echo "[33] test_tag_naming_convention_guard.sh (live repo + passing fixture)"
# R24 36th-new hard guard (catalog total: 48).
# Closes the catalog enforcement-loop gap surfaced in R24 root-cause-fix mode:
# every backend JUnit @Tag("...") value MUST follow the catalog UPPERCASE
# convention — pattern ^[A-Z][A-Z0-9_.-]*$ . The per-domain test tasks in
# backend/build.gradle.kts (testAsvs/testPayment/testCrud/testSearch/...)
# pivot on these tag values via includeTags("UPPERCASE"); tag drift silently
# excludes tests from `./gradlew test{Domain}` runs and breaks the catalog
# promise of "single command binary pass/fail". R24 surfaced 8 lowercase
# @Tag("search") drifts in backend/src/test/.../search/*.java that this
# guard catches mechanically + retroactively (and at PR time going forward).
run_guard "test_tag_naming_convention/live" 0 \
    bash "$SCRIPT_DIR/test_tag_naming_convention_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[33f] test_tag_naming_convention_guard.sh --fixtures"
    run_guard "test_tag_naming_convention/fixtures" 0 \
        bash "$SCRIPT_DIR/test_tag_naming_convention_guard.sh" --fixtures
fi

# ── R37 ralplan retrofit guards ───────────────────────────────────────────────
# Added at R37 per integrated review (architect + critic + codex consensus).
# Closes: methodology theater, dogfood ledger drift, multi-tenant traceability,
# L4 reachability, composition completeness.

echo ""
echo "[34] dogfood_ledger_guard.sh (live repo + passing fixture)"
run_guard "dogfood_ledger/live" 0 \
    bash "$SCRIPT_DIR/dogfood_ledger_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[34f] dogfood_ledger_guard.sh --fixtures"
    run_guard "dogfood_ledger/fixtures" 0 \
        bash "$SCRIPT_DIR/dogfood_ledger_guard.sh" --fixtures
fi

echo ""
echo "[35] multi_tenant_deferral_citation_guard.sh (live repo + passing fixture)"
run_guard "multi_tenant_deferral_citation/live" 0 \
    bash "$SCRIPT_DIR/multi_tenant_deferral_citation_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[35f] multi_tenant_deferral_citation_guard.sh --fixtures"
    run_guard "multi_tenant_deferral_citation/fixtures" 0 \
        bash "$SCRIPT_DIR/multi_tenant_deferral_citation_guard.sh" --fixtures
fi

echo ""
echo "[36] l4_domain_reachability_guard.sh (live repo)"
run_guard "l4_domain_reachability/live" 0 \
    bash "$SCRIPT_DIR/l4_domain_reachability_guard.sh"

echo ""
echo "[37] composition_completeness_guard.sh (live repo, WARN-level v1)"
run_guard "composition_completeness/live" 0 \
    bash "$SCRIPT_DIR/composition_completeness_guard.sh"

echo ""
echo "[38] l4_frontend_domain_mode_guard.sh (live repo — R59, 41st guard)"
run_guard "l4_frontend_domain_mode/live" 0 \
    bash "$SCRIPT_DIR/l4_frontend_domain_mode_guard.sh"

echo ""
echo "[39] l4_role_default_failclosed_guard.sh (live repo — R83, 42nd guard)"
run_guard "l4_role_default_failclosed/live" 0 \
    bash "$SCRIPT_DIR/l4_role_default_failclosed_guard.sh"

echo ""
echo "[40] stored_error_column_sanitize_guard.sh (live repo — R81, 43rd guard)"
run_guard "stored_error_column_sanitize/live" 0 \
    bash "$SCRIPT_DIR/stored_error_column_sanitize_guard.sh"

echo ""
echo "[41] background_poll_refresh_state_guard.sh (live repo — R82b, 44th guard)"
run_guard "background_poll_refresh_state/live" 0 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh"
run_guard "background_poll_refresh_state/fixture_pass" 0 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh" --root "$SCRIPT_DIR/fixtures/background-poll-refresh-state/pass_clean" --ledger "$SCRIPT_DIR/fixtures/background-poll-refresh-state/pass_clean/ledger.yaml"
run_guard "background_poll_refresh_state/fixture_fail_comment_only" 1 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh" --root "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_comment_only" --ledger "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_comment_only/ledger.yaml"
run_guard "background_poll_refresh_state/fixture_fail_unledgered_comment_only" 1 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh" --root "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_unledgered_comment_only" --ledger "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_unledgered_comment_only/ledger.yaml"
run_guard "background_poll_refresh_state/fixture_fail_no_data_updated_at" 1 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh" --root "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_no_data_updated_at" --ledger "$SCRIPT_DIR/fixtures/background-poll-refresh-state/fail_no_data_updated_at/ledger.yaml"

echo ""
echo "[42] dogfood_finding_expiry_trigger_guard.sh (live repo — R85b, 45th guard)"
run_guard "dogfood_finding_expiry_trigger/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_expiry_trigger_guard.sh"

echo ""
echo "[43] dogfood_finding_real_bug_closure_commit_guard.sh (live repo — R86b, 46th guard)"
run_guard "dogfood_finding_real_bug_closure_commit/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_real_bug_closure_commit_guard.sh"

echo ""
echo "[44] dogfood_finding_real_bug_test_coverage_guard.sh (live repo — R87b, 47th guard)"
run_guard "dogfood_finding_real_bug_test_coverage/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_real_bug_test_coverage_guard.sh"

echo ""
echo "[45] wave_kickoff_ledger_guard.sh (live repo — R97, 48th guard)"
run_guard "wave_kickoff_ledger/live" 0 \
    bash "$SCRIPT_DIR/wave_kickoff_ledger_guard.sh"

echo ""
echo "[46] registry_backfill_completeness_guard.sh (live repo — R98, 49th guard)"
run_guard "registry_backfill_completeness/live" 0 \
    bash "$SCRIPT_DIR/registry_backfill_completeness_guard.sh"

echo ""
echo "[47] entity_migration_guard.sh (IMW1-C — IDW1 entity↔migration coverage; ddl-auto hides drift)"
run_guard "entity_migration/live" 0 \
    bash "$SCRIPT_DIR/entity_migration_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # IMW3 / IDW3 G4 regression: the INLINE @Entity @Table pair on ONE line.
    # The old anchor ^\s*@Entity\s*(\(|$) missed this form (own-line=EXIT1,
    # same-line=EXIT0), so an un-migrated entity shipped GREEN. The widened
    # (?m)^\s*@Entity\b now DETECTS it. pass/ proves inline @Table resolution +
    # a backing CREATE TABLE → 0; fail_inline_entity/ proves the false negative
    # is now caught (inline @Entity, no migration, empty allowlist → 1).
    echo ""
    echo "[47f] entity_migration_guard.sh --fixtures (inline @Entity @Table detection)"
    run_guard "entity_migration/fixture_pass" 0 \
        bash "$SCRIPT_DIR/entity_migration_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/entity_migration/pass"
    run_guard "entity_migration/fixture_fail_inline_entity" 1 \
        bash "$SCRIPT_DIR/entity_migration_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/entity_migration/fail_inline_entity"

    # name_collision_guard inline-stereotype detection (IMW3-followup / IDW3 G4 audit):
    # the guard's stereotype anchor had the SAME own-line false-negative as entity_migration —
    # an inline `@Service @Transactional` escaped detection. The \b token-boundary fix catches it.
    echo ""
    echo "[50f] name_collision_guard.sh --fixtures (inline @Service collision detection)"
    run_guard "name_collision/fixture_pass" 0 \
        bash "$SCRIPT_DIR/name_collision_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/name_collision/pass"
    run_guard "name_collision/fixture_fail_inline" 1 \
        bash "$SCRIPT_DIR/name_collision_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/name_collision/fail_inline"
fi

echo ""
echo "[48] spec_ref_code_guard.sh (IMW1-C — IDW1 backend specs/*.yaml reference resolution)"
run_guard "spec_ref_code/live" 0 \
    bash "$SCRIPT_DIR/spec_ref_code_guard.sh"

echo ""
echo "[49] controller_problemdetail_guard.sh (IMW1-D — IDW2: every domain @ExceptionHandler returns RFC9457 ProblemDetail)"
run_guard "controller_problemdetail/live" 0 \
    bash "$SCRIPT_DIR/controller_problemdetail_guard.sh"

echo ""
echo "[50] name_collision_guard.sh (IMW2-C — IDW2: cross-package @Entity/@Service/@Repository/@Controller simple-name collision)"
run_guard "name_collision/live" 0 \
    bash "$SCRIPT_DIR/name_collision_guard.sh"

echo ""
echo "[51] role_literal_guard.sh (IMW2-C — IDW2: every @PreAuthorize authority literal maps to a UserRole or API scope)"
run_guard "role_literal/live" 0 \
    bash "$SCRIPT_DIR/role_literal_guard.sh"

echo ""
echo "[52] audit_on_read_guard.sh (IMW4 — IDW4: a @Phi-returning read method must reference AuditLogService.record)"
# Forward-enforcing: the live main tree has NO @Phi usage yet, so the PHI type
# set is empty and the guard exits 0. It fires only once a fork-receiver tags
# real PHI with common/Phi.java. Closes the IDW4 hole where an adversarial probe
# shipped an un-audited PHI read with a fully GREEN build (HIPAA §164.312(b)).
run_guard "audit_on_read/live" 0 \
    bash "$SCRIPT_DIR/audit_on_read_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[52f] audit_on_read_guard.sh --fixtures (pass→0, fail→1)"
    run_guard "audit_on_read/fixtures" 0 \
        bash "$SCRIPT_DIR/audit_on_read_guard.sh" --fixtures
fi

echo ""
echo "[53] phi_in_logs_guard.sh (IMW4 — IDW4: no log.{info,debug,warn,error,trace}(...) may interpolate a @Phi getter)"
# Forward-enforcing companion to [52]: the live main tree has NO @Phi usage yet,
# so the forbidden-getter set is empty and the guard exits 0. It fires only once
# a fork-receiver tags real PHI. Closes the IDW4 hole where an adversarial probe
# shipped a raw-PHI log statement with a fully GREEN build (HIPAA §164.312(b)).
run_guard "phi_in_logs/live" 0 \
    bash "$SCRIPT_DIR/phi_in_logs_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[53f] phi_in_logs_guard.sh --fixtures (pass→0, fail→1)"
    run_guard "phi_in_logs/fixtures" 0 \
        bash "$SCRIPT_DIR/phi_in_logs_guard.sh" --fixtures
fi

echo ""
echo "[54] consent_gate_guard.sh (IMW5 — IDW4: a data-sharing method in a ConsentRecord-adopting tree must reference ConsentGate)"
# Forward-enforcing: the live main tree ships common/ConsentRecord as the consent
# ledger @Entity, but has NO domain method matching a data-sharing signal, so the
# candidate set is empty → vacuous pass. The guard fires only once a fork-receiver
# BOTH adopts the consent ledger AND writes a sharing path (third-party share /
# marketing send / export) — exactly when consent-management-l0#CONSENT-PURPOSE-001
# attaches. Closes the IDW4 hole where a SPEC-ONLY consent subsystem let an
# adversarial probe ship an un-gated third-party share with a fully GREEN build.
run_guard "consent_gate/live" 0 \
    bash "$SCRIPT_DIR/consent_gate_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[54f] consent_gate_guard.sh --fixtures (pass→0, fail→1, no_entity→0)"
    run_guard "consent_gate/fixtures" 0 \
        bash "$SCRIPT_DIR/consent_gate_guard.sh" --fixtures
fi

echo ""
echo "[55] controller_repository_shell_guard.sh (IMW5 — IDW4: shell-level Controller→Repository ban; closes the run-all-guards vs ArchUnit coverage asymmetry)"
# Green-on-current: IMW1-A routed every controller through a service, so no
# *Controller injects/calls a *Repository. Mirrors ArchitectureLayerBoundaryTest
# at the shell level so the boundary is caught in run-all-guards, not only under a
# full gradle test run.
run_guard "controller_repository_shell/live" 0 \
    bash "$SCRIPT_DIR/controller_repository_shell_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[55f] controller_repository_shell_guard.sh --fixtures (pass→0, fail_repo_injection→1)"
    run_guard "controller_repository_shell/fixtures" 0 \
        bash "$SCRIPT_DIR/controller_repository_shell_guard.sh" --fixtures
fi

echo ""
echo "[56] lint_own_blocks_guard.sh (FMW1 — FDW1: the catalog must lint its OWN shipped React blocks; templates/L2/blocks + L0 must satisfy every ax/* rule)"
# FRONTEND guard (practices-react/evals). Green-on-current: column-picker /
# MappingEditor / column-reorder O(n*m) lookups fixed in FMW1. SKIPs gracefully
# (exit 0) when frontend/node_modules/eslint is absent (e.g. backend-only CI).
run_guard "lint_own_blocks/live" 0 \
    bash "$SCRIPT_DIR/../../practices-react/evals/lint_own_blocks_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # P3-87 (Lane D) — wiring detection used to be a whole-file `grep -qF` on the rule name,
    # which a rule name sitting inside a COMMENT satisfies. The fail fixture puts
    # `// 'ax/no-god-route': 'warn',` in the config and nothing else, so a config that wires
    # the rule NOWHERE reported it as wired. Now the config object's keys are parsed.
    run_guard "lint_own_blocks/fixture_fail_comment_fooled" 1 \
        bash "$SCRIPT_DIR/../../practices-react/evals/lint_own_blocks_guard.sh" --root "$SCRIPT_DIR/../../practices-react/evals/fixtures/lint-own-blocks/fail_comment_fooled"
    run_guard "lint_own_blocks/fixture_pass_clean" 0 \
        bash "$SCRIPT_DIR/../../practices-react/evals/lint_own_blocks_guard.sh" --root "$SCRIPT_DIR/../../practices-react/evals/fixtures/lint-own-blocks/pass_clean"
fi

echo ""
echo "[57] saved_view_url_state_guard.sh (FMW4b — FDW2: upgrade saved-view rule from manual review to a binary guard; localStorage forbidden in saved-view files + L4 crud ref must dogfood useUrlListState)"
run_guard "saved_view_url_state/live" 0 \
    bash "$SCRIPT_DIR/saved_view_url_state_guard.sh"
run_guard "saved_view_url_state/fixture_fail" 1 \
    bash "$SCRIPT_DIR/saved_view_url_state_guard.sh" --check-fixture \
    "$SCRIPT_DIR/fixtures/saved-view-must-be-url-state-or-server-persisted/fail_saved_view_localstorage_only"
run_guard "saved_view_url_state/fixture_pass" 0 \
    bash "$SCRIPT_DIR/saved_view_url_state_guard.sh" --check-fixture \
    "$SCRIPT_DIR/fixtures/saved-view-must-be-url-state-or-server-persisted/pass"

echo ""
echo "[58] verification_checklist_task_coverage_guard.sh (2026-05-30 audit C6 — every registered test{Domain} task must be in the verify-completion hard gate)"
run_guard "verification_checklist_task_coverage/live" 0 \
    bash "$SCRIPT_DIR/verification_checklist_task_coverage_guard.sh"

echo ""
echo "[59] practices_react_sentinel_disk_truth_guard.sh (2026-05-30 audit C5 — React mirror of [11]: re-run practices-react/generate_agents.sh + diff AGENTS.md/SKILL.md + family-table sum == rule_count)"
run_guard "practices_react_sentinel_disk_truth/live" 0 \
    bash "$SCRIPT_DIR/practices_react_sentinel_disk_truth_guard.sh"

echo ""
echo "[60] doc_headline_count_guard.sh (2026-05-30 audit C1/C2/C3 — README hero + CLAUDE.md vision + plugin.json headline counts must match disk: Java/React/ESLint rules · L4 dirs · hard guards)"
run_guard "doc_headline_count/live" 0 \
    bash "$SCRIPT_DIR/doc_headline_count_guard.sh"

echo ""
echo "[61] money_boundary_seam_guard.sh (#39 money-l0 reconcile — block raw BigDecimal.valueOf(<minor getter>) at the long-minor → BigDecimal-major payment boundary; use common/Money.toMajorUnits)"
run_guard "money_boundary_seam/live" 0 \
    bash "$SCRIPT_DIR/money_boundary_seam_guard.sh"

echo ""
echo "[62] randomport_contextcache_dirtiescontext_guard.sh (R22 lever — any @SpringBootTest that NAMES the Spring TestContext ContextCache cap-32 eviction hazard MUST carry @DirtiesContext, else the aggregate flakes return in a sibling class; narrowed to require an actual @SpringBootTest — a plain Jackson test documenting the hazard in a comment is not a candidate)"
run_guard "randomport_contextcache_dirtiescontext/live" 0 \
    bash "$SCRIPT_DIR/randomport_contextcache_dirtiescontext_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[62f] randomport_contextcache_dirtiescontext_guard.sh --fixtures (pass=plain-Jackson-test-naming-hazard→0, fail=RANDOM_PORT-no-DirtiesContext→1, pass_fqn=FQN-@SpringBootTest+FQN-@DirtiesContext→0, fail_fqn=FQN-@SpringBootTest-no-DirtiesContext-escape→1)"
    run_guard "randomport_contextcache_dirtiescontext/fixtures" 0 \
        bash "$SCRIPT_DIR/randomport_contextcache_dirtiescontext_guard.sh" --fixtures
fi

echo ""
echo "[63] liveness_probe_no_downstream_guard.sh (health-check-l0 HEALTH-LIVENESS-001 — a liveness health group that gates on a downstream dependency (db/redis/kafka/…) restart-loops the fleet on a dependency blip; dependencies belong in readiness)"
run_guard "liveness_probe_no_downstream/live" 0 \
    bash "$SCRIPT_DIR/liveness_probe_no_downstream_guard.sh"

echo ""
echo "[64] rule_tag_binding_guard.sh (A1 closure — every practices rule's verification.tag MUST resolve to a real @Tag in backend/src/test; a phantom-tag rule must not pass the gates claiming verification it lacks)"
run_guard "rule_tag_binding/live" 0 \
    bash "$SCRIPT_DIR/rule_tag_binding_guard.sh"

echo ""
echo "[65] rule_verification_binding_guard.sh (A1 tail closure — generalizes [64] to ALL verification forms: every rule's verification MUST resolve to gradle_task+tag / guard / pattern+fixture, OR declare type:review; no rule may claim verification it lacks)"
run_guard "rule_verification_binding/live" 0 \
    bash "$SCRIPT_DIR/rule_verification_binding_guard.sh"

echo ""
echo "[66] test_tag_task_coverage_guard.sh (2026-06-01 audit — class-centric @Tag→per-domain-task binding: a test class whose every @Tag is consumed by NO includeTags(...) escapes every hard gate and runs only under the advisory aggregate; complements [58] task↔checklist and [64] rule.tag→@Tag)"
run_guard "test_tag_task_coverage/live" 0 \
    bash "$SCRIPT_DIR/test_tag_task_coverage_guard.sh"

echo ""
echo "[67] no_rrn_in_log_guard.sh (2026-06-01 audit — mechanizes CRITICAL rule no-rrn-logging: no log.<level>(...) may reference a raw RRN (rrn word-bounded / 주민); rrnHash/rrnMasked allowed. phi_in_logs_guard does NOT cover it — that keys on @Phi getters, of which the backend has zero)"
run_guard "no_rrn_in_log/live" 0 \
    bash "$SCRIPT_DIR/no_rrn_in_log_guard.sh"
run_guard "no_rrn_in_log/fixture_fail" 1 \
    bash "$SCRIPT_DIR/no_rrn_in_log_guard.sh" --root "$SCRIPT_DIR/fixtures/no-rrn-logging/fail_rrn_in_log"
run_guard "no_rrn_in_log/fixture_pass" 0 \
    bash "$SCRIPT_DIR/no_rrn_in_log_guard.sh" --root "$SCRIPT_DIR/fixtures/no-rrn-logging/pass"

echo ""
echo "[68] domain_spec_trio_guard.sh (ax-plan G2/G4 — every L4 domain carries the Spec Trio its domain_mode requires; promoted to BLOCKING at G4 hard-promotion 2026-06-06)"
run_guard "domain_spec_trio/live" 0 \
    bash "$SCRIPT_DIR/domain_spec_trio_guard.sh"

echo ""
echo "[69] spec_item_verification_binding_guard.sh (ax-plan G3/G4 — every applicable spec item resolves a REAL verification binding tag|guard|rule; no fakes, no deferred. Promoted to BLOCKING at G4 hard-promotion 2026-06-06 after full backfill 286->0)"
run_guard "spec_item_verification_binding/live" 0 \
    bash "$SCRIPT_DIR/spec_item_verification_binding_guard.sh"

echo ""
echo "[70] spec_scaffold_unfilled_guard.sh (ax-plan G6 forcing wire — a scaffolded-but-unplanned spec (still carrying the '# TODO: Add' marker) keeps the catalog RED until /ax-plan fills it; this is what makes an empty skeleton FAIL, since the trio + binding guards pass an empty skeleton vacuously)"
run_guard "spec_scaffold_unfilled/live" 0 \
    bash "$SCRIPT_DIR/spec_scaffold_unfilled_guard.sh"
run_guard "spec_scaffold_unfilled/fixture_fail" 1 \
    bash "$SCRIPT_DIR/spec_scaffold_unfilled_guard.sh" --root "$SCRIPT_DIR/fixtures/spec-scaffold-unfilled/fail_unplanned"
run_guard "spec_scaffold_unfilled/fixture_pass" 0 \
    bash "$SCRIPT_DIR/spec_scaffold_unfilled_guard.sh" --root "$SCRIPT_DIR/fixtures/spec-scaffold-unfilled/pass_filled"

echo ""
echo "[71] aggregate_boundary_allowlist_guard.sh (DDD decomposition spec 2026-06-08 §5 — the aggregate_boundary_allowlist.yaml escape-hatch surface is schema-valid: every exception from/to resolves to a real class, every published_api class resolves in its feature, no wildcards outside shared_kernel, and no exception is past its expiry. Stops a grandfather edge from becoming a permanent escape hatch.)"
run_guard "aggregate_boundary_allowlist/live" 0 \
    bash "$SCRIPT_DIR/aggregate_boundary_allowlist_guard.sh"

echo ""
echo "[72] aggregate_tagging_completeness_guard.sh (DDD decomposition spec 2026-06-08 §4 back-tag wave forcing function — every JPA @Entity in backend main sources MUST carry exactly one of @AggregateRoot / @AggregateMember(root=...). Tagging cannot regress and a new untagged @Entity fails the build, which is the prerequisite that lets the marker-dependent TIER-1 guards be sound.)"
run_guard "aggregate_tagging_completeness/live" 0 \
    bash "$SCRIPT_DIR/aggregate_tagging_completeness_guard.sh"

echo ""
echo "[73] feature_boundary_allowlist_guard.sh (frontend decomposition spec 2026-06-08 §5 — practices-react/feature_boundary_allowlist.yaml is schema-valid: every exception from/to + published_api barrel resolves under frontend/src, no wildcards outside shared_layers, and no exception is past its expiry. The frontend analog of aggregate_boundary_allowlist_guard.)"
run_guard "feature_boundary_allowlist/live" 0 \
    bash "$SCRIPT_DIR/../../practices-react/evals/feature_boundary_allowlist_guard.sh"

echo "[74] evidence_quote_spotcheck_guard.sh (BACKLOG P2-1 — evidence_guard checks STRUCTURE not TRUTH, so a fabricated quote passes every blocking gate; this deterministic quote-vs-snapshot sweep closes the offline half. P2-1a 2026-07-13: QUOTE 미매칭 83건 전량 소진 → --strict --allow-missing-snapshot 승격. P2-18 2026-07-14: Java-side 스냅숏 본문 전량 커밋{47 fetch + 재앵커 21룰 + 조작 quote 4건 정직 재앵커} + react-side 잔여 2본문 커밋 → live를 FULL --strict로 최종 승격 — QUOTE_NOT_IN_SNAPSHOT와 SNAPSHOT_FILE_MISSING 둘 다 HARD-FAIL, allow-missing 잔여 0. B3 fixture 쌍은 --allow-missing-snapshot 플래그 시맨틱 회귀 방지용으로 유지. ANCHOR RATCHET (P1-1, cross-family reviewer 2026-07-30, TD-2026-07-30-P1-anchor-ratchet): the five-surface census compares five CURRENT-TREE values to each other, so lowering all five together (64→63 in LIVE_MIN_PROTECTED_ENTRIES + the frozenset + the ledger's `# min_entries:` + one `# require:` + the matching row) left the census EQUAL and the gate GREEN while an anchor silently left the fatal set — the reviewer reproduced exactly that. The reference is now the PREVIOUS RELEASE: `git show <origin/main→HEAD fallback>:<this guard>` is AST-parsed (reformatting the frozenset does not evade it) and the guard enforces LIVE_MIN_PROTECTED_ENTRIES(current) >= anchor (MONOTONIC_FLOOR_REGRESSION, exit 4) and LIVE_REQUIRED_PROTECTED_IDENTITIES(anchor) ⊆ current (PROTECTED_IDENTITY_REMOVED, exit 5 — subset, not equality, so growth stays legal while SUBSTITUTION at a constant count is caught). Because the census forces all five surfaces equal, ratcheting the two guard-side constants transitively ratchets the ledger's rows, min_entries and require directives. Sound because R25 runs at a tree AHEAD of origin/main and the pre-push recency guard binds pushes to R25-at-HEAD. Live-root only — fixture roots never anchor, so these two codes are not fixture-coverable; their evidence is the live reproductions recorded in DECISIONS.md (reviewer's repro: pre-fix copy exit 0, this guard exit 4; substitution at count 64: exit 5; both restored byte-identically). Weak spots stated in the guard header: HEAD fallback is weaker than origin/main, and a tree with no git history skips the ratchet with a WARN (it is also unpushable). P1-A ROUND-2 (reviewer round 2, 2026-07-30, TD-2026-07-30-P1-anchor-ratchet-r2): the ratchet reads the anchor through a PARSER, and everything the parser could not recognize used to WARN+SKIP — making the parser itself a TWO-RELEASE bypass. Release N changes REPRESENTATION ONLY (runtime still 64/64, so the census is equal and it ships green): LIVE_MIN_PROTECTED_ENTRIES = int(\"64\") is no longer an ast.Constant int, LIVE_REQUIRED_PROTECTED_IDENTITIES aliased to another name yields no tuple literals, or the here-document opener is respelled so the embedded python block is not located at all. Release N+1 then does the real 64→63 downgrade against an anchor its own parser cannot read ⇒ WARN ANCHOR_NO_FLOOR / ANCHOR_NO_PIN_SET / ANCHOR_UNPARSEABLE ⇒ exit 0. Reproduced end-to-end in a throwaway clone: phase-1 shape exits 0 on the round-1 guard, and phase-2 (63 across all five surfaces, accordion silently out of the fatal set: 63 files / 67 anchors) exits 0 on the round-1 guard against that synthetic anchor. TWO LAYERS now: (1) SELF-PARSE CHECK, exit 6 SELF_UNPARSEABLE — on every live-root run the guard feeds ITS OWN CURRENT FILE to the SAME parser and demands both constants parse AND that the parsed values EQUAL the runtime ones (so a decoy/shadowing assignment or an added/removed literal tuple is caught too); the unreadable representation therefore dies at its OWN gate at generation N and can never become anybody's anchor. (2) FAIL-CLOSED ANCHOR SEMANTICS, exit 7 ANCHOR_UNPARSEABLE — three anchor states are now distinguished: (i) anchor/git unavailable → WARN+skip STAYS (nothing to compare; such a tree is unpushable), (ii) this file ABSENT at the anchor → WARN+skip STAYS (git history is not attacker-creatable), (iii) PRESENT at the anchor but constants not extractable → BLOCKING, because (iii) is the only one of the three a mutable tree can manufacture on purpose. The heredoc extractor is also pattern-based now, so the benign reflow variant is simply READ rather than skipped. Both codes are anchor/live-root-bound and NOT fixture-coverable — evidence is the live reproductions in DECISIONS.md (exit 0 → 6 for the phase-1 shape, exit 0 → 7 for the phase-2 downgrade, states (i)/(ii) verified still-skipping), all restored byte-identically. P1-X/P1-Y ROUND-3 (reviewer round 3, 2026-07-30, TD-2026-07-30-P1-anchor-authenticity): rounds 1-2 hardened WHAT the anchor says; round 3 attacked WHICH COMMIT it is and HOW ITS BYTES ARE READ. (X) refs/remotes/origin/main is an ORDINARY LOCAL REF — git update-ref aims it at FAKE = the downgrade's tree MINUS the two ratcheting guards, parented on the real release, and state (ii) 'absent at the anchor ⇒ bootstrap skip' becomes reachable: measured, the coherent five-surface 64→63 downgrade exits 4 against an honest anchor and exit 0 against FAKE, because the audit line fingerprints the working TREE and a ref is not part of the tree. Three independent layers: ANCESTRY (the anchor must be an ancestor of HEAD — exit 8, fires FIRST on the reviewer's repro; a genuinely divergent branch BLOCKS with 'git fetch && git rebase origin/main' printed, never skips); BOOTSTRAP PLAUSIBILITY (a file that HAS HISTORY IN THIS REPO can never legitimately be 'absent in the previous release' — git rev-list -1 anchor -- path must be EMPTY, plus a redundant probe from HEAD's walk; measured to fire ALONE with ancestry deliberately satisfied, exit 8); and AUDIT-BOUND ANCHOR (verify-completion.sh records anchor_sha via the shared helper practices/scripts/lib/release_anchor.sh, and .githooks/pre-push passes the sha GIT hands it from the REMOTE'S OWN ADVERTISEMENT so completion_checklist_recency_guard requires equality — AUDIT_ANCHOR_FORGED / AUDIT_ANCHOR_UNBOUND, fixture-proven, and the pre-round-3 recency guard exits 0 on the same fixture). (Y) SYMLINK REPRESENTATION LAUNDERING — the anchor side reads GIT OBJECTS and the self side reads the FILESYSTEM, and any place those two disagree is a laundering channel. Path components may hold any byte but '/' and NUL, so a symlink TARGET PATHNAME can be spelled as valid weakened python AND still resolve on disk: measured, gen-N ships (round-2 exit 0) and gen-N+1 reads 'floor 0 → 63, pin set 1 → 63' out of the link blob and ships the downgrade (round-2 exit 0). Closed by a MODE CHECK on BOTH sides — ANCHOR_PATH_NOT_REGULAR (git mode must be 100644/100755, exit 8) and SELF_PATH_NOT_REGULAR (lstat on the leaf AND on every path component, so a symlinked DIRECTORY cannot launder a regular leaf; exit 2, runs on EVERY root). The anchor-side codes are live-root-bound and NOT fixture-coverable (evidence = the RED reproductions in DECISIONS.md, tree restored byte-identically); the self side IS, via fail_protected_ledger_symlink — a byte-copy of pass_template_correct_anchor differing only by the link, exit 2, and exit 0 under the pre-round-3 guard.) P1-1/P1-2/P1-3 ROUND-4 (reviewer round 4, 2026-07-30, TD-2026-07-30-P1-anchor-runtime): rounds 1-3 authenticated WHAT the anchor says, WHICH commit it is and HOW its bytes are represented — round 4 found the ratchet's own RUNTIME unauthenticated, in four places that are all INPUTS the ratchet trusts. (1) GIT REPLACEMENT REFS: `git replace <real> <fabricated>` keeps every sha identical while rev-list/show/ls-tree/merge-base all read the FABRICATED object, so the ancestry and bootstrap layers (which ARE those commands) can be answered out of a history the attacker wrote while the audit records the authentic sha and the pre-push binding against the remote still matches. Closed twice over: GIT_NO_REPLACE_OBJECTS=1 exported by the shared helper (this is what reaches the python subprocesses) plus an explicit --no-replace-objects on every call, AND a blocking refusal to run at all on a tree that carries refs/replace/* — ANCHOR_REPLACE_REFS_PRESENT, exit 8 here / exit 5 in the manifest guard / push-blocking in the hook and the recency guard, because "we read past them everywhere" is a claim about every present and future call site. (2) TOCTOU ON THE ANCHOR REF: the runner resolved it for the audit line and each guard re-resolved origin/main independently later, so aiming the ref at an ancient root commit for exactly the minutes the guards run makes every ratchet bootstrap-skip while the recorded sha stays honest. SINGLE-RESOLUTION BINDING: the runner resolves once and exports AX_RELEASE_ANCHOR_SHA/_KIND/_REF/_ROOT, each guard re-reads the ref at its own moment of use and requires equality (ANCHOR_REF_MOVED_MIDRUN), a standalone guard re-reads it after its work, and the runner re-verifies at the END and writes the run AS A FAILURE on drift (anchor_sha_end/anchor_stable, mirroring tree_stable/tree_samples; fixture fail_anchor_moved_midrun proves the consumer half). (3) THE HELPER IS AN INJECTABLE POLICY SURFACE: its idempotence guard returned BEFORE defining anything, so `export -f ax_anchor_*` + a forged _AX_RELEASE_ANCHOR_LIB=1 left the ATTACKER's policy authoritative (measured: the round-3 helper reports anchor=ATTACKER, exit 0); both loaders used [ -f ] which FOLLOWS SYMLINKS; and the helper was on nobody's anchor-critical list, so deleting it activated the AX_RELEASE_ANCHOR_LIB env fallback. Now: the early return is GONE (definitions are re-established on every source, the marker guards only the report), every reserved name is unset -f'd and an inherited definition is HELPER_FUNCTION_INJECTED exit 2, each consumer runs a DELIBERATELY DUPLICATED inline lstat+git-mode preflight before sourcing (HELPER_PATH_NOT_REGULAR — it cannot live in the file it checks), the env fallback is honored only on a NON-git root, and the helper + verify-completion.sh + the recency guard + both hook files are on BOTH guards' anchor-critical lists (deletion at the anchor is ANCHOR_BOOTSTRAP_IMPLAUSIBLE, symlinking is ANCHOR_PATH_NOT_REGULAR). All three sub-attacks are proven blocked by practices/scripts/ax-prove-helper-injection-blocked.sh, registered below with a negative control. (4) THE AUDIT RECORD WAS A CLAIM: see the recency guard's checks 9-13.)"
run_guard "evidence_quote_spotcheck/live" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict
run_guard "evidence_quote_spotcheck/fixture_fail" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_quote_absent"
run_guard "evidence_quote_spotcheck/fixture_pass" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/pass_quote_present"
run_guard "evidence_quote_spotcheck/fixture_fail_allow_missing" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --allow-missing-snapshot --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_quote_mismatch_snapshot_missing"
run_guard "evidence_quote_spotcheck/fixture_pass_allow_missing" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --allow-missing-snapshot --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/pass_only_missing_snapshot"
# BACKLOG P2-40 — evidence_quote_spotcheck previously skipped templates/** frontmatter
# entirely (a fabricated evidence:upstream_id quote there passed every gate). --include-templates
# sweeps templates/**/*.{tsx,ts}; findings stay advisory unless --strict-templates is ALSO passed
# (see the guard's --strict-templates header comment for why: the first live sweep found ~105
# pre-existing quote<->snapshot misalignments across templates/L1/components/** unrelated to the
# one fabricated anchor this item closed — tracked as a new backlog candidate, not silently
# fixed or silently dropped). Live stays advisory (exit 0); fixtures prove the --strict-templates
# blocking path is genuinely non-vacuous.
run_guard "evidence_quote_spotcheck/templates_live_advisory" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --include-templates
run_guard "evidence_quote_spotcheck/fixture_template_fail" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --include-templates --strict-templates --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_template_fabricated_anchor"
run_guard "evidence_quote_spotcheck/fixture_template_pass" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --include-templates --strict-templates --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/pass_template_correct_anchor"
# P2-40 follow-up (2026-07-28 review) — the advisory registration above does NOT block:
# restoring the fabricated currency-input.tsx anchor only WARNed and templates_live_advisory
# still exited 0, so the required RED-on-revert did not hold. The full templates sweep stays
# ADVISORY for a real reason (the ~105 pre-existing quote<->snapshot misalignments across
# templates/L1/components/**, a separate backlog item — making them fatal would freeze the
# gate on out-of-scope work), so the fatal claim is earned on an explicitly listed subset
# instead: evidence_protected_template_anchors.txt. Those anchors ARE fatal here, and the
# ledger cannot be emptied into a silent pass (missing/empty/no-min_entries/shrunk/dangling
# path/no-upstream_id-evidence/uncited-anchor/empty-quote/empty-section each exit 2 —
# fixtures below prove all nine, plus the eight round-3 identity/type fixtures registered
# after them). empty-quote and empty-section close a codex round-2
# finding: `quote` defaulted to "" when absent, and "" is a substring of every snapshot, so
# blanking/deleting the protected quote used to pass vacuously (0 findings, exit 0) instead
# of failing — the fabricated-anchor defense was bypassed by REMOVING the quote rather than
# falsifying it.
run_guard "evidence_quote_spotcheck/templates_protected_live" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected
run_guard "evidence_quote_spotcheck/fixture_protected_fail_quote" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_template_fabricated_anchor"
run_guard "evidence_quote_spotcheck/fixture_protected_pass_quote" 0 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/pass_template_correct_anchor"
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_missing" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_missing"
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_empty" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_empty"
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_no_min" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_no_min"
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_shrunk" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_shrunk"
# P1-Y round-3 (2026-07-30): the fixture-provable half of the symlink class. Byte-copy of
# pass_template_correct_anchor whose protected-anchor ledger is a SYMLINK to a sibling holding
# the identical bytes and nothing else — so pass → 0 and this → 2 is attributable to the link
# alone. The class: the anchor side reads GIT OBJECTS (`git show` returns the link's target
# PATHNAME) while the self side reads the FILESYSTEM (open() follows it), and a pathname can be
# spelled as parseable-but-weakened python. Non-vacuity: the pre-round-3 guard exits 0 on it.
# Exit 2 (structural), so the [87] exit-1 kill-proof floor is unchanged.
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_symlink" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_symlink"
# ── P1-3 ROUND-4 (2026-07-30, TD-2026-07-30-P1-anchor-runtime) ─────────────────────
# The shared helper practices/scripts/lib/release_anchor.sh decides the whole ratchet policy,
# which makes it the single thing worth attacking. Two of its three sub-attacks are not
# directory-shaped, so they cannot be `--root` fixtures: FUNCTION INJECTION lives in the
# ENVIRONMENT (bash imports exported functions across `bash script.sh`) and HELPER DELETION is
# about which file the loader falls back to. The scripted proof runs all three against the LIVE
# guard in a throwaway sandbox and includes its own negative control (the unattacked sandbox
# exits 0, so the attacks' exit 2 is attributable to them). It also reproduces the pre-fix
# behaviour from git — the round-3 helper reports anchor=ATTACKER and exits 0 — so the proof is
# not vacuous.
run_guard "release_anchor/helper_injection_blocked" 0 \
    bash "$REPO_ROOT/practices/scripts/ax-prove-helper-injection-blocked.sh"
# ── ROUND-5 (2026-07-30, TD-2026-07-30-P1-hermetic-runtime) ────────────────────────
# Round 4 proved the helper's own NAMES cannot be injected. Round 5 attacks one level down: the
# RUNTIME the gate is made of. The git REPOSITORY is chosen by the environment (GIT_DIR /
# GIT_WORK_TREE — measured: a DIRTY tree fingerprinted as the clean-tree constant of a shadow
# checkout, and reported a clean status and the shadow's toplevel); the COMMANDS are chosen by the
# environment (an exported `git` rewrote the anchor pin, an exported `pwd` moved the pin's root to
# a foreign repo, an exported `python3` turned a FAILING guard into exit 0); and the gate's own
# IMPLEMENTATION is a mutable file (a regular-file tree_fingerprint.py rewritten to print "nogit"
# turned a refused forgery into a passing push, because the recompute only objected when the
# output was non-empty and not "nogit" — unknown fell OPEN, and the runner and the verifier shared
# the compromised copy). None of that is directory-shaped, so it is a scripted proof rather than a
# `--root` fixture: it carries its own NEGATIVE CONTROL (the unattacked sandbox PASSES) and its
# own pre-fix reproduction (a sandbox built with the round-5 additions removed accepts the
# redirection attack), so neither half can be vacuous.
# ROUND 7 (TD-2026-07-30-P1-privileged-startup / P1-3) extends the same harness with three more
# classes, each with its own pre-fix twin: (M) a SELF-ERASING $BASH_ENV that leaves nothing for
# any in-script preflight to see — closed by re-executing every entry under bash PRIVILEGED MODE,
# with (M'') proving a preset AX_PRIV_REEXEC ABORTS rather than skipping; (N) the two SOURCED libs,
# which ran `export` — an ordinary command lookup — ahead of the round-6 preflight and are now
# assertions of privileged mode; (O) `filter.<n>.process`, git's long-running filter protocol,
# which converts content exactly like clean/smudge and matched neither keyword — an equal-length
# byte swap on an ordinary tracked file made `git status` EMPTY and the tree fingerprint report
# the clean-tree constant.
# ROUND 8 (TD-2026-07-31-(P1-worktree-representation) / P1-A) extends it again, on the one side
# round 7 left open: it measured BYTES only for paths whose ON-DISK SHAPE matched the index and
# `continue`d past the rest, in BOTH implementations. (P) an index-regular path marked
# --assume-unchanged, deleted and replaced by a SYMLINK to a benign file outside the repository;
# (Q) a tracked path marked --skip-worktree and deleted. Both leave `git status --porcelain`
# EMPTY, need no environment control, and — measured — produced the SAME fingerprint as a clean
# tree (0a815065…), so R25 verified bytes the push does not ship. Now GIT_INDEX_FLAGS_SET /
# GIT_WORKTREE_TYPE_MISMATCH / GIT_TRACKED_PATH_ABSENT / GIT_GITLINK_DIVERGENCE. (P')/(Q') land
# again in a committed pre-round-8 sandbox; (R1)/(R2) neuter ONLY the index-bit refusal and prove
# the representation backstop refuses on its own (a sparse checkout SETS that bit, so without this
# the backstop would be untested dead code); (S) is an over-correction control — an UNINITIALIZED
# gitlink must still PASS, because that is the ordinary post-clone shape of all three gitlinks here.
# ROUND 9 (TD-2026-07-30-(P1-representation-parity) / P1-1, P1-2, (c), (d), (e)) closes the parity
# gap round 8 left: it separated the SHAPES and then compared only BLOB BYTES, so three
# representation facts no digest carries were still accepted. (T)/(U) the EXECUTABLE BIT, both
# directions — `core.fileMode=false` + `update-index --chmod=-x` + a local `chmod +x` leaves status
# EMPTY and the fingerprint at the clean-tree constant while R25's direct `./gradlew` invocations
# run a file the push records as 100644 → GIT_EXEC_BIT_DIVERGENCE (the index mode is read from
# `ls-files -s`, so core.fileMode cannot suppress it). (V) a gitlink with NO gitdir whose directory
# is nevertheless POPULATED — round 8 exempted on the absence of `<gitlink>/.git` alone and never
# required the directory to be EMPTY, so a committed step can execute a file the push does not ship
# → GIT_GITLINK_UNINITIALIZED_POPULATED. (W) two index entries differing only in case that lstat to
# ONE inode → GIT_CASEFOLD_ALIAS (defense in depth: the ordinary form also shows as a modification).
# (T2)/(U2)/(V2)/(W2) are committed pre-round-9 twins in which each attack lands again. ROUND 9 /
# (c) additionally exercises the two round-8 branches that were implemented but never driven:
# (X) the MIRROR of (P) — an index-SYMLINK path that is a REGULAR file on disk, run with ONLY the
# index-bit refusal neutered → GIT_WORKTREE_TYPE_MISMATCH; and (Y) an INITIALIZED submodule moved
# off the recorded commit → GIT_GITLINK_DIVERGENCE. ROUND 9 / (d): a setup failure inside a case is
# now LOUD (harness exit 2) instead of `|| return 0`, which silently turned a broken case into a
# green one.
# ROUND 10 (TD-2026-07-31-(P1-casefold-prefix) / P1 + P2) closes the round-9 casefold check, which
# was LEAF-ONLY: both implementations grouped COMPLETE folded paths, so `A/check.sh` and `a/helper`
# — different leaves, ONE directory inode on APFS — never met. Measured against d567c37: status
# EMPTY, 12c's violation set EMPTY, fingerprint = the clean-tree constant, while the pushed tree
# serves no `A/helper` to a case-sensitive receiver. (Z) is that topology → GIT_CASEFOLD_DIR_ALIAS,
# with the alias measured over EVERY PATH PREFIX (gitlinks included). (Z2) is its pre-round-10 twin
# (lands again); (Z5) is the FALSE-POSITIVE control — genuinely DISTINCT `A/` and `a/` must NOT be
# refused, built on a case-sensitive filesystem when one is reachable ($WORK on Linux, or
# AX_PROVE_CS_DIR) and on the shared-prefix arm otherwise, with the arm PRINTED either way.
# ROUND 10 / P2: the round-9 twins disabled both implementations at once, so they proved the PAIR
# load-bearing and neither member; every alias/representation class now also runs sweep-only
# ((Z3)(T3)(V3)(W3) → the prior-release helper still refuses, surfacing as
# AUDIT_FINGERPRINT_UNVERIFIABLE) and helper-only ((Z4)(T4)(V4)(W4) → the 12c sweep still refuses
# on its own code). The alias cases now ASSERT their premise at gate time (two entries, one inode,
# one blob): write_audit's `git add -A` had been silently healing it, and a twin whose premise
# evaporated was passing for a reason that was not the neuter.
# ROUND 11 (TD-2026-08-01-(P1-unicode-prefix-fold) / P1) closes the FOLD the round-10 prefix map is
# KEYED with. It was `bytes.lower()` — ASCII-only and normalization-blind — so two aliases the
# filesystem serves from ONE inode were never compared, and neither needs any environment control:
# both are committed content that arrives in an ordinary clone. (AA) index `é/check.sh` (NFC c3a9,
# running `cat "é/helper"`) + index `e◌́/helper` (NFD 65cc81): measured at beee364, status EMPTY,
# `bash é/check.sh` → PASS, BOTH implementations silent, fingerprint = the clean-tree constant,
# while the pushed tree records only `e◌́/helper` → GIT_CASEFOLD_DIR_ALIAS. (AB) the same hole with
# NON-ASCII CASE (`É` c389 ≡ `é` c3a9). The key is now UNICODE CANONICAL CASELESS,
# NFC(casefold(NFD(s))) per UAX #21 §1.3, in BOTH implementations; the (st_dev, st_ino)
# discriminator and the leaf/directory code split are UNCHANGED, so no new code was needed and a
# case- or normalization-SENSITIVE fork-receiver is unaffected. (AA2)/(AB2) revert the fold to
# `bytes.lower()` in both implementations and the attacks land again; (AA3)/(AA4)/(AB3)/(AB4) are
# the per-implementation splits; (AC) proves the round-10 ASCII topology still blocks. Controls:
# (AA5) genuinely DISTINCT `É/` and `é/` on a REAL case-sensitive APFS volume are NOT refused,
# (AD) the two implementations' folds agree over every prefix of every tracked path plus an
# adversarial corpus, and (AE) drives the shipped grouping with SYNTHETIC inodes for the one
# control this platform cannot build — measured with hdiutil, case-insensitive APFS, CASE-SENSITIVE
# APFS, case-sensitive HFS+, ExFAT and FAT32 are ALL normalization-INSENSITIVE, so a distinct-inode
# NFC/NFD pair does not exist here; (AE) prints SIMULATED rather than claiming a live control.
# ROUND 12 (TD-2026-08-01-(P1-ignorable-fold) / P1) closes the THIRD equivalence axis the round-11
# fold still preserved: IGNORABLE FORMAT CHARACTERS. Case-insensitive HFS+ folds designated
# formatting controls to ZERO and skips them entirely (Apple TN1150, `FastUnicodeCompare`: "All
# ignorable characters are folded to the value zero"), so `SAFE/check.sh` (running
# `cat SAFE/helper`) plus `SAFE<U+200C ZWNJ>/helper` is ONE directory — clean checkout, local check
# PASSES, and the pushed tree carries no literal `SAFE/helper` for a receiver that treats U+200C as
# significant. Round 11's canonical caseless key PRESERVED those code points, so the shared inode
# was never compared and 12c's buckets came back EMPTY, exactly as in rounds 10 and 11. The fold
# now STRIPS general category Cf before normalizing, in both implementations; the (st_dev, st_ino)
# discriminator and the leaf/directory code split are AGAIN unchanged, so no new code was needed.
# WHY Cf AND NOT A HAND-LIST — measured, not chosen by taste: HFS+'s ignorable set is exactly 16
# code points (U+200C-200F, U+202A-202E, U+206A-206F, U+FEFF), all of them Cf; the strip must be a
# SUPERSET because a missing character is a silent false-green while an extra one cannot produce a
# refusal without an OBSERVED shared inode; no ASCII scalar is Cf, so the ASCII fast path stays a
# TRUE equivalence; and neither casefold(NFD(·)) nor NFC(·) ever introduces a Cf character, so one
# strip pass placed FIRST is provably sufficient. Default_Ignorable_Code_Point was rejected — no
# Python API, it would ship as a 4,174-code-point UCD-pinned table (3,769 unassigned), and it is
# not even a superset of Cf. (AI) asserts all four claims so they fail here rather than in a
# fork-receiver's push evidence. BOTH ARMS ARE REAL: (AF)/(AG) refuse the ZWNJ and RLO topologies
# on a case-insensitive HFS+ volume this harness ATTACHES (`hdiutil create -fs HFS+`), where the
# live volume folds exactly those 16 and none of the other 154 Cf characters; (AH) is the
# false-positive control and needs no special volume, because both APFS variants serve the two
# spellings from DISTINCT inodes and must NOT be refused. (AF2)/(AG2) revert ONLY the strip in both
# implementations and the attacks land again; (AF3)/(AF4)/(AG3)/(AG4) are the per-implementation
# splits. If no folding volume can be attached the RED direction runs SIMULATED in (AI) and says so.
# ROUND 13 (TD-2026-08-01-(P1-symlink-target-alias) / P1) attacks the census from OUTSIDE the set it
# was ever taken over. Rounds 9-12 widened the KEY three times and never widened the SUBJECT: every
# one of them registered INDEX PATHS. A symlink's TARGET is not an index path — it is BLOB CONTENT,
# read as bytes and hashed and never RESOLVED — so the whole four-round census did not apply to it
# on ANY axis. Measured at 9c8f339, committed content and no environment control: `git mv
# backend/gradlew backend/gradlew-real` + `ln -s GRADLEW-REAL backend/gradlew` leaves `git status
# --porcelain -uall` EMPTY, the wrapper RESOLVES so R25 executes it and goes green, the fingerprint
# is the clean-tree constant 0a815065… and the recency guard emits recency_pass — while a
# case-SENSITIVE receiver gets a DANGLING backend/gradlew. Both implementations now resolve a
# tracked symlink's target LEXICALLY against the link's own recorded directory and refuse a spelling
# that reaches a REGISTERED prefix's (st_dev, st_ino) under a FOLD-EQUAL but textually different
# spelling → GIT_SYMLINK_TARGET_ALIAS (fingerprint exit 15). A NEW code rather than a widening of
# 13/14, because the subject (blob content, not an index path) and the remedy (`ln -sf
# <recorded-spelling>`, not `git mv`) are both different. THE PRECISION IS THE POINT: gating on
# FOLD-EQUALITY instead of on bare inequality is what makes `..` traversal, chains through an
# intermediate symlinked directory, and absolute targets fall out without exceptions. (AJ) is the
# CASE variant, (AK) the NORMALIZATION variant — the index records NFC and the link's blob spells
# NFD, which git does not precompose, so it proves the fix reuses the SHARED fold and is not a
# case-only check — and (AL) the IGNORABLE-Cf variant on the same real HFS+ volume as (AF)/(AG).
# Each has a pre-round-13 twin that lands again and per-implementation sweep-only/helper-only
# splits. (AM) is the false-positive control and it is the one that decides shippability: NINE
# legitimate tracked symlinks in ONE tree — exact spelling, `..` traversal onto the exact record
# (the shape both live tracked symlinks in this catalog have), an ABSOLUTE target, a `..` target
# that ESCAPES the repository, an UNTRACKED (gitignored) target, a DANGLING target, a CHAIN through
# another tracked symlink, a target resolving to a tracked DIRECTORY, and one reached THROUGH an
# intermediate symlinked directory — ALL PASS. A bare "the target must equal the record" rule would
# refuse six of the nine. Deliberately NOT refused and registered rather than hidden: absolute
# targets (docs/BACKLOG.md P3-131) and committed DANGLING symlinks (P3-132 — a real defect, but a
# different class: identically broken here and at the receiver, so the evidence does not lie).
run_guard "hermetic_runtime/inherited_runtime_blocked" 0 \
    bash "$REPO_ROOT/practices/scripts/ax-prove-hermetic-runtime.sh"
# ── P1-2/P1-4 ROUND-4: the recency guard's FIXTURE SWEEP ───────────────────────────
# This script deliberately does not run completion_checklist_recency_guard against the LIVE tree
# (it audits the log verify-completion writes, and this script runs INSIDE that run — the cycle
# is documented at the top of this file). `--fixtures` has no such cycle: it reads only the
# fixture directories under practices/evals/fixtures/completion_checklist_recency and never
# touches the live .ax-verify. Registering it here is what makes the round-4 forgery detectors
# (schema pin, duplicate key, anchor endpoints, per-step ledger) part of the standing sweep
# rather than something only [87] happens to exercise for four of its items.
run_guard "completion_checklist_recency/fixtures" 0 \
    bash "$SCRIPT_DIR/completion_checklist_recency_guard.sh" --fixtures
run_guard "evidence_quote_spotcheck/fixture_protected_entry_missing_file" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_missing_file"
run_guard "evidence_quote_spotcheck/fixture_protected_entry_no_evidence" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_no_evidence"
run_guard "evidence_quote_spotcheck/fixture_protected_anchor_absent" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_anchor_absent"
run_guard "evidence_quote_spotcheck/fixture_protected_entry_empty_quote" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_empty_quote"
run_guard "evidence_quote_spotcheck/fixture_protected_entry_empty_section" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_empty_section"
# ── codex round-3 (2026-07-28): the SAME gate was bypassed a third time, with a third trick,
# because rounds 1-2 validated the SHAPE of a ledger entry and never pinned the IDENTITY of
# what must be protected nor the TYPE of the scalars compared. Both closed by construction;
# each fixture below reproduces one bypass and is non-vacuous (the HEAD~ guard exits 0 on
# seven of the eight — the eighth, snapshot_missing, exited 0 whenever --strict-templates was
# not passed and is now a flag-independent exit 2).
#   (a) TYPE COERCION — `quote: 0` became "0", a literal substring of the Stripe snapshot;
#       `section: null` became "None", which is not blank so the blank check never fired.
#       Protected scalars are no longer str()-coerced; a legally-typed but substanceless
#       quote ("the") is rejected by a length floor as the same attack in valid clothing.
#   (b) IDENTITY vs ROW COUNT — delete the currency-input row, duplicate the clean
#       currency-formatter row, keep min_entries: 2 ⇒ the formatter was checked twice, the
#       gate exited 0, and the anchor the gate exists for was free to fabricate. The ledger
#       is now a SET: duplicates rejected, min_entries counts unique identities, and required
#       identities (guard-pinned + `# require:` directives) must each be present.
#   (c) same-pass closures — a protected anchor whose snapshot body is absent is structural
#       (deleting the snapshot must not be cheaper than falsifying the quote), a ledger path
#       must be safe/single-spelled (an identity has one spelling), and the declared
#       `section` must itself occur in the snapshot (it used to be unverified free text).
run_guard "evidence_quote_spotcheck/fixture_protected_entry_int_quote" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_int_quote"
run_guard "evidence_quote_spotcheck/fixture_protected_entry_null_section" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_null_section"
run_guard "evidence_quote_spotcheck/fixture_protected_entry_short_quote" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_entry_short_quote"
run_guard "evidence_quote_spotcheck/fixture_protected_ledger_duplicate_identity" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_ledger_duplicate_identity"
run_guard "evidence_quote_spotcheck/fixture_protected_required_identity_missing" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_required_identity_missing"
run_guard "evidence_quote_spotcheck/fixture_protected_snapshot_missing" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_snapshot_missing"
# flag-independence proof for the same fixture: structural (exit 2) even WITHOUT
# --strict/--strict-templates, where the pre-round-3 guard exited 0.
run_guard "evidence_quote_spotcheck/fixture_protected_snapshot_missing_unflagged" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_snapshot_missing"
run_guard "evidence_quote_spotcheck/fixture_protected_unsafe_path" 2 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_unsafe_path"
run_guard "evidence_quote_spotcheck/fixture_protected_fabricated_section" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_fabricated_section"
# PRD-final-4 C1 (2026-07-30) — PROSE-PRESENCE. The `section` check is FATAL against the
# snapshot body, so snapshot HEADINGS are deliberately authored to carry the section names the
# citing templates declare: heading text is the one part of a snapshot the citer writes. If a
# QUOTE may also resolve against a heading, an author who cannot make a citation match the
# fetched prose just puts the sentence in the table of contents, and "verified against upstream"
# becomes verified against itself. A protected quote must now match at least one NON-heading
# region of the body (TEMPLATE_QUOTE_ONLY_IN_HEADING, exit 1 — same falsified-evidence family as
# a fabricated quote/section). The fixture's snapshot carries the cited sentence ONLY in a `##`
# heading; adding one prose line saying it flips the fixture to exit 0, which is what makes the
# fixture non-vacuous.
run_guard "evidence_quote_spotcheck/fixture_protected_quote_only_in_heading" 1 \
    bash "$SCRIPT_DIR/evidence_quote_spotcheck_guard.sh" --strict --strict-templates --templates-only-protected --root "$SCRIPT_DIR/fixtures/evidence-quote-spotcheck/fail_protected_quote_only_in_heading"

echo ""
echo "[75] catalog_example_symbol_guard.sh (catalog-example/impl-drift — a rule java fence that names a class with no backing .java teaches an agent a broken shape; iterations 2-3 fixed two such drifts by hand with no mechanical backstop. Scans ONLY java fences: a seed-deny fabricated store call (idempotencyStore.computeIfAbsent) and any *StateMachine/*Store symbol must resolve to a real backend/src/main/java symbol OR be named in a catalog-example-ok annotation. Live exits 0; fixtures prove non-vacuity.)"
run_guard "catalog_example_symbol/live" 0 \
    bash "$SCRIPT_DIR/catalog_example_symbol_guard.sh"
run_guard "catalog_example_symbol/fixture_fail" 1 \
    bash "$SCRIPT_DIR/catalog_example_symbol_guard.sh" --root "$SCRIPT_DIR/fixtures/catalog-example-symbol/fail_diverged_symbol"
run_guard "catalog_example_symbol/fixture_pass" 0 \
    bash "$SCRIPT_DIR/catalog_example_symbol_guard.sh" --root "$SCRIPT_DIR/fixtures/catalog-example-symbol/pass_annotated_ok"

echo "[76] agent_block_proof_guard.sh (falsification-test regression guard — backstops the on-disk proofs of the headline thesis 'gates mechanically constrain AI agents', one per enforcement surface: ax-prove-gate-blocks-agent.sh (agent writes a Map-returning @ExceptionHandler → controller_problemdetail_guard BLOCKS → corrects → PASSES; run-all-guards surface) and ax-prove-evidence-gate-blocks-agent.sh (agent writes a placeholder/empty evidence: block → evidence_guard BLOCKS → anchors to a real source → PASSES; pre-commit surface). Each logs block→pass for actor=agent. A proof that silently stops proving is worse than none: this asserts each proof exists, is executable, TOGGLES (run with an isolated AX_LEDGER_DIR so the real ledger is untouched), and is NON-VACUOUS (its 'blocked_rc -ne 1' fail-guard + real-guard reference are present). Live exits 0; fixtures prove non-vacuity.)"
run_guard "agent_block_proof/live" 0 \
    bash "$SCRIPT_DIR/agent_block_proof_guard.sh"
run_guard "agent_block_proof/fixture_fail" 1 \
    bash "$SCRIPT_DIR/agent_block_proof_guard.sh" --root "$SCRIPT_DIR/fixtures/agent-block-proof/fail_vacuous_proof"
run_guard "agent_block_proof/fixture_pass" 0 \
    bash "$SCRIPT_DIR/agent_block_proof_guard.sh" --root "$SCRIPT_DIR/fixtures/agent-block-proof/pass_proof_toggles"
# P3-47: fresh-clone invariant — zero real-ledger agent events must PASS by
# construction (proof writes 2 events to the ISOLATED temp ledger; the real
# ledger is only checked for non-mutation). And the converse: a proof that exits
# 0 while logging ZERO agent events to the temp ledger must FAIL (check (b)).
run_guard "agent_block_proof/fixture_pass_zero_real_ledger" 0 \
    bash "$SCRIPT_DIR/agent_block_proof_guard.sh" --root "$SCRIPT_DIR/fixtures/agent-block-proof/pass_zero_real_ledger"
run_guard "agent_block_proof/fixture_fail_no_agent_events" 1 \
    bash "$SCRIPT_DIR/agent_block_proof_guard.sh" --root "$SCRIPT_DIR/fixtures/agent-block-proof/fail_proof_writes_no_agent_events"

echo "[77] backlog_convergence_integrity_guard.sh (north-star #2 — the BACKLOG convergence rate is the project's redefined end-point, yet nothing read docs/BACKLOG.md, so the 수렴률 table tier counts / 합계 denominator / aggregate % could silently rot or mis-sum. This guard counts '- [x]'/'- [ ]' item IDs per ## P0–P3 section as disk-truth — expanding ranges (P0-1 ~ P0-11), range-plus-extra (P1-14~17 + P1-19), and slash lists; excluding denominator-marked lettered sub-items (P2-1a/b) — and asserts each tier 전체/closed cell, the 합계 == sum of tiers, and aggregate 수렴률 == round(closed/total*100). Live exits 0; fixtures prove non-vacuity.)"
run_guard "backlog_convergence_integrity/live" 0 \
    bash "$SCRIPT_DIR/backlog_convergence_integrity_guard.sh"
run_guard "backlog_convergence_integrity/fixture_fail" 1 \
    bash "$SCRIPT_DIR/backlog_convergence_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/backlog-convergence/fail_denominator_shrink"
run_guard "backlog_convergence_integrity/fixture_pass" 0 \
    bash "$SCRIPT_DIR/backlog_convergence_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/backlog-convergence/pass_consistent"

echo "[78] broadleaf_no_port_guard.sh (Broadleaf-absorption LICENSE safety — Broadleaf is under the Fair Use License v1.0, NOT OSI/permissive; its source must never be PORTED into our implementation tree. Asserts the SHIPPED tree (backend/src + frontend/src + backend/build.gradle.kts + blueprints/) contains zero Broadleaf reference — TRULY case-insensitive (grep -i, catches BROADLEAF), forbidding ported source (import/package org.broadleafcommerce + FUL header), ANY name mention in comments/Javadoc/SQL, AND provider-unique class names (BundleOrderItemImpl etc.). Citations in practices/rules evidence blocks + specs notes are intentional fair-use grounding and are NOT scanned. Live exits 0; fixtures prove non-vacuity.)"
run_guard "broadleaf_no_port/live" 0 \
    bash "$SCRIPT_DIR/broadleaf_no_port_guard.sh"
run_guard "broadleaf_no_port/fixture_fail" 1 \
    bash "$SCRIPT_DIR/broadleaf_no_port_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-no-port/fail_ported"
run_guard "broadleaf_no_port/fixture_pass" 0 \
    bash "$SCRIPT_DIR/broadleaf_no_port_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-no-port/pass_clean"

echo "[79] broadleaf_absorption_parity_guard.sh (Broadleaf-absorption METHODOLOGY completeness + VERIFICATION-GOAL parity — every absorbed vertical MUST carry a complete docs/broadleaf-parity/<vertical>.md record: vertical/broadleaf_source/spec_items/rule/behavioral_test/violation_proof/adversarial_review fields + >=1 verification-goal parity row mapping a Broadleaf test INTENT to our behavioral assertion. Referenced spec items / rule / behavioral_test / ViolationProofTest artifacts are validated to EXIST — a record cannot lie, and (since the 2026-06-26 completeness audit) cannot ship without the methodology-mandated ViolationProofTest. Makes the absorption methodology mechanically enforced with NO exception. Live exits 0; fixtures prove non-vacuity.)"
run_guard "broadleaf_absorption_parity/live" 0 \
    bash "$SCRIPT_DIR/broadleaf_absorption_parity_guard.sh"
run_guard "broadleaf_absorption_parity/fixture_fail" 1 \
    bash "$SCRIPT_DIR/broadleaf_absorption_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-parity/fail_incomplete"
run_guard "broadleaf_absorption_parity/fixture_pass" 0 \
    bash "$SCRIPT_DIR/broadleaf_absorption_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-parity/pass_complete"

echo "[80] broadleaf_module_exhaustion_guard.sh (Broadleaf module-set EXHAUSTION — a BOUNDED two-level sweep; docs/BROADLEAF-COMPLETENESS.md MUST classify EVERY Broadleaf Maven module AND core sub-package with zero silent gaps. Asserts every row (both tables) has a valid classification {ABSORBED,RE-FIND,SKIP,RESIDUE} + non-empty evidence, Maven-table rows == maven_module_count, core-table rows == module_count, RESIDUE rows == residue_count, (live) every RESIDUE has a parity record with no unledgered residue, and (live, DISK-TRUTH when the clone is present) every on-disk built (non-aggregator) Maven module + core/common/profile sub-package has a row (4-grain descent) — so the counts are disk-truthful, not self-asserted. Live exits 0; fixtures prove non-vacuity.)"
run_guard "broadleaf_module_exhaustion/live" 0 \
    bash "$SCRIPT_DIR/broadleaf_module_exhaustion_guard.sh"
run_guard "broadleaf_module_exhaustion/fixture_fail" 1 \
    bash "$SCRIPT_DIR/broadleaf_module_exhaustion_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-exhaustion/fail_unclassified"
run_guard "broadleaf_module_exhaustion/fixture_pass" 0 \
    bash "$SCRIPT_DIR/broadleaf_module_exhaustion_guard.sh" --root "$SCRIPT_DIR/fixtures/broadleaf-exhaustion/pass_complete"

echo "[81] quick_verify_no_audit_guard.sh (the ITERATION-ONLY verify/quick-verify.sh dev-loop helper must NOT be mistakable for the R25 completion gate: it must not write the .ax-verify/runs.jsonl audit log and must not invoke verify-completion.sh — so the pre-push recency guard blocks any push behind quick-verify BY CONSTRUCTION — and must print the ITERATION-ONLY banner. Live exits 0; fixtures prove non-vacuity.)"
run_guard "quick_verify_no_audit/live" 0 \
    bash "$SCRIPT_DIR/quick_verify_no_audit_guard.sh"
run_guard "quick_verify_no_audit/fixture_fail" 1 \
    bash "$SCRIPT_DIR/quick_verify_no_audit_guard.sh" --root "$SCRIPT_DIR/fixtures/quick-verify-no-audit/fail_writes_audit"
run_guard "quick_verify_no_audit/fixture_pass" 0 \
    bash "$SCRIPT_DIR/quick_verify_no_audit_guard.sh" --root "$SCRIPT_DIR/fixtures/quick-verify-no-audit/pass_clean"

echo "[82] fail_fast_blocking_audit_guard.sh (the R25 gate's step-level fail-fast must NOT weaken the no-bypass contract: when a fail_fast step FAILS it must short-circuit AND still write a BLOCKING audit line (exit=1, hard_fail>0, pass=0) so the pre-push recency guard rejects the push; on a clean run it must stay inert and the downstream step runs. Drives the REAL verify-completion.sh in an isolated harness against two committed fixtures. The 'discriminates' invocation proves non-vacuity — the guard catches a regression where fail-fast stops firing.)"
run_guard "fail_fast_blocking_audit/breaks" 0 \
    bash "$SCRIPT_DIR/fail_fast_blocking_audit_guard.sh" --checklist "$SCRIPT_DIR/fixtures/fail-fast-blocking-audit/failfast_breaks.yaml" --expect breaks
run_guard "fail_fast_blocking_audit/clean" 0 \
    bash "$SCRIPT_DIR/fail_fast_blocking_audit_guard.sh" --checklist "$SCRIPT_DIR/fixtures/fail-fast-blocking-audit/clean_runs_all.yaml" --expect clean
run_guard "fail_fast_blocking_audit/discriminates" 1 \
    bash "$SCRIPT_DIR/fail_fast_blocking_audit_guard.sh" --checklist "$SCRIPT_DIR/fixtures/fail-fast-blocking-audit/failfast_breaks.yaml" --expect clean

echo "[83] full_trio_spec_backend_or_exempt_guard.sh (G003 enforcement-coverage — the REVERSE of domain_spec_trio_guard: domain_spec_trio only checks 'every EXISTING domain (L4 dir ∪ backend test task) carries its Trio', never that a full_trio SPEC has a backing backend domain. So a spec can declare domain_mode: full_trio while its invariant is verified at RULE/REVIEW tier only (rule_verification_binding, no runtime backend gate) — weaker than the binary-test domains, and the full_trio claim is dishonest. This guard asserts EVERY full_trio spec is backend-enforced (templates/L4/<base>/ dir OR a per-domain test task whose includeTags cover the spec's item @Tags, mapped ACCURATELY via item-id→test-class→@Tag, not naive base-name) OR honestly listed in ruletier_full_trio_allowlist.yaml with a rationale; a STALE exemption naming a now-enforced spec also BLOCKS. Live exits 0; fixtures prove non-vacuity.)"
run_guard "full_trio_spec_backend_or_exempt/live" 0 \
    bash "$SCRIPT_DIR/full_trio_spec_backend_or_exempt_guard.sh"
run_guard "full_trio_spec_backend_or_exempt/fixture_fail" 1 \
    bash "$SCRIPT_DIR/full_trio_spec_backend_or_exempt_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/full-trio-backend-or-exempt/fail_no_enforcement_no_exempt"
run_guard "full_trio_spec_backend_or_exempt/fixture_pass" 0 \
    bash "$SCRIPT_DIR/full_trio_spec_backend_or_exempt_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/full-trio-backend-or-exempt/pass_exempted"

echo "[84] vacuity_class_proof_guard.sh (the MECHANICAL non-vacuity gate — this session's adversarial reviews kept catching green-but-hollow tests by hand: a gate that, if deleted/flipped, fails NO test. This makes the catch mechanical via PIT mutation testing. Every spec item that declares a non-vacuity contract {vacuity_class + gate_method + kill_mutator} is kill-proofed: (a) gate_method resolves to a real class+method, (b) the kill_mutator is consistent with the vacuity_class (fail_closed_default ⇒ TRUE_RETURNS|FALSE_RETURNS per METHODOLOGY), and (c) a SCOPED ./gradlew pitest run mutates that one method with that mutator and the catalog *ViolationProofTest MUST KILL it — SURVIVED/NO_COVERAGE ⇒ hollow ⇒ BLOCK. Live runs real PIT against the tokenized-securities fail-closed SPIs {OwnershipHolderAuthorization#controls, AllowlistInvestorEligibility#isEligible} and asserts KILLED; the offline pass/fail fixtures {hollow SURVIVED → 1, tight KILLED → 0} prove the assertion is non-vacuous.)"
run_guard "vacuity_class_proof/live" 0 \
    bash "$SCRIPT_DIR/vacuity_class_proof_guard.sh"
run_guard "vacuity_class_proof/fixture_hollow" 1 \
    bash "$SCRIPT_DIR/vacuity_class_proof_guard.sh" --report "$SCRIPT_DIR/fixtures/vacuity-class-proof/hollow_survived.xml" --gate-method com.example.Foo#bar --kill-mutator TRUE_RETURNS --vacuity-class fail_closed_default
run_guard "vacuity_class_proof/fixture_tight" 0 \
    bash "$SCRIPT_DIR/vacuity_class_proof_guard.sh" --report "$SCRIPT_DIR/fixtures/vacuity-class-proof/tight_killed.xml" --gate-method com.example.Foo#bar --kill-mutator TRUE_RETURNS --vacuity-class fail_closed_default

echo "[85] vacuity_guard_selfproof_guard.sh (anti-meta-trap — a guard that catches hollow tests can itself rot into a hollow guard {the same trap}. This self-proof keeps vacuity_class_proof_guard.sh honest, offline, on committed fixtures: it asserts the vacuity guard STILL FAILS on the bundled hollow {SURVIVED} fixture, STILL PASSES on the tight {KILLED} fixture {so the failure is discriminating, not a constant blocker}, and that its source still carries the SURVIVED/non-KILLED → exit 1 blocking branch. Live exits 0.)"
run_guard "vacuity_guard_selfproof/live" 0 \
    bash "$SCRIPT_DIR/vacuity_guard_selfproof_guard.sh"

echo "[86] private_boundary_guard.sh (R26 강제 — ax-template public base에 fork-receiver 특화·민감 정보 유입을 기계적으로 차단. 두 층: 층1 opt-in marker {.ax-private-markers 활성 ERE 패턴, case-insensitive, public base 0-match} + 층2 generic 시크릿 {PEM key / AWS AKIA / API-key / JWT, 모든 토큰 순회 allowlist + src/test/ 제외 + pragma: allow-secret은 docs/ 또는 practices/rules/ 하위 경로만 유효 — bare *.md도 코드 트리는 무시}. 스캔: backend/src frontend/src specs contracts blueprints practices/rules docs README.md CLAUDE.md .github. P2-15 신설: 층1이 커밋 메시지도 스캔 — (a) --commit-msg-file 모드 {.githooks/commit-msg 훅이 커밋마다 호출, 메시지 텍스트만 스캔} + (b) 인자 없는 기본 실행에서 REPO_ROOT가 git repo면 HEAD 커밋 메시지를 advisory backstop으로 재스캔. 한계(honest): git 히스토리(HEAD 이전)·바이너리·인코딩 시크릿은 스캔 범위 밖; 층2는 커밋 메시지에 적용 안 함. 비공허성 fixture 8종: fail_marker→exit 1 / fail_secret→exit 1 / fail_secret_incidental→exit 1 (C1: value-only allowlist) / pass_clean→exit 0 / pass_pragma_doc→exit 0 (pragma in docs/) / fail_secret_multitoken→exit 1 (N1: 동일 라인 placeholder+real 토큰 순회, differential old=0/new=1) / fail_pragma_in_code→exit 1 (n1: .java + .md in backend/src — pragma 무시, f1: *.md 접미사로 pragma 활성화 불가) / fail_commit_msg→exit 1 (P2-15: --repo-root+--commit-msg-file 조합, 커밋 메시지에만 있고 트리엔 없는 마커도 차단) + 같은 fixture의 clean 메시지→exit 0. Live exits 0.)"
run_guard "private_boundary/live" 0 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh"
run_guard "private_boundary/fixture_marker" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_marker"
run_guard "private_boundary/fixture_secret" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_secret"
run_guard "private_boundary/fixture_clean" 0 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/pass_clean"
run_guard "private_boundary/fixture_incidental" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_secret_incidental"
run_guard "private_boundary/fixture_pragma_doc" 0 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/pass_pragma_doc"
run_guard "private_boundary/fixture_multitoken" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_secret_multitoken"
run_guard "private_boundary/fixture_pragma_in_code" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_pragma_in_code"
run_guard "private_boundary/fixture_commit_msg" 1 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_commit_msg" --commit-msg-file "$SCRIPT_DIR/fixtures/private-boundary/fail_commit_msg/COMMIT_MSG"
run_guard "private_boundary/fixture_commit_msg_clean" 0 \
    bash "$SCRIPT_DIR/private_boundary_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/private-boundary/fail_commit_msg" --commit-msg-file "$SCRIPT_DIR/fixtures/private-boundary/fail_commit_msg/COMMIT_MSG_CLEAN"

echo "[87] fixture_kill_proof_guard.sh (shell-guard판 PIT — fail fixture non-vacuity를 mutation으로 기계 증명. 사람이 적발한 vacuous fixture {private_boundary fail_secret_multitoken의 N1-proof가 pre-fix guard로도 exit 1이어서 실제로 N1 로직을 증명하지 못함}를 기계화. fixture_kill_manifest.yaml에 등재된 모든 fail fixture{private_boundary 6 + completion_checklist_recency 1}: original guard → exit 1, anchor neuter → exit 0. anchor는 guard 소스에 정확히 1회 존재해야 함 {0/2+회 → stale → BLOCK}. P2-14: neuter는 6-shape allowlist(no-op/sentinel-string/condition-constant/truncation/over-broad-glob/variable-substitution)에 매칭해야 하고 exit/return/kill 같은 control-flow escape 토큰은 즉시 BLOCK — \`exit 0\` short-circuit neuter 등재 시점 차단. 자기 비공허성 fixture 3쌍: vacuous_manifest {fail_marker + 오·neuter → exit1 유지 → meta-gate exit 1} + nonvacuous_manifest {fail_marker + 올바른 neuter → exit0로 flip → meta-gate exit 0} + vacuous_shortcircuit_manifest {neuter='exit 0' → 어휘검증 단계에서 REJECT → meta-gate exit 1}. P2-50 (2026-07-30): registry integrity runs BEFORE any mutation — duplicate id, duplicate (guard,fixture) under a fresh id, and a min_items floor declared in the manifest AND pinned in the guard as LIVE_MIN_ITEMS so emptying the gate takes two coordinated edits — P3-104: the number is stated ONLY at those two constants, never restated in prose that can rot away from them (it previously read 57 in three comments while the directive enforced 62); 2026-07-30 PRD-final-4 W5b raised the pair 62 → 64 for the two new manifest_snapshot_integrity kill-proofs. A count is not an identity: without duplicate rejection, deleting the item that matters and re-adding a cheap already-proven pair keeps the floor satisfied while the proof stays gone. Three self-proof manifests dup_id/dup_proof/floor_breach each exit 1. Live exits 0.)"
run_guard "fixture_kill_proof/live" 0 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh"
run_guard "fixture_kill_proof/fixture_fail" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/vacuous_manifest.yaml"
run_guard "fixture_kill_proof/fixture_pass" 0 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/nonvacuous_manifest.yaml"
run_guard "fixture_kill_proof/fixture_shortcircuit_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/vacuous_shortcircuit_manifest.yaml"
# P2-19: three additional bypass self-proofs, one per escape class the vocabulary
# check (P2-14) must reject — exec builtin, command substitution inside a quoted
# literal, and a non-terminal mid-pipeline `| head`. Each must BLOCK (exit 1).
run_guard "fixture_kill_proof/fixture_exec_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/exec_true_neuter_manifest.yaml"
run_guard "fixture_kill_proof/fixture_cmdsubst_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/cmdsubst_neuter_manifest.yaml"
run_guard "fixture_kill_proof/fixture_midpipe_head_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/midpipe_head_neuter_manifest.yaml"
run_guard "fixture_kill_proof/fixture_dup_id_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/dup_id_manifest.yaml"
run_guard "fixture_kill_proof/fixture_dup_proof_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/dup_proof_manifest.yaml"
run_guard "fixture_kill_proof/fixture_floor_breach_reject" 1 \
    bash "$SCRIPT_DIR/fixture_kill_proof_guard.sh" --manifest "$SCRIPT_DIR/fixtures/fixture-kill-proof/floor_breach_manifest.yaml"

echo "[88] pre_push_decision_guard.sh (P2-17 — behavior-tests the pre-push hook's 10 decision branches + primary R25 block against throwaway git repos with a STUB recency guard + STUB gradlew; asserts exit code AND which stages fired per scenario, 1:1 with the committed fixtures/pre-push-decision/scenarios.yaml table. Pure decision logic extracted to .githooks/pre-push-lib.sh {sourced by the hook; refactor proven byte-identical vs original across 8 scenario runs}. Non-vacuity is INTERNAL: scenario_selfproof_nonvacuous mutates the lib and requires a scenario regression every sweep — fixture_kill_manifest deliberately NOT used {the [87] model mutates a guard and reruns a fixture dir; here the subject-under-test is the hook/lib, which the manifest schema cannot express}. Live exits 0.)"
run_guard "pre_push_decision/live" 0 \
    bash "$SCRIPT_DIR/pre_push_decision_guard.sh"

echo "[89] full_trio_artifact_completeness_guard.sh (P2-22 — FRONTEND-axis obverse of full_trio_spec_backend_or_exempt: every spec declaring domain_mode: full_trio MUST own its frontend Trio on disk {contracts/<stem>-*.yaml + blueprints/<stem>-*.yaml + templates/L4/<stem>/}. METHODOLOGY.md defines full_trio = Backend Trio AND Frontend Trio REQUIRED; before this guard NO surface checked domain_mode-vs-artifact, so 29 cross-cutting specs sat on full_trio while owning zero frontend artifacts {reclassified to backend_only in the same pass}. Zero-scan FAILs {non-vacuity — a rename of the field can't make the gate vacuous}. Live scans 21 full_trio stems, exits 0.)"
run_guard "full_trio_artifact_completeness/live" 0 \
    bash "$SCRIPT_DIR/full_trio_artifact_completeness_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "full_trio_artifact_completeness/fixture_pass" 0 \
        bash "$SCRIPT_DIR/full_trio_artifact_completeness_guard.sh" --root "$SCRIPT_DIR/fixtures/full_trio_artifact_completeness/pass"
    run_guard "full_trio_artifact_completeness/fixture_fail_missing" 1 \
        bash "$SCRIPT_DIR/full_trio_artifact_completeness_guard.sh" --root "$SCRIPT_DIR/fixtures/full_trio_artifact_completeness/fail_missing_artifacts"
    # P1-2 (cross-family review, xhigh) — os.path.isdir() alone was vacuous: an
    # empty/.gitkeep-only templates/L4/<stem>/ dir satisfied the old check. This
    # fixture (and the guard's fix) closes that hole; note the "pass" fixture
    # above was ITSELF the vacuous case until this fix (an isdir()-only widget/
    # .gitkeep) and now ships a real widget/app/page.tsx instead.
    run_guard "full_trio_artifact_completeness/fixture_fail_l4_empty_directory" 1 \
        bash "$SCRIPT_DIR/full_trio_artifact_completeness_guard.sh" --root "$SCRIPT_DIR/fixtures/full_trio_artifact_completeness/fail_l4_empty_directory"
fi

echo "[90] admin_preauthorize_guard.sh (iter2-G1 BFLA closure; binds practices/rules/bfla-privileged-endpoint-authz-presence.md's verification.guard). PURELY-LOCAL static LINT: every REQUIRED mutating admin endpoint MUST carry an effective method-/class-level @PreAuthorize requiring ROLE_ADMIN. SecurityConfig is NOT parsed (that bypass class is MOOT — no config chain to model) and adversarial SpEL evaluation is OUT OF LINT SCOPE (authoritative = domain 403 integration tests + SecurityConfig). Round-4 codex convergence: @PostAuthorize does NOT gate a mutation (Fix 1); FQN/multiline mappings + non-public handlers are scanned (Fix 2); obviously-ineffective SpEL (negation · trivial always-true disjunction) is rejected (Fix 3); admin-surface detection WIDENED to method-level /api/admin mappings (Fix 4). Round-5 codex: mapping-path extraction now reads an explicit path=/value= attribute wherever it appears, instead of the first quoted string, which misread e.g. @PostMapping(produces = \"...\", path = \"/api/admin/x\") and silently dropped the endpoint from detection (Fix 5). Round-6 codex: mapping-path extraction now returns the FULL LIST of paths, not a single scalar — a mapping annotation legally accepts an ARRAY of paths (value = {\"/public\", \"/api/admin/missed\"}), and an admin path buried as a non-first array element was silently dropped from BOTH consumers (Fix 6). Round-7 codex (Fix 7): the extraction/composition surface closed in ONE principled pass — top-level attribute tokenization so a non-path attribute's quoted content can't impersonate a path (F1); quote-aware array brace balancing so a URI-template {id} doesn't break the scan (F2); same-file String-constant + literal-concat fold (F3-simple); class×method path composition in BOTH consumers (F4); optional-leading-slash normalization (F5); class-level FQN mapping recognition (F7). The undecidable/adversarial tail — fixed path-pattern obfuscation {scope:admin} (F6), imported/opaque constants + \${...}/#{...} placeholders + Unicode/octal escapes + text blocks (F3-tail), inner-dot-whitespace FQN (F7-tail) — is DOCUMENTED out-of-scope in the guard header and deferred to the domain 403 *ComplianceTests + runtime /api/admin/** matcher.)"
run_guard "admin_preauthorize/live" 0 \
    bash "$SCRIPT_DIR/admin_preauthorize_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # Method-@PreAuthorize-based falsification set (no SecurityConfig fixtures —
    # the guard no longer reads config). pass exits 0; every fail_* exits 1.
    run_guard "admin_preauthorize/fixture_pass" 0 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/pass"
    # No @PreAuthorize anywhere on a mutating admin endpoint → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_missing_preauthorize" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_missing_preauthorize"
    # Method-level permitAll() overrides the class-level ROLE_ADMIN → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_permitall" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_permitall"
    # @PreAuthorize("isAuthenticated()") is present but not admin-requiring → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_authenticated_only" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_authenticated_only"
    # @PatchMapping (mutating) with no authz, on a class detected as admin-surface
    # via its @RequestMapping(path = "/api/admin/...") ALIAS (NOT *AdminController
    # name) → BLOCK. Proves PATCH-verb coverage + path-based detection at once.
    run_guard "admin_preauthorize/fixture_fail_patch_missing" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_patch_missing"
    # Round-4 codex convergence falsifiers.
    # Fix 1 — @PostAuthorize runs AFTER the mutation's side effect; it does NOT
    # gate a mutation. A POST protected only by @PostAuthorize → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_postauthorize_only" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_postauthorize_only"
    # Fix 2 — a FULLY-QUALIFIED @org...PostMapping must be scanned, not invisible.
    run_guard "admin_preauthorize/fixture_fail_fully_qualified_mapping" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_fully_qualified_mapping"
    # Fix 3a — a leading negation of the admin predicate (!hasAuthority('ROLE_ADMIN'))
    # inverts the gate → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_negated_spel" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_negated_spel"
    # Fix 3b — a trivial always-true disjunction ("hasAuthority('ROLE_ADMIN') or true")
    # short-circuits to always-true → BLOCK.
    run_guard "admin_preauthorize/fixture_fail_disjunction_true" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_disjunction_true"
    # Fix 5 (round-5 codex) — an attribute-ordered mapping
    # (`@PostMapping(produces = "...", path = "/api/admin/x")`) with NO
    # @PreAuthorize at all. The OLD guard took the FIRST quoted string in the
    # annotation as the path, misread "application/json" as the path, and
    # silently did not require this endpoint to carry authz → false pass.
    run_guard "admin_preauthorize/fixture_fail_attr_ordered_mapping" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_attr_ordered_mapping"
    # Fix 6 (round-6 codex) — an ARRAY-valued path=/value= attribute
    # (`@PostMapping(value = {"/public", "/api/admin/missed"})`) with the admin
    # path as a NON-FIRST array element, and NO @PreAuthorize at all. The
    # round-5 extractor only matched a scalar `"..."` immediately after `=` and
    # returned "" for an array value, silently dropping this endpoint from BOTH
    # the admin-surface detector and the per-endpoint requirement check →
    # false pass.
    run_guard "admin_preauthorize/fixture_fail_array_valued_path" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_array_valued_path"
    # Round-7 codex (Fix 7) — the mapping-path extraction/composition surface,
    # closed in ONE principled pass. Each fixture is an ungated admin mutation
    # in the named shape that the round-6 extractor silently DROPPED (proven a
    # genuine RED-on-revert: the pre-fix guard exited 0 on each) and the round-7
    # guard now BLOCKS (exit 1). See the guard header "Mapping-path extraction —
    # decidable scope vs out-of-scope tail".
    # F1 — a NON-path attribute's quoted content impersonates path syntax
    # (`@PostMapping(name = "path={}", path = "/api/admin/x")`); tokenizing
    # top-level attributes reads the REAL path.
    run_guard "admin_preauthorize/fixture_fail_attr_impersonation" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_attr_impersonation"
    # F2 — a URI-template `{id}` brace inside an array element broke the
    # non-quote-aware `{...}` scan; quote-aware brace balancing extracts every
    # element (`value = {"/public", "/api/admin/{id}/rotate", "/other"}`).
    run_guard "admin_preauthorize/fixture_fail_uri_template_array" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_uri_template_array"
    # F3-simple — a same-file `static final String` constant + literal concat
    # (`ADMIN_BASE + "/x"`) is constant-folded to `/api/admin/x`.
    run_guard "admin_preauthorize/fixture_fail_samefile_constant" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_samefile_constant"
    # F3-simple (round-8 codex constant-CHAIN closure) — a same-file constant
    # whose OWN initializer is an expression referencing ANOTHER same-file
    # constant (`static final String API = "/api"; static final String ADMIN =
    # API + "/admin";`) was never entered into the constant map pre-fix, so
    # `@PostMapping(ADMIN + "/x")` silently extracted no path. Fixed-point
    # resolution now folds ADMIN -> "/api/admin" first, then ADMIN + "/x" ->
    # "/api/admin/x" (proven a genuine RED-on-revert: the pre-fix guard exited
    # 0 on this fixture).
    run_guard "admin_preauthorize/fixture_fail_constant_chain" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_constant_chain"
    # F4 — class-level + method-level path COMPOSITION: class `@RequestMapping("/api")`
    # × method `@PostMapping("/admin/x")` → effective `/api/admin/x`.
    run_guard "admin_preauthorize/fixture_fail_class_method_composition" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_class_method_composition"
    # F5 — Spring's optional leading slash normalized (`@PostMapping("api/admin/x")`).
    run_guard "admin_preauthorize/fixture_fail_leading_slash" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_leading_slash"
    # F7 — class-level FQN mapping recognized with the same recognizer as method
    # mappings (`@org.springframework.web.bind.annotation.RequestMapping("/api/admin")`).
    run_guard "admin_preauthorize/fixture_fail_classlevel_fqn" 1 \
        bash "$SCRIPT_DIR/admin_preauthorize_guard.sh" --root "$SCRIPT_DIR/fixtures/admin-preauthorize/fail_classlevel_fqn"
fi

echo "[91] locale_aware_format_guard.sh (wave-1 exit cleanup — was shipped alongside practices-react/rules/locale-aware-number-date-format.md but never wired into run-all-guards.sh; iter1-G2 / CANARY-001 closure)"
run_guard "locale_aware_format/live" 0 \
    bash "$SCRIPT_DIR/locale_aware_format_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "locale_aware_format/fixture_pass" 0 \
        bash "$SCRIPT_DIR/locale_aware_format_guard.sh" --root "$SCRIPT_DIR/fixtures/locale-aware-format/pass_intl_format"
    # Per-detector fixtures: each isolates ONE forbidden pattern in its own
    # file so deleting any single detector greens exactly that fixture
    # (proper per-detector falsification — wave-1 codex broadening).
    run_guard "locale_aware_format/fixture_fail_manual_date" 1 \
        bash "$SCRIPT_DIR/locale_aware_format_guard.sh" --root "$SCRIPT_DIR/fixtures/locale-aware-format/fail_manual_format"
    run_guard "locale_aware_format/fixture_fail_bare_tolocale" 1 \
        bash "$SCRIPT_DIR/locale_aware_format_guard.sh" --root "$SCRIPT_DIR/fixtures/locale-aware-format/fail_bare_tolocale"
    run_guard "locale_aware_format/fixture_fail_money_tofixed" 1 \
        bash "$SCRIPT_DIR/locale_aware_format_guard.sh" --root "$SCRIPT_DIR/fixtures/locale-aware-format/fail_money_tofixed"
    run_guard "locale_aware_format/fixture_fail_currency_concat" 1 \
        bash "$SCRIPT_DIR/locale_aware_format_guard.sh" --root "$SCRIPT_DIR/fixtures/locale-aware-format/fail_currency_concat"
fi

echo "[92] contract_enum_parity_guard.sh (P2-33 — contract↔code enum parity, EXHAUSTIVE BY CONSTRUCTION. Every enum: block under contracts/*.yaml {57 today across 20 files} MUST be classified in practices/evals/contract-enum-map.yaml as either java_enum: <FQCN> {constant sets must match} or wire_only: <reason> + a mandatory wire_source: producer declaration {P0-2, see below}; an UNCLASSIFIED block FAILS and a STALE entry {pointing at a block no longer on disk} FAILS — there is no name inference, which is what makes the gate non-heuristic. Modifiers wire_extra / wire_missing / wire_case are non-redundancy-checked, so an allowance cannot rot into a lie. Java constants are extracted by parse {no JVM} across the three shapes in this tree: plain-with-javadoc, constructor-arg {reportexport.ExportFormat:12-13}, nested-in-type {WebhookDelivery.java.skeleton:38} — pass_clean binds all three, so a regression in any shape flips it RED. vocab_scan entries cover the L4 surfaces the contract schema cannot express {P2-34: the webhook fork-copy carried a NON-ISOMORPHIC 5-value vocabulary}; matching is word-boundary so SUCCESS does not match inside SUCCEEDED. Every vocab_scan carries a MANDATORY declaration: block {ts_union | java_enum_decl | marker_region} and the DECLARED token set is compared to canonical by EXACT SET EQUALITY — an added UNKNOWN token FAILS even though no forbidden: entry names it {reviewer finding: the old scan was a finite denylist, so appending | 'BOUNCED' to the TS union passed; fixture_fail_added_token now pins that RED}. A vocab_scan with no declaration: FAILS, so no surface can escape the exhaustive path. HONEST SCOPE: ts_union / java_enum_decl are exhaustive over the whole declaration; the two PROSE surfaces {providers.tsx JSDoc, README.md} are exhaustive ONLY inside the vocab:delivery-status:start/end region — whole-file set equality is unsound there {the README carries 76 unrelated ALL-CAPS tokens} and is NOT claimed; outside the region the floor is the denylist only. Birth census found 5 REAL drifts, all fixed contract→code: billing BillingEventType {wire promised 4 values the server cannot emit AND forbade UNHANDLED which it does emit}, payment PaymentState {FAILED reachable from CREATED via PROVIDER_DECLINE but absent from the contract}, report-export ExportJobResponse.format {lower-case on a response that serializes the enum via name()}, scheduled-task ×2 {ACTIVE/PAUSED vs the shipped ENABLED/DISABLED}, tokenized-securities securityType {EQUITY/BOND vs the shipped INVESTMENT_CONTRACT}. P0-2 {cross-family review, xhigh — FALSE CLOSURE}: wire_only used to be CLASSIFICATION-ONLY — the guard checked a non-empty reason: existed and continued, so ~a quarter of the contract's enum blocks could drift freely {reviewer reproduction: flip RateLimitPingController's Map.of{status, ok} to healthy — contract, guard and testRateLimit all stayed green because RateLimitComplianceTest asserts status+headers, not the body}. Every wire_only entry now carries a MANDATORY wire_source: naming WHERE the literals are produced, and the extracted literal set is compared to the block by EXACT SET EQUALITY {wire_extra/wire_missing allowances apply, non-redundancy-checked, through the SAME helper as java_enum}. Four extracting kinds — java_field_literals {a named field's initializer literals, or every static final String in the file}, java_enum_decl {a copy-target skeleton's enum, e.g. templates/backend/billing/Invoice.java for a block the reference workload has no aggregate for}, java_method_returns {every return literal of an SPI method body across a glob — an abstract declaration has no body so the interface never widens the set}, literal_pattern {one-capture regex; comments are stripped first for .java/.ts/.tsx so a javadoc example cannot fake a producer} — plus ONE absence kind, unproduced, for a block nothing in this tree produces {the PaymentCallbackVerifier SPI ships zero implementations; payment's sortBy was the other one until round 4 deleted the parameter — see below}. unproduced is not free: it requires absence_probes whose patterns must match ZERO files over a NON-EMPTY file set, so the exemption SELF-DESTRUCTS the moment a producer appears. P0-2 ROUND 2 {same reviewer, re-review — the rewrite MOVED the hole rather than closing it, because static regex extraction over java source is NOT SOUND: it can silently extract the WRONG literals and report PASS}. Two escapes, both closed by replacing text heuristics with FAIL-CLOSED extraction plus RUNTIME/BYTECODE truth. ESCAPE 1 {literal_pattern reads the wrong endpoint}: writing `Map.of{status, healthy.toString{}}` in RateLimitPingController.ping{} is not a plain literal, so the pattern SKIPPED it and captured ok from the UNRELATED /anon/ping method — the endpoint emitted healthy and the guard said PASS {RateLimitComplianceTest asserts only status codes and headers}. Now every value-extracting kind REFUSES TO GUESS: literal_pattern requires a MANDATORY residue_probe naming the producing construct, which must fire at least once in the source AND zero times after every pattern capture is deleted {so every occurrence of the construct was consumed by a plain literal}; java_field_literals requires the addressed initializer {resp. every static final String} to be literals+collection-syntax only; java_method_returns requires EVERY return in a matched body to be a plain literal. Anything else ERRORS with cannot-prove-the-literal-set. ESCAPE 2 {unproduced absence proof misses ordinary valid implementations}: the probe {implements|extends|new}\\s+PaymentCallbackVerifier requires the interface name IMMEDIATELY after implements, so `implements java.io.Serializable, PaymentCallbackVerifier` — an entirely ordinary declaration Spring registers normally — defeated it, and no tightened regex closes the class {generics, nested, anonymous, @Bean factory}. unproduced now ALSO requires verified_by {test, tag, gradle_task, anchors}: a runtime/bytecode proof the guard checks EXISTS, carries the tag, contains the declared anchors, and is wired into a per-domain gradle task R25 runs. The two proofs shipped: RateLimitPingWireVocabularyTest drives BOTH probes over live HTTP and compares the emitted value SET to the contract block parsed from disk by its JSON pointer {precedent: PageEnvelopeCatalogSweepTest}; PaymentContractAbsenceProofTest asserts the SPI absence via ArchUnit over compiled main sources {beAssignableTo, so declaration shape is irrelevant} AND via the autowired registry being empty {so a @Bean-contributed producer is caught too}, plus, against the RUNNING RequestMappingHandlerMapping's @RequestParam bindings, the sortBy absence {round 4 generalised this claim — see below}. Both mutations verified RED then reverted GREEN. ZERO_RUNTIME_VERIFIED joins the non-vacuity floor: with wire_only entries present, at least one must resolve a verified_by, so the runtime layer cannot be quietly dropped back to source regexes. An extraction yielding the EMPTY set FAILS {a pattern matching nothing must never read as no-drift}. The P0-2 census also RECLASSIFIED auth UserRole / VerificationState from wire_only to real java_enum bindings {wire_case: lower}. Zero blocks / zero java bindings / zero vocab_scans / zero structurally-exhaustive declarations / zero extracting wire_sources each FAIL. P0-2 ROUND 3 {same reviewer, third pass — round 2's fixes CONFIRMED holding, two BROADER classes found, both ordinary-accident shapes rather than adversarial ones}. CLASS A {the binding itself was an unchecked assertion}: java_enum: <FQCN> was resolved and compared, but NOTHING corroborated that the FQCN is the enum serving this contract — repoint the SessionStatus entry at apikey.ApiKeyStatus {both {ACTIVE, REVOKED}} and add EXPIRED to SessionStatus, and the guard compares the contract to the UNRELATED enum, never looks at SessionStatus again, and exits 0. One copy-paste redirects the whole check. Now the binding must be coherent with the DOMAIN ON DISK: the contract file stem {minus -openapi / -ui} must equal the java package leaf, two facts the manifest author cannot make equal by writing them. Genuine cross-domain reuse is allowlisted IN THE GUARD, not in the manifest {auth↔user, data-subject-rights↔dsr}, so widening it is a reviewed edit to the gate rather than one more line a stray copy-paste carries along — and each pair is non-redundancy-checked, so an unused allowance FAILS like a stale wire_extra. Weighed and rejected: at-most-one-block-per-enum is false here by design {ApiKeyScope backs 3 blocks, OAuthProvider 6} and domain coherence subsumes its useful part; DTO-field corroboration is stronger but inapplicable to path/query parameters and to schemas no shipped operation serves, so it would need a per-entry escape hatch — author assertion again. Residual, stated honestly: two enums in the SAME package with identical constant sets could still be swapped for each other. CLASS B {residue_probe proves ONE construct, not the construct CLASS}: rewriting the producing method as `return Collections.singletonMap{status, System.getProperty{…}}` matches neither pattern nor the `Map.of{status,` probe, while the sibling anonPing keeps ok — so the file-wide probe fires, the file-wide residue is empty, and the guard reports PASS on a payload the endpoint no longer emits. The scope is now the PRODUCING METHOD BODY, declared per entry {producer_scope: {kind: method_bodies, methods: [ping, anonPing]}}: extraction AND residue are body-scoped, so a sibling's literal can no longer stand in for the producer's own; every probe occurrence in each body must be consumed; when the captured literal IS the returned value {derived from the source, not declared — so an acceptance chain like fmt.equals{csv} inside an if is not held to it} EVERY return in EVERY declared body must be fully consumed as well, which is what turns a foreign map factory, a helper call, a ternary, a constant reference and a multi-line build into cannot-prove-the-literal-set; and a producer-shaped occurrence OUTSIDE the declared bodies is residue too, so a new sibling producer must be declared instead of silently widening the vocabulary. file_wide scope survives ONLY for a non-java producer {the MSW handler's object-literal property} and requires a reason — it is REFUSED for any .java file, so the weaker scope cannot be re-selected where method bodies are delimitable. And because `methods:` would otherwise be one more author assertion — omitting the real handler restores the identical escape — the declaration must cover EVERY request-mapped method in the producer file{s}: the handler set is a fact on disk, so dropping anonPing from the ratelimit entry now FAILS. All six evasion shapes plus the declaration-shrink shape verified BLOCK then reverted GREEN. Honest scope: this proves the DECLARED producers are literal-only and complete over the producing construct; that the declared set is EVERY method serving the block is not statically decidable here and is what the live-HTTP verified_by backstops. NOT COVERED, deliberately {registered as a documented limitation, not a gap left by accident}: a property-conditional @Bean returning a JDK Proxy that implements the SPI only under a configuration no test sets — deliberate evasion, for which no absence proof over an unexercised configuration can exist. P0-2 ROUND 4 {same reviewer, fourth pass — rounds 1-3 CONFIRMED holding; one REAL accidental-realistic hole left, requiring no knowledge of the guard to trip}. The unproduced branch verified producer ABSENCE and then RETURNED without EVER comparing the contract's declared tokens to any canonical set, so absence-of-producer was silently treated as if it discharged vocabulary-correctness too — two separate obligations, one checked. Both reproductions were CONTRACT-ONLY edits with no code change and both exited 0: the callback slug mock → accidental_typo, and the sortBy vocabulary → [accidentalTypo]. payment_provider_type_enum_guard is NOT a backstop — it reads blueprints/payment-manifest.yaml alone and never looks at the contract. This falsified the published claim that the surface blocks ACCIDENTAL contract drift, so the guarantee was a lie for exactly the entries with no producer to corroborate them. Every unproduced entry must now ALSO carry canonical_vocabulary: {kind: yaml_list, file, pointer} naming an INDEPENDENT source that legislates the legal token set, and the block must EQUAL it exactly. file: may not be the contract itself {a block compared to its own file is vacuous}, the list must be non-empty and flat {a nested member ERRORS rather than being flattened}, and wire_extra/wire_missing are REFUSED here — everywhere else an allowance is corroborated by a producer that really does or really cannot emit the token, and on this path there is nothing to corroborate it. DISPOSAL of the two live entries. The callback {provider} slug is bound to blueprints/payment-manifest.yaml#provider.type_allowed — the mirror the contract's own prose already declared {MIRROR of … the manifest value is the source of truth} but that nothing enforced in the contract direction; it is now mechanical BOTH ways, so editing either side alone BLOCKS. The sortBy parameter was DELETED from the contract rather than re-exempted: P0-2 found it advertised but UNBOUND {PaymentController#list declares only page/size and hard-codes Sort.by{DESC, createdAt}}, so a client sending sortBy=amount got createdAt ordering with a 200 and no signal — a closed vocabulary nobody legislated and nobody enforced, for which no canonical source can honestly exist. PaymentContractAbsenceProofTest's second claim was correspondingly strengthened from 'no handler binds sortBy' to the general invariant 'EVERY query parameter GET /payments still advertises IS bound by the running handler', asserted against the live RequestMappingHandlerMapping, so an unimplemented parameter cannot come back. Two CONTRACT-ONLY fixtures pin the closure {no code moves in either — the java sources are byte-identical to pass_clean}: fail_unproduced_no_canonical {the entry names no canonical source} and fail_unproduced_contract_vocab_drift {outbound → accidental_typo in the contract alone, absence proof and runtime binding still perfectly intact}. Both verified BLOCK then reverted GREEN, and both are [87] kill-proofed. P3-87 {2026-07-29} CLOSED that residual: same-package same-constant-set enum pairs are indexed and an ambiguous java_enum binding is REFUSED unless it declares enum_disambiguation {dto_reference, reason} — a file on disk naming this enum's simple name. Non-redundancy-checked both ways. No such pair exists today, so the fixtures construct one. P0-2 ROUND 5 {same reviewer, fifth pass — rounds 1-4 CONFIRMED holding; NO P0 and exactly ONE P1, again an accidental-realistic shape rather than an adversarial one}. The vocab_scan CANONICAL side was an INLINE LITERAL repeated once per surface: all four L4 webhook entries wrote [PENDING, PENDING_RETRY, SUCCEEDED, FAILED_PERMANENT] independently, so the vocabulary existed in FOUR copies {WebhookDeliveryStatus, the two webhook contract blocks, and the four manifest lists} and only the first three were mechanically tied to each other. Reproduction, which is simply how a vocabulary grows: add DELIVERING to WebhookDeliveryStatus AND to both webhook contract enums — so the contract_enum entries stay green — and forget the four fork-copy templates. Exit 0, with every template shipping a vocabulary the API no longer speaks; the guard was comparing the templates to a manifest literal that had gone stale in the same edit. `canonical:` is now REFUSED and every vocab_scan carries a MANDATORY canonical_from: {kind: java_enum fqcn: … | kind: contract_enum contract: … pointer: …} — the token set is RESOLVED from the thing that moves. A java_enum source must ALSO be bound to at least one contract enum block in this manifest {a fork-copy speaks the WIRE vocabulary, so an internal-only enum is not a legal source}, and BOTH kinds are domain-coherence-checked the way a java_enum binding is {the source's domain must be spelled by a path segment of the scanned file}, with cross-domain reuse allowlisted IN THE GUARD {VOCAB_CROSS_DOMAIN_SOURCES} rather than asserted in the manifest. Live disposal: the two front-end fork copies resolve from the CONTRACT block they consume, the java skeleton and the README from the ENUM they document — and §4/§5 already pin those two sources to each other, so a constant added anywhere in the chain reaches every template. A surface that legitimately renders only PART of the vocabulary declares canonical_subset: {omit, reason}, non-redundancy-checked like wire_missing {none does today}; a forbidden: token that IS in the resolved vocabulary is a contradiction and FAILS. ZERO_VOCAB_CANONICAL_SOURCE joins the non-vacuity floor. Three fixtures pin the closure — fail_vocab_canonical_stale {the reproduction, verified exit 0 on the pre-fix guard and exit 1 naming both stale templates after}, fail_vocab_canonical_unbound {no canonical_from at all}, fail_vocab_subset_stale {a subset allowance naming a token the source does not declare} — and all three are [87] kill-proofed. Zero blocks / zero java bindings / zero vocab_scans / zero structurally-exhaustive declarations / zero extracting wire_sources / zero producer-resolved canonical vocabularies each FAIL. Live exits 0.)"
run_guard "contract_enum_parity/live" 0 \
    bash "$SCRIPT_DIR/contract_enum_parity_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # pass_clean also IS the extractor fixture set: it binds one enum of each of
    # the three real shapes, so losing any shape flips it 0→1.
    run_guard "contract_enum_parity/fixture_pass_clean" 0 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/pass_clean"
    # constructor-arg enum loses a constant the contract still promises → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_drift" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_drift"
    # an enum: block exists on disk with no manifest entry → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_unmapped_enum" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_unmapped_enum"
    # the skeleton's real drift token SUCCESS restored — fails on BOTH axes
    # (forbidden token present AND require_all missing SUCCEEDED) → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_vocab_scan" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_vocab_scan"
    # a BRAND-NEW token nobody deny-listed ('BOUNCED' appended to the TS union) —
    # the denylist sees nothing; exact set equality against `canonical` → BLOCK.
    # This is the fixture for the reviewer finding that vocab_scan was a denylist.
    run_guard "contract_enum_parity/fixture_fail_added_token" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_added_token"
    # a vocab_scan with no `declaration:` block — no surface may opt out of the
    # exhaustive path and fall back to denylist-only → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_missing_declaration" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_missing_declaration"
    # P0-2 — a wire_only entry carrying a `reason:` and NOTHING else (the
    # classification-only shape the pre-fix guard waved through) → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_wire_only_no_source" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_wire_only_no_source"
    # P0-2 — the declared producer's literal drifted away from the contract block
    # (the reviewer's ratelimit "ok" → "healthy" reproduction, in miniature) → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_wire_only_drift" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_wire_only_drift"
    # P0-2 — an `unproduced` absence proof whose producer has since appeared: the ONLY
    # escape from set equality must self-destruct rather than rot → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_wire_only_unproduced_producer_exists" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_wire_only_unproduced_producer_exists"
    # P0-2 ROUND 2, ESCAPE 1 — the producing method holds a NON-LITERAL expression
    # (`"note-healthy".toString()`), so `pattern:` skips it and captures the contract's
    # literal from an UNRELATED sibling method: set equality held on literals nothing puts
    # on the wire. `residue_probe` survives the deletion and the guard must refuse to
    # guess → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_literal_pattern_residue" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_literal_pattern_residue"
    # P0-2 ROUND 2, ESCAPE 2 — an `unproduced` absence claim carrying source probes ONLY.
    # No regex sees an SPI implemented alongside another interface (or nested, anonymous,
    # generic, @Bean-produced), so the claim must be bound to a runtime/bytecode test →
    # BLOCK.
    run_guard "contract_enum_parity/fixture_fail_unproduced_unverified" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_unproduced_unverified"
    # …and the binding must be LOAD-BEARING: a verified_by naming a gradle task nobody
    # registers is a proof nothing runs → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_verified_by_not_run" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_verified_by_not_run"
    # P0-2 ROUND 3, ESCAPE A — one copy-pasted FQCN repoints the entry at an UNRELATED
    # domain's enum carrying the SAME constants (com.demo.other.OtherStatus for
    # com.demo.WidgetStatus). Set equality holds against the wrong enum and the right one
    # is never examined again. The binding must be corroborated by the on-disk domain →
    # BLOCK.
    run_guard "contract_enum_parity/fixture_fail_java_enum_foreign_domain" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_java_enum_foreign_domain"
    # P0-2 ROUND 3, ESCAPE B — the producing method swaps CONSTRUCT (a foreign map factory
    # + System.getProperty) so neither `pattern` nor `residue_probe` sees it, while a
    # sibling method keeps the contract's literal: file-scoped residue is empty and the
    # extracted set still matches. Body-scoped residue (every return in the declared
    # producer must be consumed) refuses to guess → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_producer_construct_swap" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_producer_construct_swap"
    # P0-2 ROUND 3, ESCAPE B' — `methods:` is itself an author assertion, so OMITTING the
    # real handler restores the same escape: a SECOND request-mapped method emits "note-b"
    # by another construct while the declared one keeps the contract's literal. The handler
    # set is a fact on disk; every mapped method must be declared → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_producer_scope_incomplete" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_producer_scope_incomplete"
    # P0-2 ROUND 4 — the `unproduced` branch proved producer ABSENCE and returned without
    # ever comparing the contract's own tokens to anything, so the CONTRACT side could be
    # edited freely. Unlike every other fixture here, these two mutate NO code: the tree's
    # java sources, producers and blueprints are byte-identical to pass_clean.
    #   (a) the entry names no independent canonical source at all — the pre-round-4 shape,
    #       in which vocabulary-correctness was simply never an obligation → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_unproduced_no_canonical" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_unproduced_no_canonical"
    #   (b) CONTRACT-ONLY token edit — `outbound` → `accidental_typo`, the miniature of the
    #       reviewer's `mock` → `accidental_typo` on the payment callback slug. The absence
    #       proof and its runtime binding still hold perfectly; only the advertised
    #       vocabulary moved, which is precisely what nothing noticed before → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_unproduced_contract_vocab_drift" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_unproduced_contract_vocab_drift"
    # P1 ROUND 5 — the CANONICAL side of a vocab_scan. Three fixtures, all CODE-realistic:
    #   (a) THE hole: the producer AND the contract gain a constant together (so the
    #       contract_enum entries stay green — this is the ordinary way a vocabulary grows)
    #       and the four fork-copy templates are simply forgotten. With `canonical:` written
    #       inline the guard compared the templates to the STALE manifest literal and exited
    #       0; resolved from the producer it names every stale surface → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_vocab_canonical_stale" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_vocab_canonical_stale"
    #   (b) a vocab_scan with NO canonical_from — the shape the hole lived in. Mandatory, so
    #       the inline-copy schema cannot be reintroduced by omission → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_vocab_canonical_unbound" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_vocab_canonical_unbound"
    #   (c) the declared-subset escape hatch cannot rot: `omit:` names a token the source
    #       does not declare, the same non-redundancy discipline wire_missing carries → BLOCK.
    run_guard "contract_enum_parity/fixture_fail_vocab_subset_stale" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_vocab_subset_stale"
    # P3-87 (Lane D) — the ROUND-3 residual, now closed. No same-package same-constant-set
    # enum pair exists live, so all three fixtures CONSTRUCT one: the binding left
    # undeclared must BLOCK, a declaration that no longer disambiguates anything must BLOCK
    # (non-redundancy), and a properly declared one must PASS.
    run_guard "contract_enum_parity/fixture_fail_same_package_same_set" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_same_package_same_set"
    run_guard "contract_enum_parity/fixture_fail_stale_disambiguation" 1 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/fail_stale_disambiguation"
    run_guard "contract_enum_parity/fixture_pass_disambiguated" 0 \
        bash "$SCRIPT_DIR/contract_enum_parity_guard.sh" --root "$SCRIPT_DIR/fixtures/contract_enum_parity_guard/pass_disambiguated"
fi

echo "[93] l2_frontmatter_deps_guard.sh (P3-66 — L2 block frontmatter dependencies: declares a real sibling template that is NOT imported while a DIFFERENT real sibling from the same directory IS. Deliberately narrow necessary-not-sufficient floor: generic UI vocabulary (button/badge) declared without a literal import is legitimate catalog-wide and is NOT flagged. P3-71: the check only fires when \`dependencies:\` has EXACTLY ONE entry — with 2+ entries the guard cannot mechanically tell a swapped/stale reference apart from ordinary vocabulary declared alongside one real functional import, and firing anyway was a live false positive (any block importing one real L1/L2 sibling flagged EVERY OTHER real-named vocabulary entry, e.g. adding \`button\` to invoice-list.tsx's deps tripped exit 1 even though \`button\` was never claimed to be imported). Live scans 113 blocks, exits 0.)"
run_guard "l2_frontmatter_deps/live" 0 \
    bash "$SCRIPT_DIR/l2_frontmatter_deps_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "l2_frontmatter_deps/fixture_pass" 0 \
        bash "$SCRIPT_DIR/l2_frontmatter_deps_guard.sh" --root "$SCRIPT_DIR/fixtures/l2_frontmatter_deps_guard/pass_clean"
    run_guard "l2_frontmatter_deps/fixture_fail_stale" 1 \
        bash "$SCRIPT_DIR/l2_frontmatter_deps_guard.sh" --root "$SCRIPT_DIR/fixtures/l2_frontmatter_deps_guard/fail_stale_dependency"
    # P3-71 — regression pin: a block that imports one real L1 sibling AND
    # declares a DIFFERENT real-L1-named entry purely as doc-only vocabulary
    # (never imported, e.g. `button`) must PASS, not be flagged as stale.
    run_guard "l2_frontmatter_deps/fixture_pass_vocab_plus_import" 0 \
        bash "$SCRIPT_DIR/l2_frontmatter_deps_guard.sh" --root "$SCRIPT_DIR/fixtures/l2_frontmatter_deps_guard/pass_vocab_plus_import"
fi

echo "[94] l4_presentational_view_guard.sh (P2-28 — every (page, view) pair recorded in l4_presentational_view_ledger.yaml must hold on disk: the view exists, imports no data-fetching hook, and page.tsx actually imports it (catches a silent re-inline). The ledger may only GROW — shrinking below min_entries FAILS. Requires python3 + PyYAML. Live checks 4 ledgered pairs, exits 0.)"
run_guard "l4_presentational_view/live" 0 \
    bash "$SCRIPT_DIR/l4_presentational_view_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "l4_presentational_view/fixture_pass" 0 \
        bash "$SCRIPT_DIR/l4_presentational_view_guard.sh" --root "$SCRIPT_DIR/fixtures/l4-presentational-view/pass_clean" --ledger "$SCRIPT_DIR/fixtures/l4-presentational-view/pass_clean/ledger.yaml"
    run_guard "l4_presentational_view/fixture_fail_reinlined" 1 \
        bash "$SCRIPT_DIR/l4_presentational_view_guard.sh" --root "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_reinlined" --ledger "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_reinlined/ledger.yaml"
    run_guard "l4_presentational_view/fixture_fail_hook_in_view" 1 \
        bash "$SCRIPT_DIR/l4_presentational_view_guard.sh" --root "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_hook_in_view" --ledger "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_hook_in_view/ledger.yaml"
    run_guard "l4_presentational_view/fixture_fail_shrunk_ledger" 1 \
        bash "$SCRIPT_DIR/l4_presentational_view_guard.sh" --root "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_shrunk_ledger" --ledger "$SCRIPT_DIR/fixtures/l4-presentational-view/fail_shrunk_ledger/ledger.yaml"
fi

echo "[95] pyyaml_preflight_coverage_guard.sh (R25 preflight ⊇ PyYAML-dependent guards. ~15 catalog guards embed 'import yaml' with no yq fallback and several SKIP SILENTLY (exit 0) without PyYAML — so a yq-only machine used to pass the 'PyYAML or yq' preflight and then report a PASS for guards that never ran. Enumerates the dependent set mechanically, computes per-step transitive reachability, and asserts the REAL verify-completion.sh blocks each step that needs the parser while leaving script-free steps (backend-only runs) unblocked. Live exits 0.)"
run_guard "pyyaml_preflight_coverage/live" 0 \
    bash "$SCRIPT_DIR/pyyaml_preflight_coverage_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "pyyaml_preflight_coverage/fixture_pass_covered" 0 \
        bash "$SCRIPT_DIR/pyyaml_preflight_coverage_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/pyyaml-preflight-coverage/pass_covered"
    # The uncovered shape: a PyYAML-dependent gate reached through a wrapper OUTSIDE evals/,
    # which the preflight's path heuristic misses → the guard must FAIL.
    run_guard "pyyaml_preflight_coverage/fixture_fail_hidden_dependency" 1 \
        bash "$SCRIPT_DIR/pyyaml_preflight_coverage_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/pyyaml-preflight-coverage/fail_hidden_dependency"
    # The fail-OPEN shape: the step IS preflight-covered, but the guard it runs skips with
    # exit 0 when the parser is missing — a green that verified nothing → the guard must FAIL.
    run_guard "pyyaml_preflight_coverage/fixture_fail_open_guard" 1 \
        bash "$SCRIPT_DIR/pyyaml_preflight_coverage_guard.sh" --repo-root "$SCRIPT_DIR/fixtures/pyyaml-preflight-coverage/fail_open_guard"
fi

echo "[96] resume_provenance_guard.sh (a BLOCKED R25 run may not launder itself green. P0-30 made an incomplete result ledger exit 2, but the step verdict was still 'no failure ⇒ PASS' and verdicts publish incrementally — so a step whose commands never executed was written to .ax-verify/last_run.jsonl as PASS, and the next --resume skipped it and exited 0 with full_run=true, which the recency guard accepts. Runs the REAL verify-completion.sh in a throwaway sandbox against a two-step harness (step 1 wipes the run's own temp dir, step 2 is \`false\`) and asserts the resume run cannot claim green, that a clean run's --resume still works, and that SHORT_CIRCUITED cannot be inherited from the environment. 2026-07-29 extension — a SKIPPED command produces a row too: an absent working_directory used to record SKIP, so \`mv frontend frontend.off; verify-completion.sh --step frontend-lint\` exited 0 with \`npm run lint\` never invoked and the next --resume inherited that PASS. A second harness (a command whose directory is moved away, beside one that really runs) asserts the absent directory BLOCKS, that a step is publishable only when every required command actually executed, and that advisory commands stay advisory. Both harnesses carry their own neuter matrix (A/B and C/D). 2026-07-29 cross-family review added two more axes: (E) resume evidence was bound to head_sha, not to the tree that ran — R25 runs on dirty trees, so an uncommitted edit that makes a step pass, then reverted at the same head, left a record \`--resume\` still consumed (a git-backed harness whose COMMITTED state fails the step reproduces it, and each record now carries a working-tree fingerprint the preloader must match); (F) a SELECTED step declaring \`commands: []\` emitted no plan row, so it vanished from STEP_ORDER and from every accounting check while the run published green — now a parse-time BLOCK. 2026-07-29 re-review extended (F) to the whole MALFORMED-step family and added (G): a comment-only or whitespace-only command makes \`bash -c\` exit 0, so the runner records an EXECUTED PASS for a shell that did nothing, and a blank/whitespace id gives the step ZERO iterations of \`for sid in \$STEP_ORDER\` — none of its commands run and the counters stay green. Selected steps now require a unique slug id + at least one command that actually runs something (parse-time BLOCK naming the step), backstopped structurally by (G): every DECLARED step must have emitted a plan row, so a step that vanished cannot be invisible to accounting that derives its step set from the plan. 2026-07-29 re-review again: the same certification survives through the canonical DISABLE IDIOMS — \`command: 'true'\` / \`':'\` / \`'exit 0'\` (and chains of them) exit with the expected status having run no program of the contract, so the runner records an EXECUTED PASS. Rejected now by a FINITE denylist of those idioms, gated on the exit matching expected_exit so that \`false\` under expected_exit 0 stays an honest FAIL; the boundary is asserted from both sides (a real \`touch\` command stays admitted). It does NOT attempt to decide whether an arbitrary command does useful work — that is undecidable and unclaimed. Per-shape matrix: F+G neutered reproduces all eight shapes, F alone reproduces exactly the six inert-command shapes (the vanishing ones must still block ⇒ G is load-bearing), G alone blocks all eight. Live exits 0.)"
run_guard "resume_provenance/live" 0 \
    bash "$SCRIPT_DIR/resume_provenance_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "resume_provenance/fixture_pass_fixed" 0 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/pass_fixed"
    # Non-vacuity: the same assertions against a verify-completion.sh copy with BOTH
    # fix layers neutered must FAIL. A guard that passes on the unfixed script proves
    # nothing — this entry is what makes the live PASS above meaningful.
    run_guard "resume_provenance/fixture_fail_unfixed" 1 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/fail_unfixed"
    # The absent-working-directory pair (layers C+D) neutered — the shape a cross-family
    # reviewer used on 2026-07-29: `mv frontend frontend.off; verify-completion.sh --step
    # frontend-lint` exited 0 with `npm run lint` never invoked, and the next --resume
    # inherited that PASS. Must FAIL, otherwise the workdir assertions prove nothing.
    run_guard "resume_provenance/fixture_fail_unfixed_workdir" 1 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/fail_unfixed_workdir"
    # Layer E neutered — resume records bound to head_sha ALONE. The reviewer's 2026-07-29
    # path: at head H an uncommitted edit makes the step pass, the edit is reverted (head
    # still H, failure back), and `--resume` skips the step on that record, publishing
    # exit=0/full_run=true for a tree that fails. Must FAIL, or the tree-binding assertion
    # would pass on the unfixed script.
    run_guard "resume_provenance/fixture_fail_unfixed_fingerprint" 1 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/fail_unfixed_fingerprint"
    # Layers F+G neutered — a SELECTED checklist step with zero commands emits no plan row, so
    # it never enters STEP_ORDER and no accounting check can see it: the run publishes green
    # with a required step never verified. BOTH layers are needed for this shape (the parse
    # check rejects it, and the declared ⊆ emitted check catches it structurally), which is
    # exactly what the live matrix asserts. Must FAIL, or the empty-step assertion is vacuous.
    run_guard "resume_provenance/fixture_fail_unfixed_emptystep" 1 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/fail_unfixed_emptystep"
    # Layer F alone neutered — the INERT-COMMAND shapes (`# temporarily disabled`, `   `).
    # These emit a plan row AND execute (bash -c exits 0), so no structural check can see
    # them: the parse-time contract is the only thing standing between a comment and an
    # "EXECUTED PASS". Must FAIL, or that half of assertion 8 is vacuous.
    run_guard "resume_provenance/fixture_fail_unfixed_inertcommand" 1 \
        bash "$SCRIPT_DIR/resume_provenance_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/resume-provenance/fail_unfixed_inertcommand"
fi

echo "[97] push_evidence_tree_binding_guard.sh (a push may ride only on evidence produced from the CLEAN tree of the pushed commit. The tree binding closed for --resume [96 layer E] was left open on the more consequential path, and this one needs NO --resume: (1) committed HEAD H fails a step, (2) an UNCOMMITTED fix makes a full R25 run pass, (3) the fix is reverted — nothing re-runs, the audit line is untouched, (4) \`git push H\` — the recency guard checked only {head_sha, exit, full_run}, all still green, and the push landed. The pushed tree was never verified; the verified tree was never pushed. Fixed as two SERIAL links: (P) verify-completion.sh records tree_clean/tree_fingerprint in the audit line, honestly (clean ⟺ \`git status --porcelain -uall\` empty at a resolvable HEAD), and (C) completion_checklist_recency_guard.sh refuses any line whose tree_clean is not True. Measured end to end in a throwaway git repo with a bare remote and the REAL .githooks/pre-push wired via core.hooksPath — ground truth is whether the REMOTE advanced to the unverified commit. Anti-over-correction axes: a COMMITTED verified change still pushes, and a dirty-tree run remains a full-value local run AND remains --resume-able (only push eligibility tightens). Matrix is honest about the shape of the fix: P and C are serial links, so EACH neuter alone must reproduce the landing — claiming either holds on its own would be false. Live exits 0.)"
run_guard "push_evidence_tree_binding/live" 0 \
    bash "$SCRIPT_DIR/push_evidence_tree_binding_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "push_evidence_tree_binding/fixture_pass_fixed" 0 \
        bash "$SCRIPT_DIR/push_evidence_tree_binding_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/push-evidence-tree-binding/pass_fixed"
    # Producer neutered — verify-completion.sh claims every run was performed on the committed
    # tree. The consumer's check is then satisfied by a lie and the unverified commit lands.
    run_guard "push_evidence_tree_binding/fixture_fail_unfixed_producer" 1 \
        bash "$SCRIPT_DIR/push_evidence_tree_binding_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/push-evidence-tree-binding/fail_unfixed_producer"
    # Consumer neutered — the recency guard ignores tree_clean, i.e. the pre-2026-07-29 push
    # gate. Must FAIL, or the assertion that the push gate reads the field is vacuous.
    run_guard "push_evidence_tree_binding/fixture_fail_unfixed_consumer" 1 \
        bash "$SCRIPT_DIR/push_evidence_tree_binding_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/push-evidence-tree-binding/fail_unfixed_consumer"
fi

echo "[98] midrun_tree_mutation_guard.sh (push evidence must describe a SETTLED tree, not two endpoints of a long window. [97] bound the evidence to the CLEAN tree of the pushed commit but read ONE snapshot: head/fingerprint/cleanliness were captured before the first step and the audit line appended after the last — a window measured at 2,225s on a real full run. So: start clean at commit H whose committed state FAILS a later step, make the uncommitted fix while an early step runs, let the later step pass on it, revert before the run ends — both endpoints are pristine and identical, and \`git push H\` lands. Endpoint evidence cannot see this by construction. Fixed as two SERIAL links: (S) verify-completion.sh re-measures the tree at every step boundary and at the end, recording {head_sha_end, tree_fingerprint_end, tree_clean_end, tree_stable, tree_samples}, and (T) completion_checklist_recency_guard.sh refuses a line that does not show a settled tree (stable, ≥2 samples, both endpoints equal to the audited sha/fingerprint). Measured end to end in a throwaway git repo with a bare remote and the REAL .githooks/pre-push wired via core.hooksPath; the checklist performs the mutation itself, so the scenario is deterministic and the tree really is clean at both endpoints — ground truth is whether the REMOTE advanced to the commit that fails the contract. Anti-over-correction: a settled run of the same shape still ships, and steps that write only GIT-IGNORED artifacts (build/, .ax-verify/ — every real R25 step) do not count as mutation. HONEST LIMIT: sampling is at step boundaries, so a change made AND undone inside one step is unobserved — the exposure is one step wide, not zero. Live exits 0.)"
run_guard "midrun_tree_mutation/live" 0 \
    bash "$SCRIPT_DIR/midrun_tree_mutation_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "midrun_tree_mutation/fixture_pass_fixed" 0 \
        bash "$SCRIPT_DIR/midrun_tree_mutation_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/midrun-tree-mutation/pass_fixed"
    # Producer neutered — verify-completion.sh never notices a sample disagreeing with the
    # start, so every line claims a settled tree and the consumer's check is satisfied by a lie.
    run_guard "midrun_tree_mutation/fixture_fail_unfixed_producer" 1 \
        bash "$SCRIPT_DIR/midrun_tree_mutation_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/midrun-tree-mutation/fail_unfixed_producer"
    # Consumer neutered — the recency guard stops requiring a settled tree, i.e. the
    # endpoints-only push gate. Must FAIL, or the live assertion is vacuous.
    run_guard "midrun_tree_mutation/fixture_fail_unfixed_consumer" 1 \
        bash "$SCRIPT_DIR/midrun_tree_mutation_guard.sh" --fixture-root "$SCRIPT_DIR/fixtures/midrun-tree-mutation/fail_unfixed_consumer"
fi

# ── 99. coverage_map_guard (gap-convergence engine — lives OUTSIDE evals/) ───
ENGINE_DIR="$REPO_ROOT/practices/consumer-proof/engine"
echo "[99] consumer-proof/engine/coverage_map_guard.sh (P2-44 — the MECE/disk-truth/nonvacuity guard for the gap-convergence engine's coverage-map.yaml, REGISTERED here after the wave-1 isolation posture was lifted. While isolated it was wired into NEITHER this script NOR verification-checklist.yaml, so all eight checks — including the S3 composition check (P2-29) and the disk-truth/nonvacuity checks (P3-58/P3-60) — held only when a human ran the file by hand, and an R25 completion could be declared while the coverage map lied about its own coverage. The row's reproduction was exact: repoint one covered cell's nonvacuity at a nonexistent path and verify-completion.sh still PASSED. The eight checks: (1) every axis value is a member of its closed enum (D/C/L/R) — free text FAILS, so a cell cannot be filed under an invented domain; (2) the cell id set equals the exact expected cross-product minus masked cells, each masked row carrying a non-null na_reason — a DROPPED or DUPLICATED cell FAILS, which is what makes the denominator honest; (3) every D value appears in docs/IMPLEMENTATION-STATUS.md and every R value is status: active in recipes/_MANIFEST.yaml — disk truth, not author assertion; (4) every covered_by / nonvacuity path resolves on disk (glob >= 1 match); (5) status: covered with an EMPTY nonvacuity list FAILS (the honesty floor — a cell may not be claimed covered by assertion alone); (6) every scored cell's weight equals the canonical per-tier/concern value, closing the weight-tamper over-report vector (a map that inflates covered-cell weight or deflates gap-cell weight passes every other check yet silently raises C_total); (7) every S3 COMPOSITION cell claimed covered must cite at least one LIVE re-executable test path (*Test.java / *IT.java / *.vitest.* / *.spec.*) and NOT a bare *-compose.spec.* file-existence artifact and NOT a sealed-verdict .md — check 4 alone only asserts a path RESOLVES, so a markdown record used to qualify a composition cell; (8) every S1/S2 covered cell cites >= 1 non-.md nonvacuity entry. TOOLCHAIN: fail-closed, exit 2 = cannot verify (never a silent exit 0) per the P2-46 convention, and the dependency is written as a parseable import so pyyaml_preflight_coverage_guard [95] — which re-derives the dependent set from disk rather than from a list — picks up both this wrapper and lib/coverage_map_guard.py automatically. Live scans 107 scored cells (S1=65 S2=31 S3=11) + masked rows, exits 0.)"
run_guard "coverage_map/live" 0 \
    bash "$ENGINE_DIR/coverage_map_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "coverage_map/fixture_pass_clean" 0 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/pass_clean.yaml"
    run_guard "coverage_map/fixture_fail_free_text_axis" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_free_text_axis.yaml"
    run_guard "coverage_map/fixture_fail_dup_cell" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_dup_cell.yaml"
    run_guard "coverage_map/fixture_fail_covered_no_nonvacuity" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_covered_no_nonvacuity.yaml"
    run_guard "coverage_map/fixture_fail_weight_tamper" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_weight_tamper.yaml"
    run_guard "coverage_map/fixture_fail_md_only_s1" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_md_only_s1.yaml"
    run_guard "coverage_map/fixture_fail_s3_covered_sealed_verdict_only" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_s3_covered_sealed_verdict_only.yaml"
    run_guard "coverage_map/fixture_fail_s3_content_blind_rename" 1 \
        bash "$ENGINE_DIR/coverage_map_guard.sh" --map "$ENGINE_DIR/fixtures/coverage_map_guard/fail_s3_content_blind_rename.yaml"
fi

# ── 100. domain_mode_consistency_guard ──────────────────────────────────────
echo "[100] domain_mode_consistency_guard.sh (P3-83 — a domain's trio mode is declared in up to THREE places and NOTHING compared them: the spec's domain_mode, trio_integrity_allowlist.yaml, and the L4 README Status line. The motivating instance: specs/ratelimit-l0.yaml said full_trio while the allowlist, the README ('Frontend trio deliberately skipped') and CLAUDE.md all said backend-only — false for as long as full_trio_artifact_completeness_guard's old os.path.isdir() check was satisfied by a lone README, and caught the moment that check was hardened. What was fixed then was the INSTANCE; this guard closes the CLASS. It is not a documentation nit: the ALLOWLIST is the enforcement input — backend_only there makes trio_integrity_guard SKIP the frontend Spec Trio entirely — so a spec that says full_trio while the allowlist says backend_only means a gate stopped running. Checks: (1) wherever >=2 of the three declare a mode they must be EQUAL (a single declaration has nothing to compare and is reported as out-of-scope rather than silently counted); (2) a README Status VALUE naming two different modes is AMBIGUOUS and FAILS — the guard refuses to rank them; (3) two backend specs for one domain declaring different modes FAILS, so the answer cannot depend on which file the reader opens (the prefix glob that trio_integrity_guard uses over-matches — webhook-signing-l0.yaml is a different spec — so the canonical <domain>-l0.yaml wins when both exist); (4) every divergence must carry an in-guard allowance with the EXACT observed triple + class + reason + backlog_ref, non-redundancy-checked BOTH ways (an allowance for a domain that no longer diverges FAILS, and a divergence that changed shape FAILS rather than being inherited); (5) census floors — 14 L4 READMEs declaring a mode and 52 cross-checked domains, measured not guessed, so deleting Status lines cannot empty the (C) axis and leave a green nothing. Allowances live IN THE GUARD ([92] precedent) so widening is a reviewed edit to the gate. LIVE CENSUS FOUND THREE REAL DIVERGENCES, all the mirror of the ratelimit one — there the spec lied, here the allowlist does: webhook (R48), scheduled-task (R49) and email-outbox (R51) each ship 6 .tsx under templates/L4/<d>/app/ and moved spec+README to full_trio, but their allowlist entries still read backend_only with a now-false 'no frontend UI in scope' comment, so trio_integrity_guard skips the frontend trio for exactly those three. NOT closable by flipping the entry: specs/<d>-frontend-l0.yaml exists for 11 domains and for none of these three, so flipping makes trio_integrity_guard fail MISSING_FRONTEND_SPEC. HISTORY: registered as known_gap (P3-83→P2-47) and printed on every run UNTIL 2026-07-30, when P2-47 authored the three frontend Spec Trios (+ -ui.yaml + -ui-manifest.yaml legs) and flipped the allowlist — the guard's own non-redundancy check then forced the three allowances OUT, and the table is now EMPTY. P2-52 (2026-07-30): every backlog_ref must name a row that EXISTS in docs/BACKLOG.md, and a known_gap's row must still be OPEN — known_gap stays exit 0 while its row is open (visibility) and BLOCKS the moment it closes or vanishes; by_design requires existence only. Non-catalog trees may declare probe subjects in domain_mode_probe_allowances.yaml (REFUSED on the catalog tree, adds subjects only). Fixtures: fail_backlog_ref_closed / fail_backlog_ref_missing. Live exits 0, zero divergences.)"
run_guard "domain_mode_consistency/live" 0 \
    bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # The fixture trees are 1-domain, so they pass explicit floors; the live floors above are
    # what protect the catalog. fail_readme_ambiguous uses floor 0 on purpose — an ambiguous
    # value contributes NO declaration, so floor 1 would fire too and the fixture would stop
    # being specific to the ambiguity check.
    run_guard "domain_mode_consistency/fixture_pass_clean" 0 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/pass_clean" --min-readme-declarations 1 --min-crosschecked 1
    run_guard "domain_mode_consistency/fixture_fail_spec_contradicts_allowlist" 1 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/fail_spec_contradicts_allowlist" --min-readme-declarations 1 --min-crosschecked 1
    run_guard "domain_mode_consistency/fixture_fail_readme_ambiguous" 1 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/fail_readme_ambiguous" --min-readme-declarations 0 --min-crosschecked 1
    run_guard "domain_mode_consistency/fixture_fail_floor_breach" 1 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/fail_floor_breach" --min-readme-declarations 1 --min-crosschecked 1
    run_guard "domain_mode_consistency/fixture_fail_backlog_ref_closed" 1 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/fail_backlog_ref_closed" --min-readme-declarations 1 --min-crosschecked 1
    run_guard "domain_mode_consistency/fixture_fail_backlog_ref_missing" 1 \
        bash "$SCRIPT_DIR/domain_mode_consistency_guard.sh" --root "$SCRIPT_DIR/fixtures/domain-mode-consistency/fail_backlog_ref_missing" --min-readme-declarations 1 --min-crosschecked 1
fi

# ── 101. manifest_snapshot_integrity_guard ──────────────────────────────────
echo "[101] manifest_snapshot_integrity_guard.sh (BACKLOG P2-57 / PRD-final-4 W1b — the protected-anchor ratchet pins 64 template citations to the TEXT OF SNAPSHOT BODIES, and until this guard nothing checksummed those bodies. Wave-start census: of the 91 manifest ids with a committed .snapshot.md, 71 recorded a sha/bytes pair that did not describe the file it claims to describe, and no gate looked — stripe-billing said 1657 bytes for a 2089-byte file, and recharts/next-intl/kakao-postcode shared ONE sha across THREE different byte counts, which a sha256 cannot do, so at least two of those records were never computed from any file. Ratcheting 46 further identities onto bodies in that state would have been a PAPER ratchet: quote locked, quoted thing freely editable. THREE DOMAINS, never a cross-domain equality: (a) FILE — every id with a body must have manifest sha/bytes == shasum/wc of the WHOLE FILE; (b) BODY — every W1-touched id's header-recorded body-sha == recompute with the header stripped at the first literal '---'+blank line (the header records the BODY digest, never its own file's — a self-referential sha is unverifiable by construction); (c) RECEIPT — that body-sha == the id's kind:assembly row in practices/upstream/_FETCH-RECEIPTS.yaml and every per-URL receipt it references must exist, else RECEIPT_MISSING exit 2. (c) is what makes a fetch the ONLY legal refresh path: editing a body and its manifest entry together — the natural way to launder a doctored snapshot past (a) — leaves no receipt describing the new bytes, and writing one means recording a URL, an HTTP status and a fetched_at. The touched set is DERIVED from the assembly rows, not hardcoded, so it is the committed ledger and not the guard author that says which ids are covered. Allowlist (63 entries at introduction) is shrink-only: the 71-entry wave-start census is FROZEN as baseline_universe and additions are exit 2, keys are unique, every entry carries a reason, a W1-touched id may not appear, and NON-REDUNDANCY fails an entry whose manifest now matches disk — so the list length always IS the residual and burn-down cannot be absorbed by padding. Suppression is honest rather than corrected because recomputing those records from the present file would MANUFACTURE the provenance claim this guard exists to prevent; closure is a per-id re-fetch. Non-vacuity: zero ids = exit 2, plus live floors LIVE_MIN_IDS=91 / LIVE_MIN_TOUCHED=8. ANCHOR RATCHET (P1-2/P1-3, cross-family reviewer 2026-07-30, TD-2026-07-30-P1-anchor-ratchet): every 'frozen' surface used to be re-read from the same mutable tree the commit under review is editing, so a coherent edit of all of them tripped nothing — the reviewer proved three bypasses. Fixed by anchoring to the PREVIOUS RELEASE via git show <origin/main→HEAD fallback>:<path>, exit 4 (distinct from 1/2): (i) chain scope is now CHANGE-DRIVEN — any id whose snapshot file differs from the anchor's, or whose manifest sha/bytes differ from the anchor's manifest, must pass the full 3-domain chain, so refreshing an allowlisted NON-assembly id (reviewer's cwv-2026: edit body + sync manifest + drop the allowlist row) dies RECEIPT_MISSING instead of never being looked at; (ii) RECEIPT_LEDGER_MUTATED — every row present in the anchor's ledger must still be present and unchanged (parsed-row identity always, byte-chunk identity when both sides chunk cleanly), killing the rewrite-an-existing-assembly-row laundering; (iii) BASELINE_MUTATED + ALLOWLIST_GREW — baseline_universe must EQUAL the anchor's and the residual must be a SUBSET of it, because enforcing 'frozen' against the same file being edited was self-certifying (reviewer's mdn-promise-all: diverge a clean id, then add it to baseline+entries+count). Consequence of append-only: multiple assembly rows per identity are now LEGAL (a repeat refresh appends rather than rewrites) and the chain binds to the LATEST row per id. Anchor checks are live-root-only and therefore NOT fixture-coverable — their evidence is the live reproductions in DECISIONS.md, each reproduced against the released copy (exit 0) and this one (exit 4/2) then restored byte-identically. Residual, stated: a receipt is self-reported, so append-only closes REWRITING one but not APPENDING a fabricated one — the chain makes a doctored refresh undeniable in the record, not impossible. Fixtures: pass_clean / fail_diverged / fail_receipt_missing / fail_stale_allowlist / pass_repeat_refresh_latest_assembly (two assembly rows for one id, latest binds — exit 2 under the pre-P1 guard) / fail_latest_assembly_mismatch (latest row wrong while an EARLIER row matches, so 'bind to latest' cannot degrade into any-row-matches); fail_diverged + fail_stale_allowlist are [87] kill-proof registered (the two new fixtures are not: one is a PASS fixture and the other exits 2, and [87] registers exit-1 fail fixtures, so the 64/64 double floor is unchanged). P1-B ROUND-2 (reviewer round 2, 2026-07-30, TD-2026-07-30-P1-anchor-ratchet-r2): allowing multiple assembly rows per identity made FILE ORDER authoritative for the verdict (the chain binds to the LAST assembly row), while the append-only check compared rows AND byte-chunks BY ID — order-blind. Reviewer's attack, reproduced against a synthetic two-generation anchor in a throwaway clone: with the anchor holding [asm-v1(body1), asm-v2(body2)] and body2 current, SWAP the two byte-intact chunks to [asm-v2, asm-v1] and revert the snapshot + header + manifest to body1 — every parsed row present and equal, every chunk present and equal, nothing 'changed', and asm-v1 is now LATEST so the chain verifies body1: a released refresh silently ROLLED BACK, exit 0. Fixed by a PREFIX RULE: the anchor's row-id sequence must be an EXACT PREFIX of the current one, and so must the byte-chunk sequence — only SUFFIX APPENDS are legal, and reordering / insertion before the end / removal are RECEIPT_LEDGER_MUTATED (exit 4; the reproduction flips 0 → 4, and a legitimate 3rd-generation suffix append still exits 0). Second attack: INDENT the whole receipts: list — semantically identical YAML, every value-level check green, and ANCHOR_RECEIPTS_UNCHUNKABLE silently retired the byte layer with an advisory (exit 0). Fixed in two places: SELF-CHUNK (exit 2 RECEIPTS_SELF_UNCHUNKABLE) applies on EVERY root — the ledger is machine-owned by practices/scripts/snapshot-extract.sh, so keeping it chunkable is our own formatting rule — which kills the shape at generation N so it can never become an anchor; and an unchunkable ANCHOR is now BLOCKING (exit 4) on a live root as belt-and-braces for anchors committed earlier. New fixture fail_ledger_unchunkable proves the self-chunk half without any anchor dependence (byte-copy of pass_clean with the list indented; pre-P1-B guard exits 0 on it). The prefix rule and the anchor-side unchunkable block remain anchor-bound and therefore NOT fixture-coverable — their evidence is the live reproductions in DECISIONS.md, restored byte-identically. P1-X/P1-Y ROUND-3 (reviewer round 3, 2026-07-30, TD-2026-07-30-P1-anchor-authenticity): (X) the anchor REF itself is attacker-mutable — git update-ref refs/remotes/origin/main pointed at a synthetic tree MINUS the ratcheting guards made every ratchet take its bootstrap skip. Three layers, all exit 5 here: ANCHOR_NOT_ANCESTOR (the anchor must be an ancestor of HEAD), ANCHOR_BOOTSTRAP_IMPLAUSIBLE (a file with history in this repo can never legitimately be 'absent in the previous release'; measured to fire ALONE with ancestry satisfied), and the audit-bound anchor authenticated at pre-push against the sha the REMOTE advertises. (Y) SYMLINK LAUNDERING — with _FETCH-RECEIPTS.yaml symlinked, git show anchor:… returns the target PATHNAME (a YAML SCALAR) while this process parses the real mapping through the link, and every append-only layer was nested under isinstance(prior_doc, dict) with NO blocking else: measured, rewriting a released row's provenance field exits 4 against an honest anchor and exit 0 against the symlink anchor — the whole ratchet retired in silence. Closed twice: ANCHOR_PATH_NOT_REGULAR / SELF_PATH_NOT_REGULAR mode checks (both guard files, the receipts ledger, both _MANIFEST.yaml, the allowlist, and every *.snapshot.md via os.path.islink), and a FAIL-CLOSED ANCHOR_MALFORMED replacing every permissive isinstance nesting. Full silent-skip sweep, each previously WARN+skip and now blocking: receipts anchor unparseable / receipts anchor not a mapping / allowlist anchor unparseable (was collapsed into 'absent ⇒ bootstrap' — anchor_yaml now returns a distinct ANCHOR_ABSENT sentinel) / ANCHOR_ALLOWLIST_SHAPE / ANCHOR_BASELINE_MALFORMED / ANCHOR_DIFF_UNAVAILABLE on a live root. New fixture fail_receipts_symlink proves the self-side half with no anchor dependence (byte-copy of pass_clean whose ledger is a symlink to a sibling holding identical bytes; exit 2, and exit 0 under the pre-round-3 guard). Exit 2, so the [87] kill-proof floor stays at 64. The anchor-side codes remain live-root-bound and NOT fixture-coverable — evidence is the RED reproductions in DECISIONS.md, restored byte-identically. ROUND-4 (P1-1/P1-2/P1-3, 2026-07-30, TD-2026-07-30-P1-anchor-runtime): this guard shares the round-4 hardening through the same helper — every git call runs --no-replace-objects with GIT_NO_REPLACE_OBJECTS=1 exported (a `git replace` ref keeps shas identical while swapping the objects the ancestry/bootstrap/ls-tree reads see, measured in a throwaway clone: anchor floor reads 0 with the replace ref and 64 without it, sha unchanged), a tree carrying refs/replace/* is refused outright (ANCHOR_REPLACE_REFS_PRESENT, exit 5), the anchor is resolved ONCE by the runner and re-authenticated at this guard's own read plus once more after its work (ANCHOR_REF_MOVED_MIDRUN, exit 5 — measured: pre-fix exit 0 against a ref flipped to the root commit, post-fix exit 8 in the sibling guard, and the standalone self-verify fires on a 0.55s window), and the helper is loaded through a duplicated inline lstat+git-mode preflight (HELPER_PATH_NOT_REGULAR, exit 2) with the env fallback restricted to non-git roots. The four ratchet-toolchain paths — the helper, verify-completion.sh, completion_checklist_recency_guard.sh and both hook files — are now ON this guard's anchor-critical list, so deleting one is ANCHOR_BOOTSTRAP_IMPLAUSIBLE and symlinking one is ANCHOR_PATH_NOT_REGULAR.)"
run_guard "manifest_snapshot_integrity/live" 0 \
    bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh"
if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    run_guard "manifest_snapshot_integrity/fixture_pass_clean" 0 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/pass_clean"
    run_guard "manifest_snapshot_integrity/fixture_fail_diverged" 1 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_diverged"
    run_guard "manifest_snapshot_integrity/fixture_fail_receipt_missing" 2 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_receipt_missing"
    run_guard "manifest_snapshot_integrity/fixture_fail_stale_allowlist" 1 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_stale_allowlist"
    # P1-2c — the receipts ledger is append-only vs the release anchor, so a REPEAT refresh must be
    # able to APPEND a second assembly row instead of rewriting the first. These two fixtures pin
    # both halves of that: the latest row binds (pass), and it binds NON-VACUOUSLY — a wrong latest
    # row is not rescued by an earlier row that happens to match (fail).
    run_guard "manifest_snapshot_integrity/fixture_pass_repeat_refresh_latest_assembly" 0 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/pass_repeat_refresh_latest_assembly"
    run_guard "manifest_snapshot_integrity/fixture_fail_latest_assembly_mismatch" 2 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_latest_assembly_mismatch"
    # P1-B round-2 (2026-07-30): the ONE half of the order/chunk ratchet a fixture can prove,
    # because it inspects the tree in front of it rather than a release history. This fixture is a
    # BYTE-COPY of pass_clean whose `receipts:` list is indented by two spaces and nothing else —
    # semantically identical YAML (asserted when the fixture was built), so pass_clean → exit 0 and
    # this → exit 2 is attributable to the indent alone. Non-vacuity: the pre-P1-B guard exits 0 on
    # it (the byte-chunk layer silently retired itself with an advisory WARN). Exit 2, not 1, so the
    # [87] kill-proof floor (which registers exit-1 fail fixtures) is unchanged.
    run_guard "manifest_snapshot_integrity/fixture_fail_ledger_unchunkable" 2 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_ledger_unchunkable"
    # P1-Y round-3 (2026-07-30): the fixture-provable half of the symlink class. The anchor side
    # reads GIT OBJECTS and the self side reads the FILESYSTEM, so a symlinked ledger makes the
    # two read different bytes by construction — `git show <anchor>:…` returns the target
    # PATHNAME (a YAML scalar) while this process parses the real file through the link, and
    # every append-only layer used to be nested under `isinstance(prior_doc, dict)` with no
    # blocking else. This fixture is a copy of pass_clean whose receipts ledger is a symlink to a
    # sibling holding the identical bytes and NOTHING else, so pass_clean → 0 and this → 2 is
    # attributable to the link alone. Non-vacuity: the pre-round-3 guard exits 0 on it. Exit 2
    # (structural), so the [87] exit-1 kill-proof floor is unchanged.
    run_guard "manifest_snapshot_integrity/fixture_fail_receipts_symlink" 2 \
        bash "$SCRIPT_DIR/manifest_snapshot_integrity_guard.sh" --root "$SCRIPT_DIR/fixtures/manifest-snapshot-integrity/fail_receipts_symlink"
fi

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "=== Results ==="
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "run-all-guards: FAIL — $FAIL guard(s) did not match expected exit code" >&2
    exit 1
fi

echo "run-all-guards: all guards PASS"
exit 0
