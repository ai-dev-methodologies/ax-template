package com.ax.template.authblueprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableJpaAuditing
public class AuthBlueprintBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthBlueprintBackendApplication.class, args);
    }
}
