#!/usr/bin/env bash
# practices/evals/restassured_port_single_writer_guard.sh — BACKLOG P2-120.
#
# THE INVARIANT: nothing under `backend/src/test/**` may ASSIGN
# `io.restassured.RestAssured.port`. That process-global has exactly ONE writer — the JUnit
# extension registered in `backend/src/test/resources/META-INF/services/
# org.junit.jupiter.api.extension.Extension`, which reads each test instance's
# `@LocalServerPort` immediately before the test runs and records what it published. Reading the
# field (`"port=" + RestAssured.port`, `RestAssured.port == expected`) stays legal; only writing
# is forbidden.
#
# WHY. `RestAssured.port` is a process-global mutable static and, before P2-120, it was written
# by hand all over the test tree. Census, re-measured against f4457530 after a fable5
# adversarial review found this header contradicting BACKLOG/DECISIONS (each said something
# different, and "84" vs "86" was a direct conflict on the same metric):
#     86  `useRandomPort` DEFINITIONS removed        (one per file)
#    141  `RestAssured.port = ...;` ASSIGNMENTS removed (string literals excluded)
#    139  FILES that held at least one assignment    (86 with a definition + 53 without)
#   git diff f4457530 -- backend/src/test | grep -c '^-.*static void useRandomPort('
# The earlier "140 = 86 + 54" in this header conflated files with statements. A global with
# that many writers has no reasoning about it —
# a file that writes it late, not at all, or with a neighbour's value silently redirects every
# request that follows, and nothing fails at the point of the mistake. This is one of the two
# surviving candidates for the P3-144 flake ("a mistargeted port"), and the machine it was
# measured on makes it concrete: rest-assured's default port is 8080, and another process there
# answers `*:8080` with a Content-Type-less 404 — byte-identical to the shape P3-144 exhibited.
# Deleting those assignments once does not stop the next one: every new domain's test class in
# this tree is written by copying a neighbour, so the property has to be structural.
#
# SUBJECT vs OBJECT — NOT AN EXCEPTION. The registered extension class assigns
# `RestAssured.port`; that is what makes it the writer. It is out of the scanned set because it
# is the rule's SUBJECT, and the distinction is DERIVED, not declared: this guard reads the
# ServiceLoader registration file, resolves each fully qualified class name it lists to
# `backend/src/test/java/<path>.java`, and excludes exactly those files. Delete the registration
# and the extension file is scanned like everything else (and the suite loses its writer, which
# is the honest consequence). Add a second class to the registration and the subject set grows by
# exactly that class — visibly, in a diff, at the place that actually confers the privilege.
#
# THERE IS NO ALLOWLIST, AND MUST NOT BE ONE. An exception list is how a guard goes quietly
# vacuous: the file that most needs the rule is the file most likely to be excused from it. The
# object set is derived from disk on every run (`find "$TEST_ROOT" -name '*.java'`), so a new
# domain is inside the rule the moment its file lands; no count is hardcoded anywhere.
#
# TWO FORBIDDEN SHAPES:
#
#   (A) an assignment whose target is `RestAssured.port` — `RestAssured.port = p;`,
#       `io.restassured.RestAssured.port = p;`, and the compound forms (`+=`, `-=`, `*=`, `/=`,
#       `%=`). `==`, `>=`, `<=`, `!=` are comparisons and are NOT assignments; they pass.
#
#   (B) a static import that brings the field in under its simple name — BOTH
#       `import static io.restassured.RestAssured.port;` AND the wildcard
#       `import static io.restassured.RestAssured.*;`. Java permits assignment through a
#       single-static-import simple name, so `port = 8080;` would then be an unqualified write
#       that no regex on shape (A) could ever see, and the wildcard imports that same name for
#       one keystroke less. The wildcard is not a theoretical hole: this tree's dominant spelling
#       is `import static io.restassured.RestAssured.given;`, so `given` -> `*` is a one-character
#       edit away from the exact "copied a neighbouring line" failure this guard exists for.
#       Refusing it costs nothing measurable — at the time of writing, ZERO files under
#       `backend/src/test/**` wildcard-import anything from `io.restassured` (17 files do
#       wildcard-import from other packages, so the measurement is not vacuous). Forbidding both
#       import shapes closes the door at the one line where it is declared, without needing to
#       resolve identifiers.
#
# Comments and string/char LITERAL CONTENTS are blanked before scanning — including Java TEXT
# BLOCK (`"""..."""`) contents, whose delimiters are lexed as delimiters rather than as an empty
# string followed by a new one. Offsets are preserved, so reported line numbers match disk. That
# is load-bearing here rather than cosmetic, in both directions: this tree asserts on the literal
# `"RestAssured.port="` in several diagnosability tests (a naive `grep 'RestAssured.port *='`
# reports every one of them as a write), and 11 files under `backend/src/test/**` already carry
# text blocks, so a scanner that mis-lexed `"""` would read their contents as code.
#
# DELIBERATELY NOT DONE:
#   • No check on `given().port(n)`. That is a PER-REQUEST port and touches no global — it is
#     the correct way to aim one call somewhere else, and one diagnosability test uses it on
#     purpose.
#   • No check that a test declares `@LocalServerPort` at all. A MOCK-environment or plain unit
#     test legitimately has none, and the extension deliberately leaves the global untouched
#     there; demanding the field would be demanding a lie.
#   • No reflective-write detection (`Field.set` onto RestAssured). Defeating a guard by
#     reflection is not the failure mode this exists for — copying a neighbouring line is.
#   • No check on the OTHER process-global fields of `RestAssured` — `RestAssured.baseURI`,
#     `RestAssured.basePath`, `RestAssured.config`, `RestAssured.requestSpecification`,
#     `RestAssured.reset()`. THE SINGLE-WRITER CLAIM THIS GUARD MAKES IS SCOPED TO THE `port`
#     AXIS AND TO NOTHING ELSE. Those fields also steer where a request lands (host, path,
#     defaults), so a mistargeted request is possible through them and this guard would not say
#     so. They are out of scope because they have live, legitimate, measured users that the
#     `port` axis does not: `RestAssured.config` is set by 2 files to widen JSON number parsing,
#     and `RestAssured.baseURI` is set — and restored in a `finally` — by the approval-workflow
#     diagnosability test, which must pin `127.0.0.1` because rest-assured's default
#     `http://localhost` is resolver-dependent. Refusing those without an escape hatch would
#     break real tests, and an escape hatch is an allowlist, which this guard must not have. If
#     the host axis is ever to be governed, it needs its own authority (an `AxHost` analogous to
#     `AxPort`), not an exception carved into this one.
#   • No PyYAML dependency (nothing here is YAML). python3 only — invoked from a top-level temp
#     file rather than a heredoc nested in `$(...)`, per P2-78 (stock bash 3.2 mis-parses that
#     nesting when the body's apostrophe count is odd).
#
# Exit: 0 PASS · 1 a file under backend/src/test assigns RestAssured.port (or imports it
#       statically) · 2 usage / environment error.
#
# Usage:
#   bash practices/evals/restassured_port_single_writer_guard.sh
#   bash practices/evals/restassured_port_single_writer_guard.sh --root DIR   # fixture tree
#     (DIR is scanned at DIR/backend/src/test)
#
# Registered in practices/evals/run-all-guards.sh as guard [117].

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
                echo "restassured_port_single_writer_guard: --root requires a directory argument" >&2
                exit 2
            fi
            ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "restassured_port_single_writer_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ -z "$ROOT" ] || [ ! -d "$ROOT" ]; then
    echo "restassured_port_single_writer_guard: --root must name an existing directory (got '${ROOT}')" >&2
    exit 2
fi

TEST_ROOT="$ROOT/backend/src/test"
if [ ! -d "$TEST_ROOT" ]; then
    echo "restassured_port_single_writer_guard: $TEST_ROOT not found — nothing to check"
    exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "restassured_port_single_writer_guard: python3 not in PATH (required for scanning)" >&2
    exit 2
fi

# P2-78: write the python body to a top-level temp file and invoke it by path, rather than
# nesting a quoted heredoc inside $(...) — bash 3.2.57 mis-parses that nesting whenever the
# body's apostrophe count is odd, and a one-word comment edit flips that parity.
SCAN_PY="$(mktemp "${TMPDIR:-/tmp}/restassured_port_single_writer.XXXXXX")"
trap 'rm -f "$SCAN_PY"' EXIT

cat <<'PY' > "$SCAN_PY"
import os
import re
import sys

root, test_root = sys.argv[1], sys.argv[2]

SERVICES = os.path.join(
    test_root, 'resources', 'META-INF', 'services',
    'org.junit.jupiter.api.extension.Extension')
JAVA_ROOT = os.path.join(test_root, 'java')


def blank_noncode(src):
    """Replace comment and string/char LITERAL characters with spaces, preserving length.

    Offsets and line numbers therefore stay identical to the file on disk. Literal contents are
    blanked (not just skipped) because this tree asserts on the literal "RestAssured.port=" in
    its diagnosability tests, and a scan that saw those would report every one as a write.

    Java TEXT BLOCKS are lexed as text blocks. Reading `\"\"\"` as an empty string followed by the
    start of a new one puts the lexer exactly out of phase, and because a plain string literal is
    force-closed at end of line, every CONTENT line of the block is then scanned as code. Newlines
    are preserved inside the block (as they are inside block comments) so line numbers still match
    disk.
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


def registered_writer_files():
    """The SUBJECT set: source files of the classes the ServiceLoader registration names.

    Derived, never declared. Missing registration -> empty subject set, and every file is
    scanned (the tree then has no sanctioned writer at all, which is itself the finding).
    """
    files, classes = set(), []
    try:
        with open(SERVICES, encoding='utf-8') as fh:
            raw = fh.read()
    except OSError:
        return files, classes, False
    for line in raw.splitlines():
        line = line.split('#', 1)[0].strip()
        if not line:
            continue
        classes.append(line)
        outer = line.split('$', 1)[0]
        files.add(os.path.join(JAVA_ROOT, *outer.split('.')) + '.java')
    return files, classes, True


# (A) an ASSIGNMENT whose target is RestAssured.port. The trailing class excludes '=' so that
#     '==' is read as the comparison it is; the compound operators are listed explicitly.
ASSIGN = re.compile(
    r'(?:\bio\s*\.\s*restassured\s*\.\s*)?\bRestAssured\s*\.\s*port\s*'
    r'(?:=(?!=)|\+=|-=|\*=|/=|%=)')
# (B) the simple-name evasion: Java allows assignment through a single-static-import name. Both
#     tails are refused — the explicit `.port;` and the wildcard `.*;`, which imports that same
#     name while looking like the `.given;` spelling every file in this tree already carries.
#     Leading indent is `[^\S\n]*`, NOT `\s*` — `\s` matches the newline that ENDS the previous
#     line, so `\s*` would start the match one line early and the report would quote a blank.
STATIC_IMPORT = re.compile(
    r'^[^\S\n]*import\s+static\s+io\s*\.\s*restassured\s*\.\s*RestAssured\s*\.\s*'
    r'(?:port|\*)\s*;',
    re.MULTILINE)

subject_files, subject_classes, services_present = registered_writer_files()

files = []
for dirpath, _dirnames, filenames in os.walk(test_root):
    for name in filenames:
        if not name.endswith('.java'):
            continue
        path = os.path.join(dirpath, name)
        if os.path.normpath(path) in {os.path.normpath(p) for p in subject_files}:
            continue
        files.append(path)
files.sort()

violations = []

for path in files:
    try:
        with open(path, encoding='utf-8') as fh:
            src = fh.read()
    except OSError as exc:
        print('restassured_port_single_writer_guard: cannot read %s (%s)' % (path, exc),
              file=sys.stderr)
        sys.exit(2)

    text = blank_noncode(src)
    starts = line_starts(text)
    raw_lines = src.split('\n')
    rel = os.path.relpath(path, root)

    for m in ASSIGN.finditer(text):
        line = line_of(starts, m.start())
        violations.append((rel, line, raw_lines[line - 1],
                           'assigns the process-global RestAssured.port'))

    for m in STATIC_IMPORT.finditer(text):
        line = line_of(starts, m.start())
        wildcard = m.group(0).rstrip().rstrip(';').rstrip().endswith('*')
        why = ('wildcard-statically imports io.restassured.RestAssured, which brings `port` in '
               'under its simple name and makes an unqualified `port = ...` a write to the '
               'process-global') if wildcard else (
              'statically imports RestAssured.port, which makes an unqualified '
              '`port = ...` a write to the process-global')
        violations.append((rel, line, raw_lines[line - 1], why))

violations.sort(key=lambda v: (v[0], v[1]))

for rel, line, raw, why in violations:
    print('VIOLATION: %s:%d — %s' % (rel, line, why), file=sys.stderr)
    print('    %s' % raw.strip(), file=sys.stderr)
    if why.startswith('wildcard'):
        print('    fix: name the members this file actually uses '
              '(`import static io.restassured.RestAssured.given;`) instead of `.*`.',
              file=sys.stderr)
        continue
    print('    fix: delete the line. Keep the @LocalServerPort field — the registered extension',
          file=sys.stderr)
    print('         reads it and publishes the port before every test. To aim at a server that',
          file=sys.stderr)
    print('         is deliberately NOT this application (a stub), say so:', file=sys.stderr)
    print('         AxPort.overrideForStub(port); ... finally { AxPort.restoreAfterStub(); }',
          file=sys.stderr)

print('%d %d %d %d' % (len(files), len(violations), len(subject_classes),
                       1 if services_present else 0))
PY

scan_out="$(python3 "$SCAN_PY" "$ROOT" "$TEST_ROOT")"
scan_rc=$?
if [ "$scan_rc" -ne 0 ]; then
    echo "restassured_port_single_writer_guard: scan failed (rc=$scan_rc)" >&2
    exit 2
fi

set -- $scan_out
if [ "$#" -ne 4 ]; then
    echo "restassured_port_single_writer_guard: scanner produced no verdict" >&2
    exit 2
fi
checked="$1"; violations="$2"; subjects="$3"; services_present="$4"

if [ "$services_present" -eq 0 ]; then
    echo "restassured_port_single_writer_guard: NOTE — no ServiceLoader registration at" >&2
    echo "  backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension," >&2
    echo "  so this tree has NO sanctioned writer and every .java file is scanned." >&2
fi

if [ "$checked" -eq 0 ]; then
    echo "restassured_port_single_writer_guard: no scannable .java under $TEST_ROOT — nothing to check"
    exit 0
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "restassured_port_single_writer_guard: $violations write(s) to the process-global RestAssured.port across $checked scanned file(s) — BLOCKED" >&2
    echo "  RestAssured.port steers EVERY request in the JVM. A second writer means a test can be" >&2
    echo "  aimed at a server that is not its own application, and nothing fails where the mistake" >&2
    echo "  was made — which is exactly how P3-144 presented (a Content-Type-less 404 from whatever" >&2
    echo "  else happens to answer on rest-assured's default port 8080)." >&2
    echo "  The single sanctioned writer is the $subjects class(es) named in" >&2
    echo "  backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension." >&2
    exit 1
fi

echo "restassured_port_single_writer_guard: PASS — $checked scanned file(s) leave RestAssured.port to its $subjects registered writer(s)"
exit 0
