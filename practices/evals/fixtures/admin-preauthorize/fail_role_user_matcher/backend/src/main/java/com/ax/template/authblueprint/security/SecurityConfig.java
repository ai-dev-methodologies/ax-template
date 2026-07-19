package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * fail_role_user_matcher SecurityConfig — grants the admin surface to
 * ROLE_USER (NOT ROLE_ADMIN). The hardened guard parses the required authority
 * and refuses to credit a non-admin authority as coverage for an admin
 * controller.
 */
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_USER")
                .anyRequest().denyAll());
        return http.build();
    }
}
