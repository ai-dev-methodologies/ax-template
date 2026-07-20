package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERIZATION of the request-path money defect (wave-2 finding-4 — the mirror image of P1-68's
 * response-path mismatch). Both {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount} and
 * {@code specs/payment-l0.yaml#PAYMENT-MONEY-002} declare that a JSON <em>integer</em> amount is
 * <b>minor units</b> ("1099 for USD $10.99"). But {@link MoneyDeserializer} maps the integer {@code 1099}
 * to {@code BigDecimal.valueOf(1099)} with NO divide-by-100, and {@code PaymentService#scale} then
 * applies the USD minor-unit scale to yield {@code 1099.00} — a MAJOR-unit value. Net effect: a
 * contract-compliant USD integer-minor request for $10.99 is charged {@code $1099.00} — a silent 100×
 * overcharge.
 *
 * <p>This is the request-side twin of {@code MoneyContractParityTest} (which pins the response side).
 * It is left {@link Disabled} on purpose: enabling it flips RED and hands wave-3's money reconciliation
 * a precise target. Do NOT "fix" it by asserting the buggy {@code 1099.00} — the assertion below encodes
 * the CONTRACT-CORRECT expectation ($10.99), which is exactly what wave-3 must make pass.
 */
@Tag("PAYMENT")
@Disabled("wave-3 money reconciliation RED target — request-path integer-minor 100x overcharge "
        + "(finding-4). Enable when MoneyDeserializer interprets JSON integers as minor units per "
        + "contracts/payment-openapi.yaml#MoneyAmount + specs/payment-l0.yaml#PAYMENT-MONEY-002, OR the "
        + "contract drops integer-minor for fraction-digit currencies. Keep DISABLED until then so R25 stays green.")
class PaymentAmountMinorUnitCharacterizationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Replicates {@code PaymentService#scale} (private) for USD: setScale(2, UNNECESSARY). */
    private static BigDecimal scaleUsd(BigDecimal raw) {
        return raw.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Test
    void usdIntegerMinorAmount_isChargedAtContractValue_not100xToLarge() {
        // Exercises the REAL request path: MoneyDeserializer (via @JsonDeserialize on CreatePaymentRequest.amount).
        // contracts/payment-openapi.yaml: integer 1099 for USD == minor units == $10.99.
        CreatePaymentRequest req = MAPPER.readValue(
                "{\"amount\":1099,\"currency\":\"USD\",\"orderId\":\"o\"}", CreatePaymentRequest.class);

        BigDecimal effectiveMajorCharge = scaleUsd(req.amount());

        // CONTRACT-CORRECT expectation. Currently effectiveMajorCharge == 1099.00 (a $1099.00 charge,
        // 100x the intended $10.99), so this assertion FAILS today — hence @Disabled. Reproduction:
        //   POST /api/payments {"amount":1099,"currency":"USD",...}  →  Payment.amount persisted as 1099.00.
        assertThat(effectiveMajorCharge)
                .as("integer 1099 for USD is minor units ($10.99) per the contract, but the deserializer "
                        + "treats it as major units and PaymentService#scale yields 1099.00 = $1099.00 (100x). "
                        + "Reconcile MoneyDeserializer/contract/money.ts in wave-3.")
                .isEqualByComparingTo(new BigDecimal("10.99"));
    }
}
