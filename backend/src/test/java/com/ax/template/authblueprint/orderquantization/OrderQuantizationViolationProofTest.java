package com.ax.template.authblueprint.orderquantization;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for order-multiple-quantization-l0. Structural assertions a deliberate break
 * cannot pass silently: the quantizer rounds UP and floors at the MOQ, the overage is exactly the
 * non-conserving surplus, the record carries @Version + the @Check backstops (overage identity +
 * positivity), every basis column is immutable, there are NO public setters and NO mutator hooks,
 * NO delete path exists anywhere in the domain, and the migration carries the same backstops.
 */
@Tag("ORDERQUANTIZATION")
class OrderQuantizationViolationProofTest {

    // ── ORDERQUANT-QUANTIZE-001 — the quantizer rounds UP and floors at the MOQ ──
    @Test @Tag("ORDERQUANT-QUANTIZE-001")
    void violation_quantizerRoundsUp_flooredAtMoq() {
        assertThat(Quantizer.quantize(10, 1, 1)).isEqualTo(10L);   // exact fit
        assertThat(Quantizer.quantize(23, 1, 10)).isEqualTo(30L);  // ceil 23→30
        assertThat(Quantizer.quantize(5, 50, 10)).isEqualTo(50L);  // MOQ floor dominates
        assertThat(Quantizer.quantize(0, 25, 5)).isEqualTo(25L);   // required 0 → MOQ
        assertThat(Quantizer.quantize(100, 1, 12)).isEqualTo(108L);// ceil 100→108
        assertThat(Quantizer.quantize(120, 1, 1)).isEqualTo(120L); // multiple 1 → exact
        // the result is NEVER below the requirement (non-conserving direction is up, not down),
        // and with a MOQ that is itself a multiple, the result is always an exact multiple of the lot.
        for (long required = 0; required <= 200; required += 7) {
            long q = Quantizer.quantize(required, 10, 10);   // moq=10 is itself a multiple of 10
            assertThat(q).as("orderQuantity >= required for required=" + required).isGreaterThanOrEqualTo(required);
            assertThat(q % 10).as("orderQuantity is an exact multiple for required=" + required).isZero();
        }
    }

    // ── ORDERQUANT-OVERAGE-001 — overage is exactly the non-conserving surplus; @Check binds it ──
    @Test @Tag("ORDERQUANT-OVERAGE-001")
    void violation_overageIdentity_isCheckBound() {
        Check check = OrderQuantization.class.getAnnotation(Check.class);
        assertThat(check).as("OrderQuantization must carry @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("overage = order_quantity - required_quantity");
        assertThat(c).contains("overage >= 0");
        assertThat(c).contains("order_quantity >= required_quantity");
    }

    // ── ORDERQUANT-CONSTRAINT-001 — positivity backstops on the entity ──
    @Test @Tag("ORDERQUANT-CONSTRAINT-001")
    void violation_constraintPositivity_isCheckBound() {
        Check check = OrderQuantization.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("required_quantity >= 0");
        assertThat(c).contains("moq >= 1");
        assertThat(c).contains("order_multiple >= 1");
    }

    // ── ORDERQUANT-BASIS-001 — every basis column immutable; no setter; no mutator hook; @Version ──
    @Test @Tag("ORDERQUANT-BASIS-001")
    void violation_basisImmutable_noSetters_noMutators_versioned() throws Exception {
        for (Method m : OrderQuantization.class.getMethods()) {
            assertThat(m.getName()).as("OrderQuantization must have no public setter").doesNotStartWith("set");
        }
        // the record is purely immutable — the ONLY non-getter/non-Object declared methods are absent;
        // every declared method is either a getter or the constructor's synthetic, so no mutator hook exists
        for (Method m : OrderQuantization.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            assertThat(m.getName())
                .as("OrderQuantization." + m.getName() + " must be a getter — no mutator hooks on this immutable record")
                .startsWith("get");
        }
        for (String f : new String[]{"id", "itemRef", "requiredQuantity", "moq", "orderMultiple",
                                     "orderQuantity", "overage", "createdAt"}) {
            Column col = OrderQuantization.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("OrderQuantization." + f + " must be immutable").isFalse();
        }
        assertThat(OrderQuantization.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        // the columns are order_quantity / order_multiple — NEVER 'value' or 'order'
        assertThat(OrderQuantization.class.getDeclaredField("orderQuantity").getAnnotation(Column.class).name())
            .isEqualTo("order_quantity");
        assertThat(OrderQuantization.class.getDeclaredField("orderMultiple").getAnnotation(Column.class).name())
            .isEqualTo("order_multiple");
    }

    // ── ORDERQUANT-BASIS-001 — NO delete path; the service guards before the divide ──
    @Test @Tag("ORDERQUANT-BASIS-001") @Tag("ORDERQUANT-CONSTRAINT-001")
    void violation_noDeletePath_serviceGuardsDivisor() throws Exception {
        for (Method m : OrderQuantizationRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"OrderQuantizationService", "OrderQuantizationController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "orderquantization", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — quantizations are append-only facts")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "orderquantization", "OrderQuantizationService.java"));
        assertThat(svc).as("the service rejects a non-positive multiple before the quantizer divides")
            .contains("multiple < 1");
        assertThat(svc).as("the overage is computed as the non-conserving surplus")
            .contains("orderQuantity - required");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("ORDERQUANT-OVERAGE-001") @Tag("ORDERQUANT-CONSTRAINT-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V066__create_orderquantization.sql")) {
            assertThat(in).as("V066__create_orderquantization.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("required_quantity >= 0 AND moq >= 1 AND order_multiple >= 1");
            assertThat(sql).contains("overage = order_quantity - required_quantity");
            assertThat(sql).contains("order_quantity BIGINT");
            assertThat(sql).contains("order_multiple BIGINT");
        }
    }
}
