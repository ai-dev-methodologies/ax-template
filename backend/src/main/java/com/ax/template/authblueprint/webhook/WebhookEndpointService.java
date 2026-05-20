package com.ax.template.authblueprint.webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Endpoint lifecycle service.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-EMIT-001 — {@link #register(String, String)} performs an idempotent
 *       upsert keyed by URL. Re-registering the same URL rotates the secret without
 *       creating a duplicate row.</li>
 *   <li>blueprints/webhook-manifest.yaml#emit — registration_idempotent_on=url</li>
 * </ul>
 */
@Service
public class WebhookEndpointService {

    private final WebhookEndpointRepository repository;
    private final HmacSigner hmacSigner;

    public WebhookEndpointService(WebhookEndpointRepository repository, HmacSigner hmacSigner) {
        this.repository = repository;
        this.hmacSigner = hmacSigner;
    }

    /**
     * WEBHOOK-EMIT-001 — upsert by URL. Same URL → SAME id; secret rotated.
     */
    @Transactional
    public WebhookEndpoint register(String url, String eventFilter) {
        return repository.findByUrl(url)
            .map(existing -> {
                existing.rotateSecret(hmacSigner.generateSecret());
                existing.updateEventFilter(eventFilter);
                return repository.save(existing);
            })
            .orElseGet(() -> repository.save(
                WebhookEndpoint.create(url, hmacSigner.generateSecret(), eventFilter)));
    }

    @Transactional(readOnly = true)
    public Optional<WebhookEndpoint> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listActive() {
        return repository.findByActiveTrue();
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Transactional
    public WebhookEndpoint reactivate(UUID id) {
        WebhookEndpoint endpoint = repository.findById(id)
            .orElseThrow(() -> new WebhookEndpointNotFoundException(id));
        endpoint.activate();
        return repository.save(endpoint);
    }
}
