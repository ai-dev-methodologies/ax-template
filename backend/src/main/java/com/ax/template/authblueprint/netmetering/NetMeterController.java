package com.ax.template.authblueprint.netmetering;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import java.util.UUID;

/**
 * signed-dual-register-l0 thin controller. Any authenticated caller may create a net meter, append a
 * directional reading, close a billing period, and read the meter / history. {@code @Digits(integer=15,
 * fraction=4)} bounds values to NUMERIC(19,4) at the validation boundary (an over-precise value is a 400,
 * never a silent rescale). Delegates to {@link NetMeterService}.
 */
@RestController
public class NetMeterController {

    public record CreateReq(@NotBlank @Size(max = 200) String meterKey,
                            @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal initialImport,
                            @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal initialExport,
                            // NETM-RATE-001 — optional; omitted defaults to 1 (symmetric rate). Never hardcoded
                            // server-side: the caller's policy is the only source, and it is REQUIRED to be
                            // strictly positive when supplied.
                            @Positive @Digits(integer = 15, fraction = 4) BigDecimal rateImport,
                            @Positive @Digits(integer = 15, fraction = 4) BigDecimal rateExport) {}
    public record ReadingReq(@NotNull MeterDirection direction,
                             @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal readingValue,
                             Instant effectiveAt) {}
    public record CloseReq(@NotNull Instant boundaryAt) {}

    public record MeterDto(String meterKey, BigDecimal cumulativeImport, BigDecimal cumulativeExport,
                           BigDecimal net, BigDecimal rateImport, BigDecimal rateExport,
                           Instant closedThroughAt, Long version) {
        static MeterDto of(NetMeter m) {
            return new MeterDto(m.getMeterKey(), m.getCumulativeImport(), m.getCumulativeExport(),
                m.getNet(), m.getRateImport(), m.getRateExport(), m.getClosedThroughAt(), m.getVersion());
        }
    }
    public record ReadingDto(UUID id, MeterDirection direction, BigDecimal readingValue,
                             BigDecimal priorCumulative, BigDecimal delta, BigDecimal netAfter,
                             BigDecimal importAfter, BigDecimal exportAfter, long sequenceNo,
                             Instant effectiveAt, Instant recordedAt) {
        static ReadingDto of(NetMeterReading r) {
            return new ReadingDto(r.getId(), r.getDirection(), r.getReadingValue(), r.getPriorCumulative(),
                r.getDelta(), r.getNetAfter(), r.getImportAfter(), r.getExportAfter(), r.getSequenceNo(),
                r.getEffectiveAt(), r.getRecordedAt());
        }
    }
    public record PeriodDto(UUID id, Instant boundaryAt, BigDecimal importCumulative,
                            BigDecimal exportCumulative, BigDecimal netStart, BigDecimal netEnd,
                            BigDecimal periodNetDelta, BigDecimal importDelta, BigDecimal exportDelta,
                            BigDecimal rateImport, BigDecimal rateExport, BigDecimal billedAmount,
                            long sequenceNo, Instant closedAt) {
        static PeriodDto of(NetMeterPeriod p) {
            return new PeriodDto(p.getId(), p.getBoundaryAt(), p.getImportCumulative(), p.getExportCumulative(),
                p.getNetStart(), p.getNetEnd(), p.getPeriodNetDelta(), p.getImportDelta(), p.getExportDelta(),
                p.getRateImport(), p.getRateExport(), p.getBilledAmount(), p.getSequenceNo(), p.getClosedAt());
        }
    }

    private final NetMeterService service;

    public NetMeterController(NetMeterService service) {
        this.service = service;
    }

    @PostMapping("/api/netmetering/meters")
    public ResponseEntity<MeterDto> create(@Valid @RequestBody CreateReq req) {
        NetMeter m = service.createMeter(req.meterKey(), req.initialImport(), req.initialExport(),
            req.rateImport(), req.rateExport());
        return ResponseEntity.status(HttpStatus.CREATED).body(MeterDto.of(m));
    }

    @PostMapping("/api/netmetering/meters/{meterKey}/readings")
    public ResponseEntity<ReadingDto> append(@PathVariable String meterKey, @Valid @RequestBody ReadingReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReadingDto.of(service.append(meterKey, req.direction(), req.readingValue(), req.effectiveAt())));
    }

    @PostMapping("/api/netmetering/meters/{meterKey}/periods")
    public ResponseEntity<PeriodDto> close(@PathVariable String meterKey, @Valid @RequestBody CloseReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PeriodDto.of(service.closePeriod(meterKey, req.boundaryAt())));
    }

    @GetMapping("/api/netmetering/meters/{meterKey}")
    public MeterDto get(@PathVariable String meterKey) {
        return MeterDto.of(service.getMeter(meterKey));
    }

    @GetMapping("/api/netmetering/meters/{meterKey}/readings")
    public PageEnvelope<ReadingDto> readings(@PathVariable String meterKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listReadings(meterKey, page, size), ReadingDto::of);
    }

    @GetMapping("/api/netmetering/meters/{meterKey}/periods")
    public PageEnvelope<PeriodDto> periods(@PathVariable String meterKey,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listPeriods(meterKey, page, size), PeriodDto::of);
    }

    @ExceptionHandler(NetMeterException.class)
    public ResponseEntity<ProblemDetail> handle(NetMeterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
