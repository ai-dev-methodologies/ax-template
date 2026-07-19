#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.saas-subscription/scenario-guards/pagination_envelope_contract_parity.sh
#
# HAND-ROLLED — capability-gap signal (the dogfood brief's ADDITIONAL
# REQUIREMENT: "a cross-boundary test validating FE pagination-state parsing
# against the BE PageEnvelope contract schema in one integrated run").
#
# Confirmed absent from the catalog before hand-rolling: `grep -ril
# "PageEnvelope\|totalElements\|hasMore" frontend/src` returns NOTHING — no FE
# code anywhere in the reference workload consumes the BE's canonical
# PAGE-OFFSET-001 envelope shape, and no catalog asset (shell guard, ESLint
# rule, or L2/L0 helper) cross-checks an FE parser's field names against the
# REAL backend/src/.../common/PageEnvelope.java source in one run. The
# nearest catalog neighbors solve adjacent-but-different problems:
#   - templates/L2/blocks/pagination.tsx takes flat {page,pageSize,total}
#     PROPS — it is a display component, not a response-envelope parser.
#   - templates/L0/fork-receiver-kit/use-url-list-state.ts owns URL-backed
#     page/sort/filter STATE — it never touches a server response body.
#   - practices/evals/*.sh guards never read frontend/src at all.
# So this scenario hand-rolls the check here, isolated to this scenario dir,
# modeled on the catalog's own text-scanning shell-guard style
# (admin_preauthorize_guard.sh / controller_problemdetail_guard.sh's
# annotation-then-declaration parsing) — a python3 heredoc doing structural
# text analysis, no compiler/bundler invoked.
#
# WHAT IT ENFORCES (cross-boundary, "one integrated run")
# 1. Reads the REAL backend/src/main/java/.../common/PageEnvelope.java to
#    derive the CANONICAL field names live from the actual BE contract
#    (never a hardcoded copy — if the BE record ever renames a field, this
#    check's expectations move with it):
#      outer record:      PageEnvelope(data, pagination)
#      inner record:      Pagination(page, pageSize, totalElements,
#                                     totalPages, hasMore)
# 2. Reads the FE parser module under
#      --root/src/features/subscription/parse-page-envelope.ts
#    and checks it actually DEREFERENCES every canonical field
#    (`.data`, `.pagination.page`, `.pagination.pageSize`,
#    `.pagination.totalElements`, `.pagination.totalPages`,
#    `.pagination.hasMore`) — not merely that the file compiles or "looks
#    plausible" in isolation.
# A parser that instead guesses a legacy/flat shape ({items,total,
# hasNextPage}) — a realistic AI-integration bug: matching the WRONG
# contract — dereferences none of the canonical BE field paths, so this
# guard catches the drift PageEnvelope.java's own doc comment warns about
# ("every domain re-typed the page response and the shapes DIVERGED").
#
# ZERO_SCAN safety: if the real BE source is missing, or its two records
# fail to parse into the expected field lists, this is an environment/usage
# problem (exit 2) — never a silent pass.
#
# Exit codes:
#   0 — the FE parser dereferences every canonical field
#   1 — at least one canonical field is never dereferenced
#       (signature: PAGINATION_ENVELOPE_CONTRACT_MISMATCH)
#   2 — usage / environment error (root missing, BE source missing/unparseable,
#       python3 missing, FE parser file missing)
#
# Usage: bash pagination_envelope_contract_parity.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "pagination_envelope_contract_parity: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "pagination_envelope_contract_parity: --root DIR is required" >&2
    exit 2
fi

FE_FILE="$ROOT_OVERRIDE/src/features/subscription/parse-page-envelope.ts"
if [ ! -f "$FE_FILE" ]; then
    echo "pagination_envelope_contract_parity: no FE parser at $FE_FILE" >&2
    exit 2
fi

BE_FILE="$REPO_ROOT/backend/src/main/java/com/ax/template/authblueprint/common/PageEnvelope.java"
if [ ! -f "$BE_FILE" ]; then
    echo "pagination_envelope_contract_parity: real BE contract not found at $BE_FILE" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "pagination_envelope_contract_parity: python3 not in PATH" >&2
    exit 2
fi

BE_FILE="$BE_FILE" FE_FILE="$FE_FILE" python3 - <<'PY'
import os
import re
import sys

be_file = os.environ["BE_FILE"]
fe_file = os.environ["FE_FILE"]

be_src = open(be_file, encoding="utf-8").read()
fe_src = open(fe_file, encoding="utf-8").read()

# Derive the canonical field names LIVE from the real BE record declarations
# (never hardcoded) — cross-boundary anchoring per the dogfood brief.
outer_m = re.search(r"record\s+PageEnvelope<T>\s*\(([^)]*)\)", be_src)
inner_m = re.search(r"record\s+Pagination\s*\(([^)]*)\)", be_src, re.DOTALL)
if not outer_m or not inner_m:
    print(
        "pagination_envelope_contract_parity: could not parse PageEnvelope/Pagination "
        "record components from " + be_file,
        file=sys.stderr,
    )
    sys.exit(2)

def component_names(params_src):
    names = []
    for part in params_src.split(","):
        part = part.strip()
        if not part:
            continue
        names.append(part.split()[-1])
    return names

outer_fields = component_names(outer_m.group(1))       # e.g. ["data", "pagination"]
inner_fields = component_names(inner_m.group(1))        # e.g. ["page","pageSize","totalElements","totalPages","hasMore"]

if "data" not in outer_fields or not inner_fields:
    print(
        "pagination_envelope_contract_parity: unexpected record shape "
        f"(outer={outer_fields} inner={inner_fields})",
        file=sys.stderr,
    )
    sys.exit(2)

checks = [("data", re.compile(r"\.\s*data\b"))]
for f in inner_fields:
    checks.append((f"pagination.{f}", re.compile(r"\.\s*pagination\s*\.\s*" + re.escape(f) + r"\b")))

missing = [label for label, pattern in checks if not pattern.search(fe_src)]

print(
    f"pagination_envelope_contract_parity: canonical fields from {os.path.basename(be_file)} "
    f"= data, pagination.{{{', '.join(inner_fields)}}}"
)
print(f"pagination_envelope_contract_parity: scanned {fe_file}")

if missing:
    print(
        "VIOLATION: FE parser never dereferences the following canonical "
        "BE envelope field(s) — it likely parses a DIFFERENT (guessed/legacy) "
        f"shape: {', '.join(missing)}",
        file=sys.stderr,
    )
    print(
        "pagination_envelope_contract_parity: "
        "PAGINATION_ENVELOPE_CONTRACT_MISMATCH — BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print("pagination_envelope_contract_parity: FE parser dereferences every canonical field — contract parity holds")
sys.exit(0)
PY
