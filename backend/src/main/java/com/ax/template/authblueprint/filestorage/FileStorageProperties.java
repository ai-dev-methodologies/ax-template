package com.ax.template.authblueprint.filestorage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * File-storage configuration mirrored from
 * {@code blueprints/file-storage-manifest.yaml}.
 * <p>
 * Defaults match the manifest; production deployments override via
 * {@code application.yml} or environment variables prefixed
 * {@code AX_FILE_STORAGE_*}.
 */
@Configuration
@ConfigurationProperties(prefix = "ax.file-storage")
public class FileStorageProperties {

    /** {@code upload.max_file_size_mb}. */
    private long maxFileSizeMb = 100;

    /** {@code upload.allowed_mime_types}. */
    private List<String> allowedMimeTypes = List.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/csv",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/svg+xml",
        "application/zip",
        "application/gzip"
    );

    /** {@code virus_scan.retry_after_seconds} — Retry-After header for PENDING downloads. */
    private int scanRetryAfterSeconds = 5;

    /** {@code quota.max_quota_mb} — per-user storage cap. */
    private long maxQuotaMb = 1024;

    /** {@code quota.error_type} — RFC 7807 type URI. */
    private String quotaErrorType = "https://ax-template.example/problems/quota-exceeded";

    public long getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(long v) { this.maxFileSizeMb = v; }

    public long getMaxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }

    public List<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(List<String> v) {
        this.allowedMimeTypes = List.copyOf(v);
    }

    /** Convenience: lowercase set for case-insensitive lookup. */
    public Set<String> allowedMimeTypesLower() {
        return allowedMimeTypes.stream()
            .map(s -> s.toLowerCase(java.util.Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public int getScanRetryAfterSeconds() { return scanRetryAfterSeconds; }
    public void setScanRetryAfterSeconds(int v) { this.scanRetryAfterSeconds = v; }

    public long getMaxQuotaMb() { return maxQuotaMb; }
    public void setMaxQuotaMb(long v) { this.maxQuotaMb = v; }

    public long getMaxQuotaBytes() { return maxQuotaMb * 1024L * 1024L; }

    public String getQuotaErrorType() { return quotaErrorType; }
    public void setQuotaErrorType(String v) { this.quotaErrorType = v; }
}
