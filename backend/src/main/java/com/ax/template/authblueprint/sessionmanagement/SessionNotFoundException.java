package com.ax.template.authblueprint.sessionmanagement;

import java.util.UUID;

/** SESS-AUTHZ-002 — mapped to HTTP 404. */
public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(UUID id) {
        super("session not found: " + id);
    }
}
