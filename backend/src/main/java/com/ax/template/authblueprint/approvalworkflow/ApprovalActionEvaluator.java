package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.TreeSet;

/**
 * P2-38b — computes the {@code allowedActions} set the wire now carries.
 *
 * <p>The gap this closes: {@code ApprovalRequestResponse} used to expose status,
 * requesterUserId and steps, and nothing else — so a client had to RE-DERIVE "may I
 * approve this?" from raw fields, guessing at guards it could not see. Two independent
 * re-derivations existed in the tree (a vitest-local selector and the L4 detail page's
 * chain descriptor), each free to drift from the enforced rules. Now the server answers
 * the question it alone can answer.
 *
 * <p><b>Zero local policy.</b> Every branch below is either a call into
 * {@link ApprovalActionGuards} (the same predicates {@link ApprovalService} enforces) or a
 * probe of the REAL state machines' transition tables. There is deliberately no
 * transition map, no status comparison and no ownership test written here — if this class
 * could decide anything by itself, it could disagree with the action path, which is the
 * entire defect being closed.
 */
@Component
public class ApprovalActionEvaluator {

    public static final String VIEW = "view";
    public static final String SUBMIT = "submit";
    public static final String CANCEL = "cancel";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";

    private final ApprovalActionGuards guards;
    private final ApprovalRequestStateMachine requestStateMachine;
    private final ApprovalStepStateMachine stepStateMachine;

    public ApprovalActionEvaluator(ApprovalActionGuards guards,
                                   ApprovalRequestStateMachine requestStateMachine,
                                   ApprovalStepStateMachine stepStateMachine) {
        this.guards = guards;
        this.requestStateMachine = requestStateMachine;
        this.stepStateMachine = stepStateMachine;
    }

    /**
     * The actions {@code callerUserId} may invoke on {@code request}, sorted for a stable
     * wire representation. Empty for a caller who cannot even see the request — an
     * unauthorized caller learns nothing, not even that they were evaluated.
     */
    public List<String> allowedActions(ApprovalRequest request, String callerUserId) {
        TreeSet<String> actions = new TreeSet<>();
        if (request == null || callerUserId == null) {
            return List.of();
        }

        if (guards.canView(request, callerUserId)) {
            actions.add(VIEW);
        }

        // submit / cancel are requester-only (ApprovalService.loadOwn → isRequester),
        // then gated by the request machine's own transition table.
        if (guards.isRequester(request, callerUserId)) {
            if (requestStateMachine.canTransition(request.getStatus(), ApprovalRequestStatus.SUBMITTED)) {
                actions.add(SUBMIT);
            }
            if (requestStateMachine.canTransition(request.getStatus(), ApprovalRequestStatus.CANCELLED)) {
                actions.add(CANCEL);
            }
        }

        // approve / reject require ALL THREE action-path guards to hold, then the step
        // machine's transition table. Dropping any one of them here would offer an action
        // the BE answers with 409/403 — exactly the parity trap the golden pins.
        if (guards.isActionable(request)) {
            for (ApprovalStep step : request.getSteps()) {
                if (!guards.isAssignedApprover(step, callerUserId)) {
                    continue;
                }
                if (!guards.isNextActionableStep(request, step)) {
                    continue;
                }
                if (stepStateMachine.canTransition(step.getStatus(), ApprovalStepStatus.APPROVED)) {
                    actions.add(APPROVE);
                }
                if (stepStateMachine.canTransition(step.getStatus(), ApprovalStepStatus.REJECTED)) {
                    actions.add(REJECT);
                }
            }
        }

        return List.copyOf(actions);
    }
}
