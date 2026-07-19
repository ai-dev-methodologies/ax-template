package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * pass_securityconfig SecurityConfig — protects "/api/admin/**" with
 * ROLE_ADMIN, genuinely covering ScAdminController's "/api/admin/sc/**"
 * endpoints.
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
