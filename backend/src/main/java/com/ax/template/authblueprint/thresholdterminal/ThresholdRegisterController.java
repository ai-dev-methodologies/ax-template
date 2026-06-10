package com.ax.template.authblueprint.thresholdterminal;

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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

/**
 * threshold-terminal-derivation-l0 thin controller. Any authenticated caller may create a register,
 * post accruals, and request a use. {@code @Digits(integer=15, fraction=4)} bounds values to
 * NUMERIC(19,4) at the validation boundary. The accrual response carries the post-accrual status so
 * the caller learns a crossing immediately (TTD-CROSS-001). Delegates to
 * {@link ThresholdRegisterService}.
 */
@RestController
public class ThresholdRegisterController {

    public record CreateReq(@NotBlank @Size(max = 200) String scopeKey,
                            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal limit,
                            @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal initialAnchor) {}
    public record AccrualReq(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal delta) {}

    public record RegisterDto(String scopeKey, BigDecimal anchor, BigDecimal limit,
                              ThresholdStatus status, Long version) {
        static RegisterDto of(ThresholdRegister r) {
            return new RegisterDto(r.getScopeKey(), r.getAnchor(), r.getLimit(), r.getStatus(), r.getVersion());
        }
    }

    private final ThresholdRegisterService service;

    public ThresholdRegisterController(ThresholdRegisterService service) {
        this.service = service;
    }

    @PostMapping("/api/threshold-registers")
    public ResponseEntity<RegisterDto> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RegisterDto.of(service.createRegister(req.scopeKey(), req.limit(), req.initialAnchor())));
    }

    /** The accrual response exposes status — a crossing returns 200 with status=EXPIRED, not a poll. */
    @PostMapping("/api/threshold-registers/{scopeKey}/accruals")
    public RegisterDto accrue(@PathVariable String scopeKey, @Valid @RequestBody AccrualReq req) {
        return RegisterDto.of(service.accrue(scopeKey, req.delta()));
    }

    /** TTD-DERIVE-001 — the derived capability (install / dispatch / issue-to-service). */
    @PostMapping("/api/threshold-registers/{scopeKey}/use")
    public RegisterDto use(@PathVariable String scopeKey) {
        return RegisterDto.of(service.use(scopeKey));
    }

    @GetMapping("/api/threshold-registers/{scopeKey}")
    public RegisterDto get(@PathVariable String scopeKey) {
        return RegisterDto.of(service.getRegister(scopeKey));
    }

    @ExceptionHandler(ThresholdException.class)
    public ResponseEntity<ProblemDetail> handle(ThresholdException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
