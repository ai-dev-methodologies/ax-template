package com.ax.template.authblueprint.filestorage;

import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO for a {@link StoredFile}.
 * <p>
 * Trace: FILE-SEC-001 / FILE-SEC-002 — deliberately omits {@code storageKey},
 * storage bucket, and storage provider URL. The {@code downloadUrl} below is
 * always a relative path on this server; opaque storage URIs (s3://, gs://, …)
 * never appear here.
 */
public record StoredFileResponse(
    UUID id,
    String fileName,
    String contentType,
    long sizeBytes,
    String sha256,
    FileStatus status,
    String downloadUrl,
    Instant uploadedAt,
    Instant scannedAt
) {
    public static StoredFileResponse from(StoredFile f) {
        return new StoredFileResponse(
            f.getId(),
            f.getFileName(),
            f.getContentType(),
            f.getSizeBytes(),
            f.getSha256(),
            f.getStatus(),
            "/api/files/" + f.getId() + "/download",
            f.getUploadedAt(),
            f.getScannedAt()
        );
    }
}
