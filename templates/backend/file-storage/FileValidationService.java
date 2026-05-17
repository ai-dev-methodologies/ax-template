// @ax-template-meta: template_id=backend/file-storage/FileValidationService layer=backend domain=file-storage
// evidence: FILE-UPLOAD-001 (MIME allowlist), FILE-UPLOAD-003 (filename sanitization)
package com.ax.template.authblueprint.filestorage;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * FileValidationService — validates uploaded file attributes before storage.
 *
 * <p>FILE-UPLOAD-001: MIME type must be in the allowlist (mirrors manifest#upload.allowed_mime_types).
 * Content-type sniffing (magic bytes) should be added for production hardening against
 * polyglot attacks (CWE-434). The reference implementation validates declared MIME only.
 */
@Service
public class FileValidationService {

    /**
     * MIME type allowlist — mirrors blueprints/file-storage-manifest.yaml#upload.allowed_mime_types.
     * Update this set when the manifest changes.
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            // Images
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            // Archives
            "application/zip",
            "application/gzip"
    );

    /**
     * Validates the content type against the allowlist (FILE-UPLOAD-001).
     *
     * @param contentType declared MIME type from MultipartFile
     * @throws UnsupportedFileTypeException if not in allowlist → 415
     */
    public void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedFileTypeException(contentType);
        }
    }

    /**
     * Checks whether the given MIME type is in the allowlist (non-throwing).
     */
    public boolean isAllowedContentType(String contentType) {
        return contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase());
    }
}
