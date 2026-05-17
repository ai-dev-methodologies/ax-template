package com.ax.template.authblueprint.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.providers")
public record ProviderFlagsProperties(
    boolean emailEnabled,
    boolean googleEnabled,
    boolean kakaoEnabled
) {
}
