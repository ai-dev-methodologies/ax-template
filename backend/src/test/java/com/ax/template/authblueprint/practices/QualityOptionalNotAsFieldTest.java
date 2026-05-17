package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-QUALITY-001")
class QualityOptionalNotAsFieldTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_QUALITY_001_optionalIsNotUsedAsFieldType() {
        // Optional was designed as a return type to signal "may be absent" at the API
        // boundary. As a field it adds an extra allocation per instance, defeats
        // serialization, and is rarely meaningful (a nullable field expresses the same
        // intent more efficiently). Effective Java Item 55 codifies the restriction.
        ArchRule rule = noFields()
                .should().haveRawType(Optional.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
