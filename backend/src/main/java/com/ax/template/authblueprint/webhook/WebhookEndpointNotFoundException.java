package com.ax.template.authblueprint.webhook;

import java.util.UUID;

public class WebhookEndpointNotFoundException extends RuntimeException {
    public WebhookEndpointNotFoundException(UUID id) {
        super("Webhook endpoint not found: id=" + id);
    }
}
