package com.ax.template.authblueprint.statemutation;

import jakarta.validation.Valid;
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
import java.util.Set;
import java.util.UUID;

/**
 * state-conditional-mutability-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link StateMutationService}.
 * The form DTO surfaces {@code mutableFields} — the SAME declared {@link StateFieldPolicy} set the
 * edit path enforces — so a caller/auditor can read what is editable now (STATEMUTATION-DECLARED-001).
 */
@RestController
public class StateMutationController {

    public record OpenReq(@Size(max = 500) String title, @Size(max = 4000) String body) {}
    public record EditReq(@NotNull FormField field, @Size(max = 4000) String value) {}
    public record TransitionReq(@NotNull FormState to, @Size(max = 1000) String reason) {}

    public record FormDto(UUID id, String owner, String title, String body, String reviewerNote,
                          FormState state, Set<FormField> mutableFields, FormField lastEditedField,
                          Instant lastEditedAt, Instant lockedAt) {
        static FormDto of(GovernedForm f) {
            return new FormDto(f.getId(), f.getOwner(), f.getTitle(), f.getBody(), f.getReviewerNote(),
                f.getState(), f.mutableFields(), f.getLastEditedField(), f.getLastEditedAt(), f.getLockedAt());
        }
    }
    public record TransitionDto(long seq, FormState fromState, FormState toState, String kind,
                                String reason, String actor, Instant occurredAt) {
        static TransitionDto of(FormTransition t) {
            return new TransitionDto(t.getSeq(), t.getFromState(), t.getToState(), t.getKind(),
                t.getReason(), t.getActor(), t.getOccurredAt());
        }
    }

    private final StateMutationService service;

    public StateMutationController(StateMutationService service) {
        this.service = service;
    }

    @PostMapping("/api/state-mutation/forms")
    public ResponseEntity<FormDto> open(@Valid @RequestBody OpenReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(FormDto.of(service.open(auth.getName(), req.title(), req.body())));
    }

    /** STATEMUTATION-AUTHORITY/TOCTOU-001 — edit one field; rejected 409 if not mutable in the current state. */
    @PostMapping("/api/state-mutation/forms/{id}/edit")
    public FormDto edit(@PathVariable UUID id, @Valid @RequestBody EditReq req) {
        return FormDto.of(service.editField(id, req.field(), req.value()));
    }

    /** STATEMUTATION-MONOTONE-001 — move the lifecycle; a re-open (widening) requires a recorded reason. */
    @PostMapping("/api/state-mutation/forms/{id}/transition")
    public FormDto transition(@PathVariable UUID id, @Valid @RequestBody TransitionReq req, Authentication auth) {
        return FormDto.of(service.transition(id, req.to(), req.reason(), auth.getName()));
    }

    @GetMapping("/api/state-mutation/forms/{id}")
    public FormDto get(@PathVariable UUID id) {
        return FormDto.of(service.get(id));
    }

    @GetMapping("/api/state-mutation/forms/{id}/transitions")
    public List<TransitionDto> transitions(@PathVariable UUID id) {
        return service.transitions(id).stream().map(TransitionDto::of).toList();
    }

    @ExceptionHandler(StateMutationException.class)
    public ResponseEntity<ProblemDetail> handle(StateMutationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
