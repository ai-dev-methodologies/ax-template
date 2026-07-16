package com.ax.template.authblueprint.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OAuthAsvsTest {

    @Autowired MockMvc mockMvc;

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.8.1")
    void asvs_V2_8_1_oauthStateParameterPreventsCsrf() throws Exception {
        // OAuth callback without state → must be rejected
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "fake-code"))
                .andExpect(status().isForbidden());

        // OAuth callback with invalid state → must be rejected
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "fake-code")
                .param("state", "invalid-state-value"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.8.2")
    void asvs_V2_8_2_oauthRedirectUriValidation() throws Exception {
        // Authorize endpoint should redirect to the correct OAuth provider
        var result = mockMvc.perform(get("/api/auth/oauth/google/authorize"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl)
            .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
            .contains("state=");
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.8.3")
    void asvs_V2_8_3_oauthSecretsNotExposed() throws Exception {
        // Error responses must NOT contain client_secret
        var result1 = mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "invalid")
                .param("state", "invalid"))
                .andReturn();
        assertThat(result1.getResponse().getContentAsString())
            .doesNotContain("client_secret")
            .doesNotContain("dummy-google-secret");

        // Authorize redirect must NOT contain client_secret
        var result2 = mockMvc.perform(get("/api/auth/oauth/google/authorize"))
                .andReturn();
        String location = result2.getResponse().getRedirectedUrl();
        if (location != null) {
            assertThat(location)
                .doesNotContain("client_secret");
        }
    }
}
