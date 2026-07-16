package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "auth.refresh.grace-window-seconds=0")
class RefreshTokenAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String loginAndGetRefreshCookie(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if (setCookie != null && setCookie.contains("refresh_token=")) {
            return setCookie.split("refresh_token=")[1].split(";")[0];
        }
        return null;
    }

    @BeforeEach
    void seedUser() {
        UserEntity user = new UserEntity();
        user.setEmail("refresh@example.com");
        user.setHashedPassword(passwordEncoder.encode("securepassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.2.1")
    void asvs_V3_2_1_newSessionTokenOnAuth() throws Exception {
        // Login twice — should get different refresh tokens each time
        String token1 = loginAndGetRefreshCookie("refresh@example.com", "securepassword12");
        String token2 = loginAndGetRefreshCookie("refresh@example.com", "securepassword12");

        assertThat(token1).isNotNull();
        assertThat(token2).isNotNull();
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.7.1")
    void asvs_V3_7_1_fullSessionRequiredForSensitiveOps() throws Exception {
        // Accessing /auth/me without a valid token should fail
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // With invalid bearer token should also fail
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_issuedNewTokens() throws Exception {
        String refreshToken = loginAndGetRefreshCookie("refresh@example.com", "securepassword12");
        assertThat(refreshToken).isNotNull();

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_oldTokenInvalidatedAfterUse() throws Exception {
        String refreshToken = loginAndGetRefreshCookie("refresh@example.com", "securepassword12");

        // Use the refresh token
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk());

        // Grace window is 0 seconds in test — old token should be immediately invalid
        Thread.sleep(100);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withoutCookie_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", "nonexistent-token")))
                .andExpect(status().isUnauthorized());
    }
}
