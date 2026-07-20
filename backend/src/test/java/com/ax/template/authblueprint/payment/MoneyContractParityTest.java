package com.ax.template.authblueprint.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.MONEY-QUANTITY.XB — frontend&lt;-&gt;backend money-quantity contract parity.
 *
 * <p>Sibling of {@code common.PageEnvelopeContractParityTest} (same pattern: one committed golden,
 * two independent consumers — this Jackson test + {@code frontend/tests/money-contract-parity.vitest.ts}
 * both read {@code frontend/tests/_fixtures/money-contract.golden.json}). Plain Jackson unit test — NO
 * {@code @SpringBootTest}, zero ContextCache pressure.
 *
 * <h2>Bound to the REAL wire emitter (wave-2 finding-3 closure)</h2>
 * The payment amount that actually crosses the HTTP boundary is produced by
 * {@link PaymentBodyMapper#toBody(Payment)} (formerly {@code PaymentController.paymentBody}, a private
 * method the controller returns as a {@code Map<String,Object>}). This test previously asserted parity
 * against {@link PaymentResponse} — a record NO endpoint returns (grep: only this test + the record
 * itself reference it). That made the money-quantity cell VACUOUSLY "covered": the assertions could
 * never observe the shape the endpoint serializes, and in particular could never observe
 * {@link PaymentBodyMapper#canonicalize} (which strips trailing zeros — e.g. USD {@code 10.90} → wire
 * {@code 10.9}), because {@code PaymentResponse} does not apply it. The payment legs below now serialize
 * {@code PaymentBodyMapper.toBody(...)} — the real wire. (The refund leg already used the LIVE
 * {@link RefundResponse}, which {@code PaymentController#refund} actually returns, so it stays.)
 *
 * <p>The golden intentionally carries only the money-relevant subset of each body (currency / amount /
 * capturedAmount / balance) — id/paymentId/status/state/timestamps are a separate, unrelated
 * serialization concern this test does not claim to pin (same scoping rationale as
 * {@code PageEnvelopeContractParityTest}'s String-only {@code CatalogItem}).
 *
 * <h2>CONFIRMED FINDING (P1-68 — see docs/BACKLOG.md + session report for the full evidence chain)</h2>
 * {@code Payment.amount}/{@code capturedAmount}/{@code balance} are MAJOR-unit {@link BigDecimal}
 * (per {@code common.Money}'s own documented "Payment / PG-edge layer" convention: BigDecimal in MAJOR
 * units), scaled to the currency's ISO-4217 minor-unit count by {@code PaymentService#scale}. For KRW
 * (scale 0) the major-unit number happens to equal the minor-unit count too, so
 * {@code templates/L0/fork-receiver-kit/money.ts}'s {@code parseMinor()} (which assumes the wire is
 * ALREADY integer minor units) accidentally "works". For USD (scale 2) it does not: the wire carries a
 * decimal point (e.g. {@code "amount":10.99}), which is neither of the two shapes
 * {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount} {@code oneOf} allows (integer OR
 * decimal-string) — it is the exact {@code VALUE_NUMBER_FLOAT} shape the contract's own description
 * says must be REJECTED with 400 if received as *input* ({@link MoneyDeserializer}).
 * {@link #usdAmount_violatesItsOwnOpenApiMoneyAmountSchema_asDocumented()} locks this as a standing,
 * currently-true assertion on the real wire; flip to green-by-default only if the contract or the
 * serialization changes. (The request-path mirror image — an integer-minor {@code 1099} charged 100×
 * as {@code 1099.00} major — is characterized separately by
 * {@code PaymentAmountMinorUnitCharacterizationTest}, tracked as the wave-3 P1 candidate.)
 */
@Tag("PAYMENT")
@Tag("MONEY-QUANTITY-XB")
class MoneyContractParityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-20T00:00:00Z");

    private static JsonNode goldenTree() throws IOException {
        Path golden = Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "money-contract.golden.json");
        return MAPPER.readTree(Files.readString(golden));
    }

    /**
     * The REAL wire body the endpoint emits: {@link PaymentBodyMapper#toBody(Payment)} on a Payment
     * whose amount/capturedAmount/balance are already scaled to the currency's minor-unit count exactly
     * as {@code PaymentService#scale} leaves them. Serialized with the same plain {@code tools.jackson}
     * mapper the app uses.
     */
    private static String realWire(String currency, String amount, String captured, String balance) {
        Payment p = new Payment();
        p.setId(FIXED_ID);
        p.setOrderId("order-money-contract");
        p.setUserId(FIXED_ID);
        p.setAmount(new BigDecimal(amount));
        p.setCapturedAmount(new BigDecimal(captured));
        p.setBalance(new BigDecimal(balance));
        p.setCurrency(currency);
        // state defaults to CREATED (non-null) — the money subset does not assert on it.
        p.setCreatedAt(FIXED_INSTANT);
        p.setUpdatedAt(FIXED_INSTANT);
        Map<String, Object> body = PaymentBodyMapper.toBody(p);
        return MAPPER.writeValueAsString(body);
    }

    @Test
    void paymentBody_krwWholeAmount_serializesAsBareIntegerMatchingGolden() throws IOException {
        String json = realWire("KRW", "12900", "12900", "12900");

        // The real wire shape, asserted independently of the golden file. Renaming any mapper key (e.g.
        // amount→amt) makes these substrings absent → RED-on-revert on the emitter's key names.
        assertThat(json).contains("\"amount\":12900");
        assertThat(json).contains("\"capturedAmount\":12900");
        assertThat(json).contains("\"balance\":12900");

        // The golden file, loaded independently, must carry the SAME literal values — this is the
        // mutation lock: hand-edit any one of these digits in the golden and this comparison flips RED
        // while the assertions above (about the wire, unrelated to the file) keep passing.
        JsonNode golden = goldenTree().get("paymentKrw");
        assertThat(golden.get("currency").asText()).isEqualTo("KRW");
        assertThat(golden.get("amount").asText()).isEqualTo("12900");
        assertThat(golden.get("capturedAmount").asText()).isEqualTo("12900");
        assertThat(golden.get("balance").asText()).isEqualTo("12900");
    }

    @Test
    void paymentBody_usd_serializesWithDecimalPoint_notMinorUnits() throws IOException {
        String json = realWire("USD", "10.99", "10.99", "10.99");

        // This IS the P1-68 finding, locked on the REAL wire: the value is "10.99" (a MAJOR-unit
        // decimal), NOT "1099" (minor units) as contracts/payment-openapi.yaml's MoneyAmount description
        // and templates/L0/fork-receiver-kit/money.ts's parseMinor() both assume.
        assertThat(json).contains("\"amount\":10.99");
        assertThat(json).doesNotContain("\"amount\":1099");

        JsonNode golden = goldenTree().get("paymentUsd");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("10.99");
    }

    /**
     * RED-on-revert for {@link PaymentBodyMapper#canonicalize}: a scale-2 USD {@code 10.90} is stripped
     * to {@code 10.9} on the real wire. This is the ONLY leg where {@code canonicalize} is load-bearing
     * (every other golden value has no trailing zeros), so deleting/neutering canonicalize would emit
     * {@code 10.90} and flip the {@code doesNotContain} assertion RED. {@code PaymentResponse} never
     * applied canonicalize, so this exact-shape parity was unobservable before finding-3 closure.
     */
    @Test
    void paymentBody_usdTrailingZero_canonicalizesToStrippedForm() throws IOException {
        String json = realWire("USD", "10.90", "10.90", "10.90");

        assertThat(json).contains("\"amount\":10.9");
        assertThat(json).doesNotContain("\"amount\":10.90");
        assertThat(json).doesNotContain("\"amount\":10.900000");

        JsonNode golden = goldenTree().get("paymentUsdTrailingZero");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("10.9");
    }

    @Test
    void paymentBody_boundaryZeroBalance_matchesGolden() throws IOException {
        String json = realWire("KRW", "5000", "5000", "0");
        assertThat(json).contains("\"balance\":0");

        JsonNode golden = goldenTree().get("paymentKrwFullyRefunded");
        assertThat(golden.get("amount").asText()).isEqualTo("5000");
        assertThat(golden.get("balance").asText()).isEqualTo("0");
    }

    @Test
    void paymentBody_boundaryLargeAmount_matchesGolden() throws IOException {
        String json = realWire("KRW", "12345678900", "12345678900", "12345678900");
        assertThat(json).contains("\"amount\":12345678900");

        JsonNode golden = goldenTree().get("paymentKrwLarge");
        assertThat(golden.get("amount").asText()).isEqualTo("12345678900");
    }

    @Test
    void refundResponse_usd_isAlwaysPositive_matchesGolden() throws IOException {
        // RefundResponse is the LIVE refund wire body (PaymentController#refund returns it), so this leg
        // was already bound to the real emitter — unchanged by the finding-3 repoint.
        RefundResponse response = new RefundResponse(
                FIXED_ID, FIXED_ID, new BigDecimal("4.99"), "USD", "COMPLETED", FIXED_INSTANT);
        String json = MAPPER.writeValueAsString(response);

        assertThat(json).contains("\"amount\":4.99");
        assertThat(json).doesNotContain("\"amount\":-4.99");

        JsonNode golden = goldenTree().get("refundUsd");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("4.99");
    }

    /**
     * Documents (does not merely assert) the schema mismatch described in the class Javadoc, now on the
     * REAL wire: {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount} is {@code oneOf [integer,
     * decimal-string]} and explicitly REJECTS a JSON float token ("JSON `number` with a decimal point
     * ... is REJECTED with HTTP 400"). The endpoint's own wire body for any non-zero-scale currency
     * (USD) serializes {@code amount} to EXACTLY that float-token shape. If this test ever fails because
     * {@code isFalse()} should become {@code isTrue()}, that is GOOD NEWS — it means the response shape
     * was fixed to conform to its own declared contract; until then this is the locked, standing proof
     * of the gap (RED-on-fix is the intended lifecycle of this one assertion, not a fixture bug).
     */
    @Test
    void usdAmount_violatesItsOwnOpenApiMoneyAmountSchema_asDocumented() {
        JsonNode node = MAPPER.readTree(realWire("USD", "10.99", "10.99", "10.99"));
        JsonNode amount = node.get("amount");

        boolean isIntegerShaped = amount.isIntegralNumber();
        boolean isStringShaped = amount.isTextual();

        assertThat(isIntegerShaped || isStringShaped)
                .as("PaymentBodyMapper.toBody's amount for USD is neither the integer nor the "
                        + "decimal-string shape MoneyAmount's oneOf allows in contracts/payment-openapi.yaml "
                        + "— it is a raw JSON number carrying a decimal point, the VALUE_NUMBER_FLOAT shape "
                        + "the contract's own description says must be REJECTED as input. Confirmed gap; see "
                        + "MoneyDeserializer.java + PaymentService#scale().")
                .isFalse();
    }
}
