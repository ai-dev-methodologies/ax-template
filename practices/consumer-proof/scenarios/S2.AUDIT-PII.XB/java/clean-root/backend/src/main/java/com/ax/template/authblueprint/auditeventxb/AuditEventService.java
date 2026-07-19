package com.ax.template.authblueprint.auditeventxb;

import java.util.List;

/**
 * Thin service — maps AuditEvent rows to the FE-facing AuditEventResponse.
 * (Backing repository omitted from this fixture: the scenario targets the
 * DTO-mapping seam, not persistence.)
 */
public class AuditEventService {

    public List<AuditEventResponse> listRecent(List<AuditEvent> rows) {
        return rows.stream().map(AuditEventResponse::from).toList();
    }
}
