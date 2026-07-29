/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagAdminController
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: bfla-privileged-endpoint-authz-presence.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Security Reference — @PreAuthorize and Method Security"
 *     url: "https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html"
 *   - source_type: external
 *     citation: "Spring Data Web Support — Pageable resolution from request parameters"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.basic"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All endpoints require ROLE_ADMIN (FF-AUTHZ-001, FF-AUTHZ-002).
 *   SecurityConfig must also map /api/v1/admin/** to hasRole('ADMIN').
 */
package com.example.app.featureflags;

import com.example.app.featureflags.FeatureFlagDto.CreateRequest;
import com.example.app.featureflags.FeatureFlagDto.FlagResponse;
import com.example.app.featureflags.FeatureFlagDto.UpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for feature flag CRUD.
 *
 * <p>All endpoints require ROLE_ADMIN. Delegates to FeatureFlagService.
 *
 * <p>Endpoints (operationId → HTTP):
 * <ul>
 *   <li>listFeatureFlags    → GET  /api/v1/admin/feature-flags
 *   <li>createFeatureFlag   → POST /api/v1/admin/feature-flags
 *   <li>updateFeatureFlag   → PATCH /api/v1/admin/feature-flags/{name}
 *   <li>deleteFeatureFlag   → DELETE /api/v1/admin/feature-flags/{name}
 * </ul>
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml (FF-AUTHZ-001..FF-AUTHZ-002, FF-CRUD-001..FF-CRUD-004)
 */
@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@PreAuthorize("hasRole('ADMIN')")
public class FeatureFlagAdminController {

    private final FeatureFlagService service;

    public FeatureFlagAdminController(FeatureFlagService service) {
        this.service = service;
    }

    /** operationId: listFeatureFlags */
    @GetMapping
    public ResponseEntity<Page<FlagResponse>> list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    /** operationId: createFeatureFlag */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FlagResponse> create(@Valid @RequestBody CreateRequest req) {
        FlagResponse created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** operationId: updateFeatureFlag */
    @PatchMapping("/{name}")
    public ResponseEntity<FlagResponse> update(
            @PathVariable String name,
            @Valid @RequestBody UpdateRequest req) {
        return ResponseEntity.ok(service.update(name, req));
    }

    /** operationId: deleteFeatureFlag */
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable String name) {
        service.delete(name);
        return ResponseEntity.noContent().build();
    }
}
