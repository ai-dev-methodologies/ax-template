package com.ax.template.authblueprint.register;

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
 * monotone-register-l0 thin controller. Any authenticated caller may create a register and append
 * reads. {@code @Digits(integer=15, fraction=4)} bounds values to NUMERIC(19,4) at the validation
 * boundary (an over-precise value is a 400, never a silent rescale). Delegates to {@link RegisterService}.
 */
@RestController
public class RegisterController {

    public record CreateReq(@NotBlank @Size(max = 200) String scopeKey,
                            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal modulus,
                            @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal initialAnchor) {}
    // reason is NOT @NotBlank here — the service rejects a missing reason for ROLLOVER/EXCHANGE with a
    // domain 422 REGISTER_REASON_REQUIRED (NORMAL reads need none), not a generic 400.
    public record ReadingReq(@NotNull ReadingKind kind,
                             @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal readingValue,
                             @Size(max = 1000) String reason) {}

    public record RegisterDto(String scopeKey, BigDecimal anchor, BigDecimal modulus,
                              BigDecimal totalConsumption, Long version) {}
    public record ReadingDto(UUID id, ReadingKind kind, BigDecimal readingValue, BigDecimal priorAnchor,
                             BigDecimal delta, long sequenceNo, String reason, Instant recordedAt) {
        static ReadingDto of(RegisterReading r) {
            return new ReadingDto(r.getId(), r.getKind(), r.getReadingValue(), r.getPriorAnchor(),
                r.getDelta(), r.getSequenceNo(), r.getReason(), r.getRecordedAt());
        }
    }

    private final RegisterService service;

    public RegisterController(RegisterService service) {
        this.service = service;
    }

    @PostMapping("/api/registers")
    public ResponseEntity<RegisterDto> create(@Valid @RequestBody CreateReq req) {
        Register r = service.createRegister(req.scopeKey(), req.modulus(), req.initialAnchor());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegisterDto(r.getScopeKey(), r.getAnchor(), r.getModulus(),
                BigDecimal.ZERO.setScale(4), r.getVersion()));
    }

    @PostMapping("/api/registers/{scopeKey}/readings")
    public ResponseEntity<ReadingDto> append(@PathVariable String scopeKey, @Valid @RequestBody ReadingReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReadingDto.of(service.append(scopeKey, req.kind(), req.readingValue(), req.reason())));
    }

    @GetMapping("/api/registers/{scopeKey}")
    public RegisterDto get(@PathVariable String scopeKey) {
        Register r = service.getRegister(scopeKey);
        return new RegisterDto(r.getScopeKey(), r.getAnchor(), r.getModulus(),
            service.totalConsumption(scopeKey), r.getVersion());
    }

    @GetMapping("/api/registers/{scopeKey}/readings")
    public PageEnvelope<ReadingDto> readings(@PathVariable String scopeKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listReadings(scopeKey, page, size), ReadingDto::of);
    }

    @ExceptionHandler(RegisterException.class)
    public ResponseEntity<ProblemDetail> handle(RegisterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
