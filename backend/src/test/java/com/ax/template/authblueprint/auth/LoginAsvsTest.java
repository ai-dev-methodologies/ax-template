package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedVerifiedUser() {
        UserEntity user = new UserEntity();
        user.setEmail("verified@example.com");
        user.setHashedPassword(passwordEncoder.encode("securepassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.2.1")
    void asvs_V2_2_1_rateLimitAfter5FailedAttemptsIn15Min() throws Exception {
        String badLogin = "{\"email\":\"verified@example.com\",\"password\":\"wrongpassword1\"}";
        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/email/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(badLogin));
        }
        // 6th attempt should be rate limited (429)
        mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badLogin))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_genericErrorMessage_sameForWrongPasswordAndNonexistentUser() throws Exception {
        // Wrong password for existing user
        var r1 = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"verified@example.com\",\"password\":\"wrongpassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Nonexistent user
        var r2 = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nonexistent@example.com\",\"password\":\"wrongpassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Both should return identical response body (no user enumeration)
        String body1 = r1.getResponse().getContentAsString();
        String body2 = r2.getResponse().getContentAsString();
        assertThat(body1).isEqualTo(body2);
    }

    @Test
    void login_success_returnsAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"verified@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void login_unverifiedUser_returns403() throws Exception {
        UserEntity unverified = new UserEntity();
        unverified.setEmail("unverified@example.com");
        unverified.setHashedPassword(passwordEncoder.encode("securepassword12"));
        unverified.setRole(UserRole.MEMBER);
        unverified.setEmailVerified(false);
        userRepository.save(unverified);

        mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unverified@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isForbidden());
    }
}
