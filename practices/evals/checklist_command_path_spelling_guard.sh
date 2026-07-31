#!/usr/bin/env bash
# practices/evals/checklist_command_path_spelling_guard.sh
#   ROUND 14 / P1-B (TD-2026-08-01-(P1-posix-resolution-and-runtime-paths)).
#
# THE DEFECT. R25 takes the `command` and `working_directory` strings out of
# practices/verification-checklist.yaml VERBATIM (verify-completion.sh:1086) and executes them
# through `cd "$exec_wd"` + `bash -c` (:1561). Nothing between the yaml and the shell asks whether
# those strings name paths the repository actually RECORDS. MEASURED, committed content only, no
# environment control:
#
#     command: "bash practices/evals/spec_policy_ref_guard.sh"
#          →   "bash PRACTICES/evals/spec_policy_ref_guard.sh"
#
# On case-insensitive APFS the command exits 0, `git cat-file -e HEAD:PRACTICES/evals/...` exits
# 128 (the recorded tree has no such path), and all ten of R25's violation buckets stay EMPTY —
# so R25 publishes GREEN EVIDENCE for a command that fails immediately on a case-sensitive
# receiver. This is the same doctrine the round-8..14 tree family enforces ("the thing R25
# verified is not the thing the push ships"), one level up: the CHECKLIST is the one input whose
# integrity the entire R25 evidence chain depends on.
#
# WHY A NEW GUARD RATHER THAN A FOLD-IN — stated because the choice is load-bearing:
#   · completion_checklist_recency_guard.sh already owns the fold and the tree sweep, but it runs
#     at PRE-PUSH and only after a full audit-log chain (checks 1-11) has been satisfied, and its
#     fixture roots are non-git trees where check 12 does not run at all. Folding here would make
#     the demonstrated reproduction detectable only at push time and NOT fixture-provable.
#   · verification_checklist_task_coverage_guard.sh [58] parses the same file, but it answers a
#     different question (is every registered per-domain task LISTED), with its own fixtures.
#   · run-all-guards.sh IS an R25 step, so a guard registered there fails the R25 run that
#     contains the aliased command — the tightest possible closure of the reproduction above.
#
# THE RULE, stated exactly. For every path-like token of every `command` and every effective
# `working_directory` in the checklist:
#   1. resolve it, repo-relative, against the command's working directory;
#   2. if the result IS a recorded spelling — a tracked path or a directory component of one —
#      PASS;
#   3. otherwise, if it FOLDS EQUAL to a recorded spelling, using the SHARED `_fold_path_key`
#      imported from practices/scripts/lib/tree_fingerprint.py (so case, normalization, non-ASCII
#      case and ignorable Cf are covered in one step, and every future axis the fold gains is
#      gained here too) — BLOCK;
#   4. otherwise → NOT THIS CLASS (a bare executable from PATH, a flag value, a subcommand, a
#      generated/ignored artifact — none of which the repository records, so none of which has a
#      recorded spelling to be an alias OF).
#
# NO (st_dev, st_ino) DISCRIMINATOR HERE, AND THAT IS DELIBERATE. The tree sweep compares two
# spellings that the VERIFYING filesystem serves from one file, so it must MEASURE that identity
# rather than assume it. This guard's subject is a STRING inside a committed file: there is no
# local resolution to measure, and the filesystem that decides is the RECEIVER'S. A token that is
# not a recorded spelling but folds onto one is broken on any faithful filesystem and silently
# green on an aliasing one — both verdicts say BLOCK. The consequence is that this guard is
# FILESYSTEM-INDEPENDENT: it returns the same answer on case-sensitive Linux and on APFS, which
# is also what makes its fixtures portable.
#
# CANDIDATE RULES — the complete list, because a token extractor that is not enumerated is a
# false-positive generator. A token is a CANDIDATE iff ALL of:
#   (a) it survives `shlex.split(command, posix=True)` — quoting and word splitting are the
#       shell's, not a regex's;
#   (b) it is not a pure shell operator (&& || | ; & ( ) { } > >> < << 2> 2>&1 …);
#   (c) it does not start with `-` (a flag, or a flag-with-value like -PcontextCacheMaxSize=32);
#   (d) it contains no `=` (an env assignment or a property assignment, not a path);
#   (e) it is not absolute and contains no `://` (a location on the receiver's root filesystem, or
#       a URL — neither is a path this repository can record, exactly as the symlink disposition
#       table reasons about absolute targets);
#   (f) it is not `.` or `..`;
#   (g) it contains no shell expansion metacharacter ($ ` * ? [ ] ~) — an unexpanded token is not
#       a spelling anyone can compare.
# `working_directory` is a candidate unconditionally (minus `.`/empty), because it is a repo path
# by schema and needs no heuristic at all.
#
# ADVISORY SECOND PASS (--advisory-scripts, non-blocking, printed and never counted): the same
# verdict applied to path-like literals inside the shell scripts the checklist INVOKES (one level,
# named by the checklist itself). It is advisory BECAUSE deciding which substrings of an arbitrary
# file are paths is undecidable by inspection — see docs/BACKLOG.md P2-72, which stays OPEN with
# its true remedy (verify on a case-sensitive, normalization-sensitive checkout) recorded there.
#
# Exit codes:
#   0 — every checklist path token names a recorded spelling (or is not a repo path at all)
#   1 — at least one token names a tracked path by a spelling this repository does not record
#   2 — usage / setup error (missing checklist, missing fold helper, no yaml parser, not a git tree)
#
# Usage:
#   bash practices/evals/checklist_command_path_spelling_guard.sh
#   bash practices/evals/checklist_command_path_spelling_guard.sh --root DIR
#   bash practices/evals/checklist_command_path_spelling_guard.sh --advisory-scripts
#   bash practices/evals/checklist_command_path_spelling_guard.sh --fixtures
#   bash practices/evals/checklist_command_path_spelling_guard.sh --show   # the candidate table

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
FIXTURES_MODE=0
ADVISORY_SCRIPTS=0
SHOW=0
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --fixtures) FIXTURES_MODE=1; shift ;;
        --advisory-scripts) ADVISORY_SCRIPTS=1; shift ;;
        --show) SHOW=1; shift ;;
        *) echo "checklist_command_path_spelling_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ──────────────────────────────────────────────────────────────
# A fixture root must be a GIT tree, because "recorded spelling" means `git ls-files`. The
# fixtures therefore ship as plain files and the git identity is created HERE, in a throwaway
# copy — a nested .git inside this repository would be a gitlink, and the round-9 family already
# refuses those. `git add -A` records the spelling the fixture ships, on every filesystem.
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/checklist-command-path-spelling"
    [ -d "$FIXTURES_DIR" ] || {
        echo "checklist_command_path_spelling_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2; }
    tmp_root="$(mktemp -d "${TMPDIR:-/tmp}/ax-ccps-fixtures.XXXXXX")" || exit 2
    trap 'rm -rf "$tmp_root"' EXIT
    pass=0; fail=0
    for sub in "$FIXTURES_DIR"/pass_* "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        name="$(basename "$sub")"
        work="$tmp_root/$name"
        mkdir -p "$work" || exit 2
        ( cd "$sub" && tar cf - . ) | ( cd "$work" && tar xf - ) || exit 2
        ( cd "$work" && git init -q . \
          && git add -A \
          && git -c user.name=ax -c user.email=ax@example.invalid commit -q -m fixture ) \
            >/dev/null 2>&1 || { echo "FAIL [$name] — fixture git setup failed"; fail=$((fail+1)); continue; }
        bash "$0" --root "$work" >/dev/null 2>&1
        rc=$?
        case "$name" in
            pass_*) want=0 ;;
            *)      want=1 ;;
        esac
        if [ "$rc" -eq "$want" ]; then
            echo "PASS [checklist_command_path_spelling/$name]"
            pass=$((pass + 1))
        else
            echo "FAIL [checklist_command_path_spelling/$name] — expected exit $want, got $rc"
            fail=$((fail + 1))
        fi
    done
    echo ""
    echo "checklist_command_path_spelling_guard: fixtures $pass PASS / $fail FAIL"
    [ "$fail" -gt 0 ] && exit 1
    exit 0
fi

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$(cd "$ROOT_OVERRIDE" 2>/dev/null && pwd)"
[ -n "$REPO_ROOT" ] || { echo "checklist_command_path_spelling_guard: root not found" >&2; exit 2; }

# The FOLD is imported from the live helper, never re-implemented — a third copy of a rule that
# already exists twice is a third chance to drift. `-B` because this is the only probe here that
# imports out of the tree, and a stray __pycache__/ would dirty the very tree R25 fingerprints.
python3 -B - "$REPO_ROOT" "$SCRIPT_DIR" "$ADVISORY_SCRIPTS" "$SHOW" <<'PYEOF'
import os
import pathlib
import posixpath
import re
import shlex
import subprocess
import sys

sys.dont_write_bytecode = True
root = pathlib.Path(sys.argv[1])
guard_dir = pathlib.Path(sys.argv[2])
advisory_scripts = sys.argv[3] == "1"
show = sys.argv[4] == "1"

checklist = root / "practices" / "verification-checklist.yaml"
if not checklist.is_file():
    print(f"checklist_command_path_spelling_guard: missing {checklist}", file=sys.stderr)
    sys.exit(2)

# ── the shared fold ───────────────────────────────────────────────────────────
# Prefer the fold helper inside the ROOT being scanned (so a fixture is self-describing); fall
# back to the guard's own repo. A missing fold is exit 2, never a silent case-only comparison.
import importlib.util
fold = None
for cand in (root / "practices/scripts/lib/tree_fingerprint.py",
             guard_dir.parent / "scripts/lib/tree_fingerprint.py"):
    if cand.is_file():
        spec = importlib.util.spec_from_file_location("ax_tf_fold", cand)
        mod = importlib.util.module_from_spec(spec)
        try:
            spec.loader.exec_module(mod)
            fold = mod._fold_path_key
        except Exception as exc:            # noqa: BLE001 — reported, never swallowed
            print(f"checklist_command_path_spelling_guard: could not load the shared fold from "
                  f"{cand}: {exc}", file=sys.stderr)
            sys.exit(2)
        break
if fold is None:
    print("checklist_command_path_spelling_guard: practices/scripts/lib/tree_fingerprint.py not "
          "found, so the shared fold cannot be used and a case-only comparison would be a "
          "weaker rule wearing this guard's name.", file=sys.stderr)
    sys.exit(2)

# ── the RECORD: tracked paths and every directory component of one ────────────
p = subprocess.run(["git", "-C", str(root), "ls-files", "-z"],
                   stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
if p.returncode != 0:
    print(f"checklist_command_path_spelling_guard: {root} is not a git tree, so there is no "
          f"RECORD to compare spellings against.", file=sys.stderr)
    sys.exit(2)
recorded = set()
for raw in p.stdout.split(b"\0"):
    if not raw:
        continue
    comps = raw.split(b"/")
    for n in range(1, len(comps) + 1):
        recorded.add(b"/".join(comps[:n]))
if not recorded:
    print("checklist_command_path_spelling_guard: the record is EMPTY (no tracked paths), so "
          "this guard would be vacuous.", file=sys.stderr)
    sys.exit(2)
foldcache = {}
by_fold = {}
for name in recorded:
    by_fold.setdefault(fold(name, foldcache), set()).add(name)

# ── the checklist ─────────────────────────────────────────────────────────────
try:
    import yaml
except Exception:
    yaml = None
if yaml is None:
    print("checklist_command_path_spelling_guard: PyYAML is required to read the checklist "
          "(R25 already declares it a prerequisite). Install it and re-run.", file=sys.stderr)
    sys.exit(2)
doc = yaml.safe_load(checklist.read_text())
if not isinstance(doc, dict) or not isinstance(doc.get("checklist"), list):
    print("checklist_command_path_spelling_guard: the checklist has no `checklist:` list — its "
          "shape is not what verify-completion.sh reads.", file=sys.stderr)
    sys.exit(2)
default_wd = ((doc.get("defaults") or {}).get("working_directory") or ".")

OPERATORS = {"&&", "||", "|", ";", "&", "(", ")", "{", "}", ">", ">>", "<", "<<",
             "2>", "2>>", "2>&1", ">&2", "!", "[[", "]]"}
META = set("$`*?[]~")


def is_candidate(tok):
    """The CANDIDATE RULES from the header, in the order they are stated there."""
    if not tok or tok in OPERATORS:
        return False, "shell operator"
    if tok.startswith("-"):
        return False, "flag"
    if "=" in tok:
        return False, "assignment"
    if tok.startswith("/"):
        return False, "absolute"
    if "://" in tok:
        return False, "url"
    if tok in (".", ".."):
        return False, "cwd marker"
    if any(ch in META for ch in tok):
        return False, "unexpanded shell metacharacter"
    return True, ""


def repo_relative(tok, wd):
    """Resolve <tok> repo-relative against <wd>. None when it leaves the repository."""
    base = "" if wd in (".", "", None) else wd
    joined = posixpath.normpath(posixpath.join(base, tok)) if base else posixpath.normpath(tok)
    if joined.startswith("../") or joined == ".." or joined.startswith("/"):
        return None
    if joined == ".":
        return None
    return joined


def verdict(rel):
    """(state, aliases). state ∈ recorded | alias | unrelated."""
    b = rel.encode("utf-8", "surrogateescape")
    if b in recorded:
        return "recorded", ()
    names = by_fold.get(fold(b, foldcache))
    if names:
        return "alias", tuple(sorted(n.decode(errors="replace") for n in names))
    return "unrelated", ()


rows = []          # (where, token, resolved, state, aliases)
violations = []
for step in doc["checklist"]:
    if not isinstance(step, dict):
        continue
    sid = step.get("id", "<no id>")
    for entry in (step.get("commands") or []):
        if not isinstance(entry, dict):
            continue
        cmd = entry.get("command", "") or ""
        wd = entry.get("working_directory", default_wd) or default_wd
        # the working_directory is a repo path BY SCHEMA — no heuristic needed
        if wd not in (".", ""):
            rel = repo_relative(wd, ".")
            if rel is not None:
                st, al = verdict(rel)
                rows.append((f"{sid}/working_directory", wd, rel, st, al))
                if st == "alias":
                    violations.append((sid, "working_directory", wd, rel, al))
        try:
            toks = shlex.split(cmd, posix=True)
        except ValueError as exc:
            print(f"VIOLATION [checklist_command_path_spelling]: CHECKLIST_COMMAND_UNPARSEABLE — "
                  f"step '{sid}' command {cmd!r} does not lex as a shell word list ({exc}). R25 "
                  f"hands this string to `bash -c`, so a string this guard cannot read is a "
                  f"string nobody audited.")
            sys.exit(1)
        for tok in toks:
            ok, why = is_candidate(tok)
            if not ok:
                rows.append((f"{sid}/command", tok, "-", f"skipped ({why})", ()))
                continue
            rel = repo_relative(tok, wd)
            if rel is None:
                rows.append((f"{sid}/command", tok, "-", "skipped (leaves the repo)", ()))
                continue
            st, al = verdict(rel)
            rows.append((f"{sid}/command", tok, rel, st, al))
            if st == "alias":
                violations.append((sid, "command", tok, rel, al))

if show:
    print(f"# candidate table for {checklist}")
    for where, tok, rel, st, al in rows:
        extra = (" ≡ " + " / ".join(al)) if al else ""
        print(f"{st:<34} {where:<44} {tok!r} -> {rel}{extra}")
    counts = {}
    for _, _, _, st, _ in rows:
        counts[st.split(" ")[0]] = counts.get(st.split(" ")[0], 0) + 1
    print(f"# totals: {counts}")

for sid, field, tok, rel, al in violations:
    print(f"VIOLATION [checklist_command_path_spelling]: CHECKLIST_PATH_ALIAS — step '{sid}' "
          f"{field} {tok!r} resolves to '{rel}', which this repository does NOT record; the "
          f"record spells it {' / '.join(al)}. R25 executes this string verbatim "
          f"(verify-completion.sh: `cd \"$exec_wd\"` + `bash -c`), so on a filesystem that "
          f"aliases the difference the step runs and R25 publishes GREEN EVIDENCE, while a "
          f"receiver whose filesystem does not alias it gets a command that fails immediately. "
          f"Measured: rewriting `practices/` as `PRACTICES/` in one command left the run exit 0 "
          f"with all ten violation buckets EMPTY, while `git cat-file -e HEAD:PRACTICES/...` "
          f"exited 128. Spell it the way the record does and re-run.")

# ── ADVISORY second pass — NON-BLOCKING BY CONSTRUCTION ───────────────────────
# One level only: the shell scripts the CHECKLIST names. This is a heuristic, and it is labelled
# one: deciding which substrings of an arbitrary file are paths is undecidable by inspection, so
# this pass can only ever report a SUBSET. It never contributes to the exit code, and the class
# stays open in docs/BACKLOG.md P2-72 with its true remedy (run the suite on a case-sensitive,
# normalization-sensitive checkout).
if advisory_scripts:
    LITERAL = re.compile(r"(?:\$\{?(SCRIPT_DIR|REPO_ROOT)\}?/)?"
                         r"((?:\./)?[A-Za-z0-9_][A-Za-z0-9_.-]*(?:/[A-Za-z0-9_.-]+)+)")
    scripts = sorted({rel for _, _, rel, st, _ in rows
                      if st == "recorded" and isinstance(rel, str) and rel.endswith(".sh")})
    reported = 0
    for rel in scripts:
        try:
            body = (root / rel).read_text(errors="replace")
        except OSError:
            continue
        seen = set()
        for m in LITERAL.finditer(body):
            var, lit = m.group(1), m.group(2)
            base = "." if var in (None, "REPO_ROOT") else posixpath.dirname(rel)
            r = repo_relative(lit, base)
            if r is None or r in seen:
                continue
            seen.add(r)
            st, al = verdict(r)
            if st == "alias":
                reported += 1
                print(f"ADVISORY [checklist_command_path_spelling]: {rel} names '{r}', which the "
                      f"record spells {' / '.join(al)} (heuristic literal scan; NON-BLOCKING — "
                      f"see docs/BACKLOG.md P2-72)")
    print(f"# advisory literal scan: {len(scripts)} script(s) invoked by the checklist, "
          f"{reported} alias(es) reported (non-blocking)")

if violations:
    sys.exit(1)
print(f"checklist_command_path_spelling_guard: {len(rows)} token(s) examined, "
      f"{sum(1 for r in rows if r[3] == 'recorded')} named a recorded spelling, 0 aliases.")
sys.exit(0)
PYEOF
exit $?
