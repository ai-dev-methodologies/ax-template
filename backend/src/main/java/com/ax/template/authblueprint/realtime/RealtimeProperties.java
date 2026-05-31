package com.ax.template.authblueprint.realtime;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Manifest-declared realtime policy values (RT-OBSERVABILITY-001 bounded topic set +
 * RT-BACKPRESSURE-001 queue threshold + RT-RECONNECT-001 retention window). Mirrors the
 * policy anchors in blueprints/realtime-policy-manifest.yaml so a fork-receiver tunes the
 * values in {@code application.yaml} (prefix {@code ax.realtime}) rather than hardcoding
 * them in the service.
 *
 * <p>Defaults are intentionally small + deterministic so the black-box compliance test
 * can trigger backpressure + replay without real-time sleeps. A production fork raises
 * {@code queueThreshold} (the manifest sketches a 100-frame ballpark) and widens
 * {@code retentionWindow} to the recipe's declared {@code realtime_retention_window_seconds}.
 */
@ConfigurationProperties(prefix = "ax.realtime")
public class RealtimeProperties {

    /**
     * RT-OBSERVABILITY-001 — the BOUNDED set of subscribable topic names. The {@code topic}
     * path segment becomes the {@code channel} Micrometer label
     * ({@link RealtimeMetrics#activeSubscribers}); accepting an unbounded path variable would
     * let an attacker subscribe to infinite unique topics and explode the metric time-series
     * (memory / Prometheus DoS). The controller rejects a subscribe/publish to a topic NOT in
     * this list with 404 BEFORE any registry entry or metric series is created. Default mirrors
     * blueprints/realtime-policy-manifest.yaml {@code sse.topics}; a fork edits this list to its
     * own known topic set in {@code application.yaml} ({@code ax.realtime.topics}).
     */
    private List<String> topics = List.of("notification", "audit", "payment", "system");

    /**
     * RT-BACKPRESSURE-001 — bounded per-emitter pending-frame depth. When a slow
     * consumer's queue reaches this depth the server completes-with-error and
     * disconnects rather than buffering unboundedly (Project Reactor backpressure
     * concept: bounded buffer + drop-by-disconnect policy).
     */
    private int queueThreshold = 50;

    /**
     * RT-RECONNECT-001 — per-topic retention ring buffer size, keyed by the monotonic
     * event id. A reconnect with {@code Last-Event-ID} replays every retained event
     * whose id is greater than the supplied cursor; events older than the window are
     * not replayable (the catalog refuses fire-and-forget but bounds memory).
     */
    private int retentionEvents = 100;

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    /** True iff {@code topic} is in the bounded allowlist (RT-OBSERVABILITY-001 cardinality bound). */
    public boolean isKnownTopic(String topic) {
        return topics.contains(topic);
    }

    public int getQueueThreshold() {
        return queueThreshold;
    }

    public void setQueueThreshold(int queueThreshold) {
        this.queueThreshold = queueThreshold;
    }

    public int getRetentionEvents() {
        return retentionEvents;
    }

    public void setRetentionEvents(int retentionEvents) {
        this.retentionEvents = retentionEvents;
    }
}
