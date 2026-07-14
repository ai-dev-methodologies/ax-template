package com.ax.template.authblueprint.approvalworkflow;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateRoutingRuleRequest(
    @NotBlank @Size(max = 64) String categoryOrDept,
    @NotNull @DecimalMin(value = "0.00") BigDecimal minAmount,
    BigDecimal maxAmount,
    @NotEmpty @Size(min = 1, max = 10) List<@NotBlank String> approverRoleChain
) {}
