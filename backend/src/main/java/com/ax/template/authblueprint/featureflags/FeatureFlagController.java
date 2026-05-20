package com.ax.template.authblueprint.featureflags;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public flag-evaluation endpoint.
 * <p>
 * Trace:
 * <ul>
 *   <li>FF-AUTHZ-001 — endpoint is permitAll in
 *       {@link com.ax.template.authblueprint.security.SecurityConfig}.</li>
 *   <li>FF-EVAL-001 — known flag returns its current enabled state.</li>
 *   <li>FF-EVAL-002 — unknown flag returns {@code {active: false}} (fail-closed).</li>
 *   <li>blueprints/feature-flags-manifest.yaml#eval</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    @GetMapping("/{name}/active")
    public FeatureFlagDto.Evaluation isActive(@PathVariable String name) {
        return new FeatureFlagDto.Evaluation(service.isActive(name));
    }
}
