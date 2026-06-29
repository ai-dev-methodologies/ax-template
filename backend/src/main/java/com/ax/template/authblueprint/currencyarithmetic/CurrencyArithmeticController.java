package com.ax.template.authblueprint.currencyarithmetic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
import java.util.List;
import java.util.UUID;

/**
 * Thin currency-arithmetic controller — delegates ALL arithmetic to {@link CurrencyArithmeticService}.
 * Every operation requires a valid JWT (the {@code /api/currency-ledgers/**} matcher in SecurityConfig
 * is {@code authenticated}); the fail-closed currency-tag guard lives entirely in
 * {@link CurrencyMoney}, so a cross-currency add/subtract surfaces here as a 422 CURRENCY_MISMATCH.
 */
@RestController
public class CurrencyArithmeticController {

    // ── Request / Response DTOs ──────────────────────────────────────────────────

    public record AmountDto(long minorUnits, @NotNull String currency) {}

    public record CreateLedgerReq(@NotNull String currency, long openingMinor) {}

    public record ConversionDto(@NotNull String fromCurrency, @NotNull String toCurrency, long convertedMinorUnits) {
        CurrencyConversion toConversion() {
            return new CurrencyConversion(fromCurrency, toCurrency, convertedMinorUnits);
        }
    }

    public record AddConvertedReq(@NotNull @Valid AmountDto amount, @NotNull @Valid ConversionDto conversion) {}

    public record ConversionRecordDto(String fromCurrency, String toCurrency,
                                      long sourceMinor, long convertedMinor, Instant recordedAt) {
        static ConversionRecordDto of(ConversionRecord r) {
            return new ConversionRecordDto(r.getFromCurrency(), r.getToCurrency(),
                r.getSourceMinor(), r.getConvertedMinor(), r.getRecordedAt());
        }
    }

    public record LedgerDto(UUID id, String currency, long balanceMinor,
                            List<ConversionRecordDto> conversions, Instant createdAt) {
        static LedgerDto of(CurrencyLedger l) {
            List<ConversionRecordDto> trail = l.getConversions().stream()
                .map(ConversionRecordDto::of).toList();
            return new LedgerDto(l.getId(), l.getCurrencyCode(), l.getBalanceMinor(), trail, l.getCreatedAt());
        }
    }

    private final CurrencyArithmeticService service;

    public CurrencyArithmeticController(CurrencyArithmeticService service) {
        this.service = service;
    }

    // ── Ledger definition ────────────────────────────────────────────────────────

    @PostMapping("/api/currency-ledgers")
    public ResponseEntity<LedgerDto> create(@Valid @RequestBody CreateLedgerReq req) {
        CurrencyLedger ledger = service.createLedger(req.currency(), req.openingMinor());
        return ResponseEntity.status(HttpStatus.CREATED).body(LedgerDto.of(ledger));
    }

    // ── Fail-closed arithmetic ─────────────────────────────────────────────────────

    @PostMapping("/api/currency-ledgers/{id}/add")
    public LedgerDto add(@PathVariable UUID id, @Valid @RequestBody AmountDto amount) {
        return LedgerDto.of(service.add(id, new CurrencyMoney(amount.minorUnits(), amount.currency())));
    }

    @PostMapping("/api/currency-ledgers/{id}/subtract")
    public LedgerDto subtract(@PathVariable UUID id, @Valid @RequestBody AmountDto amount) {
        return LedgerDto.of(service.subtract(id, new CurrencyMoney(amount.minorUnits(), amount.currency())));
    }

    @PostMapping("/api/currency-ledgers/{id}/add-converted")
    public LedgerDto addConverted(@PathVariable UUID id, @Valid @RequestBody AddConvertedReq req) {
        CurrencyMoney foreign = new CurrencyMoney(req.amount().minorUnits(), req.amount().currency());
        return LedgerDto.of(service.addConverted(id, foreign, req.conversion().toConversion()));
    }

    // ── Read ───────────────────────────────────────────────────────────────────────

    @GetMapping("/api/currency-ledgers/{id}")
    public LedgerDto get(@PathVariable UUID id) {
        return LedgerDto.of(service.getLedger(id));
    }

    // ── Exception handler ──────────────────────────────────────────────────────────

    @ExceptionHandler(CurrencyArithmeticException.class)
    public ResponseEntity<ProblemDetail> handle(CurrencyArithmeticException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }

    /**
     * A Math.*Exact overflow during same-currency arithmetic is an unprocessable monetary input,
     * not a server fault — map it to 422 rather than a raw 500.
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ProblemDetail> handleOverflow(ArithmeticException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "monetary arithmetic overflowed the representable range");
        pd.setType(URI.create("urn:problem:arithmetic-overflow"));
        pd.setProperty("code", "ARITHMETIC_OVERFLOW");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }
}
