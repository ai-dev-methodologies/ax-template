package com.ax.template.authblueprint.realtime;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reference SSE endpoint set for realtime-policy-l0 (specs/realtime-policy-l0.yaml).
 * SSE over MVC {@link SseEmitter} — plain chunked HTTP GET, so the EXISTING Spring
 * Security chain authenticates the subscribe (RT-CHANNEL-AUTH-001): the
 * {@code /api/realtime/**} matcher in SecurityConfig is {@code authenticated()}, so an
 * unauthenticated subscribe is rejected 401 by the chain BEFORE this controller runs —
 * there is no "WebSocket bypass" because there is no protocol switch to bypass.
 *
 * <p>Production projects do not need to ship the demo publish endpoint; only
 * {@link RealtimeChannelService} + {@link RealtimeMetrics} are required. The controller
 * exercises the policy BLACK-BOX so {@code RealtimePolicyComplianceTest} can assert it
 * over real {@code text/event-stream} HTTP.
 */
@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {

    /**
     * RT-CHANNEL-AUTH-002 — the resolved tenant scope every authenticated caller maps to.
     *
     * <p><b>⚠ MULTI-TENANT FORK — YOU MUST REPLACE THIS.</b> This static {@code "default"}
     * is a SINGLE-TENANT PLACEHOLDER. It is correct for the reference workload (a single
     * shared scope is what lets multiple callers fan out on one topic; a per-user scope would
     * break fan-out), but a fork that enables {@code tenant_model: multi} WITHOUT replacing
     * it gets ONE shared registry key across ALL tenants — a cross-tenant data leak. A
     * multi-tenant fork MUST resolve the scope from {@code TenantContext} (multi-tenant-l0
     * {@code MULTI-TENANT-PROPAGATION-001}, via the existing tenancy runtime + the
     * {@code realtime_connection_tenant_scope_guard}) so each tenant gets a DISTINCT registry
     * key. This controller does NOT add a tenant guard — it composes with the existing one by
     * reference. See templates/L4/realtime-policy/README.md "MULTI-TENANT FORK" section.
     */
    static final String RESOLVED_TENANT_SCOPE = "default";

    private final RealtimeChannelService channelService;
    private final RealtimeProperties properties;

    public RealtimeController(RealtimeChannelService channelService, RealtimeProperties properties) {
        this.channelService = channelService;
        this.properties = properties;
    }

    /**
     * RT-CHANNEL-AUTH-001 + RT-CHANNEL-AUTH-002 + RT-RECONNECT-001 — subscribe to a
     * tenant-scoped topic as a {@code text/event-stream}.
     *
     * <p>RT-RECONNECT-001: a {@code Last-Event-ID} request header (W3C SSE §9.2)
     * resumes the stream gap-free from the retention window. Audience membership is the
     * caller's own id (single-tenant reference; a fork layers roles/groups on top).
     */
    @GetMapping(value = "/topics/{tenantScope}/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String tenantScope,
                                @PathVariable String topic,
                                @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId,
                                Authentication authentication) {
        requireScopeMatch(tenantScope);
        requireKnownTopic(topic);
        String callerId = authentication.getName();
        Set<String> membership = Set.of(callerId);
        return channelService.subscribe(tenantScope, topic, callerId, membership, lastEventId);
    }

    /**
     * Test/demo publish hook (RT-FANOUT-001) — fan-out a frame to the topic, addressed
     * to {@code audience}. Audience filtering happens in
     * {@link RealtimeChannelService#publish} BEFORE emit, never at the client.
     * Same tenant-scope gate as subscribe (RT-CHANNEL-AUTH-002).
     */
    @PostMapping(value = "/topics/{tenantScope}/{topic}/publish")
    public Map<String, Object> publish(@PathVariable String tenantScope,
                                       @PathVariable String topic,
                                       @RequestBody PublishRequest request,
                                       Authentication authentication) {
        requireScopeMatch(tenantScope);
        requireKnownTopic(topic);
        Set<String> audience = request.audience() == null ? Set.of() : Set.copyOf(request.audience());
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        channelService.publish(tenantScope, topic, audience, request.eventId(), payload);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantScope", tenantScope);
        body.put("topic", topic);
        body.put("eventId", request.eventId());
        body.put("audienceSize", audience.size());
        return body;
    }

    /** RT-CHANNEL-AUTH-002 — reject a subscribe/publish whose scope != the resolved tenant. */
    private void requireScopeMatch(String tenantScope) {
        if (!RESOLVED_TENANT_SCOPE.equals(tenantScope)) {
            throw new CrossTenantSubscriptionException(tenantScope);
        }
    }

    /**
     * RT-OBSERVABILITY-001 (metric-cardinality bound) — reject a subscribe/publish to a topic
     * NOT in the bounded allowlist BEFORE any registry entry or Micrometer series is created,
     * so an attacker cannot explode the {@code channel} label cardinality with infinite unique
     * topics (memory / Prometheus DoS).
     */
    private void requireKnownTopic(String topic) {
        if (!properties.isKnownTopic(topic)) {
            throw new UnknownTopicException(topic);
        }
    }

    /** RT-FANOUT-001 demo publish body. */
    public record PublishRequest(long eventId, Set<String> audience, Map<String, Object> payload) {}
}
