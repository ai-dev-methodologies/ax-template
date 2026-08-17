package com.ax.template.authblueprint.sample;

import com.ax.template.authblueprint.common.AxPort;

import io.restassured.RestAssured;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PASS fixture. Everything a compliant test may do with the process-global port, so that a
 * guard which merely grepped for the substring `RestAssured.port` would fail this fixture:
 *   - declare @LocalServerPort and never publish it by hand (the extension does that),
 *   - READ RestAssured.port,
 *   - COMPARE against it,
 *   - carry the literal "RestAssured.port=" in an assertion string,
 *   - mention `RestAssured.port = port;` inside a comment,
 *   - carry the forbidden line inside a Java TEXT BLOCK (documentation of the rule, quoted
 *     verbatim), which a scanner that mis-lexed `"""` would read as code,
 *   - aim one single request elsewhere with the per-request given().port(...),
 *   - aim at a stub through the declared override API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleComplianceTest {

    // The field stays; the manual `RestAssured.port = port;` that used to sit in a @BeforeEach
    // is gone, because the registered extension publishes it.
    @LocalServerPort int port;

    /**
     * The shape this domain's tests used to be written in, quoted verbatim so a reader can see
     * what was removed. It is a TEXT BLOCK: the characters below are a string literal, not code,
     * and a scanner that read `"""` as an empty string followed by the start of another one goes
     * out of phase and reads every line here as a statement — reporting a rule's own
     * documentation as a violation of it.
     */
    static final String WHAT_WAS_DELETED = """
        @BeforeEach
        void setUp() {
            RestAssured.port = port;
            io.restassured.RestAssured.port = port;
        }
        """;

    @Test
    void readingAndComparingTheGlobalIsFine() {
        assertThat(RestAssured.port).isEqualTo(port);
        assertThat(RestAssured.port).isNotEqualTo(8080);
        String report = "RestAssured.port=" + RestAssured.port;
        assertThat(report).contains("RestAssured.port=");
    }

    @Test
    void perRequestPortTouchesNoGlobal() {
        given().port(port).when().get("/api/health").then().statusCode(200);
    }

    @Test
    void aimingAtAStubIsDeclared() {
        AxPort.overrideForStub(45679);
        try {
            given().when().get("/stub").then().statusCode(404);
        } finally {
            AxPort.restoreAfterStub();
        }
    }
}
