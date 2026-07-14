package com.ax.template.authblueprint.facetcount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * facet-count-l0 thin controller. The facet field ({@code field} query param) is forwarded
 * VERBATIM to {@link FacetCountService#facets}, which gates it through the allowlist — the
 * controller never resolves or validates it itself. The item's caller is ALWAYS the
 * authenticated principal (caller-authentication-only-no-userid-param), never a path/body param.
 */
@RestController
public class FacetCountController {

    public record CreateReq(@NotBlank @Size(max = 100) String category, @NotNull ItemStatus status) {}

    public record ItemDto(UUID id, String category, ItemStatus status, Instant createdAt) {
        static ItemDto of(FacetableItem item) {
            return new ItemDto(item.getId(), item.getCategory(), item.getStatus(), item.getCreatedAt());
        }
    }

    private final FacetCountService service;

    public FacetCountController(FacetCountService service) {
        this.service = service;
    }

    @PostMapping("/api/facet-count/items")
    public ResponseEntity<ItemDto> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ItemDto.of(service.create(auth.getName(), req.category(), req.status())));
    }

    /**
     * FACET-COUNT/ALLOWLIST/BOUND-001..003 — bucket counts for {@code field}, scoped to the
     * caller's OWN items. A non-allowlisted field → 422 naming the offending field.
     */
    @GetMapping("/api/facet-count/items/facets")
    public FacetCountService.FacetCountResponse facets(@RequestParam("field") String field, Authentication auth) {
        return service.facets(auth.getName(), field);
    }

    @ExceptionHandler(FacetCountException.class)
    public ResponseEntity<ProblemDetail> handle(FacetCountException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
