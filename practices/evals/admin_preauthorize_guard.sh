#!/usr/bin/env bash
# practices/evals/admin_preauthorize_guard.sh
#
# ─────────────────────────────────────────────────────────────────────────────
# HONEST SCOPE (read this first)
# ─────────────────────────────────────────────────────────────────────────────
# This is a BEST-EFFORT STATIC HEURISTIC backstop for the BFLA-presence
# invariant. It models the COMMON SecurityConfig matcher-chain shape
# (`requestMatchers([HttpMethod.X,] "pat")` + a terminal authorization rule, in
# declared order) plus method-level `@PreAuthorize`/`@PostAuthorize`, and it
# FAILS CLOSED on anything it cannot prove (see FAIL-CLOSED below). It is NOT an
# exhaustive Spring-Security authorization verifier: it does not evaluate custom
# `RequestMatcher` beans, `.access(...)` authorization managers, SpEL beyond the
# `has*`/`permitAll`/`anonymous` literals, `securityMatcher`-scoped multi-chain
# setups, or programmatic/variable path construction. When it meets any of those
# it BLOCKS (demands an explicit method-level `@PreAuthorize`) rather than guess.
#
# The AUTHORITATIVE BFLA control for this repo is SecurityConfig.java itself PLUS
# the per-domain integration tests that assert HTTP 403 for a non-admin caller
# (e.g. AuthzParityViolationProofTest / AccessGrantViolationProofTest and the
# `./gradlew test{Domain}` AUTHZ items). This guard is SUPPLEMENTARY: it catches
# the "someone dropped the annotation / mis-scoped the matcher" regression shape
# early and cheaply; it is not the primary non-vacuity proof for S2.AUTHZ.BE.
# ─────────────────────────────────────────────────────────────────────────────
#
# Promoted (wave-1 exit cleanup) from
# practices/consumer-proof/scenarios/S3.b2b-admin/scenario-guards/admin_preauthorize_guard.sh
# — iter2-G1 dogfood finding (docs/dogfood-ledger/engine-w1-iter2.yaml): no
# catalog guard enforced the PRESENCE of authorization on admin-surface
# controller endpoints; role_literal_guard.sh only validates that authority
# STRINGS in existing @PreAuthorize are known-valid, it is presence-blind.
# This is the accepted runnable mechanism bound by
# practices/rules/bfla-privileged-endpoint-authz-presence.md
# (verification.guard: admin_preauthorize_guard.sh).
#
# WHAT IT ENFORCES
# Every *AdminController.java under
#   <root>/backend/src/main/java/com/ax/template/authblueprint/**
# must be unreachable without an EFFECTIVE authorization check. A mapped
# method (every @GetMapping/@PostMapping/@PutMapping/@DeleteMapping/
# @PatchMapping/@RequestMapping) is COVERED if EITHER:
#   (a) a class-level @PreAuthorize/@PostAuthorize whose SpEL REQUIRES an
#       authority/role (hasAuthority/hasRole/hasAnyAuthority/hasAnyRole/
#       hasPermission/denyAll) covers the method (and the method does not
#       override it with a weaker method-level annotation), OR
#   (b) the method itself carries its own @PreAuthorize/@PostAuthorize whose
#       SpEL REQUIRES an authority/role, OR
#   (c) for EVERY HTTP verb the endpoint answers (from @GetMapping/@PostMapping/
#       @PutMapping/@DeleteMapping/@PatchMapping, or @RequestMapping(method=…);
#       a method-less @RequestMapping answers all verbs), the FIRST matcher in
#       SecurityConfig.java's declared-order authorization chain that matches
#       BOTH that verb AND the endpoint's resolved path requires an ADMIN
#       authority (normalized ROLE_ADMIN).
#
# FOUR HARDENINGS (wave-1 + codex round-2 bypass closure):
#   1. Route (c) parses each matcher's REQUIRED AUTHORITY and only credits
#      coverage when it is admin (ROLE_ADMIN / hasRole('ADMIN')). A matcher
#      that grants /api/admin/** to ROLE_USER no longer counts as protection.
#   2. Route (c) uses boundary-aware Ant matching, NOT raw startswith:
#      "/api/admin/**" covers "/api/admin" and "/api/admin/<anything>" but
#      NOT "/api/administrator..." (a different path segment).
#   3. An @PreAuthorize/@PostAuthorize is EFFECTIVE only if its SpEL requires
#      an authority/role. permitAll()/anonymous()/isAnonymous()/empty/true are
#      NON-authz and do not cover a method — and a method-level annotation
#      OVERRIDES the class-level one (Spring method-security precedence).
#   4. Route (c) models the matcher chain the way Spring evaluates it: in
#      DECLARED ORDER, honoring the optional leading HttpMethod (a
#      verb-specific matcher matches ONLY that verb; a verb-agnostic matcher
#      matches every verb), and crediting an endpoint as admin-covered ONLY IF
#      the FIRST matcher that matches its (verb, path) requires admin. This
#      closes the codex round-2 HIGH: a verb-scoped ROLE_ADMIN GET matcher
#      declared before a verb-agnostic `.authenticated()` fallback does NOT
#      protect a POST/PUT/PATCH/DELETE — Spring's first match for those verbs
#      is the `.authenticated()` rule, which admits any authenticated non-admin.
#
# FAIL-CLOSED (a security guard must never credit on uncertainty)
# Route (c) credits admin coverage ONLY when it can PROVE, for every verb the
# endpoint answers, that the first-matching declared matcher requires admin. It
# credits NOTHING (→ the endpoint then needs an effective method @PreAuthorize,
# else BLOCK) whenever it cannot prove that, including:
#   - SecurityConfig.java absent at the expected path (route (c) inactive; the
#     original (a)/(b)-only behavior for minimal fixture roots);
#   - the authorization chain is not fully modelable — not exactly one
#     authorizeHttpRequests/authorizeRequests block, a chain-level
#     securityMatcher, a selector followed by a rule we do not model
#     (.access(...), .hasIpAddress(...), a custom AuthorizationManager, …);
#   - a matcher whose path argument is not a plain string literal (a variable,
#     a custom RequestMatcher, a builder) — opaque, cannot be ruled in or out;
#   - a matcher whose wildcard shape we cannot evaluate precisely (mid-path '*',
#     '?', '{var}', mid-path '**') AND whose literal prefix does not rule the
#     endpoint out — treated as a possible match of unknown authority.
# In every one of those cases the endpoint is NOT credited and must carry an
# effective method-level @PreAuthorize, or it is BLOCKED.
#
# Route (c) matters because it is the REAL, documented mechanism this repo's
# own admin controllers already use — e.g. FeatureFlagAdminController's Javadoc
# ("...so this controller does not re-declare @PreAuthorize") relies on
# SecurityConfig's `.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")`
# (PAYMENT-AUTHZ-004) and `.requestMatchers("/api/v1/admin/feature-flags/**")
# .hasAuthority("ROLE_ADMIN")`. Both are VERB-AGNOSTIC ROLE_ADMIN matchers that
# are the first match for every verb of those paths → legitimately covered.
#
# A mapped method covered by NONE of the above is the IDOR/BFLA shape: any
# caller who reaches the route reaches the handler, no role check in between.
#
# ZERO_SCAN safety: the package dir must exist AND contain at least one
# *AdminController.java with at least one mapped method, or this is an
# environment/usage problem (exit 2), not a silent pass.
#
# Exit codes:
#   0 — every mapped method in every *AdminController.java is covered
#   1 — at least one mapped method is reachable with no authz check
#       (signature: ADMIN_ENDPOINT_MISSING_PREAUTHORIZE)
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
SECURITY_CONFIG="$TARGET_ROOT/backend/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java"

if [ ! -d "$PKG_DIR" ]; then
    echo "admin_preauthorize_guard: no backend source tree at $PKG_DIR" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "admin_preauthorize_guard: python3 not in PATH (required for java parsing)" >&2
    exit 2
fi

PKG_DIR="$PKG_DIR" SECURITY_CONFIG="$SECURITY_CONFIG" python3 - <<'PY'
import os
import re
import sys

pkg_dir = os.environ["PKG_DIR"]
security_config_path = os.environ["SECURITY_CONFIG"]

MAPPING_RE = re.compile(
    r"^\s*@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\b"
)
AUTHZ_RE = re.compile(r"^\s*@(PreAuthorize|PostAuthorize)\b")
OTHER_ANN_RE = re.compile(r"^\s*@\w")
CLASS_DECL_RE = re.compile(r"^\s*(?:public\s+)?(?:final\s+)?class\s+\w+")
METHOD_DECL_RE = re.compile(r"^\s*(?:public|protected|private).*\(.*")
CLASS_REQUEST_MAPPING_RE = re.compile(r'^\s*@RequestMapping\s*\(\s*"([^"]*)"')
QUOTED_RE = re.compile(r'"([^"]*)"')

# ── Verb model (route (c) hardening 4) ──────────────────────────────────────
VERB_BY_ANNOT = {
    "GetMapping": ["GET"],
    "PostMapping": ["POST"],
    "PutMapping": ["PUT"],
    "DeleteMapping": ["DELETE"],
    "PatchMapping": ["PATCH"],
}
# A @RequestMapping with no explicit method answers every verb; we require admin
# coverage for each of the standard mapped verbs (fail-closed for a verb-scoped
# admin matcher that would leave some of them open).
ALL_VERBS = ["GET", "POST", "PUT", "PATCH", "DELETE"]
REQUEST_METHOD_RE = re.compile(r"RequestMethod\.([A-Za-z]+)")

# SpEL that REQUIRES an authority/role (an effective authorization check).
AUTHZ_PREDICATE_RE = re.compile(
    r"\b(hasAuthority|hasAnyAuthority|hasRole|hasAnyRole|hasPermission|denyAll)\b",
    re.IGNORECASE,
)
# SpEL that is a permit-all / anonymous equivalent (NON-authz — never covers).
PERMIT_ALL_RE = re.compile(
    r"^(permitall\(\)|permitall|true|anonymous\(\)|anonymous|isanonymous\(\)|isanonymous)$"
)
# Extract the double-quoted SpEL argument of a @PreAuthorize/@PostAuthorize.
PREAUTH_SPEL_RE = re.compile(
    r'@(?:PreAuthorize|PostAuthorize)\s*\(\s*"((?:[^"\\]|\\.)*)"'
)


def extract_authz_spel(lines_, k: int):
    """Return the SpEL string literal of the @Pre/@PostAuthorize starting at
    line k (joining a few following lines to tolerate a wrapped annotation),
    or None when the argument is not a plain string literal (e.g. a constant
    reference) — in which case the caller treats it conservatively as present."""
    joined = "".join(lines_[k:k + 4])
    m = PREAUTH_SPEL_RE.search(joined)
    return m.group(1) if m else None


def spel_requires_authority(spel) -> bool:
    """True iff the @Pre/@PostAuthorize SpEL is an EFFECTIVE authz check.
    None (non-literal arg, e.g. a named constant) is treated as present/effective
    to avoid false positives against legitimate constant-based annotations —
    the codex bypass is a *literal* permitAll(), which is caught below."""
    if spel is None:
        return True
    s = spel.strip()
    if s == "":
        return False
    norm = re.sub(r"\s+", "", s).lower()
    if PERMIT_ALL_RE.match(norm):
        return False
    return bool(AUTHZ_PREDICATE_RE.search(s))


# ── Route (c): SecurityConfig authorization-chain model ─────────────────────
# We parse the single authorizeHttpRequests/authorizeRequests block into an
# ORDERED list of authorization entries, each carrying: the optional HttpMethod
# verb, the path pattern(s) (or None when opaque), and the required authority
# ('admin' | 'other'). Coverage for an endpoint is then the FIRST entry (in
# declared order) that matches its (verb, path) — Spring's first-match rule —
# and the endpoint is credited ONLY when that first match requires admin. See
# the FAIL-CLOSED block in the header for every case that credits nothing.

# Authorization terminal rules we can classify. A selector followed by anything
# NOT in this set makes the chain "not fully modelable" (fail-closed).
RULE_NAMES = (
    "permitAll", "denyAll", "authenticated", "anonymous",
    "rememberMe", "fullyAuthenticated",
    "hasAuthority", "hasAnyAuthority", "hasRole", "hasAnyRole",
)
ADMIN_RULE_NAMES = ("hasAuthority", "hasAnyAuthority", "hasRole", "hasAnyRole")

ENTRY_RE = re.compile(
    r"\.\s*(requestMatchers|anyRequest)\s*\(([^)]*)\)\s*"
    r"\.\s*(" + "|".join(RULE_NAMES) + r")\s*\(([^)]*)\)"
)
SELECTOR_COUNT_RE = re.compile(r"\.\s*(?:requestMatchers|anyRequest)\s*\(")
HTTP_METHOD_RE = re.compile(
    r"^\s*(?:org\.springframework\.http\.)?HttpMethod\.([A-Za-z]+)\s*(?:,(.*))?$",
    re.DOTALL,
)
ONLY_QUOTED_RE = re.compile(r'^\s*("[^"]*"\s*,\s*)*"[^"]*"\s*$', re.DOTALL)
WILDCARD_CHARS = "*?{"


def _strip_comments(s: str) -> str:
    """Remove // and /* */ comments WITHOUT touching string literals. A naive
    regex would treat the '/*' inside an Ant path literal ("/api/x/**", "/a/*/b")
    as a block-comment start and eat everything up to the next '*/' (which also
    occurs inside path literals) — corrupting the matcher chain. So walk the
    text char by char, tracking whether we are inside a double-quoted string."""
    out = []
    i, n = 0, len(s)
    in_str = False
    while i < n:
        c = s[i]
        if in_str:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(s[i + 1])
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == "/" and i + 1 < n and s[i + 1] == "/":
            while i < n and s[i] != "\n":
                i += 1
            out.append(" ")
            continue
        if c == "/" and i + 1 < n and s[i + 1] == "*":
            i += 2
            while i + 1 < n and not (s[i] == "*" and s[i + 1] == "/"):
                i += 1
            i += 2
            out.append(" ")
            continue
        out.append(c)
        i += 1
    return "".join(out)


def _extract_authz_block(text: str):
    """Return (block_text, modelable). modelable=False forces route (c) to
    credit nothing (fail-closed) when the chain is ambiguous to a static reader:
    not exactly one authorizeHttpRequests/authorizeRequests block, or a
    chain-level securityMatcher scopes the filter chain (we do not model which
    chain wins)."""
    n_blocks = len(re.findall(r"\bauthorize(?:HttpRequests|Requests)\s*\(", text))
    if n_blocks != 1:
        return "", False
    if re.search(r"\.\s*securityMatcher\s*\(", text):
        return "", False
    m = re.search(r"\bauthorize(?:HttpRequests|Requests)\s*\(", text)
    p = m.end() - 1  # index of the '(' that opens the block
    depth = 0
    i = p
    while i < len(text):
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[p + 1:i], True
        i += 1
    return "", False


def _parse_matcher_args(raw: str):
    """(verb_or_None, paths_list_or_None). paths=None => opaque (unparseable
    argument: a variable, a custom RequestMatcher, a builder) => fail-closed."""
    verb = None
    rest = raw
    m = HTTP_METHOD_RE.match(raw)
    if m:
        verb = m.group(1).upper()
        rest = m.group(2) if m.group(2) is not None else ""
    if rest.strip() == "":
        # requestMatchers(HttpMethod.X) with no path = every path for that verb.
        if verb is not None:
            return verb, ["/**"]
        return None, None  # requestMatchers() with nothing meaningful — opaque
    if not ONLY_QUOTED_RE.match(rest):
        return verb, None  # a non-string token is present => opaque
    paths = QUOTED_RE.findall(rest)
    return (verb, paths) if paths else (verb, None)


def _matcher_is_admin(auth_method: str, auth_args: str) -> bool:
    toks = QUOTED_RE.findall(auth_args)
    if not toks:
        return False
    is_role = auth_method.lower() in ("hasrole", "hasanyrole")
    normed = []
    for t in toks:
        if is_role:
            normed.append(t if t.startswith("ROLE_") else "ROLE_" + t)
        else:
            normed.append(t)
    # For an *Any* predicate every alternative must be admin, else a non-admin
    # alternative could reach the surface.
    return all(x == "ROLE_ADMIN" for x in normed)


sec_entries = []
chain_modelable = True
security_config_present = os.path.isfile(security_config_path)

if security_config_present:
    with open(security_config_path, encoding="utf-8") as fh:
        sc_text = fh.read()
    block, modelable = _extract_authz_block(sc_text)
    if not modelable:
        chain_modelable = False
    else:
        block_nc = _strip_comments(block)
        n_selectors = len(SELECTOR_COUNT_RE.findall(block_nc))
        matches = list(ENTRY_RE.finditer(block_nc))
        if len(matches) != n_selectors:
            # A selector is followed by a rule we do not model (e.g. .access(...),
            # .hasIpAddress(...)) or an unparseable shape — fail-closed.
            chain_modelable = False
        for mm in matches:
            selector, sel_args, rule, rule_args = (
                mm.group(1), mm.group(2), mm.group(3), mm.group(4)
            )
            if selector == "anyRequest":
                verb, paths, any_path = None, ["/**"], True
            else:
                verb, paths = _parse_matcher_args(sel_args)
                any_path = False
            if rule in ADMIN_RULE_NAMES and _matcher_is_admin(rule, rule_args):
                authority = "admin"
            else:
                authority = "other"
            sec_entries.append({
                "verb": verb,
                "paths": paths,
                "any_path": any_path,
                "authority": authority,
            })


def _is_simple_pattern(pattern: str) -> bool:
    """True when we can evaluate the pattern precisely: a literal path, or a
    literal followed by a single trailing '/**' or '/*' (no other wildcards)."""
    body = pattern
    if body.endswith("/**"):
        body = body[:-3]
    elif body.endswith("/*"):
        body = body[:-2]
    return not any(c in body for c in WILDCARD_CHARS)


def _covers_simple(pattern: str, path: str) -> bool:
    """Boundary-aware Ant match for the precisely-supported forms. `/api/admin/**`
    covers `/api/admin` and `/api/admin/<anything>` but NOT `/api/administrator`.
    `/api/admin/*` covers exactly one further segment. Exact patterns match
    exactly."""
    if pattern.endswith("/**"):
        base = pattern[:-3]
        return path == base or path.startswith(base + "/")
    if pattern.endswith("/*"):
        base = pattern[:-2]
        if not path.startswith(base + "/"):
            return False
        rest = path[len(base) + 1:]
        return rest != "" and "/" not in rest
    return path == pattern


def _literal_prefix(pattern: str) -> str:
    for i, c in enumerate(pattern):
        if c in WILDCARD_CHARS:
            return pattern[:i]
    return pattern


def _ant_match(pattern: str, path: str) -> str:
    """'yes' | 'no' | 'maybe'. 'maybe' only for wildcard shapes we cannot
    evaluate precisely AND whose literal prefix does not rule the path out —
    the caller treats 'maybe' as indeterminate (fail-closed)."""
    if not pattern or not path:
        return "no"
    if _is_simple_pattern(pattern):
        return "yes" if _covers_simple(pattern, path) else "no"
    # Unsupported wildcard shape (mid '*', '?', '{var}', mid '**').
    if not path.startswith(_literal_prefix(pattern)):
        return "no"
    return "maybe"


def _first_matcher_authority(verb: str, full_path: str) -> str:
    """'admin' | 'other' | 'indeterminate' for the FIRST declared matcher that
    matches (verb, full_path). Fail-closed: a non-modelable chain, an opaque
    matcher, or an unresolvable wildcard ('maybe') => 'indeterminate'."""
    if not chain_modelable:
        return "indeterminate"
    if not full_path:
        return "indeterminate"
    for e in sec_entries:
        # Verb gate: a verb-specific matcher only matches its own verb; a
        # verb-agnostic matcher (verb is None) matches any verb.
        if e["verb"] is not None and e["verb"] != verb:
            continue
        if e["any_path"]:
            return "admin" if e["authority"] == "admin" else "other"
        if e["paths"] is None:
            # Opaque path we could not parse — cannot rule out a match here.
            return "indeterminate"
        verdicts = [_ant_match(p, full_path) for p in e["paths"]]
        if "yes" in verdicts:
            return "admin" if e["authority"] == "admin" else "other"
        if "maybe" in verdicts:
            return "indeterminate"
        # all 'no' => this entry does not match; continue to the next entry.
    return "other"


def covered_by_security_config(verbs, full_path: str) -> bool:
    """Route (c) credit: EVERY verb the endpoint answers must resolve, by
    first-match in declared order, to an admin-authority matcher."""
    if not sec_entries:
        return False
    for v in verbs:
        if _first_matcher_authority(v, full_path) != "admin":
            return False
    return True


def join_path(base: str, suffix: str) -> str:
    if not suffix:
        result = base or ""
    else:
        result = (base.rstrip("/") + "/" + suffix.lstrip("/")) if base else suffix
    if result and not result.startswith("/"):
        result = "/" + result
    return result


files_scanned = 0
mapped_method_count = 0
violations = []

admin_controllers = []
for root, _dirs, files in os.walk(pkg_dir):
    for fn in sorted(files):
        if fn.endswith("AdminController.java"):
            admin_controllers.append(os.path.join(root, fn))
admin_controllers.sort()

for path in admin_controllers:
    with open(path, encoding="utf-8") as fh:
        lines = fh.readlines()
    files_scanned += 1

    # Class-level authz + class-level @RequestMapping base path, both found by
    # scanning the annotation block directly above the `class Foo` decl.
    class_level_authz = False
    class_base_path = ""
    for idx, line in enumerate(lines):
        if CLASS_DECL_RE.match(line):
            j = idx - 1
            while j >= 0:
                prev = lines[j]
                is_authz = bool(AUTHZ_RE.match(prev))
                m = CLASS_REQUEST_MAPPING_RE.match(prev)
                if is_authz and spel_requires_authority(extract_authz_spel(lines, j)):
                    class_level_authz = True
                if m:
                    class_base_path = m.group(1)
                if is_authz or m or OTHER_ANN_RE.match(prev) or prev.strip() == "":
                    j -= 1
                    continue
                break
            break

    idx = 0
    n = len(lines)
    while idx < n:
        line = lines[idx]
        mapmatch = MAPPING_RE.match(line)
        if not mapmatch:
            idx += 1
            continue

        # Skip the class-level mapping annotation itself (its next non-blank,
        # non-annotation line is a `class` decl, not a method) — otherwise it
        # would be double-counted as a "mapped method" with no method body.
        look = idx + 1
        while look < n and (lines[look].strip() == "" or OTHER_ANN_RE.match(lines[look])):
            look += 1
        if look < n and CLASS_DECL_RE.match(lines[look]):
            idx += 1
            continue

        mapping_lineno = idx + 1
        mapped_method_count += 1

        # The verb(s) this endpoint answers (route (c) hardening 4).
        annot = mapmatch.group(1)
        if annot == "RequestMapping":
            joined_ann = "".join(lines[idx:idx + 5])
            methods = REQUEST_METHOD_RE.findall(joined_ann)
            verbs = [x.upper() for x in methods] if methods else list(ALL_VERBS)
        else:
            verbs = VERB_BY_ANNOT[annot]

        method_suffix = ""
        mq = QUOTED_RE.search(line)
        if mq:
            method_suffix = mq.group(1)

        # A method-level @Pre/@PostAuthorize OVERRIDES the class-level one
        # (Spring method-security precedence). So when the method carries its
        # own annotation, its effectiveness is decided SOLELY by that
        # annotation — a method-level permitAll() defeats a class-level
        # ROLE_ADMIN. Only when the method has no own annotation does it
        # inherit the class-level effective coverage.
        method_has_own_annotation = False
        method_authz_effective = False
        j = idx
        while j < n:
            l2 = lines[j]
            if AUTHZ_RE.match(l2):
                method_has_own_annotation = True
                if spel_requires_authority(extract_authz_spel(lines, j)):
                    method_authz_effective = True
            if METHOD_DECL_RE.match(l2) and not l2.strip().startswith("@"):
                break
            j += 1

        has_authz = method_authz_effective if method_has_own_annotation else class_level_authz

        if not has_authz:
            full_path = join_path(class_base_path, method_suffix)
            if not covered_by_security_config(verbs, full_path):
                vlabel = "|".join(verbs)
                violations.append(
                    f"{path}:{mapping_lineno}: mapped method [{vlabel} {full_path}] "
                    f"has no effective @PreAuthorize/@PostAuthorize, and no admin-authority "
                    f"SecurityConfig matcher is the FIRST (declared-order, verb-aware) match "
                    f"for every verb it answers"
                )
        idx = j + 1

if files_scanned == 0 or mapped_method_count == 0:
    print(
        "admin_preauthorize_guard: ZERO_SCAN — no *AdminController.java with a "
        "mapped method found under " + pkg_dir,
        file=sys.stderr,
    )
    sys.exit(2)

if security_config_present and chain_modelable:
    cfg_note = (f"{len(sec_entries)} SecurityConfig authorization entr(ies) modeled "
                f"(verb + declared-order aware)")
elif security_config_present and not chain_modelable:
    cfg_note = ("SecurityConfig present but NOT fully modelable — route (c) fail-closed "
                "(credits nothing; every admin endpoint must carry an effective @PreAuthorize)")
else:
    cfg_note = "no SecurityConfig.java at expected path — route (c) inactive (annotation routes only)"

print(f"admin_preauthorize_guard: scanned {files_scanned} *AdminController.java "
      f"file(s), {mapped_method_count} mapped method(s); {cfg_note}")

if violations:
    print("VIOLATION: admin endpoint reachable with no authorization check (IDOR shape):", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every *AdminController mapped method must be covered by a class-level "
        "or method-level @PreAuthorize/@PostAuthorize, OR — for EVERY verb it "
        "answers — by a declared-order SecurityConfig matcher whose FIRST match "
        "requires admin authority (ROLE_ADMIN). A verb-scoped admin matcher does "
        "NOT protect the verbs it omits (they fall through to the next matcher). "
        "See LedgerAdminController (annotation route) or FeatureFlagAdminController "
        "(verb-agnostic SecurityConfig route) for the clean shapes.",
        file=sys.stderr,
    )
    print(f"admin_preauthorize_guard: {len(violations)} violation(s) — "
          f"ADMIN_ENDPOINT_MISSING_PREAUTHORIZE — BLOCKED", file=sys.stderr)
    sys.exit(1)

print("admin_preauthorize_guard: every admin endpoint is covered by "
      "@PreAuthorize/@PostAuthorize or a verb-aware admin-authority SecurityConfig matcher")
sys.exit(0)
PY
