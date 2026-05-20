package com.ax.template.authblueprint.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByStatusOrderByCreatedAtDesc(WebhookDeliveryStatus status, Pageable pageable);

    /**
     * Most recent N deliveries for an endpoint, newest first. Used by
     * {@link CircuitBreakerPolicy} to compute the rolling failure rate.
     */
    List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId, Pageable pageable);
}
