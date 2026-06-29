/**
 * Payment domain of a commerce platform: authorize / capture / void / refund a payment
 * against an injected provider, with a strict state machine, money conservation, idempotent
 * retries, redirect-style callbacks, and split-tender coverage — the order is paid only when
 * its tenders fully cover the amount due.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>State machine (the lifecycle keystone).</b> A payment moves only along a fixed
 *       legal-transition graph; an illegal transition is rejected BEFORE any state mutation
 *       reaches the database, so a payment can never reach a state its history does not permit.
 *       Terminal states are sinks.</li>
 *   <li><b>Split-tender coverage.</b> An order's coverage is the sum of EXACTLY the
 *       successfully-authorized tenders (AUTHORIZED + CAPTURED) — a pending / declined / voided /
 *       refunded tender structurally cannot inflate coverage. Capture is refused (unprocessable)
 *       while the tenders under-fund the amount due; only a fully-covered order proceeds.</li>
 *   <li><b>Money is integer minor units + ISO-4217.</b> Amounts never use binary float; a
 *       currency mismatch is rejected, not silently coerced.</li>
 *   <li><b>Idempotent retries.</b> A replayed request keyed by an idempotency key produces one
 *       effect, never a double charge.</li>
 *   <li><b>Append-only event ledger.</b> Every transition records an immutable
 *       ({@code updatable=false}) event, so the coverage sum and the audit trail are derived from
 *       records that cannot be mutated post-hoc to game the result.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link Payment} owns its append-only {@link PaymentEvent} ledger
 *       and its {@link Refund} children; tender amount/type are immutable once recorded.</li>
 *   <li><b>Pure transition function</b> — {@link PaymentStateMachine} enforces the legal-transition
 *       graph with no DB side effects; an illegal move throws
 *       {@link IllegalStateTransitionException} before persistence.</li>
 *   <li><b>Sole-mutator services</b> — {@link PaymentService} (and {@link RefundService}) are the
 *       only writers of the payment aggregate and its ledgers; coverage under-funding surfaces as
 *       {@link TendersUnderfundedException}.</li>
 *   <li><b>Provider seam</b> — {@link PaymentProvider} is the injected provider SPI; the domain
 *       depends on the interface, never on a concrete provider, so resilience (timeout / decline /
 *       malformed-response handling) is portable.</li>
 *   <li><b>Controller surface</b> — {@link PaymentController}, {@link PaymentAdminController}, and
 *       {@link PaymentCallbackController} (thin HTTP); {@link PaymentExceptionHandler} renders
 *       Problem Details.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testPayment} (spec {@code payment-l0}, covering the split-tender,
 * state-machine, money-conservation, idempotency, refund, provider-resilience, reconciliation,
 * callback, authorization and observability families). The package ships
 * {@code PaymentSplitTenderViolationProofTest} asserting the coverage sum counts exactly the two
 * authorized states and the tender ledger is append-only, structurally.
 *
 * <h2>External grounding</h2>
 * Amounts follow <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO 4217</a> minor
 * units (Fowler's Money pattern — never binary float); the error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details; an illegal state
 * transition is the lifecycle-violation case mapped to HTTP 409. The card-data posture targets
 * PCI-DSS SAQ-A (no PAN handled in this tier — a provider tokenizes upstream).
 */
package com.ax.template.authblueprint.payment;
