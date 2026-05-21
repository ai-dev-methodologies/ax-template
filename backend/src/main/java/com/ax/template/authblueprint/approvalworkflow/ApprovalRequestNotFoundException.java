package com.ax.template.authblueprint.approvalworkflow;

import java.util.UUID;

/** WF-AUTHZ-002 — mapped to HTTP 404 by the controller. */
public class ApprovalRequestNotFoundException extends RuntimeException {
    public ApprovalRequestNotFoundException(UUID id) {
        super("approval request not found: " + id);
    }
}
