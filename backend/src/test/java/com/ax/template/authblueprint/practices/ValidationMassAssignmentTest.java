package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-VAL-001")
class ValidationMassAssignmentTest {

    /**
     * Mimics a server-side entity. Protected fields (role, enabled) have server-controlled
     * defaults and must NOT be set from inbound HTTP payloads.
     */
    static class ProtectedUser {
        public String name;
        public String email;
        public String role = "USER";
        public boolean enabled = true;
    }

    /**
     * Whitelist DTO: only user-controllable fields. Lacks role and enabled by design.
     * @JsonIgnoreProperties drops any unexpected field (like an attacker-injected "role")
     * silently, so the deserialization never observes the protected names.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class UserUpdateDto {
        public String name;
        public String email;
    }

    @Test
    void practices_VAL_001_dtoWhitelistBlocksMassAssignment() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String malicious = """
                {"name":"hacker","email":"h@x.com","role":"ADMIN","enabled":false}
                """;

        // Anti-pattern path: binding directly to the entity lets attacker set protected fields.
        ProtectedUser direct = mapper.readValue(malicious, ProtectedUser.class);
        assertThat(direct.role).as("direct bind allows mass-assignment to role").isEqualTo("ADMIN");
        assertThat(direct.enabled).as("direct bind allows mass-assignment to enabled").isFalse();

        // Correct path: bind to the whitelist DTO, then copy only whitelisted fields onto a fresh
        // entity. Server-side defaults are preserved.
        UserUpdateDto dto = mapper.readValue(malicious, UserUpdateDto.class);
        ProtectedUser safe = new ProtectedUser();
        safe.name = dto.name;
        safe.email = dto.email;

        assertThat(safe.role).as("server default role preserved").isEqualTo("USER");
        assertThat(safe.enabled).as("server default enabled preserved").isTrue();
        assertThat(safe.name).isEqualTo("hacker");
        assertThat(safe.email).isEqualTo("h@x.com");
    }
}
