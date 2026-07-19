package com.ax.template.authblueprint.costshare;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

/**
 * cost-share thin controller. Accumulator provisioning is admin (/api/admin/cost-share/** -> ROLE_ADMIN
 * via the /api/admin/** rule); consume/release/reset/allocate are any authenticated user. Delegates to
 * {@link CostShareService} ONLY. Domain errors -> RFC 9457 ProblemDetail with a machine-readable code;
 * @Valid 400s handled by common/GlobalProblemDetailAdvice.
 */
@RestController
public class CostShareController {

    public record CreateRequest(@NotBlank String scopeKey, @NotNull BigDecimal limit, BigDecimal initialUsed) {}
    public record AmountRequest(@NotNull BigDecimal amount) {}
    public record AllocateRequest(@NotNull BigDecimal eligible, @NotBlank String deductibleKey,
                                  @NotBlank String oopMaxKey, @NotNull BigDecimal coinsuranceRate) {}

    public record AccumulatorDto(String scopeKey, BigDecimal limit, BigDecimal used, BigDecimal headroom) {
        static AccumulatorDto of(Accumulator a) {
            return new AccumulatorDto(a.getScopeKey(), a.getLimit(), a.getUsed(), a.headroom());
        }
    }
    public record ConsumeDto(BigDecimal applied, BigDecimal residual) {}

    private final CostShareService service;

    public CostShareController(CostShareService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/cost-share/accumulators")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")   // defense-in-depth backstop (SecurityConfig /api/admin/** also gates)
    public ResponseEntity<AccumulatorDto> create(@Valid @RequestBody CreateRequest req) {
        BigDecimal initial = req.initialUsed() == null ? BigDecimal.ZERO : req.initialUsed();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AccumulatorDto.of(service.create(req.scopeKey(), req.limit(), initial)));
    }

    @PostMapping("/api/cost-share/accumulators/{scopeKey}/consume")
    public ConsumeDto consume(@PathVariable String scopeKey, @Valid @RequestBody AmountRequest req) {
        CostShareService.ConsumeResult r = service.consume(scopeKey, req.amount());
        return new ConsumeDto(r.applied(), r.residual());
    }

    @PostMapping("/api/cost-share/accumulators/{scopeKey}/release")
    public AccumulatorDto release(@PathVariable String scopeKey, @Valid @RequestBody AmountRequest req) {
        return AccumulatorDto.of(service.release(scopeKey, req.amount()));
    }

    @PostMapping("/api/cost-share/accumulators/{scopeKey}/reset")
    public AccumulatorDto reset(@PathVariable String scopeKey) {
        return AccumulatorDto.of(service.reset(scopeKey));
    }

    @PostMapping("/api/cost-share/allocate")
    public CostShareService.AllocationResult allocate(@Valid @RequestBody AllocateRequest req) {
        return service.allocate(req.eligible(), req.deductibleKey(), req.oopMaxKey(), req.coinsuranceRate());
    }

    @GetMapping("/api/cost-share/accumulators/{scopeKey}")
    public AccumulatorDto get(@PathVariable String scopeKey) {
        return AccumulatorDto.of(service.get(scopeKey));
    }

    @ExceptionHandler(CostShareException.class)
    public ResponseEntity<ProblemDetail> handle(CostShareException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
