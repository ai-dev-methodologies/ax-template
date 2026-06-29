/**
 * Pricing pipeline of a commerce platform: a stateless computation that owns the
 * two cross-phase correctness properties a discount engine alone never
 * establishes — the deterministic <em>order</em> of pricing phases and the
 * conserving <em>closure</em> of the order total.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Fixed phase order — discount before tax.</b> Phases run in a
 *       deterministic, code-pinned order: apply discount, finalize each item's
 *       taxable base, compute tax, accumulate total. Each item's taxable base is
 *       {@code itemAmount - proratedOrderDiscount}, so tax is charged on the
 *       <em>net</em> (post-discount) amount, never on the gross line price —
 *       gross-tax over-charges the customer. The method-body order is the
 *       determinism source, so the same cart resolves identically across runs
 *       and nodes.</li>
 *   <li><b>Conserving total closure.</b> The order total is
 *       {@code subTotal - orderAdjustments + shipping + tax + fees}, accumulated
 *       at a single point with no penny invented or dropped; the displayed total
 *       equals the sum of its disclosed parts exactly (an auditable receipt), and
 *       the per-fulfillment-group total reconciles to the same components. This
 *       conserves <em>across</em> the order-level phases — distinct from
 *       per-offer proration, which conserves within one discount.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Stateless computation (no persistent entity)</b> —
 *       {@link PricingPipeline} is the pricing logic; its nested records
 *       ({@code PricingPipeline.Line}, {@code PricingPipeline.PricedLine},
 *       {@code PricingPipeline.PricedOrder}) are the immutable value objects of
 *       the computation. Pricing is a function over supplied line items plus an
 *       injected tax rate, not stored state.</li>
 *   <li><b>Money discipline</b> — integer minor units with
 *       {@link java.lang.Math#multiplyExact(long, long)} (overflow fails closed)
 *       and round-once apportionment.</li>
 *   <li><b>Controller surface</b> — {@link PricingController} (thin HTTP);
 *       {@link PricingException} renders RFC 9457 Problem Details;
 *       {@link PricingMetrics} for observability.</li>
 * </ul>
 * The discount math, deterministic offer order, and atomic caps live in
 * {@code com.ax.template.authblueprint.commercepromotion}; SKU price-presence in
 * {@code com.ax.template.authblueprint.commercecatalog}; the tax rate is injected
 * (jurisdiction tables are out of scope). This pipeline owns only the cross-phase
 * ordering and closure.
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testCommercePricing} (spec {@code pricing-l0}, 3 items:
 * PRICING-ORDER-001/002 and PRICING-TOTAL-001). The package ships
 * {@code CommercePricingViolationProofTest} asserting the discount-before-tax
 * ordering and the conserving total closure.
 *
 * <h2>External grounding</h2>
 * The penny-conservation rounding discipline follows Martin Fowler's
 * <a href="https://martinfowler.com/eaaCatalog/money.html">Money pattern</a>
 * (integer minor units, round once — independently-rounded parts lose pennies);
 * the error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.commercepricing;
