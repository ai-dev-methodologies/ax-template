package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
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
class SecurityBaselineAsvsTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        var u = new UserEntity();
        u.setEmail("sec@example.com");
        u.setHashedPassword(passwordEncoder.encode("securepassword12"));
        u.setRole(UserRole.MEMBER);
        u.setEmailVerified(true);
        userRepository.save(u);
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.4.1")
    void asvs_V3_4_1_cookieSecureFlag() throws Exception {
        var r = mockMvc.perform(post("/api/auth/email/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"sec@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeader("Set-Cookie")).contains("Secure");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.4.2")
    void asvs_V3_4_2_cookieHttpOnlyFlag() throws Exception {
        var r = mockMvc.perform(post("/api/auth/email/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"sec@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeader("Set-Cookie")).containsIgnoringCase("HttpOnly");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V3.4.3")
    void asvs_V3_4_3_cookieSameSiteAttribute() throws Exception {
        var r = mockMvc.perform(post("/api/auth/email/login").contentType(APPLICATION_JSON)
                .content("{\"email\":\"sec@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeader("Set-Cookie")).containsIgnoringCase("SameSite");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V4.2.2")
    void asvs_V4_2_2_antiCSRF_statelessJwtAllowsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/email/signup").contentType(APPLICATION_JSON)
                .content("{\"email\":\"csrf@example.com\",\"password\":\"securepassword12\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void jwt_algorithmNoneRejected() throws Exception {
        String noneJwt = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo5OTk5OTk5OTk5fQ.";
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + noneJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwt_invalidSignatureRejected() throws Exception {
        String badSig = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo5OTk5OTk5OTk5fQ.invalidsignature";
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + badSig))
                .andExpect(status().isUnauthorized());
    }
}
