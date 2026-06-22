package com.ax.template.authblueprint.settlement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * settlement-finality-l0 thin controller. The actor is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). Delegates to {@link SettlementService}.
 */
@RestController
public class SettlementController {

    public record CreateReq(@NotBlank @Size(max = 100) String tradeRef,
                            @NotBlank @Size(max = 200) String deliveryParty,
                            @NotBlank @Size(max = 200) String paymentParty,
                            @NotNull @DecimalMin("0.0") BigDecimal netObligation) {}

    public record NovateReq(@NotNull SettlementLeg leg,
                            @NotBlank @Size(max = 200) String assumingParty) {}

    public record InstructionDto(UUID id, String tradeRef, String deliveryParty, String paymentParty,
                                 BigDecimal netObligation, boolean deliverySettled, boolean paymentSettled,
                                 SettlementStatus status, Instant finalAt) {
        static InstructionDto of(SettlementInstruction s) {
            return new InstructionDto(s.getId(), s.getTradeRef(), s.getDeliveryParty(), s.getPaymentParty(),
                s.getNetObligation(), s.isDeliverySettled(), s.isPaymentSettled(), s.getStatus(), s.getFinalAt());
        }
    }

    public record NovationDto(SettlementLeg leg, String releasedParty, String assumingParty,
                              BigDecimal assumedObligation, String novatedBy, Instant novatedAt) {
        static NovationDto of(NovationRecord n) {
            return new NovationDto(n.getLeg(), n.getReleasedParty(), n.getAssumingParty(),
                n.getAssumedObligation(), n.getNovatedBy(), n.getNovatedAt());
        }
    }

    private final SettlementService service;

    public SettlementController(SettlementService service) {
        this.service = service;
    }

    @PostMapping("/api/settlement/instructions")
    public ResponseEntity<InstructionDto> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InstructionDto.of(
            service.createInstruction(req.tradeRef(), req.deliveryParty(), req.paymentParty(), req.netObligation())));
    }

    /** SETTLE-DVP/FINAL-001 — atomic two-leg commit to irrevocable finality. */
    @PostMapping("/api/settlement/instructions/{id}/settle")
    public InstructionDto settle(@PathVariable UUID id) {
        return InstructionDto.of(service.settle(id));
    }

    /** SETTLE-NOVATE-001 — replace one leg's counterparty (obligation conserved) before finality. */
    @PostMapping("/api/settlement/instructions/{id}/novate")
    public InstructionDto novate(@PathVariable UUID id, @Valid @RequestBody NovateReq req, Authentication auth) {
        return InstructionDto.of(service.novate(id, req.leg(), req.assumingParty(), auth.getName()));
    }

    @PostMapping("/api/settlement/instructions/{id}/fail")
    public InstructionDto fail(@PathVariable UUID id) {
        return InstructionDto.of(service.fail(id));
    }

    @PostMapping("/api/settlement/instructions/{id}/retry")
    public InstructionDto retry(@PathVariable UUID id) {
        return InstructionDto.of(service.retry(id));
    }

    @PostMapping("/api/settlement/instructions/{id}/buyin")
    public InstructionDto buyin(@PathVariable UUID id) {
        return InstructionDto.of(service.buyin(id));
    }

    @GetMapping("/api/settlement/instructions/{id}")
    public InstructionDto get(@PathVariable UUID id) {
        return InstructionDto.of(service.get(id));
    }

    @GetMapping("/api/settlement/instructions/{id}/novations")
    public List<NovationDto> novations(@PathVariable UUID id) {
        return service.novations(id).stream().map(NovationDto::of).toList();
    }

    @ExceptionHandler(SettlementException.class)
    public ResponseEntity<ProblemDetail> handle(SettlementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
