package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BACKLOG P2-120 — the live-wiring proof: {@link AxPort} really is registered, really does fire,
 * and really does publish this class's {@code @LocalServerPort}.
 *
 * <p><b>Why this test is the load-bearing one.</b> Every other file in the migration is a
 * DELETION — 140 manual {@code RestAssured.port = port;} assignments go away. A deletion-only
 * change has a false-green shape that is easy to miss: the suite can keep passing simply because
 * some earlier class in the same JVM left the right value in the global. This class contains
 * <b>no</b> assignment of its own and asserts the identity {@code RestAssured.port == port}
 * against the injected field, so if the extension is not registered (a lost
 * {@code junit-platform.properties}, a lost {@code META-INF/services} entry, an
 * {@code autodetection.include} pattern that stops matching) the value cannot be there by
 * accident and this fails.
 *
 * <p>Run: {@code ./gradlew testCommonPrimitives}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("COMMON_HTTP_EXTRACT")
class AxPortLiveWiringTest {

    // Deliberately never read by this class's own setup — the extension reads it.
    @LocalServerPort int port;

    @Test
    void theExtensionPublishesLocalServerPort_withoutThisClassAssigningAnything() {
        assertThat(port)
            .as("@LocalServerPort must be injected before any BeforeEachCallback runs")
            .isGreaterThan(0);

        assertThat(RestAssured.port)
            .as("no code in this class assigns RestAssured.port — if it equals the injected port, "
                + "AxPort is registered and fired")
            .isEqualTo(port);

        assertThat(AxPort.diagnose())
            .contains("MATCHES")
            .contains("@LocalServerPort")
            .contains(getClass().getName())
            .contains("RULED OUT");
    }

    @Test
    void aRealRequestReachesThisApplication_andItsFailureReportCarriesThePortVerdict() {
        // A real request, aimed only by the extension. 401 (not a connection error, not a
        // foreign server's Content-Type-less 404) is itself evidence the aim is this app.
        Response unauthenticated = given().when().get("/api/auth/me").then().extract().response();
        assertThat(unauthenticated.getStatusCode()).isEqualTo(401);

        // And the port verdict rides along in HttpExtract's failure message, which is the whole
        // point: the next P3-144 occurrence is adjudicated from the report, not from a re-run.
        assertThatThrownBy(() -> HttpExtract.path(unauthenticated, "userId", "GET /api/auth/me"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("RestAssured.port=" + port)
            .hasMessageContaining("port authority: MATCHES")
            .hasMessageContaining("A mistargeted port is RULED OUT");
    }

    @Test
    void driftAgainstARealPublication_isConvicted() {
        assertThat(AxPort.diagnoseAgainst(port)).contains("MATCHES");
        assertThat(AxPort.diagnoseAgainst(8080))
            .as("8080 is rest-assured's default — the impostor measured on this machine")
            .contains("CLOBBERED")
            .contains("published " + port);
    }
}
