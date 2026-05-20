#!/usr/bin/env bash
# practices/evals/multi_tenant_aop_guard_skeleton_guard.sh — dogfood-5 (30th hard guard).
#
# Mechanically enforces practices/rules/multi-tenant-aop-guard-skeleton.md.
# Every `.../multitenancy/` subpackage that ships a multi-tenant skeleton
# MUST contain the 11 canonical files defined in
# blueprints/multi-tenant-manifest.yaml. Fork-receiver adoption is
# mechanical substitution of `<root>` — this guard verifies completeness.
#
# Closes P2 Round 3 GAP-NEW-2 (manifest aop-guard named
# AuthorizedTenantInterceptor + the @AuthorizedTenant/@TenantId annotation
# pair but shipped no body — fork-receivers stalled at the most
# security-critical 60 lines of multi-tenant adoption, risking
# 403-vs-404 existence leakage and tenant_id detail leakage).
#
# Mode 1 — default (live repo + passing fixture): asserts the passing
#   fixture has all 11 files; asserts no `.../multitenancy/` package in
#   the live repo root is half-adopted.
# Mode 2 — --fixtures: ALSO asserts the failing/ sibling fixture trips
#   the guard (exit 1) — proves the guard can detect omission.
#
# Usage:
#   bash practices/evals/multi_tenant_aop_guard_skeleton_guard.sh
#   bash practices/evals/multi_tenant_aop_guard_skeleton_guard.sh --fixtures
#
# Exit codes:
#   0 — every multitenancy/ package contains the 11 expected files
#   1 — at least one file missing OR failing/ fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

REQUIRED_FILES=(
    "TenantContext.java"
    "TenantOwned.java"
    "TenantBoundaryViolationException.java"
    "TenantContextMissingException.java"
    "MultiTenantProblemDetailAdvice.java"
    "TenantAwareAsyncConfig.java"
    "TenantContextAwareTaskDecorator.java"
    "TenantFilterActivationFilter.java"
    "AuthorizedTenant.java"
    "TenantId.java"
    "AuthorizedTenantInterceptor.java"
    # 12th file added R4 — closes GAP-R3-3 (audit/ledger entity outside
    # tenant-scoped request boundary). Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#ledger-audit-tenant-scope and
    # is content-verified by ledger_audit_tenant_nullable_guard.sh (37th).
    "AuditEvent.java"
)

verify_dir() {
    local label="$1"
    local dir="$2"
    local missing=0
    for f in "${REQUIRED_FILES[@]}"; do
        if [ ! -f "$dir/$f" ]; then
            echo "VIOLATION [$label]: missing $f in $dir" >&2
            missing=$((missing + 1))
        fi
    done
    if [ "$missing" -gt 0 ]; then
        echo "multi_tenant_aop_guard_skeleton: FAIL [$label] — $missing required file(s) missing" >&2
        return 1
    fi
    echo "multi_tenant_aop_guard_skeleton: PASS [$label] — all ${#REQUIRED_FILES[@]} files present"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "multi_tenant_aop_guard_skeleton: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Live-repo scan: every backend `.../multitenancy/` package (if any) must be
# fully adopted. Single-tenant repos have zero such packages and SKIP.
LIVE_PASS=1
LIVE_FOUND=0
while IFS= read -r dir; do
    LIVE_FOUND=$((LIVE_FOUND + 1))
    verify_dir "live:$dir" "$dir" || LIVE_PASS=0
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "multi_tenant_aop_guard_skeleton: live-repo SKIP — no .../multitenancy/ package (single-tenant default)"
fi

# Passing fixture: the canonical fork-receiver simulation MUST have all 11 files.
PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
PASS_OK=1
verify_dir "passing-fixture" "$PASS_DIR" || PASS_OK=0

OVERALL=0
[ "$LIVE_PASS" -eq 0 ] && OVERALL=1
[ "$PASS_OK"   -eq 0 ] && OVERALL=1

if [ "$MODE" = "fixtures" ]; then
    # Failing fixture: omits AuthorizedTenantInterceptor.java. The guard MUST trip.
    FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"
    if verify_dir "failing-fixture" "$FAIL_DIR" 2>/dev/null; then
        echo "multi_tenant_aop_guard_skeleton: FAIL — failing/ fixture unexpectedly passes (guard cannot detect missing AuthorizedTenantInterceptor)" >&2
        OVERALL=1
    else
        echo "multi_tenant_aop_guard_skeleton: PASS [failing-fixture-detected] — failing/ correctly trips guard"
    fi
fi

exit "$OVERALL"
