#!/usr/bin/env bash
# broker_fanout_tenant_scope_guard.sh — dogfood R8 GAP-NEW-2 closure (41st hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#broker-fanout-tenant-scope`. Cross-node broker fan-out bridges
# (Redis Pub/Sub, Kafka, RabbitMQ topic exchange) used to scale SSE /
# WebSocket realtime push horizontally beyond a single node MUST adopt
# the envelope-header + per-message set/clear pattern with four
# load-bearing clauses:
#
#   (1) Publish-side wraps the payload in a TenantBrokerEnvelope
#       (or equivalent Kafka producer record with X-Tenant-Id header)
#       BEFORE handing to the broker template. The guard scans any
#       file whose name matches *RedisPubSubBridge.java /
#       *KafkaFanoutBridge.java / *BrokerBridge.java and forbids
#       .convertAndSend( / .send( / .publish( calls that are NOT
#       preceded by a TenantBrokerEnvelope constructor invocation
#       (or a Kafka header set, recognized by .headers().add(
#       "X-Tenant-Id" / TENANT_HEADER substring) in the SAME file.
#       See #failure_modes.publish_without_envelope_header.
#
#   (2) Subscribe-side listener reads tenantId from the envelope /
#       Kafka header, NOT from TenantContext.current(). The broker
#       thread has empty TenantContext by construction. The guard
#       requires that any onMessage / @KafkaListener-annotated
#       method body MUST have an envelope-shaped read (substring
#       .tenantId() / .getTenantId() / lastHeader("X-Tenant-Id")
#       / lastHeader(TENANT_HEADER)) BEFORE any
#       TenantContext.current() call. If TenantContext.current()
#       appears before any envelope read, trip the clause. See
#       #failure_modes.consumer_assumes_current_context.
#
#   (3) Per-message TenantContext.set + TenantContext.clear wraps
#       each dispatch in try/finally. count(set) == count(clear) > 0
#       in any file with a .convertAndSend( / .send( / .publish( /
#       onMessage / @KafkaListener signature. Same algorithm as the
#       R6 39th-guard clause-1 and the R7 40th-guard clause-3. See
#       #failure_modes.stale_tenant_context.
#
#   (4) Local dispatch passes envelope.tenantId() EXPLICITLY into
#       sendToTenant — NEVER TenantContext.current() and NEVER a
#       bridge-bean tenantId field. The guard scans .sendToTenant(
#       calls and rejects any with TenantContext.current() as the
#       first argument. See #failure_modes.consumer_broadcast_no_filter.
#
# Algorithm:
#   1. For the passing fixture, find TenantAwareRedisPubSubBridge.java
#      and verify all four clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package that
#      ships a TenantAwareRedisPubSubBridge.java or any file matching
#      the broker-bridge naming convention MUST satisfy the policy.
#      Single-node deployments have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side variant
#      (publish_without_envelope_header + consumer_assumes_current_context
#      + stale_tenant_context + consumer_broadcast_no_filter) trips
#      the guard.
#
# Usage:
#   bash practices/evals/broker_fanout_tenant_scope_guard.sh
#   bash practices/evals/broker_fanout_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every bridge file satisfies the four clauses
#   1 — at least one clause violated OR (with --fixtures) the failing/
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#broker-fanout-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_BRIDGE="$PASS_DIR/TenantAwareRedisPubSubBridge.java"

# Strip block comments and line-comment tails so guard regex inspects
# executable code only (same pre-pass as the R6 39th + R7 40th guards).
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

verify_bridge() {
    local bridge="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$bridge" ]; then
        echo "VIOLATION [$label]: bridge file missing: $bridge" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$bridge")

    # ── Clause (1): publish wraps payload in TenantBrokerEnvelope ───────────
    # If the file contains .convertAndSend(  / .publish(  WITHOUT a
    # TenantBrokerEnvelope constructor call AND WITHOUT a Kafka header
    # mechanism (.headers().add("X-Tenant-Id" or TENANT_HEADER substring),
    # trip the clause.
    if echo "$executable_code" | grep -qE '\.(convertAndSend|publish)[[:space:]]*\('; then
        local has_envelope=0
        local has_kafka_header=0
        if echo "$executable_code" | grep -qE 'new[[:space:]]+TenantBrokerEnvelope[[:space:]]*\('; then
            has_envelope=1
        fi
        if echo "$executable_code" | grep -qE '\.headers\(\)\.add\([^)]*("X-Tenant-Id"|TENANT_HEADER)'; then
            has_kafka_header=1
        fi
        if [ "$has_envelope" -eq 0 ] && [ "$has_kafka_header" -eq 0 ]; then
            echo "VIOLATION [$label] clause(1): publish/convertAndSend without TenantBrokerEnvelope or Kafka X-Tenant-Id header in $bridge" >&2
            echo "  Cross-node broker hop MUST carry tenantId as a structural envelope header." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.publish_without_envelope_header" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (2): subscribe reads envelope BEFORE TenantContext.current() ─
    # The check is SCOPED to the onMessage / @KafkaListener method body —
    # publisher methods are allowed (and required, by clause 1's spirit) to
    # call TenantContext.current() to source the tenantId for the envelope.
    # Algorithm:
    #   1. Extract the onMessage / @KafkaListener-annotated method body
    #      using a brace-balanced single-pass extractor.
    #   2. Within that body, the FIRST envelope-shaped read MUST precede
    #      the FIRST TenantContext.current() call.
    # Files with no onMessage/listener signature SKIP clause(2).
    if echo "$executable_code" | grep -qE '(@KafkaListener|public[[:space:]]+void[[:space:]]+onMessage[[:space:]]*\()'; then
        local listener_body
        listener_body=$(echo "$executable_code" | awk '
            BEGIN { capturing = 0; depth = 0; buf = "" }
            {
                line = $0
                if (!capturing) {
                    if (line ~ /@KafkaListener/ || line ~ /public[ \t]+void[ \t]+onMessage[ \t]*\(/) {
                        capturing = 1
                        # Start capturing immediately; the opening brace
                        # may be on this line or the next.
                    }
                }
                if (capturing) {
                    buf = buf line "\n"
                    n = length(line)
                    for (i = 1; i <= n; i++) {
                        c = substr(line, i, 1)
                        if (c == "{") depth++
                        else if (c == "}") {
                            depth--
                            if (depth == 0) { capturing = 0; print buf; buf = ""; next }
                        }
                    }
                }
            }
            END { if (buf != "") print buf }
        ')

        local envelope_line current_line
        envelope_line=$(echo "$listener_body" | grep -nE '(\.tenantId\(\)|\.getTenantId\(\)|lastHeader[[:space:]]*\([^)]*("X-Tenant-Id"|TENANT_HEADER))' | head -1 | cut -d: -f1 || true)
        current_line=$(echo "$listener_body" | grep -nE 'TenantContext\.current[[:space:]]*\(' | head -1 | cut -d: -f1 || true)

        if [ -z "$envelope_line" ] && [ -n "$current_line" ]; then
            echo "VIOLATION [$label] clause(2): listener reads TenantContext.current() with NO envelope read in $bridge" >&2
            echo "  Broker-client thread has empty TenantContext by construction." >&2
            echo "  Tenant signal MUST come from the deserialized envelope / Kafka X-Tenant-Id header." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.consumer_assumes_current_context" >&2
            violations=$((violations + 1))
        elif [ -n "$envelope_line" ] && [ -n "$current_line" ] && [ "$envelope_line" -gt "$current_line" ]; then
            echo "VIOLATION [$label] clause(2): listener calls TenantContext.current() at line $current_line (in listener body) BEFORE envelope read at line $envelope_line in $bridge" >&2
            echo "  Envelope read MUST happen first; ambient context is empty until TenantContext.set(envelope.tenantId())." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.consumer_assumes_current_context" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (3): per-message TenantContext.set + clear (count equality) ──
    if echo "$executable_code" | grep -qE '(\.(convertAndSend|publish|send)[[:space:]]*\(|onMessage|@KafkaListener)'; then
        local set_count clear_count
        set_count=$(echo "$executable_code" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)
        clear_count=$(echo "$executable_code" | grep -cE 'TenantContext\.clear[[:space:]]*\(' || true)
        # A listener-bearing file MUST set + clear at least once.
        if echo "$executable_code" | grep -qE '(@KafkaListener|public[[:space:]]+void[[:space:]]+onMessage[[:space:]]*\()'; then
            if [ "$set_count" -eq 0 ] || [ "$clear_count" -eq 0 ]; then
                echo "VIOLATION [$label] clause(3): listener missing TenantContext.set / clear in $bridge (set=$set_count, clear=$clear_count)" >&2
                echo "  Each listener invocation MUST be wrapped: try { TenantContext.set(envelope.tenantId()); dispatch(...); } finally { TenantContext.clear(); }" >&2
                echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.stale_tenant_context" >&2
                violations=$((violations + 1))
            elif [ "$set_count" -ne "$clear_count" ]; then
                echo "VIOLATION [$label] clause(3): TenantContext.set count ($set_count) != TenantContext.clear count ($clear_count) in $bridge" >&2
                echo "  Unbalanced set/clear leaks tenantId N into tenantId N+1's onMessage on the reused broker-pool thread." >&2
                echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.stale_tenant_context" >&2
                violations=$((violations + 1))
            fi
        fi
    fi

    # ── Clause (4): sendToTenant receives envelope.tenantId() explicitly ────
    # If any sendToTenant( call has TenantContext.current() as the
    # FIRST argument, trip the clause. The argument MUST come from
    # the envelope / a Kafka header — never from ambient context.
    if echo "$executable_code" | grep -qE '\.sendToTenant[[:space:]]*\([[:space:]]*TenantContext\.current[[:space:]]*\('; then
        echo "VIOLATION [$label] clause(4): .sendToTenant(TenantContext.current()...) in $bridge" >&2
        echo "  Dispatch tenantId MUST be the envelope's tenantId, NOT ambient TenantContext.current()." >&2
        echo "  Passing TenantContext.current() couples dispatch to a context that may be wrong (set from a previous message) or empty (broker thread before set)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.consumer_broadcast_no_filter" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "broker_fanout_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "broker_fanout_tenant_scope_guard: PASS [$label] — all 4 clauses hold (envelope publish + envelope-first listener + balanced per-message set/clear + explicit envelope.tenantId() dispatch)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "broker_fanout_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all four clauses.
verify_bridge "$PASS_BRIDGE" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships
# a TenantAwareRedisPubSubBridge.java / TenantAwareKafkaFanoutBridge.java /
# *BrokerBridge.java MUST satisfy the policy.
LIVE_FOUND=0
while IFS= read -r dir; do
    for candidate in \
            "$dir/TenantAwareRedisPubSubBridge.java" \
            "$dir/TenantAwareKafkaFanoutBridge.java"; do
        if [ -f "$candidate" ]; then
            LIVE_FOUND=$((LIVE_FOUND + 1))
            verify_bridge "$candidate" "live:$candidate" || OVERALL=1
        fi
    done
    # Catch-all for *BrokerBridge.java naming variant.
    while IFS= read -r catchall; do
        case "$catchall" in
            *TenantAwareRedisPubSubBridge.java|*TenantAwareKafkaFanoutBridge.java) ;;
            *)
                LIVE_FOUND=$((LIVE_FOUND + 1))
                verify_bridge "$catchall" "live:$catchall" || OVERALL=1
                ;;
        esac
    done < <(find "$dir" -maxdepth 1 -type f -name '*BrokerBridge.java' 2>/dev/null)
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "broker_fanout_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAware*Bridge.java (single-node default)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_BRIDGE="$FAIL_DIR/TenantAwareRedisPubSubBridge.java"
    if [ -f "$FAIL_BRIDGE" ]; then
        if verify_bridge "$FAIL_BRIDGE" "failing-fixture" 2>/dev/null; then
            echo "broker_fanout_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "broker_fanout_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "broker_fanout_tenant_scope_guard: failing-fixture SKIP — $FAIL_BRIDGE absent"
    fi
fi

exit "$OVERALL"
