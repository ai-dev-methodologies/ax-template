package com.ax.template.authblueprint.approvalworkflow;

/** WF-ROUTE-002 — fail-closed: no routing rule covers the request's (category, amount). */
public class NoMatchingRoutingRuleException extends RuntimeException {
    public NoMatchingRoutingRuleException(String message) {
        super(message);
    }
}
