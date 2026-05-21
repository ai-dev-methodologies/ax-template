package com.ax.template.authblueprint.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servlet filter that authenticates requests carrying an {@code X-API-Key} header.
 *
 * <p>Trace:
 * <ul>
 *   <li>KEY-AUTHN-002 — populates {@link SecurityContextHolder} with an
 *       {@link ApiKeyAuthenticationToken} carrying the key's owner as principal
 *       and the key's scopes as authorities.</li>
 *   <li>KEY-AUTHN-003 — silently does nothing on invalid / revoked / expired
 *       keys so the standard Spring Security 401 chain fires.</li>
 *   <li>KEY-AUTHZ-001 — explicitly does not run on the management surface
 *       (POST/GET/DELETE /api/api-keys, /api/api-keys/{id}, /api/api-keys/{id}/rotate)
 *       so a leaked key cannot authenticate itself to rotate or revoke.</li>
 *   <li>KEY-AUTHZ-003 — scopes are mapped to authorities via
 *       {@link ApiKeyScope#toAuthority()} so {@code @PreAuthorize} on
 *       downstream endpoints (e.g. {@link ScopeProbeController}) can gate WRITE.</li>
 * </ul>
 *
 * <p>If a Bearer JWT has already populated the SecurityContext, this filter is
 * a no-op — JWT wins per manifest {@code authentication.jwt_takes_precedence}.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    public static final String HEADER = "X-API-Key";

    /**
     * Paths that MUST be reached only via JWT (KEY-AUTHZ-001):
     *   /api/api-keys
     *   /api/api-keys/{uuid}
     *   /api/api-keys/{uuid}/rotate
     *
     * The scope-probe surface ({@code /api/api-keys/scope-probe}) is intentionally
     * NOT in this list — that endpoint is exactly where the test exercises
     * X-API-Key + scope authority.
     */
    private static final Pattern MANAGEMENT_PATH = Pattern.compile(
        "^/api/api-keys" +
        "(?:/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?:/rotate)?)?$"
    );

    private final ApiKeyService service;

    public ApiKeyAuthenticationFilter(ApiKeyService service) {
        this.service = service;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return MANAGEMENT_PATH.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        boolean alreadyAuthenticated =
            existing != null
            && existing.isAuthenticated()
            && !(existing instanceof AnonymousAuthenticationToken);
        if (alreadyAuthenticated) {
            // JWT took precedence per manifest — do not re-authenticate.
            chain.doFilter(request, response);
            return;
        }

        String headerValue = request.getHeader(HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<ApiKey> matched = service.resolvePlaintext(headerValue);
        if (matched.isEmpty()) {
            // KEY-AUTHN-003 — do not throw, do not respond. The downstream chain's
            // standard AuthenticationEntryPoint emits the 401 for protected paths;
            // permitAll paths still see no Authentication.
            chain.doFilter(request, response);
            return;
        }

        ApiKey key = matched.get();
        Set<ApiKeyScope> scopes = key.getScopes();
        List<GrantedAuthority> authorities = scopes.stream()
            .map(ApiKeyScope::toAuthority)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableList());

        ApiKeyAuthenticationToken token =
            new ApiKeyAuthenticationToken(key.getOwnerUserId(), key.getId(), authorities);
        SecurityContextHolder.getContext().setAuthentication(token);

        try {
            chain.doFilter(request, response);
        } finally {
            // Best-effort touch of lastUsedAt. Failures here MUST NOT propagate —
            // the request already succeeded.
            try {
                service.touchLastUsed(key.getId());
            } catch (RuntimeException ex) {
                LOG.debug("api-key: lastUsedAt touch failed for {} — {}", key.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Authentication carrier used when a request is authenticated by an X-API-Key.
     * {@link #getName()} returns the key's owner userId, mirroring the JWT subject
     * claim so downstream code can treat both auth flavors uniformly.
     */
    public static final class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private final String ownerUserId;
        private final java.util.UUID keyId;

        public ApiKeyAuthenticationToken(String ownerUserId,
                                         java.util.UUID keyId,
                                         java.util.Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.ownerUserId = ownerUserId;
            this.keyId = keyId;
            super.setAuthenticated(true);
        }

        @Override
        public Object getCredentials() { return ""; }

        @Override
        public Object getPrincipal() { return ownerUserId; }

        @Override
        public String getName() { return ownerUserId; }

        public java.util.UUID getKeyId() { return keyId; }
    }
}
