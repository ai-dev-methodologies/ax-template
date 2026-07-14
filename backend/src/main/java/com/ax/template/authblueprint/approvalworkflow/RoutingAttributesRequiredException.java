package com.ax.template.authblueprint.approvalworkflow;

/** WF-ROUTE-001 — a request must carry EITHER direct approverUserIds OR (category + amount). */
public class RoutingAttributesRequiredException extends RuntimeException {
    public RoutingAttributesRequiredException(String message) {
        super(message);
    }
}
