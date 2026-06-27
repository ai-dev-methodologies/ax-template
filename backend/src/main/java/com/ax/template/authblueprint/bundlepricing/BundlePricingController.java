package com.ax.template.authblueprint.bundlepricing;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.CompositeItemResponse;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.CreateCompositeItemRequest;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.PricedBundleResponse;
import com.ax.template.authblueprint.bundlepricing.BundlePricingExceptions.CompositeItemNotFoundException;
import com.ax.template.authblueprint.bundlepricing.BundlePricingExceptions.InvalidCompositeItemException;

/**
 * Thin resource controller for composite-item (bundle) pricing. Delegates to
 * {@link BundlePricingService} only — never touches a repository.
 *
 * <p>BUNDLE-AUTHZ-001: creating a composite (a catalog definition) requires ROLE_ADMIN;
 * pricing reads require a valid JWT (enforced by SecurityConfig + this @PreAuthorize).
 */
@RestController
@RequestMapping("/api/bundle-pricing")
public class BundlePricingController {

    private final BundlePricingService service;

    public BundlePricingController(BundlePricingService service) {
        this.service = service;
    }

    /** POST /api/bundle-pricing/composites — defines a composite item (ADMIN); returns 201. */
    @PostMapping("/composites")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CompositeItemResponse> create(
            @Valid @RequestBody CreateCompositeItemRequest body) {
        CompositeItemResponse resp = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/bundle-pricing/composites/" + resp.id()))
            .body(resp);
    }

    /** GET /api/bundle-pricing/composites/{id} — the composite definition. */
    @GetMapping("/composites/{id}")
    public CompositeItemResponse getDefinition(@PathVariable UUID id) {
        return service.getDefinition(id);
    }

    /** GET /api/bundle-pricing/composites/{id}/price — the conserving roll-up. */
    @GetMapping("/composites/{id}/price")
    public PricedBundleResponse price(@PathVariable UUID id) {
        return service.price(id);
    }

    @ExceptionHandler(CompositeItemNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(CompositeItemNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "COMPOSITE_ITEM_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidCompositeItemException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(InvalidCompositeItemException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_COMPOSITE_ITEM", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
