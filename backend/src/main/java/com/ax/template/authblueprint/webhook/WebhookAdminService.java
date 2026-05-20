package com.ax.template.authblueprint.webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin-only operations on existing webhook deliveries.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-DEAD-LETTER-002 — {@link #replay(UUID)} creates a NEW delivery row
 *       with a fresh {@code id} (= fresh {@code X-Webhook-Delivery-Id}) and
 *       {@code attempt_count=1}. The original {@code FAILED_PERMANENT} row is
 *       left intact for trail integrity.</li>
 * </ul>
 */
@Service
public class WebhookAdminService {

    private final WebhookDeliveryRepository deliveryRepository;

    public WebhookAdminService(WebhookDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * WEBHOOK-DEAD-LETTER-002 — re-enqueue a {@code FAILED_PERMANENT} delivery.
     * Returns the freshly created replay row (NOT yet sent).
     */
    @Transactional
    public WebhookDelivery replay(UUID originalDeliveryId) {
        WebhookDelivery original = deliveryRepository.findById(originalDeliveryId)
            .orElseThrow(() -> new WebhookDeliveryNotFoundException(originalDeliveryId));
        WebhookDelivery replay = WebhookDelivery.enqueue(
            original.getEndpointId(),
            original.getEventType(),
            original.getBody()
        );
        return deliveryRepository.save(replay);
    }
}
