package fixtures.security_config.fail_no_stateless_session;

// FIXTURE: fail_no_stateless_session
// EXPECTED_VIOLATION: SESSION_POLICY_NOT_STATELESS
// RULE: security-stateless-session-policy.md (PRACTICES-SECURITY-001)
//
// This config violates the rule: it omits SessionCreationPolicy.STATELESS.
// Without it, Spring Security defaults to IF_REQUIRED — every successful
// authentication issues a JSESSIONID cookie the JWT API never agreed to manage.
// A guard asserting sessionCreationPolicy(STATELESS) will FAIL against this fixture.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            // VIOLATION: no .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}
