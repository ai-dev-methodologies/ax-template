package com.acme.multitenancy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * FAILING fixture — intentionally trips the 44th guard
 * (kafka_streams_interactive_queries_tenant_scope_guard.sh)
 * by violating four detectable clauses in
 * #kafka-streams-interactive-queries-tenant-scope:
 *
 *   (1) Path tenantId is fed DIRECTLY into the store range
 *       prefix without consulting TenantContext.current() →
 *       path_tenant_id_as_prefix.
 *   (2) store.all() is used instead of store.range(prefix,
 *       prefix + "￿") → unscoped_store_all_scan.
 *   (3) Path tenantId mismatch throws AccessDeniedException
 *       (HTTP 403) instead of TenantBoundaryViolationException
 *       (HTTP 404) → access_denied_existence_leak.
 *   (4) ReadOnlyKeyValueStore is cached as a field across
 *       requests → cached_store_reference_after_rebalance.
 *
 * The 44th guard MUST detect all four clauses and exit
 * non-zero on --fixtures mode.
 */
@Service
public class TenantAwareInteractiveQueryService {

    static final String KEY_SEPARATOR = "#";
    static final String METRICS_STORE = "tenant-metrics-by-key";

    private final KafkaStreams streams;

    // VIOLATION clause (4): ReadOnlyKeyValueStore reference
    // cached as a field. Streams rebalance invalidates the
    // reference; a stale reference reads from the OLD
    // partition assignment and may surface other tenants'
    // data after rebalance.
    private ReadOnlyKeyValueStore<String, Long> cachedStore;

    public TenantAwareInteractiveQueryService(KafkaStreams streams) {
        this.streams = streams;
        // VIOLATION clause (4): cache at construction time.
        // The store reference is bound to the partition
        // assignment AT CONSTRUCTION; every rebalance after
        // that point makes this reference stale.
        this.cachedStore = streams.store(
            StoreQueryParameters.fromNameAndType(
                METRICS_STORE, QueryableStoreTypes.keyValueStore()));
    }

    public Map<String, Long> metricsForTenant(UUID pathTenantId) {
        // VIOLATION clause (3): mismatch throws
        // AccessDeniedException (→ HTTP 403) instead of
        // TenantBoundaryViolationException (→ HTTP 404).
        // 403 leaks the existence of the cross-tenant
        // resource; 404 hides it.
        UUID contextTenantId = TenantContext.current().orElse(null);
        if (contextTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new AccessDeniedException(
                "Forbidden: tenantId " + pathTenantId
                + " does not match authenticated tenant "
                + contextTenantId);
        }

        // VIOLATION clause (1): path tenantId is used DIRECTLY
        // as the store-range prefix. An attacker can supply
        // any tenantId in the URL and (modulo the mismatch
        // check above, which they can sometimes bypass if
        // TenantContext is absent) read another tenant's
        // metrics. The prefix MUST come from
        // TenantContext.current(), not from the request.
        String prefix = pathTenantId.toString() + KEY_SEPARATOR;

        Map<String, Long> result = new HashMap<>();
        // VIOLATION clause (2): store.all() scans every
        // tenant's state in RocksDB. The post-filter below
        // (which would be missing if a careless developer
        // refactors) is the only thing standing between this
        // call and a full multi-tenant data dump. The
        // canonical pattern uses store.range(prefix, prefix +
        // "￿") so the scan is structurally
        // tenant-scoped at the storage layer.
        try (KeyValueIterator<String, Long> it = cachedStore.all()) {
            while (it.hasNext()) {
                KeyValue<String, Long> kv = it.next();
                if (kv.key.startsWith(prefix)) {
                    int sep = kv.key.indexOf(KEY_SEPARATOR);
                    if (sep > 0) {
                        result.put(kv.key.substring(sep + 1), kv.value);
                    }
                }
            }
        }
        return result;
    }
}
