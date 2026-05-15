package com.ax.template.authblueprint.practices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-TEST-003")
class ArchitectureNoCyclicPackageTest {

    // practices/ itself is flat (no sub-packages today), so we slice one level higher
    // — across the immediate children of authblueprint (auth, crud, practices, security,
    // user). This catches cycles in the broader module shape, which is the real value of
    // the rule. When practices/ grows sub-packages, switch this back to a practices-local
    // slicing pattern.
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint..");

    @Test
    void practices_TEST_003_noCyclicPackageDependencies() {
        SlicesRuleDefinition.slices()
                .matching("com.ax.template.authblueprint.(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
