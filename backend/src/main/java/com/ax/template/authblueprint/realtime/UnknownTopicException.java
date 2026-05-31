package com.ax.template.authblueprint.realtime;

/**
 * RT-OBSERVABILITY-001 (metric-cardinality bound) — a subscribe/publish to a topic NOT in
 * the bounded {@link RealtimeProperties#getTopics() topic allowlist}. Mapped to
 * {@code 404 Not Found} by {@link RealtimeProblemAdvice}. Rejecting BEFORE any registry
 * entry or Micrometer series is created stops an attacker from exploding the {@code channel}
 * label cardinality (memory / Prometheus DoS) by subscribing to infinite unique topics.
 */
public class UnknownTopicException extends RuntimeException {

    public static final String CODE = "REALTIME_UNKNOWN_TOPIC";

    public UnknownTopicException(String requestedTopic) {
        super("unknown realtime topic (not in the bounded allowlist): requested=" + requestedTopic);
    }
}
