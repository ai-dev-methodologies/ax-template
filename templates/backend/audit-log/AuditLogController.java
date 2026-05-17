/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogController
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: contracts/audit-log-openapi.yaml#listAuditLogs
 *               contracts/audit-log-openapi.yaml#getAuditLog
 *               contracts/audit-log-openapi.yaml#exportAuditLogs
 *               contracts/audit-log-openapi.yaml#getExportJobStatus
 *               specs/audit-log-l0.yaml#AUDIT-EXPORT-002
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @RestController, @RequestMapping, @GetMapping, @PostMapping"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
 *   - source_type: external
 *     citation: "Spring Security Reference — @PreAuthorize for method-level security"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Endpoints inherit @RestController + produces=application/json from BaseController.
 *   Export endpoints require ROLE_ADMIN or ROLE_AUDITOR (AUDIT-EXPORT-002).
 */
package com.example.app.auditlog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AuditLogController — REST endpoints for the audit-log domain.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /api/audit-logs           — listAuditLogs (paginated + filtered)
 *   <li>GET  /api/audit-logs/{id}       — getAuditLog (single entry)
 *   <li>POST /api/audit-logs/export     — exportAuditLogs (async, requires AUDITOR/ADMIN)
 *   <li>GET  /api/audit-logs/export/{jobId} — getExportJobStatus (poll)
 * </ul>
 *
 * <p>Authorization (AUDIT-EXPORT-002): export endpoints require ROLE_ADMIN or ROLE_AUDITOR.
 * List and get endpoints require any authenticated session.
 */
@RequestMapping(value = "/api/audit-logs", produces = "application/json")
@Validated
public class AuditLogController extends com.example.app.controllers.BaseController {

    private final AuditLogService auditLogService;
    private final AuditLogExportService exportService;

    public AuditLogController(AuditLogService auditLogService,
                              AuditLogExportService exportService) {
        this.auditLogService = auditLogService;
        this.exportService = exportService;
    }

    // ─── List ────────────────────────────────────────────────────────────────

    /**
     * GET /api/audit-logs — paginated filtered list.
     *
     * <p>All query params are optional (AND semantics). Sorted by timestamp DESC.
     */
    @GetMapping
    public ResponseEntity<AuditLogDto.Page<AuditLogDto.Summary>> listAuditLogs(
            @Valid @ModelAttribute AuditLogQueryDto query,
            HttpServletRequest request) {
        AuditLogDto.Page<AuditLogDto.Summary> page = auditLogService.listAuditLogs(query);
        return ResponseEntity.ok()
            .header("X-Correlation-Id", correlationId(request))
            .body(page);
    }

    // ─── Get ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/audit-logs/{id} — single entry detail.
     *
     * <p>Returns 404 if not found. No 403 to prevent ID enumeration.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDto.Detail> getAuditLog(
            @PathVariable UUID id,
            HttpServletRequest request) {
        return auditLogService.getAuditLog(id)
            .map(detail -> ResponseEntity.ok()
                .header("X-Correlation-Id", correlationId(request))
                .body(detail))
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── Export ──────────────────────────────────────────────────────────────

    /**
     * POST /api/audit-logs/export — enqueue async export job (AUDIT-EXPORT-001).
     *
     * <p>Requires ROLE_ADMIN or ROLE_AUDITOR (AUDIT-EXPORT-002).
     * Returns 202 Accepted with jobId.
     */
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<AuditLogExportService.ExportJobResponse> exportAuditLogs(
            @RequestBody @Valid AuditLogExportService.ExportRequest request,
            HttpServletRequest httpRequest) {
        AuditLogExportService.ExportJobResponse job = exportService.enqueue(request);
        return ResponseEntity.accepted()
            .header("X-Correlation-Id", correlationId(httpRequest))
            .body(job);
    }

    /**
     * GET /api/audit-logs/export/{jobId} — poll export job status.
     *
     * <p>Requires ROLE_ADMIN or ROLE_AUDITOR. Returns 404 if jobId unknown.
     */
    @GetMapping("/export/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<AuditLogExportService.ExportJobStatus> getExportJobStatus(
            @PathVariable UUID jobId,
            HttpServletRequest request) {
        return exportService.getStatus(jobId)
            .map(status -> ResponseEntity.ok()
                .header("X-Correlation-Id", correlationId(request))
                .body(status))
            .orElse(ResponseEntity.notFound().build());
    }
}
