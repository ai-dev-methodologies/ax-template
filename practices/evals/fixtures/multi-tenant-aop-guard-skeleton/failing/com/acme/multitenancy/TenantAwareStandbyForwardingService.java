package com.acme.multitenancy;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * FAILING fixture for the 45th hard guard
 * (#kafka-streams-standby-rpc-tenant-scope). Each of the
 * four clauses is intentionally violated so the guard's
 * detection regex is exercised:
 *
 *   Clause (1) tenant_header_dropped_on_forward: the
 *       outbound RestTemplate call does NOT set the
 *       X-Tenant-Id header. The receiving node sees empty
 *       TenantContext.
 *
 *   Clause (2) router_uses_path_tenant_id: the
 *       queryMetadataForKey lookup key is built from
 *       pathTenantId DIRECTLY, with NO TenantContext.current()
 *       call anywhere in the file. Classic IDOR: attacker
 *       picks the routing target via the URL.
 *
 *   Clause (3) path_tenant_id_in_forward_url: the forward
 *       URL embeds tenantId as a path segment
 *       (`/api/tenants/{tenantId}/internal/metrics`). Logs
 *       and mesh telemetry capture the tenantId.
 *
 *   Clause (4) cached_metadata_lookup_after_rebalance:
 *       the KeyQueryMetadata reference and a
 *       Collection<StreamsMetadata>-like cached HostInfo
 *       are declared as fields with @PostConstruct
 *       initialisers — they go stale on the next Streams
 *       rebalance.
 */
@Service
public class TenantAwareStandbyForwardingService {

    static final String KEY_SEPARATOR = "#";
    static final String METRICS_STORE = "tenant-metrics-by-key";

    private final KafkaStreams streams;
    private final RestTemplate restTemplate;
    // Clause (4) violation: cached HostInfo reference with
    // an @PostConstruct initialiser — stale on rebalance.
    private HostInfo cachedActiveHost = new HostInfo("stale-host", 8080);
    // Clause (4) violation: cached KeyQueryMetadata reference.
    private KeyQueryMetadata cachedMetadata;

    public TenantAwareStandbyForwardingService(
            KafkaStreams streams, RestTemplate restTemplate) {
        this.streams = streams;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void primeMetadataCache() {
        // Clause (4): initialise the metadata field once at
        // startup. Subsequent rebalances are NOT propagated.
        this.cachedMetadata = streams.queryMetadataForKey(
            METRICS_STORE, "any#", new StringSerializer());
        if (cachedMetadata != null
                && cachedMetadata != KeyQueryMetadata.NOT_AVAILABLE) {
            this.cachedActiveHost = cachedMetadata.activeHost();
        }
    }

    public Map<String, Long> metricsForTenant(UUID pathTenantId) {
        // Clause (2) violation: NO TenantContext.current()
        // call. The lookup key is built from pathTenantId
        // DIRECTLY — attacker controls routing target via
        // the URL.
        String lookupKey = pathTenantId.toString() + KEY_SEPARATOR;
        // Even calls queryMetadataForKey at request time (which would
        // otherwise be fine for clause (4)) but still uses cached
        // result fields. The lookup-key path is the IDOR vector.
        KeyQueryMetadata metadata = streams.queryMetadataForKey(
            METRICS_STORE, lookupKey, new StringSerializer());
        HostInfo host = (metadata != null
                && metadata != KeyQueryMetadata.NOT_AVAILABLE)
            ? metadata.activeHost()
            : cachedActiveHost;

        // Clause (3) violation: forward URL embeds tenantId as
        // a path segment. Logs / mesh / proxy access logs all
        // capture the tenantId.
        URI forwardUri = URI.create(
            "http://" + host.host() + ":" + host.port()
            + "/api/tenants/" + pathTenantId + "/internal/metrics");

        // Clause (1) violation: NO X-Tenant-Id header set.
        // The receiving node's TenantFilterActivationFilter
        // sees no tenant signal; remote TenantContext is empty.
        ResponseEntity<Map> response =
            restTemplate.getForEntity(forwardUri, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Long> body = response.getBody();
        return Optional.ofNullable(body).orElse(Map.of());
    }
}
