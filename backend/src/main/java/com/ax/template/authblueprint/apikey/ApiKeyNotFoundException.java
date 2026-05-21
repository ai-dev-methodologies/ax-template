package com.ax.template.authblueprint.apikey;

import java.util.UUID;

/** KEY-AUTHZ-002 — mapped to HTTP 404 by the controller. */
public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(UUID id) {
        super("api key not found: " + id);
    }
}
