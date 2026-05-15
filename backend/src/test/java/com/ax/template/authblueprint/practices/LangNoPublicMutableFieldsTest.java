package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-LANG-003")
class LangNoPublicMutableFieldsTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_LANG_003_noPublicMutableInstanceFields() {
        // Public mutable instance fields bypass encapsulation, defeat invariants, and
        // make every dependent on the class fragile. Constants (static final) are fine;
        // record components project public *accessor methods*, not fields, so records are
        // unaffected by this rule.
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areNotRecords()
                .and().arePublic()
                .and().areNotStatic()
                .should(notBeAccessibleMutableState())
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaField> notBeAccessibleMutableState() {
        return new ArchCondition<>("not be public mutable instance state") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaField field, ConditionEvents events) {
                if (field.getModifiers().contains(JavaModifier.FINAL)) {
                    return;
                }
                JavaClass owner = field.getOwner();
                String message = String.format(
                        "Field %s.%s is public, non-static, non-final — anti-pattern",
                        owner.getName(), field.getName());
                events.add(SimpleConditionEvent.violated(field, message));
            }
        };
    }
}
