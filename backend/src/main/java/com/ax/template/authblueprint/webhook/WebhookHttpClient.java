package com.ax.template.authblueprint.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Thin HTTP client wrapper. Returns a {@link Response} record so the caller can
 * treat HTTP errors and network errors uniformly.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-EMIT-002 — POST JSON body with {@code Content-Type: application/json}.</li>
 *   <li>WEBHOOK-SIGN-001 / WEBHOOK-SIGN-002 — headers added by the caller, not by this class.</li>
 *   <li>WEBHOOK-RETRY-002 — {@code X-Webhook-Delivery-Id} stays stable across retries
 *       because it is supplied by the caller from {@link WebhookDelivery#getId()}.</li>
 * </ul>
 */
@Component
public class WebhookHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestTemplate restTemplate;

    @Autowired
    public WebhookHttpClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setReadTimeout(READ_TIMEOUT)
            .build();
    }

    /** Constructor for tests that want to supply a stub {@link RestTemplate}. */
    WebhookHttpClient(RestTemplate restTemplate) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
    }

    /**
     * POST the body to the URL with the supplied headers. Network errors are
     * returned as {@link Response} with {@code statusCode=null} so the retry
     * policy can classify them uniformly.
     */
    public Response post(String url, String body, Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        headers.forEach(httpHeaders::add);

        RequestEntity<String> req = RequestEntity
            .method(HttpMethod.POST, URI.create(url))
            .headers(httpHeaders)
            .body(body);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
            return new Response(resp.getStatusCode().value(), null);
        } catch (RestClientResponseException ex) {
            return new Response(ex.getStatusCode().value(), ex.getMessage());
        } catch (ResourceAccessException ex) {
            // network timeout / connection refused / DNS failure
            return new Response(null, ex.getMessage());
        } catch (RuntimeException ex) {
            return new Response(null, ex.getMessage());
        }
    }

    /** {@code statusCode} is {@code null} on network errors. */
    public record Response(Integer statusCode, String errorMessage) {
        public boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }
}
