package com.ax.template.authblueprint.practices.portability;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PORTABILITY")
@Tag("PORTABILITY-ARCH-002")
class PortabilityLayerBoundaryTest {

    // Source rule: PRACTICES-TEST-002 arch-layer-boundary. Same simple-name suffixes
    // (Controller / Service / Repository) — fixture-agnostic.

    @Test
    void portability_ARCH_002_petclinic_servicesDoNotDependOnControllers() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_ARCH_002_petclinic_repositoriesDoNotDependOnControllersOrServices() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_ARCH_002_realworld_servicesDoNotDependOnControllers() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_ARCH_002_realworld_repositoriesDoNotDependOnControllersOrServices() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_ARCH_002_modulith_servicesDoNotDependOnControllers() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.MODULITH, PortabilityFixtures.MODULITH_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_ARCH_002_modulith_repositoriesDoNotDependOnControllersOrServices() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.MODULITH, PortabilityFixtures.MODULITH_CLASSES);
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
