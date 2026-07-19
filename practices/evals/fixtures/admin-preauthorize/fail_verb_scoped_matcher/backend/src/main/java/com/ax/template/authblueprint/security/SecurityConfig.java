package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * fail_verb_scoped_matcher SecurityConfig — reproduces the codex round-2 HIGH
 * verb-bypass. A verb-SPECIFIC ROLE_ADMIN matcher for GET is declared FIRST,
 * then a verb-AGNOSTIC .authenticated() fallback for the SAME path. Spring
 * evaluates matchers in declared order and stops at the FIRST match:
 *
 *   .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAuthority("ROLE_ADMIN")
 *   .requestMatchers("/api/admin/**").authenticated()
 *
 * For a POST/DELETE/PUT/PATCH to /api/admin/x the GET matcher does NOT match
 * (wrong verb), so Spring falls through to the verb-agnostic .authenticated()
 * rule — which admits ANY authenticated NON-admin caller to the write path.
 * The GET matcher's ROLE_ADMIN authority is NOT in effect for those verbs.
 *
 * The pre-hardening guard extracted only the quoted path from the admin
 * matcher and ignored its HttpMethod, crediting "/api/admin/**" as admin
 * coverage for ALL verbs → FALSE PASS. The hardened guard models the verb +
 * declared order + first-match authority → the POST is uncovered → BLOCKED.
 */
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().denyAll());
        return http.build();
    }
}
