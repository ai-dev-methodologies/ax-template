package com.ax.template.authblueprint.approvalworkflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * {@code approverUserIds} (direct mode) and {@code category}/{@code amount} (WF-ROUTE-001
 * routing mode) are mutually exclusive input paths; {@link ApprovalService#create} requires
 * exactly one to be present (a request with neither is rejected — see
 * {@link RoutingAttributesRequiredException}).
 */
public record CreateApprovalRequest(
    @NotBlank @Size(max = 64) String type,
    @Size(max = 128) String title,
    Map<String, Object> payload,
    @Size(max = 10) List<@NotBlank String> approverUserIds,
    @Size(max = 64) String category,
    BigDecimal amount
) {}
