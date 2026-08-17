#!/usr/bin/env bash
# practices/evals/test_support_response_validation_guard.sh — BACKLOG P2-117 + P2-119.
#
# THE INVARIANT: nothing under `backend/src/test/**` may read a value out of an HTTP response
# whose status nobody described. Two layers enforce that, because the two layers have different
# information available to them:
#
#   LAYER 1 — `*TestSupport.java` (P2-117). A shared helper has NO local place to describe a
#   response: whether 201 or 409 is correct depends on the caller. So the rule there is absolute
#   and structural — every value read goes through
#   `com.ax.template.authblueprint.common.HttpExtract`, which asserts the status, then the
#   presence of a Content-Type, then the JSON family, then the extraction, then the value's
#   presence — and reports status / content-type / every header / a bounded body excerpt /
#   `RestAssured.port` on failure. Detectors (A) and (B).
#
#   LAYER 2 — every other test class (P2-119). A test METHOD is exactly the place that knows
#   which status is correct, and this tree already says so 1404 times. So the rule there is that
#   the response must be DESCRIBED — by `HttpExtract`, or by a `statusCode(...)` the test itself
#   wrote. Detector (D), with detector (C) as its enabling premise.
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
# FOUR FORBIDDEN SHAPES — all four are "pull a value out of a response nobody described", and
# none of them has an exception:
#
#   (A) [*TestSupport.java] an `.extract()` whose next chained call is neither `.response()` nor
#       `.statusCode()`. `.extract().response()` is the sanctioned handoff: it reads NO value, it
#       hands the whole `Response` object onward — to `HttpExtract` in the same method, or to a
#       caller that validates it. `.extract().statusCode()` reads the STATUS, which is never null
#       and cannot fail to parse — it is how a test DESCRIBES a response, not a way of failing
#       to. A TERMINAL `.extract()` (the chain ends there, e.g.
#       `return given()...then().extract();`) is the same handoff in `ExtractableResponse` form
#       and is likewise allowed — it too reads no value. Everything else (`.extract().path(...)`,
#       `.extract().jsonPath()`, `.extract().as(...)`, `.extract().asString()`,
#       `.extract().asByteArray()`, `.extract().header(...)`, `.extract().body()`) pulls a value
#       out of a response nobody described, or opens a chain that does.
#
#   (B) [*TestSupport.java] a `.path(` / `.pathAt(` / `.jsonPath(` call whose qualifier is not
#       `HttpExtract`. This closes the two-statement evasion of (A), which no regex on
#       `.extract().path` would ever see:
#           var r = given()...then().extract();      // legal under (A) — reads nothing
#           return r.path("accessToken");            // ← caught here
#       and equally the no-`.extract()`-at-all form `Response r = when().post(...); r.path("x")`.
#
#   (C) [*TestSupport.java] a method whose DECLARED RETURN TYPE is a rest-assured response type
#       (`Response`, `ExtractableResponse<...>`, `ValidatableResponse`, `ResponseBody`,
#       `JsonPath`, `ResponseOptions`). This is P2-119's named residue, and (A)/(B) cannot see
#       it: handing the whole response out is legal under (A) precisely because the CALLER might
#       validate it — and when the caller does not, the blind read lands in a file (A)/(B) never
#       scan. Forbidding the export closes that hole at the seam rather than chasing it
#       downstream, and it is the premise that makes (D) sound (below). Returning a VALUE read
#       through `HttpExtract` is the sanctioned shape; returning the undescribed response is not.
#
#   (D) [every other test class] a value read (the same shapes as (A) and (B)) whose response
#       object is not DESCRIBED. A response is described when any of these holds:
#         · the read goes through `HttpExtract` (then it is not a violation at all, by (B)'s
#           qualifier test);
#         · the read's own call chain carries `statusCode(` / `spec(` — the dominant idiom,
#           `given()...then().statusCode(200).extract().path("id")`;
#         · the read's receiver is a local name `V`, and somewhere in the same method a chain
#           ROOTED AT `V` carries `statusCode(` / `getStatusCode(` / `spec(` — the second
#           dominant idiom, `var r = post(...); assertThat(r.statusCode()).isEqualTo(201);`;
#         · `V` was assigned from a chain that carries `statusCode(` / `spec(`;
#         · the receiver resolves to a SAME-FILE helper method whose body carries
#           `statusCode(` / `spec(` — the third idiom, a private
#           `get(...)` that ends `.then().statusCode(200).extract()`.
#       Anything else is a read off a response nobody described.
#
# WHY (D) IS ALLOWED TO PARSE STATUS ASSERTIONS WHEN LAYER 1 IS NOT. P2-117 recorded "no
# status-assertion parsing" as deliberately not done, and gave the reason: an assertion may live
# in a DIFFERENT FILE from the extraction, so at the TestSupport layer the question has no local
# answer. Detector (C) removes exactly that: once no `*TestSupport.java` may export a response, a
# response object inside a non-TestSupport test class can only have come from a chain built in
# that file — so "was this described" becomes an intra-file question with one answer. (C) is not
# an independent nicety, it is (D)'s soundness premise. The narrower objections P2-117 also
# raised are handled mechanically rather than avoided: chains that span lines are lexed, not
# grepped (comments, string/char literals and Java text blocks are blanked with offsets
# preserved), and chains built in fragments are followed through the local name and through
# same-file helper methods.
#
# `HttpExtract.java` calls `response.path(jsonPath)` itself, and is NOT scanned. That is not an
# exception to the rule — it is the distinction between the SUBJECT and the OBJECT of the rule.
# The helper is the one place where the validated extraction is implemented; it is excluded by
# construction (it IS the validated extraction), and the exclusion is derived from the file's
# own identity rather than declared in a list.
#
# THERE IS NO ALLOWLIST, AND MUST NOT BE ONE. An exception list is how a guard goes quietly
# vacuous: the file that most needs the rule is the file most likely to be excused from it. If
# this rule ever needs an exception, the rule is wrong and should be re-derived, not annotated.
# The target set is derived from disk on every run (`find`-equivalent walk of
# `backend/src/test`); no count is hardcoded, so a new domain is inside the rule the moment its
# file lands. The one place this was TEMPTING is recorded because it was resolved without an
# exception: `HttpExtractDiagnosabilityTest` deliberately performs blind reads to DEMONSTRATE
# that they fail silently. Pinning the status those two demonstrations already know
# (`r.then().statusCode(401).extract().path("accessToken")` still yields `null`) keeps the
# demonstration verbatim and makes it strictly stronger — it now shows that a status assertion
# ALONE is insufficient, which is why `HttpExtract` also asserts the value's presence.
#
# DELIBERATELY NOT DONE — the axes that remain open, stated rather than hidden:
#   • Inter-procedural parameter flow. A read whose receiver is a METHOD PARAMETER
#     (`private static JsonPath exact(ExtractableResponse<Response> r) { return r.body()...; }`)
#     is treated as described: whether it was described is a property of the CALL SITES, and the
#     guard does not chase them. 6 such sites exist on this tree (measured, not estimated). Each
#     is one hop from a described response, but the guard does not prove that — it declines to.
#   • Cross-file helper resolution for non-TestSupport files. If a test class obtains a response
#     from a helper in a THIRD class that is neither `*TestSupport.java` nor same-file, (D)
#     reports it as undescribed (fail-closed, so this is a false-positive risk, not a hole) — and
#     no such shape exists on this tree, because (C) removes the only class of file that used to
#     export responses.
#   • Method scope is the brace block at nesting depth 2. A read inside a NESTED or anonymous
#     class body has no depth-2 enclosing block, and the resolver then falls back to file scope,
#     where a status assertion on a same-named local in an UNRELATED method would describe it
#     falsely. Measured rather than assumed: 0 read sites on this tree take that fallback (0
#     before this change, 0 after), so the exposure is currently empty — but it is a real edge
#     and it is named here rather than left for someone to discover. The narrower version of the
#     same hazard is already closed: assignment resolution tries the enclosing method FIRST and
#     only widens to file scope for a field, because widening first resolved
#     `WithholdingSplitComplianceTest`'s `legAmount(… r …)` parameter against an unrelated
#     `ExtractableResponse<Response> r = collect(period);` 95 lines away.
#   • Whether the asserted status is the RIGHT status. `statusCode(500)` describes a response as
#     surely as `statusCode(200)` does. Which status a test should expect is the test's claim, not
#     this guard's.
#   • Whether the `context` argument handed to `HttpExtract` is informative. A string's
#     usefulness is a review judgement, not a binary.
#   • Reads that never touch `path`/`pathAt`/`jsonPath` and never follow `.extract()` — e.g.
#     `response.getBody().asString()` used as a raw string. Those carry no silent-null failure
#     mode (the P2-116 measurement that motivates this rule is specifically about GPath
#     extraction), so they are out of the modelled set.
#   • No PyYAML dependency (nothing here is YAML). python3 only — invoked from a top-level temp
#     file rather than a heredoc nested in `$(...)`, per P2-78 (stock bash 3.2 mis-parses that
#     nesting when the body's apostrophe count is odd).
#
# Exit: 0 PASS · 1 a test reads a value from an unvalidated response, or a TestSupport exports an
#       undescribed response · 2 usage / environment error.
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


def blank_noncode(src):
    """Replace comment and string/char LITERAL characters with spaces, preserving length.

    Offsets and line numbers therefore stay identical to the file on disk, so a javadoc that
    QUOTES the forbidden shape (ApprovalWorkflowTestSupport does exactly that, describing the
    pattern it was moved off) cannot be reported as code. Literal CONTENT is blanked too, not
    merely skipped: this tree carries strings like "then().extract().path" inside diagnostic
    messages, and a scan that saw those would report every one as a read.

    Java TEXT BLOCKS are lexed as text blocks. Reading `\"\"\"` as an empty string followed by
    the start of a new one puts the lexer exactly out of phase, and because a plain string
    literal is force-closed at end of line, every CONTENT line of the block would then be
    scanned as code (P2-120 measured this on guard [117]). Newlines are preserved inside blocks
    and comments so line numbers still match disk.
    """
    out = list(src)
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"' and src[i:i + 3] == '"""':
            out[i] = out[i + 1] = out[i + 2] = ' '
            i += 3
            while i < n and src[i:i + 3] != '"""':
                if src[i] == '\\' and i + 1 < n:
                    out[i] = ' '
                    i += 1
                    if src[i] != '\n':
                        out[i] = ' '
                    i += 1
                    continue
                if src[i] != '\n':
                    out[i] = ' '
                i += 1
            for j in range(i, min(i + 3, n)):
                out[j] = ' '
            i += 3
        elif c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == '\\' and i + 1 < n:
                    out[i] = ' '
                    i += 1
                if i < n and src[i] == '\n':
                    break
                out[i] = ' '
                i += 1
            i += 1
        elif c == "'":
            i += 1
            while i < n and src[i] != "'":
                if src[i] == '\\' and i + 1 < n:
                    out[i] = ' '
                    i += 1
                if i < n:
                    out[i] = ' '
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

# Detector (C): rest-assured types that carry an UNDESCRIBED response across a method boundary.
# `RequestSpecification` is deliberately absent — that is a request, not a response.
RESPONSE_TYPE = (r'(?:(?:io\s*\.\s*restassured\s*\.\s*[\w.\s]*?)?'
                 r'(?:ExtractableResponse|ValidatableResponse|ResponseOptions|ResponseBody'
                 r'|JsonPath|Response)\s*(?:<[^;{}=()]*>)?)')
METHOD_DECL = re.compile(r'(?:^|[;{}])[^;{}()=]*?\b' + RESPONSE_TYPE + r'\s+([A-Za-z_]\w*)\s*\(')

# Detector (C)'s own pattern — same syntax as METHOD_DECL, different question (see
# exported_responses). Written as ONE self-contained literal on ONE line on purpose: it is the
# kill-proof anchor for (C), and a sentinel-string substitution here disables (C) and nothing else.
EXPORT_DECL = re.compile(r'(?:^|[;{}])[^;{}()=]*?\b(?:(?:io\s*\.\s*restassured\s*\.\s*[\w.\s]*?)?(?:ExtractableResponse|ValidatableResponse|ResponseOptions|ResponseBody|JsonPath|Response)\s*(?:<[^;{}=()]*>)?)\s+([A-Za-z_]\w*)\s*\(')

# Detector (D): the three forms in which this tree describes a response.
STATUS_SEG = ('statusCode', 'getStatusCode', 'spec')
STATUS_CALL = re.compile(r'\.\s*(?:statusCode|getStatusCode|spec)\s*\(')

# The followers of `.extract()` that pull NO value out of the body, so cannot exhibit the
# P2-116 failure modes:
#   `response` — the sanctioned handoff; passes the whole Response object onward.
#   `statusCode` — reads the STATUS. It returns an int, is never null, cannot fail to parse, and
#       reading it is how a test DESCRIBES a response rather than a way of failing to
#       (`int s = given()...then().extract().statusCode(); assertThat(s).isEqualTo(201);` — 25
#       sites on this tree). Forbidding it would flag the description itself.
# Everything else — `path`, `jsonPath`, `as`, `asString`, `asByteArray`, `header`, and `body`
# (whose own follower may be `asString()`, which detector (B) does not model) — pulls a value or
# opens a path to one, and stays inside the rule.
SANCTIONED_FOLLOWERS = ('response', 'statusCode')


def block_end(text, open_brace):
    depth = 0
    for i in range(open_brace, len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return i
    return len(text)


def method_bodies(text):
    """Brace blocks at nesting depth 2 — a method body inside a top-level type."""
    out, stack, depth = [], [], 0
    for i, c in enumerate(text):
        if c == '{':
            depth += 1
            stack.append((depth, i))
        elif c == '}':
            if stack:
                d, s = stack.pop()
                if d == 2:
                    out.append((s, i))
            depth -= 1
    return out


def chain_root(text, dot):
    """Walk back from the `.` that begins a call, over the whole receiver chain.

    Returns (kind, root_name, segments) where kind is 'name' (a bare identifier receiver),
    'call' (the chain starts with a method call, e.g. `given()` or a same-file helper) or
    'other' (a literal, an index expression, `this`, anything unresolvable). `segments` holds
    every method name crossed on the way back, so `.then().statusCode(200).extract().path("x")`
    yields segments ['extract', 'statusCode', 'then'] — which is how the in-chain description
    test is answered without a regex over the raw line.
    """
    segments = []
    i = dot
    while True:
        j = i - 1
        while j >= 0 and text[j].isspace():
            j -= 1
        if j < 0:
            return ('other', '', segments)
        if text[j] == ')':
            depth, k = 0, j
            while k >= 0:
                if text[k] == ')':
                    depth += 1
                elif text[k] == '(':
                    depth -= 1
                    if depth == 0:
                        break
                k -= 1
            if k < 0:
                return ('other', '', segments)
            m = k - 1
            while m >= 0 and text[m].isspace():
                m -= 1
            end = m
            while m >= 0 and (text[m].isalnum() or text[m] == '_'):
                m -= 1
            name = text[m + 1:end + 1]
            if not name:
                # a parenthesised expression or a cast, e.g. `((Response) x).path(...)`
                return ('other', '', segments)
            segments.append(name)
            p = m
            while p >= 0 and text[p].isspace():
                p -= 1
            if p >= 0 and text[p] == '.':
                i = p
                continue
            return ('call', name, segments)
        if text[j] == ']':
            return ('other', '', segments)
        end = j
        while j >= 0 and (text[j].isalnum() or text[j] == '_'):
            j -= 1
        name = text[j + 1:end + 1]
        if not name:
            return ('other', '', segments)
        segments.append(name)
        p = j
        while p >= 0 and text[p].isspace():
            p -= 1
        if p >= 0 and text[p] == '.':
            i = p
            continue
        return ('name', name, segments)


class FileScan(object):
    def __init__(self, text):
        self.text = text
        self.bodies = method_bodies(text)
        # same-file method table: name -> do ALL of its declarations describe a status?
        # The table is keyed by NAME, so OVERLOADS collapse into one entry. They are AND-folded,
        # not overwritten: with `f(a, b)` delegating to a described `f(a, b, status)`, taking the
        # last declaration seen would call every read through the UNDESCRIBED 2-arg overload
        # described, and which one that is would depend on declaration order in the file.
        self.helpers = {}
        for m in METHOD_DECL.finditer(text):
            brace = text.find('{', m.end())
            if brace == -1:
                continue
            described = bool(STATUS_CALL.search(text[brace:block_end(text, brace)]))
            name = m.group(1)
            self.helpers[name] = described and self.helpers.get(name, True)

    def body_of(self, offset):
        best = None
        for s, e in self.bodies:
            if s <= offset <= e and (best is None or s > best[0]):
                best = (s, e)
        return best if best else (0, len(self.text))

    def describes_name(self, offset, var):
        """Is there a status assertion on a chain ROOTED AT `var` in the enclosing method?"""
        lo, hi = self.body_of(offset)
        for m in STATUS_CALL.finditer(self.text, lo, hi):
            kind, root, _segs = chain_root(self.text, m.start())
            if kind == 'name' and root == var:
                return True
        return False

    def assignment_describes(self, lo, hi, var):
        """Resolve `var`'s assignment within [lo, hi).

        Returns True (described), False (assigned from something undescribed) or None (no
        assignment to `var` in this range at all). The range is passed in rather than derived
        so the caller can try the enclosing METHOD first and only widen to file scope for a
        field — widening first would resolve a local against a same-named local in an unrelated
        method, which is how a data-flow scan silently answers the wrong question.
        """
        pattern = re.compile(r'\b' + re.escape(var) + r'\b\s*=(?![=])')
        found = None
        for m in pattern.finditer(self.text, lo, hi):
            end = self.text.find(';', m.end())
            rhs = self.text[m.end():end if end != -1 else hi]
            if STATUS_CALL.search(rhs):
                return True
            call = re.search(r'([A-Za-z_]\w*)\s*\(', rhs)
            if call and call.group(1) in self.helpers:
                found = self.helpers[call.group(1)]
                if found:
                    return True
                continue
            found = False
        return found

    def is_parameter(self, offset, var):
        """Is `var` a parameter of the enclosing method? (declared residue — see header.)"""
        lo, _hi = self.body_of(offset)
        head = self.text[max(0, lo - 600):lo]
        open_paren = head.rfind('(')
        if open_paren == -1:
            return False
        params = head[open_paren:]
        return re.search(r'[(,]\s*[^,()]*\b' + re.escape(var) + r'\s*(?:,|\)|$)', params) is not None

    def is_undescribed(self, read_dot):
        """Detector (D)'s question in its POSITIVE form: is this read a violation?

        The emission gate is phrased positively for two reasons. It reads as what it is — a
        violation predicate — and it is the form a kill-proof substitution can disable: replacing
        `described(...)` with a constant would have to be `True` to silence the detector, whereas
        the manifest's PIT-style vocabulary admits only a `False` constant, which on
        `if described: continue` would invert the guard into reporting EVERY read instead of none.
        """
        return not self.described(read_dot)

    def described(self, read_dot):
        """Is the response this read pulls from described? (detector (D))"""
        kind, root, segments = chain_root(self.text, read_dot)
        if any(s in STATUS_SEG for s in segments):
            return True
        if kind == 'call':
            if root in self.helpers:
                return self.helpers[root]
            return False
        if kind == 'name':
            if root == 'HttpExtract':
                return True
            if self.describes_name(read_dot, root):
                return True
            lo, hi = self.body_of(read_dot)
            assigned = self.assignment_describes(lo, hi, root)
            if assigned is not None:
                return assigned
            if self.is_parameter(read_dot, root):
                return True
            assigned = self.assignment_describes(0, len(self.text), root)
            return bool(assigned)
        return False


def exported_responses(text):
    """Detector (C): every method declaration whose return type is a rest-assured response.

    Deliberately driven by its OWN pattern, not the `METHOD_DECL` the helper table uses. The two
    ask different questions of the same syntax — "does this method hand a response out" versus
    "does this method pin a status" — and separating them means the kill-proof manifest can
    disable (C) alone, without also emptying the helper table that detector (D) resolves through.
    """
    return list(EXPORT_DECL.finditer(text))


def read_sites(text):
    """Every value-read out of a response, as (offset_of_leading_dot, line_key, description).

    Shape (A)/(D-A): an `.extract()` whose next chained call is not `.response()`.
    Shape (B)/(D-B): a `.path(` / `.pathAt(` / `.jsonPath(` call.
    """
    sites, a_lines = [], set()
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
        if follower in SANCTIONED_FOLLOWERS:
            continue
        arg = JSONPATH_ARG.match(text, idm.end() if idm else k)
        a_lines.add(m.start())
        sites.append(('A', m.start(), j, follower,
                      ".extract().%s(...) reads a value out of a response nobody described"
                      % follower,
                      arg.group(1) if arg else 'jsonPath'))
    for m in READER.finditer(text):
        qm = QUALIFIER.search(text[:m.start()])
        qual = qm.group(1) if qm else '<expr>'
        if qual == 'HttpExtract':
            continue
        arg = JSONPATH_ARG.match(text, m.end() - 1)
        sites.append(('B', m.start(), m.start(), qual,
                      "%s.%s(...) reads a value straight off a response object — it never "
                      "passed through HttpExtract" % (qual, m.group(1)),
                      arg.group(1) if arg else 'jsonPath'))
    return sites, a_lines


support_files, other_files = [], []
for dirpath, _dirnames, filenames in os.walk(test_root):
    for name in filenames:
        if not name.endswith('.java'):
            continue
        path = os.path.join(dirpath, name)
        if name.endswith('TestSupport.java'):
            support_files.append(path)
        elif name != 'HttpExtract.java':
            other_files.append(path)
support_files.sort()
other_files.sort()

violations = []


def load(path):
    try:
        with open(path, encoding='utf-8') as fh:
            return fh.read()
    except OSError as exc:
        print('test_support_response_validation_guard: cannot read %s (%s)' % (path, exc),
              file=sys.stderr)
        sys.exit(2)


# ── LAYER 1 — *TestSupport.java: detectors (A), (B), (C) ────────────────────────────────────
for path in support_files:
    src = load(path)
    text = blank_noncode(src)
    starts = line_starts(text)
    raw_lines = src.split('\n')
    rel = os.path.relpath(path, root)
    sites, _a = read_sites(text)
    reported = set()
    for kind, offset, _dot, _q, why, jsonpath in sorted(sites, key=lambda s: s[1]):
        line = line_of(starts, offset)
        if kind == 'A':
            reported.add(line)
        elif line in reported:
            continue  # same call site, already named by (A)
        violations.append((rel, line, raw_lines[line - 1], why, jsonpath, 'helper'))
    for m in exported_responses(text):
        line = line_of(starts, m.start(1))
        violations.append((
            rel, line, raw_lines[line - 1],
            "%s(...) hands a rest-assured response object OUT of a TestSupport helper — the "
            "caller can then read it without describing it, where (A)/(B) never look" % m.group(1),
            'jsonPath', 'export'))

# ── LAYER 2 — every other test class: detector (D) ──────────────────────────────────────────
for path in other_files:
    src = load(path)
    text = blank_noncode(src)
    if 'extract' not in text and 'jsonPath' not in text and '.path(' not in text:
        continue
    starts = line_starts(text)
    raw_lines = src.split('\n')
    rel = os.path.relpath(path, root)
    scan = FileScan(text)
    sites, _a = read_sites(text)
    reported = set()
    for kind, offset, dot, _q, why, jsonpath in sorted(sites, key=lambda s: s[1]):
        if not scan.is_undescribed(dot if kind == 'A' else offset):
            continue
        line = line_of(starts, offset)
        if kind == 'A':
            reported.add(line)
        elif line in reported:
            continue
        violations.append((rel, line, raw_lines[line - 1], why, jsonpath, 'caller'))

violations.sort(key=lambda v: (v[0], v[1]))

for rel, line, raw, why, jsonpath, layer in violations:
    print('VIOLATION: %s:%d — %s' % (rel, line, why), file=sys.stderr)
    print('    %s' % raw.strip(), file=sys.stderr)
    if layer == 'export':
        print('    fix: return the VALUE, read through the helper, not the response —',
              file=sys.stderr)
        print('         Response resp = given()...when()...then().extract().response();',
              file=sys.stderr)
        print('         return HttpExtract.path(resp, "id", "<METHOD /path> (<what for>)");',
              file=sys.stderr)
    elif layer == 'caller':
        print('    fix: describe the response before reading it — either pin the status in the',
              file=sys.stderr)
        print('         chain (.then().statusCode(200).extract()...), or assert it on the local',
              file=sys.stderr)
        print('         (assertThat(r.statusCode()).isEqualTo(200);), or read through the helper',
              file=sys.stderr)
        print('         HttpExtract.path(resp, "%s", "<METHOD /path> (<what for>)");' % jsonpath,
              file=sys.stderr)
    else:
        print('    fix: hand the whole response over, then read it through the helper —',
              file=sys.stderr)
        print('         Response resp = given()...when()...then().extract().response();',
              file=sys.stderr)
        print('         HttpExtract.path(resp, "%s", "<METHOD /path> (<what this call is for>)");'
              % jsonpath, file=sys.stderr)
        print('         (use HttpExtract.pathAt(resp, <status>, ...) to pin a non-2xx status)',
              file=sys.stderr)

print('%d %d %d' % (len(support_files), len(other_files), len(violations)))
PY

scan_out="$(python3 "$SCAN_PY" "$ROOT" "$TEST_ROOT")"
scan_rc=$?
if [ "$scan_rc" -ne 0 ]; then
    echo "test_support_response_validation_guard: scan failed (rc=$scan_rc)" >&2
    exit 2
fi

set -- $scan_out
if [ $# -ne 3 ]; then
    echo "test_support_response_validation_guard: scanner produced no verdict" >&2
    exit 2
fi
support_count="$1"
other_count="$2"
violations="$3"

if [ "$support_count" -eq 0 ] && [ "$other_count" -eq 0 ]; then
    echo "test_support_response_validation_guard: no test sources under $TEST_ROOT — nothing to check"
    exit 0
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "test_support_response_validation_guard: $violations unvalidated response read(s) / response export(s) across $support_count *TestSupport.java + $other_count other test file(s) — BLOCKED" >&2
    echo "  Reading a value from a response that was never described turns every infrastructural" >&2
    echo "  failure into an exception that names nothing (P2-116/P2-117), and a 4xx JSON error body" >&2
    echo "  into a silent null carried forward as if it were the value. Handing an undescribed" >&2
    echo "  response out of a TestSupport helper moves that same read into a file the TestSupport" >&2
    echo "  detectors never scan (P2-119)." >&2
    exit 1
fi

echo "test_support_response_validation_guard: PASS — $support_count *TestSupport.java read responses through HttpExtract and export none; $other_count other test file(s) read only described responses"
exit 0
