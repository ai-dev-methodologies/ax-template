package com.ax.template.authblueprint.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(properties));
        // Servlet url-pattern mapping (NOT Ant-style) — "/api/ratelimit/*" would recursively match
        // every nested path, including the RATELIMIT-5 "/api/ratelimit/anon/*" filter's own path
        // below. Scoped to the exact demo endpoint so the two filters never both see one request.
        registration.addUrlPatterns("/api/ratelimit/ping");
        // Run before Spring Security filter chain so quota check is cheap and never depends on auth
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * RATELIMIT-5 — a second, independently-bucketed filter for UNAUTHENTICATED endpoints keyed
     * on the trusted-proxy-resolved client IP rather than X-API-Key. Registered on the sibling
     * "/api/ratelimit/anon/*" pattern — disjoint from the exact "/api/ratelimit/ping" pattern above,
     * so the two filters never see the same request. Quota values are copied from the bound
     * "ratelimit" manifest so both demos share one policy ceiling; only the key strategy differs.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitAnonFilterRegistration(
            RateLimitProperties properties) {
        RateLimitProperties anonProperties = new RateLimitProperties(
            properties.maxPerWindow(), properties.windowMillis(), properties.keyHeader(),
            properties.onMissingKey(), RateLimitProperties.KeyStrategy.IP, null);
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(anonProperties));
        registration.addUrlPatterns("/api/ratelimit/anon/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        return registration;
    }
}
