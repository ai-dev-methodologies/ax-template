package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-BUILD-003")
class BuildJavaToolchainExplicitTest {

    @Test
    void practices_BUILD_003_buildGradleDeclaresExplicitJavaToolchain() throws Exception {
        Path script = Path.of(System.getProperty("user.dir"), "build.gradle.kts");
        String src = Files.readString(script);
        assertThat(src)
                .as("explicit `java { toolchain { languageVersion = ... } }` block must declare "
                        + "the JDK version so the build does not silently shift with the developer's $PATH")
                .contains("toolchain");
        assertThat(src)
                .as("the toolchain must pin a specific JavaLanguageVersion")
                .contains("JavaLanguageVersion.of(");
    }
}
