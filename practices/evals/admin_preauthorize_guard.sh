#!/usr/bin/env bash
# practices/evals/admin_preauthorize_guard.sh
#
# ─────────────────────────────────────────────────────────────────────────────
# WHAT THIS IS — a static LINT (NOT an authoritative authorization proof)
# ─────────────────────────────────────────────────────────────────────────────
# This guard is a cheap, purely-LOCAL static lint over the controller source. It
# reads ONLY the controller file — it does NOT parse SecurityConfig and it does
# NOT perform adversarial SpEL evaluation. It catches exactly two shapes on a
# *mutating* admin endpoint:
#
#   (a) MISSING authorization — no effective admin @PreAuthorize at all; and
#   (b) OBVIOUSLY-ineffective authorization that is cheaply decidable by
#       inspection — specifically:
#         • permitAll() / anonymous() / isAnonymous()
#         • authenticated() / isAuthenticated() / fullyAuthenticated() / rememberMe() alone
#         • a non-admin authority (e.g. hasAuthority('ROLE_USER'))
#         • a hasAny*(...) that mixes in any non-admin alternative
#         • @PostAuthorize used to "protect" a MUTATION (authorization runs AFTER
#           the side effect — too late; only @PreAuthorize gates a mutation)
#         • a leading NEGATION of the admin predicate (!hasAuthority('ROLE_ADMIN'))
#         • a trivial always-true disjunction (... or true / ... || true /
#           ... or isAnonymous() / ... or permitAll())
#         • an empty or non-literal (named-constant) SpEL argument
#
# ─────────────────────────────────────────────────────────────────────────────
# WHAT THIS IS **NOT** — adversarial SpEL is OUT OF LINT SCOPE (documented)
# ─────────────────────────────────────────────────────────────────────────────
# A bash/regex lint cannot decide arbitrary Spring SpEL. A DELIBERATELY-crafted
# weakening expression that is not one of the cheap shapes above — e.g.
#   @PreAuthorize("hasAuthority('ROLE_ADMIN') or someComplexAlwaysTrue()")
# where `someComplexAlwaysTrue()` is an opaque bean method that always returns
# true — will PASS this lint. That is a CODE-REVIEW / malicious-insider concern,
# NOT what a static presence-lint defends. The AUTHORITATIVE BFLA control is:
#   1. the per-domain *ComplianceTest integration tests that actually assert
#      HTTP 403 for a non-admin caller on the privileged endpoint — e.g.
#      AnnouncementComplianceTest#authz_memberCannotWrite_... (ANN-AUTHZ-001,
#      MEMBER POST /api/admin/announcements → 403),
#      SessionComplianceTest#authz_003_adminCanForceRevokeAnyUserSession
#      (SESS-AUTHZ-003, non-admin DELETE /api/admin/sessions/{id} → 403), and
#      TagComplianceTest#authz_002_memberCannotMutateButCanAttach
#      (TAG-AUTHZ-002) — run via the `./gradlew test{Domain}` AUTHZ items, and
#   2. SecurityConfig.java's `authorizeHttpRequests` matcher chain
#      (`/api/admin/**` → hasAuthority("ROLE_ADMIN")), enforced at runtime with
#      @EnableMethodSecurity active.
# (AuthzParityViolationProofTest / AccessGrantViolationProofTest are structural
#  cell non-vacuity proofs for OTHER domains — they contain no 403 assertion and
#  are NOT the BFLA runtime control; do not cite them as 403 integration tests.)
# This lint is a SUPPLEMENTARY defense-in-depth regression net for the "admin
# mutating endpoint lost / never had its @PreAuthorize annotation" shape; it
# complements those controls, it does not replace them.
#
# ─────────────────────────────────────────────────────────────────────────────
# WHY PURELY LOCAL — the SecurityConfig-parsing bypass class is MOOT
# ─────────────────────────────────────────────────────────────────────────────
# A prior revision of this guard tried to CREDIT admin coverage by statically
# parsing the Spring `SecurityConfig.java` `authorizeHttpRequests` matcher chain
# (path Ant-matching + declared-order + verb scoping). A cross-family reviewer
# then found FOUR distinct static-analysis bypasses across three rounds
# (verb-scoped matcher; multiple / unscoped filter chains; a
# `@RequestMapping(path = ...)` alias resolving to the wrong matcher; …), because
# exhaustively modelling Spring Security's authorization from source is not
# statically decidable — every hardening left another shape to exploit. Removing
# SecurityConfig crediting ENTIRELY ends that whack-a-mole: with no config chain
# to model there is nothing to bypass. The required control is now a purely-local,
# decidable property — a method-/class-level `@PreAuthorize` requiring ROLE_ADMIN.
#
# ─────────────────────────────────────────────────────────────────────────────
# ADMIN-SURFACE DETECTION (WIDENED — round-4 codex detection-scope gap)
# ─────────────────────────────────────────────────────────────────────────────
# A controller under
#   <root>/backend/src/main/java/com/ax/template/authblueprint/**
# is treated as an ADMIN SURFACE if ANY of:
#   • its class NAME ends `AdminController`, OR
#   • its class-level `@RequestMapping` path contains `/admin`, OR
#   • (WIDENED) ANY of its *mutating* handler methods carries a METHOD-LEVEL
#     mapping whose path is under `/api/admin` (or `/api/v1/admin`) — this catches
#     controllers like OfferEligibilityController / TaxApplicationController that
#     mix a few `/api/admin/...` mutations into an otherwise non-admin class and
#     are NOT named *AdminController and have NO class-level `/admin` mapping.
#
# Which endpoints must carry authz (PER-ENDPOINT, so a mixed controller's
# non-admin mutations are NOT falsely flagged):
#   • if the controller is admin-surface by NAME or by CLASS-LEVEL `/admin` path
#     → EVERY mutating mapped endpoint must carry effective admin @PreAuthorize
#     (the whole class is an admin surface); otherwise
#   • only the mutating endpoints whose OWN method-level mapping path is under
#     `/api/admin` (or `/api/v1/admin`) must carry it. A sibling mutation on a
#     NON-admin path (e.g. POST /api/offers/{id}/evaluate) is intentionally left
#     to SecurityConfig's authenticated() rule and is NOT required to be admin.
#
# EFFECTIVE authz for a MUTATION = a method-level OR class-level `@PreAuthorize`
# whose SpEL requires an admin authority
#   hasAuthority('ROLE_ADMIN') · hasRole('ADMIN') · hasAnyAuthority(all ROLE_ADMIN)
#   · hasAnyRole(all ADMIN) · denyAll()
# A method-level `@PreAuthorize` OVERRIDES the class-level one (Spring
# method-security precedence). `@PostAuthorize` NEVER counts for a mutation (Fix 1).
#
# Recognition completeness (Fix 2): fully-qualified annotations
# (`@org.springframework.web.bind.annotation.PostMapping`), MULTILINE mapping
# annotations, and NON-PUBLIC (package-private / protected) handler methods are
# all scanned.
#
# Mapping-path extraction (Fix 5 — round-5 codex): the endpoint path is read
# from an explicit `path = "..."` / `value = "..."` attribute wherever it
# appears among the annotation's attributes — NOT just "the first quoted
# string", which misread e.g.
#   @PostMapping(produces = "application/json", path = "/api/admin/x")
# as path `"application/json"` and silently dropped the endpoint from BOTH
# the admin-surface method-path detector and the per-endpoint requirement
# check. The bare-quoted-string form (`@PostMapping("/path")`) is still
# honored as a fallback, but only when positional (no attribute name
# precedes it). Round-6 codex: the extractor now returns the FULL LIST of
# paths (not a single scalar) — a mapping annotation legally accepts an ARRAY
# of paths (`value = {"/public", "/api/admin/missed"}`), and an admin path
# buried as a non-first array element was silently dropped by both consumers
# (Fix 6).
#
# ─────────────────────────────────────────────────────────────────────────────
# Mapping-path extraction — decidable scope vs out-of-scope tail
# (Fix 7 / round-7 convergence)
# ─────────────────────────────────────────────────────────────────────────────
# This lint reads the endpoint route from the controller source. Round-7 codex
# enumerated the ways an HONEST route can be written that the round-6 extractor
# still misread, PLUS an adversarial/undecidable tail. The decision, matching
# this guard's existing "adversarial SpEL is OUT OF LINT SCOPE" philosophy:
# a cheap purely-LOCAL static presence-lint FIXES what honest code writes
# (decidable, common shapes) and DOCUMENTS the undecidable/adversarial tail as
# out-of-scope, deferring it to the AUTHORITATIVE controls (the per-domain
# *ComplianceTest 403 assertions + the runtime SecurityConfig `/api/admin/**`
# matcher). It does NOT try to be a sound static verifier of full Spring
# routing semantics — that is the same whack-a-mole this guard already rejected
# for SecurityConfig parsing.
#
# HANDLED (decidable, real-world common — each has a failing fixture):
#   F1  non-path attribute contents no longer impersonate path syntax: the
#       annotation's TOP-LEVEL attributes are tokenized (commas outside string
#       literals / braces / parens), `path`/`value` is selected by its top-level
#       NAME, and only THEN is its value read — quoted content inside
#       name/produces/consumes/headers/params is never read as a path.
#   F2  URI-template `{id}` no longer breaks array matching: the `{...}` array
#       initializer is scanned with QUOTE/ESCAPE-AWARE brace balancing (only a
#       `}` OUTSIDE a string literal closes the array); admin element first /
#       middle / last is caught identically.
#   F3-simple  same-file `static final String IDENT = "literal"` constants and
#       `+`-concatenations of string literals and/or such same-file constants
#       are constant-folded to the resulting literal, then treated as a path.
#       Round-8 codex: this now resolves constant-to-constant CHAINS to a FIXED
#       POINT — a constant whose OWN initializer is an expression referencing
#       ANOTHER same-file constant (e.g. `static final String API = "/api";
#       static final String ADMIN = API + "/admin";`) is folded too, so
#       `@PostMapping(ADMIN + "/x")` resolves to `/api/admin/x` instead of
#       silently dropping the endpoint (ADMIN previously never entered the
#       constant map at all, because only a single bare string-literal RHS was
#       captured). Same literal/identifier/`+` subset as before — no scope
#       expansion; self-/mutually-cyclic constant references simply never
#       resolve (fail-OPEN, deferred), and the fixed-point loop is bounded by
#       the number of same-file constants, so it always terminates.
#   F4  class-level + method-level path COMPOSITION: the effective route is the
#       class×method CROSS-PRODUCT (a method with no explicit path inherits the
#       class path); every composed route is matched in BOTH admin-surface
#       detection AND the per-endpoint requirement check.
#   F5  Spring's optional leading slash is normalized: a non-empty extracted /
#       composed path lacking a leading `/` gets one prepended before matching
#       (`@PostMapping("api/admin/x")` → `/api/admin/x`).
#   F7  class-level mappings are recognized with the SAME FQN-tolerant
#       recognizer as method mappings (`@(?:[A-Za-z_][\w.]*\.)?RequestMapping`),
#       so a fully-qualified class-level mapping is no longer invisible.
#
# OUT OF SCOPE (undecidable / adversarial tail — deferred, NOT fixed here):
#   F6  fixed path-pattern obfuscation, e.g. `@PostMapping("/api/{scope:admin}/x")`
#       — a variable segment whose regex constraint happens to match "admin".
#       Deciding this requires evaluating Spring PathPattern regex-constraint
#       INTERSECTION against `/api/admin/**` — the same non-decidable
#       full-Spring-semantics modeling rejected above. This static lint does
#       NOT detect this obfuscated annotation shape — but (round-8 codex
#       correction: the prior text here claimed the OPPOSITE and was WRONG) a
#       request satisfying the `{scope:admin}` constraint has a CONCRETE URI of
#       `/api/admin/x`, which the runtime `/api/admin/**` matcher DOES match.
#       So the concrete admin route IS runtime-protected — by SecurityConfig's
#       matcher AND the domain's 403 *ComplianceTest — which STRENGTHENS rather
#       than weakens the scope-out rationale: this is a static-lint BLIND SPOT
#       on the obfuscated annotation source, not a runtime authorization hole.
#       (We deliberately do NOT "fail closed on any variable in the prefix" —
#       that would false-positive on legitimate `/api/{tenant}/...` non-admin
#       routes and break the real repo.)
#   F3-tail  imported/opaque constants, Spring `${...}` / `#{...}` property
#       placeholders (controller-locally undecidable — codex itself conceded
#       these are "not counted"), Java Unicode escapes (`a`), octal escapes
#       (`\057`), and text blocks. General Java constant-expression evaluation
#       with full lexical escape / text-block decoding is disproportionate to a
#       cheap lint; an unresolvable path yields no route (fail-OPEN, deferred).
#   F7-tail  inner-dot WHITESPACE in an FQN annotation
#       (`@org.springframework.web.bind.annotation . PostMapping`) — an exotic
#       lexical form no honest code writes; deferred with the rest of the tail.
# Authoritative control for the entire out-of-scope tail: the per-domain
# *ComplianceTest 403 assertions + the runtime SecurityConfig `/api/admin/**`
# matcher (@EnableMethodSecurity active). This lint is a SUPPLEMENTARY,
# defense-in-depth regression net — it complements those controls, it does not
# replace them.
#
# The `SecurityConfig` path-matchers STAY in the real repo (belt + suspenders);
# this guard simply does not TRUST them for coverage.
#
# Bound by practices/rules/bfla-privileged-endpoint-authz-presence.md
# (verification.guard: admin_preauthorize_guard.sh). Origin: iter2-G1 dogfood
# (docs/dogfood-ledger/engine-w1-iter2.yaml); round-4 codex convergence closed the
# @PostAuthorize/recognition/SpEL-weakener holes and the detection-scope gap;
# round-5 codex closed the attribute-ordered mapping-path parse gap (Fix 5);
# round-6 codex closed the array-valued path shape (Fix 6); round-7 codex closed
# the extraction/composition surface in ONE principled pass — attribute
# tokenization (F1), quote-aware array braces (F2), same-file constant fold
# (F3-simple), class×method composition (F4), leading-slash normalization (F5),
# class-level FQN recognition (F7) — and documented the undecidable tail
# (F6, F3-tail, F7-tail) as out-of-scope above (Fix 7). Round-8 codex closed a
# constant-CHAIN gap in F3-simple (a same-file constant's OWN initializer
# referencing another same-file constant now resolves via fixed-point folding)
# and corrected a factual error in the F6 out-of-scope note (the obfuscated
# `{scope:admin}` route's CONCRETE URI IS matched by the runtime
# `/api/admin/**` matcher — the prior text claimed the opposite).
#
# Exit codes:
#   0 — every REQUIRED mutating admin endpoint carries an effective admin
#       @PreAuthorize (method- or class-level)
#   1 — at least one required mutating admin endpoint has no effective admin
#       @PreAuthorize (signature: ADMIN_ENDPOINT_MISSING_PREAUTHORIZE)
#   2 — usage / environment error (root missing, python3 missing, zero-scan)
#
# Usage:
#   bash practices/evals/admin_preauthorize_guard.sh                # default root = repo root
#   bash practices/evals/admin_preauthorize_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "admin_preauthorize_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

TARGET_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
PKG_DIR="$TARGET_ROOT/backend/src/main/java/com/ax/template/authblueprint"

if [ ! -d "$PKG_DIR" ]; then
    echo "admin_preauthorize_guard: no backend source tree at $PKG_DIR" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "admin_preauthorize_guard: python3 not in PATH (required for java parsing)" >&2
    exit 2
fi

PKG_DIR="$PKG_DIR" python3 - <<'PY'
import os
import re
import sys

pkg_dir = os.environ["PKG_DIR"]

# ── Mapping annotations (Fix 2: optional fully-qualified prefix) ──────────────
MAPPING_START_RE = re.compile(
    r"@(?:[A-Za-z_][\w.]*\.)?(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\b"
)
# Fix 2: also recognise NON-PUBLIC (package-private / protected) handlers — the
# access modifier is optional. A handler-signature line is "[modifiers] Type name(".
METHOD_DECL_RE = re.compile(
    r"^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)*"
    r"[\w.$][\w.$<>\[\], ?&]*\s+\w+\s*\("
)
QUOTED_RE = re.compile(r'"([^"]*)"')
REQUEST_METHOD_RE = re.compile(r"RequestMethod\.([A-Za-z]+)")
# ── Mapping-path extraction: tokenizer + constant-folder (Fix 7 — round-7) ────
# See the header block "Mapping-path extraction — decidable scope vs
# out-of-scope tail". Rather than regex-scanning the WHOLE annotation argument
# string for a `path=`/`value=` literal (which let a NON-path attribute's quoted
# contents impersonate path syntax — Finding 1 — and let a URI-template `{id}`
# brace break array matching — Finding 2), the extractor TOKENIZES the
# annotation's TOP-LEVEL attributes, selects `path`/`value` by its top-level
# NAME, and constant-FOLDS same-file String constants + literal concatenations
# (Finding 3-simple).
# A top-level attribute name is `IDENT =` (a single `=`, not `==`) at the head
# of a top-level segment.
ATTR_NAME_RE = re.compile(r"^\s*([A-Za-z_]\w*)\s*=(?!=)")
# Same-file `static final String IDENT = <expr>;` declarations, for the
# constant-fold (Finding 3-simple, extended round-8 to constant-to-constant
# CHAINS). This regex only anchors the declaration HEAD (`IDENT =`); the RHS
# expression — which may itself reference OTHER same-file constants, e.g.
# `static final String ADMIN = API + "/admin";` — is captured separately by
# `scan_decl_expr` (quote-aware, so a `;` inside a string literal does not
# terminate the declaration early). Optional access modifier precedes `static`.
CONST_DECL_HEAD_RE = re.compile(
    r'\bstatic\s+final\s+String\s+([A-Za-z_]\w*)\s*=\s*'
)
# Class-level mapping recogniser — the SAME FQN-tolerant shape used for method
# mappings (MAPPING_START_RE), so a fully-qualified class-level
# `@org.springframework.web.bind.annotation.RequestMapping(...)` is recognised
# (Finding 7). Inner-dot WHITESPACE (`@org.x . RequestMapping`) is exotic and
# documented OUT OF SCOPE in the header.
CLASS_RM_RE = re.compile(r"@(?:[A-Za-z_][\w.]*\.)?RequestMapping\s*\(")

MUTATING_ANNOT = {"PostMapping", "PutMapping", "PatchMapping", "DeleteMapping"}
MUTATING_VERBS = {"POST", "PUT", "PATCH", "DELETE"}

# A method endpoint path counts as an admin surface if it is under /api/admin or
# /api/v1/admin (Fix 4 — widened detection).
ADMIN_PATH_RE = re.compile(r"^/api/(?:v1/)?admin(?:/|$)")

# ── Effective-admin SpEL model ───────────────────────────────────────────────
BROAD_TOKENS = (
    "permitall", "anonymous", "isanonymous",
    "authenticated", "isauthenticated", "fullyauthenticated",
    "rememberme", "isrememberme", "hasip",
)
ADMIN_PRED_RE = re.compile(
    r"\b(hasAuthority|hasAnyAuthority|hasRole|hasAnyRole)\s*\(([^)]*)\)"
)
# Fix 3: a NEGATED admin predicate — `!hasAuthority(...)` or `not hasRole(...)` —
# inverts the gate (admin required to be ABSENT). Cheaply detectable → reject.
NEG_ADMIN_PRED_RE = re.compile(
    r"(?:!|(?<![\w'])not\s+)\s*has(?:authority|anyauthority|role|anyrole)\b"
)
SINGLE_QUOTED_RE = re.compile(r"'([^']*)'")
DENYALL_RE = re.compile(r"^denyall(\(\))?$")

# @PreAuthorize ONLY (Fix 1: @PostAuthorize never gates a mutation). Optional FQN
# prefix for symmetry with the mapping recogniser.
PREAUTH_SPEL_RE = re.compile(
    r"@(?:[\w.]+\.)?PreAuthorize\s*\(\s*\"((?:[^\"\\]|\\.)*)\""
)
PREAUTH_ANY_RE = re.compile(r"@(?:[\w.]+\.)?PreAuthorize\b")


def strip_comments_preserve_lines(s):
    """Remove // and /* */ comments while (a) NOT touching string/char literals
    (so a path literal "/api/admin/**" or "http://x" is never mistaken for a
    comment) and (b) preserving the line COUNT (block-comment newlines are kept)
    so reported line numbers stay accurate."""
    out = []
    i, n = 0, len(s)
    in_str = None  # None | '"' | "'"
    while i < n:
        c = s[i]
        if in_str is not None:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(s[i + 1]); i += 2; continue
            if c == in_str:
                in_str = None
            i += 1; continue
        if c == '"' or c == "'":
            in_str = c; out.append(c); i += 1; continue
        if c == "/" and i + 1 < n and s[i + 1] == "/":
            while i < n and s[i] != "\n":
                i += 1
            continue  # leave the newline for the next iteration
        if c == "/" and i + 1 < n and s[i + 1] == "*":
            i += 2
            while i + 1 < n and not (s[i] == "*" and s[i + 1] == "/"):
                if s[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2  # skip closing */
            continue
        out.append(c); i += 1
    return "".join(out)


def balanced_args(text, open_idx):
    """Given text[open_idx] == '(', return the substring inside the balanced
    parens, honoring quotes so a ')' inside a literal does not close early."""
    depth = 0
    i, n = open_idx, len(text)
    in_str = None
    while i < n:
        c = text[i]
        if in_str is not None:
            if c == "\\" and i + 1 < n:
                i += 2; continue
            if c == in_str:
                in_str = None
            i += 1; continue
        if c == '"' or c == "'":
            in_str = c; i += 1; continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[open_idx + 1:i]
        i += 1
    return text[open_idx + 1:]


def annotation_args(text, name_end):
    """Return the parenthesized argument string of an annotation whose name ends
    at offset `name_end`, skipping whitespace/newlines up to the '('. Handles
    MULTILINE annotations (Fix 2). Empty string when the annotation has no
    parens (e.g. a bare @PostMapping)."""
    i, n = name_end, len(text)
    while i < n and text[i] in " \t\r\n":
        i += 1
    if i < n and text[i] == "(":
        return balanced_args(text, i)
    return ""


def split_top_level(s, seps):
    """Split `s` on any char in `seps` that is at the TOP level — outside string
    literals ("..." / '...', escape-aware) and outside (), {}, [] nesting. This
    is the quote/brace-aware tokenizer underlying Findings 1 & 2."""
    parts, cur = [], []
    depth = 0
    in_str = None
    i, n = 0, len(s)
    while i < n:
        c = s[i]
        if in_str is not None:
            cur.append(c)
            if c == "\\" and i + 1 < n:
                cur.append(s[i + 1]); i += 2; continue
            if c == in_str:
                in_str = None
            i += 1; continue
        if c == '"' or c == "'":
            in_str = c; cur.append(c); i += 1; continue
        if c in "({[":
            depth += 1; cur.append(c); i += 1; continue
        if c in ")}]":
            depth -= 1; cur.append(c); i += 1; continue
        if depth == 0 and c in seps:
            parts.append("".join(cur)); cur = []; i += 1; continue
        cur.append(c); i += 1
    parts.append("".join(cur))
    return parts


def brace_inner(s):
    """s.lstrip() begins with '{'; return the content between the OUTER braces
    using QUOTE/ESCAPE-AWARE balancing — only a '}' OUTSIDE a string literal
    closes the array, so a URI-template `{id}` inside an element does not close
    it early (Finding 2). None when unbalanced / not an array."""
    s = s.lstrip()
    if not s or s[0] != "{":
        return None
    depth = 0
    in_str = None
    i, n = 0, len(s)
    while i < n:
        c = s[i]
        if in_str is not None:
            if c == "\\" and i + 1 < n:
                i += 2; continue
            if c == in_str:
                in_str = None
            i += 1; continue
        if c == '"' or c == "'":
            in_str = c; i += 1; continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return s[1:i]
        i += 1
    return None


def scan_decl_expr(text, start):
    """From `start` (immediately after 'IDENT =' in a `static final String`
    declaration matched by CONST_DECL_HEAD_RE), scan forward QUOTE-AWARE to
    the terminating TOP-LEVEL ';' and return the raw RHS expression text
    (excluding the ';'), so a ';' inside a string literal does not terminate
    the declaration early. None if no top-level ';' is found before EOF
    (malformed / unterminated — out of scope, mirrors the fail-open posture
    used throughout this extractor)."""
    i, n = start, len(text)
    in_str = None
    while i < n:
        c = text[i]
        if in_str is not None:
            if c == "\\" and i + 1 < n:
                i += 2; continue
            if c == in_str:
                in_str = None
            i += 1; continue
        if c == '"' or c == "'":
            in_str = c; i += 1; continue
        if c == ";":
            return text[start:i]
        i += 1
    return None


def build_const_map(stripped_text):
    """Map same-file `static final String IDENT = <expr>;` declarations to
    their fully constant-folded literal value (Finding 3-simple), resolving
    CONSTANT-TO-CONSTANT CHAINS to a FIXED POINT (round-8 codex): a constant
    whose initializer is itself an EXPRESSION referencing ANOTHER same-file
    constant — e.g. `static final String API = "/api"; static final String
    ADMIN = API + "/admin";` — is now folded too. Pre-fix, this map only ever
    captured a constant with a single bare string-literal RHS; `ADMIN` never
    entered the map at all, so `@PostMapping(ADMIN + "/x")` silently extracted
    no path and the endpoint went uncounted. Comment-stripped text in.

    Algorithm: first collect every declaration's RAW (unresolved) RHS
    expression text via `scan_decl_expr` (quote-aware — a ';' inside a string
    literal does not end the declaration). Then repeatedly fold each
    not-yet-resolved raw expression against the constants resolved SO FAR,
    reusing `fold_scalar` UNCHANGED — the same literal/identifier/'+' subset,
    no scope expansion (an expression referencing an unresolved or
    out-of-scope term simply stays unresolved, fail-OPEN, deferred to the
    domain 403 tests + runtime — see header). Each pass that resolves at least
    one NEW constant may unlock others that reference it; a pass that resolves
    NOTHING new means a fixed point has been reached and the loop stops. This
    is guaranteed to TERMINATE: with N raw declarations, at most N passes can
    make progress (each such pass grows the resolved set by >= 1, bounded by
    N), so a self- or mutually-CYCLIC reference (`static final String A = A +
    "/x";`, or A referencing B which references A) can never resolve — it
    simply stays out of the map forever — without ever looping indefinitely."""
    raw = {}
    for m in CONST_DECL_HEAD_RE.finditer(stripped_text):
        expr = scan_decl_expr(stripped_text, m.end())
        if expr is not None:
            raw[m.group(1)] = expr
    resolved = {}
    changed = True
    while changed:
        changed = False
        for ident, expr in raw.items():
            if ident in resolved:
                continue
            folded = fold_scalar(expr, resolved)
            if folded is not None:
                resolved[ident] = folded
                changed = True
    return resolved


def fold_scalar(expr, const_map):
    """Constant-fold a SCALAR path expression to its literal string, or None if
    any term is not controller-locally decidable.

    In scope (decidable): a "..." string literal; a same-file `static final
    String` IDENT; and any `+`-concatenation of those (Finding 3-simple). Out of
    scope (→ None, fail-OPEN, deferred to the domain 403 tests + runtime — see
    header): an opaque/imported IDENT, a `${...}`/`#{...}` placeholder, a
    parenthesised/ternary expression, Java Unicode/octal escapes, and text
    blocks (Finding 3-tail). Literal content is taken RAW — escapes are NOT
    decoded, so the undecoded adversarial tail stays out of scope rather than
    being silently 'fixed'."""
    if expr is None:
        return None
    out = []
    for t in split_top_level(expr, "+"):
        t = t.strip()
        if not t:
            return None
        if len(t) >= 2 and t[0] == '"' and t[-1] == '"':
            out.append(t[1:-1])
        elif re.fullmatch(r"[A-Za-z_]\w*", t) and t in const_map:
            out.append(const_map[t])
        else:
            return None  # non-literal / opaque / placeholder → undecidable
    return "".join(out)


def parse_path_expr(expr, const_map):
    """Turn a path/value expression (scalar OR `{...}` array) into the list of
    literal path strings it denotes; array elements are folded individually."""
    if expr is None:
        return []
    expr = expr.strip()
    if not expr:
        return []
    if expr[0] == "{":
        inner = brace_inner(expr)
        if inner is None:
            return []
        out = []
        for elem in split_top_level(inner, ","):
            folded = fold_scalar(elem, const_map)
            if folded is not None:
                out.append(folded)
        return out
    folded = fold_scalar(expr, const_map)
    return [folded] if folded is not None else []


def extract_mapping_paths(args, const_map):
    """Extract ALL endpoint paths from a mapping annotation's argument string
    (round-5 → round-7 codex convergence).

    Round-7: the argument string is TOKENIZED into top-level attributes first
    (splitting on commas OUTSIDE string literals and braces), the `path`/`value`
    attribute is selected by its TOP-LEVEL NAME, and only THEN is its value
    read — so quoted content inside a NON-path attribute (name/produces/
    consumes/headers/params) can no longer impersonate path syntax (Finding 1),
    and a URI-template `{id}` brace inside an array element no longer breaks the
    array scan (Finding 2). Preference: an explicit `path=`/`value=` attribute
    (scalar or `{...}` array), else the POSITIONAL leading argument
    (`@PostMapping("/x")` / `@PostMapping({"/a","/b"})`). Bare-identifier and
    literal-concatenation values are constant-folded against same-file String
    constants (Finding 3-simple); undecidable expressions yield no path
    (fail-open, deferred — see header)."""
    chosen = None
    positional = None
    for part in split_top_level(args, ","):
        m = ATTR_NAME_RE.match(part)
        if m:
            if m.group(1) in ("path", "value") and chosen is None:
                chosen = part[m.end():]
        elif positional is None and part.strip():
            positional = part
    expr = chosen if chosen is not None else positional
    return parse_path_expr(expr, const_map)


def normalize_path(p):
    """Spring's PathPatternParser prepends '/' to a non-empty path lacking one
    (Finding 5). Applied after extraction + class×method composition."""
    p = p.strip()
    if p and not p.startswith("/"):
        p = "/" + p
    return p


def join_class_method(cp, mp):
    """Join a class-level path with a method-level path the way Spring
    concatenates them (Finding 4): the method segment is appended with exactly
    one '/' at the seam."""
    cp = cp.strip()
    mp = mp.strip()
    if not cp:
        return mp
    if not mp:
        return cp
    return cp.rstrip("/") + (mp if mp.startswith("/") else "/" + mp)


def compose_paths(class_paths, method_paths):
    """Effective routes for one endpoint = the class×method CROSS-PRODUCT
    (Finding 4), each normalised for the optional leading slash (Finding 5). A
    method with NO explicit path inherits the class path(s); a class with no
    mapping leaves the method path(s) unchanged (normalised)."""
    if not method_paths:
        return [normalize_path(p) for p in class_paths] if class_paths else []
    if not class_paths:
        return [normalize_path(p) for p in method_paths]
    return [normalize_path(join_class_method(cp, mp))
            for cp in class_paths for mp in method_paths]


def spel_requires_admin(spel):
    """True iff the @PreAuthorize SpEL is an EFFECTIVE admin gate.

    This is a cheap lint, not a SpEL evaluator: it rejects the obviously-broken
    shapes (empty / non-literal / permitAll / anonymous / *authenticated /
    non-admin authority / mixed hasAny* / @PostAuthorize-for-mutation handled by
    the caller / NEGATED admin predicate / trivial always-true disjunct) and
    credits only a positive, required ROLE_ADMIN term (or denyAll)."""
    if spel is None:
        return False  # non-literal argument → cannot prove admin → fail-closed
    raw = spel.strip()
    if raw == "":
        return False
    # Blank authority string literals so hasAuthority('ROLE_AUTHENTICATED') is not
    # misread as authenticated(), and so a literal containing "true"/"!" cannot
    # trip the weakener detectors below.
    scan = SINGLE_QUOTED_RE.sub("''", raw)
    scan = re.sub(r'"[^"]*"', '""', scan)
    low = re.sub(r"\s+", "", scan).lower()             # no-space form
    low_sp = re.sub(r"\s+", " ", scan).strip().lower()  # single-spaced form

    if DENYALL_RE.match(low):
        return True  # denyAll() — nobody reaches it, certainly no BFLA

    # (Fix 3a) Negated admin predicate → the admin authority is required ABSENT.
    if NEG_ADMIN_PRED_RE.search(low_sp):
        return False
    # (Fix 3b) Trivial always-true disjunct: "... or true" / "... || true".
    if re.search(r"\btrue\b", low_sp):
        return False

    # Broad/weak grants — also covers "... or permitAll()" / "... or isAnonymous()".
    for tok in BROAD_TOKENS:
        if tok in low:
            return False
    preds = ADMIN_PRED_RE.findall(raw)
    if not preds:
        return False  # no authority predicate at all
    for name, args in preds:
        toks = SINGLE_QUOTED_RE.findall(args)
        if not toks:
            return False  # e.g. hasAuthority(SOME_CONST) — non-literal → fail-closed
        is_role = name.lower() in ("hasrole", "hasanyrole")
        for t in toks:
            norm = t if (not is_role or t.startswith("ROLE_")) else "ROLE_" + t
            if norm != "ROLE_ADMIN":
                return False
    return True


def class_level_facts(stripped_text, const_map):
    """(class_name, class_paths, class_authz_effective, class_decl_line0, class_off).
    class_paths = literal paths of the class-level @RequestMapping (path=/value=/
    array aliases + FQN handled). class_authz_effective = the class-level
    @PreAuthorize is an effective admin gate. Operates on the FIRST top-level
    class."""
    class_off = None
    class_line0 = None
    class_name = None
    for m in re.finditer(r"(?m)^\s*(?:public\s+|final\s+|abstract\s+)*class\s+(\w+)", stripped_text):
        class_off = m.start()
        class_name = m.group(1)
        class_line0 = stripped_text.count("\n", 0, m.start())
        break
    if class_off is None:
        return None, [], False, None, None

    before = stripped_text[:class_off]

    # class-level @RequestMapping — LAST occurrence before the class decl.
    # FQN-tolerant (Finding 7) + tokenized/constant-folded path extraction (the
    # same extractor used for method mappings), so a fully-qualified class
    # mapping and an attribute-ordered/array/constant class path are all read.
    class_paths = []
    rm_iters = list(CLASS_RM_RE.finditer(before))
    if rm_iters:
        last = rm_iters[-1]
        args = balanced_args(before, last.end() - 1)
        class_paths = extract_mapping_paths(args, const_map)

    # class-level @PreAuthorize — LAST occurrence before the class decl.
    class_authz_effective = False
    au_iters = list(re.finditer(r"@(?:[\w.]+\.)?PreAuthorize\s*\(", before))
    if au_iters:
        last = au_iters[-1]
        joined = before[last.start():]
        mm = PREAUTH_SPEL_RE.search(joined)
        spel = mm.group(1) if mm else None
        class_authz_effective = spel_requires_admin(spel)

    return class_name, class_paths, class_authz_effective, class_line0, class_off


admin_controllers = 0
mutating_endpoints = 0
violations = []
detected = []  # (class_name, detection_reason) for the human-readable summary

java_files = []
for root, _dirs, files in os.walk(pkg_dir):
    for fn in sorted(files):
        if fn.endswith(".java"):
            java_files.append(os.path.join(root, fn))
java_files.sort()

for path in java_files:
    with open(path, encoding="utf-8") as fh:
        raw_text = fh.read()
    stripped = strip_comments_preserve_lines(raw_text)
    const_map = build_const_map(stripped)
    class_name, class_paths, class_authz_eff, class_line0, class_off = class_level_facts(stripped, const_map)
    if class_name is None:
        continue

    lines = stripped.split("\n")
    n = len(lines)

    # ── First pass: collect every mapped endpoint below the class decl ─────────
    endpoints = []
    for m in MAPPING_START_RE.finditer(stripped):
        off = m.start()
        if class_off is not None and off <= class_off:
            continue  # class-level @RequestMapping, not a method mapping
        annot = m.group(1)
        args = annotation_args(stripped, m.end())
        # Verb / mutating resolution.
        if annot == "RequestMapping":
            methods = [x.upper() for x in REQUEST_METHOD_RE.findall(args)]
            if methods:
                is_mutating = any(v in MUTATING_VERBS for v in methods)
                verbs = methods
            else:
                is_mutating = True  # method-less @RequestMapping answers ALL verbs
                verbs = ["ALL"]
        elif annot in MUTATING_ANNOT:
            is_mutating = True
            verbs = [annot.replace("Mapping", "").upper()]
        else:  # GetMapping
            is_mutating = False
            verbs = ["GET"]

        ep_paths = extract_mapping_paths(args, const_map)
        lineno = stripped.count("\n", 0, off) + 1
        li = lineno - 1  # 0-based line index of the mapping annotation start

        # Contiguous annotation block: @-lines above the mapping + lines down to
        # the handler signature (so a method-level @PreAuthorize above OR below
        # the mapping annotation, and a MULTILINE mapping, are both captured).
        start = li
        k = li - 1
        while k > class_line0 and lines[k].lstrip().startswith("@"):
            start = k
            k -= 1
        j = li
        sig = None
        while j < n:
            lj = lines[j]
            if not lj.lstrip().startswith("@") and METHOD_DECL_RE.match(lj):
                sig = j
                break
            j += 1
        end = sig if sig is not None else li
        block = "\n".join(lines[start:end + 1])

        method_has_own = bool(PREAUTH_ANY_RE.search(block))
        method_authz_eff = False
        if method_has_own:
            for am in re.finditer(r"@(?:[\w.]+\.)?PreAuthorize\s*\(", block):
                sm = PREAUTH_SPEL_RE.search(block[am.start():])
                spel = sm.group(1) if sm else None
                if spel_requires_admin(spel):
                    method_authz_eff = True
                    break

        endpoints.append({
            "annot": annot, "verbs": verbs, "is_mutating": is_mutating,
            "paths": ep_paths,
            # Effective routes = class×method composition (Finding 4) + optional
            # leading-slash normalisation (Finding 5). BOTH consumers below match
            # ADMIN_PATH_RE against these composed routes, never the raw pieces.
            "composed": compose_paths(class_paths, ep_paths), "lineno": lineno,
            "method_has_own": method_has_own, "method_authz_eff": method_authz_eff,
        })

    # ── Admin-surface detection (name OR class-path OR composed method-path) ───
    admin_by_class = class_name.endswith("AdminController") or any(
        "/admin" in p for p in class_paths
    )
    admin_by_method = any(
        ep["is_mutating"] and any(ADMIN_PATH_RE.match(p) for p in ep["composed"])
        for ep in endpoints
    )
    if not (admin_by_class or admin_by_method):
        continue

    if admin_by_class:
        reason = ("name *AdminController" if class_name.endswith("AdminController")
                  else "class-level @RequestMapping '/admin'")
    else:
        reason = "method-level /api/admin mapping (WIDENED detection)"
    admin_controllers += 1
    detected.append((class_name, reason))

    # ── Per-endpoint requirement ──────────────────────────────────────────────
    for ep in endpoints:
        if not ep["is_mutating"]:
            continue
        # An endpoint must carry admin authz if the whole controller is an admin
        # surface (by name/class-path) OR its own EFFECTIVE (composed) route is
        # under /admin.
        requires = admin_by_class or any(ADMIN_PATH_RE.match(p) for p in ep["composed"])
        if not requires:
            continue
        mutating_endpoints += 1
        effective = ep["method_authz_eff"] if ep["method_has_own"] else class_authz_eff
        if not effective:
            vlabel = "|".join(ep["verbs"])
            plabel = "|".join(ep["composed"]) or "/"
            violations.append(
                f"{path}:{ep['lineno']}: mutating admin endpoint "
                f"[{vlabel} {plabel}] on {class_name} has no EFFECTIVE "
                f"admin @PreAuthorize (method- or class-level SpEL requiring "
                f"ROLE_ADMIN; @PostAuthorize does NOT gate a mutation)"
            )

if admin_controllers == 0:
    print(
        "admin_preauthorize_guard: ZERO_SCAN — no admin-surface controller "
        "(*AdminController name, class-level @RequestMapping '/admin' path, OR a "
        "method-level /api/admin mutating mapping) found under " + pkg_dir,
        file=sys.stderr,
    )
    sys.exit(2)

print(
    f"admin_preauthorize_guard: scanned {admin_controllers} admin-surface "
    f"controller(s), {mutating_endpoints} required mutating endpoint(s); "
    f"purely-local method/class @PreAuthorize LINT (SecurityConfig NOT parsed, "
    f"adversarial SpEL OUT OF SCOPE)"
)
for cn, reason in detected:
    print(f"  detected admin-surface: {cn} ({reason})")

if violations:
    print("VIOLATION: mutating admin endpoint reachable with no effective admin authorization (BFLA shape):", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every REQUIRED mutating mapped endpoint (@PostMapping/@PutMapping/@PatchMapping/"
        "@DeleteMapping, or @RequestMapping with a mutating/absent method) on an "
        "admin surface MUST carry an EFFECTIVE admin authorization annotation — a "
        "method-level or class-level @PreAuthorize whose SpEL requires ROLE_ADMIN "
        "(hasAuthority('ROLE_ADMIN') / hasRole('ADMIN') / hasAnyAuthority(all ROLE_ADMIN) "
        "/ denyAll()). permitAll(), anonymous(), authenticated() alone, a non-admin "
        "authority, a negated admin predicate, a trivial always-true disjunction, a "
        "@PostAuthorize on a mutation, or NO annotation do NOT count. A method-level "
        "annotation overrides the class-level one. This guard does NOT read "
        "SecurityConfig — add the @PreAuthorize (defense-in-depth); the path matcher "
        "stays as a complementary layer.",
        file=sys.stderr,
    )
    print(
        f"admin_preauthorize_guard: {len(violations)} violation(s) — "
        f"ADMIN_ENDPOINT_MISSING_PREAUTHORIZE — BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "admin_preauthorize_guard: every required mutating admin endpoint carries an "
    "effective admin @PreAuthorize (method- or class-level)"
)
sys.exit(0)
PY
