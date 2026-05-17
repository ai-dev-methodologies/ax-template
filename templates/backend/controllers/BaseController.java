/**
 * @ax-template-meta
 * template_id: backend/controllers/BaseController
 * layer: backend-cross-cutting
 * anchors_rule: web-rest-controller-annotation.md (PRACTICES-WEB-001)
 *               error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
 *               web-explicit-produces.md (PRACTICES-WEB-002)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "RFC 7807 Problem Details for HTTP APIs §3"
 *     url: "https://datatracker.ietf.org/doc/html/rfc7807"
 *   - source_type: external
 *     citation: "Spring Framework Reference — Annotated Controllers (@RestController)"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All REST controllers in your application should extend this class.
 *   Subclasses inherit @RestController and application/json produces.
 */
package com.example.app.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base class for all REST controllers.
 *
 * <p>Provides:
 * <ul>
 *   <li>Inherited {@code @RestController} — subclasses must NOT repeat it (archunit will catch duplicates)
 *   <li>Default {@code produces = "application/json"} enforced by the class-level annotation
 *   <li>Access to the current request's {@code correlationId} set by {@code CorrelationIdFilter}
 * </ul>
 *
 * <p>Subclass usage:
 * <pre>{@code
 * @RequestMapping(value = "/api/items", produces = "application/json")
 * public class ItemController extends BaseController {
 *     // ... handler methods
 * }
 * }</pre>
 *
 * <p>Rule reference: PRACTICES-WEB-001 (@RestController), PRACTICES-WEB-002 (explicit produces).
 */
@RestController
public abstract class BaseController {

    /**
     * Returns the X-Correlation-Id for the current request, as set by
     * {@code CorrelationIdFilter}. Never null — falls back to empty string if
     * the filter was not in the chain.
     *
     * <p>Use in response headers or error payloads to enable client-side correlation:
     * <pre>{@code
     * @GetMapping("/{id}")
     * public ResponseEntity<ItemResponse> getItem(@PathVariable Long id,
     *                                              HttpServletRequest request) {
     *     String traceId = correlationId(request);
     *     // ...
     *     return ResponseEntity.ok()
     *             .header("X-Correlation-Id", traceId)
     *             .body(response);
     * }
     * }</pre>
     */
    protected String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute("correlationId");
        return attr != null ? attr.toString() : "";
    }
}
