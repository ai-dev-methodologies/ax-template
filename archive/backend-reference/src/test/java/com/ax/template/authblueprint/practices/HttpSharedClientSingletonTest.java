package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-HTTP-003")
class HttpSharedClientSingletonTest {

    @Autowired
    private RestClient practicesHttpClient;

    @Autowired
    private RestClient againPracticesHttpClient;

    @Test
    void practices_HTTP_003_restClientBeanIsApplicationSingleton() {
        assertThat(practicesHttpClient)
                .as("the RestClient @Bean must be injectable")
                .isNotNull();
        // Spring's default singleton scope must produce the same instance on every injection.
        // A controller that creates a new RestClient per call discards the connection pool,
        // forfeits the timeout configuration, and adds GC pressure under load.
        assertThat(practicesHttpClient)
                .as("two injections of the practicesHttpClient bean must reference the same instance")
                .isSameAs(againPracticesHttpClient);
    }
}
