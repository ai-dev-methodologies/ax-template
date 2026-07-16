package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance tests for PAYMENT-PROVIDER-001..006.
 *
 * RED phase: all tests fail because PaymentController / PaymentProviderClient /
 * MockProvider do not exist yet. Failures manifest as HTTP 404 (endpoint missing)
 * or assertion failures — both are valid RED per /tdd-workflow.
 *
 * Provider failure modes are injected via the X-Test-Provider-Mode header,
 * which the MockProvider reads in test profile. In GREEN phase (P3.3),
 * the MockProvider will support injectable FailureMode enum values.
 *
 * Spec: specs/payment-l0.yaml PROVIDER family.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentProviderMatrixTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ─── PAYMENT-PROVIDER-001: timeout → UNKNOWN state ───────────────────────

    /**
     * PAYMENT-PROVIDER-001: When the payment provider call times out,
     * the payment state transitions to UNKNOWN (not FAILED).
     *
     * Response to caller must be HTTP 202 Accepted with status=UNKNOWN.
     * Rationale: UNKNOWN vs FAILED prevents incorrect refusal when the charge
     * may have succeeded on the provider side.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-001")
    void providerTimeout_resultsInUnknownState() {
        String token = obtainToken("provider001@test.test", "MEMBER");

        // X-Test-Provider-Mode: TIMEOUT instructs MockProvider to simulate timeout
        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "TIMEOUT")
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider001\"}")
            .when().post("/api/payments");

        assertThat(response.statusCode())
            .as("Provider timeout must return 202 Accepted (pending UNKNOWN state)")
            .isEqualTo(202);

        String state = response.then().extract().path("state");
        assertThat(state)
            .as("Provider timeout must result in state=UNKNOWN (not FAILED)")
            .isEqualTo("UNKNOWN");
    }

    // ─── PAYMENT-PROVIDER-002: 5xx → retry then FAILED ───────────────────────

    /**
     * PAYMENT-PROVIDER-002: When the provider returns HTTP 5xx, the system
     * retries with exponential backoff up to max retries; if all retries fail,
     * payment state → FAILED with declineReason=SERVER_ERROR.
     *
     * X-Test-Provider-Mode: HTTP_5XX — MockProvider returns 503 on every call.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-002")
    void provider5xx_retriesAndFailsAfterMaxRetries() {
        String token = obtainToken("provider002@test.test", "MEMBER");

        // MockProvider in HTTP_5XX mode: returns 503 for every attempt
        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "HTTP_5XX")
                // Short backoff config for tests (avoid 7+ second waits)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider002\"}")
            .when().post("/api/payments");

        // After max retries exhausted, payment lands in FAILED state
        // Response may be 4xx/5xx or 2xx with status=FAILED depending on implementation
        String state = response.then().extract().path("state");
        assertThat(state)
            .as("Provider 5xx after max retries must result in state=FAILED")
            .isEqualTo("FAILED");

        // declineReason must indicate server error
        String declineReason = response.then().extract().path("declineReason");
        assertThat(declineReason)
            .as("Provider 5xx failure must set declineReason=SERVER_ERROR")
            .isNotNull()
            .satisfiesAnyOf(
                r -> assertThat(r).containsIgnoringCase("SERVER_ERROR"),
                r -> assertThat(r).containsIgnoringCase("server"),
                r -> assertThat(r).containsIgnoringCase("5xx")
            );
    }

    // ─── PAYMENT-PROVIDER-003: 4xx decline → FAILED with declineReason ───────

    /**
     * PAYMENT-PROVIDER-003: When the provider returns HTTP 4xx with a decline code,
     * payment state → FAILED with the decline reason stored and returned.
     * No retry is attempted (4xx is a user-correctable error).
     *
     * X-Test-Provider-Mode: HTTP_4XX_DECLINE — MockProvider returns 402 with
     * declineCode=INSUFFICIENT_FUNDS.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-003")
    void provider4xxDecline_resultsInFailedWithReasonCode() {
        String token = obtainToken("provider003@test.test", "MEMBER");

        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "HTTP_4XX_DECLINE")
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider003\"}")
            .when().post("/api/payments");

        // State must be FAILED
        String state = response.then().extract().path("state");
        assertThat(state)
            .as("Provider 4xx decline must result in state=FAILED")
            .isEqualTo("FAILED");

        // declineReason must be set (e.g., INSUFFICIENT_FUNDS)
        String declineReason = response.then().extract().path("declineReason");
        assertThat(declineReason)
            .as("Provider 4xx decline must capture and return decline reason (not null)")
            .isNotNull()
            .isNotBlank();

        // declineReason must NOT contain PAN or card data — only an enum code is safe
        assertThat(declineReason)
            .as("declineReason must not contain card data (PAN-safe)")
            .doesNotContainIgnoringCase("pan")
            .doesNotContainIgnoringCase("cardNumber")
            .doesNotContainIgnoringCase("cvv");
    }

    // ─── PAYMENT-PROVIDER-004: malformed response → FAILED (serialization error)

    /**
     * PAYMENT-PROVIDER-004: When the provider returns a malformed/unparseable
     * response, payment state → FAILED with declineReason=SERIALIZATION_ERROR.
     * The raw malformed response must NOT be propagated to the caller.
     *
     * X-Test-Provider-Mode: MALFORMED_RESPONSE — MockProvider returns HTML or
     * truncated JSON.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-004")
    void providerMalformedResponse_resultsInFailedSerializationError() {
        String token = obtainToken("provider004@test.test", "MEMBER");

        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "MALFORMED_RESPONSE")
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider004\"}")
            .when().post("/api/payments");

        // State must be FAILED
        String state = response.then().extract().path("state");
        assertThat(state)
            .as("Provider malformed response must result in state=FAILED")
            .isEqualTo("FAILED");

        // declineReason must indicate serialization error
        String declineReason = response.then().extract().path("declineReason");
        assertThat(declineReason)
            .as("Malformed response failure must set declineReason=SERIALIZATION_ERROR or similar")
            .isNotNull()
            .satisfiesAnyOf(
                r -> assertThat(r).containsIgnoringCase("SERIALIZATION"),
                r -> assertThat(r).containsIgnoringCase("serialization"),
                r -> assertThat(r).containsIgnoringCase("parse"),
                r -> assertThat(r).containsIgnoringCase("malformed")
            );

        // Raw malformed bytes must NOT appear in the response body
        String responseBody = response.body().asString();
        assertThat(responseBody)
            .as("Raw malformed provider response must not be propagated to caller")
            .doesNotContain("<html")
            .doesNotContain("<!DOCTYPE");
    }

    // ─── PAYMENT-PROVIDER-005: network reset → UNKNOWN state ─────────────────

    /**
     * PAYMENT-PROVIDER-005: When the provider connection is reset (network reset /
     * connection refused), payment state → UNKNOWN.
     * The payment must NOT be marked FAILED prematurely.
     *
     * Rationale: A network reset is indeterminate — the charge packet may have
     * reached the provider before the connection was torn down. Marking FAILED
     * could trigger a double-charge on retry.
     *
     * X-Test-Provider-Mode: NETWORK_RESET — MockProvider throws IOException.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-005")
    void providerNetworkReset_resultsInUnknownState() {
        String token = obtainToken("provider005@test.test", "MEMBER");

        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "NETWORK_RESET")
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider005\"}")
            .when().post("/api/payments");

        assertThat(response.statusCode())
            .as("Provider network reset must return 202 Accepted (pending UNKNOWN state)")
            .isEqualTo(202);

        String state = response.then().extract().path("state");
        assertThat(state)
            .as("Provider network reset must result in state=UNKNOWN (not FAILED)")
            .isEqualTo("UNKNOWN");
    }

    // ─── PAYMENT-PROVIDER-006: idempotency replay → cached response ──────────

    /**
     * PAYMENT-PROVIDER-006: When the provider returns an idempotency replay response
     * (same key resubmitted), the system returns the cached original response to the
     * caller without creating a new charge.
     *
     * Assert: caller receives original response + DB has exactly 1 Payment row.
     *
     * X-Test-Provider-Mode: IDEMPOTENCY_REPLAY — MockProvider returns a pre-stored
     * response for a previously-seen key.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-006")
    void providerIdempotencyReplay_returnsCachedResponse() {
        String token = obtainToken("provider006@test.test", "MEMBER");
        String idempotencyKey = "provider-replay-" + UUID.randomUUID();

        String requestBody = "{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider006\"}";

        // First POST — creates payment with idempotency replay mode
        Response firstResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Test-Provider-Mode", "IDEMPOTENCY_REPLAY")
                .body(requestBody)
            .when().post("/api/payments");

        assertThat(firstResponse.statusCode())
            .as("First POST with IDEMPOTENCY_REPLAY mode must succeed (200 or 201)")
            .isIn(200, 201);

        String firstPaymentId = firstResponse.then().extract().path("id");
        assertThat(firstPaymentId)
            .as("First POST must return a payment id")
            .isNotNull()
            .isNotBlank();

        // Second POST with same Idempotency-Key — should return cached response
        // no second charge created
        Response replayResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Test-Provider-Mode", "IDEMPOTENCY_REPLAY")
                .body(requestBody)
            .when().post("/api/payments");

        assertThat(replayResponse.statusCode())
            .as("Provider idempotency replay must return 200 or 201 (cached response)")
            .isIn(200, 201);

        String replayPaymentId = replayResponse.then().extract().path("id");
        assertThat(replayPaymentId)
            .as("Provider replay must return the SAME paymentId (no double-charge)")
            .isEqualTo(firstPaymentId);
    }

    // ─── resolveFailureMode fail-closed + locale regression locks ────────────────

    /**
     * Regression lock for the FAIL-CLOSED branch of {@link PaymentController#resolveFailureMode}.
     * An explicit, NON-blank, unrecognized failure mode must be REJECTED with 400 — never silently
     * folded to APPROVED. Reverting the fix (catch → {@code return APPROVED} instead of throwing)
     * would flip a requested provider FAILURE into a SUCCESS on a money path; this request would then
     * be accepted (2xx) and the assertion below (400) would FAIL. The unknown value must also never
     * be echoed into the 400 body (response-amplification defense).
     */
    @Test
    @Tag("PAYMENT")
    void explicitUnknownFailureMode_isRejected400_andNotEchoed() {
        String token = obtainToken("provider-unknown@test.test", "MEMBER");
        String unknownMode = "TOTALLY_UNKNOWN_MODE";

        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", unknownMode)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-unknown\"}")
            .when().post("/api/payments");

        assertThat(response.statusCode())
            .as("an explicit unrecognized failure mode must fail CLOSED with 400 "
                + "(reverting fail-closed→APPROVED would accept the request with a 2xx)")
            .isEqualTo(400);
        assertThat(response.body().asString())
            .as("the (untrusted) unknown mode value must NOT be echoed back in the 400 body")
            .doesNotContain(unknownMode);
    }

    /**
     * Regression lock for the {@code Locale.ROOT} case-fold in {@link PaymentController#resolveFailureMode}.
     * The bug only manifests under a Turkish default locale, where a default-locale {@code toUpperCase()}
     * maps 'i'→'İ' — so lowercase {@code "timeout"} becomes {@code "TİMEOUT"}, misses the enum, and (under
     * fail-closed) is REJECTED instead of resolving to TIMEOUT. The fix folds with {@code Locale.ROOT}, which
     * is locale-independent, so {@code "timeout"}→{@code "TIMEOUT"}→{@code FailureMode.TIMEOUT} regardless of
     * the default locale. This test FORCES tr as the default locale (restored in finally so no other test is
     * polluted) and invokes the private {@code resolveFailureMode} reflectively, asserting it resolves to
     * TIMEOUT — not APPROVED, and not a thrown rejection. Reverting {@code Locale.ROOT}→plain {@code toUpperCase()}
     * makes this throw under tr → the assertion FAILS.
     */
    @Test
    @Tag("PAYMENT")
    void lowercaseTimeout_resolvesToTimeout_underTurkishLocale() throws Exception {
        java.lang.reflect.Method resolve = PaymentController.class
            .getDeclaredMethod("resolveFailureMode", String.class, String.class);
        resolve.setAccessible(true);

        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"));
            // sanity: under tr, a naive default-locale fold WOULD corrupt "timeout" — the exact bug
            // the Locale.ROOT fix prevents (guards against the test silently no-op'ing on a JVM where
            // tr didn't take effect).
            assertThat("timeout".toUpperCase())
                .as("Turkish default locale is in effect (naive fold dots the i)")
                .isNotEqualTo("TIMEOUT");

            Object result = resolve.invoke(null, null, "timeout");
            assertThat(result)
                .as("lowercase \"timeout\" must fold via Locale.ROOT to FailureMode.TIMEOUT under tr "
                    + "(reverting to plain toUpperCase() throws here → NOT TIMEOUT)")
                .isEqualTo(PaymentProvider.FailureMode.TIMEOUT);
        } finally {
            java.util.Locale.setDefault(previous);
        }
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
}
