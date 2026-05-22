package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SessionManagementProperties.class)
public class SessionManagementConfig {
}
