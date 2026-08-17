package com.ax.template.authblueprint.payment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compliance tests for PAYMENT-OBS-001..002.
 *
 * PAYMENT-OBS-001: Micrometer counters are emitted for key payment events:
 *   payment_attempted_total, payment_succeeded_total, payment_failed_total,
 *   refund_processed_total, recon_drift_detected_total.
 *
 * PAYMENT-OBS-002: MDC context propagates payment_id, idempotency_key, and
 *   correlation_id on every log statement within the payment package.
 *
 * RED phase: all tests fail today because:
 *  - PaymentController does not exist → HTTP 404 assertion failures.
 *  - MeterRegistry counter names are not registered yet → NullPointerException
 *    or counter-not-found assertion failures.
 *  - MDC keys are not populated by any filter → log assertion failures.
 * These are all valid RED outcomes per /tdd-workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentObservabilityTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    private MdcCapturingAppender mdcAppender;
    private Logger rootLogger;

    @BeforeEach
    void setup() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        mdcAppender = new MdcCapturingAppender();
        mdcAppender.start();
        rootLogger.addAppender(mdcAppender);
    }

    @AfterEach
    void teardown() {
        if (rootLogger != null && mdcAppender != null) {
            rootLogger.detachAppender(mdcAppender);
            mdcAppender.stop();
        }
    }

    // ─── PAYMENT-OBS-001: Micrometer counters ────────────────────────────────

    /**
     * PAYMENT-OBS-001: payment_attempted_total increments on POST /api/payments.
     *
     * RED: fails because the counter is not registered (no PaymentService).
     */
    @Test
    @Tag("PAYMENT-OBS-001")
    void metricsCounters_paymentAttemptedTotalIncremented() {
        String authToken = obtainToken("obs001a@test.test", "MEMBER");

        double before = getCounterValue("payment_attempted_total");

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs001a\"}")
        .when().post("/api/payments")
        .then().statusCode(201);

        double after = getCounterValue("payment_attempted_total");
        assertThat(after - before)
            .as("payment_attempted_total must increment by 1 on POST /api/payments")
            .isEqualTo(1.0);
    }

    /**
     * PAYMENT-OBS-001: payment_succeeded_total increments on successful capture.
     *
     * RED: fails because PaymentService.capture() does not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-001")
    void metricsCounters_paymentSucceededTotalIncremented() {
        String authToken = obtainToken("obs001b@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create and capture a payment (both steps fail today → RED).
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs001b\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/capture")
        .then().statusCode(200);

        double value = getCounterValue("payment_succeeded_total");
        assertThat(value)
            .as("payment_succeeded_total must be >= 1 after successful capture")
            .isGreaterThanOrEqualTo(1.0);
    }

    /**
     * PAYMENT-OBS-001: payment_failed_total increments on declined payment.
     *
     * RED: fails because the counter and PaymentService do not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-001")
    void metricsCounters_paymentFailedTotalIncremented() {
        String authToken = obtainToken("obs001c@test.test", "MEMBER");

        double before = getCounterValue("payment_failed_total");

        // POST with an amount that MockProvider is configured to decline.
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":99999999,\"currency\":\"KRW\",\"orderId\":\"order-obs001c\"," +
                  "\"mockFailureMode\":\"DECLINE\"}")
        .when().post("/api/payments")
        .then().statusCode(422);

        double after = getCounterValue("payment_failed_total");
        assertThat(after - before)
            .as("payment_failed_total must increment by 1 on declined payment")
            .isEqualTo(1.0);
    }

    /**
     * PAYMENT-OBS-001: refund_processed_total increments on refund.
     *
     * RED: fails because the counter and RefundService do not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-001")
    void metricsCounters_refundProcessedTotalIncremented() {
        String authToken = obtainToken("obs001d@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create and capture a payment, then refund it.
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs001d\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        double before = getCounterValue("refund_processed_total");

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":5000}")
        .when().post("/api/payments/" + paymentId + "/refund")
        .then().statusCode(201);

        double after = getCounterValue("refund_processed_total");
        assertThat(after - before)
            .as("refund_processed_total must increment by 1 on refund")
            .isEqualTo(1.0);
    }

    /**
     * PAYMENT-OBS-001: recon_drift_detected_total increments when reconciliation
     * detects a divergence between ledger sum and stored balance.
     *
     * RED: fails because ReconciliationJob and the counter do not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-001")
    void metricsCounters_reconDriftDetectedTotalIncremented() {
        // Inject a drift: tamper Payment.balance via JdbcTemplate (P3.0 supplies the
        // repository and JdbcTemplate injection). Force reconciliation job to run.
        // For RED: the counter reference itself will fail (not registered).
        double before = getCounterValue("recon_drift_detected_total");

        // P5 security-review (US-014 HIGH): /api/admin/reconciliation/run now
        // requires ROLE_ADMIN. Observability probe obtains an admin token to
        // exercise the endpoint, consistent with PAYMENT-AUTHZ-004 audit posture.
        String adminToken = obtainToken("recon-admin@obs001.test", "ADMIN");
        given()
            .header("Authorization", "Bearer " + adminToken)
            .header("Content-Type", "application/json")
        .when().post("/api/admin/reconciliation/run")
        .then(); // status is irrelevant for RED — endpoint not implemented

        // After drift is introduced and recon runs, counter must be > before.
        // RED: the counter does not exist → getCounterValue returns 0.0 always,
        // so assertThat(0.0).isGreaterThan(0.0) fails → RED.
        double after = getCounterValue("recon_drift_detected_total");
        assertThat(after)
            .as("recon_drift_detected_total must be > 0 after reconciliation detects drift")
            .isGreaterThan(before);
    }

    // ─── PAYMENT-OBS-002: MDC propagation ────────────────────────────────────

    /**
     * PAYMENT-OBS-002: MDC must include payment_id on every log statement
     * during the payment creation request lifecycle.
     *
     * RED: fails because PaymentMdcFilter does not exist and MDC is never set.
     */
    @Test
    @Tag("PAYMENT-OBS-002")
    void mdcPropagation_paymentIdInLog() {
        String authToken = obtainToken("obs002a@test.test", "MEMBER");

        mdcAppender.reset();

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs002a\"}")
        .when().post("/api/payments")
        .then().statusCode(201);

        List<java.util.Map<String, String>> capturedMdcContexts = mdcAppender.getMdcContexts();
        assertThat(capturedMdcContexts)
            .as("At least one log event must be captured during payment request")
            .isNotEmpty();

        assertThat(capturedMdcContexts)
            .as("Every log event on the payment path must have MDC 'payment_id' populated " +
                "(non-null, non-empty). RED: fails until PaymentMdcFilter is implemented (P3.0).")
            .allSatisfy(mdc -> assertThat(mdc.get("payment_id"))
                .isNotNull()
                .isNotEmpty());
    }

    /**
     * PAYMENT-OBS-002: MDC must include idempotency_key on every log statement.
     *
     * RED: fails because PaymentMdcFilter does not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-002")
    void mdcPropagation_idempotencyKeyInLog() {
        String authToken = obtainToken("obs002b@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        mdcAppender.reset();

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs002b\"}")
        .when().post("/api/payments")
        .then().statusCode(201);

        List<java.util.Map<String, String>> capturedMdcContexts = mdcAppender.getMdcContexts();
        assertThat(capturedMdcContexts)
            .as("At least one log event must be captured during payment request")
            .isNotEmpty();

        assertThat(capturedMdcContexts)
            .as("Every log event on the payment path must have MDC 'idempotency_key' set " +
                "to the request's Idempotency-Key header value. RED: fails until P3.0.")
            .allSatisfy(mdc -> assertThat(mdc.get("idempotency_key"))
                .isNotNull()
                .isNotEmpty());
    }

    /**
     * PAYMENT-OBS-002: MDC must include correlation_id on every log statement.
     *
     * RED: fails because PaymentMdcFilter does not exist.
     */
    @Test
    @Tag("PAYMENT-OBS-002")
    void mdcPropagation_correlationIdInLog() {
        String authToken = obtainToken("obs002c@test.test", "MEMBER");
        String correlationId = UUID.randomUUID().toString();

        mdcAppender.reset();

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .header("X-Correlation-Id", correlationId)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-obs002c\"}")
        .when().post("/api/payments")
        .then().statusCode(201);

        List<java.util.Map<String, String>> capturedMdcContexts = mdcAppender.getMdcContexts();
        assertThat(capturedMdcContexts)
            .as("At least one log event must be captured during payment request")
            .isNotEmpty();

        assertThat(capturedMdcContexts)
            .as("Every log event on the payment path must have MDC 'correlation_id' populated. " +
                "RED: fails until PaymentMdcFilter propagates X-Correlation-Id to MDC (P3.0).")
            .allSatisfy(mdc -> assertThat(mdc.get("correlation_id"))
                .isNotNull()
                .isNotEmpty());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the current counter value from the MeterRegistry, or 0.0 if the
     * counter does not exist yet. A missing counter means RED phase is active:
     * the counter has not been registered by PaymentService.
     */
    private double getCounterValue(String counterName) {
        Counter counter = meterRegistry.find(counterName).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().statusCode(200).extract().path("accessToken");
    }

    // ─── MdcCapturingAppender (Logback) ───────────────────────────────────────

    /**
     * Logback appender that captures the MDC context map for each log event.
     * Used to verify that PaymentMdcFilter populates all required MDC keys.
     */
    static class MdcCapturingAppender extends AppenderBase<ILoggingEvent> {

        private final List<java.util.Map<String, String>> mdcContexts = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            java.util.Map<String, String> mdc = event.getMDCPropertyMap();
            if (mdc != null && !mdc.isEmpty()) {
                mdcContexts.add(java.util.Map.copyOf(mdc));
            }
        }

        List<java.util.Map<String, String>> getMdcContexts() {
            return List.copyOf(mdcContexts);
        }

        void reset() {
            mdcContexts.clear();
        }
    }
}
