package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OAuthService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuthStateStore stateStore;
    private final UserRepository userRepository;
    private final ProviderLinkRepository providerLinkRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RestTemplate restTemplate;

    public OAuthService(ClientRegistrationRepository clientRegistrationRepository,
                        OAuthStateStore stateStore,
                        UserRepository userRepository,
                        ProviderLinkRepository providerLinkRepository,
                        JwtTokenService jwtTokenService,
                        RefreshTokenRepository refreshTokenRepository, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.stateStore = stateStore;
        this.userRepository = userRepository;
        this.providerLinkRepository = providerLinkRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String buildAuthorizationUrl(String provider, String baseUrl) {
        ClientRegistration registration = getRegistration(provider);
        String state = stateStore.generateState();
        String redirectUri = baseUrl + "/api/auth/oauth/" + provider + "/callback";

        return UriComponentsBuilder
                .fromUriString(registration.getProviderDetails().getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", String.join(" ", registration.getScopes()))
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Transactional
    public LoginResponse handleCallback(String provider, String code, String state,
                                        String baseUrl, HttpServletResponse response) {
        if (state == null || !stateStore.validateAndConsume(state)) {
            throw new InvalidOAuthStateException("Invalid or missing OAuth state parameter");
        }

        ClientRegistration registration = getRegistration(provider);
        String redirectUri = baseUrl + "/api/auth/oauth/" + provider + "/callback";

        String accessToken;
        Map<String, Object> userInfo;
        try {
            accessToken = exchangeCodeForToken(registration, code, redirectUri);
            userInfo = fetchUserInfo(registration, accessToken);
        } catch (Exception e) {
            throw new ProviderUnavailableException(provider, e);
        }

        String providerUserId = extractProviderUserId(provider, userInfo);
        String email = extractEmail(provider, userInfo);

        OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        UserEntity user = findOrCreateUser(oauthProvider, providerUserId, email);

        String jwt = jwtTokenService.generateAccessToken(user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshToken = createRefreshToken(user);
        addRefreshCookie(response, refreshToken);

        return new LoginResponse(jwt, "Bearer", 3600);
    }

    @Transactional
    public void linkProvider(String userId, OAuthLinkRequest request) {
        if (request.state() == null || !stateStore.validateAndConsume(request.state())) {
            throw new InvalidOAuthStateException("Invalid or missing OAuth state parameter");
        }

        OAuthProvider oauthProvider = OAuthProvider.valueOf(request.provider().toUpperCase());
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (providerLinkRepository.findByUserAndProvider(user, oauthProvider).isPresent()) {
            throw new IllegalStateException("Provider already linked");
        }

        ProviderLink link = new ProviderLink();
        link.setUser(user);
        link.setProvider(oauthProvider);
        link.setProviderUserId("pending-" + UUID.randomUUID());
        link.setProviderEmail(user.getEmail());
        providerLinkRepository.save(link);
    }

    @Transactional
    public void unlinkProvider(String userId, String provider) {
        OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProviderLink link = providerLinkRepository.findByUserAndProvider(user, oauthProvider)
                .orElseThrow(() -> new IllegalStateException("Provider not linked"));

        boolean hasPassword = user.getHashedPassword() != null;
        long otherProviders = providerLinkRepository.countByUser(user) - 1;
        if (!hasPassword && otherProviders == 0) {
            throw new IllegalStateException("Cannot unlink last authentication method");
        }

        providerLinkRepository.delete(link);
    }

    public List<String> getLinkedProviders(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return providerLinkRepository.findByUser(user).stream()
                .map(link -> link.getProvider().name())
                .toList();
    }

    private ClientRegistration getRegistration(String provider) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider.toLowerCase());
        if (registration == null) {
            throw new IllegalArgumentException("Unknown OAuth provider: " + provider);
        }
        return registration;
    }

    @SuppressWarnings("unchecked")
    private String exchangeCodeForToken(ClientRegistration registration, String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", registration.getClientId());
        params.add("client_secret", registration.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> resp = restTemplate.exchange(
                registration.getProviderDetails().getTokenUri(),
                HttpMethod.POST,
                new HttpEntity<>(params, headers),
                Map.class);

        Map<String, Object> body = resp.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new RuntimeException("Failed to exchange code for token");
        }
        return (String) body.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(ClientRegistration registration, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(
                registration.getProviderDetails().getUserInfoEndpoint().getUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        return resp.getBody() != null ? resp.getBody() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private String extractProviderUserId(String provider, Map<String, Object> userInfo) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) userInfo.get("sub");
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
                yield response != null ? (String) response.get("id") : null;
            }
            case "kakao" -> String.valueOf(userInfo.get("id"));
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(String provider, Map<String, Object> userInfo) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) userInfo.get("email");
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
                yield response != null ? (String) response.get("email") : null;
            }
            case "kakao" -> {
                Map<String, Object> account = (Map<String, Object>) userInfo.get("kakao_account");
                yield account != null ? (String) account.get("email") : null;
            }
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    private UserEntity findOrCreateUser(OAuthProvider provider, String providerUserId, String email) {
        Optional<ProviderLink> existingLink = providerLinkRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingLink.isPresent()) {
            return existingLink.get().getUser();
        }

        UserEntity user = (email != null)
                ? userRepository.findByEmail(email).orElseGet(() -> createOAuthUser(email))
                : createOAuthUser("oauth-" + UUID.randomUUID() + "@placeholder.local");

        ProviderLink link = new ProviderLink();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderUserId(providerUserId);
        link.setProviderEmail(email);
        providerLinkRepository.save(link);

        return user;
    }

    private UserEntity createOAuthUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private String createRefreshToken(UserEntity user) {
        String token = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);
        return token;
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
}
