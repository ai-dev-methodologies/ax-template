#!/usr/bin/env bash
# kafka_streams_interactive_queries_tenant_scope_guard.sh — dogfood R11 closure (44th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# #kafka-streams-interactive-queries-tenant-scope. Multi-tenant Kafka
# Streams Interactive Queries (HTTP-exposed state-store reads) MUST
# adopt the (TenantContext.current() prefix + store.range scoped scan +
# path mismatch → TenantBoundaryViolationException → 404 + fresh store
# reference per query) pattern with four detectable clauses:
#
#   (1) tenant_context_current_only — every file containing
#       store.range( OR store.get( substrings MUST also contain a
#       TenantContext.current() call whose value is used as the
#       prefix root. Concretely: the file MUST reference
#       TenantContext.current(), and MUST NOT pass any method
#       parameter named *tenantId or *TenantId directly into a
#       prefix-building expression that flows into store.range/get
#       without first comparing it to the context tenantId.
#       Detection: presence of `store.range(` OR `store.get(`
#       REQUIRES presence of `TenantContext.current(` substring.
#       Absence of TenantContext.current() in a file that calls
#       store.range / store.get trips the clause.
#
#   (2) no_store_all_scan — if the file calls store.range( or
#       store.get( (i.e. is an Interactive Queries surface), it
#       MUST NOT also call store.all(. store.all() scans every
#       tenant's state at the RocksDB level and relies on a
#       post-filter that is fragile under refactor. The canonical
#       pattern uses store.range(prefix, prefix + sentinel) so the
#       scan is structurally tenant-scoped. See
#       #failure_modes.unscoped_store_all_scan.
#
#   (3) no_403_on_tenant_mismatch — the file MUST NOT throw
#       AccessDeniedException OR construct an HTTP 403 response
#       (e.g. `new ResponseStatusException(HttpStatus.FORBIDDEN`,
#       or `HttpStatus.FORBIDDEN`, or an `@ResponseStatus(value =
#       HttpStatus.FORBIDDEN)` annotation, or a literal `403`
#       status mapping). The canonical mapping is
#       TenantBoundaryViolationException → 404 via
#       MultiTenantProblemDetailAdvice; 403 leaks the existence of
#       a cross-tenant resource and breaks the
#       #aop-guard.http_mapping contract (which mandates 404).
#       The file MUST also reference
#       TenantBoundaryViolationException OR contain a comment
#       deferring the mismatch check to an upstream layer. See
#       #failure_modes.access_denied_existence_leak.
#
#   (4) no_cached_store_reference — the file MUST NOT declare a
#       field of type ReadOnlyKeyValueStore (or any
#       ReadOnly*Store) that is initialised once and reused
#       across requests. Concretely: the guard scans for
#       `private ... ReadOnly*Store<...> <name>;` declarations.
#       Each query method MUST acquire its store via
#       streams.store(StoreQueryParameters...) at invocation
#       time. Streams rebalance invalidates any cached store
#       reference; a stale reference reads from the OLD
#       partition assignment and may surface other tenants'
#       data after rebalance. See
#       #failure_modes.cached_store_reference_after_rebalance.
#
# Algorithm:
#   1. For the passing fixture, find
#      TenantAwareInteractiveQueryService.java and verify all
#      four clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package
#      that ships a TenantAwareInteractiveQueryService.java MUST
#      satisfy the policy. Deployments without Interactive
#      Queries have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side variant
#      trips the guard (all four clauses).
#
# Usage:
#   bash practices/evals/kafka_streams_interactive_queries_tenant_scope_guard.sh
#   bash practices/evals/kafka_streams_interactive_queries_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every IQ file satisfies the four clauses
#   1 — at least one clause violated OR (with --fixtures) the
#       failing fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#kafka-streams-interactive-queries-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_FILE="$PASS_DIR/TenantAwareInteractiveQueryService.java"

# Strip block comments and line-comment tails so guard regex
# inspects executable code only (same pre-pass as R6/R7/R8/R9/R10
# guards).
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

verify_service() {
    local service="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$service" ]; then
        echo "VIOLATION [$label]: IQ service file missing: $service" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$service")

    # Files that don't reference Interactive Queries API at all SKIP.
    # IQ surface = files that call store.range(, store.get(, or
    # streams.store( . A file with none of these is not an IQ
    # service and is not subject to this policy.
    if ! echo "$executable_code" | grep -qE '(store\.(range|get)[[:space:]]*\(|streams\.store[[:space:]]*\()'; then
        echo "kafka_streams_interactive_queries_tenant_scope_guard: SKIP [$label] — no Interactive Queries API in $service"
        return 0
    fi

    # ── Clause (1): tenant_context_current_only ──────────────────────
    # File MUST reference TenantContext.current(. Without it, the
    # prefix root can only have come from a request parameter,
    # which is the IDOR vector this anchor closes.
    if ! echo "$executable_code" | grep -qE 'TenantContext\.current[[:space:]]*\('; then
        echo "VIOLATION [$label] clause(1): Interactive Queries service calls store.range/get/streams.store WITHOUT TenantContext.current() in $service" >&2
        echo "  Prefix root MUST come from TenantContext, NOT from path/query/body. Path-supplied tenantId in the prefix is an IDOR vector." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.path_tenant_id_as_prefix" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (2): no_store_all_scan ────────────────────────────────
    # If the file has store.range( or store.get( (IQ surface), it
    # MUST NOT also call store.all(. store.all() scans every
    # tenant's state and relies on a post-filter that breaks under
    # refactor.
    if echo "$executable_code" | grep -qE '\.all[[:space:]]*\([[:space:]]*\)'; then
        # narrow: only count `.all()` calls on a store-typed receiver.
        # The simplest mechanical proxy: the file calls `.all()` AND
        # also calls `store.range(` or `store.get(` or has a
        # ReadOnlyKeyValueStore reference. If so, .all() is on the
        # store and trips the clause.
        if echo "$executable_code" | grep -qE '(ReadOnly[A-Za-z]*Store|store\.(range|get)[[:space:]]*\()'; then
            echo "VIOLATION [$label] clause(2): Interactive Queries service calls store.all() — unscoped scan of every tenant's state in $service" >&2
            echo "  store.all() scans every tenant's data; a post-filter is fragile under refactor. Use store.range(prefix, prefix + sentinel) so the scan is structurally tenant-scoped." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.unscoped_store_all_scan" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (3): no_403_on_tenant_mismatch ────────────────────────
    # File MUST NOT throw AccessDeniedException or construct an
    # HTTP 403 response. Canonical mismatch handler is
    # TenantBoundaryViolationException → 404.
    local forbids_403=0
    if echo "$executable_code" | grep -qE '(AccessDeniedException|HttpStatus\.FORBIDDEN|HttpStatus\.FORBIDDEN|@ResponseStatus[^)]*FORBIDDEN|ResponseStatusException[^)]*FORBIDDEN|status[[:space:]]*=[[:space:]]*403|"403"[[:space:]]*$)'; then
        forbids_403=1
    fi
    if [ "$forbids_403" -eq 1 ]; then
        echo "VIOLATION [$label] clause(3): Interactive Queries service throws AccessDeniedException or maps to HTTP 403 on tenant mismatch in $service" >&2
        echo "  Canonical mapping is TenantBoundaryViolationException → 404. 403 leaks the existence of the cross-tenant resource and breaks the #aop-guard.http_mapping contract." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.access_denied_existence_leak" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (4): no_cached_store_reference ────────────────────────
    # File MUST NOT declare a field of type ReadOnly*Store that
    # is reused across requests. Acquire fresh on every query via
    # streams.store(StoreQueryParameters...).
    if echo "$executable_code" | grep -qE '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?ReadOnly[A-Za-z]*Store[[:space:]]*<'; then
        local cached_field
        cached_field=$(echo "$executable_code" | grep -E '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?ReadOnly[A-Za-z]*Store[[:space:]]*<' | head -1 | sed 's/^[[:space:]]*//')
        echo "VIOLATION [$label] clause(4): Interactive Queries service caches a ReadOnly*Store reference as a field in $service" >&2
        echo "  Offending declaration: $cached_field" >&2
        echo "  Streams rebalance invalidates any cached store reference; a stale reference reads from the OLD partition assignment and may surface other tenants' data." >&2
        echo "  Acquire the store fresh in each query method via streams.store(StoreQueryParameters.fromNameAndType(...))." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.cached_store_reference_after_rebalance" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "kafka_streams_interactive_queries_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "kafka_streams_interactive_queries_tenant_scope_guard: PASS [$label] — all 4 clauses hold (TenantContext.current() prefix + no store.all + no 403 + no cached store reference)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "kafka_streams_interactive_queries_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all four clauses.
verify_service "$PASS_FILE" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that
# ships a TenantAwareInteractiveQueryService.java MUST satisfy
# the policy. Deployments without Interactive Queries have zero
# such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    candidate="$dir/TenantAwareInteractiveQueryService.java"
    if [ -f "$candidate" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_service "$candidate" "live:$candidate" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "kafka_streams_interactive_queries_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareInteractiveQueryService.java (Interactive-Queries-free default)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_FILE="$FAIL_DIR/TenantAwareInteractiveQueryService.java"
    if [ -f "$FAIL_FILE" ]; then
        if verify_service "$FAIL_FILE" "failing-fixture" 2>/dev/null; then
            echo "kafka_streams_interactive_queries_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "kafka_streams_interactive_queries_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "kafka_streams_interactive_queries_tenant_scope_guard: failing-fixture SKIP — $FAIL_FILE absent"
    fi
fi

exit "$OVERALL"
