#!/usr/bin/env bash
# practices/evals/controller_repository_shell_guard.sh — IMW5 shell-layer guard.
#
# THE GAP THIS CLOSES (IDW4 dogfood 2026-05-30 — coverage ASYMMETRY)
# -----------------------------------------------------------------
# The layer-boundary ban "a *Controller MUST NOT touch a *Repository directly"
# (controllers route → services execute) is enforced ONLY by the JVM ArchUnit
# test backend/src/test/.../practices/ArchitectureLayerBoundaryTest.java
# (#practices_TEST_002_controllersDoNotDependOnRepositories). That test runs
# exclusively inside a full `./gradlew test` — none of the ~54 shell guards in
# run-all-guards.sh mirror it. So a Controller→Repository leak that an AI agent
# (or fork-receiver) introduces stays GREEN through the entire bash-guard sweep
# and surfaces only if a complete gradle test cycle happens to run. IDW4 named
# this the single largest shell-vs-JVM coverage asymmetry. This guard closes it
# by re-stating the ArchUnit rule at the shell level — fast, no gradle, runnable
# in every pre-commit / CI bash pass.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# Scan every *Controller.java under
#   backend/src/main/java/com/ax/template/authblueprint/**
# and FAIL when a controller couples to a *Repository in ANY of three shapes
# (matching the three ways a controller can reach a repository in Spring):
#   (1) FIELD       — a field whose declared type ends in `Repository`
#                       e.g.  private final OrderRepository orderRepository;
#   (2) CTOR PARAM  — a constructor parameter whose type ends in `Repository`
#                       e.g.  FooController(OrderRepository repo) { ... }
#   (3) METHOD CALL — a `.method(` invocation on a `*[Rr]epository` receiver
#                       e.g.  orderRepository.findById(id)
# Any one shape is a layer-boundary violation: the controller must delegate to a
# *Service, never to a repository.
#
# COMMENT STRIPPING (load-bearing — calibrated against the live tree)
# -------------------------------------------------------------------
# Comments are stripped before parsing. This is NOT cosmetic: the live tree's
# payment/PaymentAdminController.java javadoc literally says it has no
# "{@code *Repository}" dependency. Without comment stripping that prose would
# false-positive and the guard would (wrongly) fire on a clean tree. After
# stripping, the live tree has ZERO matches → the guard exits 0 (GREEN-on-current;
# IMW1-A already routed all 40 controllers through services).
#
# SUBSTRING TRAPS DELIBERATELY AVOIDED
# ------------------------------------
#   * `import a.b.c.OrderRepository;`  — an import alone is not a usage; field /
#     ctor-param / call shapes are what couple the classes. (An import without
#     any of the three shapes is dead and harmless; ArchUnit likewise keys on
#     real dependencies, not imports. We do NOT flag bare imports.)
#   * `// uses OrderRepository` or javadoc `{@code XRepository}` — comments are
#     removed first, so documentation never registers a violation.
#   * `RepositoryConfig` / `RepositoryFactoryBean` — the type-name matcher
#     requires the token to END at `Repository` (`\bRepository\b`), so a longer
#     identifier that merely STARTS with Repository is not matched as a repo type.
#
# Exit codes:
#   0 — no controller couples to a repository (field / ctor-param / call), OR no
#       backend controller tree exists (fork-receiver may be frontend-only).
#   1 — at least one Controller→Repository coupling (each printed: file:line + shape).
#   2 — usage / environment error (paths missing, python3 missing).
#
# Usage:
#   bash practices/evals/controller_repository_shell_guard.sh
#   bash practices/evals/controller_repository_shell_guard.sh --root DIR
#   bash practices/evals/controller_repository_shell_guard.sh --fixtures
#   bash practices/evals/controller_repository_shell_guard.sh --verbose
#
# Bash 3.2 compatible. Fast: pure file scan via python3 (a repo dependency). No gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ROOT_OVERRIDE=""
RUN_FIXTURES=0
VERBOSE=0

while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --fixtures) RUN_FIXTURES=1; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "controller_repository_shell_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v python3 >/dev/null 2>&1; then
    echo "controller_repository_shell_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

# scan_tree <scan-dir> <verbose> → echoes result, returns guard exit code.
# <scan-dir> is the directory under which *Controller.java files are walked.
scan_tree() {
    local scan_dir="$1"
    local verbose="$2"
    if [ ! -d "$scan_dir" ]; then
        # No tree to scan → nothing to check (fork-receiver may be frontend-only
        # or as-yet-unpopulated). Not an error.
        echo "controller_repository_shell_guard: no scan dir at $scan_dir — nothing to check"
        return 0
    fi
    python3 - "$scan_dir" "$verbose" <<'PYEOF'
import pathlib
import re
import sys

scan_dir = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"

# Only *Controller.java files participate — those are the layer whose
# repository coupling the ArchUnit rule bans. (A *Service touching a repository
# is correct layering and out of scope.)
controller_files = sorted(
    p for p in scan_dir.rglob("*.java") if p.name.endswith("Controller.java")
)
if not controller_files:
    print(f"controller_repository_shell_guard: {scan_dir} has no *Controller.java "
          "files — nothing to check")
    sys.exit(0)


# Strip /* ... */ block comments and // line comments BEFORE parsing so that a
# javadoc such as PaymentAdminController's "no {@code *Repository} dependency"
# prose never registers a phantom coupling. We preserve newlines from block
# comments so reported line numbers stay accurate.
BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.S)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")


def strip_comments(text: str) -> str:
    text = BLOCK_COMMENT_RE.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    text = LINE_COMMENT_RE.sub("", text)
    return text


# A repository TYPE token: an UpperCamel identifier that ends at `Repository`
# (whole-word). `\b` after Repository excludes RepositoryConfig /
# RepositoryFactoryBean (longer identifiers that merely start with Repository).
REPO_TYPE = r"[A-Z][A-Za-z0-9_]*Repository\b"

# (1) FIELD: a field declaration whose declared type is a *Repository. Anchored
#     at line start (after optional whitespace + optional modifiers) so a method
#     RETURN type `OrderRepository foo()` is not mistaken for a field (that head
#     is followed by `(`, which this pattern forbids by requiring an identifier
#     then `;` / `=`).
#       private final OrderRepository orderRepository;
#       OrderRepository repo = ...;
FIELD_RE = re.compile(
    r"^[ \t]*(?:@\w+[ \t]+)*"
    r"(?:private|protected|public|final|static|transient|volatile)[ \t]+"
    r"(?:(?:private|protected|public|final|static|transient|volatile)[ \t]+)*"
    r"(?P<type>" + REPO_TYPE + r")[ \t]+[a-z]\w*[ \t]*[;=]",
    re.M,
)

# (2) CONSTRUCTOR PARAM: a *Repository typed parameter inside the parameter list
#     of a constructor (the class's own simple name immediately followed by `(`).
#     We locate each `<ClassName>(` head and scan its parameter list for a repo
#     type. Constructor injection is the canonical Spring wiring shape.
CTOR_HEAD_RE = re.compile(
    r"(?:public|protected|private)?[ \t]*"
    r"(?P<cls>[A-Z]\w*Controller)[ \t]*\("
)
# A repo-typed parameter inside a parameter list: `OrderRepository name` (the
# parameter list has no `;`, so a plain type+identifier is unambiguous here).
PARAM_REPO_RE = re.compile(r"\b(?P<type>" + REPO_TYPE + r")[ \t]+[a-zA-Z_]\w*")

# (3) METHOD CALL on a repository receiver: `someRepository.method(` — a lower-
#     camel identifier ending in `Repository` (or `repository`) followed by `.`
#     then an identifier and `(`. Catches `orderRepository.findById(id)`.
CALL_RE = re.compile(
    r"\b(?P<recv>[a-z]\w*[Rr]epository)[ \t]*\.[ \t]*(?P<method>[a-z]\w*)[ \t]*\(",
    re.M,
)


def lineno_at(text: str, idx: int) -> int:
    return text.count("\n", 0, idx) + 1


def matching_paren(text: str, open_idx: int):
    """Given the index of a '(', return the index just AFTER its matching ')'."""
    depth = 0
    i = open_idx
    n = len(text)
    while i < n:
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


violations = []   # (file_rel, lineno, shape, detail)
scanned = 0

for cf in controller_files:
    scanned += 1
    try:
        raw = cf.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        print(f"controller_repository_shell_guard: cannot read {cf}: {exc}",
              file=sys.stderr)
        sys.exit(2)
    text = strip_comments(raw)
    rel = cf.as_posix()

    # (1) repository-typed FIELDS
    for m in FIELD_RE.finditer(text):
        violations.append(
            (rel, lineno_at(text, m.start()), "field",
             f"field of type {m.group('type')}")
        )

    # (2) repository-typed CONSTRUCTOR PARAMETERS
    for hm in CTOR_HEAD_RE.finditer(text):
        open_idx = text.index("(", hm.start())
        close_idx = matching_paren(text, open_idx)
        params = text[open_idx + 1:close_idx - 1]
        for pm in PARAM_REPO_RE.finditer(params):
            violations.append(
                (rel, lineno_at(text, open_idx),
                 "constructor-param",
                 f"{hm.group('cls')}(... {pm.group('type')} ...)")
            )

    # (3) METHOD CALLS on a repository receiver
    for m in CALL_RE.finditer(text):
        violations.append(
            (rel, lineno_at(text, m.start()), "method-call",
             f"{m.group('recv')}.{m.group('method')}(...)")
        )

    if verbose and not any(v[0] == rel for v in violations):
        print(f"  OK  {rel} — no repository coupling")

if violations:
    print(
        "VIOLATION: *Controller couples to a *Repository — layer-boundary "
        "break (controllers MUST route through a *Service). This mirrors "
        "ArchitectureLayerBoundaryTest"
        "#practices_TEST_002_controllersDoNotDependOnRepositories at the "
        "shell level (IMW5 — closes the IDW4 shell-vs-JVM coverage asymmetry):",
        file=sys.stderr,
    )
    for rel, lineno, shape, detail in sorted(violations):
        print(f"  [{shape}] {rel}:{lineno} — {detail}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Fix policy: move the repository access into the domain *Service and "
        "have the controller inject/call the service instead. A thin "
        "@RestController delegates to a service ONLY — see "
        "docs/NEW-DOMAIN-CHECKLIST.md §1.4.",
        file=sys.stderr,
    )
    print(
        f"controller_repository_shell_guard: {len(violations)} "
        f"Controller→Repository coupling(s) across {scanned} controller "
        "file(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"controller_repository_shell_guard: PASS — {scanned} *Controller.java "
    "file(s), none couples to a *Repository (field / ctor-param / call)"
)
sys.exit(0)
PYEOF
}

# ── --fixtures self-test mode ────────────────────────────────────────────────
if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIX="$SCRIPT_DIR/fixtures/controller_repository_shell"
    rc=0
    echo "[controller_repository_shell] pass/ (expect 0)"
    scan_tree "$FIX/pass" "$VERBOSE"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass/ exited $got, expected 0" >&2; rc=1; fi
    echo "[controller_repository_shell] fail_repo_injection/ (expect 1)"
    out=$(scan_tree "$FIX/fail_repo_injection" "$VERBOSE" 2>&1); got=$?
    echo "$out"
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail_repo_injection/ exited $got, expected 1" >&2; rc=1; fi
    if [ "$rc" -eq 0 ]; then
        echo "controller_repository_shell_guard --fixtures: PASS (pass→0, fail→1)"
    else
        echo "controller_repository_shell_guard --fixtures: FAIL" >&2
    fi
    exit "$rc"
fi

# ── live / --root mode ───────────────────────────────────────────────────────
if [ -n "$ROOT_OVERRIDE" ]; then
    SCAN_DIR="$ROOT_OVERRIDE"
else
    SCAN_DIR="$REPO_ROOT/backend/src/main/java/com/ax/template/authblueprint"
fi
scan_tree "$SCAN_DIR" "$VERBOSE"
exit $?
