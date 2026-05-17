package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-ERR-003")
class ErrorNoStacktraceLeakTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_ERR_003_responseBodyDoesNotContainStacktraceMarkers() {
        // Markers indicating raw stack-trace / exception class names leaking to the client.
        String[] forbiddenMarkers = {
            "java.lang.",
            "Exception",
            "\tat ",
            "Caused by:",
            "StackTrace",
        };

        for (String path : new String[]{"/practices/demo/bad", "/practices/demo/missing"}) {
            Response r = given().when().get(path).then().extract().response();
            String body = r.asString();
            for (String marker : forbiddenMarkers) {
                assertThat(body)
                        .as("%s response must not leak '%s' (full body: %s)", path, marker, body)
                        .doesNotContain(marker);
            }
        }
    }
}
