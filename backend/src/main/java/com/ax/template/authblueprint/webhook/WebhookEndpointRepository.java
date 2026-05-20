package com.ax.template.authblueprint.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    Optional<WebhookEndpoint> findByUrl(String url);

    List<WebhookEndpoint> findByActiveTrue();
}
