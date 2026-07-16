package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordResetAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired VerificationTokenRepository tokenRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void seedUser() {
        UserEntity user = new UserEntity();
        user.setEmail("reset@example.com");
        user.setHashedPassword(passwordEncoder.encode("oldpassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.5.2")
    void asvs_V2_5_2_noSecurityQuestions() throws Exception {
        // No security question endpoints should exist
        mockMvc.perform(post("/api/auth/email/security-question")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/auth/email/security-question"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.5.3")
    void asvs_V2_5_3_resetDoesNotRevealCurrentPassword() throws Exception {
        // Request password reset
        var result = mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Response must NOT contain the current password
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("oldpassword12");
        assertThat(body).doesNotContain("password");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.5.4")
    void asvs_V2_5_4_noDefaultAccounts() throws Exception {
        String[][] defaults = {
            {"admin@example.com", "adminpassword1"},
            {"root@example.com", "rootpassword12"},
            {"test@test.com", "testpassword1"},
            {"admin@admin.com", "password12345"},
            {"user@example.com", "userpassword1"}
        };
        for (String[] cred : defaults) {
            int status = mockMvc.perform(post("/api/auth/email/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + cred[0] + "\",\"password\":\"" + cred[1] + "\"}"))
                    .andReturn().getResponse().getStatus();
            assertThat(status).isNotEqualTo(200);
        }
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.6")
    void asvs_V2_1_6_passwordChangeRequiresCurrentAndNew() throws Exception {
        // Seed a verified user with known password
        UserEntity user = new UserEntity();
        user.setEmail("change@example.com");
        user.setHashedPassword(passwordEncoder.encode("currentpassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        userRepository.save(user);

        // Get access token by logging in
        var loginResult = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"change@example.com\",\"password\":\"currentpassword12\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        String token = mapper.readTree(loginBody).get("accessToken").asText();

        // Password change without current password should fail
        mockMvc.perform(post("/api/auth/email/password-change")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newpassword1234\"}"))
                .andExpect(status().isBadRequest());

        // Password change with wrong current password should fail
        mockMvc.perform(post("/api/auth/email/password-change")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrongpassword\",\"newPassword\":\"newpassword1234\"}"))
                .andExpect(status().isUnauthorized());

        // Password change with correct current password should succeed
        mockMvc.perform(post("/api/auth/email/password-change")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"currentpassword12\",\"newPassword\":\"newpassword1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordReset_tokenSingleUse() throws Exception {
        // Request reset
        mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk());

        // Get token from DB
        UserEntity user = userRepository.findByEmail("reset@example.com").orElseThrow();
        List<VerificationToken> tokens = tokenRepository.findByUserIdAndTokenType(user.getId(), "RESET");
        assertThat(tokens).isNotEmpty();
        String resetToken = tokens.get(0).getToken();

        // First use should succeed
        mockMvc.perform(post("/api/auth/email/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"brandnewpass12\"}"))
                .andExpect(status().isOk());

        // Second use should fail (single-use)
        mockMvc.perform(post("/api/auth/email/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"anotherpasswd12\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("ASVS")
    @Tag("AUTH-RESET-FAMILY-001")
    void passwordReset_successInvalidatesEntireTokenFamily() throws Exception {
        // Issue TWO password-reset tokens for the same user (e.g. the user clicked
        // "forgot password" twice). Both are unused and currently valid.
        mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk());

        UserEntity user = userRepository.findByEmail("reset@example.com").orElseThrow();
        List<VerificationToken> tokens = tokenRepository.findByUserIdAndTokenType(user.getId(), "RESET");
        assertThat(tokens).hasSize(2);
        String token1 = tokens.get(0).getToken();
        String token2 = tokens.get(1).getToken();

        // Consuming token1 resets the password AND must invalidate the whole family.
        mockMvc.perform(post("/api/auth/email/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token1 + "\",\"newPassword\":\"brandnewpass12\"}"))
                .andExpect(status().isOk());

        // token2 — never used, but issued before the reset — must now be rejected as
        // already-used/invalid (token-FAMILY invalidation, not just single-token).
        mockMvc.perform(post("/api/auth/email/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token2 + "\",\"newPassword\":\"anotherpasswd12\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetRequest_sameResponseForExistingAndNonexistentEmail() throws Exception {
        var r1 = mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        var r2 = mockMvc.perform(post("/api/auth/email/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nonexistent@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Same response body (prevent enumeration)
        assertThat(r1.getResponse().getContentAsString())
            .isEqualTo(r2.getResponse().getContentAsString());
    }
}
