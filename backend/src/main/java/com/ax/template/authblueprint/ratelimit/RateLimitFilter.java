package com.ax.template.authblueprint.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window per-key rate limiter. Backed by Caffeine; entries auto-expire after
 * {@code windowMillis} so a fresh bucket is allocated on the next request.
 *
 * Scope: applied via {@link org.springframework.boot.web.servlet.FilterRegistrationBean}
 * to URL patterns listed in {@code ratelimit-manifest.yaml#policy.applies_to}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> buckets;
    private final ConcurrentMap<String, AtomicInteger> bucketView;

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
        this.buckets = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(properties.windowMillis()))
            .maximumSize(10_000)
            .build();
        this.bucketView = buckets.asMap();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = extractKey(request);

        if (key == null) {
            switch (properties.onMissingKey()) {
                case REJECT -> {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required header: " + properties.keyHeader());
                    return;
                }
                case ALLOW -> {
                    chain.doFilter(request, response);
                    return;
                }
                case ANONYMOUS -> key = "__anonymous__";
            }
        }

        // computeIfAbsent on ConcurrentMap view is atomic per-key (Caffeine doc — asMap view)
        AtomicInteger counter = bucketView.computeIfAbsent(key, k -> new AtomicInteger());
        int hits = counter.incrementAndGet();

        if (hits > properties.maxPerWindow()) {
            long retryAfterSeconds = Math.max(1L,
                (properties.windowMillis() + 999L) / 1000L); // ceil to seconds, never 0
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            writeError(response, 429, "Rate limit exceeded for key");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractKey(HttpServletRequest request) {
        String raw = request.getHeader(properties.keyHeader());
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
