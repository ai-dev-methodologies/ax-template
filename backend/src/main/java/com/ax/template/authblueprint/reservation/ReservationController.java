package com.ax.template.authblueprint.reservation;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * reserve-settle-balance-l0 thin controller. Any authenticated caller may fund a balance and
 * reserve/settle/release against it. {@code @Digits(integer=15, fraction=4)} bounds amounts to
 * NUMERIC(19,4) at the validation boundary (an over-precise value is a 400, never a silent rescale).
 * Delegates to {@link ReservationService} ONLY.
 */
@RestController
public class ReservationController {

    public record CreateBalanceReq(@NotBlank @Size(max = 200) String scopeKey,
                                   @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal funded) {}
    // ttlSeconds is bounded (@Max 1 year) like the @Digits amount bound: an over-large TTL is a clean
    // 400 VALIDATION_FAILED, never an Instant.plusSeconds overflow that escapes as an unmapped 500/403.
    public record ReserveReq(@NotBlank @Size(max = 200) String scopeKey,
                             @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
                             @Positive @Max(31_536_000L) long ttlSeconds) {}
    public record SettleReq(@NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal actual) {}

    public record BalanceDto(String scopeKey, BigDecimal funded, BigDecimal committed, BigDecimal reserved,
                             BigDecimal available, Long version) {
        static BalanceDto of(ReservableBalance b) {
            return new BalanceDto(b.getScopeKey(), b.getFunded(), b.getCommitted(), b.getReserved(),
                b.available(), b.getVersion());
        }
    }
    public record HoldDto(UUID id, UUID balanceId, BigDecimal amount, ReservationStatus status,
                          BigDecimal settledAmount, Instant expiresAt, Instant createdAt) {
        static HoldDto of(Reservation r) {
            return new HoldDto(r.getId(), r.getBalanceId(), r.getAmount(), r.getStatus(),
                r.getSettledAmount(), r.getExpiresAt(), r.getCreatedAt());
        }
    }

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping("/api/balances")
    public ResponseEntity<BalanceDto> createBalance(@Valid @RequestBody CreateBalanceReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BalanceDto.of(service.createBalance(req.scopeKey(), req.funded())));
    }

    @GetMapping("/api/balances/{scopeKey}")
    public BalanceDto getBalance(@PathVariable String scopeKey) {
        return BalanceDto.of(service.getBalance(scopeKey));
    }

    @GetMapping("/api/balances/{scopeKey}/reservations")
    public PageEnvelope<HoldDto> listHolds(@PathVariable String scopeKey,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listHolds(scopeKey, page, size), HoldDto::of);
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<HoldDto> reserve(@Valid @RequestBody ReserveReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(HoldDto.of(service.reserve(req.scopeKey(), req.amount(), req.ttlSeconds())));
    }

    @PostMapping("/api/reservations/{holdId}/settle")
    public HoldDto settle(@PathVariable UUID holdId, @Valid @RequestBody SettleReq req) {
        return HoldDto.of(service.settle(holdId, req.actual()));
    }

    @PostMapping("/api/reservations/{holdId}/release")
    public HoldDto release(@PathVariable UUID holdId) {
        return HoldDto.of(service.release(holdId));
    }

    @GetMapping("/api/reservations/{holdId}")
    public HoldDto getHold(@PathVariable UUID holdId) {
        return HoldDto.of(service.getHold(holdId));
    }

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ProblemDetail> handle(ReservationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
