/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogSpecifications
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-LIST-002
 *               blueprints/audit-log-manifest.yaml#list_policy
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Specifications and JpaSpecificationExecutor"
 *     url: "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications"
 *   - source_type: external
 *     citation: "Jakarta Persistence 3.1 — CriteriaBuilder API"
 *     url: "https://jakarta.ee/specifications/persistence/3.1/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Call AuditLogSpecifications.fromQuery(queryDto) to get a Specification<AuditLog>
 *   combining all non-null filter predicates with AND semantics.
 */
package com.example.app.auditlog;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogSpecifications — factory for JPA Specification predicates.
 *
 * <p>All predicates are AND-combined. Null/blank filter values are omitted (AUDIT-LIST-002).
 *
 * <pre>{@code
 * Specification<AuditLog> spec = AuditLogSpecifications.fromQuery(queryDto);
 * Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
 * }</pre>
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    /**
     * Builds a composite Specification from the given query DTO.
     *
     * <p>Each non-null field in the query generates one predicate.
     * All predicates are AND-combined.
     *
     * @param query filter parameters from the HTTP request
     * @return composed Specification, or {@code Specification.where(null)} if no filters
     */
    public static Specification<AuditLog> fromQuery(AuditLogQueryDto query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.actorId() != null && !query.actorId().isBlank()) {
                predicates.add(cb.equal(root.get("actorId"), query.actorId()));
            }
            if (query.resourceType() != null && !query.resourceType().isBlank()) {
                predicates.add(cb.equal(root.get("resourceType"), query.resourceType()));
            }
            if (query.resourceId() != null && !query.resourceId().isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), query.resourceId()));
            }
            if (query.action() != null && !query.action().isBlank()) {
                predicates.add(cb.equal(root.get("action"), query.action()));
            }
            if (query.outcome() != null) {
                predicates.add(cb.equal(root.get("outcome"), query.outcome()));
            }
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), query.to()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();  // no-op: match all
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
