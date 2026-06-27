# Broadleaf-absorption parity — bundle-pricing (conserving roll-up of children) [G008]

- vertical: bundle-pricing
- broadleaf_source: core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/order/domain/BundleOrderItemImpl.java:260-277 (getRetailPrice ITEM_SUM roll-up), :279-306 (getSalePrice with per-child retail fallback), :222-244 (getTaxablePrice), :251-257 (shouldSumItems ⇒ ProductBundlePricingModelType.ITEM_SUM); core/.../catalog/service/type/ProductBundlePricingModelType.java:36-37 (ITEM_SUM / BUNDLE modes)
- spec_items: BUNDLE-ITEMSUM-001, BUNDLE-FIXED-001, BUNDLE-DERIVED-001, BUNDLE-AUTHZ-001
- rule: practices/rules/composite-bundle-price-is-conserving-rollup-of-children.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/bundlepricing/BundlePricingComplianceTest.java
- violation_proof: backend/src/test/java/com/ax/template/authblueprint/bundlepricing/BundlePricingViolationProofTest.java
- adversarial_review: ACCEPT (opus refute-by-default, 2026-06-27). All 5 skeptical pre-predictions REFUTED from evidence (no stored total → no drift; assertions pin literal sums so a 0-stub/echo fails every one; ViolationProof genuinely falsifies on realistic mutations; @Check exclusive; BL quotes byte-exact substrings of BundleOrderItemImpl.java:265/254 + ProductBundlePricingModelType.java:36). 0 Critical / 0 Major. Closed the 4 Minor hardening items the review surfaced: tightened @Check to reject a stray base_sale_price on ITEM_SUM rows (NULL-base UNKNOWN hole — entity + V076), added the missing BUNDLE-mode taxablePrice assertion and the all-children-no-sale ITEM_SUM salePrice assertion. testBundlePricing 15/15 GREEN after hardening.

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| an ITEM_SUM bundle's retail price is the sum of each child's retail price × quantity, plus the bundle fees | itemsum_001: ITEM_SUM [(1000×2),(500×3)] + fee 200 → retailPrice == 3700 == independently reconstructed Σ child retailSubtotal + Σ fee amount (conservation) |
| an ITEM_SUM bundle's sale price falls back to a child's retail price when that child has no sale price | itemsum_001: child1 sale 900, child2 no sale → salePrice == 900×2 + 500×3 + 200 == 3500 |
| an ITEM_SUM bundle's taxable price sums only the taxable children (+ taxable fees) | itemsum_001 / derived_001: taxablePrice == 2200 excludes the non-taxable child; mixed bundle taxablePrice == 2000 (only the taxable child) |
| a BUNDLE-mode bundle uses its fixed base price and is NOT summed from its children (shouldSumItems == false) | fixed_001: BUNDLE base 5000 with children summing to 3500 → retailPrice == 5000 ≠ 3500 |
| a bundle's taxability is derived from its children, not independently set | derived_001: all-non-taxable children → taxable=false, taxablePrice=0; one taxable child → taxable=true (ViolationProof: no stored taxable column, no public setter) |
| (ax STRENGTHENING — not Broadleaf parity) the conserving total is structurally unrepresentable to violate | ViolationProof: no stored total/rolledUp column; no public price setter; immutable children (@Column updatable=false); mode/base-price @Check exclusivity |

> Broadleaf NOTE: Broadleaf computes these prices lazily as derived getters on `BundleOrderItemImpl`
> (`getRetailPrice`/`getSalePrice`/`getTaxablePrice`) — there is no stored bundle total either. ax
> absorbs the same derive-on-read posture and HARDENS it: the conserving total has no column to
> store, the children are immutable, and a `@Check` makes the ITEM_SUM/BUNDLE base-price shape
> mutually exclusive — so the wrong mode cannot accidentally sum (or fix-price) a bundle.

> G008 direction: this is the COMPOSITION (roll-up) direction — many children → one conserving
> composite total. It is the dual of banded-pricing-l0 (one quantity → many bands) and promotion-l0
> (one discount → many lines), both decomposition; catalog-commerce-l0 explicitly DEFERS price
> computation. Genuinely new — confirmed by anti-re-find census (no existing bundle spec).
