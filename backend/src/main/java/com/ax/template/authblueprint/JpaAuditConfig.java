package com.ax.template.authblueprint;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Provides the {@code AuditorAware<String>} bean required by {@code @CreatedBy} and
 * {@code @LastModifiedBy} auditing on BaseEntity-extending entities.
 *
 * <p>{@code @EnableJpaAuditing} is declared on {@link AuthBlueprintBackendApplication}.
 */
@Configuration
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
