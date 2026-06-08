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

    // HG-FEAT-NOCYCLE (DDD decomposition spec 2026-06-08 §6): feature slices must be
    // acyclic. We slice across the immediate children of authblueprint (the ~52 feature
    // packages — auth, billing, payment, ecommerce, …) and require the slice graph to be
    // free of cycles. This is the correct slices().beFreeOfCycles() shape; the spec keeps
    // it as-is (only this stale comment, which named the 5 demo-era packages, is corrected).
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
