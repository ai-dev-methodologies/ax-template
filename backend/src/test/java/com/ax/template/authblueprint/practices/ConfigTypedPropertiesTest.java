package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Tag("PRACTICES")
@Tag("PRACTICES-CONFIG-001")
class ConfigTypedPropertiesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_CONFIG_001_noFieldUsesAtValueInjection() {
        // @Value("${...}") field injection is scattered, untyped, and produces one binding
        // site per field. The typed @ConfigurationProperties record collects the contract
        // in one place and is immutable + IDE-refactorable.
        ArchRule rule = noFields()
                .should().beAnnotatedWith(Value.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void practices_CONFIG_001_practicesAppPropertiesIsImmutableConfigurationRecord() {
        // Reflective verification of the fixture: it must be a record annotated with
        // @ConfigurationProperties("practices.app").
        Class<PracticesAppProperties> type = PracticesAppProperties.class;
        Assertions.assertThat(type.isRecord())
                .as("ConfigurationProperties must be modelled as a record (immutable contract)")
                .isTrue();
        ConfigurationProperties ann = type.getAnnotation(ConfigurationProperties.class);
        Assertions.assertThat(ann)
                .as("the fixture must carry @ConfigurationProperties to be bound")
                .isNotNull();
        Assertions.assertThat(ann.value())
                .as("@ConfigurationProperties value must declare an explicit namespace")
                .isNotBlank();
    }
}
