/**
 * Composite (bundle) pricing of a commerce platform: a bundle order-item whose
 * price is a conserving roll-up of its child components, or a declared fixed base
 * price, selected by the bundle's pricing mode.
 *
 * <h2>Correctness invariant</h2>
 * The bundle price is a <b>conserving roll-up</b> of its components:
 * <ol>
 *   <li><b>ITEM_SUM mode.</b> The bundle price is
 *       {@code sum(childPrice * childQuantity) + bundleFees} — the amounts
 *       allocated to the distinct components sum to the bundle total, with no
 *       penny invented or lost.</li>
 *   <li><b>BUNDLE (fixed) mode.</b> The bundle carries a declared fixed base
 *       price; the children are not summed.</li>
 * </ol>
 * Money is integer minor units summed exactly and rounded once, so independently
 * rounded parts cannot drift away from the total. The roll-up is the allocation
 * identity: the sum allocated to the components equals the total.
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link CompositeItem} owns its
 *       {@link CompositeComponent} members (the priced children).</li>
 *   <li><b>Value types / enum</b> — {@link BundleFee} (an additive bundle-level
 *       fee) and {@link BundlePricingModel}
 *       ({@code ITEM_SUM} / {@code BUNDLE}), the mode predicate that selects the
 *       roll-up vs the fixed base price.</li>
 *   <li><b>Sole-mutator service</b> — {@link BundlePricingService} is the only
 *       writer of the composite aggregate and the authority for the roll-up.</li>
 *   <li><b>Controller surface</b> — {@link BundlePricingController} (thin HTTP)
 *       with {@link BundlePricingDtos} request/response records;
 *       {@link BundlePricingExceptions} renders RFC 9457 Problem Details.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testBundlePricing} (spec {@code bundle-pricing-l0}, 4
 * items: ITEMSUM / FIXED / DERIVED / AUTHZ). The package ships
 * {@code BundlePricingViolationProofTest} asserting the conserving-roll-up and
 * round-once invariants are structurally enforced.
 *
 * <h2>External grounding</h2>
 * The conservation property is the transaction-price allocation identity of
 * <a href="https://www.ifrs.org/issued-standards/list-of-standards/ifrs-15-revenue-from-contracts-with-customers/">IFRS&nbsp;15</a>
 * (paragraphs 73-86, Allocating the transaction price to performance
 * obligations; mirrored by FASB ASC 606-10-32): the transaction price is
 * allocated across a contract's distinct performance obligations so the amounts
 * allocated to the components sum to the total. The round-once money discipline
 * follows Martin Fowler's
 * <a href="https://martinfowler.com/eaaCatalog/money.html">Money pattern</a>.
 */
package com.ax.template.authblueprint.bundlepricing;
