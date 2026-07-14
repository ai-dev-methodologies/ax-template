package com.ax.template.authblueprint.thresholdfiling;

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
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * threshold-filing-obligation-l0 thin controller. Any authenticated caller creates a register,
 * posts accruals, and acknowledges its bound filing. The overdue-listing endpoint is ADMIN-only
 * (an operational/compliance action, not a member capability — mirrors obligation's sweep
 * trigger). Delegates to {@link FilingService}.
 */
@RestController
public class FilingController {

    public record CreateReq(@NotBlank @Size(max = 200) String subjectKey,
                            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal threshold) {}
    public record AccrualReq(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal delta) {}

    public record RegisterDto(String subjectKey, BigDecimal accruedValue, BigDecimal threshold,
                              FilingRegisterStatus status, Long version) {
        static RegisterDto of(FilingRegister r) {
            return new RegisterDto(r.getSubjectKey(), r.getAccruedValue(), r.getThreshold(),
                r.getStatus(), r.getVersion());
        }
    }
    public record FilingDto(String subjectKey, BigDecimal thresholdSnapshot, Instant triggerInstant,
                            Instant dueAt, FilingObligationStatus status, String ackBy, Instant ackAt) {
        static FilingDto of(FilingObligation f) {
            return new FilingDto(f.getSubjectKey(), f.getThresholdSnapshot(), f.getTriggerInstant(),
                f.getDueAt(), f.getStatus(), f.getAckBy(), f.getAckAt());
        }
    }

    private final FilingService service;

    public FilingController(FilingService service) {
        this.service = service;
    }

    @PostMapping("/api/filing-registers")
    public ResponseEntity<RegisterDto> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RegisterDto.of(service.createRegister(req.subjectKey(), req.threshold())));
    }

    /** The accrual response exposes status — a crossing returns 200 with status=TRIGGERED, not a poll. */
    @PostMapping("/api/filing-registers/{subjectKey}/accruals")
    public RegisterDto accrue(@PathVariable String subjectKey, @Valid @RequestBody AccrualReq req) {
        return RegisterDto.of(service.accrue(subjectKey, req.delta()));
    }

    @GetMapping("/api/filing-registers/{subjectKey}")
    public RegisterDto get(@PathVariable String subjectKey) {
        return RegisterDto.of(service.getRegister(subjectKey));
    }

    /** TFO-FILING-RECORD-001 — 404 until the register's accrual crosses the threshold. */
    @GetMapping("/api/filing-registers/{subjectKey}/filing")
    public FilingDto getFiling(@PathVariable String subjectKey) {
        return FilingDto.of(service.getFiling(subjectKey));
    }

    /** TFO-DEADLINE-001 — the acknowledger is ALWAYS the authenticated caller. */
    @PostMapping("/api/filing-registers/{subjectKey}/filing/ack")
    public FilingDto acknowledge(@PathVariable String subjectKey, Authentication auth) {
        return FilingDto.of(service.acknowledge(subjectKey, auth.getName()));
    }

    /** TFO-DEADLINE-001 — fail-closed visibility across ALL registers; ADMIN-only operational view. */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/api/filing-registers/overdue")
    public PageEnvelope<FilingDto> overdue(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.overdueOpen(page, size), FilingDto::of);
    }

    @ExceptionHandler(FilingException.class)
    public ResponseEntity<ProblemDetail> handle(FilingException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
