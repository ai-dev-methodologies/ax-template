package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * fail_unprotected_patch SecurityConfig — protects "/api/admin/**" with
 * ROLE_ADMIN. Covers the GET at "/api/admin/mixed" but NOT the PATCH at
 * "/api/other/mixed/{id}".
 */
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().denyAll());
        return http.build();
    }
}
