package com.ax.template.authblueprint.reproducibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * reproducible-procedure-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link ReproducibilityService}.
 * The MEMBER-facing {@link ProcedureDto} carries only the MASKED subject — the raw value is reached
 * only via the ADMIN-gated unmask endpoint (PROC-BLIND-001).
 */
@RestController
public class ReproducibilityController {

    public record DrawReq(@NotBlank @Size(max = 200) String inputSetRef,
                          @NotEmpty List<@NotBlank String> candidates,
                          @NotNull @Positive Integer k,
                          @Size(max = 400) String subject) {}
    public record ClassifyReq(@NotBlank @Size(max = 200) String inputSetRef,
                              @NotBlank @Size(max = 4000) String input,
                              @NotBlank @Size(max = 60) String classifierVersion,
                              @NotBlank @Size(max = 120) String resolvedClass,
                              @Size(max = 400) String subject) {}

    /** The MEMBER-visible projection — note: maskedSubject only; the raw subject NEVER appears here. */
    public record ProcedureDto(UUID id, ProcedureKind kind, String inputSetRef, String inputHash,
                               Long seed, String algorithm, int drawK, List<String> selectedIds,
                               String classifierVersion, String resolvedClass, String maskedSubject,
                               String actor, Instant createdAt) {
        static ProcedureDto of(Procedure p) {
            return new ProcedureDto(p.getId(), p.getKind(), p.getInputSetRef(), p.getInputHash(),
                p.getSeed(), p.getAlgorithm(), p.getDrawK(),
                p.getSelectedIds() == null ? List.of() : List.of(p.getSelectedIds().split(",")),
                p.getClassifierVersion(), p.getResolvedClass(), p.maskedSubject(),
                p.getActor(), p.getCreatedAt());
        }
    }
    public record ReplayDto(UUID id, List<String> selectedIds, boolean reproduced) {}
    public record UnmaskDto(UUID id, String subject) {}

    private final ReproducibilityService service;

    public ReproducibilityController(ReproducibilityService service) {
        this.service = service;
    }

    /** PROC-DRAW-001 — record a draw; the seed is generated server-side. */
    @PostMapping("/api/reproducibility/draws")
    public ResponseEntity<ProcedureDto> draw(@Valid @RequestBody DrawReq req, Authentication auth) {
        Procedure p = service.draw(req.inputSetRef(), req.candidates(), req.k(), req.subject(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcedureDto.of(p));
    }

    /** PROC-CLASS-001 — classify pinned to a version; same input + version is idempotent. */
    @PostMapping("/api/reproducibility/classifications")
    public ResponseEntity<ProcedureDto> classify(@Valid @RequestBody ClassifyReq req, Authentication auth) {
        Procedure p = service.classify(req.inputSetRef(), req.input(), req.classifierVersion(),
            req.resolvedClass(), req.subject(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcedureDto.of(p));
    }

    @GetMapping("/api/reproducibility/procedures/{id}")
    public ProcedureDto get(@PathVariable UUID id) {
        return ProcedureDto.of(service.get(id));
    }

    /** PROC-REPLAY-001 — re-derive the byte-identical selection from the recorded seed. */
    @PostMapping("/api/reproducibility/procedures/{id}/replay")
    public ReplayDto replay(@PathVariable UUID id) {
        List<String> replayed = service.replay(id);
        return new ReplayDto(id, replayed, true);
    }

    /** PROC-BLIND-001 — the raw blinded subject; ADMIN only (least privilege). */
    @GetMapping("/api/reproducibility/procedures/{id}/unmask")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UnmaskDto unmask(@PathVariable UUID id) {
        return new UnmaskDto(id, service.unmask(id));
    }

    @ExceptionHandler(ReproducibilityException.class)
    public ResponseEntity<ProblemDetail> handle(ReproducibilityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
