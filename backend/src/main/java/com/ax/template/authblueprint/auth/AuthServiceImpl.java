package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserAccountDto;
import com.ax.template.authblueprint.user.UserAccountService;
import com.ax.template.authblueprint.user.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Auth flows (signup / login / refresh / verify / reset / logout). The User aggregate is
 * reached ONLY through the published {@link UserAccountService} port (AX-DDD-AUTH-USER
 * retire) — auth owns token/JWT/cookie/rate-limit policy; the {@code user} feature owns
 * account storage and credential verification.
 */
@Service
public class AuthServiceImpl {

    private final UserAccountService accounts;
    private final JwtTokenService jwtTokenService;
    private final LoginRateLimiter rateLimiter;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthService oAuthService;
    private final long graceWindowSeconds;
    private final boolean autoVerifyOnSignup;
    private final boolean allowRoleOverride;

    public AuthServiceImpl(UserAccountService accounts,
                           JwtTokenService jwtTokenService,
                           LoginRateLimiter rateLimiter,
                           VerificationTokenRepository verificationTokenRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           OAuthService oAuthService,
                           @Value("${auth.refresh.grace-window-seconds:30}") long graceWindowSeconds,
                           @Value("${auth.signup.auto-verify:false}") boolean autoVerifyOnSignup,
                           @Value("${auth.signup.allow-role-override:true}") boolean allowRoleOverride) {
        this.accounts = accounts;
        this.jwtTokenService = jwtTokenService;
        this.rateLimiter = rateLimiter;
        this.verificationTokenRepository = verificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oAuthService = oAuthService;
        this.graceWindowSeconds = graceWindowSeconds;
        this.autoVerifyOnSignup = autoVerifyOnSignup;
        this.allowRoleOverride = allowRoleOverride;
    }

    public List<Map<String, Object>> listAllUsers() {
        return accounts.listAll().stream()
                .map(u -> Map.<String, Object>of(
                        "userId", u.id().toString(),
                        "email", u.email(),
                        "role", u.role().name(),
                        "emailVerified", u.emailVerified()
                ))
                .collect(Collectors.toList());
    }

    public UserProfileResponse getProfile(UUID userId) {
        UserAccountDto user = accounts.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<String> linkedProviders = oAuthService.getLinkedProviders(userId);
        return new UserProfileResponse(
                user.id().toString(),
                user.email(),
                user.role().name(),
                user.emailVerified(),
                linkedProviders);
    }

    public SignupResponse signup(SignupRequest request) {
        String verificationToken = UUID.randomUUID().toString();

        if (!accounts.existsByEmail(request.getEmail())) {
            UserAccountDto saved = accounts.register(request.getEmail(), request.getPassword(),
                    resolveRole(request.getRole()), autoVerifyOnSignup);

            VerificationToken vt = new VerificationToken();
            vt.setToken(verificationToken);
            vt.setUserId(saved.id());
            vt.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            vt.setUsed(false);
            vt.setTokenType("VERIFY");
            verificationTokenRepository.save(vt);

            System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);

            return new SignupResponse(saved.id().toString(), "Signup successful. Check your email for verification.");
        }

        System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);
        return new SignupResponse(UUID.randomUUID().toString(), "Signup successful. Check your email for verification.");
    }

    private UserRole resolveRole(String requested) {
        if (!allowRoleOverride || requested == null || requested.isBlank()) {
            return UserRole.MEMBER;
        }
        try {
            return UserRole.valueOf(requested.toUpperCase());
        } catch (IllegalArgumentException unknownRole) {
            // IMW2-C (IDW2 dogfood): do NOT silently downgrade an UNKNOWN role to
            // MEMBER — that hides the deviation as confusing later 403s. Surface it
            // as a LOUD 400 (mapped to problem+json by AuthExceptionHandler).
            throw new InvalidRoleException("Unknown role: " + requested);
        }
    }

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        if (rateLimiter.isRateLimited(request.getEmail())) {
            throw new RateLimitException("Too many login attempts");
        }

        UserAccountDto user = accounts.authenticate(request.getEmail(), request.getPassword())
                .orElse(null);
        if (user == null) {
            rateLimiter.recordFailedAttempt(request.getEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.emailVerified()) {
            throw new EmailNotVerifiedException("Email not verified");
        }

        rateLimiter.clearAttempts(request.getEmail());

        String accessToken = jwtTokenService.generateAccessToken(
                user.id().toString(), user.email(), user.role().name());
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setUserId(user.id());
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);

        addRefreshCookie(response, refreshToken);

        return new LoginResponse(accessToken, "Bearer", 3600);
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken rt = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        if (rt.isRevoked()) {
            boolean withinGraceWindow = rt.getRevokedAt() != null
                    && rt.getRevokedAt().plusSeconds(graceWindowSeconds).isAfter(Instant.now());
            if (!withinGraceWindow) {
                throw new InvalidRefreshTokenException("Refresh token revoked");
            }
        }

        rt.setRevoked(true);
        rt.setRevokedAt(Instant.now());
        refreshTokenRepository.save(rt);

        UserAccountDto user = accounts.findById(rt.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
        String newAccessToken = jwtTokenService.generateAccessToken(user.id().toString(), user.email(), user.role().name());
        String newRefreshToken = UUID.randomUUID().toString();

        RefreshToken newRt = new RefreshToken();
        newRt.setToken(newRefreshToken);
        newRt.setUserId(user.id());
        newRt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        newRt.setRevoked(false);
        refreshTokenRepository.save(newRt);

        addRefreshCookie(response, newRefreshToken);

        return new LoginResponse(newAccessToken, "Bearer", 3600);
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or already used token"));

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token expired");
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        accounts.markEmailVerified(vt.getUserId());

        return new VerifyEmailResponse("Email verified successfully");
    }

    @Transactional
    public ResendVerificationResponse resendVerification(String email) {
        UserAccountDto user = accounts.findByEmail(email).orElse(null);
        if (user != null && !user.emailVerified()) {
            verificationTokenRepository.findByUserIdAndTokenType(user.id(), "VERIFY")
                    .forEach(t -> {
                        t.setUsed(true);
                        verificationTokenRepository.save(t);
                    });

            VerificationToken newToken = new VerificationToken();
            newToken.setToken(UUID.randomUUID().toString());
            newToken.setUserId(user.id());
            newToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            newToken.setUsed(false);
            newToken.setTokenType("VERIFY");
            verificationTokenRepository.save(newToken);

            System.out.println("[AUTH-TOKEN] type=VERIFY email=" + email + " token=" + newToken.getToken());
        }
        return new ResendVerificationResponse("If the email exists and is unverified, a new verification email has been sent.");
    }

    public PasswordResetRequestResponse requestPasswordReset(String email) {
        UserAccountDto user = accounts.findByEmail(email).orElse(null);
        if (user != null) {
            String resetToken = UUID.randomUUID().toString();
            VerificationToken vt = new VerificationToken();
            vt.setToken(resetToken);
            vt.setUserId(user.id());
            vt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            vt.setUsed(false);
            vt.setTokenType("RESET");
            verificationTokenRepository.save(vt);
            System.out.println("[AUTH-TOKEN] type=RESET email=" + email + " token=" + resetToken);
        }
        return new PasswordResetRequestResponse("If the email exists, a reset link has been sent.");
    }

    @Transactional
    public PasswordResetResponse resetPassword(String token, String newPassword) {
        VerificationToken vt = verificationTokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token expired");
        }
        if (!"RESET".equals(vt.getTokenType())) {
            throw new InvalidTokenException("Invalid token type");
        }

        UUID userId = vt.getUserId();

        // AUTH-RESET-FAMILY-001 / CWE-640: a successful reset invalidates the user's ENTIRE
        // family of outstanding unused reset tokens — not only the consumed one — in the same
        // transaction, so an earlier-issued reset token cannot be replayed after the password
        // has already been changed.
        verificationTokenRepository.markAllUnusedAsUsed(userId, "RESET");

        accounts.resetPassword(userId, newPassword);

        return new PasswordResetResponse("Password reset successful.");
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
                if (!rt.isRevoked()) {
                    rt.setRevoked(true);
                    // Don't set revokedAt — prevents grace window bypass on revoked-by-logout tokens
                    refreshTokenRepository.save(rt);
                }
            });
        }
        ResponseCookie cleared = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }

    public PasswordChangeResponse changePassword(String userId, String currentPassword, String newPassword) {
        // a missing user surfaces from the port's own lookup (IllegalStateException → 500,
        // same surface as the pre-port RuntimeException) — no second lookup here
        if (!accounts.changePassword(UUID.fromString(userId), currentPassword, newPassword)) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        return new PasswordChangeResponse("Password changed successfully.");
    }
}
