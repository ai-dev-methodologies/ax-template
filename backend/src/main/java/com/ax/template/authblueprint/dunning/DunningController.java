package com.ax.template.authblueprint.dunning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * dunning-collections-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link DunningService}.
 */
@RestController
public class DunningController {

    public record OpenReq(@NotBlank @Size(max = 200) String receivableRef,
                          @NotNull LocalDate dueDate,
                          @NotNull @PositiveOrZero BigDecimal overdueAmount) {}
    public record AdvanceReq(@NotNull DunningStage fromStage) {}
    public record PayReq(@NotNull @PositiveOrZero BigDecimal amount) {}

    public record CaseDto(UUID id, String receivableRef, LocalDate dueDate, BigDecimal overdueAmount,
                          BigDecimal paidAmount, DunningStage stage, AgingBucket agingBucket,
                          Instant agingAsOf, long daysOverdue, Instant cureWindowOpenedAt,
                          Instant cureDeadline, boolean ladderHalted) {
        static CaseDto of(DunningCase c) {
            return new CaseDto(c.getId(), c.getReceivableRef(), c.getDueDate(), c.getOverdueAmount(),
                c.getPaidAmount(), c.getStage(), c.getAgingBucket(), c.getAgingAsOf(),
                c.getDaysOverdue(), c.getCureWindowOpenedAt(), c.getCureDeadline(), c.isLadderHalted());
        }
    }
    public record TransitionDto(DunningStage stage, String kind, long daysOverdue, String actor,
                                Instant occurredAt) {
        static TransitionDto of(DunningStageTransition t) {
            return new TransitionDto(t.getStage(), t.getKind(), t.getDaysOverdue(), t.getActor(),
                t.getOccurredAt());
        }
    }

    private final DunningService service;

    public DunningController(DunningService service) {
        this.service = service;
    }

    @PostMapping("/api/dunning/cases")
    public ResponseEntity<CaseDto> open(@Valid @RequestBody OpenReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CaseDto.of(service.open(req.receivableRef(), req.dueDate(), req.overdueAmount())));
    }

    /** DUNNING-LADDER-001 — advance from the OBSERVED stage to its single successor. */
    @PostMapping("/api/dunning/cases/{id}/advance")
    public CaseDto advance(@PathVariable UUID id, @Valid @RequestBody AdvanceReq req, Authentication auth) {
        return CaseDto.of(service.advance(id, req.fromStage(), auth.getName()));
    }

    /** DUNNING-AGING-001 — recompute the deterministic aging bucket at the current as-of instant. */
    @PostMapping("/api/dunning/cases/{id}/reage")
    public CaseDto reage(@PathVariable UUID id) {
        return CaseDto.of(service.reage(id));
    }

    /** DUNNING-CURE-001 — record a payment; opens/keeps the cure window. */
    @PostMapping("/api/dunning/cases/{id}/payments")
    public CaseDto pay(@PathVariable UUID id, @Valid @RequestBody PayReq req) {
        return CaseDto.of(service.pay(id, req.amount()));
    }

    /** DUNNING-CURE-001 — full cure within the window resets to CURRENT and halts the ladder. */
    @PostMapping("/api/dunning/cases/{id}/cure")
    public CaseDto cure(@PathVariable UUID id, Authentication auth) {
        return CaseDto.of(service.cure(id, auth.getName()));
    }

    @GetMapping("/api/dunning/cases/{id}")
    public CaseDto get(@PathVariable UUID id) {
        return CaseDto.of(service.get(id));
    }

    @GetMapping("/api/dunning/cases/{id}/transitions")
    public List<TransitionDto> transitions(@PathVariable UUID id) {
        return service.transitions(id).stream().map(TransitionDto::of).toList();
    }

    @ExceptionHandler(DunningException.class)
    public ResponseEntity<ProblemDetail> handle(DunningException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
