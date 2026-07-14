package com.ax.template.authblueprint.withholdingsplit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
import java.util.List;
import java.util.UUID;

/**
 * withholding-split-l0 thin controller for the {@link WithholdingPosting} resource. The acting
 * principal is ALWAYS the authenticated caller. Delegates to {@link WithholdingPostingService}.
 */
@RestController
public class WithholdingPostingController {

    public record PostReq(@NotNull BigDecimal grossAmount,
                          @NotNull @DecimalMin("0.0") @DecimalMax(value = "1.0", inclusive = false) BigDecimal rate,
                          @NotBlank String period) {}

    public record PostingDto(UUID id, BigDecimal grossAmount, BigDecimal rate, String period,
                             UUID correctionOfPostingId, Instant createdAt, List<LegDto> legs) {}
    public record LegDto(UUID id, LegType legType, BigDecimal amount) {
        static LegDto of(WithholdingLeg l) { return new LegDto(l.getId(), l.getLegType(), l.getAmount()); }
    }

    private final WithholdingPostingService service;

    public WithholdingPostingController(WithholdingPostingService service) {
        this.service = service;
    }

    /** WHT-SPLIT-001 / WHT-RATE-002 — post a gross payment; splits into WITHHOLDING + NET legs. */
    @PostMapping("/api/withholding-split/postings")
    public ResponseEntity<PostingDto> post(@Valid @RequestBody PostReq req) {
        WithholdingPosting posting = service.post(req.grossAmount(), req.rate(), req.period());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(posting));
    }

    /** WHT-IMMUTABLE-004 — reverse a posting: a NEW negated posting, the original is untouched. */
    @PostMapping("/api/withholding-split/postings/{id}/reverse")
    public ResponseEntity<PostingDto> reverse(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(service.reverse(id)));
    }

    @GetMapping("/api/withholding-split/postings/{id}")
    public PostingDto get(@PathVariable UUID id) {
        return toDto(service.getPosting(id));
    }

    private PostingDto toDto(WithholdingPosting p) {
        List<LegDto> legs = service.getLegs(p.getId()).stream().map(LegDto::of).toList();
        return new PostingDto(p.getId(), p.getGrossAmount(), p.getRate(), p.getPeriod(),
            p.getCorrectionOfPostingId(), p.getCreatedAt(), legs);
    }

    @ExceptionHandler(WithholdingSplitException.class)
    public ResponseEntity<ProblemDetail> handle(WithholdingSplitException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
