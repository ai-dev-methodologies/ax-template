package com.ax.template.authblueprint.netting;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
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

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

/**
 * collection-conservation-l0 thin controller. Any authenticated caller may open a netting run, add
 * gross obligations, and trigger the conserving reduction. {@code @Digits(integer=15, fraction=4)}
 * bounds amounts at the validation boundary. Delegates to {@link NettingService} ONLY.
 */
@RestController
public class NettingController {

    public record CreateRunReq(@NotBlank @Size(max = 200) String runKey,
                               @NotBlank @Size(min = 3, max = 3) String currency) {}
    // amount integer-digits bounded to 12 (not the column's 15) so that many obligations summing onto one
    // member's net stay within the NUMERIC(19,4) position column (~10^15) — no aggregation overflow → 500.
    public record ObligationReq(@NotBlank @Size(max = 120) String fromMember,
                                @NotBlank @Size(max = 120) String toMember,
                                @NotNull @Positive @Digits(integer = 12, fraction = 4) BigDecimal amount,
                                @NotBlank @Size(min = 3, max = 3) String currency) {}

    public record RunDto(String runKey, String currency, NettingStatus status, BigDecimal netTotal, Long version) {
        static RunDto of(NettingRun r) {
            return new RunDto(r.getRunKey(), r.getCurrency(), r.getStatus(), r.getNetTotal(), r.getVersion());
        }
    }
    public record ObligationDto(UUID id, String fromMember, String toMember, BigDecimal amount, String currency) {
        static ObligationDto of(GrossObligation o) {
            return new ObligationDto(o.getId(), o.getFromMember(), o.getToMember(), o.getAmount(), o.getCurrency());
        }
    }
    public record PositionDto(String member, BigDecimal netAmount) {
        static PositionDto of(NetPosition p) {
            return new PositionDto(p.getMember(), p.getNetAmount());
        }
    }

    private final NettingService service;

    public NettingController(NettingService service) {
        this.service = service;
    }

    @PostMapping("/api/netting/runs")
    public ResponseEntity<RunDto> createRun(@Valid @RequestBody CreateRunReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RunDto.of(service.createRun(req.runKey(), req.currency())));
    }

    @PostMapping("/api/netting/runs/{runKey}/obligations")
    public ResponseEntity<ObligationDto> addObligation(@PathVariable String runKey,
                                                       @Valid @RequestBody ObligationReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ObligationDto.of(
            service.addObligation(runKey, req.fromMember(), req.toMember(), req.amount(), req.currency())));
    }

    @PostMapping("/api/netting/runs/{runKey}/net")
    public RunDto net(@PathVariable String runKey) {
        return RunDto.of(service.net(runKey));
    }

    @GetMapping("/api/netting/runs/{runKey}")
    public RunDto get(@PathVariable String runKey) {
        return RunDto.of(service.getRun(runKey));
    }

    @GetMapping("/api/netting/runs/{runKey}/positions")
    public PageEnvelope<PositionDto> positions(@PathVariable String runKey,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listPositions(runKey, page, size), PositionDto::of);
    }

    @GetMapping("/api/netting/runs/{runKey}/obligations")
    public PageEnvelope<ObligationDto> obligations(@PathVariable String runKey,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listObligations(runKey, page, size), ObligationDto::of);
    }

    @ExceptionHandler(NettingException.class)
    public ResponseEntity<ProblemDetail> handle(NettingException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
