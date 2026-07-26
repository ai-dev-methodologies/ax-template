package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-69 closure — the request-path money defect, now a STANDING GREEN regression lock.
 *
 * <p>History: this class was written in wave-2 (finding-4) as a {@code @Disabled} RED target. Both
 * {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount} and
 * {@code specs/payment-l0.yaml#PAYMENT-MONEY-002} declare that a JSON <em>integer</em> amount is
 * <b>minor units</b> ("1099 for USD $10.99"), but {@link MoneyDeserializer} mapped {@code 1099} to
 * {@code BigDecimal.valueOf(1099)} with no scaling and {@code PaymentService#scale} then produced
 * {@code 1099.00} — a MAJOR-unit value, i.e. a contract-compliant USD request for $10.99 was
 * charged $1099.00, a silent 100× overcharge. Zero-decimal currencies (KRW/JPY) coincidentally
 * agreed, which is why every KRW integration test stayed green over the defect.
 *
 * <p>Wave-3 reconciled the wire on integer minor units: {@link MoneyDeserializer} now preserves the
 * arriving SHAPE as a {@link MoneyWire}, and {@code MoneyWire.resolveMajor(currency)} converts
 * through the {@code common/Money} seam once the currency is known and validated. The assertion
 * below is the same CONTRACT-CORRECT expectation the disabled version encoded; it now passes.
 *
 * <p>Mutation lock: reverting {@code MoneyWire.resolveMajor} to return the raw minor value (or
 * swapping {@code Money.toMajorUnits} for a single-arg {@code BigDecimal.valueOf}) makes the USD leg
 * RED. The KRW leg is the scale-0 control that proves the USD leg is not passing vacuously — it must
 * stay 1000 → 1000.
 */
@Tag("PAYMENT")
@Tag("PAYMENT-MONEY-002")
class PaymentAmountMinorUnitCharacterizationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void usdIntegerMinorAmount_isChargedAtContractValue_not100xToLarge() {
        // Exercises the REAL request path: MoneyDeserializer (via @JsonDeserialize on
        // CreatePaymentRequest.amount). contracts/payment-openapi.yaml: integer 1099 for USD ==
        // minor units == $10.99.
        CreatePaymentRequest req = MAPPER.readValue(
                "{\"amount\":1099,\"currency\":\"USD\",\"orderId\":\"o\"}", CreatePaymentRequest.class);

        BigDecimal effectiveMajorCharge = req.amount().resolveMajor("USD");

        assertThat(effectiveMajorCharge)
                .as("integer 1099 for USD is MINOR units ($10.99) per contracts/payment-openapi.yaml"
                        + "#MoneyAmount + specs/payment-l0.yaml#PAYMENT-MONEY-002 — NOT $1099.00")
                .isEqualByComparingTo(new BigDecimal("10.99"));
    }

    @Test
    void krwIntegerMinorAmount_isUnchanged_scale0CurrencyControl() {
        // Scale-0 control: KRW's minor unit IS its major unit, so the same integer must survive the
        // conversion untouched. If this ever changed, the USD fix would have moved a decimal point
        // for a currency that has none.
        CreatePaymentRequest req = MAPPER.readValue(
                "{\"amount\":1000,\"currency\":\"KRW\",\"orderId\":\"o\"}", CreatePaymentRequest.class);

        assertThat(req.amount().resolveMajor("KRW"))
                .as("KRW is a 0-decimal currency: 1000 minor units == ₩1000")
                .isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void decimalStringAmount_staysMajorUnits_bothBranchesConverge() {
        // The other accepted request encoding: a decimal STRING is already major units, so it must
        // NOT be scaled again. Both branches denote the same charge — the round-trip law.
        CreatePaymentRequest stringBranch = MAPPER.readValue(
                "{\"amount\":\"10.99\",\"currency\":\"USD\",\"orderId\":\"o\"}", CreatePaymentRequest.class);
        CreatePaymentRequest integerBranch = MAPPER.readValue(
                "{\"amount\":1099,\"currency\":\"USD\",\"orderId\":\"o\"}", CreatePaymentRequest.class);

        assertThat(stringBranch.amount().resolveMajor("USD"))
                .isEqualByComparingTo(new BigDecimal("10.99"))
                .isEqualByComparingTo(integerBranch.amount().resolveMajor("USD"));
    }
}
