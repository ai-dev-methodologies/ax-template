/**
 * @ax-template-meta
 * template_id: backend/security/SecurityConfigBase
 * layer: backend-cross-cutting
 * anchors_rule: security-stateless-session-policy.md (PRACTICES-SECURITY-001)
 *               security-csrf-scoped-disable.md
 *               security-default-headers.md
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Security Reference — Session Management (SessionCreationPolicy.STATELESS)"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html"
 *   - source_type: external
 *     citation: "Spring Security Reference — OAuth2 Resource Server / JWT"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html"
 *   - source_type: external
 *     citation: "Spring Security Reference — CORS"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Extend this class in your application's SecurityConfig:
 *
 *     @Configuration
 *     @EnableWebSecurity
 *     @EnableMethodSecurity
 *     public class SecurityConfig extends SecurityConfigBase {
 *
 *         public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
 *             super(jwtFilter);
 *         }
 *
 *         @Bean
 *         SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *             applyBase(http);
 *             http.authorizeHttpRequests(auth -> auth
 *                 .requestMatchers("/api/items/**").authenticated()
 *                 .anyRequest().denyAll()
 *             );
 *             return http.build();
 *         }
 *     }
 *
 *   Override allowedOrigins(), allowedMethods() to adjust CORS for your environment.
 */
package com.example.app.security;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Base Spring Security configuration for JWT / bearer-token APIs.
 *
 * <p>Mandates:
 * <ul>
 *   <li>{@code SessionCreationPolicy.STATELESS} — prevents JSESSIONID cookie issuance
 *   <li>CSRF disabled for {@code /api/**} (stateless JWT APIs are not vulnerable to CSRF)
 *   <li>HSTS + frame-options security headers
 *   <li>{@link JwtAuthenticationFilter} registered before {@link UsernamePasswordAuthenticationFilter}
 * </ul>
 *
 * <p>Rule reference: PRACTICES-SECURITY-001 (stateless session policy).
 */
public abstract class SecurityConfigBase {

    private final JwtAuthenticationFilter jwtFilter;

    protected SecurityConfigBase(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Applies the base security configuration to the given {@link HttpSecurity}.
     * Call this from your application's {@code @Bean SecurityFilterChain} method
     * before adding domain-specific {@code authorizeHttpRequests} rules.
     *
     * @param http the {@link HttpSecurity} to configure
     * @throws Exception if configuration fails
     */
    protected void applyBase(HttpSecurity http) throws Exception {
        http
            // STATELESS: no JSESSIONID cookie (PRACTICES-SECURITY-001)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // CSRF: disabled for /api/** — stateless JWT API, not cookie-session
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            // CORS: configure via allowedOrigins() / allowedMethods() overrides
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Security headers: HSTS + deny framing
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000)))
            // JWT filter: validate Bearer token before password auth filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * CORS configuration source. Override to adjust origins, methods, or headers
     * for your deployment environment.
     */
    protected CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins());
        config.setAllowedMethods(allowedMethods());
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Override to restrict allowed CORS origins for your environment.
     * Default: localhost dev origins.
     */
    protected List<String> allowedOrigins() {
        return List.of("http://localhost:3000", "http://localhost:5173");
    }

    /**
     * Override to adjust allowed CORS HTTP methods.
     */
    protected List<String> allowedMethods() {
        return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
