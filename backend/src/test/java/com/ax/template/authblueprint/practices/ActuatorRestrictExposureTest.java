package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-ACTUATOR-002")
class ActuatorRestrictExposureTest {

    private static final String[] SENSITIVE_ENDPOINTS = {
            "env", "beans", "heapdump", "threaddump", "loggers", "configprops",
            "metrics", "shutdown"
    };

    @Test
    void practices_ACTUATOR_002_webExposureIncludeMustNotContainSensitiveEndpoints() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
        if (!Files.exists(yaml)) {
            return;
        }
        String src = Files.readString(yaml);

        // The application MUST declare management.endpoints.web.exposure.include — the
        // Spring Boot default is just `health,info`, but leaving it implicit means the
        // first developer to add `management.endpoints.web.exposure.include: '*'` for
        // a debugging session ships sensitive endpoints to production unnoticed.
        assertThat(src)
                .as("application.yml must explicitly declare management.endpoints.web.exposure.include")
                .contains("management:")
                .contains("exposure:")
                .contains("include:");

        // Find the include line and assert no sensitive endpoint appears in it.
        String includeLine = src.lines()
                .filter(l -> l.trim().startsWith("include:"))
                .findFirst()
                .orElseThrow();
        String includeValue = includeLine.split(":", 2)[1].trim().toLowerCase();
        // Allow '*' only if accompanied by an explicit `exclude:` containing every
        // sensitive endpoint — the simpler-and-safer pattern is an explicit allow-list.
        assertThat(includeValue)
                .as("include value should be an explicit allow-list, not '*' wildcard")
                .doesNotContain("'*'")
                .doesNotContain("\"*\"")
                .doesNotContain("*,");
        for (String sensitive : SENSITIVE_ENDPOINTS) {
            assertThat(includeValue)
                    .as("management.endpoints.web.exposure.include must not expose '%s'", sensitive)
                    .doesNotContain(sensitive);
        }
    }
}
