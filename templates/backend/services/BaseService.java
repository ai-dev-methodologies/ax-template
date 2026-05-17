/**
 * @ax-template-meta
 * template_id: backend/services/BaseService
 * layer: backend-cross-cutting
 * anchors_rule: core-constructor-injection.md (PRACTICES-CORE-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — Constructor-based Dependency Injection"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Transactional annotations"
 *     url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Extend this class and inject dependencies via constructor in the subclass.
 *   All write operations should carry @Transactional at the method level.
 *   Implement auditEvent() if your domain requires audit trail (see AuditHook).
 */
package com.example.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for application services.
 *
 * <p>Enforces the constructor injection pattern — subclass dependencies must be
 * declared as {@code private final} fields and injected through a constructor.
 * This surfaces missing or circular dependencies at startup and enables
 * plain-JUnit testing without a Spring container.
 *
 * <p>Subclass usage:
 * <pre>{@code
 * @Service
 * public class ItemService extends BaseService {
 *
 *     private final ItemRepository itemRepository;
 *
 *     public ItemService(ItemRepository itemRepository) {
 *         this.itemRepository = itemRepository;
 *     }
 *
 *     @Transactional
 *     public ItemResponse create(CreateItemRequest request) {
 *         Item item = itemRepository.save(new Item(request.name()));
 *         auditEvent("item.created", item.getId().toString());
 *         return new ItemResponse(item.getId(), item.getName());
 *     }
 * }
 * }</pre>
 *
 * <p>Rule reference: PRACTICES-CORE-001 (constructor injection).
 */
public abstract class BaseService {

    /** Logger available to all subclasses. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Audit hook — override in subclasses that require a domain audit trail.
     *
     * <p>Default implementation is a no-op. Production implementations should
     * write to an audit log, publish an application event, or call an
     * {@code AuditService} injected via the subclass constructor.
     *
     * <p>Call pattern:
     * <pre>{@code
     * @Transactional
     * public void deleteItem(Long id) {
     *     itemRepository.deleteById(id);
     *     auditEvent("item.deleted", id.toString());
     * }
     * }</pre>
     *
     * @param eventType domain event type string, e.g. {@code "item.created"}
     * @param entityId  affected entity identifier (string form)
     */
    protected void auditEvent(String eventType, String entityId) {
        // Default no-op. Override to publish ApplicationEvent or write to AuditLog.
        log.debug("audit: eventType={} entityId={}", eventType, entityId);
    }
}
