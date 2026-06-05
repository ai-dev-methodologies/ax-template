package com.ax.template.authblueprint.apiversioning;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * api-versioning-l0 reference workload — a thin, public demo surface that lets RestAssured drive every
 * negotiation / default / compatibility / deprecation case over black-box HTTP. The catalog-default
 * strategy is {@code url-path}: the major version is the first path segment under the demo prefix
 * ({@code /v1/...}, {@code /v2/...}), which keeps the surface PRACTICES-API-003-compliant.
 *
 * <p>The widget resource demonstrates VERSION-COMPATIBILITY-001 (SemVer additive-only within a major):
 * v1 returns its ORIGINAL required field set {@code {id, name}}; v2 adds the optional {@code tags}
 * field WITHOUT removing/renaming/retyping any v1 field — a tolerant-reader v1 client is unaffected.
 *
 * <ul>
 *   <li>GET /v{N}/widgets/{id} — explicit version request (NEGOTIATION + COMPATIBILITY + DEPRECATION);</li>
 *   <li>GET /widgets/{id} — NO version selector → the default policy resolves it (DEFAULT-001);</li>
 *   <li>GET /api/versions — the public version-discovery document (DISCOVERY-001, separate controller).</li>
 * </ul>
 *
 * <p>Spec: specs/api-versioning-l0.yaml.
 */
@RestController
@RequestMapping("/api/api-versioning-demo")
public class ApiVersioningDemoController {

    private final ApiVersionResolver resolver;

    public ApiVersioningDemoController(ApiVersionResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Explicit url-path version request. The version segment is captured as a path variable so a
     * malformed token (e.g. {@code /vX/}) and an unsupported-but-valid token (e.g. {@code /v9/}) both
     * reach {@link ApiVersionResolver} rather than 404-ing at routing (the spec distinguishes a
     * malformed 400 from an unsupported 404).
     */
    @GetMapping("/{version}/widgets/{id}")
    public ResponseEntity<Map<String, Object>> versionedWidget(
            @PathVariable String version,
            @PathVariable String id,
            @RequestHeader(value = "X-API-Version", required = false) String headerVersion) {
        ApiVersionCatalog.Version v = resolver.resolve(version, headerVersion);
        return ResponseEntity.ok().headers(resolver.responseHeaders(v)).body(widgetBody(v, id));
    }

    /**
     * No version selector → VERSION-DEFAULT-001. The resolved version is ALWAYS surfaced via the
     * {@code X-API-Version} response header AND echoed in the body's {@code apiVersion} field so the
     * client can observe which version it got.
     */
    @GetMapping("/widgets/{id}")
    public ResponseEntity<Map<String, Object>> defaultWidget(
            @PathVariable String id,
            @RequestHeader(value = "X-API-Version", required = false) String headerVersion) {
        ApiVersionCatalog.Version v = resolver.resolve(null, headerVersion);
        return ResponseEntity.ok().headers(resolver.responseHeaders(v)).body(widgetBody(v, id));
    }

    /**
     * The versioned resource representation. v1 = original required set {@code {id, name}}; v2 adds the
     * optional {@code tags} field (additive — VERSION-COMPATIBILITY-001). {@code apiVersion} is the
     * always-present resolved-version echo (VERSION-DEFAULT-001).
     */
    private Map<String, Object> widgetBody(ApiVersionCatalog.Version v, String id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("name", "widget-" + id);
        body.put("apiVersion", v.label());
        if (v.major() >= 2) {
            body.put("tags", java.util.List.of("a", "b")); // additive field introduced in v2
        }
        return body;
    }
}
