package com.ax.template.authblueprint.approvalworkflow;

import java.util.List;

public record ApprovalInboxResponse(List<ApprovalInboxEntry> items, long totalElements) {}
