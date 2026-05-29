#!/usr/bin/env bash
# practices/evals/controller_problemdetail_guard.sh
# IMW1-C / IDW1-gap closure (controller-problemdetail, 50th hard guard candidate).
#
# RFC 9457 (Problem Details for HTTP APIs) is the canonical error-body
# contract for this template. Every @ExceptionHandler that lives inside a
# domain controller or controller-advice MUST return a ProblemDetail (or
# ResponseEntity<ProblemDetail>) so the wire shape stays uniform.
#
# The IDW1 dogfood found a domain @ExceptionHandler returning a non-RFC9457
# body (a bare Map<String,String> / String / DTO) that slipped past all 49
# existing guards: nothing mechanically checked @ExceptionHandler return
# types. This guard closes that gap.
#
# What it scans:
#   backend/src/main/java/com/ax/template/authblueprint/**/*.java whose
#   filename ends in Controller.java, Advice.java, or ExceptionHandler.java.
#   (These are the file-naming conventions Spring controllers / advices use
#   in this codebase — verified against the live tree.)
#
# What it EXCLUDES:
#   - common/GlobalProblemDetailAdvice.java — the canonical RFC9457 advice
#     itself (it already returns ResponseEntity<ProblemDetail>, but it is the
#     authority and is explicitly out of scope so a future helper return type
#     on it can never be misread as a violation).
#   - test code — only src/main is walked; src/test is never scanned.
#
# What it requires of each @ExceptionHandler method:
#   the declared return type, normalized of whitespace, must be one of
#       ProblemDetail
#       ResponseEntity<ProblemDetail>
#   Anything else (Map<...>, ResponseEntity<Map<...>>, String, a bare DTO,
#   ResponseEntity<SomeDto>, void, …) is a violation.
#
# ZERO_SCAN safety: if the package dir exists but the walk finds zero
# @ExceptionHandler methods, that is a violation — a refactor that renamed
# every controller out of the naming convention would otherwise silently
# disable this guard.
#
# Exit codes:
#   0 — every in-scope @ExceptionHandler returns ProblemDetail / ResponseEntity<ProblemDetail>
#   1 — at least one returns a non-RFC9457 type, OR ZERO_SCAN
#   2 — usage / environment error (package dir missing, python3 missing)
#
# Usage:
#   bash practices/evals/controller_problemdetail_guard.sh
#   bash practices/evals/controller_problemdetail_guard.sh --root DIR
#
# Bash 3.2 compatible. Fast: pure file scan, no gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "controller_problemdetail_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

PKG_DIR="$REPO_ROOT/backend/src/main/java/com/ax/template/authblueprint"
if [ ! -d "$PKG_DIR" ]; then
    echo "controller_problemdetail_guard: package dir not found: $PKG_DIR" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "controller_problemdetail_guard: python3 not in PATH (required for java parsing)" >&2
    exit 2
fi

PKG_DIR="$PKG_DIR" python3 - <<'PY'
import os
import re
import sys

pkg_dir = os.environ["PKG_DIR"]

# Canonical advice — explicitly out of scope (it IS the RFC9457 authority).
CANONICAL_BASENAME = "GlobalProblemDetailAdvice.java"

# File-naming conventions for controllers / advices in this codebase.
def in_scope(filename: str) -> bool:
    return (
        filename.endswith("Controller.java")
        or filename.endswith("Advice.java")
        or filename.endswith("ExceptionHandler.java")
    )

# Accepted return types, after stripping ALL whitespace.
ALLOWED = {
    "ProblemDetail",
    "ResponseEntity<ProblemDetail>",
}

# Match an @ExceptionHandler annotation line (with or without args).
ANN_RE = re.compile(r"^\s*@ExceptionHandler\b")
# A line that is purely another annotation (e.g. @ResponseStatus(...)).
OTHER_ANN_RE = re.compile(r"^\s*@\w")
# Java method declaration: <modifiers> <return-type> name( ...
# Return type is everything between the modifier keywords and the method
# name+'('. We capture the segment before the last identifier-followed-by-'('.
MODIFIER_RE = re.compile(
    r"^\s*(?:public|protected|private|static|final|abstract|synchronized|default|<[^>]*>)\s+"
)
# Capture "<returntype> <methodname>(" — methodname is the identifier
# immediately preceding the first '('.
SIG_RE = re.compile(r"^\s*(.*?)\s+(\w+)\s*\(")


def extract_return_type(lines, start_idx):
    """
    Starting just after an @ExceptionHandler line, find the method signature
    line (skipping intervening annotations / blank lines) and return the
    whitespace-stripped return type, or None if it can't be resolved.
    """
    i = start_idx
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()
        if stripped == "" or stripped.startswith("//") or stripped.startswith("*") \
                or stripped.startswith("/*"):
            i += 1
            continue
        if OTHER_ANN_RE.match(line):
            i += 1
            continue
        # This should be the method declaration line.
        m = SIG_RE.match(line)
        if not m:
            return None
        prefix = m.group(1)  # "<modifiers...> <return-type>"
        # Strip leading modifier keywords + leading generic <T> declarations.
        # Repeatedly chop modifiers from the front.
        rest = prefix
        while True:
            mm = MODIFIER_RE.match(rest if rest.endswith(" ") else rest + " ")
            if mm:
                rest = rest[mm.end():].strip()
                continue
            break
        # rest is now the return type (may still contain spaces inside <...>).
        rt = re.sub(r"\s+", "", rest)
        return rt
    return None


violations = []
handler_count = 0
files_scanned = 0

for root, _dirs, files in os.walk(pkg_dir):
    for fn in sorted(files):
        if not fn.endswith(".java"):
            continue
        if not in_scope(fn):
            continue
        if fn == CANONICAL_BASENAME:
            continue
        path = os.path.join(root, fn)
        rel = os.path.relpath(path, os.path.dirname(pkg_dir.rstrip("/")))
        with open(path, encoding="utf-8") as fh:
            lines = fh.readlines()
        files_scanned += 1
        for idx, line in enumerate(lines):
            if not ANN_RE.match(line):
                continue
            handler_count += 1
            rt = extract_return_type(lines, idx + 1)
            lineno = idx + 1
            if rt is None:
                violations.append(
                    f"{path}:{lineno} — @ExceptionHandler method signature could "
                    f"not be parsed (cannot verify return type)"
                )
                continue
            if rt not in ALLOWED:
                violations.append(
                    f"{path}:{lineno} — @ExceptionHandler returns '{rt}', "
                    f"not ProblemDetail / ResponseEntity<ProblemDetail> (RFC 9457)"
                )

if handler_count == 0:
    print(
        "controller_problemdetail_guard: ZERO_SCAN — package dir exists but no "
        "@ExceptionHandler methods found in any *Controller/*Advice/"
        "*ExceptionHandler file. A naming-convention refactor must not silently "
        "disable this guard.",
        file=sys.stderr,
    )
    sys.exit(1)

if violations:
    print(
        "VIOLATION: domain @ExceptionHandler(s) return a non-RFC9457 error body "
        "(controller-problemdetail):",
        file=sys.stderr,
    )
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every @ExceptionHandler in a domain controller/advice must return "
        "org.springframework.http.ProblemDetail or "
        "ResponseEntity<ProblemDetail> (RFC 9457). Replace Map / String / bare "
        "DTO bodies with ProblemDetail.forStatus(...) — see "
        "common/GlobalProblemDetailAdvice for the canonical shape.",
        file=sys.stderr,
    )
    print(
        f"controller_problemdetail_guard: {len(violations)} violation(s) across "
        f"{files_scanned} file(s) ({handler_count} handler(s) scanned) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"controller_problemdetail_guard: all {handler_count} @ExceptionHandler "
    f"method(s) across {files_scanned} file(s) return ProblemDetail / "
    f"ResponseEntity<ProblemDetail> (RFC 9457)"
)
sys.exit(0)
PY
