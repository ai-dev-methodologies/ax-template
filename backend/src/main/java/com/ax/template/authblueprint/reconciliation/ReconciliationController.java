package com.ax.template.authblueprint.reconciliation;

import jakarta.validation.Valid;
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
 * external-reconciliation-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link ReconciliationService}.
 */
@RestController
public class ReconciliationController {

    public record FeedLine(@NotBlank @Size(max = 200) String key, @NotNull BigDecimal amount)
        implements ReconciliationService.FeedEntry {}

    public record RunReq(@NotBlank @Size(max = 200) String sourceKey,
                         @NotBlank @Size(max = 200) String feedSnapshotHash,
                         @NotNull List<@Valid FeedLine> internal,
                         @NotNull List<@Valid FeedLine> external) {}

    // reason is NOT @NotBlank here on purpose: the service is the sole enforcer of the non-blank
    // rule (RECON-DISPOSE-001 → 422 RECON_BLANK_REASON), so a blank reason is a domain 422, not a
    // framework 400. @Size still bounds the column length.
    public record DisposeReq(@NotNull DispositionType dispositionType,
                             @Size(max = 1000) String reason) {}

    public record RunDto(UUID id, String sourceKey, String feedSnapshotHash, ReconciliationStatus status,
                         Instant resolvedAt, Instant createdAt) {
        static RunDto of(ReconciliationRun r) {
            return new RunDto(r.getId(), r.getSourceKey(), r.getFeedSnapshotHash(), r.getStatus(),
                r.getResolvedAt(), r.getCreatedAt());
        }
    }

    public record ItemDto(UUID id, UUID runId, String itemKey, ItemClassification classification,
                          BigDecimal internalAmount, BigDecimal externalAmount, BigDecimal delta,
                          boolean disposed, DispositionType dispositionType, String disposedBy,
                          Instant disposedAt, String dispositionReason) {
        static ItemDto of(ReconciliationItem i) {
            return new ItemDto(i.getId(), i.getRunId(), i.getItemKey(), i.getClassification(),
                i.getInternalAmount(), i.getExternalAmount(), i.getDelta(), i.isDisposed(),
                i.getDispositionType(), i.getDisposedBy(), i.getDisposedAt(), i.getDispositionReason());
        }
    }

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    /** RECON-CLASSIFY/IDEMPOTENT-001 — match + classify; idempotent on (sourceKey, feedSnapshotHash).
     *  A re-run on the same feed returns the existing run verbatim (201 either way — the run id is
     *  the identity a caller dedupes on, not the status code). */
    @PostMapping("/api/reconciliation/runs")
    public ResponseEntity<RunDto> run(@Valid @RequestBody RunReq req) {
        ReconciliationRun r = service.run(req.sourceKey(), req.feedSnapshotHash(),
            ReconciliationService.toMap(req.internal()), ReconciliationService.toMap(req.external()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RunDto.of(r));
    }

    @GetMapping("/api/reconciliation/runs/{id}")
    public RunDto get(@PathVariable UUID id) {
        return RunDto.of(service.get(id));
    }

    @GetMapping("/api/reconciliation/runs/{id}/items")
    public List<ItemDto> items(@PathVariable UUID id) {
        return service.items(id).stream().map(ItemDto::of).toList();
    }

    /** RECON-DISPOSE-001 — record the human disposition of a BREAK (the caller is the actor). */
    @PostMapping("/api/reconciliation/runs/{id}/items/{itemId}/disposition")
    public ItemDto dispose(@PathVariable UUID id, @PathVariable UUID itemId,
                           @Valid @RequestBody DisposeReq req, Authentication auth) {
        return ItemDto.of(service.dispose(id, itemId, req.dispositionType(), req.reason(), auth.getName()));
    }

    /** RECON-RESOLVE-001 — resolve the run (refused 422 while any break is undisposed). */
    @PostMapping("/api/reconciliation/runs/{id}/resolve")
    public RunDto resolve(@PathVariable UUID id) {
        return RunDto.of(service.resolve(id));
    }

    @ExceptionHandler(ReconciliationException.class)
    public ResponseEntity<ProblemDetail> handle(ReconciliationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
