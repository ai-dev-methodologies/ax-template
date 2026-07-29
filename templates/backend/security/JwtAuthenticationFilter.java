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
 *   nothing here. Repointed to the catalog rule this filter actually embodies. The
 *   evidence block below still carries the same mis-scoped ASVS 2.7.1 citation; that is
 *   evidence CONTENT, outside this axis, and is reported to the backlog rather than
 *   rewritten to a section that has not been verified against the ASVS source.
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "OWASP ASVS 4.0 §2.7.1 — Out-of-Band Verifier Requirements"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
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
 * <p>ASVS 2.7.1 requirement: tokens must be validated for signature, expiry,
 * and required claims. Failed validation must return 401, not 200.
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
            // ASVS 2.7.1: invalid JWT MUST return 401, not silently continue
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
