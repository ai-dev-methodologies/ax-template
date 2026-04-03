package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService,
                           LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.rateLimiter = rateLimiter;
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
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            rateLimiter.recordFailedAttempt(request.getEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified");
        }

        rateLimiter.clearAttempts(request.getEmail());

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(), user.getEmail());
        String refreshToken = UUID.randomUUID().toString();

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new LoginResponse(accessToken, "Bearer", 3600);
    }
}
