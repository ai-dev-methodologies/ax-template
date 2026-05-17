package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OAuthFullFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired OAuthStateStore stateStore;
    @MockBean RestTemplate restTemplate;
    ObjectMapper mapper = new ObjectMapper();

    private void mockGoogleTokenAndUserInfo() {
        // Token exchange
        when(restTemplate.exchange(contains("/token"), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("access_token", "mock-google-token", "token_type", "Bearer")));
        // User info
        when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("sub", "google-user-123", "email", "google@test.com", "name", "Google User")));
    }

    private void mockKakaoTokenAndUserInfo() {
        when(restTemplate.exchange(contains("kauth.kakao"), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("access_token", "mock-kakao-token", "token_type", "Bearer")));
        when(restTemplate.exchange(contains("kapi.kakao"), eq(HttpMethod.GET), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("id", 12345, "kakao_account", Map.of("email", "kakao@test.com"), "properties", Map.of("nickname", "Kakao User"))));
    }

    private void mockNaverTokenAndUserInfo() {
        when(restTemplate.exchange(contains("nid.naver"), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("access_token", "mock-naver-token", "token_type", "Bearer")));
        when(restTemplate.exchange(contains("openapi.naver"), eq(HttpMethod.GET), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("response", Map.of("id", "naver-user-456", "email", "naver@test.com", "name", "Naver User"))));
    }

    private String getValidState(String provider) throws Exception {
        var result = mockMvc.perform(get("/api/auth/oauth/" + provider + "/authorize"))
            .andExpect(status().is3xxRedirection()).andReturn();
        String url = result.getResponse().getRedirectedUrl();
        return url.split("state=")[1].split("&")[0];
    }

    // === GOOGLE FULL FLOW ===
    @Test @Tag("OAUTH-FLOW")
    void google_callback_exchangesCode_createsUser_issuesJwt_profileAccessible() throws Exception {
        mockGoogleTokenAndUserInfo();
        String state = getValidState("google");

        var callbackResult = mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "mock-auth-code").param("state", state))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String jwt = mapper.readTree(callbackResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("google@test.com"));

        assertThat(userRepository.findByEmail("google@test.com")).isPresent();
    }

    // === KAKAO FULL FLOW ===
    @Test @Tag("OAUTH-FLOW")
    void kakao_callback_exchangesCode_createsUser_issuesJwt_profileAccessible() throws Exception {
        mockKakaoTokenAndUserInfo();
        String state = getValidState("kakao");

        var callbackResult = mockMvc.perform(get("/api/auth/oauth/kakao/callback")
                .param("code", "mock-auth-code").param("state", state))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String jwt = mapper.readTree(callbackResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("kakao@test.com"));

        assertThat(userRepository.findByEmail("kakao@test.com")).isPresent();
    }

    // === NAVER FULL FLOW ===
    @Test @Tag("OAUTH-FLOW")
    void naver_callback_exchangesCode_createsUser_issuesJwt_profileAccessible() throws Exception {
        mockNaverTokenAndUserInfo();
        String state = getValidState("naver");

        var callbackResult = mockMvc.perform(get("/api/auth/oauth/naver/callback")
                .param("code", "mock-auth-code").param("state", state))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        String jwt = mapper.readTree(callbackResult.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("naver@test.com"));

        assertThat(userRepository.findByEmail("naver@test.com")).isPresent();
    }

    // === PROVIDER LINK ===
    @Test @Tag("OAUTH-FLOW")
    void oauth_link_addsProviderToExistingUser() throws Exception {
        mockGoogleTokenAndUserInfo();
        String state1 = getValidState("google");
        var login = mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "c").param("state", state1))
            .andExpect(status().isOk()).andReturn();
        String jwt = mapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

        mockKakaoTokenAndUserInfo();
        String state2 = getValidState("kakao");
        mockMvc.perform(post("/api/auth/oauth/link")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"kakao\",\"code\":\"c\",\"state\":\"" + state2 + "\"}"))
            .andExpect(status().isOk());
    }

    // === PROVIDER DOWN → FALLBACK ===
    @Test @Tag("OAUTH-FLOW")
    void oauth_providerDown_returns503_withEmailFallback() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        String state = getValidState("google");
        mockMvc.perform(get("/api/auth/oauth/google/callback")
                .param("code", "c").param("state", state))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.fallback").value("email"));
    }
}
