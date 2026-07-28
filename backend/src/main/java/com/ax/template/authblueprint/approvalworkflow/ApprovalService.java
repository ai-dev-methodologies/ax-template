package com.ax.template.authblueprint.approvalworkflow;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final TypeReference<List<String>> ROLE_CHAIN_TYPE = new TypeReference<>() {};

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRequestStateMachine requestStateMachine;
    private final ApprovalStepStateMachine stepStateMachine;
    private final ApprovalWorkflowProperties properties;
    private final ObjectMapper objectMapper;
    private final RoutingRuleRepository routingRuleRepository;
    private final ApprovalActionGuards guards;
    private final ApprovalActionEvaluator evaluator;

    public ApprovalService(ApprovalRequestRepository requestRepository,
                           ApprovalRequestStateMachine requestStateMachine,
                           ApprovalStepStateMachine stepStateMachine,
                           ApprovalWorkflowProperties properties,
                           ObjectMapper objectMapper,
                           RoutingRuleRepository routingRuleRepository,
                           ApprovalActionGuards guards,
                           ApprovalActionEvaluator evaluator) {
        this.requestRepository = requestRepository;
        this.requestStateMachine = requestStateMachine;
        this.stepStateMachine = stepStateMachine;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.routingRuleRepository = routingRuleRepository;
        this.guards = guards;
        this.evaluator = evaluator;
    }

    /**
     * WF-ROUTE-001 — {@code approverUserIds} (direct mode) and {@code category}/{@code amount}
     * (routing mode) are mutually exclusive; exactly one must be present. Routing mode leaves
     * {@code steps} empty here — resolution happens later, at {@link #submit}.
     */
    @Transactional
    public ApprovalRequestResponse create(String requesterUserId, CreateApprovalRequest body) {
        boolean direct = body.approverUserIds() != null && !body.approverUserIds().isEmpty();
        boolean routed = body.category() != null && !body.category().isBlank() && body.amount() != null;
        if (!direct && !routed) {
            throw new RoutingAttributesRequiredException(
                "either a non-empty approverUserIds list or both category and amount must be provided");
        }

        ApprovalRequest.Builder builder = ApprovalRequest.builder()
            .requesterUserId(requesterUserId)
            .type(body.type())
            .title(body.title())
            .payloadJson(serializePayload(body.payload()))
            .status(ApprovalRequestStatus.DRAFT);
        if (routed) {
            builder.category(body.category().strip()).amount(body.amount());
        }
        ApprovalRequest request = builder.build();

        if (direct) {
            validateApprovers(requesterUserId, body.approverUserIds());
            int order = 0;
            for (String approverUserId : body.approverUserIds()) {
                request.addStep(ApprovalStep.builder()
                    .orderIndex(order++)
                    .approverUserId(approverUserId)
                    .status(ApprovalStepStatus.PENDING)
                    .build());
            }
        }
        // routed: steps stay empty until submit() resolves the routing rule.

        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper, evaluator, requesterUserId);
    }

    @Transactional
    public ApprovalRequestResponse submit(String requesterUserId, UUID id) {
        ApprovalRequest request = loadOwn(requesterUserId, id);
        if (request.getSteps().isEmpty() && request.getCategory() != null) {
            resolveRouting(request);
        }
        requestStateMachine.markSubmitted(request);
        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper, evaluator, requesterUserId);
    }

    /**
     * WF-ROUTE-001/002 — resolves the step chain from the matching {@link RoutingRule} exactly
     * once, BEFORE {@code requestStateMachine.markSubmitted} runs. A miss throws
     * {@link NoMatchingRoutingRuleException} (→ 422) here, so the transaction commits no state
     * change and the request stays DRAFT (fail-closed, never a silent default chain).
     */
    private void resolveRouting(ApprovalRequest request) {
        RoutingRule rule = routingRuleRepository
            .findMatches(request.getCategory(), request.getAmount())
            .stream().findFirst()
            .orElseThrow(() -> new NoMatchingRoutingRuleException(
                "no routing rule matches category='" + request.getCategory()
                + "' amount=" + request.getAmount()));

        List<String> chain = parseChain(rule.getApproverRoleChainJson());
        int order = 0;
        for (String role : chain) {
            request.addStep(ApprovalStep.builder()
                .orderIndex(order++)
                .approverUserId(role)
                .status(ApprovalStepStatus.PENDING)
                .build());
        }
        request.setResolvedChainJson(rule.getApproverRoleChainJson());
    }

    private List<String> parseChain(String json) {
        try {
            return objectMapper.readValue(json, ROLE_CHAIN_TYPE);
        } catch (JacksonException ex) {
            throw new IllegalStateException("routing rule role chain is not valid JSON: " + json, ex);
        }
    }

    @Transactional
    public ApprovalRequestResponse cancel(String requesterUserId, UUID id) {
        ApprovalRequest request = loadOwn(requesterUserId, id);
        requestStateMachine.markCancelled(request);
        ApprovalRequest saved = requestRepository.save(request);
        return ApprovalRequestResponse.from(saved, objectMapper, evaluator, requesterUserId);
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
            .map(r -> ApprovalRequestResponse.from(r, objectMapper, evaluator, requesterUserId))
            .toList();
        return new ApprovalListResponse(items, items.size());
    }

    /**
     * P2-38a — the visibility rule now comes from {@link ApprovalActionGuards#canView},
     * the SAME predicate {@link ApprovalActionEvaluator} uses to compute
     * {@code allowedActions}. It used to be a third, independently-written copy of the
     * policy expressed as JPQL ({@code ApprovalRequestRepository.findVisibleTo}), which
     * has been DELETED — a repository-level policy that only one caller used and that no
     * other surface could consult was exactly the drift risk this item closes.
     *
     * <p>Invisible → 404, never 403: the existence of another user's request must not be
     * disclosed (WF-AUTHZ-002, IDOR-safe).
     */
    @Transactional(readOnly = true)
    public ApprovalRequestResponse getVisible(String userId, UUID id) {
        ApprovalRequest req = requestRepository.findById(id)
            .orElseThrow(() -> new ApprovalRequestNotFoundException(id));
        if (!guards.canView(req, userId)) {
            throw new ApprovalRequestNotFoundException(id);
        }
        return ApprovalRequestResponse.from(req, objectMapper, evaluator, userId);
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

        // P2-38a — one predicate (isActionable), two failure MESSAGES. The distinction
        // between "already decided" and "never sent" is reporting, not policy; the policy
        // itself is single-valued and is what ApprovalActionEvaluator consults.
        if (!guards.isActionable(request)) {
            if (request.getStatus().isTerminal()) {
                throw new RequestTerminalException(
                    "request " + requestId + " is already " + request.getStatus());
            }
            throw new RequestTerminalException(
                "request " + requestId + " is not SUBMITTED (current: " + request.getStatus() + ")");
        }

        ApprovalStep target = request.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new ApprovalRequestNotFoundException(stepId));

        if (!guards.isAssignedApprover(target, actorUserId)) {
            // R83 iter1 F8 — the message MUST NOT name the assigned approver. A
            // non-approver who can already enumerate stepId would otherwise learn
            // WHO the rightful approver is, which is a cross-user PII leak in a
            // domain where step ownership is policy-sensitive (compensation
            // approvals, HR actions, anonymous-style review flows).
            throw new NotApproverException(
                "step " + stepId + " is not assigned to the caller");
        }

        // Strict ordering: every step with orderIndex < target.orderIndex must be APPROVED.
        // P2-38a — the DECISION is guards.isNextActionableStep; the loop below only
        // recovers WHICH earlier step blocked, for the message.
        if (!guards.isNextActionableStep(request, target)) {
            ApprovalStep blocker = request.getSteps().stream()
                .filter(s -> s.getOrderIndex() < target.getOrderIndex())
                .filter(s -> s.getStatus() != ApprovalStepStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "isNextActionableStep denied but no blocking step found"));
            throw new StepOutOfOrderException(
                "earlier step (orderIndex=" + blocker.getOrderIndex()
                + ") is " + blocker.getStatus() + "; expected APPROVED before acting on this step");
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
        return ApprovalRequestResponse.from(saved, objectMapper, evaluator, actorUserId);
    }

    private ApprovalRequest loadOwn(String requesterUserId, UUID id) {
        ApprovalRequest req = requestRepository.findById(id)
            .orElseThrow(() -> new ApprovalRequestNotFoundException(id));
        if (!guards.isRequester(req, requesterUserId)) {
            throw new ApprovalRequestNotFoundException(id);  // 404 — IDOR-safe
        }
        return req;
    }

    /**
     * WF-STEP-004 + WF-STEP-005 — closes two P1/P2 dogfood findings on R31-iter1:
     * <ul>
     *   <li>Duplicate approver in the list → DUPLICATE_APPROVER 400.</li>
     *   <li>Requester appears in their own approver list → SELF_APPROVE_FORBIDDEN 400
     *       (unless {@code approval-workflow.allow-self-approve=true}).</li>
     * </ul>
     */
    private void validateApprovers(String requesterUserId, List<String> approverUserIds) {
        Set<String> seen = new HashSet<>();
        for (String id : approverUserIds) {
            if (!seen.add(id)) {
                throw new DuplicateApproverException(
                    "approver '" + id + "' appears more than once in the approver list");
            }
            if (!properties.isAllowSelfApprove() && id.equals(requesterUserId)) {
                throw new SelfApproveForbiddenException(
                    "requester '" + requesterUserId + "' cannot appear in their own approver list "
                    + "(set approval-workflow.allow-self-approve=true to override)");
            }
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("payload is not JSON-serializable", ex);
        }
    }
}
