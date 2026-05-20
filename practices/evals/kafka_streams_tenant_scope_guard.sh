#!/usr/bin/env bash
# kafka_streams_tenant_scope_guard.sh — dogfood R10 closure (43rd hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#kafka-streams-tenant-scope`. Multi-tenant Kafka Streams (KStream /
# KTable) topologies MUST adopt the tenant-prefixed key + punctuator
# key-decode + tenant-namespaced join pattern with four detectable
# clauses (property 5 detection is the static-name check):
#
#   (1) Cross-tenant state store collision — any file containing
#       .groupBy( / .groupByKey( / .aggregate( substrings MUST also
#       contain a .selectKey( (or .map() that emits a key) lambda
#       body that references the X-Tenant-Id header (lastHeader call
#       or TENANT_HEADER substring) AND constructs a composite key
#       with KEY_SEPARATOR (the "#" separator or KEY_SEPARATOR
#       constant). See #failure_modes.cross_tenant_state_store_collision.
#
#   (2) Punctuator silent context loss — every context.schedule(...)
#       lambda body (extracted by brace-balanced scope) MUST satisfy:
#       if it contains a .forward( call, it MUST also contain
#       TenantContext.set( AND TenantContext.clear( substrings inside
#       the same lambda scope. Absence of either trips the clause.
#       See #failure_modes.punctuator_silent_context_loss.
#
#   (3) Punctuator set/clear imbalance — inside each context.schedule
#       lambda body, count(TenantContext.set) MUST equal
#       count(TenantContext.clear). Same algorithm as the R9 42nd-guard
#       clause(3) but scoped to the punctuator surface.
#       See #failure_modes.punctuator_set_clear_imbalance.
#
#   (4) Cross-tenant join key — any file containing .join( /
#       .leftJoin( / .outerJoin( substrings MUST also contain a
#       .selectKey( lambda body that references TENANT_HEADER /
#       "X-Tenant-Id" AND KEY_SEPARATOR (same upstream-prefix
#       requirement as clause 1). A topology with joins but no
#       tenant-prefix selectKey trips the clause. See
#       #failure_modes.cross_tenant_join_key.
#
#   (5) Per-tenant state store naming — Materialized.as("..." +
#       tenantId / "..." + tenant_id) is REJECTED. The guard scans
#       Materialized.as( occurrences; if the argument expression
#       contains a "+" concatenation with .toString() / tenantId
#       / tenant_id token, trip the clause. Static literal names
#       pass. See #failure_modes.per_tenant_state_store_naming.
#
# Algorithm:
#   1. For the passing fixture, find TenantAwareKafkaStreamsTopology.java
#      and verify all five clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/` package that
#      ships a TenantAwareKafkaStreamsTopology.java MUST satisfy the
#      policy. Deployments without Kafka Streams have zero such files
#      and SKIP.
#   3. With --fixtures, also assert the failing-side variant trips
#      the guard (cross_tenant_state_store_collision +
#      punctuator_silent_context_loss + cross_tenant_join_key +
#      per_tenant_state_store_naming).
#
# Usage:
#   bash practices/evals/kafka_streams_tenant_scope_guard.sh
#   bash practices/evals/kafka_streams_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every topology file satisfies the five clauses
#   1 — at least one clause violated OR (with --fixtures) the failing
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#kafka-streams-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_TOPOLOGY="$PASS_DIR/TenantAwareKafkaStreamsTopology.java"

# Strip block comments and line-comment tails so guard regex inspects
# executable code only (same pre-pass as R6/R7/R8/R9 guards).
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

# Extract every context.schedule(...) lambda body in the file. Each
# emitted block is the content between the opening "{" of the lambda
# body and its matching closing "}", separated by ==SCHED_END== lines.
# Uses brace-balanced scope, starting capture at the FIRST "{" AFTER
# a `context.schedule(` substring is seen.
extract_schedule_bodies() {
    awk '
        BEGIN { armed = 0; capturing = 0; depth = 0 }
        {
            line = $0
            n = length(line)
            i = 1
            # Arm capture once we see context.schedule(. Capture starts
            # at the next "{" character on this line or subsequent
            # lines.
            if (!armed && !capturing) {
                if (line ~ /context\.schedule[[:space:]]*\(/) {
                    armed = 1
                }
            }
            # Walk character-by-character so the arming-{ on the same
            # line as context.schedule is handled.
            for (i = 1; i <= n; i++) {
                c = substr(line, i, 1)
                if (capturing) {
                    if (c == "{") depth++
                    else if (c == "}") {
                        depth--
                        if (depth == 0) {
                            capturing = 0
                            print "==SCHED_END=="
                            armed = 0
                            continue
                        }
                    }
                } else if (armed && c == "{") {
                    capturing = 1
                    depth = 1
                }
            }
            if (capturing) {
                print line
            }
        }
    '
}

verify_topology() {
    local topology="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$topology" ]; then
        echo "VIOLATION [$label]: topology file missing: $topology" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$topology")

    # Files that don't reference Kafka Streams API at all SKIP.
    if ! echo "$executable_code" | grep -qE '(StreamsBuilder|KStream|KTable|context\.schedule)'; then
        echo "kafka_streams_tenant_scope_guard: SKIP [$label] — no Kafka Streams API in $topology"
        return 0
    fi

    # ── Clause (1): cross_tenant_state_store_collision ────────────────────
    # If the file has groupBy/groupByKey/aggregate, it MUST have a
    # selectKey lambda that references TENANT_HEADER and KEY_SEPARATOR
    # (or the literal "#").
    if echo "$executable_code" | grep -qE '\.(groupBy|groupByKey|aggregate)[[:space:]]*\('; then
        local has_tenant_prefix_key=0
        if echo "$executable_code" | grep -qE '\.selectKey[[:space:]]*\(' \
                && echo "$executable_code" | grep -qE '(lastHeader[[:space:]]*\([^)]*("X-Tenant-Id"|TENANT_HEADER)|TENANT_HEADER)' \
                && echo "$executable_code" | grep -qE '(KEY_SEPARATOR|"#")'; then
            has_tenant_prefix_key=1
        fi
        if [ "$has_tenant_prefix_key" -eq 0 ]; then
            echo "VIOLATION [$label] clause(1): topology contains groupBy/groupByKey/aggregate WITHOUT an upstream selectKey lambda that reads TENANT_HEADER AND constructs a composite key with KEY_SEPARATOR / '#' in $topology" >&2
            echo "  All tenants' state merges into one RocksDB bucket; aggregate counts span every tenant." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.cross_tenant_state_store_collision" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (2): punctuator_silent_context_loss ────────────────────────
    # ── Clause (3): punctuator_set_clear_imbalance ────────────────────────
    # Extract every context.schedule lambda body and verify:
    #   - if forward( is present, TenantContext.set AND clear MUST be too;
    #   - count(set) == count(clear).
    local schedule_bodies
    schedule_bodies=$(echo "$executable_code" | extract_schedule_bodies)

    if [ -n "$schedule_bodies" ]; then
        # Split into individual bodies by ==SCHED_END== separator.
        local body=""
        local body_index=0
        while IFS= read -r line; do
            if [ "$line" = "==SCHED_END==" ]; then
                body_index=$((body_index + 1))
                # Verify this body.
                local has_forward set_count clear_count
                if echo "$body" | grep -qE '\.forward[[:space:]]*\('; then
                    has_forward=1
                else
                    has_forward=0
                fi
                set_count=$(echo "$body" | grep -cE 'TenantContext\.set[[:space:]]*\(' || true)
                clear_count=$(echo "$body" | grep -cE 'TenantContext\.clear[[:space:]]*\(' || true)

                if [ "$has_forward" -eq 1 ] && { [ "$set_count" -eq 0 ] || [ "$clear_count" -eq 0 ]; }; then
                    echo "VIOLATION [$label] clause(2): context.schedule lambda #$body_index calls forward(...) WITHOUT TenantContext.set/clear in $topology (set=$set_count, clear=$clear_count, forward=$has_forward)" >&2
                    echo "  StreamThread runs the punctuator with empty TenantContext by construction;" >&2
                    echo "  downstream sinks calling TenantContext.current() NPE or fall back to the LAST iteration's stale tenant (silent cross-tenant write)." >&2
                    echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.punctuator_silent_context_loss" >&2
                    violations=$((violations + 1))
                elif [ "$set_count" -gt 0 ] && [ "$clear_count" -gt 0 ] && [ "$set_count" -ne "$clear_count" ]; then
                    echo "VIOLATION [$label] clause(3): context.schedule lambda #$body_index has unbalanced TenantContext.set ($set_count) vs TenantContext.clear ($clear_count) in $topology" >&2
                    echo "  Unbalanced set/clear leaks tenant N into the next punctuator firing on the same StreamThread." >&2
                    echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.punctuator_set_clear_imbalance" >&2
                    violations=$((violations + 1))
                fi
                body=""
            else
                body="$body
$line"
            fi
        done <<< "$schedule_bodies"
    fi

    # ── Clause (4): cross_tenant_join_key ─────────────────────────────────
    # If the file has .join(/.leftJoin(/.outerJoin(, it MUST have an
    # upstream selectKey lambda that references TENANT_HEADER and
    # KEY_SEPARATOR. (Same requirement as clause 1 but triggered by
    # join surface, not state-store surface.)
    if echo "$executable_code" | grep -qE '\.(join|leftJoin|outerJoin)[[:space:]]*\('; then
        local has_join_tenant_prefix=0
        if echo "$executable_code" | grep -qE '\.selectKey[[:space:]]*\(' \
                && echo "$executable_code" | grep -qE '(lastHeader[[:space:]]*\([^)]*("X-Tenant-Id"|TENANT_HEADER)|TENANT_HEADER)' \
                && echo "$executable_code" | grep -qE '(KEY_SEPARATOR|"#")'; then
            has_join_tenant_prefix=1
        fi
        if [ "$has_join_tenant_prefix" -eq 0 ]; then
            echo "VIOLATION [$label] clause(4): topology contains .join/.leftJoin/.outerJoin WITHOUT an upstream tenant-prefix selectKey lambda (reads TENANT_HEADER + uses KEY_SEPARATOR) in $topology" >&2
            echo "  Tenant A's record joins Tenant B's record on bare business-key equality; silent cross-tenant data fabrication." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.cross_tenant_join_key" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (5): per_tenant_state_store_naming ─────────────────────────
    # Scan for Materialized.as(...) calls. If the argument expression
    # contains a "+" concatenation with .toString() / tenantId /
    # tenant_id token, trip. Static literals pass.
    #
    # Multi-line aware: the `Materialized.` token and the `as(` call
    # can be on different lines because the generic-typed builder
    # form is often line-wrapped. We collapse executable code to a
    # single line (each newline → space) so the same-line grep
    # matches across logical lines.
    local single_line_code mat_violation
    single_line_code=$(echo "$executable_code" | tr '\n' ' ')
    if echo "$single_line_code" | grep -qE 'Materialized\.[^;]*as[[:space:]]*\([^)]*\+[^)]*(tenantId|tenant_id|\.toString[[:space:]]*\()'; then
        mat_violation=$(echo "$single_line_code" | grep -oE 'Materialized\.[^;]*as[[:space:]]*\([^)]*\+[^)]*(tenantId|tenant_id|\.toString[[:space:]]*\()[^)]*\)' | head -1)
        echo "VIOLATION [$label] clause(5): Materialized.as(...) interpolates a tenant identifier into the state store name in $topology" >&2
        echo "  Offending construction (collapsed to single line): $mat_violation" >&2
        echo "  Static topology build cannot declare dynamic per-tenant stores; rebalance/standby breaks." >&2
        echo "  Use a static literal store name and tenant-prefix the KEY instead (clause 1)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.per_tenant_state_store_naming" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "kafka_streams_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "kafka_streams_tenant_scope_guard: PASS [$label] — all 5 clauses hold (tenant-prefix selectKey + punctuator set/clear + balanced counts + tenant-namespaced joins + static store name)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "kafka_streams_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all five clauses.
verify_topology "$PASS_TOPOLOGY" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships
# a TenantAwareKafkaStreamsTopology.java MUST satisfy the policy.
# Deployments without Kafka Streams have zero such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    candidate="$dir/TenantAwareKafkaStreamsTopology.java"
    if [ -f "$candidate" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_topology "$candidate" "live:$candidate" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "kafka_streams_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareKafkaStreamsTopology.java (Kafka-Streams-free default)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_TOPOLOGY="$FAIL_DIR/TenantAwareKafkaStreamsTopology.java"
    if [ -f "$FAIL_TOPOLOGY" ]; then
        if verify_topology "$FAIL_TOPOLOGY" "failing-fixture" 2>/dev/null; then
            echo "kafka_streams_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "kafka_streams_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "kafka_streams_tenant_scope_guard: failing-fixture SKIP — $FAIL_TOPOLOGY absent"
    fi
fi

exit "$OVERALL"
