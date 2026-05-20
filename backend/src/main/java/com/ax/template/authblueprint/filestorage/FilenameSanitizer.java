package com.ax.template.authblueprint.filestorage;

import java.util.UUID;

/**
 * Sanitizes user-provided filenames before they are stored as the display name.
 * <p>
 * Trace: FILE-UPLOAD-003 — strip path traversal sequences (CWE-22) + control
 * characters; truncate to 255; fall back to UUID when result is empty.
 * Manifest: {@code blueprints/file-storage-manifest.yaml#upload.filename_sanitization}.
 *
 * <p>The output is the DISPLAY name only. The internal storage key is always
 * a server-generated UUID (FILE-SEC-001) so a malicious display name cannot
 * influence the filesystem/S3 path.
 */
public final class FilenameSanitizer {

    private static final int MAX_LENGTH = 255;

    private FilenameSanitizer() {}

    public static String sanitize(String raw) {
        if (raw == null) return UUID.randomUUID().toString();

        // 1. Strip path separators (CWE-22): / \ and ..
        //    Replace any directory traversal sequence with single underscore.
        String stripped = raw
            .replace("\\", "/")               // normalize Windows separators
            .replaceAll("\\.\\./", "")        // remove "../"
            .replaceAll("/", "_");            // collapse remaining separators

        // 2. Strip control chars (0x00-0x1F + 0x7F) — defense in depth.
        StringBuilder cleaned = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                cleaned.append(c);
            }
        }
        String result = cleaned.toString().trim();

        // 3. Remove leading dots/underscores that could re-introduce hidden files.
        while (!result.isEmpty()
            && (result.charAt(0) == '.' || result.charAt(0) == '_')) {
            result = result.substring(1);
        }

        // 4. Truncate (max 255 chars).
        if (result.length() > MAX_LENGTH) {
            result = result.substring(0, MAX_LENGTH);
        }

        // 5. Fallback to UUID when sanitization produced an empty string.
        if (result.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return result;
    }
}
