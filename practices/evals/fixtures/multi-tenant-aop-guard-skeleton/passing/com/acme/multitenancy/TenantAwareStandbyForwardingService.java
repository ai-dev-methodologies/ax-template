package com.acme.multitenancy;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#kafka-streams-standby-rpc-tenant-scope.tenant_context_propagated_standby_rpc_forward.canonical_skeleton
 * with <root> = acme.
 *
 * Cluster fan-out OBVERSE of
 * TenantAwareInteractiveQueryService (R11
 * #kafka-streams-interactive-queries-tenant-scope). R11
 * closes the SINGLE-NODE read (the partition is local to
 * the request-receiving host); R12 closes the MULTI-NODE
 * fan-out that fires whenever
 * KafkaStreams.queryMetadataForKey reports a non-local
 * active host for the current tenant's prefix key.
 *
 * The router decides per-request whether to delegate
 * locally (re-using the R11 service that already enforces
 * the four single-node clauses) or HTTP-forward to the
 * active host. When forwarding, the tenant scope MUST ride
 * on an X-Tenant-Id header so the receiving node's
 * TenantFilterActivationFilter populates TenantContext on
 * the remote thread.
 *
 * Four guard-checked properties:
 *   (1) X-Tenant-Id header on every forward: without it
 *       the receiving node's TenantContext.current() is
 *       empty, R11 clause(1) trips, and the original
 *       caller observes a 500 that masks the routing-layer
 *       omission. The header name MUST match
 *       #aop-guard.tenant_header_canonical_name so the
 *       receiving filter recognises it.
 *   (2) TenantContext.current() is the SOLE source of the
 *       partition-lookup key. Path / query / body
 *       tenantId is validated for equality against the
 *       context tenantId and discarded — it NEVER flows
 *       into streams.queryMetadataForKey(...) directly.
 *       Same IDOR-vector closure as R11 clause(1), at the
 *       routing layer.
 *   (3) Forward URL has NO tenantId path segment. The
 *       outgoing URL targets a cluster-internal endpoint
 *       (e.g. /internal/iq/metrics) and the X-Tenant-Id
 *       header is the sole carrier of tenant scope across
 *       the wire. Embedding tenantId in the URL leaks it
 *       to access / proxy / mesh logs and re-opens the
 *       IDOR vector if a misconfigured filter chain skips
 *       TenantFilterActivationFilter on internal traffic.
 *   (4) KeyQueryMetadata / HostInfo (other than the
 *       self-identity selfHostInfo, which IS allowed as a
 *       constructor-injected immutable field) MUST be
 *       fetched fresh on every query — never cached as a
 *       field. Streams rebalance reassigns partitions
 *       across hosts; a stale metadata reference routes
 *       the forward to a host that no longer owns the
 *       partition. The OBVERSE of R11 clause(4) at the
 *       metadata layer.
 *
 * Distinct surface from TenantAwareInteractiveQueryService
 * (R11): R11 is the single-node store-range read; this
 * service is the cluster fan-out router that decides
 * local-vs-remote and forwards remote calls. Both files
 * coexist in the multi-tenant fixture (R11 is the 18th
 * canonical file, R12 is the 19th).
 */
@Service
public class TenantAwareStandbyForwardingService {

    static final String KEY_SEPARATOR = "#";
    // Same store-name constant as TenantAwareInteractiveQueryService
    // (R11) and TenantAwareKafkaStreamsTopology (R10) — single
    // static literal, no per-tenant interpolation.
    static final String METRICS_STORE = "tenant-metrics-by-key";
    // X-Tenant-Id is the canonical cross-node tenant carrier;
    // matches #aop-guard.tenant_header_canonical_name so the
    // receiving node's TenantFilterActivationFilter recognises it.
    static final String TENANT_HEADER = "X-Tenant-Id";
    // Internal endpoint has NO tenantId path segment — property (3).
    // Tenant scope rides exclusively on the X-Tenant-Id header.
    static final String INTERNAL_IQ_PATH = "/internal/iq/metrics";

    private final KafkaStreams streams;
    private final RestTemplate restTemplate;
    // selfHostInfo is a constructor-injected immutable identity
    // field (this node's own host:port). It is NOT a cached
    // metadata lookup result — the 45th guard's clause (4) check
    // permits HostInfo fields that are constructor-injected and
    // have no `= ...` initialiser (they came from the DI
    // container, not from a streams.metadata...() call).
    private final HostInfo selfHostInfo;
    private final TenantAwareInteractiveQueryService localQuery;

    public TenantAwareStandbyForwardingService(
            KafkaStreams streams,
            RestTemplate restTemplate,
            HostInfo selfHostInfo,
            TenantAwareInteractiveQueryService localQuery) {
        this.streams = streams;
        this.restTemplate = restTemplate;
        this.selfHostInfo = selfHostInfo;
        this.localQuery = localQuery;
    }

    /**
     * Return the metrics map for the current tenant.
     * Routes locally when this node owns the partition;
     * otherwise HTTP-forwards to the active host.
     *
     * Property (1): every forward path sets the
     * X-Tenant-Id header before exchange().
     * Property (2): the queryMetadataForKey lookup key is
     * built from TenantContext.current() ONLY; the path
     * parameter is validated and discarded.
     * Property (3): forward URL is the cluster-internal
     * /internal/iq/metrics — no tenantId path segment.
     * Property (4): queryMetadataForKey is invoked inside
     * the method body on every call; no field caching.
     */
    public Map<String, Long> metricsForCurrentTenant(UUID pathTenantId) {
        UUID contextTenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "Standby router invoked without TenantContext — "
                + "controller must be SecurityFilterChain-protected and "
                + "TenantFilterActivationFilter must populate the context "
                + "before this service is invoked."));

        // Property (2): validate path tenantId for equality
        // against the verified context tenantId. Mismatch →
        // 404 via TenantBoundaryViolationException, NEVER 403.
        if (pathTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new TenantBoundaryViolationException(
                "Path tenantId does not match authenticated tenant");
        }

        // Property (2) + (4): lookup key derives from
        // TenantContext.current() ONLY; queryMetadataForKey
        // is invoked fresh on every call (NOT cached as a
        // field — Streams rebalance invalidates prior
        // metadata).
        String lookupKey = contextTenantId.toString() + KEY_SEPARATOR;
        KeyQueryMetadata metadata = streams.queryMetadataForKey(
            METRICS_STORE, lookupKey, new StringSerializer());
        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            // NOT_AVAILABLE means rebalance in progress.
            // NEVER fall back to a local-only scan — the local
            // node may currently host a DIFFERENT tenant's
            // partition due to the in-progress rebalance.
            throw new StandbyMetadataNotAvailableException(
                "Streams rebalance in progress — retry later");
        }

        HostInfo activeHost = metadata.activeHost();
        if (isLocal(activeHost)) {
            // Delegate to the R11 service which already
            // enforces the four single-node clauses.
            return localQuery.metricsForCurrentTenant(pathTenantId);
        }

        // Property (1) + (3): X-Tenant-Id header set; URL
        // targets the tenantId-free /internal/iq/metrics.
        URI forwardUri = URI.create(
            "http://" + activeHost.host() + ":" + activeHost.port()
            + INTERNAL_IQ_PATH);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TENANT_HEADER, contextTenantId.toString());
        RequestEntity<Void> request = new RequestEntity<>(
            headers, HttpMethod.GET, forwardUri);
        ParameterizedTypeReference<Map<String, Long>> responseType =
            new ParameterizedTypeReference<Map<String, Long>>() {};
        ResponseEntity<Map<String, Long>> response =
            restTemplate.exchange(request, responseType);
        return Optional.ofNullable(response.getBody())
            .orElse(Map.of());
    }

    /**
     * Point lookup for a specific business key under the
     * current tenant. Same four-property contract as
     * metricsForCurrentTenant — the local-vs-remote
     * decision is made per-call, metadata is fetched
     * fresh, and the X-Tenant-Id header rides the forward
     * when the partition is remote.
     */
    public Optional<Long> metricForKey(UUID pathTenantId, String businessKey) {
        UUID contextTenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "Standby router invoked without TenantContext"));

        if (pathTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new TenantBoundaryViolationException(
                "Path tenantId does not match authenticated tenant");
        }

        // Property (2) + (4): composite lookup key derived
        // from TenantContext.current(); fresh metadata on
        // every call.
        String compositeKey = contextTenantId.toString()
            + KEY_SEPARATOR + businessKey;
        KeyQueryMetadata metadata = streams.queryMetadataForKey(
            METRICS_STORE, compositeKey, new StringSerializer());
        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            throw new StandbyMetadataNotAvailableException(
                "Streams rebalance in progress — retry later");
        }

        HostInfo activeHost = metadata.activeHost();
        if (isLocal(activeHost)) {
            return localQuery.metricForKey(pathTenantId, businessKey);
        }

        // Property (1) + (3): header carries scope; URL is
        // tenantId-free.
        URI forwardUri = URI.create(
            "http://" + activeHost.host() + ":" + activeHost.port()
            + INTERNAL_IQ_PATH + "/" + businessKey);
        HttpHeaders headers = new HttpHeaders();
        headers.set(TENANT_HEADER, contextTenantId.toString());
        RequestEntity<Void> request = new RequestEntity<>(
            headers, HttpMethod.GET, forwardUri);
        ResponseEntity<Long> response =
            restTemplate.exchange(request, Long.class);
        return Optional.ofNullable(response.getBody());
    }

    private boolean isLocal(HostInfo other) {
        return selfHostInfo.host().equals(other.host())
            && selfHostInfo.port() == other.port();
    }
}
