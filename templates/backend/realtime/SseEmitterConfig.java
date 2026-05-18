/**
 * @ax-template-meta
 * template_id: backend/realtime/SseEmitterConfig
 * layer: backend-cross-cutting
 * provenance_class: internal_design
 * opt_in_via: ax.realtime.sse.enabled=true (blueprints/realtime-policy-manifest.yaml)
 * serverless_safe: false
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — SseEmitter (Web on Servlet Stack §1.11)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html#mvc-ann-async-sse"
 *   - source_type: external
 *     citation: "MDN Web Docs — Server-Sent Events: Using server-sent events"
 *     url: "https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events"
 * usage: |
 *   1. Enable via application.yaml: ax.realtime.sse.enabled=true
 *   2. Replace 'com.example.app' with your base package.
 *   3. Inject SseEmitterConfig into controllers that serve SSE streams.
 *   4. NOT for serverless runtimes (Vercel, Lambda) — use polling default instead.
 *      See blueprints/realtime-policy-manifest.yaml and README ## Serverless Deployment.
 *
 * ## Serverless Deployment
 * SSE holds an HTTP connection open until timeout or disconnect.
 * Serverless platforms enforce function timeouts (10–60s on Vercel, up to 15min on Lambda).
 * A client reconnects immediately on close, creating reconnect loops and billing spikes.
 * Use the polling default (ax.realtime.sse.enabled=false) on serverless platforms.
 */
package com.example.app.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

/**
 * SSE emitter registry — manages active {@link SseEmitter} connections per user.
 *
 * <p>Opt-in only. Activated when {@code ax.realtime.sse.enabled=true}.
 *
 * <p>Thread-safe: uses {@link CopyOnWriteArrayList} per user to allow concurrent
 * send and remove without structural modification conflicts.
 *
 * <p>Lifecycle per emitter:
 * <pre>
 *   subscribe() → SseEmitter created and registered
 *     → events arrive via send(userId, event)
 *     → emitter.onCompletion / onTimeout / onError → unregister()
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "ax.realtime.sse.enabled", havingValue = "true")
public class SseEmitterConfig {

    /** Timeout before client must reconnect (5 minutes). Avoids zombie connections. */
    private static final long TIMEOUT_MS = 300_000L;

    /** Maximum active SSE connections per user to guard against tab proliferation. */
    private static final int MAX_CONNECTIONS_PER_USER = 3;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new {@link SseEmitter} for the given user.
     *
     * <p>If the user already has {@link #MAX_CONNECTIONS_PER_USER} connections,
     * the oldest is completed (closed) before the new one is registered.
     *
     * @param userId  principal identifier (from JWT subject)
     * @param topic   event topic to filter on (e.g. "notification", "audit")
     * @return        a new SseEmitter — return this from your controller method
     */
    public SseEmitter subscribe(String userId, String topic) {
        var emitter = new SseEmitter(TIMEOUT_MS);
        var userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // Evict oldest connection if at cap
        if (userEmitters.size() >= MAX_CONNECTIONS_PER_USER) {
            var oldest = userEmitters.get(0);
            oldest.complete();
            userEmitters.remove(oldest);
        }

        userEmitters.add(emitter);

        emitter.onCompletion(() -> unregister(userId, emitter));
        emitter.onTimeout(() -> unregister(userId, emitter));
        emitter.onError(ex -> unregister(userId, emitter));

        // Send initial heartbeat so the client knows the stream is open
        sendHeartbeat(emitter, topic);

        return emitter;
    }

    /**
     * Broadcasts an event to all active emitters for the given user.
     *
     * <p>Failed sends (completed/timed-out emitters) are removed silently.
     *
     * @param userId   target user
     * @param event    SSE event to send
     */
    public void send(String userId, SseEmitter.SseEventBuilder event) {
        var userEmitters = emitters.getOrDefault(userId, List.of());
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(event);
            } catch (IOException ex) {
                dead.add(emitter);
            }
        }

        dead.forEach(d -> unregister(userId, d));
    }

    /**
     * Returns the current active connection count for a user.
     * Useful for metrics and debugging.
     */
    public int activeConnectionCount(String userId) {
        return emitters.getOrDefault(userId, List.of()).size();
    }

    /** Total active connections across all users — for {@code sse.active_connections} gauge. */
    public int totalActiveConnections() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private void unregister(String userId, SseEmitter emitter) {
        var userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    private void sendHeartbeat(SseEmitter emitter, String topic) {
        try {
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("{\"topic\":\"" + topic + "\",\"ts\":" + System.currentTimeMillis() + "}"));
        } catch (IOException ex) {
            // Emitter closed before heartbeat — ignore; lifecycle callbacks handle cleanup
        }
    }
}
