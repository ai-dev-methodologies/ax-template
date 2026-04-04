package com.ax.template.authblueprint.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @GetMapping("/{provider}/authorize")
    public void authorize(@PathVariable String provider,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        String baseUrl = buildBaseUrl(request);
        String authUrl = oAuthService.buildAuthorizationUrl(provider, baseUrl);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/{provider}/callback")
    public LoginResponse callback(@PathVariable String provider,
                                  @RequestParam String code,
                                  @RequestParam(required = false) String state,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        String baseUrl = buildBaseUrl(request);
        return oAuthService.handleCallback(provider, code, state, baseUrl, response);
    }

    @PostMapping("/link")
    public Map<String, String> link(@RequestBody OAuthLinkRequest body,
                                    @AuthenticationPrincipal Jwt jwt) {
        oAuthService.linkProvider(jwt.getSubject(), body);
        return Map.of("message", "Provider linked successfully");
    }

    @DeleteMapping("/unlink/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable String provider,
                       @AuthenticationPrincipal Jwt jwt) {
        oAuthService.unlinkProvider(jwt.getSubject(), provider);
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
