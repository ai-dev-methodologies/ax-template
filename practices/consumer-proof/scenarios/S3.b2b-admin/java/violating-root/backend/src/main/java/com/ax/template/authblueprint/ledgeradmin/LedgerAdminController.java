package com.ax.template.authblueprint.ledgeradmin;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * LedgerAdminController — VIOLATING on two axes named by this dogfood cell:
 *   1. authz (IDOR) — NO @PreAuthorize anywhere: class-level annotation is
 *      gone AND the list GET has no method-level @PreAuthorize either. Any
 *      authenticated (or unauthenticated, depending on SecurityConfig
 *      defaults) caller can list every tenant's ledger rows.
 *   2. error contract — the @ExceptionHandler returns a bare Map<String,String>
 *      instead of ProblemDetail (RFC 9457) — the exact IDW1 dogfood gap
 *      controller_problemdetail_guard.sh was built to close.
 */
@RestController
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
    public Map<String, String> handleNotFound(LedgerJobNotFoundException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return body;
    }
}
