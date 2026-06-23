package com.ax.template.authblueprint.sensitiveaccess;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
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
 * sensitive-read-audit-l0 thin controller. The acting principal is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). Delegates to {@link SensitiveAccessService}. The
 * default GET returns the MASKED projection only (SENSITIVE-MASK-001); the raw value is reached only
 * via the audited /reveal path (SENSITIVE-READ-001), whose response is Cache-Control: no-store so a
 * revealed value never lands in a browser/proxy cache. The access-log query is ROLE_ADMIN only
 * (SENSITIVE-QUERY-001).
 */
@RestController
public class SensitiveAccessController {

    public record RecordReq(@NotBlank @Size(max = 200) String recordRef,
                            @NotBlank @Size(max = 100) String fieldName,
                            @NotBlank @Size(max = 500) String rawValue) {}
    public record RevealReq(@NotBlank @Size(max = 500) String purpose) {}

    /** SENSITIVE-MASK-001 — the non-privileged projection: masked value only, raw value absent. */
    public record RecordDto(UUID id, String recordRef, String fieldName, String maskedValue,
                            String owner, Instant createdAt) {
        static RecordDto of(SensitiveRecord r) {
            return new RecordDto(r.getId(), r.getRecordRef(), r.getFieldName(), r.maskedValue(),
                r.getOwner(), r.getCreatedAt());
        }
    }
    public record RevealDto(UUID id, String recordRef, String fieldName, String rawValue) {}
    public record AccessLogDto(UUID id, String recordRef, String fieldName, String accessor,
                               String purpose, Instant occurredAt) {
        static AccessLogDto of(SensitiveAccessLog a) {
            return new AccessLogDto(a.getId(), a.getRecordRef(), a.getFieldName(), a.getAccessor(),
                a.getPurpose(), a.getOccurredAt());
        }
    }

    private final SensitiveAccessService service;

    public SensitiveAccessController(SensitiveAccessService service) {
        this.service = service;
    }

    @PostMapping("/api/sensitive-access/records")
    public ResponseEntity<RecordDto> record(@Valid @RequestBody RecordReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RecordDto.of(service.record(req.recordRef(), req.fieldName(), req.rawValue(),
                auth.getName())));
    }

    /** SENSITIVE-MASK-001 — masked projection; writes NO access-log row. */
    @GetMapping("/api/sensitive-access/records/{id}")
    public RecordDto get(@PathVariable UUID id) {
        return RecordDto.of(service.get(id));
    }

    /** SENSITIVE-READ/PURPOSE-001 — the audited reveal: records the access (who/when/what/why) in the
     *  same tx, then returns the raw value. The response is never cached. */
    @PostMapping("/api/sensitive-access/records/{id}/reveal")
    public ResponseEntity<RevealDto> reveal(@PathVariable UUID id, @Valid @RequestBody RevealReq req,
                                            Authentication auth) {
        SensitiveRecord masked = service.get(id);                    // 404 + the record's metadata
        String raw = service.reveal(id, auth.getName(), req.purpose());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(new RevealDto(masked.getId(), masked.getRecordRef(), masked.getFieldName(), raw));
    }

    /** SENSITIVE-QUERY-001 — admin-only append-only access trail (who saw what, when, why). */
    @GetMapping("/api/sensitive-access/records/{id}/access-log")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AccessLogDto> accessLog(@PathVariable UUID id) {
        return service.accessLog(id).stream().map(AccessLogDto::of).toList();
    }

    @ExceptionHandler(SensitiveAccessException.class)
    public ResponseEntity<ProblemDetail> handle(SensitiveAccessException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
