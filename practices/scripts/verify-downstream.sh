#!/usr/bin/env bash
# practices/scripts/verify-downstream.sh — downstream-fixture E2E harness (Layer 1).
#
# WHY THIS EXISTS
# ---------------
# Every other gate in this catalog measures ax-template's OWN tree. Nothing measured whether the
# artifacts ax-transform's install skills hand to a CONSUMER project actually run there — the
# skills' own "verified empirically" prose ages silently the moment a snippet is edited, and a
# green ax-template R25 says nothing at all about a stock Spring Initializr + stock Next.js repo
# whose `react.root`/`java.root` are NOT `"."`. That gap is where GH #78/#79/#82/#84/#86/#90 all
# lived: each one was a defect that reproduced only in a downstream shape.
#
# This script closes that by MATERIALIZING the install skills' artifacts — extracted from the
# `<!-- ax:artifact ... -->` markers in skills/ax-install-{hooks,java-enforcement,react-enforcement}
# /SKILL.md via practices/scripts/lib/ax_markers.py, never copy-pasted here — into a throwaway,
# out-of-repo consumer project built from practices/evals/fixtures/consumer-e2e/project/, and then
# ASKING THE INSTALLED GATES TO BLOCK REAL VIOLATIONS. There is exactly one copy of every artifact
# body (the skill file); a snippet edited there is what this harness runs tomorrow.
#
# EVERY ASSERTION PAIRS AN EXIT CODE WITH THE GATE'S OWN SIGNAL STRING (fixture_kill_proof_guard
# [85]'s rule: an exit code alone is not evidence). `git commit` exits non-zero for a dozen reasons
# that have nothing to do with a gate firing — "the commit failed" and "the ax gate rejected this
# commit for the reason we planted" are different claims, and only the second one is worth
# anything. Likewise a `0 problems` ESLint run prints NOTHING in the default stylish format, so A2
# asserts on parsed `--format json` (results > 0 AND errorCount == 0) — the `results > 0` half is
# what rules out "the glob matched no files and passed vacuously".
#
# HONEST SCOPE — printed here and RE-PRINTED at the end of every run:
#   (This block is the `--help` view; print_scope() below is what a RUN emits at its head and tail.
#   They describe the same facts and must not drift — the two bullets that follow had gone stale
#   against print_scope() by claiming ONE shape and only branch A, which P2-103/P2-104 had already
#   made false.)
#   · THREE shapes are materialized, installed and RUN, on Gradle 9.5.1 + whatever Node/npm the
#     calling machine has: (1) `react.root="frontend"` + `java.root="backend"` + TypeScript ON +
#     the default gate task; (2) `stacks=["java"]` with a RENAMED `java.testTask`; (3)
#     `react.root="."`. UNMEASURED: `react.typescript=false` (not merely unmeasured — KNOWN BROKEN,
#     see the P2-104 note in docs/BACKLOG.md), other Gradle/Node versions, Windows.
#   · It installs the skills' MACHINE-EXTRACTABLE artifacts (the ax:artifact-marked blocks). What
#     remains PROSE, and is therefore NOT executed here: the stack/layout detection heuristics, the
#     manual probe→detect→delete verification, and the F-2 MIGRATION's three mutating git commands.
#     The F-2 CONDITION is no longer prose — since P2-122 it is the `hook-worktree-preflight`
#     artifact, executed in both directions by A15b-preflight. All THREE hook branches ARE
#     exercised (A `core.hooksPath` plus its linked-worktree variant, B husky, C lefthook), each
#     running the one rendered hook-body.
#   · It is NOT R25. It does not run run-all-guards.sh, the per-domain gradle tasks, or
#     ax-template's own frontend lint.
#   · Two things the fixture does not commit are injected by THIS script at materialization time
#     and are therefore harness decisions, not consumer-shape facts: the Gradle wrapper
#     (`gradlew` + `gradle-wrapper.jar` copied from backend/, with a GENERATED
#     `gradle-wrapper.properties` pinning gradle-9.5.1-bin.zip), and `GRADLE_OPTS`
#     -Dorg.gradle.daemon=false (so no daemon outlives the temp tree).
#   · Placement of a `kind=file-fragment` is now DECLARED BY THE MARKER, not inferred here
#     (P2-112): every fragment carries `merge=` (one of json-deep / gradle-dependencies / append /
#     replace, enforced by ax_markers.lint()), and this script only executes what it reads. An
#     unregistered value is a loud failure, never a fallback to append. What remains unmeasured is
#     narrower than before but real: this script executes the declared rule mechanically, whereas a
#     consumer following the skill by hand executes it by reading — the two agree by contract, not
#     by measurement.
#
# WHAT IT MEASURES (each id is logged individually to .ax-downstream/runs.jsonl):
#   A-pc  positive control — the react gate BLOCKS a planted violation and the hook banner proves
#         the hook actually executed. If this ever passes-through, every other "BLOCKED" verdict
#         in the run is unearned, so the run is a REAL FINDING.
#   A0    ./gradlew compileTestJava -PaxRootPackage=... exits 0 AND the installed ArchUnit class
#         is actually compiled into build/classes/java/test.
#   A1    a shared -> app upward import is BLOCKED at commit AND `ax/no-upward-layer-import` names
#         the rule in the output.
#   A2    the same path, violation removed, COMMITS, and `npx eslint <src> --format json` parses to
#         results > 0 AND total errorCount == 0.
#   A3    a ProbeService -> ProbeController dependency is BLOCKED at commit AND the probe class
#         name appears in the output.
#   A4    the same path, violation removed, COMMITS, AND build/test-results/testPractices/*.xml
#         reports tests > 0 (a gate that ran zero tests is not a gate).
#   A5    a file outside BOTH roots commits, the hook banner IS present, and NEITHER the npm nor
#         the gradle marker is — PLUGIN-CHANNEL rule 6 ("no catalog outside the roots").
#   A6    A5's premise: >= 1 file was actually staged and the hook actually ran.
#   A7    ./gradlew tasks --all WITHOUT -P exits 0 AND lists testPractices (GH #90: a
#         configuration-time `error(...)` held every task in the build hostage).
#   A7b   A7's trigger premise: the fixture's build.gradle.kts really does carry the eager
#         `tasks.withType<Test>` block that realizes every Test task at configuration time.
#   A8    ./gradlew test -PaxRootPackage=... exits 0, build/test-results/test/*.xml reports
#         tests > 0, AND LayerBoundaryArchTest is ABSENT from them (GH #91 gate isolation).
#   A9-eval    EVALUATOR DIFFERENTIAL (P2-114) — the same markers rendered against TWO configs, so
#         a conditional is seen to be conditional: hook-body's `ax:if config.stacks.react` region
#         present under stacks=[react,java] and gone under stacks=[java]; its
#         `ax:subst config.java.testTask` yielding a DIFFERENT task name in each; the java region
#         present in both; react-eslint-config's tseslint wiring present at typescript=true and
#         gone at false; and zero residual directives / unsubstituted @@tokens@@ in all four
#         renders. Everything else here renders one config only, where a condition that never
#         varies is indistinguishable from one that does.
#   A11-route  THE NEXT/APP-ROUTER RULE AXIS (P2-105) — one planted `"use client"` route file at
#         `<srcDir>/app/**/page.tsx` is BLOCKED at commit and the output names ALL THREE
#         route-axis rule ids: ax/no-route-client-data-fetching, ax/no-server-state-in-local-state,
#         ax/no-god-route. A1/A2 exercise only ax/no-upward-layer-import, whose whole input is an
#         import string; these three additionally require route detection (`isRouteFile`) and the
#         `"use client"` directive to be seen, so a broken layers mapping or a settings block that
#         never reached them would leave them inert while A1/A2 stayed green.
#   A12-route  A11's pass-after half: the same route path, violations removed, COMMITS. Without it,
#         A11 is equally satisfied by a gate that rejects every route file it sees.
#   A13-husky  HOOK BRANCH B (P2-103) — the SAME body already on disk (the rendered hook-body
#         marker, not a copy) re-wired through `npx husky init`'s own core.hooksPath refuses A1's
#         probe, with banner + rule id. Honest limit printed at run time: husky's `_/h` shim ends in
#         `sh -e "$s"`, so the body runs under the PLATFORM /bin/sh with its shebang DISCARDED.
#   A13b-posix A13's UNMEASURED HALF (P2-123) — A13 can only ever measure the CALLING platform's
#         /bin/sh, and on macOS that is bash-in-POSIX-mode, which accepts `set -o pipefail`. A dash
#         /bin/sh (Debian/Ubuntu, where most CI runs) does not: it aborts at the body's second line
#         with `set: Illegal option -o pipefail`, BEFORE the banner, so the gate never runs and the
#         commit sails through. Two halves: (i) STATIC, always measured — the rendered body on disk
#         carries none of a registered bashism list (`-o pipefail`, `[[`, `local `, `<<<`, `declare`,
#         `${x^^}`, `+=(`); (ii) DYNAMIC, measured whenever a dash-class shell exists — that exact
#         on-disk body, executed as husky executes it (`<posix-sh> -e <body>`, shebang discarded),
#         prints the F-034 banner and exits 0 in a throwaway repo with nothing staged. The dynamic
#         half is skipped ONLY when no dash-class shell is installed, and that is printed as
#         UNMEASURED rather than folded silently into the verdict.
#   A15b-preflight THE F-2 PREFLIGHT CONDITION, EXECUTED (P2-122) — A15 measures the worktree
#         WIRING but treats F-2 as a premise, so the prose that decides whether to migrate was
#         never run by anything. It is now the `hook-worktree-preflight` command artifact, and this
#         assertion renders it from SKILL.md and executes it in BOTH directions: an ordinary
#         `git init` checkout (where `git init` itself wrote `core.bare = false`) must print
#         MIGRATION NOT REQUIRED, and a `git init --bare` repo must print MIGRATION REQUIRED. Both
#         directions are required, which is what makes it non-vacuous — a detector that always
#         printed one verdict satisfies neither pair. The defect this closes: the prose said "if
#         EITHER prints a value, migrate", and `false` IS a value, so the literal instruction fired
#         on every normal checkout and its migration wrote `core.bare true` into config.worktree —
#         declaring a non-bare repository BARE.
#   A14-lefthook HOOK BRANCH C (P2-103) — same body, reached through `lefthook.yml`'s
#         `run: bash .githooks/ax-pre-commit-checks.sh`, refuses the same probe. Also asserts the
#         CALLING USER'S global hooks directory was not written to: `lefthook install` targets the
#         EFFECTIVE core.hooksPath, and with a global one set that is outside this temp tree
#         (reproduced — it clobbered a real global hook — hence the repo-local wiring first).
#   A15-worktree BRANCH A's LINKED-WORKTREE VARIANT (P2-103, D-8/F-2/F-3) — `.git` is a FILE there,
#         `extensions.worktreeConfig` + `--worktree core.hooksPath` makes git invoke the hook (F-034
#         banner on an out-of-root commit, the only probe needing neither node_modules nor a gradle
#         build inside the worktree), the main worktree's `--show-origin --get core.hooksPath` is
#         byte-identical before/after, and exactly ONE new config.worktree exists. The F-2
#         core.bare/core.worktree migration is measured as a PREMISE (both empty here = not
#         applicable); a bare-main worktree farm stays unmeasured.
#   A16-alttask A SECOND SHAPE, INSTALLED AND EXECUTED (P2-104) — stacks=["java"] and
#         java.testTask="verifyAxPractices": the hook body on disk carries NO react region, the gate
#         task is discoverable under the CONFIGURED name (and the default name is absent), and a
#         planted java violation is refused BY THAT TASK, naming the probe class. A9-eval renders two
#         configs; this one installs and invokes the second. It found F-035 (the gate task name was
#         hardcoded while the hook honoured java.testTask, so any non-default name meant
#         `Task 'x' not found` on every java commit and the gate never ran).
#   A17-alttask A16's pass-after half plus A4's non-vacuity check under the renamed task: the repair
#         COMMITS and build/test-results/<configured name>/*.xml reports tests > 0.
#   A18-rootdot A THIRD SHAPE: react.root="." (P2-104a) — the fixture with its frontend relocated to
#         the repo root. Selects two previously-dead code paths: eslint.config.mjs's findAxConfig()
#         must resolve ax.config.json on its FIRST iteration (a sibling, not a parent — F-030), and
#         the hook body's `[ "$REACT_ROOT" = "." ]` FILE-EXTENSION scope proxy replaces the path
#         prefix. A planted upward import is BLOCKED, the banner shows `react=.`, the rule is named.
#   A19-rootdot A18's pass-after half with A2's non-vacuity check (eslint results > 0 AND
#         errorCount == 0) — at react.root="." the srcDir glob resolves from a different directory,
#         so "matched zero files" is a live way for this shape to look clean while linting nothing.
#   A20-rootdot-skip the OTHER half of the `"."` branch: a staged file with no react extension must
#         skip the react gate while the F-034 banner still proves the hook ran. A5 makes this claim
#         for the path-prefix branch; at react.root="." "outside the root" is not even expressible,
#         so the extension proxy is the only thing that can express it.
#   A21-plugindep THE PLUGIN IS INSTALLED WHERE THE WORKSPACE DECLARES ITS DEPENDENCIES (P2-121) —
#         react-plugin-dep used to declare `base=repo`, so `npm i -D file:<plugin>` ran at the REPO
#         ROOT and npm recorded the ax plugin in a repo-root package.json that no consumer CI ever
#         installs. Every assertion above stayed green anyway, because Node resolves
#         `@ax/eslint-plugin-ax` by walking UP into the repo-root node_modules — the gate worked by
#         accident of one machine's layout. This assertion refuses that accident on three counts:
#         (i) `<react.root>/package.json` AND `<react.root>/package-lock.json` both name the plugin;
#         (ii) nothing outside `<react.root>` declares it (measured BEFORE the hook branches create
#         their own repo-root package.json); (iii) after `rm -rf node_modules` at BOTH levels and
#         `npm ci` run ONLY inside `<react.root>` — a stock workspace CI — A1's probe is still
#         BLOCKED with banner + rule id. Pre-fix, (i) and (iii) both go RED: the lockfile has zero
#         references and the import dies with `Cannot find module '@ax/eslint-plugin-ax'`.
#   A10-tsdep  react-ts-eslint-dep is NON-VACUOUS (P2-113) — typescript-eslint is UNRESOLVABLE
#         after `npm ci` (the fixture no longer ships it) and RESOLVABLE after the artifact runs.
#         Only that transition attributes the package's presence to the marker; while the fixture
#         carried it as a baseline devDependency, skipping or breaking the marker changed nothing.
#
# ASSERTION MANIFEST — machine-readable, single source of truth, ONE line:
# ax:assertions A-pc A0 A1 A2 A3 A4 A5 A6 A7 A7b A8 A9-eval A10-tsdep A11-route A12-route A13-husky A13b-posix A14-lefthook A15-worktree A15b-preflight A16-alttask A17-alttask A18-rootdot A19-rootdot A20-rootdot-skip A21-plugindep
#   guard [114] PARSES that line out of this file (at the pushed sha, via `git show`) and requires
#   the audit log's `assertions` key set to match it EXACTLY — missing OR extra both BLOCK. Without
#   it, [114] could only check "every assertion that happens to be RECORDED is true", which a
#   one-line forgery (`{"forged-single": true}`) satisfies trivially; that was a real, reproduced
#   hole in [114] before this manifest existed.
#   A DECLARATION THAT DRIFTS FROM REALITY WOULD BE JUST AS VACUOUS, so this script CROSS-CHECKS
#   itself at run time (step 4b below): the ids actually recorded by note() must equal the ids
#   declared above, or the run fails with exit 8 and never reads as green. Add a note() call
#   without touching the line above and the very next run says so.
#
# LOG: one line appended to .ax-downstream/runs.jsonl per run, pass or fail, in the schema guard
# [114] (practices/evals/downstream_release_recency_guard.sh) already implements: head_sha,
# tree_clean, assertions{id->bool}, artifact_digests{artifact id -> sha256 of the marker body
# actually installed}. The digest map covers EVERY marker discover() finds — never a hardcoded
# count.
#
# --artifact-override <id>=<file> replaces one artifact's body with a file's contents before
# rendering (this is how a pre-fix shape is re-injected to prove an assertion actually goes RED
# without it). Such a run is recorded with a non-empty "override" list, AND — because
# artifact_digests logs the digest of what was really installed — guard [114]'s recompute against
# the SKILL.md files will not match, so an override run can never be mistaken for release
# evidence even if every assertion in it happens to be true.
#
# Usage:
#   bash practices/scripts/verify-downstream.sh
#   bash practices/scripts/verify-downstream.sh --artifact-override hook-body=/tmp/prefix-hook.txt
#   bash practices/scripts/verify-downstream.sh --keep      # leave the temp project for inspection
#
# Exit codes:
#   0  every assertion held
#   1  REAL FINDING — at least one assertion did not hold
#   2  usage error
#   3  toolchain missing (JDK 21 / node / npm / git / python3) — NOT a skip
#   4  network unreachable (npm registry / services.gradle.org) — NOT a skip
#   5  the temporary tree could not be removed (leak)
#   6  artifact extraction or installation failed
#   7  a fixture file this run DEPENDS ON is not tracked in HEAD — the run would be measuring a
#      tree nobody else can reconstruct (see the premise probe) — NOT a skip
#   8  assertion-manifest drift: the ids recorded by note() do not equal the `# ax:assertions`
#      declaration above, so guard [114]'s completeness check would be checking the wrong set
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURE_SRC="$REPO_ROOT/practices/evals/fixtures/consumer-e2e/project"
PLUGIN_PATH="$REPO_ROOT/practices-react/eslint-plugin-ax"
LOG_DIR="$REPO_ROOT/.ax-downstream"
LOG_FILE="$LOG_DIR/runs.jsonl"
GRADLE_DIST_URL='https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip'

KEEP=0
OVERRIDE_ARGS=""      # newline-separated "id=file" (bash 3.2: no associative arrays)

print_scope() {
    echo "  · THREE consumer shapes are materialized, installed and RUN: (1) react.root=frontend +"
    echo "    java.root=backend + TypeScript ON + default gate task; (2) java-only stacks with a"
    echo "    RENAMED java.testTask; (3) react.root=\".\" with the frontend at the repo root."
    echo "    UNMEASURED: react.typescript=false, other Gradle/Node versions, Windows."
    echo "    react.typescript=false is not merely unmeasured — it is KNOWN BROKEN"
    echo "    (ESLint 9's default espree does not enable JSX, so every .jsx file in a plain-JS"
    echo "    consumer is a fatal parse error); see the P2-104 note in docs/BACKLOG.md."
    echo "  · The install skills' PROSE steps (detection heuristics, manual probe->detect->delete"
    echo "    verification) are NOT executed — only the ax:artifact-marked blocks. The F-2"
    echo "    preflight CONDITION is now an artifact and IS executed both ways (A15b-preflight);"
    echo "    what stays unmeasured is the MIGRATION itself — the three mutating git commands a"
    echo "    bare-main worktree farm would then run."
    echo "  · Hook wiring: all THREE branches are exercised (A core.hooksPath, B husky, C lefthook)"
    echo "    plus branch A's linked-worktree variant, all running the ONE rendered hook-body."
    echo "    Harness-injected for B/C (declared, not consumer-shape): a minimal repo-root"
    echo "    package.json, and lefthook.yml's 5 lines of lefthook-schema wiring. husky executes"
    echo "    the body with 'sh -e' (shebang discarded), so A13 itself is measured only under THIS"
    echo "    platform's /bin/sh; A13b-posix re-runs the same on-disk body under a dash-class shell"
    echo "    when one is installed, and says UNMEASURED when none is. Neither covers a real"
    echo "    Debian/Ubuntu CI end-to-end — only the shell, not the platform."
    echo "  · This is NOT R25: no run-all-guards.sh, no per-domain gradle task, no ax-template lint."
    echo "  · Harness-injected, not consumer-shape: the Gradle wrapper (copied gradlew +"
    echo "    gradle-wrapper.jar, GENERATED gradle-wrapper.properties pinned to 9.5.1) and"
    echo "    GRADLE_OPTS=-Dorg.gradle.daemon=false."
    echo "  · kind=file-fragment placement is DECLARED BY THE MARKER (merge=json-deep |"
    echo "    gradle-dependencies | append | replace) and executed here verbatim; an unregistered"
    echo "    value fails loudly. Still unmeasured: that a HUMAN reading the skill performs the"
    echo "    declared merge the same way this script does — they agree by contract, not measurement."
}

while [ $# -gt 0 ]; do
    case "$1" in
        --artifact-override)
            OVERRIDE_ARGS="$OVERRIDE_ARGS
${2:?--artifact-override needs <id>=<file>}"; shift 2 ;;
        --artifact-override=*)
            OVERRIDE_ARGS="$OVERRIDE_ARGS
${1#--artifact-override=}"; shift ;;
        --keep) KEEP=1; shift ;;
        # Print the whole header block, however long it grows — never a hardcoded line count.
        -h|--help) awk 'NR>1 && /^[^#]/ {exit} NR>1' "$0"; exit 0 ;;
        *) echo "verify-downstream: unknown arg: $1" >&2; exit 2 ;;
    esac
done

echo "═══ verify-downstream (downstream-fixture E2E, Layer 1) ═══"
echo "  repo    : $REPO_ROOT"
echo "  fixture : $FIXTURE_SRC"
echo "═══ HONEST SCOPE — what this run does NOT say ═══"
print_scope
echo ""

[ -d "$FIXTURE_SRC" ] || { echo "verify-downstream: fixture source missing: $FIXTURE_SRC" >&2; exit 2; }
[ -d "$PLUGIN_PATH" ] || { echo "verify-downstream: plugin missing: $PLUGIN_PATH" >&2; exit 2; }

# ── work dir + teardown (created BEFORE the probes so a probe failure still logs) ────────────
WORK="$(mktemp -d "${TMPDIR:-/tmp}/ax-downstream.XXXXXX")" || {
    echo "verify-downstream: cannot create a work directory" >&2; exit 2; }
PROJ="$WORK/project"
ASSERT_FILE="$WORK/assertions.tsv"
: > "$ASSERT_FILE"

teardown() {
    if [ "$KEEP" = "1" ]; then
        echo "  (temp tree left in place on request: $WORK)"
        return
    fi
    rm -rf "$WORK" 2>/dev/null
    # LEAK CHECK — asked, not assumed. A harness that materializes a full npm+gradle project on
    # every run and silently leaves it behind turns "run it again" into a disk-filling habit.
    if [ -d "$WORK" ]; then
        echo "  LEAK: $WORK could not be removed — delete it by hand." >&2
        exit 5
    fi
}
trap teardown EXIT

# ── logging (written even when a premise fails, so an unmeasured run is visible as unmeasured) ─
cat > "$WORK/log_run.py" <<'PYEOF'
import hashlib
import json
import os
import sys
import time

log_file, assert_file, digest_file, override_list, head_sha, tree_clean, verdict = sys.argv[1:8]

assertions = {}
if os.path.isfile(assert_file):
    with open(assert_file, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            key, _, val = line.partition("\t")
            assertions[key] = (val == "true")

digests = {}
if os.path.isfile(digest_file):
    with open(digest_file, encoding="utf-8") as f:
        digests = json.load(f).get("digests", {})

entry = {
    "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
    "head_sha": head_sha,
    "tree_clean": (tree_clean == "true"),
    "assertions": assertions,
    "artifact_digests": digests,
    # Non-empty => this run installed at least one artifact body that is NOT what the SKILL.md
    # carries. It is a regression differential, never release evidence. artifact_digests above
    # records what was ACTUALLY installed, so guard [114]'s recompute from the skills will
    # mismatch and BLOCK regardless of how green the assertions look.
    "override": [x for x in override_list.split(",") if x],
    "verdict": verdict,
    "harness": "practices/scripts/verify-downstream.sh",
}
os.makedirs(os.path.dirname(log_file), exist_ok=True)
with open(log_file, "a", encoding="utf-8") as f:
    f.write(json.dumps(entry, sort_keys=False) + "\n")
print("  audit line appended: %s (assertions=%d, digests=%d, override=%s)"
      % (log_file, len(assertions), len(digests),
         entry["override"] or "none"))
PYEOF

HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null)"
# tree_clean: the repo's own working tree, EXCLUDING this harness's own log directory (which the
# orchestrator gitignores; filtering it here keeps a first-run log from making the run it is
# recording look dirty). Measured BEFORE anything else runs — nothing in this script writes into
# the repo other than that log.
DIRTY_LINES="$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null | grep -v '\.ax-downstream/' | wc -l | tr -d ' ')"
TREE_CLEAN=false
[ "$DIRTY_LINES" = "0" ] && TREE_CLEAN=true

OVERRIDE_IDS=""
finish() {   # finish <exit-code> <verdict>
    local code="$1" verdict="$2"
    python3 "$WORK/log_run.py" "$LOG_FILE" "$ASSERT_FILE" "$WORK/install.json" \
        "$OVERRIDE_IDS" "$HEAD_SHA" "$TREE_CLEAN" "$verdict" || true
    echo ""
    echo "═══ COVERAGE — what this run does NOT say (re-printed) ═══"
    print_scope
    exit "$code"
}

note() {  # note <assertion-id> <true|false>
    printf '%s\t%s\n' "$1" "$2" >> "$ASSERT_FILE"
    if [ "$2" = "true" ]; then
        echo "  PASS [$1]"
    else
        echo "  FAIL [$1]"
    fi
}

# ── 1. PREMISE PROBE — measured, never assumed; a missing premise is a LOUD failure, not a skip ─
echo "── premise probe ─────────────────────────────────────────"
PROBE_FAIL=0
for tool in git python3 node npm curl; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "  MISSING: $tool" >&2
        PROBE_FAIL=1
    else
        echo "  ok: $tool ($(command -v "$tool"))"
    fi
done

# JDK 21: resolve the JVM GRADLE will actually use — JAVA_HOME wins over PATH for the wrapper, so
# probing `java` on PATH while JAVA_HOME points elsewhere would measure the wrong runtime. The
# macOS /usr/bin/java stub with no runtime installed fails here (it exits non-zero and prints
# "Unable to locate a Java Runtime"), which is the intended verdict, not a skip.
JAVA_BIN=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(command -v java)"
fi
JAVA_MAJOR=""
if [ -n "$JAVA_BIN" ]; then
    JAVA_VER_RAW="$("$JAVA_BIN" -version 2>&1)"
    JAVA_MAJOR="$(printf '%s\n' "$JAVA_VER_RAW" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)"
fi
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 21 ] 2>/dev/null; then
    echo "  MISSING: a JDK 21+ runtime (resolved java: ${JAVA_BIN:-none}, major: ${JAVA_MAJOR:-unresolved})" >&2
    echo "    The fixture's build.gradle.kts pins JavaLanguageVersion.of(21). Point JAVA_HOME at a" >&2
    echo "    real JDK 21 and re-run, e.g." >&2
    echo "    JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home bash $0" >&2
    PROBE_FAIL=1
else
    echo "  ok: JDK $JAVA_MAJOR ($JAVA_BIN)"
fi
if [ "$PROBE_FAIL" != "0" ]; then
    echo "verify-downstream: TOOLCHAIN INCOMPLETE — no measurement was taken (this is a failure," >&2
    echo "  not a skip: an unmeasured run must never read as a green one)." >&2
    finish 3 "toolchain-missing"
fi

# FIXTURE REPRODUCIBILITY PREMISE — `npm ci` (below) REFUSES to run without frontend/package-lock
# .json, so this run's very first step depends on that file. Existing ON DISK is NOT enough: a
# lockfile that is present locally but absent from the repository makes every assertion in this run
# irreproducible on a clean clone or a CI runner, while the run itself still prints a full green.
# That is not hypothetical — it is exactly what happened: a MACHINE-LEVEL gitignore
# (core.excludesFile) carries `package-lock.json`, `git add -A` skipped it SILENTLY, and this
# harness's 11/11 PASS was produced by an uncommitted file. So the premise checked here is
# TRACKED IN HEAD, not merely readable. (Guard [115],
# practices/evals/fixture_tracked_completeness_guard.sh, enforces the same invariant across the
# whole fixtures tree; this probe is the local, loud version for the one file this run cannot start
# without.)
LOCKFILE="$FIXTURE_SRC/frontend/package-lock.json"
LOCK_REL="${LOCKFILE#$REPO_ROOT/}"
if [ ! -f "$LOCKFILE" ]; then
    echo "  MISSING: $LOCK_REL (npm ci cannot run without it)" >&2
    echo "verify-downstream: FIXTURE PREMISE NOT ESTABLISHED — no measurement was taken." >&2
    finish 7 "fixture-lockfile-missing"
elif ! git -C "$REPO_ROOT" cat-file -e "HEAD:$LOCK_REL" 2>/dev/null; then
    echo "  UNTRACKED: $LOCK_REL exists on disk but is NOT in HEAD." >&2
    echo "    Everything this run measures would rest on a file only this machine has." >&2
    IGNORED_BY="$(git -C "$REPO_ROOT" check-ignore -v -- "$LOCK_REL" 2>/dev/null)"
    if [ -n "$IGNORED_BY" ]; then
        echo "    git check-ignore -v says: $IGNORED_BY" >&2
        echo "    That rule is machine-level, and \`git add -A\` obeys it SILENTLY." >&2
    fi
    echo "    FIX: git add -f \"$LOCK_REL\" && git commit" >&2
    echo "verify-downstream: FIXTURE PREMISE NOT ESTABLISHED — no measurement was taken (this is" >&2
    echo "  a failure, not a skip: an unreproducible run must never read as a green one)." >&2
    finish 7 "fixture-lockfile-untracked"
else
    echo "  ok: $LOCK_REL tracked in HEAD"
fi

for url in "https://registry.npmjs.org/" "https://services.gradle.org/distributions/"; do
    if curl -sS -I -m 25 -o /dev/null "$url" 2>/dev/null; then
        echo "  ok: reachable $url"
    else
        echo "  UNREACHABLE: $url" >&2
        echo "verify-downstream: NETWORK UNREACHABLE — no measurement was taken." >&2
        finish 4 "network-unreachable"
    fi
done
echo ""

# ── 2. MATERIALIZE the consumer project OUTSIDE the repo tree ────────────────────────────────
# Outside on purpose: run-all-guards.sh and R25 both assert on `git status --porcelain`, so a
# harness that scribbled inside the repo would make every concurrent gate spuriously fail.
echo "── materialize ───────────────────────────────────────────"
mkdir -p "$PROJ"
( cd "$FIXTURE_SRC" && tar cf - . ) | ( cd "$PROJ" && tar xf - ) || {
    echo "verify-downstream: could not copy the fixture into $PROJ" >&2; finish 6 "materialize-failed"; }

BACKEND="$PROJ/backend"
FRONTEND="$PROJ/frontend"

# Wrapper injection (the fixture commits ZERO binaries — see its README). backend/gradlew and
# gradle-wrapper.jar ship pinned to 8.14.5; only distributionUrl is overridden, to 9.5.1.
mkdir -p "$BACKEND/gradle/wrapper"
cp "$REPO_ROOT/backend/gradlew" "$BACKEND/gradlew" || { echo "verify-downstream: no backend/gradlew" >&2; finish 6 "wrapper-missing"; }
chmod +x "$BACKEND/gradlew"
cp "$REPO_ROOT/backend/gradle/wrapper/gradle-wrapper.jar" "$BACKEND/gradle/wrapper/gradle-wrapper.jar" || {
    echo "verify-downstream: no backend/gradle/wrapper/gradle-wrapper.jar" >&2; finish 6 "wrapper-missing"; }
cat > "$BACKEND/gradle/wrapper/gradle-wrapper.properties" <<EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=$GRADLE_DIST_URL
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
echo "  wrapper injected, distributionUrl pinned to gradle-9.5.1-bin.zip"

# No daemon: a daemon outliving the temp tree is exactly the leak the teardown check exists to
# catch. Exported (not written into the tree) so the git hook's own gradle call inherits it too.
export GRADLE_OPTS="-Dorg.gradle.daemon=false"
export JAVA_HOME="$(cd "$(dirname "$(dirname "$JAVA_BIN")")" && pwd)"

pgit() {   # every git call: identity supplied with -c, never read from global config
    git -c user.name="ax-downstream" -c user.email="ax-downstream@example.invalid" \
        -c commit.gpgsign=false -c init.defaultBranch=main -C "$PROJ" "$@"
}
pgit init -q || { echo "verify-downstream: git init failed" >&2; finish 6 "git-init-failed"; }
pgit add -A >/dev/null 2>&1
pgit commit -q --no-verify -m "fixture baseline" >/dev/null 2>&1 || {
    echo "verify-downstream: baseline commit failed" >&2; finish 6 "git-init-failed"; }
echo "  git repo initialized, baseline committed"

echo "  npm ci in frontend/ …"
( cd "$FRONTEND" && npm ci ) > "$WORK/npm-ci.log" 2>&1
NPM_CI_RC=$?
if [ "$NPM_CI_RC" != "0" ]; then
    echo "verify-downstream: npm ci failed in the fixture frontend (rc=$NPM_CI_RC):" >&2
    tail -30 "$WORK/npm-ci.log" >&2
    finish 6 "npm-ci-failed"
fi
echo "  npm ci ok"

# A10-tsdep, FIRST HALF — measured HERE, before any artifact is installed, because this is the only
# moment the "before" state exists. `npm ci` has just materialized the fixture's committed
# dependency graph; `typescript-eslint` must NOT be in it (P2-113: it used to be, which made the
# skill's react-ts-eslint-dep artifact unverifiable — the package was already present no matter
# what that marker did, so breaking or skipping it changed nothing observable).
ts_resolvable() {   # 0 = resolvable from the frontend, 1 = not
    ( cd "$FRONTEND" && node -e 'require.resolve("typescript-eslint",{paths:[process.cwd()]})' ) \
        >/dev/null 2>&1
}
TSDEP_PRE=1
ts_resolvable && TSDEP_PRE=0
echo "  pre-install: typescript-eslint resolvable = $([ "$TSDEP_PRE" = 0 ] && echo yes || echo no)"
echo ""

# ── 3. INSTALL the artifacts — extracted from the skills, never copy-pasted here ─────────────
cat > "$WORK/install.py" <<'PYEOF'
"""Materialize every ax:artifact marker into the consumer project.

Zero artifact bodies live in this file: everything comes from ax_markers.discover() over the
skills as they exist on disk right now, so an edited snippet is what gets installed.
"""
import glob
import hashlib
import json
import os
import subprocess
import sys

repo_root, proj, plugin_path, overrides_path, out_path = sys.argv[1:6]
sys.path.insert(0, os.path.join(repo_root, "practices", "scripts", "lib"))
import ax_markers  # noqa: E402


def die(msg, *extra):
    print("install: " + msg, file=sys.stderr)
    for line in extra:
        print("  " + str(line), file=sys.stderr)
    sys.exit(6)


overrides = {}
if overrides_path != "-":
    with open(overrides_path, encoding="utf-8") as f:
        for spec in json.load(f):
            aid, sep, path = spec.partition("=")
            if not sep:
                die("--artifact-override %r is not of the form <id>=<file>" % spec)
            if not os.path.isfile(path):
                die("--artifact-override file does not exist: %s" % path)
            with open(path, encoding="utf-8") as fh:
                overrides[aid] = fh.read()

# Same discovery surface guard [114] recomputes against: every skills/*/SKILL.md, not a
# hand-listed subset and never a hardcoded artifact count.
skill_paths = sorted(glob.glob(os.path.join(repo_root, "skills", "*", "SKILL.md")))
if not skill_paths:
    die("no skills/*/SKILL.md found under %s" % repo_root)
artifacts = ax_markers.discover(skill_paths)
if not artifacts:
    die("discover() found ZERO ax:artifact markers across %d skill files — nothing to install, "
        "so this run would 'pass' having installed nothing." % len(skill_paths))

# Linted WITH the schema: without config_schema_path the UNKNOWN_CONFIG_PATH check is skipped
# entirely, and a marker whose `when=`/`ax:if` names a key ax.config.json has no schema for is
# FALSY FOREVER — its block is deleted on every project and looks exactly like a deliberately
# disabled feature. That is the one marker defect this harness could otherwise install and then
# measure as "green".
problems = ax_markers.lint(artifacts, config_schema_path=os.path.join(
    repo_root, "practices-react", "eslint-plugin-ax", "schemas", "ax.config.schema.json"))
if problems:
    die("the marker tree does not lint; refusing to install a partially-understood set",
        *["%s:%d: %s: %s" % (p.source_file, p.line, p.code, p.message) for p in problems])

unknown = sorted(set(overrides) - {a.id for a in artifacts})
if unknown:
    die("--artifact-override names id(s) no marker declares: %s" % unknown)

with open(os.path.join(proj, "ax.config.json"), encoding="utf-8") as f:
    config = json.load(f)
env = {"axPluginPath": plugin_path}

roots = {
    "repo": proj,
    "java.root": os.path.join(proj, config["java"]["root"]),
    "react.root": os.path.join(proj, config["react"]["root"]),
}

digests = {}
for a in artifacts:
    if not a.id:
        continue
    if a.id in overrides:
        a.body = overrides[a.id]
    digests[a.id] = hashlib.sha256(a.body.encode("utf-8")).hexdigest()


def merge_json(base, incoming):
    for k, v in incoming.items():
        if isinstance(v, dict) and isinstance(base.get(k), dict):
            merge_json(base[k], v)
        else:
            base[k] = v
    return base


def apply_merge(target, text, aid, mode):
    """Execute the merge mode the MARKER declared (P2-112).

    This function used to DECIDE the mode by sniffing — package.json basename -> JSON deep-merge,
    a *.gradle.kts fragment whose every meaningful line started with a dependency-notation prefix
    -> dependencies-block injection, everything else -> append. That made the placement rule a
    property of this harness rather than of the artifact it is about: the marker could not say
    what it meant, and a skill author changing a fragment's shape (adding a `constraints { }`
    line to a dependency fragment, say) silently changed where it landed. `merge=` now carries the
    rule, ax_markers.lint() enforces that every kind=file-fragment declares one, and this function
    only OBEYS. An unregistered value dies loudly rather than falling back to append — a quiet
    fallback would restore exactly the guess this attribute exists to delete.
    """
    if mode not in ax_markers.REGISTERED_MERGES:
        die("artifact %r declares merge=%r, which this harness does not implement (registered: "
            "%s) — refusing to guess a placement" % (aid, mode, sorted(ax_markers.REGISTERED_MERGES)))

    if mode == "replace":
        os.makedirs(os.path.dirname(target) or ".", exist_ok=True)
        with open(target, "w", encoding="utf-8") as f:
            f.write(text.rstrip("\n") + "\n")
        return "replace"

    # Every remaining mode combines with content that must already be there.
    if not os.path.isfile(target):
        die("artifact %r declares merge=%r but %s does not exist in the consumer project"
            % (aid, mode, target))

    if mode == "json-deep":
        with open(target, encoding="utf-8") as f:
            base = json.load(f)
        stripped = "\n".join(ln for ln in text.split("\n")
                             if not ln.strip().startswith("//"))
        try:
            incoming = json.loads(stripped)
        except ValueError as exc:
            die("fragment %r declares merge=json-deep but is not JSON-mergeable into %s: %s"
                % (aid, target, exc))
        merge_json(base, incoming)
        with open(target, "w", encoding="utf-8") as f:
            json.dump(base, f, indent=2)
            f.write("\n")
        return "json-deep"

    with open(target, encoding="utf-8") as f:
        existing = f.read()

    if mode == "gradle-dependencies":
        meaningful = [ln.strip() for ln in text.split("\n")
                      if ln.strip() and not ln.strip().startswith("//")]
        if not meaningful:
            die("fragment %r declares merge=gradle-dependencies but has no dependency lines" % aid)
        lines = existing.split("\n")
        for i, ln in enumerate(lines):
            if ln.strip().startswith("dependencies") and ln.rstrip().endswith("{"):
                lines[i + 1:i + 1] = ["\t" + m for m in meaningful]
                with open(target, "w", encoding="utf-8") as f:
                    f.write("\n".join(lines))
                return "gradle-dependencies"
        die("fragment %r declares merge=gradle-dependencies but %s has no `dependencies {` block"
            % (aid, target))

    # mode == "append"
    with open(target, "w", encoding="utf-8") as f:
        f.write(existing.rstrip("\n") + "\n\n" + text.rstrip("\n") + "\n")
    return "append"


installed, skipped, commands = [], [], []
files_phase = [a for a in artifacts if a.kind in ("file", "file-fragment")]
cmd_phase = [a for a in artifacts if a.kind == "command"]
for a in files_phase + cmd_phase:   # commands last: hook-install-wiring needs hook-body on disk
    if a.kind not in ("file", "file-fragment", "command"):
        die("artifact %r has unregistered kind=%r" % (a.id, a.kind))
    if a.base not in roots:
        die("artifact %r has base=%r which this harness cannot resolve" % (a.id, a.base))
    if a.when:
        ns, segments = ax_markers._split_token(a.when)
        if not ax_markers._lookup_condition(ns, segments, config, env):
            skipped.append(a.id)
            continue
    try:
        rendered = ax_markers.render(a, config, env)
    except ax_markers.RenderError as exc:
        die("render failed for id=%r: %s" % (a.id, exc))
    root = roots[a.base]

    if a.kind == "command":
        proc = subprocess.run(["bash", "-c", rendered], cwd=root,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        out = proc.stdout.decode(errors="replace")
        if proc.returncode != 0:
            die("command artifact %r failed (rc=%d) in %s" % (a.id, proc.returncode, root),
                *out.splitlines()[-20:])
        commands.append(a.id)
        installed.append(a.id)
        print("  installed %-28s command in %s" % (a.id, os.path.relpath(root, proj) or "."))
        continue

    try:
        rel = ax_markers.render_path(a, config, env)
    except ax_markers.RenderError as exc:
        die("path render failed for id=%r: %s" % (a.id, exc))
    target = os.path.join(root, rel)
    # The marker's own declaration decides placement. `kind=file` may omit merge=; the contract
    # (ax_markers module header) says an omitted merge on a whole file MEANS replace, and lint()
    # already refuses a `kind=file-fragment` that declares none — so the only way to reach
    # apply_merge with mode=None would be a contract change nobody taught this harness about.
    mode = a.merge
    if mode is None:
        if a.kind != "file":
            die("artifact %r is kind=%r and declares no merge=; this harness will not guess one"
                % (a.id, a.kind))
        mode = "replace"
    how = apply_merge(target, rendered, a.id, mode)
    installed.append(a.id)
    print("  installed %-28s %-22s %s" % (a.id, how, os.path.relpath(target, proj)))

with open(out_path, "w", encoding="utf-8") as f:
    json.dump({"digests": digests, "installed": installed, "skipped_when": skipped,
               "commands": commands, "discovered": len(digests),
               "skill_files": len(skill_paths)}, f, indent=2)
print("  discovered %d marker(s) across %d skill file(s); installed %d, when-skipped %d"
      % (len(digests), len(skill_paths), len(installed), len(skipped)))
PYEOF

echo "── install artifacts (extracted from skills/*/SKILL.md) ───"
OVERRIDE_FILE="-"
if [ -n "$(printf '%s' "$OVERRIDE_ARGS" | tr -d '[:space:]')" ]; then
    OVERRIDE_FILE="$WORK/overrides.json"
    printf '%s\n' "$OVERRIDE_ARGS" | grep -v '^[[:space:]]*$' > "$WORK/overrides.txt"
    python3 -c 'import json,sys; print(json.dumps([l.rstrip("\n") for l in open(sys.argv[1]) if l.strip()]))' \
        "$WORK/overrides.txt" > "$OVERRIDE_FILE"
    OVERRIDE_IDS="$(sed 's/=.*//' "$WORK/overrides.txt" | paste -sd, -)"
    echo "  ⚠ ARTIFACT OVERRIDE ACTIVE: $OVERRIDE_IDS"
    echo "    This run is a regression differential, NOT release evidence."
fi

python3 "$WORK/install.py" "$REPO_ROOT" "$PROJ" "$PLUGIN_PATH" "$OVERRIDE_FILE" "$WORK/install.json"
INSTALL_RC=$?
if [ "$INSTALL_RC" != "0" ]; then
    echo "verify-downstream: artifact installation failed (rc=$INSTALL_RC)" >&2
    finish 6 "install-failed"
fi
# Commit the installed artifacts with --no-verify: this commit is harness SETUP (it makes the
# working tree clean so each assertion below stages exactly what it intends), not a measurement.
# Every assertion commit that follows runs the hook for real.
pgit add -A >/dev/null 2>&1
pgit commit -q --no-verify -m "install ax artifacts" >/dev/null 2>&1

# A10-tsdep, SECOND HALF + verdict. Both halves are required: "resolvable now" alone is satisfied
# by a fixture that shipped the package all along (the pre-P2-113 state), and "unresolvable before"
# alone says nothing about the artifact. Only the TRANSITION attributes the package's presence to
# react-ts-eslint-dep having actually run.
TSDEP_POST=1
ts_resolvable && TSDEP_POST=0
if [ "$TSDEP_PRE" != "0" ] && [ "$TSDEP_POST" = "0" ]; then
    echo "  A10-tsdep: typescript-eslint unresolvable before install, resolvable after"
    note "A10-tsdep" true
else
    echo "    pre_resolvable=$([ "$TSDEP_PRE" = 0 ] && echo yes || echo no) post_resolvable=$([ "$TSDEP_POST" = 0 ] && echo yes || echo no)"
    if [ "$TSDEP_PRE" = "0" ]; then
        echo "    The fixture already carried typescript-eslint — react-ts-eslint-dep is VACUOUS"
        echo "    in this run (P2-113 regression: check frontend/package{,-lock}.json)."
    else
        echo "    react-ts-eslint-dep did not put typescript-eslint on disk."
    fi
    note "A10-tsdep" false
fi
echo ""

# ── 4. ASSERTIONS ────────────────────────────────────────────────────────────────────────────
cat > "$WORK/check.py" <<'PYEOF'
"""Assertion helpers whose verdicts cannot be read off a stylish-format string.

eslint-json: `--format json` is mandatory here — the default stylish format prints NOTHING for a
clean run, so "no output" is indistinguishable from "eslint never ran". results > 0 is the
non-vacuity half (a glob that matched zero files must not read as clean); errorCount == 0 is the
verdict half.
junit-tests: a `testPractices` task that ran zero tests reports BUILD SUCCESSFUL — a green with no
tests is exactly the vacuous gate this harness exists to detect.
"""
import glob
import json
import os
import sys
import xml.etree.ElementTree as ET

mode = sys.argv[1]

if mode == "eslint-json":
    with open(sys.argv[2], encoding="utf-8") as f:
        try:
            results = json.load(f)
        except ValueError as exc:
            print("eslint --format json output is not parseable: %s" % exc)
            sys.exit(1)
    errors = sum(r.get("errorCount", 0) for r in results)
    print("results=%d errorCount=%d" % (len(results), errors))
    sys.exit(0 if (len(results) > 0 and errors == 0) else 1)

if mode == "junit-tests":
    xmls = sorted(glob.glob(os.path.join(sys.argv[2], "*.xml")))
    total = 0
    classes = []
    for path in xmls:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        total += int(root.get("tests", "0"))
        name = root.get("name", "")
        if name:
            classes.append(name)
    print("xml_files=%d tests=%d classes=%s" % (len(xmls), total, ",".join(classes)))
    forbidden = sys.argv[3] if len(sys.argv) > 3 else ""
    if forbidden and any(forbidden in c for c in classes):
        sys.exit(1)
    sys.exit(0 if total > 0 else 1)

print("check.py: unknown mode %r" % mode, file=sys.stderr)
sys.exit(2)
PYEOF

cat > "$WORK/eval_diff.py" <<'PYEOF'
"""A9-eval — the two-config EVALUATOR DIFFERENTIAL (P2-114).

Everything else in this harness renders each artifact against ONE config (the fixture's), so a
conditional that never actually varies is indistinguishable from one that does: an `ax:if` whose
reference silently resolves to a constant, an `ax:subst` that happens to interpolate the value the
body already had, or a directive that was dropped entirely all produce a rendered file that looks
exactly right in the single shape this harness installs. The only way to see a CONDITION as a
condition is to render the same marker twice and require the outputs to DIFFER in the specific way
the directive claims.

Two pairs, both pure render — no network, no npm, no gradle:
  hook-body           fixture config (stacks=[react,java], java.testTask=testPractices)
                   vs contrast config (stacks=[java],       java.testTask=verifyAxPractices)
                      -> the `ax:if config.stacks.react` region present in the first, gone in the
                         second; the `ax:subst config.java.testTask` value different in each; the
                         gradle invocation present in BOTH (the java region is not what varies).
  react-eslint-config react.typescript true vs false
                      -> the typescript-eslint parser wiring present in the first, gone in the
                         second.
Both renders of both artifacts are additionally required to carry ZERO residual `ax:` directive
lines and ZERO unsubstituted `@@..@@` tokens — a directive that survives verbatim into a consumer
file is the failure mode a single-config render is least likely to expose.
"""
import glob
import json
import os
import re
import sys

repo_root, config_path, contrast_path, overrides_path = sys.argv[1:5]
sys.path.insert(0, os.path.join(repo_root, "practices", "scripts", "lib"))
import ax_markers  # noqa: E402

RESIDUAL_DIRECTIVE = re.compile(r'(?:#|//)\s*ax:(?:if|endif|subst)\b')
RESIDUAL_TOKEN = re.compile(r'@@[^@\s]+@@')

overrides = {}
if overrides_path != "-":
    with open(overrides_path, encoding="utf-8") as f:
        for spec in json.load(f):
            aid, sep, path = spec.partition("=")
            if sep and os.path.isfile(path):
                with open(path, encoding="utf-8") as fh:
                    overrides[aid] = fh.read()

with open(config_path, encoding="utf-8") as f:
    cfg_a = json.load(f)
with open(contrast_path, encoding="utf-8") as f:
    cfg_b = json.load(f)

env = {"axPluginPath": "/nonexistent-not-used-by-these-two-artifacts"}
by_id = {}
for a in ax_markers.discover(sorted(glob.glob(os.path.join(repo_root, "skills", "*", "SKILL.md")))):
    if a.id in overrides:
        a.body = overrides[a.id]
    by_id[a.id] = a

failures = []


def check(label, ok, detail=""):
    print("    %-4s %s%s" % ("ok" if ok else "FAIL", label, (" — " + detail) if detail else ""))
    if not ok:
        failures.append(label)


def rendered(aid, cfg, which):
    art = by_id.get(aid)
    if art is None:
        failures.append("artifact %r not found" % aid)
        print("    FAIL artifact %r not found among %d markers" % (aid, len(by_id)))
        return None
    try:
        text = ax_markers.render(art, cfg, env)
    except ax_markers.RenderError as exc:
        failures.append("render(%s,%s)" % (aid, which))
        print("    FAIL render %s [%s]: %s" % (aid, which, exc))
        return None
    check("%s[%s] no residual ax: directive" % (aid, which),
          not RESIDUAL_DIRECTIVE.search(text))
    check("%s[%s] no unsubstituted @@token@@" % (aid, which),
          not RESIDUAL_TOKEN.search(text))
    return text


# ── hook-body: stacks[] membership + the testTask substitution ────────────────────────────────
hook_a = rendered("hook-body", cfg_a, "fixture")
hook_b = rendered("hook-body", cfg_b, "contrast")
if hook_a is not None and hook_b is not None:
    check("hook-body: react region PRESENT when stacks contains react",
          "REACT_TOUCHED" in hook_a)
    check("hook-body: react region ABSENT when stacks omits react",
          "REACT_TOUCHED" not in hook_b)
    task_a = cfg_a.get("java", {}).get("testTask")
    task_b = cfg_b.get("java", {}).get("testTask")
    check("contrast config actually names a DIFFERENT testTask",
          bool(task_a) and bool(task_b) and task_a != task_b,
          "%r vs %r" % (task_a, task_b))
    # The LAST literal assignment is the one that wins at hook runtime — the body opens with an
    # unconditional `JAVA_TEST_TASK="testPractices"` documented default (F-032) that the
    # `ax:if config.java.testTask` region then overwrites, so "testPractices does not appear" is
    # NOT the claim; "the effective value differs between the two configs" is.
    def effective_task(text):
        found = re.findall(r'^JAVA_TEST_TASK="([^"]*)"', text, re.M)
        return found[-1] if found else None
    eff_a, eff_b = effective_task(hook_a), effective_task(hook_b)
    check("hook-body: effective task is %r for the fixture config" % task_a, eff_a == task_a,
          "last JAVA_TEST_TASK= assignment renders as %r" % eff_a)
    check("hook-body: effective task is %r for the contrast config" % task_b, eff_b == task_b,
          "last JAVA_TEST_TASK= assignment renders as %r" % eff_b)
    check("hook-body: the two configs really do render DIFFERENT task names",
          eff_a is not None and eff_b is not None and eff_a != eff_b)
    check("hook-body: the gradle invocation survives in BOTH (java region is not what varies)",
          'gradlew "$JAVA_TEST_TASK"' in hook_a and 'gradlew "$JAVA_TEST_TASK"' in hook_b)

# ── react-eslint-config: react.typescript on/off ──────────────────────────────────────────────
cfg_ts_on = json.loads(json.dumps(cfg_a))
cfg_ts_on.setdefault("react", {})["typescript"] = True
cfg_ts_off = json.loads(json.dumps(cfg_a))
cfg_ts_off.setdefault("react", {})["typescript"] = False
esl_on = rendered("react-eslint-config", cfg_ts_on, "ts=on")
esl_off = rendered("react-eslint-config", cfg_ts_off, "ts=off")
if esl_on is not None and esl_off is not None:
    check("react-eslint-config: tseslint parser wired when react.typescript is true",
          "tseslint" in esl_on)
    check("react-eslint-config: tseslint reference GONE when react.typescript is false",
          "tseslint" not in esl_off)

print("    evaluator differential: %d check(s) failed" % len(failures))
sys.exit(1 if failures else 0)
PYEOF

# The CONTRAST config exists only in this temp dir — it is never installed and never materialized.
# Its whole job is to be a second point of evaluation for the same markers.
cat > "$WORK/contrast.config.json" <<'EOF'
{
  "version": 1,
  "stacks": ["java"],
  "react": {
    "root": "frontend",
    "srcDir": "src",
    "typescript": false,
    "alias": { "@/": "src/" },
    "layers": { "app": ["app"], "features": ["features"], "shared": ["components", "lib"] }
  },
  "java": {
    "root": "backend",
    "buildTool": "gradle",
    "rootPackage": "com.example.backend",
    "testTask": "verifyAxPractices"
  }
}
EOF

echo "── assertions ────────────────────────────────────────────"

# A9-eval — see eval_diff.py's docstring. Pure render: no network, no npm, no gradle, so it runs
# first and costs nothing.
echo "  A9-eval: rendering hook-body / react-eslint-config against TWO configs …"
python3 "$WORK/eval_diff.py" "$REPO_ROOT" "$PROJ/ax.config.json" "$WORK/contrast.config.json" \
    "$OVERRIDE_FILE" > "$WORK/a9.log" 2>&1
A9_RC=$?
sed 's/^/  /' "$WORK/a9.log"
if [ "$A9_RC" = "0" ]; then
    note "A9-eval" true
else
    note "A9-eval" false
fi

# A7b — A7's TRIGGER PREMISE, checked against the COMMITTED fixture source (not the materialized
# copy, which this script has since appended to): the eager `tasks.withType<Test>` block is what
# realizes every Test task at configuration time. Without it, A7 would pass for free.
if grep -qF 'tasks.withType<Test> {' "$FIXTURE_SRC/backend/build.gradle.kts"; then
    note "A7b" true
else
    echo "    fixture backend/build.gradle.kts has no eager 'tasks.withType<Test> {' block —"
    echo "    A7 below would be VACUOUS (nothing forces the new Test task to be realized)."
    note "A7b" false
fi

# A0 — compile the installed ArchUnit class, and prove the CLASS FILE is really there.
echo "  A0: ./gradlew compileTestJava -PaxRootPackage=com.example.backend …"
( cd "$BACKEND" && ./gradlew compileTestJava -PaxRootPackage=com.example.backend ) \
    > "$WORK/a0.log" 2>&1
A0_RC=$?
A0_CLASS="$(find "$BACKEND/build/classes/java/test" -name 'LayerBoundaryArchTest*.class' 2>/dev/null | head -1)"
if [ "$A0_RC" = "0" ] && [ -n "$A0_CLASS" ]; then
    echo "    compiled: ${A0_CLASS#$PROJ/}"
    note "A0" true
else
    echo "    rc=$A0_RC class_file='${A0_CLASS:-none}'"
    tail -25 "$WORK/a0.log" | sed 's/^/      /'
    note "A0" false
fi

# A7 — GH #90: `./gradlew tasks --all` with NO -P must still work, and must LIST the gate.
echo "  A7: ./gradlew tasks --all (no -P) …"
( cd "$BACKEND" && ./gradlew tasks --all ) > "$WORK/a7.log" 2>&1
A7_RC=$?
if [ "$A7_RC" = "0" ] && grep -qF 'testPractices' "$WORK/a7.log"; then
    note "A7" true
else
    echo "    rc=$A7_RC  testPractices listed: $(grep -c 'testPractices' "$WORK/a7.log")"
    tail -25 "$WORK/a7.log" | sed 's/^/      /'
    note "A7" false
fi

# A-pc / A1 — POSITIVE CONTROL + the react gate's own signal. One planted violation, one commit
# attempt, two independent claims: (A-pc) the hook RAN and the commit was REFUSED; (A1) it was
# refused by ax/no-upward-layer-import specifically. Without A-pc every later "BLOCKED" verdict
# in this run would rest on an unproven assumption that a blocked commit means the gate fired.
PROBE_TS="$FRONTEND/src/lib/__ax_probe.ts"
cat > "$PROBE_TS" <<'EOF'
// Planted by verify-downstream.sh: shared (lib) importing UP into the app layer.
// The `: string` annotation is load-bearing — a bare ESM import is valid plain JS, so a probe
// without a TypeScript-only construct would still parse even if the TS parser wiring were absent.
import { probe } from '@/app/__ax_probe_target'

export const __axProbe: string = probe
EOF
pgit add frontend/src/lib/__ax_probe.ts >/dev/null 2>&1
echo "  A-pc/A1: committing a shared -> app upward import (must be BLOCKED) …"
pgit commit -m "probe: upward layer import" > "$WORK/a1.log" 2>&1
A1_RC=$?
if [ "$A1_RC" != "0" ] && grep -qF 'ax-hook: pre-commit gate' "$WORK/a1.log"; then
    note "A-pc" true
else
    echo "    rc=$A1_RC (expected non-zero) / hook banner present: $(grep -c 'ax-hook: pre-commit gate' "$WORK/a1.log")"
    echo "    POSITIVE CONTROL FAILED — no 'BLOCKED' verdict in this run is trustworthy."
    tail -25 "$WORK/a1.log" | sed 's/^/      /'
    note "A-pc" false
fi
if [ "$A1_RC" != "0" ] && grep -qF 'ax/no-upward-layer-import' "$WORK/a1.log"; then
    note "A1" true
else
    echo "    rc=$A1_RC  rule named in output: $(grep -c 'ax/no-upward-layer-import' "$WORK/a1.log")"
    tail -25 "$WORK/a1.log" | sed 's/^/      /'
    note "A1" false
fi

# A2 — same path, violation removed. Commit must SUCCEED, and the linter must be shown to have
# actually looked at files (results > 0) while reporting no errors.
cat > "$PROBE_TS" <<'EOF'
// Same file as the A1 probe, violation removed: no upward import, TypeScript construct kept.
export const __axProbe: string = 'ok'
EOF
pgit add frontend/src/lib/__ax_probe.ts >/dev/null 2>&1
echo "  A2: committing the same path with the violation removed (must PASS) …"
pgit commit -m "probe: upward layer import removed" > "$WORK/a2.log" 2>&1
A2_RC=$?
( cd "$FRONTEND" && npx eslint src --format json ) > "$WORK/a2-eslint.json" 2>"$WORK/a2-eslint.err"
A2_ESLINT_SUMMARY="$(python3 "$WORK/check.py" eslint-json "$WORK/a2-eslint.json")"
A2_ESLINT_RC=$?
if [ "$A2_RC" = "0" ] && [ "$A2_ESLINT_RC" = "0" ]; then
    echo "    commit rc=0, eslint json: $A2_ESLINT_SUMMARY"
    note "A2" true
else
    echo "    commit rc=$A2_RC  eslint json: $A2_ESLINT_SUMMARY (check rc=$A2_ESLINT_RC)"
    tail -25 "$WORK/a2.log" | sed 's/^/      /'
    note "A2" false
fi

# A11-route / A12-route — THE NEXT/APP-ROUTER RULE AXIS (P2-105).
#
# A1/A2 above prove the react gate fires, but they prove it with ONE rule —
# ax/no-upward-layer-import — whose entire input is an import path string. Three rules in the
# recommended set are gated on something A1's probe cannot reach: `isRouteFile()` (the file sits at
# `<srcDir>/<layers.app[0]>/**/(page|layout).tsx`) AND `hasUseClientDirective()`. A layout mapping
# that resolved to the wrong directory name, a `settings: { ax: axConfig.react }` block that never
# reached these rules, or a route-detection regex broken by a non-"." react.root would leave every
# app-router rule silently inert while A1/A2 stayed green — the rules would be installed and never
# fire, which is exactly the vacuous-gate shape this harness exists to detect.
#
# ONE planted route file trips all three at once, and each rule id is checked SEPARATELY so a
# partial failure names itself instead of hiding behind the other two:
#   ax/no-route-client-data-fetching  (error) — useSWR(...) + raw fetch(...) in a "use client" route
#   ax/no-server-state-in-local-state (error) — useState(useSWR(...).data)
#   ax/no-god-route                   (error) — the file is deliberately > 100 lines
# The `: string` annotation is load-bearing for the same reason as A1's (TS parser wiring), and the
# padding comments are what carry the file past no-god-route's DEFAULT_MAX_LINES=100 threshold —
# comments cannot trip any other rule, so the file's only violations are the three intended ones.
ROUTE_PROBE_DIR="$FRONTEND/src/app/__ax_route_probe"
ROUTE_PROBE="$ROUTE_PROBE_DIR/page.tsx"
mkdir -p "$ROUTE_PROBE_DIR"
{
cat <<'EOF'
"use client"

// Planted by verify-downstream.sh (P2-105): an app-router CLIENT route file that violates all
// three Next/route-axis rules at once. Every line below the code is padding, deliberately, so the
// file crosses ax/no-god-route's 100-line threshold without introducing any other violation.

export default function AxRouteProbePage() {
  // ax/no-route-client-data-fetching: a client data hook called straight from the route,
  // and ax/no-server-state-in-local-state: its .data mirrored into local state.
  const [snapshot] = useState(useSWR('/api/__ax_probe').data)
  // ax/no-route-client-data-fetching again: a raw fetch in a "use client" route.
  fetch('/api/__ax_probe_side_channel')
  const label: string = 'ax route probe'
  return <main>{label}{String(snapshot)}</main>
}
EOF
i=0
while [ "$i" -lt 92 ]; do
    echo "// padding line $i — carries this route file past ax/no-god-route's 100-line threshold."
    i=$((i + 1))
done
} > "$ROUTE_PROBE"
ROUTE_PROBE_LINES="$(wc -l < "$ROUTE_PROBE" | tr -d ' ')"
pgit add frontend/src/app/__ax_route_probe/page.tsx >/dev/null 2>&1
echo "  A11-route: committing a $ROUTE_PROBE_LINES-line \"use client\" app-router route (must be BLOCKED) …"
pgit commit -m "probe: next app-router route violations" > "$WORK/a11.log" 2>&1
A11_RC=$?
A11_MISSING=""
for rid in ax/no-route-client-data-fetching ax/no-server-state-in-local-state ax/no-god-route; do
    if grep -qF "$rid" "$WORK/a11.log"; then
        echo "    named in output: $rid"
    else
        echo "    MISSING from output: $rid"
        A11_MISSING="$A11_MISSING $rid"
    fi
done
if [ "$A11_RC" != "0" ] && [ -z "$A11_MISSING" ]; then
    note "A11-route" true
else
    echo "    rc=$A11_RC (expected non-zero)  probe lines=$ROUTE_PROBE_LINES  missing rule id(s):${A11_MISSING:- none}"
    tail -30 "$WORK/a11.log" | sed 's/^/      /'
    note "A11-route" false
fi

# A12-route — the same route path with all three violations removed must COMMIT. Without this
# half, A11 alone is satisfied by a gate that rejects EVERY route file (a broken glob, a crashing
# rule) — "it blocked" and "it blocked for the reason we planted" only separate once the repaired
# file goes through. The repair stays THIN and under the line threshold, which is the shape the
# rules are asking for.
cat > "$ROUTE_PROBE" <<'EOF'
"use client"

// Same route file as the A12 probe, all three violations removed: no client data hook, no raw
// fetch, no server state mirrored into useState, and thin enough to stay under the line threshold.
export default function AxRouteProbePage() {
  const label: string = 'ax route probe'
  return <main>{label}</main>
}
EOF
pgit add frontend/src/app/__ax_route_probe/page.tsx >/dev/null 2>&1
echo "  A12-route: committing the same route with the violations removed (must PASS) …"
pgit commit -m "probe: next app-router route violations removed" > "$WORK/a12.log" 2>&1
A12_RC=$?
if [ "$A12_RC" = "0" ] && grep -qF 'ax-hook: pre-commit gate' "$WORK/a12.log"; then
    echo "    commit rc=0, hook banner present"
    note "A12-route" true
else
    echo "    commit rc=$A12_RC  hook banner present: $(grep -c 'ax-hook: pre-commit gate' "$WORK/a12.log")"
    tail -30 "$WORK/a12.log" | sed 's/^/      /'
    note "A12-route" false
fi

# The violation A21 and the hook-wiring branches are both asked to refuse: A1's probe, re-planted.
# Reusing it (rather than inventing another) is deliberate — branch A already refused this exact
# file with the plugin resolvable, so a run that now lets it through differs in ONE thing: the
# wiring, or where the plugin was installed.
plant_upward_probe() {
    cat > "$PROBE_TS" <<'EOF'
// Re-planted by verify-downstream.sh: shared (lib) importing UP into the app layer.
import { probe } from '@/app/__ax_probe_target'

export const __axProbe: string = probe
EOF
    pgit add frontend/src/lib/__ax_probe.ts >/dev/null 2>&1
}

# A21-plugindep — P2-121. WHERE the react plugin got installed, which every assertion above is
# structurally blind to.
#
# `npm i -D` writes the dependency into the package.json OF THE DIRECTORY IT RUNS IN. With
# react-plugin-dep declaring base=repo, that was the repo root — a package.json a consumer's CI
# never installs, since a workspace CI runs `npm ci` inside <react.root>. Every assertion above
# still passed, because Node resolves `@ax/eslint-plugin-ax` by walking UP into the repo-root
# node_modules that the SAME machine's install had just created. That is an accident of layout, not
# a declared dependency, and it evaporates the moment the workspace is installed on its own.
#
# Three counts, and the third is the one that reproduces a stock CI:
#   (i)   <react.root>/package.json declares the plugin, and <react.root>/package-lock.json LOCKS
#         it — a `npm i --no-save`-shaped install satisfies neither.
#   (ii)  nothing OUTSIDE <react.root> declares it. Measured HERE, before the hook branches create
#         their own repo-root package.json for husky/lefthook; after that point the question is no
#         longer answerable.
#   (iii) node_modules wiped at BOTH levels, `npm ci` run ONLY inside <react.root>, and A1's probe
#         still BLOCKED with banner + rule id.
# Pre-fix this went RED on (i) (zero lockfile references) and on (iii) (`Cannot find module
# '@ax/eslint-plugin-ax'` — the gate cannot even load, so no rule id is ever named).
#
# NO CASCADE — MEASURED, not assumed. Step (iii) deletes the repo-root node_modules on purpose,
# because that directory IS the accidental resolution path being tested, and the obvious worry is
# that a pre-fix tree would then lose the plugin for A13-husky/A14-lefthook too, smearing one
# finding across three assertions. It does not: in the pre-fix differential those two stayed GREEN,
# because each begins with `npm i -D husky` / `npm i -D lefthook` AT THE REPO ROOT, which
# re-installs that package.json's whole tree — including, pre-fix, the ax plugin. So A21 fails
# alone and names its own cause.
AX_PLUGIN_PKG='@ax/eslint-plugin-ax'
declares_ax_plugin() {   # 0 = the package.json at $1 declares the plugin in dev or prod deps
    python3 -c 'import json,sys
try:
    d = json.load(open(sys.argv[1]))
except (OSError, ValueError):
    sys.exit(2)
deps = dict(d.get("dependencies", {}))
deps.update(d.get("devDependencies", {}))
sys.exit(0 if sys.argv[2] in deps else 1)' "$1" "$AX_PLUGIN_PKG"
}
echo "  A21-plugindep: where did react-plugin-dep put $AX_PLUGIN_PKG? …"
A21_IN_REACT_PKG=no
declares_ax_plugin "$FRONTEND/package.json" && A21_IN_REACT_PKG=yes
A21_IN_REACT_LOCK="$(grep -c "$AX_PLUGIN_PKG" "$FRONTEND/package-lock.json" 2>/dev/null | tr -d ' ')"
[ -n "$A21_IN_REACT_LOCK" ] || A21_IN_REACT_LOCK=0
A21_AT_ROOT=no
if [ -f "$PROJ/package.json" ]; then
    declares_ax_plugin "$PROJ/package.json" && A21_AT_ROOT=yes
fi
echo "    <react.root>/package.json declares it: $A21_IN_REACT_PKG"
echo "    <react.root>/package-lock.json references: $A21_IN_REACT_LOCK"
echo "    repo-root package.json declares it: $A21_AT_ROOT (repo-root package.json exists: $([ -f "$PROJ/package.json" ] && echo yes || echo no))"
echo "    wiping node_modules at BOTH levels, then npm ci INSIDE <react.root> only …"
rm -rf "$FRONTEND/node_modules" "$PROJ/node_modules"
( cd "$FRONTEND" && npm ci ) > "$WORK/a21-npm-ci.log" 2>&1
A21_CI_RC=$?
A21_RESOLVES=no
( cd "$FRONTEND" && node -e 'require.resolve("@ax/eslint-plugin-ax",{paths:[process.cwd()]})' ) \
    >/dev/null 2>&1 && A21_RESOLVES=yes
plant_upward_probe
pgit commit -m "probe: upward layer import (after react.root-only npm ci)" > "$WORK/a21.log" 2>&1
A21_RC=$?
A21_BANNER="$(grep -ac 'ax-hook: pre-commit gate' "$WORK/a21.log")"
A21_RULE="$(grep -ac 'ax/no-upward-layer-import' "$WORK/a21.log")"
echo "    npm ci rc=$A21_CI_RC  plugin resolvable from <react.root>: $A21_RESOLVES"
if [ "$A21_IN_REACT_PKG" = "yes" ] && [ "$A21_IN_REACT_LOCK" != "0" ] && [ "$A21_AT_ROOT" = "no" ] \
   && [ "$A21_CI_RC" = "0" ] && [ "$A21_RESOLVES" = "yes" ] && [ "$A21_RC" != "0" ] \
   && [ "$A21_BANNER" != "0" ] && [ "$A21_RULE" != "0" ]; then
    echo "    declared+locked under <react.root>, nothing at the repo root, and the gate still"
    echo "    BLOCKED the probe after a workspace-only npm ci (banner + rule id present)"
    note "A21-plugindep" true
else
    echo "    commit rc=$A21_RC (expected non-zero) banner=$A21_BANNER rule=$A21_RULE"
    [ "$A21_RESOLVES" = "no" ] && echo "    $AX_PLUGIN_PKG is NOT resolvable from <react.root> after its own npm ci —"
    [ "$A21_RESOLVES" = "no" ] && echo "    the install landed outside the workspace (P2-121 shape)."
    tail -20 "$WORK/a21-npm-ci.log" 2>/dev/null | sed 's/^/      /'
    tail -25 "$WORK/a21.log" | sed 's/^/      /'
    note "A21-plugindep" false
fi
# Return to HEAD: drain the index first (the rejected commit left the probe staged), then restore
# the file from it. The reverse order would restore the STAGED violation back over itself.
pgit reset -q >/dev/null 2>&1
pgit checkout -- frontend/src/lib/__ax_probe.ts >/dev/null 2>&1

# A3 — the java gate's own signal: a Service depending on a Controller, blocked BY NAME.
mkdir -p "$BACKEND/src/main/java/com/example/backend/probe"
cat > "$BACKEND/src/main/java/com/example/backend/probe/ProbeController.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

	public String describe() {
		return "probe";
	}

}
EOF
cat > "$BACKEND/src/main/java/com/example/backend/probe/ProbeService.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.stereotype.Service;

// Planted by verify-downstream.sh: a *Service depending on a *Controller -- the exact shape
// LayerBoundaryArchTest#servicesDoNotDependOnControllers must reject.
@Service
public class ProbeService {

	private final ProbeController probeController;

	public ProbeService(ProbeController probeController) {
		this.probeController = probeController;
	}

	public String describe() {
		return probeController.describe();
	}

}
EOF
pgit add backend/src/main/java/com/example/backend/probe >/dev/null 2>&1
echo "  A3: committing ProbeService -> ProbeController (must be BLOCKED) …"
pgit commit -m "probe: service depends on controller" > "$WORK/a3.log" 2>&1
A3_RC=$?
if [ "$A3_RC" != "0" ] && grep -qF 'ProbeService' "$WORK/a3.log"; then
    note "A3" true
else
    echo "    rc=$A3_RC  probe class named in output: $(grep -c 'ProbeService' "$WORK/a3.log")"
    tail -30 "$WORK/a3.log" | sed 's/^/      /'
    note "A3" false
fi

# A4 — violation removed. Commit must SUCCEED **and** the gate must have run a non-zero number of
# tests: `tasks.register<Test>(...)` starts with an EMPTY testClassesDirs/classpath, so a gate that
# lost those two lines still reports BUILD SUCCESSFUL while scanning nothing.
#
# The violation is REPAIRED IN PLACE, not deleted. Deleting the probe package was measured to make
# this assertion untestable: A3's commit was rejected, so its files were still STAGED, and removing
# them again left the index byte-identical to HEAD — git answered "nothing to commit, working tree
# clean" (rc=1) and the java gate never ran at all. A repair leaves a real, staged, backend-scoped
# change, which is the only shape that exercises the thing A4 claims to measure.
cat > "$BACKEND/src/main/java/com/example/backend/probe/ProbeService.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.stereotype.Service;

// Same file as the A3 probe, violation repaired: no dependency on any *Controller.
@Service
public class ProbeService {

	public String describe() {
		return "probe";
	}

}
EOF
rm -rf "$BACKEND/build/test-results/testPractices"
pgit add -A backend/src >/dev/null 2>&1
echo "  A4: committing the repair (must PASS, and testPractices must run > 0 tests) …"
pgit commit -m "probe: service->controller dependency removed" > "$WORK/a4.log" 2>&1
A4_RC=$?
A4_XML_SUMMARY="$(python3 "$WORK/check.py" junit-tests "$BACKEND/build/test-results/testPractices")"
A4_XML_RC=$?
if [ "$A4_RC" = "0" ] && [ "$A4_XML_RC" = "0" ]; then
    echo "    commit rc=0, testPractices results: $A4_XML_SUMMARY"
    note "A4" true
else
    echo "    commit rc=$A4_RC  testPractices results: $A4_XML_SUMMARY (check rc=$A4_XML_RC)"
    tail -30 "$WORK/a4.log" | sed 's/^/      /'
    note "A4" false
fi

# A5/A6 — PLUGIN-CHANNEL rule 6: a file outside BOTH roots commits, the hook still RAN (banner),
# and neither stack gate executed. A6 is A5's premise: without it, "no npm/gradle marker" would
# also be true of a commit that staged nothing and of a hook that never ran at all.
#
# The index is RESET first. Assertions here run in sequence against one index, and a commit that
# the hook REJECTED leaves its files staged — measured in the --artifact-override differentials,
# where A5 inherited A1's still-staged frontend probe and the react gate therefore fired on a
# "README-only" commit. On the all-green path this reset is a no-op (every prior commit drained
# the index); in a differential it keeps A5 measuring its own claim instead of a cascade.
pgit reset -q >/dev/null 2>&1
printf '\n<!-- touched by verify-downstream A5 -->\n' >> "$PROJ/README.md"
pgit add README.md >/dev/null 2>&1
A6_STAGED="$(pgit diff --cached --name-only | wc -l | tr -d ' ')"
echo "  A5/A6: committing README.md (outside both roots) …"
pgit commit -m "touch a file outside both roots" > "$WORK/a5.log" 2>&1
A5_RC=$?
A5_BANNER="$(grep -c 'ax-hook: pre-commit gate' "$WORK/a5.log")"
A5_NPM="$(grep -c -- '--max-warnings' "$WORK/a5.log")"
A5_GRADLE="$(grep -c '> Task :' "$WORK/a5.log")"
if [ "$A5_RC" = "0" ] && [ "$A5_BANNER" != "0" ] && [ "$A5_NPM" = "0" ] && [ "$A5_GRADLE" = "0" ]; then
    note "A5" true
else
    echo "    rc=$A5_RC banner=$A5_BANNER npm_marker=$A5_NPM gradle_marker=$A5_GRADLE"
    tail -25 "$WORK/a5.log" | sed 's/^/      /'
    note "A5" false
fi
if [ "$A6_STAGED" -ge 1 ] 2>/dev/null && [ "$A5_BANNER" != "0" ]; then
    echo "    premise: staged=$A6_STAGED file(s), hook banner observed"
    note "A6" true
else
    echo "    premise FAILED: staged=$A6_STAGED banner=$A5_BANNER — A5 would be vacuous"
    note "A6" false
fi

# A8 — GH #91 gate isolation: plain `test` must stay green, must actually run the project's own
# tests, and must NOT absorb the @Tag("PRACTICES") ArchUnit class (whose ax.mainClassesDirs is set
# only by testPractices' doFirst).
echo "  A8: ./gradlew test -PaxRootPackage=com.example.backend …"
rm -rf "$BACKEND/build/test-results/test"
( cd "$BACKEND" && ./gradlew test -PaxRootPackage=com.example.backend ) > "$WORK/a8.log" 2>&1
A8_RC=$?
A8_XML_SUMMARY="$(python3 "$WORK/check.py" junit-tests "$BACKEND/build/test-results/test" LayerBoundaryArchTest)"
A8_XML_RC=$?
if [ "$A8_RC" = "0" ] && [ "$A8_XML_RC" = "0" ]; then
    echo "    rc=0, test results: $A8_XML_SUMMARY (LayerBoundaryArchTest absent)"
    note "A8" true
else
    echo "    rc=$A8_RC  test results: $A8_XML_SUMMARY (check rc=$A8_XML_RC)"
    tail -30 "$WORK/a8.log" | sed 's/^/      /'
    note "A8" false
fi

# ── 4a. HOOK WIRING BRANCHES — B (husky), C (lefthook), and branch A's worktree variant (P2-103)
#
# Everything above rides ONE wiring: branch A's `core.hooksPath .githooks`, installed by the
# hook-install-wiring command artifact. The skill prescribes three, and its own claim about the
# other two is that they run "the same hook body, pasted unedited" — a claim no run had ever
# executed, so a defect reachable only through husky's or lefthook's invocation path was invisible
# here and `green` said nothing whatsoever about it.
#
# These assertions re-wire THE VERY BODY ALREADY ON DISK ($PROJ/.githooks/pre-commit — the rendered
# hook-body marker, not a second copy) through each branch's own mechanism, and then ask the same
# question A-pc/A1 ask of branch A: is a planted violation actually refused, and does the gate name
# itself in the output. The single-source property is preserved exactly: an edited snippet in
# SKILL.md is what all three branches run tomorrow.
#
# HARNESS-INJECTED, DECLARED (not consumer-shape facts): (1) a minimal repo-root package.json —
# husky and lefthook are repo-root npm tools and neither can be installed without one, so a
# consumer CHOOSING either branch necessarily has it, while this 2-stack fixture (whose only
# package.json is under react.root) does not; (2) `lefthook.yml` itself, which is 5 lines of
# lefthook's own schema authored here rather than extracted — the ax-side content that can drift is
# the BODY, and that comes from the marker.
#
# SAFETY, MEASURED THE HARD WAY: `lefthook install` writes into the EFFECTIVE core.hooksPath, and
# on a machine carrying a GLOBAL core.hooksPath that directory is OUTSIDE this temp tree — a plain
# `git config --unset core.hooksPath` here made lefthook overwrite the calling user's own global
# pre-commit hook (reproduced, then restored by hand). So branch C sets a REPO-LOCAL
# `core.hooksPath .git/hooks` explicitly and never unsets, and the assertion additionally proves
# the global hooks directory was left untouched.
echo ""
echo "── hook wiring branches (P2-103) ─────────────────────────"
HOOK_BODY_SRC="$PROJ/.githooks/pre-commit"

# A15-worktree — branch A's LINKED-WORKTREE variant (D-8/F-2/F-3). The three claims the skill's
# prose makes, each measured: (1) `.git` is a FILE there, which is the detection A0 prescribes;
# (2) with `extensions.worktreeConfig` + `--worktree core.hooksPath`, git in the worktree really
# does invoke the hook — proven by the F-034 banner on an out-of-root commit, the one probe that
# needs neither node_modules nor a gradle build inside the worktree; (3) SIBLING NON-INTERFERENCE:
# the main worktree's `--show-origin --get core.hooksPath` is byte-identical before and after, and
# exactly one new config.worktree file exists.
#
# The F-2 core.bare/core.worktree preflight is measured as a PREMISE, not performed: this fixture
# is a normal checkout with neither set, which is precisely the documented case where the migration
# steps do not apply. A bare-main worktree farm — where that migration is the whole point — is
# still UNMEASURED and stays in the coverage print.
WT="$WORK/wt"
SIB_BEFORE="$(pgit config --show-origin --get core.hooksPath 2>&1)"
WT_BARE="$(pgit config --get core.bare 2>/dev/null)"
WT_CORE_WT="$(pgit config --get core.worktree 2>/dev/null)"
# MEASURED: `git init` writes `core.bare = false` into every ordinary checkout's shared config, so
# the OLD F-2 prose ("if EITHER prints a value, migrate") fired on the completely normal case — and
# its migration then set `core.bare true` in config.worktree, declaring a non-bare repo BARE
# (P2-122). The condition that actually selects the migration is `core.bare` being TRUE (or
# `core.worktree` being set), which is what is checked here AND, since P2-122, what the skill's own
# `hook-worktree-preflight` artifact implements — A15b-preflight below executes that artifact in
# both directions so the two can no longer drift apart silently.
WT_MIGRATION_APPLICABLE=0
[ "$WT_BARE" = "true" ] && WT_MIGRATION_APPLICABLE=1
[ -n "$WT_CORE_WT" ] && WT_MIGRATION_APPLICABLE=1
echo "  A15-worktree: F-2 preflight — core.bare='$WT_BARE' core.worktree='$WT_CORE_WT'"
echo "    migration applicable: $WT_MIGRATION_APPLICABLE (0 => normal checkout, the shape measured here)"
pgit worktree add -q -b ax-wt-probe "$WT" > "$WORK/a15-add.log" 2>&1
WT_ADD_RC=$?
WT_DOT_GIT_IS_FILE=0
[ -f "$WT/.git" ] && WT_DOT_GIT_IS_FILE=1
wgit() {   # git inside the LINKED worktree, same identity discipline as pgit
    git -c user.name="ax-downstream" -c user.email="ax-downstream@example.invalid" \
        -c commit.gpgsign=false -C "$WT" "$@"
}
if [ "$WT_ADD_RC" = "0" ] && [ "$WT_DOT_GIT_IS_FILE" = "1" ] && [ "$WT_MIGRATION_APPLICABLE" = "0" ]; then
    wgit config extensions.worktreeConfig true >/dev/null 2>&1
    wgit config --worktree core.hooksPath .githooks >/dev/null 2>&1
    printf '\n<!-- touched by verify-downstream A15 (linked worktree) -->\n' >> "$WT/README.md"
    wgit add README.md >/dev/null 2>&1
    WT_STAGED="$(wgit diff --cached --name-only | wc -l | tr -d ' ')"
    wgit commit -m "worktree: out-of-root touch" > "$WORK/a15.log" 2>&1
    WT_RC=$?
    WT_BANNER="$(grep -c 'ax-hook: pre-commit gate' "$WORK/a15.log")"
    SIB_AFTER="$(pgit config --show-origin --get core.hooksPath 2>&1)"
    WT_CONFIGS="$(ls "$PROJ/.git/worktrees/"*/config.worktree 2>/dev/null | wc -l | tr -d ' ')"
    if [ "$WT_RC" = "0" ] && [ "$WT_BANNER" != "0" ] && [ "$WT_STAGED" -ge 1 ] 2>/dev/null \
       && [ "$SIB_BEFORE" = "$SIB_AFTER" ] && [ "$WT_CONFIGS" = "1" ]; then
        echo "    .git is a FILE, hook ran in the worktree (banner), staged=$WT_STAGED"
        echo "    sibling core.hooksPath unchanged: '$SIB_AFTER'; new config.worktree files: $WT_CONFIGS"
        note "A15-worktree" true
    else
        echo "    rc=$WT_RC banner=$WT_BANNER staged=$WT_STAGED config_worktree_files=$WT_CONFIGS"
        echo "    sibling before='$SIB_BEFORE' after='$SIB_AFTER'"
        tail -25 "$WORK/a15.log" 2>/dev/null | sed 's/^/      /'
        note "A15-worktree" false
    fi
else
    echo "    premise FAILED: worktree add rc=$WT_ADD_RC .git-is-file=$WT_DOT_GIT_IS_FILE"
    echo "    core.bare='$WT_BARE' core.worktree='$WT_CORE_WT' => F-2 migration path, not this shape"
    tail -15 "$WORK/a15-add.log" 2>/dev/null | sed 's/^/      /'
    note "A15-worktree" false
fi
# Leave branch A's shared-config wiring exactly as the artifact set it for the branches below, and
# drop the worktree so nothing about B/C is entangled with worktreeConfig.
pgit worktree remove --force "$WT" >/dev/null 2>&1
pgit config --unset extensions.worktreeConfig >/dev/null 2>&1

# A15b-preflight — P2-122. A15 above measures the worktree WIRING and treats F-2 as a premise, so
# nothing ever executed the condition that DECIDES whether to migrate. That condition is now the
# `hook-worktree-preflight` artifact in ax-install-hooks/SKILL.md, and this assertion renders it
# from the skill (never a copy kept here) and runs it in BOTH directions:
#   ordinary `git init` checkout  -> must print MIGRATION NOT REQUIRED
#   `git init --bare` repository  -> must print MIGRATION REQUIRED
# BOTH are required, and that is what makes the assertion non-vacuous: a detector hardwired to
# either verdict fails one of the two. The normal-direction output is additionally required to
# contain core.bare='false' — the exact non-empty value the old prose mis-read as "migrate" — so
# this assertion proves the trap is PRESENT and not selected, rather than merely absent.
# Still unmeasured, and printed as such: the migration ITSELF (three mutating git commands), which
# only a bare-main worktree farm reaches.
cat > "$WORK/render_one.py" <<'PYEOF'
"""Render ONE ax:artifact body out of the skills, by id, to stdout.

Same discover()+render() path install.py and eval_diff.py use, for the same reason: the assertion
that consumes this must execute the SKILL FILE'S OWN text, not a second copy living in the harness.
--artifact-override is honoured here too, so a naive-condition body can be injected to watch the
assertion go RED.
"""
import glob
import json
import os
import sys

repo_root, aid, config_path, overrides_path = sys.argv[1:5]
sys.path.insert(0, os.path.join(repo_root, "practices", "scripts", "lib"))
import ax_markers  # noqa: E402

overrides = {}
if overrides_path != "-":
    with open(overrides_path, encoding="utf-8") as f:
        for spec in json.load(f):
            key, sep, path = spec.partition("=")
            if sep and os.path.isfile(path):
                with open(path, encoding="utf-8") as fh:
                    overrides[key] = fh.read()

with open(config_path, encoding="utf-8") as f:
    cfg = json.load(f)

for art in ax_markers.discover(
        sorted(glob.glob(os.path.join(repo_root, "skills", "*", "SKILL.md")))):
    if art.id != aid:
        continue
    if art.id in overrides:
        art.body = overrides[art.id]
    sys.stdout.write(ax_markers.render(art, cfg, {"axPluginPath": "/nonexistent-unused-here"}))
    sys.exit(0)
print("render_one: no ax:artifact with id=%r" % aid, file=sys.stderr)
sys.exit(1)
PYEOF
echo "  A15b-preflight: executing the skill's own F-2 preflight in BOTH directions …"
python3 "$WORK/render_one.py" "$REPO_ROOT" hook-worktree-preflight "$PROJ/ax.config.json" \
    "$OVERRIDE_FILE" > "$WORK/preflight.sh" 2>"$WORK/preflight-render.err"
PF_RENDER_RC=$?
PF_NORMAL="$WORK/pf-normal"
PF_BARE="$WORK/pf-bare"
mkdir -p "$PF_NORMAL"
( cd "$PF_NORMAL" && git -c init.defaultBranch=main init -q ) >/dev/null 2>&1
git init -q --bare "$PF_BARE" >/dev/null 2>&1
PF_NORMAL_OUT=""
PF_BARE_OUT=""
if [ "$PF_RENDER_RC" = "0" ]; then
    PF_NORMAL_OUT="$( cd "$PF_NORMAL" && bash "$WORK/preflight.sh" 2>&1 )"
    PF_BARE_OUT="$( cd "$PF_BARE" && bash "$WORK/preflight.sh" 2>&1 )"
fi
PF_NORMAL_OK=0
PF_BARE_OK=0
PF_TRAP_PRESENT=0
case "$PF_NORMAL_OUT" in *"MIGRATION NOT REQUIRED"*) PF_NORMAL_OK=1 ;; esac
case "$PF_BARE_OUT"   in *"MIGRATION REQUIRED"*)     PF_BARE_OK=1 ;; esac
case "$PF_NORMAL_OUT" in *"core.bare='false'"*)      PF_TRAP_PRESENT=1 ;; esac
echo "    normal checkout : $PF_NORMAL_OUT"
echo "    bare repository : $PF_BARE_OUT"
if [ "$PF_RENDER_RC" = "0" ] && [ "$PF_NORMAL_OK" = "1" ] && [ "$PF_BARE_OK" = "1" ] \
   && [ "$PF_TRAP_PRESENT" = "1" ]; then
    echo "    both directions correct, and the normal checkout really does carry the non-empty"
    echo "    core.bare='false' the pre-P2-122 prose mis-read as \"migrate\""
    note "A15b-preflight" true
else
    echo "    render rc=$PF_RENDER_RC normal_ok=$PF_NORMAL_OK bare_ok=$PF_BARE_OK trap_present=$PF_TRAP_PRESENT"
    [ "$PF_RENDER_RC" != "0" ] && sed 's/^/      /' "$WORK/preflight-render.err" 2>/dev/null
    [ "$PF_NORMAL_OK" = "0" ] && echo "      a detector that fires on a NORMAL checkout would migrate a non-bare repo to bare"
    note "A15b-preflight" false
fi
rm -rf "$PF_NORMAL" "$PF_BARE"

# husky and lefthook are repo-root npm tools: neither installs without a package.json at the repo
# root. Since P2-121 the react plugin is installed under <react.root>, so in THIS 2-stack shape the
# repo root normally has none and the harness creates a minimal one — declared, not consumer-shape.
# The `else` branch is kept and still matters: when a repo-root package.json DOES exist (a
# react.root="." consumer, or a repo that already had one), overwriting it was measured to make npm
# prune node_modules/@ax on the next root install, after which every later react-gate invocation
# died with ERR_MODULE_NOT_FOUND — i.e. the branch-B/C assertions failed for a reason that had
# nothing to do with husky or lefthook. Create one only when genuinely absent.
if [ ! -f "$PROJ/package.json" ]; then
    cat > "$PROJ/package.json" <<'EOF'
{
  "name": "ax-downstream-consumer-root",
  "private": true,
  "version": "0.0.0",
  "description": "Repo-root package.json — required by husky/lefthook (hook branches B/C)."
}
EOF
    echo "  (repo-root package.json created by the harness — none existed)"
else
    echo "  (repo-root package.json already present — left untouched)"
fi

# A13-husky — BRANCH B. husky sets its own repo-local core.hooksPath (.husky/_), so it REPLACES
# branch A's wiring rather than racing it.
#
# HONEST LIMIT, read out of husky's own shim rather than assumed: `.husky/_/h` ends in
# `sh -e "$s"`, so husky executes the hook body with the PLATFORM /bin/sh and DISCARDS the body's
# `#!/usr/bin/env bash` shebang. On this machine /bin/sh is bash-in-POSIX-mode, which accepts the
# body's `set -euo pipefail`; on a platform whose /bin/sh is dash it would not. What this assertion
# measures is therefore "the body runs under THIS platform's sh", and the shell is printed so the
# verdict cannot be read as broader than that.
echo "  A13-husky: npm i -D husky + npx husky init, then the SAME body via .husky/pre-commit …"
HUSKY_SH="$( (command -v sh) 2>/dev/null)"
HUSKY_PIPEFAIL=no
sh -c 'set -o pipefail' >/dev/null 2>&1 && HUSKY_PIPEFAIL=yes
echo "    husky runs the body with 'sh -e' (shebang discarded): sh=$HUSKY_SH, supports pipefail=$HUSKY_PIPEFAIL"
( cd "$PROJ" && npm i -D husky --silent && npx husky init ) > "$WORK/a13-install.log" 2>&1
A13_INSTALL_RC=$?
A13_HOOKSPATH="$(pgit config --get core.hooksPath 2>/dev/null)"
if [ "$A13_INSTALL_RC" = "0" ] && [ -d "$PROJ/.husky/_" ]; then
    cp "$HOOK_BODY_SRC" "$PROJ/.husky/pre-commit"
    chmod +x "$PROJ/.husky/pre-commit"
    plant_upward_probe
    pgit commit -m "probe: upward layer import (husky wiring)" > "$WORK/a13.log" 2>&1
    A13_RC=$?
    A13_BANNER="$(grep -ac 'ax-hook: pre-commit gate' "$WORK/a13.log")"
    A13_RULE="$(grep -ac 'ax/no-upward-layer-import' "$WORK/a13.log")"
    if [ "$A13_RC" != "0" ] && [ "$A13_BANNER" != "0" ] && [ "$A13_RULE" != "0" ]; then
        echo "    core.hooksPath='$A13_HOOKSPATH' (husky's own), commit REFUSED, banner + rule id present"
        note "A13-husky" true
    else
        echo "    rc=$A13_RC (expected non-zero) hooksPath='$A13_HOOKSPATH' banner=$A13_BANNER rule=$A13_RULE"
        tail -30 "$WORK/a13.log" | sed 's/^/      /'
        note "A13-husky" false
    fi
else
    echo "    husky install/init failed (rc=$A13_INSTALL_RC) — branch B UNMEASURED, recorded as a failure"
    tail -20 "$WORK/a13-install.log" | sed 's/^/      /'
    note "A13-husky" false
fi

# A13b-posix — P2-123. A13 above can only ever measure the CALLING platform's /bin/sh. On macOS
# that is bash-in-POSIX-mode, which accepts `set -o pipefail`; on Debian/Ubuntu — where most CI
# runs — /bin/sh is dash, which aborts at that line with `set: Illegal option -o pipefail` BEFORE
# the banner. The gate then never executes and the commit sails through, and A13 is green either
# way. Two halves:
#   (i)  STATIC, always measured — the rendered body ON DISK carries none of a registered bashism
#        list. Only NON-COMMENT lines are scanned, deliberately: the body explains in comments
#        exactly which constructs it avoids, and naming them there must not trip the check.
#   (ii) DYNAMIC, measured whenever a shell that genuinely REJECTS pipefail is installed — that
#        same on-disk body executed the way husky executes it (`<sh> -e <body>`, shebang discarded)
#        in a throwaway repo with nothing staged, which must print the F-034 banner and exit 0.
#        The candidate shell is itself probed for rejecting pipefail: a "posix shell" that accepts
#        it would make this half vacuous, so it is not accepted as the probe.
# When no such shell exists the dynamic half is printed as UNMEASURED — never folded silently into
# the verdict, and never a claim about Debian/Ubuntu as a platform (only about its shell).
echo "  A13b-posix: the same on-disk body under a dash-class /bin/sh …"
A13B_BASHISMS=""
A13B_CODE="$WORK/a13b-code.sh"
grep -v '^[[:space:]]*#' "$HOOK_BODY_SRC" > "$A13B_CODE" 2>/dev/null
# Two pattern choices here were each measured against the PRE-FIX body rather than assumed, because
# a bashism scanner that matches nothing is exactly as green as a clean body:
#   · `pipefail`, bare — NOT `-o pipefail`. The defect's actual spelling is `set -euo pipefail`,
#     whose characters are `-euo pipefail`: it contains `o pipefail` but never `-o pipefail`, so the
#     hyphenated pattern silently missed the one string this assertion exists to catch (measured on
#     /tmp render of the pre-fix body: zero matches). The bare word matches both spellings, and the
#     comment-stripping above is what keeps the body's own explanation of why it avoids pipefail
#     from tripping it.
#   · `[[ ` carries its trailing space deliberately: the bash conditional is always `[[ <expr> ]]`,
#     whereas `[[` with no space is how a POSIX CHARACTER CLASS opens inside a bracket expression —
#     `[[:space:]]`, which this body uses in its sed fallback and which every POSIX sh accepts. A
#     bare `[[` pattern reported that as a bashism (measured: `static: bashisms ... [[[]` while the
#     dash run passed), i.e. it flagged the very portability construct the body uses to BE portable.
for pat in 'pipefail' '[[ ' '<<<' 'local ' 'declare ' '+=('; do
    if grep -qF -- "$pat" "$A13B_CODE"; then
        A13B_BASHISMS="$A13B_BASHISMS [$pat]"
    fi
done
A13B_SH=""
for cand in "$(command -v dash 2>/dev/null || true)" /bin/dash /usr/bin/dash; do
    [ -n "$cand" ] && [ -x "$cand" ] || continue
    "$cand" -c 'set -o pipefail' >/dev/null 2>&1 && continue   # accepts it => useless as a probe
    A13B_SH="$cand"
    break
done
A13B_DYNAMIC=UNMEASURED
A13B_RC=""
A13B_BANNER=0
if [ -n "$A13B_SH" ]; then
    PXP="$WORK/posix-probe"
    rm -rf "$PXP"
    mkdir -p "$PXP"
    cp "$PROJ/ax.config.json" "$PXP/ax.config.json"
    ( cd "$PXP" && git -c init.defaultBranch=main init -q ) >/dev/null 2>&1
    ( cd "$PXP" && "$A13B_SH" -e "$HOOK_BODY_SRC" ) > "$WORK/a13b.log" 2>&1
    A13B_RC=$?
    A13B_BANNER="$(grep -ac 'ax-hook: pre-commit gate' "$WORK/a13b.log")"
    if [ "$A13B_RC" = "0" ] && [ "$A13B_BANNER" != "0" ]; then
        A13B_DYNAMIC=pass
    else
        A13B_DYNAMIC=FAIL
    fi
    rm -rf "$PXP"
fi
echo "    static  : bashisms on non-comment lines:${A13B_BASHISMS:- none}"
echo "    dynamic : posix-only shell=${A13B_SH:-none installed} => $A13B_DYNAMIC (rc=${A13B_RC:--} banner=$A13B_BANNER)"
if [ -z "$A13B_BASHISMS" ] && [ "$A13B_DYNAMIC" != "FAIL" ]; then
    [ "$A13B_DYNAMIC" = "UNMEASURED" ] && echo "    NOTE: no dash-class shell here — the verdict rests on the static half ALONE."
    note "A13b-posix" true
else
    [ -n "$A13B_BASHISMS" ] && echo "    a bashism here is fatal under husky's 'sh -e' on a dash /bin/sh, and invisible on macOS"
    [ "$A13B_DYNAMIC" = "FAIL" ] && tail -15 "$WORK/a13b.log" 2>/dev/null | sed 's/^/      /'
    note "A13b-posix" false
fi

# A14-lefthook — BRANCH C. The body goes to the helper script lefthook.yml's `run:` invokes.
# core.hooksPath is pointed at the repo's OWN .git/hooks first — see the SAFETY note above; a
# `--unset` here was measured to make `lefthook install` clobber the calling user's global hook.
echo "  A14-lefthook: npm i -D lefthook + lefthook.yml -> helper carrying the SAME body …"
GLOBAL_HOOKS_DIR="$(git config --global --get core.hooksPath 2>/dev/null)"
list_global_hooks() {   # portable: the listing text itself, no md5/md5sum split
    [ -n "$GLOBAL_HOOKS_DIR" ] && [ -d "$GLOBAL_HOOKS_DIR" ] || return 0
    ( cd "$GLOBAL_HOOKS_DIR" && ls -l ) 2>/dev/null
}
GLOBAL_HOOKS_BEFORE="$(list_global_hooks)"
pgit config core.hooksPath .git/hooks >/dev/null 2>&1
mkdir -p "$PROJ/.githooks"
cp "$HOOK_BODY_SRC" "$PROJ/.githooks/ax-pre-commit-checks.sh"
chmod +x "$PROJ/.githooks/ax-pre-commit-checks.sh"
cat > "$PROJ/lefthook.yml" <<'EOF'
pre-commit:
  commands:
    ax-gates:
      run: bash .githooks/ax-pre-commit-checks.sh
EOF
( cd "$PROJ" && npm i -D lefthook --silent && npx lefthook install --force ) > "$WORK/a14-install.log" 2>&1
A14_INSTALL_RC=$?
GLOBAL_HOOKS_AFTER="$(list_global_hooks)"
if [ "$A14_INSTALL_RC" = "0" ] && [ -f "$PROJ/.git/hooks/pre-commit" ]; then
    plant_upward_probe
    pgit commit -m "probe: upward layer import (lefthook wiring)" > "$WORK/a14.log" 2>&1
    A14_RC=$?
    A14_BANNER="$(grep -ac 'ax-hook: pre-commit gate' "$WORK/a14.log")"
    A14_RULE="$(grep -ac 'ax/no-upward-layer-import' "$WORK/a14.log")"
    if [ "$A14_RC" != "0" ] && [ "$A14_BANNER" != "0" ] && [ "$A14_RULE" != "0" ] \
       && [ "$GLOBAL_HOOKS_BEFORE" = "$GLOBAL_HOOKS_AFTER" ]; then
        echo "    lefthook installed into the repo's own .git/hooks, commit REFUSED, banner + rule id present"
        echo "    global hooks dir '${GLOBAL_HOOKS_DIR:-none}' unchanged"
        note "A14-lefthook" true
    else
        echo "    rc=$A14_RC (expected non-zero) banner=$A14_BANNER rule=$A14_RULE"
        if [ "$GLOBAL_HOOKS_BEFORE" != "$GLOBAL_HOOKS_AFTER" ]; then
            echo "    ⚠ global hooks dir '${GLOBAL_HOOKS_DIR:-none}' listing CHANGED — lefthook wrote"
            echo "      OUTSIDE this temp tree. Restore it by hand (lefthook renames the original to"
            echo "      <hook>.old); the repo-local core.hooksPath above exists to prevent exactly this."
        fi
        tail -30 "$WORK/a14.log" | sed 's/^/      /'
        note "A14-lefthook" false
    fi
else
    echo "    lefthook install failed (rc=$A14_INSTALL_RC) — branch C UNMEASURED, recorded as a failure"
    tail -20 "$WORK/a14-install.log" | sed 's/^/      /'
    note "A14-lefthook" false
fi

# ── 4c. A SECOND CONSUMER SHAPE — RENAMED GATE TASK, JAVA-ONLY STACK (P2-104) ────────────────
#
# Everything above installs and runs ONE config. A9-eval renders two, but rendering is not running:
# a conditional that renders correctly and then breaks at install or invocation time is exactly what
# a render-only differential cannot see. This shape is materialized, installed, and EXECUTED:
#
#   stacks       = ["java"]            (the react region of the hook body must be GONE on disk)
#   java.testTask= "verifyAxPractices" (NOT the default — the gate task must carry this name)
#
# It found a real defect, which is why it exists: `java-gradle-testpractices` hardcoded
# `tasks.register<Test>("testPractices")` while the hook body resolves `java.testTask` from
# ax.config.json (F-032). A consumer who set any other name got `Task 'x' not found in root project`
# on every java-touching commit and the gate never ran once — measurable only by actually invoking
# it under a non-default name. Fixed as F-035 in ax-install-java-enforcement; re-inject the old body
# with `--artifact-override java-gradle-testpractices=<pre-fix file>` to watch A16 go RED.
#
# Cost control: this shape needs NO `npm ci`. Its assertions stage backend files only, so the hook's
# react block (which the stacks=["java"] config deletes anyway) is never reached and no frontend
# dependency tree is required.
echo ""
echo "── second shape: java-only, renamed gate task (P2-104) ───"
S2="$WORK/shape2"
S2_BACKEND="$S2/backend"
S2_TASK="verifyAxPractices"
mkdir -p "$S2"
# A failure here needs no special-casing: without ax.config.json the patch below and then
# install.py both fail, and the S2_INSTALL_RC branch records both assertions as failures once.
( cd "$FIXTURE_SRC" && tar cf - . ) | ( cd "$S2" && tar xf - ) \
    || echo "    could not copy the fixture into $S2"
mkdir -p "$S2_BACKEND/gradle/wrapper"
cp "$REPO_ROOT/backend/gradlew" "$S2_BACKEND/gradlew" 2>/dev/null && chmod +x "$S2_BACKEND/gradlew"
cp "$REPO_ROOT/backend/gradle/wrapper/gradle-wrapper.jar" "$S2_BACKEND/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null
cat > "$S2_BACKEND/gradle/wrapper/gradle-wrapper.properties" <<EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=$GRADLE_DIST_URL
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
# The config is PATCHED, not rewritten: every other key stays exactly what the committed fixture
# says, so the only differences between this shape and the first are the two being measured.
python3 - "$S2/ax.config.json" "$S2_TASK" <<'PYEOF'
import json, sys
path, task = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as f:
    cfg = json.load(f)
cfg["stacks"] = ["java"]
cfg["java"]["testTask"] = task
cfg.setdefault("react", {})["typescript"] = False
with open(path, "w", encoding="utf-8") as f:
    json.dump(cfg, f, indent=2)
    f.write("\n")
PYEOF
s2git() {
    git -c user.name="ax-downstream" -c user.email="ax-downstream@example.invalid" \
        -c commit.gpgsign=false -c init.defaultBranch=main -C "$S2" "$@"
}
s2git init -q >/dev/null 2>&1
s2git add -A >/dev/null 2>&1
s2git commit -q --no-verify -m "shape2 baseline" >/dev/null 2>&1
python3 "$WORK/install.py" "$REPO_ROOT" "$S2" "$PLUGIN_PATH" "$OVERRIDE_FILE" \
    "$WORK/install-shape2.json" > "$WORK/s2-install.log" 2>&1
S2_INSTALL_RC=$?
sed 's/^/  /' "$WORK/s2-install.log" | tail -14
s2git add -A >/dev/null 2>&1
s2git commit -q --no-verify -m "shape2: install ax artifacts" >/dev/null 2>&1

if [ "$S2_INSTALL_RC" != "0" ]; then
    echo "    artifact installation failed for this shape (rc=$S2_INSTALL_RC) — both assertions FAIL"
    note "A16-alttask" false
    note "A17-alttask" false
else
    # (i) the react region is GONE from the hook ON DISK, not merely from a render;
    # (ii) the gate task is discoverable under the CONFIGURED name and the default name is absent;
    # (iii) a planted java violation is refused, by that task, naming the probe class.
    S2_HOOK="$S2/.githooks/pre-commit"
    S2_REACT_REGION="$(grep -c 'REACT_TOUCHED' "$S2_HOOK" 2>/dev/null | tr -d ' ')"
    echo "  A16-alttask: ./gradlew tasks --all (no -P) …"
    ( cd "$S2_BACKEND" && ./gradlew tasks --all ) > "$WORK/a16-tasks.log" 2>&1
    S2_TASKS_RC=$?
    S2_HAS_ALT="$(grep -cE "^${S2_TASK}( |\$)" "$WORK/a16-tasks.log" | tr -d ' ')"
    S2_HAS_DEFAULT="$(grep -cE '^testPractices( |$)' "$WORK/a16-tasks.log" | tr -d ' ')"
    mkdir -p "$S2_BACKEND/src/main/java/com/example/backend/probe"
    cat > "$S2_BACKEND/src/main/java/com/example/backend/probe/ProbeController.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

	public String describe() {
		return "probe";
	}

}
EOF
    cat > "$S2_BACKEND/src/main/java/com/example/backend/probe/ProbeService.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.stereotype.Service;

// Planted by verify-downstream.sh (shape 2): a *Service depending on a *Controller, refused by the
// gate task registered under the CONFIGURED name.
@Service
public class ProbeService {

	private final ProbeController probeController;

	public ProbeService(ProbeController probeController) {
		this.probeController = probeController;
	}

	public String describe() {
		return probeController.describe();
	}

}
EOF
    s2git add backend/src/main/java/com/example/backend/probe >/dev/null 2>&1
    echo "    committing ProbeService -> ProbeController (must be BLOCKED by :$S2_TASK) …"
    s2git commit -m "shape2 probe: service depends on controller" > "$WORK/a16.log" 2>&1
    S2_RC=$?
    S2_NAMES_TASK="$(grep -c "Task :${S2_TASK}" "$WORK/a16.log" | tr -d ' ')"
    S2_NAMES_PROBE="$(grep -c 'ProbeService' "$WORK/a16.log" | tr -d ' ')"
    echo "    hook react region on disk: $S2_REACT_REGION occurrence(s) (0 expected for stacks=[java])"
    echo "    tasks --all: rc=$S2_TASKS_RC  '$S2_TASK' listed=$S2_HAS_ALT  'testPractices' listed=$S2_HAS_DEFAULT"
    if [ "$S2_REACT_REGION" = "0" ] && [ "$S2_TASKS_RC" = "0" ] && [ "$S2_HAS_ALT" != "0" ] \
       && [ "$S2_HAS_DEFAULT" = "0" ] && [ "$S2_RC" != "0" ] && [ "$S2_NAMES_TASK" != "0" ] \
       && [ "$S2_NAMES_PROBE" != "0" ]; then
        echo "    commit REFUSED by :$S2_TASK, probe class named in the output"
        note "A16-alttask" true
    else
        echo "    commit rc=$S2_RC (expected non-zero)  ':$S2_TASK' in output=$S2_NAMES_TASK  ProbeService=$S2_NAMES_PROBE"
        tail -30 "$WORK/a16.log" | sed 's/^/      /'
        note "A16-alttask" false
    fi

    # A17-alttask — the pass-after half, plus the non-vacuity check A4 makes for the default name:
    # a renamed gate that runs ZERO tests reports BUILD SUCCESSFUL just as convincingly.
    cat > "$S2_BACKEND/src/main/java/com/example/backend/probe/ProbeService.java" <<'EOF'
package com.example.backend.probe;

import org.springframework.stereotype.Service;

// Same file as the shape-2 probe, violation repaired.
@Service
public class ProbeService {

	public String describe() {
		return "probe";
	}

}
EOF
    rm -rf "$S2_BACKEND/build/test-results/$S2_TASK"
    s2git add -A backend/src >/dev/null 2>&1
    echo "  A17-alttask: committing the repair (must PASS, and :$S2_TASK must run > 0 tests) …"
    s2git commit -m "shape2 probe: violation removed" > "$WORK/a17.log" 2>&1
    S2R_RC=$?
    S2R_XML="$(python3 "$WORK/check.py" junit-tests "$S2_BACKEND/build/test-results/$S2_TASK")"
    S2R_XML_RC=$?
    if [ "$S2R_RC" = "0" ] && [ "$S2R_XML_RC" = "0" ]; then
        echo "    commit rc=0, $S2_TASK results: $S2R_XML"
        note "A17-alttask" true
    else
        echo "    commit rc=$S2R_RC  $S2_TASK results: $S2R_XML (check rc=$S2R_XML_RC)"
        tail -30 "$WORK/a17.log" | sed 's/^/      /'
        note "A17-alttask" false
    fi
fi

# ── 4d. A THIRD CONSUMER SHAPE — react.root="." (P2-104a) ────────────────────────────────────
#
# `react.root="."` is not a cosmetic variation: it selects DIFFERENT CODE in two artifacts, and
# neither branch had ever run.
#   1. eslint.config.mjs's findAxConfig() (F-030) walks UP from the config file's own directory. With
#      react.root="frontend" it finds ax.config.json one level up on the SECOND iteration; with "."
#      it must find it in the SAME directory on the FIRST — a first-iteration bug (an unconditional
#      `path.dirname(dir)` before the first existsSync, say) is invisible in the frontend shape.
#   2. the hook body branches on `[ "$REACT_ROOT" = "." ]` and switches from a path-prefix scope
#      check to a FILE-EXTENSION proxy, because a `^./` prefix would match every staged file. That
#      whole branch — including its scope-SKIP behaviour — was dead code in every previous run.
#
# The fixture is reused with its frontend contents RELOCATED to the repo root, which is what a
# Next-at-root + backend-subdir consumer actually looks like. stacks=["react"] keeps gradle out of
# this shape entirely (the java region is deleted from the hook), so the cost is one `npm ci`.
echo ""
echo "── third shape: react.root=\".\" (P2-104a) ─────────────────"
S3="$WORK/shape3"
mkdir -p "$S3"
( cd "$FIXTURE_SRC" && tar cf - . ) | ( cd "$S3" && tar xf - ) \
    || echo "    could not copy the fixture into $S3"
# Relocate frontend/* to the repo root — no collisions (the fixture root holds only ax.config.json,
# README.md, .gitignore and backend/), so this is a move, never a merge.
if [ -d "$S3/frontend" ]; then
    ( cd "$S3/frontend" && tar cf - . ) | ( cd "$S3" && tar xf - )
    rm -rf "$S3/frontend"
fi
python3 - "$S3/ax.config.json" <<'PYEOF'
import json, sys
with open(sys.argv[1], encoding="utf-8") as f:
    cfg = json.load(f)
cfg["stacks"] = ["react"]
cfg["react"]["root"] = "."
with open(sys.argv[1], "w", encoding="utf-8") as f:
    json.dump(cfg, f, indent=2)
    f.write("\n")
PYEOF
s3git() {
    git -c user.name="ax-downstream" -c user.email="ax-downstream@example.invalid" \
        -c commit.gpgsign=false -c init.defaultBranch=main -C "$S3" "$@"
}
s3git init -q >/dev/null 2>&1
s3git add -A >/dev/null 2>&1
s3git commit -q --no-verify -m "shape3 baseline" >/dev/null 2>&1
( cd "$S3" && npm ci ) > "$WORK/s3-npm-ci.log" 2>&1
S3_NPM_RC=$?
python3 "$WORK/install.py" "$REPO_ROOT" "$S3" "$PLUGIN_PATH" "$OVERRIDE_FILE" \
    "$WORK/install-shape3.json" > "$WORK/s3-install.log" 2>&1
S3_INSTALL_RC=$?
sed 's/^/  /' "$WORK/s3-install.log" | tail -12
s3git add -A >/dev/null 2>&1
s3git commit -q --no-verify -m "shape3: install ax artifacts" >/dev/null 2>&1

S3_PROBE="$S3/src/lib/__ax_probe.ts"
if [ "$S3_NPM_RC" != "0" ] || [ "$S3_INSTALL_RC" != "0" ]; then
    echo "    setup failed (npm ci rc=$S3_NPM_RC, install rc=$S3_INSTALL_RC) — all three FAIL"
    tail -15 "$WORK/s3-npm-ci.log" 2>/dev/null | sed 's/^/      /'
    tail -15 "$WORK/s3-install.log" 2>/dev/null | sed 's/^/      /'
    note "A18-rootdot" false
    note "A19-rootdot" false
    note "A20-rootdot-skip" false
else
    cat > "$S3_PROBE" <<'EOF'
// Planted by verify-downstream.sh (shape 3, react.root="."): shared (lib) importing UP into app.
import { probe } from '@/app/__ax_probe_target'

export const __axProbe: string = probe
EOF
    s3git add src/lib/__ax_probe.ts >/dev/null 2>&1
    echo "  A18-rootdot: committing a shared -> app upward import at react.root=\".\" (must be BLOCKED) …"
    s3git commit -m "shape3 probe: upward layer import" > "$WORK/a18.log" 2>&1
    S3_RC=$?
    S3_BANNER="$(grep -ac 'ax-hook: pre-commit gate (react=\. ' "$WORK/a18.log")"
    S3_RULE="$(grep -ac 'ax/no-upward-layer-import' "$WORK/a18.log")"
    S3_AXCFG_THROW="$(grep -ac 'ax.config.json not found' "$WORK/a18.log")"
    if [ "$S3_RC" != "0" ] && [ "$S3_BANNER" != "0" ] && [ "$S3_RULE" != "0" ]; then
        echo "    commit REFUSED, banner shows react=., rule id named — findAxConfig resolved on the"
        echo "    FIRST iteration (ax.config.json is a SIBLING of eslint.config.mjs in this shape)"
        note "A18-rootdot" true
    else
        echo "    rc=$S3_RC (expected non-zero) banner(react=.)=$S3_BANNER rule=$S3_RULE"
        [ "$S3_AXCFG_THROW" != "0" ] && echo "    findAxConfig THREW 'ax.config.json not found' — the F-030 first-try path is broken"
        tail -30 "$WORK/a18.log" | sed 's/^/      /'
        note "A18-rootdot" false
    fi

    # A19-rootdot — pass-after, with the same non-vacuity half A2 uses (results > 0 rules out a glob
    # that matched nothing, which at react.root="." is a live risk: srcDir is resolved relative to a
    # different directory than in shape 1).
    cat > "$S3_PROBE" <<'EOF'
// Same file as the shape-3 probe, violation removed.
export const __axProbe: string = 'ok'
EOF
    s3git add src/lib/__ax_probe.ts >/dev/null 2>&1
    echo "  A19-rootdot: committing the same path with the violation removed (must PASS) …"
    s3git commit -m "shape3 probe: upward layer import removed" > "$WORK/a19.log" 2>&1
    S3R_RC=$?
    ( cd "$S3" && npx eslint src --format json ) > "$WORK/a19-eslint.json" 2>"$WORK/a19-eslint.err"
    S3R_ESLINT="$(python3 "$WORK/check.py" eslint-json "$WORK/a19-eslint.json")"
    S3R_ESLINT_RC=$?
    if [ "$S3R_RC" = "0" ] && [ "$S3R_ESLINT_RC" = "0" ]; then
        echo "    commit rc=0, eslint json: $S3R_ESLINT"
        note "A19-rootdot" true
    else
        echo "    commit rc=$S3R_RC  eslint json: $S3R_ESLINT (check rc=$S3R_ESLINT_RC)"
        tail -25 "$WORK/a19.log" | sed 's/^/      /'
        note "A19-rootdot" false
    fi

    # A20-rootdot-skip — the EXTENSION-PROXY scope skip, the other half of the `"."` branch. At
    # react.root="." a path-prefix check is useless, so the hook falls back to matching staged file
    # EXTENSIONS; a file with none of them must skip the react gate while the F-034 banner still
    # proves the hook ran. This is A5's claim transplanted onto the branch that implements it
    # differently — and unlike A5, "outside the root" is not even expressible here.
    s3git reset -q >/dev/null 2>&1
    printf '\n<!-- touched by verify-downstream A20 -->\n' >> "$S3/README.md"
    s3git add README.md >/dev/null 2>&1
    S3S_STAGED="$(s3git diff --cached --name-only | wc -l | tr -d ' ')"
    echo "  A20-rootdot-skip: committing README.md (no react file extension) …"
    s3git commit -m "shape3: touch a file with no react extension" > "$WORK/a20.log" 2>&1
    S3S_RC=$?
    S3S_BANNER="$(grep -ac 'ax-hook: pre-commit gate' "$WORK/a20.log")"
    S3S_NPM="$(grep -ac -- '--max-warnings' "$WORK/a20.log")"
    if [ "$S3S_RC" = "0" ] && [ "$S3S_BANNER" != "0" ] && [ "$S3S_NPM" = "0" ] \
       && [ "$S3S_STAGED" -ge 1 ] 2>/dev/null; then
        echo "    commit rc=0, staged=$S3S_STAGED, banner present, npm gate NOT invoked"
        note "A20-rootdot-skip" true
    else
        echo "    rc=$S3S_RC staged=$S3S_STAGED banner=$S3S_BANNER npm_marker=$S3S_NPM"
        tail -25 "$WORK/a20.log" | sed 's/^/      /'
        note "A20-rootdot-skip" false
    fi
fi

# ── 4b. ASSERTION MANIFEST CROSS-CHECK ───────────────────────────────────────────────────────
# The `# ax:assertions` header line is what guard [114] reads to know how many assertions a
# complete run must record. A declaration nobody checks is a second, silently-drifting source of
# truth — so the set DECLARED there is compared here against the set this run ACTUALLY recorded via
# note(). They must be equal: an assertion added below without updating the line (or a declared id
# whose note() call was deleted) fails the run outright instead of quietly shrinking what [114]
# demands. Only reached on the full path — the early finish() exits log a partial assertion set and
# a non-"pass" verdict, which [114] rejects on its own terms.
DECLARED_IDS="$(sed -n 's/^#[[:space:]]*ax:assertions[[:space:]]\{1,\}//p' "${BASH_SOURCE[0]}")"
DECLARED_LINES="$(printf '%s\n' "$DECLARED_IDS" | grep -c . )"
DECLARED_SORTED="$(printf '%s\n' $DECLARED_IDS | grep -v '^$' | sort -u)"
RECORDED_SORTED="$(cut -f1 "$ASSERT_FILE" | grep -v '^$' | sort -u)"
if [ "$DECLARED_LINES" != "1" ]; then
    echo "verify-downstream: the '# ax:assertions' declaration must appear EXACTLY once in this" >&2
    echo "  file (found $DECLARED_LINES). Guard [114] parses it to know the complete assertion set." >&2
    finish 8 "assertion-manifest-drift"
fi
if [ "$DECLARED_SORTED" != "$RECORDED_SORTED" ]; then
    echo "verify-downstream: ASSERTION MANIFEST DRIFT — the '# ax:assertions' declaration and the" >&2
    echo "  ids this run actually recorded are not the same set." >&2
    echo "    declared: $(printf '%s ' $DECLARED_SORTED)" >&2
    echo "    recorded: $(printf '%s ' $RECORDED_SORTED)" >&2
    echo "  Update the declaration (or restore the missing note() call) — guard [114] would" >&2
    echo "  otherwise enforce completeness against a set that is not what this harness measures." >&2
    finish 8 "assertion-manifest-drift"
fi
echo "  manifest: $(printf '%s ' $DECLARED_SORTED)— declared set == recorded set"

# ── 5. VERDICT ───────────────────────────────────────────────────────────────────────────────
PASSED="$(grep -c '	true$' "$ASSERT_FILE")"
FAILED="$(grep -c '	false$' "$ASSERT_FILE")"
echo ""
echo "═══ RESULT ═══"
echo "  assertions: $PASSED passed, $FAILED failed"
if [ -n "$OVERRIDE_IDS" ]; then
    echo "  OVERRIDE ACTIVE ($OVERRIDE_IDS) — regression differential only, NOT release evidence."
fi
if [ "$FAILED" != "0" ]; then
    grep '	false$' "$ASSERT_FILE" | sed 's/^/    FAILED: /;s/	false$//'
    finish 1 "real-finding"
fi
finish 0 "pass"
