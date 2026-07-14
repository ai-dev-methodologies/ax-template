package com.ax.template.authblueprint.bilateralhandoff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.util.UUID;

/**
 * bilateral-handoff-l0 thin controller. The confirming/declining party is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param) — delegates to
 * {@link HandoffService}.
 */
@RestController
public class HandoffController {

    public record ProposeReq(@NotBlank @Size(max = 200) String releasorParty,
                             @NotBlank @Size(max = 200) String receiverParty) {}

    public record HandoffDto(UUID id, String releasorParty, String receiverParty, HandoffStatus status,
                             String custodyHolder, Instant releasorConfirmedAt, Instant receiverConfirmedAt) {
        static HandoffDto of(Handoff h) {
            return new HandoffDto(h.getId(), h.getReleasorParty(), h.getReceiverParty(), h.getStatus(),
                h.getCustodyHolder(), h.getReleasorConfirmedAt(), h.getReceiverConfirmedAt());
        }
    }

    private final HandoffService service;

    public HandoffController(HandoffService service) {
        this.service = service;
    }

    /** BHO-FSM-001 — propose a handoff between two named parties. */
    @PostMapping("/api/bilateral-handoff/handoffs")
    public ResponseEntity<HandoffDto> propose(@Valid @RequestBody ProposeReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(HandoffDto.of(service.propose(req.releasorParty(), req.receiverParty())));
    }

    /** BHO-BIND/ATOMIC-001 — the authenticated caller confirms (must be a named party). */
    @PostMapping("/api/bilateral-handoff/handoffs/{id}/confirm")
    public HandoffDto confirm(@PathVariable UUID id, Authentication auth) {
        return HandoffDto.of(service.confirm(id, auth.getName()));
    }

    /** BHO-VOID-001 — the authenticated caller declines (must be a named party). */
    @PostMapping("/api/bilateral-handoff/handoffs/{id}/decline")
    public HandoffDto decline(@PathVariable UUID id, Authentication auth) {
        return HandoffDto.of(service.decline(id, auth.getName()));
    }

    @GetMapping("/api/bilateral-handoff/handoffs/{id}")
    public HandoffDto get(@PathVariable UUID id) {
        return HandoffDto.of(service.get(id));
    }

    @ExceptionHandler(HandoffException.class)
    public ResponseEntity<ProblemDetail> handle(HandoffException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
