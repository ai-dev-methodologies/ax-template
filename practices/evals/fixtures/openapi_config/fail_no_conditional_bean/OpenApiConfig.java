package fixtures.openapi_config.fail_no_conditional_bean;

// FIXTURE: fail_no_conditional_bean
// EXPECTED_VIOLATION: OPENAPI_BEAN_NOT_CONDITIONAL_ON_MISSING_BEAN
// RULE: web-explicit-produces.md (PRACTICES-WEB-002)
//
// This config violates the rule: it declares an OpenAPI bean without
// @ConditionalOnMissingBean(OpenAPI.class). Fork receivers that provide their
// own OpenAPI configuration will get a duplicate-bean error at startup.
// A guard asserting the @Bean method carries @ConditionalOnMissingBean will
// FAIL against this fixture.

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // VIOLATION: no @ConditionalOnMissingBean — conflicts with fork-receiver custom config
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("App API")
                        .version("1.0.0"));
    }
}
