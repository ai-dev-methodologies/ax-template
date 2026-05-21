package com.ax.template.authblueprint.approvalworkflow;

import jakarta.validation.constraints.Size;

public record StepActionRequest(@Size(max = 1024) String comment) {}
