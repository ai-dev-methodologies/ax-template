package com.ax.template.authblueprint.approvalworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestration for the approval-workflow domain. Owns the boundary between the
 * controller (HTTP concerns) and the two state machines.
 *
 * <p>Trace:
 * <ul>
 *   <li>WF-LIFECYCLE-001..003 — request + step state transitions composed in a
 *       single {@code @Transactional} method so cascades are atomic.</li>
 *   <li>WF-AUTHZ-002 / 003 — visibility + actor checks happen before any state
 *       machine call.</li>
 *   <li>WF-STEP-001 — out-of-order step actions are rejected up-front.</li>
 * </ul>
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRequestStateMachine requestStateMachine;
    private final ApprovalStepStateMachine stepStateMachine;
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalRequestRepository requestRepository,
                           ApprovalRequestStateMachine requestStateMachine,
                           ApprovalStepStateMachine stepStateMachine,
                           ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.requestStateMachine = requestStateMachine;
        this.stepStateMachine = stepStateMachine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApprovalRequestResponse create(String requesterUserId, CreateApprovalRequest body) {
        ApprovalRequest request = ApprovalRequest.builder()
            .requesterUserId(requesterUserId)
            .type(body.type())
            .title(body.title())
            .payloadJson(serializePayload(body.payload()))
            .status(ApprovalRequestStatus.DRAFT)
            .build();

        int order = 0;
        for (String approverUserId : body.approverUserIds()) {
            request.addStep(ApprovalStep.builder()
                .orderIndex(order++)
                .approverUserId(approverUserId)
                .status(ApprovalStepStatus.PENDING)
                .build());
        }

        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper);
    }

    @Transactional
    public ApprovalRequestResponse submit(String requesterUserId, UUID id) {
        ApprovalRequest request = loadOwn(requesterUserId, id);
        requestStateMachine.markSubmitted(request);
        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper);
    }

    @Transactional
    public ApprovalRequestResponse cancel(String requesterUserId, UUID id) {
        ApprovalRequest request = loadOwn(requesterUserId, id);
        requestStateMachine.markCancelled(request);
        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper);
    }

    @Transactional
    public ApprovalRequestResponse approveStep(String actorUserId, UUID requestId, UUID stepId, String comment) {
        return actOnStep(actorUserId, requestId, stepId, true, comment);
    }

    @Transactional
    public ApprovalRequestResponse rejectStep(String actorUserId, UUID requestId, UUID stepId, String comment) {
        return actOnStep(actorUserId, requestId, stepId, false, comment);
    }

    @Transactional(readOnly = true)
    public ApprovalListResponse listOwn(String requesterUserId) {
        List<ApprovalRequest> rows = requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(requesterUserId);
        List<ApprovalRequestResponse> items = rows.stream()
            .map(r -> ApprovalRequestResponse.from(r, objectMapper))
            .toList();
        return new ApprovalListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getVisible(String userId, UUID id) {
        ApprovalRequest req = requestRepository.findVisibleTo(id, userId)
            .orElseThrow(() -> new ApprovalRequestNotFoundException(id));
        return ApprovalRequestResponse.from(req, objectMapper);
    }

    @Transactional(readOnly = true)
    public ApprovalInboxResponse inbox(String approverUserId) {
        List<ApprovalStep> steps = requestRepository.findInboxFor(approverUserId);
        List<ApprovalInboxEntry> items = steps.stream()
            .map(ApprovalInboxEntry::from)
            .toList();
        return new ApprovalInboxResponse(items, items.size());
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private ApprovalRequestResponse actOnStep(String actorUserId,
                                              UUID requestId,
                                              UUID stepId,
                                              boolean approve,
                                              String comment) {
        ApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ApprovalRequestNotFoundException(requestId));

        if (request.getStatus().isTerminal()) {
            throw new RequestTerminalException(
                "request " + requestId + " is already " + request.getStatus());
        }
        if (request.getStatus() != ApprovalRequestStatus.SUBMITTED) {
            throw new RequestTerminalException(
                "request " + requestId + " is not SUBMITTED (current: " + request.getStatus() + ")");
        }

        ApprovalStep target = request.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new ApprovalRequestNotFoundException(stepId));

        if (!target.getApproverUserId().equals(actorUserId)) {
            throw new NotApproverException(
                "step " + stepId + " can only be acted on by " + target.getApproverUserId());
        }

        // Strict ordering: every step with orderIndex < target.orderIndex must be APPROVED.
        for (ApprovalStep s : request.getSteps()) {
            if (s.getOrderIndex() < target.getOrderIndex()
                && s.getStatus() != ApprovalStepStatus.APPROVED) {
                throw new StepOutOfOrderException(
                    "earlier step (orderIndex=" + s.getOrderIndex()
                    + ") is " + s.getStatus() + "; expected APPROVED before acting on this step");
            }
        }

        if (approve) {
            stepStateMachine.markApproved(target, actorUserId, comment);
            boolean allApproved = request.getSteps().stream()
                .allMatch(s -> s.getStatus() == ApprovalStepStatus.APPROVED);
            if (allApproved) {
                requestStateMachine.markApproved(request);
            }
        } else {
            stepStateMachine.markRejected(target, actorUserId, comment);
            requestStateMachine.markRejected(request);
        }

        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper);
    }

    private ApprovalRequest loadOwn(String requesterUserId, UUID id) {
        ApprovalRequest req = requestRepository.findById(id)
            .orElseThrow(() -> new ApprovalRequestNotFoundException(id));
        if (!req.getRequesterUserId().equals(requesterUserId)) {
            throw new ApprovalRequestNotFoundException(id);  // 404 — IDOR-safe
        }
        return req;
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("payload is not JSON-serializable", ex);
        }
    }
}
