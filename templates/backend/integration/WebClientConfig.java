/**
 * @ax-template-meta
 * template_id: backend/integration/WebClientConfig
 * layer: backend-infrastructure
 * domain: integration
 * anchors_rule: resilient-http-client-required.md (PRACTICES-HTTP-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Resilience4j Circuit Breaker — when the circuit is OPEN, calls fail immediately without waiting for the timeout; this prevents cascading failures to downstream services"
 *     url: "https://resilience4j.readme.io/docs/circuitbreaker"
 *   - source_type: external
 *     citation: "Spring Framework RestClient (6.1+) — fluent synchronous HTTP client; successor to RestTemplate with a modern API similar to WebClient"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add resilience4j-spring-boot3 to your build.gradle.kts:
 *     implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
 *   Inject RestClient and registries into your API template classes.
 */
package com.example.app.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP client configuration with Resilience4j Circuit Breaker and Retry.
 *
 * <p>Implements PRACTICES-HTTP-002: all outbound HTTP calls must use
 * RestClient (not RestTemplate) and be wrapped with a circuit breaker.
 */
@Configuration
public class WebClientConfig {

    // ── Timeouts ───────────────────────────────────────────────────────────────
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT    = Duration.ofSeconds(5);

    // ── Circuit Breaker ────────────────────────────────────────────────────────
    private static final int      CB_SLIDING_WINDOW = 10;
    private static final float    CB_FAILURE_RATE   = 50f;
    private static final Duration CB_WAIT_OPEN      = Duration.ofSeconds(30);

    // ── Retry ─────────────────────────────────────────────────────────────────
    private static final int      RETRY_MAX         = 3;
    private static final Duration RETRY_INITIAL     = Duration.ofMillis(200);
    private static final double   RETRY_MULTIPLIER  = 2.0;

    @Bean
    public RestClient integrationRestClient() {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(CB_SLIDING_WINDOW)
                .failureRateThreshold(CB_FAILURE_RATE)
                .waitDurationInOpenState(CB_WAIT_OPEN)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(RETRY_MAX)
                .intervalFunction(attempt ->
                        (long) (RETRY_INITIAL.toMillis() * Math.pow(RETRY_MULTIPLIER, attempt - 1)))
                .retryExceptions(Exception.class)
                .build();
        return RetryRegistry.of(config);
    }
}
