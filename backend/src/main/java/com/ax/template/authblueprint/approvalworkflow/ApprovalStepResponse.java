package com.ax.template.authblueprint.approvalworkflow;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalStepResponse(
    UUID id,
    int orderIndex,
    String approverUserId,
    ApprovalStepStatus status,
    String actedByUserId,
    Instant actedAt,
    String comment,
    /**
     * P3-76 — the actions THIS caller may invoke on THIS step, computed server-side by
     * {@link ApprovalActionEvaluator#allowedStepActions}. Only {@code approve} /
     * {@code reject} can appear: view is request-scoped and submit/cancel act on the
     * request as a whole.
     *
     * <p>{@code ApprovalRequestResponse.allowedActions} (P2-38b) says whether the caller
     * may approve SOMETHING on this request; it cannot say WHICH step is theirs to act
     * on, so a client rendering an action panel still had to re-derive the step — the
     * client-side authorization guess P2-38b set out to remove, one level down. This
     * field is the server's answer to that narrower question.
     *
     * <p>Empty (never null) when the caller may do nothing with this step, including for
     * a caller-independent projection — absence of a claim, not a claim of absence.
     */
    List<String> allowedActions
) {

    public static ApprovalStepResponse from(ApprovalStep s, ApprovalRequest request,
                                            ApprovalActionEvaluator evaluator, String callerUserId) {
        return new ApprovalStepResponse(
            s.getId(),
            s.getOrderIndex(),
            s.getApproverUserId(),
            s.getStatus(),
            s.getActedByUserId(),
            s.getActedAt(),
            s.getComment(),
            evaluator == null ? List.of() : evaluator.allowedStepActions(request, s, callerUserId)
        );
    }
}
