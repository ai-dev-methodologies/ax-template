package com.acme.multitenancy;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#async-propagation.prerequisite_executor_bean
 * with <root> = acme.
 *
 * Bean name "taskExecutor" is the Spring default that @Async picks up
 * without explicit qualifier. If multiple executor beans exist, use
 * @Async("name") on the method side to disambiguate; otherwise Spring
 * logs a warning and falls back to SimpleAsyncTaskExecutor — silently
 * defeating this entire propagation strategy.
 */
@Configuration
@EnableAsync
public class TenantAwareAsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ax-async-");
        executor.setTaskDecorator(new TenantContextAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
