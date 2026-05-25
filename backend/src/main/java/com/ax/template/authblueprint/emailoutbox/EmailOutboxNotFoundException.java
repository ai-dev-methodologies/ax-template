package com.ax.template.authblueprint.emailoutbox;

import java.util.UUID;

public class EmailOutboxNotFoundException extends RuntimeException {
    public EmailOutboxNotFoundException(UUID id) {
        super("email outbox not found: " + id);
    }
}
