#!/usr/bin/env bash
# practices/evals/fixture_tracked_completeness_guard.sh — guard [115].
#
# INVARIANT: every file that exists on disk under practices/evals/fixtures/** must be TRACKED IN
# HEAD. A fixture file that lives only on the maintainer's machine makes every measurement taken
# on top of it unreproducible — a clean clone (or a GitHub Actions runner) does not have that file,
# so a green run here says nothing about the tree that was actually pushed.
#
# WHY THIS EXISTS — the observed defect, not a hypothetical:
#   practices/evals/fixtures/consumer-e2e/project/frontend/package-lock.json existed on disk and
#   was silently absent from the repository, because the MACHINE-LEVEL gitignore
#   (`core.excludesFile`, e.g. ~/.gitignore_global) carries a `package-lock.json` line and
#   `git add -A` skips ignored paths WITHOUT SAYING SO. practices/scripts/verify-downstream.sh runs
#   `npm ci`, which REQUIRES that lockfile. The harness's 11/11 PASS was therefore produced by an
#   uncommitted file, and could not be reproduced anywhere else — a false green, and its own
#   fixture README claimed the file was committed. Nothing in the suite could see this, because
#   every other gate reads the working tree, where the file is present.
#
# WHY THE DIAGNOSIS IS PART OF THE GUARD, NOT AN EXTRA: "file X is untracked" is not actionable —
# `git add` had already been run and had silently done nothing. For each offending path this guard
# prints WHY it is untracked, including the verbatim `git check-ignore -v` line, so the answer
# ("your global gitignore ate it; use `git add -f`") is on screen instead of inferred.
#
# NOTHING IS HARDCODED: the file list is derived from disk (`os.walk`), the tracked set from
# `git ls-tree -r HEAD`, and the submodule set from that same listing's gitlink (mode 160000)
# entries. Adding or removing fixtures never requires touching this file.
#
# LEGITIMATE EXCLUSIONS — deliberately narrow, because a broad exclusion is how this guard would
# become vacuous:
#   1. SUBMODULE SUBTREES. practices/evals/fixtures/{spring-petclinic,spring-realworld,
#      spring-modulith-example} are registered submodules; HEAD tracks each as a gitlink pinning an
#      exact upstream commit, and .gitmodules carries the URL, so `git submodule update --init`
#      reproduces their contents byte-for-byte on a clean clone. Their working-tree files are
#      legitimately absent from THIS repository's blob set. The prefixes are read from HEAD's
#      gitlink entries — never a hardcoded list of names.
#   2. BUILD OUTPUT DIRECTORIES named exactly node_modules, build, .gradle, .next — AND ONLY when
#      the directory contains no tracked file at all. A fixture directory that is genuinely named
#      `build` and has committed content is still scanned in full; the carve-out applies solely to
#      a tree that is entirely generated (e.g. a fixture-shaped external clone that was compiled
#      in place, or an in-place harness run). These four names are the only generated-tree names
#      this repository's toolchains (npm / gradle / next) produce.
#      NOTE: at the time this guard was written NO such directory existed under fixtures/ — the
#      exclusion is a documented affordance, not a live crutch, and it is written to stay inert.
#
# "TRACKED" MEANS PRESENT IN HEAD, not merely staged. A staged-but-uncommitted file is reported as
# a distinct, still-BLOCKING case ("STAGED, NOT COMMITTED") because the sha that gets pushed is
# what a fork-receiver clones. R25 runs at HEAD (the 49th guard binds its audit log to HEAD), so a
# fixture added in the normal commit -> verify-completion.sh -> push order is already in HEAD by
# the time this guard sees it.
#
# FIXTURE / --root DESIGN (precedent: downstream_release_recency_guard.sh [114]): fixtures under
# practices/evals/fixtures/fixture_tracked_completeness/ are NOT git repositories, so `git` cannot
# answer "is this tracked" there. `git rev-parse --show-toplevel` WALKS UP out of a subdirectory,
# so the test is whether ROOT ITSELF is a work-tree toplevel (os.path.samefile), exactly as [114]
# does it.
#   - GIT MODE (root IS a work-tree toplevel): scan <root>/practices/evals/fixtures, tracked set
#     from `git ls-tree -r HEAD`.
#   - FIXTURE-SHAPED MODE (root is not): scan every file under <root> EXCEPT the manifest file
#     `.ax-fixture-tracked.txt`, whose lines stand in for `git ls-tree -r HEAD` output (one
#     root-relative path per line, blank lines and #-comments ignored). A missing manifest is a
#     usage error (exit 2), never a silent pass. The manifest file excludes itself from the scan
#     because it is the stand-in for git's own index, which is likewise not a tracked blob.
#
# Exit codes: 0 pass · 1 violation (used by every fail_* fixture — fixture_kill_proof_guard.sh [87]
# only registers exit-1 fail fixtures) · 2 usage / premise error (missing root, missing fixtures
# directory, unresolvable HEAD, missing fixture manifest).
#
# Usage:
#   bash practices/evals/fixture_tracked_completeness_guard.sh
#   bash practices/evals/fixture_tracked_completeness_guard.sh --root DIR
#   bash practices/evals/fixture_tracked_completeness_guard.sh --fixtures

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="${2:-}"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "fixture_tracked_completeness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/fixture_tracked_completeness"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "fixture_tracked_completeness_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [fixture_tracked_completeness/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [fixture_tracked_completeness/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        rc=0
        bash "$0" --root "$sub" >/dev/null 2>&1 || rc=$?
        if [ "$rc" -eq 1 ]; then
            echo "PASS [fixture_tracked_completeness/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [fixture_tracked_completeness/$(basename "$sub")] — expected exit 1 on FAIL fixture, got $rc"
            fail=$((fail + 1))
        fi
    done

    echo ""
    echo "fixture_tracked_completeness_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live / --root mode ───────────────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "fixture_tracked_completeness_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi
SCAN_ROOT="$(cd "$SCAN_ROOT" && pwd)"

python3 - "$SCAN_ROOT" <<'PYEOF'
import os
import subprocess
import sys

root = sys.argv[1]

# Directory names that are pure build output. See the header: excluded ONLY when the directory
# holds no tracked file at all, so a committed fixture directory that happens to bear one of these
# names is still scanned in full.
GENERATED_DIR_NAMES = ("node_modules", "build", ".gradle", ".next")

MANIFEST_NAME = ".ax-fixture-tracked.txt"


def git(*args, cwd=root):
    p = subprocess.run(["git"] + list(args), cwd=cwd, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    return (p.returncode,
            p.stdout.decode(errors="replace").strip(),
            p.stderr.decode(errors="replace").strip())


def usage(msg, *extra):
    print("fixture_tracked_completeness_guard: " + msg, file=sys.stderr)
    for ln in extra:
        print("  " + str(ln), file=sys.stderr)
    sys.exit(2)


def walk(scan_dir, base, skip_prefixes, tracked_rel):
    """Every file under scan_dir as a `base`-relative path, minus the documented exclusions."""
    found = []
    for dirpath, dirnames, filenames in os.walk(scan_dir):
        rel_dir = os.path.relpath(dirpath, base)
        rel_dir = "" if rel_dir == "." else rel_dir
        if any(rel_dir == p or rel_dir.startswith(p + "/") for p in skip_prefixes):
            dirnames[:] = []
            continue
        keep = []
        for d in dirnames:
            sub_rel = os.path.join(rel_dir, d) if rel_dir else d
            if d in GENERATED_DIR_NAMES and not any(
                    t == sub_rel or t.startswith(sub_rel + "/") for t in tracked_rel):
                # Entirely generated tree: nothing under it is tracked. Skipped, and said out loud.
                print("  (skipping generated tree with zero tracked files: %s/)" % sub_rel)
                continue
            keep.append(d)
        dirnames[:] = keep
        for f in filenames:
            found.append(os.path.join(rel_dir, f) if rel_dir else f)
    return found


# ── Detect git-rootedness (root ITSELF must be the work-tree toplevel — see header) ───────────
rc, toplevel, _ = git("rev-parse", "--show-toplevel")
is_git = False
if rc == 0 and toplevel:
    try:
        is_git = os.path.samefile(toplevel, root)
    except OSError:
        is_git = False

violations = []   # (relpath, reason, detail)

if is_git:
    fixtures_rel = os.path.join("practices", "evals", "fixtures")
    fixtures_abs = os.path.join(root, fixtures_rel)
    if not os.path.isdir(fixtures_abs):
        usage("no %s directory under %s — nothing to check, and this guard refuses to report a "
              "pass it did not measure." % (fixtures_rel, root))

    rc, listing, err = git("ls-tree", "-r", "HEAD", "--", fixtures_rel)
    if rc != 0:
        usage("could not read HEAD's tree for %s (unborn branch / corrupt repo?)" % fixtures_rel,
              err or "(git printed nothing)")

    tracked = set()
    submodules = []
    for line in listing.splitlines():
        if "\t" not in line:
            continue
        meta, path = line.split("\t", 1)
        mode = meta.split()[0]
        if mode == "160000":
            submodules.append(path)
        else:
            tracked.add(path)

    rc, idx_listing, _ = git("ls-files", "--", fixtures_rel)
    staged = set(idx_listing.splitlines())

    skip_prefixes = [os.path.relpath(s, ".") for s in submodules]
    disk = walk(fixtures_abs, root, skip_prefixes, tracked)

    for rel in sorted(set(disk) - tracked):
        if rel in staged:
            violations.append((rel, "STAGED, NOT COMMITTED",
                               "in the index but absent from HEAD — a clone of the pushed sha "
                               "will not have it. Commit it."))
            continue
        rc_ci, out_ci, _ = git("check-ignore", "-v", "--", rel)
        if rc_ci == 0 and out_ci:
            violations.append((rel, "IGNORED", "git check-ignore -v says: " + out_ci))
        else:
            violations.append((rel, "NEVER ADDED",
                               "not ignored, not staged, not in HEAD — it was simply never "
                               "`git add`ed."))

    scanned_label = "%s (%d file(s) on disk, %d tracked in HEAD, %d submodule subtree(s) skipped)" \
        % (fixtures_rel, len(disk), len(tracked), len(submodules))
else:
    # ── FIXTURE-SHAPED MODE ──────────────────────────────────────────────────
    manifest_path = os.path.join(root, MANIFEST_NAME)
    if not os.path.isfile(manifest_path):
        usage("%s is not a git work tree and carries no %s — this guard cannot tell which files "
              "are 'tracked' here, and refuses to guess." % (root, MANIFEST_NAME))
    with open(manifest_path, encoding="utf-8") as f:
        tracked = set()
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                tracked.add(line)

    disk = [p for p in walk(root, root, [], tracked) if p != MANIFEST_NAME]
    for rel in sorted(set(disk) - tracked):
        violations.append((rel, "NOT IN MANIFEST",
                           "present on disk but absent from %s" % MANIFEST_NAME))
    scanned_label = "%s (fixture-shaped: %d file(s) on disk, %d listed in %s)" \
        % (root, len(disk), len(tracked), MANIFEST_NAME)

if violations:
    print("fixture_tracked_completeness_guard: FAIL — %d fixture file(s) exist on disk but are "
          "NOT tracked in HEAD." % len(violations), file=sys.stderr)
    print("  Every measurement taken on top of such a file is unreproducible: a clean clone of "
          "the pushed sha does not have it.", file=sys.stderr)
    for rel, reason, detail in violations:
        print("  [%s] %s" % (reason, rel), file=sys.stderr)
        print("      %s" % detail, file=sys.stderr)
    if any(reason == "IGNORED" for _, reason, _ in violations):
        print("  FIX: the ignore rule above is usually MACHINE-LEVEL (core.excludesFile / "
              "~/.gitignore_global), which `git add -A` obeys SILENTLY — force it in: "
              "git add -f <path>", file=sys.stderr)
    sys.exit(1)

print("fixture_tracked_completeness_guard: PASS — %s" % scanned_label)
sys.exit(0)
PYEOF
exit $?
