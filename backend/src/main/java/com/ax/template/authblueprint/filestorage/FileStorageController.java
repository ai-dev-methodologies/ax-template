package com.ax.template.authblueprint.filestorage;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * REST surface for the file-storage domain. Owner-only access is enforced in
 * {@link FileStorageService}; this controller derives the caller's userId from
 * {@link Authentication#getName()} and never accepts a userId in the URL
 * (FILE-AUTHZ-002).
 * <p>
 * Trace:
 * <ul>
 *   <li>FILE-AUTHZ-001 — SecurityConfig maps {@code /api/files/**} to authenticated()</li>
 *   <li>FILE-AUTHZ-002 — cross-user reads → 404 via {@link StoredFileNotFoundException}</li>
 *   <li>FILE-AUTHZ-003 — delete owner-only; cross-user already filtered to 404</li>
 *   <li>FILE-UPLOAD-001 → 415 / FILE-UPLOAD-002 → 413 / FILE-UPLOAD-003 — sanitization</li>
 *   <li>FILE-SCAN-002 — PENDING download returns 202 + Retry-After</li>
 *   <li>FILE-SEC-001 / FILE-SEC-002 — response DTOs never expose {@code storageKey}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
public class FileStorageController {

    private final FileStorageService service;
    private final FileStorageProperties properties;

    public FileStorageController(FileStorageService service, FileStorageProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<StoredFileResponse> upload(
        @RequestParam("file") MultipartFile file,
        Authentication auth
    ) {
        StoredFile saved = service.upload(auth.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(StoredFileResponse.from(saved));
    }

    @GetMapping("/{id}")
    public StoredFileResponse getMetadata(@PathVariable UUID id, Authentication auth) {
        return StoredFileResponse.from(service.getOwned(id, auth.getName()));
    }

    /**
     * FILE-SCAN-002 — PENDING returns 202 + Retry-After.
     * FILE-SEC-001 — body bytes flow through the controller (no presigned-URL
     * redirect for the local backend); Content-Disposition carries the
     * sanitized display name.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable UUID id, Authentication auth) {
        StoredFile file = service.getOwned(id, auth.getName());
        switch (file.getStatus()) {
            case PENDING:
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.RETRY_AFTER, Integer.toString(properties.getScanRetryAfterSeconds()))
                    .body(Map.of(
                        "status", "PENDING",
                        "message", "scan_in_progress"));
            case QUARANTINED:
                return ResponseEntity.unprocessableEntity()
                    .body(Map.of(
                        "status", "QUARANTINED",
                        "message", "file_quarantined"));
            case DELETED:
                // Already filtered by repository query, but defensive.
                return ResponseEntity.notFound().build();
            case READY:
            default:
                InputStream stream = service.openDownload(file);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                        .filename(file.getFileName())
                        .build());
                headers.setContentLength(file.getSizeBytes());
                return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(new InputStreamResource(stream));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // ExceptionHandlers
    // ──────────────────────────────────────────────────────────────────────

    /** FILE-AUTHZ-002 — 404 not 403, to avoid leaking row existence. */
    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StoredFileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "not_found"));
    }

    /** FILE-UPLOAD-001 — 415 Unsupported Media Type. */
    @ExceptionHandler(UnsupportedContentTypeException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedContentType(UnsupportedContentTypeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        pd.setTitle("Unsupported Media Type");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(pd);
    }

    /** FILE-UPLOAD-002 — 413 Payload Too Large. */
    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleSizeExceeded(FileSizeExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        pd.setTitle("Payload Too Large");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(pd);
    }

    /** FILE-QUOTA-001 — 413 with RFC 7807 quota-exceeded ProblemDetail. */
    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuota(StorageQuotaExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        pd.setType(URI.create(properties.getQuotaErrorType()));
        pd.setTitle("Storage Quota Exceeded");
        pd.setProperty("currentBytes", ex.getCurrentBytes());
        pd.setProperty("requestedBytes", ex.getRequestedBytes());
        pd.setProperty("maxBytes", ex.getMaxBytes());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
