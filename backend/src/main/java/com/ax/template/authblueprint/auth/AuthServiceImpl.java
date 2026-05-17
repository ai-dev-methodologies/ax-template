package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LoginRateLimiter rateLimiter;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long graceWindowSeconds;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService,
                           LoginRateLimiter rateLimiter,
                           VerificationTokenRepository verificationTokenRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           @Value("${auth.refresh.grace-window-seconds:30}") long graceWindowSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.rateLimiter = rateLimiter;
        this.verificationTokenRepository = verificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.graceWindowSeconds = graceWindowSeconds;
    }

    public SignupResponse signup(SignupRequest request) {
        String verificationToken = UUID.randomUUID().toString();

        if (!userRepository.existsByEmail(request.getEmail())) {
            UserEntity user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(UserRole.MEMBER);
            user.setEmailVerified(false);
            UserEntity saved = userRepository.save(user);

            VerificationToken vt = new VerificationToken();
            vt.setToken(verificationToken);
            vt.setUser(saved);
            vt.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            vt.setUsed(false);
            vt.setTokenType("VERIFY");
            verificationTokenRepository.save(vt);

            System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);

            return new SignupResponse(saved.getId().toString(), "Signup successful. Check your email for verification.");
        }

        System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);
        return new SignupResponse(UUID.randomUUID().toString(), "Signup successful. Check your email for verification.");
    }

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        if (rateLimiter.isRateLimited(request.getEmail())) {
            throw new RateLimitException("Too many login attempts");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || user.getHashedPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            rateLimiter.recordFailedAttempt(request.getEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified");
        }

        rateLimiter.clearAttempts(request.getEmail());

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setUser(user);
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

        UserEntity user = rt.getUser();
        String newAccessToken = jwtTokenService.generateAccessToken(user.getId().toString(), user.getEmail(), user.getRole().name());
        String newRefreshToken = UUID.randomUUID().toString();

        RefreshToken newRt = new RefreshToken();
        newRt.setToken(newRefreshToken);
        newRt.setUser(user);
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

        UserEntity user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        return new VerifyEmailResponse("Email verified successfully");
    }

    @Transactional
    public ResendVerificationResponse resendVerification(String email) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user != null && !user.isEmailVerified()) {
            verificationTokenRepository.findByUserAndTokenType(user, "VERIFY")
                    .forEach(t -> {
                        t.setUsed(true);
                        verificationTokenRepository.save(t);
                    });

            VerificationToken newToken = new VerificationToken();
            newToken.setToken(UUID.randomUUID().toString());
            newToken.setUser(user);
            newToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            newToken.setUsed(false);
            newToken.setTokenType("VERIFY");
            verificationTokenRepository.save(newToken);

            System.out.println("[AUTH-TOKEN] type=VERIFY email=" + email + " token=" + newToken.getToken());
        }
        return new ResendVerificationResponse("If the email exists and is unverified, a new verification email has been sent.");
    }

    public PasswordResetRequestResponse requestPasswordReset(String email) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            String resetToken = UUID.randomUUID().toString();
            VerificationToken vt = new VerificationToken();
            vt.setToken(resetToken);
            vt.setUser(user);
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

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        UserEntity user = vt.getUser();
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

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
        UserEntity user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getHashedPassword() == null || !passwordEncoder.matches(currentPassword, user.getHashedPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new PasswordChangeResponse("Password changed successfully.");
    }
}
