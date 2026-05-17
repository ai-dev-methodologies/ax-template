package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Black-box and unit-level compliance tests for PAYMENT-MONEY-001..003.
 *
 * RED phase:
 * - MONEY-001 fails with ClassNotFoundException (Payment class absent) or
 *   NoSuchFieldException (amount field absent).
 * - MONEY-002/003 fail with HTTP 404 (endpoints absent) or assertion failures.
 * Both are valid RED per /tdd-workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentMoneyTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ─── PAYMENT-MONEY-001 ────────────────────────────────────────────────────

    /**
     * PAYMENT-MONEY-001: The Payment entity's amount field must be typed
     * as java.math.BigDecimal — never float or double.
     *
     * Verification: reflection on the Payment class's 'amount' field.
     * If Payment.class does not exist yet this test fails at the
     * Class.forName call — valid RED.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-001")
    void bigDecimalAmount_javaTypeIsBigDecimalNotDouble() throws Exception {
        // RED: Class.forName will throw ClassNotFoundException if Payment doesn't exist.
        Class<?> paymentClass = Class.forName(
            "com.ax.template.authblueprint.payment.Payment"
        );

        Field amountField = paymentClass.getDeclaredField("amount");
        Class<?> fieldType = amountField.getType();

        assertThat(fieldType)
            .as("Payment.amount must be BigDecimal, not float/double")
            .isEqualTo(BigDecimal.class);

        // Also verify no float/double field named 'amount' slips in under a different name
        for (Field f : paymentClass.getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("amount")) {
                assertThat(f.getType())
                    .as("Field '%s' in Payment must not be a floating-point type", f.getName())
                    .isNotIn(float.class, double.class, Float.class, Double.class);
            }
        }
    }

    // ─── PAYMENT-MONEY-002 (positive — integer minor units) ──────────────────

    /**
     * PAYMENT-MONEY-002 (positive): POST /api/payments with amount as JSON integer
     * (minor units: 10000 KRW = ₩10,000) must be accepted with 201.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-002")
    void jsonFormat_integerMinorUnits_accepted() {
        String authToken = obtainToken("money002a@test.test", "MEMBER");

        int status =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                // amount as JSON integer (minor units)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-money002a\"}")
            .when().post("/api/payments")
            .then().extract().statusCode();

        assertThat(status)
            .as("Integer minor-unit amount must be accepted with 201")
            .isEqualTo(201);
    }

    // ─── PAYMENT-MONEY-002 (positive — decimal string) ───────────────────────

    /**
     * PAYMENT-MONEY-002 (positive): POST /api/payments with amount as explicit
     * decimal string ("100.00" USD) must be accepted with 201.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-002")
    void jsonFormat_decimalString_accepted() {
        String authToken = obtainToken("money002b@test.test", "MEMBER");

        int status =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                // amount as explicit decimal string
                .body("{\"amount\":\"100.00\",\"currency\":\"USD\",\"orderId\":\"order-money002b\"}")
            .when().post("/api/payments")
            .then().extract().statusCode();

        assertThat(status)
            .as("Decimal string amount must be accepted with 201")
            .isEqualTo(201);
    }

    // ─── PAYMENT-MONEY-002 (negative — JSON float) ───────────────────────────

    /**
     * PAYMENT-MONEY-002 (negative): POST /api/payments with amount as a JSON
     * float token (100.00 — without quotes) must be rejected with 400 and an
     * RFC 7807 ProblemDetail explaining that float JSON numbers are not accepted.
     *
     * Rationale: JSON float token loses precision for many decimal fractions;
     * only integer minor-units or explicit decimal strings are safe.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-002")
    void jsonFormat_floatJson_rejectedWith400() {
        String authToken = obtainToken("money002c@test.test", "MEMBER");

        Response response =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                // amount as JSON float token — NOT a string, NOT an integer
                .body("{\"amount\":100.00,\"currency\":\"USD\",\"orderId\":\"order-money002c\"}")
            .when().post("/api/payments");

        assertThat(response.statusCode())
            .as("JSON float amount must be rejected with 400")
            .isEqualTo(400);

        // RFC 7807 ProblemDetail: detail field must explain the rejection reason
        String body = response.body().asString();
        assertThat(body)
            .as("400 body must contain RFC 7807 detail explaining float rejection")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("float"),
                b -> assertThat(b).containsIgnoringCase("decimal string"),
                b -> assertThat(b).containsIgnoringCase("minor unit"),
                b -> assertThat(b).containsIgnoringCase("integer")
            );
    }

    // ─── PAYMENT-MONEY-003 (negative — KRW with fractional) ──────────────────

    /**
     * PAYMENT-MONEY-003 (negative): KRW has ISO 4217 minor-unit scale = 0;
     * a fractional amount ("100.50") for KRW must be rejected with 400.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-003")
    void iso4217_krwHasScale0_acceptsIntegerOnly() {
        String authToken = obtainToken("money003a@test.test", "MEMBER");

        // KRW with fractional decimal string → 400 (scale mismatch)
        int status =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":\"100.50\",\"currency\":\"KRW\",\"orderId\":\"order-money003a\"}")
            .when().post("/api/payments")
            .then().extract().statusCode();

        assertThat(status)
            .as("KRW (scale=0) with fractional amount must return 400")
            .isEqualTo(400);
    }

    // ─── PAYMENT-MONEY-003 (positive — USD scale 2) ──────────────────────────

    /**
     * PAYMENT-MONEY-003 (positive): USD has ISO 4217 minor-unit scale = 2;
     * an amount "100.50" for USD (2 decimal places) must be accepted with 201.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-MONEY-003")
    void iso4217_usdHasScale2_accepted() {
        String authToken = obtainToken("money003b@test.test", "MEMBER");

        int status =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":\"100.50\",\"currency\":\"USD\",\"orderId\":\"order-money003b\"}")
            .when().post("/api/payments")
            .then().extract().statusCode();

        assertThat(status)
            .as("USD (scale=2) with 2-decimal amount must be accepted with 201")
            .isEqualTo(201);
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
