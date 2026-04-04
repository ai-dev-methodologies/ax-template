// Copy this file to your project's test directory and adjust the package name.
// Dependencies: io.rest-assured:rest-assured:5.4.0, spring-boot-starter-test
//
// This template tests ASVS V2.1.x password rules via black-box HTTP.
// No MockMvc, no internal repository access for assertions.

package YOUR_PACKAGE;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.Map;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvsvPasswordTestTemplate {

    @LocalServerPort int port;

    @BeforeEach
    void setup() { RestAssured.port = port; }

    @Test @Tag("ASVS") @Tag("ASVS-V2.1.1")
    void asvs_V2_1_1_passwordMinLength12_rejectsShorter() {
        given().contentType(ContentType.JSON)
            .body(Map.of("email", "test@test.com", "password", "short11char"))
        .when().post("/api/auth/email/signup")
        .then().statusCode(400);
    }

    @Test @Tag("ASVS") @Tag("ASVS-V2.1.2")
    void asvs_V2_1_2_password64CharsAllowed_129Rejected() {
        given().contentType(ContentType.JSON)
            .body(Map.of("email", "test64@test.com", "password", "a".repeat(64)))
        .when().post("/api/auth/email/signup")
        .then().statusCode(201);

        given().contentType(ContentType.JSON)
            .body(Map.of("email", "test129@test.com", "password", "a".repeat(129)))
        .when().post("/api/auth/email/signup")
        .then().statusCode(400);
    }

    @Test @Tag("ASVS") @Tag("ASVS-V2.1.4")
    void asvs_V2_1_4_unicodeAndEmojiAllowed() {
        given().contentType(ContentType.JSON)
            .body(Map.of("email", "emoji@test.com", "password", "correcthorse🐴battery12"))
        .when().post("/api/auth/email/signup")
        .then().statusCode(201);
    }

    @Test @Tag("ASVS") @Tag("ASVS-V2.1.9")
    void asvs_V2_1_9_noCompositionRulesEnforced() {
        given().contentType(ContentType.JSON)
            .body(Map.of("email", "lower@test.com", "password", "alllowercaseonly"))
        .when().post("/api/auth/email/signup")
        .then().statusCode(201);
    }
}
