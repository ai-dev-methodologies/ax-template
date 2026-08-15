#!/usr/bin/env python3
"""practices/scripts/lib/ax_markers.py — THE single ax:artifact marker parser, one definition.

WHY THIS FILE EXISTS
    Two entry points read the SAME `<!-- ax:artifact ... -->` marker syntax embedded in the
    install skills (`skills/ax-install-{hooks,java-enforcement,react-enforcement}/SKILL.md`):
    guard [112] (a structural lint over the skills as authored) and verify-downstream.sh (which
    actually MATERIALIZES the marked-up blocks into a consumer project's files). If those two
    call sites each grew their own regex-and-string-munging extractor, an extractor bug in one
    could silently diverge from the other — guard [112] could report a marker tree clean while
    verify-downstream.sh renders something else entirely (or vice versa), and nobody would notice
    because nothing would ever compare the two extractions against each other. There is exactly
    one parser so that an extractor bug is a bug in ONE place, visible to both call sites, rather
    than a silent mismeasurement in whichever one happens to be wrong.

MARKER SYNTAX (see the umbrella contract, section 1, for the authoritative spec this mirrors)
    Immediately before a fenced code block that is meant to be materialized into a consumer
    project, a skill file carries a single-line HTML comment marker:

        <!-- ax:artifact id=<kebab-id> path=<consumer-relative-path-or-"-">
                          kind=<file|file-fragment|command> [when=<config-path>]
                          [base=<repo|java.root|react.root>] [substs=<comma-list>]
                          [merge=<json-deep|gradle-dependencies|append|replace>] -->

    ATTRIBUTE VOCABULARY IS CLOSED (P2-109). REQUIRED_ATTRS (`id`, `path`, `kind`) must each be
    present AND non-empty — an absent one used to default to the empty string and sail through
    every downstream check, which is how a marker with no id/path/kind at all was measured to
    lint clean (MISSING_REQUIRED_ATTR). `kind` must be one of REGISTERED_KINDS
    (`file`/`file-fragment`/`command`) — anything else is UNREGISTERED_KIND, never a silent
    pass-through to a harness that would then not know what to do with it. `path=-` (the "this
    artifact installs no file" placeholder) is legal ONLY for `kind=command`
    (INVALID_PATH_FOR_KIND). Any attribute NOT in ALLOWED_ATTRS is UNKNOWN_ATTR: an unrecognized
    name is overwhelmingly a TYPO of a real one (`whn=`, `subst=`, `kidn=`), and silently
    ignoring it means the intended constraint never applied while the marker still looked
    well-formed. The same attribute appearing twice (`id=a id=b`) is DUPLICATE_ATTR — dict()
    would keep the last and drop the first without a word about which the author meant.

    `merge=` names HOW a fragment is combined with the file `path=` already contains (P2-112).
    Before this attribute existed, that decision lived only inside the materializing harness
    (verify-downstream.sh), which sniffed the target's basename/extension and the fragment's own
    text to pick between JSON deep-merge, Gradle dependency-block injection, and append — a
    placement RULE encoded as harness code, invisible to the marker that the rule is about, and
    therefore free to diverge silently from what the skill author intended. The vocabulary is
    exactly four values (REGISTERED_MERGES): `json-deep` (parse both sides as JSON and deep-merge
    the incoming object in), `gradle-dependencies` (insert the fragment's dependency-notation
    lines inside the target's `dependencies { }` block), `append` (concatenate to the end of the
    target), `replace` (overwrite the target wholesale). The rules lint() enforces:
        kind=file           -> merge= OPTIONAL; omitted means `replace` (Artifact.merge stays
                               None and a consumer treats that as replace — writing a whole file
                               IS a replace).
        kind=file-fragment  -> merge= REQUIRED (MISSING_MERGE_FOR_FRAGMENT). A fragment with no
                               declared merge mode is precisely the case where the harness had to
                               guess, which is the defect being closed.
        kind=command        -> merge= FORBIDDEN (MERGE_ON_COMMAND). A command installs no file,
                               so there is nothing for a merge mode to describe.
    An unrecognized value is UNREGISTERED_MERGE. This file only VALIDATES the declaration; acting
    on it is the materializing harness's job.

    `base=` names the directory `path=` is relative to — without it the harness materializing
    these artifacts (verify-downstream.sh) would have to hardcode that convention itself, a
    second copy of a decision that belongs in the marker (A-FIX-2). It defaults to `repo` when
    omitted. The three registered values (REGISTERED_BASES) are the only ones a harness needs to
    know how to resolve; anything else is a Problem (UNREGISTERED_BASE), not a silent guess.

    `path=` MAY itself contain `@@<ns>.<path>@@` substitution tokens, optionally piped through a
    PATH TRANSFORMER: `@@config.java.rootPackage|pkgdir@@` resolves `config.java.rootPackage`
    (e.g. "com.example.backend") and then applies the `pkgdir` transformer (dots -> slashes),
    which is how an ArchUnit test class's install path can depend on the consumer's own Java
    package without the harness special-casing that one artifact. Use render_path() to resolve
    these; every token used in `path=` must also be declared in `substs=`, exactly like a token
    used in the body (lint() checks both, bidirectionally, against the same declaration list).

    Inside the fenced block's BODY, three directives are recognized, using a comment prefix
    DERIVED FROM THE FENCE'S INFO STRING (never guessed, never defaulted):

        fence info string                          comment prefix
        ------------------------------------------  --------------
        bash, sh, yaml, properties                  #
        js, mjs, ts, kotlin, kts, java, json5        //
        anything else                                NOT REGISTERED

    An unregistered fence language is a hard BLOCK, not a silent pass-through: lint() reports it
    as a Problem (UNREGISTERED_FENCE_LANG) and render() refuses to materialize it at all (raises
    RenderError). There is no default/fallback comment style — guessing one would let a consumer
    receive a body that still contains literal `ax:if`/`ax:subst` text because the "directive"
    never actually matched anything.

    The three directives:
        <prefix> ax:if <ns>.<path>      ... <prefix> ax:endif   — delete the lines between when
                                                                    the referenced value is falsy
        <prefix> ax:subst <ns>.<path>                            — substitute the token
                                                                    @@<ns>.<path>@@ on the very
                                                                    NEXT body line
    <ns> is `config` (looked up against the caller-supplied ax.config.json dict) or `env`
    (looked up against caller-supplied --env values).

    CONFIG PATHS ARE CHECKED AGAINST THE SCHEMA (P2-109). A `config.*` reference — in `when=`, in
    an `ax:if`, or in an `ax:subst` — that names a key ax.config.json has no schema for evaluates
    to FALSE at render time and stays false forever: `ax:if config.react.typoField` deletes its
    block on EVERY project, silently, and looks exactly like a correctly-disabled feature. That
    is the whole defect, so lint() cross-checks every config path against
    practices-react/eslint-plugin-ax/schemas/ax.config.schema.json and reports UNKNOWN_CONFIG_PATH
    for one that does not resolve. The schema is passed to lint() AS AN ARGUMENT
    (config_schema_path=) rather than located by this module — a parser that hardcodes the repo
    layout it lives in stops working the moment it is vendored, relocated, or run from a fixture
    tree, and the callers (guard [112], any future harness) already know where their schema is.
    Resolution rules, walking the JSON-Schema tree one dotted segment at a time:
        * a segment matching a key under the current node's `properties` descends into it;
        * a segment against a node with an `additionalProperties` SCHEMA (e.g. `react.alias`,
          whose keys are user-chosen alias prefixes) descends into that schema — the key names
          are open by construction, so no name there can be "unknown";
        * a node of `type: array` (e.g. `stacks`) terminates the walk as an ARRAY-MEMBERSHIP
          test: `config.stacks.react` asks "does stacks[] contain 'react'", exactly matching
          _lookup_condition()'s list branch. The membership segment must be the LAST one — there
          is nothing to descend into past a scalar array element, so a longer path is a real
          error rather than a membership test with trailing junk;
        * anything else does not resolve -> UNKNOWN_CONFIG_PATH.
    `env.*` references are NOT schema-checked: env values are supplied per-invocation by the
    caller, so there is no schema that could enumerate them. Omitting config_schema_path skips
    this one check entirely and leaves the other checks untouched.

    A human-readable explanation may follow a
    directive IN A PARENTHETICAL ON THE SAME LINE — that is not flagged. Free-text conditional
    PROSE on a line that is NOT itself a directive line (e.g. "delete this block if you don't use
    TypeScript") is flagged (FREE_TEXT_CONDITIONAL): a human reading the skill cannot rely on
    prose to be applied consistently, and neither can this parser.

STANDARD LIBRARY ONLY. PyYAML is NEVER imported by this file — the marker syntax is plain
    Markdown + an HTML comment micro-grammar; the only external artifact ever parsed as data is
    the caller-supplied ax.config.json, which is JSON (json is stdlib). Pulling in a YAML library
    here would make this module's own import fail on a toolchain that has yq but not PyYAML,
    which is a real, already-registered failure mode for other guards in this catalog (see
    CLAUDE.md's R25 toolchain-prerequisites section) — this parser must not add itself to that
    list.

PUBLIC API
    discover(skill_paths) -> list[Artifact]
        Structural extraction only. Tolerant: a marker with no fenced block immediately after it,
        or an unregistered fence language, still yields an Artifact (with fence_start_line=None
        or fence_lang left as-authored) so that lint() can report it as a Problem instead of the
        whole discovery pass aborting on the first bad marker in a large skill file.
    render(artifact, config, env) -> str
        Evaluates every ax:if/ax:endif and ax:subst in the artifact's body and returns the final
        text a consumer project would receive. Raises RenderError — never falls back — for an
        unregistered fence language, an unresolved ax:subst reference, an unbalanced ax:if, or
        (as a last-resort backstop) a residual DIRECTIVE-SHAPED line or unsubstituted @@..@@
        token surviving in the rendered output. See the note above _RESIDUAL_DIRECTIVE_RE for why
        this backstop matches directive shapes only, not every occurrence of the substring "ax:".
    render_path(artifact, config, env) -> str
        Resolves every @@<ns>.<path>@@ (optionally `|<transformer>`) token inside artifact.path
        and returns the final consumer-relative path. Shares the same reference resolver as
        render()'s ax:subst handling (_resolve_subst) — a missing reference or an unknown
        transformer both raise RenderError, and so does any residual '@@' surviving the pass.
    lint(artifacts, config_schema_path=None) -> list[Problem]
        Pure structural validation; never raises. See Problem for the (code, message,
        source_file, line) shape. Stable codes used here: MARKER_NO_FENCE, DUPLICATE_ID,
        UNREGISTERED_FENCE_LANG, DIRECTIVE_PREFIX_MISMATCH, UNBALANCED_AXIF, UNREGISTERED_BASE,
        SUBST_DECL_MISMATCH, FREE_TEXT_CONDITIONAL, MISSING_REQUIRED_ATTR, UNREGISTERED_KIND,
        INVALID_PATH_FOR_KIND, UNKNOWN_ATTR, DUPLICATE_ATTR, MISSING_MERGE_FOR_FRAGMENT,
        MERGE_ON_COMMAND, UNREGISTERED_MERGE, UNKNOWN_CONFIG_PATH, CONFIG_SCHEMA_UNREADABLE.
        SUBST_DECL_MISMATCH checks substs= against tokens used in BOTH the body and path= (a
        token in either place must be declared, and a declared token must be used in at least one
        of the two). config_schema_path, when given, enables UNKNOWN_CONFIG_PATH (see the CONFIG
        PATHS note above); when omitted, that single check is skipped and nothing else changes.

CLI
    python3 ax_markers.py lint [--schema <ax.config.schema.json>] <skill.md>...
        Prints one "<file>:<line>: <CODE>: <message>" line per problem found across every given
        file. Exit 1 if any problem was found, else exit 0. Without --schema the config-path
        check is skipped, and the CLI says so on stderr rather than passing quietly.
    python3 ax_markers.py render <id> --skill <skill.md>... [--skill <skill.md>...] \\
                                  --config <ax.config.json> [--env k=v ...]
        Discovers <id> across every --skill file, renders it against the given config/env, and
        writes the result to stdout. Exit 0 on success, 1 if render itself fails (RenderError),
        2 for a usage error (unknown id, bad --env, etc).
"""
import argparse
import json
import re
import sys
from dataclasses import dataclass, field


# ---------------------------------------------------------------------------------------------
# Registered fence languages -> the comment prefix directives use inside that fence's body.
# THIS TABLE IS THE ENTIRE REGISTRY. A fence info string not present here is NOT REGISTERED and
# there is no fallback: lint() reports it, render() refuses to materialize it.
# ---------------------------------------------------------------------------------------------
FENCE_COMMENT_PREFIX = {
    "bash": "#", "sh": "#", "yaml": "#", "properties": "#",
    "js": "//", "mjs": "//", "ts": "//", "kotlin": "//", "kts": "//", "java": "//", "json5": "//",
}

# A-FIX-2: the only values base= is allowed to carry. `repo` (the default when base= is omitted)
# means path= is repo-relative; the other two are relative to the consumer's configured java/react
# root (ax.config.json's java.root / react.root) — resolving those roots is the harness's job,
# not this parser's, so this file only validates that the DECLARED base is one of the three the
# harness knows how to handle.
REGISTERED_BASES = {"repo", "java.root", "react.root"}

# P2-109: the CLOSED attribute vocabulary. ALLOWED_ATTRS is the whole set a marker may carry;
# anything else is a typo of one of these (UNKNOWN_ATTR), never an extension point — silently
# ignoring an unrecognized name means the constraint its author was reaching for never applied.
# REQUIRED_ATTRS must each be present AND non-empty: discover() used to default a missing one to
# "" and every later check treated "" as fine, which is how a marker carrying none of the three
# was measured to lint clean.
ALLOWED_ATTRS = {"id", "path", "kind", "when", "base", "substs", "merge"}
REQUIRED_ATTRS = ("id", "path", "kind")
REGISTERED_KINDS = {"file", "file-fragment", "command"}

# The `path=` placeholder meaning "this artifact installs no file at all". Legal only alongside
# kind=command; on a file/file-fragment it would name a target the harness cannot resolve.
PATH_NONE = "-"

# P2-112: how a fragment is merged into the file path= already contains. Exactly four values —
# see the module header for what each means and which kind= requires/forbids one.
REGISTERED_MERGES = {"json-deep", "gradle-dependencies", "append", "replace"}

# A-FIX-3: path= transformers. Currently one: dotted Java package -> directory path segments,
# needed because an install path like src/test/java/<pkg-as-dirs>/... depends on the consumer's
# own rootPackage and the marker syntax has no other way to express "this dotted value, but as a
# path". Deliberately a small explicit table, not eval() or a format-string escape hatch: an
# unregistered transformer name is a Problem/RenderError, never a silent no-op.
_PATH_TRANSFORMERS = {
    "pkgdir": lambda v: str(v).replace(".", "/"),
}

# The attrs group is `.*?`, not `.+?`, ON PURPOSE (P2-109): `<!-- ax:artifact -->` with no
# attributes at all previously did not MATCH, so discover() skipped it entirely and lint() had
# nothing to report — a marker that names an install artifact and declares nothing about it
# disappeared without a word, the same silent-drop failure the required-attribute checks close
# one step later. It now matches, yields an Artifact with empty id/path/kind, and lint() reports
# MISSING_REQUIRED_ATTR for each. `\b` keeps that from also matching `ax:artifactfoo`.
_MARKER_RE = re.compile(r'^<!--\s*ax:artifact\b\s*(?P<attrs>.*?)\s*-->\s*$')
_ATTR_RE = re.compile(r'(\w+)=(\S+)')
_FENCE_OPEN_RE = re.compile(r'^```([A-Za-z0-9_+-]*)\s*$')
_FENCE_CLOSE_RE = re.compile(r'^```\s*$')

# One regex per known comment style. Both are tried against every body line so that a directive
# authored with the WRONG prefix for its fence's registered language (e.g. `#` inside a `js`
# fence, which is registered for `//`) is still RECOGNIZED as an attempted directive and flagged
# (DIRECTIVE_PREFIX_MISMATCH) rather than silently falling through as ordinary body text — which
# is exactly how a stray literal "ax:if" would leak into a rendered consumer file.
_DIRECTIVE_RE_BY_PREFIX = {
    "#": re.compile(r'^\s*#\s*ax:(if|endif|subst)(?:\s+([\w.]+))?\s*(.*)$'),
    "//": re.compile(r'^\s*//\s*ax:(if|endif|subst)(?:\s+([\w.]+))?\s*(.*)$'),
}

# ax:subst tokens as they appear in BODY text — no pipe/transformer syntax there (that is
# path=-only, see _PATH_TOKEN_RE below).
_TOKEN_RE = re.compile(r'@@([\w.]+)@@')

# ax:subst tokens as they appear in path= — same dotted-path grammar, plus an optional
# `|<transformer-name>` suffix (A-FIX-3). Group 1 is the ns.path identity that substs= must
# declare; group 2, if present, is the transformer name.
_PATH_TOKEN_RE = re.compile(r'@@([\w.]+)(?:\|(\w+))?@@')

# A-FIX-1: the render() residual backstop, NARROWED to directive shapes and unsubstituted
# tokens only — never a bare substring search for "ax:". The original bare `"ax:" in result`
# check was measured to reject perfectly ordinary rendered content: an ESLint flat-config
# artifact whose body legitimately contains `settings: { ax: axConfig.react }` and
# `plugins: { ax: axPlugin }` (the vendor-documented unquoted-key spelling — quoting the key as
# 'ax' to dodge the false positive is non-conformant and was rejected) was refused by render()
# even though nothing there is a leftover directive. The backstop's actual job is to catch a
# directive that never matched _DIRECTIVE_RE_BY_PREFIX (wrong comment prefix for the fence's
# registered language, see DIRECTIVE_PREFIX_MISMATCH) and therefore survived verbatim as body
# text, plus an ax:subst token nothing replaced — both real failure shapes, neither of which
# needs to match arbitrary "ax:" text to be caught.
_RESIDUAL_DIRECTIVE_RE = re.compile(r'(?:#|//)\s*ax:(?:if|endif|subst)\b')
_RESIDUAL_TOKEN_RE = re.compile(r'@@[^@\s]+@@')

_FREE_TEXT_PATTERNS = [
    re.compile(r'delete this block', re.I),
    re.compile(r'omit this', re.I),
    re.compile(r'swap for', re.I),
    re.compile(r'skip the', re.I),
]


@dataclass
class Artifact:
    id: str
    path: str
    kind: str
    when: "str | None"
    substs: list
    fence_lang: "str | None"
    body: str
    source_file: str
    marker_line: int
    fence_start_line: "int | None"
    fence_end_line: "int | None"
    base: str = "repo"  # A-FIX-2. Appended with a default so existing callers keyword-construct
                        # this the same way; discover() always sets it explicitly (attrs.get
                        # default "repo" applies only when the marker omits base= outright).
    merge: "str | None" = None  # P2-112. None = not declared; on kind=file that MEANS `replace`
                                # (writing a whole file is a replace), on kind=file-fragment it
                                # is MISSING_MERGE_FOR_FRAGMENT, on kind=command it is correct.
    raw_attrs: list = field(default_factory=list)
    # The marker's attributes as an ORDERED list of (name, value) pairs, exactly as authored.
    # dict() collapses `id=a id=b` to one entry and drops any unrecognized name's evidence, so
    # DUPLICATE_ATTR and UNKNOWN_ATTR are undecidable from the parsed fields alone — this keeps
    # the pre-collapse truth around for lint() to check against.


@dataclass
class Problem:
    code: str
    message: str
    source_file: str
    line: int


class RenderError(RuntimeError):
    """Raised by render() when an artifact cannot be safely materialized.

    Deliberately never caught inside render() itself and never converted into a fallback value —
    an unregistered fence language, an unresolved ax:subst reference, or a residual 'ax:' token
    in the output all mean the consumer would otherwise receive a body that is subtly wrong
    (or literally contains parser directive text), which is worse than refusing outright.
    """


# ---------------------------------------------------------------------------------------------
# discover()
# ---------------------------------------------------------------------------------------------

def discover(skill_paths):
    """Extract every ax:artifact marker + its immediately-following fenced block.

    Tolerant by design: a marker with no fence right after it, or with an unterminated fence,
    still produces an Artifact (fence_start_line/fence_end_line left None, body=""), so lint()
    can report the specific problem instead of the whole pass raising on the first bad marker.
    Fence-language registration is likewise NOT enforced here — that is lint()'s job to report
    and render()'s job to refuse; discover() only extracts what is actually on disk.
    """
    artifacts = []
    for path in skill_paths:
        with open(path, encoding="utf-8") as f:
            lines = f.read().split("\n")
        n = len(lines)
        i = 0
        while i < n:
            m = _MARKER_RE.match(lines[i])
            if not m:
                i += 1
                continue
            marker_line_no = i + 1
            raw_attrs = _ATTR_RE.findall(m.group("attrs"))
            attrs = dict(raw_attrs)
            substs_raw = attrs.get("substs", "")
            substs = [s for s in substs_raw.split(",") if s] if substs_raw else []

            fence_lang = None
            fence_start_line = None
            fence_end_line = None
            body = ""

            nxt = i + 1
            if nxt < n:
                fm = _FENCE_OPEN_RE.match(lines[nxt])
                if fm:
                    fence_lang = fm.group(1) or ""
                    fence_start_line = nxt + 1
                    close_idx = None
                    j = nxt + 1
                    while j < n:
                        if _FENCE_CLOSE_RE.match(lines[j]):
                            close_idx = j
                            break
                        j += 1
                    if close_idx is not None:
                        fence_end_line = close_idx + 1
                        body = "\n".join(lines[nxt + 1:close_idx])
                        i = close_idx
                    # else: unterminated fence — fence_end_line stays None, body stays ""

            artifacts.append(Artifact(
                id=attrs.get("id", ""),
                path=attrs.get("path", ""),
                kind=attrs.get("kind", ""),
                when=attrs.get("when"),
                substs=substs,
                fence_lang=fence_lang,
                body=body,
                source_file=path,
                marker_line=marker_line_no,
                fence_start_line=fence_start_line,
                fence_end_line=fence_end_line,
                base=attrs.get("base", "repo"),
                merge=attrs.get("merge"),
                raw_attrs=raw_attrs,
            ))
            i += 1
    return artifacts


# ---------------------------------------------------------------------------------------------
# lint()
# ---------------------------------------------------------------------------------------------

def _config_path_resolves(segments, schema):
    """Does `config.<segments>` name something ax.config.schema.json actually declares?

    See the module header's CONFIG PATHS note for the full rationale; the walk itself is four
    rules — `properties` descent, open-keyed `additionalProperties` descent, array-membership
    termination, and otherwise unresolved. Returns True/False; never raises on a malformed
    schema node (a non-dict node simply does not resolve).
    """
    if not segments:
        return False
    node = schema
    for i, seg in enumerate(segments):
        if not isinstance(node, dict):
            return False
        if node.get("type") == "array" or "items" in node:
            # config.stacks.react == "is 'react' an element of stacks[]". The element is a
            # scalar, so this segment must be the last one — a longer path has nowhere to go.
            return i == len(segments) - 1
        props = node.get("properties")
        if isinstance(props, dict) and seg in props:
            node = props[seg]
            continue
        additional = node.get("additionalProperties")
        if isinstance(additional, dict):
            node = additional
            continue
        return False
    return True


def _check_config_ref(ref, schema, problems, source_file, line, where):
    """Cross-check one `<ns>.<path>` reference against the schema. `env.*` is deliberately not
    checked (no schema could enumerate caller-supplied values) and a non-config/env namespace is
    already caught at render time, so this only ever reports UNKNOWN_CONFIG_PATH."""
    ns, segments = _split_token(ref)
    if ns != "config":
        return
    if not _config_path_resolves(segments, schema):
        problems.append(Problem(
            "UNKNOWN_CONFIG_PATH",
            f"{where} references {ref!r}, which ax.config.schema.json does not declare; "
            f"an unknown config path evaluates to FALSE forever instead of erroring",
            source_file, line))


def lint(artifacts, config_schema_path=None):
    """Pure structural validation. Never raises; returns the full list of Problems found.

    config_schema_path, when given, is the path to ax.config.schema.json and enables the
    UNKNOWN_CONFIG_PATH check over every `config.*` reference in when=/ax:if/ax:subst. It is an
    ARGUMENT rather than a constant so this parser never hardcodes the repo layout it happens to
    live in (see the module header). Omitting it skips that one check and nothing else.
    """
    problems = []

    schema = None
    if config_schema_path is not None:
        try:
            with open(config_schema_path, encoding="utf-8") as f:
                schema = json.load(f)
        except (OSError, ValueError) as exc:
            # Reported, never swallowed: a schema that cannot be read means the config-path
            # check did not run, and a caller that asked for it must not mistake silence for a
            # clean result.
            problems.append(Problem(
                "CONFIG_SCHEMA_UNREADABLE",
                f"config schema {config_schema_path!r} could not be read: {exc}",
                config_schema_path, 0))
            schema = None

    by_id = {}
    for a in artifacts:
        by_id.setdefault(a.id, []).append(a)
    for aid, group in by_id.items():
        if len(group) > 1:
            first = group[0]
            for dup in group[1:]:
                problems.append(Problem(
                    "DUPLICATE_ID",
                    f"duplicate artifact id {aid!r} (first declared at "
                    f"{first.source_file}:{first.marker_line})",
                    dup.source_file, dup.marker_line))

    for a in artifacts:
        # ── P2-109: attribute well-formedness, BEFORE anything downstream trusts these values ──
        seen = set()
        for name, _value in a.raw_attrs:
            if name in seen:
                problems.append(Problem(
                    "DUPLICATE_ATTR",
                    f"attribute {name!r} appears more than once; the later value silently wins",
                    a.source_file, a.marker_line))
            seen.add(name)
            if name not in ALLOWED_ATTRS:
                problems.append(Problem(
                    "UNKNOWN_ATTR",
                    f"attribute {name!r} is not registered (registered: "
                    f"{sorted(ALLOWED_ATTRS)}); an unrecognized name is a typo whose intended "
                    f"constraint would never apply",
                    a.source_file, a.marker_line))

        for req in REQUIRED_ATTRS:
            if not str(getattr(a, req) or "").strip():
                problems.append(Problem(
                    "MISSING_REQUIRED_ATTR",
                    f"required attribute {req}= is missing or empty",
                    a.source_file, a.marker_line))

        if a.kind and a.kind not in REGISTERED_KINDS:
            problems.append(Problem(
                "UNREGISTERED_KIND",
                f"kind={a.kind!r} is not registered (registered: {sorted(REGISTERED_KINDS)})",
                a.source_file, a.marker_line))
        elif a.path == PATH_NONE and a.kind and a.kind != "command":
            problems.append(Problem(
                "INVALID_PATH_FOR_KIND",
                f"path={PATH_NONE!r} means 'installs no file' and is legal only for "
                f"kind=command, not kind={a.kind!r}",
                a.source_file, a.marker_line))

        # ── P2-112: the merge= placement contract ──
        if a.merge is not None and a.merge not in REGISTERED_MERGES:
            problems.append(Problem(
                "UNREGISTERED_MERGE",
                f"merge={a.merge!r} is not registered (registered: {sorted(REGISTERED_MERGES)})",
                a.source_file, a.marker_line))
        if a.kind == "file-fragment" and a.merge is None:
            problems.append(Problem(
                "MISSING_MERGE_FOR_FRAGMENT",
                "kind=file-fragment must declare merge= (one of "
                f"{sorted(REGISTERED_MERGES)}); without it the materializing harness has to "
                "guess the placement rule from the target's name and the fragment's text",
                a.source_file, a.marker_line))
        if a.kind == "command" and a.merge is not None:
            problems.append(Problem(
                "MERGE_ON_COMMAND",
                f"kind=command installs no file, so merge={a.merge!r} describes nothing",
                a.source_file, a.marker_line))

        if schema is not None and a.when:
            _check_config_ref(a.when, schema, problems, a.source_file, a.marker_line, "when=")

        if a.base not in REGISTERED_BASES:
            problems.append(Problem(
                "UNREGISTERED_BASE",
                f"base={a.base!r} is not registered (registered: {sorted(REGISTERED_BASES)})",
                a.source_file, a.marker_line))

        if a.fence_start_line is None:
            problems.append(Problem(
                "MARKER_NO_FENCE",
                f"marker id={a.id!r} is not immediately followed by a fenced code block",
                a.source_file, a.marker_line))
            continue
        if a.fence_end_line is None:
            problems.append(Problem(
                "MARKER_NO_FENCE",
                f"marker id={a.id!r} opens a fenced code block that is never closed",
                a.source_file, a.fence_start_line))
            continue
        if a.fence_lang not in FENCE_COMMENT_PREFIX:
            problems.append(Problem(
                "UNREGISTERED_FENCE_LANG",
                f"fence language {a.fence_lang!r} is not registered (registered: "
                f"{sorted(FENCE_COMMENT_PREFIX)})",
                a.source_file, a.fence_start_line))
            continue

        expected_prefix = FENCE_COMMENT_PREFIX[a.fence_lang]
        body_lines = a.body.split("\n") if a.body else [""]
        base_line = a.fence_start_line
        if_depth = 0
        used_tokens = set(_TOKEN_RE.findall(a.body))
        # A-FIX-3: a token referenced in path= is exactly as much a "use" as one in the body —
        # substs= is the single declaration list for both.
        used_tokens |= {m.group(1) for m in _PATH_TOKEN_RE.finditer(a.path)}

        for offset, line in enumerate(body_lines):
            line_no = base_line + 1 + offset
            matched = False
            for prefix, rex in _DIRECTIVE_RE_BY_PREFIX.items():
                dm = rex.match(line)
                if not dm:
                    continue
                matched = True
                kind, token = dm.group(1), dm.group(2)
                if prefix != expected_prefix:
                    problems.append(Problem(
                        "DIRECTIVE_PREFIX_MISMATCH",
                        f"directive uses prefix {prefix!r} but fence language "
                        f"{a.fence_lang!r} is registered for prefix {expected_prefix!r}",
                        a.source_file, line_no))
                    break
                if kind == "if":
                    if_depth += 1
                    # An ax:if is the reference shape that FAILS SILENTLY when misspelled — a
                    # config path nothing declares is falsy forever, so the block is deleted on
                    # every project and looks like a deliberately-disabled feature.
                    if schema is not None and token:
                        _check_config_ref(token, schema, problems, a.source_file, line_no,
                                          "ax:if")
                elif kind == "endif":
                    if if_depth == 0:
                        problems.append(Problem(
                            "UNBALANCED_AXIF", "ax:endif with no matching ax:if",
                            a.source_file, line_no))
                    else:
                        if_depth -= 1
                elif kind == "subst" and token:
                    used_tokens.add(token)
                break
            if not matched:
                for pat in _FREE_TEXT_PATTERNS:
                    if pat.search(line):
                        problems.append(Problem(
                            "FREE_TEXT_CONDITIONAL",
                            f"free-text conditional prose outside any directive line: "
                            f"{line.strip()!r}",
                            a.source_file, line_no))
                        break

        if if_depth > 0:
            problems.append(Problem(
                "UNBALANCED_AXIF", f"{if_depth} unclosed ax:if block(s)",
                a.source_file, a.fence_start_line))

        # Every substitution reference (ax:subst directive, @@..@@ in the body, @@..@@ in path=)
        # lands in used_tokens, so one sweep here covers all three places a config path can be
        # spelled wrong. Anchored at the marker line: the declaration list is what a reader
        # fixes, and the same token may appear in several body lines.
        if schema is not None:
            for token in sorted(used_tokens):
                _check_config_ref(token, schema, problems, a.source_file, a.marker_line,
                                  "ax:subst/@@..@@")

        declared = set(a.substs)
        for missing in sorted(used_tokens - declared):
            problems.append(Problem(
                "SUBST_DECL_MISMATCH",
                f"{missing!r} is used via ax:subst/@@..@@ but not declared in substs=",
                a.source_file, a.marker_line))
        for unused in sorted(declared - used_tokens):
            problems.append(Problem(
                "SUBST_DECL_MISMATCH",
                f"{unused!r} is declared in substs= but never used in the body",
                a.source_file, a.marker_line))

    return problems


# ---------------------------------------------------------------------------------------------
# render()
# ---------------------------------------------------------------------------------------------

def _split_token(token):
    ns, _, remainder = token.partition(".")
    segments = remainder.split(".") if remainder else []
    return ns, segments


def _lookup_condition(ns, segments, config, env):
    """Evaluate an ax:if reference to a bool. Missing/absent paths are FALSY, not an error —
    this is feature-flag semantics: "the config doesn't mention it" and "it is off" are the
    same fact for a conditional block. (ax:subst uses the stricter _resolve_subst below, where a
    missing reference IS an error — a substitution has nowhere sensible to fall back to.)

    A path may pass through a dict (ordinary key lookup) or terminate at a list, in which case
    the NEXT segment is treated as a membership test against that list — this lets
    `config.stacks.react` mean "does the stacks array contain 'react'" using the same dotted
    syntax `config.react.typescript` uses for a plain nested boolean.
    """
    if ns == "env":
        key = ".".join(segments)
        return bool(env.get(key)) if key in env else False
    if ns != "config":
        raise RenderError(f"unknown namespace {ns!r} (expected 'config' or 'env')")
    current = config
    for seg in segments:
        if isinstance(current, dict):
            if seg not in current:
                return False
            current = current[seg]
        elif isinstance(current, list):
            return seg in current
        else:
            return False
    return bool(current)


def _resolve_subst(token, config, env):
    """Resolve an ax:subst reference to its literal value. Raises RenderError if it does not
    resolve — unlike ax:if, there is no sensible falsy default for a value that is about to be
    spliced into rendered output."""
    ns, segments = _split_token(token)
    if ns == "env":
        key = ".".join(segments)
        if key not in env:
            raise RenderError(
                f"ax:subst token 'env.{key}' has no supplied --env value")
        return env[key]
    if ns != "config":
        raise RenderError(f"unknown namespace {ns!r} in ax:subst token {token!r}")
    current = config
    for seg in segments:
        if not isinstance(current, dict) or seg not in current:
            raise RenderError(
                f"ax:subst token 'config.{'.'.join(segments)}' does not resolve "
                f"against the supplied config")
        current = current[seg]
    return current


def render(artifact, config, env):
    """Render <artifact>'s body against <config> (a dict, e.g. json.load'd ax.config.json) and
    <env> (a flat dict of caller-supplied values). Returns the final text. Raises RenderError —
    never silently falls back — for an unregistered fence language, an unresolved ax:subst
    reference, an unbalanced ax:if/ax:endif, or (last-resort backstop) any 'ax:' token still
    present in the rendered output.
    """
    if artifact.fence_lang not in FENCE_COMMENT_PREFIX:
        raise RenderError(
            f"fence language {artifact.fence_lang!r} is not registered; refusing to render "
            f"id={artifact.id!r}")
    prefix = FENCE_COMMENT_PREFIX[artifact.fence_lang]
    directive_re = _DIRECTIVE_RE_BY_PREFIX[prefix]

    lines = artifact.body.split("\n") if artifact.body else [""]
    out = []
    cond_stack = []
    pending_subst_token = None

    for line in lines:
        dm = directive_re.match(line)
        if dm:
            kind, token = dm.group(1), dm.group(2)
            enabled_before = all(cond_stack) if cond_stack else True
            if kind == "if":
                if not token:
                    raise RenderError(f"malformed ax:if directive (no reference): {line!r}")
                truth = _lookup_condition(*_split_token(token), config, env) \
                    if enabled_before else False
                cond_stack.append(truth)
                continue
            if kind == "endif":
                if not cond_stack:
                    raise RenderError(
                        f"ax:endif with no matching ax:if in id={artifact.id!r}")
                cond_stack.pop()
                continue
            if kind == "subst":
                if not token:
                    raise RenderError(f"malformed ax:subst directive (no reference): {line!r}")
                if enabled_before:
                    pending_subst_token = token
                continue
            raise RenderError(f"unknown directive kind {kind!r} in id={artifact.id!r}")

        enabled = all(cond_stack) if cond_stack else True
        if not enabled:
            pending_subst_token = None
            continue
        if pending_subst_token is not None:
            value = _resolve_subst(pending_subst_token, config, env)
            token_literal = "@@" + pending_subst_token + "@@"
            if token_literal not in line:
                raise RenderError(
                    f"ax:subst declared {pending_subst_token!r} but the following body line "
                    f"does not contain {token_literal!r}: {line!r}")
            line = line.replace(token_literal, str(value))
            pending_subst_token = None
        out.append(line)

    if cond_stack:
        raise RenderError(f"unclosed ax:if block(s) in id={artifact.id!r}")

    result = "\n".join(out)
    # A-FIX-1: narrowed to directive shapes and unsubstituted @@..@@ tokens — see
    # _RESIDUAL_DIRECTIVE_RE's definition above for the measured false positive (an ESLint flat
    # config's `settings: { ax: ... }` / `plugins: { ax: axPlugin }`) that a bare `"ax:" in
    # result` check used to reject.
    if _RESIDUAL_DIRECTIVE_RE.search(result) or _RESIDUAL_TOKEN_RE.search(result):
        raise RenderError(
            f"residual directive or unsubstituted '@@..@@' token survived render for "
            f"id={artifact.id!r}; refusing to return partially-rendered output (likely a "
            f"directive with a prefix the fence's registered language does not use)")
    return result


def render_path(artifact, config, env):
    """Resolve every @@<ns>.<path>@@[|<transformer>] token in artifact.path (A-FIX-3).

    Shares _resolve_subst with render()'s ax:subst handling — same missing-reference behavior
    (RenderError, no falsy fallback: a path is not a place to silently substitute nothing). An
    unregistered transformer name is likewise a hard RenderError, and so is any '@@' surviving
    the substitution pass (a malformed token _PATH_TOKEN_RE did not recognize at all).
    """
    def _sub(m):
        token, transformer = m.group(1), m.group(2)
        value = _resolve_subst(token, config, env)
        if transformer is not None:
            fn = _PATH_TRANSFORMERS.get(transformer)
            if fn is None:
                raise RenderError(
                    f"unknown path transformer {transformer!r} in id={artifact.id!r} "
                    f"(registered: {sorted(_PATH_TRANSFORMERS)})")
            value = fn(value)
        return str(value)

    result = _PATH_TOKEN_RE.sub(_sub, artifact.path)
    if "@@" in result:
        raise RenderError(
            f"residual '@@' survived path render for id={artifact.id!r}: {result!r}")
    return result


# ---------------------------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------------------------

def _cli_lint(argv):
    ap = argparse.ArgumentParser(prog="ax_markers.py lint")
    ap.add_argument("--schema", default=None,
                    help="path to ax.config.schema.json; enables the UNKNOWN_CONFIG_PATH check")
    ap.add_argument("files", nargs="*")
    args = ap.parse_args(argv)
    if not args.files:
        print("lint: at least one skill file is required", file=sys.stderr)
        return 2
    if args.schema is None:
        # Said out loud, not skipped quietly: a caller who does not know this check was off
        # would read a clean exit 0 as "no unknown config paths", which it does not mean.
        print("lint: note -- --schema not given, so the UNKNOWN_CONFIG_PATH check is SKIPPED",
              file=sys.stderr)
    problems = lint(discover(args.files), config_schema_path=args.schema)
    for p in problems:
        print(f"{p.source_file}:{p.line}: {p.code}: {p.message}")
    return 1 if problems else 0


def _cli_render(argv):
    ap = argparse.ArgumentParser(prog="ax_markers.py render")
    ap.add_argument("artifact_id")
    ap.add_argument("--skill", action="append", required=True)
    ap.add_argument("--config", required=True)
    ap.add_argument("--env", action="append", default=[])
    args = ap.parse_args(argv)

    with open(args.config, encoding="utf-8") as f:
        config = json.load(f)

    env = {}
    for kv in args.env:
        k, sep, v = kv.partition("=")
        if not sep:
            print(f"render: --env value {kv!r} is not of the form k=v", file=sys.stderr)
            return 2
        env[k] = v

    artifacts = discover(args.skill)
    matches = [a for a in artifacts if a.id == args.artifact_id]
    if not matches:
        print(f"render: no artifact with id={args.artifact_id!r} found in {args.skill}",
              file=sys.stderr)
        return 2

    try:
        sys.stdout.write(render(matches[0], config, env))
    except RenderError as exc:
        print(f"render: {exc}", file=sys.stderr)
        return 1
    return 0


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if not argv:
        print("usage: ax_markers.py lint [--schema <ax.config.schema.json>] <file>... | "
              "render <id> --skill <file>... --config <file> [--env k=v ...]", file=sys.stderr)
        return 2
    cmd, rest = argv[0], argv[1:]
    if cmd == "lint":
        return _cli_lint(rest)
    if cmd == "render":
        return _cli_render(rest)
    print(f"unknown subcommand {cmd!r}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
