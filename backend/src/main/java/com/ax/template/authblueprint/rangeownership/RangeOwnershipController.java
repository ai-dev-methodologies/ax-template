package com.ax.template.authblueprint.rangeownership;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * range-ownership-l0 thin controller. The acting principal is ALWAYS the authenticated caller.
 * Delegates to {@link RangeOwnershipService}.
 */
@RestController
public class RangeOwnershipController {

    public record RegisterBlockReq(@NotBlank @Size(max = 200) String ownerRef,
                                   @NotNull Long rangeStart, @NotNull Long rangeEnd) {}
    public record AssignReq(@NotNull Long identifierValue, @NotBlank @Size(max = 200) String ownerRef) {}
    public record PortReq(@NotBlank @Size(max = 200) String toOwner, @NotBlank String reason) {}

    public record BlockDto(UUID id, String ownerRef, long rangeStart, long rangeEnd, Long version) {
        static BlockDto of(RangeBlock b) {
            return new BlockDto(b.getId(), b.getOwnerRef(), b.getRangeStart(), b.getRangeEnd(), b.getVersion());
        }
    }
    public record EventDto(UUID id, String fromOwner, String toOwner, String reason, Instant occurredAt) {
        static EventDto of(OwnershipEvent e) {
            return new EventDto(e.getId(), e.getFromOwner(), e.getToOwner(), e.getReason(), e.getOccurredAt());
        }
    }
    public record CurrentOwnerDto(long identifierValue, String currentOwner) {}

    private final RangeOwnershipService service;

    public RangeOwnershipController(RangeOwnershipService service) {
        this.service = service;
    }

    @PostMapping("/api/range-ownership/blocks")
    public ResponseEntity<BlockDto> registerBlock(@Valid @RequestBody RegisterBlockReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BlockDto.of(
            service.registerBlock(req.ownerRef(), req.rangeStart(), req.rangeEnd())));
    }

    /** RNG-CONTAINMENT-001 — fail-closed: the identifier must fall inside a block the owner owns. */
    @PostMapping("/api/range-ownership/assignments")
    public ResponseEntity<CurrentOwnerDto> assign(@Valid @RequestBody AssignReq req, Authentication auth) {
        service.assign(req.identifierValue(), req.ownerRef(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CurrentOwnerDto(req.identifierValue(), req.ownerRef()));
    }

    /** RNG-PORT-003 — re-validates the destination owner's standing (a recognized plan participant); appends an immutable event. */
    @PostMapping("/api/range-ownership/assignments/{identifierValue}/port")
    public ResponseEntity<EventDto> port(@PathVariable long identifierValue, @Valid @RequestBody PortReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EventDto.of(service.port(identifierValue, req.toOwner(), req.reason(), auth.getName())));
    }

    @GetMapping("/api/range-ownership/assignments/{identifierValue}")
    public CurrentOwnerDto currentOwner(@PathVariable long identifierValue) {
        return new CurrentOwnerDto(identifierValue, service.currentOwner(identifierValue));
    }

    @GetMapping("/api/range-ownership/assignments/{identifierValue}/history")
    public List<EventDto> history(@PathVariable long identifierValue) {
        return service.history(identifierValue).stream().map(EventDto::of).toList();
    }

    @ExceptionHandler(RangeOwnershipException.class)
    public ResponseEntity<ProblemDetail> handle(RangeOwnershipException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
