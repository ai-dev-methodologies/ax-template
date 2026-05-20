package com.ax.template.authblueprint.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public emit entry-point: fan-out an event to all matching active endpoints.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-EMIT-002 — iterates active endpoints whose {@code event_filter}
 *       matches {@code eventType}, creates one {@link WebhookDelivery} row per
 *       endpoint BEFORE the HTTP call, then dispatches each attempt.</li>
 *   <li>blueprints/webhook-manifest.yaml#emit.dispatch_semantics</li>
 * </ul>
 *
 * <p>Persistence-before-send is required: the delivery row must exist before the
 * first attempt so that crash-during-send still leaves an auditable record that
 * the retry scheduler can pick up.
 */
@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSender sender;
    private final EventTypeMatcher matcher;

    public WebhookDispatcher(WebhookEndpointRepository endpointRepository,
                             WebhookDeliveryRepository deliveryRepository,
                             WebhookSender sender,
                             EventTypeMatcher matcher) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.sender = sender;
        this.matcher = matcher;
    }

    /**
     * Emit an event. Returns the list of {@link WebhookDelivery} rows AFTER each
     * has been attempted once. The persistence-before-send invariant
     * (WEBHOOK-EMIT-002) is preserved by running {@link #enqueueMatching} in a
     * separate {@code REQUIRES_NEW} transaction.
     */
    public List<WebhookDelivery> emit(String eventType, String body) {
        List<WebhookDelivery> dispatched = new ArrayList<>();
        List<UUID> deliveryIds = enqueueMatching(eventType, body);
        for (UUID id : deliveryIds) {
            try {
                dispatched.add(sender.attempt(id));
            } catch (RuntimeException ex) {
                log.error("webhook: emit attempt threw eventType={} deliveryId={} error={}",
                    eventType, id, ex.getMessage(), ex);
                deliveryRepository.findById(id).ifPresent(dispatched::add);
            }
        }
        return dispatched;
    }

    /**
     * Phase 1 — WEBHOOK-EMIT-002 persistence-before-send: insert one row per
     * matching active endpoint. Returns the IDs of the newly enqueued rows.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> enqueueMatching(String eventType, String body) {
        List<UUID> ids = new ArrayList<>();
        for (WebhookEndpoint ep : endpointRepository.findByActiveTrue()) {
            if (!matcher.matches(ep.getEventFilter(), eventType)) {
                continue;
            }
            WebhookDelivery saved = deliveryRepository.save(
                WebhookDelivery.enqueue(ep.getId(), eventType, body));
            ids.add(saved.getId());
        }
        return ids;
    }
}
