package com.ax.template.authblueprint.crud;

import com.ax.template.authblueprint.auth.JwtTokenService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CrudSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired ItemRepository itemRepository;
    ObjectMapper mapper = new ObjectMapper();

    private String tokenA, tokenB;
    private UserEntity userA, userB;

    @BeforeEach
    void setup() {
        userA = new UserEntity();
        userA.setEmail("usera@test.com");
        userA.setHashedPassword(passwordEncoder.encode("securepassword12"));
        userA.setRole(UserRole.MEMBER);
        userA.setEmailVerified(true);
        userA = userRepository.save(userA);
        tokenA = jwtTokenService.generateAccessToken(userA.getId().toString(), userA.getEmail(), "MEMBER");

        userB = new UserEntity();
        userB.setEmail("userb@test.com");
        userB.setHashedPassword(passwordEncoder.encode("securepassword12"));
        userB.setRole(UserRole.MEMBER);
        userB.setEmailVerified(true);
        userB = userRepository.save(userB);
        tokenB = jwtTokenService.generateAccessToken(userB.getId().toString(), userB.getEmail(), "MEMBER");
    }

    @Test @Tag("CRUD") @Tag("CRUD-AUTH-1")
    void crud_AUTH_1_allEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/items")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"test\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/items/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/items/00000000-0000-0000-0000-000000000000")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/items/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Tag("CRUD") @Tag("CRUD-AUTH-2")
    void crud_AUTH_2_noIdorBetweenUsers() throws Exception {
        // User A creates item
        var createResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A's item\"}"))
                .andExpect(status().isCreated()).andReturn();
        String itemId = mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // User B tries to access A's item → 404
        mockMvc.perform(get("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B tries to update A's item → 404
        mockMvc.perform(put("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"hacked\"}"))
                .andExpect(status().isNotFound());

        // User B tries to delete A's item → 404
        mockMvc.perform(delete("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test @Tag("CRUD") @Tag("CRUD-VAL-1")
    void crud_VAL_1_requestBodyValidation() throws Exception {
        // Missing title → 400
        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"no title\"}"))
                .andExpect(status().isBadRequest());

        // Title > 255 chars → 400
        String longTitle = "a".repeat(256);
        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @Tag("CRUD") @Tag("CRUD-VAL-2")
    void crud_VAL_2_sqlInjectionPrevented() throws Exception {
        String injection = "'; DROP TABLE items; --";
        var result = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + injection.replace("\"", "\\\"") + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        // Item was created with literal string, DB not affected
        String title = mapper.readTree(result.getResponse().getContentAsString()).get("title").asText();
        assertThat(title).contains("DROP TABLE");

        // Verify items table still works
        mockMvc.perform(get("/api/items")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test @Tag("CRUD") @Tag("CRUD-PAG-1")
    void crud_PAG_1_paginationMaxPageSize() throws Exception {
        // Create 60 items
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(post("/api/items")
                    .header("Authorization", "Bearer " + tokenA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"item" + i + "\"}"));
        }
        // Request size=1000 → max 50
        var result = mockMvc.perform(get("/api/items?page=0&size=1000")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        var json = mapper.readTree(result.getResponse().getContentAsString());
        // Canonical PageEnvelope (common/PageEnvelope): items under `data`, page
        // metadata under `pagination` — NOT the stale Spring { content, size } shape.
        int returnedSize = json.get("data").size();
        assertThat(returnedSize).isLessThanOrEqualTo(50);
        assertThat(json.get("pagination").get("pageSize").asInt()).isLessThanOrEqualTo(50);
    }

    @Test @Tag("CRUD") @Tag("CRUD-DEL-1")
    void crud_DEL_1_softDeleteNotPhysical() throws Exception {
        // Create item
        var createResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"to delete\"}"))
                .andExpect(status().isCreated()).andReturn();
        String itemId = mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Delete → 204
        mockMvc.perform(delete("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // GET → 404 (soft deleted)
        mockMvc.perform(get("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // But DB still has row
        assertThat(itemRepository.findById(java.util.UUID.fromString(itemId))).isPresent();
        assertThat(itemRepository.findById(java.util.UUID.fromString(itemId)).get().isDeleted()).isTrue();
    }

    @Test @Tag("CRUD") @Tag("CRUD-AUD-1")
    void crud_AUD_1_auditFieldsPopulated() throws Exception {
        // Create → has createdAt, createdBy
        var createResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"audit test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("usera@test.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        String itemId = mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Update → has updatedAt, updatedBy
        mockMvc.perform(put("/api/items/" + itemId)
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("usera@test.com"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }
}
