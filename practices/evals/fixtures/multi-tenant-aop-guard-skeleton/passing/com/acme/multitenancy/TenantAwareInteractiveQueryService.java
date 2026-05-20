package com.acme.multitenancy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#kafka-streams-interactive-queries-tenant-scope.tenant_context_scoped_store_range.canonical_skeleton
 * with <root> = acme.
 *
 * Read-side OBVERSE of TenantAwareKafkaStreamsTopology (R10
 * #kafka-streams-tenant-scope). The write-side topology
 * tenant-prefixes every state-store key as
 * `tenantId#originalKey`; Interactive Queries (IQ) must read
 * the same prefix range — and ONLY that range — for the
 * current request's tenant.
 *
 * Four guard-checked properties:
 *   (1) TenantContext.current() is the SOLE source of the
 *       store-range prefix. Path / query / body tenantId is
 *       validated against the context tenantId and discarded;
 *       it is NEVER passed into store.range(...) directly.
 *   (2) Range scan uses store.range(prefix, prefix + "\uFFFF")
 *       NOT store.all(). store.all() would scan every tenant's
 *       state (a full RocksDB scan) and would have to filter
 *       in the application layer — a cross-tenant IDOR vector
 *       the moment the filter is misimplemented or skipped.
 *   (3) Path / query tenantId mismatch with TenantContext
 *       throws TenantBoundaryViolationException (→ HTTP 404
 *       via MultiTenantProblemDetailAdvice). NEVER throw 403
 *       or AccessDeniedException — both would leak the
 *       existence of a cross-tenant resource.
 *   (4) ReadOnlyKeyValueStore reference is acquired fresh on
 *       every query via streams.store(StoreQueryParameters
 *       .fromNameAndType(...)). It is NEVER cached as a field
 *       across requests — Streams rebalances can invalidate
 *       any cached reference and a stale reference reads
 *       from the OLD partition assignment (other tenants'
 *       data after rebalance).
 *
 * Distinct surface from TenantAwareKafkaStreamsTopology (R10):
 * R10 is the WRITE-side topology (selectKey prefix +
 * punctuator set/clear); this service is the READ-side IQ
 * surface (controller-invoked, request-scoped, MUST mirror
 * the write-side prefix on the read side).
 */
@Service
public class TenantAwareInteractiveQueryService {

    static final String KEY_SEPARATOR = "#";
    // Match the write-side METRICS_STORE constant from
    // TenantAwareKafkaStreamsTopology — same static literal,
    // no per-tenant interpolation (write-side property 5).
    static final String METRICS_STORE = "tenant-metrics-by-key";

    private final KafkaStreams streams;

    public TenantAwareInteractiveQueryService(KafkaStreams streams) {
        this.streams = streams;
    }

    /**
     * Return the metrics map for the current tenant. The
     * controller MUST be invoked under a populated
     * TenantContext (TenantFilterActivationFilter runs
     * AFTER security and populates the context from the
     * authenticated principal — see
     * #aop-guard.filter_activation).
     *
     * Property (1): tenantId comes from TenantContext.current()
     * only. The path parameter is validated (property 3) but
     * NEVER fed to store.range(...).
     * Property (2): store.range(prefix, prefix + "\uFFFF")
     * scans only the current tenant's contiguous key range.
     * Property (4): the store reference is acquired fresh
     * via streams.store(StoreQueryParameters...) on every
     * call; no field caching.
     */
    public Map<String, Long> metricsForCurrentTenant(UUID pathTenantId) {
        UUID contextTenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "Interactive query invoked without TenantContext — "
                + "controller must be SecurityFilterChain-protected and "
                + "TenantFilterActivationFilter must populate the context "
                + "before this service is invoked."));

        // Property (3): validate path tenantId against the
        // context tenantId. Mismatch → 404 via
        // TenantBoundaryViolationException, NEVER 403 (would
        // leak existence of the cross-tenant resource).
        if (pathTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new TenantBoundaryViolationException(
                "Path tenantId does not match authenticated tenant");
        }

        // Property (4): fresh store lookup on every query.
        // Streams rebalances invalidate any cached reference;
        // a stale reference would read the OLD partition
        // assignment and could surface other tenants' data
        // (the partition may have been reassigned to a host
        // that now serves a different tenant range).
        ReadOnlyKeyValueStore<String, Long> store = streams.store(
            StoreQueryParameters.fromNameAndType(
                METRICS_STORE, QueryableStoreTypes.keyValueStore()));

        // Property (1) + (2): prefix derived from
        // TenantContext.current() ONLY; store.range with
        // upper bound prefix + "\uFFFF" scans only the
        // current tenant's contiguous key range.
        String prefix = contextTenantId.toString() + KEY_SEPARATOR;
        String upperBound = prefix + "\uFFFF";

        Map<String, Long> result = new HashMap<>();
        try (KeyValueIterator<String, Long> it = store.range(prefix, upperBound)) {
            while (it.hasNext()) {
                KeyValue<String, Long> kv = it.next();
                int sep = kv.key.indexOf(KEY_SEPARATOR);
                if (sep <= 0) {
                    continue;
                }
                // Strip the tenant prefix from the public
                // response key — the caller already knows
                // their own tenantId; leaking it in the
                // response is redundant and creates a
                // grep target if logs are aggregated.
                String publicKey = kv.key.substring(sep + 1);
                result.put(publicKey, kv.value);
            }
        }
        return result;
    }

    /**
     * Point lookup for a specific business key under the
     * current tenant. Same property contract as
     * metricsForCurrentTenant — store reference is acquired
     * fresh, tenantId comes from TenantContext, the composite
     * key is constructed locally (NOT taken from a request
     * field).
     */
    public Optional<Long> metricForKey(UUID pathTenantId, String businessKey) {
        UUID contextTenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "Interactive query invoked without TenantContext"));

        if (pathTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new TenantBoundaryViolationException(
                "Path tenantId does not match authenticated tenant");
        }

        ReadOnlyKeyValueStore<String, Long> store = streams.store(
            StoreQueryParameters.fromNameAndType(
                METRICS_STORE, QueryableStoreTypes.keyValueStore()));

        // Property (1): composite key built from
        // TenantContext.current() + caller-supplied business
        // key. The caller supplies the LATTER half only;
        // the tenant half comes from the context.
        String compositeKey = contextTenantId.toString()
            + KEY_SEPARATOR + businessKey;
        return Optional.ofNullable(store.get(compositeKey));
    }
}
