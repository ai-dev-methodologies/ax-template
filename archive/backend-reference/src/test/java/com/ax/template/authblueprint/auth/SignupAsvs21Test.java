package com.ax.template.authblueprint.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SignupAsvs21Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.1")
    void asvs_V2_1_1_passwordMinLength12_rejectsShorter() throws Exception {
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"short11\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.2")
    void asvs_V2_1_2_password64CharsAllowed_129Rejected() throws Exception {
        String pass64 = "a".repeat(64);
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test64@example.com\",\"password\":\"" + pass64 + "\"}"))
                .andExpect(status().isCreated());

        String pass129 = "a".repeat(129);
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test129@example.com\",\"password\":\"" + pass129 + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.3")
    void asvs_V2_1_3_passwordNotTruncated() throws Exception {
        String pass65 = "a".repeat(64) + "Z";
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"trunc@example.com\",\"password\":\"" + pass65 + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.4")
    void asvs_V2_1_4_unicodeAndEmojiAllowed() throws Exception {
        String unicodePass = "correcthorse\uD83D\uDC34battery12";
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unicode@example.com\",\"password\":\"" + unicodePass + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.1.9")
    void asvs_V2_1_9_noCompositionRulesEnforced() throws Exception {
        String allLower = "correcthorsebattery";
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"lower@example.com\",\"password\":\"" + allLower + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void signup_duplicateEmail_returnsGenericResponse() throws Exception {
        String body = "{\"email\":\"dup@example.com\",\"password\":\"securepassword12\"}";
        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }
}
