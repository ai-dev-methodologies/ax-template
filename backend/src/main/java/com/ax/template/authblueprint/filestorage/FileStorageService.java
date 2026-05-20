package com.ax.template.authblueprint.filestorage;

import com.ax.template.authblueprint.auditlog.Audited;
import com.ax.template.authblueprint.auditlog.ResourceId;

import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Application service for the file-storage domain.
 * <p>
 * Trace:
 * <ul>
 *   <li>FILE-AUTHZ-002 / 003 — every read/write filters {@code ownerUserId = caller}</li>
 *   <li>FILE-UPLOAD-001/002/003 — validates MIME allowlist, size, filename sanitization</li>
 *   <li>FILE-SCAN-001 — synchronously triggers virus scan inside the same TX; the scan
 *       transitions PENDING → READY | QUARANTINED. (Catalog scope keeps this synchronous
 *       so the test surface is deterministic; fork-receivers swap to an async dispatcher
 *       backed by a queue + Worker.)</li>
 *   <li>FILE-QUOTA-001 — sums per-user usage before accepting the new upload</li>
 *   <li>FILE-OBS-001 — MDC-tags file_id + user_id; increments Micrometer counters</li>
 * </ul>
 */
@Service
public class FileStorageService {

    private final StoredFileRepository repository;
    private final StorageBackend storage;
    private final VirusScanner scanner;
    private final FileStorageProperties properties;
    private final MeterRegistry meterRegistry;

    public FileStorageService(StoredFileRepository repository,
                              StorageBackend storage,
                              VirusScanner scanner,
                              FileStorageProperties properties,
                              MeterRegistry meterRegistry) {
        this.repository = repository;
        this.storage = storage;
        this.scanner = scanner;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * FILE-UPLOAD-001/002/003 + FILE-QUOTA-001 + FILE-SCAN-001 + FILE-OBS-001.
     */
    @Audited(action = "FILE_UPLOAD", resourceType = "file")
    @Transactional
    public StoredFile upload(@ResourceId String ownerUserId, MultipartFile multipart) {
        if (multipart == null || multipart.isEmpty()) {
            throw new IllegalArgumentException("file must be non-empty");
        }

        // FILE-UPLOAD-002 — service-layer size guard (defense in depth on top of
        // Spring's multipart layer, which is enforced via application properties).
        long size = multipart.getSize();
        long maxBytes = properties.getMaxFileSizeBytes();
        if (size > maxBytes) {
            throw new FileSizeExceededException(size, maxBytes);
        }

        // FILE-UPLOAD-001 — MIME allowlist (case-insensitive).
        String contentType = (multipart.getContentType() != null)
            ? multipart.getContentType() : "application/octet-stream";
        if (!properties.allowedMimeTypesLower().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new UnsupportedContentTypeException(contentType);
        }

        // FILE-QUOTA-001 — sum current usage + new file.
        long current = repository.sumQuotaBytesForOwner(ownerUserId);
        long maxQuota = properties.getMaxQuotaBytes();
        if (current + size > maxQuota) {
            meterRegistry.counter("storage.quota.exceeded.total").increment();
            throw new StorageQuotaExceededException(current, size, maxQuota);
        }

        // FILE-UPLOAD-003 — sanitize display filename.
        String sanitized = FilenameSanitizer.sanitize(multipart.getOriginalFilename());

        // FILE-SEC-001 — storage key is a server UUID; the sanitized name is
        // stored separately for display only and is never used as a filesystem
        // path component.
        String storageKey = UUID.randomUUID().toString();
        String sha256 = sha256Hex(multipart);
        Instant now = Instant.now();

        // Persist bytes first, then row. On row-save failure the rollback hook
        // could remove the blob; catalog scope leaves that to fork-receivers
        // who tune for their durability target.
        try (InputStream in = multipart.getInputStream()) {
            storage.put(storageKey, in, contentType, size);
        } catch (IOException ex) {
            throw new IllegalStateException("Storage put failed for " + storageKey, ex);
        }

        StoredFile entity = StoredFile.builder()
            .ownerUserId(ownerUserId)
            .fileName(sanitized)
            .contentType(contentType)
            .sizeBytes(size)
            .sha256(sha256)
            .storageKey(storageKey)
            .status(FileStatus.PENDING)
            .uploadedAt(now)
            .build();
        StoredFile saved = repository.save(entity);

        // FILE-OBS-001 — MDC + Micrometer.
        MDC.put("file_id", saved.getId().toString());
        MDC.put("user_id", ownerUserId);
        try {
            meterRegistry.counter("files.uploaded.total").increment();
            meterRegistry.counter("upload.bytes.total").increment(size);
        } finally {
            MDC.remove("file_id");
            MDC.remove("user_id");
        }

        // FILE-SCAN-001 — synchronous scan for catalog determinism. Async swap
        // is a fork-receiver concern (see class-level javadoc).
        FileScanResult result = scanner.scan(saved.getFileName(), saved.getContentType(), saved.getSizeBytes());
        FileStatus terminal = (result == FileScanResult.INFECTED)
            ? FileStatus.QUARANTINED : FileStatus.READY;
        saved.markScanResult(terminal, Instant.now());
        if (terminal == FileStatus.QUARANTINED) {
            meterRegistry.counter("files.quarantined.total").increment();
        }
        return saved;
    }

    /** FILE-AUTHZ-002 — strict owner lookup; cross-user → 404. */
    @Transactional(readOnly = true)
    public StoredFile getOwned(UUID id, String ownerUserId) {
        return repository.findByIdAndOwnerUserIdAndDeletedFalse(id, ownerUserId)
            .orElseThrow(() -> new StoredFileNotFoundException(id));
    }

    /**
     * FILE-SEC-001 — open the storage stream. Caller MUST close it.
     * The returned object is opaque; the storage key is not exposed.
     */
    public InputStream openDownload(StoredFile file) {
        try {
            meterRegistry.counter("files.downloaded.total").increment();
            return storage.get(file.getStorageKey());
        } catch (IOException ex) {
            throw new IllegalStateException("Storage get failed for file " + file.getId(), ex);
        }
    }

    /** FILE-AUTHZ-003 — owner-only deletion; cross-user already filtered to 404. */
    @Audited(action = "FILE_DELETE", resourceType = "file")
    @Transactional
    public void delete(@ResourceId UUID id, String ownerUserId) {
        StoredFile file = getOwned(id, ownerUserId);
        try {
            storage.delete(file.getStorageKey());
        } catch (IOException ex) {
            // Soft-delete continues even if the storage delete fails; a
            // retention job (manifest#retention.cleanup_cron) will retry the
            // hard delete. Catalog scope keeps this best-effort.
        }
        file.softDelete(Instant.now());
    }

    private static String sha256Hex(MultipartFile mf) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = mf.getInputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new IllegalStateException("sha256 hash failed", ex);
        }
    }
}
