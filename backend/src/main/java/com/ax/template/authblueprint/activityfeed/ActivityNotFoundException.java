package com.ax.template.authblueprint.activityfeed;

import java.util.UUID;

/** ACT-AUTHZ-002 — mapped to HTTP 404. */
public class ActivityNotFoundException extends RuntimeException {
    public ActivityNotFoundException(UUID id) {
        super("activity not found or not visible: " + id);
    }
}
