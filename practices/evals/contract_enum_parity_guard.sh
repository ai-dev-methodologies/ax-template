#!/usr/bin/env bash
# practices/evals/contract_enum_parity_guard.sh
# P2-33 — contract↔code enum parity, EXHAUSTIVE BY CONSTRUCTION.
#
# THE INVARIANT: an OpenAPI `enum:` block is a promise about the bytes on the
# wire. Nothing mechanically bound those promises to the Java enums that actually
# serialize/deserialize them, so a contract could advertise a vocabulary the code
# can never emit (client switch-cases a dead branch) or omit one the code DOES
# emit (a strict client rejects a real response). P2-34 is the same defect class
# one layer out: an L4 fork-copy carrying a vocabulary that is not the canonical
# one at all.
#
# TWO ENTRY KINDS (`practices/evals/contract-enum-map.yaml`):
#
#   kind: contract_enum   — one `enum:` block, addressed by contract path +
#                           RFC-6901 JSON pointer. EXACTLY ONE of:
#                             java_enum: <FQCN>   — the block is bound to a Java
#                                                   enum; constant sets must match.
#                             wire_only: <reason> — no Java ENUM backs the block
#                                                   (the code side is a String).
#                                                   The reason is mandatory AND a
#                                                   `wire_source:` block is mandatory
#                                                   (see below) — a reason alone is
#                                                   NOT sufficient.
#                           Modifiers on a `java_enum` entry (each needs `reason`):
#                             wire_extra:   [T…]  — tokens on the wire that are NOT
#                                                   Java constants (e.g. the `ALL`
#                                                   filter sentinel).
#                             wire_missing: [T…]  — Java constants deliberately not
#                                                   offered in THIS block (e.g. a
#                                                   creation ack that can only be
#                                                   PENDING).
#                             wire_case: lower    — the wire spells the constants in
#                                                   lower case (compared fold-wise).
#                           Both modifier lists are checked for NON-REDUNDANCY: a
#                           listed token that is not actually in the corresponding
#                           difference FAILS. A stale allowance cannot rot silently.
#
# WIRE_ONLY IS NOT AN EXEMPTION (P0-2, cross-family review). A `wire_only` entry used
# to be CLASSIFICATION-ONLY: the guard checked that a non-empty `reason` existed and
# moved on, so the literal vocabulary of ~a quarter of the contract's enum blocks could
# drift freely (the reviewer's reproduction: flip the ratelimit probe's `"ok"` to
# `"healthy"` — contract, guard and the domain task all stayed green). Every wire_only
# entry now MUST carry a `wire_source:` block naming WHERE the wire literals are
# produced; the guard EXTRACTS the literal set from that source and asserts EXACT SET
# EQUALITY with the contract block (wire_extra / wire_missing allowances apply, with the
# same non-redundancy check as a java_enum entry).
#
#   wire_source kinds — four EXTRACTING kinds (set equality) + one ABSENCE kind:
#     java_field_literals  file: X.java [symbol: NAME]
#                          With `symbol`: every string literal in THAT field's
#                          initializer (`Set.of("KRW","USD")` → {KRW, USD}).
#                          Without: the value of every `static final String` constant
#                          declared in the file — exhaustive over the constant block.
#                          FAIL-CLOSED: the initializer (resp. every constant) must be
#                          built from plain string literals and collection syntax ONLY.
#                          A value-bearing identifier (`Set.of("KRW", OTHER)`) ERRORS —
#                          the guard will not report the literals it recognised as if
#                          they were the whole set.
#     java_enum_decl       file: X.java  name: E
#                          `enum E { A, B }` → {A, B}. For a producer that lives in a
#                          templates/ skeleton (the fork-receiver's copy target) rather
#                          than in the reference workload's own source tree.
#     java_method_returns  files: <glob>  method: m
#                          Every `return "lit";` inside `m()`'s body across the matched
#                          files — the SPI shape (each adapter's providerName()).
#                          An abstract declaration (`String m();`) has no body and is
#                          skipped, so the interface never contributes.
#                          FAIL-CLOSED: EVERY return in a matched body must be a plain
#                          literal. An adapter that computes its slug ERRORS rather than
#                          being skipped while the other adapters' slugs are reported as
#                          the complete set.
#     literal_pattern      file:|files: <path|glob>  pattern: <regex with ONE group>
#                          residue_probe: <regex>
#                          Every capture of the regex across the matched files. The
#                          escape hatch for a literal produced inline (a probe payload,
#                          an `equals("json")` chain). Comments are stripped first for
#                          .java/.ts/.tsx, so a javadoc example cannot fake a producer.
#                          FAIL-CLOSED (P0-2 round 2): a regex over java source is NOT a
#                          parser, so `pattern` can silently SKIP a producing expression
#                          it does not recognise and still return a non-empty set scraped
#                          from somewhere else in the file — reporting PASS on literals
#                          that are not the ones on the wire. `residue_probe:` is
#                          therefore MANDATORY: it names the PRODUCING CONSTRUCT (not the
#                          literal), and the guard requires
#                            (a) residue_probe matches the source at least ONCE  — a probe
#                                that matches nothing is vacuous and FAILS; and
#                            (b) residue_probe matches ZERO times AFTER every `pattern`
#                                match is deleted from the source.
#                          Together those mean EVERY occurrence of the producing construct
#                          was consumed by a plain-literal capture. The moment one of them
#                          holds a non-literal expression (a constant, a concatenation, a
#                          method call) the residue survives and the guard ERRORS with
#                          "cannot prove the literal set" instead of guessing. Blocking is
#                          acceptable; silently extracting the wrong literals is not.
#     unproduced           NOTHING in this tree produces the block (the operation is
#                          unimplemented / the SPI ships zero implementations). This is
#                          the ONLY escape from set equality and it is NOT free. It
#                          requires BOTH:
#                            absence_probes: [{pattern, globs}] — the guard FAILS if any
#                              probe MATCHES, or if a probe's globs match no file at all
#                              (a probe over an empty file set proves nothing); and
#                            verified_by: {test, tag, gradle_task, anchors} — a RUNTIME or
#                              BYTECODE proof of the same absence, because a source regex
#                              cannot see the shapes that actually defeat it (a class that
#                              implements the SPI alongside another interface, a nested or
#                              anonymous class, a generic parameterisation, a @Bean factory
#                              method). The guard checks the named test EXISTS, carries the
#                              named @Tag, contains every declared `anchors:` substring, and
#                              that the tag is wired into the named per-domain gradle task —
#                              so the proof is something R25 actually RUNS, not a citation.
#                          The exemption therefore SELF-DESTRUCTS the moment a producer
#                          appears, forcing a real binding then.
#   An extraction that yields the EMPTY set FAILS: a pattern that matches nothing, or a
#   symbol/method/glob that resolves to nothing, must never read as "no drift".
#
#   verified_by: is ALSO accepted (and validated identically) on an EXTRACTING wire_source,
#   where it pins the static extraction to a live-HTTP assertion of the same vocabulary.
#
#   kind: vocab_scan      — surfaces the contract_enum schema cannot express (an L4
#                           fork-copy's TS union, a java skeleton, a README table).
#                             file:        <path>
#                             canonical:   [T…]      the legal vocabulary
#                             declaration: {…}       MANDATORY — how to extract the
#                                                    DECLARED token set (see below)
#                             require_all: true      every canonical token must appear
#                             forbidden:   [T…]      none of these may appear
#                           Matching for require_all / forbidden is WORD-BOUNDARY token
#                           grep — `SUCCESS` does not match inside `SUCCEEDED`, `FAILED`
#                           does not match inside `FAILED_PERMANENT`.
#
#                           `declaration:` is what makes a vocab_scan EXHAUSTIVE rather
#                           than a denylist: the declared set is parsed out of the file
#                           and compared to `canonical` by EXACT SET EQUALITY, so a
#                           brand-new UNKNOWN token FAILS even though no `forbidden:`
#                           entry names it. A vocab_scan with no `declaration:` FAILS —
#                           no surface can silently escape the exhaustive path.
#                           Three declaration kinds:
#                             ts_union       name: X  — parses `type X = 'A' | 'B' | …`
#                                                       and takes the quoted members. A
#                                                       non-literal member (`| string`)
#                                                       FAILS: it is not checkable.
#                             java_enum_decl name: X  — parses `enum X { A, B, … }`
#                                                       (comment-stripped, constructor
#                                                       args tolerated).
#                             marker_region  marker: M — takes ALL-CAPS tokens between
#                                                       the `M:start` / `M:end` marker
#                                                       comments. For PROSE surfaces.
#
# HONEST SCOPE — what is exhaustive where:
#   ts_union / java_enum_decl  → EXHAUSTIVE over the whole declaration. An unknown
#                                token anywhere in the union/enum body FAILS.
#   marker_region              → EXHAUSTIVE ONLY INSIDE THE DELIMITED REGION. Prose
#                                surfaces (a doc comment, a README) carry dozens of
#                                unrelated ALL-CAPS acronyms (`HTTP`, `UUID`, `POST`,
#                                spec IDs…), so whole-file set equality is NOT sound and
#                                is NOT claimed. Outside the region the only floor is
#                                the `forbidden:` DENYLIST — a status word invented in
#                                prose outside the region is NOT caught. That is a known,
#                                accepted limit, not an oversight: the region is where
#                                the vocabulary is enumerated, and the enumeration is
#                                what a fork-receiver copies.
#
# EXHAUSTIVENESS (the property that makes this non-heuristic): every `enum:` block
# found under `contracts/*.yaml` MUST appear in the manifest. An unclassified block
# FAILS — there is no name inference, no "looks like" matching, no default. The
# reverse also FAILS: a manifest entry addressing a block that no longer exists is a
# stale entry.
#
# NON-VACUITY: zero discovered blocks, zero contract_enum entries, zero vocab_scan
# entries, zero STRUCTURALLY-exhaustive declaration scans (ts_union /
# java_enum_decl), zero EXTRACTING wire_source entries, or zero RESOLVED
# `verified_by` runtime/bytecode bindings all FAIL — the gate cannot be emptied into
# a silent pass, it cannot be degraded into region-markers-only, the wire_only
# population cannot be degraded into all-`unproduced`, and the runtime-truth layer
# cannot be quietly dropped back to source regexes.
#
# Exit: 0 PASS · 1 violation · 2 usage/parse error.
#
# Usage:
#   bash practices/evals/contract_enum_parity_guard.sh
#   bash practices/evals/contract_enum_parity_guard.sh --root DIR
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        *) echo "contract_enum_parity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

python3 - "$REPO_ROOT" <<'PY'
import sys, os, re, glob

repo = sys.argv[1]
MANIFEST = os.path.join(repo, 'practices', 'evals', 'contract-enum-map.yaml')

try:
    import yaml
except ImportError:
    print("contract_enum_parity: PyYAML is required (R25 toolchain preflight)", file=sys.stderr)
    sys.exit(2)

# ── 1. discover every enum: block in contracts/*.yaml ────────────────────────
def esc(tok):
    return str(tok).replace('~', '~0').replace('/', '~1')

def walk(node, path, out):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == 'enum' and isinstance(v, list):
                out.append((path, [str(x) for x in v]))
            else:
                walk(v, path + '/' + esc(k), out)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, path + '/' + str(i), out)

discovered = {}   # (relpath, pointer) -> [tokens]
for f in sorted(glob.glob(os.path.join(repo, 'contracts', '*.yaml'))):
    rel = os.path.relpath(f, repo)
    try:
        doc = yaml.safe_load(open(f, encoding='utf-8'))
    except Exception as ex:
        print(f"FAIL: {rel} is not parseable YAML: {ex}")
        sys.exit(1)
    found = []
    walk(doc, '', found)
    for ptr, toks in found:
        discovered[(rel, ptr)] = toks

# ── 2. java enum constant extraction (grep/parse — no JVM) ───────────────────
def strip_comments(s):
    out = []; i = 0; n = len(s)
    while i < n:
        c = s[i]
        if c == '/' and i + 1 < n and s[i+1] == '*':
            j = s.find('*/', i + 2); i = n if j < 0 else j + 2; continue
        if c == '/' and i + 1 < n and s[i+1] == '/':
            j = s.find('\n', i); i = n if j < 0 else j + 1; out.append('\n'); continue
        if c in '"\'':
            q = c; out.append(c); i += 1
            while i < n:
                out.append(s[i])
                if s[i] == '\\':
                    out.append(s[i+1] if i + 1 < n else ''); i += 2; continue
                if s[i] == q:
                    i += 1; break
                i += 1
            continue
        out.append(c); i += 1
    return ''.join(out)

def split_top(s):
    parts = []; depth = 0; cur = []
    for ch in s:
        if ch in '([{': depth += 1
        elif ch in ')]}': depth -= 1
        if ch == ',' and depth == 0:
            parts.append(''.join(cur)); cur = []
        else:
            cur.append(ch)
    parts.append(''.join(cur))
    return parts

def enums_in_source(text):
    """(simple-name, [constants]) for every enum declared in a java source.

    Handles the three shapes that occur in this tree:
      plain              `public enum WebhookDeliveryStatus { A, /** doc */ B }`
      constructor-arg    `CSV("text/csv", ".csv"), XLSX(...);`
      nested-in-type     `public class X { public enum Status { … } }`
    """
    src = strip_comments(text)
    res = []
    for m in re.finditer(r'\benum\s+(\w+)\s*(?:implements\s+[\w.<>,\s]+?)?\{', src):
        name = m.group(1)
        i = src.index('{', m.end() - 1)
        depth = 0; j = i
        for j in range(i, len(src)):
            if src[j] == '{': depth += 1
            elif src[j] == '}':
                depth -= 1
                if depth == 0: break
        body = src[i+1:j]
        head = body.split(';')[0] if ';' in body else body
        consts = []
        for part in split_top(head):
            t = part.strip()
            if not t: continue
            mm = re.match(r'^(?:@\w+(?:\([^)]*\))?\s*)*([A-Za-z_$][\w$]*)', t)
            if mm and re.fullmatch(r'[A-Z][A-Z0-9_]*', mm.group(1)):
                consts.append(mm.group(1))
        res.append((name, consts))
    return res

def scan_java(root):
    enums = {}
    for f in glob.glob(os.path.join(root, 'backend/src/main/java/**/*.java'), recursive=True):
        text = open(f, encoding='utf-8', errors='ignore').read()
        pm = re.search(r'^\s*package\s+([\w.]+);', strip_comments(text), re.M)
        pkg = pm.group(1) if pm else ''
        outer = os.path.basename(f)[:-5]
        for name, consts in enums_in_source(text):
            fq = f"{pkg}.{name}" if name == outer else f"{pkg}.{outer}.{name}"
            enums[fq] = consts
    return enums

java_enums = scan_java(repo)

# ── 2b. wire_source extraction (P0-2) ────────────────────────────────────────
# A wire_only block is NOT exempt from parity: it declares a producer and the guard
# pulls the literal set out of that producer. Every helper returns (set|None, err).

WIRE_SOURCE_KINDS = ('java_field_literals', 'java_enum_decl', 'java_method_returns',
                     'literal_pattern', 'unproduced')
_STRIPPED_EXT = ('.java', '.ts', '.tsx', '.skeleton')

def read_source(path):
    body = open(path, encoding='utf-8', errors='ignore').read()
    if path.endswith(_STRIPPED_EXT):
        # comments stripped so a javadoc/JSDoc example cannot masquerade as a producer
        return strip_comments(body)
    return body

def resolve_files(spec, single):
    """[abs paths], err — `single` forbids a multi-file match (file: vs files:)."""
    if not spec:
        return None, "no file/files declared"
    pat = os.path.join(repo, str(spec))
    hits = sorted(p for p in glob.glob(pat, recursive=True) if os.path.isfile(p))
    if not hits:
        return None, f"no file matches {spec!r}"
    if single and len(hits) > 1:
        return None, f"{spec!r} matches {len(hits)} files; `file:` must address exactly one"
    return hits, None

# Identifiers an expression may mention and still be a PROVABLE literal set: collection
# factory syntax only, never a value-bearing name. Anything else means the expression
# composes its members from something the guard cannot see, so the extraction is refused.
_STRUCTURAL_IDENTS = {
    'Set', 'List', 'Map', 'Arrays', 'Collections', 'EnumSet', 'HashSet', 'LinkedHashSet',
    'TreeSet', 'ArrayList', 'of', 'copyOf', 'asList', 'unmodifiableSet', 'unmodifiableList',
    'new', 'String', 'java', 'util',
}


def literals_only(expr):
    """(True, None) if `expr` is built ONLY from string literals + collection syntax.

    The fail-closed core (P0-2 round 2). Every value-extracting kind removes the quoted
    literals it captured and then asks: is anything VALUE-BEARING left? A surviving
    identifier means the real member set is partly computed — from a constant, a call, a
    concatenation — and a regex that reported only the literals it happened to recognise
    would be reporting the WRONG set as if it were complete.
    """
    stripped = re.sub(r'"(?:[^"\\]|\\.)*"', ' ', expr)
    for ident in re.findall(r'[A-Za-z_$][\w$]*', stripped):
        if ident not in _STRUCTURAL_IDENTS:
            return False, ident
    return True, None


def src_java_field_literals(src):
    """String literals of a named field's initializer, or of every String constant.

    FAIL-CLOSED both ways: the addressed initializer must be literals-only, and in the
    whole-file form EVERY `static final String` declaration must be a plain literal — a
    computed one would otherwise be silently dropped from the set.
    """
    files, err = resolve_files(src.get('file'), True)
    if err:
        return None, err
    body = read_source(files[0])
    rel = os.path.relpath(files[0], repo)
    symbol = src.get('symbol')
    if symbol:
        m = re.search(r'\b' + re.escape(str(symbol)) + r'\s*=', body)
        if not m:
            return None, f"no field named {symbol!r} in {src.get('file')}"
        j = body.find(';', m.end())
        if j < 0:
            return None, f"initializer of {symbol!r} is unterminated"
        init = body[m.end():j]
        ok, offender = literals_only(init)
        if not ok:
            return None, (f"CANNOT PROVE THE LITERAL SET — the initializer of {symbol!r} in "
                          f"{rel} mentions {offender!r}, so its members are not all plain "
                          f"string literals; the extracted set would silently omit whatever "
                          f"{offender!r} contributes")
        toks = set(re.findall(r'"([^"]*)"', init))
        if not toks:
            return None, f"initializer of {symbol!r} holds no string literal"
        return toks, None
    decls = re.findall(r'static\s+final\s+String\s+(\w+)\s*=\s*([^;]*);', body)
    toks = set()
    for name, init in decls:
        ok, offender = literals_only(init)
        if not ok:
            return None, (f"CANNOT PROVE THE LITERAL SET — `static final String {name}` in "
                          f"{rel} is initialised from {offender!r} rather than a plain string "
                          f"literal, so the constant block is not exhaustively extractable; "
                          f"address a single collection field with `symbol:` instead")
        toks |= set(re.findall(r'"([^"]*)"', init))
    if not toks:
        return None, (f"{src.get('file')} declares no `static final String … = \"…\";` "
                      f"constant; declare `symbol:` to address a collection field instead")
    return toks, None

def src_java_enum_decl(src):
    files, err = resolve_files(src.get('file'), True)
    if err:
        return None, err
    name = str(src.get('name') or '')
    if not name:
        return None, "java_enum_decl requires `name:`"
    found = [c for (n, c) in enums_in_source(open(files[0], encoding='utf-8',
                                                 errors='ignore').read()) if n == name]
    if not found:
        return None, f"no `enum {name} {{ … }}` in {src.get('file')}"
    if len(found) > 1:
        return None, f"more than one `enum {name}` in {src.get('file')} — ambiguous"
    return set(found[0]), None

def src_java_method_returns(src):
    files, err = resolve_files(src.get('files') or src.get('file'), False)
    if err:
        return None, err
    method = str(src.get('method') or '')
    if not method:
        return None, "java_method_returns requires `method:`"
    toks = set()
    bodies = 0
    for f in files:
        body = read_source(f)
        for m in re.finditer(r'\b' + re.escape(method) + r'\s*\(\s*\)\s*\{', body):
            i = body.index('{', m.end() - 1)
            depth = 0
            j = i
            for j in range(i, len(body)):
                if body[j] == '{':
                    depth += 1
                elif body[j] == '}':
                    depth -= 1
                    if depth == 0:
                        break
            bodies += 1
            region = body[i:j]
            # FAIL-CLOSED: every return in the body must be a plain literal. An adapter
            # that computes its slug (`return SLUG;`, `return prefix + "pg";`) is not
            # statically provable, and skipping it would report the OTHER adapters' slugs
            # as if they were the complete set.
            for expr in re.findall(r'\breturn\s+([^;]*);', region):
                if not re.fullmatch(r'\s*"(?:[^"\\]|\\.)*"\s*', expr):
                    return None, (f"CANNOT PROVE THE LITERAL SET — `{method}()` in "
                                  f"{os.path.relpath(f, repo)} returns `{expr.strip()}`, which "
                                  f"is not a plain string literal; the extracted set would "
                                  f"silently omit whatever it evaluates to")
            toks |= set(re.findall(r'\breturn\s+"([^"]*)"\s*;', region))
    if bodies == 0:
        return None, (f"no `{method}()` body in the {len(files)} matched file(s) — an "
                      f"abstract declaration alone produces nothing")
    if not toks:
        return None, f"`{method}()` returns no string literal in the matched file(s)"
    return toks, None

def src_literal_pattern(src):
    """One-capture regex over the producer file(s), FAIL-CLOSED via `residue_probe`.

    A regex is not a parser: `pattern` can silently skip a producing expression it does
    not recognise while still scraping a non-empty set from elsewhere in the file, and
    the guard would report PASS on literals nothing puts on the wire. `residue_probe`
    names the PRODUCING CONSTRUCT; the guard requires it to match at least once in the
    source (else the probe is vacuous) and ZERO times once every `pattern` match is
    deleted — i.e. every occurrence of the construct was consumed by a plain-literal
    capture. Anything else ERRORS ("cannot prove the literal set") instead of guessing.
    """
    files, err = resolve_files(src.get('files') or src.get('file'), 'files' not in src)
    if err:
        return None, err
    pat = src.get('pattern')
    if not pat:
        return None, "literal_pattern requires `pattern:`"
    try:
        rx = re.compile(str(pat))
    except re.error as ex:
        return None, f"pattern is not a valid regex: {ex}"
    if rx.groups != 1:
        return None, f"pattern must have EXACTLY one capture group (has {rx.groups})"
    probe = src.get('residue_probe')
    if not probe:
        return None, ("literal_pattern requires `residue_probe:` — a regex matching the "
                      "PRODUCING CONSTRUCT (not the literal). Without it a `pattern:` that "
                      "silently skips a non-literal producer still returns literals scraped "
                      "from elsewhere in the file, and the guard reports PASS on a vocabulary "
                      "nothing emits")
    try:
        prx = re.compile(str(probe))
    except re.error as ex:
        return None, f"residue_probe is not a valid regex: {ex}"
    toks = set()
    pre_hits = 0
    residue_hits = []
    for f in files:
        body = read_source(f)
        toks |= set(rx.findall(body))
        pre_hits += len(prx.findall(body))
        residue = rx.sub('', body)
        for m in prx.finditer(residue):
            line = residue.count('\n', 0, m.start()) + 1
            residue_hits.append(f"{os.path.relpath(f, repo)}:~{line} {m.group(0)!r}")
    if pre_hits == 0:
        return None, (f"residue_probe {probe!r} matches NOTHING in the {len(files)} matched "
                      f"file(s) — a probe that never fires cannot prove the pattern consumed "
                      f"every producer; point it at the producing construct")
    if len(residue_hits) > 0:
        return None, (f"CANNOT PROVE THE LITERAL SET — residue_probe {probe!r} still matches "
                      f"{residue_hits[:4]} after removing every `pattern` capture: that "
                      f"occurrence of the producing construct does NOT hold a plain string "
                      f"literal, so the extracted set is not the set on the wire. Make the "
                      f"producer a plain literal, or bind this block to a java_enum")
    if not toks:
        return None, (f"pattern {pat!r} matched nothing in the {len(files)} matched "
                      f"file(s) — an unmatched pattern proves no producer")
    return toks, None

def src_unproduced(src):
    """Absence proof: every probe must match ZERO lines over a NON-EMPTY file set."""
    probes = src.get('absence_probes')
    if not isinstance(probes, list) or not probes:
        return None, ("unproduced requires a non-empty `absence_probes:` list "
                      "[{pattern, globs}] — an unchecked exemption is the hole itself")
    for n, p in enumerate(probes):
        if not isinstance(p, dict):
            return None, f"absence_probes[{n}] is not a mapping"
        pat = p.get('pattern')
        globs = p.get('globs')
        if not pat:
            return None, f"absence_probes[{n}] has no `pattern:`"
        if not isinstance(globs, list) or not globs:
            return None, f"absence_probes[{n}] has no `globs:` list"
        try:
            rx = re.compile(str(pat))
        except re.error as ex:
            return None, f"absence_probes[{n}] pattern is not a valid regex: {ex}"
        files = []
        for g in globs:
            files.extend(p2 for p2 in glob.glob(os.path.join(repo, str(g)), recursive=True)
                         if os.path.isfile(p2))
        if not files:
            return None, (f"absence_probes[{n}] globs {globs} match NO file — a probe over "
                          f"an empty file set proves nothing")
        probe_hits = []
        for f in sorted(set(files)):
            text = open(f, encoding='utf-8', errors='ignore').read()
            if rx.search(text):
                probe_hits.append(os.path.relpath(f, repo))
        if len(probe_hits) > 0:
            return None, (f"absence_probes[{n}] pattern {pat!r} MATCHES {probe_hits[:4]} — a "
                          f"producer now exists, so this block must be bound to it "
                          f"(java_enum / an extracting wire_source) instead of exempted")
    return set(), None

def check_verified_by(vb):
    """Validate a runtime/bytecode proof binding -> (ok, err).

    Static source scanning cannot see the shapes that actually defeat it — a class that
    implements an SPI alongside another interface, a nested or anonymous class, a generic
    parameterisation, a @Bean factory method, a value produced by composition rather than
    by a literal. `verified_by:` binds the claim to a test that observes the RUNNING
    system (live HTTP / the Spring bean graph) or its BYTECODE (ArchUnit), and this
    checker makes that binding load-bearing rather than decorative:

      test:         the file must exist on disk;
      tag:          the file must carry `@Tag("<tag>")`;
      gradle_task:  backend/build.gradle.kts must register a Test task by that name whose
                    useJUnitPlatform block includes that tag — so R25 actually RUNS it;
      anchors:      every declared substring must appear in the test, so the binding
                    cannot point at an unrelated test that happens to share the tag.
    """
    if not isinstance(vb, dict) or not vb:
        return False, ("`verified_by:` must be a mapping {test, tag, gradle_task, anchors} "
                       "— a runtime/bytecode proof of the same claim")
    test = vb.get('test')
    tag = vb.get('tag')
    task = vb.get('gradle_task')
    anchors = vb.get('anchors')
    for field, val in (('test', test), ('tag', tag), ('gradle_task', task)):
        if not str(val or '').strip():
            return False, f"`verified_by.{field}:` is required and must be non-empty"
    if not isinstance(anchors, list) or not anchors:
        return False, ("`verified_by.anchors:` must be a non-empty list of substrings the "
                       "test must contain (the contract path it reads, the SPI it probes) "
                       "— without it the binding could name any test carrying the tag")
    tpath = os.path.join(repo, str(test))
    if not os.path.isfile(tpath):
        return False, f"verified_by.test {test!r} does not exist on disk"
    tbody = open(tpath, encoding='utf-8', errors='ignore').read()
    if f'@Tag("{tag}")' not in tbody:
        return False, (f"verified_by.test {test!r} does not carry `@Tag(\"{tag}\")` — the "
                       f"named per-domain task would not run it")
    for a in anchors:
        if str(a) not in tbody:
            return False, (f"verified_by.anchors: {a!r} does not appear in {test!r} — the "
                           f"binding names a test that does not reference this surface")
    gradle = os.path.join(repo, 'backend', 'build.gradle.kts')
    if not os.path.isfile(gradle):
        return False, ("backend/build.gradle.kts not found — the gradle_task binding cannot "
                       "be verified, so the proof is not known to run")
    gbody = open(gradle, encoding='utf-8', errors='ignore').read()
    registered = re.search(r'tasks\.register<Test>\(\s*"' + re.escape(str(task)) + r'"\s*\)', gbody)
    if not registered:
        return False, (f"backend/build.gradle.kts registers no `tasks.register<Test>"
                       f"(\"{task}\")` — the proof is not wired into a per-domain task")
    # The `else gbody` fallback is unreachable in normal operation (the test above already
    # returned). It exists so that a build with that test NEUTERED degrades to "unchecked"
    # rather than crashing, which is what makes the kill-proof flip (exit 1 → 0) meaningful
    # instead of accidental — same rationale as the tolerant extractor lookup below.
    window = gbody[registered.end():registered.end() + 600] if registered else gbody
    inc = re.search(r'includeTags\(([^)]*)\)', window)
    if not inc or f'"{tag}"' not in inc.group(1):
        return False, (f"gradle task {task!r} does not includeTags(\"{tag}\") — the tagged "
                       f"proof would not be selected by the task that is supposed to run it")
    return True, None


WIRE_SOURCE_EXTRACTORS = {
    'java_field_literals': src_java_field_literals,
    'java_enum_decl': src_java_enum_decl,
    'java_method_returns': src_java_method_returns,
    'literal_pattern': src_literal_pattern,
}

# ── 3. manifest ──────────────────────────────────────────────────────────────
if not os.path.exists(MANIFEST):
    print(f"FAIL: manifest not found: {os.path.relpath(MANIFEST, repo)}")
    sys.exit(1)
manifest = yaml.safe_load(open(MANIFEST, encoding='utf-8')) or {}
entries = manifest.get('contract_enums') or []
scans = manifest.get('vocab_scans') or []

violations = []

# ── 4. exhaustiveness, both directions ───────────────────────────────────────
claimed = {}
for idx, e in enumerate(entries):
    key = (e.get('contract'), e.get('pointer'))
    if key in claimed:
        violations.append(f"manifest: duplicate entry for {key[0]}#{key[1]}")
    claimed[key] = e

for key in sorted(discovered):
    if key not in claimed:
        toks = discovered[key]
        violations.append(
            f"UNCLASSIFIED enum block {key[0]}#{key[1]} = {toks} — every enum: block in "
            f"contracts/ MUST be classified in practices/evals/contract-enum-map.yaml "
            f"(java_enum: <FQCN> | wire_only: <reason>)")
for key in sorted(claimed):
    if key not in discovered:
        violations.append(
            f"STALE manifest entry {key[0]}#{key[1]} — no such enum: block on disk "
            f"(the contract moved or was edited; re-point or delete the entry)")

# ── 5. per-entry parity ──────────────────────────────────────────────────────
def as_list(v):
    return list(v) if isinstance(v, list) else []

def compare_sets(where, wire_set, code_set, e, label):
    """Exact set equality modulo declared wire_extra / wire_missing allowances.

    Shared by the java_enum path and the wire_source path so an allowance means the
    same thing (and is non-redundancy-checked the same way) on both.
    """
    extra = set(as_list(e.get('wire_extra')))
    missing = set(as_list(e.get('wire_missing')))
    if (extra or missing) and not str(e.get('reason') or '').strip():
        violations.append(f"{where}: wire_extra / wire_missing require a `reason:`")
    real_extra = wire_set - code_set
    real_missing = code_set - wire_set
    stale_extra = extra - real_extra
    stale_missing = missing - real_missing
    if stale_extra:
        violations.append(
            f"{where}: wire_extra lists {sorted(stale_extra)} which {label} DOES produce — "
            f"stale allowance, delete it")
    if stale_missing:
        violations.append(
            f"{where}: wire_missing lists {sorted(stale_missing)} which ARE on the wire — "
            f"stale allowance, delete it")
    undeclared_extra = real_extra - extra
    undeclared_missing = real_missing - missing
    if undeclared_extra or undeclared_missing:
        parts = []
        if undeclared_extra:
            parts.append(f"wire declares {sorted(undeclared_extra)} which {label} cannot emit")
        if undeclared_missing:
            parts.append(f"{label} produces {sorted(undeclared_missing)} which the wire forbids")
        violations.append(
            f"{where}: ENUM DRIFT vs {label} — " + "; ".join(parts) +
            " — fix the contract, fix the producer, or declare wire_extra/wire_missing + reason")

wire_source_extracted = 0
wire_source_unproduced = 0
runtime_verified = 0

for key in sorted(claimed):
    if key not in discovered:
        continue
    e = claimed[key]
    where = f"{key[0]}#{key[1]}"
    kind = e.get('kind', 'contract_enum')
    if kind != 'contract_enum':
        violations.append(f"{where}: kind must be contract_enum (got {kind!r})")
        continue
    has_java = 'java_enum' in e
    has_wire_only = 'wire_only' in e
    if has_java == has_wire_only:
        violations.append(
            f"{where}: exactly ONE of java_enum / wire_only is required "
            f"(java_enum={has_java}, wire_only={has_wire_only})")
        continue
    if has_wire_only:
        # P0-2: classification is NOT enough. The entry must name a producer and the
        # extracted literal set must EQUAL the block, exactly as a java_enum entry does.
        if not str(e.get('wire_only') or '').strip():
            violations.append(f"{where}: wire_only requires a non-empty reason")
        if 'wire_case' in e:
            violations.append(f"{where}: wire_case is only valid on a java_enum entry "
                              f"(a wire_source is compared to the wire VERBATIM)")
        src = e.get('wire_source')
        skind = src.get('kind') if isinstance(src, dict) else None
        if skind not in WIRE_SOURCE_KINDS:
            if isinstance(src, dict) and src:
                violations.append(
                    f"{where}: wire_source kind {skind!r} is not one of {list(WIRE_SOURCE_KINDS)}")
            else:
                violations.append(
                    f"{where}: MISSING `wire_source:` — a wire_only entry must declare WHERE its "
                    f"literals are produced (kind: {' | '.join(WIRE_SOURCE_KINDS)}); a `wire_only:` "
                    f"reason alone is CLASSIFICATION-ONLY and lets the vocabulary drift freely")
            continue
        # A runtime/bytecode proof is MANDATORY on `unproduced` (an absence claim has no
        # producer region a static extractor could be made to fail closed over) and
        # OPTIONAL — but validated when present — on an extracting kind.
        # isinstance-guarded for the same reason as the tolerant extractor lookup below:
        # with the wire_source-kind membership test NEUTERED, `src` may be absent entirely,
        # and this line must degrade to "unchecked" rather than crash — otherwise that
        # fixture's kill-proof would flip on a traceback instead of on the logic.
        vb_declared = isinstance(src, dict) and 'verified_by' in src
        if vb_declared:
            ok, err = check_verified_by(src.get('verified_by'))
            if not ok:
                violations.append(f"{where}: wire_source ({skind}) verified_by — {err}")
            else:
                runtime_verified += 1

        if skind == 'unproduced':
            for mod in ('wire_extra', 'wire_missing'):
                if mod in e:
                    violations.append(
                        f"{where}: {mod} is meaningless on an `unproduced` wire_source "
                        f"(no set is extracted to allow a difference against)")
            if not vb_declared:
                violations.append(
                    f"{where}: `unproduced` requires `verified_by:` — absence_probes are a "
                    f"SOURCE REGEX, and the shapes that defeat one (an SPI implemented "
                    f"alongside another interface, a nested/anonymous class, a generic "
                    f"parameterisation, a @Bean factory) are exactly the ordinary ways the "
                    f"producer would actually appear. Bind the claim to a runtime/bytecode "
                    f"test {{test, tag, gradle_task, anchors}} that R25 runs")
            _, err = src_unproduced(src)
            if err:
                violations.append(f"{where}: wire_source (unproduced) — {err}")
            else:
                wire_source_unproduced += 1
            continue
        extractor = WIRE_SOURCE_EXTRACTORS.get(skind)
        if extractor is None:
            # Unreachable in normal operation — the membership test above already pinned
            # skind to a known kind. The tolerant .get() exists so that a build with that
            # test NEUTERED degrades to "unchecked" rather than crashing, which is what
            # makes the kill-proof flip (exit 1 → 0) meaningful instead of accidental.
            continue
        produced, err = extractor(src)
        if err:
            violations.append(f"{where}: wire_source ({skind}) not resolvable — {err}")
            continue
        wire_source_extracted += 1
        compare_sets(where, set(discovered[key]), produced, e,
                     f"wire_source {skind} {src.get('file') or src.get('files')}")
        continue

    fq = e['java_enum']
    if fq not in java_enums:
        violations.append(
            f"{where}: java_enum {fq} not found in backend/src/main/java "
            f"(renamed/moved/deleted?)")
        continue
    java_set = set(java_enums[fq])
    wire_raw = discovered[key]
    lower = str(e.get('wire_case', '')).lower() == 'lower'
    if lower:
        bad = [t for t in wire_raw if t != t.lower()]
        if bad:
            violations.append(f"{where}: wire_case: lower declared but {bad} are not lower-case")
        wire_set = {t.upper() for t in wire_raw}
    else:
        bad = [t for t in wire_raw if t != t.upper()]
        if bad:
            violations.append(
                f"{where}: wire tokens {bad} are not upper-case — a Java enum serializes "
                f"as name(); declare `wire_case: lower` + reason if the wire really folds case")
        wire_set = set(wire_raw)

    if 'wire_case' in e and not str(e.get('reason') or '').strip():
        violations.append(f"{where}: wire_case requires a `reason:`")
    compare_sets(where, wire_set, java_set, e, fq)

# ── 6. vocab_scan ────────────────────────────────────────────────────────────
# EXHAUSTIVE path: parse the DECLARED token set out of the file and compare it to
# `canonical` by exact set equality. A denylist can only catch tokens someone
# already thought of; set equality catches the ones nobody did.
STRUCTURAL_DECL_KINDS = ('ts_union', 'java_enum_decl')
DECL_KINDS = STRUCTURAL_DECL_KINDS + ('marker_region',)

def decl_ts_union(body, name):
    """Parse `type <name> = 'A' | 'B' | …` -> (set, err)."""
    m = re.search(r'\btype\s+' + re.escape(str(name)) + r'\s*=\s*([^\n;]*(?:\n\s*\|[^\n;]*)*)', body)
    if not m:
        return None, f"no `type {name} = …` union declaration found"
    rhs = m.group(1).strip().rstrip(';').strip()
    members = [p.strip() for p in rhs.split('|')]
    members = [p for p in members if p]
    if not members:
        return None, f"`type {name}` has an empty right-hand side"
    toks = set()
    for p in members:
        mm = re.fullmatch(r"'([^']*)'|\"([^\"]*)\"", p)
        if not mm:
            return None, (f"member {p!r} of `type {name}` is not a quoted string literal — "
                          f"the union is not exhaustively checkable; narrow the type or "
                          f"move this surface to a marker_region declaration")
        toks.add(mm.group(1) if mm.group(1) is not None else mm.group(2))
    return toks, None

def decl_java_enum(body, name):
    """Parse `enum <name> { A, B, … }` -> (set, err)."""
    found = [c for (n, c) in enums_in_source(body) if n == str(name)]
    if not found:
        return None, f"no `enum {name} {{ … }}` declaration found"
    if len(found) > 1:
        return None, f"more than one `enum {name}` declaration found — ambiguous"
    return set(found[0]), None

def decl_marker_region(body, marker):
    """ALL-CAPS tokens between `<marker>:start` and `<marker>:end` -> (set, err)."""
    marker = str(marker)
    start, end = f"{marker}:start", f"{marker}:end"
    ns, ne = body.count(start), body.count(end)
    if ns != 1 or ne != 1:
        return None, (f"expected exactly one `{start}` and one `{end}` marker "
                      f"(found {ns} / {ne})")
    i = body.index(start) + len(start)
    j = body.index(end)
    if j <= i:
        return None, f"`{end}` marker appears before `{start}`"
    region = body[i:j]
    toks = {t for t in re.findall(r'[A-Za-z_][A-Za-z0-9_]*', region)
            if re.fullmatch(r'[A-Z][A-Z0-9_]*', t)}
    if not toks:
        return None, f"vocabulary region delimited by `{marker}` contains no ALL-CAPS token"
    return toks, None

structural_scans = 0
for s in scans:
    kind = s.get('kind', 'vocab_scan')
    rel = s.get('file')
    if kind != 'vocab_scan':
        violations.append(f"vocab_scans: entry {rel!r} has kind {kind!r}, expected vocab_scan")
        continue
    path = os.path.join(repo, rel or '')
    if not rel or not os.path.isfile(path):
        violations.append(f"vocab_scan: file not found: {rel}")
        continue
    body = open(path, encoding='utf-8', errors='ignore').read()
    tokens = set(re.findall(r'[A-Za-z_][A-Za-z0-9_]*', body))
    canonical = as_list(s.get('canonical'))
    forbidden = as_list(s.get('forbidden'))
    if not canonical:
        violations.append(f"vocab_scan {rel}: `canonical` must be a non-empty list")

    # 6a. secondary floor — denylist + presence (whole file)
    hits = [t for t in forbidden if t in tokens]
    if hits:
        violations.append(
            f"vocab_scan {rel}: forbidden token(s) present: {hits} — this surface must speak "
            f"the canonical vocabulary {canonical}")
    if s.get('require_all'):
        absent = [t for t in canonical if t not in tokens]
        if absent:
            violations.append(
                f"vocab_scan {rel}: require_all — canonical token(s) missing: {absent}")

    # 6b. primary gate — EXACT SET EQUALITY over the declared vocabulary
    decl = s.get('declaration')
    if not isinstance(decl, dict) or not decl:
        violations.append(
            f"vocab_scan {rel}: MISSING `declaration:` block — every vocab_scan must say how "
            f"its declared token set is extracted (kind: {' | '.join(DECL_KINDS)}), because a "
            f"`forbidden:` denylist alone lets an UNKNOWN token pass")
        continue
    dkind = decl.get('kind')
    if dkind not in DECL_KINDS:
        violations.append(
            f"vocab_scan {rel}: declaration kind {dkind!r} is not one of {list(DECL_KINDS)}")
        continue
    if dkind == 'ts_union':
        declared, err = decl_ts_union(body, decl.get('name'))
    elif dkind == 'java_enum_decl':
        declared, err = decl_java_enum(body, decl.get('name'))
    else:
        declared, err = decl_marker_region(body, decl.get('marker'))
    if err:
        violations.append(f"vocab_scan {rel}: declaration ({dkind}) not extractable — {err}")
        continue
    if dkind in STRUCTURAL_DECL_KINDS:
        structural_scans += 1
    canon_set = set(canonical)
    unknown = sorted(declared - canon_set)
    absent = sorted(canon_set - declared)
    if unknown or absent:
        parts = []
        if unknown:
            parts.append(f"declares UNKNOWN token(s) {unknown} that are not in the canonical "
                         f"vocabulary")
        if absent:
            parts.append(f"omits canonical token(s) {absent}")
        scope = ("inside the delimited vocabulary region" if dkind == 'marker_region'
                 else f"in the {dkind} declaration")
        violations.append(
            f"vocab_scan {rel}: VOCABULARY DRIFT {scope} — " + "; ".join(parts) +
            f" — the declared set must EQUAL {sorted(canon_set)}")

# ── 7. non-vacuity ───────────────────────────────────────────────────────────
if not discovered:
    violations.append("ZERO_SCAN — no enum: block found under contracts/*.yaml; the gate would be vacuous")
if not [e for e in entries if 'java_enum' in e]:
    violations.append("ZERO_BINDING — no contract_enum entry binds a java_enum; the gate would be vacuous")
if not scans:
    violations.append("ZERO_VOCAB_SCAN — no vocab_scan entry; the L4 vocabulary axis would be unguarded")
if [e for e in entries if 'wire_only' in e] and not wire_source_extracted:
    violations.append(
        "ZERO_WIRE_SOURCE_EXTRACTION — wire_only entries exist but NONE resolves an "
        "extracting wire_source (all degraded to `unproduced`); the wire_only population "
        "would be exempt from set equality again")
if [e for e in entries if 'wire_only' in e] and not runtime_verified:
    violations.append(
        "ZERO_RUNTIME_VERIFIED — no wire_source resolves a `verified_by:` runtime/bytecode "
        "binding; every claim would rest on source regexes again, which is the exact defect "
        "class this layer exists to backstop")
if not structural_scans:
    violations.append(
        "ZERO_EXHAUSTIVE_DECL — no vocab_scan resolves a structural declaration "
        "(ts_union / java_enum_decl); every surface would be prose-region or denylist "
        "only, so an unknown token in a real type declaration could not be caught")

print(f"[contract_enum_parity] {len(discovered)} enum block(s) across "
      f"{len({k[0] for k in discovered})} contract file(s); "
      f"{len([e for e in entries if 'java_enum' in e])} java-bound, "
      f"{len([e for e in entries if 'wire_only' in e])} wire-only "
      f"({wire_source_extracted} producer-bound, {wire_source_unproduced} absence-proven, "
      f"{runtime_verified} runtime/bytecode-verified); "
      f"{len(scans)} vocab_scan surface(s) ({structural_scans} structurally exhaustive, "
      f"{len(scans) - structural_scans} region-scoped or unresolved); "
      f"{len(java_enums)} java enum(s) indexed")

if violations:
    print(f"FAIL: {len(violations)} contract↔code enum parity violation(s):")
    for v in violations:
        print(f"  {v}")
    sys.exit(1)

print("PASS — every contract enum block is classified, every bound block matches its Java enum, "
      "every wire_only block matches the literal set of its declared producer (extracted "
      "FAIL-CLOSED: a producing construct holding anything but a plain literal ERRORS rather "
      "than being skipped) or carries an absence proof backed by a runtime/bytecode test R25 "
      "runs, and every vocab_scan's declared token set equals its canonical vocabulary")
PY
rc=$?
exit $rc
