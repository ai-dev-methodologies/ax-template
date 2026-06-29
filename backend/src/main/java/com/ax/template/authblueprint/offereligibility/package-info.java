/**
 * Offer-eligibility applicability gate of a commerce platform: the
 * who-and-which-items predicate that decides, deterministically and fail-closed,
 * whether a conditional promotion (coupon / loyalty / BOGO / cart-rule) applies
 * to an order — distinct from the discount math, which is owned elsewhere.
 *
 * <h2>Correctness invariant</h2>
 * Applicability is decided by a single deterministic, side-effect-free evaluator
 * that <b>denies by default</b>:
 * <ol>
 *   <li><b>Fail-closed.</b> If the offer declares no qualifier criteria, no
 *       target criteria, or no eligibility criteria (empty customer allow-list
 *       and no matched segment), or the context carries no resolvable customer,
 *       the result is NOT-APPLIED with the corresponding reason — never applied.
 *       There is no path by which a mis-declared offer yields an applied
 *       decision.</li>
 *   <li><b>Two independent gates.</b> A qualifier-to-target minimum-quantity rule
 *       (the target line is discounted only when the qualifying lines meet a
 *       declared minimum) and a customer/segment eligibility check (an explicit
 *       allow-list or a matched segment).</li>
 *   <li><b>Recorded, pure decision.</b> The applied/not-applied decision and its
 *       reason are recorded; the same offer plus context always yields the same
 *       decision (no wall-clock, no mutation, no iteration-order dependence).</li>
 * </ol>
 * An ineligible offer can never reach the discount-application path. This is
 * applicability only; the proration/clamp/stacking math takes the
 * already-applicable offers as input (see
 * {@code com.ax.template.authblueprint.commercepromotion}).
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link EligibilityOffer} carries an
 *       {@code @ElementCollection} customer-xref allow-list, a
 *       {@code @Check(min_qualifier_qty >= 1 AND discount_basis_points >= 0)}
 *       guard, and immutable ({@code @Column(updatable=false)}) criteria with no
 *       public setter.</li>
 *   <li><b>Sole evaluator</b> — {@link OfferEligibilityService} is the single,
 *       pure {@code decide()} authority (no I/O, no clock read); the first
 *       failing gate's reason is recorded.</li>
 *   <li><b>Value type / enum</b> — {@link EligibilityReason} (the recorded
 *       not-applied / applied reason).</li>
 *   <li><b>Controller surface</b> — {@link OfferEligibilityController} (thin
 *       HTTP); {@link OfferEligibilityException} renders RFC 9457 Problem
 *       Details; {@link OfferEligibilityMetrics} emits bounded-cardinality
 *       (outcome, reason) counters.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testOfferEligibility} (spec {@code offer-eligibility-l0},
 * 4 families: FAIL-CLOSED / QUALIFIER-MINQTY / SEGMENT-ELIGIBILITY / AUTHZ). The
 * package ships {@code OfferEligibilityViolationProofTest} asserting the
 * deny-by-default and pure-evaluator invariants are structurally enforced.
 *
 * <h2>External grounding</h2>
 * Deny-by-default is anchored to
 * <a href="https://cwe.mitre.org/data/definitions/636.html">CWE-636</a> (Not
 * Failing Securely / Failing Open) and
 * <a href="https://cwe.mitre.org/data/definitions/840.html">CWE-840</a>
 * (Business Logic Errors); gating the discount as a protected resource is
 * <a href="https://cwe.mitre.org/data/definitions/285.html">CWE-285</a>
 * (Improper Authorization).
 */
package com.ax.template.authblueprint.offereligibility;
