package com.ax.template.authblueprint.practices.portability;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PORTABILITY")
@Tag("PORTABILITY-LANG-001")
class PortabilityRecordsForDtosTest {

    // Source rule: PRACTICES-LANG-001 lang-records-for-dtos. *Request/*Response naming
    // convention is universal across Spring Boot apps; the rule's only assumption is
    // that DTO classes carry one of those suffixes.

    @Test
    void portability_LANG_001_petclinic_dtoClassesAreRecords() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .should().beRecords()
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_LANG_001_realworld_dtoClassesAreRecords() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .should().beRecords()
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
