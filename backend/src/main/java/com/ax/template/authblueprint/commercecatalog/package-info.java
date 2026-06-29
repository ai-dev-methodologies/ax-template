/**
 * Purchasable-catalog core of a commerce platform: a product with exactly one
 * default sellable variant plus option-derived variant SKUs, a deterministic
 * option-value-set to SKU resolution, an ordered category graph, an
 * active-date/archival purchasability lifecycle, and price/inventory hooks
 * (presence, not computation).
 *
 * <h2>Correctness invariant</h2>
 * Two properties are made structurally unrepresentable-to-violate:
 * <ol>
 *   <li><b>Deterministic, unique variant resolution.</b> A given set of chosen
 *       option values resolves to <em>exactly one</em> active SKU by an exact
 *       match — never by an arbitrary "pick the first candidate" tie-break. A
 *       duplicate variant-generating signature is forbidden at the storage layer
 *       ({@code UNIQUE(product_id, option_signature)}), so an ambiguous
 *       resolution cannot arise.</li>
 *   <li><b>Price-presence gating.</b> A sellable SKU must carry a price; a
 *       priceless-but-purchasable SKU is rejected at the catalog boundary. A
 *       {@code @Check sale <= retail} constraint backstops the price-window
 *       well-formedness.</li>
 * </ol>
 * Purchasability is additionally gated by an active-date window
 * {@code now in [start, end)} (evaluated against an injected
 * {@link java.time.Clock}) and the tri-state {@link InventoryType} policy
 * ({@code CHECK_QUANTITY} / {@code ALWAYS_AVAILABLE} / {@code UNAVAILABLE}),
 * which hard-blocks before any quantity arithmetic.
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate roots</b> — {@link CatalogProduct} (owns its default and
 *       variant {@link Sku}s, {@link ProductOption}, {@link ProductOptionValue},
 *       {@link SkuOptionValueXref}) and {@link Category} (owns ordered
 *       {@link CategoryProductXref} membership). Identity columns are immutable
 *       ({@code @Column(updatable=false)}); variant mutations take a
 *       {@code PESSIMISTIC_WRITE} lock on the product row over {@code @Version}
 *       optimistic concurrency.</li>
 *   <li><b>Sole-mutator services</b> — {@link CatalogProductService} and
 *       {@link CategoryService} are the only writers of their respective roots.</li>
 *   <li><b>Value type / enum</b> — {@link InventoryType} (the availability-mode
 *       policy enum).</li>
 *   <li><b>Controller surface</b> — {@link CatalogController} (thin HTTP layer);
 *       {@link CatalogException} maps domain faults to RFC 9457 Problem Details;
 *       {@link CatalogMetrics} emits bounded-cardinality Micrometer counters.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testCommerceCatalog} (spec {@code catalog-commerce-l0},
 * 13 items across PRODUCT / SKU / VARIANT / CATEGORY / LIFECYCLE / PRICING-HOOK /
 * INVENTORY-GATE families). The package ships
 * {@code CommerceCatalogViolationProofTest}, which asserts by construction that
 * the uniqueness, price-presence, and immutable-identity invariants cannot be
 * violated.
 *
 * <h2>External grounding</h2>
 * The product/offer vocabulary and availability-window semantics mirror the
 * <a href="https://schema.org/Offer">schema.org Offer</a> standard (an offer
 * carries a price and an availability status bounded by
 * {@code availabilityStarts}/{@code availabilityEnds}). The error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.commercecatalog;
