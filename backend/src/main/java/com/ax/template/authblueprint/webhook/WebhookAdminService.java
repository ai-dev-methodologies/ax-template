package com.ax.template.authblueprint.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin-only operations on existing webhook deliveries.
 * <p>
 * Sole orchestrator of {@link WebhookDeliveryRepository} for the admin surface:
 * the controller routes all delivery reads + the replay write through this
 * service so it never touches the repository directly (layer-boundary discipline).
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-DEAD-LETTER-001 — {@link #listDeliveries(WebhookDeliveryStatus, int, int)}
 *       exposes retained rows (e.g. {@code FAILED_PERMANENT}) for admin inspection.</li>
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
     * WEBHOOK-DEAD-LETTER-001 — page of deliveries for the given status, newest first.
     */
    @Transactional(readOnly = true)
    public Page<WebhookDelivery> listDeliveries(WebhookDeliveryStatus status, int page, int size) {
        return deliveryRepository
            .findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
    }

    /**
     * Single delivery lookup. Throws {@link WebhookDeliveryNotFoundException} when absent.
     */
    @Transactional(readOnly = true)
    public WebhookDelivery getDelivery(UUID id) {
        return deliveryRepository.findById(id)
            .orElseThrow(() -> new WebhookDeliveryNotFoundException(id));
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
