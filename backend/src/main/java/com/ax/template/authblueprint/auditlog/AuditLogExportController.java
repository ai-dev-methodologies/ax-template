package com.ax.template.authblueprint.auditlog;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * POST /api/audit-logs/export + GET /api/audit-logs/export/{jobId}.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-EXPORT-001 — 202 + job ID; poll status endpoint</li>
 *   <li>AUDIT-EXPORT-002 — ADMIN or AUDITOR only</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/audit-logs/export")
public class AuditLogExportController {

    private final AuditLogExportService exportService;

    public AuditLogExportController(AuditLogExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ExportJobResponse> enqueue(
        @Valid @RequestBody ExportRequest request,
        Authentication authentication
    ) {
        String requestedBy = authentication == null ? "anonymous" : authentication.getName();
        String filterJson = serializeFilter(request);
        AuditExportJob job = exportService.enqueue(requestedBy, request.format(), filterJson);
        return ResponseEntity
            .accepted()
            .body(new ExportJobResponse(job.getId(), job.getStatus()));
    }

    /**
     * P1-66 — AUDIT-EXPORT-002 covers "Export requests" as a whole: the status/poll surface is
     * role-gated identically to the enqueue POST. Without this a ROLE_MEMBER / X-API-Key principal
     * holding any jobId could read a completed ADMIN/AUDITOR export's downloadUrl + recordCount
     * (IDOR). Audit-log exports are an ADMIN-global artifact (the requestedBy field is provenance,
     * not an ownership boundary), so a role-gate — not per-owner scoping — is the correct fix: an
     * ADMIN must still be able to poll a job an AUDITOR enqueued.
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ExportJobStatusResponse> status(@PathVariable UUID jobId) {
        return exportService.findById(jobId)
            .map(j -> ResponseEntity.ok(new ExportJobStatusResponse(
                j.getId(),
                j.getStatus(),
                j.getDownloadUrl(),
                j.getErrorMessage(),
                j.getRecordCount()
            )))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Compact one-line JSON of the export filter — used only for traceability;
     * the audit-log catalog does not depend on a full JSON serialization stack
     * here. Values are {@code null}-safe and pass-through; this is internal
     * trace metadata, not a user-facing payload.
     */
    private String serializeFilter(ExportRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"format\":\"").append(r.format()).append("\"");
        appendIfPresent(sb, "actorId", r.actorId());
        appendIfPresent(sb, "resourceType", r.resourceType());
        appendIfPresent(sb, "action", r.action());
        if (r.outcome() != null) appendIfPresent(sb, "outcome", r.outcome().name());
        if (r.from() != null) appendIfPresent(sb, "from", r.from().toString());
        if (r.to() != null) appendIfPresent(sb, "to", r.to().toString());
        sb.append('}');
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append(",\"").append(key).append("\":\"")
          .append(value.replace("\"", "\\\""))
          .append("\"");
    }
}
