# Broadleaf-absorption parity — inventory + customer

- vertical: inventory-customer
- broadleaf_source: core/.../inventory/service/type/InventoryType.java:40; inventory/service/InventoryServiceImpl.java:71; broadleaf-profile/.../profile/core/service/CustomerAddressServiceImpl.java:49,56; CustomerAddress.orm.xml:48
- spec_items: CAT-INVENTORY-GATE-001, DEFAULT-SINGLETON-001
- rule: practices/rules/catalog-variant-resolves-unique-active-sku-and-purchasability-gated.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/commercecatalog/CatalogInventoryGateTest.java
- violation_proof: backend/src/test/java/com/ax/template/authblueprint/commercecatalog/CommerceCatalogViolationProofTest.java
- adversarial_review: ACCEPT (0 CRITICAL/MAJOR — first vertical with no real defects; all 8 evidence quotes byte-accurate; default-singleton distinct from ordered-collection confirmed; anon→registered SKIP confirmed correct; 2 non-blocking MINOR)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| an UNAVAILABLE SKU is never purchasable regardless of stock | CatalogInventoryGateTest UNAVAILABLE → 409 |
| an ALWAYS_AVAILABLE SKU is purchasable without consulting quantity | ALWAYS_AVAILABLE → 200, no quantity field |
| a CHECK_QUANTITY SKU passes the catalog gate (quantity deferred to inventory-reservation) | CHECK_QUANTITY → 200; quantity axis not in catalog (ViolationProofTest asserts no quantity field) |
| setting a member default unsets every other default (exactly one) | DEFAULT-SINGLETON-001 (review-tier rule): clear-all-then-set-one + partial unique index |
| the first member of an empty collection auto-defaults | DEFAULT-SINGLETON-002 (review-tier): empty→first auto-default |

> Inventory conservation (decrement/increment) is a RE-FIND of INVRES-COMMIT/RELEASE — not
> re-absorbed. The customer default-singleton is review-tier (no single natural domain;
> the rule + partial-unique-index DDL is the contract). DEFAULT-SINGLETON-001 lives in
> specs/default-member-singleton-l0.yaml (new generic spec).
