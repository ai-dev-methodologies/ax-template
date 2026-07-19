#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.FE/scenario-guards/tenant_scope_missing_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# ADDITIONAL REQUIREMENT named by this dogfood: a runtime multi-tenant
# TenantContext primitive that scopes every repository query to the caller
# tenant. The catalog has NO such runtime `common/` primitive — only a
# design-time `java_skeleton:` block inside
# blueprints/multi-tenant-manifest.yaml (#row-level-strategy.filter_activation)
# and a TenantContext.java that exists ONLY as a fixture under
# practices/evals/fixtures/multi-tenant-aop-guard-skeleton/ (a test double for
# multi_tenant_aop_guard_skeleton_guard.sh, not an importable class). Confirmed
# absent: `find backend/src/main/java/.../common -iname 'TenantContext*'` = 0
# hits. multi_tenant_aop_guard_skeleton_guard.sh itself only scans for the
# NAMES of the skeleton's classes (TenantContext/TenantFilterActivationFilter/
# etc.) existing somewhere in a consumer's tree — it does not check that a
# *Service's actual repository CALLS are tenant-scoped. That call-site gap is
# what this guard closes.
#
# WHAT IT ENFORCES
# For every *Service.java file under --root that imports TenantContext (i.e.
# is a tenant-aware service): find every repository call of the form
# `<something>Repository.<method>(` where <method> is one of
# findById / findAll / getOne / getById / getReferenceById (the plain
# Spring-Data-generated finders every JpaRepository has for free). If that
# call's line does NOT also mention "TenantId" (the tenant-scoped finder
# naming convention, e.g. findByIdAndTenantId/findAllByTenantId), the query
# is NOT scoped to a tenant — a cross-tenant read.
#
# ZERO_SCAN safety: root must contain at least one *Service.java that imports
# TenantContext, or this is an environment/usage problem (exit 2).
#
# Exit codes:
#   0 — every repository finder call in a tenant-aware service is tenant-scoped
#   1 — at least one plain (unscoped) finder call found
#       (signature: TENANT_SCOPE_MISSING)
#   2 — usage / environment error (root missing, zero-scan)
#
# Usage: bash tenant_scope_missing_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "tenant_scope_missing_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "tenant_scope_missing_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "tenant_scope_missing_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

files="$(find "$ROOT_OVERRIDE" -name '*Service.java' 2>/dev/null || true)"
services_scanned=0
finder_calls=0
violations=""

FINDER_RE='Repository\.(findById|findAll|getOne|getById|getReferenceById)\('

while IFS= read -r f; do
    [ -z "$f" ] && continue
    if ! grep -qF 'TenantContext' "$f" 2>/dev/null; then
        continue
    fi
    services_scanned=$((services_scanned + 1))
    hits="$(grep -nE "$FINDER_RE" "$f" 2>/dev/null || true)"
    [ -z "$hits" ] && continue
    while IFS= read -r hitline; do
        [ -z "$hitline" ] && continue
        finder_calls=$((finder_calls + 1))
        if ! printf '%s' "$hitline" | grep -qi 'TenantId'; then
            lineno="${hitline%%:*}"
            violations="$violations
  $f:$lineno: unscoped repository finder — $(printf '%s' "$hitline" | sed 's/^[0-9]*://' | sed 's/^ *//')"
        fi
    done <<EOF
$hits
EOF
done <<EOF
$files
EOF

if [ "$services_scanned" -eq 0 ]; then
    echo "tenant_scope_missing_guard: ZERO_SCAN — no *Service.java importing TenantContext found under $ROOT_OVERRIDE" >&2
    exit 2
fi

echo "tenant_scope_missing_guard: scanned $services_scanned tenant-aware service(s), $finder_calls plain-finder call(s)"

if [ -n "$violations" ]; then
    echo "VIOLATION: repository query not scoped to the caller tenant:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Every repository read in a tenant-aware service MUST use a" >&2
    echo "TenantId-scoped finder (findByIdAndTenantId/findAllByTenantId), bound" >&2
    echo "to TenantContext.current() — never the plain findById/findAll." >&2
    echo "tenant_scope_missing_guard: TENANT_SCOPE_MISSING — BLOCKED" >&2
    exit 1
fi

echo "tenant_scope_missing_guard: every repository finder call in a tenant-aware service is tenant-scoped"
exit 0
