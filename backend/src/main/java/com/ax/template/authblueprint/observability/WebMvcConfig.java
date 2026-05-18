package com.ax.template.authblueprint.observability;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration — registers the {@link MdcCorrelationIdInterceptor} for
 * all incoming requests.
 *
 * <p>The interceptor is applied to all path patterns ({@code /**}) so that
 * every request (including Actuator endpoints) receives a correlation id.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MdcCorrelationIdInterceptor mdcCorrelationIdInterceptor;

    public WebMvcConfig(MdcCorrelationIdInterceptor mdcCorrelationIdInterceptor) {
        this.mdcCorrelationIdInterceptor = mdcCorrelationIdInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(mdcCorrelationIdInterceptor)
                .addPathPatterns("/**");
    }
}
