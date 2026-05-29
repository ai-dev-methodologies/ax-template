#!/usr/bin/env bash
# practices/evals/role_literal_guard.sh — IMW2-C hard guard.
#
# THE GAP THIS CLOSES
# -------------------
# IDW2 dogfood (2026-05-29) surfaced a *runtime-only* role deviation that NO
# static guard caught: the signup flow downgraded an UNKNOWN requested role to
# MEMBER, so the only symptom was a confusing later 403. The sibling failure
# mode — that NO existing guard verifies the authority strings hard-coded in
# @PreAuthorize SpEL actually correspond to a role/scope the system can ever
# grant — is what this guard closes statically.
#
# A typo (`hasAuthority('ROLE_ADMINS')`), a stale role name, or a role a
# fork-receiver renamed in the UserRole enum but forgot to update in a
# @PreAuthorize, all produce an authority literal that NOTHING can ever satisfy.
# Such an endpoint is then permanently un-authorizable (every caller 403s) and
# the suite stays green because no test exercises that exact path. This guard
# makes that class of deviation a BLOCK at scan time.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# Every Spring authority literal used in a method/type-level @PreAuthorize SpEL
# across backend/src/main/**/*.java MUST map to a KNOWN authority. The known set
# is derived from the source of truth (not a hand-maintained list):
#
#   ALLOWED = { "ROLE_" + each constant in user/UserRole.java }
#           ∪ { every "ROLE_*" string literal returned by apikey/ApiKeyScope.java }
#
# The UserRole arm covers ADMIN / MANAGER / MEMBER / AUDITOR (whatever the enum
# currently declares). The ApiKeyScope arm covers ROLE_API_READ / ROLE_API_WRITE
# — authorities granted at runtime by the API-key filter (ApiKeyScope.toAuthority),
# NOT by UserRole — so they are legitimate @PreAuthorize targets even though they
# are not enum constants.
#
# SpEL forms parsed (Spring Security method security):
#   hasAuthority('X')              → X
#   hasAnyAuthority('X','Y',...)   → X, Y, ...
#   hasRole('R')                   → ROLE_R   (Spring prepends ROLE_)
#   hasAnyRole('R','S',...)        → ROLE_R, ROLE_S, ...
# Both single and double quotes are accepted. Multi-line @PreAuthorize(...) is
# supported (the SpEL is gathered up to the closing paren).
#
# CALIBRATION (current tree, 2026-05-29) — guard MUST be GREEN here
# ----------------------------------------------------------------
# The only @PreAuthorize authority literals present are:
#   hasAuthority('ROLE_ADMIN')      ×8  → UserRole.ADMIN          ✓
#   hasAuthority('ROLE_API_READ')   ×1  → ApiKeyScope READ        ✓
#   hasAuthority('ROLE_API_WRITE')  ×1  → ApiKeyScope WRITE        ✓
#   hasAnyRole('ADMIN','AUDITOR')   ×1  → ROLE_ADMIN + ROLE_AUDITOR (UserRole) ✓
# All map to the derived ALLOWED set → exit 0. A @PreAuthorize naming an authority
# outside that set (e.g. a typo'd ROLE_ADMINS, or a role dropped from the enum)
# → exit 1.
#
# Exit codes:
#   0 — every @PreAuthorize authority literal maps to a UserRole or ApiKeyScope.
#   1 — at least one authority literal maps to neither.
#   2 — usage / environment error (paths missing, python3 missing).
#
# Usage:
#   bash practices/evals/role_literal_guard.sh
#   bash practices/evals/role_literal_guard.sh --root DIR
#   bash practices/evals/role_literal_guard.sh --verbose
#
# Bash 3.2 compatible. Fast: pure file scan via python3, no gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
VERBOSE=0

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "role_literal_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$REPO_ROOT" ]; then
    echo "role_literal_guard: --root '$REPO_ROOT' is not a directory" >&2
    exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo "role_literal_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

BACKEND_SRC="$REPO_ROOT/backend/src/main"
if [ ! -d "$BACKEND_SRC" ]; then
    # No backend production tree → nothing to scan. Not an error (a fork-receiver
    # may have a frontend-only or as-yet-unpopulated workload).
    echo "role_literal_guard: no backend/src/main at $BACKEND_SRC — nothing to check"
    exit 0
fi

python3 - "$REPO_ROOT" "$VERBOSE" <<'PYEOF'
import pathlib
import re
import sys

repo_root = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"
backend_main = repo_root / "backend" / "src" / "main"

# ── 1. Build the ALLOWED authority set from the sources of truth ───────────────
allowed = set()
allowed_provenance = {}  # authority -> human-readable origin

# 1a. UserRole enum constants → ROLE_<CONST>.
# Find the enum body and pull the bare uppercase identifiers that precede the
# first member with a body / the closing brace. We tolerate constants written as
# `ADMIN,` / `AUDITOR` and ignore methods, comments, and annotations.
user_role_files = list(backend_main.rglob("user/UserRole.java"))
if not user_role_files:
    print("role_literal_guard: cannot find user/UserRole.java under "
          f"{backend_main} — source of truth missing", file=sys.stderr)
    sys.exit(2)

ur_txt = user_role_files[0].read_text(encoding="utf-8", errors="replace")
# Strip block + line comments so a commented-out constant is not mistaken for one.
ur_nc = re.sub(r"/\*.*?\*/", "", ur_txt, flags=re.S)
ur_nc = re.sub(r"//[^\n]*", "", ur_nc)
m = re.search(r"enum\s+UserRole\s*\{(.*?)\}", ur_nc, flags=re.S)
if not m:
    print("role_literal_guard: could not parse the UserRole enum body",
          file=sys.stderr)
    sys.exit(2)
body = m.group(1)
# Enum constants come before the first ';' (which, if present, separates
# constants from members). Constant names are leading UPPERCASE identifiers.
const_region = body.split(";", 1)[0]
role_consts = []
for token in re.split(r",", const_region):
    tok = token.strip()
    cm = re.match(r"^([A-Z][A-Z0-9_]*)\b", tok)
    if cm:
        role_consts.append(cm.group(1))
if not role_consts:
    print("role_literal_guard: parsed zero UserRole constants — refusing to run "
          "with an empty allowed set", file=sys.stderr)
    sys.exit(2)
for c in role_consts:
    auth = "ROLE_" + c
    allowed.add(auth)
    allowed_provenance[auth] = f"UserRole.{c}"

# 1b. ApiKeyScope.toAuthority() return literals → ROLE_API_* (granted by the
# API-key filter, NOT by UserRole). Derive them from the string literals in
# apikey/ApiKeyScope.java rather than hard-coding.
scope_files = list(backend_main.rglob("apikey/ApiKeyScope.java"))
if scope_files:
    sc_txt = scope_files[0].read_text(encoding="utf-8", errors="replace")
    for lit in re.findall(r'"(ROLE_[A-Z0-9_]+)"', sc_txt):
        allowed.add(lit)
        allowed_provenance[lit] = "ApiKeyScope.toAuthority()"
# If ApiKeyScope is absent (a fork-receiver without the API-key domain), the
# guard simply does not add those authorities — and any @PreAuthorize that names
# them would then legitimately fail, because nothing can grant them.

# ── 2. Extract @PreAuthorize authority literals from production code ───────────
# Match @PreAuthorize( ... ) including multi-line; capture the SpEL up to the
# matching-ish closing paren (annotations here never nest parens past the SpEL
# string, so a non-greedy run to the next ')' that closes the annotation works;
# we gather across newlines).
PRE_RE = re.compile(r"@PreAuthorize\s*\((.*?)\)\s*(?:\n|$|//)", re.S)
# Fallback single-line form (covers the common `@PreAuthorize("...")` exactly).
PRE_RE_LINE = re.compile(r'@PreAuthorize\s*\(\s*(".*?")\s*\)')

HAS_AUTH_RE = re.compile(r"hasAuthority\(\s*(['\"])(.+?)\1\s*\)")
HAS_ANY_AUTH_RE = re.compile(r"hasAnyAuthority\(\s*(.+?)\)")
HAS_ROLE_RE = re.compile(r"hasRole\(\s*(['\"])(.+?)\1\s*\)")
HAS_ANY_ROLE_RE = re.compile(r"hasAnyRole\(\s*(.+?)\)")
QUOTED_RE = re.compile(r"['\"]([^'\"]+)['\"]")

java_files = sorted(backend_main.rglob("*.java"))
violations = []   # (file_rel, lineno, authority, spel)
checked = 0       # number of authority literals checked

def line_of(text, idx):
    return text.count("\n", 0, idx) + 1

for jf in java_files:
    try:
        text = jf.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        print(f"role_literal_guard: cannot read {jf}: {exc}", file=sys.stderr)
        sys.exit(2)
    if "@PreAuthorize" not in text:
        continue
    rel = jf.relative_to(repo_root).as_posix()

    # Collect (start_index, spel_text) for each @PreAuthorize annotation.
    spels = []
    for m in PRE_RE.finditer(text):
        spels.append((m.start(), m.group(1)))
    if not spels:
        for m in PRE_RE_LINE.finditer(text):
            spels.append((m.start(), m.group(1)))

    for start_idx, spel in spels:
        lineno = line_of(text, start_idx)
        authorities = []
        for am in HAS_AUTH_RE.finditer(spel):
            authorities.append(am.group(2))
        for am in HAS_ANY_AUTH_RE.finditer(spel):
            authorities.extend(QUOTED_RE.findall(am.group(1)))
        for rm in HAS_ROLE_RE.finditer(spel):
            authorities.append("ROLE_" + rm.group(2))
        for rm in HAS_ANY_ROLE_RE.finditer(spel):
            authorities.extend("ROLE_" + r for r in QUOTED_RE.findall(rm.group(1)))

        for auth in authorities:
            checked += 1
            if auth in allowed:
                if verbose:
                    print(f"  OK  {rel}:{lineno} → {auth} "
                          f"({allowed_provenance.get(auth, '?')})")
            else:
                violations.append((rel, lineno, auth, spel.strip()))

# ── 3. Verdict ─────────────────────────────────────────────────────────────────
allowed_sorted = ", ".join(sorted(allowed))

if violations:
    for rel, lineno, auth, spel in violations:
        print(
            f"VIOLATION [{rel}:{lineno}]: @PreAuthorize authority '{auth}' maps "
            f"to neither a UserRole nor a known API scope. SpEL: {spel}",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(f"Allowed authorities (derived): {allowed_sorted}", file=sys.stderr)
    print(
        "Fix policy: use an authority the system can actually grant. Either name an "
        "existing UserRole (ROLE_<CONST>) / API scope (ApiKeyScope.toAuthority()), "
        "fix the typo, or — if the role is genuinely new — add the constant to "
        "user/UserRole.java (and grant it) so the @PreAuthorize is satisfiable.",
        file=sys.stderr,
    )
    print(
        f"role_literal_guard: {len(violations)} unsatisfiable @PreAuthorize "
        f"authority literal(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"role_literal_guard: PASS — {checked} @PreAuthorize authority literal(s) all "
    f"map to a known authority [{allowed_sorted}]"
)
sys.exit(0)
PYEOF
