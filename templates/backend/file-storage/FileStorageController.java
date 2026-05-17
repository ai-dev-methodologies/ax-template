// @ax-template-meta: template_id=backend/file-storage/FileStorageController layer=backend domain=file-storage
// evidence: FILE-AUTHZ-001 (all endpoints authenticated), FILE-SCAN-002 (202+Retry-After),
//           FILE-SEC-001 (302 redirect to presigned URL)
package com.ax.template.authblueprint.filestorage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * FileStorageController — REST endpoints for the file-storage domain.
 *
 * <p>All endpoints require an authenticated session (FILE-AUTHZ-001).
 * Spring Security protects this route group via SecurityConfig (/api/files/** → authenticated).
 *
 * <p>Spec coverage:
 * <ul>
 *   <li>POST /api/files           → uploadFile (FILE-UPLOAD-001..003, FILE-QUOTA-001)</li>
 *   <li>GET  /api/files           → listFiles  (FILE-AUTHZ-002)</li>
 *   <li>GET  /api/files/{id}      → getFile    (FILE-AUTHZ-002)</li>
 *   <li>GET  /api/files/{id}/download → downloadFile (FILE-SCAN-002, FILE-SEC-001)</li>
 *   <li>DELETE /api/files/{id}    → deleteFile (FILE-AUTHZ-003)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
public class FileStorageController {

    private static final int SCAN_RETRY_AFTER_SECONDS = 5; // mirrors manifest#virus_scan.retry_after_seconds

    private final FileStorageService fileStorageService;

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // ─── POST /api/files ────────────────────────────────────────────────────

    /**
     * Uploads a file. Status will be PENDING until virus scan completes.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<FileStorageDto.StoredFileResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails principal
    ) {
        var request = new FileStorageDto.UploadRequest(description, List.of());
        var response = fileStorageService.uploadFile(principal.getUsername(), file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── GET /api/files ─────────────────────────────────────────────────────

    /**
     * Lists all files owned by the authenticated user (DELETED excluded).
     */
    @GetMapping
    public ResponseEntity<List<FileStorageDto.StoredFileResponse>> listFiles(
            @AuthenticationPrincipal UserDetails principal
    ) {
        var files = fileStorageService.listFiles(principal.getUsername());
        return ResponseEntity.ok(files);
    }

    // ─── GET /api/files/{fileId} ─────────────────────────────────────────────

    /**
     * Returns metadata for a specific file (FILE-AUTHZ-002: 404 for non-owned files).
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileStorageDto.StoredFileResponse> getFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        var response = fileStorageService.getFile(fileId, principal.getUsername());
        return ResponseEntity.ok(response);
    }

    // ─── GET /api/files/{fileId}/download ───────────────────────────────────

    /**
     * Returns a 302 redirect to a presigned download URL (FILE-SEC-001).
     *
     * <p>Status rules:
     * <ul>
     *   <li>READY:        302 redirect to presigned URL</li>
     *   <li>PENDING:      202 Accepted + Retry-After: 5 (FILE-SCAN-002)</li>
     *   <li>QUARANTINED:  422 Unprocessable Entity</li>
     * </ul>
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Void> downloadFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        try {
            String presignedUrl = fileStorageService.getDownloadUrl(fileId, principal.getUsername());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presignedUrl))
                    .build();
        } catch (FileScanPendingException e) {
            // FILE-SCAN-002: scan still in progress → 202 + Retry-After
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(SCAN_RETRY_AFTER_SECONDS))
                    .build();
        } catch (FileQuarantinedException e) {
            // FILE-SCAN-001: virus detected → 422
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    // ─── DELETE /api/files/{fileId} ──────────────────────────────────────────

    /**
     * Soft-deletes the file. Only the file owner may delete (FILE-AUTHZ-003).
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        fileStorageService.deleteFile(fileId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
