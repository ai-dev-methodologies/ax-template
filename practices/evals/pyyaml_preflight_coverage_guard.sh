#!/usr/bin/env bash
# practices/evals/pyyaml_preflight_coverage_guard.sh — R25 preflight ⊇ PyYAML-dependent guards [95]
#
# THE INVARIANT (binary, non-vacuous): every checklist step that (transitively) runs a
# guard which embeds `python3 ... import yaml` WITHOUT a yq fallback MUST be blocked by
# verify-completion.sh's toolchain preflight when PyYAML is absent.
#
# WHY: the preflight historically accepted "PyYAML **or** yq" — accurate for parsing the
# checklist itself (that parser really does fall back to `yq -o=json`), but NOT for the
# guards it then runs. ~14 guard scripts embed `import yaml` with no yq path, and several
# of them SILENTLY SKIP (exit 0) when PyYAML is missing:
#     aggregate_boundary_allowlist_guard.sh   → "SKIP — PyYAML not installed", exit 0
#     feature_boundary_allowlist_guard.sh     → "SKIP — PyYAML not installed", exit 0
#     l4_domain_enum_sync_guard.sh            → SKIP unless --strict (run-all-guards omits it)
#     manifest_yaml_strict_parse_guard.sh     → SKIP unless --strict
#     override_schema_guard.sh                → SKIP unless --strict
# So on a yq-only machine the preflight passed, run-all-guards reported PASS, and whole
# guards had never run — a gate reporting OK when it is not. That was not hypothetical:
# this maintainer machine's default python3 (Homebrew 3.14) had no PyYAML while yq was
# installed; only the PATH order of a second interpreter hid it.
#
# WHAT THIS GUARD DOES (mechanical, not a hardcoded list):
#   1. Enumerates every .sh/.py under the repo whose python body imports yaml (in ANY
#      import form) and offers NO yq fallback → the PyYAML-DEPENDENT set. A newly added
#      dependent guard is picked up automatically; nothing here is a hardcoded list.
#   2. Computes, per checklist step, the TRANSITIVE set of scripts that step reaches
#      (commands → referenced scripts → scripts those reference, …). This is deeper than
#      the preflight's own cheap `evals/` path heuristic ON PURPOSE: if a dependency is
#      ever reached through a wrapper that lives outside evals/, the heuristic misses it
#      and this guard FAILS.
#   3. Runs the REAL verify-completion.sh per step with AX_PREFLIGHT_FAKE_MISSING=pyyaml
#      and asserts, behaviorally:
#        • every step reaching ≥1 dependent script BLOCKS (exit 2 + PyYAML message);
#        • every step reaching NO scripts at all does NOT block on PyYAML — proving the
#          gating is step-scoped, not blanket (a fork-receiver's `--step backend-build`
#          must stay runnable without PyYAML, exactly as it is without node);
#        • the FULL step set blocks.
#   4. Runs each dependent guard ITSELF under a PYTHONPATH shim whose yaml.py raises
#      ImportError, and asserts it FAILS CLOSED: exit 2 (the repo's "cannot verify"
#      convention), never 0, never an "all pass"-shaped message, AND with output that names
#      the parser. The preflight only protects the R25 path — a fork-receiver or CI invoking
#      a guard directly, or anything reading a guard's exit code, needs the guarantee to
#      hold at the guard itself.
#      Scripts that cannot be executed safely are listed in PROBE_EXEMPT with a reason,
#      REPORTED in the output, and asserted statically instead — never silently skipped.
#
#      TWO defects were found in THIS probe while wiring the first .py dependent (P2-44),
#      and the second is the reason the message check exists:
#        (a) the interpreter was hardcoded to `bash`, correct only while every dependent was
#            a shell script. Handed a python file, bash dies on syntax with exit 2 — the
#            very code this check reads as proof of failing closed — identically with and
#            without PyYAML. Fixed by probing with the script's own interpreter.
#        (b) (a) alone only MOVED the vacuity. Measured on that same .py dependent: with
#            PyYAML installed it exits 2 from argparse ("the following arguments are
#            required: --map"); without it, exit 2 from the import guard. A code-only check
#            cannot tell those apart. So the output must NAME the parser — that is what
#            makes absence distinguishable from an ordinary usage error.
#   Weakening the preflight, adding a PyYAML-dependent guard behind a non-evals/ wrapper,
#   or letting a guard skip-with-exit-0 again, flips this guard to FAIL.
#
# Deliberate asymmetry (honest): the preflight is allowed to be MORE conservative than the
# dependent set (it requires PyYAML for any evals/ script, including the few guards that do
# not parse yaml). Guards are composed freely, so over-covering there is forward-safe; only
# UNDER-coverage is a defect. This guard therefore asserts need ⊆ blocked, plus a witness
# that non-guard steps stay unblocked.
#
# LIMITS (honest residuals):
#   1. Dependency detection is textual: a python import statement naming yaml, in a script
#      that never invokes/probes yq. Every import FORM is parsed, not just a leading bare
#      `yaml` — `import sys, os, glob, yaml`, `import yaml as y`, `from yaml import …` and
#      `from yaml.x import …` all count. (The earlier prefix-anchored pattern missed the
#      comma-list form and left vacuity_class_proof_guard.sh — a reachable hard guard that
#      DID false-pass — out of the census.) Still unseen: dynamic imports
#      (`__import__("yaml")`, importlib), which no text rule can follow.
#      Comment-only lines are stripped first, so prose about yq cannot exempt a guard —
#      but a script carrying a QUOTED yq literal in live code (this guard itself does: it
#      defines the yq-detection pattern) reads as having a fallback and drops out of the
#      dependent set. That is why the preflight is deliberately broader (any evals/
#      script): the path rule covers what the text rule can miss.
#   2. A guard invoked through an interpreter this scan cannot follow (a compiled binary,
#      a make target) is invisible to reachability, as it is to every text-based guard here.
#
# Usage:
#   bash practices/evals/pyyaml_preflight_coverage_guard.sh                # live repo
#   bash practices/evals/pyyaml_preflight_coverage_guard.sh --repo-root DIR # fixture tree
#   bash practices/evals/pyyaml_preflight_coverage_guard.sh --fixtures      # committed trio
#
# --repo-root runs the fixture's checklist + scripts against the REAL verify-completion.sh
# (copied into a throwaway harness), so the fixtures test the product, not a stub.
#
# Exit: 0 = every PyYAML-dependent step is preflight-covered AND every dependent guard
#           fails closed · 1 = coverage hole or a fail-open guard (BLOCK)
#       2 = setup/tooling error (PyYAML itself missing — never a silent skip).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# This guard needs the REAL verify-completion.sh, so it must find the repo even when the
# script itself is executed from a copy outside the tree — which is exactly what
# fixture_kill_proof_guard.sh [87] does when it runs the neutered mutant from a tmp path.
# Fall back to walking up from the working directory.
if [ ! -f "$REPO_ROOT/practices/scripts/verify-completion.sh" ]; then
    _d="$(pwd -P)"
    while [ "$_d" != "/" ]; do
        if [ -f "$_d/practices/scripts/verify-completion.sh" ]; then REPO_ROOT="$_d"; break; fi
        _d="$(dirname "$_d")"
    done
fi
FIXTURE_DIR="$REPO_ROOT/practices/evals/fixtures/pyyaml-preflight-coverage"

ROOT_OVERRIDE=""
RUN_FIXTURES=0
while [ $# -gt 0 ]; do
    case "$1" in
        --repo-root)   ROOT_OVERRIDE="$2"; shift 2 ;;
        --repo-root=*) ROOT_OVERRIDE="${1#--repo-root=}"; shift ;;
        --fixtures)    RUN_FIXTURES=1; shift ;;
        *) echo "pyyaml_preflight_coverage_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

command -v python3 >/dev/null 2>&1 || {
    echo "pyyaml_preflight_coverage_guard: python3 required" >&2; exit 2; }
python3 -c 'import yaml' >/dev/null 2>&1 || {
    echo "pyyaml_preflight_coverage_guard: BLOCK — PyYAML missing, so this guard cannot verify the" >&2
    echo "  PyYAML preflight. Reporting setup-error rather than a silent skip (that skip IS the bug" >&2
    echo "  this guard exists to prevent). Install with: python3 -m pip install pyyaml" >&2
    exit 2; }

REAL_VC="$REPO_ROOT/practices/scripts/verify-completion.sh"
[ -f "$REAL_VC" ] || {
    echo "pyyaml_preflight_coverage_guard: verify-completion.sh not found at $REAL_VC" >&2; exit 2; }

# Analyse one tree. $1 = root to analyse, $2 = verify-completion.sh to exercise.
analyse() {
    python3 - "$1" "$2" <<'PY'
import os, re, shutil, subprocess, sys, tempfile

root, vc_path = sys.argv[1], sys.argv[2]
checklist_path = os.path.join(root, "practices", "verification-checklist.yaml")
if not os.path.isfile(checklist_path):
    print(f"  checklist not found: {checklist_path}", file=sys.stderr)
    sys.exit(2)

import yaml
doc = yaml.safe_load(open(checklist_path, encoding="utf-8")) or {}
steps = [s for s in (doc.get("checklist") or []) if s.get("id")]
if not steps:
    print("  checklist declares no steps — cannot prove coverage", file=sys.stderr)
    sys.exit(2)

# ── 1. index every script in the tree, by basename ───────────────────────────
PRUNE = {".git", "node_modules", "build", ".gradle", "dist", "out", ".ax-verify",
         ".next", "__pycache__", ".idea", "bin"}
index = {}
for dirpath, dirnames, filenames in os.walk(root):
    dirnames[:] = [d for d in dirnames if d not in PRUNE]
    for fn in filenames:
        if fn.endswith(".sh") or fn.endswith(".py"):
            index.setdefault(fn, []).append(os.path.join(dirpath, fn))

def read(path):
    try:
        return open(path, encoding="utf-8", errors="replace").read()
    except OSError:
        return ""

# A script is PyYAML-DEPENDENT when it imports yaml in an embedded python body and
# offers no yq path. A yq path means yq is actually INVOKED or probed — prose that
# merely mentions yq does not exempt a file (that leniency would let a guard opt out
# of coverage with a comment).
SCRIPT_RE = re.compile(r"[A-Za-z0-9_.-]+\.(?:sh|py)")
YQ_RE = re.compile(r"""command -v yq|\byq\s+(?:-o|e\b|eval\b|'|")|["']yq["']""")

# Import detection must cover EVERY python import form, not just a leading bare `yaml`.
# A pattern anchored on "yaml appears first" (`^\s*import yaml`) silently missed
#     import sys, os, glob, yaml            ← vacuity_class_proof_guard.sh
# leaving a reachable hard guard out of the dependent set — and that guard then
# false-passed under simulated absence. Parse the statement instead of pattern-matching
# its prefix: split an `import a, b as c, yaml` list on commas and test each name, and
# read the module of a `from yaml[.x] import …`.
IMPORT_STMT_RE = re.compile(r"^\s*import\s+(.+)$")
FROM_STMT_RE = re.compile(r"^\s*from\s+([\w.]+)\s+import\b")

def _is_yaml_module(name):
    name = name.strip()
    return name == "yaml" or name.startswith("yaml.")

def imports_yaml(code):
    for line in code.splitlines():
        m = FROM_STMT_RE.match(line)
        if m and _is_yaml_module(m.group(1)):
            return True
        m = IMPORT_STMT_RE.match(line)
        if not m:
            continue
        # Strip a trailing comment, then treat the rest as the comma-separated import
        # list; each entry may carry an `as` alias, which is not part of the module name.
        names = m.group(1).split("#", 1)[0]
        for entry in names.split(","):
            if _is_yaml_module(entry.split(" as ")[0]):
                return True
    return False

def code_only(text):
    """Drop comment-only lines — prose about yq (or about importing yaml) is not a
    fallback and must not exempt a script from coverage."""
    return "\n".join(l for l in text.splitlines() if not l.lstrip().startswith("#"))

dep_cache = {}
def is_dependent(path):
    if path not in dep_cache:
        code = code_only(read(path))
        dep_cache[path] = imports_yaml(code) and not YQ_RE.search(code)
    return dep_cache[path]

def refs(path):
    """Script basenames referenced by this file, resolved through the tree index."""
    out = set()
    for name in SCRIPT_RE.findall(read(path)):
        for cand in index.get(name, ()):
            out.add(cand)
    return out

MAX_DEPTH = 6
def reachable(commands):
    seen, frontier, depth = set(), set(), 0
    for cmd in commands:
        for name in SCRIPT_RE.findall(cmd):
            for cand in index.get(name, ()):
                frontier.add(cand)
    while frontier and depth < MAX_DEPTH:
        seen |= frontier
        nxt = set()
        for p in frontier:
            nxt |= refs(p)
        frontier = nxt - seen
        depth += 1
    return seen

# ── 2. per-step reachability + dependent subset ──────────────────────────────
need_steps, bare_steps, detail = [], [], {}
for step in steps:
    sid = step["id"]
    cmds = [c.get("command", "") for c in (step.get("commands") or [])]
    reach = reachable(cmds)
    deps = sorted(p for p in reach if is_dependent(p))
    detail[sid] = (len(reach), deps)
    if deps:
        need_steps.append(sid)
    elif not reach:
        bare_steps.append(sid)

all_deps = sorted({p for _, d in detail.values() for p in d})
if not all_deps:
    print("  BLOCK — zero PyYAML-dependent scripts reachable from the checklist. A zero-item",
          file=sys.stderr)
    print("  run cannot prove coverage (the enumeration itself is broken or the tree is empty).",
          file=sys.stderr)
    sys.exit(1)

# ── 3. behavioral assertions against the REAL verify-completion.sh ───────────
def probe(args):
    env = dict(os.environ, AX_PREFLIGHT_FAKE_MISSING="pyyaml")
    p = subprocess.run(["bash", vc_path, "--dry-run"] + args,
                       env=env, capture_output=True, text=True)
    err = p.stderr or ""
    # Discriminate on the MESSAGE, not the bare exit code: an unrelated preflight
    # (missing JDK on a backend step) also exits 2, and must not be misread as
    # PyYAML coverage.
    blocked = p.returncode == 2 and "R25 BLOCK" in err and "PyYAML" in err
    return blocked, p.returncode, err.strip().splitlines()[:1]

violations = []

for step in steps:
    sid = step["id"]
    reach_n, deps = detail[sid]
    blocked, rc, first = probe(["--step", sid])
    needs_pyyaml = sid in need_steps
    if needs_pyyaml and not blocked:
        violations.append(
            f"step [{sid}] reaches {len(deps)} PyYAML-dependent guard(s) but the preflight did NOT "
            f"block without PyYAML (exit {rc}). First dependent: "
            f"{os.path.relpath(deps[0], root)}")
    if sid in bare_steps and blocked:
        violations.append(
            f"step [{sid}] reaches NO scripts at all, yet the preflight blocks on PyYAML — the "
            f"gating is blanket, not step-scoped (a backend-only run must stay runnable)")

full_blocked, full_rc, _ = probe([])
if not full_blocked:
    violations.append(
        f"the FULL step set did not block without PyYAML (exit {full_rc}) even though "
        f"{len(need_steps)} step(s) run PyYAML-dependent guards")

# ── 3b. fail-closed matrix: the guarantee must not depend on the caller ──────
# The preflight above only protects the R25 path. A fork-receiver or CI invoking a guard
# directly, or anything reading a guard's exit code, gets no protection from it — so every
# PyYAML-dependent guard must ALSO fail closed on its own: exit 2 ("cannot verify"), never
# 0, and never an "all pass"-shaped message it did not earn. Simulated deterministically
# with a PYTHONPATH shim whose yaml.py raises ImportError (no install, no network).
shim_dir = tempfile.mkdtemp(prefix="ax-noyaml-")
shim = os.path.join(shim_dir, "")
with open(os.path.join(shim, "yaml.py"), "w") as fh:
    fh.write('raise ImportError("simulated: PyYAML unavailable")\n')

# Scripts that must NOT be executed to be checked. Explicit and reported — never a silent
# pass: each is asserted STATICALLY to carry a fail-closed preflight instead.
PROBE_EXEMPT = {
    "practices/evals/adversarial/run.sh":
        "adversarial case runner: temporarily injects a crafted rule into practices/rules/ "
        "and needs --case, so executing it from a guard would mutate the live catalog",
    # ROUND 6 / P1-2 (TD-2026-07-30-P1-preflight-and-raw-bytes): these two run their python
    # body with `-E`, which IGNORES PYTHONPATH — and this probe simulates PyYAML absence with a
    # PYTHONPATH shim. The simulation is therefore inert against them, and an inert probe that
    # reports PASS is exactly the vacuity this guard exists to catch (measured: both exited 0
    # with the shim in place, having verified everything). `-E` is not negotiable — it is what
    # refuses the sitecustomize.py injection that turned a whole guard into exit 0 — so the
    # honest move is to say the probe cannot reach them and assert the preflight statically,
    # the same treatment adversarial/run.sh already gets. Their preflights are
    # `except ImportError: print("… PyYAML unavailable — cannot run"); sys.exit(2)`.
    "practices/evals/evidence_quote_spotcheck_guard.sh":
        "ROUND 6: runs its python body under `-E` (no PYTHONPATH), so this probe's PYTHONPATH "
        "shim cannot simulate PyYAML absence; asserted statically instead",
    "practices/evals/manifest_snapshot_integrity_guard.sh":
        "ROUND 6: runs its python body under `-E` (no PYTHONPATH), so this probe's PYTHONPATH "
        "shim cannot simulate PyYAML absence; asserted statically instead",
}
FAILCLOSED_MARK = re.compile(r"cannot verify.*PyYAML|PyYAML.*cannot (?:verify|run)|"
                             r"PyYAML is required|PyYAML not installed", re.I)
ALLPASS_RE = re.compile(r"all rules pass|all checks PASS|: OK —|all guards PASS", re.I)
# exit 2 ALONE does not prove a guard failed closed ON THE PARSER — plenty of unrelated
# conditions exit 2 too. Measured on the first .py dependent: WITH PyYAML installed,
# `python3 lib/coverage_map_guard.py` exits 2 from argparse ("the following arguments are
# required: --map"), and WITHOUT it exits 2 from the import guard. Identical code, opposite
# causes — so a code-only check would have accepted the argparse error as proof of failing
# closed, which is the same vacuity the interpreter fix above was meant to remove rather
# than relocate. The output must therefore NAME the parser. A traceback that says
# "ModuleNotFoundError: No module named 'yaml'" satisfies this as legitimately as a
# hand-written message does — both tell the caller the parser is why nothing was verified.
PARSER_MENTION_RE = re.compile(r"yaml", re.I)

probed, exempted = 0, []
for path in all_deps:
    rel = os.path.relpath(path, root)
    reason = PROBE_EXEMPT.get(rel)
    if reason:
        src = read(path)
        # `sys.exit(2)` counts as `exit 2`: the preflight of an embedded python body is written
        # in python, and requiring the shell spelling would reject the very shape being asserted.
        if not (FAILCLOSED_MARK.search(src) and re.search(r"exit 2|sys\.exit\(2\)", src)):
            violations.append(
                f"{rel} is probe-exempt but carries no fail-closed preflight "
                f"(expected a 'cannot verify … PyYAML' message + exit 2)")
        exempted.append((rel, reason))
        continue
    env = dict(os.environ, PYTHONPATH=shim + os.pathsep + os.environ.get("PYTHONPATH", ""))
    # The probe must use the script's OWN interpreter. This used to be a hardcoded
    # `bash`, which was correct only while every dependent happened to be a shell
    # script: the first .py dependent (the gap-convergence engine's guard body, wired
    # in by P2-44) was handed to bash, which died on python syntax with `exit 2` — the
    # SAME code this check accepts as proof of failing closed. It "passed" identically
    # whether PyYAML was present or not, i.e. the evidence was vacuous.
    interpreter = "python3" if path.endswith(".py") else "bash"
    try:
        p = subprocess.run([interpreter, path], env=env, cwd=root,
                           capture_output=True, text=True, timeout=300)
    except subprocess.TimeoutExpired:
        violations.append(f"{rel} hung under simulated PyYAML absence (>300s)")
        continue
    probed += 1
    out = (p.stdout or "") + (p.stderr or "")
    # ONE condition owns the verdict (exit 2 = "cannot verify" is the only acceptable
    # outcome); the message distinguishes the two ways it can be wrong.
    if p.returncode != 2:
        last = (out.strip().splitlines() or ["(silent)"])[-1][:120]
        why = ("exits 0 — it verified NOTHING but reports success"
               if p.returncode == 0 else
               f"exits {p.returncode} — non-zero, but not the repo's 2 = 'cannot verify' "
               f"convention, so a caller cannot tell a tooling failure from a real violation")
        violations.append(f"{rel} {why} under simulated PyYAML absence. Last line: {last}")
    elif not PARSER_MENTION_RE.search(out):
        last = (out.strip().splitlines() or ["(silent)"])[-1][:120]
        violations.append(
            f"{rel} exits 2 under simulated PyYAML absence but NOTHING in its output names "
            f"the parser, so this exit is indistinguishable from an unrelated tooling or "
            f"usage error that also exits 2 (an argparse 'required argument' failure does). "
            f"Say why verification was impossible. Last line: {last}")
    if ALLPASS_RE.search(out):
        violations.append(
            f"{rel} emits an all-pass-shaped message under simulated PyYAML absence: "
            f"{ALLPASS_RE.search(out).group(0)!r}")

# ── 4. report ────────────────────────────────────────────────────────────────
print(f"  PyYAML-dependent scripts reachable from the checklist: {len(all_deps)}")
for p in all_deps:
    print(f"    - {os.path.relpath(p, root)}")
print(f"  steps requiring PyYAML : {', '.join(need_steps) or '(none)'}")
print(f"  steps reaching no script: {', '.join(bare_steps) or '(none)'}")
print(f"  fail-closed probes      : {probed} guard(s) executed under simulated PyYAML absence")
for rel, reason in exempted:
    print(f"    probe-exempt: {rel} — {reason}")
    print(f"                  (asserted statically instead: fail-closed preflight present)")
# DESTRUCTIVE-BUG FIX (2026-07-29): this previously read
#     shutil.rmtree(os.path.dirname(shim.rstrip(os.sep)), ...)
# `shim` IS the mkdtemp directory (with a trailing separator), so dirname() resolved
# to its PARENT — i.e. TMPDIR itself — and the guard recursively deleted the whole
# system temp directory. That wiped the RESULTS_FILE of the very verify-completion.sh
# run executing this guard. Remove the shim directory we created, nothing above it.
shutil.rmtree(shim_dir, ignore_errors=True)

if violations:
    print("", file=sys.stderr)
    for v in violations:
        print(f"  VIOLATION: {v}", file=sys.stderr)
    sys.exit(1)
sys.exit(0)
PY
}

# Run one tree through a throwaway harness that pairs the tree's checklist/scripts with
# the REAL verify-completion.sh (so a fixture can never accidentally test a stub).
run_root() {
    local label="$1" src_root="$2"
    local harness; harness="$(mktemp -d)"
    cp -R "$src_root/." "$harness/" 2>/dev/null
    mkdir -p "$harness/practices/scripts"
    cp "$REAL_VC" "$harness/practices/scripts/verify-completion.sh"
    [ -f "$REPO_ROOT/practices/scripts/_collapse_plan.py" ] && \
        cp "$REPO_ROOT/practices/scripts/_collapse_plan.py" "$harness/practices/scripts/_collapse_plan.py"
    # ROUND 4 (TD-2026-07-30-P1-anchor-runtime): verify-completion.sh now sources
    # practices/scripts/lib/release_anchor.sh (release anchor + P1-1/P1-2 authentication) and
    # shells out to practices/scripts/lib/tree_fingerprint.py (the ONE definition of the working
    # -tree fingerprint, so the recency guard can recompute what the runner recorded). A sandbox
    # that installs the runner without its libs is testing a runner that cannot start.
    mkdir -p "$harness/practices/scripts/lib"
    cp "$REPO_ROOT/practices/scripts/lib/release_anchor.sh" "$harness/practices/scripts/lib/"
    cp "$REPO_ROOT/practices/scripts/lib/tree_fingerprint.py" "$harness/practices/scripts/lib/"
    echo "── [$label]"
    analyse "$harness" "$harness/practices/scripts/verify-completion.sh"
    local rc=$?
    rm -rf "$harness"
    return "$rc"
}

if [ "$RUN_FIXTURES" -eq 1 ]; then
    RC=0
    run_root "fixture pass_covered" "$FIXTURE_DIR/pass_covered"
    [ $? -eq 0 ] || { echo "pyyaml_preflight_coverage_guard: FAIL — pass fixture did not exit 0" >&2; RC=1; }
    run_root "fixture fail_hidden_dependency" "$FIXTURE_DIR/fail_hidden_dependency"
    [ $? -eq 1 ] || { echo "pyyaml_preflight_coverage_guard: FAIL — fail_hidden_dependency did not exit 1" >&2; RC=1; }
    run_root "fixture fail_open_guard" "$FIXTURE_DIR/fail_open_guard"
    [ $? -eq 1 ] || { echo "pyyaml_preflight_coverage_guard: FAIL — fail_open_guard did not exit 1" >&2; RC=1; }
    [ "$RC" -eq 0 ] && { echo "pyyaml_preflight_coverage_guard: PASS — fixture trio discriminates"; exit 0; }
    exit 1
fi

if [ -n "$ROOT_OVERRIDE" ]; then
    run_root "repo-root $ROOT_OVERRIDE" "$ROOT_OVERRIDE"
    RC=$?
    if [ "$RC" -eq 0 ]; then
        echo "pyyaml_preflight_coverage_guard: PASS — preflight covers every PyYAML-dependent step"
    elif [ "$RC" -eq 1 ]; then
        echo "pyyaml_preflight_coverage_guard: FAIL — a PyYAML-dependent surface is unprotected (see VIOLATION lines above)" >&2
    fi
    exit "$RC"
fi

echo "── [live repo]"
analyse "$REPO_ROOT" "$REAL_VC"
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "pyyaml_preflight_coverage_guard: PASS — every dependent step is preflight-covered, every dependent guard fails closed, and step-gating stays scoped"
elif [ "$RC" -eq 1 ]; then
    echo "pyyaml_preflight_coverage_guard: FAIL — a PyYAML-dependent surface is unprotected (see VIOLATION lines above)" >&2
fi
exit "$RC"
