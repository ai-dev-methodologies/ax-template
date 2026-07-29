/**
 * @ax-template-meta
 * template_id: backend/file-storage/FileStorageService
 * layer: backend
 * domain: file-storage
 * anchors_rule: soft-delete-audit-trail.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: internal
 *     rationale: "Core file-storage service. Realises specs/file-storage-l0.yaml FILE-AUTHZ-001..003, FILE-UPLOAD-001..003, FILE-SCAN-001..002, FILE-QUOTA-001, FILE-SEC-001 and FILE-OBS-001. deleteFile flips status to DELETED and records the event rather than removing the row — the anchored invariant. NOTE: the entity additionally carries @SQLDelete/@Where per soft-delete-only-on-base-entity.md; the two rules read this one domain differently and the divergence is recorded in the P3-90 closure rather than resolved here."
 */
package com.ax.template.authblueprint.filestorage;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FileStorageService — core business logic for the file-storage domain.
 *
 * <p>Spec coverage:
 * <ul>
 *   <li>FILE-AUTHZ-001: All public methods require a non-null callerUserId (enforced by controller JWT extraction).</li>
 *   <li>FILE-AUTHZ-002: All file lookups filter by ownerUserId → EntityNotFoundException → 404.</li>
 *   <li>FILE-AUTHZ-003: Delete enforces ownership; throws AccessDeniedException → 403 if not owner.</li>
 *   <li>FILE-UPLOAD-001: MIME type validated against allowlist in FileValidationService.</li>
 *   <li>FILE-UPLOAD-002: Size validated before storage; MultipartException caught at Tomcat layer → 413.</li>
 *   <li>FILE-UPLOAD-003: Filename sanitized by FilenameSanitizer before persistence.</li>
 *   <li>FILE-SCAN-001: Virus scan runs asynchronously; status transitions PENDING → READY|QUARANTINED.</li>
 *   <li>FILE-SCAN-002: Download of PENDING file returns 202 + Retry-After.</li>
 *   <li>FILE-QUOTA-001: Per-user quota checked before saving file bytes.</li>
 *   <li>FILE-SEC-001: storageKey never included in DTO responses.</li>
 *   <li>FILE-OBS-001: MDC tags + Micrometer counters emitted on upload/download.</li>
 * </ul>
 *
 * <p>Fork instructions:
 * <ol>
 *   <li>Replace LocalStorageBackend with S3StorageBackend for production.</li>
 *   <li>Replace MockVirusScanService with ClamAvVirusScanService or VirusTotalVirusScanService.</li>
 *   <li>Adjust quota limits via application.yml or environment variables.</li>
 *   <li>Add async execution config for virusScanService if running on dedicated executor.</li>
 * </ol>
 */
@Service
@Transactional
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Per-user quota: 1 GB in bytes (mirrors manifest#quota.max_quota_mb). */
    private static final long MAX_QUOTA_BYTES = 1024L * 1024 * 1024;

    private final FileStorageRepository fileStorageRepository;
    private final FileValidationService fileValidationService;
    private final StorageBackend storageBackend;
    private final VirusScanService virusScanService;
    private final PresignedUrlService presignedUrlService;
    private final MeterRegistry meterRegistry;

    public FileStorageService(
            FileStorageRepository fileStorageRepository,
            FileValidationService fileValidationService,
            StorageBackend storageBackend,
            VirusScanService virusScanService,
            PresignedUrlService presignedUrlService,
            MeterRegistry meterRegistry
    ) {
        this.fileStorageRepository = fileStorageRepository;
        this.fileValidationService = fileValidationService;
        this.storageBackend = storageBackend;
        this.virusScanService = virusScanService;
        this.presignedUrlService = presignedUrlService;
        this.meterRegistry = meterRegistry;
    }

    // ─── Upload ─────────────────────────────────────────────────────────────

    /**
     * Uploads a file for the given user.
     *
     * @param callerUserId authenticated user ID from JWT (FILE-AUTHZ-001)
     * @param file         multipart file
     * @param request      optional metadata (description, tags)
     * @return DTO for the created StoredFile (status=PENDING until scan completes)
     */
    public FileStorageDto.StoredFileResponse uploadFile(
            String callerUserId,
            MultipartFile file,
            FileStorageDto.UploadRequest request
    ) {
        // FILE-UPLOAD-001: validate MIME type against allowlist
        fileValidationService.validateContentType(file.getContentType());

        // FILE-UPLOAD-003: sanitize filename
        String sanitizedName = FilenameSanitizer.sanitize(file.getOriginalFilename());

        // FILE-QUOTA-001: check per-user quota before storing
        validateQuota(callerUserId, file.getSize());

        // Store file bytes in backend (local filesystem or S3)
        String storageKey;
        try {
            storageKey = storageBackend.store(UUID.randomUUID().toString(), file.getInputStream());
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }

        // Persist metadata with status=PENDING
        StoredFile entity = new StoredFile();
        entity.setOwnerUserId(callerUserId);
        entity.setFileName(sanitizedName);
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setStorageKey(storageKey);
        entity.setStatus(FileStatus.PENDING);
        if (request != null) {
            entity.setDescription(request.description());
        }

        StoredFile saved = fileStorageRepository.save(entity);

        // FILE-OBS-001: MDC + Micrometer
        MDC.put("file_id", saved.getId().toString());
        MDC.put("user_id", callerUserId);
        log.info("File uploaded: fileName={} contentType={} sizeBytes={}",
                sanitizedName, file.getContentType(), file.getSize());
        meterRegistry.counter("files.uploaded.total").increment();
        meterRegistry.counter("upload.bytes.total").increment(file.getSize());

        // FILE-SCAN-001: trigger async virus scan (status transitions in callback)
        virusScanService.scanAsync(saved.getId(), storageKey);

        return toResponse(saved);
    }

    // ─── List ───────────────────────────────────────────────────────────────

    /**
     * Lists files owned by the caller, excluding DELETED by default.
     *
     * @param callerUserId authenticated user ID
     * @return list of file responses (FILE-AUTHZ-002: filtered by owner)
     */
    @Transactional(readOnly = true)
    public List<FileStorageDto.StoredFileResponse> listFiles(String callerUserId) {
        return fileStorageRepository
                .findByOwnerUserIdAndStatusNotOrderByUploadedAtDesc(callerUserId, FileStatus.DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Get ────────────────────────────────────────────────────────────────

    /**
     * Returns file metadata for the caller's file (FILE-AUTHZ-002: IDOR → 404).
     */
    @Transactional(readOnly = true)
    public FileStorageDto.StoredFileResponse getFile(UUID fileId, String callerUserId) {
        StoredFile file = findOwnedOrThrow(fileId, callerUserId);
        return toResponse(file);
    }

    // ─── Download ───────────────────────────────────────────────────────────

    /**
     * Returns a presigned URL for downloading the file (FILE-SEC-001).
     *
     * <p>Status rules:
     * <ul>
     *   <li>READY: returns presigned URL string</li>
     *   <li>PENDING: throws FileScanPendingException (→ 202 + Retry-After in controller)</li>
     *   <li>QUARANTINED: throws FileQuarantinedException (→ 422)</li>
     *   <li>DELETED: EntityNotFoundException (→ 404)</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID fileId, String callerUserId) {
        StoredFile file = findOwnedOrThrow(fileId, callerUserId);

        return switch (file.getStatus()) {
            case READY -> {
                // FILE-OBS-001
                meterRegistry.counter("files.downloaded.total").increment();
                yield presignedUrlService.generatePresignedUrl(file.getId());
            }
            case PENDING -> throw new FileScanPendingException(fileId);  // → 202 in controller
            case QUARANTINED -> throw new FileQuarantinedException(fileId); // → 422
            case DELETED -> throw new FileNotFoundException(fileId);        // → 404
        };
    }

    // ─── Delete ─────────────────────────────────────────────────────────────

    /**
     * Soft-deletes the file (status → DELETED). Storage object is removed asynchronously.
     *
     * <p>FILE-AUTHZ-003: throws AccessDeniedException if caller is not the owner.
     */
    public void deleteFile(UUID fileId, String callerUserId) {
        StoredFile file = fileStorageRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        if (!file.getOwnerUserId().equals(callerUserId)) {
            throw new FileAccessDeniedException(fileId);  // → 403
        }

        file.setStatus(FileStatus.DELETED);
        fileStorageRepository.save(file);

        log.info("File soft-deleted: fileId={} ownerUserId={}", fileId, callerUserId);
    }

    // ─── Internal helpers ───────────────────────────────────────────────────

    /**
     * FILE-QUOTA-001: validates that the new upload would not exceed the per-user quota.
     */
    private void validateQuota(String ownerUserId, long newFileSizeBytes) {
        long currentUsage = fileStorageRepository.sumActiveSizeBytesByOwner(ownerUserId);
        if (currentUsage + newFileSizeBytes > MAX_QUOTA_BYTES) {
            throw new StorageQuotaExceededException(ownerUserId, currentUsage, newFileSizeBytes, MAX_QUOTA_BYTES);
        }
    }

    /**
     * FILE-AUTHZ-002: finds file by ID + ownerUserId; throws EntityNotFoundException → 404 if not found.
     */
    private StoredFile findOwnedOrThrow(UUID fileId, String ownerUserId) {
        return fileStorageRepository
                .findByIdAndOwnerUserId(fileId, ownerUserId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    /**
     * Maps a StoredFile entity to its DTO, including a presigned URL for READY files.
     * storageKey is NEVER included (FILE-SEC-001, FILE-SEC-002).
     */
    private FileStorageDto.StoredFileResponse toResponse(StoredFile file) {
        String downloadUrl = file.getStatus() == FileStatus.READY
                ? presignedUrlService.generatePresignedUrl(file.getId())
                : null;

        return new FileStorageDto.StoredFileResponse(
                file.getId(),
                file.getFileName(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getStatus(),
                file.getDescription(),
                List.of(), // tags — add tags field to StoredFile entity if needed
                file.getUploadedAt(),
                file.getExpiresAt(),
                downloadUrl
        );
    }
}
