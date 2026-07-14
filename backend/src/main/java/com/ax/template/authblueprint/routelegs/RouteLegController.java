package com.ax.template.authblueprint.routelegs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * route-leg-contiguity-l0 thin controller. Delegates to {@link RouteLegService}.
 */
@RestController
public class RouteLegController {

    public record AppendReq(@NotBlank @Size(max = 200) String originCode,
                            @NotBlank @Size(max = 200) String destCode) {}
    public record InsertReq(@Min(1) int atOrdinal,
                            @NotBlank @Size(max = 200) String originCode,
                            @NotBlank @Size(max = 200) String destCode) {}
    public record ReplaceReq(@NotBlank @Size(max = 200) String originCode,
                             @NotBlank @Size(max = 200) String destCode) {}
    public record ReorderReq(@NotNull @NotEmpty List<UUID> legIds) {}

    public record LegDto(UUID id, int ordinal, String originCode, String destCode) {
        static LegDto of(RouteLeg l) {
            return new LegDto(l.getId(), l.getOrdinal(), l.getOriginCode(), l.getDestCode());
        }
    }
    public record RouteDto(UUID id, Instant createdAt, List<LegDto> legs) {}

    private final RouteLegService service;

    public RouteLegController(RouteLegService service) {
        this.service = service;
    }

    @PostMapping("/api/route-legs/routes")
    public ResponseEntity<RouteDto> createRoute() {
        Route r = service.createRoute();
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(r.getId()));
    }

    /** LEG-SEQUENCE-001 — append a leg to the end of the route. */
    @PostMapping("/api/route-legs/routes/{routeId}/legs")
    public ResponseEntity<RouteDto> append(@PathVariable UUID routeId, @Valid @RequestBody AppendReq req) {
        service.appendLeg(routeId, req.originCode(), req.destCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(routeId));
    }

    /** LEG-MUTATE-001 — insert a leg at an arbitrary position. */
    @PostMapping("/api/route-legs/routes/{routeId}/legs/insert")
    public ResponseEntity<RouteDto> insert(@PathVariable UUID routeId, @Valid @RequestBody InsertReq req) {
        service.insertLegAt(routeId, req.atOrdinal(), req.originCode(), req.destCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(routeId));
    }

    /** LEG-MUTATE-001 — remove a leg by ordinal. */
    @DeleteMapping("/api/route-legs/routes/{routeId}/legs/{ordinal}")
    public RouteDto remove(@PathVariable UUID routeId, @PathVariable int ordinal) {
        service.removeLeg(routeId, ordinal);
        return toDto(routeId);
    }

    /** LEG-MUTATE-001 — replace a leg's origin/dest in place. */
    @PutMapping("/api/route-legs/routes/{routeId}/legs/{ordinal}")
    public RouteDto replace(@PathVariable UUID routeId, @PathVariable int ordinal, @Valid @RequestBody ReplaceReq req) {
        service.replaceLeg(routeId, ordinal, req.originCode(), req.destCode());
        return toDto(routeId);
    }

    /** LEG-GAP-001 — reorder the route's legs into a new permutation, atomically. */
    @PutMapping("/api/route-legs/routes/{routeId}/legs/reorder")
    public RouteDto reorder(@PathVariable UUID routeId, @Valid @RequestBody ReorderReq req) {
        service.reorderLegs(routeId, req.legIds());
        return toDto(routeId);
    }

    @GetMapping("/api/route-legs/routes/{routeId}")
    public RouteDto get(@PathVariable UUID routeId) {
        return toDto(routeId);
    }

    private RouteDto toDto(UUID routeId) {
        Route r = service.get(routeId);
        List<LegDto> legs = service.legs(routeId).stream().map(LegDto::of).toList();
        return new RouteDto(r.getId(), r.getCreatedAt(), legs);
    }

    @ExceptionHandler(RouteLegException.class)
    public ResponseEntity<ProblemDetail> handle(RouteLegException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
