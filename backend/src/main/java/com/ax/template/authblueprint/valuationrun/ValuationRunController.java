package com.ax.template.authblueprint.valuationrun;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
import java.util.Map;
import java.util.UUID;

/**
 * valuation-run-projection-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link ValuationRunService}.
 */
@RestController
public class ValuationRunController {

    public record CreateSubjectReq(@NotBlank @Size(max = 200) String subjectRef) {}

    public record RecomputeReq(@NotNull Integer expectedHeadVersion,
                               @NotNull @PositiveOrZero BigDecimal declaredTotal,
                               @NotBlank @Size(max = 1000) String basis,
                               @NotEmpty Map<String, BigDecimal> positions) {}

    public record RebaseReq(@NotNull Integer fromRunVersion,
                            @NotNull @PositiveOrZero BigDecimal declaredTotal,
                            @NotBlank @Size(max = 1000) String basis,
                            @NotEmpty Map<String, BigDecimal> positions) {}

    public record SubjectDto(UUID id, String subjectRef, int headRunVersion) {
        static SubjectDto of(ValuationSubject s) {
            return new SubjectDto(s.getId(), s.getSubjectRef(), s.getHeadRunVersion());
        }
    }

    public record RunDto(UUID id, UUID subjectId, int runVersion, Instant asOf, String basis,
                         BigDecimal totalValue, BigDecimal outputSum, Integer rebasedFromRunVersion) {
        static RunDto of(ValuationRun r) {
            return new RunDto(r.getId(), r.getSubjectId(), r.getRunVersion(), r.getAsOf(), r.getBasis(),
                r.getTotalValue(), r.getOutputSum(), r.getRebasedFromRunVersion());
        }
    }

    private final ValuationRunService service;

    public ValuationRunController(ValuationRunService service) {
        this.service = service;
    }

    @PostMapping("/api/valuation-run/subjects")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody CreateSubjectReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SubjectDto.of(service.createSubject(req.subjectRef())));
    }

    /** VALRUN-ASOF/FANOUT-001 — append a new immutable version, fanning out to N conserving outputs. */
    @PostMapping("/api/valuation-run/subjects/{id}/runs")
    public ResponseEntity<RunDto> recompute(@PathVariable UUID id, @Valid @RequestBody RecomputeReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RunDto.of(
            service.recompute(id, req.expectedHeadVersion(), req.declaredTotal(), req.basis(), req.positions())));
    }

    /** VALRUN-REBASE-001 — reset the basis as a NEW baseline run, retaining the prior runs verbatim. */
    @PostMapping("/api/valuation-run/subjects/{id}/rebase")
    public ResponseEntity<RunDto> rebase(@PathVariable UUID id, @Valid @RequestBody RebaseReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RunDto.of(
            service.rebase(id, req.fromRunVersion(), req.declaredTotal(), req.basis(), req.positions())));
    }

    /** VALRUN-ASOF-001 — the run that was current AS OF the given instant (greatest as-of ≤ T). */
    @GetMapping("/api/valuation-run/subjects/{id}/as-of")
    public RunDto asOf(@PathVariable UUID id, @RequestParam("at") Instant at) {
        return RunDto.of(service.asOf(id, at));
    }

    /** VALRUN-REBASE-001 — the subject's current run (the latest head version). */
    @GetMapping("/api/valuation-run/subjects/{id}/current")
    public RunDto current(@PathVariable UUID id) {
        return RunDto.of(service.current(id));
    }

    @GetMapping("/api/valuation-run/subjects/{id}")
    public SubjectDto getSubject(@PathVariable UUID id) {
        return SubjectDto.of(service.getSubject(id));
    }

    @GetMapping("/api/valuation-run/subjects/{id}/runs/{version}")
    public RunDto getRun(@PathVariable UUID id, @PathVariable("version") int version) {
        return RunDto.of(service.getRun(id, version));
    }

    @GetMapping("/api/valuation-run/subjects/{id}/runs")
    public java.util.List<RunDto> runHistory(@PathVariable UUID id) {
        return service.runHistory(id).stream().map(RunDto::of).toList();
    }

    @GetMapping("/api/valuation-run/subjects/{id}/runs/{version}/outputs")
    public Map<String, BigDecimal> outputs(@PathVariable UUID id, @PathVariable("version") int version) {
        return service.outputs(id, version);
    }

    @ExceptionHandler(ValuationRunException.class)
    public ResponseEntity<ProblemDetail> handle(ValuationRunException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
