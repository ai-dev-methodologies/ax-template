package com.ax.template.authblueprint.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from `ratelimit.*` properties. Source of truth is
 * `blueprints/ratelimit-manifest.yaml#policy` — keep keys aligned.
 */
@ConfigurationProperties(prefix = "ratelimit")
public record RateLimitProperties(
    int maxPerWindow,
    long windowMillis,
    String keyHeader,
    OnMissingKey onMissingKey,
    KeyStrategy keyStrategy,
    Integer trustedProxyDepth
) {
    public RateLimitProperties {
        if (maxPerWindow <= 0) {
            throw new IllegalArgumentException("ratelimit.max-per-window must be > 0");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("ratelimit.window-millis must be > 0");
        }
        if (keyHeader == null || keyHeader.isBlank()) {
            throw new IllegalArgumentException("ratelimit.key-header must be set");
        }
        if (onMissingKey == null) {
            onMissingKey = OnMissingKey.REJECT;
        }
        if (keyStrategy == null) {
            keyStrategy = KeyStrategy.HEADER;
        }
        if (trustedProxyDepth == null || trustedProxyDepth < 0) {
            // RATELIMIT-5 default: trust exactly one hop (a single reverse proxy in front
            // of this app) when unconfigured — never an unbounded or negative depth.
            trustedProxyDepth = 1;
        }
    }

    public enum OnMissingKey { REJECT, ALLOW, ANONYMOUS }

    /** RATELIMIT-5 — HEADER (the existing X-API-Key demo) or IP (trusted-proxy XFF resolution). */
    public enum KeyStrategy { HEADER, IP }
}
