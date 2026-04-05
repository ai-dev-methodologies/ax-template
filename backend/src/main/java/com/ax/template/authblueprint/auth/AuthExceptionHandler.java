package com.ax.template.authblueprint.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleInvalidCredentials(InvalidCredentialsException e) {
        return Map.of("message", "Invalid credentials");
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleRateLimit(RateLimitException e) {
        return Map.of("message", "Too many login attempts. Please try again later.");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleEmailNotVerified(EmailNotVerifiedException e) {
        return Map.of("message", "Email not verified");
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidToken(InvalidTokenException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleInvalidOAuthState(InvalidOAuthStateException e) {
        return Map.of("message", "Invalid OAuth state");
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleProviderUnavailable(ProviderUnavailableException e) {
        return Map.of("message", "Provider temporarily unavailable: " + e.getProvider(), "fallback", "email");
    }

}
