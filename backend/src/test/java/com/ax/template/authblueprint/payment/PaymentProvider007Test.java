package com.ax.template.authblueprint.payment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance test for PAYMENT-PROVIDER-007.
 *
 * <p>Spec: {@code specs/payment-l0.yaml#PAYMENT-PROVIDER-007} — when a provider
 * call's wall-clock latency exceeds {@code payment.provider.slow-threshold-ms}, the
 * system MUST (1) increment Micrometer counter {@code payment_provider_slow_total}
 * and (2) emit a WARN-level log. The payment state is NOT changed: slow is an
 * observability concern, not a failure outcome.
 *
 * <p>Threshold is lowered to 10ms via {@link TestPropertySource} so the simulated
 * slow call (MockProvider FailureMode.SLOW_RESPONSE sleeps ~100ms) reliably trips
 * the assertion without making the test slow.
 *
 * <p>RED phase: assertions fail because (a) {@code payment_provider_slow_total}
 * counter is not registered, (b) no decorator measures latency, (c) MockProvider
 * does not understand {@code SLOW_RESPONSE} so it falls through APPROVED — no
 * sleep, no warn log. After GREEN ({@code SlowProviderLatencyDecorator} + enum
 * addition + MockProvider sleep), both assertions pass.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // PAYMENT-PROVIDER-007: lower the slow-provider threshold so the 100ms sleep
    // injected by MockProvider's SLOW_RESPONSE mode reliably trips it.
    "payment.provider.slow-threshold-ms=10"
})
@Tag("PAYMENT")
class PaymentProvider007Test {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    private WarnLogCapturingAppender warnAppender;
    private Logger rootLogger;

    @BeforeEach
    void setup() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        warnAppender = new WarnLogCapturingAppender();
        warnAppender.start();
        rootLogger.addAppender(warnAppender);
    }

    @AfterEach
    void teardown() {
        if (rootLogger != null && warnAppender != null) {
            rootLogger.detachAppender(warnAppender);
            warnAppender.stop();
        }
    }

    /**
     * PAYMENT-PROVIDER-007: slow provider response > slow_threshold_ms logged as
     * WARN with metrics counter {@code payment_provider_slow_total} incremented.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-PROVIDER-007")
    void providerSlowResponse_emitsWarnLogAndIncrementsSlowCounter() {
        String token = obtainToken("provider007@test.test", "MEMBER");

        double slowBefore = getCounterValue("payment_provider_slow_total");

        // SLOW_RESPONSE failure mode: MockProvider sleeps for ~100ms. With threshold
        // lowered to 10ms via @TestPropertySource, the decorator MUST observe the
        // call as slow and emit WARN + counter increment.
        Response response =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Test-Provider-Mode", "SLOW_RESPONSE")
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-provider007\"}")
            .when().post("/api/payments");

        // Slow is observability-only — the payment itself must succeed (state=CREATED)
        // and the HTTP response must be a normal create (201).
        assertThat(response.statusCode())
            .as("Slow provider call must not change the HTTP outcome (still 201 created)")
            .isEqualTo(201);

        // Counter assertion: payment_provider_slow_total must increment by 1.
        double slowAfter = getCounterValue("payment_provider_slow_total");
        assertThat(slowAfter - slowBefore)
            .as("payment_provider_slow_total must increment by 1 when provider latency exceeds slow_threshold_ms")
            .isEqualTo(1.0);

        // Log assertion: at least one WARN log event must mention the slow provider.
        List<ILoggingEvent> warnEvents = warnAppender.getWarnEvents();
        assertThat(warnEvents)
            .as("At least one WARN-level log event must be emitted when provider latency exceeds slow_threshold_ms")
            .isNotEmpty();

        // The WARN message must be grep-able by operators — accept either canonical phrase.
        boolean hasSlowMessage = warnEvents.stream().anyMatch(e -> {
            String msg = e.getFormattedMessage() == null ? "" : e.getFormattedMessage().toLowerCase();
            return msg.contains("slow provider") || msg.contains("provider_slow") || msg.contains("slow_response");
        });
        assertThat(hasSlowMessage)
            .as("WARN log must contain a grep-able phrase such as 'slow provider', 'provider_slow', or 'slow_response'")
            .isTrue();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

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
        .then().extract().path("accessToken");
    }

    /** Captures only WARN-or-higher log events; ignores INFO/DEBUG noise. */
    static class WarnLogCapturingAppender extends AppenderBase<ILoggingEvent> {

        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
                events.add(event);
            }
        }

        List<ILoggingEvent> getWarnEvents() {
            return List.copyOf(events);
        }
    }
}
