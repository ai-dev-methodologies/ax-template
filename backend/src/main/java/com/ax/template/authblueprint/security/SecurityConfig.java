package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/practices/demo/**"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/mappings").permitAll()
                // PAYMENT-AUTHZ-004: all /api/admin/** paths require ROLE_ADMIN —
                // including the reconciliation heartbeat. The recon endpoint emits
                // append-only ledger events (RECONCILIATION_DRIFT) plus increments
                // observability counters; allowing unauthenticated access would let
                // an adversary pollute the ledger and metric stream without audit.
                // P5 security-review finding (US-014, HIGH): tightened from permitAll.
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/items/**").authenticated()
                .requestMatchers("/api/payments/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/email/password-change").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/oauth/link").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/auth/oauth/unlink/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/practices/demo/**").permitAll()
                .requestMatchers("/api/ratelimit/**").permitAll()
                .anyRequest().denyAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                // PAYMENT-SEC-003: HSTS — strong cryptography for cardholder data transit
                // per PCI-DSS 4.1. Production TLS termination at load balancer; this header
                // documents the policy and is asserted by PaymentSecurityTest.
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    // PAYMENT-SEC-003: enable HSTS over plain HTTP in test mode so the
                    // SecurityConfig contract is exercised. Production load balancers
                    // terminate TLS; the header documents the requires-secure policy.
                    .requestMatcher(req -> true))
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) return Collections.emptyList();
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
