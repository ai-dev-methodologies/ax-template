/**
 * @ax-template-meta
 * template_id: backend/integration/ExternalApiTemplate
 * layer: backend-application
 * domain: integration
 * anchors_rule: resilient-http-client-required.md (PRACTICES-HTTP-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Resilience4j — combining CircuitBreaker and Retry: wrap the retry around the circuit breaker so retries only happen while the circuit is CLOSED; once the circuit opens, retries stop immediately"
 *     url: "https://resilience4j.readme.io/docs/getting-started-3#combining-decorators"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Inject as a Spring bean. Use get/post methods in your service classes.
 *   The circuit breaker name maps to configuration properties in application.yml.
 */
package com.example.app.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Typed HTTP wrapper that decorates all outbound calls with CircuitBreaker + Retry.
 *
 * <p>Returns {@link Optional#empty()} on any failure (circuit open, retry exhausted,
 * HTTP error). Callers decide the fallback — no exceptions escape.
 */
@Component
public class ExternalApiTemplate {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiTemplate.class);
    private static final String DEFAULT_CB = "external-api";

    private final RestClient restClient;
    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    public ExternalApiTemplate(RestClient integrationRestClient,
                               CircuitBreakerRegistry circuitBreakerRegistry,
                               RetryRegistry retryRegistry) {
        this.restClient   = integrationRestClient;
        this.cbRegistry   = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public <T> Optional<T> get(String url, Class<T> responseType) {
        return execute(DEFAULT_CB, () -> restClient.get()
                .uri(url)
                .retrieve()
                .body(responseType));
    }

    public <T> Optional<T> get(String url, ParameterizedTypeReference<T> responseType) {
        return execute(DEFAULT_CB, () -> restClient.get()
                .uri(url)
                .retrieve()
                .body(responseType));
    }

    public <T> Optional<T> post(String url, Object body, Class<T> responseType) {
        return execute(DEFAULT_CB, () -> restClient.post()
                .uri(url)
                .body(body)
                .retrieve()
                .body(responseType));
    }

    // ── Decoration ─────────────────────────────────────────────────────────────

    private <T> Optional<T> execute(String circuitBreakerName, Supplier<T> call) {
        CircuitBreaker cb    = cbRegistry.circuitBreaker(circuitBreakerName);
        Retry          retry = retryRegistry.retry(circuitBreakerName);

        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(cb, call));

        try {
            return Optional.ofNullable(decorated.get());
        } catch (Exception ex) {
            log.warn("External API call failed (circuit={}): {}", circuitBreakerName, ex.getMessage());
            return Optional.empty();
        }
    }
}
