#!/usr/bin/env bash
# kafka_streams_standby_rpc_tenant_scope_guard.sh — dogfood R12 closure (45th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# #kafka-streams-standby-rpc-tenant-scope. Multi-tenant Kafka
# Streams Interactive Queries cluster fan-out (the cross-node
# RPC layer that activates when the local node does NOT host
# the partition for the current tenant's prefix) MUST adopt
# the (TenantContext.current() at the router + X-Tenant-Id
# header on every forward + no tenantId in forward URL +
# fresh metadata lookup per query) pattern with four
# detectable clauses:
#
#   (1) forward_carries_tenant_header — every file that
#       references a forwarding HTTP/RPC primitive
#       (RestTemplate, WebClient, HttpClient, RestClient,
#       HttpRequest) AND calls a Streams metadata API
#       (queryMetadataForKey / allMetadataForKey) MUST
#       also reference the X-Tenant-Id header literal.
#       Absence trips the clause — the receiving node's
#       TenantFilterActivationFilter sees no tenant signal
#       and R11 clause(1) (tenant_context_current_only)
#       trips on the remote IQ service, producing a 500
#       that masks the routing-layer omission.
#       Detection: file contains
#       (RestTemplate|WebClient|HttpClient|RestClient
#        |HttpRequest|getForObject|getForEntity|exchange|
#        postForObject)
#       AND
#       (queryMetadataForKey|allMetadataForKey)
#       BUT NOT
#       "X-Tenant-Id"
#       → trip.
#
#   (2) router_uses_tenant_context_only — every file that
#       calls a Streams metadata API
#       (queryMetadataForKey / allMetadataForKey) MUST
#       also call TenantContext.current(). The lookup key
#       MUST derive from TenantContext.current(); a path /
#       query / body tenantId fed DIRECTLY into the
#       metadata lookup is the same IDOR vector R11
#       clause(1) closes at the store-range layer, now
#       re-opened at the routing layer.
#       Detection: file contains
#       (queryMetadataForKey|allMetadataForKey)
#       BUT NOT
#       TenantContext\.current\(
#       → trip.
#
#   (3) no_path_tenant_in_forward_url — every file that
#       references a forwarding HTTP/RPC primitive MUST
#       NOT construct a URL string that embeds a tenantId
#       path segment. Detection: file contains
#       (RestTemplate|WebClient|HttpClient|RestClient
#        |HttpRequest)
#       AND
#       ("/tenants/" *+|"/tenants/"\{|"/api/tenants/")
#       — i.e. the URL builder embeds `/tenants/` followed
#       by an interpolated identifier or a Spring URL
#       template placeholder.
#       The canonical pattern is a tenantId-free internal
#       endpoint (e.g. /internal/iq/metrics) with the
#       tenant scope carried only by the X-Tenant-Id
#       header. The forward URL itself MUST NOT carry the
#       tenantId — embedding it leaks the tenantId to
#       access / proxy / mesh logs AND re-opens the IDOR
#       vector if a misconfigured filter chain skips
#       TenantFilterActivationFilter on internal traffic.
#
#   (4) fresh_metadata_per_query — every file that calls
#       a Streams metadata API MUST NOT declare a
#       KeyQueryMetadata, Collection<StreamsMetadata>, OR
#       a HostInfo with an `=` initialiser as a field.
#       Concretely the guard scans for:
#         `private ... KeyQueryMetadata <name>;`
#         `private ... Collection<StreamsMetadata> <name>;`
#         `private ... HostInfo <name> = ...;`  (with an
#             `=` initialiser — purely constructor-injected
#             immutable HostInfo identity fields, which
#             have NO `=` initialiser on the field line,
#             are permitted for the self-host identity)
#       Presence trips the clause. Streams rebalance
#       reassigns partitions across hosts; a stale
#       metadata reference routes the forward to a host
#       that no longer owns the partition. The OBVERSE of
#       R11 clause(4) at the metadata layer.
#
# Algorithm:
#   1. For the passing fixture, find
#      TenantAwareStandbyForwardingService.java and verify
#      all four clauses hold.
#   2. Live-repo scan: every backend `.../multitenancy/`
#      package that ships a
#      TenantAwareStandbyForwardingService.java MUST
#      satisfy the policy. Deployments without standby
#      RPC fan-out (single-node clusters, or multi-node
#      without IQ) have zero such files and SKIP.
#   3. With --fixtures, also assert the failing-side
#      variant trips the guard (all four clauses).
#
# Usage:
#   bash practices/evals/kafka_streams_standby_rpc_tenant_scope_guard.sh
#   bash practices/evals/kafka_streams_standby_rpc_tenant_scope_guard.sh --fixtures
#
# Exit codes:
#   0 — every standby forwarder file satisfies the four clauses
#   1 — at least one clause violated OR (with --fixtures) the
#       failing fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#kafka-streams-standby-rpc-tenant-scope"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_FILE="$PASS_DIR/TenantAwareStandbyForwardingService.java"

# Strip block comments and line-comment tails so guard regex
# inspects executable code only (same pre-pass as R6/R7/R8/R9/R10/R11
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
        echo "VIOLATION [$label]: standby forwarder file missing: $service" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    local executable_code
    executable_code=$(strip_comments "$service")

    # Files that don't reference the Streams metadata API at all SKIP.
    # Standby fan-out surface = files that call queryMetadataForKey
    # or allMetadataForKey. A file with neither is not a router and
    # is not subject to this policy.
    if ! echo "$executable_code" | grep -qE '(queryMetadataForKey|allMetadataForKey)[[:space:]]*\('; then
        echo "kafka_streams_standby_rpc_tenant_scope_guard: SKIP [$label] — no Streams metadata API in $service"
        return 0
    fi

    # ── Clause (1): forward_carries_tenant_header ────────────────────
    # If the file uses a forwarding HTTP/RPC primitive AND a metadata
    # lookup, the X-Tenant-Id header literal MUST be present.
    local has_forward_primitive=0
    if echo "$executable_code" | grep -qE '(RestTemplate|WebClient|HttpClient|RestClient|HttpRequest|getForObject|getForEntity|postForObject|exchange[[:space:]]*\()'; then
        has_forward_primitive=1
    fi
    if [ "$has_forward_primitive" -eq 1 ]; then
        if ! echo "$executable_code" | grep -q 'X-Tenant-Id'; then
            echo "VIOLATION [$label] clause(1): standby forwarder calls HTTP/RPC forward without setting X-Tenant-Id header in $service" >&2
            echo "  Receiving node's TenantFilterActivationFilter sees no tenant signal; TenantContext.current() on the remote IQ service throws TenantContextMissingException — surfaces as 500 that masks the routing-layer omission." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.tenant_header_dropped_on_forward" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (2): router_uses_tenant_context_only ──────────────────
    # File MUST reference TenantContext.current(. Without it, the
    # metadata-lookup key can only have come from a request
    # parameter, which is the IDOR vector this anchor closes at
    # the routing layer.
    if ! echo "$executable_code" | grep -qE 'TenantContext\.current[[:space:]]*\('; then
        echo "VIOLATION [$label] clause(2): standby forwarder calls queryMetadataForKey/allMetadataForKey WITHOUT TenantContext.current() in $service" >&2
        echo "  Metadata-lookup key MUST derive from TenantContext.current(), NOT from path/query/body. Path-supplied tenantId in the lookup key is an IDOR vector at the routing layer." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.router_uses_path_tenant_id" >&2
        violations=$((violations + 1))
    fi

    # ── Clause (3): no_path_tenant_in_forward_url ────────────────────
    # If the file uses a forwarding primitive, the forward URL MUST
    # NOT embed a tenantId path segment.
    if [ "$has_forward_primitive" -eq 1 ]; then
        # Detect:
        #   "/tenants/" + <something>     (string concatenation)
        #   "/tenants/{                   (Spring URL template)
        #   "/api/tenants/"               (same in nested path)
        # Quote inside the regex must be balanced; using single-quoted
        # awk-style pattern to avoid shell escape complexity.
        if echo "$executable_code" | grep -qE '("/tenants/"[[:space:]]*\+|"/tenants/\{|"/api/tenants/")'; then
            echo "VIOLATION [$label] clause(3): standby forwarder embeds tenantId as a URL path segment in $service" >&2
            echo "  The forward URL MUST target a tenantId-free internal endpoint (e.g. /internal/iq/metrics). The X-Tenant-Id header is the sole tenant carrier across the wire." >&2
            echo "  Embedding tenantId in the URL path leaks it to access / proxy / mesh logs AND re-opens the IDOR vector if a misconfigured filter chain skips TenantFilterActivationFilter on internal traffic." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.path_tenant_id_in_forward_url" >&2
            violations=$((violations + 1))
        fi
    fi

    # ── Clause (4): fresh_metadata_per_query ─────────────────────────
    # File MUST NOT declare:
    #   private ... KeyQueryMetadata <name>;
    #   private ... Collection<StreamsMetadata> <name>;
    #   private ... HostInfo <name> = <init>;     (only with `=`)
    # Constructor-injected immutable HostInfo identity fields are
    # permitted — they have NO `=` initialiser on the declaration
    # line. The guard targets the FIELD declaration form.
    local clause4=0
    if echo "$executable_code" | grep -qE '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?KeyQueryMetadata[[:space:]]+[a-zA-Z_]'; then
        local bad_kqm
        bad_kqm=$(echo "$executable_code" | grep -E '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?KeyQueryMetadata[[:space:]]+[a-zA-Z_]' | head -1 | sed 's/^[[:space:]]*//')
        echo "VIOLATION [$label] clause(4): standby forwarder caches a KeyQueryMetadata field in $service" >&2
        echo "  Offending declaration: $bad_kqm" >&2
        clause4=1
    fi
    if echo "$executable_code" | grep -qE '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?Collection<StreamsMetadata>[[:space:]]+[a-zA-Z_]'; then
        local bad_csm
        bad_csm=$(echo "$executable_code" | grep -E '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?Collection<StreamsMetadata>[[:space:]]+[a-zA-Z_]' | head -1 | sed 's/^[[:space:]]*//')
        echo "VIOLATION [$label] clause(4): standby forwarder caches a Collection<StreamsMetadata> field in $service" >&2
        echo "  Offending declaration: $bad_csm" >&2
        clause4=1
    fi
    # HostInfo field WITH an `=` initialiser is forbidden;
    # WITHOUT an `=` initialiser (constructor-injected self-host
    # identity) is permitted.
    if echo "$executable_code" | grep -qE '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?HostInfo[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*=[[:space:]]*'; then
        local bad_hi
        bad_hi=$(echo "$executable_code" | grep -E '^[[:space:]]*private[[:space:]]+([a-zA-Z_][a-zA-Z0-9_<>,.[:space:]]*[[:space:]]+)?HostInfo[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*=[[:space:]]*' | head -1 | sed 's/^[[:space:]]*//')
        echo "VIOLATION [$label] clause(4): standby forwarder caches a HostInfo field with initialiser in $service" >&2
        echo "  Offending declaration: $bad_hi" >&2
        echo "  Constructor-injected immutable HostInfo identity fields (no '=' on the declaration line) are permitted; this declaration has an initialiser → stale on rebalance." >&2
        clause4=1
    fi
    if [ "$clause4" -eq 1 ]; then
        echo "  Streams rebalance reassigns partitions across hosts; a stale metadata reference routes the forward to a host that no longer owns the partition." >&2
        echo "  Acquire metadata fresh in each query method via streams.queryMetadataForKey(...) / allMetadataForKey(...)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.failure_modes.cached_metadata_lookup_after_rebalance" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "kafka_streams_standby_rpc_tenant_scope_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "kafka_streams_standby_rpc_tenant_scope_guard: PASS [$label] — all 4 clauses hold (X-Tenant-Id header + TenantContext.current() at router + no tenantId in forward URL + fresh metadata lookup per query)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "kafka_streams_standby_rpc_tenant_scope_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# Passing fixture MUST satisfy all four clauses.
verify_service "$PASS_FILE" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that
# ships a TenantAwareStandbyForwardingService.java MUST satisfy
# the policy. Single-node clusters and deployments without IQ
# have zero such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    candidate="$dir/TenantAwareStandbyForwardingService.java"
    if [ -f "$candidate" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_service "$candidate" "live:$candidate" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "kafka_streams_standby_rpc_tenant_scope_guard: live-repo SKIP — no .../multitenancy/TenantAwareStandbyForwardingService.java (standby-RPC-free default; single-node clusters never adopt this surface)"
fi

# --fixtures mode: failing-fixture sibling MUST trip the guard.
if [ "$MODE" = "fixtures" ]; then
    FAIL_FILE="$FAIL_DIR/TenantAwareStandbyForwardingService.java"
    if [ -f "$FAIL_FILE" ]; then
        if verify_service "$FAIL_FILE" "failing-fixture" 2>/dev/null; then
            echo "kafka_streams_standby_rpc_tenant_scope_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "kafka_streams_standby_rpc_tenant_scope_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "kafka_streams_standby_rpc_tenant_scope_guard: failing-fixture SKIP — $FAIL_FILE absent"
    fi
fi

exit "$OVERALL"
