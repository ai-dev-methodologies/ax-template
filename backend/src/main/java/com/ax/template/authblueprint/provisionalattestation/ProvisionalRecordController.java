package com.ax.template.authblueprint.provisionalattestation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * provisional-attestation-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller ({@link Authentication#getName()}), never a client-supplied id. Delegates to
 * {@link ProvisionalRecordService}.
 */
@RestController
public class ProvisionalRecordController {

    public record AuthorReq(@NotBlank @Size(max = 4000) String content) {}

    public record EditReq(@NotBlank @Size(max = 4000) String content) {}

    public record RecordDto(UUID id, String authoredBy, String content, ProvisionalRecordStatus status,
                            String attestedBy, Instant attestedAt) {
        static RecordDto of(ProvisionalRecord r) {
            return new RecordDto(r.getId(), r.getAuthoredBy(), r.getContent(), r.getStatus(),
                r.getAttestedBy(), r.getAttestedAt());
        }
    }

    public record VerifyDto(boolean tamperDetected) {}

    private final ProvisionalRecordService service;

    public ProvisionalRecordController(ProvisionalRecordService service) {
        this.service = service;
    }

    @PostMapping("/api/provisional-attestation/records")
    public ResponseEntity<RecordDto> author(Authentication auth, @Valid @RequestBody AuthorReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RecordDto.of(service.author(auth.getName(), req.content())));
    }

    /** PATT-FREEZE-003 — only the author may edit, and only while PROVISIONAL. */
    @PutMapping("/api/provisional-attestation/records/{id}")
    public RecordDto edit(@PathVariable UUID id, Authentication auth, @Valid @RequestBody EditReq req) {
        return RecordDto.of(service.editContent(id, auth.getName(), req.content()));
    }

    /** PATT-DISTINCT-002 — the attestor MUST differ from the author. */
    @PostMapping("/api/provisional-attestation/records/{id}/attest")
    public RecordDto attest(@PathVariable UUID id, Authentication auth) {
        return RecordDto.of(service.attest(id, auth.getName()));
    }

    /** PATT-FREEZE-003 — recompute vs the attested content-hash; a mismatch is tamper-detected. */
    @GetMapping("/api/provisional-attestation/records/{id}/verify")
    public VerifyDto verify(@PathVariable UUID id) {
        return new VerifyDto(service.verifyIntegrity(id));
    }

    /** PATT-DOWNSTREAM-004 — attested-only (default) vs include-provisional filter. */
    @GetMapping("/api/provisional-attestation/records")
    public Page<RecordDto> list(@RequestParam(value = "includeProvisional", defaultValue = "false")
                                boolean includeProvisional,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return service.list(includeProvisional, pageable).map(RecordDto::of);
    }

    @GetMapping("/api/provisional-attestation/records/{id}")
    public RecordDto get(@PathVariable UUID id) {
        return RecordDto.of(service.getOrThrow(id));
    }

    @ExceptionHandler(ProvisionalAttestationException.class)
    public ResponseEntity<ProblemDetail> handle(ProvisionalAttestationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
