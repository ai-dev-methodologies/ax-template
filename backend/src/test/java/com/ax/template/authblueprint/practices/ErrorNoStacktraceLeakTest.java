package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.RestAssured;
import io.restassured.response.Response;
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

        // P2-117: these extractions are deliberately on NON-2xx responses, so `pathAt` pins the
        // exact status PracticesProblemDetailAdvice maps each exception to. Without it every
        // marker assertion below is negative and would pass vacuously on any response at all —
        // including a container error page from a server that is not this application.
        String[][] cases = {
            {"/practices/demo/bad", "400"},      // IllegalArgumentException  → 400 Bad Argument
            {"/practices/demo/missing", "404"},  // NoSuchElementException    → 404 Resource Not Found
        };

        for (String[] testCase : cases) {
            String path = testCase[0];
            int expectedStatus = Integer.parseInt(testCase[1]);
            Response r = given().when().get(path).then().extract().response();
            HttpExtract.pathAt(r, expectedStatus, "title", "GET " + path + " (PRACTICES-ERR-003)");
            String body = r.asString();
            for (String marker : forbiddenMarkers) {
                assertThat(body)
                        .as("%s response must not leak '%s' (full body: %s)", path, marker, body)
                        .doesNotContain(marker);
            }
        }
    }
}
