package com.ax.template.authblueprint.withholdingsplit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

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
import java.time.Instant;
import java.util.UUID;

/**
 * withholding-split-l0 thin controller for the {@link RemittanceRun} resource (WHT-REMIT-003).
 * Delegates to {@link RemittanceService}.
 */
@RestController
public class RemittanceController {

    public record CollectReq(@NotBlank String period) {}
    public record RemittanceDto(UUID id, String period, BigDecimal totalWithheld, int postingCount, Instant collectedAt) {
        static RemittanceDto of(RemittanceRun r) {
            return new RemittanceDto(r.getId(), r.getPeriod(), r.getTotalWithheld(), r.getPostingCount(), r.getCollectedAt());
        }
    }

    private final RemittanceService service;

    public RemittanceController(RemittanceService service) {
        this.service = service;
    }

    /** WHT-REMIT-003 — idempotent per-period collection; a rerun returns the frozen row. */
    @PostMapping("/api/withholding-split/remittances")
    public ResponseEntity<RemittanceDto> collect(@Valid @RequestBody CollectReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RemittanceDto.of(service.collect(req.period())));
    }

    @GetMapping("/api/withholding-split/remittances/{period}")
    public RemittanceDto get(@PathVariable String period) {
        return RemittanceDto.of(service.get(period));
    }

    @ExceptionHandler(WithholdingSplitException.class)
    public ResponseEntity<ProblemDetail> handle(WithholdingSplitException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
