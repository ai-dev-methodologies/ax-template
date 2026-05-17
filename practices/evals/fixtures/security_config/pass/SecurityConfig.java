package fixtures.security_config.pass;

// FIXTURE: pass
// PATTERN: SecurityConfig extends SecurityConfigBase — PASSES PRACTICES-SECURITY-001
//          (STATELESS session policy mandated by applyBase())

import com.example.app.security.JwtAuthenticationFilter;
import com.example.app.security.SecurityConfigBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        super(jwtFilter);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CORRECT: applyBase() applies STATELESS session + CSRF + headers + JWT filter
        applyBase(http);
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll()
        );
        return http.build();
    }
}
