package com.ax.template.authblueprint;

import com.ax.template.authblueprint.auth.VerificationTokenRepository;
import com.ax.template.authblueprint.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class E2ESmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired VerificationTokenRepository tokenRepository;
    @Autowired UserRepository userRepository;
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void fullAuthJourney_signupVerifyLoginMeLogout() throws Exception {
        String email = "e2e@example.com";
        String password = "securepassword12";

        // 1. SIGNUP
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
               .andExpect(status().isCreated());

        // 2. GET VERIFY TOKEN FROM DB
        var user = userRepository.findByEmail(email).orElseThrow();
        var vt = tokenRepository.findByUserAndTokenType(user, "VERIFY")
                .stream().filter(t -> !t.isUsed()).findFirst().orElseThrow();

        // 3. VERIFY EMAIL
        mockMvc.perform(post("/api/auth/email/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + vt.getToken() + "\"}"))
               .andExpect(status().isOk());

        // 4. LOGIN
        var loginResult = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accessToken").isNotEmpty())
               .andReturn();

        String accessToken = mapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
        String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
        String refreshToken = (setCookie != null && setCookie.contains("refresh_token="))
                ? setCookie.split("refresh_token=")[1].split(";")[0] : null;

        // 5. GET /ME
        var meResult = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value(email))
               .andReturn();

        String meBody = meResult.getResponse().getContentAsString();
        assertThat(meBody).contains(email);

        // 6. LOGOUT (POST, not DELETE)
        var logoutReq = post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken);
        if (refreshToken != null) {
            logoutReq = logoutReq.cookie(new Cookie("refresh_token", refreshToken));
        }
        mockMvc.perform(logoutReq).andExpect(status().isNoContent());

        // 7. VERIFY REFRESH TOKEN IS REVOKED
        if (refreshToken != null) {
            mockMvc.perform(post("/api/auth/refresh")
                    .cookie(new Cookie("refresh_token", refreshToken)))
                   .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void passwordChangeJourney() throws Exception {
        String email = "pwchange@example.com";
        String oldPw = "oldpassword12";
        String newPw = "newpassword1234";

        // Setup: signup + verify
        mockMvc.perform(post("/api/auth/email/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + oldPw + "\"}"))
               .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        var vt = tokenRepository.findByUserAndTokenType(user, "VERIFY")
                .stream().filter(t -> !t.isUsed()).findFirst().orElseThrow();

        mockMvc.perform(post("/api/auth/email/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + vt.getToken() + "\"}"))
               .andExpect(status().isOk());

        // Login with old password
        var loginRes = mockMvc.perform(post("/api/auth/email/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + oldPw + "\"}"))
               .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();

        // Change password
        mockMvc.perform(post("/api/auth/email/password-change")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + oldPw + "\",\"newPassword\":\"" + newPw + "\"}"))
               .andExpect(status().isOk());

        // Login with new password should succeed
        mockMvc.perform(post("/api/auth/email/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + newPw + "\"}"))
               .andExpect(status().isOk());
    }
}
