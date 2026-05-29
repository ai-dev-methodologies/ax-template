#!/usr/bin/env bash
# practices/evals/consent_gate_guard.sh — IMW5 hard guard (consent-gated sharing).
#
# THE GAP THIS CLOSES
# -------------------
# IDW4 dogfood (2026-05-30, 병원 예약 + 진료기록 EMR-lite): consent-management-l0 was
# SPEC-ONLY — all three personas hand-rolled the full consent subsystem, and one
# shipped a data-SHARING path (third-party share / marketing send) that read a
# subject's records and forwarded them WITHOUT checking the purpose grant, while the
# build stayed fully GREEN. CONSENT-PURPOSE-001 ("purpose-gated operations MUST check
# the specific purpose grant, never a global consent flag") was enforced by NOTHING.
# This guard closes that hole, forward-enforcing.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# ONLY in a tree that has wired the common consent ledger (i.e. a class that declares
# the JPA `ConsentRecord` @Entity is present — the marker that this fork-receiver has
# adopted consent at all), every controller/service METHOD whose name OR request-mapping
# path implies data-SHARING MUST reference `ConsentGate` (the canonical
# common/ConsentGate purpose gate). A sharing method that does NOT gate on consent is a
# violation.
#
# "data-SHARING" is matched conservatively on an explicit verb set so it cannot
# false-positive on ordinary reads/writes (a bare "forward"/"send" is deliberately
# EXCLUDED — generic helper names across non-sharing domains):
#   * method name contains: share / disclose / export / thirdParty / toThirdParty /
#     syndicate / publishExternal
#   * OR a @GetMapping/@PostMapping/... path segment contains: /share / /disclose /
#     /export / /third-party / /syndicate
# A method qualifies as a candidate when it matches a sharing signal. It satisfies the
# rule when its body (or the method's own annotations) references `ConsentGate` (any
# of: ConsentGate.requireConsent / ConsentGate.activeConsent / a ConsentGate field call).
#
# KEY ON THE ConsentRecord ENTITY MARKER (load-bearing — forward-enforcing)
# ------------------------------------------------------------------------
# The candidate scan runs ONLY when the scanned tree contains a `ConsentRecord`
# @Entity. The live main tree ships common/ConsentRecord as an @Entity, BUT main has
# NO domain method matching a sharing signal, so the candidate set is empty → vacuous
# pass. (And a fork-receiver that has not adopted consent at all has no ConsentRecord
# entity → the guard is inert for them.) The guard therefore fires precisely when a
# fork-receiver BOTH adopts the consent ledger AND writes a sharing path — exactly when
# the CONSENT-PURPOSE-001 obligation attaches.
#
# Exit codes:
#   0 — every sharing method references ConsentGate (OR no ConsentRecord entity in the
#       tree OR no sharing method → vacuous pass).
#   1 — at least one sharing method does not gate on ConsentGate.
#   2 — usage / environment error (python3 missing, bad --root).
#
# Usage:
#   bash practices/evals/consent_gate_guard.sh                 # live backend tree
#   bash practices/evals/consent_gate_guard.sh --root DIR      # scan DIR/ instead
#   bash practices/evals/consent_gate_guard.sh --fixtures      # self-test fixtures
#   bash practices/evals/consent_gate_guard.sh --verbose
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
        *) echo "consent_gate_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v python3 >/dev/null 2>&1; then
    echo "consent_gate_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

# scan_tree <scan-dir> <verbose> → echoes result, returns guard exit code.
scan_tree() {
    local scan_dir="$1"
    local verbose="$2"
    if [ ! -d "$scan_dir" ]; then
        echo "consent_gate_guard: no scan dir at $scan_dir — nothing to check"
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
    print(f"consent_gate_guard: {scan_dir} has no *.java files — nothing to check")
    sys.exit(0)

# Strip // line and /* */ block comments so a commented-out sharing method or
# ConsentGate reference never counts. Block comments collapse to newlines so
# line numbers are preserved.
def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text

per_file_text = {}
for jf in java_files:
    try:
        raw = jf.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        print(f"consent_gate_guard: cannot read {jf}: {exc}", file=sys.stderr)
        sys.exit(2)
    per_file_text[jf] = strip_comments(raw)

# ── Adoption marker: a `ConsentRecord` @Entity must exist in the tree ──────────
# We look for an @Entity stereotype on (or just before) a `class ConsentRecord`
# declaration. The reference common/ConsentRecord is exactly this shape. Absent
# the marker the fork-receiver has not adopted the consent ledger → inert guard.
ENTITY_RE = re.compile(r"^[ \t]*@Entity\b", re.M)
CONSENT_ENTITY_DECL_RE = re.compile(
    r"\b(?:public\s+|final\s+|abstract\s+)*class\s+ConsentRecord\b"
)
has_consent_entity = False
for jf, text in per_file_text.items():
    if CONSENT_ENTITY_DECL_RE.search(text) and ENTITY_RE.search(text):
        has_consent_entity = True
        if verbose:
            print(f"  consent ledger adopted: ConsentRecord @Entity in {jf.as_posix()}")
        break

if not has_consent_entity:
    print("consent_gate_guard: PASS — no ConsentRecord @Entity in tree "
          f"({len(java_files)} file(s) scanned; consent not adopted, inert)")
    sys.exit(0)

# ── Sharing-signal matchers (conservative, explicit verb set) ──────────────────
# Method NAME implies data sharing. Matched against the method name token. The
# verb set is deliberately narrow + unambiguous: a bare "forward" / "send" is NOT
# included (those appear as generic helpers — event-forward, page-forward, mail-send
# — across non-sharing domains and would false-positive). Sharing intent must be
# explicit: share / disclose / export / syndicate / thirdParty / publishExternal.
NAME_SHARING_RE = re.compile(
    r"(?:share|disclose|export|syndicate|thirdParty|toThirdParty|publishExternal)",
    re.I,
)
# Request-mapping PATH implies sharing (a @GetMapping("/x/share") etc.). We scan the
# path string literal of any *Mapping annotation on the method.
MAPPING_RE = re.compile(
    r'@(?:Get|Post|Put|Patch|Delete|Request)Mapping\s*\(\s*(?:value\s*=\s*)?(?:\{\s*)?"([^"]*)"'
)
PATH_SHARING_RE = re.compile(
    r"/(?:share|disclose|export|third-party|thirdparty|syndicate)\b",
    re.I,
)

# A method head: optional annotations are scanned separately; here we capture the
# return-type + name + '(' so we can resolve the body span. We only treat methods
# inside a @RestController/@Controller/@Service class as candidates — but to stay
# robust we scan ALL methods and rely on the sharing signal to qualify.
METHOD_HEAD_RE = re.compile(
    r"(?:public|protected|private)\s+"            # an explicit access modifier
    r"(?:static\s+|final\s+|synchronized\s+)*"
    r"[A-Za-z_][A-Za-z0-9_<>,.\[\]\s?]*?\s+"        # return type (lazy)
    r"([a-z][A-Za-z0-9_]*)\s*\("                    # method name + '('
)

# ConsentGate reference — the canonical purpose gate. Accept the type-qualified
# static call, an instance-field call, or a bare mention of the type in the body.
GATE_RE = re.compile(r"\bConsentGate\b")

def method_body(text: str, open_paren_idx: int):
    n = len(text)
    depth = 0
    i = open_paren_idx
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
    while i < n and text[i] not in "{;":
        i += 1
    if i >= n or text[i] == ";":
        return None  # abstract / interface method — no body
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

def annotation_block_before(text: str, head_start: int) -> str:
    """Return the run of annotation lines immediately preceding a method head, so a
    @PostMapping("/x/share") on the line(s) above the method is associated with it."""
    # Walk back over preceding lines that are blank or start with '@' (after ws).
    lo = text.rfind("\n", 0, head_start)
    block_lines = []
    cur_end = head_start
    while lo >= 0:
        line = text[lo + 1:cur_end]
        stripped = line.strip()
        if stripped == "" or stripped.startswith("@"):
            block_lines.append(line)
            cur_end = lo
            lo = text.rfind("\n", 0, lo)
            continue
        break
    return "\n".join(reversed(block_lines))

violations = []   # (file_rel, lineno, method, why)
candidates = 0

for jf in java_files:
    text = per_file_text[jf]
    # Skip the common ConsentRecord/ConsentGate sources themselves: ConsentGate
    # legitimately defines requireConsent/activeConsent (names contain no sharing
    # verb) and references its own type; never a candidate.
    rel = jf.as_posix()
    for m in METHOD_HEAD_RE.finditer(text):
        name = m.group(1)
        body_span = method_body(text, m.end() - 1)
        if body_span is None:
            continue
        anno = annotation_block_before(text, m.start())
        # sharing signal?
        name_hit = bool(NAME_SHARING_RE.search(name))
        path_hit = False
        for mm in MAPPING_RE.finditer(anno):
            if PATH_SHARING_RE.search(mm.group(1)):
                path_hit = True
                break
        if not (name_hit or path_hit):
            continue
        candidates += 1
        lineno = text.count("\n", 0, m.start()) + 1
        body = text[body_span[0]:body_span[1]]
        # The gate may be referenced in the body OR in the method annotations
        # (e.g. a custom @RequiresConsent meta-annotation that wraps ConsentGate —
        # we accept a ConsentGate mention in either span).
        gated = bool(GATE_RE.search(body) or GATE_RE.search(anno))
        why = "name" if name_hit else "path"
        if gated:
            if verbose:
                print(f"  OK  {rel}:{lineno} → {name}(...) shares data ({why}) and gates on ConsentGate")
        else:
            violations.append((rel, lineno, name, why))

if violations:
    for rel, lineno, name, why in violations:
        print(
            f"VIOLATION [{rel}:{lineno}]: data-sharing method '{name}(...)' "
            f"(sharing signal: {why}) does not reference ConsentGate — "
            f"un-gated purpose sharing (consent-management-l0#CONSENT-PURPOSE-001).",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(
        "Fix policy: gate the sharing path on the specific purpose grant, e.g.\n"
        "  ConsentGate.requireConsent(subjectId, \"third_party_share\", ledger);\n"
        "BEFORE forwarding the data. The canonical gate is common/ConsentGate.",
        file=sys.stderr,
    )
    print(
        f"consent_gate_guard: {len(violations)} un-gated sharing method(s) of "
        f"{candidates} sharing candidate(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"consent_gate_guard: PASS — {candidates} data-sharing method(s) all reference "
    f"ConsentGate (ConsentRecord ledger adopted)"
)
sys.exit(0)
PYEOF
}

# ── --fixtures self-test mode ────────────────────────────────────────────────
if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIX="$SCRIPT_DIR/fixtures/consent_gate"
    rc=0
    echo "[consent_gate] pass/ (expect 0)"
    scan_tree "$FIX/pass" "$VERBOSE"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass/ exited $got, expected 0" >&2; rc=1; fi
    echo "[consent_gate] fail/ (expect 1)"
    out=$(scan_tree "$FIX/fail" "$VERBOSE" 2>&1); got=$?
    echo "$out"
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail/ exited $got, expected 1" >&2; rc=1; fi
    echo "[consent_gate] no_entity/ (expect 0 — consent not adopted, inert)"
    scan_tree "$FIX/no_entity" "$VERBOSE"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: no_entity/ exited $got, expected 0" >&2; rc=1; fi
    if [ "$rc" -eq 0 ]; then
        echo "consent_gate_guard --fixtures: PASS (pass→0, fail→1, no_entity→0)"
    else
        echo "consent_gate_guard --fixtures: FAIL" >&2
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
