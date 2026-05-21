package com.ax.template.authblueprint.approvalworkflow;

/** WF-AUTHZ-003 — mapped to HTTP 403 by the controller. */
public class NotApproverException extends RuntimeException {
    public NotApproverException(String detail) {
        super(detail);
    }
}
