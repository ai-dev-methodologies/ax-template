/**
 * @ax-template-meta
 * template_id: backend/realtime/RealtimeEventBus
 * layer: backend-cross-cutting
 * anchors_rule: realtime-single-protocol-declared.md
 * provenance_class: internal_design
 * opt_in_via: ax.realtime.sse.enabled=true (blueprints/realtime-policy-manifest.yaml)
 * serverless_safe: false
 * evidence:
 *   - source_type: external
 *     citation: "Microservices.io — Transactional Outbox Pattern"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "Spring Framework Reference — SseEmitter (Web on Servlet Stack §1.11)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse"
 * usage: |
 *   Central event bus that bridges the DB outbox (RealtimeOutboxRelay) to
 *   active SSE emitters (SseEmitterConfig). Opt-in: requires ax.realtime.sse.enabled=true.
 *
 *   Flow:
 *     Domain service saves row to realtime_outbox table
 *     → RealtimeOutboxRelay polls for PENDING rows (scheduled every 1s)
 *     → RealtimeOutboxRelay calls RealtimeEventBus.publish(event)
 *     → RealtimeEventBus fans out via SseEmitterConfig.send(userId, event)
 *       OR no-ops if SSE not opted in (default polling path)
 *
 *   If SSE not enabled, this bean is a no-op stub — calling publish() returns
 *   immediately without error, so callers are transport-agnostic.
 */
package com.example.app.realtime;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

/**
 * Transport-agnostic event bus.
 *
 * <p>When SSE is opted in ({@code ax.realtime.sse.enabled=true}), routes events to
 * {@link SseEmitterConfig}. When SSE is disabled (default — polling), {@link #publish}
 * is a no-op: the client polls via TanStack Query on its own schedule.
 *
 * <p>Domain services call {@link #publish} without knowing which transport is active —
 * the decision is deferred to the active transport configuration.
 */
@Component
public class RealtimeEventBus {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventBus.class);

    private final Optional<SseEmitterConfig> sseConfig;
    private final Optional<MeterRegistry> meterRegistry;

    public RealtimeEventBus(
            @Autowired(required = false) SseEmitterConfig sseConfig,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.sseConfig = Optional.ofNullable(sseConfig);
        this.meterRegistry = Optional.ofNullable(meterRegistry);
    }

    /**
     * Publishes a realtime event to the appropriate transport.
     *
     * <p>If SSE is enabled, fans out to all active emitters for the target user.
     * If SSE is disabled (default), this method returns immediately — clients poll.
     *
     * @param userId  target user (JWT subject)
     * @param topic   event topic (matches SseSubscription.topic)
     * @param payload JSON payload string
     */
    public void publish(String userId, String topic, String payload) {
        MDC.put("sse.topic", topic);
        MDC.put("sse.user_id", userId);

        try {
            if (sseConfig.isPresent()) {
                var event = SseEmitter.event()
                    .name(topic)
                    .data(payload);
                sseConfig.get().send(userId, event);

                meterRegistry.ifPresent(r ->
                    r.counter("realtime.event_bus_relay_total",
                        "topic", topic,
                        "transport", "sse"
                    ).increment()
                );

                log.debug("SSE event published: topic={} userId={}", topic, userId);
            } else {
                // Default path: polling. No-op — client will fetch on next interval.
                log.trace("SSE disabled — polling default active for topic={} userId={}", topic, userId);

                meterRegistry.ifPresent(r ->
                    r.counter("realtime.event_bus_relay_total",
                        "topic", topic,
                        "transport", "polling"
                    ).increment()
                );
            }
        } finally {
            MDC.remove("sse.topic");
            MDC.remove("sse.user_id");
        }
    }

    /**
     * Returns true if SSE transport is active (opted in), false if polling is default.
     * Useful for health endpoints and observability checks.
     */
    public boolean isSseActive() {
        return sseConfig.isPresent();
    }
}
