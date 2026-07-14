package com.ax.template.authblueprint.duplicatesubmission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * duplicate-submission-key-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller. Delegates to {@link DuplicateSubmissionService}.
 */
@RestController
public class DuplicateSubmissionController {

    public record DefineChannelReq(@NotBlank @Size(max = 200) String scopeLabel,
                                   @NotNull @PositiveOrZero Integer fuzzyWindowDays) {}
    public record SubmitReq(@NotBlank @Size(max = 200) String subjectRef,
                            @NotNull LocalDate lossDate,
                            @NotBlank @Size(max = 100) String lossType) {}

    public record ChannelDto(UUID id, String scopeLabel, int fuzzyWindowDays, Long version) {
        static ChannelDto of(DuplicateKeyChannel c) {
            return new ChannelDto(c.getId(), c.getScopeLabel(), c.getFuzzyWindowDays(), c.getVersion());
        }
    }
    public record SubmissionDto(UUID id, UUID channelId, String subjectRef, LocalDate lossDate, String lossType,
                                SubmissionStatus status, boolean flaggedForReview, UUID suspectSubmissionId,
                                Long version, Instant createdAt) {
        static SubmissionDto of(Submission s) {
            return new SubmissionDto(s.getId(), s.getChannelId(), s.getSubjectRef(), s.getLossDate(), s.getLossType(),
                s.getStatus(), s.isFlaggedForReview(), s.getSuspectSubmissionId(), s.getVersion(), s.getCreatedAt());
        }
    }

    private final DuplicateSubmissionService service;

    public DuplicateSubmissionController(DuplicateSubmissionService service) {
        this.service = service;
    }

    @PostMapping("/api/duplicate-submissions/channels")
    public ResponseEntity<ChannelDto> defineChannel(@Valid @RequestBody DefineChannelReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelDto.of(
            service.defineChannel(req.scopeLabel(), req.fuzzyWindowDays())));
    }

    /** DUPKEY-NATURAL/FUZZY-001/002 — gate one submission against the channel's intake history. */
    @PostMapping("/api/duplicate-submissions/channels/{channelId}/submissions")
    public ResponseEntity<SubmissionDto> submit(@PathVariable UUID channelId, @Valid @RequestBody SubmitReq req,
                                                Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SubmissionDto.of(
            service.submit(channelId, req.subjectRef(), req.lossDate(), req.lossType(), auth.getName())));
    }

    /** DUPKEY-WITHDRAWN-003 — releases the natural key for a legitimate resubmission. */
    @PostMapping("/api/duplicate-submissions/submissions/{id}/withdraw")
    public SubmissionDto withdraw(@PathVariable UUID id) {
        return SubmissionDto.of(service.withdraw(id));
    }

    @PostMapping("/api/duplicate-submissions/submissions/{id}/reject")
    public SubmissionDto reject(@PathVariable UUID id) {
        return SubmissionDto.of(service.reject(id));
    }

    @GetMapping("/api/duplicate-submissions/submissions/{id}")
    public SubmissionDto get(@PathVariable UUID id) {
        return SubmissionDto.of(service.get(id));
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    public ResponseEntity<ProblemDetail> handle(DuplicateSubmissionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        if (ex.conflictingSubmissionId() != null) {
            pd.setProperty("conflictingSubmissionId", ex.conflictingSubmissionId());
        }
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
