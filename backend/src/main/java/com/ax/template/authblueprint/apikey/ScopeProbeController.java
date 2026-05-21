package com.ax.template.authblueprint.apikey;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo surface for KEY-AUTHZ-003. Two endpoints under {@code /api/api-keys/scope-probe}
 * gated by {@code @PreAuthorize} on the scope-derived authorities. The compliance test
 * uses these to verify that a READ-scope key cannot reach the WRITE endpoint.
 *
 * <p>NOTE: This path lives under {@code /api/api-keys/} so the
 * {@code ApiKeyAuthenticationFilter} explicitly DOES handle the X-API-Key header here
 * (the management-path skip applies to {@code /api/api-keys} and {@code /{id}} paths,
 * not to {@code /scope-probe}). The filter skip is checked against the full request
 * path; see {@link ApiKeyAuthenticationFilter#shouldNotFilter}.
 */
@RestController
@RequestMapping("/api/api-keys/scope-probe")
public class ScopeProbeController {

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_API_READ')")
    public Map<String, String> probeRead() {
        return Map.of("scope", "READ", "result", "ok");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_API_WRITE')")
    public Map<String, String> probeWrite() {
        return Map.of("scope", "WRITE", "result", "ok");
    }

    /**
     * Auth-agnostic identity probe used by the KEY-AUTHN-002 / KEY-LIFECYCLE-001/002
     * compliance tests. The existing {@code /api/auth/me} endpoint reads from a
     * {@code Jwt} principal and therefore NPEs when called via X-API-Key (the
     * SecurityContext carries an {@code ApiKeyAuthenticationToken}, not a Jwt).
     * This endpoint uses {@link Authentication#getName()} so it works for any
     * auth flavor — JWT or API key.
     */
    @GetMapping("/whoami")
    public Map<String, String> whoami(Authentication auth) {
        return Map.of("userId", auth.getName());
    }
}
