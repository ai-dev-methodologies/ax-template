package com.ax.template.authblueprint.approvalworkflow;

/** WF-STEP-004 — mapped to HTTP 400 DUPLICATE_APPROVER. */
public class DuplicateApproverException extends RuntimeException {
    public DuplicateApproverException(String detail) {
        super(detail);
    }
}
