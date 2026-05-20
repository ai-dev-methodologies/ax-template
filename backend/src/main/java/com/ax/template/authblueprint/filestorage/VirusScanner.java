package com.ax.template.authblueprint.filestorage;

/**
 * Virus-scan SPI — catalog default is {@link MockVirusScanner} (EICAR rule
 * mirrors the manifest mock provider).
 * <p>
 * Trace: FILE-SCAN-001 — invoked by {@link FileStorageService} after upload.
 * Fork-receivers swap to ClamAV / S3 antivirus by providing an alternative
 * {@link org.springframework.context.annotation.Primary @Primary} bean.
 * Manifest: {@code blueprints/file-storage-manifest.yaml#virus_scan.provider}.
 */
public interface VirusScanner {

    /**
     * @param fileName the SANITIZED display filename (FILE-UPLOAD-003 already applied)
     * @param contentType MIME (allowlist enforced upstream)
     * @return {@link FileScanResult#CLEAN} or {@link FileScanResult#INFECTED}
     */
    FileScanResult scan(String fileName, String contentType, long sizeBytes);
}
