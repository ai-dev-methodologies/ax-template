package com.ax.template.authblueprint.webhooktest;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * HAND-ROLLED — capability-gap signal (no catalog SPI/primitive covers this;
 * see canary-gaps.yaml CANARY-005). A minimal, fail-closed SSRF guard: rejects
 * a URL unless (a) its scheme is https, and (b) the host it resolves to is
 * NOT a loopback / link-local / site-local (RFC 1918) / any-local address —
 * this is the same class of address OWASP's SSRF cheat sheet and CANARY-005's
 * own example (169.254.169.254 cloud metadata) name as the classic bypass
 * target. A real fork-receiver implementation would also need an explicit
 * host allowlist/DNS-pinning story; this fixture's job is only to prove the
 * MISSING-CHECK defect and its guard, not to ship a production-grade
 * validator.
 */
@Component
public class UrlAllowlistValidator {

    public void assertAllowed(String url) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("SSRF_URL_REJECTED: scheme must be https: " + url);
        }
        try {
            InetAddress addr = InetAddress.getByName(uri.getHost());
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                throw new IllegalArgumentException("SSRF_URL_REJECTED: internal/reserved host: " + url);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("SSRF_URL_REJECTED: unresolvable host: " + url, e);
        }
    }
}
