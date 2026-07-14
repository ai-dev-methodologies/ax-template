package com.ax.template.authblueprint.piecewisedeadband;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
import java.util.List;
import java.util.UUID;

/**
 * piecewise-deadband-l0 thin controller. Any authenticated caller may create a config and post
 * evaluations. An evaluation replay (idempotent re-submission of the same (point, actual) pair) returns
 * 200; a NEW evaluation returns 201 (PWDB-IMMUTABLE-001). Delegates to {@link DeadbandService}.
 */
@RestController
public class DeadbandController {

    public record SegmentReq(@NotNull @Digits(integer = 15, fraction = 4) BigDecimal start,
                             @NotNull @Digits(integer = 15, fraction = 4) BigDecimal end,
                             @NotNull @Digits(integer = 15, fraction = 4) BigDecimal obligationTarget,
                             @NotNull @Digits(integer = 15, fraction = 4) BigDecimal deadbandWidth) {}

    public record CreateReq(@NotBlank @Size(max = 200) String configKey,
                            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal domainStart,
                            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal domainEnd,
                            @NotEmpty List<@Valid SegmentReq> segments) {}

    public record EvaluateReq(@NotNull @Digits(integer = 15, fraction = 4) BigDecimal pointX,
                              @NotNull @Digits(integer = 15, fraction = 4) BigDecimal actualValue) {}

    public record SegmentDto(UUID id, BigDecimal start, BigDecimal end, BigDecimal obligationTarget,
                             BigDecimal deadbandWidth) {
        static SegmentDto of(DeadbandSegment s) {
            return new SegmentDto(s.getId(), s.getStart(), s.getEnd(), s.getObligationTarget(),
                s.getDeadbandWidth());
        }
    }

    public record ConfigDto(String configKey, BigDecimal domainStart, BigDecimal domainEnd,
                            List<SegmentDto> segments) {}

    public record EvaluationDto(UUID id, UUID segmentId, BigDecimal pointX, BigDecimal actualValue,
                                BigDecimal obligationTarget, BigDecimal deadbandWidth, BigDecimal deviation,
                                boolean compliant, long sequenceNo, Instant evaluatedAt) {
        static EvaluationDto of(DeadbandEvaluation e) {
            return new EvaluationDto(e.getId(), e.getSegmentId(), e.getPointX(), e.getActualValue(),
                e.getObligationTarget(), e.getDeadbandWidth(), e.getDeviation(), e.isCompliant(),
                e.getSequenceNo(), e.getEvaluatedAt());
        }
    }

    private final DeadbandService service;

    public DeadbandController(DeadbandService service) {
        this.service = service;
    }

    @PostMapping("/api/piecewise-deadband/configs")
    public ResponseEntity<ConfigDto> create(@Valid @RequestBody CreateReq req) {
        List<DeadbandService.SegmentSpec> specs = req.segments().stream()
            .map(s -> new DeadbandService.SegmentSpec(s.start(), s.end(), s.obligationTarget(), s.deadbandWidth()))
            .toList();
        DeadbandConfig config = service.createConfig(req.configKey(), req.domainStart(), req.domainEnd(), specs);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(config));
    }

    @PostMapping("/api/piecewise-deadband/configs/{configKey}/evaluations")
    public ResponseEntity<EvaluationDto> evaluate(@PathVariable String configKey,
                                                  @Valid @RequestBody EvaluateReq req) {
        DeadbandService.EvaluationResult result = service.evaluate(configKey, req.pointX(), req.actualValue());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(EvaluationDto.of(result.evaluation()));
    }

    @GetMapping("/api/piecewise-deadband/configs/{configKey}")
    public ConfigDto get(@PathVariable String configKey) {
        return toDto(service.getConfig(configKey));
    }

    @GetMapping("/api/piecewise-deadband/configs/{configKey}/evaluations")
    public PageEnvelope<EvaluationDto> evaluations(@PathVariable String configKey,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listEvaluations(configKey, page, size), EvaluationDto::of);
    }

    private ConfigDto toDto(DeadbandConfig config) {
        List<SegmentDto> segments = service.getSegments(config.getConfigKey()).stream()
            .map(SegmentDto::of).toList();
        return new ConfigDto(config.getConfigKey(), config.getDomainStart(), config.getDomainEnd(), segments);
    }

    @ExceptionHandler(DeadbandException.class)
    public ResponseEntity<ProblemDetail> handle(DeadbandException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
