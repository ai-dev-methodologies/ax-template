package com.ax.template.authblueprint.practices.portability;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PORTABILITY")
@Tag("PORTABILITY-ARCH-001")
class PortabilityCyclicPackageTest {

    // Source rule: PRACTICES-TEST-003 arch-no-cyclic-package. The slice pattern is
    // re-anchored per fixture package root; the rule semantics are identical.

    @Test
    void portability_ARCH_001_petclinic_noCyclicPackageDependencies() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        SlicesRuleDefinition.slices()
                .matching(PortabilityFixtures.PETCLINIC_ROOT_PKG + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void portability_ARCH_001_realworld_noCyclicPackageDependencies() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        SlicesRuleDefinition.slices()
                .matching(PortabilityFixtures.REALWORLD_ROOT_PKG + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void portability_ARCH_001_modulith_noCyclicPackageDependencies() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.MODULITH, PortabilityFixtures.MODULITH_CLASSES);
        SlicesRuleDefinition.slices()
                .matching(PortabilityFixtures.MODULITH_ROOT_PKG + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }
}
