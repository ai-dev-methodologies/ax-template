package fixtures.jwt_filter.pass;

// FIXTURE: pass
// PATTERN: JwtAuthenticationFilter usage — invalid JWT returns HTTP 401 + ProblemDetail
//          PASSES ASVS-2.7.1, PRACTICES-ERR-002
//
// This fixture shows correct usage: the filter is JwtAuthenticationFilter from the
// template, which correctly returns 401 + application/problem+json on JwtException.
// See templates/backend/security/JwtAuthenticationFilter.java for implementation.

import com.example.app.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

// CORRECT: JwtAuthenticationFilter handles JwtException → HTTP 401 ProblemDetail
// Usage reference (bean wiring in SecurityConfig):
//
// @Bean
// SecurityFilterChain chain(HttpSecurity http,
//                           JwtAuthenticationFilter jwtFilter) throws Exception {
//     http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//     ...
//     return http.build();
// }
//
// The JwtAuthenticationFilter implementation:
//   try { Jwt jwt = jwtDecoder.decode(token); setAuthentication(jwt); chain.doFilter(...); }
//   catch (JwtException ex) { writeUnauthorized(request, response, ex.getMessage()); }
//
// This satisfies ASVS 2.7.1 — invalid tokens MUST return 401, not silently pass through.

@Component
class FilterWiring {
    // bean is JwtAuthenticationFilter — no custom wrapping needed
    private final JwtAuthenticationFilter filter;

    FilterWiring(JwtAuthenticationFilter filter) {
        this.filter = filter;
    }
}
