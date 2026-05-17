package com.ax.template.authblueprint.practices;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Fixtures for PRACTICES-HTTP-001 / 002 / 003.
 * Spring 6.1+ RestClient — single application-scoped instance with explicit connect /
 * read timeouts. The configuration intentionally avoids the deprecated RestTemplate and
 * documents the timeouts in code so they survive the next refactor.
 */
@Configuration
public class HttpClientConfig {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    public static final String BASE_URL = "https://example.invalid";

    @Bean
    public RestClient practicesHttpClient() {
        return buildClient(CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /** Exposed for tests so we can assert the timeouts are wired without reflection. */
    public static RestClient buildClient(Duration connect, Duration read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connect.toMillis());
        factory.setReadTimeout((int) read.toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(BASE_URL)
                .build();
    }
}
