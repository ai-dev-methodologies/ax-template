package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    ObjectMapper mapper = new ObjectMapper();

    private String loginAndGetToken(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @BeforeEach
    void seedUsers() {
        UserEntity admin = new UserEntity();
        admin.setEmail("admin@test.com");
        admin.setHashedPassword(passwordEncoder.encode("adminpassword12"));
        admin.setRole(UserRole.ADMIN);
        admin.setEmailVerified(true);
        userRepository.save(admin);

        UserEntity member = new UserEntity();
        member.setEmail("member@test.com");
        member.setHashedPassword(passwordEncoder.encode("memberpassword12"));
        member.setRole(UserRole.MEMBER);
        member.setEmailVerified(true);
        userRepository.save(member);
    }

    @Test
    void member_cannotAccessAdminEndpoint() throws Exception {
        String token = loginAndGetToken("member@test.com", "memberpassword12");
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canAccessAdminEndpoint() throws Exception {
        String token = loginAndGetToken("admin@test.com", "adminpassword12");
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_cannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
