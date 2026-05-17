package com.ax.template.authblueprint.practices.portability;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assumptions;

/**
 * Shared loader for portability fixtures. ArchUnit imports the compiled bytecode of
 * spring-petclinic and spring-realworld so the same rule code can be re-targeted at
 * each fixture without changing the rule itself. Each fixture must be built first
 * (practices/evals/portability/run.sh --full) — if the build artifacts are missing,
 * the test is skipped via JUnit Assumptions rather than failing.
 */
final class PortabilityFixtures {

    static final String PETCLINIC = "spring-petclinic";
    static final String PETCLINIC_ROOT_PKG = "org.springframework.samples.petclinic";
    static final Path PETCLINIC_CLASSES = Paths.get(
            "../practices/evals/fixtures/spring-petclinic/target/classes");

    static final String REALWORLD = "spring-realworld";
    static final String REALWORLD_ROOT_PKG = "io.spring";
    static final Path REALWORLD_CLASSES = Paths.get(
            "../practices/evals/fixtures/spring-realworld/build/classes/java/main");

    static final String MODULITH = "spring-modulith-example";
    static final String MODULITH_ROOT_PKG = "de.codecentric.spring_modulith_example";
    static final Path MODULITH_CLASSES = Paths.get(
            "../practices/evals/fixtures/spring-modulith-example/target/classes");

    private PortabilityFixtures() {}

    static JavaClasses importFixture(String fixtureName, Path classesDir) {
        Assumptions.assumeTrue(
                classesDir.toFile().isDirectory(),
                "fixture " + fixtureName + " not built — run practices/evals/portability/run.sh --full first");
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPath(classesDir);
    }
}
