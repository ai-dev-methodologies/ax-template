package com.ax.template.authblueprint.ratelimit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Reference endpoint for ratelimit-l0 compliance verification. Production
 * projects do not need to ship this; only the filter is required.
 */
@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitPingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    /**
     * RATELIMIT-5 demo endpoint — unauthenticated, keyed by the trusted-proxy-resolved client IP
     * (see {@link RateLimitConfig#rateLimitAnonFilterRegistration}), never X-API-Key.
     */
    @GetMapping("/anon/ping")
    public Map<String, String> anonPing() {
        return Map.of("status", "ok");
    }
}
