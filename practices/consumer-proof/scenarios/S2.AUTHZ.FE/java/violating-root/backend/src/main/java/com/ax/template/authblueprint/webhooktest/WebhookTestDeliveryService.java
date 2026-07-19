package com.ax.template.authblueprint.webhooktest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * VIOLATING FIXTURE — S2.AUTHZ.FE additional requirement (SSRF).
 *
 * ADDITIONAL REQUIREMENT this dogfood cell also names: a rule/guard requiring
 * outbound SSRF URL-allowlist validation BEFORE server-side fetch of a
 * user-supplied URL. Confirmed absent from the catalog by
 * practices/consumer-proof/engine/canary-gaps.yaml CANARY-005 ("A rule or
 * guard requiring an SSRF allowlist check on webhook subscription target
 * URLs at registration time... grep -rliE 'ssrf|url.?allowlist|allowlist.?url'
 * practices/rules/*.md practices/evals/*.sh — 0 matches"). This fixture shows
 * the SAME gap at the OTHER call site CANARY-005 does not cover: not
 * REGISTRATION time (storing the URL) but TEST-DELIVERY time (an admin
 * clicking "Send test delivery" causes the server to fetch the STORED
 * target URL again, right now, on demand) — the endpoint's targetUrl was
 * validated (or not) whenever it was originally registered, but nothing
 * re-validates it before this on-demand fetch, and a URL that was benign at
 * registration time can be repointed at an internal host by DNS rebinding
 * between then and now. Real defect class: SSRF (OWASP API Security Top 10
 * 2023, API7:2023 Server Side Request Forgery).
 */
@Service
public class WebhookTestDeliveryService {

    private final RestTemplate restTemplate;
    private final WebhookTestEndpointLookup endpointLookup;

    public WebhookTestDeliveryService(RestTemplate restTemplate, WebhookTestEndpointLookup endpointLookup) {
        this.restTemplate = restTemplate;
        this.endpointLookup = endpointLookup;
    }

    /**
     * sendTestDelivery — fetches the endpoint's stored target URL directly.
     * NO allowlist check precedes the outbound call: a targetUrl that now
     * resolves to an internal host (169.254.169.254, 127.0.0.1, a private
     * RFC 1918 range, a cloud metadata endpoint) is fetched exactly like any
     * other URL.
     */
    public String sendTestDelivery(String endpointId) {
        String targetUrl = endpointLookup.resolveTargetUrl(endpointId);
        return restTemplate.getForObject(targetUrl, String.class);
    }
}
