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
 * against {@link PaymentResponse} — a record NO endpoint returns — which made the money-quantity cell
 * VACUOUSLY "covered". The payment legs below serialize {@code PaymentBodyMapper.toBody(...)} — the
 * real wire. (The refund leg uses the LIVE {@link RefundResponse}, which {@code PaymentController#refund}
 * actually returns.)
 *
 * <p>The golden intentionally carries only the money-relevant subset of each body (currency / amount /
 * capturedAmount / balance) — id/paymentId/status/state/timestamps are a separate, unrelated
 * serialization concern this test does not claim to pin (same scoping rationale as
 * {@code PageEnvelopeContractParityTest}'s String-only {@code CatalogItem}).
 *
 * <h2>P1-68 CLOSED — the wire is now integer MINOR units on BOTH sides</h2>
 * {@code Payment.amount}/{@code capturedAmount}/{@code balance} and {@code Refund.amount} remain
 * MAJOR-unit {@link BigDecimal} internally (the {@code common.Money} "Payment / PG-edge layer"
 * convention), but {@link PaymentBodyMapper#minorOrNull} and {@link RefundResponse#from} convert them
 * to integer MINOR units at the wire boundary through the {@code common/Money} seam. That is the
 * integer branch of {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount} {@code oneOf}, the
 * same encoding {@link MoneyDeserializer} accepts on requests, and exactly what
 * {@code templates/L0/fork-receiver-kit/money.ts}'s {@code parseMinor()} expects. Previously USD
 * emitted {@code "amount":10.99} — a JSON float token that is NEITHER allowed branch, and the exact
 * shape the contract's own description says must be REJECTED as input.
 *
 * <p>Mutation locks in this class: neutering {@code Money.toMinorUnits} in {@code minorOrNull} (e.g.
 * returning the major value's {@code longValue()}) flips {@link
 * #paymentBody_usd_serializesAsIntegerMinorUnits()} and {@link
 * #paymentBody_usdTrailingZero_emitsMinorUnitsNotStrippedDecimal()} RED, while the KRW legs (scale 0,
 * where minor == major) stay green — proving the USD legs observe the conversion itself and not merely
 * the field name.
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

    /**
     * P1-68 closure leg: a stored MAJOR-unit USD {@code 10.99} crosses the wire as the integer
     * {@code 1099}. RED-on-revert for {@link PaymentBodyMapper#minorOrNull}'s use of
     * {@code Money.toMinorUnits} — the pre-fix emitter produced {@code 10.99}.
     */
    @Test
    void paymentBody_usd_serializesAsIntegerMinorUnits() throws IOException {
        String json = realWire("USD", "10.99", "10.99", "10.99");

        assertThat(json).contains("\"amount\":1099");
        assertThat(json).contains("\"capturedAmount\":1099");
        assertThat(json).contains("\"balance\":1099");
        assertThat(json).doesNotContain("\"amount\":10.99");

        JsonNode golden = goldenTree().get("paymentUsd");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("1099");
    }

    /**
     * Trailing-zero boundary. Before P1-68 this leg pinned {@code PaymentBodyMapper.canonicalize}
     * (which stripped {@code 10.90} → {@code 10.9}); the minor-unit conversion SUBSUMES that job —
     * {@code 10.90} USD is {@code 1090} minor units, with no trailing-zero question to answer. Also a
     * second mutation lock on {@code toMinorUnits}: a neutered conversion would emit {@code 10} (or
     * {@code 10.9}), never {@code 1090}.
     */
    @Test
    void paymentBody_usdTrailingZero_emitsMinorUnitsNotStrippedDecimal() throws IOException {
        String json = realWire("USD", "10.90", "10.90", "10.90");

        assertThat(json).contains("\"amount\":1090");
        assertThat(json).doesNotContain("\"amount\":10.9");
        assertThat(json).doesNotContain("\"amount\":10.90");

        JsonNode golden = goldenTree().get("paymentUsdTrailingZero");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("1090");
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
        // RefundResponse is the LIVE refund wire body (PaymentController#refund returns it). Its
        // amount is now a long carrying MINOR units — $4.99 → 499 (P1-68).
        Refund refund = new Refund();
        refund.setId(FIXED_ID);
        refund.setPaymentId(FIXED_ID);
        refund.setAmount(new BigDecimal("4.99"));
        refund.setCurrency("USD");
        refund.setState(RefundState.COMPLETED);
        refund.setCreatedAt(FIXED_INSTANT);
        String json = MAPPER.writeValueAsString(RefundResponse.from(refund));

        assertThat(json).contains("\"amount\":499");
        assertThat(json).doesNotContain("\"amount\":-499");
        assertThat(json).doesNotContain("\"amount\":4.99");

        JsonNode golden = goldenTree().get("refundUsd");
        assertThat(golden.get("currency").asText()).isEqualTo("USD");
        assertThat(golden.get("amount").asText()).isEqualTo("499");
    }

    /**
     * The schema-conformance assertion, FLIPPED by P1-68 (the pre-fix version asserted
     * {@code isFalse()} and documented the violation as a standing gap). The endpoint's USD wire body
     * is now the integer branch of {@code contracts/payment-openapi.yaml}'s {@code MoneyAmount}
     * {@code oneOf} — no longer the {@code VALUE_NUMBER_FLOAT} shape the contract's own description
     * says must be REJECTED.
     */
    @Test
    void usdAmount_conformsToOpenApiMoneyAmountIntegerBranch() {
        JsonNode node = MAPPER.readTree(realWire("USD", "10.99", "10.99", "10.99"));
        JsonNode amount = node.get("amount");

        assertThat(amount.isIntegralNumber())
                .as("PaymentBodyMapper.toBody's USD amount must be the INTEGER minor-unit branch of "
                        + "MoneyAmount's oneOf in contracts/payment-openapi.yaml — not a JSON number "
                        + "carrying a decimal point (the shape the contract rejects as input).")
                .isTrue();
        assertThat(amount.asLong()).isEqualTo(1099L);
    }
}
