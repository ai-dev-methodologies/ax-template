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

        // approve / reject are STEP-scoped decisions. The request-scoped answer is the
        // union of the per-step answers — computed by the very method the wire's
        // step-scoped field is built from (P3-76), so the two can never disagree.
        for (ApprovalStep step : request.getSteps()) {
            actions.addAll(allowedStepActions(request, step, callerUserId));
        }

        return List.copyOf(actions);
    }

    /**
     * P3-76 — the actions {@code callerUserId} may invoke on THIS step, sorted.
     *
     * <p>The request-scoped set answers "may I approve something here?" but not "WHICH
     * step is mine to act on", so a client holding only that array had to re-derive the
     * step — reintroducing, one level down, exactly the client-side authorization guess
     * P2-38b removed. This method is the server's answer to the narrower question.
     *
     * <p>Same zero-local-policy discipline: every branch is a {@link ApprovalActionGuards}
     * call or a probe of the REAL step machine's transition table. Dropping any one of
     * them would offer an action {@link ApprovalService} answers with 403/409 — the parity
     * trap the golden pins.
     *
     * <p>Only {@link #APPROVE} / {@link #REJECT} can ever appear: view is request-scoped
     * (a step is not independently visible), and submit/cancel act on the request as a
     * whole. The contract's step-scoped block declares that narrower vocabulary.
     */
    public List<String> allowedStepActions(ApprovalRequest request, ApprovalStep step, String callerUserId) {
        if (request == null || step == null || callerUserId == null) {
            return List.of();
        }
        if (!guards.isActionable(request)) {
            return List.of();
        }
        if (!guards.isAssignedApprover(step, callerUserId)) {
            return List.of();
        }
        if (!guards.isNextActionableStep(request, step)) {
            return List.of();
        }
        TreeSet<String> actions = new TreeSet<>();
        if (stepStateMachine.canTransition(step.getStatus(), ApprovalStepStatus.APPROVED)) {
            actions.add(APPROVE);
        }
        if (stepStateMachine.canTransition(step.getStatus(), ApprovalStepStatus.REJECTED)) {
            actions.add(REJECT);
        }
        return List.copyOf(actions);
    }
}
