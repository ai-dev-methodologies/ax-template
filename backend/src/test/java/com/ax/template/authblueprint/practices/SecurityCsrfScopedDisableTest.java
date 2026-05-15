package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-SECURITY-002")
class SecurityCsrfScopedDisableTest {

    @Test
    void practices_SECURITY_002_csrfDisabledOnlyForApiPaths_notGlobally() throws Exception {
        // CSRF protection must stay ON by default; only the bearer-token API paths bypass
        // it via ignoringRequestMatchers. A full `csrf().disable()` is an anti-pattern —
        // it weakens every browser-driven endpoint (h2-console, future server-rendered
        // pages, future form submissions).
        Path config = Path.of(System.getProperty("user.dir"),
                "src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java");
        String src = Files.readString(config);

        assertThat(src)
                .as("SecurityConfig must scope CSRF disable to specific paths via ignoringRequestMatchers")
                .contains("ignoringRequestMatchers");

        // The naive `csrf().disable()` or `csrf(csrf -> csrf.disable())` must not appear.
        assertThat(src.replaceAll("\\s+", ""))
                .as("global CSRF disable is forbidden — use ignoringRequestMatchers per path")
                .doesNotContain("csrf().disable()")
                .doesNotContain("csrf(csrf->csrf.disable())");
    }
}
