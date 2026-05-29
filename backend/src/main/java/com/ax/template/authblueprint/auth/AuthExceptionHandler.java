package com.ax.template.authblueprint.auth;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 9457 {@code application/problem+json} responses for the auth domain's business
 * exceptions. Mirrors the {@code common.GlobalProblemDetailAdvice} +
 * {@code payment.PaymentExceptionHandler} idiom: every body is a
 * {@link ProblemDetail} carrying a stable {@code code} extension member so clients
 * branch on {@code code} (not free-text {@code detail}).
 *
 * <p>IMW1-D (IDW2 dogfood 2026-05-29): these handlers previously returned
 * {@code Map<String,String>} bodies, which slipped past the catalog's RFC 9457
 * problem+json guards. Each handler below preserves the EXACT HTTP status and
 * human-readable text it conveyed before, now as a {@link ProblemDetail}. Because
 * each method returns {@code ProblemDetail} directly, Spring's
 * {@code ResponseEntityExceptionHandler} machinery serialises the body with the
 * {@code application/problem+json} content type and the matching status.
 *
 * <p>These are ASVS-tested auth errors — the security semantics (which status maps to
 * which failure) are preserved byte-for-byte; only the wire shape moves from a bare
 * Map to problem+json.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    private static final URI INVALID_CREDENTIALS_TYPE =
            URI.create("https://errors.example.com/auth/invalid-credentials");
    private static final URI RATE_LIMITED_TYPE =
            URI.create("https://errors.example.com/auth/rate-limited");
    private static final URI EMAIL_NOT_VERIFIED_TYPE =
            URI.create("https://errors.example.com/auth/email-not-verified");
    private static final URI INVALID_TOKEN_TYPE =
            URI.create("https://errors.example.com/auth/invalid-token");
    private static final URI INVALID_ROLE_TYPE =
            URI.create("https://errors.example.com/auth/invalid-role");
    private static final URI INVALID_REFRESH_TOKEN_TYPE =
            URI.create("https://errors.example.com/auth/invalid-refresh-token");
    private static final URI INVALID_OAUTH_STATE_TYPE =
            URI.create("https://errors.example.com/auth/invalid-oauth-state");
    private static final URI PROVIDER_UNAVAILABLE_TYPE =
            URI.create("https://errors.example.com/auth/provider-unavailable");

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_TYPE, "Invalid Credentials",
                "INVALID_CREDENTIALS", "Invalid credentials");
    }

    @ExceptionHandler(RateLimitException.class)
    public ProblemDetail handleRateLimit(RateLimitException e) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, RATE_LIMITED_TYPE, "Too Many Requests",
                "RATE_LIMITED", "Too many login attempts. Please try again later.");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException e) {
        return problem(HttpStatus.FORBIDDEN, EMAIL_NOT_VERIFIED_TYPE, "Email Not Verified",
                "EMAIL_NOT_VERIFIED", "Email not verified");
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException e) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_TOKEN_TYPE, "Invalid Token",
                "INVALID_TOKEN", e.getMessage());
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ProblemDetail handleInvalidRole(InvalidRoleException e) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_ROLE_TYPE, "Invalid Role",
                "INVALID_ROLE", e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return problem(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN_TYPE, "Invalid Refresh Token",
                "INVALID_REFRESH_TOKEN", e.getMessage());
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    public ProblemDetail handleInvalidOAuthState(InvalidOAuthStateException e) {
        return problem(HttpStatus.FORBIDDEN, INVALID_OAUTH_STATE_TYPE, "Invalid OAuth State",
                "INVALID_OAUTH_STATE", "Invalid OAuth state");
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ProblemDetail handleProviderUnavailable(ProviderUnavailableException e) {
        ProblemDetail pd = problem(HttpStatus.SERVICE_UNAVAILABLE, PROVIDER_UNAVAILABLE_TYPE,
                "Provider Unavailable", "PROVIDER_UNAVAILABLE",
                "Provider temporarily unavailable: " + e.getProvider());
        // Preserve the email-fallback hint the previous Map body carried; OAuthFullFlowTest
        // asserts on this extension member.
        pd.setProperty("fallback", "email");
        return pd;
    }

    private static ProblemDetail problem(HttpStatusCode status, URI type, String title,
                                         String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail == null ? "" : detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setProperty("code", code);
        return pd;
    }
}
