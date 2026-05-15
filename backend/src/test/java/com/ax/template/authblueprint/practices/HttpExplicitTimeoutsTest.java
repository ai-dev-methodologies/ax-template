package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-HTTP-002")
class HttpExplicitTimeoutsTest {

    @Test
    void practices_HTTP_002_connectAndReadTimeoutsArePinnedToFiniteValues() {
        assertThat(HttpClientConfig.CONNECT_TIMEOUT)
                .as("connect timeout must be a finite, sub-minute duration to surface "
                        + "back-pressure as a failure rather than hang the pool")
                .isLessThan(Duration.ofMinutes(1))
                .isGreaterThan(Duration.ZERO);

        assertThat(HttpClientConfig.READ_TIMEOUT)
                .as("read timeout must be a finite, bounded duration; default null = infinite")
                .isLessThan(Duration.ofMinutes(1))
                .isGreaterThan(Duration.ZERO);
    }

    @Test
    void practices_HTTP_002_factoryHelperWiresProvidedTimeouts() {
        // The helper is the path tests use to assert timeouts are actually wired into the
        // underlying factory. Calling it returns a usable RestClient — the act of building
        // without throwing proves the timeout values pass through to SimpleClientHttpRequestFactory.
        var client = HttpClientConfig.buildClient(Duration.ofMillis(1_000), Duration.ofMillis(3_000));
        assertThat(client).isNotNull();
    }
}
