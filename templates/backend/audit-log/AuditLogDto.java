/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogDto
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: contracts/audit-log-openapi.yaml#AuditLogSummary
 *               contracts/audit-log-openapi.yaml#AuditLogDetail
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Boot Reference — Using records as DTOs (immutable data carriers)"
 *     url: "https://docs.spring.io/spring-boot/docs/current/reference/html/"
 *   - source_type: external
 *     citation: "Java 16+ JEP 395 — Record Classes"
 *     url: "https://openjdk.org/jeps/395"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   AuditLogSummary is used in paginated list responses.
 *   AuditLogDetailResponse extends summary with metadata + correlationId + userAgent.
 *   Both are immutable records.
 */
package com.example.app.auditlog;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for the audit-log domain.
 *
 * <p>All records are immutable value types. Map from {@link AuditLog} entities
 * at the service/controller boundary using the static {@code from()} factory methods.
 */
public final class AuditLogDto {

    private AuditLogDto() {}

    /**
     * AuditLogSummary — lightweight view used in paginated list responses.
     * Maps to {@code AuditLogSummary} in {@code contracts/audit-log-openapi.yaml}.
     */
    public record Summary(
        UUID id,
        String actorId,
        String actorIp,
        String action,
        String resourceType,
        String resourceId,
        AuditLog.Outcome outcome,
        Instant timestamp
    ) {
        public static Summary from(AuditLog log) {
            return new Summary(
                log.getId(),
                log.getActorId(),
                log.getActorIp(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getOutcome(),
                log.getTimestamp()
            );
        }
    }

    /**
     * AuditLogDetail — full view including metadata, correlationId, and userAgent.
     * Maps to {@code AuditLogDetail} in {@code contracts/audit-log-openapi.yaml}.
     *
     * <p>Fields with {@code @JsonInclude(JsonInclude.Include.NON_NULL)} are omitted
     * from the response when null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(
        UUID id,
        String actorId,
        String actorIp,
        String action,
        String resourceType,
        String resourceId,
        AuditLog.Outcome outcome,
        Instant timestamp,
        Map<String, Object> metadata,
        String correlationId,
        String userAgent
    ) {
        public static Detail from(AuditLog log) {
            return new Detail(
                log.getId(),
                log.getActorId(),
                log.getActorIp(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getOutcome(),
                log.getTimestamp(),
                log.getMetadata(),
                log.getCorrelationId(),
                log.getUserAgent()
            );
        }
    }

    /**
     * Page wrapper for paginated audit log list responses.
     * Maps to {@code AuditLogPage} in {@code contracts/audit-log-openapi.yaml}.
     */
    public record Page<T>(
        java.util.List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
    ) {
        public static <T> Page<T> from(org.springframework.data.domain.Page<T> springPage) {
            return new Page<>(
                springPage.getContent(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.getNumber(),
                springPage.getSize()
            );
        }
    }
}
