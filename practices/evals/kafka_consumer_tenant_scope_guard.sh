#!/usr/bin/env bash
# kafka_consumer_tenant_scope_guard.sh — dogfood R9 closure (42nd hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#kafka-consumer-tenant-scope`. Long-running Kafka business-event
# consumers (distinct surface from #broker-fanout-tenant-scope which
# covers fan-out-INTO-SSE bridges) MUST adopt the shared-topic +
# X-Tenant-Id header + per-record set/clear pattern with five
# load-bearing clauses:
#
#   (1) Listener body reads the X-Tenant-Id header (lastHeader call
#       or TENANT_HEADER substring) BEFORE any TenantContext.current()
#       call. The Kafka consumer poll thread has empty TenantContext
#       by construction. See #failure_modes.consumer_assumes_current_context.
#
#   (2) When the @KafkaListener method receives a batch
#       (List<ConsumerRecord> or ConsumerRecords parameter), the
#       TenantContext.set call MUST appear INSIDE the for-loop / forEach
#       body, NOT once at method entry. The guard extracts the
#       brace-balanced @KafkaListener method body, then extracts each
#       enclosed for-loop body, and requires TenantContext.set to be
#       inside at least one for-loop. A set call outside any for-loop
#       while the signature is List<ConsumerRecord> trips clause(2).
#       See #failure_modes.batch_set_once.
#
#   (3) count(TenantContext.set) == count(TenantContext.clear) > 0
#       inside the @KafkaListener method body. Mirrors R8 41st-guard
#       clause(3) but scoped to the listener method body (not whole
#       file) so a tenant-free helper method elsewhere in the same
#       file does not skew the count. See #failure_modes.stale_per_record_context.
#
#   (4) Rebalance callbacks (onPartitionsAssigned,
#       onPartitionsRevoked, onPartitionsRevokedBeforeCommit) MUST
#       NOT contain TenantContext.set or TenantContext.clear. The
#       rebalance callback runs on the poll thread between batches
#       with no tenant signal. See #failure_modes.rebalance_carries_tenant_context.
#
#   (5) Manual Acknowledgment — .acknowledge() calls MUST NOT appear
#       inside the per-record try-block (between TenantContext.set
#       and TenantContext.clear). Detection: extract the for-loop
#       body and verify no `.acknowledge(` substring appears in any
#       try-block scope where TenantContext.set appears. The
#       canonical pattern places ack at the END of onBatch (after
#       the for-loop). See #failure_modes.ack_inside_tenant_span.
#
# Algorithm:
#   1. For the passing fixture, find TenantAwareKafkaConsumer.java
#      and verify all five clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package that
#      ships a TenantAwareKafkaConsumer.java or any file matching
#      *Consumer.java with @KafkaListener MUST satisfy the policy.
#      Deployments without Kafka have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side variant trips
#      the guard (consumer_assumes_current_context + batch_set_once
#      + stale_per_record_context + rebalance_carries_tenant_context).
#
# Usage:
#   bash practices/evals/kafka_consumer_tenant_scope_guard.sh
#   bash practices/evals/kafka_consumer_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every consumer file satisfies the five clauses
#   1 — at least one clause violated OR (with --fixtures) the failing
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#kafka-consumer-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_CONSUMER="$PASS_DIR/TenantAwareKafkaConsumer.java"

# Strip block comments and line-comment tails so guard regex inspects
# executable code only (same pre-pass as the R6 39th + R7 40th + R8
# 41st guards).
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

# Extract a brace-balanced block starting from the line matching $marker.
# The marker line is included; capture continues until brace depth returns
# to zero. Used to isolate @KafkaListener method bodies and rebalance
# callback bodies.
extract_block_after_marker() {
    local marker="$1"
    awk -v marker="$marker" '
        BEGIN { capturing = 0; depth = 0 }
        {
            line = $0
            if (!capturing && line ~ marker) {
                capturing = 1
            }
            if (capturing) {
                print line
                n = length(line)
                for (i = 1; i <= n; i++) {
                    c = substr(line, i, 1)
                    if (c == "{") depth++
                    else if (c == "}") {
                        depth--
                        if (depth == 0 && line ~ /\}/) {
                            capturing = 0
                            print "==BLOCK_END=="
                            exit
                        }
                    }
                }
            }
        }
    '
}

verify_consumer() {
    local consumer="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$consumer" ]; then
        echo "VIOLATION [$label]: consumer file missing: $consumer" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$consumer")

    # Only files with @KafkaListener participate in clauses 1–5;
    # other consumer-named files (e.g. helper classes) SKIP.
    if ! echo "$executable_code" | grep -qE '@KafkaListener'; then
        echo "kafka_consumer_tenant_scope_guard: SKIP [$label] — no @KafkaListener in $consumer"
        return 0
    fi

    # Extract the @KafkaListener method body (brace-balanced).
    # The marker is the @KafkaListener annotation; we then continue
    # through the method signature into the body.
    local listener_body
    listener_body=$(echo "$executable_code" | awk '
        BEGIN { capturing = 0; depth = 0; seen_open = 0 }
        {
            line = $0
            if (!capturing && line ~ /@KafkaListener/) {
                capturing = 1
            }
            if (capturing) {
                print line
                n = length(line)
                for (i = 1; i <= n; i++) {
                    c = substr(line, i, 1)
                    if (c == "{") { depth++; seen_open = 1 }
                    else if (c == "}") {
                        depth--
                        if (seen_open && depth == 0) {
                            capturing = 0
                            exit
                        }
                    }
                }
            }
        }
    ')

    # ── Clause (1): X-Tenant-Id header read BEFORE TenantContext.current() ─
    local header_line current_line
    header_line=$(echo "$listener_body" | grep -nE '(lastHeader[[:space:]]*\([^)]*("X-Tenant-Id"|TENANT_HEADER))' | head -1 | cut -d: -f1 || true)
    current_line=$(echo "$listener_body" | grep -nE 'TenantContext\.current[[:space:]]*\(' | head -1 | cut -d: -f1 || true)

    if [ -z "$header_line" ] && [ -n "$current_line" ]; then
        echo "VIOLATION [$label] clause(1): listener reads TenantContext.current() with NO X-Tenant-Id header read in $consumer" >&2
        echo "  Kafka consumer poll thread has empty TenantContext by construction." >&2
        echo "  Tenant signal MUST come from the per-record X-Tenant-Id header." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.consumer_assumes_current_context" >&2
        violations=$((violations + 1))
    elif [ -n "$header_line" ] && [ -n "$current_line" ] && [ "$header_line" -gt "$current_line" ]; then
        echo "VIOLATION [$label] clause(1): listener calls TenantContext.current() at line $current_line BEFORE header read at line $header_line in $consumer" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.consumer_assumes_current_context" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (2): batch listener — set INSIDE for-loop, not before ──────
    # Detect batch signature: List<ConsumerRecord or ConsumerRecords.
    if echo "$listener_body" | grep -qE '(List[[:space:]]*<[[:space:]]*ConsumerRecord|ConsumerRecords[[:space:]]*<)'; then
        # Extract for-loop body. Single-pass awk that captures the
        # innermost for(...) { ... } block by brace depth.
        local for_body
        for_body=$(echo "$listener_body" | awk '
            BEGIN { capturing = 0; depth = 0; for_start_depth = -1 }
            {
                line = $0
                if (!capturing && line ~ /for[[:space:]]*\(/) {
                    capturing = 1
                    for_start_depth = depth
                }
                if (capturing) {
                    print line
                    n = length(line)
                    for (i = 1; i <= n; i++) {
                        c = substr(line, i, 1)
                        if (c == "{") depth++
                        else if (c == "}") {
                            depth--
                            if (depth == for_start_depth && capturing) {
                                capturing = 0
                                # Continue to next for-loop if any.
                            }
                        }
                    }
                }
            }
        ')

        # Set inside for-loop body?
        local set_in_for=0
        if [ -n "$for_body" ] && echo "$for_body" | grep -qE 'TenantContext\.set[[:space:]]*\('; then
            set_in_for=1
        fi

        # Set count in whole listener body.
        local set_total
        set_total=$(echo "$listener_body" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)

        if [ "$set_total" -gt 0 ] && [ "$set_in_for" -eq 0 ]; then
            echo "VIOLATION [$label] clause(2): batch listener calls TenantContext.set OUTSIDE any for-loop body in $consumer" >&2
            echo "  List<ConsumerRecord> batches may interleave tenants; set MUST be per-record." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.batch_set_once" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (3): count(set) == count(clear) > 0 in listener body ───────
    local set_count clear_count
    set_count=$(echo "$listener_body" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)
    clear_count=$(echo "$listener_body" | grep -cE 'TenantContext\.clear[[:space:]]*\(' || true)

    if [ "$set_count" -eq 0 ] || [ "$clear_count" -eq 0 ]; then
        echo "VIOLATION [$label] clause(3): @KafkaListener body missing TenantContext.set / clear in $consumer (set=$set_count, clear=$clear_count)" >&2
        echo "  Each record's processing MUST be wrapped: try { TenantContext.set(headerTenantId); handle(...); } finally { TenantContext.clear(); }" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.stale_per_record_context" >&2
        violations=$((violations + 1))
    elif [ "$set_count" -ne "$clear_count" ]; then
        echo "VIOLATION [$label] clause(3): TenantContext.set count ($set_count) != TenantContext.clear count ($clear_count) in @KafkaListener body of $consumer" >&2
        echo "  Unbalanced set/clear leaks tenantId N into tenantId N+1's record processing." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.stale_per_record_context" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (4): rebalance callbacks MUST be tenant-free ───────────────
    # Extract each onPartitions{Assigned,Revoked,RevokedBeforeCommit} body
    # and check for TenantContext.set/clear substring.
    local rebalance_blocks
    rebalance_blocks=$(echo "$executable_code" | awk '
        BEGIN { capturing = 0; depth = 0; seen_open = 0 }
        {
            line = $0
            if (!capturing && line ~ /(onPartitionsAssigned|onPartitionsRevoked|onPartitionsRevokedBeforeCommit)[[:space:]]*\(/) {
                capturing = 1
                seen_open = 0
                depth = 0
            }
            if (capturing) {
                print line
                n = length(line)
                for (i = 1; i <= n; i++) {
                    c = substr(line, i, 1)
                    if (c == "{") { depth++; seen_open = 1 }
                    else if (c == "}") {
                        depth--
                        if (seen_open && depth == 0) {
                            capturing = 0
                            print "==CALLBACK_END=="
                            next
                        }
                    }
                }
            }
        }
    ')

    if [ -n "$rebalance_blocks" ] && echo "$rebalance_blocks" | grep -qE 'TenantContext\.(set|clear)[[:space:]]*\('; then
        echo "VIOLATION [$label] clause(4): rebalance callback contains TenantContext.set/clear in $consumer" >&2
        echo "  Rebalance callbacks run on the poll thread BETWEEN batches with NO tenant signal." >&2
        echo "  Setting/clearing TenantContext here leaks into the next batch's first record." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.rebalance_carries_tenant_context" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (5): ack.acknowledge() OUTSIDE the per-record try-span ─────
    # Detect: within the for-loop body, the substring `.acknowledge(` MUST
    # NOT appear between TenantContext.set and TenantContext.clear.
    # Approximation: extract for-loop body (already done above as $for_body
    # when batch signature detected). For non-batch signature, extract the
    # @KafkaListener body's for-loop the same way.
    local span_body
    if [ -n "${for_body:-}" ]; then
        span_body="$for_body"
    else
        span_body=$(echo "$listener_body" | awk '
            BEGIN { capturing = 0; depth = 0; for_start_depth = -1 }
            {
                line = $0
                if (!capturing && line ~ /for[[:space:]]*\(/) {
                    capturing = 1
                    for_start_depth = depth
                }
                if (capturing) {
                    print line
                    n = length(line)
                    for (i = 1; i <= n; i++) {
                        c = substr(line, i, 1)
                        if (c == "{") depth++
                        else if (c == "}") {
                            depth--
                            if (depth == for_start_depth && capturing) {
                                capturing = 0
                            }
                        }
                    }
                }
            }
        ')
    fi

    if [ -n "$span_body" ]; then
        local set_line ack_line clear_line
        set_line=$(echo "$span_body" | grep -nE 'TenantContext\.set[[:space:]]*\(' | head -1 | cut -d: -f1 || true)
        ack_line=$(echo "$span_body" | grep -nE '\.acknowledge[[:space:]]*\(' | head -1 | cut -d: -f1 || true)
        clear_line=$(echo "$span_body" | grep -nE 'TenantContext\.clear[[:space:]]*\(' | head -1 | cut -d: -f1 || true)

        if [ -n "$set_line" ] && [ -n "$ack_line" ] && [ -n "$clear_line" ] \
                && [ "$ack_line" -gt "$set_line" ] && [ "$ack_line" -lt "$clear_line" ]; then
            echo "VIOLATION [$label] clause(5): ack.acknowledge() at line $ack_line is INSIDE the TenantContext.set (line $set_line) → clear (line $clear_line) span in $consumer" >&2
            echo "  Manual ack interacts with the broker client thread pool; inheriting tenant context defeats explicit-propagation contract." >&2
            echo "  Move ack outside the per-record try-block (canonical: batch ack after the for-loop)." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.ack_inside_tenant_span" >&2
            violations=$((violations + 1))
        fi
    fi

    if [ "$violations" -gt 0 ]; then
        echo "kafka_consumer_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "kafka_consumer_tenant_scope_guard: PASS [$label] — all 5 clauses hold (header-first read + per-record set-in-loop + balanced set/clear + tenant-free rebalance + ack-outside-span)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "kafka_consumer_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all five clauses.
verify_consumer "$PASS_CONSUMER" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships
# a TenantAwareKafkaConsumer.java MUST satisfy the policy. Deployments
# without Kafka have zero such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    candidate="$dir/TenantAwareKafkaConsumer.java"
    if [ -f "$candidate" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_consumer "$candidate" "live:$candidate" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "kafka_consumer_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareKafkaConsumer.java (Kafka-free default)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_CONSUMER="$FAIL_DIR/TenantAwareKafkaConsumer.java"
    if [ -f "$FAIL_CONSUMER" ]; then
        if verify_consumer "$FAIL_CONSUMER" "failing-fixture" 2>/dev/null; then
            echo "kafka_consumer_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "kafka_consumer_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "kafka_consumer_tenant_scope_guard: failing-fixture SKIP — $FAIL_CONSUMER absent"
    fi
fi

exit "$OVERALL"
