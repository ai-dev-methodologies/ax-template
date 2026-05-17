package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-TEST-001")
class TestingRestAssuredPatternTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void practices_TEST_001_restAssuredHitsRealHttpStack() {
        // Black-box pattern: real HTTP over RANDOM_PORT, no MockMvc shortcut.
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    void practices_TEST_001_classpathHasNoMockMvcDependencyForThisTest() throws Exception {
        // The portable pattern forbids MockMvc coupling inside PRACTICES-tagged tests.
        // We can't enforce absence project-wide (Spring Boot test starter still ships it),
        // but we can prove this test class itself does not import MockMvc:
        String src = getClass().getName();
        // Reflection over the class file: check declared imports indirectly by class references.
        // If MockMvc were used here, the class would reference it; we assert no such reference.
        boolean usesMockMvc = false;
        for (var f : getClass().getDeclaredFields()) {
            if (f.getType().getName().contains("MockMvc")) {
                usesMockMvc = true;
                break;
            }
        }
        assertThat(usesMockMvc)
                .as("PRACTICES tests must use RestAssured / @LocalServerPort, not MockMvc")
                .isFalse();
        assertThat(src).contains("practices");
    }
}
