package com.ax.template.authblueprint.copresence;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * negative-copresence-gate-l0 thin controller. The knowledge base (concepts + conflict rules) is
 * ADMIN-managed (a safety KB must not be poisoned by any caller); creating subjects and adding members
 * (the gate) + reads are authenticated. Delegates to {@link CopresenceService} ONLY.
 */
@RestController
public class CopresenceController {

    public record SubjectReq(@NotBlank @Size(max = 200) String subjectKey) {}
    public record ConceptReq(@NotBlank @Size(max = 200) String concept) {}
    public record ConflictReq(@NotBlank @Size(max = 200) String conceptA,
                              @NotBlank @Size(max = 200) String conceptB,
                              @NotNull ConflictSeverity severity,
                              @NotBlank @Size(max = 500) String reason) {}
    // overrideReason is NOT @NotBlank — the service decides whether a reason is REQUIRED (only when a
    // RELATIVE finding fires) and rejects a blank one with a domain 422, not a generic 400.
    public record MemberReq(@NotBlank @Size(max = 200) String concept,
                            @NotBlank @Size(max = 400) String label,
                            @Size(max = 1000) String overrideReason) {}

    public record SubjectDto(String subjectKey, Long version) {}
    public record MemberDto(UUID id, String concept, String label, MemberStatus status,
                            String overrideReason, String overriddenFindings, Instant createdAt) {
        static MemberDto of(SubjectMember m) {
            return new MemberDto(m.getId(), m.getConcept(), m.getLabel(), m.getStatus(),
                m.getOverrideReason(), m.getOverriddenFindings(), m.getCreatedAt());
        }
    }

    private final CopresenceService service;

    public CopresenceController(CopresenceService service) {
        this.service = service;
    }

    @PostMapping("/api/copresence/subjects")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody SubjectReq req) {
        Subject s = service.createSubject(req.subjectKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(new SubjectDto(s.getSubjectKey(), s.getVersion()));
    }

    @PostMapping("/api/copresence/kb/concepts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ConceptReq> registerConcept(@Valid @RequestBody ConceptReq req) {
        service.registerConcept(req.concept());
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @PostMapping("/api/copresence/kb/conflicts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ConflictReq> addConflict(@Valid @RequestBody ConflictReq req) {
        service.addConflict(req.conceptA(), req.conceptB(), req.severity(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @PostMapping("/api/copresence/subjects/{subjectKey}/members")
    public ResponseEntity<MemberDto> addMember(@PathVariable String subjectKey, @Valid @RequestBody MemberReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MemberDto.of(service.addMember(subjectKey, req.concept(), req.label(), req.overrideReason())));
    }

    @DeleteMapping("/api/copresence/subjects/{subjectKey}/members/{memberId}")
    public MemberDto removeMember(@PathVariable String subjectKey, @PathVariable UUID memberId) {
        return MemberDto.of(service.removeMember(subjectKey, memberId));
    }

    @GetMapping("/api/copresence/subjects/{subjectKey}/members")
    public PageEnvelope<MemberDto> members(@PathVariable String subjectKey,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listMembers(subjectKey, page, size), MemberDto::of);
    }

    @ExceptionHandler(CopresenceException.class)
    public ResponseEntity<ProblemDetail> handle(CopresenceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
