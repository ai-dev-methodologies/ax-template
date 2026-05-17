package com.ax.template.authblueprint.practices.portability;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PORTABILITY")
@Tag("PORTABILITY-LANG-002")
class PortabilityNoPublicMutableFieldsTest {

    // Source rule: PRACTICES-LANG-003 lang-no-public-mutable-fields. Universal — no
    // class-specific assumption.

    @Test
    void portability_LANG_002_petclinic_noPublicMutableInstanceFields() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        rule().check(classes);
    }

    @Test
    void portability_LANG_002_realworld_noPublicMutableInstanceFields() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        rule().check(classes);
    }

    @Test
    void portability_LANG_002_modulith_noPublicMutableInstanceFields() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.MODULITH, PortabilityFixtures.MODULITH_CLASSES);
        rule().check(classes);
    }

    private static ArchRule rule() {
        return fields()
                .that().areDeclaredInClassesThat().areNotRecords()
                .and().arePublic()
                .and().areNotStatic()
                .should(notBeAccessibleMutableState())
                .allowEmptyShould(true);
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
