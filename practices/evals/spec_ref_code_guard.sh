#!/usr/bin/env bash
# practices/evals/spec_ref_code_guard.sh — IMW1-C hard gate.
#
# Backend-code analog of recipe_spec_referential_integrity_guard.sh.
#
# IDW1 gap: a SecurityConfig comment referenced `specs/issue-tracker-l0.yaml`,
# a spec file that never existed, and none of the 49 existing guards flagged it.
# This guard closes that gap for backend Java code.
#
# Predicate: every string matching `specs/<path>.yaml` mentioned ANYWHERE in
# backend/src/**/*.java (production code OR comments) MUST resolve to an existing
# file under specs/ at the repo root.
#
# A reference may carry an optional `#<ITEM-ID>` fragment (e.g.
# `specs/payment-l0.yaml#PAY-001`); only the file part (before the `#`) is
# checked here — item-id existence is the concern of spec_ref_guard.sh for
# practices/rules/*.md, not of free-text code comments.
#
# Exit 0  — every specs/*.yaml reference in backend Java resolves
# Exit 1  — one or more dangling references (each printed with file:line)
# Exit 2  — bad arguments / environment error
#
# Fast: pure file scan via python3 (already a repo dependency). No gradle.
# Bash 3.2 compatible.
#
# Usage:
#   bash practices/evals/spec_ref_code_guard.sh
#   bash practices/evals/spec_ref_code_guard.sh --root /path/to/repo
#   bash practices/evals/spec_ref_code_guard.sh --verbose

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
VERBOSE=0

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "spec_ref_code_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$REPO_ROOT" ]; then
    echo "spec_ref_code_guard: --root '$REPO_ROOT' is not a directory" >&2
    exit 2
fi

BACKEND_SRC="$REPO_ROOT/backend/src"
if [ ! -d "$BACKEND_SRC" ]; then
    # No backend Java tree → nothing to scan. Not an error (fork-receiver may
    # have a frontend-only or as-yet-unpopulated workload).
    echo "spec_ref_code_guard: no backend/src/ at $BACKEND_SRC — nothing to check"
    exit 0
fi

python3 - "$REPO_ROOT" "$VERBOSE" <<'PYEOF'
import pathlib
import re
import sys

repo_root = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"
backend_src = repo_root / "backend" / "src"

# Match specs/<path>.yaml where <path> is one or more path segments. The
# reference may be followed by an optional #ITEM-ID fragment which we strip.
# We deliberately accept the token wherever it appears (inside a Java string
# literal, a // line comment, or a /* block comment */) — provenance comments
# that name a non-existent spec are exactly the IDW1 deviation we are catching.
REF_RE = re.compile(r'specs/[A-Za-z0-9_./-]+\.yaml')

java_files = sorted(backend_src.rglob("*.java"))

if not java_files:
    print("spec_ref_code_guard: backend/src/ has no *.java files — nothing to check")
    sys.exit(0)

violations = []   # list of (file_rel, lineno, ref)
ok_refs = 0
scanned = 0

for jf in java_files:
    scanned += 1
    try:
        lines = jf.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as exc:
        print(f"spec_ref_code_guard: cannot read {jf}: {exc}", file=sys.stderr)
        sys.exit(2)

    rel = jf.relative_to(repo_root).as_posix()
    for lineno, line in enumerate(lines, start=1):
        for m in REF_RE.finditer(line):
            ref = m.group(0)
            spec_file_rel = ref  # already file-part only (regex stops at .yaml)
            spec_abs = repo_root / spec_file_rel
            if spec_abs.is_file():
                ok_refs += 1
                if verbose:
                    print(f"  OK  {rel}:{lineno} → {spec_file_rel} ✓")
            else:
                violations.append((rel, lineno, ref))

if violations:
    for rel, lineno, ref in violations:
        print(
            f"VIOLATION [{rel}:{lineno}]: spec reference '{ref}' → "
            f"file '{ref}' does not exist under specs/",
            file=sys.stderr,
        )
    print(
        f"spec_ref_code_guard: {len(violations)} dangling spec reference(s) in "
        f"backend Java ({ok_refs} resolved, {scanned} file(s) scanned) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"spec_ref_code_guard: PASS — {ok_refs} specs/*.yaml reference(s) all resolve "
    f"across {scanned} backend Java file(s)"
)
sys.exit(0)
PYEOF
