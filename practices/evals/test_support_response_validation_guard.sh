#!/usr/bin/env bash
# practices/evals/test_support_response_validation_guard.sh — BACKLOG P2-117.
#
# THE INVARIANT: a `backend/src/test/**/*TestSupport.java` helper MUST NOT read a value out of
# an HTTP response it has not described. Every value read goes through
# `com.ax.template.authblueprint.common.HttpExtract`, which asserts the status, then the
# presence of a Content-Type, then the JSON family, then the extraction, then the value's
# presence — and reports status / content-type / every header / a bounded body excerpt /
# `RestAssured.port` on failure.
#
# WHY THIS SHAPE. P2-116 measured the four ways a blind `.then().extract().path("field")` fails
# against this tree's exact rest-assured: no Content-Type → an IllegalStateException naming
# nothing (the P3-144 preimage); 200 + empty body → a parse error naming nothing; 401 +
# problem+json → NO exception at all, just a null carried forward as a token; port 0 → a throw
# before any response exists. P2-117's census found that shape in 86 `*TestSupport.java` files.
# Moving all 86 onto the helper does not stop the 87th: new domains are normal activity here, and
# each new `*TestSupport.java` is written by copying a neighbour. So the recipe has to be
# enforced structurally, not just applied once.
#
# TWO FORBIDDEN SHAPES — both are "pull a value out", and neither has an exception:
#
#   (A) an `.extract()` whose next chained call is anything other than `.response()`.
#       `.extract().response()` is the sanctioned handoff: it reads NO value, it hands the whole
#       `Response` object onward — to `HttpExtract` in the same method, or to a caller that
#       validates it. A TERMINAL `.extract()` (the chain ends there, e.g.
#       `return given()...then().extract();`) is the same handoff in `ExtractableResponse` form
#       and is likewise allowed — it too reads no value. Everything else (`.extract().path(...)`,
#       `.extract().jsonPath()`, `.extract().as(...)`, `.extract().asString()`,
#       `.extract().header(...)`) pulls a value out of a response nobody described.
#
#   (B) a `.path(` / `.pathAt(` / `.jsonPath(` call whose qualifier is not `HttpExtract`.
#       This closes the two-statement evasion of (A), which no regex on `.extract().path` would
#       ever see:
#           var r = given()...then().extract();      // legal under (A) — reads nothing
#           return r.path("accessToken");            // ← caught here
#       and equally the no-`.extract()`-at-all form `Response r = when().post(...); r.path("x")`.
#
# `HttpExtract.java` calls `response.path(jsonPath)` itself, and is NOT scanned. That is not an
# exception to the rule — it is the distinction between the SUBJECT and the OBJECT of the rule.
# The helper is the one place where the validated extraction is implemented; it is excluded by
# construction (its name does not end in `TestSupport.java`), not by a name on a list.
#
# THERE IS NO ALLOWLIST, AND MUST NOT BE ONE. An exception list is how a guard goes quietly
# vacuous: the file that most needs the rule is the file most likely to be excused from it. If
# this rule ever needs an exception, the rule is wrong and should be re-derived, not annotated.
# The target set is derived from disk on every run (`find ... -name '*TestSupport.java'`); no
# count is hardcoded, so a new domain is inside the rule the moment its file lands.
#
# DELIBERATELY NOT DONE:
#   • Only `*TestSupport.java` is scanned — NOT general test classes. This is a real limit and it
#     is not hidden. The 2026-08-16 census: 221 files call `extract()`, but only 86 had no status
#     assertion anywhere, and that 86 was exactly the `*TestSupport.java` blind set — the rest
#     carry `statusCode(...)` in the same chain, so they are already described. Widening to every
#     test class would flag those 135 and buy nothing. The residue this leaves is explicit: a
#     TestSupport may legally hand an `ExtractableResponse` to a caller that then reads it
#     blindly. That read happens in a Compliance test, outside this guard's axis.
#   • No status-assertion parsing. "Is there a `statusCode(...)` somewhere in this Java call
#     chain" is a fragile parse (chains span lines, are built in fragments, and are sometimes
#     asserted by the caller). This guard asks a purely structural question instead — did the
#     value come out through the helper — which has one answer per call site.
#   • No check that the `context` argument is informative. A string's usefulness is a review
#     judgement, not a binary.
#   • No PyYAML dependency (nothing here is YAML). python3 only — invoked from a top-level temp
#     file rather than a heredoc nested in `$(...)`, per P2-78 (stock bash 3.2 mis-parses that
#     nesting when the body's apostrophe count is odd).
#
# Exit: 0 PASS · 1 a *TestSupport.java reads a value from an unvalidated response · 2 usage /
#       environment error.
#
# Usage:
#   bash practices/evals/test_support_response_validation_guard.sh
#   bash practices/evals/test_support_response_validation_guard.sh --root DIR   # fixture tree
#     (DIR is scanned at DIR/backend/src/test)
#
# Registered in practices/evals/run-all-guards.sh as guard [116].

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT="$REPO_ROOT"
while [ $# -gt 0 ]; do
    case "$1" in
        # `shift 2` with only one arg left is a no-op that returns non-zero, and without set -e
        # that spins this loop forever on the same token — so the arity is checked, not assumed.
        --root)
            if [ $# -lt 2 ]; then
                echo "test_support_response_validation_guard: --root requires a directory argument" >&2
                exit 2
            fi
            ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "test_support_response_validation_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ -z "$ROOT" ] || [ ! -d "$ROOT" ]; then
    echo "test_support_response_validation_guard: --root must name an existing directory (got '${ROOT}')" >&2
    exit 2
fi

TEST_ROOT="$ROOT/backend/src/test"
if [ ! -d "$TEST_ROOT" ]; then
    echo "test_support_response_validation_guard: $TEST_ROOT not found — nothing to check"
    exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "test_support_response_validation_guard: python3 not in PATH (required for scanning)" >&2
    exit 2
fi

# P2-78: write the python body to a top-level temp file and invoke it by path, rather than
# nesting a quoted heredoc inside $(...) — bash 3.2.57 mis-parses that nesting whenever the
# body's apostrophe count is odd, and a one-word comment edit flips that parity.
SCAN_PY="$(mktemp "${TMPDIR:-/tmp}/test_support_response_validation.XXXXXX")"
trap 'rm -f "$SCAN_PY"' EXIT

cat <<'PY' > "$SCAN_PY"
import os
import re
import sys

root, test_root = sys.argv[1], sys.argv[2]


def blank_comments(src):
    """Replace every comment character with a space, preserving length and newlines.

    Offsets and line numbers therefore stay identical to the file on disk, and a javadoc that
    QUOTES the forbidden shape (ApprovalWorkflowTestSupport does exactly that, describing the
    pattern it was moved off) cannot be reported as code. String and char literals are tracked
    so that a "//" inside a literal does not open a comment.
    """
    out = list(src)
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == '\\':
                    i += 1
                if i < n and src[i] == '\n':
                    break
                i += 1
            i += 1
        elif c == "'":
            i += 1
            while i < n and src[i] != "'":
                if src[i] == '\\':
                    i += 1
                i += 1
            i += 1
        elif c == '/' and i + 1 < n and src[i + 1] == '/':
            while i < n and src[i] != '\n':
                out[i] = ' '
                i += 1
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            while i < n and not (src[i] == '*' and i + 1 < n and src[i + 1] == '/'):
                if src[i] != '\n':
                    out[i] = ' '
                i += 1
            for j in range(i, min(i + 2, n)):
                out[j] = ' '
            i += 2
        else:
            i += 1
    return ''.join(out)


def line_starts(text):
    starts, pos = [0], text.find('\n')
    while pos != -1:
        starts.append(pos + 1)
        pos = text.find('\n', pos + 1)
    return starts


def line_of(starts, offset):
    lo, hi = 0, len(starts) - 1
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if starts[mid] <= offset:
            lo = mid
        else:
            hi = mid - 1
    return lo + 1


EXTRACT = re.compile(r'\.\s*extract\s*\(\s*\)')
READER = re.compile(r'\.\s*(path|pathAt|jsonPath)\s*\(')
IDENT = re.compile(r'[A-Za-z_]\w*')
QUALIFIER = re.compile(r'([A-Za-z_]\w*)\s*$')
JSONPATH_ARG = re.compile(r'\(\s*"([^"]*)"')

files = []
for dirpath, _dirnames, filenames in os.walk(test_root):
    for name in filenames:
        if name.endswith('TestSupport.java'):
            files.append(os.path.join(dirpath, name))
files.sort()

violations = []

for path in files:
    try:
        with open(path, encoding='utf-8') as fh:
            src = fh.read()
    except OSError as exc:
        print('test_support_response_validation_guard: cannot read %s (%s)' % (path, exc),
              file=sys.stderr)
        sys.exit(2)

    text = blank_comments(src)
    starts = line_starts(text)
    raw_lines = src.split('\n')
    rel = os.path.relpath(path, root)
    reported_by_a = set()

    # (A) `.extract()` followed by a chained call that is not `.response()`.
    for m in EXTRACT.finditer(text):
        j = m.end()
        while j < len(text) and text[j].isspace():
            j += 1
        if j >= len(text) or text[j] != '.':
            continue  # terminal .extract() — hands the whole response on, reads nothing
        k = j + 1
        while k < len(text) and text[k].isspace():
            k += 1
        idm = IDENT.match(text, k)
        follower = idm.group(0) if idm else '<unparsed>'
        if follower == 'response':
            continue  # the sanctioned handoff
        line = line_of(starts, m.start())
        reported_by_a.add(line)
        arg = JSONPATH_ARG.match(text, idm.end() if idm else k)
        violations.append((rel, line, raw_lines[line - 1],
                           ".extract().%s(...) reads a value out of a response nobody described"
                           % follower,
                           arg.group(1) if arg else 'jsonPath'))

    # (B) a value read whose qualifier is not HttpExtract (closes the two-statement evasion).
    for m in READER.finditer(text):
        pre = text[:m.start()]
        qm = QUALIFIER.search(pre)
        if qm and qm.group(1) == 'HttpExtract':
            continue
        line = line_of(starts, m.start())
        if line in reported_by_a:
            continue  # same call site, already named by (A)
        arg = JSONPATH_ARG.match(text, m.end() - 1)
        qual = qm.group(1) if qm else '<expr>'
        violations.append((rel, line, raw_lines[line - 1],
                           "%s.%s(...) reads a value straight off a response object — "
                           "it never passed through HttpExtract" % (qual, m.group(1)),
                           arg.group(1) if arg else 'jsonPath'))

violations.sort(key=lambda v: (v[0], v[1]))

for rel, line, raw, why, jsonpath in violations:
    print('VIOLATION: %s:%d — %s' % (rel, line, why), file=sys.stderr)
    print('    %s' % raw.strip(), file=sys.stderr)
    print('    fix: hand the whole response over, then read it through the helper —', file=sys.stderr)
    print('         Response resp = given()...when()...then().extract().response();', file=sys.stderr)
    print('         HttpExtract.path(resp, "%s", "<METHOD /path> (<what this call is for>)");'
          % jsonpath, file=sys.stderr)
    print('         (use HttpExtract.pathAt(resp, <status>, ...) to pin a non-2xx status)',
          file=sys.stderr)

print('%d %d' % (len(files), len(violations)))
PY

scan_out="$(python3 "$SCAN_PY" "$ROOT" "$TEST_ROOT")"
scan_rc=$?
if [ "$scan_rc" -ne 0 ]; then
    echo "test_support_response_validation_guard: scan failed (rc=$scan_rc)" >&2
    exit 2
fi

checked="${scan_out%% *}"
violations="${scan_out##* }"

if [ -z "$checked" ] || [ -z "$violations" ]; then
    echo "test_support_response_validation_guard: scanner produced no verdict" >&2
    exit 2
fi

if [ "$checked" -eq 0 ]; then
    echo "test_support_response_validation_guard: no *TestSupport.java under $TEST_ROOT — nothing to check"
    exit 0
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "test_support_response_validation_guard: $violations unvalidated response read(s) across $checked *TestSupport.java file(s) — BLOCKED" >&2
    echo "  Reading a value from a response that was never described turns every infrastructural" >&2
    echo "  failure into an exception that names nothing (P2-116/P2-117), and a 4xx JSON error body" >&2
    echo "  into a silent null carried forward as if it were the value." >&2
    exit 1
fi

echo "test_support_response_validation_guard: PASS — all $checked *TestSupport.java read responses through HttpExtract"
exit 0
