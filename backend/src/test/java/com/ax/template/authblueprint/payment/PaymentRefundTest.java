package com.ax.template.authblueprint.payment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Black-box compliance tests for PAYMENT-REFUND-001..003.
 *
 * RED phase: all tests fail because PaymentController / RefundService /
 * refund window enforcement do not exist yet. Failures manifest as HTTP 404
 * (endpoint missing) or assertion failures — both are valid RED per /tdd-workflow.
 *
 * Spec: specs/payment-l0.yaml REFUND family.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentRefundTest {

    @LocalServerPort
    int port;

    @Autowired
    RefundRepository refundRepository;

    @Autowired
    PaymentEventRepository eventRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    RefundService refundService;

    @Autowired
    MeterRegistry meterRegistry;

    // ─── PAYMENT-REFUND-001 (positive: within 30-day window) ─────────────────

    /**
     * PAYMENT-REFUND-001 (positive): Refund within the 30-day window is accepted.
     *
     * Creates a payment, captures it, then refunds it within the allowed
     * window (simulated by a fresh capture) → 201.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-001")
    void refundWindow_within30days_allowed() {
        String token = obtainToken("refund001a@test.test", "MEMBER");

        // Create and capture a payment (capturedAt = now)
        String paymentId = createCapturedPayment(token, "order-refund001a", 10000);

        // Refund immediately — within the 30-day window
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Refund within 30-day window must return 201")
            .isEqualTo(201);
    }

    // ─── PAYMENT-REFUND-001 (negative: after 30-day window) ──────────────────

    /**
     * PAYMENT-REFUND-001 (negative): Refund outside the 30-day window is rejected.
     *
     * Creates a payment with a back-dated capturedAt (31 days ago) and attempts
     * to refund it → 409 with RFC 7807 ProblemDetail containing "refund window".
     *
     * Implementation will need to accept an override capturedAt in the test
     * context (e.g., via a test-only endpoint or time-injection mechanism).
     * In RED state this test fails with 404 (endpoint missing).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-001")
    void refundWindow_after30days_returns409() {
        String token = obtainToken("refund001b@test.test", "MEMBER");

        // Create a payment with capturedAt = now - 31 days via test-override header
        // or a dedicated test fixture endpoint that back-dates the capturedAt.
        // In RED phase, POST /api/payments returns 404 — valid RED failure.
        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                // X-Test-CapturedAt: ISO-8601 back-date used by test fixtures only
                .header("X-Test-CapturedAt", java.time.Instant.now()
                    .minus(31, java.time.temporal.ChronoUnit.DAYS)
                    .toString())
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-refund001b\"}")
            .when().post("/api/payments");

        assertThat(createResponse.statusCode())
            .as("Create payment with back-dated capturedAt must return 201")
            .isEqualTo(201);

        String paymentId = createResponse.then().extract().path("id");

        // Attempt refund outside the 30-day window → 409
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Refund outside 30-day window must return 409")
            .isEqualTo(409);

        // RFC 7807: detail must mention refund window expiry
        String body = refundResponse.body().asString();
        assertThat(body)
            .as("409 body must reference refund window in RFC 7807 detail")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("window"),
                b -> assertThat(b).containsIgnoringCase("expired"),
                b -> assertThat(b).containsIgnoringCase("refund")
            );
    }

    // ─── PAYMENT-REFUND-002 (positive: partial refund within captured amount) ─

    /**
     * PAYMENT-REFUND-002 (positive): Partial refund where sum(refunds) ≤ capturedAmount.
     *
     * Payment for 10000 KRW. Partial refund 3000 → 201.
     * Remaining balance must be 7000.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-002")
    void refundPartial_sumWithinPaymentAmount_allowed() {
        String token = obtainToken("refund002a@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund002a", 10000);

        // Partial refund: 3000 of 10000 — sum ≤ capturedAmount
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":3000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Partial refund (3000 of 10000) must return 201")
            .isEqualTo(201);

        // Verify remaining balance = 7000
        Response paymentState =
            given()
                .header("Authorization", "Bearer " + token)
            .when().get("/api/payments/" + paymentId);

        // balance or remainingAmount should be 7000
        String balanceStr = paymentState.then().extract().path("balance") != null
            ? paymentState.then().extract().path("balance").toString()
            : paymentState.then().extract().path("remainingAmount") != null
                ? paymentState.then().extract().path("remainingAmount").toString()
                : null;

        assertThat(balanceStr)
            .as("Remaining balance after 3000 partial refund on 10000 payment must be 7000")
            .isNotNull()
            .isEqualTo("7000");
    }

    // ─── PAYMENT-REFUND-002 (negative: sum exceeds captured amount) ───────────

    /**
     * PAYMENT-REFUND-002 (negative): Partial refund where sum(refunds) > capturedAmount
     * returns 400 with RFC 7807 ProblemDetail.
     *
     * Payment 10000 KRW. Refund 3000 → 201.
     * Second refund 8000 (total 11000 > 10000) → 400.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-002")
    void refundPartial_sumExceedsPaymentAmount_returns400() {
        String token = obtainToken("refund002b@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund002b", 10000);

        // First partial refund: 3000 → accepted
        int firstRefundStatus =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":3000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(firstRefundStatus)
            .as("First partial refund (3000) must return 201")
            .isEqualTo(201);

        // Second partial refund: 8000 (total 11000 > 10000) → must be rejected
        Response overRefundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":8000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(overRefundResponse.statusCode())
            .as("Over-refund (sum 11000 > captured 10000) must return 400")
            .isEqualTo(400);

        // RFC 7807: detail must reference amount violation
        String body = overRefundResponse.body().asString();
        assertThat(body)
            .as("400 body must contain RFC 7807 detail about sum exceeding captured amount")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("sum"),
                b -> assertThat(b).containsIgnoringCase("exceeds"),
                b -> assertThat(b).containsIgnoringCase("amount"),
                b -> assertThat(b).containsIgnoringCase("captured")
            );
    }

    // ─── PAYMENT-REFUND-003: refund-of-refund denied with 409 ────────────────

    /**
     * PAYMENT-REFUND-003: A refund-of-refund (refunding an already-REFUNDED
     * payment) is denied with 409 and RFC 7807 ProblemDetail.
     *
     * Flow: create → capture → full refund (state REFUNDED) →
     *       attempt second refund → 409.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-003")
    void refundOfRefund_denied_returns409() {
        String token = obtainToken("refund003@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund003", 10000);

        // Full refund → payment state becomes REFUNDED
        int fullRefundStatus =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(fullRefundStatus)
            .as("Full refund must return 201")
            .isEqualTo(201);

        // Attempt a second refund on the REFUNDED payment → must be 409
        Response secondRefundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(secondRefundResponse.statusCode())
            .as("Refund-of-refund on REFUNDED payment must return 409")
            .isEqualTo(409);

        // RFC 7807: body must reference the refund-of-refund problem type
        String body = secondRefundResponse.body().asString();
        assertThat(body)
            .as("409 body must contain RFC 7807 ProblemDetail for refund-of-refund")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("refund"),
                b -> assertThat(b).containsIgnoringCase("REFUNDED"),
                b -> assertThat(b).containsIgnoringCase("already"),
                b -> assertThat(b).containsIgnoringCase("transition")
            );
    }

    // ─── PAYMENT-IDEMP-004: refund replay (P1-70) ────────────────────────────

    /**
     * PAYMENT-IDEMP-004 (i) — PARTIAL refund retry.
     *
     * <p>A retried POST with the same Idempotency-Key must REPLAY the original refund (200 OK, same
     * refund id) rather than create a second one. Before P1-70 the key was merely stored on the row
     * and never consulted, so the retry was processed as a brand-new refund: the payment was refunded
     * twice (balance 10000 → 7000 → 4000) — a real double-refund on any client/proxy retry.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-004")
    void refundReplay_samePartialKey_returns200WithOriginalRefund_andNoSecondRow() {
        String token = obtainToken("refundidem001@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refundidem001", 10000);
        String key = UUID.randomUUID().toString();

        Response first = refund(token, paymentId, key, "{\"amount\":3000}");
        assertThat(first.statusCode()).as("first partial refund creates a row").isEqualTo(201);
        String firstRefundId = first.path("id");

        Response retry = refund(token, paymentId, key, "{\"amount\":3000}");

        assertThat(retry.statusCode())
            .as("a retry with the SAME Idempotency-Key replays: 200 OK, nothing created")
            .isEqualTo(200);
        assertThat(retry.path("id").toString())
            .as("the replay returns the ORIGINAL refund row")
            .isEqualTo(firstRefundId);

        assertThat(balanceOf(token, paymentId))
            .as("the replay must NOT refund a second time — balance stays 10000-3000")
            .isEqualTo(7000L);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("exactly one refund row exists for the payment")
            .hasSize(1);
    }

    /**
     * PAYMENT-IDEMP-004 (ii) — FULL refund retry. This is the leg that dictates WHERE the replay
     * lookup lives: a full refund leaves the payment REFUNDED, so a lookup placed after the state
     * guards would answer the retry with 409 {@code refund-of-refund} instead of replaying.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-004")
    void refundReplay_sameFullRefundKey_returns200NotRefundOfRefund409() {
        String token = obtainToken("refundidem002@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refundidem002", 10000);
        String key = UUID.randomUUID().toString();

        Response first = refund(token, paymentId, key, "{\"amount\":10000}");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstRefundId = first.path("id");

        Response retry = refund(token, paymentId, key, "{\"amount\":10000}");

        assertThat(retry.statusCode())
            .as("a retried FULL refund replays (200), it does NOT hit the refund-of-refund 409")
            .isEqualTo(200);
        assertThat(retry.path("id").toString()).isEqualTo(firstRefundId);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId))).hasSize(1);
    }

    /**
     * PAYMENT-IDEMP-004 (iii) — non-vacuity: the replay is keyed on the Idempotency-Key, so a
     * genuinely NEW key still creates a second refund and the sum invariant still governs. Without
     * this leg, "always replay" would pass (i) and (ii).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-004")
    void refundWithDifferentKey_stillCreatesASecondRefund_sumInvariantHolds() {
        String token = obtainToken("refundidem003@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refundidem003", 10000);

        assertThat(refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":3000}").statusCode())
            .isEqualTo(201);
        Response second =
            refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":2000}");

        assertThat(second.statusCode())
            .as("a DIFFERENT Idempotency-Key is a new refund, not a replay")
            .isEqualTo(201);
        assertThat(balanceOf(token, paymentId))
            .as("both refunds applied: 10000 - 3000 - 2000")
            .isEqualTo(5000L);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId))).hasSize(2);
    }

    /**
     * PAYMENT-IDEMP-004 (iv) — DB backstop non-vacuity (ViolationProof style).
     *
     * <p>The service-level lookup is the primary mechanism; the
     * {@code ux_refunds_payment_id_idempotency_key} unique constraint (Refund.java @Table +
     * db/migration/V116__refund_idempotency_unique.sql) is the backstop for two concurrent requests
     * that both miss it. This test breaks the invariant deliberately — bypassing the service — and
     * proves the DATABASE refuses the duplicate. Deleting the constraint makes it RED.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-004")
    void duplicateRefundKeyForSamePayment_isRejectedByTheDatabase() {
        UUID paymentId = UUID.randomUUID();
        String key = "dup-" + UUID.randomUUID();

        refundRepository.saveAndFlush(refundRow(paymentId, key));

        assertThatThrownBy(() -> refundRepository.saveAndFlush(refundRow(paymentId, key)))
            .as("the unique constraint on (payment_id, idempotency_key) must refuse a second row")
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Non-vacuity control for the constraint's SCOPE: it is per-payment, so the SAME key used against
     * a DIFFERENT payment is legal (clients commonly reuse one request id across resources).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-004")
    void sameRefundKeyForDifferentPayments_isAllowed() {
        String key = "shared-" + UUID.randomUUID();

        refundRepository.saveAndFlush(refundRow(UUID.randomUUID(), key));

        assertThatCode(() -> refundRepository.saveAndFlush(refundRow(UUID.randomUUID(), key)))
            .as("the constraint is (payment_id, idempotency_key) — not idempotency_key alone")
            .doesNotThrowAnyException();
    }

    // ─── PAYMENT-REFUND-004: refund amount admissibility (wave-3 codex F1) ────

    /**
     * PAYMENT-REFUND-004 (negative — money creation). A NEGATIVE refund amount must be rejected
     * with 400 before any money/state/ledger mutation.
     *
     * <p>Before this guard the wire integer branch resolved {@code -1} to {@code -0.01} USD, the sum
     * invariant ({@code newSum ≤ capturedAmount}) accepted it because it only bounds the amount from
     * ABOVE, and the service persisted a NEGATIVE refund row while raising the payment balance from
     * $10.00 to $10.01 — i.e. the refund endpoint CREATED money. USD is load-bearing here: the same
     * request in KRW is a −1 원 refund, materially smaller but the same defect class.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    void refundNegativeAmount_isRejected400_andCreatesNoMoney() {
        String token = obtainToken("refund004a@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund004a", 1000, "USD");

        Response response = refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":-1}");

        assertThat(response.statusCode())
            .as("a negative refund amount is inadmissible input → 400, not a persisted refund")
            .isEqualTo(400);
        assertThat(balanceOf(token, paymentId))
            .as("the balance must NOT increase — a negative refund would create money out of nothing")
            .isEqualTo(1000L);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("no refund row may be persisted for a rejected amount")
            .isEmpty();
        assertThat(refundedLedgerEventCount(paymentId))
            .as("no REFUNDED ledger event may be appended for a rejected amount")
            .isZero();
    }

    /**
     * PAYMENT-REFUND-004 (negative — sub-minor-unit, post-commit 500). {@code "0.001"} passes the
     * decimal-string regex but is not representable in USD minor units.
     *
     * <p>Before this guard the service persisted $0.001 and mutated state + ledger, and only THEN —
     * after the transaction had already committed — {@code RefundResponse.from} threw
     * {@link ArithmeticException} inside {@code Money.toMinorUnits}, so the client saw a 500 on top
     * of a completed money mutation. The amount must be rejected as a 400 BEFORE any mutation.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    void refundSubMinorUnitAmount_isRejected400_beforeAnyMutation() {
        String token = obtainToken("refund004b@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund004b", 1000, "USD");

        Response response = refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":\"0.001\"}");

        assertThat(response.statusCode())
            .as("an amount finer than the currency's minor unit is a 400 (inadmissible input); before the "
                + "guard the post-commit ArithmeticException surfaced as an empty-bodied 403 from the "
                + "Spring Security /error dispatch — a misleading authz signal on top of a committed mutation")
            .isEqualTo(400);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("the rejection must precede the refund insert — no row may exist")
            .isEmpty();
        assertThat(balanceOf(token, paymentId))
            .as("the rejection must precede the balance mutation")
            .isEqualTo(1000L);
        assertThat(refundedLedgerEventCount(paymentId))
            .as("the rejection must precede the ledger append")
            .isZero();
    }

    /**
     * PAYMENT-REFUND-004 (negative — zero). A zero-amount refund is a no-op state/ledger mutation
     * with a persisted 0.00 row; it is inadmissible input, mirroring create-payment's
     * {@code amount must be positive}.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    void refundZeroAmount_isRejected400() {
        String token = obtainToken("refund004c@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund004c", 1000, "USD");

        Response response = refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":0}");

        assertThat(response.statusCode())
            .as("a zero refund amount is inadmissible input → 400")
            .isEqualTo(400);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("no zero-amount refund row may be persisted")
            .isEmpty();
        assertThat(balanceOf(token, paymentId)).isEqualTo(1000L);
    }

    /**
     * PAYMENT-REFUND-004 (positive / non-vacuity control). The guard rejects only INADMISSIBLE
     * amounts: an exactly-representable positive USD amount still refunds normally. Without this leg
     * a "reject every refund" implementation would pass the three negative legs above.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    void refundExactlyRepresentableAmount_stillSucceeds() {
        String token = obtainToken("refund004d@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund004d", 1000, "USD");

        Response response = refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":\"2.50\"}");

        assertThat(response.statusCode())
            .as("$2.50 is exactly representable in USD minor units — the guard must not reject it")
            .isEqualTo(201);
        assertThat(((Number) response.path("amount")).longValue())
            .as("the response echoes integer minor units: $2.50 == 250")
            .isEqualTo(250L);
        assertThat(balanceOf(token, paymentId))
            .as("$10.00 - $2.50 = $7.50 == 750 minor units")
            .isEqualTo(750L);
    }

    /**
     * PAYMENT-REFUND-004 (unit — concurrency backstop classification, codex F3).
     *
     * <p>Two concurrent same-key refunds can both miss the replay lookup; the loser's insert is then
     * refused by {@code ux_refunds_payment_id_idempotency_key}. {@code RefundService} classifies that
     * failure as a retryable conflict (409) rather than letting it surface as a 500. This test feeds
     * the classifier the REAL {@link DataIntegrityViolationException} the database produces (not a
     * hand-written message), and asserts the negative control: an unrelated integrity error is NOT
     * swallowed.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    void duplicateRefundKeyViolation_isClassifiedAsRefundIdempotencyConflict() {
        UUID paymentId = UUID.randomUUID();
        String key = "clsfy-" + UUID.randomUUID();
        refundRepository.saveAndFlush(refundRow(paymentId, key));

        DataIntegrityViolationException actual = catchThrowableOfType(
            DataIntegrityViolationException.class,
            () -> refundRepository.saveAndFlush(refundRow(paymentId, key)));

        assertThat(RefundService.isRefundIdempotencyConflict(actual))
            .as("the real DB constraint violation must be recognised by its constraint name")
            .isTrue();
        assertThat(RefundService.isRefundIdempotencyConflict(
                new DataIntegrityViolationException("null value in column \"amount\" violates not-null constraint")))
            .as("negative control: an unrelated integrity error must NOT be classified as a refund replay conflict")
            .isFalse();
    }

    /**
     * PAYMENT-REFUND-004 (observability integrity, codex F3b). {@code refund_processed_total} must
     * count only refunds that actually COMMIT.
     *
     * <p>A Micrometer counter is not transactional, so an increment issued inline at the end of
     * {@code refund()} also counts refunds whose transaction later rolls back — the unique-index
     * loser of a concurrent same-key race, or any commit-time failure — over-reporting refunds that
     * never happened. The increment is therefore deferred to a {@code TransactionSynchronization}
     * afterCommit hook.
     *
     * <p>The test runs the service inside the test's own transaction and then rolls it back: the
     * counter must be unchanged BOTH before the rollback (proving the deferral) and after it
     * (proving the rollback does not count). With an inline increment the first assertion fails.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-004")
    @Transactional
    void refundCounter_countsOnlyCommittedRefunds() {
        double before = counterValue("refund_processed_total");
        UUID userId = UUID.randomUUID();

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId("order-refund004-counter-" + UUID.randomUUID());
        payment.setAmount(new BigDecimal("1000"));
        payment.setCurrency("KRW");
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        // Full refund (null amount) of a CREATED payment — the service implicitly captures it first.
        RefundService.RefundOutcome outcome =
            refundService.refund(payment.getId(), userId, new RefundRequest(), null);
        assertThat(outcome.replay()).isFalse();

        assertThat(counterValue("refund_processed_total"))
            .as("the increment is deferred to afterCommit — nothing may be counted while the tx is open")
            .isEqualTo(before);

        TestTransaction.flagForRollback();
        TestTransaction.end();

        assertThat(counterValue("refund_processed_total"))
            .as("the transaction rolled back, so no refund exists — the counter must not have counted it")
            .isEqualTo(before);
    }

    // ─── PAYMENT-REFUND-005: omitted amount == REMAINING balance (codex R2 F1) ───

    /**
     * PAYMENT-REFUND-005 (positive — the defect). After a PARTIAL refund, a request that OMITS
     * {@code amount} must refund the REMAINING balance and settle the payment.
     *
     * <p>The service substituted the ORIGINAL {@code capturedAmount} for a null amount, so on a
     * ₩10,000 payment already refunded ₩3,000 the sum invariant saw 3000 + 10000 = 13000 > 10000 and
     * answered 400 — the remaining ₩7,000 was unrefundable through the omitted-amount path. That path
     * is not hypothetical: it is the one the operator UI uses (the refund action in
     * frontend/apps/pay/src/features/payments/components/transactions-screen.tsx posts
     * {@code {"reason": …}} with no amount), so a partially-refunded payment could never be settled
     * from the console.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-005")
    void refundOmittedAmountAfterPartial_refundsRemainingBalance_notTheOriginalCapturedAmount() {
        String token = obtainToken("refund005a@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund005a", 10000);

        assertThat(refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":3000}").statusCode())
            .as("partial refund of 3000 on a 10000 payment")
            .isEqualTo(201);
        assertThat(balanceOf(token, paymentId)).isEqualTo(7000L);

        Response omittedFull =
            refund(token, paymentId, UUID.randomUUID().toString(), "{\"reason\":\"운영자 환불\"}");

        assertThat(omittedFull.statusCode())
            .as("an omitted amount means 'refund what is LEFT' — 7000, not a second 10000 that the "
                + "sum invariant would reject with 400")
            .isEqualTo(201);
        assertThat(((Number) omittedFull.path("amount")).longValue())
            .as("the refunded amount is exactly the remaining balance")
            .isEqualTo(7000L);
        assertThat(balanceOf(token, paymentId))
            .as("10000 - 3000 - 7000 = 0")
            .isZero();
        assertThat(stateOf(token, paymentId))
            .as("the sum now equals capturedAmount, so the payment is fully REFUNDED")
            .isEqualTo("REFUNDED");
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("exactly the two refunds (3000 + 7000) exist")
            .hasSize(2);
    }

    /**
     * PAYMENT-REFUND-005 (negative — terminal). Once the omitted-amount refund has settled the
     * payment, a further refund is the PAYMENT-REFUND-003 conflict, not a 201 or a 500.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-005")
    void refundAfterOmittedFullRefund_isRejected409_andAddsNoRow() {
        String token = obtainToken("refund005b@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund005b", 10000);

        assertThat(refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":3000}").statusCode())
            .isEqualTo(201);
        assertThat(refund(token, paymentId, UUID.randomUUID().toString(), "{\"reason\":\"운영자 환불\"}")
            .statusCode()).isEqualTo(201);

        Response further =
            refund(token, paymentId, UUID.randomUUID().toString(), "{\"reason\":\"한 번 더\"}");

        assertThat(further.statusCode())
            .as("nothing remains — a further refund is a state conflict (409), not 201 and not 500")
            .isEqualTo(409);
        assertThat(further.body().asString())
            .as("RFC 7807 type is the refund-of-refund conflict")
            .contains("urn:ax:payment:refund-of-refund");
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("the rejected request may not persist a third refund row")
            .hasSize(2);
        assertThat(refundedLedgerEventCount(paymentId))
            .as("the rejected request may not append a third REFUNDED ledger event")
            .isEqualTo(2L);
        assertThat(balanceOf(token, paymentId)).isZero();
    }

    /**
     * PAYMENT-REFUND-005 (replay). The omitted-amount shape must obey PAYMENT-IDEMP-004 exactly like
     * the explicit-amount shape: a retry with the same Idempotency-Key REPLAYS the original row
     * (200), it does not re-resolve the (now zero) remaining balance.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-005")
    void refundOmittedAmountReplay_sameKey_returns200WithOriginalRow() {
        String token = obtainToken("refund005c@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund005c", 10000);
        String key = UUID.randomUUID().toString();

        assertThat(refund(token, paymentId, UUID.randomUUID().toString(), "{\"amount\":3000}").statusCode())
            .isEqualTo(201);
        Response first = refund(token, paymentId, key, "{\"reason\":\"운영자 환불\"}");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstRefundId = first.path("id");

        Response retry = refund(token, paymentId, key, "{\"reason\":\"운영자 환불\"}");

        assertThat(retry.statusCode())
            .as("a retried omitted-amount refund replays (200) — it must not hit the 409 the fully "
                + "refunded payment would otherwise produce")
            .isEqualTo(200);
        assertThat(retry.path("id").toString())
            .as("the replay returns the ORIGINAL refund row")
            .isEqualTo(firstRefundId);
        assertThat(((Number) retry.path("amount")).longValue()).isEqualTo(7000L);
        assertThat(refundRepository.findByPaymentId(UUID.fromString(paymentId)))
            .as("the replay creates nothing")
            .hasSize(2);
        assertThat(balanceOf(token, paymentId)).isZero();
    }

    /**
     * PAYMENT-REFUND-005 (non-vacuity control). With NO prior refund the remaining balance IS the
     * captured amount, so an omitted amount still refunds the whole payment. Without this leg a
     * "always refund a partial amount" implementation would pass the three legs above.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-005")
    void refundOmittedAmountWithNoPriorRefund_stillRefundsTheWholeCapturedAmount() {
        String token = obtainToken("refund005d@test.test", "MEMBER");
        String paymentId = createCapturedPayment(token, "order-refund005d", 10000);

        Response response =
            refund(token, paymentId, UUID.randomUUID().toString(), "{\"reason\":\"운영자 환불\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(((Number) response.path("amount")).longValue())
            .as("remaining == capturedAmount when nothing was refunded yet")
            .isEqualTo(10000L);
        assertThat(balanceOf(token, paymentId)).isZero();
        assertThat(stateOf(token, paymentId)).isEqualTo("REFUNDED");
    }

    private double counterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private long refundedLedgerEventCount(String paymentId) {
        return eventRepository.findByPaymentIdOrderByOccurredAtAsc(UUID.fromString(paymentId)).stream()
            .filter(e -> e.getType() == PaymentEventType.REFUNDED)
            .count();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static Refund refundRow(UUID paymentId, String idempotencyKey) {
        Refund r = new Refund();
        r.setPaymentId(paymentId);
        r.setAmount(new BigDecimal("1000"));
        r.setCurrency("KRW");
        r.setState(RefundState.COMPLETED);
        r.setIdempotencyKey(idempotencyKey);
        return r;
    }

    private Response refund(String token, String paymentId, String idempotencyKey, String body) {
        return given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .body(body)
        .when().post("/api/payments/" + paymentId + "/refund");
    }

    private String stateOf(String token, String paymentId) {
        return given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/payments/" + paymentId)
        .then().extract().path("state");
    }

    private long balanceOf(String token, String paymentId) {
        Object balance =
            given()
                .header("Authorization", "Bearer " + token)
            .when().get("/api/payments/" + paymentId)
            .then().extract().path("balance");
        return ((Number) balance).longValue();
    }

    /**
     * Creates a payment and walks it to CAPTURED state.
     * Returns the payment ID. In RED state returns null (endpoints missing).
     */
    private String createCapturedPayment(String token, String orderId, int amount) {
        return createCapturedPayment(token, orderId, amount, "KRW");
    }

    /**
     * {@code amount} is integer MINOR units, so a USD payment of {@code 1000} is $10.00 — the
     * currency-parameterised overload the PAYMENT-REFUND-004 legs need (a 2-decimal currency is the
     * only place a sub-minor-unit or −1-minor-unit amount is observable).
     */
    private String createCapturedPayment(String token, String orderId, int amount, String currency) {
        String idempotencyKey = UUID.randomUUID().toString();

        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":" + amount + ",\"currency\":\"" + currency + "\",\"orderId\":\"" + orderId + "\"}")
            .when().post("/api/payments");

        String paymentId = createResponse.then().extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/authorize");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/capture");

        return paymentId;
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
