package com.ax.template.authblueprint.approvalworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoutingRuleResponse(
    UUID id,
    String categoryOrDept,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    List<String> approverRoleChain,
    Instant createdAt
) {

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    public static RoutingRuleResponse from(RoutingRule r, ObjectMapper objectMapper) {
        return new RoutingRuleResponse(
            r.getId(),
            r.getCategoryOrDept(),
            r.getMinAmount(),
            r.getMaxAmount(),
            parseChain(r.getApproverRoleChainJson(), objectMapper),
            r.getCreatedAt()
        );
    }

    static List<String> parseChain(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
