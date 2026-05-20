#!/usr/bin/env bash
# webclient_async_tenant_scope_guard.sh — dogfood R13 closure (46th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# #webclient-async-tenant-scope. Multi-tenant outbound WebFlux
# WebClient adoption MUST use a single ExchangeFilterFunction that
# propagates the tenant scope across the servlet-thread → Reactor-
# scheduler-thread hop. Four detectable clauses:
#
#   (1) reactive_forward_carries_tenant_header — every file that
#       implements ExchangeFilterFunction (or constructs an
#       ExchangeFilterFunction lambda) MUST reference the
#       X-Tenant-Id header literal. Absence trips the clause —
#       the third-party / downstream service sees no tenant
#       signal on the outbound ClientRequest.
#       Detection: file contains
#       (ExchangeFilterFunction|ExchangeFilterFunctions\.ofRequestProcessor)
#       BUT NOT
#       "X-Tenant-Id"
#       → trip.
#
#   (2) reactor_context_extraction_present — every file that
#       implements ExchangeFilterFunction MUST also reference
#       Mono.deferContextual / ContextView / deferContextual.
#       Without one of these, the filter has no chain-safe
#       tenant carrier — Reactor Context API is bypassed
#       entirely and the implementation is structurally
#       unsound across scheduler hops.
#       Detection: file contains
#       (ExchangeFilterFunction)
#       BUT NOT
#       (Mono\.deferContextual|deferContextual\(|ContextView)
#       → trip.
#
#   (3) no_thread_local_in_reactive_chain — every file that
#       implements ExchangeFilterFunction MUST NOT call
#       TenantContext.current / TenantContext.set /
#       TenantContext.clear at any top-level (filter-body)
#       position. The filter runs on a Reactor scheduler
#       thread where the ThreadLocal-backed TenantContext is
#       empty in the best case OR holds a STALE prior
#       subscription's tenant in a pooled-scheduler worst
#       case → cross-tenant leak. The single permitted
#       call site is the helper method that runs on the
#       SERVLET thread (e.g. `tenantContextFor(...)`); the
#       guard detects this via a static-method context: a
#       TenantContext.current() inside a `public static`
#       method is permitted (servlet-thread helper),
#       elsewhere it trips.
#       Detection: file contains
#       (ExchangeFilterFunction)
#       AND
#       a TenantContext\.(current|set|clear)\s*\( call
#       OUTSIDE a `public static .* (UUID|Context)` helper
#       method context → trip. The guard approximates
#       this by scanning for any TenantContext.set or
#       TenantContext.clear call (these have no legitimate
#       use anywhere in this surface) and any
#       TenantContext.current call that is NOT preceded
#       within ~6 lines by `public static `.
#
#   (4) no_thread_local_inside_chain_lambda — the file MUST
#       NOT contain a TenantContext.set / TenantContext.clear
#       call AT ALL. These calls have zero legitimate use
#       inside the WebClient filter (the filter's job is to
#       READ the tenantId from Reactor Context, not to WRITE
#       a ThreadLocal). Presence of EITHER call inside a file
#       that implements ExchangeFilterFunction trips the
#       clause — the canonical leak vector for the
#       reactive-chain scheduler-worker reuse case.
#       Detection: file contains
#       (ExchangeFilterFunction)
#       AND
#       (TenantContext\.(set|clear)\s*\()
#       → trip.
#
# Algorithm:
#   1. For the passing fixture, find
#      TenantAwareWebClientFilter.java and verify all four
#      clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/`
#      package that ships a TenantAwareWebClientFilter.java
#      MUST satisfy the policy. Deployments without
#      outbound WebClient calls (no third-party SaaS
#      integration) have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side
#      variant trips the guard (all four clauses).
#
# Usage:
#   bash practices/evals/webclient_async_tenant_scope_guard.sh
#   bash practices/evals/webclient_async_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every WebClient filter file satisfies the four clauses
#   1 — at least one clause violated OR (with --fixtures) the
#       failing fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#webclient-async-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_FILE="$PASS_DIR/TenantAwareWebClientFilter.java"

# Strip block comments and line-comment tails so guard regex
# inspects executable code only (same pre-pass as
# R6/R7/R8/R9/R10/R11/R12 guards).
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

# Additionally strip double-quoted string literals — used ONLY
# for clause(3) where the canonical filter embeds an error
# message containing the literal substring "TenantContext.current()"
# as developer documentation; that string is NOT executable code
# and must not trip the ThreadLocal-in-chain detection.
strip_strings() {
    awk '
        function strip_q(s,    out, i, c, n, inq) {
            out = ""; inq = 0; n = length(s)
            for (i = 1; i <= n; i++) {
                c = substr(s, i, 1)
                if (inq) {
                    if (c == "\\" && i < n) { i++; continue }
                    if (c == "\"") inq = 0
                } else {
                    if (c == "\"") { inq = 1; continue }
                    out = out c
                }
            }
            return out
        }
        { print strip_q($0) }
    '
}

verify_filter() {
    local filter="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$filter" ]; then
        echo "VIOLATION [$label]: WebClient filter file missing: $filter" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$filter")

    # Files that don't reference ExchangeFilterFunction at all SKIP.
    # This guard's surface = files that implement (or factory) an
    # ExchangeFilterFunction. A file with no such reference is not
    # a WebClient filter and is not subject to this policy.
    if ! echo "$executable_code" | grep -qE 'ExchangeFilterFunction'; then
        echo "webclient_async_tenant_scope_guard: SKIP [$label] — no ExchangeFilterFunction reference in $filter"
        return 0
    fi

    # ── Clause (1): reactive_forward_carries_tenant_header ───────────
    # File MUST reference the X-Tenant-Id header literal.
    if ! echo "$executable_code" | grep -q 'X-Tenant-Id'; then
        echo "VIOLATION [$label] clause(1): WebClient filter does NOT set X-Tenant-Id header on outbound ClientRequest in $filter" >&2
        echo "  Third-party services see no tenant attribution; downstream tenant-aware services see empty TenantContext on the receiving servlet thread." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.tenant_header_dropped_on_reactive_forward" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (2): reactor_context_extraction_present ───────────────
    # File MUST reference Mono.deferContextual / ContextView /
    # deferContextual. Without one of these, the filter has no
    # chain-safe tenant carrier.
    if ! echo "$executable_code" | grep -qE '(Mono\.deferContextual|deferContextual[[:space:]]*\(|ContextView)'; then
        echo "VIOLATION [$label] clause(2): WebClient filter does NOT use Mono.deferContextual / ContextView in $filter" >&2
        echo "  Reactor Context API is the only chain-safe tenant carrier; a filter that bypasses Context cannot be tenant-scoped across scheduler hops." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.reactor_context_missing" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (3): no_thread_local_in_reactive_chain ────────────────
    # File MUST NOT call TenantContext.current() outside a
    # `public static` helper method (which runs on the servlet
    # thread). The guard implements this as a two-pass scan:
    # if a TenantContext.current() call exists anywhere in the
    # file, the immediately-preceding ~6 lines (within the same
    # method) MUST contain `public static ` (the helper-method
    # signature). Otherwise — current() called from instance
    # method (= filter body OR a chain lambda) → trip.
    #
    # For this clause ONLY, additionally strip string literals
    # so error-message strings (which legitimately quote the
    # method name "TenantContext.current()" as documentation)
    # do not false-positive.
    local clause3_code
    clause3_code=$(echo "$executable_code" | strip_strings)
    if echo "$clause3_code" | grep -qE 'TenantContext\.current[[:space:]]*\('; then
        # Find every line with TenantContext.current(...) and
        # check the preceding 6 lines for `public static `.
        # If any current() call has no `public static ` within
        # the preceding 6 lines, trip the clause.
        local current_violation=0
        local nlines
        nlines=$(echo "$clause3_code" | wc -l)
        local i=1
        local found_current=0
        while [ "$i" -le "$nlines" ]; do
            local this_line
            this_line=$(echo "$clause3_code" | sed -n "${i}p")
            if echo "$this_line" | grep -qE 'TenantContext\.current[[:space:]]*\('; then
                found_current=1
                # Check the preceding 6 lines (inclusive of $i)
                # for `public static `.
                local start=$((i - 6))
                [ "$start" -lt 1 ] && start=1
                local window
                window=$(echo "$clause3_code" | sed -n "${start},${i}p")
                if ! echo "$window" | grep -qE 'public[[:space:]]+static[[:space:]]+'; then
                    current_violation=1
                fi
            fi
            i=$((i + 1))
        done
        if [ "$current_violation" -eq 1 ]; then
            echo "VIOLATION [$label] clause(3): WebClient filter calls TenantContext.current() OUTSIDE a 'public static' servlet-thread helper in $filter" >&2
            echo "  The filter runs on a Reactor scheduler thread where the ThreadLocal-backed TenantContext is empty in the best case OR holds a STALE prior subscription's tenant in a pooled-scheduler worst case → cross-tenant leak." >&2
            echo "  Use Mono.deferContextual(ctx -> ...) to extract the tenantId from Reactor Context. The only permitted TenantContext.current() call site is the controller-side static helper (e.g. tenantContextFor(UUID)) that runs on the servlet thread." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.thread_local_in_reactive_chain" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (4): no_thread_local_inside_chain_lambda ──────────────
    # File MUST NOT contain TenantContext.set / TenantContext.clear
    # calls AT ALL. These have zero legitimate use in this surface;
    # the filter's job is to READ Reactor Context, NEVER write a
    # ThreadLocal. Their presence is the canonical scheduler-worker
    # reuse leak vector. String-literal-stripped (clause3_code) so
    # documentation strings naming the methods do not false-positive.
    if echo "$clause3_code" | grep -qE 'TenantContext\.(set|clear)[[:space:]]*\('; then
        local bad_call
        bad_call=$(echo "$clause3_code" | grep -E 'TenantContext\.(set|clear)[[:space:]]*\(' | head -1 | sed 's/^[[:space:]]*//')
        echo "VIOLATION [$label] clause(4): WebClient filter calls TenantContext.set / TenantContext.clear in $filter" >&2
        echo "  Offending call: $bad_call" >&2
        echo "  Reactor scheduler workers are reused across subscriptions; TenantContext.set inside a chain lambda (Mono.flatMap / Mono.map / Mono.doOnNext / Mono.subscribe) leaks the tenantId into the worker's ThreadLocal for the NEXT subscription on the same worker → canonical cross-tenant leak." >&2
        echo "  Reactor Context is the only chain-safe tenant carrier; never call TenantContext.set or TenantContext.clear inside the WebClient filter." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.thread_local_inside_chain_lambda" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "webclient_async_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "webclient_async_tenant_scope_guard: PASS [$label] — all 4 clauses hold (X-Tenant-Id header + Mono.deferContextual extraction + zero ThreadLocal access in reactive chain + zero TenantContext.set/clear)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "webclient_async_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all four clauses.
verify_filter "$PASS_FILE" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that
# ships a TenantAwareWebClientFilter.java MUST satisfy the policy.
# Deployments without outbound WebFlux WebClient surface have zero
# such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    candidate="$dir/TenantAwareWebClientFilter.java"
    if [ -f "$candidate" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_filter "$candidate" "live:$candidate" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "webclient_async_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareWebClientFilter.java (WebClient-free default; deployments without external SaaS API calls never adopt this surface)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_FILE="$FAIL_DIR/TenantAwareWebClientFilter.java"
    if [ -f "$FAIL_FILE" ]; then
        if verify_filter "$FAIL_FILE" "failing-fixture" 2>/dev/null; then
            echo "webclient_async_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "webclient_async_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "webclient_async_tenant_scope_guard: failing-fixture SKIP — $FAIL_FILE absent"
    fi
fi

exit "$OVERALL"
