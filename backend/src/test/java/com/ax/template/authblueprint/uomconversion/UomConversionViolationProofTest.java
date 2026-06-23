package com.ax.template.authblueprint.uomconversion;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for dimensional-uom-conversion-l0. Structural assertions a deliberate break cannot
 * pass silently: the conversion / material-property rows are append-only immutable, the columns are
 * from_quantity / to_quantity (NEVER 'value'), the material carries @Version, NO delete path exists,
 * the convert path classifies dimensions before any arithmetic, a cross-dimension conversion goes
 * through the material bridge, the arithmetic is BigDecimal (no double), and the migration carries the
 * same backstops.
 */
@Tag("UOMCONVERSION")
class UomConversionViolationProofTest {

    // ── UOMCONV-BASIS-001 — Conversion is append-only, immutable, columns are from/to_quantity not 'value' ──
    @Test @Tag("UOMCONV-BASIS-001")
    void violation_conversionAppendOnly_immutable_noValueColumn() throws Exception {
        for (Method m : Conversion.class.getMethods()) {
            assertThat(m.getName()).as("Conversion must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "fromQuantity", "fromUnit", "toUnit", "fromDimension",
                "toDimension", "mode", "factor", "materialVersion", "resultScale", "toQuantity",
                "idempotencyBasis", "occurredAt", "actor"}) {
            Column col = Conversion.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Conversion." + f + " must be immutable").isFalse();
        }
        // the quantity columns are from_quantity / to_quantity — NEVER the reserved-ish 'value'
        assertThat(Conversion.class.getDeclaredField("fromQuantity").getAnnotation(Column.class).name())
            .isEqualTo("from_quantity");
        assertThat(Conversion.class.getDeclaredField("toQuantity").getAnnotation(Column.class).name())
            .isEqualTo("to_quantity");
        boolean anyValueColumn = false;
        for (var fld : Conversion.class.getDeclaredFields()) {
            Column c = fld.getAnnotation(Column.class);
            if (c != null && "value".equalsIgnoreCase(c.name())) {
                anyValueColumn = true;
            }
        }
        assertThat(anyValueColumn).as("no column may be named 'value'").isFalse();
    }

    // ── UOMCONV-MATERIAL/VERSION-001 — property versions append-only, uq(material_id, version) ──
    @Test @Tag("UOMCONV-MATERIAL-001") @Tag("UOMCONV-VERSION-001")
    void violation_materialPropertyAppendOnly_uniquePerVersion() throws Exception {
        for (Method m : MaterialProperty.class.getMethods()) {
            assertThat(m.getName()).as("MaterialProperty must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "materialId", "version", "fromDimension", "toDimension",
                "factor", "recordedAt"}) {
            Column col = MaterialProperty.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("MaterialProperty." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = MaterialProperty.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("material_id", "version");
        // the factor is a recorded BigDecimal — never an implicit double constant
        assertThat(MaterialProperty.class.getDeclaredField("factor").getType()).isEqualTo(BigDecimal.class);
    }

    // ── UOMCONV-VERSION-001 — the material carries @Version + the @Check backstop; advanceVersion sealed ──
    @Test @Tag("UOMCONV-VERSION-001")
    void violation_materialVersioned_checkBackstop_mutatorSealed() throws Exception {
        assertThat(Material.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        Check check = Material.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("current_version >= 0");

        Method hook = java.util.Arrays.stream(Material.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("advanceVersion")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(hook.getModifiers()))
            .as("Material.advanceVersion must be package-private").isFalse();

        for (String f : new String[]{"id", "materialRef", "createdAt"}) {
            Column col = Material.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("Material." + f + " must be immutable").isFalse();
        }
    }

    // ── UOMCONV-COMPAT-001 — NO delete path; the convert path classifies dimensions before arithmetic ──
    @Test @Tag("UOMCONV-COMPAT-001")
    void violation_noDeletePath_dimensionClassifiedBeforeArithmetic_noDouble() throws Exception {
        for (Method m : MaterialRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"UomConversionService", "UomConversionController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "uomconversion", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — records are kept, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "uomconversion", "UomConversionService.java"));
        // the dimensional-compatibility precondition: same-dim ⇒ ratio, else require a bridge → 422
        assertThat(svc).as("the convert path classifies the from/to dimensions before arithmetic")
            .contains("if (fromDim == toDim)");
        assertThat(svc).as("a cross-dimension conversion requires a bridge or 422 INCOMPATIBLE_DIMENSIONS")
            .contains("incompatibleDimensions(fromDim, toDim)");
        // the bridge is applied as a material-property factor — mass = volume × density
        assertThat(svc).as("the cross-dimension bridge multiplies by the material factor")
            .contains("multiply(bridge.getFactor())");
        // deterministic BigDecimal arithmetic — never double
        assertThat(svc).as("the arithmetic must be BigDecimal HALF_UP — no double")
            .contains("RoundingMode.HALF_UP").doesNotContain("doubleValue()");
    }

    // ── UOMCONV-VERSION-001 — the version-append path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("UOMCONV-VERSION-001")
    void violation_lockedFinder_onVersionAppend() throws Exception {
        Method locked = MaterialRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "uomconversion", "UomConversionService.java"));
        int start = svc.indexOf("public MaterialProperty recordProperty(");
        assertThat(start).as("recordProperty must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("recordProperty must take the material row lock").contains("findByIdForUpdate");
    }

    // ── the migration carries the same backstops — from_quantity/to_quantity, the uniques ──
    @Test @Tag("UOMCONV-BASIS-001") @Tag("UOMCONV-VERSION-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V067__create_uomconversion.sql")) {
            assertThat(in).as("V067__create_uomconversion.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("from_quantity").contains("to_quantity");
            assertThat(sql).doesNotContain(" value ");
            assertThat(sql).contains("current_version >= 0");
            assertThat(sql).contains("uq_uom_material_version");
            assertThat(sql).contains("(material_id, version)");
            assertThat(sql).contains("uq_uom_conversion_basis");
        }
    }
}
