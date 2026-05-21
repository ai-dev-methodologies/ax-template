package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration binding for the approval-workflow domain.
 *
 * <p>Defaults mirror {@code blueprints/approval-workflow-manifest.yaml}. Fork-receivers
 * override via {@code application.yml} when their policy differs (e.g. test fixtures or
 * single-person workflows that legitimately need self-approve).
 */
@ConfigurationProperties(prefix = "approval-workflow")
public class ApprovalWorkflowProperties {

    /**
     * Korean enterprise 결재 default: 본인 결재 금지. Flip to {@code true} only when the
     * workflow legitimately needs the requester to also be the approver (test fixtures,
     * single-person teams). WF-STEP-005 covers the default policy.
     */
    private boolean allowSelfApprove = false;

    public boolean isAllowSelfApprove() { return allowSelfApprove; }
    public void setAllowSelfApprove(boolean v) { this.allowSelfApprove = v; }
}
