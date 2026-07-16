package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LogoutAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String[] doLogin(String email, String pw) throws Exception {
        var r = mockMvc.perform(post("/api/auth/email/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String body = r.getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(body).get("accessToken").asText();
        String cookie = r.getResponse().getHeader("Set-Cookie");
        String rt = (cookie != null && cookie.contains("refresh_token="))
                ? cookie.split("refresh_token=")[1].split(";")[0] : null;
        return new String[]{token, rt};
    }

    @BeforeEach
    void seedUser() {
        var u = new UserEntity();
        u.setEmail("logout@example.com");
        u.setHashedPassword(passwordEncoder.encode("securepassword12"));
        u.setRole(UserRole.MEMBER);
        u.setEmailVerified(true);
        userRepository.save(u);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.3.1")
    void asvs_V3_3_1_logoutInvalidatesSession() throws Exception {
        String[] creds = doLogin("logout@example.com", "securepassword12");
        String accessToken = creds[0];
        String refreshToken = creds[1];

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookie() throws Exception {
        String[] creds = doLogin("logout@example.com", "securepassword12");
        var result = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + creds[0])
                .cookie(new Cookie("refresh_token", creds[1])))
                .andExpect(status().isNoContent()).andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=").contains("Max-Age=0");
    }
}
