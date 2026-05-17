package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Tag("PRACTICES")
@Tag("PRACTICES-HTTP-001")
class HttpRestClientOverRestTemplateTest {

    @Test
    void practices_HTTP_001_configReturnsRestClientNotRestTemplate() {
        // Every @Bean method in HttpClientConfig must return RestClient, not RestTemplate.
        int restClientBeans = 0;
        int restTemplateBeans = 0;
        for (Method m : HttpClientConfig.class.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            Class<?> rt = m.getReturnType();
            if (rt.equals(RestClient.class)) restClientBeans++;
            if (rt.equals(RestTemplate.class)) restTemplateBeans++;
        }
        assertThat(restClientBeans)
                .as("HttpClientConfig must declare at least one RestClient @Bean")
                .isGreaterThanOrEqualTo(1);
        assertThat(restTemplateBeans)
                .as("HttpClientConfig must NOT declare any RestTemplate @Bean (deprecated path)")
                .isZero();
    }
}
