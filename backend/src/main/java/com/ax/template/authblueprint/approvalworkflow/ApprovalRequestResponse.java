package com.ax.template.authblueprint.approvalworkflow;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ApprovalRequestResponse(
    UUID id,
    String requesterUserId,
    String type,
    String title,
    ApprovalRequestStatus status,
    Map<String, Object> payload,
    List<ApprovalStepResponse> steps,
    Instant createdAt,
    Instant submittedAt,
    Instant completedAt,
    String category,
    BigDecimal amount,
    List<String> resolvedChain,
    /**
     * P2-38b — the actions THIS caller may invoke, computed server-side by
     * {@link ApprovalActionEvaluator} from the very predicates {@link ApprovalService}
     * enforces. Clients no longer re-derive authorization from raw fields.
     *
     * <p>Empty (never null) when the caller may do nothing. A caller-independent
     * projection (e.g. an internal fan-out) passes {@code null} for the caller and gets
     * an empty list — absence of a claim, not a claim of absence.
     */
    List<String> allowedActions
) {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * Caller-agnostic projection — {@code allowedActions} is empty. Retained so internal
     * callers that have no principal keep compiling; every HTTP path uses the
     * caller-aware overload below.
     */
    public static ApprovalRequestResponse from(ApprovalRequest r, ObjectMapper objectMapper) {
        return from(r, objectMapper, null, null);
    }

    public static ApprovalRequestResponse from(ApprovalRequest r, ObjectMapper objectMapper,
                                               ApprovalActionEvaluator evaluator, String callerUserId) {
        Map<String, Object> payload = parse(r.getPayloadJson(), objectMapper);
        List<ApprovalStepResponse> steps = r.getSteps().stream()
            .map(ApprovalStepResponse::from)
            .toList();
        return new ApprovalRequestResponse(
            r.getId(),
            r.getRequesterUserId(),
            r.getType(),
            r.getTitle(),
            r.getStatus(),
            payload,
            steps,
            r.getCreatedAt(),
            r.getSubmittedAt(),
            r.getCompletedAt(),
            r.getCategory(),
            r.getAmount(),
            RoutingRuleResponse.parseChain(r.getResolvedChainJson(), objectMapper),
            evaluator == null ? List.of() : evaluator.allowedActions(r, callerUserId)
        );
    }

    private static Map<String, Object> parse(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, MAP_TYPE);
        } catch (JacksonException ex) {
            return Map.of();
        }
    }
}
