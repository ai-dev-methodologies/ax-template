#!/usr/bin/env bash
# practices/evals/multi_tenant_aop_guard_skeleton_guard.sh — dogfood-5 (30th hard guard).
#
# Mechanically enforces practices/rules/multi-tenant-aop-guard-skeleton.md.
# Every `.../multitenancy/` subpackage that ships a multi-tenant skeleton
# MUST contain the canonical files defined in
# blueprints/multi-tenant-manifest.yaml. Fork-receiver adoption is
# mechanical substitution of `<root>` — this guard verifies completeness.
# File count has grown by closure round:
#   R3 baseline: 11 files (#aop-guard + #async-propagation + #context-resolution)
#   R4 closure : +AuditEvent.java                          → 12 files (GAP-R3-3)
#   R5 closure : +TenantAwareCallbackVerifier.java + 2 exns → +callback skeleton
#                (counted separately; not added to REQUIRED_FILES list since
#                the callback-tenant-resolution guard owns content verification)
#   R6 closure : +TenantIterationScheduler.java           → 13 files (GAP-R3-5)
#   R7 closure : +TenantAwareSseEmitterRegistry.java      → 14 files (GAP-NEW-1)
#   R8 closure : +TenantAwareRedisPubSubBridge.java       → 15 files (GAP-NEW-2)
#   R9 closure : +TenantAwareKafkaConsumer.java           → 16 files (kafka-consumer)
#   R10 closure: +TenantAwareKafkaStreamsTopology.java    → 17 files (kafka-streams)
#   R11 closure: +TenantAwareInteractiveQueryService.java → 18 files (kafka-streams-interactive-queries)
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
#   0 — every multitenancy/ package contains the 18 expected files
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
    # 13th file added R6 — closes GAP-R3-5 (scheduled-task tenant scope).
    # Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#scheduled-task-tenant-scope
    # and is content-verified by scheduled_task_tenant_scope_guard.sh (39th).
    # The sibling TenantCatalog.java interface is verified by the 39th
    # guard's tenant_catalog_contract clause (not duplicated here to
    # keep this list to one canonical file per surface).
    "TenantIterationScheduler.java"
    # 14th file added R7 — closes GAP-NEW-1 (realtime long-lived
    # connection tenant scope). Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#realtime-connection-tenant-scope
    # and is content-verified by realtime_connection_tenant_scope_guard.sh (40th).
    "TenantAwareSseEmitterRegistry.java"
    # 15th file added R8 — closes GAP-NEW-2 (broker fan-out across
    # multi-node SSE scale-out). Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#broker-fanout-tenant-scope
    # and is content-verified by broker_fanout_tenant_scope_guard.sh (41st).
    # Single-node deployments SKIP via the 41st guard's live-repo SKIP
    # branch — this file is OPT-IN for multi-node fan-out adoption.
    # The Kafka variant TenantAwareKafkaFanoutBridge.java is recognized
    # by the 41st guard but not listed here to keep one canonical file
    # per surface (Redis is the documented default).
    "TenantAwareRedisPubSubBridge.java"
    # 16th file added R9 — closes the kafka-consumer open question
    # carried over from R7/R8. Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#kafka-consumer-tenant-scope
    # and is content-verified by kafka_consumer_tenant_scope_guard.sh (42nd).
    # Distinct surface from TenantAwareRedisPubSubBridge (R8): R8 dispatches
    # INTO the SSE registry (push-out); this consumer processes business
    # events and invokes service-layer code (in-direction). Kafka-free
    # deployments SKIP via the 42nd guard's live-repo SKIP branch — this
    # file is OPT-IN for fork-receivers that adopt Kafka consumers.
    "TenantAwareKafkaConsumer.java"
    # 17th file added R10 — closes the kafka-streams open question
    # staked in R9 (#kafka-consumer-tenant-scope.open_questions_remaining[0]
    # pre-R10 ordering). Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#kafka-streams-tenant-scope
    # and is content-verified by kafka_streams_tenant_scope_guard.sh (43rd).
    # Distinct surface from TenantAwareKafkaConsumer (R9): R9 is the
    # stateless @KafkaListener consumer; this topology builds durable
    # state stores (KTables), runs wall-clock punctuators, and performs
    # tenant-namespaced joins. Stream-processing-free deployments SKIP
    # via the 43rd guard's live-repo SKIP branch — this file is OPT-IN
    # for fork-receivers that adopt Kafka Streams real-time aggregation.
    "TenantAwareKafkaStreamsTopology.java"
    # 18th file added R11 — closes the kafka-streams interactive
    # queries open question staked in R10
    # (#kafka-streams-tenant-scope.open_questions_remaining[0]
    # pre-R11 ordering: "Kafka Streams interactive queries (HTTP
    # endpoint that exposes the state store via
    # store.range(prefix, prefix + sentinel)) for tenant-scoped
    # reads — the read-side filter is the OBVERSE of the
    # write-side prefix."). Pairs with anchor
    # blueprints/multi-tenant-manifest.yaml#kafka-streams-interactive-queries-tenant-scope
    # and is content-verified by
    # kafka_streams_interactive_queries_tenant_scope_guard.sh (44th).
    # Distinct surface from TenantAwareKafkaStreamsTopology (R10):
    # R10 is the WRITE-side topology (selectKey prefix +
    # punctuator set/clear); this service is the READ-side
    # Interactive Queries surface (controller-invoked,
    # request-scoped, mirrors the write-side prefix at read
    # time). Interactive-Queries-free deployments SKIP via the
    # 44th guard's live-repo SKIP branch — this file is OPT-IN
    # for fork-receivers that adopt Kafka Streams IQ HTTP reads.
    "TenantAwareInteractiveQueryService.java"
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

# Passing fixture: the canonical fork-receiver simulation MUST have all 18 files.
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
