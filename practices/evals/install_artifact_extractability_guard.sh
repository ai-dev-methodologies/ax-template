#!/usr/bin/env bash
# practices/evals/install_artifact_extractability_guard.sh — guard [112].
#
# THE INVARIANT
#   The downstream verification harness (practices/scripts/verify-downstream.sh) EXTRACTS what
#   it installs into a consumer project from the `<!-- ax:artifact ... -->` markers embedded in
#   the three install skills (skills/ax-install-{hooks,java-enforcement,react-enforcement}/
#   SKILL.md). If a marker is malformed — the fence it names has gone missing, its id collides
#   with another, its substitution declarations drifted from what the body actually uses, its
#   directive comment prefix does not match its fence language — the harness does not fail
#   loudly. It either extracts nothing for that artifact, or extracts something silently wrong,
#   and reports whatever it DID manage to install as a green run. The silence is the danger: a
#   missing/broken marker looks, from the harness's own log, exactly like "there was nothing to
#   install here." This guard closes that silence at the SOURCE — the skill files themselves —
#   before the harness ever runs, so a bad marker is a commit-time BLOCK, not a downstream
#   surprise nobody notices because nothing measured it.
#
# WHY THIS FORM (checks 1-8 delegated, checks 9-10 owned here)
#   practices/scripts/lib/ax_markers.py is THE single parser for this marker syntax — both this
#   guard and verify-downstream.sh consume it, so an extractor bug is a bug in ONE place instead
#   of two independently-maintained regex sweeps silently diverging (see that file's own header
#   for the fuller rationale). This guard does NOT re-implement any of ax_markers.py's structural
#   checks; it calls discover() + lint() and reports exactly the Problems those return —
#   marker<->fence 1:1, id uniqueness, fence language registration, ax:if/ax:endif balance,
#   directive-prefix-matches-fence-language, substs<->body/path bidirectional agreement, and
#   free-text conditional prose surviving outside a directive line. Two things sit OUTSIDE that
#   parser's job on purpose, because they are not "is this marker syntax well-formed" questions:
#     (9)  COVERAGE — does every skills/ax-install-*/SKILL.md actually carry at least one marker
#          at all? A skill with zero markers is not a syntax error the parser would ever see (it
#          has nothing to parse) — it is a coverage gap, and the set of skill files to check is
#          DERIVED FROM DISK (glob skills/ax-install-*) every run, never a hardcoded/manifest
#          list, so a fourth install skill added later is covered automatically and a skill
#          directory that quietly lost all its markers cannot go unnoticed. (Precedent for why a
#          second, separate list is refused: P2-91 rejected an earlier #88 marker-coverage guard
#          for exactly this reason — a manifest is a second truth that rots independently of the
#          disk it is supposed to describe.)
#     (10) SHELL RENDERABILITY — for every artifact whose fence language is `bash` or `sh`
#          specifically (never js/ts/kotlin/java — parsing those would pull a node/gradle
#          toolchain into an otherwise offline R25 guard, which the R25 toolchain-prerequisites
#          posture in CLAUDE.md does not allow for a guard step), this guard RENDERS the
#          artifact via ax_markers.render() — using the real consumer-e2e fixture's
#          ax.config.json (or the fixture-under-test's own ax.config.json, if it supplies one)
#          for `ax:if`/config-`ax:subst` resolution, plus a dummy value for every declared
#          `env.*` token — and then runs `bash -n` on the rendered text. A marker can be
#          perfectly well-formed by every one of ax_markers.py's structural rules and still
#          render body text that is not valid shell (an unterminated string, an unmatched `if`)
#          — that is a DIFFERENT failure mode than the eight the parser checks, so it gets its
#          own check rather than being folded into lint()'s vocabulary.
#
# WHAT THIS GUARD DELIBERATELY DOES NOT DO
#   No fix-shape grep. This guard does not assert that a specific artifact contains a specific
#   string a past bugfix introduced (e.g. "does java-gradle-testpractices mention
#   -PaxRootPackage") — a check like that only re-states, tautologically, that the fix is still
#   present in the text; it proves nothing about BEHAVIOR, and a marker could keep that exact
#   string while still being unextractable in every way this guard actually checks. Proving the
#   installed artifact BEHAVES correctly in a consumer project is verify-downstream.sh's job, not
#   this guard's — precedent: an earlier #88 marker-coverage guard was rejected by P2-91 on this
#   same reasoning (a second list/assertion that merely echoes what the source already says is
#   not a real gate). This guard only proves the marker tree is STRUCTURALLY EXTRACTABLE; it
#   proves nothing about what happens after extraction.
#
# Exit: 0 PASS (every skills/ax-install-*/SKILL.md marker is well-formed, every skill has >=1
#         marker, every bash/sh artifact renders to syntactically valid shell)
#       1 one or more violations found (checks 1-10) — BLOCK
#       2 usage/setup error (bad flag, root not found, ax_markers.py missing, python3/bash
#         missing, render config not found)
#
# Usage:
#   bash practices/evals/install_artifact_extractability_guard.sh
#       live repo: scans skills/ax-install-*/SKILL.md under this checkout.
#   bash practices/evals/install_artifact_extractability_guard.sh --root DIR
#       scans DIR instead (fixture trees use this — DIR/skills/ax-install-*/SKILL.md).
#   bash practices/evals/install_artifact_extractability_guard.sh --fixtures
#       runs every pass_*/fail_* fixture under fixtures/install_artifact_extractability/ and
#       reports a PASS/FAIL tally (self-verification convenience; not required by callers).
#
# CONFIG RESOLUTION for check (10)'s render pass: DIR/ax.config.json if the scanned root supplies
# one, else practices/evals/fixtures/consumer-e2e/project/ax.config.json (the real, already-
# committed fixture config for the downstream consumer-e2e harness) — never a config invented by
# this guard. This mirrors the contract's instruction to reuse the live fixture config rather
# than fabricate a parallel one.
#
# NOT YET registered in practices/evals/run-all-guards.sh — that registration, and the matching
# fixture_kill_manifest.yaml entry (guard [87] only registers exit-1 fail fixtures), are owned by
# the orchestrating session, not this lane.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --fixtures) FIXTURES_MODE=1; shift ;;
        *) echo "install_artifact_extractability_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/install_artifact_extractability"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "install_artifact_extractability_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [install_artifact_extractability/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [install_artifact_extractability/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        rc=0
        bash "$0" --root "$sub" >/dev/null 2>&1 || rc=$?
        if [ "$rc" -eq 1 ]; then
            echo "PASS [install_artifact_extractability/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [install_artifact_extractability/$(basename "$sub")] — expected exit 1 on FAIL fixture, got $rc"
            fail=$((fail + 1))
        fi
    done

    echo ""
    echo "install_artifact_extractability_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live / --root mode ────────────────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "install_artifact_extractability_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi
SCAN_ROOT="$(cd "$SCAN_ROOT" && pwd)"

# RELOCATED-COPY AFFORDANCE (mirrors AX_RELEASE_ANCHOR_LIB — see
# evidence_quote_spotcheck_guard.sh / manifest_snapshot_integrity_guard.sh): fixture_kill_proof_
# guard.sh [87] proves fixture non-vacuity by running a MUTATED COPY of this file from a bare temp
# path, where the repo-relative ax_markers.py does not exist. AX_MARKERS_LIB_DIR names it for
# THAT case only, and the gate is explicit: the override is consulted ONLY when the committed path
# is absent AND this root is not a git work tree — i.e. exactly the relocated sandbox. On any live
# tree a missing ax_markers.py is a BLOCK, never an invitation to load the module from elsewhere.
AX_MARKERS_DIR="$REPO_ROOT/practices/scripts/lib"
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ] \
   && ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    AX_MARKERS_DIR="${AX_MARKERS_LIB_DIR:-$AX_MARKERS_DIR}"
fi
if [ ! -f "$AX_MARKERS_DIR/ax_markers.py" ]; then
    echo "install_artifact_extractability_guard: cannot find practices/scripts/lib/ax_markers.py under $REPO_ROOT" >&2
    exit 2
fi

command -v python3 >/dev/null 2>&1 || {
    echo "install_artifact_extractability_guard: python3 required" >&2; exit 2; }
command -v bash >/dev/null 2>&1 || {
    echo "install_artifact_extractability_guard: bash required (for -n syntax checks)" >&2; exit 2; }

DEFAULT_CONFIG="$REPO_ROOT/practices/evals/fixtures/consumer-e2e/project/ax.config.json"
# Same relocated-copy affordance as AX_MARKERS_LIB_DIR above — a --root fixture that supplies its
# own ax.config.json never reaches this fallback at all, so this only matters for a bare-temp
# mutated copy scanning a fixture with no ax.config.json of its own.
if [ ! -f "$DEFAULT_CONFIG" ] \
   && ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    DEFAULT_CONFIG="${AX_CONSUMER_E2E_CONFIG:-$DEFAULT_CONFIG}"
fi
if [ -f "$SCAN_ROOT/ax.config.json" ]; then
    RENDER_CONFIG="$SCAN_ROOT/ax.config.json"
else
    RENDER_CONFIG="$DEFAULT_CONFIG"
fi
if [ ! -f "$RENDER_CONFIG" ]; then
    echo "install_artifact_extractability_guard: render config not found: $RENDER_CONFIG" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" "$AX_MARKERS_DIR" "$RENDER_CONFIG" <<'PYEOF'
import sys, os, glob, json, tempfile, subprocess

root, ax_markers_dir, config_path = sys.argv[1:4]
sys.path.insert(0, ax_markers_dir)
import ax_markers  # noqa: E402

GUARD = "install_artifact_extractability_guard"


def relpath(p):
    try:
        return os.path.relpath(p, root)
    except ValueError:
        return p


# ── check 9 setup: enumerate ax-install-* dirs FROM DISK, never a hardcoded/manifest list ──
install_dirs = sorted(
    d for d in glob.glob(os.path.join(root, "skills", "ax-install-*"))
    if os.path.isdir(d)
)
if not install_dirs:
    print(f"{GUARD}: no skills/ax-install-* directories found under {root} -- nothing to check")
    sys.exit(0)

problems = []  # (code, source_file, line, message)

skill_paths = []
for d in install_dirs:
    smd = os.path.join(d, "SKILL.md")
    if os.path.isfile(smd):
        skill_paths.append(smd)
    else:
        problems.append(("NO_SKILL_MD", d, 0, "ax-install-* directory has no SKILL.md"))

# ── checks 1-8: delegated verbatim to ax_markers.lint() — no re-implementation here ──
artifacts = ax_markers.discover(skill_paths)
for p in ax_markers.lint(artifacts):
    problems.append((p.code, p.source_file, p.line, p.message))

# ── check 9: every discovered SKILL.md must own at least one marker ──
covered_files = {a.source_file for a in artifacts}
for smd in skill_paths:
    if smd not in covered_files:
        problems.append((
            "NO_ARTIFACT_MARKER", smd, 0,
            "SKILL.md has zero ax:artifact markers -- install skill provides nothing "
            "extractable for verify-downstream.sh to materialize"))

# ── check 10: bash -n on every rendered bash/sh artifact (guard-owned; not lint()'s job) ──
with open(config_path, encoding="utf-8") as f:
    config = json.load(f)

shell_checked = 0
for a in artifacts:
    if a.fence_lang not in ("bash", "sh"):
        continue  # JS/TS/Kotlin/Java are deliberately not parsed here (see header)
    if a.fence_start_line is None:
        continue  # already reported as MARKER_NO_FENCE above -- nothing to render

    env = {}
    for token in a.substs:
        if token.startswith("env."):
            env[token[len("env."):]] = "ax-guard-dummy-value"

    try:
        rendered = ax_markers.render(a, config, env)
    except ax_markers.RenderError as exc:
        problems.append((
            "RENDER_ERROR", a.source_file, a.fence_start_line,
            f"id={a.id!r} failed to render against {relpath(config_path) if os.path.isabs(config_path) else config_path}: {exc}"))
        continue

    shell_checked += 1
    tf_path = None
    try:
        fd, tf_path = tempfile.mkstemp(suffix=".sh")
        with os.fdopen(fd, "w", encoding="utf-8") as tf:
            tf.write(rendered)
        result = subprocess.run(["bash", "-n", tf_path], capture_output=True, text=True)
        if result.returncode != 0:
            problems.append((
                "UNPARSEABLE_SHELL", a.source_file, a.fence_start_line,
                f"id={a.id!r}: rendered body fails `bash -n`: {result.stderr.strip()}"))
    finally:
        if tf_path is not None:
            os.unlink(tf_path)

for code, src, line, msg in problems:
    print(f"{relpath(src)}:{line}: {code}: {msg}")

if problems:
    print("")
    print(f"{GUARD}: {len(problems)} problem(s) across {len(skill_paths)} "
          f"skills/ax-install-*/SKILL.md file(s) -- BLOCKED", file=sys.stderr)
    sys.exit(1)

print(f"{GUARD}: PASS -- {len(skill_paths)} skills/ax-install-*/SKILL.md file(s), "
      f"{len(artifacts)} ax:artifact marker(s), all structurally extractable "
      f"(checks 1-8 via ax_markers.lint(), check 9 coverage, check 10 bash -n on "
      f"{shell_checked} shell artifact(s))")
sys.exit(0)
PYEOF
exit $?
