#!/usr/bin/env bash
# practices/evals/phi_in_logs_guard.sh — IMW4 hard guard (no raw PHI in logs).
#
# THE GAP THIS CLOSES
# -------------------
# IDW4 dogfood (2026-05-30, 병원 예약 + 진료기록 EMR-lite): an adversarial probe
# shipped a `log.info(...)` statement that interpolated a patient's PHI getter
# directly into a log message — and the build stayed fully GREEN (54 guards +
# ArchUnit + tests). Raw PHI written to a log aggregator (ELK / Splunk /
# CloudWatch) is a HIPAA §164.312(b) audit-control breach and a permanent leak
# (logs are widely replicated and long-retained). NOTHING in the catalog caught
# it. This guard closes that hole.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# No SLF4J-style log statement may pass a @Phi getter as an argument. For every
#   log.{info,debug,warn,error,trace}( ... )
# call across the scanned tree, if any argument invokes a getter of a @Phi-tagged
# field (the getter set is derived from common/Phi.java tags only), it is a
# violation. The fix is to log a non-recoverable correlation token instead, e.g.
#   log.info("verb=PHI_READ patientHash={}", AuditPiiHelper.piiHash(id));
#
# KEY ON @Phi-TAGGED GETTERS ONLY (load-bearing — no name heuristics)
# -------------------------------------------------------------------
# The forbidden getter set is derived EXCLUSIVELY from explicit @Phi tags:
#   * a method annotated `@Phi ... <type> getX()` / `<type> x()`  → getter name
#   * a field `@Phi ... <type> diagnosis;`  → getter names getDiagnosis / diagnosis
#   * a record component `record V(@Phi String diagnosis)`        → accessor diagnosis()
# We never key on getter NAMES alone — getName / getEmail / getReason live in
# dozens of non-PHI domains and would false-positive. Because the live main tree
# has ZERO @Phi usage, the forbidden-getter set is empty and the guard exits 0:
# it is forward-enforcing, firing only once a fork-receiver tags real PHI.
#
# Exit codes:
#   0 — no log statement passes a @Phi getter (OR no @Phi usage → vacuous pass).
#   1 — at least one log statement interpolates a @Phi getter.
#   2 — usage / environment error (python3 missing, bad --root).
#
# Usage:
#   bash practices/evals/phi_in_logs_guard.sh                 # live backend tree
#   bash practices/evals/phi_in_logs_guard.sh --root DIR      # scan DIR/ instead
#   bash practices/evals/phi_in_logs_guard.sh --fixtures      # self-test fixtures
#   bash practices/evals/phi_in_logs_guard.sh --verbose
#
# Bash 3.2 compatible. Fast: pure file scan via python3, no gradle.
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
        *) echo "phi_in_logs_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v python3 >/dev/null 2>&1; then
    echo "phi_in_logs_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

scan_tree() {
    local scan_dir="$1"
    local verbose="$2"
    if [ ! -d "$scan_dir" ]; then
        echo "phi_in_logs_guard: no scan dir at $scan_dir — nothing to check"
        return 0
    fi
    python3 - "$scan_dir" "$verbose" <<'PYEOF'
import pathlib
import re
import sys

scan_dir = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"

java_files = sorted(scan_dir.rglob("*.java"))
if not java_files:
    print(f"phi_in_logs_guard: {scan_dir} has no *.java files — nothing to check")
    sys.exit(0)

def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text

per_file_text = {}
for jf in java_files:
    try:
        raw = jf.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        print(f"phi_in_logs_guard: cannot read {jf}: {exc}", file=sys.stderr)
        sys.exit(2)
    per_file_text[jf] = strip_comments(raw)

# ── Pass 1: derive the forbidden getter-name set from @Phi tags only ──────────
# Three @Phi placements each yield one or more accessor names:
#   (1) @Phi on a getter method:   @Phi ... <ret> getDiagnosis( )   → getDiagnosis
#                                   @Phi ... <ret> diagnosis( )       → diagnosis (record accessor)
#   (2) @Phi on a field:           @Phi ... <type> diagnosis ;       → getDiagnosis, diagnosis
#   (3) @Phi on a record comp.:    record V( @Phi <type> diagnosis ) → diagnosis (accessor)
# We inspect ONLY the construct immediately after @Phi (bounded to its nearest
# terminator) so a field @Phi never leaps over to a later getter method.
PHI_RE = re.compile(r"@Phi\b(?:\s*\([^)]*\))?")

def cap_getter(field: str) -> str:
    return "get" + field[0].upper() + field[1:]

# Within the construct immediately after @Phi, the LAST simple identifier before
# the construct's terminator is the member name. We bound the construct to the
# nearest terminator so a field @Phi never leaps over to a later getter:
#   terminator '('  → the construct is a method → it IS the getter name
#   terminator ';' or '=' → the construct is a field → getName + record accessor
#   terminator ',' or ')' → the construct is a record component → accessor name
# Other annotations between @Phi and the construct (e.g. @Column) are skipped
# because they end in their own ')' / are word-tokens we step past via the
# leading-modifier alternation below.
MEMBER_AT_TERMINATOR_RE = re.compile(
    r"^\s*"
    r"(?:(?:@[A-Za-z_][\w.]*(?:\s*\([^)]*\))?)\s*|"            # skip annotations
    r"(?:private|protected|public|final|static|transient|volatile|abstract|default)\s+)*"
    r"[A-Za-z_][A-Za-z0-9_<>\[\].,?\s]*?"                       # return/field type
    r"\b([a-z][A-Za-z0-9_]*)\s*"                                # the member name
    r"(?P<term>[(;=,)])"                                        # construct terminator
)

forbidden = set()       # getter / accessor method NAMES whose result is PHI
origin = {}             # name -> (file, kind)

for jf in java_files:
    text = per_file_text[jf]
    if "@Phi" not in text:
        continue
    for pm in PHI_RE.finditer(text):
        after = text[pm.end():]
        m = MEMBER_AT_TERMINATOR_RE.match(after)
        if not m:
            continue
        name = m.group(1)
        term = m.group("term")
        if term == "(":
            # @Phi on a method getter → the method name is the accessor.
            forbidden.add(name)
            origin.setdefault(name, (jf, "getter"))
        elif term in ";=":
            # @Phi on a field → both the JavaBean getter and the record accessor.
            forbidden.add(cap_getter(name))
            forbidden.add(name)
            origin.setdefault(cap_getter(name), (jf, "field"))
            origin.setdefault(name, (jf, "field"))
        else:  # ',' or ')'
            # @Phi on a record component → the accessor is the component name.
            forbidden.add(name)
            origin.setdefault(name, (jf, "record-component"))

if not forbidden:
    print("phi_in_logs_guard: PASS — no @Phi-tagged getters found "
          f"({len(java_files)} file(s) scanned; forward-enforcing, vacuous pass)")
    sys.exit(0)

forbidden_alt = "|".join(re.escape(n) for n in sorted(forbidden))
# A @Phi getter call: `.getDiagnosis(` or a bare `getDiagnosis(` / `diagnosis(`
# accessor invoked on something. Require a '(' to avoid matching a same-named
# field reference.
PHI_CALL_RE = re.compile(r"\b(?:" + forbidden_alt + r")\s*\(")

# ── Pass 2: scan log.{info,debug,warn,error,trace}( ... ) for PHI getters ─────
LOG_RE = re.compile(r"\blog\s*\.\s*(info|debug|warn|error|trace)\s*\(")

def call_args_span(text: str, open_idx: int):
    """Span of the argument list ( ... ) starting at open_idx (the '(')."""
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
                return (open_idx, i + 1)
        i += 1
    return (open_idx, n)

violations = []   # (file_rel, lineno, level, getter)

# A @Phi getter call is ACCEPTABLE inside a log statement only when it is the
# argument of an approved scrubber/hasher — that is the canonical fix
# (log a non-recoverable token, not raw PHI). Recognise the common/AuditPiiHelper
# entry points plus generic mask/redact/hash wrappers. We detect "wrapped" by a
# scrubber call-open `(` appearing immediately before the getter match (allowing
# whitespace), i.e. `piiHash(<...>getDiagnosis(`.
SCRUBBER_BEFORE_RE = re.compile(
    r"(?:piiHash|sanitizeReason|mask|redact|hash)\s*\(\s*[A-Za-z0-9_.]*$"
)

for jf in java_files:
    text = per_file_text[jf]
    rel = jf.as_posix()
    for lm in LOG_RE.finditer(text):
        level = lm.group(1)
        # the '(' is the last char of the match
        open_idx = lm.end() - 1
        span = call_args_span(text, open_idx)
        args = text[span[0]:span[1]]
        for cm in PHI_CALL_RE.finditer(args):
            # Is this getter the argument of an approved scrubber? Look at the
            # text immediately preceding the match inside the log args.
            before = args[:cm.start()]
            if SCRUBBER_BEFORE_RE.search(before):
                continue  # piiHash(getDiagnosis()) etc. — the canonical fix
            lineno = text.count("\n", 0, lm.start()) + 1
            getter = cm.group(0).rstrip("( ").strip()
            violations.append((rel, lineno, level, getter))
            break

if violations:
    for rel, lineno, level, getter in violations:
        print(
            f"VIOLATION [{rel}:{lineno}]: log.{level}(...) interpolates @Phi "
            f"getter '{getter}()' — raw PHI must never reach a log aggregator "
            f"(HIPAA §164.312(b)).",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(
        "Fix policy: never log a @Phi getter directly. Log a non-recoverable\n"
        "correlation token instead, e.g.\n"
        "  log.info(\"verb=PHI_READ patientHash={}\", AuditPiiHelper.piiHash(id));",
        file=sys.stderr,
    )
    print(
        f"phi_in_logs_guard: {len(violations)} raw-PHI-in-log statement(s) — "
        f"merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"phi_in_logs_guard: PASS — no log statement interpolates a @Phi getter "
    f"[@Phi getters: {forbidden_alt}]"
)
sys.exit(0)
PYEOF
}

if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIX="$SCRIPT_DIR/fixtures/phi_in_logs"
    rc=0
    echo "[phi_in_logs] pass/ (expect 0)"
    scan_tree "$FIX/pass" "$VERBOSE"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass/ exited $got, expected 0" >&2; rc=1; fi
    echo "[phi_in_logs] fail/ (expect 1)"
    out=$(scan_tree "$FIX/fail" "$VERBOSE" 2>&1); got=$?
    echo "$out"
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail/ exited $got, expected 1" >&2; rc=1; fi
    if [ "$rc" -eq 0 ]; then
        echo "phi_in_logs_guard --fixtures: PASS (pass→0, fail→1)"
    else
        echo "phi_in_logs_guard --fixtures: FAIL" >&2
    fi
    exit "$rc"
fi

if [ -n "$ROOT_OVERRIDE" ]; then
    SCAN_DIR="$ROOT_OVERRIDE"
else
    SCAN_DIR="$REPO_ROOT/backend/src/main"
fi
scan_tree "$SCAN_DIR" "$VERBOSE"
exit $?
