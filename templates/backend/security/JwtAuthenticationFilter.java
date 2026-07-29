/**
 * @ax-template-meta
 * template_id: backend/security/JwtAuthenticationFilter
 * layer: backend-cross-cutting
 * anchors_rule: security-stateless-session-policy.md
 *               error-rfc7807-problem-detail.md
 * anchors_note: the previous value claimed auth-asvs-l1.yaml#ASVS-2.7.1, an id this spec
 *   does not define (its ids are ASVS-V<n>.<n>.<n>) AND a requirement — ASVS 4.0 2.7.1 is
 *   Out-of-Band Verifier — that has nothing to do with a bearer-token filter. It went
 *   unnoticed because _check-anchors.py only looked for *.md tokens and so checked
 *   nothing here. Repointed to the catalog rule this filter actually embodies.
 *   BACKLOG P3-94 (2026-07-30) closes the second half — the evidence CONTENT. The
 *   mis-scoped ASVS 2.7.1 citation is replaced by an in-repo-verifiable anchor
 *   (specs/auth-asvs-l1.yaml + the ASVS-tagged tests that bind it), NOT by another
 *   ASVS section quoted from memory. Scope, stated honestly — this repo's ASVS spec pins
 *   NO per-request token-validation requirement, its V3 Session Management items being
 *   V3.1.1/V3.2.1/V3.3.1/V3.4.1-4/V3.7.1 (token-in-URL, rotation on auth, logout,
 *   cookie attributes, re-auth), none of which is about validating a bearer token on
 *   each request. So the ASVS attribution is made only for what the spec really does
 *   pin — the fail-secure outcome — and the signature/expiry/claims MECHANICS stay
 *   anchored to the Spring Security reference already cited below, which is their
 *   actual source. Over-claiming an ASVS § for the mechanics is the defect P3-94 is.
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: internal
 *     rationale: "ASVS attribution re-anchored to this repo's canonical ASVS record,
 *       specs/auth-asvs-l1.yaml (version 4.0.3), which pins the two items this filter's
 *       control flow actually realises — verbatim from the spec: ASVS-V4.1.5 'Verify that
 *       access controls fail securely.' (notes 'API test: Deny by default') is the invalid-token
 *       branch, which answers 401 and does NOT continue the chain; ASVS-V4.1.1 'Verify
 *       that the application enforces access control rules on a trusted service layer.'
 *       is why the decision is made here, server-side, rather than trusted from the
 *       client. Both are bound to executable checks in the reference workload:
 *       AuthMeAsvsTest#asvs_V4_1_5_accessControlFailsSecurely (@Tag ASVS-V4.1.5) sends
 *       'Authorization: Bearer tampered.token.value' and asserts 401 — literally this
 *       filter's JwtException branch — and #asvs_V4_1_1_accessControlOnTrustedLayer
 *       (@Tag ASVS-V4.1.1) sends no header and asserts 401, which is the pass-through
 *       branch plus the downstream .authenticated() deny. NOT claimed: that ASVS
 *       prescribes JWT signature/expiry/claim validation — see anchors_note."
 *   - source_type: external
 *     citation: "Spring Security Reference — JWT Authentication"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html"
 *   - source_type: external
 *     citation: "RFC 7807 Problem Details for HTTP APIs"
 *     url: "https://datatracker.ietf.org/doc/html/rfc7807"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Wire into SecurityConfigBase:
 *     super(jwtAuthenticationFilter);
 *   Inject JwtDecoder via constructor — Spring Boot autoconfigures it when
 *   spring.security.oauth2.resourceserver.jwt.jwk-set-uri is set.
 *   On invalid/expired JWT this filter returns HTTP 401 with a ProblemDetail body.
 */
package com.example.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — validates Bearer tokens and sets Spring Security context.
 *
 * <p>Contract:
 * <ul>
 *   <li>On valid JWT: extracts {@code sub} (userId) and {@code role} claim, sets
 *       {@link SecurityContextHolder} authentication
 *   <li>On invalid / expired JWT: returns HTTP 401 with {@code application/problem+json}
 *       body (RFC 7807) — does NOT continue the filter chain
 *   <li>On missing Authorization header: passes through — downstream security
 *       rules ({@code .authenticated()}) will return 401 if the endpoint requires auth
 * </ul>
 *
 * <p>Signature, expiry and required-claim validation is {@link JwtDecoder}'s contract
 * (Spring Security Reference — JWT Authentication, cited above); this filter's own
 * invariant is the OUTCOME: failed validation must return 401, never 200. That outcome
 * is the repo's ASVS-V4.1.5 item ("Verify that access controls fail securely.",
 * specs/auth-asvs-l1.yaml), asserted by
 * {@code AuthMeAsvsTest#asvs_V4_1_5_accessControlFailsSecurely}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token — continue to let downstream security rules decide
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Jwt jwt = jwtDecoder.decode(token);
            setAuthentication(jwt);
            chain.doFilter(request, response);

        } catch (JwtException ex) {
            // ASVS-V4.1.5 (fail securely): invalid JWT MUST return 401, not silently continue
            log.warn("JWT validation failure [{}]: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(request, response, ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setAuthentication(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : Collections.emptyList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String detail) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        pd.setType(URI.create("https://api.example.com/problems/unauthorized"));
        pd.setTitle("Unauthorized");
        pd.setInstance(URI.create(request.getRequestURI()));

        // Carry correlationId from CorrelationIdFilter if available
        Object correlationId = request.getAttribute("correlationId");
        pd.setProperty("traceId", correlationId != null
                ? correlationId.toString()
                : UUID.randomUUID().toString());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}
