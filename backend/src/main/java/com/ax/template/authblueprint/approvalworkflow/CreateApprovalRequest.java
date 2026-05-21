package com.ax.template.authblueprint.approvalworkflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateApprovalRequest(
    @NotBlank @Size(max = 64) String type,
    @Size(max = 128) String title,
    Map<String, Object> payload,
    @NotEmpty @Size(min = 1, max = 10) List<@NotBlank String> approverUserIds
) {}
