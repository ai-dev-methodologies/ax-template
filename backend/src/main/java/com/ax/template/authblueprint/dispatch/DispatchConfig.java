package com.ax.template.authblueprint.dispatch;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the dispatch domain and enables the {@code @Scheduled} timeout sweep
 * ({@link DispatchSweeper}). @EnableScheduling is idempotent across the app.
 */
@Configuration
@EnableScheduling
public class DispatchConfig {
}
