/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogQueryDto
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-LIST-002
 *               contracts/audit-log-openapi.yaml#listAuditLogs
 *               blueprints/audit-log-manifest.yaml#list_policy
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @ModelAttribute for query parameter binding"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaSpecificationExecutor + Specification<T> for dynamic queries"
 *     url: "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Bind from HTTP query params via @ModelAttribute in the controller.
 *   Pass to AuditLogSpecifications.fromQuery() to build a JPA Specification.
 */
package com.example.app.auditlog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * AuditLogQueryDto — query parameters for GET /api/audit-logs.
 *
 * <p>All filter fields are optional. When null, the corresponding predicate is omitted
 * from the JPA Specification (AND semantics — AUDIT-LIST-002).
 *
 * <p>Bind via {@code @ModelAttribute AuditLogQueryDto query} in the controller.
 * Validate via {@code @Validated} on the controller method.
 */
public record AuditLogQueryDto(

    /** Zero-based page number. Default 0. */
    @PositiveOrZero
    Integer page,

    /** Page size. Default 50, max 200 (blueprints/audit-log-manifest.yaml#list_policy). */
    @Min(1) @Max(200)
    Integer size,

    /** Filter by actor user ID. */
    String actorId,

    /** Filter by resource type (payment, user, item, etc.). */
    String resourceType,

    /** Filter by resource ID. */
    String resourceId,

    /** Filter by action verb (CREATE, UPDATE, DELETE, etc.). */
    String action,

    /** Filter by outcome: SUCCESS or FAILURE. */
    AuditLog.Outcome outcome,

    /** Filter: timestamp >= from (ISO 8601 UTC). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from,

    /** Filter: timestamp <= to (ISO 8601 UTC). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to

) {
    /** Effective page number (defaults to 0). */
    public int effectivePage() {
        return page != null ? page : 0;
    }

    /** Effective page size (defaults to 50, capped at 200). */
    public int effectiveSize() {
        if (size == null) return 50;
        return Math.min(size, 200);
    }
}
