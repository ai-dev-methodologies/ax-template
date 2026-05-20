package com.ax.template.authblueprint.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates one delivery attempt: signs the request, POSTs it, classifies
 * the result, and updates the {@link WebhookDelivery} row.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-SIGN-001 / WEBHOOK-SIGN-002 — adds the three webhook headers
 *       around the canonical signed input {@code "<timestamp>.<body>"}.</li>
 *   <li>WEBHOOK-RETRY-001 — retriable failures call
 *       {@link RetryPolicy#nextAttemptAt} to schedule the next attempt.</li>
 *   <li>WEBHOOK-RETRY-002 — every attempt reuses {@code delivery.id} verbatim
 *       in the {@code X-Webhook-Delivery-Id} header.</li>
 *   <li>WEBHOOK-DEAD-LETTER-001 — exhausted retry budget or permanent 4xx
 *       transitions the delivery to {@link WebhookDeliveryStatus#FAILED_PERMANENT}.</li>
 *   <li>WEBHOOK-CIRCUIT-001 — every terminal transition triggers
 *       {@link CircuitBreakerPolicy#evaluate}.</li>
 * </ul>
 */
@Service
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final WebhookHttpClient httpClient;
    private final HmacSigner hmacSigner;
    private final RetryPolicy retryPolicy;
    private final CircuitBreakerPolicy circuitBreaker;

    public WebhookSender(WebhookDeliveryRepository deliveryRepository,
                         WebhookEndpointRepository endpointRepository,
                         WebhookHttpClient httpClient,
                         HmacSigner hmacSigner,
                         RetryPolicy retryPolicy,
                         CircuitBreakerPolicy circuitBreaker) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
        this.httpClient = httpClient;
        this.hmacSigner = hmacSigner;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Run one attempt against the given delivery. The delivery row is mutated in place
     * and saved; if the attempt is terminal, the circuit-breaker is evaluated.
     */
    @Transactional
    public WebhookDelivery attempt(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new WebhookDeliveryNotFoundException(deliveryId));
        WebhookEndpoint endpoint = endpointRepository.findById(delivery.getEndpointId())
            .orElseThrow(() -> new WebhookEndpointNotFoundException(delivery.getEndpointId()));

        Instant now = Instant.now();
        long timestamp = now.getEpochSecond();
        String signature = hmacSigner.sign(endpoint.getSigningSecret(), timestamp, delivery.getBody());

        Map<String, String> headers = Map.of(
            HmacSigner.HEADER_TIMESTAMP, Long.toString(timestamp),
            HmacSigner.HEADER_SIGNATURE, signature,
            HmacSigner.HEADER_DELIVERY_ID, delivery.getId().toString()
        );

        WebhookHttpClient.Response response = httpClient.post(endpoint.getUrl(), delivery.getBody(), headers);
        applyResponse(delivery, response, now);
        WebhookDelivery saved = deliveryRepository.save(delivery);

        if (saved.getStatus() == WebhookDeliveryStatus.SUCCEEDED
            || saved.getStatus() == WebhookDeliveryStatus.FAILED_PERMANENT) {
            circuitBreaker.evaluate(endpoint.getId());
        }
        return saved;
    }

    private void applyResponse(WebhookDelivery delivery, WebhookHttpClient.Response response, Instant now) {
        if (response.isSuccessful()) {
            delivery.markSucceeded(response.statusCode(), now);
            log.info("webhook: delivered deliveryId={} status={}",
                delivery.getId(), response.statusCode());
            return;
        }
        boolean retriable = retryPolicy.isRetriable(response.statusCode());
        int nextAttemptIndex = delivery.getAttemptCount() + 1; // we are about to record this attempt
        if (!retriable) {
            delivery.markFailedPermanent(response.statusCode(), response.errorMessage(), now);
            log.error("webhook: PERMANENT failure deliveryId={} status={}",
                delivery.getId(), response.statusCode());
            return;
        }
        if (retryPolicy.isExhausted(nextAttemptIndex)) {
            delivery.markFailedPermanent(response.statusCode(), response.errorMessage(), now);
            log.error("webhook: DEAD-LETTER deliveryId={} status={} attempts={}",
                delivery.getId(), response.statusCode(), nextAttemptIndex);
            return;
        }
        Instant nextAttemptAt = retryPolicy.nextAttemptAt(nextAttemptIndex, now);
        delivery.markRetry(response.statusCode(), response.errorMessage(), now, nextAttemptAt);
        log.warn("webhook: retry scheduled deliveryId={} status={} attempt={} nextAttemptAt={}",
            delivery.getId(), response.statusCode(), nextAttemptIndex, nextAttemptAt);
    }
}
