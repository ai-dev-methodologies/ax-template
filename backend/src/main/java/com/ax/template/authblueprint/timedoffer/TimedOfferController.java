package com.ax.template.authblueprint.timedoffer;

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

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * timed-offer-exclusive-assignment-l0 thin controller. The acting candidate is ALWAYS the
 * authenticated caller on accept/decline (caller-authentication-only-no-userid-param). Delegates to
 * {@link TimedOfferService}.
 */
@RestController
public class TimedOfferController {

    public record ExtendReq(@NotBlank @Size(max = 200) String subjectId,
                            @NotBlank @Size(max = 200) String candidate,
                            @NotNull Instant deadline) {}
    public record ReofferReq(@NotBlank @Size(max = 200) String nextCandidate,
                             @NotNull Instant deadline) {}

    public record OfferDto(UUID id, String subjectId, String candidate, OfferStatus status,
                           Instant deadline, int attemptSeq, UUID priorOfferId, String decidedBy,
                           Instant decidedAt) {
        static OfferDto of(TimedOffer o) {
            return new OfferDto(o.getId(), o.getSubjectId(), o.getCandidate(), o.getStatus(),
                o.getDeadline(), o.getAttemptSeq(), o.getPriorOfferId(), o.getDecidedBy(),
                o.getDecidedAt());
        }
    }

    private final TimedOfferService service;

    public TimedOfferController(TimedOfferService service) {
        this.service = service;
    }

    /** TIMEDOFFER-LIFECYCLE-001 — extend an offer for a subject to a candidate with a deadline. */
    @PostMapping("/api/timed-offer/offers")
    public ResponseEntity<OfferDto> extend(@Valid @RequestBody ExtendReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OfferDto.of(service.extend(req.subjectId(), req.candidate(), req.deadline())));
    }

    /** TIMEDOFFER-EXCLUSIVE-001 — the authenticated caller accepts the offer, claiming the subject. */
    @PostMapping("/api/timed-offer/offers/{id}/accept")
    public OfferDto accept(@PathVariable UUID id, Authentication auth) {
        return OfferDto.of(service.accept(id, auth.getName()));
    }

    /** TIMEDOFFER-LIFECYCLE-001 — the authenticated caller declines the offer. */
    @PostMapping("/api/timed-offer/offers/{id}/decline")
    public OfferDto decline(@PathVariable UUID id, Authentication auth) {
        return OfferDto.of(service.decline(id, auth.getName()));
    }

    /** TIMEDOFFER-LADDER-001 — re-offer a declined/expired offer to the next candidate (append-only). */
    @PostMapping("/api/timed-offer/offers/{id}/reoffer")
    public ResponseEntity<OfferDto> reoffer(@PathVariable UUID id, @Valid @RequestBody ReofferReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OfferDto.of(service.reoffer(id, req.nextCandidate(), req.deadline())));
    }

    @GetMapping("/api/timed-offer/offers/{id}")
    public OfferDto get(@PathVariable UUID id) {
        return OfferDto.of(service.get(id));
    }

    /** TIMEDOFFER-LADDER-001 — the append-only attempt ladder for a subject, in attempt order. */
    @GetMapping("/api/timed-offer/subjects/{subjectId}/ladder")
    public List<OfferDto> ladder(@PathVariable String subjectId) {
        return service.ladder(subjectId).stream().map(OfferDto::of).toList();
    }

    @ExceptionHandler(TimedOfferException.class)
    public ResponseEntity<ProblemDetail> handle(TimedOfferException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
