/**
 * @ax-template-meta
 * template_id: backend/config/OpenApiConfig
 * layer: backend-cross-cutting
 * anchors_rule: web-explicit-produces.md (PRACTICES-WEB-002)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "springdoc-openapi Reference — Auto-configuration and ConditionalOnMissingBean"
 *     url: "https://springdoc.org/#getting-started"
 *   - source_type: external
 *     citation: "Spring Boot Reference — @ConditionalOnMissingBean"
 *     url: "https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.condition-annotations.bean-conditions"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Fork receivers that want a custom OpenAPI bean simply declare their own
 *   @Bean OpenAPI openAPI() — the @ConditionalOnMissingBean ensures this
 *   template's bean is skipped.
 *   Requires: io.springdoc:springdoc-openapi-starter-webmvc-ui in build.gradle.
 *
 *   Customize the Info fields and security schemes below to match your domain.
 */
package com.example.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi auto-configuration.
 *
 * <p>Provides a default {@link OpenAPI} bean that:
 * <ul>
 *   <li>Documents the Bearer JWT security scheme
 *   <li>Populates API {@link Info} (title, version, contact, license)
 *   <li>Is conditional on no existing {@link OpenAPI} bean — fork receivers
 *       can override by declaring their own {@code @Bean OpenAPI}
 * </ul>
 *
 * <p>Verification: {@code GET /v3/api-docs} must return a valid OpenAPI schema.
 * Rule reference: PRACTICES-WEB-002 (explicit produces / content negotiation).
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Default OpenAPI bean. Skipped if the fork receiver declares its own
     * {@code @Bean OpenAPI} (Spring Boot's {@link ConditionalOnMissingBean} semantics).
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    // -------------------------------------------------------------------------
    // Overridable sub-components — override in subclass if extending
    // -------------------------------------------------------------------------

    protected Info apiInfo() {
        return new Info()
                .title("Application API")          // replace with your app name
                .version("1.0.0")
                .description("API documentation")  // replace with your description
                .contact(new Contact()
                        .name("Engineering")       // replace with team contact
                        .email("eng@example.com"))
                .license(new License()
                        .name("Proprietary"));
    }

    protected SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT access token obtained from POST /api/auth/email/login");
    }
}
