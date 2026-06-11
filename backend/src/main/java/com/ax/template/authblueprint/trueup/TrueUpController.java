package com.ax.template.authblueprint.trueup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * remeasurement-trueup-l0 thin controller. The period subject is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link TrueUpService}.
 */
@RestController
public class TrueUpController {

    public record CreatePeriodReq(@NotBlank @Size(max = 100) String label,
                                  @NotNull @Min(1) @Max(50) Integer gridSlots) {}
    public record ReadingReq(@NotNull @Min(0) Integer slotIndex,
                             // 9 integer digits: a 50-slot Σ (≤5×10^10) and its net delta vs a
                             // prior total (≤10^11) both stay inside NUMERIC(15,4)'s 11 digits
                             @NotNull @Digits(integer = 9, fraction = 4) BigDecimal value,
                             @NotNull ReadingSource source,
                             @Size(max = 50) String estimationMethod) {}
    public record RecomputeReq(UUID targetPeriodId) {}

    public record PeriodDto(UUID id, String subject, String label, int gridSlots,
                            PeriodStatus status, UUID runOfRecordId) {
        static PeriodDto of(SettlementPeriod p) {
            return new PeriodDto(p.getId(), p.getSubject(), p.getLabel(), p.getGridSlots(),
                p.getStatus(), p.getRunOfRecordId());
        }
    }
    public record ReadingDto(UUID id, int slotIndex, int slotVersion, BigDecimal value,
                             ReadingSource source, String estimationMethod, ReadingStatus status,
                             UUID supersededById) {
        static ReadingDto of(MeterReading r) {
            return new ReadingDto(r.getId(), r.getSlotIndex(), r.getSlotVersion(), r.getReadingValue(),
                r.getSource(), r.getEstimationMethod(), r.getStatus(), r.getSupersededById());
        }
    }
    public record RunDto(UUID id, UUID periodId, int runVersion, String basisJson, String basisHash,
                         BigDecimal totalValue, Instant computedAt) {
        static RunDto of(SettlementRun run) {
            return new RunDto(run.getId(), run.getPeriodId(), run.getRunVersion(), run.getBasisJson(),
                run.getBasisHash(), run.getTotalValue(), run.getComputedAt());
        }
    }
    public record PostingDto(UUID id, UUID runId, UUID sourcePeriodId, UUID targetPeriodId,
                             int fromRunVersion, int toRunVersion, BigDecimal amount, Instant postedAt) {
        static PostingDto of(TrueUpPosting t) {
            return new PostingDto(t.getId(), t.getRunId(), t.getSourcePeriodId(), t.getTargetPeriodId(),
                t.getFromRunVersion(), t.getToRunVersion(), t.getAmount(), t.getPostedAt());
        }
    }

    private final TrueUpService service;

    public TrueUpController(TrueUpService service) {
        this.service = service;
    }

    @PostMapping("/api/trueup/periods")
    public ResponseEntity<PeriodDto> createPeriod(@Valid @RequestBody CreatePeriodReq req,
                                                  Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PeriodDto.of(service.createPeriod(auth.getName(), req.label(), req.gridSlots())));
    }

    @PostMapping("/api/trueup/periods/{id}/readings")
    public ResponseEntity<ReadingDto> recordReading(@PathVariable UUID id,
                                                    @Valid @RequestBody ReadingReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReadingDto.of(service.recordReading(
            id, req.slotIndex(), req.value(), req.source(), req.estimationMethod())));
    }

    /** TUP-GRID-001 — explicit gap-fill; every created row records its estimation method. */
    @PostMapping("/api/trueup/periods/{id}/estimate-missing")
    public List<ReadingDto> estimateMissing(@PathVariable UUID id) {
        return service.estimateMissing(id).stream().map(ReadingDto::of).toList();
    }

    @PostMapping("/api/trueup/periods/{id}/recompute")
    public RunDto recompute(@PathVariable UUID id, @Valid @RequestBody RecomputeReq req) {
        return RunDto.of(service.recompute(id, req.targetPeriodId()));
    }

    @PostMapping("/api/trueup/periods/{id}/close")
    public PeriodDto close(@PathVariable UUID id) {
        return PeriodDto.of(service.close(id));
    }

    @PostMapping("/api/trueup/periods/{id}/seal")
    public PeriodDto seal(@PathVariable UUID id) {
        return PeriodDto.of(service.seal(id));
    }

    @GetMapping("/api/trueup/periods/{id}")
    public PeriodDto getPeriod(@PathVariable UUID id) {
        return PeriodDto.of(service.getPeriod(id));
    }

    /** Full supersession trail — every version of every slot (TUP-SUPERSEDE-001). */
    @GetMapping("/api/trueup/periods/{id}/readings")
    public List<ReadingDto> readings(@PathVariable UUID id) {
        return service.readingTrail(id).stream().map(ReadingDto::of).toList();
    }

    @GetMapping("/api/trueup/periods/{id}/runs")
    public List<RunDto> runs(@PathVariable UUID id) {
        return service.runsOf(id).stream().map(RunDto::of).toList();
    }

    /** Postings CORRECTING this period (source = id), wherever they were posted. */
    @GetMapping("/api/trueup/periods/{id}/postings")
    public List<PostingDto> postings(@PathVariable UUID id) {
        return service.postingsFor(id).stream().map(PostingDto::of).toList();
    }

    @ExceptionHandler(TrueUpException.class)
    public ResponseEntity<ProblemDetail> handle(TrueUpException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
