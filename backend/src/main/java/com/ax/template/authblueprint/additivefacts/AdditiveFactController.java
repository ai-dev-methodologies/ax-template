package com.ax.template.authblueprint.additivefacts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * additive-fact-ledger-l0 thin controller. The period subject is ALWAYS the authenticated
 * caller. Delegates to {@link AdditiveFactService}.
 */
@RestController
public class AdditiveFactController {

    public record CreatePeriodReq(@NotBlank @Size(max = 100) String label) {}
    public record AddFactReq(@NotBlank @Size(max = 100) String source,
                             @NotBlank @Size(max = 200) String externalFactId,
                             @NotNull @Digits(integer = 9, fraction = 4) BigDecimal amount,
                             UUID currentOpenPeriodId) {}

    public record PeriodDto(UUID id, String subject, String label, FactPeriodStatus status,
                            BigDecimal frozenAggregate, BigDecimal total) {
        static PeriodDto of(FactPeriod p, BigDecimal total) {
            return new PeriodDto(p.getId(), p.getSubject(), p.getLabel(), p.getStatus(),
                p.getFrozenAggregate(), total);
        }
    }
    public record FactDto(UUID id, UUID periodId, String source, String externalFactId,
                          BigDecimal amount, Instant createdAt) {
        static FactDto of(Fact f) {
            return new FactDto(f.getId(), f.getPeriodId(), f.getSource(), f.getExternalFactId(),
                f.getAmount(), f.getCreatedAt());
        }
    }
    public record PostingDto(UUID id, UUID currentPeriodId, UUID originPeriodId, UUID factId,
                             BigDecimal amount, Instant postedAt) {
        static PostingDto of(LateDeltaPosting p) {
            return new PostingDto(p.getId(), p.getCurrentPeriodId(), p.getOriginPeriodId(),
                p.getFactId(), p.getAmount(), p.getPostedAt());
        }
    }

    private final AdditiveFactService service;

    public AdditiveFactController(AdditiveFactService service) {
        this.service = service;
    }

    @PostMapping("/api/additive-facts/periods")
    public ResponseEntity<PeriodDto> createPeriod(@Valid @RequestBody CreatePeriodReq req, Authentication auth) {
        FactPeriod period = service.createPeriod(auth.getName(), req.label());
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodDto.of(period, BigDecimal.ZERO));
    }

    @PostMapping("/api/additive-facts/periods/{id}/facts")
    public ResponseEntity<FactDto> addFact(@PathVariable UUID id, @Valid @RequestBody AddFactReq req) {
        Fact fact = service.addFact(id, req.source(), req.externalFactId(), req.amount(),
            req.currentOpenPeriodId());
        return ResponseEntity.status(HttpStatus.CREATED).body(FactDto.of(fact));
    }

    @PostMapping("/api/additive-facts/periods/{id}/close")
    public PeriodDto close(@PathVariable UUID id) {
        FactPeriod period = service.close(id);
        return PeriodDto.of(period, service.total(id));
    }

    @GetMapping("/api/additive-facts/periods/{id}")
    public PeriodDto getPeriod(@PathVariable UUID id) {
        FactPeriod period = service.getPeriod(id);
        return PeriodDto.of(period, service.total(id));
    }

    @GetMapping("/api/additive-facts/periods/{id}/facts")
    public List<FactDto> facts(@PathVariable UUID id) {
        return service.factsOf(id).stream().map(FactDto::of).toList();
    }

    /** Postings CORRECTING this period (origin = id), wherever they were posted. */
    @GetMapping("/api/additive-facts/periods/{id}/postings")
    public List<PostingDto> postings(@PathVariable UUID id) {
        return service.postingsFor(id).stream().map(PostingDto::of).toList();
    }

    @ExceptionHandler(AdditiveFactException.class)
    public ResponseEntity<ProblemDetail> handle(AdditiveFactException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
