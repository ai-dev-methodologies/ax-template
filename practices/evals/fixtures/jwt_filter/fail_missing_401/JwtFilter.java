package fixtures.jwt_filter.fail_missing_401;

// FIXTURE: fail_missing_401
// EXPECTED_VIOLATION: INVALID_JWT_NOT_RETURNING_401_PROBLEM_DETAIL
// RULE: error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
//       auth-asvs-l1.yaml#ASVS-2.7.1
//
// This filter violates the rule: on an invalid/expired JWT it calls
// chain.doFilter() and continues rather than halting with 401 + ProblemDetail.
// An integration test asserting invalid JWT → HTTP 401 application/problem+json
// will FAIL against this fixture.

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // (validation logic omitted for brevity)
                validateToken(token);
            } catch (RuntimeException ex) {
                // VIOLATION: swallows validation error, allows request to continue
                logger.warn("Invalid JWT: " + ex.getMessage());
                // Should: response.setStatus(401); write ProblemDetail; return;
            }
        }
        chain.doFilter(request, response);  // VIOLATION: continues on invalid JWT
    }

    private void validateToken(String token) {
        // (stub)
    }
}
