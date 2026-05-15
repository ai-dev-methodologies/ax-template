package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-BUILD-002")
class BuildNoSnapshotDependenciesTest {

    @Test
    void practices_BUILD_002_noSnapshotDependenciesDeclared() throws Exception {
        Path script = Path.of(System.getProperty("user.dir"), "build.gradle.kts");
        String src = Files.readString(script);
        // Walk dependencies declarations and reject any -SNAPSHOT coordinate. The rule targets
        // *external* dependencies, not the project's own outgoing version. We exclude:
        //   - comment lines (begin with //)
        //   - the project's own `version = "..."` declaration (top-level, not a dependency)
        List<String> offenders = Arrays.stream(src.split("\n"))
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> !line.startsWith("version =") && !line.startsWith("version="))
                .filter(line -> line.contains("-SNAPSHOT"))
                .toList();
        assertThat(offenders)
                .as("production builds must not depend on -SNAPSHOT artifacts (lossy, racy, "
                        + "and break reproducibility)")
                .isEmpty();
    }
}
