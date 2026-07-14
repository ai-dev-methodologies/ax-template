package com.ax.template.authblueprint.saturatingbalance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * saturating-balance-l0 thin controller. The balance owner is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). Delegates entirely to {@link SaturatingBalanceService}.
 */
@RestController
public class SaturatingBalanceController {

    public record CreateReq(@NotNull @Digits(integer = 11, fraction = 4) BigDecimal cap) {}
    public record AmountReq(@NotNull @Digits(integer = 11, fraction = 4) BigDecimal amount) {}

    public record BalanceDto(UUID id, String ownerId, BigDecimal cap, BigDecimal current) {
        static BalanceDto of(Balance b) {
            return new BalanceDto(b.getId(), b.getOwnerId(), b.getCap(), b.getCurrent());
        }
    }
    public record LedgerEntryDto(UUID id, LedgerOp op, BigDecimal requestedAmount, BigDecimal appliedAmount,
                                 Instant occurredAt) {
        static LedgerEntryDto of(LedgerEntry e) {
            return new LedgerEntryDto(e.getId(), e.getOp(), e.getRequestedAmount(), e.getAppliedAmount(),
                e.getOccurredAt());
        }
    }

    private final SaturatingBalanceService service;

    public SaturatingBalanceController(SaturatingBalanceService service) {
        this.service = service;
    }

    @PostMapping("/api/saturating-balances")
    public ResponseEntity<BalanceDto> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BalanceDto.of(service.create(auth.getName(), req.cap())));
    }

    @PostMapping("/api/saturating-balances/{id}/accrue")
    public ResponseEntity<LedgerEntryDto> accrue(@PathVariable UUID id, @Valid @RequestBody AmountReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(LedgerEntryDto.of(service.accrue(id, req.amount())));
    }

    @PostMapping("/api/saturating-balances/{id}/debit")
    public ResponseEntity<LedgerEntryDto> debit(@PathVariable UUID id, @Valid @RequestBody AmountReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(LedgerEntryDto.of(service.debit(id, req.amount())));
    }

    @GetMapping("/api/saturating-balances/{id}")
    public BalanceDto get(@PathVariable UUID id) {
        return BalanceDto.of(service.get(id));
    }

    @GetMapping("/api/saturating-balances/{id}/ledger")
    public List<LedgerEntryDto> ledger(@PathVariable UUID id) {
        return service.ledgerOf(id).stream().map(LedgerEntryDto::of).toList();
    }

    @ExceptionHandler(SaturatingBalanceException.class)
    public ResponseEntity<ProblemDetail> handle(SaturatingBalanceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
