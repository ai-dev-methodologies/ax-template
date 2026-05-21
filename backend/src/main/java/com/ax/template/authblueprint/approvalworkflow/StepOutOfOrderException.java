package com.ax.template.authblueprint.approvalworkflow;

/** WF-STEP-001 — mapped to HTTP 409 STEP_OUT_OF_ORDER by the controller. */
public class StepOutOfOrderException extends RuntimeException {
    public StepOutOfOrderException(String detail) {
        super(detail);
    }
}
