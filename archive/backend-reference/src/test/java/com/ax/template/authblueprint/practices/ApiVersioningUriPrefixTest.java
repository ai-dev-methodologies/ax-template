package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-API-003")
class ApiVersioningUriPrefixTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_API_003_listEndpointSitsUnderVersionedPath() {
        // The versioned path resolves and returns 200.
        given()
                .when().get("/practices/demo/v1/parents")
                .then().statusCode(200);

        // The same path WITHOUT /v1/ must NOT successfully resolve — that is the contract
        // of URI versioning. The exact non-200 status varies by SecurityFilterChain
        // configuration (401 / 403 / 404), but it MUST NOT be 200.
        int unversioned = given().when().get("/practices/demo/parents").then().extract().statusCode();
        assertThat(unversioned)
                .as("un-versioned URI must NOT successfully resolve (got %d)", unversioned)
                .isNotEqualTo(200);
    }

    @Test
    void practices_API_003_handlerMappingDeclaresV1Segment() throws Exception {
        Method listParents = PracticesDemoController.class.getDeclaredMethod(
                "listParents", int.class, int.class);
        GetMapping ann = listParents.getAnnotation(GetMapping.class);
        assertThat(ann).isNotNull();
        boolean hasV1 = Arrays.stream(ann.value()).anyMatch(p -> p.contains("/v1/"));
        assertThat(hasV1)
                .as("@GetMapping value must contain the /v1/ versioning segment")
                .isTrue();
    }
}
