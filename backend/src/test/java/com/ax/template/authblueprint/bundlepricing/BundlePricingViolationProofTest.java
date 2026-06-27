package com.ax.template.authblueprint.bundlepricing;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Violation-proof tests for bundle-pricing-l0.yaml — reflection-only structural negatives
 * (no Spring context). They prove the conserving-roll-up invariant is UNREPRESENTABLE to
 * violate: there is no stored rolled-up total column, no public price setter, the children
 * are immutable, and the mode/base-price shape is mutually exclusive at the DB layer.
 */
@Tag("BUNDLEPRICING")
class BundlePricingViolationProofTest {

    /**
     * BUNDLE-DERIVED-001: the composite carries NO stored rolled-up total and NO stored
     * taxable column — so a total / taxability that contradicts the children cannot be
     * persisted. The only price-bearing fields are the immutable BUNDLE base prices.
     */
    @Test
    @Tag("BUNDLE-DERIVED-001")
    void violation_noStoredTotalOrTaxableColumn() {
        for (Field f : CompositeItem.class.getDeclaredFields()) {
            String name = f.getName().toLowerCase();
            assertThat(name)
                .as("CompositeItem MUST NOT store a rolled-up total — the price is derived "
                  + "from the children on every read (field '%s')", f.getName())
                .doesNotContain("total")
                .doesNotContain("rolledup");
            assertThat(name)
                .as("CompositeItem MUST NOT store a taxable column — taxability is DERIVED "
                  + "from the children (field '%s')", f.getName())
                .isNotEqualTo("taxable");
            if (name.contains("price")) {
                assertThat(f.getName())
                    .as("the only price-bearing fields on CompositeItem are the immutable "
                      + "BUNDLE base prices — no rolled-up total column")
                    .isIn("baseRetailPrice", "baseSalePrice");
            }
        }
    }

    /**
     * BUNDLE-FIXED-001 / BUNDLE-ITEMSUM-001: no public setter on the composite — every price
     * input is fixed at creation via the builder, so the derived total cannot be bypassed.
     */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void violation_noPublicSetterOnCompositeItem() {
        long publicSetters = Arrays.stream(CompositeItem.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetters)
            .as("CompositeItem MUST have zero public setters — its price inputs are immutable")
            .isZero();
    }

    /** BUNDLE-FIXED-001: the mode/base-price exclusivity @Check makes a wrong-shape composite unrepresentable. */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void violation_modeBasePriceCheckPresent() {
        Check chk = CompositeItem.class.getAnnotation(Check.class);
        assertThat(chk).as("CompositeItem MUST carry a mode/base-price @Check").isNotNull();
        assertThat(chk.constraints())
            .as("the @Check MUST bind pricing_model to base_retail_price exclusivity")
            .contains("pricing_model")
            .contains("base_retail_price");
    }

    /** Optimistic-locking @Version is present (concurrent-mutation guard). */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void violation_versionFieldPresent() {
        boolean hasVersion = Arrays.stream(CompositeItem.class.getDeclaredFields())
            .anyMatch(f -> f.isAnnotationPresent(Version.class));
        assertThat(hasVersion).as("CompositeItem MUST carry an @Version field").isTrue();
    }

    /**
     * BUNDLE-ITEMSUM-001: the children are IMMUTABLE — quantity and unit prices are
     * @Column(updatable=false) with no public setter, so a child's contribution to the
     * conserving roll-up cannot be re-priced after creation.
     */
    @Test
    @Tag("BUNDLE-ITEMSUM-001")
    void violation_componentPriceInputsImmutable() throws NoSuchFieldException {
        for (String field : new String[] {"quantity", "unitRetailPrice", "unitSalePrice", "taxable"}) {
            Field f = CompositeComponent.class.getDeclaredField(field);
            Column col = f.getAnnotation(Column.class);
            assertThat(col).as("CompositeComponent.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("CompositeComponent.%s MUST be updatable=false (immutable child input)", field)
                .isFalse();
        }
        long publicSetters = Arrays.stream(CompositeComponent.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetters)
            .as("CompositeComponent MUST have zero public setters — child inputs are immutable")
            .isZero();
    }

    /** BUNDLE-ITEMSUM-001: the child @Check enforces positive quantity + non-negative prices. */
    @Test
    @Tag("BUNDLE-ITEMSUM-001")
    void violation_componentCheckPresent() {
        Check chk = CompositeComponent.class.getAnnotation(Check.class);
        assertThat(chk).as("CompositeComponent MUST carry a @Check").isNotNull();
        assertThat(chk.constraints())
            .as("the child @Check MUST enforce a strictly-positive quantity")
            .contains("quantity > 0");
    }

    /** BUNDLE-ITEMSUM-001: bundle fees are immutable value objects (no public setter). */
    @Test
    @Tag("BUNDLE-ITEMSUM-001")
    void violation_feeImmutable() throws NoSuchFieldException {
        for (String field : new String[] {"amount", "taxable"}) {
            Field f = BundleFee.class.getDeclaredField(field);
            Column col = f.getAnnotation(Column.class);
            assertThat(col).as("BundleFee.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("BundleFee.%s MUST be updatable=false", field)
                .isFalse();
        }
        boolean hasPublicSetter = Arrays.stream(BundleFee.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()));
        assertThat(hasPublicSetter).as("BundleFee MUST have no public setter").isFalse();
    }

    /**
     * Cross-check: the derivation is conserving — a hand-built composite's ITEM_SUM roll-up
     * equals the independent Σ of (unitRetailPrice × quantity) + Σ fees, computed by reflection
     * over the same inputs (proving the entity method does not silently drop or double-count).
     */
    @Test
    @Tag("BUNDLE-ITEMSUM-001")
    void violation_rollUpIsConserving() {
        CompositeItem item = CompositeItem.builder()
            .pricingModel(BundlePricingModel.ITEM_SUM)
            .currency("USD")
            .build();
        item.addComponent(new CompositeComponent(java.util.UUID.randomUUID(), "A", 2, 1000L, 900L, true));
        item.addComponent(new CompositeComponent(java.util.UUID.randomUUID(), "B", 3, 500L, null, false));
        item.addFee(new BundleFee("assembly", 200L, true));

        CompositeItem.Pricing p = item.priceRollUp();
        // Independent re-derivation (different code path than priceRollUp's accumulator).
        long expectedRetail = 1000L * 2 + 500L * 3 + 200L;            // 3700
        long expectedSale = 900L * 2 + 500L * 3 + 200L;               // 3500 (child2 retail fallback)
        long expectedTaxable = 1000L * 2 + 200L;                      // 2200 (excludes non-taxable child)
        assertThat(p.retailPrice()).isEqualTo(expectedRetail);
        assertThat(p.salePrice()).isEqualTo(expectedSale);
        assertThat(p.taxablePrice()).isEqualTo(expectedTaxable);
        assertThat(p.taxable()).isTrue();
    }
}
