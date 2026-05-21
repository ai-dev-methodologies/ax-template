package com.ax.template.authblueprint.approvalworkflow;

import java.time.Instant;
import java.util.UUID;

public record ApprovalStepResponse(
    UUID id,
    int orderIndex,
    String approverUserId,
    ApprovalStepStatus status,
    String actedByUserId,
    Instant actedAt,
    String comment
) {

    public static ApprovalStepResponse from(ApprovalStep s) {
        return new ApprovalStepResponse(
            s.getId(),
            s.getOrderIndex(),
            s.getApproverUserId(),
            s.getStatus(),
            s.getActedByUserId(),
            s.getActedAt(),
            s.getComment()
        );
    }
}
