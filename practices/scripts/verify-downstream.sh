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
#   · It runs ONE shape: `react.root="frontend"` + `java.root="backend"`, TypeScript ON, Gradle
#     9.5.1, Node/npm as installed on the calling machine. It does NOT sweep `react.root:"."`,
#     `react.typescript=false`, a different `java.testTask` name, other Gradle/Node versions, or
#     Windows. Those branches of the install skills are UNMEASURED by this run.
#   · It installs the skills' MACHINE-EXTRACTABLE artifacts (the ax:artifact-marked blocks). The
#     skills' PROSE steps — detection heuristics, husky/lefthook branches, the worktree
#     `core.bare`/`worktreeConfig` preflight, the manual probe→detect→delete verification — are
#     NOT executed here. Only branch A (`core.hooksPath`) of ax-install-hooks is exercised.
#   · It is NOT R25. It does not run run-all-guards.sh, the per-domain gradle tasks, or
#     ax-template's own frontend lint.
#   · Two things the fixture does not commit are injected by THIS script at materialization time
#     and are therefore harness decisions, not consumer-shape facts: the Gradle wrapper
#     (`gradlew` + `gradle-wrapper.jar` copied from backend/, with a GENERATED
#     `gradle-wrapper.properties` pinning gradle-9.5.1-bin.zip), and `GRADLE_OPTS`
#     -Dorg.gradle.daemon=false (so no daemon outlives the temp tree).
#   · The one artifact class the markers cannot fully specify is `kind=file-fragment`: a marker
#     says WHICH file a fragment belongs to, not WHERE inside it. This script applies two
#     documented merge rules (package.json -> JSON deep-merge; a *.gradle.kts fragment consisting
#     solely of dependency-notation calls -> inserted into the `dependencies { }` block, anything
#     else -> appended at top level). A consumer following the skill by hand makes the same
#     decision by reading; here it is mechanical, and it is a MODEL of that step, not a
#     measurement of it.
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
    echo "  · ONE consumer shape only: react.root=frontend, java.root=backend, TypeScript ON."
    echo "    UNMEASURED: react.root=\".\", react.typescript=false, a different java.testTask"
    echo "    name, husky/lefthook hook branches, other Gradle/Node versions, Windows."
    echo "  · The install skills' PROSE steps (detection heuristics, worktree core.bare preflight,"
    echo "    manual probe->detect->delete verification) are NOT executed — only the"
    echo "    ax:artifact-marked blocks, and only hook branch A (core.hooksPath)."
    echo "  · This is NOT R25: no run-all-guards.sh, no per-domain gradle task, no ax-template lint."
    echo "  · Harness-injected, not consumer-shape: the Gradle wrapper (copied gradlew +"
    echo "    gradle-wrapper.jar, GENERATED gradle-wrapper.properties pinned to 9.5.1) and"
    echo "    GRADLE_OPTS=-Dorg.gradle.daemon=false."
    echo "  · kind=file-fragment placement is MODELED by two documented merge rules"
    echo "    (package.json -> JSON deep-merge; gradle dependency-notation -> into dependencies{},"
    echo "    otherwise appended), because a marker names the file, not the position in it."
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
        -h|--help) sed -n '2,101p' "$0"; exit 0 ;;
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

problems = ax_markers.lint(artifacts)
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


_DEP_PREFIXES = ("testImplementation(", "implementation(", "api(", "compileOnly(",
                 "runtimeOnly(", "testRuntimeOnly(", "annotationProcessor(")


def apply_fragment(target, text, aid):
    """Two documented merge rules — see this script's HONEST SCOPE. A marker names the FILE a
    fragment belongs to, not the position inside it, so this placement is a model of the human
    step, not a measurement of it."""
    if os.path.basename(target) == "package.json":
        with open(target, encoding="utf-8") as f:
            base = json.load(f)
        stripped = "\n".join(ln for ln in text.split("\n")
                             if not ln.strip().startswith("//"))
        try:
            incoming = json.loads(stripped)
        except ValueError as exc:
            die("fragment %r is not JSON-mergeable into %s: %s" % (aid, target, exc))
        merge_json(base, incoming)
        with open(target, "w", encoding="utf-8") as f:
            json.dump(base, f, indent=2)
            f.write("\n")
        return "json-merge"

    with open(target, encoding="utf-8") as f:
        existing = f.read()
    meaningful = [ln.strip() for ln in text.split("\n")
                  if ln.strip() and not ln.strip().startswith("//")]
    is_dep_only = bool(meaningful) and all(
        ln.startswith(_DEP_PREFIXES) for ln in meaningful)
    if target.endswith(".gradle.kts") and is_dep_only:
        lines = existing.split("\n")
        for i, ln in enumerate(lines):
            if ln.strip().startswith("dependencies") and ln.rstrip().endswith("{"):
                inject = ["\t" + m for m in meaningful]
                lines[i + 1:i + 1] = inject
                with open(target, "w", encoding="utf-8") as f:
                    f.write("\n".join(lines))
                return "into-dependencies-block"
        die("fragment %r is dependency-notation but %s has no `dependencies {` block to put it in"
            % (aid, target))
    with open(target, "w", encoding="utf-8") as f:
        f.write(existing.rstrip("\n") + "\n\n" + text.rstrip("\n") + "\n")
    return "appended"


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
    if a.kind == "file":
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8") as f:
            f.write(rendered.rstrip("\n") + "\n")
        how = "file"
    else:
        if not os.path.isfile(target):
            die("fragment %r targets %s which does not exist in the consumer project"
                % (a.id, target))
        how = apply_fragment(target, rendered, a.id)
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

echo "── assertions ────────────────────────────────────────────"

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
