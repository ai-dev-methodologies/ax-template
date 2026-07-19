#!/usr/bin/env bash
# practices/evals/admin_preauthorize_guard.sh
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
# must be unreachable without an authorization check. A mapped method is
# COVERED if EITHER:
#   (a) a class-level @PreAuthorize/@PostAuthorize covers every method, OR
#   (b) the method itself carries its own @PreAuthorize/@PostAuthorize, OR
#   (c) the endpoint's resolved path (class-level @RequestMapping prefix +
#       method-level mapping suffix) is matched by a
#       `.requestMatchers("<pattern>").hasAuthority(...)/.hasRole(...)` rule
#       in .../security/SecurityConfig.java (Ant-style "/**" prefix match).
# Route (c) matters because it is the REAL, documented mechanism this repo's
# own admin controllers already use in several places — e.g.
# FeatureFlagAdminController's own Javadoc: "...so this controller does not
# re-declare @PreAuthorize" — SecurityConfig's
# `.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")` (PAYMENT-AUTHZ-004)
# already covers it. Requiring annotations ONLY would make this guard a
# false-positive generator against the live repo's own legitimate,
# already-reviewed architecture; route (c) recognizes an existing control,
# it does not create a loophole — a controller with NEITHER an annotation
# NOR a covering SecurityConfig matcher is still BLOCKED.
# Route (c) is skipped gracefully (empty pattern set) when SecurityConfig.java
# is absent at the expected path (e.g. a minimal fixture root) — coverage
# then falls back to (a)/(b) only, matching the original hand-rolled
# scenario-guard behavior exactly.
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
    r"^\s*@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\b"
)
AUTHZ_RE = re.compile(r"^\s*@(PreAuthorize|PostAuthorize)\b")
OTHER_ANN_RE = re.compile(r"^\s*@\w")
CLASS_DECL_RE = re.compile(r"^\s*(?:public\s+)?(?:final\s+)?class\s+\w+")
METHOD_DECL_RE = re.compile(r"^\s*(?:public|protected|private).*\(.*")
CLASS_REQUEST_MAPPING_RE = re.compile(r'^\s*@RequestMapping\s*\(\s*"([^"]*)"')
QUOTED_RE = re.compile(r'"([^"]*)"')

# ── Route (c): SecurityConfig path-matcher cross-check (optional) ───────────
# Extract path prefixes from every
# `.requestMatchers(<args>).hasAuthority("...")` / `.hasRole("...")` call.
# <args> may include a leading HttpMethod.X token — only quoted path strings
# are extracted. This is a shell-level dogfood proof, not a full Spring
# Security simulator: it does not model matcher ORDER/precedence, only
# "does ANY authority-scoped requestMatcher cover this path".
admin_path_prefixes = []
if os.path.isfile(security_config_path):
    with open(security_config_path, encoding="utf-8") as fh:
        sc_text = fh.read()
    for m in re.finditer(
        r"\.requestMatchers\(([^)]*)\)\s*\.\s*(?:hasAuthority|hasRole)\(",
        sc_text,
    ):
        args = m.group(1)
        for path in QUOTED_RE.findall(args):
            prefix = re.sub(r"/\*\*?$", "", path)  # strip trailing "/**" or "/*"
            admin_path_prefixes.append(prefix)


def covered_by_security_config(full_path: str) -> bool:
    if not full_path:
        return False
    return any(prefix and full_path.startswith(prefix) for prefix in admin_path_prefixes)


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
                if is_authz:
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
        if not MAPPING_RE.match(line):
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

        method_suffix = ""
        mq = QUOTED_RE.search(line)
        if mq:
            method_suffix = mq.group(1)

        has_authz = class_level_authz
        j = idx
        while j < n:
            l2 = lines[j]
            if AUTHZ_RE.match(l2):
                has_authz = True
            if METHOD_DECL_RE.match(l2) and not l2.strip().startswith("@"):
                break
            j += 1

        if not has_authz:
            full_path = join_path(class_base_path, method_suffix)
            if not covered_by_security_config(full_path):
                violations.append(
                    f"{path}:{mapping_lineno}: mapped method has no @PreAuthorize/@PostAuthorize "
                    f"and no matching SecurityConfig hasAuthority/hasRole requestMatcher for '{full_path}'"
                )
        idx = j + 1

if files_scanned == 0 or mapped_method_count == 0:
    print(
        "admin_preauthorize_guard: ZERO_SCAN — no *AdminController.java with a "
        "mapped method found under " + pkg_dir,
        file=sys.stderr,
    )
    sys.exit(2)

print(f"admin_preauthorize_guard: scanned {files_scanned} *AdminController.java "
      f"file(s), {mapped_method_count} mapped method(s), "
      f"{len(admin_path_prefixes)} SecurityConfig admin-authority pattern(s)")

if violations:
    print("VIOLATION: admin endpoint reachable with no authorization check (IDOR shape):", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every *AdminController mapped method must be covered by a class-level "
        "or method-level @PreAuthorize/@PostAuthorize, OR by a "
        ".requestMatchers(...).hasAuthority(...)/.hasRole(...) rule in "
        "SecurityConfig.java whose path pattern covers the endpoint. See "
        "LedgerAdminController (annotation route) or FeatureFlagAdminController "
        "(SecurityConfig route) for the clean shapes.",
        file=sys.stderr,
    )
    print(f"admin_preauthorize_guard: {len(violations)} violation(s) — "
          f"ADMIN_ENDPOINT_MISSING_PREAUTHORIZE — BLOCKED", file=sys.stderr)
    sys.exit(1)

print("admin_preauthorize_guard: every admin endpoint is covered by "
      "@PreAuthorize/@PostAuthorize or a SecurityConfig authority-scoped requestMatcher")
sys.exit(0)
PY
