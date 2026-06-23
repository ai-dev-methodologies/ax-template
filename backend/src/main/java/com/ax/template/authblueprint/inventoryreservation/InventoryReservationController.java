package com.ax.template.authblueprint.inventoryreservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * two-axis-inventory-reservation-l0 thin controller. The acting principal is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link InventoryReservationService}. AVAILABLE is returned as a DERIVED field — there is no
 * 'available' on the wire that the server stored.
 */
@RestController
public class InventoryReservationController {

    public record CreateItemReq(@NotBlank @Size(max = 200) String sku,
                                @NotNull @PositiveOrZero Long onHand) {}
    public record ReserveReq(@NotNull @Positive Long quantity) {}

    public record ItemDto(UUID id, String sku, long onHand, long reserved, long available,
                          Instant createdAt) {
        static ItemDto of(InventoryItem i) {
            return new ItemDto(i.getId(), i.getSku(), i.getOnHand(), i.getReserved(),
                i.available(), i.getCreatedAt());   // available() is DERIVED, never stored
        }
    }
    public record ReservationDto(UUID id, UUID itemId, long quantity, ReservationStatus status,
                                 String actor, Instant createdAt) {
        static ReservationDto of(InventoryReservation r) {
            return new ReservationDto(r.getId(), r.getItemId(), r.getQuantity(), r.getStatus(),
                r.getActor(), r.getCreatedAt());
        }
    }

    private final InventoryReservationService service;

    public InventoryReservationController(InventoryReservationService service) {
        this.service = service;
    }

    @PostMapping("/api/inventory-reservation/items")
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody CreateItemReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ItemDto.of(service.createItem(req.sku(), req.onHand())));
    }

    @GetMapping("/api/inventory-reservation/items/{id}")
    public ItemDto getItem(@PathVariable UUID id) {
        return ItemDto.of(service.getItem(id));
    }

    /** INVRES-CONSERVE-001 — the live conservation projection: reserved == Σ(HELD quantities). */
    @GetMapping("/api/inventory-reservation/items/{id}/held-sum")
    public long heldSum(@PathVariable UUID id) {
        return service.sumHeldQuantity(id);
    }

    /** INVRES-RESERVE-001 — reserve q against derived available; 422 if available < q. */
    @PostMapping("/api/inventory-reservation/items/{id}/reservations")
    public ResponseEntity<ReservationDto> reserve(@PathVariable UUID id, @Valid @RequestBody ReserveReq req,
                                                  Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReservationDto.of(service.reserve(id, req.quantity(), auth.getName())));
    }

    @GetMapping("/api/inventory-reservation/reservations/{id}")
    public ReservationDto getReservation(@PathVariable UUID id) {
        return ReservationDto.of(service.getReservation(id));
    }

    @GetMapping("/api/inventory-reservation/items/{id}/reservations")
    public List<ReservationDto> reservations(@PathVariable UUID id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return service.reservations(id, page, size).stream().map(ReservationDto::of).toList();
    }

    /** INVRES-COMMIT-001 — commit a HELD reservation: onHand −= q AND reserved −= q. 409 if not HELD. */
    @PostMapping("/api/inventory-reservation/reservations/{id}/commit")
    public ReservationDto commit(@PathVariable UUID id) {
        return ReservationDto.of(service.commit(id));
    }

    /** INVRES-RELEASE-001 — release a HELD reservation: reserved −= q (onHand untouched). 409 if not HELD. */
    @PostMapping("/api/inventory-reservation/reservations/{id}/release")
    public ReservationDto release(@PathVariable UUID id) {
        return ReservationDto.of(service.release(id));
    }

    @ExceptionHandler(InventoryReservationException.class)
    public ResponseEntity<ProblemDetail> handle(InventoryReservationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
