#!/usr/bin/env bash
# practices/evals/downstream_release_recency_guard.sh — guard [114], release gate.
#
# Closes GH #92 step 5: a push that bumps .claude-plugin/plugin.json's `version` must not ship
# unless the downstream fixture harness (practices/scripts/verify-downstream.sh, Layer 1) actually
# ran GREEN against the exact commit being pushed. This guard does not run the harness itself
# (that would make pre-push slow and network-dependent, and duplicate Layer 1's job) — it only
# AUDITS the log Layer 1 writes to `.ax-downstream/runs.jsonl`, offline, reading only what is
# already on disk. Same division of labor as the 49th guard
# (completion_checklist_recency_guard.sh) audits `.ax-verify/runs.jsonl` written by
# verify-completion.sh, without re-running R25.
#
# PLACEMENT: this guard runs from .githooks/pre-push (wired by the orchestrator), NOT from
# run-all-guards.sh / R25. Placing a version-bump gate inside R25 is a bootstrap deadlock: the
# very commit that bumps the version could never carry a post-bump audit log entry, because R25
# runs BEFORE that commit exists. Precedent: completion_checklist_recency_guard.sh is the 49th
# guard because of exactly this reasoning, and it too lives at pre-push, not inside R25. Order:
# commit -> verify-downstream.sh (writes .ax-downstream/runs.jsonl) -> R25 -> push (THIS guard).
#
# TRIGGER CONDITION: fires only when `.claude-plugin/plugin.json`'s `version` VALUE differs
# between the push range's base and the sha being pushed (a value comparison, not a file-touched
# comparison — the file can change without the version changing, e.g. a schema-shape edit, and
# that must NOT fire this gate).
#
# WHAT "PASS" REQUIRES — the LATEST line of `.ax-downstream/runs.jsonl` must satisfy ALL of:
#   (i)   head_sha            == the sha being pushed
#   (ii)  tree_clean           is boolean true
#   (iii) assertions           is a non-empty object whose values are ALL boolean true AND whose
#         KEY SET IS EXACTLY THE HARNESS'S DECLARED ASSERTION MANIFEST — missing key and extra key
#         both BLOCK. "every value that happens to be present is true" is NOT enough and was a real
#         hole: `{"forged-single": true}` satisfied it, so a single hand-typed line passed the
#         release gate while claiming nothing. The manifest is NOT re-listed here (a second source
#         of truth would drift); it is PARSED from the harness itself — the one
#         `# ax:assertions <id> <id> …` line in practices/scripts/verify-downstream.sh, read AT THE
#         PUSHED SHA. Add or remove a note() call there and this gate follows automatically, and
#         the harness cross-checks that line against the ids it actually records at run time
#         (exit 8 on drift), so the declaration cannot rot into a lie either.
#   (iiib) verdict             == "pass", and override == [] (an empty list, and the key must be
#         present). An --artifact-override run installs a body that is NOT what the SKILL.md
#         carries — it is a regression differential by construction and can never be release
#         evidence, no matter how green its assertions look.
#   (iv)  artifact_digests      is an object mapping each ax:artifact id (see
#         practices/scripts/lib/ax_markers.py) to the sha256 of that artifact's exact body text,
#         and this guard RECOMPUTES that same map from the SKILL.md files as they exist AT THE
#         PUSHED SHA and requires an EXACT key-for-key match (missing key, extra key, or any value
#         mismatch all BLOCK). This is the one check that is genuinely hard to forge here: a green
#         audit line can be hand-typed, but the digest comparison is against the marker bodies the
#         push actually carries, recomputed fresh every time — editing a SKILL.md after
#         verify-downstream.sh ran (even if the JSONL line is left untouched, or hand-edited to
#         match the OLD content) is caught because the recompute reads the NEW content.
#
# HONEST STRENGTH DECLARATION (do not overclaim — this was a Critic-flagged risk in the design
# discussion for this guard, referenced as "D13"):
#   completion_checklist_recency_guard.sh is ~2,500 lines and ~14 forgery-resistance checks: a
#   privileged-mode re-exec that neutralizes BASH_ENV/exported-function hijacking, a full
#   GIT_*/PYTHON* environment scrub, tool identity smoke-tests, mktemp trust verification, git
#   `--no-replace-objects`, byte-for-byte tree fingerprinting sampled at every step boundary, a
#   remote-ref anchor binding, and a duplicate-key-rejecting JSON parser — and its own header
#   states plainly that even all of that does not make forgery IMPOSSIBLE, only that it closes the
#   cheap, observed shape (a hand-appended JSONL line).
#   THIS GUARD DOES NOT REIMPLEMENT THAT MACHINERY. It has exactly one check with comparable
#   teeth — the digest recompute in (iv) above — and otherwise trusts what it reads: it does not
#   re-exec under bash privileged mode, does not scrub the environment, does not authenticate the
#   `git`/`python3` binaries it calls, does not verify mktemp's returned directory, and does not
#   sample the tree across the run's lifetime (it takes `tree_clean`/`assertions` at face value —
#   an operator with shell access on the pushing machine COULD hand-write a runs.jsonl line that
#   passes (i)-(iii) outright, and could pass (iv) too if they also hand-copy the CURRENT SKILL.md
#   bytes' real digests into that forged line). What (iv) closes is specifically the case the PRD
#   calls out: a green run whose SKILL.md is edited AFTERWARD without re-running Layer 1 — the
#   most likely accidental failure mode, not a deliberately hostile one. This is NOT "a second
#   instance of a proven mechanism" — it is a smaller, weaker gate that borrows one idea from the
#   precedent and is honest about the rest of the gap. Closing that gap further is backlog, not
#   silently claimed here.
#
# FIXTURE / --root DESIGN: fixture trees under
# practices/evals/fixtures/downstream_release_recency/ are NOT git repositories (no .git/) so that
# they can be committed as plain tracked files, mirroring completion_checklist_recency's
# `.ax-verify/`-based fixtures. This guard detects git-rootedness itself:
#   - LIVE mode (SCAN_ROOT/.git resolves): version-before/after and the SKILL.md content used for
#     digest recompute are all read via `git show <sha>:<path>` — never the working tree — so a
#     dirty local edit after committing cannot influence the verdict.
#   - FIXTURE-SHAPED mode (no .git): version-after comes from <root>/.claude-plugin/plugin.json,
#     version-before from <root>/.ax-downstream/prev_version.txt, the sha to match against
#     runs.jsonl's head_sha from <root>/.ax-downstream/expected_head.txt, the declared assertion
#     manifest from <root>/.ax-downstream/expected_assertions.txt (whitespace-separated ids,
#     #-comments ignored — the stand-in for parsing the harness, which a fixture tree does not
#     carry), and the SKILL.md content
#     for digest recompute is read directly from <root>/skills/*/SKILL.md on disk. This mode is
#     used both by `--fixtures` (which drives it once per pass_*/fail_* subdirectory) and by any
#     manual `--root DIR` invocation against a non-git directory.
#
# OPT-OUT: AX_SKIP_DOWNSTREAM_RELEASE_GATE=1 skips the gate outright (loud, printed to stderr).
#   Honored ONLY for a genuine live invocation (no --root given) — a `--root` invocation is by
#   construction a controlled/test call (fixtures or a manual audit of another tree) and must not
#   be silently defeated by whatever the ambient shell happens to export, or `--fixtures` itself
#   would spuriously pass every fixture in an environment that sets the opt-out.
#   The opt-out exists for the one case this guard refuses to guess through: base commit
#   resolution failure (root commit / shallow clone / a fork-receiver's first push to a new
#   remote branch). That failure is reported LOUDLY on stderr, with the opt-out's exact spelling
#   printed inline, on a UNIQUE exit code (3) that no fixture exercises (fixtures never resolve a
#   base this way — see FIXTURE-SHAPED mode above), because a silent skip there would be exactly
#   the "unmeasured pass" this whole guard exists to prevent.
#
# Exit codes: 0 pass (gate satisfied OR did not fire OR explicitly skipped) · 1 violation (used
# by every required fail_* fixture — practices/evals/fixture_kill_proof_guard.sh [87] only
# registers exit-1 fail fixtures) · 2 usage error · 3 base commit unresolved (live only).
#
# Usage:
#   bash practices/evals/downstream_release_recency_guard.sh
#       live repo, resolves head=HEAD and base=merge-base(HEAD, origin/HEAD or origin/main)
#   bash practices/evals/downstream_release_recency_guard.sh --head SHA --base SHA
#       live repo, caller-supplied range (this is how .githooks/pre-push wires it, using the
#       local_sha/base it already resolved via pp_resolve_ref_base — see that function's header)
#   bash practices/evals/downstream_release_recency_guard.sh --root DIR
#       audit DIR instead of this repo; DIR may or may not be a git repository (see above)
#   bash practices/evals/downstream_release_recency_guard.sh --fixtures
#       run every pass_*/fail_* fixture under fixtures/downstream_release_recency/

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""
HEAD_ARG=""
BASE_ARG=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --head) HEAD_ARG="$2"; shift 2 ;;
        --head=*) HEAD_ARG="${1#--head=}"; shift ;;
        --base) BASE_ARG="$2"; shift 2 ;;
        --base=*) BASE_ARG="${1#--base=}"; shift ;;
        *) echo "downstream_release_recency_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/downstream_release_recency"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "downstream_release_recency_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [downstream_release_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [downstream_release_recency/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        rc=0
        bash "$0" --root "$sub" >/dev/null 2>&1 || rc=$?
        if [ "$rc" -eq 1 ]; then
            echo "PASS [downstream_release_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [downstream_release_recency/$(basename "$sub")] — expected exit 1 on FAIL fixture, got $rc"
            fail=$((fail + 1))
        fi
    done

    echo ""
    echo "downstream_release_recency_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live / --root mode ────────────────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "downstream_release_recency_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi
SCAN_ROOT="$(cd "$SCAN_ROOT" && pwd)"

IS_LIVE=1
[ -n "$ROOT_OVERRIDE" ] && IS_LIVE=0

# Opt-out: live invocations only (see header — a --root call must not be defeatable by ambient
# env, or `--fixtures` would spuriously pass in any environment that happens to export this).
if [ "$IS_LIVE" -eq 1 ] && [ "${AX_SKIP_DOWNSTREAM_RELEASE_GATE:-}" = "1" ]; then
    echo "downstream_release_recency_guard: gate explicitly skipped via AX_SKIP_DOWNSTREAM_RELEASE_GATE=1" >&2
    exit 0
fi

# RELOCATED-COPY AFFORDANCE (mirrors AX_RELEASE_ANCHOR_LIB — see
# evidence_quote_spotcheck_guard.sh / manifest_snapshot_integrity_guard.sh, and
# install_artifact_extractability_guard.sh's identical AX_MARKERS_LIB_DIR): fixture_kill_proof_
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
    echo "downstream_release_recency_guard: cannot find practices/scripts/lib/ax_markers.py under $REPO_ROOT" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" "$HEAD_ARG" "$BASE_ARG" "$AX_MARKERS_DIR" <<'PYEOF'
import sys
import os
import json
import hashlib
import re
import subprocess
import tempfile
import shutil

root, head_arg, base_arg, ax_markers_dir = sys.argv[1:5]
sys.path.insert(0, ax_markers_dir)
import ax_markers  # noqa: E402


def fail(code, *lines):
    print(f"downstream_release_recency_guard: {code}", file=sys.stderr)
    for ln in lines:
        print(f"  {ln}", file=sys.stderr)
    sys.exit(1)


def git(*args, cwd):
    p = subprocess.run(["git"] + list(args), cwd=cwd, stdout=subprocess.PIPE,
                        stderr=subprocess.DEVNULL)
    return p.returncode, p.stdout.decode(errors="replace").strip()


def digest_map_from_paths(paths):
    """id -> sha256(body) for every ax:artifact marker discovered across `paths`."""
    artifacts = ax_markers.discover(paths)
    out = {}
    for a in artifacts:
        if not a.id:
            continue
        out[a.id] = hashlib.sha256(a.body.encode("utf-8")).hexdigest()
    return out


def read_json_file(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


HARNESS_REL = "practices/scripts/verify-downstream.sh"
MANIFEST_RE = re.compile(r"^#\s*ax:assertions\s+(\S.*)$")
MANIFEST_SOURCE = "(unresolved)"


def declared_assertion_ids():
    """The COMPLETE set of assertion ids a full harness run must record.

    Derived, never duplicated: in live mode from the harness's single `# ax:assertions` line AS IT
    EXISTS AT THE PUSHED SHA (so the gate follows the harness automatically, and a hand-edited
    working copy cannot widen or narrow it); in fixture-shaped mode from the fixture's own
    .ax-downstream/expected_assertions.txt, because a fixture tree carries no harness. An
    unresolvable manifest is a BLOCK, never a skipped check.
    """
    global MANIFEST_SOURCE
    if is_git:
        MANIFEST_SOURCE = f"{HARNESS_REL} @ {expected_head}"
        rc, content = git("show", f"{head_sha}:{HARNESS_REL}", cwd=root)
        if rc != 0 or not content:
            fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
                 f"could not read {HARNESS_REL} at {head_sha!r}. The gate cannot know which "
                 "assertions a complete run must record, so it refuses to accept the log.")
        decls = [m.group(1) for m in
                 (MANIFEST_RE.match(ln) for ln in content.splitlines()) if m]
    else:
        MANIFEST_SOURCE = ".ax-downstream/expected_assertions.txt"
        path = os.path.join(root, ".ax-downstream", "expected_assertions.txt")
        if not os.path.isfile(path):
            fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
                 f"{path} does not exist. In fixture-shaped mode this file declares the complete "
                 "assertion set; without it the completeness check cannot run.")
        with open(path, encoding="utf-8") as f:
            body = "\n".join(ln for ln in f.read().splitlines()
                             if ln.strip() and not ln.strip().startswith("#"))
        decls = [body] if body.strip() else []

    if len(decls) != 1:
        fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
             f"expected exactly ONE assertion-manifest declaration in {MANIFEST_SOURCE}, found "
             f"{len(decls)}.")
    ids = set(decls[0].split())
    if not ids:
        fail("AX_DOWNSTREAM_MANIFEST_UNRESOLVED",
             f"the assertion manifest in {MANIFEST_SOURCE} declares zero ids — an empty manifest "
             "would make the completeness check vacuous.")
    return ids


# ── Detect git-rootedness ────────────────────────────────────────────────────
# `git -C <root> rev-parse --git-dir` WALKS UP when <root> is not itself a work tree — every
# fixture under practices/evals/fixtures/ lives INSIDE this repository, so that check alone would
# always succeed and treat every fixture as "live git mode" against ax-template's own history.
# The real question is whether ROOT ITSELF is the toplevel of a git work tree.
rc, toplevel = git("rev-parse", "--show-toplevel", cwd=root)
is_git = False
if rc == 0 and toplevel:
    try:
        is_git = os.path.samefile(toplevel, root)
    except OSError:
        is_git = False

tmpdir = None
try:
    if is_git:
        head_sha = head_arg
        if not head_sha:
            rc, out = git("rev-parse", "HEAD", cwd=root)
            head_sha = out if rc == 0 else ""
        if not head_sha:
            print("downstream_release_recency_guard: AX_DOWNSTREAM_HEAD_UNRESOLVED — could not "
                  "resolve a head sha for this repository.", file=sys.stderr)
            sys.exit(2)

        base_sha = base_arg
        if not base_sha:
            rc, out = git("merge-base", head_sha, "origin/HEAD", cwd=root)
            base_sha = out if rc == 0 else ""
        if not base_sha:
            rc, out = git("merge-base", head_sha, "origin/main", cwd=root)
            base_sha = out if rc == 0 else ""
        if not base_sha:
            print("downstream_release_recency_guard: AX_DOWNSTREAM_BASE_UNRESOLVED — could not "
                  "resolve a base commit for this push range (root commit / shallow clone / "
                  "first push to a new remote branch are the known causes). This gate cannot "
                  "tell whether .claude-plugin/plugin.json's version changed in the range, so "
                  "it refuses to guess.", file=sys.stderr)
            print("  Opt out explicitly if this is a legitimate case: "
                  "AX_SKIP_DOWNSTREAM_RELEASE_GATE=1", file=sys.stderr)
            sys.exit(3)

        rc, before_raw = git("show", f"{base_sha}:.claude-plugin/plugin.json", cwd=root)
        version_before = None
        if rc == 0:
            try:
                version_before = json.loads(before_raw).get("version")
            except ValueError:
                version_before = None
        rc, after_raw = git("show", f"{head_sha}:.claude-plugin/plugin.json", cwd=root)
        version_after = None
        if rc == 0:
            try:
                version_after = json.loads(after_raw).get("version")
            except ValueError:
                version_after = None

        if version_before == version_after:
            print("downstream_release_recency_guard: plugin.json version unchanged in this "
                  "push range — gate does not fire.")
            sys.exit(0)

        # Gate fires. Extract every skills/*/SKILL.md AT head_sha (never the working tree).
        rc, listing = git("ls-tree", "-r", "--name-only", head_sha, "--", "skills", cwd=root)
        skill_paths_in_tree = [ln for ln in listing.splitlines() if ln.endswith("/SKILL.md")]
        tmpdir = tempfile.mkdtemp(prefix="ax-downstream-recency-")
        extracted = []
        for idx, rel in enumerate(skill_paths_in_tree):
            rc, content = git("show", f"{head_sha}:{rel}", cwd=root)
            if rc != 0:
                continue
            dest = os.path.join(tmpdir, f"skill_{idx}.md")
            with open(dest, "w", encoding="utf-8") as f:
                f.write(content)
            extracted.append(dest)
        recomputed = digest_map_from_paths(extracted)

        log_path = os.path.join(root, ".ax-downstream", "runs.jsonl")
        expected_head = head_sha
    else:
        # FIXTURE-SHAPED mode — no git dependency at all.
        plugin_json = read_json_file(os.path.join(root, ".claude-plugin", "plugin.json"))
        version_after = plugin_json.get("version") if isinstance(plugin_json, dict) else None

        prev_version_file = os.path.join(root, ".ax-downstream", "prev_version.txt")
        version_before = None
        if os.path.isfile(prev_version_file):
            with open(prev_version_file, encoding="utf-8") as f:
                version_before = f.read().strip()

        if version_before == version_after:
            print("downstream_release_recency_guard: plugin.json version unchanged in this "
                  "push range — gate does not fire.")
            sys.exit(0)

        expected_head_file = os.path.join(root, ".ax-downstream", "expected_head.txt")
        expected_head = ""
        if os.path.isfile(expected_head_file):
            with open(expected_head_file, encoding="utf-8") as f:
                expected_head = f.read().strip()

        import glob
        skill_paths = sorted(glob.glob(os.path.join(root, "skills", "*", "SKILL.md")))
        recomputed = digest_map_from_paths(skill_paths)

        log_path = os.path.join(root, ".ax-downstream", "runs.jsonl")

    # ── Common validation once the gate has fired ────────────────────────────
    if not os.path.isfile(log_path):
        fail("AX_DOWNSTREAM_LOG_MISSING",
             f"{log_path} does not exist. A version bump requires a fresh "
             "practices/scripts/verify-downstream.sh run's audit log for the pushed sha.")

    with open(log_path, encoding="utf-8") as f:
        lines = [ln for ln in f.read().splitlines() if ln.strip()]
    if not lines:
        fail("AX_DOWNSTREAM_LOG_MISSING", f"{log_path} exists but is empty.")

    def _reject_dup_keys(pairs):
        seen = set()
        out = {}
        for k, v in pairs:
            if k in seen:
                raise ValueError(f"duplicate key {k!r} in audit log line")
            seen.add(k)
            out[k] = v
        return out

    try:
        latest = json.loads(lines[-1], object_pairs_hook=_reject_dup_keys)
    except ValueError as exc:
        fail("AX_DOWNSTREAM_LOG_UNPARSEABLE", f"{log_path}'s latest line is not valid JSON: {exc}")

    if not isinstance(latest, dict):
        fail("AX_DOWNSTREAM_LOG_UNPARSEABLE", f"{log_path}'s latest line is not a JSON object.")

    if latest.get("head_sha") != expected_head:
        fail("AX_DOWNSTREAM_LOG_STALE_HEAD",
             f"latest audit line's head_sha={latest.get('head_sha')!r} does not match the sha "
             f"being pushed ({expected_head!r}). Re-run verify-downstream.sh against the "
             "committed tree of the sha you are pushing.")

    if latest.get("tree_clean") is not True:
        fail("AX_DOWNSTREAM_LOG_DIRTY_TREE",
             f"latest audit line's tree_clean={latest.get('tree_clean')!r}, not boolean true. "
             "verify-downstream.sh must have run against a clean, fully-committed tree.")

    assertions = latest.get("assertions")
    if not isinstance(assertions, dict) or not assertions:
        fail("AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS",
             "latest audit line's 'assertions' is missing, not an object, or empty. A single "
             "summary flag is not accepted — every behavioral assertion must be individually "
             "recorded as boolean true.")
    failing = sorted(k for k, v in assertions.items() if v is not True)
    if failing:
        fail("AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS",
             f"the following assertion(s) are not boolean true: {failing}")

    # ── COMPLETENESS: the recorded key set must be EXACTLY the harness's declared manifest ──
    # Derived, never re-listed here (see header (iii)): parsed from the harness at the pushed sha
    # in live mode, from the fixture's expected_assertions.txt in fixture-shaped mode. Without
    # this, `{"forged-single": true}` passes every check above.
    declared = declared_assertion_ids()
    recorded_keys = set(assertions)
    missing_assertions = sorted(declared - recorded_keys)
    extra_assertions = sorted(recorded_keys - declared)
    if missing_assertions or extra_assertions:
        fail("AX_DOWNSTREAM_ASSERTION_SET_MISMATCH",
             f"the audit log's assertion key set is not the harness's declared set "
             f"({MANIFEST_SOURCE}). not_recorded={missing_assertions} "
             f"undeclared_extra={extra_assertions}",
             "A green line that records only some (or none) of the harness's assertions is not "
             "evidence that the harness ran — it is evidence that SOMETHING wrote a line.")

    if latest.get("verdict") != "pass":
        fail("AX_DOWNSTREAM_LOG_NOT_PASS",
             f"latest audit line's verdict={latest.get('verdict')!r}, not 'pass'. Only a run the "
             "harness itself declared passing can back a release.")

    override = latest.get("override")
    if override != []:
        fail("AX_DOWNSTREAM_LOG_OVERRIDE_PRESENT",
             f"latest audit line's override={override!r}; a release requires an empty list. An "
             "--artifact-override run installs a body the SKILL.md does not carry, so it is a "
             "regression differential by construction, never release evidence. A MISSING "
             "'override' key fails here too: it means the line was not written by the current "
             "harness schema.")

    logged_digests = latest.get("artifact_digests")
    if not isinstance(logged_digests, dict):
        fail("AX_DOWNSTREAM_DIGEST_MISMATCH",
             "latest audit line's 'artifact_digests' is missing or not an object.")

    recomputed_keys = set(recomputed)
    logged_keys = set(logged_digests)
    missing = sorted(recomputed_keys - logged_keys)
    extra = sorted(logged_keys - recomputed_keys)
    mismatched = sorted(
        k for k in (recomputed_keys & logged_keys) if recomputed[k] != logged_digests[k]
    )
    if missing or extra or mismatched:
        fail("AX_DOWNSTREAM_DIGEST_MISMATCH",
             f"recomputed artifact digests (from the SKILL.md files at {expected_head!r}) do not "
             f"match the audit log's artifact_digests. missing_in_log={missing} "
             f"extra_in_log={extra} value_mismatch={mismatched}")

    print("downstream_release_recency_guard: PASS — audit log matches the pushed sha, tree was "
          "clean, all assertions true, artifact digests match.")
    sys.exit(0)
finally:
    if tmpdir is not None:
        shutil.rmtree(tmpdir, ignore_errors=True)
PYEOF
exit $?
