/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagController
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: testing-archunit-layer-boundary.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @RestController and @RequestMapping"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Public endpoint — no JWT required. SecurityConfig must permitAll() for this path.
 *   operationId: isFeatureFlagActive (contracts/feature-flags-openapi.yaml)
 */
package com.example.app.featureflags;

import com.example.app.featureflags.FeatureFlagDto.FlagActiveResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for feature flag evaluation.
 *
 * <p>Only one endpoint: GET /api/v1/feature-flags/{name}/active.
 * No authentication required — clients (browsers, Next.js middleware) can call directly.
 * Fail-closed: always returns 200 with {active: false} for unknown flags (FF-EVAL-002).
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml (FF-EVAL-001, FF-EVAL-002, FF-AUTHZ-001)
 */
@RestController
@RequestMapping("/api/v1/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    /**
     * Check if a named feature flag is active.
     *
     * <p>operationId: isFeatureFlagActive
     */
    @GetMapping("/{name}/active")
    public ResponseEntity<FlagActiveResponse> isActive(@PathVariable String name) {
        boolean active = service.isActive(name);
        return ResponseEntity.ok(new FlagActiveResponse(active));
    }
}
