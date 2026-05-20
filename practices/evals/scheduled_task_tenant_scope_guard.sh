#!/usr/bin/env bash
# scheduled_task_tenant_scope_guard.sh — dogfood R6 GAP-R3-5 closure (39th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#scheduled-task-tenant-scope`. @Scheduled / Quartz / Shedlock-style
# periodic jobs in multi-tenant fork-receivers MUST adopt the
# per-tenant iteration pattern with three load-bearing properties:
#
#   (1) balanced TenantContext.set / TenantContext.clear via try/finally —
#       strict equality of set count == clear count in the same file.
#       set without clear is the canonical cross-tenant write vector
#       (tenantId N leaks into N+1's iteration on a pooled scheduler
#       thread). See #failure_modes.set_without_clear.
#
#   (2) tenant enumeration via tenantCatalog.listActive() — the SINGLE
#       source of tenant identity. Hardcoded tenant lists, foreign
#       caches, or property-file enumeration let deactivated tenants
#       continue receiving job side effects and force redeploys for
#       new tenants. See #failure_modes.catalog_bypass.
#
#   (3) distributed lock key includes the tenantId substring — Shedlock
#       SpEL "#tenantId" or "${tenantId}" inside @SchedulerLock name.
#       A bare job-name lock serializes all tenants behind one cluster
#       node and turns one tenant's hang into a fleet-wide outage.
#       See #lock_key_contract + #failure_modes.no_lock_key_tenant_scope.
#
# Algorithm:
#   1. For the passing fixture, find TenantIterationScheduler.java and
#      verify all three clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package that
#      ships a TenantIterationScheduler.java MUST satisfy the policy.
#      Single-tenant repos have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side variant (bare
#      lock, missing clear, hardcoded enumeration) trips the guard.
#
# Usage:
#   bash practices/evals/scheduled_task_tenant_scope_guard.sh
#   bash practices/evals/scheduled_task_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every scheduler file satisfies the three clauses
#   1 — at least one clause violated OR (with --fixtures) the failing/
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#scheduled-task-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_SCHEDULER="$PASS_DIR/TenantIterationScheduler.java"
PASS_CATALOG="$PASS_DIR/TenantCatalog.java"

# verify_scheduler SCHEDULER_FILE CATALOG_FILE LABEL
# Returns 0 if all three clauses hold, 1 otherwise.
verify_scheduler() {
    local scheduler="$1"
    local catalog="$2"
    local label="$3"
    local violations=0

    if [ ! -f "$scheduler" ]; then
        echo "VIOLATION [$label]: scheduler file missing: $scheduler" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    # ── Clause (1): set/clear balance ────────────────────────────────────────
    # Strip block comments and line comments before counting so that
    # documentation references like "// TenantContext.clear()" do not
    # contribute to the call-site tally. The guard inspects executable
    # code only; pre-pass scrubs /* ... */ and // ... line tails.
    local executable_code
    executable_code=$(awk '
        BEGIN { inblock = 0 }
        {
            line = $0
            # strip block comments line by line
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
            # strip line comment tail
            slash = index(line, "//")
            if (slash > 0) line = substr(line, 1, slash - 1)
            print line
        }
    ' "$scheduler")

    local set_count clear_count
    set_count=$(echo "$executable_code" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)
    clear_count=$(echo "$executable_code" | grep -cE 'TenantContext\.clear[[:space:]]*\(' || true)

    if [ "$set_count" -eq 0 ]; then
        echo "VIOLATION [$label] clause(1): scheduler file does not call TenantContext.set" >&2
        echo "  Per-tenant iteration MUST set TenantContext per iteration; bare repository.findAll() without context is forbidden." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.forbidden_alternatives" >&2
        violations=$((violations + 1))
    elif [ "$set_count" -ne "$clear_count" ]; then
        echo "VIOLATION [$label] clause(1): TenantContext.set count ($set_count) != TenantContext.clear count ($clear_count) in $scheduler" >&2
        echo "  Unbalanced set/clear leaks tenantId N into tenantId N+1's iteration on pooled scheduler threads." >&2
        echo "  Canonical pattern: set inside try, clear in finally (see #per_tenant_iteration.canonical_skeleton)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.set_without_clear" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (2): tenantCatalog.listActive() is the enumeration source ────
    # Operate on executable code (comments stripped above) so that
    # documentation mentions of tenantCatalog.listActive() do not
    # mask an actually-missing call.
    if echo "$executable_code" | grep -qE 'tenantCatalog\.listActive[[:space:]]*\(' ; then
        : # PASS — single source of tenant enumeration
    else
        echo "VIOLATION [$label] clause(2): no tenantCatalog.listActive() call in executable code of $scheduler" >&2
        echo "  Tenant enumeration MUST come from the single source (TenantCatalog#listActive)." >&2
        echo "  Hardcoded tenant lists / foreign caches / property files let deactivated tenants continue receiving job side effects." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.catalog_bypass" >&2
        violations=$((violations + 1))
    fi

    # Reject hardcoded UUID lists as a paired anti-pattern signal.
    # Multi-line tolerant: collapse the executable code to a single line
    # before scanning so `List.of(\n  UUID.fromString(...))` is detected.
    local code_flat
    code_flat=$(echo "$executable_code" | tr '\n' ' ')
    if echo "$code_flat" | grep -qE 'List\.of[[:space:]]*\([[:space:]]*UUID\.fromString'; then
        echo "VIOLATION [$label] clause(2): hardcoded UUID list detected (List.of(UUID.fromString(...))) in $scheduler" >&2
        echo "  Tenant enumeration must come from tenantCatalog.listActive(), not a literal list." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.catalog_bypass" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (3): @SchedulerLock name MUST include tenantId substring ──────
    if grep -qE '@SchedulerLock' "$scheduler"; then
        # Extract the name attribute value(s). Multi-line @SchedulerLock is
        # supported by collapsing lines from @SchedulerLock until the first ).
        local lock_block
        lock_block=$(awk '
            BEGIN { capture = 0; out = "" }
            /@SchedulerLock/ { capture = 1 }
            capture {
                out = out " " $0
                if ($0 ~ /\)/) { print out; capture = 0; out = "" }
            }
        ' "$scheduler")

        # The name attribute must contain "#tenantId" OR "${tenantId}".
        if echo "$lock_block" | grep -qE 'name[[:space:]]*=[[:space:]]*"[^"]*(#tenantId|\$\{tenantId\})[^"]*"'; then
            : # PASS — lock key is tenant-scoped
        else
            echo "VIOLATION [$label] clause(3): @SchedulerLock name does not include #tenantId (or \${tenantId}) substring in $scheduler" >&2
            echo "  Bare job-name lock serializes all tenants behind one node and turns one tenant's hang into a fleet-wide outage." >&2
            echo "  Required substring: #tenantId  (Shedlock SpEL — resolves at each iteration)" >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.lock_key_contract" >&2
            violations=$((violations + 1))
        fi
    else
        # No @SchedulerLock at all is also a violation in a multi-tenant
        # fork-receiver: distributed scheduler coordination is required to
        # prevent two cluster nodes from running the same per-tenant
        # iteration concurrently and double-billing each tenant.
        echo "VIOLATION [$label] clause(3): no @SchedulerLock annotation in $scheduler" >&2
        echo "  Distributed scheduler coordination (Shedlock / Quartz cluster) is required for multi-tenant @Scheduled methods." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.lock_key_contract" >&2
        violations=$((violations + 1))
    fi

    # ── Catalog contract sanity: TenantCatalog interface alongside ───────────
    if [ ! -f "$catalog" ]; then
        echo "VIOLATION [$label] catalog: TenantCatalog interface missing at $catalog" >&2
        echo "  Per-tenant iteration requires a non-TenantOwned TenantCatalog#listActive() bean." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.tenant_catalog_contract" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "scheduled_task_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "scheduled_task_tenant_scope_guard: PASS [$label] — all 3 clauses hold (balanced set/clear + tenantCatalog source + tenantId-scoped lock)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "scheduled_task_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# ── Default scan ────────────────────────────────────────────────────────────
# Passing fixture MUST satisfy all three clauses.
verify_scheduler "$PASS_SCHEDULER" "$PASS_CATALOG" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships a
# TenantIterationScheduler.java file MUST satisfy the policy.
LIVE_FOUND=0
while IFS= read -r dir; do
    if [ -f "$dir/TenantIterationScheduler.java" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_scheduler \
            "$dir/TenantIterationScheduler.java" \
            "$dir/TenantCatalog.java" \
            "live:$dir" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "scheduled_task_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantIterationScheduler.java (single-tenant default)"
fi

# ── --fixtures mode ─────────────────────────────────────────────────────────
if [ "$MODE" = "fixtures" ]; then
    FAIL_SCHEDULER="$FAIL_DIR/TenantIterationScheduler.java"
    if [ -f "$FAIL_SCHEDULER" ]; then
        if verify_scheduler \
                "$FAIL_SCHEDULER" \
                "$FAIL_DIR/TenantCatalog.java" \
                "failing-fixture" 2>/dev/null; then
            echo "scheduled_task_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "scheduled_task_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "scheduled_task_tenant_scope_guard: failing-fixture SKIP — $FAIL_SCHEDULER absent"
    fi
fi

exit "$OVERALL"
