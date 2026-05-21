package com.ax.template.authblueprint.approvalworkflow;

import java.util.List;

public record ApprovalListResponse(List<ApprovalRequestResponse> items, long totalElements) {}
