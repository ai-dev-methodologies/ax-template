package com.ax.template.authblueprint.auditlog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * GET /api/audit-logs and GET /api/audit-logs/{id}.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-LIST-001 — paginated list, sorted by timestamp DESC</li>
 *   <li>AUDIT-LIST-002 — filter combination</li>
 * </ul>
 * Authorization is handled by {@code SecurityConfig} ({@code /api/audit-logs/**}
 * requires authentication).
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public AuditLogPage list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) String actorId,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String resourceId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) AuditOutcome outcome,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
        return service.list(
            new AuditLogService.AuditLogFilter(actorId, resourceType, resourceId, action, outcome, from, to),
            page,
            size
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getOne(@PathVariable UUID id) {
        return service.findById(id)
            .map(AuditLogResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
