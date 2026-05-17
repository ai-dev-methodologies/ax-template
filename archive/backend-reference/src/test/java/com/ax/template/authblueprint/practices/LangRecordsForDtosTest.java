package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-LANG-001")
class LangRecordsForDtosTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_LANG_001_dtoClassesAreRecords() {
        // *Request and *Response classes are by convention transport DTOs. Records make
        // them immutable, give them equals/hashCode/toString for free, and put the field
        // contract directly in the class header — no Lombok, no manual boilerplate.
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .should().beRecords()
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
