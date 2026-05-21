package com.ax.template.authblueprint.approvalworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    Instant completedAt
) {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public static ApprovalRequestResponse from(ApprovalRequest r, ObjectMapper objectMapper) {
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
            r.getCompletedAt()
        );
    }

    private static Map<String, Object> parse(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }
}
