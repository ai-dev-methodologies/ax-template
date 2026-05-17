package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

@Tag("PRACTICES")
@Tag("PRACTICES-TEST-004")
class ArchitectureRepositoriesExtendJpaTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_TEST_004_classesNamedRepositoryAreSpringDataInterfaces() {
        // A class named *Repository must be an interface that extends Spring Data's
        // JpaRepository. This catches the common drift where a developer creates a
        // hand-rolled "repository" service class that bypasses the data-access layer.
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().resideInAPackage("com.ax.template.authblueprint.practices")
                .should().beInterfaces()
                .andShould().beAssignableTo(JpaRepository.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
