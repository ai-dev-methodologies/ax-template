package com.ax.template.authblueprint.common;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link RequestBodySizeLimitFilter} for EVERY request at
 * {@link Ordered#HIGHEST_PRECEDENCE} so the transport body cap runs before the Spring Security
 * chain and before the {@code DispatcherServlet} — i.e. before any message converter can buffer
 * a body onto the heap.
 *
 * <p>The filter is instantiated directly (not a {@code @Component}) so Spring Boot does not ALSO
 * auto-register it a second time via a default {@link FilterRegistrationBean} (same pattern the
 * ratelimit / api-key configs use to keep a single, ordered registration).
 */
@Configuration
public class RequestBodySizeLimitConfig {

    @Bean
    public FilterRegistrationBean<RequestBodySizeLimitFilter> requestBodySizeLimitFilterRegistration() {
        FilterRegistrationBean<RequestBodySizeLimitFilter> registration =
                new FilterRegistrationBean<>(new RequestBodySizeLimitFilter());
        registration.addUrlPatterns("/*");
        // Before Spring Security (DEFAULT_FILTER_ORDER = -100) and every application filter, so an
        // oversized body is rejected at the outermost edge — nothing downstream ever buffers it.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
