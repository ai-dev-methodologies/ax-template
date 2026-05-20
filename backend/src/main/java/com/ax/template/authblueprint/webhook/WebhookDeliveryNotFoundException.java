package com.ax.template.authblueprint.webhook;

import java.util.UUID;

public class WebhookDeliveryNotFoundException extends RuntimeException {
    public WebhookDeliveryNotFoundException(UUID id) {
        super("Webhook delivery not found: id=" + id);
    }
}
