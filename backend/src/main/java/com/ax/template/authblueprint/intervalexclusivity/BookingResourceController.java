package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** interval-exclusivity-l0 thin controller for the {@link BookingResource} resource. */
@RestController
public class BookingResourceController {

    public record RegisterReq(@NotBlank @Size(max = 200) String resourceKey) {}
    public record ResourceDto(UUID id, String resourceKey, Instant createdAt) {
        static ResourceDto of(BookingResource r) { return new ResourceDto(r.getId(), r.getResourceKey(), r.getCreatedAt()); }
    }

    private final BookingResourceService service;

    public BookingResourceController(BookingResourceService service) {
        this.service = service;
    }

    @PostMapping("/api/interval-exclusivity/resources")
    public ResponseEntity<ResourceDto> register(@Valid @RequestBody RegisterReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceDto.of(service.register(req.resourceKey())));
    }

    @GetMapping("/api/interval-exclusivity/resources/{resourceKey}")
    public ResourceDto get(@PathVariable String resourceKey) {
        return ResourceDto.of(service.get(resourceKey));
    }

    @ExceptionHandler(IntervalExclusivityException.class)
    public ResponseEntity<ProblemDetail> handle(IntervalExclusivityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
