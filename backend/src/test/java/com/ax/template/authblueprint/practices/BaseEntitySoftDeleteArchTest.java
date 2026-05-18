package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * ArchUnit structural check for PRACTICES-PERS-005: soft-delete via @SQLDelete + @Where.
 *
 * <p>Rule 1: every @Entity annotated with @SQLDelete must also carry @Where to auto-filter
 * deleted rows from all JPQL queries. Without @Where the app silently returns deleted records.
 *
 * <p>Rule 2: no @Entity should declare a primitive {@code boolean deleted} field.
 * Timestamp-based soft-delete (Instant deletedAt) is the required pattern.
 */
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-005")
class BaseEntitySoftDeleteArchTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_PERS_005_entitiesWithSQLDeleteMustAlsoHaveWhere() {
        // An entity that overrides the DELETE SQL but omits @Where will still execute
        // the soft-delete UPDATE correctly, but findById / findAll return deleted rows
        // because Hibernate has no automatic filter clause. Both annotations are required.
        ArchRule rule = classes()
                .that().areAnnotatedWith(SQLDelete.class)
                .should().beAnnotatedWith(Where.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void practices_PERS_005_baseEntityFixtureMustHaveAllFourAuditAnnotations() {
        // BaseEntity contract: all 4 Spring Data audit annotations must be present.
        // @CreatedDate + @LastModifiedDate (timestamps) + @CreatedBy + @LastModifiedBy (principals).
        // SoftDeletedRecord is the inline BaseEntity replica used in this test module.
        var softDeletedRecordClass = CLASSES.stream()
                .filter(c -> c.getSimpleName().equals("SoftDeletedRecord"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SoftDeletedRecord not found in scanned classes"));

        List<String> requiredAuditAnnotationNames = List.of(
                CreatedDate.class.getName(),
                LastModifiedDate.class.getName(),
                CreatedBy.class.getName(),
                LastModifiedBy.class.getName()
        );

        for (String annotationName : requiredAuditAnnotationNames) {
            boolean found = softDeletedRecordClass.getFields().stream()
                    .anyMatch(f -> f.isAnnotatedWith(annotationName));
            String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
            assertThat(found)
                    .as("SoftDeletedRecord must have a field annotated with @%s (BaseEntity 4-field audit contract)",
                            simpleName)
                    .isTrue();
        }
    }

    @Test
    void practices_PERS_005_entitiesMustNotUseBooleanDeletedField() {
        // A boolean `deleted` field cannot capture when deletion happened, blocking
        // time-based retention policies (hard-delete rows older than N days).
        // Correct pattern: Instant deletedAt (null = active, non-null = soft-deleted).
        List<String> violations = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(jakarta.persistence.Entity.class))
                .filter(javaClass -> javaClass.getFields().stream()
                        .anyMatch(f -> f.getName().equals("deleted")
                                && (f.getRawType().isEquivalentTo(boolean.class)
                                        || f.getRawType().isEquivalentTo(Boolean.class))))
                .map(c -> c.getName())
                .toList();

        assertThat(violations)
                .as("PRACTICES-PERS-005: these @Entity classes use boolean 'deleted' "
                        + "instead of Instant deletedAt + @SQLDelete + @Where")
                .isEmpty();
    }
}
