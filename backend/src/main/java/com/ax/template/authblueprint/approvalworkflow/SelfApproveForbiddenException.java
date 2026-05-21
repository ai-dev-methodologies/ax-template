package com.ax.template.authblueprint.approvalworkflow;

/** WF-STEP-005 — mapped to HTTP 400 SELF_APPROVE_FORBIDDEN. */
public class SelfApproveForbiddenException extends RuntimeException {
    public SelfApproveForbiddenException(String detail) {
        super(detail);
    }
}
