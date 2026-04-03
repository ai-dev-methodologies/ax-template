package com.ax.template.authblueprint.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
public class EmailAuthController {

    private final AuthServiceImpl authService;

    public EmailAuthController(AuthServiceImpl authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request.getToken());
    }

    @PostMapping("/resend-verification")
    public ResendVerificationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return authService.resendVerification(request.getEmail());
    }

    @PostMapping("/password-reset-request")
    public PasswordResetRequestResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        return authService.requestPasswordReset(request.getEmail());
    }

    @PostMapping("/password-reset")
    public PasswordResetResponse resetPassword(@Valid @RequestBody PasswordResetDto request) {
        return authService.resetPassword(request.getToken(), request.getNewPassword());
    }

    @PostMapping("/password-change")
    public PasswordChangeResponse changePassword(
            @Valid @RequestBody PasswordChangeDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return authService.changePassword(jwt.getSubject(), request.getCurrentPassword(), request.getNewPassword());
    }
}
