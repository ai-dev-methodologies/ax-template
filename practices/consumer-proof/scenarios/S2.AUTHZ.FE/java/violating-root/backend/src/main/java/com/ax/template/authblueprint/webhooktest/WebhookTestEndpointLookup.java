package com.ax.template.authblueprint.webhooktest;

import org.springframework.stereotype.Component;

/**
 * Fixture stand-in for "look up the stored target URL of a registered
 * webhook endpoint by id" — kept trivial on purpose, this scenario is not
 * about the lookup, it is about what happens to the URL AFTER lookup.
 */
@Component
public class WebhookTestEndpointLookup {

    public String resolveTargetUrl(String endpointId) {
        // In the real domain this reads WebhookEndpoint.getUrl() via
        // WebhookEndpointRepository; stubbed here to keep the fixture focused.
        return "https://example-consumer.invalid/webhooks/" + endpointId;
    }
}
