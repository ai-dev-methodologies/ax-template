package com.ax.template.authblueprint.transformation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * transformation-conservation-l0 thin controller. Recording a transformation is any authenticated
 * user (a shop-floor operation); /api/transformations/**. Delegates to {@link TransformationService}
 * ONLY. Domain errors -> RFC 9457 ProblemDetail with a machine-readable code; @Valid 400s handled by
 * common/GlobalProblemDetailAdvice (an unknown disposition enum or a null disposition on a residual
 * is a 400 at the validation boundary).
 */
@RestController
public class TransformationController {

    // @Digits bounds the qty to NUMERIC(19,4) at the validation boundary — an over-precise (scale>4) or
    // over-magnitude (>15 integer digits) quantity is a clean 400, never a setScale ArithmeticException / DB overflow.
    public record LegReq(@NotBlank String materialCode,
                         @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal qty,
                         @NotBlank String unit) {}
    public record ResidualLegReq(@NotBlank String materialCode,
                                 @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal qty,
                                 @NotBlank String unit, @NotNull TransformationDisposition disposition) {}
    public record RecordReq(@NotEmpty @Size(max = 100) @Valid List<LegReq> inputs,
                            @Size(max = 100) @Valid List<LegReq> goodOutputs,
                            @Size(max = 100) @Valid List<ResidualLegReq> residuals) {}

    public record LegDto(LegRole role, TransformationDisposition disposition, String materialCode,
                         BigDecimal qty, String unit) {
        static LegDto of(TransformationLeg l) {
            return new LegDto(l.getRole(), l.getDisposition(), l.getMaterialCode(), l.getQty(), l.getUnit());
        }
    }
    public record RunDto(UUID id, String createdBy, String baseUnit, BigDecimal totalInput,
                         BigDecimal totalGood, BigDecimal totalResidual, Instant createdAt, List<LegDto> legs) {
        static RunDto of(TransformationService.RecordResult r) {
            return new RunDto(r.run().getId(), r.run().getCreatedBy(), r.run().getBaseUnit(),
                r.run().getTotalInput(), r.run().getTotalGood(), r.run().getTotalResidual(),
                r.run().getCreatedAt(), r.legs().stream().map(LegDto::of).toList());
        }
    }

    private final TransformationService service;

    public TransformationController(TransformationService service) {
        this.service = service;
    }

    @PostMapping("/api/transformations")
    public ResponseEntity<RunDto> record(@Valid @RequestBody RecordReq req, Authentication auth) {
        List<ConservationCheck.Leg> legs = new ArrayList<>();
        for (LegReq i : req.inputs()) {
            legs.add(new ConservationCheck.Leg(LegRole.INPUT, i.materialCode(), i.qty(), i.unit(), null));
        }
        if (req.goodOutputs() != null) {
            for (LegReq g : req.goodOutputs()) {
                legs.add(new ConservationCheck.Leg(LegRole.GOOD_OUTPUT, g.materialCode(), g.qty(), g.unit(), null));
            }
        }
        if (req.residuals() != null) {
            for (ResidualLegReq d : req.residuals()) {
                legs.add(new ConservationCheck.Leg(LegRole.RESIDUAL, d.materialCode(), d.qty(), d.unit(), d.disposition()));
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(RunDto.of(service.record(auth.getName(), legs)));
    }

    @GetMapping("/api/transformations/{id}")
    public RunDto get(@PathVariable UUID id) {
        return RunDto.of(service.get(id));
    }

    @ExceptionHandler(TransformationException.class)
    public ResponseEntity<ProblemDetail> handle(TransformationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
