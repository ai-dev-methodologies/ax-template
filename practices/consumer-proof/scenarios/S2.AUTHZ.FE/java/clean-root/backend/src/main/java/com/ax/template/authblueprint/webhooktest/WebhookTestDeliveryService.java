package com.ax.template.authblueprint.webhooktest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * CLEAN FIXTURE — S2.AUTHZ.FE additional requirement (SSRF).
 *
 * Same "send test delivery" operation as the violating fixture, but the
 * stored target URL is re-validated against {@link UrlAllowlistValidator}
 * immediately BEFORE the outbound fetch, on every call — not only at
 * original registration time. This closes the DNS-rebinding / repoint
 * window the violating fixture is exposed to.
 */
@Service
public class WebhookTestDeliveryService {

    private final RestTemplate restTemplate;
    private final WebhookTestEndpointLookup endpointLookup;
    private final UrlAllowlistValidator urlAllowlistValidator;

    public WebhookTestDeliveryService(RestTemplate restTemplate,
                                       WebhookTestEndpointLookup endpointLookup,
                                       UrlAllowlistValidator urlAllowlistValidator) {
        this.restTemplate = restTemplate;
        this.endpointLookup = endpointLookup;
        this.urlAllowlistValidator = urlAllowlistValidator;
    }

    /**
     * sendTestDelivery — re-validates the resolved target URL against the
     * SSRF allowlist immediately before the fetch, every time this runs.
     */
    public String sendTestDelivery(String endpointId) {
        String targetUrl = endpointLookup.resolveTargetUrl(endpointId);
        urlAllowlistValidator.assertAllowed(targetUrl);
        return restTemplate.getForObject(targetUrl, String.class);
    }
}
