package com.ax.template.authblueprint.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * BACKLOG P2-117 — deliberate-break proof that {@link HttpExtract} is diagnosable on every
 * response shape that a blind {@code extract().path(...)} renders undiagnosable.
 *
 * <p>Each test drives a JDK-builtin stub server (no Spring context, no application code) so the
 * four shapes from the P3-144 matrix can be produced exactly, and asserts that the resulting
 * failure message actually carries the status, the content type and the body — the three facts
 * whose absence made the 2026-08-15 R25 failure untraceable.
 */
@Tag("COMMON_HTTP_EXTRACT")
class HttpExtractDiagnosabilityTest {

    private static HttpServer server;

    @BeforeAll
    static void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // Shape 1 — no Content-Type header at all. The single preimage of P3-144's
        // IllegalStateException. sendResponseHeaders() adds no content type of its own.
        server.createContext("/no-content-type", exchange -> {
            byte[] body = "{\"probe\":\"no-content-type\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        // Shape 2 — 200 + application/json + empty body → JsonPathException, NOT shape 1.
        server.createContext("/empty-json", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        // Shape 3 — 401 + application/problem+json. Parses fine; the path is simply absent, so a
        // blind extract yields null and no exception at all.
        server.createContext("/problem-401", exchange -> {
            byte[] body = ("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                + "\"detail\":\"stub authentication failure\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(401, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        // Shape 4 companion — what answers when RestAssured is pointed at a port that is not this
        // application: a foreign container page, no JSON anywhere.
        server.createContext("/foreign-server", exchange -> {
            byte[] body = "<html><body>Not Found</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html;charset=utf-8");
            exchange.getResponseHeaders().set("Server", "stub-not-tomcat");
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.start();
        // P2-120: this stub is deliberately NOT this application, so the aim is declared through
        // the one API that says so rather than by assigning the process-global by hand. AxPort
        // records the override, so a failure below reports "STUB OVERRIDE" instead of leaving a
        // reader to wonder whether the port was simply wrong.
        AxPort.overrideForStub(server.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        AxPort.restoreAfterStub();
        server.stop(0);
    }

    @Test
    void shape1_noContentType_isReportedWithStatusContentTypeAndBody() {
        Response r = given().when().get("/no-content-type");

        // Baseline: a blind extract dies with a parser error that names nothing.
        assertThatThrownBy(() -> r.then().extract().path("token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no content-type was present");

        assertThatThrownBy(() -> HttpExtract.path(r, "token", "GET /no-content-type"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("GET /no-content-type")
            .hasMessageContaining("status=200")
            .hasMessageContaining("content-type=<absent>")
            .hasMessageContaining("{\"probe\":\"no-content-type\"}")
            .hasMessageContaining("RestAssured.port=");
    }

    @Test
    void shape2_jsonWithEmptyBody_isReportedWithStatusContentTypeAndBody() {
        Response r = given().when().get("/empty-json");

        assertThatThrownBy(() -> HttpExtract.path(r, "token", "GET /empty-json"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("GET /empty-json")
            .hasMessageContaining("status=200")
            .hasMessageContaining("content-type=application/json")
            .hasMessageContaining("body=<empty>")
            .hasMessageContaining("RestAssured.port=");
    }

    @Test
    void shape3_problemJson401_failsInsteadOfReturningNull() {
        Response r = given().when().get("/problem-401");

        // The shape that raises nothing at all: a blind extract just hands back null.
        assertThat((Object) r.then().extract().path("accessToken")).isNull();

        assertThatThrownBy(() -> HttpExtract.path(r, "accessToken", "POST /login (stub)"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("POST /login (stub)")
            .hasMessageContaining("status=401")
            .hasMessageContaining("content-type=application/problem+json")
            .hasMessageContaining("stub authentication failure")
            .hasMessageContaining("RestAssured.port=");

        // pathAt pins the status, so a deliberate non-2xx extraction still works and still
        // refuses a null value.
        String title = HttpExtract.pathAt(r, 401, "title", "POST /login (stub)");
        assertThat(title).isEqualTo("Unauthorized");
        assertThatThrownBy(() -> HttpExtract.pathAt(r, 403, "title", "POST /login (stub)"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected HTTP 403")
            .hasMessageContaining("status=401");
    }

    @Test
    void shape4_wrongPortAndForeignServer_areBothNamed() {
        // The port-0 half of the matrix is out of the helper's reach by construction: RestAssured
        // raises before any Response object exists, so there is nothing to describe.
        assertThatThrownBy(() -> given().port(0).when().get("/no-content-type"))
            .isInstanceOf(IllegalArgumentException.class);

        // What IS reachable is the response a foreign server on a wrong port returns — and the
        // helper names the port so the wrong-target hypothesis can be tested from the message.
        Response r = given().when().get("/foreign-server");
        assertThatThrownBy(() -> HttpExtract.path(r, "token", "GET /foreign-server"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("GET /foreign-server")
            .hasMessageContaining("status=404")
            .hasMessageContaining("content-type=text/html;charset=utf-8")
            .hasMessageContaining("Not Found")
            .hasMessageContaining("Server=stub-not-tomcat")
            .hasMessageContaining("RestAssured.port=" + server.getAddress().getPort());
    }
}
