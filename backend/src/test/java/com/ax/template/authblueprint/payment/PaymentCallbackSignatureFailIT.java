package com.ax.template.authblueprint.payment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R13 dogfood — GAP-C closure for PAYMENT-CALLBACK-001 (signature verification).
 *
 * <p>Two sub-cases — both via RestAssured black-box POST /api/payments/callback/{provider}
 * with a stub verifier registered exclusively for this test (slug "stubpg"):
 *
 * <ol>
 *   <li><b>signature_mismatch_with_extractableOrderId</b> — verifier returns
 *       {@code Result.invalid("SIGNATURE_MISMATCH", inboundOrderId)} per the
 *       R11 nullable-orderId carve-out. Asserts HTTP 401, audit ledger row
 *       persisted with {@code outcome=signature_fail / inboundOrderId=<value>},
 *       and counter {@code payment_callback_signature_fail_total{provider=stubpg}}
 *       incremented by 1.</li>
 *   <li><b>signature_mismatch_payloadWithNoOrderId</b> — verifier returns the
 *       single-arg {@code Result.invalid("MISSING_SIGNATURE")} (no inbound order
 *       id extractable). Asserts HTTP 401, audit ledger row persisted with
 *       {@code paymentId=null} (R11 GAP-B closure — payment_events.payment_id
 *       is nullable) and {@code inboundOrderId="(none)"}.</li>
 * </ol>
 *
 * <p>Spec anchors:
 * <ul>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-001 — verify signature BEFORE
 *       reading any payment state; 401 on missing/mismatched; ledger audit row
 *       emitted regardless of lookup outcome.</li>
 *   <li>blueprints/payment-manifest.yaml#callback — declares the verifier SPI
 *       and the audit ledger requirement.</li>
 * </ul>
 *
 * <p>Test isolation: the stub verifier ships as a {@link TestConfiguration}
 * bean and registers under slug "stubpg" — disjoint from any production-mode
 * verifier a fork-receiver might wire (kginicis / nicepay / kcp / tossv1).
 * The registry composes both, so this test does NOT shadow production beans.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PaymentCallbackSignatureFailIT.StubVerifierConfig.class)
@Tag("PAYMENT")
class PaymentCallbackSignatureFailIT {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    PaymentEventRepository eventRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    /**
     * PAYMENT-CALLBACK-001 sub-case A: signature mismatch with extractable
     * inbound orderId. Verifier returns {@code invalid(reason, orderId)}.
     */
    @Test
    @Tag("PAYMENT-CALLBACK-001")
    void signatureMismatch_withExtractableOrderId_audits401AndIncrementsCounter() {
        String inboundOrderId = "order-r13-" + UUID.randomUUID();
        StubVerifierConfig.nextResult = PaymentCallbackVerifier.Result.invalid(
            "SIGNATURE_MISMATCH", inboundOrderId);

        long ledgerCountBefore = eventRepository.count();
        double counterBefore = counterValue("payment_callback_signature_fail_total");

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("orderId", inboundOrderId)
            .formParam("signature", "deliberately-wrong-signature")
        .when().post("/api/payments/callback/stubpg")
        .then().statusCode(401);

        // Counter: incremented exactly once per failed callback (PAYMENT-OBS-001
        // family — same Micrometer registry as other payment counters).
        double counterAfter = counterValue("payment_callback_signature_fail_total");
        assertThat(counterAfter - counterBefore)
            .as("payment_callback_signature_fail_total must increment by 1")
            .isEqualTo(1.0);

        // Ledger: exactly one new row appended, tagged source=callback /
        // outcome=signature_fail / provider=stubpg / inboundOrderId=<value>.
        long ledgerCountAfter = eventRepository.count();
        assertThat(ledgerCountAfter - ledgerCountBefore)
            .as("PAYMENT-CALLBACK-001: exactly one signature_fail row appended")
            .isEqualTo(1L);

        PaymentEvent latest = eventRepository.findAll().stream()
            .reduce((a, b) -> b.getOccurredAt().isAfter(a.getOccurredAt()) ? b : a)
            .orElseThrow();
        assertThat(latest.getType()).isEqualTo(PaymentEventType.CALLBACK_SIGNATURE_FAIL);
        String payload = latest.getPayload();
        assertThat(payload).contains("\"source\":\"callback\"");
        assertThat(payload).contains("\"outcome\":\"signature_fail\"");
        assertThat(payload).contains("\"provider\":\"stubpg\"");
        assertThat(payload).contains(inboundOrderId);
    }

    /**
     * PAYMENT-CALLBACK-001 sub-case B: signature mismatch with no extractable
     * orderId. Verifier returns the single-arg {@code invalid(reason)} — the
     * audit row persists with {@code paymentId=null} (R11 GAP-B closure) and
     * {@code inboundOrderId="(none)"}.
     */
    @Test
    @Tag("PAYMENT-CALLBACK-001")
    void signatureMismatch_payloadWithNoOrderId_persistsNullPaymentIdRow() {
        StubVerifierConfig.nextResult = PaymentCallbackVerifier.Result.invalid("MISSING_SIGNATURE");

        long ledgerCountBefore = eventRepository.count();

        given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("noise", "no-merchant-fields-here")
        .when().post("/api/payments/callback/stubpg")
        .then().statusCode(401);

        long ledgerCountAfter = eventRepository.count();
        assertThat(ledgerCountAfter - ledgerCountBefore)
            .as("PAYMENT-CALLBACK-001: signature_fail row appended even without orderId")
            .isEqualTo(1L);

        PaymentEvent latest = eventRepository.findAll().stream()
            .reduce((a, b) -> b.getOccurredAt().isAfter(a.getOccurredAt()) ? b : a)
            .orElseThrow();
        assertThat(latest.getType()).isEqualTo(PaymentEventType.CALLBACK_SIGNATURE_FAIL);
        // R11 GAP-B closure: payment_events.payment_id is nullable for orphan
        // audit rows. Direct verification that paymentId is null.
        assertThat(latest.getPaymentId())
            .as("R11 GAP-B: lookup-miss audit rows MUST persist with paymentId=null")
            .isNull();
        String payload = latest.getPayload();
        assertThat(payload).contains("\"failReason\":\"MISSING_SIGNATURE\"");
        assertThat(payload).contains("\"inboundOrderId\":\"(none)\"");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private double counterValue(String name) {
        Counter c = meterRegistry.find(name).counter();
        return c != null ? c.count() : 0.0;
    }

    /**
     * Stub verifier wired only for this IT. Returns whatever Result the test
     * stashes in {@link #nextResult} (static, mutated per-test). The slug
     * "stubpg" is intentionally outside the production type_allowed set so
     * this stub never collides with a fork-receiver's real verifier.
     */
    @TestConfiguration
    static class StubVerifierConfig {

        static volatile PaymentCallbackVerifier.Result nextResult;

        @Bean
        PaymentCallbackVerifier stubVerifier() {
            return new PaymentCallbackVerifier() {
                @Override
                public String providerName() {
                    return "stubpg";
                }

                @Override
                public Result verify(Map<String, String> rawPayload, Map<String, String> headers) {
                    PaymentCallbackVerifier.Result r = nextResult;
                    if (r == null) {
                        return Result.invalid("STUB_NOT_PRIMED");
                    }
                    return r;
                }
            };
        }
    }
}
