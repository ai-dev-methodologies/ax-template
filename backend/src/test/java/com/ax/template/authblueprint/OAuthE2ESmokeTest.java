package com.ax.template.authblueprint;

import com.ax.template.authblueprint.auth.JwtTokenService;
import com.ax.template.authblueprint.auth.OAuthStateStore;
import com.ax.template.authblueprint.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("ASVS")
class OAuthE2ESmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProviderLinkRepository providerLinkRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired OAuthStateStore oAuthStateStore;

    private UserEntity user;
    private String jwt;

    @BeforeEach
    void seedUser() {
        user = new UserEntity();
        user.setEmail("oauth-e2e@example.com");
        user.setHashedPassword(passwordEncoder.encode("securepassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        jwt = jwtTokenService.generateAccessToken(user.getId().toString(), user.getEmail(), "MEMBER");
    }

    @Test
    void authorize_returnsRedirectWithState() throws Exception {
        var result = mockMvc.perform(get("/api/auth/oauth/google/authorize"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl)
                .isNotNull()
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("state=")
                .contains("client_id=")
                .contains("redirect_uri=")
                .doesNotContain("client_secret");
    }

    @Test
    void callback_withoutState_rejected() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                        .param("code", "fake-code"))
                .andExpect(status().isForbidden());
    }

    @Test
    void callback_withInvalidState_rejected() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                        .param("code", "fake-code")
                        .param("state", "bogus-state"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_showsLinkedProvider_afterDirectSeed() throws Exception {
        seedProviderLink("google-uid-12345");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedProviders", hasItem("GOOGLE")));
    }

    @Test
    void unlink_removesProvider() throws Exception {
        seedProviderLink("google-uid-67890");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$.linkedProviders", hasItem("GOOGLE")));

        mockMvc.perform(delete("/api/auth/oauth/unlink/google")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedProviders", not(hasItem("GOOGLE"))));
    }

    @Test
    void fullOAuthE2EFlow() throws Exception {
        // given: authorize returns redirect with state
        var authorizeResult = mockMvc.perform(get("/api/auth/oauth/google/authorize"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String redirectUrl = authorizeResult.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).contains("state=");
        String state = extractQueryParam(redirectUrl, "state");
        assertThat(state).isNotBlank();

        // when: callback with invalid state → rejected
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                        .param("code", "fake")
                        .param("state", "definitely-not-valid"))
                .andExpect(status().isForbidden());

        // given: provider link seeded (stands in for successful token exchange)
        seedProviderLink("google-e2e-full-flow");

        // then: /auth/me shows GOOGLE
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("oauth-e2e@example.com"))
                .andExpect(jsonPath("$.linkedProviders", hasItem("GOOGLE")));

        // when: unlink
        mockMvc.perform(delete("/api/auth/oauth/unlink/google")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // then: GOOGLE removed
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedProviders", not(hasItem("GOOGLE"))));
    }

    private void seedProviderLink(String providerUserId) {
        ProviderLink link = new ProviderLink();
        link.setUser(user);
        link.setProvider(OAuthProvider.GOOGLE);
        link.setProviderUserId(providerUserId);
        link.setProviderEmail(user.getEmail());
        providerLinkRepository.save(link);
    }

    private String extractQueryParam(String url, String param) {
        if (url == null) return null;
        String marker = param + "=";
        int start = url.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = url.indexOf('&', start);
        return end < 0 ? url.substring(start) : url.substring(start, end);
    }
}
