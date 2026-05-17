package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthSessionController {

    private final AuthServiceImpl authService;
    private final UserRepository userRepository;
    private final OAuthService oAuthService;

    public AuthSessionController(AuthServiceImpl authService,
                                 UserRepository userRepository,
                                 OAuthService oAuthService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.oAuthService = oAuthService;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, response);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<String> linkedProviders = oAuthService.getLinkedProviders(userId);
        return new UserProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified(),
                linkedProviders);
    }
}
