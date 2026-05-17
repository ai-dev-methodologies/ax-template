package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-CONFIG-003")
class ConfigProfileIsolationTest {

    /**
     * application.yml must either avoid environment-specific config entirely, OR isolate
     * each environment in its own application-{profile}.yml file. The anti-pattern is to
     * stuff every environment's keys into one yaml file behind {@code spring.profiles.active}
     * conditionals — readers cannot tell which keys apply where, and a stale conditional
     * silently leaks dev/staging behaviour into prod.
     */
    @Test
    void practices_CONFIG_003_baseYamlDoesNotEmbedProfileSpecificBlocks() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
        if (!Files.exists(yaml)) {
            return;
        }
        String body = Files.readString(yaml);
        // Block-style profile gating using `spring.config.activate.on-profile` or the
        // legacy `spring.profiles:` key inside application.yml is the anti-pattern this
        // rule targets. Profile-specific config belongs in application-{profile}.yml.
        assertThat(body)
                .as("application.yml must not embed profile-gated blocks; move them to "
                        + "application-{profile}.yml")
                .doesNotContain("spring.config.activate.on-profile")
                .doesNotContain("on-profile:");
    }
}
