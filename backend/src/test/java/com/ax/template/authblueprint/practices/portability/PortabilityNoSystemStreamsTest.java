package com.ax.template.authblueprint.practices.portability;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PORTABILITY")
@Tag("PORTABILITY-QUALITY-001")
class PortabilityNoSystemStreamsTest {

    // Source rule: PRACTICES-QUALITY-003 quality-no-system-streams. Production code must
    // not call System.out.* / System.err.* — those bypass the logging framework and the
    // app's configured appenders. The rule is universal: System is a JDK class, the
    // ArchUnit pattern works against any compiled bytecode.

    @Test
    void portability_QUALITY_001_petclinic_doesNotCallSystemStreams() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.PETCLINIC, PortabilityFixtures.PETCLINIC_CLASSES);
        ArchRule rule = noClasses()
                .should().callMethod(System.class, "out")
                .orShould().callMethod(System.class, "err")
                .orShould().accessField(System.class, "out")
                .orShould().accessField(System.class, "err")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void portability_QUALITY_001_realworld_doesNotCallSystemStreams() {
        JavaClasses classes = PortabilityFixtures.importFixture(
                PortabilityFixtures.REALWORLD, PortabilityFixtures.REALWORLD_CLASSES);
        ArchRule rule = noClasses()
                .should().callMethod(System.class, "out")
                .orShould().callMethod(System.class, "err")
                .orShould().accessField(System.class, "out")
                .orShould().accessField(System.class, "err")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
