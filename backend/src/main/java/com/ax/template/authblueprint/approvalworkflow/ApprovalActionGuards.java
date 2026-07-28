package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.stereotype.Component;

/**
 * P2-38a — the approval domain's authorization predicates, in ONE place.
 *
 * <p>Before this component the rules existed only as inline {@code throw} sites in
 * {@link ApprovalService#actOnStep} and {@link ApprovalService} {@code loadOwn}, plus a
 * THIRD, independently-written copy of the visibility rule as JPQL inside
 * {@code ApprovalRequestRepository.findVisibleTo}. Three expressions of the same policy
 * meant a change to one could silently diverge from the others, and nothing forced the
 * "can this caller do X?" question (needed for the {@code allowedActions} field, P2-38b)
 * to give the same answer as the action path that actually enforces it.
 *
 * <p>Every consumer now calls THESE methods — the action path, the read path, and the
 * {@link ApprovalActionEvaluator}. That is what makes the P2-38a mutation proofs
 * possible: neutering one predicate here must turn TWO independent tests red, because
 * two different surfaces are genuinely reading the same line.
 *
 * <p>Pure functions over already-loaded aggregates: no repository access, no transaction,
 * no state. A {@code @Component} only so consumers can inject it rather than reach for a
 * static utility (testability, and it keeps the layering guard happy).
 */
@Component
public class ApprovalActionGuards {

    /**
     * The request is in a state where step actions are legal — i.e. exactly SUBMITTED.
     *
     * <p>{@link ApprovalService} distinguishes two failure reasons off this one predicate
     * (already-terminal vs. never-submitted) because the two messages differ; the
     * PREDICATE, however, is single-valued, which is what the evaluator needs.
     */
    public boolean isActionable(ApprovalRequest request) {
        return request != null && request.getStatus() == ApprovalRequestStatus.SUBMITTED;
    }

    /** The step is assigned to this caller. */
    public boolean isAssignedApprover(ApprovalStep step, String actorUserId) {
        return step != null && actorUserId != null
            && actorUserId.equals(step.getApproverUserId());
    }

    /**
     * Strict 결재선 ordering — every step ahead of {@code step} in the chain is APPROVED.
     *
     * <p>A rejected or still-pending earlier step blocks the later one; this is the
     * sequential Korean-enterprise approval line, not a parallel one.
     */
    public boolean isNextActionableStep(ApprovalRequest request, ApprovalStep step) {
        if (request == null || step == null) {
            return false;
        }
        return request.getSteps().stream()
            .filter(s -> s.getOrderIndex() < step.getOrderIndex())
            .allMatch(s -> s.getStatus() == ApprovalStepStatus.APPROVED);
    }

    /** The caller raised this request. */
    public boolean isRequester(ApprovalRequest request, String callerUserId) {
        return request != null && callerUserId != null
            && callerUserId.equals(request.getRequesterUserId());
    }

    /**
     * P3-63 — the full read-visibility rule, and the single source of truth for it since
     * {@code findVisibleTo} was deleted.
     *
     * <p>Visible iff the caller is:
     * <ul>
     *   <li>the REQUESTER (any status, including DRAFT); or</li>
     *   <li>an assigned approver while the request is SUBMITTED (the pre-existing rule,
     *       preserved verbatim); or</li>
     *   <li><b>(new, narrow)</b> an approver who ACTED — their own step carries
     *       {@code actedByUserId == caller} — after the request reached a terminal state.
     *   </li>
     * </ul>
     *
     * <p>The third arm closes a real gap: an approver would approve a request and then
     * lose all access to it the instant the chain completed, so they could not see the
     * outcome of their own decision. It is deliberately NARROW — an assigned-but-unacted
     * approver gets nothing after the request terminates (a rejection at step 1 must not
     * retroactively disclose the request to steps 2..n, who were never asked), and a DRAFT
     * is never visible to an approver at all (it has not been sent to them yet).
     */
    public boolean canView(ApprovalRequest request, String callerUserId) {
        if (request == null || callerUserId == null) {
            return false;
        }
        if (isRequester(request, callerUserId)) {
            return true;
        }
        boolean submitted = request.getStatus() == ApprovalRequestStatus.SUBMITTED;
        for (ApprovalStep step : request.getSteps()) {
            if (submitted && isAssignedApprover(step, callerUserId)) {
                return true;
            }
            if (request.getStatus().isTerminal() && hasActed(step, callerUserId)) {
                return true;
            }
        }
        return false;
    }

    /** The caller personally decided this step (P3-63's acted-approver qualifier). */
    public boolean hasActed(ApprovalStep step, String callerUserId) {
        return step != null && callerUserId != null
            && callerUserId.equals(step.getActedByUserId())
            && step.getStatus() != ApprovalStepStatus.PENDING;
    }
}
