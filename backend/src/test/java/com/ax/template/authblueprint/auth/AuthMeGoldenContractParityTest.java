package com.ax.template.authblueprint.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACKLOG P1-73 — FE&lt;-&gt;BE contract parity for the real {@code GET /api/auth/me} response
 * shape.
 *
 * <p>Before this fix, {@code @ax/core}'s {@code UserProfile} (and every downstream consumer:
 * both auth stores, the reference {@code DashboardPage}, the L4 auth template, and the MSW
 * mock) read a FICTIONAL shape — {@code roles[]}/{@code verificationState}/
 * {@code providerLinks[]} — that {@link AuthSessionController#me} never emits.
 * {@link UserProfileResponse} (this endpoint's real body: {@code userId, email, role,
 * emailVerified, linkedProviders}) is canonical — {@code contracts/auth-openapi.yaml}'s
 * {@code AuthState} schema already matches it.
 *
 * <p>One committed golden, {@code frontend/tests/_fixtures/auth-me.golden.json}, two independent
 * consumers: this test — a REAL HTTP round-trip through the real controller/service/security
 * chain (RestAssured black-box per PRACTICES-TEST-001, not MockMvc) — and
 * {@code frontend/tests/pages.vitest.tsx}'s {@code DashboardPage} behavioural assertions plus
 * {@code frontend/src/mocks/handlers.ts}'s {@code /auth/me} mock, both of which import the same
 * committed file directly so the mock can never drift from the wire.
 *
 * <h2>userId normalization</h2>
 * {@code userId} is DB-generated and therefore non-deterministic across runs; it is normalized
 * to the golden's fixed placeholder value immediately before the whole-tree compare. Every other
 * field — {@code email}/{@code role}/{@code emailVerified}/{@code linkedProviders} — is asserted
 * byte-for-byte exactly as the controller emitted it.
 *
 * <h2>RED-on-revert</h2>
 * (1) any FE consumer reading {@code roles[]}/{@code verificationState}/{@code providerLinks[]}
 * again diverges from this same golden on the FE leg; (2) renaming/adding/removing a field on
 * {@link UserProfileResponse} changes the emitted tree and trips
 * {@link #authMeResponse_serializesFieldWiseEqualToTheCommittedGolden()} here.
 *
 * <p>Tagged into the existing {@code testAsvs} task (the auth-domain per-domain task, already
 * registered in {@code verification-checklist.yaml}) rather than a new gradle registration —
 * several non-ASVS-titled classes in this package already do the same (e.g.
 * {@link AuthTokenLeakViolationProofTest}, {@link AuthRoleLocaleTest}).
 */
@Tag("ASVS")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthMeGoldenContractParityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GOLDEN_USER_ID = "00000000-0000-0000-0000-000000000001";

    private static Path goldenPath() {
        return Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "auth-me.golden.json");
    }

    private static JsonNode goldenTree() throws IOException {
        return MAPPER.readTree(Files.readString(goldenPath()));
    }

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired ProviderLinkRepository providerLinkRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /** Seeds a user + one linked OAuth provider directly, mirroring the golden's fixed values. */
    private String seedUserAndLinkedProviderThenIssueToken() {
        UserEntity user = new UserEntity();
        user.setEmail("golden-auth-me@example.com");
        user.setHashedPassword(passwordEncoder.encode("securepassword12"));
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        UserEntity saved = userRepository.save(user);

        ProviderLink link = new ProviderLink();
        link.setUserId(saved.getId());
        link.setProvider(OAuthProvider.GOOGLE);
        link.setProviderUserId("golden-google-sub-1");
        providerLinkRepository.save(link);

        return jwtTokenService.generateAccessToken(saved.getId().toString(), saved.getEmail(), "MEMBER");
    }

    @Test
    void authMeResponse_serializesFieldWiseEqualToTheCommittedGolden() throws IOException {
        String token = seedUserAndLinkedProviderThenIssueToken();

        String body = given().header("Authorization", "Bearer " + token)
                .when().get("/api/auth/me")
                .then().statusCode(200)
                .extract().asString();

        ObjectNode actual = (ObjectNode) MAPPER.readTree(body);
        // userId is DB-generated and non-deterministic; normalize to the golden's fixed
        // placeholder before the whole-tree compare (every other field is asserted as-emitted).
        actual.put("userId", GOLDEN_USER_ID);

        assertThat(actual)
                .as("GET /api/auth/me must match the FE-shared golden field-for-field — a "
                        + "regression back to roles[]/verificationState/providerLinks[] (or any "
                        + "renamed field on UserProfileResponse) trips this")
                .isEqualTo(goldenTree());
    }

    @Test
    void authMeResponse_carriesExactlyTheFiveCanonicalMembersWithCorrectShapes() throws IOException {
        String token = seedUserAndLinkedProviderThenIssueToken();

        String body = given().header("Authorization", "Bearer " + token)
                .when().get("/api/auth/me")
                .then().statusCode(200)
                .extract().asString();

        JsonNode actual = MAPPER.readTree(body);
        assertThat(Set.copyOf(actual.propertyNames()))
                .as("the fictional roles[]/verificationState/providerLinks[] members must never "
                        + "reappear")
                .isEqualTo(Set.of("userId", "email", "role", "emailVerified", "linkedProviders"));
        assertThat(actual.get("role").isString())
                .as("role must be a single string, not an array")
                .isTrue();
        assertThat(actual.get("emailVerified").isBoolean())
                .as("emailVerified must be a boolean, not a verificationState string")
                .isTrue();
        assertThat(actual.get("linkedProviders").isArray()).isTrue();
        assertThat(actual.get("linkedProviders").get(0).isString())
                .as("linkedProviders must be a flat array of provider-name strings, not objects")
                .isTrue();
    }
}
