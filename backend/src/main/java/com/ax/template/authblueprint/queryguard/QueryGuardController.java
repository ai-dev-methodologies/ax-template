package com.ax.template.authblueprint.queryguard;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * query-field-allowlist-l0 thin controller. The list endpoint accepts client-supplied
 * {@code sort} / {@code direction} / {@code filter} query params and delegates to
 * {@link QueryGuardService}, which bounds them by the resource's {@link QueryFieldAllowlist}
 * before any query is built. The controller NEVER constructs a {@code Sort} or predicate from
 * a raw request param itself — it forwards the raw tokens to the service's allowlist gate.
 * Domain rejections surface as RFC 9457 ProblemDetail (422 with a machine-readable {@code code}).
 */
@RestController
public class QueryGuardController {

    public record CreateReq(@NotBlank @Size(max = 200) String name,
                            @NotNull CatalogItemStatus status,
                            @NotNull @PositiveOrZero Long priceMinor,
                            @Size(max = 500) String internalNotes) {}

    private final QueryGuardService service;

    public QueryGuardController(QueryGuardService service) {
        this.service = service;
    }

    /** Seed a CatalogItem (the rows the list pages over). */
    @PostMapping("/api/query-guard/items")
    public ResponseEntity<QueryGuardService.CatalogItemDto> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            QueryGuardService.CatalogItemDto.of(
                service.create(req.name(), req.status(), req.priceMinor(), req.internalNotes())));
    }

    /**
     * QUERY-ALLOWLIST-SORT/FILTER/MAPPING/PAGE/KEYSTONE-001 — list CatalogItems. {@code sort}
     * names a PUBLIC field; {@code direction} is asc/desc; {@code filter} is {@code field:op:value}.
     * Any field outside the resource's allowlist → 422 naming the offending field.
     */
    @GetMapping("/api/query-guard/items")
    public PageEnvelope<QueryGuardService.CatalogItemDto> list(
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(sort, direction, filter, page, size);
    }

    @ExceptionHandler(QueryGuardException.class)
    public ResponseEntity<ProblemDetail> handle(QueryGuardException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
