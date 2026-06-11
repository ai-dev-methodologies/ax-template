package com.ax.template.authblueprint.obligation;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * deadline-obligation-l0 thin controller. NO endpoint accepts a raw deadline (OBL-GROUND-001) —
 * callers supply derivable axis definitions. The acknowledger is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link ObligationService}
 * / {@link ObligationSweeper}.
 */
@RestController
public class ObligationController {

    public record AxisReq(@NotNull AxisKind kind, Instant anchorAt,
                          @jakarta.validation.constraints.Max(36500) Integer intervalDays,
                          @jakarta.validation.constraints.Digits(integer = 15, fraction = 4) BigDecimal limitUnits,
                          @jakarta.validation.constraints.Digits(integer = 15, fraction = 4)
                          @jakarta.validation.constraints.DecimalMin("0.0001") BigDecimal unitsPerDay) {}
    public record CreateReq(@NotBlank @Size(max = 200) String obligationKey,
                            @NotEmpty List<@Valid AxisReq> axes) {}
    public record UsageReq(@NotNull @Positive
                           @jakarta.validation.constraints.Digits(integer = 15, fraction = 4) BigDecimal units) {}

    public record ObligationDto(String obligationKey, ObligationStatus status,
                                Instant effectiveDeadline, String ackBy, Instant ackAt,
                                Instant createdAt) {
        static ObligationDto of(Obligation o) {
            return new ObligationDto(o.getObligationKey(), o.getStatus(), o.getEffectiveDeadline(),
                o.getAckBy(), o.getAckAt(), o.getCreatedAt());
        }
    }
    public record AxisDto(UUID id, AxisKind kind, Instant anchorAt, Integer intervalDays,
                          BigDecimal limitUnits, BigDecimal usedUnits, BigDecimal unitsPerDay,
                          Instant candidateDeadline) {
        static AxisDto of(ObligationAxis a) {
            return new AxisDto(a.getId(), a.getKind(), a.getAnchorAt(), a.getIntervalDays(),
                a.getLimitUnits(), a.getUsedUnits(), a.getUnitsPerDay(), a.getCandidateDeadline());
        }
    }
    public record EscalationDto(EscalationRung rung, Instant firedAt, Instant deadlineAtFiring) {
        static EscalationDto of(EscalationEvent e) {
            return new EscalationDto(e.getRung(), e.getFiredAt(), e.getDeadlineAtFiring());
        }
    }
    public record DerivationDto(UUID axisId, Instant candidateDeadline, String formula, Instant derivedAt) {
        static DerivationDto of(DerivationRecord d) {
            return new DerivationDto(d.getAxisId(), d.getCandidateDeadline(), d.getFormula(), d.getDerivedAt());
        }
    }

    private final ObligationService service;
    private final ObligationSweeper sweeper;

    public ObligationController(ObligationService service, ObligationSweeper sweeper) {
        this.service = service;
        this.sweeper = sweeper;
    }

    @PostMapping("/api/obligations")
    public ResponseEntity<ObligationDto> create(@Valid @RequestBody CreateReq req) {
        List<ObligationService.AxisSpec> specs = req.axes().stream()
            .map(a -> new ObligationService.AxisSpec(a.kind(), a.anchorAt(), a.intervalDays(),
                a.limitUnits(), a.unitsPerDay()))
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ObligationDto.of(service.create(req.obligationKey(), specs)));
    }

    @PostMapping("/api/obligations/{key}/usage")
    public ObligationDto advanceUsage(@PathVariable String key, @Valid @RequestBody UsageReq req) {
        return ObligationDto.of(service.advanceUsage(key, req.units()));
    }

    @PostMapping("/api/obligations/{key}/ack")
    public ObligationDto acknowledge(@PathVariable String key, Authentication auth) {
        return ObligationDto.of(service.acknowledge(key, auth.getName()));
    }

    /** Deterministic sweep trigger for ONE obligation (the @Scheduled poller covers production).
     *  ADMIN-only: a sweep is an operational action, not a member capability. */
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/obligations/{key}/sweep")
    public ObligationDto sweepOne(@PathVariable String key) {
        Obligation o = service.get(key);
        sweeper.processOne(o.getId());
        return ObligationDto.of(service.get(key));
    }

    @GetMapping("/api/obligations/{key}")
    public ObligationDto get(@PathVariable String key) {
        return ObligationDto.of(service.get(key));
    }

    @GetMapping("/api/obligations/{key}/axes")
    public List<AxisDto> axes(@PathVariable String key) {
        return service.axes(key).stream().map(AxisDto::of).toList();
    }

    @GetMapping("/api/obligations/{key}/escalations")
    public List<EscalationDto> escalations(@PathVariable String key) {
        return service.escalations(key).stream().map(EscalationDto::of).toList();
    }

    @GetMapping("/api/obligations/{key}/derivations")
    public PageEnvelope<DerivationDto> derivations(@PathVariable String key,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.derivations(key, page, size), DerivationDto::of);
    }

    @ExceptionHandler(ObligationException.class)
    public ResponseEntity<ProblemDetail> handle(ObligationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
