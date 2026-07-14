package com.ax.template.authblueprint.countbudget;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

import java.net.URI;
import java.time.Instant;

/**
 * periodic-count-budget-l0 thin controller. Any authenticated caller may create a policy, adjust its cap,
 * and consume. {@code asOf} on consume is optional (defaults to now) — the reference workload exposes it
 * so a calendar-period boundary can be crossed deterministically (mirrors netmetering's {@code effectiveAt}).
 * Delegates to {@link CountBudgetService}.
 */
@RestController
public class CountBudgetController {

    public record CreateReq(@NotBlank @Size(max = 200) String subjectKey,
                            @NotNull CountBudgetCadence cadence,
                            @Positive int cap) {}
    public record UpdateCapReq(@Positive int cap) {}
    public record ConsumeReq(Instant asOf) {}

    public record PolicyDto(String subjectKey, CountBudgetCadence cadence, int cap, Long version) {
        static PolicyDto of(CountBudgetPolicy p) {
            return new PolicyDto(p.getSubjectKey(), p.getCadence(), p.getCap(), p.getVersion());
        }
    }
    public record PeriodDto(String periodKey, int capAtPeriodStart, long consumedCount, long remaining,
                            Instant firstTouchedAt) {
        static PeriodDto of(CountBudgetService.ConsumeResult r) {
            return new PeriodDto(r.period().getPeriodKey(), r.period().getCapAtPeriodStart(), r.consumedCount(),
                r.period().getCapAtPeriodStart() - r.consumedCount(), r.period().getFirstTouchedAt());
        }
    }
    public record ConsumptionDto(long sequenceNo, Instant consumedAt) {
        static ConsumptionDto of(CountBudgetConsumption c) {
            return new ConsumptionDto(c.getSequenceNo(), c.getConsumedAt());
        }
    }

    private final CountBudgetService service;

    public CountBudgetController(CountBudgetService service) {
        this.service = service;
    }

    @PostMapping("/api/count-budgets/policies")
    public ResponseEntity<PolicyDto> create(@Valid @RequestBody CreateReq req) {
        CountBudgetPolicy p = service.createPolicy(req.subjectKey(), req.cadence(), req.cap());
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyDto.of(p));
    }

    @PostMapping("/api/count-budgets/policies/{subjectKey}/cap")
    public PolicyDto updateCap(@PathVariable String subjectKey, @Valid @RequestBody UpdateCapReq req) {
        return PolicyDto.of(service.updateCap(subjectKey, req.cap()));
    }

    @PostMapping("/api/count-budgets/policies/{subjectKey}/consumptions")
    public ResponseEntity<PeriodDto> consume(@PathVariable String subjectKey, @RequestBody(required = false) ConsumeReq req) {
        Instant asOf = req == null ? null : req.asOf();
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodDto.of(service.consume(subjectKey, asOf)));
    }

    @GetMapping("/api/count-budgets/policies/{subjectKey}")
    public PolicyDto get(@PathVariable String subjectKey) {
        return PolicyDto.of(service.getPolicy(subjectKey));
    }

    @GetMapping("/api/count-budgets/policies/{subjectKey}/periods/{periodKey}")
    public PeriodDto getPeriod(@PathVariable String subjectKey, @PathVariable String periodKey) {
        return PeriodDto.of(service.getPeriod(subjectKey, periodKey));
    }

    /** PCB-AUDIT-001 — every period's first-touch is queryable; rows are never deleted. */
    @GetMapping("/api/count-budgets/policies/{subjectKey}/periods")
    public PageEnvelope<PeriodSummaryDto> periods(@PathVariable String subjectKey,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listPeriods(subjectKey, page, size), PeriodSummaryDto::of);
    }

    public record PeriodSummaryDto(String periodKey, int capAtPeriodStart, Instant firstTouchedAt) {
        static PeriodSummaryDto of(CountBudgetPeriod p) {
            return new PeriodSummaryDto(p.getPeriodKey(), p.getCapAtPeriodStart(), p.getFirstTouchedAt());
        }
    }

    @GetMapping("/api/count-budgets/policies/{subjectKey}/periods/{periodKey}/consumptions")
    public PageEnvelope<ConsumptionDto> consumptions(@PathVariable String subjectKey,
                                                     @PathVariable String periodKey,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listConsumptions(subjectKey, periodKey, page, size), ConsumptionDto::of);
    }

    @ExceptionHandler(CountBudgetException.class)
    public ResponseEntity<ProblemDetail> handle(CountBudgetException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
