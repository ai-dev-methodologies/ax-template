package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

/** interval-exclusivity-l0 thin controller for the {@link Booking} resource. */
@RestController
public class BookingController {

    public record BookReq(@NotBlank @Size(max = 200) String resourceKey, @NotNull Instant startAt, @NotNull Instant endAt) {}
    public record ResizeReq(@NotNull Instant startAt, @NotNull Instant endAt) {}

    public record BookingDto(UUID id, String resourceKey, Instant startAt, Instant endAt, BookingStatus status) {
        static BookingDto of(Booking b) {
            return new BookingDto(b.getId(), b.getResourceKey(), b.getStartAt(), b.getEndAt(), b.getStatus());
        }
    }

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /** IVX-OVERLAP-001 — book [startAt, endAt); 409 IVX_OVERLAP if it overlaps an active booking. */
    @PostMapping("/api/interval-exclusivity/bookings")
    public ResponseEntity<BookingDto> book(@Valid @RequestBody BookReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BookingDto.of(service.book(req.resourceKey(), req.startAt(), req.endAt())));
    }

    /** IVX-MUTATE-003 — shrink unconditional; extend re-validates overlap atomically. */
    @PutMapping("/api/interval-exclusivity/bookings/{id}")
    public BookingDto resize(@PathVariable UUID id, @Valid @RequestBody ResizeReq req) {
        return BookingDto.of(service.resize(id, req.startAt(), req.endAt()));
    }

    /** IVX-MUTATE-003 — cancel frees the window immediately for new bookings. */
    @PostMapping("/api/interval-exclusivity/bookings/{id}/cancel")
    public BookingDto cancel(@PathVariable UUID id) {
        return BookingDto.of(service.cancel(id));
    }

    @GetMapping("/api/interval-exclusivity/bookings/{id}")
    public BookingDto get(@PathVariable UUID id) {
        return BookingDto.of(service.get(id));
    }

    @GetMapping("/api/interval-exclusivity/resources/{resourceKey}/bookings")
    public List<BookingDto> list(@PathVariable String resourceKey) {
        return service.list(resourceKey).stream().map(BookingDto::of).toList();
    }

    @ExceptionHandler(IntervalExclusivityException.class)
    public ResponseEntity<ProblemDetail> handle(IntervalExclusivityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
