/**
 * @ax-template-meta
 * template_id: backend/realtime/SseSubscription
 * layer: backend-cross-cutting
 * provenance_class: internal_design
 * opt_in_via: ax.realtime.sse.enabled=true (blueprints/realtime-policy-manifest.yaml)
 * serverless_safe: false
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — SseEmitter (Web on Servlet Stack §1.11)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse"
 *   - source_type: external
 *     citation: "WHATWG — Server-Sent Events: event-stream format"
 *     url: "https://html.spec.whatwg.org/multipage/server-sent-events.html"
 * usage: |
 *   Value object representing a single user's SSE subscription context.
 *   Created by SseEmitterConfig.subscribe(); passed to RealtimeEventBus for routing.
 *   Opt-in: requires ax.realtime.sse.enabled=true.
 */
package com.example.app.realtime;

import java.time.Instant;

/**
 * Immutable descriptor of a single SSE subscription.
 *
 * <p>Carries the user identifier, subscribed topic, and subscription timestamp
 * for routing and observability (MDC tagging, metrics labels).
 *
 * @param userId    JWT subject — the authenticated user receiving events
 * @param topic     event stream topic (e.g. "notification", "audit", "payment")
 * @param createdAt wall-clock time the subscription was established
 */
public record SseSubscription(String userId, String topic, Instant createdAt) {

    /**
     * Factory — creates a subscription with the current timestamp.
     *
     * @param userId  JWT subject
     * @param topic   event topic
     * @return        new SseSubscription
     */
    public static SseSubscription of(String userId, String topic) {
        return new SseSubscription(userId, topic, Instant.now());
    }

    /** Returns true if this subscription matches the given event topic. */
    public boolean matchesTopic(String eventTopic) {
        return this.topic.equals(eventTopic);
    }
}
