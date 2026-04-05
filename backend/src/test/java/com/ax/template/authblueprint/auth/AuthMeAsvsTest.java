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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthMeAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;

    @BeforeEach
    void seedTwoUsers() {
        for (String email : new String[]{"user1@example.com", "user2@example.com"}) {
            var u = new UserEntity();
            u.setEmail(email);
            u.setHashedPassword(passwordEncoder.encode("securepassword12"));
            u.setRole(UserRole.MEMBER);
            u.setEmailVerified(true);
            userRepository.save(u);
        }
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V4.1.1")
    void asvs_V4_1_1_accessControlOnTrustedLayer() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V4.1.5")
    void asvs_V4_1_5_accessControlFailsSecurely() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer tampered.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V4.2.1")
    void asvs_V4_2_1_noIDOR() throws Exception {
        UserEntity user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
        String token1 = jwtTokenService.generateAccessToken(user1.getId().toString(), user1.getEmail(), "MEMBER");

        var result = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("user1@example.com").doesNotContain("user2@example.com");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.1.1")
    void asvs_V3_1_1_noTokenInURL() throws Exception {
        mockMvc.perform(get("/api/auth/me?token=sometoken")).andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_authenticatedUser_returnsProfile() throws Exception {
        UserEntity user = userRepository.findByEmail("user1@example.com").orElseThrow();
        String token = jwtTokenService.generateAccessToken(user.getId().toString(), user.getEmail(), "MEMBER");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.userId").exists());
    }
}
