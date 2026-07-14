package com.ax.template.authblueprint.mececlassification;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for mece-classification-l0. Structural assertions that a deliberate break cannot
 * pass silently: every column across all four entities is immutable, no public setter or delete
 * path exists anywhere, the scheme carries the residual @Check, the two exclusivity/uniqueness
 * constraints are declared, and the migration carries the same backstops — no Spring context.
 */
@Tag("MECE_CLASSIFICATION")
class MeceViolationProofTest {

    // ── MECE-RECLASS-003 — no public setter anywhere in the domain ──
    @Test @Tag("MECE-RECLASS-003")
    void violation_noPublicSetterAnywhere() {
        for (Class<?> entity : new Class<?>[]{
                ClassificationScheme.class, ClassificationRule.class, ItemClassification.class, ClassificationMove.class}) {
            for (Method m : entity.getMethods()) {
                assertThat(m.getName()).as(entity.getSimpleName() + " must expose no public setter").doesNotStartWith("set");
            }
        }
    }

    // ── every column on every entity is immutable ──
    @Test @Tag("MECE-EXCLUSIVE-001") @Tag("MECE-RECLASS-003")
    void violation_everyColumnImmutable() throws Exception {
        assertColumnsImmutable(ClassificationScheme.class, "id", "schemeKey", "residualCategory", "createdAt");
        assertColumnsImmutable(ClassificationRule.class, "id", "schemeKey", "matchValue", "category", "createdAt");
        assertColumnsImmutable(ItemClassification.class, "id", "schemeKey", "itemRef", "createdAt");
        assertColumnsImmutable(ClassificationMove.class,
            "id", "classificationId", "fromCategory", "toCategory", "actor", "reason", "movedAt");
    }

    private void assertColumnsImmutable(Class<?> entity, String... fields) throws Exception {
        for (String f : fields) {
            Column col = entity.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(entity.getSimpleName() + "." + f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as(entity.getSimpleName() + "." + f + " must be immutable").isFalse();
        }
    }

    // ── MECE-EXHAUSTIVE-002 — the scheme carries the non-blank-residual @Check ──
    @Test @Tag("MECE-EXHAUSTIVE-002")
    void violation_schemeCarriesResidualCheckImplication() {
        Check check = ClassificationScheme.class.getAnnotation(Check.class);
        assertThat(check).as("ClassificationScheme must carry @Check").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("LENGTH(residual_category) > 0");
    }

    // ── MECE-EXCLUSIVE-001 — uq(scheme_key, item_ref); MECE-EXHAUSTIVE-002 — uq(scheme_key, match_value) ──
    @Test @Tag("MECE-EXCLUSIVE-001") @Tag("MECE-EXHAUSTIVE-002")
    void violation_uniqueConstraintsDeclared() {
        assertThat(Arrays.stream(ItemClassification.class.getAnnotation(Table.class).uniqueConstraints())
            .map(UniqueConstraint::name)).contains("uq_mece_scheme_item");
        assertThat(Arrays.stream(ClassificationRule.class.getAnnotation(Table.class).uniqueConstraints())
            .map(UniqueConstraint::name)).contains("uq_mece_rule_match");
        assertThat(Arrays.stream(ClassificationScheme.class.getAnnotation(Table.class).uniqueConstraints())
            .map(UniqueConstraint::name)).contains("uq_mece_scheme_key");
    }

    // ── no delete method anywhere in either repository ──
    @Test @Tag("MECE-RECLASS-003")
    void violation_noDeleteMethodDeclared() {
        for (Class<?> repo : new Class<?>[]{ClassificationSchemeRepository.class, ItemClassificationRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName().toLowerCase())
                    .as(repo.getSimpleName() + " must declare no delete method")
                    .doesNotContain("delete");
            }
        }
    }

    // ── the migration carries the same backstops as the entities ──
    @Test @Tag("MECE-EXCLUSIVE-001") @Tag("MECE-EXHAUSTIVE-002")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V107__create_mece_classification.sql")) {
            assertThat(in).as("V107__create_mece_classification.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("LENGTH(residual_category) > 0");
            assertThat(sql).contains("UNIQUE INDEX uq_mece_scheme_item");
            assertThat(sql).contains("UNIQUE INDEX uq_mece_rule_match");
        }
    }
}
