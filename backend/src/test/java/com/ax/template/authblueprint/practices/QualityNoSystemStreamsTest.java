package com.ax.template.authblueprint.practices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-QUALITY-003")
class QualityNoSystemStreamsTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_QUALITY_003_noClassAccessesSystemStreams() {
        // Anything written through System.out / System.err bypasses the logger,
        // skips MDC propagation (PRACTICES-OBS-002), and breaks structured logging
        // (PRACTICES-OBS-001). The ArchUnit pre-canned rule rejects every direct
        // access to System.out / System.err.
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .check(CLASSES);
    }
}
