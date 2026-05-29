#!/usr/bin/env bash
# practices/evals/audit_on_read_guard.sh — IMW4 hard guard (PHI audit-on-read).
#
# THE GAP THIS CLOSES
# -------------------
# IDW4 dogfood (2026-05-30, 병원 예약 + 진료기록 EMR-lite): an adversarial probe
# shipped a service method that READ protected health information and returned it
# to the caller WITHOUT recording an audit entry — and the build stayed fully
# GREEN (54 guards + ArchUnit + tests). HIPAA §164.312(b) "audit controls"
# requires that access to PHI be recorded; NOTHING in the catalog enforced it.
# This guard closes that hole.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# A read method that EXPOSES @Phi-tagged data MUST record an audit entry on that
# path. Concretely, for every method in a backend @Service (or any class) whose
# RETURN TYPE is — or wraps (Optional<T>, List<T>, Page<T>, Set<T>, Collection<T>,
# T[]) — a type that carries a @Phi-tagged member, the method body MUST reference
# `AuditLogService.record` (the canonical sole-mutator audit sink). A read path
# that returns PHI without that reference is a violation.
#
# KEY ON @Phi ONLY (load-bearing — no name heuristics)
# ----------------------------------------------------
# The PHI type set is derived EXCLUSIVELY from the explicit common/Phi.java tag
# (@Phi on a field, a getter, a record component, or a whole type). We never key
# on member NAMES (getName / getDiagnosis / …) — a name heuristic would
# false-positive across the whole reference workload (getName/getEmail/getReason
# live in dozens of non-PHI domains). Because the live main tree has ZERO @Phi
# usage, the PHI type set is empty, no method is a candidate, and the guard exits
# 0 — it is forward-enforcing: it fires only once a fork-receiver tags real PHI.
#
# WRITE methods are NOT candidates: a method is a read candidate only when its
# RETURN type exposes PHI. A void / id-returning mutator that takes PHI as input
# is out of scope here (that is the phi_in_logs_guard / sanitize surface).
#
# Exit codes:
#   0 — every PHI-returning read method references AuditLogService.record
#       (OR the tree has no @Phi usage at all → vacuous pass).
#   1 — at least one PHI-returning read method has no audit-on-read evidence.
#   2 — usage / environment error (python3 missing, bad --root).
#
# Usage:
#   bash practices/evals/audit_on_read_guard.sh                 # live backend tree
#   bash practices/evals/audit_on_read_guard.sh --root DIR      # scan DIR/ instead
#   bash practices/evals/audit_on_read_guard.sh --fixtures      # self-test fixtures
#   bash practices/evals/audit_on_read_guard.sh --verbose
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
        *) echo "audit_on_read_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v python3 >/dev/null 2>&1; then
    echo "audit_on_read_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

# scan_tree <scan-dir> <verbose> → echoes result, returns guard exit code.
# <scan-dir> is the directory under which *.java files are walked.
scan_tree() {
    local scan_dir="$1"
    local verbose="$2"
    if [ ! -d "$scan_dir" ]; then
        # No tree to scan → nothing to check (fork-receiver may be frontend-only
        # or as-yet-unpopulated). Not an error.
        echo "audit_on_read_guard: no scan dir at $scan_dir — nothing to check"
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
    print(f"audit_on_read_guard: {scan_dir} has no *.java files — nothing to check")
    sys.exit(0)

# Strip // line and /* */ block comments so a commented-out @Phi or
# AuditLogService.record never counts.
def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text

PHI_RE = re.compile(r"@Phi\b")
# Top-level (or nested) type declaration: capture the simple name of any
# class / interface / record / enum so we can map a @Phi-bearing type to its name.
TYPE_DECL_RE = re.compile(
    r"\b(?:public\s+|final\s+|abstract\s+|static\s+|sealed\s+|non-sealed\s+)*"
    r"(?:class|interface|record|enum)\s+([A-Z][A-Za-z0-9_]*)"
)

# ── Pass 1: collect the set of type NAMES that carry a @Phi-tagged member ──────
# A type carries PHI if anywhere inside its source file body there is an @Phi
# annotation (on a field, getter, record component, or the type itself). Because
# the reference convention is one public type per file plus small nested helpers,
# associating any in-file @Phi with EVERY type declared in that file is a safe
# over-approximation (it can only make the guard stricter, never miss a leak).
phi_types = set()           # simple type names that expose PHI
phi_type_origin = {}        # name -> file (for verbose)

per_file_text = {}          # path -> comment-stripped text (reused in pass 2)

for jf in java_files:
    try:
        raw = jf.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        print(f"audit_on_read_guard: cannot read {jf}: {exc}", file=sys.stderr)
        sys.exit(2)
    text = strip_comments(raw)
    per_file_text[jf] = text
    if not PHI_RE.search(text):
        continue
    for tm in TYPE_DECL_RE.finditer(text):
        name = tm.group(1)
        phi_types.add(name)
        phi_type_origin.setdefault(name, jf)

if not phi_types:
    print("audit_on_read_guard: PASS — no @Phi-tagged types found "
          f"({len(java_files)} file(s) scanned; forward-enforcing, vacuous pass)")
    sys.exit(0)

# Build an alternation that matches any PHI type name as a whole word.
phi_alt = "|".join(re.escape(n) for n in sorted(phi_types))
# Read method = method whose return type IS, or wraps, a PHI type. We match the
# return-type token immediately before the method name. Wrappers recognised:
# Optional / List / Set / Collection / Page / Slice / Iterable / Stream / array.
WRAP = r"(?:Optional|List|Set|Collection|Page|Slice|Iterable|Stream)"
# return type forms:
#   <PhiType> name(            e.g.  EncounterView find(
#   <Wrap><...PhiType...> name( e.g.  List<EncounterView> findAll(
#   <PhiType>[] name(          e.g.  EncounterView[] dump(
RET_RE = re.compile(
    r"(?P<ret>"
    r"(?:" + WRAP + r"\s*<[^>]*\b(?:" + phi_alt + r")\b[^>]*>"   # wrapped generic
    r"|\b(?:" + phi_alt + r")\b\s*(?:\[\s*\])?)"                  # bare or array
    r")"
    r"\s+(?P<name>[a-z][A-Za-z0-9_]*)\s*\("
)

# We require the canonical sink reference. Two accepted forms:
#   AuditLogService.record(...)            (static-style / type-qualified)
#   <field>.record(...) where the field name contains 'audit' (instance call) —
#   matched conservatively as `<...audit...>.record(`.
RECORD_TYPE_RE = re.compile(r"AuditLogService\s*\.\s*record\s*\(")
RECORD_FIELD_RE = re.compile(r"\b\w*[Aa]udit\w*\s*\.\s*record\s*\(")

def method_body(text: str, open_paren_idx: int):
    """Return (start,end) source span of the method body { ... } following the
    parameter list that opens at open_paren_idx, or None if not found."""
    # Find the ')' that closes the param list, then the first '{'.
    depth = 0
    i = open_paren_idx
    n = len(text)
    # advance to matching close paren
    while i < n:
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                i += 1
                break
        i += 1
    # skip whitespace / throws clause up to '{' or ';'
    while i < n and text[i] not in "{;":
        i += 1
    if i >= n or text[i] == ";":
        return None  # abstract / interface method — no body to audit
    # now at '{' — find matching close brace
    bdepth = 0
    start = i
    while i < n:
        c = text[i]
        if c == "{":
            bdepth += 1
        elif c == "}":
            bdepth -= 1
            if bdepth == 0:
                return (start, i + 1)
        i += 1
    return (start, n)

violations = []   # (file_rel, lineno, method, ret)
candidates = 0

for jf in java_files:
    text = per_file_text[jf]
    rel = jf.as_posix()
    for m in RET_RE.finditer(text):
        # Skip a 'new <PhiType>(' constructor call or generic var decl that
        # happens to look like a method head: require the '(' to start a
        # parameter list at type/method scope. The simplest robust filter is to
        # require the body resolver to find a real '{ ... }' block.
        body_span = method_body(text, m.end() - 1)
        if body_span is None:
            continue
        candidates += 1
        lineno = text.count("\n", 0, m.start()) + 1
        body = text[body_span[0]:body_span[1]]
        has_record = bool(RECORD_TYPE_RE.search(body) or RECORD_FIELD_RE.search(body))
        if has_record:
            if verbose:
                print(f"  OK  {rel}:{lineno} → {m.group('name')}(...) returns "
                      f"PHI and records audit")
        else:
            violations.append((rel, lineno, m.group("name"), m.group("ret").strip()))

if violations:
    for rel, lineno, name, ret in violations:
        print(
            f"VIOLATION [{rel}:{lineno}]: read method '{name}(...)' returns "
            f"@Phi-tagged data ('{ret}') but does not reference "
            f"AuditLogService.record on that path — un-audited PHI read "
            f"(HIPAA §164.312(b)).",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(
        "Fix policy: record an audit entry on the PHI-read path, e.g.\n"
        "  auditLogService.record(AuditLog.of(\"PHI_READ\", actorId, resourceId));\n"
        "BEFORE returning the @Phi-tagged view. The canonical sink is "
        "auditlog/AuditLogService.record(...).",
        file=sys.stderr,
    )
    print(
        f"audit_on_read_guard: {len(violations)} un-audited PHI read(s) of "
        f"{candidates} PHI-returning candidate method(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"audit_on_read_guard: PASS — {candidates} PHI-returning read method(s) all "
    f"reference AuditLogService.record [PHI types: {phi_alt}]"
)
sys.exit(0)
PYEOF
}

# ── --fixtures self-test mode ────────────────────────────────────────────────
if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIX="$SCRIPT_DIR/fixtures/audit_on_read"
    rc=0
    echo "[audit_on_read] pass/ (expect 0)"
    scan_tree "$FIX/pass" "$VERBOSE"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass/ exited $got, expected 0" >&2; rc=1; fi
    echo "[audit_on_read] fail/ (expect 1)"
    out=$(scan_tree "$FIX/fail" "$VERBOSE" 2>&1); got=$?
    echo "$out"
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail/ exited $got, expected 1" >&2; rc=1; fi
    if [ "$rc" -eq 0 ]; then
        echo "audit_on_read_guard --fixtures: PASS (pass→0, fail→1)"
    else
        echo "audit_on_read_guard --fixtures: FAIL" >&2
    fi
    exit "$rc"
fi

# ── live / --root mode ───────────────────────────────────────────────────────
if [ -n "$ROOT_OVERRIDE" ]; then
    SCAN_DIR="$ROOT_OVERRIDE"
else
    SCAN_DIR="$REPO_ROOT/backend/src/main"
fi
scan_tree "$SCAN_DIR" "$VERBOSE"
exit $?
