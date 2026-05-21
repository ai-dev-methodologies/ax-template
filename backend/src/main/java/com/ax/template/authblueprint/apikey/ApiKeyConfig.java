package com.ax.template.authblueprint.apikey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
public class ApiKeyConfig {

    /**
     * Suppress auto-registration of {@link ApiKeyAuthenticationFilter} as a generic
     * servlet filter. The filter MUST run only inside the Spring Security chain
     * (registered via {@code addFilterAfter(BearerTokenAuthenticationFilter.class)}
     * in SecurityConfig). Without this, Spring Boot would auto-register the
     * {@code @Component} filter via {@link FilterRegistrationBean} and it would
     * execute BEFORE Spring Security's {@code SecurityContextHolderFilter},
     * which then overwrites the {@code SecurityContext} we just populated.
     */
    @Bean
    FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthFilterRegistration(ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
