package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-SECURITY-001")
class SecurityStatelessSessionTest {

    @Test
    void practices_SECURITY_001_securityConfigUsesStatelessSessionPolicy() throws Exception {
        // JWT / bearer-token APIs MUST be stateless. Session creation policy STATELESS
        // tells Spring Security never to create an HttpSession — without it a successful
        // authentication produces a JSESSIONID cookie that the API never agreed to issue,
        // every request carries that cookie back, and CSRF semantics shift from
        // "ignore for /api" to "must defend the cookie".
        Path config = Path.of(System.getProperty("user.dir"),
                "src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java");
        String src = Files.readString(config);
        assertThat(src)
                .as("SecurityConfig must set SessionCreationPolicy.STATELESS for a JWT-style API")
                .contains("SessionCreationPolicy.STATELESS");
    }
}
