package com.ax.template.authblueprint.approvalworkflow;

import java.util.List;
import java.util.UUID;

/**
 * Test-scope constructor for a fully-formed {@link ApprovalRequest} aggregate.
 *
 * <p>{@code ApprovalRequest#addStep} and {@code ApprovalStep#setActedByUserId} are
 * package-private on purpose (only the state machines and the service may mutate the
 * aggregate). This helper lives INSIDE that package so tests in sibling packages — in
 * particular {@code common.AuthorizedActionSetParityTest} — can assemble a probe
 * aggregate and hand it to the REAL {@link ApprovalActionEvaluator}, instead of
 * reimplementing the authorization rules they are supposed to be testing.
 */
public final class ApprovalAggregateFixtures {

    private ApprovalAggregateFixtures() {}

    /** One step of a probe aggregate. {@code actedByUserId} may be null (never acted). */
    public record StepSpec(UUID id, int orderIndex, String approverUserId,
                           ApprovalStepStatus status, String actedByUserId) {}

    public static ApprovalRequest build(String requesterUserId,
                                        ApprovalRequestStatus status,
                                        List<StepSpec> steps) {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .requesterUserId(requesterUserId)
            .type("PROBE")
            .title("probe")
            .status(status)
            .build();
        for (StepSpec spec : steps) {
            ApprovalStep step = ApprovalStep.builder()
                .id(spec.id())
                .orderIndex(spec.orderIndex())
                .approverUserId(spec.approverUserId())
                .status(spec.status())
                .build();
            if (spec.actedByUserId() != null) {
                step.setActedByUserId(spec.actedByUserId());
            }
            request.addStep(step);
        }
        return request;
    }
}
