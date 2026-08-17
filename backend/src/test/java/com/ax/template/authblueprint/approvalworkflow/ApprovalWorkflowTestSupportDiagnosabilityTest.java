package com.ax.template.authblueprint.approvalworkflow;

import com.sun.net.httpserver.HttpServer;

import io.restassured.RestAssured;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BACKLOG P3-144 — non-vacuity proof for {@link ApprovalWorkflowTestSupport}'s failure reporting.
 *
 * <p>On 2026-08-15 (R25 run E, HEAD {@code 9aa5eb20}) every test in {@code ApprovalFlowIT} failed
 * with a bare {@code java.lang.IllegalStateException} and nothing else — no status, no body, no
 * port — which made the flake undiagnosable from the report. Measured against the exact test
 * runtime classpath (rest-assured 6.0.1), that exception has exactly ONE cause: the response
 * carried <b>no {@code Content-Type} header at all</b>. (An empty body with a JSON content type
 * raises {@code JsonPathException}; {@code application/problem+json} parses and yields
 * {@code null}. A non-positive port raises {@code IllegalArgumentException("Port must be greater
 * than 0")}.) So the exception itself was evidence — "whatever answered is not this application's
 * login endpoint" — that the helper threw away.
 *
 * <p>This test pins the repaired behavior by driving the helper against a server that reproduces
 * that exact shape (Content-Type-less response). It is a deliberate-break check in the same spirit
 * as the {@code *ViolationProofTest} classes: if the helper ever regresses to extracting from an
 * unvalidated response, the assertion below fails because the thrown type flips back to
 * {@code IllegalStateException} and the message stops naming status / port / body.
 *
 * <p>No Spring context: this is a plain unit test that starts a JDK {@link HttpServer} on an
 * ephemeral port. Run: {@code ./gradlew testApprovalWorkflow}.
 */
@Tag("WORKFLOW")
class ApprovalWorkflowTestSupportDiagnosabilityTest {

    @Test
    @Tag("WF-DIAG-001")
    void contentTypeLessResponse_failsWithStatusPortAndBody_notABareParserError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Reproduces the only response shape that produced the observed IllegalStateException:
        // a body with NO Content-Type header. This is what a non-application responder (a
        // container-level error, or a foreign HTTP server on a mistargeted port) returns.
        server.createContext("/", exchange -> {
            byte[] body = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        int previousPort = RestAssured.port;
        String previousBaseUri = RestAssured.baseURI;
        try {
            // Pin the host explicitly: the stub binds 127.0.0.1, and RestAssured's default
            // baseURI ("http://localhost") is resolver-dependent (may yield ::1 first).
            RestAssured.baseURI = "http://127.0.0.1";
            ApprovalWorkflowTestSupport.useRandomPort(server.getAddress().getPort());

            assertThatThrownBy(() -> ApprovalWorkflowTestSupport.obtainToken("diag@example.com", "MEMBER"))
                .as("a Content-Type-less response must fail as itself, not as a parser error")
                .isInstanceOf(AssertionError.class)
                .isNotInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected HTTP 200")
                .hasMessageContaining("but was 404")
                .hasMessageContaining("RestAssured.port = " + server.getAddress().getPort())
                .hasMessageContaining("404 Not Found");
        } finally {
            RestAssured.port = previousPort;
            RestAssured.baseURI = previousBaseUri;
            server.stop(0);
        }
    }

    @Test
    @Tag("WF-DIAG-002")
    void nonPositivePort_isRejectedBeforeAnyRequestIsSent() {
        int previousPort = RestAssured.port;
        try {
            assertThatThrownBy(() -> ApprovalWorkflowTestSupport.useRandomPort(0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("non-positive port (0)");
            assertThat(RestAssured.port)
                .as("a rejected port must not be published to the process-global RestAssured.port")
                .isEqualTo(previousPort);
        } finally {
            RestAssured.port = previousPort;
        }
    }
}
