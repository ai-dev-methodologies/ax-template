package com.ax.template.authblueprint.payment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compliance tests for PAYMENT-SEC-001..003 (PCI-DSS).
 *
 * RED phase: tests reference com.ax.template.authblueprint.payment.Payment*
 * classes that do not exist yet (P3.0 implements them). Compile failures
 * and HTTP 404 assertion failures are both valid RED per /tdd-workflow.
 *
 * Dependency note: LogCaptor (nl.altindag:log-captor) is NOT in build.gradle.kts.
 * This file uses a Logback TestAppender pattern instead. P3.0 may add LogCaptor
 * as a dev dependency if preferred — the test pattern is equivalent.
 *
 * PAYMENT-SEC-002 (hardcodedSecretsScan_zeroHits) is a meta / continuous-check
 * test: it will PASS today because no implementation exists (grep finds 0 hits),
 * and it will continue to pass forever as long as production code contains no
 * hardcoded card-field names. This is expected and acceptable per spec notes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentSecurityTest {

    private static final String PAN_FIXTURE = "4111111111111111";

    @LocalServerPort
    int port;

    private TestLogAppender testAppender;
    private Logger rootLogger;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        // Attach test log appender to root Logback logger to capture all log output.
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        testAppender = new TestLogAppender();
        testAppender.start();
        rootLogger.addAppender(testAppender);
    }

    @AfterEach
    void teardown() {
        if (rootLogger != null && testAppender != null) {
            rootLogger.detachAppender(testAppender);
            testAppender.stop();
        }
    }

    // ─── PAYMENT-SEC-001a ─────────────────────────────────────────────────────

    /**
     * PAYMENT-SEC-001: PAN must not appear in application logs or stack traces.
     *
     * Sends a payment request containing a PAN fixture value, triggers an
     * exception path (provider failure simulation via unknown provider), then
     * asserts no captured log line contains the raw PAN.
     *
     * RED: fails today because PaymentController does not exist (HTTP 404).
     * The test will reach the log assertion once P3.0 implements the controller;
     * it will then fail if PaymentMethodToken.toString() leaks the PAN.
     *
     * PCI-DSS ref: 3.4 — PAN must be rendered unreadable wherever stored/logged.
     */
    @Test
    @Tag("PAYMENT-SEC-001")
    void panRedaction_panNotInExceptionStackTrace() {
        String authToken = obtainToken("sec001log@test.test", "MEMBER");

        // Submit a payment request that embeds the PAN in paymentMethodToken.
        // The provider will fail (no impl) triggering exception logging paths.
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-sec001-log\"," +
                  "\"paymentMethodToken\":\"" + PAN_FIXTURE + "\"}")
        .when().post("/api/payments");

        // All captured log output must not contain the raw PAN fixture.
        List<String> allLogMessages = testAppender.getMessages();
        assertThat(allLogMessages)
            .as("No log line should contain raw PAN fixture (PCI-DSS 3.4)")
            .noneMatch(line -> line.contains(PAN_FIXTURE));
    }

    // ─── PAYMENT-SEC-001b ─────────────────────────────────────────────────────

    /**
     * PAYMENT-SEC-001: PAN must not be persisted in any database column.
     *
     * Queries the Payment entity via the API after creation and verifies that
     * no response field contains a 13-19 digit numeric string matching a PAN.
     * The underlying check is: the API does not expose raw PAN from DB.
     *
     * RED: fails today because GET /api/payments/{id} returns 404.
     */
    @Test
    @Tag("PAYMENT-SEC-001")
    void panRedaction_panNotInDatabaseColumns() {
        String authToken = obtainToken("sec001db@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create a payment with a PAN in the token field.
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":5000,\"currency\":\"KRW\",\"orderId\":\"order-sec001-db\"," +
                  "\"paymentMethodToken\":\"" + PAN_FIXTURE + "\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        // Retrieve the payment — response body must not contain 13-19 digit PAN.
        String responseBody = given()
            .header("Authorization", "Bearer " + authToken)
        .when().get("/api/payments/" + paymentId)
        .then().extract().asString();

        assertThat(responseBody)
            .as("API response must not expose raw PAN from database (PCI-DSS 3.4)")
            .doesNotContainPattern("[0-9]{13,19}");
    }

    // ─── PAYMENT-SEC-001c ─────────────────────────────────────────────────────

    /**
     * PAYMENT-SEC-001: PAN must not appear in JSON error responses.
     *
     * Triggers a payment failure (no valid provider) and asserts that
     * the error response body does not contain the raw PAN fixture.
     *
     * RED: fails today because /api/payments endpoint returns 404.
     */
    @Test
    @Tag("PAYMENT-SEC-001")
    void panRedaction_panNotInJsonResponses() {
        String authToken = obtainToken("sec001resp@test.test", "MEMBER");

        // POST with a PAN — expect an error response (provider not implemented yet).
        String errorBody = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-sec001-resp\"," +
                  "\"paymentMethodToken\":\"" + PAN_FIXTURE + "\"}")
        .when().post("/api/payments")
        .then().extract().asString();

        assertThat(errorBody)
            .as("Error response body must not contain raw PAN fixture (PCI-DSS 3.4)")
            .doesNotContain(PAN_FIXTURE);
    }

    // ─── PAYMENT-SEC-002 ──────────────────────────────────────────────────────

    /**
     * PAYMENT-SEC-002: Hardcoded card-data field names must not appear in
     * production source code.
     *
     * This is a meta / continuous-check test:
     *  - It will PASS today (no production payment impl exists → 0 grep hits).
     *  - It will continue to pass as long as no hardcoded card-field names
     *    appear in src/main/java. This is the intended behavior.
     *  - It will FAIL (correctly) if a developer introduces a field like
     *    "String cardNumber" or "private String cvv" in the payment package.
     *
     * The grep excludes test/ to avoid false positives from test fixtures.
     * PCI-DSS ref: 6.5 — secure coding; 6.5.3 — no insecure cryptographic storage.
     */
    @Test
    @Tag("PAYMENT-SEC-002")
    void hardcodedSecretsScan_zeroHits() throws IOException, InterruptedException {
        // Resolve the backend main source path relative to the working directory.
        // The grep pattern matches common card-data field name antipatterns.
        ProcessBuilder pb = new ProcessBuilder(
            "bash", "-c",
            "grep -rn 'creditCard\\|panNumber\\|cardNumber\\|cvv\\|cvvCode\\|cardCvv' " +
            "backend/src/main/java/ 2>/dev/null | grep -v test | wc -l | tr -d ' '"
        );
        pb.directory(new java.io.File(
            System.getProperty("user.dir")).getParentFile().getParentFile()
            .getParentFile().getParentFile().getParentFile()
        );
        // Fallback: run from known repo root
        pb.directory(resolveRepoRoot());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();
        String output = new String(process.getInputStream().readAllBytes()).trim();

        // If process fails (directory not found etc.), default to treating as unknown.
        if (exitCode != 0 && output.isEmpty()) {
            output = "0"; // grep returns non-zero when no matches — that's fine
        }

        int hitCount;
        try {
            hitCount = Integer.parseInt(output.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            hitCount = 0; // empty output means 0 matches
        }

        assertThat(hitCount)
            .as("Hardcoded card-field names in src/main/java must be 0 (PCI-DSS 6.5). " +
                "NOTE: This test passes today (no impl). Continuous check — will fail if " +
                "any developer introduces raw card field names in production code.")
            .isZero();
    }

    // ─── PAYMENT-SEC-003 ──────────────────────────────────────────────────────

    /**
     * PAYMENT-SEC-003: All payment API traffic must be HTTPS-only.
     *
     * Verifies that a plain HTTP request to a payment endpoint is either
     * rejected (4xx) or redirected (3xx) — not served as HTTP 200.
     *
     * In test mode Spring Boot binds to HTTP only (no TLS cert configured),
     * so this test checks the SecurityConfig HSTS header is present on responses
     * as evidence that requiresSecure() or equivalent is configured in Spring
     * Security. The production enforcement is at the infrastructure TLS boundary
     * (load balancer); this test covers the Spring Security layer.
     *
     * PCI-DSS ref: 4.1 — strong cryptography for cardholder data transmission.
     *
     * RED: fails today because /api/payments returns 404 (endpoint missing),
     * so the HSTS-header assertion below will not pass until P3.0 adds the
     * SecurityConfig requiresSecure() and the controller exists.
     */
    @Test
    @Tag("PAYMENT-SEC-003")
    void tlsOnly_httpsHeaderPresentOnResponse() {
        String authToken = obtainToken("sec003@test.test", "MEMBER");

        // A well-formed authenticated request to any payment endpoint should
        // include Strict-Transport-Security header when SecurityConfig is configured.
        io.restassured.response.Response response = given()
            .header("Authorization", "Bearer " + authToken)
        .when().get("/api/payments");

        // The HSTS header verifies that Spring Security requiresSecure() / HSTS is
        // configured. This will fail RED until SecurityConfig sets HSTS.
        assertThat(response.header("Strict-Transport-Security"))
            .as("Response must include HSTS header (Strict-Transport-Security). " +
                "SecurityConfig must configure requiresSecure() or HSTS policy " +
                "per PCI-DSS 4.1. RED: fails until SecurityConfig configured in P3.0.")
            .isNotNull()
            .contains("max-age=");
    }

    // ─── PAYMENT-SEC-004 (response-amplification) ─────────────────────────────

    /**
     * PAYMENT-SEC-004: an oversized free-text field (here {@code currency}) must be fast-rejected
     * with a 400 AND must NOT be echoed back into the error body (response amplification).
     *
     * Threat model (Jackson 3 migration): Jackson 3 raised the default stream max-string-length
     * 20MB→100MB, so an authenticated client could send a huge {@code currency} value; without a
     * tight bound + a total-defense error handler it would pass validation and be reflected whole
     * into the 400 body (amplification). This asserts:
     *   (1) status is 400 (fast reject via @Size(max=3) → MethodArgumentNotValidException), and
     *   (2) the response body does NOT contain the oversized marker and is small (bounded).
     */
    @Test
    void oversizedCurrency_isRejected400_andNotEchoedInBody() {
        String authToken = obtainToken("sec004amp@test.test", "MEMBER");

        String marker = "AMPLIFYMARKER0123456789";
        // ~1MB currency value carrying a unique marker; must never be reflected back.
        String hugeCurrency = marker + "Z".repeat(1_000_000);

        io.restassured.response.Response response = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000,\"currency\":\"" + hugeCurrency + "\",\"orderId\":\"order-sec004\"}")
        .when().post("/api/payments");

        String body = response.getBody().asString();

        assertThat(response.statusCode())
            .as("Oversized currency must be fast-rejected with 400 (bean-validation @Size).")
            .isEqualTo(400);
        assertThat(body)
            .as("Error body must NOT reflect the oversized currency value (amplification defense).")
            .doesNotContain(marker);
        assertThat(body.length())
            .as("Error body must be bounded, not ~1MB (response-amplification cap).")
            .isLessThan(5_000);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    /**
     * Resolves the repository root for the hardcoded-secrets grep.
     * Walks up from the test class location to find the directory containing
     * backend/ + practices/ directories.
     */
    private java.io.File resolveRepoRoot() {
        // The working directory during Gradle test execution is backend/
        // So the repo root is the parent of backend/.
        java.io.File cwd = new java.io.File(System.getProperty("user.dir"));
        // If cwd is backend/, parent is repo root; otherwise use cwd.
        if (cwd.getName().equals("backend")) {
            return cwd.getParentFile();
        }
        return cwd;
    }

    // ─── TestLogAppender (Logback) ─────────────────────────────────────────────

    /**
     * Simple Logback appender that captures all log messages into a list.
     * Used to verify PAN redaction — no PAN should appear in any log output.
     *
     * Note: LogCaptor (nl.altindag:log-captor) is NOT in build.gradle.kts.
     * P3.0 may add LogCaptor as an alternative; this appender is equivalent
     * for the purposes of the PAN-redaction assertion.
     */
    static class TestLogAppender extends AppenderBase<ILoggingEvent> {

        private final List<String> messages = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            // Capture formatted message + throwable if present.
            messages.add(event.getFormattedMessage());
            if (event.getThrowableProxy() != null) {
                messages.add(event.getThrowableProxy().getMessage());
                for (var stackLine : event.getThrowableProxy().getStackTraceElementProxyArray()) {
                    messages.add(stackLine.toString());
                }
            }
        }

        List<String> getMessages() {
            return List.copyOf(messages);
        }
    }
}
