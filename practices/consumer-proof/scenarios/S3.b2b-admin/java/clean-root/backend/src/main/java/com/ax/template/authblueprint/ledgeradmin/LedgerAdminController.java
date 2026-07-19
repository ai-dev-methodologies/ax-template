package com.ax.template.authblueprint.ledgeradmin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * LedgerAdminController — B2B admin ledger + audit-export vertical slice.
 * CLEAN on both axes this scenario proves:
 *   1. authz — class-level @PreAuthorize covers every admin endpoint (the
 *      list GET cannot be reached without ROLE_ADMIN — no IDOR).
 *   2. error contract — the @ExceptionHandler returns ProblemDetail (RFC 9457),
 *      never a bare Map (controller_problemdetail_guard.sh).
 * Delegates to AuditExportService; never touches AuditLedgerRepository
 * directly (controller_repository_shell_guard.sh).
 */
@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class LedgerAdminController {

    private final AuditExportService auditExportService;

    public LedgerAdminController(AuditExportService auditExportService) {
        this.auditExportService = auditExportService;
    }

    @GetMapping("/api/admin/ledger")
    public Page<AuditLedgerEntry> listLedger(@RequestParam String entityRef, Pageable pageable) {
        return auditExportService.listLedger(entityRef, pageable);
    }

    @PostMapping("/api/admin/ledger/export")
    public AuditExportJob enqueueExport(Authentication authentication) {
        return auditExportService.enqueueExport(authentication.getName());
    }

    @GetMapping("/api/admin/ledger/export/{id}")
    public AuditExportJob exportStatus(@PathVariable Long id) {
        return auditExportService.getStatus(id);
    }

    @ExceptionHandler(LedgerJobNotFoundException.class)
    public ProblemDetail handleNotFound(LedgerJobNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
