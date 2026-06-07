package com.ax.template.authblueprint.transformation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof for transformation-conservation-l0. Pure ConservationCheck logic + structural
 * invariants — no Spring context.
 */
@Tag("TRANSFORMATION")
class TransformationViolationProofTest {

    private static ConservationCheck.Leg in(String q) {
        return new ConservationCheck.Leg(LegRole.INPUT, "m", new BigDecimal(q), "kg", null);
    }
    private static ConservationCheck.Leg good(String q) {
        return new ConservationCheck.Leg(LegRole.GOOD_OUTPUT, "m", new BigDecimal(q), "kg", null);
    }
    private static ConservationCheck.Leg residual(String q, TransformationDisposition d) {
        return new ConservationCheck.Leg(LegRole.RESIDUAL, "m", new BigDecimal(q), "kg", d);
    }

    // ── XFORM-ACCOUNTED-LOSS-001 — conserving passes; imbalance throws ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001")
    void violation_conservationExactOrRejected() {
        assertThatNoException().isThrownBy(() -> {
            ConservationCheck.Result r = ConservationCheck.check(List.of(
                in("100.00"), good("90.00"), residual("7.00", TransformationDisposition.SCRAP),
                residual("3.00", TransformationDisposition.YIELD_LOSS)));
            assertThat(r.totalInput()).isEqualByComparingTo("100.00");
            assertThat(r.totalGood().add(r.totalResidual())).isEqualByComparingTo("100.00");
        });
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            in("100.00"), good("90.00"), residual("5.00", TransformationDisposition.SCRAP)))) // 95 != 100
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_NOT_CONSERVED"));
    }

    // ── XFORM-RESIDUAL-CLASSIFIED-001 — residual without a governed disposition throws; enum is closed ──
    @Test @Tag("XFORM-RESIDUAL-CLASSIFIED-001")
    void violation_residualMustBeClassified_andEnumIsClosed() {
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            in("10.00"), good("8.00"),
            new ConservationCheck.Leg(LegRole.RESIDUAL, "m", new BigDecimal("2.00"), "kg", null)))) // no disposition
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_UNCLASSIFIED_RESIDUAL"));
        // the governed vocabulary is exactly the four categories — no "miscellaneous" bucket
        assertThat(TransformationDisposition.values()).containsExactlyInAnyOrder(
            TransformationDisposition.SCRAP, TransformationDisposition.REWORK,
            TransformationDisposition.YIELD_LOSS, TransformationDisposition.WIP_REMAINDER);
    }

    // ── XFORM-DIMENSION-001 — mixed units throw ──
    @Test @Tag("XFORM-DIMENSION-001")
    void violation_mixedUnitsRejected() {
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            in("100.00"),
            new ConservationCheck.Leg(LegRole.GOOD_OUTPUT, "m", new BigDecimal("90.00"), "ea", null),
            residual("10.00", TransformationDisposition.SCRAP))))
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_MIXED_UNIT"));
    }

    // ── XFORM-ATOMIC-001 — a no-output transformation (inputs consumed, nothing produced) is rejected ──
    @Test @Tag("XFORM-ATOMIC-001")
    void violation_noOutputLegRejected() {
        // a single zero-qty input conserves trivially (0==0) but produces no output -> rejected
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            new ConservationCheck.Leg(LegRole.INPUT, "m", new BigDecimal("0.00"), "kg", null))))
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_INVALID_AMOUNT"));
    }

    // ── a disposition on a non-RESIDUAL leg is rejected (disposition present IFF residual) ──
    @Test @Tag("XFORM-RESIDUAL-CLASSIFIED-001")
    void violation_dispositionOnNonResidualRejected() {
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            in("100.00"),
            new ConservationCheck.Leg(LegRole.GOOD_OUTPUT, "m", new BigDecimal("90.00"), "kg",
                TransformationDisposition.SCRAP),                                  // disposition on GOOD_OUTPUT
            residual("10.00", TransformationDisposition.YIELD_LOSS))))
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_DISPOSITION_NOT_ALLOWED"));
    }

    // ── an over-scale (>4 dp) quantity is rejected at the pure boundary (no setScale 500 deep in the service) ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001")
    void violation_overScaleQuantityRejected() {
        assertThatThrownBy(() -> ConservationCheck.check(List.of(
            in("1.23456"), good("1.00000"), residual("0.23456", TransformationDisposition.SCRAP)))) // conserves but scale 5
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_INVALID_AMOUNT"));
    }

    // ── unit whitespace is normalized — " kg" and "kg" are the same physical unit, not a mixed-unit reject ──
    @Test @Tag("XFORM-DIMENSION-001")
    void violation_unitWhitespaceNormalized() {
        ConservationCheck.Result r = ConservationCheck.check(List.of(
            new ConservationCheck.Leg(LegRole.INPUT, "m", new BigDecimal("100.00"), " kg", null),
            new ConservationCheck.Leg(LegRole.GOOD_OUTPUT, "m", new BigDecimal("100.00"), "kg", null)));
        assertThat(r.baseUnit()).isEqualTo("kg");
    }

    // ── a transformation is bounded (legs <= MAX_LEGS) so the response is always complete ──
    @Test @Tag("XFORM-ATOMIC-001")
    void violation_tooManyLegsRejected() {
        java.util.List<ConservationCheck.Leg> many = new java.util.ArrayList<>();
        many.add(in("201.00"));
        for (int i = 0; i < 201; i++) many.add(good("1.00"));   // 202 legs > MAX_LEGS(200)
        assertThatThrownBy(() -> ConservationCheck.check(many))
            .isInstanceOfSatisfying(TransformationException.class,
                e -> assertThat(e.code()).isEqualTo("XFORM_INVALID_AMOUNT"));
    }

    // ── immutable legs + @Version + no public setter ──
    @Test
    void violation_immutableLegsAndRun() throws Exception {
        for (Class<?> entity : new Class<?>[]{TransformationRun.class, TransformationLeg.class}) {
            for (Method m : entity.getMethods()) {
                assertThat(m.getName()).as(entity.getSimpleName() + " must have no public setter")
                    .doesNotStartWith("set");
            }
        }
        assertThat(TransformationRun.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        for (String f : new String[]{"id", "createdBy", "baseUnit", "totalInput", "totalGood", "totalResidual", "createdAt"}) {
            assertThat(TransformationRun.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("TransformationRun." + f + " immutable").isFalse();
        }
        for (String f : new String[]{"id", "runId", "role", "disposition", "materialCode", "qty", "unit"}) {
            assertThat(TransformationLeg.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("TransformationLeg." + f + " immutable").isFalse();
        }
    }

    // ── XFORM-ATOMIC-001 — migration declares the conservation + residual-classified CHECK backstops ──
    @Test @Tag("XFORM-ATOMIC-001")
    void violation_migrationDeclaresChecks() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V036__create_transformations.sql")) {
            assertThat(in).as("V036 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_xform_conserved");
        assertThat(sql).contains("total_input = total_good + total_residual");
        assertThat(sql).contains("chk_xform_residual_classified");
        assertThat(sql).contains("role <> 'RESIDUAL' OR disposition IS NOT NULL");
    }
}
