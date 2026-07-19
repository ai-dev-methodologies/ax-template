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
# precedes it).
#
# The `SecurityConfig` path-matchers STAY in the real repo (belt + suspenders);
# this guard simply does not TRUST them for coverage.
#
# Bound by practices/rules/bfla-privileged-endpoint-authz-presence.md
# (verification.guard: admin_preauthorize_guard.sh). Origin: iter2-G1 dogfood
# (docs/dogfood-ledger/engine-w1-iter2.yaml); round-4 codex convergence closed the
# @PostAuthorize/recognition/SpEL-weakener holes and the detection-scope gap;
# round-5 codex closed the attribute-ordered mapping-path parse gap (Fix 5).
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
# Fix 5 (round-5 codex): an explicit `path = "..."` / `value = "..."` attribute,
# in EITHER order relative to the annotation's other attributes (e.g.
# `@PostMapping(produces = "application/json", path = "/api/admin/x")`).
PATH_ATTR_RE = re.compile(r'\b(?:path|value)\s*=\s*"([^"]*)"')

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


def extract_mapping_path(args):
    """Extract the endpoint path from a mapping annotation's argument string.

    Round-5 codex finding: `QUOTED_RE.search(args)` (the prior implementation)
    took the FIRST quoted string in the annotation regardless of which named
    attribute it belonged to. For
      @PostMapping(produces = "application/json", path = "/api/admin/x")
    that mis-read "application/json" as the path, so the /api/admin path
    detector never matched — the endpoint was silently treated as NOT an
    admin surface / NOT requiring authz, even with zero @PreAuthorize.

    Fix: prefer an explicit `path = "..."` or `value = "..."` attribute
    (checked first, regardless of position among other attributes); only
    fall back to the first bare quoted string for the shorthand form
    (`@PostMapping("/path")`), where the string is positional and not
    preceded by any other attribute name."""
    m = PATH_ATTR_RE.search(args)
    if m:
        return m.group(1)
    stripped_args = args.lstrip()
    if stripped_args.startswith('"'):
        m2 = QUOTED_RE.match(stripped_args)
        if m2:
            return m2.group(1)
    return ""


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


def class_level_facts(stripped_text):
    """(class_name, class_paths, class_authz_effective, class_decl_line0, class_off).
    class_paths = literal paths of the class-level @RequestMapping (path=/value=/
    array aliases handled). class_authz_effective = the class-level @PreAuthorize
    is an effective admin gate. Operates on the FIRST top-level class."""
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
    class_paths = []
    rm_iters = list(re.finditer(r"@RequestMapping\s*\(", before))
    if rm_iters:
        last = rm_iters[-1]
        args = balanced_args(before, last.end() - 1)
        class_paths = QUOTED_RE.findall(args)

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
    class_name, class_paths, class_authz_eff, class_line0, class_off = class_level_facts(stripped)
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

        ep_path = extract_mapping_path(args)
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
            "path": ep_path, "lineno": lineno,
            "method_has_own": method_has_own, "method_authz_eff": method_authz_eff,
        })

    # ── Admin-surface detection (name OR class-path OR method-path) ────────────
    admin_by_class = class_name.endswith("AdminController") or any(
        "/admin" in p for p in class_paths
    )
    admin_by_method = any(
        ep["is_mutating"] and ADMIN_PATH_RE.match(ep["path"] or "") for ep in endpoints
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
        # surface (by name/class-path) OR the endpoint's own path is under /admin.
        requires = admin_by_class or bool(ADMIN_PATH_RE.match(ep["path"] or ""))
        if not requires:
            continue
        mutating_endpoints += 1
        effective = ep["method_authz_eff"] if ep["method_has_own"] else class_authz_eff
        if not effective:
            vlabel = "|".join(ep["verbs"])
            violations.append(
                f"{path}:{ep['lineno']}: mutating admin endpoint "
                f"[{vlabel} {ep['path'] or '/'}] on {class_name} has no EFFECTIVE "
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
