package com.ax.template.authblueprint.auditeventxb;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin admin controller exposing recent audit events to the FE viewer.
 * Delegates to AuditEventService (never touches persistence directly).
 */
@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/api/admin/audit-events")
    public List<AuditEventResponse> listRecent(List<AuditEvent> rows) {
        return auditEventService.listRecent(rows);
    }
}
