package fixtures.openapi_config.pass;

// FIXTURE: pass
// PATTERN: OpenApiConfig with @ConditionalOnMissingBean — PASSES PRACTICES-WEB-002
//          Fork receivers that declare their own OpenAPI bean will override this safely.

import com.example.app.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// CORRECT: extends OpenApiConfig which uses @ConditionalOnMissingBean(OpenAPI.class)
@Configuration
public class OpenApiConfig extends com.example.app.config.OpenApiConfig {

    // Override apiInfo() to customise for this fork:
    @Override
    protected io.swagger.v3.oas.models.info.Info apiInfo() {
        return new Info()
                .title("My App API")
                .version("2.0.0")
                .description("Domain-specific API documentation");
    }
    // The @Bean OpenAPI openAPI() from parent carries @ConditionalOnMissingBean —
    // if a sibling config declares its own OpenAPI bean, this is safely skipped.
}
