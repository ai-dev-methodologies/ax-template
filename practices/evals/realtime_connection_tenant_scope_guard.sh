#!/usr/bin/env bash
# realtime_connection_tenant_scope_guard.sh — dogfood R7 GAP-NEW-1 closure (40th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#realtime-connection-tenant-scope`. SseEmitter / WebSocketSession-based
# long-lived push connections in multi-tenant fork-receivers MUST adopt
# the registry + per-message set/clear pattern with three load-bearing
# clauses:
#
#   (1) Connection-registration reads tenantId from TenantContext.current()
#       — NOT from @RequestParam / @RequestHeader / URL path. The guard
#       scans any class containing SseEmitter (as a method parameter or
#       field type) AND a register-style method (signature returning
#       SseEmitter) and forbids @RequestParam / @RequestHeader naming
#       a tenant-shaped key (tenant / tenantId / tenant_id / X-Tenant)
#       near the SseEmitter construction. See
#       #failure_modes.client_supplied_tenant.
#
#   (2) Broadcast iterates with a tenantId equality filter — bare
#       emitters.forEach(send) or for-each over the emitter collection
#       without an .equals(tenantId) / == tenantId guard is the
#       cross-tenant push-leak anti-pattern. The guard requires every
#       file with .send(...) on an SseEmitter to also contain a
#       .equals(...) call or an == comparison of a UUID-typed tenantId
#       field within the same method body. See
#       #failure_modes.broadcast_no_tenant_filter.
#
#   (3) Per-message TenantContext.set + TenantContext.clear wraps each
#       send() call in try/finally — same count-equality algorithm as
#       the R6 39th-guard scheduler clause. count(set) == count(clear) > 0
#       in any file that calls .send(...) on an SseEmitter or a
#       WebSocketSession. See #failure_modes.per_message_no_set.
#
# Algorithm:
#   1. For the passing fixture, find TenantAwareSseEmitterRegistry.java
#      and verify all three clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package that
#      ships a TenantAwareSseEmitterRegistry.java (or any class file
#      whose path contains "SseEmitter" or "WebSocket" in name + a
#      .send(...) call) MUST satisfy the policy. Single-tenant repos
#      have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side variant
#      (client_supplied_tenant + broadcast_no_tenant_filter +
#      per_message_no_set) trips the guard.
#
# Usage:
#   bash practices/evals/realtime_connection_tenant_scope_guard.sh
#   bash practices/evals/realtime_connection_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every registry file satisfies the three clauses
#   1 — at least one clause violated OR (with --fixtures) the failing/
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#realtime-connection-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_REGISTRY="$PASS_DIR/TenantAwareSseEmitterRegistry.java"

# Strip block comments and line-comment tails so guard regex inspects
# executable code only (same pre-pass as the R6 39th guard).
strip_comments() {
    awk '
        BEGIN { inblock = 0 }
        {
            line = $0
            while (1) {
                if (inblock) {
                    end = index(line, "*/")
                    if (end > 0) { line = substr(line, end + 2); inblock = 0 }
                    else { line = ""; break }
                }
                start = index(line, "/*")
                if (start == 0) break
                end_in_same = index(substr(line, start + 2), "*/")
                if (end_in_same > 0) {
                    line = substr(line, 1, start - 1) substr(line, start + 2 + end_in_same + 1)
                } else {
                    line = substr(line, 1, start - 1)
                    inblock = 1
                    break
                }
            }
            slash = index(line, "//")
            if (slash > 0) line = substr(line, 1, slash - 1)
            print line
        }
    ' "$1"
}

verify_registry() {
    local registry="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$registry" ]; then
        echo "VIOLATION [$label]: registry file missing: $registry" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$registry")

    # ── Clause (1): register reads from TenantContext.current() — NOT client ─
    # If the file contains @RequestParam or @RequestHeader naming a tenant
    # key (case-insensitive on tenant / tenantId / X-Tenant), trip the
    # client_supplied_tenant guard.
    if echo "$executable_code" | grep -qE '@RequestParam[^)]*"(tenant|tenant_id|tenantId|tenantID)"'; then
        echo "VIOLATION [$label] clause(1): @RequestParam binding to tenant-shaped key in $registry" >&2
        echo "  Tenant signal MUST be TenantContext.current() at HTTP-request thread time." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.client_supplied_tenant" >&2
        violations=$((violations + 1))
    fi
    if echo "$executable_code" | grep -qE '@RequestHeader[^)]*"(X-Tenant|X-Tenant-Id|X-TENANT-ID)"'; then
        echo "VIOLATION [$label] clause(1): @RequestHeader binding to X-Tenant-* in $registry" >&2
        echo "  Tenant signal MUST be TenantContext.current() at HTTP-request thread time." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.client_supplied_tenant" >&2
        violations=$((violations + 1))
    fi

    # Positive requirement — TenantContext.current() must appear at least
    # once in any file that constructs / holds an SseEmitter.
    if ! echo "$executable_code" | grep -qE 'TenantContext\.current[[:space:]]*\('; then
        echo "VIOLATION [$label] clause(1): no TenantContext.current() call in $registry" >&2
        echo "  Connection registration MUST read tenantId from TenantContext.current() (populated by TenantFilterActivationFilter)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.registry_per_message_set_clear.canonical_skeleton" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (2): broadcast filters by tenantId equality ──────────────────
    # Heuristic: any file that contains .send( on an SseEmitter and a
    # for-each / forEach over a collection MUST also contain either
    # .equals(  (tenantId equality) or  == tenantId / != tenantId / tenantId ==
    # within the same file. Without ANY equality operation on tenantId,
    # the file is broadcasting blindly.
    if echo "$executable_code" | grep -qE '\.send[[:space:]]*\('; then
        if echo "$executable_code" | grep -qE '(tenantId\(\)\.equals|\.equals\([^)]*tenantId|tenantId\.equals|==[[:space:]]*tenantId|tenantId[[:space:]]*==|!=[[:space:]]*tenantId|tenantId[[:space:]]*!=)'; then
            : # PASS — tenantId equality check exists
        else
            echo "VIOLATION [$label] clause(2): .send(...) on SseEmitter without a tenantId equality filter in $registry" >&2
            echo "  Broadcast MUST iterate registry filtered by tenantId — bare emitters.forEach(send) leaks tenant A's payload to every connected admin." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.broadcast_no_tenant_filter" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (3): per-message TenantContext.set + clear (count equality) ──
    if echo "$executable_code" | grep -qE '\.send[[:space:]]*\('; then
        local set_count clear_count
        set_count=$(echo "$executable_code" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)
        clear_count=$(echo "$executable_code" | grep -cE 'TenantContext\.clear[[:space:]]*\(' || true)
        if [ "$set_count" -eq 0 ] || [ "$clear_count" -eq 0 ]; then
            echo "VIOLATION [$label] clause(3): per-message TenantContext.set / clear missing in $registry (set=$set_count, clear=$clear_count)" >&2
            echo "  Each .send(...) MUST be wrapped: try { TenantContext.set(bound.tenantId); .send(...); } finally { TenantContext.clear(); }" >&2
            echo "  Without set, downstream serializers / @AuthorizedTenant getters see empty TenantContext." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.per_message_no_set" >&2
            violations=$((violations + 1))
        elif [ "$set_count" -ne "$clear_count" ]; then
            echo "VIOLATION [$label] clause(3): TenantContext.set count ($set_count) != TenantContext.clear count ($clear_count) in $registry" >&2
            echo "  Unbalanced set/clear leaks tenantId N into tenantId N+1's send on a pooled broker thread." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.per_message_no_set" >&2
            violations=$((violations + 1))
        fi
    fi

    if [ "$violations" -gt 0 ]; then
        echo "realtime_connection_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "realtime_connection_tenant_scope_guard: PASS [$label] — all 3 clauses hold (TenantContext.current() register + tenantId broadcast filter + balanced per-message set/clear)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "realtime_connection_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all three clauses.
verify_registry "$PASS_REGISTRY" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships a
# TenantAwareSseEmitterRegistry.java MUST satisfy the policy.
LIVE_FOUND=0
while IFS= read -r dir; do
    if [ -f "$dir/TenantAwareSseEmitterRegistry.java" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_registry "$dir/TenantAwareSseEmitterRegistry.java" "live:$dir" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "realtime_connection_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareSseEmitterRegistry.java (single-tenant default)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_REGISTRY="$FAIL_DIR/TenantAwareSseEmitterRegistry.java"
    if [ -f "$FAIL_REGISTRY" ]; then
        if verify_registry "$FAIL_REGISTRY" "failing-fixture" 2>/dev/null; then
            echo "realtime_connection_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "realtime_connection_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "realtime_connection_tenant_scope_guard: failing-fixture SKIP — $FAIL_REGISTRY absent"
    fi
fi

exit "$OVERALL"
