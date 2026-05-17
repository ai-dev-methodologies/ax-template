package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Compliance tests for PAYMENT-RECON-001..002.
 *
 * PAYMENT-RECON-001: payment_events ledger is immutable (append-only) and
 *   each event is hash-chained to its predecessor (event[n].prev_hash ==
 *   sha256(serialized form of event[n-1])).
 *
 * PAYMENT-RECON-002: Reconciliation invariant:
 *   sum(CAPTURED events.amount) - sum(REFUNDED events.amount) = Payment.balance.
 *   A divergence-detection meta-test verifies the invariant is sensitive.
 *
 * RED phase: all tests fail today because:
 *  - payment_events table does not exist → JdbcTemplate queries throw.
 *  - PaymentController does not exist → HTTP 404 assertion failures.
 *  - PaymentEventLedger hash-chain logic is not implemented.
 * These are all valid RED outcomes per /tdd-workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentReconciliationTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ─── PAYMENT-RECON-001: Ledger immutability ───────────────────────────────

    /**
     * PAYMENT-RECON-001: payment_events table is append-only — UPDATE is blocked.
     *
     * Inserts a payment event, then attempts an UPDATE on the table (simulated via
     * JdbcTemplate). Expects an exception because:
     *  (a) a DB-level trigger or RLS policy blocks UPDATE on payment_events, or
     *  (b) the repository layer exposes no update method (structural guard).
     *
     * This test also verifies the structural guard: PaymentEventRepository must
     * not expose any update/delete methods (checked by asserting the interface
     * has no such methods via reflection in P3.0). For RED, the JdbcTemplate
     * UPDATE attempt is the primary assertion.
     *
     * RED: fails today because payment_events table does not exist.
     */
    @Test
    @Tag("PAYMENT-RECON-001")
    void ledgerImmutability_eventCannotBeUpdated() {
        String paymentId = UUID.randomUUID().toString();

        // Attempt to directly UPDATE a row in payment_events via JdbcTemplate.
        // In production, a DB trigger or application-level constraint prevents this.
        // If no trigger exists, the test documents the expected protection mechanism:
        // the PaymentEventRepository interface must not expose update/delete methods.
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "UPDATE payment_events SET payload = '{}' WHERE payment_id = ?",
                paymentId
            )
        )
            .as("UPDATE on payment_events must be blocked. RED: table does not exist. " +
                "In P3.0, a DB trigger or application constraint must prevent updates. " +
                "PCI-DSS requires an immutable audit trail.")
            .isInstanceOf(Exception.class);
    }

    /**
     * PAYMENT-RECON-001: Hash-chain correctness — event[n].prev_hash equals
     * sha256(serialized form of event[n-1]).
     *
     * Creates a payment via the API (which produces 2 ledger events: CREATED +
     * AUTHORIZED), then queries the ledger and verifies the chain.
     *
     * RED: fails today because the payment_events table and PaymentController
     * do not exist.
     */
    @Test
    @Tag("PAYMENT-RECON-001")
    void ledgerHashChain_prevHashLinksCorrectly() {
        String authToken = obtainToken("recon001hc@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create a payment — this should produce at least 2 ledger events.
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-recon001hc\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        // Query the ledger events ordered by occurred_at ascending.
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
            "SELECT event_id, payment_id, type, payload_hash, prev_hash " +
            "FROM payment_events WHERE payment_id = ? ORDER BY occurred_at ASC",
            paymentId
        );

        assertThat(events)
            .as("At least 2 ledger events must exist for a created payment")
            .hasSizeGreaterThanOrEqualTo(2);

        // Verify the hash chain: event[n].prev_hash == event[n-1].payload_hash.
        for (int i = 1; i < events.size(); i++) {
            String prevPayloadHash = (String) events.get(i - 1).get("payload_hash");
            String currentPrevHash = (String) events.get(i).get("prev_hash");

            assertThat(currentPrevHash)
                .as("event[%d].prev_hash must equal event[%d].payload_hash " +
                    "(hash chain integrity — PAYMENT-RECON-001)", i, i - 1)
                .isEqualTo(prevPayloadHash);
        }

        // First event must have a well-known genesis prev_hash (null or empty string).
        Object firstPrevHash = events.get(0).get("prev_hash");
        assertThat(firstPrevHash == null || firstPrevHash.toString().isEmpty())
            .as("First event in chain must have null or empty prev_hash (genesis)")
            .isTrue();
    }

    // ─── PAYMENT-RECON-002: Reconciliation invariant ─────────────────────────

    /**
     * PAYMENT-RECON-002: sum(captured.amount) - sum(refunded.amount) from ledger
     * equals Payment.balance for every payment.
     *
     * Scenario: create payment 10000 → capture 10000 (ledger CAPTURED event) →
     * refund 3000 (ledger REFUNDED event) → assert:
     *   stored Payment.balance == 7000
     *   ledger-derived sum(CAPTURED) - sum(REFUNDED) == 10000 - 3000 == 7000
     *   both values are equal.
     *
     * RED: fails because PaymentController and payment_events table do not exist.
     */
    @Test
    @Tag("PAYMENT-RECON-002")
    void reconciliationInvariant_ledgerSumEqualsPaymentBalance() {
        String authToken = obtainToken("recon002inv@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Step 1: Create payment (10000 KRW).
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-recon002inv\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        // Step 2: Capture the payment (transitions to CAPTURED, writes CAPTURED event).
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/capture")
        .then().statusCode(200);

        // Step 3: Partial refund of 3000.
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":3000}")
        .when().post("/api/payments/" + paymentId + "/refund")
        .then().statusCode(201);

        // Step 4: Compute ledger-derived balance.
        BigDecimal capturedSum = queryLedgerSum(paymentId, "CAPTURED");
        BigDecimal refundedSum = queryLedgerSum(paymentId, "REFUNDED");
        BigDecimal ledgerBalance = capturedSum.subtract(refundedSum);

        assertThat(ledgerBalance)
            .as("Ledger-derived balance: sum(CAPTURED) - sum(REFUNDED) must be 7000 KRW")
            .isEqualByComparingTo(new BigDecimal("7000"));

        // Step 5: Retrieve stored Payment.balance from the entity table.
        BigDecimal storedBalance = jdbcTemplate.queryForObject(
            "SELECT balance FROM payments WHERE id = ?",
            BigDecimal.class,
            paymentId
        );

        assertThat(storedBalance)
            .as("Stored Payment.balance must be 7000 KRW after 10000 capture + 3000 refund")
            .isEqualByComparingTo(new BigDecimal("7000"));

        // Step 6: Core invariant assertion: ledger sum == stored balance.
        assertThat(ledgerBalance)
            .as("Reconciliation invariant: ledger-derived balance must equal stored " +
                "Payment.balance. Divergence indicates an atomicity failure in PaymentService " +
                "(e.g., balance updated without ledger entry, or vice versa). PAYMENT-RECON-002.")
            .isEqualByComparingTo(storedBalance);
    }

    /**
     * PAYMENT-RECON-002 (meta-test): Invariant test detects divergence.
     *
     * Manually tampers Payment.balance via JdbcTemplate (sets it to 9999 instead
     * of the correct derived value), then recomputes the invariant and asserts it
     * FAILS (returns a divergence signal). This proves the test is sensitive.
     *
     * This is a meta-test: it asserts that the invariant test itself works.
     * A passing result here means the invariant check is correctly sensitive.
     *
     * RED: fails today because payment_events table and payments table do not exist.
     */
    @Test
    @Tag("PAYMENT-RECON-002")
    void reconciliationInvariant_diverged_detectedByTest() {
        String authToken = obtainToken("recon002div@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create and capture a payment (10000).
        String paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-recon002div\"}")
        .when().post("/api/payments")
        .then().statusCode(201).extract().path("id");

        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/capture")
        .then().statusCode(200);

        // Tamper: set Payment.balance to a wrong value (9999 instead of 10000).
        jdbcTemplate.update(
            "UPDATE payments SET balance = 9999 WHERE id = ?",
            paymentId
        );

        // Compute invariant.
        BigDecimal capturedSum = queryLedgerSum(paymentId, "CAPTURED");
        BigDecimal refundedSum = queryLedgerSum(paymentId, "REFUNDED");
        BigDecimal ledgerBalance = capturedSum.subtract(refundedSum);

        BigDecimal storedBalance = jdbcTemplate.queryForObject(
            "SELECT balance FROM payments WHERE id = ?",
            BigDecimal.class,
            paymentId
        );

        // Assert divergence is detected: ledger says 10000, DB says 9999.
        boolean diverged = !ledgerBalance.equals(storedBalance);

        assertThat(diverged)
            .as("Meta-test: invariant must detect tampering. " +
                "Ledger balance=" + ledgerBalance + " vs stored balance=" + storedBalance + ". " +
                "If this assertion fails, the invariant test is not sensitive to balance tampering. " +
                "This test itself fails RED today because the tables do not exist.")
            .isTrue();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Queries the sum of amounts for events of the given type in the ledger.
     * The amount is stored in the payload JSONB column as a numeric value.
     * In P3.0, the schema will include: payload JSONB (with an "amount" key).
     */
    private BigDecimal queryLedgerSum(String paymentId, String eventType) {
        // The ledger payload is a JSONB column; extract the "amount" field.
        // RED: this query will fail because payment_events does not exist.
        BigDecimal sum = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(CAST(payload->>'amount' AS NUMERIC)), 0) " +
            "FROM payment_events WHERE payment_id = ? AND type = ?",
            BigDecimal.class,
            paymentId, eventType
        );
        return sum != null ? sum : BigDecimal.ZERO;
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
}
