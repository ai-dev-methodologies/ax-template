package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-BUILD-001")
class BuildSpringBootBomTest {

    @Test
    void practices_BUILD_001_buildGradleAppliesSpringBootDependencyManagementPlugin() throws Exception {
        Path script = Path.of(System.getProperty("user.dir"), "build.gradle.kts");
        assertThat(Files.exists(script))
                .as("backend/build.gradle.kts must exist at the test working directory")
                .isTrue();
        String src = Files.readString(script);
        assertThat(src)
                .as("io.spring.dependency-management plugin must be applied so the Spring Boot BOM "
                        + "pins compatible versions across all spring-boot-starter-* dependencies")
                .contains("io.spring.dependency-management");
        assertThat(src)
                .as("the Spring Boot plugin id must be applied to pull in the BOM defaults")
                .contains("org.springframework.boot");
    }
}
