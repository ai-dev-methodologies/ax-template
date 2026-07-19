#!/usr/bin/env bash
# practices/evals/admin_preauthorize_guard.sh
#
# ─────────────────────────────────────────────────────────────────────────────
# WHAT IT ENFORCES (purely LOCAL check — no cross-file config parsing)
# ─────────────────────────────────────────────────────────────────────────────
# For every ADMIN-SURFACE controller under
#   <root>/backend/src/main/java/com/ax/template/authblueprint/**
# — identified as a class whose NAME ends with `AdminController` AND/OR whose
# class-level `@RequestMapping` path contains `/admin` (path=/value= aliases and
# the `{...}` array form handled) — EVERY *mutating* mapped endpoint
# (`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`, or
# `@RequestMapping(method = POST|PUT|PATCH|DELETE...)`, or a method-less
# `@RequestMapping` which answers every verb) MUST carry an EFFECTIVE admin
# authorization annotation: a method-level OR class-level
# `@PreAuthorize`/`@PostAuthorize` whose SpEL REQUIRES an admin authority —
#   hasAuthority('ROLE_ADMIN') · hasRole('ADMIN') · hasAnyAuthority(all ROLE_ADMIN)
#   · hasAnyRole(all ADMIN) · denyAll()
# A method-level annotation OVERRIDES the class-level one (Spring method-security
# precedence). REJECTED as ineffective (→ BLOCK): permitAll(), anonymous(),
# isAnonymous(), authenticated()/isAuthenticated()/fullyAuthenticated()/rememberMe()
# alone, a non-admin authority (e.g. hasAuthority('ROLE_USER')), a hasAny*(...) that
# mixes in any non-admin alternative, an empty expression, a non-literal SpEL
# argument (a named constant — we cannot prove it is admin), or NO annotation.
#
# ─────────────────────────────────────────────────────────────────────────────
# WHY THIS SHAPE — the SecurityConfig-parsing bypass class is now MOOT
# ─────────────────────────────────────────────────────────────────────────────
# A prior revision of this guard tried to CREDIT admin coverage by statically
# parsing the Spring `SecurityConfig.java` `authorizeHttpRequests` matcher chain
# (path Ant-matching + declared-order + verb scoping). A cross-family reviewer
# then found FOUR distinct static-analysis bypasses across three rounds
# (verb-scoped matcher; multiple / unscoped filter chains; a
# `@RequestMapping(path = ...)` alias resolving to the wrong matcher; …), because
# exhaustively modelling Spring Security's authorization from source is not
# statically decidable — every hardening left another shape to exploit.
#
# This guard STOPS that whack-a-mole by REMOVING SecurityConfig crediting
# ENTIRELY. It reads only the controller file. There is no config chain to model,
# so there is nothing to bypass: the three remaining codex bypasses
# (verb-scoped matcher · multiple/unscoped chain · @RequestMapping(path=) alias →
# wrong matcher) are ALL MOOT because the guard no longer credits SecurityConfig
# at all. The required control is now method-/class-level `@PreAuthorize`
# (defense-in-depth) — a purely local, decidable property.
#
# The SecurityConfig path-matchers in the real repo STAY (belt + suspenders); the
# guard simply does not TRUST them for coverage. `@EnableMethodSecurity` is active
# in SecurityConfig, so the class-level `@PreAuthorize` we require is enforced at
# runtime as a second, independent gate in front of every matcher.
#
# Relationship to the authoritative control: this guard is a cheap, precise LOCAL
# regression net for the "admin mutating endpoint lost its authorization
# annotation" shape. The authoritative BFLA control remains `SecurityConfig.java`
# PLUS the per-domain integration tests that assert HTTP 403 for a non-admin
# caller (AuthzParityViolationProofTest / AccessGrantViolationProofTest and the
# `./gradlew test{Domain}` AUTHZ items) — those stay the primary non-vacuity proof
# for S2.AUTHZ.BE.
#
# Bound by practices/rules/bfla-privileged-endpoint-authz-presence.md
# (verification.guard: admin_preauthorize_guard.sh). Origin: iter2-G1 dogfood
# (docs/dogfood-ledger/engine-w1-iter2.yaml), whose stated expiry trigger was
# "decouple from *AdminController naming convention" — satisfied here by the
# class-level `@RequestMapping` path-based admin-surface detection.
#
# Exit codes:
#   0 — every mutating mapped endpoint in every admin-surface controller carries
#       an effective admin @PreAuthorize/@PostAuthorize
#   1 — at least one mutating admin endpoint has no effective admin authorization
#       annotation (signature: ADMIN_ENDPOINT_MISSING_PREAUTHORIZE)
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

# ── Mapping annotations ──────────────────────────────────────────────────────
MAPPING_RE = re.compile(
    r"^\s*@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\b"
)
CLASS_DECL_RE = re.compile(r"^\s*(?:public\s+|final\s+|abstract\s+)*class\s+\w+")
METHOD_DECL_RE = re.compile(r"^\s*(?:public|protected|private)\b.*\(")
QUOTED_RE = re.compile(r'"([^"]*)"')
REQUEST_METHOD_RE = re.compile(r"RequestMethod\.([A-Za-z]+)")

MUTATING_ANNOT = {"PostMapping", "PutMapping", "PatchMapping", "DeleteMapping"}
MUTATING_VERBS = {"POST", "PUT", "PATCH", "DELETE"}

# ── Effective-admin SpEL model ───────────────────────────────────────────────
# A @Pre/@PostAuthorize is EFFECTIVE (admin-requiring) iff its SpEL:
#   - contains no broad/weak grant (permitAll / anonymous / *authenticated /
#     rememberMe / hasIpAddress), AND
#   - contains >=1 admin predicate (hasAuthority / hasAnyAuthority / hasRole /
#     hasAnyRole), AND every authority argument of every such predicate
#     normalizes to ROLE_ADMIN,
#   OR the whole expression is denyAll() (denies everyone → certainly no BFLA).
# A non-literal SpEL argument (e.g. a named constant) is treated as NOT proven
# admin (fail-closed): a security guard must never credit on uncertainty.
BROAD_TOKENS = (
    "permitall", "anonymous", "isanonymous",
    "authenticated", "isauthenticated", "fullyauthenticated",
    "rememberme", "isrememberme", "hasip",
)
ADMIN_PRED_RE = re.compile(
    r"\b(hasAuthority|hasAnyAuthority|hasRole|hasAnyRole)\s*\(([^)]*)\)"
)
SINGLE_QUOTED_RE = re.compile(r"'([^']*)'")
DENYALL_RE = re.compile(r"^denyall(\(\))?$")

# Extract the double-quoted SpEL literal of a @Pre/@PostAuthorize annotation.
PREAUTH_SPEL_RE = re.compile(
    r"@(?:Pre|Post)Authorize\s*\(\s*\"((?:[^\"\\]|\\.)*)\""
)
PREAUTH_ANY_RE = re.compile(r"@(?:Pre|Post)Authorize\b")


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


def spel_requires_admin(spel):
    """True iff the @Pre/@PostAuthorize SpEL is an EFFECTIVE admin gate."""
    if spel is None:
        return False  # non-literal argument → cannot prove admin → fail-closed
    raw = spel.strip()
    if raw == "":
        return False
    # Scan for broad/weak grants with authority literals blanked out (so
    # hasAuthority('ROLE_AUTHENTICATED') is not misread as authenticated()).
    scan = SINGLE_QUOTED_RE.sub("''", raw)
    scan = re.sub(r'"[^"]*"', '""', scan)
    low = re.sub(r"\s+", "", scan).lower()
    if DENYALL_RE.match(low):
        return True  # denyAll() — nobody reaches it, certainly no BFLA
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
    """(class_name, class_paths, class_authz_effective, class_decl_line0).
    class_paths = literal paths of the class-level @RequestMapping (path=/value=/
    array aliases handled). class_authz_effective = the class-level
    @Pre/@PostAuthorize is an effective admin gate. Operates on the FIRST
    top-level class."""
    class_off = None
    class_line0 = None
    class_name = None
    for m in re.finditer(r"(?m)^\s*(?:public\s+|final\s+|abstract\s+)*class\s+(\w+)", stripped_text):
        class_off = m.start()
        class_name = m.group(1)
        class_line0 = stripped_text.count("\n", 0, m.start())
        break
    if class_off is None:
        return None, [], False, None

    before = stripped_text[:class_off]

    # class-level @RequestMapping — LAST occurrence before the class decl.
    class_paths = []
    rm_iters = list(re.finditer(r"@RequestMapping\s*\(", before))
    if rm_iters:
        last = rm_iters[-1]
        args = balanced_args(before, last.end() - 1)
        class_paths = QUOTED_RE.findall(args)

    # class-level @Pre/@PostAuthorize — LAST occurrence before the class decl.
    class_authz_effective = False
    au_iters = list(re.finditer(r"@(?:Pre|Post)Authorize\s*\(", before))
    if au_iters:
        last = au_iters[-1]
        joined = before[last.start():]
        mm = PREAUTH_SPEL_RE.search(joined)
        spel = mm.group(1) if mm else None
        class_authz_effective = spel_requires_admin(spel)

    return class_name, class_paths, class_authz_effective, class_line0


admin_controllers = 0
mutating_endpoints = 0
violations = []

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
    class_name, class_paths, class_authz_eff, class_line0 = class_level_facts(stripped)
    if class_name is None:
        continue

    admin_surface = class_name.endswith("AdminController") or any(
        "/admin" in p for p in class_paths
    )
    if not admin_surface:
        continue

    admin_controllers += 1
    lines = stripped.split("\n")
    n = len(lines)

    idx = class_line0 + 1  # method mappings live below the class declaration
    while idx < n:
        line = lines[idx]
        mm = MAPPING_RE.match(line)
        if not mm:
            idx += 1
            continue
        annot = mm.group(1)
        mapping_lineno = idx + 1

        # Resolve verbs / mutating.
        if annot == "RequestMapping":
            joined_ann = "\n".join(lines[idx:idx + 6])
            methods = [x.upper() for x in REQUEST_METHOD_RE.findall(joined_ann)]
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

        # Path suffix (for the message).
        suffix_m = QUOTED_RE.search(line)
        suffix = suffix_m.group(1) if suffix_m else ""

        # Gather this method's contiguous annotation block: @-lines above the
        # mapping annotation + lines down to the method signature.
        start = idx
        k = idx - 1
        while k > class_line0 and lines[k].lstrip().startswith("@"):
            start = k
            k -= 1
        j = idx
        sig = None
        while j < n:
            lj = lines[j]
            if not lj.lstrip().startswith("@") and METHOD_DECL_RE.match(lj):
                sig = j
                break
            j += 1
        end = sig if sig is not None else idx
        block = "\n".join(lines[start:end + 1])

        method_has_own = bool(PREAUTH_ANY_RE.search(block))
        method_authz_eff = False
        if method_has_own:
            for am in re.finditer(r"@(?:Pre|Post)Authorize\s*\(", block):
                sm = PREAUTH_SPEL_RE.search(block[am.start():])
                spel = sm.group(1) if sm else None
                if spel_requires_admin(spel):
                    method_authz_eff = True
                    break

        effective = method_authz_eff if method_has_own else class_authz_eff

        if is_mutating:
            mutating_endpoints += 1
            if not effective:
                vlabel = "|".join(verbs)
                violations.append(
                    f"{path}:{mapping_lineno}: mutating admin endpoint "
                    f"[{vlabel} {suffix or '/'}] on {class_name} has no EFFECTIVE "
                    f"admin @PreAuthorize/@PostAuthorize (method- or class-level SpEL "
                    f"requiring ROLE_ADMIN)"
                )
        idx = (end if sig is not None else idx) + 1

if admin_controllers == 0:
    print(
        "admin_preauthorize_guard: ZERO_SCAN — no admin-surface controller "
        "(*AdminController name OR class-level @RequestMapping path containing "
        "'/admin') found under " + pkg_dir,
        file=sys.stderr,
    )
    sys.exit(2)

print(
    f"admin_preauthorize_guard: scanned {admin_controllers} admin-surface "
    f"controller(s), {mutating_endpoints} mutating endpoint(s); purely-local "
    f"method/class @PreAuthorize check (SecurityConfig NOT parsed)"
)

if violations:
    print("VIOLATION: mutating admin endpoint reachable with no effective admin authorization (BFLA shape):", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every mutating mapped endpoint (@PostMapping/@PutMapping/@PatchMapping/"
        "@DeleteMapping, or @RequestMapping with a mutating/absent method) on an "
        "admin-surface controller MUST carry an EFFECTIVE admin authorization "
        "annotation — a method-level or class-level @PreAuthorize/@PostAuthorize "
        "whose SpEL requires ROLE_ADMIN (hasAuthority('ROLE_ADMIN') / hasRole('ADMIN') "
        "/ hasAnyAuthority(all ROLE_ADMIN) / denyAll()). permitAll(), anonymous(), "
        "authenticated() alone, a non-admin authority, or NO annotation do NOT count. "
        "A method-level annotation overrides the class-level one. This guard does NOT "
        "read SecurityConfig — add the annotation (defense-in-depth); the path matcher "
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
    "admin_preauthorize_guard: every mutating admin endpoint carries an effective "
    "admin @PreAuthorize/@PostAuthorize (method- or class-level)"
)
sys.exit(0)
PY