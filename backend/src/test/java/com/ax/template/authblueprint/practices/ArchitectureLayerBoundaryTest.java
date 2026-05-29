package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-TEST-002")
class ArchitectureLayerBoundaryTest {

    // IMW1-A (IDW1 dogfood 2026-05-29): re-scoped from the demo `practices` package to
    // the WHOLE authblueprint tree so the layering contract is enforced on ALL ~40
    // domains, not just the demo — the largest silent enforcement hole the dogfood found.
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint");

    @Test
    void practices_TEST_002_servicesDoNotDependOnControllers() {
        // A service depending on a controller is a layer-boundary violation: it inverts
        // the conventional layering (controllers route → services execute) and produces
        // import cycles + initialization ordering problems.
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void practices_TEST_002_repositoriesDoNotDependOnControllersOrServices() {
        // Repositories sit at the bottom of the conventional layer stack — they must not
        // know about the layers above them.
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void practices_TEST_002_controllersDoNotDependOnRepositories() {
        // Controllers must route through services, never touch repositories directly.
        // IDW1 dogfood: this ban did not previously exist (even within practices), so
        // controller→repository bypasses slipped past all guards on every real domain.
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
