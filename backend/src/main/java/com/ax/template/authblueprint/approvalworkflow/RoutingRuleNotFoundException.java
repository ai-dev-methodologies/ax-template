package com.ax.template.authblueprint.approvalworkflow;

import java.util.UUID;

public class RoutingRuleNotFoundException extends RuntimeException {
    public RoutingRuleNotFoundException(UUID id) {
        super("routing rule not found: " + id);
    }
}
