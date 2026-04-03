package com.ax.template.authblueprint.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RefreshAuthController {

    private final AuthServiceImpl authService;

    public RefreshAuthController(AuthServiceImpl authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException("No refresh token provided");
        }
        return authService.refresh(refreshToken, response);
    }
}
