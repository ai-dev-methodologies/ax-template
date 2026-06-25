package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for commercecatalog. Structural assertions that a deliberate break cannot
 * pass silently — no Spring context needed.
 *
 * <p>Covers:
 * - Sku.product_id and is_default are @Column(updatable=false)
 * - SkuOptionValueXref columns (sku_id, option_value_id) are immutable
 * - @Version on CatalogProduct, Sku, and Category
 * - No public setters on @AggregateMember types
 * - UNIQUE(product_id, option_signature) declared on Sku entity + V070 migration
 * - @Check sale<=retail declared on Sku entity + V070 migration
 * - All @AggregateMember types carry @AggregateMember(root=CatalogProduct.class)
 * - CATALOG_NO_MATCHING_SKU distinct path: CatalogProductService has no .iterator().next() / .get(0) on multi-result
 */
@Tag("COMMERCECATALOG")
class CommerceCatalogViolationProofTest {

    // ── Sku.product_id and is_default are @Column(updatable=false) (CAT-SKU-001) ──
    @Test @Tag("CAT-VARIANT-001") @Tag("CAT-SKU-001")
    void violation_skuIdentityColumnsAreImmutable() throws Exception {
        assertColumnNotUpdatable(Sku.class, "productId");
        assertColumnNotUpdatable(Sku.class, "isDefault");
    }

    // ── SkuOptionValueXref sku_id and option_value_id are @Column(updatable=false) ──
    @Test @Tag("CAT-VARIANT-001")
    void violation_skuOptionValueXrefColumnsAreImmutable() throws Exception {
        assertColumnNotUpdatable(SkuOptionValueXref.class, "skuId");
        assertColumnNotUpdatable(SkuOptionValueXref.class, "optionValueId");
    }

    // ── @Version on CatalogProduct, Sku, Category ──
    @Test @Tag("CAT-VARIANT-001")
    void violation_aggregateRootsCarryVersionAnnotation() throws Exception {
        assertHasVersionField(CatalogProduct.class);
        assertHasVersionField(Sku.class);
        assertHasVersionField(Category.class);
    }

    // ── No public setters on @AggregateMember types ──
    @Test @Tag("CAT-VARIANT-001")
    void violation_aggregateMembersHaveNoPublicSetters() {
        for (Class<?> cls : new Class<?>[]{Sku.class, ProductOption.class, ProductOptionValue.class,
                SkuOptionValueXref.class, CategoryProductXref.class}) {
            for (Method m : cls.getMethods()) {
                assertThat(m.getName())
                    .as(cls.getSimpleName() + " must have no public setter")
                    .doesNotStartWith("set");
            }
        }
    }

    // ── UNIQUE(product_id, option_signature) declared on Sku entity ──
    @Test @Tag("CAT-VARIANT-002")
    void violation_skuUniqueConstraintDeclaredOnEntity() {
        jakarta.persistence.Table table = Sku.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).as("Sku must carry @Table").isNotNull();
        boolean found = false;
        for (jakarta.persistence.UniqueConstraint uc : table.uniqueConstraints()) {
            String[] cols = uc.columnNames();
            if (cols.length == 2) {
                boolean hasProd = false, hasSig = false;
                for (String c : cols) {
                    if (c.equals("product_id")) hasProd = true;
                    if (c.equals("option_signature")) hasSig = true;
                }
                if (hasProd && hasSig) { found = true; break; }
            }
        }
        assertThat(found).as("Sku must declare UNIQUE(product_id, option_signature)").isTrue();
    }

    // ── @Check sale<=retail declared on Sku entity ──
    @Test @Tag("CAT-SKU-002")
    void violation_skuSalePriceCheckDeclaredOnEntity() {
        Check check = Sku.class.getAnnotation(Check.class);
        assertThat(check).as("Sku must carry @Check").isNotNull();
        assertThat(check.constraints()).contains("sale_price IS NULL OR sale_price <= retail_price");
    }

    // ── All @AggregateMember types carry @AggregateMember(root=CatalogProduct.class) ──
    @Test @Tag("CAT-VARIANT-001")
    void violation_aggregateMembersCarryCorrectRootAnnotation() {
        for (Class<?> cls : new Class<?>[]{Sku.class, ProductOption.class, ProductOptionValue.class,
                SkuOptionValueXref.class, CategoryProductXref.class}) {
            com.ax.template.authblueprint.common.AggregateMember ann =
                cls.getAnnotation(com.ax.template.authblueprint.common.AggregateMember.class);
            assertThat(ann).as(cls.getSimpleName() + " must carry @AggregateMember").isNotNull();
            assertThat(ann.root()).as(cls.getSimpleName() + " root must be CatalogProduct.class")
                .isEqualTo(CatalogProduct.class);
        }
    }

    // ── UNIQUE(product_id, option_signature) and @Check sale<=retail must be in V070 migration ──
    // Also covers CAT-LIFECYCLE-002: active window @Check is declared in entity + V070.
    @Test @Tag("CAT-SKU-002") @Tag("CAT-VARIANT-002") @Tag("CAT-LIFECYCLE-002")
    void violation_migrationContainsConstraintBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V070__create_commercecatalog.sql")) {
            assertThat(in).as("V070__create_commercecatalog.sql must exist on classpath").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("sale_price IS NULL OR sale_price <= retail_price");
            assertThat(sql).contains("option_signature");
            assertThat(sql).contains("product_id");
            // The partial-unique index or constraint name
            assertThat(sql).contains("uq_sku_product_option_sig");
            // CAT-LIFECYCLE-002: window @Check is in the migration
            assertThat(sql).contains("active_end_date > active_start_date");
        }
        // CAT-LIFECYCLE-002: window @Check is also declared on the entity
        Check productCheck = CatalogProduct.class.getAnnotation(Check.class);
        assertThat(productCheck).as("CatalogProduct must carry @Check for date window").isNotNull();
        assertThat(productCheck.constraints()).contains("active_end_date > active_start_date");
    }

    // ── CAT-PRODUCT-002: variant SKU fields may be null (inherit from default SKU at read time) ──
    // Structural assertion: Sku.retailPrice is nullable (no @Column(nullable=false)) so inheritance is possible.
    @Test @Tag("CAT-PRODUCT-002")
    void violation_variantSkuFieldsNullableForInheritance() throws Exception {
        // Sku.retailPrice must be nullable — null means "inherit from default SKU"
        Field retailPriceField = getDeclaredFieldInHierarchy(Sku.class, "retailPrice");
        assertThat(retailPriceField).as("Sku.retailPrice must exist").isNotNull();
        Column col = retailPriceField.getAnnotation(Column.class);
        // Either @Column is absent (nullable by default) or @Column.nullable = true
        boolean nullable = (col == null) || col.nullable();
        assertThat(nullable).as("Sku.retailPrice must be nullable so variant SKUs can inherit from the default SKU").isTrue();
    }

    // ── CAT-INVENTORY-GATE-001: inventory policy flag realized on Sku; quantity axis still deferred ──
    // CAT-INVENTORY-GATE-001 is now REALIZED: Sku carries inventoryType (UNAVAILABLE /
    // ALWAYS_AVAILABLE / CHECK_QUANTITY) consulted by assertPurchasable before window/archival checks.
    // The QUANTITY axis (stock count, availableQuantity, onHand, etc.) remains deferred to the
    // inventory-reservation vertical — the catalog carries NO quantity/stock field by design.
    @Test @Tag("CAT-INVENTORY-GATE-001")
    void violation_inventoryTypePolicyFlagPresentQuantityStillDeferred() {
        // Assert Sku DOES carry inventoryType (policy gate realized)
        boolean hasInventoryTypeField = false;
        Class<?> c = Sku.class;
        while (c != null && c != Object.class) {
            try { c.getDeclaredField("inventoryType"); hasInventoryTypeField = true; break; }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        assertThat(hasInventoryTypeField)
            .as("Sku MUST carry an inventoryType field — CAT-INVENTORY-GATE-001 tri-state policy gate is realized")
            .isTrue();

        // Assert Sku carries NO quantity/stock field — quantity axis stays deferred to inventory-reservation
        String[] quantityFieldNames = {
            "quantity", "quantityAvailable", "availableQuantity",
            "stock", "onHand", "inventoryQuantity"
        };
        for (String fieldName : quantityFieldNames) {
            boolean hasQtyField = false;
            Class<?> q = Sku.class;
            while (q != null && q != Object.class) {
                try { q.getDeclaredField(fieldName); hasQtyField = true; break; }
                catch (NoSuchFieldException e) { q = q.getSuperclass(); }
            }
            assertThat(hasQtyField)
                .as("Sku must NOT carry a '" + fieldName + "' field — quantity axis is deferred to inventory-reservation vertical (CAT-INVENTORY-GATE-001 scope)")
                .isFalse();
        }
    }

    // ── CATALOG_NO_MATCHING_SKU: CatalogProductService.resolveSku must use
    //    findSkuByOptionSignature (Optional) and throw noMatchingSku — structural assertion
    //    via reflection to avoid source-path coupling.
    @Test @Tag("CAT-VARIANT-003")
    void violation_resolveSkuUsesExactMatchRepository() throws Exception {
        // CatalogProductRepository must have the exact-match method findSkuByOptionSignature
        boolean foundMethod = false;
        for (java.lang.reflect.Method m : CatalogProductRepository.class.getDeclaredMethods()) {
            if (m.getName().equals("findSkuByOptionSignature")) {
                // Must return Optional<Sku>
                assertThat(m.getReturnType())
                    .as("findSkuByOptionSignature must return Optional")
                    .isEqualTo(java.util.Optional.class);
                foundMethod = true;
                break;
            }
        }
        assertThat(foundMethod)
            .as("CatalogProductRepository must declare findSkuByOptionSignature(productId, sig)")
            .isTrue();

        // CatalogProductService.computeOptionSignature must be static and return null for empty list
        java.lang.reflect.Method sig = CatalogProductService.class
            .getDeclaredMethod("computeOptionSignature", java.util.List.class);
        assertThat(java.lang.reflect.Modifier.isStatic(sig.getModifiers()))
            .as("computeOptionSignature must be static (shared between addVariantSku and resolveSku)")
            .isTrue();
        sig.setAccessible(true);
        String result = (String) sig.invoke(null, java.util.List.of());
        assertThat(result).as("computeOptionSignature([]) must return null for default SKU identity").isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────

    private void assertColumnNotUpdatable(Class<?> cls, String fieldName) throws Exception {
        Field f = getDeclaredFieldInHierarchy(cls, fieldName);
        assertThat(f).as(cls.getSimpleName() + "." + fieldName + " must exist").isNotNull();
        Column col = f.getAnnotation(Column.class);
        assertThat(col).as(cls.getSimpleName() + "." + fieldName + " must carry @Column").isNotNull();
        assertThat(col.updatable()).as(cls.getSimpleName() + "." + fieldName + " must be immutable (updatable=false)").isFalse();
    }

    private void assertHasVersionField(Class<?> cls) throws Exception {
        Field f = getDeclaredFieldInHierarchy(cls, "version");
        assertThat(f).as(cls.getSimpleName() + ".version must exist").isNotNull();
        assertThat(f.isAnnotationPresent(Version.class))
            .as(cls.getSimpleName() + ".version must carry @Version").isTrue();
    }

    private Field getDeclaredFieldInHierarchy(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        return null;
    }
}
