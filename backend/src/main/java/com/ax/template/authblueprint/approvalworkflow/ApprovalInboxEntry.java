package com.ax.template.authblueprint.approvalworkflow;

import java.time.Instant;
import java.util.UUID;

public record ApprovalInboxEntry(
    UUID requestId,
    UUID stepId,
    String type,
    String title,
    ApprovalStepStatus status,
    String requesterUserId,
    Instant createdAt
) {

    public static ApprovalInboxEntry from(ApprovalStep step) {
        ApprovalRequest req = step.getRequest();
        return new ApprovalInboxEntry(
            req.getId(),
            step.getId(),
            req.getType(),
            req.getTitle(),
            step.getStatus(),
            req.getRequesterUserId(),
            req.getCreatedAt()
        );
    }
}
