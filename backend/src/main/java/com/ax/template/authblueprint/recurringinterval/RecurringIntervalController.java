package com.ax.template.authblueprint.recurringinterval;

import com.ax.template.authblueprint.common.PageEnvelope;

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

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * completion-reset-recurring-interval-l0 thin controller. NO endpoint accepts a windowStart,
 * nextDueAt, or due/overdue value (CRI-DUE-001) — the window is derived from anchor + interval and
 * advanced by completion. The completer is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). The deterministic sweep trigger is ADMIN-only.
 * Delegates to {@link RecurringObligationService} / {@link RecurringIntervalSweeper}.
 */
@RestController
public class RecurringIntervalController {

    /** Create contract — carries ONLY the recurring interval + optional anchor; no due/window field. */
    public record CreateReq(@NotBlank @Size(max = 200) String obligationKey,
                            @NotNull @Positive Long intervalSeconds,
                            Instant anchorAt) {}

    public record RecurringObligationDto(String obligationKey, RecurringObligationStatus status,
                                         long intervalSeconds, Instant windowStart, Instant nextDueAt,
                                         boolean overdue, boolean sweptOverdue, Instant lastCompletedAt,
                                         Instant createdAt) {
        static RecurringObligationDto of(RecurringObligation o, boolean overdue) {
            return new RecurringObligationDto(o.getObligationKey(), o.getStatus(), o.getIntervalSeconds(),
                o.getWindowStart(), o.nextDueAt(), overdue, o.isSweptOverdue(), o.getLastCompletedAt(),
                o.getCreatedAt());
        }
    }

    public record OccurrenceDto(UUID id, Instant closedWindowStart, String completedBy, Instant completedAt) {
        static OccurrenceDto of(Occurrence o) {
            return new OccurrenceDto(o.getId(), o.getClosedWindowStart(), o.getCompletedBy(), o.getCompletedAt());
        }
    }

    private final RecurringObligationService service;
    private final RecurringIntervalSweeper sweeper;

    public RecurringIntervalController(RecurringObligationService service, RecurringIntervalSweeper sweeper) {
        this.service = service;
        this.sweeper = sweeper;
    }

    @PostMapping("/api/recurring-interval")
    public ResponseEntity<RecurringObligationDto> create(@jakarta.validation.Valid @RequestBody CreateReq req) {
        RecurringObligation o = service.create(req.obligationKey(), req.intervalSeconds(), req.anchorAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringObligationDto.of(o, service.isOverdueNow(o)));
    }

    /** CRI-RESET-001 — completing advances the window to the completion instant. The completer is
     *  the authenticated caller, never a body field (CRI-AUTHZ-001). */
    @PostMapping("/api/recurring-interval/{key}/complete")
    public RecurringObligationDto complete(@PathVariable String key, Authentication auth) {
        RecurringObligation o = service.complete(key, auth.getName());
        return RecurringObligationDto.of(o, service.isOverdueNow(o));
    }

    /** Deterministic sweep trigger for ONE obligation (the @Scheduled poller covers production).
     *  ADMIN-only: a sweep is an operational action, not a member capability (CRI-AUTHZ-001). */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/recurring-interval/{key}/sweep")
    public RecurringObligationDto sweepOne(@PathVariable String key) {
        RecurringObligation o = service.get(key);
        sweeper.sweepOne(o.getId());
        RecurringObligation refreshed = service.get(key);
        return RecurringObligationDto.of(refreshed, service.isOverdueNow(refreshed));
    }

    @GetMapping("/api/recurring-interval/{key}")
    public RecurringObligationDto get(@PathVariable String key) {
        RecurringObligation o = service.get(key);
        return RecurringObligationDto.of(o, service.isOverdueNow(o));
    }

    @GetMapping("/api/recurring-interval/{key}/occurrences")
    public PageEnvelope<OccurrenceDto> occurrences(@PathVariable String key,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.occurrences(key, page, size), OccurrenceDto::of);
    }

    @ExceptionHandler(RecurringIntervalException.class)
    public ResponseEntity<ProblemDetail> handle(RecurringIntervalException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
