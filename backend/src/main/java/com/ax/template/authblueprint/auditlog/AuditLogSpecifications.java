package com.ax.template.authblueprint.auditlog;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Composable specifications for the GET /api/audit-logs filter list.
 * <p>
 * Trace: AUDIT-LIST-002 — actorId / resourceType / resourceId / action / outcome / from / to.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> actorId(String v) {
        return v == null ? null : (root, q, cb) -> cb.equal(root.get("actorUserId"), v);
    }

    public static Specification<AuditLog> resourceType(String v) {
        return v == null ? null : (root, q, cb) -> cb.equal(root.get("resourceType"), v);
    }

    public static Specification<AuditLog> resourceId(String v) {
        return v == null ? null : (root, q, cb) -> cb.equal(root.get("resourceId"), v);
    }

    public static Specification<AuditLog> action(String v) {
        return v == null ? null : (root, q, cb) -> cb.equal(root.get("action"), v);
    }

    public static Specification<AuditLog> outcome(AuditOutcome v) {
        return v == null ? null : (root, q, cb) -> cb.equal(root.get("outcome"), v);
    }

    public static Specification<AuditLog> from(Instant v) {
        return v == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), v);
    }

    public static Specification<AuditLog> to(Instant v) {
        return v == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), v);
    }
}
