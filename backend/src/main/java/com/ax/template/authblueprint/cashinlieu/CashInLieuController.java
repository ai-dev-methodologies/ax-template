package com.ax.template.authblueprint.cashinlieu;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * cash-in-lieu-l0 thin controller. The acting principal is ALWAYS the authenticated caller.
 * Delegates to {@link CashInLieuService}.
 */
@RestController
public class CashInLieuController {

    public record AllocateReq(@NotBlank @Size(max = 200) String subjectRef,
                              @NotBlank @Size(max = 200) String eventRef,
                              @NotNull BigDecimal holdingQuantity,
                              @NotNull BigDecimal ratio,
                              @NotNull BigDecimal cashRate) {}

    public record AllocationDto(UUID id, String subjectRef, String eventRef, BigDecimal rawEntitlement,
                                long unitsInKind, BigDecimal fractionalRemainder, BigDecimal cashRate,
                                BigDecimal cashValue, Instant allocatedAt) {
        static AllocationDto of(CashInLieuAllocation a) {
            return new AllocationDto(a.getId(), a.getSubjectRef(), a.getEventRef(), a.getRawEntitlement(),
                a.getUnitsInKind(), a.getFractionalRemainder(), a.getCashRate(), a.getCashValue(), a.getAllocatedAt());
        }
    }

    private final CashInLieuService service;

    public CashInLieuController(CashInLieuService service) {
        this.service = service;
    }

    /** CIL-FRACTION-001 / CIL-CONSERVE-002 / CIL-IDEMPOTENT-003 — allocate (or return the frozen prior allocation). */
    @PostMapping("/api/cash-in-lieu/allocations")
    public ResponseEntity<AllocationDto> allocate(@Valid @RequestBody AllocateReq req) {
        AllocationDto dto = AllocationDto.of(service.allocate(
            req.subjectRef(), req.eventRef(), req.holdingQuantity(), req.ratio(), req.cashRate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/cash-in-lieu/allocations/{subjectRef}/{eventRef}")
    public AllocationDto get(@PathVariable String subjectRef, @PathVariable String eventRef) {
        return AllocationDto.of(service.get(subjectRef, eventRef));
    }

    @ExceptionHandler(CashInLieuException.class)
    public ResponseEntity<ProblemDetail> handle(CashInLieuException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
