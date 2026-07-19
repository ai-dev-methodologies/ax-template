package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * fail_administrator_path SecurityConfig — protects "/api/admin/**" with
 * ROLE_ADMIN. This must NOT be credited to a controller mapped under
 * "/api/administrator" (a distinct path segment), which boundary-aware Ant
 * matching correctly rejects.
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
