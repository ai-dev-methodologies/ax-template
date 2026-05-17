package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

@Tag("PRACTICES")
@Tag("PRACTICES-WEB-001")
class WebRestControllerAnnotationTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_WEB_001_classesNamedControllerCarryRestControllerAnnotation() {
        // @Controller alone returns view names — for a JSON API we need @RestController
        // (== @Controller + @ResponseBody). Catching the drift at the annotation level
        // prevents the silent regression where a developer writes @Controller, returns a
        // DTO, and gets a 404 because Spring tries to resolve a view named after the DTO.
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(RestController.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
