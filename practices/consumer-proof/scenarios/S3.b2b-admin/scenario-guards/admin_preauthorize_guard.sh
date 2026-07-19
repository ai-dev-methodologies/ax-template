#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.b2b-admin/scenario-guards/admin_preauthorize_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# The IDOR invariant this enforces ("every mapped method inside a
# *AdminController is reachable ONLY under a @PreAuthorize/@PostAuthorize
# check — either class-level or method-level") has NO standalone,
# --root-parameterized shell guard in practices/evals/ (verified:
# `grep -ril preAuthorize practices/evals/` returns only role_literal_guard.sh,
# which validates that @PreAuthorize authority STRINGS are known-valid — it
# does not check whether an admin endpoint has @PreAuthorize AT ALL — and
# run-all-guards.sh, a mention in its own file list). The nearest catalog
# asset, controller_repository_shell_guard.sh, enforces a DIFFERENT invariant
# (controller->repository layering). So this scenario cannot reuse a catalog
# asset for the "admin GET without @PreAuthorize" violation named in the
# dogfood brief — it is hand-rolled here, isolated to this scenario dir,
# modeled on the catalog's own text-scanning shell-guard style
# (controller_problemdetail_guard.sh's annotation-then-declaration parsing).
#
# WHAT IT ENFORCES
# Every *AdminController.java under
#   backend/src/main/java/com/ax/template/authblueprint/**
# must be unreachable without an authorization check:
#   - a class-level @PreAuthorize/@PostAuthorize covers every method, OR
#   - every @GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@RequestMapping
#     method carries its OWN @PreAuthorize/@PostAuthorize annotation.
# A mapped method with neither is the IDOR shape: any caller who reaches the
# route reaches the handler, tenant/role scoping notwithstanding.
#
# ZERO_SCAN safety: package dir must exist AND contain at least one
# *AdminController.java with at least one mapped method, or this is treated
# as an environment/usage problem (exit 2) rather than a silent pass — a
# consumer with no admin controllers at all should just not invoke this guard.
#
# Exit codes:
#   0 — every mapped method in every *AdminController.java is covered
#   1 — at least one mapped method is reachable with no authz check
#       (signature: ADMIN_ENDPOINT_MISSING_PREAUTHORIZE)
#   2 — usage / environment error (root missing, python3 missing, zero-scan)
#
# Usage: bash admin_preauthorize_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "admin_preauthorize_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "admin_preauthorize_guard: --root DIR is required" >&2
    exit 2
fi

PKG_DIR="$ROOT_OVERRIDE/backend/src/main/java/com/ax/template/authblueprint"
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

MAPPING_RE = re.compile(
    r"^\s*@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\b"
)
AUTHZ_RE = re.compile(r"^\s*@(PreAuthorize|PostAuthorize)\b")
OTHER_ANN_RE = re.compile(r"^\s*@\w")
CLASS_DECL_RE = re.compile(r"^\s*(?:public\s+)?(?:final\s+)?class\s+\w+")
METHOD_DECL_RE = re.compile(r"^\s*(?:public|protected|private).*\(.*")

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

    # Strip block/line comments per-line is not attempted here (mirrors the
    # catalog's controller_problemdetail_guard which also works line-by-line
    # on annotation markers — javadoc lines start with '*' or '/*' and never
    # match the '@Xxx' annotation regexes below, so no comment-stripping pass
    # is needed for this specific check).

    # Class-level authz: an @PreAuthorize/@PostAuthorize line appearing among
    # the annotation block directly above the `class Foo` declaration.
    class_level_authz = False
    for idx, line in enumerate(lines):
        if CLASS_DECL_RE.match(line):
            j = idx - 1
            while j >= 0:
                prev = lines[j]
                if AUTHZ_RE.match(prev):
                    class_level_authz = True
                    break
                if OTHER_ANN_RE.match(prev) or prev.strip() == "":
                    j -= 1
                    continue
                break
            break

    # Every mapped method needs its OWN authz annotation somewhere in the
    # annotation block directly above the method declaration, UNLESS the
    # class-level annotation already covers the whole controller.
    idx = 0
    n = len(lines)
    while idx < n:
        line = lines[idx]
        if not MAPPING_RE.match(line):
            idx += 1
            continue
        mapping_lineno = idx + 1
        mapped_method_count += 1

        # Collect the full annotation block: from this mapping annotation
        # forward through any additional annotation lines, up to the method
        # declaration line.
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
            violations.append(f"{path}:{mapping_lineno}: mapped method has no @PreAuthorize/@PostAuthorize")
        idx = j + 1

if files_scanned == 0 or mapped_method_count == 0:
    print(
        "admin_preauthorize_guard: ZERO_SCAN — no *AdminController.java with a "
        "mapped method found under " + pkg_dir,
        file=sys.stderr,
    )
    sys.exit(2)

print(f"admin_preauthorize_guard: scanned {files_scanned} *AdminController.java "
      f"file(s), {mapped_method_count} mapped method(s)")

if violations:
    print("VIOLATION: admin endpoint reachable with no authorization check (IDOR shape):", file=sys.stderr)
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Every *AdminController mapped method must be covered by a class-level "
        "or method-level @PreAuthorize/@PostAuthorize. See LedgerAdminController "
        "for the clean shape (class-level hasAuthority('ROLE_ADMIN')).",
        file=sys.stderr,
    )
    print(f"admin_preauthorize_guard: {len(violations)} violation(s) — "
          f"ADMIN_ENDPOINT_MISSING_PREAUTHORIZE — BLOCKED", file=sys.stderr)
    sys.exit(1)

print("admin_preauthorize_guard: every admin endpoint is covered by @PreAuthorize/@PostAuthorize")
sys.exit(0)
PY
