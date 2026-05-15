package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-ACTUATOR-003")
class ActuatorBuildInfoTest {

    @Test
    void practices_ACTUATOR_003_buildGradleEnablesSpringBootBuildInfo() throws Exception {
        Path script = Path.of(System.getProperty("user.dir"), "build.gradle.kts");
        String src = Files.readString(script);
        // The Spring Boot Gradle plugin's buildInfo() task generates
        // META-INF/build-info.properties, which the actuator /info endpoint then
        // surfaces as version + groupId + artifactId + build time. Without it the
        // operator has no machine-readable "what version is this" answer at runtime.
        assertThat(src)
                .as("build.gradle.kts must invoke springBoot { buildInfo() } to generate "
                        + "META-INF/build-info.properties for /actuator/info")
                .contains("springBoot")
                .contains("buildInfo");
    }

    @Test
    void practices_ACTUATOR_003_applicationYamlEnablesInfoBuildContribution() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
        if (!Files.exists(yaml)) {
            return;
        }
        String src = Files.readString(yaml);
        assertThat(src)
                .as("application.yml must include management.info.build.enabled to surface buildInfo")
                .contains("info:")
                .contains("build:")
                .contains("enabled: true");
    }
}
