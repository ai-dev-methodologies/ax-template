package com.ax.template.authblueprint.derivedstatement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * derived-statement-l0 thin controller. The subject is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). {@code generate} accepts NO client-supplied
 * idempotency header — retry-safety is entirely STMT-DERIVE-001's derived content hash.
 */
@RestController
public class DerivedStatementController {

    public record LineItemReq(@NotBlank @Size(max = 100) String label,
                              @NotNull @Digits(integer = 11, fraction = 4) BigDecimal amount)
            implements DerivedStatementService.LineItem {}

    public record GenerateReq(@NotBlank @Size(max = 50) String period,
                              @NotEmpty List<LineItemReq> basis) {}

    public record StatementDto(UUID id, String subject, String period, int versionNo, String basisHash,
                               BigDecimal totalAmount, Instant generatedAt) {
        static StatementDto of(DerivedStatement s) {
            return new StatementDto(s.getId(), s.getSubject(), s.getPeriod(), s.getVersionNo(),
                s.getBasisHash(), s.getTotalAmount(), s.getGeneratedAt());
        }
    }

    private final DerivedStatementService service;

    public DerivedStatementController(DerivedStatementService service) {
        this.service = service;
    }

    /** STMT-DERIVE/RETRY-001/002 — no Idempotency-Key header; the derived basis hash is the guard. */
    @PostMapping("/api/statements")
    public ResponseEntity<StatementDto> generate(@Valid @RequestBody GenerateReq req, Authentication auth) {
        DerivedStatement statement = service.generate(auth.getName(), req.period(), req.basis());
        return ResponseEntity.status(HttpStatus.CREATED).body(StatementDto.of(statement));
    }

    @GetMapping("/api/statements/{id}")
    public StatementDto get(@PathVariable UUID id) {
        return StatementDto.of(service.get(id));
    }

    @GetMapping("/api/statements")
    public List<StatementDto> versions(@RequestParam String period, Authentication auth) {
        return service.versionsOf(auth.getName(), period).stream().map(StatementDto::of).toList();
    }

    @ExceptionHandler(DerivedStatementException.class)
    public ResponseEntity<ProblemDetail> handle(DerivedStatementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
