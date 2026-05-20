package com.ax.template.authblueprint.webhook;

import org.springframework.stereotype.Component;

/**
 * Simple glob-style event-type matcher: {@code "order.*"} matches
 * {@code "order.created"} but not {@code "user.created"}.
 * <p>
 * Trace: blueprints/webhook-manifest.yaml#emit.dispatch_semantics —
 * "fan-out to ALL active endpoints whose event_filter matches eventType".
 */
@Component
public class EventTypeMatcher {

    /**
     * @param filter    endpoint's {@code event_filter}; {@code null} or empty matches everything
     * @param eventType the event type being emitted (e.g. {@code "order.created"})
     * @return {@code true} when the filter matches
     */
    public boolean matches(String filter, String eventType) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        if (filter.equals(eventType)) {
            return true;
        }
        // Glob: "order.*" matches anything starting with "order."
        if (filter.endsWith(".*")) {
            String prefix = filter.substring(0, filter.length() - 1); // keep the trailing "."
            return eventType != null && eventType.startsWith(prefix);
        }
        return false;
    }
}
