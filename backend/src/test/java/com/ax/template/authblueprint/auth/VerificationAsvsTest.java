package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VerificationAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired VerificationTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UserEntity createUnverifiedUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setHashedPassword(passwordEncoder.encode("securepassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    private VerificationToken createToken(UserEntity user, Instant expiresAt) {
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(user.getId());
        token.setExpiresAt(expiresAt);
        token.setUsed(false);
        token.setTokenType("VERIFY");
        return tokenRepository.save(token);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.7.2")
    void asvs_V2_7_2_verificationTokenExpiresAfter24h() throws Exception {
        UserEntity user = createUnverifiedUser("expire@example.com");
        VerificationToken expiredToken = createToken(user, Instant.now().minus(25, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + expiredToken.getToken() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.7.3")
    void asvs_V2_7_3_verificationTokenSingleUse() throws Exception {
        UserEntity user = createUnverifiedUser("singleuse@example.com");
        VerificationToken token = createToken(user, Instant.now().plus(24, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getToken() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getToken() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_validToken_setsEmailVerified() throws Exception {
        UserEntity user = createUnverifiedUser("verify@example.com");
        VerificationToken token = createToken(user, Instant.now().plus(24, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        UserEntity updated = userRepository.findByEmail("verify@example.com").orElseThrow();
        assertThat(updated.isEmailVerified()).isTrue();
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.7.3")
    void verifyEmail_rejectsResetToken_typeConfusion() throws Exception {
        UserEntity user = createUnverifiedUser("crosstype@example.com");

        VerificationToken resetToken = new VerificationToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);
        resetToken.setTokenType("RESET");
        tokenRepository.save(resetToken);

        // A RESET token presented to the email-verify endpoint MUST be rejected (CWE-843).
        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken.getToken() + "\"}"))
                .andExpect(status().isBadRequest());

        // The email stays unverified — the wrong-type token did not take effect.
        UserEntity unchanged = userRepository.findByEmail("crosstype@example.com").orElseThrow();
        assertThat(unchanged.isEmailVerified()).isFalse();
    }

    @Test
    void resendVerification_invalidatesOldToken() throws Exception {
        UserEntity user = createUnverifiedUser("resend@example.com");
        VerificationToken oldToken = createToken(user, Instant.now().plus(24, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/auth/email/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"resend@example.com\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + oldToken.getToken() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
